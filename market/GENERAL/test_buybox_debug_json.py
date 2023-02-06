#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

from core.matcher import Round, NotEmpty, EqualToOneOf
from core.testcase import TestCase, main
from core.types import (
    BlueOffer,
    CpaCategory,
    CpaCategoryType,
    DateSwitchTimeAndRegionInfo,
    DeliveryBucket,
    DeliveryOption,
    DynamicDeliveryRestriction,
    DynamicDeliveryServiceInfo,
    DynamicWarehouseAndDeliveryServiceInfo,
    DynamicWarehouseDelivery,
    DynamicWarehouseInfo,
    DynamicWarehouseLink,
    DynamicWarehousesPriorityInRegion,
    Elasticity,
    MarketSku,
    Model,
    Offer,
    Region,
    RegionalDelivery,
    Shop,
    ShopOperationalRating,
    WarehouseWithPriority,
    WarehousesPriorityInRegion,
)


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


class T(TestCase):
    """
    https://st.yandex-team.ru/MDP-1321
    Новый формат логов в buybox debug
    """

    @classmethod
    def prepare(cls):

        cls.index.regiontree += [
            Region(rid=213, region_type=Region.CITY),
            Region(rid=2, region_type=Region.CITY),
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
                medicine_courier=True,
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
                fesh=31,
                datafeed_id=31,
                priority_region=2,
                regions=[125],
                name="3P поставщик Вася",
                supplier_type=Shop.THIRD_PARTY,
                is_supplier=True,
                blue=Shop.BLUE_REAL,
                warehouse_id=145,
                fulfillment_program=True,
            ),
        ]

        cls.index.shop_operational_rating += [
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
            DynamicDeliveryServiceInfo(id=157, rating=2),
            DynamicWarehousesPriorityInRegion(region=225, warehouses=[145, 147]),
        ]

        cls.index.warehouse_priorities += [
            # в Москве приоритет складов одинаков, и все офферы будут становиться buybox равновероятно
            WarehousesPriorityInRegion(
                regions=[213],
                warehouse_with_priority=[
                    WarehouseWithPriority(145, 0),
                    WarehouseWithPriority(147, 0),
                ],
            ),
            # в Питере оффер со 145 склада не имеет шанса попасть даже с флагом market_blue_random_buybox
            WarehousesPriorityInRegion(
                regions=[2],
                warehouse_with_priority=[
                    WarehouseWithPriority(145, 1),
                    WarehouseWithPriority(147, 0),
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
                    BlueOffer(price=80, feedid=1111, waremd5="BLUE-100008-FEED-1111g"),
                    BlueOffer(price=190, feedid=3333, waremd5="BLUE-100008-FEED-3333Q"),
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
                    BlueOffer(price=160, feedid=3333, waremd5="BLUE-100010-FEED-3333Q", offerid="blue.offer.3333q"),
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
                    BlueOffer(price=121, feedid=3333, waremd5="BLUE-100013-FEED-3333Q"),
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
                    BlueOffer(price=115, feedid=31, waremd5="BLUE-100014-FEED-1112g"),
                    BlueOffer(price=121, feedid=3333, waremd5="BLUE-100014-FEED-3333Q"),
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
                    BlueOffer(price=122, feedid=3333, waremd5="BLUE-100015-FEED-3333t"),
                    BlueOffer(price=129, feedid=1111, waremd5="BLUE-100015-FEED-1112g"),
                    BlueOffer(price=121, feedid=3333, waremd5="BLUE-100015-FEED-3333Q"),
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
                title="Выигрыш по рандому",
                hyperid=2,
                sku=100017,
                hid=100,
                delivery_buckets=[1234],
                purchase_price=120.33,
                buybox_elasticity=sorted_elasticity,
                blue_offers=[
                    BlueOffer(price=150, feedid=1111, waremd5="BLUE-100017-FEED-1112g"),
                    BlueOffer(price=150, feedid=1111, waremd5="BLUE-100017-FEED-3333Q"),
                ],
            ),
            MarketSku(
                title="Проверка коэфициента конверсии для медицинских препаратов. Один из них - 15756581",
                hyperid=4,
                sku=100018,
                hid=15756581,
                delivery_buckets=[1234],
                purchase_price=120.33,
                buybox_elasticity=sorted_elasticity,
                blue_offers=[
                    BlueOffer(price=150, feedid=1111, waremd5="BLUE-100018-FEED-1112g", is_baa=True),
                    BlueOffer(price=150, feedid=1111, waremd5="BLUE-100018-FEED-3333Q", is_baa=True),
                ],
            ),
        ]

    @classmethod
    def prepare_nordstream(cls):
        cls.settings.nordstream_autogenerate = False
        for warehouse_id in (145, 147):
            cls.dynamic.nordstream += [DynamicWarehouseLink(warehouse_id, [warehouse_id])]
            cls.dynamic.nordstream += [
                DynamicWarehouseDelivery(
                    warehouse_id,
                    {
                        225: [
                            DynamicDeliveryRestriction(min_days=1, max_days=2, cost=15),
                        ],
                        227: [
                            DynamicDeliveryRestriction(min_days=3, max_days=5, cost=15),
                        ],
                        229: [
                            DynamicDeliveryRestriction(min_days=4, max_days=6, cost=15),
                        ],
                    },
                ),
            ]

    def test_no_debug_without_flag(self):
        """Проверяем, что если нет дебаг флагов, то в выдаче нет debug выдачи"""

        response = self.report.request_json(
            'place=productoffers&pp=1&market-sku=100005&rids=213&offers-set=defaultList&pp=6&rearr-factors=market_buybox_by_supplier_on_white=1;market_blue_buybox_with_delivery_context=1'
        )
        self.assertFragmentNotIn(
            response, {'entity': 'offer', 'wareId': "EpnWVxDQxj4wg7vVI1ElnA", 'debug': {'buyboxDebug': {}}}
        )

    def test_json_debug_old_buybox(self):
        """Проверяем debug выдачу для старого байбокса"""

        response = self.report.request_json(
            'place=productoffers&pp=1&market-sku=100005&rids=213&offers-set=defaultList&pp=6&debug=da&'
            'rearr-factors=market_blue_buybox_disable_old_buybox_algo=0;market_buybox_by_supplier_on_white=1;market_blue_buybox_with_delivery_context=1;market_debug_buybox=1'
        )
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'wareId': "BLUE-100005-FEED-1111g",
                'debug': {'buyboxDebug': {'WonMethod': "OLD_BUYBOX", 'Offers': []}},
            },
        )

    def test_offer_weight(self):
        """Проверяем, OfferWeight"""

        response = self.report.request_json(
            'place=productoffers&market-sku=100005&yandexuid=1&offers-set=defaultList&pp=6&debug=da&rearr-factors=market_debug_buybox=1'
        )
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'wareId': "BLUE-100005-FEED-1111g",
                'debug': {
                    'buyboxDebug': {
                        'WonMethod': "WON_BY_EXCHANGE",
                        'Offers': [
                            {'WareMd5': 'BLUE-100005-FEED-1111g', 'OfferWeight': 2},
                            {'WareMd5': 'BLUE-100005-FEED-3333Q', 'OfferWeight': 0.0832157},
                        ],
                    }
                },
            },
        )

    def test_rejected_offers(self):
        """
        Проверяем, отклоненный оффер
        Контеста не будет, потому что останется один оффер, выберется он.
        """

        response = self.report.request_json(
            'place=productoffers&market-sku=100008&rids=213&offers-set=defaultList&yandexuid=1&pp=6&debug=da&rearr-factors=market_debug_buybox=1;market_blue_buybox_by_gmv_ue=1'
        )
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'wareId': "BLUE-100008-FEED-1111g",
                'debug': {
                    'buyboxDebug': {
                        'WonMethod': "SINGLE_OFFER_AFTER_BUYBOX_FILTERS",
                        'Offers': [{'WareMd5': 'BLUE-100008-FEED-1111g', 'FeedID': 1111}],
                        'RejectedOffers': [
                            {
                                'RejectReason': 'TOO_HIGH_PRICE',
                                'Offer': {'WareMd5': 'BLUE-100008-FEED-3333Q', 'FeedID': 3333},
                            }
                        ],
                    }
                },
            },
        )

    def test_won_by_exchange(self):
        """Проверяем, победу по юнит-экономике"""

        response = self.report.request_json(
            'place=productoffers&market-sku=100016&rids=2&yandexuid=1&offers-set=defaultList&pp=6&'
            'rearr-factors='
            'market_debug_buybox=1;'
            'market_blue_buybox_gmv_ue_mix_coef=0.0001;'
            'market_blue_buybox_use_gmv_in_rel_fields=1;'
            'market_blue_buybox_by_gmv_ue=1;'
            'market_blue_buybox_max_exchange=0.5;'
            'market_blue_buybox_max_price_rel=1000;market_blue_buybox_price_rel_max_threshold=1000;'
            'market_blue_buybox_shop_in_user_region_conversion_coef=1.1;'
            'market_blue_buybox_delivery_context_approx=0;'
            'market_blue_buybox_gmv_epsilon=0.001;'
            'market_blue_buybox_1p_cancellation_rating_default=0.001&debug=da'
        )
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'wareId': "BLUE-100016-FEED-1112g",
                'debug': {
                    'buyboxDebug': {
                        'WonMethod': "WON_BY_EXCHANGE",
                        'Offers': [
                            {
                                'WareMd5': 'BLUE-100016-FEED-1112g',
                                'Conversion': 175.209,
                                'Gmv': 26281.4,
                                'DeltaGmv': 0.00712979,
                                'DeltaUe': 0.288131,
                                'Exchange': 0.024745,
                            },
                            {
                                'WareMd5': 'BLUE-100016-FEED-3333Q',
                                'Conversion': 218.761,
                                'Gmv': 26470.1,
                                'DeltaGmv': 0,
                                'DeltaUe': 0,
                                'Exchange': 0,
                            },
                        ],
                        'RejectedOffers': [],
                    }
                },
            },
        )

    def test_offer_fields(self):
        """Проверяем, поля оффера, попавшие в выдачу"""

        response = self.report.request_json(
            'place=productoffers&market-sku=100010&rids=213&yandexuid=1&offers-set=defaultList&pp=6&rearr-factors=market_debug_buybox=1;'
            'market_blue_buybox_by_gmv_ue=1;market_blue_buybox_max_exchange=0.5;market_blue_buybox_max_price_rel=1000;'
            'market_blue_buybox_max_price_diff=5000;market_blue_buybox_price_rel_max_threshold=5000;market_blue_buybox_delivery_context_approx=0;market_blue_buybox_gmv_epsilon=0.001;'
            'market_blue_buybox_always_compute_approx_gmv_ue=1;market_operational_rating_everywhere=0;'
            'market_blue_buybox_fbs_hybrid_local_warehouse_priority=0;&debug=da'
        )
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'wareId': EqualToOneOf('BLUE-100010-FEED-3333Q', 'BLUE-100010-FEED-1112g'),
                'debug': {
                    'buyboxDebug': {
                        'WonMethod': 'WON_BY_EXCHANGE',
                        'Offers': [
                            {
                                'Conversion': NotEmpty(),
                                'ConversionByDeliveryDayCoef': 1.13,
                                'DeliveryContext': {
                                    'CourierShopPrice': 15,
                                    'CourierDaysMin': 1,
                                    'CourierDaysMax': 2,
                                },
                                'DeltaGmv': NotEmpty(),
                                'DeltaUe': NotEmpty(),
                                'Exchange': NotEmpty(),
                                'TransactionFee': 0.0200001,
                                'FeedID': 3333,
                                'Gmv': NotEmpty(),
                                'IsExactGmvUe': True,
                                'GmvUe': NotEmpty(),
                                'GmvUeRandomized': NotEmpty(),
                                'GmvUeToRelField': NotEmpty(),
                                'IsCrossdock': False,
                                'IsFiltered': False,
                                'IsFulfillment': False,
                                'IsKGT': False,
                                'IsPreferred': False,
                                'IsUserCartWarehouse': False,
                                'UserCartWarehousesWithoutOffer': "",
                                'UserCartWarehousesCountWithoutOffer': 0,
                                'UserCartWarehousesCountWithOffer': 1,
                                'IsWarehouseInUserCartCoef': 0.95,
                                'OfferId': 'blue.offer.3333q',
                                'OfferWeight': Round(0),
                                'PredictedElasticity': {
                                    'Value': 128,
                                    'Type': 'NORMAL',
                                },
                                'PromoBoostCoefficient': 1,
                                'PurchasePrice': 0,
                                'RelevanceData': {
                                    'Price': 160,
                                    'MinAllowedPrice': 160,
                                    'PriceBeforeDynamicStrategy': 0,
                                },
                                'ShopInUserRegionConversionCoef': 1,
                                'ShopOperationalRating': {
                                    'CalcTime': 0,
                                    'LateShipRate': 0.05,
                                    'CancellationRate': 0.011,
                                    'ReturnRate': 0.001,
                                    'Total': 0,
                                },
                                'ShopPriorityRegion': 213,
                                'ShopRatingCoef': NotEmpty(),
                                'SupplierId': 3333,
                                'SupplierType': 'ST_THIRD_PARTY',
                                'UeToPrice': NotEmpty(),
                                'WareMd5': 'BLUE-100010-FEED-3333Q',
                                'WarehouseId': 145,
                                'WarehousePriority': 0,
                            }
                        ],
                    }
                },
            },
        )
        self.assertFragmentIn(response, "IsCardRequest == 1")

    def test_offer_fields_with_market_blue_buybox_gmv_ue_mix_coef(self):
        """Проверяем, поля оффера, попавшие в выдачу"""

        response = self.report.request_json(
            'place=productoffers&market-sku=100010&rids=213&yandexuid=1&offers-set=defaultList&pp=6&'
            'rearr-factors=market_debug_buybox=1;market_blue_buybox_by_gmv_ue=1;market_blue_buybox_max_exchange=0.5;'
            'market_blue_buybox_max_price_rel=1000;market_blue_buybox_price_rel_max_threshold=1000;'
            'market_blue_buybox_max_price_diff=5000;market_blue_buybox_delivery_context_approx=0;'
            'market_blue_buybox_gmv_epsilon=0.001;market_blue_buybox_always_compute_approx_gmv_ue=1;'
            'market_blue_buybox_gmv_ue_mix_coef=0;market_blue_buybox_fbs_hybrid_local_warehouse_priority=0;&debug=da'
        )
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'wareId': 'BLUE-100010-FEED-1112g',
                'debug': {
                    'buyboxDebug': {
                        'WonMethod': 'WON_BY_EXCHANGE',
                        'Offers': [
                            {
                                'Conversion': 135.761,
                                'ConversionByDeliveryDayCoef': 1.13,
                                'DeliveryContext': {
                                    'CourierShopPrice': 15,
                                    'CourierDaysMin': 1,
                                    'CourierDaysMax': 2,
                                },
                                'DeltaGmv': 0,
                                'DeltaUe': 0,
                                'Exchange': 0,
                                'TransactionFee': 0.0200001,
                                'FeedID': 3333,
                                'Gmv': 21721.7,
                                'IsExactGmvUe': True,
                                'GmvUe': 21721.7,
                                'GmvUeRandomized': 21585.3,
                                'GmvUeToRelField': 14397,  # log2(21585.3 + 2) * 1000 = 14397.9
                                'IsCrossdock': False,
                                'IsFiltered': False,
                                'IsFulfillment': False,
                                'IsKGT': False,
                                'IsPreferred': False,
                                'IsUserCartWarehouse': False,
                                'IsWarehouseInUserCartCoef': 0.95,
                                'OfferId': 'blue.offer.3333q',
                                'OfferWeight': Round(0),
                                'PredictedElasticity': {
                                    'Value': 128,
                                    'Type': 'NORMAL',
                                },
                                'PromoBoostCoefficient': 1,
                                'PurchasePrice': 0,
                                'RelevanceData': {
                                    'Price': 160,
                                    'MinAllowedPrice': 160,
                                    'PriceBeforeDynamicStrategy': 0,
                                },
                                'ShopInUserRegionConversionCoef': 1,
                                'ShopOperationalRating': {
                                    'CalcTime': 0,
                                    'LateShipRate': 0.05,
                                    'CancellationRate': 0.011,
                                    'ReturnRate': 0.001,
                                    'Total': 0,
                                },
                                'ShopPriorityRegion': 213,
                                'ShopRatingCoef': 0.988011,
                                'SupplierId': 3333,
                                'SupplierType': 'ST_THIRD_PARTY',
                                'UeToPrice': -0.355,
                                'WareMd5': 'BLUE-100010-FEED-3333Q',
                                'WarehouseId': 145,
                                'WarehousePriority': 0,
                            }
                        ],
                    }
                },
            },
        )

    def test_prime(self):
        """Проверяем prime выдачу"""

        response = self.report.request_json(
            'place=prime&text=Сюрстреминг&rgb=blue&pp=1&rearr-factors=market_debug_buybox=1;market_blue_buybox_by_gmv_ue=1;market_blue_buybox_max_price_rel=1000&debug=da'
        )
        self.assertFragmentIn(
            response,
            {
                'entity': 'product',
                'offers': {
                    'count': 1,
                    'items': [
                        {
                            'entity': 'offer',
                            'wareId': 'BLUE-100003-FEED-1111Q',
                            'debug': {'buyboxDebug': {'WonMethod': 'WON_BY_EXCHANGE', 'RejectedOffers': []}},
                        }
                    ],
                },
            },
        )

    @classmethod
    def prepare_test_dsbs_output(cls):
        cls.index.shops += [
            Shop(fesh=4801, business_fesh=3, name="dsbs магазин Пети", regions=[213], cpa=Shop.CPA_REAL),
        ]

        cls.index.cpa_categories += [
            CpaCategory(hid=74421, regions=[213], cpa_type=CpaCategoryType.CPC_AND_CPA),
        ]

        cls.index.models += [
            Model(hid=74421, hyperid=654441),
        ]

        cls.index.offers += [
            Offer(
                title="DSBS Offer",
                hid=74421,
                hyperid=654441,
                price=121,
                fesh=4801,
                business_id=3,
                sku=200017,
                cpa=Offer.CPA_REAL,
                waremd5='sgf1xWYFqdGiLh4TT-lbqQ',
            )
        ]

        cls.index.mskus += [
            MarketSku(
                title="Тестируем наличие признака IsDsbs в выдаче",
                hyperid=654441,
                sku=200017,
                hid=74421,
                delivery_buckets=[1234],
                purchase_price=120.33,
                buybox_elasticity=sorted_elasticity,
                blue_offers=[
                    BlueOffer(price=250, feedid=3333, waremd5="BLUE-200017-FEED-3333g"),
                ],
            ),
        ]

    def test_dsbs_output(self):
        """Проверяем наличие признака IsDsbs в выдаче"""
        # place=prime&hyperid=654441&rgb=white&pp=1&rids=213&yandexuid=1&debug=da&allow-collapsing=1&use-default-offers=1
        response = self.report.request_json(
            'place=productoffers&market-sku=200017&rids=213&yandexuid=1&offers-set=defaultList&pp=6&debug=da'
        )
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'debug': {
                    'buyboxDebug': {
                        'Offers': [{'IsDsbs': True}],
                        'RejectedOffers': [{'Offer': {'IsDsbs': False}}],
                    }
                },
            },
        )

    def test_won_by_random_logging(self):
        """Проверяем наличие признаков IsWinnerByRandom и IsLoserByRandom в выдаче"""
        rearr_flags_str = (
            "market_blue_buybox_delivery_context_approx_use_shop_id=1;"
            + "market_blue_buybox_delivery_switch_type=3;"
            + "market_blue_buybox_disable_dsbs_pessimisation=1;"
            + "market_operational_rating_everywhere=1;"
            + "market_blue_buybox_1p_cancellation_rating_default=0.01;"
            + "market_blue_buybox_with_dsbs_white=1;"
            + "prefer_do_with_sku=1;"
            +
            # rel do fields flags:
            "market_blue_buybox_gmv_ue_mix_coef=1;"
            + "market_blue_buybox_use_gmv_in_rel_fields=1;"
            + "market_blue_buybox_gvm_ue_rand_low=0.99;"
            + "market_blue_buybox_gvm_ue_rand_delta=0.01;"
            + "market_blue_buybox_gvm_ue_to_rel_field_scaling_coef=1000;"
            + "market_blue_buybox_default_elasticity=1;"
            + "market_blue_buybox_filter_skip_always=0;"
            + "market_blue_buybox_filter_skip_if_product_offers=1;"
            + "market_blue_buybox_if_no_exact_gmv_use_do=0;"
            + "market_blue_buybox_if_no_exact_gmv_use_approx_gmv=0;"
            + "market_blue_buybox_if_no_exact_gmv_use_price=1;"
            + "market_blue_buybox_range_by_gmv_div_price=0;"
            + "market_default_offer_by_random=1"
        )  # choose DO by random (offers have same GMV)
        response = self.report.request_json(
            'place=productoffers&market-sku=100017&rids=213&rgb=green_with_blue&yandexuid=1&debug=da&rearr-factors=%s'
            % rearr_flags_str
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "wareId": "BLUE-100017-FEED-3333Q",
                        "debug": {
                            "buyboxDebug": {
                                "Offers": [
                                    {
                                        "WareMd5": "BLUE-100017-FEED-3333Q",
                                        "IsWinnerByRandom": True,
                                        "IsLoserByRandom": False,
                                    },
                                    {
                                        "WareMd5": "BLUE-100017-FEED-1112g",
                                        "IsWinnerByRandom": False,
                                        "IsLoserByRandom": True,
                                    },
                                ],
                            }
                        },
                    }
                ]
            },
        )
        self.show_log.expect(ware_md5="BLUE-100017-FEED-3333Q", is_winner_by_random=1)

    def test_split_conversion_for_medicines(self):
        """Проверяем, что флаг market_blue_buybox_split_conversion_for_medicines_coef работает"""

        """Проверяем на товаре из другой категории"""
        response = self.report.request_json(
            'place=productoffers&market-sku=100017&yandexuid=1&debug=da&rearr-factors='
            'market_blue_buybox_split_conversion_for_medicines_coef=0.93;'
            'market_blue_buybox_split_conversion_coef=0.95'
        )
        self.assertFragmentIn(
            response,
            {
                'debug': {
                    'buyboxDebug': {
                        'Offers': [
                            {
                                'IsWarehouseInUserCartCoef': 0.95,
                            },
                            {
                                'IsWarehouseInUserCartCoef': 0.95,
                            },
                        ]
                    }
                }
            },
        )

        """Проверяем на товаре из категории "Лекарственные препараты и Бады" """
        response = self.report.request_json(
            'place=productoffers&market-sku=100018&yandexuid=1&debug=da&rearr-factors='
            'market_blue_buybox_split_conversion_coef=0.95'
        )
        self.assertFragmentIn(
            response,
            {
                'debug': {
                    'buyboxDebug': {
                        'Offers': [
                            {
                                'IsWarehouseInUserCartCoef': 0.2,
                            },
                            {
                                'IsWarehouseInUserCartCoef': 0.2,
                            },
                        ]
                    }
                }
            },
        )


if __name__ == '__main__':
    main()
