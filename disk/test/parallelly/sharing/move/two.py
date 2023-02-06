# -*- coding: utf-8 -*-
import re

from lxml import etree

from test.common.sharing import CommonSharingMethods
from test.base_suit import set_up_open_url, tear_down_open_url

import mpfs.engine.process
from test.helpers.stubs.services import PushServicesStub
from mpfs.core.filesystem.dao.legacy import CollectionRoutedDatabase


db = CollectionRoutedDatabase()


class MoveTwoSharingTestCase(CommonSharingMethods):

    def setup_method(self, method):
        super(MoveTwoSharingTestCase, self).setup_method(method)
        self.json_ok('user_init', {'uid': self.uid_1})
        self.json_ok('user_init', {'uid': self.uid_3})
        self.make_dirs()

    def test_move_from_group_to_group_variant_one(self):
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        self.upload_file(self.uid_3, '/disk/folder2/file31', connection_id='12345')
        #=======================================================================
        # create new group and invite
        opts = {'uid': self.uid_1, 'path': '/disk/folder)(6)*)'}
        self.json_ok('mkdir', opts)
        args = {
            'uid': self.uid_1,
            'path': '/disk/folder)(6)*)',
            'rights': 660,
            'universe_login': self.email_3,
            'universe_service': 'email',
            'avatar': 'http://localhost/echo',
            'name': 'mpfs-test-3',
            'connection_id': '12345',
        }
        result = self.mail_ok('share_invite_user', args)
        # self.fail(etree.tostring(result, pretty_print=True))
        hsh = None
        for each in result.getchildren():
            if each.tag == 'hash' and each.text and isinstance(each.text, str):
                hsh = each.text
        self.assertNotEqual(hsh, None)
        self.assertNotEqual(db.group_invites.find_one({'_id': hsh}), None)
        args = {
            'hash': hsh,
            'uid': self.uid_3,
        }
        self.mail_ok('share_activate_invite', args)
        tear_down_open_url()
        #=======================================================================

        opts = {
            'uid': self.uid_3,
            'src': '/disk/folder2/file31',
            'dst': '/disk/folder)(6)*)/file61',
            'force': 1,
            'connection_id': '12345',
        }
        with PushServicesStub() as push_service:
            self.mail_ok('move', opts)
            pushes = [PushServicesStub.parse_send_call(push_args)
                      for push_args in push_service.send.call_args_list]
        user_push = pushes[0]
        old_owner_push = pushes[1]
        owner_push = pushes[2]

        assert owner_push['uid'] == self.uid_1
        op_values = [value['parameters']
                     for value in owner_push['json_payload']['values']
                     if value['tag'] == 'op']
        assert len(op_values) == 1
        assert owner_push['connection_id'] is None
        assert owner_push['json_payload']['root']['tag'] == 'diff'
        assert owner_push['json_payload']['root']['parameters']['committer_username'] is not None
        assert owner_push['json_payload']['root']['parameters']['committer_email'] == self.email_3
        assert owner_push['json_payload']['root']['parameters']['committer_uid'] == self.uid_3

        assert op_values[0]['key'] == '/disk/folder)(6)*)/file61'
        assert op_values[0]['type'] == 'new'

        assert user_push['uid'] == self.uid_3
        op_values = [value['parameters']
                     for value in user_push['json_payload']['values']
                     if value['tag'] == 'op']
        assert len(op_values) == 2
        assert user_push['connection_id'] == '12345'
        assert user_push['json_payload']['root']['tag'] == 'diff'

        user_found_deleted = False
        user_found_new = False
        for op in op_values:
            assert op['resource_type'] is not None
            if op.get('key') == '/disk/folder2/file31':
                assert op['type'] == 'deleted'
                assert op['folder'] == '/disk/folder2/'
                user_found_deleted = True
            elif op.get('key') == '/disk/folder)(6)*)/file61':
                assert op['type'] == 'new'
                assert op['folder'] == '/disk/folder)(6)*)/'
                user_found_new = True
        assert user_found_new
        assert user_found_deleted

        assert old_owner_push['uid'] == self.uid
        op_values = [value['parameters']
                     for value in old_owner_push['json_payload']['values']
                     if value['tag'] == 'op']
        assert len(op_values) == 1
        assert old_owner_push['connection_id'] is None
        assert old_owner_push['json_payload']['root']['parameters']['committer_username'] is not None
        assert old_owner_push['json_payload']['root']['parameters']['committer_email'] == self.email_3
        assert old_owner_push['json_payload']['root']['parameters']['committer_uid'] == self.uid_3
        assert old_owner_push['json_payload']['root']['tag'] == 'diff'

        assert op_values[0]['key'] == '/disk/new_folder/folder2/file31'
        assert op_values[0]['type'] == 'deleted'

        self.inspect_all()

    def test_move_from_group_to_group_variant_two(self):
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        opts = {'uid': self.uid_1, 'path': '/disk/folder)(6)*)'}
        self.json_ok('mkdir', opts)

        gid_1 = self.create_group(uid=self.uid_1, path='/disk/folder)(6)*)')
        hsh = self.invite_user(uid=self.uid_3, owner=self.uid_1, email=self.email_3, ext_gid=gid_1, path='/disk/folder)(6)*)/')
        self.activate_invite(uid=self.uid_3, hash=hsh)

        self.upload_file(self.uid_3, '/disk/folder)(6)*)/file61', connection_id='12345')

        opts = {
            'uid': self.uid_3,
            'src': '/disk/folder)(6)*)/file61',
            'dst': '/disk/folder2/file31',
            'force': 1,
            'connection_id': '12345',
        }

        with PushServicesStub() as push_service:
            self.mail_ok('move', opts)
            pushes = [PushServicesStub.parse_send_call(push_args)
                      for push_args in push_service.send.call_args_list]
        user_push = pushes[0]
        owner_push = pushes[1]
        old_owner_push = pushes[2]

        assert owner_push['uid'] == self.uid_1
        op_values = [value['parameters']
                     for value in owner_push['json_payload']['values']
                     if value['tag'] == 'op']
        assert len(op_values) == 1
        assert owner_push['connection_id'] is None
        assert owner_push['json_payload']['root']['tag'] == 'diff'
        assert owner_push['json_payload']['root']['parameters']['committer_username'] is not None
        assert owner_push['json_payload']['root']['parameters']['committer_email'] == self.email_3
        assert owner_push['json_payload']['root']['parameters']['committer_uid'] == self.uid_3

        assert op_values[0]['key'] == '/disk/folder)(6)*)/file61'
        assert op_values[0]['type'] == 'deleted'

        assert user_push['uid'] == self.uid_3
        op_values = [value['parameters']
                     for value in user_push['json_payload']['values']
                     if value['tag'] == 'op']
        assert len(op_values) == 2
        assert user_push['connection_id'] == '12345'
        assert user_push['json_payload']['root']['tag'] == 'diff'

        user_found_deleted = False
        user_found_new = False
        for op in op_values:
            assert op['resource_type'] is not None
            if op.get('key') == '/disk/folder2/file31':
                assert op['type'] == 'new'
                assert op['folder'] == '/disk/folder2/'
                user_found_deleted = True
            elif op.get('key') == '/disk/folder)(6)*)/file61':
                assert op['type'] == 'deleted'
                assert op['folder'] == '/disk/folder)(6)*)/'
                user_found_new = True
        assert user_found_new
        assert user_found_deleted

        assert old_owner_push['uid'] == self.uid
        op_values = [value['parameters']
                     for value in old_owner_push['json_payload']['values']
                     if value['tag'] == 'op']
        assert len(op_values) == 1
        assert old_owner_push['connection_id'] is None
        assert old_owner_push['json_payload']['root']['parameters']['committer_username'] is not None
        assert old_owner_push['json_payload']['root']['parameters']['committer_email'] == self.email_3
        assert old_owner_push['json_payload']['root']['parameters']['committer_uid'] == self.uid_3
        assert old_owner_push['json_payload']['root']['tag'] == 'diff'

        assert op_values[0]['key'] == '/disk/new_folder/folder2/file31'
        assert op_values[0]['type'] == 'new'

        self.inspect_all()

    def test_move_shared_folder_to_folder(self):
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        opts = {'uid': self.uid_1, 'path': '/disk/folder)(6)*)'}
        self.json_ok('mkdir', opts)

        gid_1 = self.create_group(uid=self.uid_1, path='/disk/folder)(6)*)')
        hsh = self.invite_user(uid=self.uid_3, owner=self.uid_1, email=self.email_3, ext_gid=gid_1, path='/disk/folder)(6)*)/')
        self.activate_invite(uid=self.uid_3, hash=hsh)

        opts = {'uid': self.uid_3, 'path': '/disk/folder)(6)*)/folder71'}
        self.json_ok('mkdir', opts)
        self.upload_file(self.uid_3, '/disk/folder)(6)*)/file71')

        child_file_found = False
        child_folder_found = False
        opts = {'uid': self.uid_3, 'path': '/disk/folder)(6)*)'}
        listing_result = self.mail_ok('list', opts)
        folder = listing_result.find('folder-list').find('folder')
        self.assertNotEqual(folder.find('id'), None)
        self.assertEqual(folder.find('id').text, '/disk/folder)(6)*)')
        self.assertNotEqual(folder.find('meta'), None)
        self.assertNotEqual(folder.find('meta').find('group'), None)
        self.assertEqual(folder.find('meta').find('group').find('owner').find('uid').text, str(self.uid_1))
        for child in folder.find('folder-list').iterchildren():
            if child.tag == 'file' and child.find('id').text == '/disk/folder)(6)*)/file71':
                child_file_found = True
            elif child.tag == 'folder' and child.find('id').text == '/disk/folder)(6)*)/folder71':
                child_folder_found = True
            else:
                self.fail(etree.tostring(child, pretty_print=True))
        self.assertTrue(child_file_found)
        self.assertTrue(child_folder_found)
        opts = {
            'uid': self.uid_3,
            'path': '/disk/folder8',
            'connection_id': '12345',
        }
        self.json_ok('mkdir', opts)

        opts = {
            'uid': self.uid_3,
            'path': '/disk/folder8/folder9',
            'connection_id': '12345',
        }
        self.json_ok('mkdir', opts)

        opts = {
            'uid': self.uid_3,
            'path': '/disk',
        }
        version_old = self.json_ok('diff', opts)['version']
        # =======================================================================
        #
        opts = {
            'uid': self.uid_3,
            'src': '/disk/folder)(6)*)',
            'dst': '/disk/folder8/folder9/folder)(6)*)',
            'connection_id': '12345',
        }
        open_url_data = set_up_open_url()
        self.mail_ok('async_move', opts)
        tear_down_open_url()

        notify_sent_user_new = False
        notify_sent_user_deleted = False
        notify_sent_user_new_internal = False
        notify_sent_user_new_internal_file = False
        for k, v in open_url_data.iteritems():
            if k.startswith('http://localhost/service/echo?uid='):
                #                self.fail(v)
                notified_uid = re.search('http://localhost/service/echo\?uid=(\d+)', k).group(1)
                if notified_uid == str(self.uid_3):
                    for each in v:
                        share_tag = etree.fromstring(each['pure_data'])
                        if share_tag.tag == 'diff':
                            self.assertEqual(len(list(share_tag.iterfind('op'))), 4)
                            for op in share_tag.iterfind('op'):
                                self.assertNotEqual(op.get('resource_type'), None)
                                if op.get('type') == 'deleted':
                                    self.assertEqual(op.get('folder'), '/disk/')
                                    self.assertEqual(op.get('key'), '/disk/folder)(6)*)')
                                    notify_sent_user_deleted = True
                                elif op.get('type') == 'new':
                                    if op.get('key') == '/disk/folder8/folder9/folder)(6)*)':
                                        self.assertEqual(op.get('folder'), '/disk/folder8/folder9/')
                                        notify_sent_user_new = True
                                    elif op.get('key').startswith('/disk/folder8/folder9/folder)(6)*)/folder9'):
                                        self.fail(op.get('key'))
                                    elif op.get('key').startswith('/disk/folder8/folder9/folder)(6)*)/'):
                                        if op.get('key') == '/disk/folder8/folder9/folder)(6)*)/file71':
                                            self.assertNotEqual(op.get('md5'), None)
                                            self.assertNotEqual(op.get('sha256'), None)
                                            self.assertNotEqual(op.get('size'), None)
                                            notify_sent_user_new_internal_file = True
                                        notify_sent_user_new_internal = True
                                    else:
                                        self.fail(op.get('key'))
                                else:
                                    self.fail()
        self.assertTrue(notify_sent_user_new)
        self.assertTrue(notify_sent_user_new_internal)
        self.assertTrue(notify_sent_user_deleted)
        self.assertTrue(notify_sent_user_new_internal_file)

        self.assertEqual(db.group_links.find_one({'uid': str(self.uid_3), 'path': '/disk/folder)(6)*)'}), None)
        group_link_data = db.group_links.find_one(
            {'uid': str(self.uid_3), 'path': '/disk/folder8/folder9/folder)(6)*)'})
        self.assertNotEqual(group_link_data, None)
        self.assertEqual(db.user_data.find_one({'uid': str(self.uid_3), 'key': "/disk/folder)(6)*)"}), None)
        self.assertNotEqual(
            db.user_data.find_one({'uid': str(self.uid_3), 'key': '/disk/folder8/folder9/folder)(6)*)'}), None)
        self.assertEqual(db.user_data.find_one({'uid': str(self.uid_3),
                                                'key': "/disk/folder8/folder9/folder)(6)*)/file71"}), None)
        gid = group_link_data['gid']
        opts = {
            'uid': self.uid_3,
            'path': '/disk',
            'version': version_old,
            'meta': '',
        }
        diff_after = self.desktop('diff', opts)['result']

        for each in diff_after:
            self.assertNotEqual(op.get('resource_type'), None)
            if each['op'] == 'deleted':
                self.assertEqual(each['key'], '/disk/folder)(6)*)')
            elif each['op'] == 'new':
                if each['key'].startswith('/disk/folder8/folder9/folder)(6)*)'):
                    if each['key'] == '/disk/folder8/folder9/folder)(6)*)':
                        self.assertNotEqual(each.get('rights'), None)
                        self.assertNotEqual(each.get('shared'), None)
                    self.assertTrue(each['key'].startswith('/disk/folder8/folder9/folder)(6)*)'))
                    self.assertEqual(each['gid'], gid)
                    self.assertEqual(each['group_path'], '/disk/folder8/folder9/folder)(6)*)')
                else:
                    self.fail(each)
            elif each['op'] == 'changed':
                self.assertEqual(each['key'], '/disk/folder8/folder9')
            else:
                self.fail(each)

        child_file_found = False
        child_folder_found = False
        opts = {'uid': self.uid_3, 'path': '/disk/folder8/folder9/folder)(6)*)'}
        listing_result = self.mail_ok('list', opts)
        folder = listing_result.find('folder-list').find('folder')
        self.assertNotEqual(folder.find('id'), None)
        self.assertEqual(folder.find('id').text, '/disk/folder8/folder9/folder)(6)*)')
        self.assertNotEqual(folder.find('meta'), None)
        self.assertNotEqual(folder.find('meta').find('group'), None)
        self.assertEqual(folder.find('meta').find('group').find('owner').find('uid').text, str(self.uid_1))
        gid = folder.find('meta').find('group').find('gid').text
        self.assertNotEqual(gid, '')
        for child in folder.find('folder-list').iterchildren():
            if child.tag == 'file' and child.find('id').text == '/disk/folder8/folder9/folder)(6)*)/file71':
                child_file_found = True
            elif child.tag == 'folder' and child.find('id').text == '/disk/folder8/folder9/folder)(6)*)/folder71':
                child_folder_found = True
            else:
                self.fail(etree.tostring(child, pretty_print=True))
        self.assertTrue(child_file_found)
        self.assertTrue(child_folder_found)

        self.assertNotEqual(db.group_links.find_one({'gid': gid}), None)

        # move parent
        opts = {
            'uid': self.uid_3,
            'path': '/disk/folder8/folder9/folder11',
        }
        self.json_ok('mkdir', opts)
        opts = {
            'uid': self.uid_3,
            'path': '/disk/folder8/folder9/folder11/folder12',
        }
        self.json_ok('mkdir', opts)
        self.upload_file(self.uid_3, '/disk/folder8/folder9/folder11/file12')
        self.upload_file(self.uid_3, '/disk/folder8/folder9/folder11/folder12/file13')
        opts = {
            'uid': self.uid_3,
            'src': '/disk/folder8/folder9',
            'dst': '/disk/folder9',
            'connection_id': '12345',
        }
        open_url_data = set_up_open_url()
        self.mail_ok('async_move', opts)
        tear_down_open_url()
        notify_sent_user_new = False
        notify_sent_user_deleted = False
        notify_sent_user_new_internal = False
        notify_sent_user_new_internal_file = False
        notify_sent_user_new_parent = False
        for k, v in open_url_data.iteritems():
            if k.startswith('http://localhost/service/echo?uid='):
                notified_uid = re.search('http://localhost/service/echo\?uid=(\d+)', k).group(1)
                if notified_uid == str(self.uid_3):
                    for each in v:
                        share_tag = etree.fromstring(each['pure_data'])
                        if share_tag.tag == 'diff':
                            self.assertEqual(len(list(share_tag.iterfind('op'))), 9)
                            for op in share_tag.iterfind('op'):
                                self.assertNotEqual(op.get('resource_type'), None)
                                if op.get('type') == 'deleted':
                                    self.assertEqual(op.get('folder'), '/disk/folder8/')
                                    self.assertEqual(op.get('key'), '/disk/folder8/folder9')
                                    notify_sent_user_deleted = True
                                elif op.get('type') == 'new':
                                    if op.get('key') == '/disk/folder9':
                                        self.assertEqual(op.get('folder'), '/disk/')
                                        notify_sent_user_new_parent = True
                                    elif op.get('key') == '/disk/folder9/folder)(6)*)':
                                        self.assertEqual(op.get('folder'), '/disk/folder9/')
                                        notify_sent_user_new = True
                                    elif op.get('key').startswith('/disk/folder9/folder)(6)*)/folder9'):
                                        self.fail(op.get('key'))
                                    elif op.get('key').startswith('/disk/folder9/'):
                                        if op.get('key') == '/disk/folder9/folder)(6)*)/file71':
                                            self.assertNotEqual(op.get('md5'), None)
                                            self.assertNotEqual(op.get('sha256'), None)
                                            self.assertNotEqual(op.get('size'), None)
                                            notify_sent_user_new_internal_file = True
                                        else:
                                            notify_sent_user_new_internal = True
                                    else:
                                        self.fail(op.get('key'))
                                else:
                                    self.fail()
        self.assertTrue(notify_sent_user_new)
        self.assertTrue(notify_sent_user_new_internal)
        self.assertTrue(notify_sent_user_deleted)
        self.assertTrue(notify_sent_user_new_internal_file)
        self.assertTrue(notify_sent_user_new_parent)
        self.assertNotEqual(db.group_links.find_one({'gid': gid}), None)
        self.assertNotEqual(db.group_links.find_one({'uid': str(self.uid_3), 'path': '/disk/folder9/folder)(6)*)'}),
                            None)
        self.assertEqual(
            db.group_links.find_one({'uid': str(self.uid_3), 'path': '/disk/folder8/folder9/folder)(6)*)'}), None)
        self.assertEqual(db.user_data.find_one(
            {'uid': str(self.uid_3), 'key': "'/disk/folder8/folder9/folder)(6)*)'"}), None)
        self.assertNotEqual(db.user_data.find_one({'uid': str(self.uid_3), 'key': '/disk/folder9/folder)(6)*)'}), None)
        self.assertEqual(db.user_data.find_one({'uid': str(self.uid_3),
                                                'key': "'/disk/folder9/folder)(6)*)/file71'"}), None)

        child_file_found = False
        child_folder_found = False
        opts = {'uid': self.uid_3, 'path': '/disk/folder9/folder)(6)*)'}
        listing_result = self.mail_ok('list', opts)
        folder = listing_result.find('folder-list').find('folder')
        self.assertNotEqual(folder.find('id'), None)
        self.assertEqual(folder.find('id').text, '/disk/folder9/folder)(6)*)')
        self.assertNotEqual(folder.find('meta'), None)
        self.assertNotEqual(folder.find('meta').find('group'), None)
        self.assertEqual(folder.find('meta').find('group').find('owner').find('uid').text, str(self.uid_1))
        for child in folder.find('folder-list').iterchildren():
            if child.tag == 'file' and child.find('id').text == '/disk/folder9/folder)(6)*)/file71':
                child_file_found = True
            elif child.tag == 'folder' and child.find('id').text == '/disk/folder9/folder)(6)*)/folder71':
                child_folder_found = True
            else:
                self.fail(etree.tostring(child, pretty_print=True))
        self.assertTrue(child_file_found)
        self.assertTrue(child_folder_found)

        self.inspect_all()

    def test_move_shared_root_to_shared(self):
        """
        https://jira.yandex-team.ru/browse/CHEMODAN-7425
        """
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        opts = {'uid': self.uid_1, 'path': '/disk/folder)(6)*)'}
        self.json_ok('mkdir', opts)
        gid_1 = self.create_group(uid=self.uid_1, path='/disk/folder)(6)*)')
        hsh = self.invite_user(uid=self.uid_3, owner=self.uid_1, email=self.email_3, ext_gid=gid_1, path='/disk/folder)(6)*)/')
        self.activate_invite(uid=self.uid_3, hash=hsh)

        opts = {'uid': self.uid_3, 'path': '/disk/folder9'}
        self.json_ok('mkdir', opts)
        opts = {
            'uid': self.uid_3,
            'src': '/disk/folder)(6)*)',
            'dst': '/disk/folder9/folder)(6)*)',
            'connection_id': '12345',
        }
        self.mail_ok('async_move', opts)

        opts = {
            'uid': self.uid_3,
            'src': '/disk/folder9/folder)(6)*)',
            'dst': '/disk/folder2/folder)(6)*)',
            'connection_id': '12345',
            'force': 1,
        }
        self.mail_error('move', opts, code=126)
        opts = {
            'uid': self.uid_3,
            'src': '/disk/folder9/folder)(6)*)',
            'dst': '/disk/folder2/folder)(6)*)',
            'connection_id': '12345',
            'force': 1,
        }
        self.mail_error('async_move', opts, code=126)
        opts = {
            'uid': self.uid_3,
            'src': '/disk/folder9',
            'dst': '/disk/folder2/folder9',
            'connection_id': '12345',
            'force': 1,
        }
        self.mail_error('move', opts, code=126)
        opts = {
            'uid': self.uid_3,
            'src': '/disk/folder9',
            'dst': '/disk/folder2/folder9',
            'connection_id': '12345',
            'force': 1,
        }
        self.mail_error('async_move', opts, code=126)
        self.inspect_all()

    def test_move_group_root_to_group(self):
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        opts = {'uid': self.uid_1, 'path': '/disk/folder)(6)*)'}
        self.json_ok('mkdir', opts)
        gid_1 = self.create_group(uid=self.uid_1, path='/disk/folder)(6)*)')
        hsh = self.invite_user(uid=self.uid_3, owner=self.uid_1, email=self.email_3, ext_gid=gid_1, path='/disk/folder)(6)*)/')
        self.activate_invite(uid=self.uid_3, hash=hsh)

        opts = {'uid': self.uid_3, 'path': '/disk/folder9'}
        self.json_ok('mkdir', opts)
        opts = {
            'uid': self.uid_3,
            'src': '/disk/folder)(6)*)',
            'dst': '/disk/folder9/folder)(6)*)',
            'connection_id': '12345',
        }
        self.mail_ok('async_move', opts)

        opts = {'uid': self.uid, 'path': '/disk/new_folder/folder3/'}
        self.json_ok('mkdir', opts)
        gid = self.create_group(path='/disk/new_folder/folder3')

        opts = {
            'uid': self.uid,
            'src': '/disk/new_folder/folder2',
            'dst': '/disk/new_folder/folder3/folder2',
            'connection_id': '12345',
            'force': 1,
        }
        self.mail_error('move', opts, code=126)

        opts = {
            'uid': self.uid,
            'src': '/disk/new_folder/folder2',
            'dst': '/disk/new_folder/folder3/folder2',
            'connection_id': '12345',
            'force': 1,
        }
        self.mail_error('async_move', opts, code=126)

        # =======================================================================
        # make group folder in /disk
        opts = {
            'uid': self.uid,
            'src': '/disk/new_folder/folder3',
            'dst': '/disk/folder3',
            'connection_id': '12345',
            'force': 1,
        }
        self.mail_ok('async_move', opts)
        #=======================================================================

        opts = {
            'uid': self.uid,
            'src': '/disk/new_folder/folder2',
            'dst': '/disk/folder3/folder2',
            'connection_id': '12345',
            'force': 1,
        }
        self.mail_error('move', opts, code=126)
        opts = {
            'uid': self.uid,
            'src': '/disk/new_folder/folder2',
            'dst': '/disk/folder3/folder2',
            'connection_id': '12345',
            'force': 1,
        }
        self.mail_error('async_move', opts, code=126)
        #=======================================================================
        # move group folder back to new_folder
        opts = {
            'uid': self.uid,
            'src': '/disk/folder3',
            'dst': '/disk/new_folder/folder3',
            'connection_id': '12345',
            'force': 1,
        }
        self.mail_ok('async_move', opts)
        #=======================================================================
        self.inspect_all()

    def test_replace_shared_folder(self):
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        opts = {'uid': self.uid_1, 'path': '/disk/folder)(6)*)'}
        self.json_ok('mkdir', opts)
        gid_1 = self.create_group(uid=self.uid_1, path='/disk/folder)(6)*)')
        hsh = self.invite_user(uid=self.uid_3, owner=self.uid_1, email=self.email_3, ext_gid=gid_1, path='/disk/folder)(6)*)/')
        self.activate_invite(uid=self.uid_3, hash=hsh)

        opts = {'uid': self.uid_3, 'path': '/disk/folder9'}
        self.json_ok('mkdir', opts)
        opts = {
            'uid': self.uid_3,
            'src': '/disk/folder)(6)*)',
            'dst': '/disk/folder9/folder)(6)*)',
            'connection_id': '12345',
        }
        self.mail_ok('async_move', opts)

        opts = {
            'uid': self.uid_3,
            'path': '/disk/simple_folder',
        }
        self.json_ok('mkdir', opts)
        self.upload_file(self.uid_3, '/disk/simple_folder/simple_file')
        opts = {
            'uid': self.uid_3,
            'path': '/disk/simple_folder/internal_folder',
        }
        self.json_ok('mkdir', opts)
        self.upload_file(self.uid_3, '/disk/simple_folder/internal_folder/internal_file')
        #=======================================================================
        opts = {
            'uid': self.uid_3,
            'src': '/disk/simple_folder',
            'dst': '/disk/folder2',
            'connection_id': '12345',
            'force': 1,
        }
        self.mail_error('move', opts, code=126)
        opts = {
            'uid': self.uid_3,
            'src': '/disk/simple_folder',
            'dst': '/disk/folder2',
            'connection_id': '12345',
            'force': 1,
        }
        self.mail_error('async_move', opts, code=126)
        #=======================================================================
        #
        opts = {
            'uid': self.uid_3,
            'src': '/disk/simple_folder',
            'dst': '/disk/folder9',
            'connection_id': '12345',
            'force': 1,
        }
        self.mail_error('move', opts, code=126)
        opts = {
            'uid': self.uid_3,
            'src': '/disk/simple_folder',
            'dst': '/disk/folder9',
            'connection_id': '12345',
            'force': 1,
        }
        self.mail_error('async_move', opts, code=126)
        opts = {
            'uid': self.uid_3,
            'path': '/disk/simple_folder',
        }
        self.mail_ok('rm', opts)
        self.inspect_all()

    def test_replace_group_folder(self):
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        opts = {'uid': self.uid_1, 'path': '/disk/folder)(6)*)'}
        self.json_ok('mkdir', opts)
        gid_1 = self.create_group(uid=self.uid_1, path='/disk/folder)(6)*)')
        hsh = self.invite_user(uid=self.uid_3, owner=self.uid_1, email=self.email_3, ext_gid=gid_1, path='/disk/folder)(6)*)/')
        self.activate_invite(uid=self.uid_3, hash=hsh)

        opts = {'uid': self.uid_3, 'path': '/disk/folder9'}
        self.json_ok('mkdir', opts)
        opts = {
            'uid': self.uid_3,
            'src': '/disk/folder)(6)*)',
            'dst': '/disk/folder9/folder)(6)*)',
            'connection_id': '12345',
        }
        self.mail_ok('async_move', opts)

        opts = {
            'uid': self.uid,
            'path': '/disk/simple_folder',
        }
        self.json_ok('mkdir', opts)
        self.upload_file(self.uid, '/disk/simple_folder/simple_file')
        opts = {
            'uid': self.uid,
            'path': '/disk/simple_folder/internal_folder',
        }
        self.json_ok('mkdir', opts)
        self.upload_file(self.uid, '/disk/simple_folder/internal_folder/internal_file')
        # =======================================================================
        opts = {
            'uid': self.uid,
            'src': '/disk/simple_folder',
            'dst': '/disk/new_folder/folder2',
            'connection_id': '12345',
            'force': 1,
        }
        self.mail_error('move', opts, code=126)
        opts = {
            'uid': self.uid,
            'src': '/disk/simple_folder',
            'dst': '/disk/new_folder/folder2',
            'connection_id': '12345',
            'force': 1,
        }
        self.mail_error('async_move', opts, code=126)
        #=======================================================================
        #
        opts = {
            'uid': self.uid,
            'src': '/disk/simple_folder',
            'dst': '/disk/new_folder',
            'connection_id': '12345',
            'force': 1,
        }
        self.mail_error('move', opts, code=126)
        opts = {
            'uid': self.uid,
            'src': '/disk/simple_folder',
            'dst': '/disk/new_folder',
            'connection_id': '12345',
            'force': 1,
        }
        self.mail_error('async_move', opts, code=126)
        opts = {
            'uid': self.uid,
            'path': '/disk/simple_folder',
        }
        self.mail_ok('rm', opts)
        #=======================================================================
        self.inspect_all()
