# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

import json
from urlparse import urljoin

import httpretty
from django.conf import settings
from django.test import Client

from common.tester.mocks import set_setting
from common.tester.testcase import TestCase


class TestApi(TestCase):
    def setUp(self):
        super(TestApi, self).setUp()

        self.client = Client()
        self.headers = {'HTTP_HOST': 'api.rasp.yandex.net'}

    def test_methods_exist(self):
        response = self.client.get('/ping', **self.headers)
        assert response.status_code == 200

        response = self.client.get('/v3/copyright/', **self.headers)
        assert response.status_code == 200

        response = self.client.get('/v3/copyright', **self.headers)
        assert response.status_code == 302

        response = self.client.get('/no_such_url', **self.headers)
        assert response.status_code == 400

        response = self.client.get('/version', **self.headers)
        assert response.status_code == 200

    @httpretty.activate
    def test_api_keys(self):
        with set_setting('APIKEYS_ENABLED', True):
            # без ключа
            response = self.client.get('/v3/copyright/', **self.headers)
            assert response.status_code == 400

            url = urljoin(settings.APIKEYS_URL, 'check_key')
            httpretty.register_uri(httpretty.GET, url,
                                   responses=[
                                       httpretty.Response(body=json.dumps({'error': 'Key not found'}), status=200),
                                       httpretty.Response(body=json.dumps({'error': 'Service not found'}), status=404),
                                       httpretty.Response(body=json.dumps({'result': 'OK'}), status=404),
                                       httpretty.Response(body='', status=200),
                                       httpretty.Response(body=json.dumps({'result': 'OK'}), status=200),
                                   ])

            # 'error': 'Key not found'
            response = self.client.get('/v3/copyright/', {'apikey': 'key'}, **self.headers)
            assert response.status_code == 400

            # status=404 Service not found
            response = self.client.get('/v3/copyright/', {'apikey': 'key'}, **self.headers)
            assert response.status_code == 200

            # status=404
            response = self.client.get('/v3/copyright/', {'apikey': 'key'}, **self.headers)
            assert response.status_code == 400

            # ValueError: No JSON object
            response = self.client.get('/v3/copyright/', {'apikey': 'key'}, **self.headers)
            assert response.status_code == 200

            # OK
            response = self.client.get('/v3/copyright/', {'apikey': 'key'}, **self.headers)
            assert response.status_code == 200
