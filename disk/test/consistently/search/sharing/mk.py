# -*- coding: utf-8 -*-
import re
import urlparse

from lxml import etree

from test.common.sharing import SharingWithSearchTestCase
from test.base_suit import set_up_open_url, tear_down_open_url

import mpfs.engine.process

from mpfs.common.util import from_json
from mpfs.core.services.search_service import DiskSearch
from mpfs.core.services.index_service import SearchIndexer
from mpfs.core.filesystem.dao.legacy import CollectionRoutedDatabase


db = CollectionRoutedDatabase()


class MkFileMkDirSharingTestCase(SharingWithSearchTestCase):

    def setup_method(self, method):
        super(MkFileMkDirSharingTestCase, self).setup_method(method)

        for uid in (self.uid_3, self.uid, self.uid_1):
            docs = []
            for doc_id in DiskSearch().get_all_documents_for_user(uid):
                docs.append({
                    'action': 'delete',
                    "file_id": doc_id,
                    "uid": uid,
                    'version': 999999999999999999,
                    'operation': 'rm',
                })
            SearchIndexer().push_change(docs)

        self.json_ok('user_init', {'uid': self.uid_1})
        self.json_ok('user_init', {'uid': self.uid_3})
        self.make_dirs()

    def test_make_folder_in_shared(self):
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        opts = {
            'uid': self.uid_3,
            'path': '/disk',
            'meta': '',
        }
        diff_result = self.desktop('diff', opts)
        version_before_user = diff_result['version']
        opts = {
            'uid': self.uid,
            'path': '/disk',
            'meta': '',
        }
        diff_result = self.desktop('diff', opts)
        version_before_owner = diff_result['version']
        opts = {'uid': self.uid_3, 'path': '/disk/folder2/folder31'}
        xiva_requests = set_up_open_url()
        self.json_ok('mkdir', opts)
        info_result = self.json_ok('info', {'uid': self.uid_3, 'path': '/disk/folder2/folder31', 'meta': ''})
        folder_rid = info_result['meta']['resource_id']
        assert info_result['meta']['group'] is not None
        tear_down_open_url()
        opts = {
            'uid': self.uid_3,
            'path': '/disk',
            'meta': '',
            'version': version_before_user,
        }
        diff_result = self.json_ok('diff', opts)
        version_after_user = diff_result['version']
        found = False
        for element in diff_result['result']:
            if element['key'] == '/disk/folder2/folder31':
                self.assertEqual(element['op'], 'new')
                found = True
        self.assertTrue(found)
        opts = {
            'uid': self.uid,
            'path': '/disk',
            'meta': '',
            'version': version_before_owner,
        }
        diff_result = self.desktop('diff', opts)
        version_after_owner = diff_result['version']
        self.assertTrue(version_before_user < version_after_user)
        self.assertTrue(version_before_owner < version_after_owner)
        found = False
        for element in diff_result['result']:
            if element['key'] == '/disk/new_folder/folder2/folder31':
                self.assertEqual(element['op'], 'new')
                found = True
        self.assertTrue(found)
        opts = {'uid': self.uid, 'path': '/disk/new_folder/folder2'}
        listing_result = self.mail_ok('list', opts)
        self.assertEqual(listing_result.find('folder-list').find('folder').find('meta').find('with_shared'), None)

        diffs = {
             self.uid_3: {'folder': '/disk/folder2/', 'key': '/disk/folder2/folder31'},
             self.uid: {'folder': '/disk/new_folder/folder2/', 'key': '/disk/new_folder/folder2/folder31'},
        }
        url = 'http://localhost/service/echo'

        search_notify_found_user = False
        search_notify_found_owner = False
        for k, v in xiva_requests.iteritems():
            if k.startswith(url + '?uid='):
                uid = re.search(url + '\?uid=(\d+)', k).group(1)
                self.assertTrue(uid in diffs, uid)
                for each in v:
                    optag = etree.fromstring(each['pure_data']).find('op')
                    self.assertEqual(optag.get('type'), 'new')
                    self.assertNotEqual(optag.get('resource_type'), None)
                    for k in ('folder', 'key'):
                        self.assertEqual(optag.get(k), diffs[uid][k])
                diffs.pop(uid)
            elif k.startswith(self.search_url):
                qs = urlparse.parse_qs(urlparse.urlparse(k).query)
                if qs['action'][0] != 'mkdir':
                    continue
                search_notify_found_owner |= qs['prefix'][0] == self.uid and folder_rid == qs['resource_id'][0]
                search_notify_found_user |= qs['prefix'][0] == self.uid_3 and folder_rid == qs['resource_id'][0]
        self.assertTrue(search_notify_found_user)
        self.assertTrue(search_notify_found_owner)

        failed = True
        for child_folder in listing_result.find('folder-list').find('folder').find('folder-list').iterchildren():
            if child_folder.tag == 'folder':
                self.assertNotEqual(child_folder, None)
                self.assertNotEqual(child_folder.find('id'), None)
                if child_folder.find('id').text == '/disk/new_folder/folder2/folder31':
                    failed = False
        self.assertFalse(failed)

        opts = {'uid': self.uid_3, 'path': '/disk/folder2'}
        listing_result = self.mail_ok('list', opts)
        failed = True
        for child_folder in listing_result.find('folder-list').find('folder').find('folder-list').iterchildren():
            if child_folder.tag == 'folder':
                self.assertNotEqual(child_folder, None)
                self.assertNotEqual(child_folder.find('id'), None)
                if child_folder.find('id').text == '/disk/folder2/folder31':
                    failed = False
        self.assertFalse(failed)
        opts = {'uid': self.uid_3, 'path': '/disk/folder2/folder31'}
        listing_result = self.mail_ok('list', opts)
        self.assertFalse(listing_result.find('folder-list').find('folder') is None)
        self.assertFalse(listing_result.find('folder-list').find('folder').find('id') is None)
        self.assertEqual(listing_result.find('folder-list').find('folder').find('id').text, '/disk/folder2/folder31')
        self.inspect_all()

    def test_make_file_in_shared(self):
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        disk_size_before = db.disk_info.find_one({'uid': self.uid_3, 'key': '/total_size'})['data']
        disk_size_before_owner = db.disk_info.find_one({'uid': self.uid, 'key': '/total_size'})['data']
        opts = {
            'uid': self.uid_3,
            'path': '/disk/folder2',
        }
        result = self.mail_ok('info', opts)
        group_size_before = int(result.find('folder').find('meta').find('group').find('size').text)
        open_url_data = {}
        file_size = self.upload_file(self.uid_3, '/disk/folder2/file31', connection_id='12345', open_url_data=open_url_data)
        file_rid = self.json_ok('info', {'uid': self.uid_3, 'path': '/disk/folder2/file31', 'meta': 'resource_id'})['meta']['resource_id']
        self.inspect_all()

        owner_found_new = False
        user_found_new = False
        search_notify_found_user = False
        search_notify_found_owner = False
        for k, v in open_url_data.iteritems():
            if k.startswith('http://localhost/service/echo?uid='):
                notified_uid = re.search('http://localhost/service/echo\?uid=(\d+)', k).group(1)
                for each in v:
                    optag = etree.fromstring(each['pure_data'])
                    if optag.tag == 'diff':
                        if notified_uid == str(self.uid):
                            self.assertEqual(len(list(optag.iterfind('op'))), 1)
                            connection_id = re.search('http://localhost/service/echo\?.*connection_id=(.*)', k)
                            self.assertEqual(connection_id, None)
                            self.assertEqual(optag.find('op').get('key'), '/disk/new_folder/folder2/file31')
                            self.assertEqual(optag.find('op').get('folder'), '/disk/new_folder/folder2/')
                            self.assertTrue(optag.find('op').get('type') in ("new", "changed"))
                            self.assertEqual(optag.find('op').get('external_setprop'), None)
                            self.assertNotEqual(optag.find('op').get('resource_type'), None)
                            owner_found_new = True
                        elif notified_uid == str(self.uid_3):
                            self.assertEqual(len(list(optag.iterfind('op'))), 1)
                            self.assertEqual(optag.find('op').get('key'), '/disk/folder2/file31')
                            self.assertEqual(optag.find('op').get('folder'), '/disk/folder2/')
                            self.assertTrue(optag.find('op').get('type') in ("new", "changed"))
                            self.assertEqual(optag.find('op').get('external_setprop'), None)
                            self.assertNotEqual(optag.find('op').get('resource_type'), None)
                            user_found_new = True
                        else:
                            self.fail()
            elif k.startswith(self.search_url):
                qs = urlparse.parse_qs(urlparse.urlparse(k).query)
                if qs['action'][0] != 'patch_file':
                    continue
                search_notify_found_owner |= qs['prefix'][0] == self.uid and file_rid == qs['resource_id'][0]
                search_notify_found_user |= qs['prefix'][0] == self.uid_3 and file_rid == qs['resource_id'][0]
        self.assertTrue(owner_found_new)
        self.assertTrue(user_found_new)
        self.assertTrue(search_notify_found_user)
        self.assertTrue(search_notify_found_owner)

        opts = {'uid': self.uid_3, 'path': '/disk/folder2/file31'}
        self.assertEqual(db.user_data.find_one({'uid': str(self.uid_3), 'key': '/disk/folder2/file31'}), None)
        self.mail_ok('info', opts)
        opts = {'uid' : self.uid, 'path' : '/disk/new_folder/folder2/file31'}
        self.mail_ok('info', opts)
        file_data = db.user_data.find_one({'uid': str(self.uid), 'path': '/disk/new_folder/folder2/file31'})
        self.assertNotEqual(file_data, None)
        opts = {
            'uid': self.uid_3,
            'path': '/disk/folder2',
        }
        result = self.mail_ok('info', opts)
        group_size_after = int(result.find('folder').find('meta').find('group').find('size').text)
        self.assertNotEqual(group_size_before, group_size_after)
        self.assertNotEqual(file_size, group_size_after)
        self.assertEqual(group_size_before + file_size, group_size_after)
        disk_size_after = db.disk_info.find_one({'uid' : str(self.uid_3), 'key' : '/total_size'})['data']
        self.assertEqual(disk_size_before, disk_size_after)
        disk_size_after_owner = db.disk_info.find_one({'uid' : str(self.uid), 'key' : '/total_size'})['data']
        self.assertNotEqual(disk_size_before_owner, disk_size_after_owner)
        self.assertEqual(disk_size_before_owner + file_size, disk_size_after_owner)

        user_version = int(db.user_index.find_one({'_id' : str(self.uid_3)})['version'])
        owner_version = int(db.user_index.find_one({'_id' : str(self.uid)})['version'])
        self.assertEqual(user_version, owner_version)

        # Create folder and file
        opts = {
            'uid': self.uid_3,
            'path': '/disk/folder2/folder_with_file'
        }
        self.json_ok('mkdir', opts)
        self.upload_file(self.uid, '/disk/new_folder/folder2/folder_with_file/owners_file')

        opts = {'uid': self.uid, 'path': '/disk/new_folder/folder2/file31'}
        self.mail_ok('info', opts)
        opts = {'uid': self.uid_3, 'path': '/disk/folder2/file31'}
        self.mail_ok('info', opts)

        self.upload_file(self.uid_3, '/disk/folder2', ok=False)
        self.inspect_all()
