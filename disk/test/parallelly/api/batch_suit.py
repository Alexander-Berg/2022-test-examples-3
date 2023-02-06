# -*- coding: utf-8 -*-

import hashlib
import json
import pytest
import random
import mock

from nose_parameterized import parameterized

from test.base_suit import UploadFileTestCaseMixin, UserTestCaseMixin
from test.parallelly.api.disk.base import DiskApiTestCase
from test.parallelly.api.fixtures.dataapi_fixtures import (
    post_dataapi_api_batch_get_profile_geopoints_address_id_get_profile_batch_addresses_geopoints,
    post_dataapi_api_batch_get_generic_profile,
    post_dataapi_api_batch_get_profile_addresses,
    post_dataapi_api_batch_post_profile_addresses,
    post_dataapi_api_batch_get_profile_addresses_address_id,
    post_dataapi_api_batch_delete_profile_adresses_address_id,
    post_dataapi_api_batch_patch_profile_adresses_address_id,
    post_dataapi_batch_put_profile_addresses_address_id,
    post_dataapi_batch_put_profile_addresses_address_id_touch,
    post_dataapi_batch_put_profile_addresses_address_id_tag,
    post_dataapi_batch_put_profile_addresses_address_id_untag
)
from test.fixtures.users import default_user
from mpfs.common.static import tags
from mpfs.common.util import to_json, from_json
from mpfs.core.services.data_api_service import data_api
from mpfs.core.services.data_api_profile_service import data_api_profile


class BatchRequestsTestCase(DiskApiTestCase):
    api_mode = tags.platform.INTERNAL
    api_version = 'v1'
    method = 'POST'
    url = 'batch/request'
    uid = default_user.uid
    yandexuid = 'yaid-703496061386152999'

    def test_batch_request(self):
        req = {
            'items': [
                {
                    'method': 'GET',
                    'relative_url': '/v1/%s/disk?client_id=123&client_name=123' % self.uid,
                },
                {
                    'method': 'GET',
                    'relative_url': '/v1/%s/404test?client_id=123&client_name=123' % self.uid,
                },
                {
                    'method': 'GET',
                    'relative_url': '/v1/%s/disk/resources?path=/&client_id=123&client_name=123' % self.uid,
                }
            ]
        }
        resp = self.client.request(self.method, self.url, data=json.dumps(req), query={'fields': 'items.code'}, uid=self.uid)
        result = json.loads(resp.content)

        responses = result['items']
        assert responses[0]['code'] == 200
        assert responses[1]['code'] == 404
        assert responses[2]['code'] == 200

    def test_personality_batch_request(self):
        req = {
            'items': [
                {
                    'method': 'GET',
                    'relative_url': '/v1/%s/personality/profile/addresses/source?client_id=123&client_name=123' % self.uid,
                },
                {
                    'method': 'GET',
                    'relative_url': '/v1/%s/personality/profile/events/flights/actual?client_id=123&client_name=123' % self.uid,
                },
                {
                    'method': 'GET',
                    'relative_url': '/v2/%s/personality/profile/addresses?client_id=123&client_name=123' % self.uid,
                },
            ]
        }
        resp = self.client.request(self.method, self.url, data=json.dumps(req), query={}, uid=self.uid)
        result = json.loads(resp.content)

        responses = result['items']
        for i in xrange(len(req['items'])):
            assert responses[i]['code'] == 200

    def test_forbid_nested_chaining(self):
        """Проверяем, что запрещено использовать чейнинг в батче"""
        req = {
            'items': [
                {
                    'method': 'POST',
                    'relative_url': '/v1/%s/chaining/request' % self.uid,
                    'body': {
                        'method': 'GET',
                        'relative_url': '/v1/%s/disk/resources?path=%%2F' % self.uid,
                        'subrequests': []
                    },
                    'subrequests': []
                },
            ]
        }
        resp = self.client.request(self.method, self.url, data=json.dumps(req), uid=self.uid)
        assert resp.status_code == 400

        req = {
            'items': [
                {
                    'method': 'POST',
                    'relative_url': '/v1/%s/chaining/request?test=arg&another=1' % self.uid,
                    'body': {
                        'method': 'GET',
                        'relative_url': '/v1/%s/disk/resources?path=%%2F' % self.uid,
                        'subrequests': []
                    },
                    'subrequests': []
                },
            ]
        }
        resp = self.client.request(self.method, self.url, data=json.dumps(req), uid=self.uid)
        assert resp.status_code == 400


class ResourcesBatchRequestsHandlerTestCase(UserTestCaseMixin, UploadFileTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.INTERNAL
    api_version = 'v1'
    method = 'POST'
    url = 'batch/request'
    uid = default_user.uid
    yandexuid = 'yaid-703496061386152999'

    def test_batch_request(self):
        self.create_user(self.uid)
        paths = (
            '/disk/1.jpg',
            '/disk/2.jpg',
        )
        api_paths = (
            '1.jpg',
            '2.jpg',
        )
        for path in paths:
            self.upload_file(self.uid, path)

        req = {
            'items': [
                {
                    'method': 'GET',
                    'relative_url': '/v1/%s/disk/resources?path=%s' % (self.uid, api_paths[0]),
                },
                {
                    'method': 'GET',
                    'relative_url': '/v1/%s/disk/resources?path=%s' % (self.uid, 'doesnt_exist.jpg'),
                },
                {
                    'method': 'GET',
                    'relative_url': '/v1/%s/disk/resources?path=%s' % (self.uid, api_paths[1]),
                },
                {
                    'method': 'GET',
                    'relative_url': '/v1/%s/disk/resources?path=%s' % (self.uid, 'doesnt_exist_2.jpg'),
                },
            ]
        }
        resp = self.client.request(self.method, self.url, data=json.dumps(req), uid=self.uid)
        result = from_json(resp.content)
        assert len(result['items']) == 4
        assert from_json(result['items'][0]['body'])['name'] == '1.jpg'
        assert result['items'][1]['code'] == 404
        assert from_json(result['items'][2]['body'])['name'] == '2.jpg'
        assert result['items'][3]['code'] == 404

    def test_batch_folders_not_implemented(self):
        self.create_user(self.uid)
        self.upload_file(self.uid, '/disk/1.jpg')
        self.json_ok('mkdir', {'uid': self.uid, 'path': '/disk/2'})

        req = {
            'items': [
                {
                    'method': 'GET',
                    'relative_url': '/v1/%s/disk/resources?path=%s' % (self.uid, '2'),
                },
                {
                    'method': 'GET',
                    'relative_url': '/v1/%s/disk/resources?path=%s' % (self.uid, '1.jpg'),
                },
            ]
        }
        resp = self.client.request(self.method, self.url, data=json.dumps(req), uid=self.uid)
        result = from_json(resp.content)
        assert len(result['items']) == 2
        assert result['items'][0]['code'] == 500
        assert result['items'][1]['code'] == 200


class BatchRequestsAccessTestCase(DiskApiTestCase):
    api_mode = tags.platform.EXTERNAL
    api_version = 'v1'
    method = 'POST'
    url = 'batch/request'
    uid = default_user.uid

    @parameterized.expand([
        (['yadisk:all'], 200),
        (['cloud_api:disk.batch'], 200),
        (['cloud_api:disk.info'], 403),
        ([], 403),
    ])
    def test_batch_request_access(self, scopes, expected_status):
        req = {
            'items': [
                {
                    'method': 'GET',
                    'relative_url': '/v1/disk',
                }
            ]
        }

        with self.specified_client(scopes=scopes):
            resp = self.client.request(self.method, self.url, data=json.dumps(req), uid=self.uid)
        assert expected_status == resp.status_code


class PersonalityBatchRequestHandlerTestCase(DiskApiTestCase):
    """Набор тестов для тестирования склейки запросов в v1/profile и v2/profile внутри батчевой ручки."""
    api_mode = tags.platform.INTERNAL
    api_version = 'v1'
    method = 'POST'
    url = 'batch/request'
    uid = default_user.uid

    @staticmethod
    def get_mocked_data_api_open_url(service, one_time_response):
        original_open_url = service.open_url
        mocked_open_url_call_cnt = [0]

        def open_url(*args, **kwargs):
            """Подменяет ответ `open_url` только один раз.
            Остальные обрабатываются оригинальной функцией."""
            mocked_open_url_call_cnt[0] += 1
            if mocked_open_url_call_cnt[0] > 1:
                return original_open_url(*args, **kwargs)
            else:
                response = to_json(one_time_response)
                if kwargs.get('return_status', False):
                    return 200, response, {}
                else:
                    return response

        return open_url

    def test_personality_batch_request_with_addresses_and_geopoints(self):
        """
        Делаем несколько запросов через батчевую ручку в
        /v1/personality/profile/addresses и проверяем что всё правильно распарсилось и отработало.
        Третий запрос идет сам в датаапи в батчевую ручку и возвращает 2 результата, поэтому
        необходимо его обязательно проверить тоже внимательно.
        """
        uid = self.uid
        req = {
            'items': [
                {
                    'method': 'GET',
                    'relative_url': '/v1/%s/personality/profile/addresses/bullshit/%s' % (uid, 'work=home=0'),
                    'headers': {'Content-type': 'application/json'}
                },
                {
                    'method': 'GET',
                    'relative_url': '/v1/%s/personality/profile/addresses' % uid,
                    'headers': {'Content-type': 'application/json'}
                },
                {
                    'method': 'GET',
                    'relative_url': '/v1/%s/personality/profile/addresses/bullshit/work' % uid,
                    'headers': {'Content-type': 'application/json'}
                }
            ]
        }

        with mock.patch(
            'mpfs.core.services.data_api_service.DataApiService.open_url',
            wraps=self.get_mocked_data_api_open_url(
                data_api,
                post_dataapi_api_batch_get_profile_geopoints_address_id_get_profile_batch_addresses_geopoints()
            )
        ):
            response = self.client.request(self.method, self.url, data=json.dumps(req), query={}, uid=uid)
            result = json.loads(response.content)
            result_items = result['items']
            first_request_result = result_items[0]
            second_request_result = result_items[1]
            third_request_result = result_items[2]

            assert first_request_result['code'] == 200
            assert second_request_result['code'] == 200
            assert third_request_result['code'] == 200

            first_request_result_body = json.loads(first_request_result['body'])
            second_request_result_body = json.loads(second_request_result['body'])
            third_request_result_body = json.loads(third_request_result['body'])

            assert first_request_result_body['latitude'] == 55.749776
            assert first_request_result_body['data_key'] == 'work=home=0'
            assert first_request_result_body['longitude'] == 37.617068

            assert len(second_request_result_body['items']) == 3
            assert second_request_result_body['items'][0]['data_key'] == 'work'
            assert second_request_result_body['items'][0]['address_line'] == u'Россия, Москва'
            assert second_request_result_body['items'][1]['data_key'] == 'work'
            assert second_request_result_body['items'][2]['data_key'] == 'work=home=0'

            assert third_request_result_body['data_key'] == 'work'

    def test_one_rate_limiter_request(self):
        uid = self.uid
        req = {
            'items': [
                {
                    'method': 'GET',
                    'relative_url': '/v1/%s/personality/profile/addresses/bullshit/%s' % (uid, 'work=home=0'),
                    'headers': {'Content-type': 'application/json'}
                },
                {
                    'method': 'GET',
                    'relative_url': '/v1/%s/personality/profile/addresses' % uid,
                    'headers': {'Content-type': 'application/json'}
                },
                {
                    'method': 'GET',
                    'relative_url': '/v1/%s/personality/profile/addresses/bullshit/work' % uid,
                    'headers': {'Content-type': 'application/json'}
                }
            ]
        }

        with mock.patch('mpfs.core.services.rate_limiter_service.RateLimiterService.is_limit_exceeded',
                        return_value=False) as mocked_rate_limiter, \
            mock.patch(
                'mpfs.core.services.data_api_service.DataApiService.open_url',
                wraps=self.get_mocked_data_api_open_url(
                    data_api,
                    post_dataapi_api_batch_get_profile_geopoints_address_id_get_profile_batch_addresses_geopoints()
                )):
            resp = self.client.request(self.method, self.url, data=json.dumps(req), query={}, uid=self.uid)

            assert resp.status_code == 200

        mocked_rate_limiter.assert_called_once()
        _, _, value = mocked_rate_limiter.call_args[0]
        assert value == 4

    def test_429_on_batch_request(self):
        uid = self.uid
        req = {
            'items': [
                {
                    'method': 'GET',
                    'relative_url': '/v1/%s/personality/profile/morda/usersettings?cheburek=true' % uid,
                    'headers': {'Content-type': 'application/json'}
                },
            ]
        }

        with mock.patch('mpfs.core.services.rate_limiter_service.RateLimiterService.is_limit_exceeded',
                        return_value=True), \
            mock.patch('mpfs.core.services.data_api_service.DataApiService.open_url',
                       wraps=self.get_mocked_data_api_open_url(data_api, post_dataapi_api_batch_get_generic_profile())):
            response = self.client.request(self.method, self.url, data=json.dumps(req), query={}, uid=uid)
            assert response.status_code == 429

    def test_personality_batch_with_generic(self):
        uid = self.uid
        req = {
            'items': [
                {
                    'method': 'GET',
                    'relative_url': '/v1/%s/personality/profile/morda/usersettings?cheburek=true' % uid,
                    'headers': {'Content-type': 'application/json'}
                },
            ]
        }

        with mock.patch(
            'mpfs.core.services.data_api_service.DataApiService.open_url',
            wraps=self.get_mocked_data_api_open_url(data_api, post_dataapi_api_batch_get_generic_profile())
        ), mock.patch('mpfs.core.services.rate_limiter_service.RateLimiterService.is_limit_exceeded',
                      return_value=False):
            response = self.client.request(self.method, self.url, data=json.dumps(req), query={}, uid=uid)
            result = json.loads(response.content)
            result_items = result['items']
            assert len(result_items) == 1
            request_result_body = json.loads(result_items[0]['body'])
            assert request_result_body['items'] == [{'top': 'kek'}]

    def test_get_v2_profile_addresses(self):
        """
        Просмотреть адреса пользователя.
        Протестировать что одиночный запрос в
        GET v2/profile/addresses внутри батчевой ручки работает корректно.
        """
        uid = self.uid
        req = {
            'items': [
                {
                    'method': 'GET',
                    'relative_url': '/v2/%s/personality/profile/addresses' % uid,
                    'headers': {'Content-type': 'application/json'}
                },
            ]
        }
        one_time_response = post_dataapi_api_batch_get_profile_addresses()
        with mock.patch(
            'mpfs.core.services.data_api_service.DataApiService.open_url',
            wraps=self.get_mocked_data_api_open_url(data_api, one_time_response)
        ):
            response = self.client.request(self.method, self.url, data=json.dumps(req), query={}, uid=uid)
            result = json.loads(response.content)
            print result
            result_items = result['items']
            assert len(result_items) == 1
            request_result_body = json.loads(result_items[0]['body'])
            assert len(request_result_body['items']) == 1
            [address_data] = request_result_body['items']
            assert address_data['address_line'] == u'Россия, Пермский край, Оса, улица Пугачёва, 22'

    def test_post_v2_profile_addresses(self):
        """
        Создать адрес пользователя.
        Протестировать что одиночный запрос в
        POST v2/profile/addresses внутри батчевой ручки работает корректно.
        """
        uid = self.uid
        req = {
            'items': [
                {
                    'method': 'POST',
                    'relative_url': '/v2/%s/personality/profile/addresses' % uid,
                    'headers': {'Content-type': 'application/json'},
                    'body': to_json({
                        "address_id": "home",
                        "title": "Дом",
                        "longitude": 55.4513816833,
                        "address_line_short": "улица Пугачёва, 22",
                        "latitude": 57.2757987976,
                        "address_line": "Россия, Пермский край, Оса, улица Пугачёва, 22",
                        "entrance_number": "2",
                        "custom_metadata": '{"smth": "meta"}'
                    })
                }
            ]
        }
        one_time_response = post_dataapi_api_batch_post_profile_addresses()
        with mock.patch(
            'mpfs.core.services.data_api_service.DataApiService.open_url',
            wraps=self.get_mocked_data_api_open_url(data_api, one_time_response)
        ):
            response = self.client.request(self.method, self.url, data=json.dumps(req), query={}, uid=uid)
            result = json.loads(response.content)
            result_items = result['items']
            assert len(result_items) == 1
            request_result_body = json.loads(result_items[0]['body'])
            assert request_result_body['address_line'] == u'Россия, Пермский край, Оса, улица Пугачёва, 22'

    def test_get_v2_profile_addresses_address_id(self):
        """
        Получить инфу по конкретному адресу.
        Протестировать что одиночный запрос в
        GET v2/profile/addresses/<address_id> внутри батчевой ручки работает корректно.
        """
        uid = self.uid
        req = {
            'items': [
                {
                    'method': 'GET',
                    'relative_url': '/v2/%s/personality/profile/addresses/%s' % (uid, 'home'),
                    'headers': {'Content-type': 'application/json'},
                }
            ]
        }
        one_time_response = post_dataapi_api_batch_get_profile_addresses_address_id()
        with mock.patch(
            'mpfs.core.services.data_api_service.DataApiService.open_url',
            wraps=self.get_mocked_data_api_open_url(data_api, one_time_response)
        ):
            response = self.client.request(self.method, self.url, data=json.dumps(req), query={}, uid=uid)
            result = json.loads(response.content)
            result_items = result['items']
            assert len(result_items) == 1
            request_result_body = json.loads(result_items[0]['body'])
            assert request_result_body['address_line'] == u'Россия, Пермский край, Оса, улица Пугачёва, 22'

    def test_delete_v2_profile_addresses_address_id(self):
        """
        Удалить конкретный адрес.
        Протестировать что одиночный запрос в
        DELETE v2/profile/addresses/<address_id> внутри батчевой ручки работает корректно.
        """
        uid = self.uid
        req = {
            'items': [
                {
                    'method': 'DELETE',
                    'relative_url': '/v2/%s/personality/profile/addresses/%s' % (uid, 'home'),
                    'headers': {'Content-type': 'application/json'},
                }
            ]
        }
        one_time_response = post_dataapi_api_batch_delete_profile_adresses_address_id()
        with mock.patch(
            'mpfs.core.services.data_api_service.DataApiService.open_url',
            wraps=self.get_mocked_data_api_open_url(data_api, one_time_response)
        ):
            response = self.client.request(self.method, self.url, data=json.dumps(req), query={}, uid=uid)
            result = json.loads(response.content)
            result_items = result['items']
            request_result_body = json.loads(result_items[0]['body'])
            assert request_result_body == {}

    def test_put_v2_profile_addresses_address_id(self):
        """
        Изменить конкретный адрес.
        Протестировать что одиночный запрос в
        PUT v2/profile/addresses/<address_id> внутри батчевой ручки работает корректно.
        """
        uid = self.uid
        req = {
            'items': [
                {
                    'method': 'PUT',
                    'relative_url': '/v2/%s/personality/profile/addresses/%s' % (uid, 'home'),
                    'headers': {'Content-type': 'application/json'},
                    'body': to_json({
                        "title": "Дом2",
                        "longitude": 55.4513816833,
                        "address_line_short": "улица Пугачёва, 23",
                        "latitude": 57.2757987976,
                        "address_line": "Россия, Пермский край, Оса, улица Пугачёва, 23",
                        "entrance_number": "2",
                        "custom_metadata": '{"smth": "meta"}'
                    })
                }
            ]
        }
        one_time_response = post_dataapi_batch_put_profile_addresses_address_id()
        with mock.patch(
            'mpfs.core.services.data_api_service.DataApiService.open_url',
            wraps=self.get_mocked_data_api_open_url(data_api, one_time_response)
        ):
            response = self.client.request(self.method, self.url, data=json.dumps(req), query={}, uid=uid)
            result = json.loads(response.content)
            result_items = result['items']
            request_result_body = json.loads(result_items[0]['body'])
            assert 'href' in request_result_body

    def test_patch_v2_profile_addresses_address_id(self):
        """
        Изменить конкретный адрес частично.
        Протестировать что одиночный запрос в
        PATCH v2/profile/addresses/<address_id> внутри батчевой ручки работает корректно.
        """
        uid = self.uid
        req = {
            'items': [
                {
                    'method': 'PATCH',
                    'relative_url': '/v2/%s/personality/profile/addresses/%s' % (uid, 'home'),
                    'headers': {'Content-type': 'application/json'},
                    'body': to_json({
                        "address_line": "Россия, Пермский край, Оса, улица Пугачёва, 23"
                    })
                }
            ]
        }
        one_time_response = post_dataapi_api_batch_patch_profile_adresses_address_id()
        with mock.patch(
            'mpfs.core.services.data_api_service.DataApiService.open_url',
            wraps=self.get_mocked_data_api_open_url(data_api, one_time_response)
        ):
            response = self.client.request(self.method, self.url, data=json.dumps(req), query={}, uid=uid)
            result = json.loads(response.content)
            result_items = result['items']
            request_result_body = json.loads(result_items[0]['body'])
            assert 'href' in request_result_body

    def test_post_v2_profile_addresses_address_id_touch(self):
        """
        Обновить дату последнего использования адреса.
        Протестировать что одиночный запрос в
        POST v2/profile/addresses/<address_id>/touch внутри батчевой ручки работает корректно.
        """
        uid = self.uid
        req = {
            'items': [
                {
                    'method': 'POST',
                    'relative_url': '/v2/%s/personality/profile/addresses/%s/touch' % (uid, 'home'),
                    'headers': {'Content-type': 'application/json'},
                }
            ]
        }
        one_time_response = post_dataapi_batch_put_profile_addresses_address_id_touch()
        with mock.patch(
            'mpfs.core.services.data_api_service.DataApiService.open_url',
            wraps=self.get_mocked_data_api_open_url(data_api, one_time_response)
        ):
            response = self.client.request(self.method, self.url, data=json.dumps(req), query={}, uid=uid)
            result = json.loads(response.content)
            result_items = result['items']
            request_result_body = json.loads(result_items[0]['body'])
            assert 'href' in request_result_body

    def test_put_v2_profile_addresses_address_id_tag(self):
        """
        Добавить тег к адресу.
        Протестировать что одиночный запрос в
        PUT v2/profile/addresses/<address_id>/tag внутри батчевой ручки работает корректно.
        """
        uid = self.uid
        req = {
            'items': [
                {
                    'method': 'PUT',
                    'relative_url': '/v2/%s/personality/profile/addresses/%s/tag?tags=lol,kek,cheburek' % (uid, 'home'),
                    'headers': {'Content-type': 'application/json'},
                }
            ]
        }
        one_time_response = post_dataapi_batch_put_profile_addresses_address_id_tag()
        with mock.patch(
            'mpfs.core.services.data_api_service.DataApiService.open_url',
            wraps=self.get_mocked_data_api_open_url(data_api, one_time_response)
        ):
            response = self.client.request(self.method, self.url, data=json.dumps(req), query={}, uid=uid)
            result = json.loads(response.content)
            result_items = result['items']
            request_result_body = json.loads(result_items[0]['body'])
            assert 'href' in request_result_body

    def test_post_in_batch_accepts_json(self):
        """
        Создать адрес пользователя.
        Протестировать что одиночный запрос в
        POST v2/profile/addresses внутри батчевой ручки работает корректно.
        """
        uid = self.uid
        req = {
            'items': [
                {
                    'method': 'POST',
                    'relative_url': '/v1/%s/personality/profile/morda/usersettings/route' % uid,
                    'headers': {'Content-type': 'application/json'},
                    'body': {"address_id": "home"},
                }
            ]
        }
        one_time_response = post_dataapi_api_batch_post_profile_addresses()
        with mock.patch(
            'mpfs.core.services.data_api_service.DataApiService.open_url',
            wraps=self.get_mocked_data_api_open_url(data_api, one_time_response)
        ) as open_url_mock:
            self.client.request(self.method, self.url, data=json.dumps(req), query={}, uid=uid)
            assert isinstance(
                from_json(from_json(open_url_mock.call_args_list[0][1]['pure_data'])['items'][0]['body']), dict)

    def test_post_in_batch_accepts_escaped_json(self):
        """
        Создать адрес пользователя.
        Протестировать что одиночный запрос в
        POST v2/profile/addresses внутри батчевой ручки работает корректно.
        """
        uid = self.uid
        req = {
            'items': [
                {
                    'method': 'POST',
                    'relative_url': '/v1/%s/personality/profile/morda/usersettings/route' % uid,
                    'headers': {'Content-type': 'application/json'},
                    'body': "\"{\"address_id\": \"home\"}\"",
                }
            ]
        }
        one_time_response = post_dataapi_api_batch_post_profile_addresses()
        with mock.patch(
            'mpfs.core.services.data_api_service.DataApiService.open_url',
            wraps=self.get_mocked_data_api_open_url(data_api, one_time_response)
        ) as open_url_mock:
            self.client.request(self.method, self.url, data=json.dumps(req), query={}, uid=uid)
            assert isinstance(
                from_json(from_json(open_url_mock.call_args_list[0][1]['pure_data'])['items'][0]['body']), dict)

    def test_put_v2_profile_addresses_address_id_untag(self):
        """
        Удалить теги у адреса.
        Протестировать что одиночный запрос в
        PUT v2/profile/addresses/<address_id>/untag внутри батчевой ручки работает корректно.
        """
        uid = self.uid
        req = {
            'items': [
                {
                    'method': 'PUT',
                    'relative_url': '/v2/%s/personality/profile/addresses/%s/tag?tags=lol,cheburek' % (uid, 'home'),
                    'headers': {'Content-type': 'application/json'},
                }
            ]
        }
        one_time_response = post_dataapi_batch_put_profile_addresses_address_id_untag()
        with mock.patch(
            'mpfs.core.services.data_api_service.DataApiService.open_url',
            wraps=self.get_mocked_data_api_open_url(data_api, one_time_response)
        ):
            response = self.client.request(self.method, self.url, data=json.dumps(req), query={}, uid=uid)
            result = json.loads(response.content)
            result_items = result['items']
            request_result_body = json.loads(result_items[0]['body'])
            assert 'href' in request_result_body
