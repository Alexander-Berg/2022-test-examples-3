# coding=utf-8

import pytest

from hamcrest import assert_that, has_length, has_item, all_of, has_entries

from yt.wrapper import ypath_join

from market.idx.yatf.resources.yt_table_resource import YtTableResource
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix

from market.idx.generation.yatf.test_envs.image_factors_collector import ImageFactorsCollectorMode, ImageFactorsCollectorTestEnv
from market.idx.generation.yatf.resources.image_factors_collector.image_factors_collector_data import ImageFactorsCollectorJoinFactorsData


IMAGE2TEXT_V13_SIZE = 200


def _get_image2text_v13_str(val, length=IMAGE2TEXT_V13_SIZE):
    return " ".join([str(val) for i in range(length)])


@pytest.fixture(scope='module')
def yt_offers_dir(yt_stuff):
    return ypath_join(get_yt_prefix(), 'mi3/main/offers')


@pytest.fixture(scope='module')
def yt_offers_shard1(yt_stuff, yt_offers_dir):
    rows = [
        {'ware_md5': 'ware_md5_1_offer_features', 'msku': 1},
        {'ware_md5': 'ware_md5_2_aggregation', 'msku': 2},
        {'ware_md5': 'ware_md5_3_aggregation', 'msku': 2},
        {'ware_md5': 'ware_md5_4_wrong_embed', 'msku': 3},
    ]

    path = ypath_join(yt_offers_dir, '0000')
    schema = [
        {'required': False, "name": "ware_md5", "type": "string"},
        {'required': False, "name": "msku", "type": "uint64"},
    ]

    return YtTableResource(yt_stuff, path, data=rows, attributes={'schema': schema})


@pytest.fixture(scope='module')
def yt_offers_shard2(yt_stuff, yt_offers_dir):
    rows = [
        {'ware_md5': 'ware_md5_5_wrong_part', 'msku': 4},
        {'ware_md5': 'ware_md5_6_join', 'msku': 5},
    ]

    path = ypath_join(yt_offers_dir, '0001')
    schema = [
        {'required': False, "name": "ware_md5", "type": "string"},
        {'required': False, "name": "msku", "type": "uint64"},
    ]

    return YtTableResource(yt_stuff, path, data=rows, attributes={'schema': schema})


@pytest.fixture(scope='module')
def yt_in_offer_factors(yt_stuff):
    rows = [
        # приджойнится фича с меньшим pic_number
        {'ware_md5': 'ware_md5_1_offer_features', 'part': 0, 'pic_number': 1, 'image2text_v13_embedding_str': _get_image2text_v13_str(0.1), 'msku': 1, 'pic_area': 1},
        {'ware_md5': 'ware_md5_1_offer_features', 'part': 0, 'pic_number': 4, 'image2text_v13_embedding_str': _get_image2text_v13_str(0.2), 'msku': 1, 'pic_area': 1},
        # эмбеды правильно проагрегируются
        {'ware_md5': 'ware_md5_2_aggregation', 'part': 0, 'pic_number': 0, 'image2text_v13_embedding_str': _get_image2text_v13_str(0.3), 'msku': 2, 'pic_area': 1},
        {'ware_md5': 'ware_md5_3_aggregation', 'part': 0, 'pic_number': 1, 'image2text_v13_embedding_str': _get_image2text_v13_str(0.4), 'msku': 2, 'pic_area': 1},
        # не попадет в итоговую таблицу, т.к. неправильный размер  эмбеддинга
        {'ware_md5': 'ware_md5_4_wrong_embed', 'part': 0, 'pic_number': 0, 'image2text_v13_embedding_str': _get_image2text_v13_str(0.4, length=10), 'msku': 3, 'pic_area': 1},
        # фичи не приджойнятся, т.к. не совпадает part с фактическим шардом оффера (такого быть не должно)
        {'ware_md5': 'ware_md5_5_wrong_part', 'part': 0, 'pic_number': 0, 'image2text_v13_embedding_str': _get_image2text_v13_str(0.5), 'msku': 4, 'pic_area': 1},
        # эмбеды правильно приджойнятся из 2 шарда
        {'ware_md5': 'ware_md5_6_join', 'part': 1, 'pic_number': 0, 'image2text_v13_embedding_str': _get_image2text_v13_str(0.6), 'msku': 5, 'pic_area': 1},
    ]

    path = ypath_join(get_yt_prefix(), 'offer_factors')
    schema = [
        {'required': False, "name": "ware_md5", "type": "string"},
        {'required': False, "name": "part", "type": "uint64"},
        {'required': False, "name": "pic_number", "type": "uint64"},
        {'required': False, "name": "image2text_v13_embedding_str", "type": "string"},
        {'required': False, "name": "msku", "type": "uint64"},
        {'required': False, "name": "pic_area", "type": "uint32"},
    ]

    return YtTableResource(yt_stuff, path, data=rows, attributes={'schema': schema})


@pytest.fixture(scope='module')
def output_path():
    return ypath_join(get_yt_prefix(), 'joined_offers')


@pytest.fixture(scope='module')
def calculate_over_msku_data(yt_offers_dir, yt_in_offer_factors, output_path, yt_offers_shard1, yt_offers_shard2):
    return ImageFactorsCollectorJoinFactorsData(
        input_offers_dir=yt_offers_dir,
        input_offers_tables=[yt_offers_shard1, yt_offers_shard2],
        input_factors_table=yt_in_offer_factors,
        output=output_path
    )


@pytest.yield_fixture(scope='module')
def workflow(yt_stuff, calculate_over_msku_data):
    resources = {
        "options": calculate_over_msku_data
    }

    with ImageFactorsCollectorTestEnv(**resources) as env:
        env.execute(ImageFactorsCollectorMode.CALCULATE_FACTORS_OVER_MSKU, yt_stuff)
        env.verify()
        yield env


def test_result_table_data(workflow):
    expected = [
        {'ware_md5': 'ware_md5_1_offer_features', 'part': 0, 'image2text_v13_embedding_str': _get_image2text_v13_str(0.1), 'msku': 1},
        {'ware_md5': 'ware_md5_2_aggregation', 'part': 0, 'image2text_v13_embedding_str': _get_image2text_v13_str(0.35), 'msku': 2},
        {'ware_md5': 'ware_md5_3_aggregation', 'part': 0, 'image2text_v13_embedding_str': _get_image2text_v13_str(0.35), 'msku': 2},
        {'ware_md5': 'ware_md5_6_join', 'part': 1, 'image2text_v13_embedding_str': _get_image2text_v13_str(0.6), 'msku': 5}
    ]

    assert_that(workflow.outputs["aggregated_factors_table"].data, has_length(len(expected)), "Records count is icorrect")
    assert_that(
        workflow.outputs["aggregated_factors_table"].data,
        all_of(*[has_item(has_entries(doc)) for doc in expected]),
        "Wrong joined offers data",
    )
