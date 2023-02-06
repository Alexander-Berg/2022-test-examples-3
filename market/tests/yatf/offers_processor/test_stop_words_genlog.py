#!/usr/bin/env python
# coding: utf-8

'''
Test offer stop words checker i.e. if we have some stop_words in
offer's title, description or sales notes we have to hide it.
In description we check only first 250 symbols (MARKETINDEXER-12049).
Code of checker: market/idx/offers/lib/checkers/offer_checker.cpp
Also test color differential stop-wordsi checker i.e words only for red market and so on
White-cpc offers will be rejected by white rules, all cpa-offers (blue & dsbs) - by blue.
'''

import pytest

from hamcrest import assert_that, is_not

from market.idx.pylibrary.offer_flags.flags import OfferFlags
from market.idx.offers.yatf.utils.fixtures import (
    default_genlog,
)
from market.idx.offers.yatf.test_envs.offers_processor import OffersProcessorTestEnv
from market.idx.offers.yatf.matchers.offers_processor.env_matchers import HasGenlogRecord
from market.idx.yatf.resources.stop_word_hiding_rules import StopWordHidingRules
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from yt.wrapper.ypath import ypath_join
from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogOffersTable

from collections import namedtuple


FeedParams = namedtuple(
    'FeedParams',
    [
        'offer_id',
        'offer_url',
        'category_id',
        'tag_name',
        'tag_value',
        'offer_color',
        'flags',
        'feed_group_id_hash',
        'is_dsbs',
        'model_id',
    ]
)

FEED = [
    FeedParams(
        offer_id='1',
        offer_url='ok',
        category_id=12345,
        tag_name=None,
        tag_value=None,
        offer_color='white',
        flags=0,
        feed_group_id_hash='',
        is_dsbs=False,
        model_id=100,
    ),
    # offer category is in white list
    FeedParams(
        offer_id='2',
        offer_url='ok_offer_whitelist',
        category_id=56789,
        tag_name='title',
        tag_value='Super discount',
        offer_color='blue',
        flags=0,
        feed_group_id_hash='',
        is_dsbs=False,
        model_id=100,
    ),
    FeedParams(
        offer_id='3',
        offer_url='hidden',
        category_id=92013,
        tag_name='description',
        tag_value='Mega action',
        offer_color='white',
        flags=0,
        feed_group_id_hash='',
        is_dsbs=False,
        model_id=100,
    ),
    FeedParams(
        offer_id='4',
        offer_url='forbidden_offer',
        category_id=92013,
        tag_name='sales_notes',
        tag_value='Cool bargain',
        offer_color='blue',
        flags=0,
        feed_group_id_hash='',
        is_dsbs=False,
        model_id=100,
    ),
    # stop word after 250th symbol
    FeedParams(
        offer_id='5',
        offer_url='ok_long_description',
        category_id=12345,
        tag_name='description',
        tag_value='word ' * 50 + 'action word',
        offer_color='white',
        flags=0,
        feed_group_id_hash='',
        is_dsbs=False,
        model_id=100,
    ),
    # stop word right before 250th symbol
    FeedParams(
        offer_id='6',
        offer_url='stop_word_long_description',
        category_id=12345,
        tag_name='description',
        tag_value='слово ' * 40 + 'action слово',
        offer_color='white',
        flags=0,
        feed_group_id_hash='',
        is_dsbs=False,
        model_id=100,
    ),
    FeedParams(
        offer_id='7',
        offer_url='dsbs_hidden_by_blue_rule',
        category_id=92013,
        tag_name='sales_notes',
        tag_value='Cool badbadcpa',
        offer_color='white',
        flags=0,
        feed_group_id_hash='',
        is_dsbs=True,
        model_id=100,
    ),
    FeedParams(
        offer_id='71',
        offer_url='blue_hidden_by_blue_rule',
        category_id=92013,
        tag_name='sales_notes',
        tag_value='Cool badbadcpa',
        offer_color='blue',
        flags=0,
        feed_group_id_hash='',
        is_dsbs=False,
        model_id=100,
    ),
    # "free" is banned on the white market: white-cpc will be rejected
    # "free" is banned on the white market, blue offers & white-cpa - wont be rejected
    FeedParams(
        offer_id='9',
        offer_url='blue_offer_not_hidden_by_white_rule',
        category_id=12345,
        tag_name='title',
        tag_value='free product',
        offer_color='blue',
        flags=0,
        feed_group_id_hash='',
        is_dsbs=False,
        model_id=100,
    ),
    FeedParams(
        offer_id='91',
        offer_url='dsbs_not_hidden_by_white_rule',
        category_id=12345,
        tag_name='title',
        tag_value='free product',
        offer_color='white',
        flags=0,
        feed_group_id_hash='',
        is_dsbs=True,
        model_id=100,
    ),
    # allowed stop word in cutprice offer
    FeedParams(
        offer_id='10',
        offer_url='allowed_stop_word_cutprice',
        category_id=12345,
        tag_name='description',
        tag_value='слово ' * 10 + 'action слово',
        offer_color='white',
        flags=OfferFlags.IS_CUTPRICE,
        feed_group_id_hash='',
        is_dsbs=False,
        model_id=100,
        ),
    # stop word in cutprice offer
    FeedParams(
        offer_id='11',
        offer_url='stop_word_cutprice',
        category_id=12345,
        tag_name='title',
        tag_value='слово discount',
        offer_color='white',
        flags=OfferFlags.IS_CUTPRICE,
        feed_group_id_hash='',
        is_dsbs=False,
        model_id=100,
    ),
    FeedParams(
        offer_id='12',
        offer_url='white_cpc_hidden_by_white_rule',
        category_id=12345,
        tag_name='title',
        tag_value='бесп*латный product',
        offer_color='white',
        flags=0,
        feed_group_id_hash='',
        is_dsbs=False,
        model_id=100,
    ),
]
WARE_MD5 = '09lEaAKkQll1XTaaaaaaaQ'
BLUE_URL_PATTERN = 'https://market.yandex.ru/product/{model_id}?offerid={waremd5}&sku={market_sku}'


def create_offer(
        offer_id,
        offer_url,
        category_id,
        tag_name,
        tag_value,
        offer_color,
        flags,
        feed_group_id_hash,
        is_dsbs,
        model_id,
):
    offer = default_genlog()
    offer['offer_id'] = offer_id
    if offer_color == 'blue':
        offer['url'] = BLUE_URL_PATTERN.format(
            market_sku=offer_id, waremd5=WARE_MD5, model_id=model_id
        )
    else:
        offer['url'] = 'www.test.ru/' + offer_url
    offer['category_id'] = category_id
    if tag_name:
        offer[tag_name] = tag_value
    if offer_color == 'blue':
        offer['is_blue_offer'] = True
        offer['ware_md5'] = WARE_MD5
        offer['market_sku'] = int(offer_id)
    offer['flags'] = flags
    if is_dsbs:
        offer['cpa'] = 4
        offer['is_dsbs'] = True
    offer['model_id'] = model_id
    return offer


@pytest.fixture(scope='module')
def genlog_rows():
    offers = [create_offer(*offer) for offer in FEED]
    return offers


@pytest.fixture(scope="module")
def genlog_table(yt_server, genlog_rows):
    genlog_table = GenlogOffersTable(yt_server,  ypath_join(get_yt_prefix(), '0000'), genlog_rows)
    genlog_table.dump()
    return genlog_table


@pytest.yield_fixture(scope='module')
def rules_workflow(genlog_table, yt_server):
    stop_words_rules = StopWordHidingRules()
    # по умолчанию для белого и синего ([0, 1])
    stop_words_rules.add_rule(
        word='action',
        tags=['description'],
        exclude_cut_price=True,
    )
    # только для красного
    stop_words_rules.add_rule(
        word='redaction',
        tags=['description'],
        rgb=[2],
    )
    # явно указанное для белого и синего
    stop_words_rules.add_rule(
        word='discount',
        tags=['title'],
        category_whitelist=[56789],
        rgb=[0, 1],
    )
    # при пустом списке используем значение по умолчанию, т.е белый и синий ([0, 1])
    stop_words_rules.add_rule(
        word='bargain',
        tags=['sales_notes'],
        forbidden=True,
        rgb=[],
    )
    # правило по скрытию указано только для белого, синие офферы не будут скрываться
    stop_words_rules.add_rule(
        word='free',
        tags=['title'],
        rgb=[0],
    )
    stop_words_rules.add_rule(
        word='бесп*латный',
        tags=['title'],
        rgb=[0],
    )
    # правило только для cpa-офферов
    stop_words_rules.add_rule(
        word='badbadcpa',
        tags=['sales_notes'],
        rgb=[1],
    )

    rules_resources = {
        'stop_word_hiding_rules_json': stop_words_rules,
    }
    input_table_paths = [genlog_table.get_path()]

    with OffersProcessorTestEnv(
            yt_server,
            use_genlog_scheme=True,
            input_table_paths=input_table_paths,
            **rules_resources
    ) as idx_env:
        idx_env.execute()
        idx_env.verify()
        yield idx_env


@pytest.mark.parametrize(
    'param, expected',
    [
        (('ok', None, None), 0),
        (('ok_offer_whitelist', 2, 100), 0),
        (('ok_long_description', None, None), 0),
        (('allowed_stop_word_cutprice', None, None), 0),
        (('blue_offer_not_hidden_by_white_rule', 9, 100), 0),
        (('dsbs_not_hidden_by_white_rule', None, None), 0),
    ]
)
def test_ok(rules_workflow, param, expected):
    '''
    Тест проверяет, что урлы офферов, которые не были отклонены по стоп-словам есть в генлогах,
    а также, что маска forbidden_market_mask для них выставленна в значение по умолчанию - 0.
    param - тупл из трех значений: (url, market_sku, feed_group_id_hash).
        Второй параметр нужен для синих офферов, потому как для них автоматически
        генерируется новый url по шаблону:
        https://beru.ru/product/{market_sku}?offerid={waremd5}
        Третий параметр -- аналог market_sku для красного урла:
        https://bringly.ru/product/{feed_group_id_hash}?offerid={waremd5}
        В тестах waremd5 у всех синих и красных одинаков, поэтому досатточно знать market_sku
    expected - битовая маска forbidden_market_mask, показывающая на каких маркетах данный оффер запрещен.
    '''
    url, market_sku, model_id = param
    if market_sku:
        offer_url = BLUE_URL_PATTERN.format(market_sku=market_sku, waremd5=WARE_MD5, model_id=model_id)
    else:
        offer_url = 'www.test.ru/{}'.format(url)
    assert_that(
        rules_workflow,
        HasGenlogRecord({
            'url': offer_url,
            'forbidden_market_mask': expected
        }),
        u'Оффер, не попадающий под фильтрацию, имеет нулевую маску'
    )


def test_hidden(rules_workflow):
    '''
    Тест проверяет, что в генлог не попадают офферы, отклоненные по стоп-словам
    '''
    not_expected_offers = ['3', '4', '6', '7', '71', '8', '11', '12']

    for offer_id in not_expected_offers:
        assert_that(
            rules_workflow,
            is_not(HasGenlogRecord({'offer_id': offer_id}))
        )
