# -*- coding: utf-8 -*-
import os

from test.common.sharing import CommonSharingMethods


class DownloadFileGroupAsArchiveTestCase(CommonSharingMethods):
    TEST_FOLDER_PATH = '/disk/test-folder'

    TEST_RESOURCE_NAME_1 = 'test-file-1.jpg'
    TEST_RESOURCE_PATH_1 = '/'.join([TEST_FOLDER_PATH, TEST_RESOURCE_NAME_1])

    TEST_RESOURCE_NAME_2 = 'test-file-2.jpg'
    TEST_RESOURCE_PATH_2 = '/'.join([TEST_FOLDER_PATH, TEST_RESOURCE_NAME_2])

    TEST_RESOURCE_PATH_3 = '/'.join(['/disk', TEST_RESOURCE_NAME_1])
    TEST_RESOURCE_NAME_1_DOUBLE = 'test-file-1 (1).jpg'

    def setup_method(self, method):
        super(DownloadFileGroupAsArchiveTestCase, self).setup_method(method)

        self.json_ok('mkdir', opts={'uid': self.uid, 'path': self.TEST_FOLDER_PATH})
        self.upload_file(self.uid, self.TEST_RESOURCE_PATH_1)
        self.upload_file(self.uid, self.TEST_RESOURCE_PATH_2)
        self.upload_file(self.uid, self.TEST_RESOURCE_PATH_3)

    def test_archive_prepare(self):
        opts = {'uid': self.uid}
        json = {
            'items': [self.TEST_RESOURCE_PATH_1, self.TEST_RESOURCE_PATH_2]
        }

        resp = self.json_ok('bulk_download_prepare', opts=opts, json=json)
        assert 'download_url' in resp
        assert 'oid' in resp

    def test_archive_prepare_with_non_existing_files(self):
        opts = {'uid': self.uid}
        json = {
            'items': [self.TEST_RESOURCE_PATH_1, self.TEST_RESOURCE_PATH_2, '/disk/no-such-file-in-disk']
        }

        resp = self.json_ok('bulk_download_prepare', opts=opts, json=json)
        assert 'download_url' in resp
        assert 'oid' in resp

    def test_archive_prepare_with_incorrect_path(self):
        opts = {'uid': self.uid}
        json = {
            'items': [self.TEST_RESOURCE_PATH_1, self.TEST_RESOURCE_PATH_2, '123:321:incorrect']
        }

        self.json_error('bulk_download_prepare', opts=opts, json=json, code=77)

    def test_find_operation(self):
        opts = {'uid': self.uid}
        json = {
            'items': [self.TEST_RESOURCE_PATH_1, self.TEST_RESOURCE_PATH_2]
        }
        resp = self.json_ok('bulk_download_prepare', opts=opts, json=json)

        opts['oid'] = resp['oid']
        resp = self.json_ok('bulk_download_list', opts)
        assert 'list' in resp

        self.assertIn(self.TEST_RESOURCE_NAME_1, [item['this']['name'] for item in resp['list']])
        self.assertIn(self.TEST_RESOURCE_NAME_2, [item['this']['name'] for item in resp['list']])

    def test_auto_suffixator(self):
        opts = {'uid': self.uid}
        json = {
            'items': [self.TEST_RESOURCE_PATH_1, self.TEST_RESOURCE_PATH_3]
        }
        resp = self.json_ok('bulk_download_prepare', opts=opts, json=json)

        opts['oid'] = resp['oid']
        resp = self.json_ok('bulk_download_list', opts)

        names = set([item['this']['name'] for item in resp['list']])
        correct_values = {self.TEST_RESOURCE_NAME_1, self.TEST_RESOURCE_NAME_1_DOUBLE}

        assert names == correct_values

    def test_shared_subfolder(self):
        subfolder_path = os.path.join(self.TEST_FOLDER_PATH, 'subfolder')
        self.json_ok('mkdir', opts={'uid': self.uid, 'path': subfolder_path})
        file_1_path = os.path.join(subfolder_path, 'file1.txt')
        file_2_path = os.path.join(subfolder_path, 'file2.txt')
        self.upload_file(self.uid, file_1_path)
        self.upload_file(self.uid, file_2_path)

        self.create_user(self.uid_3)
        hsh = self.invite_user(uid=self.uid_3, owner=self.uid, email=self.email_3, rights=660,
                               path='%s/' % self.TEST_FOLDER_PATH)
        self.activate_invite(uid=self.uid_3, hash=hsh)

        # переименуем папку у гостя для усугубления теста
        new_guest_folder_path = '/disk/guest'
        self.json_ok('move', {'uid': self.uid_3, 'src': self.TEST_FOLDER_PATH, 'dst': new_guest_folder_path})
        subfolder_path = os.path.join(new_guest_folder_path, 'subfolder')
        file_1_path = os.path.join(subfolder_path, 'file1.txt')
        file_2_path = os.path.join(subfolder_path, 'file2.txt')

        opts = {'uid': self.uid_3}
        json = {'items': [file_1_path, file_2_path]}
        resp = self.json_ok('bulk_download_prepare', opts=opts, json=json)

        opts['oid'] = resp['oid']
        resp = self.json_ok('bulk_download_list', opts)

        names = set([item['this']['name'] for item in resp['list']])
        correct_values = {os.path.basename(file_1_path), os.path.basename(file_2_path)}

        assert names == correct_values

    def test_attach(self):
        self.upload_file(self.uid, '/attach/1.jpg')
        attach_listing = self.json_ok('list', {'uid': self.uid, 'path': '/attach'})
        actual_path = [x for x in attach_listing if x['type'] == 'file'][0]['path']
        prepare_response = self.json_ok('bulk_download_prepare', opts={'uid': self.uid}, json={'items': [actual_path]})
        assert 'download_url' in prepare_response
        assert 'oid' in prepare_response
        list_response = self.json_ok('bulk_download_list', {'uid': self.uid, 'oid': prepare_response['oid']})
        assert list_response['list'][0]['this']['id'] == actual_path

    def test_public_bulk_download_prepare_only_files_support(self):
        uid = self.uid
        uid2 = self.user_2.uid
        self.create_user(uid2)
        self.json_ok('mkdir', {
            'uid': uid,
            'path': '/disk/public_folder',
        })
        self.json_ok('mkdir', {
            'uid': uid,
            'path': '/disk/public_folder/test_folder'
        })
        self.upload_file(uid, '/disk/public_folder/test_1.jpg')
        self.upload_file(uid, '/disk/public_folder/test_2.jpg')
        self.upload_file(uid, '/disk/public_folder/test_3.jpg')

        result = self.json_ok('set_public', {
            'uid': uid,
            'path': '/disk/public_folder',
        })
        public_hash = result['hash']
        result = self.json_ok('public_bulk_download_prepare', {
            'uid': uid2
        }, json={
            'items': [
                public_hash + ':/test_1.jpg',
                public_hash + ':/test_3.jpg',
                public_hash + ':/test_folder',
            ]
        })
        oid = result['oid']
        result = self.json_ok('bulk_download_list', {
            'uid': uid2,
            'oid': oid
        })
        lst = result['list']
        # только файлы поддерживаем
        assert len(lst) == len(['/disk/public_folder/test_1.jpg', '/disk/public_folder/test_3.jpg'])
        assert sorted([o['this']['id'] for o in lst]) == [
            '/disk/public_folder/test_1.jpg',
            '/disk/public_folder/test_3.jpg'
        ]

    def test_public_bulk_download_prepare_for_2_users(self):
        uid = self.uid
        uid1 = self.user_2.uid
        uid2 = self.user_3.uid
        self.create_user(uid1)
        self.create_user(uid2)

        self.json_ok('mkdir', {
            'uid': uid1,
            'path': '/disk/public_folder_usr_1',
        })
        self.json_ok('mkdir', {
            'uid': uid1,
            'path': '/disk/public_folder_usr_1/test_folder'
        })
        self.upload_file(uid1, '/disk/public_folder_usr_1/test_1.jpg')
        self.upload_file(uid1, '/disk/public_folder_usr_1/test_2.jpg')
        self.upload_file(uid1, '/disk/public_folder_usr_1/test_3.jpg')

        result = self.json_ok('set_public', {
            'uid': uid1,
            'path': '/disk/public_folder_usr_1',
        })
        public_hash_usr_1 = result['hash']

        self.json_ok('mkdir', {
            'uid': uid2,
            'path': '/disk/public_folder_usr_2',
        })
        self.json_ok('mkdir', {
            'uid': uid2,
            'path': '/disk/public_folder_usr_2/test_folder'
        })
        self.upload_file(uid2, '/disk/public_folder_usr_2/test_1.jpg')
        self.upload_file(uid2, '/disk/public_folder_usr_2/test_2.jpg')
        self.upload_file(uid2, '/disk/public_folder_usr_2/test_3.jpg')

        result = self.json_ok('set_public', {
            'uid': uid2,
            'path': '/disk/public_folder_usr_2',
        })
        public_hash_usr_2 = result['hash']

        result = self.json_ok('public_bulk_download_prepare', {
            'uid': uid
        }, json={
            'items': [
                public_hash_usr_1 + ':/test_1.jpg',
                public_hash_usr_1 + ':/test_3.jpg',
                public_hash_usr_2 + ':/test_2.jpg',
                public_hash_usr_2 + ':/test_3.jpg',
            ]
        })
        oid = result['oid']
        result = self.json_ok('bulk_download_list', {
            'uid': uid,
            'oid': oid
        })
        lst = result['list']
        ids = [o['this']['id'] for o in lst]
        assert len(ids) == 4
        assert '/disk/public_folder_usr_1/test_1.jpg' in ids
        assert '/disk/public_folder_usr_1/test_3.jpg' in ids
        assert '/disk/public_folder_usr_2/test_2.jpg' in ids
        assert '/disk/public_folder_usr_2/test_3.jpg' in ids
