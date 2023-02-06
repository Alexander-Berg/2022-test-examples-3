# -*- coding: utf-8 -*-

import random
import mock
import urlparse

from mpfs.config import settings
from mpfs.common.util import from_json, to_json
from test.parallelly.api.disk.base import DiskApiTestCase
from test.base_suit import UserTestCaseMixin
from mpfs.common.static import tags
from mpfs.core.services import notes_service
from mpfs.platform.v1.notes.permissions import NotesReadPermission, NotesWritePermission
from mpfs.platform.v1.disk.permissions import WebDavPermission


PLATFORM_DISK_APPS_IDS = settings.platform['disk_apps_ids']


class NotesProxyHandlerTestCase(UserTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.EXTERNAL
    api_version = 'v1'
    endpoint = 'notes'

    def setup_method(self, method):
        super(NotesProxyHandlerTestCase, self).setup_method(method)
        notes_service.notes.log = self.log

    def test_real_uid_passed_to_service(self):
        """Протестировать что удаляются переданные извне GET-параметры `uid` и `__uid` и передается реальный `uid__`."""
        uid = self.uid
        with self.specified_client(scopes=NotesReadPermission.scopes + NotesWritePermission.scopes, uid=uid):
            wrong_uid = '123456'
            assert wrong_uid != uid
            service_response = to_json({
                'items': [
                    {
                        'title': 'my test note', 'mtime': '2017-06-05T11:15:39.546Z', 'tags': [], 'id': '122_17',
                        'ctime': '2017-06-05T11:24:18.709Z'
                    },
                    {
                        'title': 'my test note', 'mtime': '2017-06-05T11:15:39.546Z', 'tags': [], 'id': '122_19',
                        'ctime': '2017-06-05T11:31:48.113Z'
                    },
                    {
                        'title': 'my test note', 'mtime': '2017-06-05T11:15:39.546Z', 'tags': [], 'id': '122_20',
                        'ctime': '2017-06-05T11:31:49.184Z'
                    },
                    {
                        'title': 'my test note', 'mtime': '2017-06-05T11:15:39.546Z', 'tags': [], 'id': '122_22',
                        'ctime': '2017-06-05T11:31:50.184Z'
                    },
                    {
                        'title': 'my test note', 'mtime': '2017-06-05T11:15:39.546Z', 'tags': [], 'id': '122_23',
                        'ctime': '2017-06-05T11:31:50.623Z'
                    }
                ]
            })
            with mock.patch.object(
                notes_service.notes, 'open_url', return_value=(200, service_response, {})
            ) as mocked_open_url:
                response = self.client.request('GET', self.endpoint + '/notes/?uid=%s&__uid=%s' % (wrong_uid, wrong_uid))
                assert response.status_code == 200
                assert mocked_open_url.called
                args, kwargs = mocked_open_url.call_args
                (url,) = args
                parsed_url = urlparse.urlparse(url)
                assert parsed_url.path == '/api/notes/'
                parsed_query = urlparse.parse_qs(parsed_url.query)
                assert '__uid' in parsed_query
                assert parsed_query['__uid'] == [uid]

    def test_proxied_path_and_get_params_passed_correctly_to_service(self):
        """Протестировать что проксируемый путь передается корректно и GET-параметры тоже."""
        uid = self.uid
        with self.specified_client(scopes=WebDavPermission.scopes, uid=uid):
            with mock.patch.object(
                notes_service.notes, 'open_url', return_value=(200, '[]', {})
            ) as mocked_open_url:
                response = self.client.request(
                    'GET',
                    self.endpoint + '/tags/and/some/path/to?vodka=true&balalaika=cool&matreshka=like'
                )
                assert response.status_code == 200
                assert mocked_open_url.called
                args, kwargs = mocked_open_url.call_args
                (url,) = args
                parsed_url = urlparse.urlparse(url)
                assert parsed_url.path == '/api/tags/and/some/path/to'
                parsed_query = urlparse.parse_qs(parsed_url.query)
                assert '__uid' in parsed_query
                assert parsed_query['__uid'] == [uid]
                parsed_query.pop('__uid')

                assert 'vodka' in parsed_query
                assert parsed_query['vodka'] == ['true']
                parsed_query.pop('vodka')

                assert 'balalaika' in parsed_query
                assert parsed_query['balalaika'] == ['cool']
                parsed_query.pop('balalaika')

                assert 'matreshka' in parsed_query
                assert parsed_query['matreshka'] == ['like']
                parsed_query.pop('matreshka')

                # проверяем что не передали ничего лишнего
                assert not parsed_query

    def test_method_passed_to_service(self):
        """Проверить что метод запроса и даныне передаются корректно во внешний сервис."""
        uid = self.uid
        with self.specified_client(scopes=WebDavPermission.scopes, uid=uid):
            service_response = to_json({'type': 'text', 'value': 'wp', 'id': 41})
            with mock.patch.object(
                notes_service.notes, 'open_url', return_value=(200, service_response, {})
            ) as mocked_open_url:
                response = self.client.request(
                    'PUT',
                    self.endpoint + '/tags',
                    headers={
                        'Content-Type': 'application/json',
                        'Host': 'api.disk.yandex.net',
                        'Authorization': 'OAuth token',
                        'X-Custom-Header': 'value',
                        'Accept-Encoding': 'gzip',
                        'Connection': 'keep-alive',
                    },
                    data={
                        'type': 'text',
                        'value': 'wp',
                    }
                )
                assert response.content == service_response
                assert mocked_open_url.called
                args, kwargs = mocked_open_url.call_args
                headers = kwargs['headers']
                method = kwargs['method']
                data = from_json(kwargs['pure_data'])

                assert method == 'PUT'
                assert 'Host' not in headers
                assert 'Authorization' not in headers
                assert 'Accept-Encoding' not in headers
                assert 'Connection' not in headers
                assert 'X-Custom-Header' in headers  # пробрасываем все заголовки
                assert 'X-Forwarded-For' in headers
                assert 'Content-Type' in headers
                assert 'Accept' in headers
                assert data['type'] == 'text'
                assert data['value'] == 'wp'

    def test_with_unauthorized_client(self):
        response = self.client.request(
            'PUT',
            self.endpoint + '/tags',
            headers={
                'Content-Type': 'application/json',
                'X-Some-Bullshit': 'kek'
            },
            data={
                'type': 'text',
                'value': 'wp'
            }
        )
        assert response.status_code == 401

    def test_notes_backend_service_response_headers_proxied_back(self):
        uid = self.uid
        with self.specified_client(scopes=WebDavPermission.scopes, uid=uid):
            service_response = to_json({'type': 'text', 'value': 'wp', 'id': 41})
            with mock.patch.object(
                notes_service.notes, 'open_url', return_value=(200, service_response, {'X-Top-Kek': 'Lul!'})
            ):
                response = self.client.request(
                    'PUT',
                    self.endpoint + '/tags',
                    headers={
                        'Content-Type': 'application/json',
                        'X-Some-Bullshit': 'kek'
                    },
                    data={
                        'type': 'text',
                        'value': 'wp'
                    }
                )
                assert 'X-Top-Kek' in response.headers
                assert response.headers['X-Top-Kek'] == 'Lul!'
