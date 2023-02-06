#!/usr/bin/env python
# coding: utf-8
'''
Тест на проверку инфраструктурной логики разделения набора офферов по признаку схлопываемости.
В результате работы offers-processor'а должны создастся 3 набора таблиц:
Все оффера попадают в общий генлог (mi3/main/[generation_name]/genlog/[shard]).
Если у оффера проставлен флаг IS_EXPRESS, то он попадёт в таблицу (mi3/main/[generation_name]/genlog_collapse_offers/[shard])
Иначе в таблицу (mi3/main/[generation_name]/genlog_filtered_offers/[shard])

Выходные таблицы в genlog_collapse_offers потом идут на вход редьюсеру, хоторый схлопывает их по business_id, offer_id.
'''

import pytest
from google.protobuf.json_format import MessageToDict

from market.idx.pylibrary.offer_flags.flags import OfferFlags
from market.idx.yatf.resources.shops_dat import ShopsDat
from market.idx.offers.yatf.test_envs.offers_processor import OffersProcessorBusinessOfferTestEnv
from market.idx.offers.yatf.matchers.offers_processor.env_matchers import HasGenlogRecord
from market.idx.offers.yatf.utils.fixtures import default_genlog, default_shops_dat
from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogOffersTable
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from yt.wrapper.ypath import ypath_join

from hamcrest import assert_that, has_item, has_properties, is_not, has_length, all_of, equal_to


COLLAPSED_FEED_ID = 100
NO_COLLAPSED_FEED_ID = 101
DSBS_FEED_ID = 102

TEST_OFFER1 = {
    'title': 't1',
    'feed_id': COLLAPSED_FEED_ID,
    'warehouse_id': 222,
    'flags': OfferFlags.DEPOT
             | OfferFlags.STORE
             | OfferFlags.MODEL_COLOR_WHITE
             | OfferFlags.CPC
             | OfferFlags.IS_EXPRESS
}

TEST_OFFER2 = {
    'title': 't2',
    'feed_id': NO_COLLAPSED_FEED_ID,
    'warehouse_id': 0,
    'flags': OfferFlags.DEPOT
             | OfferFlags.STORE
             | OfferFlags.MODEL_COLOR_WHITE
             | OfferFlags.CPC
}

TEST_OFFER3 = {
    'title': 't1',
    'feed_id': DSBS_FEED_ID,
    'warehouse_id': 239,
    'flags': OfferFlags.DEPOT
             | OfferFlags.STORE
             | OfferFlags.MODEL_COLOR_WHITE
             | OfferFlags.CPC,
    'cpa': 4,
    'market_sku': 512201
}


TEST_SHOP1 = {
    'business_id': '1',
    'shop_id': '10',
    'datafeed_id': '100',
}

TEST_SHOP2 = {
    'business_id': '1',
    'shop_id': '10',
    'datafeed_id': '101',
}

TEST_SHOP3 = {
    'business_id': '2',
    'shop_id': '11',
    'datafeed_id': '102',
}


@pytest.fixture(scope="module")
def genlog_rows():
    offer1 = default_genlog(**TEST_OFFER1)
    offer2 = default_genlog(**TEST_OFFER2)
    offer3 = default_genlog(**TEST_OFFER3)
    return [offer1, offer2, offer3]


@pytest.fixture(scope="module")
def genlog_table(yt_server, genlog_rows):
    genlog_table = GenlogOffersTable(yt_server,  ypath_join(get_yt_prefix(), '0000'), genlog_rows)
    genlog_table.dump()
    return genlog_table


@pytest.fixture(scope="module")
def shopsdat():
    shop1 = default_shops_dat(**TEST_SHOP1)
    shop2 = default_shops_dat(**TEST_SHOP2)
    return ShopsDat(shops=[shop1, shop2])


@pytest.fixture(params=[True, False], scope="module")
def do_collapse_offers(request):
    return request.param


@pytest.yield_fixture(scope="module")
def workflow(yt_server, genlog_table, shopsdat, do_collapse_offers):
    input_table_paths = [genlog_table.get_path()]
    resources = {'shops_dat': shopsdat}

    with OffersProcessorBusinessOfferTestEnv(
        yt_server,
        do_collapse_offers=do_collapse_offers,
        collapse_use_express=True,
        collapse_use_dsbs=True,
        use_genlog_scheme=True,
        input_table_paths=input_table_paths,
        **resources
    ) as env:
        env.execute()
        env.verify()
        yield env


@pytest.mark.skip(reason='TODO: enable when tables are back to OP, MARKETINDEXER-39724')
def test_collapse_offers_enable(workflow, do_collapse_offers):
    '''
    Проверка флага collapse_offers.
    Если он True, то на выходе мы должны увидеть в genlog_collapse_offers только один оффер.
    Тот у которого есть флаг IS_EXPRESS.

    А в genlog_filtered_offers один оффер без флага IS_EXPRESS
    '''
    if not do_collapse_offers:
        pytest.skip('Skip test do_collapse_offers=False')

    assert_that(
        workflow.collapse_offers,
        all_of(
            has_item(has_properties({'feed_id': COLLAPSED_FEED_ID})),
            has_item(has_properties({'feed_id': DSBS_FEED_ID})),
            is_not(has_item(has_properties({'feed_id': NO_COLLAPSED_FEED_ID})))
        )
    )
    assert_that(
        workflow.filtered_offers,
        all_of(
            is_not(has_item(has_properties({'feed_id': COLLAPSED_FEED_ID}))),
            is_not(has_item(has_properties({'feed_id': DSBS_FEED_ID}))),
            has_item(has_properties({'feed_id': NO_COLLAPSED_FEED_ID}))
        )
    )


def test_genlog_collapse_offers_disable(workflow, do_collapse_offers):
    '''
    Если do_collapse_offers=False, то в genlog_collapse_offers и genlog_filtered_offers не должно быть офферов
    '''
    if do_collapse_offers:
        pytest.skip('Skip test do_collapse_offers=True')

    assert_that(workflow.collapse_offers, has_length(0))
    assert_that(workflow.filtered_offers, has_length(0))


def test_genlog_contains_all(genlog_rows, workflow):
    '''
    В genlog всегда есть все оффера.
    '''
    assert_that(workflow, all_of(
        HasGenlogRecord({'feed_id': COLLAPSED_FEED_ID}),
        HasGenlogRecord({'feed_id': NO_COLLAPSED_FEED_ID}),
        HasGenlogRecord({'feed_id': DSBS_FEED_ID}),
    ))

    assert_that(workflow.genlog, has_length(len(genlog_rows)))


def test_remap(workflow, genlog_rows):
    assert_that(workflow.genlog, has_length(len(genlog_rows)))

    for offer in workflow.genlog_with_row_index:
        offer_dict = MessageToDict(offer[1], preserving_proto_field_name=True)
        assert_that(offer[0], equal_to(offer_dict['sequence_number']))
