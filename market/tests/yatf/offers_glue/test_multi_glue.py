# coding: utf-8

import pytest
from hamcrest import (
    assert_that,
    equal_to,
    empty,
    has_length,
    has_item,
    is_not,
    has_key
)
from google.protobuf.json_format import MessageToDict

from market.idx.offers.yatf.matchers.offers_processor.env_matchers import HasGenlogRecord
from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import (
    GenlogRow,
    GenlogOffersTable
)
from market.idx.offers.yatf.resources.offers_glue.glue_tables import (
    ShortGlueTable,
    FullGlueTable
)

from market.idx.offers.yatf.test_envs.offers_glue import OffersGlueTestEnv

from market.idx.library.glue.proto.GlueConfig_pb2 import EReduceKeySchema

from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from market.idx.yatf.resources.glue_config import GlueConfig

from yt.wrapper.ypath import ypath_join
import yt.wrapper as yt


@pytest.fixture(scope="module")
def offers(request):
    return [
        GenlogRow(id=0, business_id=1, offer_id='offer_1', warehouse_id=15, shop_id=1, feed_id=1, hard2_dssm_embedding_str='1234'),
        GenlogRow(id=1, business_id=1, offer_id='offer_1', warehouse_id=16, shop_id=2, feed_id=1, hard2_dssm_embedding_str='1234'),
        GenlogRow(id=2, business_id=1, offer_id='offer_1', warehouse_id=17, shop_id=1, feed_id=1, hard2_dssm_embedding_str='1234'),
        GenlogRow(id=3, business_id=2, offer_id='offer_2', warehouse_id=14, shop_id=10, feed_id=1, hard2_dssm_embedding_str='1234'),
        GenlogRow(id=4, business_id=3, offer_id='offer_3', warehouse_id=15, shop_id=10, feed_id=1, hard2_dssm_embedding_str='1234'),
        GenlogRow(id=5, business_id=5, offer_id='no_glue_fields', warehouse_id=15, shop_id=5, feed_id=1, hard2_dssm_embedding_str='1234'),
        GenlogRow(id=6, business_id=5, offer_id='offer_3', warehouse_id=12, shop_id=6, feed_id=1, hard2_dssm_embedding_str='1234'),
        GenlogRow(id=7, business_id=7, offer_id='no_glue_fields', warehouse_id=15, shop_id=7, feed_id=1, hard2_dssm_embedding_str='1234')
    ]


@pytest.fixture(scope='module')
def glue_config():
    return GlueConfig(
    {'Fields': [
        {
            'glue_id': 1,
            'declared_cpp_type': 'DOUBLE',
            'target_name': 'value_1',
            'is_from_datacamp': False,
            'owner': 'marketindexer',
            'source_name': 'ext1',
            "data_limit_per_table": 777,
            "data_limit_per_offer": 888,
            "destination_table_path": "converted_table1",
            'source_table_path': 'external_table1',
            'source_field_path': 'column_a',
            'reduce_key_schema': EReduceKeySchema.SHORT_OFFER_ID,

        },
        {
            'glue_id': 2,
            'declared_cpp_type': 'INT64',
            'target_name': 'value_2',
            'is_from_datacamp': False,
            'owner': 'marketindexer',
            'source_name': 'ext2',
            "data_limit_per_table": 777,
            "data_limit_per_offer": 888,
            "destination_table_path": "converted_table2",
            'source_table_path': 'external_table2',
            'source_field_path': 'column_a',
            'reduce_key_schema': EReduceKeySchema.SHORT_OFFER_ID,
        },
        {
            'glue_id': 3,
            'declared_cpp_type': 'UINT64',
            'target_name': 'value_3',
            'is_from_datacamp': False,
            'owner': 'marketindexer',
            'source_name': 'ext3',
            "data_limit_per_table": 777,
            "data_limit_per_offer": 888,
            "destination_table_path": "converted_table3",
            'source_table_path': 'external_table3',
            'source_field_path': 'column_a',
            'reduce_key_schema': EReduceKeySchema.SHORT_OFFER_ID,
        },
        {
            'glue_id': 4,
            'declared_cpp_type': 'DOUBLE',
            'target_name': 'value_4',
            'is_from_datacamp': False,
            'owner': 'marketindexer',
            'source_name': 'ext4',
            "data_limit_per_table": 777,
            "data_limit_per_offer": 888,
            "destination_table_path": "converted_table4",
            'source_table_path': 'external_table4',
            'source_field_path': 'column_a',
            'reduce_key_schema': EReduceKeySchema.FULL_OFFER_ID,
        },
        {
            'glue_id': 5,
            'declared_cpp_type': 'STRING',
            'target_name': 'hard2_dssm_embedding_str',
            'is_from_datacamp': False,
            'owner': 'marketindexer',
            'source_name': 'ext5',
            "data_limit_per_table": 777,
            "data_limit_per_offer": 888,
            "destination_table_path": "converted_table5",
            'source_table_path': 'external_table5',
            'source_field_path': 'column_a',
            'reduce_key_schema': EReduceKeySchema.FULL_OFFER_ID,
            'use_as_genlog_field' : True
        },
    ]}, 'glue_config.json')


@pytest.fixture(scope="module")
def extern_glues():
    return {
        "first": [
            {'business_id': 1, 'offer_id': 'offer_1', 'glue_fields': [{'glue_id': yt.yson.YsonUint64(1), 'double_value': 1.0}]},
            {'business_id': 5, 'offer_id': 'offer_3', 'glue_fields': [{'glue_id': yt.yson.YsonUint64(1), 'double_value': 4.0}]},
            {'business_id': 5, 'offer_id': 'unexist_offer_id', 'glue_fields': [{'glue_id': yt.yson.YsonUint64(1), 'double_value': 4.0}]},
            {'business_id': 20, 'offer_id': 'unexist_offer_id', 'glue_fields': [{'glue_id': yt.yson.YsonUint64(1), 'double_value': 4.0}]},
        ],
        "second": [
            {'business_id': 1, 'offer_id': 'offer_1', 'glue_fields': [{'glue_id': yt.yson.YsonUint64(2), 'int64_value': yt.yson.YsonInt64(1)}]},
            {'business_id': 2, 'offer_id': 'unexist_offer_id', 'glue_fields': [{'glue_id': yt.yson.YsonUint64(1), 'int64_value': yt.yson.YsonInt64(1)}]},
            {'business_id': 3, 'offer_id': 'offer_3', 'glue_fields': [{'glue_id': yt.yson.YsonUint64(2), 'int64_value': yt.yson.YsonInt64(4)}]},
            {'business_id': 5, 'offer_id': 'offer_3', 'glue_fields': [{'glue_id': yt.yson.YsonUint64(3), 'int64_value': yt.yson.YsonInt64(4)}]},
        ],
        "third": [
            {'business_id': 1, 'offer_id': 'offer_1', 'glue_fields': [{'glue_id': yt.yson.YsonUint64(3), 'uint64_value': yt.yson.YsonUint64(1)}]},
            {'business_id': 100500, 'offer_id': 'unexist_offer_id', 'glue_fields': [{'glue_id': yt.yson.YsonUint64(3), 'uint64_value': yt.yson.YsonUint64(1)}]},
        ],

    }


@pytest.fixture(scope="module")
def full_extern_glues():
    return {
        'full_first': [
            {'business_id': 1, 'offer_id': 'offer_1', 'shop_id': 2, 'warehouse_id': 16, 'glue_fields': [{'glue_id': yt.yson.YsonUint64(4), 'float_value': yt.yson.YsonDouble(1.0)}]},
            {'business_id': 1, 'offer_id': 'offer_1', 'shop_id': 1, 'warehouse_id': 17, 'glue_fields': [{'glue_id': yt.yson.YsonUint64(4), 'float_value': yt.yson.YsonDouble(2.0)}]},
            {'business_id': 1, 'offer_id': 'offer_1', 'shop_id': 2, 'warehouse_id': 100500, 'glue_fields': [{'glue_id': yt.yson.YsonUint64(4), 'float_value': yt.yson.YsonDouble(3.0)}]},
            {'business_id': 5, 'offer_id': 'offer_3', 'shop_id': 6, 'warehouse_id': 12, 'glue_fields': [{'glue_id': yt.yson.YsonUint64(4), 'float_value': yt.yson.YsonDouble(4.0)}]},
        ],
        'named_genlog_field': [
            {'business_id': 1, 'offer_id': 'offer_1', 'shop_id': 1, 'warehouse_id': 15, 'glue_fields': [{'glue_id': yt.yson.YsonUint64(5), 'string_value': "offer_1"}]},
            {'business_id': 1, 'offer_id': 'offer_1', 'shop_id': 1, 'warehouse_id': 17, 'glue_fields': [{'glue_id': yt.yson.YsonUint64(5), 'string_value': "offer_1"}]},
            {'business_id': 2, 'offer_id': 'offer_2', 'shop_id': 10, 'warehouse_id': 14, 'glue_fields': [{'glue_id': yt.yson.YsonUint64(5), 'string_value': "offer_2"}]},
            {'business_id': 3, 'offer_id': 'offer_3', 'shop_id': 10, 'warehouse_id': 15, 'glue_fields': [{'glue_id': yt.yson.YsonUint64(5), 'string_value': "offer_3"}]},
        ]
    }


@pytest.yield_fixture(scope="module")
def workflow(yt_server, offers, extern_glues, full_extern_glues, glue_config):
    resources = {
        'genlogs': GenlogOffersTable(yt_server,  ypath_join(get_yt_prefix(), 'in', '0000'), offers),
        'glue_config': glue_config
    }

    resources.update({
        name: FullGlueTable(
            yt_stuff=yt_server,
            path=ypath_join(get_yt_prefix(), 'externdata', name),
            data=offers,
        ) for name, offers in full_extern_glues.iteritems()
    })

    resources.update({
        name: ShortGlueTable(
            yt_stuff=yt_server,
            path=ypath_join(get_yt_prefix(), 'externdata', name),
            data=offers,
        ) for name, offers in extern_glues.iteritems()
    })

    output_table_path = ypath_join(get_yt_prefix(), 'out', '0000')

    with OffersGlueTestEnv(
        yt_server,
        output_genlog_yt_path=output_table_path,
        input_glue_yt_paths=[resources[name].get_path() for name in resources if name != 'genlogs' and name != 'glue_config'],
        **resources
    ) as env:
        env.execute()
        env.verify()
        yield env


def test_total_count(workflow, offers):
    assert_that(len(workflow.genlog), equal_to(len(offers)))


@pytest.mark.parametrize(
    "idx,expected_cnt",
    [
        (0, 3),
        (1, 4),
        (2, 4),
        (4, 1),
        (6, 3),
    ],
    ids=[
        "id0",
        "id1",
        "id2",
        "id4",
        "id6",
    ]
)
def test_field_count(workflow, offers, idx, expected_cnt):
    offer = offers[idx]
    assert_that(
        workflow,
        HasGenlogRecord(
            {
            'business_id': offer['business_id'],
            'offer_id': offer['offer_id'],
            'shop_id': offer['shop_id'],
            'warehouse_id': offer['warehouse_id'],
            'glue_fields': has_length(expected_cnt)
            }
        )
    )


def test_empty_fields(workflow):
    expected_offers = [
        {'business_id': 7, 'offer_id': 'no_glue_fields'},
        {'business_id': 5, 'offer_id': 'no_glue_fields'},
    ]

    for offer in expected_offers:
        assert_that(
            workflow,
            HasGenlogRecord(
                {
                'business_id': offer['business_id'],
                'offer_id': offer['offer_id'],
                'glue_fields': empty()
                }
            )
        )


def test_named_field_not_in_glue_fields(workflow):
    for offer in workflow.genlog:
        ids = [glue.glue_id for glue in offer.glue_fields]
        assert_that(
            ids,
            is_not(has_item(5))
        )


def test_named_fields(workflow):
    expected_offers = [
        {'business_id': 1, 'offer_id': 'offer_1', 'shop_id': 1, 'warehouse_id': 15, 'hard2_dssm_embedding_str': 'offer_1'},
        {'business_id': 1, 'offer_id': 'offer_1', 'shop_id': 1, 'warehouse_id': 17, 'hard2_dssm_embedding_str': 'offer_1'},
        {'business_id': 2, 'offer_id': 'offer_2', 'shop_id': 10, 'warehouse_id': 14, 'hard2_dssm_embedding_str': 'offer_2'},
        {'business_id': 3, 'offer_id': 'offer_3', 'shop_id': 10, 'warehouse_id': 15, 'hard2_dssm_embedding_str': 'offer_3'},

        {'business_id': 1, 'offer_id': 'offer_1', 'shop_id': 2, 'warehouse_id': 16, 'hard2_dssm_embedding_str': '1234'},
        {'business_id': 5, 'offer_id': 'no_glue_fields', 'shop_id': 5, 'warehouse_id': 15, 'hard2_dssm_embedding_str': '1234'},
        {'business_id': 5, 'offer_id': 'offer_3', 'shop_id': 6, 'warehouse_id': 12, 'hard2_dssm_embedding_str': '1234'},
        {'business_id': 7, 'offer_id': 'no_glue_fields', 'shop_id': 7, 'warehouse_id': 15, 'hard2_dssm_embedding_str': '1234'},
    ]

    for offer in expected_offers:
        assert_that(
            workflow,
            HasGenlogRecord(
                {
                'business_id': offer['business_id'],
                'offer_id': offer['offer_id'],
                'shop_id': offer['shop_id'],
                'warehouse_id': offer['warehouse_id'],
                'hard2_dssm_embedding_str':  offer['hard2_dssm_embedding_str'],
                }
            )
        )


def test_glue_keys_not_empty(workflow):
    for offer in workflow.genlog:
        for g in offer.glue_fields:
            assert_that(
                MessageToDict(g, preserving_proto_field_name=True),
                has_key('glue_id')
            )
