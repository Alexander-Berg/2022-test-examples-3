#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.testcase import TestCase, main
from core.matcher import Absent
from core.types import (
    BlueOffer,
    BnplConditionsSettings,
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
    MarketSku,
    Outlet,
    Payment,
    Region,
    RegionalDelivery,
    Shop,
    Tax,
)


MODEL_ID = 1
CATEGORY_ID = 1
MSK_RIDS = 213
WAREHOUSE_ID = 145
FEED_ID = 1
SUPPLIER_SHOP_ID = 1
VIRTUAL_SHOP_ID = 2
DELIVERY_SERVICE_ID = 10
DELIVERY_BUCKET_ID = 100
OUTLET_ID = 1000

# for testing BNPL price restriction
CHEAP_OFFER_PRICE = 3000
BNPL_PRICE_RESTRICTION_MIN = CHEAP_OFFER_PRICE + 1

# Credit plans
GLOBAL_RESTRICTIONS = CreditGlobalRestrictions(min_price=3500, max_price=250000)
GLOBAL_RESTRICTIONS_FRAGMENT = GLOBAL_RESTRICTIONS.to_json(price_as_string=True)
SBERBANK_PLAN = CreditPlan(
    plan_id='AD51BF786AA86B36BA57B8002FB4B474',
    bank="Сбербанк",
    term=12,
    rate=12.3,
    initial_payment_percent=0,
    min_price=7500,
    max_price=200000,
)
RAIFFEISEN_PLAN = CreditPlan(
    plan_id='0E966DEBAA73ABD8379FA316F8326B8D',
    bank="Райффайзен банк",
    term=24,
    rate=13,
    initial_payment_percent=0,
    min_price=30000,
)
CREDIT_PLANS = [SBERBANK_PLAN, RAIFFEISEN_PLAN]
CREDIT_PLANS_FRAGMENT = [plan.to_json(price_as_string=True) for plan in CREDIT_PLANS]
CREDIT_RANGE_APPROVAL = {'maxMinPrice': 30000, 'minMaxPrice': 200000}

# Blue offers
CHEAP_OFFER = BlueOffer(
    price=CHEAP_OFFER_PRICE, offerid='Shop1_sku10', waremd5='Sku10Price05k-vm1Goleg', feedid=FEED_ID
)
ORDINARY_OFFER = BlueOffer(price=5000, offerid='Shop1_sku11', waremd5='Sku11Price12k-vm1Goleg', feedid=FEED_ID)
EXPENSIVE_OFFER = BlueOffer(price=90000, offerid='Shop1_sku12', waremd5='Sku12Price90k-vm1Goleg', feedid=FEED_ID)
OTHER_HID_OFFER = BlueOffer(price=3000, offerid='Shop1_sku13', waremd5='Sku13Price03k-vm1Goleg', feedid=FEED_ID)

# Market SKUs
CHEAP_MSKU = MarketSku(
    title="Тестовый дешевый оффер",
    hid=CATEGORY_ID,
    hyperid=MODEL_ID,
    sku='10',
    delivery_buckets=[DELIVERY_BUCKET_ID],
    blue_offers=[CHEAP_OFFER],
)
ORDINARY_MSKU = MarketSku(
    title="Тестовый оффер",
    hid=CATEGORY_ID,
    hyperid=MODEL_ID,
    sku='11',
    delivery_buckets=[DELIVERY_BUCKET_ID],
    blue_offers=[ORDINARY_OFFER],
)
EXPENSIVE_MSKU = MarketSku(
    title="Тестовый дорогой оффер",
    hid=CATEGORY_ID,
    hyperid=MODEL_ID,
    sku='12',
    delivery_buckets=[DELIVERY_BUCKET_ID],
    blue_offers=[EXPENSIVE_OFFER],
)
OTHER_HID_MSKU = MarketSku(
    title="Оффер с хидом, отличным от остальных",
    hid=CATEGORY_ID + 1,
    hyperid=MODEL_ID + 1,
    sku='13',
    delivery_buckets=[DELIVERY_BUCKET_ID],
    blue_offers=[OTHER_HID_OFFER],
)


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.report_subrole = 'blue-main'
        cls.index.regiontree += [Region(rid=MSK_RIDS, name="Москва")]
        cls.index.bnpl_conditions.settings = BnplConditionsSettings(min_price=BNPL_PRICE_RESTRICTION_MIN)
        cls.index.bnpl_conditions.white_hids += [1]

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
        cls.index.shops += [
            Shop(
                fesh=SUPPLIER_SHOP_ID,
                datafeed_id=FEED_ID,
                priority_region=MSK_RIDS,
                regions=[MSK_RIDS],
                name="Тестовый поставщик",
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                supplier_type=Shop.FIRST_PARTY,
                blue=Shop.BLUE_REAL,
                cpa=Shop.CPA_REAL,
                warehouse_id=WAREHOUSE_ID,
            ),
            Shop(
                fesh=VIRTUAL_SHOP_ID,
                datafeed_id=2,
                priority_region=MSK_RIDS,
                regions=[MSK_RIDS],
                name="Тестовый виртуальный магазин",
                fulfillment_virtual=True,
                virtual_shop_color=Shop.VIRTUAL_SHOP_BLUE,
                delivery_service_outlets=[OUTLET_ID],
            ),
        ]
        cls.index.shipment_service_calendars += [
            DeliveryCalendar(fesh=SUPPLIER_SHOP_ID, calendar_id=10),
            DeliveryCalendar(fesh=VIRTUAL_SHOP_ID, calendar_id=11),
        ]
        cls.index.outlets += [
            Outlet(
                point_id=OUTLET_ID,
                delivery_service_id=DELIVERY_SERVICE_ID,
                region=MSK_RIDS,
                point_type=Outlet.FOR_POST,
                working_days=list(range(10)),
                bool_props=["cashAllowed", "cardAllowed", "prepayAllowed"],
            )
        ]
        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=DELIVERY_BUCKET_ID,
                dc_bucket_id=2000,
                fesh=SUPPLIER_SHOP_ID,
                carriers=[DELIVERY_SERVICE_ID],
                regional_options=[
                    RegionalDelivery(
                        rid=MSK_RIDS,
                        options=[DeliveryOption(day_from=5, day_to=25, shop_delivery_price=5)],
                        payment_methods=[Payment.PT_ALL],
                    )
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            )
        ]

    @classmethod
    def prepare_blue_offers(cls):
        cls.index.mskus += [
            CHEAP_MSKU,
            ORDINARY_MSKU,
            EXPENSIVE_MSKU,
            OTHER_HID_MSKU,
        ]

    @staticmethod
    def __get_request_string(offer_list, cgi_value=None, rearr_value=None):
        request = 'place=credit_info' '&rids={rid}' '&offers-list={offers}' '&total-price={total_price}' '&currency=RUR'

        if cgi_value is not None:
            request += '&show-credits={cgi_value}'.format(cgi_value=cgi_value)

        if rearr_value is not None:
            request += '&rearr-factors=market_show_credits={rearr_value}'.format(rearr_value=rearr_value)

        return request.format(
            rid=MSK_RIDS,
            offers=','.join(
                [
                    '{waremd5}:1;hid:{category};p:{price}'.format(
                        waremd5=offer.waremd5,
                        category=CATEGORY_ID,
                        price=offer.price,
                    )
                    for offer in offer_list
                ]
            ),
            total_price=sum([offer.price for offer in offer_list]),
        )

    def test_place_credit_info(self):
        """
        Проверяем, что рассчет информации о кредитовании корзины происходит корректно
        Тестируемый place=credit_info реализован для актуализации информации о ежемесячном
        платеже в Checkouter'е - см. MARKETOUT-24598 и MARKETOUT-24933
        """
        offer_list = [CHEAP_OFFER, ORDINARY_OFFER, EXPENSIVE_OFFER]
        credit_info = CreditInfo(
            best_option_id=RAIFFEISEN_PLAN.get_plan_id(), min_term=12, max_term=24, monthly_payment=4659
        )
        for market_type in ['&rearr-factors=show_credits_on_white=1', '&rgb=blue']:
            response = self.report.request_json(
                T.__get_request_string(offer_list=offer_list, cgi_value=1) + market_type
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
            )

    def test_place_credit_info_2(self):
        offer_list = [CHEAP_OFFER, ORDINARY_OFFER]
        credit_info = CreditInfo(
            best_option_id=SBERBANK_PLAN.get_plan_id(), min_term=12, max_term=12, monthly_payment=712
        )
        for market_type in ['&rearr-factors=show_credits_on_white=1', '&rgb=blue']:
            response = self.report.request_json(
                T.__get_request_string(offer_list=offer_list, cgi_value=1) + market_type
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
            )

    def test_too_cheap_cart(self):
        """
        Проверяем, что кредитная информация отсутствует в выдаче, если стоимость корзины
        меньше минимального порога в 3500 RUR
        """
        credit_denial = CreditDenial(reason=CreditDenialReason.CDR_TOO_CHEAP, threshold=500)
        for market_type in ['&rearr-factors=show_credits_on_white=1', '&rgb=blue']:
            response = self.report.request_json(
                T.__get_request_string(offer_list=[CHEAP_OFFER], cgi_value=1) + market_type
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
            )

    def test_too_expensive_cart(self):
        """
        Проверяем, что кредитная информация отсутствует в выдаче, если стоимость корзины
        больше максимального порога в 250000 RUR
        """
        offer_count = 3
        total_price = offer_count * EXPENSIVE_OFFER.price
        credit_denial = CreditDenial(reason=CreditDenialReason.CDR_TOO_EXPENSIVE, threshold=20000)
        request = (
            'place=credit_info'
            '&rids={rid}'
            '&offers-list={offers}'
            '&total-price={total_price}'
            '&currency=RUR'
            '&show-credits=1'
        )
        for market_type in ['&rearr-factors=show_credits_on_white=1', '&rgb=blue']:
            response = self.report.request_json(
                request.format(
                    rid=MSK_RIDS,
                    offers='{waremd5}:{count};p:{price};hid:{category}'.format(
                        waremd5=EXPENSIVE_OFFER.waremd5,
                        count=offer_count,
                        price=EXPENSIVE_OFFER.price,
                        category=CATEGORY_ID,
                    ),
                    total_price=total_price,
                )
                + market_type
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
            )

    def test_no_credit_plan(self):
        """
        Корзина не подходит ни под одну из программ кредитования

        В этом случае в параметрах ошибки приходит интервал цен для корзины, значение цен из
        которого подходит под все кредитные программы

        Такой интервал существует, если никакое из минимальных ограничений среди всезх программ
        не превосходит никакое из максимальных
        """
        credit_denial = CreditDenial(
            reason=CreditDenialReason.CDR_NO_AVAILABLE_CREDIT_PLAN,
            max_min_price=CREDIT_RANGE_APPROVAL['maxMinPrice'],
            min_max_price=CREDIT_RANGE_APPROVAL['minMaxPrice'],
        )
        for market_type in ['&rearr-factors=show_credits_on_white=1', '&rgb=blue']:
            response = self.report.request_json(
                T.__get_request_string(offer_list=[ORDINARY_OFFER], cgi_value=1) + market_type
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
            )

    def test_mandatory_params(self):
        """
        Проверяем, что параметры 'offers-list', цена оффера 'p' в 'offers-list',
        'total-price', 'currency' и 'rids' являются обязательными
        """
        request_params = [
            ';p:{price}'.format(price=ORDINARY_OFFER.price),
            ';hid:{category}'.format(category=CATEGORY_ID),
            '&total-price={price}'.format(price=ORDINARY_OFFER.price),
            '&currency=RUR',
            '&rids={rid}'.format(rid=MSK_RIDS),
        ]
        response_errors = [
            'offer buyer price is a mandatory parameter (absent or zero for offer id = {})'.format(
                ORDINARY_OFFER.waremd5
            ),
            'offer category identifier (hid) is a mandatory parameter (absent for offer id = {})'.format(
                ORDINARY_OFFER.waremd5
            ),
            'total-price and currency are mandatory parameters',
            'total-price and currency are mandatory parameters',
            'rids is a mandatory parameter',
            'invalid RGB value (must be blue)',
        ]
        for i in range(0, len(request_params)):
            request_string = (
                'place=credit_info' '&show-credits=1' '&offers-list={waremd5}:1'.format(waremd5=ORDINARY_OFFER.waremd5)
            )
            for param in request_params[:i]:
                request_string += param
            for param in request_params[i + 1 :]:
                request_string += param

            response = self.report.request_json(request_string)
            self.assertFragmentIn(response, {'error': {'code': 'INVALID_USER_CGI', 'message': response_errors[i]}})
            self.error_log.expect(code=3043)

        # В параметре 'offers-list' отсутствует цена 'p'
        request = (
            'place=credit_info'
            '&offers-list={waremd5}:1'
            '&total-price={price}'
            '&currency=RUR'
            '&rids={rid}'
            '&show-credits=1'
        )
        for market_type in ['&rearr-factors=show_credits_on_white=1', '&rgb=blue']:
            response = self.report.request_json(
                request.format(waremd5=ORDINARY_OFFER.waremd5, price=ORDINARY_OFFER.price, rid=MSK_RIDS) + market_type
            )
            self.assertFragmentIn(
                response,
                {
                    'error': {
                        'code': 'INVALID_USER_CGI',
                        'message': 'offer buyer price is a mandatory parameter (absent or zero for offer id = {waremd5})'.format(
                            waremd5=ORDINARY_OFFER.waremd5
                        ),
                    }
                },
            )
            self.error_log.expect(code=3043)

        # Параметр 'offers-list' вообще отсутствует в запросе
        request = 'place=credit_info' '&total-price={price}' '&currency=RUR' '&rids={rid}' '&show-credits=1'
        for market_type in ['&rearr-factors=show_credits_on_white=1', '&rgb=blue']:
            response = self.report.request_json(request.format(price=ORDINARY_OFFER.price, rid=MSK_RIDS) + market_type)
            self.assertFragmentIn(
                response, {'error': {'code': 'INVALID_USER_CGI', 'message': 'offers-list is a mandatory parameter'}}
            )
            self.error_log.expect(code=3043)

    def test_cgi_and_rearr_params(self):
        """
        Проверяем, что выдача в place=credit_info происходит с учетом
        CGI-параметра 'show-credits' и rearr-флага 'market_show_credits'
        """

        def get_credit_info_fragment(cgi_value, rearr_value):
            fragment = (
                CreditInfo(
                    best_option_id=RAIFFEISEN_PLAN.get_plan_id(), min_term=12, max_term=24, monthly_payment=4659
                ).to_json(price_as_string=True)
                if cgi_value or rearr_value
                else Absent()
            )

            return Absent() if rearr_value is not None and not rearr_value else fragment

        offer_list = [CHEAP_OFFER, ORDINARY_OFFER, EXPENSIVE_OFFER]

        for cgi_value in [0, 1, None]:
            for rearr_value in [0, 1, None]:
                for market_type in ['&rearr-factors=show_credits_on_white=1', '&rgb=blue']:
                    response = self.report.request_json(
                        T.__get_request_string(offer_list=offer_list, cgi_value=cgi_value, rearr_value=rearr_value)
                        + market_type
                    )
                    self.assertFragmentIn(
                        response,
                        {
                            'results': {
                                'creditInfo': get_credit_info_fragment(cgi_value=cgi_value, rearr_value=rearr_value),
                                'creditDenial': Absent(),
                                'globalRestrictions': GLOBAL_RESTRICTIONS_FRAGMENT,
                                'creditOptions': CREDIT_PLANS_FRAGMENT,
                            }
                        },
                    )

    def test_bnpl_info(self):
        """
        Проверяем, что если хотя бы для одного оффера в корзине нельзя получить рассрочку, то и для всей корзины тоже
        В конфиге BNPL стоит ограничение BNPL_PRICE_RESTRICTION_MIN, но для credit_info оно не учитывается,
        проверяется с помощью CHEAP_OFFER, его цена ниже границы
        """
        shopping_cart_with_bnpl = [(CHEAP_OFFER, CHEAP_MSKU, True), (ORDINARY_OFFER, ORDINARY_MSKU, True)]
        shopping_cart_without_bnpl = [
            (CHEAP_OFFER, CHEAP_MSKU, True),
            (OTHER_HID_OFFER, OTHER_HID_MSKU, False),  # для этого оффера нет рассрочки
        ]
        for shopping_cart in [shopping_cart_with_bnpl, shopping_cart_without_bnpl]:
            request = (
                'place=credit_info'
                '&rids={rid}'
                '&offers-list={offers}'
                '&total-price={total_price}'
                '&currency=RUR'
                '&show-credits=1'
                '&rearr-factors=show_credits_on_white=1;enable_bnpl=1'
            )

            response = self.report.request_json(
                request.format(
                    rid=MSK_RIDS,
                    offers=','.join(
                        [
                            '{waremd5}:1;hid:{category};p:{price}'.format(
                                waremd5=offer.waremd5,
                                category=msku.category,
                                price=offer.price,
                            )
                            for offer, msku, _ in shopping_cart
                        ]
                    ),
                    total_price=sum([offer.price for offer, _, _ in shopping_cart]),
                )
            )
            self.assertFragmentIn(
                response,
                {
                    'results': {
                        "offers": [
                            {
                                "entity": "offer",
                                "wareId": offer.waremd5,
                                "hid": msku.category,
                                "yandexBnplInfo": {"enabled": allow_bnpl},
                            }
                            for offer, msku, allow_bnpl in shopping_cart
                        ]
                    }
                },
            )


if __name__ == '__main__':
    main()
