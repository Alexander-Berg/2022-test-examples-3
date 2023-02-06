# -*- coding: utf-8 -*-

import pytest
from hamcrest import assert_that, equal_to, all_of, not_

from market.idx.yatf.matchers.zookeeper import ZkNodeContains

from market.idx.pylibrary.mindexer_core.zkmaster.zkmaster import ZkMaster


@pytest.fixture(params=['testing', 'production'])
def mindexer_clt(mindexer_clt, request, reusable_mysql, reusable_zk):
    """Добавляем в уже созданные для mindexer_clt пару поколений
    белое full 20180101_0101
    синее 20180101_0100
    """
    # У нас код работает по разному для тестинга и для прода,
    # что бы в таком коде ловить ошибки, запускаем все тесты mindexer_clt в обоих вариантах
    mindexer_clt.env_type = request.param
    mindexer_clt.add_generation_to_super('20180101_0101')
    return mindexer_clt


def test_make_me_master_old_style(mindexer_clt, reusable_zk):
    """Тест проверяет, что make_me_master без параметров
    1. вызов когда zookeeper пустой
    2. вызов с другого мастера
    """
    contains_mitype_stratocaster = ZkNodeContains(reusable_zk, 'stratocaster')
    contains_mitype_gibson = ZkNodeContains(reusable_zk, 'gibson')

    res = mindexer_clt.execute('make_me_master')
    assert_that(res.exit_code, equal_to(0))
    assert_that('/mimaster/currentmitype', contains_mitype_stratocaster)

    # Эмулируем другой мастер сменив mitype
    mindexer_clt.mitype = 'gibson'
    res = mindexer_clt.execute('make_me_master')
    assert_that(res.exit_code, equal_to(0))
    assert_that('/mimaster/currentmitype', contains_mitype_gibson)


def test_make_me_master(mindexer_clt, reusable_zk):
    """Тест проверяет, что make_me_master корректно работает со всеми аргументами.
    both - переключет белый и синий мастера
    main - переключет только белый
    """
    contains_mitype_gibson = ZkNodeContains(reusable_zk, 'gibson')

    # инициализируем значения в zookeeper
    res = mindexer_clt.execute('make_me_master', '--both')
    assert_that(res.exit_code, equal_to(0))

    # меняем mitype и пробуем переключить мастера
    mindexer_clt.mitype = 'gibson'
    res = mindexer_clt.execute('make_me_master', '--main')
    assert_that(res.exit_code, equal_to(0))

    # валидируем, что поменялись нужные переменные в zk
    assert_that('/mimaster/currentmitype', contains_mitype_gibson)


def test_am_i_master(mindexer_clt):
    """Тест проверяет, что zkmaster правильно определяет, кто в данный момент является мастером
    Кроме того валидируем, что раздельное переключение работает правильно.
    """
    def am_i_master():
        with ZkMaster(mindexer_clt.config) as zk_master:
            return zk_master.am_i_master()

    # инициализируем значения в zookeeper
    # меняем mitype и перегенерим конфиг
    mindexer_clt.mitype = 'gibson'
    mindexer_clt.init()

    # убеждаемся, что мы перестали быть мастером
    assert_that(all_of(
        not_(am_i_master()),
    ))
