# -*- coding: utf-8 -*-
import urlparse

from test.common.sharing import SharingWithSearchTestCase
from test.base_suit import set_up_open_url, tear_down_open_url

import mpfs.engine.process

from mpfs.common.util import from_json
from mpfs.core.filesystem.dao.legacy import CollectionRoutedDatabase


db = CollectionRoutedDatabase()


class MoveTwoSharingTestCase(SharingWithSearchTestCase):

    def setup_method(self, method):
        super(MoveTwoSharingTestCase, self).setup_method(method)
        self.json_ok('user_init', {'uid': self.uid_1})
        self.xiva_subscribe(self.uid_1)
        self.json_ok('user_init', {'uid': self.uid_3})
        self.xiva_subscribe(self.uid_3)
        self.make_dirs()

    def test_move_in_shared_folder(self):
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        self.upload_file(self.uid_3, '/disk/folder2/fileMove')
        opts = {'uid': self.uid_3, 'path': '/disk/folder2/fileMove'}
        self.mail_ok('info', opts)
        opts = {
            'uid': self.uid_3,
            'src': '/disk/folder2/fileMove',
            'dst': '/disk/folder2/movedFile',
            'force': 1,
        }
        open_url_data = set_up_open_url()
        self.mail_ok('move', opts)
        tear_down_open_url()
        file_rid = self.json_ok('info', {'uid': self.uid_3, 'path': '/disk/folder2/movedFile','meta': 'resource_id'})['meta']['resource_id']
        # =======================================================================
        #
        search_notify_found_owner = False
        search_notify_found_user = False
        for k, v in open_url_data.iteritems():
            if not k.startswith(self.search_url):
                continue
            qs = urlparse.parse_qs(urlparse.urlparse(k).query)
            if qs['action'][0] != 'move_resource':
                continue
            search_notify_found_owner |= qs['prefix'][0] == self.uid and qs['resource_id'][0] == file_rid
            search_notify_found_user |= qs['prefix'][0] == self.uid_3 and qs['resource_id'][0] == file_rid

        self.assertTrue(search_notify_found_owner)
        self.assertTrue(search_notify_found_user)

        opts = {'uid': self.uid_3, 'path': '/disk/folder2/movedFile'}
        self.mail_ok('info', opts)

        self.assertEqual(db.user_data.find_one({'uid': str(self.uid_3), 'key': '/disk/folder2/movedFile'}), None)

        opts = {'uid': self.uid, 'path': '/disk/new_folder/folder2/movedFile'}
        self.mail_ok('info', opts)

        opts = {'uid': self.uid_3, 'path': '/disk/folder2/fileMove'}
        self.mail_error('info', opts)

        opts = {'uid': self.uid, 'path': '/disk/new_folder/folder2/fileMove'}
        self.mail_error('info', opts)
        self.inspect_all()
