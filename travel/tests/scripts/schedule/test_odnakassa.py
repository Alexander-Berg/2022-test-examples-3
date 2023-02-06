# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import mock

from datetime import datetime
from hamcrest import assert_that, contains, contains_inanyorder, has_entries, has_properties, has_property

from travel.rasp.rasp_scripts.scripts.schedule.odnakassa import OKParser


day1 = [
    {
        'station_start': {'station_title': 'Тверь', 'station_id': 1375},
        'station_end': {'station_title': 'Шорново', 'station_id': 10787},
        'ride_id': '1',
        'datetime_start': '2020-08-05 07:20:00',
        'datetime_end': '2020-08-05 07:40:00',
        'distance': 55,
        'price_source_tariff': 150,
        'carrier_title': 'Автобус+',
        'route_name': 'Тверь - Москва'
    },
    {
        'station_start': {'station_title': 'Тверь', 'station_id': 1375},
        'station_end': {'station_title': 'Радченко', 'station_id': 10668},
        'ride_id': '1',
        'datetime_start': '2020-08-05 07:20:00',
        'datetime_end': '2020-08-05 08:30:00',
        'distance': 100,
        'price_source_tariff': 200,
        'carrier_title': 'Автобус+',
        'route_name': 'Тверь - Москва'
    },
    {
        'station_start': {'station_title': 'Тверь', 'station_id': 1375},
        'station_end': {'station_title': 'Шорново', 'station_id': 10787},
        'ride_id': '2',
        'datetime_start': '2020-08-05 12:20:00',
        'datetime_end': '2020-08-05 13:40:00',
        'distance': 55,
        'price_source_tariff': 150,
        'carrier_title': 'Автобус++',
        'route_name': 'Тверь - Ржев'
    }
]

day2 = [
    {
        'station_start': {'station_title': 'Тверь', 'station_id': 1375},
        'station_end': {'station_title': 'Шорново', 'station_id': 10787},
        'ride_id': '4',
        'datetime_start': '2020-08-06 07:20:00',
        'datetime_end': '2020-08-06 07:40:00',
        'distance': 55,
        'price_source_tariff': 150,
        'carrier_title': 'Автобус+',
        'route_name': 'Тверь - Москва'
    },
    {
        'station_start': {'station_title': 'Тверь', 'station_id': 1375},
        'station_end': {'station_title': 'Радченко', 'station_id': 10668},
        'ride_id': '4',
        'datetime_start': '2020-08-06 07:20:00',
        'datetime_end': '2020-08-06 08:30:00',
        'distance': 100,
        'price_source_tariff': 200,
        'carrier_title': 'Автобус+',
        'route_name': 'Тверь - Москва'
    },
    {
        'station_start': {'station_title': 'Тверь', 'station_id': 1375},
        'station_end': {'station_title': 'Шорново', 'station_id': 10787},
        'ride_id': '5',
        'datetime_start': '2020-08-06 12:20:00',
        'datetime_end': '2020-08-06 13:40:00',
        'distance': 55,
        'price_source_tariff': 150,
        'carrier_title': 'Автобус++',
        'route_name': 'Тверь - Ржев'
    },
    {
        'station_start': {'station_title': 'Тверь', 'station_id': 1375},
        'station_end': {'station_title': 'Шорново', 'station_id': 10787},
        'ride_id': '6',
        'datetime_start': '2020-08-06 13:20:00',
        'datetime_end': '2020-08-06 14:40:00',
        'distance': 55,
        'price_source_tariff': 146,
        'carrier_title': 'Автобус++',
        'route_name': 'Тверь - Ржев'
    }
]


def create_test_parser():
    return OKParser(
        base_url='https://localhost:3030/ride/list',
        agent_id='128',
        secret_key='secret',
        cities='1375',
        start_date=datetime.strptime('2020-08-05', '%Y-%m-%d'),
        days_shift=14
    )


def test_get_hash():
    # Проверяем что правильно посчитан md5 https://paste.yandex-team.ru/1089556
    # url=https://api-gds.odnakassa.ru/ride/list?agent_id=128&city_id_start=1375&date=2020-08-04
    hash_code = create_test_parser().get_hash_code('1375', '2020-08-04')

    assert hash_code == '65016395e6c0e60fa20d1202b6bfa444'


def test_get_routes_for_dates():
    test_parser = create_test_parser()
    with mock.patch.object(test_parser, 'get_segments', return_value=day1):
        routes = test_parser.get_routes_for_date('1375', '2020-08-07')

    assert len(routes) == 2

    route = routes[0]
    assert route.ride_id == '1'
    assert route.start_time == '07:20'
    assert route.carrier_title == 'Автобус+'
    assert len(route.segments) == 2

    route = routes[1]
    assert route.ride_id == '2'
    assert route.start_time == '12:20'
    assert route.carrier_title == 'Автобус++'


def test_get_threads():
    test_parser = create_test_parser()
    with mock.patch.object(test_parser, 'get_segments', side_effect=[day1, day2]):
        threads = test_parser.get_threads_for_date_range('1375', datetime.strptime('2020-08-05', '%Y-%m-%d'), 1)

    threads.sort(key=lambda t: t.title)
    assert len(threads) == 3

    thread = threads[0]
    assert thread.start_time == '07:20'
    assert thread.title == 'Тверь - Москва'
    assert thread.carrier.title == 'Автобус+'
    assert_that(thread.dates, contains_inanyorder('2020-08-05', '2020-08-06'))

    assert_that(thread.stops, contains(
        has_properties({
            'code': '1375',
            'arrival_shift': None,
            'departure_shift': '0'
        }),
        has_properties({
            'code': '10787',
            'arrival_shift': '1199',
            'departure_shift': '1200'
        }),
        has_properties({
            'code': '10668',
            'arrival_shift': '4200',
            'departure_shift': None
        }),
    ))

    assert_that(thread.prices, contains_inanyorder(
        has_entries({
            'value': '150',
            'stop_from': has_property('code', '1375'),
            'stop_to': has_property('code', '10787')
        }),
        has_entries({
            'value': '200',
            'stop_from': has_property('code', '1375'),
            'stop_to': has_property('code', '10668')
        })
    ))

    thread = threads[1]
    assert thread.start_time == '13:20'
    assert thread.title == 'Тверь - Ржев'
    assert_that(thread.dates, contains_inanyorder('2020-08-06'))

    assert_that(thread.stops, contains(
        has_properties({
            'code': '1375',
            'arrival_shift': None,
            'departure_shift': '0'
        }),
        has_properties({
            'code': '10787',
            'arrival_shift': '4800',
            'departure_shift': None
        }),
    ))

    assert_that(thread.prices, contains(
        has_entries({
            'value': '146',
            'stop_from': has_property('code', '1375'),
            'stop_to': has_property('code', '10787')
        })
    ))

    thread = threads[2]
    assert thread.start_time == '12:20'
    assert thread.title == 'Тверь - Ржев'
    assert thread.carrier.title == 'Автобус++'
    assert_that(thread.dates, contains_inanyorder('2020-08-05', '2020-08-06'))

    assert_that(thread.stops, contains(
        has_properties({
            'code': '1375',
            'arrival_shift': None,
            'departure_shift': '0'
        }),
        has_properties({
            'code': '10787',
            'arrival_shift': '4800',
            'departure_shift': None
        }),
    ))

    assert_that(thread.prices, contains(
        has_entries({
            'value': '150',
            'stop_from': has_property('code', '1375'),
            'stop_to': has_property('code', '10787')
        })
    ))
