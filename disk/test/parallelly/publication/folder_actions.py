#!/usr/bin/python
# -*- coding: utf-8 -*-
import copy

from test.parallelly.publication.base import BasePublicationMethods, UserRequest
from test.fixtures.users import user_1

import mpfs.engine.process

from mpfs.core.address import Address, PublicAddress
from mpfs.core import base as core
from mpfs.core.bus import Bus
from mpfs.metastorage.mongo.util import decompress_data, id_for_key
from mpfs.core.filesystem.dao.legacy import CollectionRoutedDatabase


mpfs_db = CollectionRoutedDatabase()


class FolderActionsPublicationTestCase(BasePublicationMethods):

    def test_copy_pub_folder(self):
        uid = str(self.uid)
        self.make_dir(True)
        self.upload_file(uid, '/disk/pub/private file.ext')
        self.make_file()

        addr = Address.Make(self.uid, '/disk/pub/' + self.pub_filename).id
        resource = Bus().resource(self.uid, addr)
        hash = resource.get_public_hash()
        req = UserRequest({})
        req.set_args(
            {'hash': hash,
             'bytes_downloaded': self.file_data['size'],
             'count': 1,
             'uid': self.uid})
        core.kladun_download_counter_inc(req)

        opts = {
            'uid': uid,
            'path': '/disk/pub',
            'meta': ''
            }
        folder_info = self.json_ok('info', opts)
        public_hash = folder_info['meta']['public_hash']
        public_addr = PublicAddress.Make(public_hash, '/private file.ext')
        opts = {
            'hash': public_addr.id,
            'bytes_downloaded': 100,
            }
        self.service('kladun_download_counter_inc', opts)
        #======================================================================
        file_info = mpfs_db.user_data.find_one({'uid': uid, 'path': '/disk/pub/' + self.pub_filename})
        zdata = decompress_data(file_info['zdata'])
        self.assertEqual(file_info['data']['download_counter'], 1)
        self.assertFalse('download_counter' in zdata['pub'])
        #======================================================================
        opts = {
            'uid': uid,
            'src': '/disk/pub',
            'dst': '/disk/priv',
            }
        self.json_ok('async_copy', opts)
        #======================================================================
        file_info = mpfs_db.user_data.find_one({'uid': uid, 'path': '/disk/pub/' + self.pub_filename})
        zdata = decompress_data(file_info['zdata'])
        self.assertEqual(file_info['data']['download_counter'], 1)
        self.assertFalse('download_counter' in zdata['pub'])
        file_info = mpfs_db.user_data.find_one({'uid': uid, 'path': '/disk/pub/private file.ext'})
        zdata = decompress_data(file_info['zdata'])
        self.assertFalse('pub' in zdata)
        file_info = mpfs_db.user_data.find_one({'uid': uid, 'path': '/disk/priv/' + self.pub_filename})
        self.assertFalse('download_counter' in file_info['data'])
        file_info = mpfs_db.user_data.find_one({'uid': uid, 'path': '/disk/priv/private file.ext'})
        self.assertFalse('download_counter' in file_info['data'])
        #======================================================================

    def test_move_pub_folder(self):
        self.make_dir(True)
        self.make_file()

        addr = Address.Make(self.uid, '/disk/pub/' + self.pub_filename).id
        resource = Bus().resource(self.uid, addr)
        hash = resource.get_public_hash()
        req = UserRequest({})
        req.set_args(
            {'hash': hash,
             'bytes_downloaded': self.file_data['size'],
             'count': 1,
             'uid': self.uid})
        core.kladun_download_counter_inc(req)

        uid = str(self.uid)
        opts = {
            'uid': uid,
            'src': '/disk/pub',
            'dst': '/disk/pub_1',
            }
        self.json_ok('async_move', opts)
        #======================================================================
        self.assertTrue('public_hash' in decompress_data(
            mpfs_db.user_data.find_one({'uid': uid, 'path': '/disk/pub_1'})['zdata'])['pub'])
        self.assertEqual(mpfs_db.user_data.find_one(
            {'uid': uid, 'path': '/disk/pub/' + self.pub_filename}), None)
        self.assertEqual(mpfs_db.user_data.find_one(
            {'uid': uid, 'path': '/disk/pub/private file.ext'}), None)
        file_info = mpfs_db.user_data.find_one(
            {'uid': uid, 'path': '/disk/pub_1/' + self.pub_filename})
        zdata = decompress_data(file_info['zdata'])
        self.assertEqual(file_info['data']['download_counter'], 1)
        self.assertFalse('download_counter' in zdata['pub'])
        #======================================================================
        opts = {
            'uid': uid,
            'src': '/disk/pub_1',
            'dst': '/disk/pub',
            }
        self.json_ok('async_move', opts)
        #======================================================================
        self.assertTrue('public_hash' in decompress_data(
            mpfs_db.user_data.find_one({'uid': uid, 'path': '/disk/pub'})['zdata'])['pub'])
        self.assertEqual(mpfs_db.user_data.find_one(
            {'uid': uid, 'path': '/disk/pub_1/' + self.pub_filename}), None)
        self.assertEqual(mpfs_db.user_data.find_one(
            {'uid': uid, 'path': '/disk/pub_1/private file.ext'}), None)
        file_info = mpfs_db.user_data.find_one(
            {'uid': uid, 'path': '/disk/pub/' + self.pub_filename})
        zdata = decompress_data(file_info['zdata'])
        self.assertEqual(file_info['data']['download_counter'], 1)
        self.assertFalse('download_counter' in zdata['pub'])
        #======================================================================

        opts = {
            'uid': uid,
            'path': '/disk/pub_parent',
            }
        self.json_ok('mkdir', opts)

        opts = {
            'uid': uid,
            'src': '/disk/pub',
            'dst': '/disk/pub_parent/pub',
            }
        self.json_ok('async_move', opts)
        #======================================================================
        self.assertEqual(
            mpfs_db.user_data.find_one({'uid': uid, 'path': '/disk/pub'}), None)
        self.assertEqual(mpfs_db.user_data.find_one(
            {'uid': uid, 'path': '/disk/pub/' + self.pub_filename}), None)
        self.assertEqual(mpfs_db.user_data.find_one(
            {'uid': uid, 'path': '/disk/pub/private file.ext'}), None)
        self.assertTrue('public_hash' in decompress_data(mpfs_db.user_data.find_one(
            {'uid': uid, 'path': '/disk/pub_parent/pub'})['zdata'])['pub'])
        file_info = mpfs_db.user_data.find_one(
            {'uid': uid, 'path': '/disk/pub_parent/pub/' + self.pub_filename})
        zdata = decompress_data(file_info['zdata'])
        self.assertEqual(file_info['data']['download_counter'], 1)
        self.assertFalse('download_counter' in zdata['pub'])
        #======================================================================
        opts = {
            'uid': uid,
            'src': '/disk/pub_parent/pub',
            'dst': '/disk/pub',
            }
        self.json_ok('async_move', opts)
        #======================================================================
        self.assertEqual(mpfs_db.user_data.find_one(
            {'uid': uid, 'path': '/disk/pub_parent/pub'}), None)
        self.assertEqual(mpfs_db.user_data.find_one(
            {'uid': uid, 'path': '/disk/pub_parent/pub/' + self.pub_filename}), None)
        self.assertEqual(mpfs_db.user_data.find_one(
            {'uid': uid, 'path': '/disk/pub_parent/pub/private file.ext'}), None)
        self.assertTrue('public_hash' in decompress_data(
            mpfs_db.user_data.find_one({'uid': uid, 'path': '/disk/pub'})['zdata'])['pub'])
        file_info = mpfs_db.user_data.find_one(
            {'uid': uid, 'path': '/disk/pub/' + self.pub_filename})
        zdata = decompress_data(file_info['zdata'])
        self.assertEqual(file_info['data']['download_counter'], 1)
        self.assertFalse('download_counter' in zdata['pub'])
        #======================================================================

    def test_move_from_pub_folder(self):
        self.make_dir(True)
        self.make_file()
        self.upload_file(self.uid, '/disk/pub/private file.ext')
        addr = Address.Make(self.uid, '/disk/pub/' + self.pub_filename).id
        resource = Bus().resource(self.uid, addr)
        hash = resource.get_public_hash()
        req = UserRequest({})
        req.set_args(
            {'hash': hash,
             'bytes_downloaded': self.file_data['size'],
             'count': 1,
             'uid': self.uid})
        core.kladun_download_counter_inc(req)
        link_id = decompress_data(mpfs_db.user_data.find_one(
            {'uid': str(self.uid), 'path': '/disk/pub'})['zdata'])['pub']['symlink']
        uid, key = link_id.split(':')
        _id = id_for_key(uid, '/%s' % key)
        self.assertNotEqual(
            mpfs_db.link_data.find_one({'_id': _id, 'uid': self.uid}), None)
        for file_name in ('public.some.info', 'private'):
            opts = {
                'uid': self.uid,
                'src': '/disk/pub/%s file.ext' % file_name,
                'dst': '/disk/%s file.ext' % file_name,
                }
            self.json_ok('async_move', opts)
            self.assertEqual(mpfs_db.user_data.find_one(
                {'uid': self.uid, 'path': '/disk/pub/%s file.ext' % file_name}), None)
            self.assertNotEqual(mpfs_db.user_data.find_one(
                {'uid': self.uid, 'path': '/disk/%s file.ext' % file_name}), None)
            if file_name == 'public.some.info':
                self.assertTrue('pub' in decompress_data(mpfs_db.user_data.find_one(
                    {'uid': self.uid, 'path': '/disk/%s file.ext' % file_name})['zdata']))
            else:
                self.assertFalse('pub' in decompress_data(mpfs_db.user_data.find_one(
                    {'uid': self.uid, 'path': '/disk/%s file.ext' % file_name})['zdata']))

        opts = {
            'uid': self.uid,
            'path': '/disk/pub',
            'meta': ''
            }
        folder_info = self.json_ok('info', opts)
        public_hash = folder_info['meta']['public_hash']

        for file_name in ('public.some.info', 'private'):
            opts = {
                'uid': self.uid,
                'src': '/disk/%s file.ext' % file_name,
                'dst': '/disk/pub/%s file.ext' % file_name,
                }
            self.json_ok('async_move', opts)
            self.assertEqual(mpfs_db.user_data.find_one(
                {'uid': self.uid, 'path': '/disk/%s file.ext' % file_name}), None)
            self.assertNotEqual(mpfs_db.user_data.find_one(
                {'uid': self.uid, 'path': '/disk/pub/%s file.ext' % file_name}), None)
            public_addr = PublicAddress.Make(
                public_hash, '/%s file.ext' % file_name)
            opts = {
                'hash': public_addr.id,
                'bytes_downloaded': 100,
                }
            self.service('kladun_download_counter_inc', opts)
            if file_name == 'public.some.infofile.ext':
                self.assertTrue('download_counter' in mpfs_db.user_data.find_one(
                    {'uid': self.uid, 'path': '/disk/pub/%s file.ext' % file_name})['data'])

    def test_grab_public_folder(self):
        self.make_dir(True)
        self.make_file()
        foldaddr = Address.Make(self.uid, self.pub_folder).id
        resource = Bus().resource(self.uid, foldaddr)
        hash = resource.get_public_hash()

        request = self.get_request({'uid': self.uid,
                                    'private_hash': hash,
                                    'name': None,
                                    'connection_id': '',
                                    'save_path': None})
        core.async_public_copy(request)

        request.set_args({'uid': self.uid,
                          'path': Address.Make(self.uid, u'/disk/Загрузки/' + self.pub_foldername).id,
                          'connection_id': '',
                          'unzip_file_id': 0,
                          'meta': ''})
        result = core.info(request)
        self.assertEqual(result['this']['id'],
                         u'/disk/Загрузки/' + self.pub_foldername)
        self.assertFalse('download_counter' in result['this']['meta'])

        spec = {
            'uid': str(self.uid),
            'path': u'/disk/Загрузки/' + self.pub_foldername,
            }
        zdata = decompress_data(mpfs_db.user_data.find_one(spec)['zdata'])
        self.assertFalse('folder_url' in zdata['setprop'])

        resource = Bus().resource(self.uid, foldaddr)
        self.assertEqual(resource.meta['download_counter'], 1)
        spec = {
            'uid': str(self.uid),
            'path': Address.Make(self.uid, self.pub_folder).path,
            }
        zdata = decompress_data(mpfs_db.user_data.find_one(spec)['zdata'])
        self.assertEqual(zdata['setprop'].get('folder_url'), None)

    def test_grab_folder_with_infected_file(self):
        self.uid_1 = user_1.uid
        self.create_user(self.uid_1)
        self.make_dir(True)
        subfolderaddr = Address.Make(self.uid, self.pub_subfolder).id
        file_data = copy.deepcopy(self.file_data)

        Bus().mkdir(self.uid, subfolderaddr, data=file_data)
        subfileaddr = Address.Make(self.uid, self.pub_subfile).id

        file_data = copy.deepcopy(self.file_data)
        Bus().mkfile(self.uid, subfileaddr, data=file_data)

        file_data = copy.deepcopy(self.file_data)
        file_data['meta']['drweb'] = 2
        file_data['meta']['md5'] = file_data['meta']['md5'][::-1]  # we need different file
        tmp = Address.Make(self.uid, '/disk/pub/subfolder/pubic subfile virus').id
        Bus().mkfile(self.uid, tmp, data=file_data)

        opts = {
            'uid': self.uid,
            'path': '/disk/pub/subfolder/pubic subfile virus',
            'meta': '',
            }
        list_result = self.json_ok('info', opts)
        self.assertEqual(list_result['meta']['drweb'], 2)
        opts = {
            'uid': self.uid,
            'path': '/disk/pub',
            'meta': ''
            }
        folder_info = self.json_ok('info', opts)
        public_hash = folder_info['meta']['public_hash']
        opts = {
            'uid': self.uid_1,
            'private_hash': public_hash,
            'connection_id': ''
            }
        copy_meth = self.json_ok('async_public_copy', opts)
        self.assertDictContainsSubset({'target_path': u'/disk/Загрузки/pub'}, copy_meth)
        opts = {
            'uid': self.uid_1,
            'path': '/disk/Загрузки/pub/subfolder',
            'meta': '',
            }
        list_result = self.json_ok('list', opts)
        found = False
        for element in list_result:
            if element['name'] == 'pubic subfile virus':
                found = True
        self.assertFalse(found)

    def test_move_folder_to_trash(self):
        self.make_dir(True)
        subfolderaddr = Address.Make(self.uid, self.pub_subfolder).id
        Bus().mkdir(self.uid, subfolderaddr, data=self.file_data)
        self.upload_file(self.uid, '/disk/pub/subfolder/file.txt')
        uid = str(self.uid)
        file_info = mpfs_db.user_data.find_one(
            {'uid': uid, 'path': '/disk/pub/subfolder/file.txt'})
        self.assertFalse('pub' in decompress_data(file_info['zdata']))
        link_id = decompress_data(mpfs_db.user_data.find_one(
            {'uid': str(self.uid), 'path': '/disk/pub'})['zdata'])['pub']['symlink']
        uid, key = link_id.split(':')
        _id = id_for_key(uid, '/%s' % key)
        self.assertNotEqual(
            mpfs_db.link_data.find_one({'_id': _id, 'uid': uid}), None)

        opts = {'uid': uid, 'path': '/disk/pub'}
        self.json_ok('async_trash_append', opts)

        self.assertNotEqual(
            mpfs_db.link_data.find_one({'_id': _id, 'uid': uid}), None)
        file_info = mpfs_db.trash.find_one(
            {'uid': uid, 'data.original_id': '/disk/pub/subfolder/file.txt'})
        self.assertFalse('download_counter' in file_info['data'])
        self.assertFalse('pub' in decompress_data(file_info['zdata']))

    def test_rm_pub_folder(self):
        self.make_dir(True)
        #======================================================================
        uid = str(self.uid)
        self.upload_file(uid, '/disk/pub/private file.ext')
        self.upload_file(uid, '/disk/pub/public file.ext')
        opts = {
            'uid': uid,
            'path': '/disk/pub/public file.ext',
            }
        self.json_ok('set_public', opts)
        opts = {
            'uid': uid,
            'path': '/disk/pub',
            'meta': ''
            }
        folder_info = self.json_ok('info', opts)
        public_hash = folder_info['meta']['public_hash']
        public_addr = PublicAddress.Make(public_hash, '/public file.ext')
        opts = {
            'hash': public_addr.id,
            'bytes_downloaded': 100,
            }
        self.service('kladun_download_counter_inc', opts)
        public_addr = PublicAddress.Make(public_hash, '/private file.ext')
        opts = {
            'hash': public_addr.id,
            'bytes_downloaded': 100,
            }
        self.service('kladun_download_counter_inc', opts)
        #======================================================================

        folder_link_id = decompress_data(mpfs_db.user_data.find_one(
            {'uid': str(self.uid), 'path': '/disk/pub'})['zdata'])['pub']['symlink']
        uid, key = folder_link_id.split(':')
        folder_id = id_for_key(uid, '/%s' % key)
        self.assertNotEqual(
            mpfs_db.link_data.find_one({'_id': folder_id, 'uid': str(self.uid)}), None)

        file_link_id = decompress_data(mpfs_db.user_data.find_one(
            {'uid': str(self.uid), 'path': '/disk/pub/public file.ext'})['zdata'])['pub']['symlink']
        uid, key = file_link_id.split(':')
        file_id = id_for_key(uid, '/%s' % key)

        opts = {
            'uid': uid,
            'path': '/disk/pub',
            }
        self.json_ok('rm', opts)

        deleted_folder_info = mpfs_db.link_data.find_one(
            {'_id': folder_id, 'uid': str(self.uid)})
        self.assertTrue('dtime' in deleted_folder_info['data'])

        deleted_file_info = mpfs_db.link_data.find_one(
            {'_id': file_id, 'uid': str(self.uid)})
        self.assertTrue('dtime' in deleted_file_info['data'])

    def test_trash_append_folder_with_public_folder(self):
        uid = str(self.uid)
        opts = {
            'uid': uid,
            'path': '/disk/pub'
        }
        self.json_ok('mkdir', opts)

        opts = {
            'uid': uid,
            'path': '/disk/pub/inner pub folder',
            'meta': '',
            }
        self.json_ok('mkdir', opts)
        self.json_ok('set_public', opts)

        folder_info = self.json_ok('info', opts)
        public_hash = folder_info['meta']['public_hash']

        opts = {
            'uid': uid,
            'path': '/disk/pub',
            }
        self.json_ok('async_trash_append', opts)

        opts = {
            'private_hash': public_hash,
        }
        self.json_error('public_info', opts, 71)

    def test_async_public_copy_by_any_url(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/d1'})
        self.upload_file(self.uid, '/disk/d1/1.jpg')
        uid2 = self.user_2.uid
        self.create_user(uid2)
        urls = self.json_ok('set_public', {'uid': self.uid, 'path': '/disk/d1'})

        n = 0
        for url_type in ['url', 'short_url', 'hash', 'short_url_named']:
            url = urls[url_type]
            n += 1
            dir_name = 'test_%s' % n
            self.json_ok('async_public_copy',
                         {'uid': uid2, 'private_hash': url, 'save_path': '/disk', 'name': dir_name})
            info = self.json_ok('info', {'uid': uid2, 'path': '/disk/' + dir_name})
            assert 'dir' == info['type']
            info = self.json_ok('info', {'uid': uid2, 'path': '/disk/' + dir_name + '/1.jpg'})
            assert 'file' == info['type']
