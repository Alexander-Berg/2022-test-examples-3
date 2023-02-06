# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

from travel.rasp.bus.scripts.cache_reseter.keys import CalendarKey, SearchResultKey, SegmentsKey


class TestSearchResultKey(object):
    def test_connector_key(self):
        assert str(SearchResultKey.create_by_connector('some_connector')) == 'task.some_connector.search:*:*:*'

    def test_direction_key(self):
        assert (
            str(SearchResultKey.create_by_direction('some_connector', 'from_point', 'to_point'))
        ) == 'task.some_connector.search:from_point:to_point:*'

    def test_universal(self):
        assert (
            str(SearchResultKey.create_universal_key())
        ) == 'task.*.search:*:*:*'

    def test_full(self):
        assert (
            str(SearchResultKey('some_connector', 'from_point', 'to_point', 'when_value'))
        ) == 'task.some_connector.search:from_point:to_point:when_value'


class TestSegmentsKey(object):
    def test_full(self):
        assert (
            str(SegmentsKey('some_connector'))
        ) == 'task.some_connector.segments'

    def test_universal(self):
        assert (
            str(SegmentsKey.create_universal_key())
        ) == 'task.*.segments'


class TestCalendarKey(object):
    def test_connector_key(self):
        assert str(CalendarKey.create_by_connector('some_connector')) == 'some_connector.calendar:*:*:*'

    def test_direction_key(self):
        assert (
            str(CalendarKey.create_by_direction('some_connector', 'from_point', 'to_point'))
        ) == 'some_connector.calendar:from_point:to_point:*'

    def test_universal(self):
        assert (
            str(CalendarKey.create_universal_key())
        ) == '*.calendar:*:*:*'

    def test_full(self):
        assert (
            str(CalendarKey('some_connector', 'from_point', 'to_point', 'when_value'))
        ) == 'some_connector.calendar:from_point:to_point:when_value'
