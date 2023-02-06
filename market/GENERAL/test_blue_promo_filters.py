#!/usr/bin/env python
# -*- coding: utf-8 -*-


import runner  # noqa

from core.matcher import Contains, Absent
from core.report import REQUEST_TIMESTAMP
from core.testcase import TestCase, main
from core.types import (
    HyperCategory,
    HyperCategoryType,
    DynamicBlueGenericBundlesPromos,
    DynamicSkuOffer,
    MnPlace,
    Promo,
    PromoType,
    Shop,
    create_valid_promo_dates,
    make_cheapest_as_gift,
    make_expired_generic_bundle_promo,
    make_generic_bundle_promo,
    make_not_started_generic_bundle_promo,
)
from core.types.dynamic_filters import DynamicBluePromosBlacklist, DynamicPromoSecondaries
from core.types.offer_promo import (
    OffersMatchingRules,
    PromoBlueFlash,
    PromoBlueSet,
    PromoDirectDiscount,
    PromoBlueCashback,
    PromoRestrictions,
)
from core.types.sku import MarketSku, BlueOffer
from core.types.parent_promos import ParentPromo
from datetime import datetime, timedelta

ANAPLAN_PROMO_ID = 'anaplan!'
PARENT_PROMO_ID = 'blackfriday!78'
PARENT_PROMO_ID_1 = 'NG#021'
PARENT_PROMO_ID_2 = '#PP221'
PARENT_PROMO_NAME_2 = 'Большая распродажа'
PARENT_PROMO_ID_3 = 'FT001'
PARENT_PROMO_NAME_3 = 'Будущая распродажа'
PERK_CASHBACK = "yandex_cashback"
PERK_EXTRA_CASHBACK = 'yandex_extra_cashback'
HYPERID_1 = 500601
HYPERID_2 = 500602
DEFAULT_PRICE = 500
HID_1 = 37


class Shops(object):
    third_party1 = Shop(
        fesh=777, datafeed_id=777, priority_region=213, supplier_type=Shop.THIRD_PARTY, blue=Shop.BLUE_REAL
    )
    third_party2 = Shop(
        fesh=888, datafeed_id=888, priority_region=213, supplier_type=Shop.THIRD_PARTY, blue=Shop.BLUE_REAL
    )


def make_blue_offer_with_defaults(shop_sku, wareId, feedId=None, price=DEFAULT_PRICE, price_old=None, ts=None):
    return BlueOffer(
        price=price,
        price_old=price_old,
        feedid=feedId or Shops.third_party1.datafeed_id,
        offerid=shop_sku,
        waremd5=wareId,
        fee=100,
        ts=ts,
    )


def get_perks(perks):
    return ','.join(perks)


class OfferIds:
    shop_sku_generic_bundle_usual_primary1 = 'shop_sku_usual_primary_1.ыыы'
    shop_sku_generic_bundle_usual_primary2 = 'shop_sku_usual_primary_2'
    shop_sku_generic_bundle_usual_primary3 = 'shop_sku_usual_primary_3'
    shop_sku_generic_bundle_usual_gift_1 = 'shop_sku_generic_bundle_usual_gift_1'
    shop_sku_generic_bundle_usual_gift_2 = 'shop_sku_generic_bundle_usual_gift_2'
    shop_sku_generic_bundle_usual_primary_another_supplier = 'shop_sku_generic_bundle_usual_primary_another_supplier'
    shop_sku_usual_generic_bundle_gift_another_supplier = 'shop_sku_usual_generic_bundle_gift_another_supplier'
    shop_sku_expired_promo_primary = 'shop_sku_expired_promo_primary'
    shop_sku_expired_promo_gift = 'shop_sku_expired_promo_GIFT'
    shop_sku_not_started_promo_primary = 'shop_sku_not_started_promo_primary'
    shop_sku_not_started_promo_gift = 'shop_sku_not_started_promo_gift'
    shop_sku_disabled_promo_primary = 'shop_sku_disabled_promo_primary'
    shop_sku_disabled_promo_gift = 'shop_sku_disabled_promo_gift'
    shop_sku_cheapest_as_gift = 'shop_S%K%U_cheapest_as_gift'
    shop_sku_cheapest_as_gift2 = 'shop_sku_cheapest_as_gift_два'
    shop_sku_blue_flash = 'shop_sku_blue_flash'
    shop_sku_blue_set_1 = 'shop_sku_blue_set_1'
    shop_sku_blue_set_2 = 'shop_sku_blue_set_2'
    shop_sku_blue_set_3 = '00065.00026.100126179423'
    shop_sku_blue_set_4 = '00065.00026.100126178698'
    shop_sku_direct_discount_1 = 'shop_sku_direct_discount_1'
    shop_sku_direct_discount_2 = 'shop_sku_direct_discount_2'
    shop_sku_direct_discount_3 = 'shop_sku_direct_discount_3'
    shop_sku_direct_discount_parent_id = 'shop_sku_direct_discount_parent_id'
    shop_sku_direct_discount_parent_id_2 = 'shop_sku_direct_discount_parent_2'
    shop_sku_direct_discount_disabled_parent_id = 'shop_sku_direct_discount_disabled_parent_id'
    shop_sku_cashback_with_parent_id = 'shop_sku_cashback_with_parent_id'
    shop_sku_direct_discount_same_model = 'shop_sku_direct_discount_same_model'
    shop_sku_direct_discount_time_shift = 'shop_sku_direct_discount_time_shift'
    shop_sku_no_promo_price_filter = 'shop_sku_no_promo_price_filter'
    shop_sku_no_promo = 'shop_sku_no_promo'


class BlueOffersFactory(object):
    required_offers = {
        OfferIds.shop_sku_generic_bundle_usual_primary1: {'wareId': 'BlueUsual1PrimaryGB--w'},
        OfferIds.shop_sku_generic_bundle_usual_primary2: {'wareId': 'BlueUsual2PrimaryGB--w'},
        OfferIds.shop_sku_generic_bundle_usual_primary3: {'wareId': 'BlueUsual3PrimaryGB--w'},
        OfferIds.shop_sku_generic_bundle_usual_gift_1: {'wareId': 'BlueUsual1GiftGB-----w'},
        OfferIds.shop_sku_generic_bundle_usual_gift_2: {'wareId': 'BlueUsual2GiftGB-----w'},
        OfferIds.shop_sku_generic_bundle_usual_primary_another_supplier: {
            'wareId': 'BlueAnotherPrimary---w',
            'feed_id': Shops.third_party2.datafeed_id,
        },
        OfferIds.shop_sku_usual_generic_bundle_gift_another_supplier: {
            'wareId': 'BlueAnotherGift------w',
            'feed_id': Shops.third_party2.datafeed_id,
        },
        OfferIds.shop_sku_expired_promo_primary: {'wareId': 'BlueExpiredPrimary---w'},
        OfferIds.shop_sku_expired_promo_gift: {'wareId': 'BlueExpiredGift------w'},
        OfferIds.shop_sku_not_started_promo_primary: {'wareId': 'BlueNotStartedPrimaryw'},
        OfferIds.shop_sku_not_started_promo_gift: {'wareId': 'BlueNotStartedGift---w'},
        OfferIds.shop_sku_disabled_promo_primary: {'wareId': 'BlueDisabledPrimary--w'},
        OfferIds.shop_sku_disabled_promo_gift: {'wareId': 'BlueDisabledGift-----w'},
        OfferIds.shop_sku_cheapest_as_gift: {'wareId': 'BlueCheapestAsGift---w'},
        OfferIds.shop_sku_cheapest_as_gift2: {'wareId': 'BlueCheapestAsGift2--w'},
        OfferIds.shop_sku_blue_flash: {'wareId': 'BlueFlash------------w'},
        OfferIds.shop_sku_blue_set_1: {'wareId': 'BlueSet-1------------w'},
        OfferIds.shop_sku_blue_set_2: {'wareId': 'BlueSet-2------------w'},
        OfferIds.shop_sku_blue_set_3: {'wareId': 'BlueSet-3------------w'},
        OfferIds.shop_sku_blue_set_4: {'wareId': 'BlueSet-4------------w'},
        OfferIds.shop_sku_direct_discount_1: {'wareId': 'DirectDiscount-1-----w'},
        OfferIds.shop_sku_direct_discount_2: {'wareId': 'DirectDiscount-2-----w', 'price_old': 1000},
        OfferIds.shop_sku_direct_discount_3: {'wareId': 'DirectDiscount-3-----w'},
        OfferIds.shop_sku_direct_discount_parent_id: {'wareId': 'DirectDiscount-p_id__w'},
        OfferIds.shop_sku_direct_discount_parent_id_2: {'wareId': 'DirectDiscount-p2----w', 'price': 500},
        OfferIds.shop_sku_direct_discount_disabled_parent_id: {'wareId': 'DirectDiscount-pdis--w', 'price': 50},
        OfferIds.shop_sku_cashback_with_parent_id: {'wareId': 'Cashback-w-parent-id-w', 'price': 10000, 'ts': 17},
        OfferIds.shop_sku_direct_discount_same_model: {'wareId': 'DirectDiscount-mod---w', 'price': 100000, 'ts': 18},
        OfferIds.shop_sku_direct_discount_time_shift: {'wareId': 'DirectDiscount-times-w'},
        OfferIds.shop_sku_no_promo_price_filter: {'wareId': 'NoPromo-filter-------w', 'price': 100},
        OfferIds.shop_sku_no_promo: {'wareId': 'NoPromo--------------w', 'price': 100},
    }

    @staticmethod
    def make_offers():
        return {
            key: make_blue_offer_with_defaults(
                key,
                requirements['wareId'],
                requirements.get('feed_id'),
                requirements.get('price', DEFAULT_PRICE),
                requirements.get('price_old'),
                requirements.get('ts'),
            )
            for key, requirements in BlueOffersFactory.required_offers.items()
        }


BLUE_OFFERS = BlueOffersFactory.make_offers()


def make_mskus():
    required_mskus = {
        'msku_usual_primary1': [BLUE_OFFERS[OfferIds.shop_sku_generic_bundle_usual_primary1]],
        'msku_usual_primary2': [BLUE_OFFERS[OfferIds.shop_sku_generic_bundle_usual_primary2]],
        'msku_usual_primary3': [BLUE_OFFERS[OfferIds.shop_sku_generic_bundle_usual_primary3]],
        'msku_usual_gift': [
            BLUE_OFFERS[OfferIds.shop_sku_generic_bundle_usual_gift_1],
            BLUE_OFFERS[OfferIds.shop_sku_generic_bundle_usual_gift_2],
        ],
        'msku_usual_primary_another_supplier': [
            BLUE_OFFERS[OfferIds.shop_sku_generic_bundle_usual_primary_another_supplier]
        ],
        'msku_usual_gift_another_supplier': [BLUE_OFFERS[OfferIds.shop_sku_usual_generic_bundle_gift_another_supplier]],
        'msku_expired_promo_primary': [BLUE_OFFERS[OfferIds.shop_sku_expired_promo_primary]],
        'msku_expired_promo_gift': [BLUE_OFFERS[OfferIds.shop_sku_expired_promo_gift]],
        'msku_not_started_promo_primary': [BLUE_OFFERS[OfferIds.shop_sku_not_started_promo_primary]],
        'msku_not_started_promo_gift': [BLUE_OFFERS[OfferIds.shop_sku_not_started_promo_gift]],
        'msku_disabled_promo_primary': [BLUE_OFFERS[OfferIds.shop_sku_disabled_promo_primary]],
        'msku_disabled_promo_gift': [BLUE_OFFERS[OfferIds.shop_sku_disabled_promo_gift]],
        'msku_cheapest_as_gift': [BLUE_OFFERS[OfferIds.shop_sku_cheapest_as_gift]],
        'msku_cheapest_as_gift2': [BLUE_OFFERS[OfferIds.shop_sku_cheapest_as_gift2]],
        'msku_blue_flash': [BLUE_OFFERS[OfferIds.shop_sku_blue_flash]],
        'msku_blue_set_1': [BLUE_OFFERS[OfferIds.shop_sku_blue_set_1]],
        'msku_blue_set_2': [BLUE_OFFERS[OfferIds.shop_sku_blue_set_2]],
        'msku_blue_set_3': [BLUE_OFFERS[OfferIds.shop_sku_blue_set_3]],
        'msku_blue_set_4': [BLUE_OFFERS[OfferIds.shop_sku_blue_set_4]],
        'msku_direct_discount_1': [BLUE_OFFERS[OfferIds.shop_sku_direct_discount_1]],
        'msku_direct_discount_2': [BLUE_OFFERS[OfferIds.shop_sku_direct_discount_2]],
        'msku_direct_discount_3': [BLUE_OFFERS[OfferIds.shop_sku_direct_discount_3]],
        'msku_direct_discount_parent_id': [BLUE_OFFERS[OfferIds.shop_sku_direct_discount_parent_id]],
        'msku_direct_discount_parent_2': [BLUE_OFFERS[OfferIds.shop_sku_direct_discount_parent_id_2]],
        'msku_direct_discount_dsabled_parent_id': [BLUE_OFFERS[OfferIds.shop_sku_direct_discount_disabled_parent_id]],
        'msku_cashback_with_parent_id': [BLUE_OFFERS[OfferIds.shop_sku_cashback_with_parent_id]],
        'msku_direct_discount_same_model': [BLUE_OFFERS[OfferIds.shop_sku_direct_discount_same_model]],
        'msku_direct_discount_time_shift': [BLUE_OFFERS[OfferIds.shop_sku_direct_discount_time_shift]],
        'msku_no_promo_price_filter': [BLUE_OFFERS[OfferIds.shop_sku_no_promo_price_filter]],
        'msku_no_promo': [BLUE_OFFERS[OfferIds.shop_sku_no_promo]],
    }

    # Два оффера с одной и той же моделью, чтобы проверить выбор дефолтного оффера с нужной акцией
    msku_to_hyperid = {
        'msku_direct_discount_2': HYPERID_1,
        'msku_no_promo': HYPERID_1,
        'msku_cashback_with_parent_id': HYPERID_2,
        'msku_direct_discount_same_model': HYPERID_2,
    }
    msku_to_hid = {
        'msku_direct_discount_parent_2': HID_1,
        'msku_no_promo_price_filter': HID_1,
    }
    return {
        key: MarketSku(
            title=key,
            hyperid=msku_to_hyperid.get(key, num),
            sku=num,
            hid=msku_to_hid.get(key, None),
            blue_offers=required_mskus[key],
        )
        for num, key in enumerate(required_mskus.keys())
    }


MSKUS = make_mskus()


class Promos(object):
    generic_bundle_usual_promo1 = make_generic_bundle_promo(
        primary_offer_ids=[
            OfferIds.shop_sku_generic_bundle_usual_primary1,
            OfferIds.shop_sku_generic_bundle_usual_primary2,
            OfferIds.shop_sku_generic_bundle_usual_primary3,
        ],
        secondary_offer_ids=[
            OfferIds.shop_sku_generic_bundle_usual_gift_1,
            OfferIds.shop_sku_generic_bundle_usual_gift_2,
            "NON_EXISTING_GIFT_ID",
        ],
        shop_promo_id="generic_bundle_usual_promo1",
        feed_id=Shops.third_party1.datafeed_id,
        now_timestamp=REQUEST_TIMESTAMP,
        key='generic_bundle_usual_promo1',
    )

    generic_bundle_usual_promo2 = make_generic_bundle_promo(
        primary_offer_ids=[OfferIds.shop_sku_generic_bundle_usual_primary_another_supplier],
        secondary_offer_ids=[OfferIds.shop_sku_usual_generic_bundle_gift_another_supplier],
        shop_promo_id="generic_bundle_usual_promo2",
        feed_id=Shops.third_party2.datafeed_id,
        now_timestamp=REQUEST_TIMESTAMP,
        key='generic_bundle_usual_promo2',
    )
    generic_bundle_expired_promo = make_expired_generic_bundle_promo(
        primary_offer_ids=[OfferIds.shop_sku_expired_promo_primary],
        secondary_offer_ids=[OfferIds.shop_sku_expired_promo_gift],
        shop_promo_id="generic_bundle_expired_promo",
        feed_id=Shops.third_party1.datafeed_id,
        now_timestamp=REQUEST_TIMESTAMP,
        key='generic_bundle_expired_promo',
    )
    generic_bundle_not_started_promo = make_not_started_generic_bundle_promo(
        primary_offer_ids=[OfferIds.shop_sku_not_started_promo_primary],
        secondary_offer_ids=[OfferIds.shop_sku_not_started_promo_gift],
        shop_promo_id="generic_bundle_not_started_promo",
        feed_id=Shops.third_party1.datafeed_id,
        now_timestamp=REQUEST_TIMESTAMP,
        key='generic_bundle_not_started_promo',
    )
    generic_bundle_disabled_promo = make_generic_bundle_promo(
        primary_offer_ids=[OfferIds.shop_sku_disabled_promo_primary],
        secondary_offer_ids=[OfferIds.shop_sku_disabled_promo_gift],
        shop_promo_id="generic_bundle_disabled_promo",
        feed_id=Shops.third_party1.datafeed_id,
        now_timestamp=REQUEST_TIMESTAMP,
        key='generic_bundle_disabled_promo',
    )

    cheapest_as_gift = make_cheapest_as_gift(
        feed_id=Shops.third_party1.datafeed_id,
        key='Promo_cheapest_as_gift',
        now_timestamp=REQUEST_TIMESTAMP,
        offer_ids=[
            (Shops.third_party1.datafeed_id, OfferIds.shop_sku_cheapest_as_gift),
            (Shops.third_party1.datafeed_id, OfferIds.shop_sku_cheapest_as_gift2),
        ],
        shop_promo_id='cheapest_as_gift_promo',
        offers_matching_rules=[
            OffersMatchingRules(
                feed_offer_ids=[
                    [Shops.third_party1.datafeed_id, OfferIds.shop_sku_cheapest_as_gift],
                    [Shops.third_party1.datafeed_id, OfferIds.shop_sku_cheapest_as_gift2],
                ]
            ),
        ],
    )

    blue_flash = Promo(
        promo_type=PromoType.BLUE_FLASH,
        key='BLUE_FLASH_PROMO',
        blue_flash=PromoBlueFlash(
            items=[
                {
                    'feed_id': 777,
                    'offer_id': OfferIds.shop_sku_blue_flash,
                    'price': {'value': BLUE_OFFERS[OfferIds.shop_sku_blue_flash].price - 90, 'currency': 'RUR'},
                },
            ],
            allow_berubonus=True,
            allow_promocode=True,
        ),
        shop_promo_id='blue_flash_promo',
        anaplan_id=ANAPLAN_PROMO_ID,
        offers_matching_rules=[
            OffersMatchingRules(
                feed_offer_ids=[
                    [777, OfferIds.shop_sku_blue_flash],
                ]
            ),
        ],
        **create_valid_promo_dates(REQUEST_TIMESTAMP)
    )

    blue_set = Promo(
        promo_type=PromoType.BLUE_SET,
        feed_id=777,
        key='BLUE_SET_PROMO',
        url='http://яндекс.рф/',
        blue_set=PromoBlueSet(
            sets_content=[
                {
                    'items': [
                        {'offer_id': OfferIds.shop_sku_blue_set_1},
                        {'offer_id': OfferIds.shop_sku_blue_set_2},
                    ],
                    'linked': True,
                },
                {
                    'items': [
                        {'offer_id': OfferIds.shop_sku_blue_set_3},
                        {'offer_id': OfferIds.shop_sku_blue_set_4},
                    ],
                    'linked': True,
                },
            ],
        ),
        shop_promo_id='blue_set_promo',
        anaplan_id=ANAPLAN_PROMO_ID,
        offers_matching_rules=[
            OffersMatchingRules(
                feed_offer_ids=[
                    [777, OfferIds.shop_sku_blue_set_1],
                    [777, OfferIds.shop_sku_blue_set_2],
                    [777, OfferIds.shop_sku_blue_set_3],
                    [777, OfferIds.shop_sku_blue_set_4],
                ]
            ),
        ],
        **create_valid_promo_dates(REQUEST_TIMESTAMP)
    )

    direct_discount = Promo(
        promo_type=PromoType.DIRECT_DISCOUNT,
        feed_id=777,
        key='DIRECT_DISCOUNT_PROMO',
        direct_discount=PromoDirectDiscount(
            items=[
                {
                    'feed_id': 777,
                    'offer_id': OfferIds.shop_sku_direct_discount_1,
                    'discount_price': {
                        'value': BLUE_OFFERS[OfferIds.shop_sku_direct_discount_1].price - 10,
                        'currency': 'RUR',
                    },
                    'old_price': {'value': 12345, 'currency': 'RUR'},
                },
                {
                    'feed_id': 777,
                    'offer_id': OfferIds.shop_sku_direct_discount_2,
                },
            ],
            allow_berubonus=True,
            allow_promocode=True,
        ),
        shop_promo_id='direct_discount_shop_promo_id',
        offers_matching_rules=[
            OffersMatchingRules(
                feed_offer_ids=[
                    [777, OfferIds.shop_sku_direct_discount_1],
                    [777, OfferIds.shop_sku_direct_discount_2],
                ]
            ),
        ],
        **create_valid_promo_dates(REQUEST_TIMESTAMP)
    )

    direct_discount_with_parent_id = Promo(
        promo_type=PromoType.DIRECT_DISCOUNT,
        feed_id=777,
        key='DIRECT_DISCOUNT_PARENT_ID',
        parent_promo_id=PARENT_PROMO_ID,
        direct_discount=PromoDirectDiscount(
            items=[
                {
                    'feed_id': 777,
                    'offer_id': OfferIds.shop_sku_direct_discount_parent_id,
                    'discount_price': {
                        'value': BLUE_OFFERS[OfferIds.shop_sku_direct_discount_parent_id].price - 10,
                        'currency': 'RUR',
                    },
                    'old_price': {'value': 12345, 'currency': 'RUR'},
                },
                {
                    'feed_id': 777,
                    'offer_id': OfferIds.shop_sku_direct_discount_3,
                    'discount_price': {
                        'value': BLUE_OFFERS[OfferIds.shop_sku_direct_discount_3].price + 10,
                        'currency': 'RUR',
                    },
                    'old_price': {'value': 12345, 'currency': 'RUR'},
                },
            ],
        ),
        shop_promo_id='direct_discount_parent_promo_id',
        offers_matching_rules=[
            OffersMatchingRules(
                feed_offer_ids=[
                    [777, OfferIds.shop_sku_direct_discount_parent_id],
                    [777, OfferIds.shop_sku_direct_discount_3],
                ]
            ),
        ],
        **create_valid_promo_dates(REQUEST_TIMESTAMP)
    )

    cashback_with_parent_id = Promo(
        promo_type=PromoType.BLUE_CASHBACK,
        key='cashback_with_parent_id',
        shop_promo_id='cashback_with_parent_id',
        url='http://blue_cashback.com/',
        parent_promo_id=PARENT_PROMO_ID_1,
        blue_cashback=PromoBlueCashback(share=0.2, version=10, priority=1),
        restrictions=PromoRestrictions(
            predicates=[
                {
                    'perks': [PERK_EXTRA_CASHBACK],
                }
            ],
        ),
        offers_matching_rules=[
            OffersMatchingRules(
                feed_offer_ids=[
                    [777, OfferIds.shop_sku_cashback_with_parent_id],
                ]
            ),
        ],
    )

    direct_discount_same_model = Promo(
        promo_type=PromoType.DIRECT_DISCOUNT,
        feed_id=777,
        key='DIRECT_DISCOUNT_SAME_MODEL',
        parent_promo_id=PARENT_PROMO_ID_1,
        direct_discount=PromoDirectDiscount(
            items=[
                {
                    'feed_id': 777,
                    'offer_id': OfferIds.shop_sku_direct_discount_same_model,
                    'discount_price': {
                        'value': BLUE_OFFERS[OfferIds.shop_sku_direct_discount_same_model].price - 10,
                        'currency': 'RUR',
                    },
                },
            ],
        ),
        shop_promo_id='direct_discount_same_model',
        offers_matching_rules=[
            OffersMatchingRules(
                feed_offer_ids=[
                    [777, OfferIds.shop_sku_direct_discount_same_model],
                ]
            ),
        ],
        **create_valid_promo_dates(REQUEST_TIMESTAMP)
    )

    direct_discount_with_parent_id_2 = Promo(
        promo_type=PromoType.DIRECT_DISCOUNT,
        feed_id=777,
        key='DIRECT_DISCOUNT_PARENT_2',
        parent_promo_id=PARENT_PROMO_ID_2,
        direct_discount=PromoDirectDiscount(
            items=[
                {
                    'feed_id': 777,
                    'offer_id': OfferIds.shop_sku_direct_discount_parent_id_2,
                    'discount_price': {
                        'value': BLUE_OFFERS[OfferIds.shop_sku_direct_discount_parent_id_2].price - 10,
                        'currency': 'RUR',
                    },
                    'old_price': {'value': 12345, 'currency': 'RUR'},
                },
            ],
        ),
        shop_promo_id='direct_discount_parent_promo_2',
        offers_matching_rules=[
            OffersMatchingRules(
                feed_offer_ids=[
                    [777, OfferIds.shop_sku_direct_discount_parent_id_2],
                ]
            ),
        ],
        **create_valid_promo_dates(REQUEST_TIMESTAMP)
    )

    direct_discount_disabled_with_parent_id = Promo(
        promo_type=PromoType.DIRECT_DISCOUNT,
        feed_id=777,
        key='DIRECT_DISCOUNT_DISABLED_PARENT_ID',
        parent_promo_id=PARENT_PROMO_ID_2,
        direct_discount=PromoDirectDiscount(
            items=[
                {
                    'feed_id': 777,
                    'offer_id': OfferIds.shop_sku_direct_discount_disabled_parent_id,
                    'discount_price': {
                        'value': BLUE_OFFERS[OfferIds.shop_sku_direct_discount_disabled_parent_id].price - 1,
                        'currency': 'RUR',
                    },
                    'old_price': {
                        'value': BLUE_OFFERS[OfferIds.shop_sku_direct_discount_disabled_parent_id].price,
                        'currency': 'RUR',
                    },
                },
            ],
        ),
        shop_promo_id='direct_discount_disabled_parent_promo_id',
        offers_matching_rules=[
            OffersMatchingRules(
                feed_offer_ids=[
                    [777, OfferIds.shop_sku_direct_discount_disabled_parent_id],
                ]
            ),
        ],
        **create_valid_promo_dates(REQUEST_TIMESTAMP)
    )

    direct_discount_time_shift = Promo(
        promo_type=PromoType.DIRECT_DISCOUNT,
        feed_id=777,
        key='DIRECT_DISCOUNT_TIME_SHIFT',
        shop_promo_id='direct_discount_time_shift',
        parent_promo_id=PARENT_PROMO_ID_3,
        direct_discount=PromoDirectDiscount(
            items=[
                {
                    'feed_id': 777,
                    'offer_id': OfferIds.shop_sku_direct_discount_time_shift,
                    'discount_price': {
                        'value': BLUE_OFFERS[OfferIds.shop_sku_direct_discount_time_shift].price - 10,
                        'currency': 'RUR',
                    },
                },
            ],
        ),
        offers_matching_rules=[
            OffersMatchingRules(
                feed_offer_ids=[
                    [777, OfferIds.shop_sku_direct_discount_time_shift],
                ]
            ),
        ],
        **create_valid_promo_dates(REQUEST_TIMESTAMP)
    )

    enabled_by_loyalty = [
        generic_bundle_usual_promo1,
        generic_bundle_expired_promo,
        generic_bundle_not_started_promo,
        generic_bundle_usual_promo2,
        cheapest_as_gift,
        blue_flash,
        blue_set,
        direct_discount_with_parent_id,
        direct_discount_with_parent_id_2,
        direct_discount_disabled_with_parent_id,
        cashback_with_parent_id,
        direct_discount_same_model,
        direct_discount_time_shift,
    ]
    all = enabled_by_loyalty + [generic_bundle_disabled_promo, direct_discount]
    all_dict = dict((promo.key, promo) for promo in all)


class T(TestCase):
    @classmethod
    def prepare(self):
        # Фиксируем порядок офферов в модельной выдаче
        self.matrixnet.on_default_place(MnPlace.BASE_SEARCH).respond(0.1)
        self.matrixnet.on_place(MnPlace.BASE_SEARCH, 17).respond(0.3)
        self.matrixnet.on_place(MnPlace.BASE_SEARCH, 18).respond(0.2)
        self.matrixnet.on_place(MnPlace.IMPLICIT_MODEL_WIZARD_META_RANK, 17).respond(0.3)
        self.matrixnet.on_place(MnPlace.IMPLICIT_MODEL_WIZARD_META_RANK, 18).respond(0.2)

        self.settings.default_search_experiment_flags += [
            'enable_fast_promo_matcher=1;enable_fast_promo_matcher_test=1'
        ]

        BLUE_OFFERS[OfferIds.shop_sku_generic_bundle_usual_primary1].promo = [Promos.generic_bundle_usual_promo1]
        BLUE_OFFERS[OfferIds.shop_sku_generic_bundle_usual_primary2].promo = [Promos.generic_bundle_usual_promo1]
        BLUE_OFFERS[OfferIds.shop_sku_generic_bundle_usual_primary3].promo = [Promos.generic_bundle_usual_promo1]
        BLUE_OFFERS[OfferIds.shop_sku_generic_bundle_usual_primary_another_supplier].promo = [
            Promos.generic_bundle_usual_promo2
        ]
        BLUE_OFFERS[OfferIds.shop_sku_expired_promo_primary].promo = [Promos.generic_bundle_expired_promo]
        BLUE_OFFERS[OfferIds.shop_sku_not_started_promo_primary].promo = [Promos.generic_bundle_not_started_promo]
        BLUE_OFFERS[OfferIds.shop_sku_disabled_promo_primary].promo = [Promos.generic_bundle_disabled_promo]
        BLUE_OFFERS[OfferIds.shop_sku_cheapest_as_gift].promo = [Promos.cheapest_as_gift]
        BLUE_OFFERS[OfferIds.shop_sku_cheapest_as_gift2].promo = [Promos.cheapest_as_gift]
        BLUE_OFFERS[OfferIds.shop_sku_blue_flash].promo = [Promos.blue_flash]
        BLUE_OFFERS[OfferIds.shop_sku_blue_set_1].promo = [Promos.blue_set]
        BLUE_OFFERS[OfferIds.shop_sku_blue_set_2].promo = [Promos.blue_set]
        BLUE_OFFERS[OfferIds.shop_sku_blue_set_3].promo = [Promos.blue_set]
        BLUE_OFFERS[OfferIds.shop_sku_blue_set_4].promo = [Promos.blue_set]
        BLUE_OFFERS[OfferIds.shop_sku_direct_discount_1].promo = [Promos.direct_discount]
        BLUE_OFFERS[OfferIds.shop_sku_direct_discount_2].promo = [Promos.direct_discount]
        BLUE_OFFERS[OfferIds.shop_sku_direct_discount_parent_id].promo = [Promos.direct_discount_with_parent_id]
        BLUE_OFFERS[OfferIds.shop_sku_direct_discount_parent_id_2].promo = [Promos.direct_discount_with_parent_id_2]
        BLUE_OFFERS[OfferIds.shop_sku_direct_discount_disabled_parent_id].promo = [
            Promos.direct_discount_disabled_with_parent_id
        ]
        BLUE_OFFERS[OfferIds.shop_sku_direct_discount_3].promo = [Promos.direct_discount_with_parent_id]
        BLUE_OFFERS[OfferIds.shop_sku_cashback_with_parent_id].promo = [Promos.cashback_with_parent_id]
        BLUE_OFFERS[OfferIds.shop_sku_direct_discount_same_model].promo = [Promos.direct_discount_same_model]
        BLUE_OFFERS[OfferIds.shop_sku_direct_discount_time_shift].promo = [Promos.direct_discount_time_shift]

        self.index.shops += [Shops.third_party1, Shops.third_party2]

        self.index.hypertree += [HyperCategory(hid=HID_1, name='коляски', output_type=HyperCategoryType.GURU)]

        self.index.mskus += MSKUS.values()

        self.index.promos += Promos.all

        secondaries_exclude = frozenset(
            [
                (777, 'NON_EXISTING_GIFT_ID'),
                (Shops.third_party1.datafeed_id, OfferIds.shop_sku_blue_set_4),
            ]
        )
        self.dynamic.promo_secondaries += [
            DynamicPromoSecondaries(promos=self.index.promos, excludes=secondaries_exclude),
        ]

        self.settings.loyalty_enabled = True
        self.dynamic.loyalty += [
            DynamicBlueGenericBundlesPromos(whitelist=[promo.key for promo in Promos.enabled_by_loyalty])
        ]
        self.dynamic.loyalty += [
            DynamicBluePromosBlacklist(
                blacklist=[
                    (Shops.third_party1.datafeed_id, OfferIds.shop_sku_cheapest_as_gift2),
                ]
            )
        ]

        for offer, promo in (
            (OfferIds.shop_sku_generic_bundle_usual_primary1, Promos.generic_bundle_usual_promo1),
            (OfferIds.shop_sku_generic_bundle_usual_primary2, Promos.generic_bundle_usual_promo1),
            (OfferIds.shop_sku_generic_bundle_usual_primary3, Promos.generic_bundle_usual_promo1),
            (OfferIds.shop_sku_generic_bundle_usual_primary_another_supplier, Promos.generic_bundle_usual_promo2),
            (OfferIds.shop_sku_cheapest_as_gift, Promos.cheapest_as_gift),
            (OfferIds.shop_sku_cheapest_as_gift2, Promos.cheapest_as_gift),
            (OfferIds.shop_sku_blue_flash, Promos.blue_flash),
            (OfferIds.shop_sku_blue_set_1, Promos.blue_set),
            (OfferIds.shop_sku_blue_set_2, Promos.blue_set),
            (OfferIds.shop_sku_blue_set_3, Promos.blue_set),
            (OfferIds.shop_sku_blue_set_4, Promos.blue_set),
            (OfferIds.shop_sku_direct_discount_1, Promos.direct_discount),
            (OfferIds.shop_sku_direct_discount_2, Promos.direct_discount),
            (OfferIds.shop_sku_direct_discount_parent_id, Promos.direct_discount_with_parent_id),
            (OfferIds.shop_sku_direct_discount_parent_id_2, Promos.direct_discount_with_parent_id_2),
            (OfferIds.shop_sku_direct_discount_disabled_parent_id, Promos.direct_discount_disabled_with_parent_id),
            (OfferIds.shop_sku_direct_discount_3, Promos.direct_discount_with_parent_id),
            (OfferIds.shop_sku_cashback_with_parent_id, Promos.cashback_with_parent_id),
            (OfferIds.shop_sku_direct_discount_same_model, Promos.direct_discount_same_model),
            (OfferIds.shop_sku_direct_discount_time_shift, Promos.direct_discount_time_shift),
        ):
            BLUE_OFFERS[offer].blue_promo_key = promo.shop_promo_id

        # литералы для поиска по anaplan_promo_id
        BLUE_OFFERS[OfferIds.shop_sku_blue_flash].anaplan_promo_id = ANAPLAN_PROMO_ID
        BLUE_OFFERS[OfferIds.shop_sku_blue_set_2].anaplan_promo_id = ANAPLAN_PROMO_ID

        self.index.parent_promos += [
            ParentPromo(
                PARENT_PROMO_ID_2,
                PARENT_PROMO_NAME_2,
                datetime.fromtimestamp(REQUEST_TIMESTAMP).replace(hour=0, minute=0, second=0),
                datetime.fromtimestamp(REQUEST_TIMESTAMP).replace(hour=23, minute=59, second=59),
            ),
            ParentPromo(
                PARENT_PROMO_ID_3,
                PARENT_PROMO_NAME_3,
                datetime.fromtimestamp(REQUEST_TIMESTAMP).replace(hour=0, minute=0, second=0) + timedelta(days=1),
                datetime.fromtimestamp(REQUEST_TIMESTAMP).replace(hour=23, minute=59, second=59) + timedelta(days=1),
            ),
        ]

    def check_receives_all_offers_and_promos(self, filter):
        self.dynamic.disabled_sku_offers += [
            DynamicSkuOffer(shop_id=777, sku=OfferIds.shop_sku_blue_set_4),
        ]
        expected_primary_offers_response = {
            Promos.generic_bundle_usual_promo1.key: [
                BLUE_OFFERS[OfferIds.shop_sku_generic_bundle_usual_primary1].waremd5,
                BLUE_OFFERS[OfferIds.shop_sku_generic_bundle_usual_primary2].waremd5,
            ],
            Promos.generic_bundle_usual_promo2.key: [
                BLUE_OFFERS[OfferIds.shop_sku_generic_bundle_usual_primary_another_supplier].waremd5
            ],
            Promos.cheapest_as_gift.key: [BLUE_OFFERS[OfferIds.shop_sku_cheapest_as_gift].waremd5],
            Promos.blue_flash.key: [BLUE_OFFERS[OfferIds.shop_sku_blue_flash].waremd5],
            Promos.blue_set.key: [
                BLUE_OFFERS[OfferIds.shop_sku_blue_set_1].waremd5,
                BLUE_OFFERS[OfferIds.shop_sku_blue_set_2].waremd5,
            ],
            Promos.direct_discount.key: [
                BLUE_OFFERS[OfferIds.shop_sku_direct_discount_1].waremd5,
                BLUE_OFFERS[OfferIds.shop_sku_direct_discount_2].waremd5,
            ],
            Promos.direct_discount_with_parent_id.key: [
                BLUE_OFFERS[OfferIds.shop_sku_direct_discount_parent_id].waremd5,
            ],
            Promos.cashback_with_parent_id.key: [
                BLUE_OFFERS[OfferIds.shop_sku_cashback_with_parent_id].waremd5,
            ],
            Promos.direct_discount_with_parent_id_2.key: [
                BLUE_OFFERS[OfferIds.shop_sku_direct_discount_parent_id_2].waremd5,
            ],
            Promos.direct_discount_time_shift.key: [
                BLUE_OFFERS[OfferIds.shop_sku_direct_discount_time_shift].waremd5,
            ],
        }

        not_expected_offers = [
            BLUE_OFFERS[OfferIds.shop_sku_expired_promo_primary],
            BLUE_OFFERS[OfferIds.shop_sku_not_started_promo_primary],
            BLUE_OFFERS[OfferIds.shop_sku_disabled_promo_primary],
            BLUE_OFFERS[OfferIds.shop_sku_generic_bundle_usual_primary3],  # нет подарка
            BLUE_OFFERS[OfferIds.shop_sku_cheapest_as_gift2],  # блок по чёрному списку
            BLUE_OFFERS[OfferIds.shop_sku_blue_set_3],  # нет вторичного оффера в стоках
            BLUE_OFFERS[OfferIds.shop_sku_direct_discount_3],  # DeclinedByPromoPriceIsGreaterThanOfferPrice
            BLUE_OFFERS[OfferIds.shop_sku_direct_discount_disabled_parent_id],  # DeclinedByDiscountIsTooLow
            BLUE_OFFERS[
                OfferIds.shop_sku_direct_discount_same_model
            ],  # Оффер скрыт, так как проигрывает shop_sku_cashback_with_parent_id
        ]

        def make_response(waremd5, promo_key=None):
            expected_promo = Absent() if promo_key is None else [{'key': promo_key}]
            if promo_key:
                promo = {'key': promo_key}
                if Promos.all_dict[promo_key].parent_promo_id:
                    promo['parentPromoId'] = Promos.all_dict[promo_key].parent_promo_id
            return {
                'entity': 'offer',
                'wareId': waremd5,
                'promos': expected_promo,
            }

        request = 'place={place}&regset=0&pp=18&rgb={rgb}&rearr-factors=yandexuid=1{filter}&hid_depth=0&numdoc=100'

        for rgb in ('green_with_blue', 'blue'):
            response = self.report.request_json(request.format(place='prime', rgb=rgb, filter=filter))

            for promo_key, offers in expected_primary_offers_response.items():
                for waremd5 in offers:
                    self.assertFragmentIn(response, make_response(waremd5, promo_key))

            for offer in not_expected_offers:
                self.assertFragmentNotIn(response, make_response(offer.waremd5))

        # stat_numbers не фильтрует промо, офферы:
        # OfferIds.shop_sku_generic_bundle_usual_primary3
        # OfferIds.shop_sku_cheapest_as_gift2
        # OfferIds.shop_sku_blue_set_3
        # OfferIds.shop_sku_blue_set_4 (но он отфильтрован по стокам)
        # OfferIds.shop_sku_direct_discount_3
        # OfferIds.shop_sku_direct_discount_same_model
        # OfferIds.shop_sku_direct_discount_disabled_parent_id
        # При включении поиска по литералу promo_type не учитывается статус промо,
        # поэтому stat_numbers начинает врать и показывает все, что есть с этим типом.
        request += '&rearr-factors=enable_promo_type_literal={promo_type_literal}'
        n_offers = 6 + sum(map(lambda x: len(x), expected_primary_offers_response.itervalues()))
        for rgb in ('green_with_blue', 'blue'):
            for promo_type_literal in (0, 1):
                expected_offers_count = (
                    n_offers + 3 if promo_type_literal == 1 and filter.startswith('&promo-type') else n_offers
                )
                response = self.report.request_json(
                    request.format(place='stat_numbers', rgb=rgb, filter=filter, promo_type_literal=promo_type_literal)
                )
                self.assertFragmentIn(response, {'result': {'offersCount': expected_offers_count}})

    def test_request_by_shop_promo_id(self):
        promos_to_request = [str(promo.shop_promo_id) for promo in Promos.all] + ['NOT_EXISTING_PROMO']
        shop_promo_ids = ",".join(promos_to_request)
        params = '&shop-promo-id={}&perks={}'.format(shop_promo_ids, get_perks([PERK_CASHBACK, PERK_EXTRA_CASHBACK]))
        self.check_receives_all_offers_and_promos(params)

    def test_pruning_promo(self):
        def run_test(header, pp, exclude_client, include_pp, filter, prun_flag, prun_value, expect, not_expect, rgb):

            headers = {'resource-meta': '{"client": "' + header + '"}'}
            request = (
                'place=prime&regset=0&rgb={rgb}&rearr-factors=yandexuid=1&{filter}&hid_depth=0&pp={pp}&debug=1'.format(
                    filter=filter, pp=pp, rgb=rgb
                )
            )
            request += '&rearr-factors=market_prime_promo_prun_exclude_clients={}'.format(exclude_client)
            request += '&rearr-factors=market_prime_promo_prun_pps={}'.format(include_pp)
            request += '&rearr-factors={}={}'.format(prun_flag, prun_value)
            response = self.report.request_json(request, headers=headers, strict=False)

            if expect:
                for e in expect:
                    fragment = {"debug": {"report": {"logicTrace": [Contains(e)]}}}
                    self.assertFragmentIn(response, fragment)

            if not_expect:
                for e in not_expect:
                    fragment = {"debug": {"report": {"logicTrace": [Contains(e)]}}}
                    self.assertFragmentNotIn(response, fragment)

        tests = [
            {
                'header': 'aaa',
                'pp': 18,
                'exclude_client': 'market.api,pokupki.api',
                'include_pp': '18,29',
                'filter': 'shop-promo-id=generic_bundle_usual_promo1',
                'prun_flag': 'market_prime_shop_promo_prun_count',
                'prun_value': 500,
                'expect': ["Pruning recommend no more then: 500", "optimized search for shop promos"],
                'not_expect': [],
            },
            {
                'header': 'pokupki.api',
                'pp': 18,
                'exclude_client': 'market.api,pokupki.api',
                'include_pp': '18,29',
                'filter': 'shop-promo-id=generic_bundle_usual_promo1',
                'prun_flag': 'market_prime_shop_promo_prun_count',
                'prun_value': 123,
                'expect': [],
                'not_expect': ['optimized search for'],
            },
            {
                'header': 'market.api',
                'pp': 18,
                'exclude_client': 'market.api,pokupki.api',
                'include_pp': '18,29',
                'filter': 'shop-promo-id=generic_bundle_usual_promo1',
                'prun_flag': 'market_prime_shop_promo_prun_count',
                'prun_value': 222,
                'expect': [],
                'not_expect': ['optimized search for'],
            },
            {
                'header': 'pokupki.api',
                'pp': 17,
                'exclude_client': 'market.api,pokupki.api',
                'include_pp': '18,29',
                'filter': 'shop-promo-id=generic_bundle_usual_promo1',
                'prun_flag': 'market_prime_shop_promo_prun_count',
                'prun_value': 333,
                'expect': [],
                'not_expect': ['optimized search for'],
            },
            {
                'header': 'aaa',
                'pp': 18,
                'exclude_client': 'market.api,pokupki.api',
                'include_pp': '18,29',
                'filter': 'shop-promo-id=generic_bundle_usual_promo1',
                'prun_flag': 'market_prime_parent_promo_prun_count',
                'prun_value': 444,
                'expect': [],
                'not_expect': ['optimized search for'],
            },
            {
                'header': 'aaa',
                'pp': 18,
                'exclude_client': 'market.api,pokupki.api',
                'include_pp': '18,29',
                'filter': 'shop-promo-id=generic_bundle_usual_promo1',
                'prun_flag': 'market_prime_promo_type_prun_count',
                'prun_value': 555,
                'expect': [],
                'not_expect': ['optimized search for'],
            },
            {
                'header': 'aaa',
                'pp': 18,
                'exclude_client': 'market.api,pokupki.api',
                'include_pp': '18,29',
                'filter': 'promo-type={}'.format(str(Promos.generic_bundle_expired_promo.type_name)),
                'prun_flag': 'market_prime_promo_type_prun_count',
                'prun_value': 666,
                'expect': ["Pruning recommend no more then: 666", "optimized search for parent type promos"],
                'not_expect': [],
            },
            {
                'header': 'aaa',
                'pp': 18,
                'exclude_client': 'market.api,pokupki.api',
                'include_pp': '18,29',
                'filter': 'parentPromoId={}'.format(PARENT_PROMO_ID),
                'prun_flag': 'market_prime_parent_promo_prun_count',
                'prun_value': 777,
                'expect': ["Pruning recommend no more then: 777", "optimized search for parent promos"],
                'not_expect': [],
            },
        ]

        for t in tests:
            for rgb in ('green_with_blue', 'blue'):
                run_test(
                    t['header'],
                    t['pp'],
                    t['exclude_client'],
                    t['include_pp'],
                    t['filter'],
                    t['prun_flag'],
                    t['prun_value'],
                    t['expect'],
                    t['not_expect'],
                    rgb,
                )

    def test_request_by_promo_type(self):
        promo_types_to_request = set(
            [
                'direct_discount' if promo.type_name == PromoType.PROMO_CODE else str(promo.type_name)
                for promo in Promos.all
            ]
        )
        promo_types = ",".join(promo_types_to_request)
        params = '&promo-type={}&perks={}'.format(promo_types, get_perks([PERK_CASHBACK, PERK_EXTRA_CASHBACK]))
        self.check_receives_all_offers_and_promos(params)

    def test_find_by_anaplan_promo_id(self):
        request = 'place={place}&rids=0&regset=1&pp=18&rgb={rgb}&yandexuid=1&anaplan-promo-id={anaplan_promo_id}'

        for rgb in ('green_with_blue', 'blue'):
            response = self.report.request_json(
                request.format(place='prime', rgb=rgb, anaplan_promo_id=ANAPLAN_PROMO_ID)
            )
            self.assertFragmentIn(
                response,
                {
                    'entity': 'offer',
                    'wareId': BLUE_OFFERS[OfferIds.shop_sku_blue_flash].waremd5,
                },
            )
            self.assertFragmentIn(
                response,
                {
                    'entity': 'offer',
                    'wareId': BLUE_OFFERS[OfferIds.shop_sku_blue_set_2].waremd5,
                },
            )

        for rgb in ('green_with_blue', 'blue'):
            response = self.report.request_json(
                request.format(place='stat_numbers', rgb=rgb, anaplan_promo_id=ANAPLAN_PROMO_ID)
            )
            self.assertFragmentIn(response, {'result': {'offersCount': 2}})

    def test_find_by_parent_promo_id(self):
        request = 'place={place}&rids=0&regset=1&pp=18&rgb={rgb}&yandexuid=1&parentPromoId={parent_promo_id}'

        # В акции с parent_promo_id=PARENT_PROMO_ID участвует два оффера, но у второго оффера акция блокируется
        # по причине DeclinedByPromoPriceIsGreaterThanOfferPrice
        blocked_promo_ware_md5 = BLUE_OFFERS[OfferIds.shop_sku_direct_discount_3].waremd5
        for rgb in ('green_with_blue', 'blue'):
            response = self.report.request_json(request.format(place='prime', rgb=rgb, parent_promo_id=PARENT_PROMO_ID))
            self.assertFragmentIn(
                response,
                {
                    'entity': 'offer',
                    'wareId': BLUE_OFFERS[OfferIds.shop_sku_direct_discount_parent_id].waremd5,
                    'promos': [{'key': Promos.direct_discount_with_parent_id.key}],
                },
            )
            self.assertFragmentNotIn(response, {'wareId': blocked_promo_ware_md5})

            default_offer_response = self.report.request_json(
                request.format(place='prime', rgb=rgb, parent_promo_id=PARENT_PROMO_ID)
                + '&allow-collapsing=1&use-default-offers=1'
            )
            self.assertFragmentIn(
                default_offer_response,
                {
                    'entity': 'offer',
                    'wareId': BLUE_OFFERS[OfferIds.shop_sku_direct_discount_parent_id].waremd5,
                    'promos': [{'key': Promos.direct_discount_with_parent_id.key}],
                },
            )
            self.assertFragmentNotIn(default_offer_response, {'wareId': blocked_promo_ware_md5})

            promo_declined_request = (
                'place=prime&rids=0&regset=1&pp=18&rgb={rgb}&yandexuid=1&offerid={ware_md5}&debug=1'
            )
            promo_declined_request += '&rearr-factors=market_documents_search_trace={ware_md5}'
            promo_declined_response = self.report.request_json(
                promo_declined_request.format(rgb=rgb, ware_md5=blocked_promo_ware_md5)
            )
            self.assertFragmentIn(
                promo_declined_response,
                {
                    'debug': {
                        'docs_search_trace': {
                            'traces': [
                                {
                                    'promos': [
                                        {
                                            'promoKey': 'DIRECT_DISCOUNT_PARENT_ID',
                                            'promoState': 'DeclinedByPromoPriceIsGreaterThanOfferPrice',
                                            'parentPromoId': PARENT_PROMO_ID,
                                        },
                                    ],
                                },
                            ],
                        }
                    }
                },
            )

        # place=stat_numbers не фильтрует акцию, заблокированную по причине DeclinedByPromoPriceIsGreaterThanOfferPrice
        for rgb in ('green_with_blue', 'blue'):
            response = self.report.request_json(
                request.format(place='stat_numbers', rgb=rgb, parent_promo_id=PARENT_PROMO_ID)
            )
            self.assertFragmentIn(response, {'result': {'offersCount': 2}})

    def test_find_by_default_parent_promo(self):
        """
        Тестируем, что при добавлении параметра default-parent-promo=1, офферы фильтруются по текущей активной родительской акции
        Также проверяем, что в блоке filters в ответе приходит фильтр default-parent-promo
        """
        request = 'place={place}&rids=0&regset=1&pp=18&rgb={rgb}&yandexuid=1&filter=default-parent-promo:{default_parent_promo}'

        for rgb in ('green_with_blue', 'blue'):
            response = self.report.request_json(request.format(place='prime', rgb=rgb, default_parent_promo=1))
            self.assertFragmentIn(
                response,
                {
                    'entity': 'offer',
                    'wareId': BLUE_OFFERS[OfferIds.shop_sku_direct_discount_parent_id_2].waremd5,
                    'promos': [{'key': Promos.direct_discount_with_parent_id_2.key}],
                },
            )
            self.assertFragmentIn(
                response,
                {
                    'filters': [
                        {
                            'id': 'default-parent-promo',
                            'name': PARENT_PROMO_NAME_2,
                            'type': 'boolean',
                            'values': [
                                {
                                    'found': 0,
                                    'value': '0',
                                },
                                {
                                    'checked': True,
                                    'found': 1,  # Счетчик не увеличился, так как промо direct_discount_disabled_with_parent_id неактивно
                                    'value': '1',
                                },
                            ],
                        }
                    ],
                },
            )

        for rgb in ('green_with_blue', 'blue'):
            response = self.report.request_json(
                request.format(place='prime', rgb=rgb, default_parent_promo=0)
                + '&shop-promo-id={}'.format(Promos.direct_discount_with_parent_id_2.shop_promo_id)
            )
            self.assertFragmentIn(
                response,
                {
                    'entity': 'offer',
                    'wareId': BLUE_OFFERS[OfferIds.shop_sku_direct_discount_parent_id_2].waremd5,
                    'promos': [{'key': Promos.direct_discount_with_parent_id_2.key}],
                },
            )
            self.assertFragmentIn(
                response,
                {
                    'filters': [
                        {
                            'id': 'default-parent-promo',
                            'name': PARENT_PROMO_NAME_2,
                            'type': 'boolean',
                            'values': [
                                {
                                    'checked': True,
                                    'found': 0,
                                    'value': '0',
                                },
                                {
                                    'found': 1,
                                    'value': '1',
                                },
                            ],
                        }
                    ],
                },
            )

        # Запрос с флагом будущего. В ответе должна быть акция со следующего инфоповода
        future_time = datetime.fromtimestamp(REQUEST_TIMESTAMP) + timedelta(days=1)
        future_request = request + '&rearr-factors=market_promo_datetime={time}'
        for rgb in ('green_with_blue', 'blue'):
            response = self.report.request_json(
                future_request.format(place='prime', rgb=rgb, default_parent_promo=1, time=future_time.isoformat())
            )
            self.assertFragmentIn(
                response,
                {
                    'entity': 'offer',
                    'wareId': BLUE_OFFERS[OfferIds.shop_sku_direct_discount_time_shift].waremd5,
                    'promos': [{'key': Promos.direct_discount_time_shift.key}],
                },
            )
            self.assertFragmentIn(
                response,
                {
                    'filters': [
                        {
                            'id': 'default-parent-promo',
                            'name': PARENT_PROMO_NAME_3,
                            'type': 'boolean',
                            'values': [
                                {
                                    'found': 0,
                                    'value': '0',
                                },
                                {
                                    'checked': True,
                                    'found': 1,
                                    'value': '1',
                                },
                            ],
                        }
                    ],
                },
            )

        # place=stat_numbers не учитывает статус акций
        for rgb in ('green_with_blue', 'blue'):
            response = self.report.request_json(request.format(place='stat_numbers', rgb=rgb, default_parent_promo=1))
            self.assertFragmentIn(response, {'result': {'offersCount': 2}})

    def test_disable_default_parent_promo(self):
        """
        Тестируем отключение фильтра при отсутствии товаров по родительской акции
        """
        request = 'place={place}&rids=0&regset=1&pp=18&yandexuid=1&hid={hid}'
        response = self.report.request_json(request.format(place='prime', hid=HID_1))
        self.assertFragmentIn(
            response,
            [
                {
                    'entity': 'offer',
                    'wareId': BLUE_OFFERS[OfferIds.shop_sku_no_promo_price_filter].waremd5,
                },
                {
                    'entity': 'offer',
                    'wareId': BLUE_OFFERS[OfferIds.shop_sku_direct_discount_parent_id_2].waremd5,
                },
            ],
        )
        self.assertFragmentIn(
            response,
            {
                'filters': [
                    {
                        'id': 'default-parent-promo',
                        'name': PARENT_PROMO_NAME_2,
                        'type': 'boolean',
                        'values': [
                            {
                                'checked': True,
                                'found': 3,
                                'value': '0',
                            },
                            {
                                'found': 1,
                                'value': '1',
                            },
                        ],
                    }
                ],
            },
        )

        hid_request = 'place={place}&rids=0&regset=1&pp=18&yandexuid=1&hid={hid}&mcpriceto=489'
        response = self.report.request_json(hid_request.format(place='prime', hid=HID_1))
        self.assertFragmentIn(
            response,
            [
                {
                    'entity': 'offer',
                    'wareId': BLUE_OFFERS[OfferIds.shop_sku_no_promo_price_filter].waremd5,
                },
            ],
        )
        self.assertFragmentNotIn(
            response,
            [
                {
                    'entity': 'offer',
                    'wareId': BLUE_OFFERS[OfferIds.shop_sku_direct_discount_parent_id_2].waremd5,
                },
            ],
        )
        self.assertFragmentIn(
            response,
            {
                'filters': [
                    {
                        'id': 'default-parent-promo',
                        'name': PARENT_PROMO_NAME_2,
                        'type': 'boolean',
                        'values': [
                            {
                                'checked': True,
                                'found': 2,
                                'value': '0',
                            },
                            {
                                'found': 0,
                                'value': '1',
                            },
                        ],
                    }
                ],
            },
        )

    def test_unknown_promo_id(self):
        # если заданного shop_promo_id нет в наличии (например, ещё не доехал до репорта), то надо выдавать 0 офферов, а не 400 ошибку
        for rgb in ('green_with_blue', 'blue'):
            request = 'place=prime&regset=0&pp=18&rgb={rgb}&shop-promo-id=НЕТУ🔍!'
            response = self.report.request_json(request.format(rgb=rgb))
            self.assertFragmentIn(
                response,
                {
                    'search': {
                        'totalOffers': 0,
                    }
                },
            )

    def test_default_offer_with_promo(self):
        '''
        Проверяем, что при запросе с &shop-promo-id&allow-collapsing=1&use-default-offers=1 у всех
        дефолтных офферов есть нужная акция из параметра &shop-promo-id
        '''
        for rgb in ('green_with_blue', 'blue'):
            request = 'place={place}&rids=0&regset=1&pp=18&rgb={rgb}&allow-collapsing=1&use-default-offers=1&yandexuid=1&shop-promo-id={shop_promo_id}'
            target_shop_promo_id = 'direct_discount_shop_promo_id'

            response = self.report.request_json(
                request.format(place='prime', rgb=rgb, shop_promo_id=target_shop_promo_id)
            )
            offers = [OfferIds.shop_sku_direct_discount_1, OfferIds.shop_sku_direct_discount_2]
            for offer in offers:
                self.assertFragmentIn(
                    response,
                    {
                        'entity': 'offer',
                        'wareId': BLUE_OFFERS[offer].waremd5,
                        'promos': [
                            {
                                'shopPromoId': target_shop_promo_id,
                            },
                        ],
                    },
                )

        for rgb in ('green_with_blue', 'blue'):
            response = self.report.request_json(
                request.format(place='stat_numbers', rgb=rgb, shop_promo_id=target_shop_promo_id)
            )
            self.assertFragmentIn(response, {'result': {'offersCount': 2}})

    def test_default_offer_with_parent_promo(self):
        '''
        Проверяем, что при запросе с &parentPromoId&use-default-offers=1 выбирается дефолтный оффер с активной акцией
        '''
        request = 'place={place}&rids=0&regset=1&pp=7&rgb={rgb}&use-default-offers=1&yandexuid=1&parentPromoId={parent_promo_id}'
        for rgb in ('green_with_blue', 'blue'):
            # Кэшбек выключен. Должен показываться оффер с прямой скидкой, даже если он хуже
            response = self.report.request_json(
                request.format(place='prime', rgb=rgb, parent_promo_id=PARENT_PROMO_ID_1)
            )
            self.assertFragmentIn(
                response,
                {
                    'entity': 'product',
                    'type': 'model',
                    'id': HYPERID_2,
                    'offers': {
                        'items': [
                            {
                                'entity': 'offer',
                                'wareId': BLUE_OFFERS[OfferIds.shop_sku_direct_discount_same_model].waremd5,
                                'promos': [
                                    {
                                        'key': Promos.direct_discount_same_model.key,
                                    },
                                ],
                            }
                        ]
                    },
                },
            )

            # Включаем кэшбек. Оффер с кэшбеком должен выигрывать, так как он более выгодный и имеет тот же parentPromoId
            perk_request = request + '&perks={perks}'
            response = self.report.request_json(
                perk_request.format(
                    place='prime',
                    rgb=rgb,
                    parent_promo_id=PARENT_PROMO_ID_1,
                    perks=get_perks([PERK_CASHBACK, PERK_EXTRA_CASHBACK]),
                )
            )
            self.assertFragmentIn(
                response,
                {
                    'entity': 'product',
                    'type': 'model',
                    'id': HYPERID_2,
                    'offers': {
                        'items': [
                            {
                                'entity': 'offer',
                                'wareId': BLUE_OFFERS[OfferIds.shop_sku_cashback_with_parent_id].waremd5,
                                'promos': [
                                    {
                                        'key': Promos.cashback_with_parent_id.key,
                                        'parentPromoId': PARENT_PROMO_ID_1,
                                    },
                                ],
                            }
                        ]
                    },
                },
            )

        for rgb in ('green_with_blue', 'blue'):
            response = self.report.request_json(
                request.format(place='stat_numbers', rgb=rgb, parent_promo_id=PARENT_PROMO_ID_1)
            )
            self.assertFragmentIn(response, {'result': {'offersCount': 2}})


if __name__ == '__main__':
    main()
