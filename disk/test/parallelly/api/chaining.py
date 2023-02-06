# -*- coding: utf-8 -*-
import json
import mock

from test.parallelly.api.disk.base import DiskApiTestCase
from test.fixtures.users import default_user
from mpfs.common.static import tags
from mpfs.config import settings


PLATFORM_CHAINING_MAX_SUBREQUESTS = settings.platform['chaining']['max_subrequests']


class ChainRequestsTestCase(DiskApiTestCase):
    api_mode = tags.platform.INTERNAL
    api_version = 'v1'
    method = 'POST'
    url = 'chaining/request'
    uid = default_user.uid
    yandexuid = 'yaid-703496061386152999'

    def setup_method(self, method):
        super(ChainRequestsTestCase, self).setup_method(method)

        self.client.request('PUT', 'disk/resources', query={'path': '/test_1'}, uid=self.uid)
        self.client.request('PUT', 'disk/resources', query={'path': '/test_1/nested_1'}, uid=self.uid)
        self.client.request('PUT', 'disk/resources', query={'path': '/test_2'}, uid=self.uid)
        self.client.request('PUT', 'disk/resources', query={'path': '/test_2/nested_2'}, uid=self.uid)

        r = self.client.request('GET', 'disk/resources', query={'path': '/'}, uid=self.uid)
        self.correct_root_list = json.loads(r.content)
        r = self.client.request('GET', 'disk/resources', query={'path': '/test_1'}, uid=self.uid)
        self.correct_test_1_list = json.loads(r.content)
        r = self.client.request('GET', 'disk/resources', query={'path': '/test_2'}, uid=self.uid)
        self.correct_test_2_list = json.loads(r.content)
        r = self.client.request('GET', 'disk/resources', query={'path': '/nonexistent_folder'}, uid=self.uid)
        self.correct_nonexistent_list = json.loads(r.content)

    def teardown_method(self, method):
        self.client.request('DELETE', 'disk/resources', query={'path': '/test_1'}, uid=self.uid)
        self.client.request('DELETE', 'disk/resources', query={'path': '/test_2'}, uid=self.uid)
        super(ChainRequestsTestCase, self).teardown_method(method)

    def test_chain_request(self):
        """
        Тестируем правильный чейн-запрос
        """
        req = {
            'method': 'GET',
            'relative_url': '/v1/%s/disk/resources?path=%%2F' % self.uid,
            'subrequests': [
                {
                    'method': 'GET',
                    'relative_url': '/v1/%s/disk/resources?path={body._embedded.items.0.name}' % self.uid
                },
                {
                    'method': 'GET',
                    'relative_url': '/v1/%s/disk/resources?path={body._embedded.items.1.name}' % self.uid
                },
            ]
        }
        resp = self.client.request(self.method, self.url, data=json.dumps(req), uid=self.uid)
        resp_content = json.loads(resp.content)
        assert resp.status_code == 200
        assert resp_content['code'] == 200
        assert json.loads(resp_content['body']) == self.correct_root_list
        assert resp_content['subrequests'][0]['code'] == 200
        assert json.loads(resp_content['subrequests'][0]['body']) == self.correct_test_1_list
        assert resp_content['subrequests'][1]['code'] == 200
        assert json.loads(resp_content['subrequests'][1]['body']) == self.correct_test_2_list

        req = {
            'method': 'GET',
            'relative_url': '/v1/%s/disk/resources?path=%%2F' % self.uid,
            'subrequests': []
        }
        resp = self.client.request(self.method, self.url, data=json.dumps(req), uid=self.uid)
        result = json.loads(resp.content)
        assert resp.status_code == 200
        assert result['code'] == 200
        assert json.loads(result['body']) == self.correct_root_list
        assert len(json.loads(resp.content)['subrequests']) == 0

    def test_chain_failed_mainrequest(self):
        """
        Тестируем поведение в случае, когда основной запрос выполнился с ошибкой (остальные должны выполниться)
        """
        req = {
            'method': 'GET',
            'relative_url': '/v1/%s/disk/resources?path=%%2Fnonexistent_folder' % self.uid,
            'subrequests': [
                {
                    'method': 'GET',
                    'relative_url': '/v1/%s/disk/resources?path=%%2F' % self.uid
                },
            ]
        }

        resp = self.client.request(self.method, self.url, data=json.dumps(req), uid=self.uid)
        result = json.loads(resp.content)
        assert resp.status_code == 200
        assert result['code'] == 404
        assert json.loads(result['body']) == self.correct_nonexistent_list
        assert len(result['subrequests']) == 1
        assert result['subrequests'][0]['code'] == 200
        assert json.loads(result['subrequests'][0]['body']) == self.correct_root_list

    def test_chain_failed_subrequest(self):
        """
        Тестируем поведение в случае, когда один из внутренних запросов выполняется с ошибкой
        (другой должен выполниться)
        """
        req = {
            'method': 'GET',
            'relative_url': '/v1/%s/disk/resources?path=%%2F' % self.uid,
            'subrequests': [
                {
                    'method': 'GET',
                    'relative_url': '/v1/%s/disk/resources?path=%%2Fnonexistent_folder' % self.uid
                },
                {
                    'method': 'GET',
                    'relative_url': '/v1/%s/disk/resources?path={body._embedded.items.1.name}' % self.uid
                },
            ]
        }

        resp = self.client.request(self.method, self.url, data=json.dumps(req), uid=self.uid)
        result = json.loads(resp.content)
        assert resp.status_code == 200
        assert len(result['subrequests']) == 2
        assert result['subrequests'][0]['code'] == 404
        assert json.loads(result['subrequests'][0]['body']) == self.correct_nonexistent_list
        assert result['subrequests'][1]['code'] == 200
        assert json.loads(result['subrequests'][1]['body']) == self.correct_test_2_list

        req = {
            'method': 'GET',
            'relative_url': '/v1/%s/disk/resources?path=%%2F' % self.uid,
            'subrequests': [
                {
                    'method': 'GET',
                    'relative_url': '/v1/%s/disk/resources?path={body._embedded.items.0.name}' % self.uid
                },
                {
                    'method': 'GET',
                    'relative_url': '/v1/%s/disk/resources?path=%%2Fnonexistent_folder' % self.uid
                },
            ]
        }

        resp = self.client.request(self.method, self.url, data=json.dumps(req), uid=self.uid)
        result = json.loads(resp.content)
        assert resp.status_code == 200
        assert len(json.loads(resp.content)['subrequests']) == 2
        assert result['subrequests'][0]['code'] == 200
        assert json.loads(result['subrequests'][0]['body']) == self.correct_test_1_list
        assert result['subrequests'][1]['code'] == 404
        assert json.loads(result['subrequests'][1]['body']) == self.correct_nonexistent_list

    def test_malformed_subrequest(self):
        """
        Тестируем случай, когда последующий запрос пытается достать несуществующее поле из ответа предыдущего или не
        указана часть ответа, из которой необходимо брать результат.
        """
        req = {
            'method': 'GET',
            'relative_url': '/v1/%s/disk/resources?path=%%2F' % self.uid,
            'subrequests': [
                {
                    'method': 'GET',
                    'relative_url': '/v1/%s/disk/resources?path={body._embedded.items.1.unknown_name}' % self.uid
                },
                {
                    'method': 'GET',
                    'relative_url': '/v1/%s/disk/resources?path={body._embedded.items.0.name}' % self.uid
                },
            ]
        }
        resp = self.client.request(self.method, self.url, data=json.dumps(req), uid=self.uid)
        result = json.loads(resp.content)
        assert resp.status_code == 200
        assert len(json.loads(resp.content)['subrequests']) == 2
        assert result['subrequests'][0]['code'] == 400
        assert json.loads(result['subrequests'][0]['body'])['error'] == 'ChainingApplyTemplateError'
        assert result['subrequests'][1]['code'] == 200
        assert json.loads(result['subrequests'][1]['body']) == self.correct_test_1_list

        req = {
            'method': 'GET',
            'relative_url': '/v1/%s/disk/resources?path=%%2F' % self.uid,
            'subrequests': [
                {
                    'method': 'GET',
                    'relative_url': '/v1/%s/disk/resources?path={_embedded.items.1.name}' % self.uid
                },
                {
                    'method': 'GET',
                    'relative_url': '/v1/%s/disk/resources?path={body._embedded.items.0.name}' % self.uid
                },
            ]
        }
        resp = self.client.request(self.method, self.url, data=json.dumps(req), uid=self.uid)
        result = json.loads(resp.content)
        assert resp.status_code == 200
        assert len(json.loads(resp.content)['subrequests']) == 2
        assert result['subrequests'][0]['code'] == 400
        assert json.loads(result['subrequests'][0]['body'])['error'] == 'ChainingApplyTemplateError'
        assert result['subrequests'][1]['code'] == 200
        assert json.loads(result['subrequests'][1]['body']) == self.correct_test_1_list

    def test_critical_error(self):
        """
        Проверяем, что вернем 500 в том случае, если что-то пошло совсем не так.
        """
        req = {
            'method': 'GET',
            'relative_url': '/v1/%s/disk/resources?path=%%2F' % self.uid,
            'subrequests': []
        }
        with mock.patch('mpfs.platform.v1.chaining.chaining_processors.ChainRequestProcessor.process',
                        side_effect=KeyError()):
            resp = self.client.request(self.method, self.url, data=json.dumps(req), uid=self.uid)
            assert resp.status_code == 500

    def test_too_many_requests(self):
        """
        Тестируем случай, когда передано больше запросов, чем допустимо в конфиге
        """
        subreq = {
            'method': 'GET',
            'relative_url': '/v1/%s/disk/resources?path={body._embedded.items.0.name}' % self.uid
        }
        req = {
            'method': 'GET',
            'relative_url': '/v1/%s/disk/resources?path=%%2F' % self.uid,
            'subrequests': [subreq] * PLATFORM_CHAINING_MAX_SUBREQUESTS
        }
        resp = self.client.request(self.method, self.url, data=json.dumps(req), uid=self.uid)
        assert resp.status_code == 200

        req['subrequests'] = [subreq] * (PLATFORM_CHAINING_MAX_SUBREQUESTS + 1)
        resp = self.client.request(self.method, self.url, data=json.dumps(req), uid=self.uid)
        assert resp.status_code == 400
        assert json.loads(resp.content)['error'] == 'ChainingTooManySubrequestsError'

    def test_forbid_nested_chaining(self):
        """Проверяем, что запрещено использовать чейнинг в чейнинге"""
        req = {
            'method': 'POST',
            'relative_url': '/v1/%s/chaining/request' % self.uid,
            'body': {
                'method': 'GET',
                'relative_url': '/v1/%s/disk/resources?path=%%2F' % self.uid,
                'subrequests': []
            },
            'subrequests': []
        }
        resp = self.client.request(self.method, self.url, data=json.dumps(req), uid=self.uid)
        assert resp.status_code == 400
        assert json.loads(resp.content)['error'] == 'ChainingForbiddenNestedContainerRequestError'

        req = {
            'method': 'GET',
            'relative_url': '/v1/%s/disk/resources?path=%%2F' % self.uid,
            'subrequests': [
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
        assert json.loads(resp.content)['error'] == 'ChainingForbiddenNestedContainerRequestError'

        req = {
            'method': 'POST',
            'relative_url': '/v1/%s/chaining/request?test=arg&another=1' % self.uid,
            'body': {
                'method': 'GET',
                'relative_url': '/v1/%s/disk/resources?path=%%2F' % self.uid,
                'subrequests': []
            },
            'subrequests': []
        }
        resp = self.client.request(self.method, self.url, data=json.dumps(req), uid=self.uid)
        assert resp.status_code == 400
        assert json.loads(resp.content)['error'] == 'ChainingForbiddenNestedContainerRequestError'

    def test_forbid_nested_batch(self):
        """Проверяем, что запрещено использовать батч в чейнинге"""
        req = {
            'method': 'GET',
            'relative_url': '/v1/%s/disk/resources?path=%%2F' % self.uid,
            'subrequests': [
                {
                    'method': 'POST',
                    'relative_url': '/v1/%s/batch/request' % self.uid,
                    'items': [
                        {
                            'method': 'GET',
                            'relative_url': '/v1/%s/disk?client_id=123&client_name=123' % self.uid,
                        }
                    ]
                },
            ]
        }
        resp = self.client.request(self.method, self.url, data=json.dumps(req), uid=self.uid)
        assert resp.status_code == 400
        assert json.loads(resp.content)['error'] == 'ChainingForbiddenNestedContainerRequestError'
