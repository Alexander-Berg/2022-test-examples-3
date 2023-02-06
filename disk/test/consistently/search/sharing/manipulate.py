# -*- coding: utf-8 -*-
import urlparse

import pytest
import mock
from nose_parameterized import parameterized
from collections import defaultdict

from mpfs.core.address import ResourceId
from test.common.sharing import SharingWithSearchTestCase
from test.base_suit import set_up_open_url, tear_down_open_url
from test.helpers.stubs.services import PushServicesStub
from mpfs.common.static.tags import experiment_names
from mpfs.common.util.experiments.logic import ExperimentManager

import mpfs.engine.process

from mpfs.common.util import from_json
from mpfs.core.filesystem.dao.legacy import CollectionRoutedDatabase


db = CollectionRoutedDatabase()


class ManipulateSharingTestCase(SharingWithSearchTestCase):

    def setup_method(self, method):
        super(ManipulateSharingTestCase, self).setup_method(method)
        self.json_ok('user_init', {'uid': self.uid_1})
        self.json_ok('user_init', {'uid': self.uid_3})
        self.make_dirs()
        self.upload_file(self.uid_1, '/disk/file3')

    @parameterized.expand([
        ['short_file_list_call_reindex', {experiment_names.NOTIFY_INDEX_ON_SHARING_DELETE_SHORT: True}, True],
        ['full_file_list_call_reindex', {experiment_names.NOTIFY_INDEX_ON_SHARING_DELETE_SHORT: True}, False],
        ['full_file_list_prev_logic', {experiment_names.NOTIFY_INDEX_ON_SHARING_DELETE_SHORT: False}, True],
        ['full_file_list_prev_logic', {experiment_names.NOTIFY_INDEX_ON_SHARING_DELETE_SHORT: False}, False],
    ])
    def test_unshare_group(self, test_name, exps, is_owner_quick_moved):
        default_is_feature_active = ExperimentManager().is_feature_active

        def fake_is_feature_active(_, name):
            return exps.get(name, default_is_feature_active(name))

        owner_group_folder = '/disk/new_folder/folder2'
        owner_group_folder_path = owner_group_folder + '/'
        gid = self.create_group(path=owner_group_folder)
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        args = {
            'uid': self.uid,
            'gid': gid,
            'rights': 660,
            'universe_login': 'mpfs-test-1@yandex.ru',
            'universe_service': 'email',
            'avatar': 'http://localhost/echo',
            'name': 'mpfs-test',
        }
        self.mail_ok('share_invite_user', args)
        self.assertNotEqual(db.group_invites.find_one({'gid' : gid}), None)
        opts = {
            'uid': self.uid,
            'path': '/disk',
            'meta': '',
        }
        diff_result = self.desktop('diff', opts)
        all_owner_resource_ids = {ResourceId(self.uid, x['fid']).serialize() for x in diff_result['result'] if x['key'] != '/disk/new_folder'}
        short_owner_resource_ids = {ResourceId(self.uid, x['fid']).serialize() for x in diff_result['result'] if
                                  x['key'] == owner_group_folder}
        folder_fid = self.json_ok('info', {'uid': self.uid_3, 'path': '/disk/folder2', 'meta': 'file_id'})['meta']['file_id']
        all_guest_resource_ids = {ResourceId(self.uid, x['fid']).serialize()
                                  for x in diff_result['result']
                                  if x['key'] not in [owner_group_folder, '/disk/new_folder']} | {ResourceId(self.uid_3, folder_fid).serialize()}

        version_before = diff_result['version']
        args = {
            'uid': self.uid,
            'gid': gid,
        }
        if is_owner_quick_moved:
            self.service_ok('reindex_for_quick_move_callback', {'uid': self.uid})
        xiva_requests = set_up_open_url()

        with PushServicesStub() as push_service, \
            mock.patch('mpfs.common.util.experiments.logic.ExperimentManager.is_feature_active',
                       fake_is_feature_active), \
            mock.patch('mpfs.core.services.index_service.SearchIndexer.start_reindex_for_quick_move',
                       return_value=None) as reindexer_mock:
            self.mail_ok('share_unshare_folder', args)
            tear_down_open_url()
            pushes = [PushServicesStub.parse_send_call(push_args)
                      for push_args in push_service.send.call_args_list]
            if test_name == 'short_file_list_call_reindex':
                reindexer_mock.assert_called_once_with(self.uid_3)
            elif test_name == 'full_file_list_call_reindex':
                reindexer_mock.assert_has_calls([mock.call(self.uid), mock.call(self.uid_3)], any_order=True)
            else:
                assert not reindexer_mock.called
        owner_push = pushes[0]
        user_push = pushes[1]
        actor_push = pushes[2]
        assert owner_push['uid'] == self.uid
        assert owner_push['json_payload']['root']['parameters']['type'] == 'folder_unshared'
        assert owner_push['json_payload']['root']['parameters']['for'] == 'owner'
        assert owner_push['json_payload']['values'][0]['parameters']['path'] == owner_group_folder_path
        assert owner_push['json_payload']['values'][0]['parameters']['gid'] == gid
        assert user_push['uid'] == self.uid_3
        assert user_push['json_payload']['root']['parameters']['type'] == 'folder_unshared'
        assert user_push['json_payload']['root']['parameters']['for'] == 'user'
        assert user_push['json_payload']['values'][0]['parameters']['path'] == '/disk/folder2/'
        assert user_push['json_payload']['values'][0]['parameters']['gid'] == gid
        assert actor_push['uid'] == self.uid_1
        assert actor_push['json_payload']['root']['parameters']['type'] == 'invite_removed'
        assert actor_push['json_payload']['root']['parameters']['for'] == 'actor'
        assert actor_push['json_payload']['values'][0]['parameters']['path'] == '/disk/folder2/'
        assert actor_push['json_payload']['values'][0]['parameters']['gid'] == gid

        opts = {
            'uid': self.uid,
            'path': '/disk',
            'version': version_before,
            'meta': '',
        }
        diff_result = self.desktop('diff', opts)
        version_after = diff_result['version']
        self.assertNotEqual(version_before, version_after)
        self.assertTrue(len(diff_result['result']) == 1)
        self.assertEqual(diff_result['result'][0]['key'], owner_group_folder)
        self.assertEqual(diff_result['result'][0]['type'], 'dir')
        self.assertEqual(diff_result['result'][0]['op'], 'changed')
        self.assertEqual(diff_result['result'][0]['rights'], 660)
        self.assertEqual(diff_result['result'][0]['shared'], 'unshared')
        #=======================================================================
        opts = {'uid': self.uid}
        result = self.mail_ok('share_list_all_folders', opts)
        self.assertEqual(result.find('folder'), None)
        opts = {'uid': self.uid_3}
        result = self.mail_ok('share_list_all_folders', opts)
        self.assertEqual(len(list(result.iterfind('folder'))), 0)
        opts = {
            'uid': self.uid_3,
            'path': '/disk/folder2',
        }
        self.mail_error('list', opts)
        self.assertEqual(db.user_data.find_one({'uid': str(self.uid_3), 'key': '/disk/folder2'}), None)
        self.assertEqual(db.group_invites.find_one({'gid': gid}), None)
        indexed_deleted_resource_ids = set()
        indexed_modified_resource_ids = set()
        for k, v in xiva_requests.iteritems():
            if not k.startswith(self.search_url):
                continue
            qs = urlparse.parse_qs(urlparse.urlparse(k).query)
            if qs['action'][0] == 'kicked_by_unshare' and qs['prefix'][0] == self.uid_3:
                indexed_deleted_resource_ids.add(qs['resource_id'][0])
            elif qs['action'][0] == 'folder_unshared' and qs['prefix'][0] == self.uid:
                indexed_modified_resource_ids.add(qs['resource_id'][0])

        assert all_guest_resource_ids == indexed_deleted_resource_ids
        if 'short_file_list' in test_name:
            assert short_owner_resource_ids == indexed_modified_resource_ids
        else:
            assert all_owner_resource_ids == indexed_modified_resource_ids

        opts = {
            'uid': self.uid,
            'path': owner_group_folder
        }
        folder_listing = self.mail_ok('list', opts)

        self.assertEqual(folder_listing.find('folder-list').find('folder').find('meta').find('group'), None)
        opts = {
            'uid': self.uid,
            'path': owner_group_folder
        }
        folder_info = self.mail_ok('info', opts)
        self.assertEqual(folder_info.find('folder').find('meta').find('group'), None)

    def test_check_parent_rights(self):
        """
            https://jira.yandex-team.ru/browse/CHEMODAN-10554
        """
        self.create_group()

        user_info = {}
        for uid in (self.uid, self.uid_1):
            opts = {
                'uid': uid,
                }
            user_info[uid] = self.json_ok('user_info', opts)
        self.assertNotEqual(user_info[self.uid]['space']['free'], 0)
        self.assertNotEqual(user_info[self.uid_1]['space']['free'], 0)
        self.assertNotEqual(user_info[self.uid]['space']['used'], 0)
        self.assertNotEqual(user_info[self.uid_1]['space']['used'], 0)
        for uid in (self.uid, self.uid_1):
            resources = self.json_ok('list', {'uid': uid, 'path': '/disk'})
            resources_paths = [r['path'] for r in resources if r['path'] != '/disk']
            for path in resources_paths:
                self.json_ok('async_trash_append', {'uid': uid, 'path': path})

            self.json_ok('async_trash_drop_all', {'uid': uid})
            self.assertEqual(db.group_links.find_one({'uid': uid}), None)
            self.assertEqual(db.groups.find_one({'owner': uid}), None)
            user_data_listing = list(db.user_data.find({'uid': uid}))
            self.assertEqual(len(user_data_listing), 2, user_data_listing)
        for uid in (self.uid, self.uid_1):
            opts = {
                'uid': uid,
                'path': '/disk'
            }
            self.service_ok('inspect', opts)
        #=======================================================================
        # Check space details are equal
        for uid in (self.uid, self.uid_1):
            opts = {
                'uid': uid,
            }
            user_info[uid] = self.json_ok('user_info', opts)
        for k in ('limit', 'used', 'free'):
            self.assertEqual(user_info[self.uid]['space'][k], user_info[self.uid_1]['space'][k], (k, user_info))
        #=======================================================================

        name_660 = 'child_A_660'
        name_640 = 'child_B_640'

        opts = {
            'uid': self.uid,
            'path': '/disk/parent_folder'
        }
        self.json_ok('mkdir', opts)
        opts = {
            'uid': self.uid,
            'path': '/disk/parent_folder/%s' % name_660,
        }
        self.json_ok('mkdir', opts)
        opts = {
            'uid': self.uid,
            'path': '/disk/parent_folder/%s' % name_640
        }
        self.json_ok('mkdir', opts)

        hsh = self.invite_user(uid=self.uid_1, email=self.email_1, rights=660, path='/disk/parent_folder/%s/' % name_660)
        args = {
            'hash': hsh,
            'uid': self.uid_1,
        }
        self.mail_ok('share_activate_invite', args)

        hsh = self.invite_user(uid=self.uid_1, email=self.email_1, rights=640, path='/disk/parent_folder/%s/' % name_640)
        args = {
            'hash': hsh,
            'uid': self.uid_1,
        }
        self.mail_ok('share_activate_invite', args)
        #=======================================================================
        # Check space details are equal
        for uid in (self.uid, self.uid_1):
            opts = {
                'uid': uid,
            }
            user_info[uid] = self.json_ok('user_info', opts)
        for k in ('limit', 'used', 'free'):
            self.assertEqual(user_info[self.uid]['space'][k], user_info[self.uid_1]['space'][k], k)
        #=======================================================================
        opts = {
            'uid': self.uid_1,
            'path': '/disk/%s' % name_640,
            'meta': '',
        }
        child_640_info = self.json_ok('info', opts)
        self.assertEqual(child_640_info['meta']['group']['rights'], 640)
        opts = {
            'uid': self.uid_1,
            'path': '/disk/%s' % name_660,
            'meta': '',
        }
        child_660_info = self.json_ok('info', opts)
        self.assertEqual(child_660_info['meta']['group']['rights'], 660)
        #=======================================================================
        # Check space details are equal
        for uid in (self.uid, self.uid_1):
            opts = {
                'uid': uid,
            }
            user_info[uid] = self.json_ok('user_info', opts)
        for k in ('limit', 'used', 'free'):
            self.assertEqual(user_info[self.uid]['space'][k], user_info[self.uid_1]['space'][k], k)
        #=======================================================================
        opts = {
            'uid': self.uid_1,
            'path': '/disk/share_parent',
        }
        self.json_ok('mkdir', opts)
        opts = {
            'uid': self.uid_1,
            'path': '/disk/share_parent',
            'meta': '',
        }
        self.json_ok('info', opts)
        opts = {
            'uid': self.uid_1,
            'src': '/disk/%s' % name_660,
            'dst': '/disk/share_parent/%s' % name_660,
        }
        self.json_ok('move', opts)
        opts = {
            'uid': self.uid_1,
            'path': '/disk/%s' % name_660,
            'meta': '',
        }
        self.json_error('info', opts)
        #=======================================================================
        # Check space details are equal
        for uid in (self.uid, self.uid_1):
            opts = {
                'uid': uid,
            }
            user_info[uid] = self.json_ok('user_info', opts)
        for k in ('limit', 'used', 'free'):
            self.assertEqual(user_info[self.uid]['space'][k], user_info[self.uid_1]['space'][k], k)
        # =======================================================================
        opts = {
            'uid': self.uid_1,
            'path': '/disk/share_parent/%s' % name_660,
            'meta': '',
        }
        self.json_ok('info', opts)
        #=======================================================================
        # Check space details are equal
        for uid in (self.uid, self.uid_1):
            opts = {
                'uid': uid,
            }
            user_info[uid] = self.json_ok('user_info', opts)
        for k in ('limit', 'used', 'free'):
            self.assertEqual(user_info[self.uid]['space'][k], user_info[self.uid_1]['space'][k], k)
        # =======================================================================
        opts = {
            'uid': self.uid_1,
            'path': '/disk/share_parent',
            'meta': '',
        }
        parent_info = self.json_ok('info', opts)
        self.assertEqual(parent_info['meta']['with_shared'], 1)
        self.assertEqual(parent_info['meta']['shared_rights'], 660)

        opts = {
            'uid': self.uid_1,
            'src': '/disk/%s' % name_640,
            'dst': '/disk/share_parent/%s' % name_640,
        }
        self.json_ok('move', opts)
        #=======================================================================
        # Check space details are equal
        for uid in (self.uid, self.uid_1):
            opts = {
                'uid': uid,
            }
            user_info[uid] = self.json_ok('user_info', opts)
        for k in ('limit', 'used', 'free'):
            self.assertEqual(user_info[self.uid]['space'][k], user_info[self.uid_1]['space'][k], k)
        # =======================================================================
        opts = {
            'uid': self.uid_1,
            'path': '/disk/%s' % name_640,
            'meta': '',
        }
        self.json_error('info', opts)
        opts = {
            'uid': self.uid_1,
            'path': '/disk/share_parent/%s' % name_640,
            'meta': '',
        }
        self.json_ok('info', opts)
        opts = {
            'uid': self.uid_1,
            'path': '/disk/share_parent',
            'meta': '',
        }
        parent_info = self.json_ok('info', opts)
        self.assertEqual(parent_info['meta']['with_shared'], 1)
        self.assertEqual(parent_info['meta']['shared_rights'], 640)

    def test_kick_user_no_folder(self):
        name_660 = 'child_A_660'
        name_640 = 'child_B_640'

        opts = {'uid': self.uid, 'path': '/disk/parent_folder'}
        self.json_ok('mkdir', opts)

        opts = {'uid': self.uid, 'path': '/disk/parent_folder/%s' % name_660}
        self.json_ok('mkdir', opts)

        opts = {'uid': self.uid, 'path': '/disk/parent_folder/%s' % name_640}
        self.json_ok('mkdir', opts)

        gid = self.create_group(path='/disk/parent_folder/%s' % name_660)

        hsh = self.invite_user(uid=self.uid_1, email=self.email_1, rights=660, path='/disk/parent_folder/%s/' % name_660)
        self.activate_invite(uid=self.uid_1, hash=hsh)

        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, rights=660, path='/disk/parent_folder/%s/' % name_660)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        group_data = db.groups.find_one({'owner': self.uid, 'path': '/disk/parent_folder/%s' % name_660})
        self.assertNotEqual(group_data, None)
        link_data = db.group_links.find_one({'uid': self.uid_3, 'gid': group_data['_id']})
        self.assertNotEqual(link_data, None)
        link_folder = db.user_data.find_one({'uid': self.uid_3, 'key': link_data['path']})
        self.assertNotEqual(link_folder, None)
        db.user_data.remove({'uid': self.uid_3, 'key': link_data['path']})
        link_folder = db.user_data.find_one({'uid': self.uid_3, 'key': link_data['path']})
        self.assertEqual(link_folder, None)
        self.inspect_all()
        opts = {
            'uid': self.uid,
            'user_uid': self.uid_3,
            'gid': gid,
        }
        self.json_ok('share_kick_from_group', opts)
        link_data = db.group_links.find_one({'uid': self.uid_3, 'gid': group_data['_id']})
        self.assertEqual(link_data, None)
        group_data = db.groups.find_one({'owner': self.uid, 'path': '/disk/parent_folder/%s' % name_660})
        self.assertNotEqual(group_data, None)
        self.inspect_all()


class ManipulateLeaveKickSharingTestCase(SharingWithSearchTestCase):

    def setup_method(self, method):
        super(ManipulateLeaveKickSharingTestCase, self).setup_method(method)
        self.json_ok('user_init', {'uid': self.uid_1})
        self.json_ok('user_init', {'uid': self.uid_3})
        self.make_dirs()
        self.upload_file(self.uid_1, '/disk/file3')

    def test_leave_group(self):
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        args = {
            'uid': self.uid_3,
            'gid': gid,
        }
        open_url_data = set_up_open_url()
        with PushServicesStub() as push_service:
            self.mail_ok('share_leave_group', args)
            tear_down_open_url()
            pushes = [PushServicesStub.parse_send_call(push_args)
                      for push_args in push_service.send.call_args_list]

        assert len(pushes) == 2, 'Должно быть только 2 пуша (для actor и для owner)'
        actor_push = pushes[0]
        owner_push = pushes[1]

        assert actor_push['uid'] == self.uid_3
        assert actor_push['json_payload']['root']['parameters']['type'] == 'user_has_left'
        assert actor_push['json_payload']['root']['parameters']['for'] == 'actor'
        assert actor_push['json_payload']['values'][0]['parameters']['path'] == '/disk/folder2/'
        assert actor_push['json_payload']['values'][0]['parameters']['gid'] == gid

        assert owner_push['uid'] == self.uid
        assert owner_push['json_payload']['root']['parameters']['type'] == 'user_has_left'
        assert owner_push['json_payload']['root']['parameters']['for'] == 'owner'
        assert owner_push['json_payload']['values'][0]['parameters']['path'] == '/disk/new_folder/folder2/'
        assert owner_push['json_payload']['values'][0]['parameters']['gid'] == gid
        assert owner_push['json_payload']['values'][1]['parameters']['uid'] == self.uid_3

        group_link = db.group_links.find_one({'path': '/disk/folder2', 'uid': str(self.uid_3)})
        self.assertEqual(group_link, None)
        self.mail_error('info', {'uid': self.uid_3, 'path': '/disk/folder2'})
        search_found = False
        for k, v in open_url_data.iteritems():
            if not k.startswith(self.search_url):
                continue
            qs = urlparse.parse_qs(urlparse.urlparse(k).query)
            if qs['action'][0] != 'leave_folder' or qs['prefix'][0] != self.uid_3:
                continue
            assert({'resource_id', 'prefix', 'version'}.issubset(set(qs.keys())))
            search_found = True
        self.assertTrue(search_found)
        self.inspect_all()

    def test_kick_user(self):
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        args = {
            'gid': gid,
            'uid': self.uid,
        }
        folder_users = self.mail_ok('share_users_in_group', args)
        # self.fail(etree.tostring(folder_users, pretty_print=True))
        users_count = len(list(folder_users.find('users').iterfind('user')))
        self.assertEqual(users_count, 2)

        args = {
            'uid': self.uid,
            'user_uid': self.uid_3,
            'gid': gid,
        }
        open_url_data = set_up_open_url()
        with PushServicesStub() as push_service:
            self.mail_ok('share_kick_from_group', args)
            tear_down_open_url()
            pushes = [PushServicesStub.parse_send_call(push_args)
                      for push_args in push_service.send.call_args_list]
        assert len(pushes) == 2, 'Должно быть только 2 пуша (для actor и для owner)'
        actor_push = pushes[0]
        owner_push = pushes[1]

        assert actor_push['uid'] == self.uid_3
        assert actor_push['json_payload']['root']['parameters']['type'] == 'user_was_banned'
        assert actor_push['json_payload']['root']['parameters']['for'] == 'actor'
        assert actor_push['json_payload']['values'][0]['parameters']['path'] == '/disk/folder2/'
        assert actor_push['json_payload']['values'][0]['parameters']['gid'] == gid

        assert owner_push['uid'] == self.uid
        assert owner_push['json_payload']['root']['parameters']['type'] == 'user_was_banned'
        assert owner_push['json_payload']['root']['parameters']['for'] == 'owner'
        assert owner_push['json_payload']['values'][0]['parameters']['path'] == '/disk/new_folder/folder2/'
        assert owner_push['json_payload']['values'][0]['parameters']['gid'] == gid
        assert owner_push['json_payload']['values'][1]['parameters']['uid'] == self.uid_3

        deleted_resources_indexed = 0
        for k, v in open_url_data.iteritems():
            if not k.startswith(self.search_url):
                continue
            qs = urlparse.parse_qs(urlparse.urlparse(k).query)
            if qs['action'][0] != 'leave_folder' or qs['prefix'][0] != self.uid_3:
                continue
            assert ({'resource_id', 'prefix', 'version'}.issubset(set(qs.keys())))
            deleted_resources_indexed += 1

        self.assertTrue(deleted_resources_indexed > 1)

        user_resources = db.user_data.find({'uid': str(self.uid_3)})
        left_resources = filter(lambda x: x['key'].startswith('/disk/folder2/'), user_resources)
        self.assertEqual(len(left_resources), 0)
        self.assertEqual(db.user_data.find_one({'uid': str(self.uid_3), 'key': '/disk/folder2'}), None)
        self.assertEqual(db.group_links.find_one({'uid': str(self.uid_3), 'path': '/disk/folder2'}), None)
        opts = {
            'uid': self.uid_3,
            'path': '/disk/folder2',
        }
        self.mail_error('list', opts)
        opts = {
            'uid': self.uid_3,
            'path': '/disk',
        }
        self.mail_ok('list', opts)
        self.inspect_all()

    @pytest.mark.skipif(True, reason='This feature is not used in production')
    def test_change_group_owner(self):
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        self.inspect_all()

        self.assertNotEqual(
            db.group_links.find_one({'uid': str(self.uid_3), 'path': '/disk/folder2', 'gid': gid}),
            None)
        self.assertNotEqual(db.groups.find_one(
            {'owner': str(self.uid), 'path': '/disk/new_folder/folder2', '_id': gid}), None)
        self.assertEqual(
            db.groups.find_one({'owner': str(self.uid_3), 'path': '/disk/folder2', 'gid': gid}),
            None)
        args = {
            'owner': self.uid,
            'uid': self.uid_3,
            'gid': gid,
        }
        self.mail_ok('share_change_group_owner', args)
        self.inspect_all()
        args = {
            'uid': self.uid,
            'user_uid': self.uid_3,
            'gid': gid,
        }
        self.mail_error('share_kick_from_group', args)
        self.assertEqual(
            db.group_links.find_one({'uid': str(self.uid_3), 'path': '/disk/folder2', 'gid': gid}),
            None)
        self.assertEqual(db.groups.find_one(
            {'owner': str(self.uid), 'path': '/disk/new_folder/folder2', '_id': gid}), None)
        self.assertNotEqual(
            db.groups.find_one({'owner': str(self.uid_3), 'path': '/disk/folder2', '_id': gid}),
            None)

        self.inspect_all()
        opts = {'uid': self.uid_3, 'meta': ''}
        result = self.json_ok('share_list_all_folders', opts)
        root_checked = False
        for each in result:
            if each['path'] == '/disk/folder2':
                uid = each['meta']['group']['owner']['uid']
                self.assertEqual(uid, str(self.uid_3))
                root_checked = True
        self.assertTrue(root_checked)
