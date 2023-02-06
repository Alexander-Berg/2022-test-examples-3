#!/usr/bin/env python
# coding: utf-8

import pytest

from hamcrest import (
    assert_that,
    is_not,
    has_length,
)

from market.idx.offers.yatf.resources.offers_indexer.rules_to_hide_offers import RulesToHideOffers
from market.idx.yatf.resources.shops_dat import ShopsDat

from market.idx.offers.yatf.utils.fixtures import default_genlog, default_shops_dat
from market.idx.offers.yatf.test_envs.offers_processor import OffersProcessorTestEnv
from market.idx.offers.yatf.matchers.offers_processor.env_matchers import HasGenlogRecord
from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogOffersTable
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from yt.wrapper.ypath import ypath_join


@pytest.fixture(scope="module")
def genlog_rows():
    ok_offer = default_genlog()
    ok_offer['url'] = 'www.test.ru/ok'
    ok_offer['model_id'] = 12345
    ok_offer['offer_id'] = 'ok_offer'

    hidden_offer = default_genlog()
    hidden_offer['url'] = 'www.test.ru/hidden'
    hidden_offer['model_id'] = 92013
    hidden_offer['market_sku'] = 123456
    hidden_offer['shop_sku'] = '5e8q9ags39f44d9jc4ro'
    hidden_offer['offer_id'] = 'hidden_offer'

    hidden_ff_offer = default_genlog()
    hidden_ff_offer['url'] = 'www.test.ru/fulfillment'
    hidden_ff_offer['feed_id'] = 1
    hidden_ff_offer['offer_id'] = 'ff_offer_id'

    rejected_offer = default_genlog()
    rejected_offer['url'] = 'www.test.ru/rejected'
    rejected_offer['rejected'] = True
    rejected_offer['offer_id'] = 'rejected_offer'

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
        'stderr_log_yt_tabe_path': 'err_logs',
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


def test_ok(rules_workflow):
    assert_that(
        rules_workflow,
        HasGenlogRecord(
            {
                'url': 'www.test.ru/ok'
            }
        ),
        u'Оффер, не попадающий под фильтрацию, остается'
    )


def test_hidden(rules_workflow):
    assert_that(
        rules_workflow,
        is_not(HasGenlogRecord(
            {
                'url': 'www.test.ru/hidden'
            }
        )),
        u'Оффер, попадающий под фильтрацию, скрывается'
    )


def test_ff_hidden(rules_workflow):
    '''Тест проверяет, что офер вирутального фулфилментовского фида отфильтровывается'''

    assert_that(
        rules_workflow,
        is_not(HasGenlogRecord(
            {
                'url': 'www.test.ru/fulfillment'
            }
        )),
        u'Оффер, вирутального магазина, скрывается'
    )


def test_std_err_table(rules_workflow):
    '''Проверяем что в логе ошибок - одна запись под выкинутый офер'''

    assert_that(
        rules_workflow.std_err_logs,
        has_length(1)
    )
