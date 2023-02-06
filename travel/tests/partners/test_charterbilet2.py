# -*- coding: utf-8 -*-
from datetime import date, datetime

import mock

from travel.avia.ticket_daemon.ticket_daemon.lib.currency import Price
from travel.avia.ticket_daemon.tests.partners.helper import (
    get_mocked_response, create_flight, get_query, assert_variants_equal,
    ComparableVariant, SettlementMock)
from travel.avia.ticket_daemon.ticket_daemon.partners import charterbilet2


@mock.patch('requests.post', return_value=get_mocked_response('charterbilet.xml'))
def test_charterbilet_query(mocked_request):
    expected_variants = [
        ComparableVariant(
            forward=[create_flight(
                **{
                    'station_from_iata': 'VKO',
                    'local_departure': datetime(2017, 1, 24, 22, 50),
                    'company_iata': 'FV',
                    'number': 'FV 5883',
                    'local_arrival': datetime(2017, 1, 25, 8, 30),
                    'station_to_iata': 'GOI',
                }
            )],
            backward=[create_flight(
                **{
                    'station_from_iata': 'GOI',
                    'local_departure': datetime(2017, 2, 9, 10, 30),
                    'company_iata': 'FV',
                    'number': 'FV 5884',
                    'local_arrival': datetime(2017, 2, 9, 15, 25),
                    'station_to_iata': 'VKO',
                }
            )],
            klass='economy',
            order_data={'url': 'http://www.charterbilet.ru/from_search.php?offer_id=913670&adult=1&child=0&infant=0'},
            tariff=Price(currency='RUR', value=29640.91),
            charter=True,
        ),
    ]
    test_query = get_query(
        point_to=SettlementMock(iata='GOI', code='IN', id=26812),
        date_forward=date(2017, 1, 24),
        date_backward=date(2017, 2, 9),
    )
    variants = next(charterbilet2.query(test_query))
    assert_variants_equal(expected_variants, variants)
