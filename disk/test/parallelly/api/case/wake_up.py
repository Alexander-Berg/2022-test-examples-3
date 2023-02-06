# -*- coding: utf-8 -*-
import random
from test.parallelly.api.disk.base import DiskApiTestCase
from test.base_suit import UserTestCaseMixin
from mpfs.config import settings
from mpfs.common.static import tags
from mpfs.common.util import from_json
from nose_parameterized import parameterized

PLATFORM_DISK_APPS_IDS = settings.platform['disk_apps_ids']


class WakeUpTestCase(UserTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.EXTERNAL
    api_version = 'v1'
    start_url = 'case/disk/ios_wake_up/autoupload'
    stop_url = 'case/disk/ios_wake_up/autoupload/%s'
    headers = {'User-Agent': 'Yandex.Disk {"os":"iOS",'
                             '"src":"disk.mobile",'
                             '"vsn":"2.30.10016",'
                             '"id":"DFBDE026-32ED-4S5A-9H9F-D1R497E64006",'
                             '"device":"tablet"}'}

    def setup_method(self, method):
        super(WakeUpTestCase, self).setup_method(method)
        self.create_user(self.uid, noemail=True)

    def test_wake_up_push_start(self):
        with self.specified_client(uid=self.uid, login=self.login, scopes=['yadisk:all'],
                                   id=random.choice(PLATFORM_DISK_APPS_IDS)):
            response = self.client.request('PUT', self.start_url, headers=self.headers)
            json_resp = from_json(response.content)
            assert 'session_id' in json_resp

    def test_wake_up_push_start_with_missing_xiva_subscription(self):
        with self.specified_client(uid=self.uid, login=self.login, scopes=['yadisk:all'],
                                   id=random.choice(PLATFORM_DISK_APPS_IDS)):
            headers = {'User-Agent': 'Yandex.Disk {"os":"iOS",''"src":"disk.mobile",'
                                     '"vsn":"2.30.10016",'
                                     '"id":"DFBDE026-32ED-4S5A-9H9F-XXXXXXXXXXXX",'
                                     '"device":"tablet"}'}
            response = self.client.request('PUT', self.start_url, headers=headers)
            assert response.status_code == 404

    def test_wake_up_push_stop(self):
        with self.specified_client(uid=self.uid, login=self.login, scopes=['yadisk:all'],
                                   id=random.choice(PLATFORM_DISK_APPS_IDS)):
            response = self.client.request('PUT', self.start_url, headers=self.headers)
            json_resp = from_json(response.content)
            session_id = json_resp['session_id']
            response = self.client.request('DELETE', self.stop_url % session_id)
            assert response.status_code == 204

    @parameterized.expand([('wakeUpPushSessionId', 400),
                           ('wakeUpPush:SessionId', 400),
                           ('123:1234', 400),
                           ('123:1234:1234', 400),
                           (':89519942f316497b2e1453f5237644b1f1c5731fb2fd669d1b08c61e8f97c041', 404),
                           ('128280859:', 404),
                           ])
    def test_wake_up_push_stop_with_invalid_session_id(self, session_id, resp_code):
        with self.specified_client(uid=self.uid, login=self.login, scopes=['yadisk:all'],
                                   id=random.choice(PLATFORM_DISK_APPS_IDS)):
            response = self.client.request('DELETE', self.stop_url % session_id)
            assert response.status_code == resp_code
            print repr(response.result)

    def test_wake_up_push_stop_with_missing_operation(self):
        with self.specified_client(uid=self.uid, login=self.login, scopes=['yadisk:all'],
                                   id=random.choice(PLATFORM_DISK_APPS_IDS)):
            session_id = '%s:%s' % (self.uid, '89519942f316497b2e1453f5237644b1f1c5731fb2fd669d1b08c61e8f97c041')
            response = self.client.request('DELETE', self.stop_url % session_id)
            assert response.status_code == 404
