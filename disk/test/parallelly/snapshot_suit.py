# -*- coding: utf-8 -*-
import os

import datetime
import pytest
import mock

from itertools import count
from random import randint
from mock import patch
from nose_parameterized import parameterized

from test.base import time_machine
from test.common.sharing import CommonSharingMethods
from test.conftest import REAL_MONGO, INIT_USER_IN_POSTGRES

from mpfs.common.static import codes
from mpfs.common.errors import SnapshotSharedFoldersTimeOut
from mpfs.config import settings
from mpfs.core.address import Address
from mpfs.core.factory import get_resource
from mpfs.core.filesystem.dao.legacy import CollectionRoutedDatabase
from mpfs.core.services import disk_service
from mpfs.core.snapshot.logic.snapshot import (SnapshotIterationKey, BaseSnapshotChunkSerializer,
                                               IndexerSnapshotChunkSerializer)
from mpfs.metastorage.mongo.util import decompress_data, compress_data
from mpfs.metastorage.mongo.collections import filesystem
from mpfs.dao.session import Session

# цикл. импорт
import mpfs.core.snapshot.logic.snapshot


SETPROP_SYMLINK_FIELDNAME = settings.system['setprop_symlink_fieldname']
SNAPSHOT_CURRENT_PROTOCOL_VERSION = settings.snapshot['current_protocol_version']


class BaseSnapshotTestCase(CommonSharingMethods):
    MAX_RESOURCES_COUNT = 1000
    SHARED_FOLDER = '/disk/Shared'

    def setup_method(self, method):
        super(BaseSnapshotTestCase, self).setup_method(method)
        self.create_user(self.uid, noemail=True, shard='disk_test_mongodb-unit1')
        self.json_ok('mkdir', {'uid': self.uid, 'path': self.SHARED_FOLDER})

    def traverse_with_snapshot(self, uid, snapshot_endpoint='snapshot', limit=3, protocol_version=SNAPSHOT_CURRENT_PROTOCOL_VERSION):
        counter = 0
        with patch.object(mpfs.core.snapshot.logic.snapshot, 'SNAPSHOT_CHUNK_SIZE', new=limit), \
                patch.object(mpfs.core.snapshot.logic.snapshot, 'SNAPSHOT_CURRENT_PROTOCOL_VERSION', protocol_version):
            response = self.json_ok(snapshot_endpoint, {'uid': uid}, json={'iteration_key': ""})
            counter += 1
            snapshot = response['items']
            while response['iteration_key']:
                response = self.json_ok(snapshot_endpoint, {'uid': uid},
                                        json={'iteration_key': response['iteration_key']})
                counter += 1
                snapshot.extend(response['items'])
        return snapshot, counter

    def _take_until_folder(self, docs):
        result = []
        it = iter(docs)
        while True:
            try:
                doc = next(it)
            except StopIteration:
                break
            result.append(doc)
            if self._is_folder(doc):
                break
        return result

    @staticmethod
    def _is_folder(doc):
        return doc['type'] == 'dir'

    def _get_doc_ids(self, docs):
        return map(self._get_doc_id, docs)

    @staticmethod
    def _get_doc_id(doc):
        return doc['_id']

    @staticmethod
    def _get_doc_key(doc):
        return doc['key']

    @staticmethod
    def _get_file_id(doc):
        return doc['data']['meta']['file_id']

    def _remove_by_key(self, key):
        self.json_ok('rm', {'uid': self.uid, 'path': key})

    def _filter_with_uploaded_files(self, snapshot):
        uploaded_file_paths = {file_['path'].split(':')[-1] for file_ in self.get_uploaded_files()}
        return [doc for doc in snapshot if doc[1]['key'] in uploaded_file_paths]


class SnapshotTestCase(BaseSnapshotTestCase):

    def setup_method(self, method):
        super(SnapshotTestCase, self).setup_method(method)
        for i in range(5):
            self.upload_file(self.uid, '/disk/path_%d' % i)

    @parameterized.expand([
        (2,),
    ])
    def test_snapshot_chunk_fields(self, protocol_version):
        with patch.object(mpfs.core.snapshot.logic.snapshot, 'SNAPSHOT_CURRENT_PROTOCOL_VERSION', protocol_version):
            resp = self.json_ok('snapshot', {'uid': self.uid})
            assert not resp.viewkeys() ^ {'iteration_key', 'revision', 'items'}

    @parameterized.expand([
        (2,),
    ])
    def test_system_folders_have_resource_id(self, protocol_version):
        with patch.object(mpfs.core.snapshot.logic.snapshot, 'SNAPSHOT_CHUNK_SIZE', new=1000), \
                patch.object(mpfs.core.snapshot.logic.snapshot, 'SNAPSHOT_CURRENT_PROTOCOL_VERSION', protocol_version):
            resp = self.json_ok('snapshot', {'uid': self.uid})
            assert resp
            for resource in resp['items']:
                if Address(resource['path'], uid=self.uid).is_system:
                    assert resource['meta']['resource_id']

    @parameterized.expand([
        (2,),
    ])
    @pytest.mark.skipif(not REAL_MONGO and not INIT_USER_IN_POSTGRES, reason='Doesn\'t work on mongo mock')
    def test_trash_files_in_indexer_snapshot(self, protocol_version):
        files_count = 5
        for i in range(files_count):
            self.json_ok('trash_append', {'uid': self.uid, 'path': '/disk/path_%d' % i})

        with patch.object(mpfs.core.snapshot.logic.snapshot, 'SNAPSHOT_CHUNK_SIZE', new=1000), \
                patch.object(mpfs.core.snapshot.logic.snapshot, 'SNAPSHOT_CURRENT_PROTOCOL_VERSION', protocol_version):
            resp = self.json_ok('indexer_snapshot', {'uid': self.uid})
            trash_resources_count = 0
            for resource in resp['items']:
                if Address(resource['path'], uid=self.uid).storage_name == 'trash':
                    trash_resources_count += 1
        assert files_count + 1 == trash_resources_count, u'Должный отдать <количество файлов> + 1 (/trash) ресурсов'

    @parameterized.expand([
        (2,),
    ])
    def test_attach_subfolders_without_mtime_in_indexer_snapshot(self, protocol_version):
        attach_subfolder_path = '/attach/archive'
        with mock.patch.dict('mpfs.config.settings.folders', {'/attach': {'allow_folders': True}}):
            self.json_ok('mkdir', {'uid': self.uid, 'path': attach_subfolder_path})

        from mpfs.core.filesystem.dao.resource import AttachDAO
        subfolder_data = AttachDAO().find_one({'uid': self.uid, 'path': attach_subfolder_path})
        subfolder_data['data'].pop('mtime')
        subfolder_data['parent'] = '/attach'
        AttachDAO().update({'uid': self.uid, 'path': attach_subfolder_path}, subfolder_data)

        with patch.object(mpfs.core.snapshot.logic.snapshot, 'SNAPSHOT_CHUNK_SIZE', new=1000), \
                patch.object(mpfs.core.snapshot.logic.snapshot, 'SNAPSHOT_CURRENT_PROTOCOL_VERSION', protocol_version):
            response = self.json_ok('indexer_snapshot', {'uid': self.uid})
            snapshot = response['items']
            while response['iteration_key']:
                response = self.json_ok('indexer_snapshot', {'uid': self.uid},
                                        json={'iteration_key': response['iteration_key']})
                snapshot.extend(response['items'])

            for item in snapshot:
                if item['path'] == attach_subfolder_path:
                    break
            else:
                self.fail('Attach subfolder item not found in indexer snapshot')

    @parameterized.expand([
        (2,),
    ])
    def test_attach_subfolders_without_mtime_and_ctime_in_indexer_snapshot(self, protocol_version):
        attach_subfolder_path = '/attach/archive'
        with mock.patch.dict('mpfs.config.settings.folders', {'/attach': {'allow_folders': True}}):
            self.json_ok('mkdir', {'uid': self.uid, 'path': attach_subfolder_path})

        from mpfs.core.filesystem.dao.resource import AttachDAO
        subfolder_data = AttachDAO().find_one({'uid': self.uid, 'path': attach_subfolder_path})
        subfolder_data['data'].pop('mtime')
        subfolder_data['zdata'] = decompress_data(subfolder_data['zdata'])
        subfolder_data['zdata']['meta'].pop('ctime')
        subfolder_data['zdata'] = compress_data(subfolder_data['zdata'])
        subfolder_data['parent'] = '/attach'
        AttachDAO().update({'uid': self.uid, 'path': attach_subfolder_path}, subfolder_data)

        with patch.object(mpfs.core.snapshot.logic.snapshot, 'SNAPSHOT_CHUNK_SIZE', new=1000), \
             patch.object(mpfs.core.snapshot.logic.snapshot, 'SNAPSHOT_CURRENT_PROTOCOL_VERSION', protocol_version):
            response = self.json_ok('indexer_snapshot', {'uid': self.uid})
            snapshot = response['items']
            while response['iteration_key']:
                response = self.json_ok('indexer_snapshot', {'uid': self.uid},
                                        json={'iteration_key': response['iteration_key']})
                snapshot.extend(response['items'])

            for item in snapshot:
                if item['path'] == attach_subfolder_path:
                    break
            else:
                self.fail('Attach subfolder item not found in indexer snapshot')

    @parameterized.expand([
        (2,),
    ])
    def test_resources_have_revisions(self, protocol_version):
        with patch.object(mpfs.core.snapshot.logic.snapshot, 'SNAPSHOT_CURRENT_PROTOCOL_VERSION', protocol_version):
            resp = self.json_ok('snapshot', {'uid': self.uid})
        assert all(long(r['meta']['revision']) >= 0 for r in resp['items'])

    @parameterized.expand([
        (2,),
    ])
    @pytest.mark.skipif(not REAL_MONGO,
                        reason='https://st.yandex-team.ru/CHEMODAN-34246')
    def test_basic_snapshot_with_pagination(self, protocol_version):
        LIMIT = 3
        with patch.object(mpfs.core.snapshot.logic.snapshot, 'SNAPSHOT_CHUNK_SIZE', new=LIMIT), \
                patch.object(mpfs.core.snapshot.logic.snapshot, 'SNAPSHOT_CURRENT_PROTOCOL_VERSION', protocol_version):
            response = self.json_ok('snapshot', {'uid': self.uid}, json={'iteration_key': ""})
            snapshot = response['items']
            while response['iteration_key']:
                response = self.json_ok('snapshot', {'uid': self.uid},
                                        json={'iteration_key': response['iteration_key']})
                assert len(response['items']) <= LIMIT
                snapshot.extend(response['items'])

        file_ids = self._find_all_docs_except_root(self.uid)
        assert len(file_ids) == len(snapshot)

        sorted_keys = sorted([item['key'] for item in file_ids])
        sorted_paths = sorted([item['path'] for item in snapshot])
        assert sorted_keys == sorted_paths

    @parameterized.expand([
        (2,),
    ])
    @pytest.mark.skipif(not REAL_MONGO,
                        reason='https://st.yandex-team.ru/CHEMODAN-34246')
    def test_snapshot_contains_group_folder(self, protocol_version):
        self.create_share_for_guest(self.uid, self.SHARED_FOLDER, self.uid_3, self.email_3)

        with patch.object(mpfs.core.snapshot.logic.snapshot, 'SNAPSHOT_CHUNK_SIZE', new=1000), \
                patch.object(mpfs.core.snapshot.logic.snapshot, 'SNAPSHOT_CURRENT_PROTOCOL_VERSION', protocol_version):
            resp = self.json_ok('snapshot', {'uid': self.uid})
            assert resp
            assert len(resp['items']) == len(self._find_all_docs_except_root(self.uid))

    @parameterized.expand([
        (2,),
    ])
    def test_error_on_infinite_loop(self, protocol_version):
        u"""Протестировать что обрабатывается случай когда два
        последовательных запроса вернули один и тот же file_id.

        Считаем что в этом случае произошло зацикливание выкачки снепшота.
        """
        LIMIT = 3
        chunk = disk_service.Disk().find_snapshot_chunk(self.uid, '', [], '', LIMIT)
        with patch.object(disk_service.Disk, 'find_snapshot_chunk', return_value=chunk) as mocked, \
                patch.object(mpfs.core.snapshot.logic.snapshot, 'SNAPSHOT_CURRENT_PROTOCOL_VERSION', protocol_version):
            resp = self.json_ok('snapshot', {'uid': self.uid})
            assert mocked.called

            iteration_key = resp['iteration_key']
            self.json_error('snapshot',
                            {'uid': self.uid},
                            json={'iteration_key': iteration_key},
                            code=codes.SNAPSHOT_INFINITE_LOOP_DETECTED)
            assert mocked.called

    @parameterized.expand([
        (2,),
    ])
    def test_snapshot_contains_shared_folders(self, protocol_version):
        u"""Протестировать получение снепшота вместе с общими папками.

         Проверить соответсвие значений полей.
        """

        # Создать шаренную папку
        self.create_share_for_guest(self.uid, self.SHARED_FOLDER, self.uid_3, self.email_3)
        # Переименовать ее для гостя
        new_shared_folder = '/disk/Shared2'
        self.json_ok('move', {'uid': self.uid_3, 'src': self.SHARED_FOLDER, 'dst': new_shared_folder})
        # Зполнить ее ресурсами у владельца
        self.create_subtree(self.uid, self.SHARED_FOLDER)

        # Получить снепшот
        snapshot, _ = self.traverse_with_snapshot(self.uid_3, limit=5, protocol_version=protocol_version)
        assert snapshot
        assert not any(item for item in snapshot if item['path'] == '/')

        # Проверить целостность и соответствие значений атрибутов
        resources = [get_resource(self.uid_3, item['path']) for item in snapshot]
        for i, resource in enumerate(resources):
            item = snapshot[i]
            if resource.resource_id is not None:
                assert item['meta']['resource_id'] == resource.resource_id.serialize()
            assert item['meta']['revision'] == long(resource.version)
            assert item['path'] == resource.visible_address.path
            assert item['type'] == resource.type
            assert item['mtime'] == resource.mtime
            if resource.type == 'file':
                assert item['meta']['size'] == resource.size
                assert item['meta']['md5'] == resource.md5()
                assert item['meta']['sha256'] == resource.sha256()

    @parameterized.expand([
        (2,),
    ])
    @pytest.mark.skipif(not REAL_MONGO,
                        reason='https://st.yandex-team.ru/CHEMODAN-34246')
    def test_snapshot_empty_disk(self, protocol_version):
        u"""Протестировать получение снепшота дял пользователя,
        у которго среди файлов только `/` и `/disk`.

        Это баг с продакшна: https://st.yandex-team.ru/CHEMODAN-33724
        """

        for doc in self._find_all_docs_except_root(self.uid):
            if doc['key'] != '/disk':
                self.json_ok('rm', {'uid': self.uid, 'path': doc['key']})

        snapshot, _ = self.traverse_with_snapshot(self.uid, limit=5, protocol_version=protocol_version)
        assert snapshot

    @parameterized.expand([
        (2,),
    ])
    def test_indexer_snapshot_returns_current_time_as_revision(self, protocol_version):
        fake_cur_timestamp = 1000
        with time_machine(datetime.datetime.fromtimestamp(fake_cur_timestamp)), \
                patch.object(mpfs.core.snapshot.logic.snapshot, 'SNAPSHOT_CURRENT_PROTOCOL_VERSION', protocol_version):
            resp = self.json_ok('indexer_snapshot', {'uid': self.uid})
        assert resp['revision'] == int(('%f' % fake_cur_timestamp).replace('.', ''))

    @parameterized.expand([
        (2,),
    ])
    def test_snapshot_returns_user_index_version_as_revision(self, protocol_version):
        fake_cur_timestamp = 1000
        with time_machine(datetime.datetime.fromtimestamp(fake_cur_timestamp)), \
                patch.object(mpfs.core.snapshot.logic.snapshot, 'SNAPSHOT_CURRENT_PROTOCOL_VERSION', protocol_version):
            resp = self.json_ok('snapshot', {'uid': self.uid})
        user_info = self.json_ok('user_info', {'uid': self.uid})
        assert resp['revision'] == int(user_info['version'])

    @parameterized.expand([
        (2,),
    ])
    def test_snapshot_doesnt_fetch_photounlim_files(self, protocol_version):
        self.json_ok('enable_unlimited_autouploading', {'uid': self.uid})
        photounlim_file_path = '/photounlim/photounlim_file.jpg'
        self.upload_file(self.uid, photounlim_file_path)

        snapshot, _ = self.traverse_with_snapshot(self.uid, protocol_version=protocol_version)
        assert not ({'/photounlim', photounlim_file_path} & {x['path'] for x in snapshot})

    @parameterized.expand([
        (2,),
    ])
    @pytest.mark.skipif(not INIT_USER_IN_POSTGRES, reason='https://st.yandex-team.ru/CHEMODAN-48639')
    def test_avoid_infinite_loop_if_last_file_in_chunk_is_disk_root(self, protocol_version):
        u"""Тест во имя бага https://st.yandex-team.ru/CHEMODAN-48639"""
        self.create_user(self.uid_3)
        self.json_ok('mkdir', {'uid': self.uid_3, 'path': '/disk/test'})
        from mpfs.core.filesystem.dao.resource import ResourceDAO
        # намеренно делаем так, чтобы в snapshot /disk отдавался последним элементом, но генерация file_id ставила бы
        # его раньше всех по порядку
        for r in ResourceDAO().find({'uid': self.uid_3}):
            if r['key'] == '/disk':
                r['data'] = {'file_id': 'f' * 64, 'visible': 1}
                r['parent'] = '/'
                r['zdata'] = {'setprop': {u'file_id_zipped': True}}  # /disk имеет такой вид в проде
                ResourceDAO().update({'uid': self.uid_3, 'path': '/disk'}, r)
            elif r['key'] == '/disk/test':
                r['data']['file_id'] = 'a' * 64
                r['parent'] = '/disk'
                ResourceDAO().update({'uid': self.uid_3, 'path': '/disk/test'}, r)
        with patch('mpfs.core.filesystem.resources.base.Resource.generate_file_id', return_value='0' * 64), \
                patch.object(mpfs.core.snapshot.logic.snapshot, 'SNAPSHOT_CURRENT_PROTOCOL_VERSION', protocol_version):
            response = self.json_ok('snapshot', {'uid': self.uid_3}, json={'iteration_key': ""})
            response = self.json_ok('snapshot', {'uid': self.uid_3}, json={'iteration_key': response['iteration_key']})
        #  проверяем, что не зациклились
        assert not response['iteration_key']

    @staticmethod
    def _find_all_docs_except_root(uid):
        resources = filesystem.UserDataCollection().find_all({'uid': uid})
        result = []
        for r in resources:
            if r['key'] == '/':
                continue
            result.append(r)
        return result

    @parameterized.expand([
        (2,),
    ])
    def test_share_folder_info_is_present(self, protocol_version):
        FOLDER = '/disk/test'
        self.create_share_for_guest(self.uid, self.SHARED_FOLDER, self.uid_1, self.email_1)
        self.json_ok('mkdir', {'uid': self.uid, 'path': FOLDER})
        self.json_ok('mkdir', {'uid': self.uid_1, 'path': FOLDER})
        with patch.object(mpfs.core.snapshot.logic.snapshot, 'SNAPSHOT_CURRENT_PROTOCOL_VERSION', protocol_version):
            owner_snapshot = self.json_ok('snapshot', {'uid': self.uid})
            guest_snapshot = self.json_ok('snapshot', {'uid': self.uid_1})

        assert owner_snapshot
        assert guest_snapshot

        for doc in owner_snapshot['items']:
            if doc['path'] == self.SHARED_FOLDER:
                assert doc['meta']['group']
                assert doc['meta']['group']['is_owned'] == 1
                assert 'rights' not in doc['meta']['group']
            else:
                assert 'group' not in doc['meta']

        for doc in guest_snapshot['items']:
            if doc['path'] == self.SHARED_FOLDER:
                assert doc['meta']['group']
                assert doc['meta']['group']['is_owned'] == 0
                assert 'rights' in doc['meta']['group']
            else:
                assert 'group' not in doc['meta']

    @parameterized.expand([
        (2,),
    ])
    def test_symlink_field_is_present_for_resources_with_symlink_in_setprop(self, protocol_version):
        SYMLINK_VALUE = '%25disk%25jntjq9ajpn7h65f3%25test508/12345'
        dir_path = '/disk/test'
        self.create_user(self.uid_3)
        self.json_ok('mkdir', {'uid': self.uid_3, 'path': dir_path})
        self.json_ok('setprop', {
            'uid': self.uid_3,
            'path': dir_path,
            SETPROP_SYMLINK_FIELDNAME: SYMLINK_VALUE
        })
        with patch.object(mpfs.core.snapshot.logic.snapshot, 'SNAPSHOT_CURRENT_PROTOCOL_VERSION', protocol_version):
            snapshot = self.json_ok('snapshot', {'uid': self.uid_3})
        for item in snapshot['items']:
            if item['path'] == dir_path:
                assert item['meta']['discsw_symlink'] == SYMLINK_VALUE
            else:
                assert 'discsw_symlink' not in item['meta']

    def test_not_sending_session_id_wont_add_it_to_iteration_key(self):
        raw_iter_key = self.json_ok('snapshot', {'uid': self.uid})['iteration_key']
        iter_key = SnapshotIterationKey.parse(raw_iter_key)
        assert iter_key.get_uid() is None
        assert iter_key.get_session_id() is None

        raw_iter_key = self.json_ok('snapshot', {'uid': self.uid}, json={'iteration_key': raw_iter_key})['iteration_key']
        iter_key = SnapshotIterationKey.parse(raw_iter_key)
        assert iter_key.get_uid() is None
        assert iter_key.get_session_id() is None

    def test_providing_session_id_and_uid_to_iterator_key(self):
        session_id = '2134567'

        raw_iter_key = self.json_ok('snapshot', {'uid': self.uid, 'session_id': session_id})['iteration_key']
        iter_key = SnapshotIterationKey.parse(raw_iter_key)
        assert iter_key.get_uid() == self.uid
        assert iter_key.get_session_id() == session_id

        raw_iter_key = self.json_ok('snapshot', {'uid': self.uid}, json={'iteration_key': raw_iter_key})['iteration_key']
        iter_key = SnapshotIterationKey.parse(raw_iter_key)
        assert iter_key.get_uid() == self.uid
        assert iter_key.get_session_id() == session_id


class SnapshotSharedFolderPagination(BaseSnapshotTestCase):

    def setup_method(self, method):
        super(SnapshotSharedFolderPagination, self).setup_method(method)
        self.owner_shared_folder_a = '/disk/shared_a'
        self.guest_shared_folder_a = '/disk/shared2_a'
        self.owner_shared_folder_b = '/disk/shared_b'
        self.guest_shared_folder_b = '/disk/shared2_b'

    @parameterized.expand([
        (1, 8, 7),
        (2, 7, 6),
        (3, 4, 4),
        (4, 4, 4),
        (5, 4, 4),
    ])
    def test_snapshot_for_several_shared_folders_several_folders(self, step_size, correct_number_of_iterations_pg, correct_number_of_iterations_mongo):
        correct_snapshot_paths = {
            '/disk/shared2_b',
            '/disk/shared2_a',
            '/disk/shared2_a/nested',
            '/disk/shared2_a/1.txt',
            '/disk/shared2_a/nested/1.txt',
            '/disk/shared2_b/nested',
            '/disk/shared2_b/1.txt',
            '/disk/shared2_b/nested/1.txt',
        }
        if INIT_USER_IN_POSTGRES or REAL_MONGO:
            correct_snapshot_paths.add('/disk')

        for owner_path, guest_path in [
            (self.owner_shared_folder_a, self.guest_shared_folder_a),
            (self.owner_shared_folder_b, self.guest_shared_folder_b),
        ]:
            self.json_ok('mkdir', {'uid': self.uid, 'path': owner_path})
            self.create_share_for_guest(self.uid, owner_path, self.uid_3, self.email_3)
            self.json_ok('move', {'uid': self.uid_3, 'src': owner_path, 'dst': guest_path})
            self.json_ok('mkdir', {'uid': self.uid, 'path': owner_path + '/nested'})
            self.upload_file(self.uid, owner_path + '/1.txt')
            self.upload_file(self.uid, owner_path + '/nested/1.txt')

        snapshot, number_of_iterations = self.traverse_with_snapshot(self.uid_3, limit=step_size, protocol_version=2)
        assert snapshot
        assert not any(item for item in snapshot if item['path'] == '/')
        assert correct_snapshot_paths == {i['path'] for i in snapshot}
        correct_number_of_iterations = correct_number_of_iterations_pg if INIT_USER_IN_POSTGRES else correct_number_of_iterations_mongo
        assert correct_number_of_iterations == number_of_iterations

    @parameterized.expand([
        (1, 6, 5),
        (2, 5, 5),
        (3, 4, 4),
        (4, 4, 4),
        (5, 3, 3),
    ])
    def test_snapshot_for_several_shared_folders_without_duplicate_file_ids(self, step_size, correct_number_of_iterations_pg, correct_number_of_iterations_mongo):
        correct_snapshot_paths = {
            '/disk/shared2_a',
            '/disk/shared2_a/nested_1',
            '/disk/shared2_a/nested_1/1.txt',
            '/disk/shared2_a/nested_2',
            '/disk/shared2_a/nested_2/1.txt',
            '/disk/shared2_a/1.txt',
        }
        if INIT_USER_IN_POSTGRES or REAL_MONGO:
            correct_snapshot_paths.add('/disk')

        self.json_ok('mkdir', {'uid': self.uid, 'path': self.owner_shared_folder_a})
        self.create_share_for_guest(self.uid, self.owner_shared_folder_a, self.uid_3, self.email_3)
        self.json_ok('move', {'uid': self.uid_3, 'src': self.owner_shared_folder_a, 'dst': self.guest_shared_folder_a})

        self.json_ok('mkdir', {'uid': self.uid, 'path': self.owner_shared_folder_a + '/nested_1'})
        self.upload_file(self.uid, self.owner_shared_folder_a + '/nested_1/1.txt')
        self.json_ok('mkdir', {'uid': self.uid, 'path': self.owner_shared_folder_a + '/nested_2'})
        self.upload_file(self.uid, self.owner_shared_folder_a + '/nested_2/1.txt')
        self.upload_file(self.uid, self.owner_shared_folder_a + '/1.txt')

        snapshot, number_of_iterations = self.traverse_with_snapshot(self.uid_3, limit=step_size, protocol_version=2)
        assert snapshot
        assert not any(item for item in snapshot if item['path'] == '/')
        assert correct_snapshot_paths == {i['path'] for i in snapshot}
        correct_number_of_iterations = correct_number_of_iterations_pg if INIT_USER_IN_POSTGRES else correct_number_of_iterations_mongo
        assert correct_number_of_iterations == number_of_iterations

    @parameterized.expand([
        (1, 5, 4),
        (2, 4, 4),
        (3, 4, 4),
        (4, 4, 4),
        (5, 3, 3),
    ])
    def test_snapshot_for_several_shared_folders_with_dup_file_ids(self, step_size, correct_number_of_iterations_pg, correct_number_of_iterations_mongo):
        correct_snapshot_paths = {
            '/disk/shared2_a',
            '/disk/shared2_a/nested_1',
            '/disk/shared2_a/nested_1/1.txt',
            '/disk/shared2_a/nested_2',
            '/disk/shared2_a/nested_2/1.txt',
            '/disk/shared2_a/1.txt',
        }
        if INIT_USER_IN_POSTGRES or REAL_MONGO:
            correct_snapshot_paths.add('/disk')

        self.json_ok('mkdir', {'uid': self.uid, 'path': self.owner_shared_folder_a})
        self.create_share_for_guest(self.uid, self.owner_shared_folder_a, self.uid_3, self.email_3)
        self.json_ok('move', {'uid': self.uid_3, 'src': self.owner_shared_folder_a, 'dst': self.guest_shared_folder_a})

        self.json_ok('mkdir', {'uid': self.uid, 'path': self.owner_shared_folder_a + '/nested_1'})
        self.upload_file(self.uid, self.owner_shared_folder_a + '/nested_1/1.txt')
        self.json_ok('mkdir', {'uid': self.uid, 'path': self.owner_shared_folder_a + '/nested_2'})
        self.upload_file(self.uid, self.owner_shared_folder_a + '/nested_2/1.txt')
        self.upload_file(self.uid, self.owner_shared_folder_a + '/1.txt')

        from mpfs.core.filesystem.dao.resource import ResourceDAO
        file_id = ResourceDAO().find({'uid': self.uid, 'path': self.owner_shared_folder_a + '/nested_1'}).next()['data']['file_id']
        r = ResourceDAO().find({'uid': self.uid, 'path': self.owner_shared_folder_a + '/nested_2'}).next()
        r['key'] = self.owner_shared_folder_a + '/nested_2'
        r['data']['file_id'] = file_id
        r['data']['visible'] = 1
        r['parent'] = self.owner_shared_folder_a
        r['zdata'] = {'setprop': {u'file_id_zipped': True}}
        ResourceDAO().update({'uid': self.uid, 'path': self.owner_shared_folder_a + '/nested_2'}, r)

        snapshot, number_of_iterations = self.traverse_with_snapshot(self.uid_3, limit=step_size, protocol_version=2)
        assert snapshot
        assert not any(item for item in snapshot if item['path'] == '/')
        assert correct_snapshot_paths == {i['path'] for i in snapshot}
        correct_number_of_iterations = correct_number_of_iterations_pg if INIT_USER_IN_POSTGRES else correct_number_of_iterations_mongo
        assert correct_number_of_iterations == number_of_iterations


class IndexerSnapshotTestCase(BaseSnapshotTestCase):

    def setup_method(self, method):
        super(IndexerSnapshotTestCase, self).setup_method(method)
        self.path = '/disk/test_file'
        self.upload_file(self.uid, self.path)

    @parameterized.expand([
        (2,),
    ])
    @pytest.mark.skipif(not INIT_USER_IN_POSTGRES, reason='Pg implementation, do not test it on Mongo')
    def test_snapshot_chunk_fields_in_pg(self, protocol_version):
        file_doc = CollectionRoutedDatabase().user_data.find_one({'uid': self.uid, 'key': self.path})
        session = Session.create_from_uid(self.uid)
        with session.begin():
            sql_remove_media_type_by_fid = 'UPDATE disk.files SET media_type=NULL WHERE uid=:uid AND fid=:fid;'
            session.execute(sql_remove_media_type_by_fid, {'fid': file_doc['_id'], 'uid': self.uid})
        snapshot, _ = self.traverse_with_snapshot(self.uid, snapshot_endpoint='indexer_snapshot', protocol_version=protocol_version)
        assert [x for x in snapshot if x['path'] == self.path][0]['meta']['media_type'] == 'unknown'

    @parameterized.expand([
        (2,),
    ])
    def test_return_400_for_uninitialized_users(self, protocol_version):
        with patch.object(mpfs.core.snapshot.logic.snapshot, 'SNAPSHOT_CURRENT_PROTOCOL_VERSION', protocol_version):
            self.json_error('indexer_snapshot', {'uid': 'fake_uid'})
        assert self.response.status == 400

    @parameterized.expand([
        (2,),
    ])
    @pytest.mark.skipif(not REAL_MONGO and not INIT_USER_IN_POSTGRES, reason='Doesn\'t work on mongo mock')
    def test_phonounlim_files_are_fetched_by_indexer_snapshot(self, protocol_version):
        self.json_ok('enable_unlimited_autouploading', {'uid': self.uid})
        photounlim_file_path = '/photounlim/photounlim_file.jpg'
        self.upload_file(self.uid, photounlim_file_path)

        snapshot, _ = self.traverse_with_snapshot(self.uid, snapshot_endpoint='indexer_snapshot', protocol_version=protocol_version)
        assert ({'/photounlim', photounlim_file_path, '/disk/test_file', '/disk/Shared', '/disk', '/trash'}
                == {x['path'] for x in snapshot})

    @parameterized.expand([
        (2,),
    ])
    @pytest.mark.skipif(not REAL_MONGO and not INIT_USER_IN_POSTGRES, reason='Doesn\'t work on mongo mock')
    def test_shared_files_are_fetched_by_indexer_snapshot(self, protocol_version):
        # Создать шаренную папку
        self.create_share_for_guest(self.uid, self.SHARED_FOLDER, self.uid_3, self.email_3)
        shared_file_path = '/disk/Shared/shared_file.jpg'
        self.upload_file(self.uid, shared_file_path)

        # Получить снепшот
        snapshot, _ = self.traverse_with_snapshot(self.uid_3, snapshot_endpoint='indexer_snapshot', limit=5, protocol_version=protocol_version)
        assert ({shared_file_path, '/disk/Shared', '/disk', '/trash'}
                == {x['path'] for x in snapshot})

    @parameterized.expand([
        (2,),
    ])
    @pytest.mark.skipif(not REAL_MONGO,
                        reason='https://st.yandex-team.ru/CHEMODAN-34246')
    def test_both_shared_and_photounlim_are_fetched_by_indexer_snapshot(self, protocol_version):
        shared_file_path = '/disk/Shared/shared_file.jpg'
        self.create_share_for_guest(self.uid, self.SHARED_FOLDER, self.uid_3, self.email_3)
        self.upload_file(self.uid, shared_file_path)

        self.json_ok('enable_unlimited_autouploading', {'uid': self.uid_3})
        photounlim_file_path = '/photounlim/photounlim_file.jpg'
        self.upload_file(self.uid_3, photounlim_file_path)

        snapshot, _ = self.traverse_with_snapshot(self.uid_3, snapshot_endpoint='indexer_snapshot', limit=5, protocol_version=protocol_version)
        assert ({shared_file_path, '/disk/Shared', '/disk', '/photounlim', photounlim_file_path, '/trash'}
                == {x['path'] for x in snapshot})


class FindSnapshotFromSubtreeTestCase(BaseSnapshotTestCase):

    @parameterized.expand([
        (2,),
    ])
    def test_find_snapshot_from_shared(self, protocol_version):
        subtree = self.create_subtree(self.uid, root_folder=self.SHARED_FOLDER)
        doc = filesystem.UserDataCollection().find_one_by_field(self.uid, {'path': subtree[0]})
        parent = self._get_doc_key(doc)
        if protocol_version == 2:
            subtree_documents, _ = disk_service.Disk().find_snapshot_from_subtree(self.uid, parent, 60, items_limit=20)
        subtree.pop(0)  # the root
        assert sorted([doc['key'] for _, doc in subtree_documents]) == sorted(subtree)

    @parameterized.expand([
        (2,),
    ])
    def test_find_snapshot_from_shared_timed_out(self, protocol_version):
        self.upload_file(self.uid, '/disk/Shared/test.txt')
        doc = filesystem.UserDataCollection().find_one_by_field(self.uid, {'path': '/disk'})
        parent = self._get_doc_key(doc)
        with self.assertRaises(SnapshotSharedFoldersTimeOut):
            if protocol_version == 2:
                subtree_documents, _ = disk_service.Disk().find_snapshot_from_subtree(self.uid, parent, time_limit=0, items_limit=20)


class BaseSnapshotChunkSerializerTestCase(BaseSnapshotTestCase):

    def test_file(self):
        self.upload_file(self.uid, '/disk/test.txt')
        doc = filesystem.UserDataCollection().find_one_by_field(self.uid, {'path': '/disk/test.txt'})
        assert doc
        doc = disk_service.Disk().get_unpacked_documents_with_fixed_file_id([doc])[0]
        doc = BaseSnapshotChunkSerializer().serialize([doc[1]])[0]
        referenced_resource = disk_service.Disk().get_resource(self.uid, Address.Make(self.uid, doc['path']))

        assert doc['path'] == referenced_resource.path
        assert doc['type'] == referenced_resource.type
        assert doc['mtime'] == referenced_resource.mtime
        assert doc['meta']['resource_id'] == referenced_resource.resource_id.serialize()
        assert doc['meta']['md5'] == referenced_resource.md5()
        assert doc['meta']['sha256'] == referenced_resource.sha256()
        assert doc['meta']['size'] == referenced_resource.size
        assert referenced_resource.version is not None
        assert doc['meta']['revision'] == long(referenced_resource.version)

    def test_folder(self):
        doc = filesystem.UserDataCollection().find_one_by_field(self.uid, {'path': '/disk'})
        assert doc
        doc = disk_service.Disk().get_unpacked_documents_with_fixed_file_id([doc])[0]
        doc = BaseSnapshotChunkSerializer().serialize([doc[1]])[0]
        referenced_resource = disk_service.Disk().get_resource(self.uid, Address.Make(self.uid, doc['path']))

        assert doc['path'] == referenced_resource.path
        if referenced_resource.resource_id is None:
            assert doc['meta']['resource_id']
        else:
            assert doc['meta']['resource_id'] == referenced_resource.resource_id.serialize()
        assert referenced_resource.version is not None
        assert doc['meta']['revision'] == long(referenced_resource.version)


class IndexerSnapshotChunkSerializerTestCase(BaseSnapshotTestCase):

    def test_file(self):
        self.upload_file(self.uid, '/disk/test.txt')
        doc = filesystem.UserDataCollection().find_one_by_field(self.uid, {'path': '/disk/test.txt'})
        assert doc
        doc = disk_service.Disk().get_unpacked_documents_with_fixed_file_id([doc])[0]
        serialized = IndexerSnapshotChunkSerializer().serialize([doc[1]])[0]
        referenced_resource = disk_service.Disk().get_resource(self.uid, Address.Make(self.uid, serialized['path']))

        assert serialized['meta']['stid'] == referenced_resource.file_mid()
        assert serialized['meta']['media_type'] == referenced_resource.media_type

    def test_folder(self):
        doc = filesystem.UserDataCollection().find_one_by_field(self.uid, {'path': '/disk'})
        assert doc
        doc = disk_service.Disk().get_unpacked_documents_with_fixed_file_id([doc])[0]
        serialized = IndexerSnapshotChunkSerializer().serialize([doc[1]])[0]
        assert 'stid' not in serialized['meta']
        assert 'media_type' not in serialized['meta']
