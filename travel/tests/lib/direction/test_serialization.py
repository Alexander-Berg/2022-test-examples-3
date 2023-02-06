# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

from datetime import date

import hamcrest
import pytest
from rest_framework import serializers

from common.models.currency import Price
from common.tester.factories import create_station, create_transport_subtype
from common.tester.utils.datetime import replace_now
from common.tester.utils.replace_setting import replace_setting
from travel.rasp.wizards.proxy_api.lib.direction.models import DirectionData, DirectionQuery, Segment
from travel.rasp.wizards.proxy_api.lib.direction.serialization import dump_direction_segments, load_direction_query, load_direction_data
from travel.rasp.wizards.proxy_api.lib.segments.models import Thread, Urls
from travel.rasp.wizards.proxy_api.lib.tests_utils import make_direction_query, make_direction_response_body, make_segment
from travel.rasp.wizards.wizard_lib.experiment_flags import ExperimentFlag
from travel.rasp.wizards.wizard_lib.tests_utils import make_dummy_segment, msk_dt, utc_dt
from travel.rasp.wizards.wizard_lib.serialization.limit import DEFAULT_SEGMENTS_LIMIT
from travel.rasp.wizards.wizard_lib.serialization.thread_express_type import ThreadExpressType

pytestmark = pytest.mark.dbuser


@replace_now('2000-01-01')
def test_load_direction_query():
    departure_point = create_station()
    arrival_point = create_station()

    assert load_direction_query(
        departure_point,
        arrival_point,
        'some_transport_code',
        frozenset([ExperimentFlag.EXPERIMENTAL_SEARCH]),
        {'date': '2000-01-01', 'number': '123', 'lang': 'uk', 'tld': 'com'}
    ) == DirectionQuery(
        departure_point=departure_point,
        arrival_point=arrival_point,
        transport_code='some_transport_code',
        departure_date=date(2000, 1, 1),
        thread_express_type=None,
        thread_number='123',
        language='uk',
        experiment_flags=frozenset([ExperimentFlag.EXPERIMENTAL_SEARCH]),
        tld='com',
        limit=DEFAULT_SEGMENTS_LIMIT,
        main_reqid=None,
    )


@replace_now('2000-01-01')
def test_load_direction_query_with_limit():
    departure_point = create_station()
    arrival_point = create_station()

    assert load_direction_query(
        departure_point,
        arrival_point,
        'some_transport_code',
        frozenset([ExperimentFlag.EXPERIMENTAL_SEARCH]),
        {'date': '2000-01-01', 'number': '123', 'lang': 'uk', 'tld': 'com', 'limit': '10'}
    ) == DirectionQuery(
        departure_point=departure_point,
        arrival_point=arrival_point,
        transport_code='some_transport_code',
        departure_date=date(2000, 1, 1),
        thread_express_type=None,
        thread_number='123',
        language='uk',
        experiment_flags=frozenset([ExperimentFlag.EXPERIMENTAL_SEARCH]),
        tld='com',
        limit=10,
        main_reqid=None,
    )


@replace_now('2000-01-01')
def test_load_direction_query_with_incorrect_limit():
    departure_point = create_station()
    arrival_point = create_station()

    with pytest.raises(serializers.ValidationError):
        load_direction_query(
            departure_point,
            arrival_point,
            'some_transport_code',
            frozenset([ExperimentFlag.EXPERIMENTAL_SEARCH]),
            {'date': '2000-01-01', 'number': '123', 'lang': 'uk', 'tld': 'com', 'limit': 'not a numer'}
        )


@replace_now('2000-01-01')
def test_load_direction_query_with_limit():
    departure_point = create_station()
    arrival_point = create_station()

    assert load_direction_query(
        departure_point,
        arrival_point,
        'some_transport_code',
        frozenset(),
        {'date': '2000-01-01', 'number': '123', 'lang': 'uk', 'tld': 'com', 'limit': '7'}
    ) == DirectionQuery(
        departure_point=departure_point,
        arrival_point=arrival_point,
        transport_code='some_transport_code',
        departure_date=date(2000, 1, 1),
        thread_express_type=None,
        thread_number='123',
        language='uk',
        experiment_flags=frozenset(),
        tld='com',
        limit=7,
        main_reqid=None,
    )


def test_load_direction_query_thread_express_type():
    departure_point = create_station()
    arrival_point = create_station()

    assert load_direction_query(
        departure_point, arrival_point, None, frozenset(), {'query': 'not_express_type'}
    ).thread_express_type is None
    assert load_direction_query(
        departure_point, arrival_point, None, frozenset(), {'query': 'express'}
    ).thread_express_type is ThreadExpressType.EXPRESS


@replace_setting('MORDA_HOST_BY_TLD', {'ru': 'rasp.yandex.ru'})
@replace_setting('TOUCH_HOST_BY_TLD', {'ru': 't.rasp.yandex.ru'})
def test_load_direction_data(rur):
    departure_point = create_station()
    arrival_point = create_station()
    transport_subtype = create_transport_subtype(t_type='suburban')
    raw_direction_data = make_direction_response_body(
        segments=(
            make_dummy_segment(
                departure_point, arrival_point, price=Price(100), transport_subtype_id=transport_subtype.id
            ),
            make_dummy_segment(
                departure_point, arrival_point
            ),
        ),
        query=make_direction_query(departure_point, arrival_point)
    )

    assert load_direction_data(raw_direction_data) == DirectionData(
        segments=(
            Segment(
                thread=Thread(
                    company=None,
                    express_type=None,
                    number='some_number',
                    start_date=date(2000, 1, 1),
                    title='some_title',
                    transport_subtype=transport_subtype,
                ),
                urls=Urls(
                    desktop=hamcrest.match_equality(hamcrest.starts_with('https://rasp.yandex.ru/')),
                    mobile=hamcrest.match_equality(hamcrest.starts_with('https://t.rasp.yandex.ru/'))
                ),
                departure_station=departure_point,
                departure_local_dt=msk_dt(2000, 1, 1),
                arrival_station=arrival_point,
                arrival_local_dt=msk_dt(2000, 1, 2),
                price=Price(100)
            ),
            Segment(
                thread=Thread(
                    company=None,
                    express_type=None,
                    number='some_number',
                    start_date=date(2000, 1, 1),
                    title='some_title',
                    transport_subtype=None,
                ),
                urls=Urls(
                    desktop=hamcrest.match_equality(hamcrest.starts_with('https://rasp.yandex.ru/')),
                    mobile=hamcrest.match_equality(hamcrest.starts_with('https://t.rasp.yandex.ru/'))
                ),
                departure_station=departure_point,
                departure_local_dt=msk_dt(2000, 1, 1),
                arrival_station=arrival_point,
                arrival_local_dt=msk_dt(2000, 1, 2),
                price=None
            ),
        ),
        total=2
    )


def test_dump_direction_segments():
    departure_station = create_station(title='some_departure_station')
    arrival_station = create_station(title='some_arrival_station')

    assert dump_direction_segments(
        (
            make_segment(
                departure_station=departure_station,
                arrival_station=arrival_station,
                departure_local_dt=utc_dt(2000, 1, 1, 1),
                arrival_local_dt=utc_dt(2000, 1, 1, 12),
                price=Price(500),
                thread=Thread(
                    company=None,
                    express_type=None,
                    number='some_number',
                    start_date=date(2000, 1, 1),
                    title='some_title',
                    transport_subtype=create_transport_subtype(t_type='suburban', title_ru='Подтип'),
                ),
                urls=Urls(desktop='http://desktop', mobile='http://mobile')
            ),
        )
    ) == (
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
                'value': 500.0
            },
            'title': 'some_title',
            'touch_url': 'http://mobile?from=wrasppp',
            'transport_subtype_title': 'Подтип',
            'url': 'http://desktop?from=wrasppp'
        },
    )
