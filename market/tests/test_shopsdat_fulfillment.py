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

shop_id=1251403
datafeed_id=3
is_enabled=true
ff_program=SBX

shop_id=1251404
datafeed_id=4
is_enabled=true
ff_program=NO

shop_id=1251405
datafeed_id=5
is_enabled=true
'''


def load_feeds(data):
    return dict((feed.id, feed) for feed in shopsdat.loadfeeds(StringIO(data), value_separator='='))


class TestShopsdat(unittest.TestCase):

    def test_status(self):
        feeds = load_feeds(DATA)
        self.assertEqual(feeds[1].status, shopsdat.STATUS_VIRTUAL)
        self.assertEqual(feeds[2].status, shopsdat.STATUS_PRODUCTION)

    def test_ff_program(self):
        feeds = load_feeds(DATA)
        self.assertEqual(feeds[1].ff_program, 'NO')
        self.assertEqual(feeds[2].ff_program, 'REAL')
        self.assertEqual(feeds[3].ff_program, 'SBX')
        self.assertEqual(feeds[4].ff_program, 'NO')
        self.assertEqual(feeds[5].ff_program, 'NO')

    def test_ff_feed_and_shop_id_props(self):
        feeds = load_feeds(DATA)
        feeds[2].ff_feed_id = feeds[1].id
        feeds[2].ff_shop_id = feeds[1].shop_id

        self.assertEqual(feeds[2].ff_feed_id, feeds[1].id)
        self.assertEqual(feeds[2].ff_shop_id, feeds[1].shop_id)


if __name__ == '__main__':
    unittest.main()
