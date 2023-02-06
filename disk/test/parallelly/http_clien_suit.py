# -*- coding: utf-8 -*-
from test.base import DiskTestCase
from mpfs.engine.http import client as http_client

class RedirectTestCase(DiskTestCase):
    URL_WITH_301_REDIRECT = 'http://api-stable.dst.yandex.net'  # тут нужен любой URL с 301 редиректом и 200 после редиректа

    def test_redirect_following(self):
        status_code, _, _ = http_client.open_url(self.URL_WITH_301_REDIRECT, follow_redirects=True, return_status=True)
        assert status_code == 200

    def test_disabled_redirect_following(self):
        status_code, _, _ = http_client.open_url(self.URL_WITH_301_REDIRECT, follow_redirects=False, return_status=True)
        assert status_code == 301
