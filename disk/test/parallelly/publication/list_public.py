# -*- coding: utf-8 -*-

from test.base import DiskTestCase

from mpfs.core.factory import get_resource
from mpfs.core.address import SymlinkAddress
from mpfs.core.filesystem.symlinks import Symlink


class ListPublicTestCase(DiskTestCase):
    """Набор тестов для ручки `list_public`."""
    endpoint = 'list_public'

    def _create_public_file_and_public_folder(self):
        public_file = '/disk/public.jpg'
        public_folder = '/disk/public'
        self.upload_file(self.uid, public_file)
        self.json_ok('mkdir', {'uid': self.uid, 'path': public_folder})
        self.json_ok('set_public', {'uid': self.uid, 'path': public_file})
        self.json_ok('set_public', {'uid': self.uid, 'path': public_folder})
        return public_file, public_folder

    def test_list_public_returns_only_public_resources(self):
        """Проверить, что возвращаются только публичные ресурсы."""
        public_paths = self._create_public_file_and_public_folder()
        non_public_file = '/disk/non_public.jpg'
        non_public_folder = '/disk/non_public'
        self.upload_file(self.uid, non_public_file)
        self.json_ok('mkdir', {'uid': self.uid, 'path': non_public_folder})
        result = self.json_ok(self.endpoint, {'uid': self.uid, 'path': '/disk'})
        assert len(result) == 2
        paths = [r['path'] for r in result]
        assert sorted(paths) == sorted(public_paths)

    def test_list_public_with_empty_meta_returns_public_time(self):
        """Протестировать, что при передаче пустого GET-параметра `meta` будет возвращена дата публикации ресурса."""
        self._create_public_file_and_public_folder()
        result = self.json_ok('list_public', {'uid': self.uid, 'path': '/disk', 'meta': ''})
        for r in result:
            assert 'public_time' in r['meta']

    def test_list_public_with_public_time_in_meta_returns_public_time(self):
        """Протестировать, что при передаче GET-параметра `meta` со значением в списке `public_time`
        будет возвращена дата публикации ресурса."""
        self._create_public_file_and_public_folder()
        result = self.json_ok(self.endpoint, {'uid': self.uid, 'path': '/disk', 'meta': 'public_time'})
        for r in result:
            assert 'public_time' in r['meta']

    def test_list_public_without_meta_returns_resources_without_meta(self):
        """Протестировать, что не передавая GET-параметр `meta` в данных для каждого ресурса
        будет отсутствовать ключ `meta`."""
        self._create_public_file_and_public_folder()
        result = self.json_ok(self.endpoint, {'uid': self.uid, 'path': '/disk'})
        for r in result:
            assert 'meta' not in r

    def test_list_public_meta_public_time_equals_to_symlink_ctime(self):
        """Протестировать, что дата публикации равна времени создания симлинка."""
        self._create_public_file_and_public_folder()
        result = self.json_ok(self.endpoint, {'uid': self.uid, 'path': '/disk', 'meta': ''})
        for r in result:
            path = r['path']
            resource = get_resource(uid=self.uid, address=path)
            assert 'symlink' in resource.meta
            [symlink] = Symlink.find_multiple([SymlinkAddress(resource.meta['symlink'])])
            assert r['meta']['public_time'] == int(symlink.ctime)
