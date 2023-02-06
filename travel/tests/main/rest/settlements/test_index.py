# -*- coding: utf-8 -*-
from __future__ import absolute_import

import ujson
from logging import Logger
from mock import Mock, patch
from typing import cast

from travel.avia.library.python.tester.testcase import TestCase

from travel.avia.library.python.common.utils import environment

from travel.avia.backend.main.rest.settlements.list import SettlementsListView, SettlementsListForm
from travel.avia.backend.repository.settlement import SettlementRepository
from travel.avia.backend.repository.translations import TranslatedTitleRepository


class SettlementsListViewTest(TestCase):
    def setUp(self):
        settlements = [
            {
                'id': 1,
                'new_L_title_id': 1,
                '_geo_id': 1,
                'time_zone': 'Asia/Yekaterinburg',
                'iata': 'SVX',
                'sirena_id': None,
                'country_id': 1,
                'region_id': 1,
                '_disputed_territory': None,
                'majority_id': None,
                'latitude': None,
                'longitude': None,
            },
            {
                'id': 2,
                'new_L_title_id': 2,
                '_geo_id': 2,
                'time_zone': 'Europe/Moscow',
                'iata': 'SVO',
                'sirena_id': None,
                'country_id': 1,
                'region_id': 2,
                '_disputed_territory': None,
                'majority_id': None,
                'latitude': None,
                'longitude': None,
            },
            {
                'id': 3,
                'new_L_title_id': 3,
                '_geo_id': 3,
                'time_zone': 'Europe/Moscow',
                'iata': 'LED',
                'sirena_id': None,
                'country_id': 1,
                'region_id': 3,
                '_disputed_territory': None,
                'majority_id': None,
                'latitude': None,
                'longitude': None,
            },
        ]

        with patch.object(SettlementRepository, '_load_db_models') as load_db_models_mock:
            load_db_models_mock.return_value = settlements
            self._repository = SettlementRepository(
                translated_title_repository=cast(TranslatedTitleRepository, Mock()),
                environment=environment,
            )
            self._repository.pre_cache()

        self._view = SettlementsListView(
            form=SettlementsListForm(),
            repository=self._repository,
            logger=cast(Logger, Mock()),
        )

    def test_list_all(self):
        result = self._view._unsafe_process({
            'id': None,
        })
        response = ujson.loads(result.response[0])

        assert response['status'] == u'ok'
        assert len(response['data']) == 3
        for d in response['data']:
            assert d['iata'] in ('SVX', 'SVO', 'LED')

    def test_list_one(self):
        result = self._view._unsafe_process({
            'id': '2',
        })
        response = ujson.loads(result.response[0])

        assert response['status'] == u'ok'
        assert len(response['data']) == 1
        assert response['data'][0]['iata'] == 'SVO'

    def test_list_two(self):
        result = self._view._unsafe_process({
            'id': '2,1',
        })
        response = ujson.loads(result.response[0])

        assert response['status'] == u'ok'
        assert len(response['data']) == 2
        for d in response['data']:
            assert d['iata'] in ('SVX', 'SVO')
