# -*- coding: utf-8 -*-
import base64
import json
import os
import urllib
from copy import deepcopy

from hamcrest import assert_that, has_entry, has_key, is_not
from mock import patch, mock
from urlparse import parse_qs, urlparse

from nose_parameterized import parameterized

from mpfs.common.static.tags import experiment_names
from mpfs.common.util.experiments.logic import experiment_manager
from mpfs.config import settings
from mpfs.core.office.static import OfficeAccessStateConst, OfficeServiceIDConst
from test.parallelly.api.disk.base import DiskApiTestCase
from test.base_suit import UploadFileTestCaseMixin, UserTestCaseMixin, SharingTestCaseMixin
from test.base_suit import SupportApiTestCaseMixin
from test.helpers.stubs.resources.users_info import DEFAULT_USERS_INFO
from test.helpers.stubs.services import PassportStub
from test.fixtures.users import user_1, default_user
from mpfs.common.static import tags
from mpfs.core.office.logic import microsoft
from mpfs.core.services.discovery_service import DiscoveryService
from mpfs.core.services.passport_service import passport
from mpfs.platform.handlers import ServiceProxyHandler
from mpfs.platform.v1.wopi import handlers


class WOPITestCase(UserTestCaseMixin, UploadFileTestCaseMixin, SharingTestCaseMixin, SupportApiTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.INTERNAL
    api_version = 'v1'
    FILE_PATH = '/disk/test.txt'

    def __init__(self, *args, **kwargs):
        super(WOPITestCase, self).__init__(*args, **kwargs)
        self.dir_path = '/disk'

    @classmethod
    def setUpClass(cls):
        super(WOPITestCase, cls).setUpClass()
        with open('fixtures/xml/discovery.xml') as fd:
            xml = fd.read()
        with patch.object(DiscoveryService, 'open_url', return_value=xml):
            DiscoveryService().ensure_cache()

    def setup_method(self, method):
        super(WOPITestCase, self).setup_method(method)
        self.create_user(self.uid, noemail=1)
        self.upload_file(self.uid, self.FILE_PATH)

    def test_wopi_url(self):
        response = self.client.get('disk/resources/wopi-url', {'path': self.FILE_PATH}, uid=self.uid)
        json_content = json.loads(response.content)
        assert 'href' in json_content
        # Отдавать во внутреннем API ссылки на внешнее API.
        assert '/v1/disk/wopi/files' in json_content['href']
        assert json_content['method'] == 'GET'
        assert json_content['templated'] == False

    def test_lock(self):
        response = self.client.get('disk/resources/wopi-url', {'path': self.FILE_PATH}, uid=self.uid)
        json_content = json.loads(response.content)
        url = json_content['href']
        wopi_endpoint = 'disk' + url.split('disk')[-1]

        # тестируем установку лока, если его изначально не было на файле
        lock_value = 'ABCDEF1234567890'
        with patch.object(ServiceProxyHandler, 'request_service',
                          return_value={'success': True, 'office_lock_id': lock_value}):
            response = self.client.post(
                wopi_endpoint,
                headers={'X-WOPI-Lock': lock_value, 'X-WOPI-Override': 'LOCK'},
                uid=self.uid
            )
            assert response.status == 200
            assert 'X-WOPI-Lock' not in response.headers

        # тестируем установку лока, если на файле лок уже установлен
        # и его значение не совпадает с новым устанавливаемым значением
        lock_value_2 = 'wrong-lock-value'
        with patch.object(ServiceProxyHandler, 'request_service',
                          return_value={'success': False, 'office_lock_id': lock_value}):
            response = self.client.post(
                wopi_endpoint,
                headers={'X-WOPI-Lock': lock_value_2, 'X-WOPI-Override': 'LOCK'},
                uid=self.uid
            )
            assert response.status == 409
            assert 'X-WOPI-Lock' in response.headers
            assert response.headers['X-WOPI-Lock'] == lock_value

        # тестируем установку лока, если на файле лок уже установлен
        # и его значение совпадает с новым устанавливаемым значением
        with patch.object(ServiceProxyHandler, 'request_service',
                          return_value={'success': False, 'office_lock_id': lock_value}):
            response = self.client.post(
                wopi_endpoint,
                headers={'X-WOPI-Lock': lock_value, 'X-WOPI-Override': 'LOCK'},
                uid=self.uid
            )
            assert response.status == 409
            assert 'X-WOPI-Lock' in response.headers
            assert response.headers['X-WOPI-Lock'] == lock_value

    def test_unlock_and_relock(self):
        response = self.client.get('disk/resources/wopi-url', {'path': self.FILE_PATH}, uid=self.uid)
        json_content = json.loads(response.content)
        url = json_content['href']
        wopi_endpoint = 'disk' + url.split('disk')[-1]

        # тестируем установку лока, если его изначально не было на файле
        old_lock_value = 'ABCDEF1234567890'
        new_lock_value = '0987654321FEDCBA'
        with patch.object(ServiceProxyHandler, 'request_service',
                          return_value={'success': False, 'office_lock_id': ''}):
            response = self.client.post(
                wopi_endpoint,
                headers={'X-WOPI-Lock': new_lock_value, 'X-WOPI-OldLock': old_lock_value, 'X-WOPI-Override': 'LOCK'},
                uid=self.uid
            )
            assert response.status == 409
            assert 'X-WOPI-Lock' in response.headers
            assert response.headers['X-WOPI-Lock'] == ''

        # тестируем установку лока, если на файле лок уже установлен
        # и его значение не совпадает с значением в X-WOPI-OldLock
        lock_value_2 = 'wrong-lock-value'
        with patch.object(ServiceProxyHandler, 'request_service',
                          return_value={'success': False, 'office_lock_id': old_lock_value}):
            response = self.client.post(
                wopi_endpoint,
                headers={'X-WOPI-Lock': new_lock_value, 'X-WOPI-OldLock': lock_value_2, 'X-WOPI-Override': 'LOCK'},
                uid=self.uid
            )
            assert response.status == 409
            assert 'X-WOPI-Lock' in response.headers
            assert response.headers['X-WOPI-Lock'] == old_lock_value

        # тестируем установку лока, если на файле лок уже установлен
        # и его значение совпадает с значением в X-WOPI-OldLock
        with patch.object(ServiceProxyHandler, 'request_service',
                          return_value={'success': True, 'office_lock_id': old_lock_value}):
            response = self.client.post(
                wopi_endpoint,
                headers={'X-WOPI-Lock': new_lock_value, 'X-WOPI-OldLock': old_lock_value, 'X-WOPI-Override': 'LOCK'},
                uid=self.uid
            )
            assert response.status == 200
            assert 'X-WOPI-Lock' not in response.headers

    def test_unlock(self):
        response = self.client.get('disk/resources/wopi-url', {'path': self.FILE_PATH}, uid=self.uid)
        json_content = json.loads(response.content)
        url = json_content['href']
        wopi_endpoint = 'disk' + url.split('disk')[-1]

        # тестируем снятие лока, если его изначально не было на файле
        lock_value = 'ABCDEF1234567890'
        with patch.object(ServiceProxyHandler, 'request_service',
                          return_value={'success': False, 'office_lock_id': ''}):
            response = self.client.post(
                wopi_endpoint,
                headers={'X-WOPI-Lock': lock_value, 'X-WOPI-Override': 'UNLOCK'},
                uid=self.uid
            )
            assert response.status == 409
            assert 'X-WOPI-Lock' in response.headers
            assert response.headers['X-WOPI-Lock'] == ''

        # тестируем снятие лока, если лок на файле не соответствует запрашиваемому
        strange_lock_value = 'wrong-lock-value'
        with patch.object(ServiceProxyHandler, 'request_service',
                          return_value={'success': False, 'office_lock_id': lock_value}):
            response = self.client.post(
                wopi_endpoint,
                headers={'X-WOPI-Lock': strange_lock_value, 'X-WOPI-Override': 'UNLOCK'},
                uid=self.uid
            )
            assert response.status == 409
            assert 'X-WOPI-Lock' in response.headers
            assert response.headers['X-WOPI-Lock'] == lock_value

        # тестируем снятие лока, если лок на файле соответствует запрашиваемому
        with patch.object(ServiceProxyHandler, 'request_service',
                          return_value={'success': True, 'office_lock_id': lock_value}):
            response = self.client.post(
                wopi_endpoint,
                headers={'X-WOPI-Lock': lock_value, 'X-WOPI-Override': 'UNLOCK'},
                uid=self.uid
            )
            assert response.status == 200
            assert 'X-WOPI-Lock' not in response.headers

    def test_refresh_lock(self):
        response = self.client.get('disk/resources/wopi-url', {'path': self.FILE_PATH}, uid=self.uid)
        json_content = json.loads(response.content)
        url = json_content['href']
        wopi_endpoint = 'disk' + url.split('disk')[-1]

        # тестируем обновление лока, если его изначально не было на файле
        lock_value = 'ABCDEF1234567890'
        with patch.object(ServiceProxyHandler, 'request_service',
                          return_value={'success': False, 'office_lock_id': ''}):
            response = self.client.post(
                wopi_endpoint,
                headers={'X-WOPI-Lock': lock_value, 'X-WOPI-Override': 'REFRESH_LOCK'},
                uid=self.uid
            )
            assert response.status == 409
            assert 'X-WOPI-Lock' in response.headers
            assert response.headers['X-WOPI-Lock'] == ''

        # тестируем обновление лока, если лок на файле не соответствует запрашиваемому
        strange_lock_value = 'wrong-lock-value'
        with patch.object(ServiceProxyHandler, 'request_service',
                          return_value={'success': False, 'office_lock_id': lock_value}):
            response = self.client.post(
                wopi_endpoint,
                headers={'X-WOPI-Lock': strange_lock_value, 'X-WOPI-Override': 'REFRESH_LOCK'},
                uid=self.uid
            )
            assert response.status == 409
            assert 'X-WOPI-Lock' in response.headers
            assert response.headers['X-WOPI-Lock'] == lock_value

        # тестируем обновление лока, если лок на файле соответствует запрашиваемому
        with patch.object(ServiceProxyHandler, 'request_service',
                          return_value={'success': True, 'office_lock_id': lock_value}):
            response = self.client.post(
                wopi_endpoint,
                headers={'X-WOPI-Lock': lock_value, 'X-WOPI-Override': 'REFRESH_LOCK'},
                uid=self.uid
            )
            assert response.status == 200
            assert 'X-WOPI-Lock' not in response.headers

    def test_rename(self):
        response = self.client.get('disk/resources/wopi-url', {'path': self.FILE_PATH}, uid=self.uid)
        json_content = json.loads(response.content)
        url = json_content['href']
        wopi_endpoint = 'disk' + url.split('disk')[-1]

        # тестируем переименование, если файл залочен и лок из запроса совпадает с локом на файле
        lock_value = 'TEST_LOCK_VALUE'
        response = self.client.post(
            wopi_endpoint,
            headers={'X-WOPI-Lock': lock_value, 'X-WOPI-Override': 'LOCK'},
            uid=self.uid
        )
        assert response.status == 200
        assert 'X-WOPI-Lock' not in response.headers

        new_file_name = 'new_name'
        response = self.client.post(
            wopi_endpoint,
            headers={'X-WOPI-Lock': lock_value, 'X-WOPI-Override': 'RENAME_FILE',
                     'X-WOPI-RequestedName': new_file_name},
            uid=self.uid
        )
        assert response.status == 200
        assert 'X-WOPI-Lock' not in response.headers
        assert json.loads(response.content)["Name"] == new_file_name

        # дополнительно проверим, что на файле остался старый лок:
        response = self.client.post(
            wopi_endpoint,
            headers={'X-WOPI-Lock': lock_value, 'X-WOPI-OldLock': lock_value, 'X-WOPI-Override': 'LOCK'},
            uid=self.uid
        )
        assert response.status == 200
        assert 'X-WOPI-Lock' not in response.headers

        # тестируем переименование, если файл залочен и лок из запроса не совпадает с локом на файле
        wrong_lock_value = 'WRONG_TEST_LOCK_VALUE'
        new_file_name_2 = 'new_name_2'
        response = self.client.post(
            wopi_endpoint,
            headers={'X-WOPI-Lock': wrong_lock_value, 'X-WOPI-Override': 'RENAME_FILE',
                     'X-WOPI-RequestedName': new_file_name_2},
            uid=self.uid
        )
        # TODO
        # Исправить на 409 и раскоментировать, когда МС починит переименование
        # assert 'X-WOPI-Lock' in response.headers
        # assert response.headers['X-WOPI-Lock'] == lock_value
        assert response.status == 200

        # тестируем переименование, если файл не залочен
        response = self.client.post(
            wopi_endpoint,
            headers={'X-WOPI-Lock': lock_value, 'X-WOPI-Override': 'UNLOCK'},
            uid=self.uid
        )
        assert response.status == 200
        assert 'X-WOPI-Lock' not in response.headers

        new_file_name_3 = 'new_name_3'
        response = self.client.post(
            wopi_endpoint,
            headers={'X-WOPI-Lock': wrong_lock_value, 'X-WOPI-Override': 'RENAME_FILE',
                     'X-WOPI-RequestedName': new_file_name_3},
            uid=self.uid
        )
        assert response.status == 200
        assert 'X-WOPI-Lock' not in response.headers
        assert json.loads(response.content)["Name"] == new_file_name_3

    def test_lock_shared_readonly(self):
        other_uid = user_1.uid
        self.create_user(other_uid)

        # создаем папку
        shared_dir = '/disk/shared-folder'
        shared_file = '/disk/shared-folder/test.docx'
        self.json_ok('mkdir', opts={'uid': self.uid, 'path': shared_dir})

        # заливаем туда файл
        self.upload_file(self.uid, shared_file)

        # расшариваем папку
        group = self.json_ok('share_create_group', {'uid': self.uid, 'path': shared_dir})
        # приглашаем тестового пользователя в расшаренную папку
        invite_hash = self.share_invite(group['gid'], other_uid, rights=640)
        # подключаем расшареную папку тестовому пользователю
        self.json_ok('share_activate_invite', {'uid': other_uid, 'hash': invite_hash})

        response = self.client.get('disk/resources/wopi-url', {'path': shared_file}, uid=other_uid)
        json_content = json.loads(response.content)
        url = json_content['href']
        wopi_endpoint = 'disk' + url.split('disk')[-1]

        response = self.client.post(
            wopi_endpoint,
            headers={'X-WOPI-Lock': 'TEST_LOCK_VALUE', 'X-WOPI-Override': 'LOCK'},
            uid=other_uid
        )
        assert response.status == 409
        assert 'X-WOPI-Lock' in response.headers
        assert response.headers['X-WOPI-Lock'] == ''

        # меняем права группе на 660 и проверяем теперь возможность лока:
        self.json_ok('share_change_rights', {'uid': self.uid,
                                             'gid': group['gid'],
                                             'user_uid': other_uid,
                                             'rights': 660})

        with patch.object(microsoft, 'OFFICE_AVAILABLE_FOR_UIDS', new=[self.uid, other_uid]):
            self.json_ok('office_action_check', {'uid': other_uid,
                                                 'action': 'edit',
                                                 'service_id': 'disk',
                                                 'service_file_id': shared_file})
        response = self.client.post(
            wopi_endpoint,
            headers={'X-WOPI-Lock': 'TEST_LOCK_VALUE', 'X-WOPI-Override': 'LOCK'},
            uid=other_uid
        )
        assert response.status == 200
        assert 'X-WOPI-Lock' not in response.headers

    def test_empty_content_on_error(self):
        response = self.client.get('disk/resources/wopi-url', {'path': self.FILE_PATH}, uid=self.uid)
        json_content = json.loads(response.content)
        url = json_content['href']
        wopi_endpoint = 'disk' + url.split('disk')[-1]

        lock_value = 'ABCDEF1234567890'
        with patch.object(ServiceProxyHandler, 'request_service',
                          return_value={'success': False, 'office_lock_id': ''}):
            response = self.client.post(
                wopi_endpoint,
                headers={'X-WOPI-Lock': lock_value, 'X-WOPI-Override': 'UNLOCK'},
                uid=self.uid
            )
            assert response.status == 409
            assert 'X-WOPI-Lock' in response.headers
            assert response.headers['X-WOPI-Lock'] == ''
            assert len(response.content) == 0

        self.json_ok('rm', {'uid': self.uid, 'path': self.FILE_PATH})

        response = self.client.post(
            wopi_endpoint,
            headers={'X-WOPI-Lock': lock_value, 'X-WOPI-Override': 'UNLOCK'},
            uid=self.uid
        )
        assert response.status == 404
        assert len(response.content) == 0

    def test_request_for_noninit_user_with_passport_bad_response(self):
        """Протестировать, что при запросе информации пользователем с аккаунтом без пароля он получит 403 ошибку."""
        uid = '123456789'
        user_info = deepcopy(DEFAULT_USERS_INFO[self.uid])
        user_info['uid'] = uid
        with self.specified_client(scopes=['yadisk:all'], uid=uid):
            with PassportStub(userinfo=user_info) as stub:
                stub.subscribe.side_effect = passport.errors_map['accountwithpasswordrequired']
                response = self.client.get('disk/resources/wopi-url', {'path': self.FILE_PATH}, uid=uid)
                assert response.status_code == 403
                content = json.loads(response.content)
                assert content['error'] == 'DiskUnsupportedUserAccountTypeError'
                assert content['description'] == 'User account type is not supported.'

    def test_request_for_blocked_user(self):
        opts = {
            'uid': self.uid,
            'moderator': 'moderator',
            'comment': 'comment',
        }
        self.support_ok('block_user', opts)
        with self.specified_client(scopes=['yadisk:all'], uid=self.uid):
            response = self.client.get('disk/resources/wopi-url', {'path': self.FILE_PATH}, uid=self.uid)
            content = json.loads(response.content)
            assert response.status_code == 403
            assert content['error'] == 'DiskUserBlockedError'
            assert content['description'] == 'User is blocked.'

    def test_request_for_noninit_user(self):
        uid = '123456789'
        user_info = deepcopy(DEFAULT_USERS_INFO[self.uid])
        user_info['uid'] = uid
        with self.specified_client(scopes=['yadisk:all'], uid=uid):
            with PassportStub(userinfo=user_info):
                response = self.client.get('disk/resources/wopi-url', {'path': self.FILE_PATH}, uid=uid)
                # init and retry with _auto_initialize_user
                assert response.status_code == 404


class WOPIExternalTestCase(UserTestCaseMixin, UploadFileTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.EXTERNAL
    api_version = 'v1'
    FILE_PATH = '/disk/test.xlsx'

    def __init__(self, *args, **kwargs):
        super(WOPIExternalTestCase, self).__init__(*args, **kwargs)
        self.dir_path = '/disk'

    @classmethod
    def setUpClass(cls):
        super(WOPIExternalTestCase, cls).setUpClass()
        with open('fixtures/xml/discovery.xml') as fd:
            xml = fd.read()
        with patch.object(DiscoveryService, 'open_url', return_value=xml):
            DiscoveryService().ensure_cache()

    def setup_method(self, method):
        super(WOPIExternalTestCase, self).setup_method(method)
        self.create_user(self.uid, noemail=1)
        self.upload_file(self.uid, self.FILE_PATH, file_data={'size': 100})

    def test_check_file_info(self):
        action_data = self.json_ok('office_action_data', {
            'uid': self.uid, 'action': 'edit', 'path': self.FILE_PATH,
            'post_message_origin': 'https://disk.qa.yandex.ru'
        })

        response = self.client.get('%s' % (action_data['resource_url']))
        assert response.status == 401

        sc = parse_qs(action_data['action_url'])['sc'][0]
        response = self.client.get('%s?access_token=%s' % (
            action_data['resource_url'], urllib.quote(action_data['access_token'], safe='')
        ), headers={'X-WOPI-SessionContext': base64.b64encode(sc)})
        assert response.status == 200

        json_content = json.loads(response.content)
        required_fields = {
            'OwnerId', 'Version', 'BaseFileName', 'Size', 'PostMessageOrigin',
            'SupportsLocks', 'SupportsUpdate', 'UserCanWrite', 'SupportsRename', 'UserCanRename',
            'AllowExternalMarketplace', 'CloseButtonClosesWindow', 'FileNameMaxLength',
            'BreadcrumbBrandName', 'BreadcrumbBrandUrl', 'BreadcrumbDocName',
            'BreadcrumbFolderName', 'BreadcrumbFolderUrl', 'DownloadUrl', 'UserId', 'UserCanNotWriteRelative',
            'SupportsCoauth', 'UserFriendlyName'
        }

        assert not required_fields ^ json_content.viewkeys()

        folder_url, file_path = json_content['BreadcrumbFolderUrl'].split('|select')
        assert folder_url.endswith(os.path.dirname(self.FILE_PATH))
        assert file_path == self.FILE_PATH

        # протестируем отдачу ссылок в iframe офиса, отдаваемые CheckFileInfo
        action_data = self.json_ok('office_action_data', {
            'uid': self.uid, 'action': 'edit', 'path': self.FILE_PATH,
            'post_message_origin': 'https://disk.qa.yandex.com'
        })

        response = self.client.get('%s' % (action_data['resource_url']))
        assert response.status == 401

        sc = parse_qs(action_data['action_url'])['sc'][0]
        response = self.client.get('%s?access_token=%s' % (
            action_data['resource_url'], urllib.quote(action_data['access_token'], safe='')
        ), headers={'X-WOPI-SessionContext': base64.b64encode(sc)})
        assert response.status == 200

        json_content = json.loads(response.content)
        assert urlparse(json_content['BreadcrumbBrandUrl']).hostname == 'disk.qa.yandex.com'
        assert urlparse(json_content['BreadcrumbFolderUrl']).hostname == 'disk.qa.yandex.com'

        # протестируем отдачу ссылок в iframe офиса, отдаваемые CheckFileInfo, если не указать origin
        action_data = self.json_ok('office_action_data', {
            'uid': self.uid, 'action': 'edit', 'path': self.FILE_PATH,
        })

        response = self.client.get('%s?access_token=%s' % (
            action_data['resource_url'], urllib.quote(action_data['access_token'], safe='')
        ))
        assert response.status == 200

        json_content = json.loads(response.content)
        assert urlparse(json_content['BreadcrumbBrandUrl']).hostname == 'disk.yandex.ru'
        assert urlparse(json_content['BreadcrumbFolderUrl']).hostname == 'disk.yandex.ru'

    @parameterized.expand([
        ('with_exp', [default_user.uid], has_entry('FileSharingPostMessage', True)),
        ('wo_exp', [user_1.uid], is_not(has_key('FileSharingPostMessage'))),
    ])
    def test_file_sharing_post_message(self, case_name, exp_uids, matcher):
        action_data = self.json_ok('office_action_data', {
            'uid': self.uid, 'action': 'edit', 'path': self.FILE_PATH,
            'post_message_origin': 'https://disk.qa.yandex.ru'
        })

        sc = parse_qs(action_data['action_url'])['sc'][0]

        new_exps = deepcopy(settings.experiments)
        new_exps[experiment_names.MS_OFFICE_FILE_SHARING_POST_MESSAGE]['clauses']['by_uid']['enabled_for'] = exp_uids
        with self.specified_client(scopes=['yadisk:all'], uid=self.uid), \
                mock.patch.dict(settings.experiments, new_exps):
            experiment_manager.load_experiments_from_conf()
            response = self.client.get('%s?access_token=%s' % (
                action_data['resource_url'],
                urllib.quote(action_data['access_token'], safe='')
            ), headers={'X-WOPI-SessionContext': base64.b64encode(sc)})

        experiment_manager.load_experiments_from_conf()
        json_content = json.loads(response.content)

        assert_that(json_content, matcher)

    def test_session_context(self):
        """Проверить что post_message_origin добавляется в SessionContext,
        обработчик правильно парсит SessionContext и post_message_origin валидируется.
        """
        responses = []
        with patch.object(handlers, 'OFFICE_CHECK_INFO_CUSTOM_RESPONSE_FIELDS', []):
            for pm_origin in ('https://disk.qa.yandex.ru', 'https://abc.example.ru'):
                response = self.json_ok('office_action_data', {
                    'uid': self.uid, 'action': 'edit', 'path': self.FILE_PATH,
                    'post_message_origin': pm_origin
                })
                sc = parse_qs(response['action_url'])['sc'][0]
                response = self.client.get('%s?access_token=%s' % (
                    response['resource_url'],
                    urllib.quote(response['access_token'], safe='')
                ),  headers={'X-WOPI-SessionContext': base64.b64encode(sc)})

                responses.append(response)

            assert (200, 200) == tuple(r.status for r in responses)
            assert 'https://disk.qa.yandex.ru' == json.loads(responses[0].content)['PostMessageOrigin']
            assert 'PostMessageOrigin' not in json.loads(responses[1].content)
