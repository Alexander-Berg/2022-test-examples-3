# -*- coding: utf-8 -*-
import datetime
import mock
import time

from nose_parameterized import parameterized

from test.base import time_machine
from test.helpers.products import INITIAL_10GB, FILE_UPLOADED
from test.helpers.size_units import GB
from test.parallelly.billing.base import (
    BaseBillingTestCase,
    fake_check_order_bad,
    fake_check_order_good,
)
from test.helpers.stubs.services import BigBillingStub

import mpfs.engine.process
import mpfs.core.services.billing_service

from mpfs.common.static.tags.billing import *
from mpfs.config import settings
from mpfs.core.billing.processing.admin import archive_lost_orders
from mpfs.core.billing.processing.repair import repair_services
from mpfs.core.billing.order import Order
from mpfs.core.billing.processing.common import simple_create_service
from mpfs.core.billing.client import Client
from mpfs.core.billing.product import Product
from mpfs.core.filesystem.dao.legacy import CollectionRoutedDatabase
from mpfs.core.services.billing_service import BillingOrderInfo

db = CollectionRoutedDatabase()


class MiscTestCase(BaseBillingTestCase):

    def test_deprecated_callback_without_uid(self):
        u"""
        Проверяем работоспособность старых коллбеков, в которых не было uid
        """
        with mock.patch('mpfs.core.services.trust_payments.TrustPaymentsService.is_enabled', return_value=True):
            self.bind_user_to_market(market='RU')

            number = self.place_order(product='test_1kb_for_one_minute')
            self.pay_order(number=number)

            args = {
                'number': number,
                'status': 'success',
                'status_code': '',
            }
            with mock.patch('mpfs.frontend.api.auth.TVM2.authorize'):
                self.billing_ok('order_process_callback', args)

            services = self.get_services_list()
            self.assertEqual(len(services), 1)

    def test_move_timeouted_orders_to_history(self):
        u"""
        Проверяем,что происходит с заказами, на которые не пришли коллбеки
        """
        def fake_check_order(uid, ip, number, pid):
            return BillingOrderInfo({STATUS: WAIT_FOR_NOTIFICATION})

        with mock.patch('mpfs.core.services.trust_payments.TrustPaymentsService.is_enabled', return_value=True):
            mpfs.core.services.billing_service.BB.check_order = fake_check_order

            self.bind_user_to_market(market='RU')
            order = self.place_order(product='test_1kb_for_one_minute', pay='bankcard')

            time.sleep(2)
            archive_lost_orders()

            args = {
                'uid': self.uid,
                'ip': self.localhost,
            }
            result = self.billing_ok('order_list_history', args)

            self.assertEqual(result[0][NUMBER], order)
            self.assertEqual(result[0][STATE], LOST)

    @parameterized.expand([
        ('service_trust_payments', 0, None),
        ('subscription_trust_payments', 1, SUCCESS,),
        ('subscription_no_binding_trust_payments', 1, SKIPPED),
    ])
    def test_check_lost_order_service(self, case_name, auto, binding_result):
        u"""
        Проверяем, что перед переносом заказа в архив мы сходим в ББ и активируем услугу, если там она оплачена.
        Последующий колбек от биллинга не должен создать или продлить существующую услугу
        """
        check_order_result = {
            STATUS: SUCCESS,
        }
        if auto:
            check_order_result[PRODUCT_TYPE] = SUBS
            check_order_result[SUBS_UNTIL] = 111
            check_order_result[SUBS_UNTIL_MSEC] = 111000
            check_order_result['binding_result'] = binding_result
        else:
            check_order_result[PRODUCT_TYPE] = APP

        def fake_check_order(uid, ip, number, pid):
            return BillingOrderInfo(check_order_result)

        with mock.patch('mpfs.core.services.trust_payments.TrustPaymentsService.is_enabled', return_value=True):
            self.bind_user_to_market(market='RU')
            number = self.place_order(product='test_1kb_for_one_minute', pay='bankcard', auto=auto)
            mpfs.core.services.billing_service.BB.check_order = fake_check_order

            with BigBillingStub():
                with mock.patch.dict(settings.billing, {'orders_lost_interval': 0}):
                    with time_machine(datetime.datetime.now() + datetime.timedelta(days=1)):
                        archive_lost_orders()
                    services_before = self.get_services_list(self.uid)
                    assert len(services_before) == 1
                    assert services_before[0]['name'] == 'test_1kb_for_one_minute'
                    assert services_before[0]['subscription'] == (auto and binding_result in (SUCCESS, SKIPPED))

                    self.manual_success_callback(number, auto)
                    services_after = self.get_services_list(self.uid)
                    assert len(services_after) == 1
                    assert services_before[0]['expires'] == services_after[0]['expires']

    def test_workaround_fuckup_order_archive(self):
        u"""
        Если заказ потеряли, а потом ББ присылает по нему коллбек -
        надо поднять и провести
        """
        mpfs.core.services.billing_service.BB.check_order = fake_check_order_bad

        with mock.patch('mpfs.core.services.trust_payments.TrustPaymentsService.is_enabled', return_value=True):
            self.bind_user_to_market(market='RU')
            number = self.place_order(product='test_1kb_for_one_minute')
            self.pay_order(number=number)

            # Портим заказ, перемещая его в архив
            order = Order(number)
            order.set(CTIME, 666)
            order.archive()
            del order

            # колбечим
            self.manual_success_callback_on_order(number=number)

            # проверяем, что услуга завелась и это не подписка
            services = self.get_services_list()
            self.assertEqual(len(services), 1)
            self.assertEqual(services[0]['state'], None)
            self.assertEqual(services[0]['subscription'], False)

    def test_workaround_fuckup_order_current(self):
        u"""
        Если у заказа дико странное время, то надо создать услугу от
        нормального времени от ББ
        """
        mpfs.core.services.billing_service.BB.check_order = fake_check_order_bad

        with mock.patch('mpfs.core.services.trust_payments.TrustPaymentsService.is_enabled', return_value=True):
            self.bind_user_to_market(market='RU')
            number = self.place_order(product='test_1kb_for_one_minute')
            self.pay_order(number=number)

            # Портим заказ, убирая у него время
            order = Order(number)
            order.set(CTIME, 666)
            del order

            # колбечим
            self.manual_success_callback_on_order(number=number)

            # проверяем, что услуга завелась
            services = self.get_services_list()
            self.assertEqual(len(services), 1)
            self.assertEqual(services[0]['state'], None)
            self.assertEqual(services[0]['subscription'], False)


class RepairTestCase(BaseBillingTestCase):
    def test_repair_when_has_no_services(self):
        """Проверяет восстановление, когда нет услуг.

        Должна быть заведена услугу 10GB
        """
        # берем юзера и смотрим, что он голодранец
        services = self.get_services_list()
        assert len(services) == 0

        user_info = self.json_ok('user_info', {'uid': self.uid})
        old_limit = user_info['space']['limit']
        assert old_limit == self.DEFAULT_SPACE_LIMIT

        # запускаем мегапересчет
        repair_services(self.uid)

        # ожидаем увидеть 10Гб
        services = self.get_services_list(with_default_products=True)
        assert len(services) == 1
        assert services[0]['name'] == INITIAL_10GB.id

        user_info = self.json_ok('user_info', {'uid': self.uid})
        new_limit = user_info['space']['limit']
        assert new_limit == INITIAL_10GB.amount

    def test_repair_when_has_only_one_service(self):
        """Проверяет восстановление, когда имеется только 1 услуга типа file_upload.

        Пользователю должна быть заведена услуга 10GB, а услуги-бонусы - удалены.
        """
        # создаем одну услугу
        self.service_create(pid=FILE_UPLOADED.id, line='bonus')

        # берем юзера и смотрим, что он почти голодранец
        services = self.get_services_list()
        assert len(services) == 1

        user_info = self.json_ok('user_info', {'uid': self.uid})
        old_limit = user_info['space']['limit']
        assert old_limit == self.DEFAULT_SPACE_LIMIT + FILE_UPLOADED.amount

        # запускаем мегапересчет
        repair_services(self.uid)

        # ожидаем увидеть 10Гб
        services = self.get_services_list(with_default_products=True)
        assert len(services) == 1
        assert services[0]['name'] == INITIAL_10GB.id

        user_info = self.json_ok('user_info', {'uid': self.uid})
        new_limit = user_info['space']['limit']
        assert new_limit == INITIAL_10GB.amount

    def test_repair_when_doesnt_have_one_or_two_services(self):
        """
        Супервосстановление всего пользовательского скарба
        Ситуация когда у него есть почти все, кроме одной услуги
        """
        # создаем все, что можно
        client = Client(self.uid)
        for p in ('file_uploaded', 'app_install', 'initial_3gb'):
            simple_create_service(client, Product(p))

        # берем юзера и смотрим, что он почти голодранец
        services = self.get_services_list()
        assert len(services) == 3

        user_info = self.json_ok('user_info', {'uid': self.uid})
        old_limit = user_info['space']['limit']
        assert old_limit == 18*GB

        # запускаем мегапересчет
        repair_services(self.uid)

        # ожидаем увидеть 10Гб
        services = self.get_services_list(with_default_products=True)
        assert len(services) == 1
        user_info = self.json_ok('user_info', {'uid': self.uid})
        new_limit = user_info['space']['limit']
        assert new_limit == 10*GB
