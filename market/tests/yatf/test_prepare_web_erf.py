# coding=utf-8
"""
Тест используется для проверки работы вызова indexerf prepare_web
"""

import pytest

from hamcrest import assert_that, equal_to, has_item, has_length, has_entries, all_of

from market.idx.yatf.resources.yt_table_resource import YtTableResource
from market.idx.yatf.resources.shops_dat import ShopsDat
from market.idx.offers.yatf.utils.fixtures import default_shops_dat

from market.idx.generation.indexerf.yatf.test_env import YtIndexErfTestEnv, ErfMode
from market.idx.generation.indexerf.yatf.resources.yt_erf_data import YTPrepareWebData
from market.idx.generation.indexerf.yatf.resources.erf_output import YtPrepareWebOutput

from mapreduce.yt.python.table_schema import extract_column_attributes


erf_web_features_table_path = "//search/erf_web_features"
result_table_dir = "//results"

erf_web_features_table_schema = [
    dict(name='Host', type='string'),
    dict(name='Path', type='string'),
    dict(name='TextFeatures', type='uint64')
]


@pytest.fixture(scope='module')
def web_features_table(yt_stuff):
    rows = [
        # blue url
        {'Host': 'http://market.yandex.ru', 'Path': '/product/10?sku=1', 'TextFeatures': 1},

        # blue url
        {'Host': 'http://market.yandex.ru', 'Path': '/product/10?sku=2&cpa=1', 'TextFeatures': 3},

        # model url
        {'Host': 'http://market.yandex.ru', 'Path': '/product--xiaomi-mi-band/10', 'TextFeatures': 4},

        # host of white shop
        {'Host': 'https://white_shop.ru', 'Path': '/product/4', 'TextFeatures': 5},

        # host of blue shop
        {'Host': 'https://blue_shop.ru', 'Path': '/product/5', 'TextFeatures': 6},

        # host of white cpa shop
        {'Host': 'https://white_cpa_shop.ru', 'Path': '/product/7', 'TextFeatures': 8},

        # not found in shops.dat host
        {'Host': 'https://not_found_shop.ru', 'Path': '/product/6', 'TextFeatures': 7},
    ]
    path = erf_web_features_table_path
    schema = erf_web_features_table_schema

    return YtTableResource(yt_stuff, path, data=rows, fail_on_exists=False, attributes={'schema': schema})


@pytest.fixture(scope='module')
def shops_dat_file():
    white_shop = default_shops_dat()
    white_shop['domain'] = 'https://white_shop.ru'

    white_cpa_shop = default_shops_dat()
    white_cpa_shop['domain'] = 'https://white_cpa_shop.ru'
    white_cpa_shop['cpa'] = 'REAL'

    blue_shop = default_shops_dat()
    blue_shop['domain'] = 'https://blue_shop.ru'
    blue_shop['blue_status'] = 'REAL'
    return ShopsDat(shops=[white_shop, blue_shop, white_cpa_shop])


@pytest.fixture(scope='module')
def input_data(web_features_table, shops_dat_file):
    return YTPrepareWebData(web_features_table, shops_dat_file)


@pytest.fixture(scope='module')
def output_data():
    result_dir = "_".join([result_table_dir])
    return YtPrepareWebOutput(result_dir)


@pytest.yield_fixture(scope='module')
def workflow(yt_stuff, input_data, output_data):
    with YtIndexErfTestEnv(input=input_data, output=output_data) as env:
        env.execute(ErfMode.PREPARE, yt_stuff, is_herf=False)
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


def test_result_table_exists(yt_stuff, blue_features_table):
    assert_that(yt_stuff.get_yt_client().exists(blue_features_table.table_path), 'Table exist')


def test_result_table_schema(features_table):
    assert_that(extract_column_attributes(list(features_table.schema)), equal_to([
        {'required': False, "name": "Host", "type": "string"},
        {'required': False, "name": "Data", "type": "string"},
        {'required': False, "name": "url", "type": "string"},
    ]), "Schema is correct")


def test_result_table_data(features_table):
    assert_that(features_table.data, has_length(2), "Records count is incorrect")

    white = {
        'Host': 'white_shop.ru',
        'url': 'white_shop.ru/product/4',
    }
    white_cpa = {
        'Host': 'white_cpa_shop.ru',
        'url': 'white_cpa_shop.ru/product/7',
    }
    assert_that(features_table.data, all_of(
        has_item(has_entries(white)),
        has_item(has_entries(white_cpa)),
    ), "White shop web features lost")


def test_blue_result_table_schema(blue_features_table):
    assert_that(extract_column_attributes(list(blue_features_table.schema)), equal_to([
        {'required': False, "name": "Host", "type": "string"},
        {'required': False, "name": "Data", "type": "string"},
        {'required': False, "name": "url", "type": "string"},
    ]), "Schema is correct")


def test_blue_result_table_data(blue_features_table):
    expected = [
        {
            'Host': 'market.yandex.ru',
            'url': 'market.yandex.ru/product/10?sku=1',
            'Data': '(\x01'
        },
        {
            'Host': 'market.yandex.ru',
            'url': 'market.yandex.ru/product/10?sku=2',   # нормализован
            'Data': '(\x03'
        }
    ]

    assert_that(blue_features_table.data, has_length(len(expected)), "Records count is correct")
    assert_that(
        blue_features_table.data,
        all_of(*(
            has_item(has_entries(feature))
            for feature in expected
        )),
        "Blue data lost"
    )


def test_model_result_table_schema(model_features_table):
    assert_that(extract_column_attributes(list(model_features_table.schema)), equal_to([
        {'required': False, "name": "Host", "type": "string"},
        {'required': False, "name": "Data", "type": "string"},
        {'required': False, "name": "model_id", "type": "string"},
    ]), "Schema is correct")


def test_model_result_table_data(model_features_table):
    expected = [
        {
            'Host': 'market.yandex.ru',
            'model_id': '10',
        },
    ]

    assert_that(model_features_table.data, has_length(len(expected)), "Records count is correct")
    assert_that(
        model_features_table.data,
        all_of(*(
            has_item(has_entries(feature))
            for feature in expected
        )),
        "Model data lost"
    )
