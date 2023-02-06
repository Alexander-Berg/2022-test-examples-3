# -*- coding: utf-8 -*-
import random
import string
import uuid
import pytest
import mock

from nose_parameterized import parameterized

from test.base import DiskTestCase

from sqlalchemy.sql.expression import select, and_

from mpfs.core import factory
from mpfs.core.address import ResourceId
from mpfs.core.filesystem.dao.resource import ResourceDAO
from mpfs.core.filesystem.dao.file import FileDAOItem
from mpfs.metastorage.postgres.exceptions import DatabaseConstraintError, UniqueConstraintViolationError
from mpfs.core.filesystem.resources.base import Resource
from mpfs.dao.session import Session
from mpfs.metastorage.postgres.schema import folders
from mpfs.metastorage.postgres import query_executer
from mpfs.metastorage.postgres.queries import SQL_FILE_BY_PATH
from test.conftest import INIT_USER_IN_POSTGRES


pytestmark = pytest.mark.skipif(not INIT_USER_IN_POSTGRES, reason='Tests are designed to work for postgres')


class ResourceDAOTestCase(DiskTestCase):
    def test_find_by_resource_ids_with_abracadabra(self):
        res_ids = []
        for i in xrange(10):
            file_path = '/disk/test-file-%d.txt' % i
            self.upload_file(self.uid, file_path)
            test_file_meta = self.json_ok('info', {'uid': self.uid, 'path': file_path, 'meta': 'resource_id'})['meta']
            res_ids.append(test_file_meta['resource_id'])

        resource_ids = [ResourceId.parse(i) for i in res_ids]
        resources = factory.get_resources_by_resource_ids(self.uid, resource_ids)
        assert len([r for r in resources if r is not None]) == 10

        not_hexadecimal_64_length_string = ''.join(random.choice(string.hexdigits) for _ in xrange(32)) + \
                                           ''.join(random.choice('GHIJKLMNOPQRSTUVWXYZ') for _ in xrange(32))
        resource_ids[2].file_id = not_hexadecimal_64_length_string
        resource_ids[4].file_id = 'Some strange string!'
        resource_ids[7].file_id = 'abf12031'

        resources = factory.get_resources_by_resource_ids(self.uid, resource_ids)
        assert len([r for r in resources if r is not None]) == 7

    def test_exclude_keys_after_conversion_to_mongo(self):
        file_path = '/disk/test-file.txt'
        self.upload_file(self.uid, file_path)

        mongo_dict = ResourceDAO().find_one({'uid': self.uid, 'path': file_path})

        def assert_left_has_no_values_from_right(left, right):
            for key, value in right.iteritems():
                if isinstance(value, dict):
                    if key in left:
                        assert_left_has_no_values_from_right(value, left[key])
                else:
                    if key in left:
                        self.assertTrue(left[key] != value,
                                        'Left contains default value "%s" for key "%s"' % (value, key))

        assert_left_has_no_values_from_right(mongo_dict, FileDAOItem.exclude_keys_after_conversion_to_mongo)

    def test_constraint_on_non_empty_folder_with_subfolders_remove(self):
        parent_folder_path = '/disk/parent'
        child_1_folder_path = parent_folder_path + '/child_1'
        child_2_folder_path = parent_folder_path + '/child_2'

        self.json_ok('mkdir', {'uid': self.uid, 'path': parent_folder_path})
        self.json_ok('mkdir', {'uid': self.uid, 'path': child_1_folder_path})
        self.json_ok('mkdir', {'uid': self.uid, 'path': child_2_folder_path})

        try:
            ResourceDAO().remove({'uid': self.uid, 'path': parent_folder_path})
        except DatabaseConstraintError:
            pass
        else:
            assert False, 'Expected DatabaseConstraintError exception'

        try:
            session = Session.create_from_uid(self.uid)
            with session.begin():
                ResourceDAO().remove({'uid': self.uid, 'path': child_2_folder_path})
                ResourceDAO().remove({'uid': self.uid, 'path': parent_folder_path})
        except DatabaseConstraintError:
            pass
        else:
            assert False, 'Expected DatabaseConstraintError exception'

        try:
            session = Session.create_from_uid(self.uid)
            with session.begin():
                ResourceDAO().remove({'uid': self.uid, 'path': child_1_folder_path})
                ResourceDAO().remove({'uid': self.uid, 'path': child_2_folder_path})
                ResourceDAO().remove({'uid': self.uid, 'path': parent_folder_path})
        except DatabaseConstraintError:
            assert False, 'Unexpected DatabaseConstraintError exception'

    def test_constraint_on_non_empty_folder_with_files_remove(self):
        parent_folder_path = '/disk/parent'
        child_1_file_path = parent_folder_path + '/child_1.txt'
        child_2_file_path = parent_folder_path + '/child_2.txt'

        self.json_ok('mkdir', {'uid': self.uid, 'path': parent_folder_path})
        self.upload_file(self.uid, child_1_file_path)
        self.upload_file(self.uid, child_2_file_path)

        try:
            ResourceDAO().remove({'uid': self.uid, 'path': parent_folder_path})
        except DatabaseConstraintError:
            pass
        else:
            assert False, 'Expected DatabaseConstraintError exception'

        try:
            session = Session.create_from_uid(self.uid)
            with session.begin():
                ResourceDAO().remove({'uid': self.uid, 'path': child_2_file_path})
                ResourceDAO().remove({'uid': self.uid, 'path': parent_folder_path})
        except DatabaseConstraintError:
            pass
        else:
            assert False, 'Expected DatabaseConstraintError exception'

        try:
            session = Session.create_from_uid(self.uid)
            with session.begin():
                ResourceDAO().remove({'uid': self.uid, 'path': child_1_file_path})
                ResourceDAO().remove({'uid': self.uid, 'path': child_2_file_path})
                ResourceDAO().remove({'uid': self.uid, 'path': parent_folder_path})
        except DatabaseConstraintError:
            assert False, 'Unexpected DatabaseConstraintError exception'

    def test_cursor_offset(self):
        parent_folder_path = '/disk/parent'
        self.json_ok('mkdir', {'uid': self.uid, 'path': parent_folder_path})

        for i in xrange(20):
            child_i_file_path = parent_folder_path + '/child_%d.txt' % i
            self.upload_file(self.uid, child_i_file_path)

        cursor = ResourceDAO().find({'uid': self.uid, 'parent': parent_folder_path}, skip=11)
        first_item = cursor[0]
        assert first_item['key'] == '/disk/parent/child_11.txt'

    def test_cursor_offset_with_folders(self):
        parent_folder_path = '/disk/parent'
        self.json_ok('mkdir', {'uid': self.uid, 'path': parent_folder_path})

        for i in xrange(10):
            child_i_folder_path = parent_folder_path + '/folder_%d' % i
            self.json_ok('mkdir', {'uid': self.uid, 'path': child_i_folder_path})

        for i in xrange(20):
            child_i_file_path = parent_folder_path + '/child_%d.txt' % i
            self.upload_file(self.uid, child_i_file_path)

        cursor = ResourceDAO().find({'uid': self.uid, 'parent': parent_folder_path}, skip=13)
        first_item = cursor[0]
        assert first_item['key'] == '/disk/parent/child_3.txt'

    def test_cursor_offset_with_folders_2(self):
        parent_folder_path = '/disk/parent'
        self.json_ok('mkdir', {'uid': self.uid, 'path': parent_folder_path})

        for i in xrange(10):
            child_i_folder_path = parent_folder_path + '/folder_%d' % i
            self.json_ok('mkdir', {'uid': self.uid, 'path': child_i_folder_path})

        for i in xrange(20):
            child_i_file_path = parent_folder_path + '/child_%d.txt' % i
            self.upload_file(self.uid, child_i_file_path)

        cursor = ResourceDAO().find({'uid': self.uid, 'parent': parent_folder_path}, skip=10)
        first_item = cursor[0]
        assert first_item['key'] == '/disk/parent/child_0.txt'

    def _create_folders_and_files(self, parent_folder_path=None):
        if parent_folder_path is None:
            parent_folder_path = '/disk/parent'
        self.json_ok('mkdir', {'uid': self.uid, 'path': parent_folder_path})

        total_folders_list = []
        for i in xrange(10):
            child_i_folder_path = parent_folder_path + '/folder_%d' % i
            self.json_ok('mkdir', {'uid': self.uid, 'path': child_i_folder_path})
            total_folders_list.append(child_i_folder_path)
        total_folders_list.sort()

        total_files_list = []
        for i in xrange(20):
            child_i_file_path = parent_folder_path + '/child_%d.txt' % i
            self.upload_file(self.uid, child_i_file_path)
            total_files_list.append(child_i_file_path)
        total_files_list.sort()

        return total_folders_list + total_files_list

    @parameterized.expand([
        (0, 12),
        (9, 3),
        (11, 2)
    ])
    def test_cursor_limit_and_offset_with_files_and_folders(self, offset, limit):
        parent_folder_path = '/disk/parent'
        total_resouces_list = self._create_folders_and_files(parent_folder_path)

        kwargs = {'limit': limit}
        if offset:
            kwargs['skip'] = offset

        cursor = ResourceDAO().find({'uid': self.uid, 'parent': parent_folder_path}, sort=[('key', 1)], **kwargs)

        for i, item in enumerate(cursor):
            assert item['key'] == total_resouces_list[i + offset]

    @pytest.mark.timeout(60)
    def test_bulk_info_by_resource_ids_without_connection_pool_overflow(self):
        resource_ids = []
        files_count = 20
        for i in xrange(files_count):
            file_path = '/disk/file-%d.txt' % i
            self.upload_file(self.uid, file_path)
            info = self.json_ok('info', {'uid': self.uid, 'path': file_path, 'meta': 'resource_id'})
            resource_ids.append(info['meta']['resource_id'])

        query_executer.PGQueryExecuter.reset_cache()
        Session.clear_cache()
        with mock.patch.object(query_executer, 'POSTGRES_POOL_OVERFLOW_SIZE', 0), \
                mock.patch.object(query_executer, 'POSTGRES_POOL_SIZE', 1):
            # проверяем, что не зависнет получение коннектов из пула
            bulk_info = self.json_ok('bulk_info_by_resource_ids',
                                     {'uid': self.uid, 'meta': 'resource_id'},
                                     json=resource_ids)

        assert len(bulk_info) == files_count


class FileConstraintsDAOTestCase(DiskTestCase):
    def test_remove_files_without_storage_files(self):
        file_path = '/disk/test-file.txt'
        self.upload_file(self.uid, file_path)

        session = Session.create_from_uid(self.uid)
        files_entry = session.execute('SELECT * FROM disk.files WHERE fid=(SELECT fid FROM code.path_to_fid(:path, :uid))', {
            'uid': self.uid,
            'path': file_path,
        }).fetchone()
        assert files_entry is not None

        file_id = files_entry['id']
        hid = files_entry['storage_id']

        storage_files_entry = session.execute('SELECT * FROM disk.storage_files WHERE storage_id=:storage_id', {
            'storage_id': hid,
        }).fetchone()
        assert storage_files_entry is not None

        session.execute('DELETE FROM disk.files WHERE id=:file_id', {'file_id': file_id})

        files_entry = session.execute('SELECT * FROM disk.files WHERE id=:file_id', {'file_id': file_id}).fetchone()
        assert files_entry is None

        storage_files_entry = session.execute('SELECT * FROM disk.storage_files WHERE storage_id=:storage_id',
                                              {'storage_id': hid}).fetchone()
        assert storage_files_entry is not None

    def test_remove_files_with_storage_files(self):
        file_path = '/disk/test-file.txt'
        self.upload_file(self.uid, file_path)

        session = Session.create_from_uid(self.uid)
        files_entry = session.execute('SELECT * FROM disk.files WHERE fid=(SELECT fid FROM code.path_to_fid(:path, :uid))', {
            'uid': self.uid,
            'path': file_path,
        }).fetchone()
        assert files_entry is not None

        file_id = files_entry['id']
        hid = files_entry['storage_id']

        storage_files_entry = session.execute('SELECT * FROM disk.storage_files WHERE storage_id=:storage_id', {
            'storage_id': hid,
        }).fetchone()
        assert storage_files_entry is not None

        session.execute('DELETE FROM disk.files WHERE id=:file_id', {'file_id': file_id})
        session.execute('DELETE FROM disk.storage_files WHERE storage_id=:storage_id', {'storage_id': hid})

        files_entry = session.execute('SELECT * FROM disk.files WHERE id=:file_id', {'file_id': file_id}).fetchone()
        assert files_entry is None

        storage_files_entry = session.execute('SELECT * FROM disk.storage_files WHERE storage_id=:storage_id',
                                              {'storage_id': hid}).fetchone()
        assert storage_files_entry is None

    def test_assert_on_remove_files_with_storage_files_for_two_files(self):
        file_path = '/disk/test-file.txt'
        file_path_2 = '/disk/test-file-2.txt'
        self.upload_file(self.uid, file_path)

        info = self.json_ok('info', {'uid': self.uid, 'path': file_path, 'meta': 'size,sha256,md5'})
        self.upload_file(self.uid, file_path_2, file_data={
            'size': info['meta']['size'],
            'sha256': info['meta']['sha256'],
            'md5': info['meta']['md5']
        })

        session = Session.create_from_uid(self.uid)
        files_entry = session.execute('SELECT * FROM disk.files WHERE fid=(SELECT fid FROM code.path_to_fid(:path, :uid))', {
            'uid': self.uid,
            'path': file_path,
        }).fetchone()
        assert files_entry is not None

        file_id = files_entry['id']
        hid = files_entry['storage_id']

        storage_files_entry = session.execute('SELECT * FROM disk.storage_files WHERE storage_id=:storage_id', {
            'storage_id': hid,
        }).fetchone()
        assert storage_files_entry is not None

        session.execute('DELETE FROM disk.files WHERE id=:file_id', {'file_id': file_id})

        try:
            session.execute('DELETE FROM disk.storage_files WHERE storage_id=:storage_id', {'storage_id': hid})
        except DatabaseConstraintError as e:
            pass
        else:
            assert False, 'DataBaseConstraint exception is expected'

        files_entry = session.execute('SELECT * FROM disk.files WHERE id=:file_id', {'file_id': file_id}).fetchone()
        assert files_entry is None

        storage_files_entry = session.execute('SELECT * FROM disk.storage_files WHERE storage_id=:storage_id',
                                              {'storage_id': hid}).fetchone()
        assert storage_files_entry is not None

    def test_assert_on_remove_from_storage_files_when_files_foreign_key_exists(self):
        file_path = '/disk/test-file.txt'
        self.upload_file(self.uid, file_path)

        session = Session.create_from_uid(self.uid)
        files_entry = session.execute('SELECT * FROM disk.files WHERE fid=(SELECT fid FROM code.path_to_fid(:path, :uid))', {
            'uid': self.uid,
            'path': file_path,
        }).fetchone()
        assert files_entry is not None

        file_id = files_entry['id']
        hid = files_entry['storage_id']

        storage_files_entry = session.execute('SELECT * FROM disk.storage_files WHERE storage_id=:storage_id', {
            'storage_id': hid,
        }).fetchone()
        assert storage_files_entry is not None

        try:
            session.execute('DELETE FROM disk.storage_files WHERE storage_id=:storage_id', {'storage_id': hid})
        except DatabaseConstraintError as e:
            pass
        else:
            assert False, 'DataBaseConstraint exception is expected'

        files_entry = session.execute('SELECT * FROM disk.files WHERE id=:file_id', {'file_id': file_id}).fetchone()
        assert files_entry is not None

        storage_files_entry = session.execute('SELECT * FROM disk.storage_files WHERE storage_id=:storage_id',
                                              {'storage_id': hid}).fetchone()
        assert storage_files_entry is not None

    def test_unique_constraint_exception(self):
        uid = int(self.uid)

        query = select([folders]).where(and_(folders.c.uid == uid, folders.c.name == 'disk'))
        session = Session.create_from_uid(uid)
        folder_disk = session.execute(query).fetchone()

        fid = uuid.uuid4()
        parent_fid = folder_disk[folders.c.fid]
        file_id = '\\x' + Resource.generate_file_id(self.uid, '/disk/folder')
        name = 'folder'

        query = folders.insert().values(uid=uid, fid=fid, parent_fid=parent_fid, id=file_id, name=name)
        session.execute(query)
        try:
            session.execute(query)
        except UniqueConstraintViolationError:
            pass
        else:
            assert False
