# -*- coding: utf-8 -*-
import datetime
import mock
import pytest

from hamcrest import assert_that, has_item, equal_to, has_property, has_entry, is_not, empty, all_of, greater_than, \
    has_entries
from hamcrest import contains_inanyorder
from nose_parameterized import parameterized
from httplib import CONFLICT

from mpfs.common.util.experiments.logic import experiment_manager
from mpfs.config import settings
from mpfs.core.billing.processing.repair import recalculate_limit_by_services
from mpfs.core.promo_codes.logic.discount_manager import DiscountManager
from mpfs.core.promo_codes.logic.errors import AttemptToActivateArchivedDiscount
from mpfs.core.yateam.logic import make_yateam
from test.api_suit import set_up_mailbox, tear_down_mailbox, patch_mailbox
from test.conftest import capture_queue_errors, COMMON_DB_IN_POSTGRES
from test.helpers import products
from test.helpers.size_units import GB
from test.parallelly.billing.base import (
    BaseBillingTestCase,
    BillingTestCaseMixin,
    fake_check_order_bad,
    fake_check_order_cancelled,
    fake_check_order_good,
    fake_check_order_unpaid,
)

import mpfs.engine.process
from test.base import time_machine
from test.helpers.stubs.services import BigBillingStub

from mpfs.common.static import codes
from mpfs.common.static.tags.billing import *
from mpfs.core.billing import Order, Product
from mpfs.core.billing.processing.admin import archive_lost_orders
from mpfs.core.billing.service import Service
from mpfs.common.static import codes
from mpfs.core.filesystem.dao.legacy import CollectionRoutedDatabase
from mpfs.core.metastorage.control import (
    billing_services,
    billing_service_attributes,
    billing_orders,
    billing_orders_history,
)
from test.parallelly.billing.trust_payments import TrustPaymentsStub
from mpfs.core.user.base import User

db = CollectionRoutedDatabase()


class BasicMethodsTestCase(BaseBillingTestCase):
    def test_send_email_on_delete_service(self):
        # Создаем услугу пользователю
        resp = self.billing_ok('service_create', {'uid': self.uid,
                                                  'line': 'partner',
                                                  'pid': 'yandex_disk_for_business',
                                                  'ip': '127.0.0.1',
                                                  'product.amount': 12345})
        sid = resp['sid']
        # Удаляем с параметром, который тестируем: send_email
        with patch_mailbox() as letters:
            self.billing_ok('service_delete', {'uid': self.uid,
                                               'sid': sid,
                                               'ip': '127.0.0.1',
                                               'send_email': 0})
        assert len(letters) == 0

    def test_bind_user_to_market(self):
        args = {
            'uid': self.uid,
            'pid': 'test_1kb_for_one_second',
            'line': 'development',
            'payment_method': 'yamoney',
            'ip': self.localhost,
        }

        self.bind_user_to_market(market='COM')
        user_info = self.json_ok('user_info', args)
        self.assertEqual(user_info['states']['billing']['market'], 'COM')

    def test_product_list(self):
        args = {'uid': self.uid}
        self.bind_user_to_market(market='COM')

        result = self.billing_ok('product_list', args)
        self.assertNotEqual(result, [])
        self.assertTrue(result[0]['id'] is not None)

        args = {'market': 'TR'}
        result = self.billing_ok('product_list', args)
        self.assertNotEqual(result, [])
        self.assertTrue(result[0]['id'] is not None)

        args = {}
        self.billing_error('product_list', args)

    def test_products_for_not_initialized_user(self):
        self.remove_user(self.uid)
        result = self.billing_ok('verstka_products', {'uid': self.uid, 'locale': 'ru'})
        assert 'items' in result
        assert result['items']

        user_check_resp = self.json_ok('user_check', {'uid': self.uid})
        assert bool(user_check_resp['need_init'])

    @parameterized.expand([
        ('ru', 'RUB', False, 'RU', {'bankcard', 'yamoney'}),
        ('uk', 'RUB', False, 'UK', {'bankcard', 'yamoney'}),
        ('en', 'USD', False, 'COM', {'bankcard'}),
        ('tr', 'USD', False, 'TR', {'bankcard'}),
        ('ru', 'RUB', True, 'RU', {'bankcard'}),
        ('uk', 'RUB', True, 'UK', {'bankcard'}),
        ('en', 'USD', True, 'COM', {'bankcard'}),
        ('tr', 'USD', True, 'TR', {'bankcard'}),
    ])
    def test_verstka_products(self, locale, correct_currency, is_pdd, market_name, correct_payment_methods):
        uid = self.uid
        if is_pdd:
            uid = self.pdd_uid
            self.json_ok('user_init', {'uid': uid})
        self.bind_user_to_market(market=market_name, uid=uid)
        resp = self.billing_ok('verstka_products', {'uid': uid, 'locale': locale})
        assert 'display_space' in resp['items'][0]
        assert 'display_space_units' in resp['items'][0]
        assert 'currency' in resp['items'][0]
        assert resp['items'][0]['currency'] == correct_currency
        assert 'space' in resp['items'][0]
        assert 'periods' in resp['items'][0]
        assert 'month' in resp['items'][0]['periods']
        assert 'year' in resp['items'][0]['periods']
        assert 'is_best_offer' in resp['items'][0]
        assert 'is_yandex_plus' in resp['items'][0]
        for i in ('month', 'year'):
            assert 'price' in resp['items'][0]['periods'][i]
            assert 'product_id' in resp['items'][0]['periods'][i]
        assert correct_payment_methods == set(resp['payment_methods'])

    @parameterized.expand([
        ('partner', ('rostelecom_2014_100gb',
                     'rostelecom_2015_100gb_paid',
                     'search_app_promo_gift',
                     'yandex_plus_10gb',
                     'yandex_directory_1tb',
                     'yandex_disk_for_business',
                     'yandex_mail_pro',
                     'yandex_b2c_mail_pro',
                     'yandex_b2c_mail_pro_promo',
                     'yandex_b2b_mail_pro',
                     )),
    ])
    def test_product_list_defaults(self, line, expected_products):
        products = self.billing_ok('product_list', {'market': 'RU',
                                                    'line': line})
        products_ids = [product['id']
                        for product in products]
        assert_that(products_ids, contains_inanyorder(*expected_products))

    @parameterized.expand([
        ('ru', 'RUB', False, 'RU'),
        ('uk', 'RUB', False, 'UK'),
        ('en', 'USD', False, 'COM'),
        ('tr', 'USD', False, 'TR'),
        ('ru', 'RUB', True, 'RU'),
        ('uk', 'RUB', True, 'UK'),
        ('en', 'USD', True, 'COM'),
        ('tr', 'USD', True, 'TR'),
    ])
    def test_verstka_products_third_tariff_no_plus(self, locale, correct_currency, is_pdd, market_name):
        # with PassportStub():
        #     uid = self.uid
        pass

    def test_verstka_products_third_tariff_has_plus(self):
        pass

    def test_client_list_attributes(self):
        args = {
            'uid': self.uid,
            'attr': 'phone',
            'value': '+799999999999',
            'ip': self.localhost,
        }
        self.billing_ok('client_set_attribute', args)
        result = self.billing_ok('client_list_attributes', args)
        self.assertTrue(result is not None)
        self.assertEqual(result['phone'], '+799999999999')

    def test_invalid_payment_method(self):
        """
        Проверяем ошибку при попытке оплатить на рынке запрещенным для этого
        рынка методом
        """
        self.bind_user_to_market(market='TR')
        args = {
            'uid': self.uid,
            'pid': 'test_1kb_for_one_second',
            'line': 'development',
            'payment_method': 'yamoney',
            'ip': self.localhost,
        }
        self.billing_error('order_place', args, code=134)

    def test_list_payment_methods(self):
        self.bind_user_to_market(market='RU')

        args = {
            'uid': self.uid,
            'ip': self.localhost,
        }
        result = self.billing_ok('client_list_payment_methods', args)

        self.assertEqual(result, ['bankcard', 'yamoney'])

    def test_billing_api_available_for_user_with_overdraft(self):
        with mock.patch('mpfs.core.services.disk_service.MPFSStorageService.used',
                        return_value=12 * GB), \
             mock.patch('mpfs.core.services.disk_service.MPFSStorageService.limit',
                        return_value=10 * GB), \
             mock.patch('mpfs.frontend.api.FEATURE_TOGGLES_DISABLE_API_FOR_OVERDRAFT_PERCENTAGE', 100):
            self.billing_ok('verstka_products',
                            {'uid': self.uid, 'locale': 'ru'},
                            headers={'Yandex-Cloud-Request-ID': 'rest-passport-trying-to-add-service'})

    def test_list_payment_methods_pdd(self):
        """
        Для ПДД запрещена оплата яндекс-деньгами
        """
        self.create_user(self.pdd_uid, noemail=True)

        args = {
            'uid': self.pdd_uid,
            'market': 'RU',
            'ip': self.localhost,
        }
        self.billing_ok('client_bind_market', args)
        user_info = self.json_ok('user_info', args)
        self.assertEqual(user_info['states']['billing']['market'], 'RU')

        args = {
            'uid': self.pdd_uid,
            'ip': self.localhost,
        }
        result = self.billing_ok('client_list_payment_methods', args)

        self.assertEqual(result, ['bankcard'])

    def test_order_place_tld(self):
        u"""
        Тестируем пересылку локалей в ручке place_order
        """
        self.bind_user_to_market('RU')

        with TrustPaymentsStub() as mock_payment_trust, \
            mock.patch('mpfs.core.services.trust_payments.TrustPaymentsService.is_enabled', return_value=True):
            # Локаль по умолчанию
            number = self.place_order()

            self.pay_order(number)
            assert 'domain_sfx' in mock_payment_trust.request.call_args_list[2][1]['data']
            assert mock_payment_trust.request.call_args_list[2][1]['data']['domain_sfx'] == 'ru'
            mock_payment_trust.request.reset_mock()

            # Указанная локаль
            number = self.place_order(domain='com')

            self.pay_order(number, domain='com')
            assert 'domain_sfx' in mock_payment_trust.request.call_args_list[2][1]['data']
            assert mock_payment_trust.request.call_args_list[2][1]['data']['domain_sfx'] == 'com'
            mock_payment_trust.request.reset_mock()

            # Недоступная локаль
            number = self.place_order(domain='kz')

            self.pay_order(number, domain='kz')
            assert 'domain_sfx' not in mock_payment_trust.request.call_args_list[2][1]['data']

    def test_buy(self):
        self.bind_user_to_market('RU')
        args = {
            'uid': self.uid,
            'pid': '1tb_1m_2019_v4',
            'payment_method': 'bankcard',
            'auto': '0',
            'ip': 'ip',
            'template_tag': 'desktop/form',
            'locale': 'ru',
            'from': 'from'
        }
        with BigBillingStub() as bbs, mock.patch(
            'mpfs.core.billing.processing.common.stat_billing_payments_log') as log:
            result = self.billing_ok('buy', args)
            assert log.info.call_args[0][0]['from'] == 'from'
        assert 'url' in result
        assert 'order_number' in result
        with mock.patch('mpfs.frontend.api.auth.TVM2.authorize'):
            self.billing_ok('order_process_callback',
                            {'uid': self.uid, 'number': bbs.order_place.call_args[0][1], 'status': 'success',
                             'status_code': ''})
        assert '1tb_1m_2019_v4' in [x['name'] for x in self.get_services_list(uid=self.uid)]

    @pytest.mark.skipif(COMMON_DB_IN_POSTGRES, reason='Test for mongo_ro mode')
    def test_buy_with_orders_in_readonly_mode(self):
        services_collections = [
            'billing_orders_history',
        ]
        with mock.patch.dict(
            'mpfs.config.settings.common_pg',
            {'billing_orders_history': {'use_pg': False, 'mongo_ro': True}}
        ):
            with patch_mailbox() as letters:
                self.bind_user_to_market('RU')
                args = {
                    'uid': self.uid,
                    'pid': '1tb_1m_2019_v4',
                    'payment_method': 'bankcard',
                    'auto': '0',
                    'ip': 'ip',
                    'template_tag': 'desktop/form',
                    'locale': 'ru',
                    'from': 'from'
                }
                with BigBillingStub() as bbs, mock.patch(
                    'mpfs.core.billing.processing.common.stat_billing_payments_log') as log:
                    result = self.billing_ok('buy', args)
                    assert log.info.call_args[0][0]['from'] == 'from'
                assert 'url' in result
                assert 'order_number' in result
                with mock.patch('mpfs.frontend.api.auth.TVM2.authorize'):
                    self.billing_ok('order_process_callback',
                                    {'uid': self.uid, 'number': bbs.order_place.call_args[0][1], 'status': 'success',
                                     'status_code': ''})
                assert '1tb_1m_2019_v4' not in [x['name'] for x in self.get_services_list(uid=self.uid)]
        assert len(letters) == 0

    @parameterized.expand([
        ('mobile/form', 'mobile/form'),
        ('desktop/form', 'desktop/form'),
        (None, 'desktop/form'),
    ])
    def test_passing_template_tag_in_order_make_payment(self, template_tag, expected_template_tag):
        self.bind_user_to_market('RU')
        number = self.place_order()

        with TrustPaymentsStub() as mock_payment:
            self.pay_order(number, template_tag=template_tag)

        assert mock_payment.request.call_args_list[1][1]['data']['template_tag'] == expected_template_tag

    def test_response_400_if_unexpected_template_tag_was_passed(self):
        self.bind_user_to_market('RU')
        number = self.place_order()

        args = {
            'uid': self.uid,
            'number': number,
            'ip': self.localhost,
            'template_tag': 'fake/tag'
        }
        self.billing_error('order_make_payment', args, code=codes.BILLING_UNEXPECTED_TEMPLATE_TAG)

    def test_order_set_payment_processing_state_changes_state(self):
        self.bind_user_to_market('RU')
        with BigBillingStub():
            number = self.place_order()
            self.pay_order(number)
        self.billing_ok('order_set_payment_processing_state', {'uid': self.uid, 'number': number})
        orders = self.billing_ok('order_list_current', {'uid': self.uid})
        assert len(orders) == 1
        assert orders[0][STATE] == PAYMENT_PROCESSING

    def test_setting_payment_processing_state_and_successfull_callback_handling(self):
        self.bind_user_to_market('RU')
        with BigBillingStub(), \
             mock.patch('mpfs.frontend.api.auth.TVM2.authorize'):
            number = self.place_order('test_1kb_for_one_second')
            self.pay_order(number)
            self.billing_ok('order_set_payment_processing_state', {'uid': self.uid, 'number': number})
            self.manual_success_callback_on_order(number)
        services = self.get_services_list(self.uid, 'test_1kb_for_one_second')
        print services
        assert len(services) == 1

    def test_setting_payment_processing_to_already_handled_order_returns_409(self):
        self.bind_user_to_market('RU')
        with BigBillingStub():
            number = self.place_order('test_1kb_for_one_second')
            self.pay_order(number)
            self.manual_success_callback_on_order(number)
            self.billing_error('order_set_payment_processing_state', {'uid': self.uid, 'number': number},
                               code=codes.BILLING_UNABLE_TO_CHANGE_ORDER_STATE)

    def test_order_set_payment_processing_state_returns_404_if_order_does_not_exist(self):
        self.bind_user_to_market('RU')
        self.billing_error('order_set_payment_processing_state', {'uid': self.uid, 'number': '123'})
        assert self.response.status == 404

    @parameterized.expand([
        (True,),
        (False,),
    ])
    def test_order_set_payment_processing_state_returns_404_if_uid_conflict(self, in_archive):
        self.bind_user_to_market('RU')
        with BigBillingStub():
            number = self.place_order('test_1kb_for_one_second')
            self.pay_order(number)
            if in_archive:
                self.manual_success_callback_on_order(number)
        self.billing_error('order_set_payment_processing_state', {'uid': '333', 'number': number})
        assert self.response.status == 404


class OrderListFilteredTestCase(BaseBillingTestCase):
    prev_default_page_size = 0

    @classmethod
    def setup_class(cls):
        from mpfs.core.billing.interface.iteration_keys.order_list_filtered import IterationKey
        cls.prev_default_page_size = IterationKey.DEFAULT_PAGE_SIZE
        IterationKey.DEFAULT_PAGE_SIZE = 3

    @classmethod
    def teardown_class(cls):
        from mpfs.core.billing.interface.iteration_keys.order_list_filtered import IterationKey
        IterationKey.DEFAULT_PAGE_SIZE = cls.prev_default_page_size

    def setup_method(self, method):
        super(OrderListFilteredTestCase, self).setup_method(method)
        self.bind_user_to_market('RU')
        self.cur_time = datetime.datetime.now()

    def test_format(self):
        self._create_orders_descending(6)
        self.cur_time += datetime.timedelta(seconds=1)  # эмулируем идущее время
        with time_machine(self.cur_time):
            results = self.billing_ok('order_list_filtered', {'uid': self.uid})
        assert 'items' in results
        assert 'iteration_key' in results
        assert 'number' in results['items'][0]
        assert 'product' in results['items'][0]
        assert all([x in results['items'][0]['product'] for x in ('pid', 'bb_pid', 'names')])
        assert all([x in results['items'][0]['product']['names'] for x in ('tr', 'ru', 'uk', 'en')])

    def test_all_orders_among_active_and_fit_page(self):
        order_numbers = self._create_orders_descending(2)
        result_order_numbers = self._iterate_through_orders()
        assert result_order_numbers == list(reversed(order_numbers))

    def test_all_orders_in_archive_and_fit_page(self):
        order_numbers = self._create_orders_descending(2)
        self._archive_orders(order_numbers)
        result_order_numbers = self._iterate_through_orders()
        assert result_order_numbers == list(reversed(order_numbers))

    def test_all_orders_in_active_and_archive_and_fit_page(self):
        order_numbers = self._create_orders_descending(2)
        self._archive_orders(order_numbers[:1])
        result_order_numbers = self._iterate_through_orders()
        assert result_order_numbers == list(reversed(order_numbers))

    def test_order_list_filtered_batch_ends_among_active(self):
        order_numbers = self._create_orders_descending(6)
        self._archive_orders(order_numbers[:4])
        result_order_numbers = self._iterate_through_orders()
        assert result_order_numbers == list(reversed(order_numbers))

    def test_order_list_filtered_batch_ends_among_archive(self):
        order_numbers = self._create_orders_descending(6)
        self._archive_orders(order_numbers[:2])
        result_order_numbers = self._iterate_through_orders()
        assert result_order_numbers == list(reversed(order_numbers))

    def test_order_list_filtered_batch_ends_in_between(self):
        order_numbers = self._create_orders_descending(6)
        self._archive_orders(order_numbers[:3])
        result_order_numbers = self._iterate_through_orders()
        assert result_order_numbers == list(reversed(order_numbers))

    def test_order_list_filtered_order_archived_has_greater_ctime_than_active(self):
        order_numbers = self._create_orders_descending(6)
        self._archive_orders(order_numbers[4:])
        result_order_numbers = self._iterate_through_orders()
        assert result_order_numbers == list(reversed(order_numbers[:4])) + list(reversed(order_numbers[4:]))

    def test_order_list_filtered_filters_states(self):
        order_numbers = self._create_orders_descending(6)
        self._archive_orders(order_numbers[:2])
        self._archive_orders(order_numbers[2:4], status='cancelled')
        self.billing_ok('order_set_payment_processing_state', {'uid': self.uid, 'number': order_numbers[5]})
        result_order_numbers = self._iterate_through_orders('payment_processing,success')
        assert result_order_numbers == [order_numbers[5]] + list(reversed(order_numbers[:2]))

    def test_order_list_filtered_incorrect_uid_does_not_match_iteration_key(self):
        self._create_orders_descending(6)
        qs = {'uid': self.uid}
        self.cur_time += datetime.timedelta(seconds=1)
        with time_machine(self.cur_time):
            results = self.billing_ok('order_list_filtered', qs)

        self.cur_time += datetime.timedelta(seconds=1)
        qs = {
            'iteration_key': results['iteration_key'],
            'uid': '12345',
        }
        with time_machine(self.cur_time):
            print self.billing_error('order_list_filtered', qs)
        assert self.response.status == 400

    def _create_orders_descending(self, amount):
        order_numbers = []
        for i in range(amount):
            with BigBillingStub(), time_machine(self.cur_time):
                order_numbers.append(self.place_order())
            self.cur_time += datetime.timedelta(seconds=1)
        return order_numbers

    def _archive_orders(self, orders, status='success'):
        for i in orders:
            with BigBillingStub(), time_machine(self.cur_time):
                Order(i).close(status)
                Order(i).archive()
            self.cur_time += datetime.timedelta(seconds=1)

    def _iterate_through_orders(self, filters=None):
        orders = []
        qs = {'uid': self.uid}
        if filters:
            qs['filters'] = filters
        iter_key = None
        while True:
            self.cur_time += datetime.timedelta(seconds=1)  # эмулируем идущее время
            with time_machine(self.cur_time):
                if iter_key:
                    qs['iteration_key'] = iter_key
                results = self.billing_ok('order_list_filtered', qs)
                orders += [x['number'] for x in results['items']]
                iter_key = results.get('iteration_key')
                if not iter_key:
                    break
        return orders


class ServicesMethodsTestCase(BaseBillingTestCase):
    def test_plain_service_create(self):
        """
        Проверяем тупое заведение услуги
        """
        services_before = self.get_services_list()

        sid = self.service_create(pid='test_1kb_eternal', line='development')
        self.assertTrue(sid is not None)

        services_after = self.get_services_list()

        self.assertEqual(len(services_after), len(services_before) + 1)
        assert_that(services_after, has_item(has_entry(SID, sid)))

    def test_plain_service_delete(self):
        """
        Тупое удаление
        """
        sid = self.service_create(pid='test_1kb_eternal', line='development')

        services_before = self.get_services_list()

        result = self.service_delete(sid)
        self.assertTrue(result is not None)

        services_after = self.get_services_list()
        self.assertEqual(len(services_after), len(services_before) - 1)

    def test_plain_service_delete_by_pid(self):
        """
        Тупое удаление по  pid
        """
        sid = self.service_create(pid='test_1kb_eternal', line='development')
        services_before = self.get_services_list()
        args = {
            'uid': self.uid,
            'ip': self.localhost,
            'pid': 'test_1kb_eternal'
        }

        result = self.service_delete(pid='test_1kb_eternal')
        self.assertTrue(result is not None)

        services_after = self.get_services_list()
        self.assertEqual(len(services_after), len(services_before) - 1)

    @parameterized.expand([
        (True,),
        (False,),
    ])
    def test_service_with_discount(self, for_yandexoids_only):
        u"""Проверяем выдачу и отбор сервиса с привязанной выдачей скидки"""
        discount_id = self.create_discount(PRIMARY_2018_DISCOUNT_30)
        original_init = Product.__init__

        def new_product(self, pid):
            """Подменяет discount_template_id на созданный в тестах"""
            original_init(self, pid)
            if pid == products.YANDEX_PLUS.id:
                self.discount_template_id = discount_id

        feature_enabled_for_all = not for_yandexoids_only

        if for_yandexoids_only:
            make_yateam(self.uid, '123')

        with mock.patch.object(Product, '__init__', new_product), \
             mock.patch.object(mpfs.core.billing.processing.common, 'FEATURE_TOGGLES_PROVIDE_DISCOUNTS_WITH_PRODUCTS',
                               feature_enabled_for_all):
            sid = self.billing_ok('service_create', {'uid': self.uid,
                                                     'line': PARTNER,
                                                     'pid': products.YANDEX_PLUS.id,
                                                     'ip': self.localhost}).get('sid')

        active_discounts = DiscountManager._get_all_user_discounts(self.uid)
        assert_that(active_discounts, has_item(has_property(DISCOUNT_TEMPLATE_ID, equal_to(discount_id))))

        service_list = self.billing_ok('service_list', {'uid': self.uid, 'ip': self.localhost})
        assert_that(service_list, has_item(has_entry('name', products.YANDEX_PLUS.id)))

        # Удаляем услугу - скидка тоже должна удалиться
        with mock.patch.object(Product, '__init__', new_product), \
             mock.patch.object(mpfs.core.billing.processing.common, 'FEATURE_TOGGLES_PROVIDE_DISCOUNTS_WITH_PRODUCTS',
                               feature_enabled_for_all):
            self.billing_ok('service_delete', {'uid': self.uid, 'ip': self.localhost, 'sid': sid})

        active_discounts = DiscountManager._get_all_user_discounts(self.uid)
        assert_that(active_discounts, empty())

        service_list = self.billing_ok('service_list', {'uid': self.uid, 'ip': self.localhost})
        assert_that(service_list, is_not(has_item(has_entry('name', products.YANDEX_PLUS.id))))

        # Снова выдаем скидку - должна добавиться
        with mock.patch.object(Product, '__init__', new_product), \
             mock.patch.object(mpfs.core.billing.processing.common, 'FEATURE_TOGGLES_PROVIDE_DISCOUNTS_WITH_PRODUCTS',
                               feature_enabled_for_all):
            self.billing_ok('service_create', {'uid': self.uid,
                                               'line': PARTNER,
                                               'pid': products.YANDEX_PLUS.id,
                                               'ip': self.localhost}).get('sid')

        active_discounts = DiscountManager._get_all_user_discounts(self.uid)
        assert_that(active_discounts, has_item(has_property(DISCOUNT_TEMPLATE_ID, equal_to(discount_id))))

        service_list = self.billing_ok('service_list', {'uid': self.uid, 'ip': self.localhost})
        assert_that(service_list, has_item(has_entry('name', products.YANDEX_PLUS.id)))

    def test_retry_create_service_with_discount(self):
        """Проверяем перевыдачу сервиса с привязанной выдачей скидки"""
        discount_id = self.create_discount(PRIMARY_2018_DISCOUNT_30)
        original_init = Product.__init__

        def new_product(self, pid):
            """Подменяет discount_template_id на созданный в тестах"""
            original_init(self, pid)
            if pid == products.YANDEX_PLUS.id:
                self.discount_template_id = discount_id

        # Выдаем услугу со скидкой и затем отбираем ее (чтобы в архиве была скидка)
        with mock.patch.object(Product, '__init__', new_product), \
             mock.patch.object(mpfs.core.billing.processing.common, 'FEATURE_TOGGLES_PROVIDE_DISCOUNTS_WITH_PRODUCTS',
                               True):
            sid = self.billing_ok('service_create', {'uid': self.uid,
                                                     'line': PARTNER,
                                                     'pid': products.YANDEX_PLUS.id,
                                                     'ip': self.localhost}).get('sid')
            self.billing_ok('service_delete', {'uid': self.uid, 'ip': self.localhost, 'sid': sid})

        # Снова выдаем скидку, сначала получим ошибку (не успеем на репликах удалить скидку из архива)
        with mock.patch.object(Product, '__init__', new_product), \
             mock.patch.object(mpfs.core.billing.processing.common, 'FEATURE_TOGGLES_PROVIDE_DISCOUNTS_WITH_PRODUCTS',
                               True), \
             mock.patch('mpfs.core.promo_codes.logic.discount.Discount.create',
                        side_effect=AttemptToActivateArchivedDiscount()):
            self.billing_error('service_create', {'uid': self.uid,
                                                  'line': PARTNER,
                                                  'pid': products.YANDEX_PLUS.id,
                                                  'ip': self.localhost},
                               status=CONFLICT)

        # Услугу выдали
        assert_that(self.billing_ok('service_list', {'uid': self.uid, 'ip': self.localhost}),
                    has_item(has_entry('name', products.YANDEX_PLUS.id)))
        # Скидку не удалось
        assert_that(DiscountManager._get_all_user_discounts(self.uid),
                    empty())

        with mock.patch.object(Product, '__init__', new_product), \
             mock.patch.object(mpfs.core.billing.processing.common, 'FEATURE_TOGGLES_PROVIDE_DISCOUNTS_WITH_PRODUCTS',
                               True):
            # retry должен быть успешным
            self.billing_ok('service_create', {'uid': self.uid,
                                               'line': PARTNER,
                                               'pid': products.YANDEX_PLUS.id,
                                               'ip': self.localhost})

        # Услуга попрежнему на месте
        assert_that(self.billing_ok('service_list', {'uid': self.uid, 'ip': self.localhost}),
                    has_item(has_entry('name', products.YANDEX_PLUS.id)))
        # Скидка тоже теперь есть
        assert_that(DiscountManager._get_all_user_discounts(self.uid),
                    has_item(has_property(DISCOUNT_TEMPLATE_ID, equal_to(discount_id))))

    def test_plain_service_list_with_product_names(self):
        """
        https://st.yandex-team.ru/CHEMODAN-22415

        проверяем, что в services_list выдаются названия продуктов на всех языках
        """
        from mpfs.core.billing.product.catalog import PRODUCTS
        webchat_names = PRODUCTS['yandex_webchat'][NAME]

        self.service_create(pid='yandex_webchat', line='bonus')
        services = self.get_services_list()
        assert len(services), 1

        service = services[0]
        assert NAMES in service
        assert service[NAMES] == webchat_names

    def test_singleton_service_create(self):
        """
        Создание синглтон-сервиса
        """
        sid = self.service_create(pid='app_install', line='bonus')
        self.assertTrue(sid is not None)

        args = {
            'uid': self.uid,
            'line': 'bonus',
            'pid': 'app_install',
            'ip': self.localhost,
        }

        self.billing_error('service_create', args, code=135)

    def test_singleton_service_states(self):
        """
        Создание синглтон-сервиса через состояния
        """
        services_before = self.get_services_list()

        for i in (1, 2):
            args = {
                'uid': self.uid,
                'key': 'yandex_staff',
                'value': '1',
            }
            self.json_ok('state_set', args)
            user_info = self.json_ok('user_info', args)
            self.assertTrue('yandex_staff' in user_info['states']['global'])

            services_after = self.get_services_list()
            self.assertEqual(len(services_after), len(services_before) + 1)

    def test_singleton_service_with_service_create_for_ps_billing(self):
        """Retry создания синглтон-сервиса через ручку без 409 на ошибку BillingProductIsSingletone"""
        previously_created_sid = self.billing_ok('service_create_for_ps_billing', {'uid': self.uid,
                                                                                   'line': PARTNER,
                                                                                   'pid': 'yandex_disk_for_business',
                                                                                   'ip': self.localhost,
                                                                                   'product.amount': 7}).get('sid')

        resp = self.billing_ok('service_create_for_ps_billing', {'uid': self.uid,
                                                                 'line': PARTNER,
                                                                 'pid': 'yandex_disk_for_business',
                                                                 'ip': self.localhost,
                                                                 'product.amount': 778},
                               status=200)
        assert 'sid' in resp
        assert resp['sid'] == previously_created_sid

    def test_set_remove_plain_attributes(self):
        """
        Проверяем, что set-remove на объекте сервиса работает корректно
        Найдено на примере флага notified в https://st.yandex-team.ru/CHEMODAN-18016
        """
        sid = self.service_create(pid='app_install', line='bonus')
        self.assertTrue(sid is not None)

        # выставляем атрибут
        service = Service(sid)
        service.set(NOTIFIED, 123)
        del service

        # загружаем услугу, проверяем, что атрибут выставлен
        service = Service(sid)
        assert service.notified == 123

        # убираем атрибут
        service.remove(NOTIFIED)
        del service

        # загружаем заново и убеждаемся, что атрибута нет
        service = Service(sid)
        assert service.notified is None

    def test_repeated_success_order_process_callback(self):
        """
        Тестируем поведение МПФС в случае, когда ББ ошибочно присылает повторные колбеки для уже оплаченного заказа
        """
        self.bind_user_to_market('RU')
        number = self.place_order()
        self.pay_order(number)

        mpfs.core.services.billing_service.BB.check_order = fake_check_order_good
        self.manual_success_callback_on_order(number)
        services_before = self.get_services_list(self.uid)

        mpfs.core.services.billing_service.BB.check_order = fake_check_order_bad
        self.manual_success_callback_on_order(number)
        services_after = self.get_services_list(self.uid)

        assert services_after[0]['expires'] == services_before[0]['expires']

    @parameterized.expand([
        (True,),
        (False,),
    ])
    def test_discarding_cancelled_callbacks_to_lost_orders(self, is_subscription):
        self.stale_order_doc['pid'] = 'test_1kb_for_five_seconds'
        self.stale_order_doc['state'] = 'lost'
        billing_orders.insert(self.stale_order_doc)
        callback_function = self.manual_fail_callback_on_order
        if is_subscription:
            callback_function = self.manual_fail_callback_on_subscription

        mpfs.core.services.billing_service.BB.check_order = fake_check_order_unpaid
        archive_lost_orders()
        assert list(billing_orders_history.find({'uid': self.uid}))

        mpfs.core.services.billing_service.BB.check_order = fake_check_order_cancelled
        with capture_queue_errors() as errors:
            callback_function(number=self.number, status=CANCELLED)
        assert not errors

    def test_service_localized_names_are_different_depending_on_auto(self):
        self.bind_user_to_market(market='RU')

        order = self.place_order(product='10gb_1m_2015', line='primary_2015', auto=0)
        self.pay_order(number=order)
        self.manual_success_callback_on_order(number=order)

        order = self.place_order(product='10gb_1m_2015', line='primary_2015', auto=1)
        self.pay_order(number=order)
        self.manual_success_callback_on_order(number=order)

        services = self.billing_ok('service_list', {'uid': self.uid, 'ip': self.localhost})
        services = [service
                    for service in services
                    if service['name'] != products.INITIAL_10GB.id]
        non_auto_service = [i for i in services if not i['subscription']][0]
        auto_service = [i for i in services if i['subscription']][0]

        assert non_auto_service['names']['en'] == u'10 GB for a month'
        assert auto_service['names']['en'] == u'10 GB monthly subscription'

    def test_service_localized_names_from_archive_are_different_depending_on_auto(self):
        self.bind_user_to_market(market='RU')

        order = self.place_order(product='10gb_1m_2015', line='primary_2015', auto=0)
        self.pay_order(number=order)
        self.manual_success_callback_on_order(number=order)

        order = self.place_order(product='10gb_1m_2015', line='primary_2015', auto=1)
        self.pay_order(number=order)
        self.manual_success_callback_on_order(number=order)

        services = self.billing_ok('service_list', {'uid': self.uid, 'ip': self.localhost})
        services = [service
                    for service in services
                    if service['name'] != products.INITIAL_10GB.id]
        non_auto_service = [i for i in services if not i['subscription']][0]
        auto_service = [i for i in services if i['subscription']][0]

        self.billing_ok('service_delete', {'uid': self.uid, 'ip': self.localhost, 'sid': non_auto_service['sid']})
        self.billing_ok('service_delete', {'uid': self.uid, 'ip': self.localhost, 'sid': auto_service['sid']})

        services = self.billing_ok('service_list_history', {'uid': self.uid, 'ip': self.localhost})
        services = [service
                    for service in services
                    if service['name'] != products.INITIAL_10GB.id]
        non_auto_service = [i for i in services if i['sid'] == non_auto_service['sid']][0]
        auto_service = [i for i in services if i['sid'] == auto_service['sid']][0]

        assert non_auto_service['names']['en'] == u'10 GB for a month'
        assert auto_service['names']['en'] == u'10 GB monthly subscription'

    @parameterized.expand([
        (True,),
        (False,),
    ])
    def test_queue_put_get_correct_names_for(self, auto):
        self.bind_user_to_market(market='RU')

        order = self.place_order(product='10gb_1m_2015', line='primary_2015', auto=int(auto))
        self.pay_order(number=order)

        if auto:
            self.manual_success_callback_on_subscription(number=order)
        else:
            self.manual_success_callback_on_order(number=order)

        services = self.billing_ok('service_list', {'uid': self.uid, 'ip': self.localhost})
        sid = None
        for service in services:
            if service['name'] == '10gb_1m_2015':
                sid = service['sid']
                break

        assert sid

        letters = set_up_mailbox()
        self.billing_ok('service_delete', {'uid': self.uid, 'ip': self.localhost, 'sid': sid})
        tear_down_mailbox()
        if auto:
            assert letters[0]['args'][2]['product_names']['en'] == u'10 GB monthly subscription'
        else:
            assert letters[0]['args'][2]['product_names']['en'] == u'10 GB for a month'

    @parameterized.expand([
        (True, 'ru', u'Подписка 10 ГБ на месяц'),
        (False, 'ru', u'10 ГБ на месяц'),
        (True, 'en', u'10 GB monthly subscription'),
        (False, 'en', u'10 GB for a month'),
        (True, 'uk', u'Передплата 10 ГБ на місяць'),
        (False, 'uk', u'10 ГБ на місяць'),
        (True, 'tr', u'Aylık abonelik 10 GB'),
        (False, 'tr', u'Aylık 10 GB'),
        (True, None, u'Подписка 10 ГБ на месяц'),
        (False, None, u'10 ГБ на месяц'),
    ])
    def test_developer_payload_is_sent_during_order_creating(self, auto, locale, expected_tariff_name):
        self.bind_user_to_market(market='RU')

        with mock.patch('mpfs.core.services.trust_payments.TrustPaymentsService.request') as mock_payment:
            self.place_order(product='10gb_1m_2015', line='primary_2015', auto=int(auto), locale=locale)

        assert 'developer_payload' in mock_payment.call_args[1]['data']
        assert 'tariff_name' in mock_payment.call_args[1]['data']['developer_payload']
        assert mock_payment.call_args[1]['data']['developer_payload']['tariff_name'] == expected_tariff_name


class RestoringServiceFromArchiveTestCase(BillingTestCaseMixin, BaseBillingTestCase):
    """Проверка восстановления услуги из архива

    https://st.yandex-team.ru/CHEMODAN-38725"""

    def setup_method(self, method):
        super(RestoringServiceFromArchiveTestCase, self).setup_method(method)

    def test_service_without_attrs(self):
        self.service_doc['pid'] = 'test_1kb_for_five_seconds'
        self.stale_order_doc['pid'] = 'test_1kb_for_five_seconds'
        billing_orders.insert(self.stale_order_doc)
        billing_services.insert(self.service_doc)
        self.service_delete(self.sid)
        archive_lost_orders()

        service_list = self.billing_ok('service_list', {'uid': self.uid, 'ip': 'localhost'})
        assert_that(service_list, has_item(has_entries({'sid': equal_to(self.sid)})))

    def test_service_with_attrs(self):
        self.service_doc['pid'] = 'test_1kb_for_five_seconds_with_attrs'
        self.stale_order_doc['pid'] = 'test_1kb_for_five_seconds_with_attrs'
        billing_orders.insert(self.stale_order_doc)
        billing_services.insert(self.service_doc)
        billing_service_attributes.insert(self.service_attrs_doc)
        self.service_delete(self.sid)
        archive_lost_orders()

        service_list = self.billing_ok('service_list', {'uid': self.uid, 'ip': 'localhost'})
        assert_that(service_list, has_item(has_entries({'sid': equal_to(self.sid),
                                                        'size': greater_than(0)})))


class AlternativeAttributesServicesTestCase(BaseBillingTestCase):
    def test_alternative_attributes_update(self):
        sid = self.service_create(pid='blat', line='bonus')
        self.assertTrue(sid is not None)

        args = {'uid': self.uid}
        old_space_limit = self.json_ok('user_info', args)['space']['limit']

        args = {
            'uid': self.uid,
            'sid': sid,
            'ip': self.localhost,
            'key': 'product.amount',
            'value': 53687091200,
        }
        self.billing_ok('service_set_attribute', args)

        args = {'uid': self.uid}
        new_space_limit = self.json_ok('user_info', args)['space']['limit']

        # у юзера было 3 + 10 (блат по умолчанию)
        # 10 меняем на 50, должно получится в итоге 3 + 50 = 3 + 10 + 50 - 10
        self.assertEqual(new_space_limit, old_space_limit + 53687091200 - 10737418240)

    def test_alternative_attributes_listing(self):
        sid = self.service_create(pid='blat', line='bonus')
        args = {
            'uid': self.uid,
            'sid': sid,
            'ip': self.localhost,
            'key': 'product.amount',
            'value': 53687091200,
        }
        self.billing_ok('service_set_attribute', args)

        services = self.get_services_list()
        self.assertEqual(services[0]['size'], 53687091200)

    def test_alternative_attributes_remove(self):
        sid = self.service_create(pid='blat', line='bonus')

        args = {
            'uid': self.uid,
        }
        old_space_limit = self.json_ok('user_info', args)['space']['limit']

        args = {
            'uid': self.uid,
            'sid': sid,
            'ip': self.localhost,
            'key': 'product.amount',
            'value': 53687091200,
        }
        self.billing_ok('service_set_attribute', args)

        args = {
            'uid': self.uid,
            'sid': sid,
            'ip': self.localhost,
            'key': 'product.amount',
        }
        self.billing_ok('service_remove_attribute', args)

        args = {'uid': self.uid}
        new_space_limit = self.json_ok('user_info', args)['space']['limit']

        self.assertEqual(new_space_limit, old_space_limit)

    def test_alternative_attributes_errors(self):
        sid = self.service_create(pid='blat', line='bonus')
        self.assertTrue(sid is not None)

        args = {
            'uid': self.uid,
            'sid': sid,
            'ip': self.localhost,
            'key': 'product.fake',
            'value': 'fake',
        }
        self.billing_error('service_set_attribute', args)

    def test_attributes_update(self):
        sid = self.service_create(pid='blat', line='bonus')
        self.assertTrue(sid is not None)

        args = {
            'uid': self.uid,
            'sid': sid,
            'ip': self.localhost,
            'key': 'service.btime',
            'value': 10000000000,
        }
        self.billing_ok('service_set_attribute', args)

        services = self.get_services_list()
        self.assertEqual(services[0]['expires'], 10000000000)

    def test_attributes_errors(self):
        sid = self.service_create(pid='blat', line='bonus')
        self.assertTrue(sid is not None)

        args = {
            'uid': self.uid,
            'sid': sid,
            'ip': self.localhost,
            'key': 'service.fake',
            'value': 'fake',
        }
        self.billing_error('service_set_attribute', args)

    def test_create_service_with_attributes(self):
        """
        Создание услуги сразу с указанными атрибутами
        """
        args = {
            'uid': self.uid,
            'line': 'distribution',
            'pid': 'kingston_flash',
            'ip': self.localhost,
            'service.btime': '123456789123',
            'product.amount': '1024',
        }

        sid = self.billing_ok('service_create', args).get('sid')

        services = self.get_services_list()
        self.assertEqual(services[0]['expires'], 123456789123)
        self.assertEqual(services[0]['size'], 1024)

    def test_delete_service_with_attributes(self):
        """
        Создаем услугу с атрибутами, удаляем
        Смотрим, что ее атрибуты сохранились в истории и отдаются в ручках листингов сервисов
        """
        args = {
            'uid': self.uid,
            'line': 'distribution',
            'pid': 'kingston_flash',
            'ip': self.localhost,
            'service.btime': '123456789123',
            'product.amount': '1024',
        }

        sid = self.billing_ok('service_create', args).get('sid')
        services = self.get_services_list()
        assert services[0]['expires'] == 123456789123
        assert services[0]['size'] == 1024
        self.service_delete(sid)

        archive_services = self.billing_ok('service_list_history', {'uid': self.uid, 'ip': '1.1.1.1'})
        print archive_services

        service_found_in_archive = False
        for item in archive_services:
            if item['sid'] == sid:
                service_found_in_archive = True
                assert item['removes'] != 123456789123  # удалена раньше указанного времени
                assert item['size'] == 1024
        assert service_found_in_archive

    def test_user_index_is_paid_provide_and_delete_paid_services(self):
        self.bind_user_to_market(market='COM')
        user_info = self.json_ok('user_info', {'uid': self.uid})
        assert user_info['paid'] == 0

        sid = self.billing_ok('service_create',
                              {'uid': self.uid, 'line': 'primary_2019', 'pid': '100gb_1m_2019', 'ip': '127.0.0.1'})[
            'sid']

        user_info = self.json_ok('user_info', {'uid': self.uid})
        assert user_info['paid'] == 1
        self.billing_ok('service_delete', {'uid': self.uid, 'sid': sid, 'ip': '127.0.0.1', 'send_email': 0})

        user_info = self.json_ok('user_info', {'uid': self.uid})
        assert user_info['paid'] == 0

    def test_user_index_is_paid_delete_one_paid_service(self):
        self.bind_user_to_market(market='COM')
        user_info = self.json_ok('user_info', {'uid': self.uid})
        assert user_info['paid'] == 0
        # first
        sid_1 = self.billing_ok('service_create',
                                {'uid': self.uid, 'line': 'primary_2019', 'pid': '100gb_1m_2019', 'ip': '127.0.0.1'})[
            'sid']
        user_info = self.json_ok('user_info', {'uid': self.uid})
        assert user_info['paid'] == 1
        # second
        sid_2 = self.billing_ok('service_create',
                                {'uid': self.uid, 'line': 'primary_2019', 'pid': '100gb_1m_2019', 'ip': '127.0.0.1'})[
            'sid']
        user_info = self.json_ok('user_info', {'uid': self.uid})
        assert user_info['paid'] == 1

        # delete only second
        self.billing_ok('service_delete', {'uid': self.uid, 'sid': sid_2, 'ip': '127.0.0.1', 'send_email': 0})
        user_info = self.json_ok('user_info', {'uid': self.uid})
        assert user_info['paid'] == 1
        # delete first
        self.billing_ok('service_delete', {'uid': self.uid, 'sid': sid_1, 'ip': '127.0.0.1', 'send_email': 0})
        user_info = self.json_ok('user_info', {'uid': self.uid})
        assert user_info['paid'] == 0

    def test_user_index_is_paid_recalculate(self):
        self.bind_user_to_market(market='COM')
        user_info = self.json_ok('user_info', {'uid': self.uid})
        assert user_info['paid'] == 0

        User(self.uid).set_is_paid(True)
        user_info = self.json_ok('user_info', {'uid': self.uid})
        assert user_info['paid'] == 1

        recalculate_limit_by_services(self.uid, dry_run=False)
        user_info = self.json_ok('user_info', {'uid': self.uid})
        assert user_info['paid'] == 0

        sid = self.billing_ok('service_create',
                              {'uid': self.uid, 'line': 'primary_2019', 'pid': '100gb_1m_2019', 'ip': '127.0.0.1'})[
            'sid']
        user_info = self.json_ok('user_info', {'uid': self.uid})
        assert user_info['paid'] == 1

        # unset flag
        User(self.uid).set_is_paid(False)
        user_info = self.json_ok('user_info', {'uid': self.uid})
        assert user_info['paid'] == 0

        # has paid service - set flag
        recalculate_limit_by_services(self.uid, dry_run=False)
        user_info = self.json_ok('user_info', {'uid': self.uid})
        assert user_info['paid'] == 1
