# coding: utf-8

import pytest
from hamcrest import (
    assert_that,
    equal_to,
    has_length,
    has_items,
)

from market.idx.offers.yatf.matchers.offers_processor.env_matchers import HasGenlogRecord
from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import (
    GenlogRow,
    GenlogOffersTable
)
from market.idx.offers.yatf.resources.offers_glue.glue_tables import (
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
        GenlogRow(id=0, business_id=1, offer_id='offer_1', warehouse_id=15, shop_id=1, feed_id=1),
        GenlogRow(id=1, business_id=2, offer_id='offer_2', warehouse_id=14, shop_id=10, feed_id=1),
        GenlogRow(id=2, business_id=3, offer_id='offer_3', warehouse_id=15, shop_id=10, feed_id=1),
    ]


@pytest.fixture(scope='module')
def glue_config():
    return GlueConfig(
    {'Fields': [
        {
            'glue_id': 5,
            'declared_cpp_type': 'STRING',
            'target_name': 'binary_promos_md5_base64',
            'is_from_datacamp': False,
            'owner': 'marketindexer',
            'source_name': 'ext5',
            "data_limit_per_table": 777,
            "data_limit_per_offer": 888,
            "destination_table_path": "converted_table5",
            'source_table_path': 'external_table5',
            'source_field_path': 'column_a',
            'reduce_key_schema': EReduceKeySchema.FULL_OFFER_ID,
            'use_as_genlog_field' : True,
            'is_array': True
        },
    ]}, 'glue_config.json')


@pytest.fixture(scope="module")
def full_extern_glues():
    return {
        'named_genlog_field': [
            {
                'business_id': 1, 'offer_id': 'offer_1', 'shop_id': 1, 'warehouse_id': 15,
                'glue_fields': [
                    {'glue_id': yt.yson.YsonUint64(5), 'string_value': "1"},
                    {'glue_id': yt.yson.YsonUint64(5), 'string_value': "2"},
                    {'glue_id': yt.yson.YsonUint64(5), 'string_value': "3"},
                ]
            },
            {
                'business_id': 2,
                'offer_id': 'offer_2',
                'shop_id': 10,
                'warehouse_id': 14,
                'glue_fields': [
                    {'glue_id': yt.yson.YsonUint64(5), 'string_value': "1"},
                    {'glue_id': yt.yson.YsonUint64(5), 'string_value': "2"},
                ]
            },
            {
                'business_id': 3,
                'offer_id': 'unexisted_id',
                'shop_id': 10,
                'warehouse_id': 15,
                'glue_fields': [
                    {'glue_id': yt.yson.YsonUint64(5), 'string_value': "1"},
                    {'glue_id': yt.yson.YsonUint64(5), 'string_value': "2"},
                    {'glue_id': yt.yson.YsonUint64(5), 'string_value': "3"},
                ]
            },
        ]
    }


@pytest.yield_fixture(scope="module")
def workflow(yt_server, offers, full_extern_glues, glue_config):
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
        (1, 2),
        (2, 0),
    ],
    ids=[
        "id0",
        "id1",
        "id2",
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
            'binary_promos_md5_base64': has_length(expected_cnt)
            }
        )
    )


def test_named_fields(workflow):
    expected_offers = [
        {'business_id': 1, 'offer_id': 'offer_1', 'shop_id': 1, 'warehouse_id': 15, 'binary_promos_md5_base64': ['1', '2', '3']},
        {'business_id': 2, 'offer_id': 'offer_2', 'shop_id': 10, 'warehouse_id': 14, 'binary_promos_md5_base64': ['1', '2']},
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
                'binary_promos_md5_base64':  has_items(*offer['binary_promos_md5_base64']),
                }
            )
        )
