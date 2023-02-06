#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    BlueOffer,
    DynamicDeliveryRestriction,
    DynamicWarehouseDelivery,
    DynamicWarehouseLink,
    Elasticity,
    MarketSku,
    Region,
    Shop,
    ShopOperationalRating,
    WarehouseWithPriority,
    WarehousesPriorityInRegion,
)
from core.testcase import TestCase, main


class T(TestCase):
    """
    Тестируем все случаи, когда байбокс переключается на старый режим
    """

    @classmethod
    def prepare(cls):
        cls.index.regiontree += [
            Region(rid=213, region_type=Region.CITY),
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
                fesh=31,
                datafeed_id=31,
                priority_region=213,
                regions=[225],
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

        _ = [
            Elasticity(price_variant=100, demand_mean=200),
            Elasticity(price_variant=200, demand_mean=80),
            Elasticity(price_variant=300, demand_mean=10),
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
        ]

        cls.index.mskus += [
            MarketSku(
                hyperid=2,
                sku=100002,
                hid=100,
                purchase_price=120.33,
                blue_offers=[
                    BlueOffer(price=200, feedid=1111, waremd5="BLUE-100002-FEED-1111Q"),
                    BlueOffer(price=210, feedid=2222, waremd5="BLUE-100002-FEED-2222g"),
                    BlueOffer(price=295, feedid=2222, waremd5="BLUE-100002-FEED-3333w"),
                ],
            ),
            MarketSku(
                hyperid=2,
                sku=100003,
                hid=100,
                buybox_elasticity=[
                    Elasticity(price_variant=180, demand_mean=200),
                    Elasticity(price_variant=200, demand_mean=80),
                    Elasticity(price_variant=220, demand_mean=10),
                ],
                purchase_price=120.33,
                blue_offers=[
                    BlueOffer(price=300, feedid=1111, waremd5="BLUE-100003-FEED-1111Q"),
                    BlueOffer(price=303, feedid=2222, waremd5="BLUE-100003-FEED-2222g"),
                    BlueOffer(price=305, feedid=2222, waremd5="BLUE-100003-FEED-3333w"),
                ],
            ),
            MarketSku(
                hyperid=2,
                sku=100004,
                hid=100,
                buybox_elasticity=[
                    Elasticity(price_variant=180, demand_mean=200),
                    Elasticity(price_variant=200, demand_mean=80),
                    Elasticity(price_variant=220, demand_mean=10),
                ],
                purchase_price=20.33,
                blue_offers=[
                    BlueOffer(price=80, feedid=1111, waremd5="BLUE-100004-FEED-1111Q"),
                    BlueOffer(price=81, feedid=2222, waremd5="BLUE-100004-FEED-2222g"),
                    BlueOffer(price=82, feedid=2222, waremd5="BLUE-100004-FEED-3333w"),
                ],
            ),
            MarketSku(
                hyperid=2,
                sku=100005,
                hid=100,
                buybox_elasticity=[
                    Elasticity(price_variant=180, demand_mean=200),
                    Elasticity(price_variant=200, demand_mean=80),
                    Elasticity(price_variant=220, demand_mean=10),
                ],
                purchase_price=120.33,
                blue_offers=[
                    BlueOffer(price=198, feedid=1111, waremd5="BLUE-100005-FEED-1111Q"),
                    BlueOffer(price=200, feedid=2222, waremd5="BLUE-100005-FEED-2222g"),
                    BlueOffer(price=202, feedid=2222, waremd5="BLUE-100005-FEED-3333w"),
                ],
            ),
            MarketSku(
                hyperid=2,
                sku=100006,
                hid=100,
                buybox_elasticity=[
                    Elasticity(price_variant=180, demand_mean=90),
                    Elasticity(price_variant=200, demand_mean=80),
                    Elasticity(price_variant=220, demand_mean=70),
                ],
                blue_offers=[
                    BlueOffer(price=198, feedid=1111, waremd5="BLUE-100006-FEED-1111Q"),
                    BlueOffer(price=200, feedid=2222, waremd5="BLUE-100006-FEED-2222g"),
                    BlueOffer(price=202, feedid=2222, waremd5="BLUE-100006-FEED-3333w"),
                ],
            ),
        ]

    @classmethod
    def prepare_nordstream(cls):
        cls.settings.nordstream_autogenerate = False
        for warehouse_id in [145, 147]:
            cls.dynamic.nordstream += [DynamicWarehouseLink(warehouse_id, [warehouse_id])]
            cls.dynamic.nordstream += [
                DynamicWarehouseDelivery(
                    warehouse_id,
                    {
                        225: [
                            DynamicDeliveryRestriction(min_days=1, max_days=2, cost=15),
                        ],
                    },
                ),
            ]

    def test_no_elasticity(self):
        """
        Тестируем переключение в старый режим байбокса, если у позиции нет эластичности
        """
        response = self.report.request_json(
            'place=productoffers&offers-set=defaultList&market-sku=100002&yandexuid=1&debug=da&'
            'rearr-factors=market_blue_buybox_disable_old_buybox_algo=0;market_blue_buybox_by_gmv_ue_with_delivery=0'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'wareId': 'BLUE-100002-FEED-1111Q',
                            'debug': {
                                'buyboxDebug': {
                                    'WonMethod': 'OLD_BUYBOX',
                                    'Offers': [
                                        {'WareMd5': 'BLUE-100002-FEED-1111Q'},
                                        {'WareMd5': 'BLUE-100002-FEED-2222g'},
                                    ],
                                    'RejectedOffers': [
                                        {
                                            'RejectReason': 'TOO_HIGH_PRICE',
                                            'Offer': {'WareMd5': 'BLUE-100002-FEED-3333w'},
                                        },
                                    ],
                                }
                            },
                        }
                    ]
                }
            },
        )
        self.assertFragmentIn(response, "NO_ELASTICITY for offer BLUE-100002-FEED-1111Q: old_algorithm_is_being_used")

    def test_no_elasticity_without_old_buybox(self):
        """
        Тестируем отсутствие переключения в старый режим байбокса, по флагу market_blue_buybox_disable_old_buybox_algo и отсутствию эластичности
        """
        response = self.report.request_json(
            'place=productoffers&offers-set=defaultList&market-sku=100002&yandexuid=1&debug=da&'
            'rearr-factors=market_blue_buybox_disable_old_buybox_algo=1;market_blue_buybox_by_gmv_ue_with_delivery=0'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'wareId': 'BLUE-100002-FEED-1111Q',
                            'debug': {
                                'buyboxDebug': {
                                    'WonMethod': 'WON_BY_EXCHANGE',
                                    'Offers': [
                                        {
                                            'WareMd5': 'BLUE-100002-FEED-1111Q',
                                            'RelevanceData': {'Price': 200},
                                            'PredictedElasticity': {'Value': 1, 'Type': 'DEFAULT'},
                                        },
                                        {
                                            'WareMd5': 'BLUE-100002-FEED-2222g',
                                            'RelevanceData': {'Price': 210},
                                            'PredictedElasticity': {
                                                'Value': 0.834311,
                                                'Type': 'DEFAULT',
                                            },  # (-0.2357 + 1.2357 * exp(-2.87938 * (-1. + 210/200)))
                                        },
                                    ],
                                    'RejectedOffers': [
                                        {
                                            'RejectReason': 'TOO_HIGH_PRICE',
                                            'Offer': {'WareMd5': 'BLUE-100002-FEED-3333w'},
                                        },
                                    ],
                                }
                            },
                        }
                    ]
                }
            },
        )
        self.assertFragmentNotIn(
            response, "NO_ELASTICITY for offer BLUE-100002-FEED-1111Q: old_algorithm_is_being_used"
        )

    def test_all_elasticities_right(self):
        """
        Тестируем переключение в старый режим байбокса, по правой эластичности
        """
        response = self.report.request_json(
            'place=productoffers&offers-set=defaultList&market-sku=100003&yandexuid=1&debug=da&'
            'rearr-factors=market_blue_buybox_disable_old_buybox_algo=0;market_blue_buybox_by_gmv_ue_with_delivery=0;'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'wareId': 'BLUE-100003-FEED-1111Q',
                            'debug': {
                                'buyboxDebug': {
                                    'WonMethod': 'OLD_BUYBOX',
                                    'Offers': [
                                        {'WareMd5': 'BLUE-100003-FEED-1111Q'},
                                        {'WareMd5': 'BLUE-100003-FEED-2222g'},
                                        {'WareMd5': 'BLUE-100003-FEED-3333w'},
                                    ],
                                }
                            },
                        }
                    ]
                }
            },
        )
        self.assertFragmentIn(response, "ALL_ELASTICITIES_RIGHT")
        self.assertFragmentIn(response, "old_algorithm_is_being_used")

    def test_all_elasticities_right_without_old_buybox(self):
        """
        Тестируем отсутствие переключения в старый режим байбокса, по флагу market_blue_buybox_disable_old_buybox_algo и правой эластичности
        """
        response = self.report.request_json(
            'place=productoffers&offers-set=defaultList&market-sku=100003&yandexuid=1&debug=da&'
            'rearr-factors=market_blue_buybox_use_old_randomization=1;market_blue_buybox_disable_old_buybox_algo=1;market_blue_buybox_by_gmv_ue_with_delivery=0;'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'wareId': 'BLUE-100003-FEED-1111Q',
                            'debug': {
                                'buyboxDebug': {
                                    'WonMethod': 'WON_BY_GMV',
                                    'Offers': [
                                        {'WareMd5': 'BLUE-100003-FEED-1111Q'},
                                        {'WareMd5': 'BLUE-100003-FEED-2222g'},
                                        {'WareMd5': 'BLUE-100003-FEED-3333w'},
                                    ],
                                }
                            },
                        }
                    ]
                }
            },
        )
        self.assertFragmentIn(response, "ALL_ELASTICITIES_RIGHT")
        self.assertFragmentNotIn(response, "old_algorithm_is_being_used")

    def test_all_elasticities_left(self):
        """
        Тестируем переключение в старый режим байбокса, по левой эластичности
        """
        response = self.report.request_json(
            'place=productoffers&offers-set=defaultList&market-sku=100004&yandexuid=1&debug=da&'
            'rearr-factors=market_blue_buybox_disable_old_buybox_algo=0;market_blue_buybox_by_gmv_ue_with_delivery=0;'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'wareId': 'BLUE-100004-FEED-1111Q',
                            'debug': {
                                'buyboxDebug': {
                                    'WonMethod': 'OLD_BUYBOX',
                                    'Offers': [
                                        {'WareMd5': 'BLUE-100004-FEED-1111Q'},
                                        {'WareMd5': 'BLUE-100004-FEED-2222g'},
                                        {'WareMd5': 'BLUE-100004-FEED-3333w'},
                                    ],
                                }
                            },
                        }
                    ]
                }
            },
        )
        self.assertFragmentIn(response, "ALL_ELASTICITIES_LEFT. Is advertisement buybox: 0")
        self.assertFragmentIn(response, "old_algorithm_is_being_used")

    def test_all_elasticities_left_without_old_buybox(self):
        """
        Тестируем отсутствие переключения в старый режим байбокса, по флагу market_blue_buybox_disable_old_buybox_algo и левой эластичности
        """
        response = self.report.request_json(
            'place=productoffers&offers-set=defaultList&market-sku=100004&yandexuid=1&debug=da&'
            'rearr-factors=market_blue_buybox_use_old_randomization=1;market_blue_buybox_disable_old_buybox_algo=1;market_blue_buybox_by_gmv_ue_with_delivery=0;'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'wareId': 'BLUE-100004-FEED-1111Q',
                            'debug': {
                                'buyboxDebug': {
                                    'WonMethod': 'WON_BY_GMV',
                                    'Offers': [
                                        {'WareMd5': 'BLUE-100004-FEED-1111Q'},
                                        {'WareMd5': 'BLUE-100004-FEED-2222g'},
                                        {'WareMd5': 'BLUE-100004-FEED-3333w'},
                                    ],
                                }
                            },
                        }
                    ]
                }
            },
        )
        self.assertFragmentIn(response, "ALL_ELASTICITIES_LEFT. Is advertisement buybox: 0")
        self.assertFragmentNotIn(response, "old_algorithm_is_being_used")

    def test_delivery(self):
        """
        Тестируем переключение в старый режим байбокса, по отсутствию доставки
        """
        response = self.report.request_json(
            'place=productoffers&offers-set=defaultList&market-sku=100005&yandexuid=1&debug=da&'
            'rearr-factors=market_blue_buybox_disable_old_buybox_algo=0;market_blue_buybox_by_gmv_ue_with_delivery=1;'
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'wareId': 'BLUE-100005-FEED-1111Q',
                            'debug': {
                                'buyboxDebug': {
                                    'WonMethod': 'OLD_BUYBOX',
                                    'Offers': [
                                        {'WareMd5': 'BLUE-100005-FEED-1111Q'},
                                        {'WareMd5': 'BLUE-100005-FEED-2222g'},
                                        {'WareMd5': 'BLUE-100005-FEED-3333w'},
                                    ],
                                }
                            },
                        }
                    ]
                }
            },
        )
        self.assertFragmentIn(
            response,
            "countOfOffersWithDelivery == 0 && Ctx.SortingParams.getExp().BuyboxByGmvUeWithDelivery: old_algorithm_is_being_used",
        )


if __name__ == '__main__':
    main()
