#!/usr/bin/python
# -*- coding: utf-8 -*-
import os
import unittest
import tempfile

from market.fcgi.banner import banner
from market.pylibrary.yatestwrap.yatestwrap import source_path


class Test(unittest.TestCase):
    def setUp(self):
        shop_rating = banner.ShopRating()
        banner.ShopRatingLoader(shop_rating, source_path('market/fcgi/banner/test/shop-rating'), None)
        self.handler = banner.Handler(shop_rating)

    def test(self):
        for i in (10, 20, 30, 40, 50, 60):
            self.assert_(self.handler.response('id=%d' % i))

        def request(idx, action):
            query = 'id={idx}&action={action}'.format(idx=idx, action=action)
            if action is None:
                query = 'id={idx}'.format(idx=idx)
            responce = self.handler.response(query)
            return responce.url if responce.url else responce.text

        html = '''\
<html>
<head><title></title></head>
<body style="padding: 0; margin: 0;">
<a href="https://market.yandex.ru/shop/1/reviews?from=1"><img src="{image_url}" alt="" border="0"/></a>
</body>
</html>'''

        image = banner.make_image_url(1, 3)
        self.assertEqual(request(1, action=None), image)
        self.assertEqual(request(1, action='bad'), image)
        self.assertEqual(request(1, action='image'), image)

        link_result = 'https://market.yandex.ru/shop/1/reviews?from=1'
        self.assertEqual(request(1, 'link'), link_result)

        self.assertEqual(request(1, 'banner'), html.format(image_url=image))


def iter_urls():
    for rating in range(banner.RATING_MIN_VALUE, banner.RATING_UNKNOWN_VALUE+1):
        for size in range(banner.SIZE_MIN_VALUE, banner.SIZE_MAX_VALUE+1):
            yield banner.make_image_url(rating, size)


def http_head(url):
    import urllib2

    class HeadRequest(urllib2.Request):
        def get_method(self):
            return 'HEAD'

    try:
        urllib2.urlopen(HeadRequest(url))
    except urllib2.HTTPError as e:
        print e.code, url
        raise


class TestImages(unittest.TestCase):
    def test(self):
        for url in iter_urls():
            print url
            http_head(url)


class TestRatingLoader(unittest.TestCase):
    def setUp(self):
        self.shop_rating_file = tempfile.mktemp()

    def tearDown(self):
        if os.path.exists(self.shop_rating_file):
            os.unlink(self.shop_rating_file)

    def test_reload(self):
        """ Если файл есть, reload его загружает """
        test_shop_id = 100
        test_shop_rating = 5

        shop_rating = banner.ShopRating()
        shop_rating_loader = banner.ShopRatingLoader(shop_rating, self.shop_rating_file, reload_interval=None)

        with open(self.shop_rating_file, "w") as fd:
            fd.write("{}:{}\n".format(test_shop_id, test_shop_rating))

        shop_rating_loader._reload()
        self.assertEqual(shop_rating.get(test_shop_id, default_rating=None), test_shop_rating)

    def test_reload_no_file(self):
        """ Если файла нет, reload завершается. Возвращается рейтинг по умолчанию. """
        shop_rating = banner.ShopRating()
        shop_rating_loader = banner.ShopRatingLoader(shop_rating, self.shop_rating_file, reload_interval=None)
        shop_rating_loader._reload()
        self.assertIsNone(shop_rating.get(1, default_rating=None))


if __name__ == '__main__':
    unittest.main()
