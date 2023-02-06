# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

import pytest

from yabus.common.fields import ConcatString, SeparatedDocumentNumber


class TestSeparatedDocumentNumber(object):
    @pytest.mark.parametrize('data, expected', (
        ({}, ''),
        ({'number': 'number_value'}, 'number_value'),
        ({'series': None, 'number': 'number_value'}, 'number_value'),
        ({'series': '', 'number': 'number_value'}, 'number_value'),
        ({'series': 'series_value', 'number': 'number_value'}, 'series_value number_value'),
    ))
    def test_output(self, data, expected):
        field = SeparatedDocumentNumber(series_attribute='series', number_attribute='number')
        assert field.output('key', data) == expected


class TestConcatString(object):
    @pytest.mark.parametrize('data, expected', (
        ({}, ''),
        ({'foo': 1, 'baz': '3'}, '1, 3'),
        ({'foo': '', 'baz': '3'}, '3'),
        ({'foo': None, 'baz': '3'}, '3'),
        ({'foo': '1', 'bar': '2', 'baz': '3'}, '1, 2, 3'),
    ))
    def test_output(self, data, expected):
        field = ConcatString('foo', 'bar', 'baz')
        assert field.output('key', data) == expected
