#!/usr/bin/env python
# -*- coding: utf-8 -*-

import runner  # noqa

from datetime import date, datetime, timedelta


from core.matcher import (
    Absent,
)
from core.testcase import (
    TestCase,
    main,
)
from core.types.delivery import DeliveryBucket, DeliveryOption, RegionalDelivery
from core.types.combinator import CombinatorOffer, create_delivery_option, create_user_info, DeliveryItem, Destination
from core.types.dynamic_filters import (
    DateSwitchTimeAndRegionInfo,
    DeliveryServiceRegionToRegionInfo,
    DynamicDaysSet,
    DynamicDeliveryServiceInfo,
    DynamicWarehouseAndDeliveryServiceInfo,
    DynamicWarehouseInfo,
    DynamicWarehouseToWarehouseInfo,
    DynamicWarehousesPriorityInRegion,
    DynamicTimeIntervalsSet,
)
from core.types import (
    TimeInfo,
    TimeIntervalInfo,
    ExpressSupplier,
    GLParam,
    GLType,
    GLValue,
    Model,
    NavCategory,
    HyperCategory,
    MnPlace,
    DynamicBlueGenericBundlesPromos,
)
from core.types.offer import OfferDimensions
from core.types.region import Region
from core.types.shop import Shop
from core.types.currency import Currency
from core.types.taxes import Vat
from market.pylibrary.const.payment_methods import (
    PaymentMethod,
)

from core.types.sku import (
    BlueOffer,
    MarketSku,
)

from core.types import Promo, PromoType, Payment, PaymentRegionalGroup, ShopPaymentMethods
from core.types.offer_promo import PromoBlueFlash
from core.report import REQUEST_TIMESTAMP

now = datetime.fromtimestamp(REQUEST_TIMESTAMP)
TODAY = date(year=2021, month=12, day=30)
MOSCOW_RIDS = 213
CENTRAL_RIDS = 3
MOSCOW_AREA_RIDS = 1

MAIN_BUCKET_ID = 123
EXPRESS_BUCKET_ID = 124

B2C_CATEGORY = 800
B2C_NAVNODE_ID = 8000
B2C_HYPER_ID = 8000

B2B_CATEGORY = 900
B2B_NAVNODE_ID = 9000
B2B_HYPER_ID = 9000


# тестовые данные для проверки карты переходов
class IPHONE:
    ID = 123
    MODEL_ID = 7890

    RED_SKU = 1234
    BLUE_SKU = 2345
    BLACK_SKU = 3456

    class COLOR:
        ID = 234
        XSL_NAME = "color"
        RUS_NAME = "Цвет"

        RED_ID = 3
        BLUE_ID = 2
        BLACK_ID = 1


def iphone_color(value):
    return GLParam(param_id=IPHONE.COLOR.ID, value=value)


def make_model(id, hid):
    return Model(hyperid=id, hid=hid, title='model ' + str(id))


def make_sku_with_params(hyper_id, sku, blue_offers, gl_params, original_glparams):
    return MarketSku(
        hyperid=hyper_id,
        sku=sku,
        blue_offers=blue_offers,
        glparams=gl_params,
        original_glparams=original_glparams,
        delivery_buckets=[MAIN_BUCKET_ID],
    )


class _TimeIntervals:
    _TimeFrom = [
        TimeInfo(10, 0),
    ]
    _TimeTo = [
        TimeInfo(18, 00),
    ]
    _TimeIntervalInfo = [
        TimeIntervalInfo(_TimeFrom[0], _TimeTo[0]),
    ]


B2B_OFFERID = 'TestB2BSku___________g'

non_b2b_filters = [
    {"id": "promo-type-filter"},
    {"id": "at-beru-warehouse"},
    {"id": "with-yandex-delivery"},
    {"id": "qrfrom"},
    {"id": "offer-shipping"},
    {"id": "fesh"},
]

# офферы для теста цен без НДС
offer_vat_10 = BlueOffer(
    price=100,
    vat=Vat.VAT_10,
    fesh=1,
    feedid=1,
    offerid='b2b.offer.vat10',
    waremd5='TestOfferVat10_______g',
    is_b2b=True,
)
offer_vat_10_110 = BlueOffer(
    price=100,
    vat=Vat.VAT_10_110,
    fesh=1,
    feedid=1,
    offerid='b2b.offer.vat10',
    waremd5='TestOfferVat10_110___g',
    is_b2b=True,
)
offer_vat_18 = BlueOffer(
    price=100,
    vat=Vat.VAT_18,
    fesh=1,
    feedid=1,
    offerid='b2b.offer.vat18',
    waremd5='TestOfferVat18_______g',
    is_b2b=True,
)
offer_vat_18_118 = BlueOffer(
    price=100,
    vat=Vat.VAT_18_118,
    fesh=1,
    feedid=1,
    offerid='b2b.offer.vat18',
    waremd5='TestOfferVat18_118___g',
    is_b2b=True,
)
offer_vat_20 = BlueOffer(
    price=100,
    vat=Vat.VAT_20,
    fesh=1,
    feedid=1,
    offerid='b2b.offer.vat20',
    waremd5='TestOfferVat20_______g',
    is_b2b=True,
)
offer_vat_20_120 = BlueOffer(
    price=100,
    vat=Vat.VAT_20_120,
    fesh=1,
    feedid=1,
    offerid='b2b.offer.vat20',
    waremd5='TestOfferVat20_120___g',
    is_b2b=True,
)
offer_vat_0 = BlueOffer(
    price=100,
    vat=Vat.VAT_0,
    fesh=1,
    feedid=1,
    offerid='b2b.offer.vat10',
    waremd5='TestOfferVat0________g',
    is_b2b=True,
)
offer_vat_no = BlueOffer(
    price=100,
    vat=Vat.NO_VAT,
    fesh=1,
    feedid=1,
    offerid='b2b.offer.vat10',
    waremd5='TestOfferVatNo_______g',
    is_b2b=True,
)
offer_promo = BlueOffer(
    price=900,
    price_old=1000,
    vat=Vat.NO_VAT,
    fesh=1,
    feedid=1,
    offerid='b2b.offer.promo',
    waremd5='TestOfferPromo_______g',
    blue_promo_key='JVvklxUgdnawSJPG4UhZGA',
    is_b2b=True,
)

offer_express = BlueOffer(
    price=900,
    vat=Vat.NO_VAT,
    fesh=2,
    feedid=2,
    offerid='b2b.offer.express',
    waremd5='TestOfferExpress_____g',
    weight=5,
    dimensions=OfferDimensions(length=20, width=30, height=10),
    cargo_types=[1, 2, 3],
    is_express=True,
    is_b2b=True,
)

# офферы для проверки карты переходов

offer_iphone_red = BlueOffer(
    price=100000,
    vat=Vat.NO_VAT,
    fesh=1,
    feedid=1,
    offerid='offer.iphone.red',
    waremd5='OfferIphoneRed_______g',
    is_b2b=True,
    is_fulfillment=False,
)
offer_iphone_blue = BlueOffer(
    price=110000,
    vat=Vat.NO_VAT,
    fesh=1,
    feedid=1,
    offerid='offer.iphone.blue',
    waremd5='OfferIphoneBlue______g',
    is_b2b=True,
    is_fulfillment=False,
)
offer_iphone_black = BlueOffer(
    price=105000,
    vat=Vat.NO_VAT,
    fesh=1,
    feedid=1,
    offerid='offer.iphone.black',
    waremd5='OfferIphoneBlack_____g',
    is_b2b=False,
    is_fulfillment=False,
)

# Мапа "id оффера - размер цены без НДС"
offers_price_without_vat_map = {
    offer_vat_0.waremd5: 100,
    offer_vat_no.waremd5: 100,
    offer_vat_10.waremd5: 90.90909,
    offer_vat_10_110.waremd5: 90.90909,
    # НДС 18 устарел, ожидаем подмену на НДС 20
    offer_vat_18.waremd5: 83.33333,
    # НДС 18/118 устарел, ожидаем подмену на НДС 20/120
    offer_vat_18_118.waremd5: 83.33333,
    offer_vat_20.waremd5: 83.33333,
    offer_vat_20_120.waremd5: 83.33333,
}

# массив офферов для добавления в индекс
vat_test_offers = [
    offer_vat_0,
    offer_vat_no,
    offer_vat_10,
    offer_vat_10_110,
    offer_vat_18,
    offer_vat_18_118,
    offer_vat_20,
    offer_vat_20_120,
]


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.check_combinator_errors = True
        cls.settings.report_subrole = 'blue-main'
        cls.settings.default_search_experiment_flags += [
            'enable_dsbs_combinator_request_in_actual_delivery=0',
            'market_combinator_use_courier_payment=1',
            'market_nordstream=0',
        ]
        cls.index.express_partners.suppliers += [
            ExpressSupplier(
                feed_id=2,
                supplier_id=2,
                warehouse_id=146,
            )
        ]
        cls.index.regiontree += [
            Region(
                rid=CENTRAL_RIDS,
                name="Центральный федеральный округ",
                region_type=Region.FEDERAL_DISTRICT,
                children=[
                    Region(
                        rid=MOSCOW_AREA_RIDS,
                        name="Москва и Московская область",
                        region_type=Region.FEDERATIVE_SUBJECT,
                        children=[Region(rid=MOSCOW_RIDS, name="Москва", region_type=Region.CITY)],
                    )
                ],
            )
        ]
        cls.index.shops += [
            Shop(
                fesh=1,
                datafeed_id=1,
                supplier_type=Shop.FIRST_PARTY,
                blue=Shop.BLUE_REAL,
                cpa=Shop.CPA_REAL,
                priority_region=MOSCOW_RIDS,
                name='shop_b2b_1',
                currency=Currency.RUR,
                fulfillment_program=False,
            ),
            Shop(
                fesh=2,
                datafeed_id=2,
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
                cpa=Shop.CPA_REAL,
                priority_region=MOSCOW_RIDS,
                name='express_shop',
                currency=Currency.RUR,
                warehouse_id=146,
                with_express_warehouse=True,
            ),
        ]
        cls.index.shops_payment_methods += [
            ShopPaymentMethods(
                fesh=1,
                payment_groups=[
                    PaymentRegionalGroup(
                        included_regions=[213],
                        payment_methods=[
                            Payment.PT_PREPAYMENT_CARD,
                            Payment.PT_PREPAYMENT_OTHER,
                            Payment.PT_CARD_ON_DELIVERY,
                            Payment.PT_CASH_ON_DELIVERY,
                        ],
                    )
                ],
            )
        ]

        cls.settings.lms_autogenerate = False
        cls.dynamic.lms += [DynamicDaysSet(key=1, days=[])]

        cls.dynamic.lms += [
            DynamicDeliveryServiceInfo(
                id=103,
                name='ds_name',
                region_to_region_info=[DeliveryServiceRegionToRegionInfo(region_from=213, region_to=225, days_key=1)],
            ),
            DynamicDeliveryServiceInfo(
                id=104,
                name='expressDeliveryService',
                region_to_region_info=[DeliveryServiceRegionToRegionInfo(region_from=213, region_to=225, days_key=1)],
            ),
        ]

        cls.dynamic.lms += [
            DynamicWarehouseInfo(
                id=145,
                home_region=213,
            ),
            DynamicWarehouseToWarehouseInfo(warehouse_from=145, warehouse_to=145),
            DynamicWarehouseInfo(
                id=146,
                home_region=213,
                is_express=True,
                shipment_schedule=[
                    DynamicTimeIntervalsSet(key=1, intervals=[_TimeIntervals._TimeIntervalInfo[0]]),
                    DynamicTimeIntervalsSet(key=2, intervals=[_TimeIntervals._TimeIntervalInfo[0]]),
                    DynamicTimeIntervalsSet(key=3, intervals=[_TimeIntervals._TimeIntervalInfo[0]]),
                    DynamicTimeIntervalsSet(key=4, intervals=[_TimeIntervals._TimeIntervalInfo[0]]),
                    DynamicTimeIntervalsSet(key=5, intervals=[_TimeIntervals._TimeIntervalInfo[0]]),
                ],
            ),
            DynamicWarehouseToWarehouseInfo(warehouse_from=146, warehouse_to=146),
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=145,
                delivery_service_id=103,
                operation_time=0,
                date_switch_time_infos=[
                    DateSwitchTimeAndRegionInfo(
                        date_switch_hour=2,
                        region_to=225,
                        date_switch_time=TimeInfo(19, 0),
                        packaging_time=TimeInfo(3, 30),
                    )
                ],
            ),
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=146,
                delivery_service_id=104,
                operation_time=0,
                date_switch_time_infos=[
                    DateSwitchTimeAndRegionInfo(
                        date_switch_hour=2,
                        region_to=225,
                        date_switch_time=TimeInfo(19, 0),
                        packaging_time=TimeInfo(3, 30),
                    )
                ],
            ),
        ]
        cls.dynamic.lms += [
            DynamicWarehouseInfo(id=145, home_region=MOSCOW_RIDS, holidays_days_set_key=1),
            DynamicWarehouseInfo(
                id=146,
                home_region=213,
                is_express=True,
            ),
            DynamicDeliveryServiceInfo(id=100, name="Test Service"),
            DynamicDeliveryServiceInfo(id=101, name="Test Express Service"),
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=145,
                delivery_service_id=100,
                operation_time=0,
                date_switch_time_infos=[
                    DateSwitchTimeAndRegionInfo(
                        date_switch_hour=2,
                        region_to=225,
                        date_switch_time=TimeInfo(19, 0),
                        packaging_time=TimeInfo(3, 30),
                    )
                ],
            ),
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=146,
                delivery_service_id=101,
                operation_time=0,
                date_switch_time_infos=[
                    DateSwitchTimeAndRegionInfo(
                        date_switch_hour=2,
                        region_to=225,
                        date_switch_time=TimeInfo(19, 0),
                        packaging_time=TimeInfo(3, 30),
                    )
                ],
            ),
            DynamicWarehousesPriorityInRegion(region=225, warehouses=[145, 146]),
        ]
        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=MAIN_BUCKET_ID,
                dc_bucket_id=MAIN_BUCKET_ID,
                fesh=1,
                carriers=[100],
                regional_options=[
                    RegionalDelivery(
                        rid=MOSCOW_RIDS,
                        options=[DeliveryOption(day_from=1, day_to=5, price=50)],
                        payment_methods=[Payment.PT_CARD_ON_DELIVERY, Payment.PT_CASH_ON_DELIVERY, Payment.PT_YANDEX],
                    )
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
            DeliveryBucket(
                bucket_id=EXPRESS_BUCKET_ID,
                dc_bucket_id=EXPRESS_BUCKET_ID,
                fesh=2,
                carriers=[101],
                regional_options=[
                    RegionalDelivery(
                        rid=MOSCOW_RIDS,
                        options=[DeliveryOption(day_from=1, day_to=1, price=100)],
                        payment_methods=[Payment.PT_CARD_ON_DELIVERY, Payment.PT_CASH_ON_DELIVERY, Payment.PT_YANDEX],
                    )
                ],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
            ),
        ]

    @classmethod
    def prepare_nordstream(cls):
        cls.settings.nordstream_types = [
            0,
        ]  # only courier

    @classmethod
    def prepare_courier_options(cls):
        '''
        Ожидаем, что при одинаковых запросах в комби, отличающихся только значением поля b2b,
        мы получаем разные способы оплаты
        '''
        # запрос для b2b (соответствует cgi: available-for-business=1)
        b2b_user_info = create_user_info()
        b2b_user_info.b2b = True
        cls.combinator.on_courier_options_request(
            items=[
                DeliveryItem(
                    required_count=1,
                    weight=1000,
                    dimensions=[100, 200, 300],
                    cargo_types=[200],
                    offers=[CombinatorOffer(shop_sku='b2b.test.offer', shop_id=1, partner_id=145, available_count=1)],
                    price=100,
                )
            ],
            destination=Destination(region_id=MOSCOW_RIDS),
            payment_methods=[],
            total_price=100,
            user_info=b2b_user_info,
        ).respond_with_courier_options(
            options=[
                create_delivery_option(
                    cost=10,
                    delivery_service_id=100,
                    date_from=TODAY + timedelta(days=2),
                    date_to=TODAY + timedelta(days=5),
                    payment_methods=[PaymentMethod.PT_B2B_ACCOUNT_PREPAYMENT],
                )
            ]
        )
        # запрос для обычного маркета (соответствует отсутствующему cgi available-for-business или available-for-business=1)
        cls.combinator.on_courier_options_request(
            items=[
                DeliveryItem(
                    required_count=1,
                    weight=1000,
                    dimensions=[100, 200, 300],
                    cargo_types=[200],
                    offers=[CombinatorOffer(shop_sku='b2b.test.offer', shop_id=1, partner_id=145, available_count=1)],
                    price=100,
                )
            ],
            destination=Destination(region_id=MOSCOW_RIDS),
            payment_methods=[],
            total_price=100,
            user_info=create_user_info(),
        ).respond_with_courier_options(
            options=[
                create_delivery_option(
                    cost=10,
                    delivery_service_id=100,
                    date_from=TODAY + timedelta(days=2),
                    date_to=TODAY + timedelta(days=5),
                    payment_methods=[PaymentMethod.PT_CARD_ON_DELIVERY, PaymentMethod.PT_YANDEX],
                )
            ]
        )

    @classmethod
    def prepare_offers(cls):
        cls.index.mskus += [
            MarketSku(
                sku=100,
                title="b2b_test_offer",
                hyperid=1,
                waremd5='B2BSkuTest___________g',
                blue_offers=[
                    BlueOffer(
                        price=100,
                        vat=Vat.NO_VAT,
                        fesh=1,
                        feedid=1,
                        offerid='b2b.test.offer',
                        waremd5=B2B_OFFERID,
                        weight=1,
                        dimensions=OfferDimensions(length=100, width=200, height=300),
                        cargo_types=[200],
                        is_b2b=True,
                        is_fulfillment=False,
                    )
                ],
                delivery_buckets=[MAIN_BUCKET_ID],
            )
        ]

        start_sku = 101
        cls.index.mskus += [
            MarketSku(
                sku=start_sku + i,
                title=offer.offerid,
                hyperid=1,
                blue_offers=[offer],
                delivery_buckets=[MAIN_BUCKET_ID],
            )
            for i, offer in enumerate(vat_test_offers)
        ]
        cls.index.mskus += [
            MarketSku(
                sku=200,
                title=offer_promo.offerid,
                hyperid=1,
                blue_offers=[offer_promo],
                delivery_buckets=[MAIN_BUCKET_ID],
            )
        ]
        cls.index.mskus += [
            MarketSku(
                sku=300,
                title=offer_express.offerid,
                hyperid=1,
                blue_offers=[offer_express],
                delivery_buckets=[EXPRESS_BUCKET_ID],
            )
        ]

    @classmethod
    def prepare_promo(cls):
        promo = Promo(
            promo_type=PromoType.BLUE_FLASH,
            key='JVvklxUgdnawSJPG4UhZGA',
            shop_promo_id='promo_not_b2b',
            blue_flash=PromoBlueFlash(
                items=[
                    {
                        'feed_id': offer_promo.feedid,
                        'offer_id': offer_promo.offerid,
                        'price': {'value': offer_promo.price, 'currency': 'RUR'},
                        'old_price': {'value': offer_promo.price_old, 'currency': 'RUR'},
                    }
                ],
            ),
        )
        offer_promo.promo = [promo]
        cls.index.promos += [promo]
        cls.settings.loyalty_enabled = True
        cls.dynamic.loyalty += [DynamicBlueGenericBundlesPromos(whitelist=[promo.key])]

    def test_b2b_prepayment_from_combinator(self):
        '''
        В этом тесте проверяем, что новый способ оплаты B2B_ACCOUNT_PREPAYMENT успешно прокидывается из комбинатора,
        при этом он доступен только для контекста B2B (явно заданный параметр available-for-business=1)
        '''
        base_request = 'place=actual_delivery&' 'rids={rids}&' 'combinator=1&' 'offers-list={offers}:{offers_count}'
        b2b_flag_variants = [('', False), ('&available-for-business=0', False), ('&available-for-business=1', True)]
        for b2b_flag, b2b_enabled in b2b_flag_variants:
            request = base_request.format(rids=MOSCOW_RIDS, offers=B2B_OFFERID, offers_count=1) + b2b_flag
            response = self.report.request_json(request)
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "results": [
                            {
                                "entity": "deliveryGroup",
                                "delivery": {
                                    "options": [
                                        {
                                            "serviceId": "100",
                                            "paymentMethods": [
                                                "B2B_ACCOUNT_PREPAYMENT" if b2b_enabled else "CARD_ON_DELIVERY"
                                            ],
                                        }
                                    ]
                                },
                            }
                        ]
                    }
                },
            )

    def test_b2b_prepayment_from_combinator_no_vat(self):
        base_request = (
            'place=actual_delivery&'
            'rids={rids}&'
            'combinator=1&'
            'offers-list={offers}:{offers_count}'
            '&available-for-business=1'
            '&prices-no-vat=1'
        )
        request = base_request.format(rids=213, offers=B2B_OFFERID, offers_count=1)
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {"delivery": {"options": [{"price": {"value": "99", "valueWithoutVAT": "82.4999967"}}]}}
                    ]
                }
            },
        )

    def test_b2b_delivery_is_for_free_with_flag(self):
        '''
        Проверяем, что если проставлен available-for-business=1 и включен флаг market_enable_free_b2b_delivery=1
        то цена доставки равна нулю. В противном случаяе цена такая же как и в b2c.
        Проверяем ручки actual_delivery, productoffers и prime как для сценариев с комбинатором, так и без него.
        https://st.yandex-team.ru/MARKETOUT-47416
        '''

        place_to_additional_params = {
            'actual_delivery': '&offers-list={offers}:{offers_count}'.format(offers=B2B_OFFERID, offers_count=1),
            'productoffers': '&hyperid=1',
            'prime': '&hyperid=1',
        }
        for place_name, place_params in place_to_additional_params.items():
            for use_combinator in ['1', '0']:
                for is_b2b in ['1', '0']:
                    for free_delivery_flag in [True, False, None]:
                        request = (
                            'place={place_name}' '&rids=213' '&combinator={combinator}' '&available-for-business={b2b}'
                        ).format(place_name=place_name, combinator=use_combinator, b2b=is_b2b)
                        request += place_params
                        if free_delivery_flag is not None:
                            free_delivery_flag_value = '1' if free_delivery_flag else '0'
                            request += '&rearr-factors=market_enable_free_b2b_delivery=' + free_delivery_flag_value

                        expected_price = '99'
                        if is_b2b == '1' and free_delivery_flag is True:
                            expected_price = '0'

                        response = self.report.request_json(request)
                        self.assertFragmentIn(
                            response,
                            {
                                "search": {
                                    "results": [{"delivery": {"options": [{"price": {"value": expected_price}}]}}]
                                }
                            },
                        )

    def test_offer_prices_no_vat(self):
        '''
        Проверка цены оффера без НДС
        Ожидаем, что цена без НДС приходит в поле prices->valueWithoutVAT
        только при включенном параметре prices-no-vat=1
        Заодно проверяем логику расчёта цены без НДС из market/library/taxes/taxes.cpp
        '''
        base_request = 'place=offerinfo&' 'rids={rids}&' 'offerid={offerid}&' 'regset=2'

        prices_no_vat_variants = [('', False), ('&prices-no-vat=0', False), ('&prices-no-vat=1', True)]
        # перебираем все варианты параметра
        for prices_no_vat_flag, prices_no_vat_enabled in prices_no_vat_variants:
            # запрашиваем оффер в offerinfo, при prices-no-vat=1 ожидаем соответствующую цену без НДС
            for offer_id in offers_price_without_vat_map:
                request = base_request.format(rids=MOSCOW_RIDS, offerid=offer_id) + prices_no_vat_flag
                response = self.report.request_json(request)
                self.assertFragmentIn(
                    response,
                    {
                        "search": {
                            "results": [
                                {
                                    "entity": "offer",
                                    "wareId": offer_id,
                                    "prices": {
                                        "value": "100",
                                        "valueWithoutVAT": str(offers_price_without_vat_map[offer_id])
                                        if prices_no_vat_enabled
                                        else Absent(),
                                    },
                                }
                            ]
                        }
                    },
                )

    def test_delivery_price_no_vat_enabled(self):
        '''
        Проверка цены доставки без НДС
        Ожидаем, что цена без НДС приходит в опции доставки в поле price->valueWithoutVAT
        При этом для доставки ставка НДС сейчас фиксированная, не зависит от НДС оффера и составляет 20% (http://st.yandex-team.ru/MARKETOUT-44490)
        '''
        request = ('place=offerinfo&' 'rids={rids}&' 'offerid={offerid}&' 'regset=2&' 'prices-no-vat=1').format(
            rids=213, offerid=offer_vat_10.waremd5
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "entity": "offer",
                            "wareId": offer_vat_10.waremd5,
                            "delivery": {"options": [{"price": {"value": "99", "valueWithoutVAT": "82.17"}}]},
                        }
                    ]
                }
            },
        )

    def test_delivery_price_no_vat_disabled(self):
        '''
        Проверка цены доставки без НДС
        Ожидаем, что цена без НДС НЕ приходит при выключенном параметре prices-no-vat=0
        '''
        request = ('place=offerinfo&' 'rids={rids}&' 'offerid={offerid}&' 'regset=2&' 'prices-no-vat=0').format(
            rids=213, offerid=offer_vat_10.waremd5
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "entity": "offer",
                            "wareId": offer_vat_10.waremd5,
                            "delivery": {"options": [{"price": {"value": "99", "valueWithoutVAT": Absent()}}]},
                        }
                    ]
                }
            },
        )

    def test_delivery_price_no_vat_default(self):
        '''
        Проверка цены доставки без НДС
        Ожидаем, что цена без НДС приходит, если параметр не указан (поведение по умолчанию)
        '''
        request = ('place=offerinfo&' 'rids={rids}&' 'offerid={offerid}&' 'regset=2').format(
            rids=213, offerid=offer_vat_10.waremd5
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "entity": "offer",
                            "wareId": offer_vat_10.waremd5,
                            "delivery": {"options": [{"price": {"value": "99", "valueWithoutVAT": Absent()}}]},
                        }
                    ]
                }
            },
        )

    def test_b2b_no_promo(self):
        base_request = 'place=offerinfo' '&rids={rids}' '&offerid={offerid}' '&regset=2'
        b2b_flag_variants = [('', False), ('&available-for-business=0', False), ('&available-for-business=1', True)]
        for b2b_flag, b2b_enabled in b2b_flag_variants:
            request = base_request.format(rids=MOSCOW_RIDS, offerid=offer_promo.waremd5) + b2b_flag
            response = self.report.request_json(request)
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "results": [
                            {
                                "entity": "offer",
                                "wareId": offer_promo.waremd5,
                                "promo": {"shopPromoId": "promo_not_b2b"} if not b2b_enabled else Absent(),
                            }
                        ]
                    }
                },
            )

    def test_filters_when_b2b_disabled(self):
        '''
        Проверяем, что фильтры приходят, когда параметр available-for-business выключен
        (случай обычного маркета)
        '''
        show_old_filter_express_delivery_enabled = '&rearr-factors=show_old_filter_express_delivery=1'
        request = 'place=prime&hyperid=1&available-for-business=0' + show_old_filter_express_delivery_enabled
        response = self.report.request_json(request)
        for non_b2b_filter in non_b2b_filters:
            self.assertFragmentIn(response, {"filters": [non_b2b_filter]})

    def test_filters_when_b2b_enabled(self):
        '''
        Проверяем, что фильтры приходят, когда параметр available-for-business включен
        (случай B2B маркета)
        '''
        request = 'place=prime&hyperid=1&available-for-business=1'
        response = self.report.request_json(request)
        for non_b2b_filter in non_b2b_filters:
            self.assertFragmentNotIn(response, {"filters": [non_b2b_filter]})

    def test_filters_when_b2b_not_set(self):
        '''
        Проверяем поведение по умолчанию, параметр не указан
        (случай обычного маркета, фильтр не указан)
        '''
        show_old_filter_express_delivery_enabled = '&rearr-factors=show_old_filter_express_delivery=1'
        request = 'place=prime&hyperid=1' + show_old_filter_express_delivery_enabled

        response = self.report.request_json(request)
        for non_b2b_filter in non_b2b_filters:
            self.assertFragmentIn(response, {"filters": [non_b2b_filter]})

    def test_express_b2c(self):
        '''
        Проверяем, что для b2c экспресс-офферы доступны
        '''
        request = 'place=prime&hyperid=1&filter-express-delivery=1'
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "totalOffers": 1,
                    "results": [
                        {"entity": "offer", "wareId": "TestOfferExpress_____g", "delivery": {"isExpress": True}}
                    ],
                }
            },
        )

    def test_express_b2b_non_express(self):
        '''
        Проверяем, что для b2b экспресс отключен, оффер становится не экспрессным
        '''
        request = 'place=prime&hyperid=1&filter-express-delivery=1&available-for-business=1'
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "totalOffers": 1,
                    "results": [
                        {"entity": "offer", "wareId": "TestOfferExpress_____g", "delivery": {"isExpress": False}}
                    ],
                }
            },
        )

    def test_productoffers_default_offer_with_express_b2c(self):
        '''
        Проверяем, что на B2C по-прежнему доступен benefit:express-cpa
        '''
        request = 'place=productoffers&hyperid=1&offers-set=defaultList'
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {"search": {"results": [{"wareId": "TestOfferExpress_____g", "benefit": {"type": "express-cpa"}}]}},
            allow_different_len=True,
        )

    def test_productoffers_default_offer_with_express_b2b(self):
        '''
        Проверяем, что на B2B вместе с отключением экспресса пропадает и benefit:express-cpa
        '''
        request = 'place=productoffers&hyperid=1&offers-set=defaultList&available-for-business=1'
        response = self.report.request_json(request)
        self.assertFragmentNotIn(response, {"wareId": "TestOfferExpress_____g", "benefit": {"type": "express-cpa"}})

    @classmethod
    def prepare_jump_table_test(cls):
        cls.index.gltypes += [
            GLType(
                name=IPHONE.COLOR.RUS_NAME,
                xslname=IPHONE.COLOR.XSL_NAME,
                param_id=IPHONE.COLOR.ID,
                hid=IPHONE.ID,
                cluster_filter=True,
                model_filter_index=2,
                gltype=GLType.ENUM,
                values=[
                    GLValue(position=IPHONE.COLOR.BLACK_ID, value_id=IPHONE.COLOR.BLACK_ID, text='black'),
                    GLValue(position=IPHONE.COLOR.BLUE_ID, value_id=IPHONE.COLOR.BLUE_ID, text='blue'),
                    GLValue(position=IPHONE.COLOR.RED_ID, value_id=IPHONE.COLOR.RED_ID, text='red'),
                ],
            )
        ]

        cls.index.mskus += [
            make_sku_with_params(
                IPHONE.MODEL_ID,
                IPHONE.RED_SKU,
                [offer_iphone_red],
                [iphone_color(IPHONE.COLOR.RED_ID)],
                [iphone_color(IPHONE.COLOR.RED_ID)],
            ),
            make_sku_with_params(
                IPHONE.MODEL_ID,
                IPHONE.BLUE_SKU,
                [offer_iphone_blue],
                [iphone_color(IPHONE.COLOR.BLUE_ID)],
                [iphone_color(IPHONE.COLOR.BLUE_ID)],
            ),
            make_sku_with_params(
                IPHONE.MODEL_ID,
                IPHONE.BLACK_SKU,
                [offer_iphone_black],
                [iphone_color(IPHONE.COLOR.BLACK_ID)],
                [iphone_color(IPHONE.COLOR.BLACK_ID)],
            ),
        ]
        cls.index.models += [
            make_model(
                IPHONE.MODEL_ID,
                IPHONE.ID,
            )
        ]

    @classmethod
    def prepare_navtree_test(cls):
        cls.index.navtree += [NavCategory(nid=B2C_NAVNODE_ID, hid=B2C_CATEGORY)]
        cls.index.navtree_b2b += [NavCategory(nid=B2B_NAVNODE_ID, hid=B2B_CATEGORY)]
        cls.index.models += [
            Model(hyperid=B2C_HYPER_ID, hid=B2C_CATEGORY, title='B2C Model'),
            Model(hyperid=B2B_HYPER_ID, hid=B2B_CATEGORY, title='B2B Model'),
        ]
        offer_navtree_blue = BlueOffer(
            price=1000,
            vat=Vat.NO_VAT,
            fesh=1,
            feedid=1,
            offerid='offer.navtree.blue',
            waremd5='OfferNavtreeBlue_____g',
            is_b2b=False,
            is_fulfillment=False,
        )
        offer_navtree_b2b = BlueOffer(
            price=1100,
            vat=Vat.NO_VAT,
            fesh=1,
            feedid=1,
            offerid='offer.navtree.b2b',
            waremd5='OfferNavtreeB2B______g',
            is_b2b=True,
            is_fulfillment=False,
        )
        cls.index.mskus += [
            MarketSku(
                sku=8000,
                hyperid=B2C_HYPER_ID,
                title='B2C Tree SKU',
                blue_offers=[offer_navtree_blue],
                delivery_buckets=[MAIN_BUCKET_ID],
            ),
            MarketSku(
                sku=9000,
                hyperid=B2B_HYPER_ID,
                title='B2B Tree SKU',
                blue_offers=[offer_navtree_b2b],
                delivery_buckets=[MAIN_BUCKET_ID],
            ),
        ]

    def test_gl_filter_b2c(self):
        '''
        Проверяем работу GL фильтров в B2C маркетплейсе (отсутствует параметр available-for-business)
        Фильтрации нет, все значения фильтра доступны
        '''
        request = 'place=productoffers&hyperid=7890&hid={hid}'.format(hid=IPHONE.ID)
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {
                        "id": "234",
                        "values": [
                            {"found": 1, "value": "black", "id": "1"},
                            {"found": 1, "value": "blue", "id": "2"},
                            {"found": 1, "value": "red", "id": "3"},
                        ],
                    }
                ]
            },
        )

    def test_gl_filter_without_b2b(self):
        '''
        Проверяем работу GL фильтров в B2C маркетплейсе  (available-for-business ясно отключен, выставлен в 0)
        Фильтрации нет, все значения фильтра доступны
        '''
        request = 'place=productoffers&hyperid={model}&hid={hid}'.format(model=IPHONE.MODEL_ID, hid=IPHONE.ID)
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {
                        "id": "234",
                        "values": [
                            {"found": 1, "value": "black", "id": "1"},
                            {"found": 1, "value": "blue", "id": "2"},
                            {"found": 1, "value": "red", "id": "3"},
                        ],
                    }
                ]
            },
        )

    def test_gl_filter_with_b2b(self):
        '''
        Проверяем работу GL фильтров в B2B маркетплейсе (включен параметр available-for-business)
        B2C офферы отфильтровываются, поэтому из фильтра должно пропасть соответствующее значение
        '''
        request = 'place=productoffers&hyperid={model}&hid={hid}&available-for-business=1'.format(
            model=IPHONE.MODEL_ID, hid=IPHONE.ID
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "filters": [
                    {"id": "glprice"},
                    {
                        "id": "234",
                        "values": [
                            {"initialFound": 1, "found": 1, "value": "blue", "id": "2"},
                            {"initialFound": 1, "found": 1, "value": "red", "id": "3"},
                        ],
                    },
                    {"id": "manufacturer_warranty"},
                ]
            },
            allow_different_len=False,
        )

    def test_payment_methods_b2c(self):
        request = 'place=productoffers&hid={hid}&hyperid={model}&use-virt-shop=0&rids={rids}'.format(
            hid=IPHONE.ID, model=IPHONE.MODEL_ID, rids=213
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "wareId": "OfferIphoneRed_______g",
                            "payments": {
                                "deliveryCard": True,
                                "deliveryCash": True,
                                "prepaymentCard": True,
                                "prepaymentOther": True,
                            },
                        },
                        {
                            "wareId": "OfferIphoneBlue______g",
                            "payments": {
                                "deliveryCard": True,
                                "deliveryCash": True,
                                "prepaymentCard": True,
                                "prepaymentOther": True,
                            },
                        },
                        {
                            "wareId": "OfferIphoneBlack_____g",
                            "payments": {
                                "deliveryCard": True,
                                "deliveryCash": True,
                                "prepaymentCard": True,
                                "prepaymentOther": True,
                            },
                        },
                    ]
                }
            },
        )

    def test_payment_methods_b2b(self):
        request = (
            'place=productoffers&hid={hid}&hyperid={model}&use-virt-shop=0&rids={rids}&available-for-business=1'.format(
                hid=IPHONE.ID, model=IPHONE.MODEL_ID, rids=213
            )
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "search": {
                    "results": [
                        {
                            "wareId": "OfferIphoneRed_______g",
                            "payments": {
                                "deliveryCard": False,
                                "deliveryCash": False,
                                "prepaymentCard": False,
                                "prepaymentOther": False,
                                "prepaymentB2B": True,
                            },
                        },
                        {
                            "wareId": "OfferIphoneBlue______g",
                            "payments": {
                                "deliveryCard": False,
                                "deliveryCash": False,
                                "prepaymentCard": False,
                                "prepaymentOther": False,
                                "prepaymentB2B": True,
                            },
                        },
                    ]
                }
            },
        )

    def test_navigation_tree_b2c(self):
        '''
        Проверяем, что в B2C маркете продолжаем использовать белое дерево,
        а узлы B2B дерева недоступны
        '''

        b2c_expected_response = {
            "search": {
                "results": [
                    {"entity": "product", "navnodes": [{"id": B2C_NAVNODE_ID}]},
                    {"entity": "offer", "navnodes": [{"id": B2C_NAVNODE_ID}]},
                ]
            }
        }

        b2c_unwanted_response = {"navnodes": [{"id": B2B_NAVNODE_ID}]}

        hid_request = 'place=prime&hid={hid}'
        b2c_hid_request = hid_request.format(hid=B2C_CATEGORY)
        response = self.report.request_json(b2c_hid_request)
        self.assertFragmentIn(response, b2c_expected_response)
        b2b_hid_request = hid_request.format(hid=B2B_CATEGORY)
        response = self.report.request_json(b2b_hid_request)
        self.assertFragmentNotIn(response, b2c_unwanted_response)

        # проверяем, что наличие и значение rearr-флага market_use_b2b_navtree не влияет на результат в b2c маркете
        for rearr in ['', '&rearr-factors=market_use_b2b_navtree=0', '&rearr-factors=market_use_b2b_navtree=1']:
            response = self.report.request_json(b2c_hid_request + rearr)
            self.assertFragmentIn(response, b2c_expected_response)
            response = self.report.request_json(b2b_hid_request + rearr)
            self.assertFragmentNotIn(response, b2c_unwanted_response)

    def test_navigation_tree_b2b(self):
        '''
        Проверяем, что в B2B маркете используется ТОЛЬКО собственное дерево
        '''
        hid_request = 'place=prime&hid={hid}&available-for-business=1'
        for rearr in ['&rearr-factors=market_use_b2b_navtree=1']:
            b2b_hid_request = hid_request.format(hid=B2B_CATEGORY) + rearr
            response = self.report.request_json(b2b_hid_request)
            self.assertFragmentIn(
                response,
                {
                    "search": {
                        "results": [
                            {"entity": "product", "navnodes": [{"id": B2B_NAVNODE_ID}]},
                            {"entity": "offer", "navnodes": [{"id": B2B_NAVNODE_ID}]},
                        ]
                    }
                },
            )

        b2c_hid_request = hid_request.format(hid=B2C_CATEGORY) + '&rearr-factors=market_use_b2b_navtree=1'
        response = self.report.request_json(b2c_hid_request)
        self.assertFragmentNotIn(response, {"navnodes": [{"id": B2C_NAVNODE_ID}]})

        # проверяем, что при отключенном флаге b2b дерево не подгружается
        b2b_hid_request = hid_request.format(hid=B2B_CATEGORY) + '&rearr-factors=market_use_b2b_navtree=0'
        response = self.report.request_json(b2b_hid_request)
        self.assertFragmentNotIn(response, {"navnodes": [{"id": B2B_NAVNODE_ID}]})

    @classmethod
    def prepare_category_redirect_test(cls):
        cls.index.hypertree += [
            HyperCategory(
                name="туалетный утёнок",
                uniq_name="туалетный утёнок",
                hid=112233,
                children=[
                    HyperCategory(
                        hid=223344,
                    ),
                ],
            ),
        ]
        cls.index.mskus += [
            MarketSku(
                sku=3,
                hyperid=2233,
                title="туалетный утёнок",
                hid=112233,
                blue_offers=[BlueOffer(is_b2b=True), BlueOffer(is_b2b=False)],
            ),
        ]
        cls.matrixnet.on_place(MnPlace.CATEGORY_REDIRECT, 112233).respond(0.9)

    def test_no_catalog_redirects_for_b2b(self):
        request_base = (
            'place=prime'
            '&cvredirect=1'
            '&text=туалетный+утёнок'
            '&available-for-business={is_b2b}'
            '&debug=da'
            '&rearr-factors=market_use_b2b_navtree=1'
        )
        for is_b2b, target in [(1, "search"), (0, "catalog")]:
            response = self.report.request_json(request_base.format(is_b2b=is_b2b))
            self.assertFragmentIn(
                response,
                {
                    "redirect": {
                        "target": target,
                    }
                },
            )


if __name__ == '__main__':
    main()
