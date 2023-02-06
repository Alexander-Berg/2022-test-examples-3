# -*- coding: utf-8 -*-
from __future__ import unicode_literals


DEFAULT_RULE = {
    'availability': 'NOT_AVAILABLE',
}

XPATH_RULE = {
    'xpath': 'Leg/Seg[FromCountry = "ZZ"]',
}


def get_mocked_company_tariffs(terms):
    return [{
        'base_class': 'ECONOMY',
        'tariff_code_pattern': '^CODE$',
        'tariff_group_name': {
            'ru': 'Эконом ПРОМО',
            'en': 'Economy PROMO'
        },
        'brand': 'PROMO',
        'terms': terms
    }]


def get_mocked_xpath_expressions(company_id='1'):
    return {
        '1': {
            'TEST_EX_1': 'Leg/Seg',
            'TEST_EX_2': 'Leg/'
        }
    }.get(company_id, {})


def get_terms(rules, code='term1_code'):
    return [{
        'code': code,
        'rules': rules,
    }]


def get_all_fare_families(terms, filename='1_ff.json'):
    yield filename, get_mocked_company_tariffs(terms)
