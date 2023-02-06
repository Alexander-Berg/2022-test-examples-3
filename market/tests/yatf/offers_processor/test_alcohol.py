# coding: utf-8

from market.idx.pylibrary.offer_flags.flags import OfferFlags
from market.idx.offers.yatf.utils.fixtures import default_genlog
from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogOffersTable
from market.idx.offers.yatf.test_envs.offers_processor import OffersProcessorTestEnv
from market.idx.offers.yatf.matchers.offers_processor.env_matchers import HasGenlogRecord
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from yt.wrapper.ypath import ypath_join

from hamcrest import assert_that
import pytest


@pytest.fixture(scope="module")
def genlog_rows():
    return [
        default_genlog(  # alcohol
            flags=OfferFlags.ALCOHOL,
            url='http://alcohol.url/',
            category_id=91491,
        ),
        default_genlog(  # no alcohol
            flags=0,
            url='http://noalco.url/',
            category_id=91491,
        ),
    ]


@pytest.fixture(scope="module")
def genlog_table(yt_server, genlog_rows):
    input_table_path = ypath_join(get_yt_prefix(), '0000')

    genlog_table = GenlogOffersTable(yt_server, input_table_path, genlog_rows)
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
        yield env


def test_alcohol_flag(workflow):
    """
    во флагах алко офера появился фдаг ALCOHOL
    """
    expected_flags = OfferFlags.MODEL_COLOR_WHITE
    expected_flags |= OfferFlags.CPC
    expected_flags |= OfferFlags.ALCOHOL

    assert_that(
        workflow,
        HasGenlogRecord(
            {
                'url': 'http://alcohol.url/',
                'flags': expected_flags,  # во флагах офера появился ALCOHOL
            }
        )
    )


def test_no_alcohol_flag(workflow):
    """
    во флагах не-алко офера не появился ALCOHOL
    """
    expected_flags = OfferFlags.MODEL_COLOR_WHITE
    expected_flags |= OfferFlags.CPC

    assert_that(
        workflow,
        HasGenlogRecord(
            {
                'url': 'http://noalco.url/',
                'flags': expected_flags,  # во флагах офера не появился ALCOHOL
            }
        )
    )
