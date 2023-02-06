# -*- coding: utf-8 -*-

from datetime import (
    datetime,
    timedelta
)
import pytest
import pytz
import time

from market.pylibrary.const.offer_promo import PromoType
from market.idx.promos.yt_promo_indexer.yatf.test_env import YtPromoIndexerTestEnv
from market.idx.promos.yt_promo_indexer.yatf.resources import (
    YtCollectedPromoDetailsTableSplit,
)
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


def to_timestamp(dt):
    return int(time.mktime(dt.timetuple()))


def reorder_promo_items(promo_details):
    # промо-заглушки GENERIC_BUNDLE_SECONDARY надо удалить
    return list([p for p in promo_details if p['type'] != PromoType.GENERIC_BUNDLE_SECONDARY])


# Список пар {part_id, promo}
# Две акции. Одна состоит из двух частей, другая из одной части.
@pytest.fixture(scope='module')
def collected_promo_details(request):
    return [
        [
            0,
            {
                'feed_id': FEED_ID,
                'type': PromoType.DIRECT_DISCOUNT,
                'url': 'http://dd_url.com',
                'landing_url': 'http://dd_url_landing.com',
                'shop_promo_id': 'dd_shop_promo_id',
                'start_date': to_timestamp(DT_NOW - DT_DELTA),
                'end_date': to_timestamp(DT_NOW + DT_DELTA),
                'allowed_payment_methods': PaymentMethod.PT_MMAP_ALL,
                'direct_discount': {
                    'items': [
                        {
                            'feed_id': FEED_ID,
                            'offer_id': 'offer_id1',
                            'discount_price': {'value': 1234 * 10**7, 'currency': 'RUR'},
                            'old_price': {'value': 12345 * 10**7, 'currency': 'RUR'},
                        },
                    ],
                    'allow_berubonus': True,
                    'allow_promocode': True,
                }
            },
        ],
        [
            1,
            {
                'feed_id': FEED_ID,
                'type': PromoType.DIRECT_DISCOUNT,
                'url': 'http://dd_url.com',
                'landing_url': 'http://dd_url_landing.com',
                'shop_promo_id': 'dd_shop_promo_id',
                'start_date': to_timestamp(DT_NOW - DT_DELTA),
                'end_date': to_timestamp(DT_NOW + DT_DELTA),
                'allowed_payment_methods': PaymentMethod.PT_MMAP_ALL,
                'direct_discount': {
                    'items': [
                        {
                            'feed_id': FEED_ID,
                            'offer_id': 'offer_id2',
                            'discount_price': {'value': 2234 * 10**7, 'currency': 'RUR'},
                            'old_price': {'value': 22345 * 10**7, 'currency': 'RUR'},
                        },
                    ],
                    'allow_berubonus': True,
                    'allow_promocode': True,
                }
            },
        ],
        [
            0,
            {
                'feed_id': FEED_ID,
                'type': PromoType.DIRECT_DISCOUNT,
                'url': 'http://dd_2_url.com',
                'landing_url': 'http://dd_2_url_landing.com',
                'shop_promo_id': 'dd_2_shop_promo_id',
                'start_date': to_timestamp(DT_NOW - DT_DELTA),
                'end_date': to_timestamp(DT_NOW + DT_DELTA),
                'allowed_payment_methods': PaymentMethod.PT_MMAP_ALL,
                'direct_discount': {
                    'items': [
                        {
                            'feed_id': FEED_ID,
                            'offer_id': 'offer_id3',
                            'discount_price': {'value': 3234 * 10**7, 'currency': 'RUR'},
                            'old_price': {'value': 32345 * 10**7, 'currency': 'RUR'},
                        },
                    ],
                    'allow_berubonus': True,
                    'allow_promocode': True,
                }
            },
        ],
    ]


@pytest.yield_fixture(scope='module')
def workflow(collected_promo_details, yt_server):
    collected_table_name = ypath_join(get_yt_prefix(), 'promos', 'collected_promo_details')
    resources = {
        'collected_promo_details_table': YtCollectedPromoDetailsTableSplit(
            yt_stuff=yt_server,
            path=collected_table_name,
            data=collected_promo_details,
        )
    }

    with YtPromoIndexerTestEnv(yt_server, **resources) as test_env:
        test_env.execute(mmap_name=MMAP_NAME)
        test_env.verify()
        yield test_env


def test_collected_promo_details(workflow, collected_promo_details):

    collected_promos = [p[1] for p in collected_promo_details]
    merged_promo = None
    promos = []
    for idx, promo in enumerate(collected_promos):
        if idx == 0:
            merged_promo = promo
        elif idx == 1:
            for item in promo['direct_discount']['items']:
                merged_promo['direct_discount']['items'].append(item)
        elif idx == 2:
            promos.append(promo)
    promos.append(merged_promo)

    compare_promo_details_collections(
        reorder_promo_items(workflow.yt_promo_details),
        reorder_promo_items(promos)
    )
