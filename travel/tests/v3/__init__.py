# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

import json
from urllib import urlencode

import mock

from django.test.client import MULTIPART_CONTENT, Client

from common.data_api.ticket_daemon.factories import create_segment, create_variant
from common.data_api.ticket_daemon.query import Query

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


class MockTicketDaemonMixin(object):
    def setUp(self):
        _statuses = {'test_carrier': 'done'}
        variants = {'test_carrier': [create_variant(segments=[create_segment()])]}
        self.patch_ticket_collect_variants = mock.patch.object(Query, 'collect_variants', return_value=(variants, _statuses))
        self.path_ticket_query_all = mock.Mock()
        self.patch_ticket_collect_variants.start()
        self.path_ticket_query_all.start()

    def tearDown(self):
        self.patch_ticket_collect_variants.stop()
        self.path_ticket_query_all.stop()


class ApiTestCase(MockTicketDaemonMixin, TestCase):
    def setUp(self):
        self.client = Client()
        self.headers = {'HTTP_HOST': 'randomhost'}
        self.api_version = 'v3'
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
        status_code = kwargs.pop('resp_status_code', 200)
        response = self.api_get(*args, **kwargs)
        assert response.status_code == status_code
        return json.loads(response.content)
