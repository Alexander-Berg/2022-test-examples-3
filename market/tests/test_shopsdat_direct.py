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
is_enabled=true
direct_status=REAL

shop_id=1251402
datafeed_id=2
is_enabled=true
direct_status=SBX

shop_id=1251403
datafeed_id=3
is_enabled=true
direct_status=NO

shop_id=1251404
datafeed_id=4
is_enabled=true

shop_id=1251405
datafeed_id=5
is_enabled=true
direct_status=MAYBE
'''


def load_feeds(data):
    return dict((feed.id, feed) for feed in shopsdat.loadfeeds(StringIO(data), value_separator='='))


class TestShopsdat(unittest.TestCase):

    def test_direct_status(self):
        feeds = load_feeds(DATA)
        self.assertEqual(feeds[1].direct_status, 'REAL')     # магазин с direct_status=REAL
        self.assertEqual(feeds[2].direct_status, 'SBX')      # магазин с direct_status=SBX
        self.assertEqual(feeds[3].direct_status, 'NO')       # магазин с direct_status=NO
        self.assertEqual(feeds[4].direct_status, 'NO')       # магазин без флага direct_status
        self.assertEqual(feeds[5].direct_status, 'NO')       # магазин с неверным флагом direct_status

if __name__ == '__main__':
    unittest.main()
