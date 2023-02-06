#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.types import (
    BucketInfo,
    CategoryRestriction,
    ComparisonOperation,
    CostModificationRule,
    Currency,
    DateSwitchTimeAndRegionInfo,
    DeliveryBucket,
    DeliveryCalcFeedInfo,
    DeliveryCalcShopInfo,
    DeliveryCostCondition,
    DeliveryModifier,
    DeliveryModifierCondition,
    DeliveryOption,
    DeliveryServicePriorityAndRegionInfo,
    DeliveryServiceRegionToRegionInfo,
    Dimensions,
    Disclaimer,
    DynamicDaysSet,
    DynamicDeliveryServiceInfo,
    DynamicSkuOffer,
    DynamicTimeIntervalsSet,
    DynamicWarehouseAndDeliveryServiceInfo,
    DynamicWarehouseToWarehouseInfo,
    DynamicWarehouseInfo,
    DynamicWarehousesPriorityInRegion,
    ExchangeRate,
    GLParam,
    GpsCoord,
    HyperCategory,
    Model,
    ModificationOperation,
    NewPickupBucket,
    NewPickupOption,
    Offer,
    OfferDeliveryInfo,
    Outlet,
    OutletDeliveryOption,
    PickupBucket,
    PickupOption,
    PickupRegionGroup,
    PreorderDates,
    Region,
    RegionalDelivery,
    RegionalRestriction,
    RegionsAvailability,
    Shop,
    TimeInfo,
    TimeIntervalInfo,
    TimeIntervalsForDaysInfo,
    TimeIntervalsForRegion,
    TimeModificationRule,
)

from core.logs import ErrorCodes
from core.matcher import Contains, Not, NotEmpty, Regex
from core.matcher import NoKey, Absent
from core.testcase import TestCase, main
from core.types.offer import OfferDimensions
from core.types.payment_methods import Payment
from core.types.sku import MarketSku, BlueOffer
from core.types.taxes import Vat, Tax
from core.delivery_calc import DCBucketInfo

from unittest import skip
from copy import copy

USE_DEPRECATED_DIRECT_SHIPPING_FLOW = "&rearr-factors=get_rid_of_direct_shipping=0"

pickup_option_groups_part = [
    {"serviceId": 103, "outletIds": [2001], "groupCount": 1},
    {"serviceId": 123, "outletIds": [2004, 2005], "paymentMethods": Absent(), "groupCount": 2},
]


def post_option_ungrouped(outlet_id, service_id, post_code, price, shipment_day, day_from, day_to, order_before):
    return {
        "serviceId": service_id,
        "outlet": {
            "id": str(outlet_id),
            "type": "post",
            "postCode": post_code,
        },
        "price": {"value": str(price)},
        "shipmentDay": shipment_day,
        "dayFrom": day_from,
        "dayTo": day_to,
        "orderBefore": order_before,
    }


def post_option_grouped(
    outlet_ids, post_codes, service_id, price, shipment_day, day_from, day_to, order_before, payment_methods
):
    return {
        "serviceId": service_id,
        "price": {"value": str(price)},
        "shipmentDay": shipment_day,
        "dayFrom": day_from,
        "dayTo": day_to,
        "orderBefore": order_before,
        "groupCount": len(outlet_ids),
        "outletIds": outlet_ids,
        "postCodes": post_codes,
        "paymentMethods": payment_methods,
    }


post_options_grouped_fragment = {
    "results": [
        {
            "entity": "deliveryGroup",
            "delivery": {
                "hasPost": True,
                "postOptions": [
                    post_option_grouped(
                        outlet_ids=[4102],
                        post_codes=[115201],
                        service_id=202,
                        price=6,
                        shipment_day=7,
                        day_from=8,
                        day_to=8,
                        order_before=20,
                        payment_methods=["YANDEX", "CASH_ON_DELIVERY", "CARD_ON_DELIVERY"],
                    ),
                    post_option_grouped(
                        outlet_ids=[4002],
                        post_codes=[115201],
                        service_id=201,
                        price=6,
                        shipment_day=7,
                        day_from=8,
                        day_to=8,
                        order_before=20,
                        payment_methods=["YANDEX", "CASH_ON_DELIVERY"],
                    ),
                    post_option_grouped(
                        outlet_ids=[4001],
                        post_codes=[115200],
                        service_id=201,
                        price=7,
                        shipment_day=7,
                        day_from=8,
                        day_to=9,
                        order_before=20,
                        payment_methods=["YANDEX", "CASH_ON_DELIVERY", "CARD_ON_DELIVERY"],
                    ),
                    post_option_grouped(
                        outlet_ids=[4101],
                        post_codes=[115200],
                        service_id=202,
                        price=7,
                        shipment_day=7,
                        day_from=8,
                        day_to=10,
                        order_before=20,
                        payment_methods=["YANDEX", "CASH_ON_DELIVERY", "CARD_ON_DELIVERY"],
                    ),
                ],
                "postStats": {
                    "minDays": 8,
                    "maxDays": 10,
                    "minPrice": {
                        "value": "6",
                    },
                    "maxPrice": {
                        "value": "7",
                    },
                },
            },
        }
    ]
}


def user_price_flag(value):
    return '&rearr-factors=market_use_delivery_service_user_price={}'.format(1 if value else 0)


# expected options for delivery_service 257 (day_from, day_to, intervals=[(hour_from, min_from, hour_to, min_to), ...])
# user_price = 3
CHEAP_OPTIONS_257 = (
    (11, 12, [(10, 00, 18, 30)]),
    (12, 12, [(10, 00, 18, 30), (19, 15, 23, 45)]),
    (16, 16, [(10, 00, 17, 30)]),
)
# user_price = 5
FAST_OPTIONS_257 = (
    (8, 9, [(9, 00, 14, 30)]),
    (9, 9, [(9, 00, 14, 30)]),
    (11, 11, [(9, 00, 14, 30)]),
    (12, 12, [(10, 00, 18, 30), (19, 15, 23, 45)]),
)

NO_COMBINATOR_FLAG = '&combinator=0'


class _Shops(object):
    virtual_shop_blue = Shop(
        fesh=1,
        datafeed_id=1,
        priority_region=213,
        name='virtual_shop',
        currency=Currency.RUR,
        tax_system=Tax.OSN,
        fulfillment_virtual=True,
        cpa=Shop.CPA_REAL,
        virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
        delivery_service_outlets=[
            2001,
            2004,
            2005,
            3001,
            3002,
            3003,
            3004,
            3005,
            3006,
            4001,
            4002,
            4101,
            4102,
            4201,
            4203,
            6001,
            6002,
            6003,
            8001,
            8002,
        ],
    )

    blue_shop_1 = Shop(
        fesh=3,
        datafeed_id=3,
        priority_region=2,
        name='blue_shop_1',
        currency=Currency.RUR,
        tax_system=Tax.OSN,
        supplier_type=Shop.FIRST_PARTY,
        blue='REAL',
        warehouse_id=145,
    )

    blue_shop_2 = Shop(
        fesh=4,
        datafeed_id=4,
        priority_region=213,
        name='blue_shop_2',
        currency=Currency.RUR,
        tax_system=Tax.OSN,
        supplier_type=Shop.THIRD_PARTY,
        blue='REAL',
        warehouse_id=145,
    )

    blue_shop_3 = Shop(
        fesh=5,
        datafeed_id=5,
        priority_region=2,
        name='blue_shop_3',
        currency=Currency.RUR,
        tax_system=Tax.OSN,
        supplier_type=Shop.FIRST_PARTY,
        blue='REAL',
        warehouse_id=145,
    )

    blue_shop_4 = Shop(
        fesh=6,
        datafeed_id=6,
        priority_region=2,
        name='blue_shop_4',
        currency=Currency.RUR,
        tax_system=Tax.OSN,
        supplier_type=Shop.FIRST_PARTY,
        blue='REAL',
        warehouse_id=146,
    )

    blue_shop_5 = Shop(
        fesh=55,
        datafeed_id=55,
        priority_region=2,
        name='blue_shop_5',
        currency=Currency.RUR,
        tax_system=Tax.OSN,
        supplier_type=Shop.FIRST_PARTY,
        blue='REAL',
        warehouse_id=400,
    )

    pharmacy = Shop(
        fesh=7,
        datafeed_id=7,
        priority_region=213,
        name='Pharmacy',
        currency=Currency.RUR,
        tax_system=Tax.OSN,
        supplier_type=Shop.THIRD_PARTY,
        blue='REAL',
        warehouse_id=160,
        fulfillment_program=False,
    )

    dropship_supplier = Shop(
        fesh=8,
        datafeed_id=8,
        priority_region=213,
        name='Dropship',
        currency=Currency.RUR,
        tax_system=Tax.OSN,
        supplier_type=Shop.THIRD_PARTY,
        blue='REAL',
        warehouse_id=164,
        fulfillment_program=False,
    )

    dropship_supplier_in_child_region = Shop(
        fesh=9,
        datafeed_id=9,
        priority_region=215,
        currency=Currency.RUR,
        tax_system=Tax.OSN,
        supplier_type=Shop.THIRD_PARTY,
        blue='REAL',
        warehouse_id=198,
        fulfillment_program=False,
    )

    blue_shop_11 = Shop(
        fesh=11,
        datafeed_id=11,
        priority_region=2,
        name='blue_shop_11',
        currency=Currency.RUR,
        tax_system=Tax.OSN,
        supplier_type=Shop.FIRST_PARTY,
        blue='REAL',
        warehouse_id=145,
    )

    white_shop_12 = Shop(
        fesh=12,
        datafeed_id=112,
        priority_region=213,
        regions=[213, 216],
        name='white_shop_12',
        currency=Currency.RUR,
        tax_system=Tax.OSN,
        client_id=12,
        cpa=Shop.CPA_REAL,
    )

    white_shop_13 = Shop(
        fesh=13,
        datafeed_id=13,
        priority_region=213,
        regions=[213, 216],
        name='white_shop_13',
        currency=Currency.RUR,
        tax_system=Tax.OSN,
        client_id=12,
        cpa=Shop.CPA_REAL,
    )

    white_shop_14 = Shop(
        fesh=14,
        datafeed_id=114,
        priority_region=213,
        regions=[213],
        name='white_shop_14',
        currency=Currency.RUR,
        tax_system=Tax.OSN,
        cpa=Shop.CPA_REAL,
    )

    white_shop_15 = Shop(
        fesh=15,
        datafeed_id=115,
        priority_region=213,
        regions=[213],
        name='white_shop_15',
        currency=Currency.RUR,
        tax_system=Tax.OSN,
        cpa=Shop.CPA_REAL,
    )

    white_shop_16 = Shop(
        fesh=16,
        datafeed_id=116,
        priority_region=213,
        regions=[213],
        name='white_shop_16',
        currency=Currency.RUR,
        tax_system=Tax.OSN,
        cpa=Shop.CPA_REAL,
    )

    white_shop_bad_options = Shop(
        fesh=17,
        datafeed_id=117,
        priority_region=213,
        regions=[213],
        name='white_shop_with_bad_delivery_options',
        currency=Currency.RUR,
        tax_system=Tax.OSN,
        cpa=Shop.CPA_REAL,
    )


class _Offers(object):
    sku1_offer1 = BlueOffer(
        price=5,
        vat=Vat.VAT_10,
        feedid=3,
        offerid='blue.offer.1.1',
        waremd5='Sku1Price5-IiLVm1Goleg',
        weight=5,
        dimensions=OfferDimensions(length=20, width=30, height=10),
        cargo_types=[44, 55, 66],
    )
    sku1_offer2 = BlueOffer(
        price=50,
        vat=Vat.VAT_0,
        feedid=4,
        offerid='blue.offer.1.2',
        waremd5='Sku1Price50-iLVm1Goleg',
        weight=10,
        dimensions=OfferDimensions(length=5, width=15, height=50),
    )
    sku1_offer3 = BlueOffer(
        price=2990,
        vat=Vat.VAT_10,
        feedid=5,
        offerid='blue.offer.1.3',
        waremd5='Sku1Pr2990-IiLVm1Goleg',
        weight=5,
        dimensions=OfferDimensions(length=20, width=30, height=10),
    )
    sku1_offer4 = BlueOffer(
        price=2991,
        vat=Vat.VAT_10,
        feedid=5,
        offerid='blue.offer.1.4',
        waremd5='Sku1Pr2991-IiLVm1Goleg',
        weight=5,
        dimensions=OfferDimensions(length=20, width=30, height=10),
    )
    sku1_offer5 = BlueOffer(
        price=2992,
        vat=Vat.VAT_10,
        feedid=5,
        offerid='blue.offer.1.5',
        waremd5='Sku1Pr2992-IiLVm1Goleg',
        weight=5,
        dimensions=OfferDimensions(length=20, width=30, height=10),
    )
    sku1_offer6 = BlueOffer(
        price=1496,
        vat=Vat.VAT_10,
        feedid=5,
        offerid='blue.offer.1.6',
        waremd5='Sku1Pr1496-IiLVm1Goleg',
        weight=2,
        dimensions=OfferDimensions(length=20, width=30, height=5),
    )

    sku1_offer7 = BlueOffer(
        price=1122,
        vat=Vat.VAT_10,
        feedid=5,
        offerid='blue.offer.1.7',
        waremd5='Sku1Pr1122-IiLVm1Goleg',
        weight=2,
        dimensions=OfferDimensions(length=2, width=8, height=21),
    )

    sku2_offer1 = BlueOffer(
        price=55,
        vat=Vat.VAT_18,
        feedid=3,
        offerid='blue.offer.2.1',
        waremd5='Sku2Price55-iLVm1Goleg',
        weight=7,
        dimensions=OfferDimensions(length=20, width=30, height=10),
    )
    sku2_offer2 = BlueOffer(
        price=50,
        vat=Vat.NO_VAT,
        feedid=4,
        offerid='blue.offer.2.2',
        waremd5='Sku2Price50-iLVm1Goleg',
        weight=10,
        dimensions=OfferDimensions(length=5, width=15, height=50),
    )

    sku3_offer1 = BlueOffer(
        price=55,
        vat=Vat.VAT_18,
        feedid=3,
        offerid='blue.offer.3.1',
        waremd5='Sku3Price55-iLVm1Goleg',
        weight=5,
        dimensions=OfferDimensions(length=20, width=30, height=10),
    )

    sku4_offer1 = BlueOffer(
        price=55,
        vat=Vat.VAT_18,
        feedid=5,
        offerid='blue.offer.4.1',
        waremd5='Sku4-1Price55-Vm1Goleg',
        weight=5,
        dimensions=OfferDimensions(length=10, width=15, height=5),
    )
    sku4_offer2 = BlueOffer(
        price=55,
        vat=Vat.VAT_18,
        feedid=6,
        offerid='blue.offer.4.2',
        waremd5='Sku4-2Price55-Vm1Goleg',
        weight=5,
        dimensions=OfferDimensions(length=10, width=15, height=5),
    )

    sku5_offer1 = BlueOffer(
        price=11223,
        vat=Vat.VAT_10,
        feedid=55,
        offerid='blue.offer.5.1',
        waremd5='Sku5Pr1121-IiLVm1Goleg',
        weight=20,
        blue_weight=20,
        dimensions=OfferDimensions(length=2, width=8, height=21),
        blue_dimensions=OfferDimensions(length=2, width=8, height=21),
        cargo_types=[456, 789, 300],
    )

    sku8_offer1 = BlueOffer(
        price=55,
        vat=Vat.VAT_18,
        feedid=3,
        offerid='blue.offer.8.1',
        waremd5='Sku8Price55-iLVm1Goleg',
        weight=7,
        dimensions=OfferDimensions(length=22, width=30, height=10),
    )

    pharma_offer = BlueOffer(
        price=710,
        vat=Vat.VAT_18,
        feedid=7,
        offerid='cure_for_cancer',
        waremd5='BlueOfferPharmaOffer_g',
        weight=7,
        dimensions=OfferDimensions(length=710, width=71, height=7),
        is_fulfillment=False,
    )
    dropship_offer = BlueOffer(
        price=81000,
        vat=Vat.VAT_18,
        feedid=8,
        offerid='fridge_dropship',
        waremd5='BlueOfferDropS_Fridgeg',
        weight=8,
        cargo_types=[300],
        dimensions=OfferDimensions(length=810, width=81, height=8),
        is_fulfillment=False,
    )
    dropship_offer_heavy = BlueOffer(
        price=81000,
        vat=Vat.VAT_18,
        feedid=8,
        offerid='heavy_dropship',
        waremd5='BlueOfferDropS_Heavyeg',
        weight=20,
        blue_weight=20,
        dimensions=OfferDimensions(length=810, width=81, height=8),
        blue_dimensions=OfferDimensions(length=810, width=81, height=8),
        is_fulfillment=False,
    )
    sku9_offer1 = BlueOffer(
        price=50,
        vat=Vat.NO_VAT,
        feedid=55,
        offerid='blue.offer.9.1',
        waremd5='Sku9Price50-iLVm1Goleg',
        weight=11,
        dimensions=OfferDimensions(length=543, width=175, height=357),
    )

    sku10_offer1 = BlueOffer(
        price=70,
        vat=Vat.VAT_18,
        feedid=5,
        offerid='blue.offer.10.1',
        waremd5='TestTariffWarning____g',
        weight=10,
        dimensions=OfferDimensions(length=8, width=1, height=2),
        delivery_buckets=[802],
    )
    sku10_offer2 = BlueOffer(
        price=80,
        vat=Vat.VAT_18,
        feedid=5,
        offerid='blue.offer.10.2',
        waremd5='TestTariffWarning_2__g',
        weight=15,
        dimensions=OfferDimensions(length=19, width=4, height=200),
        delivery_buckets=[801],
    )
    BigOffer = BlueOffer(
        price=2000,
        vat=Vat.NO_VAT,
        feedid=11,
        offerid='blue.offer.big.1.1',
        waremd5='TestSplitBigOffer_1__g',
        weight=23,
        dimensions=OfferDimensions(length=170, width=60, height=60),
        delivery_buckets=[801],
    )
    SmallOfferForMaxBox = BlueOffer(
        price=20,
        vat=Vat.NO_VAT,
        feedid=11,
        offerid='blue.offer.small.1.1',
        waremd5='TestSplitSmallOffer_1g',
        weight=23,
        dimensions=OfferDimensions(length=77, width=44.75, height=34),
        delivery_buckets=[802, 803],
    )
    sku15_offer1 = BlueOffer(
        price=5,
        vat=Vat.VAT_10,
        feedid=3,
        offerid='blue.offer.1.15',
        waremd5='Sku1Price5-JiLVm1Goleg',
        weight=5,
        dimensions=OfferDimensions(length=22, width=33, height=11),
        cargo_types=[44, 55, 66],
    )
    sku15_offer2 = BlueOffer(
        price=55,
        vat=Vat.VAT_18,
        feedid=3,
        offerid='blue.offer.15.2',
        waremd5='TestMultiPreorderCartg',
        weight=7,
        dimensions=OfferDimensions(length=20, width=35.5, height=10),
        delivery_buckets=[801, 803],
        pickup_buckets=[5001, 5002, 5004],
        post_term_delivery=True,
    )
    dropship_offer1 = BlueOffer(
        price=55,
        vat=Vat.VAT_18,
        feedid=9,
        offerid='blue.offer.15.3',
        waremd5='TestChildRegionDelivgg',
        weight=7,
        dimensions=OfferDimensions(length=20, width=35.5, height=10),
        stock_store_count=7,
    )

    # offers for external service errors test
    error_401_offer = BlueOffer(
        fesh=1001,
        price=10000,
        waremd5='22222222222222gggg401g',
        weight=4,
        dimensions=OfferDimensions(length=22, width=22, height=22),
    )
    error_501_offer = BlueOffer(
        fesh=1001,
        price=10000,
        waremd5='22222222222222gggg501g',
        weight=5,
        dimensions=OfferDimensions(length=22, width=22, height=22),
    )
    # offer for unknown bucket test
    error_404_offer = BlueOffer(
        fesh=1001,
        price=10000,
        waremd5='22222222222222gggg404g',
        weight=404,
        dimensions=OfferDimensions(length=404, width=404, height=404),
    )


class T(TestCase):
    @classmethod
    def prepare_blue_delivery_price(cls):
        '''
        Отключаем цену доставки синих оферов от пользователя,
        т.к. в этом тесте исторически много завязок на цену в тарифе
        https://st.yandex-team.ru/MARKETOUT-34206
        '''
        cls.settings.blue_delivery_price_enabled = False

    @classmethod
    def prepare(cls):
        cls.settings.cloud_service = 'test_report_lite'
        cls.settings.default_search_experiment_flags += ['disable_delivery_calculator_call_for_blue_offers=0']
        # НЕ делайте так в новых тестах!
        # Походов в КД на проде уже нет, пожалуйста, проверяйте новую функциональность, создавая доставку через комбинатор
        cls.settings.default_search_experiment_flags += ['force_disable_delivery_calculator_requests=0']
        cls.index.regiontree += [
            Region(rid=2, tz_offset=-10800),
            Region(
                rid=213,
                name='Москва',
                children=[
                    Region(rid=216, name='Зеленоград', tz_offset=10800),
                ],
            ),
            Region(rid=39, name='Ростов-на-Дону'),
            Region(
                rid=217,
                name='Свердловская область',
                children=[
                    Region(rid=215, name='Екатеринбург', tz_offset=10800),
                ],
            ),
        ]

        cls.settings.blue_market_free_delivery_threshold = 3000
        cls.settings.blue_market_prime_free_delivery_threshold = 2991
        cls.settings.blue_market_yandex_plus_free_delivery_threshold = 2992

        cls.index.models += [
            Model(hyperid=7001),
            Model(hyperid=7002),
        ]

        cls.index.shops += [
            _Shops.virtual_shop_blue,
            _Shops.blue_shop_1,
            _Shops.blue_shop_2,
            _Shops.blue_shop_3,
            _Shops.blue_shop_4,
            _Shops.blue_shop_5,
            _Shops.pharmacy,
            _Shops.dropship_supplier,
            _Shops.blue_shop_11,
            _Shops.dropship_supplier_in_child_region,
            _Shops.white_shop_12,
            _Shops.white_shop_13,
            _Shops.white_shop_14,
            _Shops.white_shop_15,
            _Shops.white_shop_16,
            _Shops.white_shop_bad_options,
        ]

        cls.settings.lms_autogenerate = False
        cls.dynamic.lms += [
            DynamicWarehouseInfo(id=145, home_region=213, holidays_days_set_key=1),
            DynamicWarehouseInfo(id=160, home_region=213, holidays_days_set_key=1),
            DynamicWarehouseInfo(id=164, home_region=213, holidays_days_set_key=1),
            DynamicWarehouseInfo(id=198, home_region=215, holidays_days_set_key=1),
            DynamicWarehouseInfo(id=400, home_region=2, holidays_days_set_key=4),
            DynamicWarehouseToWarehouseInfo(warehouse_from=145, warehouse_to=145),
            DynamicWarehouseToWarehouseInfo(warehouse_from=160, warehouse_to=160),
            DynamicWarehouseToWarehouseInfo(warehouse_from=164, warehouse_to=164),
            DynamicWarehouseToWarehouseInfo(warehouse_from=198, warehouse_to=198),
            DynamicWarehouseToWarehouseInfo(warehouse_from=400, warehouse_to=400),
            DynamicDeliveryServiceInfo(id=99, name='self-delivery'),
            DynamicDeliveryServiceInfo(id=199, name='self-delivery_199'),
            DynamicDeliveryServiceInfo(id=103, name='c_103'),
            DynamicDeliveryServiceInfo(id=123, name='c_123'),
            DynamicDeliveryServiceInfo(id=127, name='c_127'),
            DynamicDeliveryServiceInfo(id=157, name='c_157', rating=1),
            DynamicDeliveryServiceInfo(
                id=158,
                name='c_158',
                rating=2,
                time_intervals=[
                    TimeIntervalsForRegion(
                        region=213,
                        intervals=[
                            TimeIntervalsForDaysInfo(intervals_key=1, days_key=3),
                        ],
                    ),
                ],
            ),
            DynamicDeliveryServiceInfo(
                id=159,
                name='c_159',
                time_intervals=[
                    TimeIntervalsForRegion(
                        region=213,
                        intervals=[
                            TimeIntervalsForDaysInfo(intervals_key=2, days_key=7),
                        ],
                    ),
                ],
            ),
            DynamicDeliveryServiceInfo(id=161, name='c_161'),
            DynamicDeliveryServiceInfo(id=165, name='dropship_delivery'),
            DynamicDeliveryServiceInfo(
                id=257,
                name='c_257',
                rating=1,
                region_to_region_info=[
                    DeliveryServiceRegionToRegionInfo(region_from=213, region_to=213, days_key=2),
                ],
                time_intervals=[
                    TimeIntervalsForRegion(
                        region=213,
                        intervals=[
                            TimeIntervalsForDaysInfo(intervals_key=4, days_key=3),
                            TimeIntervalsForDaysInfo(intervals_key=0, days_key=4),
                            TimeIntervalsForDaysInfo(intervals_key=1, days_key=5),
                        ],
                    )
                ],
            ),
            DynamicDeliveryServiceInfo(
                id=258,
                name='c_258',
                rating=2,
                region_to_region_info=[
                    DeliveryServiceRegionToRegionInfo(region_from=213, region_to=213, days_key=2),
                    DeliveryServiceRegionToRegionInfo(region_from=213, region_to=2, days_key=2),
                ],
                time_intervals=[
                    TimeIntervalsForRegion(
                        region=225,
                        intervals=[
                            TimeIntervalsForDaysInfo(intervals_key=4, days_key=6),
                            TimeIntervalsForDaysInfo(intervals_key=2, days_key=4),
                            TimeIntervalsForDaysInfo(intervals_key=3, days_key=5),
                        ],
                    ),
                    TimeIntervalsForRegion(
                        region=2,
                        intervals=[
                            TimeIntervalsForDaysInfo(intervals_key=3, days_key=6),
                            TimeIntervalsForDaysInfo(intervals_key=4, days_key=4),
                            TimeIntervalsForDaysInfo(intervals_key=4, days_key=5),
                        ],
                    ),
                ],
            ),
            DynamicDeliveryServiceInfo(id=259, name='c_259'),
            DynamicDeliveryServiceInfo(
                id=300,
                region_to_region_info=[DeliveryServiceRegionToRegionInfo(region_from=213, region_to=213, days_key=2)],
                time_intervals=[
                    TimeIntervalsForRegion(
                        region=213,
                        intervals=[
                            TimeIntervalsForDaysInfo(intervals_key=0, days_key=4),
                            TimeIntervalsForDaysInfo(intervals_key=1, days_key=5),
                        ],
                    )
                ],
            ),
            DynamicDeliveryServiceInfo(
                id=400,
                region_to_region_info=[DeliveryServiceRegionToRegionInfo(region_from=2, region_to=2, days_key=4)],
            ),
            DynamicDeliveryServiceInfo(
                id=401,
                name='service from child region',
                region_to_region_info=[DeliveryServiceRegionToRegionInfo(region_from=215, region_to=2, days_key=1)],
            ),
            DynamicDeliveryServiceInfo(
                id=402,
                name='service from parent region',
                region_to_region_info=[DeliveryServiceRegionToRegionInfo(region_from=217, region_to=2, days_key=1)],
            ),
            DynamicDeliveryServiceInfo(id=201, name='c_201'),
            DynamicDeliveryServiceInfo(id=202, name='c_202'),
            DynamicDeliveryServiceInfo(id=203, name='c_203'),
            DynamicDaysSet(key=1, days=[0, 1, 2, 3, 4, 5, 6]),
            DynamicDaysSet(key=2, days=[13, 15]),
            DynamicDaysSet(key=3, days=[8, 9, 11]),
            DynamicDaysSet(key=4, days=[12]),
            DynamicDaysSet(key=5, days=[16]),
            DynamicDaysSet(key=6, days=[11]),
            DynamicDaysSet(key=7, days=[17]),
            DynamicTimeIntervalsSet(
                key=0,
                intervals=[
                    TimeIntervalInfo(TimeInfo(19, 15), TimeInfo(23, 45)),
                    TimeIntervalInfo(TimeInfo(10, 0), TimeInfo(18, 30)),
                ],
            ),
            DynamicTimeIntervalsSet(
                key=1,
                intervals=[
                    TimeIntervalInfo(TimeInfo(10, 0), TimeInfo(17, 30)),
                ],
            ),
            DynamicTimeIntervalsSet(
                key=2,
                intervals=[
                    TimeIntervalInfo(TimeInfo(11, 0), TimeInfo(17, 00)),
                ],
            ),
            DynamicTimeIntervalsSet(
                key=3,
                intervals=[
                    TimeIntervalInfo(TimeInfo(11, 0), TimeInfo(15, 10)),
                ],
            ),
            DynamicTimeIntervalsSet(
                key=4,
                intervals=[
                    TimeIntervalInfo(TimeInfo(9, 0), TimeInfo(14, 30)),
                ],
            ),
            DynamicWarehousesPriorityInRegion(region=225, warehouses=[145, 160, 164]),
            DynamicWarehousesPriorityInRegion(region=2, warehouses=[400]),
        ]

        cls.dynamic.lms += [
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=145,
                delivery_service_id=service,
                operation_time=0,
                date_switch_time_infos=[DateSwitchTimeAndRegionInfo(date_switch_hour=20, region_to=225)],
            )
            for service in [157, 158, 159, 257, 258, 259, 300, 103, 123, 127, 201, 202, 203, 161]
        ]
        cls.dynamic.lms.append(
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=160,
                delivery_service_id=99,
                operation_time=0,
                date_switch_time_infos=[DateSwitchTimeAndRegionInfo(date_switch_hour=19, region_to=225)],
            )
        )
        cls.dynamic.lms.append(
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=160,
                delivery_service_id=199,
                operation_time=0,
                date_switch_time_infos=[DateSwitchTimeAndRegionInfo(date_switch_hour=19, region_to=225)],
            )
        )
        cls.dynamic.lms.append(
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=164,
                delivery_service_id=165,
                operation_time=0,
                date_switch_time_infos=[DateSwitchTimeAndRegionInfo(date_switch_hour=22, region_to=225)],
            )
        )
        cls.dynamic.lms.append(
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=198,
                delivery_service_id=401,
                operation_time=0,
                date_switch_time_infos=[DateSwitchTimeAndRegionInfo(date_switch_hour=20, region_to=2)],
            )
        )
        cls.dynamic.lms.append(
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=198,
                delivery_service_id=402,
                operation_time=0,
                date_switch_time_infos=[DateSwitchTimeAndRegionInfo(date_switch_hour=20, region_to=2)],
            )
        )
        cls.dynamic.lms.append(
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=400,
                delivery_service_id=400,
                operation_time=0,
                date_switch_time_infos=[DateSwitchTimeAndRegionInfo(date_switch_hour=20, region_to=225)],
            )
        )

        cls.index.outlets += [
            Outlet(
                point_id=2001,
                delivery_service_id=103,
                region=213,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(
                    shipper_id=103, day_from=1, day_to=1, order_before=2, work_in_holiday=True, price=100
                ),
                working_days=[i for i in range(10)],
                bool_props=["prepayAllowed", "cashAllowed", "cardAllowed"],
                gps_coord=GpsCoord(37.12, 55.32),
            ),
            Outlet(
                point_id=2004,
                delivery_service_id=123,
                region=213,
                point_type=Outlet.FOR_POST_TERM,
                delivery_option=OutletDeliveryOption(shipper_id=123, day_from=1, day_to=1, price=400),
                working_days=[i for i in range(10)],
                gps_coord=GpsCoord(37.7, 55.7),
            ),
            Outlet(
                point_id=2005,
                delivery_service_id=123,
                region=213,
                point_type=Outlet.FOR_POST_TERM,
                delivery_option=OutletDeliveryOption(shipper_id=123, day_from=1, day_to=1, price=400),
                working_days=[i for i in range(10)],
                bool_props=["returnAllowed"],
                gps_coord=GpsCoord(37.7, 55.7),
            ),
            Outlet(
                point_id=3001,
                delivery_service_id=127,
                region=213,
                point_type=Outlet.FOR_POST_TERM,
                delivery_option=OutletDeliveryOption(shipper_id=127, day_from=1, day_to=1, price=400),
                working_days=[i for i in range(10)],
                gps_coord=GpsCoord(37.7, 55.7),
            ),
            Outlet(
                point_id=3002,
                delivery_service_id=127,
                region=213,
                point_type=Outlet.FOR_POST_TERM,
                delivery_option=OutletDeliveryOption(shipper_id=127, day_from=1, day_to=1, price=400),
                working_days=[i for i in range(10)],
                gps_coord=GpsCoord(37.7, 55.7),
            ),
            Outlet(
                point_id=3003,
                delivery_service_id=127,
                region=213,
                point_type=Outlet.FOR_POST_TERM,
                delivery_option=OutletDeliveryOption(shipper_id=127, day_from=1, day_to=1, price=400),
                working_days=[i for i in range(10)],
                bool_props=["prepayAllowed", "cashAllowed", "cardAllowed"],
                gps_coord=GpsCoord(37.7, 55.7),
            ),
            Outlet(
                point_id=3004,
                delivery_service_id=127,
                region=213,
                point_type=Outlet.FOR_POST_TERM,
                delivery_option=OutletDeliveryOption(shipper_id=127, day_from=1, day_to=1, price=400),
                working_days=[i for i in range(10)],
                bool_props=["prepayAllowed", "cashAllowed", "cardAllowed"],
                gps_coord=GpsCoord(37.7, 55.7),
            ),
            Outlet(
                point_id=3005,
                delivery_service_id=127,
                region=213,
                point_type=Outlet.FOR_POST_TERM,
                delivery_option=OutletDeliveryOption(shipper_id=127, day_from=1, day_to=1, price=400),
                working_days=[i for i in range(10)],
                bool_props=["prepayAllowed", "cashAllowed"],
                gps_coord=GpsCoord(37.7, 55.7),
            ),
            Outlet(
                point_id=3006,
                delivery_service_id=127,
                region=213,
                point_type=Outlet.FOR_POST_TERM,
                delivery_option=OutletDeliveryOption(shipper_id=127, day_from=1, day_to=1, price=400),
                working_days=[i for i in range(10)],
                bool_props=["prepayAllowed", "cashAllowed"],
                gps_coord=GpsCoord(37.7, 55.7),
            ),
            Outlet(
                point_id=4001,
                delivery_service_id=201,
                region=213,
                point_type=Outlet.FOR_POST,
                post_code=115200,
                delivery_option=OutletDeliveryOption(
                    shipper_id=201, day_from=2, day_to=4, price=400
                ),  # В этот ПВЗ доставится быстрее, чем в 4101 (сроки смотри в бакетах)
                working_days=[i for i in range(30)],
                bool_props=["prepayAllowed", "cashAllowed", "cardAllowed"],
                gps_coord=GpsCoord(67.7, 55.7),
            ),
            Outlet(
                point_id=4002,
                delivery_service_id=201,
                region=213,
                point_type=Outlet.FOR_POST,
                post_code=115201,
                delivery_option=OutletDeliveryOption(shipper_id=201, day_from=1, day_to=1, price=500),
                working_days=[i for i in range(30)],
                bool_props=["prepayAllowed", "cashAllowed"],
                gps_coord=GpsCoord(67.7, 55.8),
            ),
            Outlet(
                point_id=4101,
                delivery_service_id=202,
                region=213,
                point_type=Outlet.FOR_POST,
                post_code=115200,
                delivery_option=OutletDeliveryOption(shipper_id=202, day_from=2, day_to=4, price=400),
                working_days=[i for i in range(30)],
                bool_props=["prepayAllowed", "cashAllowed", "cardAllowed"],
                gps_coord=GpsCoord(67.7, 55.8),
            ),
            Outlet(
                point_id=4102,
                delivery_service_id=202,
                region=213,
                point_type=Outlet.FOR_POST,
                post_code=115201,
                delivery_option=OutletDeliveryOption(shipper_id=202, day_from=1, day_to=1, price=500),
                working_days=[i for i in range(30)],
                bool_props=[
                    "prepayAllowed",
                    "cashAllowed",
                    "cardAllowed",
                ],  # Этот ПВЗ имеет приоритет над 4002 из-за большего кол-ва опций оплаты
                gps_coord=GpsCoord(67.7, 55.8),
            ),
            Outlet(
                point_id=4201,
                delivery_service_id=203,
                region=213,
                point_type=Outlet.FOR_POST,
                post_code=115202,
                delivery_option=OutletDeliveryOption(shipper_id=203, day_from=1, day_to=1, price=500),
                working_days=[i for i in range(30)],
                bool_props=["prepayAllowed", "cashAllowed", "cardAllowed"],
                gps_coord=GpsCoord(67.7, 55.8),
            ),
            Outlet(
                point_id=4203,
                delivery_service_id=161,
                region=213,
                point_type=Outlet.FOR_POST,
                post_code=115204,
                delivery_option=OutletDeliveryOption(shipper_id=161, day_from=1, day_to=1, price=500),
                working_days=[i for i in range(30)],
                bool_props=["prepayAllowed", "cashAllowed", "cardAllowed"],
                gps_coord=GpsCoord(67.7, 55.81),
            ),
            Outlet(
                point_id=8001,
                delivery_service_id=400,
                region=2,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(shipper_id=400, day_from=1, day_to=2, price=400),
                working_days=[i for i in range(10)],
                bool_props=["prepayAllowed", "cashAllowed"],
                gps_coord=GpsCoord(37.7, 55.7),
            ),
            Outlet(
                point_id=8002,
                delivery_service_id=400,
                region=2,
                point_type=Outlet.FOR_POST,
                delivery_option=OutletDeliveryOption(shipper_id=400, day_from=1, day_to=2, price=400),
                working_days=[i for i in range(10)],
                bool_props=["prepayAllowed", "cashAllowed"],
                gps_coord=GpsCoord(37.7, 55.7),
            ),
            Outlet(
                point_id=5001,
                delivery_service_id=103,
                region=213,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(
                    shipper_id=103, day_from=1, day_to=1, order_before=2, work_in_holiday=True, price=100
                ),
                working_days=[i for i in range(10)],
                bool_props=["prepayAllowed", "cashAllowed", "cardAllowed"],
                gps_coord=GpsCoord(37.12, 55.32),
            ),
            Outlet(
                point_id=5002,
                delivery_service_id=123,
                region=213,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(
                    shipper_id=123, day_from=1, day_to=1, order_before=2, work_in_holiday=True, price=100
                ),
                working_days=[i for i in range(10)],
                bool_props=["prepayAllowed", "cashAllowed", "cardAllowed"],
                gps_coord=GpsCoord(37.12, 55.32),
            ),
            Outlet(
                point_id=5003,
                delivery_service_id=127,
                region=213,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(
                    shipper_id=127, day_from=1, day_to=1, order_before=2, work_in_holiday=True, price=100
                ),
                working_days=[i for i in range(10)],
                bool_props=["prepayAllowed", "cashAllowed", "cardAllowed"],
                gps_coord=GpsCoord(37.12, 55.32),
            ),
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=888,
                dc_bucket_id=8888,
                fesh=1,
                carriers=[157],
                regional_options=[
                    RegionalDelivery(
                        rid=213,
                        options=[DeliveryOption(price=20, day_from=10, day_to=20, shop_delivery_price=5)],
                        payment_methods=[Payment.PT_YANDEX, Payment.PT_CASH_ON_DELIVERY, Payment.PT_CARD_ON_DELIVERY],
                    )
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
            DeliveryBucket(
                bucket_id=801,
                dc_bucket_id=801,
                fesh=1,
                carriers=[157],
                tariff_id=322,
                regional_options=[
                    RegionalDelivery(
                        rid=213,
                        options=[
                            DeliveryOption(price=5, day_from=1, day_to=2, shop_delivery_price=10),
                            DeliveryOption(price=3, day_from=4, day_to=5, shop_delivery_price=10),
                        ],
                    ),
                    RegionalDelivery(
                        rid=2,
                        options=[
                            DeliveryOption(price=5, day_from=1, day_to=2, shop_delivery_price=10),
                            DeliveryOption(price=3, day_from=4, day_to=5, shop_delivery_price=10),
                        ],
                    ),
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
            DeliveryBucket(
                bucket_id=802,
                dc_bucket_id=802,
                fesh=1,
                carriers=[158],
                regional_options=[
                    RegionalDelivery(
                        rid=213,
                        options=[DeliveryOption(price=3, day_from=4, day_to=5)],
                        payment_methods=[Payment.PT_CASH_ON_DELIVERY, Payment.PT_YANDEX],
                    )
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
            DeliveryBucket(
                bucket_id=803,
                dc_bucket_id=803,
                fesh=1,
                carriers=[159],
                tariff_id=322,
                regional_options=[
                    RegionalDelivery(
                        rid=213, options=[DeliveryOption(price=20, day_from=10, day_to=20, shop_delivery_price=5)]
                    )
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
            DeliveryBucket(
                bucket_id=804,
                dc_bucket_id=804,
                fesh=1,
                carriers=[103],
                regional_options=[
                    RegionalDelivery(
                        rid=213, options=[DeliveryOption(price=20, day_from=10, day_to=20, shop_delivery_price=5)]
                    )
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
            DeliveryBucket(
                bucket_id=805,
                dc_bucket_id=805,
                fesh=1,
                carriers=[123],
                regional_options=[
                    RegionalDelivery(
                        rid=213, options=[DeliveryOption(price=20, day_from=10, day_to=20, shop_delivery_price=5)]
                    )
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
            DeliveryBucket(
                bucket_id=806,
                dc_bucket_id=806,
                fesh=1,
                carriers=[127],
                regional_options=[
                    RegionalDelivery(
                        rid=213, options=[DeliveryOption(price=20, day_from=10, day_to=20, shop_delivery_price=5)]
                    )
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
            DeliveryBucket(
                bucket_id=813,
                dc_bucket_id=813,
                fesh=1,
                carriers=[161],
                regional_options=[
                    RegionalDelivery(
                        rid=213,
                        options=[DeliveryOption(price=3, day_from=4, day_to=5)],
                        payment_methods=[Payment.PT_CASH_ON_DELIVERY, Payment.PT_YANDEX],
                    )
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
            # these bucket are equal to 801, 802, 803, but used for delviery_calc checking
            DeliveryBucket(
                bucket_id=901,
                dc_bucket_id=1,
                fesh=1,
                carriers=[257],
                regional_options=[
                    RegionalDelivery(
                        rid=213,
                        options=[
                            DeliveryOption(price=5, day_from=1, day_to=2, shop_delivery_price=10),
                            DeliveryOption(price=3, day_from=4, day_to=5, shop_delivery_price=10),
                        ],
                    )
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
            DeliveryBucket(
                bucket_id=902,
                dc_bucket_id=2,
                fesh=1,
                carriers=[258],
                tariff_id=323,
                regional_options=[
                    RegionalDelivery(rid=213, options=[DeliveryOption(price=3, day_from=4, day_to=5)]),
                    RegionalDelivery(rid=2, options=[DeliveryOption(price=3, day_from=4, day_to=5)]),
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
            DeliveryBucket(
                bucket_id=903,
                dc_bucket_id=3,
                fesh=1,
                carriers=[259],
                regional_options=[
                    RegionalDelivery(
                        rid=213, options=[DeliveryOption(price=20, day_from=10, day_to=20, shop_delivery_price=5)]
                    )
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
            DeliveryBucket(
                bucket_id=904,
                dc_bucket_id=100,
                fesh=1,
                carriers=[300],
                regional_options=[
                    RegionalDelivery(
                        rid=213, options=[DeliveryOption(price=20, day_from=4, day_to=5, shop_delivery_price=5)]
                    )
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
            DeliveryBucket(
                bucket_id=911,
                dc_bucket_id=9,
                fesh=5,
                carriers=[157],
                regional_options=[
                    RegionalDelivery(
                        rid=2, options=[DeliveryOption(price=10, day_from=10, day_to=20, shop_delivery_price=5)]
                    )
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
            DeliveryBucket(
                bucket_id=912,
                dc_bucket_id=10,
                fesh=5,
                carriers=[158],
                regional_options=[
                    RegionalDelivery(
                        rid=2, options=[DeliveryOption(price=20, day_from=10, day_to=20, shop_delivery_price=5)]
                    )
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
            DeliveryBucket(
                bucket_id=913,
                dc_bucket_id=9,
                fesh=6,
                carriers=[157],
                regional_options=[
                    RegionalDelivery(
                        rid=2, options=[DeliveryOption(price=10, day_from=10, day_to=20, shop_delivery_price=5)]
                    )
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
            DeliveryBucket(
                bucket_id=914,
                dc_bucket_id=10,
                fesh=6,
                carriers=[158],
                regional_options=[
                    RegionalDelivery(
                        rid=2, options=[DeliveryOption(price=20, day_from=10, day_to=20, shop_delivery_price=5)]
                    )
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
            DeliveryBucket(
                bucket_id=915,
                dc_bucket_id=11,
                fesh=7,
                carriers=[99],
                regional_options=[
                    RegionalDelivery(
                        rid=213, options=[DeliveryOption(price=20, day_from=10, day_to=20, shop_delivery_price=5)]
                    )
                ],
                delivery_program=DeliveryBucket.BERU_CROSSDOCK,
            ),
            DeliveryBucket(
                bucket_id=1915,
                dc_bucket_id=16,
                fesh=7,
                carriers=[199],
                regional_options=[
                    RegionalDelivery(
                        rid=213, options=[DeliveryOption(price=20, day_from=10, day_to=20, shop_delivery_price=5)]
                    )
                ],
                delivery_program=DeliveryBucket.BERU_CROSSDOCK,
            ),
            DeliveryBucket(
                bucket_id=916,
                dc_bucket_id=12,
                fesh=8,
                carriers=[165],
                regional_options=[
                    RegionalDelivery(
                        rid=213, options=[DeliveryOption(price=200, day_from=1, day_to=3, shop_delivery_price=5)]
                    )
                ],
                delivery_program=DeliveryBucket.BERU_CROSSDOCK,
            ),
            DeliveryBucket(
                bucket_id=917,
                dc_bucket_id=13,
                fesh=8,
                carriers=[165],
                regional_options=[
                    RegionalDelivery(
                        rid=213, options=[DeliveryOption(price=2500, day_from=1, day_to=3, shop_delivery_price=5)]
                    )
                ],
                delivery_program=DeliveryBucket.BERU_CROSSDOCK,
            ),
            DeliveryBucket(
                bucket_id=1001,
                dc_bucket_id=1001,
                fesh=1,
                carriers=[400],
                regional_options=[
                    RegionalDelivery(
                        rid=2, options=[DeliveryOption(price=200, day_from=1, day_to=2, shop_delivery_price=5)]
                    )
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
            DeliveryBucket(
                bucket_id=1101,
                dc_bucket_id=1101,
                fesh=12,
                regional_options=[
                    RegionalDelivery(
                        rid=213, options=[DeliveryOption(price=200, day_from=2, day_to=5, shop_delivery_price=5)]
                    )
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_WHITE_PROGRAM,
            ),
            DeliveryBucket(
                bucket_id=1102,
                dc_bucket_id=1102,
                fesh=12,
                regional_options=[
                    RegionalDelivery(
                        rid=213, options=[DeliveryOption(price=200, day_from=1, day_to=3, shop_delivery_price=5)]
                    )
                ],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            DeliveryBucket(
                bucket_id=1103,
                dc_bucket_id=11103,
                fesh=12,
                dc_15=True,
                regional_options=[RegionalDelivery(rid=213, options=[DeliveryOption(price=500, day_from=1, day_to=3)])],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            DeliveryBucket(
                bucket_id=1104,
                dc_bucket_id=None,
                fesh=12,
                dc_15=True,
                regional_options=[  # FB бакет без перенумерации
                    RegionalDelivery(rid=213, options=[DeliveryOption(price=0, day_from=2, day_to=5)])
                ],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            DeliveryBucket(
                bucket_id=1105,
                dc_bucket_id=1105,
                carriers=[401],
                regional_options=[RegionalDelivery(rid=2, options=[DeliveryOption(price=0, day_from=2, day_to=5)])],
                delivery_program=DeliveryBucket.BERU_CROSSDOCK,
            ),
            DeliveryBucket(
                bucket_id=1106,
                dc_bucket_id=1106,
                carriers=[402],
                regional_options=[RegionalDelivery(rid=2, options=[DeliveryOption(price=0, day_from=2, day_to=5)])],
                delivery_program=DeliveryBucket.BERU_CROSSDOCK,
            ),
            DeliveryBucket(
                bucket_id=1107,
                dc_bucket_id=11107,
                fesh=14,
                dc_15=True,
                regional_options=[RegionalDelivery(rid=213, options=[DeliveryOption(price=700, day_from=5, day_to=6)])],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            DeliveryBucket(
                bucket_id=1108,
                dc_bucket_id=11108,
                fesh=14,
                dc_15=True,
                regional_options=[
                    RegionalDelivery(rid=213, options=[DeliveryOption(price=550, day_from=15, day_to=16)])
                ],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            DeliveryBucket(
                bucket_id=1109,
                dc_bucket_id=11109,
                fesh=15,
                dc_15=True,
                regional_options=[
                    RegionalDelivery(rid=213, options=[DeliveryOption(price=550, day_from=15, day_to=16)])
                ],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]

        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=5001,
                dc_bucket_id=4,
                fesh=1,
                carriers=[123],
                options=[PickupOption(outlet_id=2004, day_from=1, day_to=2, price=5)],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5002,
                dc_bucket_id=5,
                fesh=1,
                carriers=[103],
                options=[PickupOption(outlet_id=2001)],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5003,
                dc_bucket_id=6,
                fesh=1,
                carriers=[123],
                options=[PickupOption(outlet_id=2004), PickupOption(outlet_id=2005)],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5004,
                fesh=1,
                carriers=[127],
                options=[PickupOption(outlet_id=3001)],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5005,
                dc_bucket_id=5005,
                fesh=1,
                carriers=[103],
                options=[PickupOption(outlet_id=5001)],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5006,
                dc_bucket_id=5006,
                fesh=1,
                carriers=[123],
                options=[PickupOption(outlet_id=5002)],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
            PickupBucket(
                bucket_id=5007,
                dc_bucket_id=5007,
                fesh=1,
                carriers=[127],
                options=[PickupOption(outlet_id=5003)],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
            # Post buckets stored in same buckets as outlets
            PickupBucket(
                bucket_id=7001,
                dc_bucket_id=7,
                fesh=1,
                carriers=[201],
                options=[
                    PickupOption(outlet_id=4001, day_from=1, day_to=2, price=7),
                    PickupOption(outlet_id=4002, day_from=1, day_to=1, price=6),
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
            PickupBucket(
                bucket_id=7002,
                dc_bucket_id=8,
                fesh=1,
                carriers=[202],
                options=[
                    PickupOption(outlet_id=4101, day_from=1, day_to=3, price=7),
                    PickupOption(outlet_id=4102, day_from=1, day_to=1, price=6),
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
            PickupBucket(
                bucket_id=9001,
                dc_bucket_id=9001,
                fesh=1,
                carriers=[400],
                options=[PickupOption(outlet_id=8001, day_from=1, day_to=2, price=6)],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
            PickupBucket(
                bucket_id=9002,
                dc_bucket_id=9002,
                fesh=1,
                carriers=[400],
                options=[PickupOption(outlet_id=8002, day_from=1, day_to=2, price=6)],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
            PickupBucket(
                bucket_id=9003,
                dc_bucket_id=14,
                fesh=1,
                carriers=[202],
                options=[PickupOption(outlet_id=4101, day_from=1, day_to=2, price=60)],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                tariff_id=11101,
            ),
            PickupBucket(
                bucket_id=9005,
                dc_bucket_id=140,
                fesh=1,
                carriers=[202],
                options=[PickupOption(outlet_id=4101, day_from=1, day_to=2, price=60)],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                tariff_id=11102,
            ),
            PickupBucket(
                bucket_id=9004,
                dc_bucket_id=15,
                fesh=1,
                carriers=[203],
                options=[PickupOption(outlet_id=4201, day_from=1, day_to=2, price=600)],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
            PickupBucket(
                bucket_id=99920,
                dc_bucket_id=17,
                fesh=1,
                carriers=[99],
                options=[PickupOption(outlet_id=2001, day_from=1, day_to=2, price=600)],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
            PickupBucket(
                bucket_id=199920,
                dc_bucket_id=18,
                fesh=1,
                carriers=[199],
                options=[PickupOption(outlet_id=2001, day_from=1, day_to=2, price=600)],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
            PickupBucket(
                bucket_id=99940,
                dc_bucket_id=19,
                fesh=1,
                carriers=[99],
                options=[PickupOption(outlet_id=4001, day_from=1, day_to=2, price=600)],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
            PickupBucket(
                bucket_id=199940,
                dc_bucket_id=20,
                fesh=1,
                carriers=[199],
                options=[PickupOption(outlet_id=4001, day_from=1, day_to=2, price=600)],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
        ]

        cls.index.mskus += [
            MarketSku(
                title="blue offer sku1",
                hyperid=1,
                sku=101010,
                waremd5='Sku1-wdDXWsIiLVm1goleg',
                blue_offers=[
                    _Offers.sku1_offer1,
                    _Offers.sku1_offer2,
                    _Offers.sku1_offer3,
                    _Offers.sku1_offer4,
                    _Offers.sku1_offer5,
                    _Offers.sku1_offer6,
                    _Offers.sku1_offer7,
                ],
                glparams=[
                    GLParam(param_id=201, value=1),
                    GLParam(param_id=202, value=1),
                    GLParam(param_id=205, value=1),
                ],
                delivery_buckets=[801, 802],
                pickup_buckets=[5002],
                post_term_delivery=True,
            ),
            MarketSku(
                title="blue offer sku2",
                hyperid=1,
                sku=202020,
                waremd5='Sku2-wdDXWsIiLVm1goleg',
                blue_offers=[_Offers.sku2_offer1, _Offers.sku2_offer2],
                glparams=[
                    GLParam(param_id=201, value=2),
                    GLParam(param_id=202, value=1),
                    GLParam(param_id=205, value=1),
                ],
                delivery_buckets=[801, 803],
                pickup_buckets=[5002],
                post_term_delivery=True,
            ),
            MarketSku(
                title="blue offer sku3",
                hyperid=1,
                sku=303030,
                waremd5='Sku3-wdDXWsIiLVm1goleg',
                blue_offers=[_Offers.sku3_offer1],
                glparams=[
                    GLParam(param_id=201, value=2),
                    GLParam(param_id=202, value=1),
                    GLParam(param_id=205, value=1),
                ],
                delivery_buckets=[801, 803],
                pickup_buckets=[5001, 5002, 5004],
                post_term_delivery=True,
            ),
            MarketSku(
                title="blue offer sku4.1",
                hyperid=1,
                sku=414141,
                waremd5='Sku4-1-DXWsIiLVm1goleg',
                blue_offers=[_Offers.sku4_offer1],
                glparams=[
                    GLParam(param_id=201, value=2),
                    GLParam(param_id=202, value=1),
                    GLParam(param_id=205, value=1),
                ],
                delivery_buckets=[911, 912],
            ),
            MarketSku(
                title="blue offer sku4.2",
                hyperid=1,
                sku=424242,
                waremd5='Sku4-2-DXWsIiLVm1goleg',
                blue_offers=[_Offers.sku4_offer2],
                glparams=[
                    GLParam(param_id=201, value=2),
                    GLParam(param_id=202, value=1),
                    GLParam(param_id=205, value=1),
                ],
                delivery_buckets=[913, 914],
            ),
            MarketSku(
                title="blue offer sku5",
                hyperid=1,
                sku=424243,
                waremd5='Sku5-0-DXWsIiLVm1goleg',
                blue_offers=[_Offers.sku5_offer1],
                delivery_buckets=[1001],
                pickup_buckets=[9001],
                post_buckets=[9002],
            ),
            MarketSku(
                title="pharmaceutical sku7.1",
                hyperid=1,
                sku=717171,
                waremd5='Sku7-1-DXWsIiLVm1goleg',
                blue_offers=[_Offers.pharma_offer],
                glparams=[
                    GLParam(param_id=201, value=2),
                    GLParam(param_id=202, value=1),
                    GLParam(param_id=205, value=1),
                ],
                delivery_buckets=[915],
            ),
            MarketSku(
                title="Ginormous fridge",
                hyperid=1,
                sku=818181,
                waremd5='Sku8-1-Refrigerator__g',
                blue_offers=[_Offers.dropship_offer],
                glparams=[
                    GLParam(param_id=201, value=2),
                    GLParam(param_id=202, value=1),
                    GLParam(param_id=205, value=1),
                ],
                delivery_buckets=[916],
            ),
            MarketSku(
                title="Ginormous heavy fridge",
                hyperid=1,
                sku=818182,
                waremd5='Sku8-11-Heavyfridge__g',
                blue_offers=[_Offers.dropship_offer_heavy],
                glparams=[
                    GLParam(param_id=201, value=2),
                    GLParam(param_id=202, value=1),
                    GLParam(param_id=205, value=1),
                ],
                delivery_buckets=[916],
            ),
            MarketSku(
                title="blue offer sku8",
                hyperid=1,
                sku=252525,
                blue_offers=[_Offers.sku8_offer1],
                glparams=[
                    GLParam(param_id=201, value=2),
                    GLParam(param_id=202, value=1),
                    GLParam(param_id=205, value=1),
                ],
                delivery_buckets=[804, 805, 806],
                pickup_buckets=[5005, 5006, 5007],
                post_term_delivery=True,
            ),
            MarketSku(
                title="blue offer sku9",
                hyperid=1,
                sku=909090,
                blue_offers=[_Offers.sku9_offer1],
                delivery_buckets=[801, 803],
                pickup_buckets=[5001],
                post_buckets=[7001],
                post_term_delivery=True,
            ),
            MarketSku(
                title="blue offer sku15",
                hyperid=1,
                sku=1010101,
                waremd5='Sku1-wdDXWsJiLVm1goleg',
                blue_offers=[_Offers.sku15_offer1, _Offers.sku15_offer2],
                glparams=[
                    GLParam(param_id=201, value=1),
                    GLParam(param_id=202, value=1),
                    GLParam(param_id=205, value=1),
                ],
                delivery_buckets=[813],
            ),
            MarketSku(
                title="some dropship offer",
                hyperid=1,
                sku=606061,
                waremd5='Sku2-wdDXWsHhlVm1goleg',
                blue_offers=[_Offers.dropship_offer1],
                delivery_buckets=[1105, 1106],
                post_term_delivery=True,
            ),
            MarketSku(
                title="offers for error tests",
                hyperid=1,
                sku=606062,
                blue_offers=[_Offers.error_401_offer, _Offers.error_404_offer, _Offers.error_501_offer],
            ),
        ]

        cls.index.shops += [
            Shop(fesh=1001, priority_region=213, cpa=Shop.CPA_REAL, client_id=1001),
        ]

        cls.index.offers += [
            Offer(
                cpa=Offer.CPA_REAL,
                hyperid=7001,
                fesh=1001,
                price=100,
                waremd5='22222222222222gggggggg',
                weight=5,
                delivery_options=[DeliveryOption(price=100, day_from=3, day_to=4, order_before=14)],
                dimensions=OfferDimensions(length=20, width=30, height=10),
                cargo_types=[256, 10],
            ),
            Offer(
                cpa=Offer.CPA_REAL,
                hyperid=7001,
                fesh=1001,
                price=200,
                waremd5='09lEaAKkQll1XTgggggggg',
                weight=1,
                dimensions=OfferDimensions(length=5, width=15, height=50),
                cargo_types=[1, 2, 3],
            ),
            Offer(
                cpa=Offer.CPA_REAL,
                hyperid=7001,
                fesh=1001,
                price=100,
                waremd5='22222222222222lightggg',
                weight=1,
                dimensions=OfferDimensions(length=20, width=30, height=10),
                cargo_types=[256, 10],
            ),
            # offers with incomplete weight and dimensions
            # no dimensions
            Offer(
                hyperid=7002,
                fesh=1001,
                price=100,
                waremd5='DuE098x_rinQLZn3KKrELw',
                weight=5,
                force_no_dimentions=True,
                delivery_options=[DeliveryOption(price=100, day_from=3, day_to=4, order_before=14)],
            ),
            # no weight
            Offer(
                hyperid=7002,
                fesh=1001,
                price=100,
                waremd5='_qQnWXU28-IUghltMZJwNw',
                dimensions=OfferDimensions(length=20, width=30, height=10),
                force_no_dimentions=True,
                delivery_options=[DeliveryOption(price=100, day_from=3, day_to=4, order_before=14)],
            ),
            # no dimensions and weight
            Offer(
                hyperid=7002,
                fesh=1001,
                price=100,
                waremd5='RPaDqEFjs1I6_lfC4Ai8jA',
                force_no_dimentions=True,
                delivery_options=[DeliveryOption(price=100, day_from=3, day_to=4, order_before=14)],
            ),
            # DSBS offers with incomplete weight and dimensions
            # no dimensions
            Offer(
                cpa=Offer.CPA_REAL,
                hyperid=7002,
                fesh=1001,
                price=100,
                waremd5='Dsbs_8x_rinQLZn3KKrELw',
                weight=5,
                force_no_dimentions=True,
                delivery_options=[DeliveryOption(price=100, day_from=3, day_to=4, order_before=14)],
            ),
            # no weight
            Offer(
                cpa=Offer.CPA_REAL,
                hyperid=7002,
                fesh=1001,
                price=100,
                waremd5='Dsbs_XU28-IUghltMZJwNw',
                force_no_dimentions=True,
                dimensions=OfferDimensions(length=20, width=30, height=10),
                delivery_options=[DeliveryOption(price=100, day_from=3, day_to=4, order_before=14)],
            ),
            # no dimensions and weight
            Offer(
                cpa=Offer.CPA_REAL,
                hyperid=7002,
                fesh=1001,
                price=100,
                force_no_dimentions=True,
                waremd5='Dsbs_EFjs1I6_lfC4Ai8jA',
                delivery_options=[DeliveryOption(price=100, day_from=3, day_to=4, order_before=14)],
            ),
            # offer with good delivery option for comparison
            Offer(
                cpa=Offer.CPA_REAL,
                hyperid=1717,
                fesh=17,
                price=100,
                waremd5='offer_with_good_do___g',
                weight=1,
                delivery_options=[DeliveryOption(price=100, day_from=3, day_to=5)],
                dimensions=OfferDimensions(length=1, width=1, height=1),
            ),
            # bad delivery options
            Offer(
                cpa=Offer.CPA_REAL,
                hyperid=1717,
                fesh=17,
                price=100,
                waremd5='offer_with_bad_do_idtg',
                weight=1,
                delivery_options=[DeliveryOption(price=100, day_from=3, day_to=255)],
                dimensions=OfferDimensions(length=1, width=1, height=1),
            ),
            Offer(
                cpa=Offer.CPA_REAL,
                hyperid=1717,
                fesh=17,
                price=100,
                waremd5='offer_with_bad_do_idfg',
                weight=1,
                delivery_options=[DeliveryOption(price=100, day_from=255, day_to=5)],
                dimensions=OfferDimensions(length=1, width=1, height=1),
            ),
        ]

        cls.index.currencies = [
            Currency('KZT', exchange_rates=[ExchangeRate(fr=Currency.RUR, rate=0.5)]),
        ]

        cls.delivery_calc.on_request_offer_buckets(weight=322, width=8, height=2, length=21).respond([8888], [], [7])
        cls.delivery_calc.on_request_offer_buckets(weight=12, width=22, height=22, length=32).respond([1], [4], [])
        cls.delivery_calc.on_request_offer_buckets(weight=5, width=11, height=22, length=32).respond(
            [2], [5, 6], [7, 8]
        )
        cls.delivery_calc.on_request_offer_buckets(weight=5, width=10, height=20, length=30).respond(
            [2], [5, 6], [7, 8]
        )
        cls.delivery_calc.on_request_offer_buckets(weight=4, width=30, height=10, length=20).respond(
            [2], [5, 6], [7, 8]
        )
        cls.delivery_calc.on_request_offer_buckets(weight=4, width=32, height=11, length=22).respond(
            [2], [5, 6], [7, 8]
        )
        cls.delivery_calc.on_request_offer_buckets(weight=12, width=22, height=22, length=38.5).respond(
            [2], [5, 6], [7, 8]
        )
        cls.delivery_calc.on_request_offer_buckets(weight=7, width=10, height=20, length=30).respond([3], [], [])

        cls.delivery_calc.on_request_offer_buckets(weight=17, width=16, height=22, length=53).respond([3], [4], [7])
        cls.delivery_calc.on_request_offer_buckets(weight=19, width=32, height=22, length=32).respond([100], [], [])
        cls.delivery_calc.on_request_offer_buckets(weight=15, width=15, height=20, length=50).respond([3], [], [])
        cls.delivery_calc.on_request_offer_buckets(weight=10, width=22, height=22, length=32).respond([3], [], [])
        cls.delivery_calc.on_request_offer_buckets(weight=16, width=48, height=43, length=53).respond([3], [], [])
        cls.delivery_calc.on_request_offer_buckets(weight=17, width=53, height=43, length=53).respond([3], [], [])
        cls.delivery_calc.on_request_offer_buckets(weight=60, width=105, height=20, length=50).respond([3], [], [])
        cls.delivery_calc.on_request_offer_buckets(weight=60, width=57, height=42, length=50).respond([3], [], [])

        cls.delivery_calc.on_request_offer_buckets(weight=4, width=22, height=22, length=22).return_code(401)
        cls.delivery_calc.on_request_offer_buckets(weight=5, width=22, height=22, length=22).return_code(501)

        cls.delivery_calc.on_request_offer_buckets(weight=404, width=404, height=404, length=404).respond(
            [404404], [], []
        )

        cls.delivery_calc.on_request_offer_buckets(weight=5, width=15, height=5, length=10).respond([9], [], [])
        cls.delivery_calc.on_request_offer_buckets(weight=5, width=15, height=5, length=10, warehouse_id=146).respond(
            [10], [], []
        )

        cls.delivery_calc.on_request_offer_buckets(weight=23, width=17, height=20, length=50).respond([3], [], [])
        cls.delivery_calc.on_request_offer_buckets(weight=23, width=20, height=23, length=50).respond([3], [], [])
        cls.delivery_calc.on_request_offer_buckets(weight=25, width=19, height=20, length=50).respond([3], [], [])
        cls.delivery_calc.on_request_offer_buckets(weight=63, width=24, height=23, length=50).respond([3], [], [])
        cls.delivery_calc.on_request_offer_buckets(weight=65, width=25, height=24, length=50).respond([3], [], [])
        cls.delivery_calc.on_request_offer_buckets(weight=75, width=25, height=24, length=50).respond([3], [], [])
        cls.delivery_calc.on_request_offer_buckets(weight=77, width=26, height=25, length=50).respond([3], [], [])
        cls.delivery_calc.on_request_offer_buckets(weight=15, width=15, height=20, length=50).respond([3], [], [])
        cls.delivery_calc.on_request_offer_buckets(weight=15, width=17, height=21, length=51).respond([3], [], [])
        cls.delivery_calc.on_request_offer_buckets(weight=15, width=16, height=22, length=53).respond([3], [], [])
        cls.delivery_calc.on_request_offer_buckets(weight=2, width=16, height=22, length=53).respond([3], [], [])
        cls.delivery_calc.on_request_offer_buckets(weight=7, width=71, height=7, length=710, warehouse_id=160).respond(
            [11], [], []
        )
        cls.delivery_calc.on_request_offer_buckets(weight=8, width=81, height=8, length=810, warehouse_id=164).respond(
            [12], [], []
        )

        cls.delivery_calc.on_request_offer_buckets(
            weight=11, width=357, height=175, length=543, warehouse_id=400
        ).respond([], [], [])
        cls.delivery_calc.on_request_offer_buckets(weight=5, width=11, height=22, length=33, warehouse_id=145).respond(
            [813], [], []
        )

        # single offer support
        cls.delivery_calc.on_request_offer_buckets(weight=7, width=71, height=7, length=711, warehouse_id=160).respond(
            [11], [17], [19]
        )
        cls.delivery_calc.on_request_offer_buckets(weight=10, width=2, height=1, length=8, warehouse_id=145).respond(
            [802], [4], [14]
        )
        cls.delivery_calc.on_request_offer_buckets(
            weight=111, width=357, height=175, length=543, warehouse_id=145
        ).respond([10001, 10002], [], [])

        cls.delivery_calc.on_request_offer_buckets(weight=20, width=8, height=2, length=21, warehouse_id=400).respond(
            [1001], [9001], [9002]
        )
        cls.delivery_calc.on_request_offer_buckets(weight=7, width=21, height=10, length=30, warehouse_id=145).respond(
            [804], [], []
        )

        cls.delivery_calc.on_request_offer_buckets(weight=7, width=22, height=10, length=30, warehouse_id=145).respond(
            [804, 805, 806], [5005, 5006, 5007], []
        )
        cls.delivery_calc.on_request_offer_buckets(weight=7, width=23, height=10, length=30, warehouse_id=145).respond(
            [804, 805, 806], [21, 22], []
        )
        cls.delivery_calc.on_request_offer_buckets(
            weight=7, width=35.5, height=10, length=20, warehouse_id=198
        ).respond([1105, 1106], [], [])

        cls.settings.loyalty_enabled = True

    @classmethod
    def prepare_white_cpa_delivery_not_priority_region(cls):

        cls.index.shops += [
            Shop(
                fesh=1002,
                priority_region=213,
                regions=[213, 512],
                cpa=Shop.CPA_REAL,
                client_id=100500,
                datafeed_id=100500,
                name='White Cpa shop',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
            ),
            Shop(
                fesh=1333,
                priority_region=213,
                regions=[213],
                cpa=Shop.CPA_REAL,
                client_id=1580,
                datafeed_id=1580,
                name='White Cpa 0',
                currency=Currency.RUR,
                tax_system=Tax.OSN,
            ),
        ]

        cls.index.delivery_modifiers += [
            DeliveryModifier(
                action=CostModificationRule(operation=ModificationOperation.ADD, parameter=55),
                modifier_id=1170,
            ),
            DeliveryModifier(
                action=CostModificationRule(operation=ModificationOperation.FIX_VALUE, parameter=100),
                modifier_id=1171,
            ),
            DeliveryModifier(
                action=CostModificationRule(operation=ModificationOperation.ADD, parameter=-50),
                modifier_id=1172,
            ),
            DeliveryModifier(
                action=TimeModificationRule(operation=ModificationOperation.MULTIPLY, parameter=2),
                modifier_id=2170,
            ),
            DeliveryModifier(
                action=TimeModificationRule(operation=ModificationOperation.FIX_VALUE, parameter=9),
                modifier_id=2171,
            ),
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=1177,
                dc_bucket_id=1177,
                fesh=1002,
                carriers=[247],
                dc_15=True,
                regional_options=[RegionalDelivery(rid=213, options=[DeliveryOption(price=150, day_from=1, day_to=2)])],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
            DeliveryBucket(
                bucket_id=1188,
                dc_bucket_id=1188,
                fesh=1002,
                carriers=[247],
                dc_15=True,
                regional_options=[
                    RegionalDelivery(
                        rid=512, options=[DeliveryOption(price=175, day_from=1, day_to=3, shop_delivery_price=5)]
                    )
                ],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
            ),
        ]

        cls.index.outlets += [
            Outlet(
                fesh=1002,
                point_id=22001,
                region=512,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(shipper_id=248, day_from=1, day_to=3, price=400),
                working_days=[i for i in range(15)],
                dimensions=Dimensions(width=1000, height=1000, length=1000),
            ),
        ]

        cls.index.new_pickup_buckets += [
            NewPickupBucket(
                bucket_id=2278,
                dc_bucket_id=2278,
                fesh=1002,
                carriers=[248],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
                region_groups=[
                    PickupRegionGroup(options=[NewPickupOption(price=200, day_from=2, day_to=5)], outlets=[22001]),
                ],
            ),
        ]

        cls.index.offers += [
            Offer(
                feedid=100500,
                cpa=Offer.CPA_REAL,
                fesh=1002,
                price=100,
                waremd5='DSBS2222222222lightggg',
                weight=27,
                dimensions=OfferDimensions(length=23, width=33, height=13),
                cargo_types=[256, 10],
                delivery_info=OfferDeliveryInfo(
                    courier_buckets=[BucketInfo(bucket_id=1177), BucketInfo(bucket_id=1188)]
                ),
                has_delivery_options=False,
            )
        ]

        cls.index.offers += [
            Offer(
                feedid=1580,
                cpa=Offer.CPA_REAL,
                fesh=1333,
                price=100,
                waremd5='DSBS_0_________1333ggg',
                weight=10,
                delivery_options=[DeliveryOption(price=100, day_from=5, day_to=8, order_before=14)],
            ),
            Offer(
                feedid=1580,
                cpa=Offer.CPA_REAL,
                fesh=1333,
                price=100,
                waremd5='DSBS_1_________1333ggg',
                weight=5,
                delivery_options=[DeliveryOption(price=100, day_from=4, day_to=7, order_before=14)],
            ),
            Offer(
                feedid=1580,
                cpa=Offer.CPA_REAL,
                fesh=1333,
                price=100,
                waremd5='DSBS_2_________1333ggg',
                weight=3,
                delivery_options=[DeliveryOption(price=100, day_from=3, day_to=6, order_before=14)],
            ),
            # Оффер без веса
            Offer(
                feedid=1580,
                cpa=Offer.CPA_REAL,
                fesh=1333,
                price=100,
                waremd5='DSBS_3_________1333ggg',
                delivery_options=[DeliveryOption(price=100, day_from=5, day_to=8, order_before=14)],
            ),
        ]

        cls.delivery_calc.on_request_shop_offers(feed_id=100500, price=100, weight=27, program_type=[0]).respond(
            [DCBucketInfo(1188, [], [], [], [], True)], [DCBucketInfo(2278, [], [], [], [], True)], []
        )
        cls.delivery_calc.on_request_shop_offers(feed_id=100500, price=200, weight=2 * 27, program_type=[0]).respond(
            [DCBucketInfo(1188, [1170], [2170], [], [], True)], [DCBucketInfo(2278, [1170], [2170], [], [], True)], []
        )
        cls.delivery_calc.on_request_shop_offers(feed_id=100500, price=300, weight=3 * 27, program_type=[0]).respond(
            [DCBucketInfo(1188, [1171], [2171], [], [], True)], [DCBucketInfo(2278, [1171], [2171], [], [], True)], []
        )
        cls.delivery_calc.on_request_shop_offers(feed_id=100500, price=400, weight=4 * 27, program_type=[0]).respond(
            [DCBucketInfo(1188, [1172], [], [], [], True)], [DCBucketInfo(2278, [1172], [], [], [], True)], []
        )
        cls.delivery_calc.on_request_shop_offers(feed_id=100500, price=3100, weight=31 * 27, program_type=[0]).respond(
            [DCBucketInfo(1188, [1172], [], [], [], True)], [DCBucketInfo(2278, [1172], [], [], [], True)], []
        )

    def test_dsbs_force_white_offer_options(self):
        """
        Проверям, что для dsbs посылок с парамметром force-white-offer-options в запросе
        или при отсутсвии веса у посылки опция доставки выбирается как максимальная по опциям офферов

        Работает без флага market_white_actual_delivery
        """

        def check_delivery_option(response, day_from):
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            "delivery": {
                                "options": [
                                    {
                                        "serviceId": "99",
                                        "dayFrom": day_from,
                                        "dayTo": day_from + 3,
                                    },
                                ]
                            }
                        }
                    ]
                },
            )

        # DSBS_0_________1333ggg: day_from=5 дней
        # DSBS_1_________1333ggg: day_from=4 дня
        # DSBS_2_________1333ggg: day_from=3 дня
        # Ответ - day_from=5 по офферу DSBS_0_________1333ggg
        flags = '&force-white-offer-options=1'
        request = (
            'place=actual_delivery&offers-list=DSBS_0_________1333ggg:1,DSBS_1_________1333ggg:1,DSBS_2_________1333ggg:1&rids=213&regset=1'
            + flags
        )
        response = self.report.request_json(request)
        check_delivery_option(response, 5)

        # Ответ - day_from=4 по офферу DSBS_1_________1333ggg
        request = (
            'place=actual_delivery&offers-list=DSBS_1_________1333ggg:1,DSBS_2_________1333ggg:1&rids=213&regset=1'
            + flags
        )
        response = self.report.request_json(request)
        check_delivery_option(response, 4)

        # Ответ - day_from=3 по офферу DSBS_2_________1333ggg
        request = 'place=actual_delivery&offers-list=DSBS_2_________1333ggg:1&rids=213&regset=1' + flags
        response = self.report.request_json(request)
        check_delivery_option(response, 3)

        # Если оффер без веса и нет параметра force-white-offer-options, то все равно работает новая логика
        # Ответ - day_from=5
        request = 'place=actual_delivery&offers-list=DSBS_3_________1333ggg:1&rids=213&regset=1'
        response = self.report.request_json(request)
        check_delivery_option(response, 5)

    def test_dsbs_delivery_not_priority_region(self):
        """
        Проверяем работу авторасчета доставки dsbs товаров с ВГХ в другой регион (не priority)
        при СД != 99
        """

        def check_delivery_option(response, price, day_from=1, day_to=3):
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            "delivery": {
                                "options": [
                                    {
                                        "serviceId": "247",
                                        "isDefault": True,
                                        "price": {
                                            "currency": "RUR",
                                            "value": str(price),
                                        },
                                        "dayFrom": day_from,
                                        "dayTo": day_to,
                                    },
                                ]
                            }
                        }
                    ]
                },
            )

        def check_pickup_option(response, price, day_from=2, day_to=5):
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            "delivery": {
                                "pickupOptions": [
                                    {
                                        "dayFrom": day_from,
                                        "dayTo": day_to,
                                        "outlet": {"id": "22001", "type": "pickup"},
                                        "partnerType": "regular",
                                        "price": {"currency": "RUR", "value": str(price)},
                                        "serviceId": 248,
                                    }
                                ],
                            }
                        }
                    ]
                },
            )

        # Обычный авторасчет доставки, бех модификаторов
        request = 'place=actual_delivery&offers-list=DSBS2222222222lightggg:1&rids=512&regset=1&force-use-delivery-calc=1&pickup-options=raw&rearr-factors=market_white_actual_delivery=1'
        response = self.report.request_json(request)
        check_delivery_option(response, 175)
        check_pickup_option(response, 200)

        # корзина на 2*100 рублей
        # Модификатор цены: id = 1170 => +55 к цене доставки
        # Модификатор срока: id = 2170 => время доставки * 2
        request = 'place=actual_delivery&offers-list=DSBS2222222222lightggg:2&rids=512&regset=1&force-use-delivery-calc=1&pickup-options=raw&rearr-factors=market_white_actual_delivery=1'
        response = self.report.request_json(request)
        check_delivery_option(response, 230, 2, 6)
        check_pickup_option(response, 255, 4, 10)

        # корзина на 3*100 рублей
        # Модификатор цены: id = 1171 => фиксируем цену на 100
        # Модификатор срока: id = 2171 => фиксируем срок на 9 дней
        request = 'place=actual_delivery&offers-list=DSBS2222222222lightggg:3&rids=512&regset=1&force-use-delivery-calc=1&pickup-options=raw&rearr-factors=market_white_actual_delivery=1'
        response = self.report.request_json(request)
        check_delivery_option(response, 100, 9, 9)
        check_pickup_option(response, 100, 9, 9)

        # корзина на 4*100 рублей
        # Модификатор цены: id = 1172 => отнимаем от цены доставки 50
        request = 'place=actual_delivery&offers-list=DSBS2222222222lightggg:4&rids=512&regset=1&force-use-delivery-calc=1&pickup-options=raw&rearr-factors=market_white_actual_delivery=1'
        response = self.report.request_json(request)
        check_delivery_option(response, 125)
        check_pickup_option(response, 150)

    def check_free_delivery(
        self,
        request,
        currency,
        offers_total_price,
        free_delivery_threshold,
        free_delivery_remainder,
        weight,
        length,
        width,
        height,
    ):
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "total": 1,  # number of deliveryGroups
                    "totalOffers": 2,
                    "offersTotalPrice": {"currency": currency, "value": offers_total_price},
                    "freeDeliveryThreshold": {"currency": currency, "value": free_delivery_threshold},
                    "freeDeliveryRemainder": {"currency": currency, "value": free_delivery_remainder},
                    "results": [
                        {
                            "entity": "deliveryGroup",
                            "weight": weight,
                            "dimensions": [length, width, height],
                            "offers": [
                                {"entity": "offer", "wareId": "09lEaAKkQll1XTgggggggg", "cargoTypes": [1, 2, 3]},
                                {"entity": "offer", "wareId": "22222222222222lightggg", "cargoTypes": [10, 256]},
                            ],
                        }
                    ],
                }
            },
            allow_different_len=False,
        )

    def check_offer_problems(
        self,
        response,
        offer_problems,
        offers,
        offers_total_price,
        free_delivery_remainder,
        weight,
        length,
        width,
        height,
    ):
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "offersTotalPrice": {"value": offers_total_price},
                    "freeDeliveryThreshold": {"value": "3000"},
                    "freeDeliveryRemainder": {"value": free_delivery_remainder},
                    "offerProblems": offer_problems,
                    "results": [
                        {
                            "entity": "deliveryGroup",
                            "weight": weight,
                            "dimensions": [length, width, height],
                            "offers": offers,
                        }
                    ],
                }
            },
            allow_different_len=False,
        )

    def test_offer_problems(self):
        offer_problems = [
            {"wareId": "RPaDqEFjs1I6_lfC4Ai8jA", "problems": ["NO_WEIGHT", "NO_DIMENSIONS"]},
            {"wareId": "_qQnWXU28-IUghltMZJwNw", "problems": ["NO_WEIGHT"]},
            {
                "wareId": "DuE098x_rinQLZn3KKrELw",
                "problems": [
                    "NO_DIMENSIONS",
                ],
            },
            {"wareId": "bad-id", "problems": ["NONEXISTENT_OFFER"]},
        ]

        """Если указан флаг &allow-incomplete-offers=1, то бесплатность доставки и общие и вес и габариты будут вычислены с учетом тех переменных, которые определены в офферах"""
        offers_3 = [
            {"entity": "offer", "wareId": "RPaDqEFjs1I6_lfC4Ai8jA"},
            {"entity": "offer", "wareId": "_qQnWXU28-IUghltMZJwNw"},
            {"entity": "offer", "wareId": "DuE098x_rinQLZn3KKrELw"},
        ]

        offers_4 = offers_3 + [{"entity": "offer", "wareId": "22222222222222gggggggg"}]

        request = 'place=actual_delivery&offers-list=DuE098x_rinQLZn3KKrELw:1,_qQnWXU28-IUghltMZJwNw:1,RPaDqEFjs1I6_lfC4Ai8jA:1,bad-id:1{}&rids=213&force-use-delivery-calc=1&rearr-factors=market_conf_loyalty_delivery_threshold_enabled=1'  # noqa

        error_no_weight = 0
        error_no_dimentions = 0
        """Проверяется выдача при различных проблемах в офферах. Наличие беспроблемного оффера 22222222222222gggggggg не меняет выдачу."""
        for good_offer in ["", ",22222222222222gggggggg:1"]:
            response = self.report.request_json(request.format(good_offer))

            error_no_weight += 2
            error_no_dimentions += 2

            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "offersTotalPrice": NoKey("offersTotalPrice"),
                        "offerProblems": offer_problems,
                        "results": [],
                    }
                },
                allow_different_len=False,
            )

        response = self.report.request_json(request.format("&allow-incomplete-offers=1"))
        self.check_offer_problems(response, offer_problems, offers_3, "300", "2700", "5", "11", "22", "32")
        error_no_weight += 2
        error_no_dimentions += 2

        response = self.report.request_json(request.format(",22222222222222gggggggg:1" + "&allow-incomplete-offers=1"))
        self.check_offer_problems(response, offer_problems, offers_4, "400", "2600", "10", "22", "22", "32")
        error_no_weight += 2
        error_no_dimentions += 2

        self.error_log.expect(code=ErrorCodes.ACD_NO_WEIGHT).times(error_no_weight)
        self.error_log.expect(code=ErrorCodes.ACD_NO_DIMENSIONS).times(error_no_dimentions)

        # проверяем, что для ДСБС офферов ошибки об отсуствии ВГХ не пишутся
        request = 'place=actual_delivery&offers-list=Dsbs_8x_rinQLZn3KKrELw:1,Dsbs_XU28-IUghltMZJwNw:1,Dsbs_EFjs1I6_lfC4Ai8jA:1&rids=213'
        self.assertFragmentNotIn(response, {"search": {"offerProblems"}})

    def test_blue_offer(self):
        """Проверяется, что синий оффер выводится, а не отфильтровывается"""
        request = "place=actual_delivery&offers-list=Sku2Price55-iLVm1Goleg:1,Sku2Price50-iLVm1Goleg:1&rids=213&force-use-delivery-calc=1"
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "entity": "deliveryGroup",
                "weight": "17",
                "dimensions": ["16", "22", "53"],
                "offers": [{"wareId": "Sku2Price55-iLVm1Goleg"}, {"wareId": "Sku2Price50-iLVm1Goleg"}],
                "delivery": {"deliveryPartnerTypes": ["YANDEX_MARKET"]},
            },
        )

    def check_blue_offers_common_courier_delivery(self, service_id_prefix):
        courier_request = "place=actual_delivery&offers-list=Sku2Price55-iLVm1Goleg:1,Sku1Price5-IiLVm1Goleg:1&rids=213&force-use-delivery-calc=1"
        """Проверяется, что выбираются общие опции курьерской доставки, а именно опции службы 157"""
        #       use_user_price, options=[(price, shipment_day, day_from, day_to), ...]
        options = ((3, 7, 11, 12), (3, 7, 12, 12), (3, 7, 16, 16), (5, 7, 8, 9))
        response = self.report.request_json(courier_request + "&debug-all-courier-options=1")
        self.assertFragmentIn(
            response,
            {
                "entity": "deliveryGroup",
                "delivery": {
                    "isAvailable": True,
                    "options": [
                        {
                            "price": {"value": str(price)},
                            "shipmentDay": shipment_day,
                            "dayFrom": day_from,
                            "dayTo": day_to,
                            "orderBefore": "20",
                            "serviceId": service_id_prefix + "57",
                        }
                        for price, shipment_day, day_from, day_to in options
                    ],
                },
            },
            allow_different_len=False,
        )

        """Проверяется, что среди общих опций доcтавки выбирается лучшая:"""
        """    по цене доставки:"""
        response = self.report.request_json(courier_request + user_price_flag(True))
        self.assertFragmentIn(
            response,
            {
                "entity": "deliveryGroup",
                "delivery": {
                    "isAvailable": True,
                    "options": [
                        {
                            "price": {"value": "3"},
                            "dayFrom": 11,
                            "dayTo": 12,
                            "orderBefore": "20",
                            "isDefault": True,
                            "serviceId": service_id_prefix + "57",
                            "shipmentDay": 7,
                        },
                        {
                            "price": {"value": "3"},
                            "dayFrom": 12,
                            "dayTo": 12,
                            "orderBefore": "20",
                            "isDefault": False,
                            "serviceId": service_id_prefix + "57",
                            "shipmentDay": 7,
                        },
                        {
                            "price": {"value": "3"},
                            "dayFrom": 16,
                            "dayTo": 16,
                            "orderBefore": "20",
                            "isDefault": False,
                            "serviceId": service_id_prefix + "57",
                            "shipmentDay": 7,
                        },
                    ],
                },
            },
            allow_different_len=False,
        )

        """    по рейтингу службы"""
        request = "place=actual_delivery&offers-list=Sku1Price5-IiLVm1Goleg:1&rids=213&force-use-delivery-calc=1"
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "entity": "deliveryGroup",
                "delivery": {
                    "isAvailable": True,
                    "options": [
                        {
                            "price": {"value": "3"},
                            "dayFrom": 11,
                            "dayTo": 12,
                            "orderBefore": "20",
                            "isDefault": True,
                            "serviceId": service_id_prefix + "58",
                            "shipmentDay": 7,
                        },
                        {
                            "price": {"value": "3"},
                            "dayFrom": 12,
                            "dayTo": 12,
                            "orderBefore": "20",
                            "isDefault": False,
                            "serviceId": service_id_prefix + "58",
                            "shipmentDay": 7,
                        },
                        {
                            "price": {"value": "3"},
                            "dayFrom": 16,
                            "dayTo": 16,
                            "orderBefore": "20",
                            "isDefault": False,
                            "serviceId": service_id_prefix + "58",
                            "shipmentDay": 7,
                        },
                    ],
                },
            },
            allow_different_len=False,
        )

        """    по цене доставки для магазина"""
        request = "place=actual_delivery&offers-list=Sku2Price55-iLVm1Goleg:1&rids=213&force-use-delivery-calc=1"
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "entity": "deliveryGroup",
                "delivery": {
                    "isAvailable": True,
                    "options": [
                        {
                            "price": {"value": "20"},
                            "dayFrom": 17,
                            "dayTo": 27,
                            "orderBefore": "20",
                            "isDefault": True,
                            "serviceId": service_id_prefix + "59",
                            "shipmentDay": 7,
                        }
                    ],
                },
            },
            allow_different_len=False,
        )

    def check_blue_offers_common_pickup_delivery_through_delivery_calc(self):
        """Проверяется, что берутся аутлеты, цена доставки, сроки доставки из pickup бакетов"""

        """    Запрос с хождением в калькулятор доставки. Выбирается аутлет службы 123."""
        """    Дополнительно проверяется, что availableServices генерируются правильно."""
        request = (
            'place=actual_delivery'
            '&offers-list=Sku2Price55-iLVm1Goleg:1,Sku1Price5-IiLVm1Goleg:1'
            '&rids=213'
            '&pickup-options=raw'
            '&force-use-delivery-calc=1'
            '&rearr-factors=market_use_post_as_pickup=0'
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "entity": "deliveryGroup",
                "weight": "12",
                "dimensions": ["22", "22", "32"],
                "delivery": {
                    "hasPickup": True,
                    "availableServices": [{"serviceId": 123, "isMarketBranded": True}],
                    "pickupOptions": [
                        {
                            "serviceId": 123,
                            "outlet": {"id": "2004"},
                            "price": {"value": "5"},  # Price is taken from bucket 5001. Original price is 400.
                            "shipmentDay": 7,
                            "dayFrom": 8,  # dayFrom and dayTo are taken from bucket 5001, because ...
                            "dayTo": 9,  # ... original dayFrom == original dayTo, however here dayFrom = dayTo - 1
                        }
                    ],
                },
            },
        )

        request = (
            'place=actual_delivery'
            '&offers-list=Sku1Price5-IiLVm1Goleg:1'
            '&rids=213'
            '&pickup-options=grouped'
            '&pickup-options-extended-grouping=1'
            '&force-use-delivery-calc=1'
            '&rearr-factors=market_use_post_as_pickup=0'
        )
        """Проверяется группировка pickup опций"""
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "entity": "deliveryGroup",
                "weight": "5",
                "dimensions": ["10", "20", "30"],
                "delivery": {"pickupOptions": pickup_option_groups_part},
            },
            allow_different_len=False,
        )

        """Проверяется, что &offer-shipping откидывает несоответствующие option группы"""
        response = self.report.request_json(request + '&offer-shipping=depot')
        self.assertFragmentIn(
            response,
            {
                "entity": "deliveryGroup",
                "weight": "5",
                "dimensions": ["10", "20", "30"],
                "delivery": {
                    "pickupOptions": [
                        {"serviceId": 103, "outletIds": [2001], "groupCount": 1},
                    ]
                },
            },
            allow_different_len=False,
        )

        """Проверяется, что при заданом &is-return=1 выводятся только аутлеты, предназначенные для возврата"""
        response = self.report.request_json(request + '&is-return=1&rearr-factors=market_return_to_wh=0')
        self.assertFragmentIn(
            response,
            {
                "entity": "deliveryGroup",
                "delivery": {
                    "pickupOptions": [
                        {"serviceId": 123, "outletIds": [2005], "groupCount": 1},
                    ]
                },
            },
            allow_different_len=False,
        )

    def test_common_delivery_through_delivery_calc(self):
        """Проверка общих опций доставки в режиме калькулятора доставки"""
        self.check_blue_offers_common_courier_delivery("2")
        self.check_blue_offers_common_pickup_delivery_through_delivery_calc()

    @skip('should be checked for DC later')
    def test_blue_offers_common_pickup_delivery(self):
        """Проверяется группировка pickup опций с учетом pickup-options-extended-grouping=1. При группировке учитываются PaymentMethods."""
        """Дополнительно проверяется вывод номеров аутлетов и PaymentMethods для групп."""
        """    Опции с одинаковыми PaymentMethods помещаются в одну группу. Не проверяются разные ShipmentDay, т.к. в пределах одной службы этот параметр вычисляется по одному и тому же календарю."""
        response = self.report.request_json(
            "pickup-options-extended-grouping=1&place=actual_delivery&offers-list=Sku1Price5-IiLVm1Goleg:1,Sku2Price50-iLVm1Goleg:1&rids=213&pickup-options=grouped&force-use-delivery-calc=1"
        )
        self.assertFragmentIn(
            response,
            {
                "entity": "deliveryGroup",
                "delivery": {
                    "pickupOptions": [
                        {
                            "serviceId": 127,
                            "outletIds": [3005, 3006],
                            "paymentMethods": ["YANDEX", "CASH_ON_DELIVERY"],
                            "groupCount": 2,
                        },
                        {
                            "serviceId": 127,
                            "outletIds": [3003, 3004],
                            "paymentMethods": ["YANDEX", "CASH_ON_DELIVERY", "CARD_ON_DELIVERY"],
                            "groupCount": 2,
                        },
                        {"serviceId": 127, "outletIds": [3001, 3002], "paymentMethods": Absent(), "groupCount": 2},
                    ]
                    + pickup_option_groups_part
                },
            },
            allow_different_len=False,
        )

    def test_free_delivery_with_common_delivery_options(self):
        """Проверяется, что бесплатность доставки учитывается в общих опциях доставки"""
        expected_options = CHEAP_OPTIONS_257
        request = "place=actual_delivery&offers-list=Sku1Pr2990-IiLVm1Goleg:1,Sku2Price55-iLVm1Goleg:1&rids=213&pickup-options=raw&force-use-delivery-calc=1&rearr-factors=market_conf_loyalty_delivery_threshold_enabled=1"  # noqa
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "offersTotalPrice": {"value": "3045"},
                "freeDeliveryThreshold": {"value": "3000"},
                "freeDeliveryRemainder": {"value": "0"},
                "results": [
                    {
                        "entity": "deliveryGroup",
                        "delivery": {
                            "options": [
                                {
                                    "dayFrom": day_from,
                                    "dayTo": day_to,
                                    "price": {"value": "0"},
                                    "discount": {
                                        "discountType": "threshold",
                                    },
                                    "serviceId": "257",
                                }
                                for (day_from, day_to, _) in expected_options
                            ],
                            "pickupOptions": [
                                {
                                    "serviceId": 123,
                                    "outlet": {"id": "2004"},
                                    "price": {"value": "0"},
                                    "discount": {
                                        "discountType": "threshold",
                                    },
                                }
                            ],
                        },
                    }
                ],
            },
            allow_different_len=False,
        )

    def _check_free_delivery_with_perks(
        self, offer, total_price, quantity, perks, discount_type, threshold=3000, delivery_cost=3, pickup_cost=100
    ):
        request = "place=actual_delivery&offers-list={}:{}&rids=213&pickup-options=raw&perks={}&force-use-delivery-calc=1&rearr-factors=market_conf_ya_plus_delivery_threshold_enabled=1&rearr-factors=market_conf_loyalty_delivery_threshold_enabled=1"  # noqa
        for delivery_flag in ('', '&no-delivery-discount=0', '&no-delivery-discount=1'):
            free_delivery = discount_type is not None and delivery_flag != '&no-delivery-discount=1'
            response = self.report.request_json(request.format(offer, quantity, perks) + delivery_flag)
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "offersTotalPrice": {"value": str(total_price)},
                        "freeDeliveryThreshold": {"value": str(threshold)},
                        "freeDeliveryRemainder": {"value": "0" if free_delivery else str(threshold - total_price)},
                        "results": [
                            {
                                "entity": "deliveryGroup",
                                "delivery": {
                                    "options": [
                                        {
                                            "price": {"value": "0" if free_delivery else str(delivery_cost)},
                                            "discount": {"discountType": discount_type} if free_delivery else Absent(),
                                        }
                                    ]
                                    if delivery_cost is not None
                                    else Absent(),
                                    "pickupOptions": [
                                        {
                                            "price": {"value": "0" if free_delivery else str(pickup_cost)},
                                            "discount": {"discountType": discount_type} if free_delivery else Absent(),
                                        }
                                    ]
                                    if pickup_cost is not None
                                    else Absent(),
                                },
                            }
                        ],
                    }
                },
            )

    def test_free_delivery_with_perks(self):
        """Проверяется, что бесплатность доставки учитывается в общих опциях доставки"""
        for perks, discount_type_2990, discount_type_2991, discount_type_2992, discount_type_2_times_1496 in [
            ('prime', None, 'prime', 'prime', None),
            ('yandex_plus', None, None, 'yandex_plus', None),
            ('prime,yandex_plus', None, 'prime', 'prime', None),
            ('yandex_plus,prime', None, 'prime', 'prime', None),
            ('beru_plus', 'beru_plus', 'beru_plus', 'beru_plus', 'beru_plus'),
            ('beru_plus,yandex_plus', 'beru_plus', 'beru_plus', 'beru_plus', 'beru_plus'),
            ('fubar', None, None, None, None),
        ]:
            self._check_free_delivery_with_perks('Sku1Pr2990-IiLVm1Goleg', 2990, 1, perks, discount_type_2990)
            self._check_free_delivery_with_perks('Sku1Pr2991-IiLVm1Goleg', 2991, 1, perks, discount_type_2991)
            self._check_free_delivery_with_perks('Sku1Pr2992-IiLVm1Goleg', 2992, 1, perks, discount_type_2992)
            self._check_free_delivery_with_perks('Sku1Pr1496-IiLVm1Goleg', 2992, 2, perks, discount_type_2_times_1496)

    def test_hours_interval(self):
        request = 'place=actual_delivery&offers-list=Sku1Price5-IiLVm1Goleg:1&force-use-delivery-calc=1&rids=213&rearr-factors=hours_interval=4'
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "entity": "deliveryGroup",
                "delivery": {
                    "options": [
                        {
                            "dayFrom": 11,
                            "dayTo": 12,
                            "isDefault": True,
                            "serviceId": "258",
                        },
                        {
                            "dayFrom": 16,
                            "dayTo": 16,
                            "timeIntervals": [{"from": "11:00", "to": "15:10"}],
                            "isDefault": False,
                            "serviceId": "258",
                        },
                    ]
                },
            },
            allow_different_len=False,
        )

        self.assertFragmentNotIn(
            response,
            {
                "timeIntervals": [{"from": "11:00", "to": "17:00"}],
            },
        )

        request = 'place=actual_delivery&offers-list=Sku1Price5-IiLVm1Goleg:1&force-use-delivery-calc=1&rids={}&rearr-factors=hours_interval=0'
        """Проверяется, что дерево регионов учитывается при вычислении timeIntervals: для службы 258 для rids=213 не заданы интервалы, но они есть для региона 225."""
        response = self.report.request_json(request.format(213))
        self.assertFragmentIn(
            response,
            {
                "entity": "deliveryGroup",
                "delivery": {
                    "options": [
                        {
                            "dayFrom": 11,
                            "dayTo": 12,
                            "timeIntervals": [{"from": "11:00", "to": "17:00"}],
                            "isDefault": True,
                            "serviceId": "258",
                        },
                        {
                            "dayFrom": 12,
                            "dayTo": 12,
                            "timeIntervals": [{"from": "11:00", "to": "17:00"}],
                            "isDefault": False,
                            "serviceId": "258",
                        },
                        {
                            "dayFrom": 16,
                            "dayTo": 16,
                            "timeIntervals": [{"from": "11:00", "to": "15:10"}],
                            "isDefault": False,
                            "serviceId": "258",
                        },
                    ]
                },
            },
            allow_different_len=False,
        )

    def test_days_to_select(self):
        """Проверяется рассчет выбора дней для доставки курьером и временные интервалы. Учитываются 5 календарных дней, начиная с самого позднего дня доставки включительно."""
        """Для диапазона дат выбирается самый длинный интервал среди интервалов всех дней."""
        """Самый длинный инервал в рамках одной опции помечается как default интервал."""
        request = 'place=actual_delivery&offers-list=Sku2Price55-iLVm1Goleg:1,Sku1Price5-IiLVm1Goleg:1&rids=213&force-use-delivery-calc=1'

        options = CHEAP_OPTIONS_257
        response = self.report.request_json(request)
        expected_options = []
        for option_idx, (day_from, day_to, intervals) in enumerate(options):
            time_intervals = []
            for interval_idx, (hour_from, min_from, hour_to, min_to) in enumerate(intervals):
                time_intervals.append(
                    {
                        "from": '{:02}:{:02}'.format(hour_from, min_from),
                        "to": '{:02}:{:02}'.format(hour_to, min_to),
                        "isDefault": interval_idx == 0,
                    }
                )
            expected_options.append(
                {
                    "dayFrom": day_from,
                    "dayTo": day_to,
                    "timeIntervals": time_intervals,
                    "isDefault": option_idx == 0,
                    "serviceId": "257",
                }
            )

        self.assertFragmentIn(
            response, {"entity": "deliveryGroup", "delivery": {"options": expected_options}}, allow_different_len=False
        )

        request = 'place=actual_delivery&offers-list=Sku1Price5-IiLVm1Goleg:1&force-use-delivery-calc=1&rids={}'
        """Проверяется, что дерево регионов учитывается при вычислении timeIntervals: для службы 258 для rids=213 не заданы интервалы, но они есть для региона 225."""
        response = self.report.request_json(request.format(213))
        self.assertFragmentIn(
            response,
            {
                "entity": "deliveryGroup",
                "delivery": {
                    "options": [
                        {
                            "dayFrom": 11,
                            "dayTo": 12,
                            "timeIntervals": [{"from": "11:00", "to": "17:00"}],
                            "isDefault": True,
                            "serviceId": "258",
                        },
                        {
                            "dayFrom": 12,
                            "dayTo": 12,
                            "timeIntervals": [{"from": "11:00", "to": "17:00"}],
                            "isDefault": False,
                            "serviceId": "258",
                        },
                        {
                            "dayFrom": 16,
                            "dayTo": 16,
                            "timeIntervals": [{"from": "11:00", "to": "15:10"}],
                            "isDefault": False,
                            "serviceId": "258",
                        },
                    ]
                },
            },
            allow_different_len=False,
        )

        """Проверяется вычисления доступных дней для региона, сдвинутого по времени на -6 часов относительно 213 региона."""
        """(Для простоты понимания можно соотнести запрос с предыдущим запросом для 213 региона)"""
        """В этом случае 16 день по 213 региону не должен выводиться, т.к. регион опаздывает на 6 часов и 5 день попадает на нерабочий день в данной локации."""
        """По этой же причине мы видим 12 и 13 день (это 11 и 12 день по 213 региону соответственно)"""
        response = self.report.request_json(request.format(2))
        self.assertFragmentIn(
            response,
            {
                "entity": "deliveryGroup",
                "delivery": {
                    "options": [
                        {
                            "dayFrom": 11,
                            "dayTo": 12,
                            "timeIntervals": [{"from": "11:00", "to": "15:10"}],
                            "isDefault": True,
                            "serviceId": "258",
                        },
                        {
                            "dayFrom": 12,
                            "dayTo": 12,
                            "timeIntervals": [{"from": "11:00", "to": "15:10"}],
                            "isDefault": False,
                            "serviceId": "258",
                        },
                        {
                            "dayFrom": 13,
                            "dayTo": 13,
                            "timeIntervals": [{"from": "09:00", "to": "14:30"}],
                            "isDefault": False,
                            "serviceId": "258",
                        },
                    ]
                },
            },
            allow_different_len=False,
        )

    def test_end_point_calendar(self):
        """Проверяется, что 11 день доставки становится 12, т.к. в 11 день в конечной локиции служба не работает (нет интервалов доставки)"""
        request = 'place=actual_delivery&offers-list=Sku2Price55-iLVm1Goleg:2,Sku1Price5-IiLVm1Goleg:1&rids=213&force-use-delivery-calc=1'
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "entity": "deliveryGroup",
                "delivery": {
                    "options": [
                        {
                            "dayFrom": 12,
                            "dayTo": 12,
                            "timeIntervals": [
                                {"from": "10:00", "to": "18:30", "isDefault": True},
                                {"from": "19:15", "to": "23:45", "isDefault": False},
                            ],
                            "isDefault": True,
                            "serviceId": "300",
                        },
                        {
                            "dayFrom": 16,
                            "dayTo": 16,
                            "timeIntervals": [{"from": "10:00", "to": "17:30", "isDefault": True}],
                            "isDefault": False,
                            "serviceId": "300",
                        },
                    ],
                },
            },
            allow_different_len=False,
        )

    BASE_POST_REQUEST = "place=actual_delivery&offers-list=Sku1Pr2990-IiLVm1Goleg:1&rids=213&force-use-delivery-calc=1&rearr-factors=market_use_post_as_pickup=0"

    def test_post_office_format(self):
        '''
        Что проверяем: формат выдачи для способа доставки почтой России.
        actual_delivery принимает на вход почтовый индекс (post-index), в который требуется доставка.
        На выдаче показываются только опции доставки для оутлетов с этим почтовым индексом
        '''
        request = T.BASE_POST_REQUEST + "&post-index=115200&pickup-options=raw"
        # Не сгруппированные опции
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "deliveryGroup",
                        "delivery": {
                            "hasPost": True,
                            "postOptions": [
                                post_option_ungrouped(
                                    outlet_id=4001,
                                    service_id=201,
                                    post_code=115200,
                                    price=7,
                                    shipment_day=7,
                                    day_from=8,
                                    day_to=9,
                                    order_before=20,
                                )
                            ],
                        },
                    }
                ]
            },
            allow_different_len=False,
        )

    def test_post_office_as_outlets(self):
        '''
        Что проверяем: при включении market_use_post_as_pickup=1 все опции из postOptions
        переезжают в pickupOptions
        '''
        request = T.BASE_POST_REQUEST + "&pickup-options=raw&rearr-factors=market_use_post_as_pickup=1"
        # Не сгруппированные опции
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "deliveryGroup",
                        "delivery": {
                            "pickupOptions": [
                                {"serviceId": 103, "outlet": {"id": "2001", "type": "pickup"}},
                                {"serviceId": 123, "outlet": {"id": "2004", "type": "pickup"}},
                                {"serviceId": 123, "outlet": {"id": "2005", "type": "pickup"}},
                                {"serviceId": 201, "outlet": {"id": "4001", "type": "post"}},
                                {"serviceId": 201, "outlet": {"id": "4002", "type": "post"}},
                                {"serviceId": 202, "outlet": {"id": "4101", "type": "post"}},
                                {"serviceId": 202, "outlet": {"id": "4102", "type": "post"}},
                            ],
                        },
                    }
                ]
            },
            allow_different_len=False,
        )

    def test_post_office_as_outlets_disabled(self):
        '''
        Что проверяем: при выключении market_use_post_as_pickup=0
        postOptions остаются на своём месте
        '''
        request = T.BASE_POST_REQUEST + "&pickup-options=raw&rearr-factors=market_use_post_as_pickup=0"
        # Не сгруппированные опции
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "deliveryGroup",
                        "delivery": {
                            "hasPost": True,
                            "pickupOptions": [
                                {"serviceId": 103, "outlet": {"id": "2001", "type": "pickup"}},
                                {"serviceId": 123, "outlet": {"id": "2004", "type": "pickup"}},
                                {"serviceId": 123, "outlet": {"id": "2005", "type": "pickup"}},
                            ],
                            "postOptions": [
                                {"serviceId": 201, "outlet": {"id": "4001", "type": "post"}},
                                {"serviceId": 201, "outlet": {"id": "4002", "type": "post"}},
                                {"serviceId": 202, "outlet": {"id": "4101", "type": "post"}},
                                {"serviceId": 202, "outlet": {"id": "4102", "type": "post"}},
                            ],
                        },
                    }
                ]
            },
            allow_different_len=False,
        )

    def test_remove_dublicates_options_from_grouped_response(self):
        '''
        Что проверяем: из сгруппированной выдачи исключаются опции с одинаковыми почтовыми индексами.
        '''
        request = (
            T.BASE_POST_REQUEST + "&pickup-options=grouped"
            "&pickup-options-extended-grouping=1"
            "&rearr-factors=market_use_post_as_pickup=1"
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "deliveryGroup",
                        "delivery": {
                            "pickupOptions": [
                                {"serviceId": 103, "outletIds": [2001]},
                                {"serviceId": 123, "outletIds": [2004, 2005]},
                                {"serviceId": 202, "outletIds": [4102], "postCodes": [115201]},
                                {"serviceId": 201, "outletIds": [4001], "postCodes": [115200]},
                            ],
                        },
                    }
                ]
            },
            allow_different_len=False,
        )

    def test_post_options_grouped_with_post_code(self):
        """
        Что проверяем:
        При запросе с указанием индекса почты берется одна лучшая опция. Остальные СД не отображаются
        """
        request = T.BASE_POST_REQUEST + "&post-index=115200&pickup-options=grouped&pickup-options-extended-grouping=1"
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "deliveryGroup",
                        "delivery": {
                            "hasPost": True,
                            "postOptions": [
                                post_option_grouped(
                                    outlet_ids=[4001],
                                    post_codes=[115200],
                                    service_id=201,
                                    price=7,
                                    shipment_day=7,
                                    day_from=8,
                                    day_to=9,
                                    order_before=20,
                                    payment_methods=["YANDEX", "CASH_ON_DELIVERY", "CARD_ON_DELIVERY"],
                                ),
                            ],
                        },
                    }
                ]
            },
            allow_different_len=False,
        )

    def test_post_payments_method_filter(self):
        """
        Что проверяем:
        при экспериментальном флаге market_blue_post_prepay_only=1 для почты остаётся только оплата онлайн
        и остальные способы доставки не затронуты
        """
        request = 'place=actual_delivery&offers-list=789_payment_gggggggggg:1&rids=213&pickup-options=grouped&pickup-options-extended-grouping=1&rearr-factors=market_use_post_as_pickup=0;{}'

        for arg, post_fst_val, post_scd_val in [
            ('', ["YANDEX", "CASH_ON_DELIVERY"], ["YANDEX", "CASH_ON_DELIVERY", "CARD_ON_DELIVERY"]),
            ('market_blue_post_prepay_only=1', ["YANDEX"], ["YANDEX"]),
        ]:
            # Нет разницы будет ли задан запрос к калькулятору доставки или нет. Ограничение по способам оплаты работает всегда
            response = self.report.request_json(request.format(arg) + user_price_flag(True) + NO_COMBINATOR_FLAG)
            self.assertFragmentIn(
                response,
                {
                    "entity": "deliveryGroup",
                    "delivery": {
                        "options": [
                            {
                                "paymentMethods": ["YANDEX", "CASH_ON_DELIVERY"],
                            }
                        ],
                        "pickupOptions": [
                            {
                                "paymentMethods": ["YANDEX", "CASH_ON_DELIVERY", "CARD_ON_DELIVERY"],
                            }
                        ],
                        "postOptions": [
                            {
                                "paymentMethods": post_fst_val,
                            },
                            {
                                "paymentMethods": post_scd_val,
                            },
                        ],
                    },
                },
                allow_different_len=False,
            )

    def check_absent_post_delivery(self, post_code, common_problems, request):
        request += "&post-index=" + post_code + "&pickup-options=grouped"
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "commonProblems": common_problems,
                "results": [{"entity": "deliveryGroup", "delivery": {"hasPost": False}}],
            },
            allow_different_len=False,
        )

    def test_absent_post_code(self):
        """Проверяется вывод ошибки NO_POST_OFFICE_FOR_POST_CODE, когда задано неизвестное репорту отделение почты"""
        self.check_absent_post_delivery("115200777", ["NO_POST_OFFICE_FOR_POST_CODE"], T.BASE_POST_REQUEST)
        self.error_log.expect(code=ErrorCodes.ACD_NO_POST_OFFICE_FOR_POST_CODE)
        """Проверяется вывод ошибки NO_DELIVERY_TO_POST_OFFICE, когда задано известное репорту отделение почты, но DC не вернул это отделение"""
        self.check_absent_post_delivery("115202", ["NO_DELIVERY_TO_POST_OFFICE"], T.BASE_POST_REQUEST)
        self.error_log.expect(code=ErrorCodes.ACD_NO_DELIVERY_TO_POST_OFFICE)

    def test_post_options_without_post_code(self):
        """
        Проверяем порядок сортировки, если почтовый индекс не указан:
        1. Сперва по цене
        2. Потом по срокам доставки
        3. По количеству способов оплаты

        На выдаче будут все почтовые опции региона
        """
        request = T.BASE_POST_REQUEST + "&pickup-options=grouped&pickup-options-extended-grouping=1"
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "deliveryGroup",
                        "delivery": {
                            "hasPost": True,
                            "postOptions": [
                                post_option_grouped(
                                    outlet_ids=[4102],
                                    post_codes=[115201],
                                    service_id=202,
                                    price=6,
                                    shipment_day=7,
                                    day_from=8,
                                    day_to=8,
                                    order_before=20,
                                    payment_methods=["YANDEX", "CASH_ON_DELIVERY", "CARD_ON_DELIVERY"],
                                ),
                                post_option_grouped(
                                    outlet_ids=[4002],
                                    post_codes=[115201],
                                    service_id=201,
                                    price=6,
                                    shipment_day=7,
                                    day_from=8,
                                    day_to=8,
                                    order_before=20,
                                    payment_methods=["YANDEX", "CASH_ON_DELIVERY"],
                                ),
                                post_option_grouped(
                                    outlet_ids=[4001],
                                    post_codes=[115200],
                                    service_id=201,
                                    price=7,
                                    shipment_day=7,
                                    day_from=8,
                                    day_to=9,
                                    order_before=20,
                                    payment_methods=["YANDEX", "CASH_ON_DELIVERY", "CARD_ON_DELIVERY"],
                                ),
                                post_option_grouped(
                                    outlet_ids=[4101],
                                    post_codes=[115200],
                                    service_id=202,
                                    price=7,
                                    shipment_day=7,
                                    day_from=8,
                                    day_to=10,
                                    order_before=20,
                                    payment_methods=["YANDEX", "CASH_ON_DELIVERY", "CARD_ON_DELIVERY"],
                                ),
                            ],
                            "postStats": {
                                "minDays": 8,
                                "maxDays": 10,
                                "minPrice": {
                                    "value": "6",
                                },
                                "maxPrice": {
                                    "value": "7",
                                },
                            },
                        },
                    }
                ]
            },
            allow_different_len=False,
            preserve_order=True,
        )

    def test_skip_post_options_calc(self):
        """
        Проверка работы флага skip-post-options-calc (MARKETOUT-23406)
        """
        request = (
            T.BASE_POST_REQUEST + "&pickup-options=grouped&pickup-options-extended-grouping=1&skip-post-options-calc={}"
        )
        response = self.report.request_json(request.format(0))
        self.assertFragmentIn(response, post_options_grouped_fragment)
        response = self.report.request_json(request.format(1))
        self.assertFragmentNotIn(response, post_options_grouped_fragment)

    def get_fake_offers(
        self, offers_total_price, free_delivery_threshold, free_delivery_remainder, delivery_days, offers, fake_offers
    ):
        first_option_day_from, first_option_day_to = delivery_days[0]
        courier_options = [
            {
                "dayFrom": first_option_day_from,
                "dayTo": first_option_day_to,
                "orderBefore": "20",
                "isDefault": True,
                "serviceId": "257",
                "shipmentDay": 7,
                "partnerType": "market_delivery",
            }
        ] + [{"dayFrom": day_from, "dayTo": day_to} for day_from, day_to in delivery_days[1:]]

        return {
            "offersTotalPrice": {"currency": "RUR", "value": offers_total_price},
            "freeDeliveryThreshold": {"currency": "RUR", "value": free_delivery_threshold},
            "freeDeliveryRemainder": {"currency": "RUR", "value": free_delivery_remainder},
            "results": [
                {
                    "entity": "deliveryGroup",
                    "delivery": {
                        "isAvailable": True,
                        "hasPickup": True,
                        "hasPost": False,
                        "availableServices": [{"serviceId": 123}],
                        "options": courier_options,
                        "pickupOptions": [
                            {
                                "serviceId": 123,
                                "price": {"currency": "RUR", "value": "5"},
                                "shipmentDay": 7,
                                "dayFrom": 8,
                                "dayTo": 9,
                                "orderBefore": 20,
                                "groupCount": 1,
                                "outletIds": [2004],
                            }
                        ],
                    },
                    "offers": offers,
                    "fakeOffers": fake_offers,
                }
            ],
        }

    def check_market_req_id_forwarded_value(self, contains_value, not_contains_value, external_service_logs):
        for i, log in enumerate(external_service_logs):
            log.expect(headers=Regex(r".*x-market-req-id={}/\d+.*".format(contains_value).lower()))
            log.expect(headers=Not(Regex(r".*x-market-req-id={}/\d+.*".format(not_contains_value).lower())))

    def check_market_req_id_forwarded(self, test_request, header_value, cgi_value, external_service_logs):
        market_req_id_header = {'X-Market-Req-ID': header_value}

        self.report.request_json(test_request, headers=market_req_id_header)
        self.check_market_req_id_forwarded_value(header_value, cgi_value, external_service_logs)

        self.report.request_json(test_request + "&market-req-id={}".format(cgi_value), headers=market_req_id_header)
        self.check_market_req_id_forwarded_value(header_value, cgi_value, external_service_logs)

        self.report.request_json(test_request + "&market-req-id={}".format(cgi_value))
        self.check_market_req_id_forwarded_value(cgi_value, header_value, external_service_logs)

    def test_market_req_id_forwarded(self):
        test_request = "place=actual_delivery&offers-list=Sku2Price55-iLVm1Goleg:1,Sku2Price50-iLVm1Goleg:1&rids=213"

        '''
        Проверяем, что репорт пробрасывает заголовок X-Market-Req-ID или
        (в отсутствие заголовка) CGI параметр market-req-id в сервис delivery_calc
        Сначала для alphanum значения, потом для numerical
        '''
        self.check_market_req_id_forwarded(test_request, "abc123", "def456", [self.delivery_calc_log])
        self.check_market_req_id_forwarded(test_request, 987654321, 12345678, [self.delivery_calc_log])

    def test_delivery_calculator_trace_log(self):
        test_request = "place=actual_delivery&offers-list=Sku2Price55-iLVm1Goleg:1,Sku2Price50-iLVm1Goleg:1"
        self.report.request_json(test_request, headers={'X-Market-Req-ID': "abc123"})
        self.external_services_trace_log.expect(
            request_id=Regex(r"abc123/\d+"),
            target_module="Delivery Calculator",
            source_module="market-report",
        ).never()

    def make_dropship_delivery(self, service_id):
        # Такие значения получаются из бакетов, соответствующих службам доставки (bucket_id = 915, 916)
        if service_id == 99:  # C&C
            day_from = 17
            day_to = 27
        else:
            assert service_id == 165  # dropship
            day_from = 8
            day_to = 10

        return {
            "entity": "deliveryGroup",
            "delivery": {
                "isAvailable": True,
                "options": [
                    {
                        "dayFrom": day_from,
                        "dayTo": day_to,
                        "serviceId": str(service_id),
                        "partnerType": DeliveryBucket.BERU_CROSSDOCK,
                    }
                ],
            },
        }

    def test_fake_offers(self):
        """Проверяется, что если оффер задан через параметры, то только эти параметры учитываются при подборе опций доставки"""
        """Все офферы заданы через параметры"""
        request = (
            'place=actual_delivery&offers-list=Sku2Price55-iLVm1Goleg:1{},Sku1Price5-IiLVm1Gole{}:1{}&rids=213&pickup-options=grouped&pickup-options-extended-grouping=1&rearr-factors=market_conf_loyalty_delivery_threshold_enabled=1'  # noqa
            + NO_COMBINATOR_FLAG
        )
        expected_options = CHEAP_OPTIONS_257
        expected_delivery_days = [option[:2] for option in expected_options]  # day_from, day_to
        response = self.report.request_json(
            request.format(
                ';w:4;d:20x30x15;p:135;wh:145;ct:1/2/3',
                'a',  # 'a' is set to check that parameters for nonexistent offer are taken from cgi
                ';w:8;d:20x20x5;p:150;wh:145;ct:3/256',
            )
        )
        self.assertFragmentIn(
            response,
            self.get_fake_offers(
                '285',
                '3000',
                '2715',
                expected_delivery_days,
                [],
                [
                    {
                        "entity": "fakeOffer",
                        "wareId": "Sku2Price55-iLVm1Goleg",
                        "weight": "4",
                        "dimensions": ["15", "20", "30"],
                        "price": {
                            "currency": "RUR",
                            "value": "135",
                        },
                        "cargoTypes": [1, 2, 3],
                    },
                    {
                        "entity": "fakeOffer",
                        "wareId": "Sku1Price5-IiLVm1Golea",
                        "weight": "8",
                        "dimensions": ["5", "20", "20"],
                        "price": {
                            "currency": "RUR",
                            "value": "150",
                        },
                        "cargoTypes": [3, 256],
                    },
                ],
            ),
        )

        """    Один оффер задан через параметры, другой будет найден в индексе"""
        response = self.report.request_json(
            request.format(';w:7;d:20x30x10;p:135;wh:145', 'g', '')  # 'g' is set to take into account offer from index
        )
        self.assertFragmentIn(
            response,
            self.get_fake_offers(
                '140',
                '3000',
                '2860',
                expected_delivery_days,
                [
                    {
                        "entity": "offer",
                        "wareId": "Sku1Price5-IiLVm1Goleg",
                        "prices": {
                            "currency": "RUR",
                            "value": "5",
                        },
                        "weight": "5",
                        "dimensions": ["20", "30", "10"],
                        "cargoTypes": [44, 55, 66],
                    }
                ],
                [
                    {
                        "entity": "fakeOffer",
                        "wareId": "Sku2Price55-iLVm1Goleg",
                        "weight": "7",
                        "dimensions": ["10", "20", "30"],
                        "price": {"currency": "RUR", "value": "135"},
                    }
                ],
            ),
        )

    def test_external_service_errors(self):
        """Проверяем логирование ошибок внешнего сервиса"""
        for offer_id in ["401", "501", "404"]:
            request = (
                'place=actual_delivery&offers-list=22222222222222gggg{}g:1&rids=213&force-use-delivery-calc=1'.format(
                    offer_id
                )
            )
            _ = self.report.request_json(request)

        self.external_services_trace_log.expect(http_code=401, http_method="POST")

        # failed requests are retried thrice
        for i in range(3):
            self.external_services_trace_log.expect(http_code=501, http_method="POST")

        self.error_log.expect(
            'request failed(HTTP/1.1 501 Not Implemented)', code=ErrorCodes.EXTREQUEST_DELIVERY_CALC_FATAL_ERROR
        )
        self.error_log.expect(
            'request failed(HTTP/1.1 401 Unauthorized)',
            code=ErrorCodes.EXTREQUEST_DELIVERY_CALC_FATAL_ERROR,
            cloud_service='test_report_lite',
        )

        # because saashub implementation does not work with local (mmap) buckets
        if self.settings.use_saashub_delivery is False:
            self.error_log.expect(
                'DeliveryCalc bucket 404404 has no appropriate local bucket',
                code=ErrorCodes.EXTREQUEST_DELIVERY_CALC_UNKNOWN_BUCKET,
            )

    @classmethod
    def prepare_single_offer(cls):
        cls.index.mskus += [
            MarketSku(
                title="single_offer_sku",
                hyperid=1,
                sku=100,
                waremd5='SingleSku-sIiLVm1goleg',
                blue_offers=[
                    BlueOffer(
                        price=50,
                        vat=Vat.NO_VAT,
                        feedid=4,
                        offerid='blue.offer.single1',
                        waremd5='SkuSingle50-iLVm1Goleg',
                        weight=11,
                        dimensions=OfferDimensions(length=543, width=175, height=357),
                    ),
                    BlueOffer(
                        price=51,
                        vat=Vat.NO_VAT,
                        feedid=4,
                        offerid='blue.offer.single2',
                        waremd5='SkuSingle51-iLVm1Goleg',
                        weight=13,
                        dimensions=OfferDimensions(length=143, width=75, height=57),
                    ),
                ],
                delivery_buckets=[801, 803],
                pickup_buckets=[5001],
                post_buckets=[7001],
                post_term_delivery=True,
            ),
        ]

        # Один офер или фэйковый офер
        cls.delivery_calc.on_request_offer_buckets(weight=11, width=543, height=175, length=357).respond([2], [5], [8])
        # Две единицы одного офера
        cls.delivery_calc.on_request_offer_buckets(weight=22, width=371, height=379, length=576).respond([2], [5], [8])
        # Два разных офера
        cls.delivery_calc.on_request_offer_buckets(weight=18, width=197, height=379, length=576).respond([2], [5], [8])
        # Две единицы одного офера для комбинатора
        cls.delivery_calc.on_request_offer_buckets(weight=22, width=371, height=379, length=576).respond([2], [5], [8])
        # Один оффер в коробочке
        cls.delivery_calc.on_request_offer_buckets(
            weight=7, width=18.1712059, height=18.1712059, length=18.1712059
        ).respond([2], [5], [8])

    @staticmethod
    def _create_sevices(courier, pickup, post):
        return {
            "results": [
                {
                    "entity": "deliveryGroup",
                    "delivery": {
                        "availableServices": [{"serviceId": pickup}],
                        "options": [
                            {
                                "serviceId": str(courier),
                                "tariffId": NotEmpty(),
                            }
                        ],
                        "pickupOptions": [
                            {
                                "serviceId": pickup,
                            }
                        ],
                        "postOptions": [
                            {
                                "serviceId": post,
                            }
                        ],
                    },
                }
            ]
        }

    @staticmethod
    def _create_courier_only_sevices(courier):
        return {
            "results": [
                {
                    "entity": "deliveryGroup",
                    "delivery": {
                        "availableServices": NoKey("availableServices"),
                        "options": [
                            {
                                "serviceId": str(courier),
                                "tariffId": NotEmpty(),
                            }
                        ],
                        "pickupOptions": NoKey("pickupOptions"),
                        "postOptions": NoKey("postOptions"),
                    },
                }
            ]
        }

    @staticmethod
    def _create_offers_services_for_single_offer_tests():
        # Бакеты, прикрепленные к оферу имеют СД, отличные от СД бакетов, возвращаемых в ответе от КД
        return T._create_sevices(157, 123, 201)

    @staticmethod
    def _create_dc_services_for_single_offer_tests():
        # Сервисы бакетов КД
        return T._create_sevices(258, 103, 202)

    @staticmethod
    def _create_dc_services_for_single_offers_tests_without_pickup():
        return T._create_courier_only_sevices(258)

    BASE_SINGLE_OFFER_REQUEST = (
        'place=actual_delivery'
        '&rids=213'
        '&pickup-options=grouped'
        '&pickup-options-extended-grouping=1'
        '&rearr-factors=rty_delivery_cart=0;market_use_post_as_pickup=0'
        '&offers-list=SkuSingle50-iLVm1Goleg:{}'
    )

    def test_do_not_request_to_dc_for_single_offer(self):
        """
        Что проверяем: для одного офера из индекса запроса к калькулятору доставки нет.
        Бакеты берутся от самого офера.
        """
        response = self.report.request_json(T.BASE_SINGLE_OFFER_REQUEST.format(1) + NO_COMBINATOR_FLAG)
        self.assertFragmentIn(response, T._create_offers_services_for_single_offer_tests())
        self.assertFragmentNotIn(response, T._create_dc_services_for_single_offer_tests())

    def test_request_to_dc_for_multi_offers(self):
        """
        Что проверяем: при запросе двух оферов одинаковых оферов запрос к калькулятору осуществляется.
        """
        request = T.BASE_SINGLE_OFFER_REQUEST + NO_COMBINATOR_FLAG
        response = self.report.request_json(request.format(2))
        self.assertFragmentNotIn(response, T._create_offers_services_for_single_offer_tests())
        # при разбиении корзины на 2 коробки и объединении нескольких опций доставок для каждой из них аутлеты игнорируются
        # see CollapseDeliveryOptions
        self.assertFragmentIn(response, T._create_dc_services_for_single_offers_tests_without_pickup())

    def test_request_to_dc_for_fake_offers(self):
        """
        Что проверяем: при запросе фэйкового офера делается запрос к КД
        """
        request = T.BASE_SINGLE_OFFER_REQUEST.format(1) + ";w:11;d:543x175x357;p:135" + NO_COMBINATOR_FLAG
        response = self.report.request_json(request)
        self.assertFragmentNotIn(response, T._create_offers_services_for_single_offer_tests())
        self.assertFragmentIn(response, T._create_dc_services_for_single_offer_tests())

    def test_request_to_dc_with_force_request(self):
        """
        Что проверяем: при запросе одиночного реального офера, но с обязательным запросом к КД
        """
        request = T.BASE_SINGLE_OFFER_REQUEST + "&force-use-delivery-calc=1"
        response = self.report.request_json(request.format(1))
        self.assertFragmentNotIn(response, T._create_offers_services_for_single_offer_tests())
        self.assertFragmentIn(response, T._create_dc_services_for_single_offer_tests())

    def test_request_to_dc_for_different_offers(self):
        """
        Что проверяем: при запросе фэйкового офера делается запрос к КД
        """
        request = T.BASE_SINGLE_OFFER_REQUEST.format(1) + ",Sku2Price55-iLVm1Goleg:1" + NO_COMBINATOR_FLAG
        response = self.report.request_json(request)
        self.assertFragmentNotIn(response, T._create_offers_services_for_single_offer_tests())
        # при разбиении корзины на 2 коробки и объединении нескольких опций доставок для каждой из них аутлеты игнорируются
        # see CollapseDeliveryOptions
        self.assertFragmentIn(response, T._create_dc_services_for_single_offers_tests_without_pickup())

    def test_combinator_service_errors_one_offer(self):
        """
        Проверяем логирование походов в комбинатор для 1 оффера (с ошибкой запроса)
        """
        request = T.BASE_SINGLE_OFFER_REQUEST.format(1) + "&rids=213&debug=1&combinator=1"
        response = self.report.request_json(request)

        self.assertFragmentIn(response, {"logicTrace": [Contains("Combinator delivery options request failed")]})

        # Запрос в комбинатор не прошел
        self.assertFragmentNotIn(response, T._create_offers_services_for_single_offer_tests())
        self.assertFragmentNotIn(response, T._create_dc_services_for_single_offer_tests())

    def test_combinator_service_errors_many_offers(self):
        """
        Проверяем логирование походов в комбинатор для 2 офферов (с ошибкой запроса)
        """
        request = T.BASE_SINGLE_OFFER_REQUEST.format(2) + "&rids=213&debug=1&combinator=1"
        response = self.report.request_json(request)
        self.assertFragmentIn(response, {"logicTrace": [Contains("Combinator delivery options request failed")]})

        # Запрос в комбинатор не прошел
        self.assertFragmentNotIn(response, T._create_offers_services_for_single_offer_tests())
        self.assertFragmentNotIn(response, T._create_dc_services_for_single_offer_tests())

    @classmethod
    def prepare_preorder(cls):
        cls.index.preorder_dates += [
            PreorderDates(
                feedId=3,
                ssku="blue.offer.15.2",
                date_from="2018-11-26T20:20:20Z",
                date_to="20181219T100509Z",
            ),
            PreorderDates(
                feedId=3,
                ssku="blue.offer.3.1",
                date_from="2018-11-27",
                date_to="2018-12-18T01:10:02Z",
                shipment_start="2018-12-01",
            ),
            PreorderDates(
                msku=101010,
                date_from="2018-11-27T04:12:34",
                date_to="20181218T010101",
            ),
        ]

    def check_with_flag(self, base_request, flags_vals):
        for flag, check in flags_vals:
            request = copy(base_request)
            if flag is not None:
                request += '&rearr-factors=market_enable_preorder_dates_shift={}'.format(flag)
            response = self.report.request_json(request)
            check(response)

    def test_preorder(self):
        DAY_FROM = 12212
        DAY_TO = 12227
        SHIPMENT_START = 12216

        self.dynamic.preorder_sku_offers = [
            DynamicSkuOffer(shop_id=3, sku="blue.offer.3.1"),
            DynamicSkuOffer(shop_id=3, sku="blue.offer.2.1"),
            DynamicSkuOffer(shop_id=3, sku="blue.offer.1.1"),  # У этого оффера msku=101010
        ]

        """Проверяется actual_delivery для предзаказных офферов:"""
        """    1) Если нет &show-preorder=1, то оффер должен отфильтроваться на базовых"""
        request = (
            'place=actual_delivery'
            '&rgb=blue'
            '&rids=213'
            '&pickup-options=grouped'
            '&force-use-delivery-calc=1'
            '&rearr-factors=market_nordstream_relevance=0;market_use_post_as_pickup=0'
            '&offers-list=Sku3Price55-iLVm1Goleg:1'
        )
        request += USE_DEPRECATED_DIRECT_SHIPPING_FLOW

        def check(response):
            return self.assertFragmentIn(
                response,
                {
                    "total": 0,
                    "offerProblems": [{"wareId": "Sku3Price55-iLVm1Goleg", "problems": ["NONEXISTENT_OFFER"]}],
                    "results": [],
                },
                allow_different_len=False,
            )

        self.check_with_flag(
            base_request=request,
            flags_vals=[
                # market_disable_preorder_dates_shift ни на что не влияет
                (None, check),
                (0, check),
                (1, check),
            ],
        )

        """    2) Срок доставки должен быть 30.11.2018 - 15.12.2018"""

        def get_service_option(service_id, day_from, day_to, has_dates=True):
            return {
                "serviceId": service_id,
                "dayFrom": day_from if has_dates else Absent(),
                "dayTo": day_to if has_dates else Absent(),
            }

        def check_shift(response):
            return self.assertFragmentIn(
                response,
                {
                    "entity": "deliveryGroup",
                    "delivery": {
                        "options": [
                            {
                                "dayFrom": SHIPMENT_START
                                + 8,  # preorder dates берутся из preorder_dates.json shipment + данные опции
                                "dayTo": SHIPMENT_START + 9,
                                "isDefault": True,
                                "serviceId": "258",
                                "timeIntervals": Absent(),
                            }  # There is single courier option. Precalculated courier options with time intervals are hidden.
                        ],
                        "pickupOptions": [
                            get_service_option(123, SHIPMENT_START + 5, SHIPMENT_START + 6),
                            get_service_option(103, SHIPMENT_START + 5, SHIPMENT_START + 6),
                        ],
                        "postOptions": [
                            get_service_option(201, SHIPMENT_START + 5, SHIPMENT_START + 5),
                            get_service_option(202, SHIPMENT_START + 5, SHIPMENT_START + 5),
                            get_service_option(201, SHIPMENT_START + 5, SHIPMENT_START + 6),
                            get_service_option(202, SHIPMENT_START + 5, SHIPMENT_START + 7),
                        ],
                        "postStats": {
                            "minDays": SHIPMENT_START + 5,
                            "maxDays": SHIPMENT_START + 7,
                        },
                    },
                },
                allow_different_len=False,
            )

        def check_old(response):
            return self.assertFragmentIn(
                response,
                {
                    "entity": "deliveryGroup",
                    "delivery": {
                        "options": [
                            {
                                "dayFrom": DAY_FROM - 3,  # preorder dates берутся из preorder_dates.json
                                "dayTo": DAY_TO + 3,
                                "isDefault": True,
                                "serviceId": "258",
                                "timeIntervals": Absent(),
                            }  # There is single courier option. Precalculated courier options with time intervals are hidden.
                        ],
                        "pickupOptions": [
                            get_service_option(123, DAY_FROM - 3, DAY_TO + 3),
                            get_service_option(103, DAY_FROM - 3, DAY_TO + 3),
                        ],
                        "postOptions": [
                            get_service_option(201, DAY_FROM - 3, DAY_TO + 3),
                            get_service_option(202, DAY_FROM - 3, DAY_TO + 3),
                            get_service_option(201, DAY_FROM - 3, DAY_TO + 3),
                            get_service_option(202, DAY_FROM - 3, DAY_TO + 3),
                        ],
                        "postStats": {
                            "minDays": DAY_FROM - 3,
                            "maxDays": DAY_TO + 3,
                        },
                    },
                },
                allow_different_len=False,
            )

        self.check_with_flag(
            base_request=request + '&show-preorder=1',
            flags_vals=[
                # market_disable_preorder_dates_shift подменяет
                (None, check_shift),
                (1, check_shift),
                (0, check_old),
            ],
        )

        """    3) То же, что и второй пункт, но теперь preorder date назначен с помощью msku"""
        msku_request = (
            'place=actual_delivery'
            '&rgb=blue'
            '&rids=213'
            '&pickup-options=grouped'
            '&force-use-delivery-calc=1'
            '&rearr-factors=market_nordstream_relevance=0;market_use_post_as_pickup=0'
            '&offers-list=Sku1Price5-IiLVm1Goleg:1'
        )
        msku_request += USE_DEPRECATED_DIRECT_SHIPPING_FLOW
        request_preorder = msku_request + '&show-preorder=1'
        response = self.report.request_json(request_preorder)
        self.assertFragmentIn(
            response,
            {
                "entity": "deliveryGroup",
                "delivery": {
                    "options": [
                        {
                            "dayFrom": DAY_FROM - 3,  # preorder dates берутся из preorder_dates.json
                            "dayTo": DAY_TO + 3,
                            "isDefault": True,
                            "serviceId": "258",
                            "timeIntervals": Absent(),
                        }  # There is single courier option. Precalculated courier options with time intervals are hidden.
                    ],
                    "pickupOptions": [
                        get_service_option(123, DAY_FROM - 3, DAY_TO + 3),
                        get_service_option(103, DAY_FROM - 3, DAY_TO + 3),
                    ],
                    "postOptions": [
                        get_service_option(201, DAY_FROM - 3, DAY_TO + 3),
                        get_service_option(202, DAY_FROM - 3, DAY_TO + 3),
                        get_service_option(201, DAY_FROM - 3, DAY_TO + 3),
                        get_service_option(202, DAY_FROM - 3, DAY_TO + 3),
                    ],
                    "postStats": {
                        "minDays": DAY_FROM - 3,
                        "maxDays": DAY_TO + 3,
                    },
                },
            },
            allow_different_len=False,
        )

        """    3) Проверяется, что выдаются настоящие даты доставки и временнЫе интервалы, если указан флаг &show-real-delivery-for-preorder=1"""
        response = self.report.request_json(request_preorder + '&show-real-delivery-for-preorder=1')
        self.assertFragmentIn(
            response,
            {
                "entity": "deliveryGroup",
                "delivery": {
                    "options": [
                        {
                            "dayFrom": 11,
                            "dayTo": 12,
                            "isDefault": True,
                            "serviceId": "258",
                            "timeIntervals": [{"from": "11:00", "to": "17:00"}],
                        },
                        {
                            "dayFrom": 12,
                            "dayTo": 12,
                            "isDefault": False,
                            "serviceId": "258",
                            "timeIntervals": [{"from": "11:00", "to": "17:00"}],
                        },
                        {
                            "dayFrom": 16,
                            "dayTo": 16,
                            "isDefault": False,
                            "serviceId": "258",
                            "timeIntervals": [{"from": "11:00", "to": "15:10"}],
                        },
                    ],
                    "pickupOptions": [
                        get_service_option(123, 8, 9),
                        get_service_option(103, 8, 9),
                    ],
                    "postOptions": [
                        get_service_option(201, 8, 8),
                        get_service_option(202, 8, 8),
                        get_service_option(201, 8, 9),
                        get_service_option(202, 8, 10),
                    ],
                    "postStats": {
                        "minDays": 8,
                        "maxDays": 10,
                    },
                },
            },
            allow_different_len=False,
        )

        """Проверяются сроки доставки для place=prime для предзаказных офферов"""
        request = 'place=prime&offerid=Sku3Price55-iLVm1Goleg&rids=213&pickup-options=grouped&show-preorder=1&rearr-factors=market_nordstream_relevance=0&rgb={}'
        request += USE_DEPRECATED_DIRECT_SHIPPING_FLOW
        """    1) На синем Маркете это 27.11.2018 - 18.12.2018. Данные берутся из конфига preorder_dates.json."""

        def check_old(response):
            return self.assertFragmentIn(
                response,
                {
                    "entity": "offer",
                    "delivery": {
                        "options": [
                            {
                                "dayFrom": DAY_FROM - 3,
                                "dayTo": DAY_TO + 3,
                                "isDefault": True,
                                "serviceId": "157",
                            }
                        ],
                        "pickupOptions": [
                            get_service_option(127, DAY_FROM - 3, DAY_TO + 3),
                            get_service_option(123, DAY_FROM - 3, DAY_TO + 3),
                            get_service_option(103, DAY_FROM - 3, DAY_TO + 3),
                        ],
                    },
                },
            )

        def check_shift(response):
            return self.assertFragmentIn(
                response,
                {
                    "entity": "offer",
                    "delivery": {
                        "options": [
                            {
                                "dayFrom": SHIPMENT_START
                                + 8,  # preorder dates берутся из preorder_dates.json shipment + данные опции
                                "dayTo": SHIPMENT_START + 9,
                                "isDefault": True,
                                "serviceId": "157",
                            }
                        ],
                        "pickupOptions": [
                            get_service_option(127, SHIPMENT_START + 5, SHIPMENT_START + 6),
                            get_service_option(123, SHIPMENT_START + 5, SHIPMENT_START + 6),
                            get_service_option(103, SHIPMENT_START + 5, SHIPMENT_START + 6),
                        ],
                    },
                },
            )

        self.check_with_flag(
            base_request=request.format('blue'),
            flags_vals=[
                # market_disable_preorder_dates_shift подменяет
                (None, check_shift),
                (1, check_shift),
                (0, check_old),
            ],
        )

        """    2) На белом маркете также 27.11.2018 - 18.12.2018"""
        self.check_with_flag(
            base_request=request.format('green_with_blue'),
            flags_vals=[
                # market_disable_preorder_dates_shift подменяет
                (None, check_shift),
                (1, check_shift),
                (0, check_old),
            ],
        )

        """Аналогичные предыдущему пункту тесты для sku, которй нет в конфиге preorder_dates.json"""
        request = 'place=prime&offerid=Sku2Price55-iLVm1Goleg&rids=213&pickup-options=grouped&show-preorder=1&rearr-factors=market_nordstream_relevance=0&rgb={}'
        request += USE_DEPRECATED_DIRECT_SHIPPING_FLOW
        """    1) На синем Маркете это 30.11.2018 - 15.12.2018. Берутся дефолтные значения."""
        response = self.report.request_json(request.format('blue'))
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "delivery": {
                    "options": [
                        {
                            "dayFrom": DAY_FROM,
                            "dayTo": DAY_TO,
                            "isDefault": True,
                            "serviceId": "157",
                        }
                    ],
                    "pickupOptions": [
                        get_service_option(103, DAY_FROM, DAY_TO),
                    ],
                },
            },
        )

        """    2) На белом маркете также 30.11.2018 - 15.12.2018"""
        response = self.report.request_json(request.format('green_with_blue'))
        self.assertFragmentIn(
            response,
            {
                "entity": "offer",
                "delivery": {
                    "options": [
                        {
                            "dayFrom": DAY_FROM,
                            "dayTo": DAY_TO,
                            "isDefault": True,
                            "serviceId": "157",
                        }
                    ],
                    "pickupOptions": [
                        get_service_option(103, DAY_FROM, DAY_TO, True),
                    ],
                },
            },
        )

        """Проверяется place=outlets: при заданном &show-preorder=1 выводятся длинные сроки доставки"""
        request = 'place=outlets&outlets=2001&fesh=1&show-preorder=1'
        request += USE_DEPRECATED_DIRECT_SHIPPING_FLOW
        request_213 = request + '&rids=213'
        response = self.report.request_xml(request_213)
        self.assertFragmentIn(
            response,
            '''
            <outlet>
                <PointId>2001</PointId>
                <SelfDeliveryRule>
                    <ShipperId>103</ShipperId>
                    <CalcMinDeliveryDays>{}</CalcMinDeliveryDays>
                    <CalcMaxDeliveryDays>{}</CalcMaxDeliveryDays>
                </SelfDeliveryRule>
            </outlet>
        '''.format(
                DAY_FROM, DAY_TO
            ),
        )

        response = self.report.request_json(request_213 + '&bsformat=2')
        self.assertFragmentIn(
            response, {"entity": "outlet", "id": "2001", "selfDeliveryRule": {"dayFrom": DAY_FROM, "dayTo": DAY_TO}}
        )

        """    Проверяется, что в регионе с другим часовым поясом количество дней ожидания доставки другое"""
        DAY_FROM_OTHER = 12213
        DAY_TO_OTHER = 12228
        response = self.report.request_json(request + '&rids=2&bsformat=2')
        self.assertFragmentIn(
            response,
            {"entity": "outlet", "id": "2001", "selfDeliveryRule": {"dayFrom": DAY_FROM_OTHER, "dayTo": DAY_TO_OTHER}},
        )

    def test_preorder_multioffer(self):
        """actual_delivery для пары предзаказных офферов"""
        # Даты, захардкоженные по умолчанию в market/library/preorder_dates/preorder_dates.h
        DEFAULT_DAY_FROM, DEFAULT_DAY_TO = 12212, 12227  # 30.11.2018 - 15.12.2018

        preorder_cart = (_Offers.sku3_offer1, _Offers.sku15_offer2)
        self.dynamic.preorder_sku_offers = [DynamicSkuOffer(shop_id=3, sku=o.offerid) for o in preorder_cart]

        request = (
            'place=actual_delivery&rgb=blue&rids=213&pickup-options=grouped&pickup-options-extended-grouping=1'
            + '&show-preorder=1&offers-list='
            + ','.join(['{}:1'.format(o.waremd5) for o in preorder_cart])
            + NO_COMBINATOR_FLAG
            + '&rearr-factors=market_use_post_as_pickup=0'
        )

        def make_service_option(service_id, day_from, day_to):
            return {"serviceId": service_id, "dayFrom": day_from, "dayTo": day_to}

        self.assertFragmentIn(
            self.report.request_json(request),
            {
                "entity": "deliveryGroup",
                "delivery": {
                    "options": [
                        {
                            "dayFrom": DEFAULT_DAY_FROM - 4,
                            "dayTo": DEFAULT_DAY_TO + 4,
                            "isDefault": True,
                            "serviceId": "258",
                            "timeIntervals": Absent(),
                        }  # There is single courier option. Precalculated courier options with time intervals are hidden.
                    ],
                    "pickupOptions": [
                        make_service_option(123, DEFAULT_DAY_FROM - 4, DEFAULT_DAY_TO + 4),
                        make_service_option(103, DEFAULT_DAY_FROM - 4, DEFAULT_DAY_TO + 4),
                    ],
                    "postOptions": [
                        make_service_option(201, DEFAULT_DAY_FROM - 4, DEFAULT_DAY_TO + 4),
                        make_service_option(202, DEFAULT_DAY_FROM - 4, DEFAULT_DAY_TO + 4),
                        make_service_option(201, DEFAULT_DAY_FROM - 4, DEFAULT_DAY_TO + 4),
                        make_service_option(202, DEFAULT_DAY_FROM - 4, DEFAULT_DAY_TO + 4),
                    ],
                    "postStats": {
                        "minDays": DEFAULT_DAY_FROM - 4,
                        "maxDays": DEFAULT_DAY_TO + 4,
                    },
                },
            },
            allow_different_len=False,
        )

        # настоящие даты доставки и временнЫе интервалы, если указан флаг &show-real-delivery-for-preorder=1
        response = self.report.request_json(request + '&show-real-delivery-for-preorder=1')
        self.assertFragmentIn(
            response,
            {
                "entity": "deliveryGroup",
                "delivery": {
                    "options": [
                        {
                            "dayFrom": day_from,
                            "dayTo": day_to,
                            "isDefault": is_default,
                            "serviceId": "258",
                            "timeIntervals": [{"from": "11:00", "to": time_to}],
                        }
                        for day_from, day_to, time_to, is_default in (
                            (11, 12, "17:00", True),
                            (12, 12, "17:00", False),
                            (16, 16, "15:10", False),
                        )
                    ],
                    "pickupOptions": [
                        make_service_option(123, 8, 9),
                        make_service_option(103, 8, 9),
                    ],
                    "postOptions": [
                        make_service_option(201, 8, 8),
                        make_service_option(202, 8, 8),
                        make_service_option(201, 8, 9),
                        make_service_option(202, 8, 10),
                    ],
                    "postStats": {
                        "minDays": 8,
                        "maxDays": 10,
                    },
                },
            },
            allow_different_len=False,
        )

    @staticmethod
    def create_big_basket_offers():
        for i in range(20):
            yield BlueOffer(
                price=5,
                vat=Vat.VAT_10,
                feedid=3,
                offerid='big_basket_{:02x}'.format(i),
                waremd5='big_busket_{:02x}_Vm1Goleg'.format(i),
                weight=5,
                dimensions=OfferDimensions(length=20, width=30, height=10),
            )

    @classmethod
    def prepare_big_basket(cls):
        cls.index.mskus += [
            MarketSku(
                title="big_basket_sku",
                hyperid=5,
                sku=7,
                waremd5='big_basket_sku_m1goleg',
                blue_offers=[offer for offer in T.create_big_basket_offers()],
                delivery_buckets=[801, 803],
                post_term_delivery=True,
            ),
        ]

        cls.delivery_calc.on_request_offer_buckets(weight=100, width=64, height=53, length=64).respond(
            [2], [5, 6], [7, 8]
        )

    def test_big_basket(self):
        '''
        Что проверяем: запрос большой корзины вернет все оферы.
        Если не задавать специально размер группы, то есть ограничение в 10 оферов, итерируемых в группе.
        Поэтому остальные оферы не находятся на мете, отсюда появляется ошибка "NONEXISTENT_OFFER"
        '''
        request = (
            'place=actual_delivery&rids=213&pickup-options=grouped&pickup-options-extended-grouping=1&offers-list={}'
        )
        response = self.report.request_json(
            request.format(','.join([offer.waremd5 + ":1" for offer in T.create_big_basket_offers()]))
        )
        self.assertFragmentNotIn(response, ["NONEXISTENT_OFFER"])

    def test_bucket_numbers_from_delivery_calc(self):
        """Проверяется, что репорт выводит номера бакетов от калькулятора доставки, когда есть &debug=1"""
        request = 'place=actual_delivery&rids=213&pickup-options=grouped&pickup-options-extended-grouping=1&offers-list=SkuSingle50-iLVm1Goleg:1&force-use-delivery-calc=1&debug=1'
        response = self.report.request_json(request)
        self.assertFragmentIn(response, {"logicTrace": [Contains('courier_buckets: 902')]})
        self.assertFragmentIn(response, {"logicTrace": [Contains('pickup_buckets: 5002')]})
        self.assertFragmentIn(response, {"logicTrace": [Contains('post_buckets: 7002')]})

    def test_fair_common_dimensions_algo(self):
        """Проверяется алгоритм расчета веса и габаритов, основанный на 'наматывании' товаров на наибольшее общее основание"""
        request_base_single_offer = (
            'place=actual_delivery&rids=213&fair-common-dimensions-algo=1'
            + NO_COMBINATOR_FLAG
            + '&offers-list=Sku1Price5-IiLVm1Goleg:1'
        )
        request_base = request_base_single_offer + ',Sku1Price50-iLVm1Goleg:1'
        request = request_base + ',Sku1Pr1122-IiLVm1Goleg:{}'
        request_no_expand_dimensions = request + '&expand-dimensions-for-item=0&expand-dimensions-for-box=0'
        """    На общее основание 50x20x15 целиком умещается 4 товара размером 21x8x2. В этом случае общая высота 15 увеличивается на 2"""
        response = self.report.request_json(request_no_expand_dimensions.format(4))
        self.assertFragmentIn(response, {"weight": "23", "dimensions": ["17", "20", "50"]})
        """    В случае 5 товаров 21x8x2 высота увеличивается на 4"""
        response = self.report.request_json(request_no_expand_dimensions.format(5))
        self.assertFragmentIn(response, {"weight": "25", "dimensions": ["19", "20", "50"]})

        """    24 товара 21x8x2 -- это предельное значение, когда на одном уровне помещается 4 товара."""
        response = self.report.request_json(request_no_expand_dimensions.format(24))
        self.assertFragmentIn(response, {"weight": "63", "dimensions": ["24", "23", "50"]})

        """    После этого значения построение нового уровня происходит только после добавления 6 таких товаров"""
        for num, weight in [(25, '65'), (30, '75')]:
            response = self.report.request_json(request_no_expand_dimensions.format(num))
            self.assertFragmentIn(response, {"weight": weight, "dimensions": ["25", "24", "50"]})

        """    Добавив седьмой товар, переходим на следующий уровень в заполнении"""
        response = self.report.request_json(request_no_expand_dimensions.format(31))
        self.assertFragmentIn(response, {"weight": "77", "dimensions": ["26", "25", "50"]})

        """Проверяется расширение каждого измерения коробки на 6%"""
        for add_req, dimensions in [
            ('&expand-dimensions-for-box=0', ["15", "20", "50"]),  # честные габариты
            ('', ["16", "22", "53"]),
        ]:  # с расширением
            response = self.report.request_json(request_base + add_req)
            self.assertFragmentIn(response, {"weight": "15", "dimensions": dimensions})

        """Проверяется, что для одного оффера не происходит никакого расширения"""
        response = self.report.request_json(request_base_single_offer)
        self.assertFragmentIn(response, {"weight": "5", "dimensions": ["10", "20", "30"]})

    def test_fair_packing_algo_region(self):
        """Проверяется алгоритм вычисления общей коробки"""
        response = self.report.request_json(
            'place=actual_delivery&expand-dimensions-for-item=0&expand-dimensions-for-box=0&rids=213&offers-list=Sku1Price5-IiLVm1Goleg:1,Sku1Price50-iLVm1Goleg:1,Sku1Pr1122-IiLVm1Goleg:4'
            + NO_COMBINATOR_FLAG
        )
        self.assertFragmentIn(response, {"weight": "23", "dimensions": ["17", "20", "50"]})

    @classmethod
    def prepare_promo_payment(cls):
        cls.index.mskus += [
            MarketSku(
                sku=789,
                title="sku with promo",
                hyperid=789,
                blue_offers=[
                    BlueOffer(
                        price=10,
                        vat=Vat.VAT_10,
                        waremd5='789_payment_gggggggggg',
                        feedid=3,
                        offerid='blue.offer.789.1',
                        weight=5,
                        dimensions=OfferDimensions(length=7, width=8, height=9),
                        blue_promo_key='Payment-Promo',
                    )
                ],
                delivery_buckets=[801, 802],
                pickup_buckets=[5002],
                post_buckets=[7001],
            ),
        ]

        cls.delivery_calc.on_request_offer_buckets(weight=5, height=8, width=7, length=9).respond([801, 802], [5], [7])

    @classmethod
    def prepare_restrictions(cls):
        cls.index.mskus += [
            MarketSku(
                sku=987,
                title="sku with restriction",
                hid=101,
                blue_offers=[
                    BlueOffer(
                        price=10,
                        vat=Vat.VAT_10,
                        waremd5='987_restriction_gggggg',
                        feedid=3,
                        offerid='blue.offer.3.987',
                        weight=50,
                        dimensions=OfferDimensions(length=70, width=80, height=90),
                    )
                ],
                delivery_buckets=[801, 802],
                pickup_buckets=[5002],
                post_buckets=[7001],
            ),
        ]

        # Умышленно отвечаем из калькулятора доставки отсутствием бакетов, чтобы убедиться,
        # что для данного офера нет никаких факторов, включающих опции доставки
        cls.delivery_calc.on_request_offer_buckets(weight=50, height=90, width=80, length=70).respond([], [], [])

        cls.index.category_restrictions += [
            CategoryRestriction(
                name='medicine',
                hids=[101],
                regional_restrictions=[
                    RegionalRestriction(
                        show_offers=True,
                        delivery=False,
                        rids=[213],
                        disclaimers=[
                            Disclaimer(
                                name='medicine1',
                                text='Лекарство. Не доставляется. Продается только в аптеках.',
                                short_text='Лекарство. Продается в аптеках',
                            ),
                        ],
                    ),
                ],
            ),
        ]
        cls.index.hypertree += [
            HyperCategory(hid=101, name='Лекарства'),
        ]

    def test_restrictions(self):
        # Проверяем, что с калькулятором доставки опций не будет
        request = (
            'place=actual_delivery'
            '&offers-list=987_restriction_gggggg:1'
            '&rids=213'
            '&pickup-options=grouped'
            '&pickup-options-extended-grouping=1'
            '&rgb=blue'
        )

        force_use_delivery_calc = '&force-use-delivery-calc=1'
        response = self.report.request_json(request + force_use_delivery_calc)
        self.assertFragmentIn(
            response,
            {
                "entity": "deliveryGroup",
                "offers": [
                    {
                        "wareId": "987_restriction_gggggg",
                    }
                ],
                "delivery": {
                    "options": Absent(),
                    "pickupOptions": Absent(),
                    "postOptions": Absent(),
                },
            },
        )

        # Проверяем, что несмотря на категорийное ограничение, все опции доставки отдаются (включая курьерку)
        # Проверка результатов предыдущего запроса гарантирует, что опции не появились по какой-то другой причине
        # Для одного офера в запросе без опции &force-use-delivery-calc=1 поход в калькулятор доставки не производится
        rearr_factors = '&rearr-factors=rty_delivery_cart=0;market_use_post_as_pickup=0'
        response = self.report.request_json(request + NO_COMBINATOR_FLAG + rearr_factors)
        self.assertFragmentIn(
            response,
            {
                "entity": "deliveryGroup",
                "offers": [
                    {
                        "wareId": "987_restriction_gggggg",
                    }
                ],
                "delivery": {
                    "options": [],
                    "pickupOptions": [],
                    "postOptions": [],
                },
            },
        )

    @classmethod
    def prepare_generation_from_feed(cls):
        cls.index.delivery_calc_feed_info += [
            DeliveryCalcFeedInfo(feed_id=3, generation_id=3, warehouse_id=145),
            DeliveryCalcFeedInfo(feed_id=4, generation_id=4, warehouse_id=145),
            DeliveryCalcFeedInfo(feed_id=5, generation_id=5, warehouse_id=145),
            DeliveryCalcFeedInfo(feed_id=6, generation_id=6, warehouse_id=146),
            DeliveryCalcFeedInfo(feed_id=55, generation_id=7),
            DeliveryCalcFeedInfo(
                feed_id=160,
                generation_id=160,
                warehouse_id=160,
                pickupBuckets=[99920, 99940, 199920, 199940],
                courierBuckets=[915, 1915],
            ),
            DeliveryCalcFeedInfo(feed_id=164, generation_id=160, warehouse_id=164),
        ]

        cls.delivery_calc.on_request_offer_buckets(weight=7, width=71, height=7, length=710, warehouse_id=161).respond(
            [11], [], []
        )

    def test_delivery_calculator_generation_for_warehouse(self):
        """
        Проверяем вычисление поколения калькулятора из фида, а не версии МарДо
        """
        force_use_delivery_calc = '&rearr-factors=market_nordstream_relevance=0&force-use-delivery-calc=1'

        # По умолчанию запрос строится по складу
        test_request = "place=actual_delivery&offers-list=Sku2Price55-iLVm1Goleg:1,Sku2Price50-iLVm1Goleg:1"
        self.report.request_json(test_request, headers={'X-Market-Req-ID': "abc123"})
        self.external_services_trace_log.expect(
            request_id=Regex(r"abc123/\d+"),
            kv_in_warehouse_id=145,
            kv_in_generation_id=5,  # максимальное поколение для склада
            http_method="POST",
        )

        # Если для склада офера не указано поколение, то берется поколение, указанное для фида
        test_request = "place=actual_delivery&rgb=blue&offers-list=Sku9Price50-iLVm1Goleg:1"
        test_request += force_use_delivery_calc
        self.report.request_json(test_request, headers={'X-Market-Req-ID': "abc124"})
        self.external_services_trace_log.expect(
            request_id=Regex(r"abc124/\d+"),
            kv_in_generation_id=7,
            http_method="POST",  # максимальное поколение для фида
        )

        # Если запрос фэйковых оферов без указания фида, то берется поколение склада
        fake_request = (
            "place=actual_delivery&rgb=blue&offers-list=FakeOffer_g:1;w:7;d:710x71x7;p:710;ff:0;wh:{}&rids=213"
        )
        fake_request += force_use_delivery_calc
        self.report.request_json(fake_request.format(160), headers={'X-Market-Req-ID': "abc125"})
        self.external_services_trace_log.expect(
            request_id=Regex(r"abc125/\d+"), kv_in_generation_id=160, http_method="POST"
        )

        # Если запрошенного склада нет, то берется поколение МарДо
        self.report.request_json(fake_request.format(161), headers={'X-Market-Req-ID': "abc126"})
        self.external_services_trace_log.expect(
            request_id=Regex(r"abc126/\d+"), kv_in_generation_id=1, http_method="POST"
        )

        self.error_log.expect(code=ErrorCodes.FAILED_TO_FIND_DELICALC_GENERATION_NUMBER)
        self.error_log.expect(code=ErrorCodes.FAILED_TO_FIND_DELICALC_GENERATION_NUMBER_BY_WAREHOUSE_ID)

        # Теперь при запросах в Report с id склада, которого нет в LMS, происходит запись в error.log
        self.error_log.expect(code=ErrorCodes.CGI_UNKNOWN_WAREHOUSE_ID)

    def test_unknown_warehouse_id(self):
        """
        Проверяем, что при незвестном id склада, переданном в параметре '&offers-list:wh', или FF складе в '&offers-list:ffWh'
        происходит запись в error.log с данными id
        """
        test_offer = _Offers.sku1_offer1
        market_colors = ['white', 'blue']
        color_count = len(market_colors)
        request_count = color_count
        warehouses = [145, 666]  # реальный склад, определенный в LMS  # несуществующий склад

        request = (
            'place=actual_delivery'
            '&rgb={color}'
            '&rids=213'
            '&feedid={feed}'
            '&offers-list={waremd5}:1;w:{weight};d:{length}x{width}x{height};wh:{warehouse};p:{price}'
            '&rearr-factors=market_nordstream_relevance=0'
        )

        for color in market_colors:
            for wh in warehouses:
                self.report.request_json(
                    request.format(
                        color=color,
                        feed=test_offer.feedid,
                        waremd5=test_offer.waremd5,
                        weight=test_offer.weight,
                        length=test_offer.dimensions.length,
                        width=test_offer.dimensions.width,
                        height=test_offer.dimensions.height,
                        warehouse=wh,
                        price=test_offer.price,
                    )
                )

        # Не известный id склада (только для 666-ого склада)
        self.error_log.expect(code=ErrorCodes.CGI_UNKNOWN_WAREHOUSE_ID).times(request_count)

        # Не удалось разрешить поколение КД ни по складу, ни по фиду
        self.error_log.expect(code=ErrorCodes.FAILED_TO_FIND_DELICALC_GENERATION_NUMBER).times(2 * request_count)
        self.error_log.expect(code=ErrorCodes.FAILED_TO_FIND_DELICALC_GENERATION_NUMBER_BY_WAREHOUSE_ID)

        # После 3 неудачных обращений к КД в error.log пишется фатальная ошибка
        self.error_log.expect(code=ErrorCodes.EXTREQUEST_DELIVERY_CALC_FATAL_ERROR).times(request_count)

    def test_negative_shipment_day(self):
        """Проверяется, что при отрицательном значении &inlet-shipment-day правильно рассчитывается доставка: при этом dayFrom и dayTo могут становиться отрицательными"""
        for preferableShipmentDay, shipmentDay, dayFrom, dayTo in [
            (None, 0, 1, 2),
            (-1, -1, 0, 1),
            (-2, -2, -1, 0),
            (-3, -3, -2, -1),
        ]:
            inletShipmentDay = str(preferableShipmentDay) if preferableShipmentDay is not None else ""
            response = self.report.request_json(
                "place=actual_delivery&offers-list=Sku5Pr1121-IiLVm1Goleg:1&rids=2&pickup-options=grouped&inlet-shipment-day="
                + inletShipmentDay
                + NO_COMBINATOR_FLAG
                + '&rearr-factors=market_use_post_as_pickup=0'
            )
            self.assertFragmentIn(
                response,
                {
                    "entity": "deliveryGroup",
                    "delivery": {
                        "options": [
                            {
                                "shipmentDay": shipmentDay,
                                "dayFrom": dayFrom,
                                "dayTo": dayTo,
                                "serviceId": "400",
                            }
                        ],
                        "pickupOptions": [
                            {
                                "shipmentDay": shipmentDay,
                                "dayFrom": dayFrom,
                                "dayTo": dayTo,
                                "serviceId": 400,
                            }
                        ],
                        "postOptions": [
                            {
                                "shipmentDay": shipmentDay,
                                "dayFrom": dayFrom,
                                "dayTo": dayTo,
                                "serviceId": 400,
                            }
                        ],
                    },
                },
            )
            self.error_log.expect(code=ErrorCodes.FAILED_TO_FIND_DELICALC_GENERATION_NUMBER_BY_WAREHOUSE_ID)

    def test_delivery_service_filter(self):
        """Проверяется, что при заданном &deliveryServiceId берутся опции только определенной СД"""
        request = (
            'place=actual_delivery&offers-list=Sku8Price55-iLVm1Goleg:1&rids=213&pickup-options=grouped'
            + NO_COMBINATOR_FLAG
        )
        """  Сначала проверяется, что без этого флага выводятся опции нескольких СД"""
        response = self.report.request_json(request + '&debug-all-courier-options=1')
        self.assertFragmentIn(
            response,
            {
                "entity": "deliveryGroup",
                "delivery": {
                    "options": [
                        {"serviceId": "103"},
                        {"serviceId": "123"},
                        {"serviceId": "127"},
                    ],
                    "pickupOptions": [
                        {"serviceId": 103},
                        {"serviceId": 123},
                        {"serviceId": 127},
                    ],
                },
            },
            allow_different_len=False,
        )

        """  Затем проверяется, что с этим флагом выбирается только опции 123 СД"""
        request_service = request + '&deliveryServiceId=123'
        response = self.report.request_json(request_service)
        self.assertFragmentIn(
            response,
            {
                "entity": "deliveryGroup",
                "delivery": {
                    "options": [
                        {"serviceId": "123"},
                    ],
                    "pickupOptions": [
                        {"serviceId": 123},
                    ],
                },
            },
            allow_different_len=False,
        )

    def test_weight_threshold(self):
        """Проверяется, что для офферов тяжелее 20 кг (значение по умолчанию) стоимость доставки при превышении threshold не обнуляется"""

        response = self.report.request_json(
            'place=actual_delivery&offers-list=Sku5Pr1121-IiLVm1Goleg:1&rids=2&pickup-options=grouped'
            + NO_COMBINATOR_FLAG
            + '&rearr-factors=market_conf_loyalty_delivery_threshold_enabled=1;market_use_post_as_pickup=0'
        )

        self.error_log.expect(code=ErrorCodes.FAILED_TO_FIND_DELICALC_GENERATION_NUMBER_BY_WAREHOUSE_ID)
        self.assertFragmentIn(
            response,
            {
                "offersTotalPrice": {"value": "11223"},
                "results": [
                    {
                        "entity": "deliveryGroup",
                        "delivery": {
                            "options": [{"price": {"value": "200"}}],
                            "pickupOptions": [{"price": {"value": "6"}}],
                            "postOptions": [{"price": {"value": "6"}}],
                            "postStats": {"minPrice": {"value": "6"}, "maxPrice": {"value": "6"}},
                        },
                    }
                ],
            },
        )

    @classmethod
    def prepare_test_priority(cls):
        def create_dynamic_warehouse_and_delivery_service_relation(service_id, priorities_and_regions):
            return DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=145,
                delivery_service_id=service_id,
                operation_time=0,
                date_switch_time_infos=[DateSwitchTimeAndRegionInfo(date_switch_hour=20, region_to=225)],
                priorities_and_regions=priorities_and_regions,
            )

        cls.dynamic.lms += [
            DynamicDeliveryServiceInfo(id=501, name='c_501'),
            DynamicDeliveryServiceInfo(id=502, name='c_502'),
            create_dynamic_warehouse_and_delivery_service_relation(501, []),
            create_dynamic_warehouse_and_delivery_service_relation(502, [DeliveryServicePriorityAndRegionInfo(2, 50)]),
        ]

        def create_delivery_bucket(bucket_id, carrier_id, price):
            return DeliveryBucket(
                bucket_id=bucket_id,
                dc_bucket_id=bucket_id,
                fesh=1,
                carriers=[carrier_id],
                regional_options=[
                    RegionalDelivery(
                        rid=50, options=[DeliveryOption(price=price, day_from=1, day_to=2, shop_delivery_price=10)]
                    )
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            )

        cls.index.delivery_buckets += [create_delivery_bucket(10001, 501, 4), create_delivery_bucket(10002, 502, 5)]

        def create_blue_offer(index, buckets):
            return BlueOffer(
                price=50,
                vat=Vat.NO_VAT,
                feedid=4,
                offerid='blue.offer.prior_{}'.format(index),
                waremd5='Skuprior5-{}-iLVm1Goleg'.format(index),
                weight=111,
                dimensions=OfferDimensions(length=543, width=175, height=357),
                delivery_buckets=buckets,
            )

        cls.index.mskus += [
            MarketSku(
                title="prior__offer_sku",
                hyperid=112233,
                sku=223344,
                waremd5='prior_Sku-sIiLVm1goleg',
                blue_offers=[create_blue_offer("1", [10001, 10002]), create_blue_offer("2", [10002, 10001])],
            )
        ]

    def test_priority(self):
        """Проверяется, что независимо от порядка бакетов (10001, 10002, или наоборот) выбирается опция, у которой задан приоритет в регионе (из бакета 10002)."""
        for index in ["1", "2"]:
            response = self.report.request_json(
                'place=actual_delivery&offers-list=Skuprior5-{}-iLVm1Goleg:1&rids=50{}'.format(
                    index, NO_COMBINATOR_FLAG
                )
            )
            self.assertFragmentIn(
                response, {"entity": "deliveryGroup", "delivery": {"options": [{"serviceId": "502"}]}}
            )

    @classmethod
    def prepare_tariff_warning(cls):
        cls.index.mskus += [
            MarketSku(
                title="blue offer sku10",
                hyperid=1,
                sku=10,
                blue_offers=[_Offers.sku10_offer1, _Offers.sku10_offer2],
                pickup_buckets=[5001],
                post_buckets=[9003, 9005],
                post_term_delivery=True,
            ),
        ]

        # 1 dropship heavy refrigerator
        cls.delivery_calc.on_request_offer_buckets(weight=20, width=81, height=8, length=810, warehouse_id=164).respond(
            [12], [], []
        )
        # 2 dropship heavy refrigerators - expensive delivery (2500 RUR)
        cls.delivery_calc.on_request_offer_buckets(
            weight=40, width=86, height=17, length=859, warehouse_id=164
        ).respond([13], [], [])
        # pharma - no change in delivery options
        cls.delivery_calc.on_request_offer_buckets(
            weight=14, width=76, height=15, length=753, warehouse_id=160
        ).respond([11], [], [])
        # 2 * sku10_offer1
        cls.delivery_calc.on_request_offer_buckets(weight=20, width=3, height=3, length=9, warehouse_id=145).respond(
            [802], [5], [15]
        )
        # sku10_offer1 + sku10_offer2
        cls.delivery_calc.on_request_offer_buckets(weight=25, width=6, height=21, length=212, warehouse_id=145).respond(
            [803], [5], [14, 140]
        )

    def test_prohibit_dublicate_index_and_postcode(self):
        """
        Тест проверяет, что у почтовых опциях доставки при двух бакетах с одинаковыми тарифами при группировке не
        будет дублей по идентификаторам аутлетов и почтовому индексу.

        Среди тарифов берется больший идентификатор
        """
        request = (
            'place=actual_delivery'
            '&rgb=blue'
            '&rids=213'
            '&offers-list=TestTariffWarning____g:1,TestTariffWarning_2__g:1'
            '&pickup-options=grouped'
            '&pickup-options-extended-grouping=1'
            '&rearr-factors=market_use_post_as_pickup=0'
        )
        response = self.report.request_json(request + NO_COMBINATOR_FLAG)
        self.assertFragmentIn(
            response,
            {
                "delivery": {
                    "postOptions": [
                        {
                            "serviceId": 202,
                            "tariffId": 11102,
                            "outletIds": [4101],
                            "postCodes": [115200],
                        }
                    ]
                }
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_show_filtered_buckets(cls):
        cls.delivery_calc.on_request_offer_buckets(
            weight=1, width=None, height=None, length=None, warehouse_id=160
        ).respond([16, 11], [18, 17], [20, 19])

    def test_show_filtered_buckets(self):
        '''Проверяется, что при отфильтровывании бакетов калькулятором доставки, указываются какие бакеты отфильтровались (а какие прошли)
        Ответ КД настроен так что отдается на заданые вгх (w:7;d:711x71x7;p:711) отдается
        11 бакет(915) для курьерок и 17 бакет(920) для ПВЗ и 19 бакет (940) для почты
        настройку смотри в on_request_offer_buckets
        '''
        fake_request = (
            "place=actual_delivery&rgb=blue&offers-list=FakeOffer_g:1;w:7;d:711x71x7;p:710;ff:0;wh:160&rids=213"
        )

        def buckets(courier, pickup, post):
            return {
                "courier": courier,
                "pickup": pickup,
                "post": post,
            }

        if self.settings.use_saashub_delivery:
            expected_buckets_all = buckets([16, 11], [18, 17], [20, 19])
            expected_buckets_active = buckets([11], [17], [19])
        else:
            expected_buckets_all = buckets([1915, 915], [199920, 99920], [199940, 99940])
            expected_buckets_active = buckets([915], [99920], [99940])

        response = self.report.request_json(
            fake_request + "&show-filtered-buckets-and-carriers=true", headers={'X-Market-Req-ID': "abc125"}
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "bucketsAll": expected_buckets_all,
                        "bucketsActive": expected_buckets_active,
                        "carriersAll": buckets([199, 99], [199, 99], [199, 99]),
                        "carriersActive": buckets([99], [99], [99]),
                    }
                ]
            },
            allow_different_len=False,
        )

        '''Проверяется, что без параметра how-filtered-buckets-and-carriers bucketsAll, bucketsActive, carriersAll, carriersActive - не выводятся.
        '''
        response = self.report.request_json(fake_request, headers={'X-Market-Req-ID': "abc125"})
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "bucketsAll": Absent(),
                        "bucketsActive": Absent(),
                        "carriersAll": Absent(),
                        "carriersActive": Absent(),
                    }
                ]
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_offer_with_several_outlets(cls):
        _Offers.sku6_offer1 = BlueOffer(
            price=55,
            vat=Vat.VAT_18,
            feedid=3,
            offerid='blue.offer.6.1',
            waremd5='Sku6Price55-iLVm1Goleg',
            weight=7,
            dimensions=OfferDimensions(length=23, width=30, height=10),
        )
        cls.index.outlets += [
            Outlet(
                point_id=6001,
                delivery_service_id=151,
                region=213,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(
                    shipper_id=151, day_from=1, day_to=1, order_before=2, work_in_holiday=True, price=100
                ),
                working_days=[i for i in range(10)],
                bool_props=["prepayAllowed", "cashAllowed", "cardAllowed"],
                gps_coord=GpsCoord(37.05, 55.15),
            ),
            Outlet(
                point_id=6002,
                delivery_service_id=153,
                region=213,
                point_type=Outlet.FOR_PICKUP,
                delivery_option=OutletDeliveryOption(
                    shipper_id=153, day_from=1, day_to=1, order_before=2, work_in_holiday=True, price=100
                ),
                working_days=[i for i in range(10)],
                bool_props=["prepayAllowed", "cashAllowed", "cardAllowed"],
                gps_coord=GpsCoord(37.12, 55.7),
            ),
            Outlet(
                point_id=6003,
                delivery_service_id=155,
                region=213,
                point_type=Outlet.FOR_POST_TERM,
                delivery_option=OutletDeliveryOption(
                    shipper_id=155, day_from=1, day_to=1, order_before=2, work_in_holiday=True, price=400
                ),
                working_days=[i for i in range(10)],
                bool_props=["prepayAllowed", "cashAllowed", "cardAllowed"],
                gps_coord=GpsCoord(37.7, 55.12),
            ),
        ]
        cls.index.pickup_buckets += [
            PickupBucket(
                bucket_id=6001,
                dc_bucket_id=21,
                fesh=1,
                carriers=[202],
                options=[
                    PickupOption(outlet_id=6001, day_from=1, day_to=1, price=7),
                    PickupOption(outlet_id=6002, day_from=1, day_to=1, price=6),
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
            PickupBucket(
                bucket_id=6002,
                dc_bucket_id=22,
                fesh=1,
                carriers=[202],
                options=[
                    PickupOption(outlet_id=6001, day_from=1, day_to=1, price=7),
                    PickupOption(outlet_id=6003, day_from=1, day_to=1, price=8),
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
        ]
        cls.index.mskus += [
            MarketSku(
                title="blue offer sku6",
                hyperid=1,
                sku=606060,
                blue_offers=[_Offers.sku6_offer1],
                glparams=[
                    GLParam(param_id=201, value=2),
                    GLParam(param_id=202, value=1),
                    GLParam(param_id=205, value=1),
                ],
                delivery_buckets=[804, 805, 806],
                pickup_buckets=[6001, 6002],
                post_term_delivery=True,
            ),
        ]

    def test_blue_nearest_outlet(self):
        """Проверяем, что для синего оффера выводится ближайшая к точке доставки точка самовывоза."""
        request = 'place=actual_delivery&offers-list=Sku6Price55-iLVm1Goleg:1&rids=213&pickup-options=grouped&pickup-options-extended-grouping=1&geo-location=37.6,55.13'
        request += '&rearr-factors=market_nordstream_relevance=0'
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'results': [
                    {
                        'entity': 'deliveryGroup',
                        'delivery': {
                            'nearestOutlet': {
                                'id': 6003,
                                'gpsCoord': {
                                    'longitude': '37.7',
                                    'latitude': '55.12',
                                },
                            },
                        },
                    }
                ]
            },
        )

    @classmethod
    def prepare_blue_split_box_delivery(cls):
        # Расчет размеров оферов и кол-ва коробок: 89.5*59.5*49.5*8=263599.875*8= 2108799 = (77 * 34 * 44.75 )*18 (8 больших коробок или 18 небольших офферов)

        cls.index.mskus += [
            MarketSku(
                title="blue big Offer for Split",
                hyperid=1,
                sku=10,
                blue_offers=[_Offers.BigOffer, _Offers.SmallOfferForMaxBox],
                pickup_buckets=[5001],
                post_buckets=[9003],
                post_term_delivery=True,
            ),
        ]

        cls.delivery_calc.on_request_offer_buckets(weight=51.75, width=89.5, height=59.5, length=49.5).respond(
            [803], [4], [7]
        )  # большая коробка
        cls.delivery_calc.on_request_offer_buckets(
            weight=23, width=48.9313909, height=48.9313909, length=48.9313909
        ).respond(
            [802, 803], [4], [7]
        )  # небольшая коробка для двух офферов
        cls.delivery_calc.on_request_offer_buckets(weight=23, width=44.75, height=34, length=77).respond(
            [802, 803], [4], [7]
        )  # небольшая коробка для старой логики
        cls.delivery_calc.on_request_offer_buckets(weight=23, width=60, height=60, length=170).respond(
            [803], [4], [7]
        )  # Холодильник

    def test_blue_split_box_delivery(self):
        """Проверяем, что для синего доставка вычисляется со сплитованием корзины."""
        query_base = 'place=actual_delivery&offers-list={}&rids=213&rgb=blue' + NO_COMBINATOR_FLAG
        queries_use_split = [query_base, query_base + "&all-courier-options=true"]

        for request in queries_use_split:
            # Тестирование несколько холодильников
            response = self.report.request_json(request.format("TestSplitBigOffer_1__g:4"))
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            "delivery": {
                                "options": [
                                    {
                                        "serviceId": "159",
                                        "isDefault": True,
                                        "dayFrom": 17,
                                        "dayTo": 27,
                                        "timeIntervals": [
                                            {
                                                "from": "11:00",
                                                "to": "17:00",
                                            },
                                        ],
                                    },
                                ]
                            }
                        }
                    ]
                },
                allow_different_len=False,
            )

            # Тестирование кучи товаров небольших, которые распаковываются в более, чем 1 большую коробку (4)
            response = self.report.request_json(request.format("TestSplitSmallOffer_1g:18"))
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            "delivery": {
                                "options": [
                                    {
                                        "serviceId": "159",
                                        "isDefault": True,
                                        "dayFrom": 17,
                                        "dayTo": 27,
                                        "timeIntervals": [
                                            {
                                                "from": "11:00",
                                                "to": "17:00",
                                            },
                                        ],
                                    },
                                ]
                            }
                        }
                    ]
                },
                allow_different_len=False,
            )

            # Тестирование кучи товаров небольших, которые распаковываются в 1+ больших коробок и одну дополнительную
            response = self.report.request_json(request.format("TestSplitSmallOffer_1g:19"))
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            "delivery": {
                                "options": [
                                    {
                                        "serviceId": "159",
                                        "isDefault": True,
                                        "dayFrom": 17,
                                        "dayTo": 27,
                                        "timeIntervals": [
                                            {
                                                "from": "11:00",
                                                "to": "17:00",
                                            },
                                        ],
                                    },
                                ]
                            }
                        }
                    ]
                },
                allow_different_len=False,
            )

            # Тестирование кучи товаров небольших, которые расспаковываются в 1+ больших коробок + несколько больших офферов, которые едут без коробки
            response = self.report.request_json(request.format("TestSplitSmallOffer_1g:19,TestSplitBigOffer_1__g:4"))
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            "delivery": {
                                "options": [
                                    {
                                        "serviceId": "159",
                                        "isDefault": True,
                                        "dayFrom": 17,
                                        "dayTo": 27,
                                        "timeIntervals": [
                                            {
                                                "from": "11:00",
                                                "to": "17:00",
                                            },
                                        ],
                                    },
                                ]
                            }
                        }
                    ]
                },
                allow_different_len=False,
            )

        # Тестирование кучи товаров небольших, которые расспаковываются одну дополнительную коробку (по старой логике)
        response = self.report.request_json(query_base.format("TestSplitSmallOffer_1g:1"))
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "delivery": {
                            "options": [
                                {
                                    "serviceId": "158",
                                    "isDefault": True,
                                    "dayFrom": 11,
                                    "dayTo": 12,
                                    "timeIntervals": [
                                        {
                                            "from": "10:00",
                                            "to": "17:30",
                                        },
                                    ],
                                },
                            ]
                        }
                    }
                ]
            },
        )

        request = query_base + "&all-courier-options=true"
        # Тестирование кучи товаров небольших, которые расспаковываются одну дополнительную коробку (по старой логики)
        # timeIntervals - только у дефотлтной СД
        response = self.report.request_json(request.format("TestSplitSmallOffer_1g:1"))
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "delivery": {
                            "options": [
                                {
                                    "serviceId": "158",
                                    "isDefault": True,
                                    "dayFrom": 11,
                                    "dayTo": 12,
                                    "timeIntervals": [
                                        {
                                            "from": "10:00",
                                            "to": "17:30",
                                        },
                                    ],
                                },
                                {
                                    "serviceId": "159",
                                    "dayFrom": 17,
                                    "dayTo": 27,
                                },
                            ]
                        }
                    }
                ]
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_for_white_modifiers(cls):
        cls.index.delivery_modifiers += [
            DeliveryModifier(
                action=CostModificationRule(operation=ModificationOperation.DIVIDE, parameter=2),
                modifier_id=2,
            ),
            DeliveryModifier(
                action=CostModificationRule(operation=ModificationOperation.FIX_VALUE, parameter=1500),
                condition=DeliveryModifierCondition(
                    regions=[216],
                ),
                modifier_id=6,
            ),
            DeliveryModifier(
                action=RegionsAvailability(False),
                condition=DeliveryModifierCondition(
                    regions=[216],
                ),
                modifier_id=7,
            ),
            DeliveryModifier(
                action=TimeModificationRule(operation=ModificationOperation.MULTIPLY, parameter=3),
                condition=DeliveryModifierCondition(
                    delivery_cost_condition=DeliveryCostCondition(
                        percent_from_offer_price=50, comparison_operation=ComparisonOperation.LESS
                    )
                ),
                modifier_id=8,
            ),
            DeliveryModifier(
                action=TimeModificationRule(operation=ModificationOperation.MULTIPLY, parameter=3),
                condition=DeliveryModifierCondition(
                    delivery_cost_condition=DeliveryCostCondition(
                        percent_from_offer_price=10, comparison_operation=ComparisonOperation.LESS
                    )
                ),
                modifier_id=9,
            ),
        ]

        cls.index.offers += [
            Offer(
                price=100,
                fesh=12,
                feedid=112,
                waremd5='22222222222222ggggMODg',
                title='Offer_with_mod',
                weight=5,
                dimensions=OfferDimensions(length=20, width=30, height=10),
                cargo_types=[256, 10],
                delivery_info=OfferDeliveryInfo(
                    courier_buckets=[BucketInfo(bucket_id=1101, cost_modifiers=[6], is_new=True)]
                ),
                cpa=Offer.CPA_REAL,
                hyperid=7001,
                has_delivery_options=False,
            ),
            Offer(
                price=100,
                fesh=12,
                feedid=112,
                waremd5='2REGULAR222222ggggMODg',
                title='Offer_with_mod',
                dimensions=OfferDimensions(length=5, width=5, height=5),
                cargo_types=[256, 10],
                delivery_info=OfferDeliveryInfo(courier_buckets=[BucketInfo(bucket_id=1102)]),
                cpa=Offer.CPA_REAL,
                hyperid=7001,
                has_delivery_options=False,
            ),
            Offer(
                price=100,
                fesh=13,
                feedid=113,
                waremd5='22222222222222ggFEEDgg',
                title='Offer_from_another_feed',
                dimensions=OfferDimensions(length=5, width=5, height=5),
                cargo_types=[256, 10],
                delivery_options=[DeliveryOption(price=100, day_from=3, day_to=4, order_before=14)],
                cpa=Offer.CPA_REAL,
                hyperid=7001,
            ),
            Offer(
                price=100,
                fesh=14,
                feedid=114,
                waremd5='Offer_with_weight_1ggg',
                title='Offer with weight 1',
                dimensions=OfferDimensions(length=5, width=5, height=5),
                weight=1,
                delivery_options=[DeliveryOption(price=100, day_from=3, day_to=4, order_before=14)],
                cpa=Offer.CPA_REAL,
                hyperid=7001,
            ),
            Offer(
                price=100,
                fesh=14,
                feedid=114,
                waremd5='Offer_with_weight_2ggg',
                title='Offer with weight 2',
                dimensions=OfferDimensions(length=5, width=5, height=5),
                weight=2,
                delivery_options=[DeliveryOption(price=100, day_from=3, day_to=4, order_before=14)],
                cpa=Offer.CPA_REAL,
                hyperid=7001,
            ),
            Offer(
                price=100,
                fesh=14,
                feedid=114,
                waremd5='Offer_with_weight_5ggg',
                title='Offer with weight 5',
                dimensions=OfferDimensions(length=5, width=5, height=5),
                delivery_options=[DeliveryOption(price=100, day_from=3, day_to=4, order_before=14)],
                weight=5,
                cpa=Offer.CPA_REAL,
                hyperid=7001,
            ),
            Offer(
                price=100,
                fesh=15,
                feedid=115,
                waremd5='Offer_without_del_geng',
                title='Simple offer',
                dimensions=OfferDimensions(length=5, width=5, height=5),
                weight=1,
                delivery_options=[DeliveryOption(price=100, day_from=3, day_to=4, order_before=14)],
                cpa=Offer.CPA_REAL,
                hyperid=7001,
            ),
            Offer(
                price=100,
                fesh=16,
                feedid=116,
                waremd5='Offer_with_mmap_gen__g',
                title='Simple offer',
                dimensions=OfferDimensions(length=5, width=5, height=5),
                weight=1,
                delivery_options=[DeliveryOption(price=100, day_from=3, day_to=4, order_before=14)],
                cpa=Offer.CPA_REAL,
                hyperid=7001,
            ),
        ]

    def test_white_modifiers_for_single_offer(self):
        """
        Проверяем, что белый маркет может использовать actual_delivery для одиночных офферов
        с опциями доставки из индекса. Модификаторы применяются по-умолчанию.
        """
        request = 'place=actual_delivery&offers-list={}&rids={}&regset=1&rearr-factors=market_white_actual_delivery=1'
        response = self.report.request_json(request.format('22222222222222ggggMODg:1', 213))
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "delivery": {
                            "options": [
                                {
                                    "serviceId": "99",
                                    "isDefault": True,
                                    "price": {
                                        "currency": "RUR",
                                        "value": "200",
                                    },
                                    "dayFrom": 2,
                                    "dayTo": 5,
                                },
                            ]
                        }
                    }
                ]
            },
        )

        # Применяется модификатор по региону
        response = self.report.request_json(request.format('22222222222222ggggMODg:1', 216))
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "delivery": {
                            "options": [
                                {
                                    "serviceId": "99",
                                    "isDefault": True,
                                    "price": {
                                        "currency": "RUR",
                                        "value": "1500",
                                    },
                                    "dayFrom": 2,
                                    "dayTo": 5,
                                },
                            ]
                        }
                    }
                ]
            },
        )

        # Проверяем белый оффер с другой программой доставки (РЕГУЛЯР)
        response = self.report.request_json(request.format('2REGULAR222222ggggMODg:1', 213))
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "delivery": {
                            "options": [
                                {
                                    "serviceId": "99",
                                    "isDefault": True,
                                    "price": {
                                        "currency": "RUR",
                                        "value": "200",
                                    },
                                    "dayFrom": 1,
                                    "dayTo": 3,
                                },
                            ]
                        }
                    }
                ]
            },
        )

    def test_white_the_same_client_id(self):
        """
        Проверяем, что репорт прокидывает ошибку, если пришли с несколькими оффера от разных поставщиков
        """
        request = 'place=actual_delivery&offers-list={}&rids={}&regset=1&rearr-factors=market_white_actual_delivery=1'
        response = self.report.request_json(request.format('22222222222222ggggMODg:1,22222222222222gggg404g:1', 213))
        self.assertFragmentIn(response, {"commonProblems": ["NOT_ALL_THE_SAME_SUPPLIER_CLIENT_ID"]})

        """
        Проверяем, что репорт прокидывает ошибку, что офферы с разных фидов
        """
        request = 'place=actual_delivery&offers-list={}&rids={}&regset=1&rearr-factors=market_white_actual_delivery=1'
        response = self.report.request_json(request.format('22222222222222ggggMODg:1,22222222222222ggFEEDgg:1', 213))
        self.assertFragmentIn(response, {"commonProblems": ["NOT_ALL_THE_SAME_FEED_ID"]})

    @classmethod
    def prepare_white_delivery_calculator_trace_log(cls):
        # Поколения доставки в MMAP файле
        cls.index.delivery_calc_feed_info += [
            DeliveryCalcFeedInfo(
                feed_id=112, generation_id=6
            ),  # Только для магазина 12 прописываем в MMAP файле другое поколение КД
            DeliveryCalcFeedInfo(feed_id=116, generation_id=6),
        ]
        # Поколения доставки в FB файле
        cls.index.delivery_calc_shop_info += [
            DeliveryCalcShopInfo(shop_id=12, generation_id=66),  # Магазин 12
            DeliveryCalcShopInfo(shop_id=13, generation_id=6),  # Магазин 13
            DeliveryCalcShopInfo(shop_id=14, generation_id=6),  # Магазин 14
            DeliveryCalcShopInfo(shop_id=1002, generation_id=6),  # Магазин 1002
        ]

    def test_white_delivery_calculator_trace_log(self):
        """
        Проверяем, что в КД 1.5 (ручка /shopOffers) уходят запросы с корректными входными данными для DSBS:
        shop_id, feed_id, program_type, generation_id
        А также, что мок калькулятора доставки корректно на них отвечает
        """

        # Запрос на оферы от магазина 12
        test_request = (
            "place=actual_delivery&offers-list={}&rids={}&regset=1&rearr-factors=market_white_actual_delivery=1".format(
                '22222222222222ggggMODg:2,2REGULAR222222ggggMODg:2', 213
            )
        )
        self.report.request_json(test_request, headers={'X-Market-Req-ID': "abc123"})
        self.external_services_trace_log.expect(
            target_host=self.delivery_calc.host_and_port(),
            request_id=Regex(r"abc123/\d+"),
            target_module="Delivery Calculator",
            source_module="market-report",
            request_method="/shopOffers",
            http_code=200,
            http_method="POST",
            retry_num=0,
            kv_out_courier_buckets=Contains("bucket_id: 1104"),
            kv_in_shop_id=12,
            kv_in_feed_id=112,
            kv_in_program_type=0,
            kv_in_generation_id=66,
        )

    @classmethod
    def prepare_for_white_delivery_calc(cls):
        cls.delivery_calc.on_request_shop_offers(feed_id=112, price=400, program_type=[0]).respond(
            [DCBucketInfo(1104, [], [], [], [], True)], [], []
        )
        cls.delivery_calc.on_request_shop_offers(feed_id=113, price=400, program_type=[0]).respond(
            [DCBucketInfo(1104, [], [], [], [], True)], [], []
        )

        cls.delivery_calc.on_request_shop_offers(feed_id=112, price=100, program_type=[0]).respond(
            [DCBucketInfo(11103, [], [], [], [], True)], [], []
        )
        cls.delivery_calc.on_request_shop_offers(feed_id=112, price=200, program_type=[0]).respond(
            [DCBucketInfo(11103, [], [], [], [], True)], [DCBucketInfo(51103, [], [], [], [], True)], []
        )
        cls.delivery_calc.on_request_shop_offers(feed_id=112, price=300, program_type=[0]).respond(
            [DCBucketInfo(11103, [2], [], [], [], True)], [DCBucketInfo(51103, [2], [], [], [], True)], []
        )
        cls.delivery_calc.on_request_shop_offers(feed_id=112, price=500, program_type=[0]).respond(
            [DCBucketInfo(11103, [6], [], [], [], True)], [DCBucketInfo(51103, [6], [], [], [], True)], []
        )
        cls.delivery_calc.on_request_shop_offers(feed_id=112, price=600, program_type=[0]).respond(
            [DCBucketInfo(11103, [], [], [], [7], True)], [DCBucketInfo(51103, [], [], [], [7], True)], []
        )
        cls.delivery_calc.on_request_shop_offers(feed_id=112, price=700, program_type=[0]).respond(
            [DCBucketInfo(11103, [], [8], [], [], True)], [DCBucketInfo(51103, [], [9], [], [], True)], []
        )
        cls.delivery_calc.on_request_shop_offers(feed_id=112, price=1100, program_type=[0]).respond(
            [DCBucketInfo(11103, [], [8], [], [], True)], [DCBucketInfo(51103, [], [9], [], [], True)], []
        )
        cls.delivery_calc.on_request_shop_offers(feed_id=114, price=400, program_type=[0], weight=20).respond(
            [DCBucketInfo(11107, [], [], [], [], True)], [], []
        )
        cls.delivery_calc.on_request_shop_offers(feed_id=114, price=200, program_type=[0], weight=7).respond(
            [DCBucketInfo(11107, [], [], [], [], True)], [], []
        )
        cls.delivery_calc.on_request_shop_offers(feed_id=114, price=200, program_type=[0], weight=3).respond(
            [DCBucketInfo(11108, [], [], [], [], True)], [], []
        )
        cls.delivery_calc.on_request_shop_offers(feed_id=114, price=100, program_type=[0], weight=5).respond(
            [DCBucketInfo(11107, [], [], [], [], True)], [], []
        )
        cls.delivery_calc.on_request_shop_offers(feed_id=115, price=200, program_type=[0], weight=2).respond(
            [DCBucketInfo(11109, [], [], [], [], True)], [], []
        )

    def test_white_force_delivery_calc_for_single_offer(self):
        """
        Проверяем, что при передаче параметра force-use-delivery-calc при расчете доставки одного белого оффера,
        опции будут запрошены из калькулятора доставки, а не взяты из фида
        """
        force_delivery_calc_param = '&force-use-delivery-calc={}'

        # передаем force-use-delivery-calc=1, получаем опции из КД - бакет 1103
        request = (
            'place=actual_delivery&offers-list={}&rids={}&regset=1&rearr-factors=market_white_actual_delivery=1'
            + force_delivery_calc_param.format(1)
        )
        response = self.report.request_json(request.format('22222222222222ggggMODg:1', 213))
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "delivery": {
                            "options": [
                                {
                                    "serviceId": "99",
                                    "isDefault": True,
                                    "price": {
                                        "currency": "RUR",
                                        "value": "500",
                                    },
                                    "dayFrom": 1,
                                    "dayTo": 3,
                                },
                            ]
                        }
                    }
                ]
            },
        )

        # передаем force-use-delivery-calc=0, получаем опции из фида - бакет 1101
        request = (
            'place=actual_delivery&offers-list={}&rids={}&regset=1&rearr-factors=market_white_actual_delivery=1'
            + force_delivery_calc_param.format(0)
        )
        response = self.report.request_json(request.format('22222222222222ggggMODg:1', 213))
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "delivery": {
                            "options": [
                                {
                                    "serviceId": "99",
                                    "isDefault": True,
                                    "price": {
                                        "currency": "RUR",
                                        "value": "200",
                                    },
                                    "dayFrom": 2,
                                    "dayTo": 5,
                                },
                            ]
                        }
                    }
                ]
            },
        )

    def test_white_delivery_calc(self):
        """
        Проверяем, что для нескольких офферов будет поход в КД 1.5
        TODO: Пока только на курьерке
        """
        request = 'place=actual_delivery&offers-list={}&rids={}&regset=1&rearr-factors=market_white_actual_delivery=1'

        # Цена корзины 200 (1 * 100 + 1 * 100), бакет 1103 без модификаторов (цена доставки = 500, сроки = 1 / 3)
        response = self.report.request_json(request.format('22222222222222ggggMODg:1,2REGULAR222222ggggMODg:1', 213))
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "delivery": {
                            "options": [
                                {
                                    "serviceId": "99",
                                    "isDefault": True,
                                    "price": {
                                        "currency": "RUR",
                                        "value": "500",
                                    },
                                    "dayFrom": 1,
                                    "dayTo": 3,
                                },
                            ]
                        }
                    }
                ]
            },
        )

        # Цена корзины 400 (2 * 100 + 2 * 100), бакет 1104 без модификаторов (цена доставки = 0, сроки = 2 / 5)
        response = self.report.request_json(request.format('22222222222222ggggMODg:2,2REGULAR222222ggggMODg:2', 213))
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "delivery": {
                            "options": [
                                {
                                    "serviceId": "99",
                                    "isDefault": True,
                                    "price": {
                                        "currency": "RUR",
                                        "value": "0",
                                    },
                                    "dayFrom": 2,
                                    "dayTo": 5,
                                },
                            ]
                        }
                    }
                ]
            },
        )

        # Потому что вес должен быть у всех офферов в корзине
        self.assertFragmentNotIn(response, {"logicTrace": [Contains("weight")]})
        self.assertFragmentNotIn(response, {"logicTrace": [Contains("width")]})

        response = self.report.request_json(request.format('22222222222222ggggMODg:2', 213) + "&debug=1")
        self.assertFragmentIn(response, {"logicTrace": [Contains("weight: 10")]})
        self.assertFragmentIn(response, {"logicTrace": [Contains("width: 22\n  height: 22\n  length: 32")]})

    def test_white_delivery_calc_cost_modifier(self):
        """
        Проверяем работу модификатора цены доставки для белых оферов и КД 1.5,
        а также условия по региону
        TODO: Пока только на курьерке
        """
        request = 'place=actual_delivery&offers-list={}&rids={}&regset=1&rearr-factors=market_white_actual_delivery=1'

        # Цена корзины 300 (2 * 100 + 1 * 100), бакет 1103 с модификатором id = 2 (цена уменьшается в два раза)
        response = self.report.request_json(request.format('22222222222222ggggMODg:2,2REGULAR222222ggggMODg:1', 213))
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "delivery": {
                            "options": [
                                {
                                    "serviceId": "99",
                                    "isDefault": True,
                                    "price": {
                                        "currency": "RUR",
                                        "value": "250",
                                    },
                                    "dayFrom": 1,
                                    "dayTo": 3,
                                },
                            ]
                        }
                    }
                ]
            },
        )

        # Цена корзины 500 (3 * 100 + 2 * 100), бакет 1103 с модификатором id = 6 (только для региона = 216 цена доставки = 1500)
        response = self.report.request_json(request.format('22222222222222ggggMODg:3,2REGULAR222222ggggMODg:2', 213))
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "delivery": {
                            "options": [
                                {
                                    "serviceId": "99",
                                    "isDefault": True,
                                    "price": {
                                        "currency": "RUR",
                                        "value": "500",
                                    },
                                    "dayFrom": 1,
                                    "dayTo": 3,
                                },
                            ]
                        }
                    }
                ]
            },
        )
        response = self.report.request_json(request.format('22222222222222ggggMODg:3,2REGULAR222222ggggMODg:2', 216))
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "delivery": {
                            "options": [
                                {
                                    "serviceId": "99",
                                    "isDefault": True,
                                    "price": {
                                        "currency": "RUR",
                                        "value": "1500",
                                    },
                                    "dayFrom": 1,
                                    "dayTo": 3,
                                },
                            ]
                        }
                    }
                ]
            },
        )

    def test_white_delivery_calc_availability_modifier(self):
        """
        Проверяем работу модификатора доступности доставки для белых оферов и КД 1.5
        вместе с условием по региону
        TODO: Пока только на курьерке
        """
        request = 'place=actual_delivery&offers-list={}&rids={}&regset=1&rearr-factors=market_white_actual_delivery=1'

        # Цена корзины 600 (3 * 100 + 3 * 100), бакет 1103 с модификатором id = 7 (только для региона = 216 доставка выключена)
        response = self.report.request_json(request.format('22222222222222ggggMODg:3,2REGULAR222222ggggMODg:3', 213))
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "delivery": {
                            "options": [
                                {
                                    "serviceId": "99",
                                    "isDefault": True,
                                    "price": {
                                        "currency": "RUR",
                                        "value": "500",
                                    },
                                    "dayFrom": 1,
                                    "dayTo": 3,
                                },
                            ]
                        }
                    }
                ]
            },
        )
        response = self.report.request_json(request.format('22222222222222ggggMODg:3,2REGULAR222222ggggMODg:3', 216))
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "delivery": {
                            "options": Absent(),
                        }
                    }
                ]
            },
        )

    def test_white_delivery_calc_time_modifier(self):
        """
        Проверяем работу модификатора сроков доставки для белых оферов и КД 1.5
        вместе с условием по цене доставки
        TODO: Пока только на курьерке
        """
        request = 'place=actual_delivery&offers-list={}&rids={}&regset=1&rearr-factors=market_white_actual_delivery=1'

        # Цена корзины 700 (4 * 100 + 3 * 100) и 1100 (6 * 100 + 5 * 100),
        # Бакет 1103 с модификатором id = 8
        # Cрок доставки увеличивается в 3 раза = 3 / 9,
        # если цена доставки < 50% от цены корзины (500 > 700 * 50% и 500 < 1100 * 50%)
        response = self.report.request_json(request.format('22222222222222ggggMODg:4,2REGULAR222222ggggMODg:3', 213))
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "delivery": {
                            "options": [
                                {
                                    "serviceId": "99",
                                    "isDefault": True,
                                    "price": {
                                        "currency": "RUR",
                                        "value": "500",
                                    },
                                    "dayFrom": 1,
                                    "dayTo": 3,
                                },
                            ]
                        }
                    }
                ]
            },
        )
        response = self.report.request_json(request.format('22222222222222ggggMODg:6,2REGULAR222222ggggMODg:5', 213))
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "delivery": {
                            "options": [
                                {
                                    "serviceId": "99",
                                    "isDefault": True,
                                    "price": {
                                        "currency": "RUR",
                                        "value": "500",
                                    },
                                    "dayFrom": 3,
                                    "dayTo": 9,
                                },
                            ]
                        }
                    }
                ]
            },
        )

    def test_white_delivery_weight_tariffs(self):
        """
        Проверяем, что репорт передает в калькулятор доставки вес для белой посылки
        """
        request = 'place=actual_delivery&offers-list={}&rids={}&regset=1&rearr-factors=market_white_actual_delivery=1'
        response = self.report.request_json(request.format('Offer_with_weight_5ggg:1,Offer_with_weight_2ggg:1', 213))
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "delivery": {
                            "options": [
                                {
                                    "serviceId": "99",
                                    "isDefault": True,
                                    "price": {
                                        "currency": "RUR",
                                        "value": "700",
                                    },
                                    "dayFrom": 5,
                                    "dayTo": 6,
                                },
                            ]
                        }
                    }
                ]
            },
        )

        response = self.report.request_json(request.format('Offer_with_weight_1ggg:1,Offer_with_weight_2ggg:1', 213))
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "delivery": {
                            "options": [
                                {
                                    "serviceId": "99",
                                    "isDefault": True,
                                    "price": {
                                        "currency": "RUR",
                                        "value": "550",
                                    },
                                    "dayFrom": 15,
                                    "dayTo": 16,
                                },
                            ]
                        }
                    }
                ]
            },
        )

    def test_white_delivery_cant_get_generation(self):
        """
        Проверяем, что репорт не падает и не рассчитывает доставку для белых офферов, если поколение доставки не удалось получить
            для магазина из fb файла
        """
        request = 'place=actual_delivery&offers-list={}&rids={}&regset=1&rearr-factors=market_white_actual_delivery=1'
        response = self.report.request_json(request.format('Offer_without_del_geng:2', 213))
        self.assertFragmentNotIn(response, {"results": [{"delivery": {"options"}}]})

        response = self.report.request_json(request.format('Offer_with_mmap_gen__g:2', 213))
        self.assertFragmentNotIn(response, {"results": [{"delivery": {"options"}}]})

    @classmethod
    def prepare_white_delivery_calc_pickup(cls):
        cls.index.outlets += [
            Outlet(
                fesh=5,
                region=213,
                point_type=Outlet.FOR_STORE,
                point_id=5103,
                delivery_option=OutletDeliveryOption(
                    shipper_id=99, day_from=1, day_to=1, order_before=2, work_in_holiday=True, price=100
                ),
                working_days=[i for i in range(10)],
                bool_props=["prepayAllowed", "cashAllowed", "cardAllowed"],
                gps_coord=GpsCoord(37.12, 55.32),
                dimensions=Dimensions(width=100, height=90, length=80),
            ),
            Outlet(
                fesh=5,
                region=216,
                point_type=Outlet.FOR_STORE,
                point_id=51033,
                delivery_option=OutletDeliveryOption(
                    shipper_id=99, day_from=1, day_to=1, order_before=2, work_in_holiday=True, price=100
                ),
                working_days=[i for i in range(10)],
                bool_props=["prepayAllowed", "cashAllowed", "cardAllowed"],
                gps_coord=GpsCoord(37.12, 55.32),
                dimensions=Dimensions(width=100, height=90, length=80),
            ),
        ]

        cls.index.new_pickup_buckets += [
            NewPickupBucket(
                bucket_id=5103,
                dc_bucket_id=51103,
                fesh=12,
                carriers=[99],
                delivery_program=DeliveryBucket.REGULAR_PROGRAM,
                region_groups=[
                    PickupRegionGroup(options=[NewPickupOption(price=100, day_from=1, day_to=2)], outlets=[5103]),
                    PickupRegionGroup(options=[NewPickupOption(price=100, day_from=1, day_to=2)], outlets=[51033]),
                ],
            ),
        ]

    def test_white_delivery_calc_pickup(self):
        """
        Проверяем, что для нескольких офферов будет поход в КД 1.5
        ...
        """
        request = 'place=actual_delivery&offers-list={}&rids={}&regset=1&pickup-options=raw&rearr-factors=market_white_actual_delivery=1'

        # Цена корзины 200 (1 * 100 + 1 * 100), бакет 5103
        response = self.report.request_json(request.format('22222222222222ggggMODg:1,2REGULAR222222ggggMODg:1', 213))
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "delivery": {
                            "pickupOptions": [
                                {
                                    "serviceId": 99,
                                    "price": {
                                        "currency": "RUR",
                                        "value": "100",
                                    },
                                    "dayFrom": 1,
                                    "dayTo": 2,
                                },
                            ]
                        }
                    }
                ]
            },
        )

    def test_white_delivery_calc_pickup_cost_modifier(self):
        """
        Проверяем работу модификатора цены доставки для белых оферов и КД 1.5,
        а также условия по региону
        """
        request = 'place=actual_delivery&offers-list={}&rids={}&regset=1&pickup-options=raw&rearr-factors=market_white_actual_delivery=1'

        # Цена корзины 300 (2 * 100 + 1 * 100), бакет 5103 с модификатором id = 2 (цена уменьшается в два раза)
        response = self.report.request_json(request.format('22222222222222ggggMODg:2,2REGULAR222222ggggMODg:1', 213))
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "delivery": {
                            "pickupOptions": [
                                {
                                    "serviceId": 99,
                                    "price": {
                                        "currency": "RUR",
                                        "value": "50",
                                    },
                                    "dayFrom": 1,
                                    "dayTo": 2,
                                },
                            ]
                        }
                    }
                ]
            },
        )

        # Цена корзины 500 (3 * 100 + 2 * 100), бакет 5103 с модификатором id = 6 (только для региона = 216 цена доставки = 1500)
        response = self.report.request_json(request.format('22222222222222ggggMODg:3,2REGULAR222222ggggMODg:2', 213))
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "delivery": {
                            "pickupOptions": [
                                {
                                    "serviceId": 99,
                                    "price": {
                                        "currency": "RUR",
                                        "value": "100",
                                    },
                                    "dayFrom": 1,
                                    "dayTo": 2,
                                },
                            ]
                        }
                    }
                ]
            },
        )
        response = self.report.request_json(request.format('22222222222222ggggMODg:3,2REGULAR222222ggggMODg:2', 216))
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "delivery": {
                            "pickupOptions": [
                                {
                                    "serviceId": 99,
                                    "price": {
                                        "currency": "RUR",
                                        "value": "1500",
                                    },
                                    "dayFrom": 1,
                                    "dayTo": 2,
                                },
                            ]
                        }
                    }
                ]
            },
        )

    def test_white_delivery_calc_pickup_availability_modifier(self):
        """
        Проверяем работу модификатора доступности доставки для белых оферов и КД 1.5
        вместе с условием по региону
        """
        request = 'place=actual_delivery&offers-list={}&rids={}&regset=1&pickup-options=raw&rearr-factors=market_white_actual_delivery=1'

        # Цена корзины 600 (3 * 100 + 3 * 100), бакет 5103 с модификатором id = 7 (только для региона = 216 доставка выключена)
        response = self.report.request_json(request.format('22222222222222ggggMODg:3,2REGULAR222222ggggMODg:3', 213))
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "delivery": {
                            "pickupOptions": [
                                {
                                    "serviceId": 99,
                                    "price": {
                                        "currency": "RUR",
                                        "value": "100",
                                    },
                                    "dayFrom": 1,
                                    "dayTo": 2,
                                },
                            ]
                        }
                    }
                ]
            },
        )
        response = self.report.request_json(request.format('22222222222222ggggMODg:3,2REGULAR222222ggggMODg:3', 216))
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "delivery": {
                            "pickupOptions": Absent(),
                        }
                    }
                ]
            },
        )

    def test_white_delivery_calc_pickup_time_modifier(self):
        """
        Проверяем работу модификатора сроков доставки для белых оферов и КД 1.5
        вместе с условием по цене доставки
        """
        request = 'place=actual_delivery&offers-list={}&rids={}&regset=1&pickup-options=raw&rearr-factors=market_white_actual_delivery=1'

        # Цена корзины 700 (4 * 100 + 3 * 100) и 1100 (6 * 100 + 5 * 100),
        # Бакет 5103 с модификатором id = 9
        # Cрок доставки увеличивается в 3 раза = 3 / 6,
        # если цена доставки < 10% от цены корзины (100 > 700 * 10% и 100 < 1100 * 10%)
        response = self.report.request_json(request.format('22222222222222ggggMODg:4,2REGULAR222222ggggMODg:3', 213))
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "delivery": {
                            "pickupOptions": [
                                {
                                    "serviceId": 99,
                                    "price": {
                                        "currency": "RUR",
                                        "value": "100",
                                    },
                                    "dayFrom": 1,
                                    "dayTo": 2,
                                },
                            ]
                        }
                    }
                ]
            },
        )
        response = self.report.request_json(request.format('22222222222222ggggMODg:6,2REGULAR222222ggggMODg:5', 213))
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "delivery": {
                            "pickupOptions": [
                                {
                                    "serviceId": 99,
                                    "price": {
                                        "currency": "RUR",
                                        "value": "100",
                                    },
                                    "dayFrom": 3,
                                    "dayTo": 6,
                                },
                            ]
                        }
                    }
                ]
            },
        )

    def test_no_offer_delivery(self):
        """Проверка того, что при передаче флага market_no_offer_delivery_in_actual_delivery
        опции доставки для оффера не вычисляются и не выводятся"""

        TEST_OFFERS = [
            (_Offers.sku2_offer1,),
            (_Offers.sku2_offer1, _Offers.sku1_offer1),
        ]

        for test_offers in TEST_OFFERS:
            rearr_template = '&rearr-factors=market_no_offer_delivery_in_actual_delivery={}'
            offers_list = ','.join(['{}:1'.format(o.waremd5) for o in test_offers])
            courier_request = "place=actual_delivery&rids=213&rgb=blue&offers-list=" + offers_list + NO_COMBINATOR_FLAG
            for rearr, no_offer_delivery in (
                ('', True),
                (rearr_template.format(0), False),
                (rearr_template.format(1), True),
            ):
                response = self.report.request_json(courier_request + rearr)
                self.assertFragmentIn(
                    response,
                    {
                        "results": [
                            {
                                "delivery": {
                                    "options": [
                                        {
                                            "partnerType": "market_delivery",
                                        }
                                    ],
                                },
                                "offers": [
                                    {
                                        "wareId": offer.waremd5,
                                        "delivery": Absent()
                                        if no_offer_delivery
                                        else {
                                            "options": [
                                                {
                                                    "partnerType": "market_delivery",
                                                }
                                            ],
                                        },
                                    }
                                    for offer in test_offers
                                ],
                            }
                        ],
                    },
                    allow_different_len=True,
                )

    def test_delivery_service_with_region_tree(self):
        """Есть дропшип в регионе 215 и магистрали доставки (401 и 402), настроенные для региона 215 и его родительского региона 217.
        Проверяем, что сроки доставки одинаковые (корректно добавляется связка дропшип в 215 регионе - магистраль для 217 региона).
        """
        request = 'place=actual_delivery&offers-list={}:1&rids=2&debug-all-courier-options=1'.format(
            _Offers.dropship_offer1.waremd5
        )
        response = self.report.request_json(request + NO_COMBINATOR_FLAG)

        self.assertFragmentIn(
            response,
            {
                "entity": "deliveryGroup",
                "delivery": {
                    "isAvailable": True,
                    "options": [
                        {"dayFrom": 9, "dayTo": 12, "orderBefore": "17", "serviceId": str(service_id)}
                        for service_id in [401, 402]
                    ],
                },
            },
        )

    def test_large_size_field(self):
        """
        Проверяем, что для синих и белых посылок будет поле largeSize в ответе actual_delivery
        """
        white_actual_delivery_request = (
            'place=actual_delivery&offers-list=Offer_with_weight_5ggg:{count}&'
            'rids=213&regset=1&rearr-factors=market_white_actual_delivery={flag}&force-use-delivery-calc={param}'
            + NO_COMBINATOR_FLAG
        )
        blue_actual_delivery_request = (
            'place=actual_delivery&offers-list={waremd}:{count}&rids=213&rgb=blue' + NO_COMBINATOR_FLAG
        )

        # Проверяем белые посылки, независимо от флага market_white_actual_delivery и параметра force-use-delivery-calc
        # поле заполняется корректно
        for flag in (0, 1):
            for param in (0, 1):
                response = self.report.request_json(
                    white_actual_delivery_request.format(count=1, flag=flag, param=param)
                )
                self.assertFragmentIn(
                    response,
                    {
                        "results": [
                            {
                                "largeSize": False,
                            }
                        ]
                    },
                )

                response = self.report.request_json(
                    white_actual_delivery_request.format(count=4, flag=flag, param=param)
                )
                self.assertFragmentIn(
                    response,
                    {
                        "results": [
                            {
                                "largeSize": True,
                            }
                        ]
                    },
                )
        # У синего оффера выставлен карготип 300, проверим, что это не влияет на largeSize (важен только вес)
        response = self.report.request_json(
            blue_actual_delivery_request.format(count=1, waremd=_Offers.dropship_offer.waremd5)
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "largeSize": False,
                    }
                ]
            },
        )
        response = self.report.request_json(
            blue_actual_delivery_request.format(count=4, waremd=_Offers.BigOffer.waremd5)
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "largeSize": True,
                    }
                ]
            },
        )

    def test_delivery_options_delivery_days_unknown(self):
        """
        Проверяем, что при неизвестных датах в опциях доставки (от и/или до)
        запрос в комбинатор не содержит некорректных опций доставки.
        """
        request = (
            'place=actual_delivery'
            '&rids=213'
            '&rearr-factors=combinator=1'
            '&rearr-factors=use_dsbs_combinator_response_in_actual_delivery=1'
            '&combinator=1'
            '&offers-list={waremd}:{count}'
            '&debug=1'
        )

        # эталонный запрос оффера с корректными данными для day_from и day_to
        response = self.report.request_json(request.format(count=1, waremd='offer_with_good_do___g'))
        self.assertFragmentIn(
            response,
            {
                "logicTrace": [
                    ".*Combinator request: items { required_count: 1 weight: 1000 dimensions: 1 dimensions: 1 dimensions: 1 available_offers { .* shop_id: 17 available_count: 1 feed_id: 117 delivery_options { day_from: 3 day_to: 5 order_before: 24 } } price: 100 } destination { region_id: 213 } total_price: 100.*"  # noqa
                ]
            },
            use_regex=True,
        )

        # запрос оффера с некорректным (255) значением day_to
        # в запросе в комбинатор должно отсутствовать поле delivery_options
        response = self.report.request_json(request.format(count=1, waremd='offer_with_bad_do_idtg'))
        self.assertFragmentIn(
            response,
            {
                "logicTrace": [
                    ".*Combinator request: items { required_count: 1 weight: 1000 dimensions: 1 dimensions: 1 dimensions: 1 available_offers { .* shop_id: 17 available_count: 1 feed_id: 117 } price: 100 } destination { region_id: 213 } total_price: 100.*"  # noqa
                ]
            },
            use_regex=True,
        )

        # запрос оффера с некорректным (255) значением day_from
        # в запросе в комбинатор должно отсутствовать поле delivery_options
        response = self.report.request_json(request.format(count=1, waremd='offer_with_bad_do_idfg'))
        self.assertFragmentIn(
            response,
            {
                "logicTrace": [
                    ".*Combinator request: items { required_count: 1 weight: 1000 dimensions: 1 dimensions: 1 dimensions: 1 available_offers { .* shop_id: 17 available_count: 1 feed_id: 117 } price: 100 } destination { region_id: 213 } total_price: 100.*"  # noqa
                ]
            },
            use_regex=True,
        )


if __name__ == '__main__':
    main()
