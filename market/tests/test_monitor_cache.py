# coding: utf-8

from market.idx.pylibrary.monitor_cache import MonitorCacher

import os
import shutil


class TestMonitorCacher(object):
    TMP = 'tmp'

    def setup(self):
        self.teardown()
        os.mkdir(self.TMP)

    def teardown(self):
        shutil.rmtree(self.TMP, ignore_errors=True, onerror=None)

    def test_all(self):
        m = MonitorCacher(self.TMP)
        m['offers-robot2-daemon'] = "0; OK"
        m['offers-robot2-processed-feeds'] = "1; FAIL"

        assert m['offers-robot2-daemon'] == "0; OK"
        assert m['offers-robot2-processed-feeds'] == "1; FAIL"
