# coding: utf-8

"""
Проверяется, что raw_vendor сохраняется в vendor_string
MARKETINDEXER-29409
"""

from hamcrest import assert_that
import pytest

from market.idx.generation.yatf.matchers.snippet_diff_builder.env_matchers import HasOutputStateRecord

from market.idx.generation.yatf.test_envs.snippet_diff_builder import SnippetDiffBuilderTestEnv

from market.idx.offers.yatf.test_envs.offers_processor import OffersProcessorTestEnv
from market.idx.offers.yatf.utils.fixtures import default_genlog
from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogOffersTable
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from yt.wrapper.ypath import ypath_join


RAW_VENDOR = 'OAO "Kulebyaka" entertainment'
genlog_rows = [default_genlog(vendor_string=RAW_VENDOR)]


@pytest.fixture(scope="module")
def genlog_table(yt_server):
    genlog_table = GenlogOffersTable(yt_server,  ypath_join(get_yt_prefix(), '0000'), genlog_rows)
    genlog_table.dump()
    return genlog_table


@pytest.yield_fixture(scope='module')
def offers_processor_workflow(yt_server, genlog_table):
    input_table_paths = [genlog_table.get_path()]

    with OffersProcessorTestEnv(
            yt_server,
            use_genlog_scheme=True,
            input_table_paths=input_table_paths
    ) as env:
        env.execute()
        yield env


@pytest.yield_fixture(scope='module')
def genlog_snippet_workflow(yt_server, offers_processor_workflow):
    genlogs = []
    for id, glProto in enumerate(offers_processor_workflow.genlog_dicts):
        genlogs.append(glProto)

    with SnippetDiffBuilderTestEnv(
        'genlog_snippet_workflow',
        yt_server,
        offers=[],
        genlogs=genlogs,
        models=[],
        state=[],
    ) as env:
        env.execute()
        env.verify()
        yield env


def test_raw_vendor_snippet(genlog_snippet_workflow):
    expected = {
        'feed_id': str(genlog_rows[0]['feed_id']),
        'vendor_string': genlog_rows[0]['vendor_string']
    }
    assert_that(
        genlog_snippet_workflow,
        HasOutputStateRecord({'value': expected})
    )
