# -*- coding: utf-8 -*-
import re
import urlparse

import mock

from test.common.sharing import SharingWithSearchTestCase
from test.base_suit import set_up_open_url, tear_down_open_url

import mpfs.engine.process

from mpfs.common.util import from_json
from test.helpers.stubs.services import PushServicesStub
from mpfs.core.filesystem.dao.legacy import CollectionRoutedDatabase


db = CollectionRoutedDatabase()


class TrashTwoSharingTestCase(SharingWithSearchTestCase):

    def setup_method(self, method):
        super(TrashTwoSharingTestCase, self).setup_method(method)
        self.json_ok('user_init', {'uid': self.uid_1})
        self.xiva_subscribe(self.uid_1)
        self.json_ok('user_init', {'uid': self.uid_3})
        self.xiva_subscribe(self.uid_3)
        self.make_dirs()

    def test_trash_append_user(self):
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        self.upload_file(self.uid, '/disk/new_folder/folder2/file_for_trash')
        opts = {'uid': self.uid, 'path': '/disk/new_folder/folder2/file_for_trash'}
        self.mail_ok('info', opts)

        opts = {
            'uid': self.uid_3,
            'path': '/disk/folder2/file_for_trash',
            'connection_id': '12345',
        }

        open_url_data = set_up_open_url()
        with PushServicesStub() as push_service:
            with mock.patch('mpfs.core.address.Address.add_trash_suffix'):
                self.mail_ok('async_trash_append', opts)
            tear_down_open_url()
            pushes = [PushServicesStub.parse_send_call(push_args)
                      for push_args in push_service.send.call_args_list]

        owner_file_rid = self.json_ok('info', {'uid': self.uid, 'path': '/trash/file_for_trash', 'meta': 'resource_id'})['meta']['resource_id']
        guest_file_rid = self.json_ok('info', {'uid': self.uid_3, 'path': '/trash/file_for_trash', 'meta': 'resource_id'})['meta']['resource_id']

        actor_push = pushes[0]
        owner_push = pushes[1]

        assert actor_push['uid'] == self.uid_3
        assert actor_push['connection_id'] == '12345'
        op_values = [value['parameters']
                     for value in actor_push['json_payload']['values']
                     if value['tag'] == 'op']
        assert len(op_values) == 2

        user_found_deleted = False
        user_found_new = False
        for op in op_values:
            assert op.get('resource_type') is not None
            if op.get('key') == '/disk/folder2/file_for_trash':
                assert op.get('folder') == '/disk/folder2/'
                assert op.get('type') == "deleted"
                user_found_deleted = True
            elif op.get('key') == '/trash/file_for_trash':
                assert op.get('folder') == '/trash/'
                assert op.get('type') == "new"
                user_found_new = True
        assert user_found_deleted
        assert user_found_new

        assert owner_push['uid'] == self.uid
        assert owner_push['connection_id'] is None
        op_values = [value['parameters']
                     for value in owner_push['json_payload']['values']
                     if value['tag'] == 'op']
        assert len(op_values) == 2
        owner_found_deleted = False
        owner_found_new = False
        for op in op_values:
            assert op.get('resource_type') is not None
            if op.get('key') == '/disk/new_folder/folder2/file_for_trash':
                assert op.get('folder') == '/disk/new_folder/folder2/'
                assert op.get('type') == "deleted"
                owner_found_deleted = True
            elif op.get('key') == '/trash/file_for_trash':
                assert op.get('folder') == '/trash/'
                assert op.get('type') == "new"
                owner_found_new = True
        assert owner_found_deleted
        assert owner_found_new

        search_notify_found_user = False
        search_notify_found_owner = False
        search_notify_found_owner_trash = False
        pushes_in_indexer_counter = 0
        for k, v in open_url_data.iteritems():
            if not k.startswith(self.search_url):
                continue
            qs = urlparse.parse_qs(urlparse.urlparse(k).query)
            search_notify_found_user |= (qs['action'][0] == 'trash_append' and qs['prefix'][0] == self.uid_3
                                         and qs['resource_id'][0] == guest_file_rid)
            search_notify_found_owner_trash |= (qs['action'][0] == 'trash_append' and qs['prefix'][0] == self.uid
                                                and qs['resource_id'][0] == owner_file_rid)
            search_notify_found_owner |= (qs['action'][0] == 'trash_append' and qs['prefix'][0] == self.uid
                                          and qs['resource_id'][0] == owner_file_rid)
            pushes_in_indexer_counter += 1
        assert pushes_in_indexer_counter == 3
        self.assertTrue(search_notify_found_user)
        #=======================================================================
        # TODO: fix
        self.assertTrue(search_notify_found_owner)
        #=======================================================================
        self.assertTrue(search_notify_found_owner_trash)

        opts = {'uid': self.uid_3, 'path': '/disk/folder2/folder_for_trash'}
        self.json_ok('mkdir', opts)
        opts = {'uid': self.uid_3, 'path': '/disk/folder2/folder_for_trash/inner_folder'}
        self.json_ok('mkdir', opts)
        self.upload_file(self.uid_3, '/disk/folder2/folder_for_trash/file_0')
        self.upload_file(self.uid_3, '/disk/folder2/folder_for_trash/inner_folder/file_1')

        self.assertNotEqual(
            self._count_path_startswith(db.user_data, self.uid, '/disk/new_folder/folder2/folder_for_trash'),
            0)

        opts = {'uid': self.uid_3, 'path': '/disk/folder2/folder_for_trash'}
        self.mail_ok('trash_append', opts)

        self.assertEqual(
            self._count_path_startswith(db.user_data, self.uid, '/disk/new_folder/folder2/folder_for_trash'),
            0)
        self.assertNotEqual(
            self._count_path_startswith(db.trash, self.uid_3, '/trash/folder_for_trash'),
            0)

        self.inspect_all()

    def _count_path_startswith(self, coll, uid, path):
        cur = coll.find({'uid': str(uid)})
        count = 0
        for i in cur:
            if i['key'].startswith(path):
                count += 1
        return count
