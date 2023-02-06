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
    MechanicsPaymentType,
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
    PaymentMethodNames,
)

DT_NOW = datetime.now(tz=pytz.timezone('Europe/Moscow'))
DT_DELTA = timedelta(hours=24)
MMAP_NAME = 'yt_promo_details_generic_bundle.mmap'
FEED_ID = 777


def to_timestamp(dt):
    return int(time.mktime(dt.timetuple()))


def get_expected_promos(promo_details):
    for p in promo_details:
        p['binary_promo_md5'] = p['shop_promo_id']
        if 'allowed_payment_methods' in p and isinstance(p['allowed_payment_methods'], int):
            p['allowed_payment_methods'] = PaymentMethodNames.to_mmapviewer_format(p['allowed_payment_methods'])
        if 'restrictions' in p and 'restricted_promo_types' in p['restrictions']:
            restricted_promo_types = 0
            for promo_type in p['restrictions']['restricted_promo_types']:
                restricted_promo_types |= promo_type
            p['restrictions']['restricted_promo_types'] = restricted_promo_types

    return list(promo_details)


@pytest.fixture(scope='module')
def collected_promo_details(request):
    return [
        {
            'feed_id': FEED_ID,
            'type': PromoType.PROMO_CODE,
            'promo_code': 'promo_code_1_text',
            'discount': {
                'value': 300,
                'currency': 'RUR',
            },
            'url': 'http://promocode_1.com',
            'landing_url': 'http://promocode_1_landing.com',
            'shop_promo_id': 'promo_code_1',
            'start_date': to_timestamp(DT_NOW - DT_DELTA),
            'end_date': to_timestamp(DT_NOW + DT_DELTA),
            'allowed_payment_methods': PaymentMethod.PT_MMAP_ALL,
            'mechanics_payment_type': MechanicsPaymentType.CPC,
            'source_type': ESourceType.LOYALTY,
            'source_reference': 'http://source_reference.ru',
            'same_type_priority': 7721,
            'restrictions': {
                'region_restriction': {
                    'regions': [41, 42, 43],
                    'excluded_regions': [50, 51],
                },
                'order_min_price': {
                    'value': 400,
                    'currency': 'RUR',
                },
                'order_max_price': {
                    'value': 500,
                    'currency': 'RUR',
                },
                'restricted_promo_types': [PromoType.DIRECT_DISCOUNT, PromoType.SECRET_SALE],
                'category_price_restrictions': [
                    {
                        'category_id': 41766,
                        'max_price': {'value': 872, 'currency': 'RUR'},
                    },
                    {
                        'category_id': 41921,
                        'min_price': {'value': 17, 'currency': 'RUR'},
                        'max_price': {'value': 9831, 'currency': 'RUR'},
                    },
                ]
            },
        },
    ]


@pytest.yield_fixture(scope='module')
def blue_promocode_workflow(collected_promo_details, yt_server):
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


def test_blue_promocode(blue_promocode_workflow, collected_promo_details):
    compare_promo_details_collections(
        blue_promocode_workflow.yt_promo_details,
        collected_promo_details
    )
