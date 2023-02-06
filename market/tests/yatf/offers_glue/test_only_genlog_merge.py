# coding: utf-8

import pytest
from hamcrest import (
    assert_that,
    equal_to
)

from market.idx.offers.yatf.test_envs.offers_glue import OffersGlueTestEnv
from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import (
    GenlogRow,
    GenlogOffersTable,
)

from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from market.idx.yatf.resources.glue_config import GlueConfig

from yt.wrapper.ypath import ypath_join


@pytest.fixture(scope="module")
def offers(request):
    return [
        GenlogRow(id=0, business_id=1, feed_id=1, offer_id='offer_1', shop_id=1, warehouse_id=15),
        GenlogRow(id=1, business_id=1, feed_id=1, offer_id='offer_1', shop_id=1, warehouse_id=16),
        GenlogRow(id=2, business_id=2, feed_id=1, offer_id='offer_2', shop_id=1, warehouse_id=14),
        GenlogRow(id=3, business_id=3, feed_id=1, offer_id='offer_3', shop_id=1, warehouse_id=15),
        GenlogRow(id=4, business_id=5, feed_id=1, offer_id='offer_3', shop_id=1, warehouse_id=12),
    ]


@pytest.fixture(scope='module')
def glue_config():
    return GlueConfig(
        {'Fields': []}
    )


@pytest.yield_fixture(scope="module")
def workflow(yt_server, offers, glue_config):
    resources = {
        'genlogs': GenlogOffersTable(yt_server,  ypath_join(get_yt_prefix(), 'in', '0000'), offers),
        'glue_config': glue_config
    }

    output_table_path = ypath_join(get_yt_prefix(), 'out', '0000')

    with OffersGlueTestEnv(
        yt_server,
        output_genlog_yt_path=output_table_path,
        merge_after_reduce=True,
        **resources
    ) as env:
        env.execute()
        env.verify()
        yield env


def test_start(workflow, offers):
    assert_that(len(workflow.genlog), equal_to(len(offers)))
