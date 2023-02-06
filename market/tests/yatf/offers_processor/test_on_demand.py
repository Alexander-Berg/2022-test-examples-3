# coding: utf-8

from hamcrest import assert_that
import pytest

from market.idx.pylibrary.offer_flags.flags import OfferFlags
from market.idx.offers.yatf.utils.fixtures import default_genlog
from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogOffersTable
from market.idx.offers.yatf.test_envs.offers_processor import OffersProcessorTestEnv
from market.idx.offers.yatf.matchers.offers_processor.env_matchers import HasGenlogRecord
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from yt.wrapper.ypath import ypath_join


@pytest.fixture(scope="module")
def genlog_rows():
    return [
        default_genlog(
            flags=OfferFlags.ON_DEMAND,
            url='http://ondemand.url/',
            category_id=91491,
        ),
        default_genlog(
            flags=0,
            url='http://notondemand.url/',
            category_id=91491,
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
            input_table_paths=input_table_paths
    ) as env:
        env.execute()
        env.verify()
        yield env


def test_on_demand(workflow):
    expected_flags = OfferFlags.MODEL_COLOR_WHITE
    expected_flags |= OfferFlags.CPC

    expected_flags |= OfferFlags.ON_DEMAND

    assert_that(
        workflow,
        HasGenlogRecord(
            {
                'url': 'http://ondemand.url/',
                'flags': expected_flags,
            }
        )
    )


def test_not_on_demand(workflow):
    expected_flags = OfferFlags.MODEL_COLOR_WHITE
    expected_flags |= OfferFlags.CPC

    assert_that(
        workflow,
        HasGenlogRecord(
            {
                'url': 'http://notondemand.url/',
                'flags': expected_flags,
            }
        )
    )
