# coding: utf8
from __future__ import absolute_import, division, print_function, unicode_literals

import random
import time

import mock
import pytest

from travel.rasp.library.python.db.cluster import DbInstance, ClusterBase, ClusterConst, ConnectionFailure, ClusterException, ClusterPeriodicUpdateMixin
from travel.rasp.library.python.db.replica_health import ReplicaHealth
from travel.rasp.library.python.db.tests.factories import assert_instances_lists_equal, get_db_instances


class ClusterBaseRealisation(ClusterBase):
    def __init__(self, instances, *args, **kwargs):
        super(ClusterBaseRealisation, self).__init__(*args, **kwargs)
        self.__instances = None
        self.set_instances(instances)

    def set_instances(self, instances):
        self.__instances = instances

    def get_actual_instances_list(self):
        return self.__instances


class TestDbInstance(object):
    def test_eq_and_hash(self):
        inst1_1 = DbInstance('h1', is_master=True, dc='sas', priority=100)
        inst1_2 = DbInstance('h1', is_master=False, dc='vla', priority=1)
        inst2 = DbInstance('h2', is_master=False, dc='vla', priority=1)
        inst3 = DbInstance('h3', is_master=False, dc='vla', priority=1)

        assert inst1_1 == inst1_2
        assert inst1_1 != inst2 != inst3
        assert inst1_1 in {inst1_1, inst3}
        assert inst2 not in {inst1_1, inst3}

    def test_is_replica(self):
        inst = DbInstance('h1', is_master=True)
        assert inst.is_master is True
        assert inst.is_replica is False
        inst = DbInstance('h1', is_master=False)
        assert inst.is_master is False
        assert inst.is_replica is True


class TestClusterBase(object):
    def test_get_instances(self):
        cluster = ClusterBaseRealisation([
            DbInstance('host1', is_master=True),
            DbInstance('host2', is_master=False),
            DbInstance('host3', is_master=False),
            DbInstance('host4', is_master=False),
        ])

        expected = [DbInstance('host1', is_master=True),
                    DbInstance('host2', is_master=False),
                    DbInstance('host3', is_master=False),
                    DbInstance('host4', is_master=False)]

        assert_instances_lists_equal(cluster.instances, expected)
        assert_instances_lists_equal(cluster.get_instances(), expected)

    def test_get_instances_filter_and_sort(self):
        cluster = ClusterBaseRealisation(
            instances=[
                DbInstance('host1', is_master=True, dc='vla'),
                DbInstance('host2', is_master=False, dc='sas'),
                DbInstance('host3', is_master=False, dc='myt'),
                DbInstance('host4', is_master=False, dc='sas'),
            ],
            instance_filter=lambda inst: inst.is_master or inst.host != 'host2',
            instance_sort_key=lambda inst, current_dc: -1 if current_dc == inst.dc else 0 if inst.host == 'host3' else 1 if inst.is_master else 2,
        )

        expected = [
            DbInstance('host3', is_master=False, dc='myt'),
            DbInstance('host1', is_master=True, dc='vla'),
            DbInstance('host4', is_master=False, dc='sas'),
        ]
        assert_instances_lists_equal(cluster.get_instances(), expected)

        expected = [
            DbInstance('host4', is_master=False, dc='sas'),
            DbInstance('host3', is_master=False, dc='myt'),
            DbInstance('host1', is_master=True, dc='vla'),
        ]
        assert_instances_lists_equal(cluster.get_instances('sas'), expected)

    def test_update_cluster_configuration(self):
        cluster = ClusterBaseRealisation(instances=[DbInstance('host1'), DbInstance('host2')])
        expected = [DbInstance('host1'), DbInstance('host2')]
        assert_instances_lists_equal(cluster.get_instances(), expected)

        cluster.set_instances([DbInstance('host3'), DbInstance('host2')])
        assert_instances_lists_equal(cluster.get_instances(), expected)

        cluster.update_cluster_configuration()
        expected = [DbInstance('host3'), DbInstance('host2')]
        assert_instances_lists_equal(cluster.get_instances(), expected)

    def test_get_connection_to_instance(self):
        def conn_getter(inst):
            if inst.host == 'host1':
                raise ConnectionFailure('no connection')
            elif inst.host == 'host2':
                return mock.sentinel.connection

        m_conn_getter = mock.Mock()
        m_conn_getter.side_effect = conn_getter

        cluster = ClusterBaseRealisation(
            instances=[DbInstance('host1'), DbInstance('host2')],
            connection_getter=m_conn_getter,
        )

        inst = DbInstance('host2')
        conn = cluster.get_connection_to_instance(inst)
        assert conn is mock.sentinel.connection
        m_conn_getter.assert_called_once_with(inst)
        assert cluster.is_instance_alive(inst) is True

        inst = DbInstance('host1')
        with pytest.raises(ConnectionFailure):
            cluster.get_connection_to_instance(inst)
        assert cluster.is_instance_alive(inst) is False

    def test_get_connection(self):
        def conn_getter(inst):
            if inst.host == 'host2':
                raise ConnectionFailure('no connection')
            elif inst.host == 'host1':
                return mock.sentinel.conn_host1
            elif inst.host == 'host4':
                return mock.sentinel.conn_host4
            else:
                raise Exception('Unknown instance')

        m_conn_getter = mock.Mock()
        m_conn_getter.side_effect = conn_getter

        cluster = ClusterBaseRealisation(
            instances=[
                DbInstance('host1', is_master=False, dc='vla', priority=0),
                DbInstance('host3', is_master=False, dc='sas', priority=1),
                DbInstance('host2', is_master=False, dc='sas', priority=2),
                DbInstance('host4', is_master=False, dc='sas', priority=3),
            ],
            connection_getter=m_conn_getter,
            instance_filter=lambda inst: inst.host != 'host3',
        )
        assert cluster.is_instance_alive(DbInstance('host2')) is True

        # host3 is filtered out, host2 is dead, host1 in bad dc -> connect to host4
        conn = cluster.get_connection('sas')
        assert conn is mock.sentinel.conn_host4
        assert m_conn_getter.call_args_list == [
            mock.call(DbInstance('host2')),
            mock.call(DbInstance('host4'))
        ]
        assert cluster.is_instance_alive(DbInstance('host2')) is False
        assert cluster.is_instance_alive(DbInstance('host4')) is True

        # host4 is marked dead -> connect to host1 in bad dc
        m_conn_getter.reset_mock()
        cluster.set_instance_dead(DbInstance('host4'))
        conn = cluster.get_connection('sas')
        assert conn is mock.sentinel.conn_host1
        assert m_conn_getter.call_args_list == [mock.call(DbInstance('host1'))]

        # host1 is dead -> everything is marked dead;
        # try to reconnect to dead replicas again -> find out that host4 is actually alive
        m_conn_getter.reset_mock()
        cluster.set_instance_dead(DbInstance('host1'))
        conn = cluster.get_connection('sas')
        assert conn is mock.sentinel.conn_host4
        assert m_conn_getter.call_args_list == [
            mock.call(DbInstance('host2')),
            mock.call(DbInstance('host4')),
        ]

        # can't connect to anything
        m_conn_getter.reset_mock()
        m_conn_getter.side_effect = ConnectionFailure
        with pytest.raises(ClusterException):
            cluster.get_connection('sas')

    def test_instance_dead_alive(self):
        inst = DbInstance('host1')
        unknown_inst = DbInstance('host2', health=ReplicaHealth.UNKNOWN)

        cluster = ClusterBaseRealisation(instances=[inst, unknown_inst])

        assert cluster.is_instance_alive(inst) is True
        assert cluster.get_inst_alive_description(inst) == 'host1 is alive'

        cluster.set_instance_dead(inst, 'because of gladiolus')
        assert cluster.is_instance_alive(inst) is False
        assert cluster.get_inst_alive_description(inst) == 'host1 is dead: because of gladiolus'

        assert cluster.is_instance_alive(unknown_inst) is False
        assert cluster.get_inst_alive_description(unknown_inst) == 'host2 is dead: health is UNKNOWN but expected ALIVE'

        current_time = time.time()
        with mock.patch.object(time, 'time') as m_time:
            m_time.return_value = current_time + 100000  # alot of time is passed, instance should not be marked dead anymore
            assert cluster.is_instance_alive(inst) is True

            assert cluster.is_instance_alive(unknown_inst) is False  # but instance with UNKNOWN health is still dead

    @pytest.mark.parametrize('instance_alive,dc,priority,is_master,current_dc,health,expected', (
        (True, 'sas', 42, True, 'sas', ReplicaHealth.ALIVE, [0, 0, 42, 1]),
        (True, 'vla', 11, True, 'vla', ReplicaHealth.SUSPENDED, [1, 0, 11, 1]),
        (False, 'iva', 43, False, 'sas', ReplicaHealth.ALIVE, [1, 3, 43, 0]),
    ))
    def test_default_instance_sort_func(self, instance_alive, dc, priority, is_master, current_dc, health, expected):
        inst = DbInstance('h', is_master, dc, priority, health)
        cluster = ClusterConst(instances=[inst])
        cluster.update_cluster_configuration()
        cluster.set_instance_alive(inst) if instance_alive else cluster.set_instance_dead(inst)

        with mock.patch.object(random, 'random', return_value=0.12):
            expected.append(0.12)
            assert cluster.default_instance_sort_func(inst, current_dc) == tuple(expected)


def test_cluster_const():
    cluster = ClusterConst(instances=get_db_instances('h1', 'h2', 'h3'))
    res_inst = cluster.get_actual_instances_list()
    assert_instances_lists_equal(res_inst, get_db_instances('h1', 'h2', 'h3'))


class TestClusterPeriodicUpdateMixin(object):
    def test_valid(self):
        class PeriodicUpdateCluster(ClusterPeriodicUpdateMixin, ClusterBaseRealisation):
            on_before_update_cluster_configuration_called = 0

            def on_before_update_cluster_configuration(self):
                self.on_before_update_cluster_configuration_called += 1

        with mock.patch.object(time, 'time') as m_time:
            def set_time(t):
                m_time.return_value = t

            set_time(0)
            cluster = PeriodicUpdateCluster(
                instances=[DbInstance('host1'), DbInstance('host2')],
                cluster_info_ttl=42,
            )
            assert_instances_lists_equal(cluster.get_instances(), [DbInstance('host1'), DbInstance('host2')])
            assert cluster.on_before_update_cluster_configuration_called == 1

            cluster.set_instances([DbInstance('host3'), DbInstance('host4')])
            set_time(41)
            assert_instances_lists_equal(cluster.get_instances(), [DbInstance('host1'), DbInstance('host2')])
            assert cluster.on_before_update_cluster_configuration_called == 1
            set_time(42)
            assert_instances_lists_equal(cluster.get_instances(), [DbInstance('host3'), DbInstance('host4')])
            assert cluster.on_before_update_cluster_configuration_called == 2

            cluster.set_instances([DbInstance('host5')])
            assert_instances_lists_equal(cluster.get_instances(), [DbInstance('host3'), DbInstance('host4')])
            assert cluster.on_before_update_cluster_configuration_called == 2
            set_time(83)
            assert_instances_lists_equal(cluster.get_instances(), [DbInstance('host3'), DbInstance('host4')])
            assert cluster.on_before_update_cluster_configuration_called == 2
            set_time(84)
            assert_instances_lists_equal(cluster.get_instances(), [DbInstance('host5')])
            assert cluster.on_before_update_cluster_configuration_called == 3

    def test_error_update(self):
        class PeriodicUpdateCluster(ClusterPeriodicUpdateMixin, ClusterBaseRealisation):
            broken = False

            class SomeError(Exception):
                pass

            def get_actual_instances_list(self):
                if self.broken:
                    raise self.SomeError
                else:
                    return super(PeriodicUpdateCluster, self).get_actual_instances_list()

        with mock.patch.object(time, 'time') as m_time:
            def set_time(t):
                m_time.return_value = t

            set_time(0)
            cluster = PeriodicUpdateCluster(
                instances=[DbInstance('host1'), DbInstance('host2')],
                cluster_info_ttl=3
            )
            assert_instances_lists_equal(cluster.get_instances(), [DbInstance('host1'), DbInstance('host2')])

            cluster.set_instances([DbInstance('host5')])
            cluster.broken = True
            assert_instances_lists_equal(cluster.get_instances(), [DbInstance('host1'), DbInstance('host2')])
            set_time(20)
            assert_instances_lists_equal(cluster.get_instances(), [DbInstance('host1'), DbInstance('host2')])

            cluster.raise_on_update_fail = True
            with pytest.raises(PeriodicUpdateCluster.SomeError):
                cluster.get_instances()
