#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import BlueOffer, MarketSku, Region, Shop
from core.testcase import TestCase, main

HID_GLOBAL = 10
SKU = 20
BLUE_VIRTUAL_FESH = 3


class RegionsData:
    class RidWithMinOrder:
        def __init__(self, rid, value):
            self.rid = rid
            self.value = value

    ROOT_REGION = RidWithMinOrder(10, None)  # значение минимального заказа для данного региона не задано
    FIRST_CITY = RidWithMinOrder(100, 500)


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.regiontree += [
            Region(
                rid=RegionsData.ROOT_REGION.rid,
                region_type=Region.CITY,
                children=[
                    Region(rid=RegionsData.FIRST_CITY.rid, region_type=Region.CITY),
                ],
            ),
        ]

        cls.index.shops += [
            Shop(
                fesh=BLUE_VIRTUAL_FESH,
                datafeed_id=BLUE_VIRTUAL_FESH,
                priority_region=RegionsData.ROOT_REGION.rid,
                name='virtual_shop',
                fulfillment_virtual=True,
                cpa=Shop.CPA_REAL,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
            ),
            Shop(
                fesh=BLUE_VIRTUAL_FESH + 1,
                datafeed_id=BLUE_VIRTUAL_FESH + 1,
                priority_region=RegionsData.ROOT_REGION.rid,
                name='blue_shop',
                warehouse_id=145,
            ),
        ]

        def blue_offer(shop_sku, price):
            return BlueOffer(
                offerid=shop_sku,
                price=price,
                hyperid=HID_GLOBAL,
            )

        cls.index.mskus += [
            MarketSku(sku=SKU, hyperid=HID_GLOBAL, blue_offers=[blue_offer("blue_offer", 400)]),
            MarketSku(sku=SKU + 1, hyperid=HID_GLOBAL, blue_offers=[blue_offer("blue_offer_2", 400)]),
        ]

        cls.index.low_ue_mskus += [SKU]

    def get_offer(self, offer_id):
        return {
            "entity": "offer",
            "shopSku": offer_id,
        }

    def add_param(self, request, param_name, param_value):
        if param_value is not None:
            request += '&{}={}'.format(param_name, param_value)
        return request

    def add_flag(self, request, flag_name, set_flag):
        return self.add_param(
            request,
            'rearr-factors',
            ('{}={}'.format(flag_name, '1' if set_flag else '0')) if (set_flag is not None) else None,
        )

    def send_productoffers_request(self, region, set_flag=None):
        request = 'place=productoffers&hyperid={}&rids={}'.format(HID_GLOBAL, region.rid)
        request = self.add_flag(request, 'hide_low_ue_beru_offers', set_flag)
        return self.report.request_json(request)

    def test_offer_is_not_low_ue_with_flag_disabled(self):
        self.assertFragmentIn(
            self.send_productoffers_request(RegionsData.FIRST_CITY, set_flag=False), self.get_offer("blue_offer")
        )

    def test_offer_is_low_ue_with_flag_enabled(self):
        self.assertFragmentNotIn(
            self.send_productoffers_request(RegionsData.FIRST_CITY, set_flag=True), self.get_offer("blue_offer")
        )

    def test_offer_is_low_ue_with_flag_default(self):
        self.assertFragmentNotIn(self.send_productoffers_request(RegionsData.FIRST_CITY), self.get_offer("blue_offer"))

    def test_offer_is_not_low_ue_with_flag_enabled(self):
        self.assertFragmentIn(
            self.send_productoffers_request(RegionsData.FIRST_CITY, set_flag=True), self.get_offer("blue_offer_2")
        )


if __name__ == '__main__':
    main()
