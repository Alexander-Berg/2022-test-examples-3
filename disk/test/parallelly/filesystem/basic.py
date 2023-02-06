# -*- coding: utf-8 -*-

import posixpath

import mock
import pytest

from base import CommonFilesystemTestCase

import mpfs.engine.process

from mpfs.common.errors import ResourceNotFound
from mpfs.core import factory
from mpfs.core.address import Address
from mpfs.core.bus import Bus
from mpfs.core.filesystem.dao.legacy import CollectionRoutedDatabase
from test.conftest import INIT_USER_IN_POSTGRES

db = CollectionRoutedDatabase()
usrctl = mpfs.engine.process.usrctl()


class BasicFilesystemTestCase(CommonFilesystemTestCase):
    def setup_method(self, method):
        """
        Создаем пачку каталогов
        """
        super(BasicFilesystemTestCase, self).setup_method(method)
        self._mkdirs()

    def test_mkdir(self):
        def _assert_make_folder(fid):
            faddr = Address.Make(self.uid, fid).id
            result = Bus().mkdir(self.uid, faddr)
            self.assertEqual(fid, result['id'])
            for field in self.folder_fields:
                self.assertTrue(field in result)
                if field in self.nonempty_fields:
                    self.assertNotEqual(result[field], None)

        for fid in ('/disk/filesystem testissimo folder',
                    '/disk/filesystem testissimo folder' + '/inner folder',
                    '/disk/filesystem testissimo folder' + '/inner folder/subinner folder'):
            _assert_make_folder(fid)

    def test_mkfile(self):
        def _assert_make_file(fid):
            faddr = Address.Make(self.uid, fid).id
            resource = Bus().mkfile(self.uid, faddr, data=self.file_data)
            result = resource.dict()
            Bus().resource(self.uid, faddr)
            self.assertEqual(fid, result['id'])
            for field in self.file_fields:
                self.assertTrue(field in result)
                if field in self.nonempty_fields:
                    self.assertNotEqual(result[field], None, 'k:%s v: %s' % (field, result))
                if field in self.int_fields:
                    self.assertTrue(isinstance(result[field], int))
            for k, v in self.file_data.iteritems():
                if k == 'meta':
                    for _k, _v in v.iteritems():
                        self.assertEqual(_v, result[k][_k])
                else:
                    self.assertEqual(v, result[k])

        for fid in ('/disk/filesystem test file', '/disk/filesystem test folder/inner file'):
            _assert_make_file(fid)

    def test_info_file(self):
        faddr = Address.Make(self.uid, '/disk/filesystem test file').id
        Bus().mkfile(self.uid, faddr, data=self.file_data)

        result = Bus().info(self.uid, faddr)
        self.assertTrue('this' in result, result)
        self.assertTrue('list' in result, result)
        self.assertEqual('/disk/filesystem test file', result['this']['id'])
        for field in self.file_fields:
            self.assertTrue(field in result['this'])
            if field in self.nonempty_fields:
                self.assertNotEqual(result['this'][field], None)
        for k, v in self.file_data.iteritems():
            if k == 'meta':
                for _k, _v in v.iteritems():
                    self.assertEqual(_v, result['this'][k][_k])
            else:
                if isinstance(v, int):
                    self.assertTrue(v - result['this'][k] <= 0)
                else:
                    self.assertEqual(v, result['this'][k])

    def test_info_folder(self):
        faddr = Address.Make(self.uid, '/disk/filesystem test folder').id
        result = Bus().info(self.uid, faddr)
        self.assertTrue('this' in result, result)
        self.assertTrue('list' in result, result)
        self.assertEqual('/disk/filesystem test folder', result['this']['id'])
        for field in self.folder_fields:
            self.assertTrue(field in result['this'])
            if field in self.nonempty_fields:
                for field in self.file_fields:
                    self.assertTrue(field in result['this'])
            if field in self.nonempty_fields:
                self.assertNotEqual(result['this'][field], None)

    def test_list_folder(self):
        faddr = Address.Make(self.uid, '/disk/filesystem test folder/inner file').id
        Bus().mkfile(self.uid, faddr, data=self.file_data)

        faddr = Address.Make(self.uid, '/disk/filesystem test folder')
        result = Bus().content(self.uid, faddr.id, self.list_params)
        self.assertTrue('this' in result, result)
        self.assertTrue('list' in result, result)
        self.assertEqual('/disk/filesystem test folder', result['this']['id'])
        self.assertEqual(result['this']['id'], '/disk/filesystem test folder')
        for field in self.folder_fields:
            self.assertTrue(field in result['this'])
            if field in self.nonempty_fields:
                self.assertNotEqual(result['this'][field], None)
        self.assertTrue(len(result['list']) == 2, result['list'])
        self.assertEqual('/disk/filesystem test folder/inner folder', result['list'][0]['id'])
        self.assertEqual('/disk/filesystem test folder/inner file', result['list'][1]['id'])

    def test_tree_folder(self):

        def _assert_inner(bunch):
            self.assertTrue('this' in bunch)
            self.assertTrue('list' in bunch)
            for field in ('id', 'type', 'name'):
                self.assertTrue(bunch['this'].get(field) is not None, bunch)
            if bunch['list']:
                for item in bunch['list']:
                    _assert_inner(item)

        result = Bus().tree(
            self.uid,
            Address.Make(self.uid, '/disk').id,
            2,
            'name',
            2
        )
        _assert_inner(result)

    def test_info_by_file_id(self):
        """Протестировать ответ ручки info_by_file_id
        в случаях запроса общего ресурса владельцем файла и не владельцем
        """
        owner_uid = self.uid
        uid = self.second_uid

        self.create_user(uid, noemail=1)

        shared_folder_path = '/disk/shared'
        shared_file_path = '%s/123.txt' % shared_folder_path
        self.json_ok('mkdir', {'uid': self.uid, 'path': shared_folder_path})
        self.upload_file(owner_uid, shared_file_path)
        self.json_ok('share_create_group', {'uid': self.uid, 'path': shared_folder_path})

        opts = {
            'rights': 660,
            'universe_login': 'boo@boo.ru',
            'universe_service': 'email',
            'avatar': 'http://localhost/echo',
            'name': 'mpfs-test',
            'connection_id': '1234',
            'uid': owner_uid,
            'path': shared_folder_path,
        }
        result = self.json_ok('share_invite_user', opts)
        hash_ = result['hash']
        assert hash_

        folder_info = self.json_ok('share_activate_invite', {'hash': hash_, 'uid': uid})
        assert folder_info

        r1 = self.json_ok('info', {'uid': uid, 'path': shared_file_path, 'meta': ''})
        r2 = self.json_ok('info', {'uid': owner_uid, 'path': shared_file_path, 'meta': ''})
        assert r1 != r2

        file_id = r1['meta']['file_id']

        self.json_error('info_by_file_id', {'uid': uid, 'file_id': file_id, 'meta': ''})

        r = self.json_ok('info_by_file_id', {'uid': owner_uid, 'file_id': file_id, 'meta': ''})
        assert r['meta']['group']['owner']['uid'] == owner_uid
        assert r['meta']['group']['is_owned'] == 1

        r = self.json_ok('info_by_file_id', {'uid': uid, 'owner_uid': owner_uid, 'file_id': file_id, 'meta': ''})
        assert r['meta']['group']['owner']['uid'] == owner_uid
        assert r['meta']['group']['is_owned'] == 0

    def test_info_by_file_id_for_public_resource(self):
        """
        Проверить что в возвращаемой информации по публичному ресурсу содержится количество просмотров.

        Запрашивает владелец ресурса.
        """
        owner_uid = self.uid

        self.create_user(self.second_uid, noemail=1)

        public_folder_path = '/disk/public'
        self.json_ok('mkdir', {'uid': owner_uid, 'path': public_folder_path})
        self.json_ok('set_public', {'uid': owner_uid, 'path': public_folder_path})
        response = self.json_ok('info', {'uid': owner_uid, 'path': public_folder_path, 'meta': ''})
        file_id = response['meta']['file_id']

        response = self.json_ok('info_by_file_id', {'uid': owner_uid, 'file_id': file_id,
                                                    'meta': ''})
        assert 'views_counter' in response['meta']

        response = self.json_ok('info_by_file_id', {'uid': owner_uid, 'file_id': file_id,
                                                    'meta': 'views_counter,size'})
        assert 'views_counter' in response['meta']

        response = self.json_ok('info_by_file_id', {'uid': owner_uid, 'file_id': file_id,
                                                    'meta': 'size'})
        assert 'views_counter' not in response['meta']

    def test_list_views_counter_in_meta(self):
        uid = self.uid
        self.create_user(self.second_uid, noemail=1)
        public_folder_path = '/disk/pub'
        self.json_ok('mkdir', {'uid': uid, 'path': public_folder_path})
        self.json_ok('set_public', {'uid': uid, 'path': public_folder_path})
        root_listing = self.json_ok('list', {'uid': uid, 'path': '/disk', 'meta': 'views_counter'})
        for item in root_listing:
            self.assertIn('meta', item)
            if item['meta'].get('public_hash'):
                self.assertIn('views_counter', item['meta'])
        subfolder_listing = self.json_ok('list', {'uid': uid, 'path': public_folder_path, 'meta': 'views_counter'})
        self.assertIn('views_counter', subfolder_listing[0]['meta'])
        subfolder_listing = self.json_ok('list', {'uid': uid, 'path': public_folder_path, 'meta': ''})
        self.assertNotIn('views_counter', subfolder_listing[0]['meta'])

    def test_bulk_info_by_resource_ids_meta_custom_preview(self):
        self.upload_file(self.uid, '/disk/1.jpg')
        resp = self.json_ok('info', {'uid': self.uid, 'path': '/disk/1.jpg', 'meta': 'resource_id'})
        resource_id = resp['meta']['resource_id']
        body = [resource_id]
        resp = self.json_ok('bulk_info_by_resource_ids', {'uid': self.uid, 'meta': 'file_id,custom_preview', 'preview_size': 'L'}, json=body)
        assert len(resp) == 1
        custom_preview = resp[0]['meta']['custom_preview']
        assert custom_preview
        assert 'size=L' in custom_preview

    def test_bulk_info_by_resource_ids(self):
        self.upload_file(self.uid, '/disk/1.jpg')
        self.upload_file(self.uid, '/disk/2.jpg')
        trash_path = self.json_ok('trash_append', {'uid': self.uid, 'path': '/disk/2.jpg'})['this']['id']
        self.upload_file(self.uid, '/disk/3.jpg')
        self.upload_file(self.uid, '/notes/4.txt')

        file_id_disk = self.json_ok('info', {'uid': self.uid, 'path': '/disk/1.jpg', 'meta': 'file_id'})['meta']['file_id']
        file_id_trash = self.json_ok('info', {'uid': self.uid, 'path': trash_path, 'meta': 'file_id'})['meta']['file_id']
        file_id_hidden = self.json_ok('info', {'uid': self.uid, 'path': '/disk/3.jpg', 'meta': 'file_id'})['meta']['file_id']
        file_id_notes = self.json_ok('info', {'uid': self.uid, 'path': '/notes/4.txt', 'meta': 'file_id'})['meta']['file_id']
        self.json_ok('rm', {'uid': self.uid, 'path': '/disk/3.jpg'})

        body = []
        for file_id in (file_id_disk, file_id_hidden, file_id_trash, file_id_notes):
            body.append("%s:%s" % (self.uid, file_id))

        resp = self.json_ok('bulk_info_by_resource_ids', {'uid': self.uid, 'meta': 'file_id'}, json=body)
        assert len(resp) == 2
        assert resp[0]['meta']['file_id'] == file_id_disk
        assert resp[1]['meta']['file_id'] == file_id_trash

        resp = self.json_ok('bulk_info_by_resource_ids', {'uid': self.uid, 'enable_service_ids': '/trash,/attach', 'meta': 'file_id'}, json=body)
        assert len(resp) == 1
        assert resp[0]['meta']['file_id'] == file_id_trash

        resp = self.json_ok('bulk_info_by_resource_ids',  {'uid': self.uid, 'enable_service_ids': '/notes', 'meta': 'file_id'}, json=body)
        assert len(resp) == 1
        assert resp[0]['meta']['file_id'] == file_id_notes

    @pytest.mark.skipif(not INIT_USER_IN_POSTGRES, reason='pg test')
    def test_rate_limiter_bulk_info_by_resource_ids_for_lenta_worker(self):
        file_path = '/disk/file.txt'
        self.upload_file(self.uid, file_path)
        resource_id = self.json_ok('info',
                                   {'uid': self.uid, 'path': file_path, 'meta': 'resource_id'})['meta']['resource_id']
        with mock.patch('mpfs.core.services.rate_limiter_service.rate_limiter.is_limit_exceeded',
                        return_value=False) as rl, \
             mock.patch('mpfs.core.base.RATE_LIMITER_BULK_INFO_BY_RESOURCE_IDS_ENABLED', True):
            self.json_ok('bulk_info_by_resource_ids', {'uid': self.uid}, json=[resource_id],
                         headers={'user-agent': 'iva8-1bd0a1f091bb/lenta-worker/100.1298.2'})
            rl.assert_called_once()

    @pytest.mark.skipif(not INIT_USER_IN_POSTGRES, reason='pg test')
    def test_rate_limiter_bulk_info_by_resource_ids_for_common_client(self):
        file_path = '/disk/file.txt'
        self.upload_file(self.uid, file_path)
        resource_id = self.json_ok('info',
                                   {'uid': self.uid, 'path': file_path, 'meta': 'resource_id'})['meta']['resource_id']
        with mock.patch('mpfs.core.services.rate_limiter_service.rate_limiter.is_limit_exceeded',
                        return_value=False) as rl:
            self.json_ok('bulk_info_by_resource_ids', {'uid': self.uid}, json=[resource_id])
            rl.assert_not_called()

    @pytest.mark.skipif(not INIT_USER_IN_POSTGRES, reason='pg test')
    def test_rate_limiter_bulk_info_by_resource_ids_for_different_client(self):
        file_path = '/disk/file.txt'
        self.upload_file(self.uid, file_path)
        resource_id = self.json_ok('info',
                                   {'uid': self.uid, 'path': file_path, 'meta': 'resource_id'})['meta']['resource_id']
        with mock.patch('mpfs.core.services.rate_limiter_service.rate_limiter.is_limit_exceeded',
                        return_value=False) as rl:
            self.json_ok('bulk_info_by_resource_ids', {'uid': self.uid}, json=[resource_id],
                         headers={'user-agent': 'iva8-1bd0a1f091bb/lenta-loader/100.1298.2'})
            rl.assert_not_called()

    def test_bulk_info_by_resource_ids_for_photounlim(self):
        self.json_ok('enable_unlimited_autouploading', {'uid': self.uid})
        self.upload_file(self.uid, '/photounlim/test.jpg')
        photo_rid = self.json_ok('info', {'uid': self.uid, 'path': '/photounlim/test.jpg', 'meta': 'resource_id'})['meta']['resource_id']
        self.json_ok('bulk_info_by_resource_ids', {'uid': self.uid, 'meta': 'file_id'}, json=[photo_rid])

    def test_resource_by_file_id_shared_diff_path(self):
        """Протестировать поиск расшаренного ресурса от имени гостя
        по file_id, когда пути различаются у гостя и владельца.
        """
        owner_uid = self.uid
        uid = self.second_uid

        self.create_user(uid, noemail=True)

        owner_folder = '/disk/shared'
        self.json_ok('mkdir', {'uid': owner_uid, 'path': owner_folder})
        owner_file = posixpath.join(owner_folder, '123.txt')
        self.upload_file(owner_uid, owner_file)
        self.json_ok('share_create_group', {'uid': self.uid, 'path': owner_folder})

        opts = {
            'rights': 660,
            'universe_login': 'boo@boo.ru',
            'universe_service': 'email',
            'avatar': 'http://localhost/echo',
            'name': 'mpfs-test',
            'connection_id': '1234',
            'uid': owner_uid,
            'path': owner_folder,
        }
        response = self.json_ok('share_invite_user', opts)
        hash_ = response['hash']
        self.json_ok('share_activate_invite', {'hash': hash_, 'uid': uid})

        folder = owner_folder + '1'
        self.json_ok('move', {
            'uid': uid, 'src': uid + ':' + owner_folder, 'dst': uid + ':' + folder
        })

        file_ = posixpath.join(folder, '123.txt')
        response = self.json_ok('info', {'uid': uid, 'path': file_, 'meta': 'file_id'})
        file_id = response['meta']['file_id']

        fs = Bus()
        try:
            fs.resource_by_file_id(uid, file_id, owner_uid)
        except ResourceNotFound:
            assert False
