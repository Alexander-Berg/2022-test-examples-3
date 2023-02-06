# -*- coding: utf-8 -*-

import pytest

from common.data_api.ticket_daemon.factories import create_segment
from common.data_api.ticket_daemon.serialization.avia_partner import AviaPartner
from common.data_api.ticket_daemon.serialization.itinerary import Itinerary, ItinerarySchema
from common.data_api.ticket_daemon.serialization.variant import Variant, VariantSchema, collect_stations, parse_variants
from common.models.currency import Price
from common.tester.factories import create_station

DUMMY_VARIANT_DATA = {
    'partner': 'partner code',
    'forward': None,
    'backward': None,
    'order_data': 'order_data value',
    'tariff': {'value': 100},
    'raw_seats': {'key': 10},
    'raw_tariffs': None,
    'raw_is_several_prices': {'key': True},
    'query_time': 123,
    'from_company': False
}

DUMMY_PARTNER = {
    'code': 'some_code',
    'title': 'some_code',
    'logoSvg': 'some_code',
}


def test_parse_variants():
    code_1 = 'partner 1'
    code_2 = 'partner 2'
    variants = parse_variants([
        dict(DUMMY_VARIANT_DATA, partner=code_1),
        dict(DUMMY_VARIANT_DATA, partner=code_2),
        dict(DUMMY_VARIANT_DATA, partner=code_1),
    ], {
        'partners': [
            dict(DUMMY_PARTNER, code=code_1),
            dict(DUMMY_PARTNER, code=code_2)
        ]
    })

    assert set(variants.keys()) == {code_1, code_2}
    assert len(variants[code_1]) == 2
    assert len(variants[code_2]) == 1
    assert all(variant.partner.code == code_1 for variant in variants[code_1])
    assert all(variant.partner.code == code_2 for variant in variants[code_2])


def test_variant_schema_load():
    partner = AviaPartner('some_code', 'some_title', 'some_url')
    schema = VariantSchema(context={'partners': {partner.code: partner}})
    result, errors = schema.load(dict(DUMMY_VARIANT_DATA, partner='some_code'))
    assert errors == {}
    assert result == Variant(
        forward=Itinerary(segments=[]),
        backward=Itinerary(segments=[]),
        order_data='order_data value',
        tariff=Price(value=100),
        partner=partner,
        raw_seats={'key': 10},
        raw_tariffs=None,
        raw_is_several_prices={'key': True},
        query_time=123,
        from_company=False
    )

    schema = VariantSchema(context={
        'itineraries': {
            'forward itinerary key': ['forward segment key'],
            'backward itinerary key': ['backward segment key'],
        },
        'flights_by_key': {
            'forward segment key': {'key': 'forward segment key'},
            'backward segment key': {'key': 'backward segment key'},
        },
        'partners': {partner.code: partner}
    })
    assert schema.load(dict(DUMMY_VARIANT_DATA, **{
        'partner': 'some_code',
        'forward': 'forward itinerary key',
        'backward': 'backward itinerary key',
        'raw_tariffs': {'foo': {'value': 1, 'currency': 'USD'}, 'bar': {'value': 2, 'currency': 'RUR'}},
    })).data == Variant(
        forward=Itinerary(segments=[create_segment(forward_key='forward itinerary key')]),
        backward=Itinerary(segments=[create_segment(forward_key='backward itinerary key')]),
        query_time=123,
        from_company=False,
        order_data='order_data value',
        tariff=Price(value=100),
        partner=partner,
        raw_seats={'key': 10},
        raw_tariffs={'foo': Price(value=1, currency='USD'), 'bar': Price(value=2, currency='RUR')},
        raw_is_several_prices={'key': True}
    )

    # проверям разные пустые значения
    schema = VariantSchema(context={
        'partners': {partner.code: partner}
    })
    assert schema.load(dict(DUMMY_VARIANT_DATA, **{
        'partner': 'some_code',
        'raw_tariffs': {'foo': {'value': 1}, 'bar': {'value': 2}},
    })).data == Variant(
        forward=Itinerary(segments=[]),
        backward=Itinerary(segments=[]),
        query_time=123,
        from_company=False,
        order_data='order_data value',
        tariff=Price(value=100),
        partner=partner,
        raw_seats={'key': 10},
        raw_tariffs={'foo': Price(value=1), 'bar': Price(value=2)},
        raw_is_several_prices={'key': True}
    )


@pytest.mark.dbuser
def test_collect_stations():
    station1 = create_station()
    station2 = create_station()
    station3 = create_station()
    result = collect_stations([
        {'station_from': station1.id, 'station_to': station2.id},
        {'station_from': station1.id, 'station_to': station2.id, 'first_station': station3.id},
        {'station_from': station1.id, 'station_to': station2.id, 'last_station': station3.id},
        {'station_from': station1.id},
    ])
    assert len(result) == 3
    assert result[station1.id] == station1
    assert result[station2.id] == station2
    assert result[station3.id] == station3


def test_itinerary_schema_pre_load():
    schema = ItinerarySchema(context={
        'itineraries': {
            'forward itinerary key': ['forward segment key'],
        },
        'flights_by_key': {
            'forward segment key': {'key': 'forward segment key'},
            'backward segment key': {'key': 'backward segment key'},
        },
    })
    result = schema.pre_load('forward itinerary key')
    assert result == {'segments': [{
        'key': 'forward segment key',
        'forward_key': 'forward itinerary key'
    }]}
