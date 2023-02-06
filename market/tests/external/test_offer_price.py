# coding: utf-8

import allure
import pytest

from common import report_response, white_only, blue_only
from constants import (
    MAGIC_PP,
    MOSCOW_RID,
    ELECTRONICS_HID,
    BLUE,
)
from hamcrest import (
    assert_that,
    has_item,
    has_entry,
    greater_than
)


@pytest.fixture()
def results():
    with allure.step('Проверка ответа репорта'):
        response = report_response({
            'place': 'prime',
            'pp': MAGIC_PP,
            'rids': MOSCOW_RID,
            'hid': ELECTRONICS_HID,
            'allow-collapsing': 1,
            'use-default-offers': 1,
            'onstock': 1,
            'prun-count': 3000,
            'waitall': 1,
        })
        yield response.json()['search']['results']


@allure.story('price_exists')
@allure.feature('report_place_prime')
@allure.feature('price')
@allure.issue('https://st.yandex-team.ru/MARKETINDEXER-19904')
@white_only
def test_offer_price_is_greater_than_zero(results):
    '''Проверяем, что у нас есть оффер с не нулевой ценой'''
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
                            'seller',
                            has_entry(
                                'price', greater_than(0)
                            )
                        )
                    )
                )
            )
        ),
        u'В выдаче ожидается хотя бы один оффер у которого цена больше 0'
    )


@pytest.fixture()
def blue_results():
    with allure.step('Проверка ответа репорта'):
        response = report_response({
            'place': 'prime',
            'pp': MAGIC_PP,
            'rids': MOSCOW_RID,
            'hid': ELECTRONICS_HID,
            'allow-collapsing': 1,
            'use-default-offers': 1,
            'onstock': 1,
            'prun-count': 3000,
            'rgb': BLUE,
            'numdoc': 30,
            'rearr-factors': 'market_white_cpa_on_blue=0'
        })
        yield response.json()['search']['results']


@allure.story('blue_price_exists')
@allure.feature('report_place_prime')
@allure.feature('price')
@allure.issue('https://st.yandex-team.ru/MARKETINDEXER-19904')
@blue_only
def test_blue_offer_price_is_greater_than_zero(blue_results):
    '''Проверяем, что у нас есть оффер с не нулевой ценой'''
    assert_that(
        blue_results,
        has_item(
            has_entry('entity', 'product')
            and
            has_entry(
                'offers',
                has_entry(
                    'items',
                    has_item(
                        has_entry(
                            'seller',
                            has_entry(
                                'price', greater_than(0)
                            )
                        )
                    )
                )
            )
        ),
        u'В выдаче ожидается хотя бы один синий оффер у которого цена больше 0'
    )
