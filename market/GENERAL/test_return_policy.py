#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import HyperCategory, Offer, Region, ReturnPolicy, Shop
from core.testcase import TestCase, main
from core.matcher import NoKey


class T(TestCase):
    @classmethod
    def prepare_regional_return_policy(cls):
        cls.index.regiontree += [
            Region(
                rid=50,
                name='Белоруссия',
                region_type=Region.COUNTRY,
                children=[
                    Region(rid=101, name='Минск'),
                ],
            ),
            Region(
                rid=200,
                name='Россия',
                region_type=Region.COUNTRY,
                children=[
                    Region(rid=213, name='Москва'),
                ],
            ),
        ]

        cls.index.hypertree += [
            HyperCategory(
                hid=101,
                name='Электроника',
                return_policies_by_region=[
                    ReturnPolicy(200, '14d'),
                    ReturnPolicy(50, '7d'),
                ],
                children=[HyperCategory(hid=201, name='Телефоны')],
            ),
            HyperCategory(
                hid=301,
                name='Одежда',
                children=[
                    HyperCategory(
                        hid=401,
                        name='Платья',
                        goods_return_policy='10d',
                        return_policies_by_region=[
                            ReturnPolicy(200, '14d'),
                            ReturnPolicy(50, None),
                        ],
                    )
                ],
            ),
        ]

        cls.index.shops += [
            Shop(fesh=1001, regions=[213], name='Moscow'),
            Shop(fesh=1002, regions=[101], name='Minsk'),
        ]

        cls.index.offers += [
            Offer(title='Phone Moscow', fesh=1001, hid=201),
            Offer(title='Phone Minsk', fesh=1002, hid=201),
            Offer(title='Dress Moscow', fesh=1001, hid=401),
            Offer(title='Dress Minsk', fesh=1002, hid=401),
        ]

    def test_regional_return_policy(self):
        """
        Проверяем, что политика для разных городов прокидывается как с самой категории, а если категории нет,
        то с родительской.
        """

        # берем с родительской категории
        response = self.report.request_json("place=prime&hid=201&rids=213")
        self.assertFragmentIn(
            response,
            {
                "titles": {"raw": "Phone Moscow"},
                "returnPolicy": "14d",
            },
        )

        response = self.report.request_json("place=prime&hid=201&rids=101")
        self.assertFragmentIn(
            response,
            {
                "titles": {"raw": "Phone Minsk"},
                "returnPolicy": "7d",
            },
        )

        # берем с самой категории, игнорируем return_policy по умолчанию
        response = self.report.request_json("place=prime&hid=401&rids=213")
        self.assertFragmentIn(
            response,
            {
                "titles": {"raw": "Dress Moscow"},
                "returnPolicy": "14d",
            },
        )

        # пустой returnPolicy - в ответе тоже нет return policy
        response = self.report.request_json("place=prime&hid=401&rids=101")
        self.assertFragmentIn(
            response,
            {
                "titles": {"raw": "Dress Minsk"},
                "returnPolicy": NoKey("returnPolicy"),
            },
        )


if __name__ == '__main__':
    main()
