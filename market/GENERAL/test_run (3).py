#!/usr/bin/env python
import __classic_import  # noqa
import unittest
from market.library.shiny.external.ydb.beam.service import Ydb
from market.pylibrary.lite.context import Context


class T(unittest.TestCase):
    def test_run(self):
        ctx = Context(svc_name='ydb', runtime_name='run-test', portman=None)
        ctx.setup()
        beam = Ydb(ctx)
        beam.start()
        self.assertTrue(beam.describe())
        self.assertFalse(beam.stop())


if __name__ == '__main__':
    unittest.main()
