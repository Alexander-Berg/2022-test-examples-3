import unittest
from yandex.tap import tap_run

from yandex.utils import host_dc

class UniqTestCase(unittest.TestCase):
    def test_host_dc(self):
        """ test description """
        self.assertEqual(host_dc(None), None, 'None as host name')
        self.assertEqual(host_dc("ppcheavy01d.yandex.ru"), 'd', 'standard 1')
        self.assertEqual(host_dc("ppcheavy01f.yandex.ru"), 'f', 'standard 2')
        self.assertEqual(host_dc("yandex.ru"), None, 'no dc info')
        self.assertEqual(host_dc("www.yandex.ru"), None, 'no dc info')

if __name__ == '__main__':
    tap_run(tests=1)


