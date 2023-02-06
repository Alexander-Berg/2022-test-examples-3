# -*- coding: utf-8 -*-
import copy
import mock
import uuid

from test.parallelly.filesystem.base import CommonFilesystemTestCase

import mpfs.engine.process
import mpfs.core.base as mbase

from mpfs.common import errors
from mpfs.core.bus import Bus
from mpfs.core.address import Address
from mpfs.core.filesystem.hardlinks.common import AbstractLink
from mpfs.core.filesystem.dao.legacy import CollectionRoutedDatabase


db = CollectionRoutedDatabase()
usrctl = mpfs.engine.process.usrctl()


class MiscFilesystemTestCase(CommonFilesystemTestCase):
    def setup_method(self, method):
        super(MiscFilesystemTestCase, self).setup_method(method)
        self.file_data = copy.deepcopy(MiscFilesystemTestCase.file_data)
        self._mkdirs()
        self._mkfiles()

    def test_store_with_hardlink(self):
        hardlink_addr = Address.Make(self.uid, '/disk/filesystem test folder/hardlink file').id
        request = self.get_request({'uid': self.uid,
                                    'path': hardlink_addr,
                                    'force': 0,
                                    'md5': self.file_data['meta']['md5'],
                                    'sha256': self.file_data['meta']['sha256'],
                                    'size': self.file_data['size'],
                                    'replace_md5': '',
                                    'client_type': 'desktop',
                                    'callback': None,
                                    'changes': {},
                                    'connection_id': None,
                                    'user_agent': None,
                                    'device_original_path': None,
                                    'device_collections': None
                                    })
        result = mbase.store(request)

        self.assertEqual(result.get('status', None), 'hardlinked')

    def test_setprop(self):
        folder_addr = Address.Make(self.uid, '/disk/filesystem test folder').id
        file_addr = Address.Make(self.uid, '/disk/filesystem test folder/inner file').id

        Bus().setprop(self.uid, file_addr, {'key': 'value'})
        fileinfo = Bus().info(self.uid, file_addr)
        self.assertEqual('value', fileinfo['this']['meta'].get('key'))

        Bus().setprop(self.uid, folder_addr, {'key': 'value'})
        folderinfo = Bus().info(self.uid, folder_addr)
        self.assertEqual('value', folderinfo['this']['meta'].get('key'))

    def test_preconditions_failed(self):
        request = self.get_request({'uid': self.uid,
                                    'path': self.uid + ':/disk/empty path',
                                    'force': 0,
                                    'md5': '',
                                    'sha256': '',
                                    'size': '',
                                    'replace_md5': '12345md5',
                                    'client_type': 'desktop',
                                    'callback': None,
                                    'changes': {},
                                    'connection_id': None,
                                    'user_agent': None,
                                    'device_original_path': None,
                                    'device_collections': None})

        self.assertRaises(errors.PreconditionsFailed, mbase.store, request)

        file_addr = Address.Make(self.uid, '/disk/filesystem test folder/inner file').id
        request.set_args({'uid': self.uid,
                          'path': file_addr,
                          'force': 0,
                          'md5': '',
                          'sha256': '',
                          'size': '',
                          'replace_md5': 'wrong_md5',
                          'client_type': 'desktop',
                          'callback': None,
                          'changes': {},
                          'connection_id': None,
                          'user_agent': None,
                          'device_original_path': None,
                          'device_collections': None
                          })
        self.assertRaises(errors.PreconditionsFailed, mbase.store, request)

    def test_resources_unique_id(self):
        addr = Address.Make(self.uid, '/disk/res_with_id').id
        addr_copied = Address.Make(self.uid, '/disk/res_with_id_cp').id
        addr_moved = Address.Make(self.uid, '/disk/res_with_id_mv').id

        def _check_fids():
            fid = Bus().info(self.uid, addr).get('this').get('meta').get('file_id')
            self.assertTrue(fid is not None)

            Bus().copy_resource(self.uid, addr, addr_copied, True)
            copied_fid = Bus().info(self.uid, addr_copied).get('this').get('meta').get('file_id')
            self.assertTrue(fid != copied_fid)

            Bus().move_resource(self.uid, addr, addr_moved, True)
            moved_fid = Bus().info(self.uid, addr_moved).get('this').get('meta').get('file_id')
            self.assertTrue(fid == moved_fid)

            Bus().rm(self.uid, addr_moved)

        Bus().mkfile(self.uid, addr, data=self.file_data)
        _check_fids()

        Bus().mkdir(self.uid, addr)
        _check_fids()

    def test_predefined_system_folders(self):
        addr = Address.Make(self.uid, u'/disk/Загрузки').id
        Bus().mksysdir(self.uid, 'downloads', 'ru')

        info = Bus().info(self.uid, addr)['this']
        self.assertEqual(info['meta']['folder_type'], 'downloads')
        Bus().rm(self.uid, addr)

        addr = Address.Make(self.uid, u'/disk/Фотокамера').id
        Bus().mksysdir(self.uid, 'photostream', 'ru')
        info = Bus().info(self.uid, addr)['this']
        self.assertEqual(info['meta']['folder_type'], 'photostream')
        Bus().rm(self.uid, addr)

    def test_conflicts_system_folders(self):
        addr = Address.Make(self.uid, u'/disk/Фотокамера').id
        addr_moved = Address.Make(self.uid, u'/disk/Фотокамера 1').id
        Bus().mkfile(self.uid, addr, data=self.file_data)

        Bus().mksysdir(self.uid, 'photostream', 'ru')
        info = Bus().info(self.uid, addr)['this']
        self.assertEqual(info['meta']['folder_type'], 'photostream')

        info = Bus().info(self.uid, addr_moved)['this']
        self.assertEqual(info['type'], 'file')

    def test_replace_file_lost_in_mulca(self):
        """Поверить ответ в случае наличия/отсутствия файла в мульке

        Клиент приходит в ручку store с параметром replace_md5 а также валидными md5,sha256,size и флагом force=1
        1. Если файл есть и md5 совпадает с replace_md5, и он есть в мульке, то отдаём 200 и {'status': 'hardlinked'}
        2. Если файл есть и md5 совпадает с replace_md5, но его нет в мульке, то отдаём 200 со ссылкой на загрузку.
        3. Если файл есть и md5 не совпадает с replace_md5, то отдаём 412.
        4. Если файла нет, то отдаём 412.

        .. note::
            Проверить 20х статусы в тестах мы не можем
        """

        file_id = Address.Make(self.uid, self.TEST_FILES[0]).id
        non_existent_file_id = Address.Make(self.uid, '/disk/{}'.format(uuid.uuid4().hex)).id
        meta_data = self.file_data['meta']
        opts = {
            'uid': self.uid,
            'path': file_id,
            'md5': meta_data['md5'],
            'sha256': meta_data['sha256'],
            'size': self.file_data['size'],
            'force': 1
        }

        opts['replace_md5'] = meta_data['md5']
        with mock.patch.object(AbstractLink, 'is_file_in_storage', return_value=True):
            response = self.json_ok('store', opts)
            assert 'hardlinked' == response['status']

        with mock.patch.object(AbstractLink, 'is_file_in_storage', return_value=False):
            response = self.json_ok('store', opts)
            assert 'upload_url' in response

        opts['replace_md5'] = uuid.uuid4().hex
        self.json_error('store', opts)
        assert self.response.status == 412

        opts['replace_md5'] = meta_data['md5']
        opts['path'] = non_existent_file_id
        self.json_error('store', opts)
        assert self.response.status == 412

    def test_able_to_store_avatars_preview_stids(self):
        avatars_stid = 'ava:disk:12345:' + 'aBc4' * 10
        self.upload_file(self.uid, '/disk/2.jpg', file_data={'pmid': avatars_stid})
        assert self.json_ok('info', {'path': '/disk/2.jpg', 'uid': self.uid, 'meta': ''})['meta']['pmid'] == avatars_stid


class SystemShareStockFolderTestCase(CommonFilesystemTestCase):
    def setup_method(self, method):
        super(SystemShareStockFolderTestCase, self).setup_method(method)
        from mpfs.core.services.stock_service import _setup
        _setup()

    def test_share_rights_simple_upload(self):
        args = {'uid': self.uid, 'path': '/share/somefile.txt'}
        self.json_error('store', args)

    def test_share_rights_hardlink_upload(self):
        self.upload_file(self.uid, '/disk/somefile.txt')

        args = {
            'uid': self.uid,
            'path': '/disk/somefile.txt',
            'meta': ''
        }
        info = self.json_ok('info', args)
        sha256, md5, size = info['meta']['sha256'], info['meta']['md5'], info['meta']['size']

        args = {
            'uid': self.uid,
            'path': '/share/somefile.txt',
            'sha256': sha256,
            'md5': md5,
            'size': size
        }

        self.json_error('store', args)

    def test_share_rights_mkdir(self):
        args = {
            'uid': self.uid,
            'path': '/share/dir'
        }
        self.json_error('mdkir', args)
