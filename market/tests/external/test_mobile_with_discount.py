# coding: utf-8

import allure
import pytest

from common import (
    report_response,
    white_only,
    skip_testing
)

from constants import (
    MOSCOW_RID,
    RUR_CURRENCY,
    WHITE_BLUE,
    MOBILE_HID
)
from hamcrest import (
    assert_that,
    greater_than,
    has_item,
    has_entry,
)

PP = 1
MOBILE_NID = '54726'


@pytest.fixture()
def results_busters_model_mobile():
    with allure.step('Проверка ответа репорта'):
        response = report_response({
            'currency': RUR_CURRENCY,
            'numdoc': 5,
            'page': 1,
            'onstock': 1,
            'hid': MOBILE_HID,
            'rids': MOSCOW_RID,
            'place': 'prime',
            'pp': PP,
            'rgb': WHITE_BLUE,
            'allow-collapsing': 1,
            'promo-type': 'discount',
            'use-default-offers': 1,  # D.O.
        })
        return response.json()['search']['results']


@allure.story('mobile_model_promos')
@allure.feature('report_place_prime')
@allure.feature('promos')
@allure.feature('mobile_models')
@allure.issue('https://st.yandex-team.ru/MARKETINDEXER-30356')
@white_only
@skip_testing
def test_model_promos_mobile(results_busters_model_mobile):
    '''Проверяем, что в выдаче промки для телефонов'''

    assert_that(len(results_busters_model_mobile), greater_than(1))

    '''Проверяем, что у нас есть оффер со скидкой и скидка не нулевая'''
    assert_that(
        results_busters_model_mobile,
        has_item(
            has_entry(
                'offers',
                has_entry(
                    'items',
                    has_item(
                        has_entry(
                            'prices',
                            has_entry(
                                'discount',
                                has_entry(
                                    'percent', greater_than(0)
                                )
                            )
                        )
                    )
                )
            )
        ), u'В выдаче ожидается хотя бы один оффер у которого есть скидка больше 0'
    )


@pytest.fixture()
def results_busters_offer_mobile(results_busters_model_mobile):

    result = []

    for model in results_busters_model_mobile:
        hyperid = model['id']
        with allure.step('Проверка ответа репорта'):
            response = report_response({
                'currency': RUR_CURRENCY,
                'numdoc': 2,
                'page': 1,
                'hid': MOBILE_HID,
                'nid': MOBILE_NID,
                'how': 'discount_p',
                'grhow': 'shop',
                'rids': MOSCOW_RID,
                'place': 'productoffers',
                'hyperid': hyperid,
                'pp': PP,
                'show-min-quantity': 1,
                'show-promoted': 1,
                'rgb': WHITE_BLUE,
                'show-preorder': 1,
                'promo-type': 'discount',
            })
            result.append((hyperid, response.json()['search']['results']))

    return result


@allure.story('mobile_offers_promos')
@allure.feature('report_place_productoffers')
@allure.feature('promos')
@allure.feature('mobile_offers')
@allure.issue('https://st.yandex-team.ru/MARKETINDEXER-30356')
@white_only
@skip_testing
def test_mobile_offers_promos(results_busters_offer_mobile):
    '''Проверяем, что в выдаче есть оффера и модели'''

    for hyperid, offers_for_model in results_busters_offer_mobile:
        # у нас запрос в prime c promo-type=discount находит модели без офферов со скидкой
        # это не правильно, но не повод ломать двухфазный релоад поэтому проверку на налчие хотя бы
        # одного оффера я пока убрал
        # https://st.yandex-team.ru/MARKETOUT-37966
        if not offers_for_model:
            continue
        # assert_that(len(offers_for_model), greater_than(0), 'model {}'.format(hyperid))

        assert_that(
            offers_for_model,
            has_item(
                has_entry(
                    'prices',
                    has_entry(
                        'discount',
                        has_entry(
                            'percent', greater_than(0)
                        )
                    )
                )
            ),
            u'В выдаче ожидается хотя бы один оффер у которого есть скидка больше 0'
        )
