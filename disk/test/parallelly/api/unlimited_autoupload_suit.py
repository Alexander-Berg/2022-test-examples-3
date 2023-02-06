# coding=utf-8
import json

import mock
from nose_parameterized import parameterized

from mpfs.common.errors.platform import MpfsProxyBadResponse

from mpfs.common.static import tags
from mpfs.common.util import from_json
from mpfs.config import settings
import mpfs.platform.v1.disk.handlers
from test.parallelly.api.disk.base import DiskApiTestCase
from test.base_suit import UserTestCaseMixin
PLATFORM_MOBILE_APPS_IDS = settings.platform['mobile_apps_ids']


class UnlimitTestCase(UserTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.EXTERNAL
    api_version = 'v1'

    def test_unlimited_sets(self):
        self.create_user(self.uid)
        with self.specified_client(id=PLATFORM_MOBILE_APPS_IDS[0], scopes=['yadisk:all'], uid=self.uid), \
             mock.patch('mpfs.platform.v1.case.handlers.UnlimitedAutouploadsSetStatusHandler.request_service') as request_service:
            res = self.client.put('case/disk/unlimited-autouploads/set-state', uid=self.uid,
                                   data={'unlimited_video_autoupload_enabled': True})

            assert 'json/set_unlimited_autouploading' in request_service.call_args[0][0]
            assert 'unlimited_video_autoupload_enabled=1' in request_service.call_args[0][0]
            assert 'unlimited_video_autoupload_reason=by_user' in request_service.call_args[0][0]
            assert 'unlimited_photo_autoupload_enabled' not in request_service.call_args[0][0]

        with self.specified_client(id=PLATFORM_MOBILE_APPS_IDS[0], scopes=['yadisk:all'], uid=self.uid), \
             mock.patch(
                 'mpfs.platform.v1.case.handlers.UnlimitedAutouploadsSetStatusHandler.request_service') as request_service:
            self.client.put('case/disk/unlimited-autouploads/set-state', uid=self.uid,
                                   data={'unlimited_photo_autoupload_enabled': True})
            assert 'unlimited_video_autoupload_enabled' not in request_service.call_args[0][0]
            assert 'unlimited_video_autoupload_reason' not in request_service.call_args[0][0]
            assert 'unlimited_photo_autoupload_enabled=1' in request_service.call_args[0][0]

        return_value = {'unlimited_video_autoupload_enabled': True, 'unlimited_photo_autoupload_enabled': False,
                        'unlimited_video_autoupload_reason': 'by_user'}

        with self.specified_client(id=PLATFORM_MOBILE_APPS_IDS[0], scopes=['yadisk:all'], uid=self.uid), \
             mock.patch(
                 'mpfs.platform.v1.case.handlers.UnlimitedAutouploadsSetStatusHandler.request_service', return_value=return_value) as request_service:
            resp =self.client.put('case/disk/unlimited-autouploads/set-state', uid=self.uid,
                                   data={'unlimited_video_autoupload_enabled': True, 'unlimited_photo_autoupload_enabled': False})
            assert json.loads(resp.result) == return_value
            assert 'unlimited_video_autoupload_enabled=1' in request_service.call_args[0][0]
            assert 'unlimited_video_autoupload_reason=by_user' in request_service.call_args[0][0]
            assert 'unlimited_photo_autoupload_enabled=0' in request_service.call_args[0][0]

        return_value = {'is_paid': 1, 'revision': '0', 'unlimited_autoupload_enabled': 1,
                        'unlimited_video_autoupload_enabled': 1, 'unlimited_photo_autoupload_enabled': 1,
                        'unlimited_video_autoupload_reason': 'by_user'}
        orig_request_service = mpfs.platform.v1.disk.handlers.GetDiskHandler.request_service

        def mock_request_service(self_, url, *args, **kwargs):
            context = self_.get_context()
            user_info_url = self_.build_url(self_.user_info_url, context)
            if url == user_info_url:
                return return_value
            else:
                return orig_request_service(self_, url, *args, **kwargs)

        mpfs.platform.v1.disk.handlers.GetDiskHandler.request_service = mock_request_service
        with self.specified_client(id=PLATFORM_MOBILE_APPS_IDS[0], scopes=['yadisk:all'], uid=self.uid):
            resp = self.client.get('disk', uid=self.uid)
            result = json.loads(resp.result)
            assert result['unlimited_video_autoupload_enabled']
            assert result['unlimited_photo_autoupload_enabled']
            assert result['unlimited_video_autoupload_reason'] == 'by_user'

        with self.specified_client(scopes=['yadisk:all'], uid=self.uid):
            resp = self.client.get('disk', uid=self.uid)
            result = json.loads(resp.result)
            assert 'unlimited_video_autoupload_enabled' not in result
            assert 'unlimited_photo_autoupload_enabled' not in result
            assert 'unlimited_video_autoupload_reason' not in result
        mpfs.platform.v1.disk.handlers.GetDiskHandler.request_service = orig_request_service

        with self.specified_client(scopes=['yadisk:all'], uid=self.uid):
            resp = self.client.get('disk/experiments', uid=self.uid)
            assert resp.status_code == 200

    @parameterized.expand([
        ('tr', ),
        ('ru', ),
    ])
    def test_experiments_wihtout_mpfs(self, country):
        # пользователь не инициализирован паспорт работает
        from mpfs.core.services.mpfsproxy_service import mpfsproxy
        from mpfs.core.services.passport_service import passport
        from mpfs.core.services.uaas_service import new_uaas
        rv = [{
            'CONTEXT': {'DISK': {'flags': ['disk_forbidden_video_unlim'], 'testid': ['190458']}},
            'HANDLER': 'DISK'
        }]
        experiments_mock = mock.Mock(return_value=rv)
        user_info_mock = mock.Mock(return_value={'country': country})
        with self.specified_client(scopes=['yadisk:all'], uid=self.uid), \
             mock.patch.object(new_uaas, 'get_disk_experiments', experiments_mock), \
             mock.patch.object(passport, 'userinfo', user_info_mock):
            resp = self.client.get('disk/experiments', uid=self.uid)
            content = from_json(resp.content)
            assert content['uas_exp_flags'] == rv
