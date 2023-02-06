#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    Currency,
    DeliveryBucket,
    DeliveryOption,
    HyperCategory,
    LinkData,
    Model,
    NavCategory,
    Region,
    RegionalDelivery,
    Shop,
    DynamicWarehouseInfo,
    DynamicWarehouseToWarehouseInfo,
)
from core.testcase import TestCase, main
from core.matcher import NidsInfoBucket, NidsInfo
from core.types.sku import MarketSku, BlueOffer
from core.types.taxes import Tax


BUCKET_300_REGIONS = [
    301,
    302,
    307,
]  # Были взяты только подрегионы типов: город и деревня (даже если город был подрегионом)
BUCKET_301_REGIONS = [501, 502, 507]
BUCKET_302_REGIONS = [601, 602, 607]


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.hypertree = [
            HyperCategory(hid=11),
            HyperCategory(hid=12),
            HyperCategory(hid=13),
        ]

        cls.index.navtree += [
            NavCategory(nid=101, hid=11, name='Leaf Exists'),  # hid этого узла имеет оферы
            NavCategory(
                nid=102,
                hid=0,
                link=LinkData(target="catalog", params={"nid": 101}),
                children=[
                    NavCategory(nid=1021, hid=12),  # 333
                ],
            ),  # ссылка на узел имеющий офферы
            NavCategory(
                nid=103,
                hid=0,
                link=LinkData(target="catalog", params={"nid": 102}),
                children=[
                    NavCategory(nid=1031, hid=13),  # 334
                ],
            ),  # ссылка на ссылку
            NavCategory(
                nid=104,
                hid=0,
                link=LinkData(target="catalog", params={"nid": 106}),
                children=[
                    NavCategory(nid=1041, hid=11),
                ],
            ),  # цикл по ссылкам (последняя связь разрушается), в ответе не проверяется, так как результат
            # зависит от порядка обхода дерева
            # в дереве присутствует, чтобы проверить, что подобный цикл не уронит запро
            NavCategory(
                nid=105,
                hid=0,
                link=LinkData(target="catalog", params={"nid": 104}),
                children=[
                    NavCategory(nid=1051, hid=12),
                ],
            ),
            NavCategory(
                nid=106,
                hid=0,
                link=LinkData(target="catalog", params={"nid": 105}),
                children=[
                    NavCategory(nid=1061, hid=13),
                ],
            ),
            NavCategory(
                nid=107,
                hid=0,
                link=LinkData(target="catalog", params={"nid": 108}),
                children=[
                    NavCategory(nid=1071, hid=11),
                ],
            ),  # цикл путь по ссылкам для этой вершины содержит цикл, однако, она должна быть обработана корректно
            # вершины самого цикла не проверяются
            NavCategory(
                nid=108,
                hid=0,
                link=LinkData(target="catalog", params={"nid": 109}),
                children=[
                    NavCategory(nid=1081, hid=12),
                ],
            ),
            NavCategory(
                nid=109,
                hid=0,
                link=LinkData(target="catalog", params={"nid": 108}),
                children=[
                    NavCategory(nid=1091, hid=13),
                ],
            ),
        ]

        cls.index.navtree += [
            NavCategory(nid=111, hid=11, name='Leaf Exists'),  # hid этого узла имеет оферы
            NavCategory(
                nid=112,
                hid=0,
                link=LinkData(target="catalog", params={"nid": 101}),
                children=[
                    NavCategory(nid=1121, hid=12),  # 333
                ],
            ),  # ссылка на узел имеющий офферы
        ]

        cls.index.navtree_blue += [
            NavCategory(nid=101, hid=11, name='Leaf Exists'),  # hid этого узла имеет оферы
            NavCategory(
                nid=102,
                hid=0,
                link=LinkData(target="catalog", params={"nid": 101}),
                children=[
                    NavCategory(nid=1021, hid=12),  # 333
                ],
            ),  # ссылка на узел имеющий офферы
            NavCategory(
                nid=103,
                hid=0,
                link=LinkData(target="catalog", params={"nid": 102}),
                children=[
                    NavCategory(nid=1031, hid=13),  # 334
                ],
            ),  # ссылка на ссылку
            NavCategory(
                nid=104,
                hid=0,
                link=LinkData(target="catalog", params={"nid": 106}),
                children=[
                    NavCategory(nid=1041, hid=11),
                ],
            ),  # цикл по ссылкам (последняя связь разрушается), в ответе не проверяется, так как результат
            # зависит от порядка обхода дерева
            # в дереве присутствует, чтобы проверить, что подобный цикл не уронит запро
            NavCategory(
                nid=105,
                hid=0,
                link=LinkData(target="catalog", params={"nid": 104}),
                children=[
                    NavCategory(nid=1051, hid=12),
                ],
            ),
            NavCategory(
                nid=106,
                hid=0,
                link=LinkData(target="catalog", params={"nid": 105}),
                children=[
                    NavCategory(nid=1061, hid=13),
                ],
            ),
            NavCategory(
                nid=107,
                hid=0,
                link=LinkData(target="catalog", params={"nid": 108}),
                children=[
                    NavCategory(nid=1071, hid=11),
                ],
            ),  # цикл путь по ссылкам для этой вершины содержит цикл, однако, она должна быть обработана корректно
            # вершины самого цикла не проверяются
            NavCategory(
                nid=108,
                hid=0,
                link=LinkData(target="catalog", params={"nid": 109}),
                children=[
                    NavCategory(nid=1081, hid=12),
                ],
            ),
            NavCategory(
                nid=109,
                hid=0,
                link=LinkData(target="catalog", params={"nid": 108}),
                children=[
                    NavCategory(nid=1091, hid=13),
                ],
            ),
        ]

        cls.index.navtree_blue += [
            NavCategory(nid=111, hid=11, name='Leaf Exists'),  # hid этого узла имеет оферы
            NavCategory(
                nid=112,
                hid=0,
                link=LinkData(target="catalog", params={"nid": 101}),
                children=[
                    NavCategory(nid=1121, hid=12),  # 333
                ],
            ),  # ссылка на узел имеющий офферы
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

        cls.dynamic.lms += [
            DynamicWarehouseInfo(id=145, home_region=213),
            DynamicWarehouseToWarehouseInfo(warehouse_from=145, warehouse_to=145),
            DynamicWarehouseInfo(id=100, home_region=213),
            DynamicWarehouseToWarehouseInfo(warehouse_from=100, warehouse_to=100),
        ]

        cls.index.models += [
            Model(hyperid=81, hid=11),
            Model(hyperid=82, hid=12),
            Model(hyperid=83, hid=13),
        ]

        def blue_offer(shop_sku, supplier=3, is_fulfillment=True):
            return BlueOffer(feedid=supplier, offerid=shop_sku, is_fulfillment=is_fulfillment)

        cls.index.mskus += [
            MarketSku(
                sku=101,
                hyperid=81,
                blue_offers=[blue_offer("811")],
                delivery_buckets=[
                    300,
                ],
            ),
            MarketSku(
                sku=102,
                hyperid=82,
                blue_offers=[blue_offer("822")],
                delivery_buckets=[
                    301,
                ],
            ),
            MarketSku(
                sku=103,
                hyperid=83,
                blue_offers=[blue_offer("833")],
                delivery_buckets=[
                    302,
                ],
            ),
        ]

    @classmethod
    def prepare_courier_regions(cls):
        # Регионы курьерской доставки. В результат попадут только CITY и VILLAGE
        cls.index.regiontree += [
            Region(
                rid=300,
                region_type=Region.FEDERAL_DISTRICT,
                children=[
                    Region(
                        rid=301,
                        region_type=Region.CITY,
                        children=[
                            Region(
                                rid=302,
                                region_type=Region.CITY,
                                children=[  # Если будет такой случай, что город является составной частью города, то его тоже покажут
                                    Region(rid=303, region_type=Region.CITY_DISTRICT),
                                    Region(rid=304, region_type=Region.OVERSEAS),
                                    Region(rid=305, region_type=Region.METRO_STATION),
                                ],
                            ),
                        ],
                    ),
                    Region(rid=306, region_type=Region.SETTLEMENT),
                    Region(rid=307, region_type=Region.VILLAGE),
                    Region(rid=308, region_type=Region.SECONDARY_DISTRICT),
                ],
            ),
            Region(
                rid=500,
                region_type=Region.FEDERAL_DISTRICT,
                children=[
                    Region(
                        rid=501,
                        region_type=Region.CITY,
                        children=[
                            Region(
                                rid=502,
                                region_type=Region.CITY,
                                children=[  # Если будет такой случай, что город является составной частью города, то его тоже покажут
                                    Region(rid=503, region_type=Region.CITY_DISTRICT),
                                    Region(rid=504, region_type=Region.OVERSEAS),
                                    Region(rid=505, region_type=Region.METRO_STATION),
                                ],
                            ),
                        ],
                    ),
                    Region(rid=506, region_type=Region.SETTLEMENT),
                    Region(rid=507, region_type=Region.VILLAGE),
                    Region(rid=508, region_type=Region.SECONDARY_DISTRICT),
                ],
            ),
            Region(
                rid=600,
                region_type=Region.FEDERAL_DISTRICT,
                children=[
                    Region(
                        rid=601,
                        region_type=Region.CITY,
                        children=[
                            Region(
                                rid=602,
                                region_type=Region.CITY,
                                children=[  # Если будет такой случай, что город является составной частью города, то его тоже покажут
                                    Region(rid=603, region_type=Region.CITY_DISTRICT),
                                    Region(rid=604, region_type=Region.OVERSEAS),
                                    Region(rid=605, region_type=Region.METRO_STATION),
                                ],
                            ),
                        ],
                    ),
                    Region(rid=606, region_type=Region.SETTLEMENT),
                    Region(rid=607, region_type=Region.VILLAGE),
                    Region(rid=608, region_type=Region.SECONDARY_DISTRICT),
                ],
            ),
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=300,
                dc_bucket_id=300,
                fesh=1,
                carriers=[201],
                regional_options=[
                    RegionalDelivery(
                        rid=300, options=[DeliveryOption(price=5, day_from=1, day_to=5, shop_delivery_price=10)]
                    ),
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
            DeliveryBucket(
                bucket_id=301,
                dc_bucket_id=301,
                fesh=1,
                carriers=[201],
                regional_options=[
                    RegionalDelivery(
                        rid=500, options=[DeliveryOption(price=5, day_from=1, day_to=5, shop_delivery_price=10)]
                    ),
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
            DeliveryBucket(
                bucket_id=302,
                dc_bucket_id=302,
                fesh=1,
                carriers=[201],
                regional_options=[
                    RegionalDelivery(
                        rid=600, options=[DeliveryOption(price=5, day_from=1, day_to=5, shop_delivery_price=10)]
                    ),
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
        ]

    def test_links_disabled(self):
        """
        Проверяем, что ссылки не учитываются в nids_info если они выключены флагом
        """
        response = self.report.request_nids_info_pb(
            'place=nids_info&rgb=blue&bsformat=7&rearr-factors=market_enable_navigation_links=0'
        )
        self.error_log.ignore(code=9332)
        self.base_logs_storage.error_log.ignore(code=9332)
        self.assertFragmentIn(
            response,
            [
                NidsInfo(
                    nid=101,
                    buckets=[
                        NidsInfoBucket(regions=BUCKET_300_REGIONS),
                    ],
                ),
                NidsInfo(
                    nid=102,
                    buckets=[
                        NidsInfoBucket(regions=BUCKET_301_REGIONS),
                    ],
                ),
                NidsInfo(
                    nid=103,
                    buckets=[
                        NidsInfoBucket(regions=BUCKET_302_REGIONS),
                    ],
                ),  # аналогично
                NidsInfo(
                    nid=107,
                    buckets=[
                        NidsInfoBucket(regions=BUCKET_300_REGIONS),
                    ],
                ),  # собственные бакеты + бакеты из цикла
            ],
        )

    def test_links_disabled_white(self):
        """
        Проверяем, что ссылки не учитываются в nids_info если они выключены флагом
        """
        response = self.report.request_nids_info_pb(
            'place=nids_info&rgb=blue&bsformat=7&rearr-factors=market_enable_navigation_links=1'
        )
        self.error_log.ignore(code=9332)
        self.base_logs_storage.error_log.ignore(code=9332)
        self.assertFragmentIn(
            response,
            [
                NidsInfo(
                    nid=111,
                    buckets=[
                        NidsInfoBucket(regions=BUCKET_300_REGIONS),
                    ],
                ),
                NidsInfo(
                    nid=112,
                    buckets=[
                        NidsInfoBucket(regions=BUCKET_301_REGIONS),
                    ],
                ),
            ],
        )


if __name__ == '__main__':
    main()
