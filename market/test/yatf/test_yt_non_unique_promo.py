# -*- coding: utf-8 -*-

from datetime import (
    datetime,
    timedelta
)
import time
import pytest
import pytz

from market.pylibrary.const.offer_promo import (
    PromoType,
)
from market.proto.common.promo_pb2 import ESourceType
from market.idx.promos.yt_promo_indexer.yatf.test_env import YtPromoIndexerTestEnv
from market.idx.promos.yt_promo_indexer.yatf.resources import YtCollectedPromoDetailsTable
from utils import compare_promo_details_collections
from market.idx.yatf.resources.yt_stuff_resource import (
    get_yt_prefix
)
from yt.wrapper import ypath_join

from market.pylibrary.const.payment_methods import (
    PaymentMethod,
)

DT_NOW = datetime.now(tz=pytz.timezone('Europe/Moscow'))
DT_DELTA = timedelta(hours=24)
MMAP_NAME = 'yt_promo_details_generic_bundle.mmap'
FEED_ID = 777


def to_timestamp(dt):
    return int(time.mktime(dt.timetuple()))


@pytest.fixture(scope='module')
def collected_promo_details(request):
    return [
        {
            'feed_id': FEED_ID,
            'type': PromoType.GENERIC_BUNDLE,
            'shop_promo_id': 'generic_bundle_1',
            'start_date': to_timestamp(DT_NOW - DT_DELTA),
            'end_date': to_timestamp(DT_NOW + DT_DELTA),
            'allowed_payment_methods': PaymentMethod.PT_MMAP_ALL,
            'url': 'http://generic_bundle.ru',
            'landing_url': 'https://generic_bundle.url/landing',
            'source_type': ESourceType.ROBOT,
            'generic_bundle': {
                'bundles_content': [
                    {
                        'primary_item': {'offer_id': 'same_offer_id', 'count': 4},
                        'secondary_item': {
                            'item': {'offer_id': 'secondary_offer2', 'count': 1},
                            'discount_price': {'value': 77, 'currency': 'RUB'}
                        },
                        'spread_discount': 34.17,
                    },
                ],
                'restrict_refund': True,
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
            'source_type': ESourceType.ROBOT,
            'source_reference': 'http://source_reference0.com',
            'cheapest_as_gift': {
                'feed_offer_ids': [
                    {'feed_id': FEED_ID, 'offer_id': 'same_offer_id'},
                ],
                'count': 3,
                'promo_url': 'some_url',
                'link_text': 'text is here',
                'allow_berubonus': True,
                'allow_promocode': False,
            },
        },
    ]


@pytest.yield_fixture(scope='module')
def non_unique_promo_workflow(collected_promo_details, yt_server):
    table_name = ypath_join(get_yt_prefix(), 'promos', 'collected_promo_details')
    resources = {
        'collected_promo_details_table': YtCollectedPromoDetailsTable(
            yt_stuff=yt_server,
            path=table_name,
            data=collected_promo_details
        ),
    }
    with YtPromoIndexerTestEnv(yt_server, **resources) as test_env:
        test_env.execute(mmap_name=MMAP_NAME)
        test_env.verify()
        yield test_env


def test_non_unique_promo(non_unique_promo_workflow, collected_promo_details):
    # промо-заглушки GENERIC_BUNDLE_SECONDARY надо удалить
    promo_details_list = non_unique_promo_workflow.yt_promo_details
    tmp = list([p for p in promo_details_list if p['type'] != PromoType.GENERIC_BUNDLE_SECONDARY])

    compare_promo_details_collections(
        tmp,
        collected_promo_details
    )
