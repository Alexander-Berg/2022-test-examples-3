#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.matcher import Absent
from core.types import (
    BlueOffer,
    CreditDenial,
    CreditDenialReason,
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
    HyperCategory,
    MarketSku,
    Outlet,
    Payment,
    PaymentRegionalGroup,
    Region,
    RegionalDelivery,
    Shop,
    ShopPaymentMethods,
    Tax,
)


DELIVERY_SERVICE_ID = 100
WAREHOUSE_ID = 145
MSK_RIDS = 213
OUTLETS = list(range(10000, 10005))

TOP_LEVEL_HID = 1000
UNLISTED_HID = 1100
PARENT_EXPLICIT_BLACK_HID = 1200
EXPLICIT_BLACK_HID = 1300
CHILD_BLACK_HID = 1210
EXPLICIT_WHITE_HID = 1220
CHILD_WHITE_HID = 1222
ANOTHER_CHILD_WHITE_HID = 1221

# Blue offers
TOP_LEVEL_OFFER = BlueOffer(price=5000, offerid='Shop2_sku10', waremd5='Sku10Price500-vm1Goleg', feedid=1)
UNLISTED_OFFER = BlueOffer(price=10000, offerid='Shop2_sku15', waremd5='Sku15Price100-vm1Goleg', feedid=1)
EXPLICIT_BLACK_OFFER = BlueOffer(price=6000, offerid='Shop2_sku11', waremd5='Sku11Price600-vm1Goleg', feedid=1)
CHILD_BLACK_OFFER = BlueOffer(price=7000, offerid='Shop2_sku12', waremd5='Sku12Price700-vm1Goleg', feedid=1)
EXPLICIT_WHITE_OFFER = BlueOffer(price=8000, offerid='Shop2_sku13', waremd5='Sku13Price800-vm1Goleg', feedid=1)
CHILD_WHITE_OFFER = BlueOffer(price=9000, offerid='Shop2_sku14', waremd5='Sku14Price900-vm1Goleg', feedid=1)

# Market SKUs
TOP_LEVEL_MSKU = MarketSku(
    title="Top level category offer",
    hid=TOP_LEVEL_HID,
    hyperid=10000,
    sku='10',
    delivery_buckets=[1000],
    blue_offers=[TOP_LEVEL_OFFER],
)
EXPLICIT_BLACK_MSKU = MarketSku(
    title="Explicitly blacklisted offer",
    hid=EXPLICIT_BLACK_HID,
    hyperid=11300,
    sku='11',
    delivery_buckets=[1000],
    blue_offers=[EXPLICIT_BLACK_OFFER],
)
CHILD_BLACK_MSKU = MarketSku(
    title="Parent category blacklisted offer",
    hid=CHILD_BLACK_HID,
    hyperid=11210,
    sku='12',
    delivery_buckets=[1000],
    blue_offers=[CHILD_BLACK_OFFER],
)
EXPLICIT_WHITE_MSKU = MarketSku(
    title="Explicitly whitelisted offer",
    hid=EXPLICIT_WHITE_HID,
    hyperid=11220,
    sku='13',
    delivery_buckets=[1000],
    blue_offers=[EXPLICIT_WHITE_OFFER],
)
CHILD_WHITE_MSKU = MarketSku(
    title="Parent category whitelisted offer",
    hid=CHILD_WHITE_HID,
    hyperid=11222,
    sku='14',
    delivery_buckets=[1000],
    blue_offers=[CHILD_WHITE_OFFER],
)
UNLISTED_MSKU = MarketSku(
    title="Unlisted category offer",
    hid=UNLISTED_HID,
    hyperid=11000,
    sku='15',
    delivery_buckets=[1000],
    blue_offers=[UNLISTED_OFFER],
)

# Credit plans
GLOBAL_RESTRICTIONS = CreditGlobalRestrictions(
    min_price=3500,
    max_price=250000,
    category_blacklist=[PARENT_EXPLICIT_BLACK_HID, EXPLICIT_BLACK_HID],
    category_whitelist=[EXPLICIT_WHITE_HID],
)
GLOBAL_RESTRICTIONS_FRAGMENT = GLOBAL_RESTRICTIONS.to_json(price_as_string=True)
CREDIT_PLAN_ID = 'AD51BF786AA86B36BA57B8002FB4B474'
CREDIT_TERM = 12
CREDIT_PLANS = [
    CreditPlan(
        plan_id=CREDIT_PLAN_ID,
        bank="Сбербанк",
        term=CREDIT_TERM,
        rate=18.1,
        initial_payment_percent=0,
        min_price=2000,
        max_price=200000,
    )
]
CREDIT_PLANS_FRAGMENT = [plan.to_json(price_as_string=True, for_output_json=True) for plan in CREDIT_PLANS]


class T(TestCase):
    @staticmethod
    def __create_outlet(id):
        return Outlet(
            point_id=id,
            delivery_service_id=DELIVERY_SERVICE_ID,
            region=MSK_RIDS,
            point_type=Outlet.FOR_POST,
            working_days=list(range(10)),
            bool_props=["cashAllowed", "cardAllowed", "prepayAllowed"],
        )

    @classmethod
    def prepare(cls):
        cls.settings.report_subrole = 'blue-main'
        cls.index.regiontree += [Region(rid=MSK_RIDS, name="Москва")]

    @classmethod
    def prepare_categories(cls):
        cls.index.hypertree = [
            HyperCategory(
                hid=TOP_LEVEL_HID,
                children=[
                    HyperCategory(hid=UNLISTED_HID),
                    HyperCategory(
                        hid=PARENT_EXPLICIT_BLACK_HID,
                        children=[
                            HyperCategory(hid=CHILD_BLACK_HID),
                            HyperCategory(
                                hid=EXPLICIT_WHITE_HID,
                                children=[
                                    HyperCategory(hid=CHILD_WHITE_HID),
                                    HyperCategory(hid=ANOTHER_CHILD_WHITE_HID),
                                ],
                            ),
                        ],
                    ),
                    HyperCategory(hid=EXPLICIT_BLACK_HID),
                ],
            )
        ]

    @classmethod
    def prepare_credit_plans(cls):
        cls.index.credit_plans_container.global_restrictions = GLOBAL_RESTRICTIONS
        cls.index.credit_plans_container.credit_plans = CREDIT_PLANS

    @classmethod
    def prepare_lms(cls):
        cls.settings.lms_autogenerate = False
        cls.dynamic.lms += [
            DynamicWarehouseInfo(id=WAREHOUSE_ID, home_region=MSK_RIDS),
            DynamicDeliveryServiceInfo(id=DELIVERY_SERVICE_ID),
            DynamicWarehouseAndDeliveryServiceInfo(warehouse_id=WAREHOUSE_ID, delivery_service_id=DELIVERY_SERVICE_ID),
        ]

    @classmethod
    def prepare_delivery(cls):
        cls.index.shipment_service_calendars += [
            DeliveryCalendar(fesh=1, calendar_id=10),
            DeliveryCalendar(fesh=2, calendar_id=11),
        ]
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
                delivery_service_outlets=OUTLETS,
            ),
        ]
        cls.index.outlets += [T.__create_outlet(id) for id in OUTLETS]
        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=1000,
                dc_bucket_id=6000,
                fesh=1,
                carriers=[DELIVERY_SERVICE_ID],
                regional_options=[
                    RegionalDelivery(
                        rid=MSK_RIDS,
                        options=[DeliveryOption(day_from=5, day_to=25, shop_delivery_price=5)],
                        payment_methods=[Payment.PT_CASH_ON_DELIVERY, Payment.PT_CARD_ON_DELIVERY, Payment.PT_YANDEX],
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
                        payment_methods=[Payment.PT_PREPAYMENT_CARD],
                    ),
                ],
            ),
        ]

    @classmethod
    def prepare_blue_offers(cls):
        cls.index.mskus += [
            TOP_LEVEL_MSKU,
            EXPLICIT_BLACK_MSKU,
            CHILD_BLACK_MSKU,
            EXPLICIT_WHITE_MSKU,
            CHILD_WHITE_MSKU,
            UNLISTED_MSKU,
        ]

    @staticmethod
    def __get_offer_fragment(waremd5, title, monthly_payment=None):
        out = {'entity': 'offer', 'wareId': waremd5, 'titles': {'raw': title}}
        if monthly_payment is not None:
            credit_info = CreditInfo(
                best_option_id=CREDIT_PLAN_ID,
                min_term=CREDIT_TERM,
                max_term=CREDIT_TERM,
                monthly_payment=monthly_payment,
            )
            out['creditInfo'] = credit_info.to_json(price_as_string=True) if credit_info is not None else Absent()
        else:
            credit_denial = CreditDenial(reason=CreditDenialReason.CDR_BLACKLIST_CATEGORY, blacklist_offers=[waremd5])
            out['creditDenial'] = credit_denial.to_json(price_as_string=True) if credit_denial is not None else Absent()

        return out

    def test_credit_info(self):
        """
        Проверяем, что скрытие информации о кредитовании в поофферной выдаче
        происходит согласно определенным спискам blacklist и whitelist категорий
        """
        msku_offer_list = [
            (TOP_LEVEL_MSKU, TOP_LEVEL_OFFER, 459),
            (EXPLICIT_BLACK_MSKU, EXPLICIT_BLACK_OFFER, None),
            (CHILD_BLACK_MSKU, CHILD_BLACK_OFFER, None),
            (EXPLICIT_WHITE_MSKU, EXPLICIT_WHITE_OFFER, 734),
            (CHILD_WHITE_MSKU, CHILD_WHITE_OFFER, 826),
            (UNLISTED_MSKU, UNLISTED_OFFER, 917),
        ]

        def get_results_fragment(white_serp):
            results_fragment = []
            for msku, offer, payment in msku_offer_list:
                results_fragment.append(T.__get_offer_fragment(offer.waremd5, msku.title, payment))
                if white_serp:
                    results_fragment.append({"entity": "product"})
            return results_fragment

        request = (
            'place=prime' '&rids={rid}' '&hid={hid}' '&pp=18' '&allow-collapsing=0' '&numdoc=100' '&show-credits=1'
        )
        for market_type, results_fragment in [
            ('&rearr-factors=show_credits_on_white=1', get_results_fragment(white_serp=True)),
            ('&rgb=blue', get_results_fragment(white_serp=False)),
        ]:
            response = self.report.request_json(request.format(rid=MSK_RIDS, hid=TOP_LEVEL_HID) + market_type)
            self.assertFragmentIn(
                response,
                {
                    'results': results_fragment,
                    'globalRestrictions': Absent(),  # глобальные ограничения есть только в выдаче place=credit_info
                    'creditOptions': CREDIT_PLANS_FRAGMENT,
                },
                allow_different_len=False,
            )

    @staticmethod
    def __get_credit_info_request(offer_hid_list):
        request = (
            'place=credit_info'
            '&rgb=blue'
            '&rids={rid}'
            '&offers-list={offer_list}'
            '&total-price={total_price}'
            '&currency=RUR'
            '&show-credits=1'
        )
        return request.format(
            rid=MSK_RIDS,
            offer_list=','.join(
                [
                    '{waremd5}:1;hid:{category};p:{price}'.format(
                        waremd5=offer.waremd5, category=category, price=offer.price
                    )
                    for offer, category in offer_hid_list
                ]
            ),
            total_price=sum([offer.price for offer, category in offer_hid_list]),
        )

    def test_place_credit_info(self):
        """
        Проверяем, что рассчет информация о кредитовании результирующей корзины
        происходит корректно в случае, когда в ней нет товаров из запрещенных
        категорий
        """
        # 3 оффера в корзине:
        #   из неупомянутой категории
        #   из подкатегории явно обеленной категории
        #   из явно обеленной категории
        offer_hid_list = [
            (UNLISTED_OFFER, UNLISTED_HID),
            (CHILD_WHITE_OFFER, CHILD_WHITE_HID),
            (EXPLICIT_WHITE_OFFER, EXPLICIT_WHITE_HID),
        ]
        response = self.report.request_json(T.__get_credit_info_request(offer_hid_list))
        credit_info = CreditInfo(
            best_option_id=CREDIT_PLAN_ID, min_term=CREDIT_TERM, max_term=CREDIT_TERM, monthly_payment=2477
        )
        self.assertFragmentIn(
            response,
            {
                'results': {
                    'creditInfo': credit_info.to_json(price_as_string=True),
                    'creditDenial': Absent(),
                    'globalRestrictions': GLOBAL_RESTRICTIONS_FRAGMENT,
                    'creditOptions': CREDIT_PLANS_FRAGMENT,
                }
            },
            allow_different_len=False,
        )

    def test_place_credit_info_blacklist(self):
        """
        Проверяем, что скрытие информации о кредитовании результирующей корзины
        происходит согласно определенным спискам blacklist и whitelist категорий
        """
        # 3 оффера в корзине:
        #   из корневой категории (по умолчанию обелена)
        #   из явно запрещенной категории
        #   из неупомянутой категории
        offer_hid_list = [
            (TOP_LEVEL_OFFER, TOP_LEVEL_HID),
            (EXPLICIT_BLACK_OFFER, EXPLICIT_BLACK_HID),
            (UNLISTED_OFFER, UNLISTED_HID),
        ]
        response = self.report.request_json(T.__get_credit_info_request(offer_hid_list))
        credit_denial = CreditDenial(
            reason=CreditDenialReason.CDR_BLACKLIST_CATEGORY, blacklist_offers=[EXPLICIT_BLACK_OFFER.waremd5]
        )
        self.assertFragmentIn(
            response,
            {
                'results': {
                    'creditInfo': Absent(),
                    'creditDenial': credit_denial.to_json(price_as_string=True),
                    'globalRestrictions': GLOBAL_RESTRICTIONS_FRAGMENT,
                    'creditOptions': CREDIT_PLANS_FRAGMENT,
                }
            },
            allow_different_len=False,
        )

    def test_place_credit_info_blacklist_2(self):
        # 2 оффера в корзине:
        #   из явно запрещенной категории
        #   из подкатегории явно запрещенной категории
        offer_hid_list = [(EXPLICIT_BLACK_OFFER, EXPLICIT_BLACK_HID), (CHILD_BLACK_OFFER, CHILD_BLACK_HID)]
        response = self.report.request_json(T.__get_credit_info_request(offer_hid_list))
        credit_denial = CreditDenial(
            reason=CreditDenialReason.CDR_BLACKLIST_CATEGORY,
            blacklist_offers=[EXPLICIT_BLACK_OFFER.waremd5, CHILD_BLACK_OFFER.waremd5],
        )
        self.assertFragmentIn(
            response,
            {
                'results': {
                    'creditInfo': Absent(),
                    'creditDenial': credit_denial.to_json(price_as_string=True),
                    'globalRestrictions': GLOBAL_RESTRICTIONS_FRAGMENT,
                    'creditOptions': CREDIT_PLANS_FRAGMENT,
                }
            },
            allow_different_len=False,
        )

    def test_place_credit_info_blacklist_3(self):
        # 1 оффер из запрещенной категории в корзине
        offer_hid_list = [(EXPLICIT_BLACK_OFFER, EXPLICIT_BLACK_HID)]
        response = self.report.request_json(T.__get_credit_info_request(offer_hid_list))
        credit_denial = CreditDenial(
            reason=CreditDenialReason.CDR_BLACKLIST_CATEGORY, blacklist_offers=[EXPLICIT_BLACK_OFFER.waremd5]
        )
        self.assertFragmentIn(
            response,
            {
                'results': {
                    'creditInfo': Absent(),
                    'creditDenial': credit_denial.to_json(price_as_string=True),
                    'globalRestrictions': GLOBAL_RESTRICTIONS_FRAGMENT,
                    'creditOptions': CREDIT_PLANS_FRAGMENT,
                }
            },
            allow_different_len=False,
        )


if __name__ == '__main__':
    main()
