#!/usr/bin/env python
# coding: utf-8

'''
Test offer markup by stop words defined for certain categories. if we have some stop_words defined for a list of
categories (node id from tova-tree.pb - hid), we check its presence in offer's title, description or sales notes.
In case if it is present and rule has same color as offer we have to reject offer.
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
from market.idx.yatf.resources.tovar_tree_pb import (
    MboCategory,
    TovarTreePbGz
)
from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogOffersTable
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from yt.wrapper.ypath import ypath_join

MARKET_COLOR = 'blue'

ROOT_HID = 1
EXPLICIT_WHITE_HID = 11
EXPLICIT_BLACK_CHILD_HID = 111
INHERITED_BLACK_HID = 1111
INHERITED_WHITE_HID = 112
UNLISTED_HID = 12
EXPLICIT_BLACK_HID = 13

OFFER_INFO = [
    {
        'id': 1,
        'title': "Чайник - Скидка",
        'hid': INHERITED_BLACK_HID,
        'to_be_rejected': False
    },
    {
        'id': 2,
        'title': "Чайник",
        'hid': INHERITED_BLACK_HID,
        'to_be_rejected': False
    },
    {
        'id': 3,
        'title': "Холодильник - Скидка",
        'hid': INHERITED_WHITE_HID,
        'to_be_rejected': False
    },
    {
        'id': 4,
        'title': "Холодильник",
        'hid': INHERITED_WHITE_HID,
        'to_be_rejected': False
    },
    {
        'id': 5,
        'title': "Пылесос - Скидка",
        'hid': EXPLICIT_WHITE_HID,
        'to_be_rejected': False
    },
    {
        'id': 6,
        'title': "Пылесос",
        'hid': EXPLICIT_WHITE_HID,
        'to_be_rejected': False
    },
    {
        'id': 7,
        'title': "Сухой корм для кошек",
        'hid': UNLISTED_HID,
        'to_be_rejected': False
    },
    {
        'id': 8,
        'title': "Восстановленный турбокомпрессор",
        'hid': EXPLICIT_BLACK_HID,
        'to_be_rejected': True
    },
    {
        'id': 9,
        'title': "Новый турбокомпрессор",
        'hid': EXPLICIT_BLACK_HID,
        'to_be_rejected': False
    },
    {
        'id': 10,
        'title': "Восстановленный поводок для собак",
        'hid': UNLISTED_HID,
        'to_be_rejected': False
    },
    {
        'id': 11,
        'title': "Электрический чайник - Скидка",
        'hid': INHERITED_BLACK_HID,
        'to_be_rejected': True
    },
    {
        'id': 12,
        'title': "Электрический чайник",
        'hid': INHERITED_BLACK_HID,
        'to_be_rejected': True
    },
    {
        'id': 13,
        'title': "Клетка для птиц - Скидка",
        'hid': UNLISTED_HID,
        'to_be_rejected': True
    }
]


def create_offer_id(color, id):
    return color + "-" + str(id)


def create_offer(
        id,
        title,
        category_id,
        color=MARKET_COLOR
):
    return default_genlog(
        offer_id=create_offer_id(color, id),
        title=title,
        category_id=category_id,
        is_blue_offer=True,
        flags=OfferFlags.IS_FULFILLMENT,
        ware_md5='09lEaAKkQll1XTaaaaaaaQ',
        market_sku=id
    )


@pytest.fixture(scope='module')
def genlog_rows():
    return [create_offer(
        id=offer['id'],
        title=offer['title'],
        category_id=offer['hid']
    ) for offer in OFFER_INFO]


@pytest.fixture(scope='module')
def tovar_tree():
    return [
        MboCategory(
            hid=ROOT_HID,
            tovar_id=0,
            unique_name="Все товары",
            name="Все товары",
            output_type=MboCategory.GURULIGHT
        ),
        MboCategory(
            hid=EXPLICIT_WHITE_HID,
            tovar_id=1,
            unique_name="Бытовая техника",
            name="Бытовая техника",
            parent_hid=ROOT_HID,
            output_type=MboCategory.GURULIGHT
        ),
        MboCategory(
            hid=EXPLICIT_BLACK_CHILD_HID,
            tovar_id=2,
            unique_name="Мелкая техника для кухни",
            name="Мелкая техника для кухни",
            parent_hid=EXPLICIT_WHITE_HID,
            output_type=MboCategory.GURULIGHT
        ),
        MboCategory(
            hid=INHERITED_BLACK_HID,
            tovar_id=3,
            unique_name="Чайники",
            name="Чайники",
            parent_hid=EXPLICIT_BLACK_CHILD_HID,
            output_type=MboCategory.GURULIGHT
        ),
        MboCategory(
            hid=INHERITED_WHITE_HID,
            tovar_id=4,
            unique_name="Крупная техника для кухни",
            name="Крупная техника для кухни",
            parent_hid=EXPLICIT_WHITE_HID,
            output_type=MboCategory.GURULIGHT
        ),
        MboCategory(
            hid=UNLISTED_HID,
            tovar_id=5,
            unique_name="Товары для животных",
            name="Товары для животных",
            parent_hid=ROOT_HID,
            output_type=MboCategory.GURULIGHT
        ),
        MboCategory(
            hid=EXPLICIT_BLACK_HID,
            tovar_id=6,
            unique_name="Автотовары",
            name="Автотовары",
            parent_hid=ROOT_HID,
            output_type=MboCategory.GURULIGHT
        )
    ]


@pytest.fixture(scope="module")
def genlog_table(yt_server, genlog_rows):
    genlog_table = GenlogOffersTable(yt_server,  ypath_join(get_yt_prefix(), '0000'), genlog_rows)
    genlog_table.dump()
    return genlog_table


@pytest.yield_fixture(scope='module')
def workflow(yt_server, genlog_table, tovar_tree):
    input_table_paths = [genlog_table.get_path()]
    stop_words_rules = StopWordHidingRules()

    # Запрещено везде кроме категории "Бытовая техника" на Беру
    stop_words_rules.add_rule(
        word="Скидка",
        tags=['title'],
        category_whitelist=[EXPLICIT_WHITE_HID],
        category_blacklist=[],
        rgb=[1]
    )

    # Запрещено только в категории "Автотовары" на Беру
    stop_words_rules.add_rule(
        word="Восстановленный",
        tags=['title'],
        category_whitelist=[ROOT_HID],
        category_blacklist=[EXPLICIT_BLACK_HID],
        rgb=[1]
    )

    # Запрещено только в категории "Мелкая техника для кухни" на Беру
    stop_words_rules.add_rule(
        word="Электрический",
        tags=['title'],
        category_whitelist=[ROOT_HID],
        category_blacklist=[EXPLICIT_BLACK_CHILD_HID],
        rgb=[1]
    )

    resources = {
        'tovar_tree_pb': TovarTreePbGz(tovar_tree),
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
    expected_offers_ids = {create_offer_id(MARKET_COLOR, offer['id']) for offer in OFFER_INFO if not offer['to_be_rejected']}
    unexpected_offers_ids = {create_offer_id(MARKET_COLOR, offer['id']) for offer in OFFER_INFO if offer['to_be_rejected']}

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
