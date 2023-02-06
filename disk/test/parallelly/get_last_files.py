# -*- coding: utf-8 -*-

import time
from nose_parameterized import parameterized
from itertools import count

from mock import patch

from mpfs.core.address import Address
from test.common.sharing import CommonSharingMethods
from mpfs.core.last_files.logic import get_last_files, SharedLastFilesProcessor, LAST_FILES_ITEMS_LIMIT
from mpfs.core.services.discovery_service import DiscoveryService


class GetLastFilesTestCase(CommonSharingMethods):
    c = count()
    now = time.time()

    @classmethod
    def setup_class(cls):
        super(GetLastFilesTestCase, cls).setup_class()
        with open('fixtures/xml/discovery.xml') as fd:
            xml = fd.read()
        with patch.object(DiscoveryService, 'open_url', return_value=xml):
            DiscoveryService().ensure_cache()

    def ts_tick(self):
        return self.now + next(self.c)

    def upload_file(self, *args, **kwargs):
        with patch.object(time, 'time', return_value=self.ts_tick()):
            return super(GetLastFilesTestCase, self).upload_file(*args, **kwargs)

    def create_and_invite(self, owner_uid):
        self.json_ok('user_init', {'uid': owner_uid})
        shared_folder = '/disk/shared_%s' % owner_uid
        self.json_ok('mkdir', {'uid': owner_uid, 'path': shared_folder})

        gid = self.create_group(uid=owner_uid, path=shared_folder)
        hsh = self.invite_user(uid=self.uid, owner=owner_uid, path=shared_folder, email=self.email, ext_gid=gid)
        self.activate_invite(uid=self.uid, hash=hsh)
        for i in xrange(2):
            file_path = '%s/test_%s_%i.txt' % (shared_folder, owner_uid, i)
            self.upload_file(self.uid, file_path)
        return gid

    def test_get_last_files(self):
        """Протестировать количество и порядок возвращаемых файлов ручки get_last_files"""
        # заливаем файлы uid_1 в ОП
        self.create_and_invite(self.uid_1)
        # заливаем файлы uid к себе в диск
        for file_path in ['/disk/test_%s_%i.txt' % (self.uid, i) for i in xrange(2)]:
            self.upload_file(self.uid, file_path)
        # заливаем файлы uid_3 в ОП
        self.create_and_invite(self.uid_3)

        last_files = get_last_files(self.uid)
        assert len(last_files) == 6
        for i, j in zip(last_files, last_files[1:]):
            assert i.mtime >= j.mtime

        # в обратном порядке как заливали
        # залито 6 файлов
        assert any([self.uid_3 in f.address.name for f in last_files[:2]])
        assert any([self.uid in f.address.name for f in last_files[2:4]])
        assert any([self.uid_1 in f.address.name for f in last_files[4:6]])

        # заливаем ещё 2 файла: 1 себе, 1 в ОП
        self.upload_file(self.uid, '/disk/common.txt')
        self.upload_file(self.uid, '/disk/shared_%s/shared.txt' % self.uid_1)
        last_files = get_last_files(self.uid, amount=2)
        assert len(last_files) == 2
        assert last_files[0].address.path == '/disk/shared_%s/shared.txt' % self.uid_1
        assert last_files[1].address.path == '/disk/common.txt'

        last_files = get_last_files(self.uid, amount=10)
        assert len(last_files) == 6 + 2

        # удаляем ОП с 3 файлами
        self.json_ok('rm', {'uid': self.uid, 'path': '/disk/shared_%s' % self.uid_1})
        last_files = get_last_files(self.uid, amount=10)
        assert len(last_files) == 4 + 1

    def test_get_over_limit_items(self):
        for file_path in ['/disk/test_%s_%i.txt' % (self.uid, i) for i in xrange(2)]:
            self.upload_file(self.uid, file_path)

        self.json_ok('new_get_last_files', {'uid': self.uid, 'amount': LAST_FILES_ITEMS_LIMIT})
        self.json_ok('new_get_last_files', {'uid': self.uid, 'offset': LAST_FILES_ITEMS_LIMIT - 2, 'amount': 2})
        self.json_error('new_get_last_files', {'uid': self.uid, 'amount': LAST_FILES_ITEMS_LIMIT + 1})

    def test_meta(self):
        for file_path in ['/disk/test_%s_%i.docx' % (self.uid, i) for i in xrange(2)]:
            self.upload_file(self.uid, file_path)

        for meta_fields in ('', 'media_type,mediatype,size,visible,office_online_url'):
            resp = self.json_ok('new_get_last_files', {'uid': self.uid, 'meta': meta_fields})
            for item in resp:
                assert 'media_type' in item['meta']
                assert 'mediatype' in item['meta']
                assert 'size' in item['meta']
                assert 'visible' in item['meta']
                assert 'office_online_url' in item['meta']

    def test_preview_sizes(self):
        self.upload_file(self.uid, '/disk/test.jpg')

        resp = self.json_ok('new_get_last_files', {'uid': self.uid, 'meta': 'sizes'})
        assert len(resp) == 1
        file = resp[0]
        preview_sizes = set([preview['name'] for preview in file['meta']['sizes']])
        assert preview_sizes == {'DEFAULT', 'ORIGINAL', 'XXXS', 'XXS', 'XS', 'S', 'M', 'L', 'XL', 'XXL', 'XXXL'}

    def test_last_files_for_photounlim(self):
        self.json_ok('enable_unlimited_autouploading', {'uid': self.uid})

        correct_order = ['%i.jpg' % i for i in range(4)]
        disk_files = ['/disk/%s' % i for i in correct_order[::2]]
        photo_files = ['/photounlim/%s' % i for i in correct_order[1::2]]

        for i in zip(disk_files, photo_files):
            for j in i:
                self.upload_file(self.uid, j)

        res = self.json_ok('new_get_last_files', {'uid': self.uid, 'amount': LAST_FILES_ITEMS_LIMIT})
        assert len(res) == 4
        assert list(reversed([i['name'] for i in res])) == correct_order


class CacheUpdateHooksTestCase(CommonSharingMethods):
    """Проверяем постановку таска обновления кеша при разных операциях с FS"""
    def setup_method(self, method):
        super(CacheUpdateHooksTestCase, self).setup_method(method)
        self.json_ok('user_init', {'uid': self.uid_1})

    @staticmethod
    def task_put_hook():
        return patch.object(SharedLastFilesProcessor, 'update_for_group_async')

    @staticmethod
    def extract_gids_from_mock(put_hook):
        return [i[0][0] for i in put_hook.call_args_list]

    def prepare_group(self):
        owner_uid = self.uid_1
        shared_folder = '/disk/shared_%s' % owner_uid
        self.json_ok('mkdir', {'uid': owner_uid, 'path': shared_folder})
        gid = self.create_group(uid=owner_uid, path=shared_folder)
        hsh = self.invite_user(uid=self.uid, owner=owner_uid, path=shared_folder, email=self.email, ext_gid=gid)
        self.activate_invite(uid=self.uid, hash=hsh)
        return {
            'owner_addr': Address.Make(owner_uid, shared_folder),
            'invited_addr': Address.Make(self.uid, shared_folder),
            'gid': gid,
        }

    def test_join_to_group(self):
        with self.task_put_hook() as put_hook:
            group_info = self.prepare_group()
            gids = self.extract_gids_from_mock(put_hook)
            assert gids == [group_info['gid']]

    def test_kick_from_group(self):
        group_info = self.prepare_group()
        with self.task_put_hook() as put_hook:
            self.json_ok('share_kick_from_group', {'gid': group_info['gid'], 'uid': self.uid_1, 'user_uid': self.uid})
            gids = self.extract_gids_from_mock(put_hook)
            assert gids == [group_info['gid']]

    @parameterized.expand(['owner_addr', 'invited_addr'])
    def test_remove_root_folder(self, addr_name):
        group_info = self.prepare_group()
        addr = group_info[addr_name]
        with self.task_put_hook() as put_hook:
            self.json_ok('rm', {'uid': addr.uid, 'path': addr.path})
            gids = self.extract_gids_from_mock(put_hook)
            assert gids == [group_info['gid']]

    @parameterized.expand(['owner_addr', 'invited_addr'])
    def test_upload_file(self, addr_name):
        group_info = self.prepare_group()
        addr = group_info[addr_name]
        with self.task_put_hook() as put_hook:
            self.upload_file(addr.uid, '%s/%s' % (addr.path, '1.jpg'))
            gids = self.extract_gids_from_mock(put_hook)
            assert gids == [group_info['gid']]

    @parameterized.expand(['owner_addr', 'invited_addr'])
    def test_mkdir(self, addr_name):
        group_info = self.prepare_group()
        addr = group_info[addr_name]
        with self.task_put_hook() as put_hook:
            self.json_ok('mkdir', {'uid': addr.uid, 'path': '%s/%s' % (addr.path, 'folder')})
            gids = self.extract_gids_from_mock(put_hook)
            assert gids == []

    @parameterized.expand(['owner_addr', 'invited_addr'])
    def test_rm_file(self, addr_name):
        group_info = self.prepare_group()
        addr = group_info[addr_name]
        resource_path = '%s/%s' % (addr.path, '1.jpg')
        self.upload_file(addr.uid, resource_path)
        with self.task_put_hook() as put_hook:
            self.json_ok('rm', {'uid': addr.uid, 'path': resource_path})
            gids = self.extract_gids_from_mock(put_hook)
            assert gids == [group_info['gid']]

    @parameterized.expand(['owner_addr', 'invited_addr'])
    def test_trash_append_file(self, addr_name):
        group_info = self.prepare_group()
        addr = group_info[addr_name]
        resource_path = '%s/%s' % (addr.path, '1.jpg')
        self.upload_file(addr.uid, resource_path)
        with self.task_put_hook() as put_hook:
            self.json_ok('trash_append', {'uid': addr.uid, 'path': resource_path})
            gids = self.extract_gids_from_mock(put_hook)
            assert gids == [group_info['gid']]

    @parameterized.expand(['owner_addr', 'invited_addr'])
    def test_move_file(self, addr_name):
        group_info = self.prepare_group()
        addr = group_info[addr_name]
        resource_path = '%s/%s' % (addr.path, '1.jpg')
        self.upload_file(addr.uid, resource_path)
        with self.task_put_hook() as put_hook:
            self.json_ok('move', {'uid': addr.uid, 'src': resource_path, 'dst': resource_path + '.bak'})
            gids = self.extract_gids_from_mock(put_hook)
            # *2 из-за создания ресурса и последующего удаления
            assert gids == [group_info['gid']] * 2

    @parameterized.expand(['owner_addr', 'invited_addr'])
    def test_copy_file(self, addr_name):
        group_info = self.prepare_group()
        addr = group_info[addr_name]
        resource_path = '%s/%s' % (addr.path, '1.jpg')
        self.upload_file(addr.uid, resource_path)
        with self.task_put_hook() as put_hook:
            self.json_ok('copy', {'uid': addr.uid, 'src': resource_path, 'dst': resource_path + '.bak'})
            gids = self.extract_gids_from_mock(put_hook)
            assert gids == [group_info['gid']]

    @parameterized.expand(['owner_addr', 'invited_addr'])
    def test_copy_folder_with_file(self, addr_name):
        group_info = self.prepare_group()
        addr = group_info[addr_name]
        folder_path = '%s/%s' % (addr.path, 'folder')
        self.json_ok('mkdir', {'uid': addr.uid, 'path': folder_path})
        for i in range(3):
            resource_path = '%s/%s' % (folder_path, '%i.jpg' % i)
            self.upload_file(addr.uid, resource_path)
        with self.task_put_hook() as put_hook:
            self.json_ok('copy', {'uid': addr.uid, 'src': folder_path, 'dst': folder_path + '.bak'})
            gids = self.extract_gids_from_mock(put_hook)
            assert gids == [group_info['gid']] * 3

    @parameterized.expand(['owner_addr', 'invited_addr'])
    def test_move_folder_with_file(self, addr_name):
        group_info = self.prepare_group()
        addr = group_info[addr_name]
        folder_path = '%s/%s' % (addr.path, 'folder')
        self.json_ok('mkdir', {'uid': addr.uid, 'path': folder_path})
        for i in range(3):
            resource_path = '%s/%s' % (folder_path, '%i.jpg' % i)
            self.upload_file(addr.uid, resource_path)
        with self.task_put_hook() as put_hook:
            self.json_ok('move', {'uid': addr.uid, 'src': folder_path, 'dst': folder_path + '.bak'})
            gids = self.extract_gids_from_mock(put_hook)
            assert gids == [group_info['gid']] * 3
