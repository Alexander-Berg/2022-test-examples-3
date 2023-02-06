#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import (
    BlueOffer,
    DateSwitchTimeAndRegionInfo,
    DeliveryBucket,
    DeliveryOption,
    DynamicDeliveryRestriction,
    DynamicDeliveryServiceInfo,
    DynamicWarehouseAndDeliveryServiceInfo,
    DynamicWarehouseDelivery,
    DynamicWarehouseInfo,
    DynamicWarehouseToWarehouseInfo,
    DynamicWarehouseLink,
    DynamicWarehousesPriorityInRegion,
    Elasticity,
    MarketSku,
    Offer,
    OfferDimensions,
    OfferInstallmentInfo,
    Payment,
    PaymentRegionalGroup,
    Region,
    RegionalDelivery,
    Shop,
    ShopOperationalRating,
    ShopPaymentMethods,
    Tax,
)
from core.types.delivery import BlueDeliveryTariff
from core.matcher import Absent
from core.types.reserveprice_fee import ReservePriceFee
from core.types.recommended_fee import RecommendedFee


def dict_to_rearr(rearr_flags):
    result = ""
    for key in rearr_flags.keys():
        result += str(key) + "=" + str(rearr_flags[key]) + ";"
    return result


class T(TestCase):
    @classmethod
    def prepare(cls):

        cls.index.blue_delivery_modifiers.set_default_modifier(
            tariffs=[
                BlueDeliveryTariff(user_price=99, large_size=0, price_to=100),
                BlueDeliveryTariff(user_price=0, large_size=0),
                BlueDeliveryTariff(user_price=399, large_size=1),
            ],
            large_size_weight=20,
        )
        cls.index.blue_delivery_modifiers.add_modifier(
            tariffs=[
                BlueDeliveryTariff(user_price=99, large_size=0, price_to=100),
                BlueDeliveryTariff(user_price=0, large_size=0),
                BlueDeliveryTariff(user_price=399, large_size=1),
            ],
            regions=[213],
            large_size_weight=20,
        )

        cls.index.regiontree += [
            Region(rid=213, region_type=Region.CITY),
            Region(rid=223, region_type=Region.CITY),
        ]

        cls.index.shops += [
            Shop(
                fesh=1,
                datafeed_id=1,
                priority_region=213,
                name='ВиртуальныйМагазинНаБеру',
                fulfillment_virtual=True,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
            ),
            Shop(
                fesh=11,
                datafeed_id=11,
                priority_region=213,
                regions=[225],
                name="Один 1P поставщик",
                supplier_type=Shop.FIRST_PARTY,
                is_supplier=True,
                blue=Shop.BLUE_REAL,
                warehouse_id=145,
                fulfillment_program=True,
            ),
            Shop(
                fesh=12,
                datafeed_id=12,
                priority_region=213,
                regions=[225],
                name="3P поставщик Игорь",
                supplier_type=Shop.THIRD_PARTY,
                is_supplier=True,
                blue=Shop.BLUE_REAL,
                warehouse_id=145,
                fulfillment_program=True,
            ),
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
                fesh=32,
                datafeed_id=32,
                priority_region=213,
                regions=[225],
                name="3P поставщик Петя",
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
                warehouse_id=145,
                fulfillment_program=True,
                direct_shipping=False,
            ),
            Shop(
                fesh=41,
                business_fesh=41,
                name="dsbs магазин Пети-41",
                priority_region=213,
                regions=[225],
                cpa=Shop.CPA_REAL,
            ),
            Shop(
                fesh=42,
                business_fesh=42,
                name="dsbs магазин Пети-42",
                priority_region=213,
                regions=[225],
                cpa=Shop.CPA_REAL,
            ),
            Shop(
                fesh=43,
                datafeed_id=43,
                priority_region=213,
                regions=[225],
                name="Экспресс магазин",
                warehouse_id=146,
                tax_system=Tax.OSN,
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
                fulfillment_program=False,
            ),
            Shop(
                fesh=44,
                datafeed_id=44,
                priority_region=213,
                regions=[225],
                name="Не экспресс магазин",
                warehouse_id=146,
                tax_system=Tax.OSN,
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
                fulfillment_program=False,
            ),
        ]

        cls.index.shops_payment_methods += [
            ShopPaymentMethods(
                fesh=11,
                payment_groups=[
                    PaymentRegionalGroup(
                        included_regions=[225],
                        payment_methods=[Payment.PT_YANDEX],
                    ),
                ],
            ),
            ShopPaymentMethods(
                fesh=12,
                payment_groups=[
                    PaymentRegionalGroup(
                        included_regions=[225],
                        payment_methods=[
                            Payment.PT_PREPAYMENT_CARD,
                            Payment.PT_PREPAYMENT_OTHER,
                            Payment.PT_CARD_ON_DELIVERY,
                            Payment.PT_CASH_ON_DELIVERY,
                        ],
                    ),
                ],
            ),
            ShopPaymentMethods(
                fesh=31,
                payment_groups=[
                    PaymentRegionalGroup(
                        included_regions=[225],
                        payment_methods=[
                            Payment.PT_PREPAYMENT_CARD,
                            Payment.PT_PREPAYMENT_OTHER,
                            Payment.PT_CARD_ON_DELIVERY,
                            Payment.PT_CASH_ON_DELIVERY,
                        ],
                    ),
                ],
            ),
            ShopPaymentMethods(
                fesh=32,
                payment_groups=[
                    PaymentRegionalGroup(
                        included_regions=[225],
                        payment_methods=[
                            Payment.PT_PREPAYMENT_CARD,
                            Payment.PT_PREPAYMENT_OTHER,
                            Payment.PT_CARD_ON_DELIVERY,
                            Payment.PT_CASH_ON_DELIVERY,
                        ],
                    ),
                ],
            ),
            ShopPaymentMethods(
                fesh=41,
                payment_groups=[
                    PaymentRegionalGroup(
                        included_regions=[225],
                        payment_methods=[
                            Payment.PT_CASH_ON_DELIVERY,
                        ],
                    ),
                ],
            ),
            ShopPaymentMethods(
                fesh=42,
                payment_groups=[
                    PaymentRegionalGroup(
                        included_regions=[225],
                        payment_methods=[
                            # Payment.PT_CARD_ON_DELIVERY,
                            Payment.PT_CASH_ON_DELIVERY,
                        ],
                    ),
                ],
            ),
            ShopPaymentMethods(
                fesh=43,
                payment_groups=[
                    PaymentRegionalGroup(
                        included_regions=[225],
                        payment_methods=[Payment.PT_YANDEX],
                    ),
                ],
            ),
            ShopPaymentMethods(
                fesh=43,
                payment_groups=[
                    PaymentRegionalGroup(
                        included_regions=[225],
                        payment_methods=[Payment.PT_YANDEX],
                    ),
                ],
            ),
        ]

        cls.settings.lms_autogenerate = False

        cls.dynamic.lms += [
            DynamicWarehouseInfo(id=145, home_region=213),
            DynamicWarehouseInfo(id=146, home_region=213, is_express=True),
            DynamicWarehouseToWarehouseInfo(warehouse_from=145, warehouse_to=145),
            DynamicWarehouseToWarehouseInfo(warehouse_from=146, warehouse_to=146),
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=145,
                delivery_service_id=157,
                date_switch_time_infos=[DateSwitchTimeAndRegionInfo(date_switch_hour=23, region_to=225)],
            ),
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=146,
                delivery_service_id=157,
                date_switch_time_infos=[DateSwitchTimeAndRegionInfo(date_switch_hour=23, region_to=225)],
            ),
            DynamicDeliveryServiceInfo(id=157, rating=2),
            DynamicWarehousesPriorityInRegion(region=225, warehouses=[145, 146]),
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=1102,
                dc_bucket_id=1102,
                fesh=41,
                regional_options=[
                    RegionalDelivery(
                        rid=225,
                        options=[DeliveryOption(price=200, day_from=2, day_to=3, shop_delivery_price=5)],
                        payment_methods=[Payment.PT_CASH_ON_DELIVERY],
                    )
                ],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            DeliveryBucket(
                bucket_id=1103,
                dc_bucket_id=1103,
                fesh=42,
                # dc_15=True,
                regional_options=[
                    RegionalDelivery(
                        rid=225,
                        options=[DeliveryOption(price=500, day_from=3, day_to=4)],
                        payment_methods=[Payment.PT_CASH_ON_DELIVERY],
                    )
                ],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            DeliveryBucket(
                bucket_id=1234,
                carriers=[157],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                regional_options=[
                    RegionalDelivery(
                        rid=225,
                        options=[DeliveryOption(price=15, shop_delivery_price=15, day_from=1, day_to=2)],
                        payment_methods=[Payment.PT_CARD_ON_DELIVERY, Payment.PT_CASH_ON_DELIVERY, Payment.PT_YANDEX],
                    ),
                ],
            ),
            DeliveryBucket(
                bucket_id=1235,
                carriers=[157],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                regional_options=[
                    RegionalDelivery(
                        rid=225,
                        options=[DeliveryOption(price=45, shop_delivery_price=45, day_from=3, day_to=5)],
                        payment_methods=[Payment.PT_PREPAYMENT_CARD, Payment.PT_CASH_ON_DELIVERY, Payment.PT_YANDEX],
                    ),
                ],
            ),
        ]

        cls.index.mskus += [
            MarketSku(
                hyperid=1,
                hid=1,
                sku=1,
                delivery_buckets=[1234],
                blue_offers=[
                    BlueOffer(
                        price=2100,
                        feedid=11,
                        waremd5='OFF1_2100_SKU1_SUP11_Q',
                        randx=1100,
                        blue_weight=10,
                        dimensions=OfferDimensions(length=5, width=1, height=3),
                    ),
                    BlueOffer(
                        price=2050,
                        feedid=12,
                        waremd5='OFF1_2050_SKU1_SUP12_Q',
                        randx=1200,
                        blue_weight=10,
                        dimensions=OfferDimensions(length=5, width=1, height=3),
                    ),
                ],
            ),
            MarketSku(
                hyperid=1,
                hid=1,
                sku=2,
                delivery_buckets=[1234],
                blue_offers=[
                    BlueOffer(
                        price=1900,
                        feedid=11,
                        waremd5='OFF1_1900_SKU2_SUP11_Q',
                        randx=1100,
                        blue_weight=10,
                        dimensions=OfferDimensions(length=5, width=1, height=3),
                    ),
                    BlueOffer(
                        price=2050,
                        feedid=12,
                        waremd5='OFF1_2050_SKU2_SUP12_Q',
                        randx=1200,
                        blue_weight=10,
                        dimensions=OfferDimensions(length=5, width=1, height=3),
                    ),
                ],
            ),
            MarketSku(
                hyperid=2,
                hid=2,
                sku=3,
                delivery_buckets=[1234],
                buybox_elasticity=[
                    Elasticity(price_variant=2100, demand_mean=200),
                    Elasticity(price_variant=2200, demand_mean=80),
                    Elasticity(price_variant=2500, demand_mean=30),
                ],
                blue_offers=[
                    BlueOffer(
                        price=2100,
                        feedid=43,
                        waremd5='EXPR_2100_SKU3_SUP43_Q',
                        randx=421,
                        blue_weight=11,
                        dimensions=OfferDimensions(length=5, width=1, height=3),
                        is_express=True,
                    ),
                    BlueOffer(
                        price=2200,
                        feedid=44,
                        waremd5='EXPR_2200_SKU3_SUP44_Q',
                        randx=421,
                        blue_weight=11,
                        dimensions=OfferDimensions(length=5, width=1, height=3),
                        is_express=False,
                    ),
                ],
            ),
            MarketSku(
                hyperid=4,
                hid=4,
                sku=4,
                delivery_buckets=[1234],
                blue_offers=[
                    BlueOffer(
                        price=1800,
                        feedid=12,
                        waremd5='OFF1_1800_SKU4_SUP12_Q',
                        randx=1200,
                        blue_weight=10,
                        dimensions=OfferDimensions(length=5, width=1, height=3),
                    )
                ],
            ),
            MarketSku(
                hyperid=5,
                hid=5,
                sku=5,
                delivery_buckets=[1234],
                buybox_elasticity=[
                    Elasticity(price_variant=2000, demand_mean=200),
                    Elasticity(price_variant=2200, demand_mean=80),
                    Elasticity(price_variant=2500, demand_mean=30),
                ],
                blue_offers=[
                    BlueOffer(
                        price=2000,
                        feedid=feedid,
                        waremd5='OFF2_2000_SKU5_SUP{}_Q'.format(feedid),
                        randx=1000 * feedid,
                        blue_weight=11,
                        dimensions=OfferDimensions(length=5, width=1, height=3),
                        fee=10 * feedid,
                    )
                    for feedid in [11, 12, 31, 32]
                ],
            ),
            MarketSku(
                hyperid=6,
                hid=6,
                sku=6,
                delivery_buckets=[1234],
                buybox_elasticity=[
                    Elasticity(price_variant=2000, demand_mean=200),
                    Elasticity(price_variant=2200, demand_mean=80),
                    Elasticity(price_variant=2500, demand_mean=30),
                ],
                blue_offers=[
                    BlueOffer(
                        price=2000,
                        feedid=45,
                        waremd5='OFF2_2000_SKU6_SUP45_Q',
                        randx=421,
                        blue_weight=11,
                        dimensions=OfferDimensions(length=5, width=1, height=3),
                        is_express=True,
                    ),
                    BlueOffer(
                        price=2000,
                        feedid=46,
                        waremd5='OFF2_2000_SKU6_SUP46_Q',
                        randx=421,
                        blue_weight=11,
                        dimensions=OfferDimensions(length=5, width=1, height=3),
                        is_express=False,
                    ),
                    BlueOffer(
                        price=3000,
                        feedid=47,
                        waremd5='OFF2_3000_SKU6_SUP47_Q',
                        installment_info=OfferInstallmentInfo(days=[180], bnpl_available=True),
                        randx=421,
                        blue_weight=11,
                        dimensions=OfferDimensions(length=5, width=1, height=3),
                        is_express=False,
                    ),
                ],
            ),
        ]

        cls.index.offers += [
            Offer(
                title="DSBS 1 sku 1",
                hid=1,
                hyperid=1,
                price=1800,
                fesh=41,
                business_id=41,
                sku=1,
                cpa=Offer.CPA_REAL,
                waremd5='DSBS_1_1800_WITH_DELIV',
                delivery_buckets=[1102],
            ),
            Offer(
                title="DSBS 2 sku 1",
                hid=1,
                hyperid=1,
                price=1850,
                fesh=42,
                business_id=42,
                sku=1,
                cpa=Offer.CPA_REAL,
                waremd5='DSBS_1_1850_WITH_DELIV',
                delivery_buckets=[1103],
            ),
            Offer(
                title="DSBS 1 sku 2",
                hid=1,
                hyperid=1,
                price=2000,
                fesh=41,
                business_id=41,
                sku=2,
                cpa=Offer.CPA_REAL,
                waremd5='DSBS_2_2000_WITH_DELIV',
                delivery_buckets=[1102],
            ),
            Offer(
                title="DSBS 2 sku 2",
                hid=1,
                hyperid=1,
                price=2150,
                fesh=42,
                business_id=42,
                sku=2,
                cpa=Offer.CPA_REAL,
                waremd5='DSBS_2_2150_WITH_DELIV',
                delivery_buckets=[1103],
            ),
            Offer(
                title="DSBS 1 sku 4",
                hid=4,
                hyperid=4,
                price=2000,
                fesh=41,
                business_id=41,
                sku=4,
                cpa=Offer.CPA_REAL,
                waremd5='DSBS_4_2000_WITH_DELIV',
                delivery_buckets=[1102],
                fee=100,
            ),
            Offer(
                title="DSBS 2 sku 4",
                hid=4,
                hyperid=4,
                price=2150,
                fesh=42,
                business_id=42,
                sku=4,
                cpa=Offer.CPA_REAL,
                waremd5='DSBS_4_2150_WITH_DELIV',
                delivery_buckets=[1103],
                fee=200,
            ),
            Offer(
                title="market DSBS Offer",
                hid=5,
                hyperid=5,
                price=1800,
                fesh=41,
                business_id=41,
                sku=5,
                cpa=Offer.CPA_REAL,
                waremd5='DSBS_5_1800_WITH_DELIV',
                delivery_buckets=[1102],
                fee=50,
            ),
            Offer(
                title="market DSBS Offer",
                hid=5,
                hyperid=5,
                price=2000,
                fesh=42,
                business_id=42,
                sku=5,
                cpa=Offer.CPA_REAL,
                waremd5='DSBS_5_2000_WITH_DELIV',
                delivery_buckets=[1103],
                fee=50,
            ),
            Offer(
                title="market DSBS Offer",
                hid=6,
                hyperid=6,
                price=1800,
                fesh=41,
                business_id=41,
                sku=6,
                cpa=Offer.CPA_REAL,
                waremd5='DSBS_6_1800_WITH_DELIV',
                delivery_buckets=[1102],
                fee=50,
            ),
            Offer(
                title="market DSBS Offer",
                hid=6,
                hyperid=6,
                price=2000,
                fesh=42,
                business_id=42,
                sku=6,
                cpa=Offer.CPA_REAL,
                waremd5='DSBS_6_2000_WITH_DELIV',
                delivery_buckets=[1103],
                fee=50,
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
                shop_id=32,
                crossdock_late_ship_rate=5.7,
                crossdock_plan_fact_rate=1.91,
                crossdock_return_rate=0.2,
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

        cls.index.recommended_fee += [
            RecommendedFee(hyper_id=4, recommended_bid=0.014),
        ]

        cls.index.reserveprice_fee += [
            ReservePriceFee(hyper_id=4, reserveprice_fee=0.007),
        ]

    @classmethod
    def prepare_nordstream(cls):
        cls.settings.nordstream_autogenerate = False
        for warehouse_id in (145, 146):
            cls.dynamic.nordstream += [DynamicWarehouseLink(warehouse_id, [warehouse_id])]
            cls.dynamic.nordstream += [
                DynamicWarehouseDelivery(
                    warehouse_id,
                    {
                        225: [
                            DynamicDeliveryRestriction(min_days=1, max_days=2, cost=15),
                        ],
                        227: [
                            DynamicDeliveryRestriction(min_days=3, max_days=5, cost=48),
                        ],
                        229: [
                            DynamicDeliveryRestriction(min_days=4, max_days=6, cost=58),
                        ],
                    },
                ),
            ]

    def test_intents_fast_delivery_slot(self):
        """Проверяем что правильно выставляются интенты FastDelivery и FasterDelivery в слот доставки (1 слот в top6)"""
        """https://st.yandex-team.ru/MADV-13"""

        # Для мскю=1 у ДО медленная доставка и есть оффера с доставкой быстрее поэтому интент ставится таким офферам "fasterDelivery"
        response = self.report.request_json(
            'place=productoffers&rgb=green_with_blue&market-sku=1&rids=213&offers-set=intents&pp=6&debug=da'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'wareId': 'OFF1_2050_SKU1_SUP12_Q',
                        'intents': ['fasterDelivery'],
                    },
                    {
                        'wareId': 'OFF1_2100_SKU1_SUP11_Q',
                        'intents': ['marketDelivery'],
                    },
                    {
                        'wareId': 'DSBS_1_1800_WITH_DELIQ',
                        'intents': ['highShopRating'],
                    },
                    {
                        'wareId': 'DSBS_1_1850_WITH_DELIQ',
                    },
                    {
                        'wareId': 'DSBS_1_1800_WITH_DELIQ',
                        'benefit': {'type': 'default'},
                    },
                ]
            },
            preserve_order=True,
        )

        # Для мскю=2 быстрее чем ДО офферов нет, но есть с таким же временем доставки и таким офферам ставится интент "fastDelivery"
        response = self.report.request_json(
            'place=productoffers&rgb=green_with_blue&market-sku=2&rids=213&offers-set=intents&pp=6&debug=da'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'wareId': 'OFF1_2050_SKU2_SUP12_Q',
                        'intents': ['fastDelivery'],
                    },
                    {
                        'wareId': 'OFF1_1900_SKU2_SUP11_Q',
                        'intents': ['marketDelivery'],
                    },
                    {
                        'wareId': 'DSBS_2_2000_WITH_DELIQ',
                        'intents': ['highShopRating'],
                    },
                    {
                        'wareId': 'DSBS_2_2150_WITH_DELIQ',
                    },
                    {
                        'wareId': 'OFF1_1900_SKU2_SUP11_Q',
                        'benefit': {'type': "default"},
                    },
                ]
            },
            preserve_order=True,
        )

    def test_intents_fast_delivery_slot_with_express_do(self):
        """Проверяем что слот доставки в top6 пропускается если есть экспресс ДО"""
        """https://st.yandex-team.ru/MADV-13"""

        response = self.report.request_json(
            'place=productoffers&rgb=green_with_blue&market-sku=3&rids=213&offers-set=intents&pp=6&debug=da'
            '&rearr-factors=market_rearrange_top6_by_intents_skip_express=0'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'wareId': 'EXPR_2100_SKU3_SUP43_Q',
                        'intents': ['marketDelivery'],
                    },
                    {
                        'wareId': 'EXPR_2200_SKU3_SUP44_Q',
                        'intents': ['paymentByCard'],
                    },
                    {
                        'wareId': 'EXPR_2100_SKU3_SUP43_Q',
                        'benefit': {'type': "default"},
                    },
                    {
                        'wareId': 'EXPR_2100_SKU3_SUP43_Q',
                        'benefit': {'type': "express-cpa"},
                    },
                ]
            },
            preserve_order=True,
        )

    def test_intents_fast_delivery_slot_with_express_do_skip_express_flag(self):
        """Проверяем что слот доставки в top6 пропускается если есть экспресс ДО"""
        """И что оффер выигравший экспресс ДО не появляется в топ6"""
        """https://st.yandex-team.ru/MADV-13"""

        response = self.report.request_json(
            'place=productoffers&rgb=green_with_blue&market-sku=3&rids=213&offers-set=intents&pp=6&debug=da'
            '&rearr-factors=market_rearrange_top6_by_intents_skip_express=1'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'wareId': 'EXPR_2200_SKU3_SUP44_Q',
                        'intents': ['marketDelivery'],
                    },
                    {
                        'wareId': 'EXPR_2100_SKU3_SUP43_Q',
                        'benefit': {'type': "default"},
                    },
                    {
                        'wareId': 'EXPR_2100_SKU3_SUP43_Q',
                        'benefit': {'type': "express-cpa"},
                    },
                ]
            },
            preserve_order=True,
        )

    def test_intents_fast_delivery_slot_without_do(self):
        """Проверяем что слот доставки пропускается если у офферов в топ6 время доставки больше чем у ДО"""
        """https://st.yandex-team.ru/MADV-13"""
        # Включил для hid=4 порог по reserve price в 0.7% чтобы отфильтровать из топ6 оффер выигравший ДО
        # Таким образом оставшиеся оффера в топ6 имеют доставку более медленную чем ДО и слот доставки пропускается
        # Здесь же покажу еще один важный нюанс: амнистия считается для изначального порядка топ6 без учета интентов
        # А после пересортировки по интентам амнистия не пересчитывается
        # Ставка DSBS_4_2150_WITH_DELIQ амнистировалась об оффер DSBS_4_2000_WITH_DELIQ, но интенты поменяли оффера местами

        response = self.report.request_json(
            'place=productoffers&rgb=green_with_blue&market-sku=4&rids=213&offers-set=intents&pp=6&debug=da'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'wareId': 'DSBS_4_2000_WITH_DELIQ',
                        'intents': ['highShopRating'],
                        'debug': {
                            "sale": {
                                "shopFee": 100,
                                "brokeredFee": 70,
                            },
                        },
                    },
                    {
                        'wareId': 'DSBS_4_2150_WITH_DELIQ',
                        'debug': {
                            "sale": {
                                "shopFee": 200,
                                "brokeredFee": 98,
                            },
                        },
                    },
                    {
                        'wareId': 'OFF1_1800_SKU4_SUP12_Q',
                        'benefit': {'type': "default"},
                    },
                ]
            },
            preserve_order=True,
        )

    def test_intents_high_shop_rating_slot(self):
        """Проверяем что интент highShopRating реагирует на флаг market_rearrange_top6_by_intents_min_shop_rating"""
        """https://st.yandex-team.ru/MADV-13"""

        response = self.report.request_json(
            'place=productoffers&rgb=green_with_blue&market-sku=2&rids=213&offers-set=intents&pp=6&debug=da'
            '&rearr-factors=market_rearrange_top6_by_intents_min_shop_rating=90.0'
        )
        self.assertFragmentIn(
            response,
            {
                'wareId': 'DSBS_2_2000_WITH_DELIQ',
                'intents': ['highShopRating'],
            },
        )

        response = self.report.request_json(
            'place=productoffers&rgb=green_with_blue&market-sku=2&rids=213&offers-set=intents&pp=6&debug=da'
            '&rearr-factors=market_rearrange_top6_by_intents_min_shop_rating=95.0'
        )
        self.assertFragmentNotIn(
            response,
            {
                'wareId': 'DSBS_2_2000_WITH_DELIQ',
                'intents': ['highShopRating'],
            },
        )

    def test_intents_tail(self):
        """Проверяем что когда все слоты заняты то оставшиеся оффера добавляются в конец сохраняя первоначальный порядок"""
        """https://st.yandex-team.ru/MADV-13"""
        # Оставшиеся места заполняем первыми кандидатами из оставшегося списка топ 6. При этом, если они имеют несколько интентов то берем первый по приоритету.
        # Один мерч не может занимать более 1 слота

        response = self.report.request_json(
            'place=productoffers&rgb=green_with_blue&market-sku=5&rids=213&offers-set=intents&pp=6&debug=da'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'wareId': 'OFF2_2000_SKU5_SUP32_Q',
                        'intents': ['fasterDelivery'],  # fast delivery slot
                    },
                    {
                        'wareId': 'OFF2_2000_SKU5_SUP31_Q',
                        'intents': ['marketDelivery'],  # market delivery slot
                    },
                    {
                        'wareId': 'OFF2_2000_SKU5_SUP12_Q',
                        'intents': ['paymentByCard'],  # card payment slot
                    },
                    {
                        'wareId': 'DSBS_5_1800_WITH_DELIQ',
                        'intents': ['highShopRating'],  # shop rating slot
                    },
                    {
                        'wareId': 'DSBS_5_2000_WITH_DELIQ',  # tail
                    },
                    {
                        'wareId': 'OFF2_2000_SKU5_SUP11_Q',
                        'intents': ['fasterDelivery'],  # tail
                    },
                ]
            },
            preserve_order=True,
        )

    def test_intents_debug_and_log(self):
        """Проверяем, что в debug и в shows log выводится информация об интентах"""

        response = self.report.request_json(
            'place=productoffers&rgb=green_with_blue&market-sku=5&rids=213&offers-set=intents&pp=6&debug=da&rearr-factors=market_shows_log_for_top6_intents=1'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'wareId': 'OFF2_2000_SKU5_SUP32_Q',
                        'intents': ['fasterDelivery'],  # fast delivery slot
                        'debug': {
                            'intents': {
                                'ChosenIntent': 'FasterDelivery',
                                'OtherIntents': 'ReliableSupplier;PaymentByCard',
                                'DOBeforeReordering': 'OFF2_2000_SKU5_SUP32_Q',
                                'DODaysTo': 3,
                                'MinDaysTo': 2,
                                'DaysTo': 2,
                                'PositionBeforeRearrange': 0,
                                'IsFromSlot': True,
                                'IsCehacIntents': False,
                                'RegionId': 213,
                                'ShopRatingTotal': 99.8,
                                'HasExpressDelivery': False,
                                'AllPaymentMethodsMask': 7,
                                'HasPrepaymentCard': False,
                                'HasPaymentCardOnDelivery': True,
                            }
                        },
                    },
                    {
                        'wareId': 'OFF2_2000_SKU5_SUP31_Q',
                        'intents': ['marketDelivery'],  # market delivery slot
                        'debug': {
                            'intents': {
                                'ChosenIntent': 'MarketDelivery',
                                'OtherIntents': 'ReliableSupplier;FasterDelivery;PaymentByCard',
                                'DOBeforeReordering': 'OFF2_2000_SKU5_SUP31_Q',
                                'DODaysTo': 3,
                                'MinDaysTo': 2,
                                'DaysTo': 2,
                                'PositionBeforeRearrange': 1,
                                'IsFromSlot': True,
                                'IsCehacIntents': False,
                                'RegionId': 213,
                                'ShopRatingTotal': 99.8,
                                'HasExpressDelivery': False,
                                'AllPaymentMethodsMask': 7,
                                'HasPrepaymentCard': False,
                                'HasPaymentCardOnDelivery': True,
                            }
                        },
                    },
                    {
                        'wareId': 'OFF2_2000_SKU5_SUP12_Q',
                        'intents': ['paymentByCard'],  # card payment slot
                        'debug': {
                            'intents': {
                                'ChosenIntent': 'PaymentByCard',
                                'OtherIntents': 'MarketDelivery;FasterDelivery',
                                'DOBeforeReordering': 'OFF2_2000_SKU5_SUP12_Q',
                                'DODaysTo': 3,
                                'MinDaysTo': 2,
                                'DaysTo': 2,
                                'PositionBeforeRearrange': 2,
                                'IsFromSlot': True,
                                'IsCehacIntents': False,
                                'RegionId': 213,
                                'ShopRatingTotal': -1,
                                'HasExpressDelivery': False,
                                'AllPaymentMethodsMask': 7,
                                'HasPrepaymentCard': False,
                                'HasPaymentCardOnDelivery': True,
                            }
                        },
                    },
                    {
                        'wareId': 'DSBS_5_1800_WITH_DELIQ',
                        'intents': ['highShopRating'],  # shop rating slot
                        'debug': {
                            'intents': {
                                'ChosenIntent': 'HighShopRating',
                                'OtherIntents': 'Cheapest;Default',
                                'DOBeforeReordering': 'DSBS_5_2000_WITH_DELIQ',
                                'DODaysTo': 3,
                                'MinDaysTo': 2,
                                'DaysTo': 3,
                                'PositionBeforeRearrange': 4,
                                'IsFromSlot': True,
                                'IsCehacIntents': False,
                                'RegionId': 213,
                                'ShopRatingTotal': 94.8,
                                'HasExpressDelivery': False,
                                'AllPaymentMethodsMask': 2,
                                'HasPrepaymentCard': False,
                                'HasPaymentCardOnDelivery': False,
                            }
                        },
                    },
                    {
                        'wareId': 'DSBS_5_2000_WITH_DELIQ',  # tail
                        'debug': {
                            'intents': {
                                'ChosenIntent': 'Empty',
                                'OtherIntents': 'Empty',
                                'DOBeforeReordering': 'DSBS_5_1800_WITH_DELIQ',
                                'DODaysTo': 3,
                                'MinDaysTo': 2,
                                'DaysTo': 4,
                                'PositionBeforeRearrange': 3,
                                'IsFromSlot': False,
                                'IsCehacIntents': False,
                                'RegionId': 213,
                                'ShopRatingTotal': -1,
                                'HasExpressDelivery': False,
                                'AllPaymentMethodsMask': 2,
                                'HasPrepaymentCard': False,
                                'HasPaymentCardOnDelivery': False,
                            }
                        },
                    },
                    {
                        'wareId': 'OFF2_2000_SKU5_SUP11_Q',
                        'intents': ['fasterDelivery'],  # tail
                        'debug': {
                            'intents': {
                                'ChosenIntent': 'FasterDelivery',
                                'OtherIntents': 'ReliableSupplier;PaymentByCard',  # 1p is always a highly rated store
                                'DOBeforeReordering': 'OFF2_2000_SKU5_SUP11_Q',
                                'DODaysTo': 3,
                                'MinDaysTo': 2,
                                'DaysTo': 2,
                                'PositionBeforeRearrange': 5,
                                'IsFromSlot': False,
                                'IsCehacIntents': False,
                                'RegionId': 213,
                                'ShopRatingTotal': -1,
                                'HasExpressDelivery': False,
                                'AllPaymentMethodsMask': 7,
                                'HasPrepaymentCard': False,
                                'HasPaymentCardOnDelivery': True,
                            }
                        },
                    },
                    {
                        'benefit': {'type': 'default'},  # Default offer (no intents)
                        'debug': {
                            'intents': Absent(),
                        },
                    },
                ],
            },
            preserve_order=True,
        )

        self.show_log_tskv.expect(
            ware_md5="OFF2_2000_SKU5_SUP32_Q",
            intent_chosen="Faster",
            intents_secondary="RSupp;Card",
            intents_pos=0,
            intents_pos_before_rearrange=0,
            intents_is_slot=1,
        )
        self.show_log_tskv.expect(
            ware_md5="OFF2_2000_SKU5_SUP31_Q",
            intent_chosen="MD",
            intents_secondary="RSupp;Faster;Card",
            intents_pos=1,
            intents_pos_before_rearrange=1,
            intents_is_slot=1,
        )
        self.show_log_tskv.expect(
            ware_md5="OFF2_2000_SKU5_SUP12_Q",
            intent_chosen="Card",
            intents_secondary="MD;Faster",
            intents_pos=2,
            intents_pos_before_rearrange=2,
            intents_is_slot=1,
        )
        self.show_log_tskv.expect(
            ware_md5="DSBS_5_1800_WITH_DELIQ",
            intent_chosen="HSRat",
            intents_secondary="Cheap;Dflt",
            intents_pos=3,
            intents_pos_before_rearrange=4,
            intents_is_slot=1,
        )
        self.show_log_tskv.expect(
            ware_md5="DSBS_5_2000_WITH_DELIQ",
            intent_chosen="E",
            intents_secondary="E",
            intents_pos=4,
            intents_pos_before_rearrange=3,
            intents_is_slot=0,
        )
        self.show_log_tskv.expect(
            ware_md5="OFF2_2000_SKU5_SUP11_Q",
            intent_chosen="Faster",
            intents_secondary="RSupp;Card",
            intents_pos=5,
            intents_pos_before_rearrange=5,
            intents_is_slot=0,
        )
        self.show_log_tskv.expect(
            ware_md5="DSBS_5_1800_WITH_DELIQ",
            intent_chosen=None,
            intents_secondary=None,
            intents_pos=None,
            intents_pos_before_rearrange=None,
            intents_is_slot=None,
        )

    def test_logs_off_by_flag(self):
        """Проверяем, что в shows log информация об интентах не логируется по умолчанию"""

        self.report.request_json(
            'place=productoffers&rgb=green_with_blue&market-sku=5&rids=213&offers-set=intents&pp=6'
        )
        self.show_log_tskv.expect(
            intent_chosen=None,
            intents_secondary=None,
            intents_pos=None,
            intents_pos_before_rearrange=None,
            intents_is_slot=None,
        ).times(14)

    def test_intents_fast_delivery_in_debug_and_logs(self):
        """Проверяем, что правильно логируются и записываются в дебаг интенты FastDelivery (в другом тесте проверены FasterDelivery)"""

        # Для мскю=2 быстрее чем ДО офферов нет, но есть с таким же временем доставки и таким офферам ставится интент "fastDelivery"
        response = self.report.request_json(
            'place=productoffers&rgb=green_with_blue&market-sku=2&rids=213&offers-set=intents&pp=6&debug=da&rearr-factors=market_shows_log_for_top6_intents=1'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'wareId': 'OFF1_2050_SKU2_SUP12_Q',
                        'intents': ['fastDelivery'],
                        'debug': {
                            'intents': {
                                'ChosenIntent': 'FastDelivery',
                                'OtherIntents': 'MarketDelivery;PaymentByCard',
                                'DOBeforeReordering': 'OFF1_2050_SKU2_SUP12_Q',
                                'DODaysTo': 2,
                                'MinDaysTo': 2,
                                'DaysTo': 2,
                                'PositionBeforeRearrange': 0,
                                'IsFromSlot': True,
                                'IsCehacIntents': False,
                                'RegionId': 213,
                                'ShopRatingTotal': -1,
                                'HasExpressDelivery': False,
                                'AllPaymentMethodsMask': 7,
                                'HasPrepaymentCard': False,
                                'HasPaymentCardOnDelivery': True,
                            },
                        },
                    },
                ]
            },
        )

        self.show_log_tskv.expect(
            ware_md5="OFF1_2050_SKU2_SUP12_Q",
            intent_chosen="FD",
            intents_secondary="MD;Card",
            intents_pos=0,
            intents_pos_before_rearrange=0,
            intents_is_slot=1,
        )

    def test_cehac_intents(self):
        """Проверяем что слоты для СЕНАС заполняются"""
        """https://st.yandex-team.ru/MADV-13"""
        # У сенас только слоты, не попавшие в них оффера отбрасываются

        response = self.report.request_json(
            'place=productoffers&rgb=green_with_blue&market-sku=5&rids=213&offers-set=cehacIntents&pp=6&debug=da'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    # {
                    #     'wareId': 'DSBS_5_1800_WITH_DELIQ',
                    #     'intents': ['cheapest'], # тоже самое что и ДО
                    # },
                    {
                        'wareId': 'OFF2_2000_SKU5_SUP32_Q',
                        'intents': ['fasterDelivery'],
                    },
                    {
                        'wareId': 'OFF2_2000_SKU5_SUP31_Q',
                        'intents': ['reliableSupplier'],
                    },
                    {
                        'wareId': 'DSBS_5_1800_WITH_DELIQ',
                        'benefit': {'type': "default"},
                    },
                ]
            },
            preserve_order=True,
        )

    def test_cehac_intents_with_express(self):
        """Проверяем что дефолтный слот экспресс для Сенас отсутсвует"""
        """https://st.yandex-team.ru/MADV-13"""
        # У сенас только слоты, не попавшие в них оффера отбрасываются
        # Один оффер не может быть в нескольких слотах

        response = self.report.request_json(
            'place=productoffers&rgb=green_with_blue&market-sku=3&rids=213&offers-set=cehacIntents&pp=6&debug=da'
        )
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'wareId': 'EXPR_2100_SKU3_SUP43_Q',
                        'benefit': {'type': "default"},
                    },
                ]
            },
            preserve_order=True,
        )

        self.assertFragmentNotIn(
            response,
            {
                'results': [
                    {
                        'wareId': 'EXPR_2100_SKU3_SUP43_Q',
                        'benefit': {'type': "express-cpa"},
                    },
                ]
            },
            preserve_order=True,
        )

    def test_cehac_intents_express_and_installment_intent(self):
        """Проверяем что у нас есть интент экспресс и рассрочка"""

        response = self.report.request_json(
            'place=productoffers&rgb=green_with_blue&market-sku=6&rids=213&offers-set=cehacIntents&pp=6&debug=da'
        )

        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'wareId': 'OFF2_2000_SKU6_SUP45_Q',
                        'intents': ['express'],
                    },
                    {
                        'wareId': 'OFF2_3000_SKU6_SUP47_Q',
                        'intents': ['installmentAvailable'],
                    },
                ]
            },
            preserve_order=True,
        )


if __name__ == '__main__':
    main()
