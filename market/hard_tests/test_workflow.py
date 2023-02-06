#!/usr/bin/env python
# -*- coding: utf-8 -*-
from collections import namedtuple

import pytest
import mock
import itertools
import os
from namedlist import namedlist

from hamcrest import (
    assert_that,
    equal_to,
)

import yatest.common
from yt.wrapper.ypath import ypath_join

from async_publishing.generation_meta import GenerationMeta
from market.idx.datacamp.yatf.resources.tokens import YtTokenStub
from market.idx.marketindexer.marketindexer import workflow
from market.idx.marketindexer.marketindexer.workflow import is_full
from market.idx.pylibrary.mindexer_core.mic.mic import MI_Cleaner
from market.idx.yatf.resources.yt_stuff_resource import yt_server   # noqa
import market.pylibrary.yenv as yenv


PREV_GENERATION = '20170909_0808'
GENERATION = '20170909_0909'


MI_CleanerMock = mock.create_autospec(MI_Cleaner)


@pytest.fixture()
def logs(tmpdir):
    log_dir = tmpdir / 'log'
    undefined_log = log_dir / '{}_{}.log'.format(
        workflow.LOG_NAME, workflow.UNDEFINED)
    undefined_log.ensure(file=True)
    return log_dir


@pytest.fixture(params=itertools.product(
    ['full', 'blue'],  # mode
    (True, False),  # blue_publish
    (True, False),  # mbo_preview_enabled
    (True, False)  # ignore_not_for_publish
))
def config(request, logs):
    mode, blue_publish, mbo_preview_enabled, ignore_not_for_publish = request.param
    stats_calc_log_dir = os.path.join(str(logs), 'stats_calc')
    os.makedirs(stats_calc_log_dir)
    MiConfig = namedlist('MiConfig', [
        ('blue_mode', False),
        ('clt_name', 'mindexer'),
        ('mitype', 'stratocaster'),
        ('run_dir', ''),
        ('lock_dir', ''),
        ('log_dir', str(logs)),
        ('stats_calc_log_dir', stats_calc_log_dir),
        ('keep_failed', 3),
        ('keep_complete', 3),
        ('keep_complete_daily', 1),
        ('keep_complete_weekly', 3),
        ('keep_half_mode', 0),
        ('clean_generations', 3),
        ('max_time_to_wait_publisher', 10),
        ('mindexer_bin', 'mindexer.bin'),
        ('async_publish_white', True),
        ('async_publish_blue', blue_publish),
        ('async_publish_blue_from_white', not blue_publish),
        ('mbo_preview_enabled', mbo_preview_enabled),
        ('envtype', yenv.PRODUCTION),
        ('is_testing', False),
        ('need_calc_dist_statistic', False),
        ('async_copybases', False),
        ('indexation_feedlog_enabled', False),
        ('dists_dir', ''),
        ('enable_clean_dists', False),
        ('calc_generation_freshness', False),
        ('save_meta_dists_to_sandbox', False),
        ('ignore_not_for_publish', ignore_not_for_publish),
        ('fresh_collection_dynamic_ttl_enabled', False),
    ])
    return MiConfig()


@pytest.yield_fixture()
def plain_config(logs, yt_client):
    yt_proxy = yt_client.config['proxy']['url'].split('.')[0]
    yt_token_path = os.path.join(yatest.common.work_path(), 'token')
    yt_token = YtTokenStub(yt_token_path)  # noqa

    stats_calc_log_dir = str(logs / 'stats_calc')
    os.makedirs(stats_calc_log_dir)
    MiConfig = namedlist('MiConfig', [
        ('yt_proxy', yt_proxy),
        ('yt_tokenpath', yt_token_path),
        ('run_dir', ''),
        ('lock_dir', ''),
        ('log_dir', str(logs)),
        ('stats_calc_log_dir', stats_calc_log_dir),
        ('mitype', 'stratocaster'),
        ('yt_mi3_type', 'main'),
        ('envtype', yenv.PRODUCTION),
        ('is_testing', False),
        ('keep_failed', 3),
        ('keep_complete', 3),
        ('keep_complete_daily', 1),
        ('keep_complete_weekly', 3),
        ('keep_half_mode', 0),
        ('clean_generations', 3),
        ('yt_mi3_dir', '//home/mi3'),
        ('yt_mi3_generations_keep_count', 2),
        ('enable_clean_dists', False),
        ('yt_clean_mi3_on_fail', True),
    ])
    return MiConfig()


@pytest.yield_fixture()
def call_mock():
    with mock.patch('market.idx.marketindexer.marketindexer.workflow.util.system_ex_detach') as call_detach,\
            mock.patch('market.idx.marketindexer.marketindexer.workflow.util.watching_system_ex'):
        yield call_detach


@pytest.yield_fixture(params=[
    True,
    False,
])
def stubs(request):
    disable_copybases = request.param

    def dummy_switch(name):
        if name == 'disable_copybases':
            return disable_copybases
        else:
            assert False, 'unknown switch: ' + name
    with mock.patch('market.idx.marketindexer.marketindexer.workflow.geninfo.need_upload_full', return_value='test reason'), \
            mock.patch('market.idx.marketindexer.marketindexer.workflow.geninfo.need_publish_full', return_value=''), \
            mock.patch('market.idx.marketindexer.marketindexer.workflow.wait_for_mindexer_clt_command'), \
            mock.patch('market.idx.marketindexer.marketindexer.workflow.file_switch.is_switch_on', dummy_switch), \
            mock.patch('market.idx.marketindexer.marketindexer.workflow.async_publisher.publish_full') as publish_full, \
            mock.patch('market.idx.marketindexer.marketindexer.workflow.async_publisher.get_publishing_generation', return_value=GenerationMeta(PREV_GENERATION)), \
            mock.patch('market.idx.pylibrary.mindexer_core.geninfo.geninfo.get_last_complete_generation_name', return_value=GENERATION), \
            mock.patch('market.pylibrary.mindexerlib.sql.get_generation_half_mode', return_value=False), \
            mock.patch('market.pylibrary.mindexerlib.sql.get_generation_not_for_publish', return_value=False), \
            mock.patch('market.idx.pylibrary.mindexer_core.publishers.publisher.reconfigure'), \
            mock.patch('market.idx.pylibrary.mindexer_core.zkmaster.zkmaster.am_i_master', return_value=True), \
            mock.patch('market.idx.pylibrary.mindexer_core.market_collections.market_collections.mark_last_complete_dist'), \
            mock.patch('market.idx.pylibrary.mindexer_core.market_collections.market_collections.crop_dists_from_workers'):
        Mocks = namedtuple('Mocks', ['publish_full', 'disable_copybases'])
        yield Mocks(publish_full, disable_copybases)


@pytest.fixture()
def before_generation(config, call_mock, stubs):
    workflow.before_generation(config, cleaner_cls=MI_CleanerMock)


@pytest.fixture()
def after_generation(config, call_mock, stubs):
    workflow.after_generation(GENERATION, success=True, config=config, cleaner_cls=MI_CleanerMock)


@pytest.fixture()
def after_failed_generation(config, call_mock, stubs):
    workflow.after_generation(GENERATION, success=False, config=config, cleaner_cls=MI_CleanerMock)


@pytest.fixture()
def after_not_publish_generation(config, call_mock, not_for_publish_stubs):
    workflow.after_generation(GENERATION, success=True, config=config, cleaner_cls=MI_CleanerMock)


@pytest.yield_fixture()
def yt_client(yt_server):  # noqa
    yield yt_server.get_yt_client()


@pytest.yield_fixture()
def yt_mi3_dir(yt_client, plain_config):
    mi3_dir = ypath_join(plain_config.yt_mi3_dir, 'main')
    yt_client.mkdir(mi3_dir, recursive=True)
    yield mi3_dir
    yt_client.remove(mi3_dir, recursive=True)


def test_publish_full_after_full_generation(after_generation, config, call_mock, stubs):
    """
    Проверяем, что после сборки полного поколения зовется publish_generation(синхронная раскладка full only)
    Если:
    * раскладка не выключено свитчом
    """
    call_publish_full_geneneration = mock.call([
        'mindexer.bin',
        '--log-file=copybases_{}.log'.format(GENERATION),
        'publish_generation',
        GENERATION
    ])
    expected = is_full(config) and not stubs.disable_copybases
    actual = call_publish_full_geneneration in call_mock.mock_calls
    assert_that(actual, equal_to(expected))


def test_async_publish_full_called_after_full_generation(after_generation, config, stubs):
    """
    Проверяем, что поколение из демона публикуется, если это не выключено свитчтом.
    Так же если публикация из синиего демона не включена в конфиге,
    то мы должны опубликовать поколение из белого(синие шарды собранные в белом демоне)
    """
    if is_full(config) and not stubs.disable_copybases:
        publish_full_white = mock.call(GENERATION, config)

        assert_that(publish_full_white in stubs.publish_full.mock_calls, equal_to(config.async_publish_white))


def test_publish_prev_generation_after_failed_generation(after_failed_generation, config, stubs):
    """
    Проверяем, что после упавшего полного поколения публикуется предыдущее поколение
    """
    if is_full(config) and not stubs.disable_copybases:
        publish_prev_white = mock.call(PREV_GENERATION, config)
        assert_that(publish_prev_white in stubs.publish_full.mock_calls, equal_to(config.async_publish_white))


@pytest.fixture(params=(True, False))
def not_for_publish_stubs(request, config, stubs):
    is_not_for_publish = request.param
    with mock.patch('market.pylibrary.mindexerlib.sql.get_generation_not_for_publish', return_value=is_not_for_publish):
        Mocks = namedtuple('Mocks', ['publish_full', 'disable_copybases', 'is_not_for_publish', 'ignore_not_for_publish'])
        yield Mocks(stubs.publish_full, stubs.disable_copybases, is_not_for_publish, config.ignore_not_for_publish)


def test_not_for_publish(after_not_publish_generation, config, not_for_publish_stubs):
    """
    Проверяем, что при разладке публикуется предыдущее поколение, если это не выключено в конфиге
    """
    if is_full(config) and not not_for_publish_stubs.disable_copybases:
        gen_to_publish = PREV_GENERATION if not_for_publish_stubs.is_not_for_publish and not not_for_publish_stubs.ignore_not_for_publish else GENERATION
        publish_full_white = mock.call(gen_to_publish, config)
        assert_that(publish_full_white in not_for_publish_stubs.publish_full.mock_calls, equal_to(config.async_publish_white))


def test_mi3_cleanup(plain_config, yt_client, yt_mi3_dir, call_mock, stubs):
    """
    Проверяем, что если поколение упало, mi3-папка чистится, независимо от того, на каком шаге мы упали
    """
    yt_client.mkdir(ypath_join(yt_mi3_dir, '20220404_0404'))
    yt_client.mkdir(ypath_join(yt_mi3_dir, '20220404_0505'))
    yt_client.mkdir(ypath_join(yt_mi3_dir, '20220404_0606'))
    yt_client.link(ypath_join(yt_mi3_dir, '20220404_0505'), ypath_join(yt_mi3_dir, 'last_complete'))
    yt_client.link(ypath_join(yt_mi3_dir, '20220404_0606'), ypath_join(yt_mi3_dir, 'recent'))

    workflow.after_generation(GENERATION, success=False, config=plain_config, cleaner_cls=MI_CleanerMock)

    assert list(sorted(yt_client.list(yt_mi3_dir))) == ['20220404_0505', '20220404_0606', 'last_complete', 'recent']
