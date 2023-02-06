# -*- coding: utf-8 -*-
from mock import patch

from travel.avia.library.python.tester.factories import create_partner, create_station, create_settlement
from travel.avia.library.python.tester.testcase import TestCase

from travel.avia.ticket_daemon_api.tests.daemon_tester import create_query
from travel.avia.ticket_daemon_api.jsonrpc.handlers.rasp.views import _serialize_rasp_results
from travel.avia.library.python.ticket_daemon.memo import reset_all_caches
from travel.avia.ticket_daemon_api.jsonrpc.rasp.complete_results import rasp_complete_results
from travel.avia.ticket_daemon_api.tests.fixtures.api_variants import get_api_variant


class TestRaspResultsFormatsCompatibility(TestCase):
    maxDiff = None
    ORDER_URL = 'https://order.url'
    REDIRECT_URL = 'https://redirect.url'
    FLIGHT_URL = 'https://flight.url'

    def setUp(self):
        reset_all_caches()
        self.partner = create_partner(code='test_partner', title='TEST_PARTNER')
        self.settlement_from = create_settlement(id=213, title='settlement_from')
        self.settlement_to = create_settlement(id=2, title='settlement_to')
        self.station_from = create_station(id=9600215, title='station_from', settlement_id=self.settlement_from.id)
        self.station_to = create_station(id=9600366, title='station_to', settlement_id=self.settlement_to.id)

        self.order_url = 'order_link'
        self.redirect_url = ''

    @patch('travel.avia.ticket_daemon_api.jsonrpc.rasp.complete_results._avia_order_link', return_value=ORDER_URL)
    @patch('travel.avia.ticket_daemon_api.jsonrpc.rasp.complete_results._avia_deep_link', return_value=REDIRECT_URL)
    @patch('travel.avia.ticket_daemon_api.jsonrpc.lib.flights.IATAFlight.url', return_value=FLIGHT_URL)
    def test_format(self, *mocks):
        q = create_query()

        tag, partner_result = get_api_variant(self.partner)

        completed_result = rasp_complete_results(q, {self.partner.code: partner_result})
        expected_result = {
            'status': {},
            'variants': [{
                'from_company': False,
                 'raw_tariffs': {},
                 'query_time': 1,
                 'deep_link': self.REDIRECT_URL,
                 'order_link': self.ORDER_URL,
                 'forward': 'UT 489.0410T0050',
                 'partner': 'test_partner',
                 'backward': '',
                 'tariff': {u'currency': u'RUR', u'value': 1990.0},
                 'order_data': {}
            }],
            'expired_date': '2018-02-09T04:43:55+00:00',
            'reference': {
                'itineraries': {
                    '': [],
                    'UT 489.0410T0050': ['UT 489-29-9600215-9600366-04100050-04100215']
                },
                'partners': [{
                    'code': u'test_partner',
                    'logoSvg': None,
                    'title': u'TEST_PARTNER'
                }],
                'flights': [{
                    'arrival': {
                        'local': '2018-04-10 02:15:00',
                        'tzname': u'Europe/Moscow'
                    },
                    'station_to': 9600366,
                    't_type_code': 'plane',
                    'key': 'UT 489-29-9600215-9600366-04100050-04100215',
                    'station_from': 9600215,
                    'url': self.FLIGHT_URL,
                    'supplier_code': None,
                    'company': 29,
                    'number': u'UT 489',
                    'departure': {
                        'local': '2018-04-10 00:50:00',
                        'tzname': u'Europe/Moscow'
                    }
                }],
                'settlements': [2, 213],
                'stations': [9600366, 9600215]
            }
        }

        self.assertDictEqual(
            expected_result, _serialize_rasp_results(q, completed_result, {})
        )

    def test_empty_datum(self):
        q = create_query()
        datum = {}
        result = rasp_complete_results(q, datum)
        expected_result = {
            'variants': [],
            'expired_date': None,
            'reference': {
                'itineraries': {},
                'partners': [],
                'flights': [],
                'settlements': [],
                'stations': []
            }}
        self.assertDictEqual(expected_result, result)
