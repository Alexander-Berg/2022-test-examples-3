# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from search.martylib.test_utils import TestCase

from search.martylib.types.int_utils import *


class TestUtils(TestCase):
    def test_is_uint(self):
        self.assertFalse(is_uint16(-1))
        self.assertFalse(is_uint32(-1))
        self.assertFalse(is_uint64(-1))
        self.assertFalse(is_uint128(-1))

        self.assertTrue(is_uint16(0))
        self.assertTrue(is_uint32(0))
        self.assertTrue(is_uint64(0))
        self.assertTrue(is_uint128(0))

        self.assertTrue(is_uint16(1))
        self.assertTrue(is_uint32(1))
        self.assertTrue(is_uint64(1))
        self.assertTrue(is_uint128(1))

        self.assertTrue(is_uint16(2 ** 16 - 1))
        self.assertFalse(is_uint16(2 ** 16))

        self.assertTrue(is_uint32(2 ** 32 - 1))
        self.assertFalse(is_uint32(2 ** 32))

        self.assertTrue(is_uint64(2 ** 64 - 1))
        self.assertFalse(is_uint64(2 ** 64))

        self.assertTrue(is_uint128(2 ** 128 - 1))
        self.assertFalse(is_uint128(2 ** 128))

        self.assertFalse(is_uint16(1.0))
        self.assertFalse(is_uint16(1.1))
        self.assertFalse(is_uint16('1'))
        self.assertFalse(is_uint16('not_integer'))
        self.assertFalse(is_uint16(None))
