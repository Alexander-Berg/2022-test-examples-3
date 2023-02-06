# coding: utf8
from __future__ import absolute_import, division, print_function, unicode_literals

import mock

from travel.library.python.avia_mdb_replica_info.avia_mdb_replica_info.containers import ClusterInfo, Replica
from travel.library.python.avia_mdb_replica_info.avia_mdb_replica_info.mdb_api import MdbAPI

from travel.rasp.library.python.db.cluster import DbInstance
from travel.rasp.library.python.db.cluster_mdb import ClusterMdb, ClusterMdbMysql
from travel.rasp.library.python.db.mysql import utils as mysql_utils
from travel.rasp.library.python.db.replica_health import ReplicaHealth


from travel.rasp.library.python.db.tests.factories import assert_instances_lists_equal, assert_db_instances_equal


class TestClusterMdb(object):
    def test_get_actual_instances_list(self):
        cluster_id = 'cluster42'

        mock_mdb_api = mock.Mock()
        mock_mdb_api.get_cluster_info.side_effect = [
            Exception('no connection to mdb api'),
            ClusterInfo(cluster_id, [Replica('host2', dc='vla', is_master=True), Replica('host1', dc='sas', is_master=False)]),
            ClusterInfo(cluster_id, [Replica('host2', dc='vla', is_master=False), Replica('host1', dc='sas', is_master=False)]),
        ]

        cluster = ClusterMdb(mdb_client=mock_mdb_api, cluster_id='cluster42')

        result = cluster.get_instances()
        assert_instances_lists_equal(result, [
            DbInstance('c-cluster42.ro.db.yandex.net', is_master=False),
            DbInstance('c-cluster42.rw.db.yandex.net', is_master=True),
        ])

        cluster.update_cluster_configuration()
        result = cluster.get_instances(current_dc='sas')
        assert_db_instances_equal(result[1], DbInstance('host2', is_master=True, dc='vla', priority=0))
        assert_db_instances_equal(result[0], DbInstance('host1', is_master=False, dc='sas', priority=0))
        assert_instances_lists_equal(result[2:], [
            DbInstance('c-cluster42.ro.db.yandex.net', is_master=False),
        ])

        cluster.update_cluster_configuration()
        result = cluster.get_instances(current_dc='sas')
        assert_db_instances_equal(result[1], DbInstance('host2', is_master=False, dc='vla', priority=0))
        assert_db_instances_equal(result[0], DbInstance('host1', is_master=False, dc='sas', priority=0))
        assert_instances_lists_equal(result[2:], [
            DbInstance('c-cluster42.ro.db.yandex.net', is_master=False),
            DbInstance('c-cluster42.rw.db.yandex.net', is_master=True),
        ])

    def test_fallback(self):
        with mock.patch.object(MdbAPI, '_request_cluster_info') as m_request_cluster_info, \
             mock.patch.object(MdbAPI, '_load_cached_response') as m_load_cached_response:

            m_request_cluster_info.side_effect = Exception('api is broken')
            m_load_cached_response.side_effect = Exception('cache is broken')

            mdb_api = MdbAPI('', '')
            cluster = ClusterMdb(
                mdb_client=mdb_api,
                cluster_id='cluster42',
                fallback_master='vla.host1',
                fallback_replicas=['man.host3', 'iva.host2'],
            )
            result = cluster.get_instances(current_dc='iva')
            assert_db_instances_equal(result[0], DbInstance('iva.host2', is_master=False, dc='iva', priority=0))
            assert_db_instances_equal(result[1], DbInstance('vla.host1', is_master=True, dc='vla', priority=0))
            assert_db_instances_equal(result[2], DbInstance('man.host3', is_master=False, dc='man', priority=0))
            assert_instances_lists_equal(result[3:], [
                DbInstance('c-cluster42.ro.db.yandex.net', is_master=False),
            ])

            # only master for fallback
            mdb_api = MdbAPI('', '')
            cluster = ClusterMdb(
                mdb_client=mdb_api,
                cluster_id='cluster42',
                fallback_master='vla.host1',
            )
            result = cluster.get_instances(current_dc='iva')
            assert_db_instances_equal(result[0], DbInstance('vla.host1', is_master=True, dc='vla', priority=0))
            assert_db_instances_equal(result[1], DbInstance('c-cluster42.ro.db.yandex.net', is_master=False))

            # test set_fallback_hosts works
            cluster.set_fallback_hosts('man.host3', ['man.host3', 'iva.host2'])
            cluster.update_cluster_configuration()
            result = cluster.get_instances(current_dc='iva')
            assert_db_instances_equal(result[0], DbInstance('iva.host2', is_master=False, dc='iva', priority=0))
            assert_db_instances_equal(result[1], DbInstance('man.host3', is_master=True, dc='man', priority=0))

    def test_fallback_no_api_call(self):
        mdb_api = MdbAPI('', '')
        cluster = ClusterMdb(
            mdb_client=mdb_api,
            mdb_api_call_enabled=False,
            cluster_id='cluster42',
            fallback_master='vla.host1',
            fallback_replicas=['man.host3', 'iva.host2'],
        )
        result = cluster.get_instances(current_dc='iva')
        assert_db_instances_equal(result[0], DbInstance('iva.host2', is_master=False, dc='iva', priority=0))
        assert_db_instances_equal(result[1], DbInstance('vla.host1', is_master=True, dc='vla', priority=0))
        assert_db_instances_equal(result[2], DbInstance('man.host3', is_master=False, dc='man', priority=0))
        assert_instances_lists_equal(result[3:], [
            DbInstance('c-cluster42.ro.db.yandex.net', is_master=False),
        ])

    def test_set_replica_health(self):
        mdb_api = MdbAPI('', '')
        cluster = ClusterMdb(
            mdb_client=mdb_api,
            mdb_api_call_enabled=False,
            cluster_id='cluster42',
            fallback_master='vla.host1',
            fallback_replicas=['man.host3', 'iva.host2'],
        )

        cluster.replicas_health = {
            'vla.host1': ReplicaHealth.DEAD,
            'man.host3': ReplicaHealth.ALIVE,
            'iva.host2': ReplicaHealth.ALIVE,
        }

        result = cluster.get_instances(current_dc='vla')
        assert_db_instances_equal(result[0], DbInstance('iva.host2', is_master=False, dc='iva', priority=0,
                                                        health=ReplicaHealth.ALIVE))
        assert_db_instances_equal(result[1], DbInstance('man.host3', is_master=False, dc='man', priority=0,
                                                        health=ReplicaHealth.ALIVE))
        assert_instances_lists_equal(result[2:], [
            DbInstance('c-cluster42.ro.db.yandex.net', is_master=False),
            DbInstance('vla.host1', is_master=True, dc='vla', priority=0, health=ReplicaHealth.DEAD)
        ])

    def test_get_connection_master_change(self):
        class ClusterWithMasterCheck(ClusterMdb):
            def find_master_host(self):
                return 'host1'

        mock_mdb_api = mock.Mock()
        mock_mdb_api.get_cluster_info.return_value = ClusterInfo(
            'cluster42', [Replica('host2', dc='vla', is_master=True), Replica('host1', dc='sas', is_master=False)]
        )
        m_connection_getter = mock.Mock(side_effect=lambda i: 'connection_to_{}'.format(i.host))

        # без авто-проверки мастера считаем, что мастер host2
        cluster = ClusterWithMasterCheck(
            mdb_client=mock_mdb_api,
            cluster_id='cluster42',
            connection_getter=m_connection_getter,
            instance_filter=lambda inst: inst.is_master
        )
        assert cluster.get_connection() == 'connection_to_host2'

        # с проверкой мастера узнаём, что мастер на самом деле host1
        cluster = ClusterWithMasterCheck(
            mdb_client=mock_mdb_api,
            cluster_id='cluster42',
            connection_getter=m_connection_getter,
            instance_filter=lambda inst: inst.is_master,
            check_master_on_each_connect=True,
        )
        assert cluster.get_connection() == 'connection_to_host1'

    def test_set_master_instance_by_host(self):
        mock_mdb_api = mock.Mock()
        mock_mdb_api.get_cluster_info.return_value = ClusterInfo(
            'cluster42', [Replica('host2', dc='vla', is_master=True), Replica('host1', dc='sas', is_master=False)]
        )
        cluster = ClusterMdb(mdb_client=mock_mdb_api, cluster_id='cluster42')

        assert_db_instances_equal(cluster.instances[0], DbInstance(host='host2', dc='vla', is_master=True, priority=0))
        assert_db_instances_equal(cluster.instances[1], DbInstance(host='host1', dc='sas', is_master=False, priority=0))

        cluster.set_master_instance_by_host('host1')
        assert_db_instances_equal(cluster.instances[0], DbInstance(host='host2', dc='vla', is_master=False, priority=0))
        assert_db_instances_equal(cluster.instances[1], DbInstance(host='host1', dc='sas', is_master=True, priority=0))


class TestClusterMdbMysql(object):
    def test_find_master_host(self):
        mock_mdb_api = mock.Mock()
        mock_mdb_api.get_cluster_info.return_value = ClusterInfo(
            'cluster42', [Replica('host2', dc='vla', is_master=True), Replica('host1', dc='sas', is_master=False)]
        )
        cluster = ClusterMdbMysql(mdb_client=mock_mdb_api, cluster_id='cluster42')

        # возвращается конкретный хост из get_master_host_from_conn -> нашли мастера
        with mock.patch.object(mysql_utils, 'get_master_host_from_conn', autospec=True) as m_get_master_host_from_conn, \
             mock.patch.object(ClusterMdb, 'get_connection_to_instance', autospec=True) as m_get_connection_to_instance:

            m_get_connection_to_instance.side_effect = [
                Exception("something went wrong"),
                mock.sentinel.conn1,
            ]

            m_get_master_host_from_conn.return_value = 'host43'
            master_host = cluster.find_master_host()
            m_get_master_host_from_conn.assert_called_once_with(mock.sentinel.conn1)
            assert master_host == 'host43'

        # не возвращается хост из get_master_host_from_conn -> значит текущий инстанс и есть мастер
        with mock.patch.object(mysql_utils, 'get_master_host_from_conn', autospec=True) as m_get_master_host_from_conn, \
             mock.patch.object(ClusterMdb, 'get_connection_to_instance', autospec=True) as m_get_connection_to_instance:

            m_get_connection_to_instance.side_effect = [
                Exception("something went wrong"),
                mock.sentinel.conn1,
            ]

            m_get_master_host_from_conn.return_value = None
            master_host = cluster.find_master_host()
            m_get_master_host_from_conn.assert_called_once_with(mock.sentinel.conn1)
            assert master_host == 'host1'
