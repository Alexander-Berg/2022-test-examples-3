# -*- coding: utf-8 -*-
import re

import mock

from lxml import etree
from nose_parameterized import parameterized

import mpfs.engine.process
from mpfs.common.static import codes

from test.common.sharing import CommonSharingMethods
from test.base_suit import set_up_open_url, tear_down_open_url
from test.helpers.stubs.services import PushServicesStub
from mpfs.core.filesystem.dao.legacy import CollectionRoutedDatabase


db = CollectionRoutedDatabase()


class TrashTwoSharingTestCase(CommonSharingMethods):

    def setup_method(self, method):
        super(TrashTwoSharingTestCase, self).setup_method(method)
        self.json_ok('user_init', {'uid': self.uid_1})
        self.json_ok('user_init', {'uid': self.uid_3})
        self.make_dirs()

    def test_move_group_to_trash(self):
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        self.assertNotEqual(db.groups.find_one({'_id': gid}), None)
        self.assertNotEqual(db.groups.find_one({'owner': str(self.uid), 'path': '/disk/new_folder/folder2'}), None)
        self.assertNotEqual(db.group_links.find_one({'uid': str(self.uid_3), 'path': '/disk/folder2'}), None)
        self.assertNotEqual(db.user_data.find_one({'uid': str(self.uid_3), 'path': '/disk/folder2'}), None)

        xiva_requests = set_up_open_url()
        opts = {
            'uid': self.uid,
            'path': '/disk/new_folder/folder2'
        }
        with mock.patch('mpfs.core.address.Address.add_trash_suffix'):
            self.mail_ok('async_trash_append', opts)
        tear_down_open_url()

        self.assertEqual(db.groups.find_one({'owner': str(self.uid), 'path': '/disk/new_folder/folder2'}), None)
        self.assertEqual(db.group_links.find_one({'uid': str(self.uid_3), 'path': '/disk/folder2'}), None)
        self.assertEqual(db.user_data.find_one({'uid': str(self.uid_3), 'path': '/disk/folder2'}), None)
        notify_sent_owner = False
        notify_sent_user = False
        remove_notify_sent_owner = False
        trash_notify_sent_owner = False
        url = 'http://localhost/service/echo?uid='

        for k, v in xiva_requests.iteritems():
            if k.startswith(url):
                uid = re.search('http://localhost/service/echo\?uid=(\d+)', k).group(1)
                for each in v:
                    share_tag = etree.fromstring(each['pure_data'])
                    if share_tag.tag == 'share':
                        if share_tag.get('type') == 'folder_unshared':
                            if share_tag.get('for') == 'owner':
                                notify_sent_owner = True
                            elif share_tag.get('for') == 'user':
                                self.assertTrue(re.match("^/disk/folder2/$|/disk/folder2 \d+/$", share_tag.find('folder').get('path')))
                                self.assertTrue(re.match("^folder2$|folder2 \d+$", share_tag.find('folder').get('name')))
                                self.assertEqual(share_tag.find('folder').get('gid'), gid)
                                notify_sent_user = True
                        elif share_tag.get('type') == 'user_has_left':
                            self.fail()
                    elif share_tag.tag == 'diff':
                        if uid == str(self.uid):
                            for op in share_tag.iterfind('op'):
                                if op.get('key') == "/disk/new_folder/folder2":
                                    self.assertEqual(op.get('type'), 'deleted')
                                    remove_notify_sent_owner = True
                                elif op.get('key') == "/trash/folder2":
                                    self.assertEqual(op.get('type'), 'new')
                                    trash_notify_sent_owner = True
        self.assertFalse(notify_sent_owner)
        self.assertTrue(notify_sent_user)
        self.assertTrue(remove_notify_sent_owner)
        self.assertTrue(trash_notify_sent_owner)

    def test_move_group_parent_to_trash(self):
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        self.assertNotEqual(db.groups.find_one({'_id': gid}), None)
        self.assertNotEqual(db.groups.find_one({'owner': str(self.uid), 'path': '/disk/new_folder/folder2'}), None)
        self.assertNotEqual(db.group_links.find_one({'uid': str(self.uid_3), 'path': '/disk/folder2'}), None)
        self.assertNotEqual(db.user_data.find_one({'uid': str(self.uid_3), 'path': '/disk/folder2'}), None)

        opts = {
            'uid': self.uid,
            'path': '/disk/new_folder'
        }
        self.json_ok('list', opts)
        xiva_requests = set_up_open_url()
        self.mail_ok('async_trash_append', opts)
        tear_down_open_url()

        self.json_error('list', opts)
        self.assertEqual(db.user_data.find_one({'uid': str(self.uid), 'path': '/disk/new_folder/folder2'}), None)
        self.assertEqual(db.groups.find_one({'owner': str(self.uid), 'path': '/disk/new_folder/folder2'}), None)
        self.assertEqual(db.group_links.find_one({'uid': str(self.uid_3), 'path': '/disk/folder2'}), None)
        self.assertEqual(db.user_data.find_one({'uid': str(self.uid_3), 'path': '/disk/folder2'}), None)
        self.assertEqual(db.user_data.find_one({'uid': str(self.uid), 'path': '/disk/new_folder'}), None)
        notify_sent_owner = False
        notify_sent_user = False
        url = 'http://localhost/service/echo?uid='
#        self.fail(xiva_requests)
        for k, v in xiva_requests.iteritems():
            if k.startswith(url):
                uid = re.search('http://localhost/service/echo\?uid=(\d+)', k).group(1)
                for each in v:
                    share_tag = etree.fromstring(each['pure_data'])
                    if share_tag.tag == 'share':
                        if share_tag.get('type') == 'folder_unshared':
                            if share_tag.get('for') == 'owner':
                                self.assertEqual(uid, str(self.uid))
                                self.assertEqual(share_tag.find('folder').get('path'), "/disk/new_folder/folder2/")
                                self.assertEqual(share_tag.find('folder').get('name'), 'folder2')
                                self.assertEqual(share_tag.find('folder').get('gid'), gid)
                                notify_sent_owner = True
                            elif share_tag.get('for') == 'user':
                                self.assertTrue(re.match("^/disk/folder2/$|/disk/folder2 \d+/$", share_tag.find('folder').get('path')))
                                self.assertTrue(re.match("^folder2$|folder2 \d+$", share_tag.find('folder').get('name')))
                                self.assertEqual(share_tag.find('folder').get('gid'), gid)
                                notify_sent_user = True
                        elif share_tag.get('type') == 'user_has_left':
                            self.fail()
        self.assertTrue(notify_sent_owner)
        self.assertTrue(notify_sent_user)

    @staticmethod
    def _has_resource_by_path(coll, uid, path):
        for item in coll.find({'uid': str(uid)}):
            if item['key'].startswith(path):
                return True
        return False

    def test_move_shared_folder_to_trash(self):
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        self.assertNotEqual(db.groups.find_one({'_id': gid}), None)
        self.assertNotEqual(db.groups.find_one({'owner': str(self.uid), 'path': '/disk/new_folder/folder2'}), None)
        self.assertNotEqual(db.group_links.find_one({'uid': str(self.uid_3), 'path': '/disk/folder2'}), None)
        self.assertNotEqual(db.user_data.find_one({'uid': str(self.uid_3), 'path': '/disk/folder2'}), None)

        xiva_requests = set_up_open_url()
        opts = {
            'uid': self.uid_3,
            'path': '/disk/folder2'
        }
        self.mail_ok('async_trash_append', opts)
        tear_down_open_url()

        self.assertNotEqual(db.groups.find_one({'owner': str(self.uid), 'path': '/disk/new_folder/folder2'}), None)
        self.assertNotEqual(db.user_data.find_one({'uid': str(self.uid), 'path': '/disk/new_folder/folder2'}), None)
        self.assertTrue(self._has_resource_by_path(db.user_data, str(self.uid), '/disk/new_folder/folder2/'))
        self.assertEqual(db.group_links.find_one({'uid': str(self.uid_3), 'path': '/disk/folder2'}), None)
        self.assertEqual(db.user_data.find_one({'uid': str(self.uid_3), 'path': '/disk/folder2'}), None)
        notify_sent_owner = False
        notify_sent_user = False
        url = 'http://localhost/service/echo?uid='
#        self.fail(xiva_requests)
        for k, v in xiva_requests.iteritems():
            if k.startswith(url):
                uid = re.search('http://localhost/service/echo\?uid=(\d+)', k).group(1)
                for each in v:
                    share_tag = etree.fromstring(each['pure_data'])
                    if share_tag.tag == 'share':
                        if share_tag.get('type') == 'user_has_left':
                            if share_tag.get('for') == 'owner':
                                self.assertEqual(uid, str(self.uid))
                                self.assertEqual(share_tag.find('folder').get('path'), "/disk/new_folder/folder2/")
                                self.assertEqual(share_tag.find('folder').get('name'), 'folder2')
                                self.assertEqual(share_tag.find('folder').get('gid'), gid)
                                notify_sent_owner = True
                            elif share_tag.get('for') == 'actor':
                                self.assertTrue(re.match("^/disk/folder2/$|/disk/folder2 \d+/$", share_tag.find('folder').get('path')))
                                self.assertEqual(share_tag.find('folder').get('gid'), gid)
                                notify_sent_user = True
                        else:
                            self.fail()
        self.assertTrue(notify_sent_owner)
        self.assertTrue(notify_sent_user)

        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid, rights=640)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        self.assertNotEqual(db.group_links.find_one({'uid': str(self.uid_3), 'path': '/disk/folder2'}), None)
        self.assertNotEqual(db.user_data.find_one({'uid': str(self.uid_3), 'path': '/disk/folder2'}), None)

        xiva_requests = set_up_open_url()
        opts = {
            'uid': self.uid_3,
            'path': '/disk/folder2'
        }
        self.mail_ok('async_trash_append', opts)
        tear_down_open_url()

        self.assertNotEqual(db.groups.find_one({'owner': str(self.uid), 'path': '/disk/new_folder/folder2'}), None)
        self.assertNotEqual(db.user_data.find_one({'uid': str(self.uid), 'path': '/disk/new_folder/folder2'}), None)
        self.assertTrue(self._has_resource_by_path(db.user_data, str(self.uid), '/disk/new_folder/folder2/'))
        self.assertEqual(db.user_data.find_one({'uid': str(self.uid_3), 'path': '/disk/folder2'}), None)
        self.assertEqual(db.group_links.find_one({'uid': str(self.uid_3), 'path': '/disk/folder2'}), None)
        notify_sent_owner = False
        notify_sent_user = False
        url = 'http://localhost/service/echo?uid='
#        self.fail(xiva_requests)
        for k, v in xiva_requests.iteritems():
            if k.startswith(url):
                uid = re.search('http://localhost/service/echo\?uid=(\d+)', k).group(1)
                for each in v:
                    share_tag = etree.fromstring(each['pure_data'])
                    if share_tag.tag == 'share':
                        if share_tag.get('type') == 'user_has_left':
                            if share_tag.get('for') == 'owner':
                                self.assertEqual(uid, str(self.uid))
                                self.assertEqual(share_tag.find('folder').get('path'), "/disk/new_folder/folder2/")
                                self.assertEqual(share_tag.find('folder').get('name'), 'folder2')
                                self.assertEqual(share_tag.find('folder').get('gid'), gid)
                                notify_sent_owner = True
                            elif share_tag.get('for') == 'actor':
                                self.assertTrue(re.match("^/disk/folder2/$|/disk/folder2 \d+/$", share_tag.find('folder').get('path')))
                                self.assertEqual(share_tag.find('folder').get('gid'), gid)
                                notify_sent_user = True
                        else:
                            self.fail()
        self.assertTrue(notify_sent_owner)
        self.assertTrue(notify_sent_user)
        self.inspect_all()

    def test_move_readonly_shared_folder_parent_to_trash(self):
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid, rights=640)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        opts = {
            'uid': self.uid_3,
            'path': '/disk/folder66',
            'connection_id': '12345',
        }
        self.json_ok('mkdir', opts)

        self.assertNotEqual(db.group_links.find_one({'uid': str(self.uid_3), 'path': '/disk/folder2'}), None)
        self.assertNotEqual(db.user_data.find_one({'uid': str(self.uid_3), 'path': '/disk/folder2'}), None)
        opts = {
            'uid': self.uid_3,
            'src': '/disk/folder2',
            'dst': '/disk/folder66/folder2',
            'connection_id': '12345',
        }
        self.mail_ok('async_move', opts)
        self.assertNotEqual(db.group_links.find_one({'uid': str(self.uid_3), 'path': '/disk/folder66/folder2'}), None)
        self.assertNotEqual(db.user_data.find_one({'uid': str(self.uid_3), 'path': '/disk/folder66/folder2'}), None)

        opts = {
            'uid': self.uid_3,
            'path': '/disk/folder66/folder67'
        }
        self.json_ok('mkdir', opts)
        opts = {
            'uid': self.uid_3,
            'path': '/disk/folder66/folder67/folder68'
        }
        self.json_ok('mkdir', opts)
        self.upload_file(self.uid_3, '/disk/folder66/folder67/file_670')
        self.upload_file(self.uid_3, '/disk/folder66/folder67/folder68/file680')

        xiva_requests = set_up_open_url()
        opts = {
            'uid': self.uid_3,
            'path': '/disk/folder66'
        }
        with mock.patch('mpfs.core.address.Address.add_trash_suffix'):
            self.mail_ok('async_trash_append', opts)
        tear_down_open_url()
        self.assertNotEqual(db.groups.find_one({'owner': str(self.uid), 'path': '/disk/new_folder/folder2'}), None)
        self.assertNotEqual(db.user_data.find_one({'uid': str(self.uid), 'path': '/disk/new_folder/folder2'}), None)
        self.assertTrue(self._has_resource_by_path(db.user_data, str(self.uid), '/disk/new_folder/folder2/'))
        self.assertEqual(db.user_data.find_one({'uid': str(self.uid_3), 'path': '/disk/folder66/folder2'}), None)
        self.assertEqual(db.user_data.find_one({'uid': str(self.uid_3), 'path': '/disk/folder66'}), None)
        self.assertEqual(db.group_links.find_one({'uid': str(self.uid_3), 'path': '/disk/folder66/folder2'}), None)
        notify_sent_owner = False
        notify_sent_user = False
        with_shared_user = False
        trash_child = False
        url = 'http://localhost/service/echo?uid='
#        self.fail(xiva_requests)
        for k, v in xiva_requests.iteritems():
            if k.startswith(url):
                uid = re.search('http://localhost/service/echo\?uid=(\d+)', k).group(1)
                for each in v:
                    share_tag = etree.fromstring(each['pure_data'])
                    if share_tag.tag == 'share':
                        if share_tag.get('type') == 'user_has_left':
                            if share_tag.get('for') == 'owner':
                                self.assertEqual(uid, str(self.uid))
                                self.assertEqual(share_tag.find('folder').get('path'), "/disk/new_folder/folder2/")
                                self.assertEqual(share_tag.find('folder').get('name'), 'folder2')
                                self.assertEqual(share_tag.find('folder').get('gid'), gid)
                                notify_sent_owner = True
                            elif share_tag.get('for') == 'actor':
                                self.assertEqual(uid, str(self.uid_3))
                                self.assertTrue(re.match("^/disk/folder66/folder2/$|/disk/folder66/folder2 \d+/$", share_tag.find('folder').get('path')))
                                self.assertEqual(share_tag.find('folder').get('gid'), gid)
                                notify_sent_user = True
                        else:
                            self.fail()
                    elif share_tag.tag == 'diff':
#                        self.fail(etree.tostring(share_tag, pretty_print=True))
                        with_shared_user = True
                        for op in share_tag.iterfind('op'):
                            self.assertNotEqual(op.get('resource_type'), None)
                            if op.get('key') == '/disk/folder66':
                                self.assertTrue(len(list(share_tag.iterfind('op'))) > 2)
                                self.assertEqual(share_tag.get('with_shared'), '1')
                                self.assertEqual(uid, str(self.uid_3))
                                self.assertEqual(op.get('type'), 'deleted')
                            elif op.get('key') == '/trash/folder66':
                                self.assertEqual(uid, str(self.uid_3))
                                self.assertEqual(op.get('type'), 'new')
                            elif op.get('key').startswith('/trash/folder66/folder67'):
                                self.assertEqual(uid, str(self.uid_3))
                                self.assertEqual(op.get('type'), 'new')
                                trash_child = True
                            else:
                                self.fail(op.get('key'))
        self.assertTrue(notify_sent_owner)
        self.assertTrue(notify_sent_user)
        self.assertTrue(with_shared_user)
        self.assertTrue(trash_child)
        self.assertTrue(self._has_resource_by_path(db.user_data, str(self.uid), '/disk/new_folder/folder2/'))
        self.assertTrue(self._has_resource_by_path(db.trash,  str(self.uid_3), '/trash/folder66'))
        self.assertFalse(self._has_resource_by_path(db.trash, str(self.uid_3), '/trash/folder66/folder2'))
        #=======================================================================

    def test_move_readonly_shared_folder_grand_parent_to_trash(self):
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid, rights=640)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        opts = {
            'uid': self.uid_3,
            'path': '/disk/folder77',
            'connection_id': '12345',
        }
        self.json_ok('mkdir', opts)
        opts = {
            'uid': self.uid_3,
            'path': '/disk/folder77/folder88',
            'connection_id': '12345',
        }
        self.json_ok('mkdir', opts)
        self.assertNotEqual(db.group_links.find_one({'uid': str(self.uid_3), 'path': '/disk/folder2'}), None)
        self.assertNotEqual(db.user_data.find_one({'uid': str(self.uid_3), 'path': '/disk/folder2'}), None)
        opts = {
            'uid': self.uid_3,
            'src': '/disk/folder2',
            'dst': '/disk/folder77/folder88/folder2',
            'connection_id': '12345',
        }
        self.mail_ok('async_move', opts)
        self.assertNotEqual(db.group_links.find_one({'uid': str(self.uid_3), 'path': '/disk/folder77/folder88/folder2'}), None)
        self.assertNotEqual(db.user_data.find_one({'uid': str(self.uid_3), 'path': '/disk/folder77/folder88/folder2'}), None)
        xiva_requests = set_up_open_url()
        opts = {
            'uid': self.uid_3,
            'path': '/disk/folder77'
        }
        with mock.patch('mpfs.core.address.Address.add_trash_suffix'):
            self.mail_ok('async_trash_append', opts)
        tear_down_open_url()
        self.assertNotEqual(db.groups.find_one({'owner': str(self.uid), 'path': '/disk/new_folder/folder2'}), None)
        self.assertNotEqual(db.user_data.find_one({'uid': str(self.uid), 'path': '/disk/new_folder/folder2'}), None)
        self.assertEqual(db.user_data.find_one({'uid': str(self.uid_3), 'path': '/disk/folder77/folder88/folder2'}), None)
        self.assertEqual(db.group_links.find_one({'uid': str(self.uid_3), 'path': '/disk/folder77/folder88/folder2'}), None)
        notify_sent_owner = False
        notify_sent_user = False
        url = 'http://localhost/service/echo?uid='
#        self.fail(xiva_requests)
        for k, v in xiva_requests.iteritems():
            if k.startswith(url):
                uid = re.search('http://localhost/service/echo\?uid=(\d+)', k).group(1)
                for each in v:
                    share_tag = etree.fromstring(each['pure_data'])
                    if share_tag.tag == 'share':
                        if share_tag.get('type') == 'user_has_left':
                            if share_tag.get('for') == 'owner':
                                self.assertEqual(uid, str(self.uid))
                                self.assertEqual(share_tag.find('folder').get('path'), "/disk/new_folder/folder2/")
                                self.assertEqual(share_tag.find('folder').get('name'), 'folder2')
                                self.assertEqual(share_tag.find('folder').get('gid'), gid)
                                notify_sent_owner = True
                            elif share_tag.get('for') == 'actor':
                                self.assertTrue(re.match("^/disk/folder77/folder88/folder2/$|/disk/folder77/folder88/folder2 \d+/$", share_tag.find('folder').get('path')))
                                self.assertEqual(share_tag.find('folder').get('gid'), gid)
                                notify_sent_user = True
                        else:
                            self.fail()
        self.assertTrue(notify_sent_owner)
        self.assertTrue(notify_sent_user)
        #=======================================================================
        self.assertTrue(self._has_resource_by_path(db.trash,  str(self.uid_3), '/trash/folder77/folder88'))
        self.assertFalse(self._has_resource_by_path(db.trash, str(self.uid_3), '/trash/folder77/folder88/folder2'))

    def test_trash_append_owner(self):
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        self.upload_file(self.uid, '/disk/new_folder/folder2/file_for_trash')
        opts = {'uid': self.uid, 'path': '/disk/new_folder/folder2/file_for_trash'}
        self.mail_ok('info', opts)

        opts = {
            'uid': self.uid,
            'path': '/disk/new_folder/folder2/file_for_trash',
            'connection_id': '12345',
        }
        with PushServicesStub() as push_service:
            with mock.patch('mpfs.core.address.Address.add_trash_suffix'):
                self.mail_ok('async_trash_append', opts)
            pushes = [PushServicesStub.parse_send_call(push_args)
                      for push_args in push_service.send.call_args_list]

        user_push = pushes[0]
        owner_push = pushes[1]
        assert owner_push['uid'] == self.uid
        assert owner_push['connection_id'] == '12345'
        assert owner_push['json_payload']['root']['tag'] == 'diff'
        op_values = [value['parameters']
                     for value in owner_push['json_payload']['values']
                     if value['tag'] == 'op']
        assert len(op_values) == 2
        assert owner_push['json_payload']['values'][0]['parameters']['key'] == '/disk/new_folder/folder2/file_for_trash'
        assert owner_push['json_payload']['values'][0]['parameters']['type'] == 'deleted'
        assert owner_push['json_payload']['values'][1]['parameters']['key'] == '/trash/file_for_trash'
        assert owner_push['json_payload']['values'][1]['parameters']['type'] == 'new'

        assert user_push['uid'] == self.uid_3
        assert user_push['connection_id'] == None
        assert user_push['json_payload']['root']['tag'] == 'diff'
        op_values = [value['parameters']
                     for value in user_push['json_payload']['values']
                     if value['tag'] == 'op']
        assert len(op_values) == 1
        assert user_push['json_payload']['values'][0]['parameters']['key'] == '/disk/folder2/file_for_trash'
        assert user_push['json_payload']['values'][0]['parameters']['type'] == 'deleted'
        assert user_push['json_payload']['values'][0]['parameters']['resource_type'] == 'file'

        self.inspect_all()

    def test_remove_group_folder(self):
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        opts = {
            'uid': self.uid,
            'path': '/disk/new_folder/folder2',
        }
        self.mail_ok('info', opts)
        open_url_data = set_up_open_url()
        opts = {
            'uid': self.uid,
            'path': '/disk/new_folder/folder2',
        }
        with mock.patch('mpfs.core.address.Address.add_trash_suffix'):
            self.mail_ok('trash_append', opts)
        tear_down_open_url()
        url = 'http://localhost/service/echo?uid='
        # self.fail(open_url_data)
        trash_found = False
        disk_found = False
        for k, v in open_url_data.iteritems():
            if k.startswith(url):
                uid = re.search('http://localhost/service/echo\?uid=(\d+)', k).group(1)
                if str(uid) == str(self.uid):
                    for each in v:
                        share_tag = etree.fromstring(each['pure_data'])
                        if share_tag.tag == 'diff':
                            self.assertEqual(share_tag.get('is_group_root'), '1')
                            for op in share_tag.iterfind('op'):
                                self.assertNotEqual(op.get('resource_type'), None)
                                if op.get('key') == '/disk/new_folder/folder2' and op.get('type') == 'deleted':
                                    self.assertEqual(op.get('is_group_root'), None)
                                    disk_found = True
                                elif op.get('key') == '/trash/folder2' and op.get('type') == 'new':
                                    self.assertEqual(op.get('is_group_root'), None)
                                    self.assertEqual(op.get('shared_rights'), None)
                                    trash_found = True
                                else:
                                    self.assertEqual(op.get('is_group_root'), None)
                                    self.assertEqual(op.get('shared_rights'), None)
        self.assertTrue(trash_found)
        self.assertTrue(disk_found)

        opts = {
            'uid': str(self.uid),
        }
        self.mail_ok('async_trash_drop_all', opts)

        opts = {
            'uid': self.uid,
            'path': '/disk/new_folder/folder2',
        }
        self.mail_error('list', opts)

        opts = {
            'uid': self.uid_3,
            'path': '/disk/folder2 1/',
        }
        self.mail_error('list', opts)

        self.assertEqual(db.group_links.find_one({'uid': str(self.uid_3), 'path': '/disk/folder2'}), None)
        self.assertEqual(db.groups.find_one({'owner': str(self.uid), 'path': '/disk/new_folder/folder2'}), None)
        opts = {
            'uid': self.uid,
            'path': '/disk/new_folder',
        }
        self.mail_ok('rm', opts)
        self.inspect_all()

    def test_remove_readonly_folder_check_lock(self):
        """
        https://st.yandex-team.ru/CHEMODAN-19687
        """
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid, rights=640)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        opts = {
            'uid': self.uid_3,
            'path': '/disk/folder2',
        }
        self.json_ok('info', opts)
        self.upload_file(self.uid, '/disk/new_folder/folder2/sometest.txt')

        # отписываемся от папки путем ее перемещения в корзну
        opts = {
            'uid': self.uid_3,
            'path': '/disk/folder2',
        }
        self.json_ok('async_trash_append', opts)

        # проверяем, что файлы у владельца заливаются
        self.upload_file(self.uid, '/disk/new_folder/folder2/sometest 2.txt')

        opts = {
            'uid': self.uid,
            'path': '/disk/new_folder/folder2/sometest 2.txt',
        }
        self.json_ok('info', opts)

    @parameterized.expand([
        (True, 'async_trash_append_by_resource_id'),
        (False, 'async_trash_append_by_resource_id'),
        (True, 'async_rm_by_resource_id'),
        (False, 'async_rm_by_resource_id'),
        (True, 'trash_append_by_resource_id'),
        (False, 'trash_append_by_resource_id'),
        (True, 'rm_by_resource_id'),
        (False, 'rm_by_resource_id'),
    ])
    def test_delete_by_resource_id_remove_file_in_shared(self, owner_deletes, endpoint):
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid)
        self.activate_invite(uid=self.uid_3, hash=hsh)
        if owner_deletes:
            path = '/disk/new_folder/folder2/file3'
            uid_deleter = self.uid
        else:
            path = '/disk/folder2/file3'
            uid_deleter = self.uid_3
        path_in_trash = '/trash/file3'
        resource_id = self.json_ok('info', {'uid': uid_deleter, 'path': path, 'meta': ''})['meta']['resource_id']
        with mock.patch('mpfs.core.address.Address.add_trash_suffix'):
            self.json_ok(endpoint, {'uid': uid_deleter, 'resource_id': resource_id})
        self.json_error('info', {'uid': uid_deleter, 'path': path, 'meta': ''}, code=codes.RESOURCE_NOT_FOUND)
        if endpoint in {'async_trash_append_by_resource_id', 'trash_append_by_resource_id'}:
            self.json_ok('info', {'uid': uid_deleter, 'path': path_in_trash, 'meta': ''})
            if not owner_deletes:
                self.json_ok('info', {'uid': self.uid, 'path': path_in_trash, 'meta': ''})
        else:
            self.json_error('info', {'uid': uid_deleter, 'path': path_in_trash, 'meta': ''}, codes.RESOURCE_NOT_FOUND)
            self.json_error('info', {'uid': self.uid, 'path': path_in_trash, 'meta': ''}, codes.RESOURCE_NOT_FOUND)

    @parameterized.expand([
        (True, 'async_trash_append_by_resource_id'),
        (False, 'async_trash_append_by_resource_id'),
        (True, 'async_rm_by_resource_id'),
        (False, 'async_rm_by_resource_id'),
        (True, 'trash_append_by_resource_id'),
        (False, 'trash_append_by_resource_id'),
        (True, 'rm_by_resource_id'),
        (False, 'rm_by_resource_id'),
    ])
    def test_delete_by_resource_id_remove_folder_in_shared(self, owner_deletes, endpoint):
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid)
        self.activate_invite(uid=self.uid_3, hash=hsh)
        if owner_deletes:
            path = '/disk/new_folder/folder2/folder3'
            uid_deleter = self.uid
        else:
            path = '/disk/folder2/folder3'
            uid_deleter = self.uid_3
        path_in_trash = '/trash/folder3'
        resource_id = self.json_ok('info', {'uid': uid_deleter, 'path': path, 'meta': ''})['meta']['resource_id']
        with mock.patch('mpfs.core.address.Address.add_trash_suffix'):
            self.json_ok(endpoint, {'uid': uid_deleter, 'resource_id': resource_id})
        self.json_error('info', {'uid': uid_deleter, 'path': path, 'meta': ''}, codes.RESOURCE_NOT_FOUND)

        if endpoint in {'async_trash_append_by_resource_id', 'trash_append_by_resource_id'}:
            assert len(self.json_ok('list', {'uid': uid_deleter, 'path': path_in_trash, 'meta': ''})) == 3
            if not owner_deletes:
                assert len(self.json_ok('list', {'uid': self.uid, 'path': path_in_trash, 'meta': ''})) == 3
        else:
            self.json_error('list', {'uid': self.uid, 'path': path_in_trash, 'meta': ''}, code=codes.LIST_NOT_FOUND)
            self.json_error('list', {'uid': uid_deleter, 'path': path_in_trash, 'meta': ''}, code=codes.LIST_NOT_FOUND)

    @parameterized.expand([
        ('async_trash_append_by_resource_id', ),
        ('async_rm_by_resource_id', ),
        ('trash_append_by_resource_id',),
        ('rm_by_resource_id',),
    ])
    def test_cannot_delete_by_resource_id_if_no_write_rights(self, endpoint):
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid, rights=640)
        self.activate_invite(uid=self.uid_3, hash=hsh)
        path = '/disk/folder2/file3'
        resource_id = self.json_ok('info', {'uid': self.uid_3, 'path': path, 'meta': ''})['meta']['resource_id']
        if 'async' in endpoint:
            oid = self.json_ok(endpoint, {'uid': self.uid_3, 'resource_id': resource_id})['oid']
            operation_res = self.json_ok('status', {'uid': self.uid_3, 'oid': oid})
            assert operation_res['status'] == 'FAILED'
            assert operation_res['error']['code'] == codes.GROUP_NOT_PERMIT
        else:
            self.json_error(endpoint, {'uid': self.uid_3, 'resource_id': resource_id}, code=codes.GROUP_NOT_PERMIT)
