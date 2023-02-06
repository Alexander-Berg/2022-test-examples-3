# coding=utf-8

from hamcrest import (
    assert_that,
)
import pytest

from market.idx.generation.yatf.matchers.arc_indexer.env_matchers import HasArchiveEntry
from market.idx.generation.yatf.matchers.snippet_diff_builder.env_matchers import HasOutputStateRecord

from market.idx.generation.yatf.test_envs.arc_indexer import IndexarcTestEnv
from market.idx.generation.yatf.test_envs.mr_mindexer import MrMindexerBuildTestEnv, MrMindexerMergeTestEnv
from market.idx.generation.yatf.test_envs.snippet_diff_builder import SnippetDiffBuilderTestEnv
from market.idx.generation.yatf.resources.mr_mindexer.mr_mindexer_helpers import MrMindexerMergeOptions, MrMindexerMergeIndexType

from market.idx.offers.yatf.test_envs.offers_processor import OffersProcessorTestEnv
from market.idx.offers.yatf.utils.fixtures import default_genlog
from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogOffersTable
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from yt.wrapper.ypath import ypath_join


test_data = [
    {
        'offer_id': '1',
        'model_title': 'apple watch 5 nike',
    },
    {
        'offer_id': '2',
        'model_title': 'iphone XX Pro glide extra plus',
    },
    {
        'offer_id': '3',
    }
]


@pytest.fixture(scope="module")
def genlog_rows():
    offers = []
    for data in test_data:
        offer = default_genlog(
            offer_id=data['offer_id'],
            model_title=data.get('model_title', None),
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
def mr_mindexer_direct_arc(yt_server, offers_processor_workflow):
    with MrMindexerBuildTestEnv() as build_env:
        build_env.execute_from_offers_list(yt_server, offers_processor_workflow.genlog_dicts)
        build_env.verify()

        resourses = {
            'merge_options': MrMindexerMergeOptions(
                input_portions_path=build_env.yt_index_portions_path,
                part=0,
                index_type=MrMindexerMergeIndexType.DIRECT_ARCH,
            ),
        }

        with MrMindexerMergeTestEnv(**resourses) as env:
            env.execute(yt_server)
            env.verify()
            env.outputs['indexarc'].load()
            yield env


@pytest.yield_fixture(scope="module")
def arc_workflow(mr_mindexer_direct_arc):
    resources = {
        'indexarc': mr_mindexer_direct_arc.outputs['indexarc']
    }
    with IndexarcTestEnv(**resources) as env:
        env.execute()
        env.verify()
        yield env


@pytest.yield_fixture(scope="module")
def expected_model_title():
    return [
        {
            'offer_id': x['offer_id'],
            'model_title': x.get('model_title'),
        }
        for x in test_data
    ]


def test_model_title_arc(arc_workflow, expected_model_title):
    for expected in expected_model_title:
        assert_that(
            arc_workflow,
            HasArchiveEntry(expected)
        )


def test_model_title_snippet(genlog_snippet_workflow, expected_model_title):
    for expected in expected_model_title:
        assert_that(
            genlog_snippet_workflow,
            HasOutputStateRecord({'value': expected})
        )
