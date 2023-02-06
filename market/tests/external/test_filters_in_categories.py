# coding: utf-8

import allure
import pytest

from common import report_response
from constants import (
    MAGIC_PP,
    MOSCOW_RID
)
from hamcrest import (
    assert_that,
    has_items,
    greater_than,
    has_entries
)


@pytest.fixture()
def results_diapers_blue():
    with allure.step('Проверка ответа репорта'):
        response = report_response({
            'place': 'prime',
            'pp': MAGIC_PP,
            'rids': MOSCOW_RID,
            'rgb': 'blue',
            'use-multi-navigation-trees': 1,
            'viewtype': 'grid',
            'new-picture-format': 1,
            'hid': '90799',
            'nid': '78092',
        })
        return response.json()


@pytest.fixture()
def results_busters_blue():
    with allure.step('Проверка ответа репорта'):
        response = report_response({
            'place': 'prime',
            'pp': MAGIC_PP,
            'rids': MOSCOW_RID,
            'rgb': 'blue',
            'use-multi-navigation-trees': 1,
            'viewtype': 'grid',
            'new-picture-format': 1,
            'hid': '512743',
            'nid': '80652',
        })
        return response.json()


@pytest.fixture()
def results_diapers_white():
    with allure.step('Проверка ответа репорта'):
        response = report_response({
            'place': 'prime',
            'pp': MAGIC_PP,
            'rids': MOSCOW_RID,
            'rgb': 'green_with_blue',
            'viewtype': 'grid',
            'hid': '90799',
            'prun-count': '100'
        })
        return response.json()


@pytest.fixture()
def results_busters_white():
    with allure.step('Проверка ответа репорта'):
        response = report_response({
            'place': 'prime',
            'pp': MAGIC_PP,
            'rids': MOSCOW_RID,
            'rgb': 'green_with_blue',
            'viewtype': 'list',
            'hid': '512743',
            'nid': '55063',
            'prun-count': '100'
        })
        return response.json()


@allure.story('filter_exists')
@allure.feature('prime')
@allure.feature('filter')
@allure.issue('https://st.yandex-team.ru/MARKETINDEXER-31790')
@pytest.mark.parametrize("results_diapers_func", [results_diapers_blue, results_diapers_white])
def test_diapers_filters_blue(results_diapers_func):
    '''Проверяем, что в выдаче в категории Подгузники есть фильтры'''

    results_diapers = results_diapers_func()
    filters = results_diapers['filters']
    assert_that(len(filters), greater_than(2))

    assert_that(filters, has_items(has_entries({"id": "7893318", "xslname": "vendor"})))
    assert_that(filters, has_items(has_entries({"id": "10476707", "xslname": "Size"})))
    assert_that(filters, has_items(has_entries({"id": "10476718", "xslname": "NumPieces"})))
    assert_that(filters, has_items(has_entries({"id": "10476701", "xslname": "Reusable"})))

    results = results_diapers['search']['results']
    assert_that(len(results), greater_than(1))

    result_filters = results[0]["filters"]
    assert_that(result_filters, has_items(has_entries({"id": "7893318", "xslname": "vendor"})))
    assert_that(result_filters, has_items(has_entries({"id": "10476707", "xslname": "Size"})))


@allure.story('filter_exists')
@allure.feature('prime')
@allure.feature('filter')
@allure.issue('https://st.yandex-team.ru/MARKETINDEXER-31790')
@pytest.mark.parametrize("results_busters_func", [results_busters_blue, results_busters_white])
def test_busters_filters_white(results_busters_func):
    '''Проверяем, что в выдаче в категории Автокресла есть фильтры'''
    results_busters = results_busters_func()

    filters = results_busters['filters']
    assert_that(len(filters), greater_than(2))

    assert_that(filters, has_items(has_entries({"id": "7893318", "xslname": "vendor"})))
    assert_that(filters, has_items(has_entries({"id": "4882947", "xslname": "Group"})))
    assert_that(filters, has_items(has_entries({"id": "13887626", "xslname": "color_glob"})))
    assert_that(filters, has_items(has_entries({"id": "15830676", "xslname": "fixation"})))

    results = results_busters['search']['results']
    assert_that(len(results), greater_than(1))

    result_filters = results[0]["filters"]
    assert_that(result_filters, has_items(has_entries({"id": "7893318", "xslname": "vendor"})))
    assert_that(result_filters, has_items(has_entries({"id": "4882947", "xslname": "Group"})))
    assert_that(result_filters, has_items(has_entries({"id": "15830676", "xslname": "fixation"})))
