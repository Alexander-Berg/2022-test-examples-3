
import unittest
from market.library.shiny.external.tvmtool.beam.service import TvmTool
from market.pylibrary.lite.context import Context


class T(unittest.TestCase):
    def test_run(self):
        ctx = Context(svc_name='tvmtool', runtime_name='test_run', portman=None)
        ctx.setup()
        beam = TvmTool(ctx)
        beam.start()
        self.assertTrue(beam.describe())
        self.assertFalse(beam.stop())


if __name__ == '__main__':
    unittest.main()
