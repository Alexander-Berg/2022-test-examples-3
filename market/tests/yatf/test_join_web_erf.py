# coding=utf-8
"""
Тест используется для проверки работы вызова indexerf join_web
"""

import pytest

from hamcrest import assert_that, has_length, has_item

from yt.wrapper import ypath_join

from market.idx.yatf.resources.yt_table_resource import YtTableResource
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix

from market.idx.generation.indexerf.yatf.test_env import YtIndexErfTestEnv, ErfMode
from market.idx.generation.indexerf.yatf.resources.yt_erf_data import YTJoinWebData
from market.idx.generation.indexerf.yatf.resources.erf_output import YtJoinWebOutput


@pytest.fixture(scope='module')
def yt_filtered_features_table(yt_stuff):
    rows = [
        {'Host': 'market.yandex.ru', 'Data': '1', 'url': 'market.yandex.ru/product/10?sku=1'},   # фича приджойнится к офферу по урлу
        {'Host': 'pokupki.market.yandex.ru', 'Data': '2', 'url': 'pokupki.market.yandex.ru/product/2'},   # не приджойнится к офферу по урлу
    ]
    path = ypath_join(get_yt_prefix(), 'erf')
    schema = [
        {'required': False, "name": "Host", "type": "string"},
        {'required': False, "name": "Data", "type": "string"},
        {'required': False, "name": "url", "type": "string"},
    ]

    return YtTableResource(yt_stuff, path, data=rows, attributes={'schema': schema})


@pytest.fixture(scope='module')
def yt_market_urls(yt_stuff):
    rows = [
        {'ware_md5': 'md1', 'url': 'market.yandex.ru/product/10?sku=1', 'part': 0, 'path': '/product/10?sku=1', 'msku': 1, 'is_fake_msku_offer': False},
        {'ware_md5': 'md2', 'url': 'market.yandex.ru/product/10?sku=2', 'part': 0, 'path': '/product/10?sku=2', 'msku': 2, 'is_fake_msku_offer': False},
    ]

    path = ypath_join(get_yt_prefix(), 'marekt_urls')
    schema = [
        {'required': False, "name": "ware_md5", "type": "string"},
        {'required': False, "name": "url", "type": "string"},
        {'required': False, "name": "part", "type": "uint64"},
        {'required': False, "name": "msku", "type": "int64"},
        {'required': False, "name": "is_fake_msku_offer", "type": "boolean"},
        {'required': False, "name": "path", "type": "string"},
    ]

    return YtTableResource(yt_stuff, path, data=rows, attributes={'schema': schema})


@pytest.fixture(scope='module')
def input_data(yt_filtered_features_table, yt_market_urls):
    return YTJoinWebData(yt_filtered_features_table, yt_market_urls, True, 1)


@pytest.fixture(scope='module')
def output_data():
    return YtJoinWebOutput('//indexer/result')


@pytest.yield_fixture(scope='module')
def workflow(yt_stuff, input_data, output_data):
    resources = {
        "input": input_data,
        "output": output_data
    }

    with YtIndexErfTestEnv(**resources) as env:
        env.execute(ErfMode.JOIN_WEB, yt_stuff, is_herf=False)
        env.verify()
        yield env


@pytest.fixture(scope='module')
def result_table(workflow):
    return workflow.result_tables['blue']


def test_result_table_exists(yt_stuff, result_table):
    assert_that(yt_stuff.get_yt_client().exists(result_table.table_path), 'Table exist')


def test_result_table_data(result_table):
    assert_that(result_table.data, has_length(1), "Records count is icorrect")
    joined = {
        'Host': 'market.yandex.ru',
        'url': 'market.yandex.ru/product/10?sku=1',
        'ware_md5': 'md1',
        'Data': '1',
        'table_index': 0
    }
    assert_that(result_table.data, has_item(joined), "No joined features")
