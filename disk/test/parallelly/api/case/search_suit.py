# -*- coding: utf-8 -*-
import mock
import socket
import urllib2

from nose_parameterized import parameterized

from mpfs.common.static import tags
from mpfs.config import settings

from test.base_suit import UserTestCaseMixin
from test.parallelly.api.disk.base import DiskApiTestCase


PLATFORM_DISK_APPS_IDS = settings.platform['disk_apps_ids']


class SearchWarmupHandlerTestCase(UserTestCaseMixin, DiskApiTestCase):
    api_mode = tags.platform.EXTERNAL
    api_version = 'v1'
    url = 'case/disk/search/warmup'

    @parameterized.expand(PLATFORM_DISK_APPS_IDS)
    def test_common(self, client_id):
        with self.specified_client(scopes=['yadisk:all'], id=client_id):
            resp = self.client.put(self.url, uid=self.uid)
        assert resp.status_code == 200

    @parameterized.expand(PLATFORM_DISK_APPS_IDS)
    def test_handler_return_200_on_timeout(self, client_id):
        with self.specified_client(scopes=['yadisk:all'], id=client_id), \
                mock.patch('urllib2.urlopen', side_effect=socket.timeout):
            resp = self.client.put(self.url, uid=self.uid)
        assert resp.status_code == 200

    @parameterized.expand(PLATFORM_DISK_APPS_IDS)
    def test_handler_return_200_on_search_error(self, client_id):
        # urllib2.HTTPError имеет необычный конструктор, нужно для работоспособности mock
        side_effect = urllib2.HTTPError('', 500, 'test error', None, None)
        with self.specified_client(scopes=['yadisk:all'], id=client_id), \
                mock.patch('urllib2.urlopen', side_effect=side_effect):
            resp = self.client.put(self.url, uid=self.uid)
        assert resp.status_code == 200

    @parameterized.expand(PLATFORM_DISK_APPS_IDS)
    def test_handler_return_500_on_unknown_error(self, client_id):
        with self.specified_client(scopes=['yadisk:all'], id=client_id), \
                mock.patch('mpfs.engine.http.client.open_url', side_effect=Exception):
            resp = self.client.put(self.url, uid=self.uid)
        assert resp.status_code == 500

    def test_not_disk_client_is_forbidden(self):
        with self.specified_client(scopes=['yadisk:all'], id='qwerty'):
            resp = self.client.put(self.url, uid=self.uid)
        assert resp.status_code == 403
