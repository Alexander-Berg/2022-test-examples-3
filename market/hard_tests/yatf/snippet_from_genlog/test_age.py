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
        'incoming_age': '0',
        'incoming_age_unit': 'month',
        'expected_age': '0',
        'expected_age_unit': 'month'
    },
    {
        'offer_id': '2',
        'incoming_age': '1',
        'incoming_age_unit': 'month',
        'expected_age': '1',
        'expected_age_unit': 'month'
    },
    {
        'offer_id': '3',
        'incoming_age': '12',
        'incoming_age_unit': 'month',
        'expected_age': '12',
        'expected_age_unit': 'month'
    },
    {
        'offer_id': '4',
        'incoming_age': '0',
        'incoming_age_unit': 'year',
        'expected_age': '0',
        'expected_age_unit': 'year'
    },
    {
        'offer_id': '5',
        'incoming_age': '18',
        'incoming_age_unit': 'year',
        'expected_age': '18',
        'expected_age_unit': 'year'
    },
    {
        'offer_id': '6',
        'expected_age': None,
        'expected_age_unit': None
    }
]


@pytest.fixture(scope="module")
def genlog_rows():
    offers = []
    for data in test_data:
        offer = default_genlog(
            offer_id=data['offer_id'],
            age=data.get('incoming_age', None),
            age_unit=data.get('incoming_age_unit', None)
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


def test_age_snippet(genlog_snippet_workflow):
    expected_ages = [
        {
            'offer_id': data['offer_id'],
            'age': data.get('expected_age', None),
            'age_unit': data.get('expected_age_unit', None)
        }
        for data in test_data
    ]
    for expected in expected_ages:
        assert_that(
            genlog_snippet_workflow,
            HasOutputStateRecord({'value': expected})
        )
