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
    has_entry,
    matches_regexp
)


@pytest.fixture()
def results():
    with allure.step('Проверка ответа репорта'):
        response = report_response({
            'place': 'prime',
            'pp': MAGIC_PP,
            'text': 'iphone',
            'rids': MOSCOW_RID
        })
        yield response.json()['search']['results']


@allure.story('picture_exists')
@allure.feature('report_place_prime')
@allure.feature('model')
@allure.feature('picture')
@allure.issue('https://st.yandex-team.ru/MARKETINDEXER-19904')
def test_model_picture_exists_in_prime_place(results):
    '''Проверяем, что в выдаче есть хотя бы одна модель с картинкой'''
    assert_that(
        results,
        has_item(
            has_entry('entity', 'product')
            and
            has_entry('type', 'model')
            and
            has_entry(
                'pictures',
                has_item(
                    has_entry('entity', 'picture')
                    and
                    has_entry('original', has_entry('url', matches_regexp('.+')))
                )
            )
        ),
        u'В выдаче ожидается хотя бы одина модель с картинкой'
    )
