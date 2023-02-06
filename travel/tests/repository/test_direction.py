from __future__ import absolute_import

from operator import attrgetter
from mock import patch

from travel.avia.library.python.tester.testcase import TestCase

from travel.avia.backend.repository.direction import DirectionRepository


class TestDirectionRepository(TestCase):
    def setUp(self):
        with patch.object(DirectionRepository, '_load_models') as load_models_mock:
            load_models_mock.return_value = self._get_models()
            self._repository = DirectionRepository()
            self._repository.pre_cache()

    def test_get_all(self):
        directions = self._repository.get_all('ru')
        self.assertSequenceEqual(directions, sorted(directions, key=attrgetter('popularity'), reverse=True))
        assert len(directions) == 3

        for direction in directions:
            assert direction.national_version == 'ru'

        directions = self._repository.get_all()
        assert len(directions) == 6

    def test_get_from_settlement(self):
        directions = self._repository.get_from_settlement('ru', 1)
        self.assertSequenceEqual(directions, sorted(directions, key=attrgetter('popularity'), reverse=True))
        assert len(directions) == 2

        for direction in directions:
            assert direction.national_version == 'ru'
            assert direction.departure_settlement_id == 1

    def test_get_to_settlement(self):
        directions = self._repository.get_to_settlement('ru', 3)
        self.assertSequenceEqual(directions, sorted(directions, key=attrgetter('popularity'), reverse=True))
        assert len(directions) == 2

        for direction in directions:
            assert direction.national_version == 'ru'
            assert direction.arrival_settlement_id == 3

    def _get_models(self):
        # type: () -> list
        return [
            {
                'departure_settlement_id': 1,
                'arrival_settlement_id': 2,
                'popularity': 10,
                'national_version': 'ru',
                'direct_flights': 1,
                'connecting_flights': 2,
            },
            {
                'departure_settlement_id': 1,
                'arrival_settlement_id': 3,
                'popularity': 9,
                'national_version': 'ru',
                'direct_flights': 1,
                'connecting_flights': 2,
            },
            {
                'departure_settlement_id': 2,
                'arrival_settlement_id': 3,
                'popularity': 8,
                'national_version': 'ru',
                'direct_flights': 1,
                'connecting_flights': 2,
            },
            {
                'departure_settlement_id': 10,
                'arrival_settlement_id': 11,
                'popularity': 7,
                'national_version': 'com',
                'direct_flights': 1,
                'connecting_flights': 2,
            },
            {
                'departure_settlement_id': 10,
                'arrival_settlement_id': 12,
                'popularity': 6,
                'national_version': 'com',
                'direct_flights': 1,
                'connecting_flights': 2,
            },
            {
                'departure_settlement_id': 12,
                'arrival_settlement_id': 10,
                'popularity': 5,
                'national_version': 'com',
                'direct_flights': 1,
                'connecting_flights': 2,
            },
        ]
