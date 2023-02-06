# -*- coding: utf-8 -*-
import mpfs.engine.process

from test.common.sharing import CommonSharingMethods
from mpfs.core.filesystem.dao.legacy import CollectionRoutedDatabase


db = CollectionRoutedDatabase()


class BrowseListSharingTestCase(CommonSharingMethods):

    def setup_method(self, method):
        super(BrowseListSharingTestCase, self).setup_method(method)

        from mpfs.core.services.stock_service import _setup
        _setup()

        self.json_ok('user_init', {'uid': self.uid_1})
        self.json_ok('user_init', {'uid': self.uid_3})
        self.make_dirs()

    def test_resource_ids(self):
        # https://st.yandex-team.ru/CHEMODAN-30922
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/f'})
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/f/f'})
        gid = self.json_ok('share_create_group', {'uid': self.uid, 'path': '/disk/f'})['gid']
        hsh = self.json_ok('share_invite_user', {'uid': self.uid, 'gid': gid, 'universe_login': self.email_3, 'universe_service': 'email'})['hash']
        self.json_ok('share_activate_invite', {'uid': self.uid_3, 'hash': hsh})

        resource_id_getter = lambda u, p: self.json_ok('info', {'uid': u, 'path': p, 'meta': 'resource_id'})['meta']['resource_id'].split(':')

        uid_in_folder = resource_id_getter(self.uid, '/disk/f')
        uid_share_folder = resource_id_getter(self.uid, '/disk/f/f')
        uid_3_in_folder = resource_id_getter(self.uid_3, '/disk/f')
        uid_3_share_folder = resource_id_getter(self.uid_3, '/disk/f/f')
        # проверка resource_id входной папки
        assert uid_in_folder[0] == self.uid
        assert uid_3_in_folder[0] == self.uid_3
        assert uid_3_in_folder[1] != uid_in_folder[1]
        # проверка общих ресурсов
        assert uid_share_folder == uid_3_share_folder == [self.uid, uid_share_folder[1]]

    def test_check_shared_in_child(self):
        self.create_group()

        args = {'uid': self.uid, 'path': '/disk/new_folder'}
        folder_info = self.mail_ok('info', args)
        self.assertNotEqual(folder_info.find('folder').find('meta').find('with_shared'), None)
        args = {'uid': self.uid, 'path': '/disk/new_folder_1'}
        self.json_ok('mkdir', args)
        folder_info = self.mail_ok('info', args)
        self.assertEqual(folder_info.find('folder').find('meta').find('with_shared'), None)
        child_found = False
        args = {'uid': self.uid, 'path': '/disk'}
        folder_list = self.mail_ok('list', args)
        for child in folder_list.find('folder-list').find('folder').find('folder-list').iterchildren():
            if child.find('id').text == '/disk/new_folder_1':
                child_found = True
                self.assertEqual(child.find('meta').find('with_shared'), None)
        self.assertTrue(child_found)

    def test_share_folder_info(self):
        gid = self.create_group()

        opts = {
            'uid': self.uid_3,
            'gid': gid,
        }
        self.mail_error('share_folder_info', opts)
        opts = {
            'uid': self.uid,
            'gid': gid,
        }
        result = self.mail_ok('share_folder_info', opts)
        self.assertEqual(result.find('folder').find('id').text, '/disk/new_folder/folder2')

    def test_group_in_group(self):
        self.create_group()

        args = {'uid': self.uid, 'path': '/disk/new_folder/folder2/folder3'}
        self.mail_error('share_create_group', args)

    def test_list_shared_folders(self):
        args = {'uid' : self.uid, 'path' : '/disk/new_folder/folder3'}
        self.json_ok('mkdir', args)

        gid = self.create_group(path='/disk/new_folder/folder2')
        gid2 = self.create_group(path='/disk/new_folder/folder3')

        opts = {'uid' : self.uid}
        result = self.mail_ok('share_list_all_folders', opts)
#        self.fail(etree.tostring(result, pretty_print=True))
        good_results = {
            gid: '/disk/new_folder/folder2',
            gid2: '/disk/new_folder/folder3',
        }
        for folder in result.iterfind('folder'):
            gid = folder.find('meta').find('group').find('gid').text
            self.assertTrue(gid in good_results)
            self.assertTrue(folder.find('id').text == good_results[gid])
            self.assertEqual('1', folder.find('meta').find('group').find('is_owned').text)
            good_results.pop(gid)
        self.assertEqual(good_results, {})
        #=======================================================================
        opts = {'uid': self.uid_3}
        result = self.mail_ok('share_list_all_folders', opts)
#        self.fail(etree.tostring(result, pretty_print=True))
        good_results = {
            gid: {
                'path': '/disk/folder2',
                'rights': str(660),
                'user_count': '2'
            },
        }
        for folder in result.iterfind('folder'):
            gid = folder.find('meta').find('group').find('gid').text
            self.assertTrue(gid in good_results)
            self.assertEqual(good_results[gid]['path'], folder.find('id').text)
            self.assertEqual(good_results[gid]['user_count'], folder.find('meta').find('group').find('user_count').text)
            self.assertEqual('0', folder.find('meta').find('group').find('is_owned').text)
            good_results.pop(gid)

        #=======================================================================
        opts = {'uid' : self.uid}
        result = self.mail_ok('share_list_owned_folders', opts)
        #=======================================================================
        opts = {'uid' : self.uid_3}
        result = self.mail_ok('share_list_joined_folders', opts)
        #=======================================================================
        self.inspect_all()

    def test_list_links(self):
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        from mpfs.core.social.share.processor import ShareProcessor
        sp = ShareProcessor()
        grps = sp.list_owned_groups(str(self.uid))
        lnks = sp.list_owned_links(str(self.uid_3))
        self.assertTrue(len(grps) > 0)
        self.assertTrue(len(lnks) > 0)

    def test_list_shared(self):
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        opts = {'uid': self.uid_3, 'path': '/disk/folder2'}
        listing_result = self.mail_ok('list', opts)

        folder = listing_result.find('folder-list').find('folder')
#        self.fail(etree.tostring(folder, pretty_print=True))
        folder_id = folder.find('id')
        self.assertNotEqual(folder_id, None)
        self.assertEqual(folder_id.text, '/disk/folder2')
        self.assertIsNotNone(folder.find('meta'))
        self.assertIsNotNone(folder.find('meta').find('group'))
        self.assertIsNotNone(folder.find('meta').find('group').find('user_count'))
        self.assertEqual('2', folder.find('meta').find('group').find('user_count').text)
        self.assertIsNone(folder.find('meta').find('with_shared'))
        group_vals = {
            'is_shared': '1',
            'is_owned': '0',
            'is_root': '1',
        }
        for k, v in group_vals.iteritems():
            self.assertNotEqual(folder.find('meta').find('group').find(k), None, k)
            self.assertEqual(folder.find('meta').find('group').find(k).text, v, k)
        self.assertTrue(folder.find('meta').find('group').find('size').text.isdigit())

        child_folder = listing_result.find('folder-list').find('folder').find('folder-list').find('folder')
        self.assertNotEqual(child_folder, None)
        self.assertNotEqual(child_folder.find('id'), None)
        self.assertEqual(child_folder.find('id').text, '/disk/folder2/folder3')
        self.assertNotEqual(child_folder.find('meta'), None)
        self.assertNotEqual(child_folder.find('meta').find('group'), None)
        group_vals = {
            'is_shared': '1',
            'is_owned': '0',
            'is_root': '0',
        }
        for k, v in group_vals.iteritems():
            self.assertNotEqual(child_folder.find('meta').find('group').find(k), None, k)
            self.assertEqual(child_folder.find('meta').find('group').find(k).text, v, k)
        self.assertEqual(child_folder.find('meta').find('group').find('size'), None)

        child_file = listing_result.find('folder-list').find('folder').find('folder-list').find('file')
        self.assertNotEqual(folder.find('meta'), None)
        self.assertNotEqual(folder.find('meta').find('group'), None)
        self.assertNotEqual(child_file, None)
        self.assertNotEqual(child_file.find('id'), None)
        self.assertEqual(child_file.find('id').text, '/disk/folder2/file3')
        self.assertNotEqual(child_file.find('meta'), None)
        self.assertNotEqual(child_file.find('meta').find('group'), None)
        group_vals = {
            'is_shared': '1',
            'is_owned': '0',
            'is_root': '0',
        }
        for k, v in group_vals.iteritems():
            self.assertNotEqual(child_file.find('meta').find('group').find(k), None, k)
            self.assertEqual(child_file.find('meta').find('group').find(k).text, v, k)
        self.assertEqual(child_file.find('meta').find('group').find('size'), None)

        opts = {'uid': self.uid_3, 'path': '/disk'}
        listing_result = self.mail_ok('list', opts)
        parent = listing_result.find('folder-list').find('folder')
#        self.fail(etree.tostring(parent, pretty_print=True))
        self.assertEqual(int(parent.find('meta').find('with_shared').text), 1)
        self.assertNotEqual(parent.find('meta').find('shared_rights'), None)

        opts = {'uid': self.uid, 'path': '/disk/new_folder'}
        listing_result = self.mail_ok('list', opts)
        parent = listing_result.find('folder-list').find('folder')
        self.assertEqual(int(parent.find('meta').find('with_shared').text), 1)
        self.assertNotEqual(parent.find('meta').find('shared_rights'), None)
        opts = {'uid': self.uid_3, 'path': '/disk/folder2'}
        listing_result = self.desktop('list', opts)
        for each in listing_result:
            self.assertTrue(each['id'].startswith('/disk/folder2'), each['id'])
        self.inspect_all()

    def test_timeline_shared(self):
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        opts = {'uid': self.uid_1, 'path': '/disk/folder0'}
        self.json_ok('mkdir', opts)
        self.upload_file(self.uid_1, '/disk/folder0/file3')
        self.create_group(uid=self.uid_1, path='/disk/folder0')

        args = {
            'uid': self.uid_1,
            'path': '/disk/folder0',
            'rights': 660,
            'universe_login': self.email_3,
            'universe_service': 'email',
            'avatar': 'http://localhost/echo',
            'name': 'mpfs-test-3',
            'connection_id': '12345',
        }
        result = self.mail_ok('share_invite_user', args)
#        self.fail(etree.tostring(result, pretty_print=True))
        hsh = None
        for each in result.getchildren():
            if each.tag == 'hash' and each.text and isinstance(each.text, str):
                hsh = each.text
        self.assertNotEqual(hsh, None)
        self.assertNotEqual(db.group_invites.find_one({'_id' : hsh}), None)
        self.inspect_all(self.uid_1, self.uid_3)
        args = {
            'hash': hsh,
            'uid': self.uid_3,
        }
        self.mail_ok('share_activate_invite', args)
        self.inspect_all(self.uid_1, self.uid_3)
        self.upload_file(self.uid_3, '/disk/folder0/file0')
        opts = {'uid': self.uid_3, 'path': '/disk/folder0/file0'}
        self.mail_ok('info', opts)
        self.inspect_all(self.uid_1, self.uid_3)

        opts = {'uid': self.uid_3, 'path': '/disk'}
        timeline_result = self.mail_ok('timeline', opts)
        list_length = len(list(timeline_result.find('folder-list').find('folder').find('folder-list').iterchildren()))
        self.assertTrue(list_length > 0, list_length)
        folder2_found = False
        folder0_found = False
        for element in timeline_result.find('folder-list').find('folder').find('folder-list').iterchildren():
            if element.find('id').text.startswith('/disk/folder2'):
                folder2_found = True
            elif element.find('id').text.startswith('/disk/folder0'):
                folder0_found = True
        self.assertTrue(folder2_found)
        self.assertTrue(folder0_found)

        opts = {'uid': self.uid_3, 'path': '/disk/folder2'}
        timeline_result = self.mail_ok('timeline', opts)
#        self.fail(etree.tostring(timeline_result, pretty_print=True))
        folder2_found = False
        for folder in timeline_result.find('folder-list').iterchildren():
            if folder.find('id').text == '/disk/folder2':
                for child in folder.find('folder-list').iterchildren():
                    if child.find('id').text.startswith('/disk/folder2'):
                        if child.find('id').text == '/disk/folder2/file0':
                            self.fail()
                        else:
                            folder2_found = True
                    else:
                        self.fail(child.find('id').text)
            else:
                self.fail()
        self.assertTrue(folder2_found)

    def test_list_shared_child(self):
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        opts = {'uid': self.uid_3, 'path': '/disk/folder2/folder3'}
        listing_result = self.mail_ok('list', opts)
#        self.fail(etree.tostring(listing_result, pretty_print=True))
        folder_id = listing_result.find('folder-list').find('folder').find('id')
        self.assertNotEqual(folder_id, None)
        self.assertEqual(folder_id.text, '/disk/folder2/folder3')
        child_folder = listing_result.find('folder-list').find('folder').find('folder-list').find('folder')
        self.assertNotEqual(child_folder, None)
        self.assertNotEqual(child_folder.find('id'), None)
        self.assertEqual(child_folder.find('id').text, '/disk/folder2/folder3/folder4')
        child_file = listing_result.find('folder-list').find('folder').find('folder-list').find('file')
        self.assertNotEqual(child_file, None)
        self.assertNotEqual(child_file.find('id'), None)
        self.assertEqual(child_file.find('id').text, '/disk/folder2/folder3/file4')
        self.inspect_all()

    def test_list_missing_folders(self):
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid, rights=640)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        opts = {'uid': self.uid, 'path': '/disk'}
        result = self.json_ok('list', opts)
        for item in result:
            if item.get('path').startswith('/disk/new_folder'):
                self.json_ok('rm', {'uid': self.uid, 'path': item.get('path')})

        opts = {'uid': self.uid_3, 'path': '/disk'}
        result = self.json_ok('list', opts)
        for item in result:
            if item.get('path').startswith('/disk/folder77/folder88/folder2'):
                self.json_ok('rm', {'uid': self.uid_3, 'path': item.get('path')})


class BrowseTreeSharingTestCase(CommonSharingMethods):

    def setup_method(self, method):
        super(BrowseTreeSharingTestCase, self).setup_method(method)

        from mpfs.core.services.stock_service import _setup
        _setup()

        self.json_ok('user_init', {'uid': self.uid_1})
        self.json_ok('user_init', {'uid': self.uid_3})
        self.make_dirs()

    def test_tree_shared(self):
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        opts = {'uid': self.uid_3, 'path': '/disk', 'deep_level': 3, 'meta': 'hasfolders'}
        tree_result = self.mail_ok('tree', opts)
#        self.fail(etree.tostring(tree_result, pretty_print=True))
        found = False
        for folder in tree_result.find('folder-tree').find('folder').find('folder-tree').iterfind('folder'):
            if not (folder.find('id').text == '/disk/folder2'):
                continue
            else:
                self.assertEqual(folder.find('hasfolders').text, '1')
                self.assertNotEqual(folder.find('meta'), None)
                self.assertNotEqual(folder.find('meta').find('group'), None)
                self.assertTrue(len(list(folder.find('folder-tree').iterfind('folder'))) > 0)
                found = True
        self.assertTrue(found)
        opts = {'uid': self.uid_3, 'path': '/disk/folder2', 'meta': 'hasfolders'}
        tree_result = self.mail_ok('tree', opts)
#        self.fail(etree.tostring(tree_result, pretty_print=True))
        self.assertTrue(tree_result.find('folder-tree').find('folder').find('id').text == '/disk/folder2')
        self.assertTrue(len(list(tree_result.find('folder-tree').find('folder').find('folder-tree').iterfind('folder'))) == 1)
        self.assertEqual(tree_result.find('folder-tree').find('folder').find('folder-tree').find('folder').find('id').text, '/disk/folder2/folder3')
        self.assertNotEqual(tree_result.find('folder-tree').find('folder').find('meta'), None)
        self.assertNotEqual(tree_result.find('folder-tree').find('folder').find('meta').find('group'), None)
        self.assertNotEqual(tree_result.find('folder-tree').find('folder').find('folder-tree').find('folder').find('meta'), None)
        self.assertNotEqual(tree_result.find('folder-tree').find('folder').find('folder-tree').find('folder').find('meta').find('group'), None)
        self.assertEqual(int(tree_result.find('folder-tree').find('folder').find('folder-tree').find('folder').find('hasfolders').text), 1)
        self.inspect_all()

    def test_tree_shared_child(self):
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        opts = {'uid': self.uid_3, 'path': '/disk/folder2/folder3', 'meta': 'hasfolders'}
        tree_result = self.mail_ok('tree', opts)
#        self.fail(etree.tostring(tree_result, pretty_print=True))
        self.assertEqual(tree_result.find('folder-tree').find('folder').find('id').text, '/disk/folder2/folder3')
        self.assertTrue(len(list(tree_result.find('folder-tree').find('folder').find('folder-tree').iterfind('folder'))) > 0)
        self.assertEqual(tree_result.find('folder-tree').find('folder').find('folder-tree').find('folder').find('id').text, '/disk/folder2/folder3/folder4')
        self.assertEqual(tree_result.find('folder-tree').find('folder').find('folder-tree').find('folder').find('hasfolders').text, '1')
        self.assertNotEqual(tree_result.find('folder-tree').find('folder').find('meta'), None)
        self.assertNotEqual(tree_result.find('folder-tree').find('folder').find('meta').find('group'), None)
        self.assertNotEqual(tree_result.find('folder-tree').find('folder').find('folder-tree').find('folder').find('meta'), None)
        self.assertNotEqual(tree_result.find('folder-tree').find('folder').find('folder-tree').find('folder').find('meta').find('group'), None)
        self.inspect_all()

    def test_tree_group(self):
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        opts = {'uid': self.uid, 'path': '/disk/new_folder', 'meta': 'hasfolders'}
        tree_result = self.mail_ok('tree', opts)
#        self.fail(etree.tostring(tree_result, pretty_print=True))
        found = False
        for folder in tree_result.find('folder-tree').find('folder').find('folder-tree').iterfind('folder'):
            if folder.find('id').text == '/disk/new_folder/folder3':
                continue
            elif folder.find('id').text == '/disk/new_folder/folder2':
                self.assertEqual(folder.find('hasfolders').text, '1')
                self.assertNotEqual(folder.find('meta'), None)
                self.assertNotEqual(folder.find('meta').find('group'), None)
                found = True
            else:
                self.fail()
        self.assertTrue(found)

        opts = {'uid': self.uid, 'path': '/disk/new_folder/folder2', 'meta': 'hasfolders'}
        tree_result = self.mail_ok('tree', opts)
#        self.fail(etree.tostring(tree_result, pretty_print=True))
        self.assertTrue(tree_result.find('folder-tree').find('folder').find('id').text == '/disk/new_folder/folder2')
        self.assertTrue(len(list(tree_result.find('folder-tree').find('folder').find('folder-tree').iterfind('folder'))) == 1)
        self.assertEqual(tree_result.find('folder-tree').find('folder').find('folder-tree').find('folder').find('id').text, '/disk/new_folder/folder2/folder3')
        self.assertEqual(int(tree_result.find('folder-tree').find('folder').find('folder-tree').find('folder').find('hasfolders').text), 1)
        self.assertNotEqual(tree_result.find('folder-tree').find('folder').find('meta'), None)
        self.assertNotEqual(tree_result.find('folder-tree').find('folder').find('meta').find('group'), None)
        self.assertNotEqual(tree_result.find('folder-tree').find('folder').find('folder-tree').find('folder').find('meta'), None)
        self.assertNotEqual(tree_result.find('folder-tree').find('folder').find('folder-tree').find('folder').find('meta').find('group'), None)
        self.inspect_all()

    def test_tree_group_child(self):
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        opts = {'uid': self.uid_3, 'path': '/disk/folder2/folder3', 'meta': 'hasfolders'}
        tree_result = self.mail_ok('tree', opts)
#        self.fail(etree.tostring(tree_result, pretty_print=True))
        self.assertEqual(tree_result.find('folder-tree').find('folder').find('id').text, '/disk/folder2/folder3')
        self.assertTrue(len(list(tree_result.find('folder-tree').find('folder').find('folder-tree').iterfind('folder'))) > 0)
        self.assertEqual(tree_result.find('folder-tree').find('folder').find('folder-tree').find('folder').find('id').text, '/disk/folder2/folder3/folder4')
        self.assertEqual(tree_result.find('folder-tree').find('folder').find('folder-tree').find('folder').find('hasfolders').text, '1')
        self.assertNotEqual(tree_result.find('folder-tree').find('folder').find('folder-tree').find('folder').find('meta'), None)
        self.assertNotEqual(tree_result.find('folder-tree').find('folder').find('folder-tree').find('folder').find('meta').find('group'), None)
        self.assertNotEqual(tree_result.find('folder-tree').find('folder').find('meta'), None)
        self.assertNotEqual(tree_result.find('folder-tree').find('folder').find('meta').find('group'), None)
        self.inspect_all()

    def test_tree_with_parent(self):
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        opts = {
            'uid': self.uid_3,
            'path': '/disk/folder1',
        }
        self.json_ok('mkdir', opts)
        opts = {
            'uid': self.uid_3,
            'path': '/disk/folder1/folder11',
        }
        self.json_ok('mkdir', opts)
        opts = {
            'uid': self.uid_3,
            'path': '/disk/folder2',
            'deep_level': 3,
            'parents': 1,
            'meta': 'hasfolders',
        }
        tree_result = self.mail_ok('tree', opts)
#        self.fail(etree.tostring(tree_result, pretty_print=True))
        found = False
        self.assertEqual(tree_result.find('folder-tree').find('folder').find('id').text, '/disk')
        self.assertTrue(len(list(tree_result.find('folder-tree').find('folder').find('folder-tree').iterfind('folder'))) > 1)
        for folder in tree_result.find('folder-tree').find('folder').find('folder-tree').iterfind('folder'):
            if not (folder.find('id').text == '/disk/folder2'):
                continue
            else:
                # self.assertEqual(folder.find('hasfolders').text, '1')
                self.assertNotEqual(folder.find('meta'), None)
                self.assertNotEqual(folder.find('meta').find('group'), None)
                self.assertTrue(len(list(folder.find('folder-tree').iterfind('folder'))) > 0)
                found = True
        self.assertTrue(found)
        opts = {
            'uid': self.uid_3,
            'path': '/disk',
            'deep_level': 3,
            'parents': 1,
            'meta': 'hasfolders',
        }
        tree_result = self.mail_ok('tree', opts)
        opts = {
            'uid': self.uid_3,
            'path': '/',
        }
        tree_result = self.mail_ok('services', opts)
        opts = {
            'uid': self.uid_3,
            'path': '/disk/folder2/folder3',
            'parents': 1,
            'meta': 'hasfolders'
        }
        tree_result = self.mail_ok('tree', opts)
        self.inspect_all()

    def test_tree_for_parent_readonly_folder(self):
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid, rights=640)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        opts = {
            'uid': self.uid_3,
            'path': '/disk/folder77',
            'connection_id': '12345',
        }
        self.json_ok('mkdir', opts)
        opts = {
            'uid': self.uid_3,
            'path': '/disk/folder77/folder88',
            'connection_id': '12345',
        }
        self.json_ok('mkdir', opts)
        self.assertNotEqual(db.group_links.find_one({'uid': str(self.uid_3), 'path': '/disk/folder2'}), None)
        self.assertNotEqual(db.user_data.find_one({'uid': str(self.uid_3), 'key': '/disk/folder2'}), None)
        opts = {
            'uid': self.uid_3,
            'src': '/disk/folder2',
            'dst': '/disk/folder77/folder88/folder2',
            'connection_id': '12345',
        }
        self.mail_ok('async_move', opts)
        self.assertNotEqual(db.group_links.find_one({'uid': str(self.uid_3), 'path': '/disk/folder77/folder88/folder2'}), None)
        opts = {'uid': self.uid_3, 'path': '/disk/folder77/folder88', 'deep_level': 3, 'meta': 'hasfolders'}
        tree_result = self.mail_ok('tree', opts)
#        self.fail(etree.tostring(tree_result, pretty_print=True))
        found_folder2 = False
        for folder in tree_result.find('folder-tree').iterfind('folder'):
            if folder.find('id').text == '/disk/folder77/folder88':
                self.assertEqual(folder.find('hasfolders').text, '1')
                self.assertNotEqual(folder.find('meta'), None)
                for child in folder.find('folder-tree').iterfind('folder'):
                    if child.find('id').text == '/disk/folder77/folder88/folder2':
                        self.assertTrue(len(list(child.find('folder-tree').iterchildren())) > 0)
                        self.assertNotEqual(child.find('meta').find('group'), None)
                        found_folder2 = True
                    else:
                        self.fail(child.find('id').text)
            else:
                self.fail()
        self.assertTrue(found_folder2)
        opts = {
            'uid': self.uid,
            'path': '/disk/new_folder/folder2/folder3/folder300',
            'connection_id': '12345',
        }
        self.json_ok('mkdir', opts)
        opts = {'uid': self.uid_3, 'path': '/disk/folder77/folder88/folder2', 'deep_level': 3, 'meta': 'hasfolders'}
        tree_result = self.mail_ok('tree', opts)
#        self.fail(etree.tostring(tree_result, pretty_print=True))
        found_folder3 = False
        for folder in tree_result.find('folder-tree').iterfind('folder'):
            if folder.find('id').text == '/disk/folder77/folder88/folder2':
                self.assertEqual(folder.find('hasfolders').text, '1')
                self.assertNotEqual(folder.find('meta'), None)
                self.assertNotEqual(folder.find('meta').find('group'), None)
                for child in folder.find('folder-tree').iterfind('folder'):
                    if child.find('id').text == '/disk/folder77/folder88/folder2/folder3':
                        self.assertEqual(len(list(child.find('folder-tree').iterchildren())), 2)
                        self.assertNotEqual(child.find('meta').find('group'), None)
                        found_folder3 = True
                    else:
                        self.fail(child.find('id').text)
            else:
                self.fail()
        self.assertTrue(found_folder3)

        opts = {'uid': self.uid_3, 'path': '/disk/folder77/folder88/folder2', 'meta': 'hasfolders'}
        tree_result = self.mail_ok('tree', opts)
#        self.fail(etree.tostring(tree_result, pretty_print=True))
        found_folder3 = False
        for folder in tree_result.find('folder-tree').iterfind('folder'):
            if folder.find('id').text == '/disk/folder77/folder88/folder2':
                self.assertEqual(folder.find('hasfolders').text, '1')
                self.assertNotEqual(folder.find('meta'), None)
                self.assertNotEqual(folder.find('meta').find('group'), None)
                for child in folder.find('folder-tree').iterfind('folder'):
                    if child.find('id').text == '/disk/folder77/folder88/folder2/folder3':
                        self.assertTrue(int(child.find('hasfolders').text) > 0)
                        self.assertNotEqual(child.find('meta').find('group'), None)
                        found_folder3 = True
                    else:
                        self.fail(child.find('id').text)
            else:
                self.fail()
        self.assertTrue(found_folder3)

        opts = {'uid': self.uid_3, 'path': '/disk/folder77/folder88', 'meta': 'hasfolders'}
        tree_result = self.mail_ok('tree', opts)
#        self.fail(etree.tostring(tree_result, pretty_print=True))
        found_folder2 = False
        for folder in tree_result.find('folder-tree').iterfind('folder'):
            if folder.find('id').text == '/disk/folder77/folder88':
                self.assertEqual(folder.find('hasfolders').text, '1')
                self.assertNotEqual(folder.find('meta'), None)
                for child in folder.find('folder-tree').iterfind('folder'):
                    if child.find('id').text == '/disk/folder77/folder88/folder2':
                        self.assertTrue(int(child.find('hasfolders').text) > 0)
                        self.assertNotEqual(child.find('meta').find('group'), None)
                        found_folder2 = True
                    else:
                        self.fail(child.find('id').text)
            else:
                self.fail()
        self.assertTrue(found_folder2)

    def test_tree_group_in_disk(self):
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        opts = {'uid': self.uid_1, 'path': '/disk/folder)(6)*)'}
        self.json_ok('mkdir', opts)
        opts = {'uid': self.uid_1, 'path': '/disk/folder)(6)*)/folder71/'}
        self.json_ok('mkdir', opts)

        gid_1 = self.create_group(uid=self.uid_1, path='/disk/folder)(6)*)')
        hsh = self.invite_user(uid=self.uid_3, owner=self.uid_1, email=self.email_3, ext_gid=gid_1, path='/disk/folder)(6)*)/')
        self.activate_invite(uid=self.uid_3, hash=hsh)

        opts = {'uid': self.uid_1, 'path': '/disk', 'meta': 'hasfolders'}
        tree_result = self.mail_ok('tree', opts)
        # self.fail(etree.tostring(tree_result, pretty_print=True))
        found = False
        for folder in tree_result.find('folder-tree').find('folder').find('folder-tree').iterfind('folder'):
            if folder.find('id').text == '/disk/folder)(6)*)':
                self.assertEqual(folder.find('hasfolders').text, '1')
                self.assertNotEqual(folder.find('meta'), None)
                self.assertNotEqual(folder.find('meta').find('group'), None)
                found = True
            else:
                continue
        self.assertTrue(found)

        opts = {'uid': self.uid_1, 'path': '/disk/folder)(6)*)/folder71/folder81'}
        self.json_ok('mkdir', opts)

        opts = {'uid': self.uid_1, 'path': '/disk/folder)(6)*)', 'meta': 'hasfolders'}
        tree_result = self.mail_ok('tree', opts)
        #        self.fail(etree.tostring(tree_result, pretty_print=True))
        self.assertTrue(tree_result.find('folder-tree').find('folder').find('id').text == '/disk/folder)(6)*)')
        self.assertTrue(
            len(list(tree_result.find('folder-tree').find('folder').find('folder-tree').iterfind('folder'))) == 1)
        self.assertEqual(
            tree_result.find('folder-tree').find('folder').find('folder-tree').find('folder').find('id').text,
            '/disk/folder)(6)*)/folder71')
        self.assertEqual(int(
            tree_result.find('folder-tree').find('folder').find('folder-tree').find('folder').find('hasfolders').text),
                         1)
        self.assertNotEqual(tree_result.find('folder-tree').find('folder').find('meta'), None)
        self.assertNotEqual(tree_result.find('folder-tree').find('folder').find('meta').find('group'), None)
        self.assertNotEqual(
            tree_result.find('folder-tree').find('folder').find('folder-tree').find('folder').find('meta'), None)
        self.assertNotEqual(
            tree_result.find('folder-tree').find('folder').find('folder-tree').find('folder').find('meta').find(
                'group'), None)
        self.inspect_all()


