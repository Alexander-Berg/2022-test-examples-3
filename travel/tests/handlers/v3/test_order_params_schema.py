# coding: utf-8
from datetime import date, datetime

import pytest
from freezegun import freeze_time
from marshmallow import ValidationError

from travel.avia.ticket_daemon_api.jsonrpc.handlers.v3.scheme import OrderParamsSchema
from travel.avia.library.python.ticket_daemon.memo import reset_all_caches
from travel.avia.ticket_daemon_api.jsonrpc.models_utils.geo import get_point_tuple_by_key
from travel.avia.library.python.tester.factories import create_settlement


def create_query(from_settlement, to_settlement):
    return {
        'point_from': from_settlement.point_key,
        'point_to': to_settlement.point_key,
        'lang': u'ru',
        'date_forward': '2017-03-21',
        'service': 'ticket',

        'klass': 'economy',
        'national_version': u'ru',

        'adults': 1,
        'children': 0,
        'infants': 1,

        'forward': 'SU 123.2017-03-22T12:35,SU 234.2017-03-22T16:20',
        'backward': 'ШИ 456.2017-03-24T00:10',

        'fare_group': '1,0;2',
    }


def create_actual(from_settlement, to_settlement):
    flights_forward = [
        {
            'departure_datetime': datetime(2017, 3, 22, 12, 35),
            'number': u'SU 123'
        },
        {
            'departure_datetime': datetime(2017, 3, 22, 16, 20),
            'number': u'SU 234'
        },
    ]
    flights_backward = [
        {
            'departure_datetime': datetime(2017, 3, 24, 0, 10),
            'number': u'ШИ 456'
        },
    ]

    return {
        'point_from': get_point_tuple_by_key(from_settlement.point_key),
        'point_to': get_point_tuple_by_key(to_settlement.point_key),

        'lang': u'ru',
        'date_forward': date(2017, 3, 21),
        'date_backward': None,
        'service': u'ticket',

        'klass': u'economy',
        'national_version': u'ru',
        'passengers': {'adults': 1, 'children': 0, 'infants': 1},

        'forward': flights_forward,
        'backward': flights_backward,
        'max_age': None,
        'ignore_outdated': False,
    }


@pytest.mark.dbuser
@freeze_time('2017-03-21')
def test_order_params_schema_without_partners():
    reset_all_caches()
    from_settlement = create_settlement()
    to_settlement = create_settlement()

    query = create_query(from_settlement, to_settlement)
    actual = create_actual(from_settlement, to_settlement)
    actual['partners'] = None

    assert OrderParamsSchema(strict=True).load(query).data == actual


@pytest.mark.dbuser
@freeze_time('2017-03-21')
def test_order_params_schema_with_partner_list():
    reset_all_caches()
    from_settlement = create_settlement()
    to_settlement = create_settlement()

    query = create_query(from_settlement, to_settlement)
    query['partners'] = 'ozon,dohop'
    actual = create_actual(from_settlement, to_settlement)
    actual['partners'] = ['ozon', 'dohop']
    actual['partner_codes'] = u'ozon,dohop'

    assert OrderParamsSchema(strict=True).load(query).data == actual


@pytest.mark.dbuser
@freeze_time('2017-03-21')
def test_order_params_schema_with_one_partner():
    reset_all_caches()
    from_settlement = create_settlement()
    to_settlement = create_settlement()

    query = create_query(from_settlement, to_settlement)
    query['partners'] = 'ozon'
    actual = create_actual(from_settlement, to_settlement)
    actual['partners'] = ['ozon']
    actual['partner_codes'] = u'ozon'

    assert OrderParamsSchema(strict=True).load(query).data == actual


@pytest.mark.dbuser
@freeze_time('2017-03-21')
def test_order_params_schema_with_wrong_partner():
    reset_all_caches()
    from_settlement = create_settlement()
    to_settlement = create_settlement()

    query = create_query(from_settlement, to_settlement)
    query['partners'] = 'ozon and ozon'

    with pytest.raises(ValidationError) as e:
        OrderParamsSchema(strict=True).load(query)

    assert e.value.messages == {'partners': ['Wrong partner code format']}
