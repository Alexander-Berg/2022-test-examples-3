# -*- coding: utf-8 -*-
import hashlib
import mock
import pytest

from test.common.sharing import CommonSharingMethods

import mpfs.engine.process

from mpfs.metastorage.mongo.util import decompress_data
from test.helpers.stubs.services import PushServicesStub
from mpfs.core.filesystem.dao.legacy import CollectionRoutedDatabase


db = CollectionRoutedDatabase()


class RightsSharingTestCase(CommonSharingMethods):

    def setup_method(self, method):
        super(RightsSharingTestCase, self).setup_method(method)
        self.json_ok('user_init', {'uid': self.uid_1})
        self.json_ok('user_init', {'uid': self.uid_3})
        self.make_dirs()

    def test_change_rights_to_readonly(self):
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        opts = {
            'uid': self.uid_3,
            'src': '/disk/folder2',
            'dst': '/disk/folder2_moved',
            'force': 1,
        }
        self.mail_ok('async_move', opts)

        args = {
            'uid': self.uid,
            'user_uid': self.uid_3,
            'gid': gid,
            'rights': 640,
        }
        with PushServicesStub() as push_service:
            self.mail_ok('share_change_rights', args)
            pushes = [PushServicesStub.parse_send_call(push_args)
                      for push_args in push_service.send.call_args_list]

        assert len(pushes) == 2, u'Должно быть только 2 пуша (для actor и для owner)'

        actor_push = pushes[0]
        owner_push = pushes[1]

        assert actor_push['uid'] == self.uid_3
        assert actor_push['json_payload']['root']['parameters']['type'] == 'rights_changed'
        assert actor_push['json_payload']['root']['parameters']['for'] == 'actor'
        assert actor_push['json_payload']['values'][0]['parameters']['rights'] == 640
        assert actor_push['json_payload']['values'][0]['parameters']['gid'] == gid
        assert actor_push['json_payload']['values'][0]['parameters']['path'] == '/disk/folder2_moved/'

        assert owner_push['uid'] == self.uid
        assert owner_push['json_payload']['root']['parameters']['type'] == 'rights_changed'
        assert owner_push['json_payload']['root']['parameters']['for'] == 'owner'
        assert owner_push['json_payload']['values'][0]['parameters']['rights'] == 640
        assert owner_push['json_payload']['values'][0]['parameters']['gid'] == gid
        assert owner_push['json_payload']['values'][0]['parameters']['path'] == "/disk/new_folder/folder2/"

        self.inspect_all()

    def test_rename_shared_folder_readonly(self):
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        opts = {
            'uid': self.uid_3,
            'src': '/disk/folder2',
            'dst': '/disk/folder2_moved',
            'force': 1,
        }
        self.mail_ok('async_move', opts)

        args = {
            'uid': self.uid,
            'user_uid': self.uid_3,
            'gid': gid,
            'rights': 640,
        }
        self.mail_ok('share_change_rights', args)

        self.assertNotEqual(db.user_data.find_one({'uid' : str(self.uid_3), 'key' : '/disk/folder2_moved'}), None)

        opts = {
            'uid': self.uid_3,
            'src': '/disk/folder2_moved',
            'dst': '/disk/folder2',
            'force': 1,
        }
        with PushServicesStub() as push_service:
            self.mail_ok('async_move', opts)
            pushes = [PushServicesStub.parse_send_call(push_args)
                      for push_args in push_service.send.call_args_list]

        diff_push = pushes[0]

        assert diff_push['uid'] == self.uid_3
        op_values = [value['parameters']
                     for value in diff_push['json_payload']['values']
                     if value['tag'] == 'op']
        assert len(op_values) == 7

        user_found_deleted = None
        user_found_new = None
        for op in op_values:
            assert op.get('resource_type') is not None

            if op.get('key') == '/disk/folder2':
                assert op.get('folder') == '/disk/'
                user_found_new = True
                assert op.get('type'), "new"
            elif op.get('key') == '/disk/folder2_moved':
                assert op.get('folder') == '/disk/'
                assert op.get('type') == "deleted"
                user_found_deleted = True
            else:
                assert op.get('key').startswith('/disk/folder2')

        self.assertTrue(user_found_deleted)
        self.assertTrue(user_found_new)

        opts = {'uid': self.uid_3, 'path': '/disk/folder2'}
        folder_info = self.mail_ok('info', opts)

        self.assertNotEqual(folder_info.find('folder').find('meta').find('group'), None)
        self.assertNotEqual(db.user_data.find_one({'uid': str(self.uid_3), 'key': '/disk/folder2'}), None)
        self.assertEqual(db.user_data.find_one({'uid': str(self.uid_3), 'key': '/disk/folder2_moved'}), None)

        self.inspect_all()

    def test_try_to_set_file_public_readonly(self):
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid, rights=640)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        opts = {'uid': self.uid_3, 'path': '/disk/folder2/file31'}
        self.mail_error('set_public', opts)
        self.inspect_all()

    def test_try_to_set_folder_public_readonly(self):
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid, rights=640)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        opts = {'uid': self.uid_3, 'path': '/disk/folder2/folder3'}
        self.mail_error('set_public', opts)
        self.inspect_all()
        opts = {'uid': self.uid_3, 'path': '/disk/folder2'}
        self.mail_error('set_public', opts)
        self.inspect_all()

    def test_try_to_set_parent_public_readonly(self):
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid, rights=640)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        opts = {'uid': self.uid_3, 'path': '/disk/shared_parent'}
        self.json_ok('mkdir', opts)
        opts = {
            'uid': self.uid_3,
            'src': '/disk/folder2',
            'dst': '/disk/shared_parent/folder2',
        }
        self.json_ok('async_move', opts)
        opts = {'uid': self.uid_3, 'path': '/disk/shared_parent/folder2'}
        self.mail_ok('list', opts)

        opts = {'uid': self.uid_3, 'path': '/disk/shared_parent'}
        self.mail_error('set_public', opts)
        self.inspect_all()

    def test_set_parent_public_readwrite(self):
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid, rights=640)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        opts = {'uid': self.uid_3, 'path': '/disk/shared_parent'}
        self.json_ok('mkdir', opts)

        args = {
            'uid': self.uid,
            'user_uid': self.uid_3,
            'gid': gid,
            'rights': 660,
        }
        self.json_ok('share_change_rights', args)
        opts = {'uid': self.uid_3, 'path': '/disk/shared_parent'}
        result = self.json_ok('set_public', opts)
        private_hash = result['hash']
        opts = {
            'private_hash': private_hash,
        }
        result = self.json_ok('public_list', opts)
        self.assertEqual(len(result), 1)

    def test_move_share_from_public(self):
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        opts = {'uid': self.uid_3, 'path': '/disk/shared_parent'}
        self.json_ok('mkdir', opts)
        opts = {
            'uid': self.uid_3,
            'src': '/disk/folder2',
            'dst': '/disk/shared_parent/folder2',
        }
        self.json_ok('async_move', opts)

        opts = {
            'uid': self.uid_3,
            'src': '/disk/shared_parent/folder2',
            'dst': '/disk/folder2',
        }
        self.json_ok('async_move', opts)
        opts = {'uid': self.uid_3, 'path': '/disk/folder2'}
        self.mail_ok('list', opts)
        opts = {'uid': self.uid_3, 'path': '/disk/shared_parent'}
        self.mail_ok('rm', opts)
        self.inspect_all()

    def test_set_public_file_private_readonly(self):
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        self.upload_file(self.uid, '/disk/new_folder/folder2/file31')
        opts = {'uid': self.uid, 'path': '/disk/new_folder/folder2/file31'}
        self.json_ok('set_public', opts)
        opts = {'uid': self.uid_3, 'path': '/disk/folder2', 'meta': ''}
        folder_info = self.json_ok('info', opts)
        args = {
            'uid': self.uid,
            'user_uid': self.uid_3,
            'gid': folder_info['meta']['group']['gid'],
            'rights': 640,
        }
        self.mail_ok('share_change_rights', args)
        opts = {'uid': self.uid_3, 'path': '/disk/folder2/file31'}
        self.json_error('set_private', opts)
        self.inspect_all()

    @pytest.mark.skipif(True, reason='https://st.yandex-team.ru/CHEMODAN-32396')
    def test_set_public_folder_private_readonly(self):
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid, rights=640)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        opts = {'uid': self.uid, 'path': '/disk/new_folder/folder2'}
        self.mail_ok('set_public', opts)
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

        opts = {'uid': self.uid_3, 'path': '/disk/folder2'}
        self.mail_error('set_private', opts)

        opts = {'uid': self.uid, 'path': '/disk/new_folder/folder2'}
        self.json_ok('set_private', opts)

        link_data = db.link_data.find_one({'uid': self.uid, 'data.tgt': self.uid + ':/disk/new_folder/folder2'})
        self.assertNotEqual(link_data, None)
        self.assertTrue('dtime' in link_data['data'])

        db.link_data.find_one({'uid': self.uid_3, 'data.tgt': self.uid_3 + ':/disk/folder2'})
        self.assertNotEqual(link_data, None)
        self.assertTrue('dtime' in link_data['data'])

        self.inspect_all()

        opts = {'uid': self.uid, 'path': '/disk/new_folder/folder2/folder3'}
        self.mail_ok('set_public', opts)
        opts = {'uid': self.uid_3, 'path': '/disk/folder2/folder3'}
        self.mail_error('set_private', opts)
        opts = {'uid': self.uid, 'path': '/disk/new_folder/folder2/folder3'}
        self.mail_ok('set_private', opts)
        self.inspect_all()

    def test_move_readonly_folder_to_public(self):
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid, rights=640)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        opts = {'uid': self.uid_3, 'path': '/disk/pub'}
        self.json_ok('mkdir', opts)
        self.mail_ok('set_public', opts)
        opts = {
            'uid': self.uid_3,
            'src': '/disk/folder2',
            'dst': '/disk/shared_parent/folder2',
        }
        self.mail_error('async_move', opts)
        self.mail_error('move', opts)
        opts = {'uid': self.uid_3, 'path': '/disk/shared_parent'}
        self.json_ok('mkdir', opts)
        opts = {
            'uid': self.uid_3,
            'src': '/disk/folder2',
            'dst': '/disk/shared_parent/folder2',
        }
        self.mail_ok('async_move', opts)
        opts = {
            'uid': self.uid_3,
            'src': '/disk/shared_parent',
            'dst': '/disk/pub/shared_parent',
        }
        self.mail_error('async_move', opts)
        opts = {
            'uid': self.uid_3,
            'src': '/disk/shared_parent/folder2',
            'dst': '/disk/folder2',
        }
        self.mail_ok('async_move', opts)
        opts = {'uid': self.uid_3, 'path': '/disk/shared_parent'}
        self.mail_ok('rm', opts)
        opts = {'uid': self.uid_3, 'path': '/disk/pub'}
        self.mail_ok('rm', opts)

    def test_readonly_create_file(self):
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid, rights=640)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        new_file_path = '/disk/folder2/fileRO'
        new_file_md5 = hashlib.md5('/disk/folder2/fileRO').hexdigest()
        new_file_size = int(new_file_md5[:5], 16)
        opts = {
            'uid': self.uid_3,
            'path': new_file_path,
            'force': 1,
            'md5': new_file_md5,
            'size': new_file_size,
            'callback': '',
        }
        result = self.mail_error('store', opts)

        self.upload_file(self.uid_3, '/disk/folder2/fileRO', ok=False)
        self.assertEqual(db.user_data.find_one({'uid': str(self.uid_3), 'key': '/disk/folder2/fileRO'}), None)
        opts = {'uid': self.uid_3, 'path': '/disk/folder2/fileRO'}
        self.mail_error('info', opts)

        self.upload_file(self.uid, '/disk/new_folder/folder2/fileRO')
        opts = {'uid': self.uid, 'path': '/disk/new_folder/folder2/fileRO'}
        self.mail_ok('info', opts)
        self.assertNotEqual(db.user_data.find_one({'uid': str(self.uid), 'path': '/disk/new_folder/folder2/fileRO'}),
                            None)

        opts = {'uid': self.uid_3, 'path': '/disk/folder2/fileRO'}
        self.mail_ok('info', opts)
        self.assertEqual(db.user_data.find_one({'uid': str(self.uid_3), 'path': '/disk/folder2/fileRO'}), None)
        self.inspect_all()

    def test_readonly_rm_file(self):
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid, rights=640)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        self.upload_file(self.uid, '/disk/new_folder/folder2/fileRO')

        opts = {'uid': self.uid_3, 'path': '/disk/folder2/fileRO'}
        self.mail_error('rm', opts)
        opts = {'uid': self.uid_3, 'path': '/disk/folder2/fileRO'}
        self.mail_ok('info', opts)
        opts = {'uid': self.uid_3, 'path': '/disk/folder2/fileRO'}
        self.mail_error('trash_append', opts)
        opts = {'uid': self.uid_3, 'path': '/disk/folder2/fileRO'}
        self.mail_ok('info', opts)
        opts = {'uid': self.uid, 'path': '/disk/new_folder/folder2/fileRO'}
        self.mail_ok('info', opts)
        self.inspect_all()

    def test_readwrite_trash_append(self):
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid, rights=640)
        self.activate_invite(uid=self.uid_3, hash=hsh)
        self.upload_file(self.uid, '/disk/new_folder/folder2/fileRO')

        # =======================================================================
        # Set RW rights
        args = {
            'uid': self.uid,
            'user_uid': self.uid_3,
            'gid': gid,
            'rights': 660,
        }
        self.mail_ok('share_change_rights', args)
        #=======================================================================

        # Push file to trash
        opts = {'uid': self.uid_3, 'path': '/disk/folder2/fileRO'}
        with mock.patch('mpfs.core.address.Address.add_trash_suffix'):
            self.mail_ok('trash_append', opts)
        #=======================================================================

        # Check file in trash
        opts = {'uid': self.uid_3, 'path': '/trash/fileRO'}
        self.mail_ok('info', opts)
        #=======================================================================

        # Check file deleted
        opts = {'uid': self.uid_3, 'path': '/disk/folder2/fileRO'}
        self.mail_error('info', opts)
        #=======================================================================

        # Check file deleted from owner
        opts = {'uid': self.uid, 'path': '/disk/new_folder/folder2/fileRO'}
        self.mail_error('info', opts)
        #=======================================================================

        # Check file not in owner's trash
        opts = {'uid': self.uid, 'path': '/trash/fileRO'}
        self.mail_ok('info', opts)
        #=======================================================================
        self.inspect_all()
