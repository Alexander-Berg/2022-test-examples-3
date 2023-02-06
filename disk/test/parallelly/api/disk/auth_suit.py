# -*- coding: utf-8 -*-
import urllib2

import mock
import os
import urlparse
import random
import pytest

from attrdict import AttrDict
from contextlib import contextmanager

from copy import copy, deepcopy
from hamcrest import assert_that, equal_to, is_, all_of, contains_string
from nose_parameterized import parameterized
from httplib import NOT_FOUND, OK, UNAUTHORIZED

from mpfs.common.errors import PassportBadResult
from mpfs.common.errors import PassportNoResponse
from mpfs.core.zookeeper.hooks import SettingsChangeEvent
from mpfs.platform.v1.case.handlers import ImportYTTableHandler
from test.parallelly.api.disk.base import DiskApiTestCase
from test.parallelly.api.personality import DataApiProfileTest
from test.base_suit import UserTestCaseMixin, UploadFileTestCaseMixin
from test.parallelly.api.base import ExternalPlatformTestClient, InternalPlatformTestClient
from test.parallelly.tvm_2_0_suit import TestTVM2Base, TVM2NetworksMocksMixin, SERVICES_TVM_2_0_CLIENT_ID
from test.fixtures import services
from mpfs.common.static.tags import PLATFORM_NETWORK_AUTHORIZATION_SILENT_MODE_LOG_PREFIX
from mpfs.common.static.tags.conf_sections import YATEAM_TVM, TVM_CLIENT_IDS
from mpfs.core.event_dispatcher.dispatcher import EventDispatcher
from mpfs.common.static import tags
from mpfs.common.util import from_json, to_json
from mpfs.config import settings
from mpfs.core.services.conductor_service import ConductorService
from mpfs.core.services.tvm_service import tvm
from mpfs.engine import process
from mpfs.platform import handlers
from mpfs.platform.auth import (
    ClientNetworks,
    ExternalTokenAuth,
    InternalAuth,
    InternalTokenAuth,
    InternalConductorAuth,
    OAuthAuth,
    PassportCookieAuth,
    TVMAuth,
    TVM2Auth,
)
from mpfs.platform.handlers import BasePlatformHandler
from mpfs.platform.rate_limiters import PerClientIdRateLimiter
from mpfs.platform.v1.disk.handlers import MpfsProxyHandler
from mpfs.platform.v1.data.handlers import DataApiProxyHandler
from mpfs.platform.v1.batch.handlers import BatchRequestHandler
from mpfs.platform.events import dispatcher

PLATFORM_NOTES_APP_ID = settings.platform['notes_app_id']


class Response(object):
    def __init__(self, content):
        self.content = content


class DiskCookieAuthTestCase(UserTestCaseMixin, DiskApiTestCase):
    """
    Тестируем авторизацию по паспортным кукам
    """
    api_mode = tags.platform.EXTERNAL
    api_version = 'v1'

    def setup_method(self, method):
        super(DiskCookieAuthTestCase, self).setup_method(method)
        self.create_user(self.uid, noemail=1)
        self.login = str(self.uid)

    def test_cookie_auth(self):
        base_headers = {
            'Cookie': 'Other=123; Session_id=vse_ravno_mock-aem;',
            'Origin': 'https://yandex.ru',
            'Host': '127.0.0.1'
        }

        with mock.patch.object(PassportCookieAuth, 'validate_session_cookie') as mock_method:
            mock_method.return_value = {'uid': self.uid, 'login': self.login}

            # все хорошо, авторизуем по куке
            resp = self.client.request('GET', 'disk', headers=base_headers)
            assert resp.status_code == 200

            # в биллинг доступа не даем
            url = 'disk/partners/%s/services' % 'rostelecom'
            resp = self.client.post(url, query={'product_id': 'rostelecom_2014_100gb'}, headers=base_headers)
            assert resp.status_code == 403

        # с неправильной кукой не пускаем
        resp = self.client.request('GET', 'disk', headers=base_headers)
        assert resp.status_code == 401


def mock_auth(mock_handler_cls):
    def mock_auth_inner(fn):
        def wrapped(self, *args, **kwargs):
            with mock.patch.object(PassportCookieAuth, 'validate_session_cookie') as mock_method:
                mock_method.return_value = {'uid': self.uid, 'login': self.login}
                with mock.patch.dict(settings.platform, self.fake_settings):
                    with mock.patch.object(mock_handler_cls, 'auth_methods', [PassportCookieAuth()]):
                        return fn(self, *args, **kwargs)
        return wrapped
    return mock_auth_inner


mock_disk_auth = mock_auth(MpfsProxyHandler)
mock_data_auth = mock_auth(DataApiProxyHandler)


def mock_batch_auth(mock_handler_cls):
    def fake_check_permission(*args, **kwargs):
        pass

    def mock_auth_inner(fn):
        def wrapped(self, *args, **kwargs):
            with mock.patch.object(PassportCookieAuth, 'validate_session_cookie') as mock_method:
                mock_method.return_value = {'uid': self.uid, 'login': self.login}
                with mock.patch.dict(settings.platform, self.fake_settings):
                    with mock.patch.object(BatchRequestHandler, 'auth_methods', [PassportCookieAuth()]):
                        with mock.patch.object(BatchRequestHandler, 'check_permissions', fake_check_permission):
                            with mock.patch.object(mock_handler_cls, 'auth_methods', [PassportCookieAuth()]):
                                return fn(self, *args, **kwargs)
        return wrapped
    return mock_auth_inner


mock_batch_auth_disk = mock_batch_auth(MpfsProxyHandler)


class CookieAuthClientIdTestCase(UserTestCaseMixin, UploadFileTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.EXTERNAL
    api_version = 'v1'
    method = 'GET'
    client_id = '5a12ecda66154db8a76dc67750a632dc'
    client_id_2 = '12345678901234567890123456789012'
    client_id_3 = '00000000000000000000000000000000'
    test_file_path = 'test-file.txt'

    def __init__(self, *args, **kwargs):
        super(CookieAuthClientIdTestCase, self).__init__(*args, **kwargs)
        base_uri = 'http://localhost/'
        self.external_client = ExternalPlatformTestClient(base_uri)
        self.internal_client = InternalPlatformTestClient(base_uri)

    def setup_method(self, method):
        super(CookieAuthClientIdTestCase, self).setup_method(method)
        self.create_user(self.uid, noemail=1)
        self.login = str(self.uid)

        self.upload_file(self.uid, os.path.join('/disk', self.test_file_path))

        self.fake_settings = {
            'auth': [
                {
                    'name': 'maps_common',
                    'enabled': True,
                    'auth_methods': ['cookie'],
                    'allowed_origin_hosts': ['^.*\.maps\.dev\.yandex\.ru$', '^.*cloud\.maps\.yandex\.ru$', 'yandex.ru'],
                    'oauth_client_id': 'maps_common',
                    'oauth_client_name': 'YaMapsWeb',
                    'oauth_scopes': ['cloud_api:disk.read', 'cloud_api:disk.write', 'cloud_api:disk.info',
                                     'cloud_api:data.user_data', 'cloud_api:data.app_data', ],
                    'cookie_auth_client_id': self.client_id,
                },
                {
                    'name': 'maps_common_2',
                    'enabled': True,
                    'auth_methods': ['cookie'],
                    'allowed_origin_hosts': ['^.*\.maps\.dev\.yandex\.ru$', '^.*cloud\.maps\.yandex\.ru$', 'yandex.ru'],
                    'oauth_client_id': 'maps_common',
                    'oauth_client_name': 'YaMapsWeb',
                    'oauth_scopes': ['yadisk:all', 'cloud_api:disk.read'],
                    'cookie_auth_client_id': self.client_id_2,
                },
                {
                    'name': 'maps_common_3',
                    'enabled': True,
                    'auth_methods': ['cookie'],
                    'allowed_origin_hosts': ['^.*\.maps\.dev\.yandex\.ru$', '^.*cloud\.maps\.yandex\.ru$', 'yandex.ru'],
                    'oauth_client_id': 'maps_common',
                    'oauth_client_name': 'YaMapsWeb',
                    'oauth_scopes': [],
                    'cookie_auth_client_id': self.client_id_3,
                }
            ]
        }

        self.base_headers = {
            'Cookie': 'Other=123; Session_id=mocked_value;',
            'Origin': 'https://yandex.ru/maps',
            'Host': '127.0.0.1'
        }

    def _build_url(self, api_url, client_id=None):
        if client_id is None:
            client_id = self.client_id
        if self.api_mode == tags.platform.EXTERNAL:
            return '%s/%s/%s' % (client_id, self.api_version, api_url)
        else:
            return '%s/%s/%s/%s' % (client_id, self.api_version, self.uid, api_url)

    @mock_disk_auth
    def test_cookie_with_client_id(self):
        modes = (tags.platform.EXTERNAL, tags.platform.INTERNAL)
        for mode in modes:
            self.api_mode = mode
            url = self._build_url('disk/resources')
            resp = self.client.request(self.method, url, query={'path': '/'}, headers=self.base_headers)
            assert resp.status_code == 200

    @mock_disk_auth
    def test_cookie_with_wrong_client_id(self):
        self.api_mode = tags.platform.EXTERNAL
        url = self._build_url('disk/resources', client_id='12345')
        resp = self.client.request(self.method, url, query={'path': '/'}, headers=self.base_headers)
        assert resp.status_code == 401

        self.api_mode = tags.platform.INTERNAL
        url = self._build_url('disk/resources', client_id='12345')
        resp = self.client.request(self.method, url, query={'path': '/'}, headers=self.base_headers)
        assert resp.status_code == 200

    @parameterized.expand([
        ('invalid_creds', 401, Response('{"status":{"id":5,"value":"INVALID"},"error":"key with specified id isn\'t found. Probably cookie is too old or cookie was got in wrong environment (production/testing)"}')),
        ('disabled_acc', 401, Response('{"status":{"id":4,"value":"DISABLED"},"error":"account is disabled"}')),
        ('bad_resp', 503, PassportBadResult()),
        ('resp_timeouted', 503, PassportNoResponse()),
    ])
    def test_passport_response(self, case_name, expected_code, blackbox_resp):
        u"""Проверяем корректный код при проблемах взаимодействия с Паспортом.

        Раньше возвращали 401, что приводило к разлогину.
        Правильно возвращать 503, т.к. проблемы с сервисом, а не с авторизационными данными от пользователя.
        """
        if isinstance(blackbox_resp, Exception):
            kwargs = {'side_effect': blackbox_resp}
        else:
            kwargs = {'return_value': blackbox_resp}

        self.api_mode = tags.platform.EXTERNAL
        url = self._build_url('disk/resources')

        with mock.patch('mpfs.platform.auth.PassportCookieAuth._get_client_info_by_host_or_client_id',
                        return_value=({}, {self.client_id: {'validators': [lambda h: {'scopes': ['cloud_api:disk.read',
                                                                                                 'cloud_api:disk.write'],
                                                                                      'id': 'test',
                                                                                      'name': 'test'}]}})), \
             mock.patch('mpfs.platform.auth.authparser.Session.parse', return_value=1), \
             mock.patch.object(MpfsProxyHandler, 'default_auth_methods', [PassportCookieAuth()]), \
             mock.patch.object(MpfsProxyHandler, 'auth_methods', []), \
             mock.patch('mpfs.core.services.passport_service.blackbox.request', **kwargs):
            resp = self.client.request(self.method, url, query={'path': '/'}, headers=self.base_headers)

        self.assertEqual(resp.status_code, expected_code)

    @mock_disk_auth
    def test_get_link(self):
        self.json_ok('rm', {'uid': self.uid, 'path': os.path.join('/disk', self.test_file_path)})

        modes = (tags.platform.EXTERNAL, tags.platform.INTERNAL)
        for mode in modes:
            self.upload_file(self.uid, os.path.join('/disk', self.test_file_path))

            self.api_mode = mode

            new_path = '/123.txt'

            url = self._build_url('disk/resources/move')
            resp = self.client.request('POST', url, query={'from': '/' + self.test_file_path, 'path': new_path},
                                       headers=self.base_headers)
            assert resp.status_code == 201
            href = from_json(resp.content)['href']
            assert self.client_id in href

            parts = urlparse.urlparse(href)
            url = parts.path
            path = urlparse.parse_qs(parts.query)['path']
            resp = self.client.request(self.method, url, query={'path': path}, headers=self.base_headers)
            assert resp.status_code == 200

            self.json_ok('rm', {'uid': self.uid, 'path': os.path.join('/disk', new_path[1:])})

    @mock_data_auth
    def test_data_api_auth(self):
        modes = (tags.platform.EXTERNAL, tags.platform.INTERNAL)
        for mode in modes:
            self.api_mode = mode
            url = self._build_url('data/app/databases')
            resp = self.client.request(self.method, url, query={'path': '/'}, headers=self.base_headers)
            assert resp.status_code == 200

    @mock_disk_auth
    def test_cookie_with_client_id_csrf_protection(self):
        base_headers = {
            'Cookie': 'Other=123; Session_id=mocked_value;',
            'Origin': 'https://malicious-site.ru/maps',
            'Host': '127.0.0.1'
        }

        self.api_mode = tags.platform.EXTERNAL
        url = self._build_url('disk/resources')
        resp = self.client.request(self.method, url, query={'path': '/'}, headers=base_headers)
        assert resp.status_code == 401

    @mock_batch_auth_disk
    def test_cookie_with_client_id_batch(self):
        # протестируем, что batch запрос прокидывает client id из path batch запроса в запросы внутри

        request_data = {
            'items': [
                {
                    'method': 'GET',
                    'relative_url': '/%s/v1/disk/resources?path=/' % self.client_id_3
                },
                {
                    'method': 'GET',
                    'relative_url': '/%s/v1/disk/resources?path=/' % self.client_id_2
                },
                {
                    'method': 'GET',
                    'relative_url': '/%s/v1/disk/resources?path=/' % self.client_id_3
                }
            ]
        }

        self.api_mode = tags.platform.EXTERNAL
        url = self._build_url('batch/request', client_id=self.client_id_3)
        resp = self.client.request('POST', url, data=request_data, headers=self.base_headers)
        assert resp.status_code == 200

        content = from_json(resp.content)
        assert content['items'][0]['code'] == 403
        assert content['items'][1]['code'] == 403  # здесь не должно быть 200, иначе он получит доступ через другой id
        assert content['items'][2]['code'] == 403

    @mock_disk_auth
    def test_unauthorize_cookie_with_http_origin(self):
        self.api_mode = tags.platform.EXTERNAL
        headers = copy(self.base_headers)
        headers['Origin'] = 'http://villainy.maps.dev.yandex.ru'
        url = self._build_url('disk/resources')
        resp = self.client.request(self.method, url, query={'path': '/'}, headers=headers)
        assert resp.status_code == 401


class ExternalClientTokenAuthTestCase(UserTestCaseMixin, UploadFileTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.EXTERNAL
    method = 'GET'

    app_external_token = 'ddf32fed38524dc28a2ac52437ed825c'

    fake_settings = {
        'auth': [
            {
                'name': 'ok-service',
                'enabled': True,
                'auth_methods': ['ext_tokens'],
                'ext_tokens': [app_external_token],
                'oauth_client_id': '123-test-123',
                'oauth_client_name': 'internal-test',
                'oauth_scopes': ['cloud_api:data.user_data', 'cloud_api:data.app_data']
            }
        ]
    }

    def test_common(self):
        with mock.patch.dict(settings.platform, self.fake_settings):
            with mock.patch.object(DataApiProxyHandler, 'default_auth_methods', [ExternalTokenAuth()]):
                with mock.patch.object(DataApiProxyHandler, 'auth_methods', []):
                    headers = {'Authorization': 'ExternalToken token=%s;device=%s;' % (self.app_external_token, self.uid)}
                    resp = self.client.request(self.method, 'data/app/databases',
                                               query={'path': '/'}, headers=headers)
                    assert resp.status_code == 200

    def test_wrong_token(self):
        with mock.patch.dict(settings.platform, self.fake_settings):
            with mock.patch.object(DataApiProxyHandler, 'default_auth_methods', [ExternalTokenAuth()]):
                with mock.patch.object(DataApiProxyHandler, 'auth_methods', []):
                    headers = {'Authorization': 'ExternalToken token=%s;device=%s;' % ('wrong_token', self.uid)}
                    resp = self.client.request(self.method, 'data/app/databases',
                                               query={'path': '/'}, headers=headers)
                    assert resp.status_code == 401

    def test_disk_handlers(self):
        with mock.patch.dict(settings.platform, self.fake_settings):
            with mock.patch.object(DataApiProxyHandler, 'default_auth_methods', [ExternalTokenAuth()]):
                with mock.patch.object(DataApiProxyHandler, 'auth_methods', []):
                    headers = {'Authorization': 'ExternalToken token=%s;device=%s;' % (self.app_external_token, self.uid)}
                    resp = self.client.request(self.method, 'disk/resources',
                                               query={'path': '/'}, headers=headers)
                    assert resp.status_code == 401

    def test_rate_limiter_check(self):
        with mock.patch.dict(settings.platform, self.fake_settings):
            with mock.patch.object(DataApiProxyHandler, 'default_auth_methods', [ExternalTokenAuth()]):
                with mock.patch.object(DataApiProxyHandler, 'auth_methods', []):
                    with mock.patch.object(PerClientIdRateLimiter, 'get_counter_key') as get_counter_key:
                        headers = {'Authorization': 'ExternalToken token=%s;device=%s;' %
                                                    (self.app_external_token, self.uid)}
                        resp = self.client.request(self.method, 'data/app/databases',
                                                   query={'path': '/'}, headers=headers)
                        assert resp.status_code == 200
                        assert get_counter_key.call_count > 0

    def test_several_tokens(self):
        app_external_token_1 = 'ddf32fed38524dc28a2ac52437ed825c'
        app_external_token_2 = 'd264617bf9d048b7a75cdf89a8ea351b'

        fake_settings = {
            'auth': [
                {
                    'name': 'ok-service',
                    'enabled': True,
                    'auth_methods': ['ext_tokens'],
                    'ext_tokens': [app_external_token_1, app_external_token_2],
                    'oauth_client_id': '123-test-123',
                    'oauth_client_name': 'internal-test',
                    'oauth_scopes': ['cloud_api:data.user_data', 'cloud_api:data.app_data']
                }
            ]
        }

        with mock.patch.dict(settings.platform, fake_settings):
            with mock.patch.object(DataApiProxyHandler, 'default_auth_methods', [ExternalTokenAuth()]):
                with mock.patch.object(DataApiProxyHandler, 'auth_methods', []):
                    headers = {'Authorization': 'ExternalToken token=%s;device=%s;' % (app_external_token_1, self.uid)}
                    resp = self.client.request(self.method, 'data/app/databases',
                                               query={'path': '/'}, headers=headers)
                    assert resp.status_code == 200

                    headers = {'Authorization': 'ExternalToken token=%s;device=%s;' % (app_external_token_2, self.uid)}
                    resp = self.client.request(self.method, 'data/app/databases',
                                               query={'path': '/'}, headers=headers)
                    assert resp.status_code == 200

    def test_settings_change(self):
        auth_methods = [ExternalTokenAuth()]
        with mock.patch('mpfs.platform.v1.data.handlers.DataApiProxyHandler.auth_methods', return_value=auth_methods):
            with mock.patch('mpfs.platform.v1.data.handlers.DataApiProxyHandler.get_auth_methods',
                            return_value=auth_methods):
                auth_settings = settings.platform['auth']
                random_client = random.choice(
                    [c for c in auth_settings if 'ext_tokens' in c['auth_methods'] and 'ext_tokens' in c]
                )
                assert random_client
                allowed_tokens = random_client['ext_tokens']
                response = self.client.request(
                    self.method,
                    'data/app/databases',
                    query={'path': '/'},
                    headers={
                        'Authorization': 'ExternalToken token=%s;device=%s' % (allowed_tokens[0], self.uid)
                    }
                )
                assert response.status_code == 200

                # патчим сеттинги, удаляем из них токен, который только что использовали (чтоб получить 401)
                # после этого кидаем событие изменения настроек, чтоб все объекты авторизации
                # подхватили их

                # удаляем клиента, которого только что использовали
                new_auth_settings = [c for c in auth_settings if c['name'] != random_client['name']]
                non_allowed_token = allowed_tokens[0]

                # меняем настройки, а потом кидаем ивент
                with mock.patch.dict('mpfs.config.settings.platform', {'auth': new_auth_settings}):
                    dispatcher.send(SettingsChangeEvent())
                    response = self.client.request(
                        self.method,
                        'data/app/databases',
                        query={'path': '/'},
                        headers={
                            'Authorization': 'ExternalToken token=%s;device=%s' % (non_allowed_token, self.uid)
                        }
                    )
                    assert response.status_code == 401

    def test_auth_url(self):
        with mock.patch.dict(settings.platform, self.fake_settings):
            with mock.patch.object(DataApiProxyHandler, 'default_auth_methods', [ExternalTokenAuth()]):
                with mock.patch.object(DataApiProxyHandler, 'auth_methods', []):
                    import mpfs.core.services.common_service
                    with mock.patch.object(mpfs.core.services.common_service.Service, 'open_url') as open_url:
                        headers = {'Authorization': 'ExternalToken token=%s;device=%s;' % (self.app_external_token, self.uid)}
                        self.client.request(self.method, 'data/app/databases', query={'path': '/'}, headers=headers)

                        is_found = False
                        for args, kwargs in open_url.call_args_list:
                            url = args[0]
                            if 'dataapi' in url:
                                is_found = True
                                qs = urlparse.parse_qs(urlparse.urlparse(url).query)
                                assert qs['__uid'][0].startswith('device_id')
                        assert is_found


class OAuthClientIdTestCase(UserTestCaseMixin, UploadFileTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.EXTERNAL
    api_version = 'v1'
    method = 'GET'
    # Тестовый токен и uid
    oauth_token = 'AgAAAACy1EoXAAAPDgbIOmBzkkBIlIboM25k2tI'
    uid = '3000257047'

    def __init__(self, *args, **kwargs):
        super(OAuthClientIdTestCase, self).__init__(*args, **kwargs)
        base_uri = 'http://localhost/'
        self.external_client = ExternalPlatformTestClient(base_uri)
        self.internal_client = InternalPlatformTestClient(base_uri)

    def setup_method(self, method):
        super(OAuthClientIdTestCase, self).setup_method(method)
        self.create_user(self.uid, noemail=1)
        self.login = str(self.uid)

    @parameterized.expand([
        ('not_pdd', {'uid': {'hosted': False}, "attributes": {}}, 200),
        ('valid_pdd', {'uid': {'hosted': True}, "attributes": {"6": "1"}}, 200),
        ('pdd_no_agreement', {'uid': {'hosted': True}, "attributes": {}}, 401),
        ('unexpected_pdd', {'uid': {'hosted': True}}, 401)
    ])
    def test_accepted_agreements_pdd(self, case_name, bb_response, correct_code):
        u"""
        Проверяем, что не авторизируем ПДД, если у них не принято пользовательское соглашение

        https://st.yandex-team.ru/CHEMODAN-35383
        """
        common_json = {
            'oauth': {
                'scope': 'yadisk:all',
                'client_id': 'maps_common',
                'client_name': 'maps_common',
                'client_is_yandex': True
            },
            'login': self.login
        }
        bb_response.update(common_json)
        bb_response['uid']['value'] = self.uid
        header = {'Authorization': 'OAuth %s' % self.oauth_token}
        response = lambda: None
        response.content = to_json(bb_response)
        with mock.patch('mpfs.core.services.passport_service.blackbox.request',
                        return_value=response) as bb_mock:
            resp = self.client.request(self.method, '%s/%s' % (self.api_version, 'disk/resources'),
                                       query={'path': '/'}, headers=header)
            assert bb_mock.call_args[1]['params']['attributes'] == '6'
            assert resp.status_code == correct_code


@mock.patch('mpfs.core.services.common_service.SERVICES_TVM_2_0_ENABLED', False)
@mock.patch('mpfs.core.services.passport_service.SERVICES_TVM_2_0_ENABLED', False)
@mock.patch('mpfs.core.services.tvm_service.SERVICES_TVM_ENABLED', True)
class OAuthWithTVMTestCase(UserTestCaseMixin, UploadFileTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.EXTERNAL
    api_version = 'v1'
    method = 'GET'
    # Тестовый токен и uid
    oauth_token = 'AgAAAACy1EoXAAAPDgbIOmBzkkBIlIboM25k2tI'
    uid = '3000257047'

    def __init__(self, *args, **kwargs):
        super(OAuthWithTVMTestCase, self).__init__(*args, **kwargs)
        base_uri = 'http://localhost/'
        self.external_client = ExternalPlatformTestClient(base_uri)
        self.internal_client = InternalPlatformTestClient(base_uri)

    def setup_method(self, method):
        super(OAuthWithTVMTestCase, self).setup_method(method)
        self.create_user(self.uid, noemail=1)
        self.login = str(self.uid)

    @parameterized.expand([
        ('invalid_creds', 401, Response('{"status":{"id":5,"value":"INVALID"},"error":"expired_token"}')),
        ('bad_resp', 503, PassportBadResult()),
        ('resp_timeouted', 503, PassportNoResponse()),
    ])
    def test_passport_response(self, case_name, expected_code, blackbox_resp):
        u"""Проверяем корректный код при проблемах взаимодействия с Паспортом.

        Раньше возвращали 401, что приводило к разлогину.
        Правильно возвращать 503, т.к. проблемы с сервисом, а не с авторизационными данными от пользователя.
        """
        if isinstance(blackbox_resp, Exception):
            kwargs = {'side_effect': blackbox_resp}
        else:
            kwargs = {'return_value': blackbox_resp}
        with mock.patch.object(MpfsProxyHandler, 'default_auth_methods', [OAuthAuth()]), \
             mock.patch.object(MpfsProxyHandler, 'auth_methods', []), \
             mock.patch('mpfs.core.services.passport_service.blackbox.request', **kwargs):
            resp = self.client.get('/v1/disk/resources',
                                   query={'path': '/'},
                                   uid=self.uid,
                                   headers={'Authorization': 'OAuth meow'})
        self.assertEqual(resp.status_code, expected_code)


class InternalAuthCommonClass(UserTestCaseMixin, UploadFileTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.INTERNAL
    api_version = 'v1'

    FILE_API_PATH = '/test-file'
    FILE_DISK_PATH = '/disk%s' % FILE_API_PATH

    def setup_method(self, method):
        super(InternalAuthCommonClass, self).setup_method(method)
        self.create_user(self.uid, noemail=1)
        self.login = str(self.uid)


class YateamTVMAuthTestCase(DataApiProfileTest):
    """
    Тестируем авторизацию по TVM тикету для yandex-team окружения
    """
    RETURNED_CLIENT_ID = 77
    NOT_RETURNED_CLIENT_ID = 78

    @classmethod
    def setup_class(cls):
        super(YateamTVMAuthTestCase, cls).setup_class()

        cls.headers = {'Ticket': services.TEST_TVM_TICKET,
                       'Authorization': 'TVM uid=yateam-%s' % cls.uid}

        yateam_tvm_service = {'name': 'yateam-tvm-service',
                              'enabled': True,
                              'auth_methods': [YATEAM_TVM],
                              TVM_CLIENT_IDS: [cls.NOT_RETURNED_CLIENT_ID],
                              'oauth_client_id': 'yateam_tvm_service',
                              'oauth_client_name': 'yateam_tvm_service',
                              'oauth_scopes': ['cloud_api:disk.read', 'cloud_api:disk.write']}
        yateam_tvm_service_with_returned_client_id = deepcopy(yateam_tvm_service)
        yateam_tvm_service_with_returned_client_id[TVM_CLIENT_IDS] = [cls.RETURNED_CLIENT_ID]
        tvm_service = {'name': 'tvm-service',
                       'enabled': True,
                       'auth_methods': ['tvm'],
                       TVM_CLIENT_IDS: [services.TEST_TVM_CLIENT_ID],
                       'oauth_client_id': 'tvm_service',
                       'oauth_client_name': 'tvm_service',
                       'oauth_scopes': ['cloud_api:disk.read', 'cloud_api:disk.write']}
        cls.cases_settings = {'ok': {'auth': [yateam_tvm_service_with_returned_client_id]},
                              'unknown_client_id': {'auth': [yateam_tvm_service,
                                                             tvm_service]}}

        # Формируем диспатчер, который деграет callback'и только нашего класса
        cls.event_dispatcher = EventDispatcher([SettingsChangeEvent])
        yateam_tvm_observers = [observer
                                for observer in dispatcher._observers[SettingsChangeEvent]
                                if isinstance(observer.callback.im_self, TVMAuth)]
        cls.event_dispatcher._observers[SettingsChangeEvent] = yateam_tvm_observers

    def setup_method(self, method):
        super(YateamTVMAuthTestCase, self).setup_method(method)
        # генерируем для каждого теста новый uid, т.к. бэкэнд не позволяет удалять пользователей
        self.uid = self.generate_uid()
        self.login = str(self.uid)

    @contextmanager
    def mock_for_yateam_tvm_auth(self, fake_settings):
        with mock.patch.dict(settings.platform, fake_settings), \
             mock.patch.dict(settings.services['tvm'], {'enabled': True}), \
             mock.patch.object(BasePlatformHandler, 'default_auth_methods', [TVMAuth()]), \
             mock.patch.object(BasePlatformHandler, 'auth_methods', []), \
             mock.patch('mpfs.core.services.tvm_service.TVMService.validate_ticket',
                        return_value=AttrDict({'has_client_id': True,
                                               'client_ids': [self.RETURNED_CLIENT_ID],
                                               'default_uid': (long(self.uid), True)})):

            # Триггерим событие ИзменениеНастроек, чтобы
            # перезапустить сбор TVM client IDs для клиентов Платформы
            self.event_dispatcher.send(SettingsChangeEvent())

            yield

    def test_successful_auth(self):
        with self.mock_for_yateam_tvm_auth(self.cases_settings['ok']):
            # Мокаем запрос в Data API, чтобы проверить успешность именно авторизации
            with mock.patch('mpfs.platform.handlers.ServiceProxyHandler.raw_request_service',
                            return_value=(200, {}, {})) as data_api_request_mock:
                resp = self.client.get('personality/profile/my/type/Name/',
                                       query={'path': '/'}, uid=self.uid, headers=self.headers)
                assert resp.status_code == OK

    def test_pass_uid_with_prefix(self):
        with self.mock_for_yateam_tvm_auth(self.cases_settings['ok']):
            with mock.patch('mpfs.platform.handlers.ServiceProxyHandler.raw_request_service',
                            return_value=(200, {}, {})) as data_api_request_mock:
                self.client.get('personality/profile/my/type/Name/',
                                query={'path': '/'}, uid=self.uid, headers=self.headers)
                qs = urlparse.urlparse(data_api_request_mock.call_args[0][0]).query
                qs_uid = urlparse.parse_qs(qs)['__uid'][0]
                assert 'yt:' + self.uid == qs_uid



class ClientTokenAuthTestCase(InternalAuthCommonClass):
    """
    Тестируем авторизацию внутреннего апи по токену
    """

    def setup_method(self, method):
        super(ClientTokenAuthTestCase, self).setup_method(method)

    @pytest.mark.skipif(True, reason='https://st.yandex-team.ru/CHEMODAN-32396')
    def test_token(self):
        client_token_1 = 'afb136c45e245fba963ddb13ae6f3c68c2ef48a2bd123'
        headers_1 = {'Authorization': 'ClientToken token=%s;uid=%s;' % (client_token_1, self.uid)}

        client_token_2 = 'ehcneiuwhcnrliuehrcnlaiuehrcnaliu327417823y4n'
        headers_2 = {'Authorization': 'ClientToken token=%s;uid=%s;' % (client_token_2, self.uid)}

        wrong_headers = {
            'Authorization': 'ClientToken token=%s;uid=%s;' % ('non-existing-token', self.uid),
        }

        fake_settings = {
            'auth': [
                {
                    'name': 'ok-service',
                    'enabled': True,
                    'auth_methods': ['cookie', 'token'],
                    'token': client_token_1,
                    'allowed_origin_hosts': ['^disk\.yandex\.(ru|ua|by|kz|com|com\.tr)$', 'yandex.net'],
                    'oauth_client_id': '123-test-123',
                    'oauth_client_name': 'internal-test',
                    'oauth_scopes': ['cloud_api:disk.read', 'cloud_api:disk.write'],
                },
                {
                    'name': 'service-without-scopes',
                    'enabled': True,
                    'auth_methods': ['cookie', 'token'],
                    'token': client_token_2,
                    'allowed_origin_hosts': ['^disk\.yandex\.(ru|ua|by|kz|com|com\.tr)$', 'yandex.net'],
                    'oauth_client_id': '123-test-123',
                    'oauth_client_name': 'internal-test',
                    'oauth_scopes': [],
                }
            ]
        }

        with mock.patch.dict(settings.platform, fake_settings):
            with mock.patch.object(MpfsProxyHandler, 'default_auth_methods', [InternalTokenAuth()]):
                with mock.patch.object(MpfsProxyHandler, 'auth_methods', []):
                    # нет такого токена
                    resp = self.client.get('disk/resources', query={'path': '/'}, uid=self.uid, headers=wrong_headers)
                    assert resp.status_code == 401

                    # токен есть, нету нужных скоупов
                    resp = self.client.get('disk/resources', query={'path': '/'}, uid=self.uid, headers=headers_2)
                    assert resp.status_code == 403

                    # все ок!
                    resp = self.client.get('disk/resources', query={'path': '/'}, uid=self.uid, headers=headers_1)
                    assert resp.status_code == 200

    def test_auth_without_uid(self):
        client_token = 'afb136c45e245fba963ddb13ae6f3c68c2ef48a2bd123'
        headers = {'Authorization': 'ClientToken token=%s;' % client_token}
        fake_settings = {
            'auth': [
                {
                    'name': 'ok-service',
                    'enabled': True,
                    'auth_methods': ['token'],
                    'token': client_token,
                    'oauth_client_id': '123-test-123',
                    'oauth_client_name': 'internal-test',
                    'oauth_scopes': ['cloud_api.import.yt:generic.test.resource.path.write'],
                },
            ]
        }
        with mock.patch.dict(settings.platform, fake_settings):
            with mock.patch.object(ImportYTTableHandler, 'auth_methods', [InternalTokenAuth()]):
                resp = self.client.request('POST', '/v1/case/personality/test/resource/path/import/yt',
                                           query={'yt_path': 'test_yt_path'}, headers=headers)
        assert resp.status_code == NOT_FOUND

    def test_url_and_header_uid(self):
        client_token = 'afb136c45e245fba963ddb13ae6f3c68c2ef48a2bd123'
        headers = {'Authorization': 'ClientToken token=%s;uid=%s;' % (client_token, self.uid)}
        uidless_headers = {'Authorization': 'ClientToken token=%s;uiddd=%s;' % (client_token, self.uid)}

        fake_settings = {
            'auth': [
                {
                    'name': 'ok-service',
                    'enabled': True,
                    'auth_methods': ['cookie', 'token'],
                    'token': client_token,
                    'allowed_origin_hosts': ['^disk\.yandex\.(ru|ua|by|kz|com|com\.tr)$', 'yandex.net'],
                    'oauth_client_id': '123-test-123',
                    'oauth_client_name': 'internal-test',
                    'oauth_scopes': ['cloud_api:disk.read', 'cloud_api:disk.write'],
                },
            ]
        }

        # тестируем, что будет, если передать uid в заголовке и не передать в url
        with mock.patch.dict(settings.platform, fake_settings):
            with mock.patch.object(MpfsProxyHandler, 'auth_methods', [InternalTokenAuth()]):
                resp = self.client.request('GET', '/v1/disk/resources', query={'path': '/'}, headers=headers)
                assert resp.status_code == 200

        # тестируем, что будет, если передать uid в заголовке и передать в url
        with mock.patch.dict(settings.platform, fake_settings):
            with mock.patch.object(MpfsProxyHandler, 'auth_methods', [InternalTokenAuth()]):
                resp = self.client.get('disk/resources', query={'path': '/'}, headers=headers, uid=self.uid)
                assert resp.status_code == 200

        # тестируем, что будет, если не передать uid в заголовке и передать в url
        with mock.patch.dict(settings.platform, fake_settings):
            with mock.patch.object(MpfsProxyHandler, 'auth_methods', [InternalTokenAuth()]):
                resp = self.client.request('GET', '/v1/%s/disk/resources' % self.uid,
                                           query={'path': '/'}, headers=uidless_headers)
                assert resp.status_code == 200

        # тестируем, что будет, если не передать uid в заголовке и не передать в url
        with mock.patch.dict(settings.platform, fake_settings):
            with mock.patch.object(MpfsProxyHandler, 'auth_methods', [InternalTokenAuth()]):
                resp = self.client.request('GET', '/v1/disk/resources', query={'path': '/'}, headers=uidless_headers)
                assert resp.status_code == 401

    def test_return_link(self):
        client_token = 'afb136c45e245fba963ddb13ae6f3c68c2ef48a2bd123'
        headers = {'Authorization': 'ClientToken token=%s;uid=%s;' % (client_token, self.uid)}

        fake_settings = {
            'auth': [
                {
                    'name': 'ok-service',
                    'enabled': True,
                    'auth_methods': ['cookie', 'token'],
                    'token': client_token,
                    'allowed_origin_hosts': ['^disk\.yandex\.(ru|ua|by|kz|com|com\.tr)$', 'yandex.net'],
                    'oauth_client_id': '123-test-123',
                    'oauth_client_name': 'internal-test',
                    'oauth_scopes': ['cloud_api:disk.read', 'cloud_api:disk.write'],
                },
            ]
        }

        # тестируем, какая вернется ссылка на скачивание, если в url не будет uid - должна вернуться без uid'a
        with mock.patch.dict(settings.platform, fake_settings):
            with mock.patch.object(MpfsProxyHandler, 'auth_methods', [InternalTokenAuth()]):
                self.upload_file(self.uid, self.FILE_DISK_PATH)
                new_path = '/123'

                resp = self.client.request('POST', '/v1/disk/resources/move',
                                           query={'from': self.FILE_API_PATH, 'path': new_path},
                                           headers=headers)
                assert resp.status_code == 201
                href = from_json(resp.content)['href']
                assert self.uid not in href

        # тестируем, какая вернется ссылка на скачивание, если в url будет uid - должна вернуться с uid'ом
        with mock.patch.dict(settings.platform, fake_settings):
            with mock.patch.object(MpfsProxyHandler, 'auth_methods', [InternalTokenAuth()]):
                self.upload_file(self.uid, self.FILE_DISK_PATH)
                new_path_2 = new_path + '321'

                resp = self.client.post('disk/resources/move',
                                        query={'from': new_path, 'path': new_path_2},
                                        headers=headers, uid=self.uid)
                assert resp.status_code == 201
                href = from_json(resp.content)['href']
                assert self.uid in href


class DisableInternalAuthTestCase(InternalAuthCommonClass):
    """
    Тестируем отключение внутренней авторизации без токенов и прочего (Internal client_id=<id>;client_name=<name>)
    """

    def setup_method(self, method):
        super(DisableInternalAuthTestCase, self).setup_method(method)

    def test_switcher(self):
        headers = {
            'Authorization': 'Internal client_id=test;client_name=test;',
        }

        with mock.patch.object(handlers, 'PLATFORM_DISABLE_INTERNAL_AUTH', False), \
             mock.patch.object(MpfsProxyHandler, 'default_auth_methods', [InternalAuth()]), \
             mock.patch.object(MpfsProxyHandler, 'auth_methods', []):
            # старая авторизация работает
            resp = self.client.get('disk', uid=self.uid, headers=headers)
            assert resp.status_code == 200

        with mock.patch.object(handlers, 'PLATFORM_DISABLE_INTERNAL_AUTH', True), \
             mock.patch.object(MpfsProxyHandler, 'default_auth_methods', [InternalAuth()]), \
             mock.patch.object(MpfsProxyHandler, 'auth_methods', []):
            # старая авторизация не работает
            resp = self.client.get('disk', uid=self.uid, headers=headers)
            assert resp.status_code == 401


def create_tmp_conductor_cache(fn):
    def wrapped(self, *args, **kwargs):
        with mock.patch.object(ConductorService, 'cache_filepath', self.tmp_cache_filename):
            return fn(self, *args, **kwargs)
    return wrapped


class InternalConductorAuthTestCase(InternalAuthCommonClass):
    """
    Тестируем авторизацию внутреннего апи по токену
    """
    def setup_method(self, method):
        super(InternalConductorAuthTestCase, self).setup_method(method)
        self.tmp_cache_filename = '/tmp/mpfs/cache_%s' % hex(random.getrandbits(8*10))[2:-1]

    def teardown_method(self, method):
        super(InternalConductorAuthTestCase, self).teardown_method(method)
        if os.path.exists(self.tmp_cache_filename):
            os.unlink(self.tmp_cache_filename)

    @staticmethod
    def fake_open_url(self, url):
        if 'groups2hosts' in url:
            last_part = url.split('/')[-1]
            group = last_part.split('?')[0]

            if group == 'disk_mpfs':
                return \
                    '[{"id":22625,"fqdn":"mpfs10h.disk.yandex.net"},{"id":22630,"fqdn":"mpfs10j.disk.yandex.net"},' \
                    '{"id":119985,"fqdn":"mpfs10o.disk.yandex.net"},{"id":116231,"fqdn":"mpfs11h.disk.yandex.net"},' \
                    '{"id":119986,"fqdn":"mpfs11o.disk.yandex.net"},{"id":116232,"fqdn":"mpfs12h.disk.yandex.net"},' \
                    '{"id":119987,"fqdn":"mpfs12o.disk.yandex.net"},{"id":116233,"fqdn":"mpfs13h.disk.yandex.net"},' \
                    '{"id":119988,"fqdn":"mpfs13o.disk.yandex.net"},{"id":116234,"fqdn":"mpfs14h.disk.yandex.net"},' \
                    '{"id":119989,"fqdn":"mpfs14o.disk.yandex.net"},{"id":116235,"fqdn":"mpfs15h.disk.yandex.net"},' \
                    '{"id":119990,"fqdn":"mpfs15o.disk.yandex.net"},{"id":15502,"fqdn":"mpfs1g.disk.yandex.net"},' \
                    '{"id":16146,"fqdn":"mpfs1h.disk.yandex.net"},{"id":19770,"fqdn":"mpfs1j.disk.yandex.net"},' \
                    '{"id":119992,"fqdn":"mpfs1o.disk.yandex.net"},{"id":20286,"fqdn":"mpfs2g.disk.yandex.net"},' \
                    '{"id":20290,"fqdn":"mpfs2h.disk.yandex.net"}]'
            elif group == 'disk_api':
                return '[{"id":42500,"fqdn":"api01d.disk.yandex.net"},{"id":42496,"fqdn":"api01e.disk.yandex.net"},' \
                       '{"id":42504,"fqdn":"api01h.disk.yandex.net"},{"id":42501,"fqdn":"api02d.disk.yandex.net"},' \
                       '{"id":42497,"fqdn":"api02e.disk.yandex.net"},{"id":42505,"fqdn":"api02h.disk.yandex.net"},' \
                       '{"id":42502,"fqdn":"api03d.disk.yandex.net"},{"id":42498,"fqdn":"api03e.disk.yandex.net"},' \
                       '{"id":42506,"fqdn":"api03h.disk.yandex.net"},{"id":42503,"fqdn":"api04d.disk.yandex.net"},' \
                       '{"id":42499,"fqdn":"api04e.disk.yandex.net"},{"id":42507,"fqdn":"api04h.disk.yandex.net"}]'
        elif 'hosts' in url:
            last_part = url.split('/')[-1]
            host = last_part.split('?')[0]

            if host == 'mpfs1g.disk.yandex.net':
                return '[{"group":"disk_mpfs","fqdn":"mpfs1g.disk.yandex.net","datacenter":"myt2",' \
                       '"root_datacenter":"myt","short_name":"mpfs1g.disk","description":"",' \
                       '"admins":["eightn","ignition","agodin","ivanlook","dmiga","pperekalov","ivanov-d-s"]}]'
            elif host == 'mpfs1h.disk.yandex.net':
                return '[{"group":"disk_mpfs","fqdn":"mpfs1h.disk.yandex.net","datacenter":"fol4",' \
                       '"root_datacenter":"fol","short_name":"mpfs1h.disk","description":"disk_mpfs",' \
                       '"admins":["eightn","ignition","agodin","ivanlook","dmiga","pperekalov","ivanov-d-s"]}]'
            else:
                return '[]'

    @pytest.mark.skipif(True, reason='https://st.yandex-team.ru/CHEMODAN-32396')
    @create_tmp_conductor_cache
    def test_auth_by_ip(self):
        with mock.patch.object(ConductorService, 'open_url', self.fake_open_url):
            fake_platform = {
                'auth': [
                    {
                        'name': 'ok-service',
                        'enabled': True,
                        'auth_methods': ['conductor'],
                        'conductor_items': ['%disk_api', '%disk_mpfs', 'mpfs1g.disk.yandex.net'],
                        'allowed_origin_hosts': ['^disk\.yandex\.(ru|ua|by|kz|com|com\.tr)$', 'yandex.net'],
                        'oauth_client_id': '123-test-123',
                        'oauth_client_name': 'internal-test',
                        'oauth_scopes': ['cloud_api:disk.read', 'cloud_api:disk.write'],
                    },
                    {
                        'name': 'service-without-scopes',
                        'enabled': True,
                        'auth_methods': ['conductor'],
                        'conductor_items': ['mpfs1h.disk.yandex.net'],
                        'allowed_origin_hosts': ['^disk\.yandex\.(ru|ua|by|kz|com|com\.tr)$', 'yandex.net'],
                        'oauth_client_id': '123-test-123',
                        'oauth_client_name': 'internal-test',
                        'oauth_scopes': [],
                    }
                ]
            }

            ConductorService(read_cache=False).update_cache(fake_platform['auth'])
            ip_with_scopes = ConductorService._get_ip_by_hostname('mpfs1g.disk.yandex.net')[0]
            ip_without_scopes = ConductorService._get_ip_by_hostname('mpfs1h.disk.yandex.net')[0]
            ip_not_valid = '192.168.123.321'

            # тестируем, что будет, если скоупы есть
            with mock.patch.dict(settings.platform, fake_platform):
                with mock.patch.object(MpfsProxyHandler, 'auth_methods', [InternalConductorAuth()]):
                    with mock.patch.object(MpfsProxyHandler, 'default_auth_methods', []):
                        resp = self.client.get('disk/resources', query={'path': '/'}, uid=self.uid, ip=ip_with_scopes)
                        assert resp.status_code == 200

            # тестируем, что будет, если скоупов нет
            with mock.patch.dict(settings.platform, fake_platform):
                with mock.patch.object(MpfsProxyHandler, 'auth_methods', [InternalConductorAuth()]):
                    with mock.patch.object(MpfsProxyHandler, 'default_auth_methods', []):
                        resp = self.client.get('disk/resources', query={'path': '/'}, uid=self.uid, ip=ip_without_scopes)
                        assert resp.status_code == 403

            # тестируем, что будет, если клиент пришел с левого ip
            with mock.patch.dict(settings.platform, fake_platform):
                with mock.patch.object(MpfsProxyHandler, 'auth_methods', [InternalConductorAuth()]):
                    with mock.patch.object(MpfsProxyHandler, 'default_auth_methods', []):
                        resp = self.client.get('disk/resources', query={'path': '/'}, uid=self.uid, ip=ip_not_valid)
                        assert resp.status_code == 401

    @create_tmp_conductor_cache
    def test_authorization_header_with_uid(self):
        with mock.patch.object(ConductorService, 'open_url', self.fake_open_url):
            headers = {'Authorization': 'IP uid=%s;' % self.uid}
            fake_platform = {
                'auth': [
                    {
                        'name': 'ok-service',
                        'enabled': True,
                        'auth_methods': ['conductor'],
                        'conductor_items': ['%disk_api', '%disk_mpfs', 'mpfs1g.disk.yandex.net'],
                        'allowed_origin_hosts': ['^disk\.yandex\.(ru|ua|by|kz|com|com\.tr)$', 'yandex.net'],
                        'oauth_client_id': '123-test-123',
                        'oauth_client_name': 'internal-test',
                        'oauth_scopes': ['cloud_api:disk.read', 'cloud_api:disk.write'],
                    },
                ]
            }

            ConductorService(read_cache=False).update_cache(fake_platform['auth'])
            ip = ConductorService._get_ip_by_hostname('mpfs1g.disk.yandex.net')[0]

            # тестируем, что будет, если скоупы есть
            with mock.patch.dict(settings.platform, fake_platform):
                with mock.patch.object(MpfsProxyHandler, 'auth_methods', [InternalConductorAuth()]):
                    with mock.patch.object(MpfsProxyHandler, 'default_auth_methods', []):
                        resp = self.client.get('disk/resources', query={'path': '/'}, ip=ip, headers=headers)
                        assert resp.status_code == 200


class NotesAppAuthTestCase(DiskApiTestCase):
    api_mode = tags.platform.EXTERNAL

    def test_data_subscriptions_app(self):
        query = {
            'databases_ids': 'db1,db2',
            'app_name': 'test_app_name',
            'platform': 'gcm',
            'registration_token': 'test_platform_push_token',
            'app_instance_id': 'test_app_instance_uuid'
        }
        with self.specified_client(id=PLATFORM_NOTES_APP_ID):
            resp = self.client.put('data/subscriptions/app', query=query)
            assert_that(resp.status_code, is_(equal_to(201)))


class NetworkAuthTestCase(TVM2NetworksMocksMixin, TestTVM2Base, UserTestCaseMixin):
    api_version = 'v1'
    api_mode = tags.platform.INTERNAL
    settings_with_empty_allowed_networks = None
    ip_from_network = '2a02:6b8:c00:a:a:a:a:a'
    ip_not_from_network = '2a02:ab8:c00:a:a:a:a:a'

    @classmethod
    def setup_class(cls):
        super(NetworkAuthTestCase, cls).setup_class()
        cls.fake_settings['with_allowed_networks'] = deepcopy(cls.fake_settings['without_allowed_networks'])
        cls.fake_settings['with_allowed_networks']['auth'][0][ClientNetworks.ALLOWED_NETWORK_ADDRESSES] = ['2a02:6b8:c00::/40']
        cls.fake_settings['with_empty_allowed_networks'] = deepcopy(cls.fake_settings['without_allowed_networks'])
        cls.fake_settings['with_empty_allowed_networks']['auth'][0][ClientNetworks.ALLOWED_NETWORK_ADDRESSES] = []

        # Формируем диспатчер, который деграет callback'и только нашего класса
        cls.client_networks_dispatcher = EventDispatcher([SettingsChangeEvent])
        client_networks_observers = [observer
                                     for observer in dispatcher._observers[SettingsChangeEvent]
                                     if isinstance(observer.callback.im_self, ClientNetworks)]
        cls.client_networks_dispatcher._observers[SettingsChangeEvent] = client_networks_observers

    @contextmanager
    def mock_for_successful_tvm_auth(self, fake_settings):
        with mock.patch.dict(settings.platform, fake_settings), \
             mock.patch.object(MpfsProxyHandler, 'default_auth_methods', [TVM2Auth()]), \
             mock.patch.object(MpfsProxyHandler, 'auth_methods', []), \
             mock.patch('mpfs.core.services.tvm_2_0_service.TVM2Service.validate_service_ticket',
                        return_value=AttrDict({'src': SERVICES_TVM_2_0_CLIENT_ID})), \
             mock.patch('mpfs.core.services.tvm_2_0_service.TVM2Service.validate_user_ticket',
                        return_value=AttrDict({'default_uid': self.token_uid, 'uids': [self.token_uid]})):

            # Триггерим событие ИзменениеНастроек, чтобы
            # перезапустить сбор разрешенных сетей для клиентов Платформы
            self.client_networks_dispatcher.send(SettingsChangeEvent())

            yield

    @parameterized.expand([
        ('authorized_ip', ip_from_network, OK),
        ('unauthorized_ip', ip_not_from_network, UNAUTHORIZED),
    ])
    def test_network_authorization_with_good_tvm_auth(self, case_name, client_ip, expected_status_code):
        with self.mock_for_successful_tvm_auth(self.fake_settings['with_allowed_networks']):
            header = {'X-Ya-Service-Ticket': self.service_ticket, 'X-Ya-User-Ticket': self.user_ticket}
            resp = self.client.get('disk/resources', query={'path': '/'}, headers=header, ip=client_ip)
            assert resp.status_code == expected_status_code

    @parameterized.expand([
        'without_allowed_networks',
        'with_empty_allowed_networks',
    ])
    def test_tvm_auth_without_allowed_networks(self, case_name):
        with self.mock_for_successful_tvm_auth(self.fake_settings[case_name]):
            header = {'X-Ya-Service-Ticket': self.service_ticket, 'X-Ya-User-Ticket': self.user_ticket}
            resp = self.client.get('disk/resources', query={'path': '/'}, headers=header, ip=self.ip_not_from_network)
            assert resp.status_code == OK

    def test_silent_mode(self):
        """Проверяем silent-mode.

        Делая запрос с IP, с которого клиенту нельзя делать запросы, ожидаем:
          * запрос пройдет
          * но будет залогировано, что запрос делался с IP, с которого клиенту нельзя делать запросы в Платформу
        """
        with self.mock_for_successful_tvm_auth(self.fake_settings['with_allowed_networks']), \
             mock.patch('mpfs.platform.handlers.logger.error_log.error') as mocked_error_log, \
             mock.patch.dict('mpfs.config.settings.auth',
                             {'network_authorization': {'silent_mode': True, 'enabled': True}}):
            header = {'X-Ya-Service-Ticket': self.service_ticket, 'X-Ya-User-Ticket': self.user_ticket}
            resp = self.client.get('disk/resources', query={'path': '/'}, headers=header, ip=self.ip_not_from_network)

            assert resp.status_code == OK
            mocked_error_log.assert_called()
            assert_that(mocked_error_log.call_args[0][0],
                        all_of(contains_string(PLATFORM_NETWORK_AUTHORIZATION_SILENT_MODE_LOG_PREFIX),
                               contains_string(str(self.ip_not_from_network))))


class LoggingUnauthorizedRequestsTestCase(TestTVM2Base, UserTestCaseMixin):
    api_version = 'v1'
    api_mode = tags.platform.EXTERNAL

    def test_succeeded_to_fetch_credentials_but_unauthorized_writes_to_log(self):
        self.base_headers = {
            'Cookie': 'Other=123; Session_id=mocked_value;',
            'Origin': 'https://yandex.ru/maps',
            'Host': '127.0.0.1'
        }
        self.api_mode = tags.platform.EXTERNAL
        with mock.patch.object(MpfsProxyHandler, 'default_auth_methods', [PassportCookieAuth()]), \
                mock.patch.object(MpfsProxyHandler, 'auth_methods', []), \
                mock.patch('mpfs.platform.common._Logger.error_log', return_value=mock.MagicMock()) as m, \
                mock.patch.object(PassportCookieAuth, '_validate_credentials', return_value=False):
            resp = self.client.get('/v1/disk/resources', query={'path': '/', 'uid': self.uid}, headers=self.base_headers)
        assert resp.status_code == 401
        assert len(m.error.call_args_list) == 2
        assert 'Authorization Failed' in m.error.call_args_list[0][0][0]
        assert 'PassportCookieAuth' in m.error.call_args_list[0][0][0]
        assert 'UnauthorizedError' in m.error.call_args_list[1][0][0]

    def test_all_authorization_has_failed(self):
        self.base_headers = {
            'Cookie': 'Other=123; Session_id=mocked_value;',
            'Origin': 'https://yandex.ru/maps',
            'Host': '127.0.0.1',
            'Authorization': 'OAuth test',
        }
        self.api_mode = tags.platform.EXTERNAL
        with mock.patch.object(MpfsProxyHandler, 'default_auth_methods', [PassportCookieAuth(), OAuthAuth()]), \
                mock.patch.object(MpfsProxyHandler, 'auth_methods', []), \
                mock.patch('mpfs.platform.common._Logger.error_log', return_value=mock.MagicMock()) as m, \
                mock.patch.object(PassportCookieAuth, '_validate_credentials', return_value=False), \
                mock.patch.object(OAuthAuth, '_validate_credentials', return_value=False):
            resp = self.client.get('/v1/disk/resources', query={'path': '/', 'uid': self.uid}, headers=self.base_headers)
        assert resp.status_code == 401
        assert len(m.error.call_args_list) == 3
        assert 'Authorization Failed' in m.error.call_args_list[0][0][0]
        assert 'PassportCookieAuth' in m.error.call_args_list[0][0][0]
        assert 'Authorization Failed' in m.error.call_args_list[1][0][0]
        assert 'OAuthAuth' in m.error.call_args_list[1][0][0]
        assert 'UnauthorizedError' in m.error.call_args_list[2][0][0]

    def test_one_authorization_has_failed(self):
        self.base_headers = {
            'Cookie': 'Other=123; Session_id=mocked_value;',
            'Origin': 'https://yandex.ru/maps',
            'Host': '127.0.0.1',
            'Authorization': 'OAuth test',
        }
        self.api_mode = tags.platform.EXTERNAL
        with mock.patch.object(MpfsProxyHandler, 'default_auth_methods', [OAuthAuth(), PassportCookieAuth()]), \
                mock.patch.object(MpfsProxyHandler, 'auth_methods', []), \
                mock.patch('mpfs.platform.common._Logger.error_log', return_value=mock.MagicMock()) as m, \
                mock.patch.object(PassportCookieAuth, 'validate_session_cookie', return_value={'uid': self.uid, 'login': self.login}), \
                mock.patch.object(OAuthAuth, '_validate_credentials', return_value=False):
            resp = self.client.get('/v1/disk/resources', query={'path': '/', 'uid': self.uid}, headers=self.base_headers)
        assert resp.status_code == 200
        assert len(m.error.call_args_list) == 1
        assert 'Authorization Failed' in m.error.call_args_list[0][0][0]
        assert 'OAuthAuth' in m.error.call_args_list[0][0][0]

    def test_not_started_authorization_not_in_log(self):
        self.base_headers = {
            'Cookie': 'Other=123; Session_id=mocked_value;',
            'Origin': 'https://yandex.ru/maps',
            'Host': '127.0.0.1',
            'Authorization': 'OAuth test',
        }
        self.api_mode = tags.platform.EXTERNAL
        with mock.patch.object(MpfsProxyHandler, 'default_auth_methods', [PassportCookieAuth(), OAuthAuth()]), \
                mock.patch.object(MpfsProxyHandler, 'auth_methods', []), \
                mock.patch('mpfs.platform.common._Logger.error_log', return_value=mock.MagicMock()) as m, \
                mock.patch.object(PassportCookieAuth, '_get_credentials', return_value=None), \
                mock.patch.object(OAuthAuth, '_validate_credentials', return_value=False):
            resp = self.client.get('/v1/disk/resources', query={'path': '/', 'uid': self.uid}, headers=self.base_headers)
        assert resp.status_code == 401
        assert len(m.error.call_args_list) == 2
        assert 'Authorization Failed' in m.error.call_args_list[0][0][0]
        assert 'UnauthorizedError' in m.error.call_args_list[1][0][0]

    def test_user_authorization_required_but_not_provided(self):
        self.base_headers = {
            'Cookie': 'Other=123; Session_id=mocked_value;',
            'Origin': 'https://yandex.ru/maps',
            'Host': '127.0.0.1',
            'Authorization': 'OAuth test',
        }
        self.api_mode = tags.platform.EXTERNAL
        with mock.patch.object(MpfsProxyHandler, 'default_auth_methods', [PassportCookieAuth()]), \
                mock.patch.object(MpfsProxyHandler, 'auth_methods', []), \
                mock.patch('mpfs.platform.common._Logger.error_log', return_value=mock.MagicMock()) as m, \
                mock.patch.object(PassportCookieAuth, '_validate_credentials', return_value=True):
            resp = self.client.get('/v1/disk/resources', query={'path': '/'}, headers=self.base_headers)
        assert resp.status_code == 401
        assert len(m.error.call_args_list) == 2
        assert 'User authorization is required' in m.error.call_args_list[0][0][0]
        assert 'UnauthorizedError' in m.error.call_args_list[1][0][0]


class DisableAutheticationMethodTestCase(UserTestCaseMixin, DiskApiTestCase):

    def test_disable_internal_auth_by_header(self):
        self.api_version = 'v1'
        self.api_mode = tags.platform.INTERNAL
        with mock.patch.object(MpfsProxyHandler, 'default_auth_methods', [InternalAuth(), TVM2Auth(), InternalConductorAuth()]), \
             mock.patch.object(MpfsProxyHandler, 'auth_methods', []), \
             mock.patch.object(InternalAuth, 'authorize', return_value=True) as auth_1_mock, \
             mock.patch.object(TVM2Auth, 'authorize', return_value=True) as auth_2_mock, \
             mock.patch.object(InternalConductorAuth, 'authorize', return_value=True) as auth_3_mock:
            headers = {'X-Disable-Auth-Methods': 'TVM2Auth;InternalAuth'}
            self.client.get('disk/resources', query={'path': '/'}, headers=headers)
            assert not auth_1_mock.called
            assert not auth_2_mock.called
            assert auth_3_mock.called

    def test_disable_internal_auth_by_header_2(self):
        self.api_version = 'v1'
        self.api_mode = tags.platform.EXTERNAL
        with mock.patch.object(MpfsProxyHandler, 'default_auth_methods', [InternalAuth(), TVM2Auth(), InternalConductorAuth()]), \
             mock.patch.object(MpfsProxyHandler, 'auth_methods', []), \
             mock.patch.object(InternalAuth, 'authorize', return_value=True) as auth_1_mock, \
             mock.patch.object(TVM2Auth, 'authorize', return_value=True) as auth_2_mock, \
             mock.patch.object(InternalConductorAuth, 'authorize', return_value=True) as auth_3_mock:
            headers = {'X-Disable-Auth-Methods': 'TVM2Auth;InternalAuth'}
            self.client.get('disk/resources', query={'path': '/'}, headers=headers)
            assert auth_1_mock.called
            assert not auth_2_mock.called
            assert not auth_3_mock.called
