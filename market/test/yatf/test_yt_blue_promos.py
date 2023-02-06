# -*- coding: utf-8 -*-

from datetime import (
    datetime,
    timedelta
)
import pytest
import pytz
import time

from market.proto.common.promo_pb2 import ESourceType
from market.pylibrary.const.offer_promo import PromoType
from market.idx.promos.yt_promo_indexer.yatf.test_env import YtPromoIndexerTestEnv
from market.idx.promos.yt_promo_indexer.yatf.resources import YtCollectedPromoDetailsTable
from utils import compare_promo_details_collections
from market.idx.yatf.resources.yt_stuff_resource import (
    get_yt_prefix
)
from yt.wrapper import ypath_join

from market.pylibrary.const.payment_methods import PaymentMethod

DT_NOW = datetime.now(tz=pytz.timezone('Europe/Moscow'))
DT_DELTA = timedelta(hours=24)
MMAP_NAME = 'yt_promo_details_generic_bundle.mmap'
FEED_ID = 777
FEED_ID2 = 888


def to_timestamp(dt):
    return int(time.mktime(dt.timetuple()))


@pytest.fixture(
    params=[{
        'valid_yt_promos': [
            {
                'feed_id': FEED_ID,
                'type': PromoType.GENERIC_BUNDLE,
                'shop_promo_id': 'generic_bundle_1',
                'start_date': to_timestamp(DT_NOW - DT_DELTA),
                'end_date': to_timestamp(DT_NOW + DT_DELTA),
                'allowed_payment_methods': PaymentMethod.PT_MMAP_ALL,
                'disabled_by_default': True,
                'generic_bundle': {
                    'bundles_content': [
                        {
                            'primary_item': {'offer_id': 'gb_offer1', 'count': 4},
                            'secondary_item': {
                                'item': {'offer_id': 'gb_offer2', 'count': 1},
                                'discount_price': {'value': 77, 'currency': 'RUB'}
                            },
                            'spread_discount': 34.17,
                        },
                        {
                            'primary_item': {'offer_id': 'gb_offer3', 'count': 1},
                            'secondary_item': {
                                'item': {'offer_id': 'gb_offer4', 'count': 5},
                                'discount_price': {'value': 20, 'currency': 'RUB'}
                            }
                        }
                    ],
                    'restrict_refund': True,
                    'spread_discount': 55.77,
                    'allow_berubonus': True,
                    'allow_promocode': False,
                },
            },
            {
                'feed_id': 0,
                'type': PromoType.CHEAPEST_AS_GIFT,
                'shop_promo_id': 'cheapest_as_gift_1',
                'start_date': to_timestamp(DT_NOW - DT_DELTA),
                'end_date': to_timestamp(DT_NOW + DT_DELTA),
                'allowed_payment_methods': PaymentMethod.PT_MMAP_ALL,
                'url': 'http://localhost.ru',
                'landing_url': 'https://landing0.url',
                'conditions': 'some conditions',
                'source_type': ESourceType.ROBOT,
                'source_reference': 'http://source_reference0.com',
                'cheapest_as_gift': {
                    'feed_offer_ids': [
                        {'feed_id': FEED_ID, 'offer_id': 'cag_offer1'},
                        {'feed_id': 3, 'offer_id': 'cag_offer2'},
                        {'feed_id': FEED_ID, 'offer_id': 'cag_offer_три'},
                    ],
                    'count': 77,
                    'promo_url': 'some_url',
                    'link_text': 'text is here',
                    'allow_berubonus': True,
                    'allow_promocode': False,
                },
            },
            {
                'feed_id': 0,
                'type': PromoType.CHEAPEST_AS_GIFT,
                'shop_promo_id': '#4321',
                'start_date': to_timestamp(DT_NOW - DT_DELTA),
                'end_date': to_timestamp(DT_NOW + DT_DELTA),
                'allowed_payment_methods': PaymentMethod.PT_MMAP_ALL,
                'url': 'http://localhost.ru',
                'landing_url': '',
                'cheapest_as_gift': {
                    'feed_offer_ids': [
                        {'feed_id': FEED_ID, 'offer_id': 'cag_offer_4001'},
                        {'feed_id': 3, 'offer_id': 'cag_offer_4002'},
                        {'feed_id': FEED_ID, 'offer_id': 'cag_offer_4003'},
                    ],
                    'count': 3,
                    'promo_url': '',
                    'link_text': 'text is here',
                    'allow_berubonus': False,
                    'allow_promocode': False,
                },
            },
            {
                'feed_id': FEED_ID,
                'type': PromoType.BLUE_FLASH,
                'shop_promo_id': 'blue_flash_1',
                'start_date': to_timestamp(DT_NOW - DT_DELTA),
                'end_date': to_timestamp(DT_NOW + DT_DELTA),
                'allowed_payment_methods': PaymentMethod.PT_MMAP_ALL,
                'url': '',
                'landing_url': 'http://landing1.url',
                'blue_flash': {
                    'items': [
                        {'price': {'value': 100500, 'currency': 'RUB'}, 'offer': {'feed_id': FEED_ID, 'offer_id': 'bf_offer1'}},
                        {'price': {'value': 33333, 'currency': 'RUB'}, 'offer': {'feed_id': FEED_ID, 'offer_id': 'bf_offer2'}},
                    ],
                    'allow_berubonus': True,
                    'allow_promocode': True,
                },
            },
            {
                'feed_id': 0,
                'type': PromoType.BLUE_FLASH,
                'shop_promo_id': 'BLUE_FLASH 2',
                'start_date': to_timestamp(DT_NOW - DT_DELTA),
                'end_date': to_timestamp(DT_NOW + DT_DELTA),
                'allowed_payment_methods': PaymentMethod.PT_MMAP_ALL,
                'landing_url': 'http://landing2.url',
                'blue_flash': {
                    'items': [
                        {'price': {'value': 100500, 'currency': 'RUB'}, 'offer': {'feed_id': FEED_ID, 'offer_id': 'bf_offer3'}},
                        {'price': {'value': 33333, 'currency': 'RUB'}, 'offer': {'feed_id': FEED_ID2, 'offer_id': 'bf_offer4'}},
                    ],
                    'allow_berubonus': False,
                    'allow_promocode': True,
                },
            },
            {
                'feed_id': FEED_ID,
                'type': PromoType.BLUE_SET,
                'shop_promo_id': 'blue_set_1',
                'start_date': to_timestamp(DT_NOW - DT_DELTA),
                'end_date': to_timestamp(DT_NOW + DT_DELTA),
                'allowed_payment_methods': PaymentMethod.PT_MMAP_ALL,
                'blue_set': {
                    'sets_content': [{
                        'items': [
                            {'offer_id': 'bs_offer1', 'count': 1, 'discount': 7.7},
                            {'offer_id': 'bs_offer2', 'count': 8},
                        ],
                        'linked': True,
                    }],
                    'restrict_refund': True,
                    'allow_berubonus': False,
                    'allow_promocode': False,
                },
                'restrictions': {
                    'predicates': [{
                        'perks': ['beru_plus', 'yandex_cashback'],
                        'at_supplier_warehouse': True,
                        'loyalty_program_status': 1,
                        'delivery_partner_types': [1, 2],
                    }]
                }
            },
            {
                'feed_id': 0,
                'type': PromoType.DIRECT_DISCOUNT,
                'shop_promo_id': 'direct_discount_1',
                'start_date': to_timestamp(DT_NOW - DT_DELTA),
                'end_date': to_timestamp(DT_NOW + DT_DELTA),
                'allowed_payment_methods': PaymentMethod.PT_MMAP_ALL,
                'direct_discount': {
                    'items': [
                        {
                            'feed_id': FEED_ID,
                            'offer_id': 'dd_offer12',
                        },
                        {
                            'feed_id': FEED_ID,
                            'offer_id': 'dd_offer1',
                            # YT table exports prices for DirectDiscount promo with 7 digit precision
                            'old_price': {'value': 12345 * 10**7, 'currency': 'RUR'},
                            'discount_price': {'value': 1234 * 10**7, 'currency': 'RUR'},
                        },
                    ],
                    'allow_berubonus': True,
                    'allow_promocode': True,
                },
            },
            {
                'feed_id': 0,
                'type': PromoType.DIRECT_DISCOUNT,
                'shop_promo_id': 'direct_discount_2',
                'start_date': to_timestamp(DT_NOW - DT_DELTA),
                'end_date': to_timestamp(DT_NOW + DT_DELTA),
                'allowed_payment_methods': PaymentMethod.PT_MMAP_ALL,
                'direct_discount': {
                    'items': [
                        {
                            'feed_id': FEED_ID,
                            'offer_id': 'dd_offer2',
                            # YT table exports prices for DirectDiscount promo with 7 digit precision
                            'old_price': {'value': 12346 * 10**7, 'currency': 'RUR'},
                            'discount_price': {'value': 1235 * 10**7, 'currency': 'RUR'},
                        },
                    ],
                    'discounts_by_category': [
                        {
                            'category_restriction': {
                                'categories': [
                                    100, 101, 102,
                                ],
                            },
                            'discount_percent': 10.7,
                        },
                        {
                            'category_restriction': {
                                'categories': [
                                    300, 301,
                                ],
                            },
                            'discount_percent': 16.2,
                        },
                    ],
                    'allow_berubonus': True,
                    'allow_promocode': True,
                },
            },
            {
                'feed_id': 0,
                'type': PromoType.DIRECT_DISCOUNT,
                'shop_promo_id': 'direct_discount_3',
                'start_date': to_timestamp(DT_NOW - DT_DELTA),
                'end_date': to_timestamp(DT_NOW + DT_DELTA),
                'allowed_payment_methods': PaymentMethod.PT_MMAP_ALL,
                'direct_discount': {
                    'items': [
                        {
                            'feed_id': FEED_ID,
                            'offer_id': 'dd_offer3',
                            'discount_percent': 7.7,
                        },
                    ],
                    'allow_berubonus': True,
                    'allow_promocode': True,
                },
            },
            {
                'feed_id': 0,
                'type': PromoType.SPREAD_DISCOUNT_COUNT,
                'shop_promo_id': 'spread_discount_count_1',
                'start_date': to_timestamp(DT_NOW - DT_DELTA),
                'end_date': to_timestamp(DT_NOW + DT_DELTA),
                'allowed_payment_methods': PaymentMethod.PT_MMAP_ALL,
                'spread_discount_count': {
                    'discount_items': [{
                        'msku': 83240,
                        'count_bounds': [
                            {'count': 7, 'percent_discount': 2.3},
                            {'count': 9, 'percent_discount': 2.4},
                        ],
                    }],
                },
            },
            {
                'feed_id': 0,
                'type': PromoType.SPREAD_DISCOUNT_RECEIPT,
                'shop_promo_id': 'spread_discount_receipt_1',
                'start_date': to_timestamp(DT_NOW - DT_DELTA),
                'end_date': to_timestamp(DT_NOW + DT_DELTA),
                'allowed_payment_methods': PaymentMethod.PT_MMAP_ALL,
                'spread_discount_receipt': {
                    'receipt_bounds': [
                        {
                            'discount_price': {'value': 17, 'currency': 'RUR'},
                            'percent_discount': 2.3,
                        },
                        {
                            'discount_price': {'value': 23, 'currency': 'RUR'},
                            'absolute_discount': {'value': 3, 'currency': 'RUR'},
                        }
                    ],
                },
            },
        ],
        'valid_yt_promos_id': [
            {
                'feed_id': FEED_ID,
                'type': PromoType.GENERIC_BUNDLE,
                'shop_promo_id': 'abcdefghijklmnopqrstuvwxy',
                'start_date': to_timestamp(DT_NOW - DT_DELTA),
                'end_date': to_timestamp(DT_NOW + DT_DELTA),
                'allowed_payment_methods': PaymentMethod.PT_MMAP_ALL,
                'disabled_by_default': True,
                'generic_bundle': {
                    'bundles_content': [
                        {
                            'primary_item': {'offer_id': 'gb_offer1', 'count': 4},
                            'secondary_item': {
                                'item': {'offer_id': 'gb_offer2', 'count': 1},
                                'discount_price': {'value': 77, 'currency': 'RUB'}
                            },
                            'spread_discount': 34.17,
                        },
                        {
                            'primary_item': {'offer_id': 'gb_offer3', 'count': 1},
                            'secondary_item': {
                                'item': {'offer_id': 'gb_offer4', 'count': 5},
                                'discount_price': {'value': 20, 'currency': 'RUB'}
                            }
                        }
                    ],
                    'restrict_refund': True,
                    'spread_discount': 55.77,
                    'allow_berubonus': True,
                    'allow_promocode': False,
                },
            },
            {
                'feed_id': 0,
                'type': PromoType.DIRECT_DISCOUNT,
                'shop_promo_id': 'z0123456789-_',
                'start_date': to_timestamp(DT_NOW - DT_DELTA),
                'end_date': to_timestamp(DT_NOW + DT_DELTA),
                'allowed_payment_methods': PaymentMethod.PT_MMAP_ALL,
                'direct_discount': {
                    'items': [
                        {
                            'feed_id': FEED_ID,
                            'offer_id': 'dd_offer1',
                            # YT table exports prices for DirectDiscount promo with 7 digit precision
                            'old_price': {'value': 12345 * 10**7, 'currency': 'RUR'},
                            'discount_price': {'value': 1234 * 10**7, 'currency': 'RUR'},
                        },
                        {
                            'feed_id': FEED_ID,
                            'offer_id': 'dd_offer12',
                        },
                    ],
                    'allow_berubonus': True,
                    'allow_promocode': True,
                },
            },
        ],
        'invalid_yt_promos_id': [
            {
                'feed_id': FEED_ID,
                'type': PromoType.GENERIC_BUNDLE,
                'shop_promo_id': 'very_long_shop_promo_id___',
                'start_date': to_timestamp(DT_NOW - DT_DELTA),
                'end_date': to_timestamp(DT_NOW + DT_DELTA),
                'allowed_payment_methods': PaymentMethod.PT_MMAP_ALL,
                'disabled_by_default': True,
                'generic_bundle': {
                    'bundles_content': [
                        {
                            'primary_item': {'offer_id': 'gb_offer1', 'count': 4},
                            'secondary_item': {
                                'item': {'offer_id': 'gb_offer2', 'count': 1},
                                'discount_price': {'value': 77, 'currency': 'RUB'}
                            },
                            'spread_discount': 34.17,
                        },
                        {
                            'primary_item': {'offer_id': 'gb_offer3', 'count': 1},
                            'secondary_item': {
                                'item': {'offer_id': 'gb_offer4', 'count': 5},
                                'discount_price': {'value': 20, 'currency': 'RUB'}
                            }
                        }
                    ],
                    'restrict_refund': True,
                    'spread_discount': 55.77,
                    'allow_berubonus': True,
                    'allow_promocode': False,
                },
            },
            {
                'feed_id': 0,
                'type': PromoType.DIRECT_DISCOUNT,
                'shop_promo_id': 'iNvalid_promo_id',
                'start_date': to_timestamp(DT_NOW - DT_DELTA),
                'end_date': to_timestamp(DT_NOW + DT_DELTA),
                'allowed_payment_methods': PaymentMethod.PT_MMAP_ALL,
                'direct_discount': {
                    'items': [
                        {
                            'feed_id': FEED_ID,
                            'offer_id': 'dd_offer1',
                            # YT table exports prices for DirectDiscount promo with 7 digit precision
                            'old_price': {'value': 12345 * 10**7, 'currency': 'RUR'},
                            'discount_price': {'value': 1234 * 10**7, 'currency': 'RUR'},
                        },
                        {
                            'feed_id': FEED_ID,
                            'offer_id': 'dd_offer12',
                        },
                    ],
                    'allow_berubonus': True,
                    'allow_promocode': True,
                },
            },
            {
                'feed_id': 0,
                'type': PromoType.CHEAPEST_AS_GIFT,
                'shop_promo_id': 'Кириллический-промо',
                'start_date': to_timestamp(DT_NOW - DT_DELTA),
                'end_date': to_timestamp(DT_NOW + DT_DELTA),
                'allowed_payment_methods': PaymentMethod.PT_MMAP_ALL,
                'cheapest_as_gift': {
                    'feed_offer_ids': [
                        {'feed_id': FEED_ID, 'offer_id': 'cag_offer1'},
                        {'feed_id': 3, 'offer_id': 'cag_offer2'},
                        {'feed_id': FEED_ID, 'offer_id': 'cag_offer_три'},
                    ],
                    'count': 77,
                    'promo_url': 'some_url',
                    'link_text': 'text is here',
                    'allow_berubonus': True,
                    'allow_promocode': False,
                },
            },
        ]
    }],
    scope='module'
)
def collected_promo_details(request):
    return request.param


@pytest.yield_fixture(scope='module')
def valid_promo_workflow(collected_promo_details, yt_server):
    table_name = ypath_join(get_yt_prefix(), 'promos', 'collected_promo_details', 'valid')
    resources = {
        'collected_promo_details_table': YtCollectedPromoDetailsTable(
            yt_stuff=yt_server,
            path=table_name,
            data=collected_promo_details['valid_yt_promos']
        )
    }
    with YtPromoIndexerTestEnv(yt_server, **resources) as test_env:
        test_env.execute(mmap_name=MMAP_NAME)
        test_env.verify()
        yield test_env


def test_yt_valid_record(valid_promo_workflow, collected_promo_details):
    compare_promo_details_collections(
        valid_promo_workflow.yt_promo_details,
        collected_promo_details['valid_yt_promos']
    )
