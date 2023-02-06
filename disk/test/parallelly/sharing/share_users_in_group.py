# -*- coding: utf-8 -*-
import itertools

from mock import patch
from nose_parameterized import parameterized

from test.common.sharing import CommonSharingMethods
import mpfs.core.base
from mpfs.common.static.codes import GROUP_NOT_FOUND
from mpfs.core.services.passport_service import Passport


class ShareUsersInGroupTestCase(CommonSharingMethods):
    """Тестовый класс для ручки `share_users_in_group`.
    """

    def setup_method(self, method):
        super(ShareUsersInGroupTestCase, self).setup_method(method)

    def test_no_gid_raise_exception(self):
        """Протестировать срабатываение исключения типа :class:`~GroupNotFound`,
        если в ручку не передан gid.
        """
        resp = self.json_error('share_users_in_group', {'gid': '', 'uid': self.uid})
        assert resp['code'] == GROUP_NOT_FOUND

    def test_display_name_instead_of_login(self):
        """Протестировать, что вместо Логина отдаем `display_name`, чтобы
        не отдавать конфиденциальную информацию в ОП.
        """
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/Shared'})
        gid = self.create_group(uid=self.uid, path='/disk/Shared')

        with patch.object(
            Passport, 'userinfo',
            return_value={
                'display_name': 'Test display_name 567',
                'uid': 101010101010,
                'decoded_email': 'test-mpfs@yandex.ru',
                'avatar': '0/0-0'
            }
        ):
            resp = self.json_ok('share_users_in_group', {'gid': gid, 'uid': self.uid})
            assert resp['users'][0]['name'] == 'Test display_name 567'

    def _prepare_group(self):
        """Создать группу: 1 владелец, 2 участника, 2 приглашенных"""
        folder = '/disk/1'
        self.json_ok('mkdir', {'uid': self.uid, 'path': folder})
        gid = self.create_group(uid=self.uid, path=folder)

        # два пользователя вступило в группу, один только приглашенный
        self.create_user(self.uid_1)
        hsh = self.invite_user(uid=self.uid_1, owner=self.uid, email=self.email_1, rights=660, path=folder)
        self.activate_invite(uid=self.uid_1, hash=hsh)

        self.create_user(self.uid_3)
        hsh = self.invite_user(uid=self.uid_3, owner=self.uid, email=self.email_3, rights=660, path=folder)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        self.create_user(self.uid_6)
        self.invite_user(uid=self.uid_6, owner=self.uid, email=self.email_6, rights=660, path=folder)

        self.create_user(self.uid_7)
        self.invite_user(uid=self.uid_7, owner=self.uid, email=self.email_7, rights=660, path=folder)

        return gid

    def test_pagination(self):
        """Тестируем пагинацию"""
        gid = self._prepare_group()
        # без параметров отдаем всё
        resp = self.json_ok('share_users_in_group', {'gid': gid, 'uid': self.uid})
        all_uids = [i['uid'] for i in resp['users']]
        assert len(all_uids) == 5
        assert all_uids[0] == self.uid  # владелец
        assert set(all_uids[1:3]) == set((self.uid_1, self.uid_3))  # 2 участника
        assert set(all_uids[3:5]) == set((self.uid_6, self.uid_7))  # 2 приглашенных
        assert resp['iteration_key'] is None

        with patch.object(mpfs.core.base.ShareUsersInGroupIterationKey, 'LIMIT', new=2):
            resp = self.json_ok('share_users_in_group', {'uid': self.uid, 'gid': gid, 'iteration_key': ''})
            assert [u['uid'] for u in resp['users']] == all_uids[:2]
            assert resp['iteration_key']

            resp = self.json_ok('share_users_in_group', {'uid': self.uid, 'gid': gid, 'iteration_key': resp['iteration_key']})
            assert [u['uid'] for u in resp['users']] == all_uids[2:4]
            assert resp['iteration_key']

            resp = self.json_ok('share_users_in_group', {'uid': self.uid, 'gid': gid, 'iteration_key': resp['iteration_key']})
            assert [u['uid'] for u in resp['users']] == all_uids[4:6]
            assert resp['iteration_key'] is None

    def test_invited_in_response(self):
        """Владельцу отдаем приглашенных, остальным - нет"""
        gid = self._prepare_group()
        # запрос владельцем - отдаем приглашенных
        resp = self.json_ok('share_users_in_group', {'gid': gid, 'uid': self.uid})
        all_uids = {i['uid'] for i in resp['users']}
        assert len(all_uids) == 5
        # запрос участником - не отдаем приглашенных
        resp = self.json_ok('share_users_in_group', {'gid': gid, 'uid': self.uid_3})
        all_uids = {i['uid'] for i in resp['users']}
        assert len(all_uids) == 3
        assert self.uid_6 not in all_uids
        assert self.uid_7 not in all_uids

    def test_ignore_rejected(self):
        """Отказавшихся от инвайта не отдаем"""
        gid = self._prepare_group()
        # в начале 5
        resp = self.json_ok('share_users_in_group', {'gid': gid, 'uid': self.uid})
        all_uids = {i['uid'] for i in resp['users']}
        assert len(all_uids) == 5
        # один отказался
        hsh = self.json_ok('share_list_not_approved_folders', {'uid': self.uid_7})[0]['hash']
        self.json_ok('share_reject_invite', {'uid': self.uid_7, 'hash': hsh})
        # стало 4
        resp = self.json_ok('share_users_in_group', {'gid': gid, 'uid': self.uid})
        all_uids = {i['uid'] for i in resp['users']}
        assert len(all_uids) == 4
        assert self.uid_7 not in all_uids

    def test_exclude_b2b(self):
        """Тестируем фильтр пользователей из списка участников для той же организации, что и владелец"""
        folder = '/disk/1'
        self.json_ok('mkdir', {'uid': self.uid, 'path': folder})
        self.json_ok('user_make_b2b', {'uid': self.uid, 'b2b_key': 'b2b_key_1'})
        gid = self.create_group(uid=self.uid, path=folder)

        # обычный пользователь
        self.create_user(self.uid_1)
        hsh = self.invite_user(uid=self.uid_1, owner=self.uid, email=self.email_1, rights=660, path=folder)
        self.activate_invite(uid=self.uid_1, hash=hsh)
        # пользователь из той же организации
        self.create_user(self.uid_3)
        self.json_ok('user_make_b2b', {'uid': self.uid_3, 'b2b_key': 'b2b_key_1'})
        hsh = self.invite_user(uid=self.uid_3, owner=self.uid, email=self.email_3, rights=660, path=folder)
        self.activate_invite(uid=self.uid_3, hash=hsh)
        # пользователь из другой организации
        self.create_user(self.uid_6)
        self.json_ok('user_make_b2b', {'uid': self.uid_6, 'b2b_key': 'b2b_key_2'})
        hsh = self.invite_user(uid=self.uid_6, owner=self.uid, email=self.email_6, rights=660, path=folder)
        self.activate_invite(uid=self.uid_6, hash=hsh)

        # без флага отдаем всех
        resp = self.json_ok('share_users_in_group', {'gid': gid, 'uid': self.uid})
        all_uids = {i['uid'] for i in resp['users']}
        assert len(all_uids) == 4  # 1 владелец и 3 участника

        # убираем из той же организации
        resp = self.json_ok('share_users_in_group', {'gid': gid, 'uid': self.uid, 'exclude_b2b': 1})
        all_uids = {i['uid'] for i in resp['users']}
        assert len(all_uids) == 3
        assert self.uid_3 not in all_uids


class ShareUIDsInGroupTestCase(CommonSharingMethods):
    """Тестовый класс для `share_uids_in_group`."""

    def request_share_uids_in_group(self, path, by_resource_id=False, error_code=None, amount=None, offset=None):
        if by_resource_id:
            resource_id = self.json_ok(
                'info', {'uid': self.uid, 'path': path, 'meta': 'resource_id'})['meta']['resource_id']
            args = {'uid': self.uid, 'resource_id': resource_id}
        else:
            file_id = self.json_ok(
                'info', {'uid': self.uid, 'path': path, 'meta': 'file_id'})['meta']['file_id']
            args = {'file_id': file_id, 'uid': self.uid, 'owner_uid': self.uid}

        if amount:
            args['amount'] = amount
        if offset:
            args['offset'] = offset

        if error_code:
            return self.json_error('share_uids_in_group', args, code=error_code)
        return self.json_ok('share_uids_in_group', args)

    def setup_method(self, method):
        super(ShareUIDsInGroupTestCase, self).setup_method(method)
        self.shared_folder_path = '/disk/shared'
        self.json_ok('mkdir', {'uid': self.uid, 'path': self.shared_folder_path})
        self.create_group(uid=self.uid, path=self.shared_folder_path)
        self.json_ok('user_init', {'uid': self.uid_1})
        hash_ = self.invite_user(uid=self.uid_1, email=self.email_1, path=self.shared_folder_path)
        self.activate_invite(uid=self.uid_1, hash=hash_)
        self.json_ok('user_init', {'uid': self.uid_3})
        self.uid3_hash = self.invite_user(uid=self.uid_3, email=self.email_3, path=self.shared_folder_path)

    @parameterized.expand([('file_id', ), ('resource_id', )])
    def test_non_approved_users_is_not_included(self, test_type):
        """Test users not in group is not included"""
        response = self.request_share_uids_in_group(
            self.shared_folder_path, by_resource_id=(test_type == 'resource_id'))
        returned_uids = {u['uid'] for u in response['users']}
        assert str(self.uid_3) not in returned_uids  # не подтвердил инвайт
        assert str(self.uid) in returned_uids
        assert str(self.uid_1) in returned_uids

    @parameterized.expand([('file_id',), ('resource_id',)])
    def test_fields_set_in_user_data(self, test_type):
        """Test users have only main field, and nothing else 

        Format: {'users':[{'status':..., 'uid':...},..]}
        """
        response = self.request_share_uids_in_group(
            self.shared_folder_path, by_resource_id=(test_type == 'resource_id'))
        fields_set = {'uid', 'status'}
        for u in response['users']:
            assert not fields_set - u.viewkeys()

    @parameterized.expand([('file_id',), ('resource_id',)])
    def test_non_shared_file_id_exception(self, test_type):
        """Test 404 for non-shared resource"""
        non_shared_folder_path = '/disk/non_shared'
        self.json_ok('mkdir', {'uid': self.uid, 'path': non_shared_folder_path})
        self.json_ok('user_init', {'uid': self.uid_1})
        self.request_share_uids_in_group(
            non_shared_folder_path, by_resource_id=(test_type == 'resource_id'), error_code=GROUP_NOT_FOUND)

    @parameterized.expand([('file_id',), ('resource_id',)])
    def test_amount(self, test_type):
        """Test `amount` parameter for limit results list"""
        self.xiva_subscribe(self.uid_1)
        self.xiva_subscribe(self.uid_3)
        self.activate_invite(uid=self.uid_3, hash=self.uid3_hash)
        response = self.request_share_uids_in_group(
            self.shared_folder_path, by_resource_id=(test_type == 'resource_id'), amount=1)
        returned_uids = {u['uid'] for u in response['users']}
        assert len(returned_uids) == 1

    @parameterized.expand([('file_id',), ('resource_id',)])
    def test_offset_with_amount_by_file_id(self, test_type):
        """Test `amount` and `offset` parameters together"""
        self.xiva_subscribe(self.uid_1)
        self.xiva_subscribe(self.uid_3)
        self.activate_invite(uid=self.uid_3, hash=self.uid3_hash)
        response = self.request_share_uids_in_group(
            self.shared_folder_path, by_resource_id=(test_type == 'resource_id'), amount=1, offset=1)
        returned_uids = {u['uid'] for u in response['users']}
        assert len(returned_uids) == 1
