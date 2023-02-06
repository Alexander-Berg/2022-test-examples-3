# -*- coding: utf-8 -*-
import json
from datetime import datetime

from django.conf import settings
from mock import patch

from travel.avia.library.python.tester.factories import create_partner
from travel.avia.library.python.tester.testcase import TestCase

from travel.avia.library.python.ticket_daemon.memo import reset_all_caches
from travel.avia.ticket_daemon.ticket_daemon.api.query import Query
from travel.avia.ticket_daemon.ticket_daemon.lib.revise import should_register_redirect, CrossWalk
from travel.avia.ticket_daemon.ticket_daemon.daemon_tester import create_query

RANDOM_IMPORT_PATH = 'travel.avia.ticket_daemon.ticket_daemon.lib.revise.random'
CURRENCY = 'test'
PRICE_VALUE = 1
PRICE_UNIXTIME = '1579240959'
WIZARD_CURRENCY = 'wizard_test'
WIZARD_PRICE_VALUE = 2
WIZARD_PRICE_UNIXTIME = '1579240901'


def format_date(unixtime):
    return datetime.fromtimestamp(int(unixtime)).isoformat()


class TestRevise(TestCase):
    def setUp(self):
        reset_all_caches()
        self._query = create_query()
        self._partner = create_partner(code='some', review_percent=60)

        self._order_data = {'qid': self._query.id}
        self._staff_user_info = {'django_user': {'is_staff': True}}
        self._user_info = {'django_user': {'is_staff': False}}

        self._prev_revise_force = settings.REVISE_FORCE
        settings.REVISE_FORCE = False

    def tearDown(self):
        settings.REVISE_FORCE = self._prev_revise_force

    def test_disabled_for_partner_with_zero_review_percent(self):
        self._partner.review_percent = 0
        self._query = Query.from_key(self._query.qkey, service='42')
        with patch(RANDOM_IMPORT_PATH, return_value=1):
            assert should_register_redirect(
                partner=self._partner,
                order_data={'qid': self._query.id},
                user_info=self._staff_user_info
            ) is False

    def test_ignore_contain_wizard_prices(self):
        with patch(RANDOM_IMPORT_PATH, return_value=0.7):
            assert should_register_redirect(
                partner=self._partner,
                order_data=self._order_data,
                user_info=self._user_info,
                additional_params={
                    'revise_price_value': 'some',
                    'revise_price_currency': 'other'
                }
            ) is False

            assert should_register_redirect(
                partner=self._partner,
                order_data=self._order_data,
                user_info=self._user_info,
                additional_params={
                    'revise_price_value': 'some',
                }
            ) is False

            assert should_register_redirect(
                partner=self._partner,
                order_data=self._order_data,
                user_info=self._user_info,
                additional_params={
                    'revise_price_currency': 'other'
                }
            ) is False

            assert should_register_redirect(
                partner=self._partner,
                order_data=self._order_data,
                user_info=self._user_info,
                additional_params={}
            ) is False

    def test_always_enable_for_yandex_staff_user(self):
        for review_chance in [0, 0.5, 1]:
            with patch(RANDOM_IMPORT_PATH, return_value=review_chance):
                assert should_register_redirect(
                    partner=self._partner,
                    order_data=self._order_data,
                    user_info=self._staff_user_info
                )

    def test_always_enable_if_revise_force(self):
        settings.REVISE_FORCE = True

        with patch(RANDOM_IMPORT_PATH, return_value=0):
            assert should_register_redirect(
                partner=self._partner,
                order_data=self._order_data,
                user_info=self._user_info
            )

    def test_always_enable_for_42(self):
        self._query = Query.from_key(self._query.qkey, service='42')

        with patch(RANDOM_IMPORT_PATH, return_value=0):
            assert should_register_redirect(
                partner=self._partner,
                order_data={'qid': self._query.id},
                user_info=self._user_info
            )

    def test_enable_if_luck(self):
        with patch(RANDOM_IMPORT_PATH, return_value=(self._partner.review_percent - 10)/100.0):
            assert should_register_redirect(
                partner=self._partner,
                order_data=self._order_data,
                user_info=self._user_info
            )

    def test_disable_if_have_not_luck(self):
        with patch(RANDOM_IMPORT_PATH, return_value=(self._partner.review_percent + 10)/100.0):
            assert not should_register_redirect(
                partner=self._partner,
                order_data=self._order_data,
                user_info=self._user_info
            )

    def test_use_review_percent_with_national_version(self):
        review_percent = self._partner.review_percent
        assert not self._should_register_redirect(review_percent + 10, 'other_nation')

        for national_version in ['ru', 'kz', 'ua', 'com', 'tr']:
            assert not self._should_register_redirect(review_percent + 10, national_version)

            field = 'review_percent_' + national_version
            setattr(self._partner, field, review_percent + 20)
            assert self._should_register_redirect(review_percent + 10, national_version)

    def _should_register_redirect(self, random_value, national_version):
        order_data = self._order_data.copy()
        order_data['national_version'] = national_version

        with patch(RANDOM_IMPORT_PATH, return_value=random_value / 100.0):
            return should_register_redirect(
                partner=self._partner,
                order_data=order_data,
                user_info=self._user_info,
            )


class TestReviseSerialization(TestCase):
    def setUp(self):
        self.test_params = {
            'hit_time': datetime.now(),
            'partner': create_partner(code='test_code'),
            'url': 'test_url',
            'tariff': {
                'value': PRICE_VALUE,
                'currency': CURRENCY,
                'price_unixtime': PRICE_UNIXTIME,
            },
            'shown_tariff': {
                'value': PRICE_VALUE,
                'currency': CURRENCY,
                'price_unixtime': PRICE_UNIXTIME,
            },
            'user_info': 'test_info',
            'post_data': 'test_data',
            'qkey': 'c54_c2_2019-12-24_None_economy_1_0_0_ru',
            'utm_source': 'test_source',
            'query_source': 'test_source',
            'additional_data': {}
        }

        self.wizard_redir_key = '8cc62bbd-3baf-4752-9267-df05ee439517'

        self.expected_keys = [
            'partner',
            'hit_time',
            'review_time',
            'price_value',
            'price_currency',
            'price_unixtime',
            'shown_price_value',
            'shown_price_currency',
            'shown_price_unixtime',
            'order_content',
            'redirect_params',
            'user_info',
            'query_source',
            'utm_source',
            'utm_campaign',
            'utm_medium',
            'utm_content',
        ]

    def add_additional_data_into_params(self):
        params = {
            'utm_source': 'unisearch_ru',
            'utm_campaign': 'city',
            'utm_content': 'offer',
            'utm_medium': 'common',
            'revise_price_value': WIZARD_PRICE_VALUE,
            'revise_price_currency': WIZARD_CURRENCY,
            'wizard_redir_key': self.wizard_redir_key,
        }
        self.test_params['additional_data'] = params

    def serialize(self, walk):
        with patch('travel.avia.ticket_daemon.ticket_daemon.lib.revise.Query.from_key', return_value=None):
            return json.loads(walk.serialize())

    def assertSerialized(self, actual_result, expected_keys):
        for expected_key in expected_keys:
            assert expected_key in actual_result
            assert actual_result[expected_key] is not None, '%s is not set' % expected_key

    def test_serialize(self):
        walk = CrossWalk(**self.test_params)
        actual = self.serialize(walk)
        self.assertSerialized(actual, self.expected_keys)
        self.assertSerialized(actual, ['wizard_redir_key', 'wizard_flags'])

    def test_serialize_direct_redirects(self):
        utms = {
            'utm_source': 'rasp',
            'utm_campaign': 'desktop',
            'utm_content': 'ru',
            'utm_medium': 'redirect',
        }
        self.test_params['additional_data'] = utms
        walk = CrossWalk(**self.test_params)
        actual = self.serialize(walk)
        self.assertSerialized(actual, self.expected_keys)
        assert actual['wizard_redir_key'] is None
        assert actual['wizard_flags'] is None

    def test_serialize_wizard_redirects(self):
        self.add_additional_data_into_params()
        self.test_params['shown_tariff'] = None
        walk = CrossWalk(**self.test_params)
        actual = self.serialize(walk)
        assert actual['shown_price_unixtime'] is None
        assert actual['wizard_flags'] is None
        assert actual['wizard_redir_key'] == self.wizard_redir_key

        self.expected_keys.remove('shown_price_unixtime')
        self.assertSerialized(actual, self.expected_keys)

    def test_shown_tariff(self):
        walk = CrossWalk(**self.test_params)
        review = walk.prepare_review()

        assert review.shown_price_value == PRICE_VALUE
        assert review.shown_price_currency == CURRENCY
        assert review.shown_price_unixtime == format_date(PRICE_UNIXTIME)

    def test_wizard_shown_tariff_without_tariff_sign(self):
        self.add_additional_data_into_params()
        self.test_params['shown_tariff'] = None

        walk = CrossWalk(**self.test_params)
        review = walk.prepare_review()

        assert review.shown_price_value == WIZARD_PRICE_VALUE
        assert review.shown_price_currency == WIZARD_CURRENCY
        assert review.shown_price_unixtime is None

    def test_wizard_shown_tariff_with_tariff_sign(self):
        self.add_additional_data_into_params()

        walk = CrossWalk(**self.test_params)
        review = walk.prepare_review()

        assert review.shown_price_value == PRICE_VALUE
        assert review.shown_price_currency == CURRENCY
        assert review.shown_price_unixtime == format_date(PRICE_UNIXTIME)
