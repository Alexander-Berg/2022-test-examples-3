# -*- coding: utf-8 -*-

import unittest
from StringIO import StringIO

from getter.service.promo_by_cart import RowFormatter
from getter.service.promo_by_cart import validate_promo_by_cart
from getter.service.promo_by_cart import validate_promo_by_filter_hid
from getter.service.promo_by_cart import validate_promo_by_filter_msku
from getter.service.promo_by_cart import validate_promo_by_cart_sku_special

from getter.exceptions import VerificationError


class Test(unittest.TestCase):

    def test_row_formatter(self):
        formatter = RowFormatter([
            ('foo', '{}'),
            ('bar', 'bar={}'),
        ])

        test_cases = [
            ({'foo': '123', 'bar': 456}, '123\tbar=456\n'),
            ({'foo': '123', 'bar': None}, '123\t\n'),
            ({'foo': '123'}, '123\t\n'),
            ({'foo': '0'}, '0\t\n'),
            ({'foo': 0, 'bar': 0}, '0\tbar=0\n'),
            ({'bar': 'abc'}, '\tbar=abc\n'),
            ({'foo': None, 'bar': 'abc', 'biz': 'xyz'}, '\tbar=abc\n'),
        ]
        for data, expected in test_cases:
            self.assertEqual(formatter(data), expected)

    def test_validator_promo_by_cart(self):
        test_cases = {
            validate_promo_by_cart: [
                ("123\t445\t99\tsplit=foo\tsupplier=3\n", None),
                ("123\t445\t99\tsplit=foo\t\n", None),
                ("123\t445\t99\t\tsupplier=3\n", None),
                ("456\t445\t99\t\t\n", None),
                ("456\t445\t0\t\t\n", None),
                (
                    "123\t445\t99\tsplit=foo\t\n"
                    "123\t446\t2\t\tsupplier=1\n"
                    "124\t447\t5\tsplit=foo\t\n",
                    None
                ),
                # wrong number of fields (trailing  \t missing):
                ("123\t445\t99\tsplit=foo\n", VerificationError),
                # supplier and split swaped:
                ("123\t445\t99\tsupplier=1\tsplit=foo\n", VerificationError),
                # wrong supplier value:
                ("123\t445\t99\t\tsupplier=2\n", VerificationError),
                ("789\t445\t99\n", VerificationError),
                # wrong hid:
                ("123\tfoo\t99\tsplit=foo\n", VerificationError),
                ("\t445\t99\tsplit=foo\n", VerificationError),
                # discount > 100
                ("123\t445\t101\tsplit=foo\n", VerificationError),
            ],
            validate_promo_by_filter_hid: [
                (
                    "10785221\tglfilter=10785221-6120782:110,120;\t\n"
                    "10785222\tglfilter=10785221-6120782:110,121\t\n"
                    "10752690\tglfilter=10785221-6120782:100,130;10785435:100;\tsplit=split1\n",
                    None
                ),
                # missign glfilter:
                ("10785221\t\tsplit=foo\n", VerificationError),
                ("10785221\tsplit=foo\n", VerificationError),
                # no trailing \t:
                ("10785222\tglfilter=10785221-6120782:110,121\n", VerificationError),
                # wrong glfilter
                ("10785222\tglfilter=10785221:foo\tsplit=split1\n", VerificationError),
            ],
            validate_promo_by_filter_msku: [
                (
                    "123\t\t\n"
                    "456\tprice_to=666\t\n"
                    "234\t\t\n"
                    "457\tprice_to=667\tsplit=split1\n"
                    "458\t\tsplit=split2\n",
                    None
                ),
                ("123\n", VerificationError),
                ("123\t\n", VerificationError),
                ("\tprice_to=1\tsplit=split1\n", VerificationError),
            ],
            validate_promo_by_cart_sku_special: [
                (
                    "456\t10\t\n"
                    "456\t0\t\n"
                    "789\t10\t\n"
                    "789\t12\tsplit=split1\n"
                    "780\t99\t\n"
                    "333\t12\tsplit=split2\n"
                    "789\t25\t\n",
                    None
                ),
                ("123\n", VerificationError),
                ("123\t\t\n", VerificationError),
                ("123\t12\tsplit1\n", VerificationError),
                ("123\t\n", VerificationError),
                ("123\tsplit=s\n", VerificationError),
                ("123\t-1\n", VerificationError),
                ("123\t101\t\n", VerificationError),
                ("123\t101\tsplit=s\n", VerificationError),
                ("xxx\t10\t\n", VerificationError),
                ("123\t\t10\t\n", VerificationError),
                ("\t456\t\n", VerificationError),
            ]
        }

        for validator, tests in test_cases.iteritems():
            for data, raised_ex in tests:
                if raised_ex is None:
                    validator(StringIO(data))
                    continue
                with self.assertRaises(raised_ex):
                    validator(StringIO(data))


if __name__ == '__main__':
    unittest.main()
