#!/usr/bin/env python
# coding: utf-8

import pytest

from hamcrest import assert_that, is_not

from market.idx.offers.yatf.resources.offers_indexer.rules_to_hide_offers import RulesToHideOffers
from market.idx.offers.yatf.test_envs.offers_processor import OffersProcessorTestEnv
from market.idx.offers.yatf.utils.fixtures import default_genlog, default_shops_dat
from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogOffersTable
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from yt.wrapper.ypath import ypath_join

from market.idx.yatf.matchers.env_matchers import (
    ContainsYtProcessLogMessage,
    ErrorMessageHasJsonDetails
)
from market.idx.yatf.resources.shops_dat import ShopsDat

import market.proto.common.process_log_pb2 as PL


@pytest.fixture(scope="module")
def genlog_rows():
    ok_offer = default_genlog()
    ok_offer['url'] = 'www.test.ru/ok'
    ok_offer['model_id'] = 12345

    hidden_offer = default_genlog()
    hidden_offer['url'] = 'www.test.ru/hidden'
    hidden_offer['model_id'] = 92013
    hidden_offer['market_sku'] = 123456
    hidden_offer['shop_sku'] = '5e8q9ags39f44d9jc4ro'
    hidden_offer['shop_id'] = 0

    hidden_ff_offer = default_genlog()
    hidden_ff_offer['url'] = 'www.test.ru/fulfillment'
    hidden_ff_offer['feed_id'] = 1
    hidden_ff_offer['offer_id'] = 'ff_offer_id'

    rejected_offer = default_genlog()
    rejected_offer['url'] = 'www.test.ru/rejected'
    rejected_offer['rejected'] = True

    return [
        ok_offer,
        hidden_offer,
        hidden_ff_offer,
        rejected_offer,
    ]


@pytest.fixture(scope="module")
def genlog_table(yt_server, genlog_rows):
    genlog_table = GenlogOffersTable(yt_server,  ypath_join(get_yt_prefix(), '0000'), genlog_rows)
    genlog_table.dump()
    return genlog_table


@pytest.fixture(scope="module")
def custom_shops_dat():
    default_shop = default_shops_dat()

    ff_shop = default_shops_dat()
    ff_shop['name'] = 'VirtualFulfillmentShop'
    ff_shop['shop_id'] = 10
    ff_shop['datafeed_id'] = 1
    ff_shop['ff_virtual'] = True

    return ShopsDat(shops=[default_shop, ff_shop])


@pytest.yield_fixture(scope="module")
def rules_workflow(genlog_table, custom_shops_dat, yt_server):
    input_table_paths = [genlog_table.get_path()]

    rules_to_hide_offers = RulesToHideOffers()
    rules_to_hide_offers.add_model(model_id=92013)

    rules_resources = {
        'rules_to_hide_offers_json': rules_to_hide_offers,
        'shops_dat': custom_shops_dat,
    }

    with OffersProcessorTestEnv(
            yt_server,
            use_genlog_scheme=True,
            input_table_paths=input_table_paths,
            **rules_resources
    ) as idx_env:
        idx_env.execute()
        idx_env.verify()
        yield idx_env


def test_log_message_contains_hidden_offer(rules_workflow, genlog_rows):
    hidden_offer = genlog_rows[1]

    expected_code = '45Y'
    expected_feed = hidden_offer['feed_id']
    expected_id = hidden_offer['offer_id']
    expected_market_sku = hidden_offer['market_sku']
    expected_shop_sku = hidden_offer['shop_sku']

    expected = {
        'code': expected_code,
        'text': 'Offer rejected by ABO rules',
        'details': ErrorMessageHasJsonDetails({
            "reason": "{0}:{1} is rejected by shop_id=0".format(expected_feed, expected_id),
            "offerId": expected_id,
            "code": expected_code,
            "market-sku": expected_market_sku,
        }),
        'feed_id': expected_feed,
        'offer_id': expected_id,
        'offer_url': hidden_offer['url'],
        'offer_model_id': hidden_offer['model_id'],
        "offer_supplier_sku": expected_shop_sku,
        'position': None,
        'level': 3,  # Error
    }

    assert_that(
        rules_workflow,
        ContainsYtProcessLogMessage(expected),
        'Process Log message is correct'
    )


def test_log_message_contains_hidden_ff_offer(rules_workflow, genlog_rows):
    '''Тест проверяет, что в ProcessLog было добавлено сообщение об
    офере, принадлежащем вуртуальному фулфилментовскому фиду'''

    hidden_ff_offer = genlog_rows[2]

    expected_code = '45a'
    expected_feed = hidden_ff_offer['feed_id']
    expected_id = hidden_ff_offer['offer_id']

    expected = {
        'code': expected_code,
        'text': 'Offer belongs to a fulfilment virtual feed',
        'details': ErrorMessageHasJsonDetails({
            "offerId": expected_id,
            "code": expected_code
        }),
        'feed_id': expected_feed,
        'offer_id': expected_id,
        'offer_url': hidden_ff_offer['url'],
        'offer_model_id': 0,
        'position': None,
        'level': 3,  # Error
        'level_enum': PL.ERROR,
        'namespace': PL.OFFER,
        'source': 'indexer',
    }

    assert_that(
        rules_workflow,
        ContainsYtProcessLogMessage(expected),
        'Process Log message contains correct message for hidden ff offer'
    )


def test_rejected_no_abo_rules_message(rules_workflow, genlog_rows):
    ''' Тест проверяет, что для Rejected офферов не пишутся ошибки 45Y в Process Log.
        https://st.yandex-team.ru/MARKETINDEXER-9898
    '''

    rejected_offer = genlog_rows[3]

    expected = {
        'code': '45Y',
        'text': 'Offer rejected by ABO rules',
        'offer_url': rejected_offer['url'],
    }

    assert_that(
        rules_workflow,
        is_not(ContainsYtProcessLogMessage(expected)),
        "Rejected offers don't produce 45Y error"
    )
