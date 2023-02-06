# -*- coding: utf-8 -*-

import mock
import urlparse

from mpfs.core.filesystem.dao.resource import NotesDAO
from test.helpers.size_units import GB

from test.helpers.stubs.services import KladunStub
from test.parallelly.json_api.base import CommonJsonApiTestCase


class NotesStorageAPITestCase(CommonJsonApiTestCase):

    def __init__(self, *args, **kwargs):
        from mpfs.core.event_history.logger import _log_raw_event_message
        super(NotesStorageAPITestCase, self).__init__(*args, **kwargs)
        self.event_log_patch = mock.patch(
            'mpfs.core.event_history.logger._log_raw_event_message',
            wraps=_log_raw_event_message
        )

    def test_store_notes_section_operation_created(self):
        """Проверить сохранение файла внутри раздела /notes (создание операции)."""
        uid = self.uid
        with self.patch_mulca_is_file_exist(func_resp=False):
            with KladunStub():
                result = self.json_ok('store', {
                    'uid': uid,
                    'path': '/notes/test.txt',
                    'md5': 'de9ef78c8819c9ab88277e1aa13c1169',  # random
                    'sha256': 'f7df23b258eab15fac46f54881a1c63a1f1f46a9c8dfd749965e22225e8d1325',  # random
                    'size': '53',  # random
                })
                assert 'oid' in result

    def test_store_notes_section_file_created(self):
        """Проверить что файл корректно создается.

        Дополнительно проверяется что будет создан раздел /notes, если его нет.
        """
        uid = self.uid

        assert not NotesDAO().find_one({'uid': uid, 'path': '/notes'})

        self.upload_file(uid, '/notes/test.txt')

        # создан нужный домен (коллекция/таблица) в базе
        assert NotesDAO().find_one({'uid': uid, 'path': '/notes'})

        self.json_ok('info', {
            'uid': uid,
            'path': '/notes/test.txt'
        })

    def test_mkdir_notes_section_folder_created(self):
        """Проверить что папка корректно создается.

        Дополнительно проверяется что будет создан раздел /notes, если его нет.
        """
        uid = self.uid

        assert not NotesDAO().find_one({'uid': uid, 'path': '/notes'})

        self.json_ok('mkdir', {
            'uid': uid,
            'path': '/notes/test_folder'
        })

        # создан нужный домен (коллекция/таблица) в базе
        assert NotesDAO().find_one({'uid': uid, 'path': '/notes'})

        self.json_ok('info', {
            'uid': uid,
            'path': '/notes/test_folder'
        })

    def test_user_init_notes_not_created(self):
        """Проверить что при инициализации пользователя не создается
        реальный раздел /notes для заметок."""
        uid = self.user_3.uid  # не инициализированный

        self.json_ok('user_init', {
            'uid': uid,
        })
        assert not NotesDAO().find_one({'uid': uid, 'path': '/notes'})

        self.json_ok('info', {
            'uid': uid,
            'path': '/notes',
        })  # работает, потому что /notes - системная папка и is_storage

        assert not NotesDAO().find_one({'uid': uid, 'path': '/notes'})

    def test_no_messages_in_event_history_after_store(self):
        """Проверить что при создании файла в разделе /notes ничего не пишется в event лог."""
        uid = self.uid
        mocked_log = self.event_log_patch.start()
        self.upload_file(uid, '/notes/test.txt')
        assert not mocked_log.call_count
        self.event_log_patch.stop()

    def test_no_messages_in_event_history_after_mkdir(self):
        """Проверить что при создании папки в разделе /notes ничего не пишется в event лог."""
        uid = self.uid
        mocked_log = self.event_log_patch.start()
        self.json_ok('mkdir', {
            'uid': uid,
            'path': '/notes/test_folder'
        })
        assert not mocked_log.call_count
        self.event_log_patch.stop()

    def test_no_messages_in_event_history_after_rm_folder(self):
        """Проверить что при удалении папки в разделе /notes ничего не пишется в event лог."""
        uid = self.uid
        self.json_ok('mkdir', {
            'uid': uid,
            'path': '/notes/test_folder'
        })
        mocked_log = self.event_log_patch.start()
        self.json_ok('rm', {
            'uid': uid,
            'path': '/notes/test_folder'
        })
        assert not mocked_log.call_count
        self.event_log_patch.stop()

    def test_no_messages_in_event_history_after_rm_file(self):
        """Проверить что при удалении файла в разделе /notes ничего не пишется в event лог."""
        uid = self.uid
        self.upload_file(uid, '/notes/test.txt')
        mocked_log = self.event_log_patch.start()
        self.json_ok('rm', {
            'uid': uid,
            'path': '/notes/test.txt'
        })
        assert not mocked_log.call_count
        self.event_log_patch.stop()

    def test_new_search_for_notes_path(self):
        """Проверить, что в настоящее время наша ручка поиска для раздела /notes отдает пустой список."""
        uid = self.uid
        self.upload_file(uid, '/notes/test.txt')
        result = self.json_ok('new_search', {
            'uid': uid,
            'path': '/notes',
            'query': '*'
        })
        assert len(result['results']) == 1  # только сама папка /notes

    def test_new_search_does_not_include_notes(self):
        """Проверить, что если не указывать специально поиск по разделу /notes,
        то результаты оттуда не возвращаются."""
        uid = self.uid
        self.upload_file(uid, '/disk/test.txt')
        self.upload_file(uid, '/notes/test.txt')
        with mock.patch(
            'mpfs.core.services.search_service.DiskSearch.open_url',
            return_value=None
        ) as mocked_search_open_url:
            self.json_ok('new_search', {
                'uid': uid,
                'query': 'test'
            })
            assert mocked_search_open_url.called
            args, kwargs = mocked_search_open_url.call_args
            (url,) = args
            parsed_url = urlparse.urlparse(url)
            parsed_query = urlparse.parse_qs(parsed_url.query)
            assert parsed_query['text'] == ['test']
            assert set(parsed_query['aux_folder']) == {'disk', 'photounlim'}

    @mock.patch('mpfs.core.filesystem.indexer.DiskDataIndexer.indexer_data_mill', return_value=None)
    def test_no_push_to_index_for_notes_mkdir_folder(self, mocked_indexer_data_mill):
        """Проверить что при создании папки внутри раздела /notes данные не уходят в индексер."""
        # indexer.push работает через indexer.push_tree
        uid = self.uid

        self.json_ok('mkdir', {
            'uid': uid,
            'path': '/disk/folder'
        })
        assert mocked_indexer_data_mill.called
        mocked_indexer_data_mill.reset_mock()

        self.json_ok('mkdir', {
            'uid': uid,
            'path': '/notes/folder'
        })
        assert not mocked_indexer_data_mill.called

    @mock.patch('mpfs.core.filesystem.indexer.DiskDataIndexer.indexer_data_mill', return_value=None)
    def test_no_push_to_index_for_notes_rm_with_subdirs(self, mocked_indexer_data_mill):
        """Проверить что при удалении папки с подпапками внутри раздела /notes данные не уходят в индексер."""
        uid = self.uid
        self.json_ok('mkdir', {
            'uid': uid,
            'path': '/notes/folder'
        })
        self.json_ok('mkdir', {
            'uid': uid,
            'path': '/notes/folder/sub'
        })
        self.json_ok('rm', {
            'uid': uid,
            'path': '/notes/folder'
        })
        assert not mocked_indexer_data_mill.called

    @mock.patch('mpfs.core.filesystem.indexer.DiskDataIndexer.indexer_data_mill', return_value=None)
    def test_no_push_to_index_for_notes_mkfile(self, mocked_indexer_data_mill):
        """Проверить что при создании файла внутри раздела /notes данные не уходят в индексер."""
        uid = self.uid
        self.upload_file(uid, '/notes/test.txt')
        assert not mocked_indexer_data_mill.called

    @mock.patch('mpfs.core.filesystem.indexer.DiskDataIndexer.indexer_data_mill', return_value=None)
    def test_no_push_to_index_for_notes_copy_file(self, mocked_indexer_data_mill):
        """Проверить что при копировании файла внутри раздела /notes данные не уходят в индексер."""
        uid = self.uid
        self.upload_file(uid, '/notes/test.txt')
        self.json_ok('copy', {
            'uid': uid,
            'src': '/notes/test.txt',
            'dst': '/notes/copied_test.txt'
        })
        assert not mocked_indexer_data_mill.called

    @mock.patch('mpfs.core.filesystem.indexer.DiskDataIndexer.indexer_data_mill', return_value=None)
    def test_no_push_to_index_for_notes_move_file(self, mocked_indexer_data_mill):
        """Проверить что при перемещении файла внутри раздела /notes данные не уходят в индексер."""
        uid = self.uid
        self.upload_file(uid, '/notes/old_test.txt')
        self.json_ok('move', {
            'uid': uid,
            'src': '/notes/old_test.txt',
            'dst': '/notes/new_test.txt'
        })
        assert not mocked_indexer_data_mill.called

    @mock.patch('mpfs.core.filesystem.indexer.DiskDataIndexer.indexer_data_mill', return_value=None)
    def test_no_push_to_index_for_notes_trash_append_file(self, mocked_indexer_data_mill):
        """Проверить что при удалении файла внутри раздела /notes данные не уходят в индексер."""
        uid = self.uid
        self.upload_file(uid, '/notes/test.txt')
        self.json_ok('trash_append', {
            'uid': uid,
            'path': '/notes/test.txt',
        })
        assert not mocked_indexer_data_mill.called

    def test_allow_overdraft_request_from_notes(self):
        user_agent = 'sas1-3648439ac7cb/notes/100.1647.2'
        with mock.patch('mpfs.core.services.disk_service.MPFSStorageService.used', return_value=12*GB), \
                mock.patch('mpfs.core.services.disk_service.MPFSStorageService.limit', return_value=10*GB), \
                mock.patch('mpfs.frontend.api.FEATURE_TOGGLES_DISABLE_API_FOR_OVERDRAFT_PERCENTAGE', 100):
            self.json_ok('info', {'uid': self.uid, 'path': '/disk/'},
                         headers={'user-agent': user_agent, 'Yandex-Cloud-Request-ID': 'rest-test'})
