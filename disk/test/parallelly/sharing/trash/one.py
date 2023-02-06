# -*- coding: utf-8 -*-
import re

import mock

from collections import defaultdict
from lxml import etree

from test.common.sharing import CommonSharingMethods
from test.base_suit import set_up_open_url, tear_down_open_url

import mpfs.engine.process
from mpfs.core.filesystem.dao.legacy import CollectionRoutedDatabase


db = CollectionRoutedDatabase()


class TrashOneSharingTestCase(CommonSharingMethods):

    def setup_method(self, method):
        super(TrashOneSharingTestCase, self).setup_method(method)
        self.json_ok('user_init', {'uid': self.uid_1})
        self.json_ok('user_init', {'uid': self.uid_3})
        self.make_dirs()

    def test_rm_folder_in_shared(self):
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid)
        self.activate_invite(uid=self.uid_3, hash=hsh)
        self.json_ok('mkdir', {'uid': self.uid_3, 'path': '/disk/folder2/folder31'})

        opts = {'uid': self.uid_3, 'path': '/disk/folder2/folder31'}
        open_url_data = set_up_open_url()
        self.mail_ok('rm', opts)
        tear_down_open_url()
        vals = defaultdict(dict)
#        self.fail(open_url_data)
        owner_found_deleted = False
        user_found_deleted = False
        for k, v in open_url_data.iteritems():
            if k.startswith('http://localhost/service/echo?uid='):
                notified_uid = re.search('http://localhost/service/echo\?uid=(\d+)', k).group(1)
                for each in v:
                    optag = etree.fromstring(each['pure_data'])
                    new_version = optag.get('new')
                    old_version = optag.get('old')
                    vals[notified_uid]['new'] = new_version
                    vals[notified_uid]['old'] = old_version
                    if optag.tag == 'diff':
                        if notified_uid == str(self.uid):
                            self.assertEqual(len(list(optag.iterfind('op'))), 1)
                            self.assertEqual(optag.find('op').get('key'), '/disk/new_folder/folder2/folder31')
                            self.assertEqual(optag.find('op').get('folder'), '/disk/new_folder/folder2/')
                            self.assertNotEqual(optag.find('op').get('resource_type'), None)
                            self.assertEqual(optag.find('op').get('type'), "deleted")
                            owner_found_deleted = True
                        elif notified_uid == str(self.uid_3):
                            self.assertEqual(len(list(optag.iterfind('op'))), 1)
                            for op in optag.iterfind('op'):
                                if op.get('key') == '/disk/folder2/folder31':
                                    self.assertEqual(op.get('type'), "deleted")
                                    self.assertEqual(op.get('folder'), '/disk/folder2/')
                                    self.assertNotEqual(op.get('resource_type'), None)
                                    user_found_deleted = True
                                else:
                                    self.fail()
                        else:
                            self.fail()

        self.assertTrue(owner_found_deleted)
        self.assertTrue(user_found_deleted)
        self.assertTrue(int(vals[str(self.uid)]['old']) < int(vals[str(self.uid)]['new']))
        self.assertTrue(int(vals[str(self.uid_3)]['old']) < int(vals[str(self.uid_3)]['new']))
        self.mail_error('info', opts)
        opts = {'uid': self.uid, 'path': '/disk/new_folder/folder2/folder31'}
        self.mail_error('info', opts)

        opts = {'uid': self.uid_3, 'path': '/disk/folder2/folder31'}
        mkdir_result = self.json_ok('mkdir', opts)

        opts = {'uid': self.uid, 'path': '/disk/new_folder/folder2/folder31'}
        self.mail_ok('rm', opts)
        self.assertNotEqual(db.groups.find_one({'owner': str(self.uid), 'path': '/disk/new_folder/folder2'}), None)
        self.assertNotEqual(db.user_data.find_one({'uid': str(self.uid), 'path': '/disk/new_folder/folder2'}), None)
        self.inspect_all()

    def test_group_file_trash_append(self):
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid)
        self.activate_invite(uid=self.uid_3, hash=hsh)
        self.upload_file(self.uid, '/disk/new_folder/folder2/file31')

        opts = {
            'uid': self.uid,
            'path': '/disk/new_folder/folder2/file31',
        }
        open_url_data = set_up_open_url()
        with mock.patch('mpfs.core.address.Address.add_trash_suffix'):
            self.mail_ok('async_trash_append', opts)
        tear_down_open_url()
        vals = defaultdict(list)

        owner_found_new = False
        owner_found_deleted = False
        user_found_deleted = False
        for k, v in open_url_data.iteritems():
            if k.startswith('http://localhost/service/echo?uid='):
                notified_uid = re.search('http://localhost/service/echo\?uid=(\d+)', k).group(1)
                for each in v:
                    optag = etree.fromstring(each['pure_data'])
                    if optag.tag == 'diff':
                        vals[notified_uid].append(etree.tostring(optag, pretty_print=True))
                        if notified_uid == str(self.uid):
                            self.assertEqual(len(list(optag.iterfind('op'))), 2)
                            for op in optag.iterfind('op'):
                                if op.get('key') == '/disk/new_folder/folder2/file31':
                                    self.assertEqual(op.get('folder'), '/disk/new_folder/folder2/')
                                    self.assertEqual(op.get('type'), "deleted")
                                    self.assertNotEqual(op.get('resource_type'), None)
                                    owner_found_deleted = True
                                elif op.get('key') == '/trash/file31':
                                    self.assertEqual(op.get('folder'), '/trash/')
                                    self.assertEqual(op.get('type'), "new")
                                    self.assertNotEqual(op.get('resource_type'), None)
                                    owner_found_new = True
                                else:
                                    self.fail()
                        elif notified_uid == str(self.uid_3):
                            self.assertEqual(len(list(optag.iterfind('op'))), 1)
                            for op in optag.iterfind('op'):
                                if op.get('key') == '/disk/folder2/file31':
                                    self.assertEqual(op.get('type'), "deleted")
                                    self.assertEqual(op.get('folder'), '/disk/folder2/')
                                    self.assertNotEqual(op.get('resource_type'), None)
                                    user_found_deleted = True
                                else:
                                    self.fail()
                        else:
                            self.fail()
        self.assertTrue(owner_found_new)
        self.assertTrue(owner_found_deleted)
        self.assertTrue(user_found_deleted)
        vals = dict(vals)
        self.assertTrue(vals)

        # проверка того, что каждому пришел 1 push
        for k, v in vals.iteritems():
            self.assertEqual(len(v), 1, v)
        self.inspect_all()

    def test_group_file_trash_restore(self):
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid)
        self.activate_invite(uid=self.uid_3, hash=hsh)
        self.upload_file(self.uid, '/disk/new_folder/folder2/file31')

        opts = {'uid': self.uid, 'path': '/disk/new_folder/folder2/file31'}
        with mock.patch('mpfs.core.address.Address.add_trash_suffix'):
            self.mail_ok('async_trash_append', opts)

        opts = {
            'uid': self.uid,
            'path': '/trash/file31',
        }
        open_url_data = set_up_open_url()
        self.mail_ok('async_trash_restore', opts)
        tear_down_open_url()
        vals = defaultdict(list)
#        self.fail(open_url_data)
        owner_found_new = False
        owner_found_deleted = False
        user_found_new = False
        for k, v in open_url_data.iteritems():
            if k.startswith('http://localhost/service/echo?uid='):
                notified_uid = re.search('http://localhost/service/echo\?uid=(\d+)', k).group(1)
                for each in v:
                    optag = etree.fromstring(each['pure_data'])
                    if optag.tag == 'diff':
                        vals[notified_uid].append(etree.tostring(optag, pretty_print=True))
                        if notified_uid == str(self.uid):
                            self.assertEqual(len(list(optag.iterfind('op'))), 2)
                            for op in optag.iterfind('op'):
                                if op.get('key') == '/disk/new_folder/folder2/file31':
                                    self.assertEqual(op.get('folder'), '/disk/new_folder/folder2/')
                                    self.assertEqual(op.get('type'), "new")
                                    self.assertNotEqual(op.get('resource_type'), None)
                                    owner_found_new = True
                                elif op.get('key') == '/trash/file31':
                                    self.assertEqual(op.get('folder'), '/trash/')
                                    self.assertEqual(op.get('type'), "deleted")
                                    self.assertNotEqual(op.get('resource_type'), None)
                                    owner_found_deleted = True
                                else:
                                    self.fail()
                        elif notified_uid == str(self.uid_3):
                            self.assertEqual(len(list(optag.iterfind('op'))), 1)
                            for op in optag.iterfind('op'):
                                if op.get('key') == '/disk/folder2/file31':
                                    self.assertEqual(op.get('type'), "new")
                                    self.assertEqual(op.get('folder'), '/disk/folder2/')
                                    self.assertNotEqual(op.get('resource_type'), None)
                                    user_found_new = True
                                else:
                                    self.fail()
                        else:
                            self.fail()

        self.assertTrue(owner_found_new)
        self.assertTrue(owner_found_deleted)
        self.assertTrue(user_found_new)
        vals = dict(vals)
        self.assertTrue(vals)

        # проверка того, что каждому пришел 1 push
        for k, v in vals.iteritems():
            self.assertEqual(len(v), 1, v)
        self.inspect_all()

    def test_shared_file_trash_append(self):
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid)
        self.activate_invite(uid=self.uid_3, hash=hsh)
        self.upload_file(self.uid_3, '/disk/folder2/file31')

        opts = {
            'uid': self.uid_3,
            'path': '/disk/folder2/file31',
        }
        open_url_data = set_up_open_url()
        with mock.patch('mpfs.core.address.Address.add_trash_suffix'):
            self.mail_ok('async_trash_append', opts)
        tear_down_open_url()
        vals = defaultdict(list)
        user_found_deleted = False
        user_found_new = False
        owner_found_deleted = False
        owner_found_new = False
        for k, v in open_url_data.iteritems():
            if k.startswith('http://localhost/service/echo?uid='):
                notified_uid = re.search('http://localhost/service/echo\?uid=(\d+)', k).group(1)
                for each in v:
                    optag = etree.fromstring(each['pure_data'])
                    if optag.tag == 'diff':
                        vals[notified_uid].append(etree.tostring(optag, pretty_print=True))
                        if notified_uid == str(self.uid):
                            self.assertEqual(len(list(optag.iterfind('op'))), 2)
                            for op in optag.iterfind('op'):
                                if op.get('key') == '/disk/new_folder/folder2/file31':
                                    self.assertEqual(op.get('folder'), '/disk/new_folder/folder2/')
                                    self.assertEqual(op.get('type'), "deleted")
                                    self.assertNotEqual(op.get('resource_type'), None)
                                    owner_found_deleted = True
                                elif op.get('key') == '/trash/file31':
                                    self.assertEqual(op.get('folder'), '/trash/')
                                    self.assertEqual(op.get('type'), "new")
                                    self.assertNotEqual(op.get('resource_type'), None)
                                    owner_found_new = True
                                else:
                                    self.fail()
                        elif notified_uid == str(self.uid_3):
                            self.assertEqual(len(list(optag.iterfind('op'))), 2)
                            for op in optag.iterfind('op'):
                                if op.get('key') == '/disk/folder2/file31':
                                    self.assertEqual(op.get('type'), "deleted")
                                    self.assertEqual(op.get('folder'), '/disk/folder2/')
                                    self.assertNotEqual(op.get('resource_type'), None)
                                    user_found_deleted = True
                                elif op.get('key') == '/trash/file31':
                                    self.assertEqual(op.get('folder'), '/trash/')
                                    self.assertEqual(op.get('type'), "new")
                                    self.assertNotEqual(op.get('resource_type'), None)
                                    user_found_new = True
                                else:
                                    self.fail()
                        else:
                            self.fail()

        self.assertTrue(owner_found_deleted)
        self.assertTrue(user_found_new)
        self.assertTrue(user_found_deleted)
        self.assertTrue(owner_found_new)
        vals = dict(vals)
        self.assertTrue(vals)

        # проверка того, что каждому пришел 1 push
        for k, v in vals.iteritems():
            self.assertEqual(len(v), 1, v)
        self.inspect_all()

    def test_shared_file_trash_restore(self):
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid)
        self.activate_invite(uid=self.uid_3, hash=hsh)
        self.upload_file(self.uid_3, '/disk/folder2/file31')

        opts = {'uid': self.uid_3, 'path': '/disk/folder2/file31'}
        with mock.patch('mpfs.core.address.Address.add_trash_suffix'):
            self.mail_ok('async_trash_append', opts)

        opts = {
            'uid': self.uid_3,
            'path': '/trash/file31',
        }
        open_url_data = set_up_open_url()
        self.mail_ok('async_trash_restore', opts)
        tear_down_open_url()
        vals = defaultdict(list)

        owner_found_new = False
        user_found_new = False
        user_found_deleted = False
        for k, v in open_url_data.iteritems():
            if k.startswith('http://localhost/service/echo?uid='):
                notified_uid = re.search('http://localhost/service/echo\?uid=(\d+)', k).group(1)
                for each in v:
                    optag = etree.fromstring(each['pure_data'])
                    if optag.tag == 'diff':
                        vals[notified_uid].append(etree.tostring(optag, pretty_print=True))
                        if notified_uid == str(self.uid):
                            self.assertEqual(len(list(optag.iterfind('op'))), 1)
                            for op in optag.iterfind('op'):
                                if op.get('key') == '/disk/new_folder/folder2/file31':
                                    self.assertEqual(op.get('folder'), '/disk/new_folder/folder2/')
                                    self.assertEqual(op.get('type'), "new")
                                    self.assertNotEqual(op.get('resource_type'), None)
                                    owner_found_new = True
                                else:
                                    self.fail()
                        elif notified_uid == str(self.uid_3):
                            self.assertEqual(len(list(optag.iterfind('op'))), 2)
                            for op in optag.iterfind('op'):
                                if op.get('key') == '/disk/folder2/file31':
                                    self.assertEqual(op.get('type'), "new")
                                    self.assertEqual(op.get('folder'), '/disk/folder2/')
                                    self.assertNotEqual(op.get('resource_type'), None)
                                    user_found_new = True
                                elif op.get('key') == '/trash/file31':
                                    self.assertEqual(op.get('folder'), '/trash/')
                                    self.assertEqual(op.get('type'), "deleted")
                                    self.assertNotEqual(op.get('resource_type'), None)
                                    user_found_deleted = True
                                else:
                                    self.fail()
                        else:
                            self.fail()
        self.assertTrue(owner_found_new)
        self.assertTrue(user_found_deleted)
        self.assertTrue(user_found_new)
        vals = dict(vals)
        self.assertTrue(vals)

        # проверка того, что каждому пришел 1 push
        for k, v in vals.iteritems():
            self.assertEqual(len(v), 1, v)
        self.inspect_all()
