
import unittest
from market.library.shiny.external.tvmapi.beam.service import TvmApi
from market.pylibrary.lite.context import Context


class T(unittest.TestCase):
    def test_run(self):
        ctx = Context(svc_name='tvmapi', runtime_name='test_run', portman=None)
        ctx.setup()
        beam = TvmApi(ctx)
        beam.start()
        self.assertTrue(beam.describe())
        self.assertFalse(beam.stop())


if __name__ == '__main__':
    unittest.main()
