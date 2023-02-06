# coding: utf-8

import allure
import pytest

from common import report_response, skip_testing
from constants import (
    MAGIC_PP,
    MOSCOW_RID
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
            'text': 'нурофен',
            'rids': MOSCOW_RID,
            'numdoc': 50,
        })
        yield response.json()['search']['results']


@allure.story('model_exists')
@allure.feature('report_place_prime')
@allure.issue('https://st.yandex-team.ru/MARKETINDEXER-45114')
def test_model_exists_in_prime_place(results):
    '''Проверяем, что в выдаче есть хотя бы одна модель нурофена'''
    assert_that(
        results,
        has_item(has_entry('entity', 'product')),
        u'В выдаче ожидается хотя бы одна модель'
    )


@allure.story('offer_exists')
@allure.feature('report_place_prime')
@allure.issue('https://st.yandex-team.ru/MARKETINDEXER-45114')
@skip_testing
def test_offer_exists_in_prime_place(results):
    '''Проверяем, что в выдаче есть хотя бы одна модель нурофена'''
    assert_that(
        results,
        has_item(has_entry('entity', 'offer')),
        u'В выдаче ожидается хотя бы один оффер нурофена'
    )
