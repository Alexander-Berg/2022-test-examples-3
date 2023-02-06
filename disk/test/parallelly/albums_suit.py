# -*- coding: utf-8 -*-
import json
import random
import urllib
import time
import base64
import os
import pytest

from copy import deepcopy

import mock
from hamcrest import assert_that, is_not, has_item
from hamcrest import contains_inanyorder
from nose_parameterized import parameterized
from urlparse import urlparse, parse_qs
from bson import ObjectId

from mpfs.common.errors import ZeroUpdateControllerMPFSError, KladunNoResponse
from mpfs.common.static import codes
from mpfs.common.util import from_json, to_json
from mpfs.core.user.base import User
from mpfs.dao.session import Session
from mpfs.metastorage.postgres.schema import AlbumType

from test.base import DiskTestCase
from test.base_suit import SharingTestCaseMixin, patch_http_client_open_url
from test.conftest import INIT_USER_IN_POSTGRES
from test.fixtures.users import turkish_user, user_1
from mpfs.core.services.previewer_service import Previewer
from mpfs.core.services.search_service import SearchDB
from mpfs.core.albums.dao.album_items import AlbumItemDAO
from mpfs.core.albums.dao.albums import AlbumDAO
from mpfs.core.albums.logic.common import get_album_item_face_coordinates, borders_is_valid
from mpfs.core.albums.models import Album, AlbumItem
from mpfs.metastorage.mongo.util import decompress_data, compress_data
from mpfs.metastorage.mongo.collections.filesystem import UserDataCollection
from test.base import parse_open_url_call
from test.helpers.stubs.resources.users_info import update_info_by_uid, DEFAULT_USERS_INFO
from test.helpers.stubs.services import PushServicesStub, VideoStreamingStub, KladunStub, PassportStub


class AlbumModelsTestCase(DiskTestCase):
    def test_save(self):
        # проверяем, что запись добавляется в БД
        a = Album()
        a.uid = self.uid
        a.title = 'MyAlbum'
        a.save()
        assert a.id is not None

        # проверяем, что запись вместо того чтобы добавиться, обновляется в БД
        old_id = a.id
        a.title = u'MyOldAlbum выаыва'
        a.uid = self.uid
        a.save()
        assert type(a.mtime) == float
        assert a.id == old_id

        # получаем альбом
        new_a = Album.controller.get(uid=self.uid, id=a.id)
        assert new_a.title == a.title

    def test_upsert(self):
        # создаем альбом
        a = Album(uid=self.uid, title='MyAlbum')
        a.save()
        # получаем его из "другого" места
        album = Album.controller.get(uid=self.uid, id=a.id)
        # удаляем из базы альбом
        a.delete()
        # альбома в БД уже нет, есть только в приложении, при save ошибка
        album.social_cover_stid = "test:stid"
        self.assertRaises(ZeroUpdateControllerMPFSError, album.save)
        # c upsert сохраняем как есть
        album.save(upsert=True, update_fields=['title'])

    def test_format_spec(self):
        spec = {'id': '544e464b0f32a54662370197'}
        fmt_spec = Album.controller._format_spec(spec)
        assert '_id' in fmt_spec
        assert 'id' not in fmt_spec
        assert ObjectId(spec['id']) == fmt_spec['_id']

        spec = {'title': 'test', '$or': [{'id': '544e464b0f32a54662370197'}, {'id': '544e464b0f32a54662370198'}]}
        fmt_spec = Album.controller._format_spec(spec)
        assert spec['title'] == fmt_spec['title']
        assert '$or' in fmt_spec
        assert fmt_spec['$or'] == map(lambda d: {'_id': ObjectId(d['id'])}, spec['$or'])

        spec = {'id': {'$in': ['544e464b0f32a54662370197', '544e464b0f32a54662370198']}}
        fmt_spec = Album.controller._format_spec(spec)
        assert '_id' in fmt_spec
        assert '$in' in fmt_spec['_id']
        assert fmt_spec['_id']['$in'] == [ObjectId(_id) for _id in spec['id']['$in']]

    def test_build_urlencode_public_url(self):
        """Проверяем, что public_key корректно экранируется в public_url"""
        public_key = Album.build_public_key(self.uid, str(ObjectId()))
        public_url = Album.build_public_url(public_key)
        quoted_public_key = urllib.quote(public_key, safe='')
        assert quoted_public_key in public_url


class AlbumsBaseTestCase(DiskTestCase):
    _original_kladun_generate_album_preview = Previewer.generate_album_preview

    current_fake_stid = None

    def generate_fake_stid(self):
        self.current_fake_stid = '1000:yadisk:fake-stid-%s' % random.randint(0, 100500)
        return self.current_fake_stid

    def setup_method(self, method):
        super(AlbumsBaseTestCase, self).setup_method(method)
        # патчим метод генерации соц. превьюшки альбома, чтоб он не ходил в кладун ибо стида всё равно такого нет
        Previewer.generate_album_preview = lambda *args, **kwargs: self.generate_fake_stid()

    def tear_down_method(self, method):
        # распатчиваем метод генерации соц. превьюшки альбома
        Previewer.generate_album_preview = self._original_kladun_generate_album_preview
        super(AlbumsBaseTestCase, self).tear_down_method(method)


class AlbumsNotificationTestCase(AlbumsBaseTestCase):
    """
    Тестирование альбомных пушей

    Структуру пушей смотри в https://st.yandex-team.ru/CHEMODAN-21403
    """
    def setup_method(self, method):
        super(AlbumsNotificationTestCase, self).setup_method(method)
        # чит для ипорта callback-ов для тестов. В проде импортятися нормально
        import mpfs.core.albums.event_subscribtions
        try:
            reload(mpfs.core.albums.event_subscribtions)
        except ImportError:
            pass

        # подписка
        self.my_xiva_callback = 'http://localhost/service/echo'
        self.service_ok('xiva_subscribe', opts={'uid': self.uid, 'format': 'json', 'callback': self.my_xiva_callback})
        self.connection_id = 'my_connection_id'

    def test_title_change(self):
        """Проверка пуша на изменение названия альбома"""
        album_dict = {'items': []}
        album = self.json_ok('albums_create_with_items', opts={'uid': self.uid}, json=album_dict)

        with PushServicesStub() as push_service:
            self.json_ok('album_set_attr',
                         opts={'uid': self.uid,
                               'album_id': album['id'],
                               'title': 'new_title',
                               'connection_id': self.connection_id})
            push = PushServicesStub.parse_send_call(push_service.send.call_args)

            assert push['connection_id'] == self.connection_id
            assert push['uid'] == self.uid
            data = push['json_payload']['root']
            assert data['tag'] == 'album'
            assert data['parameters']['type'] == 'title_change'
            assert 'public_key' in data['parameters']
            assert 'id' in data['parameters']

    def test_album_create(self):
        """Проверка пуша на создание альбома"""
        album_dict = {'items': []}
        with PushServicesStub() as push_service:
            self.json_ok('albums_create_with_items',
                         opts={'uid': self.uid,
                               'connection_id': self.connection_id},
                         json=album_dict)
            push = PushServicesStub.parse_send_call(push_service.send.call_args)

            assert push['connection_id'] == self.connection_id
            assert push['uid'] == self.uid
            data = push['json_payload']['root']
            assert data['tag'] == 'album'
            assert data['parameters']['type'] == 'album_create'
            assert 'public_key' in data['parameters']
            assert 'id' in data['parameters']

    def test_album_remove(self):
        """Проверка пуша на удаление альбома"""
        album_dict = {'items': []}
        album = self.json_ok('albums_create_with_items',
                             opts={'uid': self.uid,
                                   'connection_id': self.connection_id},
                             json=album_dict)
        with PushServicesStub() as push_service:
            self.json_ok('album_remove',
                         opts={'uid': self.uid,
                               'album_id': album['id'],
                               'connection_id': self.connection_id},
                         json=album_dict)
            push = PushServicesStub.parse_send_call(push_service.send.call_args)

            assert push['connection_id'] == self.connection_id
            assert push['uid'] == self.uid
            data = push['json_payload']['root']
            assert data['tag'] == 'album'
            assert data['parameters']['type'] == 'album_remove'
            assert 'public_key' in data['parameters']
            assert 'id' in data['parameters']

    def test_cover_change(self):
        """Проверка пуша на изменение обложки"""
        album_dict = {'items': []}
        for i in range(2):
            path = '/disk/%i.jpg' % i
            self.upload_file(self.uid, path)
            album_dict['items'].append({'type': 'resource', 'path': path})
        album = self.json_ok('albums_create_with_items', opts={'uid': self.uid}, json=album_dict)

        with PushServicesStub() as push_service:
            self.json_ok('album_set_attr',
                         opts={'uid': self.uid,
                               'album_id': album['id'],
                               'cover': '1',
                               'connection_id': self.connection_id})
            push = PushServicesStub.parse_send_call(push_service.send.call_args)

            assert push['connection_id'] == self.connection_id
            assert push['uid'] == self.uid
            data = push['json_payload']['root']
            assert data['tag'] == 'album'
            assert data['parameters']['type'] == 'cover_change'
            assert 'public_key' in data['parameters']
            assert 'id' in data['parameters']

    def test_publish_unpublish(self):
        """Проверка пуша на изменение публичности"""
        album_dict = {'items': []}
        album = self.json_ok('albums_create_with_items', opts={'uid': self.uid}, json=album_dict)

        with PushServicesStub() as push_service:
            # распубликация
            album = self.json_ok('album_unpublish',
                                 opts={'uid': self.uid,
                                       'album_id': album['id'],
                                       'connection_id': self.connection_id})
            push = PushServicesStub.parse_send_call(push_service.send.call_args)

            assert push['connection_id'] == self.connection_id
            assert push['uid'] == self.uid
            data = push['json_payload']['root']
            assert data['tag'] == 'album'
            assert data['parameters']['type'] == 'unpublish'
            assert 'public_key' in data['parameters']
            assert data['parameters']['is_public'] is False

        with PushServicesStub() as push_service:
            # публикация
            self.json_ok('album_publish',
                         opts={'uid': self.uid,
                               'album_id': album['id'],
                               'connection_id': self.connection_id})
            push = PushServicesStub.parse_send_call(push_service.send.call_args)

            assert push['connection_id'] == self.connection_id
            assert push['uid'] == self.uid
            data = push['json_payload']['root']
            assert data['tag'] == 'album'
            assert data['parameters']['type'] == 'publish'
            assert 'public_key' in data['parameters']
            assert data['parameters']['is_public'] is True

    def test_items_change(self):
        """Проверка пуша на изменение элементов альбома"""
        album_dict = {'items': []}
        album = self.json_ok('albums_create_with_items', opts={'uid': self.uid}, json=album_dict)
        items = []
        for i in range(2):
            path = '/disk/%i.jpg' % i
            self.upload_file(self.uid, path)
            items.append({'type': 'resource', 'path': path})

        with PushServicesStub() as push_service:
            self.json_ok('album_append_items',
                         opts={'uid': self.uid,
                               'album_id': album['id'],
                               'connection_id': self.connection_id},
                         json={'items': items})
            push = PushServicesStub.parse_send_call(push_service.send.call_args)

            assert push['connection_id'] == self.connection_id
            assert push['uid'] == self.uid
            data = push['json_payload']['root']
            assert data['tag'] == 'album'
            assert data['parameters']['type'] == 'append_items'
            values = push['json_payload']['values']
            assert len(values) == 2
            assert values[0]['tag'] == 'item'
            assert 'id' in values[0]['parameters']

        with PushServicesStub() as push_service:
            album = self.json_ok('album_get',
                                 opts={'uid': self.uid,
                                       'album_id': album['id']})
            item_id = album['items'][0]['id']
            self.json_ok('album_item_remove',
                         opts={'uid': self.uid,
                               'item_id': item_id,
                               'connection_id': self.connection_id})
            push = PushServicesStub.parse_send_call(push_service.send.call_args)

            assert push['connection_id'] == self.connection_id
            assert push['uid'] == self.uid
            data = push['json_payload']['root']
            assert data['tag'] == 'album'
            assert data['parameters']['type'] == 'items_remove'
            assert 'public_key' in data['parameters']
            values = push['json_payload']['values']
            assert len(values) == 1
            assert values[0]['tag'] == 'item'
            assert values[0]['parameters']['id'] == item_id


class AlbumsTestCase(SharingTestCaseMixin, AlbumsBaseTestCase):
    RESOURCE_PATH = '/disk/my_file.jpg'

    GROUP_FOLDER_PARENT_PATH = '/disk/dirs'
    GROUP_FOLDER_PATH = '/'.join([GROUP_FOLDER_PARENT_PATH, 'shared'])
    SHARED_RESOURCE_NAME = 'shared_file.jpg'
    GROUP_RESOURCE_PATH = '/'.join([GROUP_FOLDER_PATH, SHARED_RESOURCE_NAME])
    SHARED_FOLDER_PATH = '/disk/shared'
    SHARED_RESOURCE_PATH = '/'.join([SHARED_FOLDER_PATH, SHARED_RESOURCE_NAME])

    ALBUM_CREATE_JSON = {
        'title': 'MyAlbum',
        'description': 'My Test Album',
        'layout': 'rows',
        'flags': ['show_frames'],
        'items': [
            {'type': 'resource', 'path': RESOURCE_PATH},
            {'type': 'resource', 'path': SHARED_RESOURCE_PATH}
        ],
    }

    other_uid = user_1.uid

    def setup_method(self, method):
        super(AlbumsTestCase, self).setup_method(method)
        self.upload_file(self.uid, self.RESOURCE_PATH, file_data={'mtime': 100})

        # создаём другого пользователя и общую папку с файлом у него
        self.create_user(self.other_uid)
        self.json_ok('mkdir', opts={'uid': self.other_uid, 'path': self.GROUP_FOLDER_PARENT_PATH})
        self.json_ok('mkdir', opts={'uid': self.other_uid, 'path': self.GROUP_FOLDER_PATH})
        self.upload_file(self.other_uid, self.GROUP_RESOURCE_PATH, file_data={'mtime': 200})
        # расшариваем папку
        group = self.json_ok('share_create_group', opts={'uid': self.other_uid, 'path': self.GROUP_FOLDER_PATH})
        self.gid = group['gid']
        # приглашаем тестового пользователя в расшаренную папку
        invite_hash = self.share_invite(group['gid'], self.uid)
        # подключаем расшареную папку тестовому пользователю
        self.json_ok('share_activate_invite', opts={'uid': self.uid, 'hash': invite_hash})

    def test_album_name_for_unnamed_albums(self):
        album_dict = deepcopy(self.ALBUM_CREATE_JSON)
        album = self.json_ok('albums_create_with_items', opts={'uid': self.uid}, json=album_dict)

        from mpfs.dao.session import Session
        session = Session.create_from_uid(self.uid)
        session.execute('UPDATE disk.albums SET album_type=:album_type, title=:title WHERE public_key=:public_key',
                        {'public_key': album['public']['public_key'], 'title': '', 'album_type': AlbumType.FACES.value})

        resp = self.json_ok('public_album_download_url', opts={'uid': self.uid, 'public_key': album['public']['public_key']})
        assert 'filename=album.zip' in resp['url']

    def test_album_sort_order(self):
        album_dict = deepcopy(self.ALBUM_CREATE_JSON)
        my_file_path = '/disk/my_file2.jpg'
        self.upload_file(self.uid, my_file_path)
        album_dict['items'].append({'type': 'resource', 'path': my_file_path})
        album = self.json_ok('albums_create_with_items', opts={'uid': self.uid}, json=album_dict)
        assert all(int(item['order_index']) == item['object']['etime'] for item in album['items'])
        old_time = album['mtime']
        one_more_file = '/disk/one_more_file.jpg'
        items = [{'type': 'resource', 'path': one_more_file}, ]
        time.sleep(2)
        resp = self.json_ok('album_append_items', opts={'uid': self.uid, 'album_id': album['id']}, json={'items': items})
        album = self.json_ok('album_get', opts={'uid': self.uid, 'album_id': album['id']})
        assert all(int(item['order_index']) == item['object']['etime'] for item in album['items'])
        assert old_time < album['mtime']
        old_time = album['mtime']
        cover_id = album['cover']['id']
        time.sleep(2)
        self.json_ok('album_item_remove', opts={'uid': self.uid, 'item_id': cover_id})
        album = self.json_ok('album_get', opts={'uid': self.uid, 'album_id': album['id']})
        assert all(int(item['order_index']) == item['object']['etime'] for item in album['items'])
        assert old_time < album['mtime']
        assert album['items'] == sorted(album['items'], key=lambda item: item['object']['etime'])

    def test_album_favorites_sort_order(self):
        album_dict = deepcopy(self.ALBUM_CREATE_JSON)
        album_dict['album_type'] = AlbumType.FAVORITES.value
        my_file_path = '/disk/my_file2.jpg'
        self.upload_file(self.uid, my_file_path, file_data={'mtime': 300})
        album_dict['items'].append({'type': 'resource', 'path': my_file_path})
        album = self.json_ok('albums_create_with_items', opts={'uid': self.uid}, json=album_dict)
        assert all(int(item['order_index']) == item['object']['etime'] for item in album['items'])
        old_time = album['mtime']
        one_more_file = '/disk/one_more_file.jpg'
        items = [{'type': 'resource', 'path': one_more_file}, ]
        time.sleep(2)
        resp = self.json_ok('album_append_items', opts={'uid': self.uid, 'album_id': album['id']}, json={'items': items})
        album = self.json_ok('album_get', opts={'uid': self.uid, 'album_id': album['id']})
        assert all(int(item['order_index']) == item['object']['etime'] for item in album['items'])
        assert old_time < album['mtime']
        old_time = album['mtime']
        cover_id = album['cover']['id']
        time.sleep(2)
        self.json_ok('album_item_remove', opts={'uid': self.uid, 'item_id': cover_id})
        album = self.json_ok('album_get', opts={'uid': self.uid, 'album_id': album['id']})
        assert all(int(item['order_index']) == item['object']['etime'] for item in reversed(album['items']))
        assert old_time < album['mtime']
        assert album['items'] == sorted(album['items'], key=lambda item: item['object']['etime'], reverse=True)

    @parameterized.expand(['public_key', 'public_url', 'short_url'])
    def test_album_blocking_and_unblocking_by_key(self, block_param):
        album_dict = deepcopy(self.ALBUM_CREATE_JSON)
        album = self.json_ok('albums_create_with_items', opts={'uid': self.uid}, json=album_dict)

        album_id = album['id']
        album_pk = album['public']['public_key']
        block_param_value = album['public'][block_param]
        album_item_id = album['items'][0]['id']

        # проверяем, что если заданые не все обязательные параметры то будет 500очка и что блокировка не установится при этом
        self.json_error('public_album_block', opts={'public_key': album_pk})
        self.json_error('public_album_block', opts={'reason': 'lkjdfga'})
        album = self.json_ok('album_get', opts={'uid': self.uid, 'album_id': album_id})
        assert 'is_blocked' in album
        assert album['is_blocked'] is False

        # блокируем альбом
        album = self.json_ok('public_album_block', opts={'public_key': block_param_value, 'reason': 'test'})
        assert 'is_blocked' in album
        assert album['is_blocked'] is True
        # владелец по прежнему видит альбом
        album = self.json_ok('public_album_get', opts={'public_key': album_pk, 'uid': self.uid})
        # сторонний пользователь альбом не видит уаще ни как и получает 404
        opts = {'public_key': album_pk, 'uid': self.other_uid}
        item_opts = dict(opts.items() + [('item_id', album_item_id)])
        self.json_error('public_album_get', opts=opts, code=71)
        self.json_error('public_album_items_list', opts=opts, code=71)
        self.json_error('public_album_check', opts=opts, code=71)
        self.json_error('public_album_save', opts=opts, code=71)
        self.json_error('async_public_album_save', opts=opts, code=71)
        self.json_error('public_album_item_download_url', opts=item_opts, code=71)
        self.json_error('public_album_download_url', opts=opts, code=71)
        self.json_error('public_album_item_info', opts=item_opts, code=71)
        self.json_error('public_album_item_video_url', opts=item_opts, code=71)
        self.json_error('public_album_social_wall_post',
                        opts={'public_key': album_pk, 'uid': self.uid, 'provider': 'facebook'},
                        code=71)

        # разблокируем альбом
        album = self.json_ok('public_album_unblock', opts={'public_key': block_param_value})
        assert 'is_blocked' in album
        assert album['is_blocked'] is False
        assert album['block_reason'] == ''
        # и снова альбом видно всем посторонним пользователям по публичной ссылочке
        self.json_ok('public_album_get', opts=opts)
        self.json_ok('public_album_items_list', opts=opts)
        self.json_ok('public_album_check', opts=opts)
        self.json_ok('public_album_save', opts=opts)
        self.json_ok('async_public_album_save', opts=opts)
        self.json_ok('public_album_item_download_url', opts=item_opts)
        self.json_ok('public_album_download_url', opts=opts)
        self.json_ok('public_album_item_info', opts=item_opts)
        self.json_ok('public_album_item_video_url', opts=item_opts)
        # тут и сказочки конец, а кто слушал -- молодец

    def test_social_cover_url(self):
        """Для публичных альбомов добавляет в ответ ссылку на спец. превью для соц. сетей"""
        album_dict = {'items': []}
        for i in range(1):
            path = '/disk/%i.jpg' % i
            self.upload_file(self.uid, path)
            album_dict['items'].append({'type': 'resource', 'path': path})
        album = self.json_ok('albums_create_with_items', opts={'uid': self.uid}, json=album_dict)
        assert 'social_cover_url' in album
        assert 'size=' in album['social_cover_url']
        assert 'crop=' in album['social_cover_url']
        assert '/preview/' in album['social_cover_url']

    def test_async_task_to_generate_social_cover_if_sync_failed(self):
        path = '/disk/1.gif'
        self.upload_file(self.uid, path)
        album_dict = {'items': [{'type': 'resource', 'path': path}]}

        # Сначала возвращаем ошибку, потом нормальный ответ
        with mock.patch('mpfs.core.albums.models.Previewer.generate_album_preview',
                        side_effect=[KladunNoResponse, '1' * 64]):
            album = self.json_ok('albums_create_with_items', opts={'uid': self.uid}, json=album_dict)
        # До выполенения асинхронного таска обложки нет
        assert 'social_cover_stid' in album
        assert album['social_cover_stid'] is None
        album = self.json_ok('album_get',
                             opts={'uid': self.uid,
                                   'album_id': album['id']})
        # После - есть
        assert 'social_cover_stid' in album
        assert album['social_cover_stid'] is not None

    def test_social_cover_update(self):
        """Проверка обновленися соц. обложки при обновлении свойств альбома."""
        album_dict = {'items': []}
        for i in range(2):
            path = '/disk/%i.jpg' % i
            self.upload_file(self.uid, path)
            album_dict['items'].append({'type': 'resource', 'path': path})
        album = self.json_ok('albums_create_with_items', opts={'uid': self.uid}, json=album_dict)
        assert 'social_cover_stid' in album
        album_id = album['id']
        social_cover_stid = album['social_cover_stid']
        # меняем title и проверяем, что стид соц. обложки изменился
        album = self.json_ok('album_set_attr', opts={'uid': self.uid, 'album_id': album_id, 'title': '2222'})
        album = self.json_ok('album_get', opts={'uid': self.uid, 'album_id': album_id})
        assert album['social_cover_stid'] != social_cover_stid

    def test_if_not_exists(self):
        """Проверка параметра if_not_exists"""
        album_dict = {'items': [], 'title': 'Djigurda'}
        for i in range(2):
            path = '/disk/%i.jpg' % i
            self.upload_file(self.uid, path)
            album_dict['items'].append({'type': 'resource', 'path': path})
        album_dict['items'] = album_dict['items'] * 2

        # создаем альбом без флага if_not_exists
        album = self.json_ok('albums_create_with_items', opts={'uid': self.uid}, json=album_dict)
        assert len(album['items']) == 4

        # создаем альбом с флагом if_not_exists
        album = self.json_ok('albums_create_with_items', opts={'uid': self.uid, 'if_not_exists': "1"}, json=album_dict)
        assert len(album['items']) == 2

        # добавляем уже имеющиеся ресурсы в альбом без флага if_not_exists
        append_dict = {'items': album_dict['items']}
        appended_items = self.json_ok('album_append_items', opts={'uid': self.uid, 'album_id': album['id']}, json=append_dict)['items']
        assert len(appended_items) == 4
        album = self.json_ok('album_get', opts={'uid': self.uid, 'album_id': album['id']})
        assert len(album['items']) == 6

        # добавляем уже имеющиеся ресурсы в альбом с флагом if_not_exists
        appended_items = self.json_ok('album_append_items', opts={'uid': self.uid, 'album_id': album['id'], 'if_not_exists': 1}, json=append_dict)['items']
        assert len(appended_items) == 0
        album = self.json_ok('album_get', opts={'uid': self.uid, 'album_id': album['id']})
        assert len(album['items']) == 6

        # добавляем уже имеющиеся ресурсы и новый файл в альбом с флагома if_not_exists
        self.upload_file(self.uid, '/disk/3.jpg')
        append_dict['items'].append({'type': 'resource', 'path': '/disk/3.jpg'})
        appended_items = self.json_ok('album_append_items', opts={'uid': self.uid, 'album_id': album['id'], 'if_not_exists': 1}, json=append_dict)['items']
        assert len(appended_items) == 1
        album = self.json_ok('album_get', opts={'uid': self.uid, 'album_id': album['id']})
        assert len(album['items']) == 7

        # album_append_item
        self.json_ok('album_append_item', opts={'uid': self.uid, 'album_id': album['id'], 'path': '/disk/3.jpg', 'type': 'resource'})
        self.json_error('album_append_item', opts={'uid': self.uid, 'album_id': album['id'], 'path': '/disk/3.jpg', 'type': 'resource', 'if_not_exists': 1}, code=163)
        self.upload_file(self.uid, '/disk/4.jpg')
        self.json_ok('album_append_item', opts={'uid': self.uid, 'album_id': album['id'], 'path': '/disk/4.jpg', 'type': 'resource', 'if_not_exists': 1})

    def test_none_empty_cover(self):
        """Если есть подходящие элементы для обложки - обложка должна быть и
        соответственно должен быть установлен признак is_empty.
        """
        album_dict = {
            'title': 'MyAlbum',
            'items': [
            ],
        }
        for i in range(2):
            self.upload_file(self.uid, '/disk/%i.jpg' % i)
            album_dict['items'].append({'type': 'resource', 'path': '/disk/%i.jpg' % i})

        # создаем пустой альбом, потом добавляем фотку
        album = self.json_ok('albums_create', opts={'uid': self.uid})
        self.json_ok('album_append_item', opts={'uid': self.uid, 'album_id': album['id'], 'type': 'resource', 'path': '/disk/1.jpg'})
        # создаем альбом с обложкой, потом удаляем ресурс-обложку
        self.json_ok('albums_create_with_items', opts={'uid': self.uid}, json=album_dict)
        self.json_ok('rm', opts={'uid': self.uid, 'path': '/disk/0.jpg'}, json=album_dict)

        albums = self.json_ok('albums_list', opts={'uid': self.uid})
        for album in albums:
            assert album['cover'] is not None
            assert album['is_empty'] is False

        album = self.json_ok('album_get', opts={'uid': self.uid, 'album_id': album['id']})
        assert album['cover'] is not None
        assert album['is_empty'] is False

        # просто пустой альбом - обложки нет
        album = self.json_ok('albums_create', opts={'uid': self.uid})
        assert album['cover'] is None
        assert album['is_empty'] is True

    def test_album_item_count(self):
        album_dict = {'items': []}
        for file_num in range(2):
            file_path = "/disk/%i.jpg" % file_num
            self.upload_file(self.uid, file_path)
            album_dict['items'].append({'type': 'resource', 'path': file_path})

        for i in range(3):
            self.json_ok('albums_create_with_items', opts={'uid': self.uid}, json=album_dict)

        for album in self.json_ok('albums_list', opts={'uid': self.uid}):
            assert 'items_count' not in album

        for album in self.json_ok('albums_list', opts={'uid': self.uid, 'count_items': 1}):
            assert album['items_count'] == 2
            assert album['cover'] is not None

        self.json_ok('rm', {'uid': self.uid, 'path': '/disk/0.jpg'})
        for album in self.json_ok('albums_list', opts={'uid': self.uid, 'count_items': 1}):
            assert album['items_count'] == 1
            assert album['cover'] is not None

        self.json_ok('rm', {'uid': self.uid, 'path': '/disk/1.jpg'})
        for album in self.json_ok('albums_list', opts={'uid': self.uid, 'count_items': 1}):
            assert album['items_count'] == 0
            assert album['cover'] is None

    def test_group_640_660(self):
        """Тестирование групповых файлов в альбоме"""
        shared_dir = '/disk/1'
        self.json_ok('mkdir', {'uid': self.uid, 'path': shared_dir})
        album_dict = {'items': []}
        for file_num in range(2):
            file_path = "%s/%i.jpg" % (shared_dir, file_num)
            self.upload_file(self.uid, file_path)
            album_dict['items'].append({'type': 'resource', 'path': file_path})
        # расшариваем папку
        group = self.json_ok('share_create_group', opts={'uid': self.uid, 'path': shared_dir})
        # приглашаем тестового пользователя в расшаренную папку
        invite_hash = self.share_invite(group['gid'], self.other_uid, rights=640)
        # подключаем расшареную папку тестовому пользователю
        self.json_ok('share_activate_invite', opts={'uid': self.other_uid, 'hash': invite_hash})
        album = self.json_ok('albums_create_with_items', opts={'uid': self.other_uid}, json=album_dict)
        # Переносим общую папку, для полноты картины
        self.json_ok('move', opts={'uid': self.other_uid, 'src': shared_dir, 'dst': '/disk/OMG'})

        # Пришел неизвестно кто - отдаём все
        public_album = self.json_ok('public_album_get', opts={'public_key': album['public']['public_key'], 'meta': 'group'})
        assert len(public_album['items']) == 2
        assert public_album['cover']
        for item in [public_album['cover']] + public_album['items']:
            group_section = item['object']['meta']['group']
            assert group_section['is_shared'] == 1
            assert group_section['rights'] == 640
            assert group_section['is_owned'] == 0
            assert group_section['owner']['uid'] == self.uid
        public_album_items_list = self.json_ok('public_album_items_list', opts={'public_key': album['public']['public_key'], 'meta': 'group'})
        assert len(public_album_items_list) == 2
        # Пришел владелец - отдаем все
        public_album = self.json_ok('public_album_get', opts={'public_key': album['public']['public_key'], 'uid': self.other_uid, 'meta': 'group'})
        assert public_album['cover']
        assert len(public_album['items']) == 2
        ## У элементов в секции meta->group все должно быть по красоте - все от имени запрашивающего uid-а
        for item in [public_album['cover']] + public_album['items']:
            group_section = item['object']['meta']['group']
            assert group_section['is_shared'] == 1
            assert group_section['rights'] == 640
            assert group_section['is_owned'] == 0
            assert group_section['owner']['uid'] == self.uid

        # меняем права на 660 - отдаем всем все
        self.json_ok('share_change_rights', opts={'uid': self.uid, 'gid': group['gid'], 'user_uid': self.other_uid, 'rights': 660})
        public_album = self.json_ok('public_album_get', opts={'public_key': album['public']['public_key'], 'meta': 'group'})
        assert len(public_album['items']) == 2
        assert public_album['cover']
        for item in [public_album['cover']] + public_album['items']:
            group_section = item['object']['meta']['group']
            assert group_section['is_shared'] == 1
            assert group_section['rights'] == 660
            assert group_section['is_owned'] == 0
            assert group_section['owner']['uid'] == self.uid

    def test_group_resource_as_item(self):
        shared_dir = '/disk/1'
        common_dir = '/disk/0000'
        file_name = '_DSC1752.JPG'
        album_dict = deepcopy(self.ALBUM_CREATE_JSON)

        for i, dir_name in enumerate((shared_dir, common_dir)):
            self.json_ok('mkdir', {'uid': self.uid, 'path': dir_name})
            self.upload_file(self.uid, '%s/%s' % (dir_name, file_name))
            album_dict['items'][i]['path'] = '%s/%s' % (dir_name, file_name)
        # расшариваем папку
        group = self.json_ok('share_create_group', opts={'uid': self.uid, 'path': shared_dir})
        # приглашаем тестового пользователя в расшаренную папку
        invite_hash = self.share_invite(group['gid'], self.other_uid)
        # подключаем расшареную папку тестовому пользователю
        self.json_ok('share_activate_invite', opts={'uid': self.other_uid, 'hash': invite_hash})

        album = self.json_ok('albums_create_with_items', opts={'uid': self.uid}, json=album_dict)
        album_id = album['id']
        assert len([i for i in album['items']]) == 2
        album = self.json_ok('album_get', opts={'uid': self.uid, 'album_id': album_id})
        assert len([i for i in album['items']]) == 2

        print "Cоздаем альбомы приглашенным пользователем"
        album_dict = deepcopy(self.ALBUM_CREATE_JSON)
        one_more_shared_file = '%s/%s' % (shared_dir, 'one_more_file.txt')
        self.upload_file(self.other_uid, one_more_shared_file)
        album_dict['items'] = [
            {'type': 'resource', 'path': one_more_shared_file},
            {'type': 'resource', 'path': '%s/%s' % (shared_dir, file_name)},
        ]
        album = self.json_ok('albums_create_with_items', opts={'uid': self.other_uid}, json=album_dict)
        album_id = album['id']
        # для общих папок отдаем публичные превьюхи
        albums = self.json_ok('albums_list', opts={'uid': self.other_uid, 'meta': ''})
        assert 'uid=0' in albums[0]['cover']['object']['meta']['sizes'][0]['url']
        assert len([i for i in album['items']]) == 2
        album = self.json_ok('album_get', opts={'uid': self.other_uid, 'album_id': album_id})
        print [i['id'] for i in album['items']]
        assert len([i for i in album['items']]) == 2

        print "Переименовываем ОП у приглашенного пользователя и пробуем создать альбом с фото из ОП"
        new_shared_dir = "/disk/2"
        assert len(self.json_ok('list', opts={'uid': self.other_uid, 'path': shared_dir})) == 1 + 2
        self.json_ok('move', opts={'uid': self.other_uid, 'src': shared_dir, 'dst': new_shared_dir})
        assert len(self.json_ok('list', opts={'uid': self.other_uid, 'path': new_shared_dir})) == 1 + 2
        print [i['path'] for i in self.json_ok('list', opts={'uid': self.other_uid, 'path': new_shared_dir})]
        one_more_shared_file = '%s/%s' % (new_shared_dir, 'one_more_file.txt')
        album_dict = deepcopy(self.ALBUM_CREATE_JSON)
        album_dict['items'] = [
            {'type': 'resource', 'path': one_more_shared_file},
            {'type': 'resource', 'path': '%s/%s' % (new_shared_dir, file_name)},
        ]
        album = self.json_ok('albums_create_with_items', opts={'uid': self.other_uid}, json=album_dict)
        album_id = album['id']
        print [i['id'] for i in album['items']]
        assert len([i for i in album['items']]) == 2
        album = self.json_ok('album_get', opts={'uid': self.other_uid, 'album_id': album_id})
        print [i['id'] for i in album['items']]
        assert len([i for i in album['items']]) == 2

    def test_public_album_check(self):
        album_dict = deepcopy(self.ALBUM_CREATE_JSON)
        album = self.json_ok('albums_create_with_items', opts={'uid': self.uid}, json=album_dict)
        public_key = album['public']['public_key']
        album_check = self.json_ok('public_album_check', {'public_key': public_key})
        assert len(album_check.keys()) == 2
        assert 'cover' in album_check
        assert 'title' in album_check
        self.json_ok('album_unpublish', opts={'uid': self.uid, 'album_id': album['id']})
        self.json_error('public_album_check', {'public_key': public_key})
        self.json_ok('public_album_check', {'public_key': public_key, 'uid': self.uid})

    def test_public_album_check_for_blocked_by_passport_account(self):
        album_dict = deepcopy(self.ALBUM_CREATE_JSON)
        album = self.json_ok('albums_create_with_items', opts={'uid': self.uid}, json=album_dict)
        public_key = album['public']['public_key']
        album_check = self.json_ok('public_album_check', {'public_key': public_key})
        assert len(album_check.keys()) == 2
        assert 'cover' in album_check
        assert 'title' in album_check

        update_info_by_uid(self.uid, is_enabled=False)

        self.json_error('public_album_check', {'public_key': public_key}, code=codes.RESOURCE_NOT_FOUND)
        self.json_error('public_album_check', {'public_key': public_key, 'uid': self.uid}, code=codes.RESOURCE_NOT_FOUND)

    def test_public_album_items_list(self):
        album_dict = deepcopy(self.ALBUM_CREATE_JSON)
        album = self.json_ok('albums_create_with_items', opts={'uid': self.uid}, json=album_dict)
        public_key = album['public']['public_key']
        items = self.json_ok('public_album_items_list', {'public_key': public_key})
        assert len(items) == 2
        self.json_ok('album_unpublish', opts={'uid': self.uid, 'album_id': album['id']})
        self.json_error('public_album_items_list', {'public_key': public_key})
        self.json_ok('public_album_items_list', {'public_key': public_key, 'uid': self.uid})

    def test_public_album_items_list_for_blocked_by_passport_account(self):
        album_dict = deepcopy(self.ALBUM_CREATE_JSON)
        album = self.json_ok('albums_create_with_items', opts={'uid': self.uid}, json=album_dict)
        public_key = album['public']['public_key']
        items = self.json_ok('public_album_items_list', {'public_key': public_key})
        assert len(items) == 2

        update_info_by_uid(self.uid, is_enabled=False)

        self.json_error('public_album_items_list', {'public_key': public_key}, code=codes.RESOURCE_NOT_FOUND)
        self.json_error('public_album_items_list', {'public_key': public_key, 'uid': self.uid}, code=codes.RESOURCE_NOT_FOUND)

    def test_albums_create(self):
        opts = {
            'uid': self.uid,
            'title': 'Test Create Album',
            'cover': self.RESOURCE_PATH,
            'layout': 'waterfall',
            'flags': 'show_frames,show_dates',
        }
        album = self.json_ok('albums_create', opts=opts)
        assert album['cover']['object']['id'] == opts['cover']
        assert album['flags'] == opts['flags'].split(',')
        for k in ['uid', 'title', 'layout']:
            assert album[k] == opts[k]

    def test_remove_cover_item(self):
        album_dict = deepcopy(self.ALBUM_CREATE_JSON)
        album_dict['cover'] = 1
        album = self.json_ok('albums_create_with_items', opts={'uid': self.uid}, json=album_dict)
        cover_id = album['cover']['id']
        assert cover_id in [i['id'] for i in album['items']]
        # удаляем элемент-обложку
        self.json_ok('album_item_remove', opts={'uid': self.uid, 'item_id': cover_id})
        album = self.json_ok('album_get', opts={'uid': self.uid, 'album_id': album['id']})
        assert album['cover'] != None
        # добавляем новый кавер
        self.json_ok('album_set_attr', opts={'uid': self.uid, 'album_id': album['id'], 'cover': 0})
        album = self.json_ok('album_get', opts={'uid': self.uid, 'album_id': album['id']})
        cover_id = album['cover']['id']
        assert cover_id in [i['id'] for i in album['items']]

    def test_album_in_album(self):
        album_dict = deepcopy(self.ALBUM_CREATE_JSON)
        album = self.json_ok('albums_create_with_items', opts={'uid': self.uid}, json=album_dict)

        album_dict = deepcopy(self.ALBUM_CREATE_JSON)
        album_dict['items'].append({'type': 'album', 'album_id': album['id']})
        album = self.json_ok('albums_create_with_items', opts={'uid': self.uid}, json=album_dict)
        assert len(album['items']) == 3
        assert any([i['obj_type'] == 'album' for i in album['items']])
        album = self.json_ok('album_get', opts={'uid': self.uid, 'album_id': album['id']})
        assert len(album['items']) == 3
        assert any([i['obj_type'] == 'album' for i in album['items']])

    def test_albums_create_with_items(self):
        album_dict = deepcopy(self.ALBUM_CREATE_JSON)
        album_dict.pop('layout')  # должен автоматически проставиться
        album_dict['cover'] = 1
        album = self.json_ok('albums_create_with_items', opts={'uid': self.uid}, json=album_dict)
        for k in ('title', 'layout', 'flags'):
            assert self.ALBUM_CREATE_JSON[k] == album[k], k
        assert 'items' in album
        assert len(album['items']) == 2
        assert album['cover']['album_id'] == album['id']

        # создаём альбом содержащий альбом
        album_dict.pop('cover', None)
        album_dict['items'].append({'type': 'album', 'album_id': album['id']})
        album = self.json_ok('albums_create_with_items', opts={'uid': self.uid}, json=album_dict)
        assert len(album['items']) == len(album_dict['items'])

        # тест опции no_items
        album = self.json_ok('albums_create_with_items', opts={'uid': self.uid, 'no_items': 1}, json=album_dict)
        assert 'items' not in album
        album = self.json_ok('albums_create_with_items', opts={'uid': self.uid, 'no_items': 0}, json=album_dict)
        assert 'items' in album

    def test_albums_list(self):
        albums_ids = []

        album_dict = deepcopy(self.ALBUM_CREATE_JSON)
        album_dict['cover'] = 1
        album = self.json_ok('albums_create_with_items', opts={'uid': self.uid}, json=album_dict)

        albums_ids.append(album['id'])
        time.sleep(1)

        # создаём альбом содержащий альбом
        album_dict['cover'] = 0
        album_dict['items'].append({'type': 'album', 'album_id': album['id']})
        album = self.json_ok('albums_create_with_items', opts={'uid': self.uid}, json=album_dict)
        albums_ids.append(album['id'])

        with mock.patch('mpfs.core.services.logreader_service.LogreaderService.get_one_counter') as mocked_counters:
            # получаем список альбомов
            all_albums = self.json_ok('albums_list', opts={'uid': self.uid, 'meta': ''})

            # Запроса в counters при получении списка альбомов не должно быть
            mocked_counters.assert_not_called()
            assert len(all_albums) == 2
            assert all_albums[1]['cover']['object']['id'] == self.SHARED_RESOURCE_PATH
            assert all_albums[0]['cover']['object']['id'] == self.RESOURCE_PATH
            assert all_albums[0]['cover']['object']['meta']['sizes'][0]['url'] != all_albums[0]['cover']['object']['meta']['sizes'][1]['url']

            # новые альбомы идут в начале
            assert albums_ids[::-1] == [i['id'] for i in all_albums]

    def test_albums_list_pagination(self):
        albums_ids = []

        album_dict = deepcopy(self.ALBUM_CREATE_JSON)
        album_dict['cover'] = 1
        album = self.json_ok('albums_create_with_items', opts={'uid': self.uid}, json=album_dict)

        albums_ids.append(album['id'])
        time.sleep(1)

        # создаём альбом содержащий альбом
        album_dict['cover'] = 0
        album_dict['items'].append({'type': 'album', 'album_id': album['id']})
        album = self.json_ok('albums_create_with_items', opts={'uid': self.uid}, json=album_dict)
        albums_ids.append(album['id'])

        first_album = self.json_ok('albums_list', opts={'uid': self.uid, 'meta': '', 'amount': '1'})
        second_album = self.json_ok('albums_list', opts={'uid': self.uid, 'meta': '', 'amount': '1', 'offset': '1'})
        assert albums_ids[::-1] == [i['id'] for i in first_album + second_album]
        self.json_error('albums_list', opts={'uid': self.uid, 'meta': '', 'amount': '51'})
        self.json_error('albums_list', opts={'uid': self.uid, 'meta': '', 'amount': '-1'})
        assert not self.json_ok('albums_list', opts={'uid': self.uid, 'meta': '', 'album_type': 'geo'})

    def test_favorites_pagination(self):
        """Must be in reverse order"""

        opts = {'uid': self.uid}
        item_paths = [item['path'] for item in self.ALBUM_CREATE_JSON['items']]
        album_dict = deepcopy(self.ALBUM_CREATE_JSON)
        album_dict['album_type'] = 'favorites'

        album = self.json_ok('albums_create_with_items', opts=opts, json=album_dict)
        opts['album_id'] = album['id']

        items1 = self.json_ok('album_get', opts=dict(opts, amount=1))['items']
        assert [item['object']['path'] for item in items1] == [item_paths[-1]]

        items2 = self.json_ok('album_get', opts=dict(opts, amount=1, last_item_id=items1[-1]['id']))['items']
        assert [item['object']['path'] for item in items2] == [item_paths[-2]]

    def test_albums_list_filtration(self):
        albums_ids = []

        album_dict = deepcopy(self.ALBUM_CREATE_JSON)
        album_dict['cover'] = 1
        # album_dict['album_type'] = 'geo'
        session = Session.create_from_uid(self.uid)
        album_id_obj = str(ObjectId())
        album_id = session.execute('''
            INSERT INTO disk.albums
            (id, uid, title, description, layout, flags, album_type)
            VALUES (:id, :uid, :title, :description, :layout, :flags, :album_type)
            RETURNING id
        ''', {'id': '\\x'+album_id_obj,
              'uid': self.uid,
              'title': 'MyAlbum',
              'description': 'My Test Album',
              'layout': 'rows',
              'flags': ['show_frames'],
              'album_type': 'geo'})
        session.execute('''
            INSERT INTO disk.album_items
            (id, uid, album_id, obj_type, obj_id, order_index)
            VALUES (:id, :uid, :album_id, :obj_type, :obj_id, :order_index)
        ''', {'id': '\\x'+str(ObjectId()),
              'uid': self.uid,
              'album_id': album_id.fetchone()[0],
              'obj_type': 'resource',
              'obj_id': u'c4e1cf3712106412d68ace3b72a2a336b07c33c7523aaeb5d02e9620f586ea8e',
              'order_index': 0})

        album = self.json_ok('albums_create_with_items', opts={'uid': self.uid}, json=album_dict)

        albums_ids.append(album['id'])
        time.sleep(1)

        # создаём альбом содержащий альбом
        album_dict['cover'] = 0
        album_dict['items'].append({'type': 'album', 'album_id': album['id']})
        album = self.json_ok('albums_create_with_items', opts={'uid': self.uid}, json=album_dict)
        albums_ids.append(album['id'])
        geo_albums = self.json_ok('albums_list', opts={'uid': self.uid, 'meta': '', 'album_type': AlbumType.GEO.value})
        assert geo_albums[0]['id'] == album_id_obj
        personal_albums = self.json_ok('albums_list', opts={'uid': self.uid, 'meta': ''})
        assert albums_ids[::-1] == [i['id'] for i in personal_albums]
        personal_albums = self.json_ok('albums_list', opts={'uid': self.uid,
                                                            'meta': '',
                                                            'album_type': AlbumType.PERSONAL.value})
        assert albums_ids[::-1] == [i['id'] for i in personal_albums]
        self.json_error('albums_list', opts={'uid': self.uid, 'meta': '', 'album_type': 'wrong'})

    def test_layout_change_dont_change_geo_mtime(self):
        session = Session.create_from_uid(self.uid)
        album_id_obj = str(ObjectId())
        album_id = session.execute('''
            INSERT INTO disk.albums
            (id, uid, title, layout, album_type, date_modified)
            VALUES (:id, :uid, :title, :layout, :album_type, :date_modified)
            RETURNING id
        ''', {'id': '\\x'+album_id_obj,
              'uid': self.uid,
              'title': 'MyAlbum',
              'layout': 'rows',
              'album_type': 'geo',
              'date_modified': '2020-02-07 17:26:14+03'})

        session.execute('''
            INSERT INTO disk.album_items
            (id, uid, album_id, obj_type, obj_id, order_index)
            VALUES (:id, :uid, :album_id, :obj_type, :obj_id, :order_index)
        ''', {'id': '\\x'+str(ObjectId()),
              'uid': self.uid,
              'album_id': album_id.fetchone()[0],
              'obj_type': 'resource',
              'obj_id': u'c4e1cf3712106412d68ace3b72a2a336b07c33c7523aaeb5d02e9620f586ea8e',
              'order_index': 0})
        album = self.json_ok('album_get', opts={'uid': self.uid, 'album_id': album_id_obj})
        old_mtime = album['mtime']
        new_layout = 'waterfall'
        album = self.json_ok('album_set_attr', opts={'uid': self.uid, 'album_id': album_id_obj, 'layout': new_layout})
        assert album['layout'] == new_layout
        # album = self.json_ok('album_get', opts={'uid': self.uid, 'album_id': album_id})
        new_mtime = album['mtime']
        assert old_mtime == new_mtime

    def test_album_get(self):
        album_dict = deepcopy(self.ALBUM_CREATE_JSON)
        album_dict.pop('layout')  # должен автоматически проставиться
        album_dict['cover'] = 1
        album = self.json_ok('albums_create_with_items', opts={'uid': self.uid}, json=album_dict)
        album_id = album['id']

        # получить альбом вместе с содержимым
        album = self.json_ok('album_get', opts={'uid': self.uid, 'album_id': album_id})
        assert 'items' in album
        assert len(album['items']) == 2
        assert album['cover']['album_id'] == album['id']

        # получить альбом с одним элементом
        album = self.json_ok('album_get', opts={'uid': self.uid, 'album_id': album_id, 'amount': 1})
        assert 'items' in album
        assert len(album['items']) == 1
        assert album['items'][0]['object']['id'] == self.RESOURCE_PATH

        # получить альбом со вторым элементом
        album = self.json_ok('album_get',
                             opts={'uid': self.uid, 'album_id': album_id, 'amount': 1, 'last_item_id': album['items'][0]['id'], 'meta': ''})
        assert 'items' in album
        assert len(album['items']) == 1
        assert album['items'][0]['object']['id'] == self.SHARED_RESOURCE_PATH

        assert 'path' in album['items'][0]['object']
        assert 'meta' in album['items'][0]['object']
        assert 'meta' not in album['items'][0]['object']['meta']

        # preview_quality
        album = self.json_ok('album_get', opts={'uid': self.uid, 'album_id': album_id, 'preview_quality': 100, 'meta': ''})
        for item in album['items']:
            assert '&quality=100' in item['object']['meta']['preview']
            assert '&quality=100' in item['object']['meta']['thumbnail']
            assert '&quality=100' in item['object']['meta']['sizes'][0]['url']
            assert '&quality=100' in item['object']['meta']['sizes'][-1]['url']

    def test_album_item_check(self):
        album_dict = deepcopy(self.ALBUM_CREATE_JSON)
        album1 = self.json_ok('albums_create_with_items', opts={'uid': self.uid}, json=album_dict)

        item = self.json_ok('album_item_check', opts={'uid': self.uid, 'path': '/disk/my_file.jpg'})
        assert item['object']['name'] == 'my_file.jpg'

        self.upload_file(self.uid, '/disk/not_in_album_file.jpg')
        self.json_error(
            'album_item_check',
            opts={'uid': self.uid, 'path': '/disk/not_in_album_file.jpg'},
            code=71
        )

        album_dict['title'] = "MyAlbum2"
        album2 = self.json_ok('albums_create_with_items', opts={'uid': self.uid}, json=album_dict)

        self.json_ok('album_remove', opts={'uid': self.uid, 'album_id': album1['id']})
        self.json_ok('album_item_check', opts={'uid': self.uid, 'path': '/disk/my_file.jpg'})
        self.json_ok('album_remove', opts={'uid': self.uid, 'album_id': album2['id']})
        self.json_error(
            'album_item_check',
            opts={'uid': self.uid, 'path': '/disk/my_file.jpg'},
            code=71
        )

    def test_album_item_check_in_trash(self):
        """
        Тест для таска https://st.yandex-team.ru/CHEMODAN-22342
        """
        album_dict = deepcopy(self.ALBUM_CREATE_JSON)
        album = self.json_ok('albums_create_with_items', opts={'uid': self.uid}, json=album_dict)
        assert len(album['items']) == 2
        self.json_ok('album_item_check', opts={'uid': self.uid, 'path': '/disk/my_file.jpg'})
        self.json_ok('trash_append', {'uid': self.uid, 'path': '/disk/my_file.jpg'})

        self.json_error('album_item_check', opts={'uid': self.uid, 'path': '/trash/my_file.jpg'}, code=71)

        album = self.json_ok('album_get', opts={'uid': self.uid, 'album_id': album['id']})
        assert len(album['items']) == 1

    def test_album_get_not_found(self):
        self.json_error('album_get', opts={'uid': self.uid, 'album_id': 'asdflkjhkl'}, code=codes.ALBUM_NOT_FOUND)

    def test_album_set_attr(self):
        album_dict = deepcopy(self.ALBUM_CREATE_JSON)
        album = self.json_ok('albums_create_with_items', opts={'uid': self.uid}, json=album_dict)
        album_id = album['id']

        # меняем title
        new_title = u'Пыдыдыщь!'
        album = self.json_ok('album_set_attr', opts={'uid': self.uid, 'album_id': album_id, 'title': new_title})
        assert album['title'] == new_title
        album = self.json_ok('album_get', opts={'uid': self.uid, 'album_id': album_id})
        assert album['title'] == new_title

        # меняем title на пустой и проверяем, что ничего не изменилось
        new_empty_title = u''
        album = self.json_ok('album_set_attr', opts={'uid': self.uid, 'album_id': album_id, 'title': new_empty_title})
        assert album['title'] == new_title
        album = self.json_ok('album_get', opts={'uid': self.uid, 'album_id': album_id})
        assert album['title'] == new_title

        # меняем description
        new_description = u'Пыбыдабыдыбыдыщ!'
        album = self.json_ok('album_set_attr',
                             opts={'uid': self.uid, 'album_id': album_id, 'description': new_description})
        assert album['description'] == new_description
        album = self.json_ok('album_get', opts={'uid': self.uid, 'album_id': album_id})
        assert album['description'] == new_description

        # меняем description на пустой и проверяем, что ничего не изменилось
        new_empty_description = u''
        album = self.json_ok('album_set_attr', opts={'uid': self.uid, 'album_id': album_id, 'description': new_empty_description})
        assert album['description'] == new_description
        album = self.json_ok('album_get', opts={'uid': self.uid, 'album_id': album_id})
        assert album['description'] == new_description

        # меняем layout
        new_layout = 'waterfall'
        album = self.json_ok('album_set_attr', opts={'uid': self.uid, 'album_id': album_id, 'layout': new_layout})
        assert album['layout'] == new_layout
        album = self.json_ok('album_get', opts={'uid': self.uid, 'album_id': album_id})
        assert album['layout'] == new_layout

        # меняем flags
        new_flags = 'show_frames,show_dates'
        album = self.json_ok('album_set_attr', opts={'uid': self.uid, 'album_id': album_id, 'flags': new_flags})
        assert ','.join(album['flags']) == new_flags
        album = self.json_ok('album_get', opts={'uid': self.uid, 'album_id': album_id})
        assert ','.join(album['flags']) == new_flags

        # меняем кавёр по номеру
        new_cover = 1
        new_cover_id = album['items'][new_cover]['object']['id']
        album = self.json_ok('album_set_attr', opts={'uid': self.uid, 'album_id': album_id, 'cover': new_cover})
        assert album['cover']['object']['id'] == new_cover_id
        album = self.json_ok('album_get', opts={'uid': self.uid, 'album_id': album_id})
        assert album['cover']['object']['id'] == new_cover_id
        first_item_id = album['items'][0]['id']
        # меняем кавёр по id
        new_cover = first_item_id
        new_cover_path = album['items'][0]['object']['id']
        album = self.json_ok('album_set_attr', opts={'uid': self.uid, 'album_id': album_id, 'cover': new_cover})
        assert album['cover']['object']['id'] == new_cover_path
        album = self.json_ok('album_get', opts={'uid': self.uid, 'album_id': album_id})
        assert album['cover']['object']['id'] == new_cover_path

        # проверяем, что у альбома нет cover_offset_y (опциональный параметр)
        album = self.json_ok('album_get', opts={'uid': self.uid, 'album_id': album_id})
        assert not hasattr(album, 'cover_offset_y')

        # добавляем cover_offset_y
        cover_offset = 777
        album = self.json_ok('album_set_attr',
                             opts={'uid': self.uid, 'album_id': album_id, 'cover_offset_y': cover_offset})
        assert album['cover_offset_y'] == cover_offset

        # проверяем, что cover_offset_y записался
        album = self.json_ok('album_get', opts={'uid': self.uid, 'album_id': album_id})
        assert album['cover_offset_y'] == cover_offset

        # проверяем изменение cover_offset_y
        new_offset = 1.5
        album = self.json_ok('album_set_attr',
                             opts={'uid': self.uid, 'album_id': album_id, 'cover_offset_y': new_offset})
        assert album['cover_offset_y'] == new_offset
        album = self.json_ok('album_get', opts={'uid': self.uid, 'album_id': album_id})
        assert album['cover_offset_y'] == new_offset

        # проверяем изменение cover_offset_y на 0
        new_offset = 0
        album = self.json_ok('album_set_attr',
                             opts={'uid': self.uid, 'album_id': album_id, 'cover_offset_y': new_offset})
        assert album['cover_offset_y'] == new_offset
        album = self.json_ok('album_get', opts={'uid': self.uid, 'album_id': album_id})
        assert album['cover_offset_y'] == new_offset

        # проверяем валидацию cover_offset_y
        wrong_offset_param = "abc"
        album = self.json_error('album_set_attr',
                                opts={'uid': self.uid, 'album_id': album_id, 'cover_offset_y': wrong_offset_param})

        # проверяем, что ничего не изменилось
        album = self.json_ok('album_get', opts={'uid': self.uid, 'album_id': album_id})
        assert album['cover_offset_y'] == new_offset

    def test_album_append_items(self):
        album_dict = deepcopy(self.ALBUM_CREATE_JSON)
        album = self.json_ok('albums_create_with_items', opts={'uid': self.uid}, json=album_dict)
        album_id = album['id']
        old_time = album['mtime']
        time.sleep(2)
        new_resource_path = '/disk/one_more_file.jpg'
        self.upload_file(self.uid, new_resource_path)

        items = {'items': [{'type': 'resource', 'path': new_resource_path}]}
        self.json_ok('album_append_items', opts={'uid': self.uid, 'album_id': album_id}, json=items)
        album = self.json_ok('album_get', opts={'uid': self.uid, 'album_id': album_id})
        assert len(album['items']) == len(album_dict['items']) + 1
        # assert album['items'][-1]['order_index'] == len(album_dict['items']) + 1
        assert old_time < album['mtime']
        self.json_error('album_append_items', opts={'uid': self.uid, 'album_id': album_id[:-1]}, json=items,
                        code=codes.ALBUM_NOT_FOUND, status=404)


    def test_album_append_item(self):
        album_dict = deepcopy(self.ALBUM_CREATE_JSON)
        album = self.json_ok('albums_create_with_items', opts={'uid': self.uid}, json=album_dict)
        album_id = album['id']
        old_time = album['mtime']

        new_resource_path = '/disk/one_more_file.jpg'
        self.upload_file(self.uid, new_resource_path)
        time.sleep(2)
        self.json_ok('album_append_item',
                     opts={'uid': self.uid, 'album_id': album_id, 'type': 'resource', 'path': new_resource_path})
        album = self.json_ok('album_get', opts={'uid': self.uid, 'album_id': album_id})
        assert len(album['items']) == len(album_dict['items']) + 1
        # assert album['items'][-1]['order_index'] == len(album_dict['items']) + 1
        assert old_time < album['mtime']
        self.json_error('album_append_item', opts={'uid': self.uid, 'album_id': album_id[:-1],
                                                   'type': 'resource', 'path': new_resource_path},
                        code=codes.ALBUM_NOT_FOUND, status=404)
        self.json_error('album_append_item', opts={'uid': self.uid, 'album_id': album_id,
                                                   'type': 'resource', 'path': '/disk/not_existing_file.jpg'},
                        code=codes.ALBUMS_UNABLE_TO_APPEND_ITEM, status=404)

    def test_album_copy(self):
        album_dict = deepcopy(self.ALBUM_CREATE_JSON)
        album = self.json_ok('albums_create_with_items', opts={'uid': self.uid}, json=album_dict)
        album_id = album['id']

        new_album = self.json_ok('album_copy', opts={'uid': self.uid, 'album_id': album_id})
        assert new_album['id'] != album['id']
        assert new_album['id'] is not None
        assert album['cover']['album_id'] != new_album['cover']['album_id']

        album = self.json_ok('album_get', opts={'uid': self.uid, 'album_id': album_id})
        new_album = self.json_ok('album_get', opts={'uid': self.uid, 'album_id': new_album['id']})

        for item, new_item in zip(album['items'], new_album['items']):
            assert item['id'] != new_item['id']
            assert new_item['id'] is not None
            assert item['album_id'] != new_item['album_id']
            assert new_item['album_id'] is not None
            item['object'].pop('meta', None)
            for k, v in item['object'].iteritems():
                assert v == new_item['object'][k], k
        assert album['cover']['album_id'] != new_album['cover']['album_id']

    def test_album_copy_with_empty_cover(self):
        album_dict = deepcopy(self.ALBUM_CREATE_JSON)
        album_dict['items'] = []
        album = self.json_ok('albums_create_with_items', opts={'uid': self.uid}, json=album_dict)

        new_album = self.json_ok('album_copy', opts={'uid': self.uid, 'album_id': album['id']})
        assert new_album['id'] != album['id']
        assert new_album['id'] is not None
        assert album['cover'] == new_album['cover'] == None

    def test_album_item_move(self):
        album_dict = deepcopy(self.ALBUM_CREATE_JSON)
        third_file_path = '/disk/third_file.jpg'
        self.upload_file(self.uid, third_file_path)
        album_dict['items'].append({'type': 'resource', 'path': third_file_path})

        album = self.json_ok('albums_create_with_items', opts={'uid': self.uid}, json=album_dict)
        album_id = album['id']
        item_id = album['items'][-1]['id']

        # двигаем последний элемент в начало
        self.json_error('album_item_move', opts={'uid': self.uid, 'item_id': item_id, 'to_index': 0}, code=codes.ALBUM_ITEM_CAN_NOT_BE_MOVED)
        # assert item['order_index'] == (album['items'][0]['order_index'] / 2.0)
        # album = self.json_ok('album_get', opts={'uid': self.uid, 'album_id': album_id})
        # assert album['items'][0]['order_index'] == (album['items'][1]['order_index'] / 2.0)
        #
        # # двигаем первый элемент в конец
        # new_index = len(album_dict['items'])
        # item = self.json_ok('album_item_move',
        #                     opts={'uid': self.uid, 'item_id': album['items'][0]['id'], 'to_index': new_index})
        # assert item['order_index'] == new_index
        #
        # item = self.json_ok('album_item_move',
        #                     opts={'uid': self.uid, 'item_id': album['items'][1]['id'], 'to_index': new_index})
        # assert item['order_index'] == new_index + 1
        #
        # item = self.json_ok('album_item_move',
        #                     opts={'uid': self.uid, 'item_id': album['items'][2]['id'], 'to_index': new_index})
        # assert item['order_index'] == new_index + 2
        #
        # album = self.json_ok('album_get', opts={'uid': self.uid, 'album_id': album_id})
        # assert album['items'][-1]['order_index'] == new_index + 2

    def test_album_cover(self):
        album_dict = deepcopy(self.ALBUM_CREATE_JSON)
        album = self.json_ok('albums_create_with_items', opts={'uid': self.uid}, json=album_dict)
        assert 'meta' not in album['cover']['object']

    def test_albums_create_from_folder(self):
        folder_path = '/disk/test_albums_create_from_folder'
        self.json_ok('mkdir', {'uid': self.uid, 'path': folder_path})
        files_creation_order = []
        for extension in ('jpg', 'gif', 'huif', 'avi', 'txt'):
            file_path = "%s/file.%s" % (folder_path, extension)
            self.upload_file(self.uid, file_path)
            files_creation_order.append(file_path)
        for i in range(2):
            self.json_ok('mkdir', {'uid': self.uid, 'path': '%s/dir_%i' % (folder_path, i)})

        # указываем обложку
        album = self.json_ok('albums_create_from_folder', opts={'uid': self.uid, 'path': folder_path, 'cover': '%s/file.avi' % folder_path})
        assert len(album['items']) == 5
        assert album['cover']['object']['path'] == '%s/file.avi' % folder_path

        # создаем альбом из папки
        album = self.json_ok('albums_create_from_folder', opts={'uid': self.uid, 'path': folder_path})
        assert len(album['items']) == 5

        # тестируем фильтрацию по media_type
        album = self.json_ok('albums_create_from_folder', opts={'uid': self.uid, 'path': folder_path, 'media_type': 'image'})
        assert len(album['items']) == 2
        album = self.json_ok('albums_create_from_folder', opts={'uid': self.uid, 'path': folder_path, 'media_type': 'image,video'})
        assert len(album['items']) == 3

    def test_album_item_remove(self):
        album_dict = deepcopy(self.ALBUM_CREATE_JSON)
        album = self.json_ok('albums_create_with_items', opts={'uid': self.uid}, json=album_dict)
        album_id = album['id']
        item_id = album['items'][1]['id']
        old_time = album['mtime']
        time.sleep(2)
        # неизвестный альбом
        self.json_error('album_item_remove', opts={'uid': self.uid, 'item_id': '666'}, code=71)
        # не владелец альбома
        self.json_error('album_item_remove', opts={'uid': self.other_uid, 'item_id': item_id}, code=71)

        self.json_ok('album_item_remove', opts={'uid': self.uid, 'item_id': item_id})
        album = self.json_ok('album_get', opts={'uid': self.uid, 'album_id': album_id})
        assert len(album['items']) == len(album_dict['items']) - 1
        assert old_time < album['mtime']

    def test_album_item_set_attr(self):
        album_dict = deepcopy(self.ALBUM_CREATE_JSON)
        album = self.json_ok('albums_create_with_items', opts={'uid': self.uid}, json=album_dict)
        album_id = album['id']
        item_id = album['items'][1]['id']

        new_description = u'Ахтунг!'
        item = self.json_ok('album_item_set_attr',
                            opts={'uid': self.uid, 'item_id': item_id, 'description': new_description})
        assert item['description'] == new_description
        album = self.json_ok('album_get', opts={'uid': self.uid, 'album_id': album_id})
        assert album['items'][1]['description'] == new_description

        # удаляем дескрипшн элемента
        new_description = ''
        item = self.json_ok('album_item_set_attr',
                            opts={'uid': self.uid, 'item_id': item_id, 'description': new_description})
        assert item['description'] == new_description
        album = self.json_ok('album_get', opts={'uid': self.uid, 'album_id': album_id})
        assert album['items'][1]['description'] == new_description

    def test_album_with_shared_cover(self):
        album_dict = deepcopy(self.ALBUM_CREATE_JSON)
        # только шаренный файл
        album_dict['items'] = [album_dict['items'][1]]
        album_dict['cover'] = 0
        album = self.json_ok('albums_create_with_items', opts={'uid': self.uid}, json=album_dict)
        public_key = album['public']['public_key']
        self.json_ok('share_leave_group', {'gid': self.gid, 'uid': self.uid})
        album = self.json_ok('public_album_get', opts={'public_key': public_key})
        assert album['items'] == []
        assert album['cover'] == None
        album = self.json_ok('public_album_get', opts={'public_key': public_key, 'uid': self.uid})
        assert album['items'] == []
        assert album['cover'] == None

    def test_album_publishing(self):
        album_dict = deepcopy(self.ALBUM_CREATE_JSON)
        album = self.json_ok('albums_create_with_items', opts={'uid': self.uid}, json=album_dict)
        album_id = album['id']

        album = self.json_ok('album_publish', opts={'uid': self.uid, 'album_id': album_id})
        assert album.get('is_public')
        assert 'public' in album
        assert 'views_count' in album['public']
        album = self.json_ok('album_get', opts={'uid': self.uid, 'album_id': album_id})
        assert album.get('is_public')
        assert 'public' in album
        assert 'views_count' in album['public']
        assert isinstance(album['public']['short_url'], (str, unicode))  # проверяем что сохраняется url, а не список
        assert urlparse(album['public']['short_url']).path.startswith('/a/')
        public_key = album['public']['public_key']

        public_album = self.json_ok('public_album_get', opts={'public_key': public_key})
        assert public_album.get('is_public') is True
        assert 'public' in public_album

        # публичные данные альбома не должны удаляться при приватизации https://st.yandex-team.ru/CHEMODAN-20725
        album = self.json_ok('album_unpublish', opts={'uid': self.uid, 'album_id': album_id})
        assert not album.get('is_public')
        assert 'public' in album
        album = self.json_ok('album_get', opts={'uid': self.uid, 'album_id': album_id})
        assert not album.get('is_public')
        assert 'public' in album
        # после приватизации, альбом должен быть по-прежнему доступен владельцу по публичному ключу
        # https://st.yandex-team.ru/CHEMODAN-20725
        self.json_ok('public_album_get', opts={'uid': self.uid, 'public_key': public_key})

    @parameterized.expand([(None, True), ('0', True), ('1', False)])
    def test_album_set_attr_publish_unpublish_no_items(self, no_items_value, expect_items_in_response):
        album_dict = deepcopy(self.ALBUM_CREATE_JSON)
        album = self.json_ok('albums_create_with_items', opts={'uid': self.uid}, json=album_dict)
        album_id = album['id']

        opts = {'uid': self.uid, 'album_id': album_id}
        if no_items_value is not None:
            opts['no_items'] = no_items_value
        album = self.json_ok('album_publish', opts=opts)
        assert expect_items_in_response == ('items' in album)
        assert album['is_public']

        album = self.json_ok('album_unpublish', opts=opts)
        assert expect_items_in_response == ('items' in album)
        assert not album['is_public']

        opts['title'] = 'new title'
        album = self.json_ok('album_set_attr', opts=opts)
        assert expect_items_in_response == ('items' in album)
        assert 'new title' == album['title']

    def test_public_album_save(self):
        album_dict = deepcopy(self.ALBUM_CREATE_JSON)
        file_base_name = '1.jpg'
        for i in range(2):
            dir_path = '/disk/%i' % i
            file_path = '%s/%s' % (dir_path, file_base_name)
            self.json_ok('mkdir', {'uid': self.uid, 'path': dir_path})
            self.upload_file(self.uid, file_path, file_data={'mtime': 400 + i})
            album_dict['items'].append({'type': 'resource', 'path': file_path})
        album = self.json_ok('albums_create_with_items', opts={'uid': self.uid}, json=album_dict)
        album_id = album['id']
        item_id = album['items'][0]['id']
        public_key = album['public']['public_key']

        # сохряем по одному
        resp = self.json_ok('public_album_save', opts={'public_key': public_key, 'uid': self.uid, 'item_id': item_id})
        assert len(resp) == 1
        assert resp[0]['path'] == u'/disk/Загрузки/my_file.jpg'

        # сохраняем с автосуффиксом
        resp = self.json_ok('public_album_save', opts={'public_key': public_key, 'uid': self.uid, 'item_id': item_id})
        assert len(resp) == 1
        assert resp[0]['path'] == u'/disk/Загрузки/my_file (1).jpg'

        # сохраняем весь альбом
        with PushServicesStub() as xiva_stub:
            resp = self.json_ok('public_album_save', opts={'public_key': public_key, 'uid': self.uid})
            assert xiva_stub.send.call_count == 1 + 4 # folder + 4 files
        uniq_names = set([i['name'] for i in resp])
        assert len(resp) == len(album_dict['items'])
        assert len(resp) == len(uniq_names)
        assert file_base_name in uniq_names
        assert "1 (1).jpg" in uniq_names
        for item in resp:
            assert item['path'].startswith('/disk/MyAlbum/')

        resp = self.json_ok('public_album_save', opts={'public_key': public_key, 'uid': self.uid})
        assert len(resp) == len(album_dict['items'])
        for item in resp:
            assert item['path'].startswith('/disk/MyAlbum (1)/')

        resp = self.json_ok('public_album_save', opts={'public_key': public_key, 'uid': self.uid})
        assert len(resp) == len(album_dict['items'])
        for item in resp:
            assert item['path'].startswith('/disk/MyAlbum (2)/')

    def test_public_album_item_save(self):
        album_dict = deepcopy(self.ALBUM_CREATE_JSON)
        album = self.json_ok('albums_create_with_items', opts={'uid': self.uid}, json=album_dict)

        public_key = album['public']['public_key']
        path = '/disk/my_album'
        self.json_ok('mkdir', opts={'uid': self.uid, 'path': path})
        resp = self.json_ok('public_album_save', opts={'public_key': public_key, 'uid': self.uid, 'path': path,
                                                       'item_id': album['items'][0]['id']})
        assert len(resp) == 1

    def test_public_album_item_save_lock(self):
        album_dict = deepcopy(self.ALBUM_CREATE_JSON)
        album = self.json_ok('albums_create_with_items', opts={'uid': self.uid}, json=album_dict)

        public_key = album['public']['public_key']
        path = '/disk/my_album'
        self.json_ok('mkdir', opts={'uid': self.other_uid, 'path': path})
        path += '/test.jpg'
        resp = self.json_ok('public_album_save', opts={'public_key': public_key, 'uid': self.other_uid, 'path': path,
                                                       'item_id': album['items'][0]['id']})
        assert len(resp) == 1

        path = '/disk/my_new_album'
        self.json_ok('mkdir', opts={'uid': self.other_uid, 'path': path})
        path += '/test.jpg'
        resp = self.json_ok('public_album_save', opts={'public_key': public_key, 'uid': self.other_uid, 'path': path,
                                                       'item_id': album['items'][0]['id']})
        assert len(resp) == 1

    def test_public_album_save_to_existent_directory(self):
        album_dict = deepcopy(self.ALBUM_CREATE_JSON)
        album = self.json_ok('albums_create_with_items', opts={'uid': self.uid}, json=album_dict)
        album_id = album['id']

        album = self.json_ok('album_publish', opts={'uid': self.uid, 'album_id': album_id})
        public_key = album['public']['public_key']
        path = '/disk/my_album'
        self.json_ok('mkdir', opts={'uid': self.uid, 'path': path})
        resp = self.json_ok('public_album_save',
                            opts={'public_key': public_key, 'uid': self.uid, 'path': path})
        assert len(resp) == len(album_dict['items'])

    def test_async_public_album_save(self):
        self.json_ok('user_init', opts={'uid': self.uid})
        album_dict = deepcopy(self.ALBUM_CREATE_JSON)
        album = self.json_ok('albums_create_with_items', opts={'uid': self.uid}, json=album_dict)
        album_id = album['id']

        album = self.json_ok('album_publish', opts={'uid': self.uid, 'album_id': album_id})
        # Выбираем конкретный элемент из альбома
        my_file_id = next((item['id']
                           for item in album['items']
                           if item['object']['name'] == 'my_file.jpg'))
        public_key = album['public']['public_key']
        path = '/disk/my_album'
        resp = self.json_ok('async_public_album_save',
                            opts={'public_key': public_key, 'uid': self.other_uid, 'path': path})
        assert resp['path'] == path
        oid = resp['oid']
        operation = self.json_ok('status', opts={'uid': self.other_uid, 'oid': oid})
        assert operation['status'] == 'DONE'

        resp = self.json_ok('list', opts={'uid': self.other_uid, 'path': path})
        resp = resp[1:]  # выкидываем родительскую папку, возвращаемую листом
        assert len(resp) == len(album_dict['items'])
        zfill_len = len(str(len(album_dict['items'])))
        for i, item in enumerate(album_dict['items']):
            new_path = '%s/%s' % (path, os.path.split(item['path'])[-1])
            assert resp[i]['id'] == new_path

        resp = self.json_ok('async_public_album_save', opts={'public_key': public_key, 'uid': self.other_uid, 'item_id': my_file_id})
        assert resp['path'] == u'/disk/Загрузки/my_file.jpg'
        resp = self.json_ok('async_public_album_save', opts={'public_key': public_key, 'uid': self.other_uid, 'item_id': my_file_id})
        assert resp['path'] == u'/disk/Загрузки/my_file (1).jpg'

    def test_create_album_without_items_and_then_add_them(self):
        album_dict = deepcopy(self.ALBUM_CREATE_JSON)
        # выбрасываем items
        items = album_dict.pop('items')
        album = self.json_ok('albums_create_with_items', opts={'uid': self.uid}, json=album_dict)
        for k in ('title', 'layout', 'flags'):
            assert self.ALBUM_CREATE_JSON[k] == album[k], k
        assert 'items' in album
        assert len(album['items']) == 0

        # добавляем элементы
        album = self.json_ok('album_append_items', opts={'uid': self.uid, 'album_id': album['id']},
                             json={'items': items})
        assert len(album['items']) == len(items)

    def test_userinfo_in_album(self):
        album_dict = deepcopy(self.ALBUM_CREATE_JSON)
        album = self.json_ok('albums_create_with_items', opts={'uid': self.uid}, json=album_dict)
        album_id = album['id']
        public_key = album['public']['public_key']

        album = self.json_ok('album_get', opts={'uid': self.uid, 'album_id': album_id})
        assert 'user' in album
        assert 'login' in album['user']
        assert 'username' in album['user']
        assert 'public_name' in album['user']
        assert 'locale' in album['user']
        assert 'paid' in album['user']

        album = self.json_ok('public_album_get', opts={'public_key': public_key})
        assert 'user' in album
        assert 'login' in album['user']
        assert 'username' in album['user']
        assert 'public_name' in album['user']
        assert 'locale' in album['user']
        assert 'paid' in album['user']

        album = self.json_ok('album_get', opts={'uid': self.uid, 'album_id': album_id})
        assert 'user' in album
        assert 'login' in album['user']
        assert 'username' in album['user']
        assert 'public_name' in album['user']
        assert 'locale' in album['user']
        assert 'paid' in album['user']

    def test_title_dont_change_on_set_attr(self):
        album_dict = deepcopy(self.ALBUM_CREATE_JSON)
        first_album = self.json_ok('albums_create_with_items', opts={'uid': self.uid}, json=album_dict)

        # проверяем что title не меняется при изменении других атрибутов альбома
        album_id = first_album['id']
        album = self.json_ok('album_set_attr', opts={'uid': self.uid, 'album_id': album_id, 'description': 'Hello!'})
        assert album['title'] == first_album['title']

        album = self.json_ok('album_set_attr',
                             opts={'uid': self.uid, 'album_id': album_id, 'title': first_album['title']})
        assert album['title'] == first_album['title']

    def test_album_remove(self):
        album_dict = deepcopy(self.ALBUM_CREATE_JSON)
        album = self.json_ok('albums_create_with_items', opts={'uid': self.uid}, json=album_dict)
        album_id = album['id']

        resp = self.json_ok('album_remove', opts={'uid': self.uid, 'album_id': album_id})
        assert resp is None

        self.json_error('album_get', opts={'uid': self.uid, 'album_id': album_id}, code=codes.ALBUM_NOT_FOUND)

        items = list(AlbumItem.controller.filter(uid=self.uid, album_id=album_id))
        assert items == []

    def test_public_album_items_resources_preview(self):
        album_dict = deepcopy(self.ALBUM_CREATE_JSON)
        album = self.json_ok('albums_create_with_items', opts={'uid': self.uid}, json=album_dict)

        public_key = album['public']['public_key']

        public_album = self.json_ok('public_album_get', opts={'public_key': public_key, 'meta': ''})
        assert 'uid=0' in public_album['cover']['object']['meta']['preview']
        assert 'uid=0' in public_album['items'][0]['object']['meta']['preview']

    def test_album_items_order_index(self):
        album_dict = deepcopy(self.ALBUM_CREATE_JSON)
        album = self.json_ok('albums_create_with_items', opts={'uid': self.uid}, json=album_dict)
        album_id = album['id']

        assert int(album['items'][0]['order_index']) == album['items'][0]['object']['etime']
        assert int(album['items'][1]['order_index']) == album['items'][1]['object']['etime']

        album = self.json_ok('album_get', opts={'uid': self.uid, 'album_id': album_id})
        assert int(album['items'][0]['order_index']) == album['items'][0]['object']['etime']
        assert int(album['items'][1]['order_index']) == album['items'][1]['object']['etime']

        third_file_path = '/disk/third_file.jpg'
        self.upload_file(self.uid, third_file_path)
        item = self.json_ok('album_append_item',
                            opts={'uid': self.uid, 'album_id': album_id, 'type': 'resource', 'path': third_file_path})
        assert int(item['order_index']) == item['object']['etime']

        album = self.json_ok('album_get', opts={'uid': self.uid, 'album_id': album_id})
        assert int(album['items'][0]['order_index']) == album['items'][0]['object']['etime']
        assert int(album['items'][1]['order_index']) == album['items'][1]['object']['etime']
        assert int(album['items'][2]['order_index']) == album['items'][2]['object']['etime']

        # создаём альбом и добавляем элементы по одному
        album = self.json_ok('albums_create', opts={'uid': self.uid, 'title': 'test_order_items'})
        album_id = album['id']
        item = self.json_ok('album_append_item', opts={'uid': self.uid, 'album_id': album_id, 'type': 'resource',
                                                       'path': self.RESOURCE_PATH})
        assert int(item['order_index']) == item['object']['etime']
        item = self.json_ok('album_append_item', opts={'uid': self.uid, 'album_id': album_id, 'type': 'resource',
                                                       'path': self.SHARED_RESOURCE_PATH})
        assert int(item['order_index']) == item['object']['etime']
        item = self.json_ok('album_append_item', opts={'uid': self.uid, 'album_id': album_id, 'type': 'resource',
                                                       'path': third_file_path})
        assert int(item['order_index']) == item['object']['etime']
        album = self.json_ok('album_get', opts={'uid': self.uid, 'album_id': album_id})
        assert int(album['items'][0]['order_index']) == album['items'][0]['object']['etime']
        assert int(album['items'][1]['order_index']) == album['items'][1]['object']['etime']
        assert int(album['items'][2]['order_index']) == album['items'][2]['object']['etime']

    def test_cover_from_kicked_group(self):
        dir_path = '/disk/dir'
        file_path = '%s/%s' % (dir_path, 'file.jpg')
        self.json_ok('mkdir', {'uid': self.uid, 'path': dir_path})
        self.upload_file(self.uid, file_path)

        # приглашаем в группу и создаем альбом
        group = self.json_ok('share_create_group', opts={'uid': self.uid, 'path': dir_path})
        gid = group['gid']
        invite_hash = self.share_invite(group['gid'], self.other_uid)
        self.json_ok('share_activate_invite', opts={'uid': self.other_uid, 'hash': invite_hash})
        album_dict = deepcopy(self.ALBUM_CREATE_JSON)
        album_dict['items'] = [{'type': 'resource', 'path': file_path}]
        album = self.json_ok('albums_create_with_items', opts={'uid': self.other_uid}, json=album_dict)

        # есть обложка и элементы альбома
        album = self.json_ok('album_get', opts={'uid': self.other_uid, 'album_id': album['id']})
        assert album['cover'] is not None
        assert len(album['items']) == 1

        # выкидываем пользователя из группы - пропадает обложка и единственный
        # элемент альбома
        self.json_ok('share_kick_from_group', opts={'uid': self.uid, 'gid': gid, 'user_uid': self.other_uid})
        print 'KICK'
        album = self.json_ok('album_get', opts={'uid': self.other_uid, 'album_id': album['id']})
        assert album['cover'] is None
        assert len(album['items']) == 0

        # приглашаем обратно - обложка и элементы появляются
        group = self.json_ok('share_create_group', opts={'uid': self.uid, 'path': dir_path})
        invite_hash = self.share_invite(group['gid'], self.other_uid)
        self.json_ok('share_activate_invite', opts={'uid': self.other_uid, 'hash': invite_hash})

        album = self.json_ok('album_get', opts={'uid': self.other_uid, 'album_id': album['id']})
        assert album['cover'] is not None
        assert len(album['items']) == 1

    def test_public_album_get_amount(self):
        rw_shared_dir = '/disk/660'
        ro_shared_dir = '/disk/640'
        album_dict = deepcopy(self.ALBUM_CREATE_JSON)
        album_dict['items'] = []
        files_in_dir = 3

        groups = list()
        #for dir_name, rights in ((rw_shared_dir, 660), (ro_shared_dir, 640)):
        for dir_name, rights in ((rw_shared_dir, 660),):
            self.json_ok('mkdir', {'uid': self.uid, 'path': dir_name})
            for i in range(10):
                file_path = '%s/%i.jpg' % (dir_name, i)
                self.upload_file(self.uid, file_path)
                album_dict['items'].append({'type': 'resource', 'path': file_path})
            # расшариваем папку
            group = self.json_ok('share_create_group', opts={'uid': self.uid, 'path': dir_name})
            groups.append(group)
            # приглашаем тестового пользователя в расшаренную папку
            invite_hash = self.share_invite(group['gid'], self.other_uid, rights=rights)
            # подключаем расшареную папку тестовому пользователю
            self.json_ok('share_activate_invite', opts={'uid': self.other_uid, 'hash': invite_hash})

        self.json_ok('mkdir', {'uid': self.other_uid, 'path': '/disk/common'})
        for i in range(3):
            file_path = '%s/%i.jpg' % ('/disk/common', i)
            self.upload_file(self.other_uid, file_path)
            album_file = {'type': 'resource', 'path': file_path}
            if i == 0:
                album_dict['items'].insert(0, album_file)
            else:
                album_dict['items'].append(album_file)

        album = self.json_ok('albums_create_with_items', opts={'uid': self.other_uid}, json=album_dict)
        public_key = album['public']['public_key']

        # выкидываем пользователя из группы
        for group in groups:
            gid = group['gid']
            self.json_ok('share_kick_from_group', opts={'uid': self.uid, 'gid': gid, 'user_uid': self.other_uid})

        for i in range(10):
            public_album = self.json_ok('public_album_get', opts={'public_key': public_key, 'amount': i, 'uid': self.other_uid})
            assert len(public_album['items']) == min(i, 3)

    def test_public_album_order_index_after_move_last_element_to_the_middle(self):
        album_dict = deepcopy(self.ALBUM_CREATE_JSON)
        third_file_path = '/disk/third_file.jpg'
        self.upload_file(self.uid, third_file_path)
        album_dict['items'].append({'type': 'resource', 'path': third_file_path})

        album = self.json_ok('albums_create_with_items', opts={'uid': self.uid}, json=album_dict)
        album_id = album['id']
        public_key = album['public']['public_key']

        # получаем публичный альбом и проверяем что все индексы на месте
        public_album = self.json_ok('public_album_get', opts={'public_key': public_key})
        assert int(public_album['items'][0]['order_index']) == public_album['items'][0]['object']['etime']
        assert int(public_album['items'][1]['order_index']) == public_album['items'][1]['object']['etime']
        assert int(public_album['items'][2]['order_index']) == public_album['items'][2]['object']['etime']

        # перемещаем последний элемент в середину, так что его order_index из 3 превращается в 1.5
        # item = self.json_ok('album_item_move', opts={'uid': self.uid, 'item_id': public_album['items'][2]['id'],
        #                                              'to_index': 1})
        # assert item['order_index'] == 1.5
        #
        # # достаём публичный альбом и проверяем, что элементы отсортированы по order_index
        # public_album = self.json_ok('public_album_get', opts={'public_key': public_key,
        #                                                       'last_item_id': public_album['items'][0]['id']})
        # assert len(public_album['items']) == 2
        # assert int(public_album['items'][0]['order_index']) == 1.5
        #
        # public_album = self.json_ok('public_album_get', opts={'public_key': public_key,
        #                                                       'last_item_id': public_album['items'][0]['id']})
        # assert len(public_album['items']) == 1
        # assert public_album['items'][0]['order_index'] == 2

    def test_public_album_item_download_url(self):
        album_dict = deepcopy(self.ALBUM_CREATE_JSON)
        album = self.json_ok('albums_create_with_items', opts={'uid': self.uid}, json=album_dict)
        public_key = album['public']['public_key']
        item_id = album['items'][0]['id']

        # владелец получает ссылки
        opts = {'uid': self.uid, 'public_key': public_key, 'item_id': item_id}
        download_urls = self.json_ok('public_album_item_download_url', opts=opts)
        assert 'file' in download_urls
        assert 'digest' in download_urls
        assert 'disposition=attachment' in download_urls['file']
        assert 'disposition=attachment' in download_urls['digest']

        opts['inline'] = 1
        download_urls = self.json_ok('public_album_item_download_url', opts=opts)
        assert 'disposition=inline' in download_urls['file']
        assert 'disposition=inline' in download_urls['digest']

        # не владелец получает ссылки для публичного альбома
        opts['uid'] = self.other_uid
        download_urls = self.json_ok('public_album_item_download_url', opts=opts)
        assert 'file' in download_urls

        # uid отсутствует при получении ссылки для публичного альбома
        opts.pop('uid')
        download_urls = self.json_ok('public_album_item_download_url', opts=opts)
        assert 'file' in download_urls

        # не владелец не получает ссылки для непубличного альбома
        opts['uid'] = self.other_uid
        self.json_ok('album_unpublish', {'uid': self.uid, 'album_id': album['id']})
        self.json_error('public_album_item_download_url', opts=opts)

        # uid отсутствует. Непубличный альбом не отдается
        opts.pop('uid')
        self.json_error('public_album_item_download_url', opts=opts)

        # владелец получает ссылки для непубличного альбома
        opts['uid'] = self.uid
        download_urls = self.json_ok('public_album_item_download_url', opts=opts)
        assert 'file' in download_urls

    def test_album_item_info(self):
        album_dict = deepcopy(self.ALBUM_CREATE_JSON)
        album = self.json_ok('albums_create_with_items', opts={'uid': self.uid}, json=album_dict)
        album_id = album['id']
        item_id = album['items'][0]['id']

        item = self.json_ok('album_item_info', opts={'uid': self.uid, 'album_id': album_id, 'item_id': item_id, 'meta': 'preview'})
        assert 'object' in item

    def test_public_album_item_info(self):
        album_dict = deepcopy(self.ALBUM_CREATE_JSON)
        album = self.json_ok('albums_create_with_items', opts={'uid': self.uid}, json=album_dict)
        public_key = album['public']['public_key']
        item_id = album['items'][0]['id']

        opts = {'public_key': public_key, 'item_id': item_id, 'meta': 'preview'}
        item = self.json_ok('public_album_item_info', opts=opts)
        assert 'object' in item
        # для публичного альбома - публичные превью
        assert 'uid=0' in item['object']['meta']['preview']
        opts.pop('item_id')
        self.json_error('public_album_item_info', opts=opts)
        opts['item_id'] = '666'
        self.json_error('public_album_item_info', opts=opts)

        opts['item_id'] = item_id
        opts['uid'] = self.uid
        self.json_ok('album_unpublish', {'uid': self.uid, 'album_id': album['id']})
        item = self.json_ok('public_album_item_info', opts=opts)
        # для приватного альбома - приватные превью
        assert 'uid=%s' % self.uid in item['object']['meta']['preview']

    def test_public_album_search_bulk_info(self):
        album_dict = deepcopy(self.ALBUM_CREATE_JSON)
        album = self.json_ok('albums_create_with_items', opts={'uid': self.uid}, json=album_dict)
        file_ids = []
        search_results = {'hitsArray': [],
                          'hitsCount': 0}
        for item in album['items']:
            search_item = {'id': item['obj_id'],
                           'key': item['object']['path'],
                           'type': 'file',
                           'ctime': '1446749715',
                           'name': item['object']['name']}
            search_results['hitsArray'].append(search_item)
            search_results['hitsCount'] += 1
            file_ids.append(item['obj_id'])
        # append fake file_id to test filtering
        file_ids.append('abcde_fake_id')

        with mock.patch.object(SearchDB, 'open_url') as mock_obj:
            mock_obj.return_value = to_json(search_results)
            public_key = album['public']['public_key']
            opts = {'public_key': public_key,
                    'file_ids': ','.join(file_ids),
                    'search_meta': 'file_id,width,size',
                    'sort': 'size',
                    'order': 0
                    }
            self.json_ok('public_album_search_bulk_info', opts=opts)
            url_info = parse_open_url_call(mock_obj)
            # check that only valid number of file_ids present
            assert len(url_info['params']['id']) == len(file_ids) - 1
            assert url_info['params']['uid'][0] == self.uid

    def test_public_album_item_video_url(self):
        album_dict = deepcopy(self.ALBUM_CREATE_JSON)
        album = self.json_ok('albums_create_with_items', opts={'uid': self.uid}, json=album_dict)
        public_key = album['public']['public_key']
        item_id = album['items'][0]['id']

        opts = {'public_key': public_key, 'item_id': item_id}
        item = self.json_ok('public_album_item_video_url', opts=opts)
        assert 'host' in item
        assert 'stream' in item

    def test_album_id_for_items(self):
        """Проверка необязательного параметра album_id для ручек, работающих с элементами альбома"""
        # создаем два альбома с разными ресурсами
        albums = []
        for album_num in range(2):
            album_path = '/disk/album_%i' % album_num
            album_dict = deepcopy(self.ALBUM_CREATE_JSON)
            album_dict['items'] = []
            self.json_ok("mkdir", opts={'uid': self.uid, 'path': album_path})
            for file_num in range(2):
                file_path = "%s/%i.jpg" % (album_path, file_num)
                self.upload_file(self.uid, file_path)
                album_dict['items'].append({'type': 'resource', 'path': file_path})
            albums.append(self.json_ok('albums_create_with_items', opts={'uid': self.uid}, json=album_dict))

        tests = (
            (self.json_ok, 'album_item_check', {'path': '/disk/album_0/0.jpg'}),
            (self.json_ok, 'album_item_check', {'path': '/disk/album_0/0.jpg', 'album_id': albums[0]['id']}),
            (self.json_error, 'album_item_check', {'path': '/disk/album_0/0.jpg', 'album_id': albums[1]['id']}),
            (self.json_ok, 'album_item_info', {'item_id': albums[0]['items'][0]['id']}),
            (self.json_ok, 'album_item_info', {'item_id': albums[0]['items'][0]['id'], 'album_id': albums[0]['id']}),
            (self.json_error, 'album_item_info', {'item_id': albums[0]['items'][0]['id'], 'album_id': albums[1]['id']}),
            (self.json_ok, 'album_item_set_attr', {'item_id': albums[0]['items'][0]['id'], 'description': '12'}),
            (self.json_ok, 'album_item_set_attr', {'item_id': albums[0]['items'][0]['id'], 'album_id': albums[0]['id'], 'description': '12'}),
            (self.json_error, 'album_item_set_attr', {'item_id': albums[0]['items'][0]['id'], 'album_id': albums[1]['id'], 'description': '12'}),
            # (self.json_ok, 'album_item_move', {'item_id': albums[0]['items'][1]['id'], 'to_index': 0}),
            # (self.json_ok, 'album_item_move', {'item_id': albums[0]['items'][1]['id'], 'album_id': albums[0]['id'], 'to_index': 0}),
            (self.json_error, 'album_item_move', {'item_id': albums[0]['items'][1]['id'], 'album_id': albums[1]['id'], 'to_index': 0}),
            (self.json_ok, 'album_item_remove', {'item_id': albums[1]['items'][1]['id']}),
            (self.json_ok, 'album_item_remove', {'item_id': albums[0]['items'][1]['id'], 'album_id': albums[0]['id']}),
            (self.json_error, 'album_item_remove', {'item_id': albums[0]['items'][0]['id'], 'album_id': albums[1]['id']}),
        )

        for test in tests:
            test[2]['uid'] = self.uid
            print "Check method: %22.20s, expect: %10.10s. Args: %s" % (test[1], test[0].__name__, test[2])
            test[0](test[1], test[2])

    def test_public_album_resources_list_autosuffix(self):
        album_dict = {'items': []}
        for i in range(3):
            dir_path = "/disk/%i" % i
            file_path = "%s/1.jpg" % dir_path
            self.json_ok('mkdir', {'uid': self.uid, 'path': dir_path})
            self.upload_file(self.uid, file_path)
            album_dict['items'].append({'type': 'resource', 'path': file_path})
        album = self.json_ok('albums_create_with_items', opts={'uid': self.uid}, json=album_dict)
        pk = album['public']['public_key']
        fulltree = self.json_ok('public_album_resources_list', opts={'uid': album['uid'], 'public_key': pk, 'meta': 'file_mid,size,mimetype'})
        for i, item in enumerate(fulltree['list']):
            if i == 0:
                assert item['this']['name'] == '1.jpg'
                assert item['this']['path'] == '/1.jpg'
            else:
                assert item['this']['name'] == '1 (%i).jpg' % i
                assert item['this']['path'] == '/1 (%i).jpg' % i

    def test_public_album_resources_list(self):
        albums_dicts = []
        for album_num in range(2):
            album_dict = {'items': []}
            for file_num in range(2):
                file_path = "/disk/%i.jpg" % file_num
                self.upload_file(self.uid, file_path)
                album_dict['items'].append({'type': 'resource', 'path': file_path})
            albums_dicts.append(album_dict)

        # проверяем для альбома без вложенных альбомов
        album = self.json_ok('albums_create_with_items', opts={'uid': self.uid}, json=albums_dicts[0])
        pk = album['public']['public_key']
        fulltree = self.json_ok('public_album_resources_list', opts={'uid': album['uid'], 'public_key': pk, 'meta': 'file_mid,size,mimetype'})
        assert not fulltree.viewkeys() ^ {'this', 'list'}
        assert fulltree['this']['path'] == '/'
        assert fulltree['this']['type'] == 'dir'
        assert fulltree['this']['name'] == album['title']
        assert len(fulltree['list']) == 2 # два элемента у альбома
        for i, item in enumerate(fulltree['list']):
            assert not {'meta', 'type', 'name', 'path'} - item['this'].viewkeys()
            assert not item['list']
            assert item['this']['name'] == album['items'][i]['object']['name']
            assert item['this']['path'] == '/' + album['items'][i]['object']['name']

        # проверям альбом с вложенным альбомом
        albums_dicts[1]['items'].append({'type': 'album', 'album_id': album['id']})
        album = self.json_ok('albums_create_with_items', opts={'uid': self.uid}, json=albums_dicts[1])
        pk = album['public']['public_key']
        fulltree = self.json_ok('public_album_resources_list', opts={'uid': album['uid'], 'public_key': pk, 'meta': 'file_mid,size,mimetype'})
        assert fulltree['this']['name'] == album['title']
        assert len(fulltree['list']) == 3 # три элемента у альбома
        album_item = fulltree['list'][0]
        assert album_item['this']['path'] == '/%s' % album['items'][0]['object']['title']
        assert album_item['this']['type'] == 'dir'
        assert album_item['this']['name'] == album['items'][0]['object']['title']
        assert len(album_item['list']) == 2

        # обработка рекурсий
        self.json_ok('album_append_item', opts={'uid': self.uid, 'album_id': album_item['this']['id'],
                                                'type': 'album', 'src_album_id': album['id']})
        self.json_ok('public_album_resources_list', opts={'uid': album['uid'], 'public_key': pk, 'meta': 'file_mid,size,mimetype'})

    def test_public_album_download_url(self):
        album_dict = {'items': []}
        for i in range(2):
            file_path = '/disk/%i.jpg' % i
            self.upload_file(self.uid, file_path)
            album_dict['items'].append({'type': 'resource', 'path': file_path})

        album = self.json_ok('albums_create_with_items', opts={'uid': self.uid}, json=album_dict)
        public_key = album['public']['public_key']
        encoded_public_key = base64.b64encode(public_key)

        url = self.json_ok('public_album_download_url', opts={'uid': self.uid, 'public_key': public_key})['url']
        parsed_url = urlparse(url)
        parsed_params = parse_qs(parsed_url.query.encode('utf-8'))
        assert parsed_url.path.startswith('/zip-album')
        assert parsed_url.path.endswith('/%s' % encoded_public_key)
        assert parsed_params['hash'][0] == public_key
        assert parsed_params['uid'][0] == self.uid
        assert parsed_params['filename'][0].decode('utf-8') == u'%s.zip' % album['title']
        assert parsed_params['disposition'][0] == 'attachment'

        url = self.json_ok('public_album_download_url', opts={'public_key': public_key})['url']
        parsed_url = urlparse(url)
        parsed_params = parse_qs(parsed_url.query.encode('utf-8'))
        assert parsed_url.path.startswith('/zip-album')
        assert parsed_url.path.endswith('/%s' % encoded_public_key)
        assert parsed_params['hash'][0] == public_key
        assert parsed_params['filename'][0].decode('utf-8') == u'%s.zip' % album['title']
        assert parsed_params['disposition'][0] == 'attachment'
        assert parsed_params['uid'][0] == '0'

    def test_album_as_cover(self):
        album_dict = {'items': []}
        file_path = '/disk/1.jpg'
        self.upload_file(self.uid, file_path)
        for i in range(2):
            album = self.json_ok('albums_create_with_items', opts={'uid': self.uid}, json={})
            album_item = {"type": "album", "album_id": album['id']}
            album_dict['items'].append(album_item)
        album = self.json_ok('albums_create_with_items', opts={'uid': self.uid}, json=album_dict)
        assert album['cover'] is None
        albums = self.json_ok('albums_list', opts={'uid': self.uid, 'meta': ''})
        assert albums[0]['cover'] is None
        self.json_ok('album_append_item', opts={'uid': self.uid, 'album_id': album['id'], 'type': 'resource', 'path': file_path})
        self.json_ok('albums_list', opts={'uid': self.uid})
        album = self.json_ok('album_get', opts={'uid': self.uid, 'album_id': album['id']})
        assert album['cover'] is not None

    def test_select_new_cover_on_public_album_get(self):
        album_dict = {'items': []}
        for i in range(2):
            file_path = '/disk/%i.jpg' % i
            self.upload_file(self.uid, file_path, file_data={'mtime': 100 + i})
            album_dict['items'].append({'type': 'resource', 'path': file_path})

        album = self.json_ok('albums_create_with_items', opts={'uid': self.uid}, json=album_dict)
        min_index, _ = min(enumerate(album['items']), key=lambda (_, item): item['object']['etime'])
        public_key = album['public']['public_key']
        album = self.json_ok('public_album_get', opts={'uid': 0, 'public_key': public_key})
        assert album['cover']['object']['path'] == '/disk/%i.jpg' % min_index
        # после удаления обложки должны ее перевыбрать даже при публичном просмотре
        self.json_ok('rm', opts={'uid': self.uid, 'path': album['cover']['object']['path']})
        album = self.json_ok('public_album_get', opts={'public_key': public_key})
        assert album['cover']['object']['path'] == '/disk/1.jpg'

    def test_select_new_cover_when_album_is_empty_and_we_request_albums_list(self):
        album_dict = {'items': []}
        for i in range(24):  # меньше 24 не воспроизводится
            file_path = '/disk/%i.jpg' % i
            self.upload_file(self.uid, file_path, file_data={'mtime': 100 + i})
            album_dict['items'].append({'type': 'resource', 'path': file_path})

        album = self.json_ok('albums_create_with_items', opts={'uid': self.uid}, json=album_dict)
        public_key = album['public']['public_key']
        album = self.json_ok('public_album_get', opts={'uid': 0, 'public_key': public_key})
        min_index, min_item = min(enumerate(album['items']), key=lambda (_, item): item['object']['etime'])
        assert album['cover']['object']['path'] == '/disk/%i.jpg' % min_index

        # после удаления обложки должны ее перевыбрать даже при публичном просмотре
        for item in album_dict['items']:
            self.json_ok('rm', opts={'uid': self.uid, 'path': item['path']})
        albums = self.json_ok('albums_list', opts={'uid': self.uid})
        # Если ошибка, то тут будет выброшено исключение OverflowError: signed integer is greater than maximum

    def test_social_cover_mime_type(self):
        album = self.json_ok('albums_create_with_items', opts={'uid': self.uid}, json=self.ALBUM_CREATE_JSON)
        assert 'content_type=image%2Fjpeg' in album['social_cover_url']

    def test_broken_public_section(self):
        """
        https://st.yandex-team.ru/CHEMODAN-26072
        """
        album_dict = {'items': []}
        album = self.json_ok('albums_create_with_items', opts={'uid': self.uid}, json=album_dict)

        # эмулируем, что не было второго сохранения при создании альбома
        import mpfs.engine.process
        albums_coll = mpfs.engine.process.dbctl().database()['albums']
        albums_coll.update({'uid': self.uid}, {'$unset': {'public_url': 1, 'short_url': 1}})

        album = self.json_ok('album_get', opts={'uid': self.uid, 'album_id': album['id']})
        assert not album['public'].viewkeys() ^ {'public_key', 'public_url', 'views_count', 'short_url'}

    @pytest.mark.skipif(True, reason='https://st.yandex-team.ru/CHEMODAN-32396')
    def test_public_album_items_list_decode_custom_properties(self):
        """Протестировать что обрабатывается параметр `decode_custom_properties`
        """
        album_dict = deepcopy(self.ALBUM_CREATE_JSON)
        album = self.json_ok('albums_create_with_items', opts={'uid': self.uid}, json=album_dict)
        public_key = album['public']['public_key']

        json_object = json.loads('{"a": 1}')
        self.json_ok('setprop', {
            'uid': self.uid,
            'path': self.RESOURCE_PATH,
            'custom_properties': json_object
        })

        items = self.json_ok('public_album_items_list', {'public_key': public_key, 'meta': 'custom_properties', 'decode_custom_properties': 1})
        assert json_object == next((
            i['object']['meta']['custom_properties'] for i in items
            if i['object']['path'] == self.RESOURCE_PATH
        ))

    @pytest.mark.skipif(True, reason='https://st.yandex-team.ru/CHEMODAN-32396')
    def test_public_album_item_info_decode_custom_properties(self):
        """Протестировать что ручка обрабатывает флаг `decode_custom_properties`
        """

        album_dict = deepcopy(self.ALBUM_CREATE_JSON)
        album = self.json_ok('albums_create_with_items', opts={'uid': self.uid}, json=album_dict)

        json_object = json.loads('{"a": 1}')
        self.json_ok('setprop', {
            'uid': self.uid,
            'path': self.RESOURCE_PATH,
            'custom_properties': json_object
        })

        public_key = album['public']['public_key']
        item_id = album['items'][0]['id']
        item = self.json_ok('public_album_item_info', {
            'public_key': public_key,
            'item_id': item_id,
            'meta': 'custom_properties',
            'decode_custom_properties': 1
        })
        assert json_object == item['object']['meta']['custom_properties']

    def test_albums_out_of_photounlim_files(self):
        self.json_ok('enable_unlimited_autouploading', {'uid': self.uid})
        photounlim_path = '/photounlim/test.jpg'
        self.upload_file(self.uid, photounlim_path)
        json_body = {
            'title': 'MyAlbum',
            'description': 'My Test Album',
            'flags': ['show_frames'],
            'items': [
                {'type': 'resource', 'path': self.RESOURCE_PATH},
                {'type': 'resource', 'path': photounlim_path}
            ],
        }
        album = self.json_ok('albums_create_with_items', opts={'uid': self.uid}, json=json_body)
        assert len(album['items']) == 2

        resp = self.json_ok('album_get', opts={'uid': self.uid, 'album_id': album['id']})
        assert len(resp['items']) == 2

    def test_add_photounlim_file_to_album(self):
        self.json_ok('enable_unlimited_autouploading', {'uid': self.uid})
        album_dict = deepcopy(self.ALBUM_CREATE_JSON)

        album = self.json_ok('albums_create_with_items', opts={'uid': self.uid}, json=album_dict)
        prev_len = len(album['items'])

        photounlim_path = '/photounlim/test.jpg'
        self.upload_file(self.uid, photounlim_path)
        items = [{'type': 'resource', 'path': photounlim_path},]
        self.json_ok('album_append_items', opts={'uid': self.uid, 'album_id': album['id']}, json={'items': items})

        album = self.json_ok('album_get', opts={'uid': self.uid, 'album_id': album['id']})
        assert len(album['items']) == prev_len + 1

    def test_album_video_streams(self):
        client_id = '123'
        album_dict = deepcopy(self.ALBUM_CREATE_JSON)

        file_path = '/disk/1.avi'
        self.upload_file(self.uid, file_path)

        album_dict['items'].append({'type': 'resource', 'path': file_path})
        album = self.json_ok('albums_create_with_items', opts={'uid': self.uid}, json=album_dict)

        item_id = [x['id'] for x in album['items'] if x['object']['path'] == file_path][0]
        IP = '198.168.1.1'

        file_mid = self.json_ok('info', {'uid': self.uid, 'path': file_path, 'meta': 'file_mid'})['meta']['file_mid']
        with VideoStreamingStub() as stub:
            resp = self.json_ok('album_video_streams', {'uid': self.uid, 'user_ip': IP, 'album_id': album['id'], 'item_id': item_id, 'client_id': client_id})
            assert resp == VideoStreamingStub._video_info_common_response
            stub.get_video_info.assert_called_with(self.uid, file_mid, use_http=False, user_ip=IP, is_public=False, client_id=client_id)
            self.json_ok('album_video_streams', {'uid': self.uid, 'use_http': 1, 'album_id': album['id'], 'item_id': item_id})
            stub.get_video_info.assert_called_with(self.uid, file_mid, use_http=True, user_ip='', is_public=False, client_id='')

    def test_cover_change(self):
        album_dict = {'items': []}
        for i in range(4):
            path = '/disk/%i.jpg' % i
            self.upload_file(self.uid, path)
            album_dict['items'].append({'type': 'resource', 'path': path})
        album = self.json_ok('albums_create_with_items', opts={'uid': self.uid}, json=album_dict)
        new_cover_id = album['items'][3]['id']
        album_with_new_cover = self.json_ok('album_set_attr', {
            'uid': self.uid,
            'album_id': album['id'],
            'cover': new_cover_id,
        })
        assert album_with_new_cover['cover']['id'] == new_cover_id

    def test_public_album_items_regenerate_preview(self):
        album_dict = deepcopy(self.ALBUM_CREATE_JSON)
        album = self.json_ok('albums_create_with_items', opts={'uid': self.uid}, json=album_dict)
        public_key = album['public']['public_key']
        with mock.patch('mpfs.core.filesystem.resources.disk.MPFSFile.print_to_listing_log') as preview_stub:
            public_album = self.json_ok('public_album_get', opts={'public_key': public_key, 'meta': ''})
            assert preview_stub.call_count == len(public_album['items'])

    def test_find_in_favorites(self):
        # Prepare favorite album
        album_id = ObjectId()
        AlbumDAO().insert({'_id': album_id,
                           'uid': self.uid,
                           'title': 'My best raccoons',
                           'album_type': AlbumType.FAVORITES.value})

        existing_file_ids = []
        expected_item_ids = []
        for i in range(10):
            path = '/disk/%s.jpg' % i
            self.upload_file(self.uid, path)
            file_id = self.json_ok('info', {'uid': self.uid, 'path': path, 'meta': 'file_id'})['meta']['file_id']
            existing_file_ids.append(file_id)
            item_id = ObjectId()
            expected_item_ids.append(str(item_id))

            AlbumItemDAO().insert({'_id': item_id,
                                   'uid': self.uid,
                                   'album_id': album_id,
                                   'obj_id': file_id,
                                   'order_index': i + 1,
                                   'obj_type': 'resource'})
        # END of prepare-phase
        non_existing_file_ids = ['12' * 31 + '0' + str(i)
                                 for i in range(7)]
        full_file_ids = existing_file_ids + non_existing_file_ids
        full_resource_ids = {'resource_ids': ['%s:%s' % (self.uid, fid) for fid in full_file_ids]}
        result = self.json_ok('album_find_in_favorites', opts={'uid': self.uid}, json=full_resource_ids)

        assert 'album_id' in result
        assert result['album_id'] == str(album_id)

        assert 'items' in result
        actual_file_ids = [item['resource_id']
                           for item in result['items']]
        assert_that(actual_file_ids, contains_inanyorder(*['%s:%s' % (self.uid, existing_file_id)
                                                           for existing_file_id in existing_file_ids]))
        for non_existing_file_id in non_existing_file_ids:
            assert_that(actual_file_ids, is_not(has_item('%s:%s' % (self.uid, non_existing_file_id))))

        actual_item_ids = [item['item_id']
                           for item in result['items']]
        assert_that(actual_item_ids, contains_inanyorder(*expected_item_ids))

    def test_find_shared_item_in_favorites(self):
        """Добавленные из ОП ресурсы должны находится ручкой album_find_in_favorites"""
        # Prepare favorite album
        album_id = ObjectId()
        AlbumDAO().insert({'_id': album_id,
                           'uid': self.uid,
                           'title': 'Alien raccoons',
                           'album_type': AlbumType.FAVORITES.value})

        # Add shared file
        self.json_ok('album_append_items',
                     opts={'uid': self.uid,
                           'album_id': str(album_id)},
                     json={'items': [{'type': 'resource', 'path': self.SHARED_RESOURCE_PATH}]})

        shared_file_resource_id = self.json_ok(
            'info',
            {'uid': self.uid, 'path': self.SHARED_RESOURCE_PATH, 'meta': 'resource_id'}
        )['meta']['resource_id']

        result = self.json_ok('album_find_in_favorites',
                              opts={'uid': self.uid},
                              json={'resource_ids': [shared_file_resource_id]})

        assert len(result['items']) == 1
        assert result['items'][0]['resource_id'] == shared_file_resource_id

    @parameterized.expand([('too_many_items', ['0' * 64 for _ in range(101)]),
                           ('wrong_resource_id_format', ['0' * 7])])
    def test_find_in_favorites_negative(self, case_name, file_ids):
        full_resource_ids = {'resource_ids': ['%s:%s' % (self.uid, fid) for fid in file_ids]}
        self.json_error('album_find_in_favorites', opts={'uid': self.uid}, json=full_resource_ids, status=400)

    @parameterized.expand([
        (AlbumType.PERSONAL.value, False, False),
        (AlbumType.PERSONAL.value, True, True),
        (AlbumType.FAVORITES.value, False, False),
        (AlbumType.FAVORITES.value, True, False),
    ])
    def test_create_with_items_is_public(self, album_type, is_public, result):
        filenames = ('with_etime_1.jpg', 'with_etime_2.jpg')
        resource_paths = ['/disk/' + filename for filename in filenames]
        for filename, resource_path in zip(filenames, resource_paths):
            self.upload_file(self.uid, resource_path)

        album_dict = {
            'description': 'My Test Album',
            'layout': 'rows',
            'flags': ['show_frames'],
            'items': [{'type': 'resource', 'path': resource_path} for resource_path in resource_paths],
            'album_type': album_type,
            'is_public': is_public,
        }
        album = self.json_ok('albums_create_with_items', opts={'uid': self.uid}, json=album_dict)
        assert album.get('is_public', False) == result

    @parameterized.expand([
        (False, False),
        (True, True),
    ])
    def test_geo_album_creation_enabled_for_new_user(self, geo_for_new_users_enabled, should_create_revision):
        not_initialized_uid = '100500100500'
        with mock.patch('mpfs.core.user.common.FEATURE_TOGGLES_CREATE_GEO_ALBUMS_FOR_NEW_USERS',
                        geo_for_new_users_enabled):
            self.create_user(not_initialized_uid)

        user = User(not_initialized_uid)
        assert should_create_revision == user.has_geo_albums()

    @parameterized.expand([
        (False, True),
        (True, False),
    ])
    def test_geo_album_creation_task_submit(self, user_has_geo_albums, should_submit_task):
        not_initialized_uid = '100500100500'
        with mock.patch('mpfs.core.user.common.FEATURE_TOGGLES_CREATE_GEO_ALBUMS_FOR_NEW_USERS',
                        user_has_geo_albums):
            self.create_user(not_initialized_uid)

        with mock.patch('mpfs.core.albums.interface.FEATURE_TOGGLES_CREATE_GEO_ALBUMS_FOR_WEB_USERS', True), \
                mock.patch('mpfs.core.queue.mpfs_queue.put') as mock_obj:
            self.json_ok('albums_list', {'uid': not_initialized_uid, 'album_type': 'geo'})

            for call in mock_obj.call_args_list:
                if call[0][-1] == 'create_geo_albums_operation':
                    if should_submit_task:
                        break
            else:
                if should_submit_task:
                    self.fail('expected album creation task submit: ' + str(should_submit_task))

    def test_favorites_cover_reselect(self):
        opts = {'uid': self.uid, 'album_type': AlbumType.FAVORITES.value}
        album_items = []
        for i in range(2):
            path = '/disk/%i.jpg' % i
            self.upload_file(self.uid, path, file_data={'mtime': 100 + i})
            album_items.append({'type': 'resource', 'path': path})

        album = self.json_ok('albums_create', opts)
        assert album['cover'] is None
        opts['album_id'] = album['id']

        def get_cover():
            return self.json_ok('album_get', opts)['cover']['object']['name']

        self.json_ok('album_append_item', dict(opts, **album_items[0]))
        assert get_cover() == '0.jpg'

        # 1.jpg новее, поэтому заменяет обложку
        item_1_jpg_id = self.json_ok('album_append_item', dict(opts, **album_items[1]))['id']
        assert get_cover() == '1.jpg'

        # обложка возвращается к 0.jpg
        self.json_ok('album_item_remove', dict(opts, item_id=item_1_jpg_id))
        assert get_cover() == '0.jpg'


class AlbumsNameGenerationTestCase(AlbumsBaseTestCase):

    CURRENT_TIME = 1536840595  # 2018-09-13 15:09:55

    FILES_DATA = {
        'with_etime_1.jpg': {
            'etime': '1536740595',  # 2018-09-12T11:23:15Z
            'mtime': '1536540595',
        },
        'with_etime_2.jpg': {
            'etime': '1536740595',  # 2018-09-12T11:23:15Z
            'mtime': '1536540595',
        },
        'with_etime_3.jpg': {
            'etime': '1536940583',  # '2018-09-14T18:56:35Z'
            'mtime': '1536540595',
        },
        'without_etime_3.jpg': {
            'mtime': '1536540595',
        },
    }

    @parameterized.expand([
        (('with_etime_1.jpg', 'with_etime_2.jpg', 'without_etime_3.jpg'), AlbumType.PERSONAL.value,
         u'Фоточки, мемасики', u'Фоточки, мемасики'),
        (('with_etime_1.jpg',), AlbumType.PERSONAL.value, None, u'12 сентября 2018'),
        (('with_etime_1.jpg', 'with_etime_2.jpg'), AlbumType.PERSONAL.value, None, u'12 сентября 2018'),
        (('with_etime_1.jpg', 'with_etime_3.jpg'), AlbumType.PERSONAL.value, None, u'12 сентября 2018 – 14 сентября 2018'),
        (('with_etime_1.jpg', 'without_etime_3.jpg'), AlbumType.PERSONAL.value, None, u'10 сентября 2018 – 12 сентября 2018'),
        (('with_etime_1.jpg', 'without_etime_3.jpg'), AlbumType.FAVORITES.value, None, u'Избранное'),
        (('with_etime_1.jpg', 'without_etime_3.jpg'), AlbumType.FAVORITES.value, u'10 сентября 2018 – 12 сентября 2018', u'Избранное'),
    ])
    def test_albums_create_with_items_without_title(self, filenames, album_type, title, expected_title):
        resource_paths = ['/disk/' + filename for filename in filenames]
        for filename, resource_path in zip(filenames, resource_paths):
            self.upload_file(self.uid, resource_path, opts=self.FILES_DATA[filename])

        album_dict = {
            'description': 'My Test Album',
            'layout': 'rows',
            'flags': ['show_frames'],
            'items': [{'type': 'resource', 'path': resource_path} for resource_path in resource_paths],
            'album_type': album_type,
        }

        if title:
            album_dict['title'] = title

        album = self.json_ok('albums_create_with_items', opts={'uid': self.uid}, json=album_dict)
        assert album['title'] == expected_title

        if album_type != AlbumType.FAVORITES.value:
            # повторно создаем альбом с теми же параметрами, название должно остаться таким же
            album = self.json_ok('albums_create_with_items', opts={'uid': self.uid}, json=album_dict)
            assert album['title'] == "%s" % expected_title

    @parameterized.expand([
        (('with_etime_1.jpg', 'with_etime_2.jpg', 'without_etime_3.jpg'), AlbumType.PERSONAL.value,
         u'Фоточки, мемасики', u'Фоточки, мемасики'),
        (('with_etime_1.jpg',), AlbumType.PERSONAL.value, None, u'12 сентября 2018'),
        (('with_etime_1.jpg', 'with_etime_2.jpg'), AlbumType.PERSONAL.value, None, u'12 сентября 2018'),
        (('with_etime_1.jpg', 'with_etime_3.jpg'), AlbumType.PERSONAL.value, None, u'12 сентября 2018 – 14 сентября 2018'),
        (('with_etime_1.jpg', 'without_etime_3.jpg'), AlbumType.PERSONAL.value, None, u'10 сентября 2018 – 12 сентября 2018'),
        (('with_etime_1.jpg', 'without_etime_3.jpg'), AlbumType.FAVORITES.value, None, u'Избранное'),
        (('with_etime_1.jpg', 'without_etime_3.jpg'), AlbumType.FAVORITES.value,
         u'10 сентября 2018 – 12 сентября 2018', u'Избранное'),
    ])
    def test_albums_create_from_folder_without_title(self, filenames, album_type, title, expected_title):

        folder_path = '/disk/test_folder'

        self.json_ok('mkdir', {'uid': self.uid, 'path': folder_path})

        resource_paths = [folder_path + '/' + filename for filename in filenames]
        for filename, resource_path in zip(filenames, resource_paths):
            self.upload_file(self.uid, resource_path, opts=self.FILES_DATA[filename])

        opts = {'uid': self.uid, 'path': folder_path, 'album_type': album_type}

        if title:
            opts['title'] = title

        album = self.json_ok('albums_create_from_folder', opts)
        assert album['title'] == expected_title

        if album_type != AlbumType.FAVORITES.value:
            # повторно создаем альбом с теми же параметрами, название должно остаться таким же
            album = self.json_ok('albums_create_from_folder', opts)
            assert album['title'] == "%s" % expected_title

    def test_empty_albums_create_without_title(self):
        with mock.patch('time.time', return_value=self.CURRENT_TIME):
            album1 = self.json_ok('albums_create', {'uid': self.uid})
            album2 = self.json_ok('albums_create', {'uid': self.uid})
        assert album1['title'] == album2['title'] == u'13 сентября 2018'

    def test_albums_create_with_cover_without_title(self):
        filename = 'with_etime_1.jpg'
        cover_path = '/disk/' + filename
        self.upload_file(self.uid, cover_path, opts=self.FILES_DATA[filename])

        album1 = self.json_ok('albums_create', {'uid': self.uid, 'cover': cover_path})
        album2 = self.json_ok('albums_create', {'uid': self.uid, 'cover': cover_path})
        assert album1['title'] == album2['title'] == u'12 сентября 2018'

    def test_albums_bad_title(self):
        title = 'very-' * 100 + 'long album title'
        filename = 'with_etime_1.jpg'
        resource_path = '/disk/%s' % filename
        self.upload_file(self.uid, resource_path, opts=self.FILES_DATA[filename])

        album_dict = {
            'items': [{'type': 'resource', 'path': resource_path}, ],
            'title': title
        }
        self.json_error('albums_create_with_items', opts={'uid': self.uid}, json=album_dict,
                        code=codes.ALBUM_TITLE_TOO_LONG)

    @parameterized.expand([
        ('ru', u'Избранное', None),
        ('en', u'Favorites', None),
        ('tr', u'Seçilenler', None),
        ('uk', u'Вибрані', None),
        ('ru', u'Избранное', 'test'),
        ('en', u'Favorites', 'test'),
        ('tr', u'Seçilenler', 'test'),
        ('uk', u'Вибрані', 'test'),
    ])
    def test_favorites_album_locale(self, locale, expected_title, title):
        uid = self.user_3.uid
        self.json_ok('user_init', {'uid': uid, 'locale': locale})

        opts = {'uid': uid, 'album_type': AlbumType.FAVORITES.value}
        if title:
            opts['title'] = title
        album = self.json_ok('albums_create', opts=opts, json={'items': []})
        assert album['title'] == expected_title


class AlbumsGeneratesOnceTestCase(AlbumsBaseTestCase):

    FILES_DATA = {
        'with_etime_1.jpg': {
            'etime': '1536740595',  # 2018-09-12T11:23:15Z
            'mtime': '1536540595',
        },
        'with_etime_2.jpg': {
            'etime': '1536740595',  # 2018-09-12T11:23:15Z
            'mtime': '1536540595',
        },
        'with_etime_3.jpg': {
            'etime': '1536940583',  # '2018-09-14T18:56:35Z'
            'mtime': '1536540595',
        },
        'without_etime_3.jpg': {
            'mtime': '1536540595',
        },
    }

    def test_favorites_album_generates_once_albums_create(self):
        album_dict = {'items': []}
        opts = {'uid': self.uid,
                'album_type': AlbumType.FAVORITES.value}
        self.json_ok('albums_create', opts=opts, json=album_dict)
        self.json_error('albums_create', opts=opts, json=album_dict,
                        code=codes.ALBUM_ALREADY_EXISTS, status=409)

    def test_favorites_album_generates_once_albums_create_from_folder(self):
        album_dict = {'items': []}
        folder_path = '/disk/test_folder'
        filenames = ('with_etime_1.jpg', 'with_etime_2.jpg', 'without_etime_3.jpg')
        self.json_ok('mkdir', {'uid': self.uid, 'path': folder_path})
        resource_paths = [folder_path + '/' + filename for filename in filenames]
        for filename, resource_path in zip(filenames, resource_paths):
            self.upload_file(self.uid, resource_path, opts=self.FILES_DATA[filename])

        opts = {'uid': self.uid,
                'album_type': AlbumType.FAVORITES.value,
                'path': folder_path}
        self.json_ok('albums_create_from_folder', opts=opts, json=album_dict)
        self.json_error('albums_create_from_folder', opts=opts, json=album_dict,
                        code=codes.ALBUM_ALREADY_EXISTS, status=409)

    def test_favorites_album_generates_once_albums_create_with_items(self):
        album_dict = {'items': []}
        opts = {'uid': self.uid,
                'album_type': AlbumType.FAVORITES.value}
        with PushServicesStub():
            self.json_ok('albums_create_with_items', opts=opts, json=album_dict)
            self.json_error('albums_create_with_items', opts=opts, json=album_dict,
                            code=codes.ALBUM_ALREADY_EXISTS, status=409)

    def test_favorites_album_change_layout(self):
        album_dict = {'items': []}
        opts = {'uid': self.uid,
                'album_type': AlbumType.FAVORITES.value}
        with PushServicesStub():
            album = self.json_ok('albums_create_with_items', opts=opts, json=album_dict)
            self.json_error('albums_create_with_items', opts=opts, json=album_dict,
                            code=codes.ALBUM_ALREADY_EXISTS, status=409)
            album = self.json_ok('album_set_attr',
                                 opts={'uid': self.uid, 'album_id': album['id'], 'layout': 'squares'})
            assert album['layout'] == 'squares'


class AlbumFacesTestCase(AlbumsBaseTestCase):
    RESOURCE_PATH = '/disk/%s.jpg'

    ALBUM_CREATE_JSON = {
        'title': 'MyAlbum',
        'description': 'My Test Album',
        'layout': 'rows',
        'cover': 0,
    }
    SQL_UPDATE_ALBUM = """
    UPDATE disk.albums
    SET album_type=:album_type
    WHERE id=:id
    """

    class AlbumItemDummy(object):
        def __init__(self, face_info):
            self.face_info = face_info

    def test_albums_list_with_pagination(self):
        items = []
        albums_ids = []
        session = Session.create_from_uid(self.uid)

        for i in range(10):
            file_path = self.RESOURCE_PATH % i
            self.upload_file(self.uid, file_path, file_data={'mtime': 100 + i})
            items.append({'type': 'resource', 'path': file_path})
            album_dict = deepcopy(self.ALBUM_CREATE_JSON)
            album_dict["items"] = items[:]
            album = self.json_ok('albums_create_with_items', opts={'uid': self.uid}, json=album_dict)

            session.execute(self.SQL_UPDATE_ALBUM, {'album_type': AlbumType.FACES.value, 'id': "\\x%s" % album['id']})
            albums_ids.append(album['id'])

        time.sleep(1)
        all_albums = []
        albums = self.json_ok('albums_list', opts={'uid': self.uid, 'amount': 4, 'album_type': AlbumType.FACES.value})
        assert len(albums) == 4
        assert [i['items_count'] for i in albums] == [10, 9, 8, 7]
        all_albums += albums
        albums = self.json_ok('albums_list', opts={'uid': self.uid, 'amount': 4, 'offset': 4,
                                                       'album_type': AlbumType.FACES.value})
        assert len(albums) == 4
        assert [i['items_count'] for i in albums] == [6, 5, 4, 3]
        all_albums += albums
        albums = self.json_ok('albums_list', opts={'uid': self.uid, 'amount': 4, 'offset': 8,
                                                       'album_type': AlbumType.FACES.value})
        assert len(albums) == 2
        assert [i['items_count'] for i in albums] == [2, 1]
        all_albums += albums
        # альбомы c большим кол-вом элементов идут в начале
        assert albums_ids[::-1] == [i['id'] for i in all_albums]

    def test_albums_list_without_pagination(self):
        items = []
        albums_ids = []
        session = Session.create_from_uid(self.uid)

        for i in range(5):
            file_path = self.RESOURCE_PATH % i
            self.upload_file(self.uid, file_path, file_data={'mtime': 100 + i})
            items.append({'type': 'resource', 'path': file_path})
            album_dict = deepcopy(self.ALBUM_CREATE_JSON)
            album_dict["items"] = items[:]
            album = self.json_ok('albums_create_with_items', opts={'uid': self.uid}, json=album_dict)

            session.execute(self.SQL_UPDATE_ALBUM, {'album_type': AlbumType.FACES.value, 'id': "\\x%s" % album['id']})
            albums_ids.append(album['id'])

        time.sleep(1)
        all_albums = self.json_ok('albums_list', opts={'uid': self.uid, 'album_type': AlbumType.FACES.value})
        assert len(all_albums) == 5
        assert [i['items_count'] for i in all_albums] == [5, 4, 3, 2, 1]
        # альбомы c большим кол-вом элементов идут в начале
        assert albums_ids[::-1] == [i['id'] for i in all_albums]

    def test_album_fields(self):
        items = []
        session = Session.create_from_uid(self.uid)
        for i in range(2):
            file_path = self.RESOURCE_PATH % i
            self.upload_file(self.uid, file_path, file_data={'mtime': 100 + i})
            items.append({'type': 'resource', 'path': file_path})
        album_dict = deepcopy(self.ALBUM_CREATE_JSON)
        album_dict["items"] = items
        album = self.json_ok('albums_create_with_items', opts={'uid': self.uid}, json=album_dict)
        time.sleep(1)

        meta = 'size,sizes,mediatype,file_id,storage_type'
        with mock.patch('mpfs.core.albums.models.Album.select_new_cover') as mocked_select_new_cover:
            personal_albums = self.json_ok('albums_list', opts={'uid': self.uid, 'amount': 4, 'album_type': AlbumType.PERSONAL.value, 'meta': meta})

            session.execute(self.SQL_UPDATE_ALBUM,
                        {'album_type': AlbumType.FACES.value, 'id': "\\x%s" % album['id']})
            faces_albums = self.json_ok('albums_list', opts={'uid': self.uid, 'amount': 4, 'album_type': AlbumType.FACES.value, 'meta': meta})

            mocked_select_new_cover.assert_not_called()
            assert not (set(personal_albums[0].keys()) - set(faces_albums[0].keys()))
            assert faces_albums[0]["items_count"] == 2
            assert not (set(personal_albums[0]["cover"]["object"]["meta"].keys()) - set(faces_albums[0]["cover"]["object"]["meta"].keys()))

    @parameterized.expand([
        ("horizontal_overflow", "1536", "2048", "0.436081", "0.402529", "0.242184", "0.313399"),
        ("vertical_overflow", "2000", "1000", "0.5", "0.7", "0.05", "0.1"),
        ("full_size", "1000", "750", "1", "1", "0", "0"),
        ("full_size_2", "2560", "1920", "0.5361372471", "0.923567009", "0.3296781301", "0"),
    ])
    def test_check_face_coord(self, test_case, width, height, face_width, face_height, face_coord_x, face_coord_y):
        face_info = {
            "width": width,
            "height": height,
            "face_width": face_width,
            "face_height": face_height,
            "face_coord_x": face_coord_x,
            "face_coord_y": face_coord_y,
        }
        album_item = self.AlbumItemDummy(face_info)
        face_coords = get_album_item_face_coordinates(album_item)
        assert face_coords
        assert borders_is_valid(face_coords["face_left"], face_coords["face_right"], face_coords["face_top"], face_coords["face_bottom"])
