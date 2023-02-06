# -*- coding: utf-8 -*-
import datetime

import mock

from travel.avia.ticket_daemon.ticket_daemon.lib.currency import Price
from travel.avia.ticket_daemon.tests.partners.helper import (
    get_mocked_response, create_flight, get_query, assert_variants_equal,
    ComparableVariant, SettlementMock)
from travel.avia.ticket_daemon.ticket_daemon.partners import charterok


@mock.patch('requests.post', return_value=get_mocked_response('charterok.xml'))
def test_charterok_query(mocked_request):
    expected_variants = [
        ComparableVariant(
            forward=[create_flight(**{
                'charter': False,
                'station_from_iata': 'DME',
                'local_departure': datetime.datetime(2017, 1, 21, 16, 15),
                'company_iata': 'WZ',
                'number': 'WZ 301',
                'local_arrival': datetime.datetime(2017, 1, 21, 18, 50),
                'station_to_iata': 'SIP',
            })],
            backward=[create_flight(**{
                'charter': False,
                'station_from_iata': 'SIP',
                'local_departure': datetime.datetime(2017, 2, 9, 20, 10),
                'company_iata': 'WZ',
                'number': 'WZ 302',
                'local_arrival': datetime.datetime(2017, 2, 9, 22, 50),
                'station_to_iata': 'DME',
            })],
            klass='economy',
            order_data={'infants': 0, 'tariff_type': 'RT', 'variant': '23090238', 'adults': 1, 'children': 0},
            tariff=Price(currency='RUR', value=6000.0),
            charter=False,
        ),
        ComparableVariant(
            forward=[create_flight(**{
                'charter': False,
                'station_from_iata': 'DME',
                'local_departure': datetime.datetime(2017, 1, 21, 16, 15),
                'company_iata': 'WZ',
                'number': 'WZ 301',
                'local_arrival': datetime.datetime(2017, 1, 21, 18, 50),
                'station_to_iata': 'SIP',
            })],
            backward=[create_flight(**{
                'charter': False,
                'station_from_iata': 'SIP',
                'local_departure': datetime.datetime(2017, 2, 16, 20, 10),
                'company_iata': 'WZ',
                'number': 'WZ 302',
                'local_arrival': datetime.datetime(2017, 2, 16, 22, 50),
                'station_to_iata': 'DME',
            })],
            klass='economy',
            order_data={'infants': 0, 'tariff_type': 'RT', 'variant': '23092259', 'adults': 1, 'children': 0},
            tariff=Price(currency='RUR', value=6000.0),
            charter=False,
        ),
    ]
    test_query = get_query(
        point_to=SettlementMock(iata='SIP', code='RU', id=9600396),
    )
    variants = list(charterok.query(test_query))
    assert_variants_equal(expected_variants, variants[0])
