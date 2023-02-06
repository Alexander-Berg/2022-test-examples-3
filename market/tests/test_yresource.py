# -*- coding: utf-8 -*-

import unittest

from market.idx.pylibrary.downloader.downloader.yresource import is_internal, is_mock, is_upload


class YResourceTester(unittest.TestCase):
    def _test_is_flag_func(self, flag, func):
        class Stub(object):
            pass
        stub_false = Stub()
        setattr(stub_false, flag, False)
        stub_true = Stub()
        setattr(stub_true, flag, True)
        for obj in ({}, {flag: 'false'}, stub_false):
            self.assertFalse(func(obj))
        for obj in ({flag: 'true'}, stub_true):
            self.assertTrue(func(obj))

    def test_is_mock(self):
        self._test_is_flag_func('is_mock', is_mock)

    def test_is_upload(self):
        self._test_is_flag_func('is_upload', is_upload)

    def test_is_internal(self):
        combinations = (
            ('this URL is invalid', None, False),
            ('this URL is invalid', 'music', False),
            ('http://feeds.shop.com/yandex.yml', None, False),
            ('http://feeds.shop.com/yandex.yml', 'marketdatabuild', False),
            ('http://svn.yandex.ru/test_feed.xml', None, True),
            ('http://svn.yandex.ru/test_feed.xml', 'marketdatabuild', True),
            ('https://market-mbi-prod.s3.mds.yandex.net/upload-feed/545/upload-feed-1505545', None, True)
        )
        for url, username, expectation in combinations:
            if expectation:
                self.assertTrue(is_internal(url, username))
            else:
                self.assertFalse(is_internal(url, username))


if '__main__' == __name__:
    unittest.main()
