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
            'hid': 90999,  # see https://a.yandex-team.ru/arc/trunk/arcadia/market/report/data/category_redirects.json?rev=r7843734#L590
            'text': 'библия',
            'allow-collapsing': 1,
            'rids': MOSCOW_RID
        })
        yield response.json()['redirect']


@allure.story('redirects')
@allure.feature('report_place_prime')
@allure.feature('redirect')
@allure.issue('https://st.yandex-team.ru/MARKETINDEXER-37763')
def test_redirect_params_in_report_response(results):
    '''Проверяем, что в выдаче есть редирект'''
    assert_that(
        results,
            has_entry(
                'params',
                has_entry('hid', has_item('18540670'))
                and
                has_entry('glfilter', has_item('18664890:18665016'))
                and
                has_entry('permanent', has_item('1'))
                and
                has_entry('was_redir', has_item('1'))
            )
            and
            has_entry('target', 'search'),
        u'В выдаче ожидается редирект'
    )
