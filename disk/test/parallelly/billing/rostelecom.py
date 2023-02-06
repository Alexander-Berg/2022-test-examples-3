# -*- coding: utf-8 -*-
import time
import datetime

import mock
from nose_parameterized import parameterized

from mpfs.common.static import codes, SPACE_1TB
from test.base import time_machine
from test.helpers.size_units import GB, TB
from test.parallelly.billing.base import BaseBillingTestCase

from mpfs.common.static.codes import (
    ROSTELECOM_UNLIM_SERVICE_IS_DEACTIVATED,
    ROSTELECOM_UNLIM_SERVICE_IS_BLOCKED,
    ROSTELECOM_UNLIM_SERVICE_IS_ACTIVATED,
    ROSTELECOM_UNLIM_INCORRECT_SERVICE_KEY,
)
from mpfs.common.static.tags.billing import ROSTELECOM_UNLIM, ROSTELECOM_UNLIM_TEST
from mpfs.core.billing.processing.billing import push_billing_commands
from mpfs.core.rostelecom_unlim.constants import (
    ROSTELECOM_SERVICE_KEYS,
    ROSTELECOM_UNLIM_1,
    ROSTELECOM_UNLIM_2,
    ROSTELECOM_UNLIM_3,
    ROSTELECOM_PID_TO_SERVICE_KEY,
    ROSTELECOM_UNLIM_1_TEST,
    ROSTELECOM_UNLIM_3_TEST,
    ROSTELECOM_UNLIM_2_TEST,
)

PRODUCT_LINE = ROSTELECOM_UNLIM


class RostelecomTestCase(BaseBillingTestCase):
    def _get_free_space(self):
        return self.json_ok('user_info', {'uid': self.uid})['space']['limit']

    def _get_services_product_names(self):
        return [s['name'] for s in self.get_services_list()]

    @parameterized.expand(['app_install', ROSTELECOM_UNLIM_1])
    def test_activate_does_not_create_other_services(self, pid):
        opts = {'uid': self.uid, 'service_key': pid}
        self.json_error('rostelecom_activate', opts, code=ROSTELECOM_UNLIM_INCORRECT_SERVICE_KEY)

    def test_deactivate_does_not_delete_other_services(self):
        pid = 'app_install'
        self.service_create(pid, line='bonus')
        assert [pid] == self._get_services_product_names()
        opts = {'uid': self.uid, 'service_key': pid}
        self.json_error('rostelecom_deactivate', opts, code=ROSTELECOM_UNLIM_INCORRECT_SERVICE_KEY)

    @parameterized.expand([
        ('rostelecom_vas_5gb', 5 * GB), ('rostelecom_vas_100gb', 100 * GB), ('rostelecom_vas_1tb', 1024 * GB),
    ])
    def test_activate_deactivate_vas_services(self, pid, space_size):
        assert [] == self.get_services_list()
        assert self.DEFAULT_SPACE_LIMIT == self._get_free_space()

        opts = {'uid': self.uid, 'service_key': pid}
        self.json_ok('rostelecom_activate', opts)

        assert [pid] == self._get_services_product_names()
        assert self.DEFAULT_SPACE_LIMIT + space_size == self._get_free_space()

        self.json_ok('rostelecom_deactivate', opts)

        assert [] == self.get_services_list()
        assert self.DEFAULT_SPACE_LIMIT == self._get_free_space()

    def test_activate_creates_unlim_service(self):
        assert [] == self.get_services_list()
        assert self.DEFAULT_SPACE_LIMIT == self._get_free_space()

        opts = {'uid': self.uid, 'service_key': ROSTELECOM_UNLIM}
        self.json_ok('rostelecom_activate', opts)

        assert [ROSTELECOM_UNLIM_1] == self._get_services_product_names()
        assert self.DEFAULT_SPACE_LIMIT + 1*TB == self._get_free_space()

    @parameterized.expand([ROSTELECOM_UNLIM_1, ROSTELECOM_UNLIM_2, ROSTELECOM_UNLIM_3])
    def test_deactivate_deletes_any_unlim_service(self, pid):
        self.service_create(pid, PRODUCT_LINE)
        assert self._get_free_space() > self.DEFAULT_SPACE_LIMIT

        opts = {'uid': self.uid, 'service_key': ROSTELECOM_UNLIM}
        self.json_ok('rostelecom_deactivate', opts)

        assert [] == self._get_services_product_names()
        assert self.DEFAULT_SPACE_LIMIT == self._get_free_space()

    def test_deactivate_deletes_multiple_unlim_services(self):
        self.service_create(ROSTELECOM_UNLIM_1, PRODUCT_LINE)
        self.service_create(ROSTELECOM_UNLIM_2, PRODUCT_LINE)
        self.service_create(ROSTELECOM_UNLIM_3, PRODUCT_LINE)
        self.assertSetEqual(
            {ROSTELECOM_UNLIM_1, ROSTELECOM_UNLIM_2, ROSTELECOM_UNLIM_3},
            set(self._get_services_product_names()),
        )
        assert self._get_free_space() > self.DEFAULT_SPACE_LIMIT

        opts = {'uid': self.uid, 'service_key': ROSTELECOM_UNLIM}
        self.json_ok('rostelecom_deactivate', opts)

        assert [] == self._get_services_product_names()
        assert self.DEFAULT_SPACE_LIMIT == self._get_free_space()

    def test_error_if_vas_product_activated_second_time(self):
        pid = 'rostelecom_vas_5gb'
        opts = {'uid': self.uid, 'service_key': pid}
        self.json_ok('rostelecom_activate', opts)
        self.json_error('rostelecom_activate', opts, code=codes.ROSTELECOM_UNLIM_SERVICE_IS_ACTIVATED)

    @parameterized.expand([ROSTELECOM_UNLIM_1, ROSTELECOM_UNLIM_2, ROSTELECOM_UNLIM_3])
    def test_error_if_unlim_product_activated_second_time(self, existing_pid):
        opts = {'uid': self.uid, 'pid': existing_pid, 'line': PRODUCT_LINE, 'ip': '127.0.0.1'}
        self.billing_ok('service_create', opts)

        opts = {'uid': self.uid, 'service_key': ROSTELECOM_UNLIM}
        self.json_error('rostelecom_activate', opts, code=codes.ROSTELECOM_UNLIM_SERVICE_IS_ACTIVATED)

    @parameterized.expand(['rostelecom_vas_5gb', ROSTELECOM_UNLIM])
    def test_error_if_deactivate_not_existing_service(self, pid):
        assert [] == self._get_services_product_names()
        opts = {'uid': self.uid, 'service_key': pid}
        self.json_error('rostelecom_deactivate', opts, code=codes.ROSTELECOM_UNLIM_SERVICE_IS_DEACTIVATED)

    @parameterized.expand([ROSTELECOM_UNLIM_1, ROSTELECOM_UNLIM_2, ROSTELECOM_UNLIM_3,
                           'rostelecom_vas_5gb', 'rostelecom_vas_100gb', 'rostelecom_vas_1tb'])
    def test_service_create_any_service(self, pid):
        opts = {'uid': self.uid, 'pid': pid, 'line': PRODUCT_LINE, 'ip': '127.0.0.1'}
        self.billing_ok('service_create', opts)
        assert [pid] == self._get_services_product_names()

    def test_activate_error_for_uninitialized_user(self):
        pid = 'rostelecom_vas_5gb'
        uid = 123
        opts = {'uid': uid, 'service_key': pid}
        self.json_error('rostelecom_activate', opts, code=codes.WH_USER_NEED_INIT)

    @parameterized.expand([
        (ROSTELECOM_UNLIM_1, ROSTELECOM_UNLIM,),
        (ROSTELECOM_UNLIM_2, ROSTELECOM_UNLIM,),
        (ROSTELECOM_UNLIM_3, ROSTELECOM_UNLIM,),
        ('rostelecom_vas_5gb', 'rostelecom_vas_5gb'),
        ('rostelecom_vas_100gb', 'rostelecom_vas_100gb'),
        ('rostelecom_vas_1tb', 'rostelecom_vas_1tb'),
        (ROSTELECOM_UNLIM_1_TEST, ROSTELECOM_UNLIM_TEST,),
        (ROSTELECOM_UNLIM_2_TEST, ROSTELECOM_UNLIM_TEST,),
        (ROSTELECOM_UNLIM_3_TEST, ROSTELECOM_UNLIM_TEST,),
        ('rostelecom_vas_5gb_test', 'rostelecom_vas_5gb_test'),
        ('rostelecom_vas_100gb_test', 'rostelecom_vas_100gb_test'),
        ('rostelecom_vas_1tb_test', 'rostelecom_vas_1tb_test'),
    ])
    def test_freeze_active_service(self, pid, rostelecom_service_key):
        correct_limit = self.json_ok('space', {'uid': self.uid})['limit']

        opts = {'uid': self.uid, 'pid': pid, 'line': PRODUCT_LINE, 'ip': '127.0.0.1'}
        self.billing_ok('service_create', opts)

        self.json_ok('rostelecom_freeze', {'uid': self.uid, 'service_key': rostelecom_service_key})

        limit = self.json_ok('space', {'uid': self.uid})['limit']

        assert not self._get_services_product_names()
        assert limit == correct_limit

    @parameterized.expand([
        ROSTELECOM_UNLIM,
        'rostelecom_vas_5gb',
        'rostelecom_vas_100gb',
        'rostelecom_vas_1tb',
    ])
    def test_freeze_inactive_service_returns_error(self, rostelecom_service_key):
        self.json_error('rostelecom_freeze', {'uid': self.uid, 'service_key': rostelecom_service_key}, code=ROSTELECOM_UNLIM_SERVICE_IS_DEACTIVATED)

    @parameterized.expand([
        (ROSTELECOM_UNLIM_1, ROSTELECOM_UNLIM,),
        (ROSTELECOM_UNLIM_2, ROSTELECOM_UNLIM,),
        (ROSTELECOM_UNLIM_3, ROSTELECOM_UNLIM,),
        ('rostelecom_vas_5gb', 'rostelecom_vas_5gb'),
        ('rostelecom_vas_100gb', 'rostelecom_vas_100gb'),
        ('rostelecom_vas_1tb', 'rostelecom_vas_1tb'),
        (ROSTELECOM_UNLIM_1_TEST, ROSTELECOM_UNLIM_TEST,),
        (ROSTELECOM_UNLIM_2_TEST, ROSTELECOM_UNLIM_TEST,),
        (ROSTELECOM_UNLIM_3_TEST, ROSTELECOM_UNLIM_TEST,),
        ('rostelecom_vas_5gb_test', 'rostelecom_vas_5gb_test'),
        ('rostelecom_vas_100gb_test', 'rostelecom_vas_100gb_test'),
        ('rostelecom_vas_1tb_test', 'rostelecom_vas_1tb_test'),
    ])
    def test_freeze_frozen_service_returns_error(self, pid, rostelecom_service_key):
        opts = {'uid': self.uid, 'pid': pid, 'line': PRODUCT_LINE, 'ip': '127.0.0.1'}
        self.billing_ok('service_create', opts)
        self.json_ok('rostelecom_freeze', {'uid': self.uid, 'service_key': rostelecom_service_key})
        self.json_error('rostelecom_freeze', {'uid': self.uid, 'service_key': rostelecom_service_key}, code=ROSTELECOM_UNLIM_SERVICE_IS_BLOCKED)

    @parameterized.expand([
        (ROSTELECOM_UNLIM_1, ROSTELECOM_UNLIM,),
        (ROSTELECOM_UNLIM_2, ROSTELECOM_UNLIM,),
        (ROSTELECOM_UNLIM_3, ROSTELECOM_UNLIM,),
        ('rostelecom_vas_5gb', 'rostelecom_vas_5gb'),
        ('rostelecom_vas_100gb', 'rostelecom_vas_100gb'),
        ('rostelecom_vas_1tb', 'rostelecom_vas_1tb'),
        (ROSTELECOM_UNLIM_1_TEST, ROSTELECOM_UNLIM_TEST,),
        (ROSTELECOM_UNLIM_2_TEST, ROSTELECOM_UNLIM_TEST,),
        (ROSTELECOM_UNLIM_3_TEST, ROSTELECOM_UNLIM_TEST,),
        ('rostelecom_vas_5gb_test', 'rostelecom_vas_5gb_test'),
        ('rostelecom_vas_100gb_test', 'rostelecom_vas_100gb_test'),
        ('rostelecom_vas_1tb_test', 'rostelecom_vas_1tb_test'),
    ])
    def test_unfreeze_on_active_service_returns_error(self, pid, rostelecom_service_key):
        opts = {'uid': self.uid, 'pid': pid, 'line': PRODUCT_LINE, 'ip': '127.0.0.1'}
        self.billing_ok('service_create', opts)
        self.json_error('rostelecom_unfreeze', {'uid': self.uid, 'service_key': rostelecom_service_key}, code=ROSTELECOM_UNLIM_SERVICE_IS_ACTIVATED)

    @parameterized.expand([
        ROSTELECOM_UNLIM,
        'rostelecom_vas_5gb',
        'rostelecom_vas_100gb',
        'rostelecom_vas_1tb',
        ROSTELECOM_UNLIM_TEST,
        'rostelecom_vas_5gb_test',
        'rostelecom_vas_100gb_test',
        'rostelecom_vas_1tb_test',
    ])
    def test_unfreeze_on_inactive_service_returns_error(self, rostelecom_service_key):
        self.json_error('rostelecom_unfreeze', {'uid': self.uid, 'service_key': rostelecom_service_key}, code=ROSTELECOM_UNLIM_SERVICE_IS_DEACTIVATED)

    @parameterized.expand([
        'rostelecom_vas_5gb',
        'rostelecom_vas_100gb',
        'rostelecom_vas_1tb',
        'rostelecom_vas_5gb_test',
        'rostelecom_vas_100gb_test',
        'rostelecom_vas_1tb_test',
    ])
    def test_freeze_on_vas(self, rostelecom_service_key):
        opts = {'uid': self.uid, 'service_key': rostelecom_service_key}
        self.json_ok('rostelecom_activate', opts)

        correct_services = self._get_services_product_names()
        correct_limit = self.json_ok('space', {'uid': self.uid})['limit']

        self.json_ok('rostelecom_freeze', opts)
        self.json_ok('rostelecom_unfreeze', opts)

        services = self._get_services_product_names()
        limit = self.json_ok('space', {'uid': self.uid})['limit']

        assert services == correct_services
        assert limit == correct_limit

    @parameterized.expand([
        (ROSTELECOM_UNLIM_1, ROSTELECOM_UNLIM,),
        (ROSTELECOM_UNLIM_2, ROSTELECOM_UNLIM,),
        (ROSTELECOM_UNLIM_3, ROSTELECOM_UNLIM,),
        (ROSTELECOM_UNLIM_1_TEST, ROSTELECOM_UNLIM_TEST,),
        (ROSTELECOM_UNLIM_2_TEST, ROSTELECOM_UNLIM_TEST,),
        (ROSTELECOM_UNLIM_3_TEST, ROSTELECOM_UNLIM_TEST,),
    ])
    def test_freeze_in_same_day_does_not_change_btime(self, pid, rostelecom_service_key):
        opts = {'uid': self.uid, 'pid': pid, 'line': PRODUCT_LINE, 'ip': '127.0.0.1'}
        self.billing_ok('service_create', opts)

        correct_services = self._get_services_product_names()
        correct_limit = self.json_ok('space', {'uid': self.uid})['limit']
        correct_btime = self.get_services_list(uid=self.uid, pid=pid)[0]['expires']

        opts = {'uid': self.uid, 'service_key': rostelecom_service_key}
        self.json_ok('rostelecom_freeze', opts)
        self.json_ok('rostelecom_unfreeze', opts)

        services = self._get_services_product_names()
        limit = self.json_ok('space', {'uid': self.uid})['limit']
        btime = self.get_services_list(uid=self.uid, pid=pid)[0]['expires']

        assert services == correct_services
        assert limit == correct_limit
        assert btime == correct_btime

    @parameterized.expand([
        (ROSTELECOM_UNLIM_1, ROSTELECOM_UNLIM, False,),
        (ROSTELECOM_UNLIM_1, ROSTELECOM_UNLIM, True,),
        (ROSTELECOM_UNLIM_2, ROSTELECOM_UNLIM, False,),
        (ROSTELECOM_UNLIM_2, ROSTELECOM_UNLIM, True,),
        (ROSTELECOM_UNLIM_3, ROSTELECOM_UNLIM, False,),
        (ROSTELECOM_UNLIM_3, ROSTELECOM_UNLIM, True,),
        (ROSTELECOM_UNLIM_1_TEST, ROSTELECOM_UNLIM_TEST, False,),
        (ROSTELECOM_UNLIM_1_TEST, ROSTELECOM_UNLIM_TEST, True,),
        (ROSTELECOM_UNLIM_2_TEST, ROSTELECOM_UNLIM_TEST, False,),
        (ROSTELECOM_UNLIM_2_TEST, ROSTELECOM_UNLIM_TEST, True,),
        (ROSTELECOM_UNLIM_3_TEST, ROSTELECOM_UNLIM_TEST, False,),
        (ROSTELECOM_UNLIM_3_TEST, ROSTELECOM_UNLIM_TEST, True,),
    ])
    def test_freeze_on_in_different_days_changes_btime(self, pid, rostelecom_service_key, freeze_day_is_active):
        opts = {'uid': self.uid, 'pid': pid, 'line': PRODUCT_LINE, 'ip': '127.0.0.1'}
        self.billing_ok('service_create', opts)

        correct_services = self._get_services_product_names()
        correct_limit = self.json_ok('space', {'uid': self.uid})['limit']
        correct_btime = self.get_services_list(uid=self.uid, pid=pid)[0]['expires']

        with time_machine(datetime.datetime.now() + datetime.timedelta(days=1)):
            opts = {'uid': self.uid, 'service_key': rostelecom_service_key}
            if freeze_day_is_active:
                self.json_ok('rostelecom_freeze', opts)
                self.json_ok('rostelecom_unfreeze', opts)
            self.json_ok('rostelecom_freeze', opts)

        with time_machine(datetime.datetime.now() + datetime.timedelta(days=3)):
            self.json_ok('rostelecom_unfreeze', opts)
            services = self._get_services_product_names()
            limit = self.json_ok('space', {'uid': self.uid})['limit']
            btime = self.get_services_list(uid=self.uid, pid=pid)[0]['expires']

        assert services == correct_services
        assert limit == correct_limit
        if freeze_day_is_active:
            correct_btime += int(datetime.timedelta(days=1).total_seconds())
        else:
            correct_btime += int(datetime.timedelta(days=2).total_seconds())
        assert btime == correct_btime

    def test_unfreeze_restores_previous_custom_size_correctly_for_rostelecom_unlim_3(self):
        opts = {'uid': self.uid, 'pid': ROSTELECOM_UNLIM_3, 'line': PRODUCT_LINE, 'ip': '127.0.0.1', 'product.amount': 999}
        self.billing_ok('service_create', opts)
        correct_limit = self.json_ok('space', {'uid': self.uid})['limit']
        opts = {'uid': self.uid, 'service_key': ROSTELECOM_UNLIM}
        self.json_ok('rostelecom_freeze', opts)
        self.json_ok('rostelecom_unfreeze', opts)
        limit = self.json_ok('space', {'uid': self.uid})['limit']
        assert limit == correct_limit

    @parameterized.expand([
        (ROSTELECOM_UNLIM_1, ROSTELECOM_UNLIM,),
        ('rostelecom_vas_5gb', 'rostelecom_vas_5gb'),
        ('rostelecom_vas_100gb', 'rostelecom_vas_100gb'),
        ('rostelecom_vas_1tb', 'rostelecom_vas_1tb'),
        (ROSTELECOM_UNLIM_1_TEST, ROSTELECOM_UNLIM_TEST,),
        ('rostelecom_vas_5gb_test', 'rostelecom_vas_5gb_test'),
        ('rostelecom_vas_100gb_test', 'rostelecom_vas_100gb_test'),
        ('rostelecom_vas_1tb_test', 'rostelecom_vas_1tb_test'),
    ])
    def test_freeze_freezes_necessary_services_only(self, pid, rostelecom_service_key):
        for p in ROSTELECOM_SERVICE_KEYS:
            self.json_ok('rostelecom_activate', {'uid': self.uid, 'service_key': p})

        self.json_ok('rostelecom_freeze', {'uid': self.uid, 'service_key': rostelecom_service_key})
        products = set(self._get_services_product_names())
        correct_products = {
            'rostelecom_vas_5gb_test', 'rostelecom_vas_100gb_test', 'rostelecom_vas_1tb_test', ROSTELECOM_UNLIM_1_TEST,
            'rostelecom_vas_5gb', 'rostelecom_vas_100gb', 'rostelecom_vas_1tb', ROSTELECOM_UNLIM_1
        } - {pid}
        assert correct_products == products

    @parameterized.expand([
        (ROSTELECOM_UNLIM_1, ROSTELECOM_UNLIM,),
        ('rostelecom_vas_5gb', 'rostelecom_vas_5gb'),
        ('rostelecom_vas_100gb', 'rostelecom_vas_100gb'),
        ('rostelecom_vas_1tb', 'rostelecom_vas_1tb'),
        (ROSTELECOM_UNLIM_1_TEST, ROSTELECOM_UNLIM_TEST,),
        ('rostelecom_vas_5gb_test', 'rostelecom_vas_5gb_test'),
        ('rostelecom_vas_100gb_test', 'rostelecom_vas_100gb_test'),
        ('rostelecom_vas_1tb_test', 'rostelecom_vas_1tb_test'),
    ])
    def test_unfreeze_unfreezes_necessary_services_only(self, pid, rostelecom_service_key):
        for p in ROSTELECOM_SERVICE_KEYS:
            self.json_ok('rostelecom_activate', {'uid': self.uid, 'service_key': p})
            self.json_ok('rostelecom_freeze', {'uid': self.uid, 'service_key': p})

        self.json_ok('rostelecom_unfreeze', {'uid': self.uid, 'service_key': rostelecom_service_key})
        products = set(self._get_services_product_names())
        correct_products = {pid}
        assert correct_products == products

    def test_have_unlim_duplicates_then_leve_the_biggest_during_freeze(self):
        for p in [ROSTELECOM_UNLIM_1, ROSTELECOM_UNLIM_3, ROSTELECOM_UNLIM_2]:
            self.billing_ok('service_create', {'uid': self.uid, 'pid': p, 'line': PRODUCT_LINE, 'ip': '127.0.0.1'})
        opts = {'uid': self.uid, 'service_key': ROSTELECOM_UNLIM}
        self.json_ok('rostelecom_freeze', opts)
        self.json_ok('rostelecom_unfreeze', opts)
        products = set(self._get_services_product_names())
        assert products == {ROSTELECOM_UNLIM_3}

    @parameterized.expand([
        (ROSTELECOM_UNLIM_1, ROSTELECOM_UNLIM,),
        (ROSTELECOM_UNLIM_2, ROSTELECOM_UNLIM,),
        (ROSTELECOM_UNLIM_3, ROSTELECOM_UNLIM,),
        ('rostelecom_vas_5gb', 'rostelecom_vas_5gb'),
        ('rostelecom_vas_100gb', 'rostelecom_vas_100gb'),
        ('rostelecom_vas_1tb', 'rostelecom_vas_1tb'),
        (ROSTELECOM_UNLIM_1_TEST, ROSTELECOM_UNLIM_TEST,),
        (ROSTELECOM_UNLIM_2_TEST, ROSTELECOM_UNLIM_TEST,),
        (ROSTELECOM_UNLIM_3_TEST, ROSTELECOM_UNLIM_TEST,),
        ('rostelecom_vas_5gb_test', 'rostelecom_vas_5gb_test'),
        ('rostelecom_vas_100gb_test', 'rostelecom_vas_100gb_test'),
        ('rostelecom_vas_1tb_test', 'rostelecom_vas_1tb_test'),
    ])
    def test_deactivate_frozen_service(self, pid, rostelecom_service_key):
        opts = {'uid': self.uid, 'pid': pid, 'line': PRODUCT_LINE, 'ip': '127.0.0.1'}
        self.billing_ok('service_create', opts)

        opts = {'uid': self.uid, 'service_key': rostelecom_service_key}
        self.json_ok('rostelecom_freeze', opts)

        self.json_ok('rostelecom_deactivate', opts)
        self.json_error('rostelecom_freeze', opts, code=ROSTELECOM_UNLIM_SERVICE_IS_DEACTIVATED)

    def test_freeze_handlers_does_not_work_for_non_rostelecom_pids(self):
        pid = 'app_install'
        self.service_create(pid, line='bonus')
        self.json_error('rostelecom_freeze', {'uid': self.uid, 'service_key': 'app_install'}, code=ROSTELECOM_UNLIM_INCORRECT_SERVICE_KEY)

    @parameterized.expand([
        (ROSTELECOM_UNLIM_1, ROSTELECOM_UNLIM_2,),
        (ROSTELECOM_UNLIM_2, ROSTELECOM_UNLIM_3,),
        (ROSTELECOM_UNLIM_1_TEST, ROSTELECOM_UNLIM_2_TEST,),
        (ROSTELECOM_UNLIM_2_TEST, ROSTELECOM_UNLIM_3_TEST,),
    ])
    def test_billing_processing_script_upgrades_unlim_services(self, old_pid, new_pid):
        opts = {'uid': self.uid, 'pid': old_pid, 'line': PRODUCT_LINE, 'ip': '127.0.0.1'}
        self.billing_ok('service_create', opts)
        old_limit = self.json_ok('space', {'uid': self.uid})['limit']

        # правим btime так, что услуга должно заапгрейдиться
        services = self.billing_ok('service_list', {'uid': self.uid, 'ip': '127.0.0.1'})
        service_dict = [service
                        for service in services
                        if service['name'] == old_pid][0]
        opts = {
            'uid': self.uid,
            'sid': service_dict['sid'],
            'ip': '127.0.0.1',
            'key': 'service.btime',
            'value': int(time.time()) - 100
        }
        self.billing_ok('service_set_attribute', opts)
        with mock.patch('mpfs.core.billing.processing.notify.service_create') as create_notify_mock, \
                mock.patch('mpfs.core.billing.processing.notify.service_deleted') as delete_notify_mock:
            push_billing_commands()
            create_notify_mock.assert_not_called()
            delete_notify_mock.assert_not_called()

        new_limit = self.json_ok('space', {'uid': self.uid})['limit']

        assert self._get_services_product_names()[0] == new_pid
        assert new_limit == old_limit + SPACE_1TB

    def test_billing_processing_script_increases_size_of_rostelecom_unlim_3(self):
        opts = {
            'uid': self.uid,
            'pid': ROSTELECOM_UNLIM_3,
            'line': PRODUCT_LINE,
            'ip': '127.0.0.1',
            'product.amount': 10 * SPACE_1TB
        }
        self.billing_ok('service_create', opts)
        old_limit = self.json_ok('space', {'uid': self.uid})['limit']

        # правим btime так, что услуга должно заапгрейдиться
        services = self.billing_ok('service_list', {'uid': self.uid, 'ip': '127.0.0.1'})
        service_dict = [service
                        for service in services
                        if service['name'] == ROSTELECOM_UNLIM_3][0]
        btime_timestamp = int(time.time()) - 100
        correct_new_btime_timestamp = btime_timestamp + int(datetime.timedelta(days=30).total_seconds())
        opts = {
            'uid': self.uid,
            'sid': service_dict['sid'],
            'ip': '127.0.0.1',
            'key': 'service.btime',
            'value': int(time.time()) - 100
        }
        self.billing_ok('service_set_attribute', opts)
        push_billing_commands()

        new_limit = self.json_ok('space', {'uid': self.uid})['limit']

        assert self._get_services_product_names()[0] == ROSTELECOM_UNLIM_3
        assert new_limit == old_limit + SPACE_1TB

        services = self.billing_ok('service_list', {'uid': self.uid, 'ip': '127.0.0.1'})
        service_dict = [service
                        for service in services
                        if service['name'] == ROSTELECOM_UNLIM_3][0]
        assert service_dict['expires'] == correct_new_btime_timestamp

    @parameterized.expand([
        ('inactive', 'inactive', 'inactive', 'inactive', 'inactive', 'inactive',
         'inactive', 'inactive', 'inactive', 'inactive', 'inactive', 'inactive',),
        ('active', 'inactive', 'inactive', 'inactive', 'active', 'active',
         'inactive', 'inactive', 'inactive', 'inactive', 'inactive', 'inactive',),
        ('blocked', 'inactive', 'inactive', 'inactive', 'blocked', 'blocked',
         'inactive', 'inactive', 'inactive', 'inactive', 'inactive', 'inactive',),
        ('active', 'inactive', 'active', 'inactive', 'blocked', 'blocked',
         'inactive', 'inactive', 'inactive', 'inactive', 'inactive', 'inactive',),
    ])
    def test_list_services(self, *args):
        service_statuses = dict(zip(['rostelecom_unlim_1', 'rostelecom_unlim_2', 'rostelecom_unlim_3',
                                     'rostelecom_vas_1tb', 'rostelecom_vas_100gb', 'rostelecom_vas_5gb',
                                     'rostelecom_unlim_1_test', 'rostelecom_unlim_2_test', 'rostelecom_unlim_3_test',
                                     'rostelecom_vas_1tb_test', 'rostelecom_vas_100gb_test', 'rostelecom_vas_5gb_test'
                                     ], args))
        for pid, status in service_statuses.items():
            if status in ('active', 'blocked'):
                self.service_create(pid, PRODUCT_LINE)
            if status == 'blocked':
                service_key = ROSTELECOM_PID_TO_SERVICE_KEY.get(pid, pid)
                self.json_ok('rostelecom_freeze', {'uid': self.uid, 'service_key': service_key})

        resp = self.json_ok('rostelecom_list_services', {'uid': self.uid})
        assert resp == service_statuses

    def test_freezeing_several_unlim_services_leaves_only_one_service_blocked(self):
        opts = {
            'uid': self.uid,
            'pid': ROSTELECOM_UNLIM_1,
            'line': PRODUCT_LINE,
            'ip': '127.0.0.1',
        }
        self.billing_ok('service_create', opts)
        self.json_ok('rostelecom_freeze', {'uid': self.uid, 'service_key': ROSTELECOM_UNLIM})
        opts = {
            'uid': self.uid,
            'pid': ROSTELECOM_UNLIM_2,
            'line': PRODUCT_LINE,
            'ip': '127.0.0.1',
        }
        self.billing_ok('service_create', opts)
        self.json_ok('rostelecom_freeze', {'uid': self.uid, 'service_key': ROSTELECOM_UNLIM})
        resp = self.json_ok('rostelecom_list_services', {'uid': self.uid})
        assert resp[ROSTELECOM_UNLIM_1] == 'inactive'
        assert resp[ROSTELECOM_UNLIM_2] == 'blocked'

    @parameterized.expand([
        ROSTELECOM_UNLIM,
        'rostelecom_vas_5gb',
        'rostelecom_vas_100gb',
        'rostelecom_vas_1tb',
        ROSTELECOM_UNLIM_TEST,
        'rostelecom_vas_5gb_test',
        'rostelecom_vas_100gb_test',
        'rostelecom_vas_1tb_test',
    ])
    def test_rostelecom_service_makes_user_paid(self, rostelecom_service_key):
        self.json_ok('rostelecom_activate', {'uid': self.uid, 'service_key': rostelecom_service_key})
        user_info = self.json_ok('user_info', {'uid': self.uid})
        feature_info = self.json_ok('user_feature_toggles', {'uid': self.uid})
        assert user_info['paid'] == 1 and feature_info['disk_pro']

    @parameterized.expand([
        ROSTELECOM_UNLIM,
        'rostelecom_vas_5gb',
        'rostelecom_vas_100gb',
        'rostelecom_vas_1tb',
    ])
    def test_freeze_inactive_service_returns_error(self, rostelecom_service_key):
        self.json_ok('rostelecom_activate', {'uid': self.uid, 'service_key': rostelecom_service_key})
        with mock.patch('mpfs.core.billing.processing.notify.service_deleted') as call_mock:
            self.json_ok('rostelecom_freeze', {'uid': self.uid, 'service_key': rostelecom_service_key})
        assert not call_mock.called
