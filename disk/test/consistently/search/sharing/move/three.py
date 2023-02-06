# -*- coding: utf-8 -*-
import urlparse

from test.common.sharing import SharingWithSearchTestCase
from test.base_suit import set_up_open_url, tear_down_open_url

import mpfs.engine.process

from mpfs.common.util import from_json
from test.helpers.stubs.services import PushServicesStub
from mpfs.core.filesystem.dao.legacy import CollectionRoutedDatabase


db = CollectionRoutedDatabase()


class MoveThreeSharingTestCase(SharingWithSearchTestCase):

    def setup_method(self, method):
        super(MoveThreeSharingTestCase, self).setup_method(method)
        self.json_ok('user_init', {'uid': self.uid_1})
        self.json_ok('user_init', {'uid': self.uid_3})
        self.make_dirs()

    def test_move_group_folder(self):
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        opts = {
            'uid': self.uid_3,
            'path': '/disk/folder2',
        }
        self.mail_ok('list', opts)
        self.assertNotEqual(db.group_links.find_one({'uid': str(self.uid_3), 'path': '/disk/folder2'}), None)
        args = {'uid': self.uid, 'path': '/disk/folder1'}
        self.json_ok('mkdir', args)
        args = {'uid': self.uid, 'path': '/disk/folder1/folder2'}
        self.json_ok('mkdir', args)
        self.assertNotEqual(db.groups.find_one({'owner': str(self.uid), 'path': '/disk/new_folder/folder2'}), None)
        self.assertEqual(db.groups.find_one({'owner': str(self.uid), 'path': '/disk/folder1/folder2/new_group_folder'}),
                         None)
        args = {
            'uid': self.uid,
            'src': '/disk/new_folder/folder2',
            'dst': '/disk/folder1/folder2/new_group_folder',
        }
        open_url_data = set_up_open_url()
        self.mail_ok('move', args)
        tear_down_open_url()
        folder_rid = self.json_ok('info', {'uid': self.uid, 'path': '/disk/folder1/folder2/new_group_folder', 'meta': 'resource_id'})['meta']['resource_id']
        self.assertEqual(db.groups.find_one({'owner': str(self.uid), 'path': '/disk/new_folder/folder2'}), None)
        self.assertNotEqual(
            db.groups.find_one({'owner': str(self.uid), 'path': '/disk/folder1/folder2/new_group_folder'}), None)
        self.assertNotEqual(db.group_links.find_one({'uid': str(self.uid_3), 'path': '/disk/folder2'}), None)

        # =======================================================================
        #
        search_notify_found_owner = False
        for k, v in open_url_data.iteritems():
            if not k.startswith(self.search_url):
                continue
            qs = urlparse.parse_qs(urlparse.urlparse(k).query)
            if qs['action'][0] != 'move_resource':
                continue
            search_notify_found_owner |= qs['prefix'][0] == self.uid and qs['resource_id'][0] == folder_rid
        self.assertTrue(search_notify_found_owner)
        #=======================================================================
        opts = {
            'uid': self.uid,
            'path': '/disk/new_folder/folder2',
        }
        self.mail_error('list', opts)
        opts = {
            'uid': self.uid,
            'path': '/disk/folder1/folder2/new_group_folder',
        }
        self.mail_ok('list', opts)
        opts = {
            'uid': self.uid_3,
            'path': '/disk/folder2',
        }
        self.mail_ok('list', opts)
        args = {
            'uid': self.uid,
            'src': '/disk/folder1/folder2/new_group_folder',
            'dst': '/disk/new_folder/folder2',
        }
        self.mail_ok('move', args)
        self.assertNotEqual(db.groups.find_one({'owner': str(self.uid), 'path': '/disk/new_folder/folder2'}), None)
        self.assertEqual(db.groups.find_one({'owner': str(self.uid), 'path': '/disk/folder1/folder2/new_group_folder'}),
                         None)
        self.assertNotEqual(db.group_links.find_one({'uid': str(self.uid_3), 'path': '/disk/folder2'}), None)
        opts = {
            'uid': self.uid,
            'path': '/disk/new_folder/folder2',
        }
        self.mail_ok('list', opts)
        opts = {
            'uid': self.uid,
            'path': '/disk/folder1/folder2/new_group_folder',
        }
        self.mail_error('list', opts)
        self.inspect_all()

    def test_move_group_folder_parent(self):
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        args = {'uid': self.uid, 'path': '/disk/folder1'}
        self.json_ok('mkdir', args)
        args = {'uid': self.uid, 'path': '/disk/folder1/folder2'}
        self.json_ok('mkdir', args)

        opts = {
            'uid': self.uid,
            'path': '/disk/new_folder',
        }
        folder_list = self.mail_ok('list', opts).find('folder-list')
        # self.fail(etree.tostring(folder_list, pretty_print=True))
        self.assertEqual(folder_list.find('folder').find('id').text, '/disk/new_folder')
        self.assertTrue(len(list(folder_list.find('folder').find('folder-list').iterchildren())) > 0)
        self.assertEqual(folder_list.find('folder').find('meta').find('group'), None)

        opts = {
            'uid': self.uid,
            'path': '/disk/new_folder/folder2',
        }
        folder_list = self.mail_ok('list', opts).find('folder-list')
        #        self.fail(etree.tostring(folder_list, pretty_print=True))
        self.assertEqual(folder_list.find('folder').find('id').text, '/disk/new_folder/folder2')
        self.assertTrue(len(list(folder_list.find('folder').find('folder-list').iterchildren())) > 0)
        self.assertNotEqual(folder_list.find('folder').find('meta').find('group'), None)

        args = {
            'uid': self.uid,
            'src': '/disk/new_folder',
            'dst': '/disk/folder1/folder2/new_group_parent',
        }
        with PushServicesStub() as push_service:
            self.mail_ok('move', args)
            pushes = [PushServicesStub.parse_send_call(push_args)
                      for push_args in push_service.send.call_args_list]

        diff_push = pushes[0]

        assert diff_push['uid'] == self.uid
        op_values = [value['parameters']
                     for value in diff_push['json_payload']['values']
                     if value['tag'] == 'op']
        assert len(op_values) > 2
        assert diff_push['json_payload']['root']['tag'] == 'diff'

        deleted_found = False
        new_found = False
        group_root_found_2 = False
        group_root_content_found = False
        for op in op_values:
            assert op.get('resource_type') is not None
            if op.get('key') == '/disk/new_folder':
                assert op.get('type') == "deleted"
                deleted_found = True
            elif op.get('key') == '/disk/folder1/folder2/new_group_parent':
                assert op.get('type') == "new"
                assert op.get('with_shared') == 1
                new_found = True
            elif op.get('key').startswith('/disk/folder1/folder2/new_group_parent/folder2/'):
                assert op.get('type') == "new"
                group_root_content_found = True
            elif op.get('key') == "/disk/folder1/folder2/new_group_parent/folder2":
                assert op.get('type') == "new"
                assert op.get('shared_rights') == "owner"
                assert op.get('is_group_root') is None
                group_root_found_2 = True
            elif op.get('key').startswith('/disk/folder1/folder2/new_group_parent/'):
                assert op.get('type') == "new"
            elif op.get('key').startswith('/disk/folder1/folder2'):
                assert op.get('type') == "changed"

        assert deleted_found
        assert new_found
        assert group_root_content_found
        assert group_root_found_2


        opts = {
            'uid': self.uid,
            'path': '/disk/folder1/folder2/new_group_parent',
        }
        folder_list = self.mail_ok('list', opts).find('folder-list')
        #        self.fail(etree.tostring(folder_list, pretty_print=True))
        self.assertEqual(folder_list.find('folder').find('id').text, '/disk/folder1/folder2/new_group_parent')
        self.assertTrue(len(list(folder_list.find('folder').find('folder-list').iterchildren())) > 0)
        self.assertEqual(folder_list.find('folder').find('meta').find('group'), None)
        opts = {
            'uid': self.uid,
            'path': '/disk/folder1/folder2/new_group_parent/folder2',
        }
        folder_list = self.mail_ok('list', opts).find('folder-list')
        #        self.fail(etree.tostring(folder_list, pretty_print=True))
        self.assertEqual(folder_list.find('folder').find('id').text, '/disk/folder1/folder2/new_group_parent/folder2')
        self.assertTrue(len(list(folder_list.find('folder').find('folder-list').iterchildren())) > 0)
        self.assertNotEqual(folder_list.find('folder').find('meta').find('group'), None)

        args = {
            'uid': self.uid,
            'src': '/disk/folder1/folder2/new_group_parent',
            'dst': '/disk/new_folder',
        }
        self.mail_ok('move', args)
        opts = {
            'uid': self.uid,
            'path': '/disk/new_folder',
        }
        folder_list = self.mail_ok('list', opts).find('folder-list')
        #        self.fail(etree.tostring(folder_list, pretty_print=True))
        self.assertEqual(folder_list.find('folder').find('id').text, '/disk/new_folder')
        self.assertTrue(len(list(folder_list.find('folder').find('folder-list').iterchildren())) > 0)
        self.assertEqual(folder_list.find('folder').find('meta').find('group'), None)

        opts = {
            'uid': self.uid,
            'path': '/disk/new_folder/folder2',
        }
        folder_list = self.mail_ok('list', opts).find('folder-list')
        #        self.fail(etree.tostring(folder_list, pretty_print=True))
        self.assertNotEqual(folder_list.find('folder').find('meta').find('group'), None)
        self.assertEqual(folder_list.find('folder').find('id').text, '/disk/new_folder/folder2')
        self.assertTrue(len(list(folder_list.find('folder').find('folder-list').iterchildren())) > 0)
        self.inspect_all()

    def test_move_group_folder_parent_without_wrong_deletion(self):
        # case from CHEMODAN-75128 we DO NOT delete objects with same prefix as group folder
        shared_folder_path = '/disk/folder/prefix'

        args = {'uid': self.uid, 'path': '/disk/folder'}
        self.json_ok('mkdir', args)
        args = {'uid': self.uid, 'path': shared_folder_path}
        self.json_ok('mkdir', args)
        self.upload_file(self.uid, '/disk/folder/prefix/file')
        args = {'uid': self.uid, 'path': '/disk/folder/prefix-suffix'}
        self.json_ok('mkdir', args)
        self.upload_file(self.uid, '/disk/folder/prefix-suffix/file3')

        gid = self.create_group(path=shared_folder_path)
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, path=shared_folder_path, ext_gid=gid)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        args = {
            'uid': self.uid,
            'src': '/disk/folder',
            'dst': '/disk/folder_new',
        }
        self.mail_ok('move', args)

        self.json_ok('info', {'uid': self.uid, 'path': '/disk/folder_new/prefix', 'meta': ''})
        self.json_ok('info', {'uid': self.uid, 'path': '/disk/folder_new/prefix-suffix', 'meta': ''})
        self.json_ok('info', {'uid': self.uid, 'path': '/disk/folder_new/prefix-suffix/file3', 'meta': ''})

    def test_rename_parent_of_group_folder_parent(self):
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        args = {'uid': self.uid, 'path': '/disk/folder1'}
        self.json_ok('mkdir', args)
        args = {'uid': self.uid, 'path': '/disk/folder1/folder2'}
        self.json_ok('mkdir', args)

        opts = {
            'uid': self.uid,
            'path': '/disk/new_folder',
        }
        folder_list = self.mail_ok('list', opts).find('folder-list')
        # self.fail(etree.tostring(folder_list, pretty_print=True))
        self.assertEqual(folder_list.find('folder').find('id').text, '/disk/new_folder')
        self.assertTrue(len(list(folder_list.find('folder').find('folder-list').iterchildren())) > 0)
        self.assertEqual(folder_list.find('folder').find('meta').find('group'), None)

        opts = {
            'uid': self.uid,
            'path': '/disk/new_folder/folder2',
        }
        folder_list = self.mail_ok('list', opts).find('folder-list')
        #        self.fail(etree.tostring(folder_list, pretty_print=True))
        self.assertEqual(folder_list.find('folder').find('id').text, '/disk/new_folder/folder2')
        self.assertTrue(len(list(folder_list.find('folder').find('folder-list').iterchildren())) > 0)
        self.assertNotEqual(folder_list.find('folder').find('meta').find('group'), None)

        args = {'uid': self.uid,
                'src': '/disk/new_folder',
                'dst': '/disk/folder1/folder2/new_group_parent'}
        with PushServicesStub() as push_service:
            self.mail_ok('move', args)
            pushes = [PushServicesStub.parse_send_call(push_args)
                      for push_args in push_service.send.call_args_list]

        diff_push = pushes[0]

        assert diff_push['uid'] == self.uid
        op_values = [value['parameters']
                     for value in diff_push['json_payload']['values']
                     if value['tag'] == 'op']
        assert len(op_values) > 2
        assert diff_push['json_payload']['root']['tag'] == 'diff'

        deleted_found = False
        new_found = False
        group_root_found_2 = False
        group_root_content_found = False
        for op in op_values:
            assert op.get('resource_type') is not None
            if op.get('key') == '/disk/new_folder':
                assert op.get('type') == "deleted"
                deleted_found = True
            elif op.get('key') == '/disk/folder1/folder2/new_group_parent':
                assert op.get('with_shared') == 1
                assert op.get('type') == "new"
                new_found = True
            elif op.get('key').startswith('/disk/folder1/folder2/new_group_parent/folder2/folder3/'):
                assert op.get('type') == "new"
                group_root_content_found = True
            elif op.get('key') == "/disk/folder1/folder2/new_group_parent/folder2":
                assert op.get('shared_rights') == "owner"
                assert op.get('is_group_root') is None
                group_root_found_2 = True
            elif op.get('key').startswith('/disk/folder1/folder2/new_group_parent/'):
                assert op.get('type') == "new"
            elif op.get('key').startswith('/disk/folder1/folder2'):
                assert op.get('type') == "changed"

        assert deleted_found
        assert new_found
        assert group_root_content_found
        assert group_root_found_2

        opts = {
            'uid': self.uid,
            'path': '/disk/folder1/folder2/new_group_parent',
        }
        folder_list = self.mail_ok('list', opts).find('folder-list')
        #        self.fail(etree.tostring(folder_list, pretty_print=True))
        self.assertEqual(folder_list.find('folder').find('id').text, '/disk/folder1/folder2/new_group_parent')
        self.assertTrue(len(list(folder_list.find('folder').find('folder-list').iterchildren())) > 0)
        self.assertEqual(folder_list.find('folder').find('meta').find('group'), None)
        opts = {
            'uid': self.uid,
            'path': '/disk/folder1/folder2/new_group_parent/folder2',
        }
        folder_list = self.mail_ok('list', opts).find('folder-list')
        #        self.fail(etree.tostring(folder_list, pretty_print=True))
        self.assertEqual(folder_list.find('folder').find('id').text, '/disk/folder1/folder2/new_group_parent/folder2')
        self.assertTrue(len(list(folder_list.find('folder').find('folder-list').iterchildren())) > 0)
        self.assertNotEqual(folder_list.find('folder').find('meta').find('group'), None)

        #=======================================================================
        # Rename grandparent
        open_url_data = set_up_open_url()
        args = {
            'uid': self.uid,
            'src': '/disk/folder1/folder2',
            'dst': '/disk/folder1/new_grand_parent',
        }
        self.mail_ok('move', args)
        opts = {
            'uid': self.uid,
            'path': '/disk/folder1/new_grand_parent',
        }
        folder_list = self.mail_ok('list', opts).find('folder-list')
        #        self.fail(etree.tostring(folder_list, pretty_print=True))
        self.assertEqual(folder_list.find('folder').find('id').text, '/disk/folder1/new_grand_parent')
        self.assertTrue(len(list(folder_list.find('folder').find('folder-list').iterchildren())) > 0)
        self.assertEqual(folder_list.find('folder').find('meta').find('group'), None)
        opts = {
            'uid': self.uid,
            'path': '/disk/folder1/new_grand_parent/new_group_parent/folder2',
        }
        folder_list = self.mail_ok('list', opts).find('folder-list')
        #        self.fail(etree.tostring(folder_list, pretty_print=True))
        self.assertEqual(folder_list.find('folder').find('id').text,
                         '/disk/folder1/new_grand_parent/new_group_parent/folder2')
        self.assertTrue(len(list(folder_list.find('folder').find('folder-list').iterchildren())) > 0)
        self.assertNotEqual(folder_list.find('folder').find('meta').find('group'), None)
        #=======================================================================

        #=======================================================================
        # Rename great-grandparent
        open_url_data = set_up_open_url()
        args = {
            'uid': self.uid,
            'src': '/disk/folder1',
            'dst': '/disk/new_great_grand_parent',
        }
        self.mail_ok('move', args)
        opts = {
            'uid': self.uid,
            'path': '/disk/new_great_grand_parent',
        }
        folder_list = self.mail_ok('list', opts).find('folder-list')
        self.assertEqual(folder_list.find('folder').find('id').text, '/disk/new_great_grand_parent')
        self.assertTrue(len(list(folder_list.find('folder').find('folder-list').iterchildren())) > 0)
        self.assertEqual(folder_list.find('folder').find('meta').find('group'), None)
        opts = {
            'uid': self.uid,
            'path': '/disk/new_great_grand_parent/new_grand_parent',
        }
        folder_list = self.mail_ok('list', opts).find('folder-list')
        self.assertEqual(folder_list.find('folder').find('id').text, '/disk/new_great_grand_parent/new_grand_parent')
        self.assertTrue(len(list(folder_list.find('folder').find('folder-list').iterchildren())) > 0)
        self.assertEqual(folder_list.find('folder').find('meta').find('group'), None)
        opts = {
            'uid': self.uid,
            'path': '/disk/new_great_grand_parent/new_grand_parent/new_group_parent/folder2',
        }
        folder_list = self.mail_ok('list', opts).find('folder-list')
        #        self.fail(etree.tostring(folder_list, pretty_print=True))
        self.assertEqual(folder_list.find('folder').find('id').text,
                         '/disk/new_great_grand_parent/new_grand_parent/new_group_parent/folder2')
        self.assertTrue(len(list(folder_list.find('folder').find('folder-list').iterchildren())) > 0)
        self.assertNotEqual(folder_list.find('folder').find('meta').find('group'), None)
        #=======================================================================

        args = {
            'uid': self.uid,
            'src': '/disk/new_great_grand_parent/new_grand_parent/new_group_parent',
            'dst': '/disk/new_folder',
        }
        self.mail_ok('move', args)
        opts = {
            'uid': self.uid,
            'path': '/disk/new_folder',
        }
        folder_list = self.mail_ok('list', opts).find('folder-list')
        #        self.fail(etree.tostring(folder_list, pretty_print=True))
        self.assertEqual(folder_list.find('folder').find('id').text, '/disk/new_folder')
        self.assertTrue(len(list(folder_list.find('folder').find('folder-list').iterchildren())) > 0)
        self.assertEqual(folder_list.find('folder').find('meta').find('group'), None)

        opts = {
            'uid': self.uid,
            'path': '/disk/new_folder/folder2',
        }
        folder_list = self.mail_ok('list', opts).find('folder-list')
        #        self.fail(etree.tostring(folder_list, pretty_print=True))
        self.assertNotEqual(folder_list.find('folder').find('meta').find('group'), None)
        self.assertEqual(folder_list.find('folder').find('id').text, '/disk/new_folder/folder2')
        self.assertTrue(len(list(folder_list.find('folder').find('folder-list').iterchildren())) > 0)
        self.inspect_all()

    def test_move_shared_folder(self):
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        opts = {
            'uid': self.uid_3,
            'path': '/disk/folder2',
        }
        self.mail_ok('list', opts)
        self.assertNotEqual(db.group_links.find_one({'uid': str(self.uid_3), 'path': '/disk/folder2'}), None)

        opts = {
            'uid': self.uid_3,
            'src': '/disk/folder2',
            'dst': '/disk/folder3',
        }
        self.mail_ok('move', opts)

        self.assertEqual(db.user_data.find_one({'uid': str(self.uid_3), 'key': '/disk/folder2'}), None)
        self.assertEqual(db.group_links.find_one({'uid': str(self.uid_3), 'path': '/disk/folder2'}), None)
        self.assertNotEqual(db.group_links.find_one({'uid': str(self.uid_3), 'path': '/disk/folder3'}), None)
        self.assertNotEqual(db.user_data.find_one({'uid': str(self.uid_3), 'key': '/disk/folder3'}), None)
        opts = {
            'uid': self.uid_3,
            'path': '/disk/folder3',
        }
        self.mail_ok('list', opts)
        opts = {
            'uid': self.uid_3,
            'path': '/disk/folder2',
        }
        self.mail_error('list', opts)

        opts = {
            'uid': self.uid_3,
            'src': '/disk/folder3',
            'dst': '/disk/folder2',
        }
        self.mail_ok('move', opts)
        self.assertEqual(db.group_links.find_one({'uid': str(self.uid_3), 'path': '/disk/folder3'}), None)
        self.assertNotEqual(db.group_links.find_one({'uid': str(self.uid_3), 'path': '/disk/folder2'}), None)
        opts = {
            'uid': self.uid_3,
            'path': '/disk/folder3',
        }
        self.mail_error('list', opts)
        opts = {
            'uid': self.uid_3,
            'path': '/disk/folder2',
        }
        self.mail_ok('list', opts)
        self.assertNotEqual(db.group_links.find_one({'uid': str(self.uid_3), 'path': '/disk/folder2'}), None)
        self.inspect_all()

    def test_move_shared_folder_parent(self):
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        self.assertNotEqual(db.group_links.find_one({'uid': str(self.uid_3), 'path': '/disk/folder2'}), None)
        opts = {
            'uid': self.uid_3,
            'path': '/disk/shared_parent',
        }
        self.json_ok('mkdir', opts)
        self.assertNotEqual(db.group_links.find_one({'uid': str(self.uid_3), 'path': '/disk/folder2'}), None)
        opts = {
            'uid': self.uid_3,
            'path': '/disk/shared_grand_parent',
        }
        self.json_ok('mkdir', opts)

        self.assertNotEqual(db.group_links.find_one({'uid': str(self.uid_3), 'path': '/disk/folder2'}), None)

        opts = {
            'uid': self.uid_3,
            'src': '/disk/folder2',
            'dst': '/disk/shared_parent/folder2',
        }
        self.mail_ok('move', opts)

        self.assertNotEqual(db.group_links.find_one({'uid': str(self.uid_3), 'path': '/disk/shared_parent/folder2'}),
                            None)

        opts = {'uid': self.uid_3,
                'src': '/disk/shared_parent',
                'dst': '/disk/shared_grand_parent/shared_parent'}
        with PushServicesStub() as push_service:
            self.mail_ok('move', opts)
            pushes = [PushServicesStub.parse_send_call(push_args)
                      for push_args in push_service.send.call_args_list]

        diff_push = pushes[0]

        assert diff_push['uid'] == self.uid_3
        op_values = [value['parameters']
                     for value in diff_push['json_payload']['values']
                     if value['tag'] == 'op']
        assert len(op_values) > 2
        assert diff_push['json_payload']['root']['tag'] == 'diff'

        deleted_found = False
        new_found = False
        shared_root_found = False
        shared_root_content_found = False
        for op in op_values:
            assert op.get('resource_type') is not None
            if op.get('key') == '/disk/shared_parent':
                assert op.get('type') == "deleted"
                deleted_found = True
            elif op.get('key') == '/disk/shared_grand_parent/shared_parent':
                assert op.get('with_shared') == 1
                assert op.get('type') == "new"
                new_found = True
            elif op.get('key') == "/disk/shared_grand_parent/shared_parent/folder2":
                assert op.get('type') == "new"
                assert op.get('is_group_root') is None
                assert op.get('shared_rights') == '660'
                shared_root_found = True
            elif op.get('key').startswith('/disk/shared_grand_parent/shared_parent/folder2/'):
                assert op.get('type') == "new"
                shared_root_content_found = True
            elif op.get('key').startswith('/disk/shared_grand_parent/shared_parent/'):
                assert op.get('type') == "new"
            elif op.get('key').startswith('/disk/shared_grand_parent'):
                assert op.get('type') == "changed"

        assert deleted_found
        assert new_found
        assert shared_root_found
        assert shared_root_content_found

        self.assertNotEqual(db.group_links.find_one(
            {'uid': str(self.uid_3), 'path': '/disk/shared_grand_parent/shared_parent/folder2'}), None)

        opts = {
            'uid': self.uid_3,
            'src': '/disk/shared_grand_parent',
            'dst': '/disk/new_shared_grand_parent',
        }
        self.mail_ok('move', opts)

        opts = {
            'uid': self.uid_3,
            'path': '/disk/new_shared_grand_parent',
        }
        folder_list = self.mail_ok('list', opts).find('folder-list')
        self.assertEqual(folder_list.find('folder').find('id').text, '/disk/new_shared_grand_parent')
        self.assertTrue(len(list(folder_list.find('folder').find('folder-list').iterchildren())) > 0)
        self.assertEqual(folder_list.find('folder').find('meta').find('group'), None)
        opts = {
            'uid': self.uid_3,
            'path': '/disk/new_shared_grand_parent/shared_parent/folder2',
        }
        folder_list = self.mail_ok('list', opts).find('folder-list')
        # self.fail(etree.tostring(folder_list, pretty_print=True))
        self.assertEqual(folder_list.find('folder').find('id').text,
                         '/disk/new_shared_grand_parent/shared_parent/folder2')
        self.assertTrue(len(list(folder_list.find('folder').find('folder-list').iterchildren())) > 0)
        self.assertNotEqual(folder_list.find('folder').find('meta').find('group'), None)

        opts = {
            'uid': self.uid_3,
            'src': '/disk/new_shared_grand_parent/shared_parent/folder2',
            'dst': '/disk/folder2',
        }
        self.mail_ok('move', opts)
        opts = {
            'uid': self.uid_3,
            'path': '/disk/new_shared_grand_parent',
        }
        self.mail_ok('rm', opts)
        self.inspect_all()

    def test_move_public_file_to_shared(self):
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        self.upload_file(self.uid_3, '/disk/users_public_file.txt')
        opts = {
            'uid': self.uid_3,
            'path': '/disk/users_public_file.txt',
        }
        self.mail_ok('set_public', opts)

        file_info = self.mail_ok('info', opts)
        public_hash_1 = file_info.find('file').find('meta').find('public_hash').text
        opts = {
            'private_hash': public_hash_1,
        }
        result = self.mail_ok('public_info', opts)
        self.assertTrue(result.find('file') is not None)
        self.assertTrue(result.find('user') is not None)
        opts = {
            'uid': self.uid_3,
            'src': '/disk/users_public_file.txt',
            'dst': '/disk/folder2/users_public_file.txt',
        }
        self.mail_ok('move', opts)
        opts = {
            'uid': self.uid_3,
            'path': '/disk/folder2/users_public_file.txt',
        }
        file_info = self.mail_ok('info', opts)
        public_hash_2 = file_info.find('file').find('meta').find('public_hash').text
        self.assertEqual(public_hash_1, public_hash_2)
        opts = {
            'private_hash': public_hash_2,
        }
        self.mail_ok('public_info', opts)
        # =======================================================================
        #
        opts = {
            'uid': self.uid_3,
            'src': '/disk/folder2/users_public_file.txt',
            'dst': '/disk/users_public_file.txt',
        }
        self.mail_ok('move', opts)
        opts = {
            'uid': self.uid_3,
            'path': '/disk/users_public_file.txt',
        }
        file_info = self.mail_ok('info', opts)
        public_hash_3 = file_info.find('file').find('meta').find('public_hash').text
        self.assertEqual(public_hash_2, public_hash_3)
        opts = {
            'private_hash': public_hash_3,
        }
        self.mail_ok('public_info', opts)
        #=======================================================================
        #
        opts = {
            'uid': self.uid_3,
            'src': '/disk/users_public_file.txt',
            'dst': '/disk/folder2/users_public_file.txt',
        }
        self.mail_ok('move', opts)
        opts = {
            'uid': self.uid_3,
            'path': '/disk/folder2/users_public_file.txt',
        }
        file_info = self.mail_ok('info', opts)
        public_hash_4 = file_info.find('file').find('meta').find('public_hash').text
        self.assertEqual(public_hash_3, public_hash_4)
        opts = {
            'private_hash': public_hash_3,
        }
        self.mail_ok('public_info', opts)
        #=======================================================================
        #
        opts = {
            'uid': self.uid,
            'src': '/disk/new_folder/folder2/users_public_file.txt',
            'dst': '/disk/users_public_file.txt',
        }
        self.mail_ok('move', opts)
        opts = {
            'uid': self.uid,
            'path': '/disk/users_public_file.txt',
        }
        file_info = self.mail_ok('info', opts)
        public_hash_5 = file_info.find('file').find('meta').find('public_hash').text
        self.assertEqual(public_hash_4, public_hash_5)
        opts = {
            'private_hash': public_hash_3,
        }
        self.mail_ok('public_info', opts)
        #=======================================================================
        opts = {
            'uid': self.uid,
            'path': '/disk/users_public_file.txt',
        }
        file_info = self.mail_ok('rm', opts)
        self.inspect_all()

    def test_move_public_file_to_group(self):
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        self.upload_file(self.uid, '/disk/owners_public_file.txt')
        opts = {
            'uid': self.uid,
            'path': '/disk/owners_public_file.txt',
        }
        self.mail_ok('set_public', opts)

        file_info = self.mail_ok('info', opts)
        public_hash_1 = file_info.find('file').find('meta').find('public_hash').text
        opts = {
            'uid': self.uid,
            'src': '/disk/owners_public_file.txt',
            'dst': '/disk/new_folder/folder2/owners_public_file.txt',
        }
        self.mail_ok('move', opts)
        opts = {
            'uid': self.uid,
            'path': '/disk/new_folder/folder2/owners_public_file.txt',
        }
        file_info = self.mail_ok('info', opts)
        public_hash_2 = file_info.find('file').find('meta').find('public_hash').text
        self.assertEqual(public_hash_1, public_hash_2)
        opts = {
            'private_hash': public_hash_2,
        }
        self.mail_ok('public_info', opts)
        # =======================================================================
        #
        opts = {
            'uid': self.uid,
            'src': '/disk/new_folder/folder2/owners_public_file.txt',
            'dst': '/disk/owners_public_file.txt',
        }
        self.mail_ok('move', opts)
        opts = {
            'uid': self.uid,
            'path': '/disk/owners_public_file.txt',
        }
        file_info = self.mail_ok('info', opts)
        public_hash_3 = file_info.find('file').find('meta').find('public_hash').text
        self.assertEqual(public_hash_2, public_hash_3)
        opts = {
            'private_hash': public_hash_3,
        }
        self.mail_ok('public_info', opts)
        #=======================================================================
        #
        opts = {
            'uid': self.uid,
            'src': '/disk/owners_public_file.txt',
            'dst': '/disk/new_folder/folder2/owners_public_file.txt',
        }
        self.mail_ok('move', opts)
        opts = {
            'uid': self.uid,
            'path': '/disk/new_folder/folder2/owners_public_file.txt',
        }
        file_info = self.mail_ok('info', opts)
        public_hash_4 = file_info.find('file').find('meta').find('public_hash').text
        self.assertEqual(public_hash_3, public_hash_4)
        opts = {
            'private_hash': public_hash_4,
        }
        self.mail_ok('public_info', opts)
        #=======================================================================
        #
        opts = {
            'uid': self.uid_3,
            'src': '/disk/folder2/owners_public_file.txt',
            'dst': '/disk/owners_public_file.txt',
        }
        self.mail_ok('move', opts)
        opts = {
            'uid': self.uid_3,
            'path': '/disk/owners_public_file.txt',
        }
        file_info = self.mail_ok('info', opts)
        public_hash_5 = file_info.find('file').find('meta').find('public_hash').text
        self.assertEqual(public_hash_4, public_hash_5)
        opts = {
            'private_hash': public_hash_5,
        }
        self.mail_ok('public_info', opts)
        #=======================================================================
        opts = {
            'uid': self.uid_3,
            'path': '/disk/owners_public_file.txt',
        }
        file_info = self.mail_ok('rm', opts)
        #        self.fail(etree.tostring(result, pretty_print=True))
        self.inspect_all()
