# -*- coding: utf-8 -*-
import datetime

import mock

from travel.avia.ticket_daemon.ticket_daemon.lib.currency import Price
from travel.avia.ticket_daemon.tests.partners.helper import (
    get_mocked_response, create_flight, get_query, assert_variants_equal,
    ComparableVariant,
)
from travel.avia.ticket_daemon.ticket_daemon.partners import pososhok


@mock.patch('requests.get', return_value=get_mocked_response('pososhok.xml'))
def test_pososhok_query(mocked_request):
    expected_variants = [
        ComparableVariant(
            forward=[create_flight(**{
                'fare_code': 'NVUR',
                'station_from_iata': 'SVO',
                'local_departure': datetime.datetime(2017, 1, 21, 0, 45),
                'company_iata': 'SU',
                'number': 'SU 1408',
                'local_arrival': datetime.datetime(2017, 1, 21, 5, 5),
                'station_to_iata': 'SVX',
            })],
            backward=[create_flight(**{
                'fare_code': None,
                'station_from_iata': 'SVX',
                'local_departure': datetime.datetime(2017, 1, 24, 6, 0),
                'company_iata': 'SU',
                'number': 'SU 1409',
                'local_arrival': datetime.datetime(2017, 1, 24, 6, 30),
                'station_to_iata': 'SVO'
            })],
            klass='economy',
            order_data={'url': 'https://www.pososhok.ru/#unixml/flight=AA4574C288098776CB8504E9623704118BA4F3DFD58D57661A19030D8B55F218&partner=yar&begin=MOW&end=SVX&departure=2017-01-21&return=2017-01-24&adults=1&class=ECONOMY&locale=ru'},  # noqa
            tariff=Price(currency='RUR', value=7000.0)
        ),
        ComparableVariant(
            forward=[
                create_flight(**{
                    'fare_code': 'LLTRT',
                    'station_from_iata': 'VKO',
                    'local_departure': datetime.datetime(2017, 1, 21, 10, 40),
                    'company_iata': 'UT',
                    'number': 'UT 161',
                    'local_arrival': datetime.datetime(2017, 1, 21, 12, 10),
                    'station_to_iata': 'GOJ',
                }),
                create_flight(**{
                    'fare_code': None,
                    'station_from_iata': 'GOJ',
                    'local_departure': datetime.datetime(2017, 1, 21, 13, 25),
                    'company_iata': 'UT',
                    'number': 'UT 112',
                    'local_arrival': datetime.datetime(2017, 1, 21, 19, 45),
                    'station_to_iata': 'SVX',
                }),
            ],
            backward=[
                create_flight(**{
                    'fare_code': None,
                    'station_from_iata': 'SVX',
                    'local_departure': datetime.datetime(2017, 1, 24, 10, 5),
                    'company_iata': 'UT', 'number': 'UT 111',
                    'local_arrival': datetime.datetime(2017, 1, 24, 12, 35),
                    'station_to_iata': 'GOJ',
                }),
                create_flight(**{
                    'fare_code': None,
                    'station_from_iata': 'GOJ',
                    'local_departure': datetime.datetime(2017, 1, 24, 13, 45),
                    'company_iata': 'UT',
                    'number': 'UT 162',
                    'local_arrival': datetime.datetime(2017, 1, 24, 15, 10),
                    'station_to_iata': 'VKO',
                }),
            ],
            klass='economy',
            order_data={'url': 'https://www.pososhok.ru/#unixml/flight=AA4574C2880987763F230935243743BEEF78B0A4D2D5C552B28C05F9327BB17498479CCEB0D59754&partner=yar&begin=MOW&end=SVX&departure=2017-01-21&return=2017-01-24&adults=1&class=ECONOMY&locale=ru'},  # noqa
            tariff=Price(currency='RUR', value=15816.0)
        ),
    ]

    test_query = get_query()
    variants = list(pososhok.query(test_query))
    assert_variants_equal(expected_variants, variants[0])
