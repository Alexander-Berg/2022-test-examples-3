# coding=utf-8

import pytest

from hamcrest import assert_that, has_length, has_item, all_of, has_entries

from yt.wrapper import ypath_join

from market.idx.yatf.resources.yt_table_resource import YtTableResource
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from market.idx.offers.yatf.utils.fixtures import genererate_default_picture, make_proto_lenval_pictures

from market.idx.generation.yatf.test_envs.image_factors_collector import ImageFactorsCollectorMode, ImageFactorsCollectorTestEnv
from market.idx.generation.yatf.resources.image_factors_collector.image_factors_collector_data import ImageFactorsCollectorJoinFactorsData
from market.idx.yatf.resources.tovar_tree_pb import (
    MboCategory,
    TovarTreePb,
)
from market.idx.generation.yatf.utils.fixtures import make_uc_proto_str


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


@pytest.fixture(scope='module')
def yt_offers_dir(yt_stuff):
    return ypath_join(get_yt_prefix(), 'mi3/main/offers')


@pytest.fixture(scope='module')
def yt_offers_shard1(yt_stuff, yt_offers_dir):
    rows = [
        {'ware_md5': 'joined_factors1', 'msku': 0, 'pic': make_proto_lenval_pictures([genererate_default_picture(imagename='pic1')]),
         'is_fake_msku_offer': False, 'uc': make_uc_proto_str(market_sku_id=0, category_id=ROOT_CATEGORY_IMAGE_FACTORS)},
        {'ware_md5': 'offer_without_factors', 'msku': 0, 'pic': make_proto_lenval_pictures([genererate_default_picture(imagename='pic4')]),
         'is_fake_msku_offer': False, 'uc': make_uc_proto_str(market_sku_id=0, category_id=ROOT_CATEGORY_IMAGE_FACTORS)},
        {'ware_md5': 'offer_with_msku', 'msku': 1, 'pic': make_proto_lenval_pictures([genererate_default_picture(imagename='pic5')]),
         'is_fake_msku_offer': False, 'uc': make_uc_proto_str(market_sku_id=1, category_id=ROOT_CATEGORY_IMAGE_FACTORS)},
        {'ware_md5': 'offer_disabled_category', 'msku': 0, 'pic': make_proto_lenval_pictures([genererate_default_picture(imagename='pic9')]),
         'is_fake_msku_offer': False, 'uc': make_uc_proto_str(market_sku_id=0, category_id=CATEGORY_NOT_TO_CALC_IMAGE_FACTORS)},
        {'ware_md5': 'offer_with_wrong_category', 'msku': 0, 'pic': make_proto_lenval_pictures([genererate_default_picture(imagename='pic10')]),
         'is_fake_msku_offer': False, 'uc': make_uc_proto_str(market_sku_id=0, category_id=WRONG_CATEGORY)},
    ]

    path = ypath_join(yt_offers_dir, '0000')
    schema = [
        {'required': False, "name": "ware_md5", "type": "string"},
        {'required': False, "name": "msku", "type": "uint64"},
        {'required': False, "name": "is_fake_msku_offer", "type": "boolean"},
        {'required': False, "name": "pic", "type": "string"},
        {'required': False, "name": "uc", "type": "string"},
    ]

    return YtTableResource(yt_stuff, path, data=rows, attributes={'schema': schema})


@pytest.fixture(scope='module')
def yt_offers_shard2(yt_stuff, yt_offers_dir):
    rows = [
        {'ware_md5': 'joined_factors2', 'msku': 0, 'pic': make_proto_lenval_pictures([genererate_default_picture(imagename='pic2'), genererate_default_picture(imagename='pic3')]),
         'is_fake_msku_offer': False, 'uc': make_uc_proto_str(market_sku_id=0, category_id=CHILD_CATEGORY_IMAGE_FACTORS)},
        {'ware_md5': 'fake_msku_offer', 'msku': 0, 'pic': make_proto_lenval_pictures([genererate_default_picture(imagename='pic6')]),
         'is_fake_msku_offer': True, 'uc': make_uc_proto_str(market_sku_id=0, category_id=ROOT_CATEGORY_IMAGE_FACTORS)},
        {'ware_md5': 'offer_with_pic_without_i2t', 'msku': 0, 'pic': make_proto_lenval_pictures([genererate_default_picture(imagename='pic_with_empty_i2t_8')]),
         'is_fake_msku_offer': False, 'uc': make_uc_proto_str(market_sku_id=0, category_id=ROOT_CATEGORY_IMAGE_FACTORS)},
        {'ware_md5': 'offer_without_category', 'msku': 0, 'pic': make_proto_lenval_pictures([genererate_default_picture(imagename='pic11')]),
         'is_fake_msku_offer': False, 'uc': None},
    ]

    path = ypath_join(yt_offers_dir, '0001')
    schema = [
        {'required': False, "name": "ware_md5", "type": "string"},
        {'required': False, "name": "msku", "type": "uint64"},
        {'required': False, "name": "is_fake_msku_offer", "type": "boolean"},
        {'required': False, "name": "pic", "type": "string"},
        {'required': False, "name": "uc", "type": "string"},
    ]

    return YtTableResource(yt_stuff, path, data=rows, attributes={'schema': schema})


@pytest.fixture(scope='module')
def yt_in_picrobot_success(yt_stuff):
    rows = [
        {'id': 'pic1', 'image2text_v10_embedding_str': 'embed1'},
        {'id': 'pic2', 'image2text_v10_embedding_str': 'embed2'},
        {'id': 'pic3', 'image2text_v10_embedding_str': 'embed3'},
        {'id': 'pic5', 'image2text_v10_embedding_str': 'embed5'},
        {'id': 'pic6', 'image2text_v10_embedding_str': 'embed6'},
        {'id': 'pic_without_offer_7', 'image2text_v10_embedding_str': 'embed7'},
        {'id': 'pic_with_empty_i2t_8', 'image2text_v10_embedding_str': ''},
        {'id': 'pic9', 'image2text_v10_embedding_str': 'embed9'},
        {'id': 'pic10', 'image2text_v10_embedding_str': 'embed10'},
        {'id': 'pic11', 'image2text_v10_embedding_str': 'embed11'},
    ]

    path = ypath_join(get_yt_prefix(), 'picrobot', 'success')
    schema = [
        {'required': False, "name": "id", "type": "string"},
        {'required': False, "name": "image2text_v10_embedding_str", "type": "string"},
    ]

    return YtTableResource(yt_stuff, path, data=rows, attributes={'schema': schema})


@pytest.fixture(scope='module')
def output_path():
    return ypath_join(get_yt_prefix(), 'joined_offers')


@pytest.fixture(scope='module')
def join_offers_data(yt_offers_dir, yt_in_picrobot_success, output_path, yt_offers_shard1, yt_offers_shard2):
    return ImageFactorsCollectorJoinFactorsData(
        input_offers_dir=yt_offers_dir,
        input_offers_tables=[yt_offers_shard1, yt_offers_shard2],
        input_factors_table=yt_in_picrobot_success,
        output=output_path
    )


@pytest.yield_fixture(scope='module')
def workflow(yt_stuff, join_offers_data, tovar_tree):
    resources = {
        "options": join_offers_data,
        "tovar_tree_pb": TovarTreePb(tovar_tree),
    }

    with ImageFactorsCollectorTestEnv(**resources) as env:
        env.execute(ImageFactorsCollectorMode.JOIN_OFFERS, yt_stuff, categories=str(ROOT_CATEGORY_IMAGE_FACTORS))
        env.verify()
        yield env


def test_result_table_data(workflow):
    expected = [
        {'ware_md5': 'joined_factors1', 'part': 0, 'pic_number': 0, 'image2text_v10_embedding_str': 'embed1', 'msku': None},
        {'ware_md5': 'joined_factors2', 'part': 1, 'pic_number': 0, 'image2text_v10_embedding_str': 'embed2', 'msku': None},
        {'ware_md5': 'joined_factors2', 'part': 1, 'pic_number': 1, 'image2text_v10_embedding_str': 'embed3', 'msku': None},
    ]

    assert_that(workflow.outputs["joined_factors_table"].data, has_length(len(expected)), "Records count is icorrect")
    assert_that(
        workflow.outputs["joined_factors_table"].data,
        all_of(*[has_item(has_entries(doc)) for doc in expected]),
        "Wrong joined offers data",
    )
