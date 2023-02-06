# -*- coding: utf-8 -*-
import json
import time

import mock

from urlparse import urlparse
from nose_parameterized import parameterized

from test.parallelly.api.disk.base import DiskApiTestCase
from test.base_suit import UserTestCaseMixin
from mpfs.common.static import tags
from mpfs.common.util import from_json

from copy import deepcopy
from test.base_suit import SupportApiTestCaseMixin
from test.helpers.stubs.resources.users_info import DEFAULT_USERS_INFO
from test.helpers.stubs.services import PassportStub
from mpfs.core.services.passport_service import passport
from mpfs.config import settings


class TrashTestCase(UserTestCaseMixin, SupportApiTestCaseMixin, DiskApiTestCase):
    api_version = 'v1'
    api_mode = tags.platform.INTERNAL
    default_path_1 = '/TrashTestCase-1'
    default_path_2 = '/TrashTestCase-2'

    def setup_method(self, method):
        super(TrashTestCase, self).setup_method(method)
        self.create_user(uid=self.uid, locale='ru')
        for path in (self.default_path_1, self.default_path_2):
            resp = self.client.put('disk/resources', query={'path': path}, uid=self.uid)
            self.assertEqual(resp.status_code, 201)
            with mock.patch('mpfs.core.address.Address.add_trash_suffix'):
                resp = self.client.delete('disk/resources', query={'path': path}, uid=self.uid)
            self.assertEqual(resp.status_code, 204)
            time.sleep(1)

    @parameterized.expand([('deleted',),
                           ('created',)])
    def test_sort(self, sort_field):
        resp = self.client.get('disk/trash/resources',
                               query={'path': '/', 'sort': sort_field},
                               uid=self.uid)
        assert resp.status_code == 200
        resp = from_json(resp.content)
        actual_items_names = [i['name'] for i in resp['_embedded']['items']]
        assert actual_items_names == [u'TrashTestCase-1', u'TrashTestCase-2']

        resp = self.client.get('disk/trash/resources',
                               query={'path': '/', 'sort': '-%s' % sort_field},
                               uid=self.uid)
        assert resp.status_code == 200
        resp = from_json(resp.content)
        actual_items_names = [i['name'] for i in resp['_embedded']['items']]
        assert actual_items_names == [u'TrashTestCase-2', u'TrashTestCase-1']

    def test_sort_wrong_field(self):
        resp = self.client.get('disk/trash/resources',
                               query={'path': '/', 'sort': 'unknown_field'},
                               uid=self.uid)
        assert resp.status_code == 400

    def test_list(self):
        resp = self.client.get('disk/trash/resources', query={'path': '/'}, uid=self.uid)
        self.assertEqual(resp.status_code, 200)
        resp = from_json(resp.content)
        self.assertEqual(resp.get('_embedded', {}).get('items', [])[0].get('path'),
                         'trash:%s' % self.default_path_1)

        # получаем листинг папки в корзине
        resp = self.client.get('disk/trash/resources',
                               query={'path': self.default_path_1},
                               uid=self.uid)
        self.assertEqual(resp.status_code, 200)
        resp = from_json(resp.content)
        self.assertEqual(resp.get('path'), 'trash:%s' % self.default_path_1)
        self.assertEqual(resp.get('origin_path'), 'disk:%s' % self.default_path_1)

        # попытка получить несуществующий в корзине файл
        resp = self.client.get('disk/trash/resources',
                               query={'path': '/unexistent.file'},
                               uid=self.uid)
        self.assertEqual(resp.status_code, 404)

    def test_restore(self):
        # проверяем что папки нет
        resp = self.client.get('disk/resources/', query={'path': self.default_path_1}, uid=self.uid)
        self.assertEqual(resp.status_code, 404)
        # восстанавливаем папку
        resp = self.client.put('disk/trash/resources/restore',
                               query={'path': self.default_path_1},
                               uid=self.uid)
        self.assertEqual(resp.status_code, 201)
        # проверяем что папка восстановилась
        resp = self.client.get('disk/resources/', query={'path': self.default_path_1}, uid=self.uid)
        self.assertEqual(resp.status_code, 200)

    def test_clear_without_path(self):
        resp = self.client.delete('disk/trash/resources/', uid=self.uid)
        self.assertEqual(resp.status_code, 202)

    def test_clear_by_short_path(self):
        resp = self.client.delete('disk/trash/resources/', query={'path': '/'}, uid=self.uid)
        self.assertEqual(resp.status_code, 202)
        content = json.loads(resp.content)
        oid = urlparse(content['href']).path[-64:]
        resp = self.client.get('disk/operations/%s' % oid, uid=self.uid)
        content = json.loads(resp.content)
        assert content['status'] == 'success'

    def test_clear_by_full_path(self):
        resp = self.client.delete('disk/trash/resources/', query={'path': 'trash:/'}, uid=self.uid)
        self.assertEqual(resp.status_code, 202)
        content = json.loads(resp.content)
        oid = urlparse(content['href']).path[-64:]
        resp = self.client.get('disk/operations/%s' % oid, uid=self.uid)
        content = json.loads(resp.content)
        assert content['status'] == 'success'

    def test_remove_from_trash(self):
        resp = self.client.delete('disk/trash/resources/',
                                  query={'path': self.default_path_1},
                                  uid=self.uid)
        self.assertEqual(resp.status_code, 204)
        resp = self.client.get('disk/trash/resources', query={'path': '/'}, uid=self.uid)
        self.assertEqual(resp.status_code, 200)
        resp = from_json(resp.content)
        actual_items = [item
                        for item in resp['_embedded']['items']
                        if item['path'] == 'trash:%s' % self.default_path_1]
        self.assertEqual(len(actual_items), 0)

    def test_revisions(self):
        """Проверить наличие поля revision у каждого из эелементов плоского списка ресурсов.
        """
        resp = self.client.get('disk/trash/resources', query={'path': '/'}, uid=self.uid)
        assert resp.status_code == 200
        resp = from_json(resp.content)
        assert all('revision' in item for item in resp['_embedded']['items'])

    def test_clear_trash_for_noninit_user_with_passport_bad_response(self):
        """Протестировать, что при запросе информации пользователем с аккаунтом без пароля он получит 403 ошибку."""
        uid = '123456789'
        user_info = deepcopy(DEFAULT_USERS_INFO[self.uid])
        user_info['uid'] = uid
        with self.specified_client(scopes=['yadisk:all'], uid=uid):
            with PassportStub(userinfo=user_info) as stub:
                stub.subscribe.side_effect = passport.errors_map['accountwithpasswordrequired']
                response = self.client.delete('disk/trash/resources/', uid=uid)
                assert response.status_code == 403
                content = json.loads(response.content)
                assert content['error'] == 'DiskUnsupportedUserAccountTypeError'
                assert content['description'] == 'User account type is not supported.'

    def test_clear_trash_for_blocked_user(self):
        opts = {
            'uid': self.uid,
            'moderator': 'moderator',
            'comment': 'comment',
        }
        self.support_ok('block_user', opts)
        with self.specified_client(scopes=['yadisk:all'], uid=self.uid):
            response = self.client.delete('disk/trash/resources/', uid=self.uid)
            content = json.loads(response.content)
            assert response.status_code == 403
            assert content['error'] == 'DiskUserBlockedError'
            assert content['description'] == 'User is blocked.'

    def test_clear_trash_for_noninit_user(self):
        uid = '123456789'
        user_info = deepcopy(DEFAULT_USERS_INFO[self.uid])
        user_info['uid'] = uid
        with self.specified_client(scopes=['yadisk:all'], uid=uid):
            with PassportStub(userinfo=user_info):
                response = self.client.delete('disk/trash/resources/', uid=uid)
                # init and retry with _auto_initialize_user
                assert response.status_code == 204
