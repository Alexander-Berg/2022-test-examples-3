#!/usr/bin/env python
# coding: utf-8

from market.idx.pylibrary.offer_flags.flags import OfferFlags
from market.idx.offers.yatf.test_envs.offers_processor import OffersProcessorTestEnv
from market.idx.offers.yatf.matchers.offers_processor.env_matchers import HasGenlogRecord
from market.idx.offers.yatf.utils.fixtures import default_credit_template_dict, default_genlog
from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogOffersTable
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from yt.wrapper.ypath import ypath_join

from hamcrest import assert_that
import pytest


@pytest.fixture(scope='module')
def genlog_rows():
    return [
        default_genlog(
            offer_id='no_credit',
            ware_md5='Njk5ZDA1MmNlYzhjNDI5YQ',
            credit_templates=[
            ],
            flags=OfferFlags.DEPOT | OfferFlags.STORE
        ),
        default_genlog(
            offer_id='has_credit',
            ware_md5='ZWQwZDFkMzNjMjc1NGMwOQ',
            credit_templates=[
                default_credit_template_dict(),
            ],
            flags=OfferFlags.DEPOT | OfferFlags.STORE
        ),
        default_genlog(
            offer_id='has_installment',
            ware_md5='ODY0Y2VkMWI3MzAzNDJjZQ',
            credit_templates=[
                default_credit_template_dict(is_installment=True),
            ],
            flags=OfferFlags.DEPOT | OfferFlags.STORE
        ),
        default_genlog(
            offer_id='whaat',
            ware_md5='Njk5ZDA1MmNlYzhjNDI5YZ',
            credit_templates=[
            ],
            flags=OfferFlags.DEPOT | OfferFlags.STORE
        ),
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
        yield env


def test_installment_flag(workflow, genlog_rows):
    for offer in genlog_rows:
        expected_flags = OfferFlags.DEPOT
        expected_flags |= OfferFlags.STORE
        expected_flags |= OfferFlags.MODEL_COLOR_WHITE
        expected_flags |= OfferFlags.CPC

        if offer['ware_md5'] == 'ODY0Y2VkMWI3MzAzNDJjZQ':
            expected_flags |= OfferFlags.HAS_INSTALLMENT

        assert_that(
            workflow,
            HasGenlogRecord(
                {
                'ware_md5': offer['ware_md5'],
                'flags': expected_flags,
                }
            )
        )
