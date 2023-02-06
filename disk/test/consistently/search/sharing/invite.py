# -*- coding: utf-8 -*-
import re
import urlparse

from lxml import etree

import mpfs.engine.process
from mpfs.common.util import from_json
from mpfs.core.address import ResourceId
from mpfs.core.services.search_service import DiskSearch
from test.base_suit import set_up_open_url, tear_down_open_url
from test.common.sharing import CommonSharingMethods
from test.helpers.stubs.services import SearchIndexerStub
from test.helpers.stubs.manager import StubsManager
from mpfs.core.filesystem.dao.legacy import CollectionRoutedDatabase


db = CollectionRoutedDatabase()


class InviteShareTestCase(CommonSharingMethods):

    stubs_manager = StubsManager(class_stubs=set(StubsManager.DEFAULT_CLASS_STUBS) - {SearchIndexerStub})

    def setup_method(self, method):
        super(InviteShareTestCase, self).setup_method(method)
        self.json_ok('user_init', {'uid': self.uid_1})
        self.xiva_subscribe(self.uid_1)
        self.json_ok('user_init', {'uid': self.uid_3})
        self.xiva_subscribe(self.uid_3)
        self.make_dirs()

    def teardown_method(self, method):
        DiskSearch().delete(self.uid)
        super(InviteShareTestCase, self).teardown_method(method)

    def test_activate_invite(self):
        gid = self.create_group()

        hsh = self.invite_user(uid=self.uid_1, email=self.email_1, ext_gid=gid)
        self.activate_invite(uid=self.uid_1, hash=hsh)

        rights = 660
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid, rights=rights)

        disk_size_before = db.disk_info.find_one({'uid': str(self.uid_3), 'key': '/total_size'})['data']
        disk_limit = db.disk_info.find_one({'uid': str(self.uid_3), 'key': '/limit'})['data']
        group_id = db.group_invites.find_one({'_id': hsh})['gid']
        group_size = db.groups.find_one({'_id': group_id})['size']
        new_limit = disk_size_before + group_size + 1000

        for link in db.group_links.find({'uid': str(self.uid_3)}):
            new_limit += db.groups.find_one({'_id': link['gid']})['size']
        db.disk_info.update({'uid': str(self.uid_3), 'key': '/limit'}, {'$set': {'data': new_limit}})
        opts = {
            'uid': self.uid_3,
            'path': '/disk',
            'meta': '',
        }
        diff_result = self.desktop('diff', opts)
        version_before = diff_result['version']

        args = {
            'hash': hsh,
            'uid': self.uid_3,
        }
        xiva_requests = set_up_open_url()
        folder_info = self.mail_ok('share_activate_invite', args)
        tear_down_open_url()

        #        self.fail(etree.tostring(folder_info.find('folder'), pretty_print=True))
        self.assertTrue(re.match("^/disk/folder2$|/disk/folder2 \d+$", folder_info.find('folder').find('id').text))
        self.assertNotEqual(folder_info.find('folder').find('meta').find('group'), None)
        folder_fid = self.json_ok('info', {'uid': self.uid_3, 'path': '/disk/folder2', 'meta': 'file_id'})['meta']['file_id']

        db.disk_info.update({'uid': str(self.uid_3), 'key': '/limit'}, {'$set': {'data': disk_limit}})

        #=======================================================================
        # Diff with version
        opts = {
            'uid': self.uid_3,
            'path': '/disk',
            'version': version_before,
            'meta': '',
        }
        diff_result = self.desktop('diff', opts)
        all_resource_ids = {ResourceId(self.uid, x['fid']).serialize() for x in diff_result['result'] if x['key'] != '/disk/folder2'} | {ResourceId(self.uid_3, folder_fid).serialize()}
        try:
            version_after = diff_result['version']
        except:
            opts = {
                'uid': self.uid_3,
                'path': '/disk',
            }
            diff_result = self.desktop('diff', opts)
            version_after = diff_result['version']
        else:
            self.assertNotEqual(version_before, version_after)
            self.assertTrue(len(diff_result['result']) > 1)
            self.assertEqual(diff_result['result'][0]['op'], 'new')
            self.assertTrue(re.match("^/disk/folder2$|/disk/folder2 \d+$", diff_result['result'][0]['key']))
            self.assertEqual(diff_result['result'][0]['type'], 'dir')
            self.assertEqual(diff_result['result'][0]['rights'], rights)
            self.assertEqual(diff_result['result'][0]['shared'], 'group')
        #=======================================================================
        # Diff without version
        opts = {
            'uid': self.uid_3,
            'path': '/disk',
            'meta': '',
        }
        diff_result = self.desktop('diff', opts)
        version_after = diff_result['version']
        self.assertNotEqual(version_before, version_after)
        self.assertTrue(len(diff_result['result']) > 1)
        attached_diff = filter(lambda x: x['key'].startswith('/disk/folder2'), diff_result['result'])
        self.assertTrue(len(attached_diff) > 4)
        for each in diff_result['result']:
            if re.match("^/disk/folder2$|/disk/folder2 \d+$", each['key']):
                if each.get('rights') or each.get('shared'):
                    self.assertEqual(each['op'], 'new')
                    self.assertEqual(each['type'], 'dir')
                    self.assertEqual(each['rights'], rights)
                    self.assertEqual(each['shared'], 'group')

        args = {
            'gid': gid,
            'uid': self.uid,
        }
        approved_count = 0
        for each in self.mail_ok('share_users_in_group', args).find('users').iterfind('user'):
            if each.find('status').text in ('owner', 'approved'):
                approved_count += 1
        notify_send_owner = False
        notify_send_user = False
        notify_send_actor = False
        search_notify_found_user = False
        indexed_resource_ids = set()
        for k, v in xiva_requests.iteritems():
            if k.startswith('http://localhost/service/echo?uid='):
                invited_uid = re.search('http://localhost/service/echo\?uid=(\d+)', k).group(1)
                for each in v:
                    share_tag = etree.fromstring(each['pure_data'])
                    if share_tag.tag == 'share' and share_tag.get('type') == 'invite_approved':
                        if share_tag.get('for') == 'owner':
                            self.assertEqual(invited_uid, str(self.uid))
                            self.assertEqual(share_tag.find('user').get('uid'), str(self.uid_3))
                            self.assertEqual(share_tag.find('user').get('name'), "Vasily P.")
                            self.assertEqual(share_tag.find('folder').get('path'), "/disk/new_folder/folder2/")
                            self.assertEqual(share_tag.find('folder').get('name'), 'folder2')
                            self.assertEqual(share_tag.find('folder').get('gid'), gid)
                            notify_send_owner = True
                        elif share_tag.get('for') == 'actor':
                            self.assertEqual(invited_uid, str(self.uid_3))
                            self.assertTrue(
                                re.match("^/disk/folder2/$|/disk/folder2 \d+/$", share_tag.find('folder').get('path')))
                            old_ver = share_tag.get('old')
                            new_ver = share_tag.get('new')
                            self.assertNotEqual(old_ver, None)
                            self.assertNotEqual(new_ver, None)
                            self.assertTrue(int(old_ver) <= int(new_ver))
                            notify_send_actor = True
                        elif share_tag.get('for') == 'user':
                            self.assertEqual(invited_uid, str(self.uid_1))
                            self.assertEqual(share_tag.find('user').get('uid'), str(self.uid_3))
                            self.assertEqual(share_tag.find('user').get('name'), "Vasily P.")
                            self.assertEqual(share_tag.find('folder').get('path'), "/disk/folder2/")
                            self.assertEqual(share_tag.find('folder').get('name'), 'folder2')
                            self.assertEqual(share_tag.find('folder').get('gid'), gid)
                            notify_send_user = True
                    elif share_tag.tag == 'diff':
                        self.assertEqual(invited_uid, str(self.uid_3))
                        old_ver = share_tag.get('old')
                        new_ver = share_tag.get('new')
                        self.assertNotEqual(old_ver, None)
                        self.assertNotEqual(new_ver, None)
                        self.assertTrue(int(old_ver) < int(new_ver))
                        self.assertTrue(re.match("^/disk/folder2$|/disk/folder2 \d+$", share_tag.find('op').get('key')),
                                        share_tag.find('op').get('key'))
                        self.assertEqual(share_tag.find('op').get('type'), "new")
                        self.assertEqual(share_tag.find('op').get('fid'), folder_fid)
            elif k.startswith(self.search_url):
                qs = urlparse.parse_qs(urlparse.urlparse(k).query)
                if qs['action'][0] != 'invite_activated' or qs['prefix'][0] != self.uid_3:
                    continue
                indexed_resource_ids.add(qs['resource_id'][0])
                assert 'version' in qs
        self.assertTrue(notify_send_actor)
        self.assertTrue(notify_send_owner)
        assert indexed_resource_ids == all_resource_ids

        if approved_count > 2:
            self.assertTrue(notify_send_user)

        opts = {
            'uid': self.uid_3,
        }
        result = self.mail_ok('share_list_not_approved_folders', opts)
        self.assertEqual(len(list(result.iterchildren())), 0)

        # check disk size after invite approve
        disk_size_after = db.disk_info.find_one({'uid': str(self.uid_3), 'key': '/total_size'})['data']
        self.assertEqual(disk_size_before, disk_size_after)
