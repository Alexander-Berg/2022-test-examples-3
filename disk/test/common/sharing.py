# -*- coding: utf-8 -*-
from lxml import etree


from test.base import DiskTestCase

from test.fixtures import users
from test.helpers.stubs.services import SearchIndexerStub, PushServicesStub
from test.helpers.stubs.manager import StubsManager

from mpfs.core.filesystem.dao.legacy import CollectionRoutedDatabase
from mpfs.core.services.search_service import DiskSearch


db = CollectionRoutedDatabase()


class CommonSharingMethods(DiskTestCase):
    uid = users.default_user.uid
    email = users.default_user.email
    uid_1 = users.user_1.uid
    email_1 = users.user_1.email
    uid_3 = users.user_3.uid
    email_3 = users.user_3.email
    uid_6 = users.user_6.uid
    email_6 = users.user_6.email
    uid_7 = users.user_7.uid
    email_7 = users.user_7.email
    email_cyrillic = users.email_cyrillic
    email_cyrillic_dots = users.email_cyrillic_dots
    email_dots = users.email_dots

    def invite_user(self, uid=None, owner=None, email=None, rights=660, path='/disk/new_folder/folder2/', ext_gid=None):

        if not path.endswith('/'):
            path += '/'

        resource_name = path.split('/')[-2]

        if uid is None:
            uid = self.uid_3
        if email is None:
            email = self.email_3
        if owner is None:
            owner = self.uid

        opts = {
            'uid': uid,
        }
        result = self.mail_ok('share_list_not_approved_folders', opts)
        not_approved_folders = len(list(result.iterchildren()))

        with PushServicesStub() as stub:
            args = {
                'rights': rights,
                'universe_login': email,
                'universe_service': 'email',
                'avatar': 'http://localhost/echo',
                'name': 'mpfs-test',
                'connection_id': '1234',
                'uid': owner,
                'path': path,
            }
            invite_result = self.json_ok('share_invite_user', args)
            pushes = stub.send.call_args_list
            actor_push = PushServicesStub.parse_send_call(pushes[0])
            owner_push = PushServicesStub.parse_send_call(pushes[1])

            assert owner_push['event_name'] == actor_push['event_name'] == 'share_invite_new'
            assert actor_push['json_payload']['root']['parameters']['for'] == 'actor'
            assert owner_push['json_payload']['root']['parameters']['for'] == 'owner'

        opts = {
            'uid': uid,
        }
        result = self.mail_ok('share_list_not_approved_folders', opts)
        #        self.fail(etree.tostring(result, pretty_print=True))
        children_length = len(list(result.iterchildren()))
        self.assertEqual(children_length, not_approved_folders + 1)
        good_result_unknown = ('ctime', 'size')
        good_result_defined = {
            'owner_name': 'Vasily P.',
            'hash': invite_result['hash'],
            'folder_name': resource_name,
            'owner_uid': owner,
            'status': 'proposed',
            'rights': str(rights),
            'gid': ext_gid,
        }
        found = False
        for folder in result.iterfind('folder'):
            if folder.find('hash').text == invite_result['hash']:
                for each in folder.iterchildren():
                    try:
                        if each.tag == 'gid' and not ext_gid:
                            ext_gid = each.text
                        else:
                            self.assertEqual(each.text, good_result_defined[each.tag])
                    except KeyError:
                        self.assertTrue(each.tag in good_result_unknown)
                found = True
        self.assertTrue(found)

        opts = {
            'hash': invite_result['hash'],
        }
        result = self.mail_ok('share_invite_info', opts)
        self.assertEqual(len(list(result.iterchildren('invite'))), 1)
        invite = result.find('invite')
        self.assertEqual(invite.find('owner_name').text, 'Vasily P.')
        self.assertEqual(invite.find('owner_uid').text, owner)
        self.assertEqual(invite.find('folder_name').text, resource_name)
        self.assertEqual(invite.find('rights').text, str(rights))
        self.assertNotEqual(invite.find('size'), None)

        #=======================================================================
        #
        invites_count = db.group_invites.find({'gid': ext_gid}).count()
        #=======================================================================
        #
        args = {
            'uid': owner,
            'gid': ext_gid,
            'rights': rights,
            'universe_login': email,
            'universe_service': 'email',
            'avatar': 'http://localhost/echo',
            'name': 'mpfs-test',
            'connection_id': '1234',
            'path': path,
        }
        result = self.mail_ok('share_invite_user', args)
        #=======================================================================
        #
        self.assertEqual(db.group_invites.find({'gid': ext_gid}).count(), invites_count)
        #=======================================================================
        return invite_result['hash']

    def activate_invite(self, uid=None, hash=None):
        args = {
            'hash': hash,
            'uid': uid,
        }
        return self.json_ok('share_activate_invite', args)

    def leave_group(self, uid, gid):
        args = {
            'gid': gid,
            'uid': uid,
        }
        return self.json_ok('share_leave_group', args)

    def reject_invite(self, uid=None, hash=None):
        args = {
            'hash': hash,
            'uid': uid,
        }
        return self.json_ok('share_reject_invite', args)

    def inspect_all(self, owner=None, user=None, do_assertion=True):
        # берём все счётчики владельца папки
        if not owner:
            owner = self.uid
        if not user:
            user = self.uid_3
        opts = {'uid': owner}
        result = self.json_ok('space', opts)
        disk_before_owner = result['used']
        trash_before_owner = result['trash']

        opts = {'uid': owner, 'meta': ''}
        group_folders_before = dict(
            [(x['path'], x['meta']['group']['size']) for x in self.json_ok('share_list_all_folders', opts)])

        #берём все счётчики вступившего в папку
        opts = {'uid': user}
        result = self.json_ok('space', opts)
        disk_before_user = result['used']
        trash_before_user = result['trash']

        #пересчитываем владельца
        opts = {'uid': owner}
        result = self.service_ok('inspect', opts)

        #пересчитываем вступившего
        opts = {'uid': user}
        result = self.service_ok('inspect', opts)

        #потом берём счётчики второй раз и сверяем
        opts = {'uid': owner}
        result = self.json_ok('space', opts)
        disk_after_owner = result['used']
        trash_after_owner = result['trash']

        opts = {'uid': user}
        result = self.json_ok('space', opts)
        disk_after_user = result['used']
        trash_after_user = result['trash']

        opts = {'uid': owner, 'meta': ''}
        group_folders_after = dict(
            [(x['path'], x['meta']['group']['size']) for x in self.json_ok('share_list_all_folders', opts)])

        if do_assertion:
            self.assertEqual(trash_before_owner, trash_after_owner)
            self.assertEqual(disk_before_owner, disk_after_owner)

            self.assertEqual(disk_before_user, disk_after_user)
            self.assertEqual(trash_before_user, trash_after_user)

            for path, size in group_folders_before.iteritems():
                self.assertEqual(size, group_folders_after[path])
        return (disk_before_owner, disk_before_user, disk_after_owner, disk_after_user)

    def make_dirs(self):
        args = {'uid': self.uid, 'path': '/disk/new_folder'}
        self.json_ok('mkdir', args)
        args = {'uid': self.uid, 'path': '/disk/new_folder/folder2'}
        self.json_ok('mkdir', args)
        args = {'uid': self.uid, 'path': '/disk/new_folder/folder2/folder3'}
        self.json_ok('mkdir', args)
        args = {'uid': self.uid, 'path': '/disk/new_folder/folder2/folder3/folder4'}
        self.json_ok('mkdir', args)
        args = {'uid': self.uid, 'path': '/disk/new_folder/folder2/folder3/folder4/folder5'}
        self.json_ok('mkdir', args)
        self.upload_file(self.uid, '/disk/new_folder/folder2/file3')
        self.upload_file(self.uid, '/disk/new_folder/folder2/folder3/file4')

    def create_group(self, uid=None, path='/disk/new_folder/folder2'):
        if path.endswith('/'):
            path = path[:-1]

        if uid is None:
            uid = self.uid

        # check space was changed
        disk_size_before = db.disk_info.find_one({'uid': uid, 'key': '/total_size'})['data']

        args = {'uid': uid, 'path': path}
        result = self.mail_ok('share_create_group', args)
        self.assertTrue(isinstance(result, etree._Element))

        gid = None
        for each in result.getchildren():
            if each.tag == 'gid' and each.text and isinstance(each.text, str):
                gid = each.text
        self.assertTrue(gid)
        self.mail_ok('info', args)
        disk_size_after = db.disk_info.find_one({'uid': uid, 'key': '/total_size'})['data']
        # =======================================================================
        self.assertEqual(disk_size_after, disk_size_before)
        gid2 = None
        result2 = self.mail_ok('share_create_group', args)
        for each in result2.getchildren():
            if each.tag == 'gid' and each.text and isinstance(each.text, str):
                gid2 = each.text
        self.assertTrue(gid2)
        self.assertEqual(gid, gid2)

        opts = {'uid': uid, 'path': path}
        listing_result = self.mail_ok('list', opts)
        folder = listing_result.find('folder-list').find('folder')
        folder_id = folder.find('id')
        self.assertNotEqual(folder_id, None)
        self.assertEqual(folder_id.text, path)
        self.assertNotEqual(folder.find('meta'), None)
        self.assertNotEqual(folder.find('meta').find('group'), None)
        group_vals = {
            'is_shared': '1',
            'is_owned': '1',
            'is_root': '1',
            'gid': gid,
        }
        for k, v in group_vals.iteritems():
            self.assertNotEqual(folder.find('meta').find('group').find(k), None, k)
            self.assertEqual(folder.find('meta').find('group').find(k).text, v, k)
        self.assertTrue(folder.find('meta').find('group').find('size').text.isdigit())
        owner = {}
        for each in folder.find('meta').find('group').find('owner').iterchildren():
            owner[each.tag] = each.text
        self.assertEqual(owner['uid'], uid)
        disk_size_after = db.disk_info.find_one({'uid': uid, 'key': '/total_size'})['data']
        self.assertEqual(disk_size_after, disk_size_before)

        return gid

    def publicate_resource(self, uid=None, path=None):
        opts = {'uid': uid, 'path': path}
        self.json_ok('set_public', opts)

        opts = {
            'uid': uid,
            'path': path,
            'meta': 'file_id,symlink,public,public_hash,short_url,short_url_named,download_counter',
        }
        original_resource = self.json_ok('info', opts)

        for k in ('public', 'public_hash', 'short_url', 'short_url_named',):
            self.assertTrue(k in original_resource['meta'], k)
        self.assertTrue(original_resource['meta']['public'], 1)

        return original_resource

    def create_share_for_guest(self, owner, path, guest, email):
        """Создать ОП ``path``, проинициализировать гостя ``guest`` и добавить его в ОП.

        :rtype: str
        """
        self.create_user(guest)
        self.xiva_subscribe(guest)
        gid = self.create_group(owner, path)
        hash_ = self.invite_user(uid=guest, owner=owner, email=email, path=path)
        self.activate_invite(uid=guest, hash=hash_)
        return gid


class SharingWithSearchTestCase(CommonSharingMethods):
    stubs_manager = StubsManager(class_stubs=set(StubsManager.DEFAULT_CLASS_STUBS) - {SearchIndexerStub})

    @classmethod
    def teardown_class(cls):
        for uid in (cls.uid_3, cls.uid, cls.uid_1):
            DiskSearch().delete(uid)
