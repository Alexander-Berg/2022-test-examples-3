# -*- coding: utf-8 -*-
import time

import pytest
from nose_parameterized import parameterized

from base_suit import UserTestCaseMixin
from test.parallelly.api.disk.base import DiskApiTestCase
from test.parallelly.promo_codes import (
    PromoCodeMethodsMixin,
    mock_promo_code_product_line,
)

from mpfs.common.static import tags
from mpfs.common.util import to_json, from_json


class PromoCodePlatformTestCase(PromoCodeMethodsMixin, UserTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.INTERNAL
    api_version = 'v1'

    def setup_method(self, method):
        super(PromoCodePlatformTestCase, self).setup_method(method)
        self.create_user(self.uid, noemail=1)

    @pytest.mark.skipif(reason='There\'s no need in implementing promo codes im platform, that\'s why skipped')
    def test_activate_promo_code_successfully(self):
        promo_code = self._generate_promo_code()
        data = {'promo_code': promo_code}
        with self.specified_client(scopes=['cloud_api:disk.promo_codes.write']), \
                mock_promo_code_product_line():
            resp = self.client.request('PUT', 'disk/promo-codes/activate', data=to_json(data))
        assert resp.status_code == 200

    @pytest.mark.skipif(reason='There\'s no need in implementing promo codes im platform, that\'s why skipped')
    def test_failed_activation_missing_promo_code(self):
        data = {'promo_code': 'AAA'}
        with self.specified_client(scopes=['cloud_api:disk.promo_codes.write']), \
                mock_promo_code_product_line():
            resp = self.client.request('PUT', 'disk/promo-codes/activate', data=to_json(data))
        assert resp.status_code == 404
        assert from_json(resp.content)['error'] == 'DiskPromoCodeNotFoundError'

    def test_failed_activation_used_promo_code(self):
        self.create_user(self.user_1['uid'], noemail=1)
        promo_code = self._generate_promo_code()
        data = {'promo_code': promo_code}
        with self.specified_client(scopes=['cloud_api:disk.promo_codes.write']), \
                mock_promo_code_product_line():
            self.client.request('PUT', 'disk/promo-codes/activate', data=to_json(data), uid=self.user_1['uid'])
            data = {'promo_code': promo_code}
            resp = self.client.request('PUT', 'disk/promo-codes/activate', data=to_json(data))
        assert resp.status_code == 409
        assert from_json(resp.content)['error'] == 'DiskAttemptToActivateUsedPromoCodeError'

    def test_failed_activation_already_have_such_service(self):
        promo_code_1 = self._generate_promo_code()
        promo_code_2 = self._generate_promo_code()
        with self.specified_client(scopes=['cloud_api:disk.promo_codes.write']), \
                mock_promo_code_product_line():
            data = {'promo_code': promo_code_1}
            self.client.request('PUT', 'disk/promo-codes/activate', data=to_json(data))
            data = {'promo_code': promo_code_2}
            resp = self.client.request('PUT', 'disk/promo-codes/activate', data=to_json(data))
        assert resp.status_code == 409
        assert from_json(resp.content)['error'] == 'DiskUserAlreadyHasSuchPromoServiceError'

    @parameterized.expand([
        ('too_early_activation',),
        ('too_late_activation',),
    ])
    @pytest.mark.skipif(reason='There\'s no need in implementing promo codes im platform, that\'s why skipped')
    def test_failed_because_wrong_time(self, case_name):
        if case_name == 'too_late_activation':
            data = {'promo_code': self._generate_promo_code(end_datetime=int(time.time()) - 50)}
        else:
            data = {'promo_code': self._generate_promo_code(begin_datetime=int(time.time()) + 50)}
        with self.specified_client(scopes=['cloud_api:disk.promo_codes.write']), \
                mock_promo_code_product_line():
            resp = self.client.request('PUT', 'disk/promo-codes/activate', data=to_json(data))
        assert resp.status_code == 404
        assert from_json(resp.content)['error'] == 'DiskPromoNotActiveForThisCodeError'

    @pytest.mark.skipif(reason='There\'s no need in implementing promo codes im platform, that\'s why skipped')
    def test_ratelimiter_working_for_promo_code_activating(self):
        data = {'promo_code': 'AAA'}
        with self.specified_client(scopes=['cloud_api:disk.promo_codes.write']), \
                mock_promo_code_product_line():
            for i in range(10):
                self.client.request('PUT', 'disk/promo-codes/activate', data=to_json(data))
        # 11ая попытка приводит к ошибке по частоте запросов
            resp = self.client.request('PUT', 'disk/promo-codes/activate', data=to_json(data))
        assert resp.status_code == 429
        assert from_json(resp.content)['error'] == 'TooManyRequestsError'
