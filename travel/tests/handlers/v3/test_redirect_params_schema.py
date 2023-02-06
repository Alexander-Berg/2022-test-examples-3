# coding: utf-8
from datetime import date, datetime

import pytest
from freezegun import freeze_time
from marshmallow import ValidationError

from travel.avia.library.python.tester.factories import create_settlement, create_partner
from travel.avia.ticket_daemon_api.jsonrpc.handlers.v3.scheme import RedirectDataInputSchema
from travel.avia.library.python.ticket_daemon.memo import reset_all_caches
from travel.avia.ticket_daemon_api.jsonrpc.models_utils.geo import get_point_tuple_by_key


def create_query(from_settlement, to_settlement):
    return {
        'qid': u'180209-094810-812.ticket.plane.c2_c213_2017-03-21_None_economy_1_0_0_ru.ru',
        'point_from': from_settlement.point_key,
        'point_to': to_settlement.point_key,
        'lang': u'ru',
        'date_forward': '2017-03-21',
        'service': 'ticket',
        'forward': 'SU 123.2017-03-22T12:35,SU 234.2017-03-22T16:20',
        'user_info': {},
    }


DEFAULT_RESULT = {
    'lang': u'ru',
    'passengers': {'adults': 1, 'children': 0, 'infants': 0},
    'with_baggage': None,
    'variant_test_context': None,
    'national_version': u'ru',
    'qid': u'180209-094810-812.ticket.plane.c2_c213_2017-03-21_None_economy_1_0_0_ru.ru',
    'date_backward': None,
    'user_info': {},
    'service': u'ticket',
    'book_on_yandex': True,
    'date_forward': date(2017, 3, 21),
    'forward': [
        {
            'number': u'SU 123',
            'departure_datetime': datetime(2017, 3, 22, 12, 35),
        },
        {
            'number': u'SU 234',
            'departure_datetime': datetime(2017, 3, 22, 16, 20),
        },
    ],
    'partner': u'ozon',
    'backward': None,
    'klass': u'economy',
}


@pytest.mark.dbuser
@freeze_time('2017-03-21')
def test_redirect_params_schema():
    reset_all_caches()
    from_settlement = create_settlement(id=2)
    to_settlement = create_settlement(id=213)

    ozon = create_partner(code='ozon')

    query = create_query(from_settlement, to_settlement)
    query['partner'] = ozon.code

    result = DEFAULT_RESULT
    result.update({
        'point_from': get_point_tuple_by_key(from_settlement.point_key),
        'point_to': get_point_tuple_by_key(to_settlement.point_key),
    })
    assert RedirectDataInputSchema(strict=True).load(query).data == result


@pytest.mark.dbuser
@freeze_time('2017-03-21')
def test_free_redirect():
    reset_all_caches()
    from_settlement = create_settlement(id=2)
    to_settlement = create_settlement(id=213)

    ozon = create_partner(code='ozon')

    query = create_query(from_settlement, to_settlement)
    query['partner'] = ozon.code
    query['avia_brand'] = ozon.code

    result = DEFAULT_RESULT
    result.update({
        'point_from': get_point_tuple_by_key(from_settlement.point_key),
        'point_to': get_point_tuple_by_key(to_settlement.point_key),
        'avia_brand': u'ozon',
    })
    assert RedirectDataInputSchema(strict=True).load(query).data == result


@pytest.mark.dbuser
@freeze_time('2017-03-21')
def test_redirect_params_schema_with_wrong_partner():
    reset_all_caches()
    from_settlement = create_settlement(id=2)
    to_settlement = create_settlement(id=213)

    query = create_query(from_settlement, to_settlement)
    query['partner'] = 'ozon and ozon'

    with pytest.raises(ValidationError) as e:
        RedirectDataInputSchema(strict=True).load(query)

    assert e.value.messages == {'partner': ['Wrong partner code format']}
