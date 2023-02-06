#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.matcher import Absent
from core.types import (
    BlueOffer,
    CreditGlobalRestrictions,
    CreditInfo,
    CreditPlan,
    Currency,
    DeliveryBucket,
    DeliveryCalendar,
    DeliveryOption,
    DynamicDeliveryServiceInfo,
    DynamicWarehouseAndDeliveryServiceInfo,
    DynamicWarehouseInfo,
    MarketSku,
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
DELIVERY_SERVICE_ID = 100
DELIVERY_BUCKET_ID = 1001
OUTLET_ID = 10001

PRIME_REQUEST = (
    'place=prime'
    '&rids={rid}'
    '&hid={category}'
    '&pp=18'
    '&rgb={color}'
    '&allow-collapsing=0'
    '&numdoc=100'
    '&offerid={waremd5_list}'
    '&show-credits=1'
)

# Credit plans
GLOBAL_RESTRICTIONS = CreditGlobalRestrictions(min_price=3500, max_price=250000)
GLOBAL_RESTRICTIONS_FRAGMENT = GLOBAL_RESTRICTIONS.to_json(price_as_string=True)
SBERBANK_PLAN = CreditPlan(
    plan_id='AD51BF786AA86B36BA57B8002FB4B474', bank='Sberbank', term=12, rate=12.3, initial_payment_percent=0
)
ALPHA_BANK_PLAN = CreditPlan(
    plan_id='C0AE65435E1D9065A64F1335B51C54AB', bank='Alfa-bank', term=12, rate=10.5, initial_payment_percent=0
)
RAIFFEISEN_BANK_PLAN = CreditPlan(
    plan_id='0E966DEBAA73ABD8379FA316F8326B8D', bank='Raiffeisen bank', term=12, rate=13, initial_payment_percent=0
)
CREDIT_PLANS = [SBERBANK_PLAN, ALPHA_BANK_PLAN, RAIFFEISEN_BANK_PLAN]
CREDIT_PLANS_FRAGMENT = [plan.to_json(price_as_string=True) for plan in CREDIT_PLANS]

# Blue offers
COURIER_AND_PICKUP_OFFER = BlueOffer(price=20000, offerid='Shop1_sku19', waremd5='Sku19Price10k-vm1Goleg', feedid=1)
PICKUP_OFFER = BlueOffer(price=21000, offerid='Shop1_sku20', waremd5='Sku20Price11k-vm1Goleg', feedid=1)
COURIER_OFFER = BlueOffer(price=22000, offerid='Shop1_sku21', waremd5='Sku21Price12k-vm1Goleg', feedid=1)
SECRET_SALE_OFFER = BlueOffer(price=35000, offerid='Shop1_sku24', waremd5='Sku24Price35k-vm1Goleg', feedid=1)

# Market SKUs
COURIER_AND_PICKUP_MSKU = MarketSku(
    title="Courier and pickup bucket offer",
    hid=CATEGORY_ID,
    hyperid=MODEL_ID,
    sku='1',
    delivery_buckets=[DELIVERY_BUCKET_ID],
    blue_offers=[COURIER_AND_PICKUP_OFFER],
)
PICKUP_MSKU = MarketSku(
    title="Pickup bucket offer",
    hid=CATEGORY_ID,
    hyperid=MODEL_ID,
    sku='2',
    delivery_buckets=[DELIVERY_BUCKET_ID],
    blue_offers=[PICKUP_OFFER],
)
COURIER_MSKU = MarketSku(
    title="Courier bucket offer",
    hid=CATEGORY_ID,
    hyperid=MODEL_ID,
    sku='3',
    delivery_buckets=[DELIVERY_BUCKET_ID],
    blue_offers=[COURIER_OFFER],
)
SECRET_SALE_MSKU = MarketSku(
    title="Secret sale offer",
    hid=CATEGORY_ID,
    hyperid=MODEL_ID,
    sku='4',
    delivery_buckets=[DELIVERY_BUCKET_ID],
    blue_offers=[SECRET_SALE_OFFER],
)


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.report_subrole = 'blue-main'
        cls.index.regiontree += [Region(rid=MSK_RIDS, name="Москва")]

    @classmethod
    def prepare_credit_plans(cls):
        cls.index.credit_plans_container.global_restrictions = GLOBAL_RESTRICTIONS
        cls.index.credit_plans_container.credit_plans = CREDIT_PLANS

    @classmethod
    def prepare_delivery(cls):
        cls.index.shops += [
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
                fesh=2,
                datafeed_id=2,
                priority_region=MSK_RIDS,
                regions=[MSK_RIDS],
                name="Тестовый виртуальный магазин",
                fulfillment_virtual=True,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
            ),
        ]
        cls.index.shipment_service_calendars += [DeliveryCalendar(fesh=1, calendar_id=100)]
        # способы оплаты берутся из виртуального магазина, здесь специально в бакетах не делаем предоплату,
        # чтобы проверить это
        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=DELIVERY_BUCKET_ID,
                dc_bucket_id=DELIVERY_BUCKET_ID + 100,
                fesh=1,
                carriers=[DELIVERY_SERVICE_ID],
                regional_options=[
                    RegionalDelivery(
                        rid=MSK_RIDS,
                        options=[DeliveryOption(day_from=5, day_to=25, shop_delivery_price=5)],
                        payment_methods=[Payment.PT_CASH_ON_DELIVERY],
                    )
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            )
        ]
        cls.index.shops_payment_methods += [
            ShopPaymentMethods(
                fesh=2,
                payment_groups=[
                    PaymentRegionalGroup(
                        included_regions=[MSK_RIDS],
                        payment_methods=[
                            Payment.PT_PREPAYMENT_CARD,
                        ],
                    ),
                ],
            )
        ]

    @classmethod
    def prepare_lms(cls):
        cls.settings.lms_autogenerate = False
        cls.dynamic.lms += [
            DynamicWarehouseInfo(id=WAREHOUSE_ID, home_region=MSK_RIDS),
            DynamicDeliveryServiceInfo(id=DELIVERY_SERVICE_ID),
            DynamicWarehouseAndDeliveryServiceInfo(warehouse_id=WAREHOUSE_ID, delivery_service_id=DELIVERY_SERVICE_ID),
        ]

    @classmethod
    def prepare_blue_offers(cls):
        cls.index.mskus += [COURIER_AND_PICKUP_MSKU, PICKUP_MSKU, COURIER_MSKU, SECRET_SALE_MSKU]

    @staticmethod
    def __get_offer_fragment(waremd5, payment):
        out = dict(
            entity='offer',
            wareId=waremd5,
            creditInfo=CreditInfo(
                best_option_id=ALPHA_BANK_PLAN.get_plan_id(), min_term=12, max_term=12, monthly_payment=payment
            ).to_json(price_as_string=True),
            creditDenial=Absent(),
        )
        return out

    def test_credit_info(self):
        """
        Набор офферов в данном тесте по ценам совпадает с офферами, определенными в 'test_credit_info_for_offer.py'
        Отличаются ставки по кредитным программам
        В рамках этого теста проверяем, что выбирается другая оптимальная в терминах минимального ежемесячного
        платежа кредитная программа (в данном случае от Альфа-банка)
        """
        fragment_list = [
            # (Оффер, Ожидаемый ежемесячный платеж)
            (COURIER_AND_PICKUP_OFFER, 1763),
            (COURIER_OFFER, 1939),
            (PICKUP_OFFER, 1851),
            (SECRET_SALE_OFFER, 3085),
        ]

        response = self.report.request_json(
            PRIME_REQUEST.format(
                rid=MSK_RIDS,
                category=CATEGORY_ID,
                color='blue',
                waremd5_list=','.join([offer.waremd5 for offer, _ in fragment_list]),
            )
        )
        self.assertFragmentIn(
            response,
            {
                'results': [self.__get_offer_fragment(offer.waremd5, payment) for offer, payment in fragment_list],
                'globalRestrictions': Absent(),  # глобальные ограничения есть только в выдаче place=credit_info
                'creditOptions': CREDIT_PLANS_FRAGMENT,
            },
            allow_different_len=True,
        )


if __name__ == '__main__':
    main()
