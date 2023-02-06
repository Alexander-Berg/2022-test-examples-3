# coding=utf-8
from __future__ import absolute_import

from travel.avia.library.python.avia_data.models import FlightRating
from travel.avia.backend.repository.flight_rating import FlightRatingRepository
from travel.avia.library.python.tester.factories import get_model_factory
from travel.avia.library.python.tester.testcase import TestCase


def setup_default_rating():
    flight_number = 'AA 111'

    flight_rating_factory = get_model_factory(FlightRating)
    rating = flight_rating_factory(
        number=flight_number,
        bad_count=5,
        good_count=15,
        bad_percent=25,
        delayed_less_30=5,
        delayed_30_60=5,
        delayed_60_90=5,
        delayed_more_90=5,
        canceled=5,
        id=1,
        outrunning=10,
        scores=170,
        avg_scores=170 / 20.,
    )
    return flight_number.replace(' ', '-'), rating


class TestFlightRatingRepository(TestCase):
    def setUp(self):
        self.flight_number, self.rating = setup_default_rating()
        self._flight_rating_repository = FlightRatingRepository()

    def test_repository(self):
        expected = {
            'delayed3060': 5, 'delayedMore90': 5,
            'canceled': 5, 'goodCount': 15, 'scores': 170, 'delayed6090': 5,
            'delayedLess30': 5, 'avgScore': 8.5, 'badPercent': 25,
            'outrunning': 10, 'badCount': 5
        }

        self._flight_rating_repository.pre_cache()
        flight_rating = self._flight_rating_repository.get(self.flight_number)
        assert flight_rating == expected
