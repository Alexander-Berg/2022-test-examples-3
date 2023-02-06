# -+- coding: utf-8 -+-

from getter.service import marketdynamic
from getter.validator import VerificationError

from market.pylibrary.yatestwrap.yatestwrap import source_path

import os
import unittest


class Test(unittest.TestCase):

    def test_marketdynamic_validator(self):
        DATA_DIR = source_path('market/getter/tests/data/')

        VALID_PATH = os.path.join(DATA_DIR, 'market_dynamic_valid')
        NEGATIVE_BLUE_SUPPLIER_ID_PATH = os.path.join(DATA_DIR, 'market_dynamic_invalid1')
        WRONG_MAGIC_MARKET_SKU_FILTERS_PATH = os.path.join(DATA_DIR, 'market_dynamic_invalid2')

        marketdynamic.validate_marketdynamic(VALID_PATH)

        with self.assertRaises(VerificationError):
            marketdynamic.validate_marketdynamic(NEGATIVE_BLUE_SUPPLIER_ID_PATH)

        with self.assertRaises(VerificationError):
            marketdynamic.validate_marketdynamic(WRONG_MAGIC_MARKET_SKU_FILTERS_PATH)


if __name__ == '__main__':
    unittest.main()
