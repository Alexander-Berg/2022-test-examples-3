# -*- coding: utf-8 -*-
import copy
import datetime
import json

from nose_parameterized import parameterized
import mock

from helpers.stubs.resources.users_info import DEFAULT_USERS_INFO
from mpfs.common.util import datetime_to_unixtime
from mpfs.core.services.passport_service import Passport
from test.base import time_machine
from test.parallelly.api.disk.base import DiskApiTestCase
from test.base_suit import UploadFileTestCaseMixin, UserTestCaseMixin, SupportApiTestCaseMixin
from mpfs.common.static import tags
from mpfs.core.filesystem.resources.disk import BlockingsMixin, get_blockings_collection
from mpfs.core.services.clck_service import Clck
import mpfs.common.errors as errors
from mpfs.common.static import codes

from hamcrest import assert_that, has_items, has_entries, has_item


class BasePublicResourcesTestCase(UserTestCaseMixin, UploadFileTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.EXTERNAL
    file_name = 'test.txt'
    file_path = 'disk:/%s' % file_name
    mpfs_file_path = '/disk/%s' % file_name
    dir_name = 'test'
    dir_path = 'disk:/%s' % dir_name
    mpfs_dir_path = '/disk/%s' % dir_name

    resource_url = 'disk/resources'
    publish_url = 'disk/resources/publish'
    unpublish_url = 'disk/resources/unpublish'
    list_url = 'disk/resources/public'

    def setup_method(self, method):
        super(BasePublicResourcesTestCase, self).setup_method(method)
        self.create_user(self.uid)
        self.upload_file(self.uid, self.mpfs_file_path)
        self.json_ok('mkdir', {'uid': self.uid, 'path': self.mpfs_dir_path})


class PublicResourcesTestCase(BasePublicResourcesTestCase):

    def test_save_to_disk(self):
        with self.specified_client(scopes=['cloud_api:disk.write', 'cloud_api:disk.read']):
            self.client.put(self.publish_url, query={'path': self.file_path})

            resp = self.client.get(self.list_url)
            res = json.loads(resp.content)
            public_key = res['items'][0]['public_key']

            resp = self.client.request('POST', 'disk/public/resources/save-to-disk',
                                       query={'public_key': public_key, 'name': 'dummy.txt', 'save_path': '/%s' % self.dir_name})
            assert resp.status_code in (201, 202)
            resp = self.client.get(self.resource_url, query={'path': '/%s/%s' % (self.dir_name, 'dummy.txt')})
            assert resp.status_code == 200
            resource = json.loads(resp.content)
            assert resource['path'] == u'disk:/test/dummy.txt'

            resp = self.client.request('POST', 'disk/public/resources/save-to-disk',
                                       query={'public_key': public_key})
            assert resp.status_code in (201, 202)
            resp = self.client.get(self.resource_url, query={'path': '/Загрузки/test.txt'})
            assert resp.status_code == 200
            resource = json.loads(resp.content)
            assert resource['path'] == u'disk:/Загрузки/test.txt'

    def test_save_to_disk_by_any_url(self):
        urls = self.json_ok('set_public', {'uid': self.uid, 'path': self.mpfs_file_path})
        for i, url_type in enumerate(['url', 'short_url', 'hash', 'short_url_named'], 1):
            url = urls[url_type]
            file_name = 'test_%s' % i
            with self.specified_client(scopes=['cloud_api:disk.write', 'cloud_api:disk.read']):
                resp = self.client.post('disk/public/resources/save-to-disk',
                                        query={'public_key': url, 'name': file_name})
            assert 201 == resp.status_code
            self.json_ok('info', {'uid': self.uid, 'path': u'/disk/Загрузки/' + file_name})

    def test_publish(self):
        with self.specified_client(scopes=['cloud_api:disk.write', 'cloud_api:disk.read']):
            resp = self.client.put(self.publish_url, query={'path': self.file_path})
            assert resp.status_code == 200
            link = json.loads(resp.content)
            assert 'href' in link

            resp = self.client.get(self.resource_url, query={'path': self.file_path})
            assert resp.status_code == 200
            resource = json.loads(resp.content)
            assert 'public_key' in resource
            assert 'public_url' in resource

    def test_unpublish(self):
        with self.specified_client(scopes=['cloud_api:disk.write', 'cloud_api:disk.read']):
            self.client.put(self.publish_url, query={'path': self.file_path})
            resp = self.client.put(self.unpublish_url, query={'path': self.file_path})
            assert resp.status_code == 200
            link = json.loads(resp.content)
            assert 'href' in link

            resp = self.client.get(self.resource_url, query={'path': self.file_path})
            assert resp.status_code == 200
            resource = json.loads(resp.content)
            assert 'public_key' not in resource
            assert 'public_url' not in resource

    def test_list_public_resources(self):
        with self.specified_client(scopes=['cloud_api:disk.write', 'cloud_api:disk.read']):
            self.client.put(self.publish_url, query={'path': self.file_path})
            self.client.put(self.publish_url, query={'path': self.dir_path})

            # проверяем вывод полного списка
            resp = self.client.get(self.list_url, query={})
            assert resp.status_code == 200
            public_list = json.loads(resp.content)
            assert len(public_list['items']) == 2
            assert 'type' not in public_list
            assert public_list['items'][0]['path'] is not None

            # проверям вывод списка с фильтром по файлам
            resp = self.client.get(self.list_url, query={'type': 'file'})
            assert resp.status_code == 200
            public_list = json.loads(resp.content)
            assert len(public_list['items']) == 1
            assert 'type' in public_list
            assert public_list['items'][0]['type'] == 'file'

            # проверяем вывод списка с фильтром по папкам
            resp = self.client.get(self.list_url, query={'type': 'dir'})
            assert resp.status_code == 200
            public_list = json.loads(resp.content)
            assert len(public_list['items']) == 1
            assert 'type' in public_list
            assert public_list['items'][0]['type'] == 'dir'

    def test_get_public_by_key_and_url(self):
        with self.specified_client(scopes=['yadisk:all']):
            self.client.request('PUT', 'disk/resources/publish', query={'path': self.dir_path})

            resp = self.client.request('GET', 'disk/resources/public', uid=self.uid)
            res = json.loads(resp.content)
            public_key = res['items'][0]['public_key']
            public_url = res['items'][0]['public_url']

            resp = self.client.request('GET',
                                       'disk/public/resources',
                                       uid=self.uid,
                                       query={'public_key': public_key})
            assert resp.status == 200
            resp = self.client.request('GET',
                                       'disk/public/resources',
                                       uid=self.uid,
                                       query={'public_key': public_url})
            assert resp.status == 200

    def test_fields_resources_public(self):
        required_body_fields = {'items', 'limit', 'offset'}
        required_item_fields = {'public_key', 'name', 'created', 'public_url', 'modified', 'path', 'type',
                                'revision', 'resource_id', 'comment_ids', 'exif'}
        with self.specified_client(scopes=['yadisk:all']):
            self.client.request('PUT', 'disk/resources', query={'path': '/dir'})
            self.client.request('PUT', 'disk/resources/publish', query={'path': '/dir'})

            resp = self.client.request('GET', 'disk/resources/public', uid=self.uid)
            res = json.loads(resp.content)
            assert resp.status == 200
            assert not required_body_fields ^ res.viewkeys()
            for item in res['items']:
                assert not required_item_fields ^ item.viewkeys()

    def test_fields_public_resources(self):
        required_body_fields = {
            'public_key', '_embedded', 'name', 'resource_id', 'revision', 'public_url',
            'modified', 'created', 'path', 'comment_ids', 'type', 'views_count', 'owner', 'exif'
        }
        with self.specified_client(scopes=['yadisk:all']):
            self.client.request('PUT', 'disk/resources', query={'path': '/dir'})
            self.client.request('PUT', 'disk/resources/publish', query={'path': '/dir'})

            resp = self.client.request('GET', 'disk/resources/public', uid=self.uid)
            res = json.loads(resp.content)
            public_key = res['items'][0]['public_key']
            public_url = res['items'][0]['public_url']

            resp = self.client.request('GET',
                                       'disk/public/resources',
                                       uid=self.uid,
                                       query={'public_key': public_key})
            assert resp.status == 200
            res = json.loads(resp.content)
            assert not required_body_fields ^ res.viewkeys()


class PublicResourcesInfoTestCase(BasePublicResourcesTestCase):
    """Проверяет корректность данных ручки получения информации по публичному key/URL.

    Стуктура ресурсов пользователя:

    ```
    /test/ (public folder)
    └── rel_dir/
        ├── child_dir/
        └── child_file.txt
    ```

    """
    rel_dir_name = 'rel_dir'
    rel_dir_path = '%s/%s' % (BasePublicResourcesTestCase.dir_path, rel_dir_name)
    rel_dir_mpfs_path = '%s/%s' % (BasePublicResourcesTestCase.mpfs_dir_path, rel_dir_name)
    child_dir_name = 'child_dir'
    child_dir_path = '%s/%s' % (rel_dir_path, child_dir_name)
    child_dir_mpfs_path = '%s/%s' % (rel_dir_mpfs_path, child_dir_name)
    child_file_name = 'child_file.txt'
    child_file_path = '%s/%s' % (rel_dir_path, child_dir_name)
    child_file_mpfs_path = '%s/%s' % (rel_dir_mpfs_path, child_file_name)

    def setup_method(self, method):
        super(PublicResourcesInfoTestCase, self).setup_method(method)
        self.json_ok('mkdir', {'uid': self.uid, 'path': self.rel_dir_mpfs_path})
        self.json_ok('mkdir', {'uid': self.uid, 'path': self.child_dir_mpfs_path})
        self.upload_file(self.uid, self.child_file_mpfs_path)

    def test_get_public_info(self):
        with self.specified_client(scopes=['yadisk:all']):
            self.client.request('PUT', 'disk/resources/publish', query={'path': self.dir_path})

            resp = self.client.request('GET', 'disk/resources/public', uid=self.uid)
            res = json.loads(resp.content)
            public_key = res['items'][0]['public_key']
            public_url = res['items'][0]['public_url']

            resp = self.client.request('GET',
                                       'disk/public/resources',
                                       uid=self.uid,
                                       query={'public_key': public_url})
            assert resp.status == 200
            response = json.loads(resp.content)
            assert response['path'] == '/'
            assert response['_embedded']['path'] == '/'
            assert response['name'] == self.dir_name
            assert response['public_key'] == public_key

            assert_that(response['_embedded']['items'],
                        has_item(has_entries({'name': self.rel_dir_name,
                                              'path': '/%s' % self.rel_dir_name})))

    def test_get_public_info_with_relative_path(self):
        with self.specified_client(scopes=['yadisk:all']):
            self.client.request('PUT', 'disk/resources/publish', query={'path': self.dir_path})

            resp = self.client.request('GET', 'disk/resources/public', uid=self.uid)
            res = json.loads(resp.content)
            public_key = res['items'][0]['public_key']
            public_url = res['items'][0]['public_url']

            resp = self.client.request('GET',
                                       'disk/public/resources',
                                       uid=self.uid,
                                       query={'public_key': public_url,
                                              'path': '/%s' % self.rel_dir_name})
            assert resp.status == 200
            response = json.loads(resp.content)
            assert response['path'] == '/%s' % self.rel_dir_name
            assert response['_embedded']['path'] == '/%s' % self.rel_dir_name
            assert response['name'] == self.rel_dir_name
            assert response['public_key'] == public_key

            assert_that(response['_embedded']['items'],
                        has_items(has_entries({'name': self.child_dir_name,
                                               'path': '/%s/%s' % (self.rel_dir_name, self.child_dir_name)}),
                                  has_entries({'name': self.child_file_name,
                                               'path': '/%s/%s' % (self.rel_dir_name, self.child_file_name)})))


class GetPublicResourceHandlerTestCase(UserTestCaseMixin, UploadFileTestCaseMixin, DiskApiTestCase, SupportApiTestCaseMixin):
    endpoint = 'disk/public/resources'

    def test_response_contains_user_public_information(self):
        self.create_user(self.uid)
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/dir'})
        self.upload_file(self.uid, '/disk/dir/test.txt')
        self.json_ok('set_public', {'uid': self.uid, 'path': '/disk/dir'})
        response = self.json_ok('info', {'uid': self.uid, 'path': '/disk/dir', 'meta': ''})
        public_hash = response['meta']['public_hash']

        with self.specified_client(scopes=['yadisk:all']):
            response = self.client.get(self.endpoint, {'public_key': public_hash})
            result = json.loads(response.content)
            assert 'owner' in result
            owner = result['owner']
            assert 'uid' in owner
            assert 'login' in owner
            assert 'display_name' in owner
            resources = result['_embedded']['items']
            for resource in resources:
                assert 'antivirus_status' in resource
                assert resource['antivirus_status'] == 'clean'

    def test_response_for_blocked_link(self):
        """
        тест на обработку 409 от mpfs
        в случае заблокированного каталога внутри публичной папки

        mpfs вернет 409 только если заблокирован каталог внутри папки,
        но не сама папка целиком
        :return:
        """
        self.create_user(self.uid)
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/dir'})
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/dir/a'})
        self.upload_file(self.uid, '/disk/dir/a/test.txt')
        self.json_ok('set_public', {'uid': self.uid, 'path': '/disk/dir'})
        response = self.json_ok('info', {'uid': self.uid, 'path': '/disk/dir', 'meta': ''})
        public_hash = response['meta']['public_hash']

        response = self.client.get(
            self.endpoint,
            {
                'public_key': public_hash,
                'path': '/a/test.txt'
            }
        )
        self.assertEqual(200, response.status)

        # блокируем папку в публичной папке
        result = self.support_ok('block_public_file', {
            'moderator': 'moderator',
            'comment': 'comment',
            'private_hash': public_hash + ':/a',
            'type': 'block_file',
            'view': 'st',
            'link': 'https://rkn.gov.ru/',
            'notify': 0,
        })
        self.assertTrue(result)

        # подпапка внутри публичной папки заблокирована
        self.json_error('public_list', {
            'private_hash': public_hash+':/a'
        }, code=codes.RESOURCE_BLOCKED)

        response = self.client.get(
            self.endpoint,
            {
                'public_key': public_hash,
                'path': '/a/test.txt'
            }
        )
        self.assertEqual(404, response.status)

    def test_block_download_public_resource(self):
        filepath = '/disk/test.txt'
        self.create_user(self.uid)
        self.upload_file(self.uid, filepath)

        file_info = self.json_ok('info', {'uid': self.uid, 'path': filepath, 'meta': 'hid'})
        hid = file_info['meta']['hid']
        get_blockings_collection().insert({'_id': hid, 'data': 'i_dont_know_what_to_put_here'})
        public_hash = self.json_ok('set_public', {'uid': self.uid, 'path':  filepath})['hash']

        with mock.patch.object(BlockingsMixin, '_is_blockings_needed', return_value=True),\
            mock.patch('mpfs.platform.v1.disk.handlers.FEATURE_TOGGLES_BLOCK_PUBLIC_RESOURCE_DOWNLOAD', 100):
            response = self.client.get(self.endpoint, {'public_key': public_hash})
        assert response.status == 429


class GetPublicResourceDownloadLinkHandlerTestCase(BasePublicResourcesTestCase):
    endpoint = 'disk/public/resources/download'

    def test_no_auth_antifo_blocked_file_is_not_returned(self):
        file_info = self.json_ok('info', {'uid': self.uid, 'path': self.mpfs_file_path, 'meta': 'hid'})
        hid = file_info['meta']['hid']
        get_blockings_collection().insert({'_id': hid, 'data': 'i_dont_know_what_to_put_here'})
        public_hash = self.json_ok('set_public', {'uid': self.uid, 'path':  self.mpfs_file_path})['hash']
        response = self.client.get(self.endpoint, {'public_key': public_hash})
        assert response.status == 404

    @parameterized.expand([
        ('Yandex.Disk {"os":"android 7.0","device":"phone","src":"disk.mobile","vsn":"3.20-0","id":"5281b1fc6bd9b022f1b6969508ebaa57"}', True),
        ('Yandex.Disk {"os":"iOS","src":"disk.mobile","vsn":"2.14.7215","id":"E9C69BDA-0837-4867-A9B0-3AFCAAC3342A","device":"tablet"}', True),
        ('Google Chrome', False),
    ])
    def test_user_agent_auth_antifo_blocked_file(self, ua, success_expceted):
        file_info = self.json_ok('info', {'uid': self.uid, 'path': self.mpfs_file_path, 'meta': 'hid'})
        hid = file_info['meta']['hid']
        get_blockings_collection().insert({'_id': hid, 'data': 'i_dont_know_what_to_put_here'})
        public_hash = self.json_ok('set_public', {'uid': self.uid, 'path':  self.mpfs_file_path})['hash']
        response = self.client.get(self.endpoint, {'public_key': public_hash}, headers={'User-Agent': ua})
        if success_expceted:
            assert response.status == 200
        else:
            assert response.status == 404

    def test_public_key_short_url(self):
        public_url = self.json_ok('set_public', {'uid': self.uid, 'path': self.mpfs_file_path})['short_url']
        response = self.client.get(self.endpoint, {'public_key': public_url})
        assert response.status == 200

    @parameterized.expand([
        (404, 404),
        (400, 400),
        (500, 500),
    ])
    def test_public_key_clck_response(self, clck_response_code, expected_code):
        public_hash = 'https%3A%2F%2Fyadi.sk%2Fd%2FzNVTwQx13uiGavA'
        with mock.patch.object(
            Clck, 'short_url_to_full_url',
            side_effect=errors.ClckNoResponse(data={'code': clck_response_code})
        ) as clck_mock:
            response = self.client.get(self.endpoint, {'public_key': public_hash})
            clck_mock.assert_called()
            assert response.status == expected_code


class PublicSettingsTestCase(BasePublicResourcesTestCase):

    def test_get_public_settings(self):
        with self.specified_client(scopes=['yadisk:all']):
            self.client.request('PUT', self.publish_url, query={'path': self.dir_path})

            resp = self.client.get(
                'disk/public/resources/public-settings',
                query={'path': self.dir_path}
            )
            assert resp.status_code == 200
            public_settings = json.loads(resp.content)
            assert 'read_only' in public_settings
            assert 'have_password' in public_settings
            assert 'available_until' in public_settings

    @parameterized.expand([
        'file', 'folder'
    ])
    def test_set_public_settings(self, typ):
        with self.specified_client(scopes=['yadisk:all']):
            from mpfs.core.base import PUBLIC_SETTINGS_FEATURE
            obj_path = self.file_path if typ == 'file' else self.dir_path
            self.json_ok(
                'set_ps_billing_feature',
                {'uid': self.uid, 'feature_name': PUBLIC_SETTINGS_FEATURE, 'value': 1}
            )
            self.client.request('PUT', self.publish_url, query={'path': obj_path})

            resp = self.client.post(
                'disk/public/resources/public-settings',
                query={'path': obj_path},
                data={'read_only': True}
            )
            assert resp.status_code == 200
            resp = self.client.request('GET', 'disk/resources/public', uid=self.uid)
            res = json.loads(resp.content)
            public_key = res['items'][0]['public_key']
            public_url = res['items'][0]['public_url']

            resp = self.client.request('GET',
                                       'disk/public/resources',
                                       uid=self.uid,
                                       query={'public_key': public_key})
            assert resp.status == 200

            resp = self.client.request('GET',
                                       'disk/public/resources',
                                       uid=self.uid,
                                       query={'public_key': public_url})
            assert resp.status == 200

            resp = self.client.get(
                'disk/public/resources/public-settings',
                query={'path': obj_path}
            )
            assert resp.status_code == 200
            public_settings = json.loads(resp.content)
            assert 'read_only' in public_settings
            assert public_settings['read_only']
            assert 'have_password' in public_settings
            assert 'available_until' in public_settings

            # сбрасываем read_only и выставляем available_until
            with time_machine(datetime.datetime(2020, 6, 1)):
                test_dt = datetime_to_unixtime(datetime.datetime(2022, 6, 1))
                resp = self.client.post(
                    'disk/public/resources/public-settings',
                    query={'path': obj_path},
                    data={'read_only': None, 'available_until': test_dt}
                )
                assert resp.status_code == 200

            resp = self.client.get(
                'disk/public/resources/public-settings',
                query={'path': obj_path}
            )
            assert resp.status_code == 200
            public_settings = json.loads(resp.content)
            assert 'read_only' in public_settings
            assert not public_settings['read_only']
            assert 'have_password' in public_settings
            assert 'available_until' in public_settings
            assert public_settings['available_until'] == test_dt

    def test_set_public_settings_fail(self):
        with self.specified_client(scopes=['yadisk:all']):
            self.client.request('PUT', self.publish_url, query={'path': self.dir_path})

            resp = self.client.post(
                'disk/public/resources/public-settings',
                query={'path': self.dir_path},
                data={'read_only': True}
            )
            assert resp.status_code == 404

    def test_reset_public_settings_on_disable_feature(self):
        with self.specified_client(scopes=['yadisk:all']):
            from mpfs.core.base import PUBLIC_SETTINGS_FEATURE
            self.json_ok(
                'set_ps_billing_feature',
                {'uid': self.uid, 'feature_name': PUBLIC_SETTINGS_FEATURE, 'value': 1}
            )
            self.client.request('PUT', self.publish_url, query={'path': self.dir_path})

            with time_machine(datetime.datetime(2020, 6, 1)):
                test_dt = datetime_to_unixtime(datetime.datetime(2022, 6, 1))

                resp = self.client.post(
                    'disk/public/resources/public-settings',
                    query={'path': self.dir_path},
                    data={'read_only': True, 'available_until': test_dt}
                )
                assert resp.status_code == 200

            resp = self.client.get(
                'disk/public/resources/public-settings',
                query={'path': self.dir_path}
            )
            assert resp.status_code == 200
            public_settings = json.loads(resp.content)
            assert public_settings['read_only']
            assert public_settings['available_until'] == test_dt

            self.json_ok(
                'set_ps_billing_feature',
                {'uid': self.uid, 'feature_name': PUBLIC_SETTINGS_FEATURE, 'value': 0}
            )

            # сбрасываем настройки
            with time_machine(datetime.datetime(2020, 6, 1)):
                test_dt = datetime_to_unixtime(datetime.datetime(2022, 6, 1))
                resp = self.client.post(
                    'disk/public/resources/public-settings',
                    query={'path': self.dir_path},
                    data={'read_only': None, 'available_until': None},
                )
                assert resp.status_code == 200

            resp = self.client.get(
                'disk/public/resources/public-settings',
                query={'path': self.dir_path},
            )
            assert resp.status_code == 200
            public_settings = json.loads(resp.content)
            assert not public_settings['read_only']
            assert not public_settings['available_until']

    def test_get_public_by_key_and_url(self):
        with self.specified_client(scopes=['yadisk:all']):
            self.client.request('PUT', 'disk/resources/publish', query={'path': self.dir_path})

            resp = self.client.request('GET', 'disk/resources/public', uid=self.uid)
            res = json.loads(resp.content)
            public_key = res['items'][0]['public_key']
            public_url = res['items'][0]['public_url']

            resp = self.client.request('GET',
                                       'disk/public/resources',
                                       uid=self.uid,
                                       query={'public_key': public_key})
            assert resp.status == 200
            resp = self.client.request('GET',
                                       'disk/public/resources',
                                       uid=self.uid,
                                       query={'public_key': public_url})
            assert resp.status == 200

    def test_set_public_settings_verbose(self):
        with self.specified_client(scopes=['yadisk:all']):
            from mpfs.core.base import PUBLIC_SETTINGS_FEATURE
            self.json_ok(
                'set_ps_billing_feature',
                {'uid': self.uid, 'feature_name': PUBLIC_SETTINGS_FEATURE, 'value': 1}
            )
            self.client.request('PUT', self.publish_url, query={'path': self.dir_path})

            with time_machine(datetime.datetime(2020, 6, 1)):
                test_dt = datetime_to_unixtime(datetime.datetime(2022, 6, 1))

                resp = self.client.post(
                    'disk/public/resources/public-settings',
                    query={'path': self.dir_path},
                    data={'read_only': True, 'available_until_verbose': {'enabled': True, 'value': test_dt}}
                )
                assert resp.status_code == 200

            resp = self.client.get(
                'disk/public/resources/public-settings',
                query={'path': self.dir_path}
            )
            assert resp.status_code == 200
            public_settings = json.loads(resp.content)
            assert public_settings['read_only']
            assert public_settings['available_until'] == test_dt

            # сбрасываем настройки
            with time_machine(datetime.datetime(2020, 6, 1)):
                resp = self.client.post(
                    'disk/public/resources/public-settings',
                    query={'path': self.dir_path},
                    data={'available_until_verbose': {'enabled': False}},
                )
                assert resp.status_code == 200

            resp = self.client.get(
                'disk/public/resources/public-settings',
                query={'path': self.dir_path},
            )
            assert resp.status_code == 200
            public_settings = json.loads(resp.content)
            assert not public_settings['available_until']
