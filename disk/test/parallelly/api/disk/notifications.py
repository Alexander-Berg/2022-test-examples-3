# -*- coding: utf-8 -*-
import authparser
import mock
import pytest

from nose_parameterized import parameterized

from test.parallelly.api.disk.base import DiskApiTestCase
from test.base_suit import UserTestCaseMixin, UploadFileTestCaseMixin
from test.helpers.stubs.services import PushServicesStub
from mpfs.common.util import from_json
from mpfs.common.static import tags
from mpfs.config import settings
from mpfs.platform.auth import PassportCookieAuth
from mpfs.core.services.passport_service import blackbox


class ResourceNotificationsHandlersTestCase(UserTestCaseMixin, UploadFileTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.EXTERNAL
    api_version = 'v1'
    headers = {
        'Origin': 'https://disk.yandex.ru',
        'Cookie': 'Other=123; Session_id=vse_ravno_mock-aem;',
        'Host': '127.0.0.1'
    }

    def __init__(self, *args, **kwargs):
        super(ResourceNotificationsHandlersTestCase, self).__init__(*args, **kwargs)
        self.check_session_id_ret_val = {
            'uid': self.uid,
            'login': self.login,
            'status': PassportCookieAuth.good_passport_statuses[0],
        }

    def setup_method(self, method):
        super(ResourceNotificationsHandlersTestCase, self).setup_method(method)

        self.create_user(self.uid, noemail=1)

        self.file_path = '/disk/test.txt'
        self.upload_file(self.uid, self.file_path)
        self.file_info = self.json_ok('info', {'uid': self.uid, 'path': self.file_path, 'meta': ''})

    @parameterized.expand([('/yadisk_web/v1/disk/resources/%s/notifications/enable',),
                           ('/yadisk_web/v1/disk/resources/%s/notifications/disable',)])
    def test_normal_behaviour(self, url):
        with mock.patch.object(blackbox, 'check_session_id') as check_session_id, \
                mock.patch.object(authparser.Session, 'parse', create=True) as session_parse:
            check_session_id.return_value = self.check_session_id_ret_val
            session_parse.return_value = 1

            self.response = self.client.put(url % self.file_info['meta']['resource_id'], headers=self.headers)
            assert self.response.status_code == 200
            assert self.response.content == '{}'

    def test_settings_handler(self):
        with mock.patch.object(blackbox, 'check_session_id') as check_session_id, \
                mock.patch.object(authparser.Session, 'parse', create=True) as session_parse:
            check_session_id.return_value = self.check_session_id_ret_val
            session_parse.return_value = 1

            base_url = '/yadisk_web/v1/disk/resources/%s/notifications' % self.file_info['meta']['resource_id']
            response = self.client.put(base_url + '/enable', headers=self.headers)
            assert response.status_code == 200
            response = self.client.get(base_url + '/settings', headers=self.headers)
            assert response.status_code == 200

    def test_notifications_hidden_from_external_swagger(self):
        resp = self.client.get('schema/resources/v1/disk/resources')
        assert 'notifications/settings' not in resp.content
        assert 'notifications/enable' not in resp.content
        assert 'notifications/disable' not in resp.content


class NotificationsVisibleForInternalSwaggerTestCase(DiskApiTestCase):
    api_mode = tags.platform.INTERNAL
    api_version = 'v1'

    def test_hidden_endpoint_visible_for_internal_swagger(self):
        resp = self.client.get('schema/resources/v1/disk/resources')
        assert 'notifications/settings' in resp.content
        assert 'notifications/enable' in resp.content
        assert 'notifications/disable' in resp.content


class AppSubscriptionsNotificationsHandlersTestCase(UserTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.EXTERNAL
    api_version = 'v1'
    subs_url = 'disk/notifications/subscriptions/app'
    service_name = 'disk-notifier'
    query = {
        'events': 'e1,e2',
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
                    self.service_name,
                    self.query['app_instance_id'],
                    self.query['registration_token'],
                    self.query['app_name'],
                    self.query['platform'],
                )
                assert kwargs == {
                    'uid':  self.uid,
                    'filter_': {"rules":[{"do":"send_bright","if":"CommonEvents"},{"do":"skip"}],"vars":{"CommonEvents":{"$event":["e1","e2"]}}},
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

    def test_valid_xiva_token_subscribe(self):
        """Проверка что для xiva используется нужные сервис с правильным токеном"""
        def subscribe_app_stub(instance, *args, **kwargs):
            token = settings.services['XivaSubscribeNotifierService']['token']
            assert instance.token == token

        with self.specified_client(uid=self.uid, login=self.login, scopes=['yadisk:all']):
            with mock.patch(
                    'mpfs.core.services.push_service.XivaSubscribeCommonService.subscribe_app',
                    new=subscribe_app_stub):
                response = self.client.put(
                    self.subs_url,
                    query=self.query.copy()
                )
                assert response.status_code == 201

    def test_valid_xiva_token_unsubscribe(self):
        """Проверка что для xiva используется нужные сервис с правильным токеном"""
        def unsubscribe_app(instance, *args, **kwargs):
            token = settings.services['XivaSubscribeNotifierService']['token']
            assert instance.token == token

        with self.specified_client(uid=self.uid, login=self.login, scopes=['yadisk:all']):
            # подписываемся
            with PushServicesStub() as stub:
                response = self.client.put(self.subs_url, query=self.query.copy())
                assert response.status_code == 201
                # извлекаем ключ подписки
                json_resp = from_json(response.content)
                assert 'subscription_id' in json_resp
                subscription_id = json_resp['subscription_id']

        with self.specified_client(uid=self.uid, login=self.login, scopes=['yadisk:all']):
            with mock.patch(
                    'mpfs.core.services.push_service.XivaSubscribeCommonService.unsubscribe_app',
                    new=unsubscribe_app):
                response = self.client.delete(self.subs_url + '/%s' % subscription_id)
                assert response.status_code == 204

    def test_tags(self):
        """Тест подписи с дополнительным фильтром по тегам"""
        with self.specified_client(uid=self.uid, login=self.login, scopes=['yadisk:all']):
            with PushServicesStub() as stub:
                query = self.query.copy()
                query['tags'] = 'ios'
                response = self.client.put(self.subs_url, query=query)
                assert response.status_code == 201
                # проверяем верность запросов в Xiva
                assert len(stub.subscribe_app_common.call_args_list) == 1
                args, kwargs = stub.subscribe_app_common.call_args_list[0]
                assert args == (
                    self.service_name,
                    self.query['app_instance_id'],
                    self.query['registration_token'],
                    self.query['app_name'],
                    self.query['platform'],
                )
                assert kwargs == {
                    'uid': '128280859',
                    'device_id': None,
                    'filter_': {
                        'rules': [
                            {'do': 'send_bright', 'if': 'CommonEvents & Tags'},
                            {'do': 'skip'}
                        ],
                        'vars': {
                            'CommonEvents': {'$event': [u'e1', u'e2']},
                            'Tags': {'$has_tags': [u'ios']}
                        }
                    },
                }

    def test_adds_tags_and_device_id_for_mobile_automatically(self):
        device_id = 'E9C69BDA-0837-4867-A9B0-3AFCAAC3342A'
        tag_device_id = 'e9c69bda08374867a9b03afcaac3342a'
        user_agent = 'Yandex.Disk {"os":"iOS","src":"disk.mobile","id":"%s","device":"tablet"}' % device_id
        with self.specified_client(uid=self.uid, login=self.login, scopes=['yadisk:all']):
            with PushServicesStub() as stub:
                response = self.client.put(self.subs_url, query=self.query, headers={'User-Agent': user_agent})
                assert response.status_code == 201
                args, kwargs = stub.subscribe_app_common.call_args_list[0]
                assert kwargs == {
                    'uid': '128280859',
                    'device_id': device_id,
                    'filter_': {
                        'rules': [
                            {'do': 'send_bright', 'if': 'CommonEvents & Tags'},
                            {'do': 'send_bright', 'if': 'CommonEvents & DeviceIdTag'},
                            {'do': 'skip'},
                        ],
                        'vars': {
                            'CommonEvents': {'$event': [u'e1', u'e2']},
                            'Tags': {'$has_tags': [u'ios']},
                            'DeviceIdTag': {'$has_tags': [u'device_id.%s' % tag_device_id]},
                        }
                    },
                }

    def test_filter_duplicate_tags(self):
        device_id = 'E9C69BDA-0837-4867-A9B0-3AFCAAC3342A'
        tag_device_id = 'e9c69bda08374867a9b03afcaac3342a'
        user_agent = 'Yandex.Disk {"os":"iOS","src":"disk.mobile","id":"%s","device":"tablet"}' % device_id
        with self.specified_client(uid=self.uid, login=self.login, scopes=['yadisk:all']):
            with PushServicesStub() as stub:
                query = self.query.copy()
                query['tags'] = 'ios'
                response = self.client.put(self.subs_url, query=query, headers={'User-Agent': user_agent})
                assert response.status_code == 201
                args, kwargs = stub.subscribe_app_common.call_args_list[0]
                assert kwargs == {
                    'uid': '128280859',
                    'device_id': device_id,
                    'filter_': {
                        'rules': [
                            {'do': 'send_bright', 'if': 'CommonEvents & Tags'},
                            {'do': 'send_bright', 'if': 'CommonEvents & DeviceIdTag'},
                            {'do': 'skip'},
                        ],
                        'vars': {
                            'CommonEvents': {'$event': [u'e1', u'e2']},
                            'Tags': {'$has_tags': [u'ios']},
                            'DeviceIdTag': {'$has_tags': [u'device_id.%s' % tag_device_id]},
                        }
                    },
                }

    def test_bright_tags_for_ios(self):
        """Тест подписи с дополнительным фильтром по тегам"""
        device_id = 'E9C69BDA-0837-4867-A9B0-3AFCAAC3342A'
        user_agent = 'Yandex.Disk {"os":"iOS","src":"disk.mobile","id":"%s","device":"tablet"}' % device_id
        with self.specified_client(uid=self.uid, login=self.login, scopes=['yadisk:all']), \
                PushServicesStub() as stub:
            query = self.query.copy()
            query['tags'] = 'custom_tag,android'
            query['bright'] = 'true'
            response = self.client.put(self.subs_url, query=query, headers={'User-Agent': user_agent})
            assert response.status_code == 201
            # проверяем верность запросов в Xiva
            assert len(stub.subscribe_app_common.call_args_list) == 1
            args, kwargs = stub.subscribe_app_common.call_args_list[0]
            assert args == (
                self.service_name,
                self.query['app_instance_id'],
                self.query['registration_token'],
                self.query['app_name'],
                self.query['platform'],
            )
            assert kwargs == {
                'uid': '128280859',
                'device_id': device_id,
                'filter_': {
                    'rules': [
                        {'do': 'send_bright', 'if': 'CommonEvents & Tags'},
                        {'do': 'send_bright', 'if': 'CommonEvents & BrightEnabled'},
                        {'do': 'send_bright', 'if': 'CommonEvents & DeviceIdTag'},
                        {'do': 'skip'},
                    ],
                    'vars': {
                        'CommonEvents': {'$event': [u'e1', u'e2']},
                        'Tags': {'$has_tags': [u'android', 'ios', u'custom_tag']},
                        'DeviceIdTag': {'$has_tags': [u'device_id.e9c69bda08374867a9b03afcaac3342a']},
                        'BrightEnabled': {'$has_tags': ['ios_bright', u'android', u'custom_tag']},
                    }
                },
            }

    def test_version_tag_no_bright(self):
        """Тест подписи с дополнительным фильтром по тегам"""
        device_id = 'E9C69BDA-0837-4867-A9B0-3AFCAAC3342A'
        version = '2.62'
        vsn = '%s.0262' % version
        user_agent = 'Yandex.Disk {"os":"iOS","src":"disk.mobile","id":"%s","vsn":"%s","device":"tablet"}' % (device_id, vsn)
        with self.specified_client(uid=self.uid, login=self.login, scopes=['yadisk:all']), \
             PushServicesStub() as stub:
            query = self.query.copy()
            query['tags'] = 'custom_tag,android'
            response = self.client.put(self.subs_url, query=query, headers={'User-Agent': user_agent})
            assert response.status_code == 201
            # проверяем верность запросов в Xiva
            assert len(stub.subscribe_app_common.call_args_list) == 1
            args, kwargs = stub.subscribe_app_common.call_args_list[0]
            assert args == (
                self.service_name,
                self.query['app_instance_id'],
                self.query['registration_token'],
                self.query['app_name'],
                self.query['platform'],
            )
            assert kwargs == {
                'uid': '128280859',
                'device_id': device_id,
                'filter_': {
                    'rules': [
                        {'do': 'send_bright', 'if': 'CommonEvents & Tags'},
                        {'do': 'send_bright', 'if': 'CommonEvents & DeviceIdTag'},
                        {'do': 'send_bright', 'if': 'CommonEvents & Version'},
                        {'do': 'skip'},
                    ],
                    'vars': {
                        'CommonEvents': {'$event': [u'e1', u'e2']},
                        'Tags': {'$has_tags': [u'android', 'ios', u'custom_tag']},
                        'DeviceIdTag': {'$has_tags': [u'device_id.e9c69bda08374867a9b03afcaac3342a']},
                        'Version': {'$has_tags': [version]},
                    }
                },
            }

    @parameterized.expand([('2.62',), ('2.062',), ('02.062',)])
    def test_version_tag(self, version):
        u"""Тест подписи с дополнительным фильтром по тегам"""
        device_id = 'E9C69BDA-0837-4867-A9B0-3AFCAAC3342A'
        vsn = '%s.0262' % version
        user_agent = 'Yandex.Disk {"os":"iOS","src":"disk.mobile","id":"%s","vsn":"%s","device":"tablet"}' % (device_id, vsn)
        with self.specified_client(uid=self.uid, login=self.login, scopes=['yadisk:all']), \
             PushServicesStub() as stub:
            query = self.query.copy()
            query['tags'] = 'custom_tag,android'
            query['bright'] = 'true'
            response = self.client.put(self.subs_url, query=query, headers={'User-Agent': user_agent})
            assert response.status_code == 201
            # проверяем верность запросов в Xiva
            assert len(stub.subscribe_app_common.call_args_list) == 1
            args, kwargs = stub.subscribe_app_common.call_args_list[0]
            assert args == (
                self.service_name,
                self.query['app_instance_id'],
                self.query['registration_token'],
                self.query['app_name'],
                self.query['platform'],
            )
            assert kwargs == {
                'uid': '128280859',
                'device_id': device_id,
                'filter_': {
                    'rules': [
                        {'do': 'send_bright', 'if': 'CommonEvents & Tags'},
                        {'do': 'send_bright', 'if': 'CommonEvents & BrightEnabled'},
                        {'do': 'send_bright', 'if': 'CommonEvents & DeviceIdTag'},
                        {'do': 'send_bright', 'if': 'CommonEvents & BrightEnabled & Version'},
                        {'do': 'skip'},
                    ],
                    'vars': {
                        'CommonEvents': {'$event': [u'e1', u'e2']},
                        'Tags': {'$has_tags': [u'android', 'ios', u'custom_tag']},
                        'DeviceIdTag': {'$has_tags': [u'device_id.e9c69bda08374867a9b03afcaac3342a']},
                        'BrightEnabled': {'$has_tags': ['ios_bright', u'android', u'custom_tag']},
                        'Version': {'$has_tags': [version]},
                    }
                },
            }
