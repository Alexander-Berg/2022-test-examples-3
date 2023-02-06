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
            'type': PromoType.DIRECT_DISCOUNT,
            'url': 'http://dd_subsidy.com',
            'landing_url': 'http://dd_subsidy_landing.com',
            'shop_promo_id': 'dd_subsidy',
            'start_date': to_timestamp(DT_NOW - DT_DELTA),
            'end_date': to_timestamp(DT_NOW + DT_DELTA),
            'allowed_payment_methods': PaymentMethod.PT_MMAP_ALL,
            'direct_discount': {
                'items': [
                    {
                        'feed_id': FEED_ID,
                        'offer_id': 'dd_subsidy_offer',
                        'discount_price': {'value': 1234 * 10**7, 'currency': 'RUR'},
                        'old_price': {'value': 12345 * 10**7, 'currency': 'RUR'},
                        'subsidy': {'value': 111 * 10**7, 'currency': 'RUR'},
                        'max_discount': {'value': 123 * 10**7, 'currency': 'RUR'},
                        'max_discount_percent': 14.6,
                    },
                ],
                'budget_limit': {'value': 222 * 10**7, 'currency': 'RUR'},
                'allow_berubonus': True,
                'allow_promocode': True,
            },
        },
    ]


@pytest.yield_fixture(scope='module')
def workflow(collected_promo_details, yt_server):
    table_name = ypath_join(get_yt_prefix(), 'promos', 'collected_promo_details')
    resources = {
        'collected_promo_details_table': YtCollectedPromoDetailsTable(
            yt_stuff=yt_server,
            path=table_name,
            data=collected_promo_details
        )
    }
    with YtPromoIndexerTestEnv(yt_server, **resources) as test_env:
        test_env.execute(mmap_name=MMAP_NAME)
        test_env.verify()
        yield test_env


def test_write_to_mmap(workflow, collected_promo_details):
    '''
    Проверяем, что промо из YT-таблицы от монетизации корректно записываются
    в mmap-файл yt_promo_details_generic_bundle.mmap
    '''
    compare_promo_details_collections(
        workflow.yt_promo_details,
        collected_promo_details
    )
