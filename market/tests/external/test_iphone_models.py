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
            'entities': 'product',
        })
        yield response.json()['search']['results']


@allure.story('model_exists')
@allure.feature('report_place_prime')
@allure.feature('model')
@allure.issue('https://st.yandex-team.ru/MARKETINDEXER-19904')
def test_model_exists_in_prime_place(results):
    '''Проверяем, что в выдаче есть хотя бы одна модель'''
    assert_that(
        results,
        has_item(has_entry('entity', 'product')),
        u'В выдаче ожидается хотя бы одна модель'
    )
