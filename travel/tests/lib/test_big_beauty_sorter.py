# -*- coding: utf-8 -*-
from datetime import datetime

from travel.avia.library.python.common.models.geo import Settlement
from travel.avia.library.python.common.utils.iterrecipes import pairwise
from travel.avia.library.python.tester.testcase import TestCase
from travel.avia.ticket_daemon.ticket_daemon.api.query import Query
from travel.avia.ticket_daemon.ticket_daemon.daemon.big_beauty_sorter import BigBeautySorter, BigBeautySortings
from travel.avia.ticket_daemon.ticket_daemon.daemon.extended_fares.extended_fares_comparator import ExtendedFaresComparator
from travel.avia.ticket_daemon.ticket_daemon.daemon.extended_fares.fare_extender import FareExtender
from travel.avia.ticket_daemon.tests.lib.sample_search_result_provider import SampleSearchResultProvider


def get_query():
    mow = Settlement(id=213)
    sip = Settlement(id=10502)
    query = Query(
        point_from=mow,
        point_to=sip,
        passengers={
            'adults': 1,
            'children': 0,
            'infants': 0,
        },
        date_forward=datetime(2019, 8, 12),
        service='ticket'
    )
    query.id = 'test_qid'
    return query


class TestBigBeautySorter(TestCase):
    def setUp(self):
        self._big_beauty_sorter = BigBeautySorter(FareExtender(), ExtendedFaresComparator())
        self._sample_search_result_provider = SampleSearchResultProvider()
        self._sample_search_result = self._sample_search_result_provider.get()

    def _ensure_min_price_is_on_first_position(self, fares):
        if not fares:
            raise Exception('Expected fares collection not to be empty')

        min_prices_fare = min(fares, key=lambda f: f['tariff']['value'])
        assert fares[0]['tariff']['value'] == min_prices_fare['tariff']['value']

    def test_sorter_should_raise_an_exception_for_unknown_sort_name(self):
        self.assertRaises(
            Exception,
            lambda: self._big_beauty_sorter.sort(self._sample_search_result, get_query(), 'UNKNOWN SORT NAME')
        )

    def test_sorter_should_sort_fares_by_price(self):
        sorted_by_price_search_result = self._big_beauty_sorter.sort(self._sample_search_result, get_query(), BigBeautySortings.SORTED_BY_PRICE)

        assert len(sorted_by_price_search_result['fares']) == len(self._sample_search_result['fares'])

        for first_fare, second_fare in pairwise(sorted_by_price_search_result['fares']):
            assert (first_fare['tariff']['value'] <= second_fare['tariff']['value'])

    def test_control_with_weekdays_should_not_fail(self):
        control_with_weekdays_search_result = self._big_beauty_sorter.sort(self._sample_search_result, get_query(), BigBeautySortings.CONTROL_WITH_WEEKDAYS)

        assert len(control_with_weekdays_search_result['fares']) == len(self._sample_search_result['fares'])

        self._ensure_min_price_is_on_first_position(control_with_weekdays_search_result['fares'])

    def test_front_sort_should_not_fail(self):
        front_sort_search_result = self._big_beauty_sorter.sort(self._sample_search_result, get_query(), BigBeautySortings.FRONT_SORT)

        assert len(front_sort_search_result['fares']) == len(self._sample_search_result['fares'])

        self._ensure_min_price_is_on_first_position(front_sort_search_result['fares'])

    def test_kateov_sort_should_not_fail(self):
        kateov_sort_search_result = self._big_beauty_sorter.sort(self._sample_search_result, get_query(), BigBeautySortings.KATEOV_SORT)

        assert len(kateov_sort_search_result['fares']) == len(self._sample_search_result['fares'])

        self._ensure_min_price_is_on_first_position(kateov_sort_search_result['fares'])
