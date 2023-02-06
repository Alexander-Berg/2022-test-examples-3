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
    YtBlueSecretSaleTable,
    YtCollectedPromoDetailsTable,
)
from utils import compare_promo_details_collections
from market.idx.yatf.resources.yt_stuff_resource import (
    get_yt_prefix
)
from yt.wrapper import ypath_join


DT_NOW = datetime.now(tz=pytz.timezone('Europe/Moscow'))
DT_DELTA = timedelta(hours=24)
MMAP_NAME = 'yt_promo_details.mmap'
FEED_ID = 100500


def to_timestamp(dt):
    return int(time.mktime(dt.timetuple()))


@pytest.fixture(
    params=[{
        'valid_yt_promos_secret_sale': [
            {
                'type': PromoType.SECRET_SALE,
                'shop_promo_id': 'Secret sale promo key',
                'source_promo_id': 'Secret sale promo key',
                'start_date': to_timestamp(DT_NOW - DT_DELTA),
                'end_date': to_timestamp(DT_NOW + DT_DELTA),
                'generation_ts': to_timestamp(DT_NOW - DT_DELTA),
                'title': "Secret sale for Sberbank clients",
                'description': "Typical description of a sale",
                'secret_sale_details': {
                    'offer_discounts': [
                        {'msku': 10, 'percent': 9.3},
                        {'msku': 11, 'percent': 7.1},
                        {'msku': 12, 'percent': 5.5}
                    ]
                }
            },
            {
                'type': PromoType.SECRET_SALE,
                'shop_promo_id': 'Another secret sale promo key',
                'source_promo_id': 'Another secret sale promo key',
                'start_date': to_timestamp(DT_NOW + DT_DELTA),
                'generation_ts': to_timestamp(DT_NOW - DT_DELTA),
                'title': "Another secret sale",
                'description': "Typical description of another sale",
                'secret_sale_details': {
                    'offer_discounts': [
                        {'msku': 10, 'percent': 5},
                        {'msku': 11, 'percent': 6}
                    ]
                }
            },
            {
                'type': PromoType.SECRET_SALE,
                'shop_promo_id': 'Empty sale promo key',
                'source_promo_id': 'Empty sale promo key',
                'end_date': to_timestamp(DT_NOW + DT_DELTA),
                'generation_ts': to_timestamp(DT_NOW - DT_DELTA),
                'title': "Empty secret sale",
                'description': "Typical description of an empty sale"
            }
        ],
        'valid_yt_promos_fast_promos': [
            {
                'feed_id': FEED_ID,
                'type': PromoType.BLUE_CASHBACK,
                'shop_promo_id': 'cashback_collected',
                'start_date': to_timestamp(DT_NOW - DT_DELTA),
                'end_date': to_timestamp(DT_NOW + DT_DELTA),
                'generation_ts': to_timestamp(DT_NOW - DT_DELTA),
                'ui_promo_tags': ["extra_cashback", "some_tag"],
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
                },
            },
        ],
    }],
    scope='module'
)
def promo_details(request):
    return request.param


@pytest.yield_fixture(scope='module')
def valid_promo_workflow(promo_details, yt_server):
    secret_sale_table_name = ypath_join(get_yt_prefix(), 'promos', 'blue', 'secret_sale')
    collected_promo_details_table_name = ypath_join(get_yt_prefix(), 'promos', 'collected_promo_details')
    resources = {
        'blue_secret_sale_table': YtBlueSecretSaleTable(
            yt_stuff=yt_server,
            path=secret_sale_table_name,
            data=promo_details['valid_yt_promos_secret_sale']
        ),
        'collected_promo_details_table': YtCollectedPromoDetailsTable(
            yt_stuff=yt_server,
            path=collected_promo_details_table_name,
            data=promo_details['valid_yt_promos_fast_promos']
        )
    }
    cmd_args = {
        '--use-fast-promos-mode': None,
    }
    with YtPromoIndexerTestEnv(yt_server, use_collected_promo_details_table=True, **resources) as test_env:
        test_env.execute(mmap_name=MMAP_NAME, cmd_args=cmd_args)
        test_env.verify()
        yield test_env


def test_yt_valid_sale(valid_promo_workflow, promo_details):
    compare_promo_details_collections(
        valid_promo_workflow.yt_promo_details,
        promo_details['valid_yt_promos_fast_promos'] + promo_details['valid_yt_promos_secret_sale']
    )
