# coding: utf-8

import allure
import pytest

from common import report_response, skip_testing
from constants import (
    MAGIC_PP,
    MOSCOW_RID,
    BOOKS_HID
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
            'hid': BOOKS_HID,
            'text': 'библия',
            'allow-collapsing': 1,
            'rids': MOSCOW_RID
        })
        yield response.json()['search']['results']


@allure.story('picture_exists')
@allure.feature('report_place_prime')
@allure.feature('book')
@allure.feature('picture')
@allure.issue('https://st.yandex-team.ru/MARKETINDEXER-19904')
@skip_testing
def test_book_picture_exists_in_prime_place(results):
    """Проверяем, что в выдаче есть хотя бы одна книга с картинкой"""

    assert_that(
        results,
        has_item(
            has_entry('entity', 'product')
            and
            has_entry('type', 'book')
        ),
        u'По запросу библии ожидается хотя бы одна книга'
    )

    assert_that(
        results,
        has_item(
            has_entry('entity', 'product')
            and
            has_entry('type', 'book')
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
        u'В выдаче ожидается хотя бы одина книга с картинкой'
    )
