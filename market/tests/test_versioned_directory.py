# coding: utf-8

import contextlib
import os.path

from datetime import datetime, timedelta
from unittest import TestCase

import mock

from market.pylibrary import common
from market.pylibrary.common.context import close as close_context

from market.idx.pylibrary.versioned_data import VersionedDirectory, VersionError, UncompletedVersion
from market.idx.pylibrary.versioned_data import LockError, LockTimeoutError


@contextlib.contextmanager
def _create_version(vdir, date):
    datetime_mod = 'market.idx.pylibrary.versioned_data.versioned_directory.datetime'
    with mock.patch(datetime_mod) as MockDateTime:
        MockDateTime.utcnow.return_value = date
        MockDateTime.now.return_value = date
        with vdir.create_version() as ver:
            yield ver


class TestVersionedDirectory(TestCase):

    def test_create_version(self):
        with common.temp.make_dir() as temp_dir:
            vdir = VersionedDirectory(temp_dir)
            date = datetime.now()

            with _create_version(vdir, date) as ver:
                self.assertTrue(os.path.exists(ver.path))
                self.assertTrue(os.path.isdir(ver.path))

                # check raise if version.path has already created
                with self.assertRaises(VersionError):
                    close_context(_create_version(vdir, date))

                # check raise if version.path has already locked
                makedirs_mod = 'market.idx.pylibrary.versioned_data.versioned_directory.os.makedirs'
                with mock.patch(makedirs_mod) as MockMakeDirs:
                    MockMakeDirs.return_value = None
                    with self.assertRaises(LockError):
                        close_context(_create_version(vdir, date))

    def test_lock_version(self):
        with common.temp.make_dir() as temp_dir:
            vdir = VersionedDirectory(temp_dir)
            with vdir.create_version() as ver:

                with self.assertRaises(LockError):
                    close_context(vdir.lock_version(ver, blocking=False))

                with self.assertRaises(LockTimeoutError):
                    close_context(vdir.lock_version(ver, blocking=True, timeout=0.3))

            with vdir.lock_version(ver):
                self.assertTrue(os.path.exists(ver.path))
                self.assertTrue(os.path.isdir(ver.path))
                with self.assertRaises(LockError):
                    close_context(vdir.lock_version(ver, blocking=False))

    def test_versions(self):
        with common.temp.make_dir() as temp_dir:
            vdir = VersionedDirectory(temp_dir)
            v1 = close_context(_create_version(vdir, datetime.now()))
            v2 = close_context(_create_version(vdir, datetime.now() + timedelta(hours=1)))
            self.assertEqual(list(vdir.versions), [v1, v2])

    def test_recent(self):
        with common.temp.make_dir() as temp_dir:
            vdir = VersionedDirectory(temp_dir)

            v1 = close_context(_create_version(vdir, datetime.now()))
            self.assertIsNone(vdir.recent)
            vdir.update_recent(v1)
            self.assertEqual(vdir.recent, v1)

            v2 = close_context(_create_version(vdir, datetime.now() + timedelta(hours=1)))
            self.assertEqual(vdir.recent, v1)
            vdir.update_recent(v2)
            self.assertEqual(vdir.recent, v2)

            with _create_version(vdir, datetime.now() + timedelta(hours=2)) as v3:
                self.assertEqual(vdir.recent, v2)
                with self.assertRaises(UncompletedVersion):
                    vdir.update_recent(v3)
            self.assertTrue(v3)

    def test_bool(self):
        with common.temp.make_dir() as temp_dir:
            vdir = VersionedDirectory(temp_dir)
            self.assertFalse(vdir)
            close_context(vdir.create_version())
            self.assertTrue(vdir)

    def test_remove_version(self):
        with common.temp.make_dir() as temp_dir:
            vdir = VersionedDirectory(temp_dir)
            v1 = close_context(_create_version(vdir, datetime.now()))
            v2 = close_context(_create_version(vdir, datetime.now() + timedelta(hours=1)))

            self.assertEqual(list(vdir.versions), [v1, v2])
            vdir.remove_version(v1)
            self.assertEqual(list(vdir.versions), [v2])

            with vdir.lock_version(v2):
                with self.assertRaises(LockError):
                    vdir.remove_version(v2, blocking=False)
                self.assertEqual(list(vdir.versions), [v2])
                with self.assertRaises(LockTimeoutError):
                    vdir.remove_version(v2, timeout=0.3)
                self.assertEqual(list(vdir.versions), [v2])

            vdir.remove_version(v2)
            self.assertEqual(list(vdir.versions), [])

    def test_clear(self):
        with common.temp.make_dir() as temp_dir:
            vdir = VersionedDirectory(temp_dir)
            for h in range(15):
                close_context(_create_version(vdir, datetime.now() + timedelta(hours=h)))
            vdir.update_recent()

            keep_last_n = 10
            expected = list(vdir.versions)[-keep_last_n:]
            vdir.clear(keep_last_n)
            self.assertEquals(list(vdir.versions), expected)

            # bad version
            bad_version1 = expected[-1]
            os.unlink(os.path.join(bad_version1.path, '.meta'))
            bad_version2 = expected[-2]
            bad_version2.flush(completed=False)

            # unknown data
            unknown_data = os.path.join(temp_dir, 'tmp')
            os.makedirs(unknown_data)
            with open(os.path.join(unknown_data, 'data'), 'w') as fobj:
                fobj.write('test data')

            keep_last_n = 3
            expected = list(vdir.versions)[-keep_last_n:]
            vdir.clear(keep_last_n)
            self.assertEquals(list(vdir.versions), expected)
            self.assertFalse(os.path.exists(unknown_data))
            self.assertFalse(os.path.exists(bad_version1.path))
            self.assertFalse(os.path.exists(bad_version2.path))
