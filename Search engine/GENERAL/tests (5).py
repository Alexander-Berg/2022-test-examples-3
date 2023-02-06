# -*- coding:utf-8 -*-

from django.test import TestCase

import os

from get_panel_config import PanelConfig, list_files

class ConfigTestCase(TestCase):
    def test_all(self):
        import itertools
        files = list_files()

        ok = True
        defaults = {
            # Do not fail for panel snippets that contain no prj
            'prj': ['default']
        }
        for cfg in files:
            try:
                PanelConfig(cfg, default=defaults)
                # print('%s — ok' % cfg)
            except Exception as x:
                print('Failed to load config for %s:\n%s\n' % (cfg, x))
                ok = False
        self.assertTrue(ok)
