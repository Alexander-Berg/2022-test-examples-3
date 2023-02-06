# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from travel.rasp.library.python.common23.utils.text import split_string


class TestSplitString(object):
    def test_none(self):
        assert split_string(None) == []

    def test_empty(self):
        assert split_string('   ') == []

    def test_single(self):
        assert split_string('   q   ') == ['q']

    def test_multiple(self):
        assert split_string('   q1,w12,r123 ,y1234,t12345, u123456   ') == [
            'q1',
            'w12',
            'r123',
            'y1234',
            't12345',
            'u123456'
        ]

    def test_multiple_custom_separator(self):
        assert split_string('   q1*w12*r123 *y1234*t12345* u123456   ', '*') == [
            'q1',
            'w12',
            'r123',
            'y1234',
            't12345',
            'u123456'
        ]
