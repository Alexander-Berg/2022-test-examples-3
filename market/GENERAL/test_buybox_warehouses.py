#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    BlueOffer,
    BundleOfferId,
    Currency,
    DateSwitchTimeAndRegionInfo,
    DeliveryBucket,
    DeliveryOption,
    DynamicDeliveryRestriction,
    DynamicDeliveryServiceInfo,
    DynamicPriceControlData,
    DynamicWarehouseAndDeliveryServiceInfo,
    DynamicWarehouseDelivery,
    DynamicWarehouseInfo,
    DynamicWarehouseToWarehouseInfo,
    DynamicWarehouseLink,
    DynamicWarehousesPriorityInRegion,
    Elasticity,
    MarketSku,
    Promo,
    PromoMSKU,
    PromoType,
    Region,
    RegionalDelivery,
    Shop,
    Tax,
    WarehouseWithPriority,
    WarehousesPriorityInRegion,
)
from core.testcase import TestCase, main
from core.matcher import EqualToOneOf


USE_DEPRECATED_DIRECT_SHIPPING_FLOW = "&rearr-factors=get_rid_of_direct_shipping=0"


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.shops += [
            Shop(
                fesh=1,
                datafeed_id=1,
                priority_region=213,
                name='blue_shop_1',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
            ),
            Shop(
                fesh=2,
                datafeed_id=2,
                priority_region=213,
                name='blue_shop_2',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                supplier_type=Shop.FIRST_PARTY,
                blue=Shop.BLUE_REAL,
            ),
            Shop(
                fesh=3,
                datafeed_id=3,
                priority_region=213,
                name='blue_shop_3',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
            ),
            Shop(
                fesh=4,
                datafeed_id=4,
                priority_region=213,
                name='blue_shop_3',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
            ),
            Shop(
                fesh=5,
                datafeed_id=5,
                priority_region=213,
                name='blue_shop_5',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
                direct_shipping=False,
                fulfillment_program=True,
            ),
        ]
        cls.dynamic.market_dynamic.dynamic_price_control += [
            DynamicPriceControlData(2, 100),
            DynamicPriceControlData(3, 0),  # equal to not specified
            DynamicPriceControlData(4, 9.5),
        ]

        cls.index.mskus += [
            MarketSku(
                hyperid=1,
                sku=1,
                blue_offers=[
                    BlueOffer(price=1000, feedid=1, waremd5='Sku1Offer1-IiLVm1Goleg'),
                    BlueOffer(price=2000, feedid=2, waremd5='Sku1Offer2-IiLVm1Goleg', offerid=2002001),
                    BlueOffer(price=1001, feedid=3, waremd5='Sku1Offer3-IiLVm1Goleg'),
                    BlueOffer(price=2000, feedid=4, waremd5='Sku1Offer4-IiLVm1Goleg', offerid=4004001),
                ],
            ),
            MarketSku(hyperid=2, sku=2, blue_offers=[BlueOffer(price=1500, feedid=2, offerid=2002002)]),
            MarketSku(hyperid=3, sku=3, blue_offers=[BlueOffer(price=1500, feedid=4, offerid=4004003)]),
        ]

        cls.index.promos += [
            Promo(
                promo_type=PromoType.BUNDLE,
                key='promo-for-supplier2',
                bundle_offer_ids=[
                    BundleOfferId(feed_id=2, offer_id='2002002'),
                    BundleOfferId(feed_id=2, offer_id='2002001'),
                ],
                mskus=[PromoMSKU(msku=2)],
                feed_id=2,
            ),
            Promo(
                promo_type=PromoType.BUNDLE,
                key='promo-for-supplier4',
                bundle_offer_ids=[
                    BundleOfferId(feed_id=4, offer_id='4004003'),
                    BundleOfferId(feed_id=4, offer_id='4004001'),
                ],
                mskus=[PromoMSKU(msku=3)],
                feed_id=4,
            ),
        ]

    @classmethod
    def prepare_different_warehouses(cls):

        cls.index.regiontree += [
            Region(rid=213, region_type=Region.CITY),
            Region(rid=2, region_type=Region.CITY),
            Region(rid=39, region_type=Region.CITY),
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
                name="1P-Магазин 148 склад",
                supplier_type=Shop.FIRST_PARTY,
                blue=Shop.BLUE_REAL,
                warehouse_id=148,
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
                fesh=4444,
                datafeed_id=4444,
                priority_region=39,
                regions=[225],
                name="3P-Магазин 147 склад",
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
                warehouse_id=147,
                fulfillment_program=True,
            ),
        ]

        cls.settings.lms_autogenerate = False
        cls.dynamic.lms += [
            DynamicWarehouseInfo(id=145, home_region=213),
            DynamicWarehouseInfo(id=148, home_region=213),
            DynamicWarehouseInfo(id=147, home_region=39),
            DynamicWarehouseToWarehouseInfo(warehouse_from=145, warehouse_to=145),
            DynamicWarehouseToWarehouseInfo(warehouse_from=148, warehouse_to=148),
            DynamicWarehouseToWarehouseInfo(warehouse_from=147, warehouse_to=147),
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=145,
                delivery_service_id=157,
                date_switch_time_infos=[DateSwitchTimeAndRegionInfo(date_switch_hour=23, region_to=225)],
            ),
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=148,
                delivery_service_id=157,
                date_switch_time_infos=[DateSwitchTimeAndRegionInfo(date_switch_hour=23, region_to=225)],
            ),
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=147,
                delivery_service_id=157,
                date_switch_time_infos=[DateSwitchTimeAndRegionInfo(date_switch_hour=23, region_to=225)],
            ),
            DynamicDeliveryServiceInfo(id=157, rating=2),
            DynamicWarehousesPriorityInRegion(region=225, warehouses=[145, 148, 147]),
        ]

        cls.index.warehouse_priorities += [
            # в Москве приоритет складов одинаков, и все офферы будут становиться buybox равновероятно
            WarehousesPriorityInRegion(
                regions=[213],
                warehouse_with_priority=[
                    WarehouseWithPriority(145, 0),
                    WarehouseWithPriority(148, 0),
                    WarehouseWithPriority(147, 0),
                ],
            ),
            # в Ростове оффер со 147 склада всегда попадает в байбокс
            WarehousesPriorityInRegion(
                regions=[39],
                warehouse_with_priority=[
                    WarehouseWithPriority(147, 1),
                    WarehouseWithPriority(145, 3),
                    WarehouseWithPriority(148, 2),
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
                        rid=225, options=[DeliveryOption(price=15, day_from=1, day_to=2, shop_delivery_price=15)]
                    ),
                    RegionalDelivery(
                        rid=39, options=[DeliveryOption(price=15, day_from=1, day_to=2, shop_delivery_price=15)]
                    ),
                ],
            )
        ]

        sorted_elasticity = [
            Elasticity(price_variant=1100, demand_mean=200),
            Elasticity(price_variant=1200, demand_mean=80),
            Elasticity(price_variant=1300, demand_mean=10),
            Elasticity(price_variant=1400, demand_mean=5),
        ]

        # итак, у нас широкий выбор офферов для байбокса из двух имеющихся складов
        # по умолчнанию будет выбираться один из 1P офферов, причем с большой вероятностью тот что дешевше
        # под флагом market_user_cart_warehouse_priority_coefficient будет скорее выбираться тот что с того же склада что и оффер в нашей корзине
        cls.index.mskus += [
            MarketSku(
                title="Что-нибудь шведское",
                hyperid=349238,
                sku=349238,
                hid=100,
                delivery_buckets=[1234],
                blue_offers=[
                    BlueOffer(price=100, feedid=1111, waremd5="BLUE-349238-FEED-1111Q"),
                    BlueOffer(price=100, feedid=2222, waremd5="BLUE-349238-FEED-2222g"),
                ],
            ),
            MarketSku(
                title="Абрикосовые эклеры",
                hyperid=439018,
                sku=439018,
                hid=100,
                delivery_buckets=[1234],
                buybox_elasticity=sorted_elasticity,
                blue_offers=[
                    BlueOffer(price=1300, feedid=1111, waremd5="BLUE-439018-FEED-1111Q"),
                    BlueOffer(price=1305, feedid=2222, waremd5="BLUE-439018-FEED-2222g"),
                    BlueOffer(price=1301, feedid=3333, waremd5="BLUE-439018-FEED-3333w"),
                ],
            ),
            MarketSku(
                title="Сюрстреминг",
                hyperid=737290,
                sku=737290,
                hid=100,
                delivery_buckets=[1234],
                blue_offers=[
                    BlueOffer(price=1300, feedid=1111, waremd5="BLUE-737290-FEED-1111Q"),
                    BlueOffer(price=1000, feedid=2222, waremd5="BLUE-737290-FEED-2222g"),
                    BlueOffer(price=1300, feedid=3333, waremd5="BLUE-737290-FEED-3333w"),
                ],
            ),
            MarketSku(
                title="Тестируем приоритет Ростова",
                hyperid=439019,
                sku=439019,
                hid=100,
                delivery_buckets=[1234],
                buybox_elasticity=sorted_elasticity,
                blue_offers=[
                    BlueOffer(price=1300, feedid=1111, waremd5="BLUE-439019-FEED-1111Q"),
                    BlueOffer(price=1305, feedid=2222, waremd5="BLUE-439019-FEED-2222g"),
                    BlueOffer(price=1301, feedid=3333, waremd5="BLUE-439019-FEED-3333w"),
                    BlueOffer(price=1311, feedid=4444, waremd5="BLUE-439019-FEED-4444w"),
                    BlueOffer(price=1341, feedid=4444, waremd5="BLUE-439019-FEED-4444x"),
                ],
            ),  # Более дорогой оффер из Ростова
        ]

    @classmethod
    def prepare_nordstream(cls):
        cls.settings.nordstream_autogenerate = False
        for warehouse_id in (145, 147, 148):
            cls.dynamic.nordstream += [DynamicWarehouseLink(warehouse_id, [warehouse_id])]
            cls.dynamic.nordstream += [
                DynamicWarehouseDelivery(
                    warehouse_id,
                    {
                        225: [
                            DynamicDeliveryRestriction(
                                min_days=1,
                                max_days=2,
                                cost=15,
                            ),
                        ],
                    },
                ),
            ]

    def test_market_user_cart_warehouse_priority_coefficient(self):
        """Проверяем что под флагом market_user_cart_warehouse_priority_coefficient
        больший приоритет будет отдаваться офферам с того же склада, что и другие офферы в корзине пользователя
        (это работает только в Москве, т.к. в Питере 148 склад имеет жесткий приоритет)
        """
        d = '&debug=da&rearr-factors=market_debug_buybox=1;market_blue_buybox_by_gmv_ue=0;market_blue_buybox_with_delivery_context=1'
        cart145 = '&cart=BLUE-349238-FEED-1111Q&rearr-factors=market_user_cart_warehouse_priority_coefficient=100;'
        cart148 = '&cart=BLUE-349238-FEED-2222g&rearr-factors=market_user_cart_warehouse_priority_coefficient=100;'
        for yandexuid in range(1, 10):
            # добавляем корзину из 145 склада - и большой коэффициент - теперь оффер из 145 склада с большей вероятностью будет buybox
            # в Москве только офферы из склада 145 (т.к. вероятность очень большая)
            response = self.report.request_json(
                'place=prime&rgb=blue&text=эклеры&rids=213&yandexuid={}'.format(yandexuid) + cart145 + d
            )
            self.assertFragmentIn(
                response,
                {
                    'offers': {
                        'items': [
                            {
                                'wareId': EqualToOneOf('BLUE-439018-FEED-1111Q', 'BLUE-439018-FEED-3333w'),
                                'debug': {
                                    'buyboxDebug': {
                                        'WonMethod': 'OLD_BUYBOX',
                                        'Offers': [
                                            {
                                                'WareMd5': 'BLUE-439018-FEED-1111Q',
                                                'OfferWeight': 200,
                                                'IsUserCartWarehouse': True,
                                            },
                                            {'WareMd5': 'BLUE-439018-FEED-2222g', 'OfferWeight': 0.657997},
                                            {
                                                'WareMd5': 'BLUE-439018-FEED-3333w',
                                                'OfferWeight': 160.129,
                                                'IsUserCartWarehouse': True,
                                            },
                                        ],
                                    }
                                },
                            }
                        ]
                    }
                },
            )

            # если корзина будет из 148 склада то будет выбираться в buybox оффер из 148 склада
            # в Москве только офферы из склада 148 (т.к. вероятность очень большая)
            response = self.report.request_json(
                'place=prime&rgb=blue&text=эклеры&rids=213&yandexuid={}'.format(yandexuid) + cart148 + d
            )
            self.assertFragmentIn(
                response,
                {
                    'offers': {
                        'items': [
                            {
                                'wareId': 'BLUE-439018-FEED-2222g',
                                'debug': {
                                    'buyboxDebug': {
                                        'WonMethod': 'OLD_BUYBOX',
                                        'Offers': [
                                            {'WareMd5': 'BLUE-439018-FEED-1111Q', 'OfferWeight': 2},
                                            {
                                                'WareMd5': 'BLUE-439018-FEED-2222g',
                                                'OfferWeight': 65.7997,
                                                'IsUserCartWarehouse': True,
                                            },
                                            {'WareMd5': 'BLUE-439018-FEED-3333w', 'OfferWeight': 1.60129},
                                        ],
                                    }
                                },
                            }
                        ]
                    }
                },
            )

        # если в корзине несколько офферов с разных складов - то приоритет выдается всем складам (в данном случае никакому)
        cart = '&cart=BLUE-349238-FEED-1111Q,BLUE-349238-FEED-2222g&rearr-factors=market_user_cart_warehouse_priority_coefficient=100;'
        # В Москве все 3 оффера периодически становятся buybox
        # Все офферы помечены как u1, веса всех офферов умножились на 100
        response = self.report.request_json('place=prime&rgb=blue&text=эклеры&rids=213&yandexuid=1' + cart + d)
        self.assertFragmentIn(
            response,
            {
                'offers': {
                    'items': [
                        {
                            'wareId': 'BLUE-439018-FEED-1111Q',
                            'debug': {
                                'buyboxDebug': {
                                    'WonMethod': 'OLD_BUYBOX',
                                    'Offers': [
                                        {
                                            'WareMd5': 'BLUE-439018-FEED-1111Q',
                                            'OfferWeight': 200,
                                            'IsUserCartWarehouse': True,
                                        },
                                        {
                                            'WareMd5': 'BLUE-439018-FEED-2222g',
                                            'OfferWeight': 65.7997,
                                            'IsUserCartWarehouse': True,
                                        },
                                        {
                                            'WareMd5': 'BLUE-439018-FEED-3333w',
                                            'OfferWeight': 160.129,
                                            'IsUserCartWarehouse': True,
                                        },
                                    ],
                                }
                            },
                        }
                    ]
                }
            },
        )

        response = self.report.request_json('place=prime&rgb=blue&text=эклеры&rids=213&yandexuid=3' + cart + d)
        self.assertFragmentIn(
            response,
            {
                'offers': {
                    'items': [
                        {
                            'wareId': 'BLUE-439018-FEED-2222g',
                            'debug': {
                                'buyboxDebug': {
                                    'WonMethod': 'OLD_BUYBOX',
                                    'Offers': [
                                        {
                                            'WareMd5': 'BLUE-439018-FEED-1111Q',
                                            'OfferWeight': 200,
                                            'IsUserCartWarehouse': True,
                                        },
                                        {
                                            'WareMd5': 'BLUE-439018-FEED-2222g',
                                            'OfferWeight': 65.7997,
                                            'IsUserCartWarehouse': True,
                                        },
                                        {
                                            'WareMd5': 'BLUE-439018-FEED-3333w',
                                            'OfferWeight': 160.129,
                                            'IsUserCartWarehouse': True,
                                        },
                                    ],
                                }
                            },
                        }
                    ]
                }
            },
        )

        response = self.report.request_json('place=prime&rgb=blue&text=эклеры&rids=213&yandexuid=5' + cart + d)
        self.assertFragmentIn(
            response,
            {
                'offers': {
                    'items': [
                        {
                            'wareId': 'BLUE-439018-FEED-3333w',
                            'debug': {
                                'buyboxDebug': {
                                    'WonMethod': 'OLD_BUYBOX',
                                    'Offers': [
                                        {
                                            'WareMd5': 'BLUE-439018-FEED-1111Q',
                                            'OfferWeight': 200,
                                            'IsUserCartWarehouse': True,
                                        },
                                        {
                                            'WareMd5': 'BLUE-439018-FEED-2222g',
                                            'OfferWeight': 65.7997,
                                            'IsUserCartWarehouse': True,
                                        },
                                        {
                                            'WareMd5': 'BLUE-439018-FEED-3333w',
                                            'OfferWeight': 160.129,
                                            'IsUserCartWarehouse': True,
                                        },
                                    ],
                                }
                            },
                        }
                    ]
                }
            },
        )

    def test_price_meaning_for_buybox(self):
        """Если различия в цене очень значительны, а коэффициент market_user_cart_warehouse_priority_coefficient адекватный
        то будет в качестве buybox все равно выбран более дешевый оффер
        """

        cart = '&cart=BLUE-349238-FEED-1111Q&rearr-factors=market_user_cart_warehouse_priority_coefficient=3;'
        for yandexuid in range(1, 10):
            # в Москве будет выбран оффер BLUE-737290-FEED-2222g из 148 склада из-за его низкой цены независимо от корзины
            response = self.report.request_json(
                'place=prime&rgb=blue&text=сюрстреминг&rids=213&yandexuid={}'.format(yandexuid) + cart
            )
            self.assertFragmentIn(response, {'offers': {'items': [{'wareId': 'BLUE-737290-FEED-2222g'}]}})
            response = self.report.request_json(
                'place=prime&rgb=blue&text=сюрстреминг&rids=213&yandexuid={}'.format(yandexuid)
            )
            self.assertFragmentIn(response, {'offers': {'items': [{'wareId': 'BLUE-737290-FEED-2222g'}]}})

    def test_market_rostov_priority(self):
        """Проверяем для запроса из Ростова даже что под флагом market_user_cart_warehouse_priority_coefficient
        больший приоритет будет отдаваться не офферам с того же склада, что и другие офферы в корзине пользователя, а офферу из 147
        """
        d = '&debug=da&rearr-factors=market_debug_buybox=1;market_blue_buybox_with_delivery_context=1'
        cart145 = '&cart=BLUE-439019-FEED-1111Q&rearr-factors=market_user_cart_warehouse_priority_coefficient=100;'
        for yandexuid in range(1, 10):
            # добавляем корзину из 145 склада - и большой коэффициент - теперь оффер из 145 склада с большей вероятностью будет buybox
            # в Москве только офферы из склада 145 (т.к. вероятность очень большая), но мы запрашиваем в Ростове
            response = self.report.request_json(
                'place=prime&rgb=blue&text=приоритет&rids=39&yandexuid={}'.format(yandexuid) + cart145 + d
            )
            self.assertFragmentIn(
                response,
                {
                    'search': {
                        'results': [
                            {
                                'debug': {
                                    'properties': {
                                        'WARE_MD5': 'BLUE-439019-FEED-4444w',
                                    }
                                }
                            }
                        ]
                    }
                },
            )

    def test_market_user_cart_warehouse_split_gmv_coefficient(self):
        """Проверяем что под флагом market_user_cart_warehouse_priority_coefficient
        больший приоритет будет отдаваться офферам с того же склада, что и другие офферы в корзине пользователя
        (это работает только в Москве, т.к. в Питере 148 склад имеет жесткий приоритет)
        """
        d = '&debug=1&rearr-factors=market_debug_buybox=1'
        cart145 = '&cart=BLUE-349238-FEED-1111Q'
        _ = '&cart=BLUE-349238-FEED-2222g'
        for yandexuid in range(1, 10):
            # добавляем корзину из 145 склада - и большой коэффициент - теперь оффер из 145 склада с большей вероятностью будет buybox
            # в Москве только офферы из склада 145 (т.к. вероятность очень большая)
            response = self.report.request_json(
                'place=productoffers&offers-set=defaultList&market-sku=439018&rids=213&yandexuid={}'.format(yandexuid)
                + cart145
                + d
            )
            self.assertFragmentIn(
                response,
                {
                    'search': {
                        'results': [
                            {
                                'wareId': EqualToOneOf('BLUE-439018-FEED-1111Q', 'BLUE-439018-FEED-3333w'),
                                'debug': {
                                    'buyboxDebug': {
                                        'WonMethod': 'WON_BY_EXCHANGE',
                                        'Offers': [
                                            {
                                                'WareMd5': 'BLUE-439018-FEED-1111Q',
                                                'IsUserCartWarehouse': True,
                                                'IsWarehouseInUserCartCoef': 1,
                                            },
                                            {
                                                'WareMd5': 'BLUE-439018-FEED-2222g',
                                                'IsUserCartWarehouse': False,
                                                'IsWarehouseInUserCartCoef': 0.95,
                                            },
                                            {
                                                'WareMd5': 'BLUE-439018-FEED-3333w',
                                                'IsUserCartWarehouse': True,
                                                'IsWarehouseInUserCartCoef': 1,
                                            },
                                        ],
                                    }
                                },
                            }
                        ]
                    }
                },
            )

    def test_market_rostov_priority_sku_offers(self):
        """Проверяем для запроса из Ростова даже что под флагом market_user_cart_warehouse_priority_coefficient
        больший приоритет будет отдаваться не офферам с того же склада, что и другие офферы в корзине пользователя, а офферу из 147
        """
        d = '&debug=da&rearr-factors=market_debug_buybox=1;market_blue_buybox_by_gmv_ue=1;market_blue_buybox_with_delivery_context=1;'
        cart145 = '&cart=BLUE-439019-FEED-1111Q&rearr-factors=market_user_cart_warehouse_priority_coefficient=100;'
        for yandexuid in range(1, 10):
            # добавляем корзину из 145 склада - и большой коэффициент - теперь оффер из 145 склада с большей вероятностью будет buybox
            # в Москве только офферы из склада 145 (т.к. вероятность очень большая), но мы запрашиваем в Ростове
            response = self.report.request_json(
                'place=productoffers&offers-set=defaultList&market-sku=439019&rids=39&yandexuid={}'.format(yandexuid)
                + cart145
                + d
                + USE_DEPRECATED_DIRECT_SHIPPING_FLOW
            )
            self.assertFragmentIn(
                response,
                {
                    'search': {
                        'results': [
                            {
                                'debug': {
                                    'properties': {
                                        'WARE_MD5': 'BLUE-439019-FEED-4444w',
                                    }
                                }
                            }
                        ]
                    }
                },
            )
            self.assertFragmentIn(
                response,
                {
                    'debug': {
                        'buyboxDebug': {
                            'Offers': [
                                {
                                    'Conversion': 11.0341,
                                    'ConversionByDeliveryDayCoef': 1.244,
                                }
                            ],
                        }
                    }
                },
            )

    def test_market_rostov_priority_sku_offers_south_delivery_coef(self):
        """Проверяем для запроса из Ростова даже что под флагом market_user_cart_warehouse_priority_coefficient
        больший приоритет будет отдаваться не офферам с того же склада, что и другие офферы в корзине пользователя, а офферу из 147
        """
        d = '&debug=da&rearr-factors=market_debug_buybox=1;market_blue_buybox_by_gmv_ue=1;market_blue_buybox_with_delivery_context=1;'
        def_del = 'market_blue_buybox_delivery_day_add_conversion_coef=0.1;market_blue_buybox_delivery_day_mul_conversion_coef=0.1;market_blue_buybox_delivery_day_min_conversion_coef=0.1'
        cart145 = '&cart=BLUE-439019-FEED-1111Q&rearr-factors=market_user_cart_warehouse_priority_coefficient=100;'
        for yandexuid in range(1, 10):
            # добавляем корзину из 145 склада - и большой коэффициент - теперь оффер из 145 склада с большей вероятностью будет buybox
            # в Москве только офферы из склада 145 (т.к. вероятность очень большая), но мы запрашиваем в Ростове
            response = self.report.request_json(
                'place=productoffers&offers-set=defaultList&market-sku=439019&rids=39&yandexuid={}'.format(yandexuid)
                + cart145
                + d
                + def_del
                + USE_DEPRECATED_DIRECT_SHIPPING_FLOW
            )
            self.assertFragmentIn(
                response,
                {
                    'search': {
                        'results': [
                            {
                                'debug': {
                                    'properties': {
                                        'WARE_MD5': 'BLUE-439019-FEED-4444w',
                                    }
                                }
                            }
                        ]
                    }
                },
            )
            self.assertFragmentIn(
                response,
                {
                    'debug': {
                        'buyboxDebug': {
                            'Offers': [
                                {
                                    'Conversion': 0.886987,
                                    'ConversionByDeliveryDayCoef': 0.1,
                                }
                            ],
                        }
                    }
                },
            )


if __name__ == '__main__':
    main()
