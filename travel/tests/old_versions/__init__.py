# -*- coding: utf-8 -*-
import json
from urllib import urlencode

from django.test.client import MULTIPART_CONTENT, Client

from travel.rasp.api_public.tests.v3 import MockTicketDaemonMixin

from common.tester.testcase import TestCase


COORDS_TO_CHECK = [
    (0, 0, 200),
    (90, 180, 200),
    (-90, -180, 200),
    (91, 180, 400),
    (-91, 180, 400),
    (90, 181, 400),
    (90, -181, 400),
]


class ApiTestCase(MockTicketDaemonMixin, TestCase):
    def setUp(self):
        self.client = Client()
        self.headers = {'HTTP_HOST': 'randomhost'}
        self.api_version = None  # v1
        super(ApiTestCase, self).setUp()

    def get_api_url(self, view_name):
        return '/{}/{}/'.format(self.api_version, view_name)

    def api_get(self, view_name, params=None, headers=None):
        params = params if params is not None else {}
        headers = dict(self.headers, **(headers if headers else {}))
        return self.client.get(self.get_api_url(view_name), params, **headers)

    def api_post(self, view_name, params=None, data=None, headers=None, content_type=MULTIPART_CONTENT):
        params_str = urlencode(params) if params else ''
        data = data if data is not None else {}
        headers = dict(self.headers, **(headers if headers else {}))

        url = self.get_api_url(view_name)
        return self.client.post(url + '?' + params_str, data, content_type=content_type, **headers)

    def api_get_json(self, *args, **kwargs):
        response = self.api_get(*args, **kwargs)
        assert response.status_code == 200
        return json.loads(response.content)
