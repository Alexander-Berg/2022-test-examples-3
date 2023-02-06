# -*- coding: utf-8 -*-
import hashlib
import time
from collections import defaultdict

import mock
import pytest
from nose_parameterized import parameterized

from mpfs.common.static import codes
from mpfs.common.util.experiments.logic import change_experiment_context_with
from mpfs.core.filesystem.hardlinks.common import FileChecksums
from mpfs.core.global_gallery.dao.deletion_log import DeletionLogDAO
from mpfs.core.global_gallery.dao.source_id import SourceIdDAO, SourceIdDAOItem
from mpfs.core.global_gallery.logic.controller import GlobalGalleryController
from test.conftest import INIT_USER_IN_POSTGRES, capture_queue_errors
from test.helpers.stubs.smart_services import DiskSearchSmartMockHelper
from test.parallelly.global_gallery.base import GlobalGalleryTestCaseMixin
from test.parallelly.json_api.base import CommonJsonApiTestCase
from test.parallelly.live_photo_suit import LivePhotoMixin


@pytest.mark.skipif(not INIT_USER_IN_POSTGRES, reason='pg only feature')
class GlobalGalleryStoreTestCase(GlobalGalleryTestCaseMixin, CommonJsonApiTestCase):

    def test_add_source_id_file_exists_by_hashes_in_disk_on_regular_store_hardlinked_on_store(self):
        path = '/disk/1.txt'
        self.upload_file(self.uid, path)
        source_id = '111'
        r = self.json_ok('info', {'uid': self.uid, 'path': path, 'meta': ''})
        md5, sha256, size = r['meta']['md5'], r['meta']['sha256'], r['meta']['size']
        with mock.patch('mpfs.core.filesystem.hardlinks.common.AbstractLink.is_file_in_storage', return_value=True):
            res = self.json_ok('store', {'uid': self.uid,
                                         'path': '/disk/1.jpg',
                                         'sha256': sha256,
                                         'md5': md5,
                                         'size': size,
                                         'source_id': source_id})

        assert 'hardlinked' == res['status']
        assert source_id == SourceIdDAO().find_one({'uid': self.uid})['source_id']

    def test_add_source_id_for_hardlinked_file_but_from_other_user(self):
        self.create_user(self.uid_1)
        path = '/disk/1.txt'
        self.upload_file(self.uid_1, path)
        r = self.json_ok('info', {'uid': self.uid_1, 'path': path, 'meta': ''})
        md5, sha256, size = r['meta']['md5'], r['meta']['sha256'], r['meta']['size']

        source_id = '111'

        with mock.patch('mpfs.core.filesystem.hardlinks.common.AbstractLink.is_file_in_storage', return_value=True):
            self.upload_file(self.uid, '/photostream/2.jpg', file_data={'md5': md5, 'sha256': sha256, 'size': size},
                             hardlink=True, opts={'source_id': source_id})
        assert source_id == SourceIdDAO().find_one({'uid': self.uid})['source_id']

    @parameterized.expand([
        (False, '/disk/Фотокамера'),
        (True, '/photounlim'),
    ])
    def test_add_source_id_file_exists_by_hashes_in_disk_autoupload_store(self, is_photounlim, path_prefix):
        headers = None
        if is_photounlim:
            self.json_ok('enable_unlimited_autouploading', {'uid': self.uid})
            headers = {'Yandex-Cloud-Request-ID': 'ios-test'}
        path = '/disk/1.txt'
        self.upload_file(self.uid, path)
        source_id = '111'
        r = self.json_ok('info', {'uid': self.uid, 'path': path, 'meta': ''})
        md5, sha256, size = r['meta']['md5'], r['meta']['sha256'], r['meta']['size']
        res = self.json_error('store', {'uid': self.uid,
                                        'path': '/photostream/1.jpg',
                                        'sha256': sha256,
                                        'md5': md5,
                                        'size': size,
                                        'source_id': source_id,
                                        'force_deletion_log_deduplication': 1,},
                              headers=headers)

        assert codes.FILE_EXISTS == res['code']
        assert source_id == SourceIdDAO().find_one({'uid': self.uid})['source_id']

    def test_add_source_id_file_exists_by_hashes_in_upload_log_regular_store(self):
        md5 = 'b5c64136bd7f685a5bfa1e9c78d7e565'
        sha256 = 'ca4f3cbf0bdf65858cd0f12dbff6a611dfdf13037ebfa4a31199155016f64853'
        size = 16029

        self._insert_source_id_record(self.uid, FileChecksums(md5, sha256, size).hid, '111')
        new_source_id = '222'
        self.upload_file(self.uid, '/disk/2.jpg', file_data={'md5': md5, 'sha256': sha256, 'size': size},
                         opts={'source_id': new_source_id})

        assert 2 == len(list(SourceIdDAO().find({'uid': self.uid})))
        assert new_source_id in {x['source_id'] for x in SourceIdDAO().find({'uid': self.uid})}

    @parameterized.expand([
        (False, '/disk/Фотокамера'),
        (True, '/photounlim'),
    ])
    def test_add_source_id_file_exists_by_hashes_in_deletion_log_autoupload_store(self, is_photounlim, path_prefix):
        headers = None
        if is_photounlim:
            self.json_ok('enable_unlimited_autouploading', {'uid': self.uid})
            headers = {'Yandex-Cloud-Request-ID': 'ios-test'}
        md5 = 'b5c64136bd7f685a5bfa1e9c78d7e565'
        sha256 = 'ca4f3cbf0bdf65858cd0f12dbff6a611dfdf13037ebfa4a31199155016f64853'
        size = 16029
        new_source_id = '222'

        self.upload_file(self.uid, '/photostream/1.jpg', file_data={'md5': md5, 'sha256': sha256, 'size': size},
                         opts={'source_id': '111'}, headers=headers)
        self.json_ok('trash_append', {'uid': self.uid, 'path': path_prefix + '/1.jpg'})
        res = self.json_error('store', {'uid': self.uid,
                                        'path': '/photostream/2.jpg',
                                        'sha256': sha256,
                                        'md5': md5,
                                        'size': size,
                                        'source_id': new_source_id,
                                        'force_deletion_log_deduplication': 1},
                              headers=headers)

        assert codes.FILE_PRESENTED_IN_DELETION_LOG == res['code']
        assert 2 == len(list(SourceIdDAO().find({'uid': self.uid})))
        assert new_source_id in {x['source_id'] for x in SourceIdDAO().find({'uid': self.uid})}

    @parameterized.expand([
        (False, '/disk/Фотокамера'),
        (True, '/photounlim'),
    ])
    def test_add_source_id_file_exists_by_hashes_in_deletion_log_autoupload_store_successful_if_no_parameter(self, is_photounlim, path_prefix):
        headers = None
        if is_photounlim:
            self.json_ok('enable_unlimited_autouploading', {'uid': self.uid})
            headers = {'Yandex-Cloud-Request-ID': 'ios-test'}
        md5 = 'b5c64136bd7f685a5bfa1e9c78d7e565'
        sha256 = 'ca4f3cbf0bdf65858cd0f12dbff6a611dfdf13037ebfa4a31199155016f64853'
        size = 16029
        new_source_id = '222'

        self.upload_file(self.uid, '/photostream/1.jpg', file_data={'md5': md5, 'sha256': sha256, 'size': size},
                         opts={'source_id': '111'}, headers=headers)
        self.json_ok('trash_append', {'uid': self.uid, 'path': path_prefix + '/1.jpg'})
        self.upload_file(self.uid, '/photostream/1.jpg', file_data={'md5': md5, 'sha256': sha256, 'size': size},
                         opts={'source_id': new_source_id}, headers=headers)

        assert 2 == len(list(SourceIdDAO().find({'uid': self.uid})))
        assert new_source_id in {x['source_id'] for x in SourceIdDAO().find({'uid': self.uid})}

    def test_add_source_id_file_doesnt_exist_anywhere(self):
        md5 = 'b5c64136bd7f685a5bfa1e9c78d7e565'
        sha256 = 'ca4f3cbf0bdf65858cd0f12dbff6a611dfdf13037ebfa4a31199155016f64853'
        size = 16029
        source_id = '111'

        self.upload_file(self.uid, '/disk/2.jpg', file_data={'md5': md5, 'sha256': sha256, 'size': size},
                         opts={'source_id': source_id})

        assert source_id == SourceIdDAO().find_one({'uid': self.uid})['source_id']

    @parameterized.expand([
        (False, ),
        (True, ),
    ])
    def test_add_source_id_file_doesnt_exist_anywhere_autoupload(self, is_photounlim):
        headers = None
        if is_photounlim:
            self.json_ok('enable_unlimited_autouploading', {'uid': self.uid})
            headers = {'Yandex-Cloud-Request-ID': 'ios-test'}
        md5 = 'b5c64136bd7f685a5bfa1e9c78d7e565'
        sha256 = 'ca4f3cbf0bdf65858cd0f12dbff6a611dfdf13037ebfa4a31199155016f64853'
        size = 16029
        source_id = '111'

        self.upload_file(self.uid, '/photostream/2.jpg', file_data={'md5': md5, 'sha256': sha256, 'size': size},
                         opts={'source_id': source_id}, headers=headers)

        assert source_id == SourceIdDAO().find_one({'uid': self.uid})['source_id']

    def test_overwriting_file_with_source_id_adds_source_id_correctly(self):
        source_id = '111'
        self.upload_file(self.uid, '/disk/2.jpg', opts={'source_id': source_id})

        source_id = '222'
        self.upload_file(self.uid, '/disk/2.jpg', opts={'source_id': source_id})

        assert 2 == len(list(SourceIdDAO().find({'uid': self.uid})))

    def test_dont_add_source_id_if_experiment_is_off(self):
        md5 = 'b5c64136bd7f685a5bfa1e9c78d7e565'
        sha256 = 'ca4f3cbf0bdf65858cd0f12dbff6a611dfdf13037ebfa4a31199155016f64853'
        size = 16029
        source_id = '111'

        with mock.patch('mpfs.common.util.experiments.logic.ExperimentManager.is_feature_active', return_value=False):
            self.upload_file(self.uid, '/disk/2.jpg', file_data={'md5': md5, 'sha256': sha256, 'size': size},
                             opts={'source_id': source_id})

        assert SourceIdDAO().find_one({'uid': self.uid}) is None


@pytest.mark.skipif(not INIT_USER_IN_POSTGRES, reason='pg only feature')
class GlobalGalleryFileRemovingTestCase(GlobalGalleryTestCaseMixin, CommonJsonApiTestCase):

    @parameterized.expand([
        ('trash_append', '/disk/root/1.jpg', True),
        ('trash_append', '/disk/root', True),
        ('async_trash_append', '/disk/root/1.jpg', True),
        ('async_trash_append', '/disk/root', True),
        ('rm', '/disk/root/1.jpg', True),
        ('rm', '/disk/root', True),
        ('async_rm', '/disk/root/1.jpg', True),
        ('async_rm', '/disk/root', True),
        ('trash_append', '/disk/root/1.jpg', False),
        ('trash_append', '/disk/root', False),
        ('async_trash_append', '/disk/root/1.jpg', False),
        ('async_trash_append', '/disk/root', False),
        ('rm', '/disk/root/1.jpg', False),
        ('rm', '/disk/root', False),
        ('async_rm', '/disk/root/1.jpg', False),
        ('async_rm', '/disk/root', False),
    ])
    def test_write_to_deletion_log_on_deletion_for_files_with_source_id(self, endpoint, rm_path, have_source_id):
        assert 0 == len(list(DeletionLogDAO().find({'uid': self.uid})))
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/root'})
        opts = {}
        if have_source_id:
            opts = {'source_id': '111'}
        self.upload_file(self.uid, '/disk/root/1.jpg', opts=opts)
        with mock.patch('mpfs.core.address.Address.add_trash_suffix'):
            self.json_ok(endpoint, {'uid': self.uid, 'path': rm_path})
        assert len(list(DeletionLogDAO().find({'uid': self.uid}))) == 1

    @parameterized.expand([
        ('/disk/root/1.jpg', True,),
        ('/disk/root/1.jpg', False,),
    ])
    def test_write_to_deletion_log_on_overwriting_with_store_for_files_with_source_id(self, path_to_overwrite, have_source_id):
        assert 0 == len(list(DeletionLogDAO().find({'uid': self.uid})))
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/root'})
        opts = {}
        if have_source_id:
            opts = {'source_id': '111'}
        self.upload_file(self.uid, '/disk/root/1.jpg', opts=opts)
        self.upload_file(self.uid, path_to_overwrite, force=1)
        assert 1 == len(list(DeletionLogDAO().find({'uid': self.uid})))

    @parameterized.expand([
        ('/disk/root/1.jpg', True,),
        ('/disk/root', True,),
        ('/disk/root/1.jpg', False,),
        ('/disk/root', False,),
    ])
    def test_write_to_deletion_log_on_overwriting_with_store_move_for_files_with_source_id(self, path_to_overwrite, have_source_id):
        assert 0 == len(list(DeletionLogDAO().find({'uid': self.uid})))
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/root'})
        opts = {}
        if have_source_id:
            opts = {'source_id': '111'}
        self.upload_file(self.uid, '/disk/root/1.jpg', opts=opts)

        path_to_move = '/disk/to_move'
        self.upload_file(self.uid, path_to_move)

        self.json_ok('move', {'uid': self.uid, 'src': path_to_move, 'dst': path_to_overwrite, 'force': 1})
        assert len(list(DeletionLogDAO().find({'uid': self.uid}))) == 1

    @parameterized.expand([
        ('trash_drop_all', True),
        ('async_trash_drop_all', True),
        ('trash_drop_all', False),
        ('async_trash_drop_all', False),
    ])
    def test_dropping_trash_doesnt_write_to_deletion_log(self, endpoint, have_source_id):
        path = '/disk/root/1.jpg'
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/root'})
        opts = {}
        if have_source_id:
            opts = {'source_id': '111'}
        self.upload_file(self.uid, '/disk/root/1.jpg', opts=opts)
        with mock.patch('mpfs.core.address.Address.add_trash_suffix'):
            self.json_ok('trash_append', {'uid': self.uid, 'path': path})
        records_before = len(list(DeletionLogDAO().find({'uid': self.uid})))
        self.json_ok(endpoint, {'uid': self.uid})
        assert records_before == len(list(DeletionLogDAO().find({'uid': self.uid})))

    @parameterized.expand([
        ('/disk/root/1.jpg', '/trash/1.jpg', True, ),
        ('/disk/root/1.jpg', '/trash/1.jpg', False, ),
        ('/disk/root', '/trash/root', True,),
        ('/disk/root', '/trash/root', False,),
    ])
    def test_dropping_element_trash_doesnt_write_to_deletion_log(self, original_path, trash_path, have_source_id):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/root'})
        opts = {}
        if have_source_id:
            opts = {'source_id': '111'}
        self.upload_file(self.uid, '/disk/root/1.jpg', opts=opts)
        with mock.patch('mpfs.core.address.Address.add_trash_suffix'):
            self.json_ok('trash_append', {'uid': self.uid, 'path': original_path})
        records_before = len(list(DeletionLogDAO().find({'uid': self.uid})))
        self.json_ok('trash_drop', {'uid': self.uid, 'path': trash_path})
        assert records_before == len(list(DeletionLogDAO().find({'uid': self.uid})))

    @parameterized.expand([
        ('trash_append',),
        ('rm',),
    ])
    def removing_folder_with_several_files_add_record_for_every_file(self, endpoint):
        assert 0 == len(list(DeletionLogDAO().find({'uid': self.uid})))
        path = '/disk/root'
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/root'})

        self.upload_file(self.uid, '/disk/root/1.jpg', opts={'source_id': '111'})
        self.upload_file(self.uid, '/disk/root/2.jpg', opts={'source_id': '222'})

        self.json_ok(endpoint, {'uid': self.uid, 'path': path})
        assert 2 == len(list(DeletionLogDAO().find({'uid': self.uid})))

    def test_regular_move_doesnt_add_write_to_deletion_log(self):
        assert 0 == len(list(DeletionLogDAO().find({'uid': self.uid})))
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/root'})

        self.upload_file(self.uid, '/disk/root/1.jpg', opts={'source_id': '111'})

        self.json_ok('move', {'uid': self.uid, 'src': '/disk/root/1.jpg', 'dst': '/disk/root/2.jpg'})
        assert 0 == len(list(DeletionLogDAO().find({'uid': self.uid})))


@pytest.mark.skipif(not INIT_USER_IN_POSTGRES, reason='pg only feature')
class GlobalGalleryGettingSourceIdInMetaTestCase(GlobalGalleryTestCaseMixin, CommonJsonApiTestCase):

    @parameterized.expand([
        ([],),
        (['111'],),
        (['111', '222'],),
    ])
    def test_get_source_ids_by_info(self, source_ids):
        md5 = 'b5c64136bd7f685a5bfa1e9c78d7e565'
        sha256 = 'ca4f3cbf0bdf65858cd0f12dbff6a611dfdf13037ebfa4a31199155016f64853'
        size = 16029
        path = '/disk/2.jpg'
        self.upload_file(self.uid, path, file_data={'md5': md5, 'sha256': sha256, 'size': size})

        for source_id in source_ids:
            self._insert_source_id_record(self.uid, FileChecksums(md5, sha256, size).hid, source_id)

        r = self.json_ok('info', {'uid': self.uid, 'path': path, 'meta': 'source_ids'})
        assert set(source_ids) == set(r['meta']['source_ids'])

    def test_dont_return_source_id_for_empty_meta_in_info(self):
        md5 = 'b5c64136bd7f685a5bfa1e9c78d7e565'
        sha256 = 'ca4f3cbf0bdf65858cd0f12dbff6a611dfdf13037ebfa4a31199155016f64853'
        size = 16029
        source_id = '111'
        path = '/disk/2.jpg'
        self.upload_file(self.uid, path, opts={'source_id': source_id}, file_data={'md5': md5, 'sha256': sha256, 'size': size})
        r = self.json_ok('info', {'uid': self.uid, 'path': path, 'meta': ''})
        assert 'source_ids' not in r['meta']

    @parameterized.expand([
        ([],),
        (['111'],),
        (['111', '222'],),
    ])
    def test_get_source_ids_by_list(self, source_ids):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/root'})
        md5 = 'b5c64136bd7f685a5bfa1e9c78d7e565'
        sha256 = 'ca4f3cbf0bdf65858cd0f12dbff6a611dfdf13037ebfa4a31199155016f64853'
        size = 16029
        path = '/disk/root/1.jpg'
        self.upload_file(self.uid, path, file_data={'md5': md5, 'sha256': sha256, 'size': size})

        for source_id in source_ids:
            self._insert_source_id_record(self.uid, FileChecksums(md5, sha256, size).hid, source_id)

        response = self.json_ok('list', {'uid': self.uid, 'path': '/disk/root', 'meta': 'source_ids'})
        for r in response:
            if r['path'] == '/disk/root':
                continue
            assert set(r['meta']['source_ids']) == set(source_ids)

    def test_dont_return_source_id_for_empty_meta_in_list(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/root'})
        md5 = 'b5c64136bd7f685a5bfa1e9c78d7e565'
        sha256 = 'ca4f3cbf0bdf65858cd0f12dbff6a611dfdf13037ebfa4a31199155016f64853'
        size = 16029
        source_id = '111'
        path = '/disk/root/2.jpg'
        self.upload_file(self.uid, path, opts={'source_id': source_id},
                         file_data={'md5': md5, 'sha256': sha256, 'size': size})
        r = self.json_ok('info', {'uid': self.uid, 'path': path, 'meta': ''})
        assert 'source_ids' not in r['meta']

    def test_get_source_ids_by_list_on_file(self):
        md5 = 'b5c64136bd7f685a5bfa1e9c78d7e565'
        sha256 = 'ca4f3cbf0bdf65858cd0f12dbff6a611dfdf13037ebfa4a31199155016f64853'
        size = 16029
        path = '/disk/1.jpg'
        source_ids = ['111']
        self.upload_file(self.uid, path, file_data={'md5': md5, 'sha256': sha256, 'size': size})

        for source_id in source_ids:
            self._insert_source_id_record(self.uid, FileChecksums(md5, sha256, size).hid, source_id)

        response = self.json_ok('list', {'uid': self.uid, 'path': path, 'meta': 'source_ids'})
        assert set(response['meta']['source_ids']) == set(source_ids)

    @parameterized.expand([
        ([],),
        (['111'],),
        (['111', '222'],),
    ])
    def test_get_source_ids_by_info_by_file_id(self, source_ids):
        md5 = 'b5c64136bd7f685a5bfa1e9c78d7e565'
        sha256 = 'ca4f3cbf0bdf65858cd0f12dbff6a611dfdf13037ebfa4a31199155016f64853'
        size = 16029
        path = '/disk/2.jpg'
        self.upload_file(self.uid, path, file_data={'md5': md5, 'sha256': sha256, 'size': size})
        file_id = self.json_ok('info', {'uid': self.uid, 'path': path, 'meta': 'file_id'})['meta']['file_id']

        for source_id in source_ids:
            self._insert_source_id_record(self.uid, FileChecksums(md5, sha256, size).hid, source_id)

        r = self.json_ok('info_by_file_id', {'uid': self.uid, 'file_id': file_id, 'meta': 'source_ids'})
        assert set(source_ids) == set(r['meta']['source_ids'])

    def test_dont_return_source_id_for_empty_meta_in_info_by_file_id(self):
        md5 = 'b5c64136bd7f685a5bfa1e9c78d7e565'
        sha256 = 'ca4f3cbf0bdf65858cd0f12dbff6a611dfdf13037ebfa4a31199155016f64853'
        size = 16029
        source_id = '111'
        path = '/disk/2.jpg'
        self.upload_file(self.uid, path, opts={'source_id': source_id},
                         file_data={'md5': md5, 'sha256': sha256, 'size': size})
        file_id = self.json_ok('info', {'uid': self.uid, 'path': path, 'meta': 'file_id'})['meta']['file_id']
        r = self.json_ok('info_by_file_id', {'uid': self.uid, 'file_id': file_id, 'meta': ''})
        assert 'source_ids' not in r['meta']

    @parameterized.expand([
        ([[]],),
        ([['111'], ['222']],),
        ([['111'], ['222', '333'], ['444', '555', '666']],),
    ])
    def test_get_source_ids_by_bulk_info(self, source_ids_bunks):
        map_path_to_source_ids = defaultdict(list)
        for e, source_ids in enumerate(source_ids_bunks):
            path = '/disk/%s.jpg' % e
            self.upload_file(self.uid, path)
            r = self.json_ok('info', {'uid': self.uid, 'path': path, 'meta': ''})
            for source_id in source_ids:
                self._insert_source_id_record(self.uid, FileChecksums(r['meta']['md5'], r['meta']['sha256'], r['meta']['size']).hid, source_id)
            map_path_to_source_ids[path] = source_ids

        resp = self.json_ok('bulk_info', {'uid': self.uid, 'meta': 'source_ids'}, json=map_path_to_source_ids.keys())
        for res_doc in resp:
            assert set(map_path_to_source_ids[res_doc['path']]) == set(res_doc['meta']['source_ids'])

    def test_dont_return_source_id_for_empty_meta_in_bulk_info(self):
        path = '/disk/1.jpg'
        self.upload_file(self.uid, path)
        r = self.json_ok('info', {'uid': self.uid, 'path': path, 'meta': ''})
        self._insert_source_id_record(self.uid, FileChecksums(r['meta']['md5'], r['meta']['sha256'], r['meta']['size']).hid, '111')
        resp = self.json_ok('bulk_info', {'uid': self.uid, 'meta': ''}, json=[path])
        assert 'source_ids' not in resp[0]['meta']

    @parameterized.expand([
        ([[]],),
        ([['111'], ['222']],),
        ([['111'], ['222', '333'], ['444', '555', '666']],),
    ])
    def test_get_source_ids_by_bulk_info_by_resource_ids(self, source_ids_bunks):
        map_resource_id_to_source_ids = defaultdict(list)
        for e, source_ids in enumerate(source_ids_bunks):
            path = '/disk/%s.jpg' % e
            self.upload_file(self.uid, path)
            r = self.json_ok('info', {'uid': self.uid, 'path': path, 'meta': ''})
            for source_id in source_ids:
                self._insert_source_id_record(self.uid, FileChecksums(r['meta']['md5'], r['meta']['sha256'],
                                                                      r['meta']['size']).hid, source_id)
            map_resource_id_to_source_ids[r['meta']['resource_id']] = source_ids

        resp = self.json_ok('bulk_info_by_resource_ids', {'uid': self.uid, 'meta': 'source_ids,resource_id'},
                            json=map_resource_id_to_source_ids.keys())
        for res_doc in resp:
            assert set(map_resource_id_to_source_ids[res_doc['meta']['resource_id']]) == set(res_doc['meta']['source_ids'])

    def test_dont_return_source_id_for_empty_meta_in_bulk_info_by_resource_ids(self):
        path = '/disk/1.jpg'
        self.upload_file(self.uid, path)
        r = self.json_ok('info', {'uid': self.uid, 'path': path, 'meta': ''})
        self._insert_source_id_record(self.uid,
                                      FileChecksums(r['meta']['md5'], r['meta']['sha256'], r['meta']['size']).hid,
                                      '111')
        resp = self.json_ok('bulk_info_by_resource_ids', {'uid': self.uid, 'meta': ''}, json=[r['meta']['resource_id']])
        assert 'source_ids' not in resp[0]['meta']

    def test_get_source_ids_by_public_list_on_file(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/dir'})
        self.upload_file(self.uid, '/disk/dir/test.txt')
        self.json_ok('set_public', {'uid': self.uid, 'path': '/disk/dir/test.txt'})
        response = self.json_ok('info', {'uid': self.uid, 'path': '/disk/dir/test.txt', 'meta': ''})
        public_hash = response['meta']['public_hash']
        source_id = '111'
        self._insert_source_id_record(self.uid,
                                      FileChecksums(response['meta']['md5'], response['meta']['sha256'], response['meta']['size']).hid,
                                      source_id)

        resp = self.json_ok('public_list', {
            'private_hash': public_hash,
            'meta': 'source_ids'
        })
        assert {source_id} == set(resp['meta']['source_ids'])

    def test_get_source_ids_by_public_list_on_folder(self):
        dir_path = '/disk/dir'
        self.json_ok('mkdir', {'uid': self.uid, 'path': dir_path})
        map_name_to_source_ids = {
            'test_1.txt': ['111', '222'],
            'test_2.txt': ['333'],
            'test_3.txt': [''],
        }
        self.json_ok('set_public', {'uid': self.uid, 'path': '/disk/dir'})
        response = self.json_ok('info', {'uid': self.uid, 'path': '/disk/dir', 'meta': ''})
        public_hash = response['meta']['public_hash']
        for name, source_ids in map_name_to_source_ids.iteritems():
            self.upload_file(self.uid, dir_path + '/' + name)
            response = self.json_ok('info', {'uid': self.uid, 'path': dir_path + '/' + name, 'meta': ''})
            for source_id in source_ids:
                self._insert_source_id_record(self.uid,
                                              FileChecksums(response['meta']['md5'], response['meta']['sha256'],
                                                            response['meta']['size']).hid,
                                              source_id)

        resp = self.json_ok('public_list', {
            'private_hash': public_hash,
            'meta': 'source_ids'
        })

        for res_doc in resp:
            if res_doc['type'] == 'dir':
                continue
            assert set(map_name_to_source_ids[res_doc['name']]) == set(res_doc['meta']['source_ids'])

    def test_dont_return_source_id_for_empty_meta_in_public_list(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/dir'})
        self.upload_file(self.uid, '/disk/dir/test.txt')
        self.json_ok('set_public', {'uid': self.uid, 'path': '/disk/dir/test.txt'})
        response = self.json_ok('info', {'uid': self.uid, 'path': '/disk/dir/test.txt', 'meta': ''})
        public_hash = response['meta']['public_hash']
        source_id = '111'
        self._insert_source_id_record(self.uid,
                                      FileChecksums(response['meta']['md5'], response['meta']['sha256'],
                                                    response['meta']['size']).hid,
                                      source_id)
        resp = self.json_ok('public_list', {
            'private_hash': public_hash,
            'meta': ''
        })
        assert 'source_ids' not in resp['meta']

    def test_get_source_ids_by_lenta_block_list(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/root'})
        map_path_to_source_ids = {
            '/disk/root/test_1.txt': ['111', '222'],
            '/disk/root/test_2.txt': ['333'],
            '/disk/root/test_3.txt': [''],
        }
        for path, source_ids in map_path_to_source_ids.iteritems():
            self.upload_file(self.uid, path)
            response = self.json_ok('info', {'uid': self.uid, 'path': path, 'meta': ''})
            for source_id in source_ids:
                self._insert_source_id_record(self.uid,
                                              FileChecksums(response['meta']['md5'], response['meta']['sha256'],
                                                            response['meta']['size']).hid,
                                              source_id)

        resource_id = self.json_ok('info', {'uid': self.uid, 'meta': 'resource_id', 'path': '/disk/root'})['meta']['resource_id']
        resp = self.json_ok('lenta_block_list', {'uid': self.uid, 'resource_id': resource_id, 'mtime_gte': 0, 'amount': 4, 'meta': 'source_ids'})
        for res_doc in resp:
            if res_doc['type'] == 'dir':
                continue
            assert set(map_path_to_source_ids[res_doc['path']]) == set(res_doc['meta']['source_ids'])

    def test_dont_return_source_id_for_empty_meta_in_lenta_block_list(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/dir'})
        self.upload_file(self.uid, '/disk/dir/test.txt')
        response = self.json_ok('info', {'uid': self.uid, 'path': '/disk/dir/test.txt', 'meta': ''})
        source_id = '111'
        self._insert_source_id_record(self.uid,
                                      FileChecksums(response['meta']['md5'], response['meta']['sha256'],
                                                    response['meta']['size']).hid,
                                      source_id)
        response = self.json_ok('info', {'uid': self.uid, 'path': '/disk/dir', 'meta': ''})
        resource_id = response['meta']['resource_id']
        resp = self.json_ok('lenta_block_list', {'uid': self.uid, 'resource_id': resource_id, 'mtime_gte': 0, 'amount': 4, 'meta': ''})
        assert 'source_ids' not in resp[0]['meta']

    def test_get_source_ids_by_diff(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/root'})
        map_path_to_source_ids = {
            '/disk/root/test_1.txt': ['111', '222'],
            '/disk/root/test_2.txt': ['333'],
            '/disk/root/test_3.txt': [''],
        }
        for path, source_ids in map_path_to_source_ids.iteritems():
            self.upload_file(self.uid, path)
            response = self.json_ok('info', {'uid': self.uid, 'path': path, 'meta': ''})
            for source_id in source_ids:
                self._insert_source_id_record(self.uid,
                                              FileChecksums(response['meta']['md5'], response['meta']['sha256'],
                                                            response['meta']['size']).hid,
                                              source_id)

        resp = self.json_ok('diff', {'uid': self.uid, 'path': '/disk', 'meta': 'source_ids'})
        for res_doc in resp['result']:
            if res_doc['type'] == 'dir':
                continue
            assert set(map_path_to_source_ids[res_doc['key']]) == set(res_doc['source_ids'])

    def test_dont_return_source_id_for_empty_meta_in_diff(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/dir'})
        self.upload_file(self.uid, '/disk/dir/test.txt')
        response = self.json_ok('info', {'uid': self.uid, 'path': '/disk/dir/test.txt', 'meta': ''})
        source_id = '111'
        self._insert_source_id_record(self.uid,
                                      FileChecksums(response['meta']['md5'], response['meta']['sha256'],
                                                    response['meta']['size']).hid,
                                      source_id)
        resp = self.json_ok('diff', {'uid': self.uid, 'path': '/disk', 'meta': ''})
        assert all(['source_ids' not in x for x in resp['result']])

    def test_get_source_ids_by_new_search(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/root'})
        map_path_to_source_ids = {
            '/disk/root/test_1.txt': ['111', '222'],
            '/disk/root/test_2.txt': ['333'],
            '/disk/root/test_3.txt': [''],
        }
        with DiskSearchSmartMockHelper.mock():
            for path, source_ids in map_path_to_source_ids.iteritems():
                self.upload_file(self.uid, path)
                DiskSearchSmartMockHelper.add_to_index(self.uid, path)
                response = self.json_ok('info', {'uid': self.uid, 'path': path, 'meta': ''})
                for source_id in source_ids:
                    self._insert_source_id_record(self.uid,
                                                  FileChecksums(response['meta']['md5'], response['meta']['sha256'],
                                                                response['meta']['size']).hid,
                                                  source_id)

            resp = self.json_ok('new_search', {'uid': self.uid, 'path': '/disk/root', 'query': 'test', 'meta': 'source_ids'})
            for res_doc in resp['results']:
                if res_doc['type'] == 'dir':
                    continue
                assert set(map_path_to_source_ids[res_doc['path']]) == set(res_doc['meta']['source_ids'])

    def test_dont_return_source_id_for_empty_meta_in_new_search(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/dir'})
        path = '/disk/dir/test.txt'
        with DiskSearchSmartMockHelper.mock():
            self.upload_file(self.uid, path)
            DiskSearchSmartMockHelper.add_to_index(self.uid, path)
            response = self.json_ok('info', {'uid': self.uid, 'path': '/disk/dir/test.txt', 'meta': ''})
            source_id = '111'
            self._insert_source_id_record(self.uid,
                                          FileChecksums(response['meta']['md5'], response['meta']['sha256'],
                                                        response['meta']['size']).hid,
                                          source_id)
            resp = self.json_ok('new_search', {'uid': self.uid, 'path': '/disk/dir', 'query': 'test', 'meta': ''})
            assert all(['source_ids' not in x['meta'] for x in resp['results']])

    def test_get_source_ids_by_albums_list(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/root'})
        map_path_to_source_ids = {
            '/disk/root/test_1.txt': ['111', '222'],
            '/disk/root/test_2.txt': ['333'],
            '/disk/root/test_3.txt': [''],
        }

        for path, source_ids in map_path_to_source_ids.iteritems():
            self.upload_file(self.uid, path)
            response = self.json_ok('info', {'uid': self.uid, 'path': path, 'meta': ''})
            for source_id in source_ids:
                self._insert_source_id_record(self.uid,
                                              FileChecksums(response['meta']['md5'], response['meta']['sha256'],
                                                            response['meta']['size']).hid,
                                              source_id)
            album_dict = {
                'title': 'MyAlbum',
                'items': [{'type': 'resource', 'path': path}],
            }
            self.json_ok('albums_create_with_items', opts={'uid': self.uid}, json=album_dict)

        resp = self.json_ok('albums_list', opts={'uid': self.uid, 'meta': 'source_ids'})
        for res_doc in resp:
            assert set(map_path_to_source_ids[res_doc['cover']['object']['path']]) == set(res_doc['cover']['object']['meta']['source_ids'])

    def test_dont_return_source_id_for_empty_meta_in_albums_list(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/dir'})
        path = '/disk/dir/test.txt'
        self.upload_file(self.uid, path)
        album_dict = {
            'title': 'MyAlbum',
            'items': [{'type': 'resource', 'path': path}],
        }
        self.json_ok('albums_create_with_items', opts={'uid': self.uid}, json=album_dict)

        response = self.json_ok('info', {'uid': self.uid, 'path': '/disk/dir/test.txt', 'meta': ''})
        source_id = '111'
        self._insert_source_id_record(self.uid,
                                      FileChecksums(response['meta']['md5'], response['meta']['sha256'],
                                                    response['meta']['size']).hid,
                                      source_id)
        resp = self.json_ok('albums_list', opts={'uid': self.uid, 'meta': ''})
        assert all(['source_ids' not in x['cover']['object']['meta'] for x in resp])


@pytest.mark.skipif(not INIT_USER_IN_POSTGRES, reason='pg only feature')
class GlobalGalleryIntegrationWithSharedFoldersTestCase(GlobalGalleryTestCaseMixin, CommonJsonApiTestCase):

    def setup_method(self, method):
        super(GlobalGalleryIntegrationWithSharedFoldersTestCase, self).setup_method(method)
        self.create_user(self.uid_1, shard='pg')

        self.shared_root_folder_path = '/disk/shared'
        self.json_ok('mkdir', {'uid': self.uid, 'path': self.shared_root_folder_path})
        self.share_dir(self.uid, self.uid_1, self.email_1, self.shared_root_folder_path)
        result = self.json_ok('info', {'uid': self.uid, 'path': self.shared_root_folder_path, 'meta': 'group'})
        self.gid = result['meta']['group']['gid']

        self.source_id = '111'

        self.shared_file_path = self.shared_root_folder_path + '/file.jpg'
        self.upload_path = '/disk/file.jpg'

    @parameterized.expand([
        (True, True, 'trash_append', 1,),
        (True, False, 'trash_append', 0,),
        (False, True, 'trash_append', 1,),
        (False, False, 'trash_append', 0,),
        (True, True, 'rm', 1,),
        (True, False, 'rm', 0,),
        (False, True, 'rm', 1,),
        (False, False, 'rm', 0,),
    ])
    def test_file_with_source_id_deleted_from_shared_folder(self, uploaded_by_owner, removed_by_owner,
                                                            endpoint, correct_num_of_records_in_log):
        uploader_uid = self.uid if uploaded_by_owner else self.uid_1

        self.upload_file(uploader_uid, self.upload_path, opts={'source_id': self.source_id})
        # Делаем так, потому что хотим, чтобы в базе source_id был у пользователя, который загрузил файл
        self.json_ok('move', {'uid': uploader_uid, 'src': self.upload_path, 'dst': self.shared_file_path})

        remover_uid = self.uid if removed_by_owner else self.uid_1
        self.json_ok(endpoint, {'uid': remover_uid, 'path': self.shared_file_path})
        assert correct_num_of_records_in_log == len(list(DeletionLogDAO().find({'uid': self.uid}))
                                                    + list(DeletionLogDAO().find({'uid': self.uid_1})))

    @parameterized.expand([
        (True, True,),
        (True, False,),
        (False, True,),
        (False, False,),
    ])
    def test_file_with_source_id_moved_from_shared_folder(self, uploaded_by_owner, removed_by_owner):
        uploader_uid = self.uid if uploaded_by_owner else self.uid_1

        self.upload_file(uploader_uid, self.upload_path, opts={'source_id': self.source_id})
        self.json_ok('move', {'uid': uploader_uid, 'src': self.upload_path, 'dst': self.shared_file_path})

        mover_uid = self.uid if removed_by_owner else self.uid_1
        new_path = '/disk/moved.jpg'
        self.json_ok('move', {'uid': mover_uid, 'src': self.shared_file_path, 'dst': new_path})
        assert 0 == len(list(DeletionLogDAO().find({'uid': self.uid}))
                        + list(DeletionLogDAO().find({'uid': self.uid_1})))

    @parameterized.expand([
        ('share_unshare_folder', True,),
        ('share_unshare_folder', False,),
        ('share_kick_from_group', True,),
        ('share_kick_from_group', False,),
        ('share_leave_group', True,),
        ('share_leave_group', False,),
    ])
    def test_user_removed_from_folder(self, endpoint, uploaded_by_owner):
        uploader_uid = self.uid if uploaded_by_owner else self.uid_1

        self.upload_file(uploader_uid, self.upload_path, opts={'source_id': self.source_id})
        self.json_ok('move', {'uid': uploader_uid, 'src': self.upload_path, 'dst': self.shared_file_path})

        if endpoint == 'share_unshare_folder':
            self.json_ok(endpoint, {'uid': self.uid, 'gid': self.gid})
        elif endpoint == 'share_kick_from_group':
            self.json_ok(endpoint, {'uid': self.uid, 'gid': self.gid, 'user_uid': self.uid_1})
        elif endpoint == 'share_leave_group':
            self.json_ok(endpoint, {'uid': self.uid_1, 'gid': self.gid})
        else:
            raise NotImplemented('Unexpected case')

        assert 0 == len(list(DeletionLogDAO().find({'uid': self.uid}))
                        + list(DeletionLogDAO().find({'uid': self.uid_1})))

    @parameterized.expand([
        (True, 1,),
        (False, 0,),
    ])
    def test_uploading_file_with_source_id_to_shared_folder_no_hardlink(self, uploaded_by_owner, correct_source_id_number_for_owner):
        uploader_uid = self.uid if uploaded_by_owner else self.uid_1
        with capture_queue_errors() as errors:
            self.upload_file(uploader_uid, self.shared_file_path, opts={'source_id': self.source_id})
        assert correct_source_id_number_for_owner == len(list(SourceIdDAO().find({'uid': self.uid})))
        assert not len(list(SourceIdDAO().find({'uid': self.uid_1})))
        assert not errors

    @parameterized.expand([
        (True, 1,),
        (False, 0,),
    ])
    def test_uploading_file_with_source_id_to_shared_folder_hardlink_on_callback(self, uploaded_by_owner, correct_source_id_number_for_owner):
        uploader_uid = self.uid if uploaded_by_owner else self.uid_1
        path = '/disk/original_file.txt'
        self.upload_file(uploader_uid, path)
        r = self.json_ok('info', {'uid': uploader_uid, 'path': path, 'meta': ''})
        md5, sha256, size = r['meta']['md5'], r['meta']['sha256'], r['meta']['size']

        with mock.patch('mpfs.core.filesystem.hardlinks.common.AbstractLink.is_file_in_storage', return_value=True), \
                capture_queue_errors() as errors:
            self.upload_file(uploader_uid, self.shared_file_path, file_data={'md5': md5, 'sha256': sha256, 'size': size},
                             hardlink=True, opts={'source_id': self.source_id})

        assert correct_source_id_number_for_owner == len(list(SourceIdDAO().find({'uid': self.uid})))
        assert not len(list(SourceIdDAO().find({'uid': self.uid_1})))
        assert not errors

    @parameterized.expand([
        (True, 1,),
        (False, 0,),
    ])
    def test_uploading_file_with_source_id_to_shared_folder_hardlink_on_store(self, uploaded_by_owner, correct_source_id_number_for_owner):
        uploader_uid = self.uid if uploaded_by_owner else self.uid_1
        path = '/disk/original_file.txt'
        self.upload_file(uploader_uid, path)
        r = self.json_ok('info', {'uid': uploader_uid, 'path': path, 'meta': ''})
        md5, sha256, size = r['meta']['md5'], r['meta']['sha256'], r['meta']['size']

        with mock.patch('mpfs.core.filesystem.hardlinks.common.AbstractLink.is_file_in_storage', return_value=True):
            res = self.json_ok('store', {'uid': uploader_uid,
                                         'path': self.shared_file_path,
                                         'sha256': sha256,
                                         'md5': md5,
                                         'size': size,
                                         'source_id': self.source_id})

        assert 'hardlinked' == res['status']
        assert correct_source_id_number_for_owner == len(list(SourceIdDAO().find({'uid': self.uid})))
        assert not len(list(SourceIdDAO().find({'uid': self.uid_1})))


@pytest.mark.skipif(not INIT_USER_IN_POSTGRES, reason='pg only feature')
class GlobalGalleryLivePhotoIntegrationTestCase(LivePhotoMixin, GlobalGalleryTestCaseMixin, CommonJsonApiTestCase):
    md5 = 'b5c64136bd7f685a5bfa1e9c78d7e565'
    sha256 = 'ca4f3cbf0bdf65858cd0f12dbff6a611dfdf13037ebfa4a31199155016f64853'
    size = 16029

    def setup_method(self, method):
        super(GlobalGalleryLivePhotoIntegrationTestCase, self).setup_method(method)
        self.json_ok('enable_unlimited_autouploading', {'uid': self.uid})

    def test_uploading_live_photo_marks_source_ids(self):
        path = '/disk/1.jpg'
        self.store_live_photo_with_video(path, opts={'source_id': '111'})

        source_ids = list(SourceIdDAO().find({'uid': self.uid}))
        assert 1 == len(source_ids)
        assert source_ids[0]['is_live_photo']

    def test_add_source_id_file_exists_by_hashes_in_upload_log_regular_store(self):
        old_simple_photo_source_id = '111'
        old_live_photo_source_id = '222'
        new_simple_photo_source_id = '333'
        new_live_photo_source_id = '444'

        self._insert_source_id_record(self.uid, FileChecksums(self.md5, self.sha256, self.size).hid, old_simple_photo_source_id)
        self._insert_source_id_record(self.uid, FileChecksums(self.md5, self.sha256, self.size).hid, old_live_photo_source_id, is_live_photo=True)

        self.upload_file(self.uid, '/disk/2.jpg', file_data={'md5': self.md5, 'sha256': self.sha256, 'size': self.size},
                         opts={'source_id': new_simple_photo_source_id})
        self.store_live_photo_with_video('/disk/3.jpg', file_data={'md5': self.md5, 'sha256': self.sha256, 'size': self.size},
                                         opts={'source_id': new_live_photo_source_id})

        source_ids = list(SourceIdDAO().find({'uid': self.uid}))
        assert 4 == len(source_ids)

        got_source_ids = {(x['source_id'], x['is_live_photo']) for x in source_ids}
        correct_source_ids = {
            (old_simple_photo_source_id, True),
            (new_simple_photo_source_id, True),
            (old_live_photo_source_id, True),
            (new_live_photo_source_id, True),
        }
        assert got_source_ids == correct_source_ids

    def test_add_source_id_file_exists_by_hashes_in_upload_log_autoupload_store(self):
        old_simple_photo_source_id = '111'
        old_live_photo_source_id = '222'
        new_live_photo_source_id = '444'

        self._insert_source_id_record(self.uid, FileChecksums(self.md5, self.sha256, self.size).hid, old_simple_photo_source_id)
        self._insert_source_id_record(self.uid, FileChecksums(self.md5, self.sha256, self.size).hid, old_live_photo_source_id, is_live_photo=True)

        video_md5 = hashlib.md5(str(time.time())).hexdigest()
        video_sha256 = hashlib.sha256(str(time.time())).hexdigest()
        video_size = 1
        path = '/photostream/1.jpg'

        self.store_live_photo_with_video('/disk/old.jpg', file_data={
            'md5': self.md5,
            'sha256': self.sha256,
            'size': self.size,
            'live_photo_md5': video_md5,
            'live_photo_sha256': video_sha256,
            'live_photo_size': video_size,
        })

        res = self.json_error('store', {'uid': self.uid,
                                        'path': path,
                                        'md5': self.md5,
                                        'sha256': self.sha256,
                                        'size': self.size,
                                        'live_photo_md5': video_md5,
                                        'live_photo_sha256': video_sha256,
                                        'live_photo_size': video_size,
                                        'live_photo_type': 'photo',
                                        'source_id': new_live_photo_source_id},
                              code=codes.FILE_EXISTS)

        source_ids = list(SourceIdDAO().find({'uid': self.uid}))
        assert 3 == len(source_ids)

        got_source_ids = {(x['source_id'], x['is_live_photo']) for x in source_ids}
        correct_source_ids = {
            (old_simple_photo_source_id, True),
            (old_live_photo_source_id, True),
            (new_live_photo_source_id, True),
        }
        assert got_source_ids == correct_source_ids

    def test_deleting_live_photo_saves_marked_record_in_deletion_log(self):
        path = '/disk/1.jpg'
        self.store_live_photo_with_video(path, opts={'source_id': '111'})
        self.json_ok('trash_append', {'uid': self.uid, 'path': path})
        deletion_log_records = list(DeletionLogDAO().find({'uid': self.uid}))
        assert 1 == len(deletion_log_records)
        assert deletion_log_records[0]['is_live_photo']

    @parameterized.expand([
        ('delete_live_photo', True, 'trash_append'),
        ('delete_simple_photo', False, 'trash_append'),
        # ('rm_live_photo', True, 'rm'),
        ('rm_simple_photo', False, 'rm'),
        ('delete_live_photo', True, 'trash_append'),
        ('delete_simple_photo', False, 'trash_append'),
        # ('rm_live_photo', True, 'rm'),
        ('rm_simple_photo', False, 'rm'),
    ])
    def test_writing_to_deletion_log_discerns_live_photo_from_simple_photo(self, _, is_live_photo, endpoint):
        live_photo_source_id = '111'
        photo_source_id = '222'

        md5 = 'b5c64136bd7f685a5bfa1e9c78d7e565'
        sha256 = 'ca4f3cbf0bdf65858cd0f12dbff6a611dfdf13037ebfa4a31199155016f64853'
        size = 16029
        file_data = {
            'md5': md5,
            'sha256': sha256,
            'size': size,
        }

        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/r1'})
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/r1/r2'})
        path = '/disk/r1/r2/1.jpg'

        if is_live_photo:
            self.store_live_photo_with_video(path, file_data=file_data, opts={'source_id': live_photo_source_id})
        else:
            self.upload_file(self.uid, path, file_data=file_data, opts={'source_id': photo_source_id})

        self.json_ok(endpoint, {'uid': self.uid, 'path': path})
        deletion_log_records = list(DeletionLogDAO().find({'uid': self.uid}))
        assert 1 == len(deletion_log_records)
        assert deletion_log_records[0]['is_live_photo'] is is_live_photo

    def test_simple_photo_with_same_hashes_as_existing_live_photo(self):
        live_photo_source_id = '111'
        photo_source_id = '222'

        area, real_path_prefix = '/photounlim', '/photounlim'
        live_photo_store_path = area + '/photo.jpg'
        live_photo_real_path = real_path_prefix + '/photo.jpg'

        with capture_queue_errors() as errors:
            photo_md5, photo_sha256, photo_size = self.store_live_photo_with_video(
                live_photo_store_path, live_photo_real_path=live_photo_real_path, opts={'source_id': live_photo_source_id})

            regular_photo_store_path = area + '/regular_photo.jpg'
            self.upload_file(
                self.uid, regular_photo_store_path,
                file_data={'md5': photo_md5, 'sha256': photo_sha256, 'size': photo_size, 'mimetype': 'image/jpg'},
                opts={'source_id': photo_source_id}
            )

        assert not errors

        with change_experiment_context_with(uid=self.uid):
            source_id = list(GlobalGalleryController().fetch_source_ids_for_ids(self.uid, [live_photo_source_id]))[0]
            assert source_id.is_live_photo

            source_id = list(GlobalGalleryController().fetch_source_ids_for_ids(self.uid, [photo_source_id]))[0]
            assert not source_id.is_live_photo

    @parameterized.expand([
        ('can_add_new_source_id', ['111', '222'], '333', ['111', '222', '333']),
        ('cant_add_new_source_id', ['111', '222', '444'], '333', ['111', '222', '444']),
        ('can_add_new_source_id_no_new_source_id', ['111', '222'], None, ['111', '222']),
        ('cant_add_new_source_id_no_new_source_id', ['111', '222', '444'], None, ['111', '222', '444']),
    ])
    def test_regular_photo_transformed_to_live_photo_adds_source_id_and_changes_previous_source_ids(
            self, case_name, old_source_ids, new_source_id, expected_source_ids):
        area, real_path_prefix, collection = '/photounlim', '/photounlim', 'photounlim_data'

        photo_md5 = 'b5c64136bd7f685a5bfa1e9c78d7e565'
        photo_sha256 = 'ca4f3cbf0bdf65858cd0f12dbff6a611dfdf13037ebfa4a31199155016f64853'
        photo_size = 16029

        regular_photo_store_path = area + '/regular_photo.jpg'
        regular_photo_real_path = real_path_prefix + '/regular_photo.jpg'

        for source_id in old_source_ids:
            self._insert_source_id_record(
                self.uid, FileChecksums(photo_md5, photo_sha256, photo_size).hid, source_id)

        with mock.patch('mpfs.core.global_gallery.logic.controller.GLOBAL_GALLERY_MAX_SOURCE_ID_LIMIT', 3), \
                capture_queue_errors() as errors:
            opts = {}
            if new_source_id:
                opts['source_id'] = new_source_id
            self.upload_file(
                self.uid, regular_photo_store_path,
                file_data={'md5': photo_md5, 'sha256': photo_sha256, 'size': photo_size, 'mimetype': 'image/jpg'},
                opts=opts
            )

            video_md5 = hashlib.md5(str(time.time() + 1)).hexdigest()
            video_sha256 = hashlib.sha256(str(time.time() + 1)).hexdigest()
            video_size = 1234

            self.upload_file(
                self.uid,
                regular_photo_store_path,
                file_data={
                    'md5': video_md5, 'sha256': video_sha256, 'size': video_size, 'mimetype': 'video/mp4'
                },
                live_photo_md5=photo_md5,
                live_photo_sha256=photo_sha256,
                live_photo_size=photo_size,
                live_photo_type='video'
            )

            info = self.json_ok('info', {'uid': self.uid, 'path': regular_photo_real_path, 'meta': ''})
            assert info['meta']['is_live_photo']

        requested_source_ids = old_source_ids
        if new_source_id is not None:
            requested_source_ids.append(new_source_id)
        source_ids = list(GlobalGalleryController().fetch_source_ids_for_ids(self.uid, requested_source_ids))
        assert all(x.is_live_photo for x in source_ids)
        assert set(expected_source_ids) == {x.source_id for x in source_ids}

        assert not errors

    def test_uploading_live_photo_part_with_hashes_from_regular_photo_wont_add_source_id(self):
        area, real_path_prefix = '/photounlim', '/photounlim'

        live_photo_source_id = '111'
        photo_source_id = '222'

        video_md5 = hashlib.md5(str(time.time() + 1)).hexdigest()
        video_sha256 = hashlib.sha256(str(time.time() + 1)).hexdigest()
        video_size = 1234

        regular_photo_store_path = area + '/regular_photo.jpg'

        with capture_queue_errors() as errors:
            self.upload_file(
                self.uid, regular_photo_store_path,
                file_data={'md5': self.md5, 'sha256': self.sha256, 'size': self.size, 'mimetype': 'image/jpg'},
                opts={'source_id': photo_source_id}
            )

            live_photo_store_path = area + '/photo.jpg'
            live_photo_real_path = real_path_prefix + '/photo.jpg'

            self.json_error('store', {'uid': self.uid,
                                      'path': '/photostream/1.jpg',
                                      'sha256': self.sha256,
                                      'md5': self.md5,
                                      'size': self.size,
                                      'source_id': live_photo_source_id,
                                      'force_deletion_log_deduplication': 1,
                                      'live_photo_md5': video_md5,
                                      'live_photo_sha256': video_sha256,
                                      'live_photo_size': video_size,
                                      'live_photo_type': 'photo',
                                      })

            self.json_error('info', {'uid': self.uid, 'path': live_photo_real_path, 'meta': ''})

            assert 1 == len(list(SourceIdDAO().find({'uid': self.uid})))
            assert live_photo_source_id not in {x['source_id'] for x in SourceIdDAO().find({'uid': self.uid})}

        assert not errors

    def test_upload_video_and_then_photo(self):
        source_id = '111'

        photo_md5 = hashlib.md5(str(time.time() + 2)).hexdigest()
        photo_sha256 = hashlib.sha256(str(time.time() + 2)).hexdigest()
        photo_size = 1234

        with capture_queue_errors() as errors:

            source_id_item = SourceIdDAOItem.build_by_params(
                self.uid,
                FileChecksums(photo_md5, photo_sha256, photo_size).hid,
                source_id,
                True
            )
            SourceIdDAO().save(source_id_item)

            regular_photo_store_path = '/disk/2.heic'

            self.upload_file(
                self.uid, regular_photo_store_path,
                file_data={'md5': photo_md5, 'sha256': photo_sha256, 'size': photo_size, 'mimetype': 'image/jpg'},
                opts={'source_id': source_id}
            )

            video_md5 = hashlib.md5(str(time.time() + 1)).hexdigest()
            video_sha256 = hashlib.sha256(str(time.time() + 1)).hexdigest()
            video_size = 1234

            self.upload_file(
                self.uid,
                regular_photo_store_path,
                file_data={
                    'md5': video_md5, 'sha256': video_sha256, 'size': video_size, 'mimetype': 'video/mp4'
                },
                live_photo_md5=photo_md5,
                live_photo_sha256=photo_sha256,
                live_photo_size=photo_size,
                live_photo_type='video'
            )

        assert not errors

        source_ids = list(SourceIdDAO().find({'uid': self.uid}))
        assert len(source_ids) == 1
        assert source_ids[0]['is_live_photo']
