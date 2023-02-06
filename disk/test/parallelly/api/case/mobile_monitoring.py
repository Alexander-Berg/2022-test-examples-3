# -*- coding: utf-8 -*-
import random

from nose_parameterized import parameterized
from test.parallelly.api.disk.base import DiskApiTestCase
from test.base_suit import UserTestCaseMixin
from mpfs.config import settings
from mpfs.common.static import tags

PLATFORM_DISK_APPS_IDS = settings.platform['disk_apps_ids']


class GetMobileMonitoringTestCase(UserTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.EXTERNAL
    api_version = 'v1'
    mobile_url = 'case/disk/clients/mobile/monitoring'
    desktop_url = 'case/disk/clients/desktop/monitoring'

    @parameterized.expand([
        ('desktop', desktop_url),
        ('mobile', mobile_url),
    ])
    def test_monitoring(self, _, url):
        with self.specified_client(uid=self.uid, login=self.login, scopes=['yadisk:all'],
                                   id=random.choice(PLATFORM_DISK_APPS_IDS)):
            response = self.client.request('GET', url)
            assert response.status_code == 200
            assert response.content == ''
