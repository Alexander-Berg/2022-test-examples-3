# coding: utf-8

"""
Проверяется прокидывание поля sales_notes в снипеты и в indexarc.
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


SALES_NOTES = 'Useful sales notes №",°\'`'

test_data = [
    {
        'offer_id': '1',
        'sales_notes': SALES_NOTES,
        'expected_snippet': SALES_NOTES,
    },
    {
        'offer_id': '2',
        'sales_notes': None,
        'expected_snippet': None,
    },
    {
        'offer_id': '3',
        'sales_notes': SALES_NOTES,
        'expected_snippet': SALES_NOTES,
    },
]


@pytest.yield_fixture(scope='module')
def genlog_rows():
    return [
        default_genlog(
            offer_id=data['offer_id'],
            sales_notes=data['sales_notes'],
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


@pytest.yield_fixture(scope="module")
def expected_sales_notes():
    return [
        {
            'offer_id': x['offer_id'],
            'sales_notes': x['expected_snippet']
        }
        for x in test_data
    ]


def test_sales_notes_snippet(genlog_snippet_workflow, expected_sales_notes):
    for expected in expected_sales_notes:
        assert_that(
            genlog_snippet_workflow,
            HasOutputStateRecord({'value': expected})
        )
