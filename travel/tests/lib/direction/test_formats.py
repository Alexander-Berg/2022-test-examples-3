# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

from collections import OrderedDict
from datetime import date, timedelta

import pytest

from common.models.currency import Price
from common.tester.factories import create_station
from common.tester.utils.datetime import replace_now
from common.tester.utils.replace_setting import replace_setting
from travel.rasp.wizards.proxy_api.lib.direction.formats import format_direction, format_transport_direction, get_search_urls
from travel.rasp.wizards.proxy_api.lib.direction.models import AggregatedDirectionData
from travel.rasp.wizards.proxy_api.lib.tests_utils import make_direction_query, make_segment
from travel.rasp.wizards.wizard_lib.experiment_flags import ExperimentFlag
from travel.rasp.wizards.wizard_lib.serialization.thread_express_type import ThreadExpressType
from travel.rasp.wizards.wizard_lib.tests_utils import utc_dt


pytestmark = pytest.mark.dbuser


@pytest.fixture(autouse=True)
def module_fixtures(rur):
    with replace_now('2000-01-01'), \
            replace_setting('MORDA_HOST_BY_TLD', {'ru': 'rasp.yandex.ru', 'kz': 'rasp.yandex.kz'}), \
            replace_setting('TOUCH_HOST_BY_TLD', {'ru': 't.rasp.yandex.ru', 'kz': 't.rasp.yandex.kz'}):
        yield


@pytest.mark.parametrize('departure_date, experiment_flags, transport_code, expected', (
    (None, frozenset(), None, '/search/?foo=bar&when=2000-01-01'),
    (None, frozenset(), 'bus', '/search/bus/?foo=bar&when=2000-01-01'),
    (date(2000, 1, 1), frozenset(), None, '/search/?foo=bar&when=2000-01-01'),
    (date(2000, 1, 1), frozenset(), 'bus', '/search/bus/?foo=bar&when=2000-01-01'),
    (None, frozenset([ExperimentFlag.DIRECTION_SEARCH_NEXT_URL]), None, '/search/next/?foo=bar'),
    (None, frozenset([ExperimentFlag.DIRECTION_SEARCH_NEXT_URL]), 'bus', '/search/bus/next/?foo=bar'),
    (
        date(2000, 1, 1),
        frozenset([ExperimentFlag.DIRECTION_SEARCH_NEXT_URL]),
        None,
        '/search/?foo=bar&when=2000-01-01'
    ),
    (
        date(2000, 1, 1),
        frozenset([ExperimentFlag.DIRECTION_SEARCH_NEXT_URL]),
        'bus',
        '/search/bus/?foo=bar&when=2000-01-01'
    ),
))
def test_get_search_urls(departure_date, experiment_flags, transport_code, expected):
    departure_station = create_station()
    arrival_station = create_station()

    assert get_search_urls(
        query=make_direction_query(
            departure_station,
            arrival_station,
            departure_date=departure_date,
            experiment_flags=experiment_flags
        ),
        found_departure_date=date(2000, 1, 1),
        transport_code=transport_code,
        url_query=b'foo=bar',
    ) == {
        'url': 'https://rasp.yandex.ru' + expected,
        'touch_url': 'https://t.rasp.yandex.ru' + expected,
    }


@pytest.mark.parametrize('tld, expected_tld', (('ru', 'ru'), ('kz', 'kz'), ('bz', 'ru')))
def test_format_direction(tld, expected_tld):
    departure_station = create_station(title='some_departure_station')
    arrival_station = create_station(title='some_arrival_station')
    default_transport_code = 'bus'
    direction_query = make_direction_query(departure_station, arrival_station, tld=tld)
    transports_data = OrderedDict([
        ('bus', AggregatedDirectionData(
            segments=(
                make_segment(
                    departure_station=departure_station,
                    arrival_station=arrival_station,
                    departure_local_dt=utc_dt(2000, 1, 1, 1),
                    arrival_local_dt=utc_dt(2000, 1, 1, 12),
                    price=Price(500),
                    thread=dict(
                        number='some_bus_number',
                        title='some_bus_title',
                        start_date=date(2000, 1, 1),
                    ),
                ),
            ),
            total=100,
            found_departure_date=date(2000, 1, 1),
            minimum_duration=timedelta(hours=11),
            minimum_price=Price(500)
        )),
        ('suburban', AggregatedDirectionData(
            segments=(
                make_segment(
                    departure_station=departure_station,
                    arrival_station=arrival_station,
                    departure_local_dt=utc_dt(2000, 1, 1, 1),
                    arrival_local_dt=utc_dt(2000, 1, 1, 15),
                    price=Price(300),
                    thread=dict(
                        number='some_suburban_number',
                        title='some_suburban_title',
                        start_date=date(2000, 1, 1),
                    ),
                ),
            ),
            total=25,
            found_departure_date=date(2000, 1, 1),
            minimum_duration=timedelta(hours=14),
            minimum_price=Price(300)
        )),
    ])
    expected_direction_query = (
        '?from=wrasppp&fromName=some_departure_station&fromId={}&toName=some_arrival_station&toId={}'
        .format(departure_station.point_key, arrival_station.point_key)
    )
    expected_search_query = expected_direction_query + '&when=2000-01-01'

    assert format_direction(transports_data, direction_query, default_transport_code) == {
        'default_transport': {
            'departure_date': '2000-01-01',
            'minimum_duration': 660.0,
            'minimum_price': {
                'currency': 'RUR',
                'value': 500.0
            },
            'segments': (
                {
                    'arrival': '2000-01-01 12:00:00 +0000',
                    'arrival_station': 'some_arrival_station',
                    'departure': '2000-01-01 01:00:00 +0000',
                    'departure_station': 'some_departure_station',
                    'duration': 660.0,
                    'from_station': 'от some_departure_station',
                    'to_station': 'до some_arrival_station',
                    'number': 'some_bus_number',
                    'price': {
                        'currency': 'RUR',
                        'value': 500.0
                    },
                    'title': 'some_bus_title',
                    'touch_url': 'http://mobile?from=wrasppp',
                    'transport_subtype_title': None,
                    'url': 'http://desktop?from=wrasppp'
                },
            ),
            'title': 'Автобусом',
            'total': 100,
            'touch_url': 'https://t.rasp.yandex.%s/search/bus/' % expected_tld + expected_search_query,
            'transport': 'bus',
            'url': 'https://rasp.yandex.%s/search/bus/' % expected_tld + expected_search_query
        },
        'content': (
            {
                'departure_date': '2000-01-01',
                'minimum_duration': 840.0,
                'minimum_price': {
                    'currency': 'RUR',
                    'value': 300.0
                },
                'segments': (
                    {
                        'arrival': '2000-01-01 15:00:00 +0000',
                        'arrival_station': 'some_arrival_station',
                        'departure': '2000-01-01 01:00:00 +0000',
                        'departure_station': 'some_departure_station',
                        'duration': 840.0,
                        'from_station': 'от some_departure_station',
                        'to_station': 'до some_arrival_station',
                        'number': 'some_suburban_number',
                        'price': {
                            'currency': 'RUR',
                            'value': 300.0
                        },
                        'title': 'some_suburban_title',
                        'touch_url': 'http://mobile?from=wrasppp',
                        'transport_subtype_title': None,
                        'url': 'http://desktop?from=wrasppp'
                    },
                ),
                'title': 'Электричкой',
                'total': 25,
                'touch_url': 'https://t.rasp.yandex.%s/search/suburban/' % expected_tld + expected_search_query,
                'transport': 'suburban',
                'url': 'https://rasp.yandex.%s/search/suburban/' % expected_tld + expected_search_query
            },
        ),
        'path_items': (
            {
                'text': 'rasp.yandex.%s' % expected_tld,
                'touch_url': 'https://t.rasp.yandex.%s/search/' % expected_tld + expected_search_query,
                'url': 'https://rasp.yandex.%s/search/' % expected_tld + expected_search_query
            },
        ),
        'snippet': {
            '__hl':
                'На Яндекс.Расписаниях можно выбрать удобный вариант проезда по маршруту '
                'some_departure_station — some_arrival_station автобусом или электричкой. Быстрее всего получится '
                'автобусом — время в пути 11 ч.'
        },
        'title': {
            '__hl': 'some_departure_station — some_arrival_station: расписание рейсов'
        },
        'touch_url': 'https://t.rasp.yandex.%s/search/' % expected_tld + expected_search_query,
        'type': 'transports_with_default',
        'url': 'https://rasp.yandex.%s/search/' % expected_tld + expected_search_query
    }


def test_format_transport_direction():
    departure_station = create_station(title='some_departure_station')
    arrival_station = create_station(title='some_arrival_station')
    direction_query = make_direction_query(departure_station, arrival_station)
    direction_data = AggregatedDirectionData(
        segments=(
            make_segment(
                departure_station=departure_station,
                arrival_station=arrival_station,
                price=Price(5),
                thread=dict(
                    number='some_number',
                    title='some_title',
                    start_date=date(2000, 1, 1),
                ),
            ),
        ),
        total=100,
        found_departure_date=date(2000, 1, 1),
        minimum_duration=timedelta(hours=11),
        minimum_price=Price(5)
    )
    expected_direction_query = (
        '?from=wrasppp&fromName=some_departure_station&fromId={}&toName=some_arrival_station&toId={}'
        .format(departure_station.point_key, arrival_station.point_key)
    )
    expected_search_query = expected_direction_query + '&when=2000-01-01'

    assert format_transport_direction(direction_data, direction_query, 'bus') == {
        'default_transport': {
            'departure_date': '2000-01-01',
            'minimum_duration': 660.0,
            'minimum_price': {
                'currency': 'RUR',
                'value': 5
            },
            'segments': (
                {
                    'arrival': '2000-01-01 12:00:00 +0000',
                    'arrival_station': 'some_arrival_station',
                    'departure': '2000-01-01 01:00:00 +0000',
                    'departure_station': 'some_departure_station',
                    'duration': 660.0,
                    'from_station': 'от some_departure_station',
                    'to_station': 'до some_arrival_station',
                    'number': 'some_number',
                    'price': {
                        'currency': 'RUR',
                        'value': 5
                    },
                    'title': 'some_title',
                    'touch_url': 'http://mobile?from=wrasppp',
                    'transport_subtype_title': None,
                    'url': 'http://desktop?from=wrasppp'
                },
            ),
            'title': 'Автобусом',
            'total': 100,
            'touch_url': 'https://t.rasp.yandex.ru/search/bus/' + expected_search_query,
            'transport': 'bus',
            'url': 'https://rasp.yandex.ru/search/bus/' + expected_search_query
        },
        'next_link': {
            'title': 'Ближайшие',
            'touch_url': 'https://t.rasp.yandex.ru/search/bus/next/' + expected_direction_query,
            'url': 'https://rasp.yandex.ru/search/bus/next/' + expected_direction_query
        },
        'path_items': (
            {
                'text': 'rasp.yandex.ru',
                'touch_url': 'https://t.rasp.yandex.ru/search/bus/' + expected_search_query,
                'url': 'https://rasp.yandex.ru/search/bus/' + expected_search_query
            },
        ),
        'snippet': {
            '__hl':
                'Цена проезда от 5 руб. На Яндекс.Расписаниях можно посмотреть график движения автобусов, '
                'автовокзалы отправления и прибытия, остановки. Время в пути от 11 ч.'
        },
        'title': {
            '__hl': 'some_departure_station — some_arrival_station: расписание автобусов'
        },
        'tomorrow_link': {
            'title': 'Завтра',
            'touch_url': 'https://t.rasp.yandex.ru/search/bus/' + expected_direction_query + '&when=2000-01-02',
            'url': 'https://rasp.yandex.ru/search/bus/' + expected_direction_query + '&when=2000-01-02'
        },
        'touch_url': 'https://t.rasp.yandex.ru/search/bus/' + expected_search_query,
        'type': 'transports_with_default',
        'url': 'https://rasp.yandex.ru/search/bus/' + expected_search_query,
        'filter': None,
        'content': None,
    }


def test_format_aeroexpress_transport_direction():
    departure_station = create_station(title='some_departure_station')
    arrival_station = create_station(title='some_arrival_station')
    direction_query = make_direction_query(
        departure_station,
        arrival_station,
        thread_express_type=ThreadExpressType.AEROEXPRESS,
        transport_code='suburban',
    )
    direction_data = AggregatedDirectionData(
        segments=(
            make_segment(
                departure_station=departure_station,
                arrival_station=arrival_station,
                price=Price(5),
                thread=dict(
                    number='some_number',
                    title='some_title',
                    start_date=date(2000, 1, 1),
                ),
            ),
        ),
        total=100,
        found_departure_date=date(2000, 1, 1),
        minimum_duration=timedelta(hours=11),
        minimum_price=Price(5)
    )
    expected_direction_query = (
        '?from=wrasppp&fromName=some_departure_station&fromId={}&toName=some_arrival_station&toId={}&aeroex=y'
        .format(departure_station.point_key, arrival_station.point_key)
    )
    expected_search_query = expected_direction_query + '&when=2000-01-01'

    assert format_transport_direction(direction_data, direction_query, 'suburban') == {
        'default_transport': {
            'departure_date': '2000-01-01',
            'minimum_duration': 660.0,
            'minimum_price': {
                'currency': 'RUR',
                'value': 5
            },
            'segments': (
                {
                    'arrival': '2000-01-01 12:00:00 +0000',
                    'arrival_station': 'some_arrival_station',
                    'departure': '2000-01-01 01:00:00 +0000',
                    'departure_station': 'some_departure_station',
                    'duration': 660.0,
                    'from_station': 'от some_departure_station',
                    'to_station': 'до some_arrival_station',
                    'number': 'some_number',
                    'price': {
                        'currency': 'RUR',
                        'value': 5
                    },
                    'title': 'some_title',
                    'touch_url': 'http://mobile?from=wrasppp',
                    'transport_subtype_title': None,
                    'url': 'http://desktop?from=wrasppp'
                },
            ),
            'title': 'Электричкой',
            'total': 100,
            'touch_url': 'https://t.rasp.yandex.ru/search/suburban/' + expected_search_query,
            'transport': 'suburban',
            'url': 'https://rasp.yandex.ru/search/suburban/' + expected_search_query
        },
        'next_link': {
            'title': 'Ближайшие',
            'touch_url': 'https://t.rasp.yandex.ru/search/suburban/next/' + expected_direction_query,
            'url': 'https://rasp.yandex.ru/search/suburban/next/' + expected_direction_query
        },
        'path_items': (
            {
                'text': 'rasp.yandex.ru',
                'touch_url': 'https://t.rasp.yandex.ru/search/suburban/' + expected_search_query,
                'url': 'https://rasp.yandex.ru/search/suburban/' + expected_search_query
            },
        ),
        'snippet': {
            '__hl':
                'Аэроэкспрессы от автобусной остановки some_departure_station. '
                'Цена проезда в один конец эконом-классом — 5 руб. '
                'при заказе через сайт или в приложении. Промежуточных остановок нет.'
        },
        'title': {
            '__hl': 'Аэроэкспрессы some_departure_station — some_arrival_station: расписание рейсов'
        },
        'tomorrow_link': {
            'title': 'Завтра',
            'touch_url': 'https://t.rasp.yandex.ru/search/suburban/' + expected_direction_query + '&when=2000-01-02',
            'url': 'https://rasp.yandex.ru/search/suburban/' + expected_direction_query + '&when=2000-01-02'
        },
        'touch_url': 'https://t.rasp.yandex.ru/search/suburban/' + expected_search_query,
        'type': 'transports_with_default',
        'url': 'https://rasp.yandex.ru/search/suburban/' + expected_search_query,
        'filter': {'aeroexpress': True},
        'content': None,
    }


def test_format_express_transport_direction():
    departure_station = create_station(title='some_departure_station')
    arrival_station = create_station(title='some_arrival_station')
    direction_query = make_direction_query(
        departure_station,
        arrival_station,
        thread_express_type=ThreadExpressType.EXPRESS,
        transport_code='suburban',
    )
    direction_data = AggregatedDirectionData(
        segments=(
            make_segment(
                departure_station=departure_station,
                arrival_station=arrival_station,
                price=Price(5),
                thread=dict(
                    number='some_number',
                    title='some_title',
                    start_date=date(2000, 1, 1),
                ),
            ),
        ),
        total=100,
        found_departure_date=date(2000, 1, 1),
        minimum_duration=timedelta(hours=11),
        minimum_price=Price(5)
    )

    expected_direction_query = (
        '?from=wrasppp&fromName=some_departure_station&fromId={}&toName=some_arrival_station&toId={}&express=y'
        .format(departure_station.point_key, arrival_station.point_key)
    )
    expected_search_query = expected_direction_query + '&when=2000-01-01'

    assert format_transport_direction(direction_data, direction_query, 'suburban') == {
        'default_transport': {
            'departure_date': '2000-01-01',
            'minimum_duration': 660.0,
            'minimum_price': {
                'currency': 'RUR',
                'value': 5
            },
            'segments': (
                {
                    'arrival': '2000-01-01 12:00:00 +0000',
                    'arrival_station': 'some_arrival_station',
                    'departure': '2000-01-01 01:00:00 +0000',
                    'departure_station': 'some_departure_station',
                    'duration': 660.0,
                    'from_station': 'от some_departure_station',
                    'to_station': 'до some_arrival_station',
                    'number': 'some_number',
                    'price': {
                        'currency': 'RUR',
                        'value': 5
                    },
                    'title': 'some_title',
                    'touch_url': 'http://mobile?from=wrasppp',
                    'transport_subtype_title': None,
                    'url': 'http://desktop?from=wrasppp'
                },
            ),
            'title': 'Электричкой',
            'total': 100,
            'touch_url': 'https://t.rasp.yandex.ru/search/suburban/' + expected_search_query,
            'transport': 'suburban',
            'url': 'https://rasp.yandex.ru/search/suburban/' + expected_search_query
        },
        'next_link': {
            'title': 'Ближайшие',
            'touch_url': 'https://t.rasp.yandex.ru/search/suburban/next/' + expected_direction_query,
            'url': 'https://rasp.yandex.ru/search/suburban/next/' + expected_direction_query
        },
        'path_items': (
            {
                'text': 'rasp.yandex.ru',
                'touch_url': 'https://t.rasp.yandex.ru/search/suburban/' + expected_search_query,
                'url': 'https://rasp.yandex.ru/search/suburban/' + expected_search_query
            },
        ),
        'snippet': {
            '__hl':
                'Проезд по полному тарифу стоит от 5 руб. На Яндекс.Расписаниях есть информация обо всех изменениях, '
                'задержках и отменах. Время в пути от 11 ч.'
        },
        'title': {
            '__hl': 'Экспрессы some_departure_station — some_arrival_station: расписание рейсов'
        },
        'tomorrow_link': {
            'title': 'Завтра',
            'touch_url': (
                'https://t.rasp.yandex.ru/search/suburban/' + expected_direction_query + '&when=2000-01-02'
            ),
            'url': (
                'https://rasp.yandex.ru/search/suburban/' + expected_direction_query + '&when=2000-01-02'
            ),
        },
        'touch_url': 'https://t.rasp.yandex.ru/search/suburban/' + expected_search_query,
        'type': 'transports_with_default',
        'url': 'https://rasp.yandex.ru/search/suburban/' + expected_search_query,
        'filter': {'express': True},
        'content': None,
    }
