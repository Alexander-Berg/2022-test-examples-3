# -*- coding: utf-8 -*-
import json
import mock
from base_suit import UserTestCaseMixin
from common.sharing import CommonSharingMethods
from mpfs.common.static import tags
from mpfs.platform.permissions import AllowByClientIdPermission
from mpfs.platform.v1.case.permissions import YandexUserPermission
from parallelly.api.disk.base import DiskApiTestCase
from mpfs.platform.v1.case.qa.handlers import DiskSharedFoldersCreateHandler, DiskSharedFoldersCreateInviteHandler, \
    DiskSharedFoldersAcceptInviteHandler, DiskSharedFoldersRejectInviteHandler, DiskSharedFoldersSetRightsHandler, \
    DiskSharedFoldersRevokeAccessHandler, \
    DiskSharedFoldersRemoveHandler, DiskSharedFoldersListIncomingInvitesHandler
from test.fixtures.users import share_slave, share_master, default_user


class ShareCreateGroupTestCase(UserTestCaseMixin, CommonSharingMethods, DiskApiTestCase):
    api_mode = tags.platform.EXTERNAL
    api_version = 'v1'
    url = 'case/qa/disk/shared-folders'
    client_id = '12345678901234567890123456789012'
    uid = share_master.uid

    def setup_method(self, method):
        super(ShareCreateGroupTestCase, self).setup_method(method)
        self.create_user(self.uid)
        self.create_user(default_user.uid)

    def test_create_share_group(self):
        path = '/disk/group_folder'
        self.json_ok('mkdir', {'uid': self.uid, 'path': path})
        permissions = AllowByClientIdPermission([self.client_id]) & YandexUserPermission()
        with mock.patch.object(DiskSharedFoldersCreateHandler, 'permissions', permissions), \
                self.specified_client(id=self.client_id, uid=self.uid):
            resp = self.client.post(self.url, query={'path': '/group_folder'})
            payload = json.loads(resp.get_result())
            assert 'shared_folder_id' in payload

    def test_create_share_group_when_folder_not_exist(self):
        permissions = AllowByClientIdPermission([self.client_id]) & YandexUserPermission()
        with mock.patch.object(DiskSharedFoldersCreateHandler, 'permissions', permissions), \
                self.specified_client(id=self.client_id, uid=self.uid):
            resp = self.client.post(self.url, query={'path': '/group_folder'})
            resource = json.loads(resp.get_result())
            assert 'error' in resource
            assert resource['error'] == 'DiskNotFoundError'

    def test_create_share_already_shared_folder(self):
        path = '/disk/group_folder'
        self.json_ok('mkdir', {'uid': self.uid, 'path': path})
        resp = self.json_ok('share_create_group', {'uid': self.uid, 'path': path})
        gid = resp['gid']
        permissions = AllowByClientIdPermission([self.client_id]) & YandexUserPermission()
        with mock.patch.object(DiskSharedFoldersCreateHandler, 'permissions', permissions), \
                self.specified_client(id=self.client_id, uid=self.uid):
            resp = self.client.post(self.url, query={'path': '/group_folder'})
            payload = json.loads(resp.get_result())
            assert 'shared_folder_id' in payload
            assert payload['shared_folder_id'] == gid

    def test_unable_to_create_group_when_user_not_test(self):
        path = '/disk/group_folder'
        self.json_ok('mkdir', {'uid': default_user.uid, 'path': path})
        permissions = AllowByClientIdPermission([self.client_id]) & YandexUserPermission()
        with mock.patch.object(DiskSharedFoldersCreateHandler, 'permissions', permissions), \
                self.specified_client(id=self.client_id, uid=default_user.uid):
            resp = self.client.post(self.url, query={'path': '/group_folder'})
            payload = json.loads(resp.get_result())
            assert resp.status_code == 403
            assert 'error' in payload


class ShareSendInviteTestCase(UserTestCaseMixin, CommonSharingMethods, DiskApiTestCase):
    api_mode = tags.platform.EXTERNAL
    api_version = 'v1'
    url = 'case/qa/disk/shared-folders/%s/invites'
    client_id = '12345678901234567890123456789012'
    uid = share_master.uid

    def setup_method(self, method):
        super(ShareSendInviteTestCase, self).setup_method(method)
        self.create_user(self.uid)
        self.create_user(share_slave.uid)
        self.create_user(default_user.uid)

    def test_invite_user(self):
        target_user = share_slave
        path = '/disk/group_folder'
        self.json_ok('mkdir', {'uid': self.uid, 'path': path})
        resp = self.json_ok('share_create_group', {'uid': self.uid, 'path': path})
        gid = resp['gid']
        permissions = AllowByClientIdPermission([self.client_id]) & YandexUserPermission()
        with mock.patch.object(DiskSharedFoldersCreateInviteHandler, 'permissions', permissions), \
                self.specified_client(id=self.client_id, uid=self.uid):
            prepared_url = self.url % gid
            data = {
                'email': target_user.email,
                'rights': 'RO'
            }
            resp = self.client.post(prepared_url, data)
            payload = json.loads(resp.get_result())
            assert len(payload) == 0
            resp = self.json_ok('share_list_not_approved_folders', {'uid': target_user.uid})
            assert len(resp) == 1

    def test_invite_not_test_user(self):
        target_user = share_slave
        path = '/disk/group_folder'
        self.json_ok('mkdir', {'uid': self.uid, 'path': path})
        resp = self.json_ok('share_create_group', {'uid': self.uid, 'path': path})
        gid = resp['gid']
        permissions = AllowByClientIdPermission([self.client_id]) & YandexUserPermission()
        with mock.patch.object(DiskSharedFoldersCreateInviteHandler, 'permissions', permissions), \
                self.specified_client(id=self.client_id, uid=self.uid):
            prepared_url = self.url % gid
            data = {
                'email': default_user.email,
                'rights': 'RO'
            }
            resp = self.client.post(prepared_url, data)
            payload = json.loads(resp.get_result())
            assert resp.status_code == 403
            assert 'error' in payload
            resp = self.json_ok('share_list_not_approved_folders', {'uid': target_user.uid})
            assert len(resp) == 0


class ShareActivateInviteTestCase(UserTestCaseMixin, CommonSharingMethods, DiskApiTestCase):
    api_mode = tags.platform.EXTERNAL
    api_version = 'v1'
    url = 'case/qa/disk/shared-folders/incoming-invites/%s/accept'
    client_id = '12345678901234567890123456789012'
    uid = share_master.uid

    def setup_method(self, method):
        super(ShareActivateInviteTestCase, self).setup_method(method)
        self.create_user(self.uid)
        self.create_user(share_slave.uid)
        self.create_user(default_user.uid)

    def test_activate_invite(self):
        target_user = share_slave
        path = '/disk/group_folder'
        self.json_ok('mkdir', {'uid': self.uid, 'path': path})
        resp = self.json_ok('share_create_group', {'uid': self.uid, 'path': path})
        gid = resp['gid']
        resp = self.json_ok('share_invite_user', {'uid': self.uid, 'gid': gid, 'rights': 660, 'locale': 'ru',
                                                  'universe_login': target_user.email, 'universe_service': 'email',
                                                  'user_avatar': 'http://localhost/echo', 'user_name': 'mpfs-test-1'})
        invite_hash = resp['hash']
        permissions = AllowByClientIdPermission([self.client_id]) & YandexUserPermission()
        with mock.patch.object(DiskSharedFoldersAcceptInviteHandler, 'permissions', permissions), \
                self.specified_client(id=self.client_id, uid=target_user.uid):
            prepared_url = self.url % invite_hash
            resp = self.client.put(prepared_url)
            payload = json.loads(resp.get_result())
            assert len(payload) == 0

    def test_activate_invite_for_non_test_user(self):
        path = '/disk/group_folder'
        self.json_ok('mkdir', {'uid': self.uid, 'path': path})
        resp = self.json_ok('share_create_group', {'uid': self.uid, 'path': path})
        gid = resp['gid']
        resp = self.json_ok('share_invite_user', {'uid': self.uid, 'gid': gid, 'rights': 660, 'locale': 'ru',
                                                  'universe_login': default_user.email, 'universe_service': 'email',
                                                  'user_avatar': 'http://localhost/echo', 'user_name': 'mpfs-test-1'})
        invite_hash = resp['hash']
        permissions = AllowByClientIdPermission([self.client_id]) & YandexUserPermission()
        with mock.patch.object(DiskSharedFoldersAcceptInviteHandler, 'permissions', permissions), \
                self.specified_client(id=self.client_id, uid=default_user.uid):
            prepared_url = self.url % invite_hash
            resp = self.client.put(prepared_url)
            payload = json.loads(resp.get_result())
            assert resp.status_code == 403
            assert 'error' in payload


class ShareRejectInviteGroupTestCase(UserTestCaseMixin, CommonSharingMethods, DiskApiTestCase):
    api_mode = tags.platform.EXTERNAL
    api_version = 'v1'
    url = 'case/qa/disk/shared-folders/incoming-invites/%s/reject'
    client_id = '12345678901234567890123456789012'
    uid = share_master.uid

    def setup_method(self, method):
        super(ShareRejectInviteGroupTestCase, self).setup_method(method)
        self.create_user(self.uid)
        self.create_user(share_slave.uid)
        self.xiva_subscribe(share_slave.uid)

    def test_reject_invite(self):
        target_user = share_slave
        path = '/disk/group_folder'
        self.json_ok('mkdir', {'uid': self.uid, 'path': path})
        resp = self.json_ok('share_create_group', {'uid': self.uid, 'path': path})
        gid = resp['gid']
        resp = self.json_ok('share_invite_user', {'uid': self.uid, 'gid': gid, 'rights': 660, 'locale': 'ru',
                                                  'universe_login': target_user.email, 'universe_service': 'email',
                                                  'user_avatar': 'http://localhost/echo', 'user_name': 'mpfs-test-1'})
        invite_hash = resp['hash']

        permissions = AllowByClientIdPermission([self.client_id]) & YandexUserPermission()
        with mock.patch.object(DiskSharedFoldersRejectInviteHandler, 'permissions', permissions), \
                self.specified_client(id=self.client_id, uid=target_user.uid):
            prepared_url = self.url % invite_hash
            resp = self.client.put(prepared_url)
            payload = json.loads(resp.get_result())
            assert not payload

    def test_reject_invite_for_non_test_user(self):
        path = '/disk/group_folder'
        self.json_ok('mkdir', {'uid': self.uid, 'path': path})
        resp = self.json_ok('share_create_group', {'uid': self.uid, 'path': path})
        gid = resp['gid']
        resp = self.json_ok('share_invite_user', {'uid': self.uid, 'gid': gid, 'rights': 660, 'locale': 'ru',
                                                  'universe_login': default_user.email, 'universe_service': 'email',
                                                  'user_avatar': 'http://localhost/echo', 'user_name': 'mpfs-test-1'})
        invite_hash = resp['hash']

        permissions = AllowByClientIdPermission([self.client_id]) & YandexUserPermission()
        with mock.patch.object(DiskSharedFoldersRejectInviteHandler, 'permissions', permissions), \
                self.specified_client(id=self.client_id, uid=default_user.uid):
            prepared_url = self.url % invite_hash
            resp = self.client.put(prepared_url)
            payload = json.loads(resp.get_result())
            assert resp.status_code == 403
            assert 'error' in payload


class ShareChangeRightsTestCase(UserTestCaseMixin, CommonSharingMethods, DiskApiTestCase):
    api_mode = tags.platform.EXTERNAL
    api_version = 'v1'
    url = 'case/qa/disk/shared-folders/%s/set-rights'
    client_id = '12345678901234567890123456789012'
    uid = share_master.uid

    def setup_method(self, method):
        super(ShareChangeRightsTestCase, self).setup_method(method)
        self.create_user(self.uid)
        self.create_user(share_slave.uid)
        self.create_user(default_user.uid)

    def test_change_rights(self):
        target_user = share_slave
        path = '/disk/group_folder'
        self.json_ok('mkdir', {'uid': self.uid, 'path': path})
        resp = self.json_ok('share_create_group', {'uid': self.uid, 'path': path})
        gid = resp['gid']
        self.json_ok('share_invite_user', {'uid': self.uid, 'gid': gid, 'rights': 660, 'locale': 'ru',
                                           'universe_login': target_user.email, 'universe_service': 'email',
                                           'user_avatar': 'http://localhost/echo', 'user_name': 'mpfs-test-1',
                                           'auto_accept': 1})
        permissions = AllowByClientIdPermission([self.client_id]) & YandexUserPermission()
        with mock.patch.object(DiskSharedFoldersSetRightsHandler, 'permissions', permissions), \
                self.specified_client(id=self.client_id, uid=self.uid):
            data = {
                'email': target_user.email,
                'rights': 'RO'
            }
            prepared_url = self.url % gid
            resp = self.client.put(prepared_url, data)
            payload = json.loads(resp.get_result())
            assert not payload

            # Check rights was changed
            resp = self.json_ok('share_list_joined_folders', {'uid': target_user.uid, 'meta': 'group'})
            assert resp
            assert int(resp[0]['meta']['group']['rights']) == 640

    def test_change_rights_for_invite(self):
        target_user = share_slave
        path = '/disk/group_folder'
        self.json_ok('mkdir', {'uid': self.uid, 'path': path})
        resp = self.json_ok('share_create_group', {'uid': self.uid, 'path': path})
        gid = resp['gid']
        self.json_ok('share_invite_user', {'uid': self.uid, 'gid': gid, 'rights': 660, 'locale': 'ru',
                                           'universe_login': target_user.email, 'universe_service': 'email',
                                           'user_avatar': 'http://localhost/echo', 'user_name': 'mpfs-test-1',
                                           'auto_accept': 0})
        permissions = AllowByClientIdPermission([self.client_id]) & YandexUserPermission()
        with mock.patch.object(DiskSharedFoldersSetRightsHandler, 'permissions', permissions), \
                self.specified_client(id=self.client_id, uid=self.uid):
            data = {
                'email': target_user.email,
                'rights': 'RO'
            }
            prepared_url = self.url % gid
            resp = self.client.put(prepared_url, data)
            payload = json.loads(resp.get_result())
            assert not payload

            # Check rights was changed
            resp = self.json_ok('share_list_not_approved_folders', {'uid': target_user.uid, 'meta': 'group'})
            assert resp
            assert int(resp[0]['rights']) == 640

    def test_change_rights_for_non_test_user(self):
        path = '/disk/group_folder'
        self.json_ok('mkdir', {'uid': self.uid, 'path': path})
        resp = self.json_ok('share_create_group', {'uid': self.uid, 'path': path})
        gid = resp['gid']
        self.json_ok('share_invite_user', {'uid': self.uid, 'gid': gid, 'rights': 660, 'locale': 'ru',
                                           'universe_login': default_user.email, 'universe_service': 'email',
                                           'user_avatar': 'http://localhost/echo', 'user_name': 'mpfs-test-1',
                                           'auto_accept': 1})
        permissions = AllowByClientIdPermission([self.client_id]) & YandexUserPermission()
        with mock.patch.object(DiskSharedFoldersSetRightsHandler, 'permissions', permissions), \
                self.specified_client(id=self.client_id, uid=self.uid):
            data = {
                'email': default_user.email,
                'rights': 'RO'
            }
            prepared_url = self.url % gid
            resp = self.client.put(prepared_url, data)
            payload = json.loads(resp.get_result())
            assert resp.status_code == 403
            assert 'error' in payload

            # Check rights was changed
            resp = self.json_ok('share_list_joined_folders', {'uid': default_user.uid, 'meta': 'group'})
            assert resp
            assert int(resp[0]['meta']['group']['rights']) == 660


class ShareKickFromGroupTestCase(UserTestCaseMixin, CommonSharingMethods, DiskApiTestCase):
    api_mode = tags.platform.EXTERNAL
    api_version = 'v1'
    url = 'case/qa/disk/shared-folders/%s/revoke-access'
    client_id = '12345678901234567890123456789012'
    uid = share_master.uid

    def setup_method(self, method):
        super(ShareKickFromGroupTestCase, self).setup_method(method)
        self.create_user(self.uid)
        self.create_user(share_slave.uid)
        self.create_user(default_user.uid)

    def test_kick_from_group(self):
        target_user = share_slave
        path = '/disk/group_folder'
        self.json_ok('mkdir', {'uid': self.uid, 'path': path})
        resp = self.json_ok('share_create_group', {'uid': self.uid, 'path': path})
        gid = resp['gid']
        self.json_ok('share_invite_user', {'uid': self.uid, 'gid': gid, 'rights': 660, 'locale': 'ru',
                                           'universe_login': target_user.email, 'universe_service': 'email',
                                           'user_avatar': 'http://localhost/echo', 'user_name': 'mpfs-test-1',
                                           'auto_accept': 1})
        permissions = AllowByClientIdPermission([self.client_id]) & YandexUserPermission()
        with mock.patch.object(DiskSharedFoldersRevokeAccessHandler, 'permissions', permissions), \
                self.specified_client(id=self.client_id, uid=self.uid):
            data = {
                'email': target_user.email
            }
            prepared_url = self.url % gid
            resp = self.client.put(prepared_url, data)
            payload = json.loads(resp.get_result())
            assert not payload

            # Check not group joined
            resp = self.json_ok('share_list_joined_folders', {'uid': target_user.uid, 'meta': 'group'})
            assert not resp

    def test_kick_from_group_for_not_test_user(self):
        path = '/disk/group_folder'
        self.json_ok('mkdir', {'uid': self.uid, 'path': path})
        resp = self.json_ok('share_create_group', {'uid': self.uid, 'path': path})
        gid = resp['gid']
        self.json_ok('share_invite_user', {'uid': self.uid, 'gid': gid, 'rights': 660, 'locale': 'ru',
                                           'universe_login': default_user.email, 'universe_service': 'email',
                                           'user_avatar': 'http://localhost/echo', 'user_name': 'mpfs-test-1',
                                           'auto_accept': 1})
        permissions = AllowByClientIdPermission([self.client_id]) & YandexUserPermission()
        with mock.patch.object(DiskSharedFoldersRevokeAccessHandler, 'permissions', permissions), \
                self.specified_client(id=self.client_id, uid=self.uid):
            data = {
                'email': default_user.email
            }
            prepared_url = self.url % gid
            resp = self.client.put(prepared_url, data)
            payload = json.loads(resp.get_result())
            assert resp.status_code == 403
            assert 'error' in payload

            # Check not group joined
            resp = self.json_ok('share_list_joined_folders', {'uid': default_user.uid, 'meta': 'group'})
            assert len(resp) == 1


class ShareUnshareFolderTestCase(UserTestCaseMixin, CommonSharingMethods, DiskApiTestCase):
    api_mode = tags.platform.EXTERNAL
    api_version = 'v1'
    url = 'case/qa/disk/shared-folders/%s/'
    client_id = '12345678901234567890123456789012'
    uid = share_master.uid

    def setup_method(self, method):
        super(ShareUnshareFolderTestCase, self).setup_method(method)
        self.create_user(self.uid)
        self.create_user(default_user.uid)

    def test_unshare_folder(self):
        path = '/disk/group_folder'
        self.json_ok('mkdir', {'uid': self.uid, 'path': path})
        resp = self.json_ok('share_create_group', {'uid': self.uid, 'path': path})
        assert 'gid' in resp
        gid = resp['gid']
        permissions = AllowByClientIdPermission([self.client_id]) & YandexUserPermission()
        with mock.patch.object(DiskSharedFoldersRemoveHandler, 'permissions', permissions), \
                self.specified_client(id=self.client_id, uid=self.uid):
            prepared_url = self.url % gid
            resp = self.client.delete(prepared_url)
            print(resp.get_result())

            # Check not group owned
            resp = self.json_ok('share_list_owned_folders', {'uid': self.uid, 'meta': 'group'})
            assert not resp

    def test_unshare_folder_for_non_test_user(self):
        path = '/disk/group_folder'
        self.json_ok('mkdir', {'uid': default_user.uid, 'path': path})
        resp = self.json_ok('share_create_group', {'uid': default_user.uid, 'path': path})
        assert 'gid' in resp
        gid = resp['gid']
        permissions = AllowByClientIdPermission([self.client_id]) & YandexUserPermission()
        with mock.patch.object(DiskSharedFoldersRemoveHandler, 'permissions', permissions), \
                self.specified_client(id=self.client_id, uid=default_user.uid):
            prepared_url = self.url % gid
            resp = self.client.delete(prepared_url)
            payload = json.loads(resp.get_result())
            assert resp.status_code == 403
            assert 'error' in payload

            # Check not group owned
            resp = self.json_ok('share_list_owned_folders', {'uid': default_user.uid, 'meta': 'group'})
            assert len(resp) == 1


class ShareListInvitesTestCase(UserTestCaseMixin, CommonSharingMethods, DiskApiTestCase):
    api_mode = tags.platform.EXTERNAL
    api_version = 'v1'
    url = 'case/qa/disk/shared-folders/incoming-invites'
    client_id = '12345678901234567890123456789012'
    uid = share_master.uid

    def setup_method(self, method):
        super(ShareListInvitesTestCase, self).setup_method(method)
        self.create_user(self.uid)
        self.create_user(share_slave.uid)
        self.create_user(default_user.uid)

    def test_list_incoming_invites(self):
        target_user = share_slave
        path = '/disk/group_folder'
        self.json_ok('mkdir', {'uid': self.uid, 'path': path})
        resp = self.json_ok('share_create_group', {'uid': self.uid, 'path': path})
        gid = resp['gid']
        self.json_ok('share_invite_user', {'uid': self.uid, 'gid': gid, 'rights': 660, 'locale': 'ru',
                                           'universe_login': target_user.email, 'universe_service': 'email',
                                           'user_avatar': 'http://localhost/echo', 'user_name': 'mpfs-test-1',
                                           'auto_accept': 0})
        permissions = AllowByClientIdPermission([self.client_id]) & YandexUserPermission()
        with mock.patch.object(DiskSharedFoldersListIncomingInvitesHandler, 'permissions', permissions), \
             self.specified_client(id=self.client_id, uid=target_user.uid):

            resp = self.client.get(self.url)
            payload = json.loads(resp.get_result())
            assert 'items' in payload
            assert len(payload['items'][0]['invite_key']) != 0
            assert len(payload['items'][0]['shared_folder_name']) !=0
            assert payload['items'][0]['size'] == 0
            resp = self.json_ok('share_list_not_approved_folders', {'uid': target_user.uid})
            assert len(resp) == 1

    def test_list_incoming_invites_non_test_user(self):
        target_user = share_slave
        path = '/disk/group_folder'
        self.json_ok('mkdir', {'uid': self.uid, 'path': path})
        resp = self.json_ok('share_create_group', {'uid': self.uid, 'path': path})
        gid = resp['gid']
        self.json_ok('share_invite_user', {'uid': self.uid, 'gid': gid, 'rights': 660, 'locale': 'ru',
                                           'universe_login': target_user.email, 'universe_service': 'email',
                                           'user_avatar': 'http://localhost/echo', 'user_name': 'mpfs-test-1',
                                           'auto_accept': 0})
        permissions = AllowByClientIdPermission([self.client_id]) & YandexUserPermission()
        with mock.patch.object(DiskSharedFoldersListIncomingInvitesHandler, 'permissions', permissions), \
             self.specified_client(id=self.client_id, uid=default_user.uid):

            resp = self.client.get(self.url)
            payload = json.loads(resp.get_result())
            assert resp.status_code == 403
            assert 'error' in payload
            resp = self.json_ok('share_list_not_approved_folders', {'uid': target_user.uid})
            assert len(resp) == 1


