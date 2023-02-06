# -*- coding: utf-8 -*-
import json
from nose_parameterized import parameterized

from test.parallelly.api.disk.base import DiskApiTestCase
from test.base_suit import UploadFileTestCaseMixin, UserTestCaseMixin
from test.fixtures.users import user_1, default_user
from mpfs.common.static import tags

from copy import deepcopy
from test.base_suit import SupportApiTestCaseMixin
from test.helpers.stubs.resources.users_info import DEFAULT_USERS_INFO
from test.helpers.stubs.services import PassportStub
from mpfs.core.services.passport_service import passport


class OrganizationsTestCase(UserTestCaseMixin, SupportApiTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.INTERNAL
    api_version = 'v1'

    def setup_method(self, method):
        super(OrganizationsTestCase, self).setup_method(method)
        self.create_user(self.uid, noemail=1)

    def test_make_user_organization(self):
        # создаём из пользователя организацию
        resp = self.client.post('disk/organizations', uid=self.uid)
        assert resp.status_code == 201
        assert 'disk/organizations/%s' % self.uid in json.loads(resp.content)['href']

        # папка должна появиться в корне диска организации
        resp = self.json_ok('info',
                            opts={'uid': self.uid, 'path': '/disk/.organization', 'meta': ''})
        # папка должна быть общей
        assert 'group' in resp['meta']

    @parameterized.expand([('GET',), ('POST',)])
    def test_request_for_noninit_user_with_passport_bad_response(self, method):
        url = 'disk/organizations'
        uid = '123456789'
        user_info = deepcopy(DEFAULT_USERS_INFO[self.uid])
        user_info['uid'] = uid
        with self.specified_client(scopes=['yadisk:all'], uid=uid):
            with PassportStub(userinfo=user_info) as stub:
                stub.subscribe.side_effect = passport.errors_map['accountwithpasswordrequired']
                response = self.client.request(method, url, uid=uid)
                assert response.status_code == 403
                content = json.loads(response.content)
                assert content['error'] == 'DiskUnsupportedUserAccountTypeError'
                assert content['description'] == 'User account type is not supported.'

    @parameterized.expand([('GET',), ('POST',)])
    def test_create_for_blocked_user(self, method):
        url = 'disk/organizations'
        opts = {
            'uid': self.uid,
            'moderator': 'moderator',
            'comment': 'comment',
        }
        self.support_ok('block_user', opts)
        with self.specified_client(scopes=['yadisk:all'], uid=self.uid):
            response = self.client.request(method, url, uid=self.uid)
            content = json.loads(response.content)
            assert response.status_code == 403
            assert content['error'] == 'DiskUserBlockedError'
            assert content['description'] == 'User is blocked.'

    def test_request_for_blocked_user(self):
        resp = self.client.post('disk/organizations', uid=self.uid)
        assert resp.status_code == 201
        url = json.loads(resp.content)['href']
        opts = {
            'uid': self.uid,
            'moderator': 'moderator',
            'comment': 'comment',
        }
        self.support_ok('block_user', opts)
        with self.specified_client(scopes=['yadisk:all'], uid=self.uid):
            response = self.client.get(url, uid=self.uid)
            content = json.loads(response.content)
            assert response.status_code == 403
            assert content['error'] == 'DiskUserBlockedError'
            assert content['description'] == 'User is blocked.'

    @parameterized.expand([('GET', 200), ('POST', 201)])
    def test_request_for_noninit_user(self, method, http_code):
        url = 'disk/organizations'
        uid = '123456789'
        user_info = deepcopy(DEFAULT_USERS_INFO[self.uid])
        user_info['uid'] = uid
        with self.specified_client(scopes=['yadisk:all'], uid=uid):
            with PassportStub(userinfo=user_info):
                response = self.client.request(method, url, uid=uid)
                # init and retry with _auto_initialize_user
                assert response.status_code == http_code


class OrganizationsOwnerTestCase(UserTestCaseMixin, SupportApiTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.INTERNAL
    api_version = 'v1'
    organization_id = user_1.uid

    def setup_method(self, method):
        super(OrganizationsOwnerTestCase, self).setup_method(method)
        self.create_user(self.uid, noemail=1)
        self.create_user(self.organization_id, noemail=1)
        # создаём из пользователя организацию
        resp = self.client.post('disk/organizations', uid=self.organization_id)
        assert resp.status_code == 201

    def test_owner_has_access_to_organization(self):
        uri_tmpl = '/disk/organizations/%s/resources?path=disk%%3A%%2Ftest'
        expected_uri = uri_tmpl % self.organization_id

        resp = self.client.put('disk/organizations/%s/resources' % self.organization_id,
                               query={'path': 'disk:/test'},
                               uid=self.organization_id)

        assert resp.status_code == 201
        assert expected_uri in resp.content

    def test_invite_user_to_organization_by_email(self):
        grant_access_uri = 'disk/organizations/%s/resources/grant_access' % self.organization_id
        resp = self.client.put(grant_access_uri,
                               query={'path': 'disk:/test', 'email': default_user.email},
                               uid=self.organization_id)
        assert resp.status_code == 200

        self.json_ok('info', opts={'uid': self.uid, 'path': '/disk/.organization', 'meta': ''})

    def test_request_for_noninit_user_with_passport_bad_response(self):
        """Протестировать, что при запросе информации пользователем с аккаунтом без пароля он получит 403 ошибку."""
        uid = '123456789'
        user_info = deepcopy(DEFAULT_USERS_INFO[self.uid])
        user_info['uid'] = uid
        with self.specified_client(scopes=['yadisk:all'], uid=uid):
            with PassportStub(userinfo=user_info) as stub:
                stub.subscribe.side_effect = passport.errors_map['accountwithpasswordrequired']
                response = self.client.put(
                    'disk/organizations/%s/resources' % self.organization_id,
                    query={'path': 'disk:/test'},
                    uid=uid)
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
            response = self.client.put(
                'disk/organizations/%s/resources' % self.organization_id,
                query={'path': 'disk:/test'},
                uid=self.uid)
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
                response = self.client.put(
                    'disk/organizations/%s/resources' % self.organization_id,
                    query={'path': 'disk:/test'},
                    uid=uid)
                # init and retry with _auto_initialize_user
                assert response.status_code == 403

    def test_info_for_noninit_user_with_passport_bad_response(self):
        """Протестировать, что при запросе информации пользователем с аккаунтом без пароля он получит 403 ошибку."""
        uid = '123456789'
        user_info = deepcopy(DEFAULT_USERS_INFO[self.uid])
        user_info['uid'] = uid
        with self.specified_client(scopes=['yadisk:all'], uid=uid):
            with PassportStub(userinfo=user_info) as stub:
                stub.subscribe.side_effect = passport.errors_map['accountwithpasswordrequired']
                response = self.client.get(
                    'disk/organizations/%s' % self.organization_id,
                    uid=uid)
                assert response.status_code == 403
                content = json.loads(response.content)
                assert content['error'] == 'DiskUnsupportedUserAccountTypeError'
                assert content['description'] == 'User account type is not supported.'

    def test_info_for_blocked_user(self):
        opts = {
            'uid': self.uid,
            'moderator': 'moderator',
            'comment': 'comment',
        }
        self.support_ok('block_user', opts)
        with self.specified_client(scopes=['yadisk:all'], uid=self.uid):
            response = self.client.get(
                'disk/organizations/%s' % self.organization_id,
                uid=self.uid)
            content = json.loads(response.content)
            assert response.status_code == 403
            assert content['error'] == 'DiskUserBlockedError'
            assert content['description'] == 'User is blocked.'

    def test_info_for_noninit_user(self):
        uid = '123456789'
        user_info = deepcopy(DEFAULT_USERS_INFO[self.uid])
        user_info['uid'] = uid
        with self.specified_client(scopes=['yadisk:all'], uid=uid):
            with PassportStub(userinfo=user_info):
                response = self.client.get(
                    'disk/organizations/%s' % self.organization_id,
                    uid=uid)
                # init and retry with _auto_initialize_user
                assert response.status_code == 403


class OrganizationsGrantedUserTestCase(UserTestCaseMixin, UploadFileTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.INTERNAL
    api_version = 'v1'
    organization_id = user_1.uid

    def setup_method(self, method):
        super(OrganizationsGrantedUserTestCase, self).setup_method(method)
        self.create_user(self.uid, noemail=1)
        self.create_user(self.organization_id, noemail=1)
        # создаём из пользователя организацию
        resp = self.client.post('disk/organizations', uid=self.organization_id)
        assert resp.status_code == 201
        # даём доступ другому пользователю
        grant_access_uri = 'disk/organizations/%s/resources/grant_access' % self.organization_id
        resp = self.client.put(grant_access_uri,
                               query={'path': 'disk:/test', 'email': default_user.email},
                               uid=self.organization_id)
        assert resp.status_code == 200

    def test_create_folder_upload_and_download(self):
        # создаём папку
        resp = self.client.put('disk/organizations/%s/resources' % self.organization_id,
                               query={'path': 'disk:/test'},
                               uid=self.uid)
        assert resp.status_code == 201
        resp = self.client.get('disk/organizations/%s/resources' % self.organization_id,
                               query={'path': 'disk:/test'},
                               uid=self.uid)
        assert resp.status_code == 200

        # получаем ссылку на загрузку
        resp = self.client.get('disk/organizations/%s/resources/upload' % self.organization_id,
                               query={'path': 'disk:/test/my_file.txt'},
                               uid=self.uid)
        assert resp.status_code == 200

        self.upload_file(self.organization_id, '/disk/.organization/test/my_file.txt')

        # получаем ссылку на скачивание из неё
        resp = self.client.get('disk/organizations/%s/resources/download' % self.organization_id,
                               query={'path': 'disk:/test/my_file.txt'},
                               uid=self.uid)
        assert resp.status_code == 200
