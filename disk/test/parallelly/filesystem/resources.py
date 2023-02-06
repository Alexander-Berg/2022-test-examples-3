# -*- coding: utf-8 -*-

import mock
import os
import pytest
import unittest

from nose_parameterized import parameterized

from mpfs.core.filesystem.cleaner.controllers import deleted_stids_log
from test.fixtures.filesystem import DISK_FILE_RAW_DATA
from test.base import DiskTestCase
from test.common.sharing import CommonSharingMethods
from test.conftest import INIT_USER_IN_POSTGRES
from test.helpers.stubs.services import MulcaServiceStub, PreviewerStub
from test.helpers.utils import catch_return_values
from test.helpers.resource_comparators import assert_resource_equals

from mpfs.core.factory import SYS_FOLDER_MAP
from mpfs.common.errors import ResourceNotFound
from mpfs.config import settings
from mpfs.core import factory
from mpfs.core.bus import Bus
from mpfs.core.address import Address
from mpfs.core.filesystem.cleaner.models import DeletedStid
from mpfs.core.filesystem.dao.legacy import is_new_fs_spec_required
from mpfs.core.filesystem.dao.resource import MongoResourceDAOImplementation
from mpfs.core.filesystem.resources.attach import AttachFile, AttachFolder
from mpfs.core.filesystem.resources.disk import DiskFile, DiskFolder
from mpfs.core.filesystem.resources.group import GroupFolder, GroupRootFolder, GroupFile
from mpfs.core.filesystem.resources.hidden import HiddenFile, HiddenFolder
from mpfs.core.filesystem.resources.root import RootFolder
from mpfs.core.filesystem.resources.share import SharedFile
from mpfs.core.filesystem.resources.share import SharedRootFolder, SharedFolder
from mpfs.core.filesystem.resources.trash import TrashFile, TrashFolder
from mpfs.core.services.attach_service import Attach
from mpfs.core.services.disk_service import Disk
from mpfs.core.services.hidden_files_service import Hidden
from mpfs.core.services.trash_service import Trash
from mpfs.core.social.share.group import Group

from mpfs.metastorage.mongo.collections.all_user_data import AllUserDataCollection

FOLDERS = settings.folders


class RegeneratePreviewTestCase(DiskTestCase):
    def test_regenerate_preview(self):
        self.upload_file(self.uid, '/disk/1.jpg')
        resource = factory.get_resource(self.uid, '/disk/1.jpg')
        before_pmid = resource.meta['pmid']
        with mock.patch('mpfs.core.filesystem.cleaner.controllers.DeletedStidsController.bulk_create') as stids_stub:
            stids_stub.assert_not_called()

            with MulcaServiceStub():
                with PreviewerStub() as stub:
                    regerate_preview_result = stub.previewer_regenerate_preview.return_value
                    resource.regenerate_preview()
                    assert stids_stub.called_once()

            # предыдущая первьюшка улетела в deleted_stids
            deleted_stid = stids_stub.call_args[0][0][0]
            assert deleted_stid.stid == before_pmid
            assert deleted_stid.stid_type == 'pmid'

            same_resource = factory.get_resource(self.uid, '/disk/1.jpg')
            assert before_pmid != resource.meta['pmid']
            assert before_pmid != same_resource.meta['pmid']
            assert regerate_preview_result.pmid == resource.meta['pmid'] == same_resource.meta['pmid']


class RegenerateStidForAllResourcesTestCase(DiskTestCase):
    FILE_PATH = '/disk/файл.jpg'  # should work with cyrillic symbols

    def setup_method(self, method):
        super(RegenerateStidForAllResourcesTestCase, self).setup_method(method)
        self.upload_file(self.uid, self.FILE_PATH)
        self.json_ok('copy', {'uid': self.uid, 'src': self.FILE_PATH, 'dst': '/disk/2.jpg'})
        self.json_ok('trash_append', {'uid': self.uid, 'path': '/disk/2.jpg'})
        self.json_ok('copy', {'uid': self.uid, 'src': self.FILE_PATH, 'dst': '/disk/3.jpg'})
        self.json_ok('rm', {'uid': self.uid, 'path': '/disk/3.jpg'})

    def test_regenerate_preview_for_all_resources(self):
        old_pmid = self.json_ok('info', {'uid': self.uid, 'path': self.FILE_PATH, 'meta': ''})['meta']['pmid']
        assert len(list(factory.iter_resources_by_stids([old_pmid]))) == 3
        address = Address.Make(self.uid, self.FILE_PATH)
        fs = Bus()
        with MulcaServiceStub():
            with PreviewerStub() as stub:
                fs.regenerate_preview_for_all_resources(address)
                stub.previewer_regenerate_preview.assert_called_once()
        assert len(list(factory.iter_resources_by_stids([old_pmid]))) == 0
        new_pmid = self.json_ok('info', {'uid': self.uid, 'path': self.FILE_PATH, 'meta': ''})['meta']['pmid']
        assert len(list(factory.iter_resources_by_stids([new_pmid]))) == 3

    def test_regenerate_digest_for_all_resources(self):
        old_digest_mid = self.json_ok('info', {'uid': self.uid, 'path': self.FILE_PATH, 'meta': ''})['meta']['digest_mid']
        assert len(list(factory.iter_resources_by_stids([old_digest_mid]))) == 3
        address = Address.Make(self.uid, self.FILE_PATH)
        fs = Bus()
        with MulcaServiceStub():
            with PreviewerStub() as stub:
                fs.regenerate_digest_for_all_resources(address)
                stub.regenerate_digest.assert_called_once()
        assert len(list(factory.iter_resources_by_stids([old_digest_mid]))) == 0
        new_digest_mid = self.json_ok('info', {'uid': self.uid, 'path': self.FILE_PATH, 'meta': ''})['meta']['digest_mid']
        assert len(list(factory.iter_resources_by_stids([new_digest_mid]))) == 3


def find_document(uid, path):
    """Найти документ ресурса в базе.

    Если его нет и он системный, то достать данные из настроек.
    Именно так делается внутри коснтурктора ресурсов.
    """
    address = Address.Make(uid, path)

    db_result = AllUserDataCollection().find_one_on_uid_shard(uid, {'uid': uid, 'key': path})
    if db_result is None and address.is_system:
        return {
            'uid': uid,
            'key': path,
            'type': 'dir',
            'data': FOLDERS[path]
        }
    return db_result.record


class DiskFileFromDictTestCase(DiskTestCase):
    FILE_PATH = '/disk/test.txt'

    def setup_method(self, method):
        super(DiskFileFromDictTestCase, self).setup_method(method)
        self.upload_file(self.uid, self.FILE_PATH)

    def test_from_prepared_document_correct_initialization(self):
        disk_file = DiskFile.from_dict(DISK_FILE_RAW_DATA)

        assert disk_file.uid == DISK_FILE_RAW_DATA['uid']
        assert disk_file.version == DISK_FILE_RAW_DATA['version']
        assert disk_file.type == DISK_FILE_RAW_DATA['type']
        assert disk_file.path == DISK_FILE_RAW_DATA['key']
        assert disk_file.mimetype == DISK_FILE_RAW_DATA['data']['mimetype']
        assert disk_file.mtime == DISK_FILE_RAW_DATA['data']['mtime']
        assert disk_file.ctime == DISK_FILE_RAW_DATA['data']['ctime']
        assert disk_file.utime == DISK_FILE_RAW_DATA['data']['utime']
        assert disk_file.name == DISK_FILE_RAW_DATA['data']['name']
        assert disk_file.hid == DISK_FILE_RAW_DATA['data']['hid']
        assert disk_file.size == DISK_FILE_RAW_DATA['data']['size']
        assert disk_file.meta == DISK_FILE_RAW_DATA['data']['meta']

    def test_basic_success(self):
        doc = find_document(self.uid, self.FILE_PATH)
        data = Disk().get_data_for_uid(self.uid, doc)
        disk_file = DiskFile.from_dict(data)

        other_disk_file = factory.get_resource(self.uid, self.FILE_PATH)
        assert_resource_equals(disk_file, other_disk_file)

    def test_wrong_type_failed(self):
        doc = find_document(self.uid, '/disk')
        data = Disk().get_data_for_uid(self.uid, doc)
        with self.assertRaises(ResourceNotFound):
            DiskFile.from_dict(data)


class DiskFolderFromDictTestCase(DiskTestCase):
    FOLDER_PATH = '/disk/Folder'

    def setup_method(self, method):
        super(DiskFolderFromDictTestCase, self).setup_method(method)
        self.json_ok('mkdir', {'uid': self.uid, 'path': self.FOLDER_PATH})

    def test_regular_folder_success(self):
        doc = find_document(self.uid, self.FOLDER_PATH)
        data = Disk().get_data_for_uid(self.uid, doc)
        disk_folder = DiskFolder.from_dict(data)

        other_disk_folder = factory.get_resource(self.uid, self.FOLDER_PATH)
        assert_resource_equals(disk_folder, other_disk_folder)

    def test_system_folder_success(self):
        doc = find_document(self.uid, '/disk')
        data = Disk().get_data_for_uid(self.uid, doc)
        system_folder = DiskFolder.from_dict(data)

        other_system_folder = factory.get_resource(self.uid, '/disk')
        assert_resource_equals(system_folder, other_system_folder)

    def test_regular_folder_from_root_folder_failed(self):
        doc = find_document(self.uid, '/')
        data = Disk().get_data_for_uid(self.uid, doc)
        with self.assertRaises(ResourceNotFound):
            DiskFolder.from_dict(data)

    def test_wrong_type_failed(self):
        file_path = os.path.join('/disk', 'test.txt')
        self.upload_file(self.uid, file_path)

        doc = find_document(self.uid, file_path)
        data = Disk().get_data_for_uid(self.uid, doc)
        with self.assertRaises(ResourceNotFound):
            DiskFolder.from_dict(data)


class RootFolderFromDictTestCase(DiskTestCase):

    def test_root_folder_success(self):
        doc = find_document(self.uid, '/')
        data = Disk().get_data_for_uid(self.uid, doc)
        root_folder = RootFolder.from_dict(data)

        other_root_folder = factory.get_resource(self.uid, '/')
        assert_resource_equals(root_folder, other_root_folder)

    def test_root_folder_from_regular_folder_failed(self):
        doc = find_document(self.uid, '/disk')
        data = Disk().get_data_for_uid(self.uid, doc)

        with self.assertRaises(ResourceNotFound):
            RootFolder.from_dict(data)


class SharedFolderFromDictTestCase(CommonSharingMethods):
    SHARED_ROOT_FOLDER_PATH = '/disk/Shared'
    SHARED_FOLDER_PATH = os.path.join(SHARED_ROOT_FOLDER_PATH, 'Folder')

    def _create_share(self, owner, guest, email, path):
        self.create_user(guest)
        self.xiva_subscribe(guest)

        self.create_group(owner, path)
        hash_ = self.invite_user(uid=guest, owner=owner, email=email, path=path)
        self.activate_invite(uid=guest, hash=hash_)

    def setup_method(self, method):
        super(SharedFolderFromDictTestCase, self).setup_method(method)
        self.json_ok('mkdir', {'uid': self.uid, 'path': self.SHARED_ROOT_FOLDER_PATH})
        self._create_share(self.uid, self.uid_3, self.email_3, self.SHARED_ROOT_FOLDER_PATH)
        self.json_ok('mkdir', {'uid': self.uid_3, 'path': self.SHARED_FOLDER_PATH})

    def test_shared_root_folder_success(self):
        doc = find_document(self.uid_3, self.SHARED_ROOT_FOLDER_PATH)
        data = Disk().get_data_for_uid(self.uid_3, doc)

        link = Group.find(uid=self.uid_3, path=self.SHARED_ROOT_FOLDER_PATH, link=True)
        data['link'] = link
        shared_folder = SharedRootFolder.from_dict(data)

        other_shared_folder = factory.get_resource(self.uid_3, self.SHARED_ROOT_FOLDER_PATH)

        assert_resource_equals(shared_folder, other_shared_folder)

    def test_shared_folder_success(self):
        doc = find_document(self.uid, self.SHARED_FOLDER_PATH)
        data = Disk().get_data_for_uid(self.uid_3, doc)

        link = Group.find(uid=self.uid_3, path=self.SHARED_ROOT_FOLDER_PATH, link=True)
        data['link'] = link
        shared_folder = SharedFolder.from_dict(data)
        other_shared_folder = factory.get_resource(self.uid_3, self.SHARED_FOLDER_PATH)

        assert_resource_equals(shared_folder, other_shared_folder)

    def test_no_link_provided_failed(self):
        doc = find_document(self.uid, self.SHARED_FOLDER_PATH)
        data = Disk().get_data_for_uid(self.uid_3, doc)
        with self.assertRaises(ResourceNotFound):
            SharedFolder.from_dict(data)


class GroupFolderFromDictTestCase(CommonSharingMethods):
    SHARED_ROOT_FOLDER_PATH = '/disk/Shared'
    SHARED_FOLDER_PATH = os.path.join(SHARED_ROOT_FOLDER_PATH, 'Folder')

    def _create_share(self, owner, guest, email, path):
        self.create_user(guest)
        self.xiva_subscribe(guest)

        self.create_group(owner, path)
        hash_ = self.invite_user(uid=guest, owner=owner, email=email, path=path)
        self.activate_invite(uid=guest, hash=hash_)

    def setup_method(self, method):
        super(GroupFolderFromDictTestCase, self).setup_method(method)
        self.json_ok('mkdir', {'uid': self.uid, 'path': self.SHARED_ROOT_FOLDER_PATH})
        self._create_share(self.uid, self.uid_3, self.email_3, self.SHARED_ROOT_FOLDER_PATH)
        self.json_ok('mkdir', {'uid': self.uid_3, 'path': self.SHARED_FOLDER_PATH})

    def test_group_root_folder_success(self):
        doc = find_document(self.uid, self.SHARED_ROOT_FOLDER_PATH)
        data = Disk().get_data_for_uid(self.uid, doc)

        group = Group.find(uid=self.uid, path=self.SHARED_ROOT_FOLDER_PATH, link=False, group=True)
        data['group'] = group
        group_folder = GroupRootFolder.from_dict(data)

        other_group_folder = factory.get_resource(self.uid, self.SHARED_ROOT_FOLDER_PATH)

        assert_resource_equals(group_folder, other_group_folder)

    def test_group_folder_success(self):
        doc = find_document(self.uid, self.SHARED_FOLDER_PATH)
        data = Disk().get_data_for_uid(self.uid, doc)

        group = Group.find(uid=self.uid, path=self.SHARED_ROOT_FOLDER_PATH, link=False, group=True)
        data['group'] = group
        group_folder = GroupFolder.from_dict(data)
        other_group_folder = factory.get_resource(self.uid, self.SHARED_FOLDER_PATH)

        assert_resource_equals(group_folder, other_group_folder)

    def test_no_group_provided_failed(self):
        doc = find_document(self.uid, self.SHARED_FOLDER_PATH)
        data = Disk().get_data_for_uid(self.uid, doc)
        with self.assertRaises(ResourceNotFound):
            GroupFolder.from_dict(data)


class SharedFileFromDictTestCase(CommonSharingMethods):
    SHARED_ROOT_FOLDER_PATH = '/disk/Shared'
    SHARED_FILE_PATH = os.path.join(SHARED_ROOT_FOLDER_PATH, 'test.txt')

    def setup_method(self, method):
        super(SharedFileFromDictTestCase, self).setup_method(method)
        self.json_ok('mkdir', {'uid': self.uid, 'path': self.SHARED_ROOT_FOLDER_PATH})
        self._create_share(self.uid, self.uid_3, self.email_3, self.SHARED_ROOT_FOLDER_PATH)
        self.upload_file(self.uid, self.SHARED_FILE_PATH)

    def _create_share(self, owner, guest, email, path):
        self.create_user(guest)
        self.xiva_subscribe(guest)

        self.create_group(owner, path)
        hash_ = self.invite_user(uid=guest, owner=owner, email=email, path=path)
        self.activate_invite(uid=guest, hash=hash_)

    def test_shared_file_success(self):
        doc = find_document(self.uid, self.SHARED_FILE_PATH)
        data = Disk().get_data_for_uid(self.uid_3, doc)

        link = Group.find(uid=self.uid_3, path=self.SHARED_ROOT_FOLDER_PATH, link=True)
        data['link'] = link
        shared_file = SharedFile.from_dict(data)

        other_shared_file = factory.get_resource(self.uid_3, self.SHARED_FILE_PATH)

        assert_resource_equals(shared_file, other_shared_file)

    def test_group_file_success(self):
        doc = find_document(self.uid, self.SHARED_FILE_PATH)
        data = Disk().get_data_for_uid(self.uid, doc)

        group = Group.find(uid=self.uid, path=self.SHARED_ROOT_FOLDER_PATH, group=True)
        data['group'] = group
        group_file = GroupFile.from_dict(data)

        other_group_file = factory.get_resource(self.uid, self.SHARED_FILE_PATH)
        assert_resource_equals(group_file, other_group_file)


class TrashResourcesFromDictTestCase(DiskTestCase):

    def test_trash_file_success(self):
        self.upload_file(self.uid, '/disk/test.txt')
        with mock.patch('mpfs.core.address.Address.add_trash_suffix'):
            self.json_ok('async_trash_append', {'uid': self.uid, 'path': '/disk/test.txt'})

        doc = find_document(self.uid, '/trash/test.txt')
        data = Trash().get_data_for_uid(self.uid, doc)
        trash_file = TrashFile.from_dict(data)

        other_trash_file = factory.get_resource(self.uid, '/trash/test.txt')
        assert_resource_equals(trash_file, other_trash_file)

    def test_trash_folder_success(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/Folder'})
        with mock.patch('mpfs.core.address.Address.add_trash_suffix'):
            self.json_ok('async_trash_append', {'uid': self.uid, 'path': '/disk/Folder'})

        doc = find_document(self.uid, '/trash/Folder')
        data = Trash().get_data_for_uid(self.uid, doc)
        trash_folder = TrashFolder.from_dict(data)

        other_trash_folder = factory.get_resource(self.uid, '/trash/Folder')
        assert_resource_equals(trash_folder, other_trash_folder)

    def test_system_folder(self):
        doc = find_document(self.uid, '/trash')
        data = Trash().get_data_for_uid(self.uid, doc)
        trash_system_folder = TrashFolder.from_dict(data)
        other_trash_system_folder = factory.get_resource(self.uid, '/trash')
        assert_resource_equals(trash_system_folder, other_trash_system_folder)

    def test_bad_storage_failed(self):
        self.upload_file(self.uid, '/disk/test.txt')
        doc = find_document(self.uid, '/disk/test.txt')
        data = Trash().get_data_for_uid(self.uid, doc)
        with self.assertRaises(ResourceNotFound):
            TrashFile.from_dict(data)


class HiddenResourcesFromDictTestCase(DiskTestCase):

    def test_hidden_file_success(self):
        self.upload_file(self.uid, '/disk/test.txt')
        self.json_ok('async_trash_append', {'uid': self.uid, 'path': '/disk/test.txt'})
        self.json_ok('async_trash_drop_all', {'uid': self.uid})
        resp = self.support_ok('list', {'uid': self.uid, 'path': '/hidden'})
        path = next(item['path'] for item in resp if item['path'].startswith('/hidden/test.txt'))
        doc = find_document(self.uid, path)
        data = Hidden().get_data_for_uid(self.uid, doc)
        hidden_file = HiddenFile.from_dict(data)

        other_hidden_file = factory.get_resource(self.uid, path)
        assert_resource_equals(hidden_file, other_hidden_file)

    def test_hidden_system_folder_success(self):
        doc = find_document(self.uid, '/hidden')
        data = Hidden().get_data_for_uid(self.uid, doc)
        trash_system_folder = HiddenFolder.from_dict(data)
        other_trash_system_folder = factory.get_resource(self.uid, '/hidden')
        assert_resource_equals(trash_system_folder, other_trash_system_folder)


class AttachResourcesFromDictTestCase(DiskTestCase):

    def test_attach_file_success(self):
        test_file_path = '/attach/test.txt'
        self.upload_file(self.uid, test_file_path)

        response = self.json_ok('list', {'uid': self.uid, 'path': '/attach'})
        path = (item['id'] for item in response if item['id'].startswith(test_file_path)).next()

        doc = find_document(self.uid, path)
        data = Attach().get_data_for_uid(self.uid, doc)
        attach_file = AttachFile.from_dict(data)

        other_attach_file = factory.get_resource(self.uid, path)
        assert_resource_equals(attach_file, other_attach_file)

    def test_system_folder(self):
        doc = find_document(self.uid, '/attach')
        data = Attach().get_data_for_uid(self.uid, doc)
        attach_folder = AttachFolder.from_dict(data)
        other_attach_folder = factory.get_resource(self.uid, '/attach')
        assert_resource_equals(attach_folder, other_attach_folder, ('prev_version', 'utime'))

    def test_bad_storage_failed(self):
        self.upload_file(self.uid, '/disk/test.txt')
        doc = find_document(self.uid, '/disk/test.txt')
        data = Attach().get_data_for_uid(self.uid, doc)
        with self.assertRaises(ResourceNotFound):
            AttachFile.from_dict(data)


def create_system_folder_fixtures():
    """
    /narod исключил, т.к. возвращается не иснтанс Resource, а callable.
    /share ругается на неинициализированность пользователя.
    :return: list
    """
    return [item for item in factory.SYS_FOLDER_MAP.items()
            if item[0] not in ('/share', '/narod')]


class AllSystemFoldersFromDictTestCase(DiskTestCase):
    """Класс тестов инициализации всех системных папок из словаря.

    В основном нужен для тестов тех системных папок, для которых у нас нет
    никаких тестов, например: `/lnarod`, `/photoslice`, `/share`, и т.д.
    """

    @parameterized.expand(create_system_folder_fixtures())
    def test_system_folder_success(self, system_folder_path, system_info):
        resource_class = system_info['classes']['folder']
        storage_class = system_info['service']

        doc = find_document(self.uid, system_folder_path)
        data = storage_class().get_data_for_uid(self.uid, doc)

        resource = resource_class.from_dict(data)
        other_resource = factory.get_resource(self.uid, system_folder_path)

        # Не все системные папки хранятся в базе.
        # ``factory.get_resource`` для них не передает данные для инициализации
        #  в конструктор, а данные берутся из настроек МПФС внутри конструктора.
        # Мы же расчитываем что эти данные из настроек передаются в конструктор.
        # В итоге одними и теми же данными одни и те же ресурсы инициализирутся двумя разными путями.
        # Выливается это все в отличие этими атрибутами.
        skip_attributes = ('name_tree', 'prev_version', 'version', 'utime')
        assert_resource_equals(resource, other_resource, skip_attributes)


class SelectResourceClassTestCase(unittest.TestCase):
    class FakeGroup(object):
        def __init__(self, path):
            self.path = path

    class FakeLink(object):
        def __init__(self, path):
            self.group = SelectResourceClassTestCase.FakeGroup(path)
            self.path = path

    @staticmethod
    def test_select_by_storage_address():
        uid = '100000000001'
        for storage_path, storage_info in factory.SYS_FOLDER_MAP.iteritems():
            for resource_type, reference_resource_class in storage_info['classes'].iteritems():
                resource_class = \
                    factory.select_resource_class(
                        {'uid': uid, 'key': storage_path, 'type': resource_type})
                assert reference_resource_class is resource_class

    @parameterized.expand([
        (SharedFile, 'file'),
        (SharedFolder, 'folder')])
    def test_select_shared_disk_resource(self, reference_resource_class, resource_type):
        uid = '100000000001'
        path = '/disk'
        link_path = path + '123'
        resource_class = \
            factory.select_resource_class(
                {'uid': uid, 'key': path, 'type': resource_type,
                 'link': SelectResourceClassTestCase.FakeLink(link_path)})
        assert reference_resource_class is resource_class

    @parameterized.expand([
        (GroupFile, 'file'),
        (GroupFolder, 'folder')])
    def test_select_group_disk_resource(self, reference_resource_class, resource_type):
        uid = '100000000001'
        path = '/disk'
        group_path = path + '123'
        resource_class = \
            factory.select_resource_class(
                {'uid': uid, 'key': path, 'type': resource_type,
                 'group': SelectResourceClassTestCase.FakeGroup(group_path)})
        assert reference_resource_class is resource_class

    def test_bad_storage_address_failed(self):
        uid = '100000000001'
        with self.assertRaises(ResourceNotFound):
            factory.select_resource_class(
                {'uid': uid, 'key': '/ABCDEF', 'type': 'file'})

    def test_bad_resource_type_failed(self):
        uid = '100000000001'
        with self.assertRaises(ResourceNotFound):
            factory.select_resource_class(
                {'uid': uid, 'key': '/', 'type': 'file'})

    @staticmethod
    def test_select_shared_root_folder():
        uid = '100000000001'
        path = '/disk'
        resource_class = \
            factory.select_resource_class(
                {'uid': uid, 'key': path, 'type': 'folder',
                 'link': SelectResourceClassTestCase.FakeLink(path)})
        assert resource_class is SharedRootFolder

    @staticmethod
    def test_select_group_root_folder():
        uid = '100000000001'
        path = '/disk'
        resource_class = \
            factory.select_resource_class(
                {'uid': uid, 'key': path, 'type': 'folder',
                 'group': SelectResourceClassTestCase.FakeGroup(path)})
        assert resource_class is GroupRootFolder
