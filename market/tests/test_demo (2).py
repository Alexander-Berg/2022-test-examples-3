import unittest
import requests

from mock import patch

from demo.demo import get_market_hostname


class TestDemo(unittest.TestCase):
    class Response(object):
        def __init__(self, content, code=200):
            self.content = content
            self.url = 'http://example.com'
            self.status_code = code

        def raise_for_status(self):
            if 400 <= self.status_code <= 599:
                raise requests.exceptions.HTTPError('Test exception')

    def test_get_market_hostname(self):
        with patch('demo.demo.requests.get') as mock_get:
            mock_get.return_value = self.Response('pepelac01h.market.yandex.net')
            self.assertEqual(get_market_hostname(), 'pepelac01h.market.yandex.net')

            with self.assertRaises(requests.exceptions.HTTPError):
                mock_get.return_value = self.Response('pepelac01h.market.yandex.net', 503)
                get_market_hostname()


if __name__ == "__main__":
    unittest.main()
