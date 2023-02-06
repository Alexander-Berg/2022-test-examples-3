# -*- coding: utf-8 -*-
import mock
from nose_parameterized import parameterized
from test.parallelly.billing.base import BaseBillingTestCase

import mpfs.engine.process
import mpfs.core.services.billing_service

from test.api_suit import set_up_mailbox, tear_down_mailbox
from mpfs.common.static.tags.billing import *
from mpfs.core.billing.processing.billing import push_billing_commands
from mpfs.core.billing.processing.notify import push_notify_commands
from mpfs.common.util import ctimestamp
from mpfs.core.filesystem.dao.legacy import CollectionRoutedDatabase


db = CollectionRoutedDatabase()


class Products2015WorkflowTestCase(BaseBillingTestCase):
    def test_workflow(self):
        u"""
        Простая проверка:
        - заказать
        - увидеть заказ
        - оплатить
        - принять коллбек
        - проверить, что услуга завелась
        - пробиллить и удалить
        
        Все это делаем для продуктов 2015 года
        """
        with mock.patch('mpfs.core.services.trust_payments.TrustPaymentsService.is_enabled', return_value=True):
            args = {'uid': self.uid}
            free_space_before = self.json_ok('user_info', args)['space']['limit']

            # биндим юзера к рынку
            self.bind_user_to_market(market='RU')

            # оформляем заказ
            order = self.place_order(product='10gb_1m_2015', line='primary_2015')
            print order

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
            self.assertEqual(free_space_before + 1024 ** 3 * 10, space_limit_after)

            # выставляем дату удаления: текущее время + время отсылки нотифайки о кончине места
            self.service_manual_set_params(sid, btime=(ctimestamp() + 259200))

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
