# -*- coding: utf-8 -*-
import mpfs.platform.v1.disk.rostelecom.handlers

import mock
from nose_parameterized import parameterized

from test.base_suit import UserTestCaseMixin, SupportApiTestCaseMixin, BillingApiTestCaseMixin
from test.helpers.size_units import GB
from test.helpers.stubs.services import PassportStub
from mpfs.common.static.tags.billing import ROSTELECOM_UNLIM
from mpfs.common.util import from_json
from mpfs.core.rostelecom_unlim.constants import ROSTELECOM_UNLIM_2, ROSTELECOM_UNLIM_1
from mpfs.platform.v1.disk.rostelecom.exceptions import (
    RostelecomServiceActiveError,
    RostelecomServiceBlockedError,
    RostelecomServiceDeactivatedError,
)
from test.parallelly.api.disk.base import DiskApiTestCase

from mpfs.common.static import tags
from test.parallelly.billing.rostelecom import PRODUCT_LINE


class TestRostelecomPlatformTestCase(SupportApiTestCaseMixin, BillingApiTestCaseMixin, UserTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.EXTERNAL
    api_version = 'v1'

    @classmethod
    def setup_class(cls):
        super(TestRostelecomPlatformTestCase, cls).setup_class()
        cls.prev_value = mpfs.platform.v1.disk.rostelecom.handlers.ROSTELECOM_UNLIM_ENABLED_FOR_UIDS
        mpfs.platform.v1.disk.rostelecom.handlers.ROSTELECOM_UNLIM_ENABLED_FOR_UIDS = []

    @classmethod
    def teardown_class(cls):
        mpfs.platform.v1.disk.rostelecom.handlers.ROSTELECOM_UNLIM_ENABLED_FOR_UIDS = cls.prev_value
        super(TestRostelecomPlatformTestCase, cls).teardown_class()

    def test_check_registered_user_returns_uid(self):
        self.json_ok('user_init', {'uid': self.uid})
        with self.specified_client(scopes=['cloud_api:disk.partners.rostelecom_unlim.user_check']), PassportStub():
            resp = self.client.request('GET', 'disk/rostelecom/cloud-platform/check')
        assert from_json(resp.content)['uid'] == self.uid

    def test_check_blocked_user_returns_404(self):
        self.json_ok('user_init', {'uid': self.uid})
        self.support_ok('block_user', {
            'uid': self.uid,
            'moderator': 'moderator',
            'comment': 'comment',
        })
        with self.specified_client(scopes=['cloud_api:disk.partners.rostelecom_unlim.user_check']), PassportStub():
            resp = self.client.request('GET', 'disk/rostelecom/cloud-platform/check')
        assert resp.status_code == 404

    def test_check_not_registered_initiable_user_returns_uid(self):
        with self.specified_client(scopes=['cloud_api:disk.partners.rostelecom_unlim.user_check']), PassportStub():
            resp = self.client.request('GET', 'disk/rostelecom/cloud-platform/check')
        assert from_json(resp.content)['uid'] == self.uid

    def test_check_not_registered_uninitiable_user_returns_403(self):
        with self.specified_client(scopes=['cloud_api:disk.partners.rostelecom_unlim.user_check']), \
                mock.patch('mpfs.core.base.can_init_user', return_value={'can_init': '0'}):
            resp = self.client.request('GET', 'disk/rostelecom/cloud-platform/check')
        assert resp.status_code == 403
        assert from_json(resp.content)['registration_link'] == 'https://passport.yandex.ru/profile/upgrade'

    @parameterized.expand([
        (True,),
        (False,),
    ])
    def test_activate_deactivated_service_successfully(self, need_init):
        if need_init:
            self.json_ok('user_init', {'uid': self.uid})
        with self.specified_client(scopes=['cloud_api:disk.partners.rostelecom_unlim.manage_services']), PassportStub():
            resp = self.client.request('PUT', 'disk/rostelecom/cloud-platform/activate', query={'uid': self.uid, 'service_key': 'rostelecom_unlim'})
        assert from_json(resp.content) == {'status': 'active'}
        services = self.billing_ok('service_list', {'uid': self.uid, 'ip': 'localhost'})
        assert 'rostelecom_unlim_1' in [x['name'] for x in services]

    def test_deactivate_active_service_successfully(self):
        self.json_ok('user_init', {'uid': self.uid})
        with self.specified_client(scopes=['cloud_api:disk.partners.rostelecom_unlim.manage_services']):
            self.client.request('PUT', 'disk/rostelecom/cloud-platform/activate', query={'uid': self.uid, 'service_key': 'rostelecom_unlim'})
            resp = self.client.request('PUT', 'disk/rostelecom/cloud-platform/deactivate', query={'uid': self.uid, 'service_key': 'rostelecom_unlim'})
        assert from_json(resp.content) == {'status': 'inactive'}
        services = self.billing_ok('service_list', {'uid': self.uid, 'ip': 'localhost'})
        assert 'rostelecom_unlim_1' not in [x['name'] for x in services]

    def test_deactivate_blocked_service_successfully(self):
        self.json_ok('user_init', {'uid': self.uid})
        with self.specified_client(scopes=['cloud_api:disk.partners.rostelecom_unlim.manage_services']):
            self.client.request('PUT', 'disk/rostelecom/cloud-platform/activate', query={'uid': self.uid, 'service_key': 'rostelecom_unlim'})
            self.client.request('PUT', 'disk/rostelecom/cloud-platform/off', query={'uid': self.uid, 'service_key': 'rostelecom_unlim'})
            resp = self.client.request('PUT', 'disk/rostelecom/cloud-platform/deactivate', query={'uid': self.uid, 'service_key': 'rostelecom_unlim'})
        assert from_json(resp.content) == {'status': 'inactive'}
        services = self.billing_ok('service_list', {'uid': self.uid, 'ip': 'localhost'})
        assert 'rostelecom_unlim_1' not in [x['name'] for x in services]

    def test_freeze_active_service_successfully(self):
        self.json_ok('user_init', {'uid': self.uid})
        with self.specified_client(scopes=['cloud_api:disk.partners.rostelecom_unlim.manage_services']):
            self.client.request('PUT', 'disk/rostelecom/cloud-platform/activate', query={'uid': self.uid, 'service_key': 'rostelecom_unlim'})
            resp = self.client.request('PUT', 'disk/rostelecom/cloud-platform/off', query={'uid': self.uid, 'service_key': 'rostelecom_unlim'})
        assert from_json(resp.content) == {'status': 'blocked'}
        services = self.billing_ok('service_list', {'uid': self.uid, 'ip': 'localhost'})
        assert 'rostelecom_unlim_1' not in [x['name'] for x in services]

    def test_unfreeze_frozen_service_successfully(self):
        self.json_ok('user_init', {'uid': self.uid})
        with self.specified_client(scopes=['cloud_api:disk.partners.rostelecom_unlim.manage_services']):
            self.client.request('PUT', 'disk/rostelecom/cloud-platform/activate', query={'uid': self.uid, 'service_key': 'rostelecom_unlim'})
            self.client.request('PUT', 'disk/rostelecom/cloud-platform/off', query={'uid': self.uid, 'service_key': 'rostelecom_unlim'})
            resp = self.client.request('PUT', 'disk/rostelecom/cloud-platform/on', query={'uid': self.uid, 'service_key': 'rostelecom_unlim'})
        assert from_json(resp.content) == {'status': 'active'}
        services = self.billing_ok('service_list', {'uid': self.uid, 'ip': 'localhost'})
        assert 'rostelecom_unlim_1' in [x['name'] for x in services]

    @parameterized.expand([
        ('activate',),
        ('deactivate',),
        ('on',),
        ('off',),
    ])
    def test_incorrect_service_key_to_activate(self, handler):
        self.json_ok('user_init', {'uid': self.uid})
        with self.specified_client(scopes=['cloud_api:disk.partners.rostelecom_unlim.manage_services']):
            resp = self.client.request('PUT', 'disk/rostelecom/cloud-platform/%s' % handler, query={'uid': self.uid, 'service_key': 'initial_10gb'})
        assert resp.status_code == 400

    @parameterized.expand([
        ('activate',),
        ('off',),
    ])
    def test_cannot_change_blocked_service_in_cases(self, handler):
        self.json_ok('user_init', {'uid': self.uid})
        with self.specified_client(scopes=['cloud_api:disk.partners.rostelecom_unlim.manage_services']):
            self.client.request('PUT', 'disk/rostelecom/cloud-platform/activate', query={'uid': self.uid, 'service_key': 'rostelecom_unlim'})
            self.client.request('PUT', 'disk/rostelecom/cloud-platform/off', query={'uid': self.uid, 'service_key': 'rostelecom_unlim'})
            resp = self.client.request('PUT', 'disk/rostelecom/cloud-platform/%s' % handler, query={'uid': self.uid, 'service_key': 'rostelecom_unlim'})
        assert resp.status_code == 409
        assert from_json(resp.content)['error'] == RostelecomServiceBlockedError.__name__

    @parameterized.expand([
        ('activate',),
        ('on',),
    ])
    def test_cannot_change_active_service_in_cases(self, handler):
        self.json_ok('user_init', {'uid': self.uid})
        with self.specified_client(scopes=['cloud_api:disk.partners.rostelecom_unlim.manage_services']):
            self.client.request('PUT', 'disk/rostelecom/cloud-platform/activate', query={'uid': self.uid, 'service_key': 'rostelecom_unlim'})
            resp = self.client.request('PUT', 'disk/rostelecom/cloud-platform/%s' % handler, query={'uid': self.uid, 'service_key': 'rostelecom_unlim'})
        assert resp.status_code == 409
        assert from_json(resp.content)['error'] == RostelecomServiceActiveError.__name__

    @parameterized.expand([
        ('deactivate',),
        ('on',),
        ('off',),
    ])
    def test_cannot_change_inactive_service_in_cases(self, handler):
        self.json_ok('user_init', {'uid': self.uid})
        with self.specified_client(scopes=['cloud_api:disk.partners.rostelecom_unlim.manage_services']):
            resp = self.client.request('PUT', 'disk/rostelecom/cloud-platform/%s' % handler, query={'uid': self.uid, 'service_key': 'rostelecom_unlim'})
        assert resp.status_code == 409
        assert from_json(resp.content)['error'] == RostelecomServiceDeactivatedError.__name__

    @parameterized.expand([
        ('deactivate',),
        ('on',),
        ('off',),
    ])
    def test_cannot_change_not_initialized_user_and_does_not_init_him(self, handler):
        with self.specified_client(scopes=['cloud_api:disk.partners.rostelecom_unlim.manage_services']):
            resp = self.client.request('PUT', 'disk/rostelecom/cloud-platform/%s' % handler, query={'uid': self.uid, 'service_key': 'rostelecom_unlim'})
        assert resp.status_code == 403
        assert self.json_ok('user_check', {'uid': self.uid})['need_init'] == '1'

    @parameterized.expand([
        ('inactive', 'inactive', 'inactive', 'inactive', 'inactive', 'inactive', 'inactive', 'inactive',),
        ('active', 'inactive', 'active', 'active', 'inactive', 'inactive', 'inactive', 'inactive',),
        ('blocked', 'inactive', 'blocked', 'blocked', 'inactive', 'inactive', 'inactive', 'inactive',),
        ('active', 'inactive', 'blocked', 'blocked', 'inactive', 'inactive', 'inactive', 'inactive',),
    ])
    def test_service_statuses(self, *statuses):
        self.json_ok('user_init', {'uid': self.uid})
        correct_service_statuses = dict(zip(['rostelecom_unlim', 'rostelecom_vas_1tb', 'rostelecom_vas_100gb', 'rostelecom_vas_5gb',
                                             'rostelecom_unlim_test', 'rostelecom_vas_1tb_test', 'rostelecom_vas_100gb_test', 'rostelecom_vas_5gb_test'],
                                            statuses))
        with self.specified_client(scopes=['cloud_api:disk.partners.rostelecom_unlim.manage_services']):
            for service_key, status in correct_service_statuses.items():
                if status in ('active', 'blocked'):
                    self.client.request('PUT', 'disk/rostelecom/cloud-platform/activate', query={'uid': self.uid, 'service_key': service_key})
                if status == 'blocked':
                    self.client.request('PUT', 'disk/rostelecom/cloud-platform/off', query={'uid': self.uid, 'service_key': service_key})

            resp = self.client.request('GET', 'disk/rostelecom/cloud-platform/status', query={'uid': self.uid})

        assert resp.status_code == 200
        service_statuses = {x['service']['key']: x['status'] for x in from_json(resp.content)['items']}
        assert service_statuses == correct_service_statuses

    def test_if_happend_to_have_several_unlim_services_then_list_active_one(self):
        self.json_ok('user_init', {'uid': self.uid})
        opts = {
            'uid': self.uid,
            'pid': ROSTELECOM_UNLIM_2,
            'line': PRODUCT_LINE,
            'ip': '127.0.0.1',
        }
        self.billing_ok('service_create', opts)
        self.json_ok('rostelecom_freeze', {'uid': self.uid, 'service_key': ROSTELECOM_UNLIM})
        opts = {
            'uid': self.uid,
            'pid': ROSTELECOM_UNLIM_1,
            'line': PRODUCT_LINE,
            'ip': '127.0.0.1',
        }
        self.billing_ok('service_create', opts)

        with self.specified_client(scopes=['cloud_api:disk.partners.rostelecom_unlim.manage_services']):
            resp = self.client.request('GET', 'disk/rostelecom/cloud-platform/status', query={'uid': self.uid})
        assert resp.status_code == 200
        service_statuses = {x['service']['key']: x['status'] for x in from_json(resp.content)['items']}
        assert service_statuses[ROSTELECOM_UNLIM] == 'active'

    def test_cannot_list_services_for_unitialized_user(self):
        with self.specified_client(scopes=['cloud_api:disk.partners.rostelecom_unlim.manage_services']):
            resp = self.client.request('GET', 'disk/rostelecom/cloud-platform/status', query={'uid': self.uid})
        assert resp.status_code == 403

    @parameterized.expand([
        ('GET', 'status', {}),
        ('PUT', 'activate', {'service_key': 'rostelecom_unlim'}),
        ('PUT', 'deactivate', {'service_key': 'rostelecom_unlim'}),
        ('PUT', 'on', {'service_key': 'rostelecom_unlim'}),
        ('PUT', 'off', {'service_key': 'rostelecom_unlim'}),
    ])
    def test_cannot_request_rostelecom_handlers_from_wrong_ips(self, method, endpoint, query):
        query.update({'uid': self.uid})
        with self.specified_client(scopes=['cloud_api:disk.partners.rostelecom_unlim.manage_services']):
            resp = self.client.request(method, 'disk/rostelecom/cloud-platform/%s' % endpoint, query=query, ip='6.6.6.6')
        assert resp.status_code == 403

    def test_2_legged_auth_works_with_uid_in_qs(self):
        self.json_ok('user_init', {'uid': self.uid})
        url = 'disk/rostelecom/cloud-platform/status'
        with self.specified_client(scopes=['cloud_api:disk.partners.rostelecom_unlim.manage_services'], uid=None), \
                PassportStub():
            resp = self.client.get(url, query={'uid': self.uid})
            assert resp.status_code == 200

    def test_activate_service_successfully_for_overdraft_user(self):
        self.json_ok('user_init', {'uid': self.uid})
        used = 100500 * GB
        limit = 1 * GB
        ycrid = 'rest-123321-yay'

        with self.specified_client(scopes=['cloud_api:disk.partners.rostelecom_unlim.manage_services']), \
                PassportStub(), \
                mock.patch('mpfs.core.services.disk_service.MPFSStorageService.used', return_value=used), \
                mock.patch('mpfs.core.services.disk_service.MPFSStorageService.limit', return_value=limit), \
                mock.patch('mpfs.engine.process.get_cloud_req_id', return_value=ycrid), \
                mock.patch('mpfs.frontend.api.FEATURE_TOGGLES_DISABLE_API_FOR_OVERDRAFT_PERCENTAGE', 100):

            resp = self.client.request('PUT', 'disk/rostelecom/cloud-platform/activate',
                                       query={'uid': self.uid, 'service_key': 'rostelecom_unlim'})

        assert resp.status_code == 200
        assert from_json(resp.content) == {'status': 'active'}
        services = self.billing_ok('service_list', {'uid': self.uid, 'ip': 'localhost'})
        assert 'rostelecom_unlim_1' in [x['name'] for x in services]
