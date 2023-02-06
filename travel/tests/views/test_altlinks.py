# -*- coding: utf-8 -*-
from __future__ import unicode_literals

import json

from django.test import Client

from common.tester.testcase import TestCase


class TestRedirects(TestCase):
    def setUp(self):
        self.client = Client()

    def test_redirects(self):
        response = self.client.get('/altlinks/rasp/http://rasp.yandex.ua/')

        assert response.status_code == 200

        data = json.loads(response.content)

        assert data == {
            'touch': {
                'href': 'https://t.rasp.yandex.ua/',
                'media': 'only screen and (max-width: 640px)',
                'rel': 'alternate'
            }
        }

    def test_altlinks_get_params(self):
        response = self.client.get('/altlinks/?from=rasp&url=http://rasp.yandex.ua/')

        assert response.status_code == 200

        data = json.loads(response.content)

        assert data == {
            'touch': {
                'href': 'https://t.rasp.yandex.ua/',
                'media': 'only screen and (max-width: 640px)',
                'rel': 'alternate'
            }
        }
