# -+- coding: utf-8 -+-

from getter.service import loyalty
from getter.validator import VerificationError

import os
import unittest
from market.pylibrary.yatestwrap.yatestwrap import source_path


class Test(unittest.TestCase):

    def test_loyalty_validator(self):
        DATA_DIR = source_path('market/getter/tests/data/')

        VALID_PATH = os.path.join(DATA_DIR, 'loyalty_delivery_discount-valid.pbuf.sn')
        INVALID1_PATH = os.path.join(DATA_DIR, 'loyalty_delivery_discount-invalid1.pbuf.sn')
        INVALID2_PATH = os.path.join(DATA_DIR, 'loyalty_delivery_discount-invalid2.pbuf.sn')

        loyalty.validate_loyalty(open(VALID_PATH, 'rb'))

        with self.assertRaisesRegexp(VerificationError, "#01"):
            loyalty.validate_loyalty(open(INVALID1_PATH, 'rb'))

        with self.assertRaisesRegexp(VerificationError, "#02"):
            loyalty.validate_loyalty(open(INVALID2_PATH, 'rb'))

if __name__ == '__main__':
    unittest.main()
