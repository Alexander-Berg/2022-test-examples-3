# -*- coding: utf-8 -*-
import os
import mock

from nose_parameterized import parameterized

from test.base import DiskTestCase
from test.common.sharing import CommonSharingMethods
from test.helpers.resource_comparators import assert_resource_equals

from mpfs.core.factory import (
    get_resources, get_resources_by_resource_ids, get_resource_from_doc,
    get_resource, iter_resources_by_stids
)
from mpfs.core.address import Address, ResourceId
from mpfs.core.social.share.group import Group
from mpfs.core.filesystem.resources.root import RootFolder
from mpfs.core.filesystem.resources.disk import DiskFile, DiskFolder
from mpfs.core.filesystem.resources.trash import TrashFile, TrashFolder
from mpfs.core.filesystem.resources.attach import AttachFile, AttachFolder
from mpfs.core.filesystem.resources.hidden import HiddenFile, HiddenFolder
from mpfs.core.filesystem.resources.share import SharedResource
from mpfs.core.filesystem.resources.group import GroupResource

from mpfs.metastorage.mongo.collections.filesystem import UserDataCollection
from mpfs.metastorage.mongo.collections.all_user_data import AllUserDataCollection


class GetResourcesTestCase(DiskTestCase):
    def setup_method(self, method):
        super(GetResourcesTestCase, self).setup_method(method)
        self.upload_file(self.uid, '/disk/1.jpg')
        self.upload_file(self.uid, '/disk/2.jpg')
        with mock.patch('mpfs.core.address.Address.add_trash_suffix'):
            self.json_ok('trash_append', {'uid': self.uid, 'path': '/disk/2.jpg'})
        self.upload_file(self.uid, '/attach/3.jpg')
        self.upload_file(self.uid, '/disk/4.jpg')
        attach_path = self.json_ok('list', {'uid': self.uid, 'path': '/attach'})[1]['path']

        self.addresses = [Address('/disk/1.jpg', uid=self.uid),
                          Address('/trash/2.jpg', uid=self.uid),
                          Address('/disk/4.jpg', uid=self.uid),
                          Address('/trash/2.jpg', uid=self.uid),
                          Address(attach_path, uid=self.uid)]

    def test_different_storages(self):
        """Тестируем получение ресурсов из разных сервисов через get_resources"""
        resources = get_resources(self.uid, self.addresses)
        assert [a.id for a in self.addresses] == [r.address.id for r in resources]

    def test_selected_storages(self):
        resources = get_resources(self.uid, self.addresses, available_service_ids=['/disk'])
        assert len(resources) == 2
        assert [a.id for a in self.addresses if '/disk' in a.id] == [r.address.id for r in resources]


class GetResourcesByResourceIdTestCase(CommonSharingMethods, DiskTestCase):

    @parameterized.expand([(True,), (False,)])
    def test_common(self, optimized):
        self.upload_file(self.uid, '/disk/1.jpg')
        self.upload_file(self.uid, '/disk/2.jpg')
        with mock.patch('mpfs.core.address.Address.add_trash_suffix'):
            self.json_ok('trash_append', {'uid': self.uid, 'path': '/disk/2.jpg'})
        self.upload_file(self.uid, '/attach/3.jpg')

        file_id_1 = self.json_ok('info', {'uid': self.uid, 'path': '/disk/1.jpg', 'meta': 'file_id'})['meta']['file_id']
        file_id_2 = self.json_ok('info', {'uid': self.uid, 'path': '/trash/2.jpg', 'meta': 'file_id'})['meta']['file_id']
        file_id_3 = [i['meta']['file_id'] for i in self.json_ok('list', {'uid': self.uid, 'path': '/attach/', 'meta': 'file_id'}) if i['type'] == 'file'][0]

        resource_ids = []
        for f in (file_id_1,
                  'fake000000000000000000000000000000000000000000000000000000000000',
                  'fake111111111111111111111111111111111111111111111111111111111111',
                  file_id_2,
                  file_id_3):
            resource_ids.append(ResourceId(**{'uid': self.uid, 'file_id': f}))

        # по всем 3-м сервисам
        resources = get_resources_by_resource_ids(
            self.uid, resource_ids, enable_optimization=optimized)
        assert len(resources) == 5
        assert isinstance(resources[0], DiskFile)
        assert resources[1] is None
        assert resources[2] is None
        assert isinstance(resources[3], TrashFile)
        assert isinstance(resources[4], AttachFile)
        # только /trash
        resources = get_resources_by_resource_ids(
            self.uid, resource_ids, enable_service_ids=['/trash'], enable_optimization=optimized)
        assert len(resources) == 5
        assert resources[0] is None
        assert resources[1] is None
        assert resources[2] is None
        assert isinstance(resources[3], TrashFile)
        assert resources[4] is None
        # только /disk и /attach
        resources = get_resources_by_resource_ids(
            self.uid, resource_ids, enable_service_ids=['/disk', '/attach'], enable_optimization=optimized)
        assert len(resources) == 5
        assert isinstance(resources[0], DiskFile)
        assert resources[1] is None
        assert resources[2] is None
        assert resources[3] is None
        assert isinstance(resources[4], AttachFile)
        #
        self.json_ok('user_init', {'uid': self.uid_3})
        resources = get_resources_by_resource_ids(self.uid_3, resource_ids, enable_optimization=optimized)
        assert len(resources) == 5
        assert [i for i in resources if i is not None] == []

    @parameterized.expand([(True,), (False,)])
    def test_shared_folders(self, optimized):
        self.json_ok('user_init', {'uid': self.uid_3})
        self.make_dirs()

        gid = self.create_group()
        hsh = self.invite_user(uid=self.uid_3, email=self.email_3, ext_gid=gid)
        self.activate_invite(uid=self.uid_3, hash=hsh)
        self.upload_file(self.uid, '/disk/new_folder/folder2/folder3/1.jpg')

        file_id = self.json_ok('info', {'uid': self.uid, 'path': '/disk/new_folder/folder2/folder3/1.jpg', 'meta': 'file_id'})['meta']['file_id']
        resources = get_resources_by_resource_ids(
            self.uid_3, [ResourceId(**{'uid': self.uid, 'file_id': file_id})],
            enable_optimization=optimized)
        assert resources[0].visible_address.uid == self.uid_3
        resources = get_resources_by_resource_ids(
            self.uid, [ResourceId(**{'uid': self.uid, 'file_id': file_id})],
            enable_optimization=optimized)
        assert resources[0].visible_address.uid == self.uid


class GetAllUserResourcesFromDocTestCase(CommonSharingMethods):
    SHARED_ROOT_FOLDER_PATH = '/disk/Shared'
    SHARED_FOLDER_PATH = os.path.join(SHARED_ROOT_FOLDER_PATH, 'Folder')
    SHARED_FILE_PATH = os.path.join(SHARED_ROOT_FOLDER_PATH, 'test.txt')

    def test_regular_resources(self):
        db_results = AllUserDataCollection(('user_data',)).find_on_uid_shard(self.uid, {'uid': self.uid})
        for db_result in db_results:
            doc = db_result.record
            resource = get_resource_from_doc(self.uid, doc)
            assert_resource_equals(
                resource, get_resource(self.uid, doc['key']))

    def test_regular_and_shared(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': self.SHARED_ROOT_FOLDER_PATH})
        self.create_share_for_guest(self.uid, self.SHARED_ROOT_FOLDER_PATH, self.uid_3, self.email_3)
        self.create_subtree(self.uid_3, '/disk')
        self.create_subtree(self.uid_3, self.SHARED_ROOT_FOLDER_PATH)

        db_results = AllUserDataCollection(('user_data',)).find_on_uid_shard(self.uid_3, {'uid': self.uid_3})
        for db_result in db_results:
            doc = db_result.record
            resource = get_resource_from_doc(self.uid_3, doc)
            assert_resource_equals(
                resource, get_resource(self.uid_3, doc['key']))

        links = Group.load_all(self.uid_3)['uid_path']
        total_shared_resources = 0
        for (uid, path), link in links.iteritems():
            docs = UserDataCollection().iter_subtree(uid, path)
            for doc in docs:
                resource = get_resource_from_doc(self.uid_3, doc)
                assert_resource_equals(
                    resource, get_resource(self.uid_3, doc['key']))

                if isinstance(resource, SharedResource):
                    total_shared_resources += 1
        assert total_shared_resources

    def test_regular_and_group(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': self.SHARED_ROOT_FOLDER_PATH})
        self.create_share_for_guest(self.uid, self.SHARED_ROOT_FOLDER_PATH, self.uid_3, self.email_3)

        db_results = AllUserDataCollection(('user_data',)).find_on_uid_shard(self.uid, {'uid': self.uid})
        total_group_resources = 0
        for db_result in db_results:
            doc = db_result.record
            resource = get_resource_from_doc(self.uid, doc)
            assert_resource_equals(
                resource, get_resource(self.uid, doc['key']))

            if isinstance(resource, GroupResource):
                total_group_resources += 1

        assert total_group_resources

    def test_all_storage_resources(self):
        self.upload_file(self.uid, '/attach/test.txt')
        self.upload_file(self.uid, '/disk/test1.txt')
        self.upload_file(self.uid, '/disk/test2.txt')
        self.upload_file(self.uid, '/disk/test3.txt')
        self.json_ok('trash_append', {'uid': self.uid, 'path': '/disk/test1.txt'})
        self.json_ok('trash_drop_all', {'uid': self.uid})
        self.json_ok('trash_append', {'uid': self.uid, 'path': '/disk/test2.txt'})

        db_results = AllUserDataCollection().find_on_uid_shard(self.uid, {'uid': self.uid})
        resource_type_set = set()
        for db_result in db_results:
            doc = db_result.record
            resource = get_resource_from_doc(self.uid, doc)
            assert_resource_equals(
                resource, get_resource(self.uid, doc['key']))
            resource_type_set.add(type(resource))

        assert {AttachFile, AttachFolder, HiddenFile,
                HiddenFolder, TrashFile, TrashFolder,
                DiskFile, DiskFolder, RootFolder} == resource_type_set


class GetResourcesByStidsTestCase(DiskTestCase):
    def test_one_resource_with_uniq_stids(self):
        file_path = '/disk/1.jpg'
        self.upload_file(self.uid, file_path)
        resp = self.json_ok('info', {'uid': self.uid, 'path': file_path, 'meta': ''})
        for stid_type in ('file_mid', 'pmid', 'digest_mid'):
            stid = resp['meta'][stid_type]
            result = iter_resources_by_stids([stid])
            result = list(result)
            assert len(result) == 1
            address = result[0].address
            assert address.uid == self.uid
            assert address.path == file_path

    def test_misc_storage_resources(self):
        file_path = '/disk/1.jpg'
        self.upload_file(self.uid, file_path)
        self.json_ok('copy', {'uid': self.uid, 'src': file_path, 'dst': '/disk/2.jpg'})
        self.json_ok('copy', {'uid': self.uid, 'src': file_path, 'dst': '/disk/3.jpg'})
        self.json_ok('trash_append', {'uid': self.uid, 'path': '/disk/3.jpg'})
        self.json_ok('copy', {'uid': self.uid, 'src': file_path, 'dst': '/disk/4.jpg'})
        self.json_ok('rm', {'uid': self.uid, 'path': '/disk/4.jpg'})

        resp = self.json_ok('info', {'uid': self.uid, 'path': file_path, 'meta': ''})

        all_stids = []
        for stid_type in ('file_mid', 'pmid', 'digest_mid'):
            stid = resp['meta'][stid_type]
            all_stids.append(stid)
            result = iter_resources_by_stids([stid])
            assert len(list(result)) == 4
        result = iter_resources_by_stids(all_stids)
        assert len(list(result)) == 4
