# -*- coding: utf-8 -*-
import datetime

import mock
from nose_parameterized import parameterized

import mpfs.engine.process
from mpfs.common.static.tags.billing import PRIMARY_2019_DISCOUNT_20, PRIMARY_2019_DISCOUNT_30
from mpfs.core.metastorage.control import disk_info
from mpfs.core.promo_codes.logic.discount import DiscountTemplate

from mpfs.common.util import ctimestamp
from mpfs.core.filesystem.dao.legacy import CollectionRoutedDatabase
from mpfs.core.queue import mpfs_queue
from test.base import time_machine, DiskTestCase

db = CollectionRoutedDatabase()
usrctl = mpfs.engine.process.usrctl()


class FollowUpTestCase(DiskTestCase):
    def setup_method(self, method):
        super(DiskTestCase, self).setup_method(method)
        self.create_user(self.uid)
        dt = DiscountTemplate.create(PRIMARY_2019_DISCOUNT_20, '', period_timedelta=datetime.timedelta(days=5))
        dt.save()
        self.discount_template_id = dt.id


    @parameterized.expand([
        ('no', 60 * 60 * 60, True),
        ('no', 2400, False),
        ('no', None, True),
        ('with', 60 * 60 * 600, False),
        ('with', 60 * 60 * 60, False),
        ('with', 2400, False),
        ('with', None, True),
    ])
    def test_update_last_send_datetime(self, discount, send_timedelta, correct_result):
        key = '%s_discount_follow_up_email_last_send' % discount
        if send_timedelta:
            now = ctimestamp()
            new_now = now - send_timedelta
            disk_info.put(self.uid, key, new_now)
        with mock.patch('mpfs.core.job_handlers.routine.PROMO_FOLLOW_UP_EMAILS_DISCOUNT_TEMPLATE',
                        self.discount_template_id):
            res_old = disk_info.value(self.uid, key).value
            if res_old:
                res_old = res_old.data
            task_name = '%s_discount_follow_up_email' % discount
            mpfs_queue.put(
                {'uid': self.uid},
                task_name,
                deduplication_id='%s__%s' % (task_name, self.uid),
            )
        res_new = disk_info.value(self.uid, key).value.data
        result = bool((not res_old and res_new) or res_new > res_old)
        self.assertEqual(correct_result, result)

    def test_discount_30_already_exists(self):
        dt = DiscountTemplate.create(PRIMARY_2019_DISCOUNT_30, '', period_timedelta=datetime.timedelta(days=5))
        dt.save()
        biggest_discount = dt.id
        key = 'with_discount_follow_up_email_last_send'
        with mock.patch('mpfs.core.promo_codes.logic.discount_manager.DiscountManager.get_cheapest_available_discount',
                        return_value=biggest_discount):
            task_name = 'with_discount_follow_up_email'
            mpfs_queue.put(
                {'uid': self.uid},
                task_name,
                deduplication_id='%s__%s' % (task_name, self.uid),
            )
        res_new = disk_info.value(self.uid, key).value
        assert not res_new
