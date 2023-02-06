#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

import os
from core.types import Offer
from core.testcase import TestCase, main


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.init_threads = 16
        cls.settings.enable_exec_stats_for_global_init = True
        cls.index.offers += [Offer(title='iphone')]

    def test_exec_stats_for_global_init(self):
        self.assertFragmentIn(self.report.request_xml('admin_action=flushlogs'), '<status>Logs flushed ok</status>')
        log_path = os.path.join(self.meta_paths.logs, 'exec-stats.log')
        self.assertNotEqual(0, os.path.getsize(log_path))


if __name__ == '__main__':
    main()
