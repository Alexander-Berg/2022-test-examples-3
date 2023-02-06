#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import Model, Outlet, Region, Shop
from core.types.sku import MarketSku, BlueOffer


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.shops += [
            Shop(fesh=13, priority_region=213, fulfillment_virtual=True, virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE),
            Shop(fesh=777, datafeed_id=777, priority_region=213, supplier_type=Shop.THIRD_PARTY, blue=Shop.BLUE_REAL),
        ]
        cls.index.regiontree += [
            Region(rid=213, name='Москва', region_type=Region.FEDERATIVE_SUBJECT),
        ]
        cls.index.outlets += [
            Outlet(fesh=777, region=213),
        ]

        # оффер с ценой ниже минимальной референсной
        blue_offer_lt = BlueOffer(waremd5='Blue-Offer-LT-RefMinPr', price=10, feedid=777, offerid='shop_sku_lt_ref_min')
        # оффер с ценой равной минимальной референсной
        blue_offer_eq = BlueOffer(waremd5='Blue-Offer-EQ-RefMinPr', price=20, feedid=777, offerid='shop_sku_eq_ref_min')
        # оффер с ценой выше минимальной референсной
        blue_offer_gt = BlueOffer(waremd5='Blue-Offer-GT-RefMinPr', price=30, feedid=777, offerid='shop_sku_gt_ref_min')

        cls.index.mskus += [
            MarketSku(
                title='blue market sku1',
                hyperid=1,
                sku=110011,
                ref_min_price=20,
                blue_offers=[blue_offer_lt, blue_offer_eq, blue_offer_gt],
            ),
        ]

        cls.index.models += [
            Model(hyperid=1, hid=1, title='blue and green model'),
        ]

    def test_print_doc(self):
        """Проверяем, что print_doc выводит минимальную референсную цену"""
        response = self.report.request_json('place=print_doc&market-sku=110011&offerid=Blue-Offer-EQ-RefMinPr')
        self.assertFragmentIn(response, {"refMinPrice": "20"})

    def test_ref_min_price_output(self):
        """Проверяем, минимальная референсная цена есть в выдаче place=offerinfo."""
        response = self.report.request_json(
            'place=offerinfo&rids=213&regset=2&market-sku=110011&offerid=Blue-Offer-GT-RefMinPr&rgb=blue'
        )
        self.assertFragmentIn(response, {"refMinPrice": {"currency": "RUR", "value": "20"}})


if __name__ == '__main__':
    main()
