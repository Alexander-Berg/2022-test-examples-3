# -*- coding: utf-8 -*-
from __future__ import absolute_import, division, print_function, unicode_literals

from mock import create_autospec
from pytest import mark

from travel.rasp.mysql_dumper.lib.loaders import BaseDBLoader, MySQLConnector


def get_connector_mock(ids):
    connector = create_autospec(MySQLConnector)
    connector.get_min_id = lambda: ids[0]
    connector.get_max_id = lambda: ids[-1]
    connector.get_count = lambda a, b: len(filter(lambda x: a <= x < b, ids))
    return connector


@mark.parametrize('ids, left, count, expected', [
    (range(10), 0, 10, 10),
    (range(10), 1, 4, 5),
    (range(0, 20, 3), 2, 1, 4),
    (range(10, 20), 18, 2, 20),
    ([1, 2, 3, 7, 9, 15, 26, 30], 3, 5, 27),
    (range(0, 100000, 20), 1000, 10, 1181)
])
def test_find_right_border(ids, left, count, expected):
    loader = BaseDBLoader(get_connector_mock(ids))
    assert loader.find_right_border(left, count) == expected


@mark.parametrize('ids, parts, expected', [
    (range(10), 1, [(0, 10)]),
    (range(10), 11, [(0, 1), (1, 2), (2, 3), (3, 4), (4, 5), (5, 6), (6, 7), (7, 8), (8, 9), (9, 10), (10, 10)]),
    (range(1000), 2, [(0, 500), (500, 1000)]),
    (range(0, 100, 5), 3, [(0, 31), (31, 66), (66, 96)]),
])
def test_split_interval(ids, parts, expected):
    loader = BaseDBLoader(get_connector_mock(ids))
    assert loader.split_interval(parts) == expected
