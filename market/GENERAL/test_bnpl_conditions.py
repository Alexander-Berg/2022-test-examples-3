#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

from core.matcher import Absent

from core.testcase import TestCase, main

from core.types import (
    BlueOffer,
    BnplConditionsSettings,
    BnplDenialReason,
    Currency,
    DeliveryBucket,
    DeliveryCalendar,
    DeliveryOption,
    DynamicDeliveryServiceInfo,
    DynamicWarehouseAndDeliveryServiceInfo,
    DynamicWarehouseInfo,
    HyperCategory,
    MarketSku,
    Offer,
    Payment,
    PaymentRegionalGroup,
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
BUCKET_ID_BASE = 1000
BUCKET_DC_ID_BASE = 6000
SPECIAL_WHITE_SHOP = 12345
SIMPLE_WHITE_SHOP = 54321
WHITE_SHOP_CASH_ONLY = 32154
WHITE_SHOP_WITH_PREPAYMENT = 45123
VIRTUAL_SHOP_ID = 100

PRIME_REQUEST = 'place=prime' '&rids={rid}' '&pp=18' '&offerid={waremd5}' '&rearr-factors=enable_bnpl=1'
SKU_OFFERS_REQUEST = 'place=sku_offers' '&rids={rid}' '&pp=18' '&market-sku={msku}' '&rearr-factors=enable_bnpl=1'
# Offers
ALLOWED_OFFER_1P = BlueOffer(price=11000, offerid='Shop1_sku25', waremd5='Sku25Price11k-vm1Goleg', feedid=1)

ALLOWED_OFFER_3P = BlueOffer(price=11000, offerid='Shop3_sku26', waremd5='Sku26Price11k-vm1Goleg', feedid=3)

ALLOWED_OFFER_DROPSHIP = BlueOffer(price=11100, offerid='Shop4_sku1', waremd5='SkuDROPSHIP---vm1Goleg', feedid=4)

NOT_ALLOWED_OFFER_1P = BlueOffer(price=11000, offerid='Shop1_sku27', waremd5='SkuNotAllowed-1p-Goleg', feedid=1)

NOT_ALLOWED_OFFER_3P = BlueOffer(price=11000, offerid='Shop3_sku28', waremd5='SkuNotAllowed-3p-Goleg', feedid=3)

NOT_ALLOWED_OFFER_DROPSHIP = BlueOffer(price=11100, offerid='Shop4_sku2', waremd5='DROPSHIP-NotAllowed--g', feedid=4)

NOT_ALLOWED_OFFER_DROPSHIP_BLACKLIST_OVER_WHITELIST = BlueOffer(
    price=11100, offerid='Shop4_sku2', waremd5=Offer.generate_waremd5('DROPSHIP-NotAllowedB-g'), feedid=4
)

NOT_ALLOWED_OFFER_DROPSHIP_NOT_IN_WHITELIST_WHEN_WHITELIST_MODE_ENABLED = BlueOffer(
    price=11100, offerid='Shop4_sku2', waremd5=Offer.generate_waremd5('DROPSHIP-NotAllowedNWg'), feedid=4
)

NOT_ALLOWED_OFFER_DROPSHIP_ABOVE_PRICE_RESTRICTION = BlueOffer(
    price=22200, offerid='Shop4_sku2', waremd5=Offer.generate_waremd5('DROPSHIP-NotAllowedAP'), feedid=4
)

NOT_ALLOWED_OFFER_DROPSHIP_BELOW_PRICE_RESTRICTION = BlueOffer(
    price=1, offerid='Shop4_sku2', waremd5=Offer.generate_waremd5('DROPSHIP-NotAllowedBP'), feedid=4
)

NOT_ALLOWED_OFFER_DROPSHIP_FROM_SHOP_IN_BLACKLIST = BlueOffer(
    price=11100, offerid='Shop5_sku2', waremd5=Offer.generate_waremd5('DROPSHIP-NotAllowedBS'), feedid=5
)

ALLOWED_WHITE_OFFER = Offer(
    price=3000,
    fesh=SPECIAL_WHITE_SHOP,
    sku='25',
    hyperid=1,
    waremd5=Offer.generate_waremd5("SPECIAL_WHITE_SHOP"),
    delivery_buckets=[DEFAULT_DELIVERY_BUCKET_ID + SPECIAL_WHITE_SHOP - 1],
)

NOT_ALLOWED_WHITE_OFFER = Offer(
    price=3000,
    fesh=SIMPLE_WHITE_SHOP,
    sku='25',
    hyperid=1,
    waremd5=Offer.generate_waremd5("SIMPLE_WHITE_SHOP"),
    delivery_buckets=[DEFAULT_DELIVERY_BUCKET_ID + SIMPLE_WHITE_SHOP - 1],
)

CASH_ONLY_WHITE_OFFER = Offer(
    price=3000,
    fesh=WHITE_SHOP_CASH_ONLY,
    sku='25',
    hyperid=1,
    waremd5=Offer.generate_waremd5("CASH_ONLY_WHITE_OFFER"),
    delivery_buckets=[DEFAULT_DELIVERY_BUCKET_ID + WHITE_SHOP_CASH_ONLY - 1],
)

WITH_PREPAYMENT_WHITE_OFFER = Offer(
    price=3000,
    fesh=WHITE_SHOP_WITH_PREPAYMENT,
    sku='25',
    hyperid=1,
    waremd5=Offer.generate_waremd5("WITH_PREPAYMENT_WHITE_OFFER"),
    delivery_buckets=[DEFAULT_DELIVERY_BUCKET_ID + WHITE_SHOP_WITH_PREPAYMENT - 1],
)

# MSKUs
ALLOWED_MSKU_1P = MarketSku(
    title="ALLOWED MSKU 1P",
    hid=1,
    hyperid=1,
    sku='25',
    delivery_buckets=[DEFAULT_DELIVERY_BUCKET_ID],
    blue_offers=[ALLOWED_OFFER_1P],
)
ALLOWED_MSKU_3P = MarketSku(
    title="ALLOWED MSKU 3P",
    hid=2,
    hyperid=2,
    sku='26',
    delivery_buckets=[DEFAULT_DELIVERY_BUCKET_ID],
    blue_offers=[ALLOWED_OFFER_3P],
)

ALLOWED_MSKU_DROPSHIP = MarketSku(
    title="ALLOWED MSKU DROPSHIP",
    hid=3,
    hyperid=3,
    sku='27',
    delivery_buckets=[DEFAULT_DELIVERY_BUCKET_ID],
    blue_offers=[ALLOWED_OFFER_DROPSHIP],
)

NOT_ALLOWED_MSKU_1P = MarketSku(
    title="ALLOWED MSKU 1P",
    hid=4,
    hyperid=4,
    sku='28',
    delivery_buckets=[DEFAULT_DELIVERY_BUCKET_ID],
    blue_offers=[NOT_ALLOWED_OFFER_1P],
)

NOT_ALLOWED_MSKU_3P = MarketSku(
    title="ALLOWED MSKU 3P",
    hid=5,
    hyperid=5,
    sku='29',
    delivery_buckets=[DEFAULT_DELIVERY_BUCKET_ID],
    blue_offers=[NOT_ALLOWED_OFFER_3P],
)

NOT_ALLOWED_MSKU_DROPSHIP = MarketSku(
    title="ALLOWED MSKU DROPSHIP",
    hid=6,
    hyperid=6,
    sku='30',
    delivery_buckets=[DEFAULT_DELIVERY_BUCKET_ID],
    blue_offers=[NOT_ALLOWED_OFFER_DROPSHIP],
)

NOT_ALLOWED_MSKU_DROPSHIP_BLACKLIST_OVER_WHITELIST = MarketSku(
    title="NOT ALLOWED MSKU DROPSHIP BLACKLIST OVER WHITELIST",
    hid=8,
    hyperid=8,
    sku='31',
    delivery_buckets=[DEFAULT_DELIVERY_BUCKET_ID],
    blue_offers=[NOT_ALLOWED_OFFER_DROPSHIP_BLACKLIST_OVER_WHITELIST],
)

NOT_ALLOWED_MSKU_DROPSHIP_NOT_IN_WHITELIST_WHEN_WHITELIST_MODE_ENABLED = MarketSku(
    title="NOT ALLOWED MSKU DROPSHIP NOT IN WHITELIST WHEN WHITELIST MODE ENABLED",
    hid=9,
    hyperid=9,
    sku='32',
    delivery_buckets=[DEFAULT_DELIVERY_BUCKET_ID],
    blue_offers=[NOT_ALLOWED_OFFER_DROPSHIP_NOT_IN_WHITELIST_WHEN_WHITELIST_MODE_ENABLED],
)

NOT_ALLOWED_MSKU_DROPSHIP_ABOVE_PRICE_RESTRICTION = MarketSku(
    title="NOT ALLOWED MSKU DROPSHIP ABOVE PRICE RESTRICTION",
    hid=3,
    hyperid=3,
    sku='33',
    delivery_buckets=[DEFAULT_DELIVERY_BUCKET_ID],
    blue_offers=[NOT_ALLOWED_OFFER_DROPSHIP_ABOVE_PRICE_RESTRICTION],
)

NOT_ALLOWED_MSKU_DROPSHIP_BELOW_PRICE_RESTRICTION = MarketSku(
    title="NOT ALLOWED MSKU DROPSHIP BELOW PRICE RESTRICTION",
    hid=3,
    hyperid=3,
    sku='34',
    delivery_buckets=[DEFAULT_DELIVERY_BUCKET_ID],
    blue_offers=[NOT_ALLOWED_OFFER_DROPSHIP_BELOW_PRICE_RESTRICTION],
)

NOT_ALLOWED_MSKU_DROPSHIP_FROM_SHOP_IN_BLACKLIST = MarketSku(
    title="NOT_ALLOWED_MSKU_DROPSHIP_FROM_SHOP_IN_BLACKLIST",
    hid=3,
    hyperid=3,
    sku='35',
    delivery_buckets=[DEFAULT_DELIVERY_BUCKET_ID],
    blue_offers=[NOT_ALLOWED_OFFER_DROPSHIP_FROM_SHOP_IN_BLACKLIST],
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
        cls.index.bnpl_conditions.settings = BnplConditionsSettings(min_price=20, max_price=20000)
        cls.index.bnpl_conditions.white_hids += [1, 2, 3]
        cls.index.bnpl_conditions.black_hids += [4, 5, 6]
        cls.index.bnpl_conditions.black_shop_ids += [5]
        cls.index.bnpl_conditions.dsbs_suppliers_white_list += [
            SPECIAL_WHITE_SHOP,
            WHITE_SHOP_WITH_PREPAYMENT,
            WHITE_SHOP_CASH_ONLY,
        ]

        cls.index.offers += [
            ALLOWED_WHITE_OFFER,
            NOT_ALLOWED_WHITE_OFFER,
            CASH_ONLY_WHITE_OFFER,
            WITH_PREPAYMENT_WHITE_OFFER,
        ]

        cls.index.hypertree += [HyperCategory(hid=6, children=[HyperCategory(hid=7, children=[HyperCategory(hid=8)])])]

        cls.settings.lms_autogenerate = False
        cls.dynamic.lms += [
            DynamicWarehouseInfo(id=WAREHOUSE_ID, home_region=MSK_RIDS),
            DynamicDeliveryServiceInfo(id=DELIVERY_SERVICE_ID),
            DynamicWarehouseAndDeliveryServiceInfo(warehouse_id=WAREHOUSE_ID, delivery_service_id=DELIVERY_SERVICE_ID),
        ]
        for fesh in [
            VIRTUAL_SHOP_ID,
            SIMPLE_WHITE_SHOP,
            SPECIAL_WHITE_SHOP,
            WHITE_SHOP_WITH_PREPAYMENT,
            WHITE_SHOP_CASH_ONLY,
        ]:
            cls.index.shops_payment_methods += [
                ShopPaymentMethods(
                    fesh=fesh,
                    payment_groups=[
                        PaymentRegionalGroup(
                            included_regions=[213],
                            payment_methods=[
                                Payment.PT_PREPAYMENT_CARD
                                if fesh != WHITE_SHOP_CASH_ONLY
                                else Payment.PT_CASH_ON_DELIVERY
                            ],
                        ),
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
                name="Магазин Dropship - в черном списке конфига bnpl",
                supplier_type=Shop.THIRD_PARTY,
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                blue=Shop.BLUE_REAL,
                cpa=Shop.CPA_REAL,
                warehouse_id=WAREHOUSE_ID,
                fulfillment_program=False,
            ),
            Shop(
                fesh=SPECIAL_WHITE_SHOP,
                datafeed_id=6,
                priority_region=MSK_RIDS,
                regions=[MSK_RIDS],
                name='Белый dsbs магазин, для которого разрешена bnpl',
            ),
            Shop(
                fesh=SIMPLE_WHITE_SHOP,
                datafeed_id=7,
                priority_region=MSK_RIDS,
                regions=[MSK_RIDS],
                name='Обычный белый магазин',
            ),
            Shop(
                fesh=WHITE_SHOP_CASH_ONLY,
                datafeed_id=8,
                priority_region=MSK_RIDS,
                regions=[MSK_RIDS],
                name='Белый магазин с оплатой наличкой',
            ),
            Shop(
                fesh=WHITE_SHOP_WITH_PREPAYMENT,
                datafeed_id=9,
                priority_region=MSK_RIDS,
                regions=[MSK_RIDS],
                name='Белый магазин с предоплатой',
            ),
        ]
        cls.index.shipment_service_calendars += [
            DeliveryCalendar(fesh=1, calendar_id=102),
            DeliveryCalendar(fesh=3, calendar_id=102),
        ]
        for fesh in (1, SPECIAL_WHITE_SHOP, SIMPLE_WHITE_SHOP, WHITE_SHOP_CASH_ONLY, WHITE_SHOP_WITH_PREPAYMENT):
            cls.index.delivery_buckets += [
                # способы оплаты берутся из виртуального магазина, здесь специально в бакетах не делаем предоплату,
                # чтобы проверить это
                T.__create_delivery_bucket(id=(DEFAULT_DELIVERY_BUCKET_ID + fesh - 1), fesh=fesh, prepay_allowed=False),
            ]

    @classmethod
    def prepare_blue_offers(cls):
        cls.index.mskus += [
            ALLOWED_MSKU_1P,
            ALLOWED_MSKU_3P,
            ALLOWED_MSKU_DROPSHIP,
            NOT_ALLOWED_MSKU_1P,
            NOT_ALLOWED_MSKU_3P,
            NOT_ALLOWED_MSKU_DROPSHIP,
            NOT_ALLOWED_MSKU_DROPSHIP_BLACKLIST_OVER_WHITELIST,
            NOT_ALLOWED_MSKU_DROPSHIP_NOT_IN_WHITELIST_WHEN_WHITELIST_MODE_ENABLED,
            NOT_ALLOWED_MSKU_DROPSHIP_ABOVE_PRICE_RESTRICTION,
            NOT_ALLOWED_MSKU_DROPSHIP_BELOW_PRICE_RESTRICTION,
            NOT_ALLOWED_MSKU_DROPSHIP_FROM_SHOP_IN_BLACKLIST,
        ]

    def test_bnpl_info(self):
        '''
        Проверяем, что информация о рассрочке (bnpl) выдается корректно для офферов, которые подходят по условиям
        Оффер откидывается, если его хид находится в черном списке
        Если включен режим белого списка, то требуется чтобы хид был в этом списке
        Режим белого списка включен по умолчанию
        '''
        # Проверяем place=prime
        for offer, bnpl_allowed, reason in [
            (ALLOWED_OFFER_1P, True, BnplDenialReason.CDR_NONE),
            (ALLOWED_OFFER_3P, True, BnplDenialReason.CDR_NONE),
            (ALLOWED_OFFER_DROPSHIP, True, BnplDenialReason.CDR_NONE),
            (NOT_ALLOWED_OFFER_1P, False, BnplDenialReason.CDR_BLACKLIST_CATEGORY),
            (NOT_ALLOWED_OFFER_3P, False, BnplDenialReason.CDR_BLACKLIST_CATEGORY),
            (NOT_ALLOWED_OFFER_DROPSHIP, False, BnplDenialReason.CDR_BLACKLIST_CATEGORY),
            (NOT_ALLOWED_OFFER_DROPSHIP_BLACKLIST_OVER_WHITELIST, False, BnplDenialReason.CDR_BLACKLIST_CATEGORY),
            (
                NOT_ALLOWED_OFFER_DROPSHIP_NOT_IN_WHITELIST_WHEN_WHITELIST_MODE_ENABLED,
                False,
                BnplDenialReason.CDR_BLACKLIST_CATEGORY,
            ),
            (NOT_ALLOWED_OFFER_DROPSHIP_ABOVE_PRICE_RESTRICTION, False, BnplDenialReason.CDR_TOO_EXPENSIVE),
            (NOT_ALLOWED_OFFER_DROPSHIP_BELOW_PRICE_RESTRICTION, False, BnplDenialReason.CDR_TOO_CHEAP),
            (NOT_ALLOWED_OFFER_DROPSHIP_FROM_SHOP_IN_BLACKLIST, False, BnplDenialReason.CDR_BLACKLIST_SUPPLIER),
            (ALLOWED_WHITE_OFFER, True, BnplDenialReason.CDR_NONE),
            (NOT_ALLOWED_WHITE_OFFER, False, BnplDenialReason.CDR_BLACKLIST_SUPPLIER),
            (CASH_ONLY_WHITE_OFFER, False, BnplDenialReason.CDR_PREPAYMENT_UNAVAILABLE),
            (WITH_PREPAYMENT_WHITE_OFFER, True, BnplDenialReason.CDR_NONE),
        ]:
            response = self.report.request_json(PRIME_REQUEST.format(rid=MSK_RIDS, waremd5=offer.waremd5))
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            "entity": "offer",
                            "wareId": offer.waremd5,
                            "yandexBnplInfo": {
                                "enabled": bnpl_allowed,
                                "bnplDenial": {
                                    "reason": reason,
                                }
                                if not bnpl_allowed
                                else Absent(),
                            },
                        }
                    ]
                },
            )

        # Проверяем place=sku_offers
        for msku, bnpl_allowed, reason in [
            (ALLOWED_MSKU_1P, True, BnplDenialReason.CDR_NONE),
            (ALLOWED_MSKU_3P, True, BnplDenialReason.CDR_NONE),
            (ALLOWED_MSKU_DROPSHIP, True, BnplDenialReason.CDR_NONE),
            (NOT_ALLOWED_MSKU_1P, False, BnplDenialReason.CDR_BLACKLIST_CATEGORY),
            (NOT_ALLOWED_MSKU_3P, False, BnplDenialReason.CDR_BLACKLIST_CATEGORY),
            (NOT_ALLOWED_MSKU_DROPSHIP, False, BnplDenialReason.CDR_BLACKLIST_CATEGORY),
            (NOT_ALLOWED_MSKU_DROPSHIP_BLACKLIST_OVER_WHITELIST, False, BnplDenialReason.CDR_BLACKLIST_CATEGORY),
            (
                NOT_ALLOWED_MSKU_DROPSHIP_NOT_IN_WHITELIST_WHEN_WHITELIST_MODE_ENABLED,
                False,
                BnplDenialReason.CDR_BLACKLIST_CATEGORY,
            ),
            (NOT_ALLOWED_MSKU_DROPSHIP_ABOVE_PRICE_RESTRICTION, False, BnplDenialReason.CDR_TOO_EXPENSIVE),
            (NOT_ALLOWED_MSKU_DROPSHIP_BELOW_PRICE_RESTRICTION, False, BnplDenialReason.CDR_TOO_CHEAP),
            (NOT_ALLOWED_MSKU_DROPSHIP_FROM_SHOP_IN_BLACKLIST, False, BnplDenialReason.CDR_BLACKLIST_SUPPLIER),
        ]:
            response = self.report.request_json(SKU_OFFERS_REQUEST.format(rid=MSK_RIDS, msku=msku.sku))
            self.assertFragmentIn(
                response,
                {
                    "results": [
                        {
                            "entity": "sku",
                            "id": msku.sku,
                            "offers": {
                                "items": [
                                    {
                                        "yandexBnplInfo": {
                                            "enabled": bnpl_allowed,
                                            "bnplDenial": {
                                                "reason": reason,
                                                "threshold": {
                                                    "currency": "RUR",
                                                    "value": "20"
                                                    if reason == BnplDenialReason.CDR_TOO_CHEAP
                                                    else "20000",
                                                }
                                                if reason == BnplDenialReason.CDR_TOO_CHEAP
                                                or reason == BnplDenialReason.CDR_TOO_EXPENSIVE
                                                else Absent(),
                                            }
                                            if not bnpl_allowed
                                            else Absent(),
                                        }
                                    }
                                ]
                            },
                        }
                    ]
                },
            )


if __name__ == '__main__':
    main()
