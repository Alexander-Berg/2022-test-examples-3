# -*- coding: utf-8 -*-
import time

import mock
import pytest
from hamcrest import assert_that, calling, raises
from nose_parameterized import parameterized

from mpfs.common.util.experiments.logic import change_experiment_context_with
from mpfs.core import factory
from mpfs.core.filesystem.hardlinks.common import FileChecksums
from mpfs.core.global_gallery.dao.deletion_log import DeletionLogDAO
from mpfs.core.global_gallery.dao.source_id import SourceIdDAO
from mpfs.core.global_gallery.logic.controller import GlobalGalleryController
from mpfs.core.global_gallery.logic.errors import UploadRecordNotFoundError
from test.base import DiskTestCase
from test.conftest import INIT_USER_IN_POSTGRES
from test.parallelly.global_gallery.base import GlobalGalleryTestCaseMixin
from test.parallelly.json_api.base import CommonJsonApiTestCase
from test.parallelly.live_photo_suit import LivePhotoMixin


@pytest.mark.skipif(not INIT_USER_IN_POSTGRES, reason='pg only feature')
class GlobalGalleryAddSourceIdToFileTestCase(GlobalGalleryTestCaseMixin, CommonJsonApiTestCase):

    def setup_method(self, method):
        super(GlobalGalleryAddSourceIdToFileTestCase, self).setup_method(method)
        self.json_ok('enable_unlimited_autouploading', {'uid': self.uid})

    @parameterized.expand([
        ('disk',),
        ('photounlim',),
        ('trash',),
        ('hidden',),
    ])
    def test_add_source_id_for_a_single_file_no_record_in_upload_log(self, area):
        upload_path = '/disk/123.txt' if area != 'photounlim' else '/photounlim/123.txt'
        self.upload_file(self.uid, upload_path)
        r = self.json_ok('info', {'uid': self.uid, 'path': upload_path, 'meta': ''})
        if area == 'trash':
            self.json_ok('trash_append', {'uid': self.uid, 'path': upload_path})
        elif area == 'hidden':
            self.json_ok('rm', {'uid': self.uid, 'path': upload_path})

        hid = FileChecksums(r['meta']['md5'], r['meta']['sha256'], int(r['meta']['size'])).hid
        with change_experiment_context_with(uid=self.uid):
            GlobalGalleryController.add_source_ids_to_file(self.uid, hid, ['123532435435'])
        assert list(SourceIdDAO().find({'uid': self.uid}))

    def test_several_files_with_one_hid_receive_adding_single_source_id_update_no_record_in_upload_log(self):
        self.upload_file(self.uid, '/disk/1.txt')
        r = self.json_ok('info', {'uid': self.uid, 'path': '/disk/1.txt', 'meta': ''})
        self.upload_file(self.uid, '/disk/2.txt', hardlink=True,
                         file_data={'md5': r['meta']['md5'], 'sha256': r['meta']['sha256'], 'size': r['meta']['size']})

        hid = FileChecksums(r['meta']['md5'], r['meta']['sha256'], int(r['meta']['size'])).hid
        with change_experiment_context_with(uid=self.uid):
            GlobalGalleryController.add_source_ids_to_file(self.uid, hid, ['123532435435'])
        assert 1 == len(list(SourceIdDAO().find({'uid': self.uid})))

    def test_source_id_records_exist_in_upload_log_no_file_in_disk(self):
        md5 = 'b5c64136bd7f685a5bfa1e9c78d7e565'
        sha256 = 'ca4f3cbf0bdf65858cd0f12dbff6a611dfdf13037ebfa4a31199155016f64853'
        size = 16029

        self._insert_source_id_record(self.uid, FileChecksums(md5, sha256, size).hid, '111')

        with change_experiment_context_with(uid=self.uid):
            GlobalGalleryController.add_source_ids_to_file(self.uid, FileChecksums(md5, sha256, int(size)).hid, ['222', '333'])

        assert 3 == len(list(SourceIdDAO().find({'uid': self.uid})))

    def test_dont_add_source_id_if_file_exists_and_source_id_exists_in_upload_log(self):
        md5 = 'b5c64136bd7f685a5bfa1e9c78d7e565'
        sha256 = 'ca4f3cbf0bdf65858cd0f12dbff6a611dfdf13037ebfa4a31199155016f64853'
        size = 16029

        self._insert_source_id_record(self.uid, FileChecksums(md5, sha256, size).hid, '111')

        path = '/disk/123.txt'
        self.upload_file(self.uid, path, file_data={'md5': md5, 'sha256': sha256, 'size': size})
        self.json_ok('info', {'uid': self.uid, 'path': path, 'meta': ''})

        with change_experiment_context_with(uid=self.uid):
            GlobalGalleryController.add_source_ids_to_file(self.uid, FileChecksums(md5, sha256, int(size)).hid, ['222'])

        assert 2 == len(list(SourceIdDAO().find({'uid': self.uid})))

    def test_do_not_add_record_if_file_has_source_id(self):
        md5 = 'b5c64136bd7f685a5bfa1e9c78d7e565'
        sha256 = 'ca4f3cbf0bdf65858cd0f12dbff6a611dfdf13037ebfa4a31199155016f64853'
        size = 16029

        # Пусть есть две записи. В результате, должны добавить только один source_id
        self._insert_source_id_record(self.uid, FileChecksums(md5, sha256, size).hid, '111')

        with change_experiment_context_with(uid=self.uid):
            GlobalGalleryController.add_source_ids_to_file(self.uid, FileChecksums(md5, sha256, int(size)).hid, ['111', '222'])
        assert 2 == len(list(SourceIdDAO().find({'uid': self.uid})))

    def test_add_source_ids_limit_exceeded_during_adding(self):
        md5 = 'b5c64136bd7f685a5bfa1e9c78d7e565'
        sha256 = 'ca4f3cbf0bdf65858cd0f12dbff6a611dfdf13037ebfa4a31199155016f64853'
        size = 16029

        self._insert_source_id_record(self.uid, FileChecksums(md5, sha256, size).hid, '111')
        with mock.patch('mpfs.core.global_gallery.logic.controller.GLOBAL_GALLERY_MAX_SOURCE_ID_LIMIT', 2), \
                change_experiment_context_with(uid=self.uid):
            GlobalGalleryController.add_source_ids_to_file(self.uid, FileChecksums(md5, sha256, int(size)).hid, ['222', '333'])
        assert 2 == len(list(SourceIdDAO().find({'uid': self.uid})))

    def test_add_source_ids_limit_exceeded_before_adding(self):
        md5 = 'b5c64136bd7f685a5bfa1e9c78d7e565'
        sha256 = 'ca4f3cbf0bdf65858cd0f12dbff6a611dfdf13037ebfa4a31199155016f64853'
        size = 16029

        self._insert_source_id_record(self.uid, FileChecksums(md5, sha256, size).hid, '111')
        GlobalGalleryController.add_source_ids_to_file(self.uid, FileChecksums(md5, sha256, int(size)).hid, ['222'])
        with mock.patch('mpfs.core.global_gallery.logic.controller.GLOBAL_GALLERY_MAX_SOURCE_ID_LIMIT', 2), \
                change_experiment_context_with(uid=self.uid):
            GlobalGalleryController.add_source_ids_to_file(self.uid, FileChecksums(md5, sha256, int(size)).hid, ['333', '444'])
        assert 2 == len(list(SourceIdDAO().find({'uid': self.uid})))

    def test_skip_source_id_on_adding_if_resource_not_found_by_hashes(self):
        hid = FileChecksums('b5c64136bd7f685a5bfa1e9c78d7e565',
                            'ca4f3cbf0bdf65858cd0f12dbff6a611dfdf13037ebfa4a31199155016f64853', 16029).hid
        with change_experiment_context_with(uid=self.uid):
            GlobalGalleryController.add_source_ids_to_file(self.uid, hid, ['111'])

        assert not list(SourceIdDAO().find({'uid': self.uid}))


@pytest.mark.skipif(not INIT_USER_IN_POSTGRES, reason='pg only feature')
class GlobalGalleryGettingSourceId(LivePhotoMixin, GlobalGalleryTestCaseMixin, CommonJsonApiTestCase):

    @parameterized.expand([
        ([],),
        (['111'],),
        (['111', '222'],),
    ])
    def test_loading_source_ids_for_single_resource(self, source_ids):
        md5 = 'b5c64136bd7f685a5bfa1e9c78d7e565'
        sha256 = 'ca4f3cbf0bdf65858cd0f12dbff6a611dfdf13037ebfa4a31199155016f64853'
        size = 16029
        path = '/disk/123.txt'
        self.upload_file(self.uid, path, file_data={'md5': md5, 'sha256': sha256, 'size': size})

        for source_id in source_ids:
            self._insert_source_id_record(self.uid, FileChecksums(md5, sha256, size).hid, source_id)
        resource = factory.get_resource_by_path(self.uid, path)
        with change_experiment_context_with(uid=self.uid):
            GlobalGalleryController.load_and_set_source_ids_for_resources([resource])

        assert set(source_ids) == set(resource.meta['source_ids'])

    def test_loading_source_ids_for_several_resources(self):
        md5 = 'b5c64136bd7f685a5bfa1e9c78d7e565'
        sha256 = 'ca4f3cbf0bdf65858cd0f12dbff6a611dfdf13037ebfa4a31199155016f64853'
        size_1 = 16029
        path_1 = '/disk/123.jpg'
        source_ids_1 = ('111', '222')
        self.upload_file(self.uid, path_1, file_data={'md5': md5, 'sha256': sha256, 'size': size_1})
        for source_id in source_ids_1:
            self._insert_source_id_record(self.uid, FileChecksums(md5, sha256, size_1).hid, source_id)

        size_2 = 16027
        path_2 = '/disk/123_2.jpg'
        source_ids_2 = ('333', '444')
        self.upload_file(self.uid, path_2, file_data={'md5': md5, 'sha256': sha256, 'size': size_2})
        for source_id in source_ids_2:
            self._insert_source_id_record(self.uid, FileChecksums(md5, sha256, size_2).hid, source_id)

        size_3 = 16026
        path_3 = '/disk/123_3.jpg'
        source_ids_3 = tuple()
        self.upload_file(self.uid, path_3, file_data={'md5': md5, 'sha256': sha256, 'size': size_3})
        for source_id in source_ids_3:
            self._insert_source_id_record(self.uid, FileChecksums(md5, sha256, size_3).hid, source_id)

        parent_resource = factory.get_resource_by_path(self.uid, '/disk')
        parent_resource.load()
        several_resources = parent_resource.children_items['files']
        with change_experiment_context_with(uid=self.uid):
            GlobalGalleryController.load_and_set_source_ids_for_resources(several_resources)

        for resource in several_resources:
            if resource.path == path_1:
                assert set(resource.meta['source_ids']) == set(source_ids_1)
            if resource.path == path_2:
                assert set(resource.meta['source_ids']) == set(source_ids_2)
            if resource.path == path_3:
                assert set(resource.meta['source_ids']) == set(source_ids_3)

    def test_loading_source_ids_for_files_with_same_hid(self):
        md5 = 'b5c64136bd7f685a5bfa1e9c78d7e565'
        sha256 = 'ca4f3cbf0bdf65858cd0f12dbff6a611dfdf13037ebfa4a31199155016f64853'
        size = 16029
        path_1 = '/disk/123.jpg'
        path_2 = '/disk/123_2.jpg'
        source_ids = ('111', '222')
        self.upload_file(self.uid, path_1, file_data={'md5': md5, 'sha256': sha256, 'size': size})
        self.upload_file(self.uid, path_2, file_data={'md5': md5, 'sha256': sha256, 'size': size})
        for source_id in source_ids:
            self._insert_source_id_record(self.uid, FileChecksums(md5, sha256, size).hid, source_id)

        parent_resource = factory.get_resource_by_path(self.uid, '/disk')
        parent_resource.load()
        resources = parent_resource.children_items['files']
        with change_experiment_context_with(uid=self.uid):
            GlobalGalleryController.load_and_set_source_ids_for_resources(resources)

        assert 2 == len(resources)
        for child_resource in resources:
            assert set(child_resource.meta['source_ids']) == set(source_ids)

    def test_loading_source_ids_for_files_of_different_users(self):
        self.create_user(self.uid_1)
        md5 = 'b5c64136bd7f685a5bfa1e9c78d7e565'
        sha256 = 'ca4f3cbf0bdf65858cd0f12dbff6a611dfdf13037ebfa4a31199155016f64853'
        size = 16029
        path = '/disk/1.txt'
        source_ids_1 = ('111', '222')
        source_ids_2 = ('333', '444')
        self.upload_file(self.uid, path, file_data={'md5': md5, 'sha256': sha256, 'size': size})
        self.upload_file(self.uid_1, path, file_data={'md5': md5, 'sha256': sha256, 'size': size})
        for source_id in source_ids_1:
            self._insert_source_id_record(self.uid, FileChecksums(md5, sha256, size).hid, source_id)
        for source_id in source_ids_2:
            self._insert_source_id_record(self.uid_1, FileChecksums(md5, sha256, size).hid, source_id)

        resources = [factory.get_resource_by_path(self.uid, path), factory.get_resource_by_path(self.uid_1, path)]
        with change_experiment_context_with(uid=self.uid):
            GlobalGalleryController.load_and_set_source_ids_for_resources(resources)
        for resource in resources:
            if resource.uid == self.uid:
                assert set(resource.meta['source_ids']) == set(source_ids_1)
            if resource.uid == self.uid_1:
                assert set(resource.meta['source_ids']) == set(source_ids_2)

    @parameterized.expand([
        (True,),
        (False,),
    ])
    def test_reading_source_id_for_live_photo(self, is_live_photo_checking):
        photo_source_id = '222'
        path = '/disk/1.jpg'

        if is_live_photo_checking:
            self.store_live_photo_with_video(path, opts={'source_id': photo_source_id})
        else:
            self.upload_file(self.uid, path, opts={'source_id': photo_source_id})

        r = self.json_ok('info', {'uid': self.uid, 'path': path, 'meta': ''})
        md5 = r['meta']['md5']
        sha256 = r['meta']['sha256']
        size = r['meta']['size']

        with change_experiment_context_with(uid=self.uid):
            if is_live_photo_checking:
                assert GlobalGalleryController.does_source_id_exist_by_hid(self.uid, FileChecksums(md5, sha256, size).hid, is_live_photo=True)
                assert GlobalGalleryController.does_source_id_exist_by_hashes(self.uid, md5, sha256, size, is_live_photo=True)
                assert not GlobalGalleryController.does_source_id_exist_by_hid(self.uid, FileChecksums(md5, sha256, size).hid, is_live_photo=False)
                assert not GlobalGalleryController.does_source_id_exist_by_hashes(self.uid, md5, sha256, size, is_live_photo=False)
            else:
                assert GlobalGalleryController.does_source_id_exist_by_hid(self.uid, FileChecksums(md5, sha256, size).hid, is_live_photo=False)
                assert GlobalGalleryController.does_source_id_exist_by_hashes(self.uid, md5, sha256, size, is_live_photo=False)
                assert not GlobalGalleryController.does_source_id_exist_by_hid(self.uid, FileChecksums(md5, sha256, size).hid, is_live_photo=True)
                assert not GlobalGalleryController.does_source_id_exist_by_hashes(self.uid, md5, sha256, size, is_live_photo=True)

    def test_setting_correct_source_ids_to_live_photo_resources(self):
        live_photo_source_id = '111'
        photo_source_id = '222'

        resources = self._upload_sample_files_for_live_photo_case(live_photo_source_id, photo_source_id)
        with change_experiment_context_with(uid=self.uid):
            GlobalGalleryController.load_and_set_source_ids_for_resources(resources)
        assert [x.meta.get('source_ids') for x in resources] == [[live_photo_source_id], [photo_source_id], [], []]

    def test_setting_correct_source_ids_to_live_photo_diff_resource_docs(self):
        live_photo_source_id = '111'
        photo_source_id = '222'

        self._upload_sample_files_for_live_photo_case(live_photo_source_id, photo_source_id)
        r = self.json_ok('diff', {'uid': self.uid, 'meta': 'source_ids'})
        sorted_result = sorted([x for x in r['result'] if x['type'] == 'file'], key=lambda x: x['key'])
        assert [x['source_ids'] for x in sorted_result] == [[live_photo_source_id], [photo_source_id], [], []]


@pytest.mark.skipif(not INIT_USER_IN_POSTGRES, reason='pg only feature')
class GlobalGalleryAddingDeletionLogRecordsTestCase(GlobalGalleryTestCaseMixin, DiskTestCase):
    file_id = '2b7d167553a142838b8cc47eaa4f3b7b68be38c98e91495cb6cb9a43b95acac7'
    hid = '06354c2627629eb44c3251fd95b86067'

    def test_add_record_into_empty_log(self):
        with change_experiment_context_with(uid=self.uid):
            GlobalGalleryController.add_deletion_log_record(self.uid, self.file_id, self.hid)
        log = list(DeletionLogDAO().find({'uid': self.uid}))
        assert 1 == len(log)
        ctime = time.time() * 10**6
        assert abs(log[0]['deletion_log_revision'] - ctime) < 10**6

    def test_add_record_into_non_empty_log(self):
        with change_experiment_context_with(uid=self.uid):
            GlobalGalleryController.add_deletion_log_record(self.uid, self.file_id, self.hid)
            GlobalGalleryController.add_deletion_log_record(self.uid, self.file_id, self.hid)
        log = sorted(list(DeletionLogDAO().find({'uid': self.uid})),
                     key=lambda x: x['deletion_log_revision'])
        assert 2 == len(log)
        assert log[0]['deletion_log_revision'] < log[1]['deletion_log_revision']

    @parameterized.expand([
        ('/disk/1.jpg', True,),
        ('/disk/1.avi', True,),
        ('/disk/1.txt', False,),
    ])
    def test_highlevel_writing_to_log_works_for_media_files_only(self, file_path, should_be_written_to_log):
        self.upload_file(self.uid, file_path, opts={'source_id': '111'})
        disk_file = factory.get_resource_by_path(self.uid, file_path)
        records_before = len(list(DeletionLogDAO().find({'uid': self.uid})))
        with change_experiment_context_with(uid=self.uid):
            GlobalGalleryController.try_to_write_to_deletion_log_by_file_doc(self.uid, disk_file.dict())
        assert records_before + 1 if should_be_written_to_log else records_before == len(list(DeletionLogDAO().find({'uid': self.uid})))


@pytest.mark.skipif(not INIT_USER_IN_POSTGRES, reason='pg only feature')
class GlobalGalleryGettingDeletionLogRecordsTestCase(LivePhotoMixin, GlobalGalleryTestCaseMixin, DiskTestCase):
    file_id = '2b7d167553a142838b8cc47eaa4f3b7b68be38c98e91495cb6cb9a43b95acac7'
    hid = '06354c2627629eb44c3251fd95b86067'

    def test_write_to_deletion_log(self):
        self._insert_source_id_record(self.uid, self.hid, '111')
        with change_experiment_context_with(uid=self.uid):
            GlobalGalleryController.add_deletion_log_record(self.uid, self.file_id, self.hid)
        assert list(DeletionLogDAO().find({'uid': self.uid}))[0]['deletion_log_revision']

    @parameterized.expand([
        (0, 0),
        (2, 2),
        (3, 3),
        (4, 3),
    ])
    def test_reading_deletion_log(self, num_of_records, correct_num_of_records_fetched):
        self._insert_source_id_record(self.uid, self.hid, '111')
        with mock.patch('mpfs.core.global_gallery.logic.controller.GLOBAL_GALLERY_DELETION_LOG_CHUNK_SIZE', 3), \
                change_experiment_context_with(uid=self.uid):
            for _ in range(num_of_records):
                GlobalGalleryController.add_deletion_log_record(self.uid, self.file_id, self.hid)
            records = GlobalGalleryController.fetch_with_source_ids_sorted_by_revision(self.uid, 0)
        assert correct_num_of_records_fetched == len(records)

    def test_reading_deletion_log_with_offset(self):
        self._insert_source_id_record(self.uid, self.hid, '111')
        with mock.patch('mpfs.core.global_gallery.logic.controller.GLOBAL_GALLERY_DELETION_LOG_CHUNK_SIZE', 3), \
                change_experiment_context_with(uid=self.uid):
            GlobalGalleryController.add_deletion_log_record(self.uid, self.file_id, self.hid)
            last_deletion_log_revision = list(DeletionLogDAO().find({'uid': self.uid}))[0]['deletion_log_revision']
            for _ in range(4):
                GlobalGalleryController.add_deletion_log_record(self.uid, self.file_id, self.hid)
            records = GlobalGalleryController.fetch_with_source_ids_sorted_by_revision(self.uid, last_deletion_log_revision)
        assert all([x[0].get_mongo_representation()['deletion_log_revision'] > last_deletion_log_revision for x in records])

    @parameterized.expand([
        ('/disk/r1/r2/1.jpg', ['111'], True),
        ('/disk/r1/r2/2.jpg', ['222'], False),
    ])
    def test_read_live_photo_record(self, path_to_remove, correct_source_ids, correct_flag):
        live_photo_source_id = '111'
        simple_photo_source_id = '222'
        self._upload_sample_files_for_live_photo_case(live_photo_source_id, simple_photo_source_id)
        self.json_ok('trash_append', {'uid': self.uid, 'path': path_to_remove})
        with change_experiment_context_with(uid=self.uid):
            records = list(GlobalGalleryController.fetch_with_source_ids_sorted_by_revision(self.uid, 0))
        assert len(records) == 1
        assert [x.source_id for x in records[0][1]] == correct_source_ids
        assert records[0][0].is_live_photo is correct_flag

    @parameterized.expand([
        (['111'],),
        (['111', '222'],),
    ])
    def test_taking_source_ids_from_log(self, source_ids):
        path = '/disk/1.png'
        self.upload_file(self.uid, path)
        r = self.json_ok('info', {'uid': self.uid, 'path': path, 'meta': ''})
        md5 = r['meta']['md5']
        sha256 = r['meta']['sha256']
        size = r['meta']['size']

        for source_id in source_ids:
            self._insert_source_id_record(self.uid, FileChecksums(md5, sha256, size).hid, source_id)
        self.json_ok('trash_append', {'uid': self.uid, 'path': path})

        with change_experiment_context_with(uid=self.uid):
            records = list(GlobalGalleryController.fetch_with_source_ids_sorted_by_revision(self.uid, 0))
        assert {x.source_id for x in records[0][1]} == set(source_ids)

    def test_getting_deletion_record_of_files_with_same_source_ids(self):
        md5 = 'b5c64136bd7f685a5bfa1e9c78d7e565'
        sha256 = 'ca4f3cbf0bdf65858cd0f12dbff6a611dfdf13037ebfa4a31199155016f64853'
        size = 16029
        paths = ['/disk/%s.png' % i for i in range(2)]
        for path in paths:
            self.upload_file(self.uid, path, file_data={
                'md5': md5,
                'sha256': sha256,
                'size': size,
            })
        source_id = '111'
        self._insert_source_id_record(self.uid, FileChecksums(md5, sha256, size).hid, source_id)

        for path in paths:
            self.json_ok('trash_append', {'uid': self.uid, 'path': path})

        with change_experiment_context_with(uid=self.uid):
            records = list(GlobalGalleryController.fetch_with_source_ids_sorted_by_revision(self.uid, 0))
        assert [x.source_id for x in records[0][1]] == [x.source_id for x in records[0][1]] == [source_id]

    @parameterized.expand([
        (True,),
        (False,),
    ])
    def test_check_hid_presented_in_deletion_log(self, is_live_photo_checking):
        photo_source_id = '222'
        path = '/disk/1.jpg'

        if is_live_photo_checking:
            self.store_live_photo_with_video(path, opts={'source_id': photo_source_id})
        else:
            self.upload_file(self.uid, path, opts={'source_id': photo_source_id})

        r = self.json_ok('info', {'uid': self.uid, 'path': path, 'meta': ''})
        md5 = r['meta']['md5']
        sha256 = r['meta']['sha256']
        size = r['meta']['size']

        self.json_ok('trash_append', {'uid': self.uid, 'path': path, 'meta': ''})

        with change_experiment_context_with(uid=self.uid):
            if is_live_photo_checking:
                assert GlobalGalleryController.is_hid_presented_in_deletion_log(self.uid, FileChecksums(md5, sha256, size).hid, is_live_photo=True)
                assert not GlobalGalleryController.is_hid_presented_in_deletion_log(self.uid, FileChecksums(md5, sha256, size).hid, is_live_photo=False)
            else:
                assert GlobalGalleryController.is_hid_presented_in_deletion_log(self.uid, FileChecksums(md5, sha256, size).hid, is_live_photo=False)
                assert not GlobalGalleryController.is_hid_presented_in_deletion_log(self.uid, FileChecksums(md5, sha256, size).hid, is_live_photo=True)
