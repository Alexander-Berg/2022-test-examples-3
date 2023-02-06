#!/usr/bin/env python
# -*- coding: utf-8 -*-

from subprocess import check_call, CalledProcessError
import unittest
import yatest


VALID_SHOPS_DAT = yatest.common.source_path("market/tools/getter_validators/shopsdat/ut/valid_shops.dat")
OVERFLOW_INTEGER_SHOPS_DAT = yatest.common.source_path("market/tools/getter_validators/shopsdat/ut/overflow_integer_shops.dat")
SHOPSDAT_VALIDATOR = yatest.common.binary_path("market/tools/getter_validators/shopsdat/shopsdat_validator")


class T(unittest.TestCase):

    def test_shopsdat_validator(self):
        check_call([SHOPSDAT_VALIDATOR, VALID_SHOPS_DAT, ])

    def test_shopsdat_integer_overflow(self):
        with self.assertRaises(CalledProcessError):
            check_call([SHOPSDAT_VALIDATOR, OVERFLOW_INTEGER_SHOPS_DAT, ])
