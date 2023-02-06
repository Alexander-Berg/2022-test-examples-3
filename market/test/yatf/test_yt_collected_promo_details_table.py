# -*- coding: utf-8 -*-

from datetime import (
    datetime,
    timedelta
)
import time
import pytest
import pytz

from market.pylibrary.const.offer_promo import PromoType
from market.idx.promos.yt_promo_indexer.yatf.test_env import YtPromoIndexerTestEnv
from market.idx.promos.yt_promo_indexer.yatf.resources import (
    YtCollectedPromoDetailsTable,
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


@pytest.fixture(scope='module')
def collected_promo_details(request):
    return [
        {
            'feed_id': FEED_ID,
            'type': PromoType.BLUE_CASHBACK,
            'shop_promo_id': 'bluecashback_collected',
            'start_date': to_timestamp(DT_NOW - DT_DELTA),
            'end_date': to_timestamp(DT_NOW + DT_DELTA),
            'allowed_payment_methods': PaymentMethod.PT_MMAP_ALL,
            'ui_promo_tags': ["extra-cashback-2", "sometag-2"],
            'blue_cashback': {
                'share': 3,
                'version': 4,
                'priority': 5,
                'predicates': [
                    {
                        'perks': ['beru_plus', 'yandex_cashback'],
                        'at_supplier_warehouse': True,
                        'loyalty_program_status': 1,
                        'delivery_partner_types': [1, ],
                    }
                ],
                'max_offer_cashback': 1005,
            }
        },
    ]


@pytest.yield_fixture(scope='module')
def workflow(collected_promo_details, yt_server):
    collected_table_name = ypath_join(get_yt_prefix(), 'promos', 'collected_promo_details')
    resources = {
        'collected_promo_details_table': YtCollectedPromoDetailsTable(
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
    compare_promo_details_collections(
        workflow.yt_promo_details,
        collected_promo_details
    )
