# -*- coding: utf-8 -*-
import mock
import pytest
from hamcrest import assert_that, empty, has_entry, is_
from contextlib import contextmanager

from test.base import DiskTestCase
from test.conftest import capture_queue_errors
from test.helpers.stubs.manager import StubsManager
from test.helpers.stubs.services import SearchIndexerStub

from mpfs.config import settings
from mpfs.core.filesystem.indexer import DiskDataIndexer
from mpfs.core.queue import mpfs_queue


class IndexerMillTestCase(DiskTestCase):
    cumulative_limit = settings.indexer['cumulative_operations_items_limit']
    overall_limit = settings.indexer['overall_operations_items_limit']

    @contextmanager
    def patch_mpfs_queue(self):
        self._mpfs_queue_put = list()
        def my_put(*args):
            self._mpfs_queue_put.append(args)
        original_mpfs_queue_put, mpfs_queue.put = mpfs_queue.put, my_put
        try:
            yield
        finally:
            mpfs_queue.put = original_mpfs_queue_put
            self._mpfs_queue_put = list()

    def setup_method(self, method):
        super(IndexerMillTestCase, self).setup_method(method)

    def test_cumulative(self):
        with self.patch_mpfs_queue():
            indexer = DiskDataIndexer()
            num_chunks = 3
            for i in xrange((self.cumulative_limit + 1) * num_chunks):
                indexer.data['search'].append(i)
                indexer.group_data['666']['search'].append(i)
                indexer.indexer_data_mill()
            assert len(self._mpfs_queue_put) == num_chunks * 2
            # в каждом чанке по cumulative_limit данных
            for data, _ in self._mpfs_queue_put:
                assert len(data['data']) == self.cumulative_limit + 1

    def test_overall(self):
        with self.patch_mpfs_queue():
            indexer = DiskDataIndexer()
            num_chunks = 2
            for i in xrange(self.overall_limit * num_chunks):
                indexer.data['search'].append(i)
                indexer.group_data['666']['search'].append(i)
            indexer.indexer_data_mill()
            assert len(self._mpfs_queue_put) == num_chunks * 2
            for data, _ in self._mpfs_queue_put:
                assert len(data['data']) == self.overall_limit


class SearchIndexerTestCase(DiskTestCase):
    stubs_manager = StubsManager(class_stubs=set(StubsManager.DEFAULT_CLASS_STUBS) - {SearchIndexerStub})

    def test_wait_url_param(self):
        # обычный файл
        with SearchIndexerStub() as stub:
            self.upload_file(self.uid, '/disk/1.txt')
            push_kwargs = stub.push_change.call_args[1]
        assert_that(push_kwargs, has_entry('wait_search_response', False))

        # фотосрезный файл
        with SearchIndexerStub() as stub:
            self.upload_file(self.uid, '/disk/1.jpg',
                             file_data={'mimetype': 'image/jpeg', 'etime': '2012-04-05T10:00:00Z'})
            push_kwargs = stub.push_change.call_args[1]
        assert_that(push_kwargs, has_entry('wait_search_response', True))

    @pytest.mark.skipif(True, reason='https://st.yandex-team.ru/CHEMODAN-74846')
    def test_async_user_remove_does_not_lead_to_error(self):
        self.upload_file(self.uid, '/disk/disk_file')
        self.upload_file(self.uid, '/attach/attach_file')
        self.upload_file(self.uid, '/disk/trash_file')
        self.json_ok('async_trash_append', {'uid': self.uid, 'path': '/disk/trash_file'})

        with capture_queue_errors() as errors:
            self.service_ok('async_user_remove', {'uid': self.uid})

        assert_that(errors, is_(empty()))

    def test_deep_resource_not_sent_to_indexer(self):
        folder_path = '/disk/top-folder'
        folder_src_path = folder_path
        folder_dst_path = '/disk/top-folder-new'

        self.json_ok('mkdir', {'uid': self.uid, 'path': folder_path})

        current_level = 2
        max_level = 20
        for i in xrange(current_level, max_level + 1):
            folder_path += '/subfolder'
            self.json_ok('mkdir', {'uid': self.uid, 'path': folder_path})

        file_path = folder_path + '/file.txt'
        self.upload_file(self.uid, file_path)

        with SearchIndexerStub() as stub, \
                mock.patch('mpfs.core.filesystem.indexer.SERVICES_SEARCH_INDEXER_ENABLE_LIMIT_FOR_DEEP_FOLDERS', True), \
                mock.patch('mpfs.core.filesystem.indexer.SERVICES_SEARCH_INDEXER_MAX_RESOURCE_DEPTH_FOR_INDEXER', max_level):
            self.json_ok('copy', {'uid': self.uid, 'src': folder_src_path, 'dst': folder_dst_path})

            for args, kwargs in stub.push_change.call_args_list:
                data = args[0]
                if not isinstance(data, list):
                    data = [data]

                for item in data:
                    folder_level = item['id'].count('/')
                    assert folder_level < max_level

    def test_djfs_callbacks_to_indexer_on_upload_file(self):
        with mock.patch('mpfs.core.filesystem.indexer.SERVICES_SEARCH_INDEXER_SEND_DJFS_CALLBACKS_ON_STORE', True), \
                SearchIndexerStub() as stub:
            self.upload_file(self.uid, '/disk/test.jpg')

            stub.push_change.assert_called_once()
            args, _ = stub.push_change.call_args
            data = args[0][0]
            assert data.get('append_djfs_callbacks')

    def test_djfs_callbacks_sent_to_indexer_on_upload_with_hardlink(self):
        self.upload_file(self.uid, '/disk/test.jpg')
        info_result = self.json_ok('info', {'uid': self.uid, 'path': '/disk/test.jpg', 'meta': ''})
        file_data = {
            'md5': info_result['meta']['md5'],
            'size': info_result['meta']['size'],
            'sha256': info_result['meta']['sha256'],
        }

        with mock.patch('mpfs.core.filesystem.indexer.SERVICES_SEARCH_INDEXER_SEND_DJFS_CALLBACKS_ON_STORE', True), \
                SearchIndexerStub() as stub:
            self.upload_file(self.uid, '/disk/test_2.jpg', file_data=file_data, hardlink=True)

            stub.push_change.assert_called_once()
            args, _ = stub.push_change.call_args
            data = args[0][0]
            assert data.get('append_djfs_callbacks')
