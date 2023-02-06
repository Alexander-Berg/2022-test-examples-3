# -*- coding: utf-8 -*-
import datetime

import pytest
import mock
from nose_parameterized import parameterized

from test.base import DiskTestCase, time_machine
from test.base_suit import TrashTestCaseMixin
from test.common.sharing import CommonSharingMethods

from mpfs.core.filesystem.dao.file import FileDAOItem
from mpfs.core.filesystem.dao.folder import FolderDAOItem
from mpfs.core.job_handlers.trash_clean import process_queue
from mpfs.dao.session import Session
from mpfs.metastorage.postgres.queries import SQL_FILES_BY_UID, SQL_FOLDERS_BY_UID
from mpfs.core.services.reindex.utils import calculate_max_folder_depth, calculate_max_folder_depth_for_path
from mpfs.common.static import codes
from mpfs.core.filesystem.dao.legacy import CollectionRoutedDatabase
from mpfs.common.util import flatten_dict, to_json
from mpfs.common.util.experiments.logic import enable_experiment_for_uid
from mpfs.metastorage.mongo.collections.filesystem import is_reindexed_for_quick_move
from mpfs.core.services.djfs_api_service import DJFS_NOT_IMPLEMENTED_HTTP_CODE

from test.conftest import INIT_USER_IN_POSTGRES, capture_queue_parameters
from test.helpers.stubs.services import PushServicesStub, SearchIndexerStub, DiskSearchStub, SearchDBStub, \
    SearchDBTinyStub, DjfsApiMockHelper
from test.helpers.stubs.manager import StubsManager


class QuickMoveTestCase(DiskTestCase):

    def run(self, result=None):
        with mock.patch('mpfs.metastorage.mongo.collections.filesystem.POSTGRES_QUICK_MOVE_ENABLED', True), \
                mock.patch('mpfs.metastorage.mongo.collections.filesystem.POSTGRES_QUICK_MOVE_ALLOWED_UIDS', self.uid), \
                mock.patch('mpfs.core.filesystem.base.POSTGRES_QUICK_MOVE_FOLDER_RESOURCES_LIMIT', 0), \
                mock.patch('mpfs.core.job_handlers.indexer.INDEXER_PHOTOSLICE_NOTIFICATION_ON_INDEXER_SIDE', True):
            return super(QuickMoveTestCase, self).run(result)

    @pytest.mark.skipif(not INIT_USER_IN_POSTGRES, reason='Test for quick postgres move')
    def test_rename_for_pg_user(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/folder'})
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/folder/subfolder'})
        self.upload_file(self.uid, '/disk/folder/file-1.txt')
        self.upload_file(self.uid, '/disk/folder/subfolder/file-2.txt')

        result = self.json_ok('async_move', {'uid': self.uid, 'src': '/disk/folder', 'dst': '/disk/new'})
        status = self.json_ok('status', {'uid': self.uid, 'oid': result['oid']})
        assert status['status'] == 'DONE'

        self.json_error('info', {'uid': self.uid, 'path': '/disk/folder'}, code=codes.RESOURCE_NOT_FOUND)
        self.json_ok('info', {'uid': self.uid, 'path': '/disk/new'})
        self.json_ok('info', {'uid': self.uid, 'path': '/disk/new/subfolder'})
        self.json_ok('info', {'uid': self.uid, 'path': '/disk/new/file-1.txt'})
        self.json_ok('info', {'uid': self.uid, 'path': '/disk/new/subfolder/file-2.txt'})

    @pytest.mark.skipif(not INIT_USER_IN_POSTGRES, reason='Test for quick postgres move')
    def test_quick_move_for_pg_user(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/folder'})
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/folder/subfolder'})
        self.upload_file(self.uid, '/disk/folder/file-1.txt')
        self.upload_file(self.uid, '/disk/folder/subfolder/file-2.txt')

        self.json_ok('async_move', {'uid': self.uid, 'src': '/disk/folder/subfolder', 'dst': '/disk/new'})

        self.json_ok('info', {'uid': self.uid, 'path': '/disk/folder'})
        self.json_error('info', {'uid': self.uid, 'path': '/disk/folder/subfolder'}, code=codes.RESOURCE_NOT_FOUND)
        self.json_ok('info', {'uid': self.uid, 'path': '/disk/new'})
        self.json_ok('info', {'uid': self.uid, 'path': '/disk/new/file-2.txt'})
        self.json_ok('info', {'uid': self.uid, 'path': '/disk/folder/file-1.txt'})

    @pytest.mark.skipif(not INIT_USER_IN_POSTGRES, reason='Test for quick postgres move')
    def test_quick_move_push_to_search_indexer(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/folder'})
        self.upload_file(self.uid, '/disk/folder/file.txt')

        search_push_found = False
        with SearchIndexerStub() as index_service:
            self.json_ok('async_move', {'uid': self.uid, 'src': '/disk/folder', 'dst': '/disk/new'})
            for call_args in index_service.push_change.call_args_list:
                indexer_data = call_args[0][0]
                indexer_kwargs = call_args[1]

                assert indexer_kwargs.get('append_smartcache_callback') is True
                for push in indexer_data:
                    assert push['id'] == '/disk/new'
                    assert push['action'] == 'modify'
                    assert push['operation'] == 'move_resource'
                    search_push_found = True

        assert search_push_found

        self.json_ok('info', {'uid': self.uid, 'path': '/disk/new'})

    def test_desktop_client_full_diff_restriction_after_quick_move(self):
        version_before_move = self.json_ok('user_info', {'uid': self.uid})['version']
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/folder'})
        folder_info = CollectionRoutedDatabase().user_data.find_one({'uid': self.uid, 'path': '/disk/folder'})
        old_folder_version = folder_info['version']

        diff_push = None
        with PushServicesStub() as push_service:
            self.json_ok('async_move', {'uid': self.uid, 'src': '/disk/folder', 'dst': '/disk/new'})
            for call_args in push_service.send.call_args_list:
                push = PushServicesStub.parse_send_call(call_args)
                if push['event_name'] == 'diff':
                    diff_push = push

        folder_info = CollectionRoutedDatabase().user_data.find_one({'uid': self.uid, 'path': '/disk/new'})
        new_folder_version = folder_info['version']
        assert version_before_move != new_folder_version

        version_after_move = self.json_ok('user_info', {'uid': self.uid})['version']
        if INIT_USER_IN_POSTGRES:
            assert version_after_move == new_folder_version  # точно совпадает, т.к. мы только меняем parent у папки
            self.json_error('diff', {'uid': self.uid, 'version': version_before_move},
                            code=codes.VERSION_NOT_FOUND)
            self.json_error('deltas', {'uid': self.uid, 'base_revision': version_before_move},
                            code=codes.VERSION_NOT_FOUND)

            assert diff_push is not None

            assert diff_push['json_payload']['root']['parameters']['new'] == version_after_move
            assert diff_push['json_payload']['root']['parameters']['old'] == old_folder_version

            assert len(diff_push['json_payload']['values']) == 2
            # должно быть 2 события - создание новой папки и удаление старой

            delete_event = diff_push['json_payload']['values'][0]
            new_event = diff_push['json_payload']['values'][1]

            assert delete_event['parameters']['type'] == 'deleted'
            assert delete_event['parameters']['resource_type'] == 'dir'
            assert delete_event['parameters']['key'] == '/disk/folder'
            assert delete_event['parameters']['folder'] == '/disk/'
            assert delete_event['parameters']['fid'] == folder_info['data']['file_id']

            assert new_event['parameters']['type'] == 'new'
            assert new_event['parameters']['resource_type'] == 'dir'
            assert new_event['parameters']['key'] == '/disk/new'
            assert new_event['parameters']['folder'] == '/disk/'
            assert new_event['parameters']['fid'] == folder_info['data']['file_id']
        else:
            assert version_after_move > new_folder_version  # не совпадает точно, потому что мы после создания папки
            # удаляем старую и версия диска меняется
            self.json_ok('diff', {'uid': self.uid, 'version': version_before_move})

    @pytest.mark.skipif(not INIT_USER_IN_POSTGRES, reason='Test for quick postgres move')
    def test_quick_move_changelog_delta_for_new_software_client(self):
        version_before_move = self.json_ok('user_info', {'uid': self.uid})['version']

        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/folder'})
        self.json_ok('async_move', {'uid': self.uid, 'src': '/disk/folder', 'dst': '/disk/new'})

        version_after_move = self.json_ok('user_info', {'uid': self.uid})['version']

        assert version_after_move >= version_before_move

        diff = self.json_ok('diff', {'uid': self.uid, 'version': version_before_move, 'allow_quick_move_deltas': '1'})
        for delta in diff['result']:
            if delta['op'] == 'moved':
                assert delta['key'] == '/disk/folder'
                assert delta['new_key'] == '/disk/new'
                break
        else:
            self.fail('moved delta not found in diff result')

        deltas = self.json_ok('deltas', {
            'uid': self.uid, 'base_revision': version_before_move, 'allow_quick_move_deltas': '1'
        })
        for delta in deltas['items']:
            if delta['change_type'] == 'moved':
                assert delta['path'] == '/disk/folder'
                assert delta['new_path'] == '/disk/new'
                break
        else:
            self.fail('moved delta not found in deltas result')

    @pytest.mark.skipif(not INIT_USER_IN_POSTGRES, reason='Test for quick postgres move')
    def test_quick_move_filtering_disable_for_removed_files(self):
        version_before = self.json_ok('user_info', {'uid': self.uid})['version']

        self.upload_file(self.uid, '/disk/some-file.txt')
        self.json_ok('rm', {'uid': self.uid, 'path': '/disk/some-file.txt'})

        deltas = self.json_ok('deltas', {
            'uid': self.uid, 'base_revision': version_before, 'allow_quick_move_deltas': '1'
        })

        assert len(deltas['items']) > 0

        for delta in deltas['items']:
            assert 'resource' not in delta

    @pytest.mark.skipif(not INIT_USER_IN_POSTGRES, reason='Test for quick postgres move')
    def test_quick_move_changelog_deltas_error_for_old_software_client(self):
        version_before_move = self.json_ok('user_info', {'uid': self.uid})['version']

        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/folder'})
        self.json_ok('async_move', {'uid': self.uid, 'src': '/disk/folder', 'dst': '/disk/new'})

        version_after_move = self.json_ok('user_info', {'uid': self.uid})['version']

        assert version_after_move >= version_before_move

        self.json_error('diff', {'uid': self.uid, 'version': version_before_move, 'allow_quick_move_deltas': '0'},
                        code=codes.VERSION_NOT_FOUND)

        self.json_error('deltas', {
            'uid': self.uid, 'base_revision': version_before_move, 'allow_quick_move_deltas': '0'
        }, code=codes.VERSION_NOT_FOUND)

    @pytest.mark.skipif(not INIT_USER_IN_POSTGRES, reason='Test for quick postgres move')
    def test_quick_move_disable_for_small_folder(self):
        def prepare_data():
            self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/folder'})
            for i in xrange(5):
                self.upload_file(self.uid, '/disk/folder/file-%d.txt' % i)
            self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/folder/subfolder'})
            for i in xrange(5):
                self.upload_file(self.uid, '/disk/folder/subfolder/file-%d.txt' % i)
            self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/test'})

        # запускаем без быстрого мува
        prepare_data()

        with mock.patch('mpfs.metastorage.mongo.collections.filesystem.POSTGRES_QUICK_MOVE_ENABLED', False):
            version_before_move = self.json_ok('user_info', {'uid': self.uid})['version']
            with capture_queue_parameters() as parameters:
                self.json_ok('async_move', {'uid': self.uid, 'src': '/disk/folder', 'dst': '/disk/test/new'})
                xiva_job_params = map(lambda p: p[0], filter(lambda p: p[1] == 'xiva', parameters))
                for p in xiva_job_params:
                    if p['class'] == 'diff':
                        etalon_xiva_diff_xiva_data = p['xiva_data']
                        break
                else:
                    self.fail('xiva diff not found')
            version_after_move = self.json_ok('user_info', {'uid': self.uid})['version']

            assert version_after_move >= version_before_move

            etalon_deltas = self.json_ok('deltas', {
                'uid': self.uid, 'base_revision': version_before_move, 'allow_quick_move_deltas': '1'
            })

            assert all(i['change_type'] != 'moved' for i in etalon_deltas['items'])

        self.json_ok('rm', {'uid': self.uid, 'path': '/disk/test'})

        # запускаем с быстрым мувом
        prepare_data()

        version_before_move = self.json_ok('user_info', {'uid': self.uid})['version']
        with capture_queue_parameters() as parameters:
            with mock.patch('mpfs.core.filesystem.base.POSTGRES_QUICK_MOVE_FOLDER_RESOURCES_LIMIT', 1024):
                self.json_ok('async_move', {'uid': self.uid, 'src': '/disk/folder', 'dst': '/disk/test/new'})
            xiva_job_params = map(lambda p: p[0], filter(lambda p: p[1] == 'xiva', parameters))
            for p in xiva_job_params:
                if p['class'] == 'diff':
                    xiva_diff_xiva_data = p['xiva_data']
                    break
            else:
                self.fail('xiva diff not found')
        version_after_move = self.json_ok('user_info', {'uid': self.uid})['version']

        assert version_after_move >= version_before_move

        deltas = self.json_ok('deltas', {
            'uid': self.uid, 'base_revision': version_before_move, 'allow_quick_move_deltas': '1'
        })

        assert all(i['change_type'] != 'moved' for i in deltas['items'])

        # сравниваем содержимое дельт

        def filter_unstable_fields(items):
            result = []
            for delta in items:
                flatten_delta = flatten_dict(delta)
                for filter_field in ('revision', 'resource_id', 'resource.ctime', 'resource.mtime', 'resource.utime'):
                    flatten_delta.pop(filter_field, None)
                result.append(flatten_delta)
            return result

        etalon_delta_items = filter_unstable_fields(etalon_deltas['items'])
        delta_items = filter_unstable_fields(deltas['items'])

        etalon_delta_items.sort(key=lambda x: x['path'])
        delta_items.sort(key=lambda x: x['path'])
        assert etalon_delta_items == delta_items

        def filter_xiva_data_fields(items):
            for i in items:
                for field in ('fid', 'sha256', 'md5', 'size'):
                    i.pop(field, None)
        filter_xiva_data_fields(etalon_xiva_diff_xiva_data)
        filter_xiva_data_fields(xiva_diff_xiva_data)

        xiva_diff_xiva_data.sort(key=lambda x: x['key'])
        etalon_xiva_diff_xiva_data.sort(key=lambda x: x['key'])

        assert etalon_xiva_diff_xiva_data == xiva_diff_xiva_data

    @pytest.mark.skipif(not INIT_USER_IN_POSTGRES, reason='Test for quick postgres move')
    def test_with_owerwrite_flag_and_existing_destination(self):
        """
        Если в async_move передать force=1, и при этом dst существует, то старый мув (медленный) мержит содержимое src
        в dst
        """
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/folder'})
        self.upload_file(self.uid, '/disk/folder/file-1.txt')

        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/another'})
        self.upload_file(self.uid, '/disk/another/file-2.txt')

        async_move_result = self.json_ok(
            'async_move',
            {'uid': self.uid, 'src': '/disk/another', 'dst': '/disk/folder', 'force': '1'}
        )

        status_result = self.json_ok('status', {'uid': self.uid, 'oid': async_move_result['oid']})
        assert status_result['status'] == 'DONE'

        self.json_error('list', {'uid': self.uid, 'path': '/disk/another'}, code=codes.LIST_NOT_FOUND)
        list_result = self.json_ok('list', {'uid': self.uid, 'path': '/disk/folder'})

        assert len(list_result) == 3
        assert {i['name'] for i in list_result} == {'folder', 'file-1.txt', 'file-2.txt'}

    @pytest.mark.skipif(not INIT_USER_IN_POSTGRES, reason='Test for quick postgres move')
    def test_folder_with_same_name_in_destination_parent_folder(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/folder'})
        self.upload_file(self.uid, '/disk/folder/file-1.txt')

        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/another'})
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/another/folder'})

        self.json_error(
            'move',
            {'uid': self.uid, 'src': '/disk/folder', 'dst': '/disk/another/folder'},
            code=codes.COPY_D2D_RESOURCE_EXISTS
        )


class QuickMoveSharedFoldersTestCase(CommonSharingMethods, DiskTestCase):
    """
    Сейчас быстрый мув для общих папок не применяется, работает по-старому, эти тесты пока что просто проверяют, что
    все работает, как раньше
    """

    def run(self, result=None):
        with mock.patch('mpfs.metastorage.mongo.collections.filesystem.POSTGRES_QUICK_MOVE_ENABLED', True), \
                mock.patch('mpfs.metastorage.mongo.collections.filesystem.POSTGRES_QUICK_MOVE_ALLOWED_UIDS', [self.uid, self.uid_3]):
            return super(QuickMoveSharedFoldersTestCase, self).run(result)

    def test_move_shared_root_folder_by_owner(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/folder'})
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/folder/subfolder'})
        self.upload_file(self.uid, '/disk/folder/subfolder/file-1.txt')

        self.create_group(self.uid, '/disk/folder/subfolder')
        self.create_user(self.uid_3)
        invite_hash = self.invite_user(self.uid_3, owner=self.uid, path='/disk/folder/subfolder')
        self.activate_invite(self.uid_3, invite_hash)

        folder_info = CollectionRoutedDatabase().user_data.find_one({'uid': self.uid, 'path': '/disk/folder/subfolder'})
        owner_version_before_move = self.json_ok('user_info', {'uid': self.uid})['version']
        owner_folder_version_before_move = folder_info['version']
        folder_info = CollectionRoutedDatabase().user_data.find_one({'uid': self.uid_3, 'path': '/disk/subfolder'})
        user_version_before_move = self.json_ok('user_info', {'uid': self.uid_3})['version']
        user_folder_version_before_move = folder_info['version']

        self.json_ok('async_move', {'uid': self.uid, 'src': '/disk/folder/subfolder', 'dst': '/disk/new'})

        self.json_ok('info', {'uid': self.uid, 'path': '/disk/new'})
        self.json_ok('info', {'uid': self.uid_3, 'path': '/disk/subfolder'})

        folder_info = CollectionRoutedDatabase().user_data.find_one({'uid': self.uid, 'path': '/disk/new'})
        owner_version_after_move = self.json_ok('user_info', {'uid': self.uid})['version']
        owner_folder_version_after_move = folder_info['version']
        folder_info = CollectionRoutedDatabase().user_data.find_one({'uid': self.uid_3, 'path': '/disk/subfolder'})
        user_version_after_move = self.json_ok('user_info', {'uid': self.uid_3})['version']
        user_folder_version_after_move = folder_info['version']

        # при перемещении корня общей папки владельцем должны измениться только версии папки и диска у владельца
        # у участников и версия папки и версия диска должны остаться прежними
        assert owner_version_after_move > owner_version_before_move
        assert owner_folder_version_after_move > owner_folder_version_before_move
        assert user_version_after_move == user_version_before_move
        assert user_folder_version_after_move == user_folder_version_before_move

        self.json_ok('diff', {'uid': self.uid, 'version': owner_version_before_move})
        self.json_ok('diff', {'uid': self.uid_3, 'version': user_version_before_move})

    def test_move_subfolder_in_shared_folder_by_owner(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/folder'})
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/folder/subfolder'})
        self.upload_file(self.uid, '/disk/folder/subfolder/file-1.txt')

        self.create_group(self.uid, '/disk/folder')
        self.create_user(self.uid_3)
        invite_hash = self.invite_user(self.uid_3, owner=self.uid, path='/disk/folder')
        self.activate_invite(self.uid_3, invite_hash)

        folder_info = CollectionRoutedDatabase().user_data.find_one({'uid': self.uid, 'path': '/disk/folder'})
        owner_version_before_move = self.json_ok('user_info', {'uid': self.uid})['version']
        owner_folder_version_before_move = folder_info['version']
        folder_info = CollectionRoutedDatabase().user_data.find_one({'uid': self.uid_3, 'path': '/disk/folder'})
        user_version_before_move = self.json_ok('user_info', {'uid': self.uid_3})['version']
        user_folder_version_before_move = folder_info['version']

        self.json_ok('async_move', {'uid': self.uid, 'src': '/disk/folder/subfolder', 'dst': '/disk/new'})

        self.json_ok('info', {'uid': self.uid, 'path': '/disk/new'})
        self.json_ok('info', {'uid': self.uid, 'path': '/disk/folder'})
        self.json_ok('info', {'uid': self.uid_3, 'path': '/disk/folder'})
        self.json_error('info', {'uid': self.uid_3, 'path': '/disk/new'}, code=codes.RESOURCE_NOT_FOUND)

        folder_info = CollectionRoutedDatabase().user_data.find_one({'uid': self.uid, 'path': '/disk/folder'})
        owner_version_after_move = self.json_ok('user_info', {'uid': self.uid})['version']
        owner_folder_version_after_move = folder_info['version']
        folder_info = CollectionRoutedDatabase().user_data.find_one({'uid': self.uid_3, 'path': '/disk/folder'})
        user_version_after_move = self.json_ok('user_info', {'uid': self.uid_3})['version']
        user_folder_version_after_move = folder_info['version']

        assert owner_version_after_move > owner_version_before_move
        assert owner_folder_version_after_move == owner_folder_version_before_move
        assert user_version_after_move > user_version_before_move
        assert user_folder_version_after_move == user_folder_version_before_move

        self.json_ok('diff', {'uid': self.uid, 'version': owner_version_before_move})
        self.json_ok('diff', {'uid': self.uid_3, 'version': user_version_before_move})

    def test_move_shared_root_folder_by_participants(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/folder'})
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/folder/subfolder'})
        self.upload_file(self.uid, '/disk/folder/subfolder/file-1.txt')

        self.create_group(self.uid, '/disk/folder/subfolder')
        self.create_user(self.uid_3)
        invite_hash = self.invite_user(self.uid_3, owner=self.uid, path='/disk/folder/subfolder')
        self.activate_invite(self.uid_3, invite_hash)

        folder_info = CollectionRoutedDatabase().user_data.find_one({'uid': self.uid, 'path': '/disk/folder/subfolder'})
        owner_version_before_move = self.json_ok('user_info', {'uid': self.uid})['version']
        owner_folder_version_before_move = folder_info['version']
        folder_info = CollectionRoutedDatabase().user_data.find_one({'uid': self.uid_3, 'path': '/disk/subfolder'})
        user_version_before_move = self.json_ok('user_info', {'uid': self.uid_3})['version']
        user_folder_version_before_move = folder_info['version']

        self.json_ok('async_move', {'uid': self.uid_3, 'src': '/disk/subfolder', 'dst': '/disk/new'})

        self.json_ok('info', {'uid': self.uid, 'path': '/disk/folder/subfolder'})
        self.json_ok('info', {'uid': self.uid_3, 'path': '/disk/new'})
        self.json_error('info', {'uid': self.uid_3, 'path': '/disk/subfolder'}, code=codes.RESOURCE_NOT_FOUND)

        folder_info = CollectionRoutedDatabase().user_data.find_one({'uid': self.uid, 'path': '/disk/folder/subfolder'})
        owner_version_after_move = self.json_ok('user_info', {'uid': self.uid})['version']
        owner_folder_version_after_move = folder_info['version']
        folder_info = CollectionRoutedDatabase().user_data.find_one({'uid': self.uid_3, 'path': '/disk/new'})
        user_version_after_move = self.json_ok('user_info', {'uid': self.uid_3})['version']
        user_folder_version_after_move = folder_info['version']

        assert owner_version_after_move == owner_version_before_move
        assert owner_folder_version_after_move == owner_folder_version_before_move
        assert user_version_after_move > user_version_before_move
        assert user_folder_version_after_move > user_folder_version_before_move

        self.json_ok('diff', {'uid': self.uid, 'version': owner_version_before_move})
        self.json_ok('diff', {'uid': self.uid_3, 'version': user_version_before_move})

    def test_move_subfolder_in_shared_folder_by_participant(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/folder'})
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/folder/subfolder'})
        self.upload_file(self.uid, '/disk/folder/subfolder/file-1.txt')

        self.create_group(self.uid, '/disk/folder')
        self.create_user(self.uid_3)
        invite_hash = self.invite_user(self.uid_3, owner=self.uid, path='/disk/folder')
        self.activate_invite(self.uid_3, invite_hash)

        folder_info = CollectionRoutedDatabase().user_data.find_one({'uid': self.uid, 'path': '/disk/folder'})
        owner_version_before_move = self.json_ok('user_info', {'uid': self.uid})['version']
        owner_folder_version_before_move = folder_info['version']
        folder_info = CollectionRoutedDatabase().user_data.find_one({'uid': self.uid_3, 'path': '/disk/folder'})
        user_version_before_move = self.json_ok('user_info', {'uid': self.uid_3})['version']
        user_folder_version_before_move = folder_info['version']

        self.json_ok('async_move', {'uid': self.uid_3, 'src': '/disk/folder/subfolder', 'dst': '/disk/folder/subfldr'})

        self.json_ok('info', {'uid': self.uid, 'path': '/disk/folder/subfldr'})
        self.json_ok('info', {'uid': self.uid_3, 'path': '/disk/folder/subfldr'})
        self.json_error('info', {'uid': self.uid, 'path': '/disk/folder/subfolder'}, code=codes.RESOURCE_NOT_FOUND)
        self.json_error('info', {'uid': self.uid_3, 'path': '/disk/folder/subfolder'}, code=codes.RESOURCE_NOT_FOUND)

        folder_info = CollectionRoutedDatabase().user_data.find_one({'uid': self.uid, 'path': '/disk/folder/subfldr'})
        owner_version_after_move = self.json_ok('user_info', {'uid': self.uid})['version']
        owner_folder_version_after_move = folder_info['version']
        user_version_after_move = self.json_ok('user_info', {'uid': self.uid_3})['version']
        user_folder_version_after_move = folder_info['version']

        assert owner_version_after_move > owner_version_before_move
        assert owner_folder_version_after_move > owner_folder_version_before_move
        assert user_version_after_move > user_version_before_move
        assert user_folder_version_after_move > user_folder_version_before_move

        self.json_ok('diff', {'uid': self.uid, 'version': owner_version_before_move})
        self.json_ok('diff', {'uid': self.uid_3, 'version': user_version_before_move})

    def test_rename_folder_with_shared_folder_inside_by_owner(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/folder'})
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/folder/subfolder'})
        self.upload_file(self.uid, '/disk/folder/subfolder/file-1.txt')

        self.create_group(self.uid, '/disk/folder/subfolder')
        self.create_user(self.uid_3)
        invite_hash = self.invite_user(self.uid_3, owner=self.uid, path='/disk/folder/subfolder')
        self.activate_invite(self.uid_3, invite_hash)

        self.json_ok('info', {'uid': self.uid_3, 'path': '/disk/subfolder/file-1.txt'})
        self.json_ok('async_move', {'uid': self.uid, 'src': '/disk/folder', 'dst': '/disk/new'})
        self.json_ok('info', {'uid': self.uid_3, 'path': '/disk/subfolder/file-1.txt'})

    def test_rename_folder_with_shared_folder_inside_by_participant(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/folder'})
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/folder/subfolder'})
        self.upload_file(self.uid, '/disk/folder/subfolder/file-1.txt')

        self.create_group(self.uid, '/disk/folder/subfolder')

        self.create_user(self.uid_3)
        self.json_ok('mkdir', {'uid': self.uid_3, 'path': '/disk/f1'})
        self.json_ok('mkdir', {'uid': self.uid_3, 'path': '/disk/f1/f2'})

        invite_hash = self.invite_user(self.uid_3, owner=self.uid, path='/disk/folder/subfolder')
        self.activate_invite(self.uid_3, invite_hash)

        self.json_ok('async_move', {'uid': self.uid_3, 'src': '/disk/subfolder', 'dst': '/disk/f1/f2/subfolder'})

        self.json_ok('info', {'uid': self.uid_3, 'path': '/disk/f1/f2/subfolder/file-1.txt'})
        self.json_ok('async_move', {'uid': self.uid_3, 'src': '/disk/f1', 'dst': '/disk/new'})
        self.json_ok('info', {'uid': self.uid_3, 'path': '/disk/new/f2/subfolder/file-1.txt'})

    def test_move_to_shared_folder(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/folder'})
        self.upload_file(self.uid, '/disk/folder/file.txt')

        shared_path = '/disk/shared_folder'
        self.create_user(self.uid_3)
        self.json_ok('mkdir', {'uid': self.uid_3, 'path': shared_path})
        self.create_share_for_guest(self.uid_3, shared_path, self.uid, self.email)

        before_info = self.json_ok('info', {'uid': self.uid, 'path': "/disk/folder", 'meta': 'resource_id'})
        self.json_ok('async_move', {'uid': self.uid, 'src': '/disk/folder', 'dst': "%s/folder" % shared_path})
        after_info = self.json_ok('info', {'uid': self.uid, 'path': "%s/folder" % shared_path, 'meta': 'resource_id'})

        after_resource_id_parts = after_info['meta']['resource_id'].split(':', 1)
        before_resource_id_parts = before_info['meta']['resource_id'].split(':', 1)
        assert after_resource_id_parts[1] == before_resource_id_parts[1]
        assert before_resource_id_parts[0] == self.uid
        assert after_resource_id_parts[0] == self.uid_3


class LastQuickMoveVersionTestCase(DiskTestCase):

    def test_get_last_quick_move_version_is_int(self):
        import mpfs.engine.process
        mpfs.engine.process.usrctl().set_last_quick_move_version(self.uid, 1)
        last_quick_move_version = mpfs.engine.process.usrctl().last_quick_move_version(self.uid)
        assert isinstance(last_quick_move_version, int)
        assert 1 == mpfs.engine.process.usrctl().last_quick_move_version(self.uid)


class QuickMoveSearchReindexTestCase(DiskTestCase):
    def setup_method(self, method):
        with mock.patch('mpfs.core.user.standart.POSTGRES_QUICK_MOVE_ENABLE_QUICK_MOVE_FOR_NEW_USERS', False):
            return super(QuickMoveSearchReindexTestCase, self).setup_method(method)

    @pytest.mark.skipif(not INIT_USER_IN_POSTGRES, reason='Test for postgres user')
    def test_start_reindex_for_postgres_user(self):
        self.service_ok('start_reindex_for_quick_move', {'uid': self.uid})

    @pytest.mark.skipif(not INIT_USER_IN_POSTGRES, reason='Test for postgres user')
    def test_reindex_callback_for_postgres_user(self):
        self.service_ok('reindex_for_quick_move_callback', {'uid': self.uid})
        assert is_reindexed_for_quick_move(self.uid) is True

    @pytest.mark.skipif(not INIT_USER_IN_POSTGRES, reason='Test for postgres user')
    def test_check_reindex_callback_for_postgres_user(self):
        reindex_result = self.service_ok('check_reindexed_for_quick_move', {'uid': self.uid})
        assert reindex_result['result']['is_reindexed'] is False

        self.service_ok('reindex_for_quick_move_callback', {'uid': self.uid})
        reindex_result = self.service_ok('check_reindexed_for_quick_move', {'uid': self.uid})
        assert reindex_result['result']['is_reindexed'] is True

    @pytest.mark.skipif(not INIT_USER_IN_POSTGRES, reason='Test for postgres user')
    def test_try_to_reindex_after_reindex_callback_for_postgres_user(self):
        self.service_ok('reindex_for_quick_move_callback', {'uid': self.uid})
        self.service_error('start_reindex_for_quick_move', {'uid': self.uid},
                           code=codes.USER_ALREADY_REINDEXED_FOR_QUICK_MOVE)

    @pytest.mark.skipif(not INIT_USER_IN_POSTGRES, reason='Test for postgres user')
    def test_quick_move_for_reindexed_user(self):
        version_before_move = self.json_ok('user_info', {'uid': self.uid})['version']
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/folder'})
        self.service_ok('reindex_for_quick_move_callback', {'uid': self.uid})

        with mock.patch('mpfs.metastorage.mongo.collections.filesystem.POSTGRES_QUICK_MOVE_ENABLED', True), \
                mock.patch('mpfs.metastorage.mongo.collections.filesystem.POSTGRES_QUICK_MOVE_ALLOW_FOR_REINDEXED_USERS', True), \
                mock.patch('mpfs.core.filesystem.base.POSTGRES_QUICK_MOVE_FOLDER_RESOURCES_LIMIT', 0):

            self.json_ok('async_move', {'uid': self.uid, 'src': '/disk/folder', 'dst': '/disk/new'})

            version_after_move = self.json_ok('user_info', {'uid': self.uid})['version']

            assert version_after_move >= version_before_move

            deltas = self.json_ok('deltas', {
                'uid': self.uid, 'base_revision': version_before_move, 'allow_quick_move_deltas': '1'
            })
            for delta in deltas['items']:
                if delta['change_type'] == 'moved':
                    assert delta['path'] == '/disk/folder'
                    assert delta['new_path'] == '/disk/new'
                    break
            else:
                self.fail('moved delta not found in deltas result')


class SearchQueriesReindexFlagTestCase(DiskTestCase):
    stubs_manager = StubsManager(class_stubs=set(StubsManager.DEFAULT_CLASS_STUBS) - {SearchDBStub} | {SearchDBTinyStub})

    def setup_method(self, method):
        with mock.patch('mpfs.core.user.standart.POSTGRES_QUICK_MOVE_ENABLE_QUICK_MOVE_FOR_NEW_USERS', False):
            return super(SearchQueriesReindexFlagTestCase, self).setup_method(method)

    @pytest.mark.skipif(not INIT_USER_IN_POSTGRES, reason='Test for postgres user')
    def test_search_pushes_for_reindexed_user(self):
        self.service_ok('reindex_for_quick_move_callback', {'uid': self.uid})

        with SearchIndexerStub() as index_service:
            self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/folder'})

            for call_args in index_service.push_change.call_args_list:
                indexer_data = call_args[0][0]
                for push in indexer_data:
                    assert 'is_reindexed_for_quick_move' in push
                    assert push['is_reindexed_for_quick_move'] is True

    @pytest.mark.skipif(not INIT_USER_IN_POSTGRES, reason='Test for postgres user')
    def test_search_pushes_for_not_reindexed_user(self):
        with SearchIndexerStub() as index_service:
            self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/folder'})

            for call_args in index_service.push_change.call_args_list:
                indexer_data = call_args[0][0]
                for push in indexer_data:
                    assert 'is_reindexed_for_quick_move' not in push

    @pytest.mark.skipif(not INIT_USER_IN_POSTGRES, reason='Test for postgres user')
    def test_reindexed_flag_in_search_queries_for_reindexed_user(self):
        self.service_ok('reindex_for_quick_move_callback', {'uid': self.uid})

        with DiskSearchStub() as search_service:
            self.json_ok('new_search', {'uid': self.uid, 'path': '/disk', 'query': 'folder'})

            for call_args in search_service.open_url.call_args_list:
                url = call_args[0][0]
                assert 'fast-moved' in url

        with SearchDBTinyStub() as search_db_service:
            self.json_ok('dir_size', {'uid': self.uid, 'path': '/disk'})

            for call_args in search_db_service.open_url.call_args_list:
                url = call_args[0][0]
                assert 'fast-moved' in url

    @pytest.mark.skipif(not INIT_USER_IN_POSTGRES, reason='Test for postgres user')
    def test_reindexed_flag_in_search_queries_for_not_reindexed_user(self):
        with DiskSearchStub() as search_service:
            self.json_ok('new_search', {'uid': self.uid, 'path': '/disk', 'query': 'folder'})

            for call_args in search_service.open_url.call_args_list:
                url = call_args[0][0]
                assert 'fast-moved' not in url

        with SearchDBTinyStub() as search_db_service:
            self.json_ok('dir_size', {'uid': self.uid, 'path': '/disk'})

            for call_args in search_db_service.open_url.call_args_list:
                url = call_args[0][0]
                assert 'fast-moved' not in url

    @pytest.mark.skipif(not INIT_USER_IN_POSTGRES, reason='Test for postgres user')
    def test_dir_size_indexer_response_for_quick_moved_user(self):
        self.service_ok('reindex_for_quick_move_callback', {'uid': self.uid})
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/folder'})

        info_result = self.json_ok('info', {'uid': self.uid, 'path': '/disk/folder', 'meta': 'resource_id'})
        folder_size = 123
        files_count = 321
        search_folder_size_response = {
            info_result['meta']['resource_id']: {
                'size': folder_size,
                'count': files_count
            }
        }

        with mock.patch('mpfs.core.services.search_service.SearchDB.open_url',
                        return_value=to_json(search_folder_size_response)):
            dir_size_result = self.json_ok('dir_size', {'uid': self.uid, 'path': '/disk/folder'})
            assert dir_size_result['path'] == '/disk/folder'
            assert dir_size_result['files_count'] == files_count
            assert dir_size_result['size'] == folder_size

    @parameterized.expand([
        (True,),
        (False,),
    ])
    @pytest.mark.skipif(not INIT_USER_IN_POSTGRES, reason='Test for postgres user')
    def test_user_init_for_new_user(self, enable_quick_move):
        new_user_uid = '123321123'
        user_check_result = self.json_ok('user_check', {'uid': new_user_uid})
        assert user_check_result['need_init'] == '1'

        with mock.patch('mpfs.core.user.standart.POSTGRES_QUICK_MOVE_ENABLE_QUICK_MOVE_FOR_NEW_USERS', enable_quick_move), \
                SearchIndexerStub() as stub:
            self.json_ok('user_init', {'uid': new_user_uid, 'shard': 'pg'})
            if enable_quick_move:
                mkdir_names = set()
                for args, kwargs in stub.push_change.call_args_list:
                    data = args[0]
                    if not isinstance(data, list):
                        data = [data]
                    for item in data:
                        assert item['operation'] == 'mkdir'
                        mkdir_names.add(item['name'])

                assert len(mkdir_names) == 3
                assert 'disk' in mkdir_names
                assert 'trash' in mkdir_names
                assert 'hidden' in mkdir_names

        with SearchIndexerStub() as stub:
            self.upload_file(new_user_uid, '/attach/123')

            if enable_quick_move:
                mkdir_names = set()
                for args, kwargs in stub.push_change.call_args_list:
                    data = args[0]
                    if not isinstance(data, list):
                        data = [data]
                    for item in data:
                        name = item['name']
                        mkdir_names.add(name)
                        if name == 'attach':
                            assert item['operation'] == 'mkdir'

                assert len(mkdir_names) >= 1
                assert 'attach' in mkdir_names

        reindex_result = self.service_ok('check_reindexed_for_quick_move', {'uid': new_user_uid})
        assert reindex_result['result']['is_reindexed'] is enable_quick_move

    @parameterized.expand([
        (True,),
        (False,),
    ])
    @pytest.mark.skipif(not INIT_USER_IN_POSTGRES, reason='Test for postgres user')
    def test_user_init_for_already_registered_user(self, quick_move_was_enabled):
        if quick_move_was_enabled:
            self.service_ok('reindex_for_quick_move_callback', {'uid': self.uid})

        user_check_result = self.json_ok('user_check', {'uid': self.uid})
        assert user_check_result['need_init'] == '0'

        with mock.patch('mpfs.core.user.standart.POSTGRES_QUICK_MOVE_ENABLE_QUICK_MOVE_FOR_NEW_USERS', True):
            self.json_ok('user_init', {'uid': self.uid, 'shard': 'pg'})

        reindex_result = self.service_ok('check_reindexed_for_quick_move', {'uid': self.uid})
        assert reindex_result['result']['is_reindexed'] is quick_move_was_enabled


class PublicResourcesMoveTestCase(CommonSharingMethods, DiskTestCase):
    def test_public_links_after_regular_oldschool_move(self):
        with mock.patch('mpfs.metastorage.mongo.collections.filesystem.POSTGRES_QUICK_MOVE_ENABLED', False):
            self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/folder'})
            self.upload_file(self.uid, '/disk/folder/file.txt')

            set_public_result = self.json_ok('set_public', {'uid': self.uid, 'path': '/disk/folder/file.txt'})
            public_info_result = self.json_ok('public_info', {'uid': self.uid, 'private_hash': set_public_result['hash']})

            assert public_info_result['resource']['name'] == 'file.txt'

            self.json_ok('async_move', {'uid': self.uid, 'src': '/disk/folder', 'dst': '/disk/new-folder'})

            self.json_ok('info', {'uid': self.uid, 'path': '/disk/new-folder'})
            public_info_result_after_move = self.json_ok(
                'public_info',
                {'uid': self.uid, 'private_hash': set_public_result['hash']}
            )
            assert public_info_result_after_move['resource']['name'] == 'file.txt'

    @pytest.mark.skipif(not INIT_USER_IN_POSTGRES, reason='Test for postgres user')
    def test_public_links_after_quick_move_in_postgres(self):
        with mock.patch('mpfs.metastorage.mongo.collections.filesystem.POSTGRES_QUICK_MOVE_ENABLED', True), \
                 mock.patch('mpfs.metastorage.mongo.collections.filesystem.POSTGRES_QUICK_MOVE_ALLOWED_UIDS', [self.uid, ]):
            self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/folder'})
            self.upload_file(self.uid, '/disk/folder/file.txt')

            set_public_result = self.json_ok('set_public', {'uid': self.uid, 'path': '/disk/folder/file.txt'})
            public_info_result = self.json_ok('public_info', {'uid': self.uid, 'private_hash': set_public_result['hash']})

            assert public_info_result['resource']['name'] == 'file.txt'

            self.json_ok('async_move', {'uid': self.uid, 'src': '/disk/folder', 'dst': '/disk/new-folder'})

            self.json_ok('info', {'uid': self.uid, 'path': '/disk/new-folder'})
            public_info_result_after_move = self.json_ok(
                'public_info',
                {'uid': self.uid, 'private_hash': set_public_result['hash']}
            )
            assert public_info_result_after_move['resource']['name'] == 'file.txt'

    def test_move_to_shared_folder(self):
        with mock.patch('mpfs.metastorage.mongo.collections.filesystem.POSTGRES_QUICK_MOVE_ENABLED', True), \
             mock.patch('mpfs.metastorage.mongo.collections.filesystem.POSTGRES_QUICK_MOVE_ALLOWED_UIDS', [self.uid, ]):
            self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/folder'})
            self.upload_file(self.uid, '/disk/folder/file.txt')
            set_public_result = self.json_ok('set_public', {'uid': self.uid, 'path': '/disk/folder/file.txt'})
            public_info_result = self.json_ok('public_info', {'uid': self.uid, 'private_hash': set_public_result['hash']})
            assert public_info_result['resource']['name'] == 'file.txt'

            shared_path = '/disk/shared_folder'
            self.create_user(self.uid_3)
            self.json_ok('mkdir', {'uid': self.uid_3, 'path': shared_path})
            self.create_share_for_guest(self.uid_3, shared_path, self.uid, self.email)

            self.json_ok('async_move', {'uid': self.uid, 'src': '/disk/folder', 'dst': "%s/folder" % shared_path})

            public_info_result = self.json_ok('public_info', {'private_hash': set_public_result['hash']})
            assert public_info_result['resource']['name'] == 'file.txt'

    def test_move_from_shared_folder_to_disk(self):
        with mock.patch('mpfs.metastorage.mongo.collections.filesystem.POSTGRES_QUICK_MOVE_ENABLED', True), \
             mock.patch('mpfs.metastorage.mongo.collections.filesystem.POSTGRES_QUICK_MOVE_ALLOWED_UIDS', [self.uid, ]):

            shared_path = '/disk/shared_folder'
            self.create_user(self.uid_3)
            self.json_ok('mkdir', {'uid': self.uid_3, 'path': shared_path})
            self.upload_file(self.uid_3, '%s/file.txt' % shared_path)
            set_public_result = self.json_ok('set_public', {'uid': self.uid_3, 'path': '%s/file.txt' % shared_path})
            public_info_result = self.json_ok('public_info', {'private_hash': set_public_result['hash']})
            assert public_info_result['resource']['name'] == 'file.txt'
            self.create_share_for_guest(self.uid_3, shared_path, self.uid, self.email)

            self.json_ok('async_move', {'uid': self.uid, 'src': '%s/file.txt' % shared_path, 'dst': "/disk/file.txt"})

            public_info_result = self.json_ok('public_info', {'private_hash': set_public_result['hash']})
            assert public_info_result['resource']['name'] == 'file.txt'


@pytest.mark.skipif(not INIT_USER_IN_POSTGRES, reason='Test for postgres user')
class QuickMoveUtilsTestCase(CommonSharingMethods, DiskTestCase):

    def test_max_depth_calculation(self):
        assert calculate_max_folder_depth(self.uid) == 1

        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/1'})
        assert calculate_max_folder_depth(self.uid) == 2

        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/2'})
        assert calculate_max_folder_depth(self.uid) == 2

        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/1/2'})
        assert calculate_max_folder_depth(self.uid) == 3

        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/1/3'})
        assert calculate_max_folder_depth(self.uid) == 3

        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/1/2/3'})
        assert calculate_max_folder_depth(self.uid) == 4

    def test_max_depth_calculation_for_path(self):
        assert calculate_max_folder_depth_for_path(self.uid, '/') == 1

        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/1'})
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/1/2'})
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/1/2/3'})

        assert calculate_max_folder_depth_for_path(self.uid, '/') == 4
        assert calculate_max_folder_depth_for_path(self.uid, '/disk') == 3
        assert calculate_max_folder_depth_for_path(self.uid, '/disk/1/2') == 1

    def test_max_depth_for_shared_folder_participants(self):
        shared_path = '/disk/shared_folder'
        self.create_user(self.uid_3)

        self.json_ok('mkdir', {'uid': self.uid_3, 'path': shared_path})
        self.json_ok('mkdir', {'uid': self.uid_3, 'path': shared_path + '/1'})
        self.json_ok('mkdir', {'uid': self.uid_3, 'path': shared_path + '/1/2'})

        assert calculate_max_folder_depth(self.uid_3) == 4
        assert calculate_max_folder_depth(self.uid) == 1

        self.create_share_for_guest(self.uid_3, shared_path, self.uid, self.email)

        assert calculate_max_folder_depth(self.uid_3) == 4
        assert calculate_max_folder_depth(self.uid) == 4

        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/new'})
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/new/very'})
        self.json_ok('move', {'uid': self.uid, 'src': '/disk/shared_folder', 'dst': '/disk/new/very/participant'})

        assert calculate_max_folder_depth(self.uid_3) == 4
        assert calculate_max_folder_depth(self.uid) == 6


class JavaProxyQuickMoveTestCase(DiskTestCase):

    @parameterized.expand([
        ('move',),
        ('async_move',),
    ])
    def test_move_proxy_ok(self, handler_name):
        folder_path = '/disk/folder'
        new_folder_path = '/disk/new'
        self.json_ok('mkdir', {'uid': self.uid, 'path': folder_path})

        with mock.patch('mpfs.frontend.api.disk.JAVA_DJFS_API_PROXY_MOVE_ENABLED', True), \
                DjfsApiMockHelper.mock_request():
            self.json_ok(handler_name, {'uid': self.uid, 'src': folder_path, 'dst': new_folder_path})

        # проверяем, что мок позвался и реального мува через мпфс не произошло
        self.json_error('info', {'uid': self.uid, 'path': new_folder_path}, code=codes.RESOURCE_NOT_FOUND)

    @parameterized.expand([
        ('move',),
        ('async_move',),
    ])
    def test_move_proxy_not_implemented(self, handler_name):
        folder_path = '/disk/folder'
        new_folder_path = '/disk/new'
        self.json_ok('mkdir', {'uid': self.uid, 'path': folder_path})

        with mock.patch('mpfs.frontend.api.disk.JAVA_DJFS_API_PROXY_MOVE_ENABLED', True), \
                DjfsApiMockHelper.mock_request(status_code=DJFS_NOT_IMPLEMENTED_HTTP_CODE):
            self.json_ok(handler_name, {'uid': self.uid, 'src': folder_path, 'dst': new_folder_path})

        # проверяем, что если прокси возвращает 422 (Not implemented), то мпфс сам выполняет мув
        self.json_ok('info', {'uid': self.uid, 'path': new_folder_path})

    @parameterized.expand([
        ('trash_append',),
        ('async_trash_append',),
    ])
    def test_trash_append_proxy_ok(self, handler_name):
        folder_path = '/disk/folder'
        self.json_ok('mkdir', {'uid': self.uid, 'path': folder_path})

        with mock.patch('mpfs.frontend.api.disk.JAVA_DJFS_API_PROXY_TRASH_APPEND_ENABLED', True), \
                DjfsApiMockHelper.mock_request():
            self.json_ok(handler_name, {'uid': self.uid, 'path': folder_path})

        # проверяем, что мок позвался и реального мува через мпфс не произошло
        self.json_ok('info', {'uid': self.uid, 'path': folder_path})

    @parameterized.expand([
        ('trash_append',),
        ('async_trash_append',),
    ])
    def test_trash_append_proxy_not_implemented(self, handler_name):
        folder_path = '/disk/folder'
        self.json_ok('mkdir', {'uid': self.uid, 'path': folder_path})

        with mock.patch('mpfs.frontend.api.disk.JAVA_DJFS_API_PROXY_TRASH_APPEND_ENABLED', True), \
                DjfsApiMockHelper.mock_request(status_code=DJFS_NOT_IMPLEMENTED_HTTP_CODE):
            self.json_ok(handler_name, {'uid': self.uid, 'path': folder_path})

        # проверяем, что если прокси возвращает 422 (Not implemented), то мпфс сам выполняет мув
        self.json_error('info', {'uid': self.uid, 'path': folder_path}, code=codes.RESOURCE_NOT_FOUND)


@pytest.mark.skipif(not INIT_USER_IN_POSTGRES, reason='Test for postgres user')
class TrashAppendRefactorringTestCase(DiskTestCase, TrashTestCaseMixin):

    def run(self, result=None):
        with enable_experiment_for_uid('new_trash_drop_and_restore', self.uid):
            return super(TrashAppendRefactorringTestCase, self).run(result)

    def test_trash_append_of_subroot_folder(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/folder'})
        self.upload_file(self.uid, '/disk/folder/file-1.txt')
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/folder/subfolder'})
        self.upload_file(self.uid, '/disk/folder/subfolder/file-2.txt')

        self.json_ok('trash_append', {'uid': self.uid, 'path': '/disk/folder'})
        trashed_folder = self.get_trashed_item('/trash/folder')['path']

        session = Session.create_from_uid(self.uid)
        for data in session.execute(SQL_FILES_BY_UID, {'uid': self.uid, 'root_folder': '/trash'}).fetchall():
            dao_item = FileDAOItem.create_from_pg_data(data)
            assert dao_item.trash_append_time is None
            assert dao_item.original_path is None
            assert 'original_parent_id' not in dao_item.custom_setprop_fields

        trash_folders = set()

        for data in session.execute(SQL_FOLDERS_BY_UID, {'uid': self.uid, 'root_folder': '/trash'}).fetchall():
            dao_item = FolderDAOItem.create_from_pg_data(data)
            trash_folders.add(dao_item.path)
            if dao_item.path == trashed_folder + '/subfolder':
                assert dao_item.trash_append_time is None
                assert dao_item.original_path is None
                if dao_item.custom_setprop_fields is not None:
                    assert 'original_parent_id' not in dao_item.custom_setprop_fields
            elif dao_item.path == trashed_folder:
                assert dao_item.trash_append_time is not None
                assert dao_item.original_path == '/disk/folder'
                assert dao_item.custom_setprop_fields['original_parent_id'] == '/disk/'

        assert trash_folders >= {trashed_folder, trashed_folder + '/subfolder'}

    def test_trash_append_of_deep_folder(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/folder'})
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/folder/subfolder'})
        self.upload_file(self.uid, '/disk/folder/subfolder/file-1.txt')
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/folder/subfolder/deep'})
        self.upload_file(self.uid, '/disk/folder/subfolder/deep/file-2.txt')

        self.json_ok('trash_append', {'uid': self.uid, 'path': '/disk/folder/subfolder'})
        trashed_folder = self.get_trashed_item('/trash/subfolder')['path']
        session = Session.create_from_uid(self.uid)
        for data in session.execute(SQL_FILES_BY_UID, {'uid': self.uid, 'root_folder': '/trash'}).fetchall():
            dao_item = FileDAOItem.create_from_pg_data(data)
            assert dao_item.trash_append_time is None
            assert dao_item.original_path is None
            assert 'original_parent_id' not in dao_item.custom_setprop_fields

        trash_folders = set()

        for data in session.execute(SQL_FOLDERS_BY_UID, {'uid': self.uid, 'root_folder': '/trash'}).fetchall():
            dao_item = FolderDAOItem.create_from_pg_data(data)
            trash_folders.add(dao_item.path)
            if dao_item.path == trashed_folder + '/deep':
                assert dao_item.trash_append_time is None
                assert dao_item.original_path is None
                if dao_item.custom_setprop_fields is not None:
                    assert 'original_parent_id' not in dao_item.custom_setprop_fields
            elif dao_item.path == trashed_folder:
                assert dao_item.trash_append_time is not None
                assert dao_item.original_path == '/disk/folder/subfolder'
                assert dao_item.custom_setprop_fields['original_parent_id'] == '/disk/folder/'

        assert trash_folders >= {trashed_folder, trashed_folder+'/deep'}

    def test_trash_append_of_subroot_file(self):
        self.upload_file(self.uid, '/disk/file-1.txt')

        self.json_ok('trash_append', {'uid': self.uid, 'path': '/disk/file-1.txt'})

        session = Session.create_from_uid(self.uid)
        for data in session.execute(SQL_FILES_BY_UID, {'uid': self.uid, 'root_folder': '/trash'}).fetchall():
            dao_item = FileDAOItem.create_from_pg_data(data)
            assert dao_item.trash_append_time is not None
            assert dao_item.original_path == '/disk/file-1.txt'
            assert dao_item.custom_setprop_fields['original_parent_id'] == '/disk/'

    def test_trash_append_of_deep_file(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/folder'})
        self.upload_file(self.uid, '/disk/folder/file-1.txt')

        self.json_ok('trash_append', {'uid': self.uid, 'path': '/disk/folder/file-1.txt'})

        session = Session.create_from_uid(self.uid)
        for data in session.execute(SQL_FILES_BY_UID, {'uid': self.uid, 'root_folder': '/trash'}).fetchall():
            dao_item = FileDAOItem.create_from_pg_data(data)
            assert dao_item.trash_append_time is not None
            assert dao_item.original_path == '/disk/folder/file-1.txt'
            assert dao_item.custom_setprop_fields['original_parent_id'] == '/disk/folder/'

    def test_trash_clean_for_subtree(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/folder'})
        self.upload_file(self.uid, '/disk/folder/file-1.txt')
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/folder/subfolder'})
        self.upload_file(self.uid, '/disk/folder/subfolder/file-2.txt')

        with time_machine(datetime.datetime.now() - datetime.timedelta(days=31)):
            self.json_ok('trash_append', {'uid': self.uid, 'path': '/disk/folder'})

        session = Session.create_from_uid(self.uid)
        for data in session.execute(SQL_FILES_BY_UID, {'uid': self.uid, 'root_folder': '/trash'}).fetchall():
            dao_item = FileDAOItem.create_from_pg_data(data)
            assert dao_item.trash_append_time is None
            assert dao_item.original_path is None
            assert 'original_parent_id' not in dao_item.custom_setprop_fields

        process_queue()

        trash_files = session.execute(SQL_FILES_BY_UID, {'uid': self.uid, 'root_folder': '/trash'}).fetchall()
        assert len(trash_files) == 0

        for data in session.execute(SQL_FILES_BY_UID, {'uid': self.uid, 'root_folder': '/hidden'}).fetchall():
            dao_item = FileDAOItem.create_from_pg_data(data)

            assert dao_item.trash_append_time is None
            assert dao_item.original_path is None
            assert 'original_parent_id' not in dao_item.custom_setprop_fields

            assert dao_item.trash_clean_time is not None

    def test_success_trash_restore_folder(self):
        self.json_ok('mkdir', {'path': '/disk/test_folder', 'uid': self.uid})
        self.upload_file(self.uid, '/disk/test_folder/1.txt')
        self.json_ok('mkdir', {'path': '/disk/test_folder/inner_folder', 'uid': self.uid})
        self.upload_file(self.uid, '/disk/test_folder/inner_folder/2.png')

        self.json_ok('trash_append', {'path': '/disk/test_folder', 'uid': self.uid})
        trashed_item = self.get_trashed_item('/trash/test_folder')
        session = Session.create_from_uid(self.uid)
        for data in session.execute(SQL_FILES_BY_UID, {'uid': self.uid, 'root_folder': '/trash'}).fetchall():
            dao_item = FileDAOItem.create_from_pg_data(data)
            assert dao_item.trash_append_time is None
            assert dao_item.original_path is None
            assert 'original_parent_id' not in dao_item.custom_setprop_fields

        self.json_ok('trash_restore', {'path': trashed_item['path'], 'uid': self.uid})
        folder_contents = self.json_ok('list', {'uid': self.uid, 'path': '/disk/test_folder', 'meta': ''})
        folder_contents.extend(
            self.json_ok('list', {'uid': self.uid, 'path': '/disk/test_folder/inner_folder', 'meta': ''})
        )
        for item in folder_contents:
            assert item['id'] in (
                '/disk/test_folder/',
                '/disk/test_folder/inner_folder/',
                '/disk/test_folder/1.txt',
                '/disk/test_folder/inner_folder/2.png',
            )
