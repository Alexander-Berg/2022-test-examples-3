# -*- coding: utf-8 -*-
from travel.avia.ticket_daemon_api.jsonrpc.lib.result.collector.variants import _prepare_wizard_variants
from travel.avia.library.python.tester.testcase import TestCase


expected_variants = [{
    'baggage': [[u'0p0p0p'], []],
    'charter': False,
    'expire': 1588240197,
    'created': 1588239597,
    'partner': u's_seven',
    'route': ((u'2005041450S71175',), ()),
    'tariff': {'currency': u'RUR', 'value': 6271.0},
}]


expected_flights = {
    u'2005041335SU20': {
        'arrival': {'local': u'2020-05-04T15:05:00', 'tzname': u'Europe/Moscow', 'offset': 180.0},
        'to': 9600366,
        'companyTariff': 60,
        'from': 9600213,
        'key': u'2005041335SU20',
        'company': 26,
        'aviaCompany': 26,
        'departure': {'local': u'2020-05-04T13:35:00', 'tzname': u'Europe/Moscow', 'offset': 180.0},
        'number': u'SU 20',
    }
}


def wizard_results_content():
    return {
        'qid': u'200430-123955-980.ticket.plane.c213_c54_2020-05-04_None_economy_1_0_0_ru.ru',
        'version': 11,
        'offers_count': 1,
        'polling_status': {
            'remaining_partners': [u's_seven'],
            'asked_partners_count': 1,
            'asked_partners': [u'pososhok'],
            'remaining_partners_count': 19,
        },
        'flights': {
            u'2005041335SU20': {
                'arrival': {'local': u'2020-05-04T15:05:00', 'tzname': u'Europe/Moscow', 'offset': 180.0},
                'to': 9600366,
                'companyTariff': 60,
                'from': 9600213,
                'key': u'2005041335SU20',
                'company': 26,
                'aviaCompany': 26,
                'departure': {'local': u'2020-05-04T13:35:00', 'tzname': u'Europe/Moscow', 'offset': 180.0},
                'number': u'SU 20',
            },
        },
        'fares': [
            {
                'baggage': [[u'0p0p0p'], []],
                'charter': False,
                'expire': 1588240197,
                'created': 1588239597,
                'conversion_partner': u's_seven',
                'partner': u's_seven',
                'route': ((u'2005041450S71175',), ()),
                'popularity': 217,
                'tariff': {'currency': u'RUR', 'value': 6271.0},
                'tariffs': {
                    'with_baggage': {
                        'conversion_partner': u's_seven',
                        'price': {'currency': u'RUR', 'value': 7371.0},
                        'baggage': [[u'1p1p23d'], []],
                        'partner': u's_seven',
                        'created_at': 1588239597,
                        'expire_at': 1588240197,
                    },
                    'without_baggage': {
                        'conversion_partner': u's_seven',
                        'price': {'currency': u'RUR', 'value': 6271.0},
                        'baggage': [[u'0p0p0p'], []],
                        'partner': u's_seven',
                        'created_at': 1588239597,
                        'expire_at': 1588240197,
                    }
                }
            },
        ],
    }


class TestWizardVariants(TestCase):
    def test(self):
        variants_by_partner = _prepare_wizard_variants(wizard_results_content())
        api_variant = variants_by_partner.values()[0]
        assert len(api_variant.variants) == 1
        self.assertDictContainsSubset(expected_variants[0], api_variant.variants[0])
        self.assertDictEqual(expected_flights, api_variant.flights)
        assert api_variant.query_time == 0
