# -*- coding: utf-8 -*-
from copy import deepcopy
import pytest
from async_publishing import (
    HostState,
    SnippetSearchHostState,
    BaseSearchHostState,
    ReportStatus,
    HttpSearchStatus,
    GroupConfig,
    AsyncPublishingMode,
)

GENERATION_IN_PROGRESS = '20180101_0301'
DEFAULT_DOWNLOADED_FULL_GENERATIONS = ['20180101_0300', '20180101_0600']
DEFAULT_DOWNLOADED_GENERATIONS = {
    'marketsearch3': DEFAULT_DOWNLOADED_FULL_GENERATIONS,
}
DEFAULT_ACTIVE_GENERATIONS = {
    'marketsearch3': '20180101_0300',
}
DEFAULT_PROGRESS_REPORT_STATES = {
    GENERATION_IN_PROGRESS: {
        'stage': 'get_resource',
        'total_bytes': 915281,
        'done_bytes': 91528,
    }
}


def _add_default_host_state_arguments(kwargs):
    if 'is_reloading' not in kwargs:
        kwargs['is_reloading'] = False
    if 'downloaded_generations' not in kwargs:
        kwargs['downloaded_generations'] = deepcopy(DEFAULT_DOWNLOADED_GENERATIONS)
    if 'active_generations' not in kwargs:
        kwargs['active_generations'] = deepcopy(DEFAULT_ACTIVE_GENERATIONS)
    if 'services' not in kwargs:
        kwargs['services'] = ['marketsearch3']
    if 'packages' not in kwargs:
        kwargs['packages'] = {}
    if 'environment_type' not in kwargs:
        kwargs['environment_type'] = 'production'
    if 'port' not in kwargs:
        kwargs['port'] = 1234
    return kwargs


class DefaultHostState(HostState):
    def __init__(self, **kwargs):
        kwargs = _add_default_host_state_arguments(kwargs)
        super(DefaultHostState, self).__init__(**kwargs)


class DefaultSnippetHostState(SnippetSearchHostState):
    def __init__(self, **kwargs):
        if 'is_reloading' not in kwargs:
            kwargs['is_reloading'] = False
        if 'downloaded_generations' not in kwargs:
            kwargs['downloaded_generations'] = {
                'marketsearchsnippet': DEFAULT_DOWNLOADED_FULL_GENERATIONS
            }
        if 'active_generations' not in kwargs:
            kwargs['active_generations'] = {
                'marketsearchsnippet': '20180101_0300'
            }
        if 'httpsearch_status' not in kwargs:
            kwargs['httpsearch_status'] = HttpSearchStatus.UP
        if 'services' not in kwargs:
            kwargs['services'] = ['marketsearchsnippet']
        if 'packages' not in kwargs:
            kwargs['packages'] = {}
        if 'environment_type' not in kwargs:
            kwargs['environment_type'] = 'production'
        if 'generations_in_progress' not in kwargs:
            kwargs['generations_in_progress'] = ''

        super(DefaultSnippetHostState, self).__init__(**kwargs)


class DefaultSearchHostState(BaseSearchHostState):
    def __init__(self, **kwargs):
        kwargs = _add_default_host_state_arguments(kwargs)
        if 'httpsearch_status' not in kwargs:
            kwargs['httpsearch_status'] = HttpSearchStatus.UP
        if 'report_status' not in kwargs:
            kwargs['report_status'] = ReportStatus.OPENED_CONSISTENT
        if 'generations_in_progress' not in kwargs:
            kwargs['generations_in_progress'] = ''
        if 'report_cpu_usage' not in kwargs:
            kwargs['report_cpu_usage'] = 0.0
        if 'report_cpu_limit' not in kwargs:
            kwargs['report_cpu_limit'] = 0.0
        if 'last_rty_document_freshness' not in kwargs:
            kwargs['last_rty_document_freshness'] = 0
        if 'dynamic_data_timestamp' not in kwargs:
            kwargs['dynamic_data_timestamp'] = 0
        # if 'progress_report_states' not in kwargs:
        #     kwargs['progress_report_states'] = DEFAULT_PROGRESS_REPORT_STATES
        super(DefaultSearchHostState, self).__init__(**kwargs)


def add_cluster_to_group(cluster_id, group_state, group_config, datacenter=None):
    group_state['host0.cluster{}.fqdn'.format(cluster_id)] = DefaultSearchHostState(
        downloaded_generations={
            'marketsearch3': ['20180101_0000', '20180101_0300', '20180101_0600'],
        },
    )
    group_state['host1.cluster{}.fqdn'.format(cluster_id)] = DefaultSearchHostState(
        downloaded_generations={
            'marketsearch3': ['20180101_0000', '20180101_0300'],
        },
    )
    group_state['snippet_host.cluster{}.fqdn'.format(cluster_id)] = DefaultHostState(
        downloaded_generations={
            'marketsearchsnippet': ['20180101_0300', '20180101_0600'],
        },
        active_generations={
            'marketsearchsnippet': '20180101_0300',
        },
        services=['marketsearchsnippet'],
    )
    group_config.hosts.add(GroupConfig.Host(
        key='key:host0.cluster{}.fqdn'.format(cluster_id),
        fqdn='host0.cluster{}.fqdn'.format(cluster_id),
        port=9002,
        cluster_id=cluster_id,
        datacenter=datacenter,
    ))
    group_config.hosts.add(GroupConfig.Host(
        key='key:host1.cluster{}.fqdn'.format(cluster_id),
        fqdn='host1.cluster{}.fqdn'.format(cluster_id),
        port=9002,
        cluster_id=cluster_id,
        datacenter=datacenter,
    ))
    group_config.hosts.add(GroupConfig.Host(
        key='key:snippet_host.cluster{}.fqdn'.format(cluster_id),
        fqdn='snippet_host.cluster{}.fqdn'.format(cluster_id),
        port=9002,
        cluster_id=cluster_id,
        datacenter=datacenter,
    ))


@pytest.fixture()
def group():
    group_state = {}
    group_config = GroupConfig(
        simultaneous_restart=1,
        failures_threshold=1,
        hosts=[],
        reload_timeout=900,
        async_publishing=AsyncPublishingMode.enabled,
        min_alive={},
    )
    # good cluster
    add_cluster_to_group(0, group_state, group_config)

    # reloading cluster
    add_cluster_to_group(1, group_state, group_config)
    group_state['host0.cluster1.fqdn'] \
        .active_generations['marketsearch3'] = '20180101_0600'
    group_state['host0.cluster1.fqdn'] \
        .report_status = ReportStatus.CLOSED_INCONSISTENT_AUTO_OPENING
    group_state['host1.cluster1.fqdn'] \
        .downloaded_generations['marketsearch3'] \
        .append('20180101_0600')
    group_state['host1.cluster1.fqdn'].is_reloading = True
    group_state['host1.cluster1.fqdn'] \
        .report_status = ReportStatus.DOWN
    group_state['snippet_host.cluster1.fqdn'].is_reloading = True

    # cluster with dead report
    add_cluster_to_group(2, group_state, group_config)
    group_state['host1.cluster2.fqdn'] \
        .report_status = ReportStatus.DOWN

    return group_state, group_config
