# -*- coding: utf-8 -*-
import re
import pytest

from collections import defaultdict

from hamcrest import assert_that
from hamcrest import has_item
from lxml import etree
import mock
from nose_parameterized import parameterized

from mpfs.core.metastorage.control import fs_locks
from test.common.sharing import CommonSharingMethods
from test.base_suit import set_up_open_url, tear_down_open_url

import mpfs.engine.process
from test.helpers.stubs.services import PushServicesStub
from mpfs.core.filesystem.dao.legacy import CollectionRoutedDatabase
from test.conftest import INIT_USER_IN_POSTGRES
from mpfs.dao.session import Session


db = CollectionRoutedDatabase()


class MoveOneSharingTestCase(CommonSharingMethods):

    def setup_method(self, method):
        super(MoveOneSharingTestCase, self).setup_method(method)
        self.json_ok('user_init', {'uid': self.uid_1})
        self.json_ok('user_init', {'uid': self.uid_3})
        self.make_dirs()

    def test_make_move_in_shared(self):
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        opts = {
            'uid': self.uid_3,
            'path': '/disk/folder2/folder40',
        }
        self.json_ok('mkdir', opts)
        opts = {
            'uid': self.uid_3,
            'path': '/disk/folder2/folder40/folder50',
        }
        self.json_ok('mkdir', opts)
        self.upload_file(self.uid_3, '/disk/folder2/folder40/file41')

        opts = {
            'uid': self.uid,
        }
        owner_diff_before = self.json_ok('diff', opts)
        opts = {
            'uid': self.uid_3,
        }
        user_diff_before = self.json_ok('diff', opts)

        opts = {
            'uid': self.uid_3,
            'src': '/disk/folder2/folder40/file41',
            'dst': '/disk/folder2/folder40/folder50/file41',
        }
        open_url_data = set_up_open_url()
        self.json_ok('async_move', opts)
        tear_down_open_url()

        new_version_owner = None
        new_version_user = None
        new_found_user = False
        deleted_found_user = False
        new_found_owner = False
        deleted_found_owner = False
        for k, v in open_url_data.iteritems():
            if k.startswith('http://localhost/service/echo?uid='):
                notified_uid = re.search('http://localhost/service/echo\?uid=(\d+)', k).group(1)
                if notified_uid == self.uid_3:
                    for each in v:
                        event = etree.fromstring(each['pure_data'])
                        if event.tag == 'diff':
                            new_version_user = int(event.get('new'))
                            ops = list(event.iterfind('op'))
                            self.assertEqual(len(ops), 2)
                            for op in ops:
                                if op.get('type') == 'deleted':
                                    self.assertEqual(op.get('key'), '/disk/folder2/folder40/file41')
                                    deleted_found_user = True
                                elif op.get('type') == 'new':
                                    self.assertEqual(op.get('key'), '/disk/folder2/folder40/folder50/file41')
                                    new_found_user = True
                                else:
                                    self.fail(etree.tostring(event))
                elif notified_uid == self.uid:
                    for each in v:
                        event = etree.fromstring(each['pure_data'])
                        if event.tag == 'diff':
                            new_version_owner = int(event.get('new'))
                            ops = list(event.iterfind('op'))
                            self.assertEqual(len(ops), 2)
                            for op in ops:
                                if op.get('type') == 'deleted':
                                    self.assertEqual(op.get('key'), '/disk/new_folder/folder2/folder40/file41')
                                    deleted_found_owner = True
                                elif op.get('type') == 'new':
                                    self.assertEqual(op.get('key'), '/disk/new_folder/folder2/folder40/folder50/file41')
                                    new_found_owner = True
                                else:
                                    self.fail(etree.tostring(event))
        self.assertTrue(new_found_owner)
        self.assertTrue(deleted_found_owner)
        self.assertTrue(new_found_user)
        self.assertTrue(deleted_found_user)

        opts = {
            'uid': self.uid,
            'version': owner_diff_before['version'],
        }
        owner_diff_after = self.json_ok('diff', opts)
        self.assertEqual(new_version_owner, int(owner_diff_after['version']))
        opts = {
            'uid': self.uid_3,
            'version': user_diff_before['version']
        }
        user_diff_after = self.json_ok('diff', opts)
        self.assertEqual(new_version_user, int(user_diff_after['version']))
        self.inspect_all()

    def test_make_move_in_group(self):
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid)
        self.activate_invite(uid=self.uid_3, hash=hsh)
        self.json_ok('mkdir', {'uid': self.uid_3, 'path': '/disk/folder2/folder40'})

        opts = {
            'uid': self.uid_3,
            'path': '/disk/folder2/folder40',
        }
        self.json_ok('async_trash_append', opts)
        opts = {
            'uid': self.uid,
            'path': '/disk/new_folder/folder2/folder40',
        }
        self.json_ok('mkdir', opts)
        opts = {
            'uid': self.uid,
            'path': '/disk/new_folder/folder2/folder40/folder50',
        }
        self.json_ok('mkdir', opts)
        self.upload_file(self.uid, '/disk/new_folder/folder2/folder40/file41')

        opts = {
            'uid': self.uid,
        }
        owner_diff_before = self.json_ok('diff', opts)
        opts = {
            'uid': self.uid_3,
        }
        user_diff_before = self.json_ok('diff', opts)

        opts = {
            'uid': self.uid,
            'src': '/disk/new_folder/folder2/folder40/file41',
            'dst': '/disk/new_folder/folder2/folder40/folder50/file41',
        }
        open_url_data = set_up_open_url()
        self.json_ok('async_move', opts)
        tear_down_open_url()

        opts = {
            'uid': self.uid,
            'path': '/disk/new_folder/folder2/folder40/folder50/file41',
        }
        self.json_ok('info', opts)

        new_version_owner = None
        new_version_user = None
        new_found_user = False
        deleted_found_user = False
        new_found_owner = False
        deleted_found_owner = False
        for k, v in open_url_data.iteritems():
            if k.startswith('http://localhost/service/echo?uid='):
                notified_uid = re.search('http://localhost/service/echo\?uid=(\d+)', k).group(1)
                if notified_uid == self.uid_3:
                    for each in v:
                        event = etree.fromstring(each['pure_data'])
                        if event.tag == 'diff':
                            new_version_user = int(event.get('new'))
                            ops = list(event.iterfind('op'))
                            self.assertEqual(len(ops), 2)
                            for op in ops:
                                if op.get('type') == 'deleted':
                                    self.assertEqual(op.get('key'), '/disk/folder2/folder40/file41')
                                    deleted_found_user = True
                                elif op.get('type') == 'new':
                                    self.assertEqual(op.get('key'), '/disk/folder2/folder40/folder50/file41')
                                    new_found_user = True
                                else:
                                    self.fail()
                elif notified_uid == self.uid:
                    for each in v:
                        event = etree.fromstring(each['pure_data'])
                        if event.tag == 'diff':
                            new_version_owner = int(event.get('new'))
                            ops = list(event.iterfind('op'))
                            self.assertEqual(len(ops), 2)
                            for op in ops:
                                if op.get('type') == 'deleted':
                                    self.assertEqual(op.get('key'), '/disk/new_folder/folder2/folder40/file41')
                                    deleted_found_owner = True
                                elif op.get('type') == 'new':
                                    self.assertEqual(op.get('key'), '/disk/new_folder/folder2/folder40/folder50/file41')
                                    new_found_owner = True
                                else:
                                    self.fail()
                else:
                    self.fail()
        self.assertTrue(new_found_user)
        self.assertTrue(deleted_found_user)
        self.assertTrue(new_found_owner)
        self.assertTrue(deleted_found_owner)

        opts = {
            'uid': self.uid,
            'version': owner_diff_before['version'],
        }
        owner_diff_after = self.json_ok('diff', opts)
        self.assertEqual(len(owner_diff_after['result']), 2)
        self.assertEqual(new_version_owner, int(owner_diff_after['version']))

        opts = {
            'uid': self.uid_3,
            'version': user_diff_before['version']
        }
        user_diff_after = self.json_ok('diff', opts)
        self.assertEqual(len(user_diff_after['result']), 2)
        self.assertEqual(new_version_user, int(user_diff_after['version']))

        self.assertEqual(len(owner_diff_after['result']), len(user_diff_after['result']))
        opts = {
            'uid': self.uid,
            'path': '/disk/new_folder/folder2/folder40',
        }
        self.json_ok('async_trash_append', opts)
        self.inspect_all()

    def test_move_file_from_shared(self):
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid)
        self.activate_invite(uid=self.uid_3, hash=hsh)
        self.upload_file(self.uid_3, '/disk/folder2/file31')

        opts = {'uid': self.uid, 'path': '/disk/new_folder/folder2/file31'}
        self.mail_ok('info', opts)
        opts = {'uid': self.uid_3, 'path': '/disk/folder2/file31'}
        self.mail_ok('info', opts)
        opts = {
            'uid': self.uid_3,
            'src': '/disk/folder2/file31',
            'dst': '/disk/file31',
            'force': 1,
        }
        open_url_data = set_up_open_url()
        self.mail_ok('move', opts)
        tear_down_open_url()
        vals = defaultdict(list)

        owner_found_deleted = False
        user_found_deleted = False
        user_found_new = False
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
                                    self.assertEqual(op.get('type'), "deleted")
                                    self.assertNotEqual(op.get('resource_type'), None)
                                    owner_found_deleted = True
                                else:
                                    self.fail(etree.tostring(op))
                        elif notified_uid == str(self.uid_3):
                            self.assertEqual(len(list(optag.iterfind('op'))), 2)
                            for op in optag.iterfind('op'):
                                if op.get('key') == '/disk/folder2/file31':
                                    self.assertEqual(op.get('type'), "deleted")
                                    self.assertEqual(op.get('folder'), '/disk/folder2/')
                                    self.assertNotEqual(op.get('resource_type'), None)
                                    user_found_deleted = True
                                else:
                                    self.assertEqual(op.get('key'), '/disk/file31')
                                    self.assertEqual(op.get('type'), "new")
                                    self.assertEqual(op.get('folder'), '/disk/')
                                    self.assertNotEqual(op.get('resource_type'), None)
                                    user_found_new = True
                        else:
                            self.fail(etree.tostring(optag))
        self.assertTrue(owner_found_deleted)
        self.assertTrue(user_found_deleted)
        self.assertTrue(user_found_new)
        vals = dict(vals)
        self.assertTrue(vals)

        #=======================================================================
        # проверка того, что каждому пришел 1 push
        for k, v in vals.iteritems():
            self.assertEqual(len(v), 1, v)
        #=======================================================================

        opts = {'uid': self.uid_3, 'path': '/disk/folder2/file31'}
        self.mail_error('info', opts)
        opts = {'uid': self.uid_3, 'path': '/disk/file31'}
        self.mail_ok('info', opts)
        opts = {'uid': self.uid, 'path': '/disk/new_folder/folder2/file31'}
        self.mail_error('info', opts)
        opts = {'uid': self.uid_3, 'path': '/disk/file31'}
        file_info = self.mail_ok('info', opts)

        self.assertEqual(file_info.find('file').find('meta').find('group'), None)
        self.assertEqual(file_info.find('file').find('meta').find('with_shared'), None)
        self.inspect_all()

    def test_move_folder_from_shared(self):
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        opts = {
            'uid': self.uid_3,
            'path': '/disk/folder2/folder_for_move',
        }
        self.json_ok('mkdir', opts)
        opts = {
            'uid': self.uid_3,
            'path': '/disk/folder2/folder_for_move/inner_folder',
        }
        self.json_ok('mkdir', opts)
        self.upload_file(self.uid_3, '/disk/folder2/folder_for_move/inner_file')
        self.upload_file(self.uid_3, '/disk/folder2/folder_for_move/inner_folder/hidden_file')
        opts = {
            'uid': self.uid_3,
            'path': '/disk/folder2/folder_for_move',
        }
        self.mail_ok('list', opts)
        self.inspect_all()
        opts = {
            'uid': self.uid_3,
            'src': '/disk/folder2/folder_for_move',
            'dst': '/disk/folder_for_move',
            'force': 1,
        }
        open_url_data = set_up_open_url()
        self.mail_ok('move', opts)
        tear_down_open_url()
        opts = {
            'uid': self.uid_3,
            'path': '/disk/folder_for_move',
        }
        folder_list = self.mail_ok('list', opts).find('folder-list')

        self.assertEqual(folder_list.find('folder').find('meta').find('group'), None)
        self.assertEqual(folder_list.find('folder').find('id').text, '/disk/folder_for_move')
        self.assertTrue(len(list(folder_list.find('folder').find('folder-list').iterchildren())) > 0)
        opts = {
            'uid': self.uid_3,
            'path': '/disk/folder_for_move/inner_folder',
        }
        folder_list = self.mail_ok('list', opts).find('folder-list')

        self.assertEqual(folder_list.find('folder').find('meta').find('group'), None)
        self.assertEqual(folder_list.find('folder').find('id').text, '/disk/folder_for_move/inner_folder')
        self.assertTrue(len(list(folder_list.find('folder').find('folder-list').iterchildren())) > 0)
        opts = {
            'uid': self.uid_3,
            'src': '/disk/folder_for_move',
            'dst': '/disk/folder2/folder_for_move',
            'force': 1,
        }
        self.inspect_all()
        open_url_data = set_up_open_url()
        self.mail_ok('move', opts)
        tear_down_open_url()
        opts = {
            'uid': self.uid_3,
            'path': '/disk/folder2/folder_for_move',
        }
        folder_list = self.mail_ok('list', opts).find('folder-list')

        self.assertNotEqual(folder_list.find('folder').find('meta').find('group'), None)
        self.assertEqual(folder_list.find('folder').find('id').text, '/disk/folder2/folder_for_move')
        self.assertTrue(len(list(folder_list.find('folder').find('folder-list').iterchildren())) > 0)
        opts = {
            'uid': self.uid_3,
            'path': '/disk/folder2/folder_for_move/inner_folder',
        }
        folder_list = self.mail_ok('list', opts).find('folder-list')

        self.assertNotEqual(folder_list.find('folder').find('meta').find('group'), None)
        self.assertEqual(folder_list.find('folder').find('id').text, '/disk/folder2/folder_for_move/inner_folder')
        self.assertTrue(len(list(folder_list.find('folder').find('folder-list').iterchildren())) > 0)

        self.inspect_all()

    @pytest.mark.skipif(True, reason='https://st.yandex-team.ru/CHEMODAN-32396')
    def test_move_file_to_shared(self):
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid)
        self.activate_invite(uid=self.uid_3, hash=hsh)
        self.upload_file(self.uid_3, '/disk/file31')

        opts = {
            'uid': self.uid_3,
            'src': '/disk/file31',
            'dst': '/disk/folder2/file31',
            'force': 1,
        }
        open_url_data = set_up_open_url()
        self.mail_ok('move', opts)
        tear_down_open_url()

        vals = defaultdict(list)

        owner_found_new = False
        owner_new_version = False
        user_found_deleted = False
        user_found_new = False
        user_new_version = False
        for k, v in open_url_data.iteritems():
            if k.startswith('http://localhost/service/echo?uid='):
                notified_uid = re.search('http://localhost/service/echo\?uid=(\d+)', k).group(1)
                for each in v:
                    optag = etree.fromstring(each['pure_data'])
                    new_version = optag.get('new')
                    old_version = optag.get('old')
                    self.assertNotEqual(new_version, 'None')
                    self.assertNotEqual(old_version, 'None')
                    self.assertNotEqual(new_version, old_version, '%s != %s %s' % (new_version, old_version, each['pure_data']))
                    if optag.tag == 'diff':
                        vals[notified_uid].append(etree.tostring(optag, pretty_print=True))
                        if notified_uid == str(self.uid):
                            self.assertEqual(len(list(optag.iterfind('op'))), 1)
                            for op in optag.iterfind('op'):
                                if op.get('key') == '/disk/new_folder/folder2/file31':
                                    self.assertEqual(op.get('folder'), '/disk/new_folder/folder2/')
                                    self.assertEqual(op.get('type'), "new")
                                    self.assertNotEqual(op.get('resource_type'), None)
                                    owner_new_version = new_version
                                    owner_found_new = True
                                else:
                                    self.fail(etree.tostring(op))
                        elif notified_uid == str(self.uid_3):
                            user_new_version = new_version
                            self.assertEqual(len(list(optag.iterfind('op'))), 2)
                            for op in optag.iterfind('op'):
                                if op.get('key') == '/disk/folder2/file31':
                                    self.assertEqual(op.get('folder'), '/disk/folder2/')
                                    self.assertEqual(op.get('type'), "new")
                                    self.assertNotEqual(op.get('resource_type'), None)
                                    user_found_new = True
                                else:
                                    self.assertEqual(op.get('key'), '/disk/file31')
                                    self.assertEqual(op.get('folder'), '/disk/')
                                    self.assertEqual(op.get('type'), "deleted")
                                    self.assertNotEqual(op.get('resource_type'), None)
                                    user_found_deleted = True
                        else:
                            self.fail(etree.tostring(optag))

        self.assertTrue(owner_found_new)
        self.assertTrue(user_found_deleted)
        self.assertTrue(user_found_new)
        vals = dict(vals)
        self.assertTrue(vals)
        self.assertTrue(int(owner_new_version) != int(user_new_version))

        #=======================================================================
        # проверка того, что каждому пришел 1 push
        for k, v in vals.iteritems():
            self.assertEqual(len(v), 1, v)
        #=======================================================================
        opts = {'uid': self.uid_3, 'path': '/disk/file31'}
        self.mail_error('info', opts)
        opts = {'uid': self.uid_3, 'path': '/disk/folder2/file31'}
        self.mail_ok('info', opts)
        opts = {'uid': self.uid, 'path': '/disk/new_folder/folder2/file31'}
        file_info = self.mail_ok('info', opts)
        self.assertNotEqual(file_info.find('file').find('meta').find('group'), None)
        self.assertEqual(file_info.find('file').find('meta').find('with_shared'), None)
        user_version = int(db.user_index.find_one({'_id': str(self.uid_3)})['version'])
        owner_version = int(db.user_index.find_one({'_id': str(self.uid)})['version'])
        self.assertEqual(user_version, owner_version)
        self.inspect_all()

    def test_move_file_from_group(self):
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid)
        self.activate_invite(uid=self.uid_3, hash=hsh)
        self.upload_file(self.uid, '/disk/new_folder/folder2/file31')

        opts = {
            'uid': self.uid,
            'src': '/disk/new_folder/folder2/file31',
            'dst': '/disk/file61',
            'force': 1,
        }
        open_url_data = set_up_open_url()
        self.mail_ok('move', opts)
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
                            self.assertEqual(len(list(optag.iterfind('op'))), 2, etree.tostring(optag, pretty_print=True))
                            for op in optag.iterfind('op'):
                                if op.get('key') == '/disk/new_folder/folder2/file31':
                                    self.assertEqual(op.get('folder'), '/disk/new_folder/folder2/')
                                    self.assertEqual(op.get('type'), "deleted")
                                    self.assertNotEqual(op.get('resource_type'), None)
                                    owner_found_deleted = True
                                elif op.get('key') == '/disk/file61':
                                    self.assertEqual(op.get('folder'), '/disk/')
                                    self.assertEqual(op.get('type'), "new")
                                    self.assertNotEqual(op.get('resource_type'), None)
                                    owner_found_new = True
                                else:
                                    self.fail(etree.tostring(op))
                        elif notified_uid == str(self.uid_3):
                            self.assertEqual(len(list(optag.iterfind('op'))), 1)
                            for op in optag.iterfind('op'):
                                if op.get('key') == '/disk/folder2/file31':
                                    self.assertEqual(op.get('type'), "deleted")
                                    self.assertEqual(op.get('folder'), '/disk/folder2/')
                                    self.assertNotEqual(op.get('resource_type'), None)
                                    user_found_deleted = True
                                else:
                                    self.fail(etree.tostring(op))
                        else:
                            self.fail()

        self.assertTrue(owner_found_new)
        self.assertTrue(owner_found_deleted)
        self.assertTrue(user_found_deleted)
        vals = dict(vals)
        self.assertTrue(vals)

        #=======================================================================
        # проверка того, что каждому пришел 1 push
        for k, v in vals.iteritems():
            self.assertEqual(len(v), 1, v)
        self.inspect_all()

    def test_move_file_to_group(self):
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid)
        self.activate_invite(uid=self.uid_3, hash=hsh)
        self.upload_file(self.uid, '/disk/file61')

        opts = {
            'uid': self.uid,
            'src': '/disk/file61',
            'dst': '/disk/new_folder/folder2/file31',
            'force': 1,
            'connection_id': '12345',
        }
        with PushServicesStub() as push_service:
            self.mail_ok('move', opts)
            pushes = [PushServicesStub.parse_send_call(push_args)
                      for push_args in push_service.send.call_args_list]
        user_push = pushes[0]
        owner_push = pushes[1]

        assert owner_push['uid'] == self.uid
        op_values = [value['parameters']
                     for value in owner_push['json_payload']['values']
                     if value['tag'] == 'op']
        assert len(op_values) == 2
        assert owner_push['connection_id'] == '12345'
        assert owner_push['json_payload']['root']['tag'] == 'diff'

        owner_found_new = False
        owner_found_deleted = False
        for op in op_values:
            assert op['resource_type'] is not None
            if op.get('key') == '/disk/new_folder/folder2/file31':
                assert op['type'] == 'new'
                assert op['folder'] == '/disk/new_folder/folder2/'
                owner_found_new = True
            elif op.get('key') == '/disk/file61':
                assert op['type'] == 'deleted'
                assert op['folder'] == '/disk/'
                owner_found_deleted = True
        assert owner_found_new
        assert owner_found_deleted

        assert user_push['uid'] == self.uid_3
        op_values = [value['parameters']
                     for value in user_push['json_payload']['values']
                     if value['tag'] == 'op']
        assert len(op_values) == 1
        assert user_push['connection_id'] is None
        assert user_push['json_payload']['root']['tag'] == 'diff'
        assert user_push['json_payload']['root']['parameters']['committer_username'] is not None
        assert user_push['json_payload']['root']['parameters']['committer_email'] == self.email
        assert user_push['json_payload']['root']['parameters']['committer_uid'] == self.uid

        assert op_values[0]['key'] == '/disk/folder2/file31'
        assert op_values[0]['resource_type'] is not None
        assert op_values[0]['type'] == 'new'

        self.inspect_all()

    def test_rename_folder_in_shared(self):
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid)
        self.activate_invite(uid=self.uid_3, hash=hsh)
        self.json_ok('mkdir', {'uid': self.uid_3, 'path': '/disk/folder2/folder_for_move'})

        opts = {
            'uid': self.uid_3,
            'path': '/disk/folder2/folder_for_move',
        }
        info_result = self.mail_ok('info', opts)
        self.assertEqual(info_result.find('folder').find('meta').find('uid'), None)

        opts = {
            'uid': self.uid_3,
            'src': '/disk/folder2/folder_for_move',
            'dst': '/disk/folder2/folder_for_move_renamed',
        }
        self.mail_ok('async_move', opts)
        #=======================================================================
        #
        opts = {
            'uid': self.uid_3,
            'path': '/disk/folder2/folder_for_move_renamed',
            }
        info_result = self.mail_ok('info', opts)
        self.assertEqual(info_result.find('folder').find('meta').find('uid'), None)
        #=======================================================================
        #
        opts = {
            'uid': self.uid_3,
            'src': '/disk/folder2/folder_for_move_renamed',
            'dst': '/disk/folder2/folder_for_move',
        }
        self.mail_ok('async_move', opts)
        #=======================================================================
        #
        opts = {
            'uid': self.uid_3,
            'path': '/disk/folder2/folder_for_move',
        }
        info_result = self.mail_ok('info', opts)
        self.assertEqual(info_result.find('folder').find('meta').find('uid'), None)
        #=======================================================================
        #
        opts = {
            'uid': self.uid_3,
            'path': '/disk/folder2/folder_for_move',
        }
        self.mail_ok('async_trash_append', opts)
        #=======================================================================
        self.inspect_all()

    def test_rename_file_in_shared(self):
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid)
        self.activate_invite(uid=self.uid_3, hash=hsh)
        self.upload_file(self.uid_3, '/disk/folder2/file31')

        opts = {
            'uid': self.uid_3,
            'src': '/disk/folder2/file31',
            'dst': '/disk/folder2/file51',
            'force': 1,
        }
        open_url_data = set_up_open_url()
        self.mail_ok('move', opts)
        tear_down_open_url()
        vals = defaultdict(list)

        owner_found_deleted = False
        owner_found_new = False
        user_found_deleted = False
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
                                    self.assertEqual(op.get('type'), "deleted")
                                    self.assertNotEqual(op.get('resource_type'), None)
                                    owner_found_deleted = True
                                else:
                                    self.assertEqual(op.get('key'), '/disk/new_folder/folder2/file51')
                                    self.assertEqual(op.get('folder'), '/disk/new_folder/folder2/')
                                    self.assertEqual(op.get('type'), "new")
                                    self.assertNotEqual(op.get('resource_type'), None)
                                    owner_found_new = True
                        elif notified_uid == str(self.uid_3):
                            self.assertEqual(len(list(optag.iterfind('op'))), 2)
                            for op in optag.iterfind('op'):
                                if op.get('key') == '/disk/folder2/file31':
                                    self.assertEqual(op.get('folder'), '/disk/folder2/')
                                    self.assertEqual(op.get('type'), "deleted")
                                    self.assertNotEqual(op.get('resource_type'), None)
                                    user_found_deleted = True
                                else:
                                    self.assertEqual(op.get('key'), '/disk/folder2/file51')
                                    self.assertEqual(op.get('folder'), '/disk/folder2/')
                                    self.assertEqual(op.get('type'), "new")
                                    self.assertNotEqual(op.get('resource_type'), None)
                                    user_found_new = True
                        else:
                            self.fail()

        self.assertTrue(owner_found_deleted)
        self.assertTrue(owner_found_new)
        self.assertTrue(user_found_deleted)
        self.assertTrue(user_found_new)
        vals = dict(vals)
        self.assertTrue(vals)
        for k, v in vals.iteritems():
            self.assertEqual(len(v), 1, v)

        opts = {
            'uid': self.uid,
            'src': '/disk/new_folder/folder2/file51',
            'dst': '/disk/new_folder/folder2/file31',
            'force': 1,
        }
        open_url_data = set_up_open_url()
        self.mail_ok('move', opts)
        tear_down_open_url()
        vals = defaultdict(list)
        owner_found_deleted = False
        owner_found_new = False
        user_found_deleted = False
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
                                self.assertNotEqual(op.get('resource_type'), None)
                                if op.get('key') == '/disk/new_folder/folder2/file51':
                                    self.assertEqual(op.get('folder'), '/disk/new_folder/folder2/')
                                    self.assertEqual(op.get('type'), "deleted")
                                    owner_found_deleted = True
                                elif op.get('key') == '/disk/new_folder/folder2/file31':
                                    self.assertEqual(op.get('folder'), '/disk/new_folder/folder2/')
                                    self.assertEqual(op.get('type'), "new")
                                    owner_found_new = True
                                else:
                                    self.fail(etree.tostring(op, pretty_print=True))
                        elif notified_uid == str(self.uid_3):
                            self.assertEqual(len(list(optag.iterfind('op'))), 2)
                            for op in optag.iterfind('op'):
                                self.assertNotEqual(op.get('resource_type'), None)
                                if op.get('key') == '/disk/folder2/file51':
                                    self.assertEqual(op.get('folder'), '/disk/folder2/')
                                    self.assertEqual(op.get('type'), "deleted")
                                    user_found_deleted = True
                                elif op.get('key') == '/disk/folder2/file31':
                                    self.assertEqual(op.get('folder'), '/disk/folder2/')
                                    self.assertEqual(op.get('type'), "new")
                                    user_found_new = True
                                else:
                                    self.fail(etree.tostring(op, pretty_print=True))
                        else:
                            self.fail()

        self.assertTrue(owner_found_deleted)
        self.assertTrue(owner_found_new)
        self.assertTrue(user_found_deleted)
        self.assertTrue(user_found_new)
        vals = dict(vals)
        self.assertTrue(vals)
        for k, v in vals.iteritems():
            self.assertEqual(len(v), 1, v)
        self.inspect_all()

    @pytest.mark.skipif(not INIT_USER_IN_POSTGRES, reason='Tests designed to work in postgres')
    def test_move_folder_with_files_without_source(self):
        folder_path = '/disk/common-folder-old'
        file_path = folder_path + '/test.txt'
        folder_new_path = '/disk/common-folder-new'
        new_file_path = folder_new_path + '/test.txt'

        self.json_ok('mkdir', {'uid': self.uid, 'path': folder_path})

        gid = self.create_group(uid=self.uid, path=folder_path)
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid, path=folder_path)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        self.upload_file(self.uid, file_path)
        session = Session.create_from_uid(self.uid)
        session.execute('UPDATE disk.files SET source = null WHERE uid=:uid', {'uid': int(self.uid)})

        self.json_ok('async_move', {'uid': self.uid, 'src': folder_path, 'dst': folder_new_path})
        file_info = self.json_ok('info', {'uid': self.uid, 'path': new_file_path})

        assert file_info['id'] == new_file_path

    @parameterized.expand([[True], [False]])
    def test_move_folder_from_private_to_shared(self, overwrite):
        OWNER = self.uid_1
        INVITED = self.uid_3
        self.json_ok('mkdir', {'uid': OWNER, 'path': '/disk/shared'})
        if overwrite:
            self.json_ok('mkdir', {'uid': OWNER, 'path': '/disk/shared/subfolder'})
        gid = self.create_group(OWNER, '/disk/shared')
        hsh = self.invite_user(uid=INVITED, owner=OWNER, email=self.email_3, ext_gid=gid, path='/disk/shared')
        self.activate_invite(uid=INVITED, hash=hsh)
        self.json_ok('move', {'uid': INVITED, 'src': '/disk/shared', 'dst': '/disk/shared2'})
        self.json_ok('mkdir', {'uid': INVITED, 'path': '/disk/another'})
        self.upload_file(INVITED, '/disk/another/file.txt')
        opts = {'uid': INVITED, 'src': '/disk/another', 'dst': '/disk/shared2/subfolder'}
        if overwrite:
            opts['force'] = '1'
        res = self.json_ok('async_move', opts)
        oid = res['oid']
        res = self.json_ok('status', {'uid': INVITED, 'oid': oid})
        assert 'DONE' == res['status'], res
        assert '/disk/shared/subfolder/file.txt' == self.json_ok('info', {'uid': OWNER, 'path': '/disk/shared/subfolder/file.txt'})['path']
        assert '/disk/shared2/subfolder/file.txt' == self.json_ok('info', {'uid': INVITED, 'path': '/disk/shared2/subfolder/file.txt'})['path']

    def test_locks_on_move_shared_root(self):
        OWNER = self.uid_1
        src_shared_root = '/disk/shared'
        dst_shared_root = '/disk/shared 1989'
        self.json_ok('mkdir', {'uid': OWNER, 'path': src_shared_root})
        gid = self.create_group(OWNER, src_shared_root)
        with mock.patch('mpfs.core.filesystem.helpers.lock.fs_locks.set', side_effect=fs_locks.set) as mocked_lock:
            self.json_ok('move', {'uid': OWNER,
                                  'src': '/disk/shared',
                                  'dst': dst_shared_root})

        paths = [args[0][1] for args in mocked_lock.call_args_list]

        assert_that(paths, has_item(src_shared_root), 'source not locked')
        assert_that(paths, has_item(dst_shared_root), 'destination not locked')
