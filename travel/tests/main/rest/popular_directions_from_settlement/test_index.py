# -*- coding: utf-8 -*-
from __future__ import unicode_literals, absolute_import

import ujson
from mock import patch, Mock
from typing import cast, List  # noqa
from logging import Logger

from travel.avia.library.python.tester.testcase import TestCase

from travel.avia.library.python.common.utils import environment

from travel.avia.backend.main.rest.popular_directions_from_settlement.index import (
    PopularDirectionsFromSettlementView, PopularDirectionsFromSettlementForm, PopularDirectionFinder
)
from travel.avia.backend.main.services.price_index import PriceIndex
from travel.avia.backend.repository.settlement import SettlementRepository


class PopularDirectionsFromSettlementTest(TestCase):
    def setUp(self):
        with patch.object(SettlementRepository, '_load_db_models') as settlement_load_models_mock:
            settlement_load_models_mock.return_value = self._get_settlements()
            settlement_repository = SettlementRepository(Mock(), environment)
            settlement_repository.pre_cache()

        self._view = PopularDirectionsFromSettlementView(
            form=PopularDirectionsFromSettlementForm(),
            settlement_repository=settlement_repository,
            price_index=PriceIndex(None),
            logger=cast(Logger, Mock()),
        )

    @patch.object(PopularDirectionFinder, 'get_top_direction_ids')
    def test_svx_mow(self, get_top_direction_ids_mock):
        get_top_direction_ids_mock.return_value = [
            239,
            2,
            146,
            1107,
            35,
        ]
        with patch.object(PriceIndex, 'search') as price_index_mock:
            price_index_mock.return_value = [
                {
                    "to_id": 1107,
                    "forward_date": "2020-04-06",
                    "min_price": {
                        "currency": "RUR",
                        "value": 6501
                    },
                    "from_id": 54
                },
                {
                    "to_id": 239,
                    "forward_date": "2020-04-11",
                    "min_price": {
                        "currency": "RUR",
                        "value": 2999
                    },
                    "from_id": 54
                },
                {
                    "to_id": 146,
                    "forward_date": "2020-04-20",
                    "min_price": {
                        "currency": "RUR",
                        "value": 3930
                    },
                    "from_id": 54
                },
                {
                    "to_id": 2,
                    "forward_date": "2020-04-10",
                    "min_price": {
                        "currency": "RUR",
                        "value": 2181
                    },
                    "from_id": 54
                },
                {
                    "to_id": 35,
                    "forward_date": "2020-04-18",
                    "min_price": {
                        "currency": "RUR",
                        "value": 3085
                    },
                    "from_id": 54
                }
            ]

            result = self._view._unsafe_process({
                'from_settlement_id': 54,
                'to_settlement_id': 213,
                'national_version': 'ru',
            })
            response = ujson.loads(result.response[0])

            assert len(response['data']) == 5
            self.assertSequenceEqual(
                response['data'],
                [
                    {
                        "to_id": 239,
                        "forward_date": "2020-04-11",
                        "min_price": {
                            "currency": "RUR",
                            "value": 2999
                        },
                        "from_id": 54
                    },
                    {
                        "to_id": 2,
                        "forward_date": "2020-04-10",
                        "min_price": {
                            "currency": "RUR",
                            "value": 2181
                        },
                        "from_id": 54
                    },
                    {
                        "to_id": 146,
                        "forward_date": "2020-04-20",
                        "min_price": {
                            "currency": "RUR",
                            "value": 3930
                        },
                        "from_id": 54
                    },
                    {
                        "to_id": 1107,
                        "forward_date": "2020-04-06",
                        "min_price": {
                            "currency": "RUR",
                            "value": 6501
                        },
                        "from_id": 54
                    },
                    {
                        "to_id": 35,
                        "forward_date": "2020-04-18",
                        "min_price": {
                            "currency": "RUR",
                            "value": 3085
                        },
                        "from_id": 54
                    }
                ]
            )

    def _get_settlements(self):
        # type: () -> List[dict]
        return [
            self._create_settlement({
                'id': 54,
                '_geo_id': 3430,
                'iata': 'SVX',
                'sirena_id': 'ЕКБ',
                'majority_id': 2,
                'region_id': 66,
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
            'country_id': 225,
            'region_id': 1,
            'sirena_id': 'СИР',
            'iata': '',
            '_disputed_territory': False,
            'majority_id': 1,
            'latitude': 0,
            'longitude': 0,
        }
        s.update(data)

        return s
