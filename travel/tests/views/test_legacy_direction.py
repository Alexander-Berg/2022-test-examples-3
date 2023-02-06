# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

import mock
from collections import namedtuple
from datetime import datetime

import pytest
import pytz
from django.conf import settings
from django.test.client import Client

from common.models.currency import Price
from common.tester.factories import create_station
from common.tester.transaction_context import transaction_fixture
from common.tester.utils.replace_setting import replace_setting
from travel.rasp.wizards.suburban_wizard_api.lib.direction.tariffs_cache import TariffsCache
from travel.rasp.wizards.suburban_wizard_api.views import legacy_direction
from travel.rasp.wizards.wizard_lib.cache import Translations


@pytest.fixture
def m_get_tariff():
    with mock.patch.object(TariffsCache, 'get_tariff', return_value=Price(123)) as m_get_tariff:
        yield m_get_tariff


@pytest.fixture
def m_find_segments():
    with mock.patch.object(legacy_direction.schedule_cache, 'find_segments', autospec=True) as m_find_segments:
        yield m_find_segments


_DefaultQuery = namedtuple('_DefaultQuery', 'query departure_station arrival_station')


@pytest.fixture
@transaction_fixture
def default_query(request, fixed_now):
    departure_station = create_station(title='some_departure_station')
    arrival_station = create_station(title='some_arrival_station')
    return _DefaultQuery(
        query={
            'departure_point_key': departure_station.point_key,
            'arrival_point_key': arrival_station.point_key,
            'date': '2000-01-01',
            'tld': 'ru'
        },
        departure_station=departure_station,
        arrival_station=arrival_station
    )


@pytest.mark.dbuser
def test_can_not_find_segments(m_find_segments, default_query):
    m_find_segments.return_value = ()
    response = Client().get('/api/legacy_direction/', dict(default_query.query))
    assert response.status_code == 204


@pytest.mark.dbuser
@replace_setting('MORDA_HOST_BY_TLD', {'ru': 'rasp.yandex.ru'})
@replace_setting('TOUCH_HOST_BY_TLD', {'ru': 't.rasp.yandex.ru'})
def test_format(m_find_segments, m_get_tariff, rur, default_query):
    query = default_query.query
    departure_station = default_query.departure_station
    arrival_station = default_query.arrival_station

    m_find_segments.return_value = (
        mock.Mock(**{
            'thread_start_dt': datetime(2000, 1, 1),
            'thread.express_type': None,
            'thread.uid': 'some_uid',
            'thread.number': 'some_number',
            'thread.title': Translations(**{lang: 'title' for lang in settings.MODEL_LANGUAGES}),
            'departure_dt': datetime(2000, 1, 1, 3, 4, tzinfo=pytz.UTC),
            'departure_station': departure_station,
            'arrival_dt': datetime(2000, 1, 1, 5, 6, tzinfo=pytz.UTC),
            'arrival_station': arrival_station,
        }),
    )

    response = Client().get('/api/legacy_direction/', dict(query))
    assert response.status_code == 200

    expected_direction_query = (
        '?fromName=some_departure_station&fromId={}&toName=some_arrival_station&toId={}'
        .format(departure_station.point_key, arrival_station.point_key)
    )
    expected_search_query = expected_direction_query + '&when=2000-01-01'
    expected_thread_query = (
        '?departure=2000-01-01&station_from={}&station_to={}'
        .format(departure_station.id, arrival_station.id)
    )
    assert response.json() == {
        'default_transport': {
            'departure_date': '2000-01-01',
            'minimum_duration': 122.0,
            'minimum_price': {'currency': 'RUR', 'value': 123},
            'segments': [
                {
                    'arrival': '2000-01-01 08:06:00 +0300',
                    'departure': '2000-01-01 06:04:00 +0300',
                    'duration': 122.0,
                    'from_station': 'от some_departure_station',
                    'number': 'some_number',
                    'title': 'title',
                    'price': {'currency': 'RUR', 'value': 123},
                    'touch_url': 'https://t.rasp.yandex.ru/thread/some_uid/' + expected_thread_query,
                    'url': 'https://rasp.yandex.ru/thread/some_uid/' + expected_thread_query,
                }
            ],
            'total': 1,
            'touch_url': 'https://t.rasp.yandex.ru/search/suburban/' + expected_search_query,
            'transport': 'suburban',
            'url': 'https://rasp.yandex.ru/search/suburban/' + expected_search_query,
        },
        'next_link': {
            'title': 'Ближайшие',
            'touch_url': 'https://t.rasp.yandex.ru/search/suburban/next/' + expected_direction_query,
            'url': 'https://rasp.yandex.ru/search/suburban/next/' + expected_direction_query
        },
        'path_items': [{
            'text': 'rasp.yandex.ru',
            'touch_url': 'https://t.rasp.yandex.ru/search/suburban/' + expected_search_query,
            'url': 'https://rasp.yandex.ru/search/suburban/' + expected_search_query,
        }],
        'tomorrow_link': {
            'title': 'Завтра',
            'touch_url': 'https://t.rasp.yandex.ru/search/suburban/' + expected_direction_query + '&when=2000-01-02',
            'url': 'https://rasp.yandex.ru/search/suburban/' + expected_direction_query + '&when=2000-01-02'
        },
        'query': {
            'arrival_point': {'key': arrival_station.point_key, 'title': 'some_arrival_station'},
            'departure_date': '2000-01-01',
            'departure_point': {'key': departure_station.point_key, 'title': 'some_departure_station'},
        },
        'snippet': {
            '__hl':
                'Проезд по полному тарифу стоит от 123 руб. На Яндекс.Расписаниях есть информация обо всех изменениях, '
                'задержках и отменах электричек по состоянию на 1 января. Время в пути от 2 ч 2 мин.'
        },
        'title': {'__hl': 'Расписание электричек some_departure_station — some_arrival_station'},
        'touch_url': 'https://t.rasp.yandex.ru/search/suburban/' + expected_search_query,
        'type': 'transports_with_default',
        'url': 'https://rasp.yandex.ru/search/suburban/' + expected_search_query,
    }
