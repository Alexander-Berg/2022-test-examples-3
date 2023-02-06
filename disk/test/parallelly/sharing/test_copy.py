# -*- coding: utf-8 -*-
import mpfs.engine.process

from test.common.sharing import CommonSharingMethods
from mpfs.core.filesystem.dao.legacy import CollectionRoutedDatabase


db = CollectionRoutedDatabase()


class CopySharingTestCase(CommonSharingMethods):

    def setup_method(self, method):
        super(CopySharingTestCase, self).setup_method(method)
        self.json_ok('user_init', {'uid': self.uid_1})
        self.json_ok('user_init', {'uid': self.uid_3})
        self.make_dirs()

    def test_copy_from_shared(self):
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid)
        self.activate_invite(uid=self.uid_3, hash=hsh)
        self.upload_file(self.uid_3, '/disk/folder2/file31')
        self.json_ok('mkdir', {'uid': self.uid_3, 'path': '/disk/folder2/folder_with_file'})
        self.upload_file(self.uid_3, '/disk/folder2/folder_with_file/file')


        #=======================================================================
        # Child file
        opts = {
            'uid': self.uid_3,
            'path': '/disk/folder00'
        }
        self.json_ok('mkdir', opts)
        opts = {'uid': self.uid_3, 'path': '/disk/folder00'}
        folder_info = self.mail_ok('info', opts)
        self.assertEqual(folder_info.find('folder').find('meta').find('group'), None)
        self.assertEqual(folder_info.find('folder').find('meta').find('with_shared'), None)
        opts = {
            'uid': self.uid_3,
            'path': '/disk/folder2/file31',
            'meta': '',
        }
        file_info = self.mail_ok('info', opts)
        self.assertEqual(file_info.find('file').find('meta').find('pmid'), None)
        opts = {
            'uid': self.uid_3,
            'src': '/disk/folder2/file31',
            'dst': '/disk/folder00/file31',
            'force': 1,
        }
        self.mail_ok('copy', opts)
        opts = {'uid' : self.uid_3, 'path' : '/disk/folder00/file31', 'meta' : ''}
        file_info = self.mail_ok('info', opts)
        opts = {'uid' : self.uid, 'path' : '/disk/new_folder/folder2/file31'}
        self.mail_ok('info', opts)
        opts = {'uid' : self.uid_3, 'path' : '/disk/folder2/file31'}
        self.mail_ok('info', opts)
#        self.fail(etree.tostring(file_info, pretty_print=True))
        self.assertEqual(file_info.find('file').find('meta').find('group'), None)
        self.assertEqual(file_info.find('file').find('meta').find('with_shared'), None)
        self.assertEqual(file_info.find('file').find('meta').find('pmid'), None)
        #=======================================================================
        # Child folder
        opts = {'uid': self.uid_3, 'path': '/disk/folder2/folder_with_file'}
        folder_list = self.mail_ok('list', opts).find('folder-list')
#        self.fail(etree.tostring(file_info, pretty_print=True))
        self.assertTrue(len(list(folder_list.find('folder').find('folder-list').iterchildren())) > 0)
        opts = {
            'uid': self.uid_3,
            'src': '/disk/folder2/folder_with_file',
            'dst': '/disk/folder_with_file',
        }
        self.mail_ok('copy', opts)
        opts = {'uid' : self.uid_3, 'path' : '/disk/folder_with_file'}
        folder_list = self.mail_ok('list', opts).find('folder-list')
        #self.fail(etree.tostring(folder_list, pretty_print=True))
        self.assertEqual(folder_list.find('folder').find('meta').find('group'), None)
        self.assertEqual(folder_list.find('folder').find('meta').find('with_shared'), None)
        self.assertTrue(len(list(folder_list.find('folder').find('folder-list').iterchildren())) > 0)
        #=======================================================================
        # Root folder
        opts = {
            'uid': self.uid_3,
            'src': '/disk/folder2',
            'dst': '/disk/folder5',
        }
        self.mail_ok('copy', opts)
        opts = {'uid': self.uid_3, 'path': '/disk/folder5'}
        folder_list = self.mail_ok('list', opts).find('folder-list')
#        self.fail(etree.tostring(folder_list, pretty_print=True))
        self.assertEqual(folder_list.find('folder').find('meta').find('group'), None)
        self.assertEqual(folder_list.find('folder').find('meta').find('with_shared'), None)
        self.assertTrue(len(list(folder_list.find('folder').find('folder-list').iterchildren())) > 0)
        opts = {'uid': self.uid, 'path': '/disk/new_folder/folder2'}
        self.mail_ok('info', opts)
        opts = {'uid': self.uid_3, 'path': '/disk/folder2'}
        self.mail_ok('info', opts)
        opts = {'uid': self.uid, 'path': '/disk/new_folder/folder2/file31'}
        self.mail_ok('info', opts)
        opts = {'uid': self.uid_3, 'path': '/disk/folder2/file31'}
        self.mail_ok('info', opts)
        #=======================================================================
        self.inspect_all()

