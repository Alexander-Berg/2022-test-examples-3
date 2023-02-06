# -*- coding: utf-8 -*-
import re
import mock

from collections import defaultdict
from lxml import etree
from os import path

from test.common.sharing import CommonSharingMethods
from test.base_suit import set_up_open_url, tear_down_open_url

import mpfs.engine.process
from mpfs.core.filesystem.dao.legacy import CollectionRoutedDatabase


db = CollectionRoutedDatabase()


class PublicTrashSharingTestCase(CommonSharingMethods):
    def setup_method(self, method):
        super(PublicTrashSharingTestCase, self).setup_method(method)
        self.json_ok('user_init', {'uid': self.uid_1})
        self.json_ok('user_init', {'uid': self.uid_3})
        self.make_dirs()

    def test_public_shared_file_trash_append(self):
        """
        https://jira.yandex-team.ru/browse/CHEMODAN-11104
        """
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        name_public_file = 'public_file_1.txt'
        name_public_folder = 'public_folder'
        path_public_folder_user = '/disk/folder2/%s' % name_public_folder
        path_public_file_user = path_public_folder_user + '/%s' % name_public_file
        path_public_folder_owner = '/disk/new_folder/folder2/%s' % name_public_folder
        path_public_file_owner = path_public_folder_owner + '/%s' % name_public_file

        opts = {
            'uid': self.uid_3,
            'path': path_public_folder_user,
        }
        self.json_ok('mkdir', opts)
        opts = {
            'uid': self.uid_3,
            'path': path_public_folder_user,
        }
        self.json_ok('info', opts)
        self.upload_file(self.uid_3, path_public_file_user)

        original_file = self.publicate_resource(uid=self.uid_3, path=path_public_file_user)

        opts = {
            'uid': self.uid_3,
            'path': path_public_file_user,
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
                                if op.get('key') == path_public_file_owner:
                                    self.assertEqual(op.get('folder'), path_public_folder_owner + '/')
                                    self.assertEqual(op.get('type'), 'deleted')
                                    self.assertNotEqual(op.get('resource_type'), None)
                                    owner_found_deleted = True
                                elif op.get('key') == '/trash/%s' % name_public_file:
                                    self.assertEqual(op.get('folder'), '/trash/')
                                    self.assertEqual(op.get('type'), 'new')
                                    self.assertNotEqual(op.get('resource_type'), None)
                                    owner_found_new = True
                                else:
                                    self.fail()
                        elif notified_uid == str(self.uid_3):
                            self.assertEqual(len(list(optag.iterfind('op'))), 2)
                            for op in optag.iterfind('op'):
                                if op.get('key') == path_public_file_user:
                                    self.assertEqual(op.get('type'), 'deleted')
                                    self.assertEqual(op.get('folder'), path_public_folder_user + '/')
                                    self.assertNotEqual(op.get('resource_type'), None)
                                    user_found_deleted = True
                                elif op.get('key') == '/trash/%s' % name_public_file:
                                    self.assertEqual(op.get('folder'), '/trash/')
                                    self.assertEqual(op.get('type'), 'new')
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

        opts = {
            'uid': self.uid_3,
            'path': '/trash/%s' % name_public_file,
            'meta': 'file_id',
        }
        trash_file_user = self.json_ok('info', opts)
        opts = {
            'uid': self.uid,
            'path': '/trash/%s' % name_public_file,
            'meta': 'file_id',
        }
        trash_file_owner = self.json_ok('info', opts)
        self.assertEqual(trash_file_user['meta']['file_id'], original_file['meta']['file_id'])
        self.assertNotEqual(trash_file_user['meta']['file_id'], trash_file_owner['meta']['file_id'])

    def test_public_shared_file_trash_restore_user_first(self):
        """
            Восстановление файла приглашенным пользователем
        """
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        name_public_file = 'public_file_1.txt'
        name_public_folder = 'public_folder'
        path_public_folder_user = '/disk/folder2/%s' % name_public_folder
        path_public_file_user = path_public_folder_user + '/%s' % name_public_file
        path_public_folder_owner = '/disk/new_folder/folder2/%s' % name_public_folder
        path_public_file_owner = path_public_folder_owner + '/%s' % name_public_file

        opts = {
            'uid': self.uid_3,
            'path': path_public_folder_user,
        }
        self.json_ok('mkdir', opts)
        opts = {
            'uid': self.uid_3,
            'path': path_public_folder_user,
        }
        self.json_ok('info', opts)
        self.upload_file(self.uid_3, path_public_file_user)

        original_file = self.publicate_resource(uid=self.uid_3, path=path_public_file_user)
        original_public_hash = original_file['meta']['public_hash']

        opts = {
            'uid': self.uid_3,
            'path': path_public_file_user,
        }
        with mock.patch('mpfs.core.address.Address.add_trash_suffix'):
            self.mail_ok('async_trash_append', opts)

        opts = {
            'uid': self.uid_3,
            'path': '/trash/%s' % name_public_file,
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
                                if op.get('key') == path_public_file_owner:
                                    self.assertEqual(op.get('folder'), path_public_folder_owner + '/')
                                    self.assertEqual(op.get('type'), 'new')
                                    self.assertNotEqual(op.get('resource_type'), None)
                                    owner_found_new = True
                                else:
                                    self.fail()
                        elif notified_uid == str(self.uid_3):
                            self.assertEqual(len(list(optag.iterfind('op'))), 2)
                            for op in optag.iterfind('op'):
                                if op.get('key') == path_public_file_user:
                                    self.assertEqual(op.get('type'), 'new')
                                    self.assertEqual(op.get('folder'), path_public_folder_user + '/')
                                    self.assertNotEqual(op.get('resource_type'), None)
                                    user_found_new = True
                                elif op.get('key') == '/trash/%s' % name_public_file:
                                    self.assertEqual(op.get('folder'), '/trash/')
                                    self.assertEqual(op.get('type'), 'deleted')
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
        opts = {
            'uid': self.uid,
            'path': '/trash/%s' % name_public_file,
        }
        self.json_ok('info', opts)
        self.inspect_all()

        opts = {
            'uid': self.uid_3,
            'path': path_public_file_user,
            'meta': 'file_id,symlink,public,public_hash,short_url,short_url_named,download_counter',
        }
        restored_file = self.json_ok('info', opts)
        for k in ('public', 'public_hash', 'short_url', 'short_url_named',):
            self.assertTrue(k in restored_file['meta'], k)
        self.assertTrue(restored_file['meta']['public'], 1)
        link = db.link_data.find_one(
            {'uid': str(self.uid), 'key': '/%s' % restored_file['meta']['symlink'].split(':')[-1]})
        self.assertNotEqual(link, None)
        self.assertEqual(restored_file['meta']['public_hash'], original_public_hash)
        for k in ('public', 'public_hash', 'short_url', 'short_url_named',):
            self.assertTrue(k in restored_file['meta'], k)

    def test_public_shared_file_trash_restore_after_user(self):
        """
            Восстановление файла владельцем после восстановления пользователем
        """
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        name_public_file = 'public_file_1.txt'
        name_public_folder = 'public_folder'
        path_public_folder_user = '/disk/folder2/%s' % name_public_folder
        path_public_file_user = path_public_folder_user + '/%s' % name_public_file
        path_public_folder_owner = '/disk/new_folder/folder2/%s' % name_public_folder
        new_name_public_file = name_public_file + '_1234'

        opts = {
            'uid': self.uid_3,
            'path': path_public_folder_user,
        }
        self.json_ok('mkdir', opts)
        opts = {
            'uid': self.uid_3,
            'path': path_public_folder_user,
        }
        self.json_ok('info', opts)
        self.upload_file(self.uid_3, path_public_file_user)

        self.publicate_resource(uid=self.uid_3, path=path_public_file_user)

        opts = {
            'uid': self.uid_3,
            'path': path_public_file_user,
        }
        with mock.patch('mpfs.core.address.Address.add_trash_suffix'):
            self.mail_ok('async_trash_append', opts)

        link_count_before_owner = db.link_data.find({'uid': str(self.uid)}).count()
        link_count_before_user = db.link_data.find({'uid': str(self.uid_3)}).count()

        opts = {
            'uid': self.uid_3,
            'path': '/trash/%s' % name_public_file,
        }
        self.mail_ok('async_trash_restore', opts)

        opts = {'uid': self.uid,
                'path': '/trash/%s' % name_public_file,
                'name': new_name_public_file}
        open_url_data = set_up_open_url()
        self.json_ok('async_trash_restore', opts)
        tear_down_open_url()
        vals = defaultdict(list)

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
                                if op.get('key') == path_public_folder_owner + '/' + new_name_public_file:
                                    self.assertEqual(op.get('folder'), path_public_folder_owner + '/')
                                    self.assertEqual(op.get('type'), 'new')
                                    self.assertNotEqual(op.get('resource_type'), None)
                                    owner_found_new = True
                                elif op.get('key') == '/trash/%s' % name_public_file:
                                    self.assertEqual(op.get('folder'), '/trash/')
                                    self.assertEqual(op.get('type'), 'deleted')
                                    self.assertNotEqual(op.get('resource_type'), None)
                                    owner_found_deleted = True
                                else:
                                    self.fail(etree.tostring(op))
                        elif notified_uid == str(self.uid_3):
                            self.assertEqual(len(list(optag.iterfind('op'))), 1)
                            for op in optag.iterfind('op'):
                                if op.get('key') == path_public_folder_user + '/' + new_name_public_file:
                                    self.assertEqual(op.get('type'), 'new')
                                    self.assertEqual(op.get('folder'), path_public_folder_user + '/')
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
        opts = {
            'uid': self.uid,
            'path': '/trash/%s' % name_public_file,
        }
        self.json_error('info', opts, code=71)
        self.inspect_all()

        opts = {
            'uid': self.uid,
            'path': path_public_folder_owner + '/' + new_name_public_file,
            'meta': 'file_id,symlink,public,public_hash,short_url,short_url_named,download_counter',
        }
        restored_file = self.json_ok('info', opts)
        self.assertEqual(link_count_before_owner, db.link_data.find({'uid': str(self.uid)}).count())
        self.assertEqual(link_count_before_user, db.link_data.find({'uid': str(self.uid_3)}).count())
        for k in ('public', 'public_hash', 'short_url', 'short_url_named',):
            self.assertFalse(k in restored_file['meta'], k)

    def test_public_shared_file_trash_restore_owner_first(self):
        """
            Восстановление файла владельцем
        """
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        name_public_file = 'public_file_2.txt'
        name_public_folder = 'public_folder'
        path_public_folder_user = '/disk/folder2/%s' % name_public_folder
        path_public_file_user = path_public_folder_user + '/%s' % name_public_file
        path_public_folder_owner = '/disk/new_folder/folder2/%s' % name_public_folder
        path_public_file_owner = path_public_folder_owner + '/%s' % name_public_file

        self.json_ok('mkdir', {'uid': self.uid, 'path': path_public_folder_owner})
        self.upload_file(self.uid_3, path_public_file_user)

        file_info = self.publicate_resource(uid=self.uid_3, path=path_public_file_user)
        original_public_hash = file_info['meta']['public_hash']

        self.assertTrue('public' in file_info['meta'])
        self.assertTrue(file_info['meta']['public'], 1)
        opts = {
            'uid': self.uid_3,
            'path': path_public_file_user,
        }
        with mock.patch('mpfs.core.address.Address.add_trash_suffix'):
            self.mail_ok('async_trash_append', opts)
        opts = {
            'uid': self.uid_3,
            'path': '/trash/%s' % name_public_file,
        }
        self.json_ok('info', opts)
        opts = {
            'uid': self.uid,
            'path': '/trash/%s' % name_public_file,
        }
        self.json_ok('info', opts)

        opts = {
            'uid': self.uid,
            'path': '/trash/%s' % name_public_file,
        }
        open_url_data = set_up_open_url()
        self.mail_ok('async_trash_restore', opts)
        tear_down_open_url()
        vals = defaultdict(list)

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
                                if op.get('key') == path_public_file_owner:
                                    self.assertEqual(op.get('folder'), path_public_folder_owner + '/')
                                    self.assertEqual(op.get('type'), 'new')
                                    self.assertNotEqual(op.get('resource_type'), None)
                                    owner_found_new = True
                                elif op.get('key') == '/trash/%s' % name_public_file:
                                    self.assertEqual(op.get('folder'), '/trash/')
                                    self.assertEqual(op.get('type'), 'deleted')
                                    self.assertNotEqual(op.get('resource_type'), None)
                                    owner_found_deleted = True
                                else:
                                    self.fail()
                        elif notified_uid == str(self.uid_3):
                            self.assertEqual(len(list(optag.iterfind('op'))), 1)
                            for op in optag.iterfind('op'):
                                if op.get('key') == path_public_file_user:
                                    self.assertEqual(op.get('type'), 'new')
                                    self.assertEqual(op.get('folder'), path_public_folder_user + '/')
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
        opts = {
            'uid': self.uid_3,
            'path': '/trash/%s' % name_public_file,
        }
        self.json_ok('info', opts)
        self.inspect_all()
        opts = {
            'uid': self.uid,
            'path': path_public_file_owner,
            'meta': 'file_id,symlink,public,public_hash,short_url,short_url_named,download_counter',
        }
        restored_file = self.json_ok('info', opts)
        for k in ('public', 'public_hash', 'short_url', 'short_url_named',):
            self.assertTrue(k in restored_file['meta'], k)
        self.assertTrue(restored_file['meta']['public'], 1)
        self.assertEqual(restored_file['meta']['public_hash'], original_public_hash)

    def test_public_shared_file_trash_restore_after_owner(self):
        """
            Восстановление файла пользователем после восстановления владельцем
        """
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        name_public_file = 'public_file_2.txt'
        new_name_public_file = name_public_file + '_1234'
        name_public_folder = 'public_folder'
        path_public_folder_user = '/disk/folder2/%s' % name_public_folder
        path_public_file_user = path_public_folder_user + '/%s' % name_public_file
        path_public_folder_owner = '/disk/new_folder/folder2/%s' % name_public_folder

        self.json_ok('mkdir', {'uid': self.uid, 'path': path_public_folder_owner})
        self.upload_file(self.uid_3, path_public_file_user)

        self.publicate_resource(uid=self.uid_3, path=path_public_file_user)

        link_count_before_owner = db.link_data.find({'uid': str(self.uid)}).count()
        link_count_before_user = db.link_data.find({'uid': str(self.uid_3)}).count()

        opts = {
            'uid': self.uid_3,
            'path': path_public_file_user,
        }
        with mock.patch('mpfs.core.address.Address.add_trash_suffix'):
            self.mail_ok('async_trash_append', opts)

        opts = {
            'uid': self.uid,
            'path': '/trash/%s' % name_public_file,
        }
        self.mail_ok('async_trash_restore', opts)

        opts = {'uid': self.uid_3,
                'path': '/trash/%s' % name_public_file,
                'name': new_name_public_file}
        open_url_data = set_up_open_url()
        self.json_ok('async_trash_restore', opts)
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
                                if op.get('key') == path_public_folder_owner + '/' + new_name_public_file:
                                    self.assertEqual(op.get('folder'), path_public_folder_owner + '/')
                                    self.assertEqual(op.get('type'), 'new')
                                    self.assertNotEqual(op.get('resource_type'), None)
                                    owner_found_new = True
                                else:
                                    self.fail(etree.tostring(op))
                        elif notified_uid == str(self.uid_3):
                            self.assertEqual(len(list(optag.iterfind('op'))), 2)
                            for op in optag.iterfind('op'):
                                if op.get('key') == path_public_folder_user + '/' + new_name_public_file:
                                    self.assertEqual(op.get('type'), 'new')
                                    self.assertEqual(op.get('folder'), path_public_folder_user + '/')
                                    self.assertNotEqual(op.get('resource_type'), None)
                                    user_found_new = True
                                elif op.get('key') == '/trash/%s' % name_public_file:
                                    self.assertEqual(op.get('folder'), '/trash/')
                                    self.assertEqual(op.get('type'), 'deleted')
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
        opts = {
            'uid': self.uid_3,
            'path': '/trash/%s' % name_public_file,
        }
        self.json_error('info', opts, code=71)
        self.inspect_all()

        opts = {
            'uid': self.uid_3,
            'path': path_public_folder_user + '/' + new_name_public_file,
            'meta': '',
        }
        restored_file = self.json_ok('info', opts)
        for k in ('public', 'public_hash', 'short_url', 'short_url_named', 'published',):
            self.assertFalse(k in restored_file['meta'], k)
        self.assertEqual(link_count_before_owner, db.link_data.find({'uid': str(self.uid)}).count())
        self.assertEqual(link_count_before_user, db.link_data.find({'uid': str(self.uid_3)}).count())

    def test_public_shared_complicated_restoring(self):
        """
            https://jira.yandex-team.ru/browse/CHEMODAN-11104#comment-4098164
        """
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        name_public_file = 'public_file_2.txt'
        new_name_public_file = name_public_file + '_1234'
        name_public_folder = 'public_folder'
        path_public_folder_user = '/disk/folder2/%s' % name_public_folder
        path_public_file_user = path_public_folder_user + '/%s' % name_public_file
        path_public_folder_owner = '/disk/new_folder/folder2/%s' % name_public_folder

        self.json_ok('mkdir', {'uid': self.uid, 'path': path_public_folder_owner})
        self.upload_file(self.uid_3, path_public_file_user)

        self.publicate_resource(uid=self.uid_3, path=path_public_file_user)

        opts = {
            'uid': self.uid,
            'path': path_public_folder_owner + '/' + name_public_file,
            'meta': '',
        }
        file_info = self.json_ok('info', opts)
        for k in ('public', 'public_hash', 'short_url', 'short_url_named',):
            self.assertTrue(k in file_info['meta'], k)
        self.assertFalse('published' in file_info['meta'])

        # пользователь удаляет файл или каталог
        opts = {
            'uid': self.uid_3,
            'path': path_public_folder_user + '/' + name_public_file,
        }
        with mock.patch('mpfs.core.address.Address.add_trash_suffix'):
            self.async_ok('async_trash_append', opts)

        # у владельца и пользователя в корзине возникают копии одного и того же элемента,
        # но с разными file_id: у пользователя оригинальный, у владельца новый
        opts = {
            'uid': self.uid,
            'path': '/trash/' + name_public_file,
            'meta': ''
        }
        owner_info = self.json_ok('info', opts)

        opts = {
            'uid': self.uid_3,
            'path': '/trash/' + name_public_file,
            'meta': ''
        }
        user_info = self.json_ok('info', opts)
        self.assertNotEqual(owner_info['meta']['file_id'], user_info['meta']['file_id'])

        # восстанавливаем первый раз - владельцем
        opts = {
            'uid': self.uid,
            'path': '/trash/' + name_public_file,
        }
        self.json_ok('async_trash_restore', opts)

        # при восстановлении из корзины публичность восстанавливается только для первой восстановленной копии
        opts = {
            'uid': self.uid,
            'path': path_public_folder_owner + '/' + name_public_file,
            'meta': '',
        }
        file_info = self.json_ok('info', opts)
        for k in ('public', 'public_hash', 'short_url', 'short_url_named',):
            self.assertTrue(k in file_info['meta'], k)

        # восстанавливаем второй раз - пользователем
        opts = {
            'uid': self.uid_3,
            'path': '/trash/' + name_public_file,
            'name': new_name_public_file,
        }
        self.json_ok('async_trash_restore', opts)

        # вторая копия восстанавливается с новым именем и без расшаренности
        opts = {
            'uid': self.uid_3,
            'path': path_public_folder_user + '/' + new_name_public_file,
            'meta': '',
        }
        file_info = self.json_ok('info', opts)
        for k in ('public', 'public_hash', 'short_url', 'short_url_named',):
            self.assertFalse(k in file_info['meta'], k)

    def test_public_shared_folder_trash_append(self):
        """
            https://jira.yandex-team.ru/browse/CHEMODAN-11104
        """
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        name_public_file = 'public_file_1.txt'
        name_public_folder = 'public_folder'
        path_shared_folder_user = '/disk/folder2'
        path_public_folder_user = path_shared_folder_user + '/' + name_public_folder
        path_public_file_user = path_public_folder_user + '/%s' % name_public_file
        path_shared_folder_owner = '/disk/new_folder/folder2'
        path_public_folder_owner = path_shared_folder_owner + '/' + name_public_folder

        self.json_ok('mkdir', {'uid': self.uid_3, 'path': path_public_folder_user})
        self.upload_file(self.uid_3, path_public_file_user)
        original_file = self.publicate_resource(uid=self.uid_3, path=path_public_file_user)

        original_folder = self.publicate_resource(uid=self.uid_3, path=path_public_folder_user)

        opts = {
            'uid': self.uid_3,
            'path': path_public_folder_user,
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
        owner_found_new_content = False
        user_found_new_content = False
        for k, v in open_url_data.iteritems():
            if k.startswith('http://localhost/service/echo?uid='):
                notified_uid = re.search('http://localhost/service/echo\?uid=(\d+)', k).group(1)
                for each in v:
                    optag = etree.fromstring(each['pure_data'])
                    if optag.tag == 'diff':
                        vals[notified_uid].append(etree.tostring(optag, pretty_print=True))
                        if notified_uid == str(self.uid):
                            self.assertEqual(len(list(optag.iterfind('op'))), 3)
                            for op in optag.iterfind('op'):
                                if op.get('key') == path_public_folder_owner:
                                    self.assertTrue(op.get('folder').startswith(path_shared_folder_owner + '/'))
                                    self.assertEqual(op.get('type'), 'deleted')
                                    self.assertNotEqual(op.get('resource_type'), None)
                                    owner_found_deleted = True
                                elif op.get('key') == '/trash/%s' % name_public_folder:
                                    self.assertEqual(op.get('folder'), '/trash/')
                                    self.assertEqual(op.get('type'), 'new')
                                    self.assertNotEqual(op.get('resource_type'), None)
                                    owner_found_new = True
                                elif op.get('key').startswith('/trash/%s' % name_public_folder + '/'):
                                    self.assertEqual(op.get('type'), 'new')
                                    self.assertNotEqual(op.get('resource_type'), None)
                                    owner_found_new_content = True
                                else:
                                    self.fail()
                        elif notified_uid == str(self.uid_3):
                            self.assertEqual(len(list(optag.iterfind('op'))), 3)
                            for op in optag.iterfind('op'):
                                if op.get('key') == path_public_folder_user:
                                    self.assertEqual(op.get('type'), 'deleted')
                                    self.assertTrue(op.get('folder').startswith(path_shared_folder_user + '/'))
                                    self.assertNotEqual(op.get('resource_type'), None)
                                    user_found_deleted = True
                                elif op.get('key') == '/trash/%s' % name_public_folder:
                                    self.assertEqual(op.get('folder'), '/trash/')
                                    self.assertEqual(op.get('type'), 'new')
                                    self.assertNotEqual(op.get('resource_type'), None)
                                    user_found_new = True
                                elif op.get('key').startswith('/trash/%s' % name_public_folder + '/'):
                                    self.assertEqual(op.get('type'), 'new')
                                    self.assertNotEqual(op.get('resource_type'), None)
                                    user_found_new_content = True
                                else:
                                    self.fail()
                        else:
                            self.fail()

        self.assertTrue(owner_found_deleted)
        self.assertTrue(user_found_deleted)
        self.assertTrue(user_found_new)
        self.assertTrue(owner_found_new)
        self.assertTrue(owner_found_new_content)
        self.assertTrue(user_found_new_content)
        vals = dict(vals)
        self.assertTrue(vals)

        # проверка того, что каждому пришел 1 push
        for k, v in vals.iteritems():
            self.assertEqual(len(v), 1, v)
        self.inspect_all()

        opts = {
            'uid': self.uid_3,
            'path': '/trash/%s' % name_public_folder,
            'meta': 'file_id',
        }
        trash_folder_user = self.json_ok('info', opts)
        opts = {
            'uid': self.uid,
            'path': '/trash/%s' % name_public_folder,
            'meta': 'file_id',
        }
        trash_folder_owner = self.json_ok('info', opts)
        self.assertEqual(trash_folder_user['meta']['file_id'], original_folder['meta']['file_id'])
        self.assertNotEqual(trash_folder_user['meta']['file_id'], trash_folder_owner['meta']['file_id'])
        opts = {
            'uid': self.uid_3,
            'path': '/trash/%s/%s' % (name_public_folder, name_public_file),
            'meta': 'file_id',
        }
        trash_file_user = self.json_ok('info', opts)
        opts = {
            'uid': self.uid,
            'path': '/trash/%s/%s' % (name_public_folder, name_public_file),
            'meta': 'file_id',
        }
        trash_file_owner = self.json_ok('info', opts)
        self.assertEqual(trash_file_user['meta']['file_id'], original_file['meta']['file_id'])
        self.assertNotEqual(trash_file_user['meta']['file_id'], trash_file_owner['meta']['file_id'])

    def test_public_shared_folder_trash_restore_user_first(self):
        """Восстановление каталога приглашенным пользователем"""
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        name_public_file = 'public_file_1.txt'
        name_public_folder = 'public_folder'
        path_shared_folder_user = '/disk/folder2'
        path_public_folder_user = path.join(path_shared_folder_user, name_public_folder)
        path_public_file_user = path.join(path_public_folder_user, name_public_file)
        path_shared_folder_owner = '/disk/new_folder/folder2'
        path_public_folder_owner = path.join(path_shared_folder_owner, name_public_folder)

        self.json_ok('mkdir', {'uid': self.uid_3,
                               'path': path_public_folder_user})
        self.upload_file(self.uid_3, path_public_file_user)

        file_info = self.publicate_resource(uid=self.uid_3, path=path_public_file_user)
        original_public_hash_file = file_info['meta']['public_hash']

        folder_info = self.publicate_resource(uid=self.uid_3, path=path_public_folder_user)
        original_public_hash_folder = folder_info['meta']['public_hash']

        with mock.patch('mpfs.core.address.Address.add_trash_suffix'):
            self.mail_ok('async_trash_append', {'uid': self.uid_3,
                                                'path': path_public_folder_user})

        open_url_data = set_up_open_url()
        self.mail_ok('async_trash_restore', {'uid': self.uid_3,
                                             'path': '/trash/%s' % name_public_folder})
        tear_down_open_url()

        user_pushes = defaultdict(list)
        owner_found_new = False
        owner_found_new_content = False
        user_found_new = False
        user_found_deleted = False
        user_found_new_content = False
        for url, requests_data in open_url_data.iteritems():
            if not url.startswith('http://localhost/service/echo?uid='):
                continue

            notified_uid = re.search('http://localhost/service/echo\?uid=(\d+)', url).group(1)
            for request_data in requests_data:
                pure_data = etree.fromstring(request_data['pure_data'])
                if pure_data.tag != 'diff':
                    continue

                user_pushes[notified_uid].append(etree.tostring(pure_data, pretty_print=True))
                if notified_uid == str(self.uid):
                    self.assertEqual(len(list(pure_data.iterfind('op'))), 2)
                    for operation in pure_data.iterfind('op'):
                        if operation.get('key') == path_public_folder_owner:
                            self.assertEqual(operation.get('folder'), path_shared_folder_owner + '/')
                            self.assertEqual(operation.get('type'), 'new')
                            self.assertNotEqual(operation.get('resource_type'), None)
                            owner_found_new = True
                        elif operation.get('key').startswith(path_public_folder_owner + '/'):
                            self.assertEqual(operation.get('folder'), path_public_folder_owner + '/')
                            self.assertEqual(operation.get('type'), 'new')
                            owner_found_new_content = True
                        else:
                            self.fail(u'Не опознанная нотификация: uid=%s\n\tdata=%s' % (notified_uid,
                                                                                         request_data))
                elif notified_uid == str(self.uid_3):
                    self.assertEqual(len(list(pure_data.iterfind('op'))), 3)
                    for operation in pure_data.iterfind('op'):
                        if operation.get('key') == path_public_folder_user:
                            self.assertEqual(operation.get('folder'), path_shared_folder_user + '/')
                            self.assertEqual(operation.get('type'), 'new')
                            self.assertNotEqual(operation.get('resource_type'), None)
                            user_found_new = True
                        elif operation.get('key').startswith(path_public_folder_user + '/'):
                            self.assertEqual(operation.get('folder'), path_public_folder_user + '/')
                            self.assertEqual(operation.get('type'), 'new')
                            user_found_new_content = True
                        elif operation.get('key') == '/trash/%s' % name_public_folder:
                            self.assertEqual(operation.get('folder'), '/trash/')
                            self.assertEqual(operation.get('type'), 'deleted')
                            self.assertNotEqual(operation.get('resource_type'), None)
                            user_found_deleted = True
                        else:
                            self.fail(u'Не опознанная нотификация: uid=%s\n\tdata=%s' % (notified_uid,
                                                                                         request_data))
                else:
                    self.fail(u'Нотификация для не опознанного пользователя: uid=%s\n\tdata=%s' % (notified_uid,
                                                                                                   request_data))

        self.assertTrue(owner_found_new)
        self.assertTrue(user_found_deleted)
        self.assertTrue(user_found_new)
        self.assertTrue(owner_found_new_content)
        self.assertTrue(user_found_new_content)
        user_pushes = dict(user_pushes)
        self.assertTrue(user_pushes)

        # проверка того, что каждому пришел 1 push
        for url, requests_data in user_pushes.iteritems():
            self.assertEqual(len(requests_data), 1, requests_data)
        opts = {
            'uid': self.uid,
            'path': '/trash/%s' % name_public_folder,
        }
        self.json_ok('info', opts)
        self.inspect_all()

        opts = {
            'uid': self.uid_3,
            'path': path_public_folder_user,
            'meta': 'file_id,symlink,public,public_hash,short_url,short_url_named,download_counter',
        }
        restored_folder = self.json_ok('info', opts)
        for url in ('public', 'public_hash', 'short_url', 'short_url_named',):
            self.assertTrue(url in restored_folder['meta'], url)
        self.assertTrue(restored_folder['meta']['public'], 1)
        link = db.link_data.find_one({'uid': str(self.uid),
                                      'key': '/%s' % restored_folder['meta']['symlink'].split(':')[-1]})
        self.assertNotEqual(link, None)
        self.assertEqual(restored_folder['meta']['public_hash'], original_public_hash_folder)
        opts = {
            'uid': self.uid_3,
            'path': path_public_file_user,
            'meta': 'file_id,symlink,public,public_hash,short_url,short_url_named,download_counter',
        }
        restored_file = self.json_ok('info', opts)
        for url in ('public', 'public_hash', 'short_url', 'short_url_named',):
            self.assertTrue(url in restored_file['meta'], url)
        self.assertTrue(restored_file['meta']['public'], 1)
        link = db.link_data.find_one(
            {'uid': str(self.uid), 'key': '/%s' % restored_file['meta']['symlink'].split(':')[-1]})
        self.assertNotEqual(link, None)
        self.assertEqual(restored_file['meta']['public_hash'], original_public_hash_file)

    def test_public_shared_folder_trash_restore_after_user(self):
        """
            Восстановление каталога владельцем после восстановления пользователем
        """
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        name_public_file = 'public_file_1.txt'
        name_public_folder = 'public_folder'
        new_name_public_folder = name_public_folder + '_1234'
        name_public_folder = 'public_folder'
        path_shared_folder_user = '/disk/folder2'
        path_public_folder_user = '/disk/folder2/%s' % name_public_folder
        path_public_file_user = path_public_folder_user + '/%s' % name_public_file
        path_shared_folder_owner = '/disk/new_folder/folder2'

        self.json_ok('mkdir', {'uid': self.uid_3, 'path': path_public_folder_user})
        self.upload_file(self.uid_3, path_public_file_user)

        self.publicate_resource(uid=self.uid_3, path=path_public_file_user)

        self.publicate_resource(uid=self.uid_3, path=path_public_folder_user)

        opts = {
            'uid': self.uid_3,
            'path': path_public_folder_user,
        }
        with mock.patch('mpfs.core.address.Address.add_trash_suffix'):
            self.mail_ok('async_trash_append', opts)

        opts = {
            'uid': self.uid_3,
            'path': '/trash/%s' % name_public_folder,
        }
        self.mail_ok('async_trash_restore', opts)

        link_count_before_owner = db.link_data.find({'uid': str(self.uid)}).count()
        link_count_before_user = db.link_data.find({'uid': str(self.uid_3)}).count()

        opts = {'uid': self.uid,
                'path': '/trash/%s' % name_public_folder,
                'name': new_name_public_folder}
        open_url_data = set_up_open_url()
        self.json_ok('async_trash_restore', opts)
        tear_down_open_url()

        vals = defaultdict(list)

        owner_found_new = False
        owner_found_deleted = False
        user_found_new = False
        owner_found_new_content = False
        user_found_new_content = False
        for k, v in open_url_data.iteritems():
            if k.startswith('http://localhost/service/echo?uid='):
                notified_uid = re.search('http://localhost/service/echo\?uid=(\d+)', k).group(1)
                for each in v:
                    optag = etree.fromstring(each['pure_data'])
                    if optag.tag == 'diff':
                        vals[notified_uid].append(etree.tostring(optag, pretty_print=True))
                        if notified_uid == str(self.uid):
                            self.assertEqual(len(list(optag.iterfind('op'))), 3)
                            for op in optag.iterfind('op'):
                                if op.get('key') == path_shared_folder_owner + '/' + new_name_public_folder:
                                    self.assertEqual(op.get('folder'), path_shared_folder_owner + '/')
                                    self.assertEqual(op.get('type'), 'new')
                                    self.assertNotEqual(op.get('resource_type'), None)
                                    owner_found_new = True
                                elif op.get('key').startswith('%s/%s/' % (path_shared_folder_owner,
                                                                          new_name_public_folder)):
                                    self.assertTrue(op.get('folder').startswith(
                                        path_shared_folder_owner + '/' + new_name_public_folder + '/'))
                                    self.assertEqual(op.get('type'), 'new')
                                    owner_found_new_content = True
                                elif op.get('key') == '/trash/%s' % name_public_folder:
                                    self.assertEqual(op.get('folder'), '/trash/')
                                    self.assertEqual(op.get('type'), 'deleted')
                                    self.assertNotEqual(op.get('resource_type'), None)
                                    owner_found_deleted = True
                                else:
                                    self.fail(etree.tostring(op))
                        elif notified_uid == str(self.uid_3):
                            self.assertEqual(len(list(optag.iterfind('op'))), 2)
                            for op in optag.iterfind('op'):
                                if op.get('key') == path_shared_folder_user + '/' + new_name_public_folder:
                                    self.assertEqual(op.get('type'), 'new')
                                    self.assertEqual(op.get('folder'), path_shared_folder_user + '/')
                                    self.assertNotEqual(op.get('resource_type'), None)
                                    user_found_new = True
                                elif op.get('key').startswith('%s/%s/' % (path_shared_folder_user,
                                                                          new_name_public_folder)):
                                    self.assertTrue(op.get('folder').startswith(
                                        path_shared_folder_user + '/' + new_name_public_folder + '/'))
                                    self.assertEqual(op.get('type'), 'new')
                                    user_found_new_content = True
                                else:
                                    self.fail()
                        else:
                            self.fail()

        self.assertTrue(owner_found_new)
        self.assertTrue(owner_found_deleted)
        self.assertTrue(user_found_new)
        self.assertTrue(owner_found_new_content)
        self.assertTrue(user_found_new_content)
        vals = dict(vals)
        self.assertTrue(vals)

        # проверка того, что каждому пришел 1 push
        for k, v in vals.iteritems():
            self.assertEqual(len(v), 1, v)

        opts = {
            'uid': self.uid,
            'path': '/trash/%s' % name_public_folder,
        }
        self.json_error('info', opts, code=71)
        self.inspect_all()
        opts = {
            'uid': self.uid,
            'path': path_shared_folder_owner + '/' + new_name_public_folder,
            'meta': 'file_id,symlink,public,public_hash,short_url,short_url_named,download_counter',
        }
        restored_folder = self.json_ok('info', opts)
        for k in ('public', 'public_hash', 'short_url', 'short_url_named',):
            self.assertFalse(k in restored_folder['meta'], k)

        opts = {
            'uid': self.uid,
            'path': '/trash/%s/%s' % (name_public_folder, name_public_file),
        }
        self.json_error('info', opts, code=70)
        self.inspect_all()
        opts = {
            'uid': self.uid,
            'path': path_shared_folder_owner + '/' + new_name_public_folder + '/' + name_public_file,
            'meta': 'file_id,symlink,public,public_hash,short_url,short_url_named,download_counter',
        }
        restored_file = self.json_ok('info', opts)
        for k in ('public', 'public_hash', 'short_url', 'short_url_named',):
            self.assertFalse(k in restored_file['meta'], k)

        self.assertEqual(link_count_before_owner, db.link_data.find({'uid': str(self.uid)}).count())
        self.assertEqual(link_count_before_user, db.link_data.find({'uid': str(self.uid_3)}).count())

    def test_public_shared_folder_trash_restore_owner_first(self):
        """
            Восстановление каталога владельцем
        """
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        name_public_file = 'public_file_1.txt'
        name_public_folder = 'public_folder'
        path_shared_folder_user = '/disk/folder2'
        path_public_folder_user = '/disk/folder2/%s' % name_public_folder
        path_public_file_user = path_public_folder_user + '/%s' % name_public_file
        path_shared_folder_owner = '/disk/new_folder/folder2'
        path_public_folder_owner = '/disk/new_folder/folder2/%s' % name_public_folder

        self.json_ok('mkdir', {'uid': self.uid_3, 'path': path_public_folder_user})
        self.upload_file(self.uid_3, path_public_file_user)

        file_info = self.publicate_resource(uid=self.uid_3, path=path_public_file_user)

        folder_info = self.publicate_resource(uid=self.uid_3, path=path_public_folder_user)

        link_count_before_owner = db.link_data.find({'uid': str(self.uid)}).count()
        link_count_before_user = db.link_data.find({'uid': str(self.uid_3)}).count()

        opts = {
            'uid': self.uid_3,
            'path': path_public_folder_user,
            'meta': 'file_id,symlink,public,public_hash,short_url,short_url_named,download_counter',
        }
        original_folder = self.json_ok('info', opts)
        for k in ('public', 'public_hash', 'short_url', 'short_url_named',):
            self.assertTrue(k in original_folder['meta'], k)
        self.assertTrue(original_folder['meta']['public'], 1)

        opts = {
            'uid': self.uid_3,
            'path': path_public_folder_user,
        }
        with mock.patch('mpfs.core.address.Address.add_trash_suffix'):
            self.mail_ok('async_trash_append', opts)

        opts = {
            'uid': self.uid_3,
            'path': '/trash/' + name_public_folder,
        }
        self.json_ok('info', opts)
        opts = {
            'uid': self.uid,
            'path': '/trash/' + name_public_folder,
        }
        self.json_ok('info', opts)

        opts = {
            'uid': self.uid,
            'path': '/trash/%s' % name_public_folder,
        }
        open_url_data = set_up_open_url()
        self.json_ok('async_trash_restore', opts)
        tear_down_open_url()

        vals = defaultdict(list)
        owner_found_new = False
        owner_found_deleted = False
        user_found_new = False
        owner_found_new_content = False
        user_found_new_content = False
        for k, v in open_url_data.iteritems():
            if k.startswith('http://localhost/service/echo?uid='):
                notified_uid = re.search('http://localhost/service/echo\?uid=(\d+)', k).group(1)
                for each in v:
                    optag = etree.fromstring(each['pure_data'])
                    if optag.tag == 'diff':
                        vals[notified_uid].append(etree.tostring(optag, pretty_print=True))
                        if notified_uid == str(self.uid):
                            self.assertEqual(len(list(optag.iterfind('op'))), 3)
                            for op in optag.iterfind('op'):
                                if op.get('key') == path_shared_folder_owner + '/' + name_public_folder:
                                    self.assertEqual(op.get('folder'), path_shared_folder_owner + '/')
                                    self.assertEqual(op.get('type'), 'new')
                                    self.assertNotEqual(op.get('resource_type'), None)
                                    owner_found_new = True
                                elif op.get('key').startswith(
                                                                path_shared_folder_owner + '/' + name_public_folder + '/'):
                                    self.assertTrue(op.get('folder').startswith(
                                        path_shared_folder_owner + '/' + name_public_folder + '/'))
                                    self.assertEqual(op.get('type'), 'new')
                                    owner_found_new_content = True
                                elif op.get('key') == '/trash/%s' % name_public_folder:
                                    self.assertEqual(op.get('folder'), '/trash/')
                                    self.assertEqual(op.get('type'), 'deleted')
                                    self.assertNotEqual(op.get('resource_type'), None)
                                    owner_found_deleted = True
                                else:
                                    self.fail(etree.tostring(op))
                        elif notified_uid == str(self.uid_3):
                            self.assertEqual(len(list(optag.iterfind('op'))), 2)
                            for op in optag.iterfind('op'):
                                if op.get('key') == path_shared_folder_user + '/' + name_public_folder:
                                    self.assertEqual(op.get('type'), 'new')
                                    self.assertEqual(op.get('folder'), path_shared_folder_user + '/')
                                    self.assertNotEqual(op.get('resource_type'), None)
                                    user_found_new = True
                                elif op.get('key').startswith(path_shared_folder_user + '/' + name_public_folder + '/'):
                                    self.assertTrue(op.get('folder').startswith(
                                        path_shared_folder_user + '/' + name_public_folder + '/'))
                                    self.assertEqual(op.get('type'), 'new')
                                    user_found_new_content = True
                                else:
                                    self.fail()
                        else:
                            self.fail()

        self.assertTrue(owner_found_new)
        self.assertTrue(owner_found_deleted)
        self.assertTrue(user_found_new)
        self.assertTrue(owner_found_new_content)
        self.assertTrue(user_found_new_content)
        vals = dict(vals)
        self.assertTrue(vals)

        # проверка того, что каждому пришел 1 push
        for k, v in vals.iteritems():
            self.assertEqual(len(v), 1, v)
        opts = {
            'uid': self.uid,
            'path': '/trash/%s' % name_public_file,
        }
        self.json_error('info', opts, code=71)
        self.inspect_all()

        self.assertEqual(link_count_before_owner, db.link_data.find({'uid': str(self.uid)}).count())
        self.assertEqual(link_count_before_user, db.link_data.find({'uid': str(self.uid_3)}).count())

        opts = {
            'uid': self.uid,
            'path': path_shared_folder_owner + '/' + name_public_folder,
            'meta': 'file_id,symlink,public,public_hash,short_url,short_url_named,download_counter',
        }
        restored_folder = self.json_ok('info', opts)
        for k in ('public', 'public_hash', 'short_url', 'short_url_named',):
            self.assertTrue(k in restored_folder['meta'], k)
        self.assertTrue('public_hash' in restored_folder['meta'])
        self.assertEqual(restored_folder['meta']['public_hash'], original_folder['meta']['public_hash'])

    def test_public_shared_folder_trash_restore_after_owner(self):
        """
            Восстановление каталога пользователем после восстановления владельцем
        """
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        name_public_file = 'public_file_2.txt'
        name_public_folder = 'public_folder'
        new_name_public_folder = name_public_folder + '_5678'
        name_public_folder = 'public_folder'
        path_shared_folder_user = '/disk/folder2'
        path_public_folder_user = '/disk/folder2/%s' % name_public_folder
        path_public_file_user = path_public_folder_user + '/%s' % name_public_file
        path_shared_folder_owner = '/disk/new_folder/folder2'

        self.json_ok('mkdir', {'uid': self.uid_3, 'path': path_public_folder_user})
        self.upload_file(self.uid_3, path_public_file_user)

        self.publicate_resource(uid=self.uid_3, path=path_public_folder_user)

        opts = {
            'uid': self.uid_3,
            'path': path_public_folder_user,
        }
        with mock.patch('mpfs.core.address.Address.add_trash_suffix'):
            self.mail_ok('async_trash_append', opts)

        opts = {
            'uid': self.uid,
            'path': '/trash/%s' % name_public_folder,
        }
        self.json_ok('async_trash_restore', opts)

        link_count_before_owner = db.link_data.find({'uid': str(self.uid)}).count()
        link_count_before_user = db.link_data.find({'uid': str(self.uid_3)}).count()

        opts = {'uid': self.uid_3,
                'path': '/trash/%s' % name_public_folder,
                'name': new_name_public_folder}

        open_url_data = set_up_open_url()
        self.json_ok('async_trash_restore', opts)
        tear_down_open_url()

        vals = defaultdict(list)
        owner_found_new = False
        owner_found_deleted = False
        user_found_new = False
        owner_found_new_content = False
        user_found_new_content = False
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
                                if op.get('key') == path_shared_folder_owner + '/' + new_name_public_folder:
                                    self.assertEqual(op.get('folder'), path_shared_folder_owner + '/')
                                    self.assertEqual(op.get('type'), 'new')
                                    self.assertNotEqual(op.get('resource_type'), None)
                                    owner_found_new = True
                                elif op.get('key').startswith('%s/%s/' % (path_shared_folder_owner,
                                                                          new_name_public_folder)):
                                    self.assertTrue(op.get('folder').startswith(
                                        path_shared_folder_owner + '/' + new_name_public_folder + '/'))
                                    self.assertEqual(op.get('type'), 'new')
                                    owner_found_new_content = True
                                else:
                                    self.fail(etree.tostring(op))
                        elif notified_uid == str(self.uid_3):
                            self.assertEqual(len(list(optag.iterfind('op'))), 3)
                            for op in optag.iterfind('op'):
                                if op.get('key') == path_shared_folder_user + '/' + new_name_public_folder:
                                    self.assertEqual(op.get('type'), 'new')
                                    self.assertEqual(op.get('folder'), path_shared_folder_user + '/')
                                    self.assertNotEqual(op.get('resource_type'), None)
                                    user_found_new = True
                                elif op.get('key').startswith('%s/%s/' % (path_shared_folder_user,
                                                                          new_name_public_folder)):
                                    self.assertTrue(op.get('folder').startswith(
                                        path_shared_folder_user + '/' + new_name_public_folder + '/'))
                                    self.assertEqual(op.get('type'), 'new')
                                    user_found_new_content = True
                                elif op.get('key') == '/trash/%s' % name_public_folder:
                                    self.assertEqual(op.get('folder'), '/trash/')
                                    self.assertEqual(op.get('type'), 'deleted')
                                    self.assertNotEqual(op.get('resource_type'), None)
                                    owner_found_deleted = True
                                else:
                                    self.fail()
                        else:
                            self.fail()

        self.assertTrue(owner_found_new)
        self.assertTrue(owner_found_deleted)
        self.assertTrue(user_found_new)
        self.assertTrue(owner_found_new_content)
        self.assertTrue(user_found_new_content)
        vals = dict(vals)
        self.assertTrue(vals)

        # проверка того, что каждому пришел 1 push
        for k, v in vals.iteritems():
            self.assertEqual(len(v), 1, v)
        opts = {
            'uid': self.uid_3,
            'path': '/trash/%s' % name_public_file,
        }
        self.json_error('info', opts, code=71)
        self.inspect_all()

        opts = {
            'uid': self.uid_3,
            'path': path_shared_folder_user + '/' + new_name_public_folder,
            'meta': 'file_id,symlink,public,public_hash,short_url,short_url_named,download_counter',
        }
        restored_folder = self.json_ok('info', opts)
        for k in ('public', 'public_hash', 'short_url', 'short_url_named',):
            self.assertFalse(k in restored_folder['meta'], k)
        self.assertEqual(link_count_before_owner, db.link_data.find({'uid': str(self.uid)}).count())
        self.assertEqual(link_count_before_user, db.link_data.find({'uid': str(self.uid_3)}).count())
