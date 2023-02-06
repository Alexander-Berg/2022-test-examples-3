# coding: utf-8

import allure
import pytest
import re

from common import report_response, white_only
from constants import (
    MAGIC_PP,
    MOSCOW_RID,
    BLUE
)
from hamcrest import (
    assert_that,
    equal_to,
    has_entry,
    has_items,
)

OK = "ok"
MOBILE_NID = '54726'
BOOK = 'BOOK'
MODEL = 'MODEL'
SHOP = 'SHOP'
CARD = 'CARD'
CATALOG = 'CATALOG'


def getShardName(name, shard):
    return name + "-" + str(shard)


class ShardName(object):
    def __init__(self, name, shards_num=None):
        self.shards = set()
        if not shards_num:
            self.shards.add(name)
        else:
            for shard in range(shards_num):
                self.shards.add(getShardName(name, shard))

    def get_shards(self):
        return self.shards


INDEX_COLLECTIONS = [BOOK, MODEL, SHOP, CARD, CATALOG]
INDEX_COLLECTIONS_BLUE_ON_WHITE = {
    SHOP: ShardName("basesearch16", 16).get_shards(),
    MODEL: ShardName("basesearch-model", 8).get_shards(),
    BOOK: ShardName("basesearch-book", 8).get_shards(),
}

INDEX_COLLECTIONS_WHITE = {
    SHOP: ShardName("basesearch16", 16).get_shards(),
    MODEL: ShardName("basesearch-model", 8).get_shards(),
    BOOK: ShardName("basesearch-book", 8).get_shards(),
    CARD: ShardName("cardsearch").get_shards(),
    CATALOG: ShardName("catalogsearch").get_shards(),
}


@pytest.fixture()
def results_busters_collections():
    with allure.step('Проверка ответа репорта'):
        response = report_response({
            'place': 'print_doc',
            'feed_shoffer_id': '1-1',
        })
        return response


@pytest.fixture()
def results_busters_blue_on_white_collections():
    with allure.step('Проверка ответа репорта'):
        response = report_response({
            'place': 'prime',
            'text': 'iphone',
            'rids': MOSCOW_RID,
            'pp': MAGIC_PP,
            'rgb': BLUE
        })
        return response


@allure.story('index_collection')
@allure.feature('report_place_print_doc')
@allure.feature('index')
@allure.feature('statuses')
@allure.issue('https://st.yandex-team.ru/MARKETINDEXER-32230')
def test_status_index_collections(results_busters_collections):
    '''Проверяем, что в выдаче нет не ОК статусов'''

    results = results_busters_collections.json()['debug']['metasearch']['clients']
    for collection in INDEX_COLLECTIONS:
        '''Проверяем, что у нас есть коллекция из списка и её статус ОК'''
        assert_that(
            results,
            has_entry(
                collection,
                has_entry(
                    'status', equal_to(OK)
                )
            ),
            u'В выдаче ожидаются все индексы из списка со статусом ОК'
        )


def get_status(line):
    match = re.search('"status":"(.*)",', line)
    if match:
        return match.group(1)
    else:
        return None


def get_statuses(lines):
    statuses = []
    for line in lines:
        status = get_status(line)
        if status:
            statuses.append(status)

    return statuses


def get_shard(line):
    match = re.search('"searchScript":"http:[^:]*:[0-9]*/([^"@]*).*",', line)
    if match:
        return match.group(1)
    else:
        return None


def get_shards(lines):
    shards = set()
    for line in lines:
        shard = get_shard(line)
        if shard:
            shards.add(shard)

    return shards


def get_collections(collections):
    all_shards = set()
    for _, shards in collections.items():
        all_shards = all_shards.union(shards)

    return all_shards


def check_collections(response, collections):
    lines = response.text.encode('utf-8').split('\n')
    res_statuses = get_statuses(lines)
    res_shards = get_shards(lines)
    all_shards = set()
    for _, shards in collections.items():
        all_shards = all_shards.union(shards)

    assert_that(
        res_shards,
        has_items(*list(all_shards)),
        u'Список шардов должен содержать все требуемые шарды')

    assert_that(
        res_statuses,
        has_items(equal_to(OK)),
        u'Все статусы должны быть ОК'
        )


@allure.story('index_collection_white')
@allure.feature('report_place_print_doc')
@allure.feature('index')
@allure.feature('collections_white')
@allure.issue('https://st.yandex-team.ru/MARKETINDEXER-32230')
@white_only
def test_status_index_collections_white(results_busters_collections):
    '''Проверяем, что в выдаче весь список коллеций для белого маркета'''

    check_collections(results_busters_collections, INDEX_COLLECTIONS_WHITE)


@allure.story('index_collection_blue')
@allure.feature('report_place_prime')
@allure.feature('index')
@allure.feature('collections_blue')
@allure.issue('https://st.yandex-team.ru/MARKETINDEXER-32230')
@allure.issue('https://st.yandex-team.ru/MARKETINDEXER-36386')
@pytest.mark.skip(reason="Blue Shard Must Die!! (c)")
def test_status_index_collections_blue(results_busters_blue_on_white_collections):
    '''Проверяем, что в выдаче весь список коллеций для синего маркета'''

    check_collections(results_busters_blue_on_white_collections, INDEX_COLLECTIONS_BLUE_ON_WHITE)
