# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime, date, time
from unittest import TestCase

import pytest
from mock import Mock
from rest_framework import serializers

from travel.rasp.wizards.wizard_lib.direction.filters import ArrivalTimeFilter, DepartureTimeFilter


class TestTimeFilters(TestCase):
    def test_load__unknown_values(self):
        error_message = (
            'invalid time filter value: it should be one of [00:00-06:00, 06:00-12:00, 12:00-18:00, 18:00-24:00]'
        )

        not_valid_params = [
            ['00:01-06:00'],
            ['00:00-06:01'],
            ['00:00-00:00'],
            ['00:00-'],
            ['-00:00'],
        ]

        for filter_type in [ArrivalTimeFilter, DepartureTimeFilter]:
            for params in not_valid_params:
                with pytest.raises(serializers.ValidationError) as error:
                    filter_type.load(params)

                assert error.value.detail == [error_message]

    def test_load__valid_values(self):
        valid_params = [
            [],
            ['00:00-06:00'],
            ['06:00-12:00'],
            ['12:00-18:00'],
            ['18:00-24:00'],
        ]

        for filter_type in [ArrivalTimeFilter, DepartureTimeFilter]:
            for params in valid_params:
                filter_type.load(params)

    def _make_variant(self, departure_time, arrival_time):
        return Mock(segment=Mock(
            train_number=None,
            train_title=None,
            train_brand=None,
            duration=None,
            departure_station=None,
            departure_local_dt=None if departure_time is None else datetime.combine(date(2019, 1, 1), departure_time),
            arrival_station=None,
            arrival_local_dt=None if arrival_time is None else datetime.combine(date(2019, 1, 1), arrival_time),
            places=None,
            updated_at=None
        ), places_group=None)

    def test_arrival_bind__all_cases(self):
        times = [
            time(0, 0), time(1, 0), time(0, 1), time(5, 59),
            time(6, 0), time(6, 1), time(9, 0), time(11, 59),
            time(12, 0), time(12, 1), time(14, 1), time(17, 59),
            time(18, 0), time(18, 1), time(21, 0), time(23, 59)
        ]

        for value in times:
            f = DepartureTimeFilter.load(['00:00-06:00'])
            f.bind([
                self._make_variant(value, None)
            ])
            assert f.is_bound

        for value in times:
            f = ArrivalTimeFilter.load(['00:00-06:00'])
            f.bind([
                self._make_variant(None, value)
            ])
            assert f.is_bound

    def test_departure_list_selectors__all_cases(self):
        def check(params, expected_values, actual_variants):
            f = DepartureTimeFilter.load(params)
            f.bind(actual_variants)
            assert f.list_selectors() == expected_values

        variants = [
            self._make_variant(time(0,  0), None), self._make_variant(time(5, 59), None),
            self._make_variant(time(6,  0), None), self._make_variant(time(11, 59), None),
            self._make_variant(time(12, 0), None), self._make_variant(time(17, 59), None),
            self._make_variant(time(18, 0), None), self._make_variant(time(23, 59), None),
        ]

        check([], [True] * len(variants), variants)
        check(['00:00-06:00'], [True] * 2 + [False] * (len(variants) - 2), variants)
        check(['06:00-12:00'], [False] * 2 + [True] * 2 + [False] * (len(variants) - 4), variants)
        check(['12:00-18:00'], [False] * 4 + [True] * 2 + [False] * (len(variants) - 6), variants)
        check(['18:00-24:00'], [False] * 6 + [True] * 2 + [False] * (len(variants) - 8), variants)

    def test_arrival_list_selectors__all_cases(self):
        def check(params, expected_values, actual_variants):
            f = ArrivalTimeFilter.load(params)
            f.bind(actual_variants)
            assert f.list_selectors() == expected_values

        variants = [
            self._make_variant(None, time(0, 0)), self._make_variant(None,  time(5, 59)),
            self._make_variant(None, time(6, 0)), self._make_variant(None,  time(11, 59)),
            self._make_variant(None, time(12, 0)), self._make_variant(None, time(17, 59)),
            self._make_variant(None, time(18, 0)), self._make_variant(None, time(23, 59)),
        ]

        check([], [True] * len(variants), variants)
        check(['00:00-06:00'], [True] * 2 + [False] * (len(variants) - 2), variants)
        check(['06:00-12:00'], [False] * 2 + [True] * 2 + [False] * (len(variants) - 4), variants)
        check(['12:00-18:00'], [False] * 4 + [True] * 2 + [False] * (len(variants) - 6), variants)
        check(['18:00-24:00'], [False] * 6 + [True] * 2 + [False] * (len(variants) - 8), variants)
