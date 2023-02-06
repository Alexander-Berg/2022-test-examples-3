# -*- coding: utf-8 -*-

import os
import unittest

import context

from market.idx.marketindexer.marketindexer import simplecgi
from market.idx.marketindexer.www import www_whoismaster


class Test(unittest.TestCase):
    def setUp(self):
        context.setup()

    def tearDown(self):
        context.cleanup()

    def test(self):
        class Config(object):
            www_cache_dir = context.rootdir
            www_whoismaster_log = None

        def mitype():
            return 'fender'

        def get_response(update):
            config = Config()
            query = 'update' if update else ''
            os.environ['QUERY_STRING'] = query
            app = www_whoismaster.make_app(config, mitype)
            return simplecgi.run(app, testmode=True)

        resp = get_response(update=True)
        self.assertEqual(resp.status, '200')
        self.assertEqual(resp.headers['X-CACHE-HIT'], '0')
        self.assertEqual(resp.content, mitype() + '\n')

        resp = get_response(update=False)
        self.assertEqual(resp.status, '200')
        self.assertEqual(resp.headers['X-CACHE-HIT'], '1')
        self.assertEqual(resp.content, mitype() + '\n')


if __name__ == '__main__':
    unittest.main()
