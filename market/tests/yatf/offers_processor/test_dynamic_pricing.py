# coding: utf-8

"""Проверки опций динамического ценообразования - dynamic pricing
- подаем на вход одну таблицу
- проверяем, что на выходе есть непустая таблица
"""

from market.idx.offers.yatf.test_envs.offers_processor import (
    OffersProcessorTestEnv
)
from market.idx.offers.yatf.matchers.offers_processor.env_matchers import (
    HasGenlogRecord
)
from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogOffersTable
from market.idx.offers.yatf.utils.fixtures import default_genlog, generate_binary_price_dict
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix

from yt.wrapper.ypath import ypath_join
from hamcrest import assert_that
import pytest


TEST_DATA = {
    'title': '    test    title   ',
    'feed_id': 100500,
    'offer_id': '2a',
    'binary_price': generate_binary_price_dict(12345),
    'dynamic_pricing_type': 1,
    'dynamic_pricing_threshold_is_percent': False,
    'dynamic_pricing_threshold_value': 1234567000,
}


@pytest.fixture(scope="module")
def genlog_rows():
    return [default_genlog(**TEST_DATA)]


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


def test_table_exist(offers_processor_workflow):
    """Тест проверяет, что на выходе есть таблица"""
    assert len(offers_processor_workflow.output_tables_list) > 0


def test_table_has_offer(offers_processor_workflow):
    """Тест проверяет, что на выходе есть хотя бы один оффер"""
    assert len(offers_processor_workflow.genlog) > 0


def test_feed_offer_id(offers_processor_workflow):
    """Проверка id фида и оффера с использованием HasGenlogRecord
    из матчеров offers-indexer"""
    assert_that(
        offers_processor_workflow,
        HasGenlogRecord(
            {
                'feed_id': 100500,
                'offer_id': '2a',
                'dynamic_pricing_type': 1,
                'dynamic_pricing_threshold_is_percent': False,
                'dynamic_pricing_threshold_value': 1234567000
            }
        )
    )


def test_std_err_table(offers_processor_workflow):
    assert not offers_processor_workflow.has_std_err_table
