# -*- coding: utf-8 -*-
import mock
import re
import pytest


from lxml import etree

from mpfs.core.filesystem.resources.disk import get_blockings_collection
from test.base import DiskTestCase
from test.common.sharing import CommonSharingMethods
from test.base_suit import set_up_open_url, tear_down_open_url
from mpfs.common.static import codes
from test.conftest import INIT_USER_IN_POSTGRES
from mpfs.metastorage.mongo.mapper import POSTGRES_USER_INFO_ENTRY

import mpfs.engine.process

from mpfs.core.queue import mpfs_queue
from mpfs.metastorage.mongo.util import decompress_data
from mpfs.core.filesystem.dao.legacy import CollectionRoutedDatabase
from test.helpers.size_units import GB

db = CollectionRoutedDatabase()


class PublicCommonSharingTestCase(CommonSharingMethods):

    def setup_method(self, method):
        super(PublicCommonSharingTestCase, self).setup_method(method)

        if INIT_USER_IN_POSTGRES:
            self.json_ok('user_init', {'uid': self.uid_1, 'shard': POSTGRES_USER_INFO_ENTRY})
            self.json_ok('user_init', {'uid': self.uid_3, 'shard': POSTGRES_USER_INFO_ENTRY})
        else:
            self.json_ok('user_init', {'uid': self.uid_1})
            self.json_ok('user_init', {'uid': self.uid_3})

        self.make_dirs()

    def test_set_file_public_rw(self):
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid, rights=660)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        self.upload_file(self.uid_3, '/disk/folder2/file31')

        open_url_data = set_up_open_url()
        opts = {
            'uid': self.uid_3,
            'path': '/disk/folder2/file31',
            'connection_id': 12345,
        }
        self.mail_ok('set_public', opts)
        tear_down_open_url()

        opts = {'uid': self.uid, 'path': '/disk/new_folder/folder2/file31'}
        link1 = self.mail_ok('info', opts).find('file').find('meta').find('short_url').text
        opts = {'uid': self.uid_3, 'path': '/disk/folder2/file31'}
        link2 = self.mail_ok('info', opts).find('file').find('meta').find('short_url').text
        self.assertEqual(link1, link2)

        # self.fail(open_url_data)
        notify_sent_owner = False
        notify_sent_user = False
        for k, v in open_url_data.iteritems():
            if k.startswith('http://localhost/service/echo?uid='):
                notified_uid = re.search('http://localhost/service/echo\?uid=(\d+)', k).group(1)
                if notified_uid == str(self.uid):
                    for each in v:
                        share_tag = etree.fromstring(each['pure_data'])
                        if share_tag.tag == 'diff':
                            self.assertNotEqual(share_tag.get('committer_username'), None)
                            self.assertEqual(share_tag.get('committer_email'), self.email_3)
                            self.assertEqual(share_tag.get('committer_uid'), str(self.uid_3))
                            self.assertEqual(len(list(share_tag.iterfind('op'))), 1)
                            self.assertEqual(share_tag.find('op').get('folder'), '/disk/new_folder/folder2/')
                            self.assertEqual(share_tag.find('op').get('key'), '/disk/new_folder/folder2/file31')
                            self.assertEqual(share_tag.find('op').get('type'), 'published')
                            notify_sent_owner = True
                elif notified_uid == str(self.uid_3):
                    for each in v:
                        share_tag = etree.fromstring(each['pure_data'])
                        if share_tag.tag == 'diff':
                            self.assertEqual(share_tag.get('committer_username'), None)
                            self.assertEqual(share_tag.get('committer_email'), None)
                            self.assertEqual(share_tag.get('committer_uid'), None)
                            self.assertEqual(len(list(share_tag.iterfind('op'))), 1)
                            self.assertEqual(share_tag.find('op').get('folder'), '/disk/folder2/')
                            self.assertEqual(share_tag.find('op').get('key'), '/disk/folder2/file31')
                            self.assertEqual(share_tag.find('op').get('type'), 'published')
                            notify_sent_user = True
        self.assertTrue(notify_sent_owner)
        self.assertTrue(notify_sent_user)
        self.inspect_all()

    def test_rename_group_root_links(self):
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid, rights=660)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        self.upload_file(self.uid_3, '/disk/folder2/file31')
        self.publicate_resource(self.uid_3, '/disk/folder2/file31')

        self.assertNotEqual(
            db.link_data.find_one({'uid': self.uid, 'data.tgt': '128280859:/disk/new_folder/folder2/file31'}), None)
        opts = {
            'uid': self.uid,
            'src': '/disk/new_folder/folder2',
            'dst': '/disk/folder2',
            'connection_id': '12345',
            'force': 1,
        }
        self.mail_ok('async_move', opts)
        self.assertEqual(
            db.link_data.find_one({'uid': self.uid, 'data.tgt': '128280859:/disk/new_folder/folder2/file31'}), None)
        self.assertNotEqual(db.link_data.find_one({'uid': self.uid, 'data.tgt': '128280859:/disk/folder2/file31'}),
                            None)
        opts = {
            'uid': self.uid,
            'src': '/disk/folder2',
            'dst': '/disk/new_folder/folder2',
            'connection_id': '12345',
            'force': 1,
        }
        self.mail_ok('async_move', opts)
        self.assertEqual(db.link_data.find_one({'uid': self.uid, 'data.tgt': '128280859:/disk/folder2/file31'}), None)
        self.assertNotEqual(
            db.link_data.find_one({'uid': self.uid, 'data.tgt': '128280859:/disk/new_folder/folder2/file31'}), None)

    def test_rename_group_parent_links(self):
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid, rights=660)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        self.upload_file(self.uid_3, '/disk/folder2/file31')
        self.publicate_resource(self.uid_3, '/disk/folder2/file31')

        self.assertNotEqual(
            db.link_data.find_one({'uid': self.uid, 'data.tgt': '128280859:/disk/new_folder/folder2/file31'}), None)
        opts = {
            'uid': self.uid,
            'src': '/disk/new_folder',
            'dst': '/disk/new_folder5',
            'connection_id': '12345',
            'force': 1,
        }
        self.mail_ok('async_move', opts)
        self.assertEqual(
            db.link_data.find_one({'uid': self.uid, 'data.tgt': '128280859:/disk/new_folder/folder2/file31'}), None)
        self.assertNotEqual(
            db.link_data.find_one({'uid': self.uid, 'data.tgt': '128280859:/disk/new_folder5/folder2/file31'}), None)
        opts = {
            'uid': self.uid,
            'src': '/disk/new_folder5',
            'dst': '/disk/new_folder',
            'connection_id': '12345',
            'force': 1,
        }
        self.mail_ok('async_move', opts)
        self.assertEqual(
            db.link_data.find_one({'uid': self.uid, 'data.tgt': '128280859:/disk/new_folder5/folder2/file31'}), None)
        self.assertNotEqual(
            db.link_data.find_one({'uid': self.uid, 'data.tgt': '128280859:/disk/new_folder/folder2/file31'}), None)

    @pytest.mark.skipif(True, reason='https://st.yandex-team.ru/CHEMODAN-32396')
    def test_set_folder_public_rw(self):
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid, rights=660)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        self.upload_file(self.uid_3, '/disk/folder2/file31')

        opts = {'uid': self.uid_3, 'path': '/disk/folder2'}
        self.json_ok('set_public', opts)
        self.inspect_all()
        group_folder_data = decompress_data(
            db.user_data.find_one({'uid': self.uid, 'key': '/disk/new_folder/folder2'})['zdata'])
        link_folder_data = decompress_data(db.user_data.find_one({'uid': self.uid_3, 'key': '/disk/folder2'})['zdata'])
        self.assertTrue('pub' in link_folder_data)
        self.assertTrue('pub' in group_folder_data)
        for k in ('public_hash', 'symlink', 'short_url'):
            self.assertNotEqual(group_folder_data['pub'][k], link_folder_data['pub'][k])
        self.assertNotEqual(db.link_data.find_one({'uid': self.uid_3, 'data.tgt': self.uid_3 + ':/disk/folder2'}), None)
        self.assertNotEqual(
            db.link_data.find_one({'uid': self.uid, 'data.tgt': self.uid + ':/disk/new_folder/folder2'}), None)
        self.inspect_all()

    def test_public_folder_tree_owner_link(self):
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid, rights=660)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        self.publicate_resource(self.uid, '/disk/new_folder/folder2')

        opts = {
            'uid': self.uid,
            'path': '/disk/new_folder/folder2',
            'meta': '',
        }
        folder_hash = self.json_ok('info', opts)['meta']['public_hash']
        opts = {
            'private_hash': folder_hash,
            'deep_level': 0,
        }
        result = self.json_ok('public_fulltree', opts, result_type=dict)
        self.assertTrue('this' in result)
        self.assertTrue('list' in result)
        self.assertTrue(isinstance(result['this'], dict))
        self.assertTrue(isinstance(result['list'], list))
        self.assertNotEqual(result['this'], {})
        self.assertNotEqual(result['list'], [])

    @pytest.mark.skipif(True, reason='https://st.yandex-team.ru/CHEMODAN-32396')
    def test_public_folder_tree_user_link(self):
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid, rights=660)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        self.publicate_resource(self.uid, '/disk/new_folder/folder2')

        opts = {
            'uid': self.uid_3,
            'path': '/disk/folder2',
            'meta': '',
        }
        folder_hash = self.json_ok('info', opts)['meta']['public_hash']
        opts = {
            'private_hash': folder_hash,
            'deep_level': 0,
        }
        result = self.json_ok('public_fulltree', opts, result_type=dict)
        self.assertTrue('this' in result)
        self.assertTrue('list' in result)
        self.assertTrue(isinstance(result['this'], dict))
        self.assertTrue(isinstance(result['list'], list))
        self.assertNotEqual(result['this'], {})
        self.assertNotEqual(result['list'], [])

    def test_public_folder_content_owner(self):
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid, rights=660)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        self.publicate_resource(self.uid, '/disk/new_folder/folder2')
        # =======================================================================
        # root
        opts = {
            'uid': self.uid,
            'path': '/disk/new_folder/folder2',
            'meta': ''
        }
        folder_hash = self.json_ok('info', opts)['meta']['public_hash']
        content_length = len(self.json_ok('list', opts))
        self.assertNotEqual(content_length, 0)
        opts = {
            'private_hash': folder_hash,
            'meta': '',
        }
        result = self.json_ok('public_list', opts, result_type=list)
        self.assertEqual(len(result), content_length)
        self.assertEqual(result[0]['name'], 'folder2')
        #=======================================================================
        # child folder
        opts = {'uid': self.uid, 'path': '/disk/new_folder/folder2/folder3'}
        content_length = len(self.json_ok('list', opts))
        self.assertNotEqual(content_length, 0)
        opts = {
            'private_hash': folder_hash + ':/folder3',
            'meta': '',
        }
        result = self.json_ok('public_list', opts, result_type=list)
        self.assertEqual(len(result), content_length)
        #=======================================================================

    @pytest.mark.skipif(True, reason='https://st.yandex-team.ru/CHEMODAN-32396')
    def test_public_folder_content_user(self):
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid, rights=660)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        self.publicate_resource(self.uid, '/disk/new_folder/folder2')
        #=======================================================================
        # root
        opts = {
            'uid': self.uid_3,
            'path': '/disk/folder2',
            'meta': '',
        }
        folder_hash = self.json_ok('info', opts)['meta']['public_hash']
        list_result = self.json_ok('list', opts)
        content_length = len(list_result)
        self.assertNotEqual(content_length, 0)
        opts = {
            'private_hash': folder_hash,
            'meta': '',
        }
        result = self.json_ok('public_list', opts, result_type=list)
        self.assertEqual(len(result), content_length)
        self.assertEqual(result[0]['name'], 'folder2')
        for element in result[1:]:
            path = element['path'].split(':')[1]
            self.assertNotEqual(path.split('/')[1], 'disk')
        # =======================================================================
        # child folder
        opts = {'uid': self.uid_3, 'path': '/disk/folder2/folder3'}
        content_length = len(self.json_ok('list', opts))
        self.assertTrue(content_length > 1)
        opts = {
            'private_hash': folder_hash + ':/folder3',
            'meta': '',
        }
        result = self.json_ok('public_list', opts, result_type=list)
        self.assertEqual(len(result), content_length)
        opts = {
            'private_hash': folder_hash + ':/disk/folder2/folder3',
            'meta': '',
        }
        self.json_error('public_list', opts)
        #=======================================================================

    @pytest.mark.skipif(True, reason='https://st.yandex-team.ru/CHEMODAN-32396')
    def test_set_public_folder_private_rw(self):
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid, rights=660)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        self.publicate_resource(self.uid, '/disk/new_folder/folder2')

        opts = {'uid': self.uid_3, 'path': '/disk/folder2'}
        self.json_ok('set_private', opts)

        link_data = db.link_data.find_one({'uid': self.uid_3, 'data.tgt': self.uid_3 + ':/disk/folder2'})
        self.assertNotEqual(link_data, None)
        self.assertTrue('dtime' in link_data['data'])

        link_data = db.link_data.find_one({'uid': self.uid, 'data.tgt': self.uid + ':/disk/new_folder/folder2'})
        self.assertNotEqual(link_data, None)
        self.assertTrue('dtime' in link_data['data'])

        self.inspect_all()

    def test_dstore_public_file(self):
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        self.upload_file(self.uid, '/disk/new_folder/folder2/owners_public_file.txt')
        self.inspect_all()
        opts = {
            'uid': self.uid,
            'path': '/disk/new_folder/folder2/owners_public_file.txt',
        }
        public_hash = self.json_ok('set_public', opts)['hash']

        self.dstore_file(self.uid_3, '/disk/folder2/owners_public_file.txt')
        self.inspect_all()

        opts = {
            'private_hash': public_hash,
        }
        res = self.json_ok('public_info', opts)
        self.assertEqual(res['resource']['name'], 'owners_public_file.txt')

        opts = {
            'uid': self.uid,
            'path': '/disk/new_folder/folder2/owners_public_file.txt',
        }
        self.json_ok('rm', opts)
        self.inspect_all()

    @pytest.mark.skipif(True, reason='https://st.yandex-team.ru/CHEMODAN-32396')
    def test_blocked_files(self):
        gid = self.create_group()
        res = self.publicate_resource(self.uid, '/disk/new_folder')
        hsh = res['meta']['public_hash']
        opts = {
            'uid': self.uid,
            'path': '/disk/new_folder/file31',
            'blocked': 1
        }
        self.upload_file(self.uid, '/disk/new_folder/file31')
        res = self.service('public_direct_url', {'private_hash': hsh + ':/file31'}, status=[409, ])
        self.json_error('public_info', {'private_hash': hsh + ':/file31'}, code=96)

    # https://st.yandex-team.ru/CHEMODAN-36848
    def test_set_root_public_makes_it_public_for_everyone(self):
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        self.json_ok('set_public', {'uid': self.uid, 'path': '/disk/new_folder/folder2'})
        group_root = self.json_ok('info', {'uid': self.uid, 'path': '/disk/new_folder/folder2', 'meta': ''})
        assert group_root['meta']['public']
        assert group_root['meta']['public_hash']
        shared_root = self.json_ok('info', {'uid': self.uid_3, 'path': '/disk/folder2', 'meta': ''})
        assert shared_root['meta']['public']
        assert shared_root['meta']['public_hash']

        self.json_ok('set_private', {'uid': self.uid, 'path': '/disk/new_folder/folder2'})
        group_root = self.json_ok('info', {'uid': self.uid, 'path': '/disk/new_folder/folder2', 'meta': ''})
        assert not group_root['meta'].get('public')
        assert not group_root['meta'].get('public_hash')
        shared_root = self.json_ok('info', {'uid': self.uid_3, 'path': '/disk/folder2', 'meta': ''})
        assert not shared_root['meta'].get('public')
        assert not shared_root['meta'].get('public_hash')

    def test_public_list_returned_relative_paths_of_shared_disk_subfolder(self):
        self.json_ok('mkdir', {'uid': self.uid_1, 'path': '/disk/folder'})
        self.json_ok('mkdir', {'uid': self.uid_1, 'path': '/disk/folder/subfolder'})

        self.json_ok('mkdir', {'uid': self.uid_1, 'path': '/disk/folder/subfolder/d1'})
        self.json_ok('mkdir', {'uid': self.uid_1, 'path': '/disk/folder/subfolder/d2'})

        self.upload_file(self.uid_1, '/disk/folder/subfolder/f1')
        self.upload_file(self.uid_1, '/disk/folder/subfolder/d1/f2')
        self.upload_file(self.uid_1, '/disk/folder/subfolder/d2/f3')

        gid = self.json_ok('share_create_group', {'uid': self.uid_1, 'path': '/disk/folder/subfolder'})['gid']
        hsh = self.json_ok('share_invite_user', {'uid': self.uid_1, 'gid': gid, 'universe_login': self.email_3,
                                                 'universe_service': 'email', 'rights': '660'})['hash']
        self.json_ok('share_activate_invite', {'uid': self.uid_3, 'hash': hsh})

        result = self.json_ok('set_public', {'uid': self.uid_3, 'path': '/disk/subfolder'})
        public_hash = result['hash']

        result = self.json_ok('public_list', {'private_hash': public_hash})
        assert {x['path'] for x in result} == {public_hash + ':', public_hash + ':/d1', public_hash + ':/d2', public_hash + ':/f1'}
        assert {x['id'] for x in result} == {public_hash + ':/', public_hash + ':/d1/', public_hash + ':/d2/', public_hash + ':/f1'}

        result = self.json_ok('public_list', {'private_hash': public_hash + ':/d1'})
        assert {x['path'] for x in result} == {public_hash + ':/d1', public_hash + ':/d1/f2'}
        assert {x['id'] for x in result} == {public_hash + ':/d1/', public_hash + ':/d1/f2'}

        result = self.json_ok('public_list', {'private_hash': public_hash + ':/f1'})
        assert result['path'] == public_hash + ':/f1'
        assert result['id'] == public_hash + ':/f1'

        result = self.json_ok('public_list', {'private_hash': public_hash + ':/d2/f3'})
        assert result['path'] == public_hash + ':/d2/f3'
        assert result['id'] == public_hash + ':/d2/f3'

    def test_share_activate_invite_task(self):
        self.json_ok('mkdir', {'uid': self.uid_1, 'path': '/disk/folder'})
        self.json_ok('mkdir', {'uid': self.uid_1, 'path': '/disk/folder/1'})
        self.upload_file(self.uid_1, '/disk/folder/1/file.bin')

        gid = self.json_ok('share_create_group', {'uid': self.uid_1, 'path': '/disk/folder'})['gid']
        hsh = self.json_ok('share_invite_user', {'uid': self.uid_1, 'gid': gid, 'universe_login': self.email_3,
                                                 'universe_service': 'email', 'rights': '660'})['hash']

        # проверить, что push_invite_activated ставится асинхронно
        with mock.patch.object(mpfs_queue, 'put') as mpfs_queue_put_mock:
            self.json_ok('share_activate_invite', {'uid': self.uid_3, 'hash': hsh})
            push_invite_tasks = filter(
                lambda args: args[0][1] == 'push_invite_activated',
                mpfs_queue_put_mock.call_args_list
            )

        assert any(push_invite_tasks)

        # проверить, что таск выполняется корректно.
        original_mpfs_queue_put = mpfs_queue.put
        with mock.patch.object(mpfs_queue, 'put') as mpfs_queue_put_mock:
            for push_invite_task_args, push_invite_task_kwargs in push_invite_tasks:
                original_mpfs_queue_put(*push_invite_task_args, **push_invite_task_kwargs)

            assert any(filter(lambda args: args[0][1].endswith('_group'), mpfs_queue_put_mock.call_args_list))

    def test_make_shared_root_public_by_owner(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/shared'})
        self.upload_file(self.uid, '/disk/shared/file.txt')

        self.create_group(self.uid, '/disk/shared')
        invite_hash = self.invite_user(uid=self.uid_1, email=self.email_1, owner=self.uid, path='/disk/shared')
        self.activate_invite(self.uid_1, invite_hash)

        set_public_result = self.json_ok('set_public', {'uid': self.uid, 'path': '/disk/shared'})
        public_info_result = self.json_ok('public_info', {'uid': self.uid, 'private_hash': set_public_result['hash']})
        public_list_result = self.json_ok('public_list', {'uid': self.uid, 'private_hash': set_public_result['hash']})

        assert public_info_result['resource']['name'] == 'shared'
        assert public_list_result
        assert any(i['name'] == 'file.txt' for i in public_list_result)

    def test_make_shared_root_public_by_participant(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/shared'})
        self.upload_file(self.uid, '/disk/shared/file.txt')

        self.create_group(self.uid, '/disk/shared')
        invite_hash = self.invite_user(uid=self.uid_1, email=self.email_1, owner=self.uid, path='/disk/shared')
        self.activate_invite(self.uid_1, invite_hash)

        set_public_result = self.json_ok('set_public', {'uid': self.uid_1, 'path': '/disk/shared'})
        public_info_result = self.json_ok('public_info', {'uid': self.uid_1, 'private_hash': set_public_result['hash']})
        public_list_result = self.json_ok('public_list', {'uid': self.uid_1, 'private_hash': set_public_result['hash']})

        assert public_info_result['resource']['name'] == 'shared'
        assert public_list_result
        assert any(i['name'] == 'file.txt' for i in public_list_result)

    def test_make_moved_shared_root_public_by_participant(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/owner_path'})
        self.upload_file(self.uid, '/disk/owner_path/file.txt')

        self.create_group(self.uid, '/disk/owner_path')
        invite_hash = self.invite_user(uid=self.uid_1, email=self.email_1, owner=self.uid, path='/disk/owner_path')
        self.activate_invite(self.uid_1, invite_hash)

        self.json_ok('mkdir', {'uid': self.uid_1, 'path': '/disk/f1'})
        self.json_ok('mkdir', {'uid': self.uid_1, 'path': '/disk/f1/f2'})
        self.json_ok('move', {'uid': self.uid_1, 'src': '/disk/owner_path', 'dst': '/disk/f1/f2/participant_path'})

        set_public_result = self.json_ok('set_public', {'uid': self.uid_1, 'path': '/disk/f1/f2/participant_path'})
        public_info_result = self.json_ok('public_info', {'uid': self.uid_1, 'private_hash': set_public_result['hash']})
        public_list_result = self.json_ok('public_list', {'uid': self.uid_1, 'private_hash': set_public_result['hash']})

        assert public_info_result['resource']['name'] == 'participant_path'
        assert public_list_result
        assert any(i['name'] == 'file.txt' for i in public_list_result)

    def test_public_url_for_overdraft(self):
        hash = self.publicate_resource(self.uid, '/disk/new_folder')['meta']['public_hash']

        with mock.patch('mpfs.core.social.publicator.FEATURE_TOGGLES_DISABLE_PUBLIC_LINKS_PERCENTAGE', 100), \
             mock.patch('mpfs.core.services.disk_service.MPFSStorageService.used', return_value=12*GB), \
             mock.patch('mpfs.core.services.disk_service.MPFSStorageService.limit', return_value=10*GB):
            self.json_error('public_url', {'private_hash': hash},
                            code=codes.OVERDRAFT_USER_PUBLIC_LINK_IS_DISABLED)

    def test_public_info_for_overdraft(self):
        hash = self.publicate_resource(self.uid, '/disk/new_folder')['meta']['public_hash']

        with mock.patch('mpfs.core.social.publicator.FEATURE_TOGGLES_DISABLE_PUBLIC_LINKS_PERCENTAGE', 100), \
             mock.patch('mpfs.core.services.disk_service.MPFSStorageService.used', return_value=12*GB), \
             mock.patch('mpfs.core.services.disk_service.MPFSStorageService.limit', return_value=10*GB):
            self.json_error('public_info', {'private_hash': hash},
                            code=codes.OVERDRAFT_USER_PUBLIC_LINK_IS_DISABLED)

    def test_public_list_for_overdraft(self):
        hash = self.publicate_resource(self.uid, '/disk/new_folder')['meta']['public_hash']

        with mock.patch('mpfs.core.social.publicator.FEATURE_TOGGLES_DISABLE_PUBLIC_LINKS_PERCENTAGE', 100), \
             mock.patch('mpfs.core.services.disk_service.MPFSStorageService.used', return_value=12*GB), \
             mock.patch('mpfs.core.services.disk_service.MPFSStorageService.limit', return_value=10*GB):
            self.json_error('public_list', {'private_hash': hash},
                            code=codes.OVERDRAFT_USER_PUBLIC_LINK_IS_DISABLED)


class BlockedAntiFOFilesOfYandexoidTestCase(DiskTestCase):

    def setup_method(self, method):
        super(BlockedAntiFOFilesOfYandexoidTestCase, self).setup_method(method)
        path = '/disk/tst.jpg'
        self.json_ok('staff_make_yateam_admin', {'uid': self.uid, 'yateam_uid': '123'})
        self.upload_file(self.uid, path)
        self.public_hash = self.json_ok('set_public', {'uid': self.uid, 'path': path})['hash']
        file_info = self.json_ok('info', {'uid': self.uid, 'path': path, 'meta': 'hid'})
        hid = file_info['meta']['hid']
        get_blockings_collection().insert({'_id': hid, 'data': 'i_dont_know_what_to_put_here'})

    def test_blocked_by_anti_fo_file_of_yandexoid_does_not_have_blocking_meta_field(self):
        res = self.json_ok('public_info', {'uid': '0', 'meta': 'blockings', 'private_hash': self.public_hash},
                           fake_request_add_fields=(('view_args', {'path': 'json/public_info'}),))
        assert 'blockings' not in res['resource']['meta']

    def test_blocked_by_anti_fo_file_of_yandexoid_available_for_public_url(self):
        self.json_ok('public_url', {'uid': '0', 'private_hash': self.public_hash},
                     fake_request_add_fields=(('view_args', {'path': 'json/public_info'}),))
