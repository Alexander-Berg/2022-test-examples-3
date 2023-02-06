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
        'feed_id': 101967,
        'shop_category_path': u'Бытовая техника',
        'shop_category_path_ids': '0',
        'shop_category_id': '0',
        'categid': '0',
    },
    {
        'offer_id': '2',
        'feed_id': 101967,
        'shop_category_path': u'Бытовая техника',
        'shop_category_path_ids': '1',
        'shop_category_id': '1',
        'categid': '1',
    },
    {
        'offer_id': '3',
        'feed_id': 101967,
        'shop_category_path': u'Бытовая техника',
        'shop_category_path_ids': '123456789012345678',
        'shop_category_id': '123456789012345678',
        'categid': '123456789012345678',
    },
    {
        'offer_id': '4',
        'feed_id': 101967,
        'shop_category_path': u'Кат1\\Кат2\\Кат3\\Кат4',
        'shop_category_path_ids': '1200\\800\\400\\200',
        'shop_category_id': '200',
        'categid': '200'
    },
    {
        'offer_id': '5',
        'feed_id': 101967,
        'shop_category_path': '',
        'shop_category_path_ids': '200',
        'shop_category_id': '200',
        'categid': '200'
    },
    {
        'offer_id': '6',
        'feed_id': 101967,
        'shop_category_path': '',
        'shop_category_path_ids': '',
        'shop_category_id': None,
        'categid': None
    },
]


@pytest.fixture(scope="module")
def genlog_rows():
    offers = []
    for data in test_data:
        offer = default_genlog(
            offer_id=data['offer_id'],
            shop_category_path=data['shop_category_path'],
            shop_category_path_ids=data['shop_category_path_ids'],
            shop_category_id=data['shop_category_id'],
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


def test_categid_snippet(genlog_snippet_workflow):
    expected_categids = [
        {
            'feed_id': str(x['feed_id']),
            'offer_id': x['offer_id'],
            'categid': x.get('categid', None),
        }
        for x in test_data
    ]
    for expected in expected_categids:
        assert_that(
            genlog_snippet_workflow,
            HasOutputStateRecord({'value': expected})
        )
