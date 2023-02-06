# coding: utf-8

from mock import patch
import pytest
from market.idx.admin.mi_agent.lib.checkers import replication_lag_checker

MITYPE = 'gibson'
DATASOURCES = 'datasources'


class YtMock:
    def __init__(self, result_values, proxy, _):
        self.result_values_ = result_values
        self.proxy_ = proxy

    def get(self, path):
        result = self.result_values_[self.proxy_][path]
        return result


class ConfigMock:
    def __init__(self, datacamp_creation_threshold, datacamp_replication_threshold, yt_token_path, datacamp_table_path, datacamp_dyntable_path):
        self.datacamp_creation_threshold = datacamp_creation_threshold
        self.datacamp_replication_threshold = datacamp_replication_threshold
        self.yt_token_path = yt_token_path
        self.datacamp_table_path = datacamp_table_path
        self.datacamp_dyntable_path = datacamp_dyntable_path


@pytest.mark.parametrize(
    "test_data",
    [
        # check all, nothing is True
        {
            'config': ConfigMock(300, 300, 'some_path', ['{}/path1', '{}/path2'], ['{}/path1', '{}/path2']),
            'results': {
                'arnold': {
                    'production/production/path1/@table_data_timestamp': 1000,
                    'production/production/path2/@table_data_timestamp': 1000
                },
                'hahn': {
                    'production/production/path1/@table_data_timestamp': 1000,
                    'production/production/path2/@table_data_timestamp': 1000
                },
                'markov': {
                    'production/production/path1/@replicas': {
                        'replica1': {
                            'cluster_name': 'arnold',
                            'replication_lag_time': 0
                        },
                        'replica2': {
                            'cluster_name': 'hahn',
                            'replication_lag_time': 0
                        },
                    },
                    'production/production/path2/@replicas': {
                        'replica1': {
                            'cluster_name': 'arnold',
                            'replication_lag_time': 0
                        },
                        'replica2': {
                            'cluster_name': 'hahn',
                            'replication_lag_time': 0
                        },
                    },
                }
            },
            'expected': True
        },
        # check all, static is True
        {
            'config': ConfigMock(300, 300, 'some_path', ['{}/path1', '{}/path2'], ['{}/path1', '{}/path2']),
            'results': {
                'arnold': {
                    'production/path1/@table_data_timestamp': 1000,
                    'production/path2/@table_data_timestamp': 1000
                },
                'hahn': {
                    'production/path1/@table_data_timestamp': 1301,
                    'production/path2/@table_data_timestamp': 1301
                },
                'markov': {
                    'production/path1/@replicas': {
                        'replica1': {
                            'cluster_name': 'arnold',
                            'replication_lag_time': 0
                        },
                        'replica2': {
                            'cluster_name': 'hahn',
                            'replication_lag_time': 0
                        },
                    },
                    'production/path2/@replicas': {
                        'replica1': {
                            'cluster_name': 'arnold',
                            'replication_lag_time': 0
                        },
                        'replica2': {
                            'cluster_name': 'hahn',
                            'replication_lag_time': 0
                        },
                    },
                }
            },
            'expected': False
        },
        # check all, dynamic is True
        {
            'config': ConfigMock(300, 300, 'some_path', ['{}/path1', '{}/path2'], ['{}/path1', '{}/path2']),
            'results': {
                'arnold': {
                    'production/path1/@table_data_timestamp': 1000,
                    'production/path2/@table_data_timestamp': 1000
                },
                'hahn': {
                    'production/path1/@table_data_timestamp': 1000,
                    'production/path2/@table_data_timestamp': 1000
                },
                'markov': {
                    'production/path1/@replicas': {
                        'replica1': {
                            'cluster_name': 'arnold',
                            'replication_lag_time': 400
                        },
                        'replica2': {
                            'cluster_name': 'hahn',
                            'replication_lag_time': 0
                        },
                    },
                    'production/path2/@replicas': {
                        'replica1': {
                            'cluster_name': 'arnold',
                            'replication_lag_time': 400
                        },
                        'replica2': {
                            'cluster_name': 'hahn',
                            'replication_lag_time': 0
                        },
                    },
                }
            },
            'expected': False
        },
        # check bad result
        {
            'config': ConfigMock(300, 300, 'some_path', ['{}/path1', '{}/path2'], ['{}/path1', '{}/path2']),
            'results': {},
            'expected': True
        },
    ]
)
def test_replication_lag_checker(test_data):
    with patch('yt.wrapper.YtClient', side_effect=lambda proxy='', token='': YtMock(test_data['results'], proxy, token)):
        checker = replication_lag_checker.ReplicationLagChecker(MITYPE, test_data['config'])
        assert test_data['expected'] == checker.check()


def test_yt_falls():
    with patch('yt.wrapper.YtClient', side_effect=Exception('test_exception')):
        checker = replication_lag_checker.ReplicationLagChecker(MITYPE, ConfigMock(300, 300, 'some_path', ['{}/path1', '{}/path2'], ['{}/path1', '{}/path2']))
        assert checker.check()
