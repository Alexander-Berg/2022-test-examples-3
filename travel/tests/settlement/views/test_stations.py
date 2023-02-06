# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import json
from functools import partial

import mock
from django.test import Client
from hamcrest import has_entries, assert_that, contains_inanyorder

from common.data_api.baris.test_helpers import mock_baris_response
from common.models.geo import Country, StationMajority, StationType
from common.models.transport import TransportType
from common.tester.factories import create_settlement, create_station, create_thread
from common.tester.testcase import TestCase


create_thread = create_thread.mutate(__={'calculate_noderoute': True})


class TestSettlementStations(TestCase):
    def setUp(self):
        self.client = Client()
        settlement = create_settlement(country_id=Country.RUSSIA_ID)
        self.settlement_id = settlement.id

        self.st1 = create_station(settlement=settlement, id=1, t_type='plane', type_choices='tablo')

        self.st2 = create_station(id=9600213, t_type='plane', tablo_state='real', type_choices='tablo')
        settlement.station2settlement_set.create(station=self.st2)
        settlement.related_stations.create(station=self.st2)

        self.st3 = create_station(id=9600215, t_type='plane', tablo_state='real', type_choices='tablo')
        settlement.related_stations.create(station=self.st3)

        self.st4 = create_station(hidden=True, id=4, type_choices='')

        self.st5 = create_station(majority='not_in_tablo', id=5, type_choices='tablo')

        self.st6 = create_station(
            id=9600366, t_type='plane', title_ru='м. Метро', settlement=settlement,
            tablo_state='real', type_choices='tablo'
        )

        self.airport_with_aeroexpress = create_station(
            id=7, t_type='plane', has_aeroexpress=True, settlement=settlement, type_choices='tablo,aeroex'
        )

    def test_stations(self):
        baris_response = {
            'stations': [
                {'id': 9600213, 'cancelled': 0, 'delayed': 2},
                {'id': 9600366, 'cancelled': 3},
                {'id': 9600215, 'cancelled': 5, 'delayed': 4},
            ]
        }
        with mock_baris_response(baris_response):
            with mock.patch('travel.rasp.morda_backend.morda_backend.settlement.serialization.station.station_json',
                            side_effect=lambda station, status, lang: {'id': station.id, 'status': status}):
                response = self.client.get('/uk/settlement/{}/stations/'.format(self.settlement_id))

                assert response.status_code == 200

                data = json.loads(response.content)

                assert_that(data, has_entries({
                    'connected': contains_inanyorder(
                        has_entries({
                            'id': 1,
                            'status': has_entries({'cancelled': 0, 'late': 0})
                        }),
                        has_entries({
                            'id': 9600213,
                            'status': has_entries({'cancelled': 0, 'late': 2})
                        }),
                        has_entries({
                            'id': 9600366,
                            'status': has_entries({'cancelled': 3, 'late': 0})
                        }),
                        has_entries({
                            'id': 7,
                            'status': has_entries({'cancelled': 0, 'late': 0})
                        })
                    ),
                    'related': contains_inanyorder(
                        has_entries({
                            'id': 9600213,
                            'status': has_entries({'cancelled': 0, 'late': 2})
                        }),
                        has_entries({
                            'id': 9600215,
                            'status': has_entries({'cancelled': 5, 'late': 4})
                        }),
                    )
                }))

        with mock_baris_response(side_effect=Exception()):
            with mock.patch('travel.rasp.morda_backend.morda_backend.settlement.serialization.station.station_json',
                            side_effect=lambda station, status, lang: {'id': station.id, 'status': status}):
                response = self.client.get('/uk/settlement/{}/stations/'.format(self.settlement_id))

                assert response.status_code == 200


class TestTransportSettlementStations(TestCase):
    def setUp(self):
        self.client = Client()
        self.settlement = create_settlement(slug='settlement_slug', country_id=Country.RUSSIA_ID)
        create_train_station = partial(create_station, settlement=self.settlement, t_type='train')

        create_train_station(id=10, majority=StationMajority.MAIN_IN_CITY_ID, type_choices='train')
        create_train_station(id=11, majority=StationMajority.MAIN_IN_CITY_ID, type_choices='train')
        create_train_station(id=12, majority=StationMajority.MAIN_IN_CITY_ID, type_choices='suburban')
        create_train_station(id=13, majority=StationMajority.IN_TABLO_ID, type_choices='train')
        create_train_station(id=14, majority=StationMajority.IN_TABLO_ID, type_choices='train')
        create_train_station(id=15, majority=StationMajority.IN_TABLO_ID, type_choices='suburban')
        create_train_station(id=16, majority=StationMajority.NOT_IN_TABLO_ID, type_choices='train')

    def test_stations(self):
        with TransportType.objects.using_precache(), StationMajority.objects.using_precache(), \
                StationType.objects.using_precache():
            response = self.client.get('/uk/settlement/{}/stations/?t_type=train'.format(self.settlement.slug))

        assert response.status_code == 200

        data = json.loads(response.content)

        assert_that(data, has_entries({
            'connected': contains_inanyorder(
                has_entries({
                    'id': 10
                }),
                has_entries({
                    'id': 11
                }),
                has_entries({
                    'id': 13
                }),
                has_entries({
                    'id': 14
                }),
            ),
            'related': []
        }))

        with TransportType.objects.using_precache(), StationMajority.objects.using_precache(), \
                StationType.objects.using_precache():
            response = self.client.get('/uk/settlement/{}/stations/?t_type=suburban'.format(self.settlement.slug))

        assert response.status_code == 200

        data = json.loads(response.content)

        assert_that(data, has_entries({
            'connected': contains_inanyorder(
                has_entries({
                    'id': 12
                }),
                has_entries({
                    'id': 15
                }),
            ),
            'related': []
        }))
