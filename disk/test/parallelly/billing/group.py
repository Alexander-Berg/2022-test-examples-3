# -*- coding: utf-8 -*-
from datetime import datetime

import pytest

from test.parallelly.billing.base import (
    BaseBillingTestCase,
    fake_check_order_subscription_stopped,
    fake_check_order_good,
)
from test.fixtures.users import user_3

import mpfs.engine.process
import mpfs.core.services.billing_service

from test.base import time_machine
from test.api_suit import  set_up_mailbox, tear_down_mailbox

from test.helpers.stubs.services import BigBillingStub
from mpfs.common.static.tags.billing import *
from mpfs.core.billing.processing.billing import push_billing_commands
from mpfs.core.billing.processing.notify import push_notify_commands
from mpfs.common.util import ctimestamp
from mpfs.config import settings
from mpfs.core.filesystem.dao.legacy import CollectionRoutedDatabase


db = CollectionRoutedDatabase()


class GroupWorkflowTestCase(BaseBillingTestCase):
    def setup_method(self, method):
        super(GroupWorkflowTestCase, self).setup_method(method)

        self.uid_3 = user_3.uid
        self.run_000_user_check(self.uid_3)

        for uid in [self.uid, self.uid_3]:
            services = self.get_services_list(uid)
            if services:
                default_10gb_sid = services[0][SID]
                self.service_delete(default_10gb_sid)

    def test_workflow(self):
        """
        Простая проверка:
        - получить список всех групповых продуктов
        - заказать на группу пользователей
        - увидеть заказ
        - оплатить
        - принять коллбек
        - проверить, что услуга завелась
        - пробиллить и удалить
        """
        args = {'uid': self.uid}
        free_space_before = self.json_ok('user_info', args)['space']['limit']

        # биндим юзера к рынку
        self.bind_user_to_market(market='RU')

        # оформляем заказ
        group_uids = [self.uid, self.uid_3]
        with BigBillingStub():
            order = self.place_order(product='test_group_1kb_for_one_minute',
                                     group_uids=group_uids)

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
        with BigBillingStub():
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

        # проверяем, что услуга завелась и у нее есть в данных номер заказа - у пользователя uid должно быть 2 услуги,
        # т.к. он купил себе в том числе, а у uid_3 - одна
        result = self.get_services_list(self.uid)
        self.assertEqual(len(result), 2)
        self.assertTrue(ORDER in result[0])
        self.assertEqual(order, result[0][ORDER])
        sid = result[0][SID]
        sid2 = result[1][SID]

        result = self.get_services_list(self.uid_3)
        self.assertEqual(len(result), 1)
        self.assertTrue(ORDER in result[0])
        self.assertEqual(order, result[0][ORDER])
        sid3 = result[0][SID]

        # провяряем увеличение места
        args = {'uid': self.uid}
        space_limit_after = self.json_ok('user_info', args)['space']['limit']
        self.assertEqual(free_space_before + 1024, space_limit_after)
        args = {'uid': self.uid_3}
        space_limit_after = self.json_ok('user_info', args)['space']['limit']
        self.assertEqual(free_space_before + 1024, space_limit_after)

        # выставляем дату удаления одна секунда от текущего времени
        self.service_manual_set_params(sid, uid=self.uid, btime=(ctimestamp() + 1))
        self.service_manual_set_params(sid2, uid=self.uid, btime=(ctimestamp() + 1))
        self.service_manual_set_params(sid3, uid=self.uid_3, btime=(ctimestamp() + 1))

        # отсылаем нотификации о кончающемся действии услуги
        letters = set_up_mailbox()
        push_notify_commands()
        tear_down_mailbox()

        # выставляем дату удаления в прошлом
        # биллим услугу
        self.service_manual_set_params(sid, uid=self.uid, btime=(ctimestamp() - 1))
        self.service_manual_set_params(sid2, uid=self.uid, btime=(ctimestamp() - 1))
        self.service_manual_set_params(sid3, uid=self.uid_3, btime=(ctimestamp() - 1))
        push_billing_commands()

        # проверяем, что услуг не осталось
        result = self.get_services_list(self.uid)
        self.assertEqual(len(result), 0)
        result = self.get_services_list(self.uid_3)
        self.assertEqual(len(result), 0)

        # проверяем уменьшение места до изначального
        args = {'uid': self.uid}
        space_limit_after = self.json_ok('user_info', args)['space']['limit']
        self.assertEqual(free_space_before, space_limit_after)
        args = {'uid': self.uid_3}
        space_limit_after = self.json_ok('user_info', args)['space']['limit']
        self.assertEqual(free_space_before, space_limit_after)

    @pytest.mark.skipif(True, reason='https://st.yandex-team.ru/CHEMODAN-32396')
    def test_subscription(self):
        """
        Проверка подписки
        """
        group_uids = [self.uid, self.uid_3]

        args = {'uid': self.uid}
        free_space_before = self.json_ok('user_info', args)['space']['limit']

        # биндим юзера к рынку
        self.bind_user_to_market(market='RU')

        # args = {'line': 'group_2016', 'market': 'RU'}
        # self.billing_ok('product_list', args)

        # оформляем заказ
        with BigBillingStub():
            order = self.place_order(product='test_group_1kb_for_five_seconds',
                                     group_uids=group_uids,
                                     auto=1)

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
        with BigBillingStub():
            self.pay_order(number=order)

        # принимаем коллбек
        # в нашем случае он, конечно, фейковый, но суть не меняет
        self.manual_success_callback_on_subscription(number=order)

        # проверяем, что заказ ушел в историю
        args = {
            'uid': self.uid,
            'ip': self.localhost,
        }
        result = self.billing_ok('order_list_history', args)
        self.assertEqual(len(result), 1)
        self.assertEqual(result[0]['number'], order)

        check_result = []
        for uid in group_uids:
            # проверяем, что услуга завелась и у нее есть в данных номер заказа
            result = self.get_services_list(uid)
            assert len(result) > 0
            self.assertTrue(ORDER in result[0])
            self.assertEqual(order, result[0][ORDER])
            sid = result[0][SID]

            # выставляем дату биллинга в прошлом
            self.service_manual_set_params(sid, uid, btime=(ctimestamp() - 1))

            check_result.append((uid, sid))

        # биллим услугу
        push_billing_commands()

        check_result_btime = []
        for uid, sid in check_result:
            # проверяем, что перешла в состояние ожидания оплаты
            services = self.get_services_list(uid)
            assert len(result) > 0
            self.assertTrue(services[0]['state'] in (None, WAITING_PAYMENT))
            previous_billing_time = services[0]['expires']

            check_result_btime.append((uid, sid, previous_billing_time))

        # успешно продляем услугу автоплатежом
        self.manual_success_callback_on_subscription(number=order)

        for uid, sid, previous_billing_time in check_result_btime:
            # проверяем, что время следующей оплаты сдвинулось
            services = self.get_services_list(uid)
            next_billing_time = services[0]['expires']
            self.assertEqual(previous_billing_time + 5, next_billing_time)

            # выставляем дату биллинга в прошлом
            self.service_manual_set_params(sid, btime=(ctimestamp() - 1))

        # посылаем плохой коллбек
        self.manual_fail_callback_on_subscription(
            number=order,
            status=CANCELLED,
            status_code=NOT_ENOUGH_FUNDS
        )

        for uid in group_uids:
            # убеждаемся, что услуга перешла в состояние отмены
            services = self.get_services_list(uid)
            assert len(result) > 0
            self.assertEqual(services[0]['state'], PAYMENT_CANCELLED)
            self.assertEqual(services[0]['removes'],
                             services[0]['expires'] + settings.billing['subscription_payment_cancel'])

    def test_group_subscription_without_buyer(self):
        """
        Проверка подписки, если покупающий не входит в группу, для которой покупает
        Что ожидается:
        1. у пользователя, входящего в группу, появится услуга с флажком group: True и добавится место
        2. у купившего пользователя, не входящего в группу, появится услуга с флажком group: True, group_owner: True и
                не добавится место
        """
        group_uids = [self.uid_3, ]

        args = {'uid': self.uid}
        free_space_before = self.json_ok('user_info', args)['space']['limit']

        # биндим юзера к рынку
        self.bind_user_to_market(market='RU')

        # оформляем заказ
        with BigBillingStub():
            order = self.place_order(product='test_group_1kb_for_five_seconds',
                                     group_uids=group_uids,
                                     auto=1)

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
        with BigBillingStub():
            self.pay_order(number=order)

        # принимаем коллбек
        self.manual_success_callback_on_subscription(number=order)

        # проверяем, что заказ ушел в историю
        args = {
            'uid': self.uid,
            'ip': self.localhost,
        }
        result = self.billing_ok('order_list_history', args)
        self.assertEqual(len(result), 1)
        self.assertEqual(result[0]['number'], order)

        check_result = []
        for uid in group_uids:
            # проверяем, что услуга завелась и у нее есть в данных номер заказа
            result = self.get_services_list(uid)
            self.assertEqual(len(result), 1)
            self.assertTrue(ORDER in result[0])
            self.assertEqual(order, result[0][ORDER])
            sid = result[0][SID]

            # выставляем дату биллинга в прошлом
            self.service_manual_set_params(sid, uid, btime=(ctimestamp() - 1))

            check_result.append((uid, sid))

        # биллим услугу
        push_billing_commands()

        check_result_btime = []
        for uid, sid in check_result:
            # проверяем, что перешла в состояние ожидания оплаты
            services = self.get_services_list(uid)
            self.assertEqual(len(services), 1)
            self.assertTrue(services[0]['state'] in (None, WAITING_PAYMENT))
            previous_billing_time = services[0]['expires']

            check_result_btime.append((uid, sid, previous_billing_time))

        new_btime = ctimestamp() + 1000
        # успешно продляем услугу автоплатежом
        with self.patch_check_order(new_btime * 1000):
            self.manual_success_callback_on_subscription(number=order)

        # проверяем, что у купившего есть такая же услуга, с нужными флажками
        services = self.get_services_list(self.uid)
        assert len(services) == 1
        assert services[0]['group']
        assert services[0]['paid_for_other']
        assert services[0]['group_uids'] == [self.uid_3]

        # проверяем, что у купившего не изменился объем диска
        args = {'uid': self.uid}
        space_limit_after = self.json_ok('user_info', args)['space']['limit']
        self.assertEqual(free_space_before, space_limit_after)

        for uid, sid, previous_billing_time in check_result_btime:
            # проверяем, что время следующей оплаты сдвинулось
            services = self.get_services_list(uid)
            next_billing_time = services[0]['expires']
            self.assertEqual(new_btime, next_billing_time)

            # проверяем флажки
            assert services[0]['group']
            assert not services[0].get('paid_for_other', False)
            assert services[0]['buyer_uid'] == self.uid

            args = {'uid': uid}
            space_limit_after = self.json_ok('user_info', args)['space']['limit']
            self.assertEqual(free_space_before + 1024, space_limit_after)

            # выставляем дату биллинга в прошлом
            self.service_manual_set_params(sid, btime=(ctimestamp() - 1))

        # посылаем плохой коллбек
        self.manual_fail_callback_on_subscription(
            number=order,
            status=ERROR,
            status_code=NOT_ENOUGH_FUNDS
        )

        with self.patch_check_order(new_btime * 1000, finish_ts=new_btime):
            self.manual_fail_callback_on_subscription(
                number=order,
                status=CANCELLED,
                status_code=NOT_ENOUGH_FUNDS
            )

        # проверяем, что услуги не удалились после колбека
        for uid in group_uids:
            services = self.get_services_list(uid)
            self.assertEqual(len(services), 1)

        # проверяем, что услуги удалились после выполнения биллинговых тасков
        with time_machine(datetime.fromtimestamp(new_btime + settings.billing['subscription_payment_cancel'] + 1)), \
                self.patch_check_order(new_btime * 1000, finish_ts=new_btime):
            push_billing_commands()

        for uid in group_uids:
            services = self.get_services_list(uid)
            self.assertEqual(len(services), 0)
            archived_services = self.billing_ok('service_list_history', {'uid': uid, 'ip': '1.1.1.1'})
            assert 1 == len(archived_services)
            assert 'test_group_1kb_for_five_seconds_1' in {x['name'] for x in archived_services}

    def test_forever_service(self):
        args = {'uid': self.uid}
        free_space_before = self.json_ok('user_info', args)['space']['limit']

        # биндим юзера к рынку
        self.bind_user_to_market(market='RU')

        # оформляем заказ
        group_uids = [self.uid, ]
        with BigBillingStub():
            order = self.place_order(product='test_group_1kb_forever',
                                     group_uids=group_uids)

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
        with BigBillingStub():
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
        result = self.get_services_list(self.uid)
        self.assertEqual(len(result), 2)
        self.assertTrue(ORDER in result[0])
        self.assertEqual(order, result[0][ORDER])
        self.assertTrue(ORDER in result[1])
        self.assertEqual(order, result[1][ORDER])

        self.assertEqual(None, result[0]['expires'])
        self.assertEqual(None, result[1]['expires'])

        # провяряем увеличение места
        args = {'uid': self.uid}
        space_limit_after = self.json_ok('user_info', args)['space']['limit']
        self.assertEqual(free_space_before + 1024, space_limit_after)

    def test_group_name(self):
        """
        Проверяем, что если указать имя группы в заказе, потом его можно увидеть в списке услуг
        """
        # биндим юзера к рынку
        self.bind_user_to_market(market='RU')

        # оформляем заказ
        group_uids = [self.uid, ]
        group_name = 'test_group_name'
        with BigBillingStub():
            order = self.place_order(product='test_group_1kb_forever',
                                     group_uids=group_uids, group_name=group_name)

        # проверяем, что он появился
        args = {
            'uid': self.uid,
            'ip': self.localhost,
        }

        result = self.billing_ok('order_list_current', args)
        self.assertEqual(len(result), 1)
        self.assertEqual(result[0]['group_name'], group_name)

        # оплачиваем заказ
        with BigBillingStub():
            self.pay_order(number=order)

        # принимаем коллбек
        self.manual_success_callback_on_order(number=order)

        # проверяем, что услуга завелась и у нее есть в данных номер заказа
        result = self.get_services_list(self.uid)
        self.assertEqual(len(result), 2)
        self.assertEqual(group_name, result[0][GROUP_NAME])
        self.assertEqual(group_name, result[1][GROUP_NAME])
