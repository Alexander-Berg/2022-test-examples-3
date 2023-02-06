# -*- coding: utf-8 -*-
import mock
import pytest

from hamcrest import assert_that, has_items, has_entries, has_entry, instance_of, all_of, is_not, has_key

from test.base import CommonDiskTestCase
from test.fixtures import users
from test.helpers.utils import disk_path_to_area_path
from test.parallelly.api.disk.base import DiskApiTestCase
from test.base_suit import UploadFileTestCaseMixin, UserTestCaseMixin, SharingTestCaseMixin
from mpfs.common.util import from_json
from mpfs.common.static import tags
from mpfs.config import settings
from mpfs.platform.v1.disk import exceptions
from test.conftest import INIT_USER_IN_POSTGRES


SETPROP_SYMLINK_FIELDNAME = settings.system['setprop_symlink_fieldname']


class GetDeltasTestCase(SharingTestCaseMixin, CommonDiskTestCase, DiskApiTestCase):
    api_mode = tags.platform.INTERNAL
    api_version = 'v1'
    uid_1 = users.user_1.uid

    def setup_method(self, method):
        super(GetDeltasTestCase, self).setup_method(method)
        self.create_user(self.uid, noemail=1)

    def get_version(self, uid):
        return long(self.json_ok('user_info', {'uid': uid})['version'])

    def test_common(self):
        revision = self.get_version(self.uid)
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/1'})
        with self.specified_client(scopes=['cloud_api:disk.read']):
            resp = self.client.get('disk/resources/deltas', query={'base_revision': revision})
            assert resp.status_code == 200
            assert from_json(resp.content)

    def test_blocked_user(self):
        revision = self.get_version(self.uid)
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/1'})
        opts = {'uid': self.uid,
                'moderator': 'moderator',
                'comment': 'bad person'}
        self.support_ok('block_user', opts)
        with self.specified_client(scopes=['cloud_api:disk.read']):
            resp = self.client.get('disk/resources/deltas', query={'base_revision': revision})
            assert resp.status_code == 403

    def test_public(self):
        revision = self.get_version(self.uid)
        public_dir = '/disk/pubdir'
        public_file = '/disk/public.file'
        non_public_file = '/disk/private.file'
        non_public_dir = '/disk/pridir'
        file_in_public_dir = '%s/inner.file' % public_dir
        for dir in (public_dir,
                    non_public_dir):
            self.json_ok('mkdir', {'uid': self.uid, 'path': dir})
        for file_path in (public_file,
                          non_public_file,
                          file_in_public_dir):
            self.upload_file(self.uid, file_path)
        for path in (public_dir,
                     public_file):
            self.json_ok('set_public', opts={'uid': self.uid, 'path': path})

        with self.specified_client(scopes=['cloud_api:disk.read']):
            resp = self.client.get('disk/resources/deltas', query={'base_revision': revision})
            assert resp.status_code == 200
            items = from_json(resp.content)['items']
        assert_that(items, has_items(has_entries(path=disk_path_to_area_path(public_dir),
                                                 resource=has_entry('public_url', instance_of(unicode))),
                                     has_entries(path=disk_path_to_area_path(public_file),
                                                 resource=has_entry('public_url', instance_of(unicode))),
                                     has_entries(path=disk_path_to_area_path(file_in_public_dir),
                                                 resource=is_not(has_key('public_url'))),
                                     has_entries(path=disk_path_to_area_path(non_public_file),
                                                 resource=is_not(has_key('public_url')))))

    def test_fields(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/deleted'})
        revision = self.get_version(self.uid)
        self.json_ok('rm', {'uid': self.uid, 'path': '/disk/deleted'})
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/new_dir'})
        self.upload_file(self.uid, '/disk/new_file')
        with self.specified_client(scopes=['cloud_api:disk.read']):
            resp = self.client.get('disk/resources/deltas', query={'base_revision': revision})
            assert resp.status_code == 200
            data = from_json(resp.content)
            assert not {'items', 'revision', 'iteration_key'} ^ data.viewkeys()
            assert len(data['items']) == 3

            required_items_fields = {'change_type', 'path', 'resource_id', 'type', 'revision'}
            for item in data['items']:
                # проверка полей из диффа
                if item['change_type'] != 'deleted':
                    items_fields = required_items_fields | {'resource'}
                else:
                    items_fields = required_items_fields
                assert not items_fields ^ item.viewkeys()
                # проверка полей ресурса
                if item['change_type'] != 'deleted':
                    if item['type'] == 'dir':
                        resource_fields = {'modified'}
                    else:
                        resource_fields = {'size', 'md5', 'sha256', 'modified'}
                    assert not resource_fields ^ item['resource'].viewkeys()

    def test_revision_not_found(self):
        with self.specified_client(scopes=['cloud_api:disk.read']):
            resp = self.client.get('disk/resources/deltas', query={'base_revision': 1})
            assert resp.status_code == 410
            assert 'RevisionNotFoundError' in resp.content

    def test_symlink_field_is_present(self):
        DIR_WITH_SYMLINK = '/disk/new_dir'

        revision = self.get_version(self.uid)
        self.json_ok('mkdir', {'uid': self.uid, 'path': DIR_WITH_SYMLINK})

        SYMLINK_VALUE = '1234'
        self.json_ok('setprop', {'uid': self.uid, 'path': DIR_WITH_SYMLINK, SETPROP_SYMLINK_FIELDNAME: SYMLINK_VALUE})

        with self.specified_client(scopes=['cloud_api:disk.read']):
            resp = self.client.get('disk/resources/deltas', query={'base_revision': revision})
            assert resp.status_code == 200
            data = from_json(resp.content)
            assert len(data['items']) == 1
            delta_item = data['items'][0]
            assert delta_item['discsw_symlink'] == SYMLINK_VALUE

    def test_share_info_is_present_for_root_share_folder(self):
        SHARED_DIR_PATH = '/disk/test'
        SHARED_SUBDIR_PATH = '/disk/test/sub'
        SHARED_DIR_PATH_REST = 'disk:/test'

        revision_1 = self.get_version(self.uid)
        self.json_ok('mkdir', {'uid': self.uid, 'path': SHARED_DIR_PATH})

        # создаём другого пользователя и общую папку с файлом у него
        self.create_user(self.uid_1)
        # расшариваем папку
        group = self.json_ok('share_create_group', opts={'uid': self.uid, 'path': SHARED_DIR_PATH})
        gid = group['gid']
        # приглашаем тестового пользователя в расшаренную папку
        invite_hash = self.share_invite(group['gid'], self.uid_1)
        # подключаем расшареную папку тестовому пользователю
        self.json_ok('share_activate_invite', opts={'uid': self.uid_1, 'hash': invite_hash})

        with self.specified_client(scopes=['cloud_api:disk.read']):
            resp = self.client.get('disk/resources/deltas', query={'base_revision': revision_1})
            assert resp.status_code == 200
            data = from_json(resp.content)
            assert len(data['items']) == 1
            assert data['items'][0]['share'] == {'is_owned': True, 'rights': 'rw'}

        revision_2 = self.get_version(self.uid)
        self.json_ok('mkdir', {'uid': self.uid, 'path': SHARED_SUBDIR_PATH})

        with self.specified_client(scopes=['cloud_api:disk.read']):
            resp = self.client.get('disk/resources/deltas', query={'base_revision': revision_2})
            assert resp.status_code == 200
            data = from_json(resp.content)
            assert len(data['items']) == 1
            assert 'share' not in data['items'][0]

    @pytest.mark.skipif(not INIT_USER_IN_POSTGRES, reason='Postgres test')
    def test_quick_move_deltas(self):
        DIR_PATH = '/disk/folder'

        revision = self.get_version(self.uid)
        self.json_ok('mkdir', {'uid': self.uid, 'path': DIR_PATH})

        with mock.patch('mpfs.metastorage.mongo.collections.filesystem.POSTGRES_QUICK_MOVE_ENABLED', True), \
                mock.patch('mpfs.metastorage.mongo.collections.filesystem.POSTGRES_QUICK_MOVE_ALLOWED_UIDS', self.uid), \
                mock.patch('mpfs.core.filesystem.base.POSTGRES_QUICK_MOVE_FOLDER_RESOURCES_LIMIT', 0):
            self.json_ok('async_move', {'uid': self.uid, 'src': DIR_PATH, 'dst': '/disk/new'})

            with self.specified_client(scopes=['cloud_api:disk.read']):
                resp = self.client.get('disk/resources/deltas',
                                       query={'base_revision': revision, 'allow_moved_deltas': '1'})
                assert resp.status_code == 200
                data = from_json(resp.content)
                assert len(data['items']) == 2

    @pytest.mark.skipif(not INIT_USER_IN_POSTGRES, reason='Postgres test')
    def test_quick_move_deltas_error_without_parameter(self):
        DIR_PATH = '/disk/folder'

        revision = self.get_version(self.uid)
        self.json_ok('mkdir', {'uid': self.uid, 'path': DIR_PATH})

        with mock.patch('mpfs.metastorage.mongo.collections.filesystem.POSTGRES_QUICK_MOVE_ENABLED', True), \
                mock.patch('mpfs.metastorage.mongo.collections.filesystem.POSTGRES_QUICK_MOVE_ALLOWED_UIDS', self.uid), \
                mock.patch('mpfs.core.filesystem.base.POSTGRES_QUICK_MOVE_FOLDER_RESOURCES_LIMIT', 0):
            self.json_ok('async_move', {'uid': self.uid, 'src': DIR_PATH, 'dst': '/disk/new'})

            with self.specified_client(scopes=['cloud_api:disk.read']):
                resp = self.client.get('disk/resources/deltas', query={'base_revision': revision})
                print resp.content
                assert resp.status_code == exceptions.DiskRevisionNotFoundError.status_code
