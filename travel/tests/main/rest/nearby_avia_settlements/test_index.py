# -*- coding: utf-8 -*-
from __future__ import unicode_literals, absolute_import

import ujson
from typing import cast, List  # noqa
from logging import Logger
from mock import patch, Mock

from travel.avia.library.python.tester.testcase import TestCase
from travel.avia.library.python.common.utils import environment

from travel.avia.backend.main.rest.nearby_avia_settlements.index import (
    NearbyAviaSettlementsView, NearbyAviaSettlementsForm,
)
from travel.avia.backend.repository.settlement import SettlementRepository, SettlementGeoIndex, SettlementModel
from travel.avia.backend.repository.translations import TranslatedTitleRepository


class NearbyAviaSettlementsTest(TestCase):
    def setUp(self):
        self._fake_translated_repository = cast(TranslatedTitleRepository, Mock())
        self._fake_translated_repository.get = Mock(return_value='TEST')
        self._fake_translated_repository.get_genitive = Mock(return_value='TEST')
        self._fake_translated_repository.get_locative = Mock(return_value='TEST')
        self._fake_translated_repository.get_accusative = Mock(return_value='TEST')
        with patch.object(SettlementRepository, '_load_db_models') as settlement_load_models_mock:
            settlement_load_models_mock.return_value = self._get_settlements()
            settlement_repository = SettlementRepository(Mock(), environment)
            settlement_repository.pre_cache()

        self._settlement_geo_index = SettlementGeoIndex(settlement_repository)
        self._view = NearbyAviaSettlementsView(
            form=NearbyAviaSettlementsForm(),
            settlement_repository=settlement_repository,
            settlement_geo_index=self._settlement_geo_index,
            logger=cast(Logger, Mock()),
        )

    def test_near_geo_id(self):
        self.run_test('geo_id', 2130)

    def test_near_settlement_id(self):
        self.run_test('settlement_id', 213)

    def run_test(self, id_type, id_value):
        with patch.object(self._settlement_geo_index, 'get_nearest_with_distance') as settlement_geo_index_mock:
            settlement_geo_index_mock.return_value = (
                (self._create_settlement_model(s), distance)
                for s, distance in
                [
                    (
                        {
                            'sirena': 'КЛГ',
                            'pk': 6,
                            'geo_id': 6,
                        },
                        160.2019617714,
                    ),
                    (
                        {
                            'sirena': 'МКО',
                            'pk': 22215,
                            'geo_id': 144091,
                        },
                        226.4677154331
                    ),
                    (
                        {
                            'iata': 'IWA',
                            'sirena': 'ИВВ',
                            'pk': 5,
                            'geo_id': 5,
                        }, 248.6689926958),
                    (
                        {
                            'sirena': 'ЯРЛ',
                            'pk': 16,
                            'geo_id': 16,
                        },
                        250.2603290679,
                    ),
                ]
            )

            result = self._view._unsafe_process({
                id_type: id_value,
                'radius': 251,
            })
            response = ujson.loads(result.response[0])

            assert len(response['data']) == 4
            self.assertSequenceEqual(
                response['data'],
                [
                    {
                        'urlTitle': 'TEST',
                        'iata': '',
                        'sirena': 'КЛГ',
                        'phraseIn': 'TEST',
                        'id': 6,
                        'distance': 160.2019617714,
                        'countryId': 225,
                        'title': 'TEST',
                        'geoId': 6,
                        'phraseFrom': 'TEST',
                        'phraseTo': 'TEST',
                        'latitude': 1,
                        'longitude': 1,
                    },
                    {
                        'urlTitle': 'TEST',
                        'iata': '',
                        'sirena': 'МКО',
                        'phraseIn': 'TEST',
                        'id': 22215,
                        'distance': 226.4677154331,
                        'countryId': 225,
                        'title': 'TEST',
                        'geoId': 144091,
                        'phraseFrom': 'TEST',
                        'phraseTo': 'TEST',
                        'latitude': 1,
                        'longitude': 1,
                    },
                    {
                        'urlTitle': 'TEST',
                        'iata': 'IWA',
                        'sirena': 'ИВВ',
                        'phraseIn': 'TEST',
                        'id': 5,
                        'distance': 248.6689926958,
                        'countryId': 225,
                        'title': 'TEST',
                        'geoId': 5,
                        'phraseFrom': 'TEST',
                        'phraseTo': 'TEST',
                        'latitude': 1,
                        'longitude': 1,
                    },
                    {
                        'urlTitle': 'TEST',
                        'iata': '',
                        'sirena': 'ЯРЛ',
                        'phraseIn': 'TEST',
                        'id': 16,
                        'distance': 250.2603290679,
                        'countryId': 225,
                        'title': 'TEST',
                        'geoId': 16,
                        'phraseFrom': 'TEST',
                        'phraseTo': 'TEST',
                        'latitude': 1,
                        'longitude': 1,
                    }
                ]
            )

    def _get_settlements(self):
        # type: () -> List[dict]
        return [
            self._create_settlement_db_model({
                'id': 213,
                '_geo_id': 2130,
                'iata': 'MOW',
                'sirena_id': 'МОВ',
            }),
        ]

    @staticmethod
    def _create_settlement_db_model(data):
        # type: (dict) -> dict
        s = {
            'id': 1,
            '_geo_id': 10,
            'new_L_title_id': 1,
            'time_zone': 'Europe/Moscow',
            'country_id': 225,
            'region_id': 1,
            'sirena_id': 'СИР',
            'iata': '',
            '_disputed_territory': False,
            'majority_id': 1,
            'latitude': 1,
            'longitude': 1,
        }
        s.update(data)

        return s

    def _create_settlement_model(self, data):
        # type: (dict) -> SettlementModel
        s = dict(
            title_id=1,
            iata='',
            sirena='',
            country_id=225,
            region_id=7,
            is_disputed_territory=False,
            majority_id=1,
            pytz=None,
            utcoffset=1,
            latitude=1,
            longitude=1,
        )
        s.update(data)
        return SettlementModel(
            translated_title_repository=self._fake_translated_repository,
            **s
        )
