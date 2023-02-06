# -*- coding: utf-8 -*-
from datetime import datetime

import pytest
import mock

from mock import patch
from hamcrest import assert_that, ends_with, equal_to, has_length, is_
from nose_parameterized import parameterized

from test.base import time_machine
from test.conftest import capture_queue_errors
from test.parallelly.billing.base import (
    BaseBillingTestCase,
    BillingTestCaseMixin,
    fake_check_order_good,
    fake_check_order_error,
    fake_check_order_subscription_stopped, fake_check_order_subscription_subs_until_in_past,
    fake_check_order_subscription_subs_until_in_past_small,
)
from test.helpers.stubs.services import BigBillingStub, PushServicesStub

import mpfs.core.services.billing_service

from mpfs.common.errors.billing import BillingSubscriptionNotFound
from mpfs.common.static.tags.billing import *
from mpfs.common.util import ctimestamp

from mpfs.config import settings
from mpfs.common.errors import billing as billing_errors
from mpfs.core.billing.service import Service
from mpfs.core.billing.order import ArchiveOrder
from mpfs.core.billing.order.subscription import Subscription
from mpfs.core.billing.processing.billing import push_billing_commands
from mpfs.core.filesystem.dao.legacy import CollectionRoutedDatabase
from mpfs.core.services.billing_service import BillingOrderInfo


db = CollectionRoutedDatabase()

BB_LBTIME = 555000
BB_BTIME = 666000


class SubscriptionWorkflowTestCase(BaseBillingTestCase):
    def test_archive_order_on_second_sync(self):
        """Проверяем, что order архивируется, даже если он не заархивировался на первоначальной покупке"""

        from mpfs.core.metastorage.control import billing_orders, billing_orders_history

        with mock.patch('mpfs.core.services.trust_payments.TrustPaymentsService.is_enabled', return_value=True):
            # биндим юзера к рынку
            self.bind_user_to_market(market='RU')
            # делаем заказ на подписку
            subscription = self.place_order(product='test_1kb_for_five_seconds', pay='bankcard', auto=1)
            # оплачиваем
            self.pay_order(number=subscription)
            # принимаем коллбек
            self.manual_success_callback_on_subscription(number=subscription)
            # убеждаемся, что заказ не попал в историю
            args = {
                'uid': self.uid,
                'ip': self.localhost,
            }
            self.assertEqual(len(self.billing_ok('order_list_history', args)), 1)
            # восстанавливаем из архива
            ArchiveOrder(subscription).restore_from_archive()
            self.assertEqual(len(self.billing_ok('order_list_history', args)), 0)
            # order опять уехал в архив
            self.manual_success_callback_on_subscription(number=subscription)
            self.assertEqual(len(self.billing_ok('order_list_history', args)), 1)

    def test_workflow(self):
        u"""
        Простая проверка:
        - заказать
        - увидеть заказ
        - оплатить
        - принять коллбек
        - проверить, что услуга завелась
        - пробиллить и доплатить
        - не доплатить и удалить
        """
        with mock.patch('mpfs.core.services.trust_payments.TrustPaymentsService.is_enabled', return_value=True):
            # биндим юзера к рынку
            self.bind_user_to_market(market='RU')

            # делаем заказ на подписку
            subscription = self.place_order(product='test_1kb_for_five_seconds', pay='bankcard', auto=1)

            # оплачиваем
            self.pay_order(number=subscription)

            # принимаем коллбек
            self.manual_success_callback_on_subscription(number=subscription)
            args = {
                'uid': self.uid,
                'ip': self.localhost,
            }

            # убеждаемся, что заказ попал в историю
            result = self.billing_ok('order_list_history', args)
            self.assertEqual(len(result), 1)
            self.assertEqual(result[0]['number'], subscription)

            # проверяем, что услуга завелась
            services = self.get_services_list()
            self.assertEqual(len(services), 1)
            self.assertEqual(services[0]['state'], None)
            sid = services[0][SID]

            # выставляем дату биллинга в прошлом
            self.service_manual_set_params(sid, btime=(ctimestamp() - 1))

            # биллим услугу
            push_billing_commands()

            # проверяем, что перешла в состояние ожидания оплаты
            services = self.get_services_list()
            self.assertEqual(len(services), 1)
            self.assertTrue(services[0]['state'] in (None, WAITING_PAYMENT))
            previous_billing_time = services[0]['expires']

            new_btime = previous_billing_time + 5
            # успешно продляем услугу  автоплатежом
            with self.patch_check_order(new_btime * 1000):
                self.manual_success_callback_on_subscription(number=subscription)

            # проверяем, что время следующей оплаты сдвинулось
            services = self.get_services_list()
            next_billing_time = services[0]['expires']
            self.assertEqual(previous_billing_time + 5, next_billing_time)

            # посылаем плохой коллбек
            with self.patch_check_order(new_btime * 1000, finish_ts=new_btime):
                self.manual_fail_callback_on_subscription(
                    number=subscription,
                    status=CANCELLED,
                    status_code=NOT_ENOUGH_FUNDS
                )

            # убеждаемся, что услуга не перенесена в архив
            services = self.get_services_list()
            self.assertEqual(len(services), 1)

            with time_machine(datetime.fromtimestamp(new_btime + settings.billing['subscription_payment_cancel'] + 1)), \
                    self.patch_check_order(new_btime * 1000, finish_ts=new_btime):
                push_billing_commands()

            # убеждаемся, что услуга перенесена в архив
            services = self.get_services_list()
            self.assertEqual(len(services), 0)
            archived_services = self.billing_ok('service_list_history', {'uid': self.uid, 'ip': '1.1.1.1'})
            assert 1 == len(archived_services)
            assert 'test_1kb_for_five_seconds' in {x['name'] for x in archived_services}

    def test_workflow_delayed_callback(self):
        u"""
        Проверяем, что происходит, когда коллбеки не идут
        """
        with mock.patch('mpfs.core.services.trust_payments.TrustPaymentsService.is_enabled', return_value=True):
            # биндим юзера к рынку
            self.bind_user_to_market(market='RU')

            # создаем заказ, оплачиваем, принимаем коллбек
            subscription = self.place_order(product='test_1kb_for_five_seconds', pay='bankcard', auto=1)
            self.pay_order(number=subscription)
            self.manual_success_callback_on_subscription(number=subscription)

            # проверяем, что услуга завелась
            services = self.get_services_list()
            self.assertEqual(len(services), 1)
            self.assertEqual(services[0]['state'], None)
            sid = services[0][SID]

            # выставляем дату биллинга в прошлом
            self.service_manual_set_params(sid, btime=(ctimestamp() - 1))

            # биллимся, смотрим, что перешли в состояние ожидания платежа
            push_billing_commands()
            services = self.get_services_list()
            self.assertEqual(services[0]['state'], WAITING_PAYMENT)

            # выставляем дату биллинга в прошлом
            self.service_manual_set_params(sid, btime=(ctimestamp() - 3))

            # биллимся, смотрим, что перешли в состояние платеж задержан
            push_billing_commands()
            services = self.get_services_list()
            self.assertEqual(services[0]['state'], PAYMENT_DELAYED)

            # выставляем дату биллинга в прошлом
            self.service_manual_set_params(sid, btime=(ctimestamp() - 6))

            # биллимся, смотрим, что услуга удалалась
            mpfs.core.services.billing_service.BB.check_order = fake_check_order_subscription_stopped
            push_billing_commands()
            services = self.get_services_list()
            self.assertEqual(len(services), 0)

    @parameterized.expand([
        (fake_check_order_subscription_subs_until_in_past, 0),
        (fake_check_order_subscription_subs_until_in_past_small, 1)
    ])
    def test_subscription_btime_in_past(self, fake_check_order, count_services):
        u"""
        Кейс, когда биллинг возвращает активную подписку, но дата окончания в прошлом. Ожидаем отключение подписки
        """
        with mock.patch('mpfs.core.services.trust_payments.TrustPaymentsService.is_enabled', return_value=True):
            # биндим юзера к рынку
            self.bind_user_to_market(market='RU')

            # создаем заказ, оплачиваем, принимаем коллбек
            subscription = self.place_order(product='test_1kb_for_one_minute', pay='bankcard', auto=1)
            self.pay_order(number=subscription)
            self.manual_success_callback_on_subscription(number=subscription)

            # подставляем фейковый ответ, что подписка активна, но дата в прошлом
            mpfs.core.services.billing_service.BB.check_order = fake_check_order
            push_billing_commands()

            services = self.get_services_list()
            assert len(services) == count_services

    def test_unsubscribe(self):
        u"""
        Проверяем, как работает отписка
        """
        with mock.patch('mpfs.core.services.trust_payments.TrustPaymentsService.is_enabled', return_value=True):
            # биндим юзера к рынку
            self.bind_user_to_market(market='RU')

            # создаем заказ, оплачиваем, принимаем коллбек
            subscription = self.place_order(product='test_1kb_for_one_minute', pay='bankcard', auto=1)
            self.pay_order(number=subscription)

            btime = ctimestamp() + 60
            with self.patch_check_order(btime * 1000):
                self.manual_success_callback_on_subscription(number=subscription)

            # смотрим на нулевое состояние услуги
            services = self.get_services_list()
            self.assertEqual(len(services), 1)
            self.assertEqual(services[0]['state'], None)
            sid = services[0][SID]
            service = Service(sid=sid)
            assert service.auto

            # отписываемся
            self.service_unsubscribe(sid)

            # смотрим, что подписка перешла в состояние отмены
            services = self.get_services_list()
            self.assertEqual(len(services), 1)
            self.assertEqual(services[0][STATE], SUBSCRIPTION_CANCELLED)
            self.assertEqual(services[0]['subscription'], False)
            service = Service(sid=sid)
            assert not service.auto
            self.assertRaises(BillingSubscriptionNotFound, Subscription, service=service.sid)

            with self.patch_check_order(btime * 1000, finish_ts=btime), \
                    time_machine(datetime.fromtimestamp(btime)), capture_queue_errors() as errors:
                self.manual_fail_callback_on_subscription(number=subscription, status=CANCELLED)
                assert not errors

    def test_unsubscribe_not_cancelled_service(self):
        u"""
        Проверяем, как работает отписка, если у нас заказ уже отменен, а в трасте оплачен
        """
        with mock.patch('mpfs.core.services.trust_payments.TrustPaymentsService.is_enabled', return_value=True):
            # биндим юзера к рынку
            self.bind_user_to_market(market='RU')

            # создаем заказ, оплачиваем, принимаем коллбек
            subscription = self.place_order(product='test_1kb_for_one_minute', pay='bankcard', auto=1)
            self.pay_order(number=subscription)

            btime = ctimestamp() + 60
            with self.patch_check_order(btime * 1000):
                self.manual_success_callback_on_subscription(number=subscription)

            # смотрим на нулевое состояние услуги
            services = self.get_services_list()
            self.assertEqual(len(services), 1)
            self.assertEqual(services[0]['state'], None)
            sid = services[0][SID]
            service = Service(sid=sid)
            assert service.auto

            # отписываемся
            btime = ctimestamp() + 300
            lbtime = ctimestamp()
            with self.patch_check_order(btime * 1000, payment_ts_msec=lbtime * 1000):
                self.service_unsubscribe(sid)

            # смотрим, что подписка перешла в состояние отмены
            services = self.get_services_list()
            self.assertEqual(len(services), 1)
            self.assertEqual(services[0][STATE], SUBSCRIPTION_CANCELLED)
            self.assertEqual(services[0]['subscription'], False)
            service = Service(sid=sid)
            assert not service.auto
            assert service.btime == btime
            assert service.lbtime == lbtime
            self.assertRaises(BillingSubscriptionNotFound, Subscription, service=service.sid)

    @parameterized.expand([
        ('trust_payments_error', ERROR),
        ('trust_payments_skip', SKIPPED),
    ])
    def test_ignore_binding_result(self, testing_client, binding_result):
        u"""
        Игнорируем параметр binding_result из колбека и создаём подписку при любом его значении
        """
        with mock.patch('mpfs.core.services.trust_payments.TrustPaymentsService.is_enabled', return_value=True):
            # биндим юзера к рынку
            self.bind_user_to_market(market='RU')

            subscription = self.place_order(product='test_1kb_for_one_minute', pay='bankcard', auto=1)
            self.pay_order(number=subscription)

            # создаем успешную оплату, но с проблемой привязки
            args = {
                'uid': self.uid,
                'number': subscription,
                'status': 'success',
                'status_code': '',
                'binding_result': binding_result,
            }
            with mock.patch('mpfs.frontend.api.auth.TVM2.authorize'):
                self.billing_ok('subscription_process_callback', args)

            # убеждаемся, что услуга завелась и это подписка
            services = self.get_services_list()
            self.assertEqual(len(services), 1)
            self.assertEqual(services[0]['state'], None)
            self.assertEqual(services[0]['subscription'], True)

    def test_lost_subscription(self):
        with BigBillingStub():
            # биндим юзера к рынку, создаем заказ, оплачиваем, принимаем коллбек
            self.bind_user_to_market(market='RU')
            subscription = self.place_order(product='test_1kb_for_five_seconds', pay='bankcard', auto=1)
            self.pay_order(number=subscription)
            self.manual_success_callback_on_subscription(number=subscription)

            # выставляем дату биллинга в прошлом
            sid = self.get_services_list()[0][SID]
            self.service_manual_set_params(sid, btime=(ctimestamp() - 6))
            # биллимся, смотрим, что услуга удалалась
            mpfs.core.services.billing_service.BB.check_order = fake_check_order_subscription_stopped
            push_billing_commands()
            assert len(self.get_services_list()) == 0

            # если услуга удалилась, но к нам пришел ББ с пролонгацией, то райзим ошибку в джобе
            # Cейчас эта ошибка замалчивается в коде тестов. При отрывее нужно заменить на assertRaises
            self.manual_success_callback_on_subscription(number=subscription)
            services = self.get_services_list()
            assert len(services) == 0


class SubscriptionTestCase(BillingTestCaseMixin, BaseBillingTestCase):

    def test_success_first_subscription_process_callback(self):
        u"""
        Тестируем первую успешную оплату подписки
        """
        with mock.patch('mpfs.core.services.trust_payments.TrustPaymentsService.is_enabled', return_value=True):
            self.bind_user_to_market('RU')
            services_before = self.get_services_list(self.uid)
            number = self.place_order(auto=1)
            self.pay_order(number)

            with self.patch_check_order(BB_BTIME):
                self.manual_success_callback_on_subscription(number=number)
                services = self.get_services_list(self.uid)

            assert len(services) == len(services_before) + 1
            assert services[0]['expires'] == BB_BTIME / 1000

    def test_success_regular_subscription_process_callback(self):
        u"""
        Тестируем успешное продление подписки
        """
        with mock.patch('mpfs.core.services.trust_payments.TrustPaymentsService.is_enabled', return_value=True):
            self.bind_user_to_market('RU')
            number = self.place_order(auto=1)
            self.pay_order(number)

            with self.patch_check_order(BB_BTIME):
                self.manual_success_callback_on_subscription(number=number)
                services_before = self.get_services_list(self.uid)

            # Проверяем, что берем окончание подписки из ББ
            with self.patch_check_order(BB_BTIME + 1000):
                self.manual_success_callback_on_subscription(number=number)
                services = self.get_services_list(self.uid)

            assert len(services) == len(services_before)
            assert services[0]['expires'] > services_before[0]['expires']
            assert services[0]['expires'] == (BB_BTIME + 1000) / 1000

    def test_error_first_subscription_process_callback(self):
        u"""
        Тестируем ошибочную первую оплату подписки
        """
        with mock.patch('mpfs.core.services.trust_payments.TrustPaymentsService.is_enabled', return_value=True):
            self.bind_user_to_market('RU')
            services_before = self.get_services_list(self.uid)
            number = self.place_order(auto=1)
            self.pay_order(number)

            # Эмулируем поведение ББ
            self.manual_fail_callback_on_subscription(number=number, status=CANCELLED)
            orders = self.billing_ok('order_list_current', {'uid': self.uid})
            assert not orders
            archive_orders = self.billing_ok('order_list_history', {'uid': self.uid})
            assert archive_orders[0]['number'] == number and archive_orders[0]['state'] == CANCELLED
            services = self.get_services_list(self.uid)
            assert len(services) == len(services_before)

    def test_error_regular_subscription_process_callback(self):
        u"""
        Тестируем ошибочное продление подписки
        """
        with mock.patch('mpfs.core.services.trust_payments.TrustPaymentsService.is_enabled', return_value=True):
            self.bind_user_to_market('RU')
            number = self.place_order(auto=1)
            self.pay_order(number)

            new_btime = ctimestamp()

            with self.patch_check_order(new_btime * 1000):
                self.manual_success_callback_on_subscription(number=number)
            services_before = self.get_services_list(self.uid)
            # Эмулируем поведение ББ
            with self.patch_check_order(new_btime * 1000, finish_ts=new_btime):
                for i in range(3):
                    self.manual_fail_callback_on_subscription(number=number, status=CANCELLED)

            services = self.get_services_list(self.uid)
            assert len(services) == len(services_before)
            assert services[0]['expires'] == services_before[0]['expires']
            assert services[0]['state'] is None

            with self.patch_check_order(new_btime * 1000, finish_ts=new_btime):
                self.manual_fail_callback_on_subscription(number=number, status=CANCELLED)

            services = self.get_services_list(self.uid)
            assert len(services) == len(services_before)

            with time_machine(datetime.fromtimestamp(new_btime + settings.billing['subscription_payment_cancel'] + 1)), \
                    self.patch_check_order(new_btime * 1000, finish_ts=new_btime):
                push_billing_commands()

            services = self.get_services_list(self.uid)
            assert len(services) == len(services_before) - 1

            archived_services = self.billing_ok('service_list_history', {'uid': self.uid, 'ip': '1.1.1.1'})
            assert 1 == len(archived_services)
            assert 'test_1kb_for_one_second' in {x['name'] for x in archived_services}


    def test_repeated_subscription_process_callback(self):
        u"""
        Тестируем поведение МПФС в случае, когда ББ ошибочно присылает повторные колбеки при создании подписки
        """
        with mock.patch('mpfs.core.services.trust_payments.TrustPaymentsService.is_enabled', return_value=True):
            self.bind_user_to_market('RU')
            number = self.place_order(auto=1)
            self.pay_order(number)

            btime = ctimestamp()
            btime_msec = btime * 1000
            # Первый колбек, говорящий о том, что услугу не удалось оплатить
            with self.patch_check_order(btime_msec), capture_queue_errors() as errors:
                self.manual_fail_callback_on_subscription(number=number, status=CANCELLED)
                assert not errors

            # Повторный колбек, отосланный по ошибке ББ
            with self.patch_check_order(btime_msec), capture_queue_errors() as errors:
                self.manual_fail_callback_on_subscription(number=number, status=CANCELLED)
                assert not errors

            # Убеждаемся, что в случае, если btime в будущем - задача выполнится с ошибкой
            with self.patch_check_order(btime_msec), time_machine(datetime.fromtimestamp(btime - 1)), \
                    mock.patch('mpfs.frontend.api.auth.TVM2.authorize'), capture_queue_errors() as errors:
                # Повторный колбек, отосланный по ошибке ББ
                args = {
                    'uid': self.uid,
                    'number': number,
                    'status': 'error',
                    'status_code': 'test_code',
                }
                self.billing_ok('subscription_process_callback', args)
                assert len(errors) == 1
                assert isinstance(errors[0].error, billing_errors.BillingOrderUnexpectedStateError)

            # Убеждаемся, что в при отстутствии заказа задача выполнится с ошибкой
            with mock.patch('mpfs.frontend.api.auth.TVM2.authorize'), capture_queue_errors() as errors:
                # Повторный колбек, отосланный по ошибке ББ
                args['number'] = '123'
                args['status'] = CANCELLED
                self.billing_ok('subscription_process_callback', args)
                # проверяем, что колбек по несуществующему заказу зафейлился
                assert len(errors) == 1
                assert isinstance(errors[0].error, billing_errors.BillingOrderNotFound)

    @pytest.mark.skipif(True, reason='https://st.yandex-team.ru/CHEMODAN-32396')
    def test_subscription_process_callback_mode(self, testing_client, use_trust_payments):
        with patch('mpfs.core.billing.processing.common.stat') as mock_stat:
            self.bind_user_to_market('RU')

            pre_call_count = mock_stat.call_count
            self.manual_success_callback_on_subscription(number=self.place_order())
            assert mock_stat.call_count == pre_call_count + 2
            assert len(self.get_services_list(self.uid)) == 1
            args, kwargs = mock_stat.call_args
            assert 'status' in kwargs
            assert kwargs['status'] == SUCCESS

            # создаем подписку
            number = self.place_order(auto=1)

            # Тестируем продление подписки при mode=None
            pre_call_count = mock_stat.call_count
            self.manual_success_callback_on_subscription(number=number)
            assert mock_stat.call_count == pre_call_count + 1
            assert len(self.get_services_list(self.uid)) == 2
            args, kwargs = mock_stat.call_args
            assert 'status' in kwargs
            assert kwargs['status'] == SUCCESS

            # Тестируем продление подписки при mode=''
            pre_call_count = mock_stat.call_count
            self.manual_success_callback_on_subscription(number=number, mode='')
            assert mock_stat.call_count == pre_call_count + 1
            assert len(self.get_services_list(self.uid)) == 2
            args, kwargs = mock_stat.call_args
            assert 'status' in kwargs
            assert kwargs['status'] == SUCCESS

            # Тестируем продление подписки при недействительным значением mode
            pre_call_count = mock_stat.call_count
            self.manual_success_callback_on_subscription(number=number, mode='fake_mode')
            assert mock_stat.call_count == pre_call_count
            assert len(self.get_services_list(self.uid)) == 2

            # Тестируем поведение, если пришел refund, который был отменен (Подписка должна не пропасть)
            pre_call_count = mock_stat.call_count
            self.manual_fail_callback_on_subscription(number=number, mode='refund_result', status=CANCELLED)
            assert mock_stat.call_count == pre_call_count + 1
            assert len(self.get_services_list(self.uid)) == 2
            args, kwargs = mock_stat.call_args
            assert 'action' in kwargs
            assert 'status' in kwargs
            assert kwargs['action'] == REFUND_ACTION
            assert kwargs['status'] == CANCELLED

            # Тестируем поведени, если пришел успешный refund (Подписка должна удалиться)
            pre_call_count = mock_stat.call_count
            self.manual_success_callback_on_subscription(number=number, mode='refund_result')
            assert mock_stat.call_count == pre_call_count + 1
            assert len(self.get_services_list(self.uid)) == 1
            args, kwargs = mock_stat.call_args
            assert 'action' in kwargs
            assert 'status' in kwargs
            assert kwargs['action'] == REFUND_ACTION
            assert kwargs['status'] == SUCCESS

    def test_discarding_error_prolongation_if_subscription_expired(self):
        self.bind_user_to_market(market='RU')
        self.create_subscription(self.uid)
        services = self.get_services_list(self.uid)
        order_number = services[0]['order']

        self.service_delete(services[0]['sid'])
        assert not self.get_services_list(self.uid)

        mpfs.core.services.billing_service.BB.check_order = fake_check_order_error
        with capture_queue_errors() as errors:
            mpfs.core.services.billing_service.BB.check_order = fake_check_order_subscription_stopped
            self.manual_fail_callback_on_subscription(number=order_number, status=CANCELLED)
            mpfs.core.services.billing_service.BB.check_order = fake_check_order_good
        assert not errors

    def test_subscription_with_active_billing_dont_removed(self):

        new_btime = 555
        new_lbtime_msec = 333000

        self.bind_user_to_market(market='RU')
        self.create_subscription(self.uid)
        services = self.get_services_list(self.uid)

        # ставим btime в прошлом
        self.service_manual_set_params(services[0]['sid'], btime=0)

        # биллимся
        with self.patch_check_order(new_btime * 1000):
            push_billing_commands()
            service_list = self.get_services_list()
            # смотрим, что услуга не удалится после ответа биллинга без finish_ts
            assert len(service_list) == 1
            # смотрим, что btime изменился
            assert service_list[0]['expires'] == new_btime

        # биллимся, смотрим, что услуга удалится после ответа биллинга с finish_ts
        with self.patch_check_order(new_btime * 1000, finish_ts=new_btime):
            push_billing_commands()
            assert len(self.get_services_list()) == 0

    def test_subscription_btime_is_updated_on_callback(self):
        self.bind_user_to_market(market='RU')
        self.create_subscription(self.uid)
        services = self.get_services_list(self.uid)

        order_number = services[0]['order']
        old_btime = services[0]['expires']
        new_btime = old_btime + 12345

        with self.patch_check_order(new_btime * 1000):
            self.manual_success_callback_on_subscription(number=order_number)

        services = self.get_services_list(self.uid)
        assert services[0]['expires'] == new_btime

    def test_refund_subscription_with_cancelled_auto_prolongation(self):
        self.bind_user_to_market(market='RU')

        initial_btime = ctimestamp()

        with self.patch_check_order(initial_btime * 1000):
            self.create_subscription(self.uid)

        services = self.get_services_list(self.uid)

        assert len(services) == 1
        assert services[0]['expires'] == initial_btime

        order_number = services[0]['order']
        sid = services[0][SID]

        service = Service(sid=sid)
        assert service.auto

        with time_machine(datetime.fromtimestamp(initial_btime - 1)):  # отписываемся до окончания услуги
            self.service_unsubscribe(sid)

        service = Service(sid=sid)
        assert not service.auto

        btime_after_refund = initial_btime - 1

        # до первоначальной даты окончания услуги приходит колбек с рефандом от биллинга
        with time_machine(datetime.fromtimestamp(initial_btime - 1)),\
                self.patch_check_order(btime_after_refund * 1000):
            self.manual_success_callback_on_subscription(
                number=order_number, mode='refund_result', trust_refund_id='123')

        services = self.get_services_list(self.uid)
        assert not services

        order = ArchiveOrder(order_number)
        assert order.get(TRUST_REFUND_ID) == '123'
        assert order.get(REFUND_STATUS) == 'success'

    @parameterized.expand([
        ([12345, ], 0),
        ([123456789, ], 1),
        ([123456789, 12345], 1),
        ([123456789, 123456789], 2),
    ])
    def test_common_stat_log_dont_duplicate_on_extra_callback(self, btime_deltas, called_times):
        self.bind_user_to_market(market='RU')
        self.create_subscription(self.uid)
        services = self.get_services_list(self.uid)

        order_number = services[0]['order']
        old_btime = services[0]['expires']
        new_btime = old_btime
        with mock.patch('mpfs.core.billing.processing.common.stat') as mocked_stat:
            for btime_delta in btime_deltas:
                new_btime += btime_delta
                with self.patch_check_order(new_btime * 1000):
                    self.manual_success_callback_on_subscription(number=order_number)
                services = self.get_services_list(self.uid)
                assert services[0]['expires'] == new_btime
        assert len(mocked_stat.call_args_list) == called_times
