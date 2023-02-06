# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import json

import pytest
from django.conf import settings
from django.test import Client

from common.dynamic_settings.default import conf
from common.models.geo import Country
from common.models.transport import TransportType
from common.tester.factories import create_thread, create_station, create_country


@pytest.mark.dbuser
def test_train_sales_limit_on_all_days_search():
    from_express_code = 'express_from'
    to_express_code = 'express_to'
    station_from = create_station(__=dict(codes={'express': from_express_code}))
    station_to = create_station(__=dict(codes={'express': to_express_code}))

    create_thread(
        t_type=TransportType.TRAIN_ID,
        schedule_v1=[
            [None, 0, station_from],
            [10, None, station_to],
        ],
        __={'calculate_noderoute': True}
    )

    response = Client().get('/ru/search/search/', {
        'pointFrom': station_from.point_key, 'pointTo': station_to.point_key, 'transportType': 'train'
    })
    result = json.loads(response.content)
    segments = result['result']['segments']
    assert len(segments) == 1
    assert segments[0]['salesLimitInDays'] == conf.TRAIN_ORDER_DEFAULT_DEPTH_OF_SALES


@pytest.mark.dbuser
def test_not_train():
    station_from = create_station()
    station_to = create_station()
    create_thread(
        t_type=TransportType.BUS_ID,
        schedule_v1=[
            [None, 0, station_from],
            [10, None, station_to],
        ],
        __={'calculate_noderoute': True}
    )

    response = Client().get('/ru/search/search/', {
        'pointFrom': station_from.point_key, 'pointTo': station_to.point_key, 'transportType': 'bus'
    })
    result = json.loads(response.content)
    segments = result['result']['segments']
    assert len(segments) == 1
    assert 'salesLimitInDays' not in segments[0]


@pytest.mark.dbuser
@pytest.mark.parametrize('stations_country_id, national_version, depth', [
    (Country.RUSSIA_ID, 'ru', conf.TRAIN_ORDER_DEFAULT_DEPTH_OF_SALES),
    (Country.RUSSIA_ID, 'ua', conf.TRAIN_ORDER_DEFAULT_DEPTH_OF_SALES),
    (Country.UKRAINE_ID, 'ru', conf.TRAIN_ORDER_DEFAULT_DEPTH_OF_SALES),
    (Country.UKRAINE_ID, 'ua', settings.UKRMINTRANS_TRAIN_DEFAULT_DEPTH_OF_SALES),
])
def test_train_sales_limit_on_all_days_search_ua_version(stations_country_id, national_version, depth):
    from_express_code = 'express_from'
    to_express_code = 'express_to'
    country = Country.objects.filter(id=stations_country_id).first() or create_country(id=stations_country_id)
    station_from = create_station(country=country, __=dict(codes={'express': from_express_code}))
    station_to = create_station(country=country, __=dict(codes={'express': to_express_code}))

    create_thread(
        t_type=TransportType.TRAIN_ID,
        schedule_v1=[
            [None, 0, station_from],
            [10, None, station_to],
        ],
        __={'calculate_noderoute': True}
    )

    response = Client().get('/ru/search/search/', {
        'pointFrom': station_from.point_key, 'pointTo': station_to.point_key,
        'national_version': national_version, 'transportType': 'train'
    })
    result = json.loads(response.content)
    segments = result['result']['segments']
    assert len(segments) == 1
    assert segments[0]['salesLimitInDays'] == depth
