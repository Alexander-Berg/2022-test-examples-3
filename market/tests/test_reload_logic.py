# -*- coding: utf-8 -*-
from collections import defaultdict, Counter

import pytest
import mock
import random
import time
from hamcrest import (
    assert_that,
    is_,
    equal_to,
    has_length,
    only_contains,
    contains_inanyorder,
)

from freezegun import freeze_time

from backctld.client import BackctldClient
from async_publishing import (
    AsyncPublishingMode,
    GenerationMeta,
    DeadHostState,
    PackageMeta,
    GroupConfig,
    ExtraReloadParams,
    ReportStatus,
    HttpSearchStatus,
    group_reload_actions,
    clusterize_group,
    TwoPhaseReloadMetaConfig,
    TwoPhaseReloadMode,
)
from async_publishing import async_installer
from common import (  # noqa
    add_cluster_to_group,
    DefaultSearchHostState,
    DefaultSnippetHostState,
    group,
    DEFAULT_ACTIVE_GENERATIONS,
    DEFAULT_DOWNLOADED_FULL_GENERATIONS,
)

from async_publishing.actions import (
    ClusterCloseOldGeneration,
    ClusterCloseOldDynamic,
    ClusterCloseOldRtyDocument,
    ClusterDoesNotHaveActiveGeneration,
    ClusterOverCpuLimit,
    ClusterOverLimit,
    ClusterReloadFull,
    ClusterReloadingNow,
    ClusterRestart,
    NoNeedToReload,
    ClusterWaitForSecondReloadPhase,
    ClusterIsClosedDueToOldGeneration,
    ClusterIsClosedDueToOldDynamic,
    ClusterWaitForMoreCandidatesForFirstReloadPhase,
)


@pytest.fixture(scope='session')
def group_with_older_generations():
    state = {
        'newer_host': DefaultSearchHostState(
            downloaded_generations={
                'marketsearch3': DEFAULT_DOWNLOADED_FULL_GENERATIONS + ['20180101_0000'],
            },
            environment_type='production',
        ),
        'older_host': DefaultSearchHostState(
            active_generations={
                'marketsearch3': '20180101_0000',
            },
            downloaded_generations={
                'marketsearch3': DEFAULT_DOWNLOADED_FULL_GENERATIONS + ['20180101_0000'],
            },
            environment_type='production',
        ),
    }
    config = GroupConfig(
        simultaneous_restart=1,
        failures_threshold=1,
        hosts=[
            GroupConfig.Host('older_host', 9002, '0', datacenter=None),
            GroupConfig.Host('newer_host', 9002, '1', datacenter=None),
        ],
        reload_timeout=600,
        async_publishing=AsyncPublishingMode.enabled,
        min_alive={},
    )
    return state, config


def test_dont_reload_with_same_generation():
    """
    Проверим, что если требуемое поколение уже активное на кластере,
    то релоада не будет
    """
    group_state = {
        'older_host': DefaultSearchHostState(
            active_generations={
                'marketsearch3': '20180101_0000',
            },
            downloaded_generations={
                'marketsearch3': DEFAULT_DOWNLOADED_FULL_GENERATIONS + ['20180101_0000'],
            },
            environment_type='production',
        ),
    }

    group_config = GroupConfig(
        simultaneous_restart=1,
        failures_threshold=1,
        hosts=[
            GroupConfig.Host('older_host', 9002, '0', datacenter=None),
        ],
        reload_timeout=600,
        async_publishing=AsyncPublishingMode.enabled,
        min_alive={},
    )

    clusters = clusterize_group(group_state, group_config)
    full_generation = GenerationMeta('20180101_0000')

    actions = list(
        group_reload_actions(
            clusters,
            group_config,
            full_generation,
            packages={},
        )
    )

    assert_that(actions, is_(equal_to([
        NoNeedToReload(clusters['0'])
    ])))


def test_restart_with_same_generation_with_packages():
    """
    Проверим, что если требуемое поколение уже активное на кластере,
    но необходимо установить пакет, то произойдет рестарт с установкой пакета
    """

    group_state = {
        'older_host': DefaultSearchHostState(
            active_generations={
                'marketsearch3': '20180101_0000',
            },
            downloaded_generations={
                'marketsearch3': DEFAULT_DOWNLOADED_FULL_GENERATIONS + ['20180101_0000'],
            },
            environment_type='production',
            packages={
                'yandex-market-report-1.0.0': {
                    'name': 'yandex-market-report',
                    'version': '1.0.0',
                    'status': async_installer.PackageStatus.STATUS_DOWNLOADED,
                }
            }
        ),
    }

    group_config = GroupConfig(
        simultaneous_restart=1,
        failures_threshold=1,
        hosts=[
            GroupConfig.Host('older_host', 9002, '0', datacenter=None),
        ],
        reload_timeout=600,
        async_publishing=AsyncPublishingMode.enabled,
        min_alive={},
    )

    clusters = clusterize_group(group_state, group_config)
    full_generation = GenerationMeta('20180101_0000')

    actions = list(
        group_reload_actions(
            clusters,
            group_config,
            full_generation,
            packages={'production': [PackageMeta(name='yandex-market-report', version='1.0.0')]},
        )
    )

    assert_that(actions, is_(equal_to([
        ClusterRestart(clusters['0'], group_config, 600, {'yandex-market-report': '1.0.0'})
    ])))


def test_reload_group_with_older_generations(group_with_older_generations):
    '''
    Проверяем, что релоад начинается с хоста с самым старым поколением
    '''
    group_state, group_config = group_with_older_generations
    clusters = clusterize_group(group_state, group_config)
    full_generation = GenerationMeta('20180101_0600')

    actions = list(
        group_reload_actions(
            clusters,
            group_config,
            full_generation,
            packages={},
        )
    )

    assert_that(actions, is_(equal_to([
        ClusterReloadFull(clusters['0'], group_config, full_generation, 600, {}),
        ClusterOverLimit(clusters['1'])
    ])))


@freeze_time('2018-01-01 01:00')
def test_priority_reload_for_clusters_with_packages_to_install():
    """
    Проверим, что кластера, на которые нужно устанавливать пакеты,
    будут релоадится/рестартится в приоритетнм порядке:
    1. Мертрые кластера;
    2. Кластера, на которые нужно установить пакеты;
    3. Все остальные.
    """

    group_state = {
        'host0': DefaultSearchHostState(
            active_generations={
                'marketsearch3': '20180001_0000',
            },
            downloaded_generations={
                'marketsearch3': DEFAULT_DOWNLOADED_FULL_GENERATIONS + ['20180101_0000'],
            },
            environment_type='production',
            packages={}
        ),
        'host1': DefaultSearchHostState(
            active_generations={
                'marketsearch3': '20180001_0000',
            },
            downloaded_generations={
                'marketsearch3': DEFAULT_DOWNLOADED_FULL_GENERATIONS + ['20180101_0000'],
            },
            environment_type='production',
            packages={
                'yandex-market-report-1.0.0': {
                    'name': 'yandex-market-report',
                    'version': '1.0.0',
                    'status': async_installer.PackageStatus.STATUS_DOWNLOADED,
                }
            }
        ),
        'dead': DefaultSearchHostState(
            report_status=ReportStatus.DOWN,
            httpsearch_status=HttpSearchStatus.DOWN,
        ),
        'dead2': DeadHostState(
            fqdn='dead2'
        )
    }

    group_config = GroupConfig(
        simultaneous_restart=5,
        failures_threshold=5,
        hosts=[
            GroupConfig.Host('host0', 9002, '0', datacenter=None),
            GroupConfig.Host('host1', 9002, '1', datacenter=None),
            GroupConfig.Host('dead', 9002, '3', datacenter=None),
            GroupConfig.Host('dead2', 9002, '4', datacenter=None),
        ],
        reload_timeout=600,
        async_publishing=AsyncPublishingMode.enabled,
        min_alive={},
    )

    clusters = clusterize_group(group_state, group_config)
    full_generation = GenerationMeta('20180101_0000')

    actions = list(
        group_reload_actions(
            clusters,
            group_config,
            full_generation,
            packages={'production': [PackageMeta(name='yandex-market-report', version='1.0.0')]},
        )
    )

    assert_that(actions, is_(only_contains(
        ClusterDoesNotHaveActiveGeneration(clusters['4']),
        ClusterRestart(clusters['3'], group_config, 600, {}),                                  # dead cluster
        ClusterReloadFull(clusters['1'], group_config, full_generation, 600,  # got packages to install
                          {'yandex-market-report': '1.0.0'}),
        ClusterReloadFull(clusters['0'], group_config, full_generation, 600, {})
    )))


def test_reload_if_cant_choose_generation():
    """
    Проверим, что если нельзя выбрать поколения для релоада,
    то пакет все равно установится.

    В zk записано, что актуальное full поколение 20180101_0020.
    Поэтому мы не можем выбрать поколения для релоада. Тем не менее, если есть пакеты для установки,
    то мы должны выполнить релоад.

    Кластер 0: нет пакетов, поэтому на нем рестарта не будет
    Кластер 1: рестарт, потому что есть пакет
    """

    group_state = {
        'host0': DefaultSearchHostState(
            active_generations={
                'marketsearch3': '20180001_0000',
            },
            downloaded_generations={
                'marketsearch3': DEFAULT_DOWNLOADED_FULL_GENERATIONS,
            },
            environment_type='production',
            packages={}
        ),
        'host1': DefaultSearchHostState(
            active_generations={
                'marketsearch3': '20180001_0000',
            },
            downloaded_generations={
                'marketsearch3': DEFAULT_DOWNLOADED_FULL_GENERATIONS,
            },
            environment_type='production',
            packages={
                'yandex-market-report-1.0.0': {
                    'name': 'yandex-market-report',
                    'version': '1.0.0',
                    'status': async_installer.PackageStatus.STATUS_DOWNLOADED,
                }
            }
        ),
    }

    group_config = GroupConfig(
        simultaneous_restart=4,
        failures_threshold=4,
        hosts=[
            GroupConfig.Host('host0', 9002, '0', datacenter=None),
            GroupConfig.Host('host1', 9002, '1', datacenter=None),
        ],
        reload_timeout=600,
        async_publishing=AsyncPublishingMode.enabled,
        min_alive={},
    )

    clusters = clusterize_group(group_state, group_config)
    full_generation = GenerationMeta('20180101_0020')

    actions = list(
        group_reload_actions(
            clusters,
            group_config,
            full_generation,
            packages={'production': [PackageMeta(name='yandex-market-report', version='1.0.0')]},
        )
    )

    assert_that(actions, is_(only_contains(
        # cluster 0 skiped bacause we cant choose generation to realod and there is no packages to install
        ClusterRestart(clusters['1'], group_config, 600, {'yandex-market-report': '1.0.0'}),
        NoNeedToReload(clusters['0'])
    )))


@pytest.mark.parametrize('full_generation', [
    # релоад на новое поколение
    GenerationMeta('20180101_0600'),
    # релоад на то поколениe, которе уже лежит на хосте
    GenerationMeta('20180101_0300'),
])
def test_reload_group_with_down_host(full_generation):
    '''
    Проверяем, что мы первым делом порелоадим лежащий хост
    Тут 2 кейса:
    1. релоад на новое поколение
    2. релоад на то же самое которе уже лежит на хосте
    '''
    group_state = {
        'down_host': DefaultSearchHostState(
            report_status=ReportStatus.DOWN
        ),
        'alive_host': DefaultSearchHostState(),
    }
    group_config = GroupConfig(
        simultaneous_restart=1,
        failures_threshold=1,
        hosts=[
            GroupConfig.Host('down_host', 9002, '0', datacenter=None),
            GroupConfig.Host('alive_host', 9002, '1', datacenter=None),
        ],
        reload_timeout=600,
        async_publishing=AsyncPublishingMode.enabled,
        min_alive={},
    )
    clusters = clusterize_group(group_state, group_config)

    actions = list(
        group_reload_actions(
            clusters,
            group_config,
            full_generation,
            packages={},
        )
    )

    assert_that(actions, is_(only_contains(
        ClusterReloadFull(clusters['0'], group_config, full_generation, 600, {}),
        ClusterOverLimit(clusters['1'])
    )))


def test_restart_group_with_down_host():
    '''
    Если хост лежит, но новое поколение на него еще не скачалось,
    то пробуем перезапустить (restart) репорт (без распаковки поколения)
    '''

    full_generation = GenerationMeta('20180101_0700')

    group_state = {
        'down_host': DefaultSearchHostState(
            report_status=ReportStatus.DOWN
        ),
        'alive_host': DefaultSearchHostState(),
    }
    group_config = GroupConfig(
        simultaneous_restart=1,
        failures_threshold=1,
        hosts=[
            GroupConfig.Host('down_host', 9002, '0', datacenter=None),
            GroupConfig.Host('alive_host', 9002, '1', datacenter=None),
        ],
        reload_timeout=600,
        async_publishing=AsyncPublishingMode.enabled,
        min_alive={},
    )
    clusters = clusterize_group(group_state, group_config)

    actions = list(
        group_reload_actions(
            clusters,
            group_config,
            full_generation,
            packages={},
        )
    )

    assert_that(actions, is_(equal_to([
        ClusterRestart(clusters['0'], group_config, 600, {}),
        ClusterOverLimit(clusters['1'])
    ])))


@freeze_time('2018-01-01 04:00')
def test_dont_start_reload_when_generation_is_not_yet_downloaded(group_with_older_generations):
    '''
    Проверяем, что не стартуем релоад пока поколение не докачанно.
    '''
    group_state, group_config = group_with_older_generations
    clusters = clusterize_group(group_state, group_config)
    full_generation = GenerationMeta('20180101_0800')

    actions = list(
        group_reload_actions(
            clusters,
            group_config,
            full_generation,
            packages={},
        )
    )

    assert_that(actions, is_(only_contains(
        NoNeedToReload(clusters['0']),
        NoNeedToReload(clusters['1']),
    )))


def test_reload_with_downgrade(group_with_older_generations):
    '''
    Проверяем, что работает релоад на более раннее поколение.
    '''
    group_state, group_config = group_with_older_generations
    clusters = clusterize_group(group_state, group_config)
    full_generation = GenerationMeta('20180101_0000')

    actions = list(
        group_reload_actions(
            clusters,
            group_config,
            full_generation,
            packages={},
        )
    )

    assert_that(actions, is_(equal_to([
        NoNeedToReload(clusters['0']),
        ClusterReloadFull(clusters['1'], group_config, full_generation, 600, {})
    ])))


def test_dont_start_reload_when_it_is_already_running():
    group_state = {
        'host1': DefaultSearchHostState(
            report_status=ReportStatus.DOWN,
            httpsearch_status=HttpSearchStatus.DOWN,
            is_reloading=True,
        ),
        'host2': DefaultSearchHostState(
            report_status=ReportStatus.DOWN,
            httpsearch_status=HttpSearchStatus.DOWN,
            is_reloading=True,
        ),
        'host3': DefaultSearchHostState(),
        'host4': DefaultSearchHostState(),
    }
    group_config = GroupConfig(
        simultaneous_restart=1,
        failures_threshold=1,
        hosts=[
            GroupConfig.Host('host1', 9002, '1', datacenter=None),
            GroupConfig.Host('host2', 9002, '1', datacenter=None),
            GroupConfig.Host('host3', 9002, '2', datacenter=None),
            GroupConfig.Host('host4', 9002, '2', datacenter=None),
        ],
        reload_timeout=600,
        async_publishing=AsyncPublishingMode.enabled,
        min_alive={},
    )
    clusters = clusterize_group(group_state, group_config)
    full_generation = GenerationMeta('20180101_0600')

    actions = list(
        group_reload_actions(
            clusters,
            group_config,
            full_generation,
            packages={},
        )
    )

    assert_that(actions, is_(only_contains(
        ClusterReloadingNow(clusters['1']),
        ClusterOverLimit(clusters['2']),
    )))


def test_dont_reload_more_then_failuresh_threshold_clusters():
    group_state = {
        'host1': DefaultSearchHostState(
            report_status=ReportStatus.DOWN,
            httpsearch_status=HttpSearchStatus.DOWN,
        ),
        'host2': DefaultSearchHostState(
            report_status=ReportStatus.DOWN,
            httpsearch_status=HttpSearchStatus.DOWN,
        ),
        'host3': DefaultSearchHostState(
            report_status=ReportStatus.DOWN,
            httpsearch_status=HttpSearchStatus.DOWN,
        ),
        'host4': DefaultSearchHostState(
            report_status=ReportStatus.DOWN,
            httpsearch_status=HttpSearchStatus.DOWN,
        ),
    }
    group_config = GroupConfig(
        simultaneous_restart=2,
        failures_threshold=2,
        hosts=[
            GroupConfig.Host('host1', 9002, '1', datacenter=None),
            GroupConfig.Host('host2', 9002, '2', datacenter=None),
            GroupConfig.Host('host3', 9002, '3', datacenter=None),
            GroupConfig.Host('host4', 9002, '4', datacenter=None),
        ],
        reload_timeout=600,
        async_publishing=AsyncPublishingMode.enabled,
        min_alive={},
    )
    clusters = clusterize_group(group_state, group_config)
    full_generation = GenerationMeta('20180101_0600')

    actions = list(
        group_reload_actions(
            clusters,
            group_config,
            full_generation,
            packages={},
        )
    )

    # start reload only for 2 out of 4 dead clusters to limit number of reloading clusters:
    # MARKETINCIDENTS-2649
    assert_that(actions, has_length(3))
    assert_that(actions, is_(only_contains(
        ClusterReloadFull(clusters['1'], group_config, full_generation, 600, {}),
        ClusterReloadFull(clusters['2'], group_config, full_generation, 600, {}),
        ClusterOverLimit(clusters['3']),
    )))


@pytest.mark.parametrize('min_alive_total', [None, 0])
def test_reload_group_with_merged_dcs_respect_min_alive(min_alive_total):
    group_state = {
        'sas-001': DefaultSearchHostState(),
        'sas-002': DefaultSearchHostState(),
        'iva-001': DefaultSearchHostState(),
        'iva-002': DefaultSearchHostState(),
        'vla-001': DefaultSearchHostState(
            httpsearch_status=HttpSearchStatus.DOWN,
            report_status=ReportStatus.DOWN
        ),
        'vla-002': DefaultSearchHostState(),
        'vla-003': DefaultSearchHostState(),
    }
    min_alive = {
        # sas should be taken as 0 by default
        'iva': 1,
        'vla': 1,
    }
    if min_alive_total is not None:
        min_alive['total'] = min_alive_total
    group_config = GroupConfig(
        simultaneous_restart=10,
        failures_threshold=10,
        hosts=[
            GroupConfig.Host('sas-001', 9002, '0', 'sas'),
            GroupConfig.Host('sas-002', 9002, '0', 'sas'),
            GroupConfig.Host('iva-001', 9002, '1', 'iva'),
            GroupConfig.Host('iva-002', 9002, '2', 'iva'),
            GroupConfig.Host('vla-001', 9002, '3', 'vla'),
            GroupConfig.Host('vla-002', 9002, '4', 'vla'),
            GroupConfig.Host('vla-003', 9002, '5', 'vla'),
        ],
        reload_timeout=600,
        async_publishing=AsyncPublishingMode.enabled,
        min_alive=min_alive,
    )
    reload_attempts = Counter({'3': 1})
    clusters = clusterize_group(group_state, group_config)
    full_generation = GenerationMeta('20180101_0600')

    actions = list(
        group_reload_actions(
            clusters,
            group_config,
            full_generation,
            packages={},
            reload_attempts=reload_attempts,
        )
    )

    # clusters '2' and '4' should not be reloaded because of min_alive limits
    assert_that(actions, is_(only_contains(
        # reloaded because default min_alive limit for sas is 0(total - 1)
        ClusterReloadFull(clusters['0'], group_config, full_generation, 600, {}),
        ClusterReloadFull(clusters['1'], group_config, full_generation, 600, {}),
        # dead host that we alredy tried to reload, not counted in simultaneous_restart
        ClusterOverLimit(clusters['2']),
        ClusterReloadFull(clusters['3'], group_config, full_generation, 600, {}),
        ClusterReloadFull(clusters['4'], group_config, full_generation, 600, {}),
        ClusterOverLimit(clusters['5']),
    )))
    assert_that(reload_attempts, is_(equal_to({
        '0': 1,
        '1': 1,
        '3': 2,
        '4': 1,
    })))


def test_reload_group_with_merged_dcs_respect_failures_threshold():
    group_state = {
        'sas-001': DefaultSearchHostState(),
        'sas-002': DefaultSearchHostState(),
        'sas-003': DefaultSearchHostState(),
        'sas-004': DefaultSearchHostState(),
    }
    group_config = GroupConfig(
        simultaneous_restart=1,
        failures_threshold=2,
        hosts=[
            GroupConfig.Host('sas-001', 9002, '1', 'sas'),
            GroupConfig.Host('sas-002', 9002, '2', 'sas'),
            GroupConfig.Host('sas-003', 9002, '3', 'sas'),
            GroupConfig.Host('sas-004', 9002, '4', 'sas'),
        ],
        reload_timeout=700,
        async_publishing=AsyncPublishingMode.enabled,
        min_alive={
            'sas': 1,
        },
    )
    reload_attempts = Counter()
    clusters = clusterize_group(group_state, group_config)
    full_generation = GenerationMeta('20180101_0600')

    actions = list(
        group_reload_actions(
            clusters,
            group_config,
            full_generation,
            packages={},
            reload_attempts=reload_attempts,
        )
    )

    # clusters '3' and '4' should not be reloaded because of simultaneous_restart limit
    assert_that(actions, is_(only_contains(
        ClusterReloadFull(clusters['1'], group_config, full_generation, 700, {}),
        ClusterReloadFull(clusters['2'], group_config, full_generation, 700, {}),
        ClusterOverLimit(clusters['3']),
        ClusterOverLimit(clusters['4']),
    )))
    assert_that(reload_attempts, is_(equal_to({
        '1': 1,
        '2': 1,
    })))


def test_reload_group_with_merged_dcs_mix_datacenters():
    group_state = {
        'sas-001': DefaultSearchHostState(),
        'sas-002': DefaultSearchHostState(),
        'iva-001': DefaultSearchHostState(),
        'iva-002': DefaultSearchHostState(),
    }
    group_config = GroupConfig(
        simultaneous_restart=3,
        failures_threshold=2,
        hosts=[
            GroupConfig.Host('sas-001', 9002, '1', 'sas'),
            GroupConfig.Host('sas-002', 9002, '2', 'sas'),
            GroupConfig.Host('iva-001', 9002, '3', 'iva'),
            GroupConfig.Host('iva-002', 9002, '4', 'iva'),
        ],
        reload_timeout=700,
        async_publishing=AsyncPublishingMode.enabled,
        min_alive={
            'sas': 1,
            'iva': 1,
        },
    )
    reload_attempts = Counter()
    clusters = clusterize_group(group_state, group_config)
    full_generation = GenerationMeta('20180101_0600')

    actions = list(
        group_reload_actions(
            clusters,
            group_config,
            full_generation,
            packages={},
            reload_attempts=reload_attempts,
        )
    )

    # clusters '3' and '4' should not be reloaded because of simultaneous_restart limit
    assert_that(actions, is_(only_contains(
        ClusterReloadFull(clusters['1'], group_config, full_generation, 700, {}),
        ClusterOverLimit(clusters['2']),
        ClusterReloadFull(clusters['3'], group_config, full_generation, 700, {}),
        ClusterOverLimit(clusters['4']),
    )))
    assert_that(reload_attempts, is_(equal_to({
        '1': 1,
        '3': 1,
    })))


def get_install_package_cmd_suffix(packages_zk):
    package_cmds = []
    for package in packages_zk.get('production', []):
        cmd = "{}={}".format(package.name, package.version)
        package_cmds.append(cmd)
    return ",".join(package_cmds)


@pytest.fixture(scope='session', params=[None, PackageMeta(name='yandex-market-report', version='1.0.0')])
def packages_group(request):
    package = request.param
    if not package:
        return {}

    return {
        package.get_id_string(): {
            'name': package.name,
            'version': package.version,
            'status': async_installer.PackageStatus.STATUS_DOWNLOADED,
        }
    }


@pytest.fixture(scope='session', params=[None, PackageMeta(name='yandex-market-report', version='1.0.0')])
def packages_zk(request):
    package = request.param

    if not package:
        return {}

    return {
        'production': [package]
    }


@pytest.fixture(scope='session')
def group_with_snippets(packages_group):
    state = {
        'host': DefaultSearchHostState(
            environment_type='production',
            packages=packages_group,
        ),
        'snippet_host': DefaultSnippetHostState(
            environment_type='production',
            packages=packages_group,
        ),
    }
    config = GroupConfig(
        simultaneous_restart=1,
        failures_threshold=1,
        hosts=[
            GroupConfig.Host('host', 9002, '0', datacenter=None),
            GroupConfig.Host('snippet_host', 9002, '0', datacenter=None),
        ],
        reload_timeout=600,
        async_publishing=AsyncPublishingMode.enabled,
        min_alive={},
    )
    return state, config, packages_group


@pytest.fixture(scope='session')
def group_with_published_full_only(packages_group):
    state = {
        'host': DefaultSearchHostState(
            active_generations={
                'marketsearch3': '20180101_0300',
            },
            environment_type='production',
            packages=packages_group,
        )
    }
    config = GroupConfig(
        simultaneous_restart=1,
        failures_threshold=1,
        hosts=[
            GroupConfig.Host('host', 9002, '0', datacenter=None, key='key:host'),
        ],
        reload_timeout=600,
        async_publishing=AsyncPublishingMode.enabled,
        min_alive={},
    )
    return state, config, packages_group


@pytest.fixture(scope='session')
def group_with_published_full_but_dead_host(packages_group):
    state = {
        'host': DefaultSearchHostState(
            active_generations={
                'marketsearch3': '20180101_0300',
            },
            environment_type='production',
            packages=packages_group,
            httpsearch_status=HttpSearchStatus.DOWN,
            report_status=ReportStatus.DOWN
        )
    }
    config = GroupConfig(
        simultaneous_restart=1,
        failures_threshold=1,
        hosts=[
            GroupConfig.Host('host', 9002, '0', datacenter=None),
        ],
        reload_timeout=600,
        async_publishing=AsyncPublishingMode.enabled,
        min_alive={},
    )
    return state, config, packages_group


@pytest.fixture(scope='session')
def group_with_published_full_only_with_packages(packages_group):
    state = {
        'host': DefaultSearchHostState(
            active_generations={
                'marketsearch3': '20180101_0300',
            },
            downloaded_generations={
                'marketsearch3': '20180101_0300',
            },
            environment_type='production',
            packages=packages_group,
        ),
        'snippet_host': DefaultSnippetHostState(
            active_generations={
                'marketsearchsnippet': '20180101_0300',
            },
            downloaded_generations={
                'marketsearchsnippet': '20180101_0300',
            },
            environment_type='production',
            packages=packages_group,
        ),
        'dead_host': None
    }
    config = GroupConfig(
        simultaneous_restart=2,
        failures_threshold=2,
        hosts=[
            GroupConfig.Host('host', 9002, '0', datacenter=None),
            GroupConfig.Host('snippet_host', 9002, '0', datacenter=None),
            GroupConfig.Host('dead_host', 9002, '1', datacenter=None),
        ],
        reload_timeout=600,
        async_publishing=AsyncPublishingMode.enabled,
        min_alive={},

    )
    return state, config, packages_group


class DummyBackctldClient(BackctldClient):
    def __init__(self, hostname, *args, **kwargs):
        self.commands = kwargs.pop('commands', defaultdict(list))
        self._hostname = hostname
        super(DummyBackctldClient, self).__init__(hostname, *args, **kwargs)

    def send(self, command):
        self.commands[self._hostname].append(command)


@pytest.yield_fixture(scope='module')
def mock_sleep():
    with mock.patch('time.sleep'):
        yield


@pytest.fixture(scope='module')
def cluster_reload_full(group_with_snippets, mock_sleep, packages_zk):
    group_state, group_config, packages_group = group_with_snippets
    full_generation = GenerationMeta('20180101_0600')
    actions = list(
        group_reload_actions(
            clusterize_group(group_state, group_config),
            group_config,
            full_generation,
            packages=packages_zk,
        )
    )
    assert len(actions) == 1

    backctld_calls = defaultdict(list)

    def make_client(*args, **kwargs):
        return DummyBackctldClient(*args, commands=backctld_calls, **kwargs)

    for action in actions:
        action.run(backctld_client_cls=make_client)
    return backctld_calls, packages_group, packages_zk


@pytest.fixture(scope='module')
def cluster_reload_group_with_published_full_only(group_with_published_full_only, mock_sleep, packages_zk):
    group_state, group_config, packages_group = group_with_published_full_only
    full_generation = GenerationMeta('20180101_0600')
    actions = list(
        group_reload_actions(
            clusterize_group(group_state, group_config),
            group_config,
            full_generation,
            packages=packages_zk,
        )
    )
    assert len(actions) == 1

    backctld_calls = defaultdict(list)

    def make_client(*args, **kwargs):
        return DummyBackctldClient(*args, commands=backctld_calls, **kwargs)

    for action in actions:
        action.run(backctld_client_cls=make_client)
    return backctld_calls, packages_group, packages_zk


@pytest.fixture(scope='module')
def cluster_reload_group_with_published_full_but_dead_host(group_with_published_full_but_dead_host, mock_sleep, packages_zk):
    group_state, group_config, packages_group = group_with_published_full_but_dead_host
    full_generation = GenerationMeta('20180101_0300')

    actions = list(
        group_reload_actions(
            clusterize_group(group_state, group_config),
            group_config,
            full_generation,
            packages=packages_zk,
        )
    )
    assert len(actions) == 1

    backctld_calls = defaultdict(list)

    def make_client(*args, **kwargs):
        return DummyBackctldClient(*args, commands=backctld_calls, **kwargs)

    for action in actions:
        action.run(backctld_client_cls=make_client)
    return backctld_calls, packages_group, packages_zk


@pytest.fixture(scope='module')
def cluster_restart_group_with_packages_only(group_with_published_full_only_with_packages, mock_sleep, packages_zk):
    group_state, group_config, packages_group = group_with_published_full_only_with_packages
    full_generation = GenerationMeta('20180101_0600')
    actions = list(
        group_reload_actions(
            clusterize_group(group_state, group_config),
            group_config,
            full_generation,
            packages=packages_zk,
        )
    )

    assert len(actions) == 2

    backctld_calls = defaultdict(list)

    def make_client(*args, **kwargs):
        return DummyBackctldClient(*args, commands=backctld_calls, **kwargs)

    for action in actions:
        action.run(backctld_client_cls=make_client)
    return backctld_calls, packages_group, packages_zk


def test_cluster_reload_full(cluster_reload_full):
    backctld_calls, packages_group, packages_zk = cluster_reload_full

    packages_cmd_suffix = "\n"
    if packages_group and packages_zk:
        packages_cmd_suffix = " " + get_install_package_cmd_suffix(packages_zk) + "\n"

    assert backctld_calls == {
        'host': [
            'marketsearch3 close_iptruler 1\n',
            'marketsearch3 reload 20180101_0600 600{}'.format(packages_cmd_suffix),
        ],
        'snippet_host': [
            'marketsearchsnippet close_iptruler 1\n',
            'marketsearchsnippet reload 20180101_0600 600{}'.format(packages_cmd_suffix),
        ],
    }


def test_cluster_reload_group_with_published_full_only(cluster_reload_group_with_published_full_only):
    backctld_calls, packages_group, packages_zk = cluster_reload_group_with_published_full_only

    packages_cmd_suffix = "\n"
    if packages_group and packages_zk:
        packages_cmd_suffix = " " + get_install_package_cmd_suffix(packages_zk) + "\n"

    assert backctld_calls == {
        'host': [
            'marketsearch3 close_iptruler 1\n',
            'marketsearch3 reload 20180101_0600 600{}'.format(packages_cmd_suffix),
        ],
    }


def test_cluster_reload_group_with_published_full_but_dead_host(cluster_reload_group_with_published_full_but_dead_host):
    """
    Проверяем кейс: есть кластер, на котором репорт не поднят,
    при этом у него активное поколение является требуемым (оно записано в zk). Должен происходить релоад.
    """

    backctld_calls, packages_group, packages_zk = cluster_reload_group_with_published_full_but_dead_host

    packages_cmd_suffix = "\n"
    if packages_group and packages_zk:
        packages_cmd_suffix = " " + get_install_package_cmd_suffix(packages_zk) + "\n"

    assert backctld_calls == {
        'host': [
            'marketsearch3 close_iptruler 1\n',
            'marketsearch3 reload 20180101_0300 600{}'.format(packages_cmd_suffix),
        ],
    }


def test_cluster_restart_group_with_packages_only(cluster_restart_group_with_packages_only):
    """
    Проверим какие команды будут отправляться на кластер в случае если нового поколения нет:
    - если нет пакетов для установки, то никие
    - если есть пакеты для установки, то будет отправлена команда на рестарт с установкой пакетов
    """

    backctld_calls, packages_group, packages_zk = cluster_restart_group_with_packages_only

    packages_cmd_suffix = "\n"
    if packages_group and packages_zk:
        packages_cmd_suffix = " " + get_install_package_cmd_suffix(packages_zk) + "\n"

    # we got reload actions only if we have packages to install
    if packages_group and packages_zk:
        assert backctld_calls == {
            'host': [
                'marketsearch3 close_iptruler\n',
                'marketsearch3 restart 600{}'.format(packages_cmd_suffix),
            ],
            'snippet_host': [
                'marketsearchsnippet close_iptruler\n',
                'marketsearchsnippet restart 600{}'.format(packages_cmd_suffix),
            ]
        }
    else:
        assert backctld_calls == {}


def test_reload_group_with_new_hosts():
    # check that new hosts without active generations properly handled
    group_state = {
        'new_host': DefaultSearchHostState(
            active_generations={},
        ),
        'old_host': DefaultSearchHostState(),
    }
    group_config = GroupConfig(
        simultaneous_restart=1,
        failures_threshold=1,
        hosts=[
            GroupConfig.Host('new_host', 9002, '0', datacenter=None),
            GroupConfig.Host('old_host', 9002, '1', datacenter=None),
        ],
        reload_timeout=600,
        async_publishing=AsyncPublishingMode.enabled,
        min_alive={},
    )
    clusters = clusterize_group(group_state, group_config)
    full_generation = GenerationMeta('20180101_0600')

    actions = list(
        group_reload_actions(
            clusters,
            group_config,
            full_generation,
            packages={},
        )
    )

    assert_that(actions, is_(equal_to([
        ClusterReloadFull(clusters['0'], group_config, full_generation, 600, {}),
        ClusterOverLimit(clusters['1'])
    ])))


@pytest.mark.parametrize('min_alive_total', [None, 1])
def test_reload_group_with_merged_dcs_dont_reload_last_alive(min_alive_total):
    """Кейс:
    2 кластера, failuresh_threshold=1, min_alive=0(для всех ДЦ)
    1. кластер '1' живой, с новым поколением - ничего не делаем
    2. кластер '2' мертвый, но без нового поколения - restart без распаковки поколения
    """
    group_state = {
        'sas-001': DefaultSearchHostState(
            downloaded_generations={
                'marketsearch3': DEFAULT_DOWNLOADED_FULL_GENERATIONS + ['20180101_0730'],
            }
        ),
        'iva-001': DefaultSearchHostState(
            report_status=ReportStatus.DOWN
        ),
    }
    min_alive = {
        'sas': 0,
        'iva': 0,
    }
    if min_alive_total is not None:
        min_alive['total'] = min_alive_total
    group_config = GroupConfig(
        simultaneous_restart=1,
        failures_threshold=1,
        hosts=[
            GroupConfig.Host('sas-001', 9002, '1', 'sas'),
            GroupConfig.Host('iva-001', 9002, '2', 'iva'),
        ],
        reload_timeout=700,
        async_publishing=AsyncPublishingMode.enabled,
        min_alive=min_alive,
    )
    reload_attempts = Counter()
    clusters = clusterize_group(group_state, group_config)
    full_generation = GenerationMeta('20180101_0730')

    actions = list(
        group_reload_actions(
            clusters,
            group_config,
            full_generation,
            packages={},
            reload_attempts=reload_attempts,
        )
    )

    assert_that(actions, is_(equal_to([
        # С кластером '1' ничего не делеам
        ClusterRestart(clusters['2'], group_config, timeout=700, packages_to_install={}),
        ClusterOverLimit(clusters['1'])
    ])))


def test_reload_group_with_dead_host():
    """
    Проверяем, что наличие мертвого хоста в группе не ломает раскаладку.
    """
    group_state = {
        'dead_host': None,
        'host1': DefaultSearchHostState(),
        'host2': DefaultSearchHostState(),
        'host3': DefaultSearchHostState(),
        'host4': DefaultSearchHostState(),
        'host5': DefaultSearchHostState(),
    }
    group_config = GroupConfig(
        simultaneous_restart=2,
        failures_threshold=2,
        hosts=[
            GroupConfig.Host('dead_host', 9002, '0', datacenter=None),
            GroupConfig.Host('host1', 9002, '0', datacenter=None),
            GroupConfig.Host('host2', 9002, '1', datacenter=None),
            GroupConfig.Host('host3', 9002, '1', datacenter=None),
            GroupConfig.Host('host4', 9002, '2', datacenter=None),
            GroupConfig.Host('host5', 9002, '2', datacenter=None),
        ],
        reload_timeout=600,
        async_publishing=AsyncPublishingMode.enabled,
        min_alive={},
    )
    clusters = clusterize_group(group_state, group_config)
    full_generation = GenerationMeta('20180101_0600')

    actions = list(
        group_reload_actions(
            clusters,
            group_config,
            full_generation,
            packages={},
        )
    )

    assert_that(actions, is_(equal_to([
        ClusterDoesNotHaveActiveGeneration(clusters['0']),
        ClusterReloadFull(clusters['1'], group_config, full_generation, 600, {}),
        ClusterOverLimit(clusters['2'])
    ])))


def test_reload_at_least_one_on_dead_hosts():
    """
    Проверяем, что если количество мёртвых мк превышает min_alive, то раскладка идёт по одному.
    """
    group_state = {
        'sas-01': DeadHostState(fqdn='sas-01'),
        'sas-02': DeadHostState(fqdn='sas-02'),
        'sas-03': DefaultSearchHostState(),
        'sas-04': DefaultSearchHostState(),
        'iva-01': DefaultSearchHostState(),
        'iva-02': DefaultSearchHostState(),
    }
    min_alive = {
        'sas': 2,
        'iva': 1,
        'total': 2
    }
    group_config = GroupConfig(
        simultaneous_restart=2,
        failures_threshold=2,
        hosts=[
            GroupConfig.Host('sas-01', 9002, '0', datacenter='sas'),
            GroupConfig.Host('sas-02', 9002, '1', datacenter='sas'),
            GroupConfig.Host('sas-03', 9002, '2', datacenter='sas'),
            GroupConfig.Host('sas-04', 9002, '3', datacenter='sas'),
            GroupConfig.Host('iva-01', 9002, '4', datacenter='iva'),
            GroupConfig.Host('iva-02', 9002, '5', datacenter='iva'),
        ],
        reload_timeout=600,
        async_publishing=AsyncPublishingMode.enabled,
        min_alive=min_alive,
    )
    clusters = clusterize_group(group_state, group_config)
    full_generation = GenerationMeta('20180101_0600')

    actions = list(
        group_reload_actions(
            clusters,
            group_config,
            full_generation,
            packages={},
        )
    )

    assert_that(actions, is_(equal_to([
        ClusterDoesNotHaveActiveGeneration(clusters['0']),
        ClusterDoesNotHaveActiveGeneration(clusters['1']),
        ClusterReloadFull(clusters['4'], group_config, full_generation, 600, {}),
        ClusterOverLimit(clusters['5']),
        ClusterReloadFull(clusters['2'], group_config, full_generation, 600, {}),
        ClusterOverLimit(clusters['3']),
    ])))

    group_state.update(
        {
            'sas-01': None,
            'sas-02': None
        }
    )
    clusters = clusterize_group(group_state, group_config)
    actions = list(
        group_reload_actions(
            clusters,
            group_config,
            full_generation,
            packages={},
        )
    )
    assert_that(actions, is_(equal_to([
        ClusterDoesNotHaveActiveGeneration(clusters['0']),
        ClusterDoesNotHaveActiveGeneration(clusters['1']),
        ClusterReloadFull(clusters['4'], group_config, full_generation, 600, {}),
        ClusterOverLimit(clusters['5']),
        ClusterReloadFull(clusters['2'], group_config, full_generation, 600, {}),
        ClusterOverLimit(clusters['3']),
    ])))


def test_reload_dead():
    '''
    Проверяем, что мертвые хосты не перезагружаются с опцией reload_dead=False
    '''

    group_state = {
        'down_host': DefaultSearchHostState(
            report_status=ReportStatus.DOWN
        ),
        'alive_host': DefaultSearchHostState(),
    }
    group_config = GroupConfig(
        simultaneous_restart=1,
        failures_threshold=1,
        hosts=[
            GroupConfig.Host('down_host', 9002, '0', datacenter=None),
            GroupConfig.Host('alive_host', 9002, '1', datacenter=None),
        ],
        reload_timeout=600,
        async_publishing=AsyncPublishingMode.enabled,
        min_alive={}
    )
    extra_reload_params = ExtraReloadParams(
        reload_dead=False
    )
    clusters = clusterize_group(group_state, group_config)

    full_generation = GenerationMeta('20180101_0700')

    actions = list(
        group_reload_actions(
            clusters,
            group_config,
            full_generation,
            packages={},
            extra_reload_params=extra_reload_params
        )
    )

    assert_that(actions, is_(equal_to([
        NoNeedToReload(clusters['0']),
        ClusterOverLimit(clusters['1'])
    ])))


def test_reload_generation():
    '''
    Проверяем что не будет происходить релоада индекса с опцией reload_generation=False, но установятся пакеты
    '''

    group_state = {
        'host_1': DefaultSearchHostState(
            environment_type='production',
            packages={
                'yandex-market-report-1.0.0': {
                    'name': 'yandex-market-report',
                    'version': '1.0.0',
                    'status': async_installer.PackageStatus.STATUS_DOWNLOADED,
                }
            },
            downloaded_generations={
                'marketsearch3': ['20180101_0700'],
            }
        )
    }
    group_config = GroupConfig(
        simultaneous_restart=1,
        failures_threshold=1,
        hosts=[
            GroupConfig.Host('host_1', 9002, '0', datacenter=None),
        ],
        reload_timeout=600,
        async_publishing=AsyncPublishingMode.enabled,
        min_alive={}
    )
    extra_reload_params = ExtraReloadParams(
        reload_generation=False
    )
    clusters = clusterize_group(group_state, group_config)

    full_generation = GenerationMeta('20180101_0700')

    actions = list(
        group_reload_actions(
            clusters,
            group_config,
            full_generation,
            packages={'production': [PackageMeta(name='yandex-market-report', version='1.0.0')]},
            extra_reload_params=extra_reload_params
        )
    )

    assert_that(actions, is_(equal_to([ClusterRestart(clusters['0'], group_config, 600, {'yandex-market-report': '1.0.0'})])))


@freeze_time('2018-01-01 06:00')
def test_disable_installation_packages():
    '''
    Проверяем, что пакеты, перечисленные в параметре disabled_installation_packages, не будут устанавливаться
    '''

    group_state = {
        'host_1': DefaultSearchHostState(
            environment_type='production',
            packages={
                'yandex-market-report-1.0.0': {
                    'name': 'yandex-market-report',
                    'version': '1.0.0',
                    'status': async_installer.PackageStatus.STATUS_DOWNLOADED,
                }
            }
        ),
    }
    group_config = GroupConfig(
        simultaneous_restart=1,
        failures_threshold=1,
        hosts=[
            GroupConfig.Host('host_1', 9002, '0', datacenter=None),
        ],
        reload_timeout=600,
        async_publishing=AsyncPublishingMode.enabled,
        min_alive={}
    )
    extra_reload_params = ExtraReloadParams(
        disabled_installation_packages=['yandex-market-report']
    )
    clusters = clusterize_group(group_state, group_config)

    actions = list(
        group_reload_actions(
            clusters,
            group_config,
            None,
            packages={'production': [PackageMeta(name='yandex-market-report', version='1.0.0')]},
            extra_reload_params=extra_reload_params
        )
    )

    assert_that(actions, is_(equal_to([
        NoNeedToReload(clusters['0'])
    ])))


def test_do_not_restart_without_index():
    '''
    Проверяем, что не делаем рестарт если нет активного индекса
    '''

    group_state = {
        'dead': DefaultSearchHostState(
            environment_type='production',
            active_generations={},
            report_status=ReportStatus.DOWN,
            httpsearch_status=HttpSearchStatus.DOWN,
        ),
        'dead1': DefaultSearchHostState(
            environment_type='production',
            active_generations={
                'marketsearch3': '20180101_0100',
            },
            report_status=ReportStatus.DOWN,
            httpsearch_status=HttpSearchStatus.DOWN,
        ),
        'dead2': DeadHostState(
            fqdn='dead2'
        )
    }

    group_config = GroupConfig(
        simultaneous_restart=2,
        failures_threshold=2,
        hosts=[
            GroupConfig.Host('dead', 9002, '0', datacenter=None),
            GroupConfig.Host('dead1', 9002, '0', datacenter=None),
            GroupConfig.Host('dead2', 9002, '1', datacenter=None),
        ],
        reload_timeout=600,
        async_publishing=AsyncPublishingMode.enabled,
        min_alive={},
    )
    clusters = clusterize_group(group_state, group_config)

    full_generation = GenerationMeta('20180101_0700')

    actions = list(
        group_reload_actions(
            clusters,
            group_config,
            full_generation,
            packages={'production': [PackageMeta(name='yandex-market-report', version='1.0.0')]},
        )
    )

    assert_that(actions, is_(only_contains(
        ClusterDoesNotHaveActiveGeneration(clusters['0']),
        ClusterDoesNotHaveActiveGeneration(clusters['1']),
    )))


@pytest.mark.parametrize('with_actual_generation_cluster', [True, False])
@freeze_time('2018-01-01 08:00')
def test_close_old_cluster(with_actual_generation_cluster):
    """
        Закрываем старый кластер от балансера, если:
        1. Его активное поколение старше 4-х часов
        2. Есть кластер в группе, работающий на актуальном поколении
        3. Сам кластер под нагрузкой
        В других случаях не закрываем
    """

    group_state = {
        'obsolete_cluster': DefaultSearchHostState(
            environment_type='production',
            active_generations={
                'marketsearch3': '20180101_0100',
            },
            downloaded_generations={}
        ),
        'obsolete_down_cluster': DefaultSearchHostState(
            environment_type='production',
            active_generations={
                'marketsearch3': '20180101_0100',
            },
            downloaded_generations={},
            report_status=ReportStatus.DOWN,
            httpsearch_status=HttpSearchStatus.DOWN,
        ),
        'not_so_obsolete_cluster': DefaultSearchHostState(
            environment_type='production',
            active_generations={
                'marketsearch3': '20180101_0200',
            },
            downloaded_generations={}
        ),
        'not_so_obsolete_down_cluster': DefaultSearchHostState(
            environment_type='production',
            active_generations={
                'marketsearch3': '20180101_0200',
            },
            downloaded_generations={},
            report_status=ReportStatus.DOWN,
            httpsearch_status=HttpSearchStatus.DOWN,
        ),
    }

    if with_actual_generation_cluster:
        group_state['actual_cluster'] = DefaultSearchHostState(
            environment_type='production',
            active_generations={
                'marketsearch3': '20180101_0300',
            },
        )

    group_config = GroupConfig(
        simultaneous_restart=len(group_state),
        failures_threshold=len(group_state),
        hosts=[
            GroupConfig.Host('obsolete_cluster', 9002, '1', datacenter=None),
            GroupConfig.Host('obsolete_down_cluster', 9002, '2', datacenter=None),
            GroupConfig.Host('not_so_obsolete_cluster', 9002, '3', datacenter=None),
            GroupConfig.Host('not_so_obsolete_down_cluster', 9002, '4', datacenter=None),
        ],
        reload_timeout=600,
        async_publishing=AsyncPublishingMode.enabled,
        min_alive={},
    )

    if with_actual_generation_cluster:
        group_config.hosts.add(GroupConfig.Host('actual_cluster', 9002, '0', datacenter=None))

    clusters = clusterize_group(group_state, group_config)

    actions = list(
        group_reload_actions(
            clusters,
            group_config,
            GenerationMeta('20180101_0300'),
            packages={}
        )
    )

    if with_actual_generation_cluster:
        assert_that(actions, is_(only_contains(
            ClusterIsClosedDueToOldGeneration(clusters['2']),
            ClusterRestart(clusters['4'], group_config, 600, {}),
            ClusterCloseOldGeneration(clusters['1'], group_config),
            NoNeedToReload(clusters['3']),
            NoNeedToReload(clusters['0']),
        )))
    else:
        assert_that(actions, is_(only_contains(
            ClusterIsClosedDueToOldGeneration(clusters['2']),
            ClusterRestart(clusters['4'], group_config, 600, {}),
            ClusterCloseOldGeneration(clusters['1'], group_config),
            NoNeedToReload(clusters['3']),
        )))


@freeze_time('2018-01-01 08:00')
@pytest.mark.parametrize('with_actual_generation_cluster', [False, True])
@pytest.mark.parametrize('old_cluster_is_down', [False, True])
def test_close_old_cluster_if_baseline_generation_is_ambigous(with_actual_generation_cluster, old_cluster_is_down):
    actual_full_generation = '20180101_0500'

    group_state = {
        'obsolete_cluster': DefaultSearchHostState(
            environment_type='production',
            active_generations={
                'marketsearch3': '20180101_0100',
            },
            downloaded_generations={},
            report_status=ReportStatus.DOWN if old_cluster_is_down else ReportStatus.OPENED_CONSISTENT,
            httpsearch_status=HttpSearchStatus.DOWN if old_cluster_is_down else HttpSearchStatus.UP,
        )
    }

    if with_actual_generation_cluster:
        group_state['actual_cluster'] = DefaultSearchHostState(
            environment_type='production',
            active_generations={
                'marketsearch3': actual_full_generation,
            },
        )

    group_config = GroupConfig(
        simultaneous_restart=len(group_state),
        failures_threshold=len(group_state),
        hosts=[
            GroupConfig.Host('obsolete_cluster', 9002, '1', datacenter=None),
        ],
        reload_timeout=600,
        async_publishing=AsyncPublishingMode.enabled,
        min_alive={},
    )

    if with_actual_generation_cluster:
        group_config.hosts.add(GroupConfig.Host('actual_cluster', 9002, '0', datacenter=None))

    clusters = clusterize_group(group_state, group_config)

    actions = list(
        group_reload_actions(
            clusters,
            group_config,
            GenerationMeta(actual_full_generation),
            packages={}
        )
    )

    if with_actual_generation_cluster:
        assert_that(actions, is_(only_contains(
            ClusterIsClosedDueToOldGeneration(clusters['1']) if old_cluster_is_down else ClusterCloseOldGeneration(clusters['1'], group_config),
            NoNeedToReload(clusters['0']),
        )))
    elif old_cluster_is_down:
        assert_that(actions, is_(equal_to([
            ClusterRestart(clusters['1'], group_config, 600, {}),
        ])))
    else:
        assert_that(actions, is_(equal_to([
            NoNeedToReload(clusters['1'])
        ])))


@freeze_time('2018-01-01 08:00')
def test_not_close_cluster_if_actual_generation_is_old():
    """
        Закрываем старый кластер от балансера, если старое поколение (больше 6-х часов) является актуальным
    """

    group_state = {
        'old_actual_cluster1': DefaultSearchHostState(
            environment_type='production',
            active_generations={
                'marketsearch3': '20180101_0100',
            },
            downloaded_generations={},
        ),
        'old_actual_cluster2': DefaultSearchHostState(
            environment_type='production',
            active_generations={
                'marketsearch3': '20180101_0100',
            },
            downloaded_generations={},
        )
    }

    group_config = GroupConfig(
        simultaneous_restart=len(group_state),
        failures_threshold=len(group_state),
        hosts=[
            GroupConfig.Host('old_actual_cluster1', 9002, '0', datacenter=None),
            GroupConfig.Host('old_actual_cluster2', 9002, '1', datacenter=None),
        ],
        reload_timeout=600,
        async_publishing=AsyncPublishingMode.enabled,
        min_alive={},
    )
    clusters = clusterize_group(group_state, group_config)

    actions = list(
        group_reload_actions(
            clusters,
            group_config,
            GenerationMeta('20180101_0100'),
            packages={}
        )
    )

    assert_that(actions, is_(only_contains(
        NoNeedToReload(clusters['0']),
        NoNeedToReload(clusters['1']),
    )))


@freeze_time('2018-01-01 08:00')
@pytest.mark.parametrize('close_report_with_old_generation', [False, True])
def test_close_report_with_old_generation(close_report_with_old_generation):
    """
        Проверяем отключение опции close_report_with_old_generation
    """

    group_state = {
        'old_cluster': DefaultSearchHostState(
            environment_type='production',
            active_generations={
                'marketsearch3': '20180101_0100',
            },
            downloaded_generations={},
        ),
        'normal_cluster': DefaultSearchHostState(
            environment_type='production',
            active_generations={
                'marketsearch3': '20180101_0500',
            },
            downloaded_generations={},
        ),
    }

    group_config = GroupConfig(
        simultaneous_restart=len(group_state),
        failures_threshold=len(group_state),
        hosts=[
            GroupConfig.Host('old_cluster', 9002, '0', datacenter=None),
            GroupConfig.Host('normal_cluster', 9002, '1', datacenter=None),
        ],
        reload_timeout=600,
        async_publishing=AsyncPublishingMode.enabled,
        min_alive={},
    )
    clusters = clusterize_group(group_state, group_config)

    extra_reload_params = ExtraReloadParams(
        close_report_with_old_generation=close_report_with_old_generation
    )

    actions = list(
        group_reload_actions(
            clusters,
            group_config,
            GenerationMeta('20180101_0500'),
            packages={},
            extra_reload_params=extra_reload_params
        )
    )

    if close_report_with_old_generation:
        assert_that(actions, is_(only_contains(
            ClusterCloseOldGeneration(clusters['0'], group_config),
            NoNeedToReload(clusters['1']),
        )))
    else:
        assert_that(actions, is_(only_contains(
            NoNeedToReload(clusters['0']),
            NoNeedToReload(clusters['1']),
        )))


@freeze_time('2018-01-01 08:00')
@pytest.mark.parametrize('close_report_with_old_generation', [False, True])
def test_close_single_report_with_old_generation(close_report_with_old_generation):
    """
        Проверяем, что если у нас всего один мк, то он не закрывается
    """

    group_state = {
        'old_cluster': DefaultSearchHostState(
            environment_type='production',
            active_generations={
                'marketsearch3': '20180101_0100',
            },
            downloaded_generations={},
        ),
    }

    group_config = GroupConfig(
        simultaneous_restart=len(group_state),
        failures_threshold=len(group_state),
        hosts=[
            GroupConfig.Host('old_cluster', 9002, '0', datacenter=None),
        ],
        reload_timeout=600,
        async_publishing=AsyncPublishingMode.enabled,
        min_alive={
            'total': 0
        },
    )
    clusters = clusterize_group(group_state, group_config)

    extra_reload_params = ExtraReloadParams(
        close_report_with_old_generation=close_report_with_old_generation
    )

    actions = list(
        group_reload_actions(
            clusters,
            group_config,
            GenerationMeta('20180101_0500'),
            packages={},
            extra_reload_params=extra_reload_params
        )
    )

    if close_report_with_old_generation:
        assert_that(actions, is_(only_contains(
            NoNeedToReload(clusters['0']),
        )))
    else:
        assert_that(actions, is_(only_contains(
            NoNeedToReload(clusters['0']),
        )))


@pytest.mark.parametrize('close_report_with_old_docs', [False, True])
def test_close_report_with_old_docs(close_report_with_old_docs):
    """
        Проверяем отключение опции close_report_with_old_docs
    """

    def make_cluster_hosts(cluster_name, *args, **kwargs):
        return {
            '{}_host_{}'.format(cluster_name, index): DefaultSearchHostState(dynamic_data_timestamp=timestamp, **kwargs)
            for index, timestamp in enumerate(args)
        }

    def make_groups(*args):
        group_state = {}
        group_hosts = []
        for cluster_id, cluster_hosts in enumerate(args):
            group_state.update(cluster_hosts)
            for host_name in cluster_hosts:
                group_hosts.append(GroupConfig.Host(host_name, 9002, str(cluster_id), datacenter='sas'))
        return group_state, group_hosts

    # current timestamp = 1500, too old ts < 600
    group_state, group_hosts = make_groups(
        make_cluster_hosts('cluster', 1500, 1500),
        make_cluster_hosts('obsolete_cluster', 1500, 599),
    )

    min_alive = {
        'sas': 1,
    }

    group_config = GroupConfig(
        simultaneous_restart=len(group_state),
        failures_threshold=len(group_state),
        hosts=group_hosts,
        reload_timeout=600,
        async_publishing=AsyncPublishingMode.enabled,
        min_alive=min_alive,
    )

    clusters = clusterize_group(group_state, group_config)

    extra_reload_params = ExtraReloadParams(
        close_report_with_old_docs=close_report_with_old_docs
    )

    actions = list(
        group_reload_actions(
            clusters,
            group_config,
            full_generation=None,
            packages={},
            extra_reload_params=extra_reload_params,
        )
    )

    if close_report_with_old_docs:
        assert_that(actions, is_(only_contains(
            NoNeedToReload(clusters['0']),
            ClusterCloseOldDynamic(clusters['1'], group_config),
        )))
    else:
        assert_that(actions, is_(only_contains(
            NoNeedToReload(clusters['0']),
            NoNeedToReload(clusters['1']),
        )))


@freeze_time('2018-01-01 08:00')
def test_close_cluster_with_respect_of_limits():
    """
        При закрытии старого кластера от балансера учитываем лимиты
    """

    group_state = {
        'actual_cluster': DefaultSearchHostState(
            environment_type='production',
            active_generations={
                'marketsearch3': '20180101_0300',
            },
        ),
        'obsolete_cluster1': DefaultSearchHostState(
            environment_type='production',
            active_generations={
                'marketsearch3': '20180101_0100',
            },
            downloaded_generations={}
        ),
        'obsolete_cluster2': DefaultSearchHostState(
            environment_type='production',
            active_generations={
                'marketsearch3': '20180101_0200',
            },
            downloaded_generations={}
        ),
    }

    group_config = GroupConfig(
        simultaneous_restart=1,
        failures_threshold=1,
        hosts=[
            GroupConfig.Host('actual_cluster', 9002, '0', datacenter=None),
            GroupConfig.Host('obsolete_cluster1', 9002, '1', datacenter=None),
            GroupConfig.Host('obsolete_cluster2', 9002, '2', datacenter=None),
        ],
        reload_timeout=600,
        async_publishing=AsyncPublishingMode.enabled,
        min_alive={},
    )

    clusters = clusterize_group(group_state, group_config)

    actions = list(
        group_reload_actions(
            clusters,
            group_config,
            GenerationMeta('20180101_0300'),
            packages={}
        )
    )

    assert_that(actions, is_(equal_to([
        ClusterCloseOldGeneration(clusters['1'], group_config),
        ClusterOverLimit(clusters['2'])
    ])))


@pytest.mark.parametrize('report_cpu_usage', [5.0, 10.0, 15.0])
@pytest.mark.parametrize('disable_cpu_usage_limit', [False, True])
def test_cpu_usage_limit(report_cpu_usage, disable_cpu_usage_limit):
    """Кейс:
    3 кластера, min_alive=1
    Проверяем что слишком высокая загрузка ЦПУ разрешает перезапустить только один.
    А если загрузка ЦПУ низкая, то перезапускается два, как min_alive говорит.
    """
    host_state = DefaultSearchHostState(
        active_generations={
            'marketsearch3': '20180101_0200',
        },
        report_cpu_usage=report_cpu_usage,
        report_cpu_limit=20.0,
    )
    group_state = {
        'sas-001': host_state,
        'sas-002': host_state,
        'sas-003': host_state,
    }
    group_config = GroupConfig(
        simultaneous_restart=1,
        failures_threshold=1,
        hosts=[
            GroupConfig.Host('sas-001', 9002, '1', 'sas'),
            GroupConfig.Host('sas-002', 9002, '2', 'sas'),
            GroupConfig.Host('sas-003', 9002, '3', 'sas'),
        ],
        reload_timeout=700,
        async_publishing=AsyncPublishingMode.enabled,
        min_alive={
            'sas': 1,
            'total': 1,
        },
        disable_cpu_usage_limit=disable_cpu_usage_limit,
    )
    clusters = clusterize_group(group_state, group_config)
    extra_reload_params = ExtraReloadParams(
        cpu_usage_limit_multiplier=1.0
    )
    full_generation = GenerationMeta('20180101_0300')
    actions = list(
        group_reload_actions(
            clusters,
            group_config,
            full_generation,
            packages={},
            extra_reload_params=extra_reload_params
        )
    )
    cpu_usage_limit_reached = report_cpu_usage != 5.0 and not disable_cpu_usage_limit
    assert_that(actions, is_(only_contains(
        ClusterReloadFull(clusters['1'], group_config, full_generation, 700, {}),
        ClusterReloadFull(clusters['2'], group_config, full_generation, 700, {}) if not cpu_usage_limit_reached else ClusterOverCpuLimit(clusters['2']),
        ClusterOverLimit(clusters['3']) if not cpu_usage_limit_reached else ClusterOverCpuLimit(clusters['3']),
    )))


@pytest.mark.parametrize('has_reloading_clusters', [False, True])
def test_cpu_usage_and_already_reloading_clusters(has_reloading_clusters):
    reloading_host_state = DefaultSearchHostState(
        active_generations={
            'marketsearch3': '20180101_0200',
        },
        is_reloading=True,
    )
    host_state = DefaultSearchHostState(
        active_generations={
            'marketsearch3': '20180101_0200',
        },
        report_cpu_usage=20.0,
        report_cpu_limit=20.0,
    )
    group_state = {
        'sas-001': reloading_host_state if has_reloading_clusters else host_state,
        'sas-002': host_state,
        'sas-003': host_state,
    }
    group_config = GroupConfig(
        simultaneous_restart=1,
        failures_threshold=1,
        hosts=[
            GroupConfig.Host('sas-001', 9002, '1', 'sas'),
            GroupConfig.Host('sas-002', 9002, '2', 'sas'),
            GroupConfig.Host('sas-003', 9002, '3', 'sas'),
        ],
        reload_timeout=700,
        async_publishing=AsyncPublishingMode.enabled,
        min_alive={
            'sas': 1,
            'total': 1,
        },
    )
    clusters = clusterize_group(group_state, group_config)
    extra_reload_params = ExtraReloadParams(
        cpu_usage_limit_multiplier=1.0
    )
    full_generation = GenerationMeta('20180101_0300')
    actions = list(
        group_reload_actions(
            clusters,
            group_config,
            full_generation,
            packages={},
            extra_reload_params=extra_reload_params
        )
    )
    assert_that(actions, is_(only_contains(
        ClusterReloadingNow(clusters['1']) if has_reloading_clusters else ClusterReloadFull(clusters['1'], group_config, full_generation, 700, {}),
        ClusterOverCpuLimit(clusters['2']),
        ClusterOverCpuLimit(clusters['3']),
    )))


def test_close_cluster_if_dynamic_data_is_too_old():
    """
    Проверяем, закрытие кластера в случае слишком старых динамиков (поколения и пакет не заданы).
    Проверяем корректность выбора текущего времени и учет ограничения min_alive.
    """

    def make_cluster_hosts(cluster_name, *args, **kwargs):
        return {
            '{}_host_{}'.format(cluster_name, index): DefaultSearchHostState(dynamic_data_timestamp=timestamp, **kwargs)
            for index, timestamp in enumerate(args)
        }

    def make_groups(*args):
        group_state = {}
        group_hosts = []
        for cluster_id, cluster_hosts in enumerate(args):
            group_state.update(cluster_hosts)
            for host_name in cluster_hosts:
                group_hosts.append(GroupConfig.Host(host_name, 9002, str(cluster_id), datacenter='sas'))
        return group_state, group_hosts

    # current timestamp = 1500, too old ts < 600
    group_state, group_hosts = make_groups(
        make_cluster_hosts('none', None),
        make_cluster_hosts('cluster', 1500, 1500, 600),
        make_cluster_hosts('obsolete_cluster1', 1400, 1400, 599),
        make_cluster_hosts('obsolete_cluster2', 3000, 500),
        make_cluster_hosts('obsolete_cluster3', 500),
        make_cluster_hosts('obsolete_down_cluster', 100, report_status=ReportStatus.DOWN)
    )

    min_alive = {
        'sas': 3,
    }

    group_config = GroupConfig(
        simultaneous_restart=len(group_state),
        failures_threshold=len(group_state),
        hosts=group_hosts,
        reload_timeout=600,
        async_publishing=AsyncPublishingMode.enabled,
        min_alive=min_alive,
    )

    clusters = clusterize_group(group_state, group_config)

    actions = list(
        group_reload_actions(
            clusters,
            group_config,
            full_generation=None,
            packages={},
        )
    )

    assert_that(actions, contains_inanyorder(
        NoNeedToReload(clusters['0']),
        NoNeedToReload(clusters['1']),
        ClusterCloseOldDynamic(clusters['2'], group_config),
        ClusterCloseOldDynamic(clusters['3'], group_config),
        ClusterOverLimit(clusters['4']),
        ClusterIsClosedDueToOldDynamic(clusters['5']),
    ))


@pytest.mark.parametrize('reload_generation', [True, False])
@freeze_time('2018-01-01 06:00')
def test_close_cluster_while_reload_if_dynamic_data_is_too_old(reload_generation):
    """
    Проверяем, закрытие кластера в случае слишком старых динамиков (поколение задано, пакетов нет).
    """

    def make_host_state(dynamic_data_timestamp, **kwargs):
        return DefaultSearchHostState(dynamic_data_timestamp=dynamic_data_timestamp, **kwargs)

    def make_groups(*args):
        group_state = {}
        group_hosts = []
        for cluster_id, cluster_hosts in enumerate(args):
            group_state.update([cluster_hosts])
            group_hosts.append(GroupConfig.Host(cluster_hosts[0], 9002, str(cluster_id), datacenter=None))
        return group_state, group_hosts

    group_state, group_hosts = make_groups(
        ('cluster', make_host_state(1500)),
        ('cluster_reload', make_host_state(1500, report_status=ReportStatus.DOWN)),
        ('cluster_gen', make_host_state(1500, downloaded_generations={}, active_generations={'marketsearch3': '20171230_0300'})),
        ('obsolete_cluster', make_host_state(100)),
        ('obsolete_cluster_reload', make_host_state(200, report_status=ReportStatus.DOWN)),
        ('obsolete_cluster_gen', make_host_state(300, downloaded_generations={}, active_generations={'marketsearch3': '20171230_0300'})),
        ('obsolete_down_cluster', make_host_state(400, report_status=ReportStatus.DOWN)),
    )

    group_config = GroupConfig(
        simultaneous_restart=len(group_state),
        failures_threshold=len(group_state),
        hosts=group_hosts,
        reload_timeout=600,
        async_publishing=AsyncPublishingMode.enabled,
        min_alive={},
    )

    clusters = clusterize_group(group_state, group_config)

    full_generation = GenerationMeta(DEFAULT_ACTIVE_GENERATIONS['marketsearch3'])

    actions = list(
        group_reload_actions(
            clusters,
            group_config,
            full_generation=full_generation,
            packages={},
            extra_reload_params=ExtraReloadParams(reload_generation=reload_generation)
        )
    )

    reload_action = ClusterReloadFull(clusters['1'], group_config, full_generation, 600, {})
    if not reload_generation:
        reload_action = ClusterRestart(clusters['1'], group_config, 600, {})

    assert_that(actions, contains_inanyorder(
        NoNeedToReload(clusters['0']),
        reload_action,
        ClusterCloseOldGeneration(clusters['2'], group_config),
        ClusterCloseOldDynamic(clusters['3'], group_config),
        ClusterIsClosedDueToOldDynamic(clusters['4'], group_config),
        ClusterCloseOldDynamic(clusters['5'], group_config),
        ClusterIsClosedDueToOldDynamic(clusters['6'], group_config),
    ))


@pytest.mark.parametrize('phase', [1, 2])
def test_two_phase_reload_for_no_phases_group(phase):
    """
    Проверям что фазы двухфазного релоада никак не влияют на группы в которых он запрещен
    """
    group_state = {
        'sas-001': DefaultSearchHostState(),
        'sas-002': DefaultSearchHostState(),
        'iva-001': DefaultSearchHostState(),
        'iva-002': DefaultSearchHostState(),
    }
    group_config = GroupConfig(
        simultaneous_restart=3,
        failures_threshold=3,
        hosts=[
            GroupConfig.Host('sas-001', 9002, '1', 'sas'),
            GroupConfig.Host('sas-002', 9002, '2', 'sas'),
            GroupConfig.Host('iva-001', 9002, '3', 'iva'),
            GroupConfig.Host('iva-002', 9002, '4', 'iva'),
        ],
        reload_timeout=700,
        async_publishing=AsyncPublishingMode.enabled,
        min_alive={
            'sas': 0,
            'iva': 1,
        },
    )
    reload_attempts = Counter()
    clusters = clusterize_group(group_state, group_config)
    full_generation = GenerationMeta('20180101_0600', reload_phase=phase)

    actions = list(
        group_reload_actions(
            clusters,
            group_config,
            full_generation,
            packages={},
            reload_attempts=reload_attempts,
        )
    )

    assert_that(actions, is_(only_contains(
        ClusterReloadFull(clusters['1'], group_config, full_generation, 700, {}),
        ClusterReloadFull(clusters['2'], group_config, full_generation, 700, {}),
        ClusterReloadFull(clusters['3'], group_config, full_generation, 700, {}),
        ClusterOverLimit(clusters['4']),
    )))


def test_first_phase_reload_logic():
    """
    Провреяем, что при первой фазе релоада будет релоад только 1 мк,
    при этом миниклаестер выбирается из живых
    """
    group_state = {
        'sas-001': DefaultSearchHostState(),
        'sas-002': DefaultSearchHostState(),
        'iva-001': DefaultSearchHostState(report_status=ReportStatus.DOWN),
        'iva-002': DeadHostState(fqdn='iva-002'),
        'iva-003': DefaultSearchHostState(),
        'iva-004': DefaultSearchHostState(),
    }
    group_config = GroupConfig(
        simultaneous_restart=3,
        failures_threshold=3,
        hosts=[
            GroupConfig.Host('sas-001', 9002, '1', 'sas'),
            GroupConfig.Host('sas-002', 9002, '2', 'sas'),
            GroupConfig.Host('iva-001', 9002, '3', 'iva'),
            GroupConfig.Host('iva-002', 9002, '4', 'iva'),
            GroupConfig.Host('iva-003', 9002, '5', 'iva'),
            GroupConfig.Host('iva-004', 9002, '6', 'iva'),
        ],
        reload_timeout=700,
        async_publishing=AsyncPublishingMode.enabled,
        min_alive={
            'sas': 0,
            'iva': 1,
        },
        two_phase_reload=TwoPhaseReloadMode.enabled,
        first_phase_nclusters=1
    )

    reload_attempts = Counter()
    clusters = clusterize_group(group_state, group_config)
    full_generation = GenerationMeta('20180101_0600', reload_phase=1)

    actions = list(
        group_reload_actions(
            clusters,
            group_config,
            full_generation,
            packages={},
            reload_attempts=reload_attempts,
        )
    )

    assert_that(actions, is_(only_contains(
        ClusterReloadFull(clusters['5'], group_config, full_generation, 700, {}),
        ClusterDoesNotHaveActiveGeneration(clusters['4']),
        ClusterWaitForSecondReloadPhase(clusters['6']),
        ClusterWaitForSecondReloadPhase(clusters['3']),
        ClusterWaitForSecondReloadPhase(clusters['1']),
        ClusterWaitForSecondReloadPhase(clusters['2']),
    )))


def test_second_phases_reload_logic():
    """
    Проверям что перехода на вторую стадию релоада пеерегружает всю группу согласно обычно логики
    """
    group_state = {
        'sas-001': DefaultSearchHostState(active_generations={'marketsearch3': '20180101_0600'}),
        'sas-002': DefaultSearchHostState(),
        'iva-001': DefaultSearchHostState(report_status=ReportStatus.DOWN),
        'iva-002': DefaultSearchHostState(),
        'iva-003': DefaultSearchHostState(),
        'iva-004': DeadHostState(fqdn='iva-004'),
    }
    group_config = GroupConfig(
        simultaneous_restart=3,
        failures_threshold=3,
        hosts=[
            GroupConfig.Host('sas-001', 9002, '1', 'sas'),
            GroupConfig.Host('sas-002', 9002, '2', 'sas'),
            GroupConfig.Host('iva-001', 9002, '3', 'iva'),
            GroupConfig.Host('iva-002', 9002, '4', 'iva'),
            GroupConfig.Host('iva-003', 9002, '5', 'iva'),
            GroupConfig.Host('iva-004', 9002, '6', 'iva'),
        ],
        reload_timeout=700,
        async_publishing=AsyncPublishingMode.enabled,
        min_alive={
            'sas': 0,
            'iva': 1,
        },
        two_phase_reload=TwoPhaseReloadMode.enabled,
        first_phase_nclusters=1
    )

    reload_attempts = Counter()
    clusters = clusterize_group(group_state, group_config)
    full_generation = GenerationMeta('20180101_0600', reload_phase=2)

    actions = list(
        group_reload_actions(
            clusters,
            group_config,
            full_generation,
            packages={},
            reload_attempts=reload_attempts,
        )
    )

    assert_that(actions, is_(only_contains(
        NoNeedToReload(clusters['1']),
        ClusterReloadFull(clusters['3'], group_config, full_generation, 700, {}),
        ClusterReloadFull(clusters['4'], group_config, full_generation, 700, {}),
        ClusterOverLimit(clusters['2']),
        ClusterOverLimit(clusters['5']),
        ClusterDoesNotHaveActiveGeneration(clusters['6'])
    )))


def test_no_phases_reload_for_group_with_phases():
    """
    Проверяем, что если релоад не двухфазный, наличие разрешенного двухфазного релоада никак не влияет
    """
    group_state = {
        'sas-001': DefaultSearchHostState(),
        'sas-002': DefaultSearchHostState(),
        'iva-001': DefaultSearchHostState(),
        'iva-002': DefaultSearchHostState(),
    }
    group_config = GroupConfig(
        simultaneous_restart=3,
        failures_threshold=2,
        hosts=[
            GroupConfig.Host('sas-001', 9002, '1', 'sas'),
            GroupConfig.Host('sas-002', 9002, '2', 'sas'),
            GroupConfig.Host('iva-001', 9002, '3', 'iva'),
            GroupConfig.Host('iva-002', 9002, '4', 'iva'),
        ],
        reload_timeout=700,
        async_publishing=AsyncPublishingMode.enabled,
        min_alive={
            'sas': 1,
            'iva': 1,
        },
        two_phase_reload=TwoPhaseReloadMode.enabled,
        first_phase_nclusters=1,
        two_phase_reload_meta=TwoPhaseReloadMetaConfig(
            enabled=True,
            first_phase_dc='sas',
            first_phase_cluster_num=1,
        )
    )
    reload_attempts = Counter()
    clusters = clusterize_group(group_state, group_config)
    full_generation = GenerationMeta('20180101_0600')

    actions = list(
        group_reload_actions(
            clusters,
            group_config,
            full_generation,
            packages={},
            reload_attempts=reload_attempts,
        )
    )

    assert_that(actions, is_(only_contains(
        ClusterReloadFull(clusters['1'], group_config, full_generation, 700, {}),
        ClusterOverLimit(clusters['2']),
        ClusterReloadFull(clusters['3'], group_config, full_generation, 700, {}),
        ClusterOverLimit(clusters['4']),
    )))


def test_first_phase_reload_logic_reload_limited():
    """
    Провреяем, что при первой фазе релоада если один уже находится в релоаде, второй не будет релоадиться
    """
    group_state = {
        'sas-001': DefaultSearchHostState(),
        'sas-002': DefaultSearchHostState(),
        'iva-001': DefaultSearchHostState(is_reloading=True),
        'iva-002': DefaultSearchHostState(),
    }
    group_config = GroupConfig(
        simultaneous_restart=3,
        failures_threshold=3,
        hosts=[
            GroupConfig.Host('sas-001', 9002, '1', 'sas'),
            GroupConfig.Host('sas-002', 9002, '2', 'sas'),
            GroupConfig.Host('iva-001', 9002, '3', 'iva'),
            GroupConfig.Host('iva-002', 9002, '4', 'iva'),
        ],
        reload_timeout=700,
        async_publishing=AsyncPublishingMode.enabled,
        min_alive={
            'sas': 0,
            'iva': 1,
        },
        two_phase_reload=TwoPhaseReloadMode.enabled,
        first_phase_nclusters=1
    )

    reload_attempts = Counter()
    clusters = clusterize_group(group_state, group_config)
    full_generation = GenerationMeta('20180101_0600', reload_phase=1)

    actions = list(
        group_reload_actions(
            clusters,
            group_config,
            full_generation,
            packages={},
            reload_attempts=reload_attempts,
        )
    )

    assert_that(actions, is_(only_contains(
        ClusterReloadingNow(clusters['3']),
        ClusterWaitForSecondReloadPhase(clusters['4']),
        ClusterWaitForSecondReloadPhase(clusters['1']),
        ClusterWaitForSecondReloadPhase(clusters['2']),
    )))


def test_first_phase_reload_logic_reload_limited_by_reloaded():
    """
    Провреяем, что при первой фазе релоада если один уже находится в релоаде, второй не будет релоадиться
    """
    group_state = {
        'sas-001': DefaultSearchHostState(
            active_generations={
                'marketsearch3': '20180101_0600',
            },
        ),
        'sas-002': DefaultSearchHostState(),
        'iva-001': DefaultSearchHostState(),
        'iva-002': DefaultSearchHostState(),
    }
    group_config = GroupConfig(
        simultaneous_restart=3,
        failures_threshold=3,
        hosts=[
            GroupConfig.Host('sas-001', 9002, '1', 'sas'),
            GroupConfig.Host('sas-002', 9002, '2', 'sas'),
            GroupConfig.Host('iva-001', 9002, '3', 'iva'),
            GroupConfig.Host('iva-002', 9002, '4', 'iva'),
        ],
        reload_timeout=700,
        async_publishing=AsyncPublishingMode.enabled,
        min_alive={
            'sas': 0,
            'iva': 1,
        },
        two_phase_reload=TwoPhaseReloadMode.enabled,
        first_phase_nclusters=1
    )

    reload_attempts = Counter()
    clusters = clusterize_group(group_state, group_config)
    full_generation = GenerationMeta('20180101_0600', reload_phase=1)

    actions = list(
        group_reload_actions(
            clusters,
            group_config,
            full_generation,
            packages={},
            reload_attempts=reload_attempts,
        )
    )

    assert_that(actions, is_(only_contains(
        NoNeedToReload(clusters['1']),
        ClusterWaitForSecondReloadPhase(clusters['4']),
        ClusterWaitForSecondReloadPhase(clusters['3']),
        ClusterWaitForSecondReloadPhase(clusters['2']),
    )))


@freeze_time('2018-01-01 06:00')
def test_close_cluster_with_document_is_too_old():
    """
    Проверяем, закрытие кластера в случае слишком старых документов.
    """
    def make_host_state(last_rty_document_freshness, **kwargs):
        return DefaultSearchHostState(last_rty_document_freshness=last_rty_document_freshness, **kwargs)

    def make_groups(*args):
        group_state = {}
        group_hosts = []
        for cluster_id, cluster_hosts in enumerate(args):
            group_state.update([cluster_hosts])
            group_hosts.append(GroupConfig.Host(cluster_hosts[0], 9002, str(cluster_id), datacenter=None))
        return group_state, group_hosts

    now_ts = time.time()

    group_state, group_hosts = make_groups(
        ('obsolete_cluster', make_host_state(now_ts - 700)),
        ('cluster_1', make_host_state(now_ts - 350)),
        ('cluster_2', make_host_state(now_ts - 100)),
        ('obsolete_down_cluster', make_host_state(400, downloaded_generations={}, report_status=ReportStatus.DOWN)),
    )

    group_config = GroupConfig(
        simultaneous_restart=len(group_state),
        failures_threshold=len(group_state),
        hosts=group_hosts,
        reload_timeout=600,
        async_publishing=AsyncPublishingMode.enabled,
        min_alive={},
        close_report_with_old_docs=200
    )

    clusters = clusterize_group(group_state, group_config)
    full_generation = GenerationMeta(DEFAULT_ACTIVE_GENERATIONS['marketsearch3'])

    actions = list(
        group_reload_actions(
            clusters,
            group_config,
            full_generation=full_generation,
            packages={}
        )
    )

    assert_that(actions, contains_inanyorder(
        ClusterCloseOldRtyDocument(clusters['0'], group_config),
        NoNeedToReload(clusters['1']),
        NoNeedToReload(clusters['2']),
        ClusterRestart(clusters['3'], group_config, 600, {}),
    ))


@freeze_time('2018-01-01 06:00')
@pytest.mark.parametrize('max_clusters_for_reload', [1, 2])
def test_rty_old_document_cluster_close_respects_min_alive(max_clusters_for_reload):
    """
    Проверяем, что закрытие кластера в случае слишком старых документов не нарушает лимитов min_alive.
    """
    def make_host_state(last_rty_document_freshness, **kwargs):
        return DefaultSearchHostState(last_rty_document_freshness=last_rty_document_freshness, **kwargs)

    def make_groups(*args):
        group_state = {}
        group_hosts = []
        for cluster_id, cluster_hosts in enumerate(args):
            group_state.update([cluster_hosts])
            group_hosts.append(GroupConfig.Host(cluster_hosts[0], 9002, str(cluster_id), datacenter='sas'))
        return group_state, group_hosts

    now_ts = time.time()

    group_state, group_hosts = make_groups(
        ('cluster_1', make_host_state(now_ts - 100)),
        ('cluster_2', make_host_state(now_ts - 200)),
        ('cluster_3', make_host_state(now_ts - 300)),
        ('cluster_4', make_host_state(now_ts - 400)),
    )

    group_config = GroupConfig(
        simultaneous_restart=len(group_state),
        failures_threshold=len(group_state),
        hosts=group_hosts,
        reload_timeout=600,
        async_publishing=AsyncPublishingMode.enabled,
        min_alive={
            'sas': len(group_state) - max_clusters_for_reload,
        },
        close_report_with_old_docs=50
    )

    clusters = clusterize_group(group_state, group_config)
    full_generation = GenerationMeta(DEFAULT_ACTIVE_GENERATIONS['marketsearch3'])

    actions = list(
        group_reload_actions(
            clusters,
            group_config,
            full_generation=full_generation,
            packages={}
        )
    )

    assert_that(actions, contains_inanyorder(
        NoNeedToReload(clusters['0']),
        NoNeedToReload(clusters['1']),
        ClusterCloseOldRtyDocument(clusters['2'], group_config),
        ClusterOverLimit(clusters['3']) if max_clusters_for_reload == 1 else ClusterCloseOldRtyDocument(clusters['3'], group_config),
    ))


def test_reload_priority():
    '''
    Проверяме что при обычном релоаде в первую очередь происходит релоад мертвых кластеров
    '''
    group_state = {
        'sas-001': DefaultSearchHostState(active_generations={'marketsearch3': '20180101_0600'}),
        'sas-002': DefaultSearchHostState(),
        'iva-001': DefaultSearchHostState(report_status=ReportStatus.DOWN),
        'iva-002': DefaultSearchHostState(),
        'iva-003': DefaultSearchHostState(),
        'iva-004': DeadHostState(fqdn='iva-004'),
        'iva-005': DefaultSearchHostState(report_status=ReportStatus.DOWN),
    }
    group_config = GroupConfig(
        simultaneous_restart=3,
        failures_threshold=10,
        hosts=[
            GroupConfig.Host('sas-001', 9002, '1', 'sas'),
            GroupConfig.Host('sas-002', 9002, '2', 'sas'),
            GroupConfig.Host('iva-001', 9002, '3', 'iva'),
            GroupConfig.Host('iva-002', 9002, '4', 'iva'),
            GroupConfig.Host('iva-003', 9002, '5', 'iva'),
            GroupConfig.Host('iva-004', 9002, '6', 'iva'),
            GroupConfig.Host('iva-005', 9002, '7', 'iva'),
        ],
        reload_timeout=700,
        async_publishing=AsyncPublishingMode.enabled,
        min_alive={
            'sas': 0,
            'iva': 1,
        },
    )

    reload_attempts = Counter()
    clusters = clusterize_group(group_state, group_config)
    full_generation = GenerationMeta('20180101_0600')

    actions = list(
        group_reload_actions(
            clusters,
            group_config,
            full_generation,
            packages={},
            reload_attempts=reload_attempts,
        )
    )

    assert_that(actions, is_(only_contains(
        NoNeedToReload(clusters['1']),
        ClusterReloadFull(clusters['2'], group_config, full_generation, 700, {}),
        ClusterReloadFull(clusters['3'], group_config, full_generation, 700, {}),
        ClusterReloadFull(clusters['4'], group_config, full_generation, 700, {}),
        ClusterOverLimit(clusters['5']),
        ClusterDoesNotHaveActiveGeneration(clusters['6']),
        ClusterReloadFull(clusters['7'], group_config, full_generation, 700, {}),
    )))


def test_restart_without_full_generation():
    """
    Проверяем, что если full_generation отсутствует, то кластера будут порестарчены и не возникнет ошибок
    """
    group_state = {
        'sas-001': DefaultSearchHostState(
            packages={
                'yandex-market-report-1.0.0': {
                    'name': 'yandex-market-report',
                    'version': '1.0.0',
                    'status': async_installer.PackageStatus.STATUS_DOWNLOADED,
                }
            }
        ),
    }

    group_config = GroupConfig(
        simultaneous_restart=1,
        failures_threshold=1,
        hosts=[
            GroupConfig.Host('sas-001', 9002, '1', 'sas'),
        ],
        reload_timeout=600,
        async_publishing=AsyncPublishingMode.enabled,
        two_phase_reload=TwoPhaseReloadMode.enabled,
        min_alive={
            'sas': 0
        },
    )

    clusters = clusterize_group(group_state, group_config)

    actions = list(
        group_reload_actions(
            clusters,
            group_config,
            full_generation=None,
            packages={'production': [PackageMeta(name='yandex-market-report', version='1.0.0')]},
        )
    )

    assert_that(actions, is_(equal_to([
        ClusterRestart(clusters['1'], group_config, 600, {'yandex-market-report': '1.0.0'}),
    ])))


def test_waiting_for_production_requests_param():
    """
    Проверяем, что поколение с параметром wait_for_production_requests_to_stop=False, будет релоадиться, не ожидая
    окончания production запросов. Смотрим это по команде close_iptruler в backctld, в неё должен быть передан параметр False
    """
    group_state = {
        'sas-1': DefaultSearchHostState(
            active_generations={
                'marketsearch3': '20180101_0000',
            },
            downloaded_generations={
                'marketsearch3': ['20180101_0000', '20200101_1010'],
            },
            environment_type='production',
        ),
    }

    group_config = GroupConfig(
        simultaneous_restart=1,
        failures_threshold=1,
        hosts=[
            GroupConfig.Host('sas-1', 9002, '0', datacenter=None),
        ],
        reload_timeout=600,
        async_publishing=AsyncPublishingMode.enabled,
        min_alive={},
    )

    clusters = clusterize_group(group_state, group_config)
    full_generation = GenerationMeta('20200101_1010', wait_for_production_requests_to_stop=False)

    actions = list(
        group_reload_actions(
            clusters,
            group_config,
            full_generation,
            packages={},
        )
    )

    assert len(actions) == 1

    backctld_calls = defaultdict(list)

    def make_client(*args, **kwargs):
        return DummyBackctldClient(*args, commands=backctld_calls, **kwargs)

    for action in actions:
        action.run(backctld_client_cls=make_client)

    assert backctld_calls == {
        'sas-1': [
            'marketsearch3 close_iptruler 0\n',
            'marketsearch3 reload 20200101_1010 600\n'
        ]
    }


def test_first_phase_no_reload_if_not_enough_candidates():
    """
    Если недостаточно живых мк для случайного выбора, релоада не будет
    """
    group_state = {
        'sas-001': DefaultSearchHostState(),
        'sas-002': DefaultSearchHostState(),
        'iva-001': DeadHostState(fqdn='iva-001'),
    }
    group_config = GroupConfig(
        simultaneous_restart=3,
        failures_threshold=3,
        hosts=[
            GroupConfig.Host('sas-001', 9002, '1', 'sas'),
            GroupConfig.Host('sas-002', 9002, '2', 'sas'),
            GroupConfig.Host('iva-001', 9002, '3', 'iva'),
        ],
        reload_timeout=700,
        async_publishing=AsyncPublishingMode.enabled,
        min_alive={
            'sas': 0,
            'iva': 1,
        },
        two_phase_reload=TwoPhaseReloadMode.enabled,
        first_phase_nclusters=1,
        first_phase_num_candidates=3,
    )

    reload_attempts = Counter()
    clusters = clusterize_group(group_state, group_config)
    full_generation = GenerationMeta('20180101_0600', reload_phase=1)

    actions = list(
        group_reload_actions(
            clusters,
            group_config,
            full_generation,
            packages={},
            reload_attempts=reload_attempts,
        )
    )

    assert_that(actions, is_(only_contains(
        ClusterWaitForMoreCandidatesForFirstReloadPhase(clusters['1']),
        ClusterWaitForMoreCandidatesForFirstReloadPhase(clusters['2']),
        ClusterDoesNotHaveActiveGeneration(clusters['3']),
    )))


def test_first_phase_minicluster_is_chosen_at_random():
    """
    Если достаточно живых мк для первой фазы релоада, то берется случайный
    """
    group_state = {
        'sas-001': DefaultSearchHostState(),
        'sas-002': DefaultSearchHostState(),
        'iva-001': DefaultSearchHostState(),
    }
    group_config = GroupConfig(
        simultaneous_restart=3,
        failures_threshold=3,
        hosts=[
            GroupConfig.Host('sas-001', 9002, '1', 'sas'),
            GroupConfig.Host('sas-002', 9002, '2', 'sas'),
            GroupConfig.Host('iva-001', 9002, '3', 'iva'),
        ],
        reload_timeout=700,
        async_publishing=AsyncPublishingMode.enabled,
        min_alive={
            'sas': 0,
            'iva': 0,
        },
        two_phase_reload=TwoPhaseReloadMode.enabled,
        first_phase_nclusters=1,
        first_phase_num_candidates=3,
    )

    reload_attempts = Counter()
    clusters = clusterize_group(group_state, group_config)
    full_generation = GenerationMeta('20180101_0600', reload_phase=1)

    actions1 = list(
        group_reload_actions(
            clusters,
            group_config,
            full_generation,
            packages={},
            reload_attempts=reload_attempts,
            random_engine=random.Random(42).random
        )
    )

    assert_that(actions1, is_(only_contains(
        ClusterWaitForSecondReloadPhase(clusters['1']),
        ClusterReloadFull(clusters['2'], group_config, full_generation, 700, {}),
        ClusterWaitForSecondReloadPhase(clusters['3']),
    )))

    actions2 = list(
        group_reload_actions(
            clusters,
            group_config,
            full_generation,
            packages={},
            reload_attempts=reload_attempts,
            random_engine=random.Random(66).random
        )
    )

    assert_that(actions2, is_(only_contains(
        ClusterReloadFull(clusters['1'], group_config, full_generation, 700, {}),
        ClusterWaitForSecondReloadPhase(clusters['2']),
        ClusterWaitForSecondReloadPhase(clusters['3']),
    )))


def test_first_phase_on_meta_reload_logic():
    """
    Проверяем, что при первой фазе релоада на метах будет релоад только 1 мк,
    при этом миникластер выбирается из живых.
    """
    group_state = {
        'sas-001': DefaultSearchHostState(),
        'sas-002': DefaultSearchHostState(),
        'iva-001': DefaultSearchHostState(report_status=ReportStatus.DOWN),
        'iva-002': DeadHostState(fqdn='iva-002'),
        'iva-003': DefaultSearchHostState(),
        'iva-004': DefaultSearchHostState(),
    }
    group_config = GroupConfig(
        simultaneous_restart=3,
        failures_threshold=3,
        hosts=[
            GroupConfig.Host('sas-001', 9002, '1', 'sas'),
            GroupConfig.Host('sas-002', 9002, '2', 'sas'),
            GroupConfig.Host('iva-001', 9002, '3', 'iva'),
            GroupConfig.Host('iva-002', 9002, '4', 'iva'),
            GroupConfig.Host('iva-003', 9002, '5', 'iva'),
            GroupConfig.Host('iva-004', 9002, '6', 'iva'),
        ],
        reload_timeout=700,
        async_publishing=AsyncPublishingMode.enabled,
        min_alive={
            'sas': 0,
            'iva': 1,
        },
        two_phase_reload_meta=TwoPhaseReloadMetaConfig(
            enabled=True,
            first_phase_dc='iva',
            first_phase_cluster_num=1,
        )
    )

    reload_attempts = Counter()
    clusters = clusterize_group(group_state, group_config)
    full_generation = GenerationMeta('20180101_0600', reload_phase=1)

    actions = list(
        group_reload_actions(
            clusters,
            group_config,
            full_generation,
            packages={},
            reload_attempts=reload_attempts,
        )
    )

    assert_that(actions, is_(only_contains(
        ClusterReloadFull(clusters['5'], group_config, full_generation, 700, {}),
        ClusterDoesNotHaveActiveGeneration(clusters['4']),
        ClusterWaitForSecondReloadPhase(clusters['6']),
        ClusterWaitForSecondReloadPhase(clusters['3']),
        ClusterWaitForSecondReloadPhase(clusters['1']),
        ClusterWaitForSecondReloadPhase(clusters['2']),
    )))


def test_second_phase_on_meta_reload_logic():
    """
    Проверяем, что при переходе на вторую стадию релоада на метах перегружает
    всю группу согласно обычной логики.
    """
    group_state = {
        'sas-001': DefaultSearchHostState(active_generations={'marketsearch3': '20180101_0600'}),
        'sas-002': DefaultSearchHostState(),
        'iva-001': DefaultSearchHostState(report_status=ReportStatus.DOWN),
        'iva-002': DefaultSearchHostState(),
        'iva-003': DefaultSearchHostState(),
        'iva-004': DeadHostState(fqdn='iva-004'),
    }
    group_config = GroupConfig(
        simultaneous_restart=3,
        failures_threshold=3,
        hosts=[
            GroupConfig.Host('sas-001', 9002, '1', 'sas'),
            GroupConfig.Host('sas-002', 9002, '2', 'sas'),
            GroupConfig.Host('iva-001', 9002, '3', 'iva'),
            GroupConfig.Host('iva-002', 9002, '4', 'iva'),
            GroupConfig.Host('iva-003', 9002, '5', 'iva'),
            GroupConfig.Host('iva-004', 9002, '6', 'iva'),
        ],
        reload_timeout=700,
        async_publishing=AsyncPublishingMode.enabled,
        min_alive={
            'sas': 0,
            'iva': 1,
        },
        two_phase_reload_meta=TwoPhaseReloadMetaConfig(
            enabled=True,
            first_phase_dc='iva',
            first_phase_cluster_num=1,
        )
    )

    reload_attempts = Counter()
    clusters = clusterize_group(group_state, group_config)
    full_generation = GenerationMeta('20180101_0600', reload_phase=2)

    actions = list(
        group_reload_actions(
            clusters,
            group_config,
            full_generation,
            packages={},
            reload_attempts=reload_attempts,
        )
    )

    assert_that(actions, is_(only_contains(
        NoNeedToReload(clusters['1']),
        ClusterReloadFull(clusters['3'], group_config, full_generation, 700, {}),
        ClusterReloadFull(clusters['4'], group_config, full_generation, 700, {}),
        ClusterOverLimit(clusters['2']),
        ClusterOverLimit(clusters['5']),
        ClusterDoesNotHaveActiveGeneration(clusters['6'])
    )))


def test_first_phase_on_meta_reload_logic_reload_limited():
    """
    Проверяем, что при первой фазе релоада на метах, если один мк уже находится
    в релоаде, второй не будет релоадиться.
    """
    group_state = {
        'sas-001': DefaultSearchHostState(),
        'sas-002': DefaultSearchHostState(),
        'iva-001': DefaultSearchHostState(is_reloading=True),
        'iva-002': DefaultSearchHostState(),
    }
    group_config = GroupConfig(
        simultaneous_restart=3,
        failures_threshold=3,
        hosts=[
            GroupConfig.Host('sas-001', 9002, '1', 'sas'),
            GroupConfig.Host('sas-002', 9002, '2', 'sas'),
            GroupConfig.Host('iva-001', 9002, '3', 'iva'),
            GroupConfig.Host('iva-002', 9002, '4', 'iva'),
        ],
        reload_timeout=700,
        async_publishing=AsyncPublishingMode.enabled,
        min_alive={
            'sas': 0,
            'iva': 0,
        },
        two_phase_reload_meta=TwoPhaseReloadMetaConfig(
            enabled=True,
            first_phase_dc='iva',
            first_phase_cluster_num=1,
        )
    )

    reload_attempts = Counter()
    clusters = clusterize_group(group_state, group_config)
    full_generation = GenerationMeta('20180101_0600', reload_phase=1)

    actions = list(
        group_reload_actions(
            clusters,
            group_config,
            full_generation,
            packages={},
            reload_attempts=reload_attempts,
        )
    )

    assert_that(actions, is_(only_contains(
        ClusterReloadingNow(clusters['3']),
        ClusterWaitForSecondReloadPhase(clusters['4']),
        ClusterWaitForSecondReloadPhase(clusters['1']),
        ClusterWaitForSecondReloadPhase(clusters['2']),
    )))


def test_first_phase_on_meta_reload_logic_reload_limited_by_reloaded():
    """
    Проверяем, что при первой фазе релоада на мете, если один мк уже на новом
    поколении, то второй не будет релоадиться.
    """
    group_state = {
        'sas-001': DefaultSearchHostState(
            active_generations={
                'marketsearch3': '20180101_0600',
            },
        ),
        'sas-002': DefaultSearchHostState(),
        'iva-001': DefaultSearchHostState(),
        'iva-002': DefaultSearchHostState(),
    }
    group_config = GroupConfig(
        simultaneous_restart=3,
        failures_threshold=3,
        hosts=[
            GroupConfig.Host('sas-001', 9002, '1', 'sas'),
            GroupConfig.Host('sas-002', 9002, '2', 'sas'),
            GroupConfig.Host('iva-001', 9002, '3', 'iva'),
            GroupConfig.Host('iva-002', 9002, '4', 'iva'),
        ],
        reload_timeout=700,
        async_publishing=AsyncPublishingMode.enabled,
        min_alive={
            'sas': 0,
            'iva': 0,
        },
        two_phase_reload_meta=TwoPhaseReloadMetaConfig(
            enabled=True,
            first_phase_dc='sas',
            first_phase_cluster_num=1,
        )
    )

    reload_attempts = Counter()
    clusters = clusterize_group(group_state, group_config)
    full_generation = GenerationMeta('20180101_0600', reload_phase=1)

    actions = list(
        group_reload_actions(
            clusters,
            group_config,
            full_generation,
            packages={},
            reload_attempts=reload_attempts,
        )
    )

    assert_that(actions, is_(only_contains(
        NoNeedToReload(clusters['1']),
        ClusterWaitForSecondReloadPhase(clusters['4']),
        ClusterWaitForSecondReloadPhase(clusters['3']),
        ClusterWaitForSecondReloadPhase(clusters['2']),
    )))


def test_first_phase_on_meta_no_reload_if_all_allowed_candidates_are_dead():
    """
    Проверяем, что если нет живых кандидатов (из допустимых ДЦ) для мета-двухфазного релоада,
    то релоад не начнется.
    """

    generation = '20180101_0600'
    group_state = {
        'sas-001-meta-report': DefaultSearchHostState(dc='sas'),
        'man-001-meta-report': DefaultSearchHostState(dc='man', httpsearch_status=HttpSearchStatus.DOWN),
        'vla-001-meta-report': DefaultSearchHostState(dc='vla', httpsearch_status=HttpSearchStatus.DOWN),
    }
    group_config = GroupConfig(
        simultaneous_restart=3,
        failures_threshold=3,
        hosts=[
            GroupConfig.Host('sas-001-meta-report', 9002, '1', 'sas'),
            GroupConfig.Host('man-001-meta-report', 9002, '2', 'man'),
            GroupConfig.Host('vla-001-meta-report', 9002, '3', 'vla'),
        ],
        reload_timeout=700,
        async_publishing=AsyncPublishingMode.enabled,
        min_alive={dc: 0 for dc in ['sas', 'man', 'vla']},
        two_phase_reload=TwoPhaseReloadMode.disabled,
        two_phase_reload_meta=TwoPhaseReloadMetaConfig(
            enabled=True,
            first_phase_dc='man',
        ),
    )

    clusters = clusterize_group(group_state, group_config)
    full_generation = GenerationMeta(generation, reload_phase=1)

    actions = list(
        group_reload_actions(
            clusters,
            group_config,
            full_generation,
            packages={},
            reload_attempts=Counter(),
        )
    )

    assert_that(actions, is_(only_contains(
        ClusterWaitForSecondReloadPhase(clusters['1']),
        ClusterWaitForMoreCandidatesForFirstReloadPhase(clusters['2']),
        ClusterWaitForSecondReloadPhase(clusters['3']),
    )))


def test_first_phase_on_meta_full_reload():
    """
    Проверяем, что для мета-двухфазного отработает полный цикл релоада.
    """

    generation = '20180101_0600'
    # Пока что все кластера на старом поколении.
    group_state = {
        'sas-001-meta-report': DefaultSearchHostState(dc='sas'),
        'man-001-meta-report': DefaultSearchHostState(dc='man'),
        'vla-001-meta-report': DefaultSearchHostState(dc='vla'),
    }
    group_config = GroupConfig(
        simultaneous_restart=3,
        failures_threshold=3,
        hosts=[
            GroupConfig.Host('sas-001-meta-report', 9002, '1', 'sas'),
            GroupConfig.Host('man-001-meta-report', 9002, '2', 'man'),
            GroupConfig.Host('vla-001-meta-report', 9002, '3', 'vla'),
        ],
        reload_timeout=700,
        async_publishing=AsyncPublishingMode.enabled,
        min_alive={dc: 0 for dc in ['sas', 'man', 'vla']},
        two_phase_reload=TwoPhaseReloadMode.disabled,
        two_phase_reload_meta=TwoPhaseReloadMetaConfig(
            enabled=True,
            first_phase_dc='man',
        ),
    )

    clusters = clusterize_group(group_state, group_config)
    full_generation = GenerationMeta(generation, reload_phase=1)

    actions = list(
        group_reload_actions(
            clusters,
            group_config,
            full_generation,
            packages={},
            reload_attempts=Counter(),
        )
    )

    # Допустимые для первой фазы кластера в man отправились в релоад, остальные ждут фазу 2.
    assert_that(actions, is_(only_contains(
        ClusterWaitForSecondReloadPhase(clusters['1']),  # sas
        ClusterReloadFull(clusters['2'], group_config, full_generation, 700, {}),
        ClusterWaitForSecondReloadPhase(clusters['3']),  # vla
    )))

    group_state = {
        'sas-001-meta-report': DefaultSearchHostState(dc='sas'),
        'man-001-meta-report': DefaultSearchHostState(dc='man', active_generations={'marketsearch3': generation}),
        'vla-001-meta-report': DefaultSearchHostState(dc='vla'),
    }
    clusters = clusterize_group(group_state, group_config)
    full_generation = GenerationMeta(generation, reload_phase=2)  # Начинаем фазу 2.

    actions = list(
        group_reload_actions(
            clusters,
            group_config,
            full_generation,
            packages={},
            reload_attempts=Counter(),
        )
    )

    # man порелоадилась во время фазы 1, теперь релоадятся остальные.
    assert_that(actions, is_(only_contains(
        ClusterReloadFull(clusters['1'], group_config, full_generation, 700, {}),  # sas
        NoNeedToReload(clusters['2']),
        ClusterReloadFull(clusters['3'], group_config, full_generation, 700, {}),  # vla
    )))
