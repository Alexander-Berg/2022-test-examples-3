# -*- coding: utf-8 -*-
import time
from lxml import etree

from test.parallelly.billing.base import BaseBillingTestCase

import mpfs.engine.process
import mpfs.core.services.billing_service

from test.api_suit import set_up_mailbox, tear_down_mailbox, patch_mailbox
from test.base_suit import set_up_open_url, tear_down_open_url
from test.fixtures.users import user_1, user_3
from mpfs.common.static.tags.billing import *
from mpfs.core.billing.processing.billing import push_billing_commands
from mpfs.core.billing.service import Service
from mpfs.core.billing.product import Product
from mpfs.common.util import ctimestamp
from mpfs.core.filesystem.dao.legacy import CollectionRoutedDatabase


db = CollectionRoutedDatabase()


class BonusPacksTestCase(BaseBillingTestCase):
    other_uid = user_1.uid
    not_init_uid = user_3.uid

    def test_bonus_pack_create_notify(self):
        """
        Проверяем, что при создании услуги с письмом на создание продукта высылается нотификация
        """
        opts = {'uid': self.uid, 'line': 'bonus', 'pid': 'yandex_mail_mobile_app_installation', 'ip': '127.0.0.1'}
        with patch_mailbox() as letters:
            resp = self.billing_ok('service_create', opts)
            assert 'sid' in resp
            assert len(letters) == 1
            assert letters[0]['args'][1] == 'spacePack/mobmail'

        with patch_mailbox() as letters:
            opts['uid'] = self.other_uid
            opts['send_email'] = 0
            opts['auto_init_user'] = 1
            resp = self.billing_ok('service_create', opts)
            assert 'sid' in resp
            assert len(letters) == 0

        with patch_mailbox() as letters:
            opts['uid'] = self.not_init_uid
            opts['send_email'] = 1
            opts['auto_init_user'] = 0
            resp = self.billing_ok('service_create', opts)
            assert resp['code'] == 44
            assert len(letters) == 0

    def test_bonus_pack_remove_notify(self):
        """
        Проверяем, что при удалении бонусного продукта высылается нотификация
        """
        self.service_create(line=BONUS, pid='photostream_used')
        pack_size = Product('photostream_used').attributes.amount

        services = self.get_services_list()
        service = Service(services[0][SID])
        service.set(BTIME, ctimestamp())
        del service

        time.sleep(1)

        pushes = set_up_open_url()
        letters = set_up_mailbox()
        push_billing_commands()
        tear_down_mailbox()
        tear_down_open_url()

        template = letters[0]['args'][1]
        self.assertEqual(template, 'spacePack/EndingAction')

        user_notified = False
        for k, v in pushes.iteritems():
            if k.startswith('http://localhost/service/echo?uid='):
                for each in v:
                    quota = etree.fromstring(each['pure_data'])
                    self.assertEqual(quota.get('uid'), str(self.uid))
                    self.assertEqual(quota.get('reduced'), str(pack_size))
                    user_notified = True

        self.assertTrue(user_notified)

        services = self.get_services_list()
        self.assertEqual(len(services), 0)

    def test_attach_user_with_music_2012(self):
        from mpfs.core.user.base import Create

        Create(self.attach_uid, type='attach')

        sid = self.service_create(
            line=BONUS,
            pid='music_dec_2012',
            uid=self.attach_uid,
            market='RU'
        )
        self.assertTrue(sid is not None)

        current_time = ctimestamp() - 10000
        args = {
            'uid': self.attach_uid,
            'sid': sid,
            'ip': self.localhost,
            'key': 'service.btime',
            'value': current_time,
        }
        self.billing_ok('service_set_attribute', args)

        services = self.get_services_list(uid=self.attach_uid)
        self.assertEqual(services[0]['expires'], current_time)

        push_billing_commands()

        services = self.get_services_list(uid=self.attach_uid)
        self.assertEqual(len(services), 0)

    def test_pro_for_testing(self):
        assert self.json_ok('user_info', {'uid': self.uid})['paid'] == 0
        self.billing_ok('service_create',
                        {'uid': self.uid, 'line': 'bonus', 'pid': 'force_yandex_pro', 'ip': '127.0.0.1'})
        assert self.json_ok('user_info', {'uid': self.uid})['paid'] == 1
