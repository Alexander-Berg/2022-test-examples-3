# coding=utf-8

from hamcrest import (
    assert_that,
    equal_to,
)
import pytest

from market.idx.generation.yatf.matchers.snippet_diff_builder.env_matchers import HasOutputStateRecord
from market.idx.generation.yatf.test_envs.snippet_diff_builder import OfferStateRow, SnippetDiffBuilderTestEnv
from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogRow

from market.idx.offers.yatf.test_envs.offers_processor import OffersProcessorTestEnv
from market.idx.offers.yatf.utils.fixtures import default_genlog
from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogOffersTable
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from yt.wrapper.ypath import ypath_join


@pytest.yield_fixture(
    scope='module',
    params=[
        (False, False, 'offer_for_rotation_contains_nowhere'),
        (False, True, 'offer_for_rotation_contains_state'),
        (True, False, 'offer_for_rotation_contains_genlog'),
        (True, True, 'offer_for_rotation_contains_ganlog_and_state'),
    ],
    ids=[
        'offer_for_rotation_contains_nowhere',
        'offer_for_rotation_contains_state',
        'offer_for_rotation_contains_genlog',
        'offer_for_rotation_contains_ganlog_and_state',
    ]
)
def for_rotation(request):
    yield request.param


@pytest.yield_fixture(scope='module')
def workflow_params(for_rotation):
    in_genlog, in_state, name = for_rotation
    genlogs = []
    state = []

    if in_genlog:
        genlogs.append(
            GenlogRow(
                id=0,
                feed_id=1,
                offer_id=1,
                for_rotation=True
            )
        )
    if in_state:
        state.append(
            OfferStateRow(
                feed_id=1,
                offer_id=1,
                title='some state title',
                props={
                    'for_rotation': '1'
                }
            )
        )

    yield genlogs, state, name


def test_hadling_offer_for_rotation(workflow_params, yt_server):
    genlogs, state, name = workflow_params
    with SnippetDiffBuilderTestEnv(
            name,
            yt_server,
            offers=[],
            models=[],
            genlogs=genlogs,
            state=state,
            deleted_ttl=5
    ) as env:
        env.execute()
        env.verify()

        for row in env.output_state_table:
            assert_that(row['deleted'], equal_to(False))  # history data
            assert_that(row['value']['for_rotation'], equal_to('1'))

        for row in env.output_diff_table:
            assert_that(row['deleted'], equal_to(False))
            assert_that(row['value']['for_rotation'], equal_to('1'))


test_data = [
    {
        'offer_id': '1',
        'for_rotation': True,
    },
    {
        'offer_id': '2',
        'for_rotation': False,
    },
    {
        'offer_id': '3',
    }
]


@pytest.yield_fixture(scope='module')
def genlog_rows():
    offers = []
    for data in test_data:
        if 'for_rotation' in data:
            offer = default_genlog(
                offer_id=data['offer_id'],
                for_rotation=data['for_rotation'],
            )
        else:
            offer = default_genlog(
                offer_id=data['offer_id'],
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


def make_for_rotation(data):
    if data.get('for_rotation', None) is None:
        return None
    return '1' if data['for_rotation'] else '0'


def test_for_rotation_snippet(genlog_snippet_workflow):
    expected_for_rotation = [
        {
            'offer_id': x['offer_id'],
            'for_rotation': make_for_rotation(x),
        }
        for x in test_data
    ]
    for expected in expected_for_rotation:
        assert_that(
            genlog_snippet_workflow,
            HasOutputStateRecord({'value': expected})
        )
