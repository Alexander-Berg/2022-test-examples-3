#!/usr/bin/env python
# coding: utf-8
import pytest
from hamcrest import (
    assert_that,
    has_items
)

from market.idx.offers.yatf.utils.fixtures import default_genlog
from market.idx.pylibrary.offer_flags.flags import OfferFlags
from market.idx.offers.yatf.test_envs.offers_processor import OffersProcessorTestEnv
from market.idx.yatf.matchers.protobuf_matchers import IsProtobuf
from market.proto.delivery.delivery_calc.delivery_calc_pb2 import OffersDeliveryInfo, BucketInfo
from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogOffersTable
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from yt.wrapper.ypath import ypath_join
import yt.wrapper as yt

"""
Это неканоничный тест -- он проверяет поведение GenerationLogger, он вынужден экономить память в поле регионов.
"""

SEARCHLITERALREGIONSSOURCE = {
    'BUCKETS': 0,
    'SHOPS_DAT': 1,
    'EXTERNAL_TABLE': 2,
    'EARTH': 3,
    'OFFERS_REGIONS_EXTERNAL_TABLE': 4
}


@pytest.fixture(scope="module")
def genlog_rows():
    offers = [
        default_genlog(
            offer_id='1',
            mbi_delivery_bucket_ids=[2],
            mbi_pickup_bucket_ids=[5, 6],
            mbi_post_bucket_ids=[8],
            DeliveryCalcGeneration=1,
            delivery_flag=True,
            flags=OfferFlags.PICKUP | OfferFlags.STORE,
        ),
        default_genlog(
            offer_id='2',
            mbi_delivery_bucket_ids=[2],
            offers_delivery_info=OffersDeliveryInfo(
                courier_buckets_info=[BucketInfo(bucket_id=9, is_new=True)],
                pickup_buckets_info=[BucketInfo(bucket_id=10, is_new=True)],
                post_buckets_info=[BucketInfo(bucket_id=11, is_new=True)],
            ).SerializeToString(),
            DeliveryCalcGeneration=1,
            delivery_flag=True,
            flags=OfferFlags.PICKUP | OfferFlags.STORE,
    ),
        default_genlog(
            offer_id='3',
            mbi_delivery_bucket_ids=[2],
            mbi_pickup_bucket_ids=[5, 6],
            mbi_post_bucket_ids=[8],
            DeliveryCalcGeneration=1,
            delivery_flag=True,
            flags=OfferFlags.PICKUP | OfferFlags.STORE | OfferFlags.DOWNLOADABLE,  # no regions for downloadable offer
        ),
        default_genlog(
            offer_id='4',
            mbi_delivery_bucket_ids=[2],
            mbi_pickup_bucket_ids=[5, 6],
            mbi_post_bucket_ids=[8],
            DeliveryCalcGeneration=1,
            delivery_flag=True,
            is_blue_offer=True,  # no regions for blue offer if !reduceRegionsInsteadSetEarth
            flags=OfferFlags.PICKUP | OfferFlags.STORE | OfferFlags.IS_FULFILLMENT | OfferFlags.AVAILABLE,
        ),
        default_genlog(
            offer_id='5',
            mbi_delivery_bucket_ids=[2],
            mbi_pickup_bucket_ids=[5, 6],
            mbi_post_bucket_ids=[8],
            DeliveryCalcGeneration=1,
            delivery_flag=True,
            is_fake_msku_offer=True,  # no regions when is_fake_msku_offer
            flags=OfferFlags.PICKUP | OfferFlags.STORE,
        ),
        default_genlog(
            offer_id='6',
            delivery_flag=True,
            regions_from_shops_dat=[yt.yson.YsonUint64(100), yt.yson.YsonUint64(101), yt.yson.YsonUint64(100)],
            flags=OfferFlags.PICKUP | OfferFlags.STORE,
        ),
        default_genlog(
            offer_id='7',
            domain_level_regions_from_external_table=[yt.yson.YsonUint64(200), yt.yson.YsonUint64(201), yt.yson.YsonUint64(202)],
            delivery_flag=False,
            flags=OfferFlags.PICKUP | OfferFlags.STORE,
        ),
        default_genlog(
            offer_id='8',
            regions_from_shops_dat=[yt.yson.YsonUint64(100), yt.yson.YsonUint64(101)],
            domain_level_regions_from_external_table=[yt.yson.YsonUint64(200), yt.yson.YsonUint64(201), yt.yson.YsonUint64(202)],
            delivery_flag=False,
            flags=OfferFlags.PICKUP | OfferFlags.STORE,
        ),
        default_genlog(
            offer_id='9',
            mbi_delivery_bucket_ids=[2],
            mbi_pickup_bucket_ids=[5, 6],
            mbi_post_bucket_ids=[8],
            DeliveryCalcGeneration=1,
            regions_from_shops_dat=[yt.yson.YsonUint64(100), yt.yson.YsonUint64(101)],
            domain_level_regions_from_external_table=[yt.yson.YsonUint64(200), yt.yson.YsonUint64(201), yt.yson.YsonUint64(202)],
            delivery_flag=True,
            flags=OfferFlags.PICKUP | OfferFlags.STORE,
        ),
        default_genlog(
            offer_id='10',
            mbi_delivery_bucket_ids=[2],
            mbi_pickup_bucket_ids=[5, 6],
            mbi_post_bucket_ids=[8],
            DeliveryCalcGeneration=1,
            regions_from_shops_dat=[yt.yson.YsonUint64(100), yt.yson.YsonUint64(101)],
            domain_level_regions_from_external_table=[yt.yson.YsonUint64(200), yt.yson.YsonUint64(201), yt.yson.YsonUint64(202)],
            offer_level_regions_from_external_table=[yt.yson.YsonUint64(300), yt.yson.YsonUint64(301)],
            delivery_flag=True,
            flags=OfferFlags.PICKUP | OfferFlags.STORE,
        ),
        default_genlog(
            offer_id='11',
            regions_from_shops_dat=[yt.yson.YsonUint64(10000)],
            domain_level_regions_from_external_table=[yt.yson.YsonUint64(200), yt.yson.YsonUint64(201), yt.yson.YsonUint64(202)],
            delivery_flag=False,
            flags=OfferFlags.PICKUP | OfferFlags.STORE,
        ),
        default_genlog(
            offer_id='12',
            mbi_delivery_bucket_ids=[2],
            mbi_pickup_bucket_ids=[5, 6],
            mbi_post_bucket_ids=[8],
            DeliveryCalcGeneration=1,
            offer_level_regions_from_external_table=[yt.yson.YsonUint64(500), yt.yson.YsonUint64(501)],
            delivery_flag=True,
            is_blue_offer=True,
            flags=OfferFlags.PICKUP | OfferFlags.STORE | OfferFlags.IS_FULFILLMENT | OfferFlags.AVAILABLE,
        ),
    ]
    return offers


@pytest.fixture(scope="module")
def genlog_table(yt_server, genlog_rows):
    genlog_table = GenlogOffersTable(yt_server,  ypath_join(get_yt_prefix(), '0000'), genlog_rows)
    genlog_table.dump()
    return genlog_table


@pytest.yield_fixture(scope="module")
def workflow(yt_server, genlog_table):
    input_table_paths = [genlog_table.get_path()]

    with OffersProcessorTestEnv(
            yt_server,
            add_regions_from_shops_dat_or_external_table=True,
            use_genlog_scheme=True,
            input_table_paths=input_table_paths,
    ) as env:
        env.execute()
        yield env


@pytest.yield_fixture(scope="module")
def workflow_use_reduced_regions(yt_server, genlog_table):
    input_table_paths = [genlog_table.get_path()]

    with OffersProcessorTestEnv(
        yt_server,
        reduce_regions_instead_set_earth=True,
        add_regions_from_shops_dat_or_external_table=True,
        ignore_zero_region_in_buckets=True,
        prefer_reduced_regions=True,
        use_genlog_scheme=True,
        input_table_paths=input_table_paths,
    ) as env:
        env.execute()
        yield env


@pytest.yield_fixture(scope="module")
def workflow_prefer_reduced_regions(yt_server, genlog_table):
    input_table_paths = [genlog_table.get_path()]

    with OffersProcessorTestEnv(
        yt_server,
        reduce_regions_instead_set_earth=True,
        prefer_reduced_regions=True,
        use_genlog_scheme=True,
        input_table_paths=input_table_paths,
    ) as env:
        env.execute()
        yield env


@pytest.yield_fixture(scope="module")
def workflow_use_reduced_regions_dont_add_regions_from_shopsdat_or_external_table(yt_server, genlog_table):
    input_table_paths = [genlog_table.get_path()]

    with OffersProcessorTestEnv(
            yt_server,
            reduce_regions_instead_set_earth=True,
            add_regions_from_shops_dat_or_external_table=False,
            use_genlog_scheme=True,
            input_table_paths=input_table_paths,
    ) as env:
        env.execute()
        yield env


@pytest.yield_fixture(scope="module")
def workflow_add_earth(yt_server, genlog_table):
    input_table_paths = [genlog_table.get_path()]

    with OffersProcessorTestEnv(
            yt_server,
            add_earth_if_empty_regions=True,
            use_genlog_scheme=True,
            input_table_paths=input_table_paths,
    ) as env:
        env.execute()
        yield env


def test_genlog_regions(workflow):
    assert_that(workflow.genlog, has_items(
        IsProtobuf({
            'offer_id': '1',
            'orig_regions_literals': [1, 2, 3, 4, 5, 7, 8],  # 1, 2 - orig_regions; 3,4,5,7,8 - pickup_and_post_regions. !!! no 6
            'prefer_earth_over_orig_regions': False,
            'downloadable': False,
            'is_blue_offer': False,
        }),
        IsProtobuf({
            'offer_id': '2',
            'orig_regions_literals': [2],  # the only item from orig_regions
            'prefer_earth_over_orig_regions': False,
            'downloadable': False,
            'is_blue_offer': False,
        }),
        IsProtobuf({
            'offer_id': '3',
            'orig_regions_literals': [],
            'prefer_earth_over_orig_regions': False,
            'downloadable': True,
            'is_blue_offer': False,
        }),
        IsProtobuf({
            'offer_id': '4',
            'orig_regions_literals': [],
            'prefer_earth_over_orig_regions': True,
            'downloadable': False,
            'is_blue_offer': True,
        }),
        IsProtobuf({
            'offer_id': '5',
            'orig_regions_literals': [],
            'prefer_earth_over_orig_regions': False,
            'downloadable': False,
            'is_blue_offer': True,
        }),
        IsProtobuf({
            'offer_id': '6',
            'orig_regions_literals': [100, 101],
            'prefer_earth_over_orig_regions': False,
            'downloadable': False,
            'is_blue_offer': False,
        }),
        IsProtobuf({
            'offer_id': '6',
            'orig_regions_literals': [100, 101],
            'prefer_earth_over_orig_regions': False,
            'downloadable': False,
            'is_blue_offer': False,
            'orig_regions_literals_source': SEARCHLITERALREGIONSSOURCE['SHOPS_DAT']
        }),
        IsProtobuf({
            'offer_id': '7',
            'orig_regions_literals': [200, 201, 202],
            'prefer_earth_over_orig_regions': False,
            'downloadable': False,
            'is_blue_offer': False,
            'orig_regions_literals_source': SEARCHLITERALREGIONSSOURCE['EXTERNAL_TABLE']
        }),
        IsProtobuf({
            'offer_id': '8',
            'orig_regions_literals': [100, 101],
            'prefer_earth_over_orig_regions': False,
            'downloadable': False,
            'is_blue_offer': False,
            'orig_regions_literals_source': SEARCHLITERALREGIONSSOURCE['SHOPS_DAT']
        }),
        IsProtobuf({
            'offer_id': '9',
            'orig_regions_literals': [1, 2, 3, 4, 5, 7, 8],  # Копия 1 ого оффера, но с добавленными регионами из shopsdat и внешней таблиы. Ожидаем только регионы из бакетов.
            'prefer_earth_over_orig_regions': False,
            'downloadable': False,
            'is_blue_offer': False,
            'orig_regions_literals_source': SEARCHLITERALREGIONSSOURCE['BUCKETS']
        }),
        IsProtobuf({
            'offer_id': '10',
            'orig_regions_literals': [300, 301],
            'prefer_earth_over_orig_regions': False,
            'downloadable': False,
            'is_blue_offer': False,
            'orig_regions_literals_source': SEARCHLITERALREGIONSSOURCE['OFFERS_REGIONS_EXTERNAL_TABLE']
        })
    ))


def test_genlog_with_reduced_regions(workflow_use_reduced_regions):
    assert_that(workflow_use_reduced_regions.genlog, has_items(
        IsProtobuf({
            'offer_id': '1',
            'orig_regions_literals': [1, 2, 3, 4, 5, 7, 8],  # 1, 2 - orig_regions; 3,4,5,7,8 - pickup_and_post_regions. !!! no 6
            'prefer_earth_over_orig_regions': False,
            'downloadable': False,
            'is_blue_offer': False,
            'is_fake_msku_offer': False,
        }),
        IsProtobuf({
            'offer_id': '2',
            'orig_regions_literals': [2],  # the only item from orig_regions
            'prefer_earth_over_orig_regions': False,
            'downloadable': False,
            'is_blue_offer': False,
            'is_fake_msku_offer': False,
        }),
        IsProtobuf({
            'offer_id': '3',
            'orig_regions_literals': [],
            'prefer_earth_over_orig_regions': False,
            'downloadable': True,
            'is_blue_offer': False,
            'is_fake_msku_offer': False,
        }),
        IsProtobuf({
            'offer_id': '4',
            'orig_regions_literals': [1, 2],  # no orig_geo_regions and pickup_and_post_regions
            'prefer_earth_over_orig_regions': False,
            'downloadable': False,
            'is_blue_offer': True,
            'is_fake_msku_offer': False,
        }),
        IsProtobuf({
            'offer_id': '5',
            'orig_regions_literals': [],
            'prefer_earth_over_orig_regions': False,
            'downloadable': False,
            'is_blue_offer': True,
            'is_fake_msku_offer': True,
        }),
        IsProtobuf({
            'offer_id': '6',
            'orig_regions_literals': [100, 101],
            'prefer_earth_over_orig_regions': False,
            'downloadable': False,
            'is_blue_offer': False,
            'is_fake_msku_offer': False,
            'orig_regions_literals_source': SEARCHLITERALREGIONSSOURCE['SHOPS_DAT']
        }),
        IsProtobuf({
            'offer_id': '7',
            'orig_regions_literals': [200, 201, 202],
            'prefer_earth_over_orig_regions': False,
            'downloadable': False,
            'is_blue_offer': False,
            'is_fake_msku_offer': False,
            'orig_regions_literals_source': SEARCHLITERALREGIONSSOURCE['EXTERNAL_TABLE']
        }),
        IsProtobuf({
            'offer_id': '8',
            'orig_regions_literals': [100, 101],
            'prefer_earth_over_orig_regions': False,
            'downloadable': False,
            'is_blue_offer': False,
            'is_fake_msku_offer': False,
            'orig_regions_literals_source': SEARCHLITERALREGIONSSOURCE['SHOPS_DAT']
        }),
        IsProtobuf({
            'offer_id': '9',
            'orig_regions_literals': [1, 2, 3, 4, 5, 7, 8],  # Копия 1 ого оффера, но с добавленными регионами из shopsdat и внешней таблиы. Ожидаем только регионы из бакетов.
            'prefer_earth_over_orig_regions': False,
            'downloadable': False,
            'is_blue_offer': False,
            'is_fake_msku_offer': False,
            'orig_regions_literals_source': SEARCHLITERALREGIONSSOURCE['BUCKETS']
        }),
        IsProtobuf({
            'offer_id': '10',
            'orig_regions_literals': [300, 301],
            'prefer_earth_over_orig_regions': False,
            'downloadable': False,
            'is_blue_offer': False,
            'is_fake_msku_offer': False,
            'orig_regions_literals_source': SEARCHLITERALREGIONSSOURCE['OFFERS_REGIONS_EXTERNAL_TABLE']
        }),
        IsProtobuf({
            'offer_id': '11',
            'orig_regions_literals': [200, 201, 202],  # В shopsdat дефолтный регион вся Земля, должен игнорироваться
            'prefer_earth_over_orig_regions': False,
            'downloadable': False,
            'is_blue_offer': False,
            'is_fake_msku_offer': False,
            'orig_regions_literals_source': SEARCHLITERALREGIONSSOURCE['EXTERNAL_TABLE']
        }),
        IsProtobuf({
            'offer_id': '12',
            'orig_regions_literals': [500, 501],
            'prefer_earth_over_orig_regions': False,
            'downloadable': False,
            'is_blue_offer': True,
            'is_fake_msku_offer': False,
            'orig_regions_literals_source': SEARCHLITERALREGIONSSOURCE['OFFERS_REGIONS_EXTERNAL_TABLE']
        }),
    ))


def test_genlog_regions_no_additional_regions(workflow_use_reduced_regions_dont_add_regions_from_shopsdat_or_external_table):
    assert_that(workflow_use_reduced_regions_dont_add_regions_from_shopsdat_or_external_table.genlog, has_items(
        IsProtobuf({
            'offer_id': '1',
            'orig_regions_literals': [1, 2, 3, 4, 5, 7, 8],  # 1, 2 - orig_regions; 3,4,5,7,8 - pickup_and_post_regions. !!! no 6
            'prefer_earth_over_orig_regions': False,
            'downloadable': False,
            'is_blue_offer': False,
            'is_fake_msku_offer': False,
        }),
        IsProtobuf({
            'offer_id': '2',
            'orig_regions_literals': [2],  # the only item from orig_regions
            'prefer_earth_over_orig_regions': False,
            'downloadable': False,
            'is_blue_offer': False,
            'is_fake_msku_offer': False,
        }),
        IsProtobuf({
            'offer_id': '3',
            'orig_regions_literals': [],
            'prefer_earth_over_orig_regions': False,
            'downloadable': True,
            'is_blue_offer': False,
            'is_fake_msku_offer': False,
        }),
        IsProtobuf({
            'offer_id': '4',
            'orig_regions_literals': [],
            'prefer_earth_over_orig_regions': True,
            'downloadable': False,
            'is_blue_offer': True,
            'is_fake_msku_offer': False,
        }),
        IsProtobuf({
            'offer_id': '5',
            'orig_regions_literals': [],
            'prefer_earth_over_orig_regions': False,
            'downloadable': False,
            'is_blue_offer': True,
            'is_fake_msku_offer': True,
        }),
        IsProtobuf({
            'offer_id': '6',
            'orig_regions_literals': [],
            'prefer_earth_over_orig_regions': False,
            'downloadable': False,
            'is_blue_offer': False,
            'is_fake_msku_offer': False,
        }),
        IsProtobuf({
            'offer_id': '7',
            'orig_regions_literals': [],
            'prefer_earth_over_orig_regions': False,
            'downloadable': False,
            'is_blue_offer': False,
            'is_fake_msku_offer': False
        }),
        IsProtobuf({
            'offer_id': '8',
            'orig_regions_literals': [],
            'prefer_earth_over_orig_regions': False,
            'downloadable': False,
            'is_blue_offer': False,
            'is_fake_msku_offer': False
        }),
        IsProtobuf({
            'offer_id': '9',
            'orig_regions_literals': [1, 2, 3, 4, 5, 7, 8],
            'prefer_earth_over_orig_regions': False,
            'downloadable': False,
            'is_blue_offer': False,
            'is_fake_msku_offer': False,
        }),
        IsProtobuf({
            'offer_id': '10',
            'orig_regions_literals': [1, 2, 3, 4, 5, 7, 8],
            'prefer_earth_over_orig_regions': False,
            'downloadable': False,
            'is_blue_offer': False,
            'is_fake_msku_offer': False,
        }),
        IsProtobuf({
            'offer_id': '12',
            'orig_regions_literals': [1, 2],
            'prefer_earth_over_orig_regions': True,
            'downloadable': False,
            'is_blue_offer': True,
            'is_fake_msku_offer': False,
        }),
    ))


def test_genlog_regions_add_earth(workflow_add_earth):
    assert_that(workflow_add_earth.genlog, has_items(
        IsProtobuf({
            'offer_id': '1',
            'orig_regions_literals': [1, 2, 3, 4, 5, 7, 8],  # 1, 2 - orig_regions; 3,4,5,7,8 - pickup_and_post_regions. !!! no 6
            'prefer_earth_over_orig_regions': False,
            'downloadable': False,
            'is_blue_offer': False,
            'is_fake_msku_offer': False,
            'orig_regions_literals_source': SEARCHLITERALREGIONSSOURCE['BUCKETS']
        }),
        IsProtobuf({
            'offer_id': '2',
            'orig_regions_literals': [2],  # the only item from orig_regions
            'prefer_earth_over_orig_regions': False,
            'downloadable': False,
            'is_blue_offer': False,
            'is_fake_msku_offer': False,
            'orig_regions_literals_source': SEARCHLITERALREGIONSSOURCE['BUCKETS']
        }),
        IsProtobuf({
            'offer_id': '3',
            'orig_regions_literals': [],
            'prefer_earth_over_orig_regions': False,
            'downloadable': True,
            'is_blue_offer': False,
            'is_fake_msku_offer': False,
        }),
        IsProtobuf({
            'offer_id': '4',
            'orig_regions_literals': [],
            'prefer_earth_over_orig_regions': True,
            'downloadable': False,
            'is_blue_offer': True,
            'is_fake_msku_offer': False,
        }),
        IsProtobuf({
            'offer_id': '5',
            'orig_regions_literals': [],
            'prefer_earth_over_orig_regions': False,
            'downloadable': False,
            'is_blue_offer': True,
            'is_fake_msku_offer': True,
        }),
        IsProtobuf({
            'offer_id': '6',
            'orig_regions_literals': [10000],
            'prefer_earth_over_orig_regions': False,
            'downloadable': False,
            'is_blue_offer': False,
            'is_fake_msku_offer': False,
            'orig_regions_literals_source': SEARCHLITERALREGIONSSOURCE['EARTH']
        }),
        IsProtobuf({
            'offer_id': '7',
            'orig_regions_literals': [10000],
            'prefer_earth_over_orig_regions': False,
            'downloadable': False,
            'is_blue_offer': False,
            'is_fake_msku_offer': False,
            'orig_regions_literals_source': SEARCHLITERALREGIONSSOURCE['EARTH']
        }),
        IsProtobuf({
            'offer_id': '8',
            'orig_regions_literals': [10000],
            'prefer_earth_over_orig_regions': False,
            'downloadable': False,
            'is_blue_offer': False,
            'is_fake_msku_offer': False,
            'orig_regions_literals_source': SEARCHLITERALREGIONSSOURCE['EARTH']
        }),
        IsProtobuf({
            'offer_id': '9',
            'orig_regions_literals': [1, 2, 3, 4, 5, 7, 8],  # Копия 1 ого оффера, но с добавленными регионами из shopsdat и внешней таблиы. Ожидаем только регионы из бакетов.
            'prefer_earth_over_orig_regions': False,
            'downloadable': False,
            'is_blue_offer': False,
            'is_fake_msku_offer': False,
            'orig_regions_literals_source': SEARCHLITERALREGIONSSOURCE['BUCKETS']
        }),
        IsProtobuf({
            'offer_id': '10',
            'orig_regions_literals': [1, 2, 3, 4, 5, 7, 8],
            'prefer_earth_over_orig_regions': False,
            'downloadable': False,
            'is_blue_offer': False,
            'is_fake_msku_offer': False,
            'orig_regions_literals_source': SEARCHLITERALREGIONSSOURCE['BUCKETS']
        })
    ))


def test_prefer_reduced_regions(workflow_prefer_reduced_regions):
    assert_that(workflow_prefer_reduced_regions.genlog, has_items(
        IsProtobuf({
            'offer_id': '12',
            'orig_regions_literals': [1, 2],
            'prefer_earth_over_orig_regions': False,
            'downloadable': False,
            'is_blue_offer': True,
            'is_fake_msku_offer': False,
        }),
    ))
