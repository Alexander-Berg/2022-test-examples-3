# -*- coding: utf-8 -*-
import hashlib
from lxml import etree

import mock
import time

from nose_parameterized import parameterized

from mpfs.common.util.experiments.logic import experiment_manager
from test.base import CommonDiskTestCase, DiskTestCase
from common.sharing import CommonSharingMethods

from mpfs.common.errors import APIError
from mpfs.common.static import codes
from mpfs.common.static.messages import true_operation_titles
from mpfs.common.static.tags import COMMIT_FILE_INFO, COMMIT_FILE_UPLOAD, COMMIT_FINAL
from mpfs.core.address import Address
from mpfs.core.bus import Bus
from mpfs.core.filesystem.dao.resource import PhotounlimDAO, ResourceDAO
from mpfs.core.filesystem.quota import Quota
from mpfs.core.user.base import User
from mpfs.core.user.constants import PHOTOUNLIM_AREA
from mpfs.core.user.dao.user import UserDAO
from mpfs.metastorage.mongo.collections.base import UserIndexCollection
from mpfs.metastorage.mongo.collections.filesystem import PhotounlimDataCollection
from test.helpers.stubs.services import KladunStub

from test.helpers.stubs.smart_services import DiskSearchSmartMockHelper
from test.parallelly.api.disk.base import DiskApiTestCase
from test.helpers.size_units import MB, GB
from test.parallelly.json_api.store_suit import StoreUtilsMixin


class UnlimitedAutouploadTestCase(DiskTestCase):

    @parameterized.expand([
        (True,),
        (False,),
    ])
    def test_enable_unlimited_autouploading_test_case(self, unlimited_autouploading_enabled_at_start):
        self._prepare_user_state(unlimited_autouploading_enabled_at_start)
        self.json_ok('enable_unlimited_autouploading', {'uid': self.uid})
        UserIndexCollection.reset()
        assert User(self.uid).is_unlimited_autouploading_enabled()
        assert UserDAO().find_one({'_id': self.uid}).get('unlimited_autouploading_enabled')

    @parameterized.expand([
        (True,),
        (False,),
    ])
    def test_disable_unlimited_autouploading_test_case(self, unlimited_autouploading_enabled_at_start):
        self._prepare_user_state(unlimited_autouploading_enabled_at_start)
        self.json_ok('disable_unlimited_autouploading', {'uid': self.uid})
        UserIndexCollection.reset()
        assert not User(self.uid).is_unlimited_autouploading_enabled()
        assert not UserDAO().find_one({'_id': self.uid}).get('unlimited_autouploading_enabled')

    def test_user_info_returns_unlimited_autoupload_state(self):
        result = self.json_ok('user_info', {'uid': self.uid})
        assert result['unlimited_autoupload_enabled'] == 0

        self.json_ok('enable_unlimited_autouploading', {'uid': self.uid})
        UserIndexCollection.reset()

        result = self.json_ok('user_info', {'uid': self.uid})
        assert result['unlimited_autoupload_enabled'] == 1

    def test_unlimited_autoupload_activation_requests_smartcache(self):
        with mock.patch('mpfs.core.services.smartcache_service.SmartcacheService.initialize_photostream') as m:
            self.json_ok('enable_unlimited_autouploading', {'uid': self.uid})
            assert m.called

    def _prepare_user_state(self, unlimited_autouploading_enabled_at_start):
        if unlimited_autouploading_enabled_at_start:
            UserDAO().enable_unlimited_autouploading(self.uid)
        else:
            UserDAO().disable_unlimited_autouploading(self.uid)


class UnlimitedAreaTestCase(CommonDiskTestCase, StoreUtilsMixin, DiskApiTestCase):
    mobile_headers = {'Yandex-Cloud-Request-ID': 'ios-test'}
    """требуется указывать этот хедер если ожидается успешная загрузка в /photounlim через /photostream"""

    def setup_method(self, method):
        super(UnlimitedAreaTestCase, self).setup_method(method)
        self.json_ok('enable_unlimited_autouploading', {'uid': self.uid})

    def test_enabling_unlimited_autouploading_only_creates_area(self):
        with mock.patch('mpfs.metastorage.mongo.collections.filesystem.PhotounlimDataCollection.create',
                        wraps=PhotounlimDataCollection().create) as create_mock:
            self.json_ok('user_init', {'uid': self.user_1.uid})
            assert not create_mock.called
            self.json_ok('enable_unlimited_autouploading', {'uid': self.user_1.uid})
            assert create_mock.called

    def test_failed_smart_cache_init_returns_200_for_photounlim_enabling(self):
        self.json_ok('user_init', {'uid': self.user_1.uid})
        with mock.patch('mpfs.core.services.smartcache_service.SmartcacheService.initialize_photostream',
                        side_effect=APIError()) as create_mock:
            self.json_ok('enable_unlimited_autouploading', {'uid': self.user_1.uid})
        resp = self.json_ok('user_info', {'uid': self.user_1.uid})
        assert resp['unlimited_autoupload_enabled']

    def test_store_photostream(self):
        self.upload_file(self.uid, '/photostream/1.jpg', headers=self.mobile_headers)
        self.json_ok('info', {'uid': self.uid, 'path': '/photounlim/1.jpg'})

    def test_store_photounlim(self):
        self.upload_file(self.uid, '/photounlim/1.jpg', headers=self.mobile_headers)
        self.json_ok('info', {'uid': self.uid, 'path': '/photounlim/1.jpg'})

    def test_store_photostream_hardlink_in_disk_returns_resource_exists(self):
        md5 = '123456789abcdef0123456789abcdef0'
        sha256 = '123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0'
        size = 1000

        self.upload_file(self.uid, '/disk/original.jpg', file_data={'md5': md5, 'sha256': sha256, 'size': size})
        self.json_error('store', {'uid': self.uid, 'path': '/photostream/1.jpg', 'md5': md5, 'sha256': sha256, 'size': size},
                        headers=self.mobile_headers, code=codes.FILE_EXISTS)
        self.json_error('info', {'uid': self.uid, 'path': '/photounlim/1.jpg'}, code=codes.RESOURCE_NOT_FOUND)

    def test_store_photounlim_hardlink_in_disk_returns_resource_exists(self):
        md5 = '123456789abcdef0123456789abcdef0'
        sha256 = '123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0'
        size = 1000

        self.upload_file(self.uid, '/disk/original.jpg', file_data={'md5': md5, 'sha256': sha256, 'size': size})
        self.json_error('store', {'uid': self.uid, 'path': '/photounlim/1.jpg', 'md5': md5, 'sha256': sha256, 'size': size},
                        headers=self.mobile_headers, code=codes.FILE_EXISTS)
        self.json_error('info', {'uid': self.uid, 'path': '/photounlim/1.jpg'}, code=codes.RESOURCE_NOT_FOUND)

    def test_store_photostream_hardlink_in_photounlim_returns_resource_exists(self):
        md5 = '123456789abcdef0123456789abcdef0'
        sha256 = '123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0'
        size = 1000

        self.upload_file(self.uid, '/photounlim/original.jpg', file_data={'md5': md5, 'sha256': sha256, 'size': size})
        self.json_error('store', {'uid': self.uid, 'path': '/photostream/1.jpg', 'md5': md5, 'sha256': sha256, 'size': size},
                        headers=self.mobile_headers, code=codes.FILE_EXISTS)
        self.json_error('info', {'uid': self.uid, 'path': '/photounlim/1.jpg'}, code=codes.RESOURCE_NOT_FOUND)

    def test_store_photounlim_hardlink_in_photounlim_returns_resource_exists(self):
        md5 = '123456789abcdef0123456789abcdef0'
        sha256 = '123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef0'
        size = 1000

        self.upload_file(self.uid, '/photounlim/original.jpg', file_data={'md5': md5, 'sha256': sha256, 'size': size})
        self.json_error('store', {'uid': self.uid, 'path': '/photounlim/1.jpg', 'md5': md5, 'sha256': sha256, 'size': size},
                        headers=self.mobile_headers, code=codes.FILE_EXISTS)
        self.json_error('info', {'uid': self.uid, 'path': '/photounlim/1.jpg'}, code=codes.RESOURCE_NOT_FOUND)

    def test_photounlim_does_not_uses_space(self):
        user_info_before = self.json_ok('user_info', {'uid': self.uid})
        self.upload_file(self.uid, '/photostream/1.jpg', headers=self.mobile_headers)
        user_info_after = self.json_ok('user_info', {'uid': self.uid})
        assert user_info_before['space'] == user_info_after['space']

    def test_photostream_store_file_bigger_than_limit(self):
        Quota().set_limit(1000, uid=self.uid)
        self.upload_file(self.uid, '/photostream/1.jpg', headers=self.mobile_headers, file_data={'size': 10000})

    def test_photounlim_store_file_bigger_than_limit(self):
        Quota().set_limit(1000, uid=self.uid)
        self.upload_file(self.uid, '/photounlim/1.jpg', headers=self.mobile_headers, file_data={'size': 10000})

    def test_photostream_store_file_in_overdraft(self):
        with mock.patch('mpfs.core.services.disk_service.MPFSStorageService.free', return_value=0):
            self.upload_file(self.uid, '/photostream/1.jpg', headers=self.mobile_headers, file_data={'size': 10000})

    def test_photounlim_store_file_in_overdraft(self):
        with mock.patch('mpfs.core.services.disk_service.MPFSStorageService.free', return_value=0):
            self.upload_file(self.uid, '/photounlim/1.jpg', headers=self.mobile_headers, file_data={'size': 10000})

    def test_store_photostream_mtime_ignored(self):
        self.upload_file(self.uid, '/photostream/1.jpg', headers=self.mobile_headers, file_data={'mtime': 10000})
        info = self.json_ok('info', {'uid': self.uid, 'path': '/photounlim/1.jpg'})
        assert info['mtime'] == info['utime']

    def test_store_photounlim_mtime_ignored(self):
        self.upload_file(self.uid, '/photounlim/1.jpg', headers=self.mobile_headers, file_data={'mtime': 10000})
        info = self.json_ok('info', {'uid': self.uid, 'path': '/photounlim/1.jpg'})
        assert info['mtime'] == info['utime']

    def test_photounlim_store_does_not_overwrite_files(self):
        self.upload_file(self.uid, '/photounlim/1.jpg', headers=self.mobile_headers, file_data={'size': 10000})
        self.upload_file(self.uid, '/photounlim/1.jpg', headers=self.mobile_headers, file_data={'size': 20000})
        resp = self.json_ok('list', {'uid': self.uid, 'path': '/photounlim'})
        assert len([x for x in resp if x['type'] == 'file']) == 2

    def test_uploading_file_to_photounlim_notifies_search_indexer(self):
        # Изменения любых изображений с etime (кроме png) отправляют изменения по дефолту, поэтому, проверяем на PNG
        file_data = {'mimetype': 'image/png'}
        with mock.patch('mpfs.core.job_handlers.indexer._notify_smartcache_worker', return_value=None) as notify_mock:
            self.upload_file(self.uid, '/photostream/1.jpg', file_data=file_data, headers=self.mobile_headers)
            assert notify_mock.called
        info = self.json_ok('info', {'uid': self.uid, 'path': '/photounlim/1.jpg', 'meta': ''})
        assert 'photoslice_time' in info['meta']

    def test_not_mobile_clients_cant_load_to_photounlim(self):
        self.upload_file(self.uid, '/photostream/1.jpg')
        self.json_error('info', {'uid': self.uid, 'path': '/photounlim/1.jpg'})

    def test_yandex_search_mobile_user_agent_can_upload_to_photounlim_for_user_in_overdraft(self):
        with mock.patch('mpfs.core.filesystem.resources.photounlim.PHOTOUNLIM_ALLOW_YANDEX_SEARCH_MOBILE', True), \
                mock.patch('mpfs.core.filesystem.resources.photounlim.PHOTOUNLIM_ALLOW_YANDEX_SEARCH_MOBILE_UIDS', []), \
                mock.patch('mpfs.core.services.disk_service.MPFSStorageService.used', return_value=12*GB), \
                mock.patch('mpfs.core.services.disk_service.MPFSStorageService.limit', return_value=10*GB), \
                mock.patch('mpfs.frontend.api.FEATURE_TOGGLES_DISABLE_API_FOR_OVERDRAFT_PERCENTAGE', 100):
            user_agent = 'Mozilla/5.0 (Linux; Android 5.1; LYO-L21 Build/HUAWEILYO-L21; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/50.0.2661.86 Mobile Safari/537.36 YandexSearch/7.60 (deviceId: b58b2bdee4c961517da2a0a3ef9d5da3)'
            self.upload_file(self.uid, '/photostream/1.jpg', headers={'user-agent': user_agent,
                                                                      'Yandex-Cloud-Request-ID': 'dav-test'})
            self.json_ok('info', {'uid': self.uid, 'path': '/photounlim/1.jpg'})

    def test_yandex_search_mobile_user_agent_can_upload_to_photounlim(self):
        with mock.patch('mpfs.core.filesystem.resources.photounlim.PHOTOUNLIM_ALLOW_YANDEX_SEARCH_MOBILE', True), \
                mock.patch('mpfs.core.filesystem.resources.photounlim.PHOTOUNLIM_ALLOW_YANDEX_SEARCH_MOBILE_UIDS', []), \
                mock.patch('mpfs.frontend.api.FEATURE_TOGGLES_DISABLE_API_FOR_OVERDRAFT_PERCENTAGE', 100):
            user_agent = 'Mozilla/5.0 (Linux; Android 5.1; LYO-L21 Build/HUAWEILYO-L21; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/50.0.2661.86 Mobile Safari/537.36 YandexSearch/7.60 (deviceId: b58b2bdee4c961517da2a0a3ef9d5da3)'
            experiment_manager.load_experiments_from_conf()
            self.upload_file(self.uid, '/photostream/1.jpg', headers={'user-agent': user_agent})
        experiment_manager.load_experiments_from_conf()
        self.json_ok('info', {'uid': self.uid, 'path': '/photounlim/1.jpg'})

    def test_yandex_search_mobile_user_agent_can_upload_to_photounlim_when_in_uid_list(self):
        with mock.patch('mpfs.core.filesystem.resources.photounlim.PHOTOUNLIM_ALLOW_YANDEX_SEARCH_MOBILE', False), \
                mock.patch('mpfs.core.filesystem.resources.photounlim.PHOTOUNLIM_ALLOW_YANDEX_SEARCH_MOBILE_UIDS', [self.uid]):
            user_agent = 'Mozilla/5.0 (Linux; Android 5.1; LYO-L21 Build/HUAWEILYO-L21; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/50.0.2661.86 Mobile Safari/537.36 YandexSearch/7.60 (deviceId: b58b2bdee4c961517da2a0a3ef9d5da3)'
            self.upload_file(self.uid, '/photostream/1.jpg', headers={'user-agent': user_agent})
            self.json_ok('info', {'uid': self.uid, 'path': '/photounlim/1.jpg'})

    def test_yandex_search_mobile_user_agent_can_not_upload_to_photounlim_with_disabeld_setting(self):
        with mock.patch('mpfs.core.filesystem.resources.photounlim.PHOTOUNLIM_ALLOW_YANDEX_SEARCH_MOBILE', False), \
                mock.patch('mpfs.core.filesystem.resources.photounlim.PHOTOUNLIM_ALLOW_YANDEX_SEARCH_MOBILE_UIDS', []):
            user_agent = 'Mozilla/5.0 (Linux; Android 5.1; LYO-L21 Build/HUAWEILYO-L21; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/50.0.2661.86 Mobile Safari/537.36 YandexSearch/7.60 (deviceId: b58b2bdee4c961517da2a0a3ef9d5da3)'
            self.upload_file(self.uid, '/photostream/1.jpg', headers={'user-agent': user_agent})
            self.json_error('info', {'uid': self.uid, 'path': '/photounlim/1.jpg'})

    def test_yandex_search_mobile_user_agent_can_not_upload_to_photounlim_when_not_in_uid_list(self):
        with mock.patch('mpfs.core.filesystem.resources.photounlim.PHOTOUNLIM_ALLOW_YANDEX_SEARCH_MOBILE', False), \
                mock.patch('mpfs.core.filesystem.resources.photounlim.PHOTOUNLIM_ALLOW_YANDEX_SEARCH_MOBILE_UIDS', ['123456']):
            user_agent = 'Mozilla/5.0 (Linux; Android 5.1; LYO-L21 Build/HUAWEILYO-L21; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/50.0.2661.86 Mobile Safari/537.36 YandexSearch/7.60 (deviceId: b58b2bdee4c961517da2a0a3ef9d5da3)'
            self.upload_file(self.uid, '/photostream/1.jpg', headers={'user-agent': user_agent})
            self.json_error('info', {'uid': self.uid, 'path': '/photounlim/1.jpg'})

    def test_chrome_user_agent_can_not_upload_to_photounlim(self):
        with mock.patch('mpfs.core.filesystem.resources.photounlim.PHOTOUNLIM_ALLOW_YANDEX_SEARCH_MOBILE', True), \
                mock.patch('mpfs.core.filesystem.resources.photounlim.PHOTOUNLIM_ALLOW_YANDEX_SEARCH_MOBILE_UIDS', []):
            user_agent = 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_12_6) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/68.0.3440.42 Safari/537.36'
            self.upload_file(self.uid, '/photostream/1.jpg', headers={'user-agent': user_agent})
            self.json_error('info', {'uid': self.uid, 'path': '/photounlim/1.jpg'})

    def test_return_photounlim_storage_type(self):
        self.upload_file(self.uid, '/photostream/1.jpg', headers=self.mobile_headers)
        resp = self.json_ok('info', {'uid': self.uid, 'path': '/photounlim/1.jpg', 'meta': ''})
        assert resp['meta']['storage_type'] == PHOTOUNLIM_AREA

        res = PhotounlimDAO().find_one({'uid': self.uid, 'key': '/photounlim/1.jpg'})
        assert 'storage_type' not in res['data']

    def test_lenta_block_list_photostream_order(self):
        correct_order_1 = ['photounlim', '3.jpg', '2.jpg']
        correct_order_2 = ['photounlim', '1.jpg', '0.jpg']
        self.upload_file(self.uid, '/photostream/0.jpg', headers=self.mobile_headers, file_data={'etime': '2017-01-01T10:10:00Z'})
        self.upload_file(self.uid, '/photostream/3.jpg', headers=self.mobile_headers, file_data={'etime': '2017-01-01T10:10:03Z'})
        self.upload_file(self.uid, '/photostream/1.jpg', headers=self.mobile_headers, file_data={'etime': '2017-01-01T10:10:01Z'})
        self.upload_file(self.uid, '/photostream/2.jpg', headers=self.mobile_headers, file_data={'etime': '2017-01-01T10:10:02Z'})

        resource_id = self.json_ok('info', {'uid': self.uid, 'meta': 'resource_id', 'path': '/photounlim'})['meta']['resource_id']
        resp = self.json_ok('lenta_block_list', {'uid': self.uid, 'resource_id': resource_id, 'mtime_gte': 0, 'amount': 2})
        assert [i['name'] for i in resp] == correct_order_1
        resp = self.json_ok('lenta_block_list', {'uid': self.uid, 'resource_id': resource_id, 'mtime_gte': 0, 'amount': 2, 'offset': 2})
        assert [i['name'] for i in resp] == correct_order_2

    def test_copy_from_photounlim_to_disk(self):
        self.upload_file(self.uid, '/photostream/1.jpg', headers=self.mobile_headers)
        self.json_ok('copy', {'uid': self.uid, 'src': '/photounlim/1.jpg', 'dst': '/disk/1.jpg'})
        self.json_ok('info', {'uid': self.uid, 'path': '/disk/1.jpg'})

    def test_async_copy_from_photounlim_to_disk(self):
        self.upload_file(self.uid, '/photounlim/1.jpg', headers=self.mobile_headers)
        self.json_ok('async_copy', {'uid': self.uid, 'src': '/photounlim/1.jpg', 'dst': '/disk/1.jpg'})
        self.json_ok('info', {'uid': self.uid, 'path': '/disk/1.jpg'})

    def test_copy_from_disk_to_photounlim_forbidden(self):
        self.upload_file(self.uid, '/disk/1.jpg')
        self.json_error('copy', {'uid': self.uid, 'src': '/disk/1.jpg', 'dst': '/photounlim/1.jpg'})
        self.json_error('info', {'uid': self.uid, 'path': '/photounlim/1.jpg'})

    def test_async_copy_from_disk_to_photounlim_forbidden(self):
        self.upload_file(self.uid, '/disk/1.jpg')
        self.json_error('async_copy', {'uid': self.uid, 'src': '/disk/1.jpg', 'dst': '/photounlim/1.jpg'})
        self.json_error('info', {'uid': self.uid, 'path': '/photounlim/1.jpg'})

    def test_trash_append_from_photounlim_to_trash(self):
        self.upload_file(self.uid, '/photostream/1.jpg', headers=self.mobile_headers)
        trash_path = self.json_ok('trash_append', {'uid': self.uid, 'path': '/photounlim/1.jpg'})['this']['id']
        self.json_error('info', {'uid': self.uid, 'path': '/photounlim/1.jpg'})
        self.json_ok('info', {'uid': self.uid, 'path': trash_path})

    def test_async_trash_append_from_photounlim_to_trash(self):
        self.upload_file(self.uid, '/photostream/1.jpg', headers=self.mobile_headers)
        with mock.patch('mpfs.core.address.Address.add_trash_suffix'):
            self.json_ok('async_trash_append', {'uid': self.uid, 'path': '/photounlim/1.jpg'})
        self.json_error('info', {'uid': self.uid, 'path': '/photounlim/1.jpg'})
        self.json_ok('info', {'uid': self.uid, 'path': '/trash/1.jpg'})

    def test_trash_restore_photounlim(self):
        self.upload_file(self.uid, '/photostream/1.jpg', headers=self.mobile_headers)
        trash_path = self.json_ok('trash_append', {'uid': self.uid, 'path': '/photounlim/1.jpg'})['this']['id']
        self.json_error('info', {'uid': self.uid, 'path': '/photounlim/1.jpg'})
        self.json_ok('info', {'uid': self.uid, 'path': trash_path})
        self.json_ok('trash_restore', {'uid': self.uid, 'path': trash_path})
        self.json_ok('info', {'uid': self.uid, 'path': '/photounlim/1.jpg'})

    def test_async_trash_restore_photounlim(self):
        self.upload_file(self.uid, '/photostream/1.jpg', headers=self.mobile_headers)
        with mock.patch('mpfs.core.address.Address.add_trash_suffix'):
            self.json_ok('async_trash_append', {'uid': self.uid, 'path': '/photounlim/1.jpg'})
        self.json_error('info', {'uid': self.uid, 'path': '/photounlim/1.jpg'})
        self.json_ok('info', {'uid': self.uid, 'path': '/trash/1.jpg'})
        self.json_ok('async_trash_restore', {'uid': self.uid, 'path': '/trash/1.jpg'})
        self.json_ok('info', {'uid': self.uid, 'path': '/photounlim/1.jpg'})

    def test_rm_from_photounlim(self):
        self.upload_file(self.uid, '/photostream/1.jpg', headers=self.mobile_headers)
        self.json_ok('rm', {'uid': self.uid, 'path': '/photounlim/1.jpg'})
        self.json_error('info', {'uid': self.uid, 'path': '/photounlim/1.jpg'})
        self.json_error('info', {'uid': self.uid, 'path': '/trash/1.jpg'})

    def test_async_rm_from_photounlim(self):
        self.upload_file(self.uid, '/photostream/1.jpg', headers=self.mobile_headers)
        self.json_ok('async_rm', {'uid': self.uid, 'path': '/photounlim/1.jpg'})
        self.json_error('info', {'uid': self.uid, 'path': '/photounlim/1.jpg'})
        self.json_error('info', {'uid': self.uid, 'path': '/trash/1.jpg'})

    def test_publish_photounlim_files(self):
        self.upload_file(self.uid, '/photostream/1.jpg', headers=self.mobile_headers)
        result = self.json_ok('set_public', {'uid': self.uid, 'path': '/photounlim/1.jpg'})
        self.json_ok('public_info', {'private_hash': result['hash']})

    def test_move_from_disk_to_photounlim_forbidden(self):
        self.upload_file(self.uid, '/disk/1.jpg', headers=self.mobile_headers)
        self.json_error('move', {'uid': self.uid, 'dst': '/photounlim/1.jpg', 'src': '/disk/1.jpg'})
        self.json_error('info', {'uid': self.uid, 'path': '/photounlim/1.jpg'})

    def test_async_move_from_disk_to_photounlim_forbidden(self):
        self.upload_file(self.uid, '/disk/1.jpg', headers=self.mobile_headers)
        self.json_error('async_move', {'uid': self.uid, 'dst': '/photounlim/1.jpg', 'src': '/disk/1.jpg'})
        self.json_error('info', {'uid': self.uid, 'path': '/photounlim/1.jpg'})

    def test_async_move_from_photounlim_to_disk(self):
        self.upload_file(self.uid, '/photounlim/1.jpg', headers=self.mobile_headers)
        self.json_ok('async_move', {'uid': self.uid, 'src': '/photounlim/1.jpg', 'dst': '/disk/1.jpg'})
        self.json_ok('info', {'uid': self.uid, 'path': '/disk/1.jpg'})

    @parameterized.expand([
        ('move',),
        ('async_move',),
    ])
    def test_renaming_in_photounlim_enabled(self, endpoint):
        self.upload_file(self.uid, '/photounlim/1.jpg', headers=self.mobile_headers)
        self.json_ok(endpoint, {'uid': self.uid, 'src': '/photounlim/1.jpg', 'dst': '/photounlim/2.jpg'})
        self.json_ok('info', {'uid': self.uid, 'path': '/photounlim/2.jpg'})

    def test_move_from_trash_to_photounlim_forbidden(self):
        self.upload_file(self.uid, '/disk/1.jpg')
        self.json_ok('trash_append', {'uid': self.uid, 'path': '/disk/1.jpg'})
        self.json_error('move', {'uid': self.uid, 'src': '/trash/1.jpg', 'dst': '/photounlim/1.jpg'})
        self.json_error('info', {'uid': self.uid, 'path': '/photounlim/1.jpg'})

    def test_async_move_from_trash_to_photounlim_forbidden(self):
        self.upload_file(self.uid, '/disk/1.jpg')
        self.json_ok('async_trash_append', {'uid': self.uid, 'path': '/disk/1.jpg'})
        self.json_error('async_move', {'uid': self.uid, 'src': '/trash/1.jpg', 'dst': '/photounlim/1.jpg'})
        self.json_error('info', {'uid': self.uid, 'path': '/photounlim/1.jpg'})

    def test_resource_id_is_set_for_root_photounlim_resource(self):
        resp = self.json_ok('info', {'uid': self.uid, 'path': '/photounlim', 'meta': 'resource_id'})
        assert resp['meta']['resource_id']

    def test_info_by_file_id(self):
        self.upload_file(self.uid, '/photounlim/1.jpg')
        resp = self.json_ok('info', {'uid': self.uid, 'path': '/photounlim/1.jpg', 'meta': 'file_id'})
        self.json_ok('info_by_file_id', {'uid': self.uid, 'file_id': resp['meta']['file_id']})

    def test_info_by_resource_id(self):
        self.upload_file(self.uid, '/photounlim/1.jpg')
        resp = self.json_ok('info', {'uid': self.uid, 'path': '/photounlim/1.jpg', 'meta': 'file_id'})
        self.json_ok('info_by_resource_id', {'uid': self.uid, 'resource_id': self.uid + ':' + resp['meta']['file_id']})

    def test_new_search(self):
        self.upload_file(self.uid, '/photostream/my_file.jpg', headers=self.mobile_headers)
        with DiskSearchSmartMockHelper.mock():
            DiskSearchSmartMockHelper.add_to_index(self.uid, '/photounlim/my_file.jpg')
            response = self.json_ok('new_search', {'uid': self.uid, 'path': '/disk', 'query': 'my_file'})
            assert len([x for x in response['results'] if x['type'] == 'file']) == 1
            response = self.json_ok('new_search', {'uid': self.uid, 'path': '/trash', 'query': 'my_file'})
            assert len([x for x in response['results'] if x['type'] == 'file']) == 0

    def test_user_cannot_see_files_of_others(self):
        self.json_ok('user_init', {'uid': self.user_1.uid})
        self.json_ok('enable_unlimited_autouploading', {'uid': self.user_1.uid})

        self.upload_file(self.uid, '/photounlim/0.jpg')
        self.upload_file(self.user_1.uid, '/photounlim/1.jpg')

        self.json_error('lenta_block_list', {'uid': self.user_1.uid, 'resource_id': self.uid + ':/photounlim', 'mtime_gte': 0}, code=codes.RESOURCE_NOT_FOUND)

        resource_id = self.json_ok('info', {'uid': self.uid, 'path': '/photounlim', 'meta': 'resource_id'})['meta']['resource_id']
        self.json_error('lenta_block_list', {'uid': self.user_1.uid, 'resource_id': resource_id, 'mtime_gte': 0}, code=codes.RESOURCE_NOT_FOUND)

    def test_forbid_to_load_non_photo_and_video_to_photounlim(self):
        self.upload_file(self.uid, '/photostream/1.jpg', headers=self.mobile_headers)
        self.json_ok('info', {'uid': self.uid, 'path': '/photounlim/1.jpg', 'meta': 'file_id'})

        self.upload_file(self.uid, '/photostream/2.mp4', media_type='video', headers=self.mobile_headers)
        self.json_ok('info', {'uid': self.uid, 'path': '/photounlim/2.mp4', 'meta': 'file_id'})

        self.upload_file(self.uid, '/photostream/3.docx', headers=self.mobile_headers)
        self.json_error('info', {'uid': self.uid, 'path': '/photounlim/3.docx', 'meta': 'file_id'})
        self.json_ok('info', {'uid': self.uid, 'path': '/disk/Фотокамера/3.docx', 'meta': 'file_id'})

    def test_astore_works_properly_for_photounlim(self):
        rand = str('%f' % time.time()).replace('.', '')[9:]
        file_md5 = hashlib.md5(rand).hexdigest()
        file_sha256 = hashlib.sha256(rand).hexdigest()

        self.json_ok('store',
                     {'uid': self.uid, 'path': '/photostream/1.jpg', 'size': 1, 'md5': file_md5, 'sha256': file_sha256},
                     headers=self.mobile_headers)
        self.json_ok('astore', {'uid': self.uid, 'path': '/photostream/1.jpg', 'md5': file_md5},
                     headers=self.mobile_headers)

    def test_bulk_download_files_from_photounlim(self):
        self.upload_file(self.uid, '/photostream/1.jpg', headers=self.mobile_headers)
        self.upload_file(self.uid, '/photostream/2.jpg', headers=self.mobile_headers)

        opts = {'uid': self.uid}
        json = {
            'items': ['/photounlim/1.jpg', '/photounlim/2.jpg']
        }

        oid = self.json_ok('bulk_download_prepare', opts=opts, json=json)['oid']
        result = self.json_ok('bulk_download_list', opts={'uid': self.uid, 'oid': oid})
        assert result['list']

    def test_autouploading_flag_is_set(self):
        self.upload_file(self.uid, '/photostream/1.jpg')

        info = self.json_ok('info', {'uid': self.uid, 'path': '/disk/Фотокамера/1.jpg', 'meta': ''})
        assert info['meta']['autouploaded'] == True

        listing = self.json_ok('list', {'uid': self.uid, 'path': '/disk/Фотокамера', 'meta': ''})

        assert all(f['meta']['autouploaded'] for f in listing if f['type'] == 'file')

        self.upload_file(self.uid, '/photostream/3.jpg', headers=self.mobile_headers)

        info = self.json_ok('info', {'uid': self.uid, 'path': '/photounlim/3.jpg', 'meta': ''})
        assert info['meta']['autouploaded'] == True

        listing = self.json_ok('list', {'uid': self.uid, 'path': '/photounlim', 'meta': ''})

        assert all(f['meta']['autouploaded'] for f in listing if f['type'] == 'file')

    def test_rm_from_photounlim_sends_resource_to_hidden_data(self):
        self.upload_file(self.uid, '/photounlim/1.jpg')
        self.json_ok('rm', {'uid': self.uid, 'path': '/photounlim/1.jpg'})
        assert Bus().get_resource(self.uid, Address.Make(self.uid, '/hidden')).list()['list']

    def test_overwrite_in_photounlim_sends_resource_to_hidden_data(self):
        self.upload_file(self.uid, '/photounlim/1.jpg')
        self.upload_file(self.uid, '/photounlim/2.jpg')
        self.json_ok('move', {'uid': self.uid, 'src': '/photounlim/1.jpg', 'dst': '/photounlim/2.jpg', 'force': 1})
        assert Bus().get_resource(self.uid, Address.Make(self.uid, '/hidden')).list()['list']

    @parameterized.expand([
        ('photounlim',
         u'/photostream/first.jpg', u'/photounlim/first.jpg',
         u'/photostream/second.jpg', u'/photounlim/first.jpg',
         True),
        ('camera_uploads',
         u'/photostream/first.jpg', u'/disk/Фотокамера/first.jpg',
         u'/photostream/second.jpg', u'/disk/Фотокамера/first.jpg',
         False,),
        ('photounlim_same',
         u'/photostream/first.jpg', u'/photounlim/first.jpg',
         u'/photostream/first.jpg', u'/photounlim/first.jpg',
         True),
        ('camera_uploads_same',
         u'/photostream/first.jpg', u'/disk/Фотокамера/first.jpg',
         u'/photostream/first.jpg', u'/disk/Фотокамера/first.jpg',
         False),
    ])
    def test_store_with_if_match(self, case_name, orig_path, orig_resolved_path, path, resolved_path, is_photounlim_enabled):
        prev_mtime = 7777
        user = User(self.uid)
        if not is_photounlim_enabled:
            user.disable_unlimited_autouploading()

        self.upload_file(self.uid, orig_path, headers=self.mobile_headers,
                         opts={'photostream_destination': 'limit'})

        # выставляем mtime, чтобы потом сравнивать с ним
        res = list(ResourceDAO().find({'uid': self.uid, 'path': orig_resolved_path}))[0]
        res['data']['mtime'] = prev_mtime
        res['parent'] = orig_resolved_path.rsplit('/', 1)[0]
        ResourceDAO().update({'uid': self.uid, 'path': orig_resolved_path}, res, upsert=True)

        info = self.json_ok('info', {'uid': self.uid, 'path': orig_resolved_path, 'meta': ''})
        prev_resource_id = info['meta']['resource_id']

        rand = str('%f' % time.time()).replace('.', '')[9:]
        file_md5 = hashlib.md5(rand).hexdigest()
        file_sha256 = hashlib.sha256(rand).hexdigest()

        resp = self.json_ok('store',
                            {'uid': self.uid, 'path': path, 'size': 1, 'md5': file_md5, 'sha256': file_sha256,
                             'replace_md5': 'md5:%s,sha256:%s,size:%s' % (info['meta']['md5'],
                                                                          info['meta']['sha256'],
                                                                          info['meta']['size']),
                             'force': '1', 'photostream_destination': 'limit'},
                            headers=self.mobile_headers)

        kladun_callback_params = {
            'uid': self.uid,
            'md5': file_md5,
            'sha256': file_sha256,
            'size': 1,
            'path': path,
        }
        body_1, body_2, body_3 = self.prepare_kladun_callbacks(**kladun_callback_params)
        oid = resp['oid']
        with KladunStub(status_values=(body_1, body_2, body_3)):
            opts = {
                'uid': self.uid,
                'oid': oid,
                'status_xml': etree.tostring(body_1),
                'type': COMMIT_FILE_INFO,
            }
            self.service_ok('kladun_callback', opts)
            opts = {
                'uid': self.uid,
                'oid': oid,
                'status_xml': etree.tostring(body_2),
                'type': COMMIT_FILE_UPLOAD,
            }
            self.service_ok('kladun_callback', opts)
            opts = {
                'uid': self.uid,
                'oid': oid,
                'status_xml': etree.tostring(body_3),
                'type': COMMIT_FINAL,
            }
            self.service_ok('kladun_callback', opts)

        info = self.json_ok('info', {'uid': self.uid, 'path': resolved_path, 'meta': ''})
        current_resource_id = info['meta']['resource_id']

        assert current_resource_id == prev_resource_id
        assert info['mtime'] == prev_mtime

    def test_store_with_if_match_with_when_changed_autoupload(self):
        user = User(self.uid)
        user.disable_unlimited_autouploading()

        self.upload_file(self.uid, '/photostream/1.jpg', headers=self.mobile_headers, opts={'photostream_destination': 'limit'})
        info = self.json_ok('info', {'uid': self.uid, 'path': u'/disk/Фотокамера/1.jpg', 'meta': ''})['meta']

        rand = str('%f' % time.time()).replace('.', '')[9:]
        file_md5 = hashlib.md5(rand).hexdigest()
        file_sha256 = hashlib.sha256(rand).hexdigest()

        user.enable_unlimited_autouploading()
        resp = self.json_ok('store',
                            {'uid': self.uid, 'path': u'/photostream/1.jpg', 'size': 1, 'md5': file_md5, 'sha256': file_sha256,
                             'replace_md5': 'md5:%s,sha256:%s,size:%s' % (info['md5'], info['sha256'], info['size']),
                             'force': '1', 'photostream_destination': 'photounlim'},
                            headers=self.mobile_headers)

        kladun_callback_params = {
            'uid': self.uid,
            'md5': file_md5,
            'sha256': file_sha256,
            'size': 1,
            'path': u'/photostream/1.jpg',
        }
        body_1, body_2, body_3 = self.prepare_kladun_callbacks(**kladun_callback_params)
        oid = resp['oid']
        with KladunStub(status_values=(body_1, body_2, body_3)):
            opts = {
                'uid': self.uid,
                'oid': oid,
                'status_xml': etree.tostring(body_1),
                'type': COMMIT_FILE_INFO,
            }
            self.service_ok('kladun_callback', opts)
            opts = {
                'uid': self.uid,
                'oid': oid,
                'status_xml': etree.tostring(body_2),
                'type': COMMIT_FILE_UPLOAD,
            }
            self.service_ok('kladun_callback', opts)
            opts = {
                'uid': self.uid,
                'oid': oid,
                'status_xml': etree.tostring(body_3),
                'type': COMMIT_FINAL,
            }
            self.service_ok('kladun_callback', opts)

        camera_dir = self.json_ok('list', {'uid': self.uid, 'path': u'/disk/Фотокамера'})
        assert len(camera_dir) == 2
        photounlim_dir = self.json_ok('list', {'uid': self.uid, 'path': u'/photounlim'})
        assert len(photounlim_dir) == 1
        operation_status = self.json_ok('status', {'uid': self.uid, 'oid': oid})
        assert operation_status['status'] == 'DONE'

    def test_store_with_if_match_with_bug_when_changed_autoupload_reverse(self):
        user = User(self.uid)
        user.enable_unlimited_autouploading()

        self.upload_file(self.uid, '/photostream/1.jpg', headers=self.mobile_headers, opts={'photostream_destination': 'photounlim'})
        info = self.json_ok('info', {'uid': self.uid, 'path': u'/photounlim/1.jpg', 'meta': ''})['meta']

        rand = str('%f' % time.time()).replace('.', '')[9:]
        file_md5 = hashlib.md5(rand).hexdigest()
        file_sha256 = hashlib.sha256(rand).hexdigest()

        user.disable_unlimited_autouploading()
        resp = self.json_ok('store',
                            {'uid': self.uid, 'path': u'/photostream/1.jpg', 'size': 1, 'md5': file_md5, 'sha256': file_sha256,
                             'replace_md5': 'md5:%s,sha256:%s,size:%s' % (info['md5'], info['sha256'], info['size']),
                             'force': '1', 'photostream_destination': 'limit'},
                            headers=self.mobile_headers)

        kladun_callback_params = {
            'uid': self.uid,
            'md5': file_md5,
            'sha256': file_sha256,
            'size': 1,
            'path': u'/photounlim/1.jpg',
        }
        body_1, body_2, body_3 = self.prepare_kladun_callbacks(**kladun_callback_params)
        oid = resp['oid']
        with KladunStub(status_values=(body_1, body_2, body_3)):
            opts = {
                'uid': self.uid,
                'oid': oid,
                'status_xml': etree.tostring(body_1),
                'type': COMMIT_FILE_INFO,
            }
            self.service_ok('kladun_callback', opts)
            opts = {
                'uid': self.uid,
                'oid': oid,
                'status_xml': etree.tostring(body_2),
                'type': COMMIT_FILE_UPLOAD,
            }
            self.service_ok('kladun_callback', opts)
            opts = {
                'uid': self.uid,
                'oid': oid,
                'status_xml': etree.tostring(body_3),
                'type': COMMIT_FINAL,
            }
            self.service_ok('kladun_callback', opts)

        self.json_error('list', {'uid': self.uid, 'path': u'/disk/Фотокамера'})
        photounlim_dir = self.json_ok('list', {'uid': self.uid, 'path': u'/photounlim'})
        assert len(photounlim_dir) == 2
        operation_status = self.json_ok('status', {'uid': self.uid, 'oid': oid})
        assert operation_status['status'] == 'DONE'

    @parameterized.expand([
        ('same', u'/photostream/first.jpg', u'/disk/Фотокамера/first.jpg', u'/photostream/first.jpg'),
        ('different', u'/photostream/first.jpg', u'/disk/Фотокамера/first.jpg', u'/photostream/second.jpg')
    ])
    def test_astore_for_store_with_if_match(self, case_name, first_path, first_resolved_path, second_path):
        user = User(self.uid)
        user.disable_unlimited_autouploading()

        self.upload_file(self.uid, first_path, headers=self.mobile_headers, opts={'photostream_destination': 'limit'})
        info = self.json_ok('info', {'uid': self.uid, 'path': first_resolved_path, 'meta': ''})['meta']

        rand = str('%f' % time.time()).replace('.', '')[9:]
        file_md5 = hashlib.md5(rand).hexdigest()
        file_sha256 = hashlib.sha256(rand).hexdigest()

        self.json_ok('store',
                     {'uid': self.uid, 'path': second_path, 'size': 1, 'md5': file_md5, 'sha256': file_sha256,
                      'replace_md5': 'md5:%s,sha256:%s,size:%s' % (info['md5'], info['sha256'], info['size']),
                      'force': '1', 'photostream_destination': 'limit'},
                     headers=self.mobile_headers)

        # ожидаем что найдем операцию загрузки
        self.json_ok('astore',
                     {'uid': self.uid, 'path': second_path, 'md5': file_md5},
                     headers=self.mobile_headers)


class MovePhotounlimMoveToDiskTestCase(CommonSharingMethods):
    def setup_method(self, method):
        super(MovePhotounlimMoveToDiskTestCase, self).setup_method(method)
        self.json_ok('enable_unlimited_autouploading', {'uid': self.uid})

    def test_space_limit_on_move_photounlim_to_disk_ok(self):
        file_size = 5 * MB

        self.upload_file(self.uid, '/photounlim/1.jpg', file_data={'size': file_size})

        Quota().set_limit(file_size + 1, uid=self.uid)

        result = self.json_ok('async_move', {'uid': self.uid, 'src': '/photounlim/1.jpg', 'dst': '/disk/1.jpg'})
        status = self.json_ok('status', {'uid': self.uid, 'oid': result['oid']})

        assert status['status'] == true_operation_titles[codes.DONE]
        self.json_ok('info', {'uid': self.uid, 'path': '/disk/1.jpg'})

    def test_space_limit_on_move_photounlim_to_disk_failed(self):
        file_size = 5 * MB

        self.upload_file(self.uid, '/photounlim/1.jpg', file_data={'size': file_size})

        Quota().set_limit(file_size - 1, uid=self.uid)

        result = self.json_ok('async_move', {'uid': self.uid, 'src': '/photounlim/1.jpg', 'dst': '/disk/1.jpg'})
        status = self.json_ok('status', {'uid': self.uid, 'oid': result['oid']})

        assert status['status'] == true_operation_titles[codes.FAILED]
        assert status['error']['code'] == codes.NO_FREE_SPACE
        self.json_error('info', {'uid': self.uid, 'path': '/disk/1.jpg'}, code=codes.RESOURCE_NOT_FOUND)

    def test_space_limit_on_move_photounlim_to_disk_into_shared_folder_ok(self):
        self.create_user(self.uid_1)
        self.json_ok('mkdir', {'uid': self.uid_1, 'path': '/disk/shared'})
        self.create_group(self.uid_1, '/disk/shared')
        invite_hash = self.invite_user(uid=self.uid, owner=self.uid_1, email=self.email, path='/disk/shared')
        self.activate_invite(uid=self.uid, hash=invite_hash)

        file_size = 5 * MB

        self.upload_file(self.uid, '/photounlim/1.jpg', file_data={'size': file_size})

        Quota().set_limit(file_size - 1, uid=self.uid)
        Quota().set_limit(file_size + 1, uid=self.uid_1)

        result = self.json_ok('async_move', {'uid': self.uid, 'src': '/photounlim/1.jpg', 'dst': '/disk/shared/1.jpg'})
        status = self.json_ok('status', {'uid': self.uid, 'oid': result['oid']})

        assert status['status'] == true_operation_titles[codes.DONE]
        self.json_ok('info', {'uid': self.uid, 'path': '/disk/shared/1.jpg'})

    def test_space_limit_on_move_photounlim_to_disk_into_shared_folder_failed(self):
        self.create_user(self.uid_1)
        self.json_ok('mkdir', {'uid': self.uid_1, 'path': '/disk/shared'})
        self.create_group(self.uid_1, '/disk/shared')
        invite_hash = self.invite_user(uid=self.uid, owner=self.uid_1, email=self.email, path='/disk/shared')
        self.activate_invite(uid=self.uid, hash=invite_hash)

        file_size = 5 * MB

        self.upload_file(self.uid, '/photounlim/1.jpg', file_data={'size': file_size})

        Quota().set_limit(file_size - 1, uid=self.uid)
        Quota().set_limit(file_size - 1, uid=self.uid_1)

        result = self.json_ok('async_move', {'uid': self.uid, 'src': '/photounlim/1.jpg', 'dst': '/disk/shared/1.jpg'})
        status = self.json_ok('status', {'uid': self.uid, 'oid': result['oid']})

        assert status['status'] == true_operation_titles[codes.FAILED]
        assert status['error']['code'] == codes.OWNER_HAS_NO_FREE_SPACE
        self.json_error('info', {'uid': self.uid, 'path': '/disk/shared/1.jpg'}, code=codes.RESOURCE_NOT_FOUND)
