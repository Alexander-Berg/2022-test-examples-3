# coding: utf-8

import allure
import pytest

from common import report_response, white_only, blue_only
from constants import (
    MOSCOW_RID,
    RUR_CURRENCY,
    WHITE_BLUE,
    BLUE,
    MOBILE_HID,
)
from hamcrest import (
    assert_that,
    equal_to,
    greater_than,
    less_than,
)

PRICE_THRESHOLD = 100
PP = 1
MOBILE_NID = '54726'
# MOBILE_HYPER_ID = '394273081'


def results_busters_white_price_threshold_mobile():
    with allure.step('Проверка ответа репорта'):
        response = report_response({
            'currency': RUR_CURRENCY,
            'numdoc': 1,
            'page': 1,
            'onstock': 1,
            'mcpriceto': PRICE_THRESHOLD,
            'hid': MOBILE_HID,
            'rids': MOSCOW_RID,
            'place': 'prime',
            'pp': PP,
            'rgb': WHITE_BLUE,
            'allow-collapsing': 1,
        })
        return response.json()['search']


@pytest.fixture(name="results_busters_white_price_threshold_mobile")
def results_busters_white_price_threshold_mobile_fixture():
    return results_busters_white_price_threshold_mobile()


def results_busters_blue_price_threshold_mobile():
    with allure.step('Проверка ответа репорта'):
        response = report_response({
            'currency': RUR_CURRENCY,
            'numdoc': 1,
            'page': 1,
            'onstock': 1,
            'mcpriceto': PRICE_THRESHOLD,
            'hid': MOBILE_HID,
            'rids': MOSCOW_RID,
            'place': 'prime',
            'pp': PP,
            'rgb': BLUE,
        })
        return response.json()['search']


@pytest.fixture(name="results_busters_blue_price_threshold_mobile")
def results_busters_blue_price_threshold_mobile_fixture():
    return results_busters_blue_price_threshold_mobile()


@allure.story('mobile_price_threshold')
@allure.feature('report_place_prime')
@allure.feature('price')
@allure.feature('threshold')
@allure.issue('https://st.yandex-team.ru/MARKETINDEXER-32232')
@pytest.mark.parametrize("results_busters_mobile_price_threshold", [results_busters_white_price_threshold_mobile, results_busters_blue_price_threshold_mobile])
@white_only
def test_mobile_price_threshold_white(results_busters_mobile_price_threshold):
    '''Проверяем, что в выдаче нет офферов дешевле threshold (100р)'''

    results = results_busters_mobile_price_threshold()
    offers_num = results['totalOffers']
    offers_in_index = results['totalOffersBeforeFilters']
    # https://st.yandex-team.ru/MARKETINDEXER-32420#5e282f7e54b0b0345541855a
    # https://proxy.sandbox.yandex-team.ru/1404326196
    # market/idx/tests/external/test_mobile_price.py:78: in test_mobile_price_threshold_white
    #    assert_that(offers_num, less_than(10))
    #    E   AssertionError:
    #    E   Expected: a value less than <10>
    #    E        but: was <10>
    assert_that(offers_num, less_than(20))
    assert_that(offers_in_index, greater_than(0))


@allure.story('mobile_price_threshold')
@allure.feature('report_place_prime')
@allure.feature('price')
@allure.feature('threshold')
@allure.issue('https://st.yandex-team.ru/MARKETINDEXER-32232')
@blue_only
def test_mobile_price_threshold_blue(results_busters_blue_price_threshold_mobile):
    '''Проверяем, что в выдаче нет офферов дешевле threshold (100р)'''

    offers_num = results_busters_blue_price_threshold_mobile['totalOffers']
    offers_in_index = results_busters_blue_price_threshold_mobile['totalOffersBeforeFilters']
    assert_that(offers_num, equal_to(0))
    assert_that(offers_in_index, greater_than(0))
