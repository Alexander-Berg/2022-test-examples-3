# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

from common.tester.testcase import TestCase
from travel.rasp.morda_backend.morda_backend.settlement.data_layer.request import to_int


class TestDataConverterToInt(TestCase):
    def test_none(self):
        assert to_int(None) is None

    def test_list(self):
        assert to_int([1]) is None

    def test_dict(self):
        assert to_int({'id': 1}) is None

    def test_empty_string(self):
        assert to_int('') is None

    def test_not_number_string(self):
        assert to_int('123A') is None

    def test_zero_string(self):
        assert to_int('0') == 0

    def test_int_string(self):
        assert to_int('123') == 123

    def test_float_string(self):
        assert to_int('123.75') == 123

    def test_int(self):
        assert to_int(123) == 123

    def test_float(self):
        assert to_int(123.75) == 123
