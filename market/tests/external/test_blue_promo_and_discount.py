# coding: utf-8

import allure
import pytest

from common import (
    report_response
)

from constants import (
    MAGIC_PP,
    MOSCOW_RID,
    ELECTRONICS_HID
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
            'rids': MOSCOW_RID,

            'filter-promo-or-discount': 1,
            'hid': ELECTRONICS_HID,
            'allow-collapsing': 1,
            'use-default-offers': 1,
            'onstock': 1,
            'rgb': 'blue',
            'numdoc': 30,
            'rearr-factors': 'market_white_cpa_on_blue=0'
        })
        yield response.json()['search']['results']


@allure.story('offer_exists')
@allure.feature('report_place_prime')
@allure.feature('promo')
@allure.feature('discount')
@allure.issue('https://st.yandex-team.ru/MARKETINDEXER-19904')
def test_blue_offer_with_promo_or_discount(results):
    '''Проверяем, что у нас есть хотя бы один синий оффер со скидкой и промо'''
    assert_that(
        results,
        has_item(
            has_entry('entity', 'product')
            and
            has_entry(
                'offers',
                has_entry(
                    'items',
                    has_item(
                        has_entry(
                            'entity', 'offer'
                        )
                    )
                )
            )
        ),
        u'В выдаче ожидается хотя бы один синий оффер с промо и скидкой'
    )
