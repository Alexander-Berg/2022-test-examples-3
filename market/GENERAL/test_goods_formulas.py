#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.matcher import Contains
from core.types import (
    BlueOffer,
    Currency,
    HyperCategory,
    HyperCategoryType,
    MarketSku,
    MnPlace,
    Model,
    Offer,
    Picture,
    Shop,
    Tax,
)
from core.testcase import TestCase, main
import hashlib
import base64

SHOP_NAMES = [
    '0 Internet Shop',
    '1 Internet Shop',
    '2 Internet Shop',
    '3 Internet Shop',
    '4 Internet Shop',
    '5 Internet Shop',
    '6 Internet Shop',
    '7 Internet Shop',
    '8 Internet Shop',
    '9 Internet Shop',
    'A Internet Shop',
    'B Internet Shop',
    'C Internet Shop',
    'D Internet Shop',
    'E Internet Shop',
    'F Internet Shop',
]


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.report_subrole = 'goods'

        # do not set default values
        # @see market/report/lite/core/report.py set_default_search_experiment_flags
        cls.settings.ignore_search_experiment_flags += [
            'market_category_redirect_formula',
            'market_category_relevance_formula',
            'market_category_redirect_treshold',
        ]
        cls.settings.default_search_experiment_flags += ['market_write_category_redirect_features=20']

        cls.matrixnet.set_defaults = False
        cls.matrixnet.on_place(MnPlace.PRODUCT_CLASSIFIER, 0).respond(0.7)

        cls.index.shops += [
            Shop(fesh=1980, priority_region=213, regions=[225], name=SHOP_NAMES[0]),
            Shop(fesh=1981, priority_region=213, regions=[225], name=SHOP_NAMES[1]),
            Shop(fesh=1982, priority_region=213, regions=[225], name=SHOP_NAMES[2]),
            Shop(fesh=1983, priority_region=213, regions=[225], name=SHOP_NAMES[3]),
            Shop(fesh=1984, priority_region=213, regions=[225], name=SHOP_NAMES[4]),
            Shop(fesh=1985, priority_region=213, regions=[225], name=SHOP_NAMES[5]),
            Shop(fesh=1986, priority_region=213, regions=[225], name=SHOP_NAMES[6]),
            Shop(fesh=1987, priority_region=213, regions=[225], name=SHOP_NAMES[7]),
            Shop(fesh=1988, priority_region=213, regions=[225], name=SHOP_NAMES[8]),
            Shop(fesh=1989, priority_region=213, regions=[225], name=SHOP_NAMES[9]),
            Shop(fesh=1990, priority_region=213, regions=[225], name=SHOP_NAMES[10]),
            Shop(fesh=1991, priority_region=213, regions=[225], name=SHOP_NAMES[11]),
            Shop(fesh=1992, priority_region=213, regions=[225], name=SHOP_NAMES[12]),
            Shop(fesh=1993, priority_region=213, regions=[225], name=SHOP_NAMES[13]),
            Shop(fesh=1994, priority_region=213, regions=[225], name=SHOP_NAMES[14]),
            Shop(fesh=1995, priority_region=213, regions=[225], name=SHOP_NAMES[15]),
        ]

        pics = [
            Picture(
                picture_id=base64.b64encode(hashlib.md5(str(i)).digest())[:22], width=100, height=100, group_id=1234
            )
            for i in range(10)
        ]
        cls.index.offers += [
            Offer(hyperid=1, title='kiyanka 1980-1-1', fesh=1980, picture=pics[0], picture_flags=1, randx=1, hid=1),
            Offer(hyperid=1, title='kiyanka 1980-3-1', fesh=1980, picture=pics[0], picture_flags=1, randx=2, hid=1),
            Offer(title='kiyanka 1981-3-1', fesh=1981, picture=pics[0], picture_flags=1, randx=3, hid=1),
            Offer(title='kiyanka 1980-2-1', fesh=1980, picture=pics[0], picture_flags=1, randx=4),
            Offer(title='kiyanka 100-0-1', fesh=1983, picture=pics[0], picture_flags=1, randx=5),
            Offer(title='kiyanka 1980-4-2', fesh=1980, picture=pics[1], picture_flags=2, randx=6),
            Offer(title='kiyanka 102-2-1', fesh=1985, picture=pics[0], picture_flags=1, randx=7),
            Offer(title='kiyanka 1980-5-2', fesh=1980, picture=pics[1], picture_flags=2, randx=8),
            Offer(title='kiyanka 103-3-1', fesh=1986, picture=pics[0], picture_flags=1, randx=9),
            Offer(title='kiyanka 1981-1-1', fesh=1981, picture=pics[0], picture_flags=1, randx=10),
            Offer(title='kiyanka 1987-1-1', fesh=1987, picture=pics[0], picture_flags=1, randx=11),
            Offer(title='kiyanka 1988-1-1', fesh=1988, picture=pics[0], picture_flags=1, randx=12),
            Offer(title='kiyanka 1989-1-1', fesh=1989, picture=pics[0], picture_flags=1, randx=13),
            Offer(title='kiyanka 1990-1-1', fesh=1990, picture=pics[0], picture_flags=1, randx=14),
            Offer(title='kiyanka 1991-1-1', fesh=1991, picture=pics[0], picture_flags=1, randx=15),
            Offer(title='kiyanka 1992-1-1', fesh=1992, picture=pics[0], picture_flags=1, randx=16),
            Offer(title='kiyanka 1993-1-1', fesh=1993, picture=pics[0], picture_flags=1, randx=17),
            Offer(title='kiyanka 1994-1-1', fesh=1994, picture=pics[0], picture_flags=1, randx=18),
            Offer(title='kiyanka 1995-1-1', fesh=1995, picture=pics[0], picture_flags=1, randx=19),
            Offer(title='MARKETOUT-8860 1', fesh=1994, picture=pics[0], picture_flags=1, randx=11),
            Offer(title='MARKETOUT-8860 2', fesh=1994, picture=pics[1], picture_flags=2, randx=12),
            Offer(title='MARKETOUT-8860 3', fesh=1994, picture=pics[2], picture_flags=3, randx=13),
            Offer(title='MARKETOUT-8860 4', fesh=1994, picture=pics[3], picture_flags=4, randx=14),
            Offer(title='MARKETOUT-8860 5', fesh=1994, picture=pics[4], picture_flags=5, randx=15),
            Offer(title='MARKETOUT-8860 6', fesh=1994, picture=pics[5], picture_flags=6, randx=16),
            Offer(title='MARKETOUT-8860 7', fesh=1994, picture=pics[6], picture_flags=7, randx=17),
            Offer(title='MARKETOUT-8860 8', fesh=1994, picture=pics[7], picture_flags=8, randx=18),
            Offer(title='MARKETOUT-8860 9', fesh=1994, picture=pics[8], picture_flags=9, randx=19),
            Offer(title='MARKETOUT-8860 10', fesh=1994, picture=pics[9], picture_flags=10, randx=20),
        ]

        cls.index.shops += [
            Shop(
                fesh=1,
                datafeed_id=1,
                priority_region=213,
                name='virtual_shop',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                fulfillment_virtual=True,
                cpa=Shop.CPA_REAL,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
            ),
            Shop(
                fesh=2,
                datafeed_id=2,
                priority_region=213,
                name='blur_shop',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                cpa=Shop.CPA_REAL,
                warehouse_id=145,
            ),
        ]
        cls.index.mskus += [
            MarketSku(hid=1, sku=1, fesh=1, hyperid=1, title='blue kiy', blue_offers=[BlueOffer(ts=1000001)]),
            MarketSku(hid=2, sku=2, hyperid=2, title='blue circle', blue_offers=[BlueOffer(ts=2000002)]),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 1000001).respond(0.5)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 2000002).respond(0.5)

        cls.index.models += [
            Model(title="kiyanka M 1"),
            Model(title="kiyanka M 2"),
            Model(title="kiyanka M 3"),
        ]

        cls.index.hypertree += [
            HyperCategory(hid=3, output_type=HyperCategoryType.GURU, pessimize_offers=True, show_offers=True)
        ]

        cls.index.shops += [
            Shop(fesh=3000, priority_region=2, regions=[225]),
        ]

        cls.index.models += [
            Model(hyperid=31, title='cocosanka 1', hid=3, ts=31),
            Model(hyperid=32, title='cocosanka 2', hid=3, ts=32),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 31).respond(0.5)
        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 32).respond(0.4)

        cls.matrixnet.on_place(MnPlace.META_REARRANGE, 31).respond(0.3)
        cls.matrixnet.on_place(MnPlace.META_REARRANGE, 32).respond(0.4)

        cls.index.offers += [
            Offer(hyperid=31, fesh=1980, hid=3),
            Offer(hyperid=31, fesh=3000, hid=3, title='cocosanka 1 offer'),
            Offer(hyperid=32, fesh=1981, hid=3),
            Offer(fesh=1983, title='cocosanka offer', hid=3, ts=101983),
            Offer(fesh=1984, hid=3),
        ]

        cls.matrixnet.on_place(MnPlace.BASE_SEARCH, 101983).respond(0.6)
        cls.matrixnet.on_place(MnPlace.META_REARRANGE, 101983).respond(0.2)

    def _check_using_grouping(self, response, grouping_name):
        self.assertFragmentIn(response, {"g": [Contains(grouping_name)]})

    def test_mn_algo_default_parallel_goods(self):
        """Проверяем, что формула включена по умолчанию на параллельном.
        Проверяем дефолтную формулу.
        """
        # отключаем флаг market_parallel_fuzzy - но он неважен с Пантерой
        req = 'place=parallel&text=kiyanka&debug=1&rids=213&rearr-factors=market_parallel_fuzzy=0;'
        response = self.report.request_bs(req)
        self.assertFragmentIn(
            response, 'Using MatrixNet formula for search ranking: MNA_wiz_offer_843025_030T036_x_845657_070'
        )

        # запрашиваем несуществующие слова и флаг market_parallel_fuzzy (включен по умолчанию), чтобы включился fuzzy -
        # но с Пантерой fuzzy не работает, используется обычная формула
        req = 'place=parallel&text=kiyanka+qwertyuiop&debug=1&rids=213'
        response = self.report.request_bs(req)
        self.assertFragmentIn(
            response, 'Using MatrixNet formula for search ranking: MNA_wiz_offer_843025_030T036_x_845657_070'
        )

    def test_mn_algo_default_prime_goods(self):
        """На запросах с текстом на Товарной Вертикали используется формула "l2_goods_856190_017"""
        response = self.report.request_json('place=prime&text=kiyanka&debug=1&rids=213')
        self.assertFragmentIn(
            response,
            'Using MatrixNet formula: l2_goods_860492_041',
        )
        self.assertFragmentIn(response, {"rankedWith": "l2_goods_860492_041"})

    def test_mn_algo_default_goods_aprice_prime(self):
        """На запросах с текстом при сортировке по цене на Товарной Вертикали используется формула "l2_goods_856190_017"""
        response = self.report.request_json('place=prime&how=aprice&text=kiyanka&debug=1&rids=213')
        self.assertFragmentIn(
            response,
            'Using MatrixNet formula: l2_goods_860492_041',
        )
        self.assertFragmentIn(response, {"rankedWith": "l2_goods_860492_041"})

    def test_mn_algo_goods_price_sort_flag(self):
        """Проверяем переопределение под флагом на тексте при сортировке по цене в Товарной Вертикали"""
        request = 'place=prime&text=blue+kiy&debug=1&rids=213&client=products'
        flag = '&rearr-factors=market_goods_price_sort_mn_algo=MNA_Relevance'

        # флаг переопределит формулу для запроса с how=aprice
        price_sort = '&how=aprice'
        response = self.report.request_json(request + flag)
        self.assertFragmentNotIn(response, {"rankedWith": "MNA_Relevance"})
        response = self.report.request_json(request + flag + price_sort)
        self.assertFragmentIn(response, {"rankedWith": "MNA_Relevance"})

        # флаг переопределит формулу для запроса с how=dprice
        price_sort = '&how=dprice'
        response = self.report.request_json(request + flag)
        self.assertFragmentNotIn(response, {"rankedWith": "MNA_Relevance"})
        response = self.report.request_json(request + flag + price_sort)
        self.assertFragmentIn(response, {"rankedWith": "MNA_Relevance"})


if __name__ == '__main__':
    main()
