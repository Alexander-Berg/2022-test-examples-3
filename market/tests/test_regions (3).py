# -*- coding: utf-8 -*-

from market.idx.pylibrary.regional_delivery.regions import RegionContainer
from .common import GEO_C2P_FILE, COUNTRIES_UTF8_FILE


'''
Region tree (* - country):

ROOT
|-5*
| |-2
| | `-1
| |   `-3
| `-7
|-6*
`-8
'''


def test_list_to_root():
    expected = {
        1: [1, 2, 5],
        2: [2, 5],
        3: [3, 1, 2, 5],
        5: [5],
        6: [6],
        7: [7, 5],
        8: [8],
    }

    region_container = RegionContainer(GEO_C2P_FILE, COUNTRIES_UTF8_FILE)
    actual = {}
    for r in expected.keys():
        actual[r] = region_container.get_path_to_root(r)

    assert expected == actual


def test_countries():
    expected = {
        1: 5,
        2: 5,
        3: 5,
        5: 5,
        6: 6,
        7: 5,
    }

    region_container = RegionContainer(GEO_C2P_FILE, COUNTRIES_UTF8_FILE)
    actual = {}
    for r in expected.keys():
        actual[r] = region_container.get_country_for_region(r)

    assert expected == actual
