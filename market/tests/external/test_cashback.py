# coding: utf-8

import allure
import pytest

from common import (
    report_response,
    skip_testing
)
from constants import (
    MAGIC_PP,
    MOSCOW_RID,
    ELECTRONICS_HID,
    GARDEN_HID,
    CHILDRENS_GOODS_HID
)
from hamcrest import (
    assert_that,
    empty,
    is_not,
    has_item,
    equal_to
)


# Проверяем большие категории по всем группам ассортимета: CEHAC, DIY и дефолтную
@pytest.fixture(
    params=[ELECTRONICS_HID, GARDEN_HID, CHILDRENS_GOODS_HID]
)
def hid_data(request):
    return request.param


@pytest.fixture()
def results(hid_data):
    with allure.step('Проверка ответа репорта'):
        response = report_response({
            'place': 'prime',
            'pp': MAGIC_PP,
            'rids': MOSCOW_RID,
            'hid': hid_data,
            'promo-type': 'blue-cashback',
            'use-default-offers': 1,
            'onstock': 1,
            'entities': 'offer',
            'perks': 'yandex_plus,yandex_cashback',
            'prun-count': 1000,
        })
        entries = response.json()['search']['results']
        yield entries


@pytest.fixture()
def results_entries(results):
    good_entries = [entry for entry in results if 'entity' in entry]
    assert_that(good_entries, is_not(empty()), u'В выдаче есть результаты с полем entity')

    yield good_entries


@allure.story('offer_exists')
@allure.feature('report_place_prime')
@skip_testing
@allure.issue('https://st.yandex-team.ru/MARKETOUT-35465')
def test_offer_in_result_exists_in_prime_place(results_entries):
    '''Проверяем, что в выдаче есть хотя бы один оффер с кешбеком в разных категориях'''

    entities = [entry['entity'] for entry in results_entries]

    assert_that(
        entities,
        has_item(equal_to(u'offer')),
        u'В выдаче ожидается хотя бы один оффер'
    )
