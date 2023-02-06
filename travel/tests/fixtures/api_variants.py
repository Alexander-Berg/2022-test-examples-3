# -*- coding: utf-8 -*-
import itertools
from datetime import datetime

from travel.avia.ticket_daemon_api.jsonrpc.lib.flights import Variant, IATAFlight
from travel.avia.ticket_daemon_api.jsonrpc.lib.result.collector.variants_fabric import ApiVariants


def get_default_flights():
    return {u'1804100050UT489': {
        u'arrival': {u'local': u'2018-04-10T02:15:00',
                     u'tzname': u'Europe/Moscow',
                     u'offset': 180.0},
        u'from': 9600215, u'company': 29, u'aviaCompany': 29,
        u'number': u'UT 489',
        u'departure': {u'local': u'2018-04-10T00:50:00',
                       u'tzname': u'Europe/Moscow',
                       u'offset': 180.0},
        u'to': 9600366,
        u'companyTariff': 58, u'key': u'1804100050UT489'
    }}


def get_default_fares(partner):
    forward_tags = [IATAFlight.make_flight_tag(datetime(2018, 4, 10, 0, 50), 'UT 489')]
    tag = Variant.make_tag(
        forward_tags, [], 'economy', partner.code, False
    )
    return tag, [{
        u'charter': None,
        u'route': [[u'1804100050UT489'], []], u'baggage': [[u'0d1d0p'], []],
        u'fare_codes': [[None], []],
        u'fare_families': [[None], []],
        u'fare_families_hash': 'usual_fare',
        'tag': tag,
        u'created': 1518149935, u'expire': 1518151435,
        u'tariff': {u'currency': u'RUR', u'value': 1990.0}
    }]


def get_cheap_fares(partner, tag):
    return [{
        u'charter': None,
        u'route': [[u'1804100050UT489'], []], u'baggage': [[u'0d1d0p'], []],
        u'fare_codes': [['M14NQX'], []],
        u'fare_families': [[None], []],
        u'fare_families_hash': 'cheap_fare',
        'tag': tag,
        u'created': 1518149935, u'expire': 1518151435,
        u'tariff': {u'currency': u'RUR', u'value': 1950.0}
    }]


def get_expensive_fares(partner, tag):
    return [{
        u'charter': None,
        u'route': [[u'1804100050UT489'], []], u'baggage': [[u'0d0d0p'], []],
        u'fare_codes': [['Y'], []],
        u'fare_families': [[None], []],
        u'fare_families_hash': 'expensive_fare',
        'tag': tag,
        u'created': 1518149935, u'expire': 1518151435,
        u'tariff': {u'currency': u'RUR', u'value': 1999.0}
    }]


def get_expensive_fares_with_baggage(partner, tag):
    return [{
        u'charter': None,
        u'route': [[u'1804100050UT489'], []], u'baggage': [[u'1d1d10p'], []],
        u'fare_codes': [['Y'], []],
        u'fare_families': [[None], []],
        'tag': tag,
        u'created': 1518149935, u'expire': 1518151435,
        u'tariff': {u'currency': u'RUR', u'value': 2099.0}
    }]


def get_api_variant(partner):
    tag, fares = get_default_fares(partner)
    return tag, ApiVariants(
        qid='QID',
        flights=get_default_flights(),
        fares=fares,
        status='done',
        query_time=1,
        revision=0,
    )


def get_default_cheap_variants(partner, cheap_tag):
    cheap_fares = get_cheap_fares(partner, cheap_tag)
    return ApiVariants(
        qid='QID',
        flights=get_default_flights(),
        fares=list(cheap_fares),
        status='done',
        query_time=1,
        revision=0,
    )


def get_default_expensive_and_cheap_variants(partner, cheap_tag, normal_tag, expensive_tag):
    _, normal_fares = get_default_fares(partner)
    for fare in normal_fares:
        fare['tag'] = normal_tag
    cheap_fares = get_cheap_fares(partner, cheap_tag)
    expensive_fares = get_expensive_fares(partner, expensive_tag)
    return ApiVariants(
        qid='QID',
        flights=get_default_flights(),
        fares=list(itertools.chain(normal_fares, cheap_fares, expensive_fares)),
        status='done',
        query_time=1,
        revision=0,
    )


def get_default_variants_with_and_without_baggage(partner, cheap_tag, normal_tag, with_baggage_tag):
    _, normal_fares = get_default_fares(partner)
    for fare in normal_fares:
        fare['tag'] = normal_tag
    cheap_fares = get_cheap_fares(partner, cheap_tag)
    with_baggage_fares = get_expensive_fares_with_baggage(partner, with_baggage_tag)
    return ApiVariants(
        qid='QID',
        flights=get_default_flights(),
        fares=list(itertools.chain(normal_fares, cheap_fares, with_baggage_fares)),
        status='done',
        query_time=1,
        revision=0,
    )
