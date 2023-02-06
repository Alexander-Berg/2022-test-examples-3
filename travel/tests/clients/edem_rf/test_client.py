# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from future import standard_library
standard_library.install_aliases()
import json
from datetime import date
from urllib.parse import parse_qs, urlsplit

import pytest
import httpretty
from django.conf import settings
from django.utils.http import urlunquote
from hamcrest import assert_that, has_entries
from rest_framework import status

from travel.rasp.library.python.common23.tester.factories import create_settlement
from travel.rasp.library.python.common23.tester.utils.replace_setting import replace_setting

from travel.rasp.blablacar.blablacar.clients.edem_rf.client import EdemRfClient
from travel.rasp.blablacar.blablacar.serialization import BlablacarQuery


pytestmark = [pytest.mark.dbuser]

EDEM_RF_URL = 'https://test_edem_rf.ru/'


def _make_query():
    city_1 = create_settlement(id=101, title='Город1')
    city_2 = create_settlement(id=102, title='Город2')

    return BlablacarQuery(
        date=date(2020, 4, 1),
        point_from=city_1,
        point_to=city_2,
        provider='edem_rf'
    )


EDEM_RF_PARAMS = {
    'fromCityName': 'Город1',
    'toCityName': 'Город2',
    'createdDate': '2020-04-01'
}


EDEM_RF_RESPONSE = json.dumps({
    'data': {
        'searchUrl': 'url',
        'routes': [
            {'duration': '10', 'cost': '200'},
            {'duration': '20', 'cost': '100'}
        ]
    }
})


@httpretty.activate
@replace_setting('BLABLACAR_MIN_DISTANCE_KM', 0)
def test_edem_rf_response():
    httpretty.register_uri(
        httpretty.GET, EDEM_RF_URL, params=EDEM_RF_PARAMS,
        body=EDEM_RF_RESPONSE, content_type='application/json'
    )

    query = _make_query()
    client = EdemRfClient(url=EDEM_RF_URL)
    response, cache_timeout = client.make_response_and_cache_timeout(query)

    assert cache_timeout == settings.EDEM_RF_SUCCESS_CACHE_TIMEOUT
    assert_that(response.data, has_entries({
        'tariff': has_entries({
            'duration': 10,
            'offersCount': 2,
            'price': {
                'currency': 'RUB',
                'value': 100
            },
            'title': 'Город1 — Город2'
        }),
        'banned': False,
        'querying': False
    }))

    url = response.data['tariff']['orderUrl']
    assert url.startswith('url')
    url_parts = urlsplit(urlunquote(url))
    assert_that(parse_qs(url_parts.query), has_entries({
        'utm_source': ['YANDEXRASP'],
        'utm_medium': ['API'],
        'utm_campaign': ['RU_YANDEXRASP_PSGR_TIMETABLE_none'],
        'comuto_cmkt': ['RU_YANDEXRASP_PSGR_TIMETABLE_none'],
    }))


@httpretty.activate
@replace_setting('BLABLACAR_MIN_DISTANCE_KM', 0)
def test_edem_rf_empty_response():
    httpretty.register_uri(
        httpretty.GET, EDEM_RF_URL, params=EDEM_RF_PARAMS,
        body='{"data":{"routes":[]}}', content_type='application/json'
    )

    query = _make_query()
    client = EdemRfClient(url=EDEM_RF_URL)
    response, cache_timeout = client.make_response_and_cache_timeout(query)

    assert cache_timeout == settings.EDEM_RF_SUCCESS_CACHE_TIMEOUT
    assert_that(response.data, has_entries({
        'tariff': has_entries({
            'duration': None,
            'offersCount': 0,
            'price': {
                'currency': 'RUB',
                'value': None
            },
            'orderUrl': None,
            'title': 'Город1 — Город2'
        }),
        'banned': False,
        'querying': False
    }))


@httpretty.activate
@replace_setting('BLABLACAR_MIN_DISTANCE_KM', 0)
def test_edem_rf_error_response():
    query = _make_query()

    httpretty.register_uri(
        httpretty.GET, EDEM_RF_URL, params=EDEM_RF_PARAMS,
        status=500
    )
    client = EdemRfClient(url=EDEM_RF_URL)
    response, cache_timeout = client.make_response_and_cache_timeout(query)

    assert cache_timeout == settings.EDEM_RF_ERROR_CACHE_TIMEOUT
    assert response.status == status.HTTP_502_BAD_GATEWAY
    assert response.data == {'error': 'Bad status of edem_rf response'}

    httpretty.register_uri(
        httpretty.GET, EDEM_RF_URL, params=EDEM_RF_PARAMS,
        body='{erunda}'
    )
    response, cache_timeout = client.make_response_and_cache_timeout(query)

    assert cache_timeout == settings.EDEM_RF_ERROR_CACHE_TIMEOUT
    assert response.status == status.HTTP_500_INTERNAL_SERVER_ERROR
    assert response.data == {'error': 'Unparseable edem_rf response'}
