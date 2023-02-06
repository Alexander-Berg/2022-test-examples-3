# coding=utf-8
from __future__ import absolute_import

from operator import itemgetter

from travel.avia.library.python.avia_data.models import RelevantCityAirline, NationalVersion
from travel.avia.backend.repository.flights_to_city_airlines import FlightsToCityAirlinesRepository
from travel.avia.library.python.tester.testcase import TestCase
from travel.avia.library.python.tester.factories import get_model_factory, create_company, create_settlement

flights_to_city_airlines_factory = get_model_factory(RelevantCityAirline)


class TestSimilarAirlinesRepository(TestCase):
    SETTLEMENT_ID = 1

    def setUp(self):
        self.ru_id = NationalVersion.objects.get(code='ru').id
        self.flights_to_city_airlines_repository = FlightsToCityAirlinesRepository()

    def test_empty(self):
        expected = []
        self.flights_to_city_airlines_repository.pre_cache()
        actual = self.flights_to_city_airlines_repository.get(1, 'ru')
        self.assertItemsEqual(expected, actual)

    def precache_test_data(self, airline_scores):
        create_settlement(id=self.SETTLEMENT_ID)
        default = {
            'national_version__code': 'ru',
            'settlement_to_id': self.SETTLEMENT_ID,
        }
        expected = [
            dict(default, airline_id=airline_id, score=score)
            for airline_id, score in airline_scores.iteritems()
        ]
        for r in expected:
            create_company(id=r['airline_id'])
            flights_to_city_airlines_factory(
                settlement_to_id=self.SETTLEMENT_ID,
                national_version_id=self.ru_id,
                airline_id=r['airline_id'],
                score=r['score'],
            )

        self.flights_to_city_airlines_repository.pre_cache()
        return expected

    def test_repository_get(self):
        expected = self.precache_test_data({1: 1, 2: 2})
        actual = self.flights_to_city_airlines_repository.get(self.SETTLEMENT_ID, 'ru')
        self.assertItemsEqual(expected, actual)

    def test_empty_by_another_nv(self):
        _ = self.precache_test_data({1: 1, 2: 2})
        actual = self.flights_to_city_airlines_repository.get(self.SETTLEMENT_ID, 'com')
        self.assertItemsEqual([], actual)

    def test_repository_get_top_n(self):
        expected = self.precache_test_data({1: 1, 2: 2})
        expected = [max(expected, key=itemgetter('score'))]
        actual = self.flights_to_city_airlines_repository.get_top_n(self.SETTLEMENT_ID, 'ru', 1)
        self.assertItemsEqual(expected, actual)
