# -*- coding: utf-8 -*-
import mpfs.engine.process

from test.common.sharing import CommonSharingMethods
from mpfs.core.filesystem.dao.legacy import CollectionRoutedDatabase
from mpfs.metastorage.mongo.collections.changelog import ChangelogCollection
from test.conftest import INIT_USER_IN_POSTGRES

db = CollectionRoutedDatabase()


class DiffSharingTestCase(CommonSharingMethods):

    def setup_method(self, method):
        super(DiffSharingTestCase, self).setup_method(method)
        self.json_ok('user_init', {'uid': self.uid_1})
        self.json_ok('user_init', {'uid': self.uid_3})
        self.make_dirs()

    def test_diff(self):
        args = {'uid': self.uid_3, 'path': '/disk/myfolder'}
        self.json_ok('mkdir', args)

        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid)

        opts = {
            'uid': self.uid_3,
            'path': '/disk'
        }
        result = self.desktop('diff', opts)
        first_version = result['version']

        self.activate_invite(uid=self.uid_3, hash=hsh)

        opts = {
            'uid': self.uid_3,
            'path': '/disk',
            'version': first_version,
        }
        result = self.desktop('diff', opts)
        activation_version = result['version']
#        self.fail(result)
        self.assertTrue(int(first_version) < int(activation_version))
        self.assertEqual(len(result['result']), 6)
        for each in result['result']:
            self.assertTrue(each['key'].startswith('/disk/folder2'))
            self.assertEqual(each['op'], 'new')
        #=======================================================================
        # kick user
        args = {
            'uid': self.uid,
            'user_uid': self.uid_3,
            'gid': gid,
        }
        self.mail_ok('share_kick_from_group', args)
        #=======================================================================
        # check folder deleted
        opts = {
            'uid': self.uid_3,
            'path': '/disk',
            'version': activation_version,
        }
        result = self.desktop('diff', opts)
#        self.fail(result)
        self.assertEqual(len(result['result']), 1)
        self.assertEqual(result['result'][0]['key'], '/disk/folder2')
        self.assertEqual(result['result'][0]['op'], 'deleted')
        #=======================================================================
        # RO rights
        opts = {
            'uid': self.uid_3,
            'path': '/disk'
        }
        result = self.desktop('diff', opts)
        first_version = result["version"]

        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid, rights=640)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        opts = {
            'uid': self.uid_3,
            'path': '/disk',
            'version': first_version,
        }
        result = self.desktop('diff', opts)
#        self.fail(result)
        self.assertTrue(int(first_version) < int(result['version']))
        self.assertEqual(len(result['result']), 6)
        for each in result['result']:
            self.assertTrue(each['key'].startswith('/disk/folder2'))
            self.assertEqual(each['op'], 'new')

    def test_unbalanced_parenthesis_diff(self):
        """
            https://jira.yandex-team.ru/browse/CHEMODAN-11171
        """
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        opts = {
            'uid': self.uid,
            'path': '/disk/folder.*))',
        }
        self.json_ok('mkdir', opts)
        opts = {
            'uid': self.uid,
            'path': '/disk/folder.*))/folder2',
        }
        self.json_ok('mkdir', opts)
        opts = {
            'uid': self.uid,
            'path': '/disk/folder.*))/folder2/folder3',
        }
        self.json_ok('mkdir', opts)

        opts = {
            'uid': self.uid_3,
            'path': '/disk',
        }
        uid_3_version = self.json_ok('diff', opts)['version']

        gid = self.create_group(path='/disk/folder.*))/folder2')

        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, rights=660, path='/disk/folder.*))/folder2/', ext_gid=gid)
        args = {
            'hash': hsh,
            'uid': self.uid_3,
        }
        self.mail_ok('share_activate_invite', args)

        opts = {
            'uid': self.uid_3,
            'path': '/disk',
        }
        self.json_ok('diff', opts)

        opts = {
            'uid': self.uid_3,
            'path': '/disk',
            'version': uid_3_version
        }
        self.json_ok('diff', opts)

    # https://st.yandex-team.ru/CHEMODAN-39443
    def test_group_link_wihtout_participant_shared_folder_desktop_diff_does_not_fail(self):
        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid)
        self.mail_ok('share_activate_invite', {'hash': hsh, 'uid': self.uid_3})
        db['user_data'].remove({'uid': self.uid_3, 'key': '/disk/folder2'})

        self.json_ok('diff', {'uid': self.uid_3, 'path': '/disk'})


class DiffWithSharedFolderTestCase(CommonSharingMethods):
    def setup_method(self, method):
        super(DiffWithSharedFolderTestCase, self).setup_method(method)

        user_init_params = {'uid': self.uid_3}
        if INIT_USER_IN_POSTGRES:
            user_init_params['shard'] = 'pg'

        self.json_ok('user_init', user_init_params)

        self.files_count = 6

    def prepare_file_structure(self):
        """
        Создаем такую структуру:

        /
        |
        ---- disk
               |
               -----shared-folder
                           |
                           |----shared-subfolder
                           |          |
                           |          |---file-1.txt
                           |          |---file-2.txt
                           |          |---file-3.txt
                           |
                           |----file-1.txt
                           |----file-2.txt
                           |----file-3.txt

        Далее делаем shared-folder общей, приглашаем пользователя uid_3 и переименовываем shared-folder в member-folder
        И проверяем ручки diff версионный и полный.
        """
        resources = set()
        shared_folder_path = '/disk/shared-folder'
        shared_subfolder_path = shared_folder_path + '/shared-subfolder'
        new_shared_folder_path = '/disk/member-folder'

        self.json_ok('mkdir', {'uid': self.uid, 'path': shared_folder_path})
        resources.add(shared_folder_path)

        for i in xrange(self.files_count/2):
            file_path = shared_folder_path + '/file-%d.txt' % i
            self.upload_file(self.uid, file_path)
            resources.add(file_path)

        self.json_ok('mkdir', {'uid': self.uid, 'path': shared_subfolder_path})
        resources.add(shared_subfolder_path)

        for i in xrange(self.files_count/2):
            file_path = shared_subfolder_path + '/file-%d.txt' % i
            self.upload_file(self.uid, file_path)
            resources.add(file_path)

        gid = self.create_group(path=shared_folder_path)
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid, path=shared_folder_path)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        # переименуем папку у участника, чтобы было проще различать папку владельца и участника
        self.json_ok('move', {'uid': self.uid_3, 'src': shared_folder_path, 'dst': new_shared_folder_path})

        # возвращаем список ресурсов у участника (подменяем путь)
        return {r.replace(shared_folder_path, new_shared_folder_path) for r in resources}

    def test_full_diff_for_shared_folder(self):
        resources = self.prepare_file_structure()

        full_diff = self.json_ok('diff', {'uid': self.uid_3})
        assert full_diff['amount'] > self.files_count + 1  # 1 - это сама папка, больше - потому что есть еще дефолтные

        full_diff_resources = {item['key'] for item in full_diff['result']}
        assert resources <= full_diff_resources

    def test_version_diff_for_shared_folder(self):
        resources = self.prepare_file_structure()

        versions = list(ChangelogCollection().find({'uid': self.uid_3}))
        versions.sort(key=lambda i: i['version'])

        full_diff = self.json_ok('diff', {'uid': self.uid_3, 'version': versions[0]['version']})
        assert full_diff['amount'] > self.files_count + 1  # 1 - это сама папка, больше - потому что есть еще дефолтные

        full_diff_resources = {item['key'] for item in full_diff['result']}
        assert resources < full_diff_resources
