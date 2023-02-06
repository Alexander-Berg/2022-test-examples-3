#!/usr/bin/env python
# coding: utf-8
import pytest

from market.idx.pylibrary.offer_flags.flags import OfferFlags
from market.idx.offers.yatf.utils.fixtures import default_blue_genlog
from market.idx.offers.yatf.test_envs.offers_processor import OffersProcessorTestEnv

from hamcrest import assert_that
from market.idx.offers.yatf.matchers.offers_processor.env_matchers import HasGenlogRecord
from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogOffersTable
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from yt.wrapper.ypath import ypath_join


@pytest.fixture(scope="module")
def genlog_rows():
    light_offer = default_blue_genlog(
        offer_id='light_offer',
        weight=19.999,
        length=1.0,
        width=1.0,
        height=1.0,
        flags=OfferFlags.DEPOT | OfferFlags.STORE | OfferFlags.IS_FULFILLMENT | OfferFlags.AVAILABLE
    )

    large_offer = default_blue_genlog(
        offer_id='large_size_offer',
        weight=20.0,
        length=1.0,
        width=1.0,
        height=1.0,
        flags=OfferFlags.DEPOT | OfferFlags.STORE | OfferFlags.IS_FULFILLMENT | OfferFlags.AVAILABLE
    )

    return [
        light_offer,
        large_offer,
    ]


@pytest.fixture(scope="module")
def genlog_table(yt_server, genlog_rows):
    genlog_table = GenlogOffersTable(yt_server,  ypath_join(get_yt_prefix(), '0000'), genlog_rows)
    genlog_table.dump()
    return genlog_table


@pytest.yield_fixture(scope="module")
def workflow(yt_server, genlog_table):
    input_table_paths = [genlog_table.get_path()]

    with OffersProcessorTestEnv(
            yt_server,
            drop_offers_with_no_sizes=True,
            use_genlog_scheme=True,
            input_table_paths=input_table_paths,
    ) as env:
        env.execute()
        env.verify()
        yield env


def test_table_exist(workflow):
    """Тест проверяет, что на выходе есть таблица"""
    assert len(workflow.output_tables_list) > 0


def test_table_has_offer(workflow):
    """Тест проверяет, что на выходе есть хотя бы один оффер"""
    assert len(workflow.genlog) > 0


@pytest.mark.parametrize(
    'offer_id, is_large_size',
    [
        ('light_offer', False),
        ('large_size_offer', True),
    ]
)
def test_large_size_flags(workflow, offer_id, is_large_size):
    """
    Проверяем наличие флага КГТ
    """
    expected_flags = OfferFlags.DEPOT
    expected_flags |= OfferFlags.AVAILABLE
    expected_flags |= OfferFlags.STORE
    expected_flags |= OfferFlags.MODEL_COLOR_WHITE
    expected_flags |= OfferFlags.IS_FULFILLMENT
    expected_flags |= OfferFlags.CPC
    expected_flags |= OfferFlags.BLUE_OFFER
    if is_large_size:
        expected_flags |= OfferFlags.IS_LARGE_SIZE

    assert_that(
        workflow,
        HasGenlogRecord(
            {
                'offer_id': offer_id,
                'flags': expected_flags,
            }
        ),
    )
