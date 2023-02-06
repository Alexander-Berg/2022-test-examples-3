# -*- coding: utf-8 -*-
import datetime
from itertools import product

import mock

from travel.avia.ticket_daemon.ticket_daemon.lib.currency import Price
from travel.avia.ticket_daemon.tests.partners.helper import (
    get_mocked_response, create_flight, get_query, assert_variants_equal,
    ComparableVariant,
)
from travel.avia.ticket_daemon.ticket_daemon.partners import biletix2


@mock.patch('requests.post', return_value=get_mocked_response('biletix2.xml'))
def test_biletix2_query(mocked_request):
    flights_to = [
        {'station_from_iata': 'DME',
         'local_departure': datetime.datetime(2017, 1, 21, 14, 50),
         'company_iata': 'S7', 'number': 'S7 55',
         'local_arrival': datetime.datetime(2017, 1, 21, 19, 10),
         'station_to_iata': 'SVX', 'baggage': '0pc',
         'fare_code': 'WBSMOWR'},
        {'station_from_iata': 'DME',
         'local_departure': datetime.datetime(2017, 1, 21, 23, 35),
         'company_iata': 'S7', 'number': 'S7 51',
         'local_arrival': datetime.datetime(2017, 1, 22, 3, 55),
         'station_to_iata': 'SVX', 'baggage': '0pc',
         'fare_code': 'WBSMOWR'}
    ]
    flights_back = [
        {'station_from_iata': 'SVX',
         'local_departure': datetime.datetime(2017, 1, 24, 6, 10),
         'company_iata': 'S7', 'number': 'S7 52',
         'local_arrival': datetime.datetime(2017, 1, 24, 6, 40),
         'station_to_iata': 'DME', 'baggage': '0pc',
         'fare_code': 'WBSMOWR'},
        {'station_from_iata': 'SVX',
         'local_departure': datetime.datetime(2017, 1, 24, 15, 40),
         'company_iata': 'S7', 'number': 'S7 54',
         'local_arrival': datetime.datetime(2017, 1, 24, 16, 10),
         'station_to_iata': 'DME', 'baggage': '0pc',
         'fare_code': 'WBSMOWR'},
        {'station_from_iata': 'SVX',
         'local_departure': datetime.datetime(2017, 1, 24, 19, 55),
         'company_iata': 'S7', 'number': 'S7 56',
         'local_arrival': datetime.datetime(2017, 1, 24, 20, 25),
         'station_to_iata': 'DME', 'baggage': '0pc',
         'fare_code': 'WBSMOWR'}
    ]

    variants_permutations = [ComparableVariant(
        forward=[create_flight(**to)],
        backward=[create_flight(**back)],
        klass='economy',
        tariff=Price(currency='RUR', value=6313.0)
    ) for to, back in product(flights_to, flights_back)]
    expected_variants = variants_permutations + [
        ComparableVariant(
            forward=[create_flight(
                **{
                    'station_from_iata': 'DME',
                    'local_departure': datetime.datetime(2017, 1, 21, 10, 0),
                    'company_iata': 'U6',
                    'number': 'U6 261',
                    'local_arrival': datetime.datetime(2017, 1, 21, 14, 20),
                    'station_to_iata': 'SVX',
                    'baggage': '1pc',
                    'fare_code': 'WBSMOWR',
                }
                )],
            backward=[create_flight(
                **{
                    'station_from_iata': 'SVX',
                    'local_departure': datetime.datetime(2017, 1, 24, 6, 50),
                    'company_iata': 'U6',
                    'number': 'U6 264',
                    'local_arrival': datetime.datetime(2017, 1, 24, 7, 20),
                    'station_to_iata': 'DME',
                    'baggage': '1pc',
                    'fare_code': 'WBSMOWR',
                }
                )],
            klass='economy',
            order_data={'url': 'http://search.biletix.ru/booking/?message=eyJ3IjoiTU9XIiwiZSI6IlNWWCIsInIiOiJSVCIsInQiOiIyMS4wMS4yMDE3IiwieSI6IjI0LjAxLjIwMTciLCJ1IjoiMSIsImkiOiIwIiwibyI6IjAiLCJwIjoiIiwiYSI6IiIsInMiOiIiLCJkIjoiIiwiZiI6IiIsImciOiIiLCJoIjoiXHUwNDJkIiwicSI6IjM0NDk2IiwiaiI6ImRlZmF1bHQiLCJrIjoxNDgzMTAxMzI3LCJsIjo0MzU0MiwibiI6IjY0MzI4NzExNVwvODAwNSJ9&flightout=eyJ4IjoiVTYiLCJjIjoiMjEuMDEuMjAxNyIsInYiOiJRRkxSVCIsImIiOiIyNjEifQ&utm_source=yandex.ru&utm_medium=referral&utm_campaign=metasearch&flightback=eyJ4IjoiVTYiLCJjIjoiMjQuMDEuMjAxNyIsInYiOiJRRkxSVCIsImIiOiIyNjQifQ&utm_source=yandex.ru&utm_medium=referral&utm_campaign=metasearch'},  # noqa
            tariff=Price(currency='RUR', value=43542.0)
        ),
    ]

    test_query = get_query()
    variants = next(biletix2.query(test_query))
    assert_variants_equal(expected_variants, variants)
