#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

import datetime

from core.types import (
    Currency,
    DeliveryBucket,
    DeliveryCalcFeedInfo,
    DeliveryOption,
    Outlet,
    OutletDeliveryOption,
    PickupBucket,
    PickupOption,
    RegionalDelivery,
    Shop,
)

from market.pylibrary.const.payment_methods import (
    PaymentMethod,
)

from core.matcher import Absent
from core.testcase import TestCase, main
from core.types.payment_methods import Payment
from core.types.taxes import Tax
from core.types.delivery import BlueDeliveryTariff, OutletType

from core.types.combinator import (
    CombinatorOffer,
    create_delivery_option,
    create_virtual_box,
    DeliveryItem,
    Destination,
    PickupPointGrouped,
    PointTimeInterval,
)

# This date is fixed for all test
TODAY = datetime.date(2020, 5, 18)
DATE_1 = TODAY + datetime.timedelta(days=1)
DATE_2 = TODAY + datetime.timedelta(days=2)
DATE_3 = TODAY + datetime.timedelta(days=3)
DATE_4 = TODAY + datetime.timedelta(days=4)
DATE_5 = TODAY + datetime.timedelta(days=5)

MOSCOW_RID = 213
ROSTOV_RID = 39
EKB_RID = 54
PITER_RID = 2
OTHER_RID = 100500
VOLOGDA_RID = 21  # В этом регионе разные цены для разных типов доставки
KIROV_RID = 46  # В этом регионе доступна доставка только курьером и почтой, а тяжелые только курьером
MITINO_RID = 20569  # В этом регионе доступна доставка только в ПВЗ
SAMARA_RID = 51  # В этом регионе увеличенный порог КГТ доставки - 30 кг
UNIFIED_RID = 100  # Регион с унифицированными тарифами

# Список складов сейчас захардкожен в логике плэйса
ROSTOV_WH = 147
TOMILINO_WH = 171
SOFINO_WH = 172

DEFAULT_KGT_WEIGHT_THRESHOLD = 20
SAMARA_KGT_WEIGHT_THRESHOLD = 30

ALL_WH = [ROSTOV_WH, TOMILINO_WH, SOFINO_WH]

# Образец цен доставки
# Первый набор городов - это Москва
FIRST_LIGHT = [
    {
        'entity': 'deliveryRate',
        'price': {
            'currency': 'RUR',
            'value': '99',
        },
        'minOrderPrice': Absent(),
        'maxOrderPrice': {
            'currency': 'RUR',
            'value': '799',
        },
    },
    {
        'entity': 'deliveryRate',
        'price': {
            'currency': 'RUR',
            'value': '49',
        },
        'minOrderPrice': {
            'currency': 'RUR',
            'value': '799',
        },
        'maxOrderPrice': {
            'currency': 'RUR',
            'value': '2999',
        },
    },
    {
        'entity': 'deliveryRate',
        'price': {
            'currency': 'RUR',
            'value': '1',
        },
        'minOrderPrice': {
            'currency': 'RUR',
            'value': '2999',
        },
        'maxOrderPrice': Absent(),
    },
]

FIRST_HEAVY = [
    {
        'entity': 'deliveryRate',
        'price': {
            'currency': 'RUR',
            'value': '555',
        },
        'minOrderPrice': Absent(),
        'maxOrderPrice': Absent(),
    }
]

# Первый набор городов - это Ростов
SECOND_LIGHT = [
    {
        'entity': 'deliveryRate',
        'price': {
            'currency': 'RUR',
            'value': '123',
        },
        'minOrderPrice': Absent(),
        'maxOrderPrice': {
            'currency': 'RUR',
            'value': '2499',
        },
    },
    {
        'entity': 'deliveryRate',
        'price': {
            'currency': 'RUR',
            'value': '101',
        },
        'minOrderPrice': {
            'currency': 'RUR',
            'value': '2499',
        },
        'maxOrderPrice': Absent(),
    },
]

SECOND_HEAVY = [
    {
        'entity': 'deliveryRate',
        'price': {
            'currency': 'RUR',
            'value': '777',
        },
        'minOrderPrice': Absent(),
        'maxOrderPrice': {
            'currency': 'RUR',
            'value': '2499',
        },
    },
    {
        'entity': 'deliveryRate',
        'price': {
            'currency': 'RUR',
            'value': '666',
        },
        'minOrderPrice': {
            'currency': 'RUR',
            'value': '2499',
        },
        'maxOrderPrice': Absent(),
    },
]

# Остальные города
OTHER_LIGHT = [
    {
        'entity': 'deliveryRate',
        'price': {
            'currency': 'RUR',
            'value': '888',
        },
        'minOrderPrice': Absent(),
        'maxOrderPrice': {
            'currency': 'RUR',
            'value': '1234',
        },
    },
    {
        'entity': 'deliveryRate',
        'price': {
            'currency': 'RUR',
            'value': '654',
        },
        'minOrderPrice': {
            'currency': 'RUR',
            'value': '1234',
        },
        'maxOrderPrice': Absent(),
    },
]

OTHER_HEAVY = OTHER_LIGHT  # Тариф легких посылок и КГТ одинаковый

# Особенная цена для экспресс в Вологде
VOLOGDA_EXPRESS = [
    {
        'entity': 'deliveryRate',
        'price': {
            'currency': 'RUR',
            'value': '350',
        },
        'minOrderPrice': Absent(),
        'maxOrderPrice': {
            'currency': 'RUR',
            'value': '5000',
        },
    },
    {
        'entity': 'deliveryRate',
        'price': {
            'currency': 'RUR',
            'value': '99',
        },
        'minOrderPrice': {
            'currency': 'RUR',
            'value': '5000',
        },
        'maxOrderPrice': Absent(),
    },
]

# Особенная цена для почты в Вологде
VOLOGDA_POST = [
    {
        'entity': 'deliveryRate',
        'price': {
            'currency': 'RUR',
            'value': '987',
        },
        'minOrderPrice': Absent(),
        'maxOrderPrice': {
            'currency': 'RUR',
            'value': '5000',
        },
    },
    {
        'entity': 'deliveryRate',
        'price': {
            'currency': 'RUR',
            'value': '0',
        },
        'minOrderPrice': {
            'currency': 'RUR',
            'value': '5000',
        },
        'maxOrderPrice': Absent(),
    },
]

VOLOGDA_COURIER_PICKUP = [
    {
        'entity': 'deliveryRate',
        'price': {
            'currency': 'RUR',
            'value': '987',
        },
        'minOrderPrice': Absent(),
        'maxOrderPrice': Absent(),
    }
]


def create_region(rid, day_from, day_to):
    return RegionalDelivery(
        rid=rid,
        options=[DeliveryOption(day_from=day_from, day_to=day_to, shop_delivery_price=5)],
        payment_methods=[Payment.PT_YANDEX, Payment.PT_CASH_ON_DELIVERY, Payment.PT_CARD_ON_DELIVERY],
    )


def create_courier_bucket(id, regions):
    return DeliveryBucket(
        bucket_id=id,
        dc_bucket_id=id,
        fesh=1,
        carriers=[10],
        regional_options=regions,
        delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
    )


class BUCKETS(object):
    '''
    Этот бакет перенесен в комбинатор. Для проверки работоспособности ответа от комбинатора
    MOSCOW_COURIER_FAST = create_courier_bucket(
        id=1,
        regions=[
            create_region(MOSCOW_RID, 1, 2),
        ],
    )
    '''

    MOSCOW_COURIER = create_courier_bucket(
        id=2,
        regions=[
            create_region(MOSCOW_RID, 10, 20),
        ],
    )
    EKB_COURIER = create_courier_bucket(
        id=3,
        regions=[
            create_region(EKB_RID, 8, 9),
        ],
    )
    EKB_FROM_ROSTOV_COURIER = create_courier_bucket(
        id=4,
        regions=[
            create_region(EKB_RID, 3, 10),
        ],
    )
    ROSTOV_COURIER = create_courier_bucket(
        id=5,
        regions=[
            create_region(ROSTOV_RID, 2, 5),
        ],
    )
    PITER_COURIER = create_courier_bucket(
        id=6,
        regions=[
            create_region(PITER_RID, 7, 8),
        ],
    )
    UNUSED_COURIER = create_courier_bucket(
        id=7,
        regions=[
            create_region(OTHER_RID, 6, 6),
        ],
    )
    VOLOGDA_COURIER = create_courier_bucket(
        id=8,
        regions=[
            create_region(VOLOGDA_RID, 7, 8),
        ],
    )
    KIROV_COURIER = create_courier_bucket(
        id=9,
        regions=[
            create_region(KIROV_RID, 7, 8),
        ],
    )
    UNIFIED_COURIER = create_courier_bucket(
        id=10,
        regions=[
            create_region(UNIFIED_RID, 1, 2),
        ],
    )


def OUTLET(id, rid, type=Outlet.FOR_PICKUP):
    return Outlet(
        point_id=id,
        delivery_service_id=10,
        region=rid,
        point_type=type,
        delivery_option=OutletDeliveryOption(
            shipper_id=10, day_from=1, day_to=1, order_before=23, work_in_holiday=True
        ),
        working_days=[i for i in range(40)],
        bool_props=["prepayAllowed", "cashAllowed", "cardAllowed"],
    )


def PICKUP_BUCKET(id, outlet_id, day_from, day_to):
    return PickupBucket(
        bucket_id=id,
        dc_bucket_id=id,
        carriers=[10],
        options=[
            PickupOption(
                outlet_id=outlet_id,
                day_from=day_from,
                day_to=day_to,
            )
        ],
        delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
    )


def POST_OUTLET(**kwargs):
    return OUTLET(type=Outlet.FOR_POST, **kwargs)


class OUTLETS(object):
    MOSCOW_OUTLET_1 = OUTLET(id=101, rid=MOSCOW_RID)
    '''
    Скрываю бакет в пользу ответа комбинатора
    MOSCOW_P_BUCKET_1_MARSHRUT = PICKUP_BUCKET(outlet_id=101, id=1011, day_from=2, day_to=4)
    '''

    MOSCOW_OUTLET_2 = OUTLET(id=102, rid=MOSCOW_RID)
    MOSCOW_P_BUCKET_2_MARSHRUT = PICKUP_BUCKET(outlet_id=102, id=1021, day_from=3, day_to=5)

    ROSTOV_OUTLET_1 = OUTLET(id=111, rid=ROSTOV_RID)
    ROSTOV_P_BUCKET_1_MARSHRUT = PICKUP_BUCKET(outlet_id=111, id=1111, day_from=1, day_to=3)
    ROSTOV_P_BUCKET_1_ROSTOV = PICKUP_BUCKET(outlet_id=111, id=1112, day_from=5, day_to=7)

    EKB_POST_1 = POST_OUTLET(id=121, rid=EKB_RID)
    EKB_POST_BUCKET_1_SOFINO = PICKUP_BUCKET(outlet_id=121, id=1211, day_from=17, day_to=18)

    VOLOGDA_OUTLET = OUTLET(id=131, rid=VOLOGDA_RID)
    VOLOGDA_OUTLET_BUCKET = PICKUP_BUCKET(outlet_id=131, id=1311, day_from=2, day_to=4)

    VOLOGDA_POST = POST_OUTLET(id=132, rid=VOLOGDA_RID)
    VOLOGDA_POST_BUCKET = PICKUP_BUCKET(outlet_id=132, id=1312, day_from=2, day_to=4)

    KIROV_POST = POST_OUTLET(id=141, rid=KIROV_RID)
    KIROV_POST_BUCKET = PICKUP_BUCKET(outlet_id=141, id=1411, day_from=2, day_to=4)

    MITINO_OUTLET = OUTLET(id=151, rid=MITINO_RID)
    MITINO_OUTLET_BUCKET = PICKUP_BUCKET(outlet_id=151, id=1511, day_from=2, day_to=4)

    SAMARA_OUTLET = OUTLET(id=161, rid=SAMARA_RID)
    SAMARA_P_BUCKET = PICKUP_BUCKET(outlet_id=161, id=1611, day_from=2, day_to=4)

    UNIFIED_OUTLET = OUTLET(id=171, rid=UNIFIED_RID)
    UNIFIED_P_BUCKET = PICKUP_BUCKET(outlet_id=171, id=1711, day_from=1, day_to=2)


class T(TestCase):
    @classmethod
    def beforePrepare(cls):
        cls.settings.delivery_calendar_start_date = datetime.date(day=18, month=5, year=2020)

    @classmethod
    def prepare(cls):
        # НЕ делайте так в новых тестах!
        # Походов в КД на проде уже нет, пожалуйста, проверяйте новую функциональность, создавая доставку через комбинатор
        cls.settings.default_search_experiment_flags += ['force_disable_delivery_calculator_requests=0']

        # Current date 18/05/2020 @ 23:16 MSK
        cls.settings.microseconds_for_disabled_random = 1589833013000000

        cls.index.shops += [
            Shop(
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
                    OUTLETS.MOSCOW_OUTLET_1.point_id,
                    OUTLETS.MOSCOW_OUTLET_2.point_id,
                    OUTLETS.ROSTOV_OUTLET_1.point_id,
                    OUTLETS.EKB_POST_1.point_id,
                    OUTLETS.VOLOGDA_OUTLET.point_id,
                    OUTLETS.VOLOGDA_POST.point_id,
                    OUTLETS.KIROV_POST.point_id,
                    OUTLETS.MITINO_OUTLET.point_id,
                    OUTLETS.SAMARA_OUTLET.point_id,
                    OUTLETS.UNIFIED_OUTLET.point_id,
                ],
            )
        ]

        cls.index.shops += [
            Shop(
                fesh=id,
                datafeed_id=id,
                warehouse_id=id,
                currency=Currency.RUR,
                tax_system=Tax.OSN,
                supplier_type=Shop.FIRST_PARTY,
                blue='REAL',
            )
            for id in ALL_WH
        ]

    @classmethod
    def prepare_courier_buckets(cls):
        cls.index.delivery_buckets += [
            # BUCKETS.MOSCOW_COURIER_FAST,
            BUCKETS.MOSCOW_COURIER,
            BUCKETS.EKB_COURIER,
            BUCKETS.EKB_FROM_ROSTOV_COURIER,
            BUCKETS.ROSTOV_COURIER,
            BUCKETS.PITER_COURIER,
            BUCKETS.UNUSED_COURIER,
            BUCKETS.VOLOGDA_COURIER,
            BUCKETS.KIROV_COURIER,
            BUCKETS.UNIFIED_COURIER,
        ]

    @classmethod
    def prepare_outlets(cls):
        cls.index.outlets += [
            OUTLETS.MOSCOW_OUTLET_1,
            OUTLETS.MOSCOW_OUTLET_2,
            OUTLETS.ROSTOV_OUTLET_1,
            OUTLETS.EKB_POST_1,
            OUTLETS.VOLOGDA_OUTLET,
            OUTLETS.VOLOGDA_POST,
            OUTLETS.KIROV_POST,
            OUTLETS.MITINO_OUTLET,
            OUTLETS.SAMARA_OUTLET,
            OUTLETS.UNIFIED_OUTLET,
        ]

        cls.index.pickup_buckets += [
            # OUTLETS.MOSCOW_P_BUCKET_1_MARSHRUT,
            OUTLETS.MOSCOW_P_BUCKET_2_MARSHRUT,
            OUTLETS.ROSTOV_P_BUCKET_1_MARSHRUT,
            OUTLETS.ROSTOV_P_BUCKET_1_ROSTOV,
            OUTLETS.EKB_POST_BUCKET_1_SOFINO,
            OUTLETS.VOLOGDA_OUTLET_BUCKET,
            OUTLETS.VOLOGDA_POST_BUCKET,
            OUTLETS.KIROV_POST_BUCKET,
            OUTLETS.MITINO_OUTLET_BUCKET,
            OUTLETS.SAMARA_P_BUCKET,
            OUTLETS.UNIFIED_P_BUCKET,
        ]

    @classmethod
    def prepare_delivery_calculator(cls):
        cls.index.delivery_calc_feed_info += [
            DeliveryCalcFeedInfo(feed_id=ROSTOV_WH, generation_id=1, warehouse_id=ROSTOV_WH),
            DeliveryCalcFeedInfo(feed_id=TOMILINO_WH, generation_id=1, warehouse_id=TOMILINO_WH),
            DeliveryCalcFeedInfo(feed_id=SOFINO_WH, generation_id=1, warehouse_id=SOFINO_WH),
        ]

        # Легкие посылки
        cls.delivery_calc.on_request_offer_buckets(
            warehouse_id=SOFINO_WH, weight=1, width=1, height=1, length=1, program_type=[4]  # MARKET_DELIVERY_PROGRAM
        ).respond(
            [  # Курьерские бакеты
                # BUCKETS.MOSCOW_COURIER_FAST.dc_bucket_id,
                BUCKETS.MOSCOW_COURIER.dc_bucket_id,
                BUCKETS.VOLOGDA_COURIER.dc_bucket_id,
                BUCKETS.KIROV_COURIER.dc_bucket_id,
                BUCKETS.UNIFIED_COURIER.dc_bucket_id,
                BUCKETS.PITER_COURIER.dc_bucket_id,
            ],
            [  # ПВЗ
                # OUTLETS.MOSCOW_P_BUCKET_1_MARSHRUT.dc_bucket_id,
                OUTLETS.MOSCOW_P_BUCKET_2_MARSHRUT.dc_bucket_id,
                OUTLETS.ROSTOV_P_BUCKET_1_MARSHRUT.dc_bucket_id,
                OUTLETS.VOLOGDA_OUTLET_BUCKET.dc_bucket_id,
                OUTLETS.MITINO_OUTLET_BUCKET.dc_bucket_id,
                OUTLETS.SAMARA_P_BUCKET.dc_bucket_id,
                OUTLETS.UNIFIED_P_BUCKET.dc_bucket_id,
            ],
            [  # Почта
                OUTLETS.VOLOGDA_POST_BUCKET.dc_bucket_id,
                OUTLETS.KIROV_POST_BUCKET.dc_bucket_id,
                OUTLETS.EKB_POST_BUCKET_1_SOFINO.dc_bucket_id,
            ],
        )
        cls.delivery_calc.on_request_offer_buckets(
            warehouse_id=ROSTOV_WH, weight=1, width=1, height=1, length=1
        ).respond(
            [BUCKETS.ROSTOV_COURIER.dc_bucket_id, BUCKETS.EKB_FROM_ROSTOV_COURIER.dc_bucket_id],
            [OUTLETS.ROSTOV_P_BUCKET_1_ROSTOV.dc_bucket_id],
            [],
        )
        cls.delivery_calc.on_request_offer_buckets(
            warehouse_id=TOMILINO_WH, weight=1, width=1, height=1, length=1
        ).respond(
            [
                # BUCKETS.MOSCOW_COURIER_FAST.dc_bucket_id,
                BUCKETS.EKB_COURIER.dc_bucket_id
            ],
            [],
            [],
        )
        # Тяжелые бакеты
        for parcel_weight in (DEFAULT_KGT_WEIGHT_THRESHOLD, SAMARA_KGT_WEIGHT_THRESHOLD):
            cls.delivery_calc.on_request_offer_buckets(
                warehouse_id=SOFINO_WH, weight=parcel_weight, width=1, height=1, length=1
            ).respond(
                [
                    BUCKETS.MOSCOW_COURIER.dc_bucket_id,
                    BUCKETS.KIROV_COURIER.dc_bucket_id,
                    BUCKETS.VOLOGDA_COURIER.dc_bucket_id,
                    BUCKETS.UNIFIED_COURIER.dc_bucket_id,
                    BUCKETS.PITER_COURIER.dc_bucket_id,
                ],
                [
                    # OUTLETS.MOSCOW_P_BUCKET_1_MARSHRUT.dc_bucket_id,
                    OUTLETS.VOLOGDA_OUTLET_BUCKET.dc_bucket_id,
                    OUTLETS.SAMARA_P_BUCKET.dc_bucket_id,
                    OUTLETS.UNIFIED_P_BUCKET.dc_bucket_id,
                ],
                [
                    OUTLETS.VOLOGDA_POST_BUCKET.dc_bucket_id,
                ],
            )
            cls.delivery_calc.on_request_offer_buckets(
                warehouse_id=ROSTOV_WH, weight=parcel_weight, width=1, height=1, length=1
            ).respond([BUCKETS.ROSTOV_COURIER.dc_bucket_id], [OUTLETS.ROSTOV_P_BUCKET_1_ROSTOV.dc_bucket_id], [])
            cls.delivery_calc.on_request_offer_buckets(
                warehouse_id=TOMILINO_WH, weight=parcel_weight, width=1, height=1, length=1
            ).respond(
                [],
                [],
                [],
            )

    @classmethod
    def prepare_blue_delivery_modifiers(cls):
        '''
        Подготавливаем параметры цены доставки для пользователя
        '''
        cls.index.blue_delivery_modifiers.add_modifier(
            tariffs=[
                BlueDeliveryTariff(user_price=99, large_size=0, price_to=799),
                BlueDeliveryTariff(user_price=49, large_size=0, price_to=2999),
                BlueDeliveryTariff(user_price=1, large_size=0),
                BlueDeliveryTariff(user_price=9999, large_size=1, weight_threshold=5555),
                BlueDeliveryTariff(user_price=555),
            ],
            regions=[MOSCOW_RID],
            market_outlet_threshold=1111,
            ya_plus_threshold=321,
            tier="tier_1",
        )
        cls.index.blue_delivery_modifiers.add_modifier(
            tariffs=[
                BlueDeliveryTariff(user_price=123, large_size=0, price_to=2499),
                BlueDeliveryTariff(user_price=101, large_size=0),
                BlueDeliveryTariff(user_price=777, price_to=2499),
                BlueDeliveryTariff(user_price=666),
            ],
            regions=[ROSTOV_RID],
            beru_bonus_threshold=2345,
        )
        cls.index.blue_delivery_modifiers.add_modifier(
            tariffs=[
                BlueDeliveryTariff(user_price=987, price_to=5000),
                # Особенная цена для почты от 5000 рублей
                BlueDeliveryTariff(user_price=987, post_price=0),
            ],
            regions=[VOLOGDA_RID],
        )
        cls.index.blue_delivery_modifiers.set_default_modifier(
            tariffs=[
                # Тариф одинаковый, как для обычных посылок, так и для КГТ
                BlueDeliveryTariff(user_price=888, price_to=1234),
                BlueDeliveryTariff(user_price=654),
            ],
        )
        cls.index.blue_delivery_modifiers.add_modifier(
            tariffs=[
                BlueDeliveryTariff(user_price=99, price_to=799),
                BlueDeliveryTariff(user_price=49, price_to=2999),
                BlueDeliveryTariff(user_price=1),
            ],
            regions=[MOSCOW_RID],
            fast_delivery=True,
            tier="tier_1",
        )
        cls.index.blue_delivery_modifiers.add_modifier(
            tariffs=[
                BlueDeliveryTariff(user_price=350, price_to=5000),
                BlueDeliveryTariff(user_price=99),
            ],
            regions=[VOLOGDA_RID],
            fast_delivery=True,
        )
        cls.index.blue_delivery_modifiers.set_default_modifier(
            tariffs=[
                BlueDeliveryTariff(user_price=888, price_to=1234),
                BlueDeliveryTariff(user_price=654),
            ],
            fast_delivery=True,
        )
        cls.index.blue_delivery_modifiers.add_modifier(
            tariffs=[BlueDeliveryTariff(user_price=1, large_size=0), BlueDeliveryTariff(user_price=555)],
            regions=[SAMARA_RID],
            large_size_weight=SAMARA_KGT_WEIGHT_THRESHOLD,
        )

    def test_precheck(self):
        '''
        Проверяем генерацию ошибки, если не передали необходимые параметры
        '''
        request = 'place=blue_tariffs&rgb=blue'
        response = self.report.request_json(request)
        self.assertFragmentIn(response, {"error": {"code": "INVALID_USER_CGI", "message": "rids is required"}})
        self.error_log.expect(code=3043)

    def test_moscow_courier(self):
        '''
        В Москву легкие посылки курьер возит со склада Маршрут (двумя способами) и Томилино (быстрым).
        Комбинатор считает, что доставка быстрая 1-2 дня.
        Но Калькулятор доставки настроен на долгую доставку
        Поэтому в итоге показан срок доставки от 2х до 20 дней


        Тяжелые посылки доставляются из Маршрута (медленный) и Софьино (быстрый)
        Поэтому в итоге получаем объединение диапазонов
        Тарифы экспресса входят в ту же группу
        '''
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'
        request = 'place=blue_tariffs&rgb=blue&rids={}'.format(MOSCOW_RID) + unified_off_flags
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "results": {
                    "base": {
                        "deliveryTime": {"courier": {"dayFrom": 1, "dayTo": 20}},
                        "deliveryRates": FIRST_LIGHT,
                        "deliveryRatesGroups": [
                            {
                                "deliveryType": ["courier", "pickup", 'express'],
                                "deliveryRates": FIRST_LIGHT,
                            }
                        ],
                    },
                    "bulk": {
                        "deliveryTime": {"courier": {"dayFrom": 1, "dayTo": 20}},
                        "deliveryRates": FIRST_HEAVY,
                        "deliveryRatesGroups": [
                            {
                                "deliveryType": ["courier", "pickup"],
                                "deliveryRates": FIRST_HEAVY,
                            }
                        ],
                        "attributes": {"weight": DEFAULT_KGT_WEIGHT_THRESHOLD},
                    },
                    "special": {
                        "marketBrandedPickup": {
                            "threshold": {
                                "value": "1111",
                                "currency": "RUR",
                            }
                        },
                        "yaPlus": {
                            "threshold": {
                                "value": "321",
                                "currency": "RUR",
                            }
                        },
                        "newcomerFreeDelivery": Absent(),
                    },
                }
            },
            allow_different_len=False,
        )

    def test_moscow_pickup(self):
        '''
        В Москве есть два оутлета с разными сроками доставки
        Легкие посылки возятся в оба оутлета. Тяжелые только в один.
        Тарифы экспресса входят в ту же группу
        '''
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'
        request = 'place=blue_tariffs&rgb=blue&rids={}'.format(MOSCOW_RID) + unified_off_flags
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "results": {
                    "base": {
                        "deliveryTime": {"pickup": {"dayFrom": 2, "dayTo": 5}},
                        "deliveryRates": FIRST_LIGHT,
                        "deliveryRatesGroups": [
                            {
                                "deliveryType": ["courier", "pickup", 'express'],
                                "deliveryRates": FIRST_LIGHT,
                            }
                        ],
                    },
                    "bulk": {
                        "deliveryTime": {"pickup": {"dayFrom": 2, "dayTo": 4}},
                        "deliveryRates": FIRST_HEAVY,
                        "deliveryRatesGroups": [
                            {
                                "deliveryType": ["courier", "pickup"],
                                "deliveryRates": FIRST_HEAVY,
                            }
                        ],
                        "attributes": {"weight": DEFAULT_KGT_WEIGHT_THRESHOLD},
                    },
                }
            },
            allow_different_len=False,
        )

    def test_rostov_courier(self):
        '''
        В Ростов легкие посылки курьер возит со склада из Ростова.
        '''
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'
        request = 'place=blue_tariffs&rgb=blue&rids={}'.format(ROSTOV_RID) + unified_off_flags
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "results": {
                    "base": {
                        "deliveryTime": {"courier": {"dayFrom": 2, "dayTo": 5}},
                        "deliveryRates": SECOND_LIGHT,
                    },
                    "bulk": {
                        "deliveryTime": {"courier": {"dayFrom": 2, "dayTo": 5}},
                        "deliveryRates": SECOND_HEAVY,
                        "attributes": {"weight": DEFAULT_KGT_WEIGHT_THRESHOLD},
                    },
                    "special": {
                        "newcomerFreeDelivery": {
                            "applicable": True,
                            "threshold": {
                                "value": "2345",
                                "currency": "RUR",
                            },
                        }
                    },
                }
            },
        )

    def test_rostov_pickup(self):
        '''
        В Ростове есть один оутлет в который возят с двух складов
        '''
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'
        request = 'place=blue_tariffs&rgb=blue&rids={}'.format(ROSTOV_RID) + unified_off_flags
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "results": {
                    "base": {
                        "deliveryTime": {"pickup": {"dayFrom": 1, "dayTo": 7}},
                        "deliveryRates": SECOND_LIGHT,
                    },
                    "bulk": {
                        "deliveryTime": {"pickup": {"dayFrom": 5, "dayTo": 7}},
                        "deliveryRates": SECOND_HEAVY,
                        "attributes": {"weight": DEFAULT_KGT_WEIGHT_THRESHOLD},
                    },
                }
            },
        )

    def test_ekb(self):
        '''
        В Екб легкие посылки курьер возит со склада из Ростова и Томилино.
        Ростовский диапазон дат шире, чем Томилино. Поэтому будет показан именно он
        Так же для легких посылок есть почта
        Тяжелые посылки не возятся совсем
        '''
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'
        request = 'place=blue_tariffs&rgb=blue&rids={}'.format(EKB_RID) + unified_off_flags
        for disable_post_as_pickup in ['', '&rearr-factors=market_use_post_as_pickup=0']:
            response = self.report.request_json(request + disable_post_as_pickup)
            field_name = 'post' if disable_post_as_pickup else 'pickup'
            self.assertFragmentIn(
                response,
                {
                    "results": {
                        "base": {
                            "deliveryTime": {
                                "courier": {"dayFrom": 3, "dayTo": 10},
                                field_name: {"dayFrom": 17, "dayTo": 18},
                            },
                            "deliveryRates": OTHER_LIGHT,
                        },
                        "bulk": Absent(),
                    }
                },
            )

    def test_piter_courier(self):
        '''
        В Питер легкие и тяжелые посылки курьер возит из Софьино.
        '''
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'
        request = 'place=blue_tariffs&rgb=blue&rids={}'.format(PITER_RID) + unified_off_flags
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "results": {
                    "base": {
                        "deliveryTime": {"courier": {"dayFrom": 7, "dayTo": 8}},
                        "deliveryRates": OTHER_LIGHT,
                    },
                    "bulk": {
                        "deliveryTime": {"courier": {"dayFrom": 7, "dayTo": 8}},
                        "deliveryRates": OTHER_HEAVY,
                        "attributes": {"weight": DEFAULT_KGT_WEIGHT_THRESHOLD},
                    },
                }
            },
        )

    def test_other_delivery(self):
        '''
        Проверяем регион, в который никто не возит. Базовые и тяжелые опции не доступны
        '''
        request = 'place=blue_tariffs&rgb=blue&rids={}'.format(OTHER_RID)
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "results": {
                    "base": Absent(),
                    "bulk": Absent(),
                }
            },
        )

    def test_vologda(self):
        '''
        В Вологду цена доставки почтой отличается от других для посылок от 5000 рублей.
        Проверяем, что тариф почты имеет две записи, а тарифы курьерки и ПВЗ объединены
        Тарифы экспресса идут отдельной группой
        '''
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0;market_use_post_as_pickup=0'
        request = 'place=blue_tariffs&rgb=blue&rids={}'.format(VOLOGDA_RID) + unified_off_flags
        response = self.report.request_json(request)
        sample_base = {
            "deliveryRates": VOLOGDA_COURIER_PICKUP,  # DEPRECATED для совместимости это поле показывает базовую цену доставки
            "deliveryRatesGroups": [
                {
                    "deliveryType": ["courier", "pickup"],
                    "deliveryRates": VOLOGDA_COURIER_PICKUP,
                },
                {
                    "deliveryType": ["post"],
                    "deliveryRates": VOLOGDA_POST,
                },
                {
                    "deliveryType": ["express"],
                    "deliveryRates": VOLOGDA_EXPRESS,
                },
            ],
        }
        sample_bulk = {
            "deliveryRates": VOLOGDA_COURIER_PICKUP,  # DEPRECATED для совместимости это поле показывает базовую цену доставки
            "deliveryRatesGroups": [
                {
                    "deliveryType": ["courier", "pickup"],
                    "deliveryRates": VOLOGDA_COURIER_PICKUP,
                },
                {
                    "deliveryType": ["post"],
                    "deliveryRates": VOLOGDA_POST,
                },
            ],
            "attributes": {"weight": DEFAULT_KGT_WEIGHT_THRESHOLD},
        }
        self.assertFragmentIn(
            response, {"results": {"base": sample_base, "bulk": sample_bulk}}, allow_different_len=False
        )

    def test_vologda_post_as_pickup(self):
        '''
        В Вологду цена доставки почтой отличается от других для посылок от 5000 рублей.
        Проверяем, что тариф почты имеет две записи, а тарифы курьерки и ПВЗ объединены,
        используется почта как ПВЗ
        Тарифы экспресса идут отдельной группой
        '''
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'
        request = 'place=blue_tariffs&rgb=blue&rids={}'.format(VOLOGDA_RID) + unified_off_flags
        response = self.report.request_json(request)
        sample_base = {
            "deliveryRates": VOLOGDA_COURIER_PICKUP,  # DEPRECATED для совместимости это поле показывает базовую цену доставки
            "deliveryRatesGroups": [
                {
                    "deliveryType": ["courier", "pickup"],
                    "deliveryRates": VOLOGDA_COURIER_PICKUP,
                },
                {
                    "deliveryType": ["express"],
                    "deliveryRates": VOLOGDA_EXPRESS,
                },
            ],
        }
        sample_bulk = {
            "deliveryRates": VOLOGDA_COURIER_PICKUP,  # DEPRECATED для совместимости это поле показывает базовую цену доставки
            "deliveryRatesGroups": [
                {
                    "deliveryType": ["courier", "pickup"],
                    "deliveryRates": VOLOGDA_COURIER_PICKUP,
                },
            ],
            "attributes": {"weight": DEFAULT_KGT_WEIGHT_THRESHOLD},
        }
        self.assertFragmentIn(
            response, {"results": {"base": sample_base, "bulk": sample_bulk}}, allow_different_len=False
        )

    def test_kirov(self):
        '''
        Легкие посылки доставляются курьером и почтой со склада Маршрута. Тяжелые только курьером
        Тарифы экспресса не заданы, поэтому в список не попадают
        '''
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0;market_use_post_as_pickup=0'
        request = 'place=blue_tariffs&rgb=blue&rids={}'.format(KIROV_RID) + unified_off_flags
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "results": {
                    "base": {
                        "deliveryRates": OTHER_LIGHT,
                        "deliveryRatesGroups": [
                            {
                                "deliveryType": ["courier", "post"],
                                "deliveryRates": OTHER_LIGHT,
                            }
                        ],
                        "deliveryTime": {"courier": {"dayFrom": 7, "dayTo": 8}, "post": {"dayFrom": 2, "dayTo": 5}},
                    },
                    "bulk": {
                        "deliveryRates": OTHER_LIGHT,
                        "deliveryRatesGroups": [
                            {
                                "deliveryType": ["courier"],
                                "deliveryRates": OTHER_LIGHT,
                            }
                        ],
                        "attributes": {"weight": DEFAULT_KGT_WEIGHT_THRESHOLD},
                    },
                }
            },
            allow_different_len=False,
        )

    def test_kirov_post_as_pickup(self):
        '''
        Легкие посылки доставляются курьером и почтой со склада Маршрута. Тяжелые только курьером
        Тарифы экспресса не заданы, поэтому в список не попадают; используется почта как ПВЗ
        '''
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'
        request = 'place=blue_tariffs&rgb=blue&rids={}'.format(KIROV_RID) + unified_off_flags
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "results": {
                    "base": {
                        "deliveryRates": OTHER_LIGHT,
                        "deliveryRatesGroups": [
                            {
                                "deliveryType": ["courier", "pickup"],
                                "deliveryRates": OTHER_LIGHT,
                            }
                        ],
                        "deliveryTime": {"courier": {"dayFrom": 7, "dayTo": 8}, "pickup": {"dayFrom": 2, "dayTo": 5}},
                    },
                    "bulk": {
                        "deliveryRates": OTHER_LIGHT,
                        "deliveryRatesGroups": [
                            {
                                "deliveryType": ["courier"],
                                "deliveryRates": OTHER_LIGHT,
                            }
                        ],
                        "attributes": {"weight": DEFAULT_KGT_WEIGHT_THRESHOLD},
                    },
                }
            },
            allow_different_len=False,
        )

    def test_mitino(self):
        '''
        В этот регион доступна доставка только в ПВЗ и только легких посылок. Остальные тарифы не показаны
        '''
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'
        request = 'place=blue_tariffs&rgb=blue&rids={}'.format(MITINO_RID) + unified_off_flags
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "results": {
                    "base": {
                        "deliveryRates": OTHER_LIGHT,
                        "deliveryRatesGroups": [
                            {
                                "deliveryType": ["pickup"],
                                "deliveryRates": OTHER_LIGHT,
                            }
                        ],
                    },
                    "bulk": Absent(),
                }
            },
            allow_different_len=False,
        )

    def test_without_delivery_calc(self):
        '''
        по флагу &calculate-delivery=0 перестаём рассчитывать и выдавать сроки доставки и проверять доступность
        способов доставки при подобном упрощении принимаем, что все способы доставки доступны
        '''
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'
        request = 'place=blue_tariffs&rgb=blue&calculate-delivery=0&rids={}'.format(MOSCOW_RID) + unified_off_flags
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                "results": {
                    "base": {
                        "deliveryTime": Absent(),
                        "deliveryRates": FIRST_LIGHT,
                        "deliveryRatesGroups": [
                            {
                                "deliveryType": ["courier", "pickup", "post", "express"],
                                "deliveryRates": FIRST_LIGHT,
                            }
                        ],
                    },
                    "bulk": {
                        "deliveryTime": Absent(),
                        "deliveryRates": FIRST_HEAVY,
                        "deliveryRatesGroups": [
                            {
                                "deliveryType": ["courier", "pickup", "post"],
                                "deliveryRates": FIRST_HEAVY,
                            }
                        ],
                        "attributes": {"weight": DEFAULT_KGT_WEIGHT_THRESHOLD},
                    },
                    "special": {
                        "marketBrandedPickup": {
                            "threshold": {
                                "value": "1111",
                                "currency": "RUR",
                            }
                        },
                        "yaPlus": {
                            "threshold": {
                                "value": "321",
                                "currency": "RUR",
                            }
                        },
                    },
                }
            },
            allow_different_len=False,
        )

    def test_tier_info(self):
        '''
        пробрасывать информацию о tier
        '''
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'
        for rid, tier in ((MOSCOW_RID, "tier_1"), (ROSTOV_RID, "tier_3")):
            request = 'place=blue_tariffs&rgb=blue&calculate-delivery=0&rids={}'.format(rid) + unified_off_flags
            response = self.report.request_json(request)
            self.assertFragmentIn(
                response,
                {
                    "results": {
                        "tier": tier,
                    }
                },
                allow_different_len=False,
            )

    def test_kgt_weight_threshold(self):
        '''
        Проверяется выдача трешхолда кгт по регионам
        '''
        unified_off_flags = '&rearr-factors=market_dsbs_tariffs=0;market_unified_tariffs=0'
        for rid, threshold in ((MOSCOW_RID, DEFAULT_KGT_WEIGHT_THRESHOLD), (SAMARA_RID, SAMARA_KGT_WEIGHT_THRESHOLD)):
            request = 'place=blue_tariffs&rgb=blue&calculate-delivery=1&rids={}'.format(rid) + unified_off_flags
            response = self.report.request_json(request)
            self.assertFragmentIn(
                response,
                {
                    "results": {
                        "bulk": {
                            "attributes": {"weight": threshold},
                        },
                    }
                },
                allow_different_len=False,
            )

    @classmethod
    def prepare_unified_delivery_modifiers(cls):
        '''
        Подготавливаем параметры цены доставки для пользователя для
            эксперимента с едиными тарифами
        '''
        EXP_UNIFIED_TARIFFS = 'unified'
        TARIFFS = [
            BlueDeliveryTariff(large_size=0, for_plus=1, user_price=99, price_to=999),
            BlueDeliveryTariff(large_size=0, for_plus=1, user_price=0),
            BlueDeliveryTariff(large_size=0, for_plus=0, user_price=159, price_to=1999),
            BlueDeliveryTariff(large_size=0, for_plus=0, user_price=49, price_to=2999),
            BlueDeliveryTariff(
                large_size=0,
                for_plus=0,
                user_price=0,
            ),
            BlueDeliveryTariff(large_size=1, user_price=599),
        ]

        cls.index.blue_delivery_modifiers.add_modifier(
            tariffs=TARIFFS,
            regions=[UNIFIED_RID],
            exp_name=EXP_UNIFIED_TARIFFS,
            large_size_weight=DEFAULT_KGT_WEIGHT_THRESHOLD,
        )
        cls.index.blue_delivery_modifiers.set_default_modifier(
            tariffs=[BlueDeliveryTariff(user_price=55, large_size=0), BlueDeliveryTariff(user_price=44)],
            exp_name=EXP_UNIFIED_TARIFFS,
        )

        # Эксперимент с едиными тарифами, тариф экспресс оферов в конкретном городе
        cls.index.blue_delivery_modifiers.add_modifier(
            exp_name=EXP_UNIFIED_TARIFFS,
            tariffs=[
                BlueDeliveryTariff(user_price=49, for_plus=1, large_size=0, price_to=1200),
                BlueDeliveryTariff(user_price=0, for_plus=1, large_size=0),
                BlueDeliveryTariff(user_price=199, for_plus=0, large_size=0, price_to=1200),
                BlueDeliveryTariff(user_price=99, for_plus=0, large_size=0),
                BlueDeliveryTariff(user_price=599, large_size=1),
            ],
            regions=[UNIFIED_RID],
            is_express=True,
        )
        # Эксперимент с едиными тарифами, тариф экспресс оферов везде
        cls.index.blue_delivery_modifiers.set_default_modifier(
            tariffs=[BlueDeliveryTariff(user_price=350)], exp_name=EXP_UNIFIED_TARIFFS, is_express=True
        )

    def test_unified_tariffs(self):
        '''
        Проверяем blue_tariffs с новыми тарифами
        '''
        request = 'place=blue_tariffs&rgb=blue&rids={}&rearr-factors=market_unified_tariffs=1'.format(UNIFIED_RID)
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            {
                'results': {
                    'base': {
                        'deliveryRatesGroups': [
                            {
                                # проверяем, что тарифы обычной доставки пишутся по одному разу и корректно
                                # до 1999 - 159, с 1999 до 2999 - 49, бесплатно с 2999
                                "entity": "deliveryRateGroup",
                                "deliveryType": ["courier", "pickup"],
                                "deliveryRates": [
                                    {
                                        "entity": "deliveryRate",
                                        "maxOrderPrice": {"currency": "RUR", "value": "1999"},
                                        "price": {"currency": "RUR", "value": "159"},
                                    },
                                    {
                                        "entity": "deliveryRate",
                                        "minOrderPrice": {"currency": "RUR", "value": "1999"},
                                        "maxOrderPrice": {"currency": "RUR", "value": "2999"},
                                        "price": {"currency": "RUR", "value": "49"},
                                    },
                                    {
                                        "entity": "deliveryRate",
                                        "minOrderPrice": {"currency": "RUR", "value": "2999"},
                                        "price": {"currency": "RUR", "value": "0"},
                                    },
                                ],
                            },
                            {
                                "entity": "deliveryRateGroup",
                                "deliveryType": ["express"],
                                # здесь тарифы для экспресса без плюса - до 1200 199 рублей, с 1200 99
                                "deliveryRates": [
                                    {
                                        "entity": "deliveryRate",
                                        "maxOrderPrice": {"currency": "RUR", "value": "1200"},
                                        "price": {"currency": "RUR", "value": "199"},
                                    },
                                    {
                                        "entity": "deliveryRate",
                                        "minOrderPrice": {"currency": "RUR", "value": "1200"},
                                        "price": {"currency": "RUR", "value": "99"},
                                    },
                                ],
                            },
                        ]
                    },
                    "bulk": {
                        "deliveryRates": [
                            {
                                # цена за доставку КГТ - 599
                                "entity": "deliveryRate",
                                "price": {"currency": "RUR", "value": "599"},
                            }
                        ],
                        "deliveryRatesGroups": [
                            {
                                "entity": "deliveryRateGroup",
                                "deliveryType": ["courier", "pickup"],
                                "deliveryRates": [
                                    {"entity": "deliveryRate", "price": {"currency": "RUR", "value": "599"}}
                                ],
                            }
                        ],
                    },
                    "special": {
                        "yaPlus": {
                            "threshold": {"currency": "RUR", "value": "999"},
                            "price": {
                                # в значении до  всегда приходит 0 до MARKETOUT-39793
                                "currency": "RUR",
                                "value": "0",
                            },
                            "expressDeliveryRates": [
                                {
                                    # здесь тарифы для экспресса всегда с плюсом - до 1200 49 рублей, с 1200 0
                                    "entity": "deliveryRate",
                                    "maxOrderPrice": {"currency": "RUR", "value": "1200"},
                                    "price": {"currency": "RUR", "value": "49"},
                                },
                                {
                                    "entity": "deliveryRate",
                                    "minOrderPrice": {"currency": "RUR", "value": "1200"},
                                    "price": {"currency": "RUR", "value": "0"},
                                },
                            ],
                            "bulk": {
                                "deliveryRates": [
                                    {
                                        # цена за доставку КГТ с Яндекс Плюс такая же как и без него - 599
                                        "entity": "deliveryRate",
                                        "price": {"currency": "RUR", "value": "599"},
                                    }
                                ],
                                "deliveryRatesGroups": [
                                    {
                                        "entity": "deliveryRateGroup",
                                        "deliveryType": ["courier", "pickup"],
                                        "deliveryRates": [
                                            {"entity": "deliveryRate", "price": {"currency": "RUR", "value": "599"}}
                                        ],
                                    }
                                ],
                            },
                        },
                    },
                }
            },
            allow_different_len=False,
        )

    @classmethod
    def prepare_combinator_courier_options(cls):
        # Вес, который репорт отправляет в запросе
        REPORT_WEIGHT = 1000  # Обычная посылка
        REPORT_KGT_WEIGHT = 20000  # Тяжелая посылка

        def add_combinator_courier(region, weight, dateFrom, dateTo):
            cls.combinator.on_courier_options_request(
                items=[
                    DeliveryItem(
                        required_count=1,
                        weight=weight,
                        dimensions=[1, 1, 1],
                        cargo_types=[],
                        offers=[
                            CombinatorOffer(
                                shop_sku="",
                                shop_id=4294967295,
                                partner_id=172,
                                available_count=1,
                            )
                        ],
                        price=1000,
                    ),
                ],
                destination=Destination(region_id=region),
                payment_methods=[],
                total_price=1000,
            ).respond_with_courier_options(
                options=[
                    create_delivery_option(
                        cost=5,
                        date_from=dateFrom,
                        date_to=dateTo,
                        time_from=datetime.time(10, 0),
                        time_to=datetime.time(22, 0),
                        delivery_service_id=123,
                        payment_methods=[
                            PaymentMethod.PT_YANDEX,
                            PaymentMethod.PT_CARD_ON_DELIVERY,
                        ],
                        leave_at_the_door=True,
                        do_not_call=True,
                        airship_delivery=False,
                        customizers=[
                            {
                                "key": "leave_at_the_door",
                                "name": "Оставить у двери",
                                "type": "boolean",
                            },
                            {
                                "key": "not_call",
                                "name": "Не звонить",
                                "type": "boolean",
                            },
                        ],
                    )
                ],
                virtual_box=create_virtual_box(weight=weight, length=10, width=10, height=10),
            )

        add_combinator_courier(MOSCOW_RID, REPORT_WEIGHT, DATE_1, DATE_2)
        add_combinator_courier(MOSCOW_RID, REPORT_KGT_WEIGHT, DATE_1, DATE_2)

    @classmethod
    def prepare_combinator_pickup_options(cls):
        # Вес, который репорт отправляет в запросе
        REPORT_WEIGHT = 1000  # Обычная посылка
        REPORT_KGT_WEIGHT = 20000  # Тяжелая посылка

        def add_combinator_pickup(region, weight, outlets, type, dateFrom, dateTo):
            cls.combinator.on_pickup_points_grouped_request(
                items=[
                    DeliveryItem(
                        required_count=1,
                        weight=weight,
                        dimensions=[1, 1, 1],
                        cargo_types=[],
                        offers=[
                            CombinatorOffer(
                                shop_sku="",
                                shop_id=4294967295,
                                partner_id=172,
                                available_count=1,
                            )
                        ],
                        price=1000,
                    ),
                ],
                destination_regions=[region],
                point_types=[],
                total_price=1000,
            ).respond_with_grouped_pickup_points(
                groups=[
                    PickupPointGrouped(
                        ids_list=outlets,
                        outlet_type=type,
                        service_id=322,
                        cost=100,
                        date_from=dateFrom,
                        date_to=dateTo,
                        payment_methods=[PaymentMethod.PT_CASH_ON_DELIVERY],
                        delivery_intervals=[
                            PointTimeInterval(point_id=1, time_from=datetime.time(10, 0), time_to=datetime.time(22, 0)),
                        ],
                    ),
                ],
                virtual_box=create_virtual_box(weight=weight, length=10, width=10, height=10),
            )

        add_combinator_pickup(MOSCOW_RID, REPORT_WEIGHT, [101], OutletType.FOR_PICKUP, DATE_2, DATE_4)
        add_combinator_pickup(MOSCOW_RID, REPORT_KGT_WEIGHT, [101], OutletType.FOR_PICKUP, DATE_2, DATE_4)

        # почта в Кирове
        add_combinator_pickup(KIROV_RID, REPORT_WEIGHT, [141], OutletType.FOR_POST, DATE_2, DATE_5)


if __name__ == '__main__':
    main()
