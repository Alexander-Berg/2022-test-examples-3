# -*- coding: utf-8 -*-
import base64
import json
import time
import urlparse

from contextlib import nested
from copy import deepcopy, copy

import mock
import pytest
from hamcrest import assert_that, has_item, contains_string
from nose_parameterized import parameterized

from mpfs.common.errors import DataApiBadResponse
from mpfs.platform.rate_limiters import PerPublicDBRateLimiter
from test.fixtures.users import default_user
from test.helpers.assertions import assert_log_contains
from test.parallelly.api.base import ApiTestCase, InternalPlatformTestClient
from test.base_suit import UserTestCaseMixin
from test.helpers.stubs.services import PushServicesStub, DataApiStub
from test.parallelly.api.disk.base import DiskApiTestCase

from mpfs.common.static import tags
from mpfs.common.util import from_json, to_json
from mpfs.config import settings
from mpfs.core.services.data_api_service import data_api
from mpfs.platform.v1.data import data_pb2
from mpfs.platform.v1.data.data_pb2 import RecordChange, FieldChange
from mpfs.platform.v1.data.exceptions import (DataInvalidIdError,
                                              DataDeltaIsNewerThanDatabaseRevisionError,
                                              DataFieldListItemMoveWithoutDestError,
                                              DataFieldListItemWithoutIndexError,
                                              DataFieldSetWithoutValueError)
from mpfs.platform.v1.data.handlers import (
    DataApiProxyHandler,
    PublicDbId,
)


class DataApiTest(ApiTestCase):
    api_mode = tags.platform.EXTERNAL
    api_version = 'v1'
    uid = None
    master_slave_sync_delay = 0.1
    """Время синхронизации мастера (в который всё пишется) со слэйвами (с котороых всё читается)."""

    def generate_uid(self):
        return str(int(time.time() * 1000000000))

    def setup_method(self, method):
        super(DataApiTest, self).setup_method(method)
        data_api.log = self.log


class DatabasesTestCase(DataApiTest):
    """Тесты ручек для работы с БД"""

    def setup_method(self, method):
        super(DatabasesTestCase, self).setup_method(method)
        # генерируем для каждого теста новый uid, т.к. бэкэнд не позволяет удалять пользователей
        self.uid = self.generate_uid()
        self.login = str(self.uid)

    def test_user_auto_initialization(self):
        with self.specified_client(scopes=['cloud_api.data:user_data', 'cloud_api.data:app_data'], uid=self.uid, login=self.login):
            # создаём базочку для приложения не имея на то прав
            resp = self.client.request('GET', 'data/app/databases')
            self.assertEqual(resp.status_code, 200)

    def test_rate_limiter(self):
        public_db_id = PublicDbId(app_id='raccoon', db_id='house')
        with self.specified_client(scopes=['cloud_api.data:user_data', 'cloud_api.data:app_data']), \
                DataApiStub(), \
                mock.patch('mpfs.core.services.rate_limiter_service.RateLimiterService.is_limit_exceeded') as mocked_rate_limiter:
            self.client.request('GET', 'data/app/databases/.pub.%s/deltas?base_revision=89' % public_db_id)

        mocked_rate_limiter.assert_called_once()
        group, key, value = mocked_rate_limiter.call_args[0]
        assert group in (PerPublicDBRateLimiter.DEFAULT_GROUP_TMPL % i for i in xrange(1, 4))
        assert key == str(public_db_id)
        assert value == 1

    def test_no_rate_limiter_for_non_public_db(self):
        client_id = InternalPlatformTestClient.client_id
        with self.specified_client(id=client_id, scopes=['cloud_api.data:user_data', 'cloud_api.data:app_data']), \
                DataApiStub(), \
                mock.patch('mpfs.core.services.rate_limiter_service.RateLimiterService.is_limit_exceeded') as mocked_rate_limiter:
            self.client.request('GET', 'data/app/databases/.ext.%s/deltas?base_revision=89' % 'maps')

        # должен отработать дефолтный RL (дефолтный это теперь по клиент id)
        mocked_rate_limiter.assert_called_once_with('dataapi_by_client_id', client_id, 1)

    def test_context_permissions(self):
        with self.specified_client(scopes=['cloud_api.data:user_data'], uid=self.uid, login=self.login):
            resp = self.client.request('GET', 'data/app/databases')
            self.assertEqual(resp.status_code, 403)
        with self.specified_client(scopes=['cloud_api.data:app_data'], uid=self.uid, login=self.login):
            resp = self.client.request('GET', 'data/user/databases')
            self.assertEqual(resp.status_code, 403)

        with self.specified_client(scopes=['cloud_api:data.user_data'], uid=self.uid, login=self.login):
            resp = self.client.request('GET', 'data/app/databases')
            self.assertEqual(resp.status_code, 403)
        with self.specified_client(scopes=['cloud_api:data.app_data'], uid=self.uid, login=self.login):
            resp = self.client.request('GET', 'data/user/databases')
            self.assertEqual(resp.status_code, 403)

        with self.specified_client(scopes=['cloud_api:data.app_data'], uid=self.uid, login=self.login):
            resp = self.client.request('GET', 'data/app/databases')
            self.assertEqual(resp.status_code, 200)
        with self.specified_client(scopes=['cloud_api:data.user_data'], uid=self.uid, login=self.login):
            resp = self.client.request('GET', 'data/user/databases')
            self.assertEqual(resp.status_code, 200)
        with self.specified_client(scopes=['cloud_api.data:app_data'], uid=self.uid, login=self.login):
            resp = self.client.request('GET', 'data/app/databases')
            self.assertEqual(resp.status_code, 200)
        with self.specified_client(scopes=['cloud_api.data:user_data'], uid=self.uid, login=self.login):
            resp = self.client.request('GET', 'data/user/databases')
            self.assertEqual(resp.status_code, 200)

        with self.specified_client(id='7a54f58d4ebe431caaaa53895522bf2d', uid=self.uid, login=self.login):
            resp = self.client.request('GET', 'data/user/databases')
            self.assertEqual(resp.status_code, 200)

    def test_databases_number_limit_error(self):
        with self.specified_client(scopes=['cloud_api.data:user_data', 'cloud_api.data:app_data'], uid=self.uid, login=self.login):
            statuses = set()
            for i in xrange(20):
                resp = self.client.request('PUT', 'data/user/databases/test_%i' % i)
                statuses.add(resp.status)
                self.assertIn(resp.status_code, [201, 507])
                if resp.set_status == 507:
                    self.assertEqual(resp.content['error'], 'DataDatabasesNumberLimitExceededError')
            self.assertEqual(statuses, {201, 507})

    def test_normal_behaviour(self):
        with self.specified_client(scopes=['cloud_api.data:user_data', 'cloud_api.data:app_data'], uid=self.uid, login=self.login):
            # создаём базочку пользователя имея на то право
            resp = self.client.request('PUT', 'data/user/databases/test')
            self.assertEqual(resp.status_code, 201)
            self.assert_(resp.headers.get('ETag') == 0)

            # читаем созданную базочку
            resp = self.client.request('GET', 'data/user/databases/test')
            self.assertEqual(resp.status_code, 200)
            self.assert_(resp.headers.get('ETag') == 0)

            # пытаемся cоздать одноимённую базочку
            resp = self.client.request('PUT', 'data/user/databases/test')
            self.assertEqual(resp.status_code, 200)
            self.assert_(resp.headers.get('ETag') == 0)

            # читаем список баз
            resp = self.client.request('GET', 'data/user/databases')
            self.assertEqual(resp.status_code, 200)

            # читаем снапшотик базы
            resp = self.client.request('GET', 'data/user/databases/test/snapshot')
            self.assertEqual(resp.status_code, 200)
            self.assert_(resp.headers.get('ETag') == 0)
            resp = json.loads(resp.content)
            self.assertEqual(resp.get('records_count'), 0)
            self.assert_('created' in resp)
            self.assert_('modified' in resp)
            self.assert_('records' in resp)
            self.assert_('size' in resp)
            self.assert_('handle' in resp)
            self.assert_(resp.get('database_id') == 'test')
            self.assert_(resp.get('revision') == 0)

            # удаляем базочку
            resp = self.client.request('DELETE', 'data/user/databases/test')
            self.assertEqual(resp.status_code, 204)

            time.sleep(self.master_slave_sync_delay)  # к сожалению, бэкэнд не сразу осознаёт, что в нём что-то изменили

            # пытаемся получить только что удалённую базочку
            resp = self.client.request('GET', 'data/user/databases/test')
            self.assertEqual(resp.status_code, 404)

    def test_get_app_database_snapshot(self):
        with self.specified_client(scopes=['cloud_api:data.app_data'], uid=self.uid, login=self.login):
            resp = self.client.request('PUT', 'data/app/databases/test')
            assert resp.status_code == 201

            resp = self.client.request('GET', 'data/app/databases/test/snapshot')
            self.assertEqual(resp.status_code, 200)

    def test_update_metadata(self):
        with self.specified_client(scopes=['cloud_api.data:user_data'], uid=self.uid, login=self.login):
            resp = self.client.request('PUT', 'data/user/databases/test')
            assert resp.status_code == 201

            title = 'Hello!'
            resp = self.client.request('PATCH', 'data/user/databases/test', data={'title': title})
            assert resp.status_code == 200
            # проверяем что возвращённая ручкой база содержит уже новый title
            db = json.loads(resp.content)
            assert db['title'] == title

            time.sleep(self.master_slave_sync_delay)  # к сожалению, бэкэнд не сразу осознаёт, что в нём что-то изменили

            # проверяем, что на GET базы данных возвращается БД с новым title
            resp = self.client.request('GET', 'data/user/databases/test')
            assert resp.status_code == 200
            db = json.loads(resp.content)
            assert db['title'] == title

            # проверяем что если передать в теле запроса readonly параметры,
            # то изменятся только те значения которые можно менять, в данном случае title
            data = {
                'title': 'Hello world!',
                'database_id': 'asdf',
                'revision': 100500,
                'records_count': 500100,
                'created': '2025-10-15T12:34:53.008000+00:00',
                'modified': '2035-10-15T12:34:53.008000+00:00',
                'size': 1000000,
            }
            resp = self.client.request('PATCH', 'data/user/databases/test', data=data)
            assert resp.status_code == 200
            updated_db = json.loads(resp.content)
            assert updated_db['title'] == data['title']
            db.pop('title')
            assert updated_db['modified'] != db.pop('modified')
            for k, v in db.iteritems():
                assert updated_db[k] == v
            time.sleep(self.master_slave_sync_delay)  # к сожалению, бэкэнд не сразу осознаёт, что в нём что-то изменили
            # проверяем, что в бэкэнде ни чего лишнего не изменилось
            resp = self.client.request('GET', 'data/user/databases/test')
            assert resp.status_code == 200
            updated_db = json.loads(resp.content)
            assert updated_db['title'] == data['title']
            for k, v in db.iteritems():
                assert updated_db[k] == v

            # Удаляем title вообще.
            resp = self.client.request('PATCH', 'data/user/databases/test', data={'title': None})
            assert resp.status_code == 200
            updated_db = json.loads(resp.content)
            assert 'title' not in updated_db

            # Пустой patch не возвращает title и не фэйлит ни чего
            resp = self.client.request('PATCH', 'data/user/databases/test', data={})
            assert resp.status_code == 200
            updated_db = json.loads(resp.content)
            assert 'title' not in updated_db

            # Возвращаем title на место
            title = u'Превееееед!'
            resp = self.client.request('PATCH', 'data/user/databases/test', data={'title': title})
            assert resp.status_code == 200
            updated_db = json.loads(resp.content)
            assert updated_db['title'] == title

            # Пустой patch не удаляет title и не фэйлит ни чего
            resp = self.client.request('PATCH', 'data/user/databases/test', data={})
            assert resp.status_code == 200
            updated_db = json.loads(resp.content)
            assert updated_db['title'] == title

    def test_context_switch(self):
        with self.specified_client(scopes=['cloud_api.data:user_data', 'cloud_api.data:app_data'], uid=self.uid, login=self.login):
            # создаём базу в одном контексте
            resp = self.client.request('PUT', 'data/app/databases/test1')
            self.assertEqual(resp.status_code, 201)
            # пытаемся прочитать её из другого контекста
            resp = self.client.request('GET', 'data/user/databases/test1')
            self.assertEqual(resp.status_code, 404)

            # и наоборот
            # создаём базу в одном контексте
            resp = self.client.request('PUT', 'data/user/databases/test2')
            self.assertEqual(resp.status_code, 201)

            time.sleep(self.master_slave_sync_delay)  # к сожалению, бэкэнд не сразу осознаёт, что в нём что-то изменили

            # пытаемся прочитать её из другого контекста
            resp = self.client.request('GET', 'data/app/databases/test2')
            self.assertEqual(resp.status_code, 404)

    def test_database_not_found(self):
        with self.specified_client(scopes=['cloud_api.data:user_data', 'cloud_api.data:app_data'], uid=self.uid, login=self.login):
            resp = self.client.request('GET', 'data/user/databases/test')
            self.assertEqual(resp.status_code, 404)

    def test_invalid_id(self):
        with self.specified_client(scopes=['cloud_api.data:user_data', 'cloud_api.data:app_data'], uid=self.uid, login=self.login):
            resp = self.client.request('PUT', 'data/app/databases/test%40!')
            self.assertEqual(resp.status_code, 404)


class DataYaTeamCookieAuthTestCase(DataApiTest):
    @staticmethod
    def mock_auth_checks(uid, login):
        return nested(
            mock.patch('mpfs.platform.auth.PassportCookieAuth.validate_session_cookie', side_effect=ValueError()),
            mock.patch('mpfs.platform.auth.YaTeamCookieAuth.validate_session_cookie',
                       return_value={'uid': uid, 'login': login}),
            mock.patch('mpfs.platform.permissions.AllowByClientIdPermission.has_permission', return_value=True))

    def setup_method(self, method):
        super(DataYaTeamCookieAuthTestCase, self).setup_method(method)
        # генерируем для каждого теста новый uid, т.к. бэкэнд не позволяет удалять пользователей
        self.uid = self.generate_uid()
        self.login = str(self.uid)

        self.base_headers = {
            'Cookie': 'Other=123; Session_id=hernia!_Vse_ravno_mock-aem;',
            'Origin': 'https://disk.yandex.ru',
            'Host': '127.0.0.1'
        }

        self.query = {
            'databases_ids': 'db1,db2',
            'app_name': 'test_app_name',
            'platform': 'gcm',
            'registration_token': 'test_client_registration_token',
            'app_instance_id': 'test_app_instance_uuid',
            'client_token': 'ctoken1',
            'callback': 'callback_url_1',
            'session': 'session1',
        }

    def test_database_endpoints_use_yateam_auth(self):
        with self.mock_auth_checks(self.uid, self.login):
            with mock.patch('mpfs.platform.v1.data.handlers.DataApiProxyHandler.raw_request_service',
                            return_value=(200, {}, {})) as data_api_request_mock:
                self.client.request('PUT', 'data/user/databases/test', headers=self.base_headers)
                qs = urlparse.urlparse(data_api_request_mock.call_args[0][0]).query
                qs_uid = urlparse.parse_qs(qs)['__uid'][0]
                assert 'yt:' + self.uid == qs_uid

    def test_app_subscriptions_endpoints_use_yateam_auth(self):
        with self.mock_auth_checks(self.uid, self.login):
            with PushServicesStub() as stub:
                self.client.request('PUT', 'data/subscriptions/app', query=self.query, headers=self.base_headers)
                qs_uid = stub.subscribe_app_common.call_args[1]['uid']
                assert 'yt:' + self.uid == qs_uid

    def test_callback_subscriptions_endpoints_use_yateam_auth(self):
        with self.mock_auth_checks(self.uid, self.login):
            with mock.patch('mpfs.platform.handlers.ServiceProxyHandler.raw_request_service',
                            return_value=(200, {}, {})) as data_api_request_mock:
                self.client.request('GET', 'data/subscriptions/callback', query=self.query, headers=self.base_headers)
                qs = urlparse.urlparse(data_api_request_mock.call_args[0][0]).query
                qs_uid = urlparse.parse_qs(qs)['__uid'][0]
                assert 'yt:' + self.uid == qs_uid


class DataCookieAuthTestCase(DataApiTest):
    """
    Тестируем авторизацию по паспортным кукам
    """

    def setup_method(self, method):
        super(DataCookieAuthTestCase, self).setup_method(method)
        self.uid = self.generate_uid()
        self.login = str(self.uid)

    def test_multi_session_cookie_auth(self):
        base_headers = {
            'Cookie': 'Other=123; Session_id=hernia!_Vse_ravno_mock-aem;',
            'Origin': 'https://disk.yandex.ru',
            'Host': '127.0.0.1',
            'X-Uid': self.uid,
        }
        from mpfs.platform.auth import PassportCookieAuth
        with mock.patch.object(PassportCookieAuth, 'validate_session_cookie') as mock_method:
            mock_method.return_value = [{'uid': self.uid, 'login': self.login}]
            # все хорошо, авторизуем по куке
            resp = self.client.request('GET', 'data/app/databases', headers=base_headers)
            assert resp.status_code == 200

            # uid-а нет в ответе паспорта
            base_headers['X-Uid'] = '123'
            resp = self.client.request('GET', 'data/app/databases', headers=base_headers)
            assert resp.status_code == 401

    def test_cookie_auth(self):
        base_headers = {
            'Cookie': 'Other=123; Session_id=hernia!_Vse_ravno_mock-aem;',
            'Origin': 'https://disk.yandex.ru',
            'Host': '127.0.0.1'
        }

        from mpfs.platform.auth import PassportCookieAuth
        # Куки протухают, поэтому мокаем получение информации по куке
        with mock.patch.object(PassportCookieAuth, 'validate_session_cookie') as mock_method:
            mock_method.return_value = {'uid': self.uid, 'login': self.login}

            # все хорошо, авторизуем по куке
            resp = self.client.request('GET', 'data/app/databases', headers=base_headers)
            assert resp.status_code == 200

            # нет Cookie
            headers = base_headers.copy()
            headers.pop('Cookie')
            resp = self.client.request('GET', 'data/app/databases', headers=headers)
            assert resp.status_code == 401

            # нет Origin
            headers = base_headers.copy()
            headers.pop('Origin')
            resp = self.client.request('GET', 'data/app/databases', headers=headers)
            assert resp.status_code == 401

            # не тот Origin в конфиге
            headers = base_headers.copy()
            headers['Origin'] = 'http://google.com'
            resp = self.client.request('GET', 'data/app/databases', headers=headers)
            assert resp.status_code == 401

            # Нужной куки нет
            headers = base_headers.copy()
            headers['Cookie'] = 'Yandex_cookie=test; Image=rest;'
            resp = self.client.request('GET', 'data/app/databases', headers=headers)
            assert resp.status_code == 401

    def test_cookie_auth_by_regexp(self):
        valid_origins = [
            'https://abc.maps.dev.yandex.ru',
            'https://123.maps.dev.yandex.ru',
            'https://abc.maps.dev.yandex.ru',
            'https://abc.xxx.maps.dev.yandex.ru',
            'https://cloud.maps.yandex.ru',
            'https://123cloud.maps.yandex.ru',
            'https://abccloud.maps.yandex.ru',
            'https://abc.xxxcloud.maps.yandex.ru',
        ]
        invalid_origins = [
            'https://maps.dev.yandex.ru',
            'https://abc.maps1.dev.yandex.ru',
            'https://loud.maps.yandex.ru',
        ]

        fake_settings = {
            'auth': [
                {
                  'name': 'maps_common',
                  'auth_methods': ['cookie'],
                  'enabled': True,
                  'allowed_origin_hosts': ['^.*\.maps\.dev\.yandex\.ru$', '^.*cloud\.maps\.yandex\.ru$'],
                  'oauth_client_id': 'maps_common',
                  'oauth_client_name': 'YaMapsWeb',
                  'oauth_scopes': ['cloud_api:data.app_data', 'cloud_api:data.user_data',
                                   'cloud_api.data:app_data', 'cloud_api.data:user_data'],
                }
            ]
        }

        for origin in valid_origins + invalid_origins:
            base_headers = {
                'Cookie': 'Other=123; Session_id=hernia!_Vse_ravno_mock-aem;',
                'Origin': origin,
                'Host': '127.0.0.1',
            }

            from mpfs.platform.auth import PassportCookieAuth

            # Куки протухают, поэтому мокаем получение информации по куке
            with mock.patch.object(PassportCookieAuth, 'validate_session_cookie') as mock_method:
                mock_method.return_value = {'uid': self.uid, 'login': self.login}

                with mock.patch.dict(settings.platform, fake_settings):
                    with mock.patch.object(DataApiProxyHandler, 'auth_methods', [PassportCookieAuth()]):
                        resp = self.client.request('GET', 'data/app/databases', headers=base_headers)

                        if origin in valid_origins:
                            assert resp.status_code == 200
                        elif origin in invalid_origins:
                            assert resp.status_code == 401


class DataManipulationTestCase(DataApiTest):
    db_id = None

    def setup_method(self, method):
        super(DataManipulationTestCase, self).setup_method(method)
        # Если uid ещё не сгенерирован, то генерируем.
        # Вообще эту процедуру было бы разумнее вынести в setUpClass, если использовать nose-тесты.
        if not self.uid:
            self.uid = self.generate_uid()
            self.login = str(self.uid)
        # создаём тестовую базу
        self.db_id = 'test'
        with self.specified_client(scopes=['cloud_api.data:user_data', 'cloud_api.data:app_data'], uid=self.uid, login=self.login):
            resp = self.client.request('PUT', 'data/user/databases/%s' % self.db_id)
            if resp.status_code == 200:
                # если база почему-то уже откуда-то есть, то убиваем её и создаём заново
                resp = self.client.request('DELETE', 'data/user/databases/%s' % self.db_id)
                self.assertEqual(resp.status_code, 204)

                time.sleep(self.master_slave_sync_delay)  # к сожалению, бэкэнд не сразу осознаёт, что в нём что-то изменили

                resp = self.client.request('PUT', 'data/user/databases/%s' % self.db_id)
            self.assertEqual(resp.status_code, 201)

    def teardown_method(self, method):
        # прибиваем созданную базу
        with self.specified_client(scopes=['cloud_api.data:user_data', 'cloud_api.data:app_data'], uid=self.uid, login=self.login):
            resp = self.client.request('DELETE', 'data/user/databases/%s' % self.db_id)
            self.assertEqual(resp.status_code, 204)
        super(DataManipulationTestCase, self).teardown_method(method)

    def test_request_with_empty_deltas(self):
        # создаём дельту c пустым телом
        with self.specified_client(scopes=['cloud_api.data:user_data', 'cloud_api.data:app_data'], uid=self.uid, login=self.login):
            revision = 0
            headers = {'If-Match': revision}
            resp = self.client.request('POST', 'data/user/databases/%s/deltas' % self.db_id,
                                       data='', headers=headers)
            self.assertEqual(resp.status_code, 400)

    def test_put_delta(self):
        collection_id = 'test'
        record_id = '1'
        binary_field_id = 'binary_field'
        binary_field_value = u'Превед!'
        src_datetime_field_value = '2014-10-23T17:31:12.808328-04:00'
        dst_datetime_field_value = '2014-10-23T21:31:12.808000+00:00'  # обнуляем миллионные доли секунды, т.к. мы не поддерживаем такую точность
        delta = {
            'changes': [
                {
                    'collection_id': collection_id,
                    'record_id': record_id,
                    'change_type': 'insert',
                    'changes': [
                        {
                            'field_id': binary_field_id,
                            'change_type': 'set',
                            'value': {
                                'binary': base64.b64encode(binary_field_value.encode('utf-8')),
                            }
                        },
                        {
                            'field_id': 'string_field',
                            'change_type': 'set',
                            'value': {
                                'string': u'Медвед!',
                            }
                        },
                        {
                            'field_id': 'nan_field',
                            'change_type': 'set',
                            'value': {
                                'nan': True,
                            }
                        },
                        {
                            'field_id': 'list_field',
                            'change_type': 'set',
                            'value': {
                                'list': [
                                    {
                                        'type': 'double',
                                        'double': 123,
                                    },
                                ],
                            }
                        },
                        {
                            'field_id': '-inf_field',
                            'change_type': 'set',
                            'value': {
                                'ninf': True,
                            }
                        },
                        {
                            'field_id': 'double_field',
                            'change_type': 'set',
                            'value': {
                                'double': 123,
                            }
                        },
                        {
                            'field_id': 'boolean_field',
                            'change_type': 'set',
                            'value': {
                                'boolean': False,
                            }
                        },
                        {
                            'field_id': 'datetime_field',
                            'change_type': 'set',
                            'value': {
                                'datetime': src_datetime_field_value,
                            }
                        },
                        {
                            'field_id': 'integer_field',
                            'change_type': 'set',
                            'value': {
                                'integer': 123,
                            }
                        },
                        {
                            'field_id': 'inf_field',
                            'change_type': 'set',
                            'value': {
                                'inf': True,
                            }
                        },
                        {
                            'field_id': 'null_field',
                            'change_type': 'set',
                            'value': {
                                'null': True,
                            }
                        },
                    ]
                },
            ]
        }
        with self.specified_client(scopes=['cloud_api.data:user_data', 'cloud_api.data:app_data'], uid=self.uid, login=self.login):
            revision = 0
            resp = self.client.request('POST', 'data/user/databases/%s/deltas' % self.db_id, data=delta,
                                       headers={'If-Match': revision})
            self.assertEqual(resp.status_code, 201)
            self.assertTrue('ETag' in resp.headers)
            self.assertTrue(resp.headers['ETag'] == revision + 1)

            content = json.loads(resp.content)
            self.assertTrue('href' in content)
            self.assertTrue('revision=1' in content['href'])

            time.sleep(self.master_slave_sync_delay)  # к сожалению, бэкэнд не сразу осознаёт, что в нём что-то изменили

            resp = self.client.request('GET', 'data/user/databases/%s/snapshot' % self.db_id)
            self.assertEqual(resp.status_code, 200)

            content = json.loads(resp.content)
            changes = dict([(f['field_id'], f['value']) for f in delta['changes'][0]['changes']])
            changes['datetime_field']['datetime'] = dst_datetime_field_value
            fields = dict([(f['field_id'], f['value']) for f in content['records']['items'][0]['fields']])
            for c_id, c_val in changes.iteritems():
                f = fields[c_id]
                # ключ в котором мы сохранили значение должен совпадать с возвращаемым типом
                f_type = f['type']
                self.assertEqual(c_val.keys()[0], f_type)
                # сохранённое значение должно совпадать с тем, которое было передано
                f_val = f[f_type]
                self.assertEqual(c_val[f_type], f_val)

            # отдельно проверяем содержимое бинарного поля
            f_val = base64.b64decode(fields[binary_field_id]['binary']).decode('utf-8')
            self.assertEqual(binary_field_value, f_val)

            # пытаемся применить дельту ещё раз
            resp = self.client.request('POST', 'data/user/databases/%s/deltas' % self.db_id, data=delta,
                                       headers={'If-Match': revision + 1})
            self.assertEqual(resp.status_code, 400)

            # пытаемся применить дельту к прошлой ревизии
            resp = self.client.request('POST', 'data/user/databases/%s/deltas' % self.db_id, data=delta,
                                       headers={'If-Match': revision})
            self.assertEqual(resp.status_code, 409)
            self.assertEqual(resp.headers['ETag'], revision + 1)
            content = json.loads(resp.content)
            assert content['base_revision'] == revision
            self.assertEqual(content['total'], 1)
            self.assertEqual(content['total'], len(content['items']))

            # пытаемся применить дельту к более новой версии БД чем она есть на самом деле
            resp = self.client.request('POST', 'data/user/databases/%s/deltas' % self.db_id, data=delta,
                                       headers={'If-Match': revision + 2})
            self.assertEqual(resp.status_code, 412)
            self.assertEqual(resp.headers['ETag'], revision + 1)
            content = json.loads(resp.content)
            self.assertEqual(content['error'], DataDeltaIsNewerThanDatabaseRevisionError.__name__)

            # применяем дельту 20 раз, т.к. бэкэнд в тестинге должен хранить только 10 последних дельт
            delta['changes'][0]['change_type'] = 'update'
            for revision in xrange(1, 20):
                resp = self.client.request('POST', 'data/user/databases/%s/deltas' % self.db_id, data=delta,
                                           headers={'If-Match': revision})
                self.assertEqual(resp.status_code, 201)
            # проверяем ошибку запроса сильно старых дельт
            resp = self.client.request('GET', 'data/user/databases/%s/deltas?base_revision=0' % self.db_id)
            self.assertEqual(resp.status_code, 410)

            # применяем дельту с невалидным иднетификатором коллекции
            d = deepcopy(delta)
            d['changes'][0]['record_id'] = 'test@!'
            d['changes'][0]['change_type'] = 'insert'
            resp = self.client.request('POST', 'data/user/databases/%s/deltas' % self.db_id, data=d,
                                       headers={'If-Match': revision + 1})
            self.assertEqual(resp.status_code, 400)
            self.assertTrue(DataInvalidIdError.__name__ in resp.content)

            # удаляем последовательно все поля
            fields = [f['field_id'] for f in delta['changes'][0]['changes']]
            change = {
                'field_id': '',
                'change_type': 'delete',
            }
            d = deepcopy(delta)
            d['changes'][0]['change_type'] = 'update'
            d['changes'][0]['changes'] = [change]
            for f_id in fields:
                revision += 1
                change['field_id'] = f_id
                resp = self.client.request('POST', 'data/user/databases/%s/deltas' % self.db_id, data=d,
                                           headers={'If-Match': revision})
                self.assertEqual(resp.status_code, 201)

            # удаляем запись
            d = deepcopy(delta)
            d['changes'][0]['change_type'] = 'delete'
            del d['changes'][0]['changes']
            revision += 1
            resp = self.client.request('POST', 'data/user/databases/%s/deltas' % self.db_id, data=d,
                                       headers={'If-Match': revision})
            self.assertEqual(resp.status_code, 201)

    def test_list_item_operations(self):
        collection_id = 'test'
        record_id = '1'
        f_change = {}
        delta = {
            'changes': [
                {
                    'collection_id': collection_id,
                    'record_id': record_id,
                    'change_type': 'insert',
                    'changes': [
                        f_change,
                    ]
                }
            ]
        }
        with self.specified_client(scopes=['cloud_api.data:user_data', 'cloud_api.data:app_data'], uid=self.uid, login=self.login):
            revision = 0
            # добавляем запись со списком
            f_list = [
                {
                    'type': 'double',
                    'double': 123,
                },
                {
                    'type': 'integer',
                    'integer': 321,
                },
            ]
            f_change.update({
                'field_id': 'list_field',
                'change_type': 'set',
                'value': {
                    'list': f_list,
                }
            })
            resp = self.client.request('POST', 'data/user/databases/%s/deltas' % self.db_id, data=delta,
                                       headers={'If-Match': revision})
            self.assertEqual(resp.status_code, 201)
            revision = resp.headers['ETag']
            delta['changes'][0]['change_type'] = 'update'
            del f_change['value']

            # перемещеаем первый элемент на второе место
            f_change.update({
                'change_type': 'list_item_move',
                'list_item': 0,
                'list_item_dest': 1,
            })
            resp = self.client.request('POST', 'data/user/databases/%s/deltas' % self.db_id, data=delta,
                                       headers={'If-Match': revision})
            self.assertEqual(resp.status_code, 201)

            time.sleep(self.master_slave_sync_delay)  # к сожалению, бэкэнд не сразу осознаёт, что в нём что-то изменили

            revision = resp.headers['ETag']
            resp = self.client.request('GET', 'data/user/databases/%s/snapshot' % self.db_id)
            self.assertEqual(resp.status_code, 200)
            content = json.loads(resp.content)
            self.assertEqual(list(reversed(f_list)), content['records']['items'][0]['fields'][0]['value']['list'])

            # пытаемся сделать тоже самое но без указания куда
            del f_change['list_item_dest']
            resp = self.client.request('POST', 'data/user/databases/%s/deltas' % self.db_id, data=delta,
                                       headers={'If-Match': revision})
            self.assertEqual(resp.status_code, 400)
            self.assertTrue(DataFieldListItemMoveWithoutDestError.__name__ in resp.content)

            # пытаемся сделать тоже самое но без указания откуда
            del f_change['list_item']
            resp = self.client.request('POST', 'data/user/databases/%s/deltas' % self.db_id, data=delta,
                                       headers={'If-Match': revision})
            self.assertEqual(resp.status_code, 400)
            self.assertTrue(DataFieldListItemWithoutIndexError.__name__ in resp.content)

            # пытаемся задать значение без указания номера
            f_change.update({
                'change_type': 'list_item_set',
            })
            resp = self.client.request('POST', 'data/user/databases/%s/deltas' % self.db_id, data=delta,
                                       headers={'If-Match': revision})
            self.assertEqual(resp.status_code, 400)
            self.assertTrue(DataFieldListItemWithoutIndexError.__name__ in resp.content)

            # пытаемся задать значение без указания значения
            f_change.update({
                'change_type': 'list_item_set',
                'list_item': 1,
            })
            resp = self.client.request('POST', 'data/user/databases/%s/deltas' % self.db_id, data=delta,
                                       headers={'If-Match': revision})
            self.assertEqual(resp.status_code, 400)
            self.assertTrue(DataFieldSetWithoutValueError.__name__ in resp.content)

            # пытаемся задать значение с указанием всего что надо
            f_change.update({
                'change_type': 'list_item_set',
                'list_item': 1,
                'value': {
                    'double': 100500,
                }
            })
            resp = self.client.request('POST', 'data/user/databases/%s/deltas' % self.db_id, data=delta,
                                       headers={'If-Match': revision})
            self.assertEqual(resp.status_code, 201)

            time.sleep(self.master_slave_sync_delay)  # к сожалению, бэкэнд не сразу осознаёт, что в нём что-то изменили

            resp = self.client.request('GET', 'data/user/databases/%s/snapshot' % self.db_id)
            self.assertEqual(resp.status_code, 200)
            content = json.loads(resp.content)
            self.assertEqual(100500, content['records']['items'][0]['fields'][0]['value']['list'][1]['double'])

    @pytest.mark.skipif(True, reason='https://st.yandex-team.ru/CHEMODAN-32396')
    def test_etag_when_fields_filter_applied(self):
        collection_id = 'test'
        record_id = '1'
        f_change = {}
        delta = {
            'changes': [
                {
                    'collection_id': collection_id,
                    'record_id': record_id,
                    'change_type': 'insert',
                    'changes': [
                        f_change,
                    ]
                }
            ]
        }
        with self.specified_client(scopes=['cloud_api.data:user_data', 'cloud_api.data:app_data'], uid=self.uid, login=self.login):
            revision = 0
            # добавляем запись со списком
            f_change.update({
                'field_id': 'list_field',
                'change_type': 'set',
                'value': {
                    'integer': 10,
                }
            })
            resp = self.client.request('POST', 'data/user/databases/%s/deltas' % self.db_id, data=delta,
                                       headers={'If-Match': revision})
            self.assertEqual(resp.status_code, 201)
            revision = resp.headers['ETag']

            time.sleep(self.master_slave_sync_delay)  # к сожалению, бэкэнд не сразу осознаёт, что в нём что-то изменили

            resp = self.client.request('GET', 'data/user/databases/%s/snapshot' % self.db_id,
                                       query={'fields': 'database_id'})
            self.assertEqual(resp.status_code, 200)
            assert json.loads(resp.content) == {u'database_id': u'test'}
            assert 'ETag' in resp.headers
            assert resp.headers['ETag'] == revision

    def test_cors_headers(self):
        with self.specified_client(scopes=['cloud_api.data:user_data', 'cloud_api.data:app_data'], uid=self.uid, login=self.login):
            resp = self.client.request('GET', 'data/user/databases/%s' % self.db_id)
            assert 'Access-Control-Allow-Origin' in resp.headers
            assert 'Access-Control-Allow-Headers' in resp.headers
            assert 'Access-Control-Expose-Headers' in resp.headers
            assert 'Access-Control-Allow-Credentials' in resp.headers
            assert resp.headers['Access-Control-Allow-Credentials'] == 'true'
            assert 'ETag' in map(str.strip, resp.headers['Access-Control-Expose-Headers'].split(','))
            allow_headers = map(str.strip, resp.headers['Access-Control-Allow-Headers'].split(','))
            assert 'X-Uid' in allow_headers

    def test_cache_control_header(self):
        with self.specified_client(
                scopes=['cloud_api.data:user_data', 'cloud_api.data:app_data'], uid=self.uid, login=self.login):
            resp = self.client.request('GET', 'data/user/databases/%s' % self.db_id)
            assert 'Cache-Control' in resp.headers
            assert resp.headers['Cache-Control'] == 'no-cache'

    def test_origin_header(self):
        with self.specified_client(scopes=['cloud_api.data:user_data', 'cloud_api.data:app_data'], uid=self.uid, login=self.login):
            for method in ('GET', 'OPTIONS'):
                resp = self.client.request(method, 'data/user/databases/%s' % self.db_id, headers={'Origin': 'waser.ru'})
                assert resp.headers['Access-Control-Allow-Origin'] == 'waser.ru'
                resp = self.client.request(method, 'data/user/databases/%s' % self.db_id)
                assert resp.headers['Access-Control-Allow-Origin'] == '*'

    def test_snapshot_filter_by_collection_id(self):
        """Проверка фильтрации снапшота по collection_id."""
        delta = {
            'changes': [
                {
                    'collection_id': None,
                    'record_id': '1',
                    'change_type': 'insert',
                    'changes': [
                        {
                            'field_id': 'list_field',
                            'change_type': 'set',
                            'value': {
                                'integer': 10,
                            }
                        }
                    ]
                }
            ]
        }
        with self.specified_client(scopes=['cloud_api:data.user_data', 'cloud_api:data.app_data'], uid=self.uid, login=self.login):
            revision = 0
            collection_id_1 = 'collection_1'
            collection_id_2 = 'collection_2'
            for collection_id in (collection_id_1, collection_id_2):
                delta['changes'][0]['collection_id'] = collection_id
                resp = self.client.request('POST', 'data/user/databases/%s/deltas' % self.db_id, data=delta,
                                           headers={'If-Match': revision})
                self.assertEqual(resp.status_code, 201)
                revision = resp.headers['ETag']

            # убеждаемся что у нас 2 коллекции в снапшоте
            resp = self.client.request('GET', 'data/user/databases/%s/snapshot' % self.db_id)
            self.assertEqual(resp.status_code, 200)
            snapshot = json.loads(resp.content)
            collection_ids = set([r['collection_id'] for r in snapshot['records']['items']])
            assert collection_id_1 in collection_ids
            assert collection_id_2 in collection_ids

            # проверяем фильтр по collection_id
            resp = self.client.request('GET', 'data/user/databases/%s/snapshot' % self.db_id,
                                       query={'collection_id': collection_id_1})
            self.assertEqual(resp.status_code, 200)
            snapshot = json.loads(resp.content)
            collection_ids = set([r['collection_id'] for r in snapshot['records']['items']])
            assert collection_id_1 in collection_ids
            assert collection_id_2 not in collection_ids

    def test_empty_deltas_list(self):
        """
        При запросе пустого списка дельт API не должно падать.
        https://st.yandex-team.ru/CHEMODAN-27781
        """
        with self.specified_client(scopes=['cloud_api:data.user_data', 'cloud_api:data.app_data'], uid=self.uid, login=self.login):
            resp = self.client.get('data/user/databases/%s/deltas?base_revision=0' % self.db_id)
            assert resp.status_code == 200
            assert json.loads(resp.content)['items'] == []


class ProtobufTestCase(DataApiTest):
    db_id = None
    headers = {'Accept': 'application/protobuf', 'Content-Type': 'application/protobuf'}

    def setup_method(self, method):
        super(ProtobufTestCase, self).setup_method(method)
        # Если uid ещё не сгенерирован, то генерируем.
        # Вообще эту процедуру было бы разумнее вынести в setUpClass, если использовать nose-тесты.
        if not self.uid:
            self.uid = self.generate_uid()
            self.login = str(self.uid)
        # создаём тестовую базу
        self.db_id = 'test'

    @pytest.mark.skipif(True, reason='https://st.yandex-team.ru/CHEMODAN-32396')
    def test_normal_behaviour(self):
        with self.specified_client(scopes=['cloud_api.data:user_data', 'cloud_api.data:app_data'], uid=self.uid, login=self.login):
            # создаём базу
            resp = self.client.request('PUT', 'data/user/databases/%s' % self.db_id, headers=self.headers)
            self.assertEqual(resp.status_code, 201)
            db = data_pb2.Database.FromString(resp.content)
            self.assertTrue(isinstance(db.created, long))
            self.assertTrue(isinstance(db.modified, long))

            time.sleep(self.master_slave_sync_delay)  # к сожалению, бэкэнд не сразу осознаёт, что в нём что-то изменили

            # получаем список баз
            resp = self.client.request('GET', 'data/user/databases/', headers=self.headers)
            self.assertEqual(resp.status_code, 200)
            db_list = data_pb2.DatabaseList.FromString(resp.content)
            self.assertEqual(len(db_list.items), 1)

            # получаем одну базу
            resp = self.client.request('GET', 'data/user/databases/%s' % self.db_id, headers=self.headers)
            self.assertEqual(resp.status_code, 200)
            db = data_pb2.Database.FromString(resp.content)

            # создаём дельту
            revision = 0
            collection_id = 'test'
            record_id = '1'
            delta = data_pb2.Delta()
            r_change = delta.changes.add()
            r_change.collection_id = collection_id
            r_change.record_id = record_id
            r_change.change_type = RecordChange.INSERT
            f_change = r_change.changes.add()
            f_change.field_id = 'string_field'
            f_change.change_type = FieldChange.SET
            f_change.value.string = u'Медвед!'
            headers = {'If-Match': revision}
            headers.update(self.headers)
            resp = self.client.request('POST', 'data/user/databases/%s/deltas' % self.db_id,
                                       data=delta.SerializeToString(), headers=headers)
            self.assertEqual(resp.status_code, 201)
            lnk = data_pb2.Link.FromString(resp.content)

            time.sleep(self.master_slave_sync_delay)  # к сожалению, бэкэнд не сразу осознаёт, что в нём что-то изменили

            # пытаемся ещё раз применить дельту к старой ревизии
            resp = self.client.request('POST', 'data/user/databases/%s/deltas' % self.db_id,
                                       data=delta.SerializeToString(), headers=headers)
            self.assertEqual(resp.status_code, 409)
            deltas = data_pb2.DeltaList.FromString(resp.content)

            # получаем список дельт
            resp = self.client.request('GET', 'data/user/databases/%s/deltas' % self.db_id,
                                       query={'base_revision': revision}, headers=headers)
            self.assertEqual(resp.status_code, 200)
            deltas = data_pb2.DeltaList.FromString(resp.content)

            # получаем снапшот базы
            resp = self.client.request('GET', 'data/user/databases/%s/snapshot' % self.db_id, headers=headers)
            self.assertEqual(resp.status_code, 200)
            snapshot = data_pb2.DatabaseSnapshot.FromString(resp.content)

            # прибиваем созданную базу
            resp = self.client.request('DELETE', 'data/user/databases/%s' % self.db_id, headers=self.headers)
            self.assertEqual(resp.status_code, 204)

    def test_binary_field(self):
        with self.specified_client(scopes=['cloud_api.data:user_data', 'cloud_api.data:app_data'], uid=self.uid, login=self.login):
            # создаём базу
            resp = self.client.request('PUT', 'data/user/databases/%s' % self.db_id, headers=self.headers)
            self.assertEqual(resp.status_code, 201)

            # создаём дельту
            revision = 0
            collection_id = 'test'
            record_id = 'binary_data_test_record'
            binary_data = u'Медвед!'.encode('utf-8')
            delta = data_pb2.Delta()
            r_change = delta.changes.add()
            r_change.collection_id = collection_id
            r_change.record_id = record_id
            r_change.change_type = RecordChange.INSERT
            f_change = r_change.changes.add()
            f_change.field_id = 'binary_field'
            f_change.change_type = FieldChange.SET
            f_change.value.binary = binary_data
            headers = {'If-Match': revision}
            headers.update(self.headers)
            resp = self.client.request('POST', 'data/user/databases/%s/deltas' % self.db_id,
                                       data=delta.SerializeToString(), headers=headers)
            self.assertEqual(resp.status_code, 201)

            time.sleep(self.master_slave_sync_delay)  # к сожалению, бэкэнд не сразу осознаёт, что в нём что-то изменили

            # достаём содержимое поля и проверяем, что оно идентично тому, что отправляли
            # получаем снапшот базы
            resp = self.client.request('GET', 'data/user/databases/%s/snapshot' % self.db_id, headers=self.headers)
            self.assertEqual(resp.status_code, 200)
            snapshot = data_pb2.DatabaseSnapshot.FromString(resp.content)
            self.assertEqual(snapshot.records.items[0].fields[0].value.binary, binary_data)

    def test_empty_lists_in_response(self):
        """
        Проверяем не подавится ли протобуф пустыми списками в ответах

        https://jira.yandex-team.ru/browse/CHEMODAN-19208
        """
        with self.specified_client(scopes=['cloud_api.data:user_data', 'cloud_api.data:app_data'], uid=self.uid, login=self.login):
            # получаем пустой список баз
            resp = self.client.request('GET', 'data/user/databases/', headers=self.headers)
            self.assertEqual(resp.status_code, 200)
            db_list = data_pb2.DatabaseList.FromString(resp.content)
            self.assertEqual(len(db_list.items), 0)

            # создаём базу
            resp = self.client.request('PUT', 'data/user/databases/%s' % self.db_id, headers=self.headers)
            self.assertEqual(resp.status_code, 201)

            time.sleep(self.master_slave_sync_delay)  # к сожалению, бэкэнд не сразу осознаёт, что в нём что-то изменили

            # получаем снапшот пустой базы
            resp = self.client.request('GET', 'data/user/databases/%s/snapshot' % self.db_id, headers=self.headers)
            self.assertEqual(resp.status_code, 200)
            snapshot = data_pb2.DatabaseSnapshot.FromString(resp.content)
            self.assertEqual(len(snapshot.records.items), 0)

            # получаем пустой список дельт
            resp = self.client.request('GET', 'data/user/databases/%s/deltas?base_revision=%s' % (self.db_id, 0),
                                       headers=self.headers)
            self.assertEqual(resp.status_code, 200)
            snapshot = data_pb2.DeltaList.FromString(resp.content)
            self.assertEqual(len(snapshot.items), 0)

    def test_body_logging_when_500_from_data_api(self):
        """Проверяем логирование тела запроса при 500 от Data API"""
        with mock.patch('mpfs.platform.v1.data.handlers.FEATURE_TOGGLES_LOG_PROTOBUF_BODY_ON_DATA_API_ERRORS', True),\
             self.specified_client(scopes=['cloud_api.data:user_data',
                                           'cloud_api.data:app_data'], uid=self.uid, login=self.login),\
             DataApiStub(open_url_mock_kwargs={'side_effect': DataApiStub.data_api_open_url_with_500_on_put_with_data,
                                              'autospec': True}), \
             mock.patch('mpfs.frontend.api.auth.error_log.error') as mocked_error_log:
            # создаём базу
            resp = self.client.request('PUT', 'data/user/databases/%s' % self.db_id, headers=self.headers)
            self.assertEqual(resp.status_code, 201)

            time.sleep(self.master_slave_sync_delay)  # к сожалению, бэкэнд не сразу осознаёт, что в нём что-то изменили

            # создаём дельту
            delta = '\n,\x08\x01\x12\x0etestCollection\x1a\ntestRecord"\x0c\x08\x02\x12\x04list\x1a\x02h\x0b\x12\ntestClient\x18\x00'
            expected_logged_data = base64.b64encode(delta)
            headers = {'If-Match': 0}
            headers.update(self.headers)
            resp = self.client.request('POST', 'data/user/databases/%s/deltas' % self.db_id, data=delta,
                                       headers=headers)
            self.assertEqual(resp.status_code, 500)
            # В error лог должны были залогировать body в base64
            assert_log_contains(mocked_error_log, expected_logged_data)

    def test_empty_lists_in_request(self):
        """
        Проверяем не подавится ли протобуф пустыми списками в запросах

        https://st.yandex-team.ru/CHEMODAN-21340
        """
        with self.specified_client(scopes=['cloud_api.data:user_data', 'cloud_api.data:app_data'], uid=self.uid, login=self.login):
            # создаём базу
            resp = self.client.request('PUT', 'data/user/databases/%s' % self.db_id, headers=self.headers)
            self.assertEqual(resp.status_code, 201)

            time.sleep(self.master_slave_sync_delay)  # к сожалению, бэкэнд не сразу осознаёт, что в нём что-то изменили

            # создаём дельту
            revision = 0
            collection_id = 'testCollection'
            record_id = 'testRecord'
            delta = '\n,\x08\x01\x12\x0e%s\x1a\n%s"\x0c\x08\x02\x12\x04list\x1a\x02h\x0b\x12\ntestClient\x18\x00' % (
                collection_id, record_id
            )
            headers = {'If-Match': revision}
            headers.update(self.headers)
            resp = self.client.request('POST', 'data/user/databases/%s/deltas' % self.db_id, data=delta,
                                       headers=headers)
            self.assertEqual(resp.status_code, 201)

            time.sleep(self.master_slave_sync_delay)  # к сожалению, бэкэнд не сразу осознаёт, что в нём что-то изменили

            # достаём содержимое поля и проверяем, что там пустой список
            # получаем снапшот базы
            resp = self.client.request('GET', 'data/user/databases/%s/snapshot' % self.db_id, headers=self.headers)
            self.assertEqual(resp.status_code, 200)
            snapshot = data_pb2.DatabaseSnapshot.FromString(resp.content)
            assert len(snapshot.records.items) == 1
            assert list(snapshot.records.items[0].fields[0].value.list) == []
            assert snapshot.records.items[0].fields[0].value.type == data_pb2.Value.LIST

    def test_errors(self):
        with self.specified_client(scopes=['cloud_api.data:user_data', 'cloud_api.data:app_data'], uid=self.uid, login=self.login):
            # создаём базу в несуществующем контексте
            # проверяем обработку ошибки выброшенной ещё в диспатчере при поиске хэндлера
            resp = self.client.request('PUT', 'datsda/dsaf/databases/%s' % self.db_id, headers=self.headers)
            self.assertEqual(resp.status_code, 404)
            error = data_pb2.Error.FromString(resp.content)
            self.assertEqual(error.error, 'NotFoundError')
            self.assertEqual(error.description, 'Not Found')

            # создаём базу
            resp = self.client.request('PUT', 'data/user/databases/%s' % self.db_id, headers=self.headers)
            self.assertEqual(resp.status_code, 201)
            db = data_pb2.Database.FromString(resp.content)

            # создаём дельту с 2мя значениями для одного поля
            # проверяем обработку ошибки выброшенную в процессе работы хэндлера
            revision = 0
            collection_id = 'test'
            record_id = '1'
            delta = data_pb2.Delta()
            r_change = delta.changes.add()
            r_change.collection_id = collection_id
            r_change.record_id = record_id
            r_change.change_type = 1
            f_change = r_change.changes.add()
            f_change.field_id = 'string_field'
            f_change.change_type = 2
            f_change.value.string = u'Медвед!'
            f_change.value.boolean = True
            headers = {'If-Match': revision}
            headers.update(self.headers)
            resp = self.client.request('POST', 'data/user/databases/%s/deltas' % self.db_id,
                                       data=delta.SerializeToString(), headers=headers)
            self.assertEqual(resp.status_code, 400)
            error = data_pb2.Error.FromString(resp.content)
            self.assertEqual(error.error, 'DataValueObjectProvideMultipleValuesError')
            self.assertEqual(error.description, 'Multiple values provided in Value object.')

    def test_GONE_error(self):
        with self.specified_client(scopes=['cloud_api.data:user_data', 'cloud_api.data:app_data'], uid=self.uid,
                                   login=self.login):
            # Формируем ошибку 410 GONE
            resp_error = DataApiBadResponse()
            resp_error.data = {
                'text': '\n\x0bdeltas-gone\x12\x00\x1a9ru.yandex.chemodan.app.dataapi.web.DeltasGoneException: \n',
                'code': 7,
                'headers': {}
            }
            with mock.patch('mpfs.platform.v1.data.handlers.DataApiProxyHandler.raw_request_service',
                            side_effect=resp_error):
                resp = self.client.request('GET',
                                           'data/user/databases/%s/deltas?base_revision=322' % self.db_id,
                                           headers=self.headers)

            self.assertEqual(resp.status_code, 410)
            error = data_pb2.Error.FromString(resp.content)
            self.assertEqual(error.error, 'DataDeltasForRevisionIsGoneError')
            assert 'Requested revision is too old and deltas for it is gone.' in error.description


class ListAppUsersTestCase(DataApiTest):
    api_mode = tags.platform.INTERNAL

    def create_database(self, client_id, uid, db_id):
        with self.specified_client(id=client_id, uid=uid):
            resp = self.client.request('PUT', '%i/data/app/databases/%s' % (uid, db_id))
            self.assertIn(resp.status_code, [200, 201])

    @pytest.mark.skipif(True, reason='https://st.yandex-team.ru/CHEMODAN-32396')
    def test_normal_behaviour(self):
        app = 'test-users-list'

        self.create_database(app, 1, 'db1')
        self.create_database(app, 1, 'db2')
        self.create_database(app, 2, 'db1')
        self.create_database(app, 3, 'db1')
        self.create_database(app, 3, 'db2')

        time.sleep(self.master_slave_sync_delay)  # к сожалению, бэкэнд не сразу осознаёт, что в нём что-то изменили

        with self.specified_client(id=app):
            resp = self.client.request('GET', '0/data/users', query={'limit': 2})
            self.assertEqual(resp.status_code, 200)

            res = from_json(resp.result)
            self.assertEqual(2, res['limit'])
            self.assertEqual(2, len(res['items']))
            self.assertEqual('1', res['items'][0]['uid'])
            self.assertEqual('2', res['items'][1]['uid'])
            self.assertIn('iteration_key', res)

            resp = self.client.request('GET', '0/data/users', query={'iteration_key': res['iteration_key']})
            self.assertEqual(resp.status_code, 200)

            res = from_json(resp.result)
            self.assertEqual(1, len(res['items']))
            self.assertNotIn('iteration_key', res)

            resp = self.client.request('GET', '0/data/users', query={'database_id': 'db2', 'limit': 2})
            self.assertEqual(resp.status_code, 200)

            res = from_json(resp.result)
            self.assertEqual('1', res['items'][0]['uid'])
            self.assertEqual('3', res['items'][1]['uid'])
            self.assertIn('iteration_key', res)

    def test_errors(self):
        with self.specified_client():
            resp = self.client.request('GET', '1/data/users', query={'limit': 100500})
            self.assertEqual(resp.status_code, 400)


class GetWebSubscriptionUrlTestCase(DataApiTest):
    api_mode = tags.platform.INTERNAL

    def setup_method(self, method):
        super(GetWebSubscriptionUrlTestCase, self).setup_method(method)
        self.uid = self.generate_uid()
        self.login = str(self.uid)

    def test_web_normal_behaviour(self):
        app = 'test-xiva-subscription-web'
        query = {
            'session': 'session1',
            'databases_ids': 'db1,db2'
        }
        headers = {'Authorization': 'Internal client_id=123;client_name=456'}

        with self.specified_client(id=app, uid=self.uid, login=self.login):
            resp = self.client.request('GET', 'data/subscriptions/web', query=query, headers=headers)

            self.assertEqual(resp.status_code, 200)

            res = from_json(resp.result)

            self.assertEqual('GET', res['method'])
            self.assertEqual(False, res['templated'])

            assert 'href' in res
            assert '/v1/subscribe?' in res['href']
            assert 'uid=%s' % self.uid in res['href']
            assert 'client=%s' % app in res['href']
            assert 'session=session1' in res['href']
            assert 'service=datasync%3Adb1%2Bdb2' in res['href']

    @parameterized.expand([
        ('disk.yandex.ru', 'ru'),
        ('disk.yandex.com.tr', 'com.tr'),
        ('disk.yandex.hack', 'ru'),  # default-значение tld
    ])
    def test_web_tld(self, host, tld):
        app = 'test-xiva-subscription-web'
        query = {
            'session': 'session1',
            'databases_ids': 'db1,db2'
        }
        headers = {'Authorization': 'Internal client_id=123;client_name=456', 'Host': host}

        with self.specified_client(id=app, uid=self.uid, login=self.login):
            resp = self.client.request('GET', 'data/subscriptions/web', query=query, headers=headers)

            self.assertEqual(resp.status_code, 200)

            res = from_json(resp.result)
            assert '.%s/v1/subscribe?' % tld in res['href']

    def test_callback_normal_behaviour(self):
        app = 'test-xiva-subscription-url'

        with self.specified_client(id=app, uid=self.uid, login=self.login):
            resp = self.client.request('GET', '%s/data/subscriptions/callback' % self.uid,
                                       query={'client_token': 'ctoken1',
                                              'callback': 'callback_url_1',
                                              'session': 'session1',
                                              'databases_ids': 'db1,db2'},
                                       headers={'Authorization': 'Internal client_id=123;client_name=456'})
            self.assertEqual(resp.status_code, 200)

            res = from_json(resp.result)
            self.assertEqual('GET', res['method'])
            self.assertEqual(False, res['templated'])
            assert 'href' in res
            assert '/v1/subscribe/url?' in res['href']
            assert 'ctoken=ctoken1' in res['href']
            assert 'uid=%s' % self.uid in res['href']
            assert 'callback=callback_url_1' in res['href']
            assert 'session=session1' in res['href']
            assert 'service=datasync%3Adb1%2Bdb2' in res['href']

    @parameterized.expand([
        ('disk.yandex.ru', 'ru'),
        ('disk.yandex.com.tr', 'com.tr'),
        ('disk.yandex.hack', None),
    ])
    def test_callback_tld(self, host, tld):
        query = {
            'client_token': 'ctoken1',
            'callback': 'callback_url_1',
            'session': 'session1',
            'databases_ids': 'db1,db2'
        }
        headers = {'Authorization': 'Internal client_id=123;client_name=456', 'Host': host}

        with self.specified_client(id='id', uid=self.uid, login=self.login):
            with mock.patch('mpfs.platform.handlers.ServiceProxyHandler.raw_request_service',
                            return_value=(200, {}, {})) as data_api_request_mock:
                self.client.request('GET', 'data/subscriptions/callback', query=query, headers=headers)
        qs = urlparse.urlparse(data_api_request_mock.call_args[0][0]).query
        q = urlparse.parse_qs(qs)
        if tld:
            assert q['tld'][0] == tld
        else:
            assert 'tld' not in q


class CreateAppSubscriptionHandlerTestCase(UserTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.EXTERNAL
    api_version = 'v1'
    query = {
        'databases_ids': 'db1,db2',
        'app_name': 'test_app_name',
        'platform': 'gcm',
        'registration_token': 'test_client_registration_token',
        'app_instance_id': 'test_app_instance_uuid'
    }
    service_name = 'datasync'

    def test_xiva_url_correct_structure(self):
        with self.specified_client(uid=self.uid, login=self.login, scopes=['yadisk:all']):
            with PushServicesStub() as stub:
                self.client.put(
                    'data/subscriptions/app',
                    query=self.query,
                )
                args, kwargs = stub.subscribe_app_common.call_args

                assert args == (
                    "%s:%s" % (self.service_name, self.query['databases_ids'].replace(',', '+')),
                    self.query['app_instance_id'],
                    self.query['registration_token'],
                    self.query['app_name'],
                    self.query['platform'],
                )
                assert kwargs == {
                    'uid':  self.uid,
                    'filter_': None,
                    'device_id': None,
                }

    def test_xiva_non_200_status_code_transformed_to_500(self):
        with self.specified_client(uid=self.uid, login=self.login, scopes=['yadisk:all']):
            with PushServicesStub() as stub:
                stub.subscribe_app_common.side_effect = Exception
                response = self.client.put(
                    'data/subscriptions/app',
                    query={
                        'databases_ids': 'db1,db2',
                        'app_name': 'test_app_name',
                        'platform': 'gcm',
                        'registration_token': 'test_platform_push_token',
                        'app_instance_id': 'test_app_instance_uuid'
                    }
                )
                assert response.status_code == 500

    @pytest.mark.skipif(True, reason='https://st.yandex-team.ru/CHEMODAN-32396')
    def test_xiva_200(self):
        with self.specified_client(uid=self.uid, login=self.login, scopes=['yadisk:all']):
            with PushServicesStub():
                response = self.client.put(
                    'data/subscriptions/app',
                    query={
                        'databases_ids': 'db1,db2',
                        'app_name': 'test_app_name',
                        'platform': 'gcm',
                        'registration_token': 'test_platform_push_token',
                        'app_instance_id': 'test_app_instance_uuid'
                    },
                    headers={'Accept': 'application/json'}
                )
                assert response.status_code == 201
                assert response.headers.get('Content-Type') == 'application/json; charset=utf-8'
                data = from_json(response.content)
                assert 'subscription_id' in data
                subscription_data = from_json(base64.b64decode(data['subscription_id']))
                assert 'push_token' in subscription_data
                assert subscription_data['push_token'] == 'test_platform_push_token'
                assert 'service' in subscription_data
                assert subscription_data['service'] == 'datasync:db1+db2'
                assert 'uid' in subscription_data
                assert subscription_data['uid'] == str(self.uid)
                assert 'uuid' in subscription_data
                assert subscription_data['uuid'] == 'test_app_instance_uuid'

    def test_xiva_forbidden_symbols_replaced_with_underscore(self):
        """Протестировать что неподдерживаемые символы для тегов Xiva при подписке на базы Датасинка
        заменяются на нижнее подчеркивание."""
        # https://st.yandex-team.ru/CHEMODAN-32521
        with self.specified_client(uid=self.uid, login=self.login, scopes=['yadisk:all']):
            with PushServicesStub() as stub:
                query = deepcopy(self.query)
                query['databases_ids'] = '.ext.lenta@lenta_blocks'  # @ в названии!
                self.client.put(
                    'data/subscriptions/app',
                    query=query
                )
                args, kwargs = stub.subscribe_app_common.call_args
                assert len(args) == 5
                service, uuid, push_token, app_name, platform = args
                assert service == 'datasync:.ext.lenta_lenta_blocks'

    def test_valid_xiva_token(self):
        """Проверка что для xiva используется нужные сервис с правильным токеном"""
        def subscribe_app_stub(instance, *args, **kwargs):
            token = settings.services['XivaSubscribeDataSyncService']['token']
            assert instance.token == token

        with self.specified_client(uid=self.uid, login=self.login, scopes=['yadisk:all']):
            with mock.patch(
                    'mpfs.core.services.push_service.XivaSubscribeCommonService.subscribe_app',
                    new=subscribe_app_stub):
                response = self.client.put(
                    'data/subscriptions/app',
                    query=self.query.copy()
                )
                assert response.status_code == 201

    def test_xiva_url_special_access(self):
        query = copy(self.query)
        with self.specified_client(uid=self.uid, id='dummy_client_id'):
            with PushServicesStub() as stub:
                resp = self.client.put(
                    'data/subscriptions/app',
                    query=query,
                )
                assert resp.status_code == 201

        # неизвестный клиент
        query['databases_ids'] = 'db1,db2'
        with self.specified_client(uid=self.uid, id='dummy_client_id_unknown'):
            with PushServicesStub() as stub:
                resp = self.client.put(
                    'data/subscriptions/app',
                    query=query,
                )
                assert resp.status_code == 403

        # не разрешенная в конфиге БД
        query['databases_ids'] = 'db1,db60'
        with self.specified_client(uid=self.uid, id='dummy_client_id'):
            with PushServicesStub() as stub:
                resp = self.client.put(
                    'data/subscriptions/app',
                    query=query,
                )
                assert resp.status_code == 403


class DeleteAppSubscriptionHandlerTestCase(UserTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.EXTERNAL
    api_version = 'v1'
    url = 'data/subscriptions/app/%s'

    def setup_method(self, method):
        super(DeleteAppSubscriptionHandlerTestCase, self).setup_method(method)

    def test_subscription_id_base64_encoded(self):
        subsctiption_id = 'non_base64_encoded_string'
        with self.specified_client():
            response = self.client.delete(self.url % subsctiption_id)
            assert response.status_code == 400

    def test_subscription_id_is_not_json_dumped_string(self):
        subscription_id = base64.b64encode('non_json_string')
        with self.specified_client():
            response = self.client.delete(self.url % subscription_id)
            assert response.status_code == 400

    def test_subscription_id_without_required_param_inside_json(self):
        for key in ('uid', 'uuid', 'push_token', 'service'):
            data = {
                'uid': str(self.uid),
                'uuid': 'test_app_instance_id',
                'push_token': 'test_push_token',
                'service': 'datasync'
            }
            data.pop(key)
            subscription_id = base64.b64encode(to_json(data))
            with self.specified_client():
                response = self.client.delete(self.url % subscription_id)
                assert response.status_code == 400

    def test_xiva_url_has_correct_structure_with_correct_subscription(self):
        push_token = 'test_push_token'
        service = 'datasync'
        app_instance_id = 'test_app_instance_uuid'
        data = {
            'uid': str(self.uid),
            'uuid': app_instance_id,
            'push_token': push_token,
            'service': service
        }
        subscription_id = base64.b64encode(to_json(data))
        with self.specified_client():
            with PushServicesStub() as stub:
                self.client.delete(self.url % subscription_id)
                args, kwargs = stub.unsubscribe_app_common.call_args
                assert args[0] == 'datasync'  # ATTENTION: Без тегов!
                assert args[1] == app_instance_id
                assert kwargs['push_token'] == push_token
                assert kwargs['uid'] == str(self.uid)

    def test_xiva_200(self):
        push_token = 'test_push_token'
        service = 'datasync'
        app_instance_id = 'test_app_instance_uuid'
        data = {
            'uid': str(self.uid),
            'uuid': app_instance_id,
            'push_token': push_token,
            'service': service
        }
        subscription_id = base64.b64encode(to_json(data))
        with self.specified_client():
            with PushServicesStub() as stub:
                response = self.client.delete(self.url % subscription_id)
                assert response.status_code == 204

    def test_service_name_with_tags_cuted_to_name(self):
        push_token = 'test_push_token'
        service = 'datasync'
        app_instance_id = 'test_app_instance_uuid'
        data = {
            'uid': str(self.uid),
            'uuid': app_instance_id,
            'push_token': push_token,
            'service': service
        }
        subscription_id = base64.b64encode(to_json(data))
        with self.specified_client():
            with PushServicesStub() as stub:
                self.client.delete(self.url % subscription_id)
                args, kwargs = stub.unsubscribe_app_common.call_args
                assert args[0] == 'datasync'

    def test_service_name_without_tags_stayed_the_same(self):
        push_token = 'test_push_token'
        service = 'datasync'
        app_instance_id = 'test_app_instance_uuid'
        data = {
            'uid': str(self.uid),
            'uuid': app_instance_id,
            'push_token': push_token,
            'service': service
        }
        subscription_id = base64.b64encode(to_json(data))
        with self.specified_client():
            with PushServicesStub() as stub:
                self.client.delete(self.url % subscription_id)
                args, kwargs = stub.unsubscribe_app_common.call_args
                assert args[0] == 'datasync'

    def test_xiva_non_200_status_code_transformed_to_500(self):
        push_token = 'test_push_token'
        service = 'datasync'
        app_instance_id = 'test_app_instance_uuid'
        data = {
            'uid': str(self.uid),
            'uuid': app_instance_id,
            'push_token': push_token,
            'service': service
        }
        subscription_id = base64.b64encode(to_json(data))
        with self.specified_client():
            with PushServicesStub() as stub:
                stub.unsubscribe_app_common.side_effect = Exception
                response = self.client.delete(self.url % subscription_id)
                assert response.status_code == 500


class AppSubscriptionsNotificationsHandlersTestCase(UserTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.EXTERNAL
    api_version = 'v1'
    subs_url = 'data/subscriptions/app'
    service_name = 'datasync'
    query = {
        'databases_ids': 'db1,db2',
        'app_name': 'test_app_name',
        'platform': 'gcm',
        'registration_token': 'test_platform_push_token',
        'app_instance_id': 'test_app_instance_uuid'
    }

    def test_common(self):
        """Проверяем рабочий workflow
        """
        with self.specified_client(uid=self.uid, login=self.login, scopes=['yadisk:all']):
            # подписываемся
            with PushServicesStub() as stub:
                response = self.client.put(self.subs_url, query=self.query.copy())
                assert response.status_code == 201
                # проверяем верность запросов в Xiva
                assert len(stub.subscribe_app_common.call_args_list) == 1
                args, kwargs = stub.subscribe_app_common.call_args_list[0]
                assert args == (
                    "%s:%s" % (self.service_name, self.query['databases_ids'].replace(',', '+')),
                    self.query['app_instance_id'],
                    self.query['registration_token'],
                    self.query['app_name'],
                    self.query['platform'],
                )
                assert kwargs == {
                    'uid':  self.uid,
                    'filter_': None,
                    'device_id': None,
                }
                # извлекаем ключ подписки
                json_resp = from_json(response.content)
                assert 'subscription_id' in json_resp
                subscription_id = json_resp['subscription_id']

            # идем с ключем отписываться
            with PushServicesStub() as stub:
                response = self.client.delete(self.subs_url + '/%s' % subscription_id)
                assert response.status_code == 204
                # проверяем верность запросов в Xiva
                assert len(stub.unsubscribe_app_common.call_args_list) == 1
                args, kwargs = stub.unsubscribe_app_common.call_args_list[0]
                assert args == (
                    self.service_name,
                    self.query['app_instance_id'],
                )
                assert kwargs == {
                    'push_token': self.query['registration_token'],
                    'uid':  self.uid,
                }
