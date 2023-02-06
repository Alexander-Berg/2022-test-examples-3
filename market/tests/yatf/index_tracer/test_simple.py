#!/usr/bin/env python
# coding: utf-8
import pytest
from hamcrest import assert_that, is_not, all_of

from market.idx.generation.yatf.test_envs.index_tracer import IndexTracerTestEnv
from market.idx.offers.yatf.resources.offers_indexer.rules_to_hide_offers import RulesToHideOffers
from market.idx.offers.yatf.test_envs.offers_processor import OffersProcessorTestEnv
from market.idx.offers.yatf.matchers.offers_indexer.env_matchers import HasTracelogRecord
from market.idx.offers.yatf.utils.fixtures import default_blue_genlog
from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogOffersTable

from market.idx.yatf.matchers.env_matchers import ContainsYtProcessLogMessage
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from yt.wrapper.ypath import ypath_join


GENERATION_NAME = '20210830_1527'


@pytest.fixture(scope="module")
def genlog_rows():
    blue_1p_offer = default_blue_genlog()
    blue_1p_offer['offer_id'] = 'blue-1'
    blue_1p_offer['rejected'] = False
    blue_1p_offer['feed_id'] = 1337

    # useless stuff for coverage
    blue_1p_offer['weight'] = 1001.0
    blue_1p_offer['length'] = 1002.0
    blue_1p_offer['width'] = 1003.0
    blue_1p_offer['height'] = 1004.0
    blue_1p_offer['has_delivery'] = True
    blue_1p_offer['delivery_flag'] = True
    blue_1p_offer['is_blue_offer'] = True
    blue_1p_offer['is_fake_msku_offer'] = True
    blue_1p_offer['is_buyboxes'] = True
    blue_1p_offer['market_sku'] = 1005
    blue_1p_offer['supplier_id'] = 0
    blue_1p_offer['warehouse_id'] = 1006
    blue_1p_offer['classifier_magic_id'] = '1007'
    blue_1p_offer['fulfillment_shop_id'] = 1008
    blue_1p_offer['shop_sku'] = '1009'

    blue_3p_offer = default_blue_genlog()
    blue_3p_offer['offer_id'] = 'blue-3'
    blue_3p_offer['rejected'] = False
    blue_3p_offer['feed_id'] = 1337

    blue_rejected_offer = default_blue_genlog()
    blue_rejected_offer['offer_id'] = 'blue-rejected'
    blue_rejected_offer['rejected'] = True

    blue_hidden_offer = default_blue_genlog()
    blue_hidden_offer['offer_id'] = 'blue-hidden'
    blue_hidden_offer['rejected'] = False
    blue_hidden_offer['model_id'] = 92013
    blue_hidden_offer['feed_id'] = 666

    return [blue_1p_offer, blue_3p_offer, blue_rejected_offer, blue_hidden_offer]


@pytest.fixture(scope="module")
def genlog_table(yt_server, genlog_rows):
    genlog_table = GenlogOffersTable(yt_server,  ypath_join(get_yt_prefix(), '0000'), genlog_rows)
    genlog_table.dump()
    return genlog_table


@pytest.yield_fixture(scope='module')
def offers_processor_workflow(yt_server, genlog_table):
    input_table_paths = [genlog_table.get_path()]

    rules_to_hide_offers = RulesToHideOffers()
    rules_to_hide_offers.add_model(model_id=92013)

    resources = {
        'rules_to_hide_offers_json': rules_to_hide_offers,
    }

    with OffersProcessorTestEnv(
            yt_server,
            generation=GENERATION_NAME,
            enable_completed_offers_tracing=True,
            use_genlog_scheme=True,
            input_table_paths=input_table_paths,
            **resources
    ) as env:
        env.execute()
        env.verify()
        yield env


@pytest.yield_fixture(scope='module')
def workflow(yt_server, offers_processor_workflow):
    with IndexTracerTestEnv() as env:
        env.execute(yt_server, offers_processor_workflow.yt_table_process_log, offers_processor_workflow.generation, offers_processor_workflow.yt_table_completed_offers_for_tracer)
        env.verify()
        yield env


def test_process_log(offers_processor_workflow):
    expected_hidden = {
        'code': '45Y',
        'text': 'Offer rejected by ABO rules',
        'offer_id': 'blue-hidden'
    }
    expected_rejected = {
        'offer_id': 'blue-rejected'
    }

    # rejected оффер не появился в process log, значит не увидим и в trace log

    assert_that(offers_processor_workflow, all_of(
        ContainsYtProcessLogMessage(expected_hidden),
        is_not(ContainsYtProcessLogMessage(expected_rejected))
    ))


def test_completed_tracer(workflow):
    assert_that(workflow, all_of(
        HasTracelogRecord({
            'target_module': 'Success',
            'request_method': 'Offer processing finished',
            'kv.offer_id': 'blue-1',
            'kv.feed_id': '1337',
        }),
        HasTracelogRecord({
            'target_module': 'Success',
            'request_method': 'Offer processing finished',
            'kv.offer_id': 'blue-3',
            'kv.feed_id': '1337',
        }),
    ))


def test_not_completed_offers_tracer(workflow):
    assert_that(workflow, all_of(
        HasTracelogRecord({
            'http_code': '400',
            'request_method': 'Offer was hidden or rejected',
            'kv.offer_id': 'blue-hidden',
            'kv.generation_name': GENERATION_NAME,
            'kv.feed_id': '666',
        })
    ))
