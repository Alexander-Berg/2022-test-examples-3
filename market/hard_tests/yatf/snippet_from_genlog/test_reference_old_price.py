# coding=utf-8

from hamcrest import assert_that
import pytest

from market.idx.generation.yatf.matchers.snippet_diff_builder.env_matchers import HasOutputStateRecord
from market.idx.generation.yatf.test_envs.snippet_diff_builder import SnippetDiffBuilderTestEnv

from market.idx.offers.yatf.test_envs.offers_processor import OffersProcessorTestEnv

from market.idx.offers.yatf.utils.fixtures import (
    default_genlog,
    generate_binary_price_dict,
)
from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogOffersTable
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from yt.wrapper.ypath import ypath_join


PRICE_LIMIT = 500

test_data = [
    {
        'offer_id': '1',
        'binary_reference_old_price': generate_binary_price_dict(PRICE_LIMIT),
        'expected_reference_old_price': str(PRICE_LIMIT),
    },
    {
        'offer_id': '2',
        'expected_reference_old_price': None,
    }
]


@pytest.fixture(scope="module")
def genlog_rows():
    feed = []
    for data in test_data:
        offer = default_genlog(
            offer_id=data['offer_id'],
            binary_reference_old_price=data.get('binary_reference_oldprice', None),
            snippet_reference_old_price=data['expected_reference_old_price']
        )
        feed.append(offer)
    return feed


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
def expected_binary_reference_oldprice():
    return [
        {
            'offer_id': data['offer_id'],
            'reference_old_price': data['expected_reference_old_price'],
        }
        for data in test_data
    ]


def test_binary_reference_oldprice_snippet(genlog_snippet_workflow, expected_binary_reference_oldprice):
    for expected in expected_binary_reference_oldprice:
        assert_that(
            genlog_snippet_workflow,
            HasOutputStateRecord({'value': expected})
        )
