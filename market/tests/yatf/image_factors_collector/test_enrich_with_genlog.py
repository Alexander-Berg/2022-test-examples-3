# coding=utf-8

import pytest

from hamcrest import assert_that, has_length, has_item, all_of, has_entries

from yt.wrapper import ypath_join

from market.idx.yatf.resources.yt_table_resource import YtTableResource
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogRow
from market.idx.yatf.utils.genlog import genlog_table_schema

from market.idx.generation.yatf.test_envs.image_factors_collector import ImageFactorsCollectorMode, ImageFactorsCollectorTestEnv
from market.idx.generation.yatf.resources.image_factors_collector.image_factors_collector_data import ImageFactorsCollectorJoinFactorsData


@pytest.fixture(scope='module')
def yt_genlog_dir(yt_stuff):
    return ypath_join(get_yt_prefix(), 'mi3/main/genlog')


@pytest.fixture(scope='module')
def yt_genlog_shard0(yt_stuff, yt_genlog_dir):
    rows = [
        GenlogRow(shard_id=0, id=0, feed_id=1, offer_id='1', ware_md5='ware_md5_1_no_features', market_sku=None),
        GenlogRow(shard_id=0, id=1, feed_id=1, offer_id='2', ware_md5='ware_md5_2_offer_features', market_sku=None),
    ]
    path = ypath_join(yt_genlog_dir, '0000')
    return YtTableResource(yt_stuff, path, data=rows, attributes={'schema': genlog_table_schema()})


@pytest.fixture(scope='module')
def yt_genlog_shard1(yt_stuff, yt_genlog_dir):
    rows = [
        GenlogRow(shard_id=1, id=0, feed_id=2, offer_id='1', ware_md5='ware_md5_3_msku_features', market_sku=1),
        GenlogRow(shard_id=1, id=1, feed_id=2, offer_id='2', ware_md5='ware_md5_4_wrong_part', market_sku=None),
    ]
    path = ypath_join(yt_genlog_dir, '0001')
    return YtTableResource(yt_stuff, path, data=rows, attributes={'schema': genlog_table_schema()})


@pytest.fixture(scope='module')
def yt_in_offer_factors(yt_stuff):
    rows = [
        # приджойнится фича с меньшим pic_number
        {'ware_md5': 'ware_md5_2_offer_features', 'part': 0, 'pic_number': 1, 'image2text_v10_embedding_str': 'pic1_ware_md5_2_offer_features'},
        {'ware_md5': 'ware_md5_2_offer_features', 'part': 0, 'pic_number': 4, 'image2text_v10_embedding_str': 'pic4_ware_md5_2_offer_features'},
        # фичи не приджойнятся, т.к. нет оффера в генлоге
        {'ware_md5': 'ware_md5_3_no_genlog_offer', 'part': 1, 'pic_number': 0, 'image2text_v10_embedding_str': 'embed3'},
        # фичи не приджойнятся, т.к. не совпадает part с фактическим шардом оффера (такого быть не должно)
        {'ware_md5': 'ware_md5_4_wrong_part', 'part': 0, 'pic_number': 0, 'image2text_v10_embedding_str': 'embed6'},
    ]

    path = ypath_join(get_yt_prefix(), 'offer_factors')
    schema = [
        {'required': False, "name": "ware_md5", "type": "string"},
        {'required': False, "name": "part", "type": "uint64"},
        {'required': False, "name": "pic_number", "type": "uint64"},
        {'required': False, "name": "image2text_v10_embedding_str", "type": "string"},
    ]

    return YtTableResource(yt_stuff, path, data=rows, attributes={'schema': schema})


@pytest.fixture(scope='module')
def yt_in_msku_factors(yt_stuff):
    rows = [
        # приджойнится фича с меньшим pic_number
        {'ware_md5': 'ware_md5_3_msku_features', 'part': 1, 'pic_number': 1, 'image2text_v10_embedding_str': 'pic1_ware_md5_3_msku_features'},
        {'ware_md5': 'ware_md5_3_msku_features', 'part': 1, 'pic_number': 0, 'image2text_v10_embedding_str': 'pic0_ware_md5_3_msku_features'},
        # фичи не приджойнятся, т.к. нет оффера в генлоге
        {'ware_md5': 'ware_md5_4_no_genlog_offer', 'part': 0, 'pic_number': 1, 'image2text_v10_embedding_str': 'embed4'},
    ]

    path = ypath_join(get_yt_prefix(), 'msku_factors')
    schema = [
        {'required': False, "name": "ware_md5", "type": "string"},
        {'required': False, "name": "part", "type": "uint64"},
        {'required': False, "name": "pic_number", "type": "uint64"},
        {'required': False, "name": "image2text_v10_embedding_str", "type": "string"},
    ]

    return YtTableResource(yt_stuff, path, data=rows, attributes={'schema': schema})


@pytest.fixture(scope='module')
def output_dir():
    return ypath_join(get_yt_prefix(), 'enriched')


@pytest.fixture(scope='module')
def enrich_data(yt_genlog_dir, yt_in_offer_factors, yt_in_msku_factors, output_dir, yt_genlog_shard0, yt_genlog_shard1):
    return ImageFactorsCollectorJoinFactorsData(
        input_offers_dir=yt_genlog_dir,
        input_offers_tables=[yt_genlog_shard0, yt_genlog_shard1],
        input_factors_table=yt_in_offer_factors,
        input_factors_table2=yt_in_msku_factors,
        output=output_dir
    )


@pytest.yield_fixture(scope='module')
def workflow(yt_stuff, enrich_data):
    resources = {
        "options": enrich_data
    }

    with ImageFactorsCollectorTestEnv(**resources) as env:
        env.execute(ImageFactorsCollectorMode.ENRICH_WITH_GENLOG, yt_stuff)
        env.verify()
        yield env


def test_result_table_data_shard0(workflow):
    expected = [
        {'doc_id': 1, 'image2text_v10_embedding_str': 'pic1_ware_md5_2_offer_features', 'msku': None},
    ]

    assert_that(workflow.outputs["enriched_shard_tables"][0].data, has_length(len(expected)), "Records count is icorrect")
    assert_that(
        workflow.outputs["enriched_shard_tables"][0].data,
        all_of(*[has_item(has_entries(doc)) for doc in expected]),
        "Wrong joined offers data",
    )


def test_result_table_data_shard1(workflow):
    expected = [
        {'doc_id': 0, 'image2text_v10_embedding_str': 'pic0_ware_md5_3_msku_features', 'msku': 1},
    ]

    assert_that(workflow.outputs["enriched_shard_tables"][1].data, has_length(len(expected)), "Records count is icorrect")
    assert_that(
        workflow.outputs["enriched_shard_tables"][1].data,
        all_of(*[has_item(has_entries(doc)) for doc in expected]),
        "Wrong joined offers data",
    )
