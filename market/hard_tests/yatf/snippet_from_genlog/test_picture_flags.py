# coding=utf-8

from hamcrest import assert_that
import pytest

from market.idx.generation.yatf.matchers.snippet_diff_builder.env_matchers import HasOutputStateRecord
from market.idx.generation.yatf.test_envs.snippet_diff_builder import SnippetDiffBuilderTestEnv

from market.idx.offers.yatf.test_envs.offers_processor import OffersProcessorTestEnv

from market.idx.offers.yatf.utils.fixtures import (
    default_genlog,
)
from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogOffersTable
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from yt.wrapper.ypath import ypath_join


test_data = [
    {
        'offer_id': '1',
        'expected_picture_flags': None,
    },
    {
        'offer_id': '2',
        'picture_urls': ['http://ya.ru'],
        'expected_picture_flags': None,
    },
    {
        'offer_id': '3',
        'picture_urls': ['http://shock_content.jpg'],
        'expected_picture_flags': [10],
    },
    {
        'offer_id': '4',
        'picture_urls': ['http://market.jpg', 'https://beru.png'],
        'expected_picture_flags': [100, 200],
    },
]


@pytest.yield_fixture(scope='module')
def genlog_rows():
    offers = []
    for data in test_data:
        offer = default_genlog(
            offer_id=data['offer_id'],
            picture_urls=data.get('picture_urls', None),
            picture_flags=make_pic(data['expected_picture_flags']),
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
            input_table_paths=input_table_paths,
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
def expected_picture_flags_arc():
    return [
        {
            'offer_id': data['offer_id'],
            'picture_flags': data['expected_picture_flags'],
        }
        for data in test_data
    ]


def make_pic(data):
    if data is None:
        return None
    return '|'.join([str(i) for i in data])


@pytest.yield_fixture(scope="module")
def expected_picture_flags_snippet():
    return [
        {
            'offer_id': data['offer_id'],
            'picture_flags': make_pic(data['expected_picture_flags']),
        }
        for data in test_data
    ]


def test_picture_flags_snippet(
        genlog_snippet_workflow,
        expected_picture_flags_snippet
):
    for expected in expected_picture_flags_snippet:
        assert_that(
            genlog_snippet_workflow,
            HasOutputStateRecord({'value': expected})
        )
