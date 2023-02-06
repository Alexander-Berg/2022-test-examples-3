import unittest

import pandas as pd
import numpy as np

from lib.Kraken import Kraken


class KrakenTest(unittest.TestCase):

    def test_get_rps_limit(self):
        exception = False
        try:
            Kraken.get_rps_limit(service='abc')
        except Exception:
            exception = True
        self.assertEqual(True, exception)

        self.assertEqual(True, isinstance( Kraken.get_rps_limit(service='report_white_main'), pd.DataFrame ))
        self.assertEqual(True, isinstance( Kraken.get_rps_limit(service='report_white_main', return_last=True), np.int64 ))

    def test_get_rps_limit_by_date(self):
        self.assertEqual(True, isinstance( Kraken.get_rps_limit_by_date(service='report_white_main', date='2019-05-12', return_last=True), np.int64 ))
        self.assertEqual(True, isinstance( Kraken.get_rps_limit_by_date(service='report_white_main', date='2019-05-12'), pd.DataFrame ))


if __name__ == '__main__':
    unittest.main()
