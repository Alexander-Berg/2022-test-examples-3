#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import BlueOffer, Currency, GLParam, GLType, MarketSku, Model, Shop, Tax


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.shops += [
            Shop(
                fesh=120,
                datafeed_id=1,
                priority_region=213,
                regions=[213],
                name='virtual_shop',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                fulfillment_virtual=True,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
                cpa=Shop.CPA_REAL,
            ),
        ]

        cls.index.models += [
            Model(hyperid=111, hid=101),
        ]

        cls.index.gltypes += [
            GLType(param_id=501, hid=101, cluster_filter=True, gltype=GLType.ENUM),
        ]

        cls.index.mskus += [
            MarketSku(
                title="white iphone",
                blue_offers=[BlueOffer(waremd5='AAAAAAAAAAAAAAAAAAAAAB', price=200)],
                sku=11,
                hyperid=111,
                glparams=[
                    GLParam(param_id=501, value=1),
                ],
                published=True,
            ),
            MarketSku(
                title="gray iphone",
                blue_offers=[BlueOffer(waremd5='AAAAAAAAAAAAAAAAAAAABA', price=250)],
                sku=12,
                hyperid=111,
                glparams=[
                    GLParam(param_id=501, value=2),
                ],
                published=True,
            ),
            MarketSku(
                title="black iphone",
                blue_offers=[BlueOffer(waremd5='AAAAAAAAAAAAAAAAAAAABB', price=260)],
                sku=13,
                hyperid=111,
                glparams=[
                    GLParam(param_id=501, value=3),
                ],
                published=False,
            ),
        ]

    def test_sku_offers(self):
        """place=sku_offers: Неопубликованные msku не должны показываться в таблице переходов"""
        for pipeline in [0, 1]:
            # Unpublished msku filter is enabled by default
            response = self.report.request_json(
                'place=sku_offers&market-sku=11&show-urls=direct&rearr-factors=use_new_jump_table_pipeline={}'.format(
                    pipeline
                )
            )
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            "entity": "sku",
                            "id": "11",
                            "filters": [
                                {
                                    "values": [
                                        {
                                            "marketSku": "11",  # ours, checked
                                        },
                                        {
                                            "marketSku": "12",  # ours, checked
                                        },
                                    ]
                                }
                            ],
                        }
                    ]
                },
                allow_different_len=False,
                preserve_order=False,
            )

    def test_sku_search(self):
        """place=sku_search: Неопубликованные msku не должны находиться поиском"""

        response = self.report.request_json('place=sku_search&text2=iphone&rgb=blue')
        self.assertFragmentIn(
            response,
            {
                "total": 2,
                "results": [
                    {"entity": "sku", "titles": {"raw": "white iphone"}},
                    {"entity": "sku", "titles": {"raw": "gray iphone"}},
                ],
            },
            allow_different_len=False,
        )

    def test_ignore_rule(self):
        """place=sku_search: Неопубликованные msku находиться поиском, если скрытие запрещено"""

        response = self.report.request_json(
            'place=sku_search&text2=iphone&rgb=blue&rearr-factors=show_unpublished_msku=1'
        )
        self.assertFragmentIn(
            response,
            {
                "total": 3,
                "results": [
                    {"entity": "sku", "titles": {"raw": "white iphone"}},
                    {"entity": "sku", "titles": {"raw": "black iphone"}},
                    {"entity": "sku", "titles": {"raw": "gray iphone"}},
                ],
            },
            allow_different_len=False,
        )


if __name__ == '__main__':
    main()
