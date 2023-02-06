# -*- coding: utf-8 -*-

import json

from context import StatsCalcBaseTestCase
from market.idx.pylibrary.offer_flags.flags import OfferFlags


class TestShopStats(StatsCalcBaseTestCase):
    def setUp(self):
        super(TestShopStats, self).setUp()

        self.gls = [
            {
                'shop_id': 1,
                'binary_price': '12 1 0 RUR RUR',
                'binary_oldprice': '140 1 0 RUR RUR',
                'flags': 0,
                'category_id': 91491,
                'url': 'https://yandex.ru',
            },
            {
                'shop_id': 1,
                'binary_price': '13 1 0 RUR RUR',
                'binary_oldprice': '150 1 0 RUR RUR',
                'flags': 0,
                'category_id': 91491,
                'url': '__yx_tovar__',
            },
            {
                'shop_id': 1,
                'binary_price': '14 1 0 RUR RUR',
                'binary_oldprice': '160 1 0 RUR RUR',
                'flags': 0,
                'category_id': 91491,
                'url': 'https://ya.ru',
                'contex_info': {
                    'original_msku_id': 100,
                },
            },
            {
                'shop_id': 1,
                'binary_price': '15 1 0 RUR RUR',
                'binary_oldprice': '170 1 0 RUR RUR',
                'category_id': 91491,
                'url': 'https://sbermarket.ru',
                'flags': OfferFlags.IS_DIRECT.value
            },
            {
                'shop_id': 1,
                'binary_price': '15 1 0 RUR RUR',
                'binary_oldprice': '170 1 0 RUR RUR',
                'category_id': 91491,
                'url': 'https://sbermarket.ru',
                'flags': OfferFlags.IS_LAVKA.value
            },
            {
                'shop_id': 1,
                'binary_price': '15 1 0 RUR RUR',
                'binary_oldprice': '170 1 0 RUR RUR',
                'category_id': 91491,
                'url': 'https://sbermarket.ru',
                'flags': OfferFlags.IS_EDA_RESTAURANTS.value
            },
        ]

    def test_no_url_filter(self):
        self.run_stats_calc('ShopStats', json.dumps(self.gls))

        expected = {
            ('1', 'https://yandex.ru', '12RUR 4.03UAH 59.16KZT 0.34BYN', '140RUR 47UAH 690.2KZT 3.92BYN'),
        }

        offers_samples_path = self.tmp_file_path('offers_samples.csv')
        with open(offers_samples_path, 'r') as offer_samples_file:
            actual = {
                tuple(line.rstrip().split('\t'))
                for line in offer_samples_file.readlines()
            }
            assert(expected == actual)
