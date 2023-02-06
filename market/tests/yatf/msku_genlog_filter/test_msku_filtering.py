# coding: utf-8

import pytest

from hamcrest import assert_that, has_item, has_entries, equal_to, is_not

from market.idx.generation.yatf.test_envs.msku_genlog_filter import MskuGenlogFilterTestEnv
from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogRow


# sequence_number здесь называется id
@pytest.fixture(scope="module")
def offers(request):
    return [
        # MSKU (должен отфильтровтаься) и два оффера
        GenlogRow(shard_id=0, id=0, feed_id=1, offer_id='1', is_fake_msku_offer=False, market_sku=1),
        GenlogRow(shard_id=0, id=1, feed_id=1, offer_id='2', is_fake_msku_offer=False, market_sku=1, is_blue_offer=True),
        GenlogRow(shard_id=0, id=2, feed_id=1, offer_id='3', is_fake_msku_offer=True, market_sku=1),
    ]


@pytest.yield_fixture(scope="module")
def workflow(yt_server, offers):
    with MskuGenlogFilterTestEnv(yt_server, offers) as env:
        env.execute()
        env.verify()
        yield env


def test_offers_count(workflow):
    assert_that(len(workflow.filtered_genlog_rows), equal_to(2))  # остались только офферы


def test_msku_filtering(workflow):
    assert_that(workflow.filtered_genlog_rows, is_not(has_item(has_entries({'is_fake_msku_offer': True}))), "Msku was not filtered")
