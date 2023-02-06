# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from future import standard_library
standard_library.install_aliases()
from past.builtins import basestring
from datetime import date
from urllib.parse import parse_qs, urlsplit

import pytest
import requests
import httpretty
from django.core.cache.backends.locmem import LocMemCache
from django.test import Client
from django.utils.http import urlunquote
from hamcrest import assert_that, instance_of, has_entries
from mock import patch
from rest_framework import status

from travel.rasp.library.python.common23.models.currency.price import Price
from travel.rasp.library.python.common23.models.core.geo.country import Country
from travel.rasp.library.python.common23.models.core.geo.region import Region
from travel.rasp.library.python.common23.tester.factories import create_country, create_region, create_settlement, create_station
from travel.rasp.library.python.common23.tester.utils.datetime import replace_now
from travel.rasp.library.python.common23.tester.utils.replace_dynamic_setting import replace_dynamic_setting
from travel.rasp.library.python.common23.tester.utils.replace_setting import replace_setting
from travel.rasp.library.python.common23.models.core.geo.title_generator import DASH

from travel.rasp.blablacar.blablacar.clients.blablacar_v3.client import BLABLACAR_URL
from travel.rasp.blablacar.blablacar.clients.blablacar_v3.serialization import (
    BlablacarV3Response, BlablacarV3ResponseSchema, Trip, SearchInfo
)
from travel.rasp.blablacar.blablacar.clients.blablacar_v3.tariff import create_response
from travel.rasp.blablacar.blablacar.clients.const import (
    BLABLACAR_API_KEY, BLABLACAR_PROCESSING_ERRORS, BLABLACAR_SPECIAL_ERRORS_BY_STATUS_CODE
)
from travel.rasp.blablacar.blablacar.serialization import BlablacarQuery
from travel.rasp.blablacar.blablacar.service import query_and_cache
from travel.rasp.blablacar.blablacar.worker import Worker


some_date = date(2016, 8, 31)

TESTS = [
    dict(
        service_url='/?date=2021-10-15&national_version=ru&pointFrom=c44&pointTo=c54&isTouch=false&minimalDistance=50',
        blablacar_mock_url='https://public-api.blablacar.com/api/v3/trips',
        blablacar_mock_content=r"""{"link":"https://www.blablacar.ru/search?fc=56.852775,53.211463&tc=56.838607,60.605514&fn=Ижевск&tn=Екатеринбург&db=2021-10-15&de=2021-10-15","search_info":{"count":10,"full_trip_count":0},"trips":[{"link":"https://www.blablacar.ru/trip?source=CARPOOLING&id=2299059598-izhevsk-ekaterinburg","waypoints":[{"date_time":"2021-10-15T17:30:00","place":{"city":"Ижевск","address":"Ижевск, Россия","latitude":56.86186,"longitude":53.232428,"country_code":"RU"}},{"date_time":"2021-10-16T04:10:00","place":{"city":"Екатеринбург","address":"Екатеринбург, Россия","latitude":56.843099,"longitude":60.645408,"country_code":"RU"}}],"price":{"amount":"830.00","currency":"RUB"},"distance_in_meters":657648,"duration_in_seconds":34800}],"next_cursor":"cGFnZT0x"}""",  # noqa: E501
        expected_response={
            'querying': False,
            'distance': 449.4318986136815,
            'tariff': {
                'duration': 34800,
                'offersCount': 10,
                'price': {
                    'currency': 'RUB',
                    'value': 830.0
                },
                'orderUrl': 'https://www.blablacar.ru/search?fc=56.852775%2C53.211463&tc=56.838607%2C60.605514&fn=%D0%98%D0%B6%D0%B5%D0%B2%D1%81%D0%BA&tn=%D0%95%D0%BA%D0%B0%D1%82%D0%B5%D1%80%D0%B8%D0%BD%D0%B1%D1%83%D1%80%D0%B3&db=2021-10-15&de=2021-10-15&utm_source=YANDEXRASP&utm_medium=API&utm_campaign=RU_YANDEXRASP_PSGR_TIMETABLE_none&comuto_cmkt=RU_YANDEXRASP_PSGR_TIMETABLE_none&seats=1',  # noqa: E501
                'title': 'Ижевск — Екатеринбург'
            }
        },
        expected_params={
            'from_coordinate': '56.852775,53.211463',
            'to_coordinate': '56.838607,60.605514',
            'locale': 'ru_RU',
            'currency': 'RUB',
            'key': BLABLACAR_API_KEY,
            'sort': 'price:asc',
            'count': '1',
            'requested_seats': '1',
            'end_date_local': '2021-10-15T00:00:00',
            'start_date_local': '2021-10-15T00:00:00'
        }
    ),
    dict(
        service_url='/?date=2021-10-16&national_version=ru&pointFrom=c44&pointTo=c54&isTouch=false&minimalDistance=100',
        blablacar_mock_url='https://public-api.blablacar.com/api/v3/trips',
        blablacar_mock_content=r"""{"link":"https://www.blablacar.ru/search?fc=56.852775,53.211463&tc=56.838607,60.605514&fn=Ижевск&tn=Екатеринбург&db=2021-10-16&de=2021-10-16","search_info":{"count":10,"full_trip_count":0},"trips":[{"link":"https://www.blablacar.ru/trip?source=CARPOOLING&id=2299059598-izhevsk-ekaterinburg","waypoints":[{"date_time":"2021-10-15T17:30:00","place":{"city":"Ижевск","address":"Ижевск, Россия","latitude":56.86186,"longitude":53.232428,"country_code":"RU"}},{"date_time":"2021-10-16T04:10:00","place":{"city":"Екатеринбург","address":"Екатеринбург, Россия","latitude":56.843099,"longitude":60.645408,"country_code":"RU"}}],"price":{"amount":"830.00","currency":"RUB"},"distance_in_meters":657648,"duration_in_seconds":34800}],"next_cursor":"cGFnZT0x"}""",  # noqa: E501
        expected_response={
            'querying': False,
            'distance': 449.4318986136815,
            'tariff': {
                'duration': 34800,
                'offersCount': 10,
                'price': {
                    'currency': 'RUB',
                    'value': 830.0
                },
                'orderUrl': 'https://www.blablacar.ru/search?fc=56.852775%2C53.211463&tc=56.838607%2C60.605514&fn=%D0%98%D0%B6%D0%B5%D0%B2%D1%81%D0%BA&tn=%D0%95%D0%BA%D0%B0%D1%82%D0%B5%D1%80%D0%B8%D0%BD%D0%B1%D1%83%D1%80%D0%B3&db=2021-10-16&de=2021-10-16&utm_source=YANDEXRASP&utm_medium=API&utm_campaign=RU_YANDEXRASP_PSGR_TIMETABLE_none&comuto_cmkt=RU_YANDEXRASP_PSGR_TIMETABLE_none&seats=1',  # noqa: E501
                'title': 'Ижевск — Екатеринбург'
            }
        },
        expected_params={
            'from_coordinate': '56.852775,53.211463',
            'to_coordinate': '56.838607,60.605514',
            'locale': 'ru_RU',
            'currency': 'RUB',
            'key': BLABLACAR_API_KEY,
            'sort': 'price:asc',
            'count': '1',
            'requested_seats': '1',
            'end_date_local': '2021-10-16T00:00:00',
            'start_date_local': '2021-10-16T00:00:00'
        }
    ),
    dict(
        service_url='/?date=2050-01-01&national_version=ru&pointFrom=c44&pointTo=c54&isTouch=false',
        blablacar_mock_url='https://public-api.blablacar.com/api/v3/trips',
        blablacar_mock_content=r"""{"link":"https://www.blablacar.ru/search?fc=56.852775,53.211463&tc=56.838607,60.605514&fn=Ижевск&tn=Екатеринбург&db=2021-10-16&de=2021-10-16","search_info":{"count":1,"full_trip_count":0},"trips":[{"link":"https://www.blablacar.ru/trip?source=CARPOOLING&id=2299059598-izhevsk-ekaterinburg","waypoints":[{"date_time":"2021-10-15T17:30:00","place":{"city":"Ижевск","address":"Ижевск, Россия","latitude":56.86186,"longitude":53.232428,"country_code":"RU"}},{"date_time":"2021-10-16T04:10:00","place":{"city":"Екатеринбург","address":"Екатеринбург, Россия","latitude":56.843099,"longitude":60.645408,"country_code":"RU"}}],"price":{"amount":"830.00","currency":"RUB"},"distance_in_meters":657648,"duration_in_seconds":34800}],"next_cursor":"cGFnZT0x"}""",  # noqa: E501
        expected_response={
            'querying': False,
            'distance': 449.4318986136815,
            'tariff': {
                'duration': 34800,
                'offersCount': 1,
                'price': {
                    'currency': 'RUB',
                    'value': 830.0
                },
                'orderUrl': 'https://www.blablacar.ru/search?fc=56.852775%2C53.211463&tc=56.838607%2C60.605514&fn=%D0%98%D0%B6%D0%B5%D0%B2%D1%81%D0%BA&tn=%D0%95%D0%BA%D0%B0%D1%82%D0%B5%D1%80%D0%B8%D0%BD%D0%B1%D1%83%D1%80%D0%B3&db=2021-10-16&de=2021-10-16&utm_source=YANDEXRASP&utm_medium=API&utm_campaign=RU_YANDEXRASP_PSGR_TIMETABLE_none&comuto_cmkt=RU_YANDEXRASP_PSGR_TIMETABLE_none&seats=1',  # noqa: E501
                'title': 'Ижевск — Екатеринбург'
            }
        },
        expected_params={
            'from_coordinate': '56.852775,53.211463',
            'to_coordinate': '56.838607,60.605514',
            'locale': 'ru_RU',
            'currency': 'RUB',
            'key': BLABLACAR_API_KEY,
            'sort': 'price:asc',
            'count': '1',
            'requested_seats': '1',
            'end_date_local': '2050-01-01T00:00:00',
            'start_date_local': '2050-01-01T00:00:00'
        }
    )
]


def worker_start_daemon_stun(self):
    super(Worker, self).run()


@pytest.fixture()
def blablacar_cache():
    cache = LocMemCache('blablacar_cache', {'timeout': 3600})
    with patch.object(query_and_cache, 'caches') as m_caches:
        m_caches.__getitem__.side_effect = {'blablacar': cache}.__getitem__
        try:
            yield cache
        finally:
            cache.clear()


def throw_timeout_error(_request, _uri, _headers):
    raise requests.Timeout('Connection timed out.')


@httpretty.activate
@replace_setting('BLABLACAR_MIN_DISTANCE_KM', 75)
@replace_dynamic_setting('BLABLACAR_MIN_OFFERS_COUNT', 0)
@patch.object(Worker, 'start_daemon', worker_start_daemon_stun)
@pytest.mark.dbuser
def test_simple(blablacar_cache):
    httpretty.register_uri('GET', 'http://somehost.yandex.ru/show-blablacar', body='{"show": true}')
    create_settlement(id=44, longitude=53.211463, latitude=56.852775, title='Ижевск', country_id=Country.RUSSIA_ID)
    create_settlement(
        id=54, longitude=60.605514, latitude=56.838607, title='Екатеринбург', country_id=Country.RUSSIA_ID
    )

    for test in TESTS:
        blablacar_mock_url = test['blablacar_mock_url']
        blablacar_mock_content = test['blablacar_mock_content']
        httpretty.register_uri(httpretty.GET, blablacar_mock_url, body=blablacar_mock_content)
        service_url = test['service_url']
        expected_response = test['expected_response']
        expected_params = test['expected_params']

        init_response = Client().get(service_url).data
        blablacar_request = httpretty.last_request()

        assert init_response == {'tariff': {}, "banned": False, 'querying': True}

        done_response = Client().get(service_url).data

        real_response = done_response
        real_params = dict((key, value[0]) for key, value in blablacar_request.querystring.items())
        assert len(real_response['tariff']) == len(expected_response['tariff'])
        for key in real_response['tariff'].keys():
            assert real_response['tariff'][key] == expected_response['tariff'][key],\
                """
                Несовпадение ключа: {}
                Ожидалось значение: {}
                Получено значение: {}
                """.format(
                    key,
                    expected_response['tariff'][key],
                    real_response['tariff'][key],
                )  # noqa: E123
        assert real_response['distance'] == expected_response['distance']

        assert real_params == expected_params
    assert len(httpretty.latest_requests()) == len(TESTS)


@replace_dynamic_setting('BLABLACAR_MIN_OFFERS_COUNT', 0)
@pytest.mark.parametrize('create_function, expected_title_template', [
    (create_station, 'авт.ост. A {} авт.ост. B'),
    (create_settlement, 'A {} B'),
])
@pytest.mark.dbuser
def test_empty_tariff_city_city(create_function, expected_title_template):
    query = BlablacarQuery(
        point_from=create_function(title_ru='A'),
        point_to=create_function(title_ru='B'),
        date=some_date,
        national_version='ru'
    )
    blablacar_response = BlablacarV3Response(
        link='https://domian.ru/path/?fn=from&tn=to',
        trips=[Trip(price=Price(1), duration_in_seconds=5)],
        search_info=SearchInfo(count=0, full_trip_count=0)
    )
    tariff, error = create_response(blablacar_response, query)
    assert_that(tariff.to_response(), has_entries(
        offersCount=0,  # ключевой признак "пустого" ответа
        orderUrl=instance_of(basestring),
        title=expected_title_template.format(DASH),
    ))


@replace_dynamic_setting('BLABLACAR_MIN_OFFERS_COUNT', 0)
@pytest.mark.parametrize('national_version, is_touch, total_trip_count, expected', [
    ('ru', True, 10, {
        'utm_source': ['YANDEXRASP'],
        'utm_medium': ['API'],
        'utm_campaign': ['RU_YANDEXRASP_PSGR_TRASP_none'],
        'comuto_cmkt': ['RU_YANDEXRASP_PSGR_TRASP_none'],
    }),
    ('ru', False, 10, {
        'utm_source': ['YANDEXRASP'],
        'utm_medium': ['API'],
        'utm_campaign': ['RU_YANDEXRASP_PSGR_TIMETABLE_none'],
        'comuto_cmkt': ['RU_YANDEXRASP_PSGR_TIMETABLE_none'],
    }),
    ('zz', True, 10, {
        'utm_source': ['YANDEXRASP'],
        'utm_medium': ['API'],
        'utm_campaign': ['RU_YANDEXRASP_PSGR_TRASP_none'],
        'comuto_cmkt': ['RU_YANDEXRASP_PSGR_TRASP_none'],
    }),
    ('zz', False, 10, {
        'utm_source': ['YANDEXRASP'],
        'utm_medium': ['API'],
        'utm_campaign': ['RU_YANDEXRASP_PSGR_TIMETABLE_none'],
        'comuto_cmkt': ['RU_YANDEXRASP_PSGR_TIMETABLE_none'],
    }),
    ('ua', True, 10, {
        'utm_source': ['YANDEXRASP'],
        'utm_medium': ['API'],
        'utm_campaign': ['UA_YANDEXRASP_PSGR_TRASP_none'],
        'comuto_cmkt': ['UA_YANDEXRASP_PSGR_TRASP_none'],
    }),
    ('ua', False, 10, {
        'utm_source': ['YANDEXRASP'],
        'utm_medium': ['API'],
        'utm_campaign': ['UA_YANDEXRASP_PSGR_TIMETABLE_none'],
        'comuto_cmkt': ['UA_YANDEXRASP_PSGR_TIMETABLE_none'],
    }),
    ('ru', False, 0, {
        'utm_source': ['YANDEXRASP'],
        'utm_medium': ['API'],
        'utm_campaign': ['RU_YANDEXRASP_PSGR_NOOFEERS_none'],
        'comuto_cmkt': ['RU_YANDEXRASP_PSGR_NOOFEERS_none'],
    }),
    ('ua', True, 0, {
        'utm_source': ['YANDEXRASP'],
        'utm_medium': ['API'],
        'utm_campaign': ['UA_YANDEXRASP_PSGR_NOOFEERS_none'],
        'comuto_cmkt': ['UA_YANDEXRASP_PSGR_NOOFEERS_none'],
    }),
])
@pytest.mark.dbuser
def test_from_daemon_tracking_params(national_version, is_touch, total_trip_count, expected):
    query = BlablacarQuery(
        point_from=create_settlement(),
        point_to=create_settlement(),
        date=some_date,
        national_version=national_version,
        is_touch=is_touch
    )
    blablacar_response = BlablacarV3Response(
        link='https://domian.ru/path/?fn=from&tn=to',
        trips=([Trip(price=Price(1), duration_in_seconds=2)]),
        search_info=SearchInfo(count=total_trip_count, full_trip_count=0)
    )
    response, error = create_response(blablacar_response, query)
    url_parts = urlsplit(urlunquote(response.to_response()['orderUrl']))
    assert_that(parse_qs(url_parts.query), has_entries(expected))


@httpretty.activate
@replace_now('2018-11-26')
@patch.object(Worker, 'start_daemon', worker_start_daemon_stun)
@replace_dynamic_setting('ENABLE_BLABLACAR_VIEW_YANDEX_BUS_CHECK', False)
@pytest.mark.dbuser
def test_special_case(blablacar_cache):
    point_from = create_settlement(longitude=30.315868, latitude=59.939095, country_id=Country.RUSSIA_ID)
    point_to = create_settlement(longitude=37.619899, latitude=55.753676, country_id=Country.RUSSIA_ID)
    httpretty.register_uri(httpretty.GET, BLABLACAR_URL, body='{}', status=status.HTTP_429_TOO_MANY_REQUESTS)
    url = '/?pointFrom={}&pointTo={}&date=2018-12-01'.format(point_from.point_key, point_to.point_key)

    init_response = Client().get(url)
    response = Client().get('/poll' + url)

    assert init_response.data == {'tariff': {}, "banned": False, 'querying': True}
    expected = BLABLACAR_SPECIAL_ERRORS_BY_STATUS_CODE[status.HTTP_429_TOO_MANY_REQUESTS]
    assert response.data == expected['data']
    assert response.status_code == expected['status']
    assert len(httpretty.latest_requests()) == 1


@httpretty.activate
@replace_now('2020-02-01')
@patch.object(Worker, 'start_daemon', worker_start_daemon_stun)
@pytest.mark.dbuser
def test_banned_directions(blablacar_cache):
    point_russia = create_settlement(longitude=30.315868, latitude=59.939095, country_id=Country.RUSSIA_ID)
    crimea_region = create_region(id=Region.CRIMEA_REGION_ID)
    point_crimea = create_settlement(longitude=33.826230, latitude=45.921484, region=crimea_region)
    finland = create_country(id=Country.FINLAND_ID)
    point_abroad1 = create_settlement(country=finland)
    point_abroad2 = create_settlement(country=finland)

    banned_respose = {'tariff': {}, "banned": True, 'querying': False}

    url = '/?pointFrom={}&pointTo={}&date=2020-02-01'.format(point_russia.point_key, point_crimea.point_key)
    init_response = Client().get(url)
    assert init_response.status_code == status.HTTP_200_OK
    assert init_response.data == banned_respose

    url = '/?pointFrom={}&pointTo={}&date=2020-02-01'.format(point_abroad1.point_key, point_abroad2.point_key)
    init_response = Client().get(url)
    assert init_response.status_code == status.HTTP_200_OK
    assert init_response.data == banned_respose


@httpretty.activate
@replace_now('2018-11-26')
@patch.object(Worker, 'start_daemon', worker_start_daemon_stun)
@replace_dynamic_setting('ENABLE_BLABLACAR_VIEW_YANDEX_BUS_CHECK', False)
@pytest.mark.dbuser
def test_communication_error(blablacar_cache):
    point_from = create_settlement(longitude=30.315868, latitude=59.939095, country_id=Country.RUSSIA_ID)
    point_to = create_settlement(longitude=37.619899, latitude=55.753676, country_id=Country.RUSSIA_ID)
    httpretty.register_uri(httpretty.GET, BLABLACAR_URL, status=status.HTTP_200_OK, body=throw_timeout_error)
    url = '/?pointFrom={}&pointTo={}&date=2018-12-01'.format(point_from.point_key, point_to.point_key)

    init_response = Client().get(url)
    response = Client().get('/poll' + url)

    assert init_response.data == {'tariff': {}, "banned": False, 'querying': True}
    expected = BLABLACAR_PROCESSING_ERRORS['communication_error']
    assert response.data == expected['data']
    assert response.status_code == expected['status']
    assert len(httpretty.latest_requests()) == 1


def test_blablacar_response_serialization():
    blablacar_response = '''{
    "link":"https://www.blablacar.ru/search?fc=56.852775,53.211463&tc=56.838607,60",
    "search_info":{"count":10,"full_trip_count":3},
    "trips":[{"link":"https://www.blablacar.ru/trip?source=CARPOOLING&id=2299059598-izhevsk-ekaterinburg","waypoints":[{"date_time":"2021-10-15T17:30:00","place":{"city":"Ижевск","address":"Ижевск, Россия","latitude":56.86186,"longitude":53.232428,"country_code":"RU"}},{"date_time":"2021-10-16T04:10:00","place":{"city":"Екатеринбург","address":"Екатеринбург, Россия","latitude":56.843099,"longitude":60.645408,"country_code":"RU"}}],"price":{"amount":"350.00","currency":"RUB"},"distance_in_meters":657648,"duration_in_seconds":22}]
    }'''  # noqa: E501

    result, _ = BlablacarV3ResponseSchema().loads_old(blablacar_response)

    assert result.link == 'https://www.blablacar.ru/search?fc=56.852775,53.211463&tc=56.838607,60'
    trip = result.trips[0]
    assert trip.price.value == 350.0
    assert trip.price.currency == 'RUB'
    assert trip.duration_in_seconds == 22
    assert result.search_info.count == 7


@httpretty.activate
@pytest.mark.dbuser
def test_settlement():
    settlement_ru = create_settlement(id=111, title_ru='Ст. Город', country_id=Country.RUSSIA_ID)
    settlement_ua = create_settlement(id=222, title_uk='Micто', country_id=Country.RUSSIA_ID)

    url = '/settlement/?id={}&nationalVersion=ru&language=ru'.format(settlement_ru.id)
    response = Client().get(url)
    assert response.status_code == status.HTTP_200_OK
    assert response.data == {
        'url': 'https://www.blablacar.ru/search?fn=%D0%A1%D1%82.%20%D0%93%D0%BE%D1%80%D0%BE%D0%B4%2C%20%D0%A0%D0%BE%D1%81%D1%81%D0%B8%D1%8F',  # noqa

        'banned': False
    }

    url = '/settlement/?id={}&nationalVersion=ua&language=uk'.format(settlement_ua.id)
    response = Client().get(url)
    assert response.status_code == status.HTTP_200_OK
    assert response.data == {
        'url': 'https://www.blablacar.com.ua/search?fn=Mic%D1%82%D0%BE%2C%20%D0%A0%D0%BE%D1%81%D1%96%D1%8F',
        'banned': False
    }


@httpretty.activate
@pytest.mark.dbuser
def test_banned_settlements():
    crimea_region = create_region(id=Region.CRIMEA_REGION_ID)
    settlement_crimea = create_settlement(id=111, region=crimea_region, country_id=Country.RUSSIA_ID)
    finland = create_country(id=Country.FINLAND_ID)
    settlement_abroad = create_settlement(id=222, country=finland)

    banned_respose = {'url': '', "banned": True}

    url = '/settlement/?id={}&nationalVersion=ru&language=ru'.format(settlement_crimea.id)
    response = Client().get(url)
    assert response.status_code == status.HTTP_200_OK
    assert response.data == banned_respose

    url = '/settlement/?id={}&nationalVersion=ua&language=uk'.format(settlement_crimea.id)
    response = Client().get(url)
    assert response.status_code == status.HTTP_200_OK
    assert response.data == banned_respose

    url = '/settlement/?id={}&nationalVersion=ru&language=ru'.format(settlement_abroad.id)
    response = Client().get(url)
    assert response.status_code == status.HTTP_200_OK
    assert response.data == banned_respose


@httpretty.activate
@replace_setting('BLABLACAR_MIN_DISTANCE_KM', 0)
@patch.object(Worker, 'start_daemon', worker_start_daemon_stun)
@pytest.mark.dbuser
@pytest.mark.parametrize('min_offers_count, expected_response', [
    (0, {'tariff': has_entries({'title': 'Ижевск — Екатеринбург'})}),
    (5, {'error': 'not_enough_trips in blablacar'}),
])
def test_empty_response_by_min_offers(blablacar_cache, min_offers_count, expected_response):
    create_settlement(id=44, title='Ижевск', country_id=Country.RUSSIA_ID)
    create_settlement(id=54, title='Екатеринбург', country_id=Country.RUSSIA_ID)

    test = TESTS[-1]
    blablacar_mock_url = test['blablacar_mock_url']
    blablacar_mock_content = test['blablacar_mock_content']
    httpretty.register_uri(httpretty.GET, blablacar_mock_url, body=blablacar_mock_content)
    service_url = test['service_url']

    with replace_dynamic_setting('BLABLACAR_MIN_OFFERS_COUNT', min_offers_count):
        Client().get(service_url)
        response = Client().get(service_url).data
        assert_that(response, has_entries(expected_response))
