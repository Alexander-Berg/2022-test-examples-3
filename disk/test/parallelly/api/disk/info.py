# -*- coding: utf-8 -*-

import json
from httplib import OK, FORBIDDEN
import mock
import random

from attrdict import AttrDict

from mpfs.common.util.experiments.logic import enable_experiment_for_uid
from mpfs.common.util.overdraft import OVERDRAFT_STATUS_FIELD, OVERDRAFT_DATE_FIELD, OVERDRAFT_HARD_DATE_FIELD, \
    OVERDRAFT_BLOCK_DATE_FIELD
from mpfs.core.zookeeper.hooks import SettingsChangeEvent
from test.parallelly.api.disk.base import DiskApiTestCase
from test.base_suit import UploadFileTestCaseMixin, UserTestCaseMixin, SupportApiTestCaseMixin
from test.helpers.stubs.services import PassportStub
from test.helpers.stubs.resources.users_info import USERS_INFO, DEFAULT_USERS_INFO
from mpfs.common.static import tags
from mpfs.config import settings
from mpfs.common.util import to_json
from mpfs.core.services.passport_service import Passport
from mpfs.platform.events import dispatcher
from mpfs.platform.auth import TVMAuth, TVM2Auth, InternalTokenAuth
from mpfs.platform.v1.disk.handlers import GetDiskHandler
from mpfs.core.services.tvm_2_0_service import tvm2

PLATFORM_MOBILE_APPS_IDS = settings.platform['mobile_apps_ids']
PLATFORM_DISK_APPS_IDS = settings.platform['disk_apps_ids']


class GetDiskHandlerTestCase(UserTestCaseMixin, UploadFileTestCaseMixin, SupportApiTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.EXTERNAL
    api_version = 'v1'
    method = 'GET'
    url = 'disk'

    def __init__(self, *args, **kwargs):
        super(GetDiskHandlerTestCase, self).__init__(*args, **kwargs)
        self.dir_path = '/disk'

    def setup_method(self, method):
        super(GetDiskHandlerTestCase, self).setup_method(method)
        self.create_user(self.uid, noemail=True)

    def test_fields(self):
        file_path = '/disk/test.txt'
        self.upload_file(self.uid, file_path)
        self.json_ok('trash_append', {'uid': self.uid, 'path': file_path})
        for scope in ['cloud_api:disk.read', 'cloud_api:disk.info']:
            with self.specified_client(scopes=[scope], uid=self.uid, login=self.login, display_name=self.display_name):
                resp = self.client.request(self.method, self.url)
                json_content = json.loads(resp.content)

                self.assertStatusCodeEqual(resp.status_code, OK)
                self.assertIn('total_space', json_content)
                assert json_content['total_space'] > 0
                self.assertIn('used_space', json_content)
                assert json_content['used_space'] > 0
                self.assertIn('trash_size', json_content)
                assert json_content['trash_size'] > 0
                self.assertIn('max_file_size', json_content)
                assert json_content['max_file_size'] > 0
                self.assertIn('is_paid', json_content)
                self.assertIn('revision', json_content)
                self.assertIn('unlimited_autoupload_enabled', json_content)
                self.assertIsInstance(json_content['total_space'], int)
                self.assertIsInstance(json_content['used_space'], int)
                self.assertIsInstance(json_content['trash_size'], int)
                self.assertIsInstance(json_content['is_paid'], bool)
                self.assertIsInstance(json_content['revision'], int)
                self.assertIsInstance(json_content['unlimited_autoupload_enabled'], bool)
                self.assertEqual(json_content['trash_size'], json_content['used_space'])

                # check system_folders
                self.assertIn('system_folders', json_content)
                sys_folders = json_content['system_folders']
                self.assertIn('downloads', sys_folders)
                self.assertIn('applications', sys_folders)
                self.assertIn('screenshots', sys_folders)
                self.assertIn('photostream', sys_folders)

                # check uid, login and display_name in user
                self.assertIn('user', json_content)
                self.assertEqual(self.login, json_content['user']['login'])
                self.assertEqual(self.uid, json_content['user']['uid'])
                self.assertEqual(DEFAULT_USERS_INFO[self.uid]['country'], json_content['user']['country'])
                self.assertEqual(DEFAULT_USERS_INFO[self.uid]['display_name'], json_content['user']['display_name'])

    def test_permissions(self):
        scopes_to_status = (
            ([], FORBIDDEN),
            (['cloud_api:disk.read'], OK),
            (['cloud_api:disk.app_folder'], FORBIDDEN),
            (['cloud_api:disk.write'], FORBIDDEN),
        )
        self._permissions_test(scopes_to_status, self.method, self.url)

    def test_fields_with_set_query_param_fields(self):
        with self.specified_client(scopes=['cloud_api:disk.read']):
            resp = self.client.request(self.method, self.url, query={'fields': 'total_space'})
            info = json.loads(resp.content)

            self.assertStatusCodeEqual(resp.status_code, OK)
            self.assertEqual(len(info), 1)
            self.assertIn('total_space', info)

    def test_fields_with_empty_query_param_fields(self):
        with self.specified_client(scopes=['cloud_api:disk.read']):
            resp = self.client.request(self.method, self.url, query={'fields': ''})
            info = json.loads(resp.content)

            self.assertStatusCodeEqual(resp.status_code, OK)
            self.assertGreater(len(info), 0)

    def test_fields_with_nested_query_param_fields(self):
        with self.specified_client(scopes=['cloud_api:disk.read']):
            resp = self.client.request(self.method,
                                       self.url,
                                       query={'fields': 'system_folders.downloads'})
            info = json.loads(resp.content)

            self.assertStatusCodeEqual(resp.status_code, OK)
            self.assertEqual(len(info), 1)
            self.assertIn('system_folders', info)
            self.assertEqual(len(info['system_folders']), 1)
            self.assertIn('downloads', info['system_folders'])

    def test_request_with_social_account(self):
        """Протестировать, что при запросе информации пользователем с социальным аккаунтом он получит 403 ошибку."""
        with PassportStub() as stub:
            with self.specified_client(scopes=['cloud_api:disk.read'], uid=self.user_3.uid):
                # по сути нам все равно для теста что возвращает userinfo и остальные методы,
                # главное адекватное и чтоб юзер в запросе апи был не инициализирован
                # самое важное для воспроизведения - рейз нужной ошибки
                # для соц.аккаунта при подписке паспорт вернет ошибку
                stub.subscribe.side_effect = Passport.errors_map['accountwithpasswordrequired']
                response = self.client.request(self.method, self.url)
                assert response.status_code == 403
                content = json.loads(response.content)
                assert content['error'] == 'DiskUnsupportedUserAccountTypeError'

    def test_request_for_blocked_user(self):
        """Протестировать что отдаётся правильная ошибка в случае если пользователь заблокирован."""
        uid = self.uid
        self.support_ok('block_user', {
            'uid': uid,
            'moderator': 'moderator',
            'comment': 'comment',
        })
        with self.specified_client(scopes=['cloud_api:disk.read']):
            response = self.client.request(
                self.method,
                self.url,
                uid=uid
            )
            content = json.loads(response.content)
            assert response.status_code == 403
            assert content['error'] == 'DiskUserBlockedError'
            assert content['description'] == 'User is blocked.'
            assert content['message'] == u'Пользователь заблокирован.'

    def test_ip_forward_to_mpfs_requests(self):
        with self.specified_client(scopes=['cloud_api:disk.read']), \
                mock.patch('mpfs.core.services.mpfsproxy_service.MpfsProxy.open_url') as mpfs_proxy_open_url:
            self.client.request(self.method, self.url, uid=self.uid)
            _, kwargs = mpfs_proxy_open_url.call_args
            assert 'headers' in kwargs
            assert 'X-Real-Ip' in kwargs['headers']
            assert kwargs['headers']['X-Real-Ip']

    def test_online_editor_name(self):
        for scope in ['cloud_api:disk.read', 'cloud_api:disk.info']:
            with self.specified_client(id=PLATFORM_MOBILE_APPS_IDS[0], scopes=[scope], uid=self.uid,
                                       login=self.login, display_name=self.display_name):
                resp = self.client.request(self.method, self.url)
                json_content = json.loads(resp.content)

                self.assertStatusCodeEqual(resp.status_code, OK)
                self.assertIn('office_online_editor_name', json_content)

    def test_overdraft_fields(self):
        get_overdraft_info = {
            OVERDRAFT_STATUS_FIELD: 2,
            OVERDRAFT_DATE_FIELD: '2021-05-28',
            OVERDRAFT_HARD_DATE_FIELD: '2021-05-28',
            OVERDRAFT_BLOCK_DATE_FIELD: '2021-05-28'}

        for scope in ['cloud_api:disk.read', 'cloud_api:disk.info']:
            with self.specified_client(id=PLATFORM_MOBILE_APPS_IDS[0], scopes=[scope], uid=self.uid,
                                       login=self.login, display_name=self.display_name), \
                 mock.patch('mpfs.core.user.common.CommonUser.get_overdraft_info', return_value=get_overdraft_info), \
                 enable_experiment_for_uid('new_overdraft_strategy', self.uid), \
                 mock.patch('mpfs.core.services.passport_service.Passport.userinfo', return_value={'country': 'ru'}), \
                 mock.patch('mpfs.core.user.common.CommonUser.is_in_overdraft_for_restrictions', return_value=True):
                resp = self.client.request(self.method, self.url)
                json_content = json.loads(resp.content)

                self.assertStatusCodeEqual(resp.status_code, OK)
                self.assertIn(OVERDRAFT_STATUS_FIELD, json_content)
                self.assertIn(OVERDRAFT_DATE_FIELD, json_content)
                self.assertIn(OVERDRAFT_HARD_DATE_FIELD, json_content)
                self.assertIn(OVERDRAFT_BLOCK_DATE_FIELD, json_content)

    def test_avatars_field_for_disk_client(self):
        for scope in ['cloud_api:disk.read', 'cloud_api:disk.info']:
            with self.specified_client(id=PLATFORM_DISK_APPS_IDS[0], scopes=[scope], uid=self.uid,
                                       login=self.login, display_name=self.display_name):
                resp = self.client.request(self.method, self.url)
                json_content = json.loads(resp.content)

                self.assertStatusCodeEqual(resp.status_code, OK)
                self.assertIn('user', json_content)
                self.assertIn('avatar_url', json_content['user'])
                self.assertIn('is_yandex_staff', json_content['user'])

    def test_avatars_field_for_external_client(self):
        for scope in ['cloud_api:disk.read', 'cloud_api:disk.info']:
            with self.specified_client(scopes=[scope], uid=self.uid, login=self.login, display_name=self.display_name):
                resp = self.client.request(self.method, self.url)
                json_content = json.loads(resp.content)

                self.assertStatusCodeEqual(resp.status_code, OK)
                self.assertIn('user', json_content)
                self.assertNotIn('avatar_url', json_content['user'])
                self.assertNotIn('is_yandex_staff', json_content['user'])


class GetDiskHandlerInternalTestCase(UserTestCaseMixin, UploadFileTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.INTERNAL
    api_version = 'v1'
    method = 'GET'
    url = 'disk'

    def __init__(self, *args, **kwargs):
        super(GetDiskHandlerInternalTestCase, self).__init__(*args, **kwargs)
        self.dir_path = '/disk'

    def setup_method(self, method):
        super(GetDiskHandlerInternalTestCase, self).setup_method(method)
        self.create_user(self.uid, noemail=True)

    def test_settings_change_internal_token_auth(self):
        """Протестировать, что внутренняя аутентификация по токену подхватывает изменение настроек."""
        auth_methods = [InternalTokenAuth()]
        with mock.patch(
            'mpfs.platform.v1.disk.handlers.GetDiskHandler.auth_methods',
            return_value=auth_methods
        ):
            with mock.patch(
                'mpfs.platform.v1.disk.handlers.GetDiskHandler.get_auth_methods',
                return_value=auth_methods
            ):
                auth_settings = settings.platform['auth']
                random_client = random.choice(
                    [c for c in auth_settings if 'token' in c['auth_methods'] and 'token' in c]
                )
                assert random_client
                allowed_token = random_client['token']
                response = self.client.request(
                    self.method,
                    self.url,
                    headers={
                        'Authorization': 'ClientToken token=%s;uid=%s' % (allowed_token, self.uid)
                    }
                )
                assert response.status_code == 200

                # патчим сеттинги, удаляем из них токен, который только что использовали (чтоб получить 401)
                # после этого кидаем событие изменения настроек, чтоб все объекты авторизации
                # подхватили их

                # удаляем клиента, которого только что использовали
                new_auth_settings = [c for c in auth_settings if c['name'] != random_client['name']]
                non_allowed_token = allowed_token

                # меняем настройки, а потом кидаем ивент
                with mock.patch.dict('mpfs.config.settings.platform', {'auth': new_auth_settings}):
                    dispatcher.send(SettingsChangeEvent())
                    response = self.client.request(
                        self.method,
                        self.url,
                        headers={
                            'Authorization': 'ClientToken token=%s;uid=%s' % (non_allowed_token, self.uid)
                        }
                    )
                    assert response.status_code == 401

    def test_settings_change_tvm_ticket(self):
        """Протестировать, что внутренняя аутентификация по TVM тикету подхватывает изменение настроек."""
        tvm_client_id = 123  # random
        tvm_ticket = 'fake_ticket'
        platform_fake_settings = {
            'auth': [
                {'name': 'yandex.test',
                 'auth_methods': ['tvm'],
                 'oauth_scopes': [],
                 'enabled': True,
                 'tvm_client_ids': [tvm_client_id],
                 'oauth_client_id': 'A',
                 'oauth_client_name': 'B'
                 }
            ]
        }
        with mock.patch.dict('mpfs.config.settings.platform', platform_fake_settings), \
             mock.patch.object(GetDiskHandler, 'auth_methods', [TVMAuth()]):
            with mock.patch('mpfs.core.services.tvm_service.TVMService.validate_ticket',
                            return_value=AttrDict({'has_client_id': True,
                                                   'client_ids': [tvm_client_id],
                                                   'default_uid': [],
                                                   'uids': []})) as mocked_validate_ticket:
                response = self.client.request(self.method,
                                               self.url,
                                               headers={'Authorization': 'TVM uid=%s' % self.uid,
                                                        'Ticket': tvm_ticket})
                assert response.status_code == 200
                assert mocked_validate_ticket.called

            # валидация во внешнем сервисе проходит, но мы его убрали из наших настроек
            # и сообщили что настройки изменились
            # теперь тот же хендлер должен вернуть 401
            with mock.patch.dict('mpfs.platform.handlers.settings.platform',
                                 {'auth': []}):
                dispatcher.send(SettingsChangeEvent())
                response = self.client.request(self.method,
                                               self.url,
                                               headers={'Authorization': 'TVM uid=%s' % self.uid,
                                                        'Ticket': tvm_ticket})
                assert response.status_code == 401
        dispatcher.send(SettingsChangeEvent())

    def test_settings_change_tvm_2_0_ticket(self):
        """Протестировать, что внутренняя аутентификация по TVM 2.0 тикету подхватывает изменение настроек."""
        tvm2.update_public_keys()
        tvm_client_ids = [123, 124]  # random
        fake_service_ticket = 'fake_service_ticket'
        platform_fake_settings = {
            'auth': [
                {
                    'name': 'yandex.test',
                    'auth_methods': ['tvm_2_0'],
                    'oauth_scopes': [],
                    'enabled': True,
                    'tvm_2_0_client_ids': tvm_client_ids,
                    'oauth_client_id': 'A',
                    'oauth_client_name': 'B'

                }
            ]
        }
        with mock.patch.dict('mpfs.config.settings.platform', platform_fake_settings):
            auth_methods = [TVM2Auth()]
            with mock.patch('mpfs.platform.v1.disk.handlers.GetDiskHandler.auth_methods', return_value=auth_methods), \
                mock.patch('mpfs.platform.v1.disk.handlers.GetDiskHandler.get_auth_methods', return_value=auth_methods), \
                mock.patch('mpfs.core.services.tvm_2_0_service.TVM2Service.validate_service_ticket',
                           return_value=AttrDict({'src': tvm_client_ids[1]})) as mocked_validate_ticket:
                response = self.client.request(
                    self.method,
                    self.url,
                    headers={
                        'X-Uid': self.uid,
                        'X-Ya-Service-Ticket': fake_service_ticket
                    }
                )
                assert response.status_code == 200
                assert mocked_validate_ticket.called

            # валидация во внешнем сервисе проходит, но мы его убрали из наших настроек
            # и сообщили что настройки изменились
            # теперь тот же хендлер должен вернуть 401

            with mock.patch.dict('mpfs.config.settings.platform', {'auth': []}), \
                mock.patch('mpfs.core.services.tvm_2_0_service.TVM2Service.validate_service_ticket',
                           return_value=AttrDict({'src': tvm_client_ids[1]})):
                    dispatcher.send(SettingsChangeEvent())
                    response = self.client.request(
                        self.method,
                        self.url,
                        headers={
                            'X-Uid': self.uid,
                            'X-Ya-Service-Ticket': fake_service_ticket
                        }
                    )
                    assert response.status_code == 401
