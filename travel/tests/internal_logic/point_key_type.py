# -*- coding: utf-8 -*-

import unittest

from parameterized import parameterized

from travel.avia.ticket_daemon_processing.pretty_fares.internal_logic.point_key import PointKeyType, PointKeyTypeParser, PointKeyTypeFormatter


class PointKeyTypeParserTestCase(unittest.TestCase):
    @parameterized.expand([
        ('s', PointKeyType.Station),
        ('c', PointKeyType.Settlement),
        ('r', PointKeyType.Region),
        ('l', PointKeyType.Country),
        ('?', PointKeyType.Other),
        ('#', PointKeyType.Other),
    ])
    def test_parse_returns_correct_types(self, raw, expected):
        # type: (str, PointKeyType) -> None

        assert PointKeyTypeParser.parse(raw) == expected


class PointKeyTypeFormatterTestCase(unittest.TestCase):
    @parameterized.expand([
        (PointKeyType.Station, 's'),
        (PointKeyType.Settlement, 'c'),
        (PointKeyType.Region, 'r'),
        (PointKeyType.Country, 'l'),
        (PointKeyType.Other, '?'),
    ])
    def test_parse_returns_correct_types(self, point_key_type, expected_string):
        # type: (str, PointKeyType) -> None

        assert PointKeyTypeFormatter.to_string(point_key_type) == expected_string
