# -*- coding: utf-8 -*-
from copy import deepcopy

import pytz
from ciso8601 import parse_datetime_as_naive

from travel.avia.library.python.tester.testcase import TestCase
from travel.avia.ticket_daemon.ticket_daemon.daemon.extended_fares.fare_extender import FareExtender
from travel.avia.ticket_daemon.tests.lib.sample_search_result_provider import SampleSearchResultProvider


class TestFareExtender(TestCase):
    def setUp(self):
        self._fare_extender = FareExtender()
        self._sample_search_result_provider = SampleSearchResultProvider()

    def test_fare_extender_has_to_create_new_fare(self):
        search_result = self._sample_search_result_provider.get()
        fare = search_result['fares'][0]
        fare_deepcopy = deepcopy(fare)

        extended_fare = self._fare_extender.extend(fare, search_result['flights'])

        assert extended_fare is not fare
        assert fare_deepcopy == fare

    def test_fare_extender_has_to_add_new_fields_to_extended_fare(self):
        search_result = self._sample_search_result_provider.get()
        fare = search_result['fares'][0]

        extended_fare = self._fare_extender.extend(fare, search_result['flights'])

        expected_new_fields = ['start_time', 'end_time', 'duration', 'transfers_count', 'discomfort_level']

        for expected_new_field in expected_new_fields:
            assert expected_new_field in extended_fare

        assert extended_fare['start_time'] == (
            pytz.timezone('Asia/Jerusalem')
                .localize(parse_datetime_as_naive('2019-09-03T20:10:00'))
        )
        assert extended_fare['end_time'] == (
            pytz.timezone('Europe/London')
                .localize(parse_datetime_as_naive('2019-09-04T23:10:00'))
        )
        assert extended_fare['duration'] == 1740
        assert extended_fare['transfers_count'] == 2
        assert extended_fare['discomfort_level'] == 0
