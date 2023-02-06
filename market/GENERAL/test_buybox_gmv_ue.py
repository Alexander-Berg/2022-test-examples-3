#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    BlueOffer,
    DateSwitchTimeAndRegionInfo,
    DeliveryBucket,
    DeliveryOption,
    DynamicDeliveryRestriction,
    DynamicDeliveryServiceInfo,
    DynamicShop,
    DynamicWarehouseAndDeliveryServiceInfo,
    DynamicWarehouseDelivery,
    DynamicWarehouseInfo,
    DynamicWarehouseLink,
    DynamicWarehousesPriorityInRegion,
    Elasticity,
    MarketSku,
    Region,
    RegionalDelivery,
    Shop,
    ShopOperationalRating,
    WarehouseWithPriority,
    WarehousesPriorityInRegion,
    WarehouseToRegions,
    ResaleCondition,
)
from core.testcase import TestCase, main
from core.matcher import Absent, EqualToOneOf
from unittest import skip

elasticity_from_ticket = [
    Elasticity(price_variant=165, demand_mean=1.06861),
    Elasticity(price_variant=181, demand_mean=0.969983),
    Elasticity(price_variant=198, demand_mean=0.888021),
    Elasticity(price_variant=215, demand_mean=0.767469),
    Elasticity(price_variant=232, demand_mean=0.646268),
    Elasticity(price_variant=248, demand_mean=0.533094),
    Elasticity(price_variant=265, demand_mean=0.494469),
    Elasticity(price_variant=282, demand_mean=0.452901),
    Elasticity(price_variant=299, demand_mean=0.429835),
    Elasticity(price_variant=316, demand_mean=0.424944),
]

unsorted_elasticity = [
    Elasticity(price_variant=300, demand_mean=10),
    Elasticity(price_variant=100, demand_mean=200),
    Elasticity(price_variant=200, demand_mean=80),
]

sorted_elasticity = [
    Elasticity(price_variant=100, demand_mean=200),
    Elasticity(price_variant=200, demand_mean=80),
    Elasticity(price_variant=300, demand_mean=10),
]

unsorted_elasticity_with_duplicates = [
    Elasticity(price_variant=300, demand_mean=10),
    Elasticity(price_variant=100, demand_mean=200),
    Elasticity(price_variant=100, demand_mean=200),
    Elasticity(price_variant=300, demand_mean=10),
    Elasticity(price_variant=100, demand_mean=200),
    Elasticity(price_variant=200, demand_mean=80),
]

decreasing_bug_elasticity = [
    Elasticity(price_variant=85, demand_mean=89.559),
    Elasticity(price_variant=92, demand_mean=82.7448),
    Elasticity(price_variant=99, demand_mean=76.8941),
    Elasticity(price_variant=106, demand_mean=71.8162),
    Elasticity(price_variant=113, demand_mean=67.3674),
    Elasticity(price_variant=120, demand_mean=63.4376),
    Elasticity(price_variant=127, demand_mean=59.9411),
    Elasticity(price_variant=134, demand_mean=43.5061),
    Elasticity(price_variant=141, demand_mean=43.5061),
    Elasticity(price_variant=148, demand_mean=41.4869),
]


def dict_to_rearr(rearr_flags):
    result = ""
    for key in rearr_flags.keys():
        result += str(key) + "=" + str(rearr_flags[key]) + ";"
    return result


class T(TestCase):
    """
    Тестирование органического байбокса
    """

    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['enable_fast_promo_matcher=0;enable_fast_promo_matcher_test=0']

        cls.index.regiontree += [
            Region(
                rid=14838,
                region_type=Region.FEDERAL_DISTRICT,
                children=[
                    Region(
                        rid=1,
                        region_type=Region.FEDERATIVE_SUBJECT,
                        children=[
                            Region(
                                rid=213,
                                tz_offset=10680,
                                children=[Region(rid=3, tz_offset=10800), Region(rid=4, tz_offset=10800)],
                            ),
                            Region(
                                rid=2,
                                tz_offset=10800,
                                children=[
                                    Region(rid=5, tz_offset=10800),
                                ],
                            ),
                        ],
                    ),
                    Region(
                        rid=11029,
                        region_type=Region.FEDERATIVE_SUBJECT,
                        children=[
                            Region(rid=39, children=[Region(rid=123)]),
                        ],
                    ),
                ],
            )
        ]

        cls.index.shops += [
            Shop(
                fesh=1111,
                datafeed_id=1111,
                priority_region=213,
                regions=[225],
                name="1P-Магазин 145 склад",
                supplier_type=Shop.FIRST_PARTY,
                blue=Shop.BLUE_REAL,
                warehouse_id=145,
            ),
            Shop(
                fesh=2222,
                datafeed_id=2222,
                priority_region=213,
                regions=[225],
                name="1P-Магазин 147 склад",
                supplier_type=Shop.FIRST_PARTY,
                blue=Shop.BLUE_REAL,
                warehouse_id=147,
            ),
            Shop(
                fesh=3333,
                datafeed_id=3333,
                priority_region=213,
                regions=[225],
                name="3P-Магазин 145 склад",
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
                warehouse_id=145,
            ),
            Shop(
                fesh=30,
                datafeed_id=30,
                priority_region=2,
                regions=[125],
                name="3P поставщик Вася",
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
                warehouse_id=145,
            ),
            Shop(
                fesh=31,
                datafeed_id=31,
                priority_region=2,
                regions=[2],
                name="3P поставщик Вася Питерский",
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
                warehouse_id=1470,
            ),
            Shop(
                fesh=32,
                datafeed_id=32,
                priority_region=213,
                regions=[225],
                name="3P поставщик Вася Московский",
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
                warehouse_id=147,
            ),
        ]

        cls.index.shop_operational_rating += [
            ShopOperationalRating(
                calc_time=1589936458409,
                shop_id=30,
                late_ship_rate=5.9,
                cancellation_rate=1.93,
                return_rate=0.1,
                total=99.8,
            ),
            ShopOperationalRating(
                calc_time=1589936458409,
                shop_id=31,
                late_ship_rate=5.9,
                cancellation_rate=1.93,
                return_rate=0.1,
                total=99.8,
            ),
        ]

        cls.settings.lms_autogenerate = False
        cls.dynamic.lms += [
            DynamicWarehouseInfo(id=145, home_region=213),
            DynamicWarehouseInfo(id=147, home_region=213),
            DynamicWarehouseInfo(id=1470, home_region=2),
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=145,
                delivery_service_id=157,
                date_switch_time_infos=[DateSwitchTimeAndRegionInfo(date_switch_hour=23, region_to=225)],
            ),
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=147,
                delivery_service_id=157,
                date_switch_time_infos=[DateSwitchTimeAndRegionInfo(date_switch_hour=23, region_to=225)],
            ),
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=1470,
                delivery_service_id=157,
                date_switch_time_infos=[DateSwitchTimeAndRegionInfo(date_switch_hour=23, region_to=225)],
            ),
            DynamicDeliveryServiceInfo(id=157, rating=2),
            DynamicWarehousesPriorityInRegion(region=225, warehouses=[145, 147, 1470]),
        ]

        cls.index.warehouse_priorities += [
            # в Москве приоритет складов одинаков, и все офферы будут становиться buybox равновероятно
            WarehousesPriorityInRegion(
                regions=[213],
                warehouse_with_priority=[
                    WarehouseWithPriority(145, 0),
                    WarehouseWithPriority(147, 0),
                    WarehouseWithPriority(1470, 0),
                ],
            ),
            # в Питере оффер со 145 склада не имеет шанса попасть даже с флагом market_blue_random_buybox
            WarehousesPriorityInRegion(
                regions=[2],
                warehouse_with_priority=[
                    WarehouseWithPriority(145, 1),
                    WarehouseWithPriority(147, 0),
                    WarehouseWithPriority(1470, 0),
                ],
            ),
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=1234,
                carriers=[157],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                regional_options=[
                    RegionalDelivery(
                        rid=225, options=[DeliveryOption(price=15, shop_delivery_price=15, day_from=1, day_to=2)]
                    ),
                    RegionalDelivery(
                        rid=227, options=[DeliveryOption(price=48, shop_delivery_price=48, day_from=3, day_to=5)]
                    ),
                    RegionalDelivery(
                        rid=229, options=[DeliveryOption(price=58, shop_delivery_price=58, day_from=4, day_to=6)]
                    ),
                ],
            ),
            DeliveryBucket(
                bucket_id=1235,
                carriers=[157],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                regional_options=[
                    RegionalDelivery(
                        rid=224, options=[DeliveryOption(price=35, shop_delivery_price=35, day_from=2, day_to=4)]
                    ),
                    RegionalDelivery(
                        rid=225, options=[DeliveryOption(price=45, shop_delivery_price=45, day_from=3, day_to=5)]
                    ),
                    RegionalDelivery(
                        rid=226, options=[DeliveryOption(price=55, shop_delivery_price=55, day_from=4, day_to=6)]
                    ),
                ],
            ),
        ]

        # при включенной рандомизации должна проверятся логика приоритета складов,
        # при этом, если все склады имеют одинаковый приоритет, то
        # остальные оффера, попадающие в диапазон цены от [minPrice; minPirce * (1 + market_blue_random_buybox_price_threshold)]
        # показываются равновероятно независимо от их цены
        cls.index.mskus += [
            MarketSku(
                title="Абрикосовые эклеры",
                hyperid=2,
                sku=100002,
                hid=100,
                delivery_buckets=[1234],
                buybox_elasticity=[
                    Elasticity(price_variant=100, demand_mean=200),
                    Elasticity(price_variant=200, demand_mean=80),
                    Elasticity(price_variant=300, demand_mean=10),
                ],
                blue_offers=[
                    BlueOffer(price=1300, feedid=1111, waremd5="BLUE-100002-FEED-1111Q"),
                    BlueOffer(price=1315, feedid=2222, waremd5="BLUE-100002-FEED-2222g"),
                    BlueOffer(price=1301, feedid=3333, waremd5="BLUE-100002-FEED-3333w"),
                ],
            ),
            MarketSku(
                title="Сюрстреминг",
                hyperid=3,
                sku=100003,
                hid=100,
                delivery_buckets=[1234],
                blue_offers=[
                    BlueOffer(price=1000, feedid=1111, waremd5="BLUE-100003-FEED-1111Q"),
                    BlueOffer(price=1100, feedid=2222, waremd5="BLUE-100003-FEED-2222g"),
                    BlueOffer(price=1300, feedid=3333, waremd5="BLUE-100003-FEED-3333w"),
                ],
            ),
            MarketSku(
                title="Эксперименты с формулами 11",
                hyperid=2,
                sku=100005,
                hid=100,
                delivery_buckets=[1234],
                blue_offers=[
                    BlueOffer(price=1000, feedid=1111, waremd5="BLUE-100005-FEED-1111g"),
                    BlueOffer(price=1011, feedid=3333, waremd5="BLUE-100005-FEED-3333Q"),
                ],
            ),
            MarketSku(
                title="Эксперименты с gmv оффера справа от границы",
                hyperid=2,
                sku=100006,
                hid=100,
                delivery_buckets=[1234],
                buybox_elasticity=sorted_elasticity,
                blue_offers=[
                    BlueOffer(price=1000, feedid=1111, waremd5="BLUE-100006-FEED-1111g"),
                    BlueOffer(price=1011, feedid=3333, waremd5="BLUE-100006-FEED-3333Q"),
                ],
            ),
            MarketSku(
                title="Эксперименты с gmv несортированная эластичность оффера справа от границы",
                hyperid=2,
                sku=100007,
                hid=100,
                delivery_buckets=[1235],
                buybox_elasticity=unsorted_elasticity,
                blue_offers=[
                    BlueOffer(price=1000, feedid=1111, waremd5="BLUE-100007-FEED-1111g"),
                    BlueOffer(price=1011, feedid=3333, waremd5="BLUE-100007-FEED-3333Q"),
                ],
            ),
            MarketSku(
                title="Эксперименты с gmv несортированная эластичность оффера слева от границы",
                hyperid=2,
                sku=100008,
                hid=100,
                delivery_buckets=[1235],
                buybox_elasticity=unsorted_elasticity,
                blue_offers=[
                    BlueOffer(price=20, feedid=1111, waremd5="BLUE-100008-FEED-1111g"),
                    BlueOffer(price=80, feedid=3333, waremd5="BLUE-100008-FEED-3333Q"),
                ],
            ),
            MarketSku(
                title="Эксперименты с gmv оффера слева от границы",
                hyperid=2,
                sku=100009,
                hid=100,
                delivery_buckets=[1234],
                buybox_elasticity=sorted_elasticity,
                blue_offers=[
                    BlueOffer(price=80, feedid=1111, waremd5="BLUE-100009-FEED-1111g"),
                    BlueOffer(price=70, feedid=1111, waremd5="BLUE-100009-FEED-1111e"),
                    BlueOffer(price=90, feedid=3333, waremd5="BLUE-100009-FEED-3333Q"),
                ],
            ),
            MarketSku(
                title="Эксперименты с gmv репрезентативные оффера",
                hyperid=2,
                sku=100010,
                hid=100,
                delivery_buckets=[1234],
                buybox_elasticity=sorted_elasticity,
                blue_offers=[
                    BlueOffer(price=90, feedid=1111, waremd5="BLUE-100010-FEED-1111g"),
                    BlueOffer(price=120, feedid=1111, waremd5="BLUE-100010-FEED-1112g"),
                    BlueOffer(price=160, feedid=3333, waremd5="BLUE-100010-FEED-3333Q"),
                    BlueOffer(price=200, feedid=3333, waremd5="BLUE-100010-FEED-3334Q"),
                    BlueOffer(price=240, feedid=3333, waremd5="BLUE-100010-FEED-3335Q"),
                    BlueOffer(price=280, feedid=3333, waremd5="BLUE-100010-FEED-3336Q"),
                    BlueOffer(price=320, feedid=3333, waremd5="BLUE-100010-FEED-3337Q"),
                ],
            ),
            MarketSku(
                title="Эксперименты с gmv несортированная эластичность репрезентативные оффера",
                hyperid=2,
                sku=100011,
                hid=100,
                delivery_buckets=[1234],
                buybox_elasticity=unsorted_elasticity,
                blue_offers=[
                    BlueOffer(price=90, feedid=1111, waremd5="BLUE-100011-FEED-1111g"),
                    BlueOffer(price=120, feedid=1111, waremd5="BLUE-100011-FEED-1112g"),
                    BlueOffer(price=160, feedid=3333, waremd5="BLUE-100011-FEED-3333Q"),
                    BlueOffer(price=200, feedid=3333, waremd5="BLUE-100011-FEED-3334Q"),
                    BlueOffer(price=240, feedid=3333, waremd5="BLUE-100011-FEED-3335Q"),
                    BlueOffer(price=280, feedid=3333, waremd5="BLUE-100011-FEED-3336Q"),
                    BlueOffer(price=320, feedid=3333, waremd5="BLUE-100011-FEED-3337Q"),
                ],
            ),
            MarketSku(
                title="Эксперименты с gmv несортированная эластичность с дубликатами репрезентативные оффера",
                hyperid=2,
                sku=100012,
                hid=100,
                delivery_buckets=[1234],
                buybox_elasticity=unsorted_elasticity_with_duplicates,
                blue_offers=[
                    BlueOffer(price=90, feedid=1111, waremd5="BLUE-100012-FEED-1111g"),
                    BlueOffer(price=120, feedid=1111, waremd5="BLUE-100012-FEED-1112g"),
                    BlueOffer(price=160, feedid=3333, waremd5="BLUE-100012-FEED-3333Q"),
                    BlueOffer(price=200, feedid=3333, waremd5="BLUE-100012-FEED-3334Q"),
                    BlueOffer(price=240, feedid=3333, waremd5="BLUE-100012-FEED-3335Q"),
                    BlueOffer(price=280, feedid=3333, waremd5="BLUE-100012-FEED-3336Q"),
                    BlueOffer(price=320, feedid=3333, waremd5="BLUE-100012-FEED-3337Q"),
                ],
            ),
            MarketSku(
                title="Эксперименты с gmv оффера в первом диапазоне",
                hyperid=2,
                sku=100013,
                hid=100,
                delivery_buckets=[1234],
                buybox_elasticity=sorted_elasticity,
                blue_offers=[
                    BlueOffer(price=120, feedid=1111, waremd5="BLUE-100013-FEED-1112g"),
                    BlueOffer(price=126, feedid=3333, waremd5="BLUE-100013-FEED-3333Q"),
                ],
            ),
            MarketSku(
                title="Эксперименты с gmv рейтинги",
                hyperid=2,
                sku=100014,
                hid=100,
                delivery_buckets=[1235],
                buybox_elasticity=sorted_elasticity,
                blue_offers=[
                    BlueOffer(price=115, feedid=30, waremd5="BLUE-100014-FEED-1112g", weight=100000),
                    BlueOffer(price=121, feedid=3333, waremd5="BLUE-100014-FEED-3333Q", weight=100000),
                ],
            ),
            MarketSku(
                title="проверка эластичности",
                hyperid=2,
                sku=100015,
                hid=100,
                delivery_buckets=[1235],
                buybox_elasticity=decreasing_bug_elasticity,
                blue_offers=[
                    BlueOffer(price=122, feedid=3333, waremd5="BLUE-100015-FEED-3333t", weight=100000),
                    BlueOffer(price=129, feedid=1111, waremd5="BLUE-100015-FEED-1112g", weight=100000),
                    BlueOffer(price=121, feedid=3333, waremd5="BLUE-100015-FEED-3333Q", weight=100000),
                ],
            ),
            MarketSku(
                title="Первый выигрывает по юнит-экономике",
                hyperid=2,
                sku=100016,
                hid=100,
                delivery_buckets=[1234],
                purchase_price=120.33,
                buybox_elasticity=sorted_elasticity,
                blue_offers=[
                    BlueOffer(price=150, feedid=1111, waremd5="BLUE-100016-FEED-1112g"),
                    BlueOffer(price=121, feedid=1111, waremd5="BLUE-100016-FEED-3333Q"),
                ],
            ),
            MarketSku(
                title="Выбираем случайный оффер среди офферов с одинаковой UE",
                hyperid=2,
                sku=100017,
                hid=100,
                delivery_buckets=[1234],
                purchase_price=120.33,
                buybox_elasticity=sorted_elasticity,
                blue_offers=[
                    BlueOffer(price=150, feedid=1111, waremd5="BLUE-100017-FEED-1112g"),
                    BlueOffer(price=148, feedid=1111, waremd5="BLUE-100017-FEED-1123g"),
                    BlueOffer(price=149, feedid=1111, waremd5="BLUE-100017-FEED-1122g"),
                    BlueOffer(price=121, feedid=1111, waremd5="BLUE-100017-FEED-3333Q"),
                ],
            ),
            MarketSku(
                title="Расчёт эластичности на примере из прода",
                hyperid=2,
                sku=100018,
                hid=100,
                delivery_buckets=[1234],
                buybox_elasticity=elasticity_from_ticket,
                blue_offers=[
                    BlueOffer(price=306, feedid=1111, waremd5="BLUE-100018-FEED-1112g"),
                    BlueOffer(price=313, feedid=1111, waremd5="BLUE-100018-FEED-3333Q"),
                ],
            ),
            MarketSku(
                title="Буст локальных поставщиков",
                hyperid=2,
                sku=100025,
                hid=100,
                delivery_buckets=[1235],
                buybox_elasticity=sorted_elasticity,
                blue_offers=[
                    BlueOffer(price=118, feedid=31, waremd5="BLUE-100025-FEED-3133g"),
                    BlueOffer(price=115, feedid=32, waremd5="BLUE-100025-FEED-3233g"),
                ],
            ),
            MarketSku(
                title="Проверка бу товаров",
                hyperid=2,
                sku=100027,
                hid=100,
                delivery_buckets=[1235],
                buybox_elasticity=sorted_elasticity,
                blue_offers=[
                    BlueOffer(price=118, feedid=31, waremd5="BLUE-100027-FEED-3133g"),
                    BlueOffer(price=115, feedid=32, waremd5="BLUE-100027-FEED-3233g"),
                    BlueOffer(
                        price=116, feedid=33, resale_condition=ResaleCondition.PERFECT, waremd5="BLUE-100027-FEED-3333g"
                    ),
                    BlueOffer(
                        price=117,
                        feedid=34,
                        resale_condition=ResaleCondition.EXCELLENT,
                        waremd5="BLUE-100027-FEED-3433g",
                    ),
                ],
            ),
            MarketSku(
                title="Только бу",
                hyperid=2,
                sku=100028,
                hid=100,
                delivery_buckets=[1235],
                buybox_elasticity=sorted_elasticity,
                blue_offers=[
                    BlueOffer(
                        price=118, feedid=41, resale_condition=ResaleCondition.PERFECT, waremd5="BLUE-100028-FEED-4133g"
                    ),
                    BlueOffer(
                        price=115, feedid=42, resale_condition=ResaleCondition.PERFECT, waremd5="BLUE-100028-FEED-4233g"
                    ),
                    BlueOffer(
                        price=116, feedid=43, resale_condition=ResaleCondition.PERFECT, waremd5="BLUE-100028-FEED-4333g"
                    ),
                    BlueOffer(
                        price=117,
                        feedid=44,
                        resale_condition=ResaleCondition.EXCELLENT,
                        waremd5="BLUE-100028-FEED-4433g",
                    ),
                ],
            ),
        ]

        cls.index.warehouse_to_regions += [
            WarehouseToRegions(region_id=2, warehouse_id=1470),
        ]

    @classmethod
    def prepare_nordstream(cls):
        cls.settings.nordstream_autogenerate = False
        for warehouse_id in (145, 147, 1470):
            cls.dynamic.nordstream += [DynamicWarehouseLink(warehouse_id, [warehouse_id])]
            cls.dynamic.nordstream += [
                DynamicWarehouseDelivery(
                    warehouse_id,
                    {
                        225: [
                            DynamicDeliveryRestriction(min_days=1, max_days=2, cost=15, max_phys_weight=1000),
                            DynamicDeliveryRestriction(min_days=3, max_days=5, cost=45),
                        ],
                    },
                ),
            ]

    def test_random_buybox_with_priority_warehouse(self):
        """В Питере 147 склад имеет жесткий приоритет,
        поэтому оффера со склада 145 не могут попасть в рандомный buybox,
        при этом оффер со склада 147 попадет туда несмотря на порог по цене
        """
        d = '&rearr-factors=market_debug_buybox=1;market_blue_random_buybox=1;market_blue_random_buybox_price_threshold=0.1;market_blue_buybox_no_warehouse_priority=0'
        sku = 100002  # sku c поставщикм с 147 склада
        for yandexuid in range(1, 10):
            response = self.report.request_json(
                'place=productoffers&market-sku={}&offers-set=defaultList&regset=2&rids=2&yandexuid={}'.format(
                    sku, yandexuid
                )
                + d
            )
            self.assertFragmentIn(response, {'search': {'results': [{'wareId': 'BLUE-100002-FEED-2222g'}]}})

    def test_random_buybox(self):
        """Проверяем, что рандомный байбокс отсекает оффера с большой ценой по порогу"""
        d = '&debug=da&rearr-factors=market_debug_buybox=1;market_blue_random_buybox=1;market_blue_buybox_by_gmv_ue=0;market_blue_random_buybox_price_threshold=0.1;market_blue_buybox_no_warehouse_priority=0'  # noqa
        hits_counter = {'BLUE-100003-FEED-1111Q': 0, 'BLUE-100003-FEED-2222g': 0}
        count = 100
        for yandexuid in range(1, count):
            sku = 100003  # sku с отсекаемым по цене оффером
            response = self.report.request_json(
                'place=productoffers&market-sku={}&offers-set=defaultList&regset=2&rids=213&yandexuid={}'.format(
                    sku, yandexuid
                )
                + d
            )
            self.assertFragmentIn(
                response,
                {
                    'search': {
                        'results': [
                            {
                                'wareId': EqualToOneOf('BLUE-100003-FEED-1111Q', 'BLUE-100003-FEED-2222g'),
                            }
                        ]
                    }
                },
            )
            hit1, _ = response.contains({'search': {'results': [{'wareId': 'BLUE-100003-FEED-1111Q'}]}})
            hit2, _ = response.contains({'search': {'results': [{'wareId': 'BLUE-100003-FEED-2222g'}]}})
            hits_counter['BLUE-100003-FEED-1111Q'] += int(hit1)
            hits_counter['BLUE-100003-FEED-2222g'] += int(hit2)

        # Цены должны быть равномерно распределены независимо от цены
        epsilon = 0.05
        self.assertAlmostEqual(
            hits_counter['BLUE-100003-FEED-1111Q'], hits_counter['BLUE-100003-FEED-2222g'], delta=epsilon * count
        )

    def test_buybox_by_gmv_ue_no_elasticity(self):
        d = '&rearr-factors=market_debug_buybox=1;market_blue_buybox_by_gmv_ue=1&debug=da'
        response = self.report.request_json(
            'place=productoffers&market-sku=100005&offers-set=defaultList&yandexuid=1' + d
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'wareId': 'BLUE-100005-FEED-1111g',
                            'debug': {
                                'buyboxDebug': {
                                    'WonMethod': 'WON_BY_EXCHANGE',
                                }
                            },
                        }
                    ]
                }
            },
        )
        self.show_log.expect(won_method=1)

    def test_buybox_by_gmv_ue_win_gmv_right(self):
        d = '&rearr-factors=market_debug_buybox=1;market_blue_buybox_by_gmv_ue=1&debug=da'
        response = self.report.request_json(
            'place=productoffers&market-sku=100006&offers-set=defaultList&rids=213&yandexuid=1' + d
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'wareId': 'BLUE-100006-FEED-1111g',
                            'debug': {
                                'buyboxDebug': {
                                    'WonMethod': 'WON_BY_EXCHANGE',
                                }
                            },
                        }
                    ]
                }
            },
        )
        self.assertFragmentIn(response, "ALL_ELASTICITIES_RIGHT. Is advertisement buybox: 0")
        self.assertFragmentNotIn(response, "old_algorithm_is_being_used")

    def test_buybox_by_gmv_ue_win_gmv_unsorted_elasticity_right(self):
        d = '&rearr-factors=market_debug_buybox=1;market_blue_buybox_by_gmv_ue=1&debug=da'
        response = self.report.request_json(
            'place=productoffers&market-sku=100007&offers-set=defaultList&rids=213&yandexuid=1' + d
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'wareId': 'BLUE-100007-FEED-1111g',
                            'debug': {
                                'buyboxDebug': {
                                    'WonMethod': 'WON_BY_EXCHANGE',
                                }
                            },
                        }
                    ]
                }
            },
        )
        self.assertFragmentIn(response, "ALL_ELASTICITIES_RIGHT")
        self.assertFragmentNotIn(response, "old_algorithm_is_being_used")

    def test_buybox_by_gmv_ue_win_gmv_left(self):
        d = '&rearr-factors=market_blue_buybox_by_gmv_ue=1;market_blue_buybox_max_price_rel=1000;market_blue_buybox_price_rel_max_threshold=1000&debug=da'
        response = self.report.request_json(
            'place=productoffers&market-sku=100008&offers-set=defaultList&rids=213&yandexuid=1' + d
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'wareId': 'BLUE-100008-FEED-3333Q',  # problem: expensive offer wins by GMV (because of multiplication on price)
                            'debug': {
                                'buyboxDebug': {
                                    'WonMethod': 'WON_BY_EXCHANGE',
                                    'Offers': [
                                        {'WareMd5': 'BLUE-100008-FEED-1111g'},
                                        {'WareMd5': 'BLUE-100008-FEED-3333Q'},
                                    ],
                                }
                            },
                        }
                    ]
                }
            },
        )
        self.assertFragmentIn(response, "ALL_ELASTICITIES_LEFT")
        self.assertFragmentNotIn(response, "old_algorithm_is_being_used")

    def test_buybox_by_gmv_ue_win_gmv_left_too_high_price(self):
        d = '&rearr-factors=market_debug_buybox=1;market_blue_buybox_max_price_rel=1.1;market_blue_buybox_by_gmv_ue=1&debug=da'
        response = self.report.request_json(
            'place=productoffers&market-sku=100008&offers-set=defaultList&rids=213&yandexuid=1' + d
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'wareId': 'BLUE-100008-FEED-1111g',
                            'debug': {
                                'buyboxDebug': {
                                    'WonMethod': 'SINGLE_OFFER_AFTER_BUYBOX_FILTERS',
                                    'Offers': [{'WareMd5': 'BLUE-100008-FEED-1111g'}],
                                    'RejectedOffers': [
                                        {
                                            'RejectReason': 'TOO_HIGH_PRICE',
                                            'Offer': {'WareMd5': 'BLUE-100008-FEED-3333Q'},
                                        }
                                    ],
                                }
                            },
                        }
                    ]
                }
            },
        )
        # Проверяем, что WonMethod пишется в shows log (3 соответствует SINGLE_OFFER_AFTER_BUYBOX_FILTER)
        self.show_log.expect(won_method=3)

    def test_buybox_by_gmv_ue_win_gmv_unsorted_elasticity_left(self):
        d = '&rearr-factors=market_debug_buybox=1;market_blue_buybox_by_gmv_ue=1;market_blue_buybox_by_gmv_ue_with_delivery=1;market_blue_buybox_max_price_rel=1000&debug=da'
        response = self.report.request_json(
            'place=productoffers&market-sku=100009&offers-set=defaultList&rids=213&yandexuid=1' + d
        )
        self.assertFragmentIn(response, {'search': {'results': [{'wareId': 'BLUE-100009-FEED-1111Q'}]}})
        self.assertFragmentIn(response, "ALL_ELASTICITIES_LEFT")
        self.assertFragmentNotIn(response, "old_algorithm_is_being_used")

    def test_buybox_by_gmv_ue_win_gmv_2offers(self):
        d = (
            '&rearr-factors=market_debug_buybox=1;market_blue_buybox_by_gmv_ue=1;'
            'market_blue_buybox_by_gmv_ue_with_delivery=1;market_blue_buybox_max_price_rel=1000;market_blue_buybox_1p_cancellation_rating_default=0.001;'
            'market_blue_buybox_fbs_hybrid_local_warehouse_priority=0;&debug=da'
        )
        response = self.report.request_json(
            'place=productoffers&market-sku=100013&offers-set=defaultList&rids=213&yandexuid=1' + d
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'wareId': 'BLUE-100013-FEED-1112g',
                            'debug': {
                                'buyboxDebug': {
                                    'WonMethod': 'WON_BY_EXCHANGE',
                                    'Offers': [
                                        {'WareMd5': 'BLUE-100013-FEED-1112g', 'Gmv': 22627},
                                        {'WareMd5': 'BLUE-100013-FEED-3333Q', 'Gmv': 22558.3},
                                    ],
                                }
                            },
                        }
                    ]
                }
            },
        )
        # Проверяем, что WonMethod пишется в shows log (2 соответствует WON_BY_GMV)
        self.show_log.expect(won_method=1)

    def test_buybox_by_gmv_ue_sorted_elasticity(self):
        d = (
            '&rearr-factors=market_debug_buybox=1;market_blue_buybox_by_gmv_ue=1;market_blue_buybox_by_gmv_ue_with_delivery=1;'
            'market_blue_buybox_delivery_context_approx=0;market_blue_buybox_max_exchange=0.5;market_blue_buybox_max_price_rel=1000;market_blue_buybox_price_rel_max_threshold=1000;market_blue_buybox_1p_cancellation_rating_default=0.001;market_blue_buybox_fbs_hybrid_local_warehouse_priority=0;&debug=da'  # noqa
        )
        response = self.report.request_json(
            'place=productoffers&offers-set=defaultList&market-sku=100010&rids=2&yandexuid=1' + d
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'wareId': 'BLUE-100010-FEED-1112g',
                            'debug': {
                                'buyboxDebug': {
                                    'WonMethod': 'WON_BY_EXCHANGE',
                                    'Offers': [
                                        {
                                            'WareMd5': 'BLUE-100010-FEED-1112g',
                                            'Gmv': 26431.5,
                                        },
                                        {
                                            'WareMd5': 'BLUE-100010-FEED-3333Q',
                                            'Gmv': 25374,
                                        },
                                        {'WareMd5': 'BLUE-100010-FEED-1111g', 'Gmv': 22526.9},
                                        {'WareMd5': 'BLUE-100010-FEED-3334Q', 'Gmv': 19823.5},
                                        {'WareMd5': 'BLUE-100010-FEED-3335Q', 'Gmv': 15462.3},
                                        {'WareMd5': 'BLUE-100010-FEED-3336Q', 'Gmv': 8325.85},
                                        {'WareMd5': 'BLUE-100010-FEED-3337Q', 'Gmv': 0},
                                    ],
                                }
                            },
                        }
                    ]
                }
            },
        )

    def test_buybox_by_gmv_ue_ratings(self):
        d = (
            '&rearr-factors=market_debug_buybox=1;market_blue_buybox_by_gmv_ue=1;market_blue_buybox_by_gmv_ue_with_delivery=1;'
            'market_blue_buybox_max_price_rel=1000000;market_blue_buybox_fbs_hybrid_local_warehouse_priority=0;&debug=da'
        )
        response = self.report.request_json(
            'place=productoffers&offers-set=defaultList&market-sku=100014&rids=2&yandexuid=1' + d
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'wareId': 'BLUE-100014-FEED-3333Q',
                            'debug': {
                                'buyboxDebug': {
                                    'WonMethod': 'WON_BY_EXCHANGE',
                                    'Offers': [
                                        {
                                            'WareMd5': 'BLUE-100014-FEED-1112g',
                                            'Gmv': 19090.6,
                                        },
                                        {
                                            'WareMd5': 'BLUE-100014-FEED-3333Q',
                                            'Gmv': 19455.3,
                                        },
                                    ],
                                }
                            },
                        }
                    ]
                }
            },
        )

    def test_buybox_elasticity(self):
        d = (
            '&rearr-factors=market_debug_buybox=1;market_blue_buybox_by_gmv_ue=1;market_blue_buybox_by_gmv_ue_with_delivery=1;'
            'market_blue_buybox_max_price_rel=1000;market_blue_buybox_1p_cancellation_rating_default=0.001;market_blue_buybox_fbs_hybrid_local_warehouse_priority=0;&debug=da'
        )
        response = self.report.request_json(
            'place=productoffers&offers-set=defaultList&market-sku=100015&rids=213&yandexuid=1' + d
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'wareId': 'BLUE-100015-FEED-3333g',
                            'debug': {
                                'buyboxDebug': {
                                    'WonMethod': 'WON_BY_EXCHANGE',
                                    'Offers': [
                                        {
                                            'WareMd5': 'BLUE-100015-FEED-3333g',
                                            'Gmv': 5719.9,
                                        },
                                        {
                                            'WareMd5': 'BLUE-100015-FEED-1112g',
                                            'Gmv': 5405.43,
                                        },
                                        {
                                            'WareMd5': 'BLUE-100015-FEED-3333Q',
                                            'Gmv': 5718.4,
                                        },
                                    ],
                                }
                            },
                        }
                    ]
                }
            },
        )

    @skip('test is relevant for old place sku_offers')
    def test_buybox_by_gmv_ue_without_delivery(self):
        """
        Тут проверялось, что флагом market_blue_buybox_by_gmv_ue_with_delivery=0 можно выключить взятие доставки в байбоксе в sku_offers.
        Но это старый плейс, байбоксовые тесты должны делать запросы в productoffers, а там этот флаг не работает, так как ДО на КМ, там
        доставка учитывается раньше, чем её успевает отключить этот флаг.
        """
        d = '&rearr-factors=market_debug_buybox=1;market_blue_buybox_by_gmv_ue=1;market_blue_buybox_max_price_rel=1000;market_blue_buybox_by_gmv_ue_with_delivery=0&debug=da'
        response = self.report.request_json('place=sku_offers&market-sku=100014&rids=213&yandexuid=1' + d)
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'wareId': 'BLUE-100014-FEED-3333Q',
                            'debug': {
                                'buyboxDebug': {
                                    'WonMethod': 'WON_BY_EXCHANGE',
                                    'Offers': [
                                        {
                                            'WareMd5': 'BLUE-100014-FEED-1112g',
                                            'Gmv': 19480.2,
                                        },
                                        {
                                            'WareMd5': 'BLUE-100014-FEED-3333Q',
                                            'Gmv': 19852.4,
                                        },
                                    ],
                                }
                            },
                        }
                    ]
                }
            },
        )

    def test_buybox_by_gmv_ue_shop_in_user_region(self):
        # для запроса из Петербурга выбираем оффер из Петербурга
        sku = 100025
        rearr_flags_dict = {
            "market_blue_buybox_max_price_rel": 1000,
            "market_blue_buybox_shop_in_user_region_conversion_coef": 1.1,
            "market_search_boost_regional_warehouses": 4,
            "market_search_boost_regional_warehouses_in": 2,
            "market_blue_buybox_by_gmv_ue_with_delivery": 0,
            "market_blue_buybox_no_warehouse_priority": 0,
            "market_load_boost_locality_from_fb_file": 0,
            "market_blue_buybox_fbs_hybrid_local_warehouse_priority": 0,
        }
        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        query = (
            'place=productoffers&offers-set=defaultList&market-sku=%s&rids=2&yandexuid=1&rearr-factors=%s;&debug=da'
            % (
                sku,
                rearr_flags_str,
            )
        )
        response = self.report.request_json(query)
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'wareId': 'BLUE-100025-FEED-3133g',
                            'debug': {
                                'buyboxDebug': {
                                    'WonMethod': 'WON_BY_EXCHANGE',
                                    'Offers': [
                                        {'WareMd5': 'BLUE-100025-FEED-3133g', 'ShopInUserRegionConversionCoef': 1.1},
                                        {'WareMd5': 'BLUE-100025-FEED-3233g', 'ShopInUserRegionConversionCoef': 1},
                                    ],
                                }
                            },
                        }
                    ]
                }
            },
        )

        rearr_flags_dict = {
            "market_blue_buybox_max_price_rel": 1000,
            "market_blue_buybox_gmv_ue_mix_coef": 1000000,
            "market_blue_buybox_shop_in_user_region_conversion_coef": 1.1,
            "market_search_boost_regional_warehouses": 4,
            "market_search_boost_regional_warehouses_in": 176,
            "market_blue_buybox_by_gmv_ue_with_delivery": 0,
            "market_blue_buybox_no_warehouse_priority": 0,
            "market_blue_buybox_fbs_hybrid_local_warehouse_priority": 0,
        }
        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        query = (
            'place=productoffers&offers-set=defaultList&market-sku=%s&rids=213&yandexuid=1&rearr-factors=%s;&debug=da'
            % (
                sku,
                rearr_flags_str,
            )
        )
        response = self.report.request_json(query)
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'wareId': 'BLUE-100025-FEED-3133g',
                            'debug': {
                                'buyboxDebug': {
                                    'WonMethod': 'WON_BY_EXCHANGE',
                                    'Offers': [
                                        {'WareMd5': 'BLUE-100025-FEED-3133g', 'ShopInUserRegionConversionCoef': 1},
                                        {'WareMd5': 'BLUE-100025-FEED-3233g', 'ShopInUserRegionConversionCoef': 1},
                                    ],
                                }
                            },
                        }
                    ]
                }
            },
        )

    def test_buybox_by_gmv_ue_first_ue_wins(self):
        d = (
            '&rearr-factors=market_debug_buybox=1;market_blue_buybox_by_gmv_ue=1;market_blue_buybox_max_price_rel=1000;market_blue_buybox_price_rel_max_threshold=1000;'
            'market_blue_buybox_shop_in_user_region_conversion_coef=1.1;market_blue_buybox_max_exchange=0.5;market_blue_buybox_1p_cancellation_rating_default=0.001;'
            'market_blue_buybox_gmv_ue_mix_coef=100000;market_blue_buybox_fbs_hybrid_local_warehouse_priority=0;&debug=da'
        )
        response = self.report.request_json(
            'place=productoffers&offers-set=defaultList&market-sku=100016&rids=2&yandexuid=1' + d
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'wareId': 'BLUE-100016-FEED-1112g',
                            'debug': {
                                'buyboxDebug': {
                                    'WonMethod': 'WON_BY_EXCHANGE',
                                    'Offers': [
                                        {
                                            'WareMd5': 'BLUE-100016-FEED-1112g',
                                            'Gmv': 26281.4,
                                            'UeToPrice': -0.2022,
                                        },
                                        {
                                            'WareMd5': 'BLUE-100016-FEED-3333Q',
                                            'Gmv': 26470.1,
                                            'UeToPrice': -0.490331,
                                        },
                                    ],
                                }
                            },
                        }
                    ]
                }
            },
        )

    def test_buybox_random_gmv(self):
        sku = 100014
        count = 100
        hits_counter = {'BLUE-100014-FEED-1112g': 0, 'BLUE-100014-FEED-3333Q': 0}
        for yandexuid in range(1, count):
            d = '&rearr-factors=market_blue_buybox_gmv_ue_mix_coef=0.0001;market_blue_buybox_by_gmv_ue=1;market_blue_buybox_max_price_rel=1000;market_blue_buybox_by_gmv_ue_with_delivery=0;market_blue_buybox_gmv_epsilon=0.1&debug=da'  # noqa
            response = self.report.request_json(
                'place=productoffers&offers-set=defaultList&market-sku={}&rids=2&yandexuid={}'.format(sku, yandexuid)
                + d
            )
            self.assertFragmentIn(
                response,
                {
                    'search': {
                        'results': [
                            {
                                'wareId': EqualToOneOf('BLUE-100014-FEED-1112g', 'BLUE-100014-FEED-3333Q'),
                                'debug': {'buyboxDebug': {'WonMethod': "WON_BY_GMV"}},
                            }
                        ]
                    }
                },
            )
            hit1, _ = response.contains({'search': {'results': [{'wareId': 'BLUE-100014-FEED-1112g'}]}})
            hit2, _ = response.contains({'search': {'results': [{'wareId': 'BLUE-100014-FEED-3333Q'}]}})
            hits_counter['BLUE-100014-FEED-1112g'] += int(hit1)
            hits_counter['BLUE-100014-FEED-3333Q'] += int(hit2)
        epsilon = 0.05
        self.assertAlmostEqual(
            hits_counter['BLUE-100014-FEED-1112g'], hits_counter['BLUE-100014-FEED-3333Q'], delta=epsilon * count
        )

    @classmethod
    def prepare_filtering_by_dynamic(cls):
        cls.dynamic.market_dynamic.disabled_cpa_shops += [
            DynamicShop(6666),
        ]

        cls.index.shops += [
            Shop(
                fesh=6666,
                datafeed_id=6666,
                priority_region=213,
                regions=[225],
                fulfillment_program=False,
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
                name="Отрубленный",
                warehouse_id=145,
                cpc=Shop.CPC_NO,
            )
        ]

        cls.index.mskus += [
            MarketSku(
                title="Сюрстреминг",
                hyperid=4,
                sku=100066,
                hid=100,
                delivery_buckets=[1234],
                blue_offers=[
                    BlueOffer(price=1000, feedid=1111, waremd5="BLUE-100066-FEED-1111Q"),
                    BlueOffer(price=700, feedid=6666, waremd5="BLUE-100066-FEED-6666Q"),
                ],
            ),
        ]

    def test_filtered_offers_filtered_before_buybox(self):
        """
        Проверяем, что фильтр по динамику отрабатывает до выбора байбокса. И в результаты покажется более дорогой оффер.
        """
        sku = 100066
        request = 'place=productoffers&offers-set=defaultList&pp=6&market-sku={}&rids=213&debug=da'.format(sku)
        # Используем трейс поиска ДО, чтобы увидеть, что более дешёвый оффер BLUE-100066-FEED-6666Q отфильтровался, так как он из динамик-магазина
        rearrs = '&rearr-factors=market_documents_search_trace_default_offer=BLUE-100066-FEED-6666Q'
        response = self.report.request_json(request + rearrs)
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'wareId': 'BLUE-100066-FEED-1111Q',
                            'debug': {
                                'buyboxDebug': {
                                    'Offers': [{'WareMd5': 'BLUE-100066-FEED-1111Q'}],
                                }
                            },
                        }
                    ]
                }
            },
        )
        self.assertFragmentIn(
            response,
            {
                'debug': {
                    'docs_search_trace_default_offer': {
                        'traces': [
                            {
                                'document': 'BLUE-100066-FEED-6666Q',
                                'in_accept_doc': True,
                                'passed_accept_doc': False,
                                'accept_doc_filtered_reason': 'DYNAMIC_SHOP',
                                'on_page': Absent(),
                                'passed_rearrange': Absent(),
                            }
                        ]
                    }
                }
            },
        )

    def test_buybox_elasticity_from_ticket(self):
        # We had a problem with elasticity into the ticket: https://st.yandex-team.ru/MARKETMPE-759.
        # Here we've reproduced that situation
        response = self.report.request_json(
            'place=productoffers&market-sku=100018&offers-set=defaultList&rids=213&yandexuid=1&debug=da'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'wareId': 'BLUE-100018-FEED-3333Q',  # Offer with HIGH price is won. It is strange, but it happens because elastisity values are too close
                            'debug': {
                                'buyboxDebug': {
                                    'WonMethod': 'WON_BY_EXCHANGE',
                                    'Offers': [
                                        {
                                            'WareMd5': 'BLUE-100018-FEED-1112g',
                                            'PredictedElasticity': {'Value': 0.427821, 'Type': 'NORMAL'},
                                        },
                                        {
                                            'WareMd5': 'BLUE-100018-FEED-3333Q',
                                            'PredictedElasticity': {'Value': 0.425807, 'Type': 'NORMAL'},
                                        },
                                    ],
                                }
                            },
                        }
                    ]
                }
            },
        )
        # Проверяем, что WonMethod пишется в shows log (2 соответствует WON_BY_GMV)
        self.show_log.expect(won_method=1)

    @classmethod
    def prepare_hybrid_local_warehouse_priority(cls):

        cls.index.shops += [
            Shop(
                fesh=33,
                datafeed_id=33,
                name='Dropship',
                priority_region=213,
                regions=[225],
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
                is_supplier=True,
                fulfillment_program=False,
            ),
        ]

        cls.index.mskus += [
            MarketSku(
                hid=94301,
                hyperid=94301,
                title='blue market sku100026',
                delivery_buckets=[1235],
                sku=100026,
                blue_offers=[BlueOffer(title='Dropship', price=100, feedid=33, waremd5='TestOffer_Dropship___g')],
            ),
        ]

    def test_buybox_hybrid_local_warehouse_priority(self):
        # для запроса из Петербурга выбираем оффер из Петербурга
        sku = 100026
        rearr_flags_dict = {
            "market_blue_buybox_fbs_hybrid_local_warehouse_priority": 1,
        }
        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        query = (
            'place=productoffers&offers-set=defaultList&market-sku=%s&rids=2&yandexuid=1&rearr-factors=%s;&debug=da'
            % (
                sku,
                rearr_flags_str,
            )
        )
        response = self.report.request_json(query)
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'debug': {
                                'buyboxDebug': {
                                    'Offers': [
                                        {'WareMd5': 'TestOffer_Dropship___g', 'ShopInUserRegionConversionCoef': 1.2},
                                    ],
                                }
                            },
                        }
                    ]
                }
            },
        )

        rearr_flags_dict = {
            "market_blue_buybox_fbs_hybrid_local_warehouse_priority": 0,
        }
        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        query = (
            'place=productoffers&offers-set=defaultList&market-sku=%s&rids=213&yandexuid=1&rearr-factors=%s;&debug=da'
            % (
                sku,
                rearr_flags_str,
            )
        )
        response = self.report.request_json(query)
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'debug': {
                                'buyboxDebug': {
                                    'Offers': [
                                        {'WareMd5': 'TestOffer_Dropship___g', 'ShopInUserRegionConversionCoef': 1},
                                    ],
                                }
                            },
                        }
                    ]
                }
            },
        )

    def test_resale_filter(self):
        """
        Проверяем пессимизацию ресейл офферов в смешанной выдаче
        """
        sku = 100027
        used = 'resale_resale'
        new = 'resale_new'

        # Ресейл выключен. Розыгрыш происходит среди новых офферов
        # Бу оффера фильтруются
        request = 'place=productoffers&offers-set=defaultList&pp=6&market-sku={}&rids=213&debug=da'.format(sku)
        rearrs = '&rearr-factors=market_enable_resale_goods=1'

        response = self.report.request_json(request + rearrs)
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'wareId': "BLUE-100027-FEED-3133g",
                            'debug': {
                                'buyboxDebug': {
                                    'Offers': [{'WareMd5': 'BLUE-100027-FEED-3133g'}],
                                    'RejectedOffers': [
                                        {
                                            'RejectReason': 'LESS_PRIORITY_WAREHOUSE',
                                            'Offer': {'WareMd5': "BLUE-100027-FEED-3233g"},
                                        },
                                    ],
                                }
                            },
                        }
                    ]
                }
            },
        )

        # Ресейл включен, но фильтр отсутвует. Розыгрыш происходит среди новых офферов
        # Бу оффера пессимизируются
        request = 'place=productoffers&offers-set=defaultList&pp=6&enable-resale-goods=1&market-sku={}&rids=213&debug=da'.format(
            sku
        )
        rearrs = '&rearr-factors=market_enable_resale_goods=1'

        response = self.report.request_json(request + rearrs)
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'wareId': "BLUE-100027-FEED-3133g",
                            'debug': {
                                'buyboxDebug': {
                                    'Offers': [{'WareMd5': 'BLUE-100027-FEED-3133g'}],
                                    'RejectedOffers': [
                                        {
                                            'RejectReason': 'RESALE_PESSIMIZE',
                                            'Offer': {'WareMd5': 'BLUE-100027-FEED-3333g'},
                                        },
                                        {
                                            'RejectReason': 'LESS_PRIORITY_WAREHOUSE',
                                            'Offer': {'WareMd5': "BLUE-100027-FEED-3233g"},
                                        },
                                        {
                                            'RejectReason': 'RESALE_PESSIMIZE',
                                            'Offer': {'WareMd5': "BLUE-100027-FEED-3433g"},
                                        },
                                    ],
                                }
                            },
                        }
                    ]
                }
            },
        )

        # Ресейл фильтр включен в режим новых офферов. Розыгрыш происходит среди новых офферов.
        # Бу оффера фильтуются до байбокса
        request = (
            'place=productoffers&offers-set=defaultList&pp=6&enable-resale-goods=1'
            '&market-sku={}&resale_goods={}&rids=213&debug=da'
        ).format(sku, new)
        response = self.report.request_json(request + rearrs)
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'wareId': "BLUE-100027-FEED-3133g",
                            'debug': {
                                'buyboxDebug': {
                                    'Offers': [{'WareMd5': "BLUE-100027-FEED-3133g"}],
                                    'RejectedOffers': [
                                        {
                                            'RejectReason': 'LESS_PRIORITY_WAREHOUSE',
                                            'Offer': {'WareMd5': "BLUE-100027-FEED-3233g"},
                                        },
                                    ],
                                }
                            },
                        }
                    ]
                }
            },
        )

        # Ресейл фильтр включен в режим бу офферов. Розыгрыш происходит среди бу офферов
        # Новые оффера фильтруются до байбокса
        request = (
            'place=productoffers&offers-set=defaultList&pp=6&enable-resale-goods=1&'
            'market-sku={}&resale_goods={}&rids=213&debug=da'
        ).format(sku, used)
        response = self.report.request_json(request + rearrs)
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'wareId': "BLUE-100027-FEED-3333g",
                            'debug': {
                                'buyboxDebug': {
                                    'Offers': [{'WareMd5': "BLUE-100027-FEED-3333g"}],
                                }
                            },
                        }
                    ]
                }
            },
        )

    def test_resale_only_filter(self):
        """
        Проверяем розыгрыш байбокса среди ресейл офферов в выдаче только из ресейла
        """
        sku = 100028
        # Ресейл фильтр отсутвует. Новых офферов нет. Розыгрыш происходит среди бу офферов
        request = 'place=productoffers&offers-set=defaultList&enable-resale-goods=1&pp=6&market-sku={}&rids=213&debug=da'.format(
            sku
        )
        rearrs = '&rearr-factors=market_enable_resale_goods=1'

        response = self.report.request_json(request + rearrs)
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'wareId': "BLUE-100028-FEED-4433g",
                            'debug': {
                                'buyboxDebug': {
                                    'Offers': [
                                        {'WareMd5': "BLUE-100028-FEED-4433g"},
                                        {'WareMd5': "BLUE-100028-FEED-4133g"},
                                        {'WareMd5': "BLUE-100028-FEED-4233g"},
                                        {'WareMd5': "BLUE-100028-FEED-4333g"},
                                    ],
                                }
                            },
                        }
                    ]
                }
            },
        )


if __name__ == '__main__':
    main()
