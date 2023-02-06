# -*- coding: utf-8 -*-

import unittest

from pyb import core

import context


class TestCore(unittest.TestCase):

    def test_all(self):
        config = core.Config(plugins_dir=context.ETC_DATA_PLUGINS_DIR, prefix_dir='root')
        app = core.App(config)
        app.test_plugins()


if '__main__' == __name__:
    unittest.main()
