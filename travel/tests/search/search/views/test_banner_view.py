# coding: utf8
from __future__ import unicode_literals

import json

import pytest
from django.test import Client
from hamcrest import assert_that, has_entries

from common.models.transport import TransportType
from common.tester.factories import create_thread, create_station, create_settlement, create_country
from common.tester.utils.replace_setting import replace_setting
from travel.rasp.morda_backend.morda_backend.middleware.set_exps_flags import EXPERIMENTS_HEADER
from travel.rasp.morda_backend.tests.search.search.helpers import mock_does_hotel_city_static_page_exist_request

pytestmark = [pytest.mark.dbuser]

REQUEST_HEADERS = {EXPERIMENTS_HEADER: json.dumps({'banner_hotels': 'enabled'})}


@replace_setting('ENABLE_HOTEL_BANNER', True)
def test_banner_view_settlement():
    mock_does_hotel_city_static_page_exist_request()

    country = create_country()
    settlement_from = create_settlement(
        id=101,
        title_ru='From',
        title_ru_locative='From in',
        slug='from',
        country=country,
        _geo_id=101
    )
    settlement_to = create_settlement(
        id=102,
        title_ru='To',
        title_ru_locative='To in',
        slug='to',
        country=country,
        _geo_id=102
    )

    station_from = create_station(
        id=101, settlement=settlement_from, title='Вокзал от',
        t_type=TransportType.TRAIN_ID, country=country, type_choices='train'
    )
    station_to = create_station(
        id=102, settlement=settlement_to, title='Вокзал до',
        t_type=TransportType.TRAIN_ID, country=country, type_choices='train'
    )
    create_thread(
        t_type=TransportType.TRAIN_ID,
        schedule_v1=[[None, 0, station_from], [10, None, station_to]],
        __={'calculate_noderoute': True}
    )

    query = {
        'pointFrom': 's101',
        'pointTo': 's102',
        'when': '2023-03-23',
        'transportType': 'train'
    }

    response = json.loads(Client().get('/{}/search/search/'.format('ru'), query, **REQUEST_HEADERS).content)

    assert_that(response['result']['bannerInfo'], has_entries({
        'bannerType': 'yaTravelHotels',
        'data': {
            'slug': 'to',
            'doesLandingExist': True,
            'region': 'To in',
            'geoId': 102
        }
    }))

    response = json.loads(Client().get('/{}/search/search/'.format('ru'), query, ).content)

    assert_that(response['result']['bannerInfo'], has_entries({
        'bannerType': 'advertising',
        'data': None
    }))


@replace_setting('ENABLE_HOTEL_BANNER', True)
def test_banner_view_outside_settlement():
    mock_does_hotel_city_static_page_exist_request()

    country = create_country()
    settlement_from = create_settlement(
        id=101,
        title_ru='From',
        title_ru_locative='From in',
        slug='from',
        country=country,
        _geo_id=101
    )

    station_from = create_station(
        id=101, settlement=settlement_from, title='Вокзал от',
        t_type=TransportType.TRAIN_ID, country=country, type_choices='train'
    )
    station_to = create_station(
        id=102, title='Вокзал до',
        t_type=TransportType.TRAIN_ID, country=country, type_choices='train'
    )
    create_thread(
        t_type=TransportType.TRAIN_ID,
        schedule_v1=[[None, 0, station_from], [10, None, station_to]],
        __={'calculate_noderoute': True}
    )

    query = {
        'pointFrom': 's101',
        'pointTo': 's102',
        'when': '2023-03-23',
        'transportType': 'train'
    }

    response = json.loads(Client().get('/{}/search/search/'.format('ru'), query, **REQUEST_HEADERS).content)

    assert_that(response['result']['bannerInfo'], has_entries({
        'bannerType': 'advertising',
        'data': None
    }))
