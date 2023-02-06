# -*- coding: utf-8 -*-
import pytest
from async_publishing import (
    clusterize_group,
    HostState,
    ClusterState,
    DeadHostState,
    GenerationMeta,
    GroupConfig,
    AsyncPublishingMode,
)

from common import group, DefaultSearchHostState, DEFAULT_PROGRESS_REPORT_STATES, GENERATION_IN_PROGRESS  # noqa


@pytest.fixture()
def clusterize_group_result(group):  # noqa
    group_state, group_config = group
    return clusterize_group(group_state, group_config)


@pytest.mark.parametrize('cluster_id,is_reloading', [
    ('0', False),
    ('1', True),
    ('2', False),
])
def test_cluster_is_reloading(clusterize_group_result, cluster_id, is_reloading):
    assert clusterize_group_result[cluster_id].is_reloading == is_reloading


@pytest.mark.parametrize('cluster_id,is_alive', [
    ('0', True),
    ('1', False),
    ('2', False),
])
def test_cluster_is_alive(clusterize_group_result, cluster_id, is_alive):
    assert clusterize_group_result[cluster_id].is_alive == is_alive


@pytest.mark.parametrize('cluster_id,active_generations', [
    ('0', {
        'marketsearch3': '20180101_0300',
        'marketsearchsnippet': '20180101_0300',
    }),
    ('1', {
        'marketsearchsnippet': '20180101_0300',
    }),
    ('2', {
        'marketsearch3': '20180101_0300',
        'marketsearchsnippet': '20180101_0300',
    }),
])
def test_cluster_active_generations(clusterize_group_result, cluster_id, active_generations):
    assert clusterize_group_result[cluster_id].active_generations == active_generations


@pytest.mark.parametrize('cluster_id,downloaded_generations', [
    ('0', {
        'marketsearch3': set(['20180101_0000', '20180101_0300']),
        'marketsearchsnippet': set(['20180101_0300', '20180101_0600']),
    }),
    ('1', {
        'marketsearch3': set(['20180101_0000', '20180101_0300', '20180101_0600']),
        'marketsearchsnippet': set(['20180101_0300', '20180101_0600']),
    }),
    # cluster 3 is same as cluster 0
])
def test_cluster_downloaded_generations(clusterize_group_result, cluster_id, downloaded_generations):
    assert clusterize_group_result[cluster_id].downloaded_generations == downloaded_generations


def test_cluster_services(clusterize_group_result):
    assert all(
        set(cluster.services) == set(['marketsearch3', 'marketsearchsnippet'])
        for cluster in clusterize_group_result.values()
    )


@pytest.fixture(scope='module')
def group_with_failed_host():
    group_state = {
        'good_host': DefaultSearchHostState(),
        'bad_host': None,
    }
    group_config = GroupConfig(
        1,
        1,
        hosts=[
            GroupConfig.Host(
                key='key:good_host',
                fqdn='good_host',
                port=9002,
                cluster_id=0,
                datacenter='sas',
            ),
            GroupConfig.Host(
                key='key:bad_host',
                fqdn='bad_host',
                port=9002,
                cluster_id=0,
                datacenter='sas',
            ),
        ],
        reload_timeout=900,
        async_publishing=AsyncPublishingMode.enabled,
        min_alive={},
    )
    return group_state, group_config


@pytest.fixture(scope='module')
def clusterize_group_group_with_failed_host(group_with_failed_host):
    group_state, group_config = group_with_failed_host
    return clusterize_group(group_state, group_config)


def test_cluster_with_dead_is_reloading(clusterize_group_group_with_failed_host):
    assert clusterize_group_group_with_failed_host['0'].is_reloading is False


def test_cluster_with_dead_is_dead(clusterize_group_group_with_failed_host):
    assert clusterize_group_group_with_failed_host['0'].is_alive is False


def test_cluster_with_dead_active_generations(clusterize_group_group_with_failed_host):
    assert clusterize_group_group_with_failed_host['0'].active_generations is None


def test_cluster_with_dead_downloaded_generations(clusterize_group_group_with_failed_host):
    assert clusterize_group_group_with_failed_host['0'].downloaded_generations == {}


def test_cluster_with_dead_services(clusterize_group_group_with_failed_host):
    assert clusterize_group_group_with_failed_host['0'].services == set()


def test_dead_host():
    dead_host = DeadHostState('iva-1211')
    assert isinstance(dead_host, HostState)
    assert not dead_host.is_alive
    assert not dead_host.is_reloading
    assert len(dead_host.active_generations) == 0
    assert len(dead_host.services) == 0
    assert len(dead_host.packages) == 0
    assert len(dead_host.generations_in_progress) == 0
    assert dead_host.is_dead_host
    assert dead_host.fqdn == 'iva-1211'
    assert dead_host == HostState.from_json_dict(dead_host._to_json_dict())
    assert HostState.info_absent(dead_host)


def test_healthy_host():
    host_state = HostState(
        is_reloading=False,
        downloaded_generations=dict(),
        active_generations='20180101_0000',
        services=[],
        packages={},
        environment_type='production',
        port=239
    )

    assert not host_state.is_dead_host
    assert not HostState.info_absent(host_state)
    assert host_state.port == 239


@pytest.mark.parametrize('generation_downloaded,downloaded_generations', [
    (True, {
        'marketsearch3': set(['20180101_0000']),
        'marketsearchsnippet': set(['20180101_0000']),
    }),
    (False, {
        'marketsearch3': set(['20180101_0000']),
        'marketsearchsnippet': set(['20180101_0300']),
    }),
    (False, {
        'marketsearch3': set(),
        'marketsearchsnippet': set(),
    }),
])
def test_full_generation_downloaded(generation_downloaded, downloaded_generations):
    host_state = HostState(
        is_reloading=False,
        downloaded_generations=downloaded_generations,
        active_generations='20180101_0000',
        services=downloaded_generations.keys(),
        packages={},
        environment_type='production'
    )
    generation = GenerationMeta('20180101_0000')
    assert (
        generation_downloaded == (generation in host_state) and
        generation_downloaded == host_state.generation_downloaded(generation)
    )


@pytest.mark.parametrize('is_generation_active,active_generations', [
    (True, {
        'marketsearch3': '20180101_0000',
        'marketsearchsnippet': '20180101_0000',
    }),
    (False, {
        'marketsearch3': '20180101_0000',
        'marketsearchsnippet': '20180101_0300',
    }),
    (False, {
        'marketsearch3': '20180101_0300',
        'marketsearchsnippet': '20180101_0300',
    }),
])
def test_full_generation_active(is_generation_active, active_generations):
    host_state = HostState(
        is_reloading=False,
        downloaded_generations={},
        active_generations=active_generations,
        services=active_generations.keys(),
        packages={},
        environment_type='production'
    )
    generation = GenerationMeta('20180101_0000')
    assert is_generation_active == host_state.generation_active(generation)


def test_host_state_init():
    host_state = HostState(
        is_reloading=True,
        downloaded_generations={},
        active_generations='20180101_0000',
        services={},
        packages={},
        environment_type='production',
        fqdn='iva-001@iva',
        report_cluster_id='1',
        port=239
    )

    assert str(host_state)
    assert host_state.is_reloading
    assert host_state.environment_type == 'production'
    assert host_state.fqdn == 'iva-001@iva'
    assert host_state.report_cluster_id == '1'
    assert host_state.port == 239


def test_progress_report():
    host_state = HostState(
        is_reloading=False,
        downloaded_generations={},
        active_generations='20180101_0000',
        services={},
        packages={},
        environment_type='production',
        fqdn='iva-001@iva',
        report_cluster_id='1',
        port=239,
        progress_report_states=DEFAULT_PROGRESS_REPORT_STATES
    )

    assert GENERATION_IN_PROGRESS in host_state.progress_report_states
    assert 'total_bytes' in host_state.progress_report_states[GENERATION_IN_PROGRESS]
    assert 'done_bytes' in host_state.progress_report_states[GENERATION_IN_PROGRESS]
    assert 'stage' in host_state.progress_report_states[GENERATION_IN_PROGRESS]

    assert host_state.progress_report_states[GENERATION_IN_PROGRESS]['total_bytes'] >= host_state.progress_report_states[GENERATION_IN_PROGRESS]['done_bytes']


def test_cluster_state():
    cluster = ClusterState(
        host_states={'good_host': DefaultSearchHostState()},
        cluster_id=1
    )

    assert str(cluster)
