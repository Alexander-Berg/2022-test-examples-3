# -*- coding: utf-8 -*-
import pytest
import mock

from datetime import datetime

from mpfs.common.static import SPACE_1GB
from test.base import time_machine
from test.helpers.stubs.resources.users_info import DEFAULT_USERS_INFO
from test.helpers.stubs.services import PushServicesStub, PassportStub
from test.parallelly.billing.base import BaseBillingTestCase

from mpfs.common.static.tags.push import SERVICE_PROLONGATION

from test.api_suit import set_up_mailbox, tear_down_mailbox, patch_mailbox
from mpfs.common.static.tags.billing import *
from mpfs.core.billing.order import ArchiveOrder
from mpfs.core.billing.processing.billing import push_billing_commands
from mpfs.core.billing.processing.notify import push_notify_commands
from mpfs.common.util import ctimestamp
from mpfs.core.filesystem.dao.legacy import CollectionRoutedDatabase


db = CollectionRoutedDatabase()


class OnetimeWorkflowTestCase(BaseBillingTestCase):

    def test_workflow(self):
        u"""
        Простая проверка:
        - заказать
        - увидеть заказ
        - оплатить
        - принять коллбек
        - проверить, что услуга завелась
        - пробиллить и удалить
        """

        with mock.patch('mpfs.core.services.trust_payments.TrustPaymentsService.is_enabled', return_value=True):
            args = {'uid': self.uid}
            free_space_before = self.json_ok('user_info', args)['space']['limit']

            # биндим юзера к рынку
            self.bind_user_to_market(market='RU')

            # оформляем заказ
            order = self.place_order(product='test_1kb_for_one_second')

            # проверяем, что он появился
            args = {
                'uid': self.uid,
                'ip': self.localhost,
            }

            result = self.billing_ok('order_list_current', args)
            self.assertEqual(len(result), 1)
            self.assertEqual(result[0]['number'], order)
            self.assertTrue(ORDER_TYPE in result[0])
            self.assertEqual(result[0][ORDER_TYPE], BUY_NEW)

            # оплачиваем заказ
            self.pay_order(number=order)

            # принимаем коллбек
            # в нашем случае он, конечно, фейковый, но суть не меняет
            self.manual_success_callback_on_order(number=order)

            # проверяем, что заказ ушел в историю
            args = {
                'uid': self.uid,
                'ip': self.localhost,
            }
            result = self.billing_ok('order_list_history', args)
            self.assertEqual(len(result), 1)
            self.assertEqual(result[0]['number'], order)

            # проверяем, что услуга завелась и у нее есть в данных номер заказа
            result = self.get_services_list()
            self.assertEqual(len(result), 1)
            self.assertTrue(ORDER in result[0])
            self.assertEqual(order, result[0][ORDER])
            sid = result[0][SID]

            # провяряем увеличение места
            args = {'uid': self.uid}
            space_limit_after = self.json_ok('user_info', args)['space']['limit']
            self.assertEqual(free_space_before + 1024, space_limit_after)

            # выставляем дату удаления одна секунда от текущего времени
            self.service_manual_set_params(sid, btime=(ctimestamp() + 1))

            # отсылаем нотификации о кончающемся действии услуги
            letters = set_up_mailbox()
            push_notify_commands()
            tear_down_mailbox()

            letter_sent = False
            for item in letters:
                if 'billing/service/monthServiceEnds' in item['args']:
                    letter_sent = True
            self.assertTrue(letter_sent)

            # выставляем дату удаления в прошлом
            # биллим услугу
            self.service_manual_set_params(sid, btime=(ctimestamp() - 1))
            push_billing_commands()

            # проверяем, что услуг не осталось
            result = self.get_services_list()
            self.assertEqual(len(result), 0)

            # проверяем уменьшение места до изначального
            args = {'uid': self.uid}
            space_limit_after = self.json_ok('user_info', args)['space']['limit']
            self.assertEqual(free_space_before, space_limit_after)

    @pytest.mark.skipif(True, reason='https://st.yandex-team.ru/CHEMODAN-32396')
    def test_try_to_pay_order_with_yamoney(self):
        with mock.patch('mpfs.core.services.trust_payments.TrustPaymentsService.is_enabled', return_value=True):
            # биндим юзера к рынку
            self.bind_user_to_market(market='RU')

            order = self.place_order(product='test_1kb_for_one_second', pay='yamoney')
            result = self.pay_order(number=order)
            self.assertTrue('url' in result)
            self.assertTrue('params' in result)

    def test_workflow_manual_prolongate(self):
        with mock.patch('mpfs.core.services.trust_payments.TrustPaymentsService.is_enabled', return_value=True):
            # биндим юзера к рынку
            self.bind_user_to_market(market='RU')

            # создаем заказ, оплачиваем, присылаем коллбек
            create_order = self.place_order(product='test_10GB_for_five_minutes')
            self.pay_order(number=create_order)
            self.manual_success_callback_on_order(create_order)

            # проверяем
            services = self.get_services_list()
            sid = services[0][SID]
            expires = int(services[0]['expires'])

            # ручное продление - создаем заказ
            args = {
                'uid': self.uid,
                'sid': sid,
                'payment_method': 'bankcard',
                'ip': self.localhost,
            }
            result = self.billing_ok('service_prolongate', args)
            self.assertTrue(result is not None)
            prolongate_order = result['number']

            # проверяем, что заказ есть
            args = {
                'uid': self.uid,
                'ip': self.localhost,
            }
            result = self.billing_ok('order_list_current', args)
            self.assertTrue(ORDER_TYPE in result[0])
            self.assertEqual(result[0][ORDER_TYPE], PROLONG_MANUAL)

            args = {'uid': self.uid}
            old_space_limit = self.json_ok('user_info', args)['space']['limit']

            with PushServicesStub() as push_service:
                # коллбечим на заказ продления
                self.manual_success_callback_on_order(prolongate_order)
                pushes = [PushServicesStub.parse_send_call(push_args) for push_args in push_service.send.call_args_list]

            # проверяем, что при успешном продлении отправляем пуш о продлении
            assert pushes
            assert pushes[0]['event_name'] == SERVICE_PROLONGATION
            assert pushes[0]['json_payload']['root']['tag'] == SERVICE_PROLONGATION
            assert pushes[0]['json_payload']['root']['parameters']['state'] == SUCCESS

            # проверяем, что услуга продлилась
            services = self.get_services_list()
            assert len(services) == 1
            new_expires = int(services[0]['expires'])
            self.assertTrue(new_expires == expires + 300)
            assert services[0]['order'] == prolongate_order

            # проверяем, что место не изменилось
            args = {'uid': self.uid}
            new_space_limit = self.json_ok('user_info', args)['space']['limit']

            self.assertEqual(old_space_limit, new_space_limit)

    def test_billing_manual_prolongate_after_auto_canceled(self):
        u"""Проверить возможно ли продлить вручную авто-продлеваемую услугу после того как продление было отменено."""
        with mock.patch('mpfs.core.services.trust_payments.TrustPaymentsService.is_enabled', return_value=True):
            # биндим юзера к рынку
            self.bind_user_to_market(market='RU')

            # создаем заказ, оплачиваем, присылаем коллбек
            create_order = self.place_order(product='test_10GB_for_five_minutes', auto=bool(True))
            self.pay_order(number=create_order)

            btime = ctimestamp() + 300

            with self.patch_check_order(btime * 1000):
                self.manual_success_callback_on_subscription(create_order)

            # проверяем
            services = self.get_services_list()
            sid = services[0][SID]
            expires = int(services[0]['expires'])

            # отменяем автопродление
            self.service_unsubscribe(sid)

            # ручное продление - создаем заказ
            args = {
                'uid': self.uid,
                'sid': sid,
                'payment_method': 'bankcard',
                'ip': self.localhost,
            }
            result = self.billing_ok('service_prolongate', args)
            assert result is not None
            prolongate_order = result['number']

            # # проверяем, что заказ есть
            args = {
                'uid': self.uid,
                'ip': self.localhost,
            }
            result = self.billing_ok('order_list_current', args)
            self.assertTrue(ORDER_TYPE in result[0])
            self.assertEqual(result[0][ORDER_TYPE], PROLONG_MANUAL)

            # коллбечим на заказ продления
            self.manual_success_callback_on_order(prolongate_order)

            # проверяем, что услуга продлилась
            services = self.get_services_list()
            new_expires = int(services[0]['expires'])
            assert new_expires - expires == 300

    def test_refund_workflow_for_manual_prolongated_service(self):
        self.bind_user_to_market(market='RU')

        order = self.place_order(product='test_10GB_for_five_minutes', auto=bool(True))
        self.pay_order(number=order)

        initial_btime = ctimestamp() + 300

        with self.patch_check_order(initial_btime * 1000):
            self.manual_success_callback_on_subscription(order)

        services = self.get_services_list()
        assert len(services) == 1
        assert services[0]['expires'] == initial_btime

        old_space_limit = self.json_ok('user_info', {'uid': self.uid})['space']['limit']
        sid = services[0][SID]

        with time_machine(datetime.fromtimestamp(initial_btime - 1)):  # отписываемся до окончания услуги
            self.service_unsubscribe(sid)

        # ручное продление - создаем заказ
        result = self.billing_ok('service_prolongate', {
            'uid': self.uid,
            'sid': sid,
            'payment_method': 'bankcard',
            'ip': self.localhost,
        })
        prolongate_order_1 = result['number']

        with PushServicesStub():
            # коллбечим на заказ продления
            self.manual_success_callback_on_order(prolongate_order_1)

        # проверяем, что услуга продлилась
        services = self.get_services_list()
        assert len(services) == 1
        new_expires_1 = int(services[0]['expires'])
        self.assertTrue(new_expires_1 == initial_btime + 300)
        assert services[0]['order'] == prolongate_order_1

        # проверяем, что место не изменилось
        args = {'uid': self.uid}
        new_space_limit = self.json_ok('user_info', args)['space']['limit']

        self.assertEqual(old_space_limit, new_space_limit)

        # еще раз продляем
        result = self.billing_ok('service_prolongate', {
            'uid': self.uid,
            'sid': sid,
            'payment_method': 'bankcard',
            'ip': self.localhost,
        })
        prolongate_order_2 = result['number']

        with PushServicesStub():
            # коллбечим на заказ продления
            self.manual_success_callback_on_order(prolongate_order_2)

        # проверяем, что услуга продлилась
        services = self.get_services_list()
        assert len(services) == 1
        new_expires_2 = int(services[0]['expires'])
        self.assertTrue(new_expires_2 == new_expires_1 + 300)
        assert services[0]['order'] == prolongate_order_2

        # проверяем, что место не изменилось
        args = {'uid': self.uid}
        new_space_limit = self.json_ok('user_info', args)['space']['limit']

        self.assertEqual(old_space_limit, new_space_limit)

        # шлем refund callback
        self.manual_success_callback_on_order(number=prolongate_order_2, mode='refund_result', trust_refund_id='123')

        services = self.get_services_list()
        new_expires_3 = int(services[0]['expires'])
        self.assertTrue(new_expires_3 == new_expires_1)

        order = ArchiveOrder(prolongate_order_2)
        assert order.get(TRUST_REFUND_ID) == '123'
        assert order.get(REFUND_STATUS) == 'success'

        # проверяем, что место не изменилось
        args = {'uid': self.uid}
        new_space_limit = self.json_ok('user_info', args)['space']['limit']

        self.assertEqual(old_space_limit, new_space_limit)

        # шлем повторный refund callback
        self.manual_success_callback_on_order(number=prolongate_order_2, mode='refund_result', trust_refund_id='789')

        # и проверяем, что ничего не изменилось
        services = self.get_services_list()
        new_expires_4 = int(services[0]['expires'])
        self.assertTrue(new_expires_4 == new_expires_1)

        order = ArchiveOrder(prolongate_order_2)
        assert order.get(TRUST_REFUND_ID) == '123'
        assert order.get(REFUND_STATUS) == 'success'

        # проверяем, что место не изменилось
        args = {'uid': self.uid}
        new_space_limit = self.json_ok('user_info', args)['space']['limit']

        self.assertEqual(old_space_limit, new_space_limit)

        # рефандим первое ручное продление
        with time_machine(datetime.fromtimestamp(initial_btime + 1)):
            self.manual_success_callback_on_order(
                number=prolongate_order_1, mode='refund_result', trust_refund_id='234')

        services = self.get_services_list()
        assert not services

        order = ArchiveOrder(prolongate_order_1)
        assert order.get(TRUST_REFUND_ID) == '234'
        assert order.get(REFUND_STATUS) == 'success'

    def test_refund_first_order_for_manual_prolongated_service_before_btime(self):
        self.bind_user_to_market(market='RU')

        order = self.place_order(product='test_10GB_for_five_minutes', auto=bool(True))
        self.pay_order(number=order)

        initial_btime = ctimestamp() + 300

        with self.patch_check_order(initial_btime * 1000):
            self.manual_success_callback_on_subscription(order)

        services = self.get_services_list()
        assert len(services) == 1
        assert services[0]['expires'] == initial_btime

        sid = services[0][SID]

        with time_machine(datetime.fromtimestamp(initial_btime - 1)):  # отписываемся до окончания услуги
            self.service_unsubscribe(sid)

        # ручное продление - создаем заказ
        result = self.billing_ok('service_prolongate', {
            'uid': self.uid,
            'sid': sid,
            'payment_method': 'bankcard',
            'ip': self.localhost,
        })
        prolongate_order_1 = result['number']

        with PushServicesStub():
            # коллбечим на заказ продления
            self.manual_success_callback_on_order(prolongate_order_1)

        # проверяем, что услуга продлилась
        services = self.get_services_list()
        assert len(services) == 1
        new_expires_1 = int(services[0]['expires'])
        self.assertTrue(new_expires_1 == initial_btime + 300)
        assert services[0]['order'] == prolongate_order_1

        # шлем refund callback на самый первый (подписочный) заказ до btime
        with time_machine(datetime.fromtimestamp(initial_btime - 1)):
            self.manual_success_callback_on_order(number=order, mode='refund_result', trust_refund_id='123')

        services = self.get_services_list()
        assert len(services) == 1
        new_expires_2 = int(services[0]['expires'])
        self.assertTrue(new_expires_2 == initial_btime)

    def test_refund_first_order_for_manual_prolongated_service_after_btime(self):
        self.bind_user_to_market(market='RU')

        order = self.place_order(product='test_10GB_for_five_minutes', auto=bool(True))
        self.pay_order(number=order)

        initial_btime = ctimestamp() + 300

        with self.patch_check_order(initial_btime * 1000):
            self.manual_success_callback_on_subscription(order)

        services = self.get_services_list()
        assert len(services) == 1
        assert services[0]['expires'] == initial_btime

        sid = services[0][SID]

        with time_machine(datetime.fromtimestamp(initial_btime - 1)):  # отписываемся до окончания услуги
            self.service_unsubscribe(sid)

        # ручное продление - создаем заказ
        result = self.billing_ok('service_prolongate', {
            'uid': self.uid,
            'sid': sid,
            'payment_method': 'bankcard',
            'ip': self.localhost,
        })
        prolongate_order_1 = result['number']

        with PushServicesStub():
            # коллбечим на заказ продления
            self.manual_success_callback_on_order(prolongate_order_1)

        # проверяем, что услуга продлилась
        services = self.get_services_list()
        assert len(services) == 1
        new_expires_1 = int(services[0]['expires'])
        self.assertTrue(new_expires_1 == initial_btime + 300)
        assert services[0]['order'] == prolongate_order_1

        # шлем refund callback на самый первый (подписочный) заказ до btime
        with time_machine(datetime.fromtimestamp(initial_btime + 1)):
            self.manual_success_callback_on_order(number=order, mode='refund_result', trust_refund_id='123')

        services = self.get_services_list()
        assert not services

    def test_update_order_in_service_for_successful_prolongation(self):
        self.bind_user_to_market(market='RU')
        initial_order_id = self.place_order(product='test_10GB_for_five_minutes')
        self.pay_order(number=initial_order_id)
        self.manual_success_callback_on_order(initial_order_id)

        services = self.get_services_list()
        assert len(services) == 1
        sid = services[0][SID]

        result = self.billing_ok('service_prolongate', {
            'uid': self.uid,
            'sid': sid,
            'payment_method': 'bankcard',
            'ip': self.localhost,
        })
        self.assertTrue(result is not None)
        prolongate_order_id = result['number']

        self.manual_success_callback_on_order(prolongate_order_id)
        services = self.get_services_list()
        assert len(services) == 1
        assert services[0]['order'] == prolongate_order_id

    def test_not_update_order_in_service_for_unsuccessful_prolongation(self):
        self.bind_user_to_market(market='RU')
        initial_order_id = self.place_order(product='test_10GB_for_five_minutes')
        self.pay_order(number=initial_order_id)
        self.manual_success_callback_on_order(initial_order_id)

        services = self.get_services_list()
        assert len(services) == 1
        sid = services[0][SID]

        result = self.billing_ok('service_prolongate', {
            'uid': self.uid,
            'sid': sid,
            'payment_method': 'bankcard',
            'ip': self.localhost,
        })
        self.assertTrue(result is not None)
        prolongate_order_id = result['number']

        self.manual_fail_callback_on_order(prolongate_order_id, status='cancelled')
        services = self.get_services_list()
        assert len(services) == 1
        assert services[0]['order'] == initial_order_id

    def test_check_mail_pro_promo_20gb(self):
        # прверяем состояние до создания услуги
        services = self.get_services_list()
        user_info = self.json_ok('user_info', {'uid': self.uid})
        old_limit = user_info['space']['limit']
        assert not [x for x in services if x['name'] == 'mail_pro_promo_20gb']

        # создаём услугу
        self.service_create(pid='mail_pro_promo_20gb', line='bonus')

        # пытаемся создать услугу ещё раз
        args = {'uid': self.uid, 'pid': 'mail_pro_promo_20gb', 'line': 'bonus', 'ip': 'localhost'}
        self.billing_error('service_create', args, status=409)

        # проверяем состояние после создания услуги
        services = self.get_services_list()
        assert [x for x in services if x['name'] == 'mail_pro_promo_20gb']
        user_info = self.json_ok('user_info', {'uid': self.uid})
        new_limit = user_info['space']['limit']
        assert new_limit - old_limit == 21474836480

        # удаляем услугу
        self.service_delete(pid='mail_pro_promo_20gb')

        # пытаемся удалить услугу повторно
        self.billing_error('service_delete', args, status=404)

        # проверяем состояние после удаления услуги
        services = self.get_services_list()
        assert not [x for x in services if x['name'] == 'mail_pro_promo_20gb']
        user_info = self.json_ok('user_info', {'uid': self.uid})
        new_limit = user_info['space']['limit']
        assert new_limit == old_limit

    def test_check_no_mail_for_deleted_account(self):
        # создаём услугу
        self.service_create(pid='b2b_10gb', line='bonus')

        # удаляем услугу
        with mock.patch('mpfs.core.overdraft.notifications.send_email_async_by_uid') as send_email, \
            mock.patch('mpfs.common.util.experiments.logic.ExperimentManager.is_feature_active', return_value=True), \
            mock.patch('mpfs.core.services.disk_service.MPFSStorageService.free', return_value=-SPACE_1GB*2), \
                PassportStub(userinfo=DEFAULT_USERS_INFO['deleted_account']):
            self.service_delete(pid='b2b_10gb')

            assert send_email.call_count == 0
