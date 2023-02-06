#!/usr/bin/env python
# coding: utf-8
"""
Проверяем проставление флага б/у товаров на офферы
"""

import pytest
from hamcrest import assert_that

from market.idx.offers.yatf.matchers.offers_processor.env_matchers import HasGenlogRecord
from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogOffersTable
from market.idx.offers.yatf.test_envs.offers_processor import OffersProcessorTestEnv
from market.idx.offers.yatf.utils.fixtures import default_genlog
from market.idx.pylibrary.offer_flags.flags import OfferFlags
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix

from yt.wrapper.ypath import ypath_join


@pytest.fixture(scope="module")
def genlog_rows():
    return [
        default_genlog(
            offer_id='1',
            flags=OfferFlags.IS_RESALE,
        ),
        default_genlog(
            offer_id='2',
            flags=0,
        )
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
            use_genlog_scheme=True,
            input_table_paths=input_table_paths,
    ) as env:
        env.execute()
        env.verify()
        yield env


def test_is_resale(workflow):
    expected_flags = OfferFlags.MODEL_COLOR_WHITE | OfferFlags.CPC
    expected_flags |= OfferFlags.IS_RESALE

    assert_that(
        workflow,
        HasGenlogRecord(
            {
                'offer_id': '1',
                'flags': expected_flags,
            }
        )
    )


def test_not_is_resale(workflow):
    expected_flags = OfferFlags.MODEL_COLOR_WHITE | OfferFlags.CPC

    assert_that(
        workflow,
        HasGenlogRecord(
            {
                'offer_id': '2',
                'flags': expected_flags,
            }
        )
    )
