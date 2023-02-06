# coding: utf-8

"""
Проверяется проставление top_queries_offer и top_queries_all
"""

import pytest
from hamcrest import assert_that, equal_to

from market.idx.offers.yatf.test_envs.offers_processor import (
    OffersProcessorTestEnv
)
from market.idx.offers.yatf.resources.offers_indexer.top_query import TopQuery
from market.idx.offers.yatf.utils.fixtures import default_genlog, generate_binary_price_dict
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from yt.wrapper.ypath import ypath_join
from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogOffersTable


TOP_QUERY_OFFER_DATA = 'TOP_QUERY_OFFER_DATA'
TOP_QUERIES_ALL_DATA = 'TOP_QUERIES_ALL_DATA'

TEST_DATA = {
    'title': '    test    title   ',
    'feed_id': 100500,
    'offer_id': '2a',
    'binary_price': generate_binary_price_dict(12345),
    'top_queries_offer': TopQuery(TOP_QUERY_OFFER_DATA).base64,
    'top_queries_all': TopQuery(TOP_QUERIES_ALL_DATA).base64,
}


@pytest.fixture(scope="module")
def genlog_rows():
    return [default_genlog(**TEST_DATA)]


@pytest.fixture(scope="module")
def genlog_table(yt_server, genlog_rows):
    genlog_table = GenlogOffersTable(yt_server,  ypath_join(get_yt_prefix(), '0000'), genlog_rows)
    genlog_table.dump()
    return genlog_table


@pytest.yield_fixture(scope="module")
def offers_processor_workflow(yt_server, genlog_table):
    input_table_paths = [genlog_table.get_path()]

    with OffersProcessorTestEnv(
            yt_server,
            use_genlog_scheme=True,
            input_table_paths=input_table_paths,
    ) as env:
        env.execute()
        env.verify()
        yield env


def test_genlog_has_only_one_record(offers_processor_workflow):
    assert_that(len(offers_processor_workflow.genlog), equal_to(1))


def test_top_queries_offer(offers_processor_workflow):
    offer = offers_processor_workflow.genlog[0]
    top_queries_offer = TopQuery.parse(offer.top_queries_offer)
    assert_that(
        top_queries_offer.record[0].query,
        equal_to(TOP_QUERY_OFFER_DATA)
    )


def test_top_queries_all(offers_processor_workflow):
    offer = offers_processor_workflow.genlog[0]
    top_queries_all = TopQuery.parse(offer.top_queries_all)
    assert_that(
        top_queries_all.record[0].query,
        equal_to(TOP_QUERIES_ALL_DATA)
    )
