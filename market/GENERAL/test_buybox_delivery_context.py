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
    DynamicWarehouseLink,
    DynamicWarehousesPriorityInRegion,
    Elasticity,
    MarketSku,
    Offer,
    OfferDimensions,
    Region,
    RegionalDelivery,
    Shop,
    Tax,
)
from core.types.delivery import BlueDeliveryTariff
from core.matcher import Capture, NotEmpty
from core.types.hypercategory import (
    EATS_CATEG_ID,
    CategoryStreamRecord,
    Stream,
)
from core.types.express_partners import EatsWarehousesEncoder

CATEGORY_FOOD = EATS_CATEG_ID
PEPSI_SKU = 25


def dict_to_rearr(rearr_flags):
    result = ""
    for key in rearr_flags.keys():
        result += str(key) + "=" + str(rearr_flags[key]) + ";"
    return result


class T(TestCase):
    @classmethod
    def prepare(cls):

        cls.settings.market_access_settings.enabled = True
        cls.settings.market_access_settings.download_catstreams = True
        cls.index.category_streams += [
            CategoryStreamRecord(CATEGORY_FOOD, Stream.FMCG.value),
        ]

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
                name="Другой 1P поставщик",
                supplier_type=Shop.FIRST_PARTY,
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
                is_supplier=True,
                blue=Shop.BLUE_REAL,
                warehouse_id=145,
                fulfillment_program=True,
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
            ),
            Shop(fesh=41, business_fesh=41, name="dsbs магазин Пети-41", regions=[213], cpa=Shop.CPA_REAL),
            Shop(fesh=42, business_fesh=42, name="dsbs магазин Пети-42", regions=[213], cpa=Shop.CPA_REAL),
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
            Shop(
                fesh=45,
                datafeed_id=45,
                priority_region=213,
                regions=[225],
                name="Eda Retail Поставщик",
                cpa=Shop.CPA_REAL,
                warehouse_id=146,
                is_eats=True,
            ),
        ]

        cls.settings.lms_autogenerate = False

        cls.dynamic.lms += [
            DynamicWarehouseInfo(id=145, home_region=213),
            DynamicWarehouseInfo(id=146, home_region=213, is_express=True),
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

        cls.index.mskus += [
            MarketSku(
                hyperid=1,
                hid=1,
                sku=1,
                delivery_buckets=[1234],
                blue_offers=[
                    BlueOffer(
                        price=2100,
                        feedid=feedid,
                        waremd5='OFF1_2100_SKU1_SUP{}_Q'.format(feedid),
                        randx=100 * feedid,
                        blue_weight=10,
                        dimensions=OfferDimensions(length=543, width=175, height=357),
                    )
                    for feedid in [11, 12]
                ]
                + [
                    BlueOffer(
                        price=2000,
                        feedid=feedid,
                        waremd5='OFF2_2000_SKU1_SUP{}_Q'.format(feedid),
                        randx=200 * feedid,
                        blue_weight=11,
                        dimensions=OfferDimensions(length=5, width=1, height=3),
                    )
                    for feedid in [11, 12, 31, 32]
                ],
            ),
            MarketSku(
                hyperid=1,
                hid=1,
                sku=2,
                delivery_buckets=[1235],
                blue_offers=[
                    BlueOffer(
                        price=2000,
                        feedid=feedid,
                        waremd5='OFF1_2000_SKU2_SUP{}_Q'.format(feedid),
                        randx=300 * feedid,
                        blue_weight=13,
                        dimensions=OfferDimensions(length=543, width=175, height=357),
                    )
                    for feedid in [11, 12]
                ]
                + [
                    BlueOffer(
                        price=2100,
                        feedid=feedid,
                        waremd5='OFF2_2100_SKU2_SUP{}_Q'.format(feedid),
                        randx=400 * feedid,
                        blue_weight=20,
                        dimensions=OfferDimensions(length=5, width=1, height=3),
                    )
                    for feedid in [11, 12, 31, 32]
                ],
            ),
            MarketSku(
                hyperid=1,
                hid=1,
                sku=3,
                blue_offers=[
                    BlueOffer(
                        price=2000,
                        feedid=feedid,
                        waremd5='OFF1_2000_SKU3_SUP{}_Q'.format(feedid),
                        randx=300 * feedid,
                        blue_weight=13,
                        dimensions=OfferDimensions(length=543, width=175, height=357),
                    )
                    for feedid in [11, 12]
                ]
                + [
                    BlueOffer(
                        price=2100,
                        feedid=feedid,
                        waremd5='OFF2_2100_SKU3_SUP{}_Q'.format(feedid),
                        randx=400 * feedid,
                        blue_weight=20,
                        dimensions=OfferDimensions(length=5, width=1, height=3),
                    )
                    for feedid in [11, 12, 31, 32]
                ],
            ),
            MarketSku(
                hyperid=1,
                hid=1,
                sku=4,
                delivery_buckets=[1234],
                blue_offers=[
                    BlueOffer(
                        price=1000,
                        feedid=feedid,
                        waremd5='OFF1_1000_SKU4_SUP{}_Q'.format(feedid),
                        randx=500 * feedid,
                        blue_weight=13,
                        dimensions=OfferDimensions(length=543, width=175, height=357),
                    )
                    for feedid in [11]
                ]
                + [
                    BlueOffer(
                        price=2000,
                        feedid=feedid,
                        waremd5='OFF2_2000_SKU4_SUP{}_Q'.format(feedid),
                        randx=600 * feedid,
                        blue_weight=20,
                        dimensions=OfferDimensions(length=5, width=1, height=3),
                    )
                    for feedid in [11]
                ],
            ),
            MarketSku(
                hyperid=5,
                hid=5,
                sku=5,
                delivery_buckets=[1234],
                blue_offers=[
                    BlueOffer(
                        price=2100,
                        feedid=feedid,
                        waremd5='OFF1_2100_SKU5_SUP{}_Q'.format(feedid),
                        randx=700 * feedid,
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
                        randx=800 * feedid,
                        blue_weight=11,
                        dimensions=OfferDimensions(length=5, width=1, height=3),
                    )
                    for feedid in [11, 12, 31, 32]
                ],
            ),
            MarketSku(
                hyperid=5,
                hid=5,
                sku=6,
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
                        waremd5='EXPR_2000_SKU5_SUP43_Q',
                        randx=421,
                        blue_weight=11,
                        dimensions=OfferDimensions(length=5, width=1, height=3),
                        is_express=True,
                    ),
                    BlueOffer(
                        price=2200,
                        feedid=44,
                        waremd5='EXPR_2000_SKU5_SUP44_Q',
                        randx=421,
                        blue_weight=11,
                        dimensions=OfferDimensions(length=5, width=1, height=3),
                        is_express=False,
                    ),
                ],
            ),
            MarketSku(
                hyperid=7,
                hid=7,
                sku=7,
                delivery_buckets=[1234],
                blue_offers=[
                    BlueOffer(
                        price=2201,
                        feedid=43,
                        waremd5='EXP1_2201_SKU7_SUP43_Q',
                        randx=421,
                        blue_weight=11,
                        dimensions=OfferDimensions(length=5, width=1, height=3),
                        is_express=True,
                    ),
                    BlueOffer(
                        price=2200,
                        feedid=44,
                        waremd5='OFF2_2200_SKU7_SUP44_Q',
                        randx=421,
                        blue_weight=11,
                        dimensions=OfferDimensions(length=5, width=1, height=3),
                        is_express=False,
                    ),
                ],
            ),
            MarketSku(
                hyperid=8,
                hid=CATEGORY_FOOD,
                sku=PEPSI_SKU,
                delivery_buckets=[1234],
                blue_offers=[
                    BlueOffer(
                        price=2201,
                        feedid=43,
                        waremd5='EXP1_2201_SKU8_SUP43_Q',
                        randx=421,
                        blue_weight=11,
                        dimensions=OfferDimensions(length=5, width=1, height=3),
                        is_express=True,
                    ),
                    BlueOffer(
                        price=2200,
                        feedid=44,
                        waremd5='OFF2_2200_SKU8_SUP44_Q',
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
                title="market DSBS Offer",
                hid=5,
                hyperid=5,
                price=500,
                fesh=41,
                business_id=41,
                sku=5,
                cpa=Offer.CPA_REAL,
                waremd5='DSBS_5_500_WITH_DELIVw',
                delivery_buckets=[1234],
            ),
            Offer(
                title="market DSBS Offer",
                hid=5,
                hyperid=5,
                price=510,
                fesh=42,
                business_id=42,
                sku=5,
                cpa=Offer.CPA_REAL,
                waremd5='DSBS_5_510_WITH_DELIVw',
                delivery_buckets=[1235],
            ),
            Offer(
                title="EdaRetail_sku8",
                hid=CATEGORY_FOOD,
                sku=PEPSI_SKU,
                hyperid=8,
                price=2000,
                fesh=45,
                business_id=45,
                cpa=Offer.CPA_REAL,
                waremd5='Sku8Price2000-155wh-eg',
                is_eda_retail=True,
                is_express=True,
            ),
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

    def test_prime(self):
        """Проверяем что выбираются габариты оффера и курьерская доставка из нужного бакета и региона"""
        response = self.report.request_json(
            'place=prime&hid=1&allow-collapsing=0&rids=213&yandexuid=1&debug=da'
            '&rearr-factors=market_buybox_by_supplier_on_white=1;market_debug_buybox=1;market_blue_buybox_with_delivery_context=1'
            '&rearr-factors=market_metadoc_search=no'
        )
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'wareId': 'OFF2_2100_SKU2_SUP31_Q',
                'debug': {
                    'buyboxDebug': {
                        'WonMethod': 'SINGLE_OFFER_BEFORE_BUYBOX_FILTERS',
                        'Offers': [
                            {
                                'WareMd5': 'OFF2_2100_SKU2_SUP31_Q',
                                'DeliveryContext': {
                                    'CourierDaysMax': 2,
                                    'CourierDaysMin': 1,
                                    # 'CourierPrice': 0,
                                    'CourierShopPrice': 15,
                                },
                            },
                        ],
                    }
                },
            },
        )

    def test_no_delivery(self):
        """Проверяем что с флагом market_blue_buybox_with_delivery_context=0 доставка не загружается"""
        response = self.report.request_json(
            'place=prime&hid=1&allow-collapsing=0&rids=213&yandexuid=1'
            '&rearr-factors=market_buybox_by_supplier_on_white=1;market_blue_buybox_by_gmv_ue=0;market_debug_buybox=1;market_blue_buybox_with_delivery_context=0'
            '&rearr-factors=market_metadoc_search=no'
        )
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'wareId': 'OFF2_2100_SKU2_SUP31_Q',
            },
        )
        self.assertFragmentNotIn(response, 'DeliveryContext')

    def test_delivery_not_loading_when_price_is_high(self):
        """Проверяем, что информация по доставке не загружается для офферов, цена которых сильно выше минимальной цены по SKU"""
        response = self.report.request_json(
            'place=productoffers&market-sku=4&rids=223&offers-set=defaultList&pp=6&rearr-factors=market_buybox_by_supplier_on_white=1;market_blue_buybox_by_gmv_ue=0;market_blue_buybox_with_delivery_context=1;market_debug_buybox=1'  # noqa
        )
        self.assertFragmentIn(
            response,
            {
                "wareId": "OFF1_1000_SKU4_SUP11_Q",
            },
        )
        self.assertFragmentNotIn(response, 'DeliveryContext')

    def test_delivery_in_dsbs(self):
        """Проверяем, что информация по доставке загружается и для dsbs и для md (пока без кеширования: market_blue_buybox_delivery_context_approx=0)"""
        response = self.report.request_json(
            'place=productoffers&market-sku=5&rids=213&offers-set=defaultList&pp=6&debug=da&rearr-factors=market_blue_buybox_disable_old_buybox_algo=0;market_blue_buybox_delivery_switch_type=1;prefer_do_with_sku=0;market_blue_buybox_dsbs_conversion_coef=1;market_blue_buybox_delivery_context_approx=1;market_blue_buybox_delivery_context_approx_use_shop_id=1;market_blue_buybox_with_delivery_context=1;market_blue_buybox_disable_dsbs_pessimisation=1;market_blue_buybox_always_compute_delivery_and_ue=1'  # noqa
        )
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'debug': {
                    'buyboxDebug': {
                        'Offers': [
                            {
                                'WareMd5': 'DSBS_5_500_WITH_DELIVw',
                                'DeliveryContext': {
                                    'CourierDaysMax': 2,
                                    'CourierDaysMin': 1,
                                    'CourierPrice': 15,
                                    'CourierShopPrice': 15,
                                },
                            },
                            {
                                'WareMd5': 'DSBS_5_510_WITH_DELIVw',
                                'DeliveryContext': {
                                    'CourierDaysMax': 5,
                                    'CourierDaysMin': 3,
                                    'CourierPrice': 45,
                                    'CourierShopPrice': 45,
                                },
                            },
                        ],
                    }
                },
            },
        )

    def test_delivery_in_express(self):
        """Проверяем, что информация по доставке загружается и для express"""
        response = self.report.request_json(
            'place=productoffers&market-sku=6&rids=213&offers-set=defaultList&pp=6&debug=da&rearr-factors=market_blue_buybox_delivery_context_approx=1;market_blue_buybox_delivery_context_approx_use_shop_id=1;market_blue_buybox_with_delivery_context=1;market_blue_buybox_disable_dsbs_pessimisation=1;market_blue_buybox_always_compute_delivery_and_ue=1'  # noqa
        )
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'debug': {
                    'buyboxDebug': {
                        'Offers': [
                            {
                                'WareMd5': 'EXPR_2000_SKU5_SUP43_Q',
                                'DeliveryContext': {
                                    'CourierDaysMax': 0,
                                    'CourierDaysMin': 0,
                                },
                            },
                        ],
                    }
                },
            },
        )

    def test_delivery_in_express_in_old_buybox(self):
        """Проверяем, что express доставка учитывается и в случае старого байбокса - при небольшом проигрыше в цене все равно выберется оффер с экспресс доставкой"""
        response = self.report.request_json(
            'place=productoffers&pp=6&market-sku=7&rids=213&offers-set=defaultList&debug=da&rearr-factors=market_blue_buybox_express_delivery_conv_mult=1.0;market_blue_buybox_delivery_context_approx=1;market_blue_buybox_delivery_context_approx_use_shop_id=1;market_blue_buybox_with_delivery_context=1;market_blue_buybox_disable_dsbs_pessimisation=1;market_blue_buybox_always_compute_delivery_and_ue=1'  # noqa
        )
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'debug': {
                    'buyboxDebug': {
                        'Offers': [
                            {
                                'WareMd5': 'EXP1_2201_SKU7_SUP43_Q',
                                'DeliveryContext': {
                                    'CourierDaysMax': 0,
                                    'CourierDaysMin': 0,
                                },
                            },
                        ],
                        'WonMethod': 'WON_BY_EXCHANGE',
                    }
                },
            },
        )

    def test_delivery_zero_days_and_express(self):
        """ "
        Проверяю, что в байбоксе учитывается экспресс. И доставка в тот же день.
        """
        market_blue_buybox_express_delivery_conv_mult = 1.2
        market_blue_buybox_conversion_msk_0_day_coef = 1.5

        rearr_flags_dict = {
            "market_blue_buybox_delivery_context_approx_use_shop_id": 1,
            "market_blue_buybox_delivery_switch_type": 3,
            "market_blue_buybox_disable_dsbs_pessimisation": 1,
            "market_blue_buybox_1p_cancellation_rating_default": 0.01,
            "market_blue_buybox_with_dsbs_white_do": 1,
            "prefer_do_with_sku": 1,
            "market_blue_buybox_express_delivery_conv_mult": market_blue_buybox_express_delivery_conv_mult,
            "market_blue_buybox_conversion_msk_0_day_coef": market_blue_buybox_conversion_msk_0_day_coef,
            "market_blue_buybox_always_compute_delivery_and_ue": 1,
        }
        rearr_str = dict_to_rearr(rearr_flags_dict)
        response = self.report.request_json(
            'place=productoffers&pp=6&hyperid=5&market-sku=6&rids=213&debug=da&rearr-factors=%s' % rearr_str
        )

        conversion_by_delivery = Capture()
        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'debug': {
                    'buyboxDebug': {
                        'Offers': [
                            {
                                'WareMd5': 'EXPR_2000_SKU5_SUP43_Q',
                                'DeliveryContext': {
                                    'CourierDaysMax': 0,
                                    'CourierDaysMin': 0,
                                },
                                'ConversionByDeliveryDayCoef': NotEmpty(capture=conversion_by_delivery),
                            },
                        ],
                    }
                },
            },
        )

        self.assertAlmostEqual(
            market_blue_buybox_express_delivery_conv_mult * market_blue_buybox_conversion_msk_0_day_coef,
            conversion_by_delivery.value,
            delta=0.003,
        )

    def test_delivery_zero_days_and_express_in_old_buybox(self):
        """ "
        Проверяем, что express доставка учитывается и в случае старого байбокса - при небольшом проигрыше в цене все равно выберется оффер с экспресс доставкой
        """
        market_blue_buybox_express_delivery_conv_mult = 1.2
        market_blue_buybox_conversion_msk_0_day_coef = 1.5

        rearr_flags_dict = {
            "market_blue_buybox_delivery_context_approx_use_shop_id": 1,
            "market_blue_buybox_delivery_switch_type": 3,
            "market_blue_buybox_disable_dsbs_pessimisation": 1,
            "market_blue_buybox_1p_cancellation_rating_default": 0.01,
            "market_blue_buybox_with_dsbs_white_do": 1,
            "prefer_do_with_sku": 1,
            "market_blue_buybox_express_delivery_conv_mult": market_blue_buybox_express_delivery_conv_mult,
            "market_blue_buybox_conversion_msk_0_day_coef": market_blue_buybox_conversion_msk_0_day_coef,
            "market_blue_buybox_always_compute_delivery_and_ue": 1,
        }
        rearr_str = dict_to_rearr(rearr_flags_dict)
        response = self.report.request_json(
            'place=productoffers&pp=6&hyperid=7&market-sku=7&rids=213&debug=da&allow-collapsing=1&offers-set=defaultList&rearr-factors=%s'
            % rearr_str
        )

        self.assertFragmentIn(
            response,
            {
                'entity': 'offer',
                'debug': {
                    'buyboxDebug': {
                        'Offers': [
                            {
                                'WareMd5': 'EXP1_2201_SKU7_SUP43_Q',
                                'DeliveryContext': {
                                    'CourierDaysMax': 0,
                                    'CourierDaysMin': 0,
                                },
                            },
                        ],
                    }
                },
            },
        )

    def test_delivery_eda_retail(self):
        wh_encoder = EatsWarehousesEncoder()
        wh_encoder.add_warehouse(wh_id=146)

        response = self.report.request_json(
            'place=productoffers&pp=6&hyperid=8&market-sku=25&rids=213&debug=da&allow-collapsing=1&offers-set=defaultList&eats-warehouses-compressed=%s&enable-foodtech-offers=eda_retail,lavka'
            % wh_encoder.encode()
        )

        self.assertFragmentIn(
            response,
            {
                'benefit': {'type': 'express-cpa'},
                'debug': {
                    'buyboxDebug': {
                        'Offers': [
                            {
                                'WareMd5': 'Sku8Price2000-155wh-eg',
                                'DeliveryContext': {
                                    'CourierDaysMax': 0,
                                    'CourierDaysMin': 0,
                                },
                            },
                        ],
                        'RejectedOffers': [
                            {
                                'RejectReason': 'TOO_HIGH_PRICE',
                                'Offer': {
                                    'WareMd5': 'EXP1_2201_SKU8_SUP43_Q',
                                },
                            }
                        ],
                    },
                },
            },
        )


if __name__ == '__main__':
    main()
