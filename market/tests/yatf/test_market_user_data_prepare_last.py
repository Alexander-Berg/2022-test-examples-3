# coding=utf-8
import pytest

from hamcrest import assert_that, equal_to

from market.idx.streams.src.prepare_market_user_data.yatf.test_env import PrepareLastDayTestEnv
from market.idx.yatf.resources.yt_table_resource import YtTableResource

from yt.wrapper.ypath import ypath_join


"""
Тест проверяет YT джобу, которая берет последнюю дневную таблицу из выжимок и готовит входные данные для расчета CPA-стримов
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
def yesterday_table():
    return [
        # expected to pass
        {'has_order': True, 'dwelltime': 1, 'has_cpa_click': False, 'shown_by_metrika': True,
         'device': 'touch', 'search_type': 'text', 'pos': 10, 'query': 'ноутбуки', 'hyper_id': '0'},
        {'has_order': True, 'dwelltime': 0, 'has_cpa_click': True, 'shown_by_metrika': True,
         'device': 'desktop', 'search_type': 'text', 'pos': 1, 'query': 'чайники', 'hyper_id': '1'},
        # expected to be filtered
        {'has_order': False, 'dwelltime': 1, 'has_cpa_click': False, 'shown_by_metrika': True,
         'device': 'touch', 'search_type': 'text', 'pos': 10, 'query': 'чашки', 'hyper_id': '0'},
        {'has_order': True, 'dwelltime': 0, 'has_cpa_click': False, 'shown_by_metrika': True,
         'device': 'touch', 'search_type': 'text', 'pos': 10, 'query': 'наушники', 'hyper_id': '0'},
        {'has_order': True, 'dwelltime': 1, 'has_cpa_click': False, 'shown_by_metrika': False,
         'device': 'touch', 'search_type': 'text', 'pos': 10, 'query': 'лампочки', 'hyper_id': '0'},
        {'has_order': True, 'dwelltime': 1, 'has_cpa_click': False, 'shown_by_metrika': True,
         'device': 'calculator', 'search_type': 'text', 'pos': 10, 'query': 'гробы', 'hyper_id': '0'},
        {'has_order': True, 'dwelltime': 1, 'has_cpa_click': False, 'shown_by_metrika': True,
         'device': 'touch', 'search_type': 'textless', 'pos': 10, 'query': 'ракетки', 'hyper_id': '0'},
        {'has_order': True, 'dwelltime': 1, 'has_cpa_click': False, 'shown_by_metrika': True,
         'device': 'touch', 'search_type': 'text', 'pos': 100, 'query': 'книги', 'hyper_id': '0'},
        {'has_order': True, 'dwelltime': 1, 'has_cpa_click': False, 'shown_by_metrika': True,
         'device': 'touch', 'search_type': 'text', 'pos': 10, 'query': 'веники', 'hyper_id': ''},
    ]


@pytest.fixture(scope='module')
def today_table():
    return [
        # expected to pass
        {'has_order': True, 'dwelltime': 1, 'has_cpa_click': False, 'shown_by_metrika': True,
         'device': 'touch', 'search_type': 'text', 'pos': 10, 'query': 'лепнина', 'hyper_id': '0'},
        {'has_order': True, 'dwelltime': 0, 'has_cpa_click': True, 'shown_by_metrika': True,
         'device': 'desktop', 'search_type': 'text', 'pos': 1, 'query': 'статуи', 'hyper_id': '1'},
        # expected to be filtered
        {'has_order': False, 'dwelltime': 1, 'has_cpa_click': False, 'shown_by_metrika': True,
         'device': 'touch', 'search_type': 'text', 'pos': 10, 'query': 'чашки', 'hyper_id': '0'},
        {'has_order': True, 'dwelltime': 0, 'has_cpa_click': False, 'shown_by_metrika': True,
         'device': 'touch', 'search_type': 'text', 'pos': 10, 'query': 'наушники', 'hyper_id': '0'},
        {'has_order': True, 'dwelltime': 1, 'has_cpa_click': False, 'shown_by_metrika': False,
         'device': 'touch', 'search_type': 'text', 'pos': 10, 'query': 'лампочки', 'hyper_id': '0'},
        {'has_order': True, 'dwelltime': 1, 'has_cpa_click': False, 'shown_by_metrika': True,
         'device': 'calculator', 'search_type': 'text', 'pos': 10, 'query': 'гробы', 'hyper_id': '0'},
        {'has_order': True, 'dwelltime': 1, 'has_cpa_click': False, 'shown_by_metrika': True,
         'device': 'touch', 'search_type': 'textless', 'pos': 10, 'query': 'ракетки', 'hyper_id': '0'},
        {'has_order': True, 'dwelltime': 1, 'has_cpa_click': False, 'shown_by_metrika': True,
         'device': 'touch', 'search_type': 'text', 'pos': 100, 'query': 'книги', 'hyper_id': '0'},
        {'has_order': True, 'dwelltime': 1, 'has_cpa_click': False, 'shown_by_metrika': True,
         'device': 'touch', 'search_type': 'text', 'pos': 10, 'query': 'веники', 'hyper_id': ''},
    ]


@pytest.fixture(scope='module')
def squeeze_path(yt_stuff, yesterday_table, today_table):
    squeeze_path = '//home/marketrank/squeeze/daily'
    yt_stuff.get_yt_client().create('map_node', squeeze_path, recursive=True, ignore_existing=True)
    yesterday_table_res = YtTableResource(yt_stuff,
                                          ypath_join(squeeze_path, '2021-08-04'),
                                          yesterday_table,
                                          attributes={'schema': squeeze_schema})
    yesterday_table_res.dump()
    today_table_res = YtTableResource(yt_stuff,
                                      ypath_join(squeeze_path, '2021-08-05'),
                                      today_table,
                                      attributes={'schema': squeeze_schema})
    today_table_res.dump()
    return squeeze_path


@pytest.yield_fixture(scope='module')
def workflow(yt_stuff, squeeze_path):
    resources = {}
    with PrepareLastDayTestEnv(**resources) as env:
        env.execute(yt_stuff,
                    bin_path=None,
                    market_squeeze_path=squeeze_path,
                    daily_state_path='//home/market/production/indexer/stratocaster/streams/cpa_queries')
        env.verify()
        yield env


@pytest.fixture(scope='module')
def daily_state_path(workflow):
    return workflow.outputs.get('daily_state_path')


def test_last_day_existence(yt_stuff, daily_state_path):
    yt = yt_stuff.get_yt_client()
    expected_day = ypath_join(daily_state_path, '2021-08-05')
    assert_that(yt.exists(expected_day))
    needless_day = ypath_join(daily_state_path, '2021-08-04')
    assert_that(not yt.exists(needless_day))


@pytest.fixture(scope='module')
def expected_result():
    return [
        {'text': 'лепнина', 'model_id': '0'},
        {'text': 'статуи', 'model_id': '1'},
    ]


def test_result_table(yt_stuff, daily_state_path, expected_result):
    expected_day_path = ypath_join(daily_state_path, '2021-08-05')
    expected_day_table = YtTableResource(yt_stuff, expected_day_path, load=True)
    sorted_by_text = lambda lst: sorted(lst, key=lambda row: row['text'])
    assert_that(sorted_by_text(list(expected_day_table.data)),
                equal_to(sorted_by_text(expected_result)))


def test_ttl(yt_stuff, daily_state_path):
    yt = yt_stuff.get_yt_client()
    ttl_attribute_path = ypath_join(daily_state_path, '2021-08-05', '@expiration_time')
    assert_that(yt.exists(ttl_attribute_path))
    assert_that(yt.get(ttl_attribute_path))


def test_state_attribute(yt_stuff, daily_state_path):
    yt = yt_stuff.get_yt_client()
    state_attribute_path = ypath_join(daily_state_path, '@last_processed_date')
    assert_that(yt.exists(state_attribute_path))
    assert_that(yt.get(state_attribute_path) == '2021-08-05')
