#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    Currency,
    DateSwitchTimeAndRegionInfo,
    DeliveryBucket,
    DeliveryOption,
    DynamicDeliveryServiceInfo,
    DynamicWarehouseAndDeliveryServiceInfo,
    DynamicWarehouseInfo,
    DynamicWarehouseToWarehouseInfo,
    DynamicWarehousesPriorityInRegion,
    GLParam,
    GLType,
    HyperCategory,
    Model,
    NavCategory,
    NavRecipe,
    NavRecipeFilter,
    Outlet,
    OutletDeliveryOption,
    PickupBucket,
    PickupOption,
    ProhibitedBlueOffers,
    Region,
    RegionalDelivery,
    Shop,
)
from core.testcase import TestCase, main
from core.matcher import NidsInfoBucket, NidsInfo, BucketRestriction
from core.types.sku import MarketSku, BlueOffer
from core.types.taxes import Tax
from core.types.autogen import Const

import copy


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.hypertree = [
            HyperCategory(
                hid=1,
                children=[
                    HyperCategory(hid=11),
                    HyperCategory(hid=12),
                ],
            ),
            HyperCategory(
                hid=2,
                children=[
                    HyperCategory(hid=21),
                    HyperCategory(hid=22),
                ],
            ),
            HyperCategory(
                hid=3,
                children=[
                    HyperCategory(hid=31),
                    HyperCategory(hid=32),
                ],
            ),
            HyperCategory(
                hid=4,
                children=[
                    HyperCategory(hid=41),
                    HyperCategory(hid=42),
                ],
            ),
            HyperCategory(
                hid=5,
                children=[
                    HyperCategory(hid=51),
                    HyperCategory(hid=52),
                ],
            ),
        ]

        cls.index.navtree = [
            NavCategory(
                nid=91,
                hid=1,
                name='A',
                children=[
                    NavCategory(nid=911, hid=11),
                    NavCategory(nid=912, hid=12),
                ],
            ),
            NavCategory(
                nid=92,
                hid=2,
                name='B',
                children=[
                    NavCategory(nid=921, hid=21),
                    NavCategory(nid=922, hid=22),
                ],
            ),
            NavCategory(nid=93, hid=3, name='C', children=[NavCategory(nid=931, hid=31), NavCategory(nid=932, hid=32)]),
            NavCategory(nid=94, hid=4, name='D', children=[NavCategory(nid=941, hid=41), NavCategory(nid=942, hid=42)]),
            NavCategory(nid=54, hid=4, name='E', children=[NavCategory(nid=951, hid=51), NavCategory(nid=952, hid=52)]),
        ]

        cls.index.navtree_blue = [
            NavCategory(
                nid=91,
                hid=1,
                name='A',
                children=[
                    NavCategory(nid=911, hid=11),
                    NavCategory(nid=912, hid=12),
                ],
            ),
            NavCategory(
                nid=92,
                hid=2,
                name='B',
                children=[
                    NavCategory(nid=921, hid=21),
                    NavCategory(nid=922, hid=22),
                ],
            ),
            NavCategory(nid=93, hid=3, name='C', children=[NavCategory(nid=931, hid=31), NavCategory(nid=932, hid=32)]),
            NavCategory(nid=94, hid=4, name='D', children=[NavCategory(nid=941, hid=41), NavCategory(nid=942, hid=42)]),
            NavCategory(nid=54, hid=4, name='E', children=[NavCategory(nid=951, hid=51), NavCategory(nid=952, hid=52)]),
        ]

        cls.index.models += [
            Model(hyperid=51, hid=1),
            Model(hyperid=52, hid=2),
            Model(hyperid=53, hid=3),
            Model(hyperid=511, hid=11),
            Model(hyperid=512, hid=12),
            Model(hyperid=521, hid=21),
            Model(hyperid=522, hid=22),
            Model(hyperid=531, hid=31),
            Model(hyperid=532, hid=32),
            Model(hyperid=541, hid=41),
            Model(hyperid=542, hid=42),
            Model(hyperid=551, hid=51),
            Model(hyperid=552, hid=52),
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
                cpa=Shop.CPA_REAL,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
            ),
            Shop(
                fesh=3,
                datafeed_id=3,
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                supplier_type=Shop.FIRST_PARTY,
                blue='REAL',
                cpa=Shop.CPA_REAL,
                warehouse_id=100,
            ),
        ]

        def blue_offer(shop_sku, supplier=3, is_fulfillment=True):
            return BlueOffer(feedid=supplier, offerid=shop_sku, is_fulfillment=is_fulfillment)

        cls.index.mskus += [
            MarketSku(
                sku=211,
                hyperid=511,
                blue_offers=[blue_offer("711")],
                delivery_buckets=[
                    311,
                ],
                pickup_buckets=[
                    411,
                ],
            ),
            MarketSku(
                sku=212,
                hyperid=512,
                blue_offers=[blue_offer("712")],
                delivery_buckets=[
                    311,
                ],
                pickup_buckets=[
                    411,
                ],
            ),
            MarketSku(
                sku=221,
                hyperid=521,
                blue_offers=[blue_offer("721")],
                delivery_buckets=[
                    311,
                ],
                pickup_buckets=[
                    411,
                ],
            ),
            MarketSku(
                sku=222,
                hyperid=522,
                blue_offers=[blue_offer("722")],
                delivery_buckets=[
                    311,
                ],
                pickup_buckets=[
                    411,
                ],
            ),
            MarketSku(
                sku=231,
                hyperid=531,
                blue_offers=[blue_offer("731")],
                delivery_buckets=[
                    311,
                ],
                pickup_buckets=[
                    411,
                ],
            ),
            MarketSku(
                sku=232,
                hyperid=532,
                blue_offers=[blue_offer("732")],
                delivery_buckets=[
                    311,
                ],
                pickup_buckets=[
                    411,
                ],
            ),
            MarketSku(
                sku=241,
                hyperid=541,
                blue_offers=[blue_offer("741")],
                delivery_buckets=[
                    311,
                ],
                pickup_buckets=[
                    411,
                ],
            ),
            MarketSku(
                sku=242,
                hyperid=542,
                blue_offers=[blue_offer("742")],
                delivery_buckets=[
                    312,
                ],
                pickup_buckets=[
                    411,
                ],
            ),
            MarketSku(
                sku=251,
                hyperid=551,
                blue_offers=[blue_offer("751")],
                delivery_buckets=[
                    311,
                ],
                pickup_buckets=[
                    411,
                ],
            ),
            MarketSku(
                sku=252,
                hyperid=552,
                blue_offers=[blue_offer("752")],
                delivery_buckets=[
                    311,
                ],
                pickup_buckets=[
                    411,
                ],
            ),
        ]

        cls.index.regiontree += [
            Region(
                rid=81,
                region_type=Region.FEDERAL_DISTRICT,
                children=[
                    Region(rid=811, region_type=Region.CITY),
                    Region(rid=812, region_type=Region.CITY),
                    Region(rid=813, region_type=Region.VILLAGE),
                    Region(rid=814, region_type=Region.CITY),
                    Region(rid=815, region_type=Region.CITY),
                    Region(rid=816, region_type=Region.VILLAGE),
                ],
            ),
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=311,
                fesh=1,
                carriers=[
                    201,
                ],
                regional_options=[
                    RegionalDelivery(
                        rid=811 + offset,
                        options=[DeliveryOption(price=5, day_from=1, day_to=5, shop_delivery_price=10)],
                    )
                    for offset in range(0, 3)
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
            DeliveryBucket(
                bucket_id=312,
                fesh=1,
                carriers=[
                    201,
                ],
                regional_options=[
                    RegionalDelivery(
                        rid=811 + offset,
                        options=[DeliveryOption(price=5, day_from=1, day_to=5, shop_delivery_price=10)],
                    )
                    for offset in range(1, 3)
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
        ]

        cls.index.outlets += [
            Outlet(
                point_id=71 + offset,
                delivery_service_id=201,
                region=814 + offset,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(shipper_id=1, day_from=1, day_to=1, order_before=2, price=100),
                working_days=[i for i in range(10)],
            )
            for offset in range(0, 3)
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=411,
                fesh=1,
                carriers=[201],
                options=[
                    PickupOption(outlet_id=71, day_from=1, day_to=2, price=5),
                    PickupOption(outlet_id=72, day_from=1, day_to=2, price=5),
                    PickupOption(outlet_id=73, day_from=1, day_to=2, price=5),
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
        ]

        cls.index.prohibited_blue_offers = [
            ProhibitedBlueOffers(region_id=811, categories=[1]),
            ProhibitedBlueOffers(region_id=814, categories=[1]),
            ProhibitedBlueOffers(region_id=811, categories=[21]),
            ProhibitedBlueOffers(region_id=814, categories=[21]),
            ProhibitedBlueOffers(region_id=815, categories=[21]),
            ProhibitedBlueOffers(region_id=812, categories=[22]),
            ProhibitedBlueOffers(region_id=814, categories=[22]),
            ProhibitedBlueOffers(region_id=816, categories=[22]),
            ProhibitedBlueOffers(region_id=811, categories=[31]),
            ProhibitedBlueOffers(region_id=815, categories=[31]),
            ProhibitedBlueOffers(region_id=812, categories=[32]),
            ProhibitedBlueOffers(region_id=814, categories=[32]),
            ProhibitedBlueOffers(region_id=816, categories=[32]),
            ProhibitedBlueOffers(region_id=811, categories=[41]),
            ProhibitedBlueOffers(region_id=811, categories=[51]),
        ]

    @classmethod
    def prepare_lms(cls):
        cls.settings.lms_autogenerate = False
        cls.dynamic.lms += [
            DynamicWarehouseInfo(id=145, home_region=213),
            DynamicWarehouseToWarehouseInfo(warehouse_from=145, warehouse_to=145),
            DynamicWarehouseInfo(id=100, home_region=81, holidays_days_set_key=4),
            DynamicWarehouseToWarehouseInfo(warehouse_from=100, warehouse_to=100),
            DynamicWarehousesPriorityInRegion(
                region=Const.ROOT_RID,
                warehouses=[
                    100,
                ],
            ),
            cls.get_warehouse_and_delivery_service(100, 201),
            DynamicDeliveryServiceInfo(201, "c_201"),
        ]
        cls.index.lms = copy.deepcopy(cls.dynamic.lms)

    @staticmethod
    def get_warehouse_and_delivery_service(warehouse_id, service_id):
        date_switch_hours = [
            DateSwitchTimeAndRegionInfo(date_switch_hour=1, region_to=81),
        ]
        return DynamicWarehouseAndDeliveryServiceInfo(
            warehouse_id=warehouse_id,
            delivery_service_id=service_id,
            operation_time=0,
            date_switch_time_infos=date_switch_hours,
            shipment_holidays_days_set_key=6,
        )

    def test_nids_info_descended_restriction(self):
        '''
        Проверяем, что условия родительских нидов передаются дочерним в явном виде
        '''
        response = self.report.request_nids_info_pb('place=nids_info&rgb=blue&bsformat=7')
        expected_buckets = [
            NidsInfoBucket(regions=[811, 812, 813], restriction=BucketRestriction(prohibited_regions=[811, 814])),
            NidsInfoBucket(regions=[814, 815, 816], restriction=BucketRestriction(prohibited_regions=[811, 814])),
        ]
        self.assertFragmentIn(
            response,
            [
                NidsInfo(nid=911, buckets=expected_buckets),
                NidsInfo(nid=912, buckets=expected_buckets),
                NidsInfo(nid=91, buckets=expected_buckets),
            ],
            allow_different_len=True,
        )

    def test_nids_info_ascended_restriction(self):
        '''
        Проверяем, что условия из дочерних навигационных категорий передаются в родительскую категорию
        при условии, что ДЛЯ ОДНОГО И ТОГО ЖЕ БАКЕТА в дочерних категориях было пересечение по условиям
        '''
        response = self.report.request_nids_info_pb('place=nids_info&rgb=blue&bsformat=7')
        self.assertFragmentIn(
            response,
            [
                NidsInfo(
                    nid=921,
                    buckets=[
                        NidsInfoBucket(
                            regions=[811, 812, 813], restriction=BucketRestriction(prohibited_regions=[811, 814, 815])
                        ),
                        NidsInfoBucket(
                            regions=[814, 815, 816], restriction=BucketRestriction(prohibited_regions=[811, 814, 815])
                        ),
                    ],
                ),
                NidsInfo(
                    nid=922,
                    buckets=[
                        NidsInfoBucket(
                            regions=[811, 812, 813], restriction=BucketRestriction(prohibited_regions=[812, 814, 816])
                        ),
                        NidsInfoBucket(
                            regions=[814, 815, 816], restriction=BucketRestriction(prohibited_regions=[812, 814, 816])
                        ),
                    ],
                ),
                NidsInfo(
                    nid=92,
                    buckets=[
                        NidsInfoBucket(
                            regions=[811, 812, 813],
                            restriction=BucketRestriction(
                                prohibited_regions=[
                                    814,
                                ]
                            ),
                        ),
                        NidsInfoBucket(
                            regions=[814, 815, 816],
                            restriction=BucketRestriction(
                                prohibited_regions=[
                                    814,
                                ]
                            ),
                        ),
                    ],
                ),
            ],
            allow_different_len=True,
        )

    def test_nids_info_ascended_restriction_empty(self):
        '''
        Проверяем, что условия из дочерних навигационных категорий передаются в родительскую категорию
        при условии, что ДЛЯ ОДНОГО И ТОГО ЖЕ БАКЕТА в дочерних категориях было пересечение по условиям
        (для пустого случая)
        '''
        response = self.report.request_nids_info_pb('place=nids_info&rgb=blue&bsformat=7')
        self.assertFragmentIn(
            response,
            [
                NidsInfo(
                    nid=931,
                    buckets=[
                        NidsInfoBucket(
                            regions=[811, 812, 813],
                            restriction=BucketRestriction(
                                prohibited_regions=[
                                    811,
                                    815,
                                ]
                            ),
                        ),
                        NidsInfoBucket(
                            regions=[814, 815, 816],
                            restriction=BucketRestriction(
                                prohibited_regions=[
                                    811,
                                    815,
                                ]
                            ),
                        ),
                    ],
                ),
                NidsInfo(
                    nid=932,
                    buckets=[
                        NidsInfoBucket(
                            regions=[811, 812, 813], restriction=BucketRestriction(prohibited_regions=[812, 814, 816])
                        ),
                        NidsInfoBucket(
                            regions=[814, 815, 816],
                            restriction=BucketRestriction(
                                prohibited_regions=[
                                    812,
                                    814,
                                    816,
                                ]
                            ),
                        ),
                    ],
                ),
                NidsInfo(
                    nid=93,
                    buckets=[
                        NidsInfoBucket(
                            regions=[811, 812, 813],
                        ),
                        NidsInfoBucket(
                            regions=[814, 815, 816],
                        ),
                    ],
                ),
            ],
            allow_different_len=True,
        )

    def test_one_empty_one_forbidden(self):
        '''
        Тест на котором может быть потеряно условие. Т.к. в
        одной дочерней категории регион в категории запрещается явно,
        а в другой - просто нет бакетов доставляющих в данную категорию.
        '''
        response = self.report.request_nids_info_pb('place=nids_info&rgb=blue&bsformat=7')
        self.assertFragmentIn(
            response,
            [
                NidsInfo(
                    nid=941,
                    buckets=[
                        NidsInfoBucket(
                            regions=[811, 812, 813],
                            restriction=BucketRestriction(
                                prohibited_regions=[
                                    811,
                                ]
                            ),
                        ),
                        NidsInfoBucket(
                            regions=[814, 815, 816],
                            restriction=BucketRestriction(
                                prohibited_regions=[
                                    811,
                                ]
                            ),
                        ),
                    ],
                ),
                NidsInfo(
                    nid=942,
                    buckets=[
                        NidsInfoBucket(
                            regions=[812, 813],
                        ),
                        NidsInfoBucket(
                            regions=[814, 815, 816],
                        ),
                    ],
                ),
                NidsInfo(
                    nid=94,
                    buckets=[
                        NidsInfoBucket(
                            regions=[811, 812, 813],
                            restriction=BucketRestriction(
                                prohibited_regions=[
                                    811,
                                ],
                            ),
                        ),
                        NidsInfoBucket(
                            regions=[814, 815, 816],
                        ),
                        NidsInfoBucket(
                            regions=[
                                812,
                                813,
                            ]
                        ),
                    ],
                ),
            ],
            allow_different_len=True,
        )

    @classmethod
    def prepare_nids_recipes(cls):
        cls.index.hypertree += [
            HyperCategory(hid=6),
            HyperCategory(
                hid=7,
                children=[
                    HyperCategory(hid=77),
                ],
            ),
            HyperCategory(hid=8),
            HyperCategory(hid=9),
        ]

        cls.index.gltypes += [
            GLType(param_id=701, hid=6, gltype=GLType.ENUM, hidden=False, values=[15]),
            GLType(param_id=702, hid=7, gltype=GLType.NUMERIC, hidden=False),
            GLType(param_id=703, hid=8, gltype=GLType.ENUM, hidden=False, values=[10, 20]),
            GLType(param_id=704, hid=9, gltype=GLType.ENUM, hidden=False, values=[30]),
        ]

        cls.index.navtree += [
            NavCategory(
                nid=96,
                hid=6,
                recipe=NavRecipe(
                    filters=[
                        NavRecipeFilter(filter_type=NavRecipeFilter.ENUM, param_id=701, enum_values=[15]),
                    ]
                ),
            ),
            NavCategory(
                nid=97,
                hid=7,
                recipe=NavRecipe(
                    filters=[
                        NavRecipeFilter(filter_type=NavRecipeFilter.NUMBER, param_id=702, min_value=5, max_value=7),
                    ]
                ),
            ),
            NavCategory(nid=977, hid=77),
            NavCategory(
                nid=98,
                hid=8,
                name='Karandashi',
                children=[
                    NavCategory(
                        nid=981,
                        hid=8,
                        name='Sinie',
                        recipe=NavRecipe(
                            filters=[
                                NavRecipeFilter(filter_type=NavRecipeFilter.ENUM, param_id=703, enum_values=[10]),
                            ]
                        ),
                    ),
                    NavCategory(
                        nid=982,
                        hid=8,
                        name='Krasnie',
                        recipe=NavRecipe(
                            filters=[
                                NavRecipeFilter(filter_type=NavRecipeFilter.ENUM, param_id=703, enum_values=[20]),
                            ]
                        ),
                    ),
                ],
            ),
            NavCategory(
                nid=99,
                hid=9,
                recipe=NavRecipe(
                    filters=[
                        NavRecipeFilter(filter_type=NavRecipeFilter.ENUM, param_id=704, enum_values=[30]),
                    ]
                ),
            ),
        ]

        cls.index.navtree_blue += [
            NavCategory(
                nid=96,
                hid=6,
                recipe=NavRecipe(
                    filters=[
                        NavRecipeFilter(filter_type=NavRecipeFilter.ENUM, param_id=701, enum_values=[15]),
                    ]
                ),
            ),
            NavCategory(
                nid=97,
                hid=7,
                recipe=NavRecipe(
                    filters=[
                        NavRecipeFilter(filter_type=NavRecipeFilter.NUMBER, param_id=702, min_value=5, max_value=7),
                    ]
                ),
            ),
            NavCategory(nid=977, hid=77),
            NavCategory(
                nid=98,
                hid=8,
                name='Karandashi',
                children=[
                    NavCategory(
                        nid=981,
                        hid=8,
                        name='Sinie',
                        recipe=NavRecipe(
                            filters=[
                                NavRecipeFilter(filter_type=NavRecipeFilter.ENUM, param_id=703, enum_values=[10]),
                            ]
                        ),
                    ),
                    NavCategory(
                        nid=982,
                        hid=8,
                        name='Krasnie',
                        recipe=NavRecipe(
                            filters=[
                                NavRecipeFilter(filter_type=NavRecipeFilter.ENUM, param_id=703, enum_values=[20]),
                            ]
                        ),
                    ),
                ],
            ),
            NavCategory(
                nid=99,
                hid=9,
                recipe=NavRecipe(
                    filters=[
                        NavRecipeFilter(filter_type=NavRecipeFilter.ENUM, param_id=704, enum_values=[30]),
                    ]
                ),
            ),
        ]

        cls.index.models += [
            Model(hyperid=56, hid=6),
            Model(hyperid=57, hid=77),
            Model(hyperid=588, hid=8),
            Model(hyperid=589, hid=8),
            Model(hyperid=59, hid=9),
        ]

        cls.index.mskus += [
            MarketSku(
                sku=256,
                hyperid=56,
                blue_offers=[
                    BlueOffer(
                        feedid=3,
                        offerid="756",
                        is_fulfillment=True,
                        glparams=[GLParam(param_id=701, value=15)],
                    )
                ],
                delivery_buckets=[
                    311,
                ],
                pickup_buckets=[
                    411,
                ],
            ),
            MarketSku(
                sku=257,
                hyperid=57,
                blue_offers=[
                    BlueOffer(
                        feedid=3,
                        offerid="757",
                        is_fulfillment=True,
                        glparams=[GLParam(param_id=702, value=6)],
                    )
                ],
                delivery_buckets=[
                    311,
                ],
                pickup_buckets=[
                    411,
                ],
            ),
            MarketSku(
                sku=2588,
                hyperid=588,
                blue_offers=[
                    BlueOffer(
                        feedid=3,
                        offerid="7588",
                        is_fulfillment=True,
                        glparams=[GLParam(param_id=703, value=10)],
                    )
                ],
                delivery_buckets=[
                    311,
                ],
                pickup_buckets=[
                    411,
                ],
            ),
            MarketSku(
                sku=2589,
                hyperid=589,
                blue_offers=[
                    BlueOffer(
                        feedid=3,
                        offerid="7589",
                        is_fulfillment=True,
                        glparams=[GLParam(param_id=703, value=20)],
                    )
                ],
                delivery_buckets=[
                    311,
                ],
                pickup_buckets=[
                    411,
                ],
            ),
            MarketSku(
                sku=259,
                hyperid=59,
                blue_offers=[
                    BlueOffer(
                        feedid=3,
                        offerid="759",
                        is_fulfillment=True,
                        glparams=[GLParam(param_id=704, value=30)],
                    )
                ],
                delivery_buckets=[
                    311,
                ],
                pickup_buckets=[
                    411,
                ],
            ),
        ]

        cls.index.prohibited_blue_offers += [
            ProhibitedBlueOffers(region_id=811, categories=[9]),
        ]

    def test_nids_recipes(self):
        response = self.report.request_nids_info_pb('place=nids_info&rgb=blue&bsformat=7')
        self.assertFragmentIn(
            response,
            [
                NidsInfo(
                    nid=96,
                    buckets=[
                        NidsInfoBucket(
                            regions=[811, 812, 813],
                        ),
                        NidsInfoBucket(
                            regions=[814, 815, 816],
                        ),
                    ],
                ),
                NidsInfo(
                    nid=977,
                    buckets=[
                        NidsInfoBucket(
                            regions=[811, 812, 813],
                        ),
                        NidsInfoBucket(
                            regions=[814, 815, 816],
                        ),
                    ],
                ),
                NidsInfo(
                    nid=981,
                    buckets=[
                        NidsInfoBucket(
                            regions=[811, 812, 813],
                        ),
                        NidsInfoBucket(
                            regions=[814, 815, 816],
                        ),
                    ],
                ),
                NidsInfo(
                    nid=982,
                    buckets=[
                        NidsInfoBucket(
                            regions=[811, 812, 813],
                        ),
                        NidsInfoBucket(
                            regions=[814, 815, 816],
                        ),
                    ],
                ),
                NidsInfo(
                    nid=99,
                    buckets=[
                        NidsInfoBucket(
                            regions=[811, 812, 813], restriction=BucketRestriction(prohibited_regions=[811])
                        ),
                        NidsInfoBucket(
                            regions=[814, 815, 816], restriction=BucketRestriction(prohibited_regions=[811])
                        ),
                    ],
                ),
            ],
            allow_different_len=True,
        )


if __name__ == '__main__':
    main()
