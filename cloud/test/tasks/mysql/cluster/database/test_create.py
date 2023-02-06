"""
MySQL database create tests
"""

from test.mocks import checked_run_task_with_mocks
from test.tasks.mysql.utils import get_mysql_compute_host, get_mysql_porto_host
from test.tasks.utils import check_mlock_usage, check_task_interrupt_consistency


def test_porto_mysql_database_create_interrupt_consistency(mocker):
    """
    Check porto database create interruptions
    """
    args = {
        'hosts': {
            'host1': get_mysql_porto_host(geo='geo1'),
            'host2': get_mysql_porto_host(geo='geo2'),
            'host3': get_mysql_porto_host(geo='geo3'),
        },
        'zk_hosts': 'test-zk',
    }
    *_, state = checked_run_task_with_mocks(mocker, 'mysql_cluster_create', dict(**args, s3_bucket='test-s3-bucket'))

    args['target-database'] = 'test-database'

    state['zookeeper']['mysql'] = {'cid-test': {'master': '"host3"'}}

    check_task_interrupt_consistency(
        mocker,
        'mysql_database_create',
        args,
        state,
    )


def test_compute_mysql_database_create_interrupt_consistency(mocker):
    """
    Check compute database create interruptions
    """
    args = {
        'hosts': {
            'host1': get_mysql_compute_host(geo='geo1'),
            'host2': get_mysql_compute_host(geo='geo2'),
            'host3': get_mysql_compute_host(geo='geo3'),
        },
        'zk_hosts': 'test-zk',
    }
    *_, state = checked_run_task_with_mocks(mocker, 'mysql_cluster_create', dict(**args, s3_bucket='test-s3-bucket'))

    args['target-database'] = 'test-database'

    state['zookeeper']['mysql'] = {'cid-test': {'master': '"host3"'}}

    check_task_interrupt_consistency(
        mocker,
        'mysql_database_create',
        args,
        state,
    )


def test_mysql_database_create_mlock_usage(mocker):
    """
    Check database create mlock usage
    """
    args = {
        'hosts': {
            'host1': get_mysql_porto_host(geo='geo1'),
            'host2': get_mysql_porto_host(geo='geo2'),
            'host3': get_mysql_porto_host(geo='geo3'),
        },
        'zk_hosts': 'test-zk',
    }
    *_, state = checked_run_task_with_mocks(mocker, 'mysql_cluster_create', dict(**args, s3_bucket='test-s3-bucket'))

    args['target-database'] = 'test-database'

    state['zookeeper']['mysql'] = {'cid-test': {'master': '"host3"'}}

    check_mlock_usage(
        mocker,
        'mysql_database_create',
        args,
        state,
    )