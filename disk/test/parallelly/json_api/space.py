# -*- coding: utf-8 -*-
import json
import random
import re
import pytest
import mock

from lxml import etree

from base import CommonJsonApiTestCase
from mpfs.config import settings
from test.base_suit import set_up_open_url, tear_down_open_url
from nose_parameterized import parameterized

import mpfs.engine.process
from mpfs.core.queue import mpfs_queue
from mpfs.core.filesystem.dao.legacy import CollectionRoutedDatabase
from mpfs.core.filesystem.quota import Quota
from mpfs.common.static import SPACE_1GB, SPACE_1MB
from mpfs.core.user.constants import DEFAULT_FOLDERS, SUPPORTED_LOCALES

db = CollectionRoutedDatabase()


@mock.patch.dict(settings.services['disk'], {'default_limit': 0,
                                             'filesize_limit': 10737418240,
                                             'paid_filesize_limit': 53687091200,
                                             'timeout': 1.0,
                                             'unique_items': True})
class SpaceJsonApiTestCase(CommonJsonApiTestCase):

    @pytest.mark.skipif(True, reason='https://st.yandex-team.ru/CHEMODAN-32396')
    def test_space_low(self):
        """
        https://jira.yandex-team.ru/browse/CHEMODAN-10766
        """
        file_path_1 = '/disk/something_big_1.jpeg'
        file_path_2 = '/disk/something_big_2.jpeg'

        opts = {
            'uid': self.uid
        }
        self.service_ok('inspect', opts)
        user_info = self.json_ok('user_info', opts)
        # used = user_info['space']['used']
        free = user_info['space']['free']
        # limit = user_info['space']['limit']
        self.assertTrue(free > 1073741824)
        file_size = (free - 104857600 + 1024) / 2
        self.assertTrue(file_size > 0)
        file_data_1 = {
            'size': file_size,
        }
        xiva_requests = {}
        self.upload_file(self.uid, file_path_1, file_data=file_data_1, open_url_data=xiva_requests)
        tear_down_open_url()
        found = False
        for k, v in xiva_requests.iteritems():
            if k.startswith('http://localhost/service/echo?uid='):
                for each in v:
                    uid = re.search('http://localhost/service/echo\?uid=(\d+)', k).group(1)
                    self.assertEqual(uid, self.uid)
                    data = etree.fromstring(each['pure_data'])
                    if data.tag == 'space':
                        self.assertEqual(data.get('type'), 'is_low')
                        found = True
        self.assertFalse(found)

        disk_info_push = db.disk_info.find_one({'uid': str(self.uid), 'key': '/states/disk/push'})
        self.assertEqual(disk_info_push, None)

        # hardlink
        opts = {
            'uid': self.uid,
            'path': file_path_1,
            'meta': '',
        }
        file_info_1 = self.json_ok('info', opts)
        file_data_2 = {}
        for k in ('md5', 'sha256', 'size'):
            file_data_2[k] = file_info_1['meta'][k]
        self.assertEqual(file_size, file_info_1['meta']['size'])
        opts = {
            'uid': self.uid,
            'path': file_path_2,
        }
        opts.update(file_data_2)
        xiva_requests = set_up_open_url()
        with self.patch_mulca_is_file_exist(func_resp=True):
            result = self.json_ok('store', opts)
            self.assertEqual(result['status'], 'hardlinked')
        tear_down_open_url()
        found = False
        for k, v in xiva_requests.iteritems():
            if k.startswith('http://localhost/service/echo?uid='):
                for each in v:
                    uid = re.search('http://localhost/service/echo\?uid=(\d+)', k).group(1)
                    self.assertEqual(uid, self.uid)
                    data = etree.fromstring(each['pure_data'])
                    if data.tag == 'space':
                        self.assertEqual(data.get('type'), 'is_low')
                        found = True
        self.assertTrue(found)

        disk_info_push = db.disk_info.find_one({'uid': str(self.uid), 'key': '/states/disk/push'})
        self.assertNotEqual(disk_info_push, None)
        self.assertNotEqual(dict(disk_info_push['data']).get('space_is_low'), None)
        opts = {
            'uid': self.uid,
            'key': 'space_is_low',
            'namespace': 'push',
        }
        self.json_ok('state_remove', opts)
        disk_info_push = db.disk_info.find_one({'uid': str(self.uid), 'key': '/states/disk/push'})
        self.assertNotEqual(disk_info_push, None)
        self.assertEqual(disk_info_push['data'], [])

        # copy
        opts = {
            'uid': self.uid,
            'path': file_path_2,
        }
        self.json_ok('rm', opts)
        opts = {
            'uid': self.uid,
            'path': file_path_2,
        }
        self.json_error('info', opts, code=71)
        opts = {
            'uid': self.uid,
            'src': file_path_1,
            'dst': file_path_2,
        }
        xiva_requests = set_up_open_url()
        self.json_ok('async_copy', opts)
        tear_down_open_url()
        found = False
        for k, v in xiva_requests.iteritems():
            if k.startswith('http://localhost/service/echo?uid='):
                for each in v:
                    uid = re.search('http://localhost/service/echo\?uid=(\d+)', k).group(1)
                    self.assertEqual(uid, self.uid)
                    data = etree.fromstring(each['pure_data'])
                    if data.tag == 'space':
                        self.assertEqual(data.get('type'), 'is_low')
                        found = True
        self.assertTrue(found)
        opts = {
            'uid': self.uid,
            'path': file_path_2,
        }
        self.json_ok('info', opts)

        disk_info_push = db.disk_info.find_one({'uid': str(self.uid), 'key': '/states/disk/push'})
        self.assertNotEqual(disk_info_push, None)
        self.assertNotEqual(dict(disk_info_push['data']).get('space_is_low'), None)

        file_path_2 = '/disk/something_big_2.jpeg'

        trash_path_2 = '/trash/something_big_2.jpeg'
        opts = {
            'uid': self.uid,
            'path': file_path_2,
        }
        self.json_ok('async_trash_append', opts)
        opts = {
            'uid': self.uid,
            'key': 'space_is_low',
            'namespace': 'push',
        }
        self.json_ok('state_remove', opts)
        disk_info_push = db.disk_info.find_one({'uid': str(self.uid), 'key': '/states/disk/push'})
        self.assertNotEqual(disk_info_push, None)
        self.assertEqual(disk_info_push['data'], [])
        opts = {
            'uid': self.uid,
            'path': trash_path_2,
        }
        xiva_requests = set_up_open_url()
        self.json_ok('async_trash_restore', opts)
        tear_down_open_url()
        found = False
        for k, v in xiva_requests.iteritems():
            if k.startswith('http://localhost/service/echo?uid='):
                for each in v:
                    uid = re.search('http://localhost/service/echo\?uid=(\d+)', k).group(1)
                    self.assertEqual(uid, self.uid)
                    data = etree.fromstring(each['pure_data'])
                    if data.tag == 'space':
                        self.assertEqual(data.get('type'), 'is_low')
                        found = True
        self.assertFalse(found)
        opts = {
            'uid': self.uid,
            'path': file_path_2,
        }
        self.json_ok('info', opts)
        disk_info_push = db.disk_info.find_one({'uid': str(self.uid), 'key': '/states/disk/push'})
        self.assertNotEqual(disk_info_push, None)
        self.assertEqual(disk_info_push['data'], [])

        # new file
        file_path_1 = '/disk/something_big_1.jpeg'
        file_path_2 = '/disk/something_big_2.jpeg'
        file_path_3 = '/disk/something_big_3.jpeg'
        opts = {
            'uid': self.uid,
            'path': file_path_2,
        }
        self.json_ok('rm', opts)

        opts = {
            'uid': self.uid,
            'path': file_path_2,
        }
        self.json_error('info', opts, code=71)
        opts = {
            'uid': self.uid,
            'path': file_path_1,
            'meta': '',
        }
        file_info_1 = self.json_ok('info', opts)
        file_data_3 = {
            'size': file_info_1['meta']['size'],
        }
        xiva_requests = {}
        self.upload_file(self.uid, file_path_3, ok=True, file_data=file_data_3, open_url_data=xiva_requests)
        tear_down_open_url()
        found = False
        for k, v in xiva_requests.iteritems():
            if k.startswith('http://localhost/service/echo?uid='):
                for each in v:
                    uid = re.search('http://localhost/service/echo\?uid=(\d+)', k).group(1)
                    self.assertEqual(uid, self.uid)
                    data = etree.fromstring(each['pure_data'])
                    if data.tag == 'space':
                        self.assertEqual(data.get('type'), 'is_low')
                        found = True
        self.assertTrue(found)

        disk_info_push = db.disk_info.find_one({'uid': str(self.uid), 'key': '/states/disk/push'})
        self.assertNotEqual(disk_info_push, None)
        self.assertNotEqual(dict(disk_info_push['data']).get('space_is_low'), None)

        opts = {
            'uid': self.uid
        }
        user_info = self.json_ok('user_info', opts)
        # used = user_info['space']['used']
        free = user_info['space']['free']
        # limit = user_info['space']['limit']
        self.assertTrue(free < 1073741824)
        self.assertTrue(free > 1048576)

        """
        Assert no notify received
        """
        file_data = {
            'size': 52428800,
        }

        file_path_4 = '/disk/something_big_4.jpeg'

        xiva_requests = {}
        self.upload_file(self.uid, file_path_4, file_data=file_data, open_url_data=xiva_requests)
        tear_down_open_url()
        found = False
        for k, v in xiva_requests.iteritems():
            if k.startswith('http://localhost/service/echo?uid='):
                for each in v:
                    uid = re.search('http://localhost/service/echo\?uid=(\d+)', k).group(1)
                    self.assertEqual(uid, self.uid)
                    data = etree.fromstring(each['pure_data'])
                    if data.tag == 'space':
                        self.assertEqual(data.get('type'), 'is_low')
                        found = True
        self.assertFalse(found)

        opts = {
            'uid': self.uid
        }
        user_info = self.json_ok('user_info', opts)
        used = user_info['space']['used']
        free = user_info['space']['free']
        limit = user_info['space']['limit']
        self.assertTrue(free < 1073741824)
        self.assertTrue(free > 1048576)

        user_info = self.json_ok('user_info', {'uid': self.uid})
        used = user_info['space']['used']
        free = user_info['space']['free']
        limit = user_info['space']['limit']
        self.assertTrue(free < 1073741824)

        self.assertTrue(free > 1048576)
        file_data = {
            'size': free - 1048576 / 2
        }

        xiva_requests = {}
        self.upload_file(self.uid, '/disk/something_small.jpeg', file_data=file_data, open_url_data=xiva_requests)
        tear_down_open_url()
        found = False
        for k, v in xiva_requests.iteritems():
            if k.startswith('http://localhost/service/echo?uid='):
                for each in v:
                    uid = re.search('http://localhost/service/echo\?uid=(\d+)', k).group(1)
                    self.assertEqual(uid, self.uid)
                    data = etree.fromstring(each['pure_data'])
                    if data.tag == 'space':
                        self.assertEqual(data.get('type'), 'is_full')
                        found = True
        self.assertTrue(found)

        opts = {
            'uid': self.uid
        }
        user_info = self.json_ok('user_info', opts)
        free = user_info['space']['free']
        self.assertTrue(free < 1048576)

        file_path = '/disk/trash_drop_file'
        trash_path = '/trash/trash_drop_file'

        file_data = {
            'size': 10
        }
        self.upload_file(self.uid, '/disk/trash_drop_file', file_data=file_data)
        opts = {
            'uid': self.uid,
            'path': file_path,
        }
        self.json_ok('async_trash_append', opts)
        opts = {
            'uid': self.uid
        }
        user_info = self.json_ok('user_info', opts)
        used_before = user_info['space']['used']
        trash_before = user_info['space']['trash']
        opts = {
            'uid': self.uid,
            'path': trash_path,
        }
        self.json_ok('async_trash_drop', opts)
        opts = {
            'uid': self.uid
        }
        user_info = self.json_ok('user_info', opts)
        used_after = user_info['space']['used']
        trash_after = user_info['space']['trash']
        self.assertTrue(used_before > used_after)
        self.assertTrue(trash_before > trash_after)

    @parameterized.expand([
        ('space_is_low', 2),
        ('space_is_full', 0.9),
    ])
    def test_deduplication_id_in_out_of_space_push_notification(self, test_type, file_size_factor):
        Quota().set_limit(SPACE_1GB * 5, uid=self.uid)
        file_data = {'size': SPACE_1GB * 5 - int(SPACE_1MB*file_size_factor)}
        path = '/disk/file.dat'
        self.upload_file(self.uid, path, file_data=file_data)
        file_data = {'size': int(SPACE_1MB*0.5)}
        path = '/disk/file_x.dat'
        with mock.patch.object(mpfs_queue, 'put', return_value=None) as mocked_put:
            self.upload_file(self.uid, path, file_data=file_data)
        for args, kwargs in mocked_put.call_args_list:
            action_name = args[0].get('action_name')
            if args[1] == 'xiva' and action_name == 'space':
                action_class = args[0]['class']
                deduplication_id = kwargs['deduplication_id']
                self.assertEqual(test_type, action_class)
                self.assertEqual(deduplication_id, 'xiva_push_{}__{}_{}'.format(action_name, action_class, self.uid))

    @parameterized.expand(
        [
            (
                folder_type,
                random.randint(SPACE_1GB, 10 * SPACE_1GB),
                random.randint(1, 99999999),
                random.choice(SUPPORTED_LOCALES.keys())
            ) for folder_type in ['attach']  # задел под то что захотят и другие папки, у некоторых папок есть нюансы,
                                             # так что при добавлени новых, нужно отлаживать тест
        ]
    )
    def test_count_default_folders_param(self, folder_type, size, files_count, locale):
        folder_path = DEFAULT_FOLDERS[folder_type][locale]
        space_qs_params =  {'uid': self.uid, 'count_default_folders': folder_type}
        resp = self.json_ok('space', space_qs_params)
        self.assertNotIn('default_folders', resp)

        self.json_ok('mksysdir', {'uid': self.uid, 'type': folder_type})

        with mock.patch('mpfs.core.services.search_service.SearchDB.folder_size',
                               return_value={'path': folder_path, 'size': size, 'files_count': files_count}):
            resp = self.json_ok('space', space_qs_params)
            self.assertIn('default_folders', resp)
            self.assertIn(folder_type, resp['default_folders'])
            self.assertEqual(resp['default_folders'][folder_type]['path'], folder_path)
            self.assertEqual(resp['default_folders'][folder_type]['size'], size)
            self.assertEqual(resp['default_folders'][folder_type]['files_count'], files_count)
