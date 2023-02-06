#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    Currency,
    DateSwitchTimeAndRegionInfo,
    DeliveryBucket,
    DeliveryOption,
    DynamicDeliveryServiceInfo,
    DynamicShop,
    DynamicSkuOffer,
    DynamicWarehouseAndDeliveryServiceInfo,
    DynamicWarehouseInfo,
    DynamicWarehouseToWarehouseInfo,
    DynamicWarehousesPriorityInRegion,
    HyperCategory,
    Model,
    NavCategory,
    Offer,
    Outlet,
    OutletDeliveryOption,
    PickupBucket,
    PickupOption,
    Region,
    RegionalDelivery,
    Shop,
)
from core.testcase import TestCase, main
from core.matcher import NidsInfoBucket, NidsInfo
from core.types.sku import MarketSku, BlueOffer
from core.types.taxes import Tax
from core.types.autogen import Const

import copy

BUCKET_300_REGIONS = [
    301,
    302,
    307,
]  # Были взяты только подрегионы типов: город и деревня (даже если город был подрегионом)
BUCKET_310_REGIONS = [312, 313, 314, 315]  # Был пропущен регион 311. В него доставка запрещена
BUCKET_320_REGIONS = [321, 323]  # Были взяты только родительские регионы: город и деревня
BUCKET_330_REGIONS = [331, 333]  # Были взяты только родительские регионы: город и деревня

BUCKET_350_REGIONS = [311, 312, 313, 314, 315]  # Доставка доступна во все города
BUCKET_351_REGIONS = [321, 323]
BUCKET_352_REGIONS = [331, 333]

BUCKET_DROP_SHIP_REGIONS = [341]

BUCKET_200_REGIONS = [213]  # Были взяты только родительский регион: город
BUCKET_410_REGIONS = [411]  # Были взяты только родительский регион: город
BUCKET_500_REGIONS = [
    501,
    502,
    507,
]  # Были взяты только подрегионы типов: город и деревня (даже, eсли город был подрегионом)
BUCKET_600_REGIONS = [
    601,
    602,
    607,
]  # Были взяты только подрегионы типов: город и деревня (даже, eсли город был подрегионом)
BUCKET_899_REGIONS = [899]  # Региональная доставка для 7 магазина
BUCKET_999_REGIONS = [999]  # Региональная доставка для 8 магазина


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.hypertree = [
            HyperCategory(hid=1),
            HyperCategory(hid=2),
            HyperCategory(hid=3),
            HyperCategory(hid=4),
            HyperCategory(hid=51),
            HyperCategory(hid=61),
            HyperCategory(
                hid=7,
                children=[
                    HyperCategory(hid=71),
                ],
            ),
            HyperCategory(hid=8),
            HyperCategory(hid=9),
        ]

        cls.index.navtree_blue = [
            NavCategory(nid=1, hid=1, name='Leaf Exists'),  # hid этого узла имеет оферы
            NavCategory(nid=2, hid=2, name='Leaf without offer'),  # hid этого узла не имеет оферов
            NavCategory(nid=3, hid=3, name='Leaf offer no has_gone'),  # hid этого узла имеет офер, скрытый has_gone
            NavCategory(
                nid=4, hid=4, name='Leaf offer hidden by dynamic'
            ),  # hid этого узла имеет офер, скрытый динамиком
            NavCategory(
                nid=5,
                hid=0,
                name='Virtual',
                children=[  # Виртуальный узел, имеющий узел в продаже
                    NavCategory(nid=51, hid=51),
                    NavCategory(nid=52, hid=52),
                ],
            ),
            NavCategory(
                nid=6,
                hid=0,
                name='Virtual without offers',
                children=[  # Виртуальный узел, не имеющий узла в продаже
                    NavCategory(nid=61, hid=61),
                ],
            ),
            NavCategory(nid=7, hid=7, name='Sub hid', children=[NavCategory(nid=71, hid=71)]),  # это не листовой hid
            NavCategory(nid=8, hid=8, name='Without delivery'),  # Для офера этого узла доставка запрещена через LMS
            NavCategory(nid=9, hid=9, name='DropShip'),  # Доставка из магазина
        ]

        cls.index.navtree_blue += [
            NavCategory(nid=101, hid=1, name='Leaf Exists'),  # hid этого узла имеет оферы
        ]

        # White Tree
        cls.index.navtree = [
            NavCategory(nid=10001, hid=1, name='Leaf Exists'),  # hid этого узла имеет оферы
            NavCategory(nid=10002, hid=2, name='Leaf without offer'),  # hid этого узла не имеет оферов
            NavCategory(nid=10003, hid=3, name='Leaf offer no has_gone'),  # hid этого узла имеет офер, скрытый has_gone
            NavCategory(
                nid=10004, hid=4, name='Leaf offer hidden by dynamic'
            ),  # hid этого узла имеет офер, скрытый динамиком
            NavCategory(
                nid=10005,
                hid=0,
                name='Virtual',
                children=[  # Виртуальный узел, имеющий узел в продаже
                    NavCategory(nid=10051, hid=51),
                    NavCategory(nid=10052, hid=52),
                ],
            ),
            NavCategory(
                nid=10006,
                hid=0,
                name='Virtual without offers',
                children=[  # Виртуальный узел, не имеющий узла в продаже
                    NavCategory(nid=10061, hid=61),
                ],
            ),
            NavCategory(
                nid=10007, hid=7, name='Sub hid', children=[NavCategory(nid=10071, hid=71)]  # это не листовой hid
            ),
            NavCategory(nid=10008, hid=8, name='Without delivery'),  # Для офера этого узла доставка запрещена через LMS
            NavCategory(nid=10009, hid=9, name='DropShip'),  # Доставка из магазина
            NavCategory(nid=10101, hid=1, name='Leaf Exists'),  # hid этого узла имеет оферы
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
            Shop(
                fesh=4,
                datafeed_id=4,
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                supplier_type=Shop.FIRST_PARTY,
                blue='REAL',
                cpa=Shop.CPA_REAL,
                warehouse_id=101,
            ),
            Shop(
                fesh=5,
                datafeed_id=5,
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                supplier_type=Shop.FIRST_PARTY,
                blue='REAL',
                cpa=Shop.CPA_REAL,
                warehouse_id=102,  # Связки магазина с этим складом нет. Бакеты доставки этих оферов не будут добавлены
            ),
            Shop(
                fesh=6,
                datafeed_id=6,
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                supplier_type=Shop.FIRST_PARTY,
                blue='REAL',
                cpa=Shop.CPA_REAL,
                fulfillment_program=False,
                ignore_stocks=True,
                warehouse_id=103,
            ),
            Shop(
                fesh=7,
                datafeed_id=7,
                priority_region=213,
                regions=[213],
                name='dsbs_shop_1',
                cpa=Shop.CPA_REAL,
            ),
        ]

        cls.index.models += [
            Model(hyperid=1, hid=1),
            Model(hyperid=2, hid=2),
            Model(hyperid=3, hid=3),
            Model(hyperid=4, hid=4),
            Model(hyperid=51, hid=51),
            Model(hyperid=52, hid=52),
            Model(hyperid=61, hid=61),
            Model(hyperid=71, hid=71),
            Model(hyperid=8, hid=8),
            Model(hyperid=9, hid=9),
        ]

        def blue_offer(shop_sku, supplier=3, is_fulfillment=True):
            return BlueOffer(feedid=supplier, offerid=shop_sku, is_fulfillment=is_fulfillment)

        cls.index.mskus += [
            MarketSku(sku=1, hyperid=1, blue_offers=[blue_offer("1")], delivery_buckets=[300, 350]),
            MarketSku(sku=2, hyperid=2, blue_offers=[]),
            MarketSku(sku=3, hyperid=3, blue_offers=[blue_offer("3")], has_gone=True),
            MarketSku(
                sku=4,
                hyperid=4,
                blue_offers=[blue_offer("4", supplier=4)],
                delivery_buckets=[310],
                pickup_buckets=[351],
            ),
            MarketSku(sku=51, hyperid=51, blue_offers=[blue_offer("51")], pickup_buckets=[320], post_buckets=[352]),
            MarketSku(sku=52, hyperid=52, blue_offers=[blue_offer("52")], delivery_buckets=[300]),
            MarketSku(sku=61, hyperid=61, blue_offers=[]),
            MarketSku(sku=71, hyperid=71, blue_offers=[blue_offer("71")], post_buckets=[330]),
            # Оферы с заблокированной доставкой через LMS
            MarketSku(
                sku=81, hyperid=8, blue_offers=[blue_offer("81", supplier=5)], delivery_buckets=[350]
            ),  # Нет связи магазина и склада
            MarketSku(
                sku=82,
                hyperid=8,
                blue_offers=[blue_offer("82", supplier=3)],
                delivery_buckets=[350],
                pickup_buckets=[351],
                post_buckets=[352],
            ),  # Нет связи склада и службы
            MarketSku(
                sku=9,
                hyperid=9,
                blue_offers=[blue_offer("9", supplier=6, is_fulfillment=False)],
                has_delivery_options=False,
                pickup_buckets=[500],
            ),  # dropship offer
        ]

        cls.index.offers += [
            Offer(title='white offer', hyperid=2),
            Offer(
                hyperid=2,
                fesh=7,
                hid=2,
                delivery_buckets=[300, 350],
                delivery_options=[DeliveryOption(price=350, day_from=1, day_to=7, order_before=10)],
                cpa=Offer.CPA_REAL,
                title="dsbs offer",
            ),  # pickup_buckets=[417]),
        ]

    @staticmethod
    def get_warehouse_and_delivery_service(warehouse_id, service_id):
        date_switch_hours = [
            DateSwitchTimeAndRegionInfo(date_switch_hour=1, region_to=213),
            DateSwitchTimeAndRegionInfo(date_switch_hour=1, region_to=225),
        ]
        return DynamicWarehouseAndDeliveryServiceInfo(
            warehouse_id=warehouse_id,
            delivery_service_id=service_id,
            operation_time=0,
            date_switch_time_infos=date_switch_hours,
            shipment_holidays_days_set_key=6,
        )

    @classmethod
    def prepare_lms(cls):
        cls.settings.lms_autogenerate = False
        cls.dynamic.lms += [
            DynamicWarehouseInfo(id=100, home_region=300, holidays_days_set_key=4),
            DynamicWarehouseInfo(id=101, home_region=310, holidays_days_set_key=4),
            DynamicWarehouseInfo(id=102, home_region=310, holidays_days_set_key=4),
            DynamicWarehouseInfo(id=103, home_region=341, holidays_days_set_key=4),
            DynamicWarehouseInfo(id=145, home_region=213),
            DynamicWarehouseToWarehouseInfo(warehouse_from=100, warehouse_to=100),
            DynamicWarehouseToWarehouseInfo(warehouse_from=101, warehouse_to=101),
            DynamicWarehouseToWarehouseInfo(warehouse_from=102, warehouse_to=102),
            DynamicWarehouseToWarehouseInfo(warehouse_from=103, warehouse_to=103),
            DynamicWarehouseToWarehouseInfo(warehouse_from=145, warehouse_to=145),
            DynamicWarehousesPriorityInRegion(region=Const.ROOT_RID, warehouses=[100, 101, 102, 103]),
            cls.get_warehouse_and_delivery_service(100, 201),
            cls.get_warehouse_and_delivery_service(101, 201),
            cls.get_warehouse_and_delivery_service(102, 202),
            cls.get_warehouse_and_delivery_service(103, 99),
            DynamicDeliveryServiceInfo(201, "c_201"),
            DynamicDeliveryServiceInfo(202, "c_202"),
            DynamicDeliveryServiceInfo(99, "c_99"),
        ]
        cls.index.lms = copy.deepcopy(cls.dynamic.lms)

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
        ]

    @classmethod
    def prepare_courier_regions_exclude(cls):
        # Регионы курьерской доставки, но регион 311 выколот в бакете
        cls.index.regiontree += [
            Region(
                rid=310,
                region_type=Region.FEDERAL_DISTRICT,
                children=[Region(rid=311 + rid, region_type=Region.CITY) for rid in range(5)],
            ),
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=310,
                dc_bucket_id=310,
                fesh=1,
                carriers=[201],
                regional_options=[
                    RegionalDelivery(
                        rid=310, options=[DeliveryOption(price=5, day_from=1, day_to=5, shop_delivery_price=10)]
                    ),
                    RegionalDelivery(
                        rid=311, forbidden=True
                    ),  # 311 регион запрещен в этом бакете. Доставки в него не будет
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
        ]

    @classmethod
    def prepare_pickup_regions(cls):
        # Доставка в оутлеты
        # Оутлеты добавляются в CITY_DISTRICT и SECONDARY_DISTRICT, но будут доступны только CITY и VILLAGE
        cls.index.regiontree += [
            Region(
                rid=320,
                region_type=Region.FEDERAL_DISTRICT,
                children=[
                    Region(
                        rid=321, region_type=Region.CITY, children=[Region(rid=322, region_type=Region.CITY_DISTRICT)]
                    ),
                    Region(
                        rid=323,
                        region_type=Region.VILLAGE,
                        children=[Region(rid=324, region_type=Region.SECONDARY_DISTRICT)],
                    ),
                ],
            ),
            Region(
                rid=410,
                region_type=Region.FEDERAL_DISTRICT,
                children=[
                    Region(
                        rid=411, region_type=Region.CITY, children=[Region(rid=412, region_type=Region.CITY_DISTRICT)]
                    ),
                ],
            ),
        ]

        cls.index.outlets += [
            Outlet(
                point_id=322,
                delivery_service_id=201,
                region=322,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(shipper_id=1, day_from=1, day_to=1, order_before=2, price=100),
                working_days=[i for i in range(10)],
            ),
            Outlet(
                point_id=324,
                delivery_service_id=201,
                region=324,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(shipper_id=1, day_from=1, day_to=1, order_before=2, price=100),
                working_days=[i for i in range(10)],
            ),
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=320,
                dc_bucket_id=320,
                fesh=8,
                carriers=[201],
                options=[
                    PickupOption(outlet_id=322, day_from=1, day_to=2, price=5),
                    PickupOption(outlet_id=324, day_from=1, day_to=2, price=5),
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
        ]

    @classmethod
    def prepare_post_regions(cls):
        # Доставка почтой
        # Оутлеты добавляются в CITY_DISTRICT и SECONDARY_DISTRICT, но будут доступны только CITY и VILLAGE
        cls.index.regiontree += [
            Region(
                rid=330,
                region_type=Region.FEDERAL_DISTRICT,
                children=[
                    Region(
                        rid=331, region_type=Region.CITY, children=[Region(rid=332, region_type=Region.CITY_DISTRICT)]
                    ),
                    Region(
                        rid=333,
                        region_type=Region.VILLAGE,
                        children=[Region(rid=334, region_type=Region.SECONDARY_DISTRICT)],
                    ),
                ],
            ),
            Region(
                rid=200,
                region_type=Region.FEDERAL_DISTRICT,
                children=[
                    Region(rid=213, region_type=Region.CITY),
                ],
            ),
        ]

        cls.index.outlets += [
            Outlet(
                point_id=332,
                delivery_service_id=201,
                region=332,
                point_type=Outlet.FOR_POST,
                delivery_option=OutletDeliveryOption(shipper_id=1, day_from=1, day_to=1, order_before=2, price=100),
                working_days=[i for i in range(10)],
            ),
            Outlet(
                point_id=334,
                delivery_service_id=201,
                region=334,
                point_type=Outlet.FOR_POST,
                delivery_option=OutletDeliveryOption(shipper_id=1, day_from=1, day_to=1, order_before=2, price=100),
                working_days=[i for i in range(10)],
            ),
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=330,
                dc_bucket_id=330,
                fesh=2,
                carriers=[201],
                options=[
                    PickupOption(outlet_id=332, day_from=1, day_to=2, price=5),
                    PickupOption(outlet_id=334, day_from=1, day_to=2, price=5),
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
        ]

    @classmethod
    def prepare_filtered_by_lms(cls):
        # Настройка бакетов и оферов, которые будут отфильтровываться по LMS
        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=350,
                dc_bucket_id=350,
                fesh=1,
                carriers=[202],
                regional_options=[
                    RegionalDelivery(
                        rid=310, options=[DeliveryOption(price=5, day_from=1, day_to=5, shop_delivery_price=10)]
                    ),
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
        ]

        cls.index.outlets += [
            Outlet(
                point_id=351,
                delivery_service_id=202,
                region=322,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(shipper_id=1, day_from=1, day_to=1, order_before=2, price=100),
                working_days=[i for i in range(10)],
            ),
            Outlet(
                point_id=352,
                delivery_service_id=202,
                region=324,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(shipper_id=1, day_from=1, day_to=1, order_before=2, price=100),
                working_days=[i for i in range(10)],
            ),
            Outlet(
                point_id=353,
                delivery_service_id=202,
                region=332,
                point_type=Outlet.FOR_POST,
                delivery_option=OutletDeliveryOption(shipper_id=1, day_from=1, day_to=1, order_before=2, price=100),
                working_days=[i for i in range(10)],
            ),
            Outlet(
                point_id=354,
                delivery_service_id=202,
                region=334,
                point_type=Outlet.FOR_POST,
                delivery_option=OutletDeliveryOption(shipper_id=1, day_from=1, day_to=1, order_before=2, price=100),
                working_days=[i for i in range(10)],
            ),
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=351,
                dc_bucket_id=351,
                fesh=2,
                carriers=[202],
                options=[
                    PickupOption(outlet_id=351, day_from=1, day_to=2, price=5),
                    PickupOption(outlet_id=352, day_from=1, day_to=2, price=5),
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
        ]
        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=352,
                dc_bucket_id=352,
                fesh=2,
                carriers=[202],
                options=[
                    PickupOption(outlet_id=353, day_from=1, day_to=2, price=5),
                    PickupOption(outlet_id=354, day_from=1, day_to=2, price=5),
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
        ]

    @classmethod
    def prepare_for_drop_ship(cls):
        # Регионы доставки ДропШип оферов (фарма).
        # Сейчас у этих оферов нет бакетов ПВЗ. Информация о регионах доставки берется из оутлетов магазина, прикрепленных в shops_outlets
        cls.index.regiontree += [
            Region(
                rid=340,
                region_type=Region.FEDERAL_DISTRICT,
                children=[
                    Region(
                        rid=341, region_type=Region.CITY, children=[Region(rid=342, region_type=Region.CITY_DISTRICT)]
                    ),
                    Region(
                        rid=343,
                        region_type=Region.VILLAGE,
                        children=[Region(rid=344, region_type=Region.SECONDARY_DISTRICT)],
                    ),
                ],
            ),
        ]
        cls.index.outlets += [
            Outlet(
                point_id=341,
                fesh=6,
                region=341,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(
                    day_from=3,
                    day_to=5,
                    order_before=6,
                    work_in_holiday=False,
                    price=500,
                    price_to=1000,
                    shipper_readable_id="self shipper",
                ),
                working_days=[i for i in range(10)],
            ),
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=500,
                dc_bucket_id=500,
                fesh=6,
                carriers=[99],
                options=[PickupOption(outlet_id=341, day_from=3, day_to=5, price=500)],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]

    def test_nids_info_blue(self):
        """
        Что проверяем: рассчет доступных навигационных узлов, для синего.
        Возвращаются все узлы, в которых есть офер в продаже.
        Виртуальный узел показывается, если есть хоть один офер в дочерних узлах
        """
        request = 'place=nids_info&rids=213&rgb=blue'
        response = self.report.request_json(request)

        expectedAllowedNids = [
            Const.ROOT_NID,  # Корневые узлы будут добавлены всегда (хотя бы один офер есть в наличии)
            10001,  # Есть офер в продаже
            10004,  # офер пока не скрыт динамиком
            10005,
            10051,
            10052,  # Виртуальный узел и его дочерний в продаже
            10007,  # Не листовой hid тоже отобразится
            10071,
            10008,  # Доставка этого офера выключена по LMS, но в общей логике он все-равно показывается
            10009,
            10101,  # Узел из нового синего дерева
            10002,  # nid с dsbs-оффером
        ]

        expectedEmptyNids = [
            10003,  # офер скрыт has_gone
            10006,
            10061,  # Виртуальный и его дочерний без оферов скрыт
        ]

        self.assertFragmentIn(response, {"allowedNids": expectedAllowedNids}, allow_different_len=False)
        self.assertFragmentIn(response, {"emptyNids": expectedEmptyNids}, allow_different_len=True)

    def test_sold(self):
        """
        Что проверяем: скрытие узла, если офер был продан
        """

        self.dynamic.disabled_sku_offers += [
            DynamicSkuOffer(shop_id=4, sku="4", warehouse_id=101),
        ]
        response = self.report.request_json('place=nids_info&rids=213&rgb=blue')
        self.assertFragmentIn(
            response,
            {
                "allowedNids": [
                    Const.ROOT_NID,  # Корневые узлы будут добавлены всегда (хотя бы один офер есть в наличии)
                    10001,  # Есть офер в продаже
                    10002,  # Есть белый оффер в продаже
                    10005,
                    10051,
                    10052,  # Виртуальный узел и его дочерний в продаже
                    10007,  # Не листовой hid тоже отобразится
                    10071,
                    10008,
                    10009,
                    10101,  # Узел из нового синего дерева
                ]
            },
            allow_different_len=False,
        )
        self.assertFragmentIn(
            response,
            {
                "emptyNids": [
                    10003,  # офер скрыт has_gone
                    10004,  # офер скрыт по стокам
                    10006,
                    10061,  # Виртуальный и его дочерний без оферов скрыт
                ]
            },
            allow_different_len=True,
        )

    def test_dynamic_by_supplier(self):
        """
        Что проверяем: скрытие узла, если офер был скрыт по поставщику
        """
        # Проверяем, что возвращается белое дерево
        self.dynamic.market_dynamic.disabled_blue_suppliers += [DynamicShop(4)]
        response = self.report.request_json('place=nids_info&rids=213&rgb=blue')
        self.assertFragmentIn(
            response,
            {
                "allowedNids": [
                    Const.ROOT_NID,  # Корневые узлы будут добавлены всегда (хотя бы один офер есть в наличии)
                    10001,  # Есть офер в продаже
                    10002,  # Есть белый оффер в продаже
                    10005,
                    10051,
                    10052,  # Виртуальный узел и его дочерний в продаже
                    10007,  # Не листовой hid тоже отобразится
                    10071,
                    10008,
                    10009,
                    10101,  # Узел из нового синего дерева
                ]
            },
            allow_different_len=False,
        )
        self.assertFragmentIn(
            response,
            {
                "emptyNids": [
                    10003,  # офер скрыт has_gone
                    10004,  # офер скрыт по поставщику
                    10006,
                    10061,  # Виртуальный и его дочерний без оферов скрыт
                ]
            },
            allow_different_len=True,
        )

    def test_nids_info_regions_blue(self):
        """
        Проверяем выдачу плэйса nids_info в режиме подсчета регионов доставки:
            - показывает для всех категорий бакеты доставки и для каждого бакета показывает регионы, в которые он доставляет
            - добавляются только регионы городов и деревень
        """

        response = self.report.request_nids_info_pb('place=nids_info&rgb=blue&bsformat=7')
        self.assertFragmentIn(
            response,
            [
                NidsInfo(
                    nid=10001,
                    buckets=[
                        NidsInfoBucket(regions=BUCKET_300_REGIONS),
                        # Бакет 350 не был добавлен, т.к. нет связи склада и СД
                    ],
                ),
                NidsInfo(
                    nid=10004,
                    buckets=[
                        NidsInfoBucket(regions=BUCKET_310_REGIONS),
                        # Бакет 351 не был добавлен, т.к. нет связи склада и СД
                    ],
                ),
                NidsInfo(
                    nid=10051,
                    buckets=[
                        NidsInfoBucket(regions=BUCKET_320_REGIONS),
                        # Бакет 352 не был добавлен, т.к. нет связи склада и СД
                    ],
                ),
                NidsInfo(
                    nid=10052,
                    buckets=[
                        NidsInfoBucket(regions=BUCKET_300_REGIONS),
                    ],
                ),
                NidsInfo(
                    nid=10005,
                    buckets=[
                        # Виртуальный узел имеет те же регионы, что его дочерние
                        NidsInfoBucket(regions=BUCKET_320_REGIONS),  # 51
                        NidsInfoBucket(regions=BUCKET_300_REGIONS),  # 52
                    ],
                ),
                NidsInfo(
                    nid=10007,
                    buckets=[
                        NidsInfoBucket(regions=BUCKET_330_REGIONS),
                    ],
                ),
                NidsInfo(
                    nid=10009,
                    buckets=[
                        NidsInfoBucket(regions=BUCKET_DROP_SHIP_REGIONS),
                    ],
                ),
                NidsInfo(
                    nid=10101,
                    buckets=[
                        NidsInfoBucket(regions=BUCKET_300_REGIONS),
                    ],
                ),
                NidsInfo(
                    nid=Const.ROOT_NID,
                    buckets=[
                        NidsInfoBucket(regions=BUCKET_300_REGIONS),
                        NidsInfoBucket(regions=BUCKET_310_REGIONS),
                        NidsInfoBucket(regions=BUCKET_320_REGIONS),
                        NidsInfoBucket(regions=BUCKET_330_REGIONS),
                    ],
                ),
            ],
        )

        # Для категории 8 офер не имеет бакетов, которые разрешены LMS, но все равно находится в протобуфе
        self.assertFragmentIn(response, [NidsInfo(nid=10008, buckets=[])])

    def test_lms_warehouse_carrier(self):
        """
        Проверяем, что при наличии связи склад-СД (warehouse=100, carrier=202, офер 82) категория 8 показывается с доставкой в регионы 30x, 32x, 33x
        То, что категория скрыта проверяется в тесте test_nids_info_regions
        """
        # Добавляем связь склада и СД
        self.dynamic.lms += [
            self.get_warehouse_and_delivery_service(100, 202),
            self.get_warehouse_and_delivery_service(101, 202),
        ]

        response = self.report.request_nids_info_pb('place=nids_info&rgb=blue&bsformat=7')
        self.assertFragmentIn(
            response,
            [
                NidsInfo(
                    nid=10001,
                    buckets=[
                        NidsInfoBucket(regions=BUCKET_300_REGIONS),
                        NidsInfoBucket(regions=BUCKET_350_REGIONS),
                    ],
                ),
                NidsInfo(
                    nid=10004,
                    buckets=[
                        NidsInfoBucket(regions=BUCKET_310_REGIONS),
                        NidsInfoBucket(regions=BUCKET_351_REGIONS),
                    ],
                ),
                NidsInfo(
                    nid=10051,
                    buckets=[
                        NidsInfoBucket(regions=BUCKET_320_REGIONS),
                        NidsInfoBucket(regions=BUCKET_352_REGIONS),
                    ],
                ),
                NidsInfo(
                    nid=10008,
                    buckets=[
                        NidsInfoBucket(regions=BUCKET_350_REGIONS),
                        NidsInfoBucket(regions=BUCKET_351_REGIONS),
                        NidsInfoBucket(regions=BUCKET_352_REGIONS),
                    ],
                ),
            ],
        )


if __name__ == '__main__':
    main()
