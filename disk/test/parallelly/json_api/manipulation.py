# -*- coding: utf-8 -*-
import multiprocessing
import re
import mock
import urlparse
import time

from datetime import datetime
from functools import partial

from helpers.utils import filter_dict
from nose_parameterized import parameterized

from mpfs.common.static import codes
from mpfs.core.address import ResourceId
from mpfs.core.filesystem.dao.resource import ResourceDAO
from mpfs.core.user.constants import PHOTOUNLIM_AREA_PATH
from mpfs.metastorage.mongo.collections.filesystem import UserDataCollection
from test.base import DiskTestCase, time_machine

from test.parallelly.json_api.base import CommonJsonApiTestCase

from mpfs.core.metastorage.control import hidden_data


class ManipulationJsonApiTestCase(CommonJsonApiTestCase):

    def test_make_folder(self):
        opts = {'uid': self.uid, 'path': '/disk/folder_with_file'}
        self.json_ok('mkdir', opts)
        self.json_ok('list', opts)
        opts = {'uid': self.uid, 'path': '/disk/folder_with_folder/'}
        self.json_ok('mkdir', opts)
        self.json_ok('list', opts)

        opts = {'uid': self.uid, 'path': '/disk/folder_with_folder/child_folder'}
        self.json_ok('mkdir', opts)
        self.json_ok('list', opts)

        self.upload_file(self.uid, '/disk/folder_with_file/file.txt')
        opts = {
            'uid': self.uid,
            'path': '/disk/folder_with_file/file.txt',
        }
        self.json_ok('info', opts)

        opts = {
            'uid': self.uid,
            'path': '/disk',
            'parents': 1,
        }
        tree_result = self.json_ok('tree', opts)

        self.assertEqual(len(tree_result['resource']), 1)
        self.assertEqual(tree_result['resource'][0]['id'], '/disk/')
        self.assertEqual(len(tree_result['resource'][0]['resource']), 2)

        opts = {
            'uid': self.uid,
            'path': '/disk',
            'meta': 'hasfolders',
        }
        tree_result = self.json_ok('tree', opts)
        self.assertTrue(tree_result['resource'][0]['resource'][1]['hasfolders'], 1)

    def test_make_folder_exists(self):
        opts = {'uid': self.uid, 'path': '/disk/test'}
        self.json_ok('mkdir', opts)
        self.json_ok('list', opts)

        opts = {'uid': self.uid, 'path': '/disk/test/New folder'}
        self.json_ok('mkdir', opts)
        self.json_ok('list', opts)

        opts = {'uid': self.uid, 'path': '/disk/test/New folder'}
        check_response = self.json_error('mkdir', opts, code=codes.MKDIR_EXISTS)
        self.assertIn('data', check_response)
        self.assertIn('autosuffix_path', check_response['data'])
        self.assertEqual(check_response['data']['autosuffix_path'], '/disk/test/New folder (1)')
        for i in range(1, 11):
            opts = {'uid': self.uid, 'path': '/disk/test/New folder'}
            response = self.json_error('mkdir', opts, code=codes.MKDIR_EXISTS)
            self.assertIn('autosuffix_path', response['data'])
            self.assertEqual(response['data']['autosuffix_path'], '/disk/test/New folder (%s)' % i)
            opts['path'] = response['data']['autosuffix_path']
            self.json_ok('mkdir', opts)
            self.json_ok('list', opts)

        self.upload_file(self.uid, '/disk/test/file')
        self.json_ok('info', {'uid': self.uid, 'path': '/disk/test/file'})

        opts = {'uid': self.uid, 'path': '/disk/test/file'}
        file_response = self.json_error('mkdir', opts, code=codes.MKDIR_EXISTS)
        self.assertIn('data', file_response)
        self.assertIn('autosuffix_path', file_response['data'])
        self.assertEqual(file_response['data']['autosuffix_path'], '/disk/test/file (1)')

    def test_mksysdir(self):
        opts = {
            'uid': self.uid,
            'type': 'downloads',
        }
        result = self.json_ok('mksysdir', opts)
        self.assertEqual(result['id'], u'/disk/Загрузки/')

    def test_simultaneous_mksysdir(self):
        # проверяем тут, что два одновременных вызова создания системной папки не приведут к ошибке в одном из них про
        # то, что папка уже создана

        class TestProcess(multiprocessing.Process):
            uid = self.uid
            json_ok = self.json_ok
            assertEqual = self.assertEqual

            def __init__(self):
                super(TestProcess, self).__init__()
                self.exception = None

            def run(self):
                try:
                    opts = {
                        'uid': self.uid,
                        'type': 'downloads',
                    }
                    result = self.json_ok('mksysdir', opts)
                    self.assertEqual(result['id'], u'/disk/Загрузки/')
                except Exception, e:
                    self.exception = e

        th1 = TestProcess()
        th2 = TestProcess()

        th1.start()
        th2.start()

        th1.join()
        th2.join()

        assert not th1.exception
        assert not th2.exception

    def test_bulk_info(self):
        resources = (
            ('/disk/batch_folder', 'dir'),
            ('/disk/batch_folder/subfolder', 'dir'),
            ('/disk/tmp', 'dir'),
            ('/disk/batch_folder/1.txt', 'file'),
            ('/disk/batch_folder/2.txt', 'file'),
            ('/disk/batch_folder/subfolder/3.txt', 'file'),
        )
        for path, r_type in resources:
            if r_type == 'dir':
                self.json_ok('mkdir', {'uid': self.uid, 'path': path})
            else:
                self.upload_file(self.uid, path)

        bulk_info = self.json_ok('bulk_info', {'uid': self.uid}, json=[r[0] for r in resources])
        assert len(bulk_info) == len(resources)
        assert [i['path'] for i in bulk_info] == [i[0] for i in resources]
        assert [i['type'] for i in bulk_info] == [i[1] for i in resources]
        self.json_error('bulk_info', {'uid': self.uid}, json={r[0]: r[1] for r in resources})

    def test_bulk_info_in_the_same_folder(self):
        resources = (
            ('/disk/folder/1.txt', 'file'),
            ('/disk/folder/2.txt', 'file'),
            ('/disk/folder/3.txt', 'file'),
            ('/disk/folder/4.txt', 'file'),
            ('/disk/folder/5.txt', 'file'),
            ('/disk/folder/folder-1', 'dir'),
            ('/disk/folder/folder-2', 'dir'),
            ('/disk/folder/folder-3', 'dir'),
            ('/disk/folder/folder-4', 'dir')
        )

        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/folder'})
        for path, res_type in resources:
            if res_type == 'file':
                self.upload_file(self.uid, path)
            else:
                self.json_ok('mkdir', {'uid': self.uid, 'path': path})

        bulk_info = self.json_ok('bulk_info', {'uid': self.uid}, json=[r[0] for r in resources])

        assert len(bulk_info) == len(resources)
        assert [i['path'] for i in bulk_info] == [i[0] for i in resources]
        assert [i['type'] for i in bulk_info] == [i[1] for i in resources]

    def test_bulk_info_in_the_different_folders(self):
        resources = (
            ('/disk/folder/1.txt', 'file'),
            ('/disk/folder/2.txt', 'file'),
            ('/disk/folder/3.txt', 'file'),
            ('/disk/folder1/4.txt', 'file'),
            ('/disk/folder1/5.txt', 'file'),
            ('/disk/folder/folder-1', 'dir'),
            ('/disk/folder/folder-2', 'dir'),
            ('/disk/folder1/folder-3', 'dir'),
            ('/disk/folder2/folder-4', 'dir')
        )

        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/folder'})
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/folder1'})
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/folder2'})
        for path, res_type in resources:
            if res_type == 'file':
                self.upload_file(self.uid, path)
            else:
                self.json_ok('mkdir', {'uid': self.uid, 'path': path})

        bulk_info = self.json_ok('bulk_info', {'uid': self.uid}, json=[r[0] for r in resources])

        assert len(bulk_info) == len(resources)
        assert [i['path'] for i in bulk_info] == [i[0] for i in resources]
        assert [i['type'] for i in bulk_info] == [i[1] for i in resources]

    def test_info_for_not_existent_sets_check_task(self):
        path = '/disk/1.jpg'
        file_id = '1' * 64
        now = datetime.now()
        search_open_mocker = mock.patch(
            'mpfs.core.services.search_service.SearchDB.open_url',
            return_value='{"hitsCount":1,"hitsArray":[{"key":"%s","id":"%s","etime":1,"ctime":1,"mimetype":"image/jpeg"}]}' % (path, file_id))
        search_index_push_mocker = mock.patch('mpfs.core.services.index_service.SearchIndexer.push_change')
        feature_toggle_mock = mock.patch('mpfs.core.base.FEATURE_NOTIFY_PHOTOSLICE_INDEX_WHEN_FILE_NOT_FOUND', True)
        photoslice_notification_mock = mock.patch(
            'mpfs.core.job_handlers.indexer.INDEXER_PHOTOSLICE_NOTIFICATION_ON_INDEXER_SIDE', True)

        with search_index_push_mocker as search_index_push_mock, \
                search_open_mocker as search_open_mock, \
                time_machine(now), \
                feature_toggle_mock, \
                photoslice_notification_mock:
            self.json_error('info', {'uid': self.uid, 'path': path}, code=71)

        assert 1 == search_open_mock.call_count
        push_call_args = search_index_push_mock.call_args_list
        filter_fields = partial(filter_dict, fields=['version', 'action', 'operation', 'resource_id', 'metric'])
        push_data = [filter_fields(args) for call in push_call_args for args in call[0][0]]
        expected_version = int(time.mktime(now.timetuple())) * 1000000
        expected_push_data = [
            {
                'action': 'delete',
                'operation': 'rm',
                'resource_id': '%s:%s' % (self.uid, file_id),
                'version': expected_version,
                'metric': '404_info_repair',
            },
        ]
        assert expected_push_data == push_data

        expected_push_kwargs = {'wait_search_response': False, 'append_smartcache_callback': True,
                                 'service': 'photoslice'}
        push_kwargs = [call[1] for call in push_call_args]
        assert [expected_push_kwargs] == push_kwargs

        expected_push_kwargs = {'wait_search_response': False, 'append_smartcache_callback': True,
                                 'service': 'photoslice'}
        push_kwargs = [call[1] for call in push_call_args]
        assert [expected_push_kwargs] == push_kwargs

    def test_bulk_info_for_not_existent_sets_check_task(self):
        resource_exists = '/disk/f1.jpg'
        resource_renamed_orig_name = '/disk/f2.jpg'
        resource_renamed_new_name = '/disk/f22.jpg'
        resource_not_exists = '/disk/f3.jpg' # есть в индексе, нет на диске
        resource_unknown = '/disk/f5.jpg' # нет на диске и в индексе

        self.upload_file(self.uid, resource_exists, file_data={'mimetype': 'image/jpeg'})
        self.upload_file(self.uid, resource_renamed_new_name, file_data={'mimetype': 'image/jpeg'})

        resource_exists_file_id = self.json_ok('info', {'uid': self.uid, 'path': resource_exists, 'meta': ''})['meta']['file_id']
        resource_renamed_info = self.json_ok('info', {'uid': self.uid, 'path': resource_renamed_new_name, 'meta': ''})
        resource_renamed_file_id = resource_renamed_info['meta']['file_id']
        resource_renamed_version = resource_renamed_info['meta']['revision']
        resource_not_exists_file_id = '1' * 64

        search_calls = []
        def open_url_mock(url, **kwargs):
            path = dict(urlparse.parse_qs(urlparse.urlparse(url).query))['key'][0]
            search_calls.append(path)

            return {
                resource_exists:
                    '{"hitsCount":1,"hitsArray":[{"key":"%s","id":"%s","etime":1,"ctime":1,"mimetype":"image/jpeg"}]}' %
                    (resource_exists, resource_exists_file_id),
                resource_not_exists:
                    '{"hitsCount":1,"hitsArray":[{"key":"%s","id":"%s","etime":1,"ctime":1,"mimetype":"image/jpeg"}]}' %
                    (resource_not_exists, resource_not_exists_file_id),
                resource_renamed_orig_name:
                    '{"hitsCount":1,"hitsArray":[{"key":"%s","id":"%s","etime":1,"ctime":1,"mimetype":"image/jpeg"}]}' %
                    (resource_renamed_orig_name, resource_renamed_file_id),
                resource_unknown: '{"hitsCount":0,"hitsArray":[]}',
            }[path]

        search_open_mocker = mock.patch('mpfs.core.services.search_service.SearchDB.open_url',
                                        side_effect=open_url_mock)
        search_index_push_mocker = mock.patch('mpfs.core.services.index_service.SearchIndexer.push_change')
        feature_toggle_mock = mock.patch('mpfs.core.base.FEATURE_NOTIFY_PHOTOSLICE_INDEX_WHEN_FILE_NOT_FOUND', True)
        photoslice_notification_mock = mock.patch(
            'mpfs.core.job_handlers.indexer.INDEXER_PHOTOSLICE_NOTIFICATION_ON_INDEXER_SIDE', True)

        now = datetime.now()
        with search_index_push_mocker as search_index_push_mock, \
                search_open_mocker, \
                time_machine(now), \
                feature_toggle_mock, \
                photoslice_notification_mock:
            self.json_ok('bulk_info', {'uid': self.uid}, json=[
                resource_exists, resource_not_exists, resource_unknown, resource_renamed_orig_name])
        assert ['/disk/f2.jpg', '/disk/f3.jpg', '/disk/f5.jpg'] == sorted(search_calls)
        push_call_args = search_index_push_mock.call_args_list
        filter_fields = partial(filter_dict, fields=['version', 'action', 'operation', 'resource_id', 'metric'])
        push_data = [filter_fields(args) for call in push_call_args for args in call[0][0]]
        expected_version = int(time.mktime(now.timetuple())) * 1000000
        expected_push_data = [
            {
                'action': 'delete',
                'operation': 'rm',
                'resource_id': '%s:%s' % (self.uid, resource_not_exists_file_id),
                'version': expected_version,
                'metric': '404_info_repair',
            },
            {
                'action': 'modify',
                'operation': 'move_resource',
                'resource_id': '%s:%s' % (self.uid, resource_renamed_file_id),
                'version': resource_renamed_version,
                'metric': '404_info_repair',
            },
        ]
        assert expected_push_data == push_data

        expected_push_kwargs = {
            'wait_search_response': False,
            'append_smartcache_callback': True,
            'service': 'photoslice'
        }
        push_kwargs = [call[1] for call in push_call_args]
        assert [expected_push_kwargs] == push_kwargs

        expected_push_kwargs = {'wait_search_response': False, 'append_smartcache_callback': True,
                                 'service': 'photoslice'}
        push_kwargs = [call[1] for call in push_call_args]
        assert [expected_push_kwargs] == push_kwargs

    def test_copy_file_from_disk_to_attach(self):
        self.upload_file(self.uid, '/disk/science_fiction.epub')

        opts = {
            'uid': self.uid,
            'src': '/disk/science_fiction.epub',
            'dst': '/attach/science_fiction.epub',
        }
        result = self.json_ok('async_copy', opts)
        opts = {
            'uid': self.uid,
            'oid': result['oid'],
            'meta': '',
        }
        status = self.json_ok('status', opts)
        self.assertEqual(status['status'], 'DONE', status)
        path = status['resource']['path']
        name = status['resource']['name']
        self.assertNotEqual(status['resource']['meta'].get('short_url'), None)
        self.assertNotEqual(path, '/attach/science_fiction.epub')
        self.assertEqual(name, 'science_fiction.epub')

        opts = {
            'uid': self.uid,
            'src': '/disk/science_fiction.epub',
            'dst': '/attach/science_fiction.epub',
        }
        result = self.json_ok('async_copy', opts)
        opts = {
            'uid': self.uid,
            'oid': result['oid'],
            'meta': '',
        }
        status = self.json_ok('status', opts)
        self.assertEqual(status['status'], 'DONE')
        path = status['resource']['path']
        name = status['resource']['name']
        self.assertNotEqual(status['resource']['meta'].get('short_url'), None)
        self.assertNotEqual(path, '/attach/science_fiction.epub')
        self.assertEqual(name, 'science_fiction.epub')

        opts = {
            'uid': self.uid,
            'path': '/attach',
        }
        attach_listing = self.json_ok('list', opts)
        self.assertEqual(len(attach_listing), 3)

        opts = {
            'uid': self.uid,
            'src': '/disk/science_fiction.epub',
            'dst': path,
        }
        result = self.json_ok('async_copy', opts)
        opts = {
            'uid': self.uid,
            'oid': result['oid'],
            'meta': '',
        }
        status = self.json_ok('status', opts)
        self.assertEqual(status['status'], 'DONE')
        name = status['resource']['name']
        self.assertEqual(name, path.split('/')[-1])
        path = status['resource']['path']
        self.assertNotEqual(status['resource']['meta'].get('short_url'), None)
        self.assertNotEqual(path, '/attach/science_fiction.epub')

        opts = {
            'uid': self.uid,
            'path': '/attach',
        }
        attach_listing = self.json_ok('list', opts)
        opts = {
            'uid': self.uid,
            'path': path,
            'meta': '',
        }
        attach_info = self.json_ok('info', opts)
        self.assertNotEqual(attach_info['meta'].get('short_url'), None)
        URL_RE = re.compile('https:\/\/.*\/mail/\?hash\=.*')
        self.assertNotEqual(URL_RE.match(attach_info['meta'].get('short_url')), None)

    def test_move_file_from_attach_to_disk_fails(self):
        self.upload_file(self.uid, '/attach/1.jpg')
        attach_listing = self.json_ok('list', {'uid': self.uid, 'path': '/attach'})
        actual_path = [x for x in attach_listing if x['type'] == 'file'][0]['path']
        self.json_error('async_move', {'uid': self.uid, 'src': actual_path, 'dst': '/disk/1.jpg'})

    def test_move_file_from_attach_yafotki_to_disk(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/attach/YaFotki'})
        self.upload_file(self.uid, '/attach/YaFotki/1.jpg')
        attach_listing = self.json_ok('list', {'uid': self.uid, 'path': '/attach/YaFotki'})
        actual_path = [x for x in attach_listing if x['type'] == 'file'][0]['path']
        self.json_ok('async_move', {'uid': self.uid, 'src': actual_path, 'dst': '/disk/1.jpg'})
        yafotki_listing = self.json_ok('list', {'uid': self.uid, 'path': '/attach/YaFotki'})
        assert len([x for x in yafotki_listing if x['type'] == 'file']) == 0
        disk_listing = self.json_ok('list', {'uid': self.uid, 'path': '/disk'})
        assert len([x for x in disk_listing if x['type'] == 'file']) == 1
        assert [x for x in disk_listing if x['type'] == 'file'][0]['path'] == '/disk/1.jpg'

    def test_copy_folder_from_disk_to_attach(self):
        opts = {
            'uid': self.uid,
            'path': '/disk/mail_attach',
        }
        self.json_ok('mkdir', opts)
        self.upload_file(self.uid, '/disk/mail_attach/file1.txt')
        self.upload_file(self.uid, '/disk/mail_attach/file2.txt')
        self.upload_file(self.uid, '/disk/mail_attach/file3.txt')
        opts = {
            'uid': self.uid,
            'path': '/disk/mail_attach/folder1',
        }
        self.json_ok('mkdir', opts)
        opts = {
            'uid': self.uid,
            'path': '/disk/mail_attach/folder1/folder2',
        }
        self.json_ok('mkdir', opts)
        opts = {
            'uid': self.uid,
            'path': '/disk/mail_attach/folder1/folder2/folder3',
        }
        self.json_ok('mkdir', opts)
        self.upload_file(self.uid, '/disk/mail_attach/folder1/folder2/folder3/file1.txt')
        self.upload_file(self.uid, '/disk/mail_attach/folder1/folder2/folder3/file2.txt')
        self.upload_file(self.uid, '/disk/mail_attach/folder1/folder2/folder3/file3.txt')
        opts = {
            'uid': self.uid,
            'src': '/disk/mail_attach',
            'dst': '/attach/mail_attach',
        }
        result = self.json_ok('async_copy', opts)
        opts = {
            'uid': self.uid,
            'oid': result['oid'],
            'meta': '',
        }
        status = self.json_ok('status', opts)
        self.assertEqual(status['status'], 'DONE')
        path = status['resource']['path']
        name = status['resource']['name']
        self.assertNotEqual(status['resource']['meta'].get('short_url'), None)
        self.assertNotEqual(path, '/attach/mail_attach')
        self.assertEqual(name, 'mail_attach')
        opts = {
            'uid': self.uid,
            'path': path,
            'meta': '',
        }
        attach_info = self.json_ok('info', opts)
        self.assertNotEqual(attach_info['meta'].get('short_url'), None)

    def test_rm_folder(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/folder_without_file'})

        opts = {'uid': self.uid, 'path': '/disk/folder_without_file'}
        self.json_ok('rm', opts)

    def test_list_public_trash_file(self):
        # выбираем публичные файлы только из /disk
        dir_path = '/disk/to_trash'
        self.json_ok('mkdir', {'uid': self.uid, 'path': dir_path})
        for i in range(3):
            file_path = '%s/%i' % (dir_path, i)
            self.upload_file(self.uid, file_path)
            self.json_ok('set_public', {'uid': self.uid, 'path': file_path})

        public_resources = self.json_ok('list_public', {'uid': self.uid})
        self.json_ok('trash_append', {'uid': self.uid, 'path': public_resources[0]['path']})
        assert len(self.json_ok('list_public', {'uid': self.uid})) == 2

    def test_remove_attach_folder(self):
        self.json_ok('mksysdir', {'uid': self.uid, 'type': 'yalivelettersarchive'})
        self.upload_file(self.uid, '/attach/yalivelettersarchive/1.txt')
        self.upload_file(self.uid, '/attach/yalivelettersarchive/2.txt')

        resp = self.json_ok('list', {'uid': self.uid, 'path': '/attach/yalivelettersarchive'})
        expected_hidden_files_pathes = {i['path'].replace('attach', 'hidden') for i in resp if i['type'] == 'file'}

        self.json_ok('rm', {'uid': self.uid, 'path': '/attach/yalivelettersarchive'})

        real_hidden_files_pathes = {i['key'].split(':')[0] for i in hidden_data.collection.find({'uid': self.uid}) if i['type'] == 'file'}
        assert real_hidden_files_pathes == expected_hidden_files_pathes

    def test_rm_nonempty_subfolder_in_not_owned_shared_folder(self):
        shared_folder_path = '/disk/shared_folder'
        shared_subfolder_path = shared_folder_path + '/subfolder'
        shared_subfolder_file_path = shared_subfolder_path + '/file.txt'

        self.json_ok('mkdir', {'uid': self.uid, 'path': shared_folder_path})
        self.json_ok('mkdir', {'uid': self.uid, 'path': shared_subfolder_path})
        self.upload_file(self.uid, shared_subfolder_file_path)

        self.create_user(self.uid_3)
        self.share_dir(self.uid, self.uid_3, self.email_3, shared_folder_path)

        shared_folder_info = self.json_ok('info', {'uid': self.uid_3, 'path': shared_folder_path})
        assert shared_folder_info['path'] == shared_folder_path

        operation = self.json_ok('async_rm', {'uid': self.uid_3, 'path': shared_subfolder_path})
        oid = operation['oid']

        status = self.json_ok('status', {'uid': self.uid_3, 'oid': oid})
        assert status['status'] == 'DONE'

        self.json_ok('info', {'uid': self.uid, 'path': '/hidden/shared_folder'})
        self.json_error('info', {'uid': self.uid_3, 'path': '/hidden/shared_folder'}, code=codes.RESOURCE_NOT_FOUND)

    def test_rm_file_with_right_md5_is_successfull(self):
        file_path = '/disk/1.txt'
        self.upload_file(self.uid, file_path)
        file_info = self.json_ok('info', {'uid': self.uid, 'path': file_path, 'meta': 'md5'})
        md5 = file_info['meta']['md5']
        self.json_ok('rm', {'uid': self.uid, 'path': file_path, 'md5': md5})

    def test_async_rm_file_with_right_md5_is_successfull(self):
        file_path = '/disk/1.txt'
        self.upload_file(self.uid, file_path)
        file_info = self.json_ok('info', {'uid': self.uid, 'path': file_path, 'meta': 'md5'})
        md5 = file_info['meta']['md5']
        self.json_ok('async_rm', {'uid': self.uid, 'path': file_path, 'md5': md5})

    def test_rm_file_with_wrong_md5_lead_to_error(self):
        file_path = '/disk/1.txt'
        self.upload_file(self.uid, file_path)
        self.json_error('rm', {'uid': self.uid, 'path': file_path, 'md5': '123'}, code=codes.PRECONDITIONS_FAILED)

    def test_async_rm_file_with_wrong_md5_lead_to_error(self):
        file_path = '/disk/1.txt'
        self.upload_file(self.uid, file_path)
        self.json_error('async_rm', {'uid': self.uid, 'path': file_path, 'md5': '123'}, code=codes.PRECONDITIONS_FAILED)

    def test_rm_folder_with_md5_lead_to_error(self):
        folder_path = '/disk/test_folder'
        self.json_ok('mkdir', {'uid': self.uid, 'path': folder_path})
        self.json_error('rm', {'uid': self.uid, 'path': folder_path, 'md5': '123'}, code=codes.MD5_CHECK_NOT_SUPPORTED)

    def test_async_rm_folder_with_md5_lead_to_error(self):
        folder_path = '/disk/test_folder'
        self.json_ok('mkdir', {'uid': self.uid, 'path': folder_path})
        self.json_error(
            'async_rm', {'uid': self.uid, 'path': folder_path, 'md5': '123'}, code=codes.MD5_CHECK_NOT_SUPPORTED)

    def test_sending_push_for_photounlim_root_folder_creation(self):
        with mock.patch('mpfs.core.filesystem.indexer.DiskDataIndexer.push') as index_mock:
            self.json_ok('enable_unlimited_autouploading', {'uid': self.uid})

        assert index_mock.call_count == 1
        assert index_mock.call_args_list[0][0][0].path == PHOTOUNLIM_AREA_PATH
        assert index_mock.call_args_list[0][0][1] == 'modify'
        assert index_mock.call_args_list[0][1]['operation'] == 'mkdir'


class UserDataCollectionManipulationTestCase(DiskTestCase):
    def _create_subtree(self, uid, root, folders_count, files_count, depth):
        if depth <= 0:
            return 0

        size = 0
        self.json_ok('mkdir', {'uid': uid, 'path': root})
        for i in xrange(files_count):
            size += self.upload_file(uid, '%s/f_%d.txt' % (root, i))
        for i in xrange(folders_count):
            subfolder = '%s/f_%d' % (root, i)
            size += self._create_subtree(uid, subfolder, folders_count, files_count, depth-1)
        return size

    def test_remove_folder(self):
        self._create_subtree(self.uid, '/disk/folder', 3, 2, 3)
        UserDataCollection().remove(self.uid, '/disk/folder')
        assert UserDataCollection().find_one_by_field(self.uid, {'path': '/disk/folder'}) is None

    def test_remove_folder_with_limit(self):
        self._create_subtree(self.uid, '/disk/folder', 3, 3, 3)
        from mpfs.metastorage.mongo.collections import base
        with mock.patch.object(base, 'LIMIT', 2):
            UserDataCollection().remove(self.uid, '/disk/folder')
            assert UserDataCollection().find_one_by_field(self.uid, {'path': '/disk/folder'}) is None


class RemovingByResourceIdTestCase(DiskTestCase):
    def setup_method(self, method):
        super(RemovingByResourceIdTestCase, self).setup_method(method)
        self.json_ok('enable_unlimited_autouploading', {'uid': self.uid})
        self.upload_file(self.uid, '/photounlim/1.jpg')
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/new_folder'})
        self.upload_file(self.uid, '/disk/new_folder/file_in_folder.txt')
        self.upload_file(self.uid, '/disk/new_folder/file_in_folder_2.txt')  # Нужен для проверки фильтрации
        self.upload_file(self.uid, '/disk/file.txt')

    @parameterized.expand([
        ('async_trash_append_by_resource_id',),
        ('async_rm_by_resource_id',),
        ('trash_append_by_resource_id',),
        ('rm_by_resource_id',),
    ])
    def test_delete_by_resource_id_for_file(self, endpoint):
        resource_id = self.json_ok('info', {'uid': self.uid, 'path': '/disk/file.txt', 'meta': ''})['meta']['resource_id']
        with mock.patch('mpfs.core.address.Address.add_trash_suffix'):
            self.json_ok(endpoint, {'uid': self.uid, 'resource_id': resource_id})
        self.json_error('info', {'uid': self.uid, 'path': '/disk/file.txt', 'meta': ''}, code=codes.RESOURCE_NOT_FOUND)
        if endpoint in {'async_trash_append_by_resource_id', 'trash_append_by_resource_id'}:
            self.json_ok('info', {'uid': self.uid, 'path': '/trash/file.txt', 'meta': ''})
        else:
            self.json_error('info', {'uid': self.uid, 'path': '/trash/file.txt', 'meta': ''}, code=codes.RESOURCE_NOT_FOUND)

    @parameterized.expand([
        ('async_trash_append_by_resource_id',),
        ('async_rm_by_resource_id',),
        ('trash_append_by_resource_id',),
        ('rm_by_resource_id',),
    ])
    def test_delete_by_resource_id_for_folder(self, endpoint):
        resource_id = self.json_ok('info', {'uid': self.uid, 'path': '/disk/new_folder', 'meta': ''})['meta']['resource_id']
        with mock.patch('mpfs.core.address.Address.add_trash_suffix'):
            self.json_ok(endpoint, {'uid': self.uid, 'resource_id': resource_id})
        self.json_error('info', {'uid': self.uid, 'path': '/disk/new_folder', 'meta': ''}, code=codes.RESOURCE_NOT_FOUND)
        if endpoint in {'async_trash_append_by_resource_id', 'trash_append_by_resource_id'}:
            assert len(self.json_ok('list', {'uid': self.uid, 'path': '/trash/new_folder', 'meta': ''})) == 3
        else:
            self.json_error('list', {'uid': self.uid, 'path': '/trash/new_folder', 'meta': ''}, code=codes.LIST_NOT_FOUND)

    @parameterized.expand([
        ('async_trash_append_by_resource_id',),
        ('async_rm_by_resource_id',),
        ('trash_append_by_resource_id',),
        ('rm_by_resource_id',),
    ])
    def test_cannot_delete_by_resource_id_of_different_user(self, endpoint):
        self.create_user(self.user_3.uid)
        self.upload_file(self.user_3.uid, '/disk/file.txt')
        resource_id = self.json_ok('info', {'uid': self.uid, 'path': '/disk/file.txt', 'meta': ''})['meta']['resource_id']
        self.json_error(endpoint, {'uid': self.user_3.uid, 'resource_id': resource_id}, code=codes.RESOURCE_NOT_FOUND)

    @parameterized.expand([
        ('async_trash_append_by_resource_id', False, False, False, 0, 2),
        ('async_trash_append_by_resource_id', False, False, False, None, 2),
        ('async_trash_append_by_resource_id', False, False, False, 1, 1),
        ('async_trash_append_by_resource_id', True, False, False, 1, 2),
        ('async_trash_append_by_resource_id', False, True, False, 1, 2),
        ('async_trash_append_by_resource_id', False, False, True, 1, 2),
        ('async_trash_append_by_resource_id', True, True, True, 1, 2),
        ('async_rm_by_resource_id', False, False, False, 0, 2),
        ('async_rm_by_resource_id', False, False, False, None, 2),
        ('async_rm_by_resource_id', False, False, False, 1, 1),
        ('async_rm_by_resource_id', True, False, False, 1, 2),
        ('async_rm_by_resource_id', False, True, False, 1, 2),
        ('async_rm_by_resource_id', False, False, True, 1, 2),
        ('async_rm_by_resource_id', True, True, True, 1, 2),
        ('trash_append_by_resource_id', False, False, False, 0, 2),
        ('trash_append_by_resource_id', False, False, False, None, 2),
        ('trash_append_by_resource_id', False, False, False, 1, 1),
        ('trash_append_by_resource_id', True, False, False, 1, 2),
        ('trash_append_by_resource_id', False, True, False, 1, 2),
        ('trash_append_by_resource_id', False, False, True, 1, 2),
        ('trash_append_by_resource_id', True, True, True, 1, 2),
        ('rm_by_resource_id', False, False, False, 0, 2),
        ('rm_by_resource_id', False, False, False, None, 2),
        ('rm_by_resource_id', False, False, False, 1, 1),
        ('rm_by_resource_id', True, False, False, 1, 2),
        ('rm_by_resource_id', False, True, False, 1, 2),
        ('rm_by_resource_id', False, False, True, 1, 2),
        ('rm_by_resource_id', True, True, True, 1, 2),
    ])
    def test_filtering(self, endpoint, md5, sha256, size, delete_all, left_resources):
        file_1_info = self.json_ok('info', {'uid': self.uid, 'path': '/disk/new_folder/file_in_folder.txt', 'meta': ''})['meta']
        self._set_resource_id_for_resource(file_1_info['resource_id'], '/disk/new_folder/file_in_folder_2.txt')

        opts = {
            'uid': self.uid,
            'resource_id': file_1_info['resource_id'],
        }
        if md5:
            opts['md5'] = file_1_info['md5']
        if sha256:
            opts['sha256'] = file_1_info['sha256']
        if size:
            opts['size'] = file_1_info['size']
        if delete_all:
            if endpoint in {'async_trash_append_by_resource_id', 'trash_append_by_resource_id'}:
                opts['append_all'] = delete_all
            elif endpoint in {'async_rm_by_resource_id', 'rm_by_resource_id'}:
                opts['rm_all'] = delete_all
        self.json_ok(endpoint, opts)

        assert len(self.json_ok('list', {'uid': self.uid, 'path': '/disk/new_folder'})) == left_resources

    @parameterized.expand([
        ('async_trash_append_by_resource_id', True, False, False),
        ('async_trash_append_by_resource_id', False, True, False),
        ('async_trash_append_by_resource_id', False, False, True),
        ('async_rm_by_resource_id', True, False, False),
        ('async_rm_by_resource_id', False, True, False),
        ('async_rm_by_resource_id', False, False, True),
        ('trash_append_by_resource_id', True, False, False),
        ('trash_append_by_resource_id', False, True, False),
        ('trash_append_by_resource_id', False, False, True),
        ('rm_by_resource_id', True, False, False),
        ('rm_by_resource_id', False, True, False),
        ('rm_by_resource_id', False, False, True),
    ])
    def test_delete_nothing_if_resource_ids_match_but_hashes_not(self, endpoint, md5, sha256, size):
        resource_id = self.json_ok('info', {'uid': self.uid, 'path': '/disk/new_folder/file_in_folder.txt', 'meta': ''})['meta']['resource_id']
        opts = {
            'uid': self.uid,
            'resource_id': resource_id,
        }
        if endpoint in {'async_trash_append_by_resource_id', 'trash_append_by_resource_id'}:
            opts['append_all'] = 1
        elif endpoint in {'async_rm_by_resource_id', 'rm_by_resource_id'}:
            opts['rm_all'] = 1

        if md5:
            opts['md5'] = '1' * 32
        if sha256:
            opts['sha256'] = '1' * 64
        if size:
            opts['size'] = 1
        self.json_error(endpoint, opts, code=codes.RESOURCE_NOT_FOUND)
        assert len(self.json_ok('list', {'uid': self.uid, 'path': '/disk/new_folder'})) == 3

    @parameterized.expand([
        ('async_trash_append_by_resource_id',),
        ('async_rm_by_resource_id',),
        ('trash_append_by_resource_id',),
        ('rm_by_resource_id',)
    ])
    def test_delete_resource_from_photounlim(self, endpoint):
        resource_id = self.json_ok('info', {'uid': self.uid, 'path': '/photounlim/1.jpg', 'meta': ''})['meta']['resource_id']

        opts = {
            'uid': self.uid,
            'resource_id': resource_id
        }
        with mock.patch('mpfs.core.address.Address.add_trash_suffix'):
            self.json_ok(endpoint, opts)
        self.json_error('info', {'uid': self.uid, 'path': '/photounlim/1.jpg', 'meta': ''}, code=codes.RESOURCE_NOT_FOUND)
        if endpoint in {'async_trash_append_by_resource_id', 'trash_append_by_resource_id'}:
            self.json_ok('info', {'uid': self.uid, 'path': '/trash/1.jpg', 'meta': ''})
        else:
            self.json_error('info', {'uid': self.uid, 'path': '/trash/1.jpg', 'meta': ''}, code=codes.RESOURCE_NOT_FOUND)

    @parameterized.expand([
        ('async_trash_append_by_resource_id',),
        ('async_rm_by_resource_id',),
        ('trash_append_by_resource_id',),
        ('rm_by_resource_id',)
    ])
    def test_delete_resources_from_photounlim_and_disk_with_same_resource_id(self, endpoint):
        file_1_info = self.json_ok('info', {'uid': self.uid, 'path': '/photounlim/1.jpg', 'meta': ''})['meta']

        self._set_resource_id_for_resource(file_1_info['resource_id'], '/disk/file.txt')

        opts = {
            'uid': self.uid,
            'resource_id': file_1_info['resource_id']
        }
        if endpoint in {'async_trash_append_by_resource_id', 'trash_append_by_resource_id'}:
            opts['append_all'] = 1
        elif endpoint in {'async_rm_by_resource_id', 'rm_by_resource_id'}:
            opts['rm_all'] = 1

        with mock.patch('mpfs.core.address.Address.add_trash_suffix'):
            self.json_ok(endpoint, opts)

        self.json_error('info', {'uid': self.uid, 'path': '/photounlim/1.jpg', 'meta': ''}, code=codes.RESOURCE_NOT_FOUND)
        self.json_error('info', {'uid': self.uid, 'path': '/disk/file.txt', 'meta': ''}, code=codes.RESOURCE_NOT_FOUND)

        if endpoint in {'async_trash_append_by_resource_id', 'trash_append_by_resource_id'}:
            self.json_ok('info', {'uid': self.uid, 'path': '/trash/1.jpg', 'meta': ''})
            self.json_ok('info', {'uid': self.uid, 'path': '/trash/file.txt', 'meta': ''})
        else:
            self.json_error('info', {'uid': self.uid, 'path': '/trash/1.jpg', 'meta': ''}, code=codes.RESOURCE_NOT_FOUND)
            self.json_error('info', {'uid': self.uid, 'path': '/trash/file.txt', 'meta': ''}, code=codes.RESOURCE_NOT_FOUND)

    @parameterized.expand([
        ('async_trash_append_by_resource_id',),
        ('async_rm_by_resource_id',),
        ('trash_append_by_resource_id',),
        ('rm_by_resource_id',)
    ])
    def test_delete_by_resource_id_files_only_flag(self, endpoint):
        resource_id = self.json_ok('info', {'uid': self.uid, 'path': '/disk/new_folder', 'meta': ''})['meta']['resource_id']
        self.json_error(endpoint, {'uid': self.uid, 'resource_id': resource_id, 'files_only': '1'}, code=codes.FOLDER_DELETION_BY_RESOURCE_ID_FORBIDDEN)
        self.json_ok('info', {'uid': self.uid, 'path': '/disk/new_folder', 'meta': ''})

    def _set_resource_id_for_resource(self, resource_id, resource_to_modify_path):
        r = list(ResourceDAO().find({'uid': self.uid, 'path': resource_to_modify_path}))[0]
        r['data']['file_id'] = ResourceId.parse(resource_id).file_id
        r['parent'] = '/disk/new_folder'
        ResourceDAO().update({'uid': self.uid, 'path': resource_to_modify_path}, r, upsert=True)
