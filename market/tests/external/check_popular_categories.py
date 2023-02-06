# coding: utf-8

import allure
import pytest

from common import (
    report_response,
    white_only,
    skip_testing
)
from constants import (
    MOSCOW_RID,
    RUR_CURRENCY
)
from hamcrest import (
    assert_that,
    greater_than,
)

PP = 1


categories_to_check = [
    '91491',
    '90639',
    '91013',
    '90555',
    '6427100',
    '90566',
    '10498025',
    '91052',
    '90490',
    '91259',
    '15450081',
    '91031',
    '138608',
    '10470548',
    '2724669',
    '237418',
    '91148',
    '90580',
    '91019'
]


@pytest.fixture(params=categories_to_check)
def results_busters_white_categories_offers(request):
    with allure.step('Проверка ответа репорта'):
        response = report_response({
            'place': 'prime',
            'pp': PP,
            'rids': MOSCOW_RID,
            'rgb': 'green_with_blue',
            'currency': RUR_CURRENCY,
            'hid': request.param,
            'numdoc': 5,
            'page': 1,
            'onstock': 1,
            'entities': 'offer',
            'regset': 0,
            'prun-count': 1000,
        })
        return response.json()


@pytest.fixture(params=categories_to_check)
def results_busters_white_categories_models(request):
    with allure.step('Проверка ответа репорта'):
        response = report_response({
            'place': 'prime',
            'pp': PP,
            'rids': MOSCOW_RID,
            'rgb': 'green_with_blue',
            'currency': RUR_CURRENCY,
            'hid': request.param,
            'numdoc': 5,
            'page': 1,
            'onstock': 1,
            'entities': 'model',
            'regset': 0,
        })
        return response.json()


@allure.story('offers_presented')
@allure.feature('report_place_prime')
@allure.feature('popular')
@allure.feature('category')
@allure.issue('https://st.yandex-team.ru/MARKETINDEXER-32231')
@white_only
def test_popular_category_offers_white(results_busters_white_categories_offers):
    '''Проверяем, что в выдаче есть оффера'''

    results = results_busters_white_categories_offers['search']['results']
    assert_that(len(results), greater_than(1))

    total_offers = results_busters_white_categories_offers['search']['totalOffers']
    assert_that(total_offers, greater_than(0))


@allure.story('models_presented')
@allure.feature('report_place_prime')
@allure.feature('popular')
@allure.feature('category')
@allure.issue('https://st.yandex-team.ru/MARKETINDEXER-32231')
@white_only
@skip_testing
def test_popular_category_models_white(results_busters_white_categories_models):
    '''Проверяем, что в выдаче есть оффера'''

    results = results_busters_white_categories_models['search']['results']
    assert_that(len(results), greater_than(1))

    total_models = results_busters_white_categories_models['search']['totalModels']
    assert_that(total_models, greater_than(0))
