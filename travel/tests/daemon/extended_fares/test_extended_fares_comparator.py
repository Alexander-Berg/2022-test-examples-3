# -*- coding: utf-8 -*-
from travel.avia.library.python.tester.testcase import TestCase
from travel.avia.ticket_daemon.tests.lib.sample_search_result_provider import SampleSearchResultProvider
from travel.avia.ticket_daemon.ticket_daemon.daemon.extended_fares.extended_fares_comparator import ExtendedFaresComparator, ExtendedFaresComparingResult
from travel.avia.ticket_daemon.ticket_daemon.daemon.extended_fares.fare_extender import FareExtender


class TestExtendedFaresComparator(TestCase):
    def setUp(self):
        self._comparator = ExtendedFaresComparator()
        self._fare_extender = FareExtender()
        self._sample_search_result_provider = SampleSearchResultProvider()
        self._sample_search_result = self._sample_search_result_provider.get()

    def test_comparator_does_not_fail_and_returns_valid_result(self):
        fares = self._sample_search_result['fares']
        flights = self._sample_search_result['flights']

        fares_count_to_process = min(len(fares), 50)

        for i in range(fares_count_to_process):
            for j in range(fares_count_to_process):
                extended_fare_1 = self._fare_extender.extend(fares[i], flights)
                extended_fare_2 = self._fare_extender.extend(fares[j], flights)

                comparing_result = self._comparator.compare(extended_fare_1, extended_fare_2)

                assert isinstance(comparing_result, int)
                assert comparing_result in ExtendedFaresComparingResult.possible_results()
