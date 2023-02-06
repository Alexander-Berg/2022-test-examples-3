# -*- coding: utf-8 -*-
import pytest

from test.common.sharing import CommonSharingMethods

from mpfs.core.operations.manager import get_operation
from mpfs.common.static.codes import COMPLETED
from mpfs.core.filesystem.dao.legacy import CollectionRoutedDatabase


class RmSharingTestCase(CommonSharingMethods):

    def setup_method(self, method):
        super(RmSharingTestCase, self).setup_method(method)

    def test_rm_root_empty_group(self):
        path = '/disk/444'
        owner = self.uid_1

        self.json_ok('user_init', {'uid': owner})
        self.xiva_subscribe(owner)

        self.json_ok('mkdir', {'uid': owner, 'path': path})
        self.json_ok('share_create_group', {'uid': owner, 'path': path})
        self.json_ok('rm', {'uid': owner, 'path': path})

    def test_rm_internal_folder_in_group(self):
        self.create_user(self.uid)
        self.xiva_subscribe(self.uid)

        self.create_user(self.uid_3)
        self.xiva_subscribe(self.uid_3)

        shared_folder_path = '/disk/shared_folder'
        shared_folder_subfolder_path = shared_folder_path + '/shared_folder_subfolder'

        self.json_ok('mkdir', {'uid': self.uid, 'path': shared_folder_path})
        self.json_ok('mkdir', {'uid': self.uid, 'path': shared_folder_subfolder_path})
        self.create_group(path=shared_folder_path)
        hash_ = self.invite_user(uid=self.uid_3, email=self.email_3, path=shared_folder_path)
        self.activate_invite(uid=self.uid_3, hash=hash_)
        self.json_ok('rm', {'uid': self.uid, 'path': shared_folder_subfolder_path})

    def test_shared_root_folder_removed_by_non_owner(self):
        """Создаем группу, приглашаем пользователя и подтверждаем им приглашение.
        Приглашенный пользователь удаляет расшаренную папку у себя."""

        self.create_user(self.uid)
        self.xiva_subscribe(self.uid)

        self.create_user(self.uid_3)
        self.xiva_subscribe(self.uid_3)

        shared_folder_path = '/disk/shared_folder'
        self.json_ok('mkdir', {'uid': self.uid, 'path': shared_folder_path})

        self.create_group(path=shared_folder_path)

        hash_ = self.invite_user(uid=self.uid_3, email=self.email_3, path=shared_folder_path)
        self.activate_invite(uid=self.uid_3, hash=hash_)

        self.json_ok('rm', {'uid': self.uid_3, 'path': shared_folder_path})

    def test_group_root_folder_with_file_removed_by_non_owner(self):
        self.create_user(self.uid)
        self.xiva_subscribe(self.uid)

        self.create_user(self.uid_3)
        self.xiva_subscribe(self.uid_3)

        shared_folder_path = '/disk/shared_folder'
        self.json_ok('mkdir', {'uid': self.uid, 'path': shared_folder_path})

        self.create_group(path=shared_folder_path)

        hash_ = self.invite_user(uid=self.uid_3, email=self.email_3, path=shared_folder_path)
        self.activate_invite(uid=self.uid_3, hash=hash_)

        self.upload_file(self.uid, path=shared_folder_path + '/test.jpg')

        self.json_ok('rm', {'uid': self.uid_3, 'path': shared_folder_path})

    def test_space_after_rm_resource(self):
        for uid in (self.uid,
                    self.uid_3,
                    self.uid_6):
            self.create_user(uid)
            self.xiva_subscribe(uid)

        # Загружаем несколько файлов всем, чтобы проверять проверять точное значением места
        for i in xrange(3):
            for uid in (self.uid,
                        self.uid_3,
                        self.uid_6):
                self.upload_file(uid, path='/disk/%s.jpg' % i)

        shared_folder_path = '/disk/shared_folder'
        self.json_ok('mkdir', {'uid': self.uid, 'path': shared_folder_path})

        self.create_group(path=shared_folder_path)

        for uid, email in ((self.uid_3, self.email_3),
                           (self.uid_6, self.email_6)):
            hash_ = self.invite_user(uid=uid, email=email, path=shared_folder_path)
            self.activate_invite(uid=uid, hash=hash_)

        invited_initiator_space_without_shared = self.json_ok('space', {'uid': self.uid_3})
        another_invited_space_without_shared = self.json_ok('space', {'uid': self.uid_6})

        file_path = shared_folder_path + '/test.jpg'
        file_size = 773
        self.upload_file(self.uid_3, path=file_path, file_data={'size': file_size})

        owner_space_before_rm = self.json_ok('space', {'uid': self.uid})
        invited_initiator_space_before_rm = self.json_ok('space', {'uid': self.uid_3})
        another_invited_space_before_rm = self.json_ok('space', {'uid': self.uid_6})

        self.json_ok('rm', {'uid': self.uid_3, 'path': file_path})

        owner_space_after_rm = self.json_ok('space', {'uid': self.uid})
        invited_initiator_space_after_rm = self.json_ok('space', {'uid': self.uid_3})
        another_invited_space_after_rm = self.json_ok('space', {'uid': self.uid_6})

        assert another_invited_space_after_rm['used'] == another_invited_space_before_rm['used'] == \
               another_invited_space_without_shared['used']
        assert invited_initiator_space_after_rm['used'] == invited_initiator_space_before_rm['used'] == \
               invited_initiator_space_without_shared['used']
        assert owner_space_after_rm['used'] == owner_space_before_rm['used'] - file_size

    def test_group_folder_with_file_trash_appended_by_owner(self):
        self.create_user(self.uid)
        self.xiva_subscribe(self.uid)

        self.create_user(self.uid_3)
        self.xiva_subscribe(self.uid_3)

        shared_folder_path = '/disk/shared_folder'
        self.json_ok('mkdir', {'uid': self.uid, 'path': shared_folder_path})

        self.create_group(path=shared_folder_path)

        hash_ = self.invite_user(uid=self.uid_3, email=self.email_3, path=shared_folder_path)
        self.activate_invite(uid=self.uid_3, hash=hash_)

        self.upload_file(self.uid, path=shared_folder_path + '/test.jpg')

        response = self.json_ok('async_trash_append', {'uid': self.uid, 'path': shared_folder_path})

        operation_id = response['oid']
        operation = get_operation(uid=self.uid, oid=operation_id)
        assert operation.get_status()['status'] == COMPLETED

    def test_remove_common_folder_containing_shared_folder(self):
        self.create_user(self.uid)
        self.xiva_subscribe(self.uid)

        self.create_user(self.uid_3)
        self.xiva_subscribe(self.uid_3)

        non_shared_folder_path = '/disk/non_shared_folder'
        self.json_ok('mkdir', {'uid': self.uid, 'path': non_shared_folder_path})

        shared_folder_1_path = non_shared_folder_path + '/shared_folder_1'
        shared_folder_2_path = non_shared_folder_path + '/shared_folder_2'

        for path in (shared_folder_1_path, shared_folder_2_path):
            self.json_ok('mkdir', {'uid': self.uid, 'path': path})
            self.create_group(path=path)
            group_hash = self.invite_user(uid=self.uid_3, email=self.email_3, path=path)
            self.activate_invite(uid=self.uid_3, hash=group_hash)

        self.json_ok('rm', {'uid': self.uid, 'path': non_shared_folder_path})

        result = self.json_ok('share_list_all_folders', {'uid': self.uid})
        assert result == []

        db = CollectionRoutedDatabase()
        groups_entries = list(db.groups.find({'owner': self.uid}))
        group_link_entries = list(db.group_links.find({'uid': self.uid_3}))

        assert len(groups_entries) == 0
        assert len(group_link_entries) == 0
