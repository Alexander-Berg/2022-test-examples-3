import logging
import unittest

import sys

from mindexer_clt_api import prepare_app


class TestApp(unittest.TestCase):
    def setUp(self):
        self.app = prepare_app('local/settings.py')
        self.app.testing = True
        self.client = self.app.test_client()

    # def test_datasources(self):
    #     self.assertIsNotNone(self.app.config.get('TOKEN'))
    #
    # def open_with_auth(self, url, method):
    #     return self.app.test_client().open(url, method=method, headers={'Authorization': 'Bearer ' + self.app.config.get('TOKEN', '')})
    #
    # def test_ping_auth_allow(self):
    #     result = self.open_with_auth('http://[::1]:5000/ping_auth', 'GET')
    #     self.assertEqual(result.status_code, 200)
    #     self.assertEqual(result.data, '0;ok')
    #
    # def test_ping_auth_deny(self):
    #     result = self.client.get('http://[::1]:5000/ping_auth')
    #     self.assertEqual(result.status_code, 401)

    def test_ping(self):
        result = self.client.get('http://[::1]:5000/ping')
        self.assertEqual(result.status_code, 200)
        self.assertEqual(result.data, '0;ok')


if __name__ == '__main__':
    logging.basicConfig(stream=sys.stdout, level=logging.DEBUG)
    logging.getLogger('test')
    unittest.main()
