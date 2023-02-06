# -*- coding: utf-8 -*-

import tempfile

import context
from market.pylibrary.mindexerlib.recent_symlink_system import RecentSymlinkSystem, unixtime2generation
from market.pylibrary.mindexerlib import util

import unittest
import yatest.common


SECONDS_IN_DAY = 3600 * 24


class TestRecentSymlinkSystem(unittest.TestCase):
    def setUp(self):
        self._work_dir = tempfile.mkdtemp(dir=yatest.common.output_path())
        self._folder = RecentSymlinkSystem(self._work_dir)

    def make_dummy_generation(self, generation_name, is_recent=False):
        with self._folder.make_tmpdir() as tmpdir:
            self._folder.make_generation(tmpdir, generation_name)
            if is_recent:
                self._folder.update_recent(generation_name)

    def test_clear(self):
        now = util.now() - 60
        self.make_dummy_generation(unixtime2generation(now - 2 * SECONDS_IN_DAY))
        self.make_dummy_generation(unixtime2generation(now - 1 * SECONDS_IN_DAY))
        self.make_dummy_generation(unixtime2generation(now), is_recent=True)
        self.make_dummy_generation(unixtime2generation(now + 1 * SECONDS_IN_DAY))

        # remove all except now
        self._folder.clear(keep_alive=2, timeout_seconds=SECONDS_IN_DAY)
        self.assertEqual(self._folder.get_all_generations(), [unixtime2generation(now)])

        # remove all include recent
        self._folder.clear(timeout_seconds=10, keep_recent=False)
        self.assertTrue(self._folder.empty())

    def test_clear_by_keep_alive(self):
        now = util.now()
        self.make_dummy_generation(unixtime2generation(now - 3 * SECONDS_IN_DAY))  # 1
        self.make_dummy_generation(unixtime2generation(now - 2 * SECONDS_IN_DAY))  # 2
        self.make_dummy_generation(unixtime2generation(now - 1 * SECONDS_IN_DAY))  # 3
        self.make_dummy_generation(unixtime2generation(now), is_recent=True)       # 4
        self.make_dummy_generation(unixtime2generation(now + 1 * SECONDS_IN_DAY))  # 5

        self._folder.clear(keep_alive=2)

        self.assertEqual(self._folder.get_all_generations(),
                         [unixtime2generation(now - 1 * SECONDS_IN_DAY),
                          unixtime2generation(now)])

    def test_clear_by_timeout(self):
        now = util.now()
        self.make_dummy_generation(unixtime2generation(now - 3 * SECONDS_IN_DAY))  # 1
        self.make_dummy_generation(unixtime2generation(now - 2 * SECONDS_IN_DAY))  # 2
        self.make_dummy_generation(unixtime2generation(now - 1 * SECONDS_IN_DAY))  # 3
        self.make_dummy_generation(unixtime2generation(now), is_recent=True)       # 4
        self.make_dummy_generation(unixtime2generation(now + 1 * SECONDS_IN_DAY))  # 5

        # remove all generations older than recent_ts - timeouts_seconds
        self._folder.clear(timeout_seconds=SECONDS_IN_DAY + 60)

        self.assertEqual(self._folder.get_all_generations(),
                         [unixtime2generation(now - 1 * SECONDS_IN_DAY),
                          unixtime2generation(now)])

    def test_clear_only_recent(self):
        now = util.now()
        self.make_dummy_generation(unixtime2generation(now - 4 * SECONDS_IN_DAY))  # 1
        self.make_dummy_generation(unixtime2generation(now - 3 * SECONDS_IN_DAY))  # 2
        self.make_dummy_generation(unixtime2generation(now - 2 * SECONDS_IN_DAY))  # 3
        self.make_dummy_generation(unixtime2generation(now - 1 * SECONDS_IN_DAY),  # 4
                                   is_recent=True)
        self.make_dummy_generation(unixtime2generation(now))                       # 5

        self._folder.clear(keep_alive=3, timeout_seconds=SECONDS_IN_DAY - 60)

        self.assertEqual(self._folder.get_all_generations(), [unixtime2generation(now - 1 * SECONDS_IN_DAY)])

    def test_clear_absolute(self):
        now = util.now()
        self.make_dummy_generation(unixtime2generation(now - 4 * SECONDS_IN_DAY))  # 1
        self.make_dummy_generation(unixtime2generation(now - 3 * SECONDS_IN_DAY))  # 2
        self.make_dummy_generation(unixtime2generation(now - 2 * SECONDS_IN_DAY))  # 3
        self.make_dummy_generation(unixtime2generation(now - 1 * SECONDS_IN_DAY),  # 4
                                   is_recent=True)
        self.make_dummy_generation(unixtime2generation(now))                       # 5

        self._folder.clear(keep_alive=3, timeout_seconds=SECONDS_IN_DAY - 60, keep_recent=False)

        self.assertEqual(self._folder.get_all_generations(), [])
        self.assertTrue(self._folder.empty())

    def test_clear_totally(self):
        now = util.now()
        self.make_dummy_generation(unixtime2generation(now - 4 * SECONDS_IN_DAY))  # 1
        self.make_dummy_generation(unixtime2generation(now - 3 * SECONDS_IN_DAY))  # 2
        self.make_dummy_generation(unixtime2generation(now - 2 * SECONDS_IN_DAY))  # 3
        self.make_dummy_generation(unixtime2generation(now - 1 * SECONDS_IN_DAY),  # 4
                                   is_recent=True)
        self.make_dummy_generation(unixtime2generation(now))                       # 5

        self._folder.clear(keep_alive=0)
        self.assertTrue(self._folder.empty())

    def test_default(self):
        now = util.now()
        self.make_dummy_generation(unixtime2generation(now - 4 * SECONDS_IN_DAY))  # 1
        self.make_dummy_generation(unixtime2generation(now - 3 * SECONDS_IN_DAY))  # 2
        self.make_dummy_generation(unixtime2generation(now - 2 * SECONDS_IN_DAY))  # 3
        self.make_dummy_generation(unixtime2generation(now - 1 * SECONDS_IN_DAY),  # 4
                                   is_recent=True)
        self.make_dummy_generation(unixtime2generation(now))                       # 5

        self._folder.clear()  # assume keep_alive = 1
        self.assertEqual(self._folder.get_all_generations(), [unixtime2generation(now - 1 * SECONDS_IN_DAY)])

if '__main__' == __name__:
    context.main()
