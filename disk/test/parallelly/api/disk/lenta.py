# -*- coding: utf-8 -*-
from __future__ import unicode_literals

import os
import random
import mock
import urlparse

from nose_parameterized import parameterized
from test.parallelly.api.disk.base import DiskApiTestCase
from test.base_suit import UploadFileTestCaseMixin, UserTestCaseMixin
from mpfs.common.static import tags
from mpfs.common.util import from_json
from mpfs.config import settings
from mpfs.core.services.mpfsproxy_service import MpfsProxy
from mpfs.core.services.lenta_loader_service import LentaLoaderService


PLATFORM_DISK_APPS_IDS = settings.platform['disk_apps_ids']


class LentaResourcesHandlerTestCase(UserTestCaseMixin,
                                    UploadFileTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.EXTERNAL
    api_version = 'v1'

    def setup_method(self, method):
        super(LentaResourcesHandlerTestCase, self).setup_method(method)
        self.create_user(self.uid, noemail=True)

    def test_response_contains_share_rights(self):
        path = '/disk/group_folder'
        self.json_ok('mkdir', {'uid': self.uid, 'path': path})
        self.json_ok('share_create_group', {'uid': self.uid, 'path': path})
        with self.specified_client(id=random.choice(PLATFORM_DISK_APPS_IDS)):
            response = self.client.get('disk/lenta/resources', {
                'modified_gte': '2005-08-09T18:31:42',
                'path': '/group_folder'
            })
            assert response.status_code == 200
            data = from_json(response.content)
            assert 'resource_id' in data
            assert 'share' in data
            assert data['share']['rights'] == 'rw'

    def test_modified_lte_and_modified_gte_are_forwarded_to_mpfs(self):
        """Протестировать что `modified_lte` и `modified_gte` пробрасываются в MPFS."""
        # https://st.yandex-team.ru/CHEMODAN-31799#1477401757000
        path = '/disk/group_folder'
        self.json_ok('mkdir', {'uid': self.uid, 'path': path})
        self.json_ok('share_create_group', {'uid': self.uid, 'path': path})
        with self.specified_client(id=random.choice(PLATFORM_DISK_APPS_IDS)):
            with mock.patch.object(MpfsProxy, 'open_url', return_value=None) as mock_open_url:
                self.client.get('disk/lenta/resources', {
                    'modified_gte': '2005-08-09T18:31:42',
                    'modified_lte': '2030-08-09T18:31:42',
                    'path': '/group_folder'
                })
                assert mock_open_url.called
                args, kwargs = mock_open_url.call_args
                if 'url' in kwargs:
                    url = kwargs['url']
                else:
                    url = args[0]

                print url
                parsed_url = urlparse.urlparse(url)
                parsed_qs = urlparse.parse_qs(parsed_url.query)

                assert 'mtime_gte' in parsed_qs
                assert parsed_qs['mtime_gte'] == ['1123612302']

                assert 'mtime_lte' in parsed_qs
                assert parsed_qs['mtime_lte'] == ['1912530702']

    def test_modified_gte_gmt_plus_0(self):
        """Проверить что `modified_gte` с GMT+0 правильно конвертится в epoch time."""
        path = '/disk/group_folder'
        self.json_ok('mkdir', {'uid': self.uid, 'path': path})
        self.json_ok('share_create_group', {'uid': self.uid, 'path': path})
        with self.specified_client(id=random.choice(PLATFORM_DISK_APPS_IDS)):
            with mock.patch.object(MpfsProxy, 'open_url', return_value=None) as mock_open_url:
                self.client.get('disk/lenta/resources', {
                    'modified_gte': '2016-10-27T21:17:56+00:00',
                    'path': '/group_folder'
                })
                assert mock_open_url.called
                args, kwargs = mock_open_url.call_args
                if 'url' in kwargs:
                    url = kwargs['url']
                else:
                    url = args[0]

                parsed_url = urlparse.urlparse(url)
                parsed_qs = urlparse.parse_qs(parsed_url.query)

                assert 'mtime_gte' in parsed_qs
                assert parsed_qs['mtime_gte'] == ['1477603076']

    def test_modified_gte_gmt_plus_3(self):
        """Проверить что `modified_gte` с GMT+3 правильно конвертится в epoch time."""
        path = '/disk/group_folder'
        self.json_ok('mkdir', {'uid': self.uid, 'path': path})
        self.json_ok('share_create_group', {'uid': self.uid, 'path': path})
        with self.specified_client(id=random.choice(PLATFORM_DISK_APPS_IDS)):
            with mock.patch.object(MpfsProxy, 'open_url', return_value=None) as mock_open_url:
                self.client.get('disk/lenta/resources', {
                    'modified_gte': '2016-10-28T00:17:56+03:00',
                    'path': '/group_folder'
                })
                assert mock_open_url.called
                args, kwargs = mock_open_url.call_args
                if 'url' in kwargs:
                    url = kwargs['url']
                else:
                    url = args[0]

                parsed_url = urlparse.urlparse(url)
                parsed_qs = urlparse.parse_qs(parsed_url.query)

                assert 'mtime_gte' in parsed_qs
                assert parsed_qs['mtime_gte'] == ['1477603076']

    def test_etag_when_block_did_not_changed(self):
        """Проверить случай, когда набор ресурсов и сами ресурсы в ответе не меняются => ETag совпадает."""
        path = '/disk/group_folder'
        self.json_ok('mkdir', {'uid': self.uid, 'path': path})
        self.upload_file(self.uid, '/disk/group_folder/trump.jpg')
        self.json_ok('share_create_group', {'uid': self.uid, 'path': path})
        with self.specified_client(id=random.choice(PLATFORM_DISK_APPS_IDS)):
            response = self.client.get('disk/lenta/resources', {
                'modified_gte': '2005-08-09T18:31:42',
                'path': '/group_folder'
            })
            assert 'ETag' in response.headers
            etag = str(response.headers['ETag'])
            response = self.client.get('disk/lenta/resources', {
                'modified_gte': '2005-08-09T18:31:42',
                'path': '/group_folder'
            })
            assert str(response.headers['ETag']) == etag

    def test_video_metadata(self):
        path = '/disk/folder'
        self.json_ok('mkdir', {'uid': self.uid, 'path': path})

        # file with video_info
        file_with_video_metadata_path = '/raccoon_eating_popcorn.avi'
        path = '%s%s' % (path, file_with_video_metadata_path)
        self.upload_video(self.uid, path)

        with self.specified_client(id=random.choice(PLATFORM_DISK_APPS_IDS)):
            response = self.client.get('disk/lenta/resources', {
                'modified_gte': '2005-08-09T18:31:42',
                'path': '/folder'
            })
        data = from_json(response.content)
        assert 'video_metadata' in data['_embedded']['items'][0]
        assert isinstance(data['_embedded']['items'][0]['video_metadata']['duration'], int)

    def test_etag_when_block_changed(self):
        """Проверить случай, когда набор ресурсов в ответе изменился => ETag не совпадает."""
        path = '/disk/group_folder'
        self.json_ok('mkdir', {'uid': self.uid, 'path': path})
        self.upload_file(self.uid, '/disk/group_folder/trump.jpg')
        self.json_ok('share_create_group', {'uid': self.uid, 'path': path})
        with self.specified_client(id=random.choice(PLATFORM_DISK_APPS_IDS)):
            response = self.client.get('disk/lenta/resources', {
                'modified_gte': '2005-08-09T18:31:42',
                'path': '/group_folder'
            })
            assert 'ETag' in response.headers
            etag = str(response.headers['ETag'])
            self.upload_file(self.uid, '/disk/group_folder/putin.jpg')
            response = self.client.get('disk/lenta/resources', {
                'modified_gte': '2005-08-09T18:31:42',
                'path': '/group_folder'
            })
            assert str(response.headers['ETag']) != etag

    @parameterized.expand([
        ('disk_returned_for_old_clients', '2.14.7215', '/disk', 200),
        ('photounlim_not_returned_for_old_clients', '2.14.7215', '/photounlim', 404),
        ('disk_returned_for_new_clients', '3.23', '/disk', 200),
        ('photounlim_returned_for_new_clients', '3.23', '/photounlim', 200),
    ])
    def test_photounlim_returns_for_new_clients_only(self, case_name, version, path, expected_status_code):
        user_agent_with_version = 'Yandex.Disk {"os":"iOS","src":"disk.mobile","vsn":"%s","id":"E9C69BDA-0837-4867-A9B0-3AFCAAC3342A","device":"tablet"}' % version

        self.json_ok('enable_unlimited_autouploading', {'uid': self.uid})
        self.upload_file(self.uid, '/disk/test.jpg')
        self.upload_file(self.uid, '/photounlim/test.jpg')

        with self.specified_client(id=random.choice(PLATFORM_DISK_APPS_IDS)):
            # Для старого клиента не должны возвращаться блоки из /photoumlim
            response = self.client.get('disk/lenta/resources', {
                'modified_gte': '2005-08-09T18:31:42',
                'resource_id': ':'.join([self.uid, path])
            }, headers={'User-Agent': user_agent_with_version})
            assert response.status_code == expected_status_code

    def test_handle_malformed_version_correctly(self):
        user_agent_with_version = 'Yandex.Disk {"os":"iOS","src":"disk.mobile","vsn":"MARLFORMED","id":"E9C69BDA-0837-4867-A9B0-3AFCAAC3342A","device":"tablet"}'
        self.json_ok('enable_unlimited_autouploading', {'uid': self.uid})
        self.upload_file(self.uid, '/photounlim/test.jpg')
        with self.specified_client(id=random.choice(PLATFORM_DISK_APPS_IDS)):
            response = self.client.get('disk/lenta/resources', {
                'modified_gte': '2005-08-09T18:31:42',
                'resource_id': ':'.join([self.uid, '/photounlim'])
            }, headers={'User-Agent': user_agent_with_version})
            assert response.status_code == 200


class LentaReportBadBlockHandlerTestCase(UserTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.EXTERNAL
    api_version = 'v1'
    endpoint = 'disk/lenta/report-bad-block'

    def setup_method(self, method):
        super(LentaReportBadBlockHandlerTestCase, self).setup_method(method)
        self.create_user(self.uid, noemail=True)

    def test_lenta_delete_empty_block_called(self):
        """При запросе на эту ручку мы должны дергать lenta_delete_empty_block с пробросом всех GET-параметров."""
        response_status_code = 200
        response_content = 'OK'
        # статус код и контент рандомные, главное чтоб пробрасывались
        with mock.patch.object(LentaLoaderService, 'open_url',
                               return_value=(response_status_code, 'OK', None)) as mock_open_url:
            with self.specified_client(id=random.choice(PLATFORM_DISK_APPS_IDS)):
                response = self.client.get(self.endpoint, {
                    'modified_gte': '2005-08-09T18:31:42',
                    'path': '/path/to/some/folder',
                    'meaning_of_life': '42',
                    'vodka': 'true',
                    'balalaika': 'like',
                })
                assert response.status_code == response_status_code
                assert response.content == response_content

                assert mock_open_url.called
                args, kwargs = mock_open_url.call_args
                if 'url' in kwargs:
                    url = kwargs['url']
                else:
                    url = args[0]

                parsed_url = urlparse.urlparse(url)
                parsed_qs = urlparse.parse_qs(parsed_url.query)

                assert 'modified_gte' in parsed_qs
                assert parsed_qs['modified_gte'] == ['2005-08-09T18:31:42']
                parsed_qs.pop('modified_gte')

                assert 'path' in parsed_qs
                assert parsed_qs['path'] == ['/path/to/some/folder']
                parsed_qs.pop('path')

                assert 'meaning_of_life' in parsed_qs
                assert parsed_qs['meaning_of_life'] == ['42']
                parsed_qs.pop('meaning_of_life')

                assert 'vodka' in parsed_qs
                assert parsed_qs['vodka'] == ['true']
                parsed_qs.pop('vodka')

                assert 'balalaika' in parsed_qs
                assert parsed_qs['balalaika'] == ['like']
                parsed_qs.pop('balalaika')

                assert 'uid' in parsed_qs
                assert parsed_qs['uid'] == [str(self.uid)]
                parsed_qs.pop('uid')

                # проверяем что ничего лишнего не пихнули в GET-параметры
                assert not parsed_qs

    def test_real_uid_in_qs(self):
        """Протестировать что `uid` пробрасывается реальный, и нельзя его подменить через GET-параметры."""
        response_status_code = 200
        response_content = 'OK'
        with mock.patch.object(LentaLoaderService, 'open_url',
                               return_value=(response_status_code, response_content, None)) as mock_open_url:
            with self.specified_client(id=random.choice(PLATFORM_DISK_APPS_IDS)):
                insecure_uid = '123456789'
                assert str(self.uid) != insecure_uid
                response = self.client.get(self.endpoint, {
                    'uid': insecure_uid
                })
                assert response.status_code == response_status_code

                assert mock_open_url.called
                args, kwargs = mock_open_url.call_args
                if 'url' in kwargs:
                    url = kwargs['url']
                else:
                    url = args[0]

                parsed_url = urlparse.urlparse(url)
                parsed_qs = urlparse.parse_qs(parsed_url.query)

                assert 'uid' in parsed_qs
                assert parsed_qs['uid'] == [self.uid]
                parsed_qs.pop('uid')


class LentaDeleteBlockHandlerTestCase(UserTestCaseMixin, DiskApiTestCase):
    """Набор тестов для ресура удаления блока ленты."""
    api_mode = tags.platform.EXTERNAL
    api_version = 'v1'

    def setup_method(self, method):
        super(LentaDeleteBlockHandlerTestCase, self).setup_method(method)
        self.create_user(self.uid, noemail=True)

    def test_lenta_delete_block_called(self):
        """При запросе на эту ручку мы должны дергать ручку сервиса LentaLoader для удаления блока."""
        response_status_code = 200
        test_lenta_block_id = 's7vk923ka234'
        with mock.patch.object(LentaLoaderService, 'open_url',
                               return_value=(response_status_code, 'OK', None)) as mock_open_url:
            with self.specified_client(id=random.choice(PLATFORM_DISK_APPS_IDS)):
                response = self.client.delete('disk/lenta/blocks/%s' % test_lenta_block_id)
                assert response.status_code == 204

                assert mock_open_url.called
                args, kwargs = mock_open_url.call_args
                if 'url' in kwargs:
                    url = kwargs['url']
                else:
                    url = args[0]

                parsed_url = urlparse.urlparse(url)
                parsed_qs = urlparse.parse_qs(parsed_url.query)

                assert 'uid' in parsed_qs
                assert parsed_qs['uid'] == [self.uid]
                parsed_qs.pop('uid')

                assert 'block_id' in parsed_qs
                assert parsed_qs['block_id'] == [test_lenta_block_id]
                parsed_qs.pop('block_id')

                # проверяем что ничего лишнего не пихнули в GET-параметры
                assert not parsed_qs

    def test_lenta_loader_500_transformed_to_503(self):
        """Любой ответ кроме 200 (в данном тесте 500) от Java компонента приводит к 503 ошибке."""
        test_lenta_block_id = 's7vk923ka234'
        e = LentaLoaderService.api_error()
        assert e.status_code == 500
        with mock.patch.object(LentaLoaderService, 'open_url') as mock_open_url:
            mock_open_url.side_effect = e
            with self.specified_client(id=random.choice(PLATFORM_DISK_APPS_IDS)):
                response = self.client.delete('disk/lenta/blocks/%s' % test_lenta_block_id)
                assert response.status_code == 503


class LentaCreateAlbumFromBlockHandlerTestCase(UserTestCaseMixin,
                                               UploadFileTestCaseMixin, DiskApiTestCase):
    endpoint = 'disk/lenta/resources/create-album'

    def setup_method(self, method):
        super(LentaCreateAlbumFromBlockHandlerTestCase, self).setup_method(method)

    def test_default_request(self):
        self.create_user(self.uid, noemail=True)
        path = '/disk/test_folder'
        self.json_ok('mkdir', {'uid': self.uid, 'path': path})
        self.upload_file(self.uid, '/disk/test_folder/test1.jpg')
        self.upload_file(self.uid, '/disk/test_folder/test2.jpg')
        self.json_ok('share_create_group', {'uid': self.uid, 'path': path})
        with self.specified_client(id=random.choice(PLATFORM_DISK_APPS_IDS)):
            response = self.client.post(self.endpoint, {
                'modified_gte': '2005-08-09T18:31:42',
                'path': '/test_folder',
                'title': 'Название нового альбома'
            })
            assert response.status_code == 200
            data = from_json(response.content)
            assert 'album_id' in data
            assert 'cover' in data
            assert 'created' in data
            assert 'is_empty' in data
            assert 'is_public' in data
            assert 'items' in data
            assert 'layout' in data
            assert 'public_url' in data
            assert 'title' in data
            assert 'views_count' in data

            assert data['title'] == 'Название нового альбома'

            items = data['items']
            assert len(items) == 2
            f, s = items
            assert 'album_id' in f
            assert 'item_id' in f
            assert 'resource' in f

            resource = f['resource']
            assert 'md5' in resource
            assert 'media_type' in resource
            assert 'mime_type' in resource

            assert sorted([f['resource']['name'], s['resource']['name']]) == sorted(['test1.jpg', 'test2.jpg'])

    def test_request_without_title(self):
        """Протестировать, что при попытке создать альбом без названия будет возвращена 400 ошибка."""
        self.create_user(self.uid, noemail=True)
        path = '/disk/test_folder'
        self.json_ok('mkdir', {'uid': self.uid, 'path': path})
        self.json_ok('share_create_group', {'uid': self.uid, 'path': path})
        with self.specified_client(id=random.choice(PLATFORM_DISK_APPS_IDS)):
            response = self.client.post(self.endpoint, {
                'modified_gte': '2005-08-09T18:31:42',
                'path': '/test_folder',
            })
            assert response.status_code == 400

    def test_empty_block(self):
        """Протестировать, что при попытке создать альбом из пустого блока будет возвращена 404 ошибка."""
        self.create_user(self.uid, noemail=True)
        path = '/disk/test_folder'
        self.json_ok('mkdir', {'uid': self.uid, 'path': path})
        self.json_ok('share_create_group', {'uid': self.uid, 'path': path})
        with self.specified_client(id=random.choice(PLATFORM_DISK_APPS_IDS)):
            response = self.client.post(self.endpoint, {
                'modified_gte': '2005-08-09T18:31:42',
                'path': path.replace('/disk', '', 1),
                'title': 'Название нового альбома'
            })
            assert response.status_code == 404
            data = from_json(response.content)
            assert data['error'] == 'LentaEmptyBlockError'
            assert data['message'] == 'Полученный блок пуст.'

    def test_too_large_block(self):
        """Протестировать, что при попытке создать альбом из слишком большого блока будет возвращена 400 ошибка."""
        self.create_user(self.uid, noemail=True)
        path = '/disk/test_folder'
        self.json_ok('mkdir', {'uid': self.uid, 'path': path})
        self.upload_file(self.uid, os.path.join(path, 'test1.jpg'))
        self.upload_file(self.uid, os.path.join(path, 'test2.jpg'))
        self.json_ok('share_create_group', {'uid': self.uid, 'path': path})
        with self.specified_client(id=random.choice(PLATFORM_DISK_APPS_IDS)):
            with mock.patch('mpfs.core.lenta.logic.create_album_from_block.MAX_BLOCK_SIZE_TO_CREATE_ALBUM_FOR', 1):
                response = self.client.post(self.endpoint, {
                    'modified_gte': '2005-08-09T18:31:42',
                    'path': path.replace('/disk', '', 1),
                    'title': 'Название нового альбома'
                })
                assert response.status_code == 400
                data = from_json(response.content)
                assert data['error'] == 'LentaTooLargeBlockSizeError'
                assert data['message'] == 'Получен слишком большой блок ленты для создания альбома.'


class LentaCreateBlockHandler(UserTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.EXTERNAL
    api_version = 'v1'
    endpoint = 'disk/lenta/blocks/generate'

    def test_common(self):
        with mock.patch.object(
                LentaLoaderService, 'open_url',
                return_value=(200, {}, {})
        ) as mock_open_url:
            with self.specified_client(id=random.choice(PLATFORM_DISK_APPS_IDS)):
                response = self.client.post(self.endpoint, query={'items_limit': 20})
                # check response
                assert response.status_code == 204
                assert response.content == ''
                # check lenta call
                assert mock_open_url.call_count == 1
                args, kwargs = mock_open_url.call_args
                url = args[0]
                assert url.endswith('/api/generate-block?uid=%s&count=%s' % (self.uid, 20))


class LentaCreateBlockHandlerTestCase(UserTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.EXTERNAL
    api_version = 'v1'

    def setup_method(self, method):
        super(LentaCreateBlockHandlerTestCase, self).setup_method(method)
        self.create_user(self.uid, noemail=True)

    def _test_lenta_block_types_endpoint(self, endpoint):
        """При запросе на эту ручку (endpoint) мы должны дергать соответствующую ручку
        с пробросом всех GET-параметров."""
        assert endpoint.startswith('disk/lenta/blocks/types/')
        response_status_code = 200
        response_content = '{"only_json":"here"}'
        with mock.patch.object(
            LentaLoaderService, 'open_url',
            return_value=(response_status_code, response_content, {})
        ) as mock_open_url:
            with self.specified_client(id=random.choice(PLATFORM_DISK_APPS_IDS)):
                response = self.client.post(endpoint, {
                    'meaning_of_life': '42',
                    'vodka': 'true',
                    'balalaika': 'like',
                    # 'матрёшка': 'купить',  # FIXME: Да, с русскими упадет.
                    'version': ['1', '2']
                })
                assert response.status_code == response_status_code
                assert response.content == response_content

                assert mock_open_url.called
                args, kwargs = mock_open_url.call_args
                if 'url' in kwargs:
                    url = kwargs['url']
                else:
                    url = args[0]

                parsed_url = urlparse.urlparse(url)
                parsed_qs = urlparse.parse_qs(parsed_url.query.encode('ascii'))

                assert 'meaning_of_life' in parsed_qs
                assert parsed_qs['meaning_of_life'] == ['42']
                parsed_qs.pop('meaning_of_life')

                assert 'vodka' in parsed_qs
                assert parsed_qs['vodka'] == ['true']
                parsed_qs.pop('vodka')

                assert 'balalaika' in parsed_qs
                assert parsed_qs['balalaika'] == ['like']
                parsed_qs.pop('balalaika')

                # assert 'матрёшка'.encode('utf-8') in parsed_qs
                # assert parsed_qs['матрёшка'.encode('utf-8')] == ['купить'.encode('utf-8')]
                # parsed_qs.pop('матрёшка'.encode('utf-8'))

                assert 'version' in parsed_qs
                assert sorted(parsed_qs['version']) == ['1', '2']
                parsed_qs.pop('version')

                assert 'uid' in parsed_qs
                assert parsed_qs['uid'] == [str(self.uid)]
                parsed_qs.pop('uid')

                # проверяем что ничего лишнего не пихнули в GET-параметры
                assert not parsed_qs

                return parsed_url

    def test_block_type_proxied(self):
        """Проверить что любой тип блока `some_block_type` проксируется."""
        parsed_proxy_url = self._test_lenta_block_types_endpoint(
            'disk/lenta/blocks/types/some_block_type'
        )
        lenta_backend_api_method = parsed_proxy_url.path
        assert lenta_backend_api_method == '/api/blocks/types/some_block_type'

    def test_200_response_proxy_content_status(self):
        """Проверить что при 200-ом статус коде проксируется статус код и контент."""
        # Говорят что будет возвращаться только JSON и по умолчанию REST ставит JSON,
        # поэтому мы это даже не обрабатываем в своем коде.
        response_status_code = 200
        response_content = '{"only_json":"here"}'
        response_headers = {
            'X-Bullshit': 'Yo!',
        }
        with mock.patch.object(
            LentaLoaderService, 'open_url',
            return_value=(response_status_code, response_content, response_headers)
        ):
            with self.specified_client(id=random.choice(PLATFORM_DISK_APPS_IDS)):
                response = self.client.post('disk/lenta/blocks/types/some_method', {
                    'meaning_of_life': '42',
                    'vodka': 'true',
                    'balalaika': 'like',
                })
                assert response.status_code == response_status_code
                assert response.content == response_content
                assert 'X-Bullshit' not in response.headers
                assert 'Content-Type' in response.headers

    def test_non_200_response(self):
        """Проверить что при не 200 статус коде API возвращает 503."""
        response_status_code = 500
        response_content = 'Some shit has happened.'
        response_headers = {
            'X-Bullshit': 'Yo!'
        }
        with mock.patch.object(
            LentaLoaderService, 'open_url',
            return_value=(response_status_code, response_content, response_headers)
        ):
            with self.specified_client(id=random.choice(PLATFORM_DISK_APPS_IDS)):
                response = self.client.post('disk/lenta/blocks/types/some_method', {
                    'meaning_of_life': '42',
                    'vodka': 'true',
                    'balalaika': 'like',
                })
                assert response.status_code == 503
                assert response.content != response_content
                assert 'X-Bullshit' not in response.headers

    def test_real_uid_in_qs(self):
        """Протестировать что `uid` пробрасывается реальный, и нельзя его подменить через GET-параметры."""
        response_status_code = 200
        response_content = '{"only_json": "here"}'
        with mock.patch.object(
            LentaLoaderService, 'open_url',
            return_value=(response_status_code, response_content, {})
        ) as mock_open_url:
            with self.specified_client(id=random.choice(PLATFORM_DISK_APPS_IDS)):
                insecure_uid = '123456789'
                assert str(self.uid) != insecure_uid
                response = self.client.post('disk/lenta/blocks/types/some_block_type', {
                    'uid': insecure_uid
                })
                assert response.status_code == response_status_code

                assert mock_open_url.called
                args, kwargs = mock_open_url.call_args
                if 'url' in kwargs:
                    url = kwargs['url']
                else:
                    url = args[0]

                parsed_url = urlparse.urlparse(url)
                parsed_qs = urlparse.parse_qs(parsed_url.query)

                assert 'uid' in parsed_qs
                assert parsed_qs['uid'] == [self.uid]
                parsed_qs.pop('uid')


class LentaBlockPublicLinkHandlerTestCase(UserTestCaseMixin, UploadFileTestCaseMixin, DiskApiTestCase):
    def test_content_block_album_publication(self):
        self.create_user(self.uid, noemail=True)
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/folder'})
        resource_id = self.json_ok('info', {'uid': self.uid, 'path': '/disk/folder', 'meta': ''})['meta']['resource_id']
        self.upload_file(self.uid, '/disk/folder/test1.jpg', media_type='image')
        self.upload_file(self.uid, '/disk/folder/test2.jpg', media_type='image')

        body = {
            "id": "1",
            "files_count": 2,
            "media_type": "image",
            "folder_id": resource_id,
            "type": "content_block",
            "modifier_uid": self.uid,
            "mfrom": 0,
        }
        with self.specified_client(id=PLATFORM_DISK_APPS_IDS[0]):
            response = self.client.put('disk/lenta/resources/publish', data=body)

        assert response.status_code == 200
        response = from_json(response.content)
        assert 'album_id' in response
        assert response['is_public'] is True
        album_info = self.json_ok('album_get', {'uid': self.uid, 'album_id': response['album_id'], 'amount': 1000})
        assert len(album_info['items']) == 2

    def test_content_block_file_publication(self):
        self.create_user(self.uid, noemail=True)
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/folder'})
        resource_id = self.json_ok('info', {'uid': self.uid, 'path': '/disk/folder', 'meta': ''})['meta']['resource_id']
        self.upload_file(self.uid, '/disk/folder/test1.jpg', media_type='image')

        body = {
            "id": "1",
            "files_count": 1,
            "media_type": "image",
            "folder_id": resource_id,
            "type": "content_block",
            "modifier_uid": self.uid,
            "mfrom": 0,
        }
        with self.specified_client(id=PLATFORM_DISK_APPS_IDS[0]):
            response = self.client.put('disk/lenta/resources/publish', data=body)

        assert response.status_code == 200
        response = from_json(response.content)
        assert 'album_id' in response
        assert response['is_public'] is True
        album_info = self.json_ok('album_get', {'uid': self.uid, 'album_id': response['album_id'], 'amount': 1000})
        assert len(album_info['items']) == 1

    def test_content_block_album_max_size(self):
        self.create_user(self.uid, noemail=True)
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/folder'})
        resource_id = self.json_ok('info', {'uid': self.uid, 'path': '/disk/folder', 'meta': ''})['meta']['resource_id']
        self.upload_file(self.uid, '/disk/folder/test1.jpg', media_type='image')
        self.upload_file(self.uid, '/disk/folder/test2.jpg', media_type='image')
        self.upload_file(self.uid, '/disk/folder/test3.jpg', media_type='image')

        body = {
            "id": "1",
            "files_count": 3,
            "media_type": "image",
            "folder_id": resource_id,
            "type": "content_block",
            "modifier_uid": self.uid,
            "mfrom": 0,
        }
        with mock.patch('mpfs.core.lenta.logic.lenta_block_public_link.MAX_BLOCK_SIZE_TO_CREATE_ALBUM_FOR', 2), \
                self.specified_client(id=PLATFORM_DISK_APPS_IDS[0]):
            response = self.client.put('disk/lenta/resources/publish', data=body)

        assert response.status_code == 200
        response = from_json(response.content)
        assert 'album_id' in response
        assert response['is_public'] is True
        album_info = self.json_ok('album_get', {'uid': self.uid, 'album_id': response['album_id'], 'amount': 1000})
        assert len(album_info['items']) == 2

    @parameterized.expand([
        ('photo_remind_block'),
        ('photo_selection_block'),
    ])
    def test_album_publication(self, block_type):
        self.create_user(self.uid, noemail=True)
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/folder'})
        self.upload_file(self.uid, '/disk/folder/test1.jpg', media_type='image')
        self.upload_file(self.uid, '/disk/folder/test2.jpg', media_type='image')
        resource_id_1 = self.json_ok('info', {'uid': self.uid, 'path': '/disk/folder/test1.jpg', 'meta': ''})['meta']['resource_id']
        resource_id_2 = self.json_ok('info', {'uid': self.uid, 'path': '/disk/folder/test2.jpg', 'meta': ''})['meta']['resource_id']

        body = {
            "id": "1",
            "resource_ids": [
                resource_id_1,
                resource_id_2,
            ],
            "type": block_type,
        }
        with self.specified_client(id=PLATFORM_DISK_APPS_IDS[0]):
            response = self.client.put('disk/lenta/resources/publish', data=body)

        assert response.status_code == 200
        response = from_json(response.content)
        assert 'album_id' in response
        assert response['is_public'] is True
        album_info = self.json_ok('album_get', {'uid': self.uid, 'album_id': response['album_id'], 'amount': 1000})
        assert len(album_info['items']) == 2

    @parameterized.expand([
        ('photo_remind_block'),
        ('photo_selection_block'),
    ])
    def test_file_publication(self, block_type):
        self.create_user(self.uid, noemail=True)
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/folder'})
        self.upload_file(self.uid, '/disk/folder/test1.jpg', media_type='image')
        resource_id = self.json_ok('info', {'uid': self.uid, 'path': '/disk/folder/test1.jpg', 'meta': ''})['meta']['resource_id']

        body = {
            "id": "1",
            "resource_ids": [
                resource_id,
            ],
            "type": block_type,
        }
        with self.specified_client(id=PLATFORM_DISK_APPS_IDS[0]):
            response = self.client.put('disk/lenta/resources/publish', data=body)

        assert response.status_code == 200
        response = from_json(response.content)
        assert 'album_id' in response
        assert response['is_public'] is True
        album_info = self.json_ok('album_get', {'uid': self.uid, 'album_id': response['album_id'], 'amount': 1000})
        assert len(album_info['items']) == 1

    @parameterized.expand([
        ('photo_remind_block'),
        ('photo_selection_block'),
    ])
    def test_album_max_size(self, block_type):
        self.create_user(self.uid, noemail=True)
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/folder'})
        self.upload_file(self.uid, '/disk/folder/test1.jpg', media_type='image')
        self.upload_file(self.uid, '/disk/folder/test2.jpg', media_type='image')
        self.upload_file(self.uid, '/disk/folder/test3.jpg', media_type='image')
        resource_id_1 = self.json_ok('info', {'uid': self.uid, 'path': '/disk/folder/test1.jpg', 'meta': ''})['meta']['resource_id']
        resource_id_2 = self.json_ok('info', {'uid': self.uid, 'path': '/disk/folder/test2.jpg', 'meta': ''})['meta']['resource_id']
        resource_id_3 = self.json_ok('info', {'uid': self.uid, 'path': '/disk/folder/test3.jpg', 'meta': ''})['meta']['resource_id']

        body = {
            "id": "1",
            "resource_ids": [
                resource_id_1,
                resource_id_2,
                resource_id_3,
            ],
            "type": block_type,
        }
        with mock.patch('mpfs.core.lenta.logic.lenta_block_public_link.MAX_BLOCK_SIZE_TO_CREATE_ALBUM_FOR', 2), \
                self.specified_client(id=PLATFORM_DISK_APPS_IDS[0]):
            response = self.client.put('disk/lenta/resources/publish', data=body)

        assert response.status_code == 200
        response = from_json(response.content)
        assert 'album_id' in response
        assert response['is_public'] is True
        album_info = self.json_ok('album_get', {'uid': self.uid, 'album_id': response['album_id'], 'amount': 1000})
        assert len(album_info['items']) == 2
