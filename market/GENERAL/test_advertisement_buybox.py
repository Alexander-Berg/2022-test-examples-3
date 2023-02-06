#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.types import (
    BlueOffer,
    ClickType,
    DateSwitchTimeAndRegionInfo,
    DeliveryBucket,
    DeliveryOption,
    DynamicDeliveryServiceInfo,
    DynamicWarehouseAndDeliveryServiceInfo,
    DynamicWarehouseInfo,
    DynamicWarehouseToWarehouseInfo,
    DynamicWarehousesPriorityInRegion,
    Elasticity,
    MarketSku,
    Region,
    RegionalDelivery,
    Shop,
    Model,
    UngroupedModel,
    MnPlace,
    WarehouseWithPriority,
    WarehousesPriorityInRegion,
    ShopPaymentMethods,
    PaymentRegionalGroup,
    Payment,
    Offer,
    WarehouseToRegions,
)
from core.matcher import Greater, Contains


USE_DEPRECATED_DIRECT_SHIPPING_FLOW = "&rearr-factors=get_rid_of_direct_shipping=0"


def dict_to_rearr(rearr_flags):
    result = ""
    for key in rearr_flags.keys():
        result += str(key) + "=" + str(rearr_flags[key]) + ";"
    return result


base_rearr_flags_dict = {
    "market_blue_buybox_gvm_ue_rand_low_adv": 0.90,
    # previous value market_blue_buybox_gvm_ue_rand_low=0.99
    "market_blue_buybox_gvm_ue_rand_delta_adv": 0.10,
    # previous value market_blue_buybox_gvm_ue_rand_delta=0.01
    "market_buybox_fulfillment_in_conv_adv_coef": 1.4,
    # previous value market_buybox_fulfillment_in_conv_coef=1.0
    "market_buybox_dropship_in_conv_adv_coef": 1.3,
    # previous value market_buybox_dropship_in_conv_coef=1.0
    "market_blue_buybox_dsbs_conversion_adv_coef": 1.2,
    # previous value market_blue_buybox_dsbs_conversion_coef=0.9
    "market_blue_buybox_express_delivery_conv_mult_adv": 0.0,
    # previous value market_blue_buybox_express_delivery_conv_mult=1.1
    "market_blue_buybox_non_courier_delivery_conversion_coef_adv": 0.9,
    # previous value market_blue_buybox_non_courier_delivery_conversion_coef=1.0
    "market_blue_buybox_shop_in_user_region_conversion_adv_coef": 1.1,
    # previous value market_blue_buybox_shop_in_user_region_conversion_coef=1.2
    "market_buybox_cash_only_shop_coef_adv": 0.9,
    # previous value market_buybox_cash_only_shop_coef=1.0
    "market_white_search_auction_cpa_fee": 1,
    "market_report_mimicry_in_serp_pattern": 2,
    "market_buybox_auction_search_sponsored_places_app": 1,
    "market_buybox_auction_search_sponsored_places_web": 1,
    "market_white_search_auction_cpa_fee_transfer_fee_do_desktop": 1,
    "market_blue_buybox_express_delivery_conv_mult": 1.1,
}

CEHAC_CATEG_ID = 91063  # товар категории CEHAC(Consumer Electronics, Home Appliance and Computers)


class T(TestCase):
    @classmethod
    def prepare_advertisement_buybox(cls):

        cls.index.regiontree += [
            Region(rid=213, name='Москва', region_type=Region.FEDERATIVE_SUBJECT),
        ]

        cls.settings.lms_autogenerate = False
        cls.dynamic.lms += [
            DynamicWarehouseInfo(id=1, home_region=213, is_express=True),
            DynamicWarehouseInfo(id=2, home_region=213),
            DynamicWarehouseInfo(id=3, home_region=213),
            DynamicWarehouseToWarehouseInfo(warehouse_from=1, warehouse_to=1),
            DynamicWarehouseToWarehouseInfo(warehouse_from=2, warehouse_to=2),
            DynamicWarehouseToWarehouseInfo(warehouse_from=3, warehouse_to=3),
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=1,
                delivery_service_id=157,
                date_switch_time_infos=[
                    DateSwitchTimeAndRegionInfo(date_switch_hour=23, region_to=213),
                ],
            ),
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=2,
                delivery_service_id=157,
                operation_time=0,
                date_switch_time_infos=[
                    DateSwitchTimeAndRegionInfo(date_switch_hour=23, region_to=213),
                ],
            ),
            # non local warehouse
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=3,
                delivery_service_id=157,
                date_switch_time_infos=[
                    DateSwitchTimeAndRegionInfo(date_switch_hour=23, region_to=213),
                ],
            ),
            DynamicDeliveryServiceInfo(id=157, rating=2),
            DynamicWarehousesPriorityInRegion(region=213, warehouses=[1, 2, 3]),
        ]

        cls.index.warehouse_priorities += [
            WarehousesPriorityInRegion(
                regions=[213],
                warehouse_with_priority=[
                    WarehouseWithPriority(1, 0),
                    WarehouseWithPriority(2, 0),
                    WarehouseWithPriority(3, 0),
                    WarehouseWithPriority(4, 0),
                ],
            )
        ]

        cls.index.warehouse_to_regions += [
            WarehouseToRegions(region_id=213, warehouse_id=1),
            WarehouseToRegions(region_id=213, warehouse_id=2),
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=1111,
                fesh=41,
                regional_options=[
                    RegionalDelivery(
                        rid=213,
                        options=[DeliveryOption(price=200, day_from=2, day_to=3, shop_delivery_price=5)],
                    )
                ],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]

        cls.index.shops += [
            Shop(
                fesh=3100,
                datafeed_id=3100,
                priority_region=213,
                regions=[213],
                supplier_type=Shop.THIRD_PARTY,
                is_supplier=True,
                blue=Shop.BLUE_REAL,
                fulfillment_program=True,
                warehouse_id=1,
            ),
            # Cash only
            Shop(
                fesh=3101,
                datafeed_id=3101,
                priority_region=213,
                regions=[213],
                supplier_type=Shop.THIRD_PARTY,
                is_supplier=True,
                blue=Shop.BLUE_REAL,
                fulfillment_program=True,
                warehouse_id=2,
            ),
            # Non local shop
            Shop(
                fesh=3102,
                datafeed_id=3102,
                priority_region=213,
                regions=[213],
                supplier_type=Shop.THIRD_PARTY,
                is_supplier=True,
                blue=Shop.BLUE_REAL,
                fulfillment_program=True,
                warehouse_id=3,
            ),
            # Dsbs shop
            Shop(
                fesh=3200,
                datafeed_id=3200,
                priority_region=213,
                regions=[213],
                cis=Shop.CIS_REAL,
                cpa=Shop.CPA_REAL,
                warehouse_id=1,
            ),
        ] + [
            Shop(
                fesh=3300 + i,
                datafeed_id=3300 + i,
                priority_region=213,
                regions=[213],
                supplier_type=Shop.THIRD_PARTY,
                is_supplier=True,
                blue=Shop.BLUE_REAL,
                fulfillment_program=True,
                warehouse_id=1,
            )
            for i in range(1, 5)
        ]

        cls.index.shops_payment_methods += [
            ShopPaymentMethods(
                fesh=3100,
                payment_groups=[
                    PaymentRegionalGroup(
                        included_regions=[213],
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
                fesh=3101,
                payment_groups=[
                    PaymentRegionalGroup(
                        included_regions=[213],
                        payment_methods=[
                            Payment.PT_CASH_ON_DELIVERY,
                        ],
                    ),
                ],
            ),
            ShopPaymentMethods(
                fesh=3102,
                payment_groups=[
                    PaymentRegionalGroup(
                        included_regions=[213],
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
                fesh=3200,
                payment_groups=[
                    PaymentRegionalGroup(
                        included_regions=[213],
                        payment_methods=[
                            Payment.PT_PREPAYMENT_CARD,
                            Payment.PT_PREPAYMENT_OTHER,
                            Payment.PT_CARD_ON_DELIVERY,
                            Payment.PT_CASH_ON_DELIVERY,
                        ],
                    ),
                ],
            ),
        ]

        cls.index.models += [
            Model(
                hyperid=94301,
                hid=94301,
                title="Исходная модель 1",
                ungrouped_blue=[
                    UngroupedModel(
                        group_id=1,
                        title="Расхлопнутая модель 1.1",
                        key='94301_1',
                    ),
                    UngroupedModel(
                        group_id=2,
                        title="Расхлопнутая модель 1.2",
                        key='94301_2',
                    ),
                ],
            ),
        ]

        cls.index.mskus += [
            MarketSku(
                hyperid=94301,
                hid=94301,
                sku=1,
                buybox_elasticity=[
                    Elasticity(price_variant=2100, demand_mean=200),
                    Elasticity(price_variant=2200, demand_mean=160),
                    Elasticity(price_variant=2500, demand_mean=30),
                ],
                blue_offers=[
                    # express offer
                    BlueOffer(
                        ts=7500,
                        price=2200,
                        fee=100,
                        feedid=3100,
                        delivery_buckets=[1111],
                        waremd5='OFF1_2200_SKU1_SUP01_Q',
                        title="SKU first buybox offer 1",
                        is_express=True,
                    ),
                    BlueOffer(
                        ts=750,
                        price=2100,
                        fee=1000,
                        feedid=3102,
                        delivery_buckets=[1111],
                        waremd5='OFF2_2100_SKU1_SUP01_Q',
                        title="SKU first buybox offer 2",
                    ),
                ],
                ungrouped_model_blue=1,
            ),
            MarketSku(
                hyperid=94302,
                hid=94302,
                sku=2,
                buybox_elasticity=[
                    Elasticity(price_variant=2100, demand_mean=200),
                    Elasticity(price_variant=2200, demand_mean=160),
                    Elasticity(price_variant=2500, demand_mean=30),
                ],
                blue_offers=[
                    BlueOffer(
                        ts=7509,
                        price=2000,
                        fee=0,
                        feedid=3100,
                        delivery_buckets=[1111],
                        waremd5='OFF1_2000_SKU2_SUP01_Q',
                        is_express=True,
                        title="SKU second buybox offer 1",
                    ),
                    BlueOffer(
                        ts=7511,
                        price=2050,
                        fee=0,
                        feedid=3100,
                        delivery_buckets=[1111],
                        waremd5='OFF2_2050_SKU2_SUP01_Q',
                        title="SKU second buybox offer 2",
                    ),
                    BlueOffer(
                        ts=7512,
                        price=2150,
                        fee=1000,
                        feedid=3100,
                        delivery_buckets=[1111],
                        waremd5='OFF3_2150_SKU2_SUP01_Q',
                        title="SKU second buybox offer 3",
                    ),
                ],
                ungrouped_model_blue=2,
            ),
            MarketSku(
                hyperid=94303,
                hid=94303,
                sku=3,
                buybox_elasticity=[
                    Elasticity(price_variant=2100, demand_mean=200),
                    Elasticity(price_variant=2200, demand_mean=160),
                    Elasticity(price_variant=2500, demand_mean=30),
                ],
                blue_offers=[
                    BlueOffer(
                        ts=7520,
                        price=2000,
                        fee=100,
                        feedid=3100,
                        delivery_buckets=[1111],
                        waremd5='OFF1_2000_SKU3_SUP01_Q',
                        is_express=True,
                        title="SKU third SKU buybox offer 1",
                    ),
                    BlueOffer(
                        ts=7521,
                        price=2050,
                        fee=500,
                        feedid=3102,
                        delivery_buckets=[1111],
                        waremd5='OFF2_2050_SKU3_SUP01_Q',
                        title="SKU third buybox offer 2",
                    ),
                    BlueOffer(
                        ts=7522,
                        price=2160,
                        fee=100,
                        feedid=3102,
                        delivery_buckets=[1111],
                        waremd5='OFF3_2160_SKU3_SUP01_Q',
                        title="SKU third buybox offer 3",
                    ),
                    BlueOffer(
                        ts=7523,
                        price=2260,
                        fee=100,
                        feedid=3102,
                        delivery_buckets=[1111],
                        waremd5='OFF4_2260_SKU3_SUP01_Q',
                        title="SKU third buybox offer 4",
                    ),
                ],
                ungrouped_model_blue=2,
            ),
            MarketSku(
                hyperid=94304,
                hid=94304,
                sku=4,
                buybox_elasticity=[
                    Elasticity(price_variant=2100, demand_mean=200),
                    Elasticity(price_variant=2200, demand_mean=160),
                    Elasticity(price_variant=2500, demand_mean=30),
                ],
                blue_offers=[
                    BlueOffer(
                        ts=7531,
                        price=2000,
                        fee=0,
                        feedid=3100,
                        delivery_buckets=[1111],
                        waremd5='OFF1_2000_SKU4_SUP01_Q',
                        is_express=True,
                        title="SKU fourth buybox offer 1",
                    ),
                    BlueOffer(
                        ts=7532,
                        price=2050,
                        fee=0,
                        feedid=3100,
                        delivery_buckets=[1111],
                        waremd5='OFF2_2050_SKU4_SUP01_Q',
                        title="SKU fourth buybox offer 2",
                    ),
                    BlueOffer(
                        ts=7533,
                        price=2260,
                        fee=1000,
                        feedid=3100,
                        delivery_buckets=[1111],
                        waremd5='OFF3_2260_SKU4_SUP01_Q',
                        title="SKU fourth buybox offer 3",
                    ),
                ],
                ungrouped_model_blue=2,
            ),
            MarketSku(
                hyperid=94305,
                hid=94305,
                sku=5,
                buybox_elasticity=[
                    Elasticity(price_variant=2100, demand_mean=200),
                    Elasticity(price_variant=2200, demand_mean=160),
                    Elasticity(price_variant=2500, demand_mean=30),
                ],
                blue_offers=[
                    BlueOffer(
                        ts=7541,
                        price=2000,
                        fee=100,
                        feedid=3102,
                        delivery_buckets=[1111],
                        waremd5='OFF1_2000_SKU5_SUP01_Q',
                        title="SKU fifth SKU buybox offer 1",
                    ),
                    BlueOffer(
                        ts=7542,
                        price=2160,
                        fee=100,
                        feedid=3102,
                        delivery_buckets=[1111],
                        waremd5='OFF2_2160_SKU5_SUP01_Q',
                        title="SKU fifth buybox offer 2",
                    ),
                    BlueOffer(
                        ts=7543,
                        price=2250,
                        fee=100,
                        feedid=3102,
                        delivery_buckets=[1111],
                        waremd5='OFF3_2250_SKU5_SUP01_Q',
                        title="SKU fifth buybox offer 3",
                    ),
                ],
                ungrouped_model_blue=2,
            ),
            MarketSku(
                hyperid=94306,
                hid=94306,
                sku=6,
                buybox_elasticity=[
                    Elasticity(price_variant=2100, demand_mean=200),
                    Elasticity(price_variant=2200, demand_mean=160),
                    Elasticity(price_variant=2500, demand_mean=30),
                ],
                blue_offers=[
                    BlueOffer(
                        ts=7550,
                        price=2000,
                        fee=0,
                        feedid=3100,
                        delivery_buckets=[1111],
                        waremd5='OFF1_2000_SKU6_SUP01_Q',
                        is_express=True,
                        title="SKU sixth SKU buybox offer 1",
                    ),
                    BlueOffer(
                        ts=7551,
                        price=2260,
                        fee=500,
                        feedid=3102,
                        delivery_buckets=[1111],
                        waremd5='OFF2_2050_SKU6_SUP01_Q',
                        title="SKU sixth buybox offer 2",
                    ),
                    BlueOffer(
                        ts=7552,
                        price=2280,
                        fee=100,
                        feedid=3102,
                        delivery_buckets=[1111],
                        waremd5='OFF3_2160_SKU6_SUP01_Q',
                        title="SKU sixth buybox offer 3",
                    ),
                    BlueOffer(
                        ts=7553,
                        price=2290,
                        fee=100,
                        feedid=3102,
                        delivery_buckets=[1111],
                        waremd5='OFF4_2260_SKU6_SUP01_Q',
                        title="SKU sixth buybox offer 4",
                    ),
                ],
                ungrouped_model_blue=2,
            ),
            MarketSku(
                hyperid=94307,
                hid=94307,
                sku=7,
                buybox_elasticity=[
                    Elasticity(price_variant=1900, demand_mean=200),
                    Elasticity(price_variant=2000, demand_mean=160),
                    Elasticity(price_variant=2100, demand_mean=30),
                ],
                blue_offers=[
                    BlueOffer(
                        ts=8000,
                        price=2000,
                        fee=0,
                        feedid=3301,
                        delivery_buckets=[1111],
                        waremd5='OFF1_2000_SKU7_SUP01_Q',
                        is_express=True,
                        title="SKU seventh SKU buybox offer 1",
                    ),
                    BlueOffer(
                        ts=8001,
                        price=2050,
                        fee=0,
                        feedid=3302,
                        delivery_buckets=[1111],
                        waremd5='OFF2_2050_SKU7_SUP01_Q',
                        title="SKU seventh buybox offer 2",
                    ),
                    BlueOffer(
                        ts=8002,
                        price=2160,
                        fee=100,
                        feedid=3303,
                        delivery_buckets=[1111],
                        waremd5='OFF3_2160_SKU7_SUP01_Q',
                        title="SKU seventh buybox offer 3",
                    ),
                    BlueOffer(
                        ts=8003,
                        price=2260,
                        fee=100,
                        feedid=3304,
                        delivery_buckets=[1111],
                        waremd5='OFF4_2260_SKU7_SUP01_Q',
                        title="SKU seventh buybox offer 4",
                    ),
                ],
                ungrouped_model_blue=2,
            ),
            MarketSku(
                hyperid=94308,
                hid=94308,
                sku=8,
                buybox_elasticity=[
                    Elasticity(price_variant=1900, demand_mean=200),
                    Elasticity(price_variant=2000, demand_mean=160),
                    Elasticity(price_variant=2100, demand_mean=30),
                ],
                blue_offers=[
                    BlueOffer(
                        ts=9000,
                        price=2000,
                        fee=0,
                        feedid=3301,
                        delivery_buckets=[1111],
                        waremd5='OFF1_2000_SKU8_SUP01_Q',
                        is_express=True,
                        title="SKU seventh SKU buybox offer 1",
                    ),
                    BlueOffer(
                        ts=9001,
                        price=2050,
                        fee=0,
                        feedid=3302,
                        delivery_buckets=[1111],
                        waremd5='OFF2_2050_SKU8_SUP01_Q',
                        title="SKU seventh buybox offer 2",
                    ),
                    BlueOffer(
                        ts=9002,
                        price=2160,
                        fee=100,
                        feedid=3303,
                        delivery_buckets=[1111],
                        waremd5='OFF3_2160_SKU8_SUP01_Q',
                        title="SKU seventh buybox offer 3",
                    ),
                    BlueOffer(
                        ts=9003,
                        price=2260,
                        fee=100,
                        feedid=3304,
                        delivery_buckets=[1111],
                        waremd5='OFF4_2260_SKU8_SUP01_Q',
                        title="SKU seventh buybox offer 4",
                    ),
                ],
                ungrouped_model_blue=2,
            ),
        ]

        cls.index.offers += [
            Offer(
                title="SKU first buybox offer 3",
                hid=94301,
                hyperid=94301,
                price=2100,
                fesh=3200,
                business_id=3200,
                sku=1,
                fee=100,
                cpa=Offer.CPA_REAL,
                waremd5='sgf1xWYFqdGiLh4TT-111Q',
                feedid=7,
                delivery_buckets=[1111],
                offerid="proh.offer",
                ts=7600,
                ungrouped_model_blue=1,
                is_express=True,
            ),
        ]

        for place in [MnPlace.BASE_SEARCH, MnPlace.META_REARRANGE]:
            cls.matrixnet.on_place(place, 7500).respond(0.9)
            cls.matrixnet.on_place(place, 7501).respond(0.89)
            cls.matrixnet.on_place(place, 7502).respond(0.88)
            cls.matrixnet.on_place(place, 7510).respond(0.87)
            cls.matrixnet.on_place(place, 7511).respond(0.86)
            cls.matrixnet.on_place(place, 7512).respond(0.85)
            cls.matrixnet.on_place(place, 7600).respond(0.84)
            cls.matrixnet.on_place(place, 7601).respond(0.83)
            cls.matrixnet.on_place(place, 7520).respond(0.90)
            cls.matrixnet.on_place(place, 7521).respond(0.84)
            cls.matrixnet.on_place(place, 7522).respond(0.85)
            cls.matrixnet.on_place(place, 7523).respond(0.86)
            cls.matrixnet.on_place(place, 7531).respond(0.84)
            cls.matrixnet.on_place(place, 7532).respond(0.85)
            cls.matrixnet.on_place(place, 7533).respond(0.86)
            cls.matrixnet.on_place(place, 7541).respond(0.84)
            cls.matrixnet.on_place(place, 7542).respond(0.85)
            cls.matrixnet.on_place(place, 7543).respond(0.86)
            cls.matrixnet.on_place(place, 7550).respond(0.84)
            cls.matrixnet.on_place(place, 7551).respond(0.85)
            cls.matrixnet.on_place(place, 7552).respond(0.86)
            cls.matrixnet.on_place(place, 7553).respond(0.87)
            cls.matrixnet.on_place(place, 8000).respond(0.81)
            cls.matrixnet.on_place(place, 8001).respond(0.84)
            cls.matrixnet.on_place(place, 8002).respond(0.87)
            cls.matrixnet.on_place(place, 8003).respond(0.90)
            cls.matrixnet.on_place(place, 9000).respond(0.81)
            cls.matrixnet.on_place(place, 9001).respond(0.84)
            cls.matrixnet.on_place(place, 9002).respond(0.87)
            cls.matrixnet.on_place(place, 9003).respond(0.90)

    def test_delivery_type_and_express(self):

        # Проверяем что правильно выставляются DeliveryTypeConversionCoef & express coefficient
        rearr_flags_dict = base_rearr_flags_dict.copy()
        rearr_flags_dict["market_buybox_enable_advert_buybox"] = 1

        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        request = (
            'pp=7&hid=94301&place=prime&rgb=green_with_blue&rids=213'
            '&market-sku=1&show-urls=external,decrypted,direct%2Ccpa&bsformat=2&&viewtype=list&use-default-offers=1'
            '&allow-collapsing=1&allow-ungrouping=1&waitall=da&numdoc=48&debug=1'
            '&rearr-factors={}'.format(rearr_flags_str) + USE_DEPRECATED_DIRECT_SHIPPING_FLOW
        )

        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            "offers": {
                                "items": [
                                    {
                                        "wareId": "sgf1xWYFqdGiLh4TT-111Q",
                                        "debug": {
                                            "buyboxDebug": {
                                                "Offers": [
                                                    #  DSBS offer & express offer
                                                    {
                                                        "WareMd5": "sgf1xWYFqdGiLh4TT-111Q",
                                                        "Conversion": 228.589,
                                                        # PredictedElasticity: 200
                                                        # ConversionByDeliveryDayCoef: 1.353
                                                        # IsWarehouseInUserCartCoef: 0.95
                                                        # ShopRatingCoef: 0.988011
                                                        # ShopInUserRegionConversionCoef: 1
                                                        # PromoBoostCoefficient: 1
                                                        # DeliveryTypeConversionCoef: 0.9
                                                        # CashOnlyShopCoef: 1
                                                        # Conversion: 228.589
                                                    },
                                                    # FF offer & express offer
                                                    {
                                                        "WareMd5": "OFF1_2200_SKU1_SUP01_Q",
                                                        "Conversion": 203.19,
                                                        # PredictedElasticity: 160
                                                        # ConversionByDeliveryDayCoef: 1.353
                                                        # IsWarehouseInUserCartCoef: 0.95
                                                        # ShopRatingCoef: 0.988011
                                                        # ShopInUserRegionConversionCoef: 1
                                                        # PromoBoostCoefficient: 1
                                                        # DeliveryTypeConversionCoef: 1
                                                        # CashOnlyShopCoef: 1
                                                        # Conversion: 203.19
                                                    },
                                                    {
                                                        "WareMd5": "OFF2_2100_SKU1_SUP01_Q",
                                                        "Conversion": 182.09,
                                                        # PredictedElasticity: 200
                                                        # ConversionByDeliveryDayCoef: 0.97
                                                        # IsWarehouseInUserCartCoef: 0.95
                                                        # ShopRatingCoef: 0.988011
                                                        # ShopInUserRegionConversionCoef: 1
                                                        # PromoBoostCoefficient: 1
                                                        # DeliveryTypeConversionCoef: 1
                                                        # CashOnlyShopCoef: 1
                                                        # Conversion: 182.09
                                                    },
                                                ],
                                            },
                                        },
                                    },
                                ],
                            },
                        },
                        {
                            "offers": {
                                "items": [
                                    {
                                        "wareId": "OFF2_2100_SKU1_SUP01_Q",
                                        "sponsored": True,
                                        "debug": {
                                            "buyboxDebug": {
                                                "Offers": [
                                                    # DSBS offer & express offer
                                                    {
                                                        "WareMd5": "sgf1xWYFqdGiLh4TT-111Q",
                                                        "Conversion": 277.078,
                                                        # PredictedElasticity: 200
                                                        # ConversionByDeliveryDayCoef: 1.23
                                                        # - так как express offer не бустится
                                                        # IsWarehouseInUserCartCoef: 0.95
                                                        # ShopRatingCoef: 0.988011
                                                        # ShopInUserRegionConversionCoef: 1
                                                        # PromoBoostCoefficient: 1
                                                        # DeliveryTypeConversionCoef: 1.2
                                                        # - так как market_blue_buybox_dsbs_conversion_adv_coef=1.2
                                                        # CashOnlyShopCoef: 1
                                                        # ConversionAdv: 277.078
                                                    },
                                                    # express offer
                                                    {
                                                        "WareMd5": "OFF1_2200_SKU1_SUP01_Q",
                                                        "Conversion": 258.606,
                                                        # PredictedElasticity: 160
                                                        # ConversionByDeliveryDayCoef: 1.23
                                                        # - так как express offer не бустится
                                                        # IsWarehouseInUserCartCoef: 0.95
                                                        # ShopRatingCoef: 0.988011
                                                        # ShopInUserRegionConversionCoef: 1
                                                        # PromoBoostCoefficient: 1
                                                        # DeliveryTypeConversionCoef: 1.4
                                                        # - так как market_buybox_fulfillment_in_conv_adv_coef=1.4
                                                        # CashOnlyShopCoef: 1
                                                        # Conversion: 258.606
                                                    },
                                                    {
                                                        "WareMd5": "OFF2_2100_SKU1_SUP01_Q",
                                                        "Conversion": 254.927,
                                                        # PredictedElasticity: 200
                                                        # ConversionByDeliveryDayCoef: 0.97
                                                        # IsWarehouseInUserCartCoef: 0.95
                                                        # ShopRatingCoef: 0.988011
                                                        # ShopInUserRegionConversionCoef: 1
                                                        # PromoBoostCoefficient: 1
                                                        # DeliveryTypeConversionCoef: 1.4
                                                        # - так как market_buybox_fulfillment_in_conv_adv_coef=1.4
                                                        # CashOnlyShopCoef: 1
                                                        # Conversion: 254.927
                                                    },
                                                ],
                                            },
                                        },
                                    },
                                ],
                            },
                        },
                    ],
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        # Проверяем что при выключенном рекламном baybox-е Conversion одинаковая в обычном поиска и рекламном месте
        rearr_flags_dict["market_buybox_enable_advert_buybox"] = 0
        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        request = (
            'pp=7&hid=94301&place=prime&rgb=green_with_blue&rids=213&market-sku=1'
            '&show-urls=external,decrypted,direct%2Ccpa&bsformat=2&&viewtype=list&use-default-offers=1'
            '&allow-collapsing=1&allow-ungrouping=1&waitall=da&numdoc=48&debug=1'
            '&rearr-factors={}'.format(rearr_flags_str) + USE_DEPRECATED_DIRECT_SHIPPING_FLOW
        )

        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            "offers": {
                                "items": [
                                    {
                                        "wareId": "sgf1xWYFqdGiLh4TT-111Q",
                                        "debug": {
                                            "buyboxDebug": {
                                                "Offers": [
                                                    #  DSBS offer & express offer
                                                    {
                                                        "WareMd5": "sgf1xWYFqdGiLh4TT-111Q",
                                                        "Conversion": 228.589,
                                                        # PredictedElasticity: 200
                                                        # ConversionByDeliveryDayCoef: 1.353
                                                        # IsWarehouseInUserCartCoef: 0.95
                                                        # ShopRatingCoef: 0.988011
                                                        # ShopInUserRegionConversionCoef: 1
                                                        # PromoBoostCoefficient: 1
                                                        # DeliveryTypeConversionCoef: 0.9
                                                        # CashOnlyShopCoef: 1
                                                        # Conversion: 228.589
                                                    },
                                                    # FF offer & express offer
                                                    {
                                                        "WareMd5": "OFF1_2200_SKU1_SUP01_Q",
                                                        "Conversion": 203.19,
                                                        # PredictedElasticity: 160
                                                        # ConversionByDeliveryDayCoef: 1.353
                                                        # IsWarehouseInUserCartCoef: 0.95
                                                        # ShopRatingCoef: 0.988011
                                                        # ShopInUserRegionConversionCoef: 1
                                                        # PromoBoostCoefficient: 1
                                                        # DeliveryTypeConversionCoef: 1
                                                        # CashOnlyShopCoef: 1
                                                        # Conversion: 203.19
                                                    },
                                                    {
                                                        "WareMd5": "OFF2_2100_SKU1_SUP01_Q",
                                                        "Conversion": 182.09,
                                                        # PredictedElasticity: 200
                                                        # ConversionByDeliveryDayCoef: 0.97
                                                        # IsWarehouseInUserCartCoef: 0.95
                                                        # ShopRatingCoef: 0.988011
                                                        # ShopInUserRegionConversionCoef: 1
                                                        # PromoBoostCoefficient: 1
                                                        # DeliveryTypeConversionCoef: 1
                                                        # CashOnlyShopCoef: 1
                                                        # Conversion: 182.09
                                                    },
                                                ],
                                            },
                                        },
                                    },
                                ],
                            },
                        },
                        {
                            "offers": {
                                "items": [
                                    {
                                        "wareId": "OFF2_2100_SKU1_SUP01_Q",
                                        "sponsored": True,
                                        "debug": {
                                            "buyboxDebug": {
                                                "Offers": [
                                                    #  DSBS offer & express offer
                                                    {
                                                        "WareMd5": "sgf1xWYFqdGiLh4TT-111Q",
                                                        "Conversion": 228.589,
                                                        # PredictedElasticity: 200
                                                        # ConversionByDeliveryDayCoef: 1.353
                                                        # IsWarehouseInUserCartCoef: 0.95
                                                        # ShopRatingCoef: 0.988011
                                                        # ShopInUserRegionConversionCoef: 1
                                                        # PromoBoostCoefficient: 1
                                                        # DeliveryTypeConversionCoef: 0.9
                                                        # CashOnlyShopCoef: 1
                                                        # Conversion: 228.589
                                                    },
                                                    # FF offer & express offer
                                                    {
                                                        "WareMd5": "OFF1_2200_SKU1_SUP01_Q",
                                                        "Conversion": 203.19,
                                                        # PredictedElasticity: 160
                                                        # ConversionByDeliveryDayCoef: 1.353
                                                        # IsWarehouseInUserCartCoef: 0.95
                                                        # ShopRatingCoef: 0.988011
                                                        # ShopInUserRegionConversionCoef: 1
                                                        # PromoBoostCoefficient: 1
                                                        # DeliveryTypeConversionCoef: 1
                                                        # CashOnlyShopCoef: 1
                                                        # Conversion: 203.19
                                                    },
                                                    {
                                                        "WareMd5": "OFF2_2100_SKU1_SUP01_Q",
                                                        "Conversion": 182.09,
                                                        # PredictedElasticity: 200
                                                        # ConversionByDeliveryDayCoef: 0.97
                                                        # IsWarehouseInUserCartCoef: 0.95
                                                        # ShopRatingCoef: 0.988011
                                                        # ShopInUserRegionConversionCoef: 1
                                                        # PromoBoostCoefficient: 1
                                                        # DeliveryTypeConversionCoef: 1
                                                        # CashOnlyShopCoef: 1
                                                        # Conversion: 182.09
                                                    },
                                                ],
                                            },
                                        },
                                    },
                                ],
                            },
                        },
                    ],
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_nobit_threshold(self):
        # Проверяем что правильно работает no bit порог
        rearr_flags_dict = base_rearr_flags_dict.copy()
        rearr_flags_dict["market_buybox_enable_advert_buybox"] = 1

        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        request = (
            'pp=7&hid=94302&place=prime&rgb=green_with_blue&rids=213&market-sku=2'
            '&show-urls=external,decrypted,direct%2Ccpa&bsformat=2&&viewtype=list&use-default-offers=1'
            '&allow-collapsing=1&allow-ungrouping=1&waitall=da&numdoc=48&debug=1'
            '&rearr-factors={}'.format(rearr_flags_str)
        )

        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            "offers": {
                                "items": [
                                    {
                                        "wareId": "OFF2_2050_SKU2_SUP01_Q",
                                        "debug": {
                                            "buyboxDebug": {
                                                "Offers": [
                                                    {
                                                        "WareMd5": "OFF1_2000_SKU2_SUP01_Q",
                                                        "ShopFee": 0,
                                                    },
                                                    {
                                                        "WareMd5": "OFF2_2050_SKU2_SUP01_Q",
                                                        "ShopFee": 0,
                                                    },
                                                    {
                                                        "WareMd5": "OFF3_2150_SKU2_SUP01_Q",
                                                        "ShopFee": 1000,
                                                    },
                                                ],
                                            },
                                        },
                                    },
                                ],
                            },
                        },
                        {
                            "offers": {
                                "items": [
                                    {
                                        "wareId": "OFF3_2150_SKU2_SUP01_Q",
                                        "sponsored": True,
                                        "debug": {
                                            "buyboxDebug": {
                                                "Offers": [
                                                    {
                                                        "WareMd5": "OFF3_2150_SKU2_SUP01_Q",
                                                        "ShopFee": 1000,
                                                    },
                                                ],
                                                "RejectedOffers": [
                                                    {
                                                        "RejectReason": "NOBID_PRIORITY",
                                                        "Offer": {
                                                            "WareMd5": "OFF1_2000_SKU2_SUP01_Q",
                                                            "ShopFee": 0,
                                                        },
                                                    },
                                                    {
                                                        "RejectReason": "NOBID_PRIORITY",
                                                        "Offer": {
                                                            "WareMd5": "OFF2_2050_SKU2_SUP01_Q",
                                                            "ShopFee": 0,
                                                        },
                                                    },
                                                ],
                                            },
                                        },
                                    },
                                ],
                            },
                        },
                    ],
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

        rearr_flags_dict["market_buybox_enable_advert_buybox"] = 0
        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        request = (
            'pp=7&hid=94302&place=prime&rgb=green_with_blue&rids=213&market-sku=2'
            '&show-urls=external,decrypted,direct%2Ccpa&bsformat=2&&viewtype=list&use-default-offers=1'
            '&allow-collapsing=1&allow-ungrouping=1&waitall=da&numdoc=48&debug=1'
            '&rearr-factors={}'.format(rearr_flags_str)
        )

        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            "offers": {
                                "items": [
                                    {
                                        "wareId": "OFF2_2050_SKU2_SUP01_Q",
                                        "debug": {
                                            "buyboxDebug": {
                                                "Offers": [
                                                    {
                                                        "WareMd5": "OFF1_2000_SKU2_SUP01_Q",
                                                        "ShopFee": 0,
                                                    },
                                                    {
                                                        "WareMd5": "OFF2_2050_SKU2_SUP01_Q",
                                                        "ShopFee": 0,
                                                    },
                                                    {
                                                        "WareMd5": "OFF3_2150_SKU2_SUP01_Q",
                                                        "ShopFee": 1000,
                                                    },
                                                ],
                                            },
                                        },
                                    },
                                ],
                            },
                        },
                        {
                            "offers": {
                                "items": [
                                    {
                                        "wareId": "OFF3_2150_SKU2_SUP01_Q",
                                        "sponsored": True,
                                        "debug": {
                                            "buyboxDebug": {
                                                "Offers": [
                                                    {
                                                        "WareMd5": "OFF1_2000_SKU2_SUP01_Q",
                                                        "ShopFee": 0,
                                                    },
                                                    {
                                                        "WareMd5": "OFF2_2050_SKU2_SUP01_Q",
                                                        "ShopFee": 0,
                                                    },
                                                    {
                                                        "WareMd5": "OFF3_2150_SKU2_SUP01_Q",
                                                        "ShopFee": 1000,
                                                    },
                                                ],
                                            },
                                        },
                                    },
                                ],
                            },
                        },
                    ],
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_adv_price_threshold(self):
        # Проверяем что правильно работает порог по цене
        rearr_flags_dict = base_rearr_flags_dict.copy()
        rearr_flags_dict["market_buybox_enable_advert_buybox"] = 1
        rearr_flags_dict["market_buybox_adv_buybox_price_rel_max_threshold"] = 1.1

        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        request = (
            'pp=7&hid=94303&place=prime&rgb=green_with_blue&rids=213&market-sku=3'
            '&show-urls=external,decrypted,direct%2Ccpa&bsformat=2&&viewtype=list&use-default-offers=1'
            '&allow-collapsing=1&allow-ungrouping=1&waitall=da&numdoc=48&debug=1'
            '&rearr-factors={}'.format(rearr_flags_str)
        )

        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            "offers": {
                                "items": [
                                    {
                                        "wareId": "OFF1_2000_SKU3_SUP01_Q",
                                        "debug": {
                                            "buyboxDebug": {
                                                "Offers": [
                                                    {
                                                        "WareMd5": "OFF1_2000_SKU3_SUP01_Q",
                                                        "PriceAfterCashback": 2000,
                                                    },
                                                    {
                                                        "WareMd5": "OFF2_2050_SKU3_SUP01_Q",
                                                        "PriceAfterCashback": 2050,
                                                    },
                                                ],
                                                # не проходит по порогу 5% + 50
                                                "RejectedOffers": [
                                                    {
                                                        "RejectReason": "TOO_HIGH_PRICE",
                                                        "Offer": {
                                                            "WareMd5": "OFF4_2260_SKU3_SUP01_Q",
                                                            "PriceAfterCashback": 2260,
                                                        },
                                                    },
                                                    {
                                                        "RejectReason": "TOO_HIGH_PRICE",
                                                        "Offer": {
                                                            "WareMd5": "OFF3_2160_SKU3_SUP01_Q",
                                                            "PriceAfterCashback": 2160,
                                                        },
                                                    },
                                                ],
                                            },
                                        },
                                    },
                                ],
                            },
                        },
                        {
                            "offers": {
                                "items": [
                                    {
                                        "wareId": "OFF2_2050_SKU3_SUP01_Q",
                                        "sponsored": True,
                                        "debug": {
                                            "buyboxDebug": {
                                                "Offers": [
                                                    {
                                                        "WareMd5": "OFF1_2000_SKU3_SUP01_Q",
                                                        "PriceAfterCashback": 2000,
                                                    },
                                                    {
                                                        "WareMd5": "OFF2_2050_SKU3_SUP01_Q",
                                                        "PriceAfterCashback": 2050,
                                                    },
                                                    {
                                                        "WareMd5": "OFF3_2160_SKU3_SUP01_Q",
                                                        "PriceAfterCashback": 2160,
                                                    },
                                                ],
                                                "RejectedOffers": [
                                                    {
                                                        # не проходит по рекламному порогу 10% + 50
                                                        "RejectReason": "TOO_HIGH_PRICE",
                                                        "Offer": {
                                                            "WareMd5": "OFF4_2260_SKU3_SUP01_Q",
                                                            "PriceAfterCashback": 2260,
                                                        },
                                                    },
                                                ],
                                            },
                                        },
                                    },
                                ],
                            },
                        },
                    ],
                },
            },
        )

        rearr_flags_dict["market_blue_buybox_max_price_rel_add_diff_adv"] = 70
        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        request = (
            'pp=7&hid=94303&place=prime&rgb=green_with_blue&rids=213&market-sku=3'
            '&show-urls=external,decrypted,direct%2Ccpa&bsformat=2&&viewtype=list&use-default-offers=1'
            '&allow-collapsing=1&allow-ungrouping=1&waitall=da&numdoc=48&debug=1'
            '&rearr-factors={}'.format(rearr_flags_str)
        )

        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            "offers": {
                                "items": [
                                    {
                                        "wareId": "OFF1_2000_SKU3_SUP01_Q",
                                        "debug": {
                                            "buyboxDebug": {
                                                "Offers": [
                                                    {
                                                        "WareMd5": "OFF1_2000_SKU3_SUP01_Q",
                                                        "PriceAfterCashback": 2000,
                                                    },
                                                    {
                                                        "WareMd5": "OFF2_2050_SKU3_SUP01_Q",
                                                        "PriceAfterCashback": 2050,
                                                    },
                                                ],
                                                # не проходит по порогу 5% + 50
                                                "RejectedOffers": [
                                                    {
                                                        "RejectReason": "TOO_HIGH_PRICE",
                                                        "Offer": {
                                                            "WareMd5": "OFF3_2160_SKU3_SUP01_Q",
                                                            "PriceAfterCashback": 2160,
                                                        },
                                                    },
                                                    {
                                                        "RejectReason": "TOO_HIGH_PRICE",
                                                        "Offer": {
                                                            "WareMd5": "OFF4_2260_SKU3_SUP01_Q",
                                                            "PriceAfterCashback": 2260,
                                                        },
                                                    },
                                                ],
                                            },
                                        },
                                    },
                                ],
                            },
                        },
                        {
                            "offers": {
                                "items": [
                                    {
                                        "wareId": "OFF2_2050_SKU3_SUP01_Q",
                                        "sponsored": True,
                                        "debug": {
                                            "buyboxDebug": {
                                                "Offers": [
                                                    {
                                                        "WareMd5": "OFF1_2000_SKU3_SUP01_Q",
                                                        "PriceAfterCashback": 2000,
                                                    },
                                                    {
                                                        "WareMd5": "OFF2_2050_SKU3_SUP01_Q",
                                                        "PriceAfterCashback": 2050,
                                                    },
                                                    {
                                                        "WareMd5": "OFF3_2160_SKU3_SUP01_Q",
                                                        "PriceAfterCashback": 2160,
                                                    },
                                                    # стал проходить порог так как он стал 10% + 70
                                                    {
                                                        "WareMd5": "OFF4_2260_SKU3_SUP01_Q",
                                                        "PriceAfterCashback": 2260,
                                                    },
                                                ],
                                                "RejectedOffers": [],
                                            },
                                        },
                                    },
                                ],
                            },
                        },
                    ],
                },
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_delivery_type_and_express_cpa_ship_incat(self):
        # Проверяем что правильно выставляются DeliveryTypeConversionCoef & express coefficient в cpa_ship_incat
        rearr_flags_dict = base_rearr_flags_dict.copy()
        rearr_flags_dict["market_buybox_enable_advert_buybox"] = 1
        rearr_flags_dict["market_buybox_auction_cpa_shop_incut"] = 1
        rearr_flags_dict["market_buybox_auction_coef_b_multiplicative_bid_coef_cs_incut"] = 0.001

        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        request = 'place=cpa_shop_incut&pp=18&text=first&numdoc=10&min-num-doc=1&debug=1' '&rearr-factors={}'.format(
            rearr_flags_str
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "wareId": "OFF2_2100_SKU1_SUP01_Q",
                        "debug": {
                            "buyboxDebug": {
                                "Offers": [
                                    {
                                        "WareMd5": "sgf1xWYFqdGiLh4TT-111Q",
                                        "ConversionByDeliveryDayCoef": 1.36,
                                        "Conversion": 306.362,
                                    },
                                    {
                                        "WareMd5": "OFF1_2200_SKU1_SUP01_Q",
                                        "ConversionByDeliveryDayCoef": 1.36,
                                        "Conversion": 285.938,
                                    },
                                    {
                                        "WareMd5": "OFF2_2100_SKU1_SUP01_Q",
                                        "ConversionByDeliveryDayCoef": 0.9,
                                        "Conversion": 236.53,
                                    },
                                ],
                            },
                        },
                    },
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )

        rearr_flags_dict = base_rearr_flags_dict.copy()
        rearr_flags_dict["market_buybox_enable_advert_buybox"] = 0
        rearr_flags_dict["market_buybox_auction_cpa_shop_incut"] = 1
        rearr_flags_dict["market_buybox_auction_coef_b_multiplicative_bid_coef_cs_incut"] = 0.001

        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        request = 'place=cpa_shop_incut&pp=18&text=first&numdoc=10&min-num-doc=1&debug=1&rearr-factors={}'.format(
            rearr_flags_str
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "wareId": "OFF2_2100_SKU1_SUP01_Q",
                        "debug": {
                            "buyboxDebug": {
                                "Offers": [
                                    {
                                        "WareMd5": "sgf1xWYFqdGiLh4TT-111Q",
                                        "ConversionByDeliveryDayCoef": 1.496,
                                        "Conversion": 252.749,
                                    },
                                    {
                                        "WareMd5": "OFF1_2200_SKU1_SUP01_Q",
                                        "ConversionByDeliveryDayCoef": 1.496,
                                        "Conversion": 224.666,
                                    },
                                    {
                                        "WareMd5": "OFF2_2100_SKU1_SUP01_Q",
                                        "ConversionByDeliveryDayCoef": 1,
                                        "Conversion": 187.722,
                                    },
                                ],
                            },
                        },
                    },
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_nobit_threshold_cpa_ship_incat(self):
        # Проверяем что правильно работает no bit порог в cpa_ship_incat
        rearr_flags_dict = base_rearr_flags_dict.copy()
        rearr_flags_dict["market_buybox_enable_advert_buybox"] = 1
        rearr_flags_dict["market_buybox_auction_cpa_shop_incut"] = 1
        rearr_flags_dict["market_buybox_auction_coef_b_multiplicative_bid_coef_cs_incut"] = 0.001

        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        request = 'place=cpa_shop_incut&pp=18&text=second&numdoc=10&min-num-doc=1&debug=1&rearr-factors={}'.format(
            rearr_flags_str
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "wareId": "OFF3_2150_SKU2_SUP01_Q",
                        "debug": {
                            "buyboxDebug": {
                                "Offers": [
                                    {
                                        "WareMd5": "OFF3_2150_SKU2_SUP01_Q",
                                        "ShopFee": 1000,
                                    },
                                ],
                                "RejectedOffers": [
                                    {
                                        "RejectReason": "NOBID_PRIORITY",
                                        "Offer": {
                                            "WareMd5": "OFF1_2000_SKU2_SUP01_Q",
                                            "ShopFee": 0,
                                        },
                                    },
                                    {
                                        "RejectReason": "NOBID_PRIORITY",
                                        "Offer": {
                                            "WareMd5": "OFF2_2050_SKU2_SUP01_Q",
                                            "ShopFee": 0,
                                        },
                                    },
                                ],
                            },
                        },
                    },
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_nobit_threshold_priority_cpa_ship_incat(self):
        # Проверяем что фильтр по цене имеет более высокий приоритет чем фильтр по ставке
        # Оффер со ставкой но плохой ценой отсеится в байбоксе и выиграет оффер без ставки
        # В дальнейшем это оффер отфильтруются в SHOP_FEE_THRESHOLD
        rearr_flags_dict = base_rearr_flags_dict.copy()
        rearr_flags_dict["market_buybox_enable_advert_buybox"] = 1
        rearr_flags_dict["market_buybox_auction_cpa_shop_incut"] = 1
        rearr_flags_dict["market_buybox_auction_coef_b_multiplicative_bid_coef_cs_incut"] = 0.001

        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        request = 'place=cpa_shop_incut&pp=18&text=fourth&numdoc=10&min-num-doc=1&debug=1&rearr-factors={}'.format(
            rearr_flags_str
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {"results": []},
            preserve_order=True,
            allow_different_len=False,
        )

    def test_adv_price_threshold_cpa_ship_incat(self):
        # Проверяем что правильно работает порог по цене в cpa_ship_incat
        rearr_flags_dict = base_rearr_flags_dict.copy()
        rearr_flags_dict["market_buybox_enable_advert_buybox"] = 1
        rearr_flags_dict["market_buybox_auction_cpa_shop_incut"] = 1
        rearr_flags_dict["market_buybox_auction_coef_b_multiplicative_bid_coef_cs_incut"] = 0.001
        rearr_flags_dict["market_buybox_adv_buybox_price_rel_max_threshold"] = 1.05

        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        request = 'place=cpa_shop_incut&pp=18&text=fifth&numdoc=10&min-num-doc=1&debug=1&rearr-factors={}'.format(
            rearr_flags_str
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "wareId": "OFF1_2000_SKU5_SUP01_Q",
                        "debug": {
                            "buyboxDebug": {
                                "Offers": [
                                    {
                                        "WareMd5": "OFF1_2000_SKU5_SUP01_Q",
                                        "PriceAfterCashback": 2000,
                                    },
                                ],
                                "RejectedOffers": [
                                    {
                                        "RejectReason": "TOO_HIGH_PRICE",
                                        "Offer": {
                                            "WareMd5": "OFF2_2160_SKU5_SUP01_Q",
                                            "PriceAfterCashback": 2160,
                                        },
                                    },
                                    {
                                        "RejectReason": "TOO_HIGH_PRICE",
                                        "Offer": {
                                            "WareMd5": "OFF3_2250_SKU5_SUP01_Q",
                                            "PriceAfterCashback": 2250,
                                        },
                                    },
                                ],
                            },
                        },
                    },
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )

        rearr_flags_dict = base_rearr_flags_dict.copy()
        rearr_flags_dict["market_buybox_enable_advert_buybox"] = 0
        rearr_flags_dict["market_buybox_auction_cpa_shop_incut"] = 1
        rearr_flags_dict["market_buybox_auction_coef_b_multiplicative_bid_coef_cs_incut"] = 0.001
        rearr_flags_dict["market_buybox_adv_buybox_price_rel_max_threshold"] = 1.05

        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        request = 'place=cpa_shop_incut&pp=18&text=fifth&numdoc=10&min-num-doc=1&debug=1&rearr-factors={}'.format(
            rearr_flags_str
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "wareId": "OFF1_2000_SKU5_SUP01_Q",
                        "debug": {
                            "buyboxDebug": {
                                "Offers": [
                                    {
                                        "WareMd5": "OFF1_2000_SKU5_SUP01_Q",
                                        "PriceAfterCashback": 2000,
                                    },
                                    {
                                        "WareMd5": "OFF2_2160_SKU5_SUP01_Q",
                                        "PriceAfterCashback": 2160,
                                    },
                                ],
                                "RejectedOffers": [
                                    {
                                        "RejectReason": "TOO_HIGH_PRICE",
                                        "Offer": {
                                            "WareMd5": "OFF3_2250_SKU5_SUP01_Q",
                                            "PriceAfterCashback": 2250,
                                        },
                                    },
                                ],
                            },
                        },
                    },
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )

    # Проверяем случай, когда все оффера со ставками отсеиваются по цене и gmv.
    # Несмотря на то, что последний оффер не имеет ставки он все равно должен проходить порог по ставке.
    def test_auction_error(self):
        request = (
            'place=productoffers&rids=213&market-sku=6&offers-set=defaultList&yandexuid=1&debug=1'
            '&rearr-factors=market_buybox_enable_advert_buybox=1;market_buybox_auction_cpa_fee=1'
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "debug": {
                            "buyboxDebug": {
                                "WonMethod": "SINGLE_OFFER_AFTER_BUYBOX_FILTERS",
                                "Offers": [
                                    {
                                        "WareMd5": "OFF1_2000_SKU6_SUP01_Q",
                                        "ShopFee": 0,
                                    },
                                ],
                                "RejectedOffers": [
                                    {
                                        "RejectReason": "TOO_HIGH_PRICE",
                                        "Offer": {
                                            "WareMd5": "OFF4_2260_SKU6_SUP01_Q",
                                            "ShopFee": 100,
                                        },
                                    },
                                    {
                                        "RejectReason": "TOO_HIGH_PRICE",
                                        "Offer": {
                                            "WareMd5": "OFF2_2050_SKU6_SUP01_Q",
                                            "ShopFee": 500,
                                        },
                                    },
                                    {
                                        "RejectReason": "TOO_HIGH_PRICE",
                                        "Offer": {
                                            "WareMd5": "OFF3_2160_SKU6_SUP01_Q",
                                            "ShopFee": 100,
                                        },
                                    },
                                ],
                            },
                        },
                    },
                ],
            },
        )

    def test_right_border_elasticity(self):
        rearr_flags_dict = base_rearr_flags_dict.copy()
        rearr_flags_dict["market_buybox_enable_advert_buybox"] = 1

        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        request = (
            'pp=7&hid=94307&place=prime&rgb=green_with_blue&rids=213&market-sku=7'
            '&show-urls=external,decrypted,direct%2Ccpa&bsformat=2&&viewtype=list&use-default-offers=1'
            '&allow-collapsing=1&allow-ungrouping=1&waitall=da&numdoc=48&debug=1'
            '&rearr-factors={}'.format(rearr_flags_str)
        )

        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            "offers": {
                                "items": [
                                    {
                                        "wareId": "OFF1_2000_SKU7_SUP01_Q",
                                        "debug": {
                                            "buyboxDebug": {
                                                "Offers": [
                                                    {
                                                        "WareMd5": "OFF1_2000_SKU7_SUP01_Q",
                                                        "PredictedElasticity": {
                                                            "Value": 160,
                                                            "Type": "NORMAL",
                                                        },
                                                        "Gmv": Greater(0),
                                                    },
                                                    {
                                                        "WareMd5": "OFF2_2050_SKU7_SUP01_Q",
                                                        "PredictedElasticity": {
                                                            "Value": 95,
                                                            "Type": "NORMAL",
                                                        },
                                                        "Gmv": Greater(0),
                                                    },
                                                ],
                                                "RejectedOffers": [
                                                    {
                                                        "RejectReason": "TOO_HIGH_PRICE",
                                                        "Offer": {
                                                            "WareMd5": "OFF4_2260_SKU7_SUP01_Q",
                                                            "PredictedElasticity": {
                                                                "Value": 0,
                                                                "Type": "RIGHT_BORDER",
                                                            },
                                                        },
                                                    },
                                                    {
                                                        "RejectReason": "TOO_HIGH_PRICE",
                                                        "Offer": {
                                                            "WareMd5": "OFF3_2160_SKU7_SUP01_Q",
                                                            "PredictedElasticity": {
                                                                "Value": 0,
                                                                "Type": "RIGHT_BORDER",
                                                            },
                                                        },
                                                    },
                                                ],
                                            },
                                        },
                                    },
                                ],
                            },
                        },
                        {
                            "offers": {
                                "items": [
                                    {
                                        "wareId": "OFF3_2160_SKU7_SUP01_Q",
                                        "sponsored": True,
                                        "debug": {
                                            "buyboxDebug": {
                                                "Offers": [
                                                    {
                                                        "WareMd5": "OFF3_2160_SKU7_SUP01_Q",
                                                        "PredictedElasticity": {
                                                            "Value": 0.745761,
                                                            "Type": "DEFAULT",
                                                        },
                                                        "Gmv": Greater(0),
                                                    },
                                                ],
                                                "RejectedOffers": [
                                                    {
                                                        "RejectReason": "TOO_HIGH_PRICE",
                                                        "Offer": {
                                                            "WareMd5": "OFF4_2260_SKU7_SUP01_Q",
                                                            "PredictedElasticity": {
                                                                "Value": 0,
                                                                "Type": "RIGHT_BORDER",
                                                            },
                                                        },
                                                    },
                                                    {
                                                        "RejectReason": "NOBID_PRIORITY",
                                                        "Offer": {
                                                            "WareMd5": "OFF1_2000_SKU7_SUP01_Q",
                                                            "PredictedElasticity": {
                                                                "Value": 160,
                                                                "Type": "NORMAL",
                                                            },
                                                            "Gmv": Greater(0),
                                                        },
                                                    },
                                                    {
                                                        "RejectReason": "NOBID_PRIORITY",
                                                        "Offer": {
                                                            "WareMd5": "OFF2_2050_SKU7_SUP01_Q",
                                                            "PredictedElasticity": {
                                                                "Value": 95,
                                                                "Type": "NORMAL",
                                                            },
                                                            "Gmv": Greater(0),
                                                        },
                                                    },
                                                ],
                                            },
                                        },
                                    },
                                ],
                            },
                        },
                    ],
                }
            },
            preserve_order=True,
            allow_different_len=False,
        )

    def test_won_method_to_click_log(self):
        # Проверяем что правильно работает порог по цене в cpa_ship_incat
        rearr_flags_dict = base_rearr_flags_dict.copy()
        rearr_flags_dict["market_buybox_enable_advert_buybox"] = 1
        rearr_flags_dict["market_buybox_auction_cpa_shop_incut"] = 1
        rearr_flags_dict["market_buybox_auction_coef_b_multiplicative_bid_coef_cs_incut"] = 0.001
        rearr_flags_dict["market_buybox_adv_buybox_price_rel_max_threshold"] = 1.05

        rearr_flags_str = dict_to_rearr(rearr_flags_dict)
        request = 'place=cpa_shop_incut&pp=18&text=fifth&numdoc=10&min-num-doc=1&debug=1&rearr-factors={}'.format(
            rearr_flags_str
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "wareId": "OFF1_2000_SKU5_SUP01_Q",
                        'urls': {
                            'cpa': Contains('ware_md5=OFF1_2000_SKU5_SUP01_Q/', '/won_method=3/'),
                        },
                    },
                ],
            },
            preserve_order=True,
            allow_different_len=False,
        )
        self.click_log.expect(ClickType.CPA, position=1, won_method=3)

    # Проверяем розыгрыш байбокса ML формулой с учетом реджекта офферов по TOO_HIGH_PRICE
    def test_buybox_ml(self):
        rearr_flags_dict = base_rearr_flags_dict.copy()
        rearr_flags_dict["market_buybox_enable_advert_buybox"] = 1
        rearr_flags_dict["enable_advertisement_buybox_ml"] = 1
        rearr_flags_str = dict_to_rearr(rearr_flags_dict)

        request = (
            'place=productoffers&rids=213&market-sku=8&yandexuid=1&debug=1&numdoc=48&use-default-offers=1&offers-set=defaultList'
            '&rearr-factors={}'.format(rearr_flags_str)
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "wareId": "OFF2_2050_SKU8_SUP01_Q",
                        "debug": {
                            "rank": [{"name": "MATRIXNET_VALUE", "value": "901943104"}],
                            "buyboxDebug": {
                                "WonMethod": "WON_BY_EXCHANGE",
                                "Offers": [
                                    {
                                        "WareMd5": "OFF1_2000_SKU8_SUP01_Q",
                                    },
                                    {
                                        "WareMd5": "OFF2_2050_SKU8_SUP01_Q",
                                    },
                                ],
                                "RejectedOffers": [
                                    {
                                        "RejectReason": "TOO_HIGH_PRICE",
                                        "Offer": {
                                            "WareMd5": "OFF3_2160_SKU8_SUP01_Q",
                                        },
                                    },
                                    {
                                        "RejectReason": "TOO_HIGH_PRICE",
                                        "Offer": {
                                            "WareMd5": "OFF4_2260_SKU8_SUP01_Q",
                                        },
                                    },
                                ],
                            },
                        },
                    },
                ],
            },
        )

        rearr_flags_dict = base_rearr_flags_dict.copy()
        rearr_flags_dict["market_buybox_enable_advert_buybox"] = 1
        rearr_flags_dict["enable_advertisement_buybox_ml"] = 0
        rearr_flags_str = dict_to_rearr(rearr_flags_dict)

        request = (
            'place=productoffers&rids=213&market-sku=8&yandexuid=1&debug=1&numdoc=48&use-default-offers=1&offers-set=defaultList'
            '&rearr-factors={}'.format(rearr_flags_str)
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "wareId": "OFF1_2000_SKU8_SUP01_Q",
                        "debug": {
                            "rank": [{"name": "MATRIXNET_VALUE", "value": "869730880"}],
                            "buyboxDebug": {
                                "WonMethod": "WON_BY_EXCHANGE",
                                "Offers": [
                                    {
                                        "WareMd5": "OFF1_2000_SKU8_SUP01_Q",
                                    },
                                    {
                                        "WareMd5": "OFF2_2050_SKU8_SUP01_Q",
                                    },
                                ],
                                "RejectedOffers": [
                                    {
                                        "RejectReason": "TOO_HIGH_PRICE",
                                        "Offer": {
                                            "WareMd5": "OFF3_2160_SKU8_SUP01_Q",
                                        },
                                    },
                                    {
                                        "RejectReason": "TOO_HIGH_PRICE",
                                        "Offer": {
                                            "WareMd5": "OFF4_2260_SKU8_SUP01_Q",
                                        },
                                    },
                                ],
                            },
                        },
                    },
                ],
            },
        )


if __name__ == '__main__':
    main()
