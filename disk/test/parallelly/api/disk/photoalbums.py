# -*- coding: utf-8 -*-
import json
import pytest
import re

from copy import deepcopy

from nose_parameterized import parameterized

from mpfs.core.albums.static import GeneratedAlbumType
from mpfs.metastorage.postgres.schema import AlbumType
from test.parallelly.api.disk.base import DiskApiTestCase
from test.base_suit import UploadFileTestCaseMixin, UserTestCaseMixin
from test.parallelly.social_suit import CommonSocialMethods
from mpfs.common.util import from_json
from mpfs.common.static import tags


class GetPhotoAlbumsTestCase(UserTestCaseMixin, UploadFileTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.EXTERNAL
    api_version = 'v1'
    method = 'GET'
    url = 'disk/photoalbums'

    album_dict = {
        'title': 'MyAlbum',
        'description': 'My Test Album',
        'items': [
            {'type': 'resource', 'path': '/disk/0.jpg'},
            {'type': 'resource', 'path': '/disk/1.jpg'},
            {'type': 'resource', 'path': '/disk/2.jpg'},
        ],
    }

    def setup_method(self, method):
        super(GetPhotoAlbumsTestCase, self).setup_method(method)
        self.create_user(self.uid, noemail=1)
        for i in range(2):
            self.upload_file(self.uid, '/disk/%i.jpg' % i)

        for i in range(3):
            album_dict = deepcopy(self.album_dict)
            album_dict['title'] = "Album %i" % i
            self.album = self.json_ok('albums_create_with_items',
                                      opts={'uid': self.uid}, json=album_dict)

    def test_album_without_cover(self):
        self.json_ok('albums_create', opts={'uid': self.uid})
        with self.specified_client(scopes=['cloud_api:disk.read']):
            resp = self.client.request(self.method, self.url, query={})
            res = json.loads(resp.content)
            assert bool([i for i in res['items'] if 'cover' not in i])
            assert len(res['items']) == 4

    def test_fields(self):
        """Проверка структуры ответа"""
        required_body_fields = {'album_id', 'title', 'created', 'layout', 'public_url',
                                'is_public', 'cover', 'is_empty', 'views_count'}
        required_cover_fields = {'size', 'name', 'created', 'modified', 'path', 'media_type', 'preview', 'type',
                                 'mime_type', 'md5', 'sha256', 'revision', 'resource_id', 'comment_ids', 'exif',
                                 'antivirus_status', 'file'}
        with self.specified_client(scopes=['cloud_api:disk.read']):
            resp = self.client.request(self.method, self.url, query={})
            res = json.loads(resp.content)
            assert not set(['limit', 'offset', 'total', 'items']) ^ res.viewkeys()
            for item in res['items']:
                assert not required_body_fields ^ item.viewkeys()
                assert not required_cover_fields ^ item['cover'].viewkeys()

    def test_amount_offset(self):
        """Проверка пагинации"""
        with self.specified_client(scopes=['cloud_api:disk.read']):
            resp = self.client.request(self.method, self.url, query={})
            res = json.loads(resp.content)
            # загружено 3 альбома, ожидаем 3 альбома в ответе
            assert len(res['items']) == 3
            assert res['total'] == 3

            resp = self.client.request(self.method, self.url, query={'limit': 2})
            res = json.loads(resp.content)
            # запрашиваем 2 альбома, ожидаем 2 альбома в ответе
            assert len(res['items']) == 2
            assert res['total'] == 3

            resp = self.client.request(self.method, self.url, query={'limit': 2, 'offset': 2})
            res = json.loads(resp.content)
            # всего 3 альбома, смещение 2, ожидаем 1 альбом в ответе
            assert len(res['items']) == 1
            assert res['total'] == 3


class PostPhotoAlbumTestCase(UserTestCaseMixin, UploadFileTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.EXTERNAL
    api_version = 'v1'
    method = 'POST'
    url = 'disk/photoalbums'

    post_body = {
        'title': 'MyAlbum',
        'layout': 'rows',
        'is_public': True,
        'cover': 1,
        'items': [
            {'resource': {'path': 'disk:/0.jpg'}},
            {'resource': {'path': 'disk:/1.jpg'}},
        ],
    }

    def setup_method(self, method):
        super(PostPhotoAlbumTestCase, self).setup_method(method)
        self.create_user(self.uid, noemail=1)
        for i in range(2):
            self.upload_file(self.uid, '/disk/%i.jpg' % i)

    def test_fields(self):
        """Проверка структуры ответа"""
        required_body_fields = {'album_id', 'title', 'created', 'layout', 'public_url',
                                'is_public', 'cover', 'is_empty', 'views_count'}
        required_cover_fields = {'size', 'name', 'created', 'modified', 'path', 'sha256', 'revision',
                                 'media_type', 'preview', 'type', 'mime_type', 'md5', 'resource_id',
                                 'comment_ids', 'exif', 'antivirus_status', 'file'}
        with self.specified_client(scopes=['cloud_api:disk.write']):
            resp = self.client.request(self.method, self.url, data=self.post_body)
            res = json.loads(resp.content)
            assert not required_body_fields ^ res.viewkeys()
            assert not required_cover_fields ^ res['cover'].viewkeys()

    def test_empty_body(self):
        """Пустой тело запроса"""
        with self.specified_client(scopes=['cloud_api:disk.write']):
            post_body = self.post_body.copy()
            post_body['items'] = []
            resp = self.client.request(self.method, self.url, data=post_body)
            assert resp.status_code == 400

    def test_public(self):
        """Проверка параметра is_public"""
        with self.specified_client(scopes=['cloud_api:disk.write']):
            resp = self.client.request(self.method, self.url, data=self.post_body)
            res = json.loads(resp.content)
            assert res['is_public']

            post_body = deepcopy(self.post_body)
            post_body['is_public'] = False
            resp = self.client.request(self.method, self.url, data=post_body)
            res = json.loads(resp.content)
            assert not res['is_public']


@pytest.mark.skipif(True, reason='https://st.yandex-team.ru/CHEMODAN-32396')
class SocialPostPhotoAlbumTestCase(UserTestCaseMixin, UploadFileTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.INTERNAL
    api_version = 'v1'
    method = 'POST'
    url = 'disk/photoalbums'
    social_post_url = 'disk/photoalbums/%s/share/facebook'

    album_dict = {
        'title': 'MyAlbum',
        'description': 'My Test Album',
        'items': [
            {'type': 'resource', 'path': '/disk/0.jpg'},
        ],
    }

    def setup_method(self, method):
        self.uid = CommonSocialMethods.test_uid

        super(SocialPostPhotoAlbumTestCase, self).setup_method(method)
        self.create_user(self.uid, noemail=1)
        for i in xrange(len(self.album_dict['items'])):
            self.upload_file(self.uid, '/disk/%i.jpg' % i)
        album = self.json_ok('albums_create_with_items',
                             opts={'uid': self.uid},
                             json=self.album_dict)
        self.album_id = album['id']

    def test_publishing(self):
        resp = self.client.request(self.method, self.social_post_url % self.album_id, uid=self.uid)
        res = json.loads(resp.content)

        assert resp.status_code == 202
        assert 'href' in res

    def test_errors(self):
        # не найден альбом
        unknown_album_id = '1234567890'
        resp = self.client.request(self.method, self.social_post_url % unknown_album_id, uid=self.uid)
        assert resp.status_code == 404

        # у альбома убрали публичный доступ
        self.json_ok('album_unpublish', opts={'uid': self.uid, 'album_id': self.album_id})
        resp = self.client.request(self.method, self.social_post_url % self.album_id, uid=self.uid)
        assert resp.status_code == 409


class PostPhotoAlbumItemTestCase(UserTestCaseMixin, UploadFileTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.EXTERNAL
    api_version = 'v1'
    method = 'POST'
    url = 'disk/photoalbums/%s/items'

    album_dict = {
        'title': 'MyAlbum',
        'description': 'My Test Album',
        'items': [
            {'type': 'resource', 'path': '/disk/0.jpg'},
        ],
    }
    post_body = {'resource': {'path': 'disk:/1.jpg'}}

    def setup_method(self, method):
        super(PostPhotoAlbumItemTestCase, self).setup_method(method)
        self.create_user(self.uid, noemail=1)
        for i in range(2):
            self.upload_file(self.uid, '/disk/%i.jpg' % i)
        album = self.json_ok('albums_create_with_items',
                             opts={'uid': self.uid},
                             json=self.album_dict)
        self.album_id = album['id']

    def test_fields(self):
        """Проверка структуры ответа"""
        required_resource_fields = {'size', 'name', 'created', 'modified', 'path', 'sha256', 'revision',
                                    'media_type', 'preview', 'type', 'mime_type', 'md5', 'resource_id',
                                    'comment_ids', 'exif', 'antivirus_status', 'file'}
        with self.specified_client(scopes=['cloud_api:disk.write']):
            resp = self.client.request(self.method, self.url % self.album_id, data=self.post_body)
            res = json.loads(resp.content)
            assert not set(['album_id', 'item_id', 'resource']) ^ set(res.keys())
            assert not required_resource_fields ^ res['resource'].viewkeys()

    def test_item_append(self):
        """Проверка добавления элемента"""
        with self.specified_client(scopes=['cloud_api:disk.write']):
            resp = self.client.request(self.method, self.url % self.album_id, data=self.post_body)
            res = json.loads(resp.content)
            album_id = res['album_id']
            items = self.json_ok('album_get', {'uid': self.uid, 'album_id': album_id})['items']
            assert len(items) == 2


class PostPhotoAlbumBulkCreateItemsTestCase(UserTestCaseMixin, UploadFileTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.EXTERNAL
    api_version = 'v1'
    method = 'POST'
    url = 'disk/photoalbums/%s/items/bulk-create'

    album_dict = {
        'title': 'MyAlbum',
        'description': 'My Test Album',
        'items': [
            {'type': 'resource', 'path': '/disk/0.jpg'},
        ],
    }
    post_body = {'resource': {'path': 'disk:/1.jpg'}}

    def setup_method(self, method):
        super(PostPhotoAlbumBulkCreateItemsTestCase, self).setup_method(method)
        self.create_user(self.uid, noemail=1)
        for i in range(3):
            self.upload_file(self.uid, '/disk/%i.jpg' % i)
        album = self.json_ok('albums_create_with_items',
                             opts={'uid': self.uid},
                             json=self.album_dict)
        self.album_id = album['id']

    def test_items_num_limit(self):
        post_body = {
            'resources': [
                {'path': 'disk:/1.jpg'},
            ] * 101
        }
        with self.specified_client(scopes=['cloud_api:disk.write']):
            resp = self.client.request(self.method, self.url % self.album_id, data=post_body)
            assert resp.status_code == 400

    def test_bulk_items_append(self):
        post_body = {
            'resources': [
                {'path': 'disk:/1.jpg'},
                {'path': 'disk:/0.jpg'},
            ]
        }
        with self.specified_client(scopes=['cloud_api:disk.write']):
            resp = self.client.request(self.method, self.url % self.album_id, data=post_body)
            assert resp.status_code == 200
            res = json.loads(resp.content)
            assert res['album_id'] == self.album_id
            assert len(res['items']) == 1
            assert res['items'][0]['resource']['path'] == 'disk:/1.jpg'

            post_body = {
                'resources': [
                    {'path': 'disk:/2.jpg'},
                ]
            }
            resp = self.client.request(self.method, self.url % self.album_id, data=post_body)
            assert resp.status_code == 200
            res = json.loads(resp.content)
            assert res['album_id'] == self.album_id
            assert len(res['items']) == 1
            assert res['items'][0]['resource']['path'] == 'disk:/2.jpg'


class DeletePhotoAlbumItemTestCase(UserTestCaseMixin, UploadFileTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.EXTERNAL
    api_version = 'v1'
    pictures = tuple('/disk/{}.jpg'.format(i) for i in xrange(5))
    album_id = None

    def setup_method(self, method):
        super(DeletePhotoAlbumItemTestCase, self).setup_method(method)
        self.create_user(self.uid, noemail=1)

        for picture in self.pictures:
            self.upload_file(self.uid, picture)

        album_create_body = {
            'title': 'MyAlbum',
            'description': 'My Test Album',
            'items': map(
                lambda p: {'type': 'resource', 'path': p},
                self.pictures
            ),
        }

        album = self.json_ok('albums_create_with_items',
                             opts={'uid': self.uid},
                             json=album_create_body)
        self.album_id = album['id']

    def test_remove_item_by_id(self):
        """Проверяем удаляется ли элемент альбома по айди"""
        for picture in self.pictures:

            kwargs = {
                'method': 'album_item_check',
                'opts': {'uid': self.uid, 'path': picture}
            }
            response = self.json_ok(**kwargs)

            ids = {
                'album_id': self.album_id,
                'item_id': response['id']
            }
            url = 'disk/photoalbums/{album_id}/items/{item_id}'.format(**ids)
            with self.specified_client(scopes=['cloud_api:disk.write']):
                response = self.client.request('DELETE', url)
                assert response.status == 204

            response = self.json_error(**kwargs)
            assert response['code'] == 71  # 71 means not found


class GetPhotoAlbumItemsTestCase(UserTestCaseMixin, UploadFileTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.EXTERNAL
    api_version = 'v1'
    method = 'GET'
    url = 'disk/photoalbums/%s/items'
    album_dict = {
        'title': 'MyAlbum',
        'description': 'My Test Album',
        'items': [
            {'type': 'resource', 'path': '/disk/0.jpg'},
            {'type': 'resource', 'path': '/disk/1.jpg'},
            {'type': 'resource', 'path': '/disk/2.jpg'},
        ],
    }

    def setup_method(self, method):
        super(GetPhotoAlbumItemsTestCase, self).setup_method(method)
        self.create_user(self.uid, noemail=1)
        for i in xrange(len(self.album_dict['items'])):
            self.upload_file(self.uid, '/disk/%i.jpg' % i)
        album = self.json_ok('albums_create_with_items',
                             opts={'uid': self.uid},
                             json=self.album_dict)
        self.album_id = album['id']

    def test_fields(self):
        """Проверка структуры ответа"""
        required_body_fields = {
            'items', 'limit', 'last_item_id'
        }
        required_item_fields = {
            'item_id', 'resource', 'album_id'
        }
        required_resource_fields = {
            'size', 'sha256', 'name', 'created', 'modified', 'path', 'media_type',
            'preview', 'type', 'mime_type', 'md5', 'revision', 'resource_id', 'comment_ids', 'exif',
            'antivirus_status', 'file'
        }

        with self.specified_client(scopes=['cloud_api:disk.read']):
            resp = self.client.request(self.method, self.url % self.album_id)
            res = json.loads(resp.content)
            assert not required_body_fields ^ res.viewkeys()
            for i in res['items']:
                assert not required_item_fields ^ i.viewkeys()
                assert not required_resource_fields ^ i['resource'].viewkeys()

    @pytest.mark.skipif(True, reason='https://st.yandex-team.ru/CHEMODAN-32396')
    def test_etag(self):
        """Проверка механизма etag"""
        with self.specified_client(scopes=['cloud_api:disk.read']):
            resp = self.client.request(self.method, self.url % self.album_id)
            assert 'ETag' in resp.headers
            etag = resp.headers['ETag']
            assert etag[0] == etag[-1] == '"'

            resp = self.client.request(self.method,
                                       self.url % self.album_id,
                                       headers={'If-None-Match': '"%s"' % etag})
            assert resp.status == 304
            resp = self.client.request(self.method,
                                       self.url % self.album_id,
                                       headers={'If-None-Match': '"random123"'})
            assert resp.status == 200
            assert 'ETag' in resp.headers
            assert resp.headers['ETag'] == etag

            resp = self.client.request(self.method, self.url % self.album_id, query={'limit': 1})
            assert resp.status == 200
            assert resp.headers['ETag'] != etag

    def test_pagination(self):
        """Проверка пагинации"""
        with self.specified_client(scopes=['cloud_api:disk.read']):
            items = self.json_ok('album_get', {'uid': self.uid, 'album_id': self.album_id})['items']
            item_ids = [i['id'] for i in items]

            # всего 3 элемента альбома
            resp = self.client.request(self.method,
                                       self.url % self.album_id,
                                       query={'last_item_id': item_ids[0]})
            res = json.loads(resp.content)
            assert len(res['items']) == 2

            resp = self.client.request(self.method,
                                       self.url % self.album_id,
                                       query={'last_item_id': item_ids[1]})
            res = json.loads(resp.content)
            assert len(res['items']) == 1

            resp = self.client.request(self.method,
                                       self.url % self.album_id,
                                       query={'last_item_id': item_ids[2]})
            res = json.loads(resp.content)
            assert len(res['items']) == 0

            resp = self.client.request(self.method,
                                       self.url % self.album_id,
                                       query={'last_item_id': item_ids[0], 'limit': 1})
            res = json.loads(resp.content)
            assert len(res['items']) == 1


class GetPhotoAlbumItemTestCase(UserTestCaseMixin, UploadFileTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.EXTERNAL
    api_version = 'v1'
    method = 'GET'
    url = 'disk/photoalbums/%s/items/%s'
    album_dict = {
        'title': 'MyAlbum',
        'description': 'My Test Album',
        'items': [
            {'type': 'resource', 'path': '/disk/0.jpg'},
        ],
    }

    def setup_method(self, method):
        super(GetPhotoAlbumItemTestCase, self).setup_method(method)
        self.create_user(self.uid, noemail=1)
        self.upload_file(self.uid, '/disk/0.jpg')
        album = self.json_ok('albums_create_with_items',
                             opts={'uid': self.uid},
                             json=self.album_dict)
        self.album_id = album['id']
        self.album_item_id = album['items'][0]['id']

    def test_fields(self):
        """Проверка структуры ответа"""
        required_cover_fields = {
            'preview', 'name', 'created', 'modified',
            'media_type', 'path', 'md5', 'type', 'sha256',
            'mime_type', 'size', 'revision', 'resource_id', 'comment_ids', 'exif',
            'antivirus_status', 'file'
        }
        with self.specified_client(scopes=['cloud_api:disk.read']):
            resp = self.client.request(self.method, self.url % (self.album_id, self.album_item_id))
            res = json.loads(resp.content)
            assert not set(['album_id', 'item_id', 'resource']) ^ res.viewkeys()
            assert not required_cover_fields ^ res['resource'].viewkeys()


class GetPhotoAlbumTestCase(UserTestCaseMixin, UploadFileTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.EXTERNAL
    api_version = 'v1'
    method = 'GET'
    url = 'disk/photoalbums/{album_id}/'
    required_album_id = None

    def setup_method(self, method):
        super(GetPhotoAlbumTestCase, self).setup_method(method)

        self.create_user(self.uid, noemail=1)
        self.upload_file(self.uid, '/disk/0.jpg')

        json_data = {
            'title': 'MyAlbum',
            'description': 'My Album',
            'items': [
                {'type': 'resource', 'path': '/disk/0.jpg'}
            ],
        }
        for i in xrange(5):
            json_data['title'] = 'MyAlbum #{}'.format(i)
            response = self.json_ok(method='albums_create_with_items',
                                    opts={'uid': self.uid},
                                    json=json_data)
            self.required_album_id = response['id']

    def test_returned_required_album_id(self):
        """Проверяем, что в ответ приходит альбом с нужным Id"""

        with self.specified_client(scopes=['cloud_api:disk.read']):
            url = self.url.format(album_id=self.required_album_id)
            response = self.client.request(self.method, url)
            assert response.status == 200

            body = json.loads(response.content)
            assert body['album_id'] == self.required_album_id

    @pytest.mark.skipif(True, reason='https://st.yandex-team.ru/CHEMODAN-32396')
    def test_required_fields_in_response(self):
        """Проверяем, что в ответе присутствуют все необходимые поля

        Список необходимых полей был взят из схемы:
        https://github.yandex-team.ru/akinfold/cloud-api-schema/blob/master/cloud-api/disk/photoalbums/photoalbums.raml#L50-L64
        """

        required_body_fields = {
            'album_id', 'title', 'created', 'layout',
            'public_url', 'is_public', 'cover', 'is_empty', 'views_count'
        }
        required_cover_fields = {
            'preview', 'name', 'created', 'modified',
            'media_type', 'path', 'md5', 'type', 'sha256',
            'mime_type', 'size', 'revision'
        }
        with self.specified_client(scopes=['cloud_api:disk.read']):
            url = self.url.format(album_id=self.required_album_id)
            response = self.client.request(self.method, url)
            assert response.status == 200

            body = json.loads(response.content)
            assert not required_body_fields ^ body.viewkeys()
            assert not required_cover_fields ^ body['cover'].viewkeys()


class ChangePhotoAlbumTestCase(UserTestCaseMixin, UploadFileTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.EXTERNAL
    api_version = 'v1'
    url = 'disk/photoalbums/{album_id}/'
    required_album_id = None
    new_attributes = [
        ('title', 'Changed title'),
        ('layout', 'waterfall'),
        ('is_public', False),
        ('cover', 3)
    ]

    def setup_method(self, method):
        super(ChangePhotoAlbumTestCase, self).setup_method(method)
        self.create_user(self.uid, noemail=1)

        pictures = tuple('/disk/{}.jpg'.format(i) for i in xrange(5))
        for picture in pictures:
            self.upload_file(self.uid, picture)

        album = {
            'title': 'MyAlbum',
            'description': 'My Test Album',
            'items': map(
                lambda p: {'type': 'resource', 'path': p},
                pictures
            ),
        }
        album = self.json_ok('albums_create_with_items', opts={'uid': self.uid}, json=album)
        self.required_album_id = album['id']

    def test_cyrillic_attributes(self):
        url = self.url.format(album_id=self.required_album_id)
        with self.specified_client(scopes=['cloud_api:disk.read', 'cloud_api:disk.write']):
            resp = self.client.request('PATCH', url, data={'title': u'джигурда'})
            assert resp.status_code == 200
            assert json.loads(resp.content)['title'] == u'джигурда'
            resp = self.client.request('GET', url)
            assert resp.status_code == 200
            assert json.loads(resp.content)['title'] == u'джигурда'

    def test_attributes_subsequently_changed(self):
        """Проверяем что каждый из атрибутов по отдельности изменяется"""

        url = self.url.format(album_id=self.required_album_id)

        with self.specified_client(scopes=['cloud_api:disk.read']):
            response = self.client.request('GET', url)
            assert response.status == 200

            old_body = json.loads(response.content)
            assert old_body['album_id'] == self.required_album_id

        with self.specified_client(scopes=['cloud_api:disk.write']):

            for attribute, pivot_value in self.new_attributes:

                response = self.client.request('PATCH', url, data={attribute: pivot_value})
                assert response.status == 200

                new_body = json.loads(response.content)
                assert new_body['album_id'] == self.required_album_id

                old_value = old_body[attribute]
                new_value = new_body[attribute]

                if attribute == 'cover':
                    old_value = old_body['cover']['path']
                    new_value = new_body['cover']['path']
                    pivot_value = 'disk:/{}.jpg'.format(pivot_value)

                assert old_value != new_value == pivot_value

    def test_attributes_changed_at_once(self):
        """Проверяем что все атрибуты изменились за один запрос"""

        url = self.url.format(album_id=self.required_album_id)

        with self.specified_client(scopes=['cloud_api:disk.read']):
            response = self.client.request('GET', url)
            assert response.status == 200

            old_body = json.loads(response.content)
            assert old_body['album_id'] == self.required_album_id

        with self.specified_client(scopes=['cloud_api:disk.write']):

            response = self.client.request('PATCH', url, data=dict(self.new_attributes))
            assert response.status == 200

            new_body = json.loads(response.content)
            assert new_body['album_id'] == self.required_album_id

            for attribute, pivot_value in self.new_attributes:

                old_value = old_body[attribute]
                new_value = new_body[attribute]

                if attribute == 'cover':
                    old_value = old_body['cover']['path']
                    new_value = new_body['cover']['path']
                    pivot_value = 'disk:/{}.jpg'.format(pivot_value)

                assert old_value != new_value == pivot_value

    def test_publish_private(self):
        """Проверяем публикацию приватного альбома, важно не потерять публичные ссылки"""
        url = self.url.format(album_id=self.required_album_id)
        with self.specified_client(scopes=['cloud_api:disk.write']):
            resp = self.client.request('POST', 'disk/photoalbums',
                                       query={'uid': self.uid},
                                       data={'is_public': False})
            assert resp.status_code == 200
            old_body = json.loads(resp.content)
            assert not old_body['is_public']
            assert old_body['public_url']
            assert old_body['album_id']

            response = self.client.request('PATCH', url, data={'is_public': True})
            new_body = json.loads(response.content)
            assert old_body['is_public'] != new_body['is_public']

    def test_publish_unpublished(self):
        """Проверяем публикацию приватного альбома"""

        url = self.url.format(album_id=self.required_album_id)

        with self.specified_client(scopes=['cloud_api:disk.read']):
            response = self.client.request('GET', url)
            assert response.status == 200

            old_body = json.loads(response.content)
            assert old_body['album_id'] == self.required_album_id

        with self.specified_client(scopes=['cloud_api:disk.write']):
            response = self.client.request('PATCH', url, data={'is_public': False})

            new_body = json.loads(response.content)
            assert old_body['is_public'] != new_body['is_public']
            assert 'preview' in new_body['cover']
            old_body = new_body.copy()

            response = self.client.request('PATCH', url, data={'is_public': True})

            new_body = json.loads(response.content)
            assert old_body['is_public'] != new_body['is_public']
            assert 'preview' in new_body['cover']

    def test_required_fields_in_response(self):
        """Проверяем, что в ответе присутствуют все необходимые поля
        """

        required_body_fields = {
            'album_id', 'title', 'created', 'layout',
            'public_url', 'is_public', 'cover', 'is_empty', 'views_count'
        }
        required_cover_fields = {
            'preview', 'name', 'created', 'modified',
            'media_type', 'path', 'md5', 'type', 'sha256',
            'mime_type', 'size', 'revision', 'resource_id', 'comment_ids',
            'exif', 'antivirus_status', 'file'
        }
        with self.specified_client(scopes=['cloud_api:disk.write']):
            url = self.url.format(album_id=self.required_album_id)
            response = self.client.request('PATCH', url, data={'is_public': False})
            assert response.status == 200

            body = json.loads(response.content)
            assert not required_body_fields ^ body.viewkeys()
            assert not required_cover_fields ^ body['cover'].viewkeys()


class DeletePhotoAlbumTestCase(UserTestCaseMixin, UploadFileTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.EXTERNAL
    api_version = 'v1'
    url = 'disk/photoalbums/{album_id}/'
    required_album_id = None

    def setup_method(self, method):
        super(DeletePhotoAlbumTestCase, self).setup_method(method)

        self.create_user(self.uid, noemail=1)
        self.upload_file(self.uid, '/disk/0.jpg')

        json_data = {
            'title': 'MyAlbum',
            'description': 'My Album',
            'items': [
                {'type': 'resource', 'path': '/disk/0.jpg'}
            ],
        }
        response = self.json_ok(method='albums_create_with_items',
                                opts={'uid': self.uid},
                                json=json_data)
        self.required_album_id = response['id']

    def test_returned_required_album_id(self):
        """Проверяем, что альбом удаляется"""

        url = self.url.format(album_id=self.required_album_id)

        with self.specified_client(scopes=['cloud_api:disk.write']):
            response = self.client.request('DELETE', url)
            assert response.status == 204

        with self.specified_client(scopes=['cloud_api:disk.read']):
            response = self.client.request('GET', url)
            assert response.status == 404


class IsEmptyPhotoAlbumTestCase(UserTestCaseMixin, UploadFileTestCaseMixin, DiskApiTestCase):
    """ Проверяем поведение флага is_empty для альбомов
    """
    api_mode = tags.platform.EXTERNAL
    api_version = 'v1'
    method = 'GET'
    url = 'disk/photoalbums'
    body = {
        'title': 'MyAlbum',
        'description': 'My Test Album',
        'items': [
            {'type': 'resource', 'path': '/disk/0.jpg'},
        ],
    }

    def setup_method(self, method):
        super(IsEmptyPhotoAlbumTestCase, self).setup_method(method)
        self.create_user(self.uid, noemail=1)
        for item in self.body['items']:
            self.upload_file(self.uid, item['path'])

    def test_not_empty_has_cover(self):
        """ Альбом не пустой если есть обложка
        """
        album = self.json_ok('albums_create_with_items', opts={'uid': self.uid}, json=self.body)
        album_id = album['id']
        with self.specified_client(scopes=['cloud_api:disk.read']):
            resp = self.client.request('GET', 'disk/photoalbums/{}'.format(album_id), query={})
            album = json.loads(resp.content)
            assert album['cover']
            assert album['is_empty'] is False

    def test_empty_has_no_cover(self):
        """ Альбом пустой если нет обложки
        """
        album = self.json_ok('albums_create', opts={'uid': self.uid})
        album_id = album['id']
        with self.specified_client(scopes=['cloud_api:disk.read']):
            resp = self.client.request('GET', 'disk/photoalbums/{}'.format(album_id), query={})

            album = json.loads(resp.content)
            assert 'cover' not in album
            assert album['is_empty'] is True

    def test_becomes_empty(self):
        """ Альбом становится пустым после удаления файла обложки
        """
        album = self.json_ok('albums_create_with_items', opts={'uid': self.uid}, json=self.body)
        album_id = album['id']

        self.remove_uploaded_files()

        with self.specified_client(scopes=['cloud_api:disk.read']):
            resp = self.client.request('GET', 'disk/photoalbums/{}'.format(album_id), query={})
            album = json.loads(resp.content)
            assert 'cover' not in album
            assert album['is_empty'] is True


class ExcludFromGeneratedAlbumsTestCase(UserTestCaseMixin, UploadFileTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.INTERNAL
    api_version = 'v1'
    method = 'POST'
    url = 'disk/resources/exclude-from-generated-album'

    def setup_method(self, method):
        super(ExcludFromGeneratedAlbumsTestCase, self).setup_method(method)
        self.create_user(self.uid, noemail=1)
        mpfs_path = '/disk/enot.JPG'
        self.path = 'disk:/enot.JPG'
        self.upload_file(self.uid, mpfs_path)

    def test_exclude_from_generated_albums(self):
        with self.specified_client(scopes=['cloud_api:disk.write', 'cloud_api:disk.read']):
            resp = self.client.request(self.method, self.url,
                                       query={'path': self.path,
                                              'album_type': GeneratedAlbumType.BEAUTIFUL.value})
            result = json.loads(resp.content)

            resp = self.client.get('disk/resources',
                                   query={'path': self.path})
            info_result = json.loads(resp.content)

        assert GeneratedAlbumType.BEAUTIFUL.value in result['items']


class FavoritesAlbumsTestCase(UserTestCaseMixin, UploadFileTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.INTERNAL
    api_version = 'v1'
    method = 'POST'
    url = 'disk/photoalbums'

    body = {'items': [{'type': 'resource', 'path': '/disk/0.jpg'}, ], }

    def setup_method(self, method):
        super(FavoritesAlbumsTestCase, self).setup_method(method)
        self.create_user(self.uid, noemail=1)
        for item in self.body['items']:
            self.upload_file(self.uid, item['path'])

    def _create_favorite_album(self):
        resp_create = self.client.request(self.method, self.url, query={'uid': self.uid,
                                                                        'album_type': AlbumType.FAVORITES.value})
        assert resp_create.status_code == 200
        return json.loads(resp_create.content)

    def test_favorites_album_create(self):
        """ Создать альбом избранное """
        album = self._create_favorite_album()
        # Альбом избранное всегда приватный
        assert not album['is_public']
        assert album['album_id']

    def test_favorites_album_is_singlton(self):
        """ Проверка что при повторном создании избранного получаем ошибку """
        resp_first_album_id = self._create_favorite_album()['album_id']
        resp_second = self.client.request(self.method, self.url, query={'uid': self.uid,
                                                                        'album_type': AlbumType.FAVORITES.value})
        assert resp_second.status_code == 409
        assert u'Album already exists. album_id: ' in resp_second.content
        resp_second_album_id = resp_second.content.split(u'Album already exists. album_id: ')[1].split('.')[0]
        assert resp_second_album_id == resp_first_album_id

    @parameterized.expand([
        ('delete', 'DELETE', None),
        ('public_url', 'PATCH', {'is_public': True}),
    ])
    def test_forbidden(self, test_name, method, data):
        with self.specified_client(scopes=['cloud_api:disk.write']):
            album_id = self._create_favorite_album()['album_id']

            url = 'disk/photoalbums/{album_id}/'.format(album_id=album_id)
            resp = self.client.request(method, url, data=data)
            assert resp.status_code == 403

    @parameterized.expand([
        ('rename', 'PATCH', {'title': u'My amazing fail'}),
    ])
    def test_ignored(self, test_name, method, data):
        with self.specified_client(scopes=['cloud_api:disk.write']):
            album_id = self._create_favorite_album()['album_id']

            url = 'disk/photoalbums/{album_id}/'.format(album_id=album_id)
            resp_not_changed = self.client.request(method, url, data=data)
            assert resp_not_changed.status_code == 200

            resp = self.client.request('GET', url)
            assert resp.status_code == 200
            album = json.loads(resp.content)
            assert album['title'] != data['title']

