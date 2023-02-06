#!/usr/bin/env python
# -*- coding: utf-8 -*-

import copy
import mock
import os
import pytest
import yatest
from kazoo.client import KazooClient
from market.pylibrary.graphite.graphite import DummyGraphite
from health_monitoring import (
    _closed_for_roles,
    _old_generations_for_index_types,
    _unloaded_generations_for_index_types,
    send_data_to_graphite,
    make_zk_client,
    _version_diversity,
    _make_log_entry,
    Cluster,
    IndexType,
)

from market.pylibrary.mindexerlib.util import now


REPORT_STATE = {
    'clusters': [
        {
            'status': status,
            'backctld': backctld,
            'backctld_diff': backctld_diff,
            'group': [
                'prod_report_{}_iva'.format(role),
                'prod_report_{}_snippet_iva'.format(role),
            ],
            'generation': generation,
            'diff_generation': diff_generation,
            'version': '1',
            'formulas_version': '2',
            'debversion': '3',
            'dssm_version': '4',
            'dynamic_data_version': '100' if role == 'market' else '200',
            'dynamic_data_timestamp': '0',
            'group_type': [
                'nanny_groups',
                'snippet_nanny_groups'
            ],
            'cluster_id': role + '@iva@0',
            'color': 'green' if role == 'market' else 'gray',
            'nanny_current_state': 'ACTIVE' if role == 'market' else 'UNKNOWN',
        }
        for status in [
            'OPENED_CONSISTENT',
            'CLOSED_CONSISTENT_MANUAL_OPENING',
            'CLOSED_INCONSISTENT_MANUAL_OPENING',
            'CLOSED_INCONSISTENT_AUTO_OPENING',
        ]

        for backctld, backctld_diff in [
            ('ok', 'ok'),
            ('! in progress', 'ok'),
            ('ok', '! in progress'),
        ]
        for role in ['api', 'market']
        for generation, diff_generation in [
            ('20180101_0505', '20180101_1010'),
            ('20180101_0101', '20180101_0505'),
            ('20180101_1515', '20180101_2020'),
        ]
    ],
    'hosts': [
        {
            'status': status,
            'group': 'prod_report_{}_iva'.format(role),
            'async_publisher_state': {
                'downloaded_generations': {
                    'marketsearch3': generations
                },
            },
        }
        for status in [
            'OPENED_CONSISTENT',
            'CLOSED_CONSISTENT_MANUAL_OPENING',
            'CLOSED_INCONSISTENT_MANUAL_OPENING',
            'CLOSED_INCONSISTENT_AUTO_OPENING',
        ]
        for role in ['api', 'market']
        for generations in [['20180101_1010', '20180101_1000'], ['20180101_1010'], ['20180101_100']]
    ]
}

REPORT_STATE_VERSIONS = {
    'clusters': [
        {
            'status': status,
            'group': [
                'prod_report_{}_iva'.format(role),
                'prod_report_{}_snippet_iva'.format(role),
            ],
            'version': '1' if role == 'api' else version,

        }
        for status in [
            'OPENED_CONSISTENT',
            'CLOSED_CONSISTENT_MANUAL_OPENING'
        ]

        for role in ['api', 'market']
        for version in ['1', '2', '3']
    ]
}


def test_closed():
    closed_for_role = _closed_for_roles(REPORT_STATE)
    assert closed_for_role['market'] == 9
    assert closed_for_role['api'] == 9


def test_versions():
    versions_for_role = _version_diversity(REPORT_STATE_VERSIONS, lambda c: c.report_version,)
    assert versions_for_role['market'] == {'1', '2', '3'}
    assert versions_for_role['api'] == {'1'}


def test_old_generations():
    old_generations = _old_generations_for_index_types(
        REPORT_STATE,
        {IndexType.MARKET: '20180101_1010'}
    )
    assert old_generations[IndexType.MARKET]['market'] == 6
    assert old_generations[IndexType.MARKET]['api'] == 6


def test_unloaded_generations():
    unloaded_generations = _unloaded_generations_for_index_types(
        REPORT_STATE,
        {IndexType.MARKET: '20180101_1010'}
    )
    assert unloaded_generations[IndexType.MARKET]['market'] == 1
    assert unloaded_generations[IndexType.MARKET]['api'] == 1


def test_send_data():
    zk_client = mock.create_autospec(KazooClient, instance=True)
    zk_client.get.return_value = '20180101_1010', None
    send_data_to_graphite(
        REPORT_STATE,
        graphite_cls=DummyGraphite,
        zk_client=zk_client,
    )


def test_make_zk_client():
    zk_config_path = yatest.common.source_path('market/pinger-report/tests/zookeeper.conf')
    os.environ['ZK_CONFIG_PATH'] = zk_config_path
    make_zk_client(mock.create_autospec(KazooClient, instance=False))


@pytest.mark.parametrize("role, expected_log_entry", [
    ('api', {
        'total_clusters': 36,
        'closed_clusters': 27,
        'different_debversions': 1,
        'different_diff_generations': 3,
        'red_clusters': 0,
        'gray_clusters': 36,
        'role': 'api',
        'different_dssm_versions': 1,
        'different_dynamic_data_versions': 1,
        'different_formula_versions': 1,
        'different_full_generations': 3,
        'different_report_versions': 1
    }),
    ('market', {
        'total_clusters': 36,
        'closed_clusters': 27,
        'different_debversions': 1,
        'different_diff_generations': 3,
        'red_clusters': 9,
        'gray_clusters': 0,
        'role': 'market',
        'different_dssm_versions': 1,
        'different_dynamic_data_versions': 1,
        'different_formula_versions': 1,
        'different_full_generations': 3,
        'different_report_versions': 1
    }),
], ids=['api', 'market'])
def test_tskv_log_generation(role, expected_log_entry):
    clusters = [Cluster(cluster_json) for cluster_json in REPORT_STATE['clusters']]
    filtered_clusters = [c for c in clusters if c.role == role]
    log_entry = _make_log_entry(filtered_clusters, role)

    assert 'date' in log_entry
    del log_entry['date']
    del log_entry['dynamic_data_freshness']

    assert log_entry == expected_log_entry


def test_tskv_log_generation_all():
    clusters = [Cluster(cluster_json) for cluster_json in REPORT_STATE['clusters']]
    log_entry = _make_log_entry(clusters, 'all')

    assert 'date' in log_entry
    del log_entry['date']
    del log_entry['dynamic_data_freshness']

    assert log_entry == {
        'total_clusters': 72,
        'closed_clusters': 54,
        'different_debversions': 1,
        'different_diff_generations': 3,
        'red_clusters': 9,
        'gray_clusters': 36,
        'role': 'all',
        'different_dssm_versions': 1,
        'different_formula_versions': 1,
        'different_full_generations': 3,
        'different_report_versions': 1,
        'different_dynamic_data_versions': 2,
    }


def test_tskv_log_with_indigo():
    repors_state_with_indigo = {
        'clusters': [
            # indigo
            {
                'status': 'OPENED_CONSISTENT',
                'backctld': 'ok',
                'backctld_diff': 'ok',
                'group': [
                    'prod_report_{}_iva'.format('market'),
                    'prod_report_{}_snippet_iva'.format('market'),
                ],
                'generation': '20180101_0505',
                'diff_generation': '20180101_1010',
                'version': '1',
                'formulas_version': '2',
                'debversion': '3',
                'dssm_version': '4',
                'dynamic_data_version': '200',
                'dynamic_data_timestamp': '0',
                'group_type': [
                    'nanny_groups',
                    'snippet_nanny_groups'
                ],
                'cluster_id': 'market' + '@iva@0',
                'color': 'indigo',
                'nanny_current_state': 'ACTIVE',
            },
            # opened
            {
                'status': 'OPENED_CONSISTENT',
                'backctld': 'ok',
                'backctld_diff': 'ok',
                'group': [
                    'prod_report_{}_iva'.format('market'),
                    'prod_report_{}_snippet_iva'.format('market'),
                ],
                'generation': '20180101_0505',
                'diff_generation': '20180101_1010',
                'version': '1',
                'formulas_version': '2',
                'debversion': '3',
                'dssm_version': '4',
                'dynamic_data_version': '100',
                'dynamic_data_timestamp': '0',
                'group_type': [
                    'nanny_groups',
                    'snippet_nanny_groups'
                ],
                'cluster_id': 'market' + '@iva@0',
                'color': 'green',
                'nanny_current_state': 'ACTIVE',
            },
            # closed, ACTIVE & not in progress
            {
                'status': 'CLOSED_CONSISTENT_MANUAL_OPENING',
                'backctld': 'ok',
                'backctld_diff': 'ok',
                'group': [
                    'prod_report_{}_iva'.format('market'),
                    'prod_report_{}_snippet_iva'.format('market'),
                ],
                'generation': '20180101_0505',
                'diff_generation': '20180101_1010',
                'version': '1',
                'formulas_version': '2',
                'debversion': '3',
                'dssm_version': '4',
                'dynamic_data_version': '0',
                'dynamic_data_timestamp': '0',
                'group_type': [
                    'nanny_groups',
                    'snippet_nanny_groups'
                ],
                'cluster_id': 'market' + '@iva@0',
                'color': 'indigo',
                'nanny_current_state': 'ACTIVE',
            }
        ]
    }

    clusters = [Cluster(cluster_json) for cluster_json in
                repors_state_with_indigo['clusters']]
    log_entry = _make_log_entry(clusters, 'all')

    assert 'date' in log_entry
    del log_entry['date']
    del log_entry['dynamic_data_freshness']

    assert log_entry == {
        'total_clusters': 3,
        'closed_clusters': 1,
        'different_debversions': 1,
        'different_diff_generations': 1,
        'red_clusters': 2,
        'gray_clusters': 0,
        'role': 'all',
        'different_dssm_versions': 1,
        'different_formula_versions': 1,
        'different_full_generations': 1,
        'different_report_versions': 1,
        'different_dynamic_data_versions': 1,
    }


def test_tskv_log_with_different_report_versions():
    cluster_state = {
        'status': 'OPENED_CONSISTENT',
        'backctld': 'ok',
        'backctld_diff': 'ok',
        'group': [
            'prod_report_{}_iva'.format('market'),
            'prod_report_{}_snippet_iva'.format('market'),
        ],
        'generation': '20180101_0505',
        'diff_generation': '20180101_1010',
        'formulas_version': '2',
        'debversion': '3',
        'dssm_version': '4',
        'dynamic_data_version': '100',
        'dynamic_data_timestamp': '0',
        'group_type': [
            'nanny_groups',
            'snippet_nanny_groups'
        ],
        'cluster_id': 'market' + '@iva@0',
        'color': 'green',
        'nanny_current_state': 'ACTIVE',
    }

    reports_state = {
        'clusters': [copy.deepcopy(cluster_state) for _ in range(3)]
    }

    reports_state['clusters'][0]['version'] = None
    reports_state['clusters'][1]['version'] = '2020.1.11.0'
    reports_state['clusters'][2]['version'] = '"None":1, "2020.1.9.0":7'

    clusters = [
        Cluster(cluster_json)
        for cluster_json in reports_state['clusters']
    ]
    log_entry = _make_log_entry(clusters, 'all')

    assert 'date' in log_entry
    del log_entry['date']
    del log_entry['dynamic_data_freshness']

    assert log_entry == {
        'total_clusters': 3,
        'closed_clusters': 0,
        'different_debversions': 1,
        'different_diff_generations': 1,
        'red_clusters': 0,
        'gray_clusters': 0,
        'role': 'all',
        'different_dssm_versions': 1,
        'different_formula_versions': 1,
        'different_full_generations': 1,
        'different_report_versions': 3,
        'different_dynamic_data_versions': 1,
    }


def test_tskv_log_with_dynamic_freshness():
    cluster_state = {
        'status': 'OPENED_CONSISTENT',
        'backctld': 'ok',
        'backctld_diff': 'ok',
        'group': [
            'prod_report_{}_iva'.format('market'),
            'prod_report_{}_snippet_iva'.format('market'),
        ],
        'version': '2020.1.11.0',
        'generation': '20180101_0505',
        'diff_generation': '20180101_1010',
        'formulas_version': '2',
        'debversion': '3',
        'dssm_version': '4',
        'dynamic_data_version': '100',
        'group_type': [
            'nanny_groups',
            'snippet_nanny_groups'
        ],
        'cluster_id': 'market' + '@iva@0',
        'color': 'green',
        'nanny_current_state': 'ACTIVE',
    }

    reports_state = {
        'clusters': [copy.deepcopy(cluster_state) for _ in range(4)]
    }

    min_ts = 1000
    current_ts = now()
    reports_state['clusters'][0]['dynamic_data_timestamp'] = None
    reports_state['clusters'][1]['dynamic_data_timestamp'] = str(min_ts)
    reports_state['clusters'][2]['dynamic_data_timestamp'] = '"None":1, "5000":1, "{}":6'.format(current_ts)
    reports_state['clusters'][3]['dynamic_data_timestamp'] = 0
    reports_state['clusters'][3]['color'] = 'indigo'

    clusters = [
        Cluster(cluster_json)
        for cluster_json in reports_state['clusters']
    ]
    log_entry = _make_log_entry(clusters, 'all')

    assert current_ts - min_ts <= log_entry['dynamic_data_freshness'] <= now() - min_ts


@pytest.mark.parametrize('group, expected_role', [
    ('prep_report_market_vla', 'market'),
    ('prep_report_parallel_vla', 'parallel'),
    ('prep_report_api_sas', 'api'),
    ('prod_report_mbo_man', 'mbo'),
    ('prod_report_shadow_sas', 'shadow'),
    ('prod_report_blue_kraken_vla', 'blue_kraken'),
    ('prod_report_blue_shadow_vla', 'blue_shadow'),
    ('prod_report_blue_market_man', 'blue'),
])
def test_cluster_property_group(group, expected_role):
    cluster = Cluster({'group': [group]})
    assert expected_role == cluster.role
