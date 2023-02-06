#!/usr/bin/env python
# coding: utf-8
import pytest

from market.idx.pylibrary.offer_flags.flags import OfferFlags
from market.idx.offers.yatf.utils.fixtures import default_genlog
from market.idx.offers.yatf.test_envs.offers_processor import OffersProcessorTestEnv
from market.idx.offers.yatf.matchers.offers_processor.env_matchers import HasGenlogRecord
from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogOffersTable
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from yt.wrapper.ypath import ypath_join

from hamcrest import assert_that


@pytest.fixture(scope="module")
def genlog_rows():
    return [
        default_genlog(  # Cut price offer
            flags=OfferFlags.IS_CUTPRICE | OfferFlags.LIKE_NEW,
            url='http://cutprice.url/',
            category_id=91491,
        ),
        default_genlog(  # Cut price & previously used offer
            flags=OfferFlags.IS_CUTPRICE | OfferFlags.PREVIOUSLY_USED,
            url='http://cutpriceused.url/',
            category_id=91491,
        ),
        default_genlog(  # New offer
            flags=0,
            url='http://new.url/',
            category_id=91491,
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


def test_cutprice_flag(workflow):
    """
    во флагах уцененного не БУ офера появился фдаг IS_CUTPRICE без PREVIOUSLY_USED
    """
    expected_flags = OfferFlags.MODEL_COLOR_WHITE
    expected_flags |= OfferFlags.CPC
    expected_flags |= OfferFlags.IS_CUTPRICE
    expected_flags |= OfferFlags.LIKE_NEW

    assert_that(
        workflow,
        HasGenlogRecord(
            {
                'url': 'http://cutprice.url/',
                'flags': expected_flags,  # во флагах офера появились IS_CUTPRICE & LIKE_NEW
            }
        )
    )


def test_cutprice_prev_used_flag(workflow):
    """
    во флагах уцененного БУ офера появились флаги IS_CUTPRICE & PREVIOUSLY_USED
    """
    expected_flags = OfferFlags.MODEL_COLOR_WHITE
    expected_flags |= OfferFlags.CPC
    expected_flags |= OfferFlags.IS_CUTPRICE
    expected_flags |= OfferFlags.PREVIOUSLY_USED

    assert_that(
        workflow,
        HasGenlogRecord(
            {
                'url': 'http://cutpriceused.url/',
                'flags': expected_flags,  # во флагах офера появился IS_CUTPRICE & PREVIOUSLY_USED
            }
        )
    )


def test_no_cutprice_flag(workflow):
    """
    во флагах неуцененного офера не появился IS_CUTPRICE & LIKE_NEW_FLAG & PREVIOUSLY_USED
    """
    # MODEL_COLOR_WHITE & CPC
    expected_flags = OfferFlags.MODEL_COLOR_WHITE
    expected_flags |= OfferFlags.CPC

    assert_that(
        workflow,
        HasGenlogRecord(
            {
                'url': 'http://new.url/',
                'flags': expected_flags,  # во флагах офера не появился IS_CUTPRICE & PREVIOUSLY_USED
            }
        )
    )
