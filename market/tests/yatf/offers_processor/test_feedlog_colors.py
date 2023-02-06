#!/usr/bin/env python
# coding: utf-8

from hamcrest import assert_that
import pytest

from market.idx.offers.yatf.matchers.offers_processor.env_matchers import HasFeedlogRecord
from market.idx.offers.yatf.test_envs.offers_processor import OffersProcessorTestEnv
from market.idx.offers.yatf.utils.fixtures import default_genlog, default_blue_genlog, default_shops_dat
from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogOffersTable
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from yt.wrapper.ypath import ypath_join

from market.proto.common.common_pb2 import EColor
from market.idx.yatf.resources.shops_dat import ShopsDat


STANDART_FEED_ID = 1
STANDART_SHOP_ID = 1

BLUE_FEED_ID = 2
BLUE_SHOP_ID = 2

FF_VIRTUAL_FEED_ID = 3
FF_VIRTUAL_SHOP_ID = 3


@pytest.fixture(scope="module")
def custom_shops_dat():
    shop = default_shops_dat()
    shop['name'] = 'StandartShop'
    shop['shop_id'] = STANDART_SHOP_ID
    shop['datafeed_id'] = STANDART_FEED_ID
    shop['ff_program'] = 'NO'

    ff_shop = default_shops_dat()
    ff_shop['name'] = 'FullfillmentShop'
    ff_shop['shop_id'] = BLUE_SHOP_ID
    ff_shop['datafeed_id'] = BLUE_FEED_ID
    ff_shop['ff_feed_id'] = FF_VIRTUAL_FEED_ID
    ff_shop['ff_shop_id'] = FF_VIRTUAL_SHOP_ID
    ff_shop['ff_program'] = 'REAL'
    ff_shop['is_supplier'] = True

    ff_virtual_shop = default_shops_dat()
    ff_virtual_shop['name'] = 'VirtualFulfillmentShop'
    ff_virtual_shop['shop_id'] = FF_VIRTUAL_SHOP_ID
    ff_virtual_shop['datafeed_id'] = FF_VIRTUAL_FEED_ID
    ff_virtual_shop['priority_regions'] = '19328'
    ff_virtual_shop['ff_virtual'] = True
    ff_virtual_shop['domain'] = 'test.virtual-domain.ru'

    return ShopsDat(shops=[ff_virtual_shop, ff_shop, shop])


@pytest.fixture(scope="module")
def genlog_rows():
    offer = default_genlog(
        feed_id=STANDART_FEED_ID,
        shop_id=STANDART_SHOP_ID,
        offer_id='1',
    )
    return [offer]


@pytest.fixture(scope="module")
def genlog_table(yt_server, genlog_rows):
    genlog_table = GenlogOffersTable(yt_server,  ypath_join(get_yt_prefix(), '0000'), genlog_rows)
    genlog_table.dump()
    return genlog_table


@pytest.fixture(scope="module")
def blue_genlog_rows():
    offer1 = default_blue_genlog(
        feed_id=BLUE_FEED_ID,
        shop_id=BLUE_SHOP_ID,
        offer_id='1',

        market_sku=777,
    )

    offer2 = default_blue_genlog(
        feed_id=BLUE_FEED_ID,
        shop_id=BLUE_SHOP_ID,
        offer_id='2',
        market_sku=888,
    )

    return [offer1, offer2]


@pytest.fixture(scope="module")
def blue_genlog_table(yt_server, blue_genlog_rows):
    genlog_table = GenlogOffersTable(yt_server,  ypath_join(get_yt_prefix(), '0001'), blue_genlog_rows)
    genlog_table.dump()
    return genlog_table


@pytest.fixture(scope="module")
def virtual_genlog_rows():
    msku = default_genlog(
        feed_id=FF_VIRTUAL_FEED_ID,
        shop_id=FF_VIRTUAL_SHOP_ID,
        offer_id='1',
        market_sku=999,
        is_fake_msku_offer=True,
    )

    not_blue = default_genlog(
        feed_id=FF_VIRTUAL_FEED_ID,
        shop_id=FF_VIRTUAL_SHOP_ID,
        offer_id='2',
        market_sku=123,
    )

    return [msku, not_blue]


@pytest.fixture(scope="module")
def virtual_genlog_table(yt_server, virtual_genlog_rows):
    genlog_table = GenlogOffersTable(yt_server,  ypath_join(get_yt_prefix(), '0002'), virtual_genlog_rows)
    genlog_table.dump()
    return genlog_table


@pytest.fixture(scope='module')
def dropped_offers_table(yt_server):
    yt = yt_server.get_yt_client()

    yt.create(
        'table',
        '//tmp/dropped_offers',
        recursive=True,
        attributes={
            'schema': [
                {'name': 'feed_id', 'type': 'uint64'},
                {'name': 'offer_id', 'type': 'string'},
            ],
            'strict': True,
        },
    )

    data = [
        {
            'feed_id': 101967,  # default feed id
            'offer_id': 'dropped-offer1'
        }
    ]
    yt.write_table('//tmp/dropped_offers', data)

    yield '//tmp/dropped_offers'

    yt.remove("//tmp/dropped_offers", force=True)


@pytest.yield_fixture(scope="module")
def workflow(yt_server, dropped_offers_table, genlog_table, blue_genlog_table, virtual_genlog_table, custom_shops_dat):
    input_table_paths = [genlog_table.get_path(), blue_genlog_table.get_path(), virtual_genlog_table.get_path()]

    resources = {
        'shops_utf8_dat': custom_shops_dat,
    }

    with OffersProcessorTestEnv(
        yt_server,
        dropped_offers_table_path=dropped_offers_table,
        use_genlog_scheme=True,
        input_table_paths=input_table_paths,
        **resources
    ) as env:
        env.execute()
        yield env


def test_color(workflow):
    '''
    Тест проверяет корректность раскараски записей в фидлоге в соответствие с цветом магазина
    '''
    assert_that(
        workflow,
        HasFeedlogRecord({'feed_id': STANDART_FEED_ID, 'Color': EColor.Value('C_WHITE')}),
        u'В фидлоге для feed_id {} корректный цвет {}'.format(STANDART_FEED_ID, 'C_WHITE')
    )
    assert_that(
        workflow,
        HasFeedlogRecord({'feed_id': BLUE_FEED_ID, 'Color': EColor.Value('C_BLUE')}),
        u'В фидлоге для feed_id {} корректный цвет {}'.format(BLUE_FEED_ID, 'C_BLUE')
    )


def test_virtual_feed_not_in_feedlog(workflow):
    '''
    Тест проверяет что в фидлог не попадает запись о фиде виртуального магазина
    '''
    assert_that(
        workflow,
        not(HasFeedlogRecord({'feed_id': FF_VIRTUAL_FEED_ID})),
        u'Фид {} виртуального магазина не попал в feedlog'.format(FF_VIRTUAL_FEED_ID)
    )
