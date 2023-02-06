#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.matcher import Absent, NotEmpty
from core.report import REQUEST_TIMESTAMP
from core.testcase import TestCase, main
from core.types.dynamic_pricing_strategy_ssku import DYNAMIC_PRICING_TYPE
from core.types.offer_promo import PromoDirectDiscount, PromoRestrictions, OffersMatchingRules
from core.types.autogen import b64url_md5
from datetime import datetime, timedelta
from itertools import count
from math import floor
from market.proto.common.promo_pb2 import ESourceType
from core.types import (
    BlueOffer,
    DynamicPriceControlData,
    DynamicPricingStrategySSKU,
    MarketSku,
    Offer,
    Promo,
    PromoType,
    RegionalMsku,
    RtyOffer,
    Shop,
)

now = datetime.fromtimestamp(REQUEST_TIMESTAMP)
delta_big = timedelta(days=1)
delta_small = timedelta(hours=5)


FEED_ID = 777
DEFAULT_HID = 10
HID_1 = 233100
HID_2 = 233101
HID_3 = 233102
HID_4 = 233103

nummer = count()


def get_offer_id(x):
    return 'offer_id_{}'.format(x)


def __blue_offer(
    offer_id,
    feed_id=FEED_ID,
    fesh=FEED_ID,
    price=1000,
    price_old=1000,
    promo=None,
    is_fulfillment=True,
    purchase_price=None,
    ref_min_price=None,
):
    num = next(nummer)
    return BlueOffer(
        waremd5=b64url_md5(num),
        price=price,
        price_old=price_old,
        fesh=fesh,
        feedid=feed_id,
        offerid=get_offer_id(offer_id),
        promo=promo,
        is_fulfillment=is_fulfillment,
        purchase_price=purchase_price,
        ref_min_price=ref_min_price,
        use_custom_regional_stats=True,
    )


def __msku(offers, hid=DEFAULT_HID):
    num = next(nummer)
    return MarketSku(sku=num, hyperid=num, hid=hid, blue_offers=offers if isinstance(offers, list) else [offers])


promo_gr75 = Promo(
    promo_type=PromoType.DIRECT_DISCOUNT,
    key="dSqg9t-_4oPE7Rs_dHVrlA",
    feed_id=FEED_ID,
    source_type=ESourceType.DCO_3P_DISCOUNT,
    direct_discount=PromoDirectDiscount(
        items=[
            {
                'feed_id': FEED_ID,
                'offer_id': get_offer_id(111),
                'discount_price': {'value': 240, 'currency': 'RUR'},
                'old_price': {'value': 1000, 'currency': 'RUR'},
                'max_discount': {'value': 1000, 'currency': 'RUR'},
                'max_discount_percent': 100.0,
            },
        ],
        allow_berubonus=True,
        allow_promocode=True,
    ),
    offers_matching_rules=[
        OffersMatchingRules(
            feed_offer_ids=[
                [FEED_ID, get_offer_id(111)],
            ]
        )
    ],
)

blue_offer_gr75 = __blue_offer(
    price=1000, price_old=1000, offer_id=111, promo=promo_gr75, fesh=FEED_ID, feed_id=FEED_ID
)
msku_gr75 = __msku(blue_offer_gr75)

# Действующая акция
promo1 = Promo(
    promo_type=PromoType.DIRECT_DISCOUNT,
    feed_id=FEED_ID,
    key=b64url_md5(next(nummer)),
    url='http://direct_discount_1.com/',
    shop_promo_id='promo1',
    conditions='ccc',
    direct_discount=PromoDirectDiscount(
        items=[
            {
                'feed_id': FEED_ID,
                'offer_id': get_offer_id(1),
                'discount_price': {'value': 1234, 'currency': 'RUR'},
                'old_price': {'value': 12345, 'currency': 'RUR'},
            }
        ],
        allow_berubonus=True,
        allow_promocode=True,
    ),
    offers_matching_rules=[
        OffersMatchingRules(
            feed_offer_ids=[
                [FEED_ID, get_offer_id(1)],
            ]
        )
    ],
)

# Акция уже закончилась
promo2 = Promo(
    promo_type=PromoType.DIRECT_DISCOUNT,
    feed_id=FEED_ID,
    key=b64url_md5(next(nummer)),
    start_date=now + delta_small,
    direct_discount=PromoDirectDiscount(
        items=[
            {
                'feed_id': FEED_ID,
                'offer_id': get_offer_id(2),
                'discount_price': {'value': 1300, 'currency': 'RUR'},
                'old_price': {'value': 1500, 'currency': 'RUR'},
            }
        ],
        allow_berubonus=True,
        allow_promocode=True,
    ),
    offers_matching_rules=[
        OffersMatchingRules(
            feed_offer_ids=[
                [FEED_ID, get_offer_id(2)],
            ]
        )
    ],
)

# Акция ещё не началась
promo3 = Promo(
    promo_type=PromoType.DIRECT_DISCOUNT,
    feed_id=FEED_ID,
    key=b64url_md5(next(nummer)),
    end_date=now - delta_small,
    direct_discount=PromoDirectDiscount(
        items=[
            {
                'feed_id': FEED_ID,
                'offer_id': get_offer_id(3),
                'discount_price': {'value': 1000, 'currency': 'RUR'},
                'old_price': {'value': 1100, 'currency': 'RUR'},
            }
        ],
        allow_berubonus=True,
        allow_promocode=True,
    ),
    offers_matching_rules=[
        OffersMatchingRules(
            feed_offer_ids=[
                [FEED_ID, get_offer_id(3)],
            ]
        )
    ],
)

# Тест учёта промо в buybox
promo4 = Promo(
    promo_type=PromoType.DIRECT_DISCOUNT,
    feed_id=FEED_ID,
    key=b64url_md5(next(nummer)),
    shop_promo_id='promo4',
    source_type=ESourceType.DCO_3P_DISCOUNT,
    direct_discount=PromoDirectDiscount(
        items=[
            {
                'feed_id': FEED_ID,
                'offer_id': get_offer_id(4),
                'discount_price': {'value': 700, 'currency': 'RUR'},
                'old_price': {'value': 1500, 'currency': 'RUR'},
                'max_discount': {'value': 2000, 'currency': 'RUR'},
                'max_discount_percent': 45.0,
            }
        ],
    ),
    offers_matching_rules=[
        OffersMatchingRules(
            feed_offer_ids=[
                [FEED_ID, get_offer_id(4)],
            ]
        )
    ],
)

blue_offer_1 = __blue_offer(price=1235, price_old=2000, offer_id=1, promo=promo1)
blue_offer_2 = __blue_offer(offer_id=2, promo=promo2)
blue_offer_3 = __blue_offer(offer_id=3, promo=promo3)
blue_offer_4 = __blue_offer(price=1000, offer_id=4, promo=promo4)
blue_offer_4a = __blue_offer(offer_id=100, price=800)

msku_1 = __msku(blue_offer_1)
msku_2 = __msku(blue_offer_2)
msku_3 = __msku(blue_offer_3)
msku_4 = __msku([blue_offer_4, blue_offer_4a])

# Действующая акция на две категории с различным процентом скидки
promo5 = Promo(
    promo_type=PromoType.DIRECT_DISCOUNT,
    feed_id=FEED_ID,
    key=b64url_md5(next(nummer)),
    url='http://direct_discount_5.com/',
    shop_promo_id='promo5',
    direct_discount=PromoDirectDiscount(
        discounts_by_category=[
            {
                'category_restriction': {
                    'categories': [
                        HID_1,
                    ]
                },
                'discount_percent': 10.0,
            },
            {
                'category_restriction': {
                    'categories': [
                        HID_2,
                    ]
                },
                'discount_percent': 15.0,
            },
        ],
        allow_berubonus=True,
        allow_promocode=True,
    ),
    offers_matching_rules=[
        OffersMatchingRules(categories=[HID_1, HID_2]),
    ],
)

blue_offer_5 = __blue_offer(price=890, price_old=1000, offer_id=5, promo=promo5)
blue_offer_6 = __blue_offer(price=900, price_old=1000, offer_id=6, promo=promo5)
msku_5 = __msku(
    [
        blue_offer_5,
    ],
    hid=HID_1,
)
msku_6 = __msku(
    [
        blue_offer_6,
    ],
    hid=HID_2,
)


# Действующая акция на оффер, у которого не задаются ни price, ни oldPrice.
# Такая акция нужна только для группировки офферов на лендинге
promo6 = Promo(
    promo_type=PromoType.DIRECT_DISCOUNT,
    feed_id=FEED_ID,
    key=b64url_md5(next(nummer)),
    url='http://direct_discount_6.com/',
    shop_promo_id='promo6',
    direct_discount=PromoDirectDiscount(
        items=[
            {
                'feed_id': FEED_ID,
                'offer_id': get_offer_id(7),
            },
            {
                'feed_id': FEED_ID,
                'offer_id': get_offer_id(8),
            },
            {
                'feed_id': FEED_ID,
                'offer_id': get_offer_id(9),
            },
            {
                'feed_id': FEED_ID,
                'offer_id': get_offer_id(10),
            },
        ],
        allow_berubonus=True,
        allow_promocode=True,
    ),
    offers_matching_rules=[
        OffersMatchingRules(
            feed_offer_ids=[
                [FEED_ID, get_offer_id(7)],
                [FEED_ID, get_offer_id(8)],
                [FEED_ID, get_offer_id(9)],
                [FEED_ID, get_offer_id(10)],
            ]
        )
    ],
)

# У этому офера скидка должна показываться
blue_offer_7 = __blue_offer(price=3000, price_old=4000, offer_id=7, promo=promo6)
msku_7 = __msku(
    [
        blue_offer_7,
    ]
)

# У этому офера скидка не показывается, так как меньше 5%
blue_offer_8 = __blue_offer(price=3000, price_old=3125, offer_id=8, promo=promo6)
msku_8 = __msku(
    [
        blue_offer_8,
    ]
)

# У этому офера скидка не показывается, так как отсутствует price_old
blue_offer_9 = __blue_offer(price=3000, price_old=None, offer_id=9, promo=promo6)
msku_9 = __msku(
    [
        blue_offer_9,
    ]
)

# У этому офера скидка не показывается, так как старая цена меньше новой
blue_offer_10 = __blue_offer(price=3000, price_old=2000, offer_id=10, promo=promo6)
msku_10 = __msku(
    [
        blue_offer_10,
    ]
)


# Действующая акция, в которой для офферов заданы discountPrice, но не заданы oldPrice
# В этом случае: если у оффера есть offerOldPrice и offerOldPrice > discountPrice,
# то применяем промо, иначе отклoняем
promo7 = Promo(
    promo_type=PromoType.DIRECT_DISCOUNT,
    feed_id=FEED_ID,
    key=b64url_md5(next(nummer)),
    url='http://direct_discount_7.com/',
    shop_promo_id='promo7',
    direct_discount=PromoDirectDiscount(
        items=[
            {
                'feed_id': FEED_ID,
                'offer_id': get_offer_id(11),
                'discount_price': {'value': 1340, 'currency': 'RUR'},
            },
            {
                'feed_id': FEED_ID,
                'offer_id': get_offer_id(12),
                'discount_price': {'value': 1450, 'currency': 'RUR'},
            },
            {
                'feed_id': FEED_ID,
                'offer_id': get_offer_id(13),
                'discount_price': {'value': 1450, 'currency': 'RUR'},
            },
            {
                'feed_id': FEED_ID,
                'offer_id': get_offer_id(13001),
                'discount_price': {'value': 1000, 'currency': 'RUR'},
            },
        ],
        allow_berubonus=True,
        allow_promocode=True,
    ),
    offers_matching_rules=[
        OffersMatchingRules(
            feed_offer_ids=[
                [FEED_ID, get_offer_id(11)],
                [FEED_ID, get_offer_id(12)],
                [FEED_ID, get_offer_id(13)],
                [FEED_ID, get_offer_id(13001)],
            ]
        )
    ],
)

blue_offer_11 = __blue_offer(price=1215, price_old=None, offer_id=11, promo=promo7)
msku_11 = __msku(blue_offer_11)
blue_offer_12 = __blue_offer(price=1215, price_old=1300, offer_id=12, promo=promo7)
msku_12 = __msku(blue_offer_12)
blue_offer_13 = __blue_offer(price=1215, price_old=1500, offer_id=13, promo=promo7)
msku_13 = __msku(blue_offer_13)
blue_offer_13001 = __blue_offer(price=1215, price_old=1500, offer_id=13001, promo=promo7)
msku_13001 = __msku(blue_offer_13001)


# Акция, в которой размер скидки меньше, чем 5% и меньше, чем 500 рублей. Акция блокируется по величине скидки
promo8 = Promo(
    promo_type=PromoType.DIRECT_DISCOUNT,
    feed_id=FEED_ID,
    key=b64url_md5(next(nummer)),
    url='http://direct_discount_8.com/',
    shop_promo_id='promo8',
    direct_discount=PromoDirectDiscount(
        items=[
            {
                'feed_id': FEED_ID,
                'offer_id': get_offer_id(14),
                'discount_price': {'value': 956, 'currency': 'RUR'},
                'old_price': {'value': 1000, 'currency': 'RUR'},
            }
        ],
        allow_berubonus=True,
        allow_promocode=True,
    ),
    offers_matching_rules=[
        OffersMatchingRules(
            feed_offer_ids=[
                [FEED_ID, get_offer_id(14)],
            ]
        )
    ],
)

blue_offer_14 = __blue_offer(price=1111, price_old=2000, offer_id=14, promo=promo8)
msku_14 = __msku(blue_offer_14)


# Акция, в которой размер скидки меньше, чем 5%, но больше, чем 500 рублей. Это валидная акция.
promo9 = Promo(
    promo_type=PromoType.DIRECT_DISCOUNT,
    feed_id=FEED_ID,
    key=b64url_md5(next(nummer)),
    url='http://direct_discount_9.com/',
    shop_promo_id='promo9',
    direct_discount=PromoDirectDiscount(
        items=[
            {
                'feed_id': FEED_ID,
                'offer_id': get_offer_id(15),
                'discount_price': {'value': 99499, 'currency': 'RUR'},
                'old_price': {'value': 100000, 'currency': 'RUR'},
            }
        ],
        allow_berubonus=True,
        allow_promocode=True,
    ),
    offers_matching_rules=[
        OffersMatchingRules(
            feed_offer_ids=[
                [FEED_ID, get_offer_id(15)],
            ]
        )
    ],
)

blue_offer_15 = __blue_offer(price=100000, price_old=200000, offer_id=15, promo=promo9)
msku_15 = __msku(blue_offer_15)


# Акция, в которой размер скидки меньше, чем 500 рублей, но больше, чем 5%. Это валидная акция.
promo10 = Promo(
    promo_type=PromoType.DIRECT_DISCOUNT,
    feed_id=FEED_ID,
    key=b64url_md5(next(nummer)),
    url='http://direct_discount_10.com/',
    shop_promo_id='promo10',
    direct_discount=PromoDirectDiscount(
        items=[
            {
                'feed_id': FEED_ID,
                'offer_id': get_offer_id(16),
                'discount_price': {'value': 930, 'currency': 'RUR'},
                'old_price': {'value': 1000, 'currency': 'RUR'},
            }
        ],
        allow_berubonus=True,
        allow_promocode=True,
    ),
    offers_matching_rules=[
        OffersMatchingRules(
            feed_offer_ids=[
                [FEED_ID, get_offer_id(16)],
            ]
        )
    ],
)

blue_offer_16 = __blue_offer(price=1111, price_old=2000, offer_id=16, promo=promo10)
msku_16 = __msku(blue_offer_16)


# Действующая акция DCO_3P_DISCOUNT
promo11 = Promo(
    promo_type=PromoType.DIRECT_DISCOUNT,
    feed_id=FEED_ID,
    key=b64url_md5(next(nummer)),
    url='http://direct_discount_11.com/',
    shop_promo_id='promo11',
    source_type=ESourceType.DCO_3P_DISCOUNT,
    direct_discount=PromoDirectDiscount(
        items=[
            {
                'feed_id': FEED_ID,
                'offer_id': get_offer_id(17),
                'discount_price': {'value': 1400, 'currency': 'RUR'},
                'old_price': {'value': 2050, 'currency': 'RUR'},
                'max_discount': {'value': 800, 'currency': 'RUR'},
                'max_discount_percent': 45.0,
            },
            {
                'feed_id': FEED_ID,
                'offer_id': get_offer_id(117),
                'discount_price': {'value': 1400, 'currency': 'RUR'},
                'old_price': {'value': 2050, 'currency': 'RUR'},
                'max_discount': {'value': 800, 'currency': 'RUR'},
                'max_discount_percent': 45.0,
            },
        ],
        allow_berubonus=True,
        allow_promocode=True,
    ),
    offers_matching_rules=[
        OffersMatchingRules(
            feed_offer_ids=[
                [FEED_ID, get_offer_id(17)],
                [FEED_ID, get_offer_id(117)],
            ]
        )
    ],
)

# Акция, которая добивает до минрефа
promo22 = Promo(
    promo_type=PromoType.DIRECT_DISCOUNT,
    feed_id=222,
    key=b64url_md5(next(nummer)),
    url='http://direct_discount_22.com/',
    shop_promo_id='promo22',
    source_type=ESourceType.DCO_3P_DISCOUNT,
    direct_discount=PromoDirectDiscount(
        items=[
            {
                'feed_id': 222,
                'offer_id': get_offer_id(222),
                'discount_price': {'value': 1400, 'currency': 'RUR'},
                'old_price': {'value': 2050, 'currency': 'RUR'},
                'max_discount': {'value': 800, 'currency': 'RUR'},
                'max_discount_percent': 40.0,
            },
        ],
        allow_berubonus=True,
        allow_promocode=True,
    ),
    offers_matching_rules=[
        OffersMatchingRules(
            feed_offer_ids=[
                [222, get_offer_id(222)],
            ]
        )
    ],
)

# Акция, которая не добивает до минрефа
promo23 = Promo(
    promo_type=PromoType.DIRECT_DISCOUNT,
    feed_id=223,
    key=b64url_md5(next(nummer)),
    url='http://direct_discount_22.com/',
    shop_promo_id='promo23',
    source_type=ESourceType.DCO_3P_DISCOUNT,
    direct_discount=PromoDirectDiscount(
        items=[
            {
                'feed_id': 223,
                'offer_id': get_offer_id(223),
                'discount_price': {'value': 1400, 'currency': 'RUR'},
                'old_price': {'value': 2050, 'currency': 'RUR'},
                'max_discount': {'value': 100, 'currency': 'RUR'},
                'max_discount_percent': 10.0,
            },
        ],
        allow_berubonus=True,
        allow_promocode=True,
    ),
    offers_matching_rules=[
        OffersMatchingRules(
            feed_offer_ids=[
                [223, get_offer_id(223)],
            ]
        )
    ],
)

# Акция, которая не потребуется, т.к. ДЦО сам по себе добивает до минрефа
promo24 = Promo(
    promo_type=PromoType.DIRECT_DISCOUNT,
    feed_id=224,
    key=b64url_md5(next(nummer)),
    url='http://direct_discount_22.com/',
    shop_promo_id='promo24',
    source_type=ESourceType.DCO_3P_DISCOUNT,
    direct_discount=PromoDirectDiscount(
        items=[
            {
                'feed_id': 224,
                'offer_id': get_offer_id(224),
                'discount_price': {'value': 1400, 'currency': 'RUR'},
                'old_price': {'value': 2050, 'currency': 'RUR'},
                'max_discount': {'value': 800, 'currency': 'RUR'},
                'max_discount_percent': 45.0,
            },
        ],
        allow_berubonus=True,
        allow_promocode=True,
    ),
    offers_matching_rules=[
        OffersMatchingRules(
            feed_offer_ids=[
                [224, get_offer_id(224)],
            ]
        )
    ],
)

# Акция, которая добивает до минрефа
promo25 = Promo(
    promo_type=PromoType.DIRECT_DISCOUNT,
    feed_id=225,
    key=b64url_md5(next(nummer)),
    url='http://direct_discount_25.com/',
    shop_promo_id='promo25',
    source_type=ESourceType.DCO_3P_DISCOUNT,
    direct_discount=PromoDirectDiscount(
        items=[
            {
                'feed_id': 225,
                'offer_id': get_offer_id(225),
                'discount_price': {'value': 478, 'currency': 'RUR'},
                'old_price': {'value': 533, 'currency': 'RUR'},
                'max_discount': {'value': 6000, 'currency': 'RUR'},
                'max_discount_percent': 40.0,
            },
        ],
        allow_berubonus=True,
        allow_promocode=True,
    ),
    offers_matching_rules=[
        OffersMatchingRules(
            feed_offer_ids=[
                [225, get_offer_id(225)],
            ]
        )
    ],
)

# Акция, которая добивает до минрефа
promo26 = Promo(
    promo_type=PromoType.DIRECT_DISCOUNT,
    feed_id=226,
    key=b64url_md5(next(nummer)),
    url='http://direct_discount_26.com/',
    shop_promo_id='promo26',
    source_type=ESourceType.DCO_3P_DISCOUNT,
    direct_discount=PromoDirectDiscount(
        items=[
            {
                'feed_id': 226,
                'offer_id': get_offer_id(226),
                'discount_price': {'value': 800, 'currency': 'RUR'},
                'old_price': {'value': 1005, 'currency': 'RUR'},
                'max_discount': {'value': 6000, 'currency': 'RUR'},
                'max_discount_percent': 40.0,
            },
        ],
        allow_berubonus=True,
        allow_promocode=True,
    ),
    offers_matching_rules=[
        OffersMatchingRules(
            feed_offer_ids=[
                [226, get_offer_id(226)],
            ]
        )
    ],
)

promo27 = Promo(
    promo_type=PromoType.DIRECT_DISCOUNT,
    feed_id=227,
    key=b64url_md5(next(nummer)),
    url='http://direct_discount_27.com/',
    shop_promo_id='promo27',
    source_type=ESourceType.DCO_3P_DISCOUNT,
    direct_discount=PromoDirectDiscount(
        items=[
            {
                'feed_id': 227,
                'offer_id': get_offer_id(227),
                'discount_price': {'value': 1000, 'currency': 'RUR'},
                'old_price': {'value': 1005, 'currency': 'RUR'},
                'max_discount': {'value': 6000, 'currency': 'RUR'},
                'max_discount_percent': 40.0,
            },
        ],
        allow_berubonus=True,
        allow_promocode=True,
    ),
    offers_matching_rules=[
        OffersMatchingRules(
            feed_offer_ids=[
                [227, get_offer_id(227)],
            ]
        )
    ],
)

promo28 = Promo(
    promo_type=PromoType.DIRECT_DISCOUNT,
    feed_id=228,
    key=b64url_md5(next(nummer)),
    url='http://direct_discount_28.com/',
    shop_promo_id='promo28',
    source_type=ESourceType.DCO_3P_DISCOUNT,
    direct_discount=PromoDirectDiscount(
        items=[
            {
                'feed_id': 228,
                'offer_id': get_offer_id(228),
                'discount_price': {'value': 1000, 'currency': 'RUR'},
                'old_price': {'value': 1005, 'currency': 'RUR'},
                'max_discount': {'value': 6000, 'currency': 'RUR'},
                'max_discount_percent': 40.0,
            },
        ],
        allow_berubonus=True,
        allow_promocode=True,
    ),
    offers_matching_rules=[
        OffersMatchingRules(
            feed_offer_ids=[
                [228, get_offer_id(228)],
            ]
        )
    ],
)

promo29 = Promo(
    promo_type=PromoType.DIRECT_DISCOUNT,
    feed_id=229,
    key=b64url_md5(next(nummer)),
    url='http://direct_discount_29.com/',
    shop_promo_id='promo29',
    source_type=ESourceType.DCO_3P_DISCOUNT,
    direct_discount=PromoDirectDiscount(
        items=[
            {
                'feed_id': 229,
                'offer_id': get_offer_id(229),
                'discount_price': {'value': 1000, 'currency': 'RUR'},
                'old_price': {'value': 1005, 'currency': 'RUR'},
                'max_discount': {'value': 6000, 'currency': 'RUR'},
                'max_discount_percent': 40.0,
            },
        ],
        allow_berubonus=True,
        allow_promocode=True,
    ),
    offers_matching_rules=[
        OffersMatchingRules(
            feed_offer_ids=[
                [229, get_offer_id(229)],
            ]
        )
    ],
)

promo30 = Promo(
    promo_type=PromoType.DIRECT_DISCOUNT,
    feed_id=230,
    key=b64url_md5(next(nummer)),
    url='http://direct_discount_30.com/',
    shop_promo_id='promo30',
    source_type=ESourceType.DCO_3P_DISCOUNT,
    direct_discount=PromoDirectDiscount(
        items=[
            {
                'feed_id': 230,
                'offer_id': get_offer_id(230),
                'discount_price': {'value': 800, 'currency': 'RUR'},
                'old_price': {'value': 900, 'currency': 'RUR'},
                'max_discount': {'value': 6000, 'currency': 'RUR'},
                'max_discount_percent': 40.0,
            },
        ],
        allow_berubonus=True,
        allow_promocode=True,
    ),
    offers_matching_rules=[
        OffersMatchingRules(
            feed_offer_ids=[
                [230, get_offer_id(230)],
            ]
        )
    ],
)

promo31 = Promo(
    promo_type=PromoType.DIRECT_DISCOUNT,
    feed_id=231,
    key=b64url_md5(next(nummer)),
    url='http://direct_discount_31.com/',
    shop_promo_id='promo31',
    source_type=ESourceType.DCO_3P_DISCOUNT,
    direct_discount=PromoDirectDiscount(
        items=[
            {
                'feed_id': 231,
                'offer_id': get_offer_id(231),
                'discount_price': {'value': 700, 'currency': 'RUR'},
                'old_price': {'value': 900, 'currency': 'RUR'},
                'max_discount': {'value': 6000, 'currency': 'RUR'},
                'max_discount_percent': 40.0,
            },
        ],
        allow_berubonus=True,
        allow_promocode=True,
    ),
    offers_matching_rules=[
        OffersMatchingRules(
            feed_offer_ids=[
                [231, get_offer_id(231)],
            ]
        )
    ],
)

promo32 = Promo(
    promo_type=PromoType.DIRECT_DISCOUNT,
    feed_id=232,
    key=b64url_md5(next(nummer)),
    url='http://direct_discount_32.com/',
    shop_promo_id='promo32',
    source_type=ESourceType.DCO_3P_DISCOUNT,
    direct_discount=PromoDirectDiscount(
        items=[
            {
                'feed_id': 232,
                'offer_id': get_offer_id(232),
                'discount_price': {'value': 700, 'currency': 'RUR'},
                'old_price': {'value': 900, 'currency': 'RUR'},
                'max_discount': {'value': 50, 'currency': 'RUR'},
                'max_discount_percent': 5.0,
            },
        ],
        allow_berubonus=True,
        allow_promocode=True,
    ),
    offers_matching_rules=[
        OffersMatchingRules(
            feed_offer_ids=[
                [232, get_offer_id(232)],
            ]
        )
    ],
)

promo33 = Promo(
    promo_type=PromoType.DIRECT_DISCOUNT,
    feed_id=233,
    key=b64url_md5(next(nummer)),
    url='http://direct_discount_33.com/',
    shop_promo_id='promo33',
    source_type=ESourceType.DCO_3P_DISCOUNT,
    direct_discount=PromoDirectDiscount(
        items=[
            {
                'feed_id': 233,
                'offer_id': get_offer_id(233),
                'discount_price': {'value': 600, 'currency': 'RUR'},
                'old_price': {'value': 900, 'currency': 'RUR'},
                'max_discount': {'value': 6000, 'currency': 'RUR'},
                'max_discount_percent': 40.0,
            },
        ],
        allow_berubonus=True,
        allow_promocode=True,
    ),
    offers_matching_rules=[
        OffersMatchingRules(
            feed_offer_ids=[
                [233, get_offer_id(233)],
            ]
        )
    ],
)

promo34 = Promo(
    promo_type=PromoType.DIRECT_DISCOUNT,
    feed_id=234,
    key=b64url_md5(next(nummer)),
    url='http://direct_discount_34.com/',
    shop_promo_id='promo34',
    source_type=ESourceType.DCO_3P_DISCOUNT,
    direct_discount=PromoDirectDiscount(
        items=[
            {
                'feed_id': 234,
                'offer_id': get_offer_id(234),
                'discount_price': {'value': 600, 'currency': 'RUR'},
                'old_price': {'value': 900, 'currency': 'RUR'},
                'max_discount': {'value': 50, 'currency': 'RUR'},
                'max_discount_percent': 5.0,
            },
        ],
        allow_berubonus=True,
        allow_promocode=True,
    ),
    offers_matching_rules=[
        OffersMatchingRules(
            feed_offer_ids=[
                [234, get_offer_id(234)],
            ]
        )
    ],
)

promo35 = Promo(
    promo_type=PromoType.DIRECT_DISCOUNT,
    feed_id=235,
    key=b64url_md5(next(nummer)),
    url='http://direct_discount_35.com/',
    shop_promo_id='promo35',
    source_type=ESourceType.DCO_3P_DISCOUNT,
    direct_discount=PromoDirectDiscount(
        items=[
            {
                'feed_id': 235,
                'offer_id': get_offer_id(235),
                'discount_price': {'value': 600, 'currency': 'RUR'},
                'old_price': {'value': 1005, 'currency': 'RUR'},
                'max_discount': {'value': 6000, 'currency': 'RUR'},
                'max_discount_percent': 40.0,
            },
        ],
        allow_berubonus=True,
        allow_promocode=True,
    ),
    offers_matching_rules=[
        OffersMatchingRules(
            feed_offer_ids=[
                [235, get_offer_id(235)],
            ]
        )
    ],
)

promo36 = Promo(
    promo_type=PromoType.DIRECT_DISCOUNT,
    feed_id=236,
    key=b64url_md5(next(nummer)),
    url='http://direct_discount_36.com/',
    shop_promo_id='promo36',
    source_type=ESourceType.DCO_3P_DISCOUNT,
    direct_discount=PromoDirectDiscount(
        items=[
            {
                'feed_id': 236,
                'offer_id': get_offer_id(236),
                'discount_price': {'value': 600, 'currency': 'RUR'},
                'old_price': {'value': 1005, 'currency': 'RUR'},
                'max_discount': {'value': 50, 'currency': 'RUR'},
                'max_discount_percent': 5.0,
            },
        ],
        allow_berubonus=True,
        allow_promocode=True,
    ),
    offers_matching_rules=[
        OffersMatchingRules(
            feed_offer_ids=[
                [236, get_offer_id(236)],
            ]
        )
    ],
)

promo37 = Promo(
    promo_type=PromoType.DIRECT_DISCOUNT,
    feed_id=237,
    key=b64url_md5(next(nummer)),
    url='http://direct_discount_37.com/',
    shop_promo_id='promo37',
    source_type=ESourceType.DCO_3P_DISCOUNT,
    direct_discount=PromoDirectDiscount(
        items=[
            {
                'feed_id': 237,
                'offer_id': get_offer_id(237),
                'discount_price': {'value': 700, 'currency': 'RUR'},
                'old_price': {'value': 1005, 'currency': 'RUR'},
                'max_discount': {'value': 6000, 'currency': 'RUR'},
                'max_discount_percent': 45.0,
            },
        ],
        allow_berubonus=True,
        allow_promocode=True,
    ),
    offers_matching_rules=[
        OffersMatchingRules(
            feed_offer_ids=[
                [237, get_offer_id(237)],
            ]
        )
    ],
)

promo38 = Promo(
    promo_type=PromoType.DIRECT_DISCOUNT,
    feed_id=238,
    key=b64url_md5(next(nummer)),
    url='http://direct_discount_38.com/',
    shop_promo_id='promo38',
    source_type=ESourceType.DCO_3P_DISCOUNT,
    direct_discount=PromoDirectDiscount(
        items=[
            {
                'feed_id': 238,
                'offer_id': get_offer_id(238),
                'discount_price': {'value': 700, 'currency': 'RUR'},
                'old_price': {'value': 900, 'currency': 'RUR'},
                'max_discount': {'value': 50, 'currency': 'RUR'},
                'max_discount_percent': 5.0,
            },
        ],
        allow_berubonus=True,
        allow_promocode=True,
    ),
    offers_matching_rules=[
        OffersMatchingRules(
            feed_offer_ids=[
                [238, get_offer_id(238)],
            ]
        )
    ],
)

promo39 = Promo(
    promo_type=PromoType.DIRECT_DISCOUNT,
    feed_id=239,
    key=b64url_md5(next(nummer)),
    url='http://direct_discount_39.com/',
    shop_promo_id='promo39',
    source_type=ESourceType.DCO_3P_DISCOUNT,
    direct_discount=PromoDirectDiscount(
        items=[
            {
                'feed_id': 239,
                'offer_id': get_offer_id(239),
                'discount_price': {'value': 600, 'currency': 'RUR'},
                'old_price': {'value': 1005, 'currency': 'RUR'},
                'max_discount': {'value': 6000, 'currency': 'RUR'},
                'max_discount_percent': 45.0,
            },
        ],
        allow_berubonus=True,
        allow_promocode=True,
    ),
    offers_matching_rules=[
        OffersMatchingRules(
            feed_offer_ids=[
                [239, get_offer_id(239)],
            ]
        )
    ],
)

promo40 = Promo(
    promo_type=PromoType.DIRECT_DISCOUNT,
    feed_id=240,
    key=b64url_md5(next(nummer)),
    url='http://direct_discount_40.com/',
    shop_promo_id='promo40',
    source_type=ESourceType.DCO_3P_DISCOUNT,
    direct_discount=PromoDirectDiscount(
        items=[
            {
                'feed_id': 240,
                'offer_id': get_offer_id(240),
                'discount_price': {'value': 600, 'currency': 'RUR'},
                'old_price': {'value': 1005, 'currency': 'RUR'},
                'max_discount': {'value': 50, 'currency': 'RUR'},
                'max_discount_percent': 5.0,
            },
        ],
        allow_berubonus=True,
        allow_promocode=True,
    ),
    offers_matching_rules=[
        OffersMatchingRules(
            feed_offer_ids=[
                [240, get_offer_id(240)],
            ]
        )
    ],
)

promo41 = Promo(
    promo_type=PromoType.DIRECT_DISCOUNT,
    feed_id=241,
    key=b64url_md5(next(nummer)),
    url='http://direct_discount_41.com/',
    shop_promo_id='promo41',
    source_type=ESourceType.DCO_3P_DISCOUNT,
    direct_discount=PromoDirectDiscount(
        items=[
            {
                'feed_id': 241,
                'offer_id': get_offer_id(241),
                'discount_price': {'value': 700, 'currency': 'RUR'},
                'old_price': {'value': 1005, 'currency': 'RUR'},
                'max_discount': {'value': 5000, 'currency': 'RUR'},
                'max_discount_percent': 45.0,
            },
        ],
        allow_berubonus=True,
        allow_promocode=True,
    ),
    offers_matching_rules=[
        OffersMatchingRules(
            feed_offer_ids=[
                [241, get_offer_id(241)],
            ]
        )
    ],
)

promo42 = Promo(
    promo_type=PromoType.DIRECT_DISCOUNT,
    feed_id=242,
    key=b64url_md5(next(nummer)),
    url='http://direct_discount_42.com/',
    shop_promo_id='promo42',
    source_type=ESourceType.DCO_3P_DISCOUNT,
    direct_discount=PromoDirectDiscount(
        items=[
            {
                'feed_id': 242,
                'offer_id': get_offer_id(242),
                'discount_price': {'value': 700, 'currency': 'RUR'},
                'old_price': {'value': 1005, 'currency': 'RUR'},
                'max_discount': {'value': 50, 'currency': 'RUR'},
                'max_discount_percent': 5.0,
            },
        ],
        allow_berubonus=True,
        allow_promocode=True,
    ),
    offers_matching_rules=[
        OffersMatchingRules(
            feed_offer_ids=[
                [242, get_offer_id(242)],
            ]
        )
    ],
)

promo43 = Promo(
    promo_type=PromoType.DIRECT_DISCOUNT,
    feed_id=243,
    key=b64url_md5(next(nummer)),
    url='http://direct_discount_43.com/',
    shop_promo_id='promo43',
    source_type=ESourceType.DCO_3P_DISCOUNT,
    direct_discount=PromoDirectDiscount(
        items=[
            {
                'feed_id': 243,
                'offer_id': get_offer_id(243),
                'discount_price': {'value': 800, 'currency': 'RUR'},
                'old_price': {'value': 1005, 'currency': 'RUR'},
                'max_discount': {'value': 50, 'currency': 'RUR'},
                'max_discount_percent': 5.0,
            },
        ],
        allow_berubonus=True,
        allow_promocode=True,
    ),
    offers_matching_rules=[
        OffersMatchingRules(
            feed_offer_ids=[
                [243, get_offer_id(243)],
            ]
        )
    ],
)

promo44 = Promo(
    promo_type=PromoType.DIRECT_DISCOUNT,
    feed_id=244,
    key=b64url_md5(next(nummer)),
    url='http://direct_discount_44.com/',
    shop_promo_id='promo44',
    source_type=ESourceType.DCO_3P_DISCOUNT,
    direct_discount=PromoDirectDiscount(
        items=[
            {
                'feed_id': 244,
                'offer_id': get_offer_id(244),
                'discount_price': {'value': 800, 'currency': 'RUR'},
                'old_price': {'value': 1005, 'currency': 'RUR'},
                'max_discount': {'value': 50, 'currency': 'RUR'},
                'max_discount_percent': 5.0,
            },
        ],
        allow_berubonus=True,
        allow_promocode=True,
    ),
    offers_matching_rules=[
        OffersMatchingRules(
            feed_offer_ids=[
                [244, get_offer_id(244)],
            ]
        )
    ],
)

# Персональное промо DCO_PERSONAL
promo45 = Promo(
    promo_type=PromoType.DIRECT_DISCOUNT,
    feed_id=FEED_ID,
    key=b64url_md5(next(nummer)),
    url='http://direct_discount_45.com/',
    shop_promo_id='promo45',
    source_type=ESourceType.DCO_PERSONAL,
    direct_discount=PromoDirectDiscount(
        items=[
            {
                'feed_id': FEED_ID,
                'offer_id': get_offer_id(245),
                'discount_price': {'value': 1400, 'currency': 'RUR'},
                'old_price': {'value': 2050, 'currency': 'RUR'},
                'max_discount': {'value': 800, 'currency': 'RUR'},
                'max_discount_percent': 45.0,
            },
        ],
        allow_berubonus=True,
        allow_promocode=True,
    ),
    restrictions=PromoRestrictions(
        predicates=[
            {
                'perks': ['perk1', 'perk2', 'perk3', '!perk4', 'yalogin'],
            }
        ]
    ),
    offers_matching_rules=[
        OffersMatchingRules(
            feed_offer_ids=[
                [FEED_ID, get_offer_id(245)],
            ]
        )
    ],
)

blue_offer_45 = __blue_offer(price=2000, price_old=2005, offer_id=245, promo=promo45)
msku_45 = __msku(blue_offer_45)

# Если у оффера и у промо отсутствует oldPrice,
# при этом discount_price < price (офферная),
# то промо применяется
promo46 = Promo(
    promo_type=PromoType.DIRECT_DISCOUNT,
    feed_id=FEED_ID,
    key=b64url_md5(next(nummer)),
    url='http://direct_discount_46.com/',
    shop_promo_id='promo46',
    direct_discount=PromoDirectDiscount(
        items=[
            {
                'feed_id': FEED_ID,
                'offer_id': get_offer_id(46),
                'discount_price': {'value': 1000, 'currency': 'RUR'},
            },
        ],
        allow_berubonus=True,
        allow_promocode=True,
    ),
    offers_matching_rules=[
        OffersMatchingRules(
            feed_offer_ids=[
                [FEED_ID, get_offer_id(46)],
            ]
        )
    ],
)

blue_offer_46 = __blue_offer(price=1500, price_old=None, offer_id=46, promo=promo46)
msku_46 = __msku(blue_offer_46)

# Если у оффера и у промо отсутствует oldPrice,
# при этом discount_price > price (офферная),
# то промо НЕ применяется
promo47 = Promo(
    promo_type=PromoType.DIRECT_DISCOUNT,
    feed_id=FEED_ID,
    key=b64url_md5(next(nummer)),
    url='http://direct_discount_47.com/',
    shop_promo_id='promo47',
    direct_discount=PromoDirectDiscount(
        items=[
            {
                'feed_id': FEED_ID,
                'offer_id': get_offer_id(47),
                'discount_price': {'value': 1500, 'currency': 'RUR'},
            },
        ],
        allow_berubonus=True,
        allow_promocode=True,
    ),
    offers_matching_rules=[
        OffersMatchingRules(
            feed_offer_ids=[
                [FEED_ID, get_offer_id(47)],
            ]
        )
    ],
)

blue_offer_47 = __blue_offer(price=1000, price_old=None, offer_id=47, promo=promo47)
msku_47 = __msku(blue_offer_47)

# Персональное промо DCO_PERSONAL
# и два оффера для тестирования выдачи на
# разделение скидки на базовую и персональную часть
promo48 = Promo(
    promo_type=PromoType.DIRECT_DISCOUNT,
    feed_id=FEED_ID,
    key=b64url_md5(next(nummer)),
    url='http://direct_discount_48.com/',
    shop_promo_id='promo48',
    source_type=ESourceType.DCO_PERSONAL,
    direct_discount=PromoDirectDiscount(
        items=[
            {
                'feed_id': FEED_ID,
                'offer_id': get_offer_id(248),
                'discount_price': {'value': 375, 'currency': 'RUR'},
            },
            {
                'feed_id': FEED_ID,
                'offer_id': get_offer_id(249),
                'discount_price': {'value': 375, 'currency': 'RUR'},
            },
        ],
        allow_berubonus=True,
        allow_promocode=True,
    ),
    offers_matching_rules=[
        OffersMatchingRules(
            feed_offer_ids=[
                [FEED_ID, get_offer_id(248)],
                [FEED_ID, get_offer_id(249)],
            ]
        )
    ],
)

blue_offer_48 = __blue_offer(price=400, price_old=500, offer_id=248, promo=promo48)
msku_48 = __msku(blue_offer_48)
blue_offer_49 = __blue_offer(price=500, price_old=None, offer_id=248, promo=promo48)
msku_49 = __msku(blue_offer_49)

# Прямая скидка
# только с процентом скидки
promo50 = Promo(
    promo_type=PromoType.DIRECT_DISCOUNT,
    feed_id=FEED_ID,
    key=b64url_md5(next(nummer)),
    url='http://direct_discount_50.com/',
    shop_promo_id='promo50',
    direct_discount=PromoDirectDiscount(
        items=[
            {
                'feed_id': FEED_ID,
                'offer_id': get_offer_id(250),
                'discount_percent': 7,
            },
            {
                'feed_id': FEED_ID,
                'offer_id': get_offer_id(251),
                'discount_percent': 4,
            },
            {
                'feed_id': FEED_ID,
                'offer_id': get_offer_id(252),
                'discount_percent': 4,
            },
            {
                'feed_id': FEED_ID,
                'offer_id': get_offer_id(253),
                'discount_percent': 3,
            },
        ],
        allow_berubonus=True,
        allow_promocode=True,
    ),
    offers_matching_rules=[
        OffersMatchingRules(
            feed_offer_ids=[
                [FEED_ID, get_offer_id(250)],
                [FEED_ID, get_offer_id(251)],
                [FEED_ID, get_offer_id(252)],
                [FEED_ID, get_offer_id(253)],
            ]
        )
    ],
)

blue_offer_50 = __blue_offer(price=500, price_old=None, offer_id=250, promo=promo50)
msku_50 = __msku(blue_offer_50)

blue_offer_51 = __blue_offer(price=500, price_old=None, offer_id=251, promo=promo50)
msku_51 = __msku(blue_offer_51)

blue_offer_52 = __blue_offer(price=20000, price_old=None, offer_id=252, promo=promo50)
msku_52 = __msku(blue_offer_52)

blue_offer_53 = __blue_offer(price=300, price_old=500, offer_id=253, promo=promo50)
msku_53 = __msku(blue_offer_53)


# Прямая скидка
# с неверной ценой
promo55 = Promo(
    promo_type=PromoType.DIRECT_DISCOUNT,
    feed_id=FEED_ID,
    key=b64url_md5(next(nummer)),
    url='http://direct_discount_55.com/',
    shop_promo_id='promo55',
    source_type=ESourceType.DCO_PERSONAL,
    direct_discount=PromoDirectDiscount(
        items=[
            {
                'feed_id': FEED_ID,
                'offer_id': get_offer_id(255),
                'discount_price': {'value': 10, 'currency': 'RUR'},
            },
            {
                'feed_id': FEED_ID,
                'offer_id': get_offer_id(256),
                'discount_price': {'value': 300, 'currency': 'RUR'},
            },
            {'feed_id': FEED_ID, 'offer_id': get_offer_id(257), 'discount_percent': 98},
            {'feed_id': FEED_ID, 'offer_id': get_offer_id(258), 'discount_percent': 85},
        ],
        allow_berubonus=True,
        allow_promocode=True,
    ),
    offers_matching_rules=[
        OffersMatchingRules(
            feed_offer_ids=[
                [FEED_ID, get_offer_id(255)],
                [FEED_ID, get_offer_id(256)],
                [FEED_ID, get_offer_id(257)],
                [FEED_ID, get_offer_id(258)],
            ]
        )
    ],
)

blue_offer_55 = __blue_offer(price=2000, price_old=None, offer_id=255, promo=promo55)
msku_55 = __msku(blue_offer_55)
blue_offer_56 = __blue_offer(price=2000, price_old=None, offer_id=256, promo=promo55, purchase_price=500)
msku_56 = __msku(blue_offer_56)
blue_offer_57 = __blue_offer(price=2000, price_old=None, offer_id=257, promo=promo55)
msku_57 = __msku(blue_offer_57)
blue_offer_58 = __blue_offer(price=2000, price_old=None, offer_id=258, promo=promo55, purchase_price=500)
msku_58 = __msku(blue_offer_58)

# Персональное промо DCO_PERSONAL
# не должно перебивать более низкую цену
promo59 = Promo(
    promo_type=PromoType.DIRECT_DISCOUNT,
    feed_id=FEED_ID,
    key=b64url_md5(next(nummer)),
    url='http://direct_discount_59.com/',
    shop_promo_id='promo59',
    source_type=ESourceType.DCO_PERSONAL,
    direct_discount=PromoDirectDiscount(
        items=[
            {
                'feed_id': FEED_ID,
                'offer_id': get_offer_id(259),
                'discount_price': {'value': 37, 'currency': 'RUR'},
            }
        ],
        allow_berubonus=True,
        allow_promocode=True,
    ),
    offers_matching_rules=[
        OffersMatchingRules(
            feed_offer_ids=[
                [FEED_ID, get_offer_id(259)],
            ]
        )
    ],
)

blue_offer_59 = __blue_offer(price=12, price_old=40, offer_id=259, promo=promo59)
msku_59 = __msku(blue_offer_59)

blue_offer_17 = __blue_offer(price=2000, price_old=2005, offer_id=17, promo=promo11)
msku_17 = __msku(blue_offer_17)

blue_offer_117 = __blue_offer(price=2000, price_old=2005, offer_id=117, promo=promo11, is_fulfillment=False)
msku_117 = __msku(blue_offer_117)

blue_offer_222 = __blue_offer(
    price=2000, price_old=2005, offer_id=222, feed_id=222, fesh=222, promo=promo22, ref_min_price=1400
)
msku_222 = __msku(blue_offer_222)

blue_offer_223 = __blue_offer(
    price=2000, price_old=2005, offer_id=223, feed_id=223, fesh=223, promo=promo23, ref_min_price=1400
)
msku_223 = __msku(blue_offer_223)

blue_offer_224 = __blue_offer(
    price=2000, price_old=2005, offer_id=224, feed_id=224, fesh=224, promo=promo24, ref_min_price=1400
)
msku_224 = __msku(blue_offer_224)

blue_offer_225 = __blue_offer(
    price=533, price_old=533, offer_id=225, feed_id=225, fesh=225, promo=promo25, ref_min_price=582
)
msku_225 = __msku(blue_offer_225)

blue_offer_226 = __blue_offer(
    price=1000, price_old=1005, offer_id=226, feed_id=226, fesh=226, promo=promo26, ref_min_price=600
)
msku_226 = __msku(blue_offer_226)

blue_offer_227 = __blue_offer(
    price=800, price_old=1005, offer_id=227, feed_id=227, fesh=227, promo=promo27, ref_min_price=700
)
msku_227 = __msku(blue_offer_227)

blue_offer_228 = __blue_offer(
    price=800, price_old=1005, offer_id=228, feed_id=228, fesh=228, promo=promo28, ref_min_price=600
)
msku_228 = __msku(blue_offer_228)

blue_offer_229 = __blue_offer(
    price=700, price_old=1005, offer_id=229, feed_id=229, fesh=229, promo=promo29, ref_min_price=800
)
msku_229 = __msku(blue_offer_229)

blue_offer_230 = __blue_offer(
    price=700, price_old=900, offer_id=230, feed_id=230, fesh=230, promo=promo30, ref_min_price=1000
)
msku_230 = __msku(blue_offer_230)

blue_offer_231 = __blue_offer(
    price=800, price_old=900, offer_id=231, feed_id=231, fesh=231, promo=promo31, ref_min_price=1000
)
msku_231 = __msku(blue_offer_231)

blue_offer_232 = __blue_offer(
    price=800, price_old=900, offer_id=232, feed_id=232, fesh=232, promo=promo32, ref_min_price=1000
)
msku_232 = __msku(blue_offer_232)

blue_offer_233 = __blue_offer(
    price=800, price_old=900, offer_id=233, feed_id=233, fesh=233, promo=promo33, ref_min_price=1000
)
msku_233 = __msku(blue_offer_233)

blue_offer_234 = __blue_offer(
    price=800, price_old=900, offer_id=234, feed_id=234, fesh=234, promo=promo34, ref_min_price=1000
)
msku_234 = __msku(blue_offer_234)

blue_offer_235 = __blue_offer(
    price=1000, price_old=1005, offer_id=235, feed_id=235, fesh=235, promo=promo35, ref_min_price=800
)
msku_235 = __msku(blue_offer_235)

blue_offer_236 = __blue_offer(
    price=1000, price_old=1005, offer_id=236, feed_id=236, fesh=236, promo=promo36, ref_min_price=800
)
msku_236 = __msku(blue_offer_236)

blue_offer_237 = __blue_offer(
    price=1000, price_old=1005, offer_id=237, feed_id=237, fesh=237, promo=promo37, ref_min_price=800
)
msku_237 = __msku(blue_offer_237)

blue_offer_238 = __blue_offer(
    price=1000, price_old=900, offer_id=238, feed_id=238, fesh=238, promo=promo38, ref_min_price=800
)
msku_238 = __msku(blue_offer_238)

blue_offer_239 = __blue_offer(
    price=1000, price_old=1005, offer_id=239, feed_id=239, fesh=239, promo=promo39, ref_min_price=700
)
msku_239 = __msku(blue_offer_239)

blue_offer_240 = __blue_offer(
    price=1000, price_old=1005, offer_id=240, feed_id=240, fesh=240, promo=promo40, ref_min_price=700
)
msku_240 = __msku(blue_offer_240)

blue_offer_241 = __blue_offer(
    price=1000, price_old=1005, offer_id=241, feed_id=241, fesh=241, promo=promo41, ref_min_price=600
)
msku_241 = __msku(blue_offer_241)

blue_offer_242 = __blue_offer(
    price=1000, price_old=1005, offer_id=242, feed_id=242, fesh=242, promo=promo42, ref_min_price=600
)
msku_242 = __msku(blue_offer_242)

blue_offer_243 = __blue_offer(
    price=1000, price_old=1005, offer_id=243, feed_id=243, fesh=243, promo=promo43, ref_min_price=600
)
msku_243 = __msku(blue_offer_243)

blue_offer_244 = __blue_offer(
    price=1000, price_old=1005, offer_id=244, feed_id=244, fesh=244, promo=promo44, ref_min_price=700
)
msku_244 = __msku(blue_offer_244)

# Акция DCO_3P_DISCOUNT будет отключена по max_discount
promo12 = Promo(
    promo_type=PromoType.DIRECT_DISCOUNT,
    feed_id=FEED_ID,
    key=b64url_md5(next(nummer)),
    url='http://direct_discount_12.com/',
    shop_promo_id='promo12',
    source_type=ESourceType.DCO_3P_DISCOUNT,
    direct_discount=PromoDirectDiscount(
        items=[
            {
                'feed_id': FEED_ID,
                'offer_id': get_offer_id(18),
                'discount_price': {'value': 1500, 'currency': 'RUR'},
                'old_price': {'value': 2200, 'currency': 'RUR'},
                'max_discount': {'value': 600, 'currency': 'RUR'},
                'max_discount_percent': 45.0,
            }
        ],
        allow_berubonus=True,
        allow_promocode=True,
    ),
    offers_matching_rules=[
        OffersMatchingRules(
            feed_offer_ids=[
                [FEED_ID, get_offer_id(18)],
            ]
        )
    ],
)

blue_offer_18 = __blue_offer(price=2200, price_old=2200, offer_id=18, promo=promo12)
msku_18 = __msku(blue_offer_18)

# Акция DCO_3P_DISCOUNT будет отключена из-за низкой цены оффера
promo13 = Promo(
    promo_type=PromoType.DIRECT_DISCOUNT,
    feed_id=FEED_ID,
    key=b64url_md5(next(nummer)),
    url='http://direct_discount_13.com/',
    shop_promo_id='promo13',
    source_type=ESourceType.DCO_3P_DISCOUNT,
    direct_discount=PromoDirectDiscount(
        items=[
            {
                'feed_id': FEED_ID,
                'offer_id': get_offer_id(19),
                'discount_price': {'value': 1500, 'currency': 'RUR'},
                'old_price': {'value': 2000, 'currency': 'RUR'},
                'max_discount': {'value': 600, 'currency': 'RUR'},
                'max_discount_percent': 45.0,
            }
        ],
        allow_berubonus=True,
        allow_promocode=True,
    ),
    offers_matching_rules=[
        OffersMatchingRules(
            feed_offer_ids=[
                [FEED_ID, get_offer_id(19)],
            ]
        )
    ],
)

blue_offer_19 = __blue_offer(price=1499, price_old=2000, offer_id=19, promo=promo13)
msku_19 = __msku(blue_offer_19)

# Акция DCO_3P_DISCOUNT будет отключена по max_discount_percent
promo14 = Promo(
    promo_type=PromoType.DIRECT_DISCOUNT,
    feed_id=FEED_ID,
    key=b64url_md5(next(nummer)),
    url='http://direct_discount_14.com/',
    shop_promo_id='promo14',
    source_type=ESourceType.DCO_3P_DISCOUNT,
    direct_discount=PromoDirectDiscount(
        items=[
            {
                'feed_id': FEED_ID,
                'offer_id': get_offer_id(20),
                'discount_price': {'value': 1500, 'currency': 'RUR'},
                'old_price': {'value': 2200, 'currency': 'RUR'},
                'max_discount': {'value': 900, 'currency': 'RUR'},
                'max_discount_percent': 15.0,
            }
        ],
        allow_berubonus=True,
        allow_promocode=True,
    ),
    offers_matching_rules=[
        OffersMatchingRules(
            feed_offer_ids=[
                [FEED_ID, get_offer_id(20)],
            ]
        )
    ],
)

blue_offer_20 = __blue_offer(price=2200, price_old=2200, offer_id=20, promo=promo14)
msku_20 = __msku(blue_offer_20)


# Акция DCO_1P_YANDEX_PLUS_DISCOUNT для yandex_plus
promo15 = Promo(
    promo_type=PromoType.DIRECT_DISCOUNT,
    feed_id=FEED_ID,
    key=b64url_md5(next(nummer)),
    url='http://direct_discount_15.com/',
    shop_promo_id='promo15',
    source_type=ESourceType.DCO_1P_YANDEX_PLUS_DISCOUNT,
    direct_discount=PromoDirectDiscount(
        items=[
            {
                'feed_id': FEED_ID,
                'offer_id': get_offer_id(21),
                'discount_price': {'value': 800, 'currency': 'RUR'},
                'old_price': {'value': 1000, 'currency': 'RUR'},
                'max_discount': {'value': 300, 'currency': 'RUR'},
                'max_discount_percent': 30.0,
            }
        ],
    ),
    restrictions=PromoRestrictions(
        predicates=[
            {
                'perks': ['yandex_plus'],
            }
        ]
    ),
    offers_matching_rules=[
        OffersMatchingRules(
            feed_offer_ids=[
                [FEED_ID, get_offer_id(21)],
            ]
        )
    ],
)

blue_offer_21 = __blue_offer(price=1000, price_old=1000, offer_id=21, promo=promo15)
msku_21 = __msku(blue_offer_21)

############
# Акция DCO_3P_DISCOUNT будет отключена из-за высокой цены оффера
promo1013 = Promo(
    promo_type=PromoType.DIRECT_DISCOUNT,
    feed_id=FEED_ID,
    key=b64url_md5(next(nummer)),
    url='http://direct_discount_1013.com/',
    shop_promo_id='promo1013',
    source_type=ESourceType.DCO_3P_DISCOUNT,
    direct_discount=PromoDirectDiscount(
        items=[
            {
                'feed_id': FEED_ID,
                'offer_id': get_offer_id(1013),
                'discount_price': {'value': 1500, 'currency': 'RUR'},
                'old_price': {'value': 2000, 'currency': 'RUR'},
                'max_discount': {'value': 600, 'currency': 'RUR'},
                'max_discount_percent': 45.0,
            }
        ],
        allow_berubonus=True,
        allow_promocode=True,
    ),
    offers_matching_rules=[
        OffersMatchingRules(
            feed_offer_ids=[
                [FEED_ID, get_offer_id(1013)],
            ]
        )
    ],
)

blue_offer_1013 = __blue_offer(price=2001, price_old=2000, offer_id=1013, promo=promo1013)
msku_1013 = __msku(blue_offer_1013)
############

# Действующая акция на две категории с различным процентом скидки
promo16 = Promo(
    promo_type=PromoType.DIRECT_DISCOUNT,
    feed_id=FEED_ID,
    key=b64url_md5(next(nummer)),
    url='http://direct_discount_16.com/',
    shop_promo_id='promo16',
    direct_discount=PromoDirectDiscount(
        discounts_by_category=[
            {
                'category_restriction': {
                    'categories': [
                        HID_3,
                    ]
                },
            },
            {
                'category_restriction': {
                    'categories': [
                        HID_4,
                    ]
                },
            },
        ],
        allow_berubonus=True,
        allow_promocode=True,
    ),
    offers_matching_rules=[
        OffersMatchingRules(categories=[HID_3, HID_4]),
    ],
)

blue_offer_22 = __blue_offer(price=890, price_old=1000, offer_id=22, promo=promo16)
blue_offer_23 = __blue_offer(price=100, price_old=104, offer_id=23, promo=promo16)
blue_offer_24 = __blue_offer(price=890, price_old=None, offer_id=24, promo=promo16)
msku_22 = __msku(
    [
        blue_offer_22,
    ],
    hid=HID_3,
)
msku_23 = __msku(
    [
        blue_offer_23,
    ],
    hid=HID_4,
)
msku_24 = __msku(
    [
        blue_offer_24,
    ],
    hid=HID_3,
)


# Невалидная акция (цена с промо выше, чем цена оффера без промо)
promo17 = Promo(
    promo_type=PromoType.DIRECT_DISCOUNT,
    feed_id=FEED_ID,
    key=b64url_md5(next(nummer)),
    url='http://direct_discount_17.com/',
    shop_promo_id='promo17',
    conditions='ccc_17',
    direct_discount=PromoDirectDiscount(
        items=[
            {
                'feed_id': FEED_ID,
                'offer_id': get_offer_id(25),
                'discount_price': {'value': 1234, 'currency': 'RUR'},
                'old_price': {'value': 12345, 'currency': 'RUR'},
            }
        ],
        allow_berubonus=True,
        allow_promocode=True,
    ),
    offers_matching_rules=[
        OffersMatchingRules(
            feed_offer_ids=[
                [FEED_ID, get_offer_id(25)],
            ]
        )
    ],
)

blue_offer_25 = __blue_offer(price=1230, price_old=2000, offer_id=25, promo=promo17)
msku_25 = __msku(
    [
        blue_offer_25,
    ]
)


# DSBS
FEED_ID_DSBS = 11111
SHOP_ID_DSBS = 111110
DSBS_SKU_1 = 10010
DSBS_SKU_2 = 10011
DSBS_HID = 10011001

dsbs_shop = Shop(
    fesh=SHOP_ID_DSBS,
    datafeed_id=FEED_ID_DSBS,
    priority_region=213,
    name='DSBS shop',
    cpa=Shop.CPA_REAL,
    warehouse_id=35984,
)

promo18 = Promo(
    promo_type=PromoType.DIRECT_DISCOUNT,
    key=b64url_md5(next(nummer)),
    shop_promo_id='promo18',
    direct_discount=PromoDirectDiscount(
        items=[
            {
                'feed_id': FEED_ID_DSBS,
                'offer_id': 'dsbs',
                'discount_price': {'value': 3750, 'currency': 'RUR'},
                'old_price': {'value': 5000, 'currency': 'RUR'},
            }
        ],
        allow_berubonus=True,
        allow_promocode=True,
    ),
    offers_matching_rules=[
        OffersMatchingRules(
            feed_offer_ids=[
                [FEED_ID_DSBS, 'dsbs'],
            ]
        )
    ],
)

# субсидируемое промо
promo19 = Promo(
    promo_type=PromoType.DIRECT_DISCOUNT,
    key=b64url_md5(nummer.next()),
    shop_promo_id='promo19',
    source_type=ESourceType.DCO_3P_DISCOUNT,
    url='http://direct_discount_19.com/',
    direct_discount=PromoDirectDiscount(
        items=[
            {
                'feed_id': FEED_ID_DSBS,
                'offer_id': 'dsbs_subsidy',
                'discount_price': {'value': 3650, 'currency': 'RUR'},
                'old_price': {'value': 5900, 'currency': 'RUR'},
                'max_discount': {'value': 8000, 'currency': 'RUR'},
                'max_discount_percent': 40.0,
            }
        ],
        allow_berubonus=True,
        allow_promocode=True,
    ),
    offers_matching_rules=[
        OffersMatchingRules(
            feed_offer_ids=[
                [FEED_ID_DSBS, 'dsbs_subsidy'],
            ]
        )
    ],
)

dsbs_offer = Offer(
    fesh=dsbs_shop.fesh,
    waremd5=b64url_md5(next(nummer)),
    hyperid=DSBS_SKU_1,
    sku=DSBS_SKU_1,
    price=29990,
    promo=[promo18],
    blue_promo_key=[promo18.shop_promo_id],
    cpa=Offer.CPA_REAL,
    offerid='dsbs',
)

dsbs_subsidy_offer = Offer(
    fesh=dsbs_shop.fesh,
    waremd5=b64url_md5(nummer.next()),
    hyperid=DSBS_SKU_2,
    sku=DSBS_SKU_2,
    price=5900,
    promo=[promo19],
    blue_promo_key=[promo19.shop_promo_id],
    cpa=Offer.CPA_REAL,
    offerid='dsbs_subsidy',
)

msku_dsbs_1 = MarketSku(sku=DSBS_SKU_1, hyperid=DSBS_SKU_1, hid=DSBS_HID)
msku_dsbs_2 = MarketSku(sku=DSBS_SKU_2, hyperid=DSBS_SKU_2, hid=DSBS_HID)


class T(TestCase):
    @classmethod
    def prepare(cls):
        cls.settings.default_search_experiment_flags += ['enable_fast_promo_matcher=1;enable_fast_promo_matcher_test=1']
        cls.settings.rty_qpipe = True

        cls.index.blue_regional_mskus += [
            RegionalMsku(msku_id=msku_222.sku, offers=2, price_min=1400, rids=[213]),
            RegionalMsku(msku_id=msku_225.sku, offers=2, price_min=582, rids=[213]),
            RegionalMsku(msku_id=msku_226.sku, offers=2, price_min=600, rids=[213]),
            RegionalMsku(msku_id=msku_231.sku, offers=2, price_min=1000, rids=[213]),
            RegionalMsku(msku_id=msku_233.sku, offers=2, price_min=1000, rids=[213]),
            RegionalMsku(msku_id=msku_235.sku, offers=2, price_min=800, rids=[213]),
            RegionalMsku(msku_id=msku_237.sku, offers=2, price_min=800, rids=[213]),
            RegionalMsku(msku_id=msku_239.sku, offers=2, price_min=700, rids=[213]),
            RegionalMsku(msku_id=msku_241.sku, offers=2, price_min=600, rids=[213]),
        ]

        cls.index.shops += [
            Shop(
                fesh=FEED_ID,
                datafeed_id=FEED_ID,
                priority_region=213,
                supplier_type=Shop.THIRD_PARTY,
                blue=Shop.BLUE_REAL,
            ),
            Shop(fesh=222, datafeed_id=222, priority_region=213, supplier_type=Shop.THIRD_PARTY, blue=Shop.BLUE_REAL),
            Shop(fesh=223, datafeed_id=223, priority_region=213, supplier_type=Shop.THIRD_PARTY, blue=Shop.BLUE_REAL),
            Shop(fesh=224, datafeed_id=224, priority_region=213, supplier_type=Shop.THIRD_PARTY, blue=Shop.BLUE_REAL),
            Shop(fesh=225, datafeed_id=225, priority_region=213, supplier_type=Shop.THIRD_PARTY, blue=Shop.BLUE_REAL),
            Shop(fesh=226, datafeed_id=226, priority_region=213, supplier_type=Shop.THIRD_PARTY, blue=Shop.BLUE_REAL),
            Shop(fesh=227, datafeed_id=227, priority_region=213, supplier_type=Shop.THIRD_PARTY, blue=Shop.BLUE_REAL),
            Shop(fesh=228, datafeed_id=228, priority_region=213, supplier_type=Shop.THIRD_PARTY, blue=Shop.BLUE_REAL),
            Shop(fesh=229, datafeed_id=229, priority_region=213, supplier_type=Shop.THIRD_PARTY, blue=Shop.BLUE_REAL),
            Shop(fesh=230, datafeed_id=230, priority_region=213, supplier_type=Shop.THIRD_PARTY, blue=Shop.BLUE_REAL),
            Shop(fesh=231, datafeed_id=231, priority_region=213, supplier_type=Shop.THIRD_PARTY, blue=Shop.BLUE_REAL),
            Shop(fesh=232, datafeed_id=232, priority_region=213, supplier_type=Shop.THIRD_PARTY, blue=Shop.BLUE_REAL),
            Shop(fesh=233, datafeed_id=233, priority_region=213, supplier_type=Shop.THIRD_PARTY, blue=Shop.BLUE_REAL),
            Shop(fesh=234, datafeed_id=234, priority_region=213, supplier_type=Shop.THIRD_PARTY, blue=Shop.BLUE_REAL),
            Shop(fesh=235, datafeed_id=235, priority_region=213, supplier_type=Shop.THIRD_PARTY, blue=Shop.BLUE_REAL),
            Shop(fesh=236, datafeed_id=236, priority_region=213, supplier_type=Shop.THIRD_PARTY, blue=Shop.BLUE_REAL),
            Shop(fesh=237, datafeed_id=237, priority_region=213, supplier_type=Shop.THIRD_PARTY, blue=Shop.BLUE_REAL),
            Shop(fesh=238, datafeed_id=238, priority_region=213, supplier_type=Shop.THIRD_PARTY, blue=Shop.BLUE_REAL),
            Shop(fesh=239, datafeed_id=239, priority_region=213, supplier_type=Shop.THIRD_PARTY, blue=Shop.BLUE_REAL),
            Shop(fesh=240, datafeed_id=240, priority_region=213, supplier_type=Shop.THIRD_PARTY, blue=Shop.BLUE_REAL),
            Shop(fesh=241, datafeed_id=241, priority_region=213, supplier_type=Shop.THIRD_PARTY, blue=Shop.BLUE_REAL),
            Shop(fesh=242, datafeed_id=242, priority_region=213, supplier_type=Shop.THIRD_PARTY, blue=Shop.BLUE_REAL),
            Shop(fesh=243, datafeed_id=243, priority_region=213, supplier_type=Shop.THIRD_PARTY, blue=Shop.BLUE_REAL),
            Shop(fesh=244, datafeed_id=244, priority_region=213, supplier_type=Shop.THIRD_PARTY, blue=Shop.BLUE_REAL),
            dsbs_shop,
        ]

        cls.index.offers += [dsbs_offer, dsbs_subsidy_offer]

        cls.index.mskus += [
            msku_gr75,
            msku_1,
            msku_2,
            msku_3,
            msku_4,
            msku_5,
            msku_6,
            msku_7,
            msku_8,
            msku_9,
            msku_10,
            msku_11,
            msku_12,
            msku_13,
            msku_13001,
            msku_14,
            msku_15,
            msku_16,
            msku_17,
            msku_18,
            msku_19,
            msku_20,
            msku_45,
            msku_46,
            msku_47,
            msku_48,
            msku_49,
            msku_50,
            msku_51,
            msku_52,
            msku_53,
            msku_55,
            msku_56,
            msku_57,
            msku_58,
            msku_59,
            msku_117,
            msku_222,
            msku_223,
            msku_224,
            msku_225,
            msku_226,
            msku_227,
            msku_228,
            msku_229,
            msku_230,
            msku_231,
            msku_232,
            msku_233,
            msku_234,
            msku_235,
            msku_236,
            msku_237,
            msku_238,
            msku_239,
            msku_240,
            msku_241,
            msku_242,
            msku_243,
            msku_244,
            msku_21,
            msku_22,
            msku_23,
            msku_24,
            msku_25,
            msku_1013,
            msku_dsbs_1,
            msku_dsbs_2,
        ]

        cls.index.promos += [
            promo_gr75,
            promo1,
            promo2,
            promo3,
            promo4,
            promo6,
            promo7,
            promo8,
            promo9,
            promo10,
            promo11,
            promo12,
            promo13,
            promo14,
            promo22,
            promo23,
            promo24,
            promo25,
            promo26,
            promo27,
            promo28,
            promo29,
            promo30,
            promo31,
            promo32,
            promo33,
            promo34,
            promo35,
            promo36,
            promo37,
            promo38,
            promo39,
            promo40,
            promo41,
            promo42,
            promo43,
            promo44,
            promo45,
            promo46,
            promo47,
            promo48,
            promo50,
            promo55,
            promo59,
            promo15,
            promo16,
            promo17,
            promo18,
            promo19,
            promo1013,
        ]

        # add minRef strategy with maximum promo 30 percents to drop to minRef(set by supplier)
        cls.dynamic.market_dynamic.dynamic_price_control += [
            DynamicPriceControlData(222, 20, 1),
            DynamicPriceControlData(223, 10, 1),
            DynamicPriceControlData(224, 30, 1),
            DynamicPriceControlData(225, 7, 1),
            DynamicPriceControlData(226, 30, 1),
            DynamicPriceControlData(227, 25, 1),
            DynamicPriceControlData(228, 12, 1),
            DynamicPriceControlData(229, 15, 1),
            DynamicPriceControlData(230, 15, 1),
            DynamicPriceControlData(231, 25, 1),
            DynamicPriceControlData(232, 25, 1),
            DynamicPriceControlData(233, 12, 1),
            DynamicPriceControlData(234, 12, 1),
            DynamicPriceControlData(235, 30, 1),
            DynamicPriceControlData(236, 30, 1),
            DynamicPriceControlData(237, 40, 1),
            DynamicPriceControlData(238, 40, 1),
            DynamicPriceControlData(239, 20, 1),
            DynamicPriceControlData(240, 20, 1),
            DynamicPriceControlData(241, 20, 1),
            DynamicPriceControlData(242, 20, 1),
            DynamicPriceControlData(243, 30, 1),
            DynamicPriceControlData(244, 40, 1),
        ]

    def __calc_discount_percent(self, price, oldprice):
        return int(floor((1 - price * 1.0 / oldprice) * 100 + 0.5))

    def check_promo(self, promo, msku, offer, promo_has_oldprice):
        def __get_promo_price(promo, offer, price_type, ignore_discount=True):
            offerid = offer.offerid if isinstance(offer, BlueOffer) else offer.offer_id()
            for item in promo.direct_discount.items:
                if item.get('offer_id') == offerid:
                    if price_type not in item:
                        return None
                    promo_price = item[price_type]['value']
                    if ignore_discount:
                        return promo_price
                    discount = item.get('discount', 0)
                    return int(promo_price * ((100 - discount) / 100.0))
            return None

        def __get_promo_percent(promo, offer):
            offerid = offer.offerid if isinstance(offer, BlueOffer) else offer.offer_id()
            for item in promo.direct_discount.items:
                if item.get('offer_id') == offerid:
                    return item.get('discount_percent')
            return None

        promo_discount_percent = __get_promo_percent(promo, offer)
        promo_discount_price = __get_promo_price(promo, offer, 'discount_price') or offer.price
        promo_discount_price = (
            int(promo_discount_price * (100 - promo_discount_percent) / 100.0)
            if promo_discount_percent
            else promo_discount_price
        )
        promo_old_price = (
            __get_promo_price(promo, offer, 'old_price')
            or offer.price_old
            or (offer.price if offer.price > promo_discount_price else None)
        )
        for place in ('sku_offers', 'prime'):
            for disable_promo_match_rearr in (None, 0, 1):
                request = 'place={place}&rids=0&regset=1&pp=18&market-sku={msku}&rgb=blue'
                rearr_factors = []
                if disable_promo_match_rearr is not None:
                    rearr_factors.append(
                        'market_disable_promo_matching_by_feed_offer_id={}'.format(disable_promo_match_rearr)
                    )
                if promo.source_type == ESourceType.DCO_PERSONAL:
                    rearr_factors.append('personal_promo_direct_discount_enabled=1')
                if len(rearr_factors) > 0:
                    request += '&rearr-factors=' + ';'.join(rearr_factors)
                request = request.format(place=place, msku=msku)
                response = self.report.request_json(request)

                # Проверяем что в выдаче есть оффер с корректным блоком "promo"
                self.assertFragmentIn(
                    response,
                    [
                        {
                            'entity': 'offer',
                            'wareId': offer.waremd5,
                            'prices': {
                                'value': str(promo_discount_price),
                                'currency': 'RUR',
                                'discount': {
                                    'percent': self.__calc_discount_percent(
                                        promo_discount_price or offer.price, promo_old_price
                                    ),
                                    'oldMin': str(promo_old_price),
                                }
                                if promo_old_price
                                else Absent(),
                            },
                            'promos': [
                                {
                                    'type': promo.type_name,
                                    'key': promo.key,
                                    'startDate': NotEmpty() if promo.start_date else Absent(),
                                    'endDate': NotEmpty() if promo.end_date else Absent(),
                                    'itemsInfo': {
                                        'constraints': {
                                            'allow_promocode': promo.direct_discount.allow_promocode,
                                            'allow_berubonus': promo.direct_discount.allow_berubonus,
                                        },
                                        'price': {
                                            'value': str(promo_discount_price),
                                            'currency': 'RUR',
                                            'discount': {
                                                'percent': self.__calc_discount_percent(
                                                    promo_discount_price, promo_old_price
                                                ),
                                                'oldMin': str(promo_old_price),
                                                'absolute': str(promo_old_price - promo_discount_price),
                                            }
                                            if promo_old_price
                                            else Absent(),
                                            'baseDiscount': {
                                                'oldMin': str(promo_old_price),
                                                'percent': self.__calc_discount_percent(offer.price, offer.price_old)
                                                if offer.price_old
                                                else 0,
                                                'absolute': str(offer.price_old - offer.price)
                                                if offer.price_old
                                                else '0',
                                            }
                                            if promo.source_type == ESourceType.DCO_PERSONAL
                                            else Absent(),
                                            'personalDiscount': {
                                                'oldMin': str(promo_old_price),
                                                'percent': self.__calc_discount_percent(
                                                    promo_discount_price, promo_old_price
                                                )
                                                - (
                                                    self.__calc_discount_percent(offer.price, offer.price_old)
                                                    if offer.price_old
                                                    else 0
                                                ),
                                                'absolute': str(offer.price - promo_discount_price),
                                            }
                                            if promo.source_type == ESourceType.DCO_PERSONAL
                                            else Absent(),
                                        },
                                    },
                                }
                            ],
                        }
                    ],
                    allow_different_len=False,
                )

    # Сокращённый вариант проверки ответа репорта для промо, которое задаётся на
    # категории, а не на список конкретных офферов
    def check_promo_for_category(self, promo, msku, offer):
        for rgb in ('blue', 'green', 'green_with_blue'):
            for place in ('sku_offers', 'prime'):
                for disable_promo_match_rearr in (None, 0, 1):
                    request = 'place={place}&rids=0&regset=1&pp=18&market-sku={msku}&rgb={rgb}'
                    if disable_promo_match_rearr is not None:
                        request += '&rearr-factors=market_disable_promo_matching_by_feed_offer_id={}'.format(
                            disable_promo_match_rearr
                        )
                    request = request.format(place=place, msku=msku, rgb=rgb)
                    response = self.report.request_json(request)

                    # Проверяем что в выдаче есть оффер с корректным блоком "promos"
                    self.assertFragmentIn(
                        response,
                        [
                            {
                                'entity': 'offer',
                                'wareId': offer.waremd5,
                                'prices': {
                                    'value': str(offer.price),
                                    'currency': 'RUR',
                                },
                                'promos': [
                                    {
                                        'type': promo.type_name,
                                        'key': promo.key,
                                        'startDate': NotEmpty() if promo.start_date else Absent(),
                                        'endDate': NotEmpty() if promo.end_date else Absent(),
                                        'itemsInfo': {
                                            'constraints': {
                                                'allow_promocode': promo.direct_discount.allow_promocode,
                                                'allow_berubonus': promo.direct_discount.allow_berubonus,
                                            },
                                            'price': {
                                                'value': str(offer.price),  # повторяем цену оффера
                                                'currency': 'RUR',
                                                'discount': {
                                                    'percent': self.__calc_discount_percent(
                                                        offer.price, offer.price_old
                                                    ),
                                                    'oldMin': str(offer.price_old),
                                                    'absolute': str(offer.price_old - offer.price),
                                                }
                                                if offer.price_old > 0
                                                else Absent(),
                                            },
                                        },
                                    }
                                ],
                            }
                        ],
                        allow_different_len=False,
                    )

    def check_promo_absent(self, msku, waremd5):
        for rgb in ('blue', 'green', 'green_with_blue'):
            for place in ('sku_offers', 'prime'):
                for disable_promo_match_rearr in (None, 0, 1):
                    for enable_offline_buybox_price in (0, 1):
                        request = 'place={place}&rids=0&regset=1&pp=18&market-sku={msku}&rgb={rgb}&yandexuid=1'
                        if disable_promo_match_rearr is not None:
                            request += '&rearr-factors=market_disable_promo_matching_by_feed_offer_id={};enable_offline_buybox_price={}'.format(
                                disable_promo_match_rearr, enable_offline_buybox_price
                            )
                        request = request.format(place=place, msku=msku, rgb=rgb)
                        response = self.report.request_json(request)
                        # Блок промо должен отсутстовать
                        self.assertFragmentIn(
                            response,
                            [
                                {
                                    'entity': 'offer',
                                    'wareId': waremd5,
                                    'promos': Absent(),
                                }
                            ],
                        )

    def test_direct_discount_active(self):
        ACTIVE_PROMO_DATA = (
            (promo1, msku_1, blue_offer_1, True, False, True),
            (promo5, msku_5, blue_offer_5, False, True, True),
            (promo6, msku_7, blue_offer_7, False, False, True),
            (promo7, msku_13001, blue_offer_13001, True, False, False),
            (promo9, msku_15, blue_offer_15, True, False, True),
            (promo10, msku_16, blue_offer_16, True, False, True),
            (promo11, msku_17, blue_offer_17, True, False, True),
            (promo11, msku_117, blue_offer_117, True, False, True),
            (promo16, msku_22, blue_offer_22, False, False, True),
            (promo46, msku_46, blue_offer_46, True, False, False),
            (promo48, msku_48, blue_offer_48, True, False, True),
            (promo48, msku_49, blue_offer_49, True, False, False),
            (promo50, msku_50, blue_offer_50, True, False, False),
            (promo50, msku_52, blue_offer_52, True, False, False),
            (promo50, msku_53, blue_offer_53, True, False, False),
            (
                promo_gr75,
                msku_gr75,
                blue_offer_gr75,
                True,
                False,
                True,
            ),  # Проверка, что прямая скидка позволяет ставить скидку > 75%
        )

        for (
            promo,
            msku,
            blue_offer,
            promo_has_items_with_price,
            promo_has_category_discount,
            promo_has_oldprice,
        ) in ACTIVE_PROMO_DATA:
            if promo_has_category_discount:
                self.check_promo_for_category(promo, msku.sku, blue_offer)
            else:
                self.check_promo(promo, msku.sku, blue_offer, promo_has_oldprice)

    def test_direct_discount_inactive(self):
        # Проверяем отключение акций по времени
        self.check_promo_absent(msku_2.sku, blue_offer_2.waremd5)
        self.check_promo_absent(msku_3.sku, blue_offer_3.waremd5)
        # Проверяем отключение акций по значению процента скидки на категорию
        self.check_promo_absent(msku_6.sku, blue_offer_6.waremd5)
        # Заводим акцию DirectDiscount на оффер, при этом не указываем в промо oldPrice и discountPrice.
        # Проверяем, что если у оффера нет oldPrice, либо price >= oldPrice, то промо не применится к нему.
        self.check_promo_absent(msku_8.sku, blue_offer_8.waremd5)
        self.check_promo_absent(msku_9.sku, blue_offer_9.waremd5)
        self.check_promo_absent(msku_10.sku, blue_offer_10.waremd5)
        # Заводим акцию DirectDiscount на оффер, при этом указываем в промо discountPrice, но не указываем oldPrice.
        # У самого оффера oldPrice также отсутствует. Проверяем, что промо не применится к офферу, потому что скидочная цена выше цены оффера
        self.check_promo_absent(msku_11.sku, blue_offer_11.waremd5)
        # Заводим акцию DirectDiscount на оффер, при этом указываем в промо discountPrice, но не указфваем oldPrice.
        # У самого оффера offerOldPrice есть, но цена со скидкой из промо (discountPrice) больше, чем
        # старая цена оффера (offerOldPrice). Проверяем, что промо не применится к офферу
        self.check_promo_absent(msku_12.sku, blue_offer_12.waremd5)
        # Заводим акцию DirectDiscount на оффер, при этом указываем в промо discountPrice=956 и oldPrice=1000.
        # Скидка по акции получается меньше, чем 5% и меньше, чем 500 рублей. Промо блокируется по размеру скидки
        self.check_promo_absent(msku_14.sku, blue_offer_14.waremd5)
        # Акция DCO_3P_DISCOUNT будет отключена по max_discount
        self.check_promo_absent(msku_18.sku, blue_offer_18.waremd5)
        # Акция DCO_3P_DISCOUNT будет отключена из-за низкй цены оффера
        self.check_promo_absent(msku_19.sku, blue_offer_19.waremd5)
        # Акция DCO_3P_DISCOUNT будет отключена по max_discount_percent
        self.check_promo_absent(msku_20.sku, blue_offer_20.waremd5)
        # Акция DCO_3P_DISCOUNT будет отключена так как по ДЦО и промо совместно не достигается минреф
        self.check_promo_absent(msku_223.sku, blue_offer_223.waremd5)
        # Акция DCO_3P_DISCOUNT будет отключена так как по стартегии минрефа уже достигнут минреф
        self.check_promo_absent(msku_224.sku, blue_offer_224.waremd5)
        # Акция DCO_1P_DISCOUNT будет отключена при отсуствии перки
        self.check_promo_absent(msku_21.sku, blue_offer_21.waremd5)
        # Акция задана на категорию, но скидка меньше 5%
        self.check_promo_absent(msku_23.sku, blue_offer_23.waremd5)
        # Акция задана на категорию, но у оффера отсутствует old_price
        self.check_promo_absent(msku_24.sku, blue_offer_24.waremd5)
        # Акция блокируется на репорте, так как цена по акции выше, чем цена оффера без акции
        self.check_promo_absent(msku_13.sku, blue_offer_13.waremd5)
        self.check_promo_absent(msku_25.sku, blue_offer_25.waremd5)
        # Акция будет отключена, т.к. price > old_price
        self.check_promo_absent(msku_1013.sku, blue_offer_1013.waremd5)
        self.check_promo_absent(msku_227.sku, blue_offer_227.waremd5)
        self.check_promo_absent(msku_228.sku, blue_offer_228.waremd5)
        self.check_promo_absent(msku_229.sku, blue_offer_229.waremd5)
        self.check_promo_absent(msku_230.sku, blue_offer_230.waremd5)
        self.check_promo_absent(msku_232.sku, blue_offer_232.waremd5)
        self.check_promo_absent(msku_234.sku, blue_offer_234.waremd5)
        self.check_promo_absent(msku_236.sku, blue_offer_236.waremd5)
        self.check_promo_absent(msku_238.sku, blue_offer_238.waremd5)
        self.check_promo_absent(msku_240.sku, blue_offer_240.waremd5)
        self.check_promo_absent(msku_242.sku, blue_offer_242.waremd5)
        self.check_promo_absent(msku_243.sku, blue_offer_243.waremd5)
        self.check_promo_absent(msku_244.sku, blue_offer_244.waremd5)
        self.check_promo_absent(msku_47.sku, blue_offer_47.waremd5)
        # Акция будет отключена, т.к. discount_percent < 5 и в абсолюте скидка < 500
        self.check_promo_absent(msku_51.sku, blue_offer_51.waremd5)
        # Акция будет отключена, т.к. скидка более 95%
        self.check_promo_absent(msku_55.sku, blue_offer_55.waremd5)
        self.check_promo_absent(msku_57.sku, blue_offer_57.waremd5)
        # Акция будет отключена, т.к. скидочная цена ниже закупочной - 33%
        self.check_promo_absent(msku_56.sku, blue_offer_56.waremd5)
        self.check_promo_absent(msku_58.sku, blue_offer_58.waremd5)
        self.check_promo_absent(msku_59.sku, blue_offer_59.waremd5)

    def test_direct_discount_in_buybox(self):
        # blue_offer_4 дороже чем blue_offer_4а, но с учётом промо - дешевле, и он дожен выиграть
        self.check_promo(promo4, msku_4.sku, blue_offer_4, True)

    def test_direct_discount_partial_subsidy(self):
        # проверяем market_sku_222, тут должно быть частичное субсидирование 200
        # проверяем market_sku_225, тут должно быть полное субсидирование 55, минреф выше начальной цены
        mskusForCheck = [msku_222, msku_225, msku_231, msku_233, msku_235, msku_237, msku_239, msku_241]
        offersForCheck = [
            blue_offer_222,
            blue_offer_225,
            blue_offer_231,
            blue_offer_233,
            blue_offer_235,
            blue_offer_237,
            blue_offer_239,
            blue_offer_241,
        ]

        # make it as same as in cls.dynamic.market_dynamic.dynamic_price_control for these offers
        percentsForBuyboxStrategy = [20, 7, 25, 12, 30, 40, 20, 20]

        answersForCheck = [
            {
                'value': str(1400),
                'currency': 'RUR',
                'discount': {
                    'oldMin': str(2050),
                    'absolute': str(650),
                },
                'subsidy': {
                    'oldMin': str(2000),
                    'absolute': str(200),
                },
            },
            {
                'value': str(478),
                'currency': 'RUR',
                'discount': {
                    'oldMin': str(533),
                    'absolute': str(55),
                },
                'subsidy': {
                    'oldMin': str(533),
                    'absolute': str(55),
                },
            },
            {
                'value': str(700),
                'currency': 'RUR',
                'discount': {
                    'oldMin': str(900),
                    'absolute': str(200),
                },
                'subsidy': {
                    'oldMin': str(800),
                    'absolute': str(100),
                },
            },
            {
                'value': str(600),
                'currency': 'RUR',
                'discount': {
                    'oldMin': str(900),
                    'absolute': str(300),
                },
                'subsidy': {
                    'oldMin': str(800),
                    'absolute': str(200),
                },
            },
            {
                'value': str(600),
                'currency': 'RUR',
                'discount': {
                    'oldMin': str(1005),
                    'absolute': str(405),
                },
                'subsidy': {
                    'oldMin': str(1000),
                    'absolute': str(200),
                },
            },
            {
                'value': str(700),
                'currency': 'RUR',
                'discount': {
                    'oldMin': str(1005),
                    'absolute': str(305),
                },
                'subsidy': {
                    'oldMin': str(1000),
                    'absolute': str(100),
                },
            },
            {
                'value': str(600),
                'currency': 'RUR',
                'discount': {
                    'oldMin': str(1005),
                    'absolute': str(405),
                },
                'subsidy': {
                    'oldMin': str(1000),
                    'absolute': str(200),
                },
            },
            {
                'value': str(700),
                'currency': 'RUR',
                'discount': {
                    'oldMin': str(1005),
                    'absolute': str(305),
                },
                'subsidy': {
                    'oldMin': str(1000),
                    'absolute': str(100),
                },
            },
        ]

        for mskuForCheck, offerForCheck, answer, percent in zip(
            mskusForCheck, offersForCheck, answersForCheck, percentsForBuyboxStrategy
        ):
            for rgb in ('blue', 'green', 'green_with_blue'):
                for place in ('sku_offers', 'prime'):
                    for disable_promo_match_rearr in (None, 0, 1):
                        request = 'place={place}&rids=0&regset=1&pp=18&market-sku={msku}&rgb={rgb}'
                        if disable_promo_match_rearr is not None:
                            request += '&rearr-factors=market_disable_promo_matching_by_feed_offer_id={}'.format(
                                disable_promo_match_rearr
                            )
                        request = request.format(place=place, msku=mskuForCheck.sku, rgb=rgb)
                        response = self.report.request_json(request)

                        # Проверяем что в выдаче есть оффер с корректным блоком "promo"
                        self.assertFragmentIn(
                            response,
                            [
                                {
                                    'promos': [
                                        {
                                            'itemsInfo': {
                                                "has_dco_3p_subsidy": True,
                                                'price': answer,
                                            },
                                        }
                                    ],
                                }
                            ],
                            allow_different_len=False,
                        )

            # Проверка place=offerinfo без флага rgb=blue
            request = 'place=offerinfo&rids=0&regset=1&pp=18&offerid={waremd5}'.format(waremd5=offerForCheck.waremd5)
            response = self.report.request_json(request)
            self.assertFragmentIn(
                response,
                [
                    {
                        'promos': [
                            {
                                'itemsInfo': {"has_dco_3p_subsidy": True, 'price': answer},
                            }
                        ],
                    }
                ],
                allow_different_len=False,
            )

            self.rty.offers += [
                RtyOffer(
                    feedid=offerForCheck.feedid,
                    offerid=offerForCheck.offerid,
                    dynamic_pricing=DynamicPricingStrategySSKU(
                        dynamic_pricing_type=DYNAMIC_PRICING_TYPE.BUYBOX,
                        dynamic_pricing_threshold_is_percent=1,
                        dynamic_pricing_threshold_value=percent,
                    ),
                ),
            ]

            request = 'place=offerinfo&rids=213&regset=1&pp=18&offerid={waremd5}&rearr-factors=enable_offline_buybox_price=1'.format(
                waremd5=offerForCheck.waremd5
            )
            response = self.report.request_json(request)
            self.assertFragmentIn(
                response,
                [
                    {
                        'promos': [
                            {
                                'itemsInfo': {"has_dco_3p_subsidy": True, 'price': answer},
                            }
                        ],
                    }
                ],
                allow_different_len=False,
            )

    def test_direct_discount_subsidy(self):
        # проверяем promo11 с msku_17 и msku_117
        for msku in [msku_17, msku_117]:
            for rgb in ('blue', 'green', 'green_with_blue'):
                for place in ('sku_offers', 'prime'):
                    for disable_promo_match_rearr in (None, 0, 1):
                        request = 'place={place}&rids=0&regset=1&pp=18&market-sku={msku}&rgb={rgb}'
                        if disable_promo_match_rearr is not None:
                            request += '&rearr-factors=market_disable_promo_matching_by_feed_offer_id={}'.format(
                                disable_promo_match_rearr
                            )
                        request = request.format(place=place, msku=msku.sku, rgb=rgb)
                        response = self.report.request_json(request)

                        # Проверяем что в выдаче есть оффер с корректным блоком "promo"
                        self.assertFragmentIn(
                            response,
                            [
                                {
                                    'promos': [
                                        {
                                            'itemsInfo': {
                                                "has_dco_3p_subsidy": True,
                                                'price': {
                                                    'value': str(1400),  # повторяем цену оффера
                                                    'currency': 'RUR',
                                                    'discount': {
                                                        'oldMin': str(2050),
                                                        'absolute': str(650),
                                                    },
                                                    'subsidy': {
                                                        'oldMin': str(2000),
                                                        'absolute': str(600),
                                                    },
                                                },
                                            },
                                        }
                                    ],
                                }
                            ],
                            allow_different_len=False,
                        )

        # Проверяем promo11 с msku_17 и msku_117 place=offerinfo без флага rgb=blue
        for offer in [blue_offer_17, blue_offer_117]:
            request = 'place=offerinfo&rids=0&regset=1&pp=18&offerid={waremd5}'.format(waremd5=offer.waremd5)
            response = self.report.request_json(request)
            self.assertFragmentIn(
                response,
                [
                    {
                        'promos': [
                            {
                                'itemsInfo': {
                                    "has_dco_3p_subsidy": True,
                                    'price': {
                                        'value': str(1400),  # повторяем цену оффера
                                        'currency': 'RUR',
                                        'discount': {
                                            'oldMin': str(2050),
                                            'absolute': str(650),
                                        },
                                        'subsidy': {
                                            'oldMin': str(2000),
                                            'absolute': str(600),
                                        },
                                    },
                                },
                            }
                        ],
                    }
                ],
                allow_different_len=False,
            )

    def test_drop_to_discount_price_without_subsidy(self):
        # если discount_price выше ref_min/buybox, скинуть до него мы можем, а до min_ref/buybox - не можем, то скидываем за счет стратегии
        answer = [
            {
                "prices": {
                    "currency": "RUR",
                    "value": "800",
                },
                "priceBeforeDynamicStrategy": {"currency": "RUR", "value": "1000"},
                'promos': Absent(),
            }
        ]

        request = 'place=offerinfo&rids=0&regset=1&pp=18&offerid={waremd5}'.format(waremd5=blue_offer_226.waremd5)
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            answer,
            allow_different_len=False,
        )

        self.rty.offers += [
            RtyOffer(
                feedid=blue_offer_226.feedid,
                offerid=blue_offer_226.offerid,
                dynamic_pricing=DynamicPricingStrategySSKU(
                    dynamic_pricing_type=DYNAMIC_PRICING_TYPE.BUYBOX,
                    dynamic_pricing_threshold_is_percent=1,
                    dynamic_pricing_threshold_value=30,
                ),
            ),
        ]

        request = 'place=offerinfo&rids=213&regset=1&pp=18&offerid={waremd5}&rearr-factors=enable_offline_buybox_price=1'.format(
            waremd5=blue_offer_226.waremd5
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            answer,
            allow_different_len=False,
        )

    def test_direct_discount_yandex_plus(self):
        # проверяем promo15 с msku_21

        for rgb in ('blue', 'green', 'green_with_blue'):
            for place in ('sku_offers', 'prime'):
                for disable_promo_match_rearr in (None, 0, 1):
                    request = 'place={place}&rids=0&regset=1&pp=18&market-sku={msku}&rgb={rgb}&perks=yandex_plus'
                    if disable_promo_match_rearr is not None:
                        request += '&rearr-factors=market_disable_promo_matching_by_feed_offer_id={}'.format(
                            disable_promo_match_rearr
                        )
                    request = request.format(place=place, msku=msku_21.sku, rgb=rgb)
                    response = self.report.request_json(request)

                    # Проверяем что в выдаче есть оффер с корректным блоком "promo"
                    self.assertFragmentIn(
                        response,
                        [
                            {
                                'promos': [
                                    {
                                        'itemsInfo': {
                                            'price': {
                                                'value': str(800),  # повторяем цену оффера
                                                'currency': 'RUR',
                                                'discount': {
                                                    'oldMin': str(1000),
                                                    'absolute': str(200),
                                                },
                                            },
                                        },
                                    }
                                ],
                            }
                        ],
                        allow_different_len=False,
                    )

        # Проверяем promo15 с msku_21: place=offerinfo без флага rgb=blue
        request = 'place=offerinfo&rids=0&regset=1&pp=18&perks=yandex_plus&offerid={waremd5}'.format(
            waremd5=blue_offer_21.waremd5
        )
        response = self.report.request_json(request)
        self.assertFragmentIn(
            response,
            [
                {
                    'promos': [
                        {
                            'itemsInfo': {
                                'price': {
                                    'value': str(800),  # повторяем цену оффера
                                    'currency': 'RUR',
                                    'discount': {
                                        'oldMin': str(1000),
                                        'absolute': str(200),
                                    },
                                },
                            },
                        }
                    ],
                }
            ],
            allow_different_len=False,
        )

    def test_check_prices_offer_with_promo(self):
        self.assertFragmentIn(
            self.report.request_json('place=check_prices&feed_shoffer_id=777-offer_id_1'),
            {
                'price': {'value': '1234'},
            },
        )

    def test_dd_on_dsbs(self):
        # Проверяем что прямая скидка корректно работает на ДСБС офферах
        self.check_promo(promo18, DSBS_SKU_1, dsbs_offer, True)

    def test_direct_discount_subsidy_on_dsbs(self):
        # Проверяем, что можем выдать субсидию на DBS
        self.check_promo(promo19, DSBS_SKU_2, dsbs_subsidy_offer, True)

    def test_personal_promo(self):
        # Проверяем что прямая скидка корректно работает на персональных промо
        for rearr_personal in (None, 0, 1):
            for antiperk in ('', 'perk4'):
                for puid in (
                    '11999',
                    '1152921505811852323',
                ):  # 1152921505811852323 лежит внутри [2**60, 2**61-1], это анонимы
                    request = 'place=offerinfo&rids=0&regset=1&pp=18&offerid={waremd5}&puid={puid}'.format(
                        waremd5=blue_offer_45.waremd5, puid=puid
                    )
                    if rearr_personal is not None:
                        request += '&rearr-factors=personal_promo_direct_discount_enabled={rearr_personal}'.format(
                            rearr_personal=rearr_personal
                        )
                        request += ';exp_perks=perk2'
                        request += '&rearr-factors=exp_perks=perk1,perk3' + (',' + antiperk if antiperk else '')
                    response = self.report.request_json(request)

                    if (rearr_personal == 1) and (not antiperk) and (int(puid) < (2**60)):
                        self.assertFragmentIn(
                            response,
                            [
                                {
                                    'promos': [
                                        {
                                            'isPersonal': True,
                                        }
                                    ],
                                }
                            ],
                            allow_different_len=False,
                        )
                    else:
                        self.assertFragmentIn(
                            response,
                            {
                                'search': {
                                    'results': [
                                        {
                                            'promos': Absent(),
                                        }
                                    ]
                                }
                            },
                            allow_different_len=False,
                        )


if __name__ == '__main__':
    main()
