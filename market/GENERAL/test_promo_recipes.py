#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import HyperCategory, Offer, Promo, PromoPurchase, PromoRecipe, PromoType
from core.matcher import NotEmpty, NoKey
from core.types.hypercategory import ADULT_CATEG_ID

from datetime import datetime


class T(TestCase):
    @classmethod
    def prepare_recipes(cls):
        cls.index.promo_recipes += [
            PromoRecipe(shop_name="shop 1", link="shop_promo_code_1"),
            PromoRecipe(shop_name="shop 2", link="shop_all_promo_2"),
            PromoRecipe(vendor_name="vendor 3", link="vendor_promo_code_3"),
            PromoRecipe(vendor_name="vendor 4", link="vendor_all_promo_4"),
            PromoRecipe(category_name="cat 5", link="category_promo_code_5"),
            PromoRecipe(category_name="cat 6", link="category_all_promo_6"),
            PromoRecipe(shop_name="shop 101", link="shop_promo_code_101"),
            PromoRecipe(shop_name="shop 102", link="shop_promo_code_102_110"),
            PromoRecipe(shop_name="shop 103", link="shop_promo_code_103_102"),
        ]

    def test_mult_types(self):
        response = self.report.request_json('place=promo_recipes&promo-recipe=vendor-promo-code')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "promo-recipe",
                        "vendor_name": "vendor 3",
                        "link": "vendor_promo_code_3",
                        "type": "vendor-promo-code",
                    },
                ]
            },
            allow_different_len=False,
        )

        response = self.report.request_json('place=promo_recipes&promo-recipe=vendor-promo-code,vendor-all-promo')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "promo-recipe",
                        "vendor_name": "vendor 3",
                        "link": "vendor_promo_code_3",
                        "type": "vendor-promo-code",
                    },
                    {
                        "entity": "promo-recipe",
                        "vendor_name": "vendor 4",
                        "link": "vendor_all_promo_4",
                        "type": "vendor-all-promo",
                    },
                ]
            },
            allow_different_len=False,
        )

    def test_recipes(self):
        response = self.report.request_json('place=promo_recipes&promo-recipe=shop-promo-code')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "promo-recipe",
                        "shop_name": "shop 1",
                        "link": "shop_promo_code_1",
                        "type": "shop-promo-code",
                    }
                ]
            },
        )
        response = self.report.request_json('place=promo_recipes&promo-recipe=shop-all-promo')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "promo-recipe",
                        "shop_name": "shop 2",
                        "link": "shop_all_promo_2",
                        "type": "shop-all-promo",
                    }
                ]
            },
        )
        response = self.report.request_json('place=promo_recipes&promo-recipe=vendor-promo-code')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "promo-recipe",
                        "vendor_name": "vendor 3",
                        "link": "vendor_promo_code_3",
                        "type": "vendor-promo-code",
                    }
                ]
            },
        )
        response = self.report.request_json('place=promo_recipes&promo-recipe=vendor-all-promo')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "promo-recipe",
                        "vendor_name": "vendor 4",
                        "link": "vendor_all_promo_4",
                        "type": "vendor-all-promo",
                    }
                ]
            },
        )
        response = self.report.request_json('place=promo_recipes&promo-recipe=category-promo-code')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "promo-recipe",
                        "category_name": "cat 5",
                        "link": "category_promo_code_5",
                        "type": "category-promo-code",
                    }
                ]
            },
        )
        response = self.report.request_json('place=promo_recipes&promo-recipe=category-all-promo')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "promo-recipe",
                        "category_name": "cat 6",
                        "link": "category_all_promo_6",
                        "type": "category-all-promo",
                    }
                ]
            },
        )

    def test_filters(self):
        response = self.report.request_json('place=promo_recipes&promo-recipe=shop-promo-code&promo-recipe-id=101')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "promo-recipe",
                        "shop_name": "shop 101",
                        "link": "shop_promo_code_101",
                        "type": "shop-promo-code",
                    }
                ]
            },
            allow_different_len=False,
        )

        response = self.report.request_json('place=promo_recipes&promo-recipe=shop-promo-code&promo-recipe-id=102')
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "promo-recipe",
                        "shop_name": "shop 102",
                        "link": "shop_promo_code_102_110",
                        "type": "shop-promo-code",
                    },
                    {
                        "entity": "promo-recipe",
                        "shop_name": "shop 103",
                        "link": "shop_promo_code_103_102",
                        "type": "shop-promo-code",
                    },
                ]
            },
            allow_different_len=False,
        )

        response = self.report.request_json(
            'place=promo_recipes&promo-recipe=shop-promo-code&promo-recipe-name=shop 101'
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "promo-recipe",
                        "shop_name": "shop 101",
                        "link": "shop_promo_code_101",
                        "type": "shop-promo-code",
                    }
                ]
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_redirects(cls):
        cls.index.hypertree += [
            HyperCategory(hid=41, uniq_name='Тракторы'),
            HyperCategory(hid=42, uniq_name='Хуракторы'),
        ]

        cls.index.offers += [
            Offer(
                title='promocode 1 offer',
                fesh=11,
                hid=ADULT_CATEG_ID,  # to avoid category redirects
                price=500,
                promo=Promo(
                    promo_type=PromoType.PROMO_CODE,
                    start_date=datetime(1980, 1, 1),
                    end_date=datetime(2050, 1, 1),
                    key='xMpCOKC5I4INzFCab3WEmw',
                    url='http://my.url',
                    promo_code="my promo code",
                    discount_value=300,
                    discount_currency='RUR',
                    purchases=[
                        PromoPurchase(category_id=41),
                    ],
                ),
            )
        ]

    def test_redirects(self):
        # no &promo-redirect=1 -- no gain
        response = self.report.request_json('place=prime&fesh=42,53&cvredirect=1&promo-type=All')
        self.assertFragmentNotIn(response, {"redirect": NotEmpty()})

        # no promo filters -- no gain
        response = self.report.request_json('place=prime&fesh=42,53&cvredirect=1&promo-redirect=1')
        self.assertFragmentNotIn(response, {"redirect": NotEmpty()})

        # something found -- no gain
        response = self.report.request_json('place=prime&fesh=11,12&cvredirect=1&promo-redirect=1&promo-type=All')
        self.assertFragmentNotIn(response, {"redirect": NotEmpty()})

        response = self.report.request_json('place=prime&fesh=42,53&cvredirect=1&promo-type=All&promo-redirect=1')
        self.assertFragmentIn(
            response,
            {
                "redirect": {
                    "params": {
                        "fesh": ["42", "53"],
                        "promo-type": ["market"],
                        "cvredirect": NoKey("cvredirect"),
                        "from-promo": ["1"],
                    },
                    "target": "search",
                }
            },
        )

        response = self.report.request_json('place=prime&fesh=42,53&cvredirect=1&promo-type=market&promo-redirect=1')
        self.assertFragmentIn(response, {"redirect": {"params": {"nid": ["61522"]}, "target": "search"}})


if __name__ == '__main__':
    main()
