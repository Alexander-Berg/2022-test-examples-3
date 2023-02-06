# coding=utf-8
"""
Тест используется для проверки работы вызова indexerf join_sku_features
"""

import pytest

from hamcrest import assert_that, equal_to, has_item, has_entries

from yt import wrapper as yt

from market.proto.feedparser.deprecated.OffersData_pb2 import Offer
from market.proto.common.common_pb2 import PriceExpression
from market.idx.yatf.resources.yt_table_resource import YtTableResource

from market.idx.generation.indexerf.yatf.test_env import YtIndexErfTestEnv, ErfMode
from market.idx.generation.indexerf.yatf.resources.yt_erf_data import YTJoinSkuFeaturesErfData
from market.idx.generation.indexerf.yatf.resources.erf_output import YtErfOutput

from mapreduce.yt.python.table_schema import extract_column_attributes
from market.proto.indexer import GenerationLog_pb2


@pytest.fixture(scope='module')
def blue_offers_shard0():
    return [
        Offer(
            genlog=GenerationLog_pb2.Record(
                ware_md5='WareId_1_1',
                market_sku=1,
                binary_price=PriceExpression(price=100000000, id='RUR'),    # price_variant = 10
            ),
        ),
        Offer(
            genlog=GenerationLog_pb2.Record(
                ware_md5='WareId_1_2',
                market_sku=1,
                binary_price=PriceExpression(price=124000000, id='RUR'),    # price_variant = 10
            ),
        ),
        Offer(
            genlog=GenerationLog_pb2.Record(
                ware_md5='WareId_1_3',
                market_sku=1,
                binary_price=PriceExpression(price=126000000, id='RUR'),    # price_variant = 15
            ),
        ),
        Offer(
            genlog=GenerationLog_pb2.Record(
                ware_md5='WareId_1_4',
                market_sku=1,
                binary_price=PriceExpression(price=50000000, id='RUR'),    # price_variant = 10
            ),
        )
    ]


@pytest.fixture(scope='module')
def blue_offers_shard1():
    return [
        Offer(
            genlog=GenerationLog_pb2.Record(
                ware_md5='WareId_2_1',
                market_sku=2,
                binary_price=PriceExpression(price=800000000, id='RUR'),    # price_variant = 20
            ),
        ),
        Offer(
            genlog=GenerationLog_pb2.Record(
                ware_md5='WareId_3_1',
                market_sku=3,
                binary_price=PriceExpression(price=100000000, id='RUR'),    # no elasticity data -> should not see in result
            ),
        ),
        Offer(
            genlog=GenerationLog_pb2.Record(
                market_sku=4,    # no ware_md5 -> should not see in result
                binary_price=PriceExpression(price=100000000, id='RUR'),
            ),
        ),
        Offer(
            genlog=GenerationLog_pb2.Record(
                ware_md5='WareId_4_2',  # no market_sku -> should not see in result
                binary_price=PriceExpression(price=100000000, id='RUR'),
            ),
        ),
        Offer(
            genlog=GenerationLog_pb2.Record(
                ware_md5='WareId_4_3',  # no binary_price -> should not see in result
                market_sku=4,
            ),
        ),
        Offer(
            genlog=GenerationLog_pb2.Record(
                ware_md5='WareId_4_4',  # binary_price not in RUR -> should not see in result
                market_sku=4,
                binary_price=PriceExpression(price=100000000, id='USD'),
            ),
        ),
        Offer(
            genlog=GenerationLog_pb2.Record(
                ware_md5='WareId_5_1',
                market_sku=5,    # wrong elasticity data -> should not see in result
                binary_price=PriceExpression(price=100000000, id='RUR'),
            ),
        ),
        Offer(
            genlog=GenerationLog_pb2.Record(
                ware_md5='WareId_0_1',
                market_sku=0,    # wrong elasticity data -> should not see in result
                binary_price=PriceExpression(price=100000000, id='RUR'),
            ),
        ),
    ]


blue_offer_shards_dir = '//indexer/stratocaster/mi'


blue_offers_schema = [
    dict(name='offer', type='string'),
]


@pytest.fixture(scope='module')
def blue_offers_table0(yt_stuff, blue_offers_shard0):
    table_path = yt.ypath_join(blue_offer_shards_dir, '0000')
    rows = [
        dict(offer=o.SerializeToString()) for o in blue_offers_shard0
    ]
    return YtTableResource(yt_stuff, table_path, data=rows, attributes={'schema': blue_offers_schema})


@pytest.fixture(scope='module')
def blue_offers_table1(yt_stuff, blue_offers_shard1):
    table_path = yt.ypath_join(blue_offer_shards_dir, '0001')
    rows = [
        dict(offer=o.SerializeToString()) for o in blue_offers_shard1
    ]
    return YtTableResource(yt_stuff, table_path, data=rows, attributes={'schema': blue_offers_schema})


elasticity_table_path = "//monetize/elasticity"


elasticity_table_schema = [
    dict(name='sku', type='int64'),
    dict(name='price_variant', type='int64'),
    dict(name='demand_mean', type='double'),
]


@pytest.fixture(scope='module')
def blue_offers_tables(blue_offers_table0, blue_offers_table1):
    return [blue_offers_table0, blue_offers_table1]


@pytest.fixture(scope='module')
def elasticity_table(yt_stuff):
    rows = []
    rows.append({'sku': 1, 'price_variant': 10, 'demand_mean': 0.1})
    rows.append({'sku': 1, 'price_variant': 15, 'demand_mean': 0.15})
    rows.append({'sku': 2, 'price_variant': 20, 'demand_mean': 0.2})
    rows.append({'sku': 4, 'price_variant': 100, 'demand_mean': 10.0})
    rows.append({'sku': 5, 'price_variant': -30, 'demand_mean': 30.0})   # wrong data because price_variant < 0
    rows.append({'sku': 0, 'price_variant': 30, 'demand_mean': 30.0})   # wrong data because sku == 0
    return YtTableResource(yt_stuff, elasticity_table_path, data=rows, attributes={'schema': elasticity_table_schema})


@pytest.fixture(scope='module')
def input_data(blue_offers_tables, elasticity_table):
    res = YTJoinSkuFeaturesErfData(blue_offer_shards_dir, blue_offers_tables, elasticity_table)
    return res


# output info and YT tables
@pytest.fixture(scope='module')
def erf_output():
    erf_path = '//indexer/stratocaster/result-table-elasticity'
    return YtErfOutput(erf_path, 0)   # result table not sharded


# Execution of binary
@pytest.yield_fixture(scope='module')
def workflow(yt_stuff, input_data, erf_output):
    resources = {
        "input": input_data,
        "output": erf_output
    }

    with YtIndexErfTestEnv(**resources) as env:
        env.execute(ErfMode.JOIN_SKU_FEATURES, yt_stuff)
        env.verify()
        yield env


# Helper functions
@pytest.fixture(scope='module')
def result_elasticity_features_table(workflow):
    return workflow.result_tables[0]


def should_see_only_expected_features(res_data, expected_data):
    for item in expected_data:
        assert_that(res_data, has_item(has_entries(item)), "No match for result data")
    assert_that(len(res_data), equal_to(len(expected_data)), "No match for result data length")


# Tests
def test_result_table_count(workflow, erf_output):
    assert_that(len(workflow.result_tables), equal_to(1), "output number result tables != 1")


def test_result_table_exists(yt_stuff, result_elasticity_features_table):
    assert_that(yt_stuff.get_yt_client().exists(result_elasticity_features_table.get_path()), 'Table doesn\'t exist')


def test_result_table_schema(result_elasticity_features_table):
    assert_that(extract_column_attributes(list(result_elasticity_features_table.schema)),
                equal_to([
                    {'required': False, "name": "ware_md5", "type": "string"},
                    {'required': False, "name": "part", "type": "uint64"},
                    {'required': False, "name": "elasticity_dm", "type": "double"},
                ]), "Schema is incorrect")


def test_elasticity_result(result_elasticity_features_table):
    expected_rows = [
        {'ware_md5': 'WareId_1_1', 'part': 0, 'elasticity_dm': 0.1},
        {'ware_md5': 'WareId_1_2', 'part': 0, 'elasticity_dm': 0.1},
        {'ware_md5': 'WareId_1_3', 'part': 0, 'elasticity_dm': 0.15},
        {'ware_md5': 'WareId_1_4', 'part': 0, 'elasticity_dm': 0.1},
        {'ware_md5': 'WareId_2_1',  'part': 1, 'elasticity_dm': 0.2},
    ]
    should_see_only_expected_features(result_elasticity_features_table.data, expected_rows)
