#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import (
    DynamicDeliveryRestriction,
    DynamicWarehouseDelivery,
    DynamicWarehouseLink,
    Offer,
    OfferDimensions,
    Region,
    ReturnRate,
    Shop,
    ShopOperationalRating,
    DynamicWarehouseInfo,
    DynamicWarehouseToWarehouseInfo,
)
from core.types.sku import MarketSku, BlueOffer
from core.matcher import NoKey, Round


USE_DEPRECATED_DIRECT_SHIPPING_FLOW = "&rearr-factors=get_rid_of_direct_shipping=0"


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.index.regiontree += [
            Region(
                rid=1,
                name='Московская область',
                region_type=Region.FEDERATIVE_SUBJECT,
                children=[
                    Region(rid=213, name='Москва'),
                ],
            )
        ]

        cls.index.shops += [
            Shop(fesh=13, priority_region=213, fulfillment_virtual=True, virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE),
            Shop(
                fesh=31,
                datafeed_id=31,
                priority_region=213,
                regions=[225],
                name="3P поставщик Вася",
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
                warehouse_id=145,
            ),
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
                name="3P поставщик Петя",
                supplier_type=Shop.THIRD_PARTY,
                is_supplier=True,
                blue=Shop.BLUE_REAL,
                warehouse_id=145,
                fulfillment_program=True,
            ),
            Shop(fesh=41, business_fesh=41, name="dsbs магазин", regions=[213], cpa=Shop.CPA_REAL),
        ]

        cls.dynamic.lms += [
            DynamicWarehouseInfo(id=145, home_region=213),
            DynamicWarehouseToWarehouseInfo(warehouse_from=145, warehouse_to=145),
        ]

        cls.index.mskus += [
            MarketSku(
                hyperid=1,
                title='blue market sku1',
                sku=1,
                blue_offers=[
                    BlueOffer(
                        waremd5='EpnWVxDQxj4wg7vVI1ElnA',
                        price=30,
                        offerid='shop_sku_gt_ref_min',
                        feedid=31,
                        randx=3100,
                        fee=125,
                    ),
                ],
            ),
            MarketSku(
                hyperid=1,
                title='blue market sku2',
                sku=2,
                blue_offers=[
                    BlueOffer(
                        waremd5='BLUE-100011-FEED-4444g',
                        price=30,
                        offerid='shop_sku_gt_ref_min',
                        feedid=4444,
                        randx=3100,
                        fee=125,
                    ),
                ],
            ),
            MarketSku(
                hyperid=1,
                title='blue market sku3',
                sku=3,
                blue_offers=[
                    BlueOffer(
                        waremd5='BLUE-100011-FEED-2222g',
                        price=30,
                        offerid='shop_sku_gt_ref_min',
                        feedid=2222,
                        randx=3100,
                        fee=125,
                    )
                ],
            ),
            MarketSku(
                hyperid=1,
                title='blue market sku4',
                sku=4,
                blue_offers=[
                    BlueOffer(
                        waremd5='BLUE-100011-FEED-1111g',
                        price=30,
                        offerid='shop_sku_gt_ref_min',
                        feedid=1111,
                        randx=3100,
                        fee=125,
                    )
                ],
            ),
            MarketSku(
                hyperid=5,
                hid=5,
                sku=5,
                blue_offers=[
                    BlueOffer(
                        price=2100,
                        feedid=feedid,
                        waremd5='OFF1_2100_SKU5_SUP{}_Q'.format(feedid),
                        randx=900 * feedid,
                        blue_weight=10,
                        dimensions=OfferDimensions(length=543, width=175, height=357),
                    )
                    for feedid in [11, 12]
                ]
                + [
                    BlueOffer(
                        price=2000,
                        feedid=feedid,
                        waremd5='OFF2_2000_SKU5_SUP{}_Q'.format(feedid),
                        randx=1000 * feedid,
                        blue_weight=11,
                        dimensions=OfferDimensions(length=5, width=1, height=3),
                    )
                    for feedid in [11, 12, 31, 32]
                ],
            ),
        ]

        cls.index.offers += [
            Offer(
                title="market DSBS Offer",
                hid=5,
                hyperid=5,
                price=500,
                fesh=41,
                business_id=41,
                sku=5,
                cpa=Offer.CPA_REAL,
                waremd5='DSBS_5_500_WITH_DELIVw',
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
            ShopOperationalRating(
                calc_time=1589936458409,
                shop_id=41,
                dsbs_late_delivery_rate=4.7,
                dsbs_cancellation_rate=4.91,
                dsbs_return_rate=4.2,
                total=94.8,
            ),  # dsbs rating
        ]

        cls.index.return_rate += [
            ReturnRate(
                supplier_id=2222,
                type='FF',
                return_rate=0.77,
                late_ship_rate=-1,
                cancellation_rate=-1,
            ),
        ]

    @classmethod
    def prepare_nordstream(cls):
        cls.settings.nordstream_autogenerate = False
        for warehouse_id in [145]:
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

    def test_dropship_only_flag(self):
        """Проверяем работу флага market_operational_rating_dropship_only
        По умолчанию он выключен,
        остальные тесты должны работать без измененний,
        Когда он включен, то в выдаче репорта операционный рейтинг должен быть только у синих дропшипов
        """

        """Проверяем dsbs - рейтинг должен быть только в debug'е байбокса
        """
        response = self.report.request_json(
            'place=productoffers&offers-set=defaultList&market-sku=5&rearr-factors=market_operational_rating=1;market_nordstream_buybox=0;market_blue_buybox_disable_dsbs_pessimisation=1;market_operational_rating_dropship_only=1&debug=1'  # noqa
        )
        self.assertFragmentIn(
            response,
            {
                "wareId": "DSBS_5_500_WITH_DELIVw",
                "supplier": {
                    "id": 41,
                    "operationalRating": NoKey("operationalRating"),
                },
                'debug': {
                    'buyboxDebug': {
                        'Offers': [
                            {
                                'WareMd5': 'DSBS_5_500_WITH_DELIVw',
                                'ShopOperationalRating': {
                                    'CancellationRate': 0.0491,
                                    'LateShipRate': 0.047,
                                    'ReturnRate': 0.042,
                                },
                            },
                        ],
                    }
                },
            },
        )

        """Проверяем IsFulfillment - рейтинг должен быть только в debug'е байбокса
        """
        response = self.report.request_json(
            'place=productoffers&offers-set=defaultList&market-sku=3&rearr-factors=market_operational_rating=1;market_nordstream_buybox=0;market_blue_buybox_disable_dsbs_pessimisation=1;market_operational_rating_dropship_only=1&debug=1'  # noqa
        )
        self.assertFragmentIn(
            response,
            {
                "wareId": "BLUE-100011-FEED-2222g",
                "supplier": {"id": 2222, "operationalRating": NoKey("operationalRating")},
                'debug': {
                    'buyboxDebug': {
                        'Offers': [
                            {
                                'WareMd5': 'BLUE-100011-FEED-2222g',
                                'ShopOperationalRating': {
                                    'CancellationRate': 0.011,
                                    'LateShipRate': 0.05,
                                    'ReturnRate': 0.001,
                                },
                            },
                        ],
                    }
                },
            },
        )

        """Проверяем 3p - рейтинг должен быть и в выдаче и в debug'е байбокса
        """
        response = self.report.request_json(
            'place=productoffers&offers-set=defaultList&market-sku=1&rearr-factors=market_operational_rating=1;market_nordstream_buybox=0;market_blue_buybox_disable_dsbs_pessimisation=1;market_operational_rating_dropship_only=1&debug=1'  # noqa
        )
        self.assertFragmentIn(
            response,
            {
                "wareId": "EpnWVxDQxj4wg7vVI1ElnA",
                "supplier": {
                    "id": 31,
                    "operationalRating": {
                        "calcTime": 1589936458409,
                        "lateShipRate": 5.9,
                        "cancellationRate": 1.93,
                        "returnRate": 0.1,
                        "total": 99.8,
                    },
                },
                'debug': {
                    'buyboxDebug': {
                        'Offers': [
                            {
                                'WareMd5': 'EpnWVxDQxj4wg7vVI1ElnA',
                                'ShopOperationalRating': {
                                    'CancellationRate': 0.0193,
                                    'LateShipRate': 0.059,
                                    'ReturnRate': 0.001,
                                },
                            },
                        ],
                    }
                },
            },
        )

    def test_dsbs_rating(self):
        """Проверяем, что в выдаче place=sku_offers правильный operationalRating для dsbs."""
        response = self.report.request_json(
            'place=productoffers&offers-set=defaultList&market-sku=5&rearr-factors=market_operational_rating=1;market_nordstream_buybox=0;market_blue_buybox_disable_dsbs_pessimisation=1;market_operational_rating_dropship_only=0&debug=1'  # noqa
        )
        self.assertFragmentIn(
            response,
            {
                "wareId": "DSBS_5_500_WITH_DELIVw",
                "supplier": {
                    "id": 41,
                    "operationalRating": {
                        "calcTime": 1589936458409,
                        "dsbsLateDeliveryRate": 4.7,
                        "dsbsCancellationRate": 4.91,
                        "dsbsReturnRate": 4.2,
                        "total": 94.8,
                    },
                },
                'debug': {
                    'buyboxDebug': {
                        'Offers': [
                            {
                                'WareMd5': 'DSBS_5_500_WITH_DELIVw',
                                'ShopOperationalRating': {
                                    'CancellationRate': 0.0491,
                                    'LateShipRate': 0.047,
                                    'ReturnRate': 0.042,
                                },
                            },
                        ],
                    }
                },
            },
        )

    def test_productoffers_rating(self):
        """Проверяем, что в выдаче place=productoffers есть fee и operationalRating."""
        response = self.report.request_json(
            'place=productoffers&offers-set=defaultList&text=market&pp=1&market-sku=1&rearr-factors=market_operational_rating=1'
        )
        self.assertFragmentIn(
            response,
            {
                "wareId": "EpnWVxDQxj4wg7vVI1ElnA",
                "fee": "0.0000",
                "supplier": {
                    "id": 31,
                    "operationalRating": {
                        "calcTime": 1589936458409,
                        "lateShipRate": 5.9,
                        "cancellationRate": 1.93,
                        "returnRate": 0.1,
                        "total": 99.8,
                    },
                },
            },
        )

    def test_1p_rating(self):
        """
        Для 1p берем рейтинги из rearr флагов
        @see https://st.yandex-team.ru/MARKETOUT-38387
        """
        response = self.report.request_json(
            'place=productoffers&offers-set=defaultList&text=market&pp=1&market-sku=4&debug=da'
            '&rearr-factors=market_operational_rating=1;'
            'market_blue_buybox_1p_cancellation_rating_default=0.1;'
            'market_blue_buybox_1p_late_ship_rating_default=0.2;'
            'market_blue_buybox_1p_return_rating_default=0.3;'
        )
        self.assertFragmentIn(
            response,
            {
                "wareId": "BLUE-100011-FEED-1111g",
                "supplier": {"id": 1111, "operationalRating": NoKey("operationalRating")},
                'debug': {
                    'buyboxDebug': {
                        'Offers': [
                            {
                                'WareMd5': 'BLUE-100011-FEED-1111g',
                                'ShopOperationalRating': {
                                    'CancellationRate': Round(0.1),
                                    'LateShipRate': Round(0.2),
                                    'ReturnRate': Round(0.3),
                                },
                            },
                        ],
                    }
                },
            },
        )

    def test_return_rate_with_flag(self):
        """Проверяем, что в выдаче c включенным флагом market_operational_rating_everywhere рейтинг return_rate не загружается."""
        response = self.report.request_json(
            'place=productoffers&offers-set=defaultList&text=market&pp=1&market-sku=3&debug=da&rearr-factors=market_operational_rating=1'
        )
        self.assertFragmentIn(
            response,
            {
                "wareId": "BLUE-100011-FEED-2222g",
                'debug': {
                    'buyboxDebug': {
                        'Offers': [
                            {
                                'WareMd5': 'BLUE-100011-FEED-2222g',
                                'ShopOperationalRating': {
                                    'ReturnRate': 0.001,
                                },
                            },
                        ],
                    }
                },
            },
        )

    def test_buybox_rating(self):
        """Проверяем, что fee и operationalRating есть в BuyboxContest."""
        response = self.report.request_json(
            'place=productoffers&offers-set=defaultList&text=market&pp=1&market-sku=1&debug=da&rearr-factors=market_debug_buybox=1;market_blue_buybox_by_gmv_ue=0;market_ranging_cpa_by_ue_in_top=1;market_priority_blue_fee_from_params=200'  # noqa
        )
        self.assertFragmentIn(
            response,
            {
                "wareId": "EpnWVxDQxj4wg7vVI1ElnA",
                'debug': {
                    'buyboxDebug': {
                        'Offers': [
                            {
                                'WareMd5': 'EpnWVxDQxj4wg7vVI1ElnA',
                                'TransactionFee': 0.0200001,
                                'ShopOperationalRating': {
                                    'CancellationRate': 0.0193,
                                    'LateShipRate': 0.059,
                                    'ReturnRate': 0.001,
                                },
                            },
                        ],
                    }
                },
            },
        )


if __name__ == '__main__':
    main()
