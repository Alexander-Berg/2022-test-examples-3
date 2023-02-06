# -*- coding: utf-8 -*-
import mock

from test.helpers.assertions import assert_log_contains, assert_log_not_contains
from test.parallelly.api.base import ApiTestCase
from mpfs.common.static import tags


class CommonApiTestCase(ApiTestCase):
    api_mode = tags.platform.INTERNAL

    def test_ping(self):
        resp = self.client.request('GET', '/ping', parse=False)
        assert resp.status_code == 200
        assert resp.content == 'pong'

    def test_x_http_method(self):
        resp = self.client.request('POST', '/ping')
        assert resp.status_code == 405
        resp = self.client.request('POST', '/ping', headers={'X-HTTP-Method': 'GET'})
        assert resp.status_code == 200

    def test_head_root(self):
        resp = self.client.request('HEAD', '/')
        assert resp.status_code == 200
        assert resp.headers
        assert not resp.content

    def test_header_OAuth_is_secret(self):
        oauth = 'OAuth'
        my_token = 'my-token'
        with mock.patch('mpfs.platform.common.logger.access_log.info') as mocked_log:
            resp = self.client.request('GET', '/', headers={'X-Forwarded-User': '%s %s' % (oauth, my_token)})

        assert resp.status_code == 200
        mocked_log.assert_called()
        assert_log_contains(mocked_log, oauth)
        assert_log_not_contains(mocked_log, my_token)
