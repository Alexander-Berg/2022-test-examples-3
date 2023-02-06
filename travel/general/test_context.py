# -*- coding: utf-8 -*-
import base64
import itertools
import logging
import six
from datetime import datetime, timedelta

from travel.orders.proto.commons_pb2 import TAviaTestContext


log = logging.getLogger(__name__)


def parse_test_context_proto(test_context_token):
    if not test_context_token:
        return None
    test_context_token_parts = test_context_token.split(':')
    if not test_context_token_parts:
        return None
    token_bytes = base64.urlsafe_b64decode(six.ensure_str(test_context_token_parts[0]))
    tk = TAviaTestContext()
    tk.ParseFromString(token_bytes)
    return tk


def parse_test_context(test_context_token):
    try:
        result = []
        tk = parse_test_context_proto(test_context_token)
        if not tk or not tk.AviaVariants or not tk.MockAviaVariants:
            return result
        created = datetime.utcnow()
        expire = created + timedelta(hours=5)
        for tk_var in tk.AviaVariants:
            result.append(
                (
                    tk_var.Partner,
                    {
                        'all_variants_count': 1,
                        'created': _epoch(created),
                        'expire': _epoch(expire),
                        'fares': {
                            'test_fare_key': {
                                'baggage': [tk_var.BaggageForward, tk_var.BaggageBackward],
                                'charter': tk_var.Charter if tk_var.Charter else None,
                                'created': _epoch(created),
                                'expire': _epoch(expire),
                                'fare_codes': [tk_var.FareCodesForward, tk_var.FareCodesBackward],
                                'price_category': 'unknown',
                                'route': [_route_keys(tk_var.RouteForward), _route_keys(tk_var.RouteBackward)],
                                'selfconnect': tk_var.SelfConnect,
                                'tariff': {
                                    'currency': tk_var.Tariff.Currency,
                                    'value': tk_var.Tariff.Value,
                                },
                                'yandex_plus_promo': {},
                            },
                        },
                        'flights': _route_flights(tk_var.RouteForward, tk_var.RouteBackward),
                        'qid': _route_qid(tk_var.RouteForward, tk_var.RouteBackward),
                        'query_time': 1.24159,
                    },
                )
            )
        return result
    except Exception as e:
        log.exception('parse_test_context_token: %r', e)
        raise


def _route_flights(route_forward, route_backward):
    result = {}
    for flight in itertools.chain(route_forward, route_backward):
        result[_flight_key(flight)] = {
            'arrival': {
                'local': flight.Arrival.Local,
                'offset': flight.Arrival.Offset,
                'tzname': flight.Arrival.TzName,
            },
            'aviaCompany': flight.Company,
            'company': flight.Company,
            'companyTariff': flight.CompanyTariff,
            'departure': {
                'local': flight.Departure.Local,
                'offset': flight.Departure.Offset,
                'tzname': flight.Departure.TzName,
            },
            'from': flight.From,
            'key': _flight_key(flight),
            'number': flight.Number,
            'operating': {
                'company': flight.Operating.Company,
                'number': flight.Operating.Number,
            },
            'to': flight.To,
        }

    return result


def _route_keys(flight_list):
    return [_flight_key(flight) for flight in flight_list]


def _flight_key(flight):
    return '{}{}{}'.format(
        flight.Departure.Local[:16].replace('-', '').replace('T', '').replace(':', ''),
        flight.Number.replace(' ', ''),
        flight.Arrival.Local[:16].replace('-', '').replace('T', '').replace(':', ''),
    )


def _route_qid(route_forward, route_backward):
    now = datetime.utcnow().strftime('%y%m%d-%H%M%S-%j')
    backward_departure = 'None'
    if route_backward:
        backward_departure = route_backward[0].Departure.Local[:10]

    return '{}.mavia-travel.plane.s{}_s{}_{}_{}_economy_1_0_0_ru.ru'.format(
        now,
        route_forward[0].From,
        route_forward[-1].To,
        route_forward[0].Departure.Local[:10],
        backward_departure,
    )


def _epoch(utc_date):
    return int(utc_date.strftime('%s'))
