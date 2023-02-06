# -*- coding: utf-8 -*-
import json
import mock
import os.path

from django.utils.encoding import force_text
from library.python import resource

from travel.avia.ticket_daemon_api.jsonrpc.lib.fare_families import get_all_resources, FARE_FAMILIES_PATH
from travel.avia.ticket_daemon_api.jsonrpc.lib.fare_families.data_validator import FareFamiliesDataValidator
from travel.avia.ticket_daemon_api.tests.lib.fare_families import (
    get_mocked_company_tariffs, get_mocked_xpath_expressions,
    get_terms, DEFAULT_RULE, XPATH_RULE
)
from travel.avia.library.python.tester.testcase import TestCase


get_xpath_expressions_method = (
    'travel.avia.ticket_daemon_api.jsonrpc.lib.fare_families'
    '.data_validator'
    '.get_xpath_expressions'
)
results_path = os.path.join(
    'resfs/file',
    os.path.dirname(__file__),
    'fixtures',
    'expected_results.json',
)


def assert_expected_errors(test_function):
    def wrapper(test_case):
        errors = test_function(test_case)
        key = test_function.__name__[len('test_'):]

        assert set(test_case.expected_results[key]) == set(errors)

    return wrapper


class DataValidatorTests(TestCase):
    def setUp(self):
        content = force_text(resource.find(results_path))
        self.expected_results = json.loads(content)

    @assert_expected_errors
    def test_default_rule_must_be_last(self):
        return self._mock_fare_families_terms([
            DEFAULT_RULE,
            XPATH_RULE
        ])

    @assert_expected_errors
    def test_many_default_rules(self):
        return self._mock_fare_families_terms([
            XPATH_RULE,
            DEFAULT_RULE,
            DEFAULT_RULE,
        ])

    @assert_expected_errors
    def test_invalid_xpath(self):
        return self._mock_fare_families_terms([
            XPATH_RULE,
            {'xpath': '/leg/'}
        ])

    @assert_expected_errors
    def test_filename_must_contain_company_id(self):
        return self._mock_fare_families_terms(
            filename='nocompanyid.json'
        )

    @assert_expected_errors
    def test_company_id_in_filename_must_be_integer(self):
        return self._mock_fare_families_terms(
            filename='f_ff.json'
        )

    @assert_expected_errors
    def test_unknown_external_xpath_ref(self):
        return self._mock_ext_xpaths_and_fare_families(
            terms=[
                {'external_xpath_ref': 'UNKNOWN_REF'},
                {'external_xpath_ref': 'TEST_EX_1'},
            ]
        )

    @assert_expected_errors
    def test_invalid_external_xpath(self):
        return self._mock_ext_xpaths_and_fare_families(
            terms=[
                {'external_xpath_ref': 'TEST_EX_1'},
                {'external_xpath_ref': 'TEST_EX_2'},
            ]
        )

    @assert_expected_errors
    def test_ignore_rules(self):
        return self._mock_ext_xpaths_and_fare_families(
            terms=[
                {
                    'external_xpath_ref': 'UNKNOWN_REF',
                    'ignore': True
                },
                {
                    'xpath': 'error/',
                    'ignore': True
                }
            ]
        )

    @assert_expected_errors
    def test_empty_required_field(self):
        empty_tariff_info = {
            'base_class': '',
            'tariff_code_pattern': '',
            'tariff_group_name': {},
            'brand': {},
            'terms': [],
        }

        return self._mock_fare_family(json.dumps([empty_tariff_info]), filename='1_ff.json')

    @assert_expected_errors
    def test_invalid_tariff_file_format(self):
        bad_tariff_info = '{"key": "value",}'
        return self._mock_fare_family(bad_tariff_info, filename='1_ff.json')

    @assert_expected_errors
    def test_not_unique_fare_family(self):
        not_unique_fare_family = {
            'base_class': 'ECONOMY',
            'tariff_code_pattern': 'flex',
            'tariff_group_name': {'ru': u'Гибкий', 'en': 'flex'},
            'brand': 'FLEX',
            'terms': [{
                'code': 'test',
                'rules': [DEFAULT_RULE],
            }],
        }
        return self._mock_fare_family(
            json.dumps([not_unique_fare_family, not_unique_fare_family]),
            filename='1_ff.json',
        )

    @assert_expected_errors
    def test_invalid_tariff_code_pattern(self):
        invalid_tariff_code_pattern_fare_family = {
            'base_class': 'ECONOMY',
            'tariff_code_pattern': '^(BASE|STD)|BSE|BAS)\\w*\\d*$',
            'tariff_group_name': {'ru': u'Базовый', 'en': 'Base'},
            'brand': 'BASE',
            'terms': [{
                'code': 'test',
                'rules': [DEFAULT_RULE],
            }],
        }
        return self._mock_fare_family(
            json.dumps([invalid_tariff_code_pattern_fare_family]),
            filename='1_ff.json',
        )

    @staticmethod
    def _mock_fare_family(tariff_info, filename='1_ff.json'):
        with mock.patch.object(FareFamiliesDataValidator, '_get_content', return_value=tariff_info):
            import travel.avia.ticket_daemon_api.jsonrpc.lib.fare_families.data_validator
            with mock.patch.object(travel.avia.ticket_daemon_api.jsonrpc.lib.fare_families.data_validator,
                                   'get_all_resources', return_value={filename: filename}):
                validation_result = FareFamiliesDataValidator.validate()
        return validation_result

    def _mock_fare_families_terms(self, terms=(), filename='1_ff.json'):
        terms = get_terms(terms)
        tariff_info = get_mocked_company_tariffs(terms)
        return self._mock_fare_family(json.dumps(tariff_info), filename)

    def _mock_ext_xpaths_and_fare_families(self, terms=(), filename='1_ff.json'):
        with mock.patch(
            get_xpath_expressions_method,
            side_effect=get_mocked_xpath_expressions
        ):
            return self._mock_fare_families_terms(terms=terms, filename=filename)


def test_fare_families_data_correctness():
    errors = FareFamiliesDataValidator.validate()

    assert not errors, '\nErrors count: %d\n' % len(errors) + '\n'.join(errors)


def test_have_fare_families():
    assert get_all_resources(FARE_FAMILIES_PATH)
