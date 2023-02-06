# -*- coding: utf-8 -*-
from __future__ import absolute_import

from mock import patch, Mock
from typing import cast

from travel.avia.library.python.common.models.transport import TransportType
from travel.avia.library.python.tester.testcase import TestCase

from travel.avia.backend.repository.station import StationRepository
from travel.avia.backend.repository.translations import TranslatedTitleRepository


class AirportViewTest(TestCase):
    def setUp(self):
        with patch.object(StationRepository, '_load_station_iata_codes') as load_station_iata_codes_mock, \
                patch.object(StationRepository, '_load_db_models') as load_models_mock:
            load_station_iata_codes_mock.return_value = {
                9600370: 'SVX',
                9600371: 'JFK',
            }
            load_models_mock.return_value = self._load_db_models()

            self._repository = StationRepository(
                translated_title_repository=cast(TranslatedTitleRepository, Mock())
            )
            self._repository.pre_cache()

    def _load_db_models(self):
        # type: () -> list
        a1 = {
            'new_L_title_id': 1,
            'new_L_popular_title_id': 2,
            'country_id': 225,
            'latitude': 56.750107,
            'longitude': 60.804833,
            'id': 9600370,
            'point_key': 's9600370',
            'region_id': 11162,
            'settlement_id': 54,
            'sirena_id': 'КЛЦ',
            'station_type_id': 9,
            'time_zone': 'Asia/Yekaterinburg',
            'type_choices': None,
            't_type_id': TransportType.PLANE_ID,
        }
        a2 = {
            'new_L_title_id': 3,
            'new_L_popular_title_id': 4,
            'country_id': 84,
            'latitude': 40.639722,
            'longitude': -73.778889,
            'id': 9600371,
            'point_key': 's9600371',
            'region_id': 21705,
            'settlement_id': 202,
            'sirena_id': None,
            'station_type_id': 9,
            'time_zone': 'America/New_York',
            'type_choices': None,
            't_type_id': TransportType.PLANE_ID,
        }

        return [a1, a2]
