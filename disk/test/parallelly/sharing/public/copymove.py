# -*- coding: utf-8 -*-
import re
import pytest

from lxml import etree

from test.common.sharing import CommonSharingMethods
from test.base_suit import set_up_open_url, tear_down_open_url

import mpfs.engine.process
from mpfs.core.filesystem.dao.legacy import CollectionRoutedDatabase


db = CollectionRoutedDatabase()


class PublicCopyMoveSharingTestCase(CommonSharingMethods):
    def setup_method(self, method):
        super(PublicCopyMoveSharingTestCase, self).setup_method(method)
        self.json_ok('user_init', {'uid': self.uid_1})
        self.json_ok('user_init', {'uid': self.uid_3})
        self.make_dirs()

    @pytest.mark.skipif(True, reason='https://st.yandex-team.ru/CHEMODAN-32396')
    def test_public_copy(self):
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid, rights=660)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        self.publicate_resource(self.uid, '/disk/new_folder/folder2')

        spec = {
            'uid': self.uid_3,
            'data.tgt': '%s:/disk/folder2' % self.uid_3,
        }
        self.assertNotEqual(db.link_data.find_one(spec), None)
        spec = {
            'uid': self.uid,
            'data.tgt': '%s:/disk/new_folder/folder2' % self.uid,
        }
        self.assertNotEqual(db.link_data.find_one(spec), None)
        opts = {
            'uid': self.uid_3,
            'src': '/disk/folder2',
            'dst': '/disk/folder2_renamed'
        }
        self.json_ok('async_move', opts)
        spec = {
            'uid': self.uid_3,
            'data.tgt': '%s:/disk/folder2' % self.uid_3,
        }
        self.assertEqual(db.link_data.find_one(spec), None)
        spec = {
            'uid': self.uid_3,
            'data.tgt': '%s:/disk/folder2_renamed' % self.uid_3,
        }
        self.assertNotEqual(db.link_data.find_one(spec), None)
        spec = {
            'uid': self.uid,
            'data.tgt': '%s:/disk/new_folder/folder2' % self.uid,
        }
        self.assertNotEqual(db.link_data.find_one(spec), None)
        opts = {
            'uid': self.uid_3,
            'path': '/disk/folder2_renamed',
            'meta': '',
        }
        public_hash = self.json_ok('info', opts)['meta']['public_hash']
        opts = {
            'uid': self.uid_1,
            'private_hash': public_hash,
            'connection_id': ''
        }
        self.json_ok('async_public_copy', opts)
        opts = {
            'uid': self.uid_1,
            'path': '/disk/Загрузки',
        }
        listing = self.json_ok('list', opts)
        self.assertEqual(len(listing), 2)
        opts = {
            'uid': self.uid_3,
            'src': '/disk/folder2_renamed',
            'dst': '/disk/folder2'
        }
        self.json_ok('async_move', opts)

    def test_rename_shared_folder_rw(self):
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid, rights=660)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        self.publicate_resource(self.uid, '/disk/new_folder/folder2')

        opts = {'uid': self.uid_3, 'path': '/disk/folder2'}
        folder_list = self.mail_ok('list', opts).find('folder-list')
        # self.fail(etree.tostring(folder_list, pretty_print=True))
        self.assertNotEqual(folder_list.find('folder').find('meta').find('group'), None)
        self.assertEqual(folder_list.find('folder').find('id').text, '/disk/folder2')
        self.assertEqual(len(list(folder_list.find('folder').find('folder-list').iterchildren())), 2)

        self.assertNotEqual(db.user_data.find_one({'uid': str(self.uid_3), 'key': '/disk/folder2'}), None)
        self.assertNotEqual(db.group_links.find_one({'uid': str(self.uid_3), 'path': '/disk/folder2'}), None)
        self.assertEqual(db.group_links.find_one({'uid': str(self.uid_3), 'path': '/disk/folder2_moved'}), None)
        open_url_data = set_up_open_url()
        opts = {
            'uid': self.uid_3,
            'src': '/disk/folder2',
            'dst': '/disk/folder2_moved',
            'force': 1,
        }
        self.mail_ok('async_move', opts)
        tear_down_open_url()
        opts = {'uid': self.uid_3, 'path': '/disk/folder2_moved'}
        folder_list = self.mail_ok('list', opts).find('folder-list')
        self.assertNotEqual(folder_list.find('folder').find('meta').find('group'), None)
        self.assertEqual(folder_list.find('folder').find('id').text, '/disk/folder2_moved')
        self.assertEqual(len(list(folder_list.find('folder').find('folder-list').iterchildren())), 2)

        self.assertEqual(db.user_data.find_one({'uid': str(self.uid_3), 'key': '/disk/folder2'}), None)
        self.assertNotEqual(db.user_data.find_one({'uid': str(self.uid_3), 'key': '/disk/folder2_moved'}), None)
        self.assertEqual(db.group_links.find_one({'uid': str(self.uid_3), 'path': '/disk/folder2'}), None)
        self.assertNotEqual(db.group_links.find_one({'uid': str(self.uid_3), 'path': '/disk/folder2_moved'}), None)
        user_found_deleted = False
        user_found_new = False
        #        self.fail(open_url_data)
        for k, v in open_url_data.iteritems():
            if k.startswith('http://localhost/service/echo?uid='):
                notified_uid = re.search('http://localhost/service/echo\?uid=(\d+)', k).group(1)
                for each in v:
                    optag = etree.fromstring(each['pure_data'])
                    if optag.tag == 'diff':
                        if notified_uid == str(self.uid_3):
                            #                            self.fail(etree.tostring(optag, pretty_print=True))
                            self.assertEqual(len(list(optag.iterfind('op'))), 7)
                            for op in optag.iterfind('op'):
                                self.assertNotEqual(op.get('resource_type'), None)
                                if op.get('key') == '/disk/folder2_moved':
                                    self.assertEqual(op.get('folder'), '/disk/')
                                    self.assertEqual(op.get('type'), "new")
                                    user_found_deleted = True
                                elif op.get('key') == '/disk/folder2':
                                    self.assertEqual(op.get('folder'), '/disk/')
                                    self.assertEqual(op.get('type'), "deleted")
                                    user_found_new = True
                                else:
                                    self.assertTrue(op.get('key').startswith('/disk/folder2'), op.get('key'))
                        else:
                            self.fail()

        self.assertTrue(user_found_deleted)
        self.assertTrue(user_found_new)
        self.inspect_all()
