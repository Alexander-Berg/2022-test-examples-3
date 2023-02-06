# -*- coding: utf-8 -*-
import os
import json

import pytest
from hamcrest import (
    assert_that,
    equal_to,
    not_,
    all_of,
    none,
    has_properties
)

from async_publishing import (
    GenerationMeta,
    HostState,
    HostStateEncoder
)
from market.idx.yatf.matchers.zookeeper import ZkNodeExists
from market.pylibrary.mindexerlib import util


@pytest.fixture()
def config():
    return {
        ('publish.async', 'async_copybases'): 'true',
        ('publish.async', 'async_publish_dists_separately'): 'true',
        ('two_phase_reload', 'two_phase_groups'): 'some_group@test, dummy_report@no_dc',
        ('two_phase_reload', 'reload_wait_timeout'): '20',
        ('two_phase_reload', 'two_phase_set_not_for_publish_on_fail'): 'true',
        ('two_phase_reload', 'stats_wait_time'): '20',
    }


NOT_FOR_PUBLISH_GENERATION = '2020101_0203'


@pytest.fixture()
def mindexer_clt(mindexer_clt, reusable_mysql, reusable_zk, config):
    """Добавляем в уже созданные для mindexer_clt пару поколений
    белое full 20180101_0101
    синее 20180101_0100
    """
    mindexer_clt.add_generation_to_super('20180101_0101')
    mindexer_clt.add_generation_to_super('20180101_0100', blue=True)
    mindexer_clt.add_generation_to_super('20180101_0102')
    # not_for_publish поколение
    mindexer_clt.add_generation_to_super(NOT_FOR_PUBLISH_GENERATION, not_for_publish=True)

    res = mindexer_clt.execute('make_me_master', '--both', '--no-publish')
    assert_that(res.exit_code, equal_to(0))

    mindexer_clt.make_local_config(config)
    res = mindexer_clt.execute('reconfigure_publisher')
    assert_that(res.exit_code, equal_to(0))

    # для того что бы тесты публикации имели смысл, изначально мы не должны иметь опубликованных поколений
    assert_that(all_of(
        '/publisher/generations/full_generation', not_(ZkNodeExists(reusable_zk)),
        '/publisher/blue_generations/full_generation', not_(ZkNodeExists(reusable_zk)),
        '/publisher/generations/dists', not_(ZkNodeExists(reusable_zk)),
        '/publisher/generations/by_name', not_(ZkNodeExists(reusable_zk)),
    ))
    return mindexer_clt


@pytest.fixture()
def report_state(mindexer_clt):
    '''
    Сохраняем текущий стейт репорат.
    Считаем, что все репорты на поколение 20180101_0101
    '''
    hosts_name = [('dummy_report-{}.net'.format(i), 'marketsearch3') for i in range(8)]
    hosts_name.append(('dummy_report_snippet-0.net', 'marketsearchsnippet'))

    hosts_states = {
        fqdn: HostState(
            is_reloading=False,
            downloaded_generations={service: set(['20180101_0101'])},
            active_generations={service: '20180101_0101'},
            services=[service],
            packages=dict(),
            environment_type="",
            fqdn=fqdn,
            report_cluster_id='0'
            )
        for fqdn, service in hosts_name
    }
    util.makedirs(os.path.dirname(mindexer_clt.config.search_state_path))
    with open(mindexer_clt.config.search_state_path, 'w') as state_file_fd:
        json.dump(hosts_states, state_file_fd, cls=HostStateEncoder, indent=4)


def test_async_copybases_already_reload(mindexer_clt, reusable_zk, report_state):
    '''
    Провреяем, что если в отслеживаемой группе репорта хотя бы один мк перешел на новое поколение,
    то переходим ко второй фазе релоада
    '''
    res = mindexer_clt.execute('copybases', '20180101_0101')
    meta = GenerationMeta.from_str(reusable_zk.get('/publisher/generations/full_generation')[0])
    assert_that(res.exit_code, equal_to(0))
    assert_that(
        meta,
        has_properties({
            'name': '20180101_0101',
            'reload_phase': 2
        })
    )


def test_async_copybases_timeout(mindexer_clt, reusable_zk, report_state):
    '''
    Проверяем, что если за отведенный таймаут релоад не случился,
    то происходит откат к предыдущему поколению
    '''
    mindexer_clt.execute('copybases', '20180101_0101')
    res = mindexer_clt.execute('copybases', '20180101_0102')
    assert_that(res.exit_code, equal_to(0))
    assert_that(
        GenerationMeta.from_str(reusable_zk.get('/publisher/generations/full_generation')[0]).name,
        equal_to('20180101_0101')
    )


def test_async_copybases_off(mindexer_clt, reusable_zk, report_state):
    '''
    Проверяем что, при отключенном двухфазном релоаде, релоад метка поколеия
    выставляется корректно, фаза релоада не проставляется
    '''
    mindexer_clt.update_local_config({
        ('publish.async', 'async_copybases'): 'false'
    })
    res = mindexer_clt.execute('copybases', '20180101_0102')
    meta = GenerationMeta.from_str(reusable_zk.get('/publisher/generations/full_generation')[0])
    assert_that(res.exit_code, equal_to(0))
    assert_that(
        meta,
        has_properties({
            'name': '20180101_0102',
            'reload_phase': none()
        })
    )


def test_no_group_in_async_copybases(mindexer_clt, reusable_zk, report_state):
    '''
    Провреям, что если не нашлось групп с двухфазным релоадом,
    то релоад происходит в обычном порядке
    '''
    mindexer_clt.update_local_config({
        ('two_phase_reload', 'two_phase_groups'): ''
    })
    res = mindexer_clt.execute('copybases', '20180101_0102')
    assert_that(res.exit_code, equal_to(0))
    assert_that(
        GenerationMeta.from_str(reusable_zk.get('/publisher/generations/full_generation')[0]).name,
        equal_to('20180101_0102')
    )


def test_async_copybases_marks_generation_as_not_for_publish_on_failure(mindexer_clt, reusable_zk, report_state):
    '''
    Проверяем, что при фейле двухфазного релоада (например, таймауте)
    поколение помечается как нераскладываемое
    '''
    # arrange
    mindexer_clt.execute('copybases', '20180101_0101')

    # act
    mindexer_clt.execute('copybases', '20180101_0102')

    # assert
    res = mindexer_clt.execute('check_generation_for_not_publish', '20180101_0102')
    assert_that(
        res.stdout.strip(),
        equal_to('True')
    )


def test_copybases_exit_if_not_for_publish(mindexer_clt, reusable_zk):
    res = mindexer_clt.execute('copybases', NOT_FOR_PUBLISH_GENERATION)
    assert_that(res.exit_code, equal_to(0))
