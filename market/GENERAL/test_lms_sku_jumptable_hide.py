#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    Currency,
    DateSwitchTimeAndRegionInfo,
    DeliveryBucket,
    DeliveryOption,
    DynamicDeliveryRestriction,
    DynamicDeliveryServiceInfo,
    DynamicWarehouseAndDeliveryServiceInfo,
    DynamicWarehouseDelivery,
    DynamicWarehouseInfo,
    DynamicWarehouseToWarehouseInfo,
    DynamicWarehouseLink,
    DynamicWarehousesPriorityInRegion,
    GLParam,
    GLType,
    Model,
    Region,
    RegionalDelivery,
    Shop,
)
from core.testcase import TestCase, main
from core.types.sku import MarketSku, BlueOffer
from core.types.taxes import Vat, Tax
import copy


def get_warehouse_and_delivery_service(warehouse_id, service_id):
    date_switch_hours = [
        DateSwitchTimeAndRegionInfo(date_switch_hour=1, region_to=213),
        DateSwitchTimeAndRegionInfo(date_switch_hour=1, region_to=214),
    ]
    return DynamicWarehouseAndDeliveryServiceInfo(
        warehouse_id=warehouse_id,
        delivery_service_id=service_id,
        operation_time=0,
        date_switch_time_infos=date_switch_hours,
        shipment_holidays_days_set_key=6,
    )


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.nordstream_autogenerate = False
        cls.settings.default_search_experiment_flags += ['market_nordstream=0']

        cls.index.regiontree += [
            Region(rid=213, region_type=Region.CITY),
            Region(rid=214, region_type=Region.CITY),
        ]

        cls.index.shops += [
            Shop(
                fesh=1,
                datafeed_id=1,
                priority_region=213,
                name='virtual_shop',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                fulfillment_virtual=True,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
                cpa=Shop.CPA_REAL,
            ),
            Shop(
                fesh=1,
                datafeed_id=3,
                priority_region=213,
                name='blue_shop_1',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                supplier_type=Shop.FIRST_PARTY,
                blue='REAL',
                cpa=Shop.CPA_REAL,
                warehouse_id=145,
            ),
            Shop(
                fesh=1,
                datafeed_id=2,
                priority_region=214,
                name='blue_shop_2',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                supplier_type=Shop.FIRST_PARTY,
                blue='REAL',
                cpa=Shop.CPA_REAL,
                warehouse_id=147,
            ),
        ]

        cls.index.gltypes += [
            GLType(
                param_id=201,
                hid=1,
                cluster_filter=True,
                gltype=GLType.ENUM,
                values=[1, 2, 3],
                model_filter_index=322,
            ),
        ]

        cls.settings.lms_autogenerate = False
        cls.dynamic.lms += [
            DynamicWarehouseInfo(id=147, home_region=213, holidays_days_set_key=4),
            DynamicWarehouseInfo(id=145, home_region=213, holidays_days_set_key=4),
            DynamicWarehouseToWarehouseInfo(warehouse_from=147, warehouse_to=147),
            DynamicWarehouseToWarehouseInfo(warehouse_from=145, warehouse_to=145),
            DynamicWarehousesPriorityInRegion(
                region=214,
                warehouses=[
                    147,
                ],
            ),
            DynamicWarehousesPriorityInRegion(
                region=213,
                warehouses=[
                    145,
                ],
            ),
            get_warehouse_and_delivery_service(145, 257),
            get_warehouse_and_delivery_service(147, 258),
            DynamicDeliveryServiceInfo(257, "c_257"),
            DynamicDeliveryServiceInfo(258, "c_258"),
        ]

        cls.index.lms = copy.deepcopy(cls.dynamic.lms)

        cls.dynamic.nordstream += [
            DynamicWarehouseLink(145, [145]),
            DynamicWarehouseLink(147, [147]),
        ]

        def create_simple_restriction():
            return DynamicDeliveryRestriction(
                max_phys_weight=40000,
                max_dim_sum=3000,
                max_dimensions=[1000, 1000, 1000],
                min_days=3,
                max_days=4,
            )

        cls.dynamic.nordstream += [
            DynamicWarehouseDelivery(
                145,
                {
                    213: [
                        create_simple_restriction(),
                    ],
                },
            ),
            DynamicWarehouseDelivery(
                147,
                {
                    214: [
                        create_simple_restriction(),
                    ],
                },
            ),
        ]

        # Добавляем статические объекты для СД в индекс
        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=113,
                fesh=1,
                carriers=[
                    257,
                ],
                regional_options=[
                    RegionalDelivery(
                        rid=213, options=[DeliveryOption(price=100, day_from=0, day_to=2, order_before=24)]
                    ),
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
            DeliveryBucket(
                bucket_id=125,
                fesh=1,
                carriers=[
                    258,
                ],
                regional_options=[
                    RegionalDelivery(
                        rid=214, options=[DeliveryOption(price=100, day_from=0, day_to=2, order_before=24)]
                    ),
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
        ]

        cls.index.models += [
            Model(
                hyperid=1,
                hid=1,
                title='blue and green model',
                glparams=[
                    GLParam(param_id=201, value=1),
                    GLParam(param_id=201, value=2),
                    GLParam(param_id=201, value=3),
                ],
            ),
        ]

        cls.index.mskus += [
            MarketSku(
                title="blue offer sku1 john",
                hyperid=1,
                sku=1,
                blue_offers=[
                    BlueOffer(
                        price=6,
                        price_old=8,
                        vat=Vat.VAT_10,
                        feedid=3,
                        offerid='blue.offer.1.1',
                        waremd5='Sku1Price6-IiLVm1Goleg',
                        delivery_buckets=[113],
                    ),
                    BlueOffer(
                        price=7,
                        price_old=8,
                        vat=Vat.VAT_10,
                        feedid=2,
                        offerid='blue.offer.1.2',
                        waremd5='Sku1Price7-IiLVm1Goleg',
                        delivery_buckets=[125],
                    ),
                ],
                glparams=[
                    GLParam(param_id=201, value=1),
                ],
            ),
            MarketSku(
                title="blue offer sku2",
                hyperid=1,
                sku=2,
                blue_offers=[
                    BlueOffer(
                        price=6,
                        price_old=8,
                        vat=Vat.VAT_10,
                        feedid=3,
                        offerid='blue.offer.2.1',
                        waremd5='Sku2Price6-IiLVm1Goleg',
                        delivery_buckets=[113],
                    ),
                    BlueOffer(
                        price=7,
                        price_old=8,
                        vat=Vat.VAT_10,
                        feedid=2,
                        offerid='blue.offer.2.2',
                        waremd5='Sku2Price7-IiLVm1Goleg',
                        delivery_buckets=[125],
                    ),
                ],
                glparams=[
                    GLParam(param_id=201, value=2),
                ],
            ),
            MarketSku(
                title="blue offer sku3",
                hyperid=1,
                sku=3,
                blue_offers=[
                    BlueOffer(
                        price=5,
                        vat=Vat.VAT_10,
                        feedid=2,
                        offerid='blue.offer3.1',
                        waremd5='Sku3Price5-IiLVm1Goleg',
                        delivery_buckets=[125],
                    )
                ],
                glparams=[
                    GLParam(param_id=201, value=3),
                ],
            ),
        ]

    def test_lms_hide(self):
        """
        Что тестируем: скрытие ску без офферов в запрашиваемом регионе
        """

        for rearr in [
            '',
            '&rearr-factors=market_nordstream=1',
        ]:
            base_req = 'place=sku_offers&market-sku=1&show-urls=direct&rids={}' + rearr
            response = self.report.request_json(base_req.format(214))
            # Запрашиваем sku 1 для региона где у всех есть офферы
            # все sku должны быть в jump_table
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            "entity": "sku",
                            "id": "1",
                            "filters": [
                                {
                                    "id": "201",
                                    "values": [
                                        {
                                            "id": "1",
                                            "marketSku": "1",
                                        },
                                        {
                                            "id": "2",
                                            "marketSku": "2",
                                        },
                                        {
                                            "id": "3",
                                            "marketSku": "3",
                                        },
                                    ],
                                },
                            ],
                        }
                    ]
                },
                allow_different_len=False,
                preserve_order=True,
            )

            response = self.report.request_json(base_req.format(213))
            # Запрашиваем sku 1 для региона где нет офферов у sku 3
            # sku 3 должен быть исключён из jump_table
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            "entity": "sku",
                            "id": "1",
                            "filters": [
                                {
                                    "id": "201",
                                    "values": [
                                        {
                                            "id": "1",
                                            "marketSku": "1",
                                        },
                                        {
                                            "id": "2",
                                            "marketSku": "2",
                                        },
                                    ],
                                },
                            ],
                        }
                    ]
                },
                allow_different_len=False,
                preserve_order=True,
            )

    def test_lms_hidden(self):
        """
        Что тестируем: скрытие ску без офферов в запрашиваемом регионе
        для ску без офферов в запрашиваемом регионе
        """

        for rearr in [
            '',
            '&rearr-factors=market_nordstream=1',
        ]:
            base_req = 'place=sku_offers&market-sku=3&show-urls=direct&rids={}' + rearr
            response = self.report.request_json(base_req.format(214))
            # Запрашиваем sku 3 для региона где у всех есть офферы
            # все sku должны быть в jump_table
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            "entity": "sku",
                            "id": "3",
                            "filters": [
                                {
                                    "id": "201",
                                    "values": [
                                        {
                                            "id": "1",
                                            "marketSku": "1",
                                        },
                                        {
                                            "id": "2",
                                            "marketSku": "2",
                                        },
                                        {
                                            "id": "3",
                                            "marketSku": "3",
                                        },
                                    ],
                                },
                            ],
                        }
                    ]
                },
                allow_different_len=False,
                preserve_order=True,
            )

            base_req = 'place=sku_offers&market-sku=3&show-urls=direct&rids={}'
            response = self.report.request_json(base_req.format(213))
            # Запрашиваем sku 3 для региона где нет офферов у sku 3
            # sku 3 должен присутствовать в jump table, т.к. является запрашиваемым
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            "entity": "sku",
                            "id": "3",
                            "filters": [
                                {
                                    "id": "201",
                                    "values": [
                                        {
                                            "id": "1",
                                            "marketSku": "1",
                                        },
                                        {
                                            "id": "2",
                                            "marketSku": "2",
                                        },
                                        {
                                            "id": "3",
                                            "marketSku": "3",
                                        },
                                    ],
                                },
                            ],
                        }
                    ]
                },
                allow_different_len=False,
                preserve_order=True,
            )


if __name__ == '__main__':
    main()
