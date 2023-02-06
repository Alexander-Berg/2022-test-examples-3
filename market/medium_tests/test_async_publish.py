# -*- coding: utf-8 -*-
import os
import time

import pytest
from hamcrest import (
    assert_that,
    equal_to,
    not_,
    all_of,
    greater_than_or_equal_to,
    starts_with,
)

from async_publishing import (
    GenerationMeta,
    DistGenerationMeta,
)
from market.idx.yatf.matchers.file_matchers import FileExists, FileEmpty, FileProperty
from market.idx.yatf.matchers.zookeeper import ZkNodeExists


@pytest.fixture()
def mindexer_clt(mindexer_clt, reusable_mysql, reusable_zk):
    """Добавляем в уже созданные для mindexer_clt пару поколений
    белое full 20180101_0101
    синее 20180101_0100
    """
    mindexer_clt.add_generation_to_super('20180101_0101')

    res = mindexer_clt.execute('make_me_master', '--both', '--no-publish')
    assert_that(res.exit_code, equal_to(0))

    mindexer_clt.make_local_config({
        ('publish.async', 'async_publish_dists_separately'): 'true'
    })
    res = mindexer_clt.execute('reconfigure_publisher')
    assert_that(res.exit_code, equal_to(0))

    # для того что бы тесты публикации имели смысл, изначально мы не должны иметь опубликованных поколений
    assert_that(all_of(
        '/publisher/generations/full_generation', not_(ZkNodeExists(reusable_zk)),
        '/publisher/generations/dists', not_(ZkNodeExists(reusable_zk)),
        '/publisher/generations/by_name', not_(ZkNodeExists(reusable_zk)),
    ))
    return mindexer_clt


@pytest.fixture()
def mindexer_clt_reserve(mindexer_clt):
    mindexer_clt.mitype = 'gibson'
    mindexer_clt.init()
    return mindexer_clt


@pytest.mark.parametrize('additional_args', [
    [],
    ['20180101_0101'],
    ['--force'],
    ['20180101_0101', '--force'],
])
def test_async_publish_full_from_master(mindexer_clt, reusable_zk, additional_args):
    """Тест проверяет, что async_publish_full корректно работает,
    когда вызывается с мастера для белого полного поколения.
    Валидируем все наборы флажков, для мастера все запуски равнозначны.
    """
    res = mindexer_clt.execute('async_publish_full', *additional_args)
    assert_that(res.exit_code, equal_to(0))

    assert_that(all_of(
        GenerationMeta.from_str(reusable_zk.get('/publisher/generations/full_generation')[0]).name, equal_to('20180101_0101'),
        reusable_zk.get('/publisher/generations/full_generation')[0], equal_to(reusable_zk.get('/publisher/generations/by_name/20180101_0101/full_generation')[0]),
        DistGenerationMeta.from_str(reusable_zk.get('/publisher/generations/dists/search-part-base-1')[0]).generation, equal_to('20180101_0101'),
        reusable_zk.get('/publisher/generations/dists/search-part-base-1')[0], equal_to(reusable_zk.get('/publisher/generations/by_name/20180101_0101/dists/search-part-base-1')[0]),
        DistGenerationMeta.from_str(reusable_zk.get('/publisher/generations/dists/search-snippet-1')[0]).generation, equal_to('20180101_0101'),
        reusable_zk.get('/publisher/generations/dists/search-snippet-1')[0], equal_to(reusable_zk.get('/publisher/generations/by_name/20180101_0101/dists/search-snippet-1')[0]),
        DistGenerationMeta.from_str(reusable_zk.get('/publisher/generations/dists/search-report-data')[0]).generation, equal_to('20180101_0101'),
        reusable_zk.get('/publisher/generations/dists/search-report-data')[0], equal_to(reusable_zk.get('/publisher/generations/by_name/20180101_0101/dists/search-report-data')[0]),
    ))


def test_async_publish_white_from_reserve(mindexer_clt_reserve, reusable_zk):
    """Тест проверяет, что async_publish_full корректно работает,
    когда вызывается с резервного мастера для белого полного поколения.

    - поколение не проставлется
    - мониторинг раскладки гасится
    """
    test_start_ts = time.time()
    res = mindexer_clt_reserve.execute('async_publish_full')
    assert_that(res.exit_code, equal_to(0))

    publisher_status_path = os.path.join(mindexer_clt_reserve.config.run_dir, 'publisher.status')
    publisher_state_path = os.path.join(mindexer_clt_reserve.config.run_dir, 'publisher.success')
    assert_that('/publisher/generations/full_generation', not_(ZkNodeExists(reusable_zk))),

    assert_that(publisher_status_path, FileEmpty())
    assert_that(publisher_state_path, all_of(
        FileExists(),
        FileProperty(os.path.getmtime, greater_than_or_equal_to(test_start_ts))
    ))

    # мониторинг не горит
    res = mindexer_clt_reserve.execute('check_publisher')
    assert_that(res.exit_code, equal_to(0))
    assert_that(res.std_out, starts_with('0;'))


def test_async_publish_search_part_base_from_master(mindexer_clt, reusable_zk):
    """Тест проверяет, что async_publish_search_part_base корректно работает,
    когда вызывается с мастера для белого полного поколения.
    """
    res = mindexer_clt.execute('async_publish_search_part_base')
    assert_that(res.exit_code, equal_to(0))

    assert_that(all_of(
        DistGenerationMeta.from_str(reusable_zk.get('/publisher/generations/dists/search-part-base-1')[0]).generation, equal_to('20180101_0101'),
        reusable_zk.get('/publisher/generations/dists/search-part-base-1')[0], equal_to(reusable_zk.get('/publisher/generations/by_name/20180101_0101/dists/search-part-base-1')[0]),
    ))
