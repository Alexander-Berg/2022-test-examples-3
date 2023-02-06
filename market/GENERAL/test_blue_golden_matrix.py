#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import Model, Region, Shop
from core.types.sku import MarketSku, BlueOffer


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.shops += [
            Shop(fesh=1, priority_region=213, fulfillment_virtual=True, virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE),
            Shop(fesh=2, datafeed_id=2, priority_region=213, supplier_type=Shop.THIRD_PARTY, blue=Shop.BLUE_REAL),
        ]
        cls.index.regiontree += [
            Region(rid=213, name='Москва', region_type=Region.FEDERATIVE_SUBJECT),
        ]

        # оффер с msku в Золотой матрице
        blue_offer_in_golden_matrix_1 = BlueOffer(
            waremd5='BlueOffer1-IN-GoldenMt', price=10, feedid=2, offerid='shop_sku_in_gold_1'
        )
        blue_offer_in_golden_matrix_2 = BlueOffer(
            waremd5='BlueOffer2-IN-GoldenMt', price=30, feedid=1, offerid='shop_sku_in_gold_2'
        )
        # оффер с msku не в Золотой матрице
        blue_offer_not_golden_matrix = BlueOffer(
            waremd5='BlueOffer-OUT-GoldenMt', price=20, feedid=1, offerid='shop_sku_out_gold_mx'
        )

        cls.index.mskus += [
            MarketSku(
                title='Golden Matrix Sku #1',
                hyperid=1,
                sku=777999,
                waremd5='MarketSku1-IiLVm1goleg',
                is_golden_matrix=True,
                blue_offers=[blue_offer_in_golden_matrix_1, blue_offer_in_golden_matrix_2],
            ),
            MarketSku(
                title='Simple Sku #2',
                hyperid=1,
                waremd5='MarketSku2-IiLVm1goleg',
                sku=888999,
                is_golden_matrix=False,
                blue_offers=[blue_offer_not_golden_matrix],
            ),
        ]

        cls.index.models += [
            Model(hyperid=1, hid=1, title='blue and green model'),
        ]

    def test_offerinfo_golden_matrix(self):
        """Проверяем, что print_doc выводит флаг принадлежности Золотой матрице"""
        response = self.report.request_json(
            'place=offerinfo&market-sku=777999&offerid=BlueOffer1-IN-GoldenMt&regset=2&rids=213&rgb=blue'
        )
        self.assertFragmentIn(response, {"isGoldenMatrix": True})

    def test_offerinfo_not_in_golden_matrix(self):
        """Проверяем, что print_doc выводит флаг принадлежности Золотой матрице"""
        response = self.report.request_json(
            'place=offerinfo&market-sku=888999&offerid=BlueOffer-OUT-GoldenMt&regset=2&rids=213'
        )
        self.assertFragmentIn(response, {"isGoldenMatrix": False})


if __name__ == '__main__':
    main()
