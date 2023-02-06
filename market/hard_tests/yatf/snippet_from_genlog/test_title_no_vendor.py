# coding=utf-8

from hamcrest import (
    assert_that,
)
import pytest

from market.idx.generation.yatf.matchers.snippet_diff_builder.env_matchers import (
    HasOutputStateRecord,
)

from market.idx.generation.yatf.test_envs.snippet_diff_builder import SnippetDiffBuilderTestEnv

from market.idx.offers.yatf.test_envs.offers_processor import OffersProcessorTestEnv
from market.idx.offers.yatf.utils.fixtures import default_genlog
from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogOffersTable
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from yt.wrapper.ypath import ypath_join


test_data = [
    {
        'offer_id': 'oifone',
        'title': 'Apple iPhone 10x',
        'title_no_vendor': 'iPhone 10x',
    },
    {
        'offer_id': 'oipad',
        'title': 'Apple iPad 100',
        'title_no_vendor': 'iPad 100',
    }
]


@pytest.yield_fixture(scope='module')
def genlog_rows():
    return [
        default_genlog(
            offer_id=data['offer_id'],
            title=data['title'],
            title_no_vendor=data['title_no_vendor']
        )
        for data in test_data
    ]


@pytest.fixture(scope="module")
def genlog_table(yt_server, genlog_rows):
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


@pytest.yield_fixture(scope='module')
def expected_title_no_vendor():
    return [
        {
            'offer_id': x['offer_id'],
            'title_no_vendor': x['title_no_vendor'],
        }
        for x in test_data
    ]


def test_title_no_vendor_snippet(genlog_snippet_workflow, expected_title_no_vendor):
    for expected in expected_title_no_vendor:
        assert_that(
            genlog_snippet_workflow,
            HasOutputStateRecord({'value': expected})
        )
