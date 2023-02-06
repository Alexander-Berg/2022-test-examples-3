# -*- coding: utf-8 -*-
import itertools
import urlparse

import mock
import os
import urllib
from httplib import OK, UNAUTHORIZED, BAD_REQUEST, NOT_FOUND, FORBIDDEN

import pytest
from nose_parameterized import parameterized

from mpfs.common.util import from_json, to_json
from mpfs.config import settings
from mpfs.core.services.hbf_service import HbfService
from mpfs.core.user_activity_info.dao import UserActivityInfoDAO
from mpfs.core.user_activity_info.utils import ErrorInfoContainer
from mpfs.platform.v1.disk.handlers import GetClientsInstallerWithAutologonHandler
from mpfs.platform.v1.disk.permissions import WebDavPermission
from mpfs.platform.v1.telemost.handlers import GetTelemostClientsInstallerWithAutologonHandler
from test.helpers.stubs.services import PassportStub, HbfServiceStub, DjfsApiMockHelper
from test.parallelly.api.disk.base import DiskApiTestCase
from test.base_suit import UserTestCaseMixin, UploadFileTestCaseMixin
from mpfs.common.static import tags
from test.conftest import INIT_USER_IN_POSTGRES
from test.fixtures.filesystem import file_data


SOFTWARE_INSTALLER_PATH = settings.platform['software_installer']['path']
SOFTWARE_INSTALLER_TEST_BASE_URL = settings.platform['software_installer']['test_builds_base_url']
PLATFORM_DISK_APPS_IDS = settings.platform['disk_apps_ids']
TELEMOST_SOFTWARE_INSTALLER_PATH = settings.platform['software_installer']['telemost_path']
ADSCIM_SOFTWARE_INSTALLER_PATH = settings.platform['software_installer']['adscim_path']


class GetClientConfigTestCase(UserTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.EXTERNAL
    api_version = 'v1'
    method = 'GET'
    url = 'disk/clients/config'

    def setup_method(self, method):
        super(GetClientConfigTestCase, self).setup_method(method)
        self.create_user(self.uid, noemail=True)

    @parameterized.expand([('staff', True),
                           ('', False)])
    def test_get_config(self, expected_prefix, has_staff):
        PassportStub.update_info_by_uid(self.uid, has_staff=has_staff)
        with self.specified_client(scopes=WebDavPermission.scopes):
            response = self.client.request(self.method, self.url)
            config = from_json(response.content)
            assert config['config_db_prefix'] == expected_prefix

    def test_endpoint_hidden_from_external_swagger(self):
        resp = self.client.get('schema/resources/v1/disk/clients/config')
        assert 'clients/config' not in resp.content


class PhotounlimLastModifiedVisibleForInternalSwaggerTestCase(DiskApiTestCase):
    api_mode = tags.platform.INTERNAL
    api_version = 'v1'

    def test_endpoint_visible_for_internal_swagger(self):
        resp = self.client.get('schema/resources/v1/disk/clients/config')
        assert 'clients/config' in resp.content


def get_all_platforms_and_builds():
    for platform, builds in SOFTWARE_INSTALLER_PATH.items():
        for build, path in builds.items():
            for external in (True, False):
                test_case = '%s_%s_%s' % (platform, 'ext' if external else 'int', build)
                yield test_case, platform, external, build


class GetClientInstallerTestCase(UserTestCaseMixin, UploadFileTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.EXTERNAL
    api_version = 'v1'
    method = 'GET'
    url = 'disk/clients/%s/installer'
    installers_path = SOFTWARE_INSTALLER_PATH

    def setup_method(self, method):
        super(GetClientInstallerTestCase, self).setup_method(method)
        HbfService().update_cache()
        import mpfs.core.services.stock_service
        mpfs.core.services.stock_service._setup()
        share_uid = mpfs.core.services.stock_service.SHARE_UID
        self.share_uid = share_uid
        self.create_user(share_uid, add_services='initial_10gb')
        for installer_types in self.installers_path.values():
            for installer_type, path in installer_types.items():
                if installer_type == "beta":
                    continue
                try:
                    self.json_ok('mkdir', {'uid': share_uid, 'path': os.path.dirname(path)})
                except AssertionError:
                    pass
                file_md5, file_sha256, file_size = file_data['meta']['md5'], file_data['meta']['sha256'], file_data['size']
                self.upload_file(share_uid, path, file_data={'md5': file_md5, 'sha256': file_sha256, 'size': file_size})
                self.json_ok(
                    'setprop',
                    {'uid': share_uid, 'path': path, 'version|urn:yandex:disk:dist': installer_type}
                )

    def get_installer_path(self, platform_id, external, build):
        installer_type = 'stable' if external else build
        return self.installers_path[platform_id][installer_type]

    def get_file_info(self, file_path):
        file_info = self.json_ok('info', {'uid': self.share_uid, 'path': file_path, 'meta': 'version|urn:yandex:disk:dist,cdn|urn:yandex:disk:dist:url,sha256,md5,file_url,size'})
        return file_info

    def get_response(self, platform_id, external, build):
        ip = HbfServiceStub.ip_excluded if external else HbfServiceStub.ip_included
        url = self.url % platform_id
        if not external and build != 'stable':
            # Запросы из внутренних сетей для alpha и beta релизов обрабатываем через тестовый мпфс
            url += '?build=%s' % build
            response = self.client.request(self.method, url, ip=ip)
        else:
            # Запросы для stable релизов обрабатываем через прямые походы в djfs-api
            installer_path = self.get_installer_path(platform_id, external, build)
            file_info = self.get_file_info(installer_path)
            with self.specified_client(scopes=[], uid=self.share_uid),\
                 DjfsApiMockHelper.mock_request(status_code=200, content=to_json(file_info)):
                response = self.client.request(self.method, url, ip=ip)
        return response

    @parameterized.expand(list(get_all_platforms_and_builds()))
    def test_get_installer_base_url_and_path(self, _, platform_id, external, build):
        if platform_id not in self.installers_path:
            return  # не должно запускаться с платформами для которых нет клиента
        if not external and build != 'stable':
            # Запросы из внутренних сетей для alpha и beta релизов обрабатываем через тестовый мпфс
            base_url = SOFTWARE_INSTALLER_TEST_BASE_URL
            with mock.patch('mpfs.platform.handlers.ServiceProxyHandler.request_service') as request_stub:
                self.get_response(platform_id, external, build)
                assert request_stub.called_once()
                request_url = request_stub.call_args[0][0]
                assert request_url.startswith(base_url)
                installer_path = self.get_installer_path(platform_id, external, build)
                assert urllib.quote_plus(installer_path) in request_url
        else:
            # Запросы для stable релизов обрабатываем через прямые походы в djfs-api
            with mock.patch('requests.Request.__init__') as mocked_request:
                base_url = settings.services['DjfsApiService']['base_url']
                self.get_response(platform_id, external, build)
                assert mocked_request.called_once()
                request_url = mocked_request.call_args[0][1]
                assert request_url.startswith(base_url)
                requested_installer_path = mocked_request.call_args[1]['params']['path']
                installer_path = self.get_installer_path(platform_id, external, build)
                assert requested_installer_path == installer_path

    @parameterized.expand(list(get_all_platforms_and_builds()))
    def test_get_installer(self, _, platform_id, external, build):
        if platform_id not in self.installers_path:
            return  # не должно запускаться с платформами для которых нет клиента
        response = self.get_response(platform_id, external, build)
        config = from_json(response.content)
        if not external and build == 'alpha':
            # beta тут мапится в тот же файл, поэтому не проверить. Проверяется в test_get_installer_base_url_and_path
            installer_type = build
        else:
            installer_type = 'stable'
        assert file_data['meta']['sha256'] == config['sha256']
        assert file_data['meta']['md5'] == config['md5']
        assert file_data['size'] == config['size']
        installer_path = self.get_installer_path(platform_id, external, build)
        assert installer_path.split('/')[-1] in config['file']
        assert installer_type == config['version']

    @parameterized.expand([(True,), (False,)])
    def test_get_installer_bad_platform(self, external):
        platform = 'plan9'
        build = 'stable'
        ip = HbfServiceStub.ip_excluded if external else HbfServiceStub.ip_included
        url = self.url % platform
        url += '?build=%s' % build
        response = self.client.request(self.method, url, ip=ip)
        assert response.status_code == BAD_REQUEST
        answer = from_json(response.content)
        assert 'FieldValidationError' == answer['error']

    def test_get_installer_bad_type_internal(self):
        response = self.get_response('win64', False, 'super_stable_fake_update')
        assert response.status_code == BAD_REQUEST
        answer = from_json(response.content)
        assert 'FieldValidationError' == answer['error']

    @parameterized.expand(SOFTWARE_INSTALLER_PATH.keys())
    def test_endpoint_hidden_from_external_swagger(self, platform_id):
        if platform_id not in self.installers_path:
            return  # не должно запускаться с платформами для которых нет клиента
        resp = self.client.get('schema/resources/v1/disk/clients/%s/installer' % platform_id)
        assert '/installer' not in resp.content

    def test_send_shared_uid_to_mpfs_doing_request_without_user(self):
        with DjfsApiMockHelper.mock_request(status_code=200),\
             mock.patch('requests.Request.__init__') as mocked_request:
                self.client.get('disk/clients/win64/installer?build=stable')
                assert mocked_request.call_args[1]['params']['uid'] == '0'

    def test_send_shared_uid_to_mpfs_doing_request_with_user(self):
        with self.specified_client(scopes=[], uid=self.uid, login=self.login),\
             mock.patch('requests.Request.__init__') as mocked_request:
            self.client.get('disk/clients/win64/installer?build=stable')
            assert mocked_request.call_args[1]['params']['uid'] == '0'

    @parameterized.expand(list(get_all_platforms_and_builds()))
    def test_get_installer_with_cdn_meta(self, _, platform_id, external, build):
        if platform_id not in self.installers_path:
            return  # не должно запускаться с платформами для которых нет клиента
        cdn_url = 'https://download.cdn.yandex.net/disk/yandex/stable/289ea52b3355f99b8484a757a8768d8a/YandexDisk30Setup_x64.exe'
        path = self.get_installer_path('win64', True, 'stable')
        if build == 'stable' and external and platform_id == 'win64':
            self.json_ok(
                'setprop',
                {'uid': self.share_uid, 'path': path, 'cdn|urn:yandex:disk:dist:url': cdn_url}
            )
        response = self.get_response(platform_id, external, build)
        config = from_json(response.content)
        if not external and build == 'alpha':
            installer_type = build
        else:
            installer_type = 'stable'
        assert file_data['meta']['sha256'] == config['sha256']
        assert file_data['meta']['md5'] == config['md5']
        assert file_data['size'] == config['size']
        assert installer_type == config['version']
        if build == 'stable' and external and platform_id == 'win64':
            assert config['file'] == cdn_url
        else:
            installer_path = self.get_installer_path(platform_id, external, build)
            assert installer_path.split('/')[-1] in config['file']


class GetTelemostClientInstallerTestCase(GetClientInstallerTestCase):
    url = 'telemost/clients/%s/installer'
    installers_path = TELEMOST_SOFTWARE_INSTALLER_PATH


class GetTelemostClientInstallerInternalTestCase(UploadFileTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.INTERNAL
    api_version = 'v1'
    method = 'GET'
    url = 'telemost/clients/%s/installer?build=%s'
    installers_path = SOFTWARE_INSTALLER_PATH

    def setup_method(self, method):
        super(GetTelemostClientInstallerInternalTestCase, self).setup_method(method)
        HbfService().update_cache()
        import mpfs.core.services.stock_service
        mpfs.core.services.stock_service._setup()

    def get_response(self, platform_id, build):
        url = self.url % (platform_id, build)
        return self.client.request(self.method, url, ip=HbfServiceStub.ip_included)

    def test_get_installer_without_user(self):
        with self.specified_client(uid=None):
            response = self.get_response('win64', 'beta')
            assert response.status_code == OK


class GetADSCIMClientInstallerTestCase(GetClientInstallerTestCase):
    url = 'adscim/%s/installer'
    installers_path = ADSCIM_SOFTWARE_INSTALLER_PATH


class GetClientInstallerWithAutologonTestCase(UserTestCaseMixin, UploadFileTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.EXTERNAL
    api_version = 'v1'
    method = 'GET'
    url = 'disk/clients/win/installer'
    installer_paths = GetClientsInstallerWithAutologonHandler.installer_paths

    def setup_method(self, method):
        super(GetClientInstallerWithAutologonTestCase, self).setup_method(method)
        import mpfs.core.services.stock_service
        mpfs.core.services.stock_service._setup()
        share_uid = mpfs.core.services.stock_service.SHARE_UID
        self.share_uid = share_uid
        self.create_user(share_uid, add_services='initial_10gb')
        for installer_type, path in self.installer_paths.items():
            try:
                self.json_ok('mkdir', {'uid': share_uid, 'path': os.path.dirname(path)})
            except AssertionError:
                # скипаем если уже есть папка
                pass
            self.upload_file(share_uid, path)
            self.json_ok('setprop', {'uid': share_uid,
                                     'path': path,
                                     'auto_login': 1})

    def test_without_auth(self):
        with self.specified_client(uid=None):
            response = self.client.request(self.method, self.url)
            assert response.status_code == OK
            actual_result = from_json(response.content)
            actual_query_params = urlparse.parse_qs(urlparse.urlparse(actual_result['file']).query)
            assert 'al' not in actual_query_params

    def test_with_auth(self):
        with self.specified_client(uid=self.uid):
            response = self.client.request(self.method, self.url)
            assert response.status_code == OK
            actual_result = from_json(response.content)
            actual_query_params = urlparse.parse_qs(urlparse.urlparse(actual_result['file']).query)
            assert actual_query_params['al'][0] == '1'


class GetTelemostClientInstallerWithAutologonTestCase(GetClientInstallerWithAutologonTestCase):
    url = 'telemost/clients/win/installer'
    installer_paths = GetTelemostClientsInstallerWithAutologonHandler.installer_paths


class GetClientFeaturesTestCase(UserTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.EXTERNAL
    api_version = 'v1'
    method = 'GET'
    url = 'disk/clients/features'
    feature_list = (
        'advertising',
        'antifo',
        'disk_pro',
        'online_editor',
        'priority_support',
        'versioning_extended_period',
        'desktop_folder_autosave',
        'unlimited_video_autouploading',
        'unlimited_photo_autouploading',
        'promote_mail360',
        'public_settings'
    )

    def setup_method(self, method):
        super(GetClientFeaturesTestCase, self).setup_method(method)
        self.create_user(self.uid, noemail=True)

    @parameterized.expand(PLATFORM_DISK_APPS_IDS)
    def test_get_features(self, client_id):
        with self.specified_client(uid=self.uid, login=self.login, scopes=['yadisk:all'], id=client_id):
            response = self.client.request(self.method, self.url)
            self._check_ok_response(response)

    def test_not_auto_init_user(self):
        client_id = PLATFORM_DISK_APPS_IDS[0]
        with self.specified_client(uid='1111111111', login=self.login, scopes=['yadisk:all'], id=client_id):
            response = self.client.request(self.method, self.url)
            self._check_ok_response(response)

    def test_get_features_unauth(self):
        response = self.client.request(self.method, self.url)
        assert response.status_code == UNAUTHORIZED

    def test_endpoint_hidden_from_external_swagger(self):
        resp = self.client.get('schema/resources/v1/disk/clients/features')
        assert 'clients/features' not in resp.content

    def _check_ok_response(self, response):
        assert response.status_code == OK
        config = from_json(response.content)
        for feature in self.feature_list:
            assert feature in config
            assert 'enabled' in config[feature]
            assert isinstance(config[feature]['enabled'], bool)


class GetClientActivityTestCase(UserTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.EXTERNAL
    api_version = 'v1'
    method = 'GET'
    url = 'disk/clients/activity'
    dao = UserActivityInfoDAO()

    def setup_method(self, method):
        super(GetClientActivityTestCase, self).setup_method(method)
        self.create_user(self.uid, noemail=True)
        list(self.dao.bulk_update_activity_dates_and_fetch_closest_activity_dates([
            {'uid': self.uid,
             'platform_type': 'web',
             'first_activity': '2019-01-01',
             'last_activity': '2019-01-01'},
            {'uid': self.uid,
             'platform_type': 'ios',
             'first_activity': '2019-01-01',
             'last_activity': '2019-01-01'},
            {'uid': self.uid,
             'platform_type': 'android',
             'first_activity': '2019-01-01',
             'last_activity': '2019-01-01'},
            {'uid': self.uid,
             'platform_type': 'windows',
             'first_activity': '2019-01-01',
             'last_activity': '2019-01-01'},
            {'uid': self.uid,
             'platform_type': 'mac',
             'first_activity': '2019-01-01',
             'last_activity': '2019-01-01'},
            {'uid': self.uid,
             'platform_type': 'search_app',
             'first_activity': '2019-01-01',
             'last_activity': '2019-01-01'},
        ], ErrorInfoContainer()))

    def test_get_activity(self):
        with self.specified_client(uid=self.uid, login=self.login, scopes=['yadisk:all']):
            response = self.client.request(self.method, self.url)
            assert response.status_code == OK
            activity_info = from_json(response.content)
            if INIT_USER_IN_POSTGRES:
                assert activity_info == {
                    'web': {'first_activity': '2019-01-01', 'last_activity': '2019-01-01'},
                    'mac': {'first_activity': '2019-01-01', 'last_activity': '2019-01-01'},
                    'ios': {'first_activity': '2019-01-01', 'last_activity': '2019-01-01'},
                    'search_app': {'first_activity': '2019-01-01', 'last_activity': '2019-01-01'},
                    'android': {'first_activity': '2019-01-01', 'last_activity': '2019-01-01'},
                    'windows': {'first_activity': '2019-01-01', 'last_activity': '2019-01-01'},
                }
            else:
                assert activity_info == {}

    def test_not_auto_init_user(self):
        client_id = PLATFORM_DISK_APPS_IDS[0]
        with self.specified_client(uid='1111111111', login=self.login, scopes=['yadisk:all'], id=client_id):
            response = self.client.request(self.method, self.url)
            assert response.status_code == NOT_FOUND
            message = from_json(response.content)
            assert message['error'] == 'DiskUserNotFoundError'

    def test_get_activity_with_wrong_scope(self):
        with self.specified_client(uid=self.uid, login=self.login, scopes=['yadisk:read']):
            response = self.client.request(self.method, self.url)
            assert response.status_code == FORBIDDEN

    def test_get_activity_unauth(self):
        response = self.client.request(self.method, self.url)
        assert response.status_code == UNAUTHORIZED

    def test_endpoint_hidden_from_external_swagger(self):
        resp = self.client.get('schema/resources/v1/disk/clients/activity')
        assert 'clients/activity' not in resp.content
