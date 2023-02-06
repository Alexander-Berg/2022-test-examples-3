# -*- coding: utf-8 -*-
from base import BaseShardingMethods
from mock import patch

import mpfs.engine.process
import mpfs.core.filesystem.base

from mpfs.config import settings
from mpfs.core.metastorage.control import disk

from test.common.sharing import CommonSharingMethods

dbctl = mpfs.engine.process.dbctl()


class FilesystemTestCase(BaseShardingMethods):
    def setup_method(self, method):
        super(FilesystemTestCase, self).setup_method(method)

        settings.mongo['options']['new_registration'] = True
        self.create_user(self.uid, noemail=1)

    def test_basic_methods(self):
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/test'})
        self.json_ok('info', {'uid': self.uid, 'path': '/disk/test'})

        self.upload_file(self.uid, '/disk/test/somefile.txt')
        self.json_ok('info', {'uid': self.uid, 'path': '/disk/test/somefile.txt'})

        with patch('mpfs.core.address.Address.add_trash_suffix'):
            self.json_ok('async_trash_append', {'uid': self.uid, 'path': '/disk/test'})
        self.json_ok('info', {'uid': self.uid, 'path': '/trash/test'})
        self.json_ok('info', {'uid': self.uid, 'path': '/trash/test/somefile.txt'})

        self.json_ok('async_trash_restore', {'uid': self.uid, 'path': '/trash/test'})
        self.json_ok('info', {'uid': self.uid, 'path': '/disk/test'})
        self.json_ok('info', {'uid': self.uid, 'path': '/disk/test/somefile.txt'})

        self.json_ok('async_move', {'uid': self.uid, 'src': '/disk/test', 'dst': '/disk/moved'})
        self.json_ok('info', {'uid': self.uid, 'path': '/disk/moved'})
        self.json_ok('info', {'uid': self.uid, 'path': '/disk/moved/somefile.txt'})
        self.json_error('info', {'uid': self.uid, 'path': '/disk/test'})


class RevisionTestCase(BaseShardingMethods, CommonSharingMethods):

    def setup_method(self, method):
        super(RevisionTestCase, self).setup_method(method)

        with patch.dict(settings.mongo['options'], {'new_registration': True}):
            self.create_user(self.uid, noemail=1)

            self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/test'})
            self.upload_file(self.uid, '/disk/test/somefile.txt')
            opts = {
                'uid': self.uid,
                'path': '/disk/test',
            }
            self.public_folder_hash = self.json_ok('set_public', opts)['hash']

            opts = {
                'uid': self.uid,
                'path': '/disk/test/somefile.txt',
            }
            self.public_file_hash = self.json_ok('set_public', opts)['hash']

    def _get_version(self, resource_path, uid=None):
        # version возвращается как string, поэтому конвертируем
        if not uid:
            uid = self.uid
        db_info = disk.show(uid, resource_path)
        return long(db_info.value.version)

    def test_revision_in_info(self):
        """
        Тестируем, что возвращается настоящая версия объекта в поле revision в ручке info
        """
        handler_info = self.json_ok('info', {'uid': self.uid, 'path': '/disk/test', 'meta': 'revision'})
        assert self._get_version('/disk/test') == handler_info['meta']['revision']
        handler_info = self.json_ok('info',
                                    {'uid': self.uid, 'path': '/disk/test/somefile.txt', 'meta': 'revision'})
        assert self._get_version('/disk/test/somefile.txt') == handler_info['meta']['revision']

    def test_revision_in_list(self):
        """
        Тестируем, что возвращается настоящая версия объекта в поле revision в ручке list
        """
        handler_info = self.json_ok('list', {'uid': self.uid, 'path': '/disk/test', 'meta': 'revision'})
        for i in handler_info:
            assert self._get_version(i['id']) == i['meta']['revision']
        handler_info = self.json_ok('list', {'uid': self.uid, 'path': '/disk/test/somefile.txt', 'meta': 'revision'})
        assert self._get_version('/disk/test/somefile.txt') == handler_info['meta']['revision']

    def test_revision_in_tree(self):
        """
        Тестируем, что возвращается настоящая версия объекта в поле revision в ручке tree
        """
        handler_info = self.json_ok('tree', {'uid': self.uid, 'path': '/disk', 'meta': 'revision'})
        for i in handler_info['resource']:
            assert self._get_version(i['id']) == i['meta']['revision']

    def test_revision_in_fulltree(self):
        """
        Тестируем, что возвращается настоящая версия объекта в поле revision в ручке fulltree
        """
        handler_info = self.json_ok('fulltree', {'uid': self.uid, 'path': '/disk', 'meta': 'revision'})
        assert self._get_version('/disk') == handler_info['this']['meta']['revision']
        for i in handler_info['list']:
            if i['this']['path'] == '/disk/test':
                assert self._get_version('/disk/test') == i['this']['meta']['revision']
                assert self._get_version('/disk/test/somefile.txt') == i['this']['list'][0]['this']['meta']['revision']

    def test_revision_in_timeline(self):
        """
        Тестируем, что возвращается настоящая версия объекта в поле revision в ручке timeline
        """
        handler_info = self.json_ok('timeline', {'uid': self.uid, 'path': '/disk', 'meta': 'revision'})
        for i in handler_info:
            assert self._get_version(i['id']) == i['meta']['revision']

    def test_revision_in_public_handlers(self):
        """
        Тестируем, что возвращается настоящая версия объекта в поле revision в ручках public_info и public_list
        """
        handler_info = self.json_ok('public_info',
                                    {'uid': self.uid, 'private_hash': self.public_folder_hash, 'meta': 'revision'})
        assert self._get_version('/disk/test') == handler_info['resource']['meta']['revision']
        handler_info = self.json_ok('public_list',
                                    {'uid': self.uid, 'private_hash': self.public_folder_hash, 'meta': 'revision'})
        assert self._get_version('/disk/test') == handler_info[0]['meta']['revision']

        handler_info = self.json_ok('public_info',
                                    {'uid': self.uid, 'private_hash': self.public_file_hash, 'meta': 'revision'})
        assert self._get_version('/disk/test/somefile.txt') == handler_info['resource']['meta']['revision']
        handler_info = self.json_ok('public_list',
                                    {'uid': self.uid, 'private_hash': self.public_file_hash, 'meta': 'revision'})
        assert self._get_version('/disk/test/somefile.txt') == handler_info['meta']['revision']

    def test_revision_invite_folders(self):
        """
        Проверяем, что возвращается правильная версия объекта в поле revision ручкой timeline в случае, когда у
        пользователь принял приглашение в расшаренную папку
        """
        self.json_ok('user_init', {'uid': self.uid_1})
        shared_folder_path = '/disk/shared'
        self.json_ok('mkdir', {'uid': self.uid, 'path': shared_folder_path})
        self.upload_file(self.uid, '/disk/shared/somefile.txt')
        self.create_group(uid=self.uid, path=shared_folder_path)
        hash_ = self.invite_user(
            uid=self.uid_1,
            email=self.email_1,
            path=shared_folder_path
        )
        self.activate_invite(uid=self.uid_1, hash=hash_)

        handler_info = self.json_ok('timeline', {'uid': self.uid_1, 'path': '/disk', 'meta': 'revision'})
        # Проверяем корректность корня и файла именно так, потому что файл принадлежит другому юзеру
        assert self._get_version('/disk', self.uid_1) == handler_info[0]['meta']['revision']
        assert self._get_version('/disk/shared/somefile.txt') == handler_info[1]['meta']['revision']
