#!/usr/bin/python
# -*- coding: utf-8 -*-

import six
import unittest
if six.PY2:
    from StringIO import StringIO
else:
    from io import StringIO

import market.pylibrary.shopsdat as shopsdat


DATA = '''\
shop_id=1251401
datafeed_id=1
ff_virtual=true
is_enabled=true
is_mock=true
is_tested=true

shop_id=1251402
datafeed_id=2
is_enabled=true
ff_program=REAL
is_supplier=true

shop_id=1251406
datafeed_id=3
is_enabled=true
ff_program=REAL
blue_status=REAL

shop_id=1251407
datafeed_id=4
is_enabled=true
ff_program=REAL
blue_status=NO
is_supplier=true
'''


def load_feeds(data):
    return dict((feed.id, feed) for feed in shopsdat.loadfeeds(StringIO(data), value_separator='='))


class TestShopsdat(unittest.TestCase):

    def test_is_supplier(self):
        feeds = load_feeds(DATA)
        # виртуальный магазин
        self.assertEqual(feeds[1].blue_status, 'NO')
        # магазин участвующий в программе FF с is_supplier=true. Требуем выставленного blue_status
        self.assertEqual(feeds[2].blue_status, 'NO')
        # проверка того, что blue_status достаточно для включения магазина как синего
        self.assertEqual(feeds[3].blue_status, 'REAL')
        # Проверка приоритета blue_status перед is_supplier
        self.assertEqual(feeds[4].blue_status, 'NO')


if __name__ == '__main__':
    unittest.main()
