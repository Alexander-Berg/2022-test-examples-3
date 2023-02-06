#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    BlueOffer,
    HyperCategory,
    HyperCategoryType,
    MarketSku,
    MinOrderCostByRegion,
    Model,
    NavCategory,
    Offer,
    Region,
    Shop,
)
from core.testcase import TestCase, main
from core.matcher import Absent

HID_GLOBAL = 10
SKU = 21
SKU_WHITE = 22
BLUE_FESH = 2
BLUE_VIRTUAL_FESH = 3
WHITE_FESH = 4
WHITE_DEFAULT_COST = 10000
BLUE_DEFAULT_COST = 11000


class RegionsData:
    class RidWithMinOrder:
        def __init__(self, rid, value):
            self.rid = rid
            self.value = value

    ROOT_REGION = RidWithMinOrder(10, None)  # значение минимального заказа для данного региона не задано
    FIRST_LEVEL_CITY = RidWithMinOrder(100, 555)
    ANCESTOR_WITH_OVERRIDE = RidWithMinOrder(1001, 444)
    ANCESTOR_WITHOUT_OVERRIDE = RidWithMinOrder(1002, None)


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.regiontree += [
            Region(
                rid=RegionsData.ROOT_REGION.rid,
                region_type=Region.CITY,
                children=[
                    Region(
                        rid=RegionsData.FIRST_LEVEL_CITY.rid,
                        region_type=Region.CITY,
                        children=[
                            Region(
                                rid=RegionsData.ANCESTOR_WITH_OVERRIDE.rid,
                                region_type=Region.CITY,
                            ),
                            Region(
                                rid=RegionsData.ANCESTOR_WITHOUT_OVERRIDE.rid,
                                region_type=Region.CITY,
                            ),
                        ],
                    ),
                ],
            ),
        ]

        cls.index.min_blue_order_cost_by_region = [
            MinOrderCostByRegion(region=region.rid, cost=region.value)
            for region in [RegionsData.FIRST_LEVEL_CITY, RegionsData.ANCESTOR_WITH_OVERRIDE]
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
                order_min_cost=BLUE_DEFAULT_COST,
            ),
            Shop(
                fesh=BLUE_FESH,
                datafeed_id=BLUE_FESH,
                priority_region=RegionsData.ROOT_REGION.rid,
                name='blue_shop_1',
                blue=Shop.BLUE_REAL,
                cpa=Shop.CPA_REAL,
            ),
            Shop(
                fesh=WHITE_FESH,
                priority_region=RegionsData.ROOT_REGION.rid,
                order_min_cost=WHITE_DEFAULT_COST,
            ),
        ]

        cls.index.hypertree = [HyperCategory(hid=HID_GLOBAL, output_type=HyperCategoryType.GURU)]

        cls.index.navtree_blue = [NavCategory(nid=HID_GLOBAL, hid=HID_GLOBAL)]

        cls.index.models += [
            Model(hyperid=HID_GLOBAL, hid=HID_GLOBAL),
        ]

        def blue_offer(shop_sku, price):
            return BlueOffer(
                offerid=shop_sku,
                price=price,
                hyperid=HID_GLOBAL,
            )

        def offer(shop_sku, price):
            return Offer(offerid=shop_sku, price=price, fesh=WHITE_FESH, hyperid=HID_GLOBAL, sku=SKU_WHITE)

        cls.index.mskus += [
            MarketSku(
                sku=SKU,
                hyperid=HID_GLOBAL,
                blue_offers=[blue_offer("blue_offer", 10)],
            ),
        ]
        cls.index.offers += [offer("white_offer", 10)]

    def get_offer_with_min_order_cost(self, shop_sku, fesh, order_min_cost):
        return {
            "entity": "offer",
            "shopSku": shop_sku,
            "shop": {
                "id": fesh,
            },
            "orderMinCost": {
                "value": str(order_min_cost),
            },
        }

    def get_shops_pattern(self, white_min_cost, blue_min_cost):
        return {
            "results": [
                self.get_offer_with_min_order_cost(ssku, fesh, min_cost)
                for ssku, fesh, min_cost in [
                    ("blue_offer", BLUE_VIRTUAL_FESH, blue_min_cost),
                    (Absent(), WHITE_FESH, white_min_cost),
                ]
            ]
        }

    def check_region(self, region, blue_expected=None):
        '''
        если не задано значение, которое должно быть у синего оффера - ожидаем значение установленную для региона, как минимальную сумму
        если значение для региона не задано - ожидаем дефолтную минимальную сумму для магазина
        '''
        if blue_expected is None:
            blue_expected = region.value
        if blue_expected is None:
            blue_expected = BLUE_DEFAULT_COST

        response = self.report.request_json('place=productoffers&hyperid={}&rids={}'.format(HID_GLOBAL, region.rid))
        self.assertFragmentIn(response, self.get_shops_pattern(WHITE_DEFAULT_COST, blue_expected))

    def test_min_order_price_simple(self):
        '''
        получаем данные о минимальной сумме заказа для региона, для которого (также как и для его родителей)
        нет дополнительной информации о минимальной сумме заказа на синем
        (ожидаем дефолтное значение)
        '''
        self.check_region(RegionsData.ROOT_REGION, BLUE_DEFAULT_COST)

    def test_min_order_price_with_specified_min_order_cost(self):
        '''
        получаем данные о минимальной сумме заказа для региона, для которого
        есть дополнительная информация о минимальной сумме заказа на синем (а для его родителей - нет)
        (ожидаем значение указанное для данного региона в файле)
        '''
        self.check_region(RegionsData.FIRST_LEVEL_CITY)

    def test_min_order_price_with_specified_min_order_cost_override(self):
        '''
        получаем данные о минимальной сумме заказа для региона, для которого
        есть дополнительная информация о минимальной сумме заказа на синем
        (ожидаем значение указанное для данного региона в файле)
        '''
        self.check_region(RegionsData.ANCESTOR_WITH_OVERRIDE)

    def test_min_order_price_with_specified_min_order_cost_inheritance(self):
        '''
        получаем данные о минимальной сумме заказа для региона, для которого
        нет дополнительной информации о минимальной сумме заказа на синем, но такая информация есть для родительского региона
        (ожидаем значение минимальной суммы для родительского региона)
        '''
        self.check_region(RegionsData.ANCESTOR_WITHOUT_OVERRIDE, RegionsData.FIRST_LEVEL_CITY.value)


if __name__ == '__main__':
    main()
