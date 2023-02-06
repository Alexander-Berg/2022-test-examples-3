# -*- coding: utf-8 -*-

import os
import unittest
from mock import patch

import context
import time
import datetime

from market.idx.pylibrary.mindexer_core.monitor_me.monitor_me import monitor_me
from market.idx.pylibrary.mindexer_core.monitor_me.monitor_me import OK, WARN, FAIL
from market.pylibrary.mindexerlib.recent_symlink_system import RecentSymlinkSystem


# текущее время в тесте - 12.10.2017 12:00:00
CURRENT_TIME_FOR_TEST = int(time.mktime(datetime.datetime(2017, 10, 12, 12, 0, 0).timetuple()))
TEST_TIMEOUT_HOURS_CRIT = 2
TEST_TIMEOUT_HOURS_WARN = 1


class Test(unittest.TestCase):
    def tearDown(self):
        context.cleanup()

    def _init_recent(self, recent_name):
        os.mkdir(os.path.join(context.rootdir, recent_name))
        os.symlink(os.path.abspath(os.path.join(context.rootdir, recent_name)), os.path.join(context.rootdir, RecentSymlinkSystem.RECENT))

    def _monitor_me(self, timeout_crit=TEST_TIMEOUT_HOURS_CRIT, timeout_warn=TEST_TIMEOUT_HOURS_WARN):
        with patch('market.idx.pylibrary.mindexer_core.monitor_me.monitor_me._get_current_time', return_value=CURRENT_TIME_FOR_TEST):
            errcode, msg = monitor_me(
                working_dir=context.rootdir,
                timeout_hours=timeout_crit,
                timeout_hours_warn=timeout_warn,
                config=context.create_config()
            )
        return (errcode, msg)

    def test_ok(self):
        context.setup()
        self._init_recent('20171012_110000')
        errcode, msg = self._monitor_me()
        assert errcode == OK
        assert msg == 'Норм'

    def test_warn1(self):
        context.setup()
        self._init_recent('20171012_105959')
        errcode, msg = self._monitor_me()
        assert errcode == WARN
        assert msg.count('Не очень-то свежее поколение') == 1

    def test_warn2(self):
        context.setup()
        self._init_recent('20171012_100000')
        errcode, msg = self._monitor_me()
        assert errcode == WARN
        assert msg.count('Не очень-то свежее поколение') == 1

    def test_crit(self):
        context.setup()
        self._init_recent('20171012_095959')
        errcode, msg = self._monitor_me()
        assert errcode == FAIL
        assert msg.count('Старенькое поколение') == 1

    def test_wrong_recent(self):
        context.setup()
        os.mkdir(os.path.join(context.rootdir, RecentSymlinkSystem.RECENT))
        errcode, msg = self._monitor_me()
        assert errcode == FAIL
        assert msg.count('Не могу разобрать') == 1

    def test_no_warn_timeout1(self):
        context.setup()
        self._init_recent('20171012_100000')
        errcode, msg = self._monitor_me(timeout_warn=None)
        assert errcode == OK
        assert msg == 'Норм'

    def test_no_warn_timeout2(self):
        context.setup()
        self._init_recent('20171012_095959')
        errcode, msg = self._monitor_me(timeout_warn=None)
        assert errcode == FAIL
        assert msg.count('Старенькое поколение') == 1

    def test_warn_timeout_bigger_than_crit(self):
        context.setup()
        self._init_recent('20171012_085959')
        errcode, msg = self._monitor_me(timeout_warn=3)
        assert errcode == FAIL
        assert msg.count('Старенькое поколение') == 1

    def test_recent_not_found(self):
        context.setup()
        errcode, msg = self._monitor_me()
        assert errcode == WARN
        assert msg.count('Не найдено') == 1


if __name__ == '__main__':
    unittest.main()
