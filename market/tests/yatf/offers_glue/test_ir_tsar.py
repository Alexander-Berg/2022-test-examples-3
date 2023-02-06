# coding: utf-8

import pytest
from hamcrest import (
    assert_that,
    equal_to,
    empty,
    has_length
)

from market.idx.offers.yatf.matchers.offers_processor.env_matchers import HasGenlogRecord
from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogRow, GenlogOffersTable
from market.idx.offers.yatf.resources.offers_glue.glue_tables import ShortGlueTable

from market.idx.offers.yatf.test_envs.offers_glue import OffersGlueTestEnv


from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from market.idx.yatf.resources.glue_config import GlueConfig

from yt.wrapper.ypath import ypath_join
import yt.wrapper as yt


@pytest.fixture(scope="module")
def offers(request):
    return [
        GenlogRow(
            id=0,
            feed_id=1,
            offer_id='offer_1',
            warehouse_id=15,
            shard_id=0,
            business_id=2,
        ),
        GenlogRow(
            id=1,
            feed_id=1,
            offer_id='offer_2',
            warehouse_id=14,
            shard_id=0,
            business_id=2,
        ),
        GenlogRow(
            id=2,
            feed_id=1,
            offer_id='offer_3',
            warehouse_id=15,
            shard_id=0,
            business_id=3,
        ),
        GenlogRow(
            id=3,
            feed_id=1,
            offer_id='offer_1',
            warehouse_id=15,
            shard_id=0,
            business_id=5,
        ),
        GenlogRow(
            id=4,
            feed_id=1,
            offer_id='offer_3',
            warehouse_id=15,
            shard_id=0,
            business_id=5,
        ),
    ]


@pytest.fixture(scope="module")
def ir_offer_index():
    return [
        {'business_id': 1, 'offer_id': 'offer_1', 'glue_fields': [{'glue_id': yt.yson.YsonUint64(1), 'bool_value': True}]},
        {'business_id': 2, 'offer_id': 'offer_2', 'glue_fields': [{'glue_id': yt.yson.YsonUint64(1), 'bool_value': True}]},
        {'business_id': 3, 'offer_id': 'offer_3', 'glue_fields': [{'glue_id': yt.yson.YsonUint64(1), 'bool_value': False}]},
        {'business_id': 5, 'offer_id': 'offer_3', 'glue_fields': [{'glue_id': yt.yson.YsonUint64(1), 'bool_value': False}]},
    ]


@pytest.fixture(scope='module')
def glue_config():
    return GlueConfig(
        {'Fields': []}
    )


@pytest.yield_fixture(scope="module")
def workflow(yt_server, offers, ir_offer_index, glue_config):
    resources = {
        'genlogs': GenlogOffersTable(yt_server,  ypath_join(get_yt_prefix(), 'in', '0000'), offers),
        'yt_ir_by_offer_table': ShortGlueTable(
            yt_stuff=yt_server,
            path=ypath_join(get_yt_prefix(), 'externdata', 'input_ir_tsar_offer_index'),
            data=ir_offer_index,
        ),
        'glue_config': glue_config
    }

    output_table_path = ypath_join(get_yt_prefix(), 'out', '0000')

    with OffersGlueTestEnv(
        yt_server,
        output_genlog_yt_path=output_table_path,
        input_glue_yt_paths=[
            resources['yt_ir_by_offer_table'].get_path(),
        ],
        **resources
    ) as env:
        env.execute()
        env.verify()
        yield env


def test_start(workflow, offers):
    assert_that(len(workflow.genlog), equal_to(len(offers)))


def test_is_tsar(workflow):
    expected_offers = [
        {'business_id': 2, 'offer_id': 'offer_2', 'is_not_tsar': True},
        {'business_id': 3, 'offer_id': 'offer_3', 'is_not_tsar': False},
        {'business_id': 5, 'offer_id': 'offer_3', 'is_not_tsar': False},
    ]

    for offer in expected_offers:
        assert_that(
            workflow,
            HasGenlogRecord(
                {
                'business_id': offer['business_id'],
                'offer_id': offer['offer_id'],
                'glue_fields': has_length(1)
                }
            )
        )


def test_empty_fields(workflow):
    expected_offers = [
        {'business_id': 2, 'offer_id': 'offer_1'},
        {'business_id': 5, 'offer_id': 'offer_1'},
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
