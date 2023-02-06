#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    BlueOffer,
    DeliveryBucket,
    DynamicDeliveryRestriction,
    DynamicWarehouseDelivery,
    DynamicWarehouseLink,
    GpsCoord,
    MarketSku,
    Model,
    Offer,
    Outlet,
    PickupBucket,
    PickupOption,
    Shop,
    DynamicWarehouseInfo,
    DynamicWarehouseToWarehouseInfo,
)

USE_DEPRECATED_DIRECT_SHIPPING_FLOW = "&rearr-factors=get_rid_of_direct_shipping=0"

from core.testcase import TestCase, main
from core.matcher import Absent


class T(TestCase):
    @classmethod
    def prepare_market_sku_usage(cls):
        cls.settings.nordstream_autogenerate = False
        cls.settings.default_search_experiment_flags += ['market_nordstream=0']
        cls.index.shops += [
            Shop(fesh=1, priority_region=213, regions=[213, 225]),
            Shop(fesh=2, priority_region=213, regions=[213, 225]),
        ]

        cls.dynamic.lms += [
            DynamicWarehouseInfo(id=145, home_region=213),
            DynamicWarehouseToWarehouseInfo(warehouse_from=145, warehouse_to=145),
        ]

        cls.index.outlets += [
            Outlet(point_id=211, fesh=1, region=213, gps_coord=GpsCoord(37.1, 55.1), point_type=Outlet.FOR_STORE),
            Outlet(point_id=212, fesh=1, region=213, gps_coord=GpsCoord(38.0, 55.3)),
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=5001,
                fesh=1,
                carriers=[99],
                options=[
                    PickupOption(outlet_id=211),
                ],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5002,
                fesh=2,
                carriers=[99],
                options=[PickupOption(outlet_id=212)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]

        cls.index.mskus += [
            MarketSku(
                hyperid=1,
                sku=11,
                title="Бутылка синего молока",
                blue_offers=[BlueOffer(price=110, pickup_buckets=[5001])],
            ),
        ]

        cls.index.models += [
            Model(hid=10, hyperid=1, title="Бутылка белого молока"),
        ]

        cls.index.offers += [
            # white offer with sku
            Offer(fesh=1, hid=10, hyperid=1, sku=11, price=120, pickup_buckets=[5002]),
            # White offers without sku
            Offer(fesh=1, hid=10, hyperid=1, price=125, pickup_buckets=[5001]),
            Offer(fesh=2, hid=10, hyperid=1, price=130, pickup_buckets=[5002]),
        ]

    @classmethod
    def prepare_nordstream(cls):
        cls.dynamic.nordstream += [
            DynamicWarehouseLink(145, [145]),
            DynamicWarehouseDelivery(
                145, {region: [DynamicDeliveryRestriction(min_days=1, max_days=2)] for region in (213, 225)}
            ),
        ]

    def test_geo_filters_by_market_sku(self):
        '''
        Тестируем, что если передать параметр market-sku вместо hyperid, то geo возвращать будет оффера с таким sku
        '''

        request = 'place=geo&market-sku=11&debug=1' + USE_DEPRECATED_DIRECT_SHIPPING_FLOW
        response = self.report.request_json(request)

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "offer", "sku": "11", "prices": {"value": "120"}},
                    {"entity": "offer", "sku": "11", "prices": {"value": "110"}},
                ]
            },
            allow_different_len=False,
        )

    def test_geo_filters_by_zero_sku(self):
        '''
        Тестируем, что если передать параметр market-sku=0, то geo возвращать будет только оффера без sku (так называемый кадавр)
        '''

        request = 'place=geo&market-sku=0&hyperid=1&debug=1'
        response = self.report.request_json(request)

        # skuKadaver заполняется только при наличии флага generate_kadavers
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "offer", "sku": Absent(), "skuKadaver": Absent(), "prices": {"value": "130"}},
                    {"entity": "offer", "sku": Absent(), "skuKadaver": Absent(), "prices": {"value": "125"}},
                ]
            },
            allow_different_len=False,
        )

        request = 'place=geo&market-sku=0&hyperid=1&how=distance&geo-location=37.15,55.15&debug=1'
        response = self.report.request_json(request)

        self.assertFragmentIn(
            response,
            {
                "results": [
                    {"entity": "offer", "sku": Absent(), "skuKadaver": Absent(), "prices": {"value": "125"}},
                    {"entity": "offer", "sku": Absent(), "skuKadaver": Absent(), "prices": {"value": "130"}},
                ]
            },
            allow_different_len=False,
            preserve_order=True,
        )


if __name__ == '__main__':
    main()
