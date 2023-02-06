# coding=utf-8

from hamcrest import assert_that
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
        'expected_manufacturer_country_ids': None,
    },
    {
        'offer_id': '2',
        'manufacturer_country_ids': '',
        'expected_manufacturer_country_ids': [],
    },
    {
        'offer_id': '3',
        'manufacturer_country_ids': '123',
        'expected_manufacturer_country_ids': ['123'],
    },
    {
        'offer_id': '4',
        'manufacturer_country_ids': 'No country',
        'expected_manufacturer_country_ids': [],
    },
    {
        'offer_id': '5',
        'manufacturer_country_ids': '1,2,3',
        'expected_manufacturer_country_ids': ['1', '2', '3'],
    },
    {
        'offer_id': '6',
        'manufacturer_country_ids': '1 2 3',
        'expected_manufacturer_country_ids': [],
    },
    {
        'offer_id': '7',
        'manufacturer_country_ids': 'one,two',
        'expected_manufacturer_country_ids': [],
    },
    {
        'offer_id': '8',
        'manufacturer_country_ids': 'one,2,три',
        'expected_manufacturer_country_ids': ['2'],
    },
]


@pytest.fixture(scope="module")
def genlog_rows():
    feed = []
    for data in test_data:
        offer = default_genlog(
            offer_id=data['offer_id'],
            manufacturer_country_ids=data.get('manufacturer_country_ids', None),
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


def make_manufacturer_country_ids(data):
    if data is None:
        return None
    return ','.join(data)


def test_manufacturer_country_ids_snippet(genlog_snippet_workflow):
    expected_manufacturer_country_ids = [
        {
            'offer_id': x['offer_id'],
            'manufacturer_country_ids': make_manufacturer_country_ids(
                x['expected_manufacturer_country_ids']
            ),
        }
        for x in test_data
    ]
    for expected in expected_manufacturer_country_ids:
        assert_that(
            genlog_snippet_workflow,
            HasOutputStateRecord({'value': expected})
        )
