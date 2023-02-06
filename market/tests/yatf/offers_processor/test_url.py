# coding=utf-8

"""

Url - адрес страницы товара на сайте магазина.
Не длиннее 512 символов. У оффлайн магазина может быть не указан

Только позитивные кейсы из
https://a.yandex-team.ru/arc/trunk/arcadia/market/idx/feeds/feedparser/test/yatf/positive/test_url.py

"""

import pytest
from hamcrest import assert_that

from market.idx.offers.yatf.utils.fixtures import default_genlog, default_shops_dat
from market.idx.offers.yatf.test_envs.offers_processor import OffersProcessorTestEnv
from market.idx.offers.yatf.matchers.offers_processor.env_matchers import HasGenlogRecord
from market.idx.yatf.resources.shops_dat import ShopsDat
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from yt.wrapper.ypath import ypath_join
from market.idx.offers.yatf.resources.offers_indexer.genlog_offers_table import GenlogOffersTable


test_data = [
    {  # http
        'offer_id': '1',
        'url': 'ya.ru'
    },
    {  # https
        'offer_id': '2',
        'url': 'https://ya.ru'
    },
    {  # 512 - max length
        'offer_id': '3',
        'url': 'https://www.1.ru/test/test1/test2/test3/test4/test5/'
               'test6/test7/test8/test9/test10/test11/test12/test13/'
               'test14/test15/test16/test17/test18/test123/test11/'
               'test21/test31/test41/test51/test61/test71/test81/'
               'test91/test101/test111/test12/test113/test114/'
               'test115/test116/test117/test118?id=6&a=test8&b=test9&'
               'c=test10&d=test11&e=test12&f=test13&aa=test14&bb=test'
               '15&cc=test16&dd=test17&ee=test18&ff=test123&aaa=test11'
               '&bbb=test21&ccc=test31&qw=test41&qwe=test51&qwer=test6'
               '1&qwert=test71&qwerty=test81&asd=test91&bvn=t-512'
    },
    {
        'offer_id': '4',
        'url': 'https://localhost'
    },
    {  # has port
        'offer_id': '5',
        'url': 'https://whatever.com:80/hi'
    },
    {  # has double slashes in path
        'offer_id': '6',
        'url': 'https://ya.ru//page?param=1'
    },
    {  # Punycoded
        'offer_id': '7',
        'url': 'https://xn--f1aijeow.xn--p1ai/office/'
    },
    {
        'offer_id': '8',
        'url': '1.2.3.4'
    },
    # - -
    # c якорем
    {  # https
        'offer_id': '9',
        'url': 'https://www.1.ru/?id=4#test'
    },
    {  # http
        'offer_id': '10',
        'url': 'www.1.ru/?id=4#test'
    },
    # - -
    # настоящие нормальные урлы
    {
        'offer_id': '11',
        'url': 'https://market.yandex.ru/catalog/65511/list?hid='
               '13371306&track=fr_ctlg&local-offers-first=1'
    },
    {
        'offer_id': '12',
        'url': 'nsk.shop.megafon.ru/connect/'
               'chtarif_g__g_36_tree_1__102681.html'
    },
    {
        'offer_id': '13',
        'url': 'https://github.yandex-team.ru/market-at/market-'
               'autotests/blob/master/io/market-buker-backend/market-'
               'buker-utils/pom.xml'
    },
    # - -
    #  валидные с несколько экзотичными символами
    {
        'offer_id': '14',
        'url': 'en.wikipedia.org/wiki/%22@%22_%28album%29'
    },
    {
        'offer_id': '15',
        'url': 'https://en.wikipedia.org/wiki/"@"'
    },
    {
        'offer_id': '16',
        'url': 'https://ya.ru/&sect;new/&nbsp;param=1'
    },
    {  # по мотивам https://st.yandex-team.ru/MARKETINDEXER-10669#1514565253000
        'offer_id': '17',
        'url': "topperr-store.ru/products#!/<U+200B>Вода-"
               "парфюмированная-для-утюга-3008-3018-3019/p/66752961"
    },
    # - -
    #  вариации на тему кириллицы
    {
        'offer_id': '18',
        'url': 'кириллическийдомен.рф/?id=5'
    },
    {
        'offer_id': '19',
        'url': 'dom.ru/параграф/глава'
    },
    {
        'offer_id': '20',
        'url': 'кириллический.subdom.рф/параграф/?id=5'
    },
    {
        'offer_id': '21',
        'url': 'https://www.1.ru/тест/test?id=6'
    },
    {
        'offer_id': '22',
        'url': 'https://winomir.ru/products/%D0%9F%D1%80%D0%B5%D1%81%'
               'D1%81_%D0%B4%D0%BB%D1%8F_%D0%B2%D0%B8%D0%BD%D0%B0_Farm'
               '_%D0%BC%D0%BE%D0%B4_25',
    }
]


@pytest.fixture(scope="module")
def genlog_rows():
    offers = []
    for data in test_data:
        offer = default_genlog(
            offer_id=data['offer_id'],
            url=data['url']
        )
        offers.append(offer)

    return offers


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


def test_url(workflow):
    for data in test_data:
        assert_that(
            workflow,
            HasGenlogRecord(
                {
                    'offer_id': data['offer_id'],
                    'url': data['url'],
                }
            )
        )


# = = = offline shop & empty offer url


@pytest.fixture(scope="module")
def offline_shop():
    shop = default_shops_dat()
    shop['is_online'] = 'false'
    shop['is_offline'] = 'true'
    return ShopsDat(shops=[shop])


@pytest.yield_fixture(scope="module")
def workflow_offline_shop(yt_server, offline_shop):
    offer = default_genlog(offer_id='empty_url', url='')

    genlog_table = GenlogOffersTable(yt_server,  ypath_join(get_yt_prefix(), '0001'), [offer])
    genlog_table.dump()
    resources = {
        'shops_dat': offline_shop,
    }
    input_table_paths = [genlog_table.get_path()]

    with OffersProcessorTestEnv(
            yt_server,
            use_genlog_scheme=True,
            input_table_paths=input_table_paths,
            **resources
    ) as env:
        env.execute()
        env.verify()
        yield env


def test_offline_shop_without_url(workflow_offline_shop):
    # https://a.yandex-team.ru/arc/trunk/arcadia/market/indexer/src/OfferDocumentBuilder.cpp?rev=3373755&blame=true#L277
    assert_that(
        workflow_offline_shop,
        HasGenlogRecord(
            {
                'offer_id': 'empty_url',
                'url': '__yx_tovar__',
            }
        )
    )
