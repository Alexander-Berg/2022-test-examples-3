import unittest
import pandas as pd
import numpy as np
import os

from lib.DAU import DAU
from lib.oauth import get_stat_oauth_token

class DAUTest(unittest.TestCase):

    def setUp(self):
        self.DAU = DAU(oauth_token=get_stat_oauth_token())

    def test_get_DAU(self):
        exception = False
        try:
            self.DAU.get(service='ozon')
        except Exception:
            exception = True
        self.assertEqual(True, exception)

        self.assertEqual(True, isinstance(self.DAU.get(service="market", date_from='2019-04-01', date_to='2019-04-07'), pd.DataFrame))
        self.assertEqual(False, self.DAU.get(service="market", date_from='2019-04-01', date_to='2019-04-07').empty)

    def test_get_DAU_by_date(self):
        self.assertEqual(False, self.DAU.get_by_date(service="market", date='2019-04-01').empty)
        self.assertEqual(True, isinstance(self.DAU.get_by_date(service="market", date='2019-04-01').loc['touch'].DAU, np.int64))


if __name__ == '__main__':
    unittest.main()
