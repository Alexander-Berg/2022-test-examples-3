# coding: utf-8

import allure
import pytest
import yatest

from collections import namedtuple

from common import (
    report_response,
    blue_only
)

from constants import (
    MOSCOW_RID,
)

from hamcrest import (
    assert_that,
    greater_than_or_equal_to,
)

PP = 1

blue_hids_named_tuple = namedtuple(
    'blue_hids_named_tuple',
    ('hid', 'unique_name', 'testing_offers_count', 'stable_offers_count')
)

# Количество офферов на выдаче в тестинге и стейбле было получено
# запросами в place=stat_numbers 9 сентября 2020 года и используется как некоторое базовое
# значение для количества офферов в категории (https://st.yandex-team.ru/MARKETINDEXER-35351)
POPULAR_BLUE_HIDS_DATA = [
    (90533, 'Контактные линзы', 8768, 16607),
    (90566, 'Стиральные машины', 1, 501),
    (91013, 'Ноутбуки', 5, 1340),
    (91491, 'Мобильные телефоны', 31, 1192),
    (91498, 'Чехлы для мобильных телефонов', 0, 24408),
    (1003092, 'Матрасы', 0, 46979),
    (15450081, 'Холодильники', 2, 449),
    # 28.04.2022
    (7877999, 'Одежда, обувь и аксессуары', 10, 100000),
    (8475840, 'Аптека', 10, 10000),
]

POPULAR_BLUE_HIDS = [blue_hids_named_tuple(*elem) for elem in POPULAR_BLUE_HIDS_DATA]

# Минимальное кол-во офферов на выдаче, с которым сравнивается значение поля offersCount из ответa place=stat_numbers
MIN_OFFERS_COUNT_STABLE = 100


def is_testing():
    return yatest.common.get_param('env') == 'testing'


@pytest.fixture(
    params=[(x.hid, x.testing_offers_count, x.stable_offers_count) for x in POPULAR_BLUE_HIDS],
    ids=[x.unique_name for x in POPULAR_BLUE_HIDS])
def hid_data(request):
    return request.param


@pytest.fixture()
def popular_blue_categories_offers_count(hid_data):
    with allure.step('Проверка ответа репорта'):
        response = report_response({
            'place': 'stat_numbers',
            'pp': PP,
            'rids': MOSCOW_RID,
            'rgb': 'blue',
            'hid': hid_data[0]
        })
        return response.json()


@allure.story('offers_count_for_popular_blue_categories')
@allure.feature('report_place_stat_numbers')
@allure.feature('popular')
@allure.feature('category')
@allure.issue('https://st.yandex-team.ru/MARKETINDEXER-35351')
@blue_only
def test_blue_popular_category_offers(hid_data, popular_blue_categories_offers_count):
    '''
    Проверяем, что на выдаче в Москве есть офферы из популярных
    категорий на синем маркете
    '''

    offers_count_in_category_now = popular_blue_categories_offers_count['result']['offersCount']
    if is_testing():
        expected_min_offers_count_testing = 1 if hid_data[1] > 0 else 0
        assert_that(offers_count_in_category_now, greater_than_or_equal_to(expected_min_offers_count_testing))
    else:
        expected_min_offers_count_stable = MIN_OFFERS_COUNT_STABLE
        assert_that(offers_count_in_category_now, greater_than_or_equal_to(expected_min_offers_count_stable))
