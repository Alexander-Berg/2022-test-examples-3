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
    ELECTRONICS_HID
)
from hamcrest import (
    assert_that,
    empty,
    is_not,
    has_item,
    equal_to
)


@pytest.fixture()
def results():
    with allure.step('Проверка ответа репорта'):
        response = report_response({
            'place': 'prime',
            'pp': MAGIC_PP,
            'rids': MOSCOW_RID,

            'filter-promo-or-discount': 1,
            'hid': ELECTRONICS_HID,

            'use-default-offers': 1,
            'onstock': 1,
            'entities': 'offer',
        })

        entries = response.json()['search']['results']
        assert_that(entries, is_not(empty()), u'В выдаче есть результаты')
        yield entries


@pytest.fixture()
def results_entries(results):
    good_entries = [entry for entry in results if 'entity' in entry]
    assert_that(good_entries, is_not(empty()), u'В выдаче есть результаты с полем entity')

    yield good_entries


@allure.story('offer_exists')
@allure.feature('report_place_prime')
@allure.feature('promo')
@allure.feature('discount')
@allure.issue('https://st.yandex-team.ru/MARKETINDEXER-19904')
@allure.label('Checks that we have at least one offer with promo or discount')
@white_only
def test_offer_in_result_exists_in_prime_place(results_entries):
    '''Проверяем, что в выдаче есть хотя бы один оффер с промо и скидкой в категории электроника'''

    entities = [entry['entity'] for entry in results_entries]

    assert_that(
        entities,
        has_item(equal_to(u'offer')),
        u'В выдаче ожидается хотя бы один оффер'
    )
