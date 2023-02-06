# -*- coding: utf-8 -*-
from __future__ import unicode_literals, absolute_import

import json

from django.utils.http import urlencode

from travel.avia.library.python.tester.testcase import TestCase

from travel.avia.backend.main.application import create_app

flask_app = create_app({'not_close_connection': True})


class TestApiHandler(TestCase):
    endpoint_root = '/'
    lang = 'ru'
    national_version = 'ru'

    def endpoint(self, other_params=None):
        params = {
            'lang': self.lang,
            'national_version': self.national_version
        }
        if other_params:
            params.update(other_params)

        return "{host}?{params}".format(host=self.endpoint_root,
                                        params=urlencode(params))

    def setUp(self):
        self.client = flask_app.test_client()

    def request(self, payload, params=None, body_autocorrection=True):
        if body_autocorrection and not isinstance(payload, list):
            payload = [payload]

        return self.client.post(
            self.endpoint(params), data=json.dumps(payload), content_type='application/json'
        )

    def api_data(self, payload, params=None, status_code=200, body_autocorrection=True):
        r = self.request(payload, params, body_autocorrection)

        assert r.status_code == status_code, r.get_data()

        return json.loads(r.get_data())

    def wrap_expect(self, res):
        return {
            'status': 'success',
            'data': [res]
        }

    def wrap_error_expect(self, payload, response):
        response['args'] = {
            'lang': [u'ru'],
            'national_version': [u'ru']
        }
        response['request'] = [payload]

        return response
