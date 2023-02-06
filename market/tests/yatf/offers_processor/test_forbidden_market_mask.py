#!/usr/bin/env python
# coding: utf-8

'''
Test offer stop words checker i.e. if we have some stop_words in
offer's title, description or sales notes we have to reject offer if
rule and offer have same color.
Begining of story (MARKETOUT-23781)
Ending of story https://nda.ya.ru/t/okjlyN-444vSMe
In description we check only first 250 symbols (MARKETINDEXER-12049).
Code of checker: market/idx/offers/lib/checkers/offer_checker.cpp
'''

import pytest
from hamcrest import assert_that, not_

from market.idx.offers.yatf.matchers.offers_processor.env_matchers import HasGenlogRecord
from market.idx.offers.yatf.test_envs.offers_processor import OffersProcessorTestEnv
from market.idx.offers.yatf.utils.fixtures import default_genlog

from market.idx.pylibrary.offer_flags.flags import OfferFlags
from market.idx.yatf.resources.stop_word_hiding_rules import StopWordHidingRules
from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogOffersTable
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from yt.wrapper.ypath import ypath_join


class MarketColor(object):
    MC_WHITE = 0
    MC_BLUE = 1


class OfferColor(object):
    OC_WHITE = 'white'
    OC_BLUE = 'blue'


STOP_WORD_MAP = {
    MarketColor.MC_WHITE: 'restored',
    MarketColor.MC_BLUE: 'broken',
}

OFFER_TITLE_PREFIXES = [
    "Completely normal",  # 0
    "Restored",  # 1
    "Broken",  # 2
    "Broken and restored",  # 3
    "Fractured",  # 4
    "Fractured and restored",  # 5
    "Fractured and broken",  # 6
    "Fractured, broken and restored"  # 7
]


def create_offer_id(offer_color, id):
    return offer_color + '-' + str(id)


def create_offer(
        id,
        title_prefix,
        color=OfferColor.OC_WHITE
):
    title = title_prefix + " " + color + " offer"
    offer_id = create_offer_id(color, id)
    offer = default_genlog(
        offer_id=offer_id,
        title=title,
    )
    if color == OfferColor.OC_BLUE:
        offer['is_blue_offer'] = True
        offer['flags'] = OfferFlags.IS_FULFILLMENT
        offer['ware_md5'] = '09lEaAKkQll1XTaaaaaaaQ'
        offer['market_sku'] = id
    return offer


@pytest.fixture(scope='module')
def genlog_rows():
    offers = []
    for offer_color in [OfferColor.OC_WHITE, OfferColor.OC_BLUE]:
        for i in range(0, len(OFFER_TITLE_PREFIXES)):
            offers += [create_offer(
                id=i,
                title_prefix=OFFER_TITLE_PREFIXES[i],
                color=offer_color
            )]
    return offers


@pytest.fixture(scope="module")
def genlog_table(yt_server, genlog_rows):
    genlog_table = GenlogOffersTable(yt_server,  ypath_join(get_yt_prefix(), '0000'), genlog_rows)
    genlog_table.dump()
    return genlog_table


@pytest.yield_fixture(scope="module")
def workflow(yt_server, genlog_table):
    input_table_paths = [genlog_table.get_path()]

    stop_words_rules = StopWordHidingRules()
    for market_color in [MarketColor.MC_WHITE, MarketColor.MC_BLUE]:
        stop_words_rules.add_rule(
            word=STOP_WORD_MAP[market_color],
            tags=['title'],
            rgb=[market_color]
        )

    resources = {
        'stop_word_hiding_rules_json': stop_words_rules,
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


def test_rejected_by_colored_rule(workflow):
    """
    Проверяем, что отклонились офферы по соответствующему их цвету/программе правилу. И не отклонились по соседнему.
    """
    expected_offers_ids = {
        'blue-0',
        'blue-1',
        'blue-4',
        'blue-5',
        'white-0',
        'white-2',
        'white-4',
        'white-6',
    }
    unexpected_offers_ids = {
        'blue-2',
        'blue-3',
        'blue-6',
        'blue-7',
        'white-1',
        'white-3',
        'white-5',
        'white-7',
    }

    for x in expected_offers_ids:
        assert_that(
            workflow,
            HasGenlogRecord({'offer_id': x})
        )

    for x in unexpected_offers_ids:
        assert_that(
            workflow,
            not_(HasGenlogRecord({'offer_id': x}))
        )
