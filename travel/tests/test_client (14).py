# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import json
import httpretty
import six

from six.moves.urllib.parse import parse_qs, urlparse

from travel.rasp.library.python.api_clients.sandbox import SandboxClient


class TestSandboxApiClient(object):
    def setup(self):
        self.client = SandboxClient(
            auth_token='sec123',
            host='https://localhost/api/v1/'
        )

    def last_url(self):
        if six.PY3:
            return httpretty.last_request().url
        else:
            req = httpretty.last_request()
            return 'https://' + req.headers.get('Host', '') + req.path

    @httpretty.activate(allow_net_connect=False)
    def test_auth_header(self):
        httpretty.register_uri(httpretty.PUT, 'https://localhost/api/v1/batch/tasks/start', status=200)
        self.client.start_task(155)

        assert httpretty.last_request().headers['Authorization'] == 'OAuth sec123'

    @httpretty.activate(allow_net_connect=False)
    def test_create_draft(self):
        task_type = 'TEST_TASK_TYPE'
        task_draft_id = 42

        httpretty.register_uri(
            httpretty.POST, 'https://localhost/api/v1/task',
            status=200,
            body=json.dumps({'id': task_draft_id}),
            content_type='application/json'
        )

        assert self.client.create_task_draft(
            task_type=task_type,
            param1='param1_value',
            param2='param1_value'
        ) == task_draft_id

        assert json.loads(httpretty.last_request().body) == {
            'type': task_type,
            'param2': 'param1_value',
            'param1': 'param1_value'
        }

    @httpretty.activate(allow_net_connect=False)
    def test_start_task(self):
        task_id = 123

        httpretty.register_uri(httpretty.PUT, 'https://localhost/api/v1/batch/tasks/start', status=200)

        self.client.start_task(task_id)

        assert json.loads(httpretty.last_request().body) == [task_id]

    @httpretty.activate(allow_net_connect=False)
    def test_get_task_status(self):
        task_id = 123
        task_status = 'RUNNING'

        httpretty.register_uri(
            httpretty.GET, 'https://localhost/api/v1/task/123',
            status=200,
            body=json.dumps({'status': task_status}),
            content_type='application/json'
        )

        assert self.client.get_task_status(task_id) == task_status
        assert self.last_url() == 'https://localhost/api/v1/task/123'

    @httpretty.activate(allow_net_connect=False)
    def test_get_task_resources(self):
        task_id = 123

        httpretty.register_uri(
            httpretty.GET, 'https://localhost/api/v1/task/{}/resources'.format(task_id),
            status=200,
            body=json.dumps({'items': [1, 2, 3]}),
            content_type='application/json'
        )

        assert self.client.get_task_resources(task_id) == [1, 2, 3]
        assert self.last_url() == 'https://localhost/api/v1/task/123/resources'

    @httpretty.activate(allow_net_connect=False)
    def test_get_resources(self):
        httpretty.register_uri(
            httpretty.GET, 'https://localhost/api/v1/resource',
            status=200,
            body=json.dumps({'items': ['res_1', 'res_2']}),
            content_type='application/json'
        )

        assert self.client.get_resources(
            'RES_TYPE',
            limit=10,
            attrs={'attr_1': 'value_1'}
        ) == ['res_1', 'res_2']

        parsed_url = urlparse(self.last_url())
        assert parsed_url.path == '/api/v1/resource'
        assert parse_qs(parsed_url.query) == {
            'limit': ['10'],
            'type': ['RES_TYPE'],
            'attrs': ['{"attr_1": "value_1"}']
        }

    @httpretty.activate(allow_net_connect=False)
    def test_get_resource(self):
        httpretty.register_uri(
            httpretty.GET, 'https://localhost/api/v1/resource',
            status=200,
            body=json.dumps({'items': ['res_1']}),
            content_type='application/json'
        )

        assert self.client.get_resource(
            'RES_TYPE',
            attrs={'attr_1': 'value_1'}
        ) == 'res_1'

        parsed_url = urlparse(self.last_url())
        assert parsed_url.path == '/api/v1/resource'
        assert parse_qs(parsed_url.query) == {
            'limit': ['1'],
            'type': ['RES_TYPE'],
            'order': ['-id'],
            'attrs': ['{"attr_1": "value_1"}']
        }
