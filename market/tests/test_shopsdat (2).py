#!/usr/bin/python
# -*- coding: utf-8 -*-
import os
import six
import unittest
if six.PY2:
    from StringIO import StringIO
else:
    from io import StringIO

import market.pylibrary.shopsdat as shopsdat


DATA = '''\
shop_id=1
datafeed_id=1
url=http://yandex.ru/1
is_enabled=true
desc=production

shop_id=2
datafeed_id=2
url=http://yandex.ru/2
is_enabled=true
is_mock=true
desc=testing

#shop_id=3
datafeed_id=3
url=http://yandex.ru/3
is_tested=true
desc=preproduction

#shop_id=4
datafeed_id=4
url=http://yandex.ru/4
is_tested=false
desc=off

shop_id=8
datafeed_id=8
url=http://yandex.ru/8
is_tested=false
desc=off

shop_id=9
datafeed_id=9
url=http://yandex.ru/9
is_tested=true
desc=preproduction

#shop_id=1069
datafeed_id=1069
url=http://yandex.ru/1069
is_tested=false
desc=system

shop_id=7
datafeed_id=7
url=http://yandex.ru/7
is_tested=true
is_enabled=true
desc=duplex

shop_id=10
datafeed_id=10
url=http://yandex.ru/10
is_tested=false
is_enabled=true
desc=production
is_dsbs=true
'''

DATA2 = '''\
shop_id=1
datafeed_id=1
url=http://yandex.ru/1
prefix=HTTP_AUTH='basic:*:login:password'
is_enabled=true
home_region=1
priority_regions=1
regions=1;2;3
unknown_key=unknown_value

'''

DATA3 = '''\
datafeed_id\t123
shop_id\t11
data\tbla
yet_another_data\tbla_bla

datafeed_id\t345
shop_id\t22
data\tbla777
yet_another_data\tbla_bla888

'''

DATA4 = '''\
datafeed_id\t123
shop_id\t11
data\tbla
yet_another_data\tbla_bla

datafeed_id    345
shop_id\t22
data                  bla777
yet_another_data\tbla_bla888

'''


def load_feeds(data):
    return dict((feed.id, feed) for feed in shopsdat.loadfeeds(StringIO(data), value_separator='='))


class TestShopsdat(unittest.TestCase):

    def test_status(self):
        feeds = load_feeds(DATA)
        self.assertEqual(feeds[1].status, shopsdat.STATUS_PRODUCTION)
        self.assertEqual(feeds[10].status, shopsdat.STATUS_PRODUCTION)
        self.assertEqual(feeds[2].status, shopsdat.STATUS_TESTING)
        self.assertEqual(feeds[3].status, shopsdat.STATUS_PREPRODUCTION)
        self.assertEqual(feeds[9].status, shopsdat.STATUS_PREPRODUCTION)
        self.assertEqual(feeds.get(4), None)
        self.assertEqual(feeds.get(8), None)
        self.assertEqual(feeds[1069].status, shopsdat.STATUS_SYSTEM)
        self.assertEqual(feeds[7].status, shopsdat.STATUS_BIPRODUCTION)

    def test_props(self):
        feeds = load_feeds(DATA2)
        feed = feeds[1]
        self.assertEqual(feed.home_region, 1)
        self.assertEqual(feed.priority_region, 1)
        self.assertEqual(feed.regions, (1, 2, 3))

    def test_converting_shopsdat_to_map(self):
        feeds = list(shopsdat.iterfeeds(StringIO(DATA3), status_flags=shopsdat.STATUS_ANY))

        self.assertEqual(len(feeds), 2)
        self.assertEqual(dict(feeds[0].iteritems()), {'datafeed_id': '123', 'shop_id': '11', 'data': 'bla', 'yet_another_data': 'bla_bla'})
        self.assertEqual(dict(feeds[1].iteritems()), {'datafeed_id': '345', 'shop_id': '22', 'data': 'bla777', 'yet_another_data': 'bla_bla888'})

    def test_feed_status(self):
        self.assertEqual(shopsdat.Feed({'datafeed_id': '77', '#shop_id': '1', 'is_tested': 'true'}).status, shopsdat.STATUS_PREPRODUCTION)
        self.assertEqual(shopsdat.Feed({'datafeed_id': '77', 'shop_id': '1', 'is_enabled': 'false', 'is_tested': 'true'}).status, shopsdat.STATUS_PREPRODUCTION)

        self.assertEqual(shopsdat.Feed({'datafeed_id': '77', '#shop_id': '1', 'is_tested': 'false'}).status, shopsdat.STATUS_DISABLE)
        self.assertEqual(shopsdat.Feed({'datafeed_id': '77', '#shop_id': '1'}).status, shopsdat.STATUS_DISABLE)
        self.assertEqual(shopsdat.Feed({'datafeed_id': '77', 'shop_id': '1'}).status, shopsdat.STATUS_DISABLE)
        self.assertEqual(shopsdat.Feed({'datafeed_id': '77', 'shop_id': '1', 'is_enabled': 'false'}).status, shopsdat.STATUS_DISABLE)

        self.assertEqual(shopsdat.Feed({'datafeed_id': '77', 'shop_id': '1', 'is_enabled': 'true', 'is_mock': 'true'}).status, shopsdat.STATUS_TESTING)

        self.assertEqual(shopsdat.Feed({'datafeed_id': '77', 'shop_id': '1', 'is_enabled': 'true', 'is_mock': 'false'}).status, shopsdat.STATUS_PRODUCTION)
        self.assertEqual(shopsdat.Feed({'datafeed_id': '77', 'shop_id': '1', 'is_enabled': 'true', 'is_tested': 'true'}).status, shopsdat.STATUS_BIPRODUCTION)

        self.assertEqual(shopsdat.Feed({'datafeed_id': '1069', 'shop_id': '1'}).status, shopsdat.STATUS_SYSTEM)

    def test_set_status(self):
        get_status = shopsdat.calc_status
        set_status = shopsdat._set_status
        statuslist = [shopsdat.STATUS_DISABLE,
                      shopsdat.STATUS_TESTING,
                      shopsdat.STATUS_PREPRODUCTION,
                      shopsdat.STATUS_PRODUCTION,
                      shopsdat.STATUS_BIPRODUCTION]
        # production
        feed = {'datafeed_id': '1', 'shop_id': '1', 'is_enabled': 'true'}
        self.assertEqual(get_status(feed), shopsdat.STATUS_PRODUCTION)
        for newstatus in statuslist:
            set_status(feed, newstatus)
            self.assertEqual(get_status(feed), newstatus)
        # testing
        feed = {'datafeed_id': '1', 'shop_id': '1', 'is_enabled': 'true',
                'is_mock': 'true'}
        self.assertEqual(get_status(feed), shopsdat.STATUS_TESTING)
        for newstatus in statuslist:
            set_status(feed, newstatus)
            self.assertEqual(get_status(feed), newstatus)
        # preproduction
        feed = {'datafeed_id': '1', '#shop_id': '1', 'is_tested': 'true'}
        self.assertEqual(get_status(feed), shopsdat.STATUS_PREPRODUCTION)
        for newstatus in statuslist:
            set_status(feed, newstatus)
            self.assertEqual(get_status(feed), newstatus)
        # disable
        feed = {'datafeed_id': '1', '#shop_id': '1'}
        self.assertEqual(get_status(feed), shopsdat.STATUS_DISABLE)
        for newstatus in statuslist:
            set_status(feed, newstatus)
            self.assertEqual(get_status(feed), newstatus)

    def test_model(self):
        feeds = load_feeds(DATA)
        self.assertEqual(feeds[1].is_dsbs, False)
        self.assertEqual(feeds[10].is_dsbs, True)


class TestShopsdatFromFile(unittest.TestCase):
    filepath = 'shops.dat'

    def setUp(self):
        self.feeds = list(load_feeds(DATA).values())
        with open(self.filepath, 'w') as fobj:
            shopsdat.dumpfeeds(self.feeds, fobj)

    def tearDown(self):
        if os.path.exists(self.filepath):
            os.unlink(self.filepath)

    def test(self):
        feeds1 = self.feeds
        feeds2 = shopsdat.loadfeeds(self.filepath)
        ids1 = [feed.id for feed in feeds1]
        ids2 = [feed.id for feed in feeds2]
        self.assertEqual(set(ids1), set(ids2))


class TestShopsdatFromFileWithMixedValueSeparator(unittest.TestCase):
    def test(self):
        feeds = list(shopsdat.iterfeeds(StringIO(DATA4), status_flags=shopsdat.STATUS_ANY))
        self.assertEqual(len(feeds), 2)


if __name__ == '__main__':
    unittest.main()
