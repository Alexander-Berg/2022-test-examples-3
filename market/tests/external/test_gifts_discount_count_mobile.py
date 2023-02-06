# coding: utf-8

import allure
import pytest

from common import (
    report_response,
    white_only,
    skip_testing
)

from constants import (
    RUR_CURRENCY,
    WHITE_BLUE,
    MOBILE_HID
)
from hamcrest import (
    assert_that,
    greater_than,
)

PP = 1
'''пока проект cpa_only распространяется на телефоны, большее число скидок/подарков не гарантируется'''
MAGIC_NUMBER = 1


def results_busters_discount_mobile():
    with allure.step('Проверка ответа репорта'):
        response = report_response({
            'currency': RUR_CURRENCY,
            'onstock': 1,
            'hid': MOBILE_HID,
            'place': 'prime',
            'pp': PP,
            'rgb': WHITE_BLUE,
            'showdiscounts': 1,
            'promo-type': 'discount',
            'use-default-offers': 1,  # D.O.
        })
        return response.json()


@pytest.fixture()
def results_busters_discount_mobile_fixture(name="results_busters_discount_mobile"):
    return results_busters_discount_mobile()


@pytest.fixture()
def results_busters_gifts_mobile():
    with allure.step('Проверка ответа репорта'):
        response = report_response({
            'currency': RUR_CURRENCY,
            'onstock': 1,
            'hid': MOBILE_HID,
            'place': 'prime',
            'pp': PP,
            'rgb': WHITE_BLUE,
            'showdiscounts': 1,
            'promo-type': 'gift-with-purchase',
            'use-default-offers': 1,  # D.O.
        })
        return response.json()


@allure.story('mobile_model_promos')
@allure.feature('report_place_prime')
@allure.feature('promos')
@allure.feature('mobile_models')
@allure.issue('https://st.yandex-team.ru/MARKETINDEXER-30356')
@pytest.mark.parametrize("results_busters_mobile", [
    results_busters_discount_mobile,
    # results_busters_gifts_mobile,  # MARKETINDEXER-41030
])
@white_only
@pytest.mark.skip(reason="MARKETINCIDENTS-12976")
@skip_testing
def test_promos_gifs_mobile(results_busters_mobile):
    '''Проверяем, что в выдаче промки для телефонов'''

    results = results_busters_mobile()
    promos_count = results['search']['totalOffers']
    assert_that(promos_count, greater_than(MAGIC_NUMBER))
