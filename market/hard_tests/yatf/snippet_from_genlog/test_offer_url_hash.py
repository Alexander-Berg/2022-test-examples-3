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
        'url': None,
        'expected_offer_url_hash': '0',
    },
    {
        'offer_id': '2',
        'url': '',
        'expected_offer_url_hash': '0',
    },
    {
        'offer_id': '3',
        'url': 'https://www.labirint.ru/books/165455/?point=ym',
        'expected_offer_url_hash': '17473388429278409663',
    },
    {
        'offer_id': '4',
        'url': 'www.netshopping.ru/vcd-161715-1-165880/GoodsInfo.html',
        'expected_offer_url_hash': '700221328632402130',
    },
    {
        'offer_id': '5',
        'url': 'https://beru.ru/product/sandalii-bottilini-razmer-21-5-rozovyi/100583982734',
        'expected_offer_url_hash': '4295367246755380672',
    },
    {
        'offer_id': '6',
        'url': 'https://beru.ru/product/1000320?offerid=S5VYJ6fziTR5Cc1ACDghXw',
        'expected_offer_url_hash': '16198208574815788281',
    },
    {
        'offer_id': '7',
        'url': 'https://market.yandex.ru/product--smartfon-apple-iphone-11-128gb/558168089',
        'expected_offer_url_hash': '2161332867369272391',
    },
    {
        'offer_id': '8',
        'url': 'https://bringly.ru/product/dlia-xiaomi-huami-amazfit-a1602-milanskii-magnitnaia-petlia-iz-nerzhaveiushchei-stali-gruppa/MTI5ODcwMDYxOTM3OTIwNjMyNDI?offer=yVXskBsDLFzf7rkCmNeoeg',
        'expected_offer_url_hash': '2124520257569300615',
    },
]


@pytest.fixture(scope="module")
def genlog_rows():
    feed = []
    for data in test_data:
        offer = default_genlog(
            offer_id=data['offer_id'],
            url=data['url'],
            offer_url_hash=data['expected_offer_url_hash'],
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
def expected_offer_url_hash():
    return [
        {
            'offer_id': data['offer_id'],
            'offer_url_hash': data['expected_offer_url_hash'],
        }
        for data in test_data
    ]


def test_offer_url_hash_snippet(genlog_snippet_workflow, expected_offer_url_hash):
    for expected in expected_offer_url_hash:
        assert_that(
            genlog_snippet_workflow,
            HasOutputStateRecord({'value': expected})
        )
