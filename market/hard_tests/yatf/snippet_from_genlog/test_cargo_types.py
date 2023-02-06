# coding: utf-8

"""
Проверяется проставление свойства cargo_type для красных офферов

UPD: судя по коду, не только для красных.
"""

from hamcrest import assert_that
import pytest

from market.idx.generation.yatf.matchers.snippet_diff_builder.env_matchers import HasOutputStateRecord
from market.idx.generation.yatf.test_envs.snippet_diff_builder import SnippetDiffBuilderTestEnv

from market.idx.offers.yatf.test_envs.offers_processor import OffersProcessorTestEnv

from market.idx.offers.yatf.utils.fixtures import (
    default_genlog,
    genererate_default_pictures,
)
from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogOffersTable
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from yt.wrapper.ypath import ypath_join


# Red status values (NO=1/SBX=2/REAL=4)
REAL_VALUE = 4
SBX_VALUE = 2
NO_VALUE = 1

test_data = [
    {
        'offer_id': 'NO_VALUE',
        'cargo_types': [621, 622, 623],
    }
]


@pytest.yield_fixture(scope='module')
def genlog_rows():
    offers = []
    for data in test_data:
        offer = default_genlog(
            offer_id=data['offer_id'],
            cargo_types=data['cargo_types'],
            pictures=genererate_default_pictures(),
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


def make_cargo_types(data):
    if data is None:
        return None
    return '/'.join([str(i) for i in data])


def test_cargo_types_snippet(genlog_snippet_workflow):
    expected_cargo_types = [
        {
            'offer_id': x['offer_id'],
            'cargo_types': make_cargo_types(x['cargo_types']),
        }
        for x in test_data
    ]

    for expected in expected_cargo_types:
        assert_that(
            genlog_snippet_workflow,
            HasOutputStateRecord({'value': expected})
        )
