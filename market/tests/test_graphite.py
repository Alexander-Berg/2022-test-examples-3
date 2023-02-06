# -*- coding: utf-8 -*-


from functools import partial
import socket
import time
import unittest


from market.pylibrary.graphite.graphite import Graphite, DummyGraphite


class Test(unittest.TestCase):
    def test_makemsg_inferno(self):
        hostname = 'unicorn.market.yandex.net'
        g = Graphite(hostname)
        mm = partial(g._make_message, time_seconds=1000)

        self.assertEqual(mm(None, 10), None)
        self.assertEqual(mm('', 10), None)
        self.assertEqual(mm('key', None), None)
        self.assertEqual(mm('key', 10), 'one_hour.unicorn_market_yandex_net.key 10 1000\n')

    def test_makemsg_autohost(self):
        g = Graphite()
        actual = g._make_message('key', 10, time_seconds=1000)
        hostname = socket.getfqdn().replace('.', '_')
        expected = 'one_hour.{hostname}.key 10 1000\n'.format(hostname=hostname)
        self.assertEqual(actual, expected)

    def test_makemsg_autoall(self):
        g = Graphite()
        msg = g._make_message('a', 1)
        name, value, timestamp = msg.rstrip().split()
        self.assertTrue(name.endswith('.a'))
        self.assertEqual(value, '1')
        self.assertTrue(int(timestamp) <= time.time())

    def test_path(self):
        g = Graphite('bfg9T.yandex.ru', path=['9M', '9G'], period='one_nanosecond')
        msg = g._make_message('b', 2)
        name, value, timestamp = msg.rstrip().split()
        self.assertEqual(name, 'one_nanosecond.bfg9T_yandex_ru.9M.9G.b')
        self.assertEqual(value, '2')
        self.assertTrue(int(timestamp) <= int(time.time()))

    def test_dummy_graphite(self):
        g = DummyGraphite('bfg9T.yandex.ru', path=['9M', '9G'], period='infinity')
        g.send_metric('snow', 'fall')


if __name__ == '__main__':
    unittest.main()
