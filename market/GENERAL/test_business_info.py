#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import Shop
from core.matcher import NotEmpty


class T(TestCase):
    @classmethod
    def prepare(cls):

        cls.index.shops += [
            Shop(
                fesh=1,
                datafeed_id=1,
                priority_region=213,
                name='ВиртуальныйМагазинНаБеру',
                fulfillment_virtual=True,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
                business_fesh=1,
            ),
            Shop(
                fesh=31,
                datafeed_id=31,
                priority_region=213,
                regions=[225],
                name="3P поставщик Вася",
                supplier_type=Shop.THIRD_PARTY,
                is_supplier=True,
                blue=Shop.BLUE_REAL,
                warehouse_id=145,
                fulfillment_program=True,
                business_fesh=2,
                phone=None,
            ),
            Shop(
                fesh=29,
                business_fesh=3,
                business_name="petya",
                business_metrika_counter_id=333,
                datafeed_id=29,
                priority_region=213,
                regions=[225],
                name="Петя в отрубе",
                is_enabled=False,
            ),
            Shop(
                fesh=32,
                datafeed_id=32,
                priority_region=213,
                regions=[225],
                name="3P поставщик Петя",
                supplier_type=Shop.THIRD_PARTY,
                is_supplier=True,
                blue=Shop.BLUE_REAL,
                warehouse_id=145,
                fulfillment_program=True,
                business_name="petya",
                business_fesh=3,
                business_metrika_counter_id=333,
                phone='+76665554433',
                shop_logo_url="www.slu.jpg",
                shop_logo_info="www.sli.jpg",
            ),
            Shop(
                fesh=111,
                business_fesh=3,
                business_name="petya",
                business_metrika_counter_id=333,
                datafeed_id=110,
                shop_logo_retina_url="www.slru.jpg",
                priority_region=213,
                regions=[225],
                name="простой белый магазин Пети",
            ),
            Shop(
                fesh=1110,
                business_fesh=3,
                business_name="petya",
                business_metrika_counter_id=333,
                datafeed_id=1100,
                priority_region=213,
                regions=[225],
                name="dsbs белый магазин Пети",
                cpa=Shop.CPA_REAL,
            ),
        ]

    def test_business_info(self):
        response = self.report.request_json('place=business_info&pp=18&fesh=3')

        self.assertFragmentIn(
            response,
            {
                "business_id": 3,
                "business_name": "petya",
                "business_metrika_counter_id": 333,
                "shop_name": "3P поставщик Петя",
                "shop_logo_url": "www.slu.jpg",
                "shop_logo_retina_url": "www.slru.jpg",
                "shop_logo_info": "www.sli.jpg",
                "slug": "3p-postavshchik-petia",
                "shop_phone": "+76665554433",
                "shop_info": [
                    {"shop_id": 32, "supplier_type": 3},
                    {"shop_id": 1110, "supplier_type": 0},
                    {"shop_id": 111, "supplier_type": 0},
                ],
            },
        )

    def test_business_info_without_phone(self):
        response = self.report.request_json('place=business_info&pp=18&fesh=2')

        self.assertFragmentIn(response, {"business_id": 2, "shop_name": "3P поставщик Вася"})

        self.assertFragmentNotIn(response, {"shop_phone": NotEmpty()})


if __name__ == '__main__':
    main()
