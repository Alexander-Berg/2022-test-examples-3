# coding=utf-8
from __future__ import absolute_import

from travel.avia.library.python.common.models.transport import TransportType
from travel.avia.library.python.avia_data.models import CompanyRating
from travel.avia.library.python.tester.factories import create_company, get_model_factory
from travel.avia.library.python.tester.testcase import TestCase

from travel.avia.backend.repository.airline_rating import AirlineRatingRepository


def setup_default_rating():
    company_id = 42

    airline = create_company(
        id=company_id,
        t_type_id=TransportType.PLANE_ID,
    )

    company_rating_factory = get_model_factory(CompanyRating)
    rating = company_rating_factory(
        company_id=company_id,
        bad_count=5,
        good_count=15,
        bad_percent=25,
        delayed_less_30=5,
        delayed_30_60=5,
        delayed_60_90=5,
        delayed_more_90=5,
        canceled=5,
        flight_count=6,
        id=1,
        outrunning=10,
        scores=170,
        avg_scores=170 / 20.,
    )
    return airline, rating


class TestAirlineRatingRepository(TestCase):
    def setUp(self):
        self.airline, self.rating = setup_default_rating()
        self._airline_rating_repository = AirlineRatingRepository()

    def test_repository(self):
        expected = {
            'delayed3060': 5, 'flightCount': 6, 'delayedMore90': 5,
            'canceled': 5, 'goodCount': 15, 'scores': 170, 'delayed6090': 5,
            'delayedLess30': 5, 'avgScore': 8.5, 'badPercent': 25,
            'outrunning': 10, 'badCount': 5
        }

        self._airline_rating_repository.pre_cache()
        airline_rating = self._airline_rating_repository.get(self.airline.id)
        assert airline_rating == expected
