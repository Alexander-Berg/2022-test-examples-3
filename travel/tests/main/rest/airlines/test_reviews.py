# -*- coding: utf-8 -*-
from __future__ import absolute_import

import ujson
from logging import Logger
from mock import Mock, patch
from typing import cast

from travel.avia.library.python.tester.testcase import TestCase

from travel.avia.backend.main.rest.airlines.reviews import FlightReviewsView
from travel.avia.backend.repository.review import ReviewStatRepository


class FlightReviewsViewTest(TestCase):
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

        self._view = FlightReviewsView(self._repository, logger=cast(Logger, Mock()))

    def test_view_null_flight(self):
        result = self._view._unsafe_process({
            'airline_id': 1,
            'flight_number': '103',
        })
        response = ujson.loads(result.response[0])

        assert response[u'status'] == u'ok'
        assert response[u'data']['total'] == 0
        assert response[u'data']['airline_total'] == 3

    def test_view_null(self):
        result = self._view._unsafe_process({
            'airline_id': 3,
            'flight_number': '100',
        })
        response = ujson.loads(result.response[0])

        assert response[u'status'] == u'ok'
        assert response[u'data']['total'] == 0
        assert response[u'data']['airline_total'] == 0

    def test_view(self):
        result = self._view._unsafe_process({
            'airline_id': 1,
            'flight_number': '101',
        })

        response = ujson.loads(result.response[0])

        assert response[u'status'] == u'ok'
        assert response[u'data']['total'] == 2
        assert response[u'data']['airline_total'] == 3
