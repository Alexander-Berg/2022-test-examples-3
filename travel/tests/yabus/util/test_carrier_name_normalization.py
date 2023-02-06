# -*- coding: utf-8 -*-
from __future__ import absolute_import, division, print_function, unicode_literals

import pytest

from yabus.util.carrier_name_normalization import normalize


def test_remove_spaces():
    assert normalize(' a b c') == 'abc'


def test_strange_yo():
    assert normalize('е a ё Ё') == 'еaее'


def test_lower():
    upper_abc = '.0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZАБВГДЕЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ'
    lower_abc = '.0123456789abcdefghijklmnopqrstuvwxyzабвгдежзийклмнопрстуфхцчшщъыьэюя'
    assert normalize(upper_abc) == lower_abc


def test_strange_chars():
    assert normalize(' -()[]_,^&$%#') == ''


@pytest.mark.parametrize("test_input,expected", [
    ("ип иванов", 'иванов'),
    ("ипанов", 'ипанов'),
    ("ипанов ип", 'ипанов'),
    ("ип ип ип", 'ип'),

    ("ооо иванов", 'иванов'),
    ("ипанов", 'ипанов'),
    ("ипанов ооо", 'ипанов'),
    ("ооо ооо ооо", 'ооо'),
])
def test_stopwords(test_input, expected):
    assert normalize(test_input) == expected
