# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import mock
import pytest
from hamcrest import assert_that, contains_inanyorder

from travel.rasp.library.python.db.cluster import ClusterConst
from travel.rasp.library.python.common23.db.backends.mysql.base import (
    DatabaseWrapper, choose_cluster_instance_filter, inst_filter_any, inst_filter_master, inst_filter_replica
)

from travel.rasp.library.python.common23.tester.utils.replace_setting import replace_setting


def test_create_cluster():
    dbw = DatabaseWrapper({
        'HOST': 'somehost',  # should be ignored when HOSTS defined
        'CLUSTER': {
            'USE_MASTER': False,
            'USE_REPLICAS': True,
            'INSTANCE_DEAD_TTL': 42,
            'HOSTS': ['host1', 'host2', 'host3']
        }
    })

    assert_that(dbw.get_hosts(), contains_inanyorder('host2', 'host3'))  # first host is master -> should be ignored
    assert_that(dbw.get_all_hosts(), contains_inanyorder('host1', 'host2', 'host3'))

    cluster = dbw.get_cluster()
    assert isinstance(cluster, ClusterConst)
    assert cluster.instance_dead_ttl == 42
    assert cluster.connection_getter == dbw.get_connection_to_instance


def test_get_connection_to_host():
    with mock.patch('travel.rasp.library.python.common23.db.backends.mysql.base.connect', autospec=True) as m_connect:
        m_connect.get_connection.return_value = 42
        dw = DatabaseWrapper({'HOST': 'somehost'})
        assert dw.get_connection_to_host('somehost') == 42
        m_connect.get_connection.assert_called_once_with({'HOST': 'somehost'}, 'somehost')


def test_get_create_and_get_connection():
    dw = DatabaseWrapper({'HOST': 'somehost'})
    with mock.patch('travel.rasp.library.python.common23.db.backends.mysql.base.connect', autospec=True) as m_connect:
        m_connect.get_connection.return_value = 42
        assert dw.get_new_connection(None) == 42
        m_connect.get_connection.assert_called_once_with({'HOST': 'somehost'}, 'somehost')


def test_use_current_dc():
    with replace_setting('YANDEX_DATA_CENTER', 'mydc'):
        dw = DatabaseWrapper({'HOST': 'somehost'})
        assert dw.current_dc == 'mydc'
        with mock.patch.object(dw.cluster, 'get_connection', autospec=True) as m_get_connection:
            dw.get_new_connection(None)
            m_get_connection.assert_called_once_with(current_dc='mydc')


@pytest.mark.parametrize('use_replicas, use_master, expected', (
    (True, True, inst_filter_any),
    (False, True, inst_filter_master),
    (True, False, inst_filter_replica),
))
def tests_choose_cluster_instance_filter2(use_replicas, use_master, expected):
    assert expected == choose_cluster_instance_filter(use_replicas, use_master)
