# -*- coding: utf-8 -*-
from __future__ import absolute_import

import ujson
from mock import patch, Mock
from typing import cast, List
from logging import Logger

from travel.avia.library.python.common.utils import environment
from travel.avia.library.python.tester.testcase import TestCase

from travel.avia.backend.main.rest.direction.index import DirectionIndexView, DirectionIndexForm
from travel.avia.backend.repository.direction import DirectionRepository
from travel.avia.backend.repository.settlement import SettlementRepository


class DirectionIndexViewTest(TestCase):
    def setUp(self):
        with patch.object(DirectionRepository, '_load_models') as direction_load_models_mock:
            with patch.object(SettlementRepository, '_load_db_models') as settlement_load_models_mock:
                direction_load_models_mock.return_value = self._get_directions()
                settlement_load_models_mock.return_value = self._get_settlements()
                direction_repository = DirectionRepository()
                direction_repository.pre_cache()
                settlement_repository = SettlementRepository(Mock(), environment)
                settlement_repository.pre_cache()
                self._view = DirectionIndexView(
                    form=DirectionIndexForm(settlement_repository),
                    repository_direction=direction_repository,
                    logger=cast(Logger, Mock()),
                )

    def test_get_all(self):
        result = self._view._unsafe_process({
            'national_version': 'ru',
        })
        response = ujson.loads(result.response[0])

        assert response[u'status'] == u'ok'
        assert len(response[u'data']) == 3
        self.assertSequenceEqual(
            response[u'data'],
            sorted(response[u'data'], key=lambda x: x['popularity'], reverse=True)
        )

        for direction in response[u'data']:
            assert direction['national_version'] == 'ru'

    def test_get_all_with_limit(self):
        result = self._view._unsafe_process({
            'national_version': 'ru',
            'limit': 2,
        })
        response = ujson.loads(result.response[0])

        assert response[u'status'] == u'ok'
        assert len(response[u'data']) == 2
        self.assertSequenceEqual(
            response[u'data'],
            sorted(response[u'data'], key=lambda x: x['popularity'], reverse=True)
        )

        for direction in response[u'data']:
            assert direction['national_version'] == 'ru'

    def test_get_from(self):
        result = self._view._unsafe_process({
            'national_version': 'ru',
            'departure_settlement_id': 1,
        })
        response = ujson.loads(result.response[0])

        assert response[u'status'] == u'ok'
        assert len(response[u'data']) == 2
        self.assertSequenceEqual(
            response[u'data'],
            sorted(response[u'data'], key=lambda x: x['popularity'], reverse=True)
        )

        for direction in response[u'data']:
            assert direction['national_version'] == 'ru'
            assert direction['departure_settlement_id'] == 1

    def test_get_from_with_limit(self):
        result = self._view._unsafe_process({
            'national_version': 'ru',
            'departure_settlement_id': 1,
            'limit': 1,
        })
        response = ujson.loads(result.response[0])

        assert response[u'status'] == u'ok'
        assert len(response[u'data']) == 1

        for direction in response[u'data']:
            assert direction['national_version'] == 'ru'
            assert direction['departure_settlement_id'] == 1

    def test_get_to(self):
        result = self._view._unsafe_process({
            'national_version': 'ru',
            'arrival_settlement_id': 3,
        })
        response = ujson.loads(result.response[0])

        assert response[u'status'] == u'ok'
        assert len(response[u'data']) == 2
        self.assertSequenceEqual(
            response[u'data'],
            sorted(response[u'data'], key=lambda x: x['popularity'], reverse=True)
        )

        for direction in response[u'data']:
            assert direction['national_version'] == 'ru'
            assert direction['arrival_settlement_id'] == 3

    def test_get_to_with_limit(self):
        result = self._view._unsafe_process({
            'national_version': 'ru',
            'arrival_settlement_id': 3,
            'limit': 1,
        })
        response = ujson.loads(result.response[0])

        assert response[u'status'] == u'ok'
        assert len(response[u'data']) == 1

        for direction in response[u'data']:
            assert direction['national_version'] == 'ru'
            assert direction['arrival_settlement_id'] == 3

    def test_get_from_by_geo_id(self):
        result = self._view._unsafe_process({
            'national_version': 'ru',
            'departure_settlement_geo_id': 10,
        })
        response = ujson.loads(result.response[0])

        assert response[u'status'] == u'ok'
        assert len(response[u'data']) == 2
        self.assertSequenceEqual(
            response[u'data'],
            sorted(response[u'data'], key=lambda x: x['popularity'], reverse=True)
        )

        for direction in response[u'data']:
            assert direction['national_version'] == 'ru'
            assert direction['departure_settlement_id'] == 1

    def test_get_to_by_geo_id(self):
        result = self._view._unsafe_process({
            'national_version': 'ru',
            'arrival_settlement_geo_id': 30,
        })
        response = ujson.loads(result.response[0])

        assert response[u'status'] == u'ok'
        assert len(response[u'data']) == 2
        self.assertSequenceEqual(
            response[u'data'],
            sorted(response[u'data'], key=lambda x: x['popularity'], reverse=True)
        )

        for direction in response[u'data']:
            assert direction['national_version'] == 'ru'
            assert direction['arrival_settlement_id'] == 3

    def test_to_invalid_by_geo_id(self):
        result = self._view._unsafe_process({
            'national_version': 'ru',
            'arrival_settlement_geo_id': 1,
        })

        assert result.status_code == 400

        response = ujson.loads(result.response[0])

        assert response[u'status'] == u'error'
        assert response[u'data'] == {u'_schema': [u'Unknown settlement with geo_id: 1']}

    def test_from_invalid_by_geo_id(self):
        result = self._view._unsafe_process({
            'national_version': 'ru',
            'departure_settlement_geo_id': 1,
        })

        assert result.status_code == 400

        response = ujson.loads(result.response[0])

        assert response[u'status'] == u'error'
        assert response[u'data'] == {u'_schema': [u'Unknown settlement with geo_id: 1']}

    @staticmethod
    def _get_directions():
        # type: () -> List[dict]
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

    def _get_settlements(self):
        # type: () -> List[dict]
        return [
            self._create_settlement({
                'id': 1,
                '_geo_id': 10,
            }),
            self._create_settlement({
                'id': 2,
                '_geo_id': 20,
            }),
            self._create_settlement({
                'id': 3,
                '_geo_id': 30,
            }),
        ]

    @staticmethod
    def _create_settlement(data):
        # type: (dict) -> dict
        s = {
            'id': 1,
            '_geo_id': 10,
            'new_L_title_id': 1,
            'time_zone': 'Europe/Moscow',
            'iata': 'MOW',
            'sirena_id': u'МОС',
            'country_id': 1,
            'region_id': 1,
            '_disputed_territory': False,
            'majority_id': 1,
            'latitude': 0,
            'longitude': 0,
        }
        s.update(data)

        return s
