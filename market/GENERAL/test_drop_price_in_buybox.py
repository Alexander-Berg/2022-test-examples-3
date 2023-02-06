#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    BlueOffer,
    Currency,
    DateSwitchTimeAndRegionInfo,
    DeliveryBucket,
    DeliveryOption,
    DynamicDeliveryServiceInfo,
    DynamicPriceControlData,
    DynamicWarehouseAndDeliveryServiceInfo,
    DynamicWarehouseInfo,
    DynamicWarehousesPriorityInRegion,
    Elasticity,
    MarketSku,
    Region,
    RegionalDelivery,
    RegionalMsku,
    Shop,
    Tax,
    WarehouseWithPriority,
    WarehousesPriorityInRegion,
)
from core.testcase import TestCase, main


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

        cls.index.blue_regional_mskus += [
            RegionalMsku(msku_id=1, offers=4, price_min=1000, price_max=2000, rids=[213]),
        ]

    def test_sku_offers(self):
        """
        Проверяем подмену цен в place=sku_offers
        """
        # находится самый дешевый оффер (При равных ценах должен выбраться 3p оффер)
        request_template = 'place=sku_offers&market-sku=1&rearr-factors=market_blue_buybox_disable_old_buybox_algo=0'
        self.assertFragmentIn(
            self.report.request_json(request_template + '&yandexuid=1'),
            {
                'supplier': {'id': 1},
                'prices': {'value': '1000'},
            },
        )

        # находится оффер который может сбросить цену до самого дешевого
        self.assertFragmentIn(
            self.report.request_json(request_template + ';enable_offline_buybox_price=0&yandexuid=4'),
            {
                'supplier': {'id': 2},
                'prices': {'value': '1000'},
            },
        )

        # c оффлайн байбоксом выбирается другой оффер с начальной ценой 1000
        self.assertFragmentIn(
            self.report.request_json(request_template + ';enable_offline_buybox_price=1&yandexuid=4'),
            {
                'supplier': {'id': 1},
                'prices': {'value': '1000'},
            },
        )

        # c rids=213 цена падает до наименьшей по Москве у оффера со стратегией байбокса
        self.assertFragmentIn(
            self.report.request_json(request_template + ';enable_offline_buybox_price=1&yandexuid=4&rids=213'),
            {
                'supplier': {'id': 2},
                'prices': {'value': '1000'},
            },
        )

        # находится почти самый дешевый оффер и его цена не меняется
        self.assertFragmentIn(
            self.report.request_json(request_template + '&yandexuid=3'),
            {
                'supplier': {'id': 3},
                'prices': {'value': '1001'},
            },
        )

        # НЕ находится оффер который НЕ может сбросить цену до самого дешевого
        for yuid in range(1, 100):
            self.assertFragmentNotIn(
                self.report.request_json(request_template + '&yandexuid={}'.format(yuid)),
                {
                    'supplier': {'id': 4},
                },
            )

    def test_sku_offers_with_offer_id(self):
        """
        Проверяем подмену цен в place=sku_offers при явно запрошенном BuyBox
        """
        # оффер который может сбросить цену до самого дешевого
        response = self.report.request_json(
            'place=sku_offers&market-sku=1'
            '&offerid=Sku1Offer2-IiLVm1Goleg'
            '&rearr-factors=enable_offline_buybox_price=0'
        )
        self.assertFragmentIn(
            response,
            {
                'supplier': {'id': 2},
                'prices': {'value': '1000'},
            },
        )

        # c оффлайн байбоксом без указания региона, цена байбокса не определяется
        response = self.report.request_json(
            'place=sku_offers&market-sku=1'
            '&offerid=Sku1Offer2-IiLVm1Goleg'
            '&rearr-factors=enable_offline_buybox_price=1'
        )
        self.assertFragmentIn(
            response,
            {
                'supplier': {'id': 2},
                'prices': {'value': '2000'},
            },
        )

        # c rids=213 цена падает до наименьшей по Москве
        response = self.report.request_json(
            'place=sku_offers&market-sku=1&rids=213'
            '&offerid=Sku1Offer2-IiLVm1Goleg'
            '&rearr-factors=enable_offline_buybox_price=1'
        )
        self.assertFragmentIn(
            response,
            {
                'supplier': {'id': 2},
                'prices': {'value': '1000'},
            },
        )

        # оффер который мог бы сбросить цену, но НЕ может сбросить цену до самого дешевого
        response = self.report.request_json('place=sku_offers&market-sku=1' '&offerid=Sku1Offer4-IiLVm1Goleg')
        self.assertFragmentIn(
            response,
            {
                'supplier': {'id': 4},
                'prices': {'value': '2000'},
            },
        )

    def test_offerinfo_with_offer_id(self):
        """Проверяем подмену цен в place=offerinfo при явно запрошенном BuyBox"""

        # оффер который может сбросить цену до самого дешевого
        for x in ('&market-sku=1', '&market-sku=', '', '&show-urls=cpa,external,beruOrder'):
            for id in (
                'offerid=Sku1Offer2-IiLVm1Goleg',
                'feed_shoffer_id=2-2002001',
            ):  # различные варианты запросить оффер
                response = self.report.request_json('place=offerinfo&rids=213&regset=2&{}'.format(id) + x)
                self.assertFragmentIn(
                    response,
                    {
                        'supplier': {'id': 2},
                        'prices': {'value': '1000'},
                    },
                )

        # оффер который мог бы сбросить цену, но НЕ может сбросить цену до самого дешевого
        for x in ('&market-sku=1', '&market-sku=', '', '&show-urls=cpa,external,beruOrder'):
            for id in (
                'offerid=Sku1Offer4-IiLVm1Goleg',
                'feed_shoffer_id=4-4004001',
            ):  # различные варианты запросить оффер
                response = self.report.request_json('place=offerinfo&rids=213&regset=2&{}'.format(id) + x)
                self.assertFragmentIn(
                    response,
                    {
                        'supplier': {'id': 4},
                        'prices': {'value': '2000'},
                    },
                )

    def test_checkprices_with_offer_id(self):
        """Проверяем подмену цен в place=check_prices при явно запрошенном BuyBox"""
        response = self.report.request_json(
            'place=check_prices&feed_shoffer_id=2-2002001&rearr-factors=enable_offline_buybox_price=0'
        )
        self.assertFragmentIn(
            response,
            {
                "offerId": "2.2002001",
                'price': {'value': '1000'},
            },
        )

        # c оффлайн байбоксом без указания региона, цена байбокса не определяется
        response = self.report.request_json(
            'place=check_prices&feed_shoffer_id=2-2002001&rearr-factors=enable_offline_buybox_price=1'
        )
        self.assertFragmentIn(
            response,
            {
                "offerId": "2.2002001",
                'price': {'value': '2000'},
            },
        )

        # c rids=213 цена падает до наименьшей по Москве
        response = self.report.request_json(
            'place=check_prices&rids=213&feed_shoffer_id=2-2002001&rearr-factors=enable_offline_buybox_price=1'
        )
        self.assertFragmentIn(
            response,
            {
                "offerId": "2.2002001",
                'price': {'value': '1000'},
            },
        )

        response = self.report.request_json('place=check_prices&feed_shoffer_id=4-4004001')
        self.assertFragmentIn(
            response,
            {
                "offerId": "4.4004001",
                'price': {'value': '2000'},
            },
        )

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
            ),
        ]

        cls.settings.lms_autogenerate = False
        cls.dynamic.lms += [
            DynamicWarehouseInfo(id=145, home_region=213),
            DynamicWarehouseInfo(id=148, home_region=213),
            DynamicWarehouseInfo(id=147, home_region=39),
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
            WarehousesPriorityInRegion(
                regions=[2],
                warehouse_with_priority=[
                    WarehouseWithPriority(145, 1),
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
                    RegionalDelivery(rid=225, options=[DeliveryOption(price=15, day_from=1, day_to=2)]),
                    RegionalDelivery(rid=39, options=[DeliveryOption(price=15, day_from=1, day_to=2)]),
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

    def test_default_buybox(self):
        """Дефолтный выбор байбокса

        Флаг debug=1&rearr-factors=market_debug_buybox=1
        позволяют посмотреть какие офферы участвовали в выборе байбокса и какие победили
        офферы участвующие в выборе buybox кодируются выражением
        d8s1111w145p1300u0:200
        d<id>s<supplier>w<warehouse>p<price>u<in_user_cart>:weight
        weight - вес документа при выборе buybox умноженный на 100 и округленный до целого числа
        """
        d = '&debug=da&rearr-factors=market_debug_buybox=1;market_blue_buybox_by_gmv_ue=0;market_blue_buybox_with_delivery_context=1'

        # В Москве все 3 оффера периодически становятся buybox
        response = self.report.request_json('place=prime&text=эклеры&rids=213&rgb=blue&yandexuid=1' + d)
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
                                        {'WareMd5': 'BLUE-439018-FEED-1111Q', 'OfferWeight': 2},
                                        {'WareMd5': 'BLUE-439018-FEED-2222g', 'OfferWeight': 0.657997},
                                        {'WareMd5': 'BLUE-439018-FEED-3333w', 'OfferWeight': 1.60129},
                                    ],
                                }
                            },
                        }
                    ]
                }
            },
        )

        response = self.report.request_json('place=prime&text=эклеры&rids=213&rgb=blue&yandexuid=3' + d)
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
                                        {'WareMd5': 'BLUE-439018-FEED-2222g', 'OfferWeight': 0.657997},
                                        {'WareMd5': 'BLUE-439018-FEED-3333w', 'OfferWeight': 1.60129},
                                    ],
                                }
                            },
                        }
                    ]
                }
            },
        )

        response = self.report.request_json('place=prime&text=эклеры&rids=213&rgb=blue&yandexuid=5' + d)
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
                                        {'WareMd5': 'BLUE-439018-FEED-1111Q', 'OfferWeight': 2},
                                        {'WareMd5': 'BLUE-439018-FEED-2222g', 'OfferWeight': 0.657997},
                                        {'WareMd5': 'BLUE-439018-FEED-3333w', 'OfferWeight': 1.60129},
                                    ],
                                }
                            },
                        }
                    ]
                }
            },
        )

        # В Питере buybox становится только оффер из 148 склада т.к. этот склад имеет приоритет
        for yandexuid in range(1, 10):
            response = self.report.request_json(
                'place=prime&text=эклеры&rids=2&rgb=blue&yandexuid={}'.format(yandexuid) + d
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
                                            {'WareMd5': 'BLUE-439018-FEED-2222g', 'OfferWeight': 2},
                                        ],
                                        'RejectedOffers': [
                                            {
                                                'RejectReason': 'LESS_PRIORITY_WAREHOUSE',
                                                'Offer': {
                                                    'WareMd5': 'BLUE-439018-FEED-1111Q',
                                                },
                                            },
                                            {
                                                'RejectReason': 'LESS_PRIORITY_WAREHOUSE',
                                                'Offer': {
                                                    'WareMd5': 'BLUE-439018-FEED-3333w',
                                                },
                                            },
                                        ],
                                    }
                                },
                            }
                        ]
                    }
                },
            )

        # В Москве оффер BLUE-737290-FEED-2222g имеет существенно более низкую цену
        for yandexuid in range(1, 10):
            response = self.report.request_json(
                'place=prime&text=сюрстреминг&rids=213&rgb=blue&yandexuid={}'.format(yandexuid) + d
            )
            self.assertFragmentIn(
                response,
                {
                    'offers': {
                        'items': [
                            {
                                'wareId': 'BLUE-737290-FEED-2222g',
                                'debug': {
                                    'buyboxDebug': {
                                        'WonMethod': 'OLD_BUYBOX',
                                        'Offers': [
                                            {'WareMd5': 'BLUE-737290-FEED-2222g', 'OfferWeight': 2},
                                        ],
                                        'RejectedOffers': [
                                            {
                                                'RejectReason': 'TOO_HIGH_PRICE',
                                                'Offer': {
                                                    'WareMd5': 'BLUE-737290-FEED-1111Q',
                                                },
                                            },
                                            {
                                                'RejectReason': 'TOO_HIGH_PRICE',
                                                'Offer': {
                                                    'WareMd5': 'BLUE-737290-FEED-3333w',
                                                },
                                            },
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
