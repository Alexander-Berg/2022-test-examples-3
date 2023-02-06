# -*- coding: utf-8 -*-
from mock import patch

from test.common.sharing import CommonSharingMethods
from test.fixtures import users
from mpfs.core.social.share.processor import ShareProcessor
from mpfs.core.user.base import User
from mpfs.core.social.share.group import Group
from mpfs.core.services.directory_service import DirectoryContact, DirectoryService


class ShareB2bSynchronizeAccessTestCase(CommonSharingMethods):

    def test_group_not_exists(self):
        self.json_error('share_b2b_synchronize_access', {'uid': self.uid, 'gid': '1234'}, code=107)

    def test_owner_not_b2b(self):
        path = '/disk/1'
        self.json_ok('mkdir', {'uid': self.uid, 'path': path})
        gid = self.create_group(uid=self.uid, path=path)
        self.json_error('share_b2b_synchronize_access', {'uid': self.uid, 'gid': gid}, code=196)

    def test_create_operation(self):
        self.json_ok('user_make_b2b', {'uid': self.uid, 'b2b_key': 'test_b2b_key'})
        path = '/disk/1'
        self.json_ok('mkdir', {'uid': self.uid, 'path': path})
        gid = self.create_group(uid=self.uid, path=path)

        resp = self.json_ok('share_b2b_synchronize_access', {'uid': self.uid, 'gid': gid})
        assert 'oid' in resp
        assert 'type' in resp
        assert resp['type'] == 'share'


class SynchronizeWithB2bTestCase(CommonSharingMethods):
    b2b_key = 'test_b2b_key'

    def setup_method(self, method):
        super(SynchronizeWithB2bTestCase, self).setup_method(method)
        self.json_ok('user_make_b2b', {'uid': self.uid, 'b2b_key': self.b2b_key})
        self.json_ok('user_init', {'uid': users.usr_1.uid, 'b2b_key': self.b2b_key})
        self.json_ok('user_init', {'uid': users.usr_2.uid, 'b2b_key': self.b2b_key})
        self.json_ok('user_init', {'uid': users.usr_3.uid, 'b2b_key': self.b2b_key})

        path = '/disk/1'
        self.json_ok('mkdir', {'uid': self.uid, 'path': path})
        self.gid = self.create_group(uid=self.uid, path=path)
        self.user = User(self.uid)
        self.group = Group.load(gid=self.gid)

    @staticmethod
    def mock_get_shared_folder_members(rw_test_users, ro_test_users):
        as_directory = lambda u: {'id': u.uid, 'email': u.email}
        rw_contacts = [DirectoryContact(as_directory(u)) for u in rw_test_users]
        ro_contacts = [DirectoryContact(as_directory(u)) for u in ro_test_users]
        return_value = (rw_contacts, ro_contacts)
        return patch.object(DirectoryService, 'get_shared_folder_members', return_value=return_value)

    def _assert_group_members(self, members_and_rights_set):
        users_in_group = self.json_ok('share_users_in_group', {'uid': self.uid, 'gid': self.gid})['users']
        assert len(users_in_group) == len(members_and_rights_set) + 1  # + owner
        uids_and_rights = {(u['uid'], u['rights']) for u in users_in_group[1:]}
        assert uids_and_rights == members_and_rights_set

    def test_no_users_in_directory(self):
        with self.mock_get_shared_folder_members([], []):
            ShareProcessor().synchronize_with_b2b(self.user, self.group)

        users_in_group = self.json_ok('share_users_in_group', {'uid': self.uid, 'gid': self.gid})['users']
        assert len(users_in_group) == 1
        assert users_in_group[0]['uid'] == self.uid

    def test_invite_3_users(self):
        with self.mock_get_shared_folder_members([users.usr_1, users.usr_2], [users.usr_3]):
            ShareProcessor().synchronize_with_b2b(self.user, self.group)
        self._assert_group_members({(users.usr_1.uid, 660), (users.usr_2.uid, 660), (users.usr_3.uid, 640)})

    def test_resync_kick(self):
        with self.mock_get_shared_folder_members([users.usr_1, users.usr_2], [users.usr_3]):
            ShareProcessor().synchronize_with_b2b(self.user, self.group)
        with self.mock_get_shared_folder_members([users.usr_2], []):
            ShareProcessor().synchronize_with_b2b(self.user, self.group)
        self._assert_group_members({(users.usr_2.uid, 660)})

    def test_resync_invite(self):
        with self.mock_get_shared_folder_members([users.usr_1], [users.usr_3]):
            ShareProcessor().synchronize_with_b2b(self.user, self.group)
        with self.mock_get_shared_folder_members([users.usr_1, users.usr_2], [users.usr_3]):
            ShareProcessor().synchronize_with_b2b(self.user, self.group)
        self._assert_group_members({(users.usr_1.uid, 660), (users.usr_2.uid, 660), (users.usr_3.uid, 640)})

    def test_change_rights(self):
        with self.mock_get_shared_folder_members([users.usr_1], [users.usr_2]):
            ShareProcessor().synchronize_with_b2b(self.user, self.group)

        with self.mock_get_shared_folder_members([], [users.usr_1, users.usr_2]):
            ShareProcessor().synchronize_with_b2b(self.user, self.group)
        self._assert_group_members({(users.usr_1.uid, 640), (users.usr_2.uid, 640)})

        with self.mock_get_shared_folder_members([users.usr_1, users.usr_2], []):
            ShareProcessor().synchronize_with_b2b(self.user, self.group)
        self._assert_group_members({(users.usr_1.uid, 660), (users.usr_2.uid, 660)})

    def test_no_exists_user_no_effect(self):
        with self.mock_get_shared_folder_members([users.usr_1], [users.usr_5]):
            ShareProcessor().synchronize_with_b2b(self.user, self.group)
        self._assert_group_members({(users.usr_1.uid, 660)})

    def test_aviod_common_users(self):
        # пользователь из другой организации
        self.json_ok('user_init', {'uid': users.usr_4.uid, 'b2b_key': 'another_b2b_user'})
        # обычный пользователь
        self.json_ok('user_init', {'uid': users.usr_5.uid})
        for user in (users.usr_5, users.usr_4):
            result = self.json_ok('share_invite_user', {'uid': self.uid, 'gid': self.gid, 'rights': 660,
                                                        'universe_login': user.email, 'universe_service': 'email'})
            self.json_ok('share_activate_invite', {'uid': user.uid, 'hash': result['hash']})

        self._assert_group_members({(users.usr_4.uid, 660), (users.usr_5.uid, 660)})

        with self.mock_get_shared_folder_members([users.usr_1], [users.usr_2]):
            ShareProcessor().synchronize_with_b2b(self.user, self.group)
        self._assert_group_members({
            (users.usr_4.uid, 660), (users.usr_5.uid, 660),
            (users.usr_1.uid, 660), (users.usr_2.uid, 640),
        })
