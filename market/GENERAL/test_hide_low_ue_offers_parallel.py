#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import BlueOffer, MarketSku, Region, Shop, DynamicWarehouseInfo, DynamicWarehouseToWarehouseInfo
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
        cls.settings.nordstream_autogenerate = False
        cls.settings.default_search_experiment_flags += ['market_nordstream=0']
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
            )
        ]

        cls.dynamic.lms += [
            DynamicWarehouseInfo(id=145, home_region=213),
            DynamicWarehouseToWarehouseInfo(warehouse_from=145, warehouse_to=145),
        ]

        def blue_offer(shop_sku, price):
            return BlueOffer(
                offerid=shop_sku,
                price=price,
                hyperid=HID_GLOBAL,
            )

        cls.index.mskus += [
            MarketSku(sku=SKU, hyperid=HID_GLOBAL, blue_offers=[blue_offer("blue_offer", 400)], title='iphone X'),
        ]

        cls.index.low_ue_mskus += [SKU]

    def get_offer_with_title(self, name):
        return {
            "market_model": [
                {
                    "showcase": {
                        "items": [
                            {
                                "title": {
                                    "text": {"__hl": {"text": name, "raw": True}},
                                }
                            }
                        ]
                    }
                }
            ]
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

    def send_parallel_request(self, rids=None, text=None, set_flag=None):
        request = 'place=parallel'
        request = self.add_param(request, 'rids', rids)
        request = self.add_param(request, 'text', text)
        request = self.add_flag(request, 'hide_low_ue_beru_offers', set_flag)
        return self.report.request_bs(request)

    def test_parallel_offer_is_low_ue_with_flag(self):
        response = self.send_parallel_request(rids=RegionsData.FIRST_CITY.rid, text='iphone X', set_flag=True)
        self.assertFragmentNotIn(response, self.get_offer_with_title('iphone X'))

    def test_parallel_offer_is_visible_without_flag(self):
        response = self.send_parallel_request(rids=RegionsData.FIRST_CITY.rid, text='iphone X', set_flag=False)
        self.assertFragmentIn(response, self.get_offer_with_title('iphone X'))


if __name__ == '__main__':
    main()
