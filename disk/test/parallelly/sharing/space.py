# -*- coding: utf-8 -*-
import re
import uuid

import pytest
from hamcrest import assert_that, calling, raises

from lxml import etree
from mock import patch, mock
from nose_parameterized import parameterized

from mpfs.common.errors import OwnerHasNoFreeSpace
from mpfs.config import settings
from mpfs.core.address import Address
from mpfs.core.filesystem.quota import Quota
from mpfs.core.job_handlers.routine import handle_set_group_size
from test.base_suit import set_up_open_url, tear_down_open_url
from test.common.sharing import CommonSharingMethods
from test.fixtures import users
from test.helpers.size_units import GB, KB
from test.parallelly.yateam_suit import BaseYaTeamTestCase

import mpfs.engine.process
from mpfs.common.static import codes
from mpfs.core.filesystem.dao.legacy import CollectionRoutedDatabase

db = CollectionRoutedDatabase()


@mock.patch.dict(settings.services['disk'], {'default_limit': 0,
                                             'filesize_limit': 10737418240,
                                             'paid_filesize_limit': 53687091200,
                                             'timeout': 1.0,
                                             'unique_items': True})
class SpaceSharingTestCase(CommonSharingMethods,
                           BaseYaTeamTestCase):

    def setup_method(self, method):
        super(SpaceSharingTestCase, self).setup_method(method)
        self.json_ok('user_init', {'uid': self.uid_1})
        self.json_ok('user_init', {'uid': self.uid_3})
        self.json_ok('user_init', {'uid': self.uid_6})
        # Проверяем функциональность без игнорирования
        self.patch_ignore_shared = patch('mpfs.core.user.utils.FEATURE_TOGGLES_IGNORE_SHARED_FOLDER_IN_QUOTA_FOR_ALL',
                                         False)
        self.patch_ignore_shared.start()
        self._make_yateam(self.uid_6)
        self.make_dirs()

    def teardown_method(self, method):
        self.patch_ignore_shared.stop()
        super(SpaceSharingTestCase, self).teardown_method(method)

    def make_dirs(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/parent_folder/'})
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/parent_folder/child_A_660/'})

    def setup_shared_folders_with_overdraft(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/shared_1/'})
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/shared_2/'})

        gid = self.create_group(path='/disk/shared_1')
        hsh = self.invite_user(uid=self.uid_1, path='/disk/shared_1/', email=self.email_1, ext_gid=gid)
        self.activate_invite(uid=self.uid_1, hash=hsh)

        upload_file_path = '/disk/shared_1/f_1.jpeg'
        file_data = {'size': GB * 6}
        self.upload_file(self.uid_1, upload_file_path, file_data=file_data)

        gid = self.create_group(path='/disk/shared_2')
        hsh = self.invite_user(uid=self.uid_3, path='/disk/shared_2/', email=self.email_3, ext_gid=gid)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        upload_file_path = '/disk/shared_2/f_2.jpeg'
        # Загружаем до лимата в 10GB
        file_data = {'size': GB * 4}
        self.upload_file(self.uid_3, upload_file_path, file_data=file_data)

    @parameterized.expand([
        ('invitee_has_no_space',),
        ('owner_has_no_space',),
    ])
    def test_forbid_move_from_shared_if_no_free_space(self, case_name):
        u"""
        Проверка квоты при муве из общей папки

        Первое перемещение срабатывает без каких-либо проблем.
        Второе перемещение не срабатывает, поскольку не хватает места.
        """
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/shared/'})
        gid = self.create_group(path='/disk/shared')
        hsh = self.invite_user(uid=self.uid_1, path='/disk/shared/', email=self.email_1, ext_gid=gid)
        self.activate_invite(uid=self.uid_1, hash=hsh)

        if case_name == 'invitee_has_no_space':
            uploader_uid = self.uid
            downloader_uid = self.uid_1
        else:
            uploader_uid = self.uid_1
            downloader_uid = self.uid

        upload_file_path = '/disk/shared/f_1.jpeg'
        file_data = {'size': GB * 6}
        self.upload_file(uploader_uid, upload_file_path, file_data=file_data)
        self.json_ok('move', {'uid': downloader_uid, 'src': upload_file_path, 'dst': '/disk/f_1.jpg'})

        # Догружаем до лимита того, кто делает move
        file_size = self.get_space(downloader_uid)['free']
        file_data = {'size': file_size}
        upload_file_path = '/disk/shared/f_1.jpeg'
        self.upload_file(uploader_uid, upload_file_path, file_data=file_data)
        self.json_error('move', {'uid': downloader_uid, 'src': upload_file_path, 'dst': '/disk/f_2.jpg'},
                        code=codes.NO_FREE_SPACE)

    def test_forbid_creating_shared_folder_if_no_free_space(self):
        self.setup_shared_folders_with_overdraft()

        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/shared_3/'})
        self.mail_error('share_create_group', {'uid': self.uid, 'path': '/disk/shared_3/'}, code=codes.NO_FREE_SPACE)

    def test_forbid_accepting_invite_to_shared_folder_if_no_free_space(self):
        self.setup_shared_folders_with_overdraft()

        self.json_ok('mkdir', {'uid': self.uid_3, 'path': '/disk/shared_3/'})
        gid = self.create_group(path='/disk/shared_3/', uid=self.uid_3)
        hsh = self.invite_user(uid=self.uid, owner=self.uid_3, path='/disk/shared_3/', email=self.email, ext_gid=gid)
        self.json_error('share_activate_invite', {'hash': hsh, 'uid': self.uid}, code=codes.WH_NO_SPACE_LEFT)

    def test_moving_to_trash_is_permitted_in_overdraft(self):
        self.setup_shared_folders_with_overdraft()
        self.json_ok('trash_append', {'uid': self.uid, 'path': '/disk/shared_1/f_1.jpeg'})

    def test_rm_is_permitted_in_overdraft(self):
        self.setup_shared_folders_with_overdraft()
        self.json_ok('rm', {'uid': self.uid, 'path': '/disk/shared_1/f_1.jpeg'})

    @pytest.mark.skipif(True, reason='https://st.yandex-team.ru/CHEMODAN-32396')
    def test_space_low(self):
        """
            https://jira.yandex-team.ru/browse/CHEMODAN-10766
        """
        gid = self.create_group(path='/disk/parent_folder/child_A_660')
        hsh = self.invite_user(uid=self.uid_1, path='/disk/parent_folder/child_A_660/', email=self.email_1, ext_gid=gid)
        self.activate_invite(uid=self.uid_1, hash=hsh)

        #=======================================================================
        # Check space details are equal
        user_info = {}
        for uid in (self.uid, self.uid_1):
            opts = {'uid': uid}
            user_info[uid] = self.json_ok('user_info', opts)
        for k in ('free', 'limit', 'used'):
            self.assertEqual(user_info[self.uid]['space'][k], user_info[self.uid_1]['space'][k], k)
        #=======================================================================

        file_path_1 = '/disk/something_big_1.jpeg'
        file_path_2 = '/disk/parent_folder/child_A_660/something_big_2.jpeg'
        file_path_3 = '/disk/parent_folder/child_A_660/something_big_3.jpeg'

        opts = {'uid': self.uid}
        user_info = self.json_ok('user_info', opts)
        used = user_info['space']['used']
        free = user_info['space']['free']
        limit = user_info['space']['limit']
        self.assertTrue(free > 1073741824)
        file_size = free - 104857600 #100MB

        file_data_1 = {'size': file_size/2}

        for uid in (self.uid, self.uid_1):
            xiva_requests = {}
            self.upload_file(uid, file_path_1, file_data=file_data_1, open_url_data=xiva_requests)
            tear_down_open_url()
            space_found = False
            diff_found = False
            for k, v in xiva_requests.iteritems():
                if k.startswith('http://localhost/service/echo?uid='):
                    for each in v:
                        uid = re.search('http://localhost/service/echo\?uid=(\d+)', k).group(1)
                        self.assertEqual(uid, uid)
                        data = etree.fromstring(each['pure_data'])
                        if data.tag == 'space':
                            space_found = True
                        elif data.tag == 'diff':
                            diff_found = True
            self.assertFalse(space_found)
            self.assertTrue(diff_found)

            for uid in (self.uid, self.uid_1):
                disk_info_push = db.disk_info.find_one({'uid': str(uid), 'key': '/states/disk/push'})
                self.assertEqual(disk_info_push, None)

        #=======================================================================
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
        found_owner = False
        found_user = False
        for k, v in xiva_requests.iteritems():
            if k.startswith('http://localhost/service/echo?uid='):
                for each in v:
                    uid = re.search('http://localhost/service/echo\?uid=(\d+)', k).group(1)
                    data = etree.fromstring(each['pure_data'])
                    if data.tag == 'space':
                        self.assertEqual(data.get('type'), 'is_low')
                        if uid == self.uid:
                            found_owner = True
                        elif uid == self.uid_1:
                            found_user = True
        self.assertTrue(found_owner)
        self.assertTrue(found_user)
        #=======================================================================
        for uid in (self.uid, self.uid_1):
            disk_info_push = db.disk_info.find_one({'uid': str(uid), 'key': '/states/disk/push'})
            self.assertNotEqual(disk_info_push, None)
            self.assertNotEqual(dict(disk_info_push['data']).get('space_is_low'), None)
            opts = {
                'uid': uid,
                'key': 'space_is_low',
                'namespace': 'push',
            }
            self.json_ok('state_remove', opts)
            disk_info_push = db.disk_info.find_one({'uid': str(uid), 'key': '/states/disk/push'})
            self.assertNotEqual(disk_info_push, None)
            self.assertEqual(disk_info_push['data'], [])
        #=======================================================================
        # Check space details are equal
        user_info = {}
        for uid in (self.uid, self.uid_1):
            opts = {
                'uid': uid,
            }
            user_info[uid] = self.json_ok('user_info', opts)
        for k in ('free', 'limit', 'used'):
            self.assertEqual(user_info[self.uid]['space'][k], user_info[self.uid_1]['space'][k])
        #=======================================================================
        # copy
        opts = {
            'uid': self.uid,
            'path': file_path_2,
        }
        self.json_ok('async_trash_append', opts)
        opts = {
            'uid': self.uid,
        }
        self.json_ok('async_trash_drop_all', opts)
        #=======================================================================
        # Check space details are equal
        user_info = {}
        for uid in (self.uid, self.uid_1):
            opts = {
                'uid': uid,
            }
            user_info[uid] = self.json_ok('user_info', opts)
        for k in ('free', 'limit', 'used'):
            self.assertEqual(user_info[self.uid]['space'][k], user_info[self.uid_1]['space'][k])
        #=======================================================================
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
        found_owner = False
        found_user = False
        for k, v in xiva_requests.iteritems():
            if k.startswith('http://localhost/service/echo?uid='):
                for each in v:
                    uid = re.search('http://localhost/service/echo\?uid=(\d+)', k).group(1)
                    data = etree.fromstring(each['pure_data'])
                    if data.tag == 'space':
                        self.assertEqual(data.get('type'), 'is_low')
                        if uid == self.uid:
                            found_owner = True
                        elif uid == self.uid_1:
                            found_user = True
        self.assertTrue(found_owner)
        self.assertTrue(found_user)
        opts = {
            'uid': self.uid,
            'path': file_path_2,
        }
        self.json_ok('info', opts)
        #=======================================================================
        # Check space details are equal
        user_info = {}
        for uid in (self.uid, self.uid_1):
            opts = {
                'uid': uid,
            }
            user_info[uid] = self.json_ok('user_info', opts)
        for k in ('free', 'limit', 'used'):
            self.assertEqual(user_info[self.uid]['space'][k], user_info[self.uid_1]['space'][k])
        #=======================================================================
        # Rest push states
        for uid in (self.uid, self.uid_1):
            disk_info_push = db.disk_info.find_one({'uid': str(uid), 'key': '/states/disk/push'})
            self.assertNotEqual(disk_info_push, None)
            self.assertNotEqual(dict(disk_info_push['data']).get('space_is_low'), None)
            opts = {
                'uid': uid,
                'key': 'space_is_low',
                'namespace': 'push',
            }
            self.json_ok('state_remove', opts)
            disk_info_push = db.disk_info.find_one({'uid' : str(uid), 'key' : '/states/disk/push'})
            self.assertNotEqual(disk_info_push, None)
            self.assertEqual(disk_info_push['data'], [])
        #=======================================================================
        # new file
        opts = {
            'uid': self.uid,
            'path': file_path_2,
        }
        self.json_ok('async_trash_append', opts)
        opts = {
            'uid': self.uid,
        }
        self.json_ok('async_trash_drop_all', opts)

        opts = {
            'uid': self.uid,
            'path': file_path_2,
        }
        self.json_error('info', opts, code=71)

        file_data_3 = {
            'size': file_info_1['meta']['size'],
        }
        xiva_requests = {}
        self.upload_file(self.uid, file_path_3, ok=True, file_data=file_data_3, open_url_data=xiva_requests)
        tear_down_open_url()
        found_owner = False
        found_user = False
        for k, v in xiva_requests.iteritems():
            if k.startswith('http://localhost/service/echo?uid='):
                for each in v:
                    uid = re.search('http://localhost/service/echo\?uid=(\d+)', k).group(1)
                    data = etree.fromstring(each['pure_data'])
                    if data.tag == 'space':
                        self.assertEqual(data.get('type'), 'is_low')
                        if uid == self.uid:
                            found_owner = True
                        elif uid == self.uid_1:
                            found_user = True
        self.assertTrue(found_owner)
        self.assertTrue(found_user)
        #=======================================================================
        # Check push states
        for uid in (self.uid, self.uid_1):
            disk_info_push = db.disk_info.find_one({'uid': str(uid), 'key': '/states/disk/push'})
            self.assertNotEqual(disk_info_push, None)
            self.assertNotEqual(dict(disk_info_push['data']).get('space_is_low'), None)
        #=======================================================================
        # Check space details are equal
        user_info = {}
        for uid in (self.uid, self.uid_1):
            opts = {
                'uid': uid,
            }
            user_info[uid] = self.json_ok('user_info', opts)
        for k in ('free', 'limit', 'used'):
            self.assertEqual(user_info[self.uid]['space'][k], user_info[self.uid_1]['space'][k])
        #=======================================================================
        # Rest push states
        for uid in (self.uid_1, ):
            disk_info_push = db.disk_info.find_one({'uid': str(uid), 'key': '/states/disk/push'})
            self.assertNotEqual(disk_info_push, None)
            self.assertNotEqual(dict(disk_info_push['data']).get('space_is_low'), None)
            opts = {
                'uid': uid,
                'key': 'space_is_low',
                'namespace': 'push',
            }
            self.json_ok('state_remove', opts)
            disk_info_push = db.disk_info.find_one({'uid': str(uid), 'key': '/states/disk/push'})
            self.assertNotEqual(disk_info_push, None)
            self.assertEqual(disk_info_push['data'], [])
        #=======================================================================
        # Check space details are equal
        user_info = {}
        for uid in (self.uid, self.uid_1):
            opts = {
                'uid': uid,
            }
            user_info[uid] = self.json_ok('user_info', opts)
        for k in ('free', 'limit', 'used'):
            self.assertEqual(user_info[self.uid]['space'][k], user_info[self.uid_1]['space'][k])
        #=======================================================================
        opts = {
            'uid': self.uid_1,
            'path': file_path_1,
        }
        self.json_ok('async_trash_append', opts)
        opts = {
            'uid': self.uid_1,
        }
        self.json_ok('async_trash_drop_all', opts)
        #=======================================================================
        # Check space details are NOT equal
        user_info = {}
        for uid in (self.uid, self.uid_1):
            opts = {
                'uid': uid,
            }
            user_info[uid] = self.json_ok('user_info', opts)
        for k in ('free', 'used',):
            self.assertNotEqual(user_info[self.uid]['space'][k], user_info[self.uid_1]['space'][k])
        self.assertNotEqual(user_info[self.uid]['space']['used'], 0)
        self.assertNotEqual(user_info[self.uid_1]['space']['used'], 0)
        #=======================================================================
        # move
        opts = {
            'uid': self.uid,
            'src': file_path_1,
            'dst': file_path_2,
        }
        xiva_requests = set_up_open_url()
        self.json_ok('async_move', opts)
        tear_down_open_url()
        found_owner = False
        found_user = False
        for k, v in xiva_requests.iteritems():
            if k.startswith('http://localhost/service/echo?uid='):
                for each in v:
                    uid = re.search('http://localhost/service/echo\?uid=(\d+)', k).group(1)
                    data = etree.fromstring(each['pure_data'])
                    if data.tag == 'space':
                        self.assertEqual(data.get('type'), 'is_low')
                        if uid == self.uid:
                            found_owner = True
                        elif uid == self.uid_1:
                            found_user = True
        self.assertFalse(found_owner)
        self.assertTrue(found_user)
        opts = {
            'uid': self.uid,
            'path': file_path_3,
        }
        self.json_ok('info', opts)
        #=======================================================================
        opts = {
            'uid': self.uid
        }
        user_info = self.json_ok('user_info', opts)
        used = user_info['space']['used']
        free = user_info['space']['free']
        limit = user_info['space']['limit']
        self.assertTrue(free < 1073741824)
        self.assertTrue(free > 1048576)

    @pytest.mark.skipif(True, reason='https://st.yandex-team.ru/CHEMODAN-32396')
    def test_space_low_second_time(self):
        """
            Assert no notify received
        """
        gid = self.create_group(path='/disk/parent_folder/child_A_660')
        hsh = self.invite_user(uid=self.uid_1, path='/disk/parent_folder/child_A_660/', email=self.email_1, ext_gid=gid)
        self.activate_invite(uid=self.uid_1, hash=hsh)

        opts = {'uid': self.uid}
        user_info = self.json_ok('user_info', opts)
        used = user_info['space']['used']
        free = user_info['space']['free']
        limit = user_info['space']['limit']
        self.assertTrue(free > 1073741824)
        file_size = free - 104857600 #100MB
        file_data_1 = {'size': file_size/2}

        file_path_1 = '/disk/something_big_1.jpeg'
        file_path_2 = '/disk/parent_folder/child_A_660/something_big_2.jpeg'
        file_path_3 = '/disk/parent_folder/child_A_660/something_big_3.jpeg'

        for uid in (self.uid, self.uid_1):
            self.upload_file(uid, file_path_1, file_data=file_data_1)

        opts = {
            'uid': self.uid,
            'path': file_path_1,
            'meta': '',
        }
        file_info_1 = self.json_ok('info', opts)
        file_data_2 = {}
        for k in ('md5', 'sha256', 'size'):
            file_data_2[k] = file_info_1['meta'][k]
        opts = {
            'uid': self.uid,
            'path': file_path_2,
        }
        opts.update(file_data_2)
        with self.patch_mulca_is_file_exist(func_resp=True):
            result = self.json_ok('store', opts)
            self.assertEqual(result['status'], 'hardlinked')



        # ---- start ----


        file_data = {
            'size': 52428800,
        }

        file_path_4 = '/disk/parent_folder/child_A_660/something_big_4.jpeg'

        xiva_requests = {}
        self.upload_file(self.uid, file_path_4, file_data=file_data, open_url_data=xiva_requests)
        tear_down_open_url()
        found_owner = False
        found_user = False
        for k, v in xiva_requests.iteritems():
            if k.startswith('http://localhost/service/echo?uid='):
                for each in v:
                    uid = re.search('http://localhost/service/echo\?uid=(\d+)', k).group(1)
                    data = etree.fromstring(each['pure_data'])
                    if data.tag == 'space':
                        self.assertEqual(data.get('type'), 'is_low')
                        if uid == self.uid:
                            found_owner = True
                        elif uid == self.uid_1:
                            found_user = True
        self.assertFalse(found_owner)
        self.assertFalse(found_user)

        for uid in (self.uid, self.uid_1):
            opts = {
                'uid': uid,
            }
            user_info = self.json_ok('user_info', opts)
            used = user_info['space']['used']
            free = user_info['space']['free']
            limit = user_info['space']['limit']
            self.assertTrue(free < 1073741824, {'uid': uid, 'free': free})
            self.assertTrue(free > 1048576, {'uid': uid, 'free': free})
        #=======================================================================
        # Check space details are equal
        user_info = {}
        for uid in (self.uid, self.uid_1):
            opts = {
                'uid': uid,
            }
            user_info[uid] = self.json_ok('user_info', opts)
        for k in ('free', 'limit', 'used'):
            self.assertEqual(user_info[self.uid]['space'][k], user_info[self.uid_1]['space'][k])
        #=======================================================================

    def test_space_full(self):
        gid = self.create_group(path='/disk/parent_folder/child_A_660')
        hsh = self.invite_user(uid=self.uid_1, path='/disk/parent_folder/child_A_660/', email=self.email_1, ext_gid=gid)
        self.activate_invite(uid=self.uid_1, hash=hsh)

        db.disk_info.update({'uid': self.uid, 'key': '/total_size'}, {'$set': {'data': 9663676419 }})
        db.disk_info.update({'uid': self.uid_1, 'key': '/total_size'}, {'$set': {'data': 9663676419 }})

        user_info = self.json_ok('user_info', {'uid': self.uid})
        used = user_info['space']['used']
        free = user_info['space']['free']
        limit = user_info['space']['limit']
        self.assertTrue(free < 1073741824)
        self.assertTrue(free > 1048576)
        file_data = {
            'size': free - 1048576/2
        }

        xiva_requests = {}
        self.upload_file(self.uid, '/disk/parent_folder/child_A_660/something_small.jpeg', file_data=file_data, open_url_data=xiva_requests)
        tear_down_open_url()
        found_owner = False
        found_user = False
        for k, v in xiva_requests.iteritems():
            if k.startswith('http://localhost/service/echo?uid='):
                for each in v:
                    uid = re.search('http://localhost/service/echo\?uid=(\d+)', k).group(1)
                    data = etree.fromstring(each['pure_data'])
                    if data.tag == 'space':
                        self.assertEqual(data.get('type'), 'is_full')
                        if uid == self.uid:
                            found_owner = True
                        elif uid == self.uid_1:
                            found_user = True
        self.assertTrue(found_owner)
        self.assertTrue(found_user)

        opts = {
            'uid': self.uid
        }
        user_info = self.json_ok('user_info', opts)
        used = user_info['space']['used']
        free = user_info['space']['free']
        limit = user_info['space']['limit']
        self.assertTrue(free < 1048576)

    def test_space_full_trash(self):
        gid = self.create_group(path='/disk/parent_folder/child_A_660')
        hsh = self.invite_user(uid=self.uid_1, path='/disk/parent_folder/child_A_660/', email=self.email_1, ext_gid=gid)
        self.activate_invite(uid=self.uid_1, hash=hsh)

        opts = {'uid': self.uid}
        user_info = self.json_ok('user_info', opts)
        used = user_info['space']['used']
        free = user_info['space']['free']
        limit = user_info['space']['limit']
        self.assertTrue(free > 1073741824)
        file_size = free - 104857600 #100MB
        file_data_1 = {'size': file_size/2}

        file_path_1 = '/disk/something_big_1.jpeg'
        file_path_2 = '/disk/parent_folder/child_A_660/something_big_2.jpeg'
        file_path_3 = '/disk/parent_folder/child_A_660/something_big_3.jpeg'

        for uid in (self.uid, self.uid_1):
            self.upload_file(uid, file_path_1, file_data=file_data_1)

        opts = {
            'uid': self.uid,
            'path': file_path_1,
            'meta': '',
        }
        file_info_1 = self.json_ok('info', opts)
        file_data_2 = {}
        for k in ('md5', 'sha256', 'size'):
            file_data_2[k] = file_info_1['meta'][k]
        opts = {
            'uid': self.uid,
            'path': file_path_2,
        }
        opts.update(file_data_2)
        with self.patch_mulca_is_file_exist(func_resp=True):
            result = self.json_ok('store', opts)
            self.assertEqual(result['status'], 'hardlinked')

        file_data = {
            'size': 123
        }

        xiva_requests = {}
        self.upload_file(self.uid, '/disk/parent_folder/child_A_660/something_small.jpeg', file_data=file_data, open_url_data=xiva_requests)


        #--- start ---

        for uid in (self.uid, self.uid_1):
            disk_info_push = db.disk_info.find_one({'uid': str(uid), 'key': '/states/disk/push'})
            self.assertNotEqual(disk_info_push, None)
            self.assertNotEqual(dict(disk_info_push['data']).get('space_is_low'), None)
            opts = {
                'uid': uid,
                'key': 'space_is_low',
                'namespace': 'push',
            }
            self.json_ok('state_remove', opts)

            disk_info_push = db.disk_info.find_one({'uid': str(uid), 'key': '/states/disk/push'})
            self.assertNotEqual(disk_info_push, None)
            self.assertEqual(disk_info_push['data'], [])
        opts = {
            'uid': self.uid,
            'path': '/disk/parent_folder/child_A_660/something_small.jpeg',
        }
        with patch('mpfs.core.address.Address.add_trash_suffix'):
            self.json_ok('async_trash_append', opts)
        for uid in (self.uid, self.uid_1):
            disk_info_push = db.disk_info.find_one({'uid': str(uid), 'key': '/states/disk/push'})
            self.assertNotEqual(disk_info_push, None)
            self.assertEqual(disk_info_push['data'], [])
        opts = {
            'uid': self.uid,
            'path': '/trash/something_small.jpeg',
        }
        self.json_ok('async_trash_restore', opts)
        for uid in (self.uid, self.uid_1):
            disk_info_push = db.disk_info.find_one({'uid': str(uid), 'key': '/states/disk/push'})
            self.assertNotEqual(disk_info_push, None)
            self.assertEqual(disk_info_push['data'], [])
        opts = {
            'uid': self.uid,
            'path': '/disk/parent_folder/child_A_660/something_small.jpeg',
        }
        self.json_ok('info', opts)

    def test_ignore_shared_folder_space(self):
        """Проверяет работу фичи игнорирования места ОП для не владельцев.

        Владелец ОП приглашает двух пользователей: с включенной фичей и без.
        После загрузки файла в ОП проверяем:
          * у Владельца место учитывается
          * у Пользователя без фичи место учитывается
          * у Пользователя с фичей место не учитывается

        """
        folder_path = '/disk/shared_folder'
        self.json_ok('mkdir', {'uid': self.uid, 'path': folder_path})
        invited_uid_non_yateam = self.uid_1
        invited_uid_yateam = self.uid_6
        hsh = self.invite_user(uid=invited_uid_non_yateam, owner=self.uid,
                               email=self.email_1, rights=660, path=folder_path)
        self.activate_invite(uid=invited_uid_non_yateam, hash=hsh)
        hsh = self.invite_user(uid=invited_uid_yateam, owner=self.uid,
                               email=self.email_6, rights=660, path=folder_path)
        self.activate_invite(uid=invited_uid_yateam, hash=hsh)

        # Изначально у всех место не занято
        assert self.get_space(self.uid)['used'] == 0
        assert self.get_space(invited_uid_non_yateam)['used'] == 0
        assert self.get_space(invited_uid_yateam)['used'] == 0

        file_size = 107 * KB
        self.upload_file(self.uid, '%s/%s' % (folder_path, '1.txt'),
                         file_data={'size': file_size})

        with patch('mpfs.core.user.utils.FEATURE_TOGGLES_IGNORE_SHARED_FOLDER_IN_QUOTA', False):
            assert self.get_space(self.uid)['used'] == \
                   self.get_space(invited_uid_non_yateam)['used'] == \
                   self.get_space(invited_uid_yateam)['used'] == file_size

        with patch('mpfs.core.user.utils.FEATURE_TOGGLES_IGNORE_SHARED_FOLDER_IN_QUOTA', True):
            assert self.get_space(self.uid)['used'] == \
                   self.get_space(invited_uid_non_yateam)['used'] == file_size
            assert self.get_space(invited_uid_yateam)['used'] == 0

    def test_ignore_shared_folder_space_on_join_group(self):
        """Проверяет работу фичи игнорирования места ОП для не владельцев.

        Владелец ОП приглашает пользователя, у которого не достаточно места.
        Но у которого работает фича игнорирования места ОП.
        Ожидаем, что пользователь сможет присоединиться к группе ОП.

        """
        folder_path = '/disk/shared_folder'
        self.json_ok('mkdir', {'uid': self.uid, 'path': folder_path})
        invited_uid_yateam = self.uid_6

        free_less_than_file_size = 260 * GB - 10 * KB
        file_size = 107 * KB
        self.upload_file(self.uid, '%s/%s' % (folder_path, '1.txt'),
                         file_data={'size': file_size})

        hsh = self.invite_user(uid=invited_uid_yateam, owner=self.uid,
                               email=self.email_6, rights=660, path=folder_path)

        with patch('mpfs.core.user.utils.FEATURE_TOGGLES_IGNORE_SHARED_FOLDER_IN_QUOTA',
                   False), \
                patch('mpfs.core.services.disk_service.MPFSStorageService.used',
                      return_value=free_less_than_file_size):
            assert_that(calling(self.activate_invite).with_args(uid=invited_uid_yateam, hash=hsh),
                        raises(AssertionError, '.*no free space.*'))

        with patch('mpfs.core.user.utils.FEATURE_TOGGLES_IGNORE_SHARED_FOLDER_IN_QUOTA',
                   True), \
                patch('mpfs.core.services.disk_service.MPFSStorageService.used',
                      return_value=free_less_than_file_size):
            self.activate_invite(uid=invited_uid_yateam, hash=hsh)


class IgnoreSharingSpaceOnWriteTestCase(CommonSharingMethods,
                                        BaseYaTeamTestCase):
    FILE_SIZE = 107 * KB

    NO_FREE_SPACE = 0
    ENOUGH_SPACE = FILE_SIZE * 2
    NOT_ENOUGH_SPACE = int(FILE_SIZE / 2)

    INVITED_UID = users.user_6.uid
    OWNER_UID = users.default_user.uid

    def setup_method(self, method):
        super(IgnoreSharingSpaceOnWriteTestCase, self).setup_method(method)
        self.json_ok('user_init', {'uid': self.uid_1})
        self.invited_uid_with_ignore = self.uid_6
        self.invited_uid_without_ignore = self.uid_3
        self.json_ok('user_init', {'uid': self.invited_uid_with_ignore})
        self.json_ok('user_init', {'uid': self.invited_uid_without_ignore})
        self._make_yateam(self.uid_6)

        self.owner_folder_path = '/disk/external/shared_folder'
        self.invited_folder_path = '/disk/shared_folder'
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/external'})
        self.json_ok('mkdir', {'uid': self.uid, 'path': self.owner_folder_path})

        # Каждому пользователю вручную задаем опцию игнорирования, для точных проверок
        self.patch_ignore_shared = patch('mpfs.core.user.utils.FEATURE_TOGGLES_IGNORE_SHARED_FOLDER_IN_QUOTA_FOR_ALL',
                                         False)
        self.patch_ignore_shared.start()

        for uid, email in ((self.invited_uid_with_ignore, self.email_6),
                           (self.invited_uid_without_ignore, self.email_3)):
            hsh = self.invite_user(uid=uid, owner=self.uid,
                                   email=email, rights=660, path=self.owner_folder_path)
            self.activate_invite(uid=uid, hash=hsh)

    def teardown_method(self, method):
        self.patch_ignore_shared.stop()
        super(IgnoreSharingSpaceOnWriteTestCase, self).teardown_method(method)

    @parameterized.expand([
        ('owner_and_invited_has_free', ENOUGH_SPACE, ENOUGH_SPACE, False),
        ('owner_and_invited_has_free', ENOUGH_SPACE, ENOUGH_SPACE, True),
        ('owner_has_and_invited_doesnt_have', ENOUGH_SPACE, NOT_ENOUGH_SPACE, True),
    ])
    def test_ignore_shared_folder_space_on_store(self, case_name, owner_free_space,
                                                 invited_free_space, ignore_enabled):
        u"""Проверяет возможность загрузить файл в расшаренную папку"""
        with patch('mpfs.core.user.utils.FEATURE_TOGGLES_IGNORE_SHARED_FOLDER_IN_QUOTA',
                   ignore_enabled), \
             patch('mpfs.core.services.disk_service.MPFSStorageService.free',
                   side_effect=lambda uid: owner_free_space if uid == self.uid else invited_free_space):
            self.upload_file(self.invited_uid_with_ignore, '%s/%s' % (self.invited_folder_path, '1.txt'),
                             file_data={'size': self.FILE_SIZE})

    @parameterized.expand([
        ('owner_has_and_invited_doesnt_have', ENOUGH_SPACE, NOT_ENOUGH_SPACE, False),
        ('owner_doesnt_have_and_invited_has', NOT_ENOUGH_SPACE, ENOUGH_SPACE, False),
        ('owner_doesnt_have_and_invited_has', NOT_ENOUGH_SPACE, ENOUGH_SPACE, True),
        ('owner_doesnt_have_and_invited_doesnt_have', NOT_ENOUGH_SPACE, NOT_ENOUGH_SPACE, False),
        ('owner_doesnt_have_and_invited_doesnt_have', NOT_ENOUGH_SPACE, NOT_ENOUGH_SPACE, True),
    ])
    def test_ignore_shared_folder_space_on_store_negative_cases(self, case_name, owner_free_space,
                                                                invited_free_space, ignore_enabled):
        u"""Проверяет возможность загрузить файл в расшаренную папку"""
        error_msg = '.*no free space within limit.*'
        if owner_free_space == self.NO_FREE_SPACE or owner_free_space == self.NOT_ENOUGH_SPACE:
            error_msg = '"code":%s' % codes.OWNER_HAS_NO_FREE_SPACE
        with patch('mpfs.core.user.utils.FEATURE_TOGGLES_IGNORE_SHARED_FOLDER_IN_QUOTA',
                   ignore_enabled), \
             patch('mpfs.core.services.disk_service.MPFSStorageService.free',
                   side_effect=lambda uid: owner_free_space if uid == self.uid else invited_free_space):
            assert_that(calling(self.upload_file).with_args(self.invited_uid_with_ignore,
                                                            '%s/%s' % (self.invited_folder_path, '1.txt'),
                                                            file_data={'size': self.FILE_SIZE}),
                        raises(AssertionError, error_msg))

    @parameterized.expand([
        ('owner_and_invited_has_free', ENOUGH_SPACE, ENOUGH_SPACE, False),
        ('owner_and_invited_has_free', ENOUGH_SPACE, ENOUGH_SPACE, True),
        ('owner_has_and_invited_doesnt_have', ENOUGH_SPACE, NOT_ENOUGH_SPACE, False),
        ('owner_has_and_invited_doesnt_have', ENOUGH_SPACE, NOT_ENOUGH_SPACE, True),
        ('owner_doesnt_have_and_invited_has', NOT_ENOUGH_SPACE, ENOUGH_SPACE, False),
        ('owner_doesnt_have_and_invited_has', NOT_ENOUGH_SPACE, ENOUGH_SPACE, True),
        ('owner_doesnt_have_and_invited_doesnt_have', NOT_ENOUGH_SPACE, NOT_ENOUGH_SPACE, False),
        ('owner_doesnt_have_and_invited_doesnt_have', NOT_ENOUGH_SPACE, NOT_ENOUGH_SPACE, True),
    ])
    def test_ignore_shared_folder_space_on_move_to_trash(self, case_name, owner_free_space,
                                                         invited_free_space, ignore_enabled):
        u"""Проверяет возможность переместить файл из расшаренной папки в Корзину

        Когда свободного пространства не 0 (может быть меньше или больше размера перемещаемого файла).
        """
        file_path = '%s/%s' % (self.invited_folder_path, '1.txt')
        self.upload_file(self.invited_uid_with_ignore, file_path,
                         file_data={'size': self.FILE_SIZE})

        with patch('mpfs.core.user.utils.FEATURE_TOGGLES_IGNORE_SHARED_FOLDER_IN_QUOTA',
                   ignore_enabled), \
             patch('mpfs.core.services.disk_service.MPFSStorageService.free',
                   side_effect=lambda uid: owner_free_space if uid == self.uid else invited_free_space):
            self.json_ok('trash_append', {'uid': self.invited_uid_with_ignore, 'path': file_path})

    @parameterized.expand([
        ('owner_has_and_invited_doesnt_have', ENOUGH_SPACE, NO_FREE_SPACE, True),
    ])
    def test_ignore_shared_folder_space_on_async_move_to_trash_when_no_space(self, case_name, owner_free_space,
                                                                             invited_free_space, ignore_enabled):
        u"""Проверяет возможность переместить файл асинхронно из расшаренной папки в Корзину"""
        file_path = '%s/%s' % (self.invited_folder_path, '1.txt')
        self.upload_file(self.invited_uid_with_ignore, file_path,
                         file_data={'size': self.FILE_SIZE})

        with patch('mpfs.core.user.utils.FEATURE_TOGGLES_IGNORE_SHARED_FOLDER_IN_QUOTA',
                   ignore_enabled), \
             patch('mpfs.core.services.disk_service.MPFSStorageService.free',
                   side_effect=lambda uid: owner_free_space if uid == self.uid else invited_free_space):
            # выполняем асинхронную операцию, по аналогии с ПО
            self.json_ok('async_trash_append', {'uid': self.invited_uid_with_ignore, 'path': file_path})

        shared_folder_list = self.json_ok('list', {'uid': self.invited_uid_with_ignore,
                                                   'path': self.invited_folder_path})
        invited_trash_list = self.json_ok('list', {'uid': self.invited_uid_with_ignore, 'path': '/trash'})
        owner_trash_list = self.json_ok('list', {'uid': self.uid, 'path': '/trash'})

        # У Приглашенного папки пустые (только корневой элемент)
        assert len(shared_folder_list) == 1
        assert len(invited_trash_list) == 1
        # У Владельца в Корзине есть удаленный файл
        assert len(owner_trash_list) == 2

    def test_ignore_shared_folder_space_on_move_to_trash_when_no_space(self):
        u"""Проверяет возможность переместить файл из расшаренной папки в Корзину

        Когда нет свободного места вовсе.
        """
        invited_file_path = '%s/%s' % (self.invited_folder_path, '1.txt')
        owner_file_path = '%s/%s' % (self.owner_folder_path, '2.txt')
        self.upload_file(self.invited_uid_with_ignore, invited_file_path,
                         file_data={'size': self.FILE_SIZE})
        self.upload_file(self.uid, owner_file_path,
                         file_data={'size': self.FILE_SIZE})

        with patch('mpfs.core.user.utils.FEATURE_TOGGLES_IGNORE_SHARED_FOLDER_IN_QUOTA',
                   False), \
             patch('mpfs.core.services.disk_service.MPFSStorageService.free',
                   return_value=0):
            self.json_ok('trash_append', {'uid': self.uid, 'path': owner_file_path})
            self.json_ok('trash_append', {'uid': self.invited_uid_with_ignore, 'path': invited_file_path})

        self.upload_file(self.invited_uid_with_ignore, invited_file_path,
                         file_data={'size': self.FILE_SIZE})
        self.upload_file(self.uid, owner_file_path,
                         file_data={'size': self.FILE_SIZE})
        with patch('mpfs.core.user.utils.FEATURE_TOGGLES_IGNORE_SHARED_FOLDER_IN_QUOTA',
                   True), \
             patch('mpfs.core.services.disk_service.MPFSStorageService.free',
                   return_value=0):
            self.json_ok('trash_append', {'uid': self.uid, 'path': owner_file_path})
            self.json_ok('trash_append', {'uid': self.invited_uid_with_ignore, 'path': invited_file_path})

    @parameterized.expand([
        ('owner_and_invited_has_free', ENOUGH_SPACE, ENOUGH_SPACE, False),
        ('owner_and_invited_has_free', ENOUGH_SPACE, ENOUGH_SPACE, True),
        ('owner_doesnt_have_and_invited_has', NO_FREE_SPACE, ENOUGH_SPACE, False),
        ('owner_doesnt_have_and_invited_has', NO_FREE_SPACE, ENOUGH_SPACE, True),
    ])
    def test_ignore_shared_folder_space_on_move_from_shared(self, case_name, owner_free_space,
                                                            invited_free_space, ignore_enabled):
        u"""Проверяет возможность переместить файл из расшаренной папки"""
        file_path = '%s/%s' % (self.invited_folder_path, '1.txt')
        self.upload_file(self.invited_uid_with_ignore, file_path,
                         file_data={'size': self.FILE_SIZE})
        new_path = '/disk/%s' % '1.txt'

        with patch('mpfs.core.user.utils.FEATURE_TOGGLES_IGNORE_SHARED_FOLDER_IN_QUOTA',
                   ignore_enabled), \
             patch('mpfs.core.services.disk_service.MPFSStorageService.free',
                   side_effect=lambda uid: owner_free_space if uid == self.uid else invited_free_space):
            self.json_ok('move', {'uid': self.invited_uid_with_ignore, 'src': file_path,
                                  'dst': new_path})


    @parameterized.expand([
        ('owner_has_and_invited_doesnt_have', ENOUGH_SPACE, NO_FREE_SPACE, False),
        ('owner_has_and_invited_doesnt_have', ENOUGH_SPACE, NO_FREE_SPACE, True),
        ('owner_doesnt_have_and_invited_doesnt_have', NO_FREE_SPACE, NO_FREE_SPACE, False),
        ('owner_doesnt_have_and_invited_doesnt_have', NO_FREE_SPACE, NO_FREE_SPACE, True),
    ])
    def test_ignore_shared_folder_space_on_move_from_shared_negative(self, case_name, owner_free_space,
                                                                     invited_free_space, ignore_enabled):
        u"""Проверяет возможность переместить файл из расшаренной папки (негативные кейсы)"""
        file_path = '%s/%s' % (self.invited_folder_path, '1.txt')
        self.upload_file(self.invited_uid_with_ignore, file_path,
                         file_data={'size': self.FILE_SIZE})
        new_path = '/disk/%s' % '1.txt'

        error_code = codes.NO_FREE_SPACE
        if owner_free_space == self.NO_FREE_SPACE and invited_free_space != self.NO_FREE_SPACE:
            error_code = codes.OWNER_HAS_NO_FREE_SPACE

        with patch('mpfs.core.user.utils.FEATURE_TOGGLES_IGNORE_SHARED_FOLDER_IN_QUOTA',
                   ignore_enabled), \
             patch('mpfs.core.services.disk_service.MPFSStorageService.free',
                   side_effect=lambda uid: owner_free_space if uid == self.uid else invited_free_space):
            self.json_error('move', {'uid': self.invited_uid_with_ignore, 'src': file_path, 'dst': new_path},
                            code=error_code)


    @parameterized.expand([
        ('owner_and_invited_has_free', ENOUGH_SPACE, ENOUGH_SPACE, False),
        ('owner_and_invited_has_free', ENOUGH_SPACE, ENOUGH_SPACE, True),
        ('owner_has_and_invited_doesnt_have', ENOUGH_SPACE, NO_FREE_SPACE, True),
    ])
    def test_ignore_shared_folder_space_on_move_to_shared(self, case_name, owner_free_space,
                                                          invited_free_space, ignore_enabled):
        u"""Проверяет возможность переместить файл в расшаренную папку"""
        file_path = '/disk/%s' % '1.txt'
        new_path = '%s/%s' % (self.invited_folder_path, '1.txt')
        self.upload_file(self.invited_uid_with_ignore, file_path,
                         file_data={'size': self.FILE_SIZE})

        with patch('mpfs.core.user.utils.FEATURE_TOGGLES_IGNORE_SHARED_FOLDER_IN_QUOTA',
                   ignore_enabled), \
             patch('mpfs.core.services.disk_service.MPFSStorageService.free',
                   side_effect=lambda uid: owner_free_space if uid == self.uid else invited_free_space):
            self.json_ok('move', {'uid': self.invited_uid_with_ignore, 'src': file_path,
                                  'dst': new_path})


    @parameterized.expand([
        ('owner_has_and_invited_doesnt_have', ENOUGH_SPACE, NO_FREE_SPACE, False),
        ('owner_doesnt_have_and_invited_has', NO_FREE_SPACE, ENOUGH_SPACE, False),
        ('owner_doesnt_have_and_invited_has', NO_FREE_SPACE, ENOUGH_SPACE, True),
        ('owner_doesnt_have_and_invited_doesnt_have', NO_FREE_SPACE, NO_FREE_SPACE, False),
        ('owner_doesnt_have_and_invited_doesnt_have', NO_FREE_SPACE, NO_FREE_SPACE, True),
    ])
    def test_ignore_shared_folder_space_on_move_to_shared_negative(self, case_name, owner_free_space,
                                                                   invited_free_space, ignore_enabled):
        u"""Проверяет возможность переместить файл в расшаренную папку (негативные кейсы)"""
        file_path = '/disk/%s' % '1.txt'
        new_path = '%s/%s' % (self.invited_folder_path, '1.txt')
        self.upload_file(self.invited_uid_with_ignore, file_path,
                         file_data={'size': self.FILE_SIZE})

        error_code = codes.NO_FREE_SPACE
        if owner_free_space == self.NO_FREE_SPACE or owner_free_space == self.NOT_ENOUGH_SPACE:
            error_code = codes.OWNER_HAS_NO_FREE_SPACE

        with patch('mpfs.core.user.utils.FEATURE_TOGGLES_IGNORE_SHARED_FOLDER_IN_QUOTA',
                   ignore_enabled), \
             patch('mpfs.core.services.disk_service.MPFSStorageService.free',
                   side_effect=lambda uid: owner_free_space if uid == self.uid else invited_free_space):
            self.json_error('move', {'uid': self.invited_uid_with_ignore, 'src': file_path, 'dst': new_path},
                            code=error_code)

    @parameterized.expand([
        ('owner_and_invited_has_free', ENOUGH_SPACE, ENOUGH_SPACE, False),
        ('owner_and_invited_yateam_has_free', ENOUGH_SPACE, ENOUGH_SPACE, True),
    ])
    def test_ignore_shared_folder_space_on_copy_to_shared(self, case_name, owner_free_space,
                                                          invited_free_space, ignore_enabled):
        u"""Проверяет возможность копировать файл в расшаренную папку"""
        invited_file_path = '/disk/2.txt'
        self.upload_file(self.invited_uid_with_ignore, invited_file_path,
                         file_data={'size': self.FILE_SIZE})

        with patch('mpfs.core.user.utils.FEATURE_TOGGLES_IGNORE_SHARED_FOLDER_IN_QUOTA',
                   ignore_enabled), \
             patch('mpfs.core.services.disk_service.MPFSStorageService.free',
                   side_effect=lambda uid: owner_free_space if uid == self.uid else invited_free_space):
            self.json_ok('copy', {'uid': self.invited_uid_with_ignore,
                                  'src': invited_file_path,
                                  'dst': '%s/%s' % (self.invited_folder_path, '1.txt')})

    @parameterized.expand([
        ('owner_has_and_invited_doesnt_have', ENOUGH_SPACE, NO_FREE_SPACE, False),
        ('owner_has_and_invited_yateam_doesnt_have', ENOUGH_SPACE, NO_FREE_SPACE, True),
        ('owner_doesnt_have_and_invited_has', NO_FREE_SPACE, ENOUGH_SPACE, False),
        ('owner_doesnt_have_and_invited_yateam_has', NO_FREE_SPACE, ENOUGH_SPACE, True),
        ('owner_doesnt_have_and_invited_doesnt_have', NO_FREE_SPACE, NO_FREE_SPACE, False),
        ('owner_doesnt_have_and_invited_yateam_doesnt_have', NO_FREE_SPACE, NO_FREE_SPACE, True),
    ])
    def test_ignore_shared_folder_space_on_copy_to_shared_negative(self, case_name, owner_free_space,
                                                                   invited_free_space, ignore_enabled):
        u"""Проверяет возможность копировать файл в расшаренную папку (негативные кейсы)"""
        invited_file_path = '/disk/2.txt'
        self.upload_file(self.invited_uid_with_ignore, invited_file_path,
                         file_data={'size': self.FILE_SIZE})

        error_msg = '.*no free space.*'
        if owner_free_space == self.NO_FREE_SPACE or owner_free_space == self.NOT_ENOUGH_SPACE:
            error_msg = '"code":%s' % codes.OWNER_HAS_NO_FREE_SPACE

        with patch('mpfs.core.user.utils.FEATURE_TOGGLES_IGNORE_SHARED_FOLDER_IN_QUOTA',
                   ignore_enabled), \
             patch('mpfs.core.services.disk_service.MPFSStorageService.free',
                   side_effect=lambda uid: owner_free_space if uid == self.uid else invited_free_space):
            assert_that(calling(self.json_ok).with_args('copy', {'uid': self.invited_uid_with_ignore,
                                                                 'src': invited_file_path,
                                                                 'dst': '%s/%s' % (self.invited_folder_path, '1.txt')}),
                        raises(AssertionError, error_msg))
            # Асинхронно тоже не позволяем копировать
            assert_that(calling(self.json_ok).with_args('async_copy', {'uid': self.invited_uid_with_ignore,
                                                                       'src': invited_file_path,
                                                                       'dst': '%s/%s' % (self.invited_folder_path,
                                                                                         '1.txt')}),
                        raises(AssertionError, error_msg))

    @parameterized.expand([
        ('owner_and_invited_has_free', ENOUGH_SPACE, ENOUGH_SPACE, False),
        ('owner_and_invited_yateam_has_free', ENOUGH_SPACE, ENOUGH_SPACE, True),
    ])
    def test_ignore_shared_folder_space_on_copy_from_shared(self, case_name, owner_free_space,
                                                            invited_free_space, ignore_enabled):
        u"""Проверяет возможность копировать файл в расшаренную папку"""
        file_path = '%s/%s' % (self.invited_folder_path, '2.txt')
        self.upload_file(self.invited_uid_with_ignore, file_path,
                         file_data={'size': self.FILE_SIZE})

        with patch('mpfs.core.user.utils.FEATURE_TOGGLES_IGNORE_SHARED_FOLDER_IN_QUOTA',
                   ignore_enabled), \
             patch('mpfs.core.services.disk_service.MPFSStorageService.free',
                   side_effect=lambda uid: owner_free_space if uid == self.uid else invited_free_space):
            self.json_ok('copy', {'uid': self.invited_uid_with_ignore,
                                  'src': file_path,
                                  'dst': '/disk/1.txt'})


    @parameterized.expand([
        ('owner_has_and_invited_doesnt_have', ENOUGH_SPACE, NO_FREE_SPACE, False),
        ('owner_has_and_invited_yateam_doesnt_have', ENOUGH_SPACE, NO_FREE_SPACE, True),
        ('owner_doesnt_have_and_invited_has', NO_FREE_SPACE, ENOUGH_SPACE, False),
        ('owner_doesnt_have_and_invited_yateam_has', NO_FREE_SPACE, ENOUGH_SPACE, True),
        ('owner_doesnt_have_and_invited_doesnt_have', NO_FREE_SPACE, NO_FREE_SPACE, False),
        ('owner_doesnt_have_and_invited_yateam_doesnt_have', NO_FREE_SPACE, NO_FREE_SPACE, True),
    ])
    def test_ignore_shared_folder_space_on_copy_from_shared_negative(self, case_name, owner_free_space,
                                                                     invited_free_space, ignore_enabled):
        u"""Проверяет возможность копировать файл в расшаренную папку (негативные кейсы)"""
        shared_file_path = '%s/%s' % (self.invited_folder_path, '2.txt')
        self.upload_file(self.invited_uid_with_ignore, shared_file_path,
                         file_data={'size': self.FILE_SIZE})

        error_code = codes.NO_FREE_SPACE
        copy_from = shared_file_path
        copy_to = '/disk/1.txt'

        if owner_free_space == self.NO_FREE_SPACE and invited_free_space != self.NO_FREE_SPACE:
            error_code = codes.OWNER_HAS_NO_FREE_SPACE
            copy_from = '/disk/1.txt'
            copy_to = shared_file_path

        with patch('mpfs.core.user.utils.FEATURE_TOGGLES_IGNORE_SHARED_FOLDER_IN_QUOTA',
                   ignore_enabled), \
             patch('mpfs.core.services.disk_service.MPFSStorageService.free',
                   side_effect=lambda uid: owner_free_space if uid == self.uid else invited_free_space):
            self.json_error('copy', {'uid': self.invited_uid_with_ignore,
                                     'src': copy_from,
                                     'dst': copy_to},
                            code=error_code)

    def test_ignore_shared_folder_space_dry_run(self):
        """Проверяет режим прогона для новых проверок места.

        У Владельца нет места - новые проверки падают с No Free Space.
        Проверяем, что в DRY-режиме все операции проходят (старые проверки не проверяют место владельца) и
        проверяем что в логе будет запись о фейле в новых проверках.

        """
        owner_free_space = self.NO_FREE_SPACE
        invited_free_space = self.ENOUGH_SPACE
        invited_file_path = '/disk/2.txt'
        self.upload_file(self.invited_uid_with_ignore, invited_file_path,
                         file_data={'size': self.FILE_SIZE})
        with patch('mpfs.core.user.utils.FEATURE_TOGGLES_IGNORE_SHARED_FOLDER_IN_QUOTA',
                   True), \
                 patch('mpfs.core.user.utils.FEATURE_TOGGLES_CORRECT_SPACE_CHECKS_FOR_SHARED_FOLDERS_DRY_RUN',
                       True), \
                 patch('mpfs.core.filesystem.base.log.info') as log_info, \
                 patch('mpfs.core.services.disk_service.MPFSStorageService.free',
                       side_effect=lambda uid: owner_free_space if uid == self.uid else invited_free_space):
            self.upload_file(self.invited_uid_with_ignore, '%s/%s' % (self.invited_folder_path, '1.txt'),
                             file_data={'size': self.FILE_SIZE})
            assert any('Owner space checks failed' in log_record[0][0] for log_record in log_info.call_args_list)
            log_info.reset_mock()

            new_file_path = '%s/%s' % (self.invited_folder_path, '3.txt')
            self.json_ok('copy', {'uid': self.invited_uid_with_ignore,
                                  'src': invited_file_path,
                                  'dst': new_file_path})
            assert any('Owner space checks failed' in log_record[0][0] for log_record in log_info.call_args_list)
            log_info.reset_mock()

            self.json_ok('move', {'uid': self.invited_uid_with_ignore, 'src': new_file_path,
                                  'dst': '%s/%s' % (self.invited_folder_path, '4.txt')})
            assert any('Owner space checks failed' in log_record[0][0] for log_record in log_info.call_args_list)
            log_info.reset_mock()

    def test_ignore_shared_folder_space_dry_run_black_list(self):
        """Проверяет режим прогона для новых проверок места.

        У Владельца нет места - новые проверки падают с No Free Space.
        Проверяем, что в DRY-режиме все операции проходят (старые проверки не проверяют место владельца) и
        проверяем что в логе будет запись о фейле в новых проверках.

        """
        owner_free_space = self.NO_FREE_SPACE
        invited_free_space = self.ENOUGH_SPACE
        invited_file_path = '/disk/2.txt'
        self.upload_file(self.invited_uid_with_ignore, invited_file_path,
                         file_data={'size': self.FILE_SIZE})
        shared_file_path = '%s/%s' % (self.invited_folder_path, '3.txt')
        self.upload_file(self.invited_uid_with_ignore, shared_file_path,
                         file_data={'size': self.FILE_SIZE})
        error_msg = '"code":234'
        with patch('mpfs.core.user.utils.FEATURE_TOGGLES_IGNORE_SHARED_FOLDER_IN_QUOTA',
                   True), \
                 patch('mpfs.core.user.utils.FEATURE_TOGGLES_CORRECT_SPACE_CHECKS_FOR_SHARED_FOLDERS_DRY_RUN',
                       True), \
                 patch('mpfs.core.user.utils.FEATURE_TOGGLES_CORRECT_SPACE_CHECKS_FOR_SHARED_FOLDERS_DRY_RUN_BLACK_LIST_UIDS',
                       [self.invited_uid_with_ignore]), \
                 patch('mpfs.core.services.disk_service.MPFSStorageService.free',
                       side_effect=lambda uid: owner_free_space if uid == self.uid else invited_free_space):
            assert_that(calling(self.upload_file).with_args(self.invited_uid_with_ignore,
                                                            '%s/%s' % (self.invited_folder_path, '1.txt'),
                                                            file_data={'size': self.FILE_SIZE}),
                        raises(AssertionError, error_msg))

            new_shared_file_path = '%s/%s' % (self.invited_folder_path, '4.txt')
            assert_that(calling(self.json_ok).with_args('copy', {'uid': self.invited_uid_with_ignore,
                                                                 'src': shared_file_path,
                                                                 'dst': new_shared_file_path}),
                        raises(AssertionError, error_msg))

            assert_that(calling(self.json_ok).with_args('move', {'uid': self.invited_uid_with_ignore,
                                                                 'src': shared_file_path,
                                                                 'dst': new_shared_file_path}),
                        raises(AssertionError, error_msg))

    def test_error_details_in_failed_operation_info(self):
        """Проверяет передачу деталей фейла операций при запросе статуса операции"""
        file_path = '/disk/%s' % '1.txt'
        new_path = '%s/%s' % (self.invited_folder_path, '1.txt')
        self.upload_file(self.invited_uid_with_ignore, file_path,
                         file_data={'size': self.FILE_SIZE})

        with patch('mpfs.core.user.utils.FEATURE_TOGGLES_IGNORE_SHARED_FOLDER_IN_QUOTA',
                   True), \
             patch('mpfs.core.services.disk_service.MPFSStorageService.free',
                   side_effect=lambda uid: self.NO_FREE_SPACE if uid == self.uid else self.ENOUGH_SPACE):
            # Операция запускается успешно, т.к. проверки места делаются в самой операции.
            oid = self.json_ok('async_move',
                               {'uid': self.invited_uid_with_ignore, 'src': file_path, 'dst': new_path})['oid']
            operation_info = self.json_ok('status', {'uid': self.invited_uid_with_ignore, 'oid': oid})
            assert 'error' in operation_info, u'Операция не зафейлилась, но у Владельца нет места'
            assert 'title' in operation_info['error'], u'В информации о фейле нет деталей'
            assert OwnerHasNoFreeSpace.__name__ in operation_info['error']['title']

    def test_successully_uploading_to_shared_folder(self):
        path = '%s/fluffy_raccoon.jpg' % self.invited_folder_path
        quota = Quota()
        address = Address.Make(self.invited_uid_with_ignore, path)
        with patch('mpfs.core.services.disk_service.MPFSStorageService.free',
                   side_effect=lambda uid: self.NO_FREE_SPACE if uid == self.invited_uid_with_ignore else self.ENOUGH_SPACE):
            assert quota.free(address=address.get_parent()) == self.NO_FREE_SPACE
            assert quota.free_with_shared_support(address=address.get_parent()) == self.ENOUGH_SPACE

    def test_handle_set_group_size_not_fail_if_group_not_found(self):
        gid = uuid.uuid4().hex
        with patch('mpfs.core.job_handlers.routine.log.info') as mocked_log:
            handle_set_group_size.apply((gid,))
            assert mocked_log.called
            assert 'Unable to set group size' in mocked_log.call_args[0][0]

