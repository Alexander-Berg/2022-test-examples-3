#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from datetime import datetime, timedelta
from core.report import REQUEST_TIMESTAMP
from core.testcase import TestCase, main
from core.matcher import Absent
from core.types import (
    BlueOffer,
    CreditDenial,
    CreditDenialReason,
    CreditGlobalRestrictions,
    CreditInfo,
    CreditPlan,
    CreditTerm,
    CreditTermInfo,
    Currency,
    DeliveryBucket,
    DeliveryCalendar,
    DeliveryOption,
    DynamicDeliveryServiceInfo,
    DynamicQPromos,
    DynamicSkuOffer,
    DynamicWarehouseAndDeliveryServiceInfo,
    DynamicWarehouseInfo,
    DynamicWarehouseToWarehouseInfo,
    MarketSku,
    Outlet,
    Payment,
    PaymentRegionalGroup,
    PickupBucket,
    PickupOption,
    Promo,
    PromoSaleDetails,
    PromoType,
    Region,
    RegionalDelivery,
    Shop,
    ShopPaymentMethods,
    Tax,
)


DT_FORMAT = '%Y-%m-%dT%H:%M:%SZ'
DT_NOW = datetime.fromtimestamp(REQUEST_TIMESTAMP)

MODEL_ID = 1
CATEGORY_ID = 1

MSK_RIDS = 213
WAREHOUSE_ID = 145
DELIVERY_SERVICE_ID = 102

OUTLET_ID_BASE = 13000
BUCKET_ID_BASE = 1000
BUCKET_DC_ID_BASE = 6000
PICKUP_BUCKET_ID_BASE = BUCKET_ID_BASE + 100
PICKUP_BUCKET_DC_ID_BASE = BUCKET_DC_ID_BASE + 100

DEFAULT_DELIVERY_BUCKET_ID = 1001
NO_PREPAY_DELIVERY_BUCKET_ID = 1002
DEFAULT_POST_BUCKET_ID = 1111

PRIME_REQUEST = (
    'place=prime'
    '&rids={rid}'
    '&hid={category}'
    '&pp=18'
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
    '&allow-collapsing=0'
    '&numdoc=100'
    '&show-preorder=1'
    '&market-sku={msku_list}'
    '&show-credits=1'
)

# Credit plans
GLOBAL_RESTRICTIONS = CreditGlobalRestrictions(min_price=3500, max_price=250000)
GLOBAL_RESTRICTIONS_FRAGMENT = GLOBAL_RESTRICTIONS.to_json(price_as_string=True)
SBERBANK_PLAN = CreditPlan(
    plan_id='AD51BF786AA86B36BA57B8002FB4B474',
    bank='Sberbank',
    term=12,
    rate=12.3,
    initial_payment_percent=0,
    max_price=150000,
)
ALPHA_BANK_PLAN = CreditPlan(
    plan_id='C0AE65435E1D9065A64F1335B51C54AB',
    bank='Alfa-bank',
    term=6,
    rate=10.5,
    initial_payment_percent=0,
    min_price=5000,
    max_price=210000,
)
RAIFFEISEN_BANK_PLAN = CreditPlan(
    plan_id='0E966DEBAA73ABD8379FA316F8326B8D',
    bank='Raiffeisen bank',
    terms=[
        CreditTerm(term=6, is_default=False),
        CreditTerm(term=12, is_default=False),
        CreditTerm(term=24, is_default=True),
    ],
    rate=13,
    initial_payment_percent=0,
    min_price=10000,
    max_price=200000,
)
CREDIT_PLANS = [SBERBANK_PLAN, ALPHA_BANK_PLAN, RAIFFEISEN_BANK_PLAN]
CREDIT_PLANS_FRAGMENT = [plan.to_json(price_as_string=True) for plan in CREDIT_PLANS]

# Secret sale promo details
SECRET_SALE_ID = 'Secret_sale_promo_key'
SECRET_SALE_TITLE = "Закрытая распродажа для клиентов Сбербанка"
SECRET_SALE_DESCR = "Типичное описание распродажи"
SECRET_SALE_DISCOUNT_PERCENT = 10
SECRET_SALE_START = DT_NOW - timedelta(days=5)
SECRET_SALE_END = DT_NOW + timedelta(days=5)
SECRET_SALE_PROMO_RESPONSE_FRAGMENT = [
    {
        'type': PromoType.SECRET_SALE,
        'key': SECRET_SALE_ID,
        'startDate': SECRET_SALE_START.strftime(DT_FORMAT),
        'endDate': SECRET_SALE_END.strftime(DT_FORMAT),
        'description': SECRET_SALE_DESCR,
    }
]

# Blue offers
COURIER_AND_PICKUP_OFFER = BlueOffer(price=20000, offerid='Shop1_sku19', waremd5='Sku19Price10k-vm1Goleg', feedid=1)
PICKUP_OFFER = BlueOffer(price=21000, offerid='Shop1_sku20', waremd5='Sku20Price11k-vm1Goleg', feedid=1)
COURIER_OFFER = BlueOffer(price=22000, offerid='Shop1_sku21', waremd5='Sku21Price12k-vm1Goleg', feedid=1)
NO_PREPAY_OFFER = BlueOffer(price=23000, offerid='Shop1_sku22', waremd5='Sku22Price13k-vm1Goleg', feedid=1)
NO_CREDIT_OFFER = BlueOffer(price=220000, offerid='Shop1_sku13', waremd5='Sku13Price375-vm1Goleg', feedid=1)
CHEAP_OFFER = BlueOffer(price=3000, offerid='Shop1_sku16', waremd5='Sku16Price500-vm1Goleg', feedid=1)
PREORDER_OFFER = BlueOffer(price=15000, offerid='Shop1_sku17', waremd5='Sku17Price150-vm1Goleg', feedid=1)
EXTREMELY_EXPENSIVE_OFFER = BlueOffer(price=1000000, offerid='Shop1_sku23', waremd5='Sku23Price1kk-vm1Goleg', feedid=1)
SECRET_SALE_OFFER = BlueOffer(price=35000, offerid='Shop1_sku24', waremd5='Sku24Price35k-vm1Goleg', feedid=1)

# Market SKUs
NO_CREDIT_MSKU = MarketSku(
    title="Offer (no_credit)",
    hid=CATEGORY_ID,
    hyperid=MODEL_ID,
    sku='13',
    delivery_buckets=[DEFAULT_DELIVERY_BUCKET_ID],
    post_buckets=[DEFAULT_POST_BUCKET_ID],
    blue_offers=[NO_CREDIT_OFFER],
)
CHEAP_MSKU = MarketSku(
    title="Cheap offer",
    hid=CATEGORY_ID,
    hyperid=MODEL_ID,
    sku='16',
    delivery_buckets=[DEFAULT_DELIVERY_BUCKET_ID],
    post_buckets=[DEFAULT_POST_BUCKET_ID],
    blue_offers=[CHEAP_OFFER],
)
PREORDER_MSKU = MarketSku(
    title="PreOrder offer",
    hid=CATEGORY_ID,
    hyperid=MODEL_ID,
    sku='17',
    delivery_buckets=[DEFAULT_DELIVERY_BUCKET_ID],
    post_buckets=[DEFAULT_POST_BUCKET_ID],
    blue_offers=[PREORDER_OFFER],
)
COURIER_AND_PICKUP_MSKU = MarketSku(
    title="Courier and pickup bucket offer",
    hid=CATEGORY_ID,
    hyperid=MODEL_ID,
    sku='19',
    delivery_buckets=[DEFAULT_DELIVERY_BUCKET_ID],
    pickup_buckets=[1101],
    post_buckets=[1102],
    blue_offers=[COURIER_AND_PICKUP_OFFER],
)
PICKUP_MSKU = MarketSku(
    title="Pickup bucket offer",
    hid=CATEGORY_ID,
    hyperid=MODEL_ID,
    sku='20',
    delivery_buckets=[NO_PREPAY_DELIVERY_BUCKET_ID],
    pickup_buckets=[1103],
    post_buckets=[1104],
    blue_offers=[PICKUP_OFFER],
)
COURIER_MSKU = MarketSku(
    title="Courier bucket offer",
    hid=CATEGORY_ID,
    hyperid=MODEL_ID,
    sku='21',
    delivery_buckets=[DEFAULT_DELIVERY_BUCKET_ID],
    pickup_buckets=[1105],
    post_buckets=[1106],
    blue_offers=[COURIER_OFFER],
)
NO_PREPAY_MSKU = MarketSku(
    title="Offer without prepayment",
    hid=CATEGORY_ID,
    hyperid=MODEL_ID,
    sku='22',
    delivery_buckets=[NO_PREPAY_DELIVERY_BUCKET_ID],
    pickup_buckets=[1107],
    post_buckets=[1108],
    blue_offers=[NO_PREPAY_OFFER],
)
EXTREMELY_EXPENSIVE_MSKU = MarketSku(
    title="Extremely expensive offer",
    hid=CATEGORY_ID,
    hyperid=MODEL_ID,
    sku='23',
    delivery_buckets=[DEFAULT_DELIVERY_BUCKET_ID],
    post_buckets=[DEFAULT_POST_BUCKET_ID],
    blue_offers=[EXTREMELY_EXPENSIVE_OFFER],
)
SECRET_SALE_MSKU = MarketSku(
    title="Secret sale offer",
    hid=CATEGORY_ID,
    hyperid=MODEL_ID,
    sku='24',
    delivery_buckets=[DEFAULT_DELIVERY_BUCKET_ID],
    post_buckets=[DEFAULT_POST_BUCKET_ID],
    blue_offers=[SECRET_SALE_OFFER],
)


class T(TestCase):
    @staticmethod
    def __create_outlet(id, type, prepay_allowed):
        payment_methods = ['cashAllowed', 'cardAllowed']
        if prepay_allowed:
            payment_methods += ['prepayAllowed']

        return Outlet(
            point_id=id,
            delivery_service_id=DELIVERY_SERVICE_ID,
            region=MSK_RIDS,
            point_type=type,
            working_days=list(range(10)),
            bool_props=payment_methods,
        )

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
    def __create_delivery_bucket(id, prepay_allowed):
        return DeliveryBucket(
            bucket_id=id,
            dc_bucket_id=BUCKET_DC_ID_BASE + (id - BUCKET_ID_BASE),
            fesh=1,
            carriers=[DELIVERY_SERVICE_ID],
            regional_options=[T.__create_delivery_options(prepay_allowed)],
            delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
        )

    @staticmethod
    def __create_pickup_bucket(id, options):
        return PickupBucket(
            bucket_id=id,
            dc_bucket_id=PICKUP_BUCKET_DC_ID_BASE + (id - PICKUP_BUCKET_ID_BASE),
            fesh=1,
            carriers=[DELIVERY_SERVICE_ID],
            options=[PickupOption(outlet_id=i) for i in options],
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
    def prepare_secret_sale(cls):
        promo = Promo(
            promo_type=PromoType.SECRET_SALE,
            key=SECRET_SALE_ID,
            title=SECRET_SALE_TITLE,
            description=SECRET_SALE_DESCR,
            start_date=SECRET_SALE_START,
            end_date=SECRET_SALE_END,
            secret_sale_details=PromoSaleDetails(
                msku_list=[int(SECRET_SALE_MSKU.sku)], discount_percent_list=[SECRET_SALE_DISCOUNT_PERCENT]
            ),
            source_promo_id=SECRET_SALE_ID,
        )
        cls.index.promos += [promo]
        cls.dynamic.qpromos += [DynamicQPromos([promo])]

    @classmethod
    def prepare_lms(cls):
        cls.settings.lms_autogenerate = False
        cls.dynamic.lms += [
            DynamicWarehouseInfo(id=WAREHOUSE_ID, home_region=MSK_RIDS),
            DynamicWarehouseToWarehouseInfo(warehouse_from=WAREHOUSE_ID, warehouse_to=WAREHOUSE_ID),
            DynamicDeliveryServiceInfo(id=DELIVERY_SERVICE_ID),
            DynamicWarehouseAndDeliveryServiceInfo(warehouse_id=WAREHOUSE_ID, delivery_service_id=DELIVERY_SERVICE_ID),
        ]
        cls.dynamic.preorder_sku_offers += [DynamicSkuOffer(shop_id=1, sku=PREORDER_OFFER.offerid)]

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
                delivery_service_outlets=[10001, 10002, 10003] + list(range(13001, 13017)),
            ),
        ]
        cls.index.shipment_service_calendars += [
            DeliveryCalendar(fesh=1, calendar_id=102),
            DeliveryCalendar(fesh=2, calendar_id=1111),
        ]
        cls.index.delivery_buckets += [
            # sku = 19, 21
            T.__create_delivery_bucket(id=DEFAULT_DELIVERY_BUCKET_ID, prepay_allowed=True),
            # sku = 20, 22
            T.__create_delivery_bucket(id=NO_PREPAY_DELIVERY_BUCKET_ID, prepay_allowed=False),
        ]
        cls.index.outlets += [
            # pickup_bucket_id = 1101
            T.__create_outlet(id=13001, type=Outlet.FOR_PICKUP, prepay_allowed=False),
            T.__create_outlet(id=13002, type=Outlet.FOR_PICKUP, prepay_allowed=False),
            # pickup_bucket_id = 1102
            T.__create_outlet(id=13003, type=Outlet.FOR_POST, prepay_allowed=False),
            T.__create_outlet(id=13004, type=Outlet.FOR_POST, prepay_allowed=True),
            # pickup_bucket_id = 1103
            T.__create_outlet(id=13005, type=Outlet.FOR_PICKUP, prepay_allowed=True),
            T.__create_outlet(id=13006, type=Outlet.FOR_PICKUP, prepay_allowed=False),
            # pickup_bucket_id = 1104
            T.__create_outlet(id=13007, type=Outlet.FOR_POST, prepay_allowed=False),
            T.__create_outlet(id=13008, type=Outlet.FOR_POST, prepay_allowed=False),
            # pickup_bucket_id = 1105
            T.__create_outlet(id=13009, type=Outlet.FOR_PICKUP, prepay_allowed=False),
            T.__create_outlet(id=13010, type=Outlet.FOR_PICKUP, prepay_allowed=False),
            # pickup_bucket_id = 1106
            T.__create_outlet(id=13011, type=Outlet.FOR_POST, prepay_allowed=False),
            T.__create_outlet(id=13012, type=Outlet.FOR_POST, prepay_allowed=False),
            # pickup_bucket_id = 1107
            T.__create_outlet(id=13013, type=Outlet.FOR_PICKUP, prepay_allowed=False),
            T.__create_outlet(id=13014, type=Outlet.FOR_PICKUP, prepay_allowed=False),
            # pickup_bucket_id = 1108
            T.__create_outlet(id=13015, type=Outlet.FOR_POST, prepay_allowed=False),
            T.__create_outlet(id=13016, type=Outlet.FOR_POST, prepay_allowed=False),
            # pickup_bucket_id = DEFAULT_POST_BUCKET_ID
            T.__create_outlet(id=10001, type=Outlet.FOR_POST, prepay_allowed=True),
            T.__create_outlet(id=10002, type=Outlet.FOR_POST, prepay_allowed=True),
            T.__create_outlet(id=10003, type=Outlet.FOR_POST, prepay_allowed=True),
        ]
        cls.index.pickup_buckets += [
            # sku = 19
            T.__create_pickup_bucket(id=1101, options=[13001, 13002]),  # prepay_allowed=False
            T.__create_pickup_bucket(id=1102, options=[13003, 13004]),  # prepay_allowed=True
            # sku = 20
            T.__create_pickup_bucket(id=1103, options=[13005, 13006]),  # prepay_allowed=True
            T.__create_pickup_bucket(id=1104, options=[13007, 13008]),  # prepay_allowed=False
            # sku = 21
            T.__create_pickup_bucket(id=1105, options=[13009, 13010]),  # prepay_allowed=False
            T.__create_pickup_bucket(id=1106, options=[13011, 13012]),  # prepay_allowed=False
            # sku = 22
            T.__create_pickup_bucket(id=1107, options=[13013, 13014]),  # prepay_allowed=False
            T.__create_pickup_bucket(id=1108, options=[13015, 13016]),  # prepay_allowed=False
            # sku = 13, 16, 17, 23, 24
            T.__create_pickup_bucket(id=DEFAULT_POST_BUCKET_ID, options=[10001, 10002, 10003]),
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
    def prepare_blue_offers(cls):
        cls.index.mskus += [
            COURIER_AND_PICKUP_MSKU,
            PICKUP_MSKU,
            COURIER_MSKU,
            NO_PREPAY_MSKU,
            NO_CREDIT_MSKU,
            CHEAP_MSKU,
            PREORDER_MSKU,
            EXTREMELY_EXPENSIVE_MSKU,
            SECRET_SALE_MSKU,
        ]

    @staticmethod
    def __get_offer_fragment(waremd5, credit_info=None, credit_denial=None):
        out = dict(
            entity='offer',
            wareId=waremd5,
            creditInfo=credit_info.to_json(price_as_string=True) if credit_info is not None else Absent(),
            creditDenial=credit_denial.to_json(price_as_string=True) if credit_denial is not None else Absent(),
        )
        return out

    def test_credit_info(self):
        """
        Проверяем, что рассчет информации о кредитовании оффера происходит корректно
        """

        # msku, offer, credit_info, credit_denial
        fragment_list = [
            (
                COURIER_AND_PICKUP_MSKU,
                COURIER_AND_PICKUP_OFFER,
                CreditInfo(
                    best_option_id=RAIFFEISEN_BANK_PLAN.get_plan_id(), min_term=6, max_term=24, monthly_payment=951
                ),
                None,
            ),
            (
                PICKUP_MSKU,
                PICKUP_OFFER,
                CreditInfo(
                    best_option_id=RAIFFEISEN_BANK_PLAN.get_plan_id(), min_term=6, max_term=24, monthly_payment=998
                ),
                None,
            ),
            (
                COURIER_MSKU,
                COURIER_OFFER,
                CreditInfo(
                    best_option_id=RAIFFEISEN_BANK_PLAN.get_plan_id(), min_term=6, max_term=24, monthly_payment=1046
                ),
                None,
            ),
            # в бакете у оффера нет предоплаты, но у виртуального магазина предоплата есть
            # информацию о способах оплаты берем по магазину, поэтому кредит для товара разрешен
            (
                NO_PREPAY_MSKU,
                NO_PREPAY_OFFER,
                CreditInfo(
                    best_option_id=RAIFFEISEN_BANK_PLAN.get_plan_id(), min_term=6, max_term=24, monthly_payment=1093
                ),
                None,
            ),
            (
                NO_CREDIT_MSKU,
                NO_CREDIT_OFFER,
                None,
                CreditDenial(reason=CreditDenialReason.CDR_NO_AVAILABLE_CREDIT_PLAN),
            ),
            (CHEAP_MSKU, CHEAP_OFFER, None, CreditDenial(reason=CreditDenialReason.CDR_TOO_CHEAP, threshold=500)),
            (
                EXTREMELY_EXPENSIVE_MSKU,
                EXTREMELY_EXPENSIVE_OFFER,
                None,
                CreditDenial(reason=CreditDenialReason.CDR_TOO_EXPENSIVE, threshold=750000),
            ),
            (
                SECRET_SALE_MSKU,
                SECRET_SALE_OFFER,
                CreditInfo(
                    best_option_id=RAIFFEISEN_BANK_PLAN.get_plan_id(), min_term=6, max_term=24, monthly_payment=1664
                ),
                None,
            ),
            (PREORDER_MSKU, PREORDER_OFFER, None, CreditDenial(reason=CreditDenialReason.CDR_PRE_ORDER)),
        ]

        # prime
        for market_type in [
            '&rearr-factors=market_nordstream_relevance=0',
            '&rgb=blue&rearr-factors=market_nordstream_relevance=0',
        ]:
            response = self.report.request_json(
                PRIME_REQUEST.format(
                    rid=MSK_RIDS,
                    category=CATEGORY_ID,
                    waremd5_list=','.join([offer.waremd5 for msku, offer, credit_info, credit_denial in fragment_list]),
                    show_credits_cgi=1,
                )
                + market_type
            )
            self.assertFragmentIn(
                response,
                {
                    'results': [
                        self.__get_offer_fragment(offer.waremd5, credit_info, credit_denial)
                        for msku, offer, credit_info, credit_denial in fragment_list
                    ]
                },
                allow_different_len=True,
            )

            # sku_offers
            response = self.report.request_json(
                SKU_OFFERS_REQUEST.format(
                    rid=MSK_RIDS,
                    category=CATEGORY_ID,
                    msku_list=','.join([msku.sku for msku, offer, credit_info, credit_denial in fragment_list]),
                )
                + market_type
            )
            self.assertFragmentIn(
                response,
                {
                    'results': [
                        {
                            'entity': 'sku',
                            'offers': {'items': [self.__get_offer_fragment(offer.waremd5, credit_info, credit_denial)]},
                        }
                        for msku, offer, credit_info, credit_denial in fragment_list
                    ]
                },
                allow_different_len=True,
            )

    @staticmethod
    def __apply_discount(price, discount):
        return int(price * (1.0 - discount / 100.0))

    def test_credit_info_disabled_by_promo(self):
        """
        Проверяем, что информация о кредитовании оффера, участвующего в Закрытой распродаже,
        отсутствует в выдаче для Беру
        """
        request_string = (
            'place={place}'
            '&hid={category}'
            '&pp=18'
            '&debug=0'
            '&rids={rid}'
            '&allow-collapsing=0'
            '&numdoc=100'
            '&offerid={offerid}'
            '&allowed-promos={promo}'
            '&show-credits=1'
        )

        for market_type in ['&rearr-factors=show_credits_on_white=1', '&rgb=blue']:
            response = self.report.request_json(
                request_string.format(
                    place='prime',
                    category=CATEGORY_ID,
                    rid=MSK_RIDS,
                    offerid=SECRET_SALE_OFFER.waremd5,
                    promo=SECRET_SALE_ID,
                )
                + market_type
            )
            self.assertFragmentIn(
                response,
                {
                    'results': [
                        {
                            'entity': 'offer',
                            'wareId': SECRET_SALE_OFFER.waremd5,
                            'creditInfo': Absent(),
                            'promos': SECRET_SALE_PROMO_RESPONSE_FRAGMENT,
                            'prices': {
                                'currency': Currency.RUR,
                                'value': str(
                                    T.__apply_discount(
                                        price=SECRET_SALE_OFFER.price, discount=SECRET_SALE_DISCOUNT_PERCENT
                                    )
                                ),
                                'discount': {
                                    'oldMin': str(SECRET_SALE_OFFER.price),
                                    'percent': SECRET_SALE_DISCOUNT_PERCENT,
                                },
                            },
                        }
                    ]
                },
            )

    def test_cgi_and_rearr_params(self):
        """
        Проверяем, что при CGI-параметре 'show-credits=0'
        информация о ежемесячном платеже отсутствует на выдаче

        Проверяем, что rearr-флаг 'market_show_credits' переопределяет
        значение CGI-параметра, если присутствует в запросе явно
        """

        def get_expected_credit_info(cgi_value, rearr_value):
            credit_info = (
                CreditInfo(
                    best_option_id=RAIFFEISEN_BANK_PLAN.get_plan_id(), min_term=6, max_term=24, monthly_payment=951
                )
                if cgi_value or rearr_value
                else None
            )

            if rearr_value is not None and not rearr_value:
                credit_info = None

            return [self.__get_offer_fragment(waremd5=COURIER_AND_PICKUP_OFFER.waremd5, credit_info=credit_info)]

        for cgi_value in [0, 1]:
            for rearr_value in [0, 1, None]:
                for market_type in ['&rearr-factors=show_credits_on_white=1', '&rgb=blue']:
                    request = PRIME_REQUEST + market_type
                    if rearr_value is not None:
                        request += '&rearr-factors=market_show_credits={show_credits_rearr}'.format(
                            show_credits_rearr=rearr_value
                        )
                    response = self.report.request_json(
                        request.format(
                            rid=MSK_RIDS,
                            category=CATEGORY_ID,
                            waremd5_list=COURIER_AND_PICKUP_OFFER.waremd5,
                            show_credits_cgi=cgi_value,
                        )
                    )
                    self.assertFragmentIn(
                        response,
                        {'results': get_expected_credit_info(cgi_value=cgi_value, rearr_value=rearr_value)},
                        allow_different_len=True,
                    )

    def test_rearr_only(self):
        """
        Проверяем, что при отсутствии CGI-параметра 'show-credits' информация
        о ежемесячном платеже присутствует на выдаче в зависимости от значения
        rearr-флага 'market_show_credits'
        """
        request = (
            'place=prime'
            '&rids={rid}'
            '&hid={category}'
            '&pp=18'
            '&allow-collapsing=0'
            '&numdoc=100'
            '&show-preorder=1'
            '&offerid={waremd5_list}'
            '&rearr-factors=market_show_credits={show_credits}'
        )

        for rearr_value in [0, 1]:
            for market_type in ['&rearr-factors=show_credits_on_white=1', '&rgb=blue']:
                response = self.report.request_json(
                    request.format(
                        rid=MSK_RIDS,
                        category=CATEGORY_ID,
                        waremd5_list=COURIER_AND_PICKUP_OFFER.waremd5,
                        show_credits=rearr_value,
                    )
                    + market_type
                )
                self.assertFragmentIn(
                    response,
                    {
                        'results': [
                            self.__get_offer_fragment(
                                waremd5=COURIER_AND_PICKUP_OFFER.waremd5,
                                credit_info=CreditInfo(
                                    best_option_id=RAIFFEISEN_BANK_PLAN.get_plan_id(),
                                    min_term=6,
                                    max_term=24,
                                    monthly_payment=951,
                                )
                                if rearr_value
                                else None,
                            )
                        ]
                    },
                    allow_different_len=True,
                )

    @classmethod
    def prepare_test_credit_for_offer_with_subscription_goodd(cls):
        """
        Проверяем, что для товара по подписке кредит не доступен
        """
        cls.index.shops += [
            Shop(
                fesh=11317159,
                business_fesh=11317160,
                datafeed_id=3,
                priority_region=213,
                blue=Shop.BLUE_REAL,
                cpa=Shop.CPA_REAL,
            )
        ]

        cls.index.mskus += [
            MarketSku(
                hyperid=1,
                sku=1001,
                fesh=11317159,
                blue_offers=[
                    BlueOffer(
                        title='subscription offer', business_id=11317160, feedid=3, price=12200, credit_template_id=2
                    )
                ],
                delivery_buckets=[DEFAULT_DELIVERY_BUCKET_ID],
            ),
        ]

    def test_several_terms_in_credit_plan(self):
        """
        Проверяем, что при задании нескольких сроков в кредитном плане, они все
        отдаются на выдачу с ежемесячными платежами в creditInfo для выбранного кредитного плана
        При этом вся информация в ответе репорта времен одного срока в кредитном плане остается
        для обратной совместимости (тикет на удаление MARKETOUT-46238)
        """
        request = 'place=prime' '&rids={rid}' '&pp=18' '&allow-collapsing=0' '&offerid={waremd5}' '&show-credits=1'
        response = self.report.request_json(
            request.format(
                rid=MSK_RIDS,
                waremd5=COURIER_AND_PICKUP_OFFER.waremd5,
            )
        )

        self.assertFragmentIn(
            response,
            {
                'results': [
                    self.__get_offer_fragment(
                        waremd5=COURIER_AND_PICKUP_OFFER.waremd5,
                        credit_info=CreditInfo(
                            best_option_id=RAIFFEISEN_BANK_PLAN.get_plan_id(),
                            min_term=6,
                            max_term=24,
                            terms=[
                                CreditTermInfo(
                                    term=6,
                                    monthly_payment=3461,
                                ),
                                CreditTermInfo(
                                    term=12,
                                    monthly_payment=1786,
                                ),
                                CreditTermInfo(
                                    term=24,
                                    monthly_payment=951,
                                ),
                            ],
                            monthly_payment=951,  # берется из дефолтного плана на 24 месяца
                        ),
                    )
                ]
            },
            allow_different_len=False,
        )


if __name__ == '__main__':
    main()
