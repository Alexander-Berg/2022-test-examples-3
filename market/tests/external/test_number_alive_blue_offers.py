# coding: utf-8

import allure
import pytest
import yatest

from common import report_response, white_only, skip_testing
from constants import (
    MOSCOW_RID,
    BLUE
)
from hamcrest import (
    assert_that,
    greater_than,
)

PP = 18
MIN_BLUE_THRESHOLD = 265000 if yatest.common.get_param('env') != "testing" else 7700


@pytest.fixture()
def results_busters_blue_alive_offers(request):
    with allure.step('Проверка ответа репорта'):
        response = report_response({
            'place': 'stat_numbers',
            'pp': PP,
            'rids': MOSCOW_RID,
            'rgb': BLUE,
        })
        return response.json()


@allure.story('number_of_alive_blue_offers')
@allure.feature('report_place_stat_numbers')
@allure.feature('number')
@allure.feature('alive_offers')
@allure.issue('https://st.yandex-team.ru/MARKETINDEXER-32233')
@white_only
@skip_testing
def test_number_of_alive_blue_offers(results_busters_blue_alive_offers):
    '''Проверяем, что в выдаче есть синие оффера в нужном количестве'''

    total_offers = results_busters_blue_alive_offers['result']['offersCount']
    assert_that(total_offers, greater_than(MIN_BLUE_THRESHOLD))
