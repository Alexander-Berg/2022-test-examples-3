# coding=utf-8
"""
Тест используется для проверки работы вызова indexerf prepare_web --is-herf
"""

import pytest

from hamcrest import assert_that, equal_to, has_item, has_length, has_entries

from yt.wrapper import ypath_join

from market.idx.yatf.resources.yt_table_resource import YtTableResource
from market.idx.yatf.resources.yt_stuff_resource import get_yt_prefix
from market.idx.yatf.resources.shops_dat import ShopsDat
from market.idx.offers.yatf.utils.fixtures import default_shops_dat

from market.idx.generation.indexerf.yatf.test_env import YtIndexErfTestEnv, ErfMode
from market.idx.generation.indexerf.yatf.resources.yt_erf_data import YTPrepareWebData
from market.idx.generation.indexerf.yatf.resources.erf_output import YtPrepareWebOutput

from mapreduce.yt.python.table_schema import extract_column_attributes


@pytest.fixture(scope='module')
def web_features_table(yt_stuff):
    rows = [
        {'Host': 'https://market.yandex.ru', 'Valuable': 1},
        {'Host': 'https://white_shop.ru', 'Valuable': 3},
        {'Host': 'https://blue_shop.ru', 'Valuable': 4},
        {'Host': 'https://not_found_shop.ru', 'Valuable': 5},
        {'Host': 'https://white_cpa_shop.ru', 'Valuable': 6},
    ]
    path = ypath_join(get_yt_prefix(), 'features')
    schema = [
        dict(name='Host', type='string'),
        dict(name='Valuable', type='uint64'),
    ]

    return YtTableResource(yt_stuff, path, data=rows, fail_on_exists=False, attributes={'schema': schema})


@pytest.fixture(scope='module')
def shops_dat_file():
    white_shop = default_shops_dat()
    white_shop['domain'] = 'https://white_shop.ru'

    blue_shop = default_shops_dat()
    blue_shop['domain'] = 'https://blue_shop.ru'
    blue_shop['blue_status'] = 'REAL'

    white_cpa_shop = default_shops_dat()
    white_cpa_shop['domain'] = 'https://white_cpa_shop.ru'
    white_cpa_shop['cpa'] = 'REAL'

    return ShopsDat(shops=[white_shop, blue_shop, white_cpa_shop])


@pytest.fixture(scope='module')
def input_data(web_features_table, shops_dat_file):
    return YTPrepareWebData(web_features_table, shops_dat_file)


@pytest.fixture(scope='module')
def output_data():
    return YtPrepareWebOutput(ypath_join(get_yt_prefix(), 'result'))


@pytest.yield_fixture(scope='module')
def workflow(yt_stuff, input_data, output_data):
    with YtIndexErfTestEnv(input=input_data, output=output_data) as env:
        env.execute(ErfMode.PREPARE, yt_stuff, is_herf=True)
        env.verify()
        yield env


@pytest.fixture(scope='module')
def blue_features_table(workflow):
    return workflow.result_tables['blue']


@pytest.fixture(scope='module')
def features_table(workflow):
    return workflow.result_tables['white']


@pytest.fixture(scope='module')
def model_features_table(workflow):
    return workflow.result_tables['models']


def test_result_table_exists(yt_stuff, features_table):
    assert_that(yt_stuff.get_yt_client().exists(features_table.table_path), 'Table exist')


def test_result_table_schema(features_table):
    assert_that(extract_column_attributes(list(features_table.schema)), equal_to([
        {'required': False, "name": "Host", "type": "string"},
        {'required': False, "name": "Data", "type": "string"}
    ]), "Schema is correct")


def test_result_table_data(features_table):
    expected_data = [
        {'Host': 'white_shop.ru'},
        {'Host': 'market.yandex.ru'},
        {'Host': 'white_cpa_shop.ru'},
    ]
    assert_that(features_table.data, has_length(len(expected_data)), "Records count is correct")
    for elem in expected_data:
        assert_that(features_table.data, has_item(has_entries(elem)), "Herf data lost")


def test_blue_result_table_exists(yt_stuff, blue_features_table):
    assert_that(yt_stuff.get_yt_client().exists(blue_features_table.table_path), 'Table exist')


def test_blue_result_table_data(blue_features_table):
    assert_that(blue_features_table.data, has_length(0), "Records count is correct")


def test_model_result_table_exists(yt_stuff, blue_features_table):
    assert_that(yt_stuff.get_yt_client().exists(blue_features_table.table_path), 'Table exist')


def test_model_result_table_data(model_features_table):
    expected_data = [
        {'Host': 'market.yandex.ru'},
    ]
    assert_that(model_features_table.data, has_length(len(expected_data)), "Records count is correct")
    for elem in expected_data:
        assert_that(model_features_table.data, has_item(has_entries(elem)), "Herf data lost")
