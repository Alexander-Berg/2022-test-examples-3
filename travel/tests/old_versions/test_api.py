# -*- coding: utf-8 -*-
from __future__ import absolute_import

from django.test import Client

from common.tester.testcase import TestCase


class TestApi(TestCase):
    def setUp(self):
        super(TestApi, self).setUp()

        self.client = Client()
        self.headers = {'HTTP_HOST': 'api.rasp.yandex.net'}

    def test_methods_exist(self):
        response = self.client.get('/ping', **self.headers)
        assert response.status_code == 200

        response = self.client.get('/v1/copyright/', **self.headers)
        assert response.status_code == 200

        response = self.client.get('/no_such_url', **self.headers)
        assert response.status_code == 400

        response = self.client.get('/version', **self.headers)
        assert response.status_code == 200
