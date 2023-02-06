# coding: utf-8

""" Проверяем Contex https://wiki.yandex-team.ru/market/report/infra/abtcontent
"""
from hamcrest import assert_that
import pytest


from market.idx.offers.yatf.matchers.offers_processor.env_matchers import (
    HasGenlogRecordRecursive,
)
from market.idx.offers.yatf.test_envs.offers_processor import (
    OffersProcessorTestEnv
)
from market.idx.offers.yatf.utils.fixtures import default_blue_genlog
from market.proto.feedparser.OffersData_pb2 import ContexInfo
from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogOffersTable
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix

from yt.wrapper.ypath import ypath_join
import yt.wrapper as yt


@pytest.fixture(scope="module")
def offers_data():
    return [
        {
            'title': 'offer of basic msku',  # basic=original, dont use original because of stop words))
            'feed_id': 100500,
            'offer_id': '2a',
            'contex_info': {
                'experiment_id': 'some_exp',
                'experimental_msku_id': yt.yson.YsonUint64(2000)
            },
            'randx': 22277,
        },
        {
            'title': 'cloned offer',
            'feed_id': 100500,
            'offer_id': '2a',
            'contex_info': {
                'experiment_id': 'some_exp',
                'original_msku_id': yt.yson.YsonUint64(1000)
            },
            'randx': 22277,
        },
    ]


@pytest.fixture(scope="module")
def genlog_rows(offers_data):
    return [
        default_blue_genlog(**data)
        for data in offers_data
    ]


@pytest.fixture(scope="module")
def genlog_table(yt_server, genlog_rows):
    genlog_table = GenlogOffersTable(yt_server,  ypath_join(get_yt_prefix(), '0000'), genlog_rows)
    genlog_table.dump()
    return genlog_table


@pytest.yield_fixture(scope="module")
def offers_processor_workflow(yt_server, genlog_table):
    input_table_paths = [genlog_table.get_path()]

    with OffersProcessorTestEnv(
            yt_server,
            use_genlog_scheme=True,
            input_table_paths=input_table_paths,
    ) as env:
        env.execute()
        env.verify()
        yield env


def test_original(offers_processor_workflow):
    """Проверка id фида, оффера и contex_info с использованием HasGenlogRecord
    из матчеров offers-indexer"""
    assert_that(
        offers_processor_workflow,
        HasGenlogRecordRecursive(
            {
                'feed_id': 100500,
                'offer_id': '2a',
                'contex_info': ContexInfo(
                    experiment_id='some_exp',
                    experimental_msku_id=2000
                ),
                'randx': 22277,
            }
        )
    )


def test_experimental(offers_processor_workflow):
    """Проверка id фида, оффера и contex_info с использованием HasGenlogRecord
    из матчеров offers-indexer"""
    assert_that(
        offers_processor_workflow,
        HasGenlogRecordRecursive(
            {
                'feed_id': 100500,
                'offer_id': '2a',
                'contex_info': ContexInfo(
                    experiment_id='some_exp',
                    original_msku_id=1000
                ),
            }
        )
    )
