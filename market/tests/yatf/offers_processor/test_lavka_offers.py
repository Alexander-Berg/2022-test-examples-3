#!/usr/bin/env python
# coding: utf-8

import pytest

from market.idx.offers.yatf.utils.fixtures import create_random_ware_md5, generate_binary_price_dict
from market.idx.offers.yatf.test_envs.offers_processor import OffersProcessorTestEnv
from market.idx.offers.yatf.matchers.offers_processor.env_matchers import HasGenlogRecord, HasGenlogRecordRecursive
from market.idx.pylibrary.offer_flags.flags import OfferFlags
from market.idx.yatf.resources.stop_word_hiding_rules import StopWordHidingRules
from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogOffersTable
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from yt.wrapper.ypath import ypath_join
from hamcrest import all_of, assert_that, has_item, has_key, has_properties, is_not
import yt.wrapper as yt


ROOT_CATEGORY = 90401
LAVKA_FEED_ID = 1


@pytest.fixture(scope="module")
def genlog_rows():
    return [{
        'feed_id': LAVKA_FEED_ID,
        'offer_id': 'lavka_offer_1',
        'flags': OfferFlags.CPA | OfferFlags.IS_LAVKA | OfferFlags.DELIVERY,
        'shop_category_id': str(ROOT_CATEGORY),
        'ware_md5': create_random_ware_md5(),
        'binary_price': generate_binary_price_dict(10000),
        'has_delivery': True,
        'delivery_flag': True,
        'title': 'crab chowder',
        'description': 'yummy sweet crab chowder',
        'language_tag': 'en',
        'ingredients': ['crab meat', 'palm oil', 'sugar'],
        'vat': 7,
        'cpa': 4,
        'offer_delivery_options': [{'Cost': 100.0, 'DaysMin': yt.yson.YsonUint64(2), 'DaysMax': yt.yson.YsonUint64(3)}],
    },
    {
        'feed_id': LAVKA_FEED_ID,
        'offer_id': 'lavka_offer_with_stop_word',
        'flags':  OfferFlags.IS_LAVKA | OfferFlags.DELIVERY | OfferFlags.CPA,
        'shop_category_id': str(ROOT_CATEGORY),
        'ware_md5': create_random_ware_md5(),
        'binary_price': generate_binary_price_dict(10000),
        'has_delivery': True,
        'delivery_flag': True,
        'title': 'awesome crab cake',
        'language_tag': 'en',
    }]


@pytest.fixture(scope="module")
def stop_word_list():
    stop_words_rules = StopWordHidingRules()
    stop_words_rules.add_rule(
        word='awesome',
        tags=['title'],
        forbidden=True,
    )
    return stop_words_rules


@pytest.fixture(scope="module")
def genlog_table(yt_server, genlog_rows):
    genlog_table = GenlogOffersTable(yt_server,  ypath_join(get_yt_prefix(), '0000'), genlog_rows)
    genlog_table.dump()
    return genlog_table


@pytest.yield_fixture(scope="module")
def workflow(yt_server, genlog_table, stop_word_list):
    input_table_paths = [genlog_table.get_path()]
    resources = {
        'stop_word_hiding_rules_json': stop_word_list,
    }

    with OffersProcessorTestEnv(
            yt_server,
            use_genlog_scheme=True,
            input_table_paths=input_table_paths,
            **resources
    ) as env:
        env.execute()
        env.verify()
        yield env


def test_lavka_offers(workflow):
    """ Офферы Лавки попадают в генлог с флагом доставки и пустыми бакетами """
    assert_that(
        workflow,
        HasGenlogRecordRecursive({
            'offer_id': 'lavka_offer_1',
            'flags': OfferFlags.IS_LAVKA | OfferFlags.DELIVERY | OfferFlags.CPA,
            'delivery_flag': True,
            'title': 'crab chowder',
            'description': 'yummy sweet crab chowder',
            'language_tag': 'en',
            'ingredients': ['crab meat', 'palm oil', 'sugar'],
            'vat': 7,
            'cpa': 4,
            'offer_delivery_options': [{'Cost': 100.0, 'DaysMin': 2, 'DaysMax': 3}],
        })
    )

    assert_that(
        workflow.genlog,
        has_item(
            all_of(
                has_properties({'offer_id': 'lavka_offer_1'}),
                is_not(has_key('delivery_bucket_ids')),
                is_not(has_key('pickup_bucket_ids')),
                is_not(has_key('post_bucket_ids')),
            )
        )
    )


def test_lavka_offers_ignore_stop_words(workflow):
    """ Офферы Лавки должны просачиваться сквозь фильтр стоп-слов """
    assert_that(
        workflow,
        HasGenlogRecord({
            'offer_id': 'lavka_offer_with_stop_word',
            'forbidden_market_mask': 0
        })
    )
