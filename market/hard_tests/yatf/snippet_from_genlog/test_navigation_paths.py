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
import yt.wrapper as yt


def generate_navigation_path(path):
    return {'nodes': [{'id': yt.yson.YsonUint64(node_id), 'name': node_name} for node_id, node_name in path]}


@pytest.fixture(scope='module')
def genlog_rows():
    return [
        default_genlog(
            offer_id='1',
            navigation_paths=[
                generate_navigation_path([(1, 'Super cat'), (2, 'Cat')]),
                generate_navigation_path([(3, 'Uber cat')]),
            ],
        ),
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


def test_navigation_paths_snippet(genlog_snippet_workflow):
    expected = {
        'offer_id': '1',
        'navigation_node_ids': r'1\2\3',
        'navigation_node_names': r'Super cat\Cat\Uber cat',
        'navigation_path_lengths': r'2\1',
    }

    assert_that(
        genlog_snippet_workflow,
        HasOutputStateRecord({'value': expected})
    )
