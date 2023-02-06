# -*- coding: utf-8 -*-

from copy import deepcopy
from mock import patch

from test.parallelly.publication.base import BasePublicationMethods  # цикл импорт
from mpfs.core.user.base import StandartUser, User

from test.base import DiskTestCase


class IsAdvertisingEnabledTestCase(DiskTestCase):
    @patch.object(StandartUser, 'is_b2b', return_value=True)
    def test_b2b_advertising_disabled(self, *_):
        assert not User(self.uid).is_advertising_enabled()

    @patch.object(StandartUser, 'is_paid', return_value=True)
    def test_paid_advertising_disabled(self, *_):
        assert not User(self.uid).is_advertising_enabled()

    @patch.object(StandartUser, 'is_paid', return_value=False)
    @patch.object(StandartUser, 'is_b2b', return_value=False)
    def test_domains_advertising_disabled(self, *_):
        for tld in ('com', 'com.tr'):
            assert not User(self.uid).is_advertising_enabled(tld)

    @patch.object(StandartUser, 'is_paid', return_value=False)
    @patch.object(StandartUser, 'is_b2b', return_value=False)
    def test_advertising_enabled(self, *_):
        assert User(self.uid).is_advertising_enabled()


class AdvertisingInUserInfoTestCase(DiskTestCase):
    def test_user_info_advertising_enabled(self):
        response = self.json_ok('user_info', {'uid': self.uid})
        assert 'advertising_enabled' in response
        assert response['advertising_enabled'] == 1

    def test_user_info_forbidden_tld_advertising_enabled(self):
        response = self.json_ok('user_info', {'uid': self.uid, 'tld': 'com'})
        assert 'advertising_enabled' in response
        assert response['advertising_enabled'] == 0


class AdvertisingInPublicInfoTestCase(BasePublicationMethods):
    def test_public_info(self):
        self.make_file(is_public=True)
        info = self.json_ok('info', {'uid': self.uid, 'path': self.pub_file, 'meta': 'public_hash'})
        public_hash = info['meta']['public_hash']
        public_info = self.json_ok('public_info', {'private_hash': public_hash})
        user_info = public_info['user']

        assert 'advertising_enabled' in user_info
        assert user_info['advertising_enabled'] == 1

    def test_public_info_forbidden_tld_advertising_disabled(self):
        self.make_file(is_public=True)
        info = self.json_ok('info', {'uid': self.uid, 'path': self.pub_file, 'meta': 'public_hash'})
        public_hash = info['meta']['public_hash']
        public_info = self.json_ok('public_info', {'private_hash': public_hash, 'tld': 'com.tr'})
        user_info = public_info['user']

        assert 'advertising_enabled' in user_info
        assert user_info['advertising_enabled'] == 0


class AdvertisingInAlbumsTestCase(DiskTestCase):
    ALBUM_CREATE_JSON = {
        'title': 'MyAlbum',
        'description': 'My Test Album',
        'layout': 'rows',
        'flags': ['show_frames'],
        'items': [
            {'type': 'resource', 'path': '/disk/my_file1.jpg'},
            {'type': 'resource', 'path': '/disk/my_file2.jpg'}
        ],
    }

    def test_album_get(self):
        album_dict = deepcopy(self.ALBUM_CREATE_JSON)
        album = self.json_ok('albums_create_with_items', opts={'uid': self.uid}, json=album_dict)
        album_id = album['id']
        public_key = album['public']['public_key']

        album = self.json_ok('album_get', opts={'uid': self.uid, 'album_id': album_id})
        assert album['user']['advertising_enabled'] == 1

        album = self.json_ok('public_album_get', opts={'public_key': public_key})
        assert album['user']['advertising_enabled'] == 1

        album = self.json_ok('album_get', opts={'uid': self.uid, 'album_id': album_id})
        assert album['user']['advertising_enabled'] == 1

    def test_album_get_forbidden_tld_advertising_disabled(self):
        album_dict = deepcopy(self.ALBUM_CREATE_JSON)
        album = self.json_ok('albums_create_with_items', opts={'uid': self.uid}, json=album_dict)
        album_id = album['id']
        public_key = album['public']['public_key']

        album = self.json_ok('album_get', opts={'uid': self.uid, 'album_id': album_id, 'tld': 'com'})
        assert album['user']['advertising_enabled'] == 0

        album = self.json_ok('public_album_get', opts={'public_key': public_key, 'tld': 'com'})
        assert album['user']['advertising_enabled'] == 0

        album = self.json_ok('album_get', opts={'uid': self.uid, 'album_id': album_id, 'tld': 'com'})
        assert album['user']['advertising_enabled'] == 0
