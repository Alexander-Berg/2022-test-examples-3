# coding=utf-8
from __future__ import absolute_import

from travel.avia.library.python.avia_data.models import SimilarAirlines
from travel.avia.backend.repository.similar_airlines import SimilarAirlinesRepository
from travel.avia.library.python.tester.testcase import TestCase
from travel.avia.library.python.tester.factories import get_model_factory, create_company


similar_airlines_factory = get_model_factory(SimilarAirlines)


class TestSimilarAirlinesRepository(TestCase):
    def setUp(self):
        self._similar_airlines_repository = SimilarAirlinesRepository()

    def test_empty(self):
        expected = []
        self._similar_airlines_repository.pre_cache()
        actual_similar_airlines = self._similar_airlines_repository.get(1)

        assert actual_similar_airlines == expected

    def test_repository(self):
        company_id = 1
        expected = [{'company_id': 2, 'score': 0.5}]
        create_company(id=company_id)
        for r in expected:
            create_company(id=r['company_id'])
            similar_airlines_factory(
                airline_id=company_id,
                similar_airline_id=r['company_id'],
                score=r['score'],
            )

        self._similar_airlines_repository.pre_cache()
        actual_similar_airlines = self._similar_airlines_repository.get(company_id)

        assert actual_similar_airlines == expected
