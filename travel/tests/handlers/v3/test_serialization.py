# -*- coding: utf-8 -*-
import random
from collections import namedtuple, defaultdict

import pytest
import six
from django.conf import settings
from mock import Mock, MagicMock

from travel.avia.ticket_daemon_api.jsonrpc.handlers.v3.serialization import (
    get_result_variants, _fill_book_on_yandex_availability
)
from travel.avia.ticket_daemon_api.jsonrpc.application import create_app
from travel.avia.library.python.ticket_daemon.memo import reset_all_caches
from travel.avia.ticket_daemon_api.tests.daemon_tester import create_query
from travel.avia.library.python.tester.testcase import TestCase
from travel.avia.library.python.tester.factories import create_partner


Variants = namedtuple('Variants', ('qid', 'variants', 'query_time'))


class SerializationBookOnYandexTest(TestCase):
    def setUp(self):
        reset_all_caches()
        self.booking_p_code = 'test_bookable_partner_code'
        self.settings_afl_partner_code = settings.AFL_PARTNER_CODE
        settings.AFL_PARTNER_CODE = self.booking_p_code

    def tearDown(self):
        settings.AFL_PARTNER_CODE = self.settings_afl_partner_code

    def test_get_result_variants_book_on_yandex_False(self):
        p_code = 'test_partner_code'
        create_partner(code=p_code)

        test_variant = self._test_variant()
        test_variant['route'] = [['r1'], []]
        variants = Variants('test_qid', [test_variant], 1)
        results = {
            p_code: variants
        }
        actual = get_result_variants(results)
        ab_flags = {
            'BOY_AFL_ENABLE': '1'
        }
        _fill_book_on_yandex_availability(actual, MagicMock(), MagicMock(), create_query(), ab_flags)
        for fare in actual['fares']:
            for price in fare['prices']:
                assert price['boy'] is False

    def test_get_result_variants_book_on_yandex_True(self):
        p_code = self.booking_p_code
        create_partner(code=p_code)

        test_variant = self._test_variant()
        test_variant['route'] = [['r1'], []]
        variants = Variants('test_qid', [test_variant], 1)
        results = {
            p_code: variants
        }
        actual = get_result_variants(results)

        ab_flags = {
            'BOY_AFL_ENABLE': '1',
            'BOY_DESKTOP_AFL_ENABLE': '1',
        }
        with create_app({}).test_request_context():
            _fill_book_on_yandex_availability(actual, MagicMock(), MagicMock(), create_query(), ab_flags)
        for fare in actual['fares']:
            for price in fare['prices']:
                assert price['boy'] is True

    def test_get_result_variants_with_selfconnect(self):
        p_code = self.booking_p_code
        create_partner(code=p_code)

        routes_selfconnect = {
            (('r1',), ()): None,
            (('r2',), ()): True,
            (('r3',), ()): False,
        }
        test_variants = [
            self._test_variant_with_route(route, selfconnect)
            for route, selfconnect in six.iteritems(routes_selfconnect)
        ]
        variants = Variants('test_qid', test_variants, 1)
        results = {
            p_code: variants
        }
        actual = get_result_variants(results)['fares']

        for route, expected_selfconnect in six.iteritems(routes_selfconnect):
            actual_variant = self._find_variant_with_route(
                actual, route
            )['prices'][0]

            if expected_selfconnect is None:
                assert 'selfconnect' not in actual_variant
            else:
                assert expected_selfconnect == actual_variant['selfconnect']

    def test_get_result_variants_with_promo(self):
        p_code = self.booking_p_code
        create_partner(code=p_code)

        promo_by_routes = {
            (('r1',), ()): None,
            (('r2',), ()): {
                'code': 'white-monday',
                'some-other-field': 'some-value',
            },
        }
        test_variants = [
            self._test_variant_with_route(route, promo=promo)
            for route, promo in six.iteritems(promo_by_routes)
        ]
        variants = Variants('test_qid', test_variants, 1)
        results = {
            p_code: variants
        }
        actual = get_result_variants(results)['fares']

        for route, expected_promo in six.iteritems(promo_by_routes):
            actual_variant = self._find_variant_with_route(
                actual, route
            )['prices'][0]

            if expected_promo is None:
                assert 'promo' not in actual_variant
            else:
                assert expected_promo == actual_variant['promo']

    def _test_variant(self):
        variants = defaultdict(Mock())
        variants['fare_families'] = [[None], []]
        variants['tariff'] = {
            'value': random.randint(100, 100000),
            'currency': 'RUR',
        }
        return variants

    def _test_variant_with_route(self, route, selfconnect=None, promo=None):
        v = self._test_variant()

        if selfconnect is not None:
            v['selfconnect'] = selfconnect

        if promo is not None:
            v['promo'] = promo

        v['route'] = route

        return v

    @staticmethod
    def _find_variant_with_route(variants, route):
        for variant in variants:
            if variant['route'] == route:
                return variant

        raise AssertionError('Variant with that route does not exist')


@pytest.mark.dbuser
@pytest.mark.parametrize('category, expected', (
    (None, None),
    ('unknown', 'unknown'),
    ('good', 'good'),
    ('bad', 'bad'),
))
def test_serialize_price_category(category, expected):
    reset_all_caches()
    partner = create_partner()

    variant = defaultdict(Mock())
    variant['fare_families'] = [[None], []]
    variant['tariff'] = {
        'value': 100500,
        'currency': 'RUR',
    }
    if category:
        variant['price_category'] = category
    variant['route'] = (('forward',), ())
    variants = Variants('test_qid', [variant], 1)
    results = {
        partner.code: variants
    }

    actual = get_result_variants(results)['fares']

    if expected is None:
        assert 'priceCategory' not in actual[0]['prices'][0]
    else:
        assert actual[0]['prices'][0]['priceCategory'] == expected
