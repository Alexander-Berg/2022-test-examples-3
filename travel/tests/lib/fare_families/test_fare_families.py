# -*- coding: utf-8 -*-
import mock

from travel.avia.ticket_daemon_api.tests.daemon_tester import create_query
from travel.avia.ticket_daemon_api.jsonrpc.lib.fare_families.fare_families import FareFamilies, _fill_rules
from travel.avia.library.python.ticket_daemon.memo import reset_all_caches
from travel.avia.ticket_daemon_api.tests.lib.fare_families import (
    get_mocked_company_tariffs, get_mocked_xpath_expressions,
    DEFAULT_RULE, XPATH_RULE
)
from travel.avia.library.python.tester.factories import create_airport, create_country
from travel.avia.library.python.tester.testcase import TestCase


class FareFamiliesTests(TestCase):
    def setUp(self):
        reset_all_caches()
        self.country = create_country(code='ZZ')
        self.from_station = create_airport(iata='SVO', country=self.country)
        self.to_station = create_airport(iata='JFK', country=self.country)
        self._flight = {
            'company': 0,
            'from': self.from_station.id,
            'to': self.to_station.id,
            'key': 'flight_key',
            'arrival': {
                'local': '2020-10-20T17:27:00',
                'tzname': 'Europe/Moscow',
                'offset': 180,
            },
        }
        self.query = create_query()

    def test_default_rule(self):
        terms_with_default_rule = [
            {
                'code': 'term1_code',
                'rules': [
                    DEFAULT_RULE
                ]
            }
        ]
        ff = FareFamilies()

        with mock.patch(
            'travel.avia.ticket_daemon_api.jsonrpc.lib.fare_families.fare_families.get_tariffs',
            return_value=get_mocked_company_tariffs(terms_with_default_rule)
        ):
            tariff = ff.get_tariff('CODE', self._flight, self.query)

        assert tariff['key'] == '0;ECONOMY;PROMO;term1_code=0'
        assert tariff['terms'][0]['rule'] == DEFAULT_RULE

    def test_business_tariff_default_rule(self):
        terms_with_default_rule = [
            {
                'code': 'term1_code',
                'rules': [
                    DEFAULT_RULE
                ]
            }
        ]
        tariffs = get_mocked_company_tariffs(terms_with_default_rule)
        business_tariff = dict(tariffs[0], base_class='BUSINESS')
        tariffs.append(business_tariff)
        self.query.klass = 'business'
        ff = FareFamilies()

        with mock.patch(
            'travel.avia.ticket_daemon_api.jsonrpc.lib.fare_families.fare_families.get_data_for_all_companies',
            return_value={0: tariffs}
        ):
            tariff = ff.get_tariff('CODE', self._flight, self.query)

        assert tariff['key'] == '0;BUSINESS;PROMO;term1_code=0'
        assert tariff['terms'][0]['rule'] == DEFAULT_RULE

    def test_xpath_rule(self):
        terms = [
            {
                'code': 'term1_code',
                'rules': [
                    XPATH_RULE,
                    DEFAULT_RULE
                ]
            }
        ]
        ff = FareFamilies()

        with mock.patch(
            'travel.avia.ticket_daemon_api.jsonrpc.lib.fare_families.fare_families.get_tariffs',
            return_value=get_mocked_company_tariffs(terms)
        ):
            tariff = ff.get_tariff('CODE', self._flight, self.query)

        assert tariff['key'] == '0;ECONOMY;PROMO;term1_code=0'
        assert tariff['terms'][0]['rule'] == XPATH_RULE

    def test_external_xpath_ref(self):
        xpath_ref_rule = {
            'external_xpath_ref': 'TEST_EX_1',
        }
        terms = [
            {
                'code': 'term1_code',
                'rules': [
                    xpath_ref_rule,
                    DEFAULT_RULE,
                ],
            }
        ]
        ff = FareFamilies()

        with mock.patch(
            'travel.avia.ticket_daemon_api.jsonrpc.lib.fare_families.fare_families.get_tariffs',
            return_value=get_mocked_company_tariffs(terms)
        ):
            with mock.patch(
                'travel.avia.ticket_daemon_api.jsonrpc.lib.fare_families.fare_families.get_xpath_expressions',
                return_value=get_mocked_xpath_expressions()
            ):
                tariff = ff.get_tariff('CODE', self._flight, self.query)

        assert tariff['terms'][0]['rule'] == xpath_ref_rule

    def test_ignore_rule(self):

        xpath_ref_rule = {
            'external_xpath_ref': 'UNKNOWN_REF',
            'ignore': True,
        }
        terms = [
            {
                'code': 'term1_code',
                'rules': [
                    xpath_ref_rule,
                    DEFAULT_RULE,
                ],
            }
        ]
        tariffs = get_mocked_company_tariffs(terms)
        with mock.patch(
            'travel.avia.ticket_daemon_api.jsonrpc.lib.fare_families.fare_families.get_xpath_expressions',
            return_value=get_mocked_xpath_expressions()
        ):
            _fill_rules(tariffs, 'CODE')

        assert len(tariffs) == 1
        assert len(tariffs[0]['terms'][0]['rules']) == 1
        assert tariffs[0]['terms'][0]['rules'][0] == DEFAULT_RULE

    def test_get_tariffs_caching(self):
        ff = FareFamilies()
        ff.check_rule = mock.Mock(return_value=True)
        terms = [
            {
                'code': 'term1_code',
                'rules': [
                    DEFAULT_RULE
                ]
            }
        ]

        with mock.patch(
            'travel.avia.ticket_daemon_api.jsonrpc.lib.fare_families.fare_families.get_tariffs',
            return_value=get_mocked_company_tariffs(terms)
        ):
            ff.get_tariff('CODE', self._flight, self.query)
            assert ff.check_rule.call_count == 1
            ff.get_tariff('CODE', self._flight, self.query)
            assert ff.check_rule.call_count == 1
