# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from builtins import range
import json
import mock
from datetime import datetime
from io import BytesIO
from django.utils.encoding import force_bytes

import freezegun

from travel.library.python.avia_mdb_replica_info.avia_mdb_replica_info import ClusterInfo, Replica
from travel.rasp.library.python.db.replica_health import ReplicaHealth

from travel.rasp.library.python.common23.data_api.mdb.instance import mdb_mysql_api
from travel.rasp.library.python.common23.db.backends.cluster_mdb.base import DatabaseWrapper, ClusterMdbMysqlWithFallbackUpdate
from travel.rasp.library.python.common23.db.backends.cluster_mdb import fallback_hosts_mds
from travel.rasp.library.python.common23.db.backends.cluster_mdb import base as cluster_mdb_base
from travel.rasp.library.python.common23.db.backends.cluster_mdb.fallback_hosts_mds import FallbackHostsStorage, update_mdb_cluster_info_in_mds, update_mdb_clusters_info_in_mds
from travel.rasp.library.python.common23.db.mds.base import MDSS3Wrapper
from travel.rasp.library.python.common23.db.replica_checker import DefaultReplicaChecker

from travel.rasp.library.python.common23.tester.utils.replace_setting import replace_setting


def test_create_cluster():
    dbw = DatabaseWrapper({
        'CLUSTER': {
            'CLUSTER_ID': 'mdb_cluster_id_42',
            'CLUSTER_INFO_TTL': 430,
            'MDB_API_CALL_ENABLED': False,
            'CHECK_MASTER_ON_EACH_CONNECT': True,
            'FALLBACK': {
                'MASTER': 'vla1',
                'REPLICAS': ['sas1', 'iva2'],
            }
        }
    })

    cluster = dbw.get_cluster()
    assert isinstance(cluster, ClusterMdbMysqlWithFallbackUpdate)
    assert cluster.cluster_id == 'mdb_cluster_id_42'
    assert cluster.mdb_client is mdb_mysql_api
    assert cluster.mdb_api_call_enabled is False
    assert cluster.check_master_on_each_connect is True
    assert cluster.cluster_info_ttl == 430
    assert cluster.fallback_master == 'vla1'
    assert cluster.fallback_replicas == ['sas1', 'iva2']


class TestFallbackHostsMds(object):
    def _get_stream(self, data):
        data_str = force_bytes(json.dumps(data, indent=4))

        stream = BytesIO()
        stream.write(data_str)
        stream.seek(0)
        return stream

    @replace_setting('MDS_PATH_MDB_CLUSTER_INFO', 'test/mds/path/')
    def test_fallback_hosts_storage_get(self):
        fbs = FallbackHostsStorage('cluster_id_42')

        with mock.patch.object(MDSS3Wrapper, 'get_data') as m_mds_get_data:
            m_mds_get_data.return_value = self._get_stream({
                "master": "host2.db.yandex.net",
                "hosts": [
                    "host1.db.yandex.net",
                    "host2.db.yandex.net",
                ],
            })

            result = fbs.get_cluster_info()
            assert result == {
                "master": "host2.db.yandex.net",
                "hosts": [
                    "host1.db.yandex.net",
                    "host2.db.yandex.net",
                ],
            }

            m_mds_get_data.assert_called_once_with('test/mds/path/cluster_id_42')

    @replace_setting('MDS_PATH_MDB_CLUSTER_INFO', 'test/mds/path/')
    def test_fallback_hosts_storage_set(self):
        fbs = FallbackHostsStorage('cluster_id_42')

        data = {
            "master": "host2.db.yandex.net",
            "hosts": [
                "host1.db.yandex.net",
                "host2.db.yandex.net",
            ],
        }

        with mock.patch.object(MDSS3Wrapper, 'save_data') as m_mds_save_data:
            fbs.set_cluster_info(data)
            expected_data_str = json.dumps(data, indent=4)
            m_mds_save_data.assert_called_once_with('test/mds/path/cluster_id_42', expected_data_str)

    def test_update_mdb_cluser_info_in_mds(self):
        with mock.patch.object(fallback_hosts_mds, 'mdb_mysql_api') as m_mdb_mysql_api, \
            mock.patch.object(fallback_hosts_mds, 'FallbackHostsStorage') as m_fallback_storage, \
                freezegun.freeze_time(datetime(2020, 10, 20, 23, 42, 43, 114)):
            m_set_cluster_info = mock.Mock()
            m_fallback_storage.return_value = mock.Mock(set_cluster_info=m_set_cluster_info)

            m_mdb_mysql_api.get_cluster_info.return_value = ClusterInfo(
                cluster_id='cluster_id_42',
                instances=[
                    Replica(
                        hostname='host{}'.format(i),
                        dc='sas',
                        is_master=(i == 2),
                        raw_data={'health': ReplicaHealth.ALIVE if i != 0 else ReplicaHealth.SUSPENDED})
                    for i in range(3)
                ]
            )

            update_mdb_cluster_info_in_mds('cluster_id_42', 'cluster_name_42', DefaultReplicaChecker())

            m_mdb_mysql_api.get_cluster_info.assert_called_once_with('cluster_id_42')

            m_fallback_storage.assert_called_once_with('cluster_id_42')
            m_set_cluster_info.assert_called_once_with({
                'updated': '2020-10-20T23:42:43.000114',
                'hosts': ['host0', 'host1', 'host2'],
                'master': 'host2',
                'replicas_health': {
                    'host0': ReplicaHealth.SUSPENDED,
                    'host1': ReplicaHealth.ALIVE,
                    'host2': ReplicaHealth.ALIVE
                },
                'cluster_id': 'cluster_id_42',
                'cluster_name': 'cluster_name_42'
            })

    def test_update_mdb_clusers_info_in_mds(self):
        replica_checker = DefaultReplicaChecker()

        clusters = (
            {"cluster_id": "cluster_id_43", "cluster_name": "cluster_name_43", "replica_checker": replica_checker},
            {"cluster_id": "cluster_id_42", "cluster_name": "cluster_name_42", "replica_checker": replica_checker},
            {"cluster_id": "cluster_id_44", "cluster_name": "cluster_name_44", "replica_checker": replica_checker},
        )

        with mock.patch.object(fallback_hosts_mds, 'update_mdb_cluster_info_in_mds',
                               autospec=True) as m_update_mdb_cluser_info_in_mds:
            # результат вызова нам не важен, но важно, чтобы при ошибке обновления одного кластера весь вызов не упал
            m_update_mdb_cluser_info_in_mds.side_effect = [
                'ok',
                Exception,
                'ok',
            ]

            update_mdb_clusters_info_in_mds(clusters)

            calls = m_update_mdb_cluser_info_in_mds.call_args_list
            assert calls[0] == mock.call("cluster_id_43", "cluster_name_43", replica_checker)
            assert calls[1] == mock.call("cluster_id_42", "cluster_name_42", replica_checker)
            assert calls[2] == mock.call("cluster_id_44", "cluster_name_44", replica_checker)


class TestClusterMdbMysqlWithFallbackUpdate(object):
    def test_get_new_fallback_hosts(self):
        cluster = ClusterMdbMysqlWithFallbackUpdate(
            mdb_client=None,
            cluster_id='cluster_id_42',
            mdb_api_call_enabled=False,
        )

        with mock.patch.object(cluster_mdb_base, 'FallbackHostsStorage') as m_fallback_storage:
            m_get_cluster_info = mock.Mock()
            m_fallback_storage.return_value = mock.Mock(get_cluster_info=m_get_cluster_info)
            m_get_cluster_info.return_value = {
                'hosts': ['host0', 'host1', 'host2'],
                'master': 'host2',
                'replicas_health': {
                    'host0': ReplicaHealth.ALIVE,
                    'host1': ReplicaHealth.DEAD,
                    'host2': ReplicaHealth.ALIVE
                }
            }

            master_host, all_hosts, replicas_health = cluster.get_new_fallback_hosts()

            assert master_host == 'host2'
            assert all_hosts == ['host0', 'host1', 'host2']
            assert replicas_health == {
                'host0': ReplicaHealth.ALIVE,
                'host1': ReplicaHealth.DEAD,
                'host2': ReplicaHealth.ALIVE
            }

            m_fallback_storage.assert_called_once_with('cluster_id_42')
            m_get_cluster_info.assert_called_once_with()

    def test_update_fallback_hosts(self):
        cluster = ClusterMdbMysqlWithFallbackUpdate(
            mdb_client=mock.Mock(),
            cluster_id='cluster_id_42',
            mdb_api_call_enabled=False,
            fallback_master='vla.host1',
            fallback_replicas=['man.host3', 'iva.host2'],
        )

        with mock.patch.object(cluster_mdb_base, 'FallbackHostsStorage') as m_fallback_storage, \
                mock.patch.object(ClusterMdbMysqlWithFallbackUpdate, 'set_fallback_hosts') as m_set_fallback_hosts, \
                mock.patch.object(ClusterMdbMysqlWithFallbackUpdate, 'set_replicas_health') as m_set_replicas_health:

            m_get_cluster_info = mock.Mock()
            m_fallback_storage.return_value = mock.Mock(get_cluster_info=m_get_cluster_info)
            m_get_cluster_info.return_value = {
                'hosts': ['host0', 'host1', 'host2'],
                'master': 'host2',
                'replicas_health': {
                    'host0': ReplicaHealth.ALIVE,
                    'host1': ReplicaHealth.DEAD,
                    'host2': ReplicaHealth.ALIVE
                }
            }

            cluster.update_fallback_hosts()

            m_fallback_storage.assert_called_once_with('cluster_id_42')
            m_get_cluster_info.assert_called_once_with()
            m_set_fallback_hosts.assert_called_once_with('host2', ['host0', 'host1', 'host2'])
            m_set_replicas_health.assert_called_once_with({
                'host0': ReplicaHealth.ALIVE,
                'host1': ReplicaHealth.DEAD,
                'host2': ReplicaHealth.ALIVE
            })
