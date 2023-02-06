#!/usr/bin/env python
# coding: utf-8

"""
manufacturer_warranty - гарантия производителя.
значения: 1/0/может отсутствовать

https://yandex.ru/support/partnermarket/offers.html
"""

from market.idx.pylibrary.offer_flags.flags import OfferFlags
from market.idx.offers.yatf.utils.fixtures import default_genlog
from market.idx.offers.yatf.test_envs.offers_processor import OffersProcessorTestEnv
from market.idx.offers.yatf.matchers.offers_processor.env_matchers import HasGenlogRecord
from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogOffersTable
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from yt.wrapper.ypath import ypath_join

from hamcrest import assert_that
import pytest


test_data = [
    {
        'offer_id': '1',
        'has_manufacturer_warranty': True
    },
    {
        'offer_id': '2',
        'has_manufacturer_warranty': False
    }
]


@pytest.fixture(scope="module")
def genlog_rows():
    offers = []
    manufacture_flags = OfferFlags.DEPOT
    manufacture_flags |= OfferFlags.STORE
    manufacture_flags |= OfferFlags.MANUFACTURER_WARRANTY
    for data in test_data:
        if data['has_manufacturer_warranty']:
            offer = default_genlog(
                offer_id=data['offer_id'],
                flags=manufacture_flags
            )
        else:
            offer = default_genlog(
                offer_id=data['offer_id'],
                flags=OfferFlags.DEPOT | OfferFlags.STORE
            )
        offers.append(offer)

    return offers


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
        yield env


def test_manufacturer_warranty(workflow):
    for offer in test_data:
        expected_flags = OfferFlags.DEPOT
        expected_flags |= OfferFlags.STORE
        expected_flags |= OfferFlags.MODEL_COLOR_WHITE
        expected_flags |= OfferFlags.CPC

        if offer['has_manufacturer_warranty']:
            expected_flags |= OfferFlags.MANUFACTURER_WARRANTY

        assert_that(
            workflow,
            HasGenlogRecord(
                {
                'offer_id': offer['offer_id'],
                'flags': expected_flags,
                }
            )
        )
