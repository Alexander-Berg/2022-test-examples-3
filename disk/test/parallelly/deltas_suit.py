# -*- coding: utf-8 -*-
import mock
import pytest

from mpfs.common.static import codes
from mpfs.config import settings
from mpfs.core.user.dao.user import UserDAO
from mpfs.metastorage.mongo.collections.base import UserIndexCollection
from test.base import DiskTestCase
from test.base_suit import SharingTestCaseMixin
from test.conftest import INIT_USER_IN_POSTGRES
from test.fixtures import users


SETPROP_SYMLINK_FIELDNAME = settings.system['setprop_symlink_fieldname']


class DeltasTestCase(SharingTestCaseMixin, DiskTestCase):
    """Тестирование deltas
    """
    uid_1 = users.user_1.uid

    def get_version(self, uid):
        return long(self.json_ok('user_info', {'uid': uid})['version'])

    def test_base_fields(self):
        """Проверям наличие обязательных полей в ответе
        """
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/changed'})
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/deleted'})

        version = self.get_version(self.uid)
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/new'})
        self.json_ok('setprop', {'uid': self.uid, 'path': '/disk/changed', 'test_key': 'test_value'})
        self.json_ok('rm', {'uid': self.uid, 'path': '/disk/deleted'})
        resp = self.json_ok('deltas', {'uid': self.uid, 'base_revision': version})

        assert 'items' in resp
        assert 'revision' in resp

        for item in resp['items']:
            for key in ('resource_id', 'type', 'path', 'change_type', 'revision'):
                assert key in item

    def test_meta_formatting_for_item_resource(self):
        version = self.get_version(self.uid)
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/new'})
        resp = self.json_ok('deltas', {'uid': self.uid, 'base_revision': version, 'meta': 'resource_id'})
        item = resp['items'][0]
        assert item['resource']['meta']['resource_id'] == item['resource_id']

    def test_new_common(self):
        version = self.get_version(self.uid)
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/1'})
        self.upload_file(self.uid, '/disk/1.txt')
        resp = self.json_ok('deltas', {'uid': self.uid, 'base_revision': version})

        for item in resp['items']:
            assert item['change_type'] == 'new'
            assert item['resource']

    def test_changed_common(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/1'})
        self.upload_file(self.uid, '/disk/1.txt')

        version = self.get_version(self.uid)
        self.json_ok('setprop', {'uid': self.uid, 'path': '/disk/1', 'test_key': 'test_value'})
        self.json_ok('setprop', {'uid': self.uid, 'path': '/disk/1.txt', 'test_key': 'test_value'})
        resp = self.json_ok('deltas', {'uid': self.uid, 'base_revision': version})

        for item in resp['items']:
            assert item['change_type'] == 'changed'
            assert item['resource']

    def test_deleted_common(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/1'})
        self.upload_file(self.uid, '/disk/1.txt')

        version = self.get_version(self.uid)
        self.json_ok('rm', {'uid': self.uid, 'path': '/disk/1'})
        self.json_ok('rm', {'uid': self.uid, 'path': '/disk/1.txt'})
        resp = self.json_ok('deltas', {'uid': self.uid, 'base_revision': version})

        for item in resp['items']:
            assert item['change_type'] == 'deleted'
            assert 'resource' not in item

    def test_discsw_symlink_field(self):
        SYMLINK_VALUE = '%25disk%25jntjq9ajpn7h65f3%25test508/12345'
        version = self.get_version(self.uid)
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/test'})
        self.json_ok('setprop', {'uid': self.uid, 'path': '/disk/test', SETPROP_SYMLINK_FIELDNAME: SYMLINK_VALUE})
        resp = self.json_ok('deltas', {'uid': self.uid, 'base_revision': version})
        assert len(resp['items']) == 1
        assert resp['items'][0]['discsw_symlink'] == SYMLINK_VALUE

    def test_group_info_is_present_for_shared_folder(self):
        SHARED_DIR_PATH = '/disk/test'
        SHARED_SUBDIR_PATH = '/disk/test/sub'
        version = self.get_version(self.uid)
        self.json_ok('mkdir', {'uid': self.uid, 'path': SHARED_DIR_PATH})
        self.json_ok('mkdir', {'uid': self.uid, 'path': SHARED_SUBDIR_PATH})

        # создаём другого пользователя и общую папку с файлом у него
        self.create_user(self.uid_1)
        # расшариваем папку
        group = self.json_ok('share_create_group', opts={'uid': self.uid, 'path': SHARED_DIR_PATH})
        gid = group['gid']
        # приглашаем тестового пользователя в расшаренную папку
        invite_hash = self.share_invite(group['gid'], self.uid_1)
        # подключаем расшареную папку тестовому пользователю
        self.json_ok('share_activate_invite', opts={'uid': self.uid_1, 'hash': invite_hash})

        resp = self.json_ok('deltas', {'uid': self.uid, 'base_revision': version})
        assert len(resp['items']) == 2
        for item in resp['items']:
            if item['path'] == SHARED_DIR_PATH:
                assert item['group'] == {'is_owned': True, 'rights': 660}
            else:
                assert 'group' not in item

    def test_deltas_for_participant_with_shared_folder_invite(self):
        """
        Проверяем, что если участника приглашаем в ОП, а потом приходим за дельтами с версией до приглашения, то
        отдадим ошибку, что версий нет, чтобы пользователь пошел за полным снепшотом
        """
        SHARED_DIR_PATH = '/disk/test'
        SHARED_SUBDIR_PATH = '/disk/test/sub'

        self.json_ok('mkdir', {'uid': self.uid, 'path': SHARED_DIR_PATH})
        self.json_ok('mkdir', {'uid': self.uid, 'path': SHARED_SUBDIR_PATH})

        self.upload_file(self.uid, SHARED_SUBDIR_PATH + '/file-1.txt')

        self.create_user(self.uid_1)
        version = self.get_version(self.uid_1)

        group = self.json_ok('share_create_group', opts={'uid': self.uid, 'path': SHARED_DIR_PATH})
        invite_hash = self.share_invite(group['gid'], self.uid_1)
        self.json_ok('share_activate_invite', opts={'uid': self.uid_1, 'hash': invite_hash})
        self.upload_file(self.uid, SHARED_SUBDIR_PATH + '/file-2.txt')

        with mock.patch('mpfs.metastorage.mongo.collections.filesystem.DELTAS_SNAPSHOT_ON_GROUP_FOLDER_INVITE_ENABLED', True), \
                mock.patch('mpfs.metastorage.mongo.collections.filesystem.DELTAS_SNAPSHOT_ON_GROUP_FOLDER_INVITE_DRY_RUN', False):
            self.json_error('deltas', {'uid': self.uid_1, 'base_revision': version}, code=codes.VERSION_NOT_FOUND)

    @pytest.mark.skipif(not INIT_USER_IN_POSTGRES, reason='only for pg')
    def test_force_snapshot(self):
        user_index_mpfs_collection = UserIndexCollection()

        user_info = user_index_mpfs_collection.check_user(self.uid)
        version_before = user_info['version']
        assert user_info['force_snapshot_version'] is None

        self.service_ok('force_snapshot', {'uid': self.uid})

        user_index_mpfs_collection.reset()
        user_info = user_index_mpfs_collection.check_user(self.uid)
        assert user_info['version'] == version_before + 1
        assert user_info['force_snapshot_version'] == user_info['version']

        self.json_error('deltas', {'uid': self.uid, 'base_revision': version_before}, code=codes.VERSION_NOT_FOUND)

        version_after = user_info['version']
        diff_result = self.json_ok('diff', {'uid': self.uid, 'path': '/disk'})
        assert int(diff_result['version']) == version_after

        deltas = self.json_ok('deltas', {'uid': self.uid, 'base_revision': version_after})
        assert int(deltas['revision']) == version_after
