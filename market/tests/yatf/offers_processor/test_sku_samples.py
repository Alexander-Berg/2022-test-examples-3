#!/usr/bin/env python
# coding: utf-8
"""
Проверяем проставление флага пробника на офферы
"""

import pytest
from hamcrest import assert_that

from market.idx.offers.yatf.utils.fixtures import default_genlog
from market.idx.offers.yatf.resources.offers_indexer.sku_sample import SkuSamples
from market.idx.offers.yatf.test_envs.offers_processor import OffersProcessorTestEnv
from market.idx.offers.yatf.matchers.offers_processor.env_matchers import HasGenlogRecordRecursive
from market.idx.pylibrary.offer_flags.flags import OfferFlags
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from yt.wrapper.ypath import ypath_join
from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogOffersTable


test_data = [
    {
        'offer_id': '1',
        'sku_id': 1,
        'is_sample': True,
    },
    {
        'offer_id': '2',
        'sku_id': 2,
        'is_sample': False,
    },
]


@pytest.fixture(scope="module")
def sku_sample():
    return SkuSamples([1])


@pytest.fixture(scope="module")
def genlog_rows():
    rows = []
    for data in test_data:
        offer = default_genlog(
            offer_id=data['offer_id'],
            market_sku=data['sku_id'],
            flags=OfferFlags.DEPOT | OfferFlags.STORE
        )
        rows.append(offer)
    return rows


@pytest.fixture(scope="module")
def genlog_table(yt_server, genlog_rows):
    genlog_table = GenlogOffersTable(yt_server,  ypath_join(get_yt_prefix(), '0000'), genlog_rows)
    genlog_table.dump()
    return genlog_table


@pytest.yield_fixture(scope="module")
def workflow(yt_server, genlog_table, sku_sample):
    input_table_paths = [genlog_table.get_path()]
    resources = {
        'sku_sample': sku_sample,
    }

    with OffersProcessorTestEnv(
            yt_server,
            use_genlog_scheme=True,
            input_table_paths=input_table_paths,
            **resources
    ) as env:
        env.execute()
        env.verify()
        yield env


def test_sku_samples(workflow):
    for data in test_data:
        flags = OfferFlags.DEPOT | OfferFlags.STORE | OfferFlags.MODEL_COLOR_WHITE | OfferFlags.CPC
        if data['is_sample']:
            flags |= OfferFlags.IS_SAMPLE
        assert_that(
            workflow,
            HasGenlogRecordRecursive({
                'offer_id': data['offer_id'],
                'flags': flags,
            })
        )
