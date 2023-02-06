# coding=utf-8

import pytest

from hamcrest import assert_that, has_length, has_item, all_of, has_entries

from yt.wrapper import ypath_join

from market.idx.yatf.resources.yt_table_resource import YtTableResource
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix

from market.idx.generation.yatf.test_envs.image_factors_collector import ImageFactorsCollectorMode, ImageFactorsCollectorTestEnv
from market.idx.generation.yatf.resources.image_factors_collector.image_factors_collector_data import ImageFactorsCollectorJoinFactorsData
from market.idx.yatf.resources.tovar_tree_pb import (
    MboCategory,
    TovarTreePb,
)
from market.idx.generation.yatf.utils.fixtures import make_uc_proto_str


IMAGE2TEXT_V10_SIZE = 200

# Для отладки значений, полученных после сжатия эмбедов, см. kernel/dssm_applier/decompession/decompression.h
IMAGE2TEXT_V10_MIN_BOUND = -0.197

ROOT_CATEGORY_IMAGE_FACTORS = 2
CHILD_CATEGORY_IMAGE_FACTORS = 3
CATEGORY_NOT_TO_CALC_IMAGE_FACTORS = 4
WRONG_CATEGORY = 5


@pytest.yield_fixture(scope="module")
def tovar_tree():
    return [
        MboCategory(
            hid=1,
            tovar_id=0,
            unique_name="Все товары",
            name="Все товары",
            output_type=MboCategory.GURULIGHT,
        ),
        MboCategory(
            hid=ROOT_CATEGORY_IMAGE_FACTORS,
            parent_hid=1,
            tovar_id=0,
            unique_name="cat2-1",
            name="cat2-1",
            output_type=MboCategory.GURULIGHT,
        ),
        MboCategory(
            hid=CHILD_CATEGORY_IMAGE_FACTORS,
            parent_hid=ROOT_CATEGORY_IMAGE_FACTORS,
            tovar_id=0,
            unique_name="cat3-2",
            name="cat3-2",
            output_type=MboCategory.GURULIGHT,
        ),
        MboCategory(
            hid=CATEGORY_NOT_TO_CALC_IMAGE_FACTORS,
            parent_hid=1,
            tovar_id=0,
            unique_name="cat4-1",
            name="cat4-1",
            output_type=MboCategory.GURULIGHT,
        ),
    ]


def _get_raw_image2text_v10_str(val):
    l = [str(val) for i in range(IMAGE2TEXT_V10_SIZE)]
    return " ".join(l)


@pytest.fixture(scope='module')
def yt_offers_dir(yt_stuff):
    return ypath_join(get_yt_prefix(), 'mi3/main/offers')


@pytest.fixture(scope='module')
def yt_offers_shard1(yt_stuff, yt_offers_dir):
    rows = [
        {'ware_md5': 'msku_joined_factors1', 'msku': 1, 'is_fake_msku_offer': True, 'uc': make_uc_proto_str(market_sku_id=1, category_id=ROOT_CATEGORY_IMAGE_FACTORS)},
        {'ware_md5': 'msku_without_factors', 'msku': 2, 'is_fake_msku_offer': True, 'uc': make_uc_proto_str(market_sku_id=2, category_id=ROOT_CATEGORY_IMAGE_FACTORS)},
        {'ware_md5': 'msku_with_empty_i2t', 'msku': 3, 'is_fake_msku_offer': True, 'uc': make_uc_proto_str(market_sku_id=3, category_id=ROOT_CATEGORY_IMAGE_FACTORS)},
        {'ware_md5': 'msku_with_wrong_i2t', 'msku': 4, 'is_fake_msku_offer': True, 'uc': make_uc_proto_str(market_sku_id=4, category_id=ROOT_CATEGORY_IMAGE_FACTORS)},
    ]
    path = ypath_join(yt_offers_dir, '0000')
    schema = [
        {'required': False, "name": "ware_md5", "type": "string"},
        {'required': False, "name": "msku", "type": "uint64"},
        {'required': False, "name": "is_fake_msku_offer", "type": "boolean"},
        {'required': False, "name": "uc", "type": "string"},
    ]

    return YtTableResource(yt_stuff, path, data=rows, attributes={'schema': schema})


@pytest.fixture(scope='module')
def yt_offers_shard2(yt_stuff, yt_offers_dir):
    rows = [
        {'ware_md5': 'msku_joined_factors2', 'msku': 5, 'is_fake_msku_offer': True, 'uc': make_uc_proto_str(market_sku_id=5, category_id=CHILD_CATEGORY_IMAGE_FACTORS)},
        {'ware_md5': 'not_msku', 'msku': 6, 'is_fake_msku_offer': False, 'uc': make_uc_proto_str(market_sku_id=6, category_id=ROOT_CATEGORY_IMAGE_FACTORS)},
        {'ware_md5': 'wrong_msku', 'msku': 0, 'is_fake_msku_offer': True, 'uc': make_uc_proto_str(market_sku_id=0, category_id=ROOT_CATEGORY_IMAGE_FACTORS)},
        {'ware_md5': 'wrong_factors', 'msku': 7, 'is_fake_msku_offer': True, 'uc': make_uc_proto_str(market_sku_id=7, category_id=ROOT_CATEGORY_IMAGE_FACTORS)},
        {'ware_md5': 'disabled_cat', 'msku': 8, 'is_fake_msku_offer': True, 'uc': make_uc_proto_str(market_sku_id=8, category_id=CATEGORY_NOT_TO_CALC_IMAGE_FACTORS)},
        {'ware_md5': 'wrong_cat', 'msku': 9, 'is_fake_msku_offer': True, 'uc': make_uc_proto_str(market_sku_id=9, category_id=WRONG_CATEGORY)},
        {'ware_md5': 'no_cat', 'msku': 10, 'is_fake_msku_offer': True, 'uc': make_uc_proto_str(market_sku_id=10, category_id=None)},
    ]
    path = ypath_join(yt_offers_dir, '0001')
    schema = [
        {'required': False, "name": "ware_md5", "type": "string"},
        {'required': False, "name": "msku", "type": "uint64"},
        {'required': False, "name": "is_fake_msku_offer", "type": "boolean"},
        {'required': False, "name": "uc", "type": "string"},
    ]

    return YtTableResource(yt_stuff, path, data=rows, attributes={'schema': schema})


@pytest.fixture(scope='module')
def yt_in_msku_factors(yt_stuff):
    rows = [
        # joined factors
        {'model_id': 1, 'signature_i2t_v10': _get_raw_image2text_v10_str(IMAGE2TEXT_V10_MIN_BOUND), 'picture_index': 0},
        {'model_id': 1, 'signature_i2t_v10': _get_raw_image2text_v10_str(IMAGE2TEXT_V10_MIN_BOUND), 'picture_index': 1},
        {'model_id': 5, 'signature_i2t_v10': _get_raw_image2text_v10_str(IMAGE2TEXT_V10_MIN_BOUND), 'picture_index': 0},
        # empty i2t
        {'model_id': 3, 'signature_i2t_v10': '', 'picture_index': 0},
        # wrong (too short) i2t
        {'model_id': 4, 'signature_i2t_v10': '1 2 3', 'picture_index': 0},
        # wrong model_id
        {'model_id': 0, 'signature_i2t_v10': _get_raw_image2text_v10_str(IMAGE2TEXT_V10_MIN_BOUND), 'picture_index': 0},
        # no i2t
        {'model_id': 7, 'signature_i2t_v10': None, 'picture_index': 0},
        # no model_id
        {'model_id': None, 'signature_i2t_v10': _get_raw_image2text_v10_str(IMAGE2TEXT_V10_MIN_BOUND), 'picture_index': 0},
        # msku from category with disabled image factors
        {'model_id': 8, 'signature_i2t_v10': _get_raw_image2text_v10_str(IMAGE2TEXT_V10_MIN_BOUND), 'picture_index': 0},
        # msku from wrong category (not in tovar tree)
        {'model_id': 9, 'signature_i2t_v10': _get_raw_image2text_v10_str(IMAGE2TEXT_V10_MIN_BOUND), 'picture_index': 0},
        # msku without category
        {'model_id': 10, 'signature_i2t_v10': _get_raw_image2text_v10_str(IMAGE2TEXT_V10_MIN_BOUND), 'picture_index': 0},
    ]

    path = ypath_join(get_yt_prefix(), 'msku_factors')
    schema = [
        {'required': False, "name": "model_id", "type": "uint64"},
        {'required': False, "name": "signature_i2t_v10", "type": "string"},
        {'required': False, "name": "picture_index", "type": "uint64"},
    ]

    return YtTableResource(yt_stuff, path, data=rows, attributes={'schema': schema})


@pytest.fixture(scope='module')
def output_path():
    return ypath_join(get_yt_prefix(), 'joined_msku')


@pytest.fixture(scope='module')
def join_factors_data(yt_offers_dir, yt_in_msku_factors, output_path, yt_offers_shard1, yt_offers_shard2):
    return ImageFactorsCollectorJoinFactorsData(
        input_offers_dir=yt_offers_dir,
        input_offers_tables=[yt_offers_shard1, yt_offers_shard2],
        input_factors_table=yt_in_msku_factors,
        output=output_path
    )


@pytest.yield_fixture(scope='module')
def workflow(yt_stuff, join_factors_data, tovar_tree):
    resources = {
        "options": join_factors_data,
        "tovar_tree_pb": TovarTreePb(tovar_tree),
    }

    with ImageFactorsCollectorTestEnv(**resources) as env:
        env.execute(ImageFactorsCollectorMode.JOIN_MSKU, yt_stuff, categories=str(ROOT_CATEGORY_IMAGE_FACTORS))
        env.verify()
        yield env


def test_result_table_data(workflow):
    expected = [
        {'ware_md5': 'msku_joined_factors1', 'part': 0, 'pic_number': 0, 'msku': 1},
        {'ware_md5': 'msku_joined_factors1', 'part': 0, 'pic_number': 1, 'msku': 1},
        {'ware_md5': 'msku_joined_factors2', 'part': 1, 'pic_number': 0, 'msku': 5},
    ]

    assert_that(workflow.outputs["joined_factors_table"].data, has_length(len(expected)), "Records count is icorrect")
    assert_that(
        workflow.outputs["joined_factors_table"].data,
        all_of(*[has_item(has_entries(doc)) for doc in expected]),
        "Wrong joined offers data",
    )
