# -*- coding: utf-8 -*-
import json

from travel.avia.ticket_daemon_api.jsonrpc.application import create_app
from travel.avia.library.python.ticket_daemon.memo import reset_all_caches
from travel.avia.library.python.tester.factories import create_settlement
from travel.avia.library.python.tester.testcase import TestCase


TEST_APP_CONFIG = {}


def get_client_app():
    return create_app(TEST_APP_CONFIG).test_client()


class ApiTestCase(TestCase):
    def setUp(self):
        reset_all_caches()
        create_settlement(id=2)
        create_settlement(id=213)

    def assertStatusOK(self, response, status_code=200):
        assert response.status_code == status_code, 'Status code {}, expected {}'.format(response.status_code, status_code)
        data = json.loads(response.data)
        assert data['status'] == 'success'

    def parseData(self, response):
        return json.loads(response.data)['data']
