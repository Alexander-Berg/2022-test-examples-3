# coding=utf-8
import pytest

from hamcrest import assert_that, equal_to

from market.idx.streams.src.prepare_market_user_data.yatf.test_env import MergeDaysTestEnv
from market.idx.yatf.resources.yt_table_resource import YtTableResource

from yt.wrapper.ypath import ypath_join


"""
Тест проверяет YT джобу, которая берет таблицы для стримов по дням и готовит один стейт
"""


@pytest.fixture(scope='module')
def table_2020_01_01():
    # too old should be ignored
    return [
        {'text': 'лепнина', 'model_id': '0'},
        {'text': 'лепнина', 'model_id': '1'},
        {'text': 'лепнина', 'model_id': '2'},
    ]


@pytest.fixture(scope='module')
def table_2020_06_01():
    return [
        # should be deduplicated
        {'text': 'москва', 'model_id': '0'},
        {'text': 'москва', 'model_id': '0'},
        {'text': 'москва', 'model_id': '2'},
    ]


@pytest.fixture(scope='module')
def table_2020_07_01():
    return [
        {'text': 'питер', 'model_id': '0'},
        {'text': 'питер', 'model_id': '1'},
        {'text': 'питер', 'model_id': '2'},
    ]


@pytest.fixture(scope='module')
def table_2020_08_01():
    return [
        # should be deduplicated
        {'text': 'москва', 'model_id': '2'},
        {'text': 'бар', 'model_id': '1'},
        {'text': 'бар', 'model_id': '2'},
    ]


@pytest.fixture(scope='module')
def state(yt_stuff, table_2020_01_01, table_2020_06_01, table_2020_07_01, table_2020_08_01):
    state_path = '//home/market/production/indexer/stratocaster/streams/preprocess_cpa_queries'
    yt = yt_stuff.get_yt_client()
    yt.create('map_node', state_path, recursive=True, ignore_existing=True)
    table_2020_01_01 = YtTableResource(yt_stuff,
                                       ypath_join(state_path, '2020-01-01'),
                                       table_2020_01_01)
    table_2020_01_01.dump()
    table_2020_06_01 = YtTableResource(yt_stuff,
                                       ypath_join(state_path, '2020-06-01'),
                                       table_2020_06_01)
    table_2020_06_01.dump()
    table_2020_07_01 = YtTableResource(yt_stuff,
                                       ypath_join(state_path, '2020-07-01'),
                                       table_2020_07_01)
    table_2020_07_01.dump()
    table_2020_08_01 = YtTableResource(yt_stuff,
                                       ypath_join(state_path, '2020-08-01'),
                                       table_2020_08_01)
    table_2020_08_01.dump()
    state_attribute_path = ypath_join(state_path, '@last_processed_date')
    yt.set(state_attribute_path, '2020-08-01')
    return state_path


@pytest.yield_fixture(scope='module')
def workflow(yt_stuff, state):
    resources = {}

    with MergeDaysTestEnv(**resources) as env:
        env.execute(yt_stuff,
                    bin_path=None,
                    daily_state_path=state,
                    prod_state_path='//home/market/production/indexer/stratocaster/streams/cpa_queries/2020-08-01',
                    days_to_merge='180')
        env.verify()
        yield env


@pytest.fixture(scope='module')
def prod_state_path(workflow):
    return workflow.outputs.get('prod_state_path')


@pytest.fixture(scope='module')
def expected_result():
    return [
        {'text': 'москва', 'model_id': '0', 'part': 0, 'region': 225, 'value': '1', 'url': 'https://market.yandex.ru/product/0'},
        {'text': 'москва', 'model_id': '2', 'part': 0, 'region': 225, 'value': '1', 'url': 'https://market.yandex.ru/product/2'},
        {'text': 'питер', 'model_id': '0', 'part': 0, 'region': 225, 'value': '1', 'url': 'https://market.yandex.ru/product/0'},
        {'text': 'питер', 'model_id': '1', 'part': 0, 'region': 225, 'value': '1', 'url': 'https://market.yandex.ru/product/1'},
        {'text': 'питер', 'model_id': '2', 'part': 0, 'region': 225, 'value': '1', 'url': 'https://market.yandex.ru/product/2'},
        {'text': 'бар', 'model_id': '1', 'part': 0, 'region': 225, 'value': '1', 'url': 'https://market.yandex.ru/product/1'},
        {'text': 'бар', 'model_id': '2', 'part': 0, 'region': 225, 'value': '1', 'url': 'https://market.yandex.ru/product/2'},
    ]


def test_prod_state_table(yt_stuff, prod_state_path, expected_result):
    prod_state_table = YtTableResource(yt_stuff, prod_state_path, load=True)
    sorted_by_text_and_model = lambda lst: sorted(lst, key=lambda row: (row['text'], row['model_id']))
    assert_that(sorted_by_text_and_model(list(prod_state_table.data)),
                equal_to(sorted_by_text_and_model(expected_result)))
