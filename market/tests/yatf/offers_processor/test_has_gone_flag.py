# coding: utf-8

from hamcrest import assert_that
import pytest

from market.idx.pylibrary.offer_flags.flags import DisabledFlags, OfferFlags
from market.idx.offers.yatf.utils.fixtures import default_genlog
from market.idx.offers.yatf.test_envs.offers_processor import OffersProcessorTestEnv
from market.idx.offers.yatf.matchers.offers_processor.env_matchers import HasGenlogRecord
from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogOffersTable
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from yt.wrapper.ypath import ypath_join


@pytest.fixture(scope="module")
def genlog_rows():
    offer_without_has_gone = default_genlog()
    offer_without_has_gone['offer_id'] = '1'
    offer_without_has_gone['ware_md5'] = 'nomkVuHL2Q0wYGPdnvvfKg'
    offer_without_has_gone['flags'] = OfferFlags.DEPOT | OfferFlags.STORE

    offer_with_has_gone_false = default_genlog()
    offer_with_has_gone_false['offer_id'] = '2'
    offer_with_has_gone_false['ware_md5'] = 'AomkVuHL2Q0wYGPdnvvfKg'
    offer_with_has_gone_false['flags'] = OfferFlags.DEPOT | OfferFlags.STORE

    offer_with_has_gone_true = default_genlog()
    offer_with_has_gone_true['offer_id'] = '3'
    offer_with_has_gone_true['ware_md5'] = 'BomkVuHL2Q0wYGPdnvvfKg'
    offer_with_has_gone_true['flags'] = OfferFlags.DEPOT | OfferFlags.STORE | OfferFlags.OFFER_HAS_GONE

    offer_with_has_gone_false_and_disabled_flags = default_genlog()
    offer_with_has_gone_false_and_disabled_flags['offer_id'] = '4'
    offer_with_has_gone_false_and_disabled_flags['disabled_flags'] = DisabledFlags.MARKET_STOCK
    offer_with_has_gone_false_and_disabled_flags['ware_md5'] = 'ComkVuHL2Q0wYGPdnvvfKg'
    offer_with_has_gone_false_and_disabled_flags['flags'] = OfferFlags.DEPOT | OfferFlags.STORE

    return [
        offer_without_has_gone,
        offer_with_has_gone_false,
        offer_with_has_gone_true,
        offer_with_has_gone_false_and_disabled_flags
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


def test_documents_in_index(workflow, genlog_rows):
    """ Проверяем что у первых двух офферов в индексе нет флага gone, у третьего есть
    """
    for offer in genlog_rows:
        expected_flags = OfferFlags.DEPOT
        expected_flags |= OfferFlags.STORE
        expected_flags |= OfferFlags.MODEL_COLOR_WHITE
        expected_flags |= OfferFlags.CPC

        if offer.get('has_gone') is True:
            expected_flags |= OfferFlags.OFFER_HAS_GONE

        assert_that(
            workflow,
            HasGenlogRecord(
                {
                'ware_md5': offer['ware_md5'],
                'flags': expected_flags,
                }
            )
        )
