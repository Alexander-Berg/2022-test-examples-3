# -*- coding: utf-8 -*-
from datetime import date, timedelta
from os import path
from time import sleep

import before_after
import math
import mock
import pytest
from hamcrest import (
    assert_that,
    has_item,
    has_entry,
    has_entries,
    is_not,
    equal_to,
    starts_with,
)
from nose_parameterized import parameterized

import mpfs.engine.process
from test.helpers.stubs.utils import IterdbuidsStub

from mpfs.common.static import codes
from mpfs.common.util import trace_calls
from mpfs.config import settings
from mpfs.core.address import Address
from mpfs.core.filesystem.dao.legacy import CollectionRoutedDatabase
from mpfs.core.filesystem.helpers.lock import LockHelper
from mpfs.core.job_handlers import trash_clean
from mpfs.core.operations import manager
from mpfs.core.queue import mpfs_queue
from test.base import time_machine
from test.conftest import delay_async_tasks
from test.parallelly.filesystem.base import CommonFilesystemTestCase

db = CollectionRoutedDatabase()
usrctl = mpfs.engine.process.usrctl()

ALBUM_MAX_DJFS_RESOURCES_BULK_SIZE = settings.album['max_djfs_resources_bulk_size']


def check_djfs_albums_callbacks_per_resource_ids(counter, content, uid, resource_ids, mock_obj, bulk=True):
    assert len(content) == len(resource_ids)
    all_data = []
    times_called = 0
    for call in mock_obj.call_args_list:
        if call[0][-1] == 'file_changed_operation':
            data = call[0][0]
            all_data.append(data)
            times_called += 1
    if bulk:
        assert times_called == int(math.ceil(counter * 1.0 / ALBUM_MAX_DJFS_RESOURCES_BULK_SIZE))
    else:
        assert times_called == counter
    all_resource_ids = []
    for data in all_data:
        assert not data.viewkeys() ^ {'uid', 'resource_ids'}
        assert data['uid'] == uid
        all_resource_ids.extend(data['resource_ids'])
    assert not set(all_resource_ids) ^ set(resource_ids)


class TrashFilesystemTestCase(CommonFilesystemTestCase):
    def setup_method(self, method):
        super(TrashFilesystemTestCase, self).setup_method(method)
        self._mkdirs()
        self._mkfiles()

    def _mocked_address_json_ok(self, method, src_path, md5=None):
        params = {'uid': self.uid, 'path': src_path}
        if md5 is not None:
            params['md5'] = md5
        with mock.patch('mpfs.core.address.Address.add_trash_suffix'):
            self.json_ok(method, params)

    def _trash_append_folder(self, method):
        self._mocked_address_json_ok(method, '/disk/filesystem test folder')
        trashed_folder = self.get_trashed_item('/trash/filesystem test folder')
        folder_info = self.json_ok('info', {'uid': self.uid, 'path': trashed_folder['path'], 'meta': ''})
        assert folder_info['meta']['original_id'] == '/disk/filesystem test folder'

        folder_contents = self.json_ok('list', {'uid': self.uid, 'path': trashed_folder['path'], 'meta': ''})
        folder_counter = 0
        for item in folder_contents:
            folder_counter += 1
            assert item['id'] in (
                trashed_folder['path'] + '/',
                trashed_folder['path'] + '/inner file',
                trashed_folder['path'] + '/inner folder/',
            )
        assert folder_counter == 3

    def _trash_append_file(self, method):
        self._mocked_address_json_ok(method, '/disk/filesystem test folder/inner file')
        file_info = self.get_trashed_item('/trash/inner file')
        assert file_info['meta']['original_id'] == '/disk/filesystem test folder/inner file'

    def _trash_restore_file(self, method):
        file_info = self.json_ok('info', {'uid': self.uid, 'path': '/disk/filesystem test folder/inner file', 'meta': ''})
        original_file_id = file_info['meta']['file_id']

        self._mocked_address_json_ok('trash_append', '/disk/filesystem test folder/inner file')
        file_info = self.get_trashed_item('/trash/inner file')
        assert file_info['meta']['file_id'] == original_file_id
        assert file_info['meta']['original_id'] == '/disk/filesystem test folder/inner file'

        self.json_ok(method, {'uid': self.uid, 'path': file_info['path']})
        restored_file_info = self.json_ok('info', {'uid': self.uid, 'path': '/disk/filesystem test folder/inner file', 'meta': ''})
        assert restored_file_info['meta']['file_id'] == original_file_id

    def _trash_restore_folder(self, method):
        folder_info = self.json_ok('info', {'uid': self.uid, 'path': '/disk/filesystem test folder/', 'meta': ''})
        original_folder_id = folder_info['meta']['file_id']

        self._mocked_address_json_ok('trash_append', '/disk/filesystem test folder')
        trashed_folder = self.get_trashed_item('/trash/filesystem test folder')
        folder_info = self.json_ok('info', {'uid': self.uid, 'path': trashed_folder['path'], 'meta': ''})
        assert folder_info['meta']['file_id'] == original_folder_id
        assert folder_info['meta']['original_id'] == '/disk/filesystem test folder'

        self.json_ok(method, {'uid': self.uid, 'path': trashed_folder['path']})
        restored_folder_info = self.json_ok('info', {'uid': self.uid, 'path': '/disk/filesystem test folder', 'meta': ''})
        assert restored_folder_info['meta']['file_id'] == original_folder_id

        folder_contents = self.json_ok('list', {'uid': self.uid, 'path': '/disk/filesystem test folder', 'meta': ''})
        folder_counter = 0
        for item in folder_contents:
            folder_counter += 1
            assert item['id'] in (
                '/disk/filesystem test folder/',
                '/disk/filesystem test folder/inner file',
                '/disk/filesystem test folder/inner folder/',
            )
        assert folder_counter == 3

    def _trash_drop_one(self, method):
        folder_name = 'filesystem test folder'
        self._mocked_address_json_ok('trash_append', path.join('/disk/', folder_name))
        trash_path = self.get_trashed_item(path.join('/trash/', folder_name))['path']
        self.json_ok('info', {'uid': self.uid, 'path': trash_path, 'meta': ''})

        self.json_ok(method, {'uid': self.uid, 'path': trash_path})
        self.json_error('info', {'uid': self.uid, 'path': trash_path, 'meta': ''}, code=codes.RESOURCE_NOT_FOUND)

        hidden_path = trash_path.replace('/trash/', '/hidden/')
        hidden_data_content = list(db.hidden_data.find({'uid': self.uid}))
        assert_that(hidden_data_content,
                    has_item(has_entry('key', hidden_path)))

    def _trash_drop_all(self, method):
        self._mocked_address_json_ok('trash_append', '/disk/filesystem test folder')
        self.json_ok('trash_append', {'uid': self.uid, 'path': '/disk/filesystem test file'})

        hidden_data_content = self._drop(method, '/trash/filesystem test folder')
        assert len(hidden_data_content) == 5

    def _trash_without_subfolders_drop_all(self, method):
        self._mocked_address_json_ok('trash_append', '/disk/filesystem test file')

        hidden_data_content = self._drop(method, '/trash/filesystem test file')
        assert len(hidden_data_content) == 3

    def _trash_subfolder_only_drop_all(self, method):
        self._mocked_address_json_ok('trash_append', '/disk/filesystem test folder')
        hidden_data_content = self._drop(method, '/trash/filesystem test folder')
        assert len(hidden_data_content) == 4

    def _drop(self, method, exist_trash_path):
        self.json_ok(method, {'uid': self.uid})
        self.json_error('info', {'uid': self.uid, 'path': exist_trash_path, 'meta': ''}, code=codes.RESOURCE_NOT_FOUND)
        return list(db.hidden_data.find({'uid': self.uid}))

    def test_trash_append_folder_sync(self):
        self._trash_append_folder('trash_append')

    def test_trash_append_folder_async(self):
        self._trash_append_folder('async_trash_append')

    def test_trash_append_file_sync(self):
        self._trash_append_file('trash_append')

    def test_trash_append_file_async(self):
        self._trash_append_file('async_trash_append')

    def test_trash_restore_file_sync(self):
        self._trash_restore_file('trash_restore')

    def test_trash_restore_file_async(self):
        self._trash_restore_file('async_trash_restore')

    def test_trash_restore_folder_sync(self):
        self._trash_restore_folder('trash_restore')

    def test_trash_restore_folder_async(self):
        self._trash_restore_folder('async_trash_restore')

    def test_trash_drop_one_sync(self):
        self._trash_drop_one('trash_drop')

    def test_trash_drop_one_async(self):
        self._trash_drop_one('async_trash_drop')

    def test_trash_drop_all_sync(self):
        self._trash_drop_all('trash_drop_all')

    def test_trash_drop_all_async(self):
        self._trash_drop_all('async_trash_drop_all')

    def test_trash_subfolder_only_drop_all_sync(self):
        self._trash_subfolder_only_drop_all('trash_drop_all')

    def test_trash_subfolder_only_drop_all_async(self):
        self._trash_subfolder_only_drop_all('async_trash_drop_all')

    def test_trash_without_subfolders_drop_all_sync(self):
        self._trash_without_subfolders_drop_all('trash_drop_all')

    def test_trash_without_subfolders_drop_all_async(self):
        self._trash_without_subfolders_drop_all('async_trash_drop_all')

    def test_trash_append_put_trash_cleaner_queue(self):
        self._trash_append_file('trash_append')
        queue_task = list(db.trash_cleaner_queue.find({}))[0]
        assert self.uid == queue_task['uid']
        assert int(queue_task['date'])

    def test_async_trash_append_put_trash_cleaner_queue(self):
        self._trash_append_file('async_trash_append')
        queue_task = list(db.trash_cleaner_queue.find({}))[0]
        assert self.uid == queue_task['uid']
        assert int(queue_task['date'])

    def test_trash_drop_all_clear_trash_cleaner_queue(self):
        self._trash_drop_all('async_trash_drop_all')
        queue_task_list = list(db.trash_cleaner_queue.find({}))
        assert queue_task_list == []

    def test_trash_append_put_trash_cleaner_queue_more_than_one_time_diff_date(self):
        """
        Когда мы делаем trash_append в разные дни,
        на каждый день создаётся отдельная запись в очереди.
        """
        self._trash_append_file('trash_append')

        tomorrow = date.today() + timedelta(1)
        with time_machine(tomorrow):
            self.upload_file(self.uid, '/disk/test_file')
            self.json_ok('trash_append', {'uid': self.uid, 'path': '/disk/test_file'})
            queue_tasks = list(db.trash_cleaner_queue.find({}))

        assert queue_tasks[0]['date'] != queue_tasks[1]['date']

    def test_trash_append_put_trash_cleaner_queue_more_than_one_time_same_date(self):
        """
        Когда мы делаем несколько trash_append в один день,
        на этот день создаётся только одна запись в очереди.
        """
        self._trash_append_file('trash_append')
        self.upload_file(self.uid, '/disk/test_file')
        self.json_ok('trash_append', {'uid': self.uid, 'path': '/disk/test_file'})
        queue_tasks = list(db.trash_cleaner_queue.find({}))

        assert len(queue_tasks) == 1

    def test_trash_sort_with_limit(self):
        total_amount = 10
        for i in xrange(total_amount):
            path = '/disk/file_to_remove_%d' % i
            self.upload_file(self.uid, path)
            self.json_ok('trash_append', {'uid': self.uid, 'path': path})

        from mpfs.metastorage.mongo.collections.filesystem import UserCollectionZipped
        from mpfs.core.services import disk_service
        requested_amount = 5

        # тестируем, что если количество элементов в треше больше ограничения, то мы убираем сортировку по append_time
        # и выдаем столько элементов, сколько запрошено
        with mock.patch.object(disk_service, 'SYSTEM_SYSTEM_TRASH_SORT_BY_APPEND_TIME_LIMIT', total_amount - 1):
            with trace_calls(UserCollectionZipped, 'folder_content') as tracer:
                self.json_ok('list', {'uid': self.uid, 'path': '/trash', 'sort': 'append_time', 'order': '0',
                                      'meta': 'append_time', 'offset': '0', 'amount': str(requested_amount)})

                response = tracer['return_value']
                assert len(response.value) == requested_amount

        # тестируем, что если количество элементов в треше меньше ограничения, то мы не убираем сортировку по
        # append_time, вытаскиваем все элементы из корзины и только потом их сортируем
        with mock.patch.object(disk_service, 'SYSTEM_SYSTEM_TRASH_SORT_BY_APPEND_TIME_LIMIT', total_amount):
            with trace_calls(UserCollectionZipped, 'folder_content') as tracer:
                self.json_ok('list', {'uid': self.uid, 'path': '/trash', 'sort': 'append_time', 'order': '0',
                                      'meta': 'append_time', 'offset': '0', 'amount': str(requested_amount)})

                response = tracer['return_value']
                assert len(response.value) == total_amount

    def test_trash_restore_autosuffix(self):
        path = '/disk/1.txt'
        self.upload_file(self.uid, path)
        self._mocked_address_json_ok('trash_append', path)
        self.upload_file(self.uid, path)
        trashed_file = self.get_trashed_item('/trash/1.txt')
        self.json_ok('async_trash_restore', {'uid': self.uid, 'path': trashed_file['path']})
        root_list = self.json_ok('list', {'uid': self.uid, 'path': '/disk/'})
        names = [i['name'] for i in root_list]
        assert '1.txt' in names
        assert '1 (1).txt' in names

    def test_trash_cleaner_async_task(self):
        self._trash_append_file('trash_append')
        with time_machine(date.today() + timedelta(days=40)):
            queue_list = list(db.trash_cleaner_queue.find({}))
            assert len(queue_list) == 1
            assert len(list(db.hidden_data.find({'uid': self.uid}))) < 3
            mpfs_queue.put({'uid': self.uid}, 'trash_clean')
            assert len(list(db.hidden_data.find({'uid': self.uid}))) > 2

    def test_trash_cleaner_async_task_for_all(self):
        self._trash_append_file('trash_append')
        with time_machine(date.today() + timedelta(days=40)):
            queue_list = list(db.trash_cleaner_queue.find({}))
            assert len(queue_list) == 1
            assert len(list(db.hidden_data.find({'uid': self.uid}))) < 3
            with IterdbuidsStub():
                mpfs_queue.put({'max_processes': 4}, 'trash_clean')
            assert len(list(db.hidden_data.find({'uid': self.uid}))) > 2

    def test_trash_clean_process_queue(self):
        self._trash_append_file('trash_append')
        with time_machine(date.today() + timedelta(days=40)):
            queue_list = list(db.trash_cleaner_queue.find({}))
            assert len(queue_list) == 1
            assert len(list(db.hidden_data.find({'uid': self.uid}))) < 3
            trash_clean.process_queue()
            assert len(list(db.hidden_data.find({'uid': self.uid}))) > 2

    def test_trash_append_file_with_right_md5_is_successfull(self):
        file_info = self.json_ok(
            'info', {'uid': self.uid, 'path': '/disk/filesystem test folder/inner file', 'meta': 'md5'})
        md5 = file_info['meta']['md5']
        self._mocked_address_json_ok('trash_append', '/disk/filesystem test folder/inner file', md5=md5)
        file_info = self.get_trashed_item('/trash/inner file')
        assert file_info['meta']['original_id'] == '/disk/filesystem test folder/inner file'

    def test_async_trash_append_file_with_right_md5_is_successfull(self):
        file_info = self.json_ok(
            'info', {'uid': self.uid, 'path': '/disk/filesystem test folder/inner file', 'meta': 'md5'})
        md5 = file_info['meta']['md5']
        self._mocked_address_json_ok('async_trash_append', '/disk/filesystem test folder/inner file', md5=md5)
        file_info = self.get_trashed_item('/trash/inner file')
        assert file_info['meta']['original_id'] == '/disk/filesystem test folder/inner file'

    def test_trash_append_file_with_wrong_md5_lead_to_error(self):
        self.json_error(
            'trash_append',
            {'uid': self.uid, 'path': '/disk/filesystem test folder/inner file', 'md5': '123'},
            code=codes.PRECONDITIONS_FAILED
        )

    def test_async_trash_append_file_with_wrong_md5_lead_to_error(self):
        self.json_error(
            'async_trash_append',
            {'uid': self.uid, 'path': '/disk/filesystem test folder/inner file', 'md5': '123'},
            code=codes.PRECONDITIONS_FAILED
        )

    def test_trash_append_folder_with_md5_lead_to_error(self):
        self.json_error(
            'trash_append',
            {'uid': self.uid, 'path': '/disk/filesystem test folder', 'md5': '123'},
            code=codes.MD5_CHECK_NOT_SUPPORTED
        )

    def test_async_trash_append_folder_with_md5_lead_to_error(self):
        self.json_error(
            'async_trash_append',
            {'uid': self.uid, 'path': '/disk/filesystem test folder', 'md5': '123'},
            code=codes.MD5_CHECK_NOT_SUPPORTED
        )

    @parameterized.expand([
        ('append',),
        ('append_bulk',),
    ])
    def test_forbid_to_drop_trash_when_active_trash_append_operation_exists(self, op_subtype):
        path = '/disk/1.txt'
        self.upload_file(self.uid, path)
        with delay_async_tasks(), \
                mock.patch('mpfs.core.base.FEATURE_TOGGLES_CHECK_TRASH_APPEND_BEFORE_TRASH_DROP', return_value=True):
            operation = manager.create_operation(
                self.uid,
                'trash',
                op_subtype,
                odata=dict(
                    path=Address.Make(self.uid, path).id,
                    paths=[Address.Make(self.uid, path).id, ],
                    connection_id='1',
                )
            )
            from mpfs.core import factory
            resource = factory.get_resource(self.uid, Address.Make(self.uid, path))
            # Имитируем, будто операция началась выполняться и успела поставить лок
            LockHelper().lock(resource, {'oid': operation.id}, operation='trash_append')
            self.json_error('async_trash_drop_all', {'uid': self.uid}, code=codes.TRASH_APPEND_IS_RUNNING)

    def test_trash_append_same_name(self):
        self.upload_file(self.uid, '/disk/filesystem test folder/test_file')
        self.upload_file(self.uid, '/disk/filesystem test folder/inner folder/test_file')
        self.upload_file(self.uid, '/disk/test_file')
        self.json_ok('trash_append', {'uid': self.uid, 'path': '/disk/test_file'})
        self.json_ok('trash_append', {'uid': self.uid, 'path': '/disk/filesystem test folder/test_file'})
        self.json_ok('trash_append', {'uid': self.uid, 'path': '/disk/filesystem test folder/inner folder/test_file'})

        trash_contents = self.json_ok('list', {'uid': self.uid, 'path': '/trash/', 'meta': ''})
        trash_contents = filter(lambda x: x['type'] == 'file', trash_contents)
        assert len(trash_contents) == 3
        for item in trash_contents:
            assert item['path'].startswith('/trash/test_file_')

    # CHEMODAN-64739: Не удаляются файлы с одинаковыми простыми именами
    def test_trash_append_file_with_same_names_async(self):
        original_lock = LockHelper.lock_by_address
        locks = []

        def lock_wrapper(address, data=None, operation=None):
            locks.append((address, data, operation))
            original_lock(address, data, operation)

        def trash_file(file_path):
            with mock.patch('mpfs.core.filesystem.helpers.lock.LockHelper.lock_by_address', side_effect=lock_wrapper):
                self.json_ok('trash_append', {'uid': self.uid, 'path': file_path})

        def get_files(folder):
            return self.json_ok('list', {'uid': self.uid, 'path': folder, 'meta': ''})

        file_name = 'file.png'
        file1 = '/disk/' + file_name
        file2 = '/disk/other/' + file_name

        self.mkfile(file1)
        self.mkdir('/disk/other')
        self.mkfile(file2)

        empty_count = len(get_files('/trash/'))
        trash_file(file1)

        # имитируем лок первого файла в корзине - он не должен помешать удалить другой файл с таким же именем
        assert len(locks) == 1
        lock = locks[0]
        original_lock(lock[0], lock[1], lock[2])

        trash_file(file2)
        filled_content = get_files('/trash/')
        assert len(filled_content) == 2 + empty_count

    def test_trash_append_same_file(self):
        file_count = 3
        for i in range(file_count):
            self.upload_file(self.uid, '/disk/test_file')
            sleep(1)
            self.json_ok('trash_append', {'uid': self.uid, 'path': '/disk/test_file'})
            trash_contents = self.json_ok('list', {'uid': self.uid, 'path': '/trash/', 'meta': ''})
            trash_contents = filter(lambda x: x['type'] == 'file', trash_contents)
            assert len(trash_contents) == i+1

        for item in trash_contents:
            assert item['path'].startswith('/trash/test_file_')

    def test_lock_target_on_trash_append(self):
        main_dir_path = '/disk/main dir'
        dir_path = '/disk/main dir/dir with thousands subdirs'
        self.json_ok('mkdir', {'uid': self.uid, 'path': main_dir_path})
        self.json_ok('mkdir', {'uid': self.uid, 'path': dir_path})

        self.result = {}

        def restore_before_trash_append_finished(*args, **kwargs):
            """Конкурентная операции trash_restore.

            Должна зафейлиться об лок и получить данные о локе.
            """
            dir_path_in_trash = self.json_ok('list', {'uid': self.uid, 'path': '/trash'})[1]['path']
            # Попытка восстановить ресурс пока он удаляется должна быть неуспешной
            self.json_error('trash_restore', {'uid': self.uid, 'path': dir_path_in_trash},
                            code=codes.RESOURCE_LOCKED)

        with before_after.before('mpfs.metastorage.mongo.collections.filesystem.FilesystemLocksCollection.release',
                                 restore_before_trash_append_finished):
            self.json_ok('trash_append', {'uid': self.uid, 'path': dir_path})

        # В основной паке нет больше ресурса (и не появилось нового восстановленного) - только корневой ресурс
        assert len(self.json_ok('list', {'uid': self.uid, 'path': main_dir_path})) == 1
        # В Корзине лежит удаленный ресурс + корневой
        assert len(self.json_ok('list', {'uid': self.uid, 'path': '/trash/'})) == 2

    @parameterized.expand([
        ('photo_with_coords.jpg', 55.734243, 37.589006, True),  # фоточка с координатами
        ('photo_without_coords.jpg', None, None, True),  # фоточка без координат, но с превью
        ('not_photo_without_coords.txt', None, None, False),  # не фоточка без координат
    ])
    def test_djfs_album_callback(self, filename, latitude, longitude, result):
        filepath = '/disk/filesystem test folder/inner folder/%s' % filename
        self.upload_file_with_coordinates(self.uid, filepath, latitude, longitude)
        resource_id = self.json_ok('info', {'uid': self.uid,
                                            'path': filepath, 'meta': 'resource_id'})['meta']['resource_id']
        with mock.patch('mpfs.core.queue.mpfs_queue.put') as mock_obj:
            self.json_ok('trash_append', {'uid': self.uid, 'path': filepath})
            trash_contents = self.json_ok('list', {'uid': self.uid, 'path': '/trash/', 'meta': ''})
            trash_contents = filter(lambda x: x['type'] == 'file', trash_contents)
            assert len(trash_contents) == 1
            item = trash_contents[0]
            assert item['path'].startswith('/trash/%s' % filename)
            data = None
            for call in mock_obj.call_args_list:
                if call[0][-1] == 'file_changed_operation':
                    data = call[0][0]
            assert bool(data) == result
            if result:
                assert not data.viewkeys() ^ {'uid', 'resource_ids'}
                assert data['uid'] == self.uid
                assert data['resource_ids'] == [resource_id, ]

    @parameterized.expand([
        (3,),  # фоточки с координатами влезают в 1 bulk
        (400,),  # фоточки с координатами влезают в 1 bulk. предел
        (410,)  # фоточки с координатами влезают в 2 bulk
    ])
    def test_djfs_albums_bulk_trash_append(self, counter):
        filenames = ['photo_with_coords-%d.jpg' % i for i in range(counter)]
        file_folder = '/disk/filesystem test folder/inner folder'
        resource_ids = list()
        for filename in filenames:
            filepath = '%s/%s' % (file_folder, filename)
            self.upload_file_with_coordinates(self.uid, filepath)
            resource_id = self.json_ok('info', {'uid': self.uid,
                                                'path': filepath, 'meta': 'resource_id'})['meta']['resource_id']
            resource_ids.append(resource_id)
        with mock.patch('mpfs.core.queue.mpfs_queue.put') as mock_obj:
            self.json_ok('trash_append', {'uid': self.uid, 'path': file_folder})
            trash_content = self.get_trashed_item('/trash/inner folder')
            trash_content = self.json_ok('list', {'uid': self.uid, 'path': trash_content['path'], 'meta': ''})
            trash_content = filter(lambda x: x['type'] == 'file', trash_content)
            check_djfs_albums_callbacks_per_resource_ids(counter, trash_content, self.uid, resource_ids, mock_obj)

    def test_djfs_albums_trash_restore(self):
        counter = 3
        filenames = ['photo_with_coords-%d.jpg' % i for i in range(counter)]
        file_folder = '/disk/filesystem test folder/inner folder'
        resource_ids = list()
        for filename in filenames:
            filepath = '%s/%s' % (file_folder, filename)
            self.upload_file_with_coordinates(self.uid, filepath)
            resource_id = self.json_ok('info', {'uid': self.uid,
                                                'path': filepath, 'meta': 'resource_id'})['meta']['resource_id']
            resource_ids.append(resource_id)
            self._mocked_address_json_ok('trash_append', filepath)
        with mock.patch('mpfs.core.queue.mpfs_queue.put') as mock_obj:
            for filename in filenames:
                filepath = self.get_trashed_item('/trash/%s' % filename)['path']
                self.json_ok('trash_restore', {'uid': self.uid, 'path': filepath})
            content = self.json_ok('list', {'uid': self.uid, 'path': file_folder, 'meta': ''})
            content = filter(lambda x: x['type'] == 'file', content)
            check_djfs_albums_callbacks_per_resource_ids(counter, content, self.uid, resource_ids, mock_obj, bulk=False)

    @parameterized.expand([
        (3,),  # фоточки с координатами влезают в 1 bulk
    ])
    def test_djfs_albums_bulk_trash_restore(self, counter):
        filenames = ['photo_with_coords-%d.jpg' % i for i in range(counter)]
        file_folder = '/disk/filesystem test folder/inner folder'
        resource_ids = list()
        for filename in filenames:
            filepath = '%s/%s' % (file_folder, filename)
            self.upload_file_with_coordinates(self.uid, filepath)
            resource_id = self.json_ok('info', {'uid': self.uid,
                                                'path': filepath, 'meta': 'resource_id'})['meta']['resource_id']
            resource_ids.append(resource_id)
        self.json_ok('trash_append', {'uid': self.uid, 'path': file_folder})
        trashed_folder = self.get_trashed_item('/trash/inner folder')
        with mock.patch('mpfs.core.queue.mpfs_queue.put') as mock_obj:
            self.json_ok('trash_restore', {'uid': self.uid, 'path': trashed_folder['path']})
            restored_content = self.json_ok('list', {'uid': self.uid, 'path': file_folder, 'meta': ''})
            restored_content = filter(lambda x: x['type'] == 'file', restored_content)
            check_djfs_albums_callbacks_per_resource_ids(counter, restored_content, self.uid, resource_ids, mock_obj)


class FileIDInHiddenTestCase(CommonFilesystemTestCase):
    def setup_method(self, method):
        super(FileIDInHiddenTestCase, self).setup_method(method)
        folder_name = 'fluffy pics'
        self.folder_path = path.join('/disk/', folder_name)
        file_name = 'enot.JPG'
        self.file_path = path.join('/disk/', folder_name, file_name)
        self.hidden_path = path.join('/hidden/', folder_name, file_name)

        self.json_ok('mkdir', {'uid': self.uid, 'path': self.folder_path})
        self.upload_file(self.uid, self.file_path)

    def test_hidden_data_file_id_after_trash_drop_all(self):
        original_file_id = list(db.user_data.find({'uid': self.uid, 'key': self.file_path}))[0]['data']['file_id']

        with mock.patch('mpfs.core.address.Address.add_trash_suffix'):
            self.json_ok('trash_append', {'uid': self.uid, 'path': self.folder_path})
        self.json_ok('trash_drop_all', {'uid': self.uid})

        hidden_data_content = list(db.hidden_data.find({'uid': self.uid}))
        assert_that(hidden_data_content,
                    # К пути файла добавляется суффикс, поэтому путь файла проверяем по началу пути
                    has_item(has_entries({'key': starts_with(self.hidden_path),
                                          # Проверяем, что file_id у файла теперь другой
                                          'data': has_entry('file_id', is_not(equal_to(original_file_id)))})))

    def test_hidden_data_file_id_after_rm(self):
        original_file_id = list(db.user_data.find({'uid': self.uid, 'key': self.file_path}))[0]['data']['file_id']

        self.json_ok('rm', {'uid': self.uid, 'path': self.folder_path})

        hidden_data_content = list(db.hidden_data.find({'uid': self.uid}))
        assert_that(hidden_data_content,
                    # К пути файла добавляется суффикс, поэтому путь файла проверяем по началу пути
                    has_item(has_entries({'key': starts_with(self.hidden_path),
                                          # Проверяем, что file_id у файла теперь другой
                                          'data': has_entry('file_id', is_not(equal_to(original_file_id)))})))


class RmFilesystemTestCase(CommonFilesystemTestCase):
    def setup_method(self, method):
        super(RmFilesystemTestCase, self).setup_method(method)
        self._mkdirs()
        self._mkfiles()

    def _mocked_address_json_ok(self, method, src_path, md5=None):
        params = {'uid': self.uid, 'path': src_path}
        if md5 is not None:
            params['md5'] = md5
        with mock.patch('mpfs.core.address.Address.add_trash_suffix'):
            self.json_ok(method, params)

    @parameterized.expand([
        ('photo_with_coords.jpg', 55.734243, 37.589006, 'image', False, True),  # фоточка с координатами
        ('video_without_coords.avi', None, None, 'video', False, True),  # видео без координат
        ('photo_without_coords.jpg', None, None, 'image', False, False),  # фото без координат и без превью
        ('photo_without_coords.jpg', None, None, 'image', True, True),  # фото без координат, но с превью
        ('not_photo_without_coords.txt', None, None, 'text', False, False),  # не фоточка без координат
    ])
    def test_djfs_album_callback(self, filename, latitude, longitude, media_type, preview, result):
        filepath = '/disk/filesystem test folder/inner folder/%s' % filename
        self.upload_file_with_coordinates(self.uid, filepath, latitude, longitude,
                                          media_type=media_type, preview=preview)
        resource = self.json_ok('info', {'uid': self.uid, 'path': filepath, 'meta': 'resource_id,preview'})
        resource_id = resource['meta']['resource_id']
        with mock.patch('mpfs.core.queue.mpfs_queue.put') as mock_obj:
            self.json_ok('trash_append', {'uid': self.uid, 'path': filepath})
            trash_contents = self.json_ok('list', {'uid': self.uid, 'path': '/trash/', 'meta': ''})
            trash_contents = filter(lambda x: x['type'] == 'file', trash_contents)
            assert len(trash_contents) == 1
            item = trash_contents[0]
            assert item['path'].startswith('/trash/%s' % filename)
            data = None
            for call in mock_obj.call_args_list:
                if call[0][-1] == 'file_changed_operation':
                    data = call[0][0]
            assert bool(data) == result
            if result:
                assert not data.viewkeys() ^ {'uid', 'resource_ids'}
                assert data['uid'] == self.uid
                assert data['resource_ids'] == [resource_id,]

    @parameterized.expand([
        (3, ),  # фоточки с координатами влезают в 1 bulk
        (400, ),  # фоточки с координатами влезают в 1 bulk. предел
        (402, )  # фоточки с координатами влезают в 2 bulk
    ])
    def test_djfs_albums_bulk_trash_append(self, counter):
        filenames = ['photo_with_coords-%d.jpg' % i for i in range(counter)]
        file_folder = '/disk/filesystem test folder/inner folder'
        resource_ids = list()
        for filename in filenames:
            filepath = '%s/%s' % (file_folder, filename)
            self.upload_file_with_coordinates(self.uid, filepath)
            resource_id = self.json_ok('info', {'uid': self.uid,
                                                'path': filepath, 'meta': 'resource_id'})['meta']['resource_id']
            resource_ids.append(resource_id)
        with mock.patch('mpfs.core.queue.mpfs_queue.put') as mock_obj:
            self.json_ok('trash_append', {'uid': self.uid, 'path': file_folder})
            trash_folder_path = self.get_trashed_item('/trash/inner folder')['path']
            trash_content = self.json_ok('list', {'uid': self.uid, 'path': trash_folder_path, 'meta': ''})
            trash_content = filter(lambda x: x['type'] == 'file', trash_content)
            check_djfs_albums_callbacks_per_resource_ids(counter, trash_content, self.uid, resource_ids, mock_obj)

    def test_djfs_albums_trash_restore(self):
        counter = 3
        filenames = ['photo_with_coords-%d.jpg' % i for i in range(counter)]
        file_folder = '/disk/filesystem test folder/inner folder'
        resource_ids = list()
        for filename in filenames:
            filepath = '%s/%s' % (file_folder, filename)
            self.upload_file_with_coordinates(self.uid, filepath)
            resource_id = self.json_ok('info', {'uid': self.uid,
                                                'path': filepath, 'meta': 'resource_id'})['meta']['resource_id']
            resource_ids.append(resource_id)
            self._mocked_address_json_ok('trash_append', filepath)
        with mock.patch('mpfs.core.queue.mpfs_queue.put') as mock_obj:
            for filename in filenames:
                filepath = '/trash/%s' % filename
                self.json_ok('trash_restore', {'uid': self.uid, 'path': filepath})
            content = self.json_ok('list', {'uid': self.uid, 'path': file_folder, 'meta': ''})
            content = filter(lambda x: x['type'] == 'file', content)
            check_djfs_albums_callbacks_per_resource_ids(counter, content, self.uid, resource_ids, mock_obj, bulk=False)

    @parameterized.expand([
        (3, ),  # фоточки с координатами влезают в 1 bulk
    ])
    def test_djfs_albums_bulk_trash_restore(self, counter):
        filenames = ['photo_with_coords-%d.jpg' % i for i in range(counter)]
        file_folder = '/disk/filesystem test folder/inner folder'
        resource_ids = list()
        for filename in filenames:
            filepath = '%s/%s' % (file_folder, filename)
            self.upload_file_with_coordinates(self.uid, filepath)
            resource_id = self.json_ok('info', {'uid': self.uid,
                                                'path': filepath, 'meta': 'resource_id'})['meta']['resource_id']
            resource_ids.append(resource_id)
        self.json_ok('trash_append', {'uid': self.uid, 'path': file_folder})
        trashed_folder = self.get_trashed_item('/trash/inner folder')
        with mock.patch('mpfs.core.queue.mpfs_queue.put') as mock_obj:
            self.json_ok('trash_restore', {'uid': self.uid, 'path': trashed_folder['path']})
            restored_content = self.json_ok('list', {'uid': self.uid, 'path': file_folder, 'meta': ''})
            restored_content = filter(lambda x: x['type'] == 'file', restored_content)
            check_djfs_albums_callbacks_per_resource_ids(counter, restored_content, self.uid, resource_ids, mock_obj)
