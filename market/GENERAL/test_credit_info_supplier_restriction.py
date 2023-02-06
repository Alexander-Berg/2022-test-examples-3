#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.matcher import Absent
from core.types import (
    BlueOffer,
    BucketInfo,
    CreditGlobalRestrictions,
    CreditPlan,
    Currency,
    DeliveryBucket,
    DeliveryCalendar,
    DeliveryOption,
    Dimensions,
    DynamicDeliveryServiceInfo,
    DynamicWarehouseAndDeliveryServiceInfo,
    DynamicWarehouseInfo,
    DynamicWarehouseToWarehouseInfo,
    DynamicWarehousesPriorityInRegion,
    MarketSku,
    NewPickupBucket,
    NewPickupOption,
    Offer,
    OfferDeliveryInfo,
    Outlet,
    OutletDeliveryOption,
    Payment,
    PaymentRegionalGroup,
    PickupRegionGroup,
    Region,
    RegionalDelivery,
    Shop,
    ShopPaymentMethods,
    Tax,
)

MODEL_ID = 1
CATEGORY_ID = 1

MSK_RIDS = 213
WAREHOUSE_ID = 145
DELIVERY_SERVICE_ID = 102

DEFAULT_DELIVERY_BUCKET_ID = 1001
DEFAULT_POST_BUCKET_ID = 1111

BUCKET_ID_BASE = 1000
BUCKET_DC_ID_BASE = 6000

VIRTUAL_SHOP_ID = 10000

PRIME_REQUEST = (
    'place=prime'
    '&rids={rid}'
    '&hid={category}'
    '&pp=18'
    '&rgb={color}'
    '&allow-collapsing=0'
    '&numdoc=100'
    '&show-preorder=1'
    '&offerid={waremd5_list}'
    '&show-credits={show_credits_cgi}'
)

SKU_OFFERS_REQUEST = (
    'place=sku_offers'
    '&rids={rid}'
    '&hid={category}'
    '&pp=18'
    '&rgb=blue'
    '&allow-collapsing=0'
    '&numdoc=100'
    '&show-preorder=1'
    '&market-sku={msku_list}'
    '&show-credits=1'
)

# Credit plans
GLOBAL_RESTRICTIONS = CreditGlobalRestrictions(min_price=3500, max_price=250000, suppliers_blacklist=[10])
GLOBAL_RESTRICTIONS_FRAGMENT = GLOBAL_RESTRICTIONS.to_json(price_as_string=True)
TINKOFF_BANK_PLAN = CreditPlan(
    plan_id='0E966DEBAA73ABD8379FA316F8326B8F',
    bank='Тинькофф',
    term=24,
    rate=12.5,
    initial_payment_percent=0,
    min_price=10000,
    max_price=200000,
    is_third_party_allowed=False,
)
ALFA_BANK_PLAN = CreditPlan(
    plan_id='0E966DEBAA73ABD8379FA316F8326B8C',
    bank='Альфа-Банк',
    term=24,
    rate=13,
    initial_payment_percent=0,
    min_price=10000,
    max_price=200000,
)
RAIFFAIZEN_BANK_PLAN = CreditPlan(
    plan_id='0E966DEBAA73ABD8379FA316F8326B8D',
    bank='Райффайзен-Банк',
    term=24,
    rate=10.0,
    initial_payment_percent=0,
    min_price=100000,
    max_price=200000,
    suppliers_blacklist=[11],
)
CREDIT_PLANS = [TINKOFF_BANK_PLAN, ALFA_BANK_PLAN, RAIFFAIZEN_BANK_PLAN]
CREDIT_PLANS_FRAGMENT = [plan.to_json(price_as_string=True) for plan in CREDIT_PLANS]

OFFER_1P = BlueOffer(price=11000, offerid='Shop1_sku25', waremd5='Sku25Price11k-vm1Goleg', feedid=1)
OFFER_3P = BlueOffer(price=11000, offerid='Shop3_sku26', waremd5='Sku26Price11k-vm1Goleg', feedid=3)

OFFER_DROPSHIP = BlueOffer(price=11100, offerid='Shop4_sku1', waremd5='SkuDROPSHIP---vm1Goleg', feedid=4)

OFFER_CROSSDOCK = BlueOffer(price=11200, offerid='Shop5_sku1', waremd5='SkuCROSSDOCK--vm1Goleg', feedid=5)

OFFER_CLICK_AND_COLLECT = BlueOffer(price=11300, offerid='Shop6_sku1', waremd5='SkuCLICK_AND_COLLECTeg', feedid=6)

OFFER_DSBS = Offer(
    title="DSBS Offer",
    hid=CATEGORY_ID,
    hyperid=MODEL_ID,
    price=10500,
    feedid=7,
    fesh=7,
    business_id=7,
    sku=30,
    cpa=Offer.CPA_REAL,
    waremd5='SkuDSBS-------------eg',
)

OFFER_WHITE = Offer(
    title="White Offer",
    hid=CATEGORY_ID,
    hyperid=MODEL_ID,
    price=700,
    feedid=8,
    fesh=8,
    business_id=8,
    sku=31,
    waremd5='SkuWhite------------eg',
    delivery_info=OfferDeliveryInfo(pickup_buckets=[BucketInfo(bucket_id=1313)]),
)


def create_white_offer_from_supplier(fesh):
    return Offer(
        title="White Offer",
        hid=CATEGORY_ID,
        hyperid=MODEL_ID,
        price=130501,
        feedid=fesh,
        fesh=fesh,
        business_id=fesh,
        sku=31,
        waremd5='SkuWhite{}----------eg'.format(fesh),
    )


OFFER_DSBS_PROHIBITED_FOR_CREDITS = create_white_offer_from_supplier(fesh=10)

OFFER_DSBS_PROHIBITED_FOR_RAIF_PLAN = create_white_offer_from_supplier(fesh=11)

OFFER_DSBS_ALLOWED_FOR_RAIF_PLAN = create_white_offer_from_supplier(fesh=12)

# MSKUs
MSKU_1P = MarketSku(
    title="MSKU 1P",
    hid=CATEGORY_ID,
    hyperid=MODEL_ID,
    sku='25',
    delivery_buckets=[DEFAULT_DELIVERY_BUCKET_ID],
    # post_buckets=[DEFAULT_POST_BUCKET_ID],
    blue_offers=[OFFER_1P],
)
MSKU_3P = MarketSku(
    title="MSKU 3P",
    hid=CATEGORY_ID,
    hyperid=MODEL_ID,
    sku='26',
    delivery_buckets=[DEFAULT_DELIVERY_BUCKET_ID],
    # post_buckets=[DEFAULT_POST_BUCKET_ID],
    blue_offers=[OFFER_3P],
)

MSKU_DROPSHIP = MarketSku(
    title="MSKU DROPSHIP",
    hid=CATEGORY_ID,
    hyperid=MODEL_ID,
    sku='27',
    delivery_buckets=[DEFAULT_DELIVERY_BUCKET_ID],
    blue_offers=[OFFER_DROPSHIP],
)

MSKU_CROSSDOCK = MarketSku(
    title="MSKU CROSSDOCK",
    hid=CATEGORY_ID,
    hyperid=MODEL_ID,
    sku='28',
    delivery_buckets=[DEFAULT_DELIVERY_BUCKET_ID],
    blue_offers=[OFFER_CROSSDOCK],
)

MSKU_CLICK_AND_COLLECT = MarketSku(
    title="MSKU CLICK_AND_COLLECT",
    hid=CATEGORY_ID,
    hyperid=MODEL_ID,
    sku='29',
    delivery_buckets=[DEFAULT_DELIVERY_BUCKET_ID],
    blue_offers=[OFFER_CLICK_AND_COLLECT],
)

MSKU_DSBS = MarketSku(
    title="MSKU DSBS",
    hid=CATEGORY_ID,
    hyperid=MODEL_ID,
    sku='30',
    delivery_buckets=[DEFAULT_DELIVERY_BUCKET_ID],
)


class T(TestCase):
    @staticmethod
    def __create_delivery_options(prepay_allowed):
        methods = [Payment.PT_CASH_ON_DELIVERY, Payment.PT_CARD_ON_DELIVERY]
        if prepay_allowed:
            methods += [Payment.PT_YANDEX]

        return RegionalDelivery(
            rid=MSK_RIDS,
            options=[DeliveryOption(day_from=5, day_to=25, shop_delivery_price=5)],
            payment_methods=methods,
        )

    @staticmethod
    def __create_delivery_bucket(id, fesh, prepay_allowed):
        return DeliveryBucket(
            bucket_id=id,
            dc_bucket_id=BUCKET_DC_ID_BASE + (id - BUCKET_ID_BASE),
            fesh=fesh,
            carriers=[DELIVERY_SERVICE_ID],
            regional_options=[T.__create_delivery_options(prepay_allowed)],
            delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
        )

    @classmethod
    def prepare(cls):
        cls.settings.report_subrole = 'blue-main'
        cls.index.regiontree += [Region(rid=MSK_RIDS, name="Москва")]

    @classmethod
    def prepare_credit_plans(cls):
        cls.index.credit_plans_container.global_restrictions = GLOBAL_RESTRICTIONS
        cls.index.credit_plans_container.credit_plans = CREDIT_PLANS

    @classmethod
    def prepare_lms(cls):
        cls.settings.lms_autogenerate = False
        cls.dynamic.lms += [
            DynamicWarehouseInfo(id=WAREHOUSE_ID, home_region=MSK_RIDS),
            DynamicWarehouseInfo(id=999, home_region=MSK_RIDS),
            DynamicDeliveryServiceInfo(id=DELIVERY_SERVICE_ID),
            DynamicWarehouseToWarehouseInfo(warehouse_from=999, warehouse_to=WAREHOUSE_ID),
            DynamicWarehouseToWarehouseInfo(warehouse_from=WAREHOUSE_ID, warehouse_to=WAREHOUSE_ID),
            DynamicWarehouseAndDeliveryServiceInfo(warehouse_id=WAREHOUSE_ID, delivery_service_id=DELIVERY_SERVICE_ID),
            DynamicWarehouseAndDeliveryServiceInfo(warehouse_id=999, delivery_service_id=DELIVERY_SERVICE_ID),
            DynamicWarehousesPriorityInRegion(
                region=MSK_RIDS,
                warehouses=[
                    WAREHOUSE_ID,
                    999,
                ],
            ),
        ]

    @classmethod
    def prepare_delivery(cls):
        cls.index.shops += [
            Shop(
                fesh=VIRTUAL_SHOP_ID,
                datafeed_id=VIRTUAL_SHOP_ID,
                priority_region=213,
                name='Виртуальный магазин',
                fulfillment_virtual=True,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
            ),
            Shop(
                fesh=1,
                datafeed_id=1,
                priority_region=MSK_RIDS,
                regions=[MSK_RIDS],
                name="Тестовый поставщик",
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                supplier_type=Shop.FIRST_PARTY,
                blue=Shop.BLUE_REAL,
                cpa=Shop.CPA_REAL,
                warehouse_id=WAREHOUSE_ID,
                fulfillment_program=True,
            ),
            Shop(
                fesh=3,
                datafeed_id=3,
                priority_region=MSK_RIDS,
                regions=[MSK_RIDS],
                name="Магазин 3P",
                supplier_type=Shop.THIRD_PARTY,
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                blue=Shop.BLUE_REAL,
                cpa=Shop.CPA_REAL,
                warehouse_id=WAREHOUSE_ID,
                fulfillment_program=True,
            ),
            Shop(
                fesh=4,
                datafeed_id=4,
                priority_region=MSK_RIDS,
                regions=[MSK_RIDS],
                name="Магазин Dropship",
                supplier_type=Shop.THIRD_PARTY,
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                blue=Shop.BLUE_REAL,
                cpa=Shop.CPA_REAL,
                warehouse_id=WAREHOUSE_ID,
                fulfillment_program=False,
            ),
            Shop(
                fesh=5,
                datafeed_id=5,
                priority_region=MSK_RIDS,
                regions=[MSK_RIDS],
                name="Магазин Crossdock",
                supplier_type=Shop.THIRD_PARTY,
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                blue=Shop.BLUE_REAL,
                cpa=Shop.CPA_REAL,
                warehouse_id=999,
                fulfillment_program=True,
                direct_shipping=False,
            ),
            Shop(
                fesh=6,
                datafeed_id=6,
                priority_region=MSK_RIDS,
                regions=[MSK_RIDS],
                name="Магазин C&C",
                supplier_type=Shop.THIRD_PARTY,
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                blue=Shop.BLUE_REAL,
                cpa=Shop.CPA_REAL,
                warehouse_id=WAREHOUSE_ID,
                fulfillment_program=False,
                ignore_stocks=True,
                click_and_collect=True,
            ),
            Shop(fesh=7, datafeed_id=7, business_fesh=7, name="Магазин DSBS", regions=[MSK_RIDS], cpa=Shop.CPA_REAL),
            Shop(fesh=8, datafeed_id=8, business_fesh=8, name="Магазин DSBS", regions=[MSK_RIDS]),
            Shop(
                fesh=10,
                datafeed_id=10,
                business_fesh=9,
                name="Магазин DSBS, запрещенный в глобальных ограничениях",
                regions=[MSK_RIDS],
            ),
            Shop(
                fesh=11,
                datafeed_id=11,
                business_fesh=10,
                name="Магазин DSBS, запрещенный в ограничениях райфа",
                regions=[MSK_RIDS],
            ),
            Shop(
                fesh=12,
                datafeed_id=12,
                business_fesh=11,
                name="Магазин DSBS, разрешенный в ограничениях райфа",
                regions=[MSK_RIDS],
            ),
        ]
        cls.index.shipment_service_calendars += [
            DeliveryCalendar(fesh=1, calendar_id=102),
            DeliveryCalendar(fesh=3, calendar_id=102),
        ]
        cls.index.delivery_buckets += [
            T.__create_delivery_bucket(id=DEFAULT_DELIVERY_BUCKET_ID, fesh=1, prepay_allowed=True)
        ]
        cls.index.outlets += [
            Outlet(
                point_id=1,
                dimensions=Dimensions(width=100, height=90, length=80),
                region=213,
                delivery_option=OutletDeliveryOption(day_from=0, day_to=1, order_before=2, work_in_holiday=False),
                working_days=[i for i in range(10)],
                delivery_service_id=101,
                point_type=Outlet.FOR_PICKUP,
            )
        ]

        cls.index.new_pickup_buckets += [
            NewPickupBucket(
                bucket_id=1313,
                region_groups=[
                    PickupRegionGroup(options=[NewPickupOption(price=220, day_from=1, day_to=3)], outlets=[1])
                ],
            ),
        ]

        cls.index.shops_payment_methods += [
            ShopPaymentMethods(
                fesh=fesh,
                payment_groups=[
                    PaymentRegionalGroup(
                        included_regions=[MSK_RIDS],
                        payment_methods=[
                            Payment.PT_CASH_ON_DELIVERY,
                            Payment.PT_CARD_ON_DELIVERY,
                            Payment.PT_PREPAYMENT_CARD,
                        ],
                    ),
                ],
            )
            for fesh in [7, 10, 11, 12, VIRTUAL_SHOP_ID]
        ]

    @classmethod
    def prepare_blue_offers(cls):
        cls.index.mskus += [MSKU_1P, MSKU_3P, MSKU_DROPSHIP, MSKU_CROSSDOCK, MSKU_CLICK_AND_COLLECT, MSKU_DSBS]

    @classmethod
    def prepare_offers(cls):
        cls.index.offers += [
            OFFER_DSBS,
            OFFER_WHITE,
            OFFER_DSBS_PROHIBITED_FOR_CREDITS,
            OFFER_DSBS_PROHIBITED_FOR_RAIF_PLAN,
            OFFER_DSBS_ALLOWED_FOR_RAIF_PLAN,
        ]

    def test_1p_restriction(self):
        response = self.report.request_json(
            PRIME_REQUEST.format(
                rid=MSK_RIDS,
                category=CATEGORY_ID,
                color='blue',
                waremd5_list=','.join([offer.waremd5 for offer in [OFFER_1P, OFFER_3P]]),
                show_credits_cgi=1,
            )
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "wareId": "Sku25Price11k-vm1Goleg",
                        "supplier": {"type": "1"},
                        "creditInfo": {"bestOptionId": "0E966DEBAA73ABD8379FA316F8326B8F"},  # Тинькофф
                    },
                    {
                        "entity": "offer",
                        "wareId": "Sku26Price11k-vm1Goleg",
                        "supplier": {"type": "3"},
                        "creditInfo": {
                            "bestOptionId": "0E966DEBAA73ABD8379FA316F8326B8C"  # Для 3P при той же цене и категории выбираем другой банк - Альфа. Для него 1P разрешены по умолчанию (нет явного запрета в json)  # noqa
                        },
                    },
                ]
            },
        )
        # Проверяем place=sku
        response = self.report.request_json(
            SKU_OFFERS_REQUEST.format(
                rid=MSK_RIDS,
                category=CATEGORY_ID,
                rgb="blue",
                msku_list=','.join([msku.sku for msku in [MSKU_1P, MSKU_3P]]),
            )
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "sku",
                        "id": "25",
                        "offers": {
                            "items": [
                                {
                                    "supplier": {"type": "1"},
                                    "creditInfo": {"bestOptionId": "0E966DEBAA73ABD8379FA316F8326B8F"},
                                }
                            ]
                        },
                    },
                    {
                        "entity": "sku",
                        "id": "26",
                        "offers": {
                            "items": [
                                {
                                    "supplier": {"type": "3"},
                                    "creditInfo": {"bestOptionId": "0E966DEBAA73ABD8379FA316F8326B8C"},
                                }
                            ]
                        },
                    },
                ]
            },
        )

    def test_3p_restriction(self):
        """
        Проверяется, что кредиты не отображаются только для C&C офферов.
        """
        for waremd5, msku, show_credit in [
            (
                OFFER_DROPSHIP.waremd5,
                MSKU_DROPSHIP.sku,
                True,
            ),
            (OFFER_CROSSDOCK.waremd5, MSKU_CROSSDOCK.sku, True),
            (OFFER_CLICK_AND_COLLECT.waremd5, MSKU_CLICK_AND_COLLECT.sku, False),
            (OFFER_DSBS.waremd5, MSKU_DSBS.sku, True),
        ]:
            response = self.report.request_json(
                PRIME_REQUEST.format(
                    rid=MSK_RIDS, category=CATEGORY_ID, color='blue', waremd5_list=waremd5, show_credits_cgi=1
                )
            )
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            "entity": "offer",
                            "wareId": waremd5,
                            "creditInfo": {"bestOptionId": "0E966DEBAA73ABD8379FA316F8326B8C"}
                            if show_credit
                            else Absent(),
                        }
                    ]
                },
            )
            # Проверяем place=sku_offers
            response = self.report.request_json(
                SKU_OFFERS_REQUEST.format(rid=MSK_RIDS, category=CATEGORY_ID, rgb="blue", msku_list=msku)
            )
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            "entity": "sku",
                            "id": msku,
                            "offers": {
                                "items": [
                                    {
                                        "creditInfo": {"bestOptionId": "0E966DEBAA73ABD8379FA316F8326B8C"}
                                        if show_credit
                                        else Absent()
                                    }
                                ]
                            },
                        },
                    ]
                },
            )

    @classmethod
    def prepare_reject_credit_when_at_supplier_warehouse(cls):
        cls.index.shops += [
            Shop(
                fesh=100,
                datafeed_id=100,
                priority_region=MSK_RIDS,
                regions=[MSK_RIDS],
                name="Магазин DropshipWD",
                supplier_type=Shop.THIRD_PARTY,
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                blue=Shop.BLUE_REAL,
                cpa=Shop.CPA_REAL,
                warehouse_id=WAREHOUSE_ID,
                direct_shipping=False,
                fulfillment_program=False,
            )
        ]

        OFFER_DROPSHIPWD = BlueOffer(price=11100, offerid='Shop100_sku1', waremd5='SkuDROPSHIPWD-vm1Goleg', feedid=100)

        cls.index.mskus += [
            MarketSku(
                title="MSKU DROPSHIP_WITHOUT_DIRECT_SHIPPING",
                hid=CATEGORY_ID,
                hyperid=MODEL_ID,
                sku='127',
                delivery_buckets=[10010],
                blue_offers=[OFFER_DROPSHIPWD],
            )
        ]

        cls.index.shipment_service_calendars += [
            DeliveryCalendar(fesh=100, calendar_id=103),
        ]

        cls.index.delivery_buckets += [T.__create_delivery_bucket(id=10010, fesh=100, prepay_allowed=True)]

    def test_white_offer(self):
        """
        Проверяется корректная работа с белыми офферами на белом
        """
        response = self.report.request_json(
            PRIME_REQUEST.format(
                rid=MSK_RIDS, category=CATEGORY_ID, color='', waremd5_list=OFFER_WHITE.waremd5, show_credits_cgi=1
            )
            + "&rearr-factors=show_credits_on_white=1"
        )
        self.assertFragmentIn(
            response, {"results": [{"entity": "offer", "wareId": OFFER_WHITE.waremd5, "creditInfo": Absent()}]}
        )

    def test_suppliers_restrictions(self):
        """
        Проверяем ограничения кредитов по спискам поставщиков
        """
        # поставщик в черном списке глобальных ограничений на кредиты
        response = self.report.request_json(
            PRIME_REQUEST.format(
                rid=MSK_RIDS,
                category=CATEGORY_ID,
                color='',
                waremd5_list=OFFER_DSBS_PROHIBITED_FOR_CREDITS.waremd5,
                show_credits_cgi=1,
            )
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "wareId": OFFER_DSBS_PROHIBITED_FOR_CREDITS.waremd5,
                        "creditDenial": {
                            "reason": "BLACKLIST_SUPPLIER",
                            "blacklistOffers": [
                                OFFER_DSBS_PROHIBITED_FOR_CREDITS.waremd5,
                            ],
                        },
                    }
                ]
            },
        )
        # поставщик в черном списке у кредитного плана Райфа, выигрывает другой план
        response = self.report.request_json(
            PRIME_REQUEST.format(
                rid=MSK_RIDS,
                category=CATEGORY_ID,
                color='',
                waremd5_list=OFFER_DSBS_PROHIBITED_FOR_RAIF_PLAN.waremd5,
                show_credits_cgi=1,
            )
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "wareId": OFFER_DSBS_PROHIBITED_FOR_RAIF_PLAN.waremd5,
                        "creditInfo": {"bestOptionId": "0E966DEBAA73ABD8379FA316F8326B8C"},
                    }
                ]
            },
        )
        # у таких же офферов, разрешенных глобальными ограничениями и планом Райфа - выигрывает Райф
        response = self.report.request_json(
            PRIME_REQUEST.format(
                rid=MSK_RIDS,
                category=CATEGORY_ID,
                color='',
                waremd5_list=OFFER_DSBS_ALLOWED_FOR_RAIF_PLAN.waremd5,
                show_credits_cgi=1,
            )
        )
        self.assertFragmentIn(
            response,
            {
                "results": [
                    {
                        "entity": "offer",
                        "wareId": OFFER_DSBS_ALLOWED_FOR_RAIF_PLAN.waremd5,
                        "creditInfo": {"bestOptionId": "0E966DEBAA73ABD8379FA316F8326B8D"},
                    }
                ]
            },
        )


if __name__ == '__main__':
    main()
