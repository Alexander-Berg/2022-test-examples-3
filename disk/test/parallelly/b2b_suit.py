# -*- coding: utf-8 -*-
import os

import pytest
from mock import patch

import mpfs.engine.process

from test.common.sharing import CommonSharingMethods
from test.fixtures.users import user_1
from test.helpers import products
from test.helpers.size_units import MB
from test.helpers.stubs.services import DirectoryServiceStub

import mpfs.core.services.disk_service
from mpfs.common.static import codes
from mpfs.common.static.tags.push import Low, Full
from mpfs.common.util import mailer
from mpfs.core.billing.processing import notify
from mpfs.core.filesystem.quota import Quota
from mpfs.core.services.discovery_service import DiscoveryService
from mpfs.core.office import util
from mpfs.core.social.share.notifier import MailNotifier
from mpfs.core.user.base import User
from mpfs.core.user import common, standart
from mpfs.core.filesystem.dao.legacy import CollectionRoutedDatabase


class TestB2B(CommonSharingMethods):
    other_uid = user_1.uid

    space_10gb = 10737418240
    space_250gb = 268435456000
    space_1tb = 1099511627776

    base_space = space_10gb
    b2b_space = base_space + space_10gb

    def test_shared_folder_space(self):
        folder = '/disk/shared_folder'
        self.json_ok('user_make_b2b', {'uid': self.uid, 'b2b_key': 'true'})
        self.create_user(self.uid_3)
        self.json_ok('user_make_b2b', {'uid': self.uid_3, 'b2b_key': 'true'})

        owner_uid = self.uid
        invited_uid = self.uid_3

        self.json_ok('mkdir', {'uid': owner_uid, 'path': folder})
        self.xiva_subscribe(self.uid_3)
        hsh = self.invite_user(uid=invited_uid, owner=owner_uid, email=self.email_3, rights=660, path=folder)
        self.activate_invite(uid=invited_uid, hash=hsh)

        get_used = lambda uid: int(self.json_ok('user_info', {'uid': uid})['space']['used'])

        # Изначально у всех место свободно
        assert get_used(owner_uid) == get_used(invited_uid) == 0

        file_size = 7 * MB
        self.upload_file(self.uid, '%s/%s' % (folder, '1.txt'), file_data={'size': file_size})

        with patch('mpfs.core.user.utils.FEATURE_TOGGLES_B2B_IGNORE_SHARED_FOLDER_IN_QUOTA',
                   False), \
             patch('mpfs.core.user.utils.FEATURE_TOGGLES_IGNORE_SHARED_FOLDER_IN_QUOTA_FOR_ALL',
                False):
            # После загрузки файла в общую папку
            # У Владельца место занято
            assert get_used(owner_uid) == file_size
            # У Приглашенного (при выключенного фиче) место занято
            assert get_used(invited_uid) == file_size

        with patch('mpfs.core.user.utils.FEATURE_TOGGLES_B2B_IGNORE_SHARED_FOLDER_IN_QUOTA',
                   True), \
             patch('mpfs.core.user.utils.FEATURE_TOGGLES_IGNORE_SHARED_FOLDER_IN_QUOTA_FOR_ALL',
                False):
            # У Владельца место занято, даже если фича включена
            assert get_used(owner_uid) == file_size
            # У Приглашенного (при включенной фиче) место занято
            assert get_used(invited_uid) == 0

    @patch.object(common, 'FEATURE_TOGGLES_B2B_ADD_SPACE', new=True)
    @patch.object(standart, 'FEATURE_TOGGLES_B2B_ADD_SPACE', new=True)
    def test_b2b_user_init_add_additional_space(self):
        db = CollectionRoutedDatabase()
        assert 'b2b_key' not in db.user_index.find_one({'_id': self.uid})
        self.remove_user(self.uid)

        self.create_user(self.uid, b2b_key='true', add_services='b2b_10gb')

        assert 'true' == db.user_index.find_one({'_id': self.uid})['b2b_key']

        space_report = Quota().report(self.uid)
        assert self.b2b_space == space_report['limit']

    @patch.object(common, 'FEATURE_TOGGLES_B2B_ADD_SPACE', new=True)
    @patch.object(standart, 'FEATURE_TOGGLES_B2B_ADD_SPACE', new=True)
    def test_b2b_user_init_default_space(self):
        db = CollectionRoutedDatabase()
        assert 'b2b_key' not in db.user_index.find_one({'_id': self.uid})
        self.remove_user(self.uid)

        self.create_user(self.uid, b2b_key='true')

        assert 'true' == db.user_index.find_one({'_id': self.uid})['b2b_key']

        space_report = Quota().report(self.uid)
        assert self.space_10gb == space_report['limit']

    @patch.object(common, 'FEATURE_TOGGLES_B2B_ADD_SPACE', new=True)
    @patch.object(standart, 'FEATURE_TOGGLES_B2B_ADD_SPACE', new=True)
    def test_user_make_b2b_add_additional_space(self):
        db = CollectionRoutedDatabase()
        assert 'b2b_key' not in db.user_index.find_one({'_id': self.uid})

        self.json_ok('user_make_b2b', {'uid': self.uid, 'b2b_key': 'true'})

        # Проверяем, что аттрибуты b2bшного у пользователя появились
        assert 'true' == db.user_index.find_one({'_id': self.uid})['b2b_key']
        space_report = Quota().report(self.uid)
        assert self.b2b_space == space_report['limit']

    @patch.object(common, 'FEATURE_TOGGLES_B2B_ADD_SPACE', new=False)
    @patch.object(standart, 'FEATURE_TOGGLES_B2B_ADD_SPACE', new=False)
    def test_user_make_b2b_no_additional_space(self):
        db = CollectionRoutedDatabase()
        assert 'b2b_key' not in db.user_index.find_one({'_id': self.uid})
        self.json_ok('user_make_b2b', {'uid': self.uid, 'b2b_key': 'true'})
        assert 'true' == db.user_index.find_one({'_id': self.uid})['b2b_key']
        space_report = Quota().report(self.uid)
        assert self.space_10gb == space_report['limit']

    @patch.object(common, 'FEATURE_TOGGLES_B2B_ADD_SPACE', new=True)
    @patch.object(standart, 'FEATURE_TOGGLES_B2B_ADD_SPACE', new=True)
    def test_user_reset_b2b(self):
        db = CollectionRoutedDatabase()
        assert 'b2b_key' not in db.user_index.find_one({'_id': self.uid})

        self.json_ok('user_make_b2b', {'uid': self.uid, 'b2b_key': 'true'})

        # Проверяем, что аттрибуты b2bшного у пользователя появились
        assert 'true' == db.user_index.find_one({'_id': self.uid})['b2b_key']
        space_report = Quota().report(self.uid)
        assert self.b2b_space == space_report['limit']

        self.json_ok('user_reset_b2b', {'uid': self.uid})

        # Проверяем, что аттрибуты пропали и место опять отобралось
        assert 'b2b_key' not in db.user_index.find_one({'_id': self.uid})
        space_report = Quota().report(self.uid)
        assert self.base_space == space_report['limit']

        # Проверяем, что сброс для обычного пользователя вернет ошибку
        self.json_error('user_reset_b2b', {'uid': self.uid}, code=codes.USER_IS_NOT_B2B)

    @pytest.mark.skipif(True, reason='https://st.yandex-team.ru/CHEMODAN-74846')
    def test_b2b_async_user_remove_empty_user(self):
        operation = self.service_ok('async_user_remove', {'uid': self.uid})
        status = self.json_ok('status', {'uid': self.uid, 'oid': operation['result']['oid']})
        assert 'COMPLETED' == status['state']
        user_info = self.json_ok('user_info', {'uid': self.uid})
        assert 'deleted' in user_info
        assert 0 == len([x for x in self.support_ok('list', {'uid': self.uid, 'path': '/hidden'}) if x['type'] == 'file'])

        space_report = Quota().report(self.uid)
        assert space_report['free'] == products.INITIAL_10GB.amount
        assert space_report['used'] == 0

    @pytest.mark.skipif(True, reason='https://st.yandex-team.ru/CHEMODAN-74846')
    def test_b2b_async_user_remove(self):
        self.remove_user(self.uid)
        self.create_user(self.uid, b2b_key='true')

        self.upload_file(self.uid, '/disk/disk_file')
        self.upload_file(self.uid, '/attach/attach_file')
        self.upload_file(self.uid, '/disk/trash_file')
        self.json_ok('async_trash_append', {'uid': self.uid, 'path': '/disk/trash_file'})

        operation = self.json_ok('async_user_remove', {'uid': self.uid})
        status = self.json_ok('status', {'uid': self.uid, 'oid': operation['oid']})

        assert 'COMPLETED' == status['state']

        user_info = self.json_ok('user_info', {'uid': self.uid})

        assert 'deleted' in user_info

        # один файл из диска, один из корзины и один аттач(всего должна быть 3(три))
        assert 3 == len([x for x in self.support_ok('list', {'uid': self.uid, 'path': '/hidden'}) if x['type'] == 'file'])

        space_report = Quota().report(self.uid)
        assert space_report['free'] == products.INITIAL_10GB.amount
        assert space_report['used'] == 0

    @pytest.mark.skipif(True, reason='https://st.yandex-team.ru/CHEMODAN-74846')
    def test_b2b_async_user_remove_allowed_only_on_service_handler(self):
        USER_REMOVE_NOT_ALLOWED_ERROR_CODE = 194
        self.json_error('async_user_remove', {'uid': self.uid}, code=USER_REMOVE_NOT_ALLOWED_ERROR_CODE)

        self.service_ok('async_user_remove', {'uid': self.uid})

    def test_b2b_user_info(self):
        db = CollectionRoutedDatabase()
        assert 'b2b_key' not in db.user_index.find_one({'_id': self.uid})
        self.remove_user(self.uid)

        self.create_user(self.uid, b2b_key='true')
        info = self.json_ok('user_info', {'uid': self.uid})

        # проверим, что флаг добавился
        assert 'is_b2b' in info
        assert info['is_b2b'] is True
        assert 'b2b_key' in info

        self.remove_user(self.uid)

        self.create_user(self.uid)
        info = self.json_ok('user_info', {'uid': self.uid})

        # проверим, что если пользователь не b2b, то даже признака такого не добавляется
        assert 'is_b2b' not in info

    def test_copy_disk(self):
        self.create_user(self.uid)
        self.create_user(self.other_uid)

        folder1_name = 'folder1'
        folder2_name = 'folder2'
        folder1_path = os.path.join('/disk', folder1_name)
        folder2_path = os.path.join('/disk', folder2_name)
        folder1_files = [os.path.join(folder1_path, 'file-%i' % i) for i in xrange(10)]
        folder2_files = [os.path.join(folder2_path, 'file-%i' % i) for i in xrange(6)]

        dest_path = '/disk/dest'

        self.json_ok('mkdir', {'uid': self.uid, 'path': folder1_path})
        self.json_ok('mkdir', {'uid': self.uid, 'path': folder2_path})
        for file_path in folder1_files + folder2_files:
            self.upload_file(self.uid, file_path)

        res = self.json_ok('copy_disk', {'uid': self.other_uid, 'src_uid': self.uid, 'dst': dest_path})
        oid = res['oid']
        result = self.json_ok('status', {'uid': self.other_uid, 'oid': oid})
        assert result['status'] == 'DONE'

        res = self.json_ok('list', {'uid': self.other_uid, 'path': dest_path})

        list_folders = set([i['path'] for i in res])
        folder1_dest_path = os.path.join(dest_path, folder1_name)
        folder2_dest_path = os.path.join(dest_path, folder2_name)
        assert folder1_dest_path in list_folders
        assert folder2_dest_path in list_folders

        res = self.json_ok('list', {'uid': self.other_uid, 'path': folder1_dest_path})
        list_files = [i['path'] for i in res if i['type'] == 'file']
        assert len(list_files) == len(folder1_files)

        res = self.json_ok('list', {'uid': self.other_uid, 'path': folder2_dest_path})
        list_files = [i['path'] for i in res if i['type'] == 'file']
        assert len(list_files) == len(folder2_files)

        res = self.json_ok('list', {'uid': self.other_uid, 'path': '/disk'})

    def test_copy_disk_recursion_error(self):
        self.create_user(self.uid)
        self.create_user(self.uid_3)

        # проверим, что нельзя скопировать свой диск себе же
        self.json_error('copy_disk', {'uid': self.uid, 'src_uid': self.uid, 'dst': '/disk/123'},
                        code=codes.BAD_TARGET_USER_SPECIFIED)

        # проверим, что если dst - это подпапка общей папки, владелец которой - src_uid
        folder_name = 'folder'
        folder_path = os.path.join('/disk', folder_name)
        file_path = os.path.join(folder_path, 'file.txt')

        self.json_ok('mkdir', {'uid': self.uid, 'path': folder_path})
        self.upload_file(self.uid, file_path)

        common_folder = '/disk/abc/'
        self.json_ok('mkdir', {'uid': self.uid, 'path': common_folder})

        hsh = self.invite_user(uid=self.uid_3, owner=self.uid, email=self.email_3, rights=660, path=common_folder)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        dest_path = os.path.join(common_folder, 'user1')

        self.json_error('copy_disk', {'uid': self.uid_3, 'src_uid': self.uid, 'dst': dest_path},
                        code=codes.BAD_TARGET_USER_SPECIFIED)

    @pytest.mark.skipif(True, reason='Readonly not implemented in Sharpei https://st.yandex-team.ru/CHEMODAN-64842')
    def test_user_read_only(self):
        self.json_ok('user_set_readonly', {'uid': self.uid})

        self.json_error('mkdir', {'uid': self.uid, 'path': '/disk/test_folder'}, code=150)

        assert 'RO' == self.json_ok('user_info', {'uid': self.uid})['db']['status']

        self.json_ok('user_unset_readonly', {'uid': self.uid})

        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/test_folder'})

        assert 'RW' == self.json_ok('user_info', {'uid': self.uid})['db']['status']

    def test_guest_activate_invite_b2b_owner_got_no_email(self):
        """Протестировать, что b2b-владелец ОП не получает уведомления о том,
        что b2b/не-b2b юзер принял приглашение.
        """
        folder = '/disk/shared_folder'
        self.json_ok('user_make_b2b', {'uid': self.uid, 'b2b_key': 'true'})
        self.create_user(self.uid_3)
        self.json_ok('user_make_b2b', {'uid': self.uid_3, 'b2b_key': 'true'})
        self.create_user(self.uid_6)

        self.json_ok('mkdir', {'uid': self.uid, 'path': folder})
        self.xiva_subscribe(self.uid_3)
        self.xiva_subscribe(self.uid_6)

        # b2b
        hsh = self.invite_user(uid=self.uid_3, owner=self.uid, email=self.email_3, rights=660, path=folder)
        with patch.object(MailNotifier, 'user_activated_invite') as mocked:
            self.activate_invite(uid=self.uid_3, hash=hsh)
            assert not mocked.called

        # не b2b
        hsh = self.invite_user(uid=self.uid_6, owner=self.uid, email=self.email_6, rights=660, path=folder)
        with patch.object(MailNotifier, 'user_activated_invite') as mocked:
            self.activate_invite(uid=self.uid_6, hash=hsh)
            assert not mocked.called

    def test_guest_left_folder_b2b_owner_got_no_email(self):
        """Протестировать, что b2b-владелец ОП не получает уведомления о том,
        что b2b/не-b2b юзер покинул группу.
        """
        folder = '/disk/shared_folder'
        self.json_ok('user_make_b2b', {'uid': self.uid, 'b2b_key': 'true'})
        self.create_user(self.uid_3)
        self.json_ok('user_make_b2b', {'uid': self.uid_3, 'b2b_key': 'true'})
        self.create_user(self.uid_6)

        self.json_ok('mkdir', {'uid': self.uid, 'path': folder})
        self.xiva_subscribe(self.uid_3)
        self.xiva_subscribe(self.uid_6)

        hsh = self.invite_user(uid=self.uid_3, owner=self.uid, email=self.email_3, rights=660, path=folder)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        result = self.json_ok('info', {'uid': self.uid, 'path': folder, 'meta': 'group'})
        gid = result['meta']['group']['gid']
        # b2b
        with patch.object(MailNotifier, 'user_left_folder') as mocked:
            self.json_ok('share_leave_group', {'uid': self.uid_3, 'gid': gid})
            assert not mocked.called

        # не b2b
        hsh = self.invite_user(uid=self.uid_6, owner=self.uid, email=self.email_6, rights=660, path=folder)
        self.activate_invite(uid=self.uid_6, hash=hsh)
        with patch.object(MailNotifier, 'user_left_folder') as mocked:
            self.json_ok('share_leave_group', {'uid': self.uid_6, 'gid': gid})
            assert not mocked.called

    def test_guest_kicked_from_folder_b2b_owner_got_no_email(self):
        """Протестировать, что b2b-владелец ОП не получает уведомления о том,
        что b2b/не-b2b юзер вылетел из группы.
        """
        folder = '/disk/shared_folder'
        self.json_ok('user_make_b2b', {'uid': self.uid, 'b2b_key': 'true'})
        self.create_user(self.uid_3)
        self.json_ok('user_make_b2b', {'uid': self.uid_3, 'b2b_key': 'true'})
        self.create_user(self.uid_6)

        self.json_ok('mkdir', {'uid': self.uid, 'path': folder})
        self.xiva_subscribe(self.uid_3)
        self.xiva_subscribe(self.uid_6)

        hsh = self.invite_user(uid=self.uid_3, owner=self.uid, email=self.email_3, rights=660, path=folder)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        result = self.json_ok('info', {'uid': self.uid, 'path': folder, 'meta': 'group'})
        gid = result['meta']['group']['gid']
        # b2b
        with patch.object(MailNotifier, 'user_kicked') as mocked:
            self.json_ok('share_kick_from_group', {'uid': self.uid, 'user_uid': self.uid_3, 'gid': gid})
            assert not mocked.called

        # не b2b
        hsh = self.invite_user(uid=self.uid_6, owner=self.uid, email=self.email_6, rights=660, path=folder)
        self.activate_invite(uid=self.uid_6, hash=hsh)
        with patch.object(MailNotifier, 'user_kicked') as mocked:
            self.json_ok('share_kick_from_group', {'uid': self.uid, 'user_uid': self.uid_6, 'gid': gid})
            assert not mocked.called

    def test_b2b_got_space_finished_email(self):
        """Протестировать значение шаблона письма для b2b пользователя.

        О том, что место закончилось и что не посылаем письмо о том, что место заканчивается.
        """

        self.json_ok('user_make_b2b', {'uid': self.uid, 'b2b_key': 'true'})

        with patch.object(mailer, 'send') as mocked_send:
            with patch.object(Quota, 'get_push_type', return_value=Low()):
                self.upload_file(self.uid, '/disk/test2', file_data={'size': 3})
                assert not mocked_send.called

        with patch.object(mailer, 'send') as mocked_send:
            with patch.object(Quota, 'get_push_type', return_value=Full()):
                self.upload_file(self.uid, '/disk/test3', file_data={'size': 1})
                assert mocked_send.called
                args, _ = mocked_send.call_args
                assert 'b2b/space/finished' in args

    def test_user_reset_b2b_send_email(self):
        self.json_ok('user_make_b2b', {'uid': self.uid, 'b2b_key': 'true'})
        with patch.object(notify, 'service_deleted') as mocked:
            self.json_ok('user_reset_b2b', {'uid': self.uid, 'noemail': 1})
            assert not mocked.called

    def test_user_reset_b2b_disable_send_email(self):
        self.json_ok('user_make_b2b', {'uid': self.uid, 'b2b_key': 'true'})
        with patch.object(notify, 'service_deleted') as mocked:
            self.json_ok('user_reset_b2b', {'uid': self.uid, 'noemail': 0})
            assert mocked.called

    @patch.object(standart.StandartUser, 'get_pdd_domain', return_value='abc@gmail.com')
    def test_b2b_user_has_allowed_office_domain(self, mocked_get_pdd_domain):
        self.json_ok('user_make_b2b', {'uid': self.uid, 'b2b_key': 'true'})

        with patch.object(common, 'was_editor_used', return_value=True) as mocked_was_editor_used:
            user = User(self.uid)
            user.is_domain_allowed_for_office()

            assert mocked_get_pdd_domain.called
            assert mocked_was_editor_used.call_count == 1

            assert user.is_b2b()
            assert user.is_domain_allowed_for_office()

    @patch.object(standart.StandartUser, 'get_pdd_domain', return_value='abc@gmail.com')
    def test_b2b_user_has_no_allowed_office_domain(self, mocked_get_pdd_domain):
        self.json_ok('user_make_b2b', {'uid': self.uid, 'b2b_key': 'true'})

        with patch.object(common, 'was_editor_used', return_value=False) as mocked_was_editor_used:
            user = User(self.uid)
            user.is_domain_allowed_for_office()
            user.is_domain_allowed_for_office()
            user.is_domain_allowed_for_office()

            assert mocked_get_pdd_domain.called
            assert mocked_was_editor_used.call_count == 1

            assert user.is_b2b()
            assert not user.is_domain_allowed_for_office()


class B2bFairSharingTestCase(CommonSharingMethods):
    """Тесты честного шаринга

    https://wiki.yandex-team.ru/disk/mpfs/rd/fair-sharing/
    """
    def test_notify_directory_on_activate_invite(self):
        folder = '/disk/1'
        self.json_ok('mkdir', {'uid': self.uid, 'path': folder})
        self.json_ok('user_make_b2b', {'uid': self.uid, 'b2b_key': 'b2b_key_1'})

        self.create_user(self.uid_3)
        self.json_ok('user_make_b2b', {'uid': self.uid_3, 'b2b_key': 'b2b_key_1'})
        self.create_user(self.uid_6)
        self.json_ok('user_make_b2b', {'uid': self.uid_6, 'b2b_key': 'b2b_key_2'})

        hsh = self.invite_user(uid=self.uid_3, owner=self.uid, email=self.email_3, rights=660, path=folder)
        with DirectoryServiceStub() as stub:
            self.activate_invite(uid=self.uid_3, hash=hsh)
            assert stub.notify_user_activate_invite.called
            args, kwargs = stub.notify_user_activate_invite.call_args
            assert args[0] == self.uid
            assert isinstance(args[1], basestring)
            assert args[2] == self.uid_3
            assert kwargs == {'read_only': False}

        # пользователь из другой организации
        hsh = self.invite_user(uid=self.uid_6, owner=self.uid, email=self.email_6, rights=660, path=folder)
        with DirectoryServiceStub() as stub:
            self.activate_invite(uid=self.uid_6, hash=hsh)
            assert not stub.notify_user_activate_invite.called
