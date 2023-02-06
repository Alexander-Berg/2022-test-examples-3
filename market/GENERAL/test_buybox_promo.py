#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa
from core.types import (
    BlueOffer,
    DateSwitchTimeAndRegionInfo,
    DeliveryBucket,
    DeliveryOption,
    DynamicBlueGenericBundlesPromos,
    DynamicDeliveryRestriction,
    DynamicDeliveryServiceInfo,
    DynamicWarehouseAndDeliveryServiceInfo,
    DynamicWarehouseDelivery,
    DynamicWarehouseInfo,
    DynamicWarehouseLink,
    DynamicWarehousesPriorityInRegion,
    Elasticity,
    MarketSku,
    Model,
    Offer,
    Promo,
    PromoType,
    Region,
    RegionalDelivery,
    Shop,
    ShopOperationalRating,
    WarehouseWithPriority,
    WarehousesPriorityInRegion,
    generate_dsbs,
)
from core.testcase import TestCase, main
from core.types.offer_promo import (
    PromoBlueCashback,
    PromoDirectDiscount,
    PromoCheapestAsGift,
    PromoRestrictions,
    OffersMatchingRules,
    make_generic_bundle_content,
)
from datetime import (
    datetime,
    timedelta,
)
from core.report import REQUEST_TIMESTAMP
from itertools import count
from core.types.autogen import b64url_md5
from market.pylibrary.const.offer_promo import MechanicsPaymentType
from market.proto.common.promo_pb2 import ESourceType
from core.matcher import Capture, NotEmpty, Absent

now = datetime.fromtimestamp(REQUEST_TIMESTAMP)
delta_small = timedelta(days=1)

nummer = count()

promo_blue_cashback = Promo(
    promo_type=PromoType.BLUE_CASHBACK,
    description='blue_cashback_1_description',
    key=b64url_md5(next(nummer)),
    blue_cashback=PromoBlueCashback(share=0.2, version=10, priority=1),
)

promo_blue_cashback_with_predicates = Promo(
    promo_type=PromoType.BLUE_CASHBACK,
    key=b64url_md5(next(nummer)),
    blue_cashback=PromoBlueCashback(
        share=0.5,
        version=6,
        priority=1,
    ),
    restrictions=PromoRestrictions(
        predicates=[
            {
                'perks': ['yandex_extra_cashback'],
                'at_supplier_warehouse': True,
            }
        ]
    ),
)

promo_blue_cashback_with_more_predicates = Promo(
    promo_type=PromoType.BLUE_CASHBACK,
    key=b64url_md5(next(nummer)),
    blue_cashback=PromoBlueCashback(
        share=0.9,
        version=6,
        priority=-1,
    ),
    restrictions=PromoRestrictions(
        predicates=[
            {
                'perks': ['yandex_extra_cashback', 'yandex_blabla'],
                'at_supplier_warehouse': True,
            }
        ]
    ),
)

finised_blue_cashback = Promo(
    promo_type=PromoType.BLUE_CASHBACK,
    description='blue_cashback_2_description',
    key=b64url_md5(next(nummer)),
    start_date=now + delta_small,
    blue_cashback=PromoBlueCashback(share=0.25, version=11, priority=1),
)


def get_offer_id(x):
    return "offer_id_{}".format(x)


# Персональная скидка через промокод
OFFER_ID_WITH_PERSONAL_PROMOCODE = 123026
OFFER_ID_WITHOUT_PERSONAL_PROMOCODE = 124026
PERSONAL_PROMOCODE_PERK = 'promo_personal_discount'
promo_personal_promocode = Promo(
    promo_type=PromoType.PROMO_CODE,
    promo_code=PERSONAL_PROMOCODE_PERK,
    discount_value=15,
    feed_id=1111,
    key=b64url_md5(next(nummer)),
    url='http://' + PERSONAL_PROMOCODE_PERK + '.ru/',
    landing_url='http://' + PERSONAL_PROMOCODE_PERK + '.ru/land',
    mechanics_payment_type=MechanicsPaymentType.CPA,
    shop_promo_id=PERSONAL_PROMOCODE_PERK,
    conditions='conditions to buy',
    promo_internal_priority=4,
    source_type=ESourceType.DCO_PERSONAL,
    restrictions=PromoRestrictions(
        predicates=[
            {
                'perks': [PERSONAL_PROMOCODE_PERK],
            }
        ]
    ),
    offers_matching_rules=[
        OffersMatchingRules(
            feed_offer_ids=[
                [1111, get_offer_id(OFFER_ID_WITH_PERSONAL_PROMOCODE)],
            ]
        )
    ],
)


# Персональная прямая скидка
PERSONAL_DISCOUNT_PERK = 'promo_personal_direct'
OFFER_ID_WITH_PERSONAL_DISCOUNT = 123027
OFFER_ID_WITHOUT_PERSONAL_DISCOUNT = 124027
promo_personal_direct = Promo(
    promo_type=PromoType.DIRECT_DISCOUNT,
    feed_id=1111,
    key=b64url_md5(next(nummer)),
    url='http://' + PERSONAL_DISCOUNT_PERK + '.com/',
    shop_promo_id=PERSONAL_DISCOUNT_PERK,
    source_type=ESourceType.DCO_PERSONAL,
    direct_discount=PromoDirectDiscount(
        items=[
            {
                'feed_id': 1111,
                'offer_id': get_offer_id(OFFER_ID_WITH_PERSONAL_DISCOUNT),
                # Закомментированные ниже поля могут быть не заданы, это не должно ничего ломать
                # см. тикет https://st.yandex-team.ru/MARKETMPE-743
                #
                # 'discount_price': {'value': 170, 'currency': 'RUR'},
                # 'old_price': {'value': 200, 'currency': 'RUR'},
                # 'max_discount': {'value': 30, 'currency': 'RUR'},
                # 'max_discount_percent': 15.0,
                'discount_percent': 15.0,  # Надо, чтобы был задан процент скидки
            },
        ],
        allow_berubonus=True,
        allow_promocode=True,
    ),
    restrictions=PromoRestrictions(
        predicates=[
            {
                'perks': [PERSONAL_DISCOUNT_PERK],
            }
        ]
    ),
    offers_matching_rules=[
        OffersMatchingRules(
            feed_offer_ids=[
                [1111, get_offer_id(OFFER_ID_WITH_PERSONAL_DISCOUNT)],
            ]
        )
    ],
)


# Прямая скидка, заданная процентом
DIRECT_DISCOUNT_BY_PERCENT_PERK = 'promo_direct_by_percent'
OFFER_ID_WITH_DIRECT_DISCOUNT_BY_PERCENT = 123028
promo_direct_discount_by_percent = Promo(
    promo_type=PromoType.DIRECT_DISCOUNT,
    feed_id=1111,
    key=b64url_md5(next(nummer)),
    url='http://' + DIRECT_DISCOUNT_BY_PERCENT_PERK + '.com/',
    shop_promo_id=DIRECT_DISCOUNT_BY_PERCENT_PERK,
    direct_discount=PromoDirectDiscount(
        items=[
            {
                'feed_id': 1111,
                'offer_id': get_offer_id(OFFER_ID_WITH_DIRECT_DISCOUNT_BY_PERCENT),
                'discount_percent': 15,  # Надо, чтобы был задан процент скидки
            },
        ],
        allow_berubonus=True,
        allow_promocode=True,
    ),
    restrictions=PromoRestrictions(
        predicates=[
            {
                'perks': [DIRECT_DISCOUNT_BY_PERCENT_PERK],
            }
        ]
    ),
    offers_matching_rules=[
        OffersMatchingRules(
            feed_offer_ids=[
                [1111, get_offer_id(OFFER_ID_WITH_DIRECT_DISCOUNT_BY_PERCENT)],
            ]
        )
    ],
)

# Прямая скидка
# с неверной ценой
PERSONAL_WRONG_DISCOUNT_PERK = 'promo_wrong_personal_direct'
OFFER_ID_WITH_WRONG_GLOBALLY_LOW_PERSONAL_DISCOUNT = 123029
OFFER_ID_WITHOUT_WRONG_PERSONAL_DISCOUNT = 124029
OFFER_ID_WITH_WRONG_PURCHASE_LOW_PERSONAL_DISCOUNT = 125029
OFFER_ID_WITH_WRONG_GLOBALLY_LOW_PERSONAL_DISCOUNT_BY_PERCENT = 126029
OFFER_ID_WITH_WRONG_PURCHASE_LOW_PERSONAL_DISCOUNT_BY_PERCENT = 127029
promo_personal_direct_wrong = Promo(
    promo_type=PromoType.DIRECT_DISCOUNT,
    feed_id=1111,
    key=b64url_md5(next(nummer)),
    url='http://' + PERSONAL_WRONG_DISCOUNT_PERK + '.com/',
    shop_promo_id=PERSONAL_WRONG_DISCOUNT_PERK,
    source_type=ESourceType.DCO_PERSONAL,
    direct_discount=PromoDirectDiscount(
        items=[
            {
                'feed_id': 1111,
                'offer_id': get_offer_id(OFFER_ID_WITH_WRONG_GLOBALLY_LOW_PERSONAL_DISCOUNT),
                'discount_price': {'value': 10, 'currency': 'RUR'},
            },
            {
                'feed_id': 1111,
                'offer_id': get_offer_id(OFFER_ID_WITH_WRONG_PURCHASE_LOW_PERSONAL_DISCOUNT),
                'discount_price': {'value': 300, 'currency': 'RUR'},
            },
            {
                'feed_id': 1111,
                'offer_id': get_offer_id(OFFER_ID_WITH_WRONG_GLOBALLY_LOW_PERSONAL_DISCOUNT_BY_PERCENT),
                'discount_percent': 98,
            },
            {
                'feed_id': 1111,
                'offer_id': get_offer_id(OFFER_ID_WITH_WRONG_PURCHASE_LOW_PERSONAL_DISCOUNT_BY_PERCENT),
                'discount_percent': 85,
            },
        ],
        allow_berubonus=True,
        allow_promocode=True,
    ),
    offers_matching_rules=[
        OffersMatchingRules(
            feed_offer_ids=[
                [1111, get_offer_id(OFFER_ID_WITH_WRONG_GLOBALLY_LOW_PERSONAL_DISCOUNT)],
                [1111, get_offer_id(OFFER_ID_WITH_WRONG_PURCHASE_LOW_PERSONAL_DISCOUNT)],
                [1111, get_offer_id(OFFER_ID_WITH_WRONG_GLOBALLY_LOW_PERSONAL_DISCOUNT_BY_PERCENT)],
                [1111, get_offer_id(OFFER_ID_WITH_WRONG_PURCHASE_LOW_PERSONAL_DISCOUNT_BY_PERCENT)],
            ]
        )
    ],
)


# Прямая скидка
# с неверной ценой
PERSONAL_NO_PROFIT_DISCOUNT_PERK = 'promo_no_profit_personal_direct'
OFFER_ID_WITH_NO_PROFIT_PERSONAL_DISCOUNT = 128030
OFFER_ID_WITHOUT_PERSONAL_DISCOUNT = 129030
promo_personal_direct_no_profit = Promo(
    promo_type=PromoType.DIRECT_DISCOUNT,
    feed_id=1111,
    key=b64url_md5(next(nummer)),
    url='http://' + PERSONAL_NO_PROFIT_DISCOUNT_PERK + '.com/',
    shop_promo_id=PERSONAL_NO_PROFIT_DISCOUNT_PERK,
    source_type=ESourceType.DCO_PERSONAL,
    direct_discount=PromoDirectDiscount(
        items=[
            {
                'feed_id': 1111,
                'offer_id': get_offer_id(OFFER_ID_WITH_NO_PROFIT_PERSONAL_DISCOUNT),
                'discount_price': {'value': 37, 'currency': 'RUR'},
            }
        ],
        allow_berubonus=True,
        allow_promocode=True,
    ),
    offers_matching_rules=[
        OffersMatchingRules(
            feed_offer_ids=[
                [1111, get_offer_id(OFFER_ID_WITH_NO_PROFIT_PERSONAL_DISCOUNT)],
            ]
        )
    ],
)


sorted_elasticity = [
    Elasticity(price_variant=100, demand_mean=200),
    Elasticity(price_variant=200, demand_mean=80),
    Elasticity(price_variant=300, demand_mean=10),
]


def dict_to_rearr(rearr_flags):
    result = ""
    for key in rearr_flags.keys():
        result += str(key) + "=" + str(rearr_flags[key]) + ";"
    return result


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += [
            'enable_fast_promo_matcher=0;enable_fast_promo_matcher_test=0;market_new_buybox_promo=1'
        ]

        cls.index.regiontree += [
            Region(
                rid=14838,
                region_type=Region.FEDERAL_DISTRICT,
                children=[
                    Region(
                        rid=1,
                        region_type=Region.FEDERATIVE_SUBJECT,
                        children=[
                            Region(
                                rid=213,
                                tz_offset=10680,
                                children=[Region(rid=3, tz_offset=10800), Region(rid=4, tz_offset=10800)],
                            ),
                            Region(
                                rid=2,
                                tz_offset=10800,
                                children=[
                                    Region(rid=5, tz_offset=10800),
                                ],
                            ),
                        ],
                    ),
                    Region(
                        rid=11029,
                        region_type=Region.FEDERATIVE_SUBJECT,
                        children=[
                            Region(rid=39, children=[Region(rid=123)]),
                        ],
                    ),
                ],
            )
        ]

        cls.index.shops += [
            Shop(
                fesh=1111,
                datafeed_id=1111,
                priority_region=213,
                regions=[225],
                name="1P-Магазин 145 склад",
                supplier_type=Shop.FIRST_PARTY,
                blue=Shop.BLUE_REAL,
                warehouse_id=145,
            ),
            # Shop(
            #     fesh=2222,
            #     datafeed_id=2222,
            #     priority_region=213,
            #     regions=[225],
            #     name="1P-Магазин 147 склад",
            #     supplier_type=Shop.FIRST_PARTY,
            #     blue=Shop.BLUE_REAL,
            #     warehouse_id=147,
            # ),
            Shop(
                fesh=3333,
                datafeed_id=3333,
                priority_region=213,
                regions=[225],
                name="3P-Магазин 145 склад",
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
                warehouse_id=145,
            ),
            # Shop(
            #     fesh=30,
            #     datafeed_id=30,
            #     priority_region=2,
            #     regions=[125],
            #     name="3P поставщик Вася",
            #     supplier_type=Shop.THIRD_PARTY,
            #     blue=Shop.BLUE_REAL,
            #     warehouse_id=145,
            # ),
            Shop(
                fesh=31,
                datafeed_id=31,
                priority_region=2,
                regions=[2],
                name="3P поставщик Вася Питерский",
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
                warehouse_id=1470,
            ),
            Shop(
                fesh=32,
                datafeed_id=32,
                priority_region=213,
                regions=[225],
                name="3P поставщик Вася Московский",
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
                warehouse_id=147,
            ),
            Shop(
                fesh=4444,
                datafeed_id=4444,
                priority_region=213,
                regions=[225],
                fulfillment_program=False,
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
                name="3P поставщик Борис",
                warehouse_id=145,
            ),
            Shop(
                fesh=5555,
                datafeed_id=5555,
                priority_region=213,
                regions=[225],
                fulfillment_program=False,
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
                name="3P поставщик Анатлий",
                warehouse_id=145,
            ),
        ]

        cls.index.shop_operational_rating += [
            ShopOperationalRating(
                calc_time=1589936458409,
                shop_id=30,
                late_ship_rate=5.9,
                cancellation_rate=1.93,
                return_rate=0.1,
                total=99.8,
            ),
            ShopOperationalRating(
                calc_time=1589936458409,
                shop_id=31,
                late_ship_rate=5.9,
                cancellation_rate=1.93,
                return_rate=0.1,
                total=99.8,
            ),
        ]

        cls.settings.lms_autogenerate = False
        cls.dynamic.lms += [
            DynamicWarehouseInfo(id=145, home_region=213),
            DynamicWarehouseInfo(id=147, home_region=213),
            DynamicWarehouseInfo(id=1470, home_region=2),
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=145,
                delivery_service_id=157,
                date_switch_time_infos=[DateSwitchTimeAndRegionInfo(date_switch_hour=23, region_to=225)],
            ),
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=147,
                delivery_service_id=157,
                date_switch_time_infos=[DateSwitchTimeAndRegionInfo(date_switch_hour=23, region_to=225)],
            ),
            DynamicWarehouseAndDeliveryServiceInfo(
                warehouse_id=1470,
                delivery_service_id=157,
                date_switch_time_infos=[DateSwitchTimeAndRegionInfo(date_switch_hour=23, region_to=225)],
            ),
            DynamicDeliveryServiceInfo(id=157, rating=2),
            DynamicWarehousesPriorityInRegion(region=225, warehouses=[145, 147, 1470]),
        ]

        cls.index.warehouse_priorities += [
            # в Москве приоритет складов одинаков, и все офферы будут становиться buybox равновероятно
            WarehousesPriorityInRegion(
                regions=[213],
                warehouse_with_priority=[
                    WarehouseWithPriority(145, 0),
                    WarehouseWithPriority(147, 0),
                    WarehouseWithPriority(1470, 0),
                ],
            ),
            # в Питере оффер со 145 склада не имеет шанса попасть даже с флагом market_blue_random_buybox
            WarehousesPriorityInRegion(
                regions=[2],
                warehouse_with_priority=[
                    WarehouseWithPriority(145, 1),
                    WarehouseWithPriority(147, 0),
                    WarehouseWithPriority(1470, 0),
                ],
            ),
        ]

        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=1234,
                carriers=[157],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                regional_options=[
                    RegionalDelivery(
                        rid=225, options=[DeliveryOption(price=15, shop_delivery_price=15, day_from=1, day_to=2)]
                    ),
                    RegionalDelivery(
                        rid=227, options=[DeliveryOption(price=48, shop_delivery_price=48, day_from=3, day_to=5)]
                    ),
                    RegionalDelivery(
                        rid=229, options=[DeliveryOption(price=58, shop_delivery_price=58, day_from=4, day_to=6)]
                    ),
                ],
            ),
            DeliveryBucket(
                bucket_id=1235,
                carriers=[157],
                delivery_program=DeliveryBucket.MARKET_DELIVERY_PROGRAM,
                regional_options=[
                    RegionalDelivery(
                        rid=224, options=[DeliveryOption(price=35, shop_delivery_price=35, day_from=2, day_to=4)]
                    ),
                    RegionalDelivery(
                        rid=225, options=[DeliveryOption(price=45, shop_delivery_price=45, day_from=3, day_to=5)]
                    ),
                    RegionalDelivery(
                        rid=226, options=[DeliveryOption(price=55, shop_delivery_price=55, day_from=4, day_to=6)]
                    ),
                ],
            ),
        ]

        cls.index.mskus += [
            MarketSku(
                title="Выигрывает Промо",
                hyperid=2,
                sku=100018,
                hid=100,
                delivery_buckets=[1234],
                purchase_price=120.33,
                buybox_elasticity=sorted_elasticity,
                blue_offers=[
                    BlueOffer(price=150, feedid=3333, waremd5="BLUE-100018-FEED-1112g", promo=promo_blue_cashback),
                    BlueOffer(price=145, feedid=1111, waremd5="BLUE-100018-FEED-1123g"),
                    BlueOffer(price=148, feedid=1111, waremd5="BLUE-100018-FEED-1122g"),
                ],
            ),
            MarketSku(
                title="Кэшбек срабатывает для 3P FF и для дропшипов",
                hyperid=2,
                sku=100019,
                hid=100,
                delivery_buckets=[1234],
                purchase_price=120.33,
                buybox_elasticity=sorted_elasticity,
                blue_offers=[
                    BlueOffer(
                        price=150,
                        feedid=1111,
                        waremd5="BLUE-100019-FEED-1112g",
                        promo=promo_blue_cashback_with_predicates,
                    ),
                    BlueOffer(
                        price=145,
                        feedid=4444,
                        waremd5="BLUE-100019-FEED-1123g",
                        promo=promo_blue_cashback_with_predicates,
                    ),
                    BlueOffer(price=148, feedid=1111, waremd5="BLUE-100019-FEED-1122g"),
                ],
            ),
            MarketSku(
                title="Кэшбек проверяется на валидность",
                hyperid=2,
                sku=100020,
                hid=100,
                delivery_buckets=[1234],
                purchase_price=120.33,
                buybox_elasticity=sorted_elasticity,
                blue_offers=[
                    BlueOffer(price=150, feedid=3333, waremd5="BLUE-100020-FEED-1112g", promo=finised_blue_cashback),
                    BlueOffer(price=145, feedid=1111, waremd5="BLUE-100020-FEED-1123g"),
                    BlueOffer(price=148, feedid=1111, waremd5="BLUE-100020-FEED-1122g"),
                ],
            ),
            MarketSku(
                title="Кэшбек проверяем срабатывает ли проверка loyalty_program_status",
                hyperid=2,
                sku=100021,
                hid=100,
                delivery_buckets=[1234],
                purchase_price=120.33,
                buybox_elasticity=sorted_elasticity,
                blue_offers=[
                    BlueOffer(price=208, feedid=3333, waremd5="BLUE-100021-FEED-1112g"),
                    BlueOffer(
                        price=205,
                        feedid=4444,
                        waremd5="BLUE-100021-FEED-1123g",
                        promo=promo_blue_cashback_with_predicates,
                    ),
                    BlueOffer(
                        price=200,
                        feedid=5555,
                        waremd5="BLUE-100021-FEED-1122g",
                    ),
                ],
            ),
            MarketSku(
                title="Размер кэшбека учитывается в ценовых огрничениях",
                hyperid=2,
                sku=100022,
                hid=100,
                delivery_buckets=[1234],
                purchase_price=120.33,
                buybox_elasticity=sorted_elasticity,
                blue_offers=[
                    BlueOffer(price=150, feedid=3333, waremd5="BLUE-100022-FEED-1112g"),
                    BlueOffer(
                        price=200,
                        feedid=4444,
                        waremd5="BLUE-100022-FEED-1123g",
                        promo=promo_blue_cashback_with_predicates,
                    ),
                    BlueOffer(
                        price=190,
                        feedid=4444,
                        waremd5="BLUE-100022-FEED-1122g",
                        promo=promo_blue_cashback_with_predicates,
                    ),
                ],
            ),
            MarketSku(
                title="Старый байбокс, цена с кэшбеком совпадает с min price",
                hyperid=2,
                sku=100023,
                hid=100,
                delivery_buckets=[1234],
                purchase_price=120.33,
                buybox_elasticity=sorted_elasticity,
                blue_offers=[
                    BlueOffer(price=150, feedid=3333, waremd5="BLUE-100023-FEED-1112g"),
                    BlueOffer(
                        price=200,
                        feedid=4444,
                        waremd5="BLUE-100023-FEED-1123g",
                        promo=promo_blue_cashback_with_predicates,
                    ),
                ],
            ),
            MarketSku(
                title="Старый байбокс, цена с кэшбеком меньше min price",
                hyperid=2,
                sku=100024,
                hid=100,
                delivery_buckets=[1234],
                purchase_price=120.33,
                buybox_elasticity=sorted_elasticity,
                blue_offers=[
                    BlueOffer(price=150, feedid=3333, waremd5="BLUE-100024-FEED-1112g"),
                    BlueOffer(
                        price=199,
                        feedid=4444,
                        waremd5="BLUE-100024-FEED-1123g",
                        promo=[
                            promo_blue_cashback_with_predicates,
                            promo_blue_cashback_with_more_predicates,
                        ],
                    ),
                ],
            ),
            MarketSku(
                title="Буст локальных поставщиков",
                hyperid=2,
                sku=100025,
                hid=100,
                delivery_buckets=[1235],
                buybox_elasticity=sorted_elasticity,
                blue_offers=[
                    BlueOffer(price=118, feedid=31, waremd5="BLUE-100025-FEED-3133g"),
                    BlueOffer(price=115, feedid=32, waremd5="BLUE-100025-FEED-3233g"),
                ],
            ),
            MarketSku(
                title="Выигрыш по персональной скидке (промокод)",
                hyperid=3,
                sku=120026,
                hid=100,
                delivery_buckets=[1234],
                purchase_price=120,
                buybox_elasticity=sorted_elasticity,
                blue_offers=[
                    BlueOffer(
                        price=200,
                        feedid=1111,
                        fesh=1111,
                        waremd5="BLUE-120026-FEED-1112g",
                        promo=[promo_personal_promocode],
                        title="with personal discount",
                        offerid=get_offer_id(OFFER_ID_WITH_PERSONAL_PROMOCODE),
                        is_fulfillment=True,
                        blue_promo_key=[promo_personal_promocode.shop_promo_id],
                    ),
                    BlueOffer(
                        price=200,
                        feedid=1111,
                        fesh=1111,
                        waremd5="BLUE-120026-FEED-1123g",
                        title="no personal discount",
                        offerid=get_offer_id(OFFER_ID_WITHOUT_PERSONAL_PROMOCODE),
                        is_fulfillment=True,
                    ),
                ],
            ),
            MarketSku(
                title="Выигрыш по персональной скидке (прямая скидка)",
                hyperid=3,
                sku=120027,
                hid=100,
                delivery_buckets=[1234],
                purchase_price=120,
                buybox_elasticity=sorted_elasticity,
                blue_offers=[
                    BlueOffer(
                        price=200,
                        feedid=1111,
                        fesh=1111,
                        waremd5="BLUE-120027-FEED-1112g",
                        promo=[promo_personal_direct],
                        title="with personal discount",
                        offerid=get_offer_id(OFFER_ID_WITH_PERSONAL_DISCOUNT),
                        is_fulfillment=True,
                        blue_promo_key=[promo_personal_direct.shop_promo_id],
                    ),
                    BlueOffer(
                        price=200,
                        feedid=1111,
                        fesh=1111,
                        waremd5="BLUE-120027-FEED-1123g",
                        title="no personal discount",
                        offerid=get_offer_id(OFFER_ID_WITHOUT_PERSONAL_DISCOUNT),
                        is_fulfillment=True,
                    ),
                ],
            ),
            MarketSku(
                title="Прямая скидка задана процентом",
                hyperid=3,
                sku=120028,
                hid=100,
                delivery_buckets=[1234],
                purchase_price=120,
                buybox_elasticity=sorted_elasticity,
                blue_offers=[
                    BlueOffer(
                        price=200,
                        feedid=1111,
                        fesh=1111,
                        waremd5="BLUE-120028-FEED-1112g",
                        promo=[promo_direct_discount_by_percent],
                        offerid=get_offer_id(OFFER_ID_WITH_DIRECT_DISCOUNT_BY_PERCENT),
                        is_fulfillment=True,
                        blue_promo_key=[promo_direct_discount_by_percent.shop_promo_id],
                    ),
                ],
            ),
            MarketSku(
                title="Проигрыш из-за некорректной персональной скидки",
                hyperid=3,
                sku=120029,
                hid=100,
                delivery_buckets=[1234],
                purchase_price=120,
                buybox_elasticity=sorted_elasticity,
                blue_offers=[
                    BlueOffer(
                        price=2000,
                        feedid=1111,
                        fesh=1111,
                        waremd5="BLUE-120029-FEED-1112g",
                        promo=[promo_personal_direct_wrong],
                        title="with wrong globally personal discount",
                        offerid=get_offer_id(OFFER_ID_WITH_WRONG_GLOBALLY_LOW_PERSONAL_DISCOUNT),
                        is_fulfillment=True,
                        blue_promo_key=[promo_personal_direct_wrong.shop_promo_id],
                    ),
                    BlueOffer(
                        price=2000,
                        purchase_price=500,
                        feedid=1111,
                        fesh=1111,
                        waremd5="BLUE-120029-FEED-1113g",
                        promo=[promo_personal_direct_wrong],
                        title="with wrong purchase personal discount",
                        offerid=get_offer_id(OFFER_ID_WITH_WRONG_PURCHASE_LOW_PERSONAL_DISCOUNT),
                        is_fulfillment=True,
                        blue_promo_key=[promo_personal_direct_wrong.shop_promo_id],
                    ),
                    BlueOffer(
                        price=2000,
                        feedid=1111,
                        fesh=1111,
                        waremd5="BLUE-120029-FEED-1114g",
                        promo=[promo_personal_direct_wrong],
                        title="with wrong globally personal discount by percent",
                        offerid=get_offer_id(OFFER_ID_WITH_WRONG_GLOBALLY_LOW_PERSONAL_DISCOUNT_BY_PERCENT),
                        is_fulfillment=True,
                        blue_promo_key=[promo_personal_direct_wrong.shop_promo_id],
                    ),
                    BlueOffer(
                        price=2000,
                        purchase_price=500,
                        feedid=1111,
                        fesh=1111,
                        waremd5="BLUE-120029-FEED-1115g",
                        promo=[promo_personal_direct_wrong],
                        title="with wrong purchase personal discount by percent",
                        offerid=get_offer_id(OFFER_ID_WITH_WRONG_PURCHASE_LOW_PERSONAL_DISCOUNT_BY_PERCENT),
                        is_fulfillment=True,
                        blue_promo_key=[promo_personal_direct_wrong.shop_promo_id],
                    ),
                    BlueOffer(
                        price=1000,
                        feedid=1111,
                        fesh=1111,
                        waremd5="BLUE-120029-FEED-1123g",
                        title="no personal discount",
                        offerid=get_offer_id(OFFER_ID_WITHOUT_WRONG_PERSONAL_DISCOUNT),
                        is_fulfillment=True,
                    ),
                ],
            ),
            MarketSku(
                title="Проигрыш из-за невыгодной персональной скидки",
                hyperid=3,
                sku=120030,
                hid=100,
                delivery_buckets=[1234],
                purchase_price=20,
                buybox_elasticity=sorted_elasticity,
                blue_offers=[
                    BlueOffer(
                        price=12,
                        price_old=40,
                        feedid=1111,
                        fesh=1111,
                        waremd5="BLUE-120030-FEED-1112g",
                        promo=[promo_personal_direct_no_profit],
                        title="with no profit personal discount",
                        offerid=get_offer_id(OFFER_ID_WITH_NO_PROFIT_PERSONAL_DISCOUNT),
                        is_fulfillment=True,
                        blue_promo_key=[promo_personal_direct_wrong.shop_promo_id],
                    ),
                    BlueOffer(
                        price=20,
                        price_old=40,
                        feedid=1111,
                        fesh=1111,
                        waremd5="BLUE-120030-FEED-1113g",
                        title="without personal discount",
                        offerid=get_offer_id(OFFER_ID_WITHOUT_PERSONAL_DISCOUNT),
                        is_fulfillment=True,
                    ),
                ],
            ),
        ]

    @classmethod
    def prepare_nordstream(cls):
        cls.settings.nordstream_autogenerate = False
        for warehouse_id in [145, 147, 1470]:
            cls.dynamic.nordstream += [DynamicWarehouseLink(warehouse_id, [warehouse_id])]
            cls.dynamic.nordstream += [
                DynamicWarehouseDelivery(
                    warehouse_id,
                    {
                        224: [
                            DynamicDeliveryRestriction(min_days=2, max_days=4, cost=35),
                        ],
                        225: [
                            DynamicDeliveryRestriction(min_days=1, max_days=2, cost=15),
                            DynamicDeliveryRestriction(min_days=3, max_days=5, cost=45),
                        ],
                        226: [
                            DynamicDeliveryRestriction(min_days=4, max_days=6, cost=55),
                        ],
                        227: [
                            DynamicDeliveryRestriction(min_days=3, max_days=5, cost=48),
                        ],
                        229: [
                            DynamicDeliveryRestriction(min_days=4, max_days=6, cost=58),
                        ],
                    },
                ),
            ]
        cls.dynamic.nordstream += generate_dsbs(cls.index)

    def test_buybox_promo_cashback_win(self):
        """Проверяем, что для дропшипов из цены вычитается половина размера кэшбека. При условии, что кэшбек валидный."""
        sku = 100018
        d = '&rearr-factors=market_debug_buybox=1;buybox_dropship_bluecashback=1;&debug=da'
        response = self.report.request_json(
            'place=productoffers&offers-set=defaultList&market-sku={}&rids=2&perks=yandex_cashback'.format(sku) + d
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'wareId': 'BLUE-100018-FEED-1112g',
                            'debug': {
                                'buyboxDebug': {
                                    'Offers': [
                                        {
                                            'WareMd5': 'BLUE-100018-FEED-1112g',
                                            'PredictedElasticity': {'Value': 158, 'Type': 'NORMAL'},
                                            'CashBackSize': 30,
                                        }
                                    ],
                                }
                            },
                        }
                    ]
                }
            },
        )

    def test_buybox_promo_no_extra_perk_cashback_check(self):
        """Что бы получить повышенный кэшбек нужен перк yandex_extra_cashback."""
        sku = 100021
        d = '&rearr-factors=market_debug_buybox=1;buybox_dropship_bluecashback=1;&debug=da'
        response = self.report.request_json(
            'place=productoffers&offers-set=defaultList&market-sku={}&rids=2&perks=yandex_cashback'.format(sku) + d
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'wareId': 'BLUE-100021-FEED-1122g',
                            'debug': {
                                'buyboxDebug': {
                                    'Offers': [
                                        {
                                            'WareMd5': 'BLUE-100021-FEED-1122g',
                                            'PredictedElasticity': {'Value': 80, 'Type': 'NORMAL'},
                                            'CashBackSize': 0,
                                        },
                                        {'WareMd5': 'BLUE-100021-FEED-1123g', 'CashBackSize': 0},
                                    ],
                                }
                            },
                        }
                    ]
                }
            },
        )

    def test_buybox_promo_cashback_disabled_with_flag(self):
        """Проверяем, что учет кэшбека отключается через флаг buybox_dropship_bluecashback=0"""
        sku = 100018
        d = '&rearr-factors=market_blue_buybox_gmv_ue_mix_coef=0.0001;buybox_dropship_bluecashback=0;&debug=da'
        response = self.report.request_json(
            'place=productoffers&offers-set=defaultList&market-sku={}&rids=2&perks=yandex_cashback'.format(sku) + d
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'wareId': 'BLUE-100018-FEED-1122g',
                            'debug': {
                                'buyboxDebug': {
                                    'Offers': [
                                        {
                                            'WareMd5': 'BLUE-100018-FEED-1112g',
                                            'PredictedElasticity': {'Value': 140, 'Type': 'NORMAL'},
                                            'CashBackSize': 0,
                                        }
                                    ]
                                }
                            },
                        }
                    ]
                }
            },
        )

    def test_buybox_promo_cashback_for_dropship_ff(self):
        """Проверяем, что мы учитываем кэшбек для FF тоже."""
        sku = 100019
        d = '&rearr-factors=market_debug_buybox=1;buybox_dropship_bluecashback=1;&debug=da&perks=yandex_cashback,yandex_extra_cashback'
        response = self.report.request_json(
            'place=productoffers&offers-set=defaultList&market-sku={}&rids=2'.format(sku) + d
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'wareId': 'BLUE-100019-FEED-1112g',
                            'debug': {
                                'buyboxDebug': {
                                    'Offers': [
                                        {
                                            'WareMd5': 'BLUE-100019-FEED-1112g',
                                            'CashBackSize': 75,
                                            'PredictedElasticity': {'Value': 185, 'Type': 'NORMAL'},
                                        },
                                        {
                                            'WareMd5': 'BLUE-100019-FEED-1123g',
                                            'CashBackSize': 73,
                                            'PredictedElasticity': {'Value': 189.8, 'Type': 'NORMAL'},
                                        },
                                    ]
                                }
                            },
                        }
                    ]
                }
            },
        )

    def test_buybox_promo_cashback_use_only_valid(self):
        """Проверяем, что мы учитываем только валидный кэшбек."""
        sku = 100020
        d = '&rearr-factors=market_debug_buybox=1;buybox_dropship_bluecashback=1;buybox_dropship_bluecashback=1;&debug=da&perks=yandex_cashback'
        response = self.report.request_json(
            'place=productoffers&offers-set=defaultList&market-sku={}&rids=2'.format(sku) + d
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'wareId': 'BLUE-100020-FEED-1123g',
                            'debug': {
                                'buyboxDebug': {
                                    'Offers': [
                                        {
                                            'WareMd5': 'BLUE-100020-FEED-1112g',
                                            'PredictedElasticity': {'Value': 140, 'Type': 'NORMAL'},
                                            'CashBackSize': 0,
                                        },
                                    ]
                                }
                            },
                        }
                    ]
                }
            },
        )

    def test_buybox_extra_perk_cashback_check_oldbuybox(self):
        """Проверяем, что кэшбек влияет на старый байбокс market_blue_buybox_by_gmv_ue=0;"""
        sku = 100021
        d = '&rearr-factors=market_debug_buybox=1;buybox_dropship_bluecashback=1;market_blue_buybox_by_gmv_ue=0;&debug=da'
        response = self.report.request_json(
            'place=productoffers&offers-set=defaultList&market-sku={}&rids=2&perks=yandex_extra_cashback,yandex_cashback'.format(
                sku
            )
            + d
        )

        self.assertFragmentIn(
            response,
            {'buyboxDebug': {'Offers': [{'WareMd5': 'BLUE-100021-FEED-1123g', 'CashBackSize': 103, 'OfferWeight': 2}]}},
        )

        self.assertFragmentIn(response, "MinPriceAfterCashback: 153.5")

    def test_buybox_price_rejections_consider_cashback_size(self):
        """Проверяем, что перед отсечением по цене, половина от суммы кэшбека вычитается от цены"""
        sku = 100022
        d = '&rearr-factors=market_debug_buybox=1;buybox_dropship_bluecashback=1;&debug=da'
        response = self.report.request_json(
            'place=productoffers&offers-set=defaultList&market-sku={}&rids=2&perks=yandex_extra_cashback,yandex_cashback'.format(
                sku
            )
            + d
        )
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'wareId': 'BLUE-100022-FEED-1122g',
                            'debug': {
                                'buyboxDebug': {'Offers': [{'WareMd5': 'BLUE-100022-FEED-1122g', 'CashBackSize': 95}]}
                            },
                        }
                    ]
                }
            },
        )

    def test_buybox_cashback_price_eq_regular_price(self):
        """Проверяем, что цена с кэшбеком совпадает с min price
        В результате OfferWeight у них сравнивается
        """
        sku = 100023
        d = '&rearr-factors=market_debug_buybox=1;buybox_dropship_bluecashback=1;market_blue_buybox_by_gmv_ue=0;&debug=da'
        response = self.report.request_json(
            'place=productoffers&offers-set=defaultList&market-sku={}&rids=2&perks=yandex_extra_cashback,yandex_cashback'.format(
                sku
            )
            + d
        )

        self.assertFragmentIn(
            response,
            {
                'buyboxDebug': {
                    'Offers': [
                        {'WareMd5': 'BLUE-100023-FEED-1112g', 'CashBackSize': 0, 'OfferWeight': 2},
                        {'WareMd5': 'BLUE-100023-FEED-1123g', 'CashBackSize': 100, 'OfferWeight': 2},
                    ]
                }
            },
        )

        self.assertFragmentIn(response, "MinPriceAfterCashback: 150")

    def test_buybox_cashback_several_promo(self):
        """
        Проверяем, что при наличии нескольких промо на товаре, все активные промо будут посчитаны до байбокса
        В байбокс пойдет та же самая информация о промо, которая будет показана на выдаче
        """
        sku = 100024
        d = '&rearr-factors=market_debug_buybox=1;buybox_dropship_bluecashback=1;market_blue_buybox_by_gmv_ue=0;&debug=da'
        response = self.report.request_json(
            'place=productoffers&offers-set=defaultList&market-sku={}&rids=2&perks={}'.format(
                sku, 'yandex_extra_cashback,yandex_cashback'
            )
            + d
        )
        # На товаре BLUE-100024-FEED-1123g два кешбека по приоритету:
        # 1. promo_blue_cashback_with_more_predicates 90% и скрытый под перки yandex_extra_cashback и yandex_blabla
        # 2. promo_blue_cashback_with_predicates номиналом 50% и скрытый перком yandex_extra_cashback
        # Делаем запрос с перком yandex_extra_cashback - первый кешбек откидывается и его место занимает второй
        self.assertFragmentIn(
            response,
            {
                'buyboxDebug': {
                    'Offers': [
                        {'WareMd5': 'BLUE-100024-FEED-1112g', 'CashBackSize': 0, 'OfferWeight': 0.287442},
                        {'WareMd5': 'BLUE-100024-FEED-1123g', 'CashBackSize': 100, 'OfferWeight': 2},
                    ]
                }
            },
        )
        # Цена товара - половина баллов кешбека (199 - 50 = 149)
        self.assertFragmentIn(response, "MinPriceAfterCashback: 149")
        # Делаем второй запрос с перками yandex_extra_cashback и yandex_blabla - в байбоксе и на выдаче первый кешбек
        response = self.report.request_json(
            'place=productoffers&offers-set=defaultList&market-sku={}&rids=2&perks={}'.format(
                sku, 'yandex_extra_cashback,yandex_cashback,yandex_blabla'
            )
            + d
        )
        self.assertFragmentIn(
            response,
            {
                'buyboxDebug': {
                    'Offers': [
                        {'WareMd5': 'BLUE-100024-FEED-1112g', 'CashBackSize': 0, 'OfferWeight': 0},
                        {'WareMd5': 'BLUE-100024-FEED-1123g', 'CashBackSize': 180, 'OfferWeight': 2},
                    ]
                }
            },
        )
        # Цена товара - половина баллов кешбека (199 - 180 = 109)
        self.assertFragmentIn(response, "MinPriceAfterCashback: 109")

    def test_buybox_cashback_price_less_regular_price(self):
        """Проверяем, когда цена с кэшбеком становится меньше min price
        В результате побеждает оффер с кэшбэком
        """
        sku = 100024
        d = '&rearr-factors=market_debug_buybox=1;buybox_dropship_bluecashback=1;market_blue_buybox_by_gmv_ue=0;&debug=da'
        response = self.report.request_json(
            'place=productoffers&offers-set=defaultList&market-sku={}&rids=2&perks=yandex_extra_cashback,yandex_cashback'.format(
                sku
            )
            + d
        )

        self.assertFragmentIn(
            response,
            {
                'buyboxDebug': {
                    'Offers': [
                        {'WareMd5': 'BLUE-100024-FEED-1112g', 'CashBackSize': 0, 'OfferWeight': 0.287442},
                        {'WareMd5': 'BLUE-100024-FEED-1123g', 'CashBackSize': 100, 'OfferWeight': 2},
                    ]
                }
            },
        )

        self.assertFragmentIn(response, "MinPriceAfterCashback: 149")

    @classmethod
    def prepare_test_buybox_promocode_cashback(cls):
        # Действующая акция. Скидка по промокоду в абсолютной величинe (рублях).
        blue_promo_promocode = Promo(
            key='JVvklxUgdnawSJPG4UhZ-1',
            promo_type=PromoType.PROMO_CODE,
            promo_code='promocode_1_text',
            discount_value=300,
            discount_currency='RUR',
        )

        # Корзинный промокод, отличается наличием поля min_order_price в restrictions
        blue_promo_cart_promocode = Promo(
            key='JVvklxUgdnawSJPG4UhZ-2',
            promo_type=PromoType.PROMO_CODE,
            promo_code='promocode_2_text',
            discount_value=500,
            discount_currency='RUR',
            restrictions=PromoRestrictions(
                order_min_price={
                    'value': 3000,
                    'currency': 'RUR',
                }
            ),
        )

        cls.index.mskus += [
            MarketSku(
                title="Тестирование промо типа PROMO_CODE",
                hyperid=2,
                sku=100026,
                hid=100,
                delivery_buckets=[1234],
                purchase_price=1200,
                buybox_elasticity=sorted_elasticity,
                blue_offers=[
                    BlueOffer(
                        price=1500,
                        offerid="offer.1",
                        feedid=3333,
                        waremd5="BLUE-100026-FEED-1112g",
                        promo=blue_promo_promocode,
                    ),
                    BlueOffer(
                        price=1600,
                        offerid="offer.2",
                        feedid=4444,
                        waremd5="BLUE-100026-FEED-1123g",
                        promo=promo_blue_cashback_with_predicates,
                    ),
                    BlueOffer(
                        price=1300,
                        offerid="offer.3",
                        feedid=1111,
                        waremd5="BLUE-100026-FEED-1136g",
                        promo=blue_promo_cart_promocode,
                    ),
                    BlueOffer(
                        price=1900,
                        offerid="offer.4",
                        feedid=4444,
                        waremd5="BLUE-100026-FEED-1148g",
                        promo=[blue_promo_promocode, promo_blue_cashback_with_predicates],
                    ),
                ],
            ),
        ]

    def test_buybox_promocode_cashback(self):
        """Проверяем, промо типа PROMO_CODE
        В выдаче четыре оффера. По первому применился промо-код, цена, учитываемая байбоксом стала равна 1200
        Ко второму приминился кэшбэк.
        К третьему промокод не применился, потому что в нем есть поле order_min_amount, мы такие игнорируем
        К четвертому применился и кэшбэк и промо-код
        """

        sku = 100026
        d = '&rearr-factors=market_blue_buybox_disable_old_buybox_algo=0;market_blue_buybox_promocode=1;buybox_dropship_bluecashback=1;market_blue_buybox_by_gmv_ue=1;&debug=da'
        response = self.report.request_json(
            'place=productoffers&offers-set=defaultList&market-sku={}&rids=2&perks=yandex_extra_cashback,yandex_cashback'.format(
                sku
            )
            + d
        )

        self.assertFragmentIn(
            response,
            {
                'buyboxDebug': {
                    'Offers': [
                        {
                            'WareMd5': 'BLUE-100026-FEED-1112g',
                            'CashBackSize': 0,
                            'PromoCodeDiscount': 300,
                            'PriceAfterCashback': 1200,
                        },
                        {'WareMd5': 'BLUE-100026-FEED-1123g', 'CashBackSize': 800, 'PriceAfterCashback': 1200},
                        {'WareMd5': 'BLUE-100026-FEED-1136g', 'CashBackSize': 0, 'PriceAfterCashback': 1300},
                        {
                            'WareMd5': 'BLUE-100026-FEED-1148g',
                            'CashBackSize': 800,
                            'PromoCodeDiscount': 300,
                            'PriceAfterCashback': 1200,
                        },
                    ]
                }
            },
        )

    @classmethod
    def prepare_test_buybox_promocode_win(cls):
        # Действующая акция. Скидка по промокоду в относительной величинe (проценты).
        blue_promo_promocode_percent = Promo(
            key='JVvklxUgdnawSJPG4UhZ-3',
            promo_type=PromoType.PROMO_CODE,
            promo_code='promocode_3_text',
            discount_value=10,
        )

        cls.index.mskus += [
            MarketSku(
                title="Тестирование промо типа PROMO_CODE",
                hyperid=2,
                sku=100027,
                hid=100,
                delivery_buckets=[1234],
                purchase_price=1200,
                buybox_elasticity=sorted_elasticity,
                blue_offers=[
                    BlueOffer(
                        price=1500,
                        offerid="offer.5",
                        feedid=3333,
                        waremd5="BLUE-100027-FEED-1112g",
                        promo=blue_promo_promocode_percent,
                    ),
                    BlueOffer(price=1450, offerid="offer.6", feedid=4444, waremd5="BLUE-100027-FEED-1123g"),
                ],
            ),
        ]

    def test_buybox_promocode_win(self):
        """Проверяем, промо типа PROMO_CODE
        Но промо с процентным снижением цены. Оффер с промо выигрывает байбокс.
        """

        sku = 100027
        d = '&rearr-factors=market_blue_buybox_max_gmv_rel=100;market_blue_buybox_promocode=1;buybox_dropship_bluecashback=1;market_blue_buybox_by_gmv_ue=1;&debug=da'
        response = self.report.request_json(
            'place=productoffers&offers-set=defaultList&market-sku={}&rids=2&perks=yandex_extra_cashback,yandex_cashback'.format(
                sku
            )
            + d
        )

        self.assertFragmentIn(
            response,
            {
                'wareId': 'BLUE-100027-FEED-1112g',
                'debug': {
                    'buyboxDebug': {
                        'Offers': [
                            {'WareMd5': 'BLUE-100027-FEED-1112g', 'PromoCodeDiscount': 150, 'PriceAfterCashback': 1350},
                            {'WareMd5': 'BLUE-100027-FEED-1123g', 'PriceAfterCashback': 1450},
                        ]
                    }
                },
            },
        )
        self.assertFragmentIn(
            response, "PromoCode applied for offer BLUE-100027-FEED-1112g. Discount: 150. priceAfterPromos: 1350"
        )

    @classmethod
    def prepare_test_buybox_cheapest_as_gift_win(cls):
        cheapest_as_gift_12_promo = Promo(
            promo_type=PromoType.CHEAPEST_AS_GIFT,
            feed_id=3333,
            key='JVvklxUgdnawSJPGGIFT-2',
            url='http://localhost.ru/',
            cheapest_as_gift=PromoCheapestAsGift(
                offer_ids=[
                    (3333, "offer.9"),
                ],
                count=2,
                promo_url='url',
                link_text='text',
                allow_berubonus=False,
                allow_promocode=False,
            ),
        )
        cheapest_as_gift_23_promo = Promo(
            promo_type=PromoType.CHEAPEST_AS_GIFT,
            feed_id=3333,
            key='JVvklxUgdnawSJPGGIFT-3',
            url='http://localhost.ru/',
            cheapest_as_gift=PromoCheapestAsGift(
                offer_ids=[
                    (3333, "offer.7"),
                ],
                count=3,
                promo_url='url',
                link_text='text',
                allow_berubonus=False,
                allow_promocode=False,
            ),
        )
        cheapest_as_gift_34_promo = Promo(
            promo_type=PromoType.CHEAPEST_AS_GIFT,
            feed_id=3333,
            key='JVvklxUgdnawSJPGGIFT-4',
            url='http://localhost.ru/',
            cheapest_as_gift=PromoCheapestAsGift(
                offer_ids=[
                    (3333, "offer.13"),
                ],
                count=4,
                promo_url='url',
                link_text='text',
                allow_berubonus=False,
                allow_promocode=False,
            ),
        )
        cheapest_as_gift_45_promo = Promo(
            promo_type=PromoType.CHEAPEST_AS_GIFT,
            feed_id=3333,
            key='JVvklxUgdnawSJPGGIFT-5',
            url='http://localhost.ru/',
            cheapest_as_gift=PromoCheapestAsGift(
                offer_ids=[
                    (3333, "offer.15"),
                ],
                count=5,
                promo_url='url',
                link_text='text',
                allow_berubonus=False,
                allow_promocode=False,
            ),
        )
        cheapest_as_gift_default_promo = Promo(
            promo_type=PromoType.CHEAPEST_AS_GIFT,
            feed_id=3333,
            key='JVvklxUgdnawSJPGGIFT-6',
            url='http://localhost.ru/',
            cheapest_as_gift=PromoCheapestAsGift(
                offer_ids=[
                    (3333, "offer.11"),
                ],
                count=6,
                promo_url='url',
                link_text='text',
                allow_berubonus=False,
                allow_promocode=False,
            ),
        )

        cls.index.promos += [
            cheapest_as_gift_default_promo,
            cheapest_as_gift_12_promo,
            cheapest_as_gift_23_promo,
            cheapest_as_gift_34_promo,
            cheapest_as_gift_45_promo,
        ]
        cls.settings.loyalty_enabled = True
        cls.dynamic.loyalty += [
            DynamicBlueGenericBundlesPromos(
                whitelist=[
                    cheapest_as_gift_default_promo.key,
                    cheapest_as_gift_12_promo.key,
                    cheapest_as_gift_23_promo.key,
                    cheapest_as_gift_34_promo.key,
                    cheapest_as_gift_45_promo.key,
                ]
            )
        ]
        # for promo 2=3
        offer_7 = BlueOffer(price=1500, offerid="offer.7", feedid=3333, waremd5="BLUE-100028-FEED-1112g")
        offer_7.promo = [cheapest_as_gift_23_promo]
        offer_8 = BlueOffer(price=1450, offerid="offer.8", feedid=4444, waremd5="BLUE-100028-FEED-1123g")

        # for promo 1=2
        offer_9 = BlueOffer(price=1500, offerid="offer.9", feedid=3333, waremd5="BLUE-100029-FEED-1112g")
        offer_9.promo = [cheapest_as_gift_12_promo]
        offer_10 = BlueOffer(price=1450, offerid="offer.10", feedid=4444, waremd5="BLUE-100029-FEED-1123g")

        # for other promo
        offer_11 = BlueOffer(price=1500, offerid="offer.11", feedid=3333, waremd5="BLUE-100030-FEED-1112g")
        offer_11.promo = [cheapest_as_gift_default_promo]
        offer_12 = BlueOffer(price=1450, offerid="offer.12", feedid=4444, waremd5="BLUE-100030-FEED-1123g")

        # for promo 3=4
        offer_13 = BlueOffer(price=1500, offerid="offer.13", feedid=3333, waremd5="BLUE-100034-FEED-1112g")
        offer_13.promo = [cheapest_as_gift_34_promo]
        offer_14 = BlueOffer(price=1450, offerid="offer.14", feedid=4444, waremd5="BLUE-100034-FEED-1123g")

        # for promo 4=5
        offer_15 = BlueOffer(price=1500, offerid="offer.15", feedid=3333, waremd5="BLUE-100035-FEED-1112g")
        offer_15.promo = [cheapest_as_gift_45_promo]
        offer_16 = BlueOffer(price=1450, offerid="offer.16", feedid=4444, waremd5="BLUE-100035-FEED-1123g")

        elasticity = [
            Elasticity(price_variant=1500, demand_mean=160),
            Elasticity(price_variant=1450, demand_mean=180),
            Elasticity(price_variant=1400, demand_mean=200),
        ]

        cls.index.mskus += [
            MarketSku(
                title="Тестирование промо типа CHEAPEST_AS_GIFT 2=3",
                hyperid=2,
                sku=100028,
                hid=100,
                delivery_buckets=[1234],
                purchase_price=1200,
                buybox_elasticity=elasticity,
                blue_offers=[
                    offer_7,
                    offer_8,
                ],
            ),
            MarketSku(
                title="Тестирование промо типа CHEAPEST_AS_GIFT 1=2",
                hyperid=2,
                sku=100029,
                hid=100,
                delivery_buckets=[1234],
                purchase_price=1200,
                buybox_elasticity=elasticity,
                blue_offers=[
                    offer_9,
                    offer_10,
                ],
            ),
            MarketSku(
                title="Тестирование промо типа CHEAPEST_AS_GIFT",
                hyperid=2,
                sku=100030,
                hid=100,
                delivery_buckets=[1234],
                purchase_price=1200,
                buybox_elasticity=elasticity,
                blue_offers=[
                    offer_11,
                    offer_12,
                ],
            ),
            MarketSku(
                title="Тестирование промо типа CHEAPEST_AS_GIFT 3=4",
                hyperid=2,
                sku=100034,
                hid=100,
                delivery_buckets=[1234],
                purchase_price=1200,
                buybox_elasticity=elasticity,
                blue_offers=[
                    offer_13,
                    offer_14,
                ],
            ),
            MarketSku(
                title="Тестирование промо типа CHEAPEST_AS_GIFT 4=5",
                hyperid=2,
                sku=100035,
                hid=100,
                delivery_buckets=[1234],
                purchase_price=1200,
                buybox_elasticity=elasticity,
                blue_offers=[
                    offer_15,
                    offer_16,
                ],
            ),
        ]

    def test_buybox_cheapest_as_gift_win_23(self):
        """Проверяем, промо типа CHEAPEST_AS_GIFT 2=3. Флаг market_blue_buybox_cheapest_as_gift_12_anlytics_rate.
        Оффер с промо выигрывает байбокс.
        """

        sku = 100028
        rearr_line = '&rearr-factors=market_blue_buybox_cheapest_as_gift_32_anlytics_rate=0.4;'
        response = self.report.request_json(
            'place=productoffers&offers-set=default&hyperid=2&rids=213&market-sku={}&rids=2&rgb=green_with_blue&debug=da'.format(
                sku
            )
            + rearr_line
        )

        self.assertFragmentIn(
            response,
            {
                'wareId': 'BLUE-100028-FEED-1112g',
                'debug': {
                    'buyboxDebug': {
                        'Offers': [
                            {
                                'WareMd5': 'BLUE-100028-FEED-1112g',
                                'CheapestAsGiftDiscount': 160,  # 1500 * 0.4 * 0.333 * 0.8
                                'PriceAfterCashback': 1340,
                            }
                        ]
                    }
                },
            },
        )

    def test_buybox_cheapest_as_gift_win_12(self):
        """Проверяем, промо типа CHEAPEST_AS_GIFT 1=2. Флаг market_blue_buybox_cheapest_as_gift_12_anlytics_rate.
        Оффер с промо выигрывает байбокс.
        """

        sku = 100029
        rearr_line = '&rearr-factors=market_blue_buybox_cheapest_as_gift_12_anlytics_rate=0.6;'
        response = self.report.request_json(
            'place=productoffers&offers-set=default&hyperid=2&rids=213&market-sku={}&rids=2&rgb=green_with_blue&debug=da'.format(
                sku
            )
            + rearr_line
        )

        self.assertFragmentIn(
            response,
            {
                'wareId': 'BLUE-100029-FEED-1112g',
                'debug': {
                    'buyboxDebug': {
                        'Offers': [
                            {
                                'WareMd5': 'BLUE-100029-FEED-1112g',
                                'CheapestAsGiftDiscount': 360,  # 1500 * 0.6 * 0.5 * 0.8
                                'PriceAfterCashback': 1140,
                            }
                        ]
                    }
                },
            },
        )

    def test_buybox_cheapest_as_gift_win(self):
        """Проверяем, промо типа CHEAPEST_AS_GIFT флаг market_blue_buybox_cheapest_as_gift_promo_rate.
        Оффер с промо должен выигрыть ДО, проверяем посчитаную скидку CheapestAsGiftDiscount.
        """

        sku = 100030
        d = '&rearr-factors=market_blue_buybox_cheapest_as_gift_anlytics_rate=0.3;&debug=da'
        response = self.report.request_json(
            'place=productoffers&offers-set=default&hyperid=2&rids=213&market-sku={}&rids=2&rgb=green_with_blue'.format(
                sku
            )
            + d
        )

        self.assertFragmentIn(
            response,
            {
                'wareId': 'BLUE-100030-FEED-1112g',
                'debug': {
                    'buyboxDebug': {
                        'Offers': [
                            {
                                'WareMd5': 'BLUE-100030-FEED-1112g',
                                'CheapestAsGiftDiscount': 60,  # 1500 * 0.3 * 0.16 * 0.8
                                'PriceAfterCashback': 1440,
                            }
                        ]
                    }
                },
            },
        )

    def test_buybox_cheapest_as_gift_win_34(self):
        """Проверяем, промо типа CHEAPEST_AS_GIFT 3=4. Флаг market_blue_buybox_cheapest_as_gift_34_anlytics_rate.
        Оффер с промо выигрывает байбокс.
        """

        sku = 100034
        rearr_line = '&rearr-factors=market_blue_buybox_cheapest_as_gift_34_anlytics_rate=0.44;'
        response = self.report.request_json(
            'place=productoffers&offers-set=default&hyperid=2&rids=213&market-sku={}&rids=2&rgb=green_with_blue&debug=da'.format(
                sku
            )
            + rearr_line
        )

        self.assertFragmentIn(
            response,
            {
                'wareId': 'BLUE-100034-FEED-1112g',
                'debug': {
                    'buyboxDebug': {
                        'Offers': [
                            {
                                'WareMd5': 'BLUE-100034-FEED-1112g',
                                'CheapestAsGiftDiscount': 132,  # 1500 * 0.44 * 0.25 * 0.8
                                'PriceAfterCashback': 1368,
                            }
                        ]
                    }
                },
            },
        )

    def test_buybox_cheapest_as_gift_win_45(self):
        """Проверяем, промо типа CHEAPEST_AS_GIFT 4=5. Флаг market_blue_buybox_cheapest_as_gift_45_anlytics_rate.
        Оффер с промо выигрывает байбокс.
        """

        sku = 100035
        rearr_line = '&rearr-factors=market_blue_buybox_cheapest_as_gift_45_anlytics_rate=0.55;'
        response = self.report.request_json(
            'place=productoffers&offers-set=default&hyperid=2&rids=213&market-sku={}&rids=2&rgb=green_with_blue&debug=da'.format(
                sku
            )
            + rearr_line
        )

        self.assertFragmentIn(
            response,
            {
                'wareId': 'BLUE-100035-FEED-1112g',
                'debug': {
                    'buyboxDebug': {
                        'Offers': [
                            {
                                'WareMd5': 'BLUE-100035-FEED-1112g',
                                'CheapestAsGiftDiscount': 132,  # 1500 * 0.55 * 0.2 * 0.8
                                'PriceAfterCashback': 1368,
                            }
                        ]
                    }
                },
            },
        )

    @classmethod
    def prepare_test_buybox_generic_bundle_win(cls):
        generic_bundle_promo = Promo(
            promo_type=PromoType.GENERIC_BUNDLE,
            feed_id=3333,
            key='JVvklxUgdnawSJPG4UhZ-5',
            generic_bundles_content=[
                make_generic_bundle_content("offer.9", "offer.2"),
            ],
        )

        cls.index.promos += [generic_bundle_promo]
        cls.settings.loyalty_enabled = True
        cls.dynamic.loyalty += [DynamicBlueGenericBundlesPromos(whitelist=[generic_bundle_promo.key])]

        offer_9 = BlueOffer(price=1500, offerid="offer.9", feedid=3333, waremd5="BLUE-100031-FEED-1112g")
        offer_9.promo = [
            generic_bundle_promo,
        ]
        offer_10 = BlueOffer(price=1400, offerid="offer.10", feedid=4444, waremd5="BLUE-100031-FEED-1123g")

        cls.index.mskus += [
            MarketSku(
                title="Тестирование промо типа GENERIC_BUNDLE",
                hyperid=2,
                sku=100031,
                hid=100,
                delivery_buckets=[1234],
                purchase_price=1200,
                buybox_elasticity=sorted_elasticity,
                blue_offers=[
                    offer_9,
                    offer_10,
                ],
            ),
        ]

    def test_buybox_generic_bundle_win(self):
        """Проверяем, промо типа GENERIC_BUNDLE
        Оффер с промо выигрывает байбокс.
        """

        sku = 100031
        d = '&rearr-factors=market_blue_buybox_max_gmv_rel=100;buybox_dropship_bluecashback=1;market_blue_buybox_by_gmv_ue=1;&debug=da'
        response = self.report.request_json(
            'place=productoffers&offers-set=defaultList&market-sku={}&rids=2&perks=yandex_extra_cashback,yandex_cashback'.format(
                sku
            )
            + d
        )

        self.assertFragmentIn(
            response,
            {
                'wareId': 'BLUE-100031-FEED-1112g',
                'debug': {
                    'buyboxDebug': {
                        'Offers': [
                            {
                                'WareMd5': 'BLUE-100031-FEED-1112g',
                                'GenericBundleDiscount': 150,
                                'PriceAfterCashback': 1350,
                            },
                            {'WareMd5': 'BLUE-100031-FEED-1123g', 'PriceAfterCashback': 1400},
                        ]
                    }
                },
            },
        )

    @classmethod
    def prepare_test_buybox_promocode_gmv_reject(cls):
        # Действующая акция. Скидка по промокоду в относительной величинe (проценты).
        blue_promo_promocode_percent2 = Promo(
            key='JVvklxUgdnawSJPG4UhZ-6',
            promo_type=PromoType.PROMO_CODE,
            promo_code='promocode_4_text',
            discount_value=7,
        )

        cls.index.mskus += [
            MarketSku(
                title="Тестирование отсечение оффера по gmv",
                hyperid=2,
                sku=100032,
                hid=100,
                delivery_buckets=[1234],
                purchase_price=1200,
                buybox_elasticity=sorted_elasticity,
                blue_offers=[
                    BlueOffer(
                        price=109, feedid=3333, waremd5="BLUE-100032-FEED-1112g", promo=blue_promo_promocode_percent2
                    ),
                    BlueOffer(price=101, feedid=4444, waremd5="BLUE-100032-FEED-1123g"),
                ],
            ),
        ]

    def test_buybox_promocode_gmv_reject(self):
        """Проверяем, что оффер с промо не будет отсечен по цене или по gmv
        Он сильно дороже, но из-за промо он попадает в байбокс контест и не отсекается
        """

        sku = 100032
        d = '&rearr-factors=market_blue_buybox_promocode=1;buybox_dropship_bluecashback=1;market_blue_buybox_by_gmv_ue=1;&debug=da'
        response = self.report.request_json(
            'place=productoffers&offers-set=defaultList&market-sku={}&rids=2&perks=yandex_extra_cashback,yandex_cashback'.format(
                sku
            )
            + d
        )

        self.assertFragmentIn(
            response,
            {
                'wareId': 'BLUE-100032-FEED-1112g',
                'debug': {
                    'buyboxDebug': {
                        'Offers': [
                            {'WareMd5': 'BLUE-100032-FEED-1112g', 'PromoCodeDiscount': 7, 'PriceAfterCashback': 102},
                            {'WareMd5': 'BLUE-100032-FEED-1123g', 'PriceAfterCashback': 101},
                        ]
                    }
                },
            },
        )

    @classmethod
    def prepare_test_buybox_min_price_after_promos(cls):
        # Действующая акция. Скидка по промокоду в абсолютной величинe (рублях).
        blue_promo_promocode = Promo(
            key='JVvklxUgdnawSJPG4UhZ-7',
            promo_type=PromoType.PROMO_CODE,
            promo_code='promocode_1_text',
            discount_value=300,
            discount_currency='RUR',
        )

        cls.index.mskus += [
            MarketSku(
                title="Тестирование промо типа PROMO_CODE",
                hyperid=2,
                sku=100036,
                hid=100,
                delivery_buckets=[1234],
                purchase_price=1200,
                buybox_elasticity=sorted_elasticity,
                blue_offers=[
                    BlueOffer(
                        price=1000,  # this is a winner
                        offerid="offer.1",
                        feedid=3333,
                        waremd5="BLUE-100036-FEED-1112g",
                        promo=blue_promo_promocode,
                    ),
                    BlueOffer(
                        price=1500,  # this offer should be filtered in any case
                        offerid="offer.2",
                        feedid=3333,
                        waremd5="BLUE-100036-FEED-1113g",
                    ),
                    BlueOffer(
                        price=1040,  # this offer should be filtered by TOO_HIGH_PRICE in case using MinPriceAfterPromos but shouldn't otherwise
                        offerid="offer.3",
                        feedid=3333,
                        waremd5="BLUE-100036-FEED-1114g",
                    ),
                ],
            ),
        ]

    def test_buybox_min_price_after_promos(self):
        """Проверяем, что учитывается минимальная цена после применения промо в ценовомй фильтрации"""

        sku = 100036
        d = '&rearr-factors=market_blue_buybox_use_min_price_after_promos=0'
        response = self.report.request_json(
            'place=productoffers&offers-set=defaultList&market-sku={}&rids=1&perks=yandex_extra_cashback,yandex_cashback&debug=da'.format(
                sku
            )
            + d
        )

        self.assertFragmentIn(
            response,
            {
                'buyboxDebug': {
                    'Offers': [
                        {
                            'WareMd5': 'BLUE-100036-FEED-1112g',
                            'CashBackSize': 0,
                            'PriceAfterCashback': 700,
                        },
                        {'WareMd5': 'BLUE-100036-FEED-1114g'},
                        # this offer is not filtered because for filtering we are using source minPrice(=1000) here
                    ],
                    'RejectedOffers': [
                        {'Offer': {'WareMd5': 'BLUE-100036-FEED-1113g'}, "RejectReason": "TOO_HIGH_PRICE"},
                    ],
                }
            },
        )

        d = '&rearr-factors=market_blue_buybox_use_min_price_after_promos=1'
        response = self.report.request_json(
            'place=productoffers&offers-set=defaultList&market-sku={}&rids=1&perks=yandex_extra_cashback,yandex_cashback&debug=da'.format(
                sku
            )
            + d
        )

        self.assertFragmentIn(
            response,
            {
                'buyboxDebug': {
                    'Offers': [
                        {
                            'WareMd5': 'BLUE-100036-FEED-1112g',
                            'CashBackSize': 0,
                            'PriceAfterCashback': 700,
                        },
                    ],
                    'RejectedOffers': [
                        {'Offer': {'WareMd5': 'BLUE-100036-FEED-1113g'}, "RejectReason": "TOO_HIGH_PRICE"},
                        {
                            'Offer': {'WareMd5': 'BLUE-100036-FEED-1114g'},
                            "RejectReason": "TOO_HIGH_PRICE",
                        },  # this offer is filtered because for filtering we are using minPriceAfterPromo(=700) here
                    ],
                }
            },
        )

    @classmethod
    def prepare_test_buybox_use_price_after_promos(cls):
        # Действующая акция. Скидка по промокоду в абсолютной величинe (рублях).
        blue_promo_promocode = Promo(
            key='JVvklxUgdnawSJPG4UhZ-8',
            promo_type=PromoType.PROMO_CODE,
            promo_code='promocode_1_text',
            discount_value=60,
            discount_currency='RUR',
        )
        cls.index.promos += [blue_promo_promocode]

        cls.index.mskus += [
            MarketSku(
                title="Тестирование промо типа PROMO_CODE",
                hyperid=2,
                sku=100037,
                hid=100,
                delivery_buckets=[1234],
                purchase_price=1200,
                buybox_elasticity=sorted_elasticity,
                blue_offers=[
                    BlueOffer(
                        price=1030,  # this offer should win in case using price after promo(because price=1030-60=970)
                        offerid="offer.1",
                        feedid=3333,
                        waremd5="BLUE-100037-FEED-1112g",
                        promo=blue_promo_promocode,
                    ),
                    BlueOffer(price=1015, offerid="offer.2", feedid=3333, waremd5="BLUE-100037-FEED-1113g"),
                    BlueOffer(price=1000, offerid="offer.3", feedid=3333, waremd5="BLUE-100037-FEED-1114g"),
                ],
            ),
        ]

    def test_buybox_use_price_after_promos(self):
        """Проверяем, что учитывается цена после применения промо в ценовомй фильтрации"""

        sku = 100037
        d = '&rearr-factors=market_blue_buybox_use_min_price_after_promos=0;market_blue_buybox_fbs_hybrid_local_warehouse_priority=0;'
        response = self.report.request_json(
            'place=productoffers&offers-set=defaultList&market-sku={}&rids=2&perks=yandex_extra_cashback,yandex_cashback&debug=da'.format(
                sku
            )
            + d
        )

        self.assertFragmentIn(
            response,
            {
                'wareId': 'BLUE-100037-FEED-1112g',
                'debug': {
                    'buyboxDebug': {
                        'Offers': [
                            {
                                'WareMd5': 'BLUE-100037-FEED-1112g',
                                'PriceAfterCashback': 970,
                                'Gmv': 1418.41,
                            },  # Gmv = originPrice * conversion; originPrice = 1030
                            {
                                'WareMd5': 'BLUE-100037-FEED-1113g',
                                'PriceAfterCashback': 1015,
                                'Gmv': 1191.86,
                            },
                            {
                                'WareMd5': 'BLUE-100037-FEED-1114g',
                                'PriceAfterCashback': 1000,
                                'Gmv': 1238.97,
                            },
                        ],
                    }
                },
            },
        )

        d = '&rearr-factors=market_blue_buybox_use_min_price_after_promos=1;market_blue_buybox_fbs_hybrid_local_warehouse_priority=0;'
        response = self.report.request_json(
            'place=productoffers&offers-set=defaultList&market-sku={}&rids=2&perks=yandex_extra_cashback,yandex_cashback&debug=da'.format(
                sku
            )
            + d
        )

        self.assertFragmentIn(
            response,
            {
                'wareId': 'BLUE-100037-FEED-1112g',
                'debug': {
                    'buyboxDebug': {
                        'Offers': [
                            {
                                'WareMd5': 'BLUE-100037-FEED-1112g',
                                'PriceAfterCashback': 970,
                                'Gmv': 1335.78,
                            },  # Gmv was changed because we are using another price here: Gmv = priceAfterPricePromos * conversion; priceAfterPricePromos = 970
                            {'WareMd5': 'BLUE-100037-FEED-1114g', 'PriceAfterCashback': 1000, 'Gmv': 1238.97},
                            {
                                'WareMd5': 'BLUE-100037-FEED-1113g',
                                'PriceAfterCashback': 1015,
                                'Gmv': 1191.86,
                            },
                        ],
                    }
                },
            },
        )

    def test_personal_discount_in_gmv_promocode(self):
        """
        Проверяем, что в байбоксе при расчёте GMV в минимальных ценах верно учитывается персональная скидка (в виде промокода)
        Тут байбокс разыгрывается между 2 офферами, единственная разница - у одного есть персональная скидка
        """
        sku = 120026
        rearr_flags_dict = {
            'personal_promo_direct_discount_enabled': 1,  # Активация персональной скидки
            'personal_promo_enabled': 1,
        }
        rearr_flags_str = 'rearr-factors=' + ';'.join(
            [rearr_flag + '=' + str(rearr_flags_dict[rearr_flag]) for rearr_flag in rearr_flags_dict]
        )
        request_str = (
            'place=productoffers&offers-set=defaultList&market-sku={}&rids=213&debug=da&regset=0&rgb=green_with_blue&'.format(
                sku
            )
            + rearr_flags_str
        )
        request_str += '&perks={}'.format(PERSONAL_PROMOCODE_PERK)
        response = self.report.request_json(request_str)
        PriceWinner, GmvWinner, ConversionWinner, ElsWinner = Capture(), Capture(), Capture(), Capture()
        PriceLoser, GmvLoser, ConversionLoser, ElsLoser = Capture(), Capture(), Capture(), Capture()
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'slug': 'with-personal-discount',
                            'wareId': 'BLUE-120026-FEED-1112g',
                            'debug': {
                                'buyboxDebug': {
                                    'Offers': [
                                        {
                                            'WareMd5': 'BLUE-120026-FEED-1112g',  # Это оффер с персональной скидкой
                                            'PriceAfterCashback': NotEmpty(capture=PriceWinner),
                                            # По сути нам надо PriceAfterPricePromos вместо PriceAfterCashback, но поскольку они равны, то в ответе только PriceAfterCashback
                                            'PriceAfterPricePromos': Absent(),  # Проверяем, что PriceAfterPricePromos == PriceAfterCashback
                                            'Gmv': NotEmpty(capture=GmvWinner),
                                            'Conversion': NotEmpty(capture=ConversionWinner),
                                            'PredictedElasticity': {
                                                'Value': NotEmpty(capture=ElsWinner),
                                                'Type': 'NORMAL',  # Разница конверсий должна быть только в эластичности
                                            },
                                        },
                                        {
                                            'WareMd5': 'BLUE-120026-FEED-1123g',
                                            'PriceAfterCashback': NotEmpty(capture=PriceLoser),
                                            'PriceAfterPricePromos': Absent(),
                                            'Gmv': NotEmpty(capture=GmvLoser),
                                            'Conversion': NotEmpty(capture=ConversionLoser),
                                            'PredictedElasticity': {
                                                'Value': NotEmpty(capture=ElsLoser),
                                                'Type': 'NORMAL',
                                            },
                                        },
                                    ],
                                },
                            },
                        },
                    ],
                },
            },
        )
        # Проверяем, что GMV вычислялось для обоих офферов
        self.assertTrue(GmvWinner.value > 0 and GmvLoser.value > 0)
        # Проверяем, что персональная скидка применилась только к одному офферу
        self.assertTrue(PriceWinner.value < PriceLoser.value)
        # Цена в конверсии учитывается через эластичность, поэтому вместо пропорции проверяем неравенство
        self.assertTrue(abs(ConversionWinner.value - ConversionLoser.value) > 1e-2)
        # Проверяем, что конверсии относятся как эластичности (разница - только в цене => разница конверсий - только в эластичностях)
        self.assertAlmostEqual(
            ConversionWinner.value / ConversionLoser.value, float(ElsWinner.value) / ElsLoser.value, delta=1e-2
        )
        # Проверяем, что берётся верная цена в формуле GMV = price * conversion
        for gmv, conversion, gmv_price in [
            (GmvWinner, ConversionWinner, PriceWinner),
            (GmvLoser, ConversionLoser, PriceLoser),
        ]:
            # Тут намеренно большая дельта, для Gmv в репорте используют флоты, перемножают числа большого порядка (знаки после запятой будут врать)
            self.assertAlmostEqual(gmv_price.value * conversion.value, gmv.value, delta=1)

    def test_personal_discount_in_gmv_direct_discount(self):
        """
        Проверяем, что в байбоксе при расчёте GMV в минимальных ценах верно учитывается персональная скидка (прямая скидка)
        Тут байбокс разыгрывается между 2 офферами, единственная разница - у одного есть персональная скидка
        """
        sku = 120027
        rearr_flags_dict = {
            'personal_promo_direct_discount_enabled': 1,  # Активация персональной скидки
        }
        rearr_flags_str = 'rearr-factors=' + ';'.join(
            [rearr_flag + '=' + str(rearr_flags_dict[rearr_flag]) for rearr_flag in rearr_flags_dict]
        )
        request_str = (
            'place=productoffers&offers-set=defaultList&market-sku={}&rids=213&debug=da&regset=0&rgb=green_with_blue&'.format(
                sku
            )
            + rearr_flags_str
        )
        request_str += '&perks={}'.format(PERSONAL_DISCOUNT_PERK)
        response = self.report.request_json(request_str)
        PriceWinner, GmvWinner, ConversionWinner, ElsWinner = Capture(), Capture(), Capture(), Capture()
        PriceLoser, GmvLoser, ConversionLoser, ElsLoser = Capture(), Capture(), Capture(), Capture()
        price_before_discount, price_after_discount = 200, 170
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'slug': 'with-personal-discount',
                            'wareId': 'BLUE-120027-FEED-1112g',
                            'prices': {  # Обязательно надо проверить, что после байбокса скидка не применилась второй раз
                                'value': str(170),
                                'discount': {
                                    'oldMin': str(price_before_discount),
                                    'percent': 15,
                                    'absolute': str(price_before_discount - price_after_discount),
                                },
                            },
                            'debug': {
                                'buyboxDebug': {
                                    'Offers': [
                                        {
                                            'WareMd5': 'BLUE-120027-FEED-1112g',  # Это оффер с персональной скидкой
                                            'PriceAfterCashback': NotEmpty(capture=PriceWinner),
                                            # По сути нам надо PriceAfterPricePromos вместо PriceAfterCashback, но поскольку они равны, то в ответе только PriceAfterCashback
                                            'PriceAfterPricePromos': Absent(),  # Проверяем, что PriceAfterPricePromos == PriceAfterCashback
                                            'Gmv': NotEmpty(capture=GmvWinner),
                                            'Conversion': NotEmpty(capture=ConversionWinner),
                                            'PredictedElasticity': {
                                                'Value': NotEmpty(capture=ElsWinner),
                                                'Type': 'NORMAL',  # Разница конверсий должна быть только в эластичности
                                            },
                                        },
                                        {
                                            'WareMd5': 'BLUE-120027-FEED-1123g',
                                            'PriceAfterCashback': NotEmpty(capture=PriceLoser),
                                            'PriceAfterPricePromos': Absent(),
                                            'Gmv': NotEmpty(capture=GmvLoser),
                                            'Conversion': NotEmpty(capture=ConversionLoser),
                                            'PredictedElasticity': {
                                                'Value': NotEmpty(capture=ElsLoser),
                                                'Type': 'NORMAL',
                                            },
                                        },
                                    ],
                                },
                            },
                        },
                    ],
                },
            },
        )
        # Проверяем, что GMV вычислялось для обоих офферов
        self.assertTrue(GmvWinner.value > 0 and GmvLoser.value > 0)
        # Проверяем, что персональная скидка применилась только к одному офферу
        self.assertTrue(PriceWinner.value < PriceLoser.value)
        self.assertAlmostEqual(PriceWinner.value, price_after_discount, delta=1e-8)
        self.assertAlmostEqual(PriceLoser.value, price_before_discount, delta=1e-8)
        # Цена в конверсии учитывается через эластичность, поэтому вместо пропорции проверяем неравенство
        self.assertTrue(abs(ConversionWinner.value - ConversionLoser.value) > 1e-2)
        # Проверяем, что конверсии относятся как эластичности (разница - только в цене => разница конверсий - только в эластичностях)
        self.assertAlmostEqual(
            ConversionWinner.value / ConversionLoser.value, float(ElsWinner.value) / ElsLoser.value, delta=1e-2
        )
        # Проверяем, что берётся верная цена в формуле GMV = price * conversion
        for gmv, conversion, gmv_price in [
            (GmvWinner, ConversionWinner, PriceWinner),
            (GmvLoser, ConversionLoser, PriceLoser),
        ]:
            # Тут намеренно большая дельта, для Gmv в репорте используют флоты, перемножают числа большого порядка (знаки после запятой будут врать)
            self.assertAlmostEqual(gmv_price.value * conversion.value, gmv.value, delta=1)

    def test_wrong_personal_discount_loses(self):
        """
        Байбокс разыгрывается между 5 офферами, оффера с неправильными персональными скидками должны проиграть (и скидка не должна примениться)
        """
        sku = 120029
        rearr_flags_dict = {
            'personal_promo_direct_discount_enabled': 1,  # Активация персональной скидки
        }
        rearr_flags_str = 'rearr-factors=' + ';'.join(
            [rearr_flag + '=' + str(rearr_flags_dict[rearr_flag]) for rearr_flag in rearr_flags_dict]
        )
        request_str = (
            'place=productoffers&offers-set=defaultList&market-sku={}&rids=213&debug=da&regset=0&rgb=green_with_blue&'.format(
                sku
            )
            + rearr_flags_str
        )
        request_str += '&perks={}'.format(PERSONAL_WRONG_DISCOUNT_PERK)
        response = self.report.request_json(request_str)
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'slug': 'no-personal-discount',
                            'wareId': 'BLUE-120029-FEED-1123g',
                            'prices': {
                                'value': str(1000),
                            },
                            'debug': {
                                'buyboxDebug': {
                                    'Offers': [
                                        {
                                            'WareMd5': 'BLUE-120029-FEED-1123g',  # Это оффер без персональной скидки
                                            'PriceAfterCashback': 1000,
                                        },
                                    ],
                                    'RejectedOffers': [
                                        {
                                            'Offer': {
                                                'WareMd5': 'BLUE-120029-FEED-1112g',
                                                'PriceAfterCashback': 2000,
                                            }
                                        },
                                        {
                                            'Offer': {
                                                'WareMd5': 'BLUE-120029-FEED-1113g',
                                                'PriceAfterCashback': 2000,
                                            }
                                        },
                                        {
                                            'Offer': {
                                                'WareMd5': 'BLUE-120029-FEED-1114g',
                                                'PriceAfterCashback': 2000,
                                            }
                                        },
                                        {
                                            'Offer': {
                                                'WareMd5': 'BLUE-120029-FEED-1115g',
                                                'PriceAfterCashback': 2000,
                                            }
                                        },
                                    ],
                                },
                            },
                        },
                    ],
                },
            },
        )

    def test_no_profit_personal_discount_loses(self):
        """
        Байбокс разыгрывается между 2 офферами, оффер с невыгодной персональными скидкой должен проиграть (и скидка не должна примениться)
        """
        sku = 120030
        rearr_flags_dict = {
            'personal_promo_direct_discount_enabled': 1,  # Активация персональной скидки
        }
        rearr_flags_str = 'rearr-factors=' + ';'.join(
            [rearr_flag + '=' + str(rearr_flags_dict[rearr_flag]) for rearr_flag in rearr_flags_dict]
        )
        request_str = (
            'place=productoffers&offers-set=defaultList&market-sku={}&rids=213&debug=da&regset=0&rgb=green_with_blue&'.format(
                sku
            )
            + rearr_flags_str
        )
        request_str += '&perks={}'.format(PERSONAL_NO_PROFIT_DISCOUNT_PERK)
        response = self.report.request_json(request_str)
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'slug': 'with-no-profit-personal-discount',
                            'wareId': 'BLUE-120030-FEED-1112g',
                            'prices': {
                                'value': str(12),
                            },
                            'debug': {
                                'buyboxDebug': {
                                    'Offers': [
                                        {
                                            'WareMd5': 'BLUE-120030-FEED-1112g',  # Это оффер без персональной скидки
                                            'PriceAfterCashback': 12,
                                        },
                                    ],
                                    'RejectedOffers': [
                                        {
                                            'Offer': {
                                                'WareMd5': 'BLUE-120030-FEED-1113g',
                                                'PriceAfterCashback': 20,
                                            }
                                        },
                                    ],
                                },
                            },
                        },
                    ],
                },
            },
        )

    @classmethod
    def prepare_dsbs_with_cashback(cls):
        cls.index.delivery_buckets += [
            DeliveryBucket(
                bucket_id=4240,
                fesh=6,
                carriers=[99],
                regional_options=[RegionalDelivery(rid=213, options=[DeliveryOption(price=100, day_from=1, day_to=1)])],
            ),
        ]
        cls.index.shops += [
            Shop(
                fesh=6,
                datafeed_id=6,
                business_fesh=6,
                name="dsbs магазин Пети",
                regions=[225],
                cis=Shop.CIS_REAL,
                cpa=Shop.CPA_REAL,
                cpc=Shop.CPC_NO,
                priority_region=213,
                warehouse_id=145,
            ),
        ]
        cls.index.models += [
            Model(hid=102, ts=500, hyperid=1, title='model_1', vbid=10),
        ]
        cls.index.mskus += [
            MarketSku(
                title="Выигрывает dsbs-оффер за счёт кэшбека",
                hyperid=1,
                sku=100040,
                hid=102,
                delivery_buckets=[1234],
                blue_offers=[
                    BlueOffer(
                        title="100040 blue cheap offer",
                        price=1000,
                        feedid=1111,
                        waremd5="BLUE-100040-FEED-1112g",
                    ),
                ],
            ),
        ]
        cls.index.offers += [
            Offer(
                title="market DSBS Offer msku 100040 with cashback",
                hid=102,
                hyperid=1,
                price=1100,
                fesh=6,
                business_id=6,
                sku=100040,
                cpa=Offer.CPA_REAL,
                waremd5='sgf1xWYFqdGiLh4TT-222Q',
                feedid=6,
                delivery_buckets=[4240],
                ts=510,
                promo=promo_blue_cashback_with_predicates,
            ),
        ]

    def test_dsbs_with_cashback(self):
        """
        Проверяем, что на dsbs офферах кэшбек в байбоксе учитывается
        """
        sku = 100040
        rearr_flags_dict = {}
        request_base = 'place=productoffers&offers-set=defaultList&market-sku={}&rids=213&perks=yandex_cashback,yandex_extra_cashback&debug=da&rearr-factors='.format(
            sku
        )
        response = self.report.request_json(request_base + dict_to_rearr(rearr_flags_dict))
        # Dsbs оффер дороже, чем синий, но он должен выиграть байбокс за счёт кэшбека
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'wareId': 'sgf1xWYFqdGiLh4TT-222Q',
                            'debug': {
                                'buyboxDebug': {
                                    'Offers': [
                                        {
                                            'WareMd5': 'sgf1xWYFqdGiLh4TT-222Q',
                                            'CashBackSize': 550,
                                            'PriceAfterPricePromos': 1100,
                                            'PriceAfterCashback': 825,  # в байбоксе от цены отнимаем половину кэшбека
                                            'IsDsbs': True,
                                        },
                                        {
                                            'WareMd5': 'BLUE-100040-FEED-1112g',
                                            'CashBackSize': 0,
                                            'PriceAfterCashback': 1000,
                                        },
                                    ],
                                }
                            },
                        }
                    ]
                }
            },
        )
        # Проверим, что можно вернуть прежнее поведение флагом фикса - кэшбек к dsbs не применится, и он проиграет из-за высокой цены
        rearr_flags_dict["market_buybox_cashback_dsbs_fix"] = 0
        response = self.report.request_json(request_base + dict_to_rearr(rearr_flags_dict))
        self.assertFragmentIn(
            response,
            {
                'search': {
                    'results': [
                        {
                            'wareId': 'BLUE-100040-FEED-1112g',
                            'debug': {
                                'buyboxDebug': {
                                    'Offers': [
                                        {
                                            'WareMd5': 'BLUE-100040-FEED-1112g',
                                            'CashBackSize': 0,
                                            'PriceAfterCashback': 1000,
                                        },
                                        {
                                            'WareMd5': 'sgf1xWYFqdGiLh4TT-222Q',
                                            'CashBackSize': 0,
                                            'PriceAfterCashback': 1100,
                                        },
                                    ],
                                }
                            },
                        }
                    ]
                }
            },
        )
        self.assertFragmentIn(response, "Cashback was not applied for dsbs;")


if __name__ == '__main__':
    main()
