# coding: utf-8

import allure
import pytest

from common import (
    report_response,
)
from constants import (
    MAGIC_PP,
    MOSCOW_RID
)
from hamcrest import (
    assert_that,
    greater_than_or_equal_to
)


@pytest.fixture()
def debug():
    with allure.step('Проверка ответа репорта'):
        response = report_response({
            'place': 'prime',
            'pp': MAGIC_PP,
            'text': 'кружка',
            'rids': MOSCOW_RID
        })
        yield response.json()['debug']


@allure.story('qbis_generation')
@allure.feature('debug')
@allure.feature('qbis')
@allure.issue('https://st.yandex-team.ru/MARKETINDEXER-19904')
@pytest.mark.skip(reason="MARKETINDEXER-32006")
def test_check_full_and_qbids_generations(debug):
    '''Проверка номера поколения qbids. Ожидаем увидеть, что поколение qbids будет
    выше, чем номер основного поколения'''
    full_generation = debug['brief']['generation']
    qbis_generation = max(debug['brief']['qbidsGeneration'].split(","))

    assert_that(
        qbis_generation,
        greater_than_or_equal_to(full_generation),
        u'Поколение qbids должно быть выше, чем основное поколение'
    )
