"""
Проверям что записи о бакетах white cpa оферов присутствуют в offer_delivery_buckets.mmap под флагом
"""

import pytest

from hamcrest import assert_that, contains_inanyorder
from market.idx.pylibrary.offer_flags.flags import OfferFlags

from market.idx.generation.yatf.resources.genlog_dumper.input_records_proto import InputRecordsProto, make_gl_record
from market.idx.generation.yatf.resources.genlog_dumper.input_run_options import RunOptions
from market.idx.generation.yatf.test_envs.genlog_dumper import (
    GenlogDumperTestEnv,
    RUN_RESOURCE_NAME,
    OFFERS_RESOURCE_NAME
)
from market.idx.generation.yatf.utils.fixtures import CpaStatus


@pytest.fixture(scope="module")
def offers(request):
    return [
        # White Cpa
        make_gl_record(
            offer_id='white cpa',
            cpa=CpaStatus.REAL,
            is_blue_offer=False,
            delivery_bucket_ids=[0, 1580],
            pickup_bucket_ids=[0, 1580],
            post_bucket_ids=[0],
        ),
        # Wnite not Cpa
        make_gl_record(
            offer_id='white not cpa',
            cpa=CpaStatus.NO,
            is_blue_offer=False,
            delivery_bucket_ids=[1],
            pickup_bucket_ids=[1],
            post_bucket_ids=[1],
        ),
        # Blue
        make_gl_record(
            offer_id='blue',
            cpa=CpaStatus.REAL,
            is_blue_offer=True,
            flags=OfferFlags.BLUE_OFFER,
            delivery_bucket_ids=[2],
            pickup_bucket_ids=[2],
            post_bucket_ids=[2],
        ),
    ]


@pytest.fixture(
    scope="module",
    params=[
        True,
        False
    ]
)
def clear_buckets_for_wcpa(request):
    return request.param


@pytest.yield_fixture(scope="module")
def genlog_dumper(offers, clear_buckets_for_wcpa):
    gd_resources = {
        RUN_RESOURCE_NAME: RunOptions([
            '--dumper', 'REGIONAL_DELIVERY',
        ]),
        OFFERS_RESOURCE_NAME: InputRecordsProto(offers)
    }
    with GenlogDumperTestEnv(**gd_resources) as env:
        env.execute(
            clear_old_bucket_ids_for_white_offers=True,
            clear_buckets_for_wcpa=clear_buckets_for_wcpa
        )
        env.verify()
        yield env


def test_output(genlog_dumper, offers, clear_buckets_for_wcpa):
    offers_db_mmap = genlog_dumper.offer_delivery_buckets_mmap

    all_delivery_buckets = offers_db_mmap.get_all_delivery_buckets()
    all_pickup_buckets = offers_db_mmap.get_all_pickup_buckets()
    all_post_buckets = offers_db_mmap.get_all_post_buckets()

    if not clear_buckets_for_wcpa:
        assert_that(all_delivery_buckets, contains_inanyorder('0', '2', '1580'))
        assert_that(all_pickup_buckets, contains_inanyorder('0', '2', '1580'))
        assert_that(all_post_buckets, contains_inanyorder('0', '2'))
    else:
        assert_that(all_delivery_buckets, contains_inanyorder('2'))
        assert_that(all_pickup_buckets, contains_inanyorder('2'))
        assert_that(all_post_buckets, contains_inanyorder('2'))
