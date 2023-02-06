# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from future import standard_library
standard_library.install_aliases()
import json
from datetime import date
from urllib.parse import parse_qs, urlsplit

import httpretty
import pytest
from django.conf import settings
from django.utils.http import urlunquote
from hamcrest import assert_that, has_entries
from rest_framework import status

from travel.rasp.library.python.common23.tester.factories import create_settlement

from travel.rasp.blablacar.blablacar.clients.blablacar_v3.client import BlablacarV3Client
from travel.rasp.blablacar.blablacar.serialization import BlablacarQuery


pytestmark = [pytest.mark.dbuser]

BLABLACAR_URL = 'https://test_blablacar_v3.ru/'
BLABLACAR_PARAMS = {

}
BLABLACAR_RESPONSE = {
    "link": "https://www.blablacar.ru/search?fc=56.102608,54.286739&tc=56.267227,54.931977&db=2021-06-02&de=2021-06-04",
    "trips": [
        {
            "price": {
                "amount": 100.0,
                "currency": "RUB",
            },
            "duration_in_seconds": 2400
        }
    ],
    "search_info": {
        "count": 14,
        "full_trip_count": 2
    }
}
EMPTY_BLABLACAR_RESPONSE = {
    "link": "https://www.blablacar.ru/search?fc=56.102608,54.286739&tc=56.267227,54.931977&db=2021-06-02&de=2021-06-04",
    "search_info": {
        "count": 0,
        "full_trip_count": 0
    },
    "trips": []
}


def _make_query():
    city_1 = create_settlement(id=101, title='Город1')
    city_2 = create_settlement(id=102, title='Город2')

    return BlablacarQuery(
        date=date(2021, 6, 2),
        point_from=city_1,
        point_to=city_2,
        provider='blablacar'
    )


@httpretty.activate
def test_blablacar_v3_response():
    httpretty.register_uri(
        httpretty.GET, BLABLACAR_URL, params=BLABLACAR_PARAMS,
        body=json.dumps(BLABLACAR_RESPONSE), content_type='application/json'
    )

    client = BlablacarV3Client(url=BLABLACAR_URL)
    response, cache_timeout = client.make_response_and_cache_timeout(_make_query())

    assert cache_timeout == settings.BLABLACAR_SUCCESS_CACHE_TIMEOUT
    assert_that(response.data, has_entries({
        'tariff': has_entries({
            'duration': 2400,
            'offersCount': 12,
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
    assert url.startswith('https://www.blablacar.ru/search')
    url_parts = urlsplit(urlunquote(url))
    assert_that(parse_qs(url_parts.query), has_entries({
        'utm_source': ['YANDEXRASP'],
        'utm_medium': ['API'],
        'utm_campaign': ['RU_YANDEXRASP_PSGR_TIMETABLE_none'],
        'comuto_cmkt': ['RU_YANDEXRASP_PSGR_TIMETABLE_none'],
    }))


@httpretty.activate
def test_blablacar_v3_empty_response():
    httpretty.register_uri(
        httpretty.GET, BLABLACAR_URL, params=BLABLACAR_PARAMS,
        body=json.dumps(EMPTY_BLABLACAR_RESPONSE), content_type='application/json'
    )

    client = BlablacarV3Client(url=BLABLACAR_URL)
    response, cache_timeout = client.make_response_and_cache_timeout(_make_query())

    assert cache_timeout == settings.BLABLACAR_SUCCESS_CACHE_TIMEOUT
    assert_that(response.data, has_entries({
        'error': 'Unknown direction in blablacar'
    }))


@httpretty.activate
def test_blablacar_v3_error_response():
    query = _make_query()
    httpretty.register_uri(
        httpretty.GET, BLABLACAR_URL, params=BLABLACAR_PARAMS, status=500
    )
    client = BlablacarV3Client(url=BLABLACAR_URL)
    response, cache_timeout = client.make_response_and_cache_timeout(query)

    assert cache_timeout == settings.BLABLACAR_ERROR_CACHE_TIMEOUT
    assert response.status == status.HTTP_502_BAD_GATEWAY
    assert response.data == {'error': 'Bad status'}

    httpretty.register_uri(
        httpretty.GET, BLABLACAR_URL, params=BLABLACAR_PARAMS, body='{erunda}'
    )
    response, cache_timeout = client.make_response_and_cache_timeout(query)

    assert cache_timeout == settings.BLABLACAR_ERROR_CACHE_TIMEOUT
    assert response.status == status.HTTP_500_INTERNAL_SERVER_ERROR
    assert response.data == {'error': 'Unparseable blablacar response'}
