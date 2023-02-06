# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import httpretty
import pytest
from django.http import QueryDict

from common.tester.factories import create_station, create_settlement
from common.tester.utils.replace_setting import replace_setting
from travel.rasp.morda_backend.morda_backend.search.search.data_layer.banner import get_hotels_banner_info
from travel.rasp.morda_backend.morda_backend.search.search.serialization.request_serialization import ContextQuerySchema
from travel.rasp.morda_backend.tests.search.search.helpers import mock_does_hotel_city_static_page_exist_request


pytestmark = [pytest.mark.dbuser]


@replace_setting('ENABLE_HOTEL_BANNER', True)
@httpretty.activate
def test_banner_settlement_with_landing():
    mock_does_hotel_city_static_page_exist_request()

    create_settlement(
        id=101,
        slug='wolf',
        title_ru='Wolf',
        title_ru_locative='Wolf in',
        _geo_id=101
    )
    create_settlement(
        id=102,
        slug='rabbit',
        title_ru='Rabbit',
        title_ru_locative='Rabbit in',
        _geo_id=102
    )

    query_dict = QueryDict(mutable=True)
    query_dict.update({
        'pointFrom': 'c101',
        'pointTo': 'c102'
    })
    context, _ = ContextQuerySchema().load(query_dict)

    result = get_hotels_banner_info(context)
    assert result == {
        'banner_type': 'yaTravelHotels',
        'data': {
            'slug': 'rabbit',
            'does_landing_exist': True,
            'region': 'Rabbit in',
            'geo_id': 102
        }
    }


@replace_setting('ENABLE_HOTEL_BANNER', True)
@httpretty.activate
def test_banner_settlement_without_landing():
    hotel_city_static_page_response = {'exists': False}
    mock_does_hotel_city_static_page_exist_request(body=hotel_city_static_page_response)

    create_settlement(
        id=101,
        slug='wolf',
        title_ru='Wolf',
        title_ru_locative='Wolf in',
        _geo_id=101
    )
    create_settlement(
        id=102,
        slug='rabbit',
        title_ru='Rabbit',
        title_ru_locative='Rabbit in',
        _geo_id=102
    )

    query_dict = QueryDict(mutable=True)
    query_dict.update({
        'pointFrom': 'c101',
        'pointTo': 'c102'
    })
    context, _ = ContextQuerySchema().load(query_dict)

    result = get_hotels_banner_info(context)
    assert result == {
        'banner_type': 'yaTravelHotels',
        'data': {
            'slug': 'rabbit',
            'does_landing_exist': False,
            'region': 'Rabbit in',
            'geo_id': 102
        }
    }


@replace_setting('ENABLE_HOTEL_BANNER', True)
@httpretty.activate
def test_banner_settlement_with_http500():
    mock_does_hotel_city_static_page_exist_request(status=500)

    create_settlement(
        id=101,
        slug='wolf',
        title_ru='Wolf',
        title_ru_locative='Wolf in',
        _geo_id=101
    )
    create_settlement(
        id=102,
        slug='rabbit',
        title_ru='Rabbit',
        title_ru_locative='Rabbit in',
        _geo_id=102
    )

    query_dict = QueryDict(mutable=True)
    query_dict.update({
        'pointFrom': 'c101',
        'pointTo': 'c102'
    })
    context, _ = ContextQuerySchema().load(query_dict)

    result = get_hotels_banner_info(context)
    assert result == {
        'banner_type': 'yaTravelHotels',
        'data': {
            'slug': 'rabbit',
            'does_landing_exist': False,
            'region': 'Rabbit in',
            'geo_id': 102
        }
    }


@replace_setting('ENABLE_HOTEL_BANNER', True)
@httpretty.activate
def test_banner_station_inside_settlement():
    mock_does_hotel_city_static_page_exist_request()

    create_station(id=101, slug='wolf-station', settlement={
        'id': 101,
        'slug': 'wolf-city',
        'title_ru': 'Wolf-city',
        'title_ru_locative': 'Wolf-city in',
        '_geo_id': 101
    })
    create_station(id=102, slug='rabbit', settlement={
        'id': 102,
        'slug': 'rabbit-city',
        'title_ru': 'Rabbit-city',
        'title_ru_locative': 'Rabbit-city in',
        '_geo_id': 102
    })

    query_dict = QueryDict(mutable=True)
    query_dict.update({
        'pointFrom': 's101',
        'pointTo': 's102'
    })
    context, _ = ContextQuerySchema().load(query_dict)

    result = get_hotels_banner_info(context)
    assert result == {
        'banner_type': 'yaTravelHotels',
        'data': {
            'slug': 'rabbit-city',
            'does_landing_exist': True,
            'region': 'Rabbit-city in',
            'geo_id': 102
        }
    }


@replace_setting('ENABLE_HOTEL_BANNER', True)
@httpretty.activate
def test_banner_station_outside_settlement():
    mock_does_hotel_city_static_page_exist_request()

    create_station(id=101, slug='wolf-station', settlement={
        'id': 101,
        'slug': 'wolf-city',
        'title_ru': 'Wolf-city',
        'title_ru_locative': 'Wolf-city in',
        '_geo_id': 101
    })
    create_station(id=102, slug='rabbit')

    query_dict = QueryDict(mutable=True)
    query_dict.update({
        'pointFrom': 's101',
        'pointTo': 's102'
    })
    context, _ = ContextQuerySchema().load(query_dict)

    result = get_hotels_banner_info(context)
    assert result is None
