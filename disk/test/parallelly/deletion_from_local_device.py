# -*- coding: utf-8 -*-
import hashlib
from contextlib import contextmanager

import itertools

import mock
from nose_parameterized import parameterized
from test.base import CommonDiskTestCase
from test.common.sharing import CommonSharingMethods
from test.parallelly.api.disk.base import DiskApiTestCase

from test.base_suit import  UploadFileTestCaseMixin
from test.base_suit import UserTestCaseMixin
from mpfs.common.errors import ResourceNotFound
from mpfs.common.static import tags
from mpfs.common.util import from_json
from mpfs.core.address import ResourceId


@contextmanager
def patch_limit_for_database_read(limit=1):
    """
    Патчим размер ответа при походе в базу для проверки пагинации
    """
    import mpfs.core.deletion_from_local_device.logic
    original_limit = mpfs.core.deletion_from_local_device.logic.DB_CHUNK_SIZE
    mpfs.core.deletion_from_local_device.logic.LIMIT = limit
    try:
        yield
    finally:
        mpfs.core.deletion_from_local_device.logic.LIMIT = original_limit


class ListResourcesByPathsMocker(object):

    @staticmethod
    def all_indexed():
        def get_indexed_files(uid, file_ids):
            return {'total': len(file_ids), 'items': [{'file_id': x} for x in file_ids]}
        return get_indexed_files

    @staticmethod
    def index_some_files_only(indexed_file_ids):
        def get_indexed_files(uid, file_ids):
            items = [{'file_id': x} for x in file_ids if x in indexed_file_ids]
            return {'total': len(items), 'items': items}
        return get_indexed_files


class DeletionFromLocalMethodsMixin(object):

    def _upload_files_by_paths_and_get_hashes(self, paths, uid=None):
        if uid is None:
            uid = self.uid
        hashes = []
        for i in paths:
            # Явно генерируем md5 различными, чтобы случайно не совпали хэши
            self.upload_file(uid, i, file_data={'md5': hashlib.md5(i).hexdigest()})
            meta = self.json_ok('info', {'uid': uid, 'path': i, 'meta': ''})['meta']
            hashes.append({'md5': meta['md5'], 'sha256': meta['sha256'], 'size': meta['size']})
        return hashes


class DeletionFromLocalDeviceTestCase(CommonSharingMethods, DeletionFromLocalMethodsMixin, DiskApiTestCase):
    def setup_method(self, method):
        super(DeletionFromLocalDeviceTestCase, self).setup_method(method)
        self.json_ok('enable_unlimited_autouploading', {'uid': self.uid})

    def test_format(self):
        self.upload_file(self.uid, '/disk/1.jpg')
        resp = self.json_ok('info', {'uid': self.uid, 'path': '/disk/1.jpg', 'meta': 'file_id,md5,sha256,size'})['meta']
        correct_found_item = {
            'resource_id': ResourceId(self.uid, resp['file_id']).serialize(),
            'can_delete': True,
        }
        correct_not_found_item = {
            'can_delete': False,
        }
        items = [
            {
                'md5': resp['md5'],
                'sha256': resp['sha256'],
                'size': resp['size'],
            },
            {
                'md5': hashlib.md5('same').hexdigest(),
                'sha256': hashlib.md5('same').hexdigest(),
                'size': '1',
            },
        ]
        with mock.patch('mpfs.core.services.search_service.SearchDB.get_indexed_files',
                        side_effect=ListResourcesByPathsMocker.all_indexed()):
            results = self.json_ok('can_delete', opts={'uid': self.uid}, json={'items': items})
        assert results['items'] == [correct_found_item, correct_not_found_item]

    @parameterized.expand([
        ('several_pages', 1,),
        ('one_page', 100,),
    ])
    def test_pagination_all_files_found(self, case_name, limit):
        paths = ('/photounlim/1.jpg', '/photounlim/2.jpg', '/disk/3.jpg', '/disk/4.jpg')
        items = self._upload_files_by_paths_and_get_hashes(paths)

        with patch_limit_for_database_read(limit=limit), \
                mock.patch('mpfs.core.services.search_service.SearchDB.get_indexed_files',
                           side_effect=ListResourcesByPathsMocker.all_indexed()):
            results = self.json_ok('can_delete', opts={'uid': self.uid}, json={'items': items})
        assert all([x['can_delete'] for x in results['items']])

    def test_same_hashes_several_times(self):
        paths = ('/disk/1.jpg',)
        items = self._upload_files_by_paths_and_get_hashes(paths) * 2

        with mock.patch('mpfs.core.services.search_service.SearchDB.get_indexed_files',
                        side_effect=ListResourcesByPathsMocker.all_indexed()):
            results = self.json_ok('can_delete', opts={'uid': self.uid}, json={'items': items})
        assert all([x['can_delete'] for x in results['items']])

    def test_some_files_not_found(self):
        """
        Проверяем ситуацию, когда 2 файла из запроса не присутствуют в базе. В запросе это будут 2ой и 4ый
        ресурсы. Для их мы должны вернуть, что их удалять нельзя.
        """
        paths = ('/photounlim/1.jpg', '/disk/2.jpg')
        unexistent_hashes = []
        for i in range(2):
            unexistent_hashes.append({'size': '1', 'md5': hashlib.md5(str(i)).hexdigest(), 'sha256': hashlib.sha256(str(i)).hexdigest()})
        existent_hashes = self._upload_files_by_paths_and_get_hashes(paths)

        items = list(itertools.chain(*zip(existent_hashes, unexistent_hashes)))

        with mock.patch('mpfs.core.services.search_service.SearchDB.get_indexed_files',
                        side_effect=ListResourcesByPathsMocker.all_indexed()):
            results = self.json_ok('can_delete', opts={'uid': self.uid}, json={'items': items})
        assert [x['can_delete'] for x in results['items']] == [True, False, True, False]

    @parameterized.expand([
        (['/photounlim/1.jpg', '/photounlim/2.jpg'], [True, True]),
        (['/photounlim/2.jpg'], [False, True]),
        (['/photounlim/1.jpg'], [True, False]),
        ([], [False, False]),
    ])
    def test_photounlim_files_indexed_or_not(self, indexed_files, correct_answer):
        paths = ('/photounlim/1.jpg', '/photounlim/2.jpg')
        items = self._upload_files_by_paths_and_get_hashes(paths)

        indexed_file_ids = []
        for i in indexed_files:
            r = self.json_ok('info', opts={'uid': self.uid, 'path': i, 'meta': 'file_id'})
            indexed_file_ids.append(r['meta']['file_id'])

        with mock.patch('mpfs.core.services.search_service.SearchDB.get_indexed_files',
                        side_effect=ListResourcesByPathsMocker.index_some_files_only(indexed_file_ids)):
            results = self.json_ok('can_delete', opts={'uid': self.uid}, json={'items': items})
        assert [x['can_delete'] for x in results['items']] == correct_answer

    def test_many_hardlinked_files_but_photounlim_not_indexed(self):
        """
        Проверяем, что если загружено много файлов с одинаковым хидом, но при этом есть один такой, который присутствует
        в фотоанлиме и не проиндексирован, то все равно можно удалить, так как он есть в /disk
        """
        hashes_opts = {
            'md5': hashlib.md5('same').hexdigest(),
            'sha256': hashlib.sha256('same').hexdigest(),
            'size': '1'
        }
        self.upload_file(self.uid, '/photounlim/1.jpg', file_data=hashes_opts)
        paths = ('/disk/3.jpg', '/disk/4.jpg')
        for i in paths:
            self.upload_file(self.uid, i, file_data=hashes_opts)

        with mock.patch('mpfs.core.services.search_service.SearchDB.get_indexed_files',
                        side_effect=ListResourcesByPathsMocker.index_some_files_only([])):
            results = self.json_ok('can_delete', opts={'uid': self.uid}, json={'items': [hashes_opts]})
        assert [x['can_delete'] for x in results['items']] == [True, ]

    def test_files_from_shared_folders_always_forbidden_to_delete(self):
        self.create_user(self.uid_1)
        self.create_user(self.uid_3)

        self.json_ok('mkdir', {'uid': self.uid_1, 'path': '/disk/i_am_guest'})
        self.create_group(uid=self.uid_1, path='/disk/i_am_guest')
        hash_ = self.invite_user(owner=self.uid_1, uid=self.uid, email=self.email, path='/disk/i_am_guest', rights=660)
        self.activate_invite(uid=self.uid, hash=hash_)

        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/i_am_owner'})
        self.create_group(uid=self.uid, path='/disk/i_am_owner')
        hash_ = self.invite_user(owner=self.uid, uid=self.uid_3, email=self.email_3, path='/disk/i_am_owner', rights=660)
        self.activate_invite(uid=self.uid_3, hash=hash_)

        paths = ('/disk/i_am_owner/1.jpg', '/disk/i_am_guest/2.jpg')
        items = self._upload_files_by_paths_and_get_hashes(paths)
        with mock.patch('mpfs.core.services.search_service.SearchDB.get_indexed_files',
                        side_effect=ListResourcesByPathsMocker.all_indexed()):
            results = self.json_ok('can_delete', opts={'uid': self.uid}, json={'items': items})
        assert [x['can_delete'] for x in results['items']] == [False, False]

class DeletionFromLocalDevicePlatformTestCase(DeletionFromLocalMethodsMixin, UserTestCaseMixin, UploadFileTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.INTERNAL
    api_version = 'v1'

    def setup_method(self, method):
        super(DeletionFromLocalDevicePlatformTestCase, self).setup_method(method)
        self.create_user(self.uid, noemail=1)

    def test_proxy_fo_mpfs(self):
        unexistent_hashes = [{'size': '1', 'md5': hashlib.md5('1').hexdigest(), 'sha256': hashlib.sha256('1').hexdigest()}]
        existent_hashes = self._upload_files_by_paths_and_get_hashes(['/disk/1.jpg'])
        items = unexistent_hashes + existent_hashes
        with self.specified_client(scopes=['cloud_api:disk.write']):
            resp = self.client.request('POST', 'case/disk/can-delete', data={'items': items})
        assert resp.status_code == 200
        assert [x['can_delete'] for x in from_json(resp.content)['items']] == [False, True]

    def test_proxy_fo_mpfs_returns_500_on_any_error(self):
        items = [{'size': '1', 'md5': hashlib.md5('1').hexdigest(), 'sha256': hashlib.sha256('1').hexdigest()}]
        # Имитируем ситуация, когда МПФС возвращает 404. Платформа в такой ситуации все равно должна вернуть 500
        with self.specified_client(scopes=['cloud_api:disk.write']), \
                mock.patch('mpfs.core.base.can_delete', side_effect=ResourceNotFound()):
            resp = self.client.request('POST', 'case/disk/can-delete', data={'items': items})
        assert resp.status_code == 500

    def test_number_of_items_to_handle_is_too_large(self):
        items = [{'size': '1', 'md5': hashlib.md5('1').hexdigest(), 'sha256': hashlib.sha256('1').hexdigest()}] * 201
        with self.specified_client(scopes=['cloud_api:disk.write']), \
                mock.patch('mpfs.core.base.can_delete', side_effect=ResourceNotFound()):
            resp = self.client.request('POST', 'case/disk/can-delete', data={'items': items})
        assert resp.status_code == 413
