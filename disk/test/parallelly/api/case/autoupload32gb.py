# -*- coding: utf-8 -*-
import mock

from nose_parameterized import parameterized

from test.fixtures.users import default_user
from test.parallelly.billing.base import BaseBillingTestCase
from test.parallelly.api.disk.base import DiskApiTestCase
from mpfs.common.static import tags
from mpfs.common.util import (
    ctimestamp,
    from_json,
)

TOO_EARLY_TIMESTAMP = 1491166799  # 2 апреля 2017 г., 23:59:59
START_TIMESTAMP = 1491166800  # 3 апреля 2017 г., 00:00:00
END_TIMESTAMP = 1499126399  # 3 июля 2017 г., 23:59:59
TOO_LATE_TIMESTAMP = 1499126400  # 4 июля 2017 г., 00:00:00


class AutouploadBonusTestCase(DiskApiTestCase, BaseBillingTestCase):

    api_mode = tags.platform.INTERNAL
    api_version = 'v1'
    uid = default_user.uid

    def setup_method(self, method):
        DiskApiTestCase.setup_method(self, method)
        BaseBillingTestCase.setup_method(self, method)

        from mpfs.core.billing.processing import marketing
        marketing.line_export('bonus', ctimestamp(), False)

    def test_grant_bonus(self):
        """Проверяем, что можно выдать бонус 32-gb-autoupload и только единожды. Проверяем ручки activate и status"""
        with mock.patch('mpfs.platform.v1.case.handlers.StatusAutouploadBonusHandler._get_current_time',
                        return_value=START_TIMESTAMP):
            resp = self.client.request('GET', 'case/disk/32-gb-autoupload/status', uid=self.uid)
            assert resp.status_code == 200
            resp = from_json(resp.content)
            assert resp['show_notification']

            opts = {'uid': self.uid, 'line': 'bonus', 'pid': '32_gb_autoupload', 'ip': '127.0.0.1'}
            self.billing_ok('service_create', opts)

            resp = self.client.request('GET', 'case/disk/32-gb-autoupload/status', uid=self.uid)
            assert resp.status_code == 200
            resp = from_json(resp.content)
            assert not resp['show_notification']

    @parameterized.expand([
        ('too_early', TOO_EARLY_TIMESTAMP, 404),
        ('too_late', TOO_LATE_TIMESTAMP, 404),
        ('first_day', START_TIMESTAMP, 200),
        ('last_day', END_TIMESTAMP, 200),
    ])
    def test_valid_dates(self, case_name, mock_date, code):
        u"""Проверяем, что в зависимости от различных дат, услугу можно выдать или нет.
        Также проверяем, какой статус по этой услуге возвращает ручка status"""
        with mock.patch('mpfs.platform.v1.case.handlers.StatusAutouploadBonusHandler._get_current_time',
                        return_value=mock_date):
            resp = self.client.request('GET', 'case/disk/32-gb-autoupload/status', uid=self.uid)
            assert resp.status_code == code

    @parameterized.expand([
        ('no_login', None, 404),
        ('not_test_login', 'common.login', 404),
        ('test_login', 'promo-auto-upload-32-gb-user-a1', 200),
        ('test_login', 'promo.auto.upload.32.gb.user.a1', 200),
    ])
    def test_test_login(self, case_name, login, code):
        with mock.patch('mpfs.platform.v1.case.handlers.StatusAutouploadBonusHandler._get_current_time',
                        return_value=TOO_EARLY_TIMESTAMP):
            with self.specified_client(scopes=['cloud_api:disk.read'], login=login):
                resp = self.client.request('GET', 'case/disk/32-gb-autoupload/status')
                assert resp.status_code == code
