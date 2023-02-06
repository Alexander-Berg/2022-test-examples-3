# -*- coding: utf-8 -*-
import re
import mock
import time

from hamcrest import assert_that, calling, raises
from nose_parameterized import parameterized

from test.base import DiskTestCase
from test.helpers.stubs.base import ChainedPatchBaseStub

from mpfs.common.errors.billing import BillingOrderNotFound
from mpfs.core.billing import Order, ArchiveOrder, look_for_order
from mpfs.common.errors.billing import BigBillingBadResult
from mpfs.core.metastorage.control import billing_orders
from mpfs.core.services.billing_service import trust_payment_service


SUBS_UNTIL_TIME = time.time()


class TrustPaymentsStub(ChainedPatchBaseStub):
    TARGET = 'mpfs.core.services.trust_payments.TrustPaymentsService'

    order_place_clause = ('POST', re.compile(r'.*/trust-payments/v2/(orders|subscriptions)$'))
    create_payment = ('POST', re.compile(r'.*/trust-payments/v2/payments$'))
    start_payment = ('POST', re.compile(r'.*/trust-payments/v2/payments/[0-9a-z]+/start$'))
    get_payment_info = ('GET', re.compile(r'.*/trust-payments/v2/payments/[0-9a-z]+$'))
    get_payment_methods = ('GET', re.compile(r'.*/trust-payments/v2/payment-methods$'))
    get_subscription_info = ('GET', re.compile(r'.*/trust-payments/v2/subscriptions/[0-9a-z]+$'))
    get_orders_info = ('GET', re.compile(r'.*/trust-payments/v2/orders/[0-9a-z]+$'))

    @staticmethod
    def payment_trust_request_mock_success(already_created=False, already_started=True, already_paid=False, auto=False):
        def mock_function(method, relative_url, params=None, data=None, headers=None, cookies=None,
                          timeout=None, send_tvm_ticket=None):
            if (method == TrustPaymentsStub.order_place_clause[0]
                    and TrustPaymentsStub.order_place_clause[1].match(relative_url)):
                return {
                    'status': 'success',
                    'status_code': 'created',
                    'order_id': data['order_id'],
                    'product_id': 'test_1kb_for_one_second_subs' if auto else 'test_1kb_for_one_second_app',
                }
            elif (method == TrustPaymentsStub.create_payment[0]
                  and TrustPaymentsStub.create_payment[1].match(relative_url)):
                return {'status': 'success', 'purchase_token': '0184d2773297183b9519cd9ff396d85b'}
            elif (method == TrustPaymentsStub.start_payment[0]
                    and TrustPaymentsStub.start_payment[1].match(relative_url)):
                return {
                    'status': 'success',
                    'uid': headers['X-Uid'],
                    'payment_url': 'https://trust-test.yandex.ru/web/payment?purchase_token=0184d2773297183b9519cd9ff396d85b',
                    'payment_status': 'started',
                    'start_ts': '1519304430.251',
                    'orders': [
                        {
                            'product_type': 'subs' if auto else 'app',
                            'uid': '4010326924',
                            'paid_amount': '0.00',
                            'order_ts': '1519303869.193',
                            'current_qty': '0.00',
                            'order_id': '506877014',
                            'orig_amount': '30.00',
                            'product_name': 'test_1kb_for_one_second',
                            'product_id': 'test_1kb_for_one_second_subs' if auto else 'test_1kb_for_one_second_app',
                        }
                    ],
                    'update_ts': '1519304113.013',
                    'currency': 'RUB',
                    'amount': '30.00',
                    'payment_timeout': '1200.000',
                    'purchase_token': '0184d2773297183b9519cd9ff396d85b'
                }
            elif (method == TrustPaymentsStub.get_payment_info[0]
                    and TrustPaymentsStub.get_payment_info[1].match(relative_url)):
                result = {
                    'status': 'success',
                    'uid': headers['X-Uid'],
                    'payment_status': 'started',
                    'orders': [
                        {
                            'product_type': 'subs' if auto else 'app',
                            'uid': '4010326924',
                            'paid_amount': '0.00',
                            'order_ts': '1519303869.193',
                            'current_qty': '0.00',
                            'order_id': '506877014',
                            'orig_amount': '30.00',
                            'product_name': 'test_1kb_for_one_second',
                            'product_id': 'test_1kb_for_one_second_subs' if auto else 'test_1kb_for_one_second_app',
                        }
                    ],
                    'update_ts': '1519304113.013',
                    'currency': 'RUB',
                    'amount': '30.00',
                    'payment_timeout': '1200.000',
                    'purchase_token': '0184d2773297183b9519cd9ff396d85b'
                }
                if already_created:
                    result['payment_url'] = 'https://trust-test.yandex.ru/web/payment?purchase_token=0184d2773297183b9519cd9ff396d85b'
                if already_paid:
                    result['payment_status'] = 'cleared'
                if auto:
                    result['binding_result'] = 'success'
                return result
            elif (method == TrustPaymentsStub.get_payment_methods[0]
                    and TrustPaymentsStub.get_payment_methods[1].match(relative_url)):
                return {
                    'status': 'success',
                    'bound_payment_methods': [
                        {
                            'expiration_month': '11',
                            'account': '411111****1111',
                            'region_id': 225,
                            'expiration_year': '2025',
                            'holder': 'TEST',
                            'system': 'VISA',
                            'orig_uid': '4010326924',
                            'binding_type': 'fake',
                            'binding_ts': '1518791931.831',
                            'payment_method': 'card',
                            'binding_systems': ['trust'],
                            'expired': False,
                            'id': 'card-x3133'
                        }
                    ]
                }
            elif (method == TrustPaymentsStub.get_subscription_info[0]
                    and TrustPaymentsStub.get_subscription_info[1].match(relative_url)):
                result = {
                    'status': 'success',
                    'product_type': 'subs',
                    'uid': headers['X-Uid'],
                    'subs_period_count': 1,
                    'current_amount': [['RUB', '30.00']],
                    'order_ts': '1519294684.666',
                    'current_qty': '1.00',
                    'subs_period': '1S',
                    'order_id': '689957594',
                    'product_id': 'test_1kb_for_one_second_subs',
                    'finish_ts': '1519294767.777',
                    'parent_service_product_id': 'test_1kb_for_one_second_app',
                    'subs_state': 4,
                    'ready_to_pay': 0,
                    'product_name': 'test_1kb_for_one_second',
                    'begin_ts': '1519294751.827'
                }
                if already_started:
                    result['payments'] = ['349b5be5e26d2565c4130c96db9defc1']
                if auto:
                    result['subs_until_ts'] = SUBS_UNTIL_TIME
                return result
            elif (method == TrustPaymentsStub.get_orders_info[0]
                    and TrustPaymentsStub.get_orders_info[1].match(relative_url)):
                return {
                    'status': 'success',
                    'product_type': 'app',
                    'uid': headers['X-Uid'],
                    'order_ts': '1519311881.238',
                    'current_qty': '0.00',
                    'order_id': '837903182',
                    'product_name': 'test_1kb_for_one_second',
                    'product_id': 'test_1kb_for_one_second_app'
                }
            return None
        return mock_function

    def __init__(self, already_created=False, already_paid=False, already_started=True, auto=False):
        super(TrustPaymentsStub, self).__init__()
        self.request = mock.patch('%s.request' % self.TARGET, side_effect=self.payment_trust_request_mock_success(
            already_created=already_created, already_started=already_started, already_paid=already_paid, auto=auto))
        self.is_enabled = mock.patch('%s.is_enabled' % self.TARGET, return_value=True)


class TrustPaymentsTestCase(DiskTestCase):

    def setup_method(self, method):
        super(TrustPaymentsTestCase, self).setup_method(method)
        self.billing_ok('client_bind_market', {'uid': self.uid, 'market': 'RU', 'ip': '127.0.0.1'})

    @parameterized.expand([
        (0, 3, 2),
        (1, 5, 3)
    ])
    def test_payment_make(self, auto, count_first_time, count_second_time):
        with TrustPaymentsStub():
            args = {
                'uid': self.uid,
                'pid': 'test_1kb_for_one_second',
                'line': 'development',
                'payment_method': 'bankcard',
                'ip': '127.0.0.1',
                'auto': auto,
            }
            number = self.billing_ok('order_place', args)['number']

        with TrustPaymentsStub() as stub:
            args = {
                'uid': self.uid,
                'number': number,
                'return_path': 'http://www.example.org',
                'ip': '127.0.0.1',
            }
            url_1 = self.billing_ok('order_make_payment', args)['url']
            assert stub.request.call_count == count_first_time  # X запросов в биллинг означает, что оплату создали

        order = billing_orders.find_one({'_id': number})
        # Храним payment_id только для не автопродляемых услуг
        if bool(auto):
            assert order.get('payment_id') is None

        with TrustPaymentsStub(already_created=True) as stub:
            # Проверяем, что при повторном дергании order_make_payment не создадим новой оплаты
            args = {
                'uid': self.uid,
                'number': number,
                'return_path': 'http://www.example.org',
                'ip': '127.0.0.1',
            }
            url_2 = self.billing_ok('order_make_payment', args)['url']
            assert stub.request.call_count == count_second_time  # Y запросов в биллинг означает, что оплату не создали
            assert url_1 == url_2

    @parameterized.expand([
        (0, 2),
        (1, 2),
    ])
    def test_check_order(self, auto, correct_call_count):
        with TrustPaymentsStub(already_started=False, auto=bool(auto)):
            args = {
                'uid': self.uid,
                'pid': 'test_1kb_for_one_second',
                'line': 'development',
                'payment_method': 'bankcard',
                'ip': '127.0.0.1',
                'auto': auto,
            }
            number = self.billing_ok('order_place', args)['number']
            assert_that(calling(trust_payment_service.check_order).with_args(self.uid, number), raises(BigBillingBadResult))

        with TrustPaymentsStub(auto=bool(auto)):
            args = {
                'uid': self.uid,
                'number': number,
                'return_path': 'http://www.example.org',
                'ip': '127.0.0.1',
            }
            self.billing_ok('order_make_payment', args)
            result = trust_payment_service.check_order(self.uid, number)
            assert result['status'] == 'wait_for_notification'

        with TrustPaymentsStub(already_paid=True, auto=bool(auto)) as stub:
            result = trust_payment_service.check_order(self.uid, number)
            assert result['status'] == 'success'
            assert stub.request.call_count == correct_call_count
            if auto:
                assert int(result['subs_until_ts_msec']) / 1000 == int(SUBS_UNTIL_TIME)

    @parameterized.expand([
        (None, 'ru'),
        ('ru', 'ru'),
        ('tr', 'tr'),
        ('uk', 'uk'),
        ('en', 'en'),
    ])
    def test_order_make_payment_passess_locale_to_bb(self, sent_locale, expected_language):
        with TrustPaymentsStub(already_started=False):
            args = {
                'uid': self.uid,
                'pid': 'test_1kb_for_one_second',
                'line': 'development',
                'payment_method': 'bankcard',
                'ip': '127.0.0.1',
            }
            number = self.billing_ok('order_place', args)['number']

        with TrustPaymentsStub() as payment_mock:
            args = {
                'uid': self.uid,
                'number': number,
                'return_path': 'http://www.example.org',
                'ip': '127.0.0.1',
            }
            if sent_locale:
                args['locale'] = sent_locale
            self.billing_ok('order_make_payment', args)
        assert payment_mock.request.call_args_list[1][1]['data']['lang'] == expected_language

    def test_search_in_archive(self):
        mocked_number = '3945fb57173c4160a2e5a730d86692c0'
        with TrustPaymentsStub(), \
             mock.patch('mpfs.core.billing.entity.UniqEntity.GenerateUuidID', return_value=mocked_number):
            args = {
                'uid': self.uid,
                'pid': 'test_1kb_for_one_second',
                'line': 'development',
                'payment_method': 'bankcard',
                'ip': '127.0.0.1',
            }
            self.billing_ok('order_place', args)
            order = look_for_order(mocked_number)
            order.archive()

            args = {
                'uid': self.uid,
                'pid': 'test_1kb_for_two_seconds',
                'line': 'development',
                'payment_method': 'bankcard',
                'ip': '127.0.0.1',
            }
            self.billing_ok('order_place', args)
        order = look_for_order(mocked_number)
        assert order.get('pid') == 'test_1kb_for_two_seconds'
