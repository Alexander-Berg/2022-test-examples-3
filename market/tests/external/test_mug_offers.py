# coding: utf-8

import allure
import pytest

from common import (
    report_response,
    white_only
)
from constants import (
    MAGIC_PP,
    MOSCOW_RID,
    INTERNAL_REPORT_TIMEOUT,
    WAIT_ALL_YES
)
from hamcrest import (
    assert_that,
    has_item,
    has_entry
)


@pytest.fixture()
def results():
    with allure.step('Проверка ответа репорта'):
        response = report_response({
            'place': 'prime',
            'pp': MAGIC_PP,
            'text': 'iphone',
            'rids': MOSCOW_RID,
            'waitall': WAIT_ALL_YES,
            'timeout': INTERNAL_REPORT_TIMEOUT,
            'entities': 'offer'
        })
        yield response.json()['search']['results']


@allure.story('offer_exists')
@allure.feature('report_place_prime')
@allure.feature('offer')
@allure.issue('https://st.yandex-team.ru/MARKETINDEXER-19904')
@white_only
def test_offer_exists_in_prime_place(results):
    '''Проверяем, что в выдаче есть хотя бы один оффер по запросу iphone'''
    assert_that(
        results,
        has_item(has_entry('entity', 'offer')),
        u'В выдаче ожидается хотя бы один оффер'
    )
