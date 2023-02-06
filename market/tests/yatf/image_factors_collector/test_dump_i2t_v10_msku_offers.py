# coding=utf-8

import os
import pytest
import yatest.common

from yt.wrapper import ypath_join

from hamcrest import assert_that, equal_to
from market.idx.yatf.resources.yt_table_resource import YtTableResource
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogRow
from market.idx.yatf.utils.genlog import genlog_table_schema

from market.idx.generation.yatf.test_envs.image_factors_collector import ImageFactorsCollectorMode, ImageFactorsCollectorTestEnv
from market.idx.generation.yatf.resources.image_factors_collector.image_factors_collector_data import ImageFactorsCollectorDumpData

from market.idx.yatf.matchers.env_matchers import HasOutputFiles, HasOutputFileSize


IMAGE2TEXT_V10_SIZE = 200


def _get_compressed_image2text_v10_str(hex_val, length=IMAGE2TEXT_V10_SIZE):
    l = [str(hex_val) for i in range(length)]
    return "".join(l)


@pytest.fixture(scope='module')
def yt_genlog_shard(yt_stuff):
    rows = [
        GenlogRow(shard_id=0, id=0, feed_id=1, offer_id='1', ware_md5='ware_md5_1'),
        GenlogRow(shard_id=0, id=1, feed_id=1, offer_id='2', ware_md5='ware_md5_2'),
        GenlogRow(shard_id=0, id=2, feed_id=1, offer_id='3', ware_md5='ware_md5_3'),
        GenlogRow(shard_id=0, id=3, feed_id=1, offer_id='4', ware_md5='ware_md5_4'),
    ]
    path = ypath_join(get_yt_prefix(), 'genlog_0')
    return YtTableResource(yt_stuff, path, data=rows, attributes={'schema': genlog_table_schema()})


@pytest.fixture(scope='module')
def yt_in_offer_factors(yt_stuff):
    rows = [
        {'doc_id': 0, 'image2text_v10_embedding_str': _get_compressed_image2text_v10_str(1), 'msku': 1},
        {'doc_id': 1, 'image2text_v10_embedding_str': _get_compressed_image2text_v10_str(2)},
        {'doc_id': 2, 'image2text_v10_embedding_str': _get_compressed_image2text_v10_str(1), 'msku': 1},
        # дампим
        {'doc_id': 3, 'image2text_v10_embedding_str': _get_compressed_image2text_v10_str(3), 'msku': 0},
        # слишком большой doc_id (в генлоге нет столько офферов) - не дампим)
        {'doc_id': 4, 'image2text_v10_embedding_str': _get_compressed_image2text_v10_str(4), 'msku': 2},
        # нет doc_id - не дампим
        {'doc_id': None, 'image2text_v10_embedding_str': _get_compressed_image2text_v10_str(2), 'msku': 2},
        # нет эмбедов - не дампим
        {'doc_id': None, 'image2text_v10_embedding_str': None, 'msku': 2},
        # неверная длина эмбедов - не дампим
        {'doc_id': None, 'image2text_v10_embedding_str': _get_compressed_image2text_v10_str(3, length=199), 'msku': 2},
    ]

    path = ypath_join(get_yt_prefix(), 'offer_factors')
    schema = [
        {'required': False, "name": "doc_id", "type": "uint32"},
        {'required': False, "name": "image2text_v10_embedding_str", "type": "string"},
        {'required': False, "name": "msku", "type": "uint64"},
    ]

    return YtTableResource(yt_stuff, path, data=rows, attributes={'schema': schema})


@pytest.fixture(scope='module')
def output_dir():
    return yatest.common.output_path()


@pytest.fixture(scope='module')
def dump_data(yt_genlog_shard, yt_in_offer_factors, output_dir):
    return ImageFactorsCollectorDumpData(
        input_factors_table=yt_in_offer_factors,
        input_genlog_table=yt_genlog_shard,
        output_dir=output_dir
    )


@pytest.yield_fixture(scope='module')
def workflow(yt_stuff, dump_data):
    resources = {
        "options": dump_data
    }

    with ImageFactorsCollectorTestEnv(**resources) as env:
        env.execute(ImageFactorsCollectorMode.DUMP_IMAGE2TEXT_V10, yt_stuff)
        env.verify()
        yield env


def test_index_file(workflow):
    file = os.path.basename(workflow.outputs["image_i2t_v12_dssm_index"])
    expected_size = 4 * 4
    workflow.verify([HasOutputFiles({file}), HasOutputFileSize(file, expected_size)])

    expexted_data = b'\x00\x00\x00\x00\x01\x00\x00\x00\x00\x00\x00\x00\x02\x00\x00\x00'  # offset=0 + offset=1 + offset=0 + offset=2
    buf = b''
    with open(workflow.outputs["image_i2t_v12_dssm_index"], 'rb') as fn:
        buf = fn.read(expected_size)
    assert_that(buf, equal_to(expexted_data))


def test_embeds_file(workflow):
    file = os.path.basename(workflow.outputs["image_i2t_v12_dssm_binary"])
    expected_size = 3 * IMAGE2TEXT_V10_SIZE
    workflow.verify([HasOutputFiles({file}), HasOutputFileSize(file, expected_size)])

    expexted_data = bytearray(b'')
    for i in range(IMAGE2TEXT_V10_SIZE):
        expexted_data += b'1'    # 1st embed (msku)
    for i in range(IMAGE2TEXT_V10_SIZE):
        expexted_data += b'2'    # 2nd embed (offer)
    for i in range(IMAGE2TEXT_V10_SIZE):
        expexted_data += b'3'    # 3nd embed (offer)
    buf = b''
    with open(workflow.outputs["image_i2t_v12_dssm_binary"], 'rb') as fn:
        buf = fn.read(expected_size)
    assert_that(buf, equal_to(expexted_data))
