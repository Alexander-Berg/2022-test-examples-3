# coding=utf-8

from hamcrest import (
    assert_that,
)
import pytest

from market.idx.generation.yatf.matchers.snippet_diff_builder.env_matchers import HasOutputStateRecord
from market.idx.generation.yatf.test_envs.snippet_diff_builder import SnippetDiffBuilderTestEnv

from market.idx.offers.yatf.test_envs.offers_processor import OffersProcessorTestEnv
from market.idx.offers.yatf.utils.fixtures import default_genlog
from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogOffersTable
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from yt.wrapper.ypath import ypath_join


test_data = [
    {
        'offer_id': '1',
        'seller_warranty': None,
        'expected_seller_warranty': None,
    },
    {
        'offer_id': '2',
        'seller_warranty': '',
        'expected_seller_warranty': '',
    },
    {
        'offer_id': '3',
        'seller_warranty': 'P1Y2M10DT2H30M',
        'expected_seller_warranty': 'P1Y2M10DT2H30M',
    },
    {
        'offer_id': '4',
        'seller_warranty': 'false',
        'expected_seller_warranty': 'false'
    },
    {
        'offer_id': '5',
        'seller_warranty': 'P1Y2M10DT2H30M',
        'expected_seller_warranty': 'P1Y2M10DT2H30M'
    },
]


@pytest.fixture(scope="module")
def genlog_rows():
    offers = []
    for data in test_data:
        offer = default_genlog(
            offer_id=data['offer_id'],
            seller_warranty=data['seller_warranty']
        )
        offers.append(offer)
    return offers


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
def expected_seller_warranty():
    return [
        {
            'offer_id': x['offer_id'],
            'seller_warranty': x['expected_seller_warranty'],
        }
        for x in test_data
    ]


def test_seller_warranty_snippet(genlog_snippet_workflow, expected_seller_warranty):
    for expected in expected_seller_warranty:
        assert_that(
            genlog_snippet_workflow,
            HasOutputStateRecord({'value': expected})
        )
