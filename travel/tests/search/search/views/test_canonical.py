# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import json

import pytest
from django.test import Client
from hamcrest import has_entries, assert_that

from common.data_api.baris.test_helpers import mock_baris_response
from common.models.geo import StationMajority, Country
from common.models.transport import TransportType
from common.tester.factories import create_thread, create_station, create_settlement


pytestmark = pytest.mark.dbuser

create_thread = create_thread.mutate(__={'calculate_noderoute': True})


def test_canonical():
    station_from1 = create_station(settlement=create_settlement(slug='settlement_slug_from'), slug='slug_from')
    station_from2 = create_station(settlement=station_from1.settlement, slug='slug_from_2')
    station_to = create_station(settlement=create_settlement(slug='settlement_slug_to'), slug='slug_to')

    with mock_baris_response({}):
        response = json.loads(Client().get('/ru/search/search/', {
            'national_version': 'ru',
            'pointFrom': station_from1.point_key,
            'pointTo': station_to.point_key
        }).content)

        assert not response['result']['canonical']

        create_thread(t_type=TransportType.BUS_ID, schedule_v1=[
            [None, 0, station_from1],
            [10, None, station_to]
        ])

        response = json.loads(Client().get('/ru/search/search/', {
            'national_version': 'ru',
            'pointFrom': station_from1.point_key,
            'pointTo': station_to.point_key
        }).content)

        assert_that(response['result']['canonical'],
                    has_entries({'pointFrom': station_from1.slug,
                                 'pointTo': station_to.slug,
                                 'transportType': 'bus'}))

        create_thread(t_type=TransportType.SUBURBAN_ID, schedule_v1=[
            [None, 0, station_from1],
            [10, None, station_to]
        ])

        response = json.loads(Client().get('/ru/search/search/', {
            'national_version': 'ru',
            'pointFrom': station_from1.point_key,
            'pointTo': station_to.point_key
        }).content)

        assert_that(response['result']['canonical'],
                    has_entries({'pointFrom': station_from1.slug,
                                 'pointTo': station_to.slug,
                                 'transportType': None}))

        create_thread(t_type=TransportType.SUBURBAN_ID, schedule_v1=[
            [None, 0, station_from2],
            [3, 5, station_from1],
            [10, None, station_to]
        ])

        response = json.loads(Client().get('/ru/search/search/', {
            'national_version': 'ru',
            'pointFrom': station_from1.settlement.point_key,
            'pointTo': station_to.point_key
        }).content)

        assert_that(response['result']['canonical'],
                    has_entries({'pointFrom': station_from1.settlement.slug,
                                 'pointTo': station_to.slug,
                                 'transportType': None}))

        response = json.loads(Client().get('/ru/search/search/', {
            'national_version': 'ru',
            'pointFrom': station_from1.settlement.point_key,
            'pointTo': station_to.point_key,
            'transport_type': 'bus'
        }).content)

        assert_that(response['result']['canonical'],
                    has_entries({'pointFrom': station_from1.settlement.slug,
                                 'pointTo': station_to.slug,
                                 'transportType': 'bus'}))

        response = json.loads(Client().get('/ru/search/search/', {
            'national_version': 'ru',
            'pointFrom': station_from1.settlement.point_key,
            'pointTo': station_to.point_key,
            'transport_type': 'suburban'
        }).content)

        assert_that(response['result']['canonical'],
                    has_entries({'pointFrom': station_from1.settlement.slug,
                                 'pointTo': station_to.slug,
                                 'transportType': 'suburban'}))


def test_canonical_majority():
    station_from1 = create_station(settlement=create_settlement(slug='settlement_slug_from'), slug='slug_from',
                                   majority=StationMajority.IN_TABLO_ID, country=Country.RUSSIA_ID)
    station_from2 = create_station(settlement=station_from1.settlement, slug='slug_from_2',
                                   majority=StationMajority.MAIN_IN_CITY_ID, country=Country.RUSSIA_ID)
    station_to = create_station(settlement=create_settlement(slug='settlement_slug_to'), slug='slug_to',
                                country=Country.RUSSIA_ID)
    with mock_baris_response({}):
        response = json.loads(Client().get('/ru/search/search/', {
            'national_version': 'ru',
            'pointFrom': station_from1.point_key,
            'pointTo': station_to.point_key
        }).content)

        assert not response['result']['canonical']

        create_thread(t_type=TransportType.SUBURBAN_ID, schedule_v1=[
            [None, 0, station_from1],
            [10, 20, station_from2],
            [30, None, station_to]
        ])

        response = json.loads(Client().get('/ru/search/search/', {
            'national_version': 'ru',
            'pointFrom': station_from1.settlement.point_key,
            'pointTo': station_to.settlement.point_key
        }).content)

        assert_that(response['result']['canonical'],
                    has_entries({'pointFrom': station_from2.slug,
                                 'pointTo': station_to.slug,
                                 'transportType': 'suburban'}))


def test_canonical_train():
    settlement_from = create_settlement(slug='settlement_slug_from')
    settlement_to = create_settlement(slug='settlement_slug_to')
    station_from_1 = create_station(settlement=settlement_from, slug='slug_from',
                                    majority=StationMajority.IN_TABLO_ID, country=Country.RUSSIA_ID)
    station_from_2 = create_station(settlement=station_from_1.settlement, slug='slug_from_2',
                                    majority=StationMajority.MAIN_IN_CITY_ID, country=Country.RUSSIA_ID)
    station_to_1 = create_station(settlement=settlement_to, slug='slug_to', country=Country.RUSSIA_ID)
    station_to_2 = create_station(settlement=settlement_to, slug='slug_to_2', country=Country.RUSSIA_ID)

    def get_response():
        return json.loads(Client().get('/ru/search/search/', {
            'national_version': 'ru',
            'pointFrom': station_from_1.settlement.point_key,
            'pointTo': station_to_1.settlement.point_key
        }).content)

    with mock_baris_response({}):
        response = get_response()
        assert not response['result']['canonical']

        create_thread(t_type=TransportType.TRAIN_ID, schedule_v1=[
            [None, 0, station_from_1],
            [10, 20, station_from_2],
            [30, None, station_to_1]
        ])
        response = get_response()
        assert_that(response['result']['canonical'],
                    has_entries({
                        'pointFrom': settlement_from.slug,
                        'pointTo': settlement_to.slug,
                        'transportType': 'train'
                    }))

        station_from_2.title = settlement_to.title
        station_from_2.save()
        response = get_response()
        # Сужение pointFrom не рабоет, сузить можно только оба города
        assert_that(response['result']['canonical'],
                    has_entries({
                        'pointFrom': settlement_from.slug,
                        'pointTo': settlement_to.slug,
                        'transportType': 'train'
                    }))

        station_to_1.title = settlement_from.title
        station_to_1.save()
        response = get_response()
        # Сужение pointFrom и pointTo до одноименных станции
        assert_that(response['result']['canonical'],
                    has_entries({
                        'pointFrom': station_from_2.slug,
                        'pointTo': station_to_1.slug,
                        'transportType': 'train'
                    }))

        create_thread(t_type=TransportType.TRAIN_ID, schedule_v1=[
            [None, 0, station_from_2],
            [30, None, station_to_2]
        ])
        response = get_response()
        # Сужение не работает, стало две станции прибытия
        assert_that(response['result']['canonical'],
                    has_entries({
                        'pointFrom': settlement_from.slug,
                        'pointTo': settlement_to.slug,
                        'transportType': 'train'
                    }))
