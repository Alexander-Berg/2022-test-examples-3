# -*- coding: utf-8 -*-
import mock
import pytest
from nose_parameterized import parameterized

from mpfs.common.static import codes
from mpfs.core.filesystem.hardlinks.common import FileChecksums
from mpfs.core.global_gallery.dao.deletion_log import DeletionLogDAO
from mpfs.core.global_gallery.dao.source_id import SourceIdDAO
from test.base import DiskTestCase
from test.conftest import INIT_USER_IN_POSTGRES
from test.parallelly.global_gallery.base import GlobalGalleryTestCaseMixin
from test.parallelly.live_photo_suit import LivePhotoMixin


@pytest.mark.skipif(not INIT_USER_IN_POSTGRES, reason='pg only feature')
class GlobalGalleryAddSourceIdEndpointTestCase(LivePhotoMixin, GlobalGalleryTestCaseMixin, DiskTestCase):

    def test_error_code_on_404(self):
        self.json_ok('add_source_ids', {
            'uid': self.uid,
            'md5': 'b5c64136bd7f685a5bfa1e9c78d7e565',
            'sha256': 'ca4f3cbf0bdf65858cd0f12dbff6a611dfdf13037ebfa4a31199155016f64853',
            'size': 16029,
        }, json={'source_ids': ['111', ]})
        assert not list(SourceIdDAO().find({'uid': self.uid}))

    @parameterized.expand([
        (0,),
        (1,),
    ])
    def test_add_source_ids(self, is_live_photo):
        path = '/disk/1.jpg'
        if is_live_photo:
            self.store_live_photo_with_video(path)
        else:
            self.upload_file(self.uid, path)

        source_id = '111'
        r = self.json_ok('info', {'uid': self.uid, 'path': path, 'meta': ''})
        md5, sha256, size = r['meta']['md5'], r['meta']['sha256'], r['meta']['size']
        self.json_ok('add_source_ids', {'uid': self.uid, 'md5': md5, 'sha256': sha256, 'size': size, 'is_live_photo': is_live_photo},
                     json={'source_ids': [source_id, ]})

        all_source_ids = list(SourceIdDAO().find({'uid': self.uid}))
        assert 1 == len(all_source_ids)
        assert all_source_ids[0]['source_id'] == source_id

    @parameterized.expand([
        (0,),
        (1,),
    ])
    def test_cant_find_source_id_if_is_live_photo_flag_is_wrong(self, is_live_photo):
        path = '/disk/1.jpg'
        if is_live_photo:
            self.upload_file(self.uid, path)
        else:
            self.store_live_photo_with_video(path)
        assert not len(list(SourceIdDAO().find({'uid': self.uid})))

        source_id = '111'
        r = self.json_ok('info', {'uid': self.uid, 'path': path, 'meta': ''})
        md5, sha256, size = r['meta']['md5'], r['meta']['sha256'], r['meta']['size']
        self.json_ok('add_source_ids',
                     {'uid': self.uid, 'md5': md5, 'sha256': sha256, 'size': size, 'is_live_photo': is_live_photo},
                     json={'source_ids': [source_id, ]})
        assert not len(list(SourceIdDAO().find({'uid': self.uid})))


@pytest.mark.skipif(not INIT_USER_IN_POSTGRES, reason='pg only feature')
class GlobalGalleryCheckBySourceIdTestCase(GlobalGalleryTestCaseMixin, DiskTestCase):

    def test_check_record_doesnt_exist(self):
        self.json_error('check_source_id', {'uid': self.uid, 'source_id': '111'}, code=codes.UPLOAD_RECORD_NOT_FOUND)

    @parameterized.expand([
        (True,),
        (False,),
    ])
    def test_check_record_exist(self, file_uploaded):
        md5 = 'b5c64136bd7f685a5bfa1e9c78d7e565'
        sha256 = 'ca4f3cbf0bdf65858cd0f12dbff6a611dfdf13037ebfa4a31199155016f64853'
        size = 16029

        if file_uploaded:
            path = '/disk/1.jpg'
            self.upload_file(self.uid, path, file_data={'md5': md5, 'sha256': sha256, 'size': size})
            r = self.json_ok('info', {'uid': self.uid, 'path': path, 'meta': ''})

        self._insert_source_id_record(self.uid, FileChecksums(md5, sha256, size).hid, '111')
        self.json_ok('check_source_id', {'uid': self.uid, 'source_id': '111'})

    @parameterized.expand([
        ('requested_empty_chunk', [], {}),
        ('requested_all_source_ids', ['0', '1', '2'], {'0': True, '1': True, '2': True}),
        ('requested_missing_source_ids', ['3', '4'], {'3': False, '4': False}),
        ('requested_extra_source_id', ['0', '1', '2', '3'], {'0': True, '1': True, '2': True, '3': False}),
        ('requested_same_multiple_times', ['0', '0', '0'], {'0': True, }),
        ('requested_same_missing_multiple_times', ['3', '3', '3'], {'3': False, }),
    ])
    def test_check_records_exist_bulk(self, case_name, requested_source_ids, correct_answer):
        md5 = 'b5c64136bd7f685a5bfa1e9c78d7e565'
        sha256 = 'ca4f3cbf0bdf65858cd0f12dbff6a611dfdf13037ebfa4a31199155016f64853'
        size = 16029

        for i in range(3):
            self._insert_source_id_record(self.uid, FileChecksums(md5, sha256, size).hid, str(i))

        res = self.json_ok('check_source_ids', {'uid': self.uid}, json={'source_ids': requested_source_ids})
        assert set([x['source_id'] for x in res['items']]) == set(requested_source_ids)
        for x in res['items']:
            assert x['found'] == correct_answer[x['source_id']]


@pytest.mark.skipif(not INIT_USER_IN_POSTGRES, reason='pg only feature')
class GlobalGalleryReadDeletionLogTestCase(GlobalGalleryTestCaseMixin, DiskTestCase):

    def test_format(self):
        for i in range(2):
            self.upload_file(self.uid, '/disk/%s.jpg' % i, opts={'source_id': str(i)})
            self.json_ok('trash_append', {'uid': self.uid, 'path': '/disk/%s.jpg' % i})

        r = self.json_ok('read_deletion_log', {'uid': self.uid})
        assert set(r.keys()) == {'total', 'items', 'deletion_log_revision'}
        for i in r['items']:
            assert set(i.keys()) == {'resource_id', 'hid', 'uid', 'source_ids', 'is_live_photo', 'deletion_log_revision'}

    def test_reading_deletion_log(self):
        with mock.patch('mpfs.core.global_gallery.logic.controller.GLOBAL_GALLERY_DELETION_LOG_CHUNK_SIZE', 3):
            correct_resource_ids = []
            for i in range(5):
                self.upload_file(self.uid, '/disk/%s.jpg' % i, opts={'source_id': str(i)})
                r = self.json_ok('info', {'uid': self.uid, 'path': '/disk/%s.jpg' % i, 'meta': ''})
                correct_resource_ids.append(r['meta']['resource_id'])
                self.json_ok('trash_append', {'uid': self.uid, 'path': '/disk/%s.jpg' % i})

            read_resource_ids = []

            last_deletion_log_revision = self._parse_response_and_assert(self.uid, 0, read_resource_ids, 3)
            last_deletion_log_revision = self._parse_response_and_assert(self.uid, last_deletion_log_revision, read_resource_ids, 2)
            self._parse_response_and_assert(self.uid, last_deletion_log_revision, read_resource_ids, 0)

            assert all([x == y for x, y in zip(correct_resource_ids, read_resource_ids)])

    @parameterized.expand([
        ('no_revision', False),
        ('0_revision_no_file', False),
        ('0_revision_is_file', True),
        ('revision_and_file_same', True),
        ('revision_higher', True),
    ])
    def test_no_new_records(self, case, file_deleted):
        path = '/disk/1.jpg'
        if file_deleted:
            self.upload_file(self.uid, path, opts={'source_id': '111'})
            self.json_ok('trash_append', {'uid': self.uid, 'path': path})
        opts = {'uid': self.uid}
        if case == 'no_revision':
            expected_revision = 0
        elif case == '0_revision_no_file':
            expected_revision = opts['deletion_log_revision'] = 0
        elif case == '0_revision_is_file':
            expected_revision = list(DeletionLogDAO().find({'uid': self.uid}))[0]['deletion_log_revision']
            opts['deletion_log_revision'] = 0
        elif case == 'revision_and_file_same':
            expected_revision = opts['deletion_log_revision'] = list(
                DeletionLogDAO().find({'uid': self.uid}))[0]['deletion_log_revision']
        else:
            expected_revision = opts['deletion_log_revision'] = list(
                DeletionLogDAO().find({'uid': self.uid}))[0]['deletion_log_revision'] + 100

        r = self.json_ok('read_deletion_log', opts)

        assert r['deletion_log_revision'] == expected_revision

    def _parse_response_and_assert(self, uid, last_deletion_log_revision, resource_ids_buffer, correct_amount):
        opts = {'uid': uid}
        if last_deletion_log_revision:
            opts['deletion_log_revision'] = last_deletion_log_revision
        r = self.json_ok('read_deletion_log', opts)
        assert r['total'] == correct_amount
        resource_ids_buffer.extend([x['resource_id'] for x in r['items']])
        return r['deletion_log_revision']
