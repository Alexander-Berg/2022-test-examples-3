#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    BlueOffer,
    DynamicDeliveryRestriction,
    DynamicWarehouseDelivery,
    DynamicWarehouseLink,
    Elasticity,
    HyperCategory,
    MarketSku,
    Region,
    Shop,
    ShopOperationalRating,
    WarehouseWithPriority,
    WarehousesPriorityInRegion,
    WarehouseToRegions,
)
from core.testcase import TestCase, main

CEHAC_CATEG_ID = 91063  # товар категории CEHAC(Consumer Electronics, Home Appliance and Computers)


class T(TestCase):
    """
    Тестируем отсечение офферов в байбоксе
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
                business_fesh=3,
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
                business_fesh=3,
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
                business_fesh=4,
            ),
            Shop(
                fesh=32,
                datafeed_id=32,
                priority_region=213,
                regions=[225],
                name="3P поставщик Петя",
                supplier_type=Shop.THIRD_PARTY,
                is_supplier=True,
                blue=Shop.BLUE_REAL,
                warehouse_id=145,
                fulfillment_program=True,
                business_fesh=4,
            ),
            Shop(
                fesh=33,
                datafeed_id=33,
                priority_region=213,
                regions=[225],
                name="3P поставщик Вова",
                supplier_type=Shop.THIRD_PARTY,
                is_supplier=True,
                blue=Shop.BLUE_REAL,
                warehouse_id=145,
                fulfillment_program=True,
                business_fesh=5,
            ),
            Shop(
                fesh=34,
                datafeed_id=34,
                priority_region=65,
                regions=[225],
                name="3P поставщик Дима из Новосиба",
                supplier_type=Shop.THIRD_PARTY,
                is_supplier=True,
                blue=Shop.BLUE_REAL,
                warehouse_id=303,
                fulfillment_program=True,
                business_fesh=5,
            ),
            Shop(
                fesh=41,
                datafeed_id=41,
                priority_region=213,
                regions=[225],
                name="dropship магазин Пети-41",
                supplier_type=Shop.THIRD_PARTY,
                is_supplier=True,
                blue=Shop.BLUE_REAL,
                warehouse_id=401,
                fulfillment_program=False,
                business_fesh=6,
            ),
            Shop(
                fesh=42,
                datafeed_id=42,
                priority_region=65,
                name="dropship магазин Пети-42",
                regions=[225],
                supplier_type=Shop.THIRD_PARTY,
                is_supplier=True,
                blue=Shop.BLUE_REAL,
                warehouse_id=402,
                fulfillment_program=False,
                business_fesh=6,
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

        sorted_elasticity = [
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
            )
        ]

        cls.index.warehouse_to_regions += [
            WarehouseToRegions(region_id=65, warehouse_id=303),
            WarehouseToRegions(region_id=213, warehouse_id=401),
            WarehouseToRegions(region_id=65, warehouse_id=402),
        ]

        cls.index.mskus += [
            MarketSku(
                hyperid=2,
                sku=100002,
                hid=100,
                buybox_elasticity=sorted_elasticity,
                purchase_price=120.33,
                blue_offers=[
                    BlueOffer(price=200, feedid=1111, waremd5="BLUE-100002-FEED-1111Q"),
                    BlueOffer(price=210, feedid=2222, waremd5="BLUE-100002-FEED-2222g"),
                    BlueOffer(price=215, feedid=2222, waremd5="BLUE-100002-FEED-3333w"),
                ],
            ),
            MarketSku(
                hyperid=2,
                sku=100003,
                hid=100,
                buybox_elasticity=[
                    Elasticity(price_variant=90000, demand_mean=1),
                    Elasticity(price_variant=100000, demand_mean=0.8),
                    Elasticity(price_variant=110000, demand_mean=0.6),
                ],
                purchase_price=40000,
                blue_offers=[
                    BlueOffer(price=100000, feedid=1111, waremd5="BLUE-100003-FEED-1111Q"),
                    BlueOffer(price=97000, feedid=2222, waremd5="BLUE-100003-FEED-2222g"),
                    BlueOffer(price=96000, feedid=2222, waremd5="BLUE-100003-FEED-3333w"),
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
                purchase_price=120.33,
                blue_offers=[
                    BlueOffer(price=199, feedid=1111, waremd5="BLUE-100004-FEED-1111Q"),
                    BlueOffer(price=200, feedid=2222, waremd5="BLUE-100004-FEED-2222g"),
                    BlueOffer(price=208, feedid=2222, waremd5="BLUE-100004-FEED-3333w"),
                ],
            ),
            MarketSku(
                hyperid=3,
                sku=100005,
                hid=CEHAC_CATEG_ID,  # товар категории CEHAC
                buybox_elasticity=[
                    Elasticity(price_variant=180, demand_mean=200),
                    Elasticity(price_variant=200, demand_mean=80),
                    Elasticity(price_variant=220, demand_mean=10),
                ],
                purchase_price=120.33,
                blue_offers=[
                    BlueOffer(price=199, feedid=2222, waremd5="BLUE-100005-FEED-1111Q"),
                    BlueOffer(price=200, feedid=2222, waremd5="BLUE-100005-FEED-2222g"),
                    BlueOffer(price=1000, feedid=2222, waremd5="BLUE-100005-FEED-3333w"),
                ],
            ),
            MarketSku(
                hyperid=4,
                sku=100006,
                hid=105,
                blue_offers=[
                    BlueOffer(price=200, feedid=31, waremd5="BLUE-100006-FEED-1111Q", business_id=4),
                    BlueOffer(price=195, feedid=31, waremd5="BLUE-100006-FEED-1112Q", business_id=4),
                    BlueOffer(price=180, feedid=32, waremd5="BLUE-100006-FEED-2221Q", business_id=4),
                    BlueOffer(price=165, feedid=32, waremd5="BLUE-100006-FEED-2222Q", business_id=4),
                    BlueOffer(price=160, feedid=33, waremd5="BLUE-100006-FEED-3331Q", business_id=5),
                    BlueOffer(price=155, feedid=33, waremd5="BLUE-100006-FEED-3332Q", business_id=5),
                ],
            ),
            MarketSku(
                hyperid=5,
                sku=100007,
                hid=106,
                buybox_elasticity=sorted_elasticity,
                blue_offers=[
                    BlueOffer(price=200, feedid=31, waremd5="BLUE-100007-FEED-1111Q", business_id=4),
                    BlueOffer(price=195, feedid=31, waremd5="BLUE-100007-FEED-1112Q", business_id=4),
                    BlueOffer(price=180, feedid=32, waremd5="BLUE-100007-FEED-2221Q", business_id=4),
                    BlueOffer(price=165, feedid=32, waremd5="BLUE-100007-FEED-2222Q", business_id=4),
                    BlueOffer(price=150, feedid=33, waremd5="BLUE-100007-FEED-3331Q", business_id=5),
                    BlueOffer(price=145, feedid=33, waremd5="BLUE-100007-FEED-3332Q", business_id=5),
                    BlueOffer(price=160, feedid=34, waremd5="BLUE-100007-FEED-3334Q", business_id=5),
                    BlueOffer(price=155, feedid=34, waremd5="BLUE-100007-FEED-3335Q", business_id=5),
                    BlueOffer(price=200, feedid=41, waremd5="FBS1_1_200_WITH_DELIVw", business_id=6),
                    BlueOffer(price=300, feedid=42, waremd5="FBS2_2_300_WITH_DELIVw", business_id=6),
                ],
            ),
        ]

        cls.index.hypertree += [HyperCategory(hid=CEHAC_CATEG_ID)]

    @classmethod
    def prepare_nordstream(cls):
        cls.settings.nordstream_autogenerate = False
        for warehouse_id in [145, 147, 303, 401, 402]:
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

    def test_high_price_1(self):
        """
        Тестируем отсечение оффера по цене.
        Цена оффера выше более, чем на 5%, чем цена минимального оффера
        """
        response = self.report.request_json(
            'place=productoffers&offers-set=defaultList&_offers&market-sku=100002&yandexuid=1&debug=da&rearr-factors=market_blue_buybox_max_price_rel_add_diff=0;market_blue_buybox_by_gmv_ue_with_delivery=0'  # noqa
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
                                        {'WareMd5': 'BLUE-100002-FEED-1111Q'},
                                        {'WareMd5': 'BLUE-100002-FEED-2222g'},
                                    ],
                                    'RejectedOffers': [
                                        {
                                            'RejectReason': 'TOO_HIGH_PRICE',
                                            'Offer': {'WareMd5': 'BLUE-100002-FEED-3333w'},
                                        },
                                    ],
                                    'Settings': {'MaxPriceRel': 1.05},
                                }
                            },
                        }
                    ]
                }
            },
        )

    def test_high_price_2(self):
        """
        Тестируем отсечение оффера по цене.
        Тот же тест, что и тест 1, но через факторы изменяем порог отсечения, и оффер больше не отсекается
        """
        response = self.report.request_json(
            'place=productoffers&offers-set=defaultList&_offers&market-sku=100002&yandexuid=1&debug=da&rearr-factors=market_debug_buybox=1;market_blue_buybox_by_gmv_ue_with_delivery=0;market_blue_buybox_max_price_rel=1.1'  # noqa
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
                                    'Offers': [
                                        {'WareMd5': 'BLUE-100002-FEED-1111Q'},
                                        {'WareMd5': 'BLUE-100002-FEED-2222g'},
                                        {'WareMd5': 'BLUE-100002-FEED-3333w'},
                                    ],
                                    'Settings': {'MaxPriceRel': 1.1},
                                }
                            },
                        }
                    ]
                }
            },
        )

        # Понижаем цену оффера BLUE-100003-FEED-1111Q до 96100, чтобы он перестал фильтроваться по порогу в 5%
        # Также, незначительно меняем цену у оффера BLUE-100003-FEED-2222g на 97001, чтобы проверить, что флаг работает на нескольких офферах
        rearr_price_list = (
            'market_specified_offer_price_list='
            'ware_md5=BLUE-100003-FEED-1111Q:price=96100,'
            'ware_md5=BLUE-100003-FEED-2222g:price=97001'
        )
        response = self.report.request_json(
            'place=productoffers&offers-set=defaultList&_offers&market-sku=100003&yandexuid=1&debug=da'
            '&rearr-factors={};market_debug_buybox=1;market_blue_buybox_by_gmv_ue_with_delivery=0'.format(
                rearr_price_list
            )
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'wareId': 'BLUE-100003-FEED-3333w',
                            'debug': {
                                'buyboxDebug': {
                                    'WonMethod': 'WON_BY_EXCHANGE',
                                    'Offers': [
                                        {'WareMd5': 'BLUE-100003-FEED-2222g', 'PriceAfterCashback': 97001},
                                        {'WareMd5': 'BLUE-100003-FEED-3333w', 'PriceAfterCashback': 96000},
                                        {'WareMd5': 'BLUE-100003-FEED-1111Q', 'PriceAfterCashback': 96100},
                                    ],
                                    'Settings': {'MaxPriceDiff': 3000},
                                }
                            },
                        }
                    ]
                }
            },
        )

    def test_high_price_for_cehac(self):
        """
        Тестируем отсечение оффера по цене для товаров категории CEHAC(Consumer Electronics, Home Appliance and Computers).
        """
        # Тестируем до включения флага market_blue_buybox_max_price_rel_for_cehac. Оффер с очень большой ценой фильтруется
        response = self.report.request_json(
            'place=productoffers&offers-set=defaultList&_offers&market-sku=100005&yandexuid=1&debug=da&rearr-factors=market_debug_buybox=1;market_blue_buybox_by_gmv_ue_with_delivery=0'
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
                                    'WonMethod': 'WON_BY_EXCHANGE',
                                    'Offers': [
                                        {'WareMd5': 'BLUE-100005-FEED-2222g'},
                                    ],
                                    'RejectedOffers': [
                                        {
                                            'RejectReason': 'TOO_HIGH_PRICE',
                                            'Offer': {'WareMd5': 'BLUE-100005-FEED-3333w'},
                                        },
                                    ],
                                    'Settings': {'MaxPriceRel': 1.05},
                                }
                            },
                        }
                    ]
                }
            },
        )

        # Включаем флаг market_blue_buybox_max_price_rel_for_cehac=10 что позволяет не фильтровать предложения с завышением цены в десять раз
        response = self.report.request_json(
            'place=productoffers&offers-set=defaultList&_offers&market-sku=100005&yandexuid=1&debug=da&rearr-factors=market_blue_buybox_by_gmv_ue_with_delivery=0;market_blue_buybox_max_price_rel_for_cehac=10;market_blue_buybox_price_rel_max_threshold=10'  # noqa
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
                                    'WonMethod': 'WON_BY_EXCHANGE',
                                    'Offers': [
                                        {'WareMd5': 'BLUE-100005-FEED-2222g'},
                                        {'WareMd5': 'BLUE-100005-FEED-3333w'},
                                    ],
                                    'Settings': {'MaxPriceRel': 10},
                                }
                            },
                        }
                    ]
                }
            },
        )

        # Проверяем, что включение флага market_blue_buybox_max_price_rel_for_cehac не ломает товары других категорий
        response = self.report.request_json(
            'place=productoffers&offers-set=defaultList&_offers&market-sku=100002&yandexuid=1&debug=da&rearr-factors=market_debug_buybox=1;market_blue_buybox_by_gmv_ue_with_delivery=0;market_blue_buybox_max_price_rel=1.1;market_blue_buybox_max_price_rel_for_cehac=10'  # noqa
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
                                    'Offers': [
                                        {'WareMd5': 'BLUE-100002-FEED-1111Q'},
                                        {'WareMd5': 'BLUE-100002-FEED-2222g'},
                                        {'WareMd5': 'BLUE-100002-FEED-3333w'},
                                    ],
                                    'Settings': {'MaxPriceRel': 1.1},
                                }
                            },
                        }
                    ]
                }
            },
        )

    def test_high_price_4(self):
        """
        Тестируем отсечение оффера по цене.
        Тот же тест, что и тест 3, но через факторы изменяем порог отсечения, и оффер больше не отсекается и выигрывает байбокс
        """
        response = self.report.request_json(
            'place=productoffers&offers-set=defaultList&_offers&market-sku=100003&yandexuid=1&debug=da&rearr-factors=market_debug_buybox=1;market_blue_buybox_by_gmv_ue_with_delivery=0;market_blue_buybox_max_price_diff=5000'  # noqa
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'wareId': 'BLUE-100003-FEED-3333w',
                            'debug': {
                                'buyboxDebug': {
                                    'WonMethod': 'WON_BY_EXCHANGE',
                                    'Offers': [
                                        {'WareMd5': 'BLUE-100003-FEED-1111Q'},
                                        {'WareMd5': 'BLUE-100003-FEED-2222g'},
                                        {'WareMd5': 'BLUE-100003-FEED-3333w'},
                                    ],
                                    'Settings': {'MaxPriceDiff': 5000},
                                }
                            },
                        }
                    ]
                }
            },
        )

    def test_price_filter_by_supplier(self):
        """
        Есть 6 офферов из одного msku но разных поставшиков.
        1) Supplier 33
        BLUE-100006-FEED-3333Q
        BLUE-100006-FEED-3333W
        Разница в ценах 160-155=5 - оба оффера проходят
        2) Supplier 32
        BLUE-100006-FEED-2222Q
        BLUE-100006-FEED-2222W
        Разница в ценах 180-165=15 - проходит только один оффер
        3) Supplier 31
        BLUE-100006-FEED-1111
        BLUE-100006-FEED-1111W
        Разница в ценах 200-195=5 - оба оффера проходят
        """
        response = self.report.request_json(
            'place=productoffers&hyperid=4&hid=105&rearr-factors=market_blue_buybox_disable_old_buybox_algo=0;market_uncollapse_supplier=1;market_enable_buybox_by_business=0;market_blue_buybox_max_price_rel_add_diff=0;&debug=da'  # noqa
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 3,
                    'results': [
                        {
                            'wareId': 'BLUE-100006-FEED-1112Q',
                            'debug': {
                                'buyboxDebug': {
                                    'WonMethod': 'OLD_BUYBOX',
                                    'Offers': [
                                        {
                                            'WareMd5': 'BLUE-100006-FEED-1111Q',
                                            'PriceAfterCashback': 200,
                                            'BuyboxFilter': 'BUYBOX_BY_SUPPLIER',
                                        },
                                        {
                                            'WareMd5': 'BLUE-100006-FEED-1112Q',
                                            'PriceAfterCashback': 195,
                                            'BuyboxFilter': 'BUYBOX_BY_SUPPLIER',
                                        },
                                    ],
                                }
                            },
                        },
                        {
                            'wareId': 'BLUE-100006-FEED-2222Q',
                            'debug': {
                                'buyboxDebug': {
                                    'WonMethod': 'SINGLE_OFFER_AFTER_BUYBOX_FILTERS',
                                    'Offers': [
                                        {
                                            'WareMd5': 'BLUE-100006-FEED-2222Q',
                                            'PriceAfterCashback': 165,
                                            'BuyboxFilter': 'BUYBOX_BY_SUPPLIER',
                                        },
                                    ],
                                }
                            },
                        },
                        {
                            'wareId': 'BLUE-100006-FEED-3332Q',
                            'debug': {
                                'buyboxDebug': {
                                    'WonMethod': 'OLD_BUYBOX',
                                    'Offers': [
                                        {
                                            'WareMd5': 'BLUE-100006-FEED-3331Q',
                                            'PriceAfterCashback': 160,
                                            'BuyboxFilter': 'BUYBOX_BY_SUPPLIER',
                                        },
                                        {
                                            'WareMd5': 'BLUE-100006-FEED-3332Q',
                                            'PriceAfterCashback': 155,
                                            'BuyboxFilter': 'BUYBOX_BY_SUPPLIER',
                                        },
                                    ],
                                }
                            },
                        },
                    ],
                }
            },
        )

    def test_price_filter_by_business(self):
        """
        Есть 6 офферов из одного msku но от разных бизнесов.
        1) Business 5
        BLUE-100006-FEED-3333Q
        BLUE-100006-FEED-3333W
        Разница в ценах 160-155=5 - оба оффера проходят
        2) Business 4
        BLUE-100006-FEED-1111Q
        BLUE-100006-FEED-1111W
        BLUE-100006-FEED-2222Q
        BLUE-100006-FEED-2222W
        Разница в ценах в самых дешевых офферов 180-165=15 - проходит только один оффер
        """
        response = self.report.request_json(
            'place=productoffers&hyperid=4&hid=105&grhow=supplier&rearr-factors=market_blue_buybox_disable_old_buybox_algo=0;market_uncollapse_supplier=1;enable_business_id=1;market_enable_buybox_by_business=1;market_blue_buybox_max_price_rel_add_diff=0;&debug=da'  # noqa
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'total': 2,
                    'results': [
                        {
                            'wareId': 'BLUE-100006-FEED-2222Q',
                            'bundled': {'count': 2},
                            'debug': {
                                'buyboxDebug': {
                                    'WonMethod': 'SINGLE_OFFER_AFTER_BUYBOX_FILTERS',
                                    'Offers': [
                                        {
                                            'WareMd5': 'BLUE-100006-FEED-2222Q',
                                            'PriceAfterCashback': 165,
                                            'BuyboxFilter': 'BUYBOX_BY_BUSINESS',
                                        },
                                    ],
                                }
                            },
                        },
                        {
                            'wareId': 'BLUE-100006-FEED-3332Q',
                            'bundled': {'count': 1},
                            'debug': {
                                'buyboxDebug': {
                                    'WonMethod': 'OLD_BUYBOX',
                                    'Offers': [
                                        {
                                            'WareMd5': 'BLUE-100006-FEED-3331Q',
                                            'PriceAfterCashback': 160,
                                            'BuyboxFilter': 'BUYBOX_BY_BUSINESS',
                                        },
                                        {
                                            'WareMd5': 'BLUE-100006-FEED-3332Q',
                                            'PriceAfterCashback': 155,
                                            'BuyboxFilter': 'BUYBOX_BY_BUSINESS',
                                        },
                                    ],
                                }
                            },
                        },
                    ],
                }
            },
        )

    def test_local_warehouse_priority(self):
        """
        Есть 8 офферов из одного msku но от разных бизнесов.
        1) Business 5
        BLUE-100007-FEED-3331Q
        BLUE-100007-FEED-3332Q
        BLUE-100007-FEED-3334Q  -- local warehouse
        BLUE-100007-FEED-3335Q  -- local warehouse
        Разница в ценах 160-155=5 - оба оффера проходят
        2) Business 4
        BLUE-100007-FEED-1111Q
        BLUE-100007-FEED-1112Q
        BLUE-100007-FEED-2221Q
        BLUE-100007-FEED-2222Q
        3) Business 40
        FBS1_1_200_WITH_DELIVw
        FBS2_2_300_WITH_DELIVw -- local warehouse

        Разница в ценах в самых дешевых офферов 180-165=15 - проходит только один оффер
        """

        # Проверяем, что при выключенных флагах market_blue_buybox_local_warehouse_priority_for_business и market_blue_buybox_fbs_local_warehouse_priority
        # байбокс работает как и раньше. Дорогие оффера отфильтровываются
        response = self.report.request_json(
            'place=productoffers&rids=65&pp=6&offers-set=defaultList,top&market-sku=100007&grhow=supplier&rearr-factors=market_hide_regional_delimiter=1;market_blue_buybox_fbs_local_warehouse_priority=0;market_blue_buybox_local_warehouse_priority_for_business=0;market_load_boost_locality_from_fb_file=0;market_blue_buybox_disable_old_buybox_algo=0;market_uncollapse_supplier=1;enable_business_id=1;market_enable_buybox_by_business=1;market_blue_buybox_max_price_rel_add_diff=0;market_blue_buybox_fbs_hybrid_local_warehouse_priority=0;&debug=da'  # noqa
        )

        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'wareId': 'BLUE-100007-FEED-3331Q',
                            'bundled': {'count': 2},
                            'debug': {
                                'buyboxDebug': {
                                    'WonMethod': 'WON_BY_EXCHANGE',
                                    'Offers': [
                                        {'WareMd5': 'BLUE-100007-FEED-3331Q', 'BuyboxFilter': 'BUYBOX_BY_BUSINESS'},
                                        {'WareMd5': 'BLUE-100007-FEED-3332Q', 'BuyboxFilter': 'BUYBOX_BY_BUSINESS'},
                                    ],
                                    'RejectedOffers': [
                                        {
                                            'RejectReason': 'TOO_HIGH_PRICE',
                                            'Offer': {'WareMd5': "BLUE-100007-FEED-3334Q"},
                                        },
                                        {
                                            'RejectReason': 'TOO_HIGH_PRICE',
                                            'Offer': {'WareMd5': "BLUE-100007-FEED-3335Q"},
                                        },
                                    ],
                                }
                            },
                        },
                        {
                            'wareId': 'BLUE-100007-FEED-2222Q',
                            'bundled': {'count': 2},
                            'debug': {
                                'buyboxDebug': {
                                    'WonMethod': 'SINGLE_OFFER_AFTER_BUYBOX_FILTERS',
                                    'Offers': [
                                        {'WareMd5': 'BLUE-100007-FEED-2222Q', 'BuyboxFilter': 'BUYBOX_BY_BUSINESS'},
                                    ],
                                    'RejectedOffers': [
                                        {
                                            'RejectReason': 'TOO_HIGH_PRICE',
                                            'Offer': {'WareMd5': "BLUE-100007-FEED-1111Q"},
                                        },
                                        {
                                            'RejectReason': 'TOO_HIGH_PRICE',
                                            'Offer': {'WareMd5': "BLUE-100007-FEED-1112Q"},
                                        },
                                        {
                                            'RejectReason': 'TOO_HIGH_PRICE',
                                            'Offer': {'WareMd5': "BLUE-100007-FEED-2221Q"},
                                        },
                                    ],
                                }
                            },
                        },
                        {
                            'wareId': 'FBS1_1_200_WITH_DELIVw',
                            'bundled': {'count': 2},
                            'debug': {
                                'buyboxDebug': {
                                    'WonMethod': 'SINGLE_OFFER_AFTER_BUYBOX_FILTERS',
                                    'Offers': [
                                        {'WareMd5': 'FBS1_1_200_WITH_DELIVw', 'BuyboxFilter': 'BUYBOX_BY_BUSINESS'},
                                    ],
                                    'RejectedOffers': [
                                        {
                                            'RejectReason': 'TOO_HIGH_PRICE',
                                            'Offer': {'WareMd5': "FBS2_2_300_WITH_DELIVw"},
                                        },
                                    ],
                                }
                            },
                        },
                        {
                            'wareId': 'BLUE-100007-FEED-3331Q',
                            'benefit': {'type': 'cheapest'},
                            'debug': {
                                'buyboxDebug': {
                                    'WonMethod': 'WON_BY_EXCHANGE',
                                    'Offers': [
                                        {'WareMd5': 'BLUE-100007-FEED-3331Q', 'BuyboxFilter': 'BUYBOX_ONLY'},
                                        {'WareMd5': 'BLUE-100007-FEED-3332Q', 'BuyboxFilter': 'BUYBOX_ONLY'},
                                    ],
                                    'RejectedOffers': [
                                        {
                                            'RejectReason': 'TOO_HIGH_PRICE',
                                            'Offer': {'WareMd5': "BLUE-100007-FEED-1111Q"},
                                        },
                                        {
                                            'RejectReason': 'TOO_HIGH_PRICE',
                                            'Offer': {'WareMd5': "BLUE-100007-FEED-1112Q"},
                                        },
                                        {
                                            'RejectReason': 'TOO_HIGH_PRICE',
                                            'Offer': {'WareMd5': "BLUE-100007-FEED-2221Q"},
                                        },
                                        {
                                            'RejectReason': 'TOO_HIGH_PRICE',
                                            'Offer': {'WareMd5': "BLUE-100007-FEED-2222Q"},
                                        },
                                        {
                                            'RejectReason': 'TOO_HIGH_PRICE',
                                            'Offer': {'WareMd5': "BLUE-100007-FEED-3334Q"},
                                        },
                                        {
                                            'RejectReason': 'TOO_HIGH_PRICE',
                                            'Offer': {'WareMd5': "BLUE-100007-FEED-3335Q"},
                                        },
                                        {
                                            'Offer': {'WareMd5': "FBS1_1_200_WITH_DELIVw"},
                                        },
                                        {
                                            'Offer': {'WareMd5': "FBS2_2_300_WITH_DELIVw"},
                                        },
                                    ],
                                }
                            },
                        },
                    ],
                }
            },
        )
        self.show_log.expect(ware_md5="BLUE-100007-FEED-3331Q", pp=200, is_local_warehouse=1)
        # Проверяем, что при включенных флагах "market_blue_buybox_local_warehouse_priority_for_business=1;market_blue_buybox_fbs_local_warehouse_priority=1"
        # нелокальные оффера для business_id 5 и 40 фильтруются, а все остальные оффера не поменяли свое поведение
        response = self.report.request_json(
            'place=productoffers&rids=65&pp=6&offers-set=defaultList,top&market-sku=100007&grhow=supplier&rearr-factors=market_hide_regional_delimiter=1;market_blue_buybox_fbs_local_warehouse_priority=1;market_load_boost_locality_from_fb_file=0;market_blue_buybox_disable_old_buybox_algo=0;market_uncollapse_supplier=1;enable_business_id=1;market_enable_buybox_by_business=1;market_blue_buybox_max_price_rel_add_diff=0;market_blue_buybox_fbs_hybrid_local_warehouse_priority=0;&debug=da'  # noqa
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'wareId': 'BLUE-100007-FEED-3335Q',
                            'debug': {
                                'buyboxDebug': {
                                    'WonMethod': 'WON_BY_EXCHANGE',
                                    'Offers': [
                                        {
                                            'WareMd5': 'BLUE-100007-FEED-3334Q',
                                            'PriceAfterCashback': 160,
                                            'BuyboxFilter': 'BUYBOX_BY_BUSINESS',
                                        },
                                        {
                                            'WareMd5': 'BLUE-100007-FEED-3335Q',
                                            'PriceAfterCashback': 155,
                                            'BuyboxFilter': 'BUYBOX_BY_BUSINESS',
                                        },
                                    ],
                                    'RejectedOffers': [
                                        {
                                            'RejectReason': 'LOCAL_WAREHOUSE_PRIORITY',
                                            'Offer': {'WareMd5': "BLUE-100007-FEED-3331Q"},
                                        },
                                        {
                                            'RejectReason': 'LOCAL_WAREHOUSE_PRIORITY',
                                            'Offer': {'WareMd5': "BLUE-100007-FEED-3332Q"},
                                        },
                                    ],
                                }
                            },
                        },
                        {
                            'wareId': 'BLUE-100007-FEED-2222Q',
                            'debug': {
                                'buyboxDebug': {
                                    'WonMethod': 'SINGLE_OFFER_AFTER_BUYBOX_FILTERS',
                                    'Offers': [
                                        {'PriceAfterCashback': 165, 'BuyboxFilter': 'BUYBOX_BY_BUSINESS'},
                                    ],
                                    'RejectedOffers': [
                                        {
                                            'RejectReason': 'TOO_HIGH_PRICE',
                                            'Offer': {'WareMd5': "BLUE-100007-FEED-1111Q"},
                                        },
                                        {
                                            'RejectReason': 'TOO_HIGH_PRICE',
                                            'Offer': {'WareMd5': "BLUE-100007-FEED-1112Q"},
                                        },
                                        {
                                            'RejectReason': 'TOO_HIGH_PRICE',
                                            'Offer': {'WareMd5': "BLUE-100007-FEED-2221Q"},
                                        },
                                    ],
                                }
                            },
                        },
                        {
                            'wareId': 'FBS2_2_300_WITH_DELIVw',
                            'debug': {
                                'buyboxDebug': {
                                    'WonMethod': 'SINGLE_OFFER_AFTER_BUYBOX_FILTERS',
                                    'Offers': [
                                        {'WareMd5': 'FBS2_2_300_WITH_DELIVw', 'BuyboxFilter': 'BUYBOX_BY_BUSINESS'},
                                    ],
                                    'RejectedOffers': [
                                        {
                                            'RejectReason': 'LOCAL_WAREHOUSE_PRIORITY',
                                            'Offer': {'WareMd5': "FBS1_1_200_WITH_DELIVw"},
                                        },
                                    ],
                                }
                            },
                        },
                        {
                            'wareId': 'BLUE-100007-FEED-3335Q',
                            'benefit': {'type': 'cheapest'},
                            'debug': {
                                'buyboxDebug': {
                                    'WonMethod': 'WON_BY_EXCHANGE',
                                    'Offers': [
                                        {'WareMd5': 'BLUE-100007-FEED-3334Q', 'BuyboxFilter': 'BUYBOX_ONLY'},
                                        {'WareMd5': 'BLUE-100007-FEED-3335Q', 'BuyboxFilter': 'BUYBOX_ONLY'},
                                    ],
                                    'RejectedOffers': [
                                        {
                                            'RejectReason': 'TOO_HIGH_PRICE',
                                            'Offer': {'WareMd5': "BLUE-100007-FEED-1111Q"},
                                        },
                                        {
                                            'RejectReason': 'TOO_HIGH_PRICE',
                                            'Offer': {'WareMd5': "BLUE-100007-FEED-1112Q"},
                                        },
                                        {
                                            'RejectReason': 'TOO_HIGH_PRICE',
                                            'Offer': {'WareMd5': "BLUE-100007-FEED-2221Q"},
                                        },
                                        {
                                            'RejectReason': 'TOO_HIGH_PRICE',
                                            'Offer': {'WareMd5': "BLUE-100007-FEED-2222Q"},
                                        },
                                        {
                                            'RejectReason': 'LOCAL_WAREHOUSE_PRIORITY',
                                            'Offer': {'WareMd5': "BLUE-100007-FEED-3331Q"},
                                        },
                                        {
                                            'RejectReason': 'LOCAL_WAREHOUSE_PRIORITY',
                                            'Offer': {'WareMd5': "BLUE-100007-FEED-3332Q"},
                                        },
                                        {
                                            'RejectReason': 'LOCAL_WAREHOUSE_PRIORITY',
                                            'Offer': {'WareMd5': "FBS1_1_200_WITH_DELIVw"},
                                        },
                                        {
                                            'RejectReason': 'TOO_HIGH_PRICE',
                                            'Offer': {'WareMd5': "FBS2_2_300_WITH_DELIVw"},
                                        },
                                    ],
                                }
                            },
                        },
                    ],
                }
            },
        )
        self.show_log.expect(ware_md5="BLUE-100007-FEED-3335Q", pp=200, is_local_warehouse=0)


if __name__ == '__main__':
    main()
