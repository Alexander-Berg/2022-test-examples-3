# -*- coding: utf-8 -*-
import logging
import os
import json

import pytest
from hamcrest import (
    assert_that,
    equal_to,
    not_,
    all_of,
    has_properties
)

from async_publishing import (
    Client as AsyncPublisherClient,
    GenerationMeta,
    HostState,
    HostStateEncoder
)

from market.idx.yatf.matchers.zookeeper import ZkNodeExists
from market.pylibrary.mindexerlib import util


logger = logging.getLogger()
OLD_META_GENERATION = '20210101_0101'
NEW_META_GENERATION = '20210101_0401'


os.environ['ZOOKEEPER_PREFIX'] = '/'


# ================================== miconfig fixtures ==================================
@pytest.fixture()
def config():
    config = dict()
    config.update(_common_config_values())
    return config


# ================================== mindexer_clt fixtures ==================================
@pytest.fixture()
def mindexer_clt_with_meta(mindexer_clt, reusable_mysql, reusable_zk, config):
    """
    Настройки мастера и индексов для групп репорта с метой
    """

    return _common_mindexer_clt(mindexer_clt, reusable_zk, config)


# ================================== report_state fixtures ==================================
@pytest.fixture()
def report_state_with_already_reloaded_meta(mindexer_clt_with_meta):
    # Все репорты на поколении OLD_META_GENERATION.
    return _save_configs(mindexer_clt_with_meta)


@pytest.fixture()
def report_state_with_partially_reloaded_meta(mindexer_clt_with_meta, reusable_zk):
    _set_current_generation(OLD_META_GENERATION, mindexer_clt_with_meta, reusable_zk)
    return _save_configs(mindexer_clt_with_meta, per_group_generations={
        'market-report-meta-exp1': {
            'man': NEW_META_GENERATION,
        },
        'market-report-exp1': {
            'man': NEW_META_GENERATION,
        },
        'api-report-exp1': {
            'man': NEW_META_GENERATION,
        },
    })


@pytest.fixture()
def report_state_with_partially_reloaded_meta_reload_fail(mindexer_clt_with_meta, reusable_zk):
    _set_current_generation(OLD_META_GENERATION, mindexer_clt_with_meta, reusable_zk)
    return _save_configs(mindexer_clt_with_meta, per_group_generations={
        'market-report-meta-exp1': {
            'man': NEW_META_GENERATION,
        },
        'market-report-exp1': {
            'vla': NEW_META_GENERATION,
        },
        'api-report-exp1': {
            'sas': NEW_META_GENERATION,
        },
    })


# ================================== helpers for fixtures ==================================
def _save_configs(mindexer_clt, per_group_generations=None):
    """
    Вспомогательная функция, которая готовит конфиг редуктора и state-файлик по репортам
    На входе:
        * mindexer_clt: mindexer_clt fixture
        * per_group_generations: Dict[str, Dict[str, str]] - group_name->dc->generation
    На выходе:
        * mindexer_clt fixture с настроенными конфигами
    """

    per_group_generations = per_group_generations or dict()
    all_dcs = ['man', 'vla', 'sas']
    group_map = {
        'market-report-meta-exp1': 'market_report_meta_exp1@atlantis',
        'market-report-exp1': 'market_report_exp1@atlantis',
        'api-report-exp1': 'api_report_exp1@atlantis',
    }
    hosts_per_cluster = {
        'market-report-meta-exp1': 1,
        'market-report-exp1': 16,
        'api-report-exp1': 16,
    }

    def build_host_states(host_info):
        return {
            fqdn: HostState(
                is_reloading=False,
                downloaded_generations={'marketsearch3': [OLD_META_GENERATION, NEW_META_GENERATION]},
                active_generations={'marketsearch3': generation},
                services=['marketsearch3'],
                packages=dict(),
                environment_type="",
                fqdn=fqdn,
                report_cluster_id='prod@report@{}@{:02d}'.format(dc, cluster_id),
                dc=dc,
            )
            for fqdn, generation, dc, cluster_id in host_info
        }

    def get_group_by_fqdn(fqdn):
        for rtc_name, group in group_map.items():
            if rtc_name in fqdn:
                return group

    def get_generation(group_name, dc):
        gen = per_group_generations.get(group_name, dict()).get(dc)
        return gen or OLD_META_GENERATION

    host_info = []
    for group_id, group_name in enumerate(hosts_per_cluster.keys(), start=1):
        cluster_id = 0
        for dc in all_dcs:
            cluster_id += 1
            for host_id in range(1, hosts_per_cluster[group_name] + 1):
                fqdn = '{dc}0-1234-312-{dc}-market-{group}--{group_id}{host_id:02}-17050.gencfg-c.yandex.net'.format(
                    dc=dc,
                    group=group_name,
                    group_id=group_id,
                    host_id=host_id,
                )
                gen = get_generation(group_name, dc)
                host_info.append((fqdn, gen, dc, cluster_id))
    hosts_states = build_host_states(host_info)

    # Готовим конфиг редуктора
    reductor_config = {'dcgroups': dict()}
    dcgroups = reductor_config['dcgroups']
    for fqdn, _, _, cluster_id in host_info:
        group = get_group_by_fqdn(fqdn)
        if group not in dcgroups:
            dcgroups[group] = {
                'async_publishing_mode': 'enabled',
                'generations_prefix': 'generations',
                'hosts': dict(),
            }
        dcgroups[group]['hosts'][fqdn] = {
            'cluster': cluster_id,
            'datacenter': fqdn[:3],
            'key': fqdn,
            'name': fqdn,
            'service': 'marketsearch3',
        }

    util.makedirs(os.path.dirname(mindexer_clt.config.search_state_path))
    util.makedirs(os.path.dirname(mindexer_clt.config.reductor_config_path))
    with open(mindexer_clt.config.search_state_path, 'w') as state_file_fd:
        logger.info('Saving report state to: %s', mindexer_clt.config.search_state_path)
        json.dump(hosts_states, state_file_fd, cls=HostStateEncoder, indent=4)
    with open(mindexer_clt.config.reductor_config_path, 'w') as reductor_config_fd:
        logger.info('Saving reductor config to: %s', mindexer_clt.config.reductor_config_path)
        json.dump(reductor_config, reductor_config_fd, indent=4)
    return mindexer_clt


def _common_config_values():
    return {
        ('publish.async', 'async_copybases'): 'true',
        ('publish.async', 'async_publish_dists_separately'): 'true',
        ('publish.async', 'root_prefix'): '/publisher',
        ('publish.async', 'generations_prefix'): 'generations',
        ('two_phase_reload', 'reload_wait_timeout'): '20',
        ('two_phase_reload', 'two_phase_set_not_for_publish_on_fail'): 'true',
        ('two_phase_reload', 'stats_wait_time'): '20',

        # group: market_report_meta_exp1@atlantis
        ('two_phase_reload_meta:market_report_meta_exp1@atlantis', 'enabled'): 'true',
        ('two_phase_reload_meta:market_report_meta_exp1@atlantis', 'base_group'): 'market_report_exp1@atlantis',
        ('two_phase_reload_meta:market_report_meta_exp1@atlantis', 'dc_allowed_for_reload'): 'man,vla,sas',
        ('two_phase_reload_meta:market_report_meta_exp1@atlantis', 'stats_wait_time'): '20',
    }


def _common_mindexer_clt(mindexer_clt, reusable_zk, config):
    mindexer_clt.add_generation_to_super(OLD_META_GENERATION)
    mindexer_clt.add_generation_to_super(NEW_META_GENERATION)

    res = mindexer_clt.execute('make_me_master', '--both', '--no-publish')
    assert_that(res.exit_code, equal_to(0))

    mindexer_clt.make_local_config(config)
    res = mindexer_clt.execute('reconfigure_publisher')
    assert_that(res.exit_code, equal_to(0))

    assert_that(all_of(
        '/publisher/generations/full_generation', not_(ZkNodeExists(reusable_zk)),
        '/publisher/blue_generations/full_generation', not_(ZkNodeExists(reusable_zk)),
        '/publisher/generations/dists', not_(ZkNodeExists(reusable_zk)),
        '/publisher/generations/by_name', not_(ZkNodeExists(reusable_zk)),
    ))
    return mindexer_clt


def _set_current_generation(generation, mindexer_clt, reusable_zk):
    async_publisher_client = AsyncPublisherClient(
        zk_client=reusable_zk,
        prefix=mindexer_clt.config.async_publish_root_prefix,
        generations_prefix=mindexer_clt.config.async_publish_generations_prefix,
    )
    async_publisher_client.publish_full_generation(
        GenerationMeta(generation, reload_phase=2),
        phase=2,
    )


# ================================== tests of two-phase reload for meta ==================================
def test_async_copybases_already_reloaded_meta(report_state_with_already_reloaded_meta, reusable_zk):
    """
    Проверяем, что если в отслеживаемой группе репорта хотя бы одна пара <мета, базовый> перешла на новое поколение,
    то переходим ко второй фазе релоада
    """

    res = report_state_with_already_reloaded_meta.execute('copybases', OLD_META_GENERATION)
    assert_that(res.exit_code, equal_to(0))
    meta = GenerationMeta.from_str(reusable_zk.get('/publisher/generations/full_generation')[0])
    assert_that(
        meta,
        has_properties({
            'name': OLD_META_GENERATION,
            'reload_phase': 2
        })
    )


def test_async_copybases_partially_reloaded_consistent_meta(report_state_with_partially_reloaded_meta, reusable_zk):
    """
    Проверяем, что если в отслеживаемой группе репорта хотя бы одна пара <мета, базовый> перешла на новое поколение
    в одном и том же ДЦ (читай, состояние консистентно), то переходим ко второй фазе релоада.
    """

    res = report_state_with_partially_reloaded_meta.execute('copybases', NEW_META_GENERATION)
    meta = GenerationMeta.from_str(reusable_zk.get('/publisher/generations/full_generation')[0])
    assert_that(res.exit_code, equal_to(0))
    assert_that(
        meta,
        has_properties({
            'name': NEW_META_GENERATION,
            'reload_phase': 2
        })
    )


def test_async_copybases_partially_reloaded_inconsistent_meta_fail(report_state_with_partially_reloaded_meta_reload_fail, reusable_zk):
    """
    Проверяем, что если в отслеживаемой группе репорта нет ни одной консистентной (в рамках одного ДЦ) пары <мета, базовый>,
    то переход на вторую фазу не происходит, а происходит откат на предыдущее поколение.
    """
    # Релоадимся на новое поколение
    res = report_state_with_partially_reloaded_meta_reload_fail.execute('copybases', NEW_META_GENERATION)
    assert_that(res.exit_code, equal_to(0))
    assert_that(
        GenerationMeta.from_str(reusable_zk.get('/publisher/generations/full_generation')[0]).name,
        equal_to(OLD_META_GENERATION)  # Откатились обратно
    )
