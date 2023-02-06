#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

from core.matcher import Absent

from core.testcase import TestCase, main

from core.types import (
    BnplConditionsSettings,
    BnplDenialReason,
    DeliveryBucket,
    DeliveryOption,
    DynamicDeliveryServiceInfo,
    DynamicWarehouseAndDeliveryServiceInfo,
    DynamicWarehouseInfo,
    MarketSku,
    Offer,
    Payment,
    PaymentRegionalGroup,
    Region,
    RegionalDelivery,
    Shop,
    ShopPaymentMethods,
)


MSK_RIDS = 213
WAREHOUSE_ID = 145
DELIVERY_SERVICE_ID = 102
DEFAULT_DELIVERY_BUCKET_ID = 1001
BUCKET_ID_BASE = 1000
BUCKET_DC_ID_BASE = 6000
WHITE_SHOP = 12345
WHITE_SHOP_IN_BLACKLIST = 54321
WHITE_SHOP_WITHOUT_PREPAYMENT = 123454


PRIME_REQUEST = 'place=prime' '&rids={rid}' '&pp=18' '&offerid={waremd5}' '&rearr-factors=enable_bnpl=1'


# Offers
WHITE_OFFER = Offer(
    price=3000,
    fesh=WHITE_SHOP,
    sku='25',
    hyperid=1,
    waremd5=Offer.generate_waremd5("WHITE_SHOP"),
    delivery_buckets=[DEFAULT_DELIVERY_BUCKET_ID + WHITE_SHOP],
)

WHITE_OFFER_IN_BLACKLIST = Offer(
    price=3000,
    fesh=WHITE_SHOP_IN_BLACKLIST,
    sku='25',
    hyperid=1,
    waremd5=Offer.generate_waremd5("WHITE_OFFER_IN_BLACKLIST"),
    delivery_buckets=[DEFAULT_DELIVERY_BUCKET_ID + WHITE_SHOP_IN_BLACKLIST],
)

WHITE_OFFER_WITHOUT_PREPAYMENT = Offer(
    price=3000,
    fesh=WHITE_SHOP_WITHOUT_PREPAYMENT,
    sku='25',
    hyperid=1,
    waremd5=Offer.generate_waremd5("WHITE_OFFER_WITHOUT_PREPAYMENT"),
    delivery_buckets=[DEFAULT_DELIVERY_BUCKET_ID + WHITE_SHOP_WITHOUT_PREPAYMENT],
)

# Msku's
WHITE_MSKU = MarketSku(
    title="WHITE_MSKU",
    hid=1,
    hyperid=1,
    sku='25',
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
        cls.index.regiontree += [Region(rid=MSK_RIDS, name="Москва")]

        cls.index.bnpl_conditions.settings = BnplConditionsSettings(
            min_price=20, max_price=20000, allowed_for_all_dsbs=True
        )
        cls.index.bnpl_conditions.white_hids += [1]
        cls.index.bnpl_conditions.dsbs_suppliers_white_list += [WHITE_SHOP_IN_BLACKLIST]

        cls.index.offers += [WHITE_OFFER, WHITE_OFFER_IN_BLACKLIST, WHITE_OFFER_WITHOUT_PREPAYMENT]
        cls.index.mskus += [WHITE_MSKU]

        cls.settings.lms_autogenerate = False
        cls.dynamic.lms += [
            DynamicWarehouseInfo(id=WAREHOUSE_ID, home_region=MSK_RIDS),
            DynamicDeliveryServiceInfo(id=DELIVERY_SERVICE_ID),
            DynamicWarehouseAndDeliveryServiceInfo(warehouse_id=WAREHOUSE_ID, delivery_service_id=DELIVERY_SERVICE_ID),
        ]

        for fesh in (WHITE_SHOP, WHITE_SHOP_IN_BLACKLIST, WHITE_SHOP_WITHOUT_PREPAYMENT):
            cls.index.shops_payment_methods += [
                ShopPaymentMethods(
                    fesh=fesh,
                    payment_groups=[
                        PaymentRegionalGroup(
                            included_regions=[213],
                            payment_methods=[
                                Payment.PT_PREPAYMENT_CARD
                                if fesh != WHITE_SHOP_WITHOUT_PREPAYMENT
                                else Payment.PT_CARD_ON_DELIVERY,
                            ],
                        ),
                    ],
                ),
            ]

    @classmethod
    def prepare_delivery(cls):
        cls.index.shops += [
            Shop(
                fesh=WHITE_SHOP,
                datafeed_id=6,
                priority_region=MSK_RIDS,
                regions=[MSK_RIDS],
                name='Обычный белый магазин',
            ),
            Shop(
                fesh=WHITE_SHOP_IN_BLACKLIST,
                datafeed_id=7,
                priority_region=MSK_RIDS,
                regions=[MSK_RIDS],
                name='Белый магазин, который есть в черном списке для сплита',
            ),
            Shop(
                fesh=WHITE_SHOP_WITHOUT_PREPAYMENT,
                datafeed_id=8,
                priority_region=MSK_RIDS,
                regions=[MSK_RIDS],
                name='Белый магазин, у которого нет предоплаты',
            ),
        ]
        # для WHITE_SHOP_WITHOUT_PREPAYMENT бакет создаем также с предоплатой,
        # так как для белых офферов корректные способы оплаты приходят через файл shops_payment_methods.json,
        # задающийся через cls.index.shops_payment_methods,
        # так мы проверяем, что способы оплаты для белых офферов в бакетах не влияют на способы оплаты на выдаче и, соответственно, на рассрочку
        for fesh in (WHITE_SHOP, WHITE_SHOP_IN_BLACKLIST, WHITE_SHOP_WITHOUT_PREPAYMENT):
            cls.index.delivery_buckets += [
                T.__create_delivery_bucket(id=(DEFAULT_DELIVERY_BUCKET_ID + fesh), fesh=fesh, prepay_allowed=True),
            ]

    def test_bnpl_for_dsbs_offers(self):
        '''
        Проверяем, что если установлен флаг isAllowedForAllDsbs в настройках рассрочки - рассрочка будет на всех подходящих по другим условиям товарах,
        даже на товарах тех магазинов, которые прописаны в черном списке в ограничениях для рассрочки
        '''
        for offer, bnpl_allowed, reason in [
            (WHITE_OFFER_IN_BLACKLIST, True, BnplDenialReason.CDR_NONE),
            (WHITE_OFFER, True, BnplDenialReason.CDR_NONE),
            (WHITE_OFFER_WITHOUT_PREPAYMENT, False, BnplDenialReason.CDR_PREPAYMENT_UNAVAILABLE),
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


if __name__ == '__main__':
    main()
