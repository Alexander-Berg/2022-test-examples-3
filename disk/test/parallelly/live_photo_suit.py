# -*- coding: utf-8 -*-
import hashlib
import time

import mock
import pytest
from nose_parameterized import parameterized

from mpfs.core.address import Address
from mpfs.core.user.constants import ADDITIONAL_AREA
from test.base import DiskTestCase

from mpfs.common.static import codes
from mpfs.common.util import hashed
from mpfs.core.filesystem.cleaner.hidden import HiddenDataCleanerWorker, HiddenDataCleanerToggle
from mpfs.core.filesystem.dao.legacy import CollectionRoutedDatabase
from mpfs.dao.session import Session
from mpfs.metastorage.mongo.binary import Binary
from mpfs.metastorage.postgres.query_executer import PGQueryExecuter
from test.base_suit import TrashTestCaseMixin
from test.common.sharing import CommonSharingMethods
from test.conftest import INIT_USER_IN_POSTGRES
from test.fixtures.users import default_user, user_3
from test.helpers.stubs.services import SearchIndexerStub
from test.parallelly.event_history_suit import EventHistoryTestCase


class LivePhotoMixin(object):

    def store_live_photo_with_video(self, live_photo_store_path, file_data=None, opts=None, live_photo_real_path=None):
        opts = {} if opts is None else opts
        live_photo_real_path = live_photo_store_path if live_photo_real_path is None else live_photo_real_path
        file_data = {} if file_data is None else file_data

        photo_md5 = file_data.get('md5', hashlib.md5(str(time.time())).hexdigest())
        photo_sha256 = file_data.get('sha256', hashlib.sha256(str(time.time())).hexdigest())
        photo_size = file_data.get('size', 1234)

        video_md5 = file_data.pop('live_photo_video_md5', hashlib.md5(str(time.time() + 1)).hexdigest())
        video_sha256 = file_data.pop('live_photo_video_sha256', hashlib.sha256(str(time.time() + 1)).hexdigest())
        video_size = file_data.pop('live_photo_video_size', 1234)

        self.upload_file(
            self.uid,
            live_photo_store_path,
            file_data={
                'md5': photo_md5, 'sha256': photo_sha256, 'size': photo_size, 'mimetype': 'image/jpg'
            },
            live_photo_md5=video_md5,
            live_photo_sha256=video_sha256,
            live_photo_size=video_size,
            live_photo_type='photo',
            opts=opts,
        )

        self.json_error('info', {'uid': self.uid, 'path': live_photo_real_path}, code=codes.RESOURCE_NOT_FOUND)

        self.upload_file(
            self.uid,
            live_photo_store_path,
            file_data={
                'md5': video_md5, 'sha256': video_sha256, 'size': video_size, 'mimetype': 'video/mp4'
            },
            live_photo_md5=photo_md5,
            live_photo_sha256=photo_sha256,
            live_photo_size=photo_size,
            live_photo_type='video',
            opts=opts,
        )

        return photo_md5, photo_sha256, photo_size

@pytest.mark.skipif(not INIT_USER_IN_POSTGRES, reason='Test postgres implementation')
class LivePhotoPostgresTestCase(DiskTestCase, TrashTestCaseMixin):

    def setup_method(self, method):
        super(LivePhotoPostgresTestCase, self).setup_method(method)
        self.json_ok('enable_unlimited_autouploading', {'uid': self.uid})
        self.uid_3 = user_3.uid
        self.email = default_user.email

    def store_live_photo_with_video(self, live_photo_store_path, live_photo_real_path=None, uid=None):
        if uid is None:
            uid = self.uid

        if live_photo_real_path is None:
            live_photo_real_path = live_photo_store_path

        photo_md5 = hashlib.md5(str(time.time())).hexdigest()
        photo_sha256 = hashlib.sha256(str(time.time())).hexdigest()
        photo_size = 1234

        video_md5 = hashlib.md5(str(time.time() + 1)).hexdigest()
        video_sha256 = hashlib.sha256(str(time.time() + 1)).hexdigest()
        video_size = 1234

        self.upload_file(
            uid,
            live_photo_store_path,
            file_data={
                'md5': photo_md5, 'sha256': photo_sha256, 'size': photo_size, 'mimetype': 'image/jpg'
            },
            live_photo_md5=video_md5,
            live_photo_sha256=video_sha256,
            live_photo_size=video_size,
            live_photo_type='photo',
        )

        self.json_error('info', {'uid': uid, 'path': live_photo_real_path}, code=codes.RESOURCE_NOT_FOUND)

        self.upload_file(
            uid,
            live_photo_store_path,
            file_data={
                'md5': video_md5, 'sha256': video_sha256, 'size': video_size, 'mimetype': 'video/mp4'
            },
            live_photo_md5=photo_md5,
            live_photo_sha256=photo_sha256,
            live_photo_size=photo_size,
            live_photo_type='video'
        )

        return photo_md5, photo_sha256, photo_size

    @parameterized.expand([
        ('/disk', '/disk', 'user_data'),
        ('/photostream', u'/disk/Фотокамера', 'user_data'),
        ('/photounlim', '/photounlim', 'photounlim_data'),
    ])
    def test_store_photo_with_video(self, area, real_path_prefix, collection):
        live_photo_store_path = area + '/photo.jpg'
        live_photo_real_path = real_path_prefix + '/photo.jpg'
        photo_md5, photo_sha256, photo_size = self.store_live_photo_with_video(
            live_photo_store_path, live_photo_real_path=live_photo_real_path)

        info = self.json_ok('info', {'uid': self.uid, 'path': live_photo_real_path, 'meta': ''})

        assert info['meta']['md5'] == photo_md5
        assert info['meta']['sha256'] == photo_sha256
        assert info['meta']['size'] == photo_size

        assert info['meta']['is_live_photo'] is True
        assert 'live_video_id' not in info['meta']

        db = CollectionRoutedDatabase()
        item = db[collection].find_one({'uid': self.uid, 'path': live_photo_real_path})
        assert 'live_video_id' not in item

        session = Session.create_from_uid(self.uid)
        all_file_links = list(session.execute('SELECT * FROM disk.additional_file_links WHERE uid=:uid',
                                              {'uid': self.uid}))

        assert len(all_file_links) == 1
        files_link = all_file_links[0]
        live_video_id = Binary(str(files_link['additional_file_fid'].hex))

        live_video_files = list(db.additional_data.find({'uid': self.uid, 'type': 'file'}))
        assert len(live_video_files) == 1
        assert live_video_files[0]['_id'] == live_video_id

    @parameterized.expand([
        ('/disk', '/disk', 'user_data'),
        ('/photounlim', '/photounlim', 'photounlim_data'),
    ])
    def test_store_video_for_already_uploaded_photo(self, area, real_path_prefix, collection):
        live_photo_store_path = area + '/photo.jpg'
        live_photo_real_path = real_path_prefix + '/photo.jpg'

        photo_md5 = hashlib.md5(str(time.time())).hexdigest()
        photo_sha256 = hashlib.sha256(str(time.time())).hexdigest()
        photo_size = 1234

        video_md5 = hashlib.md5(str(time.time() + 1)).hexdigest()
        video_sha256 = hashlib.sha256(str(time.time() + 1)).hexdigest()
        video_size = 1234

        self.upload_file(
            self.uid,
            live_photo_store_path,
            file_data={
                'md5': photo_md5, 'sha256': photo_sha256, 'size': photo_size, 'mimetype': 'image/jpg'
            }
        )

        self.json_ok('info', {'uid': self.uid, 'path': live_photo_real_path})

        self.upload_file(
            self.uid,
            live_photo_store_path,
            file_data={
                'md5': video_md5, 'sha256': video_sha256, 'size': video_size, 'mimetype': 'video/mp4'
            },
            live_photo_md5=photo_md5,
            live_photo_sha256=photo_sha256,
            live_photo_size=photo_size,
            live_photo_type='video'
        )

        info = self.json_ok('info', {'uid': self.uid, 'path': live_photo_real_path, 'meta': ''})

        assert info['meta']['md5'] == photo_md5
        assert info['meta']['sha256'] == photo_sha256
        assert info['meta']['size'] == photo_size

        assert info['meta']['is_live_photo'] is True
        assert 'live_video_id' not in info['meta']

        db = CollectionRoutedDatabase()
        item = db[collection].find_one({'uid': self.uid, 'path': live_photo_real_path})
        assert 'live_video_id' not in item

        session = Session.create_from_uid(self.uid)
        all_file_links = list(session.execute('SELECT * FROM disk.additional_file_links WHERE uid=:uid',
                                              {'uid': self.uid}))

        assert len(all_file_links) == 1
        files_link = all_file_links[0]
        live_video_id = Binary(str(files_link['additional_file_fid'].hex))

        live_video_files = list(db.additional_data.find({'uid': self.uid, 'type': 'file'}))
        assert len(live_video_files) == 1
        assert live_video_files[0]['_id'] == live_video_id

    @parameterized.expand([
        ('/disk', '/disk', 'user_data'),
        ('/photostream', u'/disk/Фотокамера', 'user_data'),
        ('/photounlim', '/photounlim', 'photounlim_data'),
    ])
    def test_simple_photo_with_same_hashes_as_existing_live_photo(self, area, real_path_prefix, collection):
        live_photo_store_path = area + '/photo.jpg'
        live_photo_real_path = real_path_prefix + '/photo.jpg'
        photo_md5, photo_sha256, photo_size = self.store_live_photo_with_video(
            live_photo_store_path, live_photo_real_path=live_photo_real_path)

        regular_photo_store_path = area + '/regular_photo.jpg'
        regular_photo_real_path = real_path_prefix + '/regular_photo.jpg'
        self.upload_file(
            self.uid,
            regular_photo_store_path,
            file_data={
                'md5': photo_md5, 'sha256': photo_sha256, 'size': photo_size, 'mimetype': 'image/jpg'
            },
            hardlink=True,
        )

        info = self.json_ok('info', {'uid': self.uid, 'path': regular_photo_real_path, 'meta': ''})
        assert not info['meta'].get('is_live_photo')

        info = self.json_ok('info', {'uid': self.uid, 'path': live_photo_real_path, 'meta': ''})
        assert info['meta']['is_live_photo']

    @parameterized.expand([
        ('/disk', '/disk', 'user_data'),
        ('/photostream', u'/disk/Фотокамера', 'user_data'),
        ('/photounlim', '/photounlim', 'photounlim_data'),
    ])
    def test_store_video_for_missing_photo(self, area, real_path_prefix, collection):
        live_photo_store_path = area + '/photo.jpg'
        live_photo_real_path = real_path_prefix + '/photo.jpg'

        photo_md5 = hashlib.md5(str(time.time())).hexdigest()
        photo_sha256 = hashlib.sha256(str(time.time())).hexdigest()
        photo_size = 1234

        video_md5 = hashlib.md5(str(time.time() + 1)).hexdigest()
        video_sha256 = hashlib.sha256(str(time.time() + 1)).hexdigest()
        video_size = 1234

        self.json_error('store', {
            'uid': self.uid,
            'path': live_photo_store_path,
            'md5': video_md5,
            'sha256': video_sha256,
            'size': video_size,
            'live_photo_md5': photo_md5,
            'live_photo_sha256': photo_sha256,
            'live_photo_size': photo_size,
            'live_photo_type': 'video',
        }, code=codes.LIVE_PHOTO_NOT_FOUND)

        db = CollectionRoutedDatabase()
        live_video_files = list(db.additional_data.find({'uid': self.uid, 'type': 'file'}))
        assert len(live_video_files) == 0

    def test_trash_append_live_photo_from_folder(self):
        folder_path = '/disk/folder'
        subfolder_path = folder_path + '/subfolder'
        self.json_ok('mkdir', {'uid': self.uid, 'path': folder_path})
        self.json_ok('mkdir', {'uid': self.uid, 'path': subfolder_path})
        live_photo_path = subfolder_path + '/photo.jpg'
        self.store_live_photo_with_video(live_photo_path)

        self.json_ok('trash_append', {'uid': self.uid, 'path': folder_path})

        listing = self.json_ok('list', {'uid': self.uid, 'path': '/trash'})
        assert any([i['path'].startswith('/trash/folder') for i in listing])

    def test_store_video_for_same_multiple_photos(self):
        live_photo_path = '/disk/photo.jpg'
        photo_md5 = hashlib.md5(str(time.time())).hexdigest()
        photo_sha256 = hashlib.sha256(str(time.time())).hexdigest()
        photo_size = 1234

        for i in xrange(4):
            path = '/disk/photo-%d.jpg' % i
            self.upload_file(self.uid, path, file_data={
                'md5': photo_md5, 'sha256': photo_sha256, 'size': photo_size, 'mimetype': 'image/jpg'
            })

        video_md5 = hashlib.md5(str(time.time() + 1)).hexdigest()
        video_sha256 = hashlib.sha256(str(time.time() + 1)).hexdigest()
        video_size = 1234

        self.upload_file(
            self.uid,
            live_photo_path,
            file_data={
                'md5': video_md5, 'sha256': video_sha256, 'size': video_size, 'mimetype': 'video/mp4'
            },
            live_photo_md5=photo_md5,
            live_photo_sha256=photo_sha256,
            live_photo_size=photo_size,
            live_photo_type='video'
        )

        self.json_error('info', {'uid': self.uid, 'path': live_photo_path, 'meta': ''}, code=codes.RESOURCE_NOT_FOUND)

        db = CollectionRoutedDatabase()
        photo_items = list(db.user_data.find({'uid': self.uid, 'type': 'file'}))
        for i in photo_items:
            assert 'live_video_id' not in i['data']

        live_video_files = list(db.additional_data.find({'uid': self.uid, 'type': 'file'}))
        assert len(live_video_files) == 1

        session = Session.create_from_uid(self.uid)
        all_file_links = list(session.execute('SELECT * FROM disk.additional_file_links WHERE uid=:uid',
                                              {'uid': self.uid}))
        assert len(all_file_links) == 0

    def test_copy_live_photo(self):
        live_photo_path = '/disk/photo.jpg'
        self.store_live_photo_with_video(live_photo_path)

        live_photo_copy = '/disk/another-photo.jpg'
        self.json_ok(
            'async_copy',
            {
                'uid': self.uid,
                'src': live_photo_path,
                'dst': live_photo_copy,
            }
        )

        db = CollectionRoutedDatabase()
        live_photo_item = db.user_data.find_one({'uid': self.uid, 'path': live_photo_path})
        live_photo_copy_item = db.user_data.find_one({'uid': self.uid, 'path': live_photo_copy})

        assert 'live_video_id' not in live_photo_item['data']
        assert 'live_video_id' not in live_photo_copy_item['data']

        session = Session.create_from_uid(self.uid)
        all_file_links = list(session.execute('SELECT * FROM disk.additional_file_links WHERE uid=:uid',
                                              {'uid': self.uid}))

        assert len(all_file_links) == 2

        assert all_file_links[0]['main_file_fid'] != all_file_links[1]['main_file_fid']
        assert all_file_links[0]['additional_file_fid'] != all_file_links[1]['additional_file_fid']

    def test_copy_folder_with_live_photo(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/folder'})

        live_photo_path = '/disk/folder/photo.jpg'
        self.store_live_photo_with_video(live_photo_path)

        live_photo_copy = '/disk/another-folder/photo.jpg'
        self.json_ok(
            'async_copy',
            {
                'uid': self.uid,
                'src': '/disk/folder',
                'dst': '/disk/another-folder',
            }
        )

        db = CollectionRoutedDatabase()
        live_photo_item = db.user_data.find_one({'uid': self.uid, 'path': live_photo_path})
        live_photo_copy_item = db.user_data.find_one({'uid': self.uid, 'path': live_photo_copy})

        assert 'live_video_id' not in live_photo_item['data']
        assert 'live_video_id' not in live_photo_copy_item['data']

        session = Session.create_from_uid(self.uid)
        all_file_links = list(session.execute('SELECT * FROM disk.additional_file_links WHERE uid=:uid',
                                              {'uid': self.uid}))

        assert len(all_file_links) == 2

        assert all_file_links[0]['main_file_fid'] != all_file_links[1]['main_file_fid']
        assert all_file_links[0]['additional_file_fid'] != all_file_links[1]['additional_file_fid']

    def test_move_live_photo(self):
        live_photo_path = '/disk/photo.jpg'
        self.store_live_photo_with_video(live_photo_path)

        live_photo_copy = '/disk/another-photo.jpg'
        self.json_ok(
            'async_move',
            {
                'uid': self.uid,
                'src': live_photo_path,
                'dst': live_photo_copy,
            }
        )

        db = CollectionRoutedDatabase()
        live_photo_item = db.user_data.find_one({'uid': self.uid, 'path': live_photo_path})
        live_photo_copy_item = db.user_data.find_one({'uid': self.uid, 'path': live_photo_copy})

        assert live_photo_item is None
        assert 'live_video_id' not in live_photo_copy_item['data']

        session = Session.create_from_uid(self.uid)
        all_file_links = list(session.execute('SELECT * FROM disk.additional_file_links WHERE uid=:uid',
                                              {'uid': self.uid}))
        assert len(all_file_links) == 1
        assert all_file_links[0]['main_file_fid'].hex == live_photo_copy_item['_id']

    def test_move_folder_with_live_photo(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/folder'})

        live_photo_path = '/disk/folder/photo.jpg'
        self.store_live_photo_with_video(live_photo_path)

        live_photo_copy = '/disk/another-folder/photo.jpg'
        self.json_ok(
            'async_move',
            {
                'uid': self.uid,
                'src': '/disk/folder',
                'dst': '/disk/another-folder',
            }
        )

        db = CollectionRoutedDatabase()
        live_photo_item = db.user_data.find_one({'uid': self.uid, 'path': live_photo_path})
        live_photo_copy_item = db.user_data.find_one({'uid': self.uid, 'path': live_photo_copy})

        assert live_photo_item is None
        assert 'live_video_id' not in live_photo_copy_item['data']

        session = Session.create_from_uid(self.uid)
        all_file_links = list(session.execute('SELECT * FROM disk.additional_file_links WHERE uid=:uid',
                                              {'uid': self.uid}))
        assert len(all_file_links) == 1
        assert all_file_links[0]['main_file_fid'].hex == live_photo_copy_item['_id']

    def test_move_to_hidden_live_photo(self):
        live_photo_path = '/disk/photo.jpg'
        self.store_live_photo_with_video(live_photo_path)

        self.json_ok('async_trash_append', {'uid': self.uid, 'path': live_photo_path})
        self.json_ok('trash_drop_all', {'uid': self.uid})

        db = CollectionRoutedDatabase()
        live_photo_item = db.user_data.find_one({'uid': self.uid, 'path': live_photo_path})
        assert live_photo_item is None

        live_video_items = list(db.additional_data.find({'uid': self.uid, 'type': 'file'}))
        assert len(live_video_items) == 1

        hidden_data_files = list(db.hidden_data.find({'uid': self.uid, 'type': 'file'}))
        print hidden_data_files
        assert len(hidden_data_files) == 1

    def test_move_to_hidden_single_live_photo(self):
        live_photo_path = '/disk/photo.jpg'
        self.store_live_photo_with_video(live_photo_path)

        self.json_ok('async_trash_append', {'uid': self.uid, 'path': live_photo_path})

        live_photo_trash_path = self.get_trashed_item(live_photo_path.replace('/disk', '/trash'))['path']
        self.json_ok('trash_drop', {'uid': self.uid, 'path': live_photo_trash_path})

        db = CollectionRoutedDatabase()
        live_photo_item = db.user_data.find_one({'uid': self.uid, 'path': live_photo_path})
        assert live_photo_item is None

        live_video_items = list(db.additional_data.find({'uid': self.uid, 'type': 'file'}))
        assert len(live_video_items) == 1

        hidden_data_files = list(db.hidden_data.find({'uid': self.uid, 'type': 'file'}))
        print hidden_data_files
        assert len(hidden_data_files) == 1

    def test_restore_deleted_live_photo(self):
        live_photo_path = '/disk/photo.jpg'
        self.store_live_photo_with_video(live_photo_path)

        with mock.patch('mpfs.core.address.Address.add_trash_suffix'):
            self.json_ok('async_trash_append', {'uid': self.uid, 'path': live_photo_path})
        self.json_ok('trash_drop_all', {'uid': self.uid})

        db = CollectionRoutedDatabase()
        hidden_data_files = list(db.hidden_data.find({'uid': self.uid, 'type': 'file'}))
        print hidden_data_files
        assert len(hidden_data_files) == 1

        hidden_data_live_photo_path = hidden_data_files[0]['key']
        folder_to_restore = '/disk/restored'
        self.json_ok('mkdir', {'uid': self.uid, 'path': folder_to_restore})
        self.service_ok(
            'restore_deleted',
            {
                'uid': self.uid,
                'path': '%s:%s' % (self.uid, hidden_data_live_photo_path),
                'dest': '%s:%s' % (self.uid, folder_to_restore),
                'force': 0
            }
        )

        live_photo_path = live_photo_path.replace('/disk', folder_to_restore)
        info = self.json_ok('info', {'uid': self.uid, 'path': live_photo_path, 'meta': ''})

        assert info['meta']['is_live_photo'] is True
        assert 'live_video_id' not in info['meta']

        db = CollectionRoutedDatabase()
        item = db.user_data.find_one({'uid': self.uid, 'path': live_photo_path})
        assert 'live_video_id' not in item['data']

        session = Session.create_from_uid(self.uid)
        all_file_links = list(session.execute('SELECT * FROM disk.additional_file_links WHERE uid=:uid',
                                              {'uid': self.uid}))
        assert len(all_file_links) == 1
        assert all_file_links[0]['main_file_fid'].hex == item['_id']

    def test_hidden_data_clean_with_live_photo(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/folder'})
        live_photo_path = '/disk/folder/photo.jpg'
        self.store_live_photo_with_video(live_photo_path)

        self.json_ok('async_trash_append', {'uid': self.uid, 'path': '/disk/folder'})
        self.json_ok('trash_drop_all', {'uid': self.uid})

        db = CollectionRoutedDatabase()
        hidden_data_files = list(db.hidden_data.find({'uid': self.uid, 'type': 'file'}))
        assert len(hidden_data_files) == 1
        live_video_files = list(db.additional_data.find({'uid': self.uid, 'type': 'file'}))
        assert len(live_video_files) == 1

        shard = PGQueryExecuter().get_shard_id(self.uid)
        with mock.patch.object(HiddenDataCleanerToggle, 'is_enable', return_value=True), \
                mock.patch.object(HiddenDataCleanerWorker, 'CLEAN_DELAY', 0), \
                mock.patch.object(HiddenDataCleanerWorker, 'MIN_FILE_SIZE', 0):
            HiddenDataCleanerWorker().put(shard)

        db = CollectionRoutedDatabase()
        hidden_data_files = list(db.hidden_data.find({'uid': self.uid, 'type': 'file'}))
        assert len(hidden_data_files) == 0
        live_video_files = list(db.additional_data.find({'uid': self.uid, 'type': 'file'}))
        assert len(live_video_files) == 0

    def test_rm_live_photo_from_shared_folder(self):
        live_photo_path = '/disk/photo.jpg'
        self.store_live_photo_with_video(live_photo_path)

        self.create_user(self.uid_3)
        shared_folder_path = '/disk/folder'
        self.json_ok('mkdir', {'uid': self.uid_3, 'path': shared_folder_path})
        gid = self.json_ok('share_create_group', {'uid': self.uid_3, 'path': shared_folder_path})['gid']
        hsh = self.json_ok(
            'share_invite_user',
            {'uid': self.uid_3, 'gid': gid, 'universe_login': self.email, 'universe_service': 'email', 'rights': 660}
        )['hash']
        self.json_ok('share_activate_invite', {'uid': self.uid, 'hash': hsh})

        live_photo_copy = shared_folder_path + '/another-photo.jpg'
        self.json_ok(
            'async_move',
            {
                'uid': self.uid,
                'src': live_photo_path,
                'dst': live_photo_copy,
            }
        )

        self.json_ok(
            'async_rm',
            {
                'uid': self.uid,
                'path': live_photo_copy
            }
        )

        db = CollectionRoutedDatabase()
        hidden_data_files = list(db.hidden_data.find({'uid': self.uid, 'type': 'file'}))

        assert len(hidden_data_files) == 1
        assert hidden_data_files[0]['uid'] == self.uid

    def test_move_folder_with_subfolder_with_live_photo_to_shared_folder(self):
        self.create_user(self.uid_3)
        shared_folder_path = '/disk/folder'
        self.json_ok('mkdir', {'uid': self.uid_3, 'path': shared_folder_path})
        gid = self.json_ok('share_create_group', {'uid': self.uid_3, 'path': shared_folder_path})['gid']
        hsh = self.json_ok(
            'share_invite_user',
            {'uid': self.uid_3, 'gid': gid, 'universe_login': self.email, 'universe_service': 'email', 'rights': 660}
        )['hash']
        self.json_ok('share_activate_invite', {'uid': self.uid, 'hash': hsh})

        participant_folder = '/disk/participant-folder'
        participant_subfolder = participant_folder + '/subfolder'
        self.json_ok('mkdir', {'uid': self.uid, 'path': participant_folder})
        self.json_ok('mkdir', {'uid': self.uid, 'path': participant_subfolder})

        live_photo_path = participant_subfolder + '/photo.jpg'
        self.store_live_photo_with_video(live_photo_path)

        folder_dst_path = shared_folder_path + '/inner'
        self.json_ok(
            'move',
            {
                'uid': self.uid,
                'src': participant_folder,
                'dst': folder_dst_path,
            }
        )

        file_info = self.json_ok('info', {'uid': self.uid, 'path': folder_dst_path + '/subfolder/photo.jpg',
                                          'meta': 'is_live_photo'})
        assert file_info['meta']['is_live_photo'] is True

    @staticmethod
    def _generate_hashes(offset=0):
        md5 = hashlib.md5(str(time.time() + offset)).hexdigest()
        sha256 = hashlib.sha256(str(time.time() + offset)).hexdigest()
        size = 1234 + offset
        return md5, sha256, size

    def test_simultaneous_upload_with_same_name(self):
        path_1 = '/photostream/2018-09-05%2017-34-55.JPG'
        path_2 = path_1

        photo_md5_1, photo_sha256_1, photo_size_1 = self._generate_hashes()
        video_md5_1, video_sha256_1, video_size_1 = self._generate_hashes(1)

        photo_md5_2, photo_sha256_2, photo_size_2 = self._generate_hashes(2)
        video_md5_2, video_sha256_2, video_size_2 = self._generate_hashes(3)

        self.upload_file(
            self.uid,
            path_1,
            file_data={
                'md5': photo_md5_1, 'sha256': photo_sha256_1, 'size': photo_size_1, 'mimetype': 'image/jpg'
            },
            live_photo_md5=video_md5_1,
            live_photo_sha256=video_sha256_1,
            live_photo_size=video_size_1,
            live_photo_type='photo',
        )
        self.upload_file(
            self.uid,
            path_2,
            file_data={
                'md5': photo_md5_2, 'sha256': photo_sha256_2, 'size': photo_size_2, 'mimetype': 'image/jpg'
            },
            live_photo_md5=video_md5_2,
            live_photo_sha256=video_sha256_2,
            live_photo_size=video_size_2,
            live_photo_type='photo',
        )

        self.upload_file(
            self.uid,
            path_1,
            file_data={
                'md5': video_md5_1, 'sha256': video_sha256_1, 'size': video_size_1, 'mimetype': 'video/mp4'
            },
            live_photo_md5=photo_md5_1,
            live_photo_sha256=photo_sha256_1,
            live_photo_size=photo_size_1,
            live_photo_type='video'
        )

        self.upload_file(
            self.uid,
            path_2,
            file_data={
                'md5': video_md5_2, 'sha256': video_sha256_2, 'size': video_size_2, 'mimetype': 'video/mp4'
            },
            live_photo_md5=photo_md5_2,
            live_photo_sha256=photo_sha256_2,
            live_photo_size=photo_size_2,
            live_photo_type='video'
        )


class LivePhotoPushesTestCase(DiskTestCase):
    def test_store_photo_with_video_xiva_pushes(self):
        live_photo_path = '/disk/photo.jpg'
        photo_md5 = hashlib.md5(str(time.time())).hexdigest()
        photo_sha256 = hashlib.sha256(str(time.time())).hexdigest()
        photo_size = 1234

        video_md5 = hashlib.md5(str(time.time() + 1)).hexdigest()
        video_sha256 = hashlib.sha256(str(time.time() + 1)).hexdigest()
        video_size = 1234

        with SearchIndexerStub() as index_service:
            self.upload_file(
                self.uid,
                live_photo_path,
                file_data={
                    'md5': photo_md5, 'sha256': photo_sha256, 'size': photo_size, 'mimetype': 'image/jpg'
                },
                live_photo_md5=video_md5,
                live_photo_sha256=video_sha256,
                live_photo_size=video_size,
                live_photo_type='photo',
            )
            assert index_service.push_change.call_count == 0  # проверяем, что не было нотификаций в поиск

        with SearchIndexerStub() as index_service:
            self.upload_file(
                self.uid,
                live_photo_path,
                file_data={
                    'md5': video_md5, 'sha256': video_sha256, 'size': video_size, 'mimetype': 'video/mp4'
                },
                live_photo_md5=photo_md5,
                live_photo_sha256=photo_sha256,
                live_photo_size=photo_size,
                live_photo_type='video',
            )
            assert index_service.push_change.call_count > 0  # проверяем, что были нотификаций в поиск

        self.json_ok('info', {'uid': self.uid, 'path': live_photo_path})


@pytest.mark.skipif(not INIT_USER_IN_POSTGRES, reason='Postgres test')
class LivePhotoWithHardlinksTestCase(DiskTestCase):

    def test_store_with_photo_hardlink(self):
        live_photo_path = '/disk/photo.jpg'
        photo_md5 = hashlib.md5(str(time.time())).hexdigest()
        photo_sha256 = hashlib.sha256(str(time.time())).hexdigest()
        photo_size = 1234

        video_md5 = hashlib.md5(str(time.time() + 1)).hexdigest()
        video_sha256 = hashlib.sha256(str(time.time() + 1)).hexdigest()
        video_size = 1234

        self.upload_file(
            self.uid,
            '/disk/some-path-for-file-to-be-hardlinked-with.jpg',
            file_data={
                'md5': photo_md5, 'sha256': photo_sha256, 'size': photo_size, 'mimetype': 'image/jpg'
            },
        )

        self.upload_file(
            self.uid,
            live_photo_path,
            file_data={
                'md5': photo_md5, 'sha256': photo_sha256, 'size': photo_size, 'mimetype': 'image/jpg'
            },
            live_photo_md5=video_md5,
            live_photo_sha256=video_sha256,
            live_photo_size=video_size,
            live_photo_type='photo',
            hardlink=True
        )

        self.json_error('info', {'uid': self.uid, 'path': live_photo_path}, code=codes.RESOURCE_NOT_FOUND)

        self.upload_file(
            self.uid,
            live_photo_path,
            file_data={
                'md5': video_md5, 'sha256': video_sha256, 'size': video_size, 'mimetype': 'video/mp4'
            },
            live_photo_md5=photo_md5,
            live_photo_sha256=photo_sha256,
            live_photo_size=photo_size,
            live_photo_type='video'
        )

        info = self.json_ok('info', {'uid': self.uid, 'path': live_photo_path, 'meta': ''})

        assert info['meta']['md5'] == photo_md5
        assert info['meta']['sha256'] == photo_sha256
        assert info['meta']['size'] == photo_size

        assert info['meta']['is_live_photo'] is True

    def test_store_with_video_hardlink(self):
        live_photo_path = '/disk/photo.jpg'
        photo_md5 = hashlib.md5(str(time.time())).hexdigest()
        photo_sha256 = hashlib.sha256(str(time.time())).hexdigest()
        photo_size = 1234

        video_md5 = hashlib.md5(str(time.time() + 1)).hexdigest()
        video_sha256 = hashlib.sha256(str(time.time() + 1)).hexdigest()
        video_size = 1234

        self.upload_file(
            self.uid,
            '/disk/some-path-for-file-to-be-hardlinked-with.jpg',
            file_data={
                'md5': video_md5, 'sha256': video_sha256, 'size': video_size, 'mimetype': 'video/mp4'
            },
        )

        self.upload_file(
            self.uid,
            live_photo_path,
            file_data={
                'md5': photo_md5, 'sha256': photo_sha256, 'size': photo_size, 'mimetype': 'image/jpg'
            },
            live_photo_md5=video_md5,
            live_photo_sha256=video_sha256,
            live_photo_size=video_size,
            live_photo_type='photo',
        )

        self.json_error('info', {'uid': self.uid, 'path': live_photo_path}, code=codes.RESOURCE_NOT_FOUND)

        self.upload_file(
            self.uid,
            live_photo_path,
            file_data={
                'md5': video_md5, 'sha256': video_sha256, 'size': video_size, 'mimetype': 'video/mp4'
            },
            live_photo_md5=photo_md5,
            live_photo_sha256=photo_sha256,
            live_photo_size=photo_size,
            live_photo_type='video',
            hardlink=True
        )

        info = self.json_ok('info', {'uid': self.uid, 'path': live_photo_path, 'meta': ''})

        assert info['meta']['md5'] == photo_md5
        assert info['meta']['sha256'] == photo_sha256
        assert info['meta']['size'] == photo_size

        assert info['meta']['is_live_photo'] is True

    def test_store_hardlink_with_another_live_photo(self):
        live_photo_path = '/disk/photo.jpg'
        another_live_photo_path = '/disk/photo-2.jpg'

        photo_md5 = hashlib.md5(str(time.time())).hexdigest()
        photo_sha256 = hashlib.sha256(str(time.time())).hexdigest()
        photo_size = 1234

        video_md5 = hashlib.md5(str(time.time() + 1)).hexdigest()
        video_sha256 = hashlib.sha256(str(time.time() + 1)).hexdigest()
        video_size = 1234

        self.upload_file(
            self.uid,
            live_photo_path,
            file_data={
                'md5': photo_md5, 'sha256': photo_sha256, 'size': photo_size, 'mimetype': 'image/jpg'
            },
            live_photo_md5=video_md5,
            live_photo_sha256=video_sha256,
            live_photo_size=video_size,
            live_photo_type='photo'
        )

        self.json_error('info', {'uid': self.uid, 'path': live_photo_path}, code=codes.RESOURCE_NOT_FOUND)

        self.upload_file(
            self.uid,
            live_photo_path,
            file_data={
                'md5': video_md5, 'sha256': video_sha256, 'size': video_size, 'mimetype': 'video/mp4'
            },
            live_photo_md5=photo_md5,
            live_photo_sha256=photo_sha256,
            live_photo_size=photo_size,
            live_photo_type='video'
        )

        info = self.json_ok('info', {'uid': self.uid, 'path': live_photo_path, 'meta': ''})

        assert info['meta']['md5'] == photo_md5
        assert info['meta']['sha256'] == photo_sha256
        assert info['meta']['size'] == photo_size

        assert info['meta']['is_live_photo'] is True

        self.upload_file(
            self.uid,
            another_live_photo_path,
            file_data={
                'md5': photo_md5, 'sha256': photo_sha256, 'size': photo_size, 'mimetype': 'image/jpg'
            },
            live_photo_md5=video_md5,
            live_photo_sha256=video_sha256,
            live_photo_size=video_size,
            live_photo_type='photo',
            hardlink=True
        )

        self.json_error('info', {'uid': self.uid, 'path': another_live_photo_path}, code=codes.RESOURCE_NOT_FOUND)

        self.upload_file(
            self.uid,
            another_live_photo_path,
            file_data={
                'md5': video_md5, 'sha256': video_sha256, 'size': video_size, 'mimetype': 'video/mp4'
            },
            live_photo_md5=photo_md5,
            live_photo_sha256=photo_sha256,
            live_photo_size=photo_size,
            live_photo_type='video',
            hardlink=True
        )

        info = self.json_ok('info', {'uid': self.uid, 'path': another_live_photo_path, 'meta': ''})

        assert info['meta']['md5'] == photo_md5
        assert info['meta']['sha256'] == photo_sha256
        assert info['meta']['size'] == photo_size

        assert info['meta']['is_live_photo'] is True


class LivePhotosInEventHistoryLogTestCase(CommonSharingMethods, EventHistoryTestCase):
    photo_md5 = hashlib.md5('1').hexdigest()
    photo_sha256 = hashlib.sha256('1').hexdigest()
    photo_size = 1234

    video_md5 = hashlib.md5('2').hexdigest()
    video_sha256 = hashlib.sha256('2').hexdigest()
    video_size = 1234

    def test_store_live_photo_doesnt_log_video_parts(self):
        self.reset_mock_log()
        live_photo_path = '/disk/photo.jpg'

        self.upload_file(
            self.uid,
            live_photo_path,
            file_data={
                'md5': self.photo_md5, 'sha256': self.photo_sha256, 'size': self.photo_size, 'mimetype': 'image/jpg'
            },
            live_photo_md5=self.video_md5,
            live_photo_sha256=self.video_sha256,
            live_photo_size=self.video_size,
            live_photo_type='photo',
        )

        self.upload_file(
            self.uid,
            live_photo_path,
            file_data={
                'md5': self.video_md5, 'sha256': self.video_sha256, 'size': self.video_size, 'mimetype': 'video/mp4'
            },
            live_photo_md5=self.photo_md5,
            live_photo_sha256=self.photo_sha256,
            live_photo_size=self.photo_size,
            live_photo_type='video',
        )

        assert len(self.mock_log.call_args_list) == 1
        for called_args in self.mock_log.call_args_list:
            _, actual_kwargs = self.extract_logged_arguments(called_args)
            assert Address(actual_kwargs['tgt_rawaddress']).storage_name != ADDITIONAL_AREA

    @parameterized.expand([
        (True,),
        # (False,),  # skip until https://st.yandex-team.ru/CHEMODAN-68488
    ])
    def test_store_shared_live_photo_doesnt_log_video_parts(self, action_is_made_by_owner):
        self.json_ok('user_init', {'uid': self.uid_3})
        args = {'uid': self.uid, 'path': '/disk/shared_folder'}
        self.json_ok('mkdir', args)
        gid = self.create_group(path='/disk/shared_folder')
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid, path='/disk/shared_folder')
        self.activate_invite(uid=self.uid_3, hash=hsh)

        live_photo_path = '/disk/shared_folder/photo.jpg'

        self.reset_mock_log()

        if action_is_made_by_owner:
            uploader_uid = self.uid
        else:
            uploader_uid = self.uid_3
        self.upload_file(
            uploader_uid,
            live_photo_path,
            file_data={
                'md5': self.photo_md5, 'sha256': self.photo_sha256, 'size': self.photo_size, 'mimetype': 'image/jpg'
            },
            live_photo_md5=self.video_md5,
            live_photo_sha256=self.video_sha256,
            live_photo_size=self.video_size,
            live_photo_type='photo',
        )

        self.upload_file(
            uploader_uid,
            live_photo_path,
            file_data={
                'md5': self.video_md5, 'sha256': self.video_sha256, 'size': self.video_size, 'mimetype': 'video/mp4'
            },
            live_photo_md5=self.photo_md5,
            live_photo_sha256=self.photo_sha256,
            live_photo_size=self.photo_size,
            live_photo_type='video',
        )

        assert len(self.mock_log.call_args_list) == 2
        for called_args in self.mock_log.call_args_list:
            _, actual_kwargs = self.extract_logged_arguments(called_args)
            assert Address(actual_kwargs['tgt_rawaddress']).storage_name != ADDITIONAL_AREA
