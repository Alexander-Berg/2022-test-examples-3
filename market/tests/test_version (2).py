# coding: utf-8

import os.path
from unittest import TestCase

from market.pylibrary import common
from market.idx.pylibrary.versioned_data import Version, Meta
from market.idx.pylibrary.versioned_data.errors import UncompletedVersion


class TestVersion(TestCase):

    def test_init(self):
        path = '/test/path/ver00'
        meta = {'attr0': 123, 'attr1': 'asdf'}
        with self.assertRaises(AssertionError):
            v = Version(path, meta)

        meta['timestamp'] = 123123123
        v = Version(path, meta)
        self.assertEqual(v.path, path)
        self.assertEqual(v.name, os.path.basename(path))
        self.assertEqual(v.meta.timestamp, meta['timestamp'])
        self.assertEqual(v.meta.__dict__, meta)

    def test_from_path(self):
        with common.temp.make_dir() as temp_dir:
            meta = Meta(timestamp=123123123, attr0='test_val')
            v0 = Version(temp_dir, meta)
            v0.flush()

            v1 = Version.from_path(temp_dir)
            self.assertEqual(v1, v0)

            v2 = Version.from_path(temp_dir)
            v2.meta.new_attr = 'new val'
            self.assertNotEqual(v2, v0)

            v0.flush(completed=False)
            with self.assertRaises(UncompletedVersion):
                Version.from_path(temp_dir)

    def test_lt_operator(self):
        v0 = Version('/test/path/ver00', {'timestamp': 123})
        v1 = Version('/test/path/ver01', {'timestamp': 234})
        self.assertGreater(v1, v0)
        self.assertLess(v0, v1)

    def test_hash(self):
        hash(Version('/test/path/ver00', {'timestamp': 123}))
