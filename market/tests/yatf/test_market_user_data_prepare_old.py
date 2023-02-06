# coding=utf-8
import pytest

from hamcrest import assert_that, equal_to

from market.idx.streams.src.prepare_market_user_data.yatf.test_env import PrepareOldDaysTestEnv
from market.idx.yatf.resources.yt_table_resource import YtTableResource

from yt.wrapper.ypath import ypath_join


"""
Тест проверяет YT джобу, которая берет дневые таблицы из выжимок по диапозону дат и готовит входные данные для расчета CPA-стримов
"""

squeeze_schema = [
    dict(name='has_order', type='boolean'),
    dict(name='dwelltime', type='int64'),
    dict(name='has_cpa_click', type='boolean'),
    dict(name='shown_by_metrika', type='boolean'),
    dict(name='device', type='string'),
    dict(name='search_type', type='string'),
    dict(name='pos', type='uint64'),
    dict(name='query', type='string'),
    dict(name='hyper_id', type='string')
]


@pytest.fixture(scope='module')
def table_2020_01_01():
    return [
        # expected to pass
        {'has_order': True, 'dwelltime': 1, 'has_cpa_click': False, 'shown_by_metrika': True,
         'device': 'touch', 'search_type': 'text', 'pos': 10, 'query': 'стаканы', 'hyper_id': '0'},
        # expected to be filtered
        {'has_order': True, 'dwelltime': 1, 'has_cpa_click': False, 'shown_by_metrika': True,
         'device': 'touch', 'search_type': 'text', 'pos': 10, 'query': 'чашки', 'hyper_id': ''},
    ]


@pytest.fixture(scope='module')
def table_2020_07_01():
    return [
        # expected to pass
        {'has_order': True, 'dwelltime': 1, 'has_cpa_click': False, 'shown_by_metrika': True,
         'device': 'touch', 'search_type': 'text', 'pos': 10, 'query': 'наушники', 'hyper_id': '0'},
        # expected to be filtered
        {'has_order': True, 'dwelltime': 1, 'has_cpa_click': False, 'shown_by_metrika': True,
         'device': 'touch', 'search_type': 'text', 'pos': 1000, 'query': 'чашки', 'hyper_id': '0'},
    ]


@pytest.fixture(scope='module')
def table_2020_07_29():
    return [
        # expected to pass
        {'has_order': True, 'dwelltime': 1, 'has_cpa_click': False, 'shown_by_metrika': True,
         'device': 'touch', 'search_type': 'text', 'pos': 10, 'query': 'рации', 'hyper_id': '0'},
        # expected to be filtered
        {'has_order': True, 'dwelltime': 1, 'has_cpa_click': False, 'shown_by_metrika': False,
         'device': 'touch', 'search_type': 'text', 'pos': 10, 'query': 'чашки', 'hyper_id': '0'},
    ]


@pytest.fixture(scope='module')
def table_2020_07_30():
    return [
        # expected to pass
        {'has_order': True, 'dwelltime': 1, 'has_cpa_click': False, 'shown_by_metrika': True,
         'device': 'touch', 'search_type': 'text', 'pos': 10, 'query': 'телефоны', 'hyper_id': '0'},
        # expected to be filtered
        {'has_order': True, 'dwelltime': 1, 'has_cpa_click': False, 'shown_by_metrika': True,
         'device': 'car', 'search_type': 'text', 'pos': 10, 'query': 'чашки', 'hyper_id': '0'},
    ]


@pytest.fixture(scope='module')
def table_2020_08_01():
    return [
        # expected to pass
        {'has_order': True, 'dwelltime': 1, 'has_cpa_click': False, 'shown_by_metrika': True,
         'device': 'touch', 'search_type': 'text', 'pos': 10, 'query': 'ноутбуки', 'hyper_id': '0'},
        # expected to be filtered
        {'has_order': False, 'dwelltime': 1, 'has_cpa_click': False, 'shown_by_metrika': True,
         'device': 'touch', 'search_type': 'text', 'pos': 10, 'query': 'чашки', 'hyper_id': '0'},
    ]


@pytest.fixture(scope='module')
def initial_squeeze_state(yt_stuff,
                          table_2020_01_01,
                          table_2020_07_01,
                          table_2020_07_29,
                          table_2020_07_30,
                          table_2020_08_01):
    squeeze_path = '//home/marketrank/squeeze/daily'
    yt_stuff.get_yt_client().create('map_node', squeeze_path, recursive=True, ignore_existing=True)
    table_2020_01_01_res = YtTableResource(yt_stuff,
                                           ypath_join(squeeze_path, '2020-01-01'),
                                           table_2020_01_01,
                                           attributes={'schema': squeeze_schema})
    table_2020_01_01_res.dump()
    table_2020_07_01_res = YtTableResource(yt_stuff,
                                           ypath_join(squeeze_path, '2020-07-01'),
                                           table_2020_07_01,
                                           attributes={'schema': squeeze_schema})
    table_2020_07_01_res.dump()
    table_2020_07_29_res = YtTableResource(yt_stuff,
                                           ypath_join(squeeze_path, '2020-07-29'),
                                           table_2020_07_29,
                                           attributes={'schema': squeeze_schema})
    table_2020_07_29_res.dump()
    table_2020_07_30_res = YtTableResource(yt_stuff,
                                           ypath_join(squeeze_path, '2020-07-30'),
                                           table_2020_07_30,
                                           attributes={'schema': squeeze_schema})
    table_2020_07_30_res.dump()
    table_2020_08_01_res = YtTableResource(yt_stuff,
                                           ypath_join(squeeze_path, '2020-08-01'),
                                           table_2020_08_01,
                                           attributes={'schema': squeeze_schema})
    table_2020_08_01_res.dump()
    return squeeze_path


@pytest.yield_fixture(scope='module')
def first_run(yt_stuff, initial_squeeze_state):
    resources = {}

    with PrepareOldDaysTestEnv(**resources) as env:
        env.execute(yt_stuff,
                    bin_path=None,
                    market_squeeze_path=initial_squeeze_state,
                    daily_state_path='//home/market/production/indexer/stratocaster/streams/cpa_queries',
                    start_date='2020-02-01',
                    end_date='2020-08-01',
                    days_per_run='2')
        env.verify()
        yield env


@pytest.fixture(scope='module')
def state_after_first_run(first_run):
    return first_run.outputs.get('daily_state_path')


def test_first_run(yt_stuff, state_after_first_run):
    yt = yt_stuff.get_yt_client()
    table_2020_01_01 = ypath_join(state_after_first_run, '2020-01-01')
    table_2020_07_01 = ypath_join(state_after_first_run, '2020-07-01')
    table_2020_07_29 = ypath_join(state_after_first_run, '2020-07-29')
    table_2020_07_30 = ypath_join(state_after_first_run, '2020-07-30')
    table_2020_08_01 = ypath_join(state_after_first_run, '2020-08-01')
    assert_that(not yt.exists(table_2020_01_01))
    assert_that(not yt.exists(table_2020_07_01))
    assert_that(not yt.exists(table_2020_07_29))
    assert_that(yt.exists(table_2020_07_30))
    assert_that(yt.exists(table_2020_08_01))


@pytest.yield_fixture(scope='module')
def second_run(yt_stuff, initial_squeeze_state):
    resources = {}

    with PrepareOldDaysTestEnv(**resources) as env:
        env.execute(yt_stuff,
                    bin_path=None,
                    market_squeeze_path=initial_squeeze_state,
                    daily_state_path='//home/market/production/indexer/stratocaster/streams/cpa_queries',
                    start_date='2020-02-01',
                    end_date='2020-08-01',
                    days_per_run='2')
        env.verify()
        yield env


@pytest.fixture(scope='module')
def state_after_second_run(second_run):
    return second_run.outputs.get('daily_state_path')


def test_second_run(yt_stuff, state_after_second_run):
    yt = yt_stuff.get_yt_client()
    table_2020_01_01 = ypath_join(state_after_second_run, '2020-01-01')
    table_2020_07_01 = ypath_join(state_after_second_run, '2020-07-01')
    table_2020_07_29 = ypath_join(state_after_second_run, '2020-07-29')
    table_2020_07_30 = ypath_join(state_after_second_run, '2020-07-30')
    table_2020_08_01 = ypath_join(state_after_second_run, '2020-08-01')
    assert_that(not yt.exists(table_2020_01_01))
    assert_that(yt.exists(table_2020_07_01))
    assert_that(yt.exists(table_2020_07_29))
    assert_that(yt.exists(table_2020_07_30))
    assert_that(yt.exists(table_2020_08_01))


@pytest.yield_fixture(scope='module')
def third_run(yt_stuff, initial_squeeze_state):
    resources = {}

    with PrepareOldDaysTestEnv(**resources) as env:
        env.execute(yt_stuff,
                    bin_path=None,
                    market_squeeze_path=initial_squeeze_state,
                    daily_state_path='//home/market/production/indexer/stratocaster/streams/cpa_queries',
                    start_date='2020-02-01',
                    end_date='2020-08-01',
                    days_per_run='2')
        env.verify()
        yield env


@pytest.fixture(scope='module')
def state_after_third_run(third_run):
    return third_run.outputs.get('daily_state_path')


def test_third_run(yt_stuff, state_after_third_run):
    yt = yt_stuff.get_yt_client()
    table_2020_01_01 = ypath_join(state_after_third_run, '2020-01-01')
    table_2020_07_01 = ypath_join(state_after_third_run, '2020-07-01')
    table_2020_07_29 = ypath_join(state_after_third_run, '2020-07-29')
    table_2020_07_30 = ypath_join(state_after_third_run, '2020-07-30')
    table_2020_08_01 = ypath_join(state_after_third_run, '2020-08-01')
    assert_that(not yt.exists(table_2020_01_01))
    assert_that(yt.exists(table_2020_07_01))
    assert_that(yt.exists(table_2020_07_29))
    assert_that(yt.exists(table_2020_07_30))
    assert_that(yt.exists(table_2020_08_01))


@pytest.fixture(scope='module')
def expected_table_2020_07_01():
    return [
        {'text': 'наушники', 'model_id': '0'},
    ]


@pytest.fixture(scope='module')
def expected_table_2020_07_29():
    return [
        {'text': 'рации', 'model_id': '0'},
    ]


@pytest.fixture(scope='module')
def expected_table_2020_07_30():
    return [
        {'text': 'телефоны', 'model_id': '0'},
    ]


@pytest.fixture(scope='module')
def expected_table_2020_08_01():
    return [
        {'text': 'ноутбуки', 'model_id': '0'},
    ]


def test_result_table(yt_stuff,
                      state_after_third_run,
                      expected_table_2020_07_01,
                      expected_table_2020_07_29,
                      expected_table_2020_07_30,
                      expected_table_2020_08_01):
    table_2020_07_01_path = ypath_join(state_after_third_run, '2020-07-01')
    table_2020_07_29_path = ypath_join(state_after_third_run, '2020-07-29')
    table_2020_07_30_path = ypath_join(state_after_third_run, '2020-07-30')
    table_2020_08_01_path = ypath_join(state_after_third_run, '2020-08-01')
    table_2020_07_01 = YtTableResource(yt_stuff, table_2020_07_01_path, load=True)
    table_2020_07_29 = YtTableResource(yt_stuff, table_2020_07_29_path, load=True)
    table_2020_07_30 = YtTableResource(yt_stuff, table_2020_07_30_path, load=True)
    table_2020_08_01 = YtTableResource(yt_stuff, table_2020_08_01_path, load=True)
    sorted_by_text = lambda lst: sorted(lst, key=lambda row: row['text'])
    assert_that(sorted_by_text(list(table_2020_07_01.data)),
                equal_to(sorted_by_text(expected_table_2020_07_01)))
    assert_that(sorted_by_text(list(table_2020_07_29.data)),
                equal_to(sorted_by_text(expected_table_2020_07_29)))
    assert_that(sorted_by_text(list(table_2020_07_30.data)),
                equal_to(sorted_by_text(expected_table_2020_07_30)))
    assert_that(sorted_by_text(list(table_2020_08_01.data)),
                equal_to(sorted_by_text(expected_table_2020_08_01)))


def test_ttl(yt_stuff, state_after_third_run):
    yt = yt_stuff.get_yt_client()
    table_2020_07_01_attr_path = ypath_join(state_after_third_run, '2020-07-01', '@expiration_time')
    table_2020_07_29_attr_path = ypath_join(state_after_third_run, '2020-07-29', '@expiration_time')
    table_2020_07_30_attr_path = ypath_join(state_after_third_run, '2020-07-30', '@expiration_time')
    table_2020_08_01_attr_path = ypath_join(state_after_third_run, '2020-08-01', '@expiration_time')
    assert_that(yt.exists(table_2020_07_01_attr_path))
    assert_that(yt.get(table_2020_07_01_attr_path))
    assert_that(yt.exists(table_2020_07_29_attr_path))
    assert_that(yt.get(table_2020_07_29_attr_path))
    assert_that(yt.exists(table_2020_07_30_attr_path))
    assert_that(yt.get(table_2020_07_30_attr_path))
    assert_that(yt.exists(table_2020_08_01_attr_path))
    assert_that(yt.get(table_2020_08_01_attr_path))
