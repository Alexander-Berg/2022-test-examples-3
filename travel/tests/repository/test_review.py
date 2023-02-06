# -*- coding: utf-8 -*-
from __future__ import absolute_import

from mock import Mock, patch

from travel.avia.library.python.tester.testcase import TestCase

from travel.avia.backend.repository.review import ReviewStatRepository


class TestReviewStatRepository(TestCase):
    def setUp(self):
        with patch.object(ReviewStatRepository, '_load_flight_stat') as load_flight_stat:
            load_flight_stat.return_value = [
                {
                    'flight_number': 'SU 100',
                    'airline_id': 1,
                    'amount': 1,
                },
                {
                    'flight_number': 'SU 101',
                    'airline_id': 1,
                    'amount': 2,
                },
                {
                    'flight_number': 'FV 100',
                    'airline_id': 2,
                    'amount': 2,
                },
            ]
            self._repository = ReviewStatRepository(
                conn=Mock()
            )
            self._repository.pre_cache()

    def test_get_none(self):
        UNKNOWN_AIRLINE_COMPANY_ID = 3
        result = self._repository.get(UNKNOWN_AIRLINE_COMPANY_ID, 'SU 100')
        assert result['total'] == 0
        assert result['airline_total'] == 0

    def test_get_none_flight(self):
        result = self._repository.get(1, 'UNKNOWN NUMBER')
        assert result['total'] == 0
        assert result['airline_total'] == 3

    def test_get_by_flight_number(self):
        result = self._repository.get(1, '101')
        assert result['total'] == 2
        assert result['airline_total'] == 3
