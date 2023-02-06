# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from typing import Dict

from pysyncobj.node import TCPNode
from six import with_metaclass, PY2
from sqlalchemy import tuple_
from sqlalchemy.orm import Query
from sqlalchemy.util._collections import _LW as SQLAlchemyQueryResult

from search.priemka.yappy.sqla.yappy import model
from search.priemka.yappy.src.yappy_lib.utils import session_scope
from search.priemka.yappy.src.yappy_lib.queue.exceptions import PartitionNotAssigned, QueryPartitionFailure
from search.priemka.yappy.src.yappy_lib.queue.partitions_manager import (
    DBPartitionsConsumer,
    DBPartitionsManager,
    Partition,
    PartitionedMeta,
)

from search.priemka.yappy.tests.utils.test_cases import TestCase, TestCaseWithStaticDB

if PY2:
    import mock
else:
    import unittest.mock as mock


class PartitionedTestCase(TestCase):
    ACTIVE_NODES = [TCPNode('a:1'), TCPNode('b:2'), TCPNode('a:2')]
    INACTIVE_NODES = [TCPNode('in_a:1'), TCPNode('in_b:2'), TCPNode('in_a:2')]
    ALL_NODES = ACTIVE_NODES + INACTIVE_NODES
    REVISION = 5
    PARTITIONS = None  # type: Dict[int, Partition]

    NOW = 100000
    NODE_TTL = 100

    class DummyException(Exception):
        """ Exception for tests """

    @classmethod
    def setUpClass(cls):
        super(PartitionedTestCase, cls).setUpClass()
        cls.PARTITIONS = {
            i: Partition(i, node, cls.REVISION)
            for i, node in enumerate(DBPartitionsConsumer.sorted_nodes(cls.ACTIVE_NODES))
        }

    def setUp(self):
        super(PartitionedTestCase, self).setUp()
        self.now_patch = mock.patch('search.priemka.yappy.src.yappy_lib.queue.partitions_manager.now')
        self.mocked_now = self.now_patch.start()().timestamp
        self.mocked_now.return_value = self.NOW

        self.addCleanup(self.now_patch.stop)

    def set_up_consumer(self, consumer_class=DBPartitionsConsumer, non_replicated=True):
        class ConsumerClass(consumer_class):  # Consumer is a singleton
            pass

        consumer = ConsumerClass(name='dummy', ttl=self.NODE_TTL)

        for node in self.ACTIVE_NODES:
            consumer._heartbeat(node, self.NOW - self.NODE_TTL / 2)
        for node in self.INACTIVE_NODES:
            consumer._heartbeat(node, self.NOW - self.NODE_TTL * 2)

        consumer.partitions = self.PARTITIONS.copy()
        consumer.revision = self.REVISION
        consumer.last_split_points_update = self.NOW

        if non_replicated:
            mock_sync_obj = mock.patch.object(consumer, '_syncObj').start()
            mock_sync_obj.selfNode = self.ALL_NODES[0]
            mock_sync_obj.otherNodes = set(self.ALL_NODES[1:])
            mock_sync_obj._getLeader.return_value = mock_sync_obj.selfNode

            def _reassign(partition_id, node, revision, **kwargs):
                return consumer._reassign_partition(partition_id, node, revision)

            def _heartbeat(node, timestamp, **kwargs):
                return consumer._heartbeat(node, timestamp)

            def _update(split_points, revision, **kwargs):
                return consumer._update_split_points(split_points, revision)

            mock.patch.object(consumer, 'reassign_partition', side_effect=_reassign).start()
            mock.patch.object(consumer, 'heartbeat', side_effect=_heartbeat).start()
            mock.patch.object(consumer, 'update_split_points', side_effect=_update).start()

        return consumer


class NonReplicatedConsumerTestCase(PartitionedTestCase):
    def setUp(self):
        super(NonReplicatedConsumerTestCase, self).setUp()
        self.consumer = self.set_up_consumer()
        self.mock_sync_obj = self.consumer._syncObj

    def test_sorted_list(self):
        nodes = [
            TCPNode('z:1000'),
            TCPNode('a:1000'),
            TCPNode('z:300'),
            TCPNode('a:200'),
        ]
        sorted_nodes = [  # alpha sort
            TCPNode('a:1000'),
            TCPNode('a:200'),
            TCPNode('z:1000'),
            TCPNode('z:300'),
        ]
        result = DBPartitionsConsumer.sorted_nodes(nodes)
        self.assertEqual(result, sorted_nodes)

    def test_active_nodes_set(self):
        result = set(self.consumer.active_nodes)
        expected = set(self.ACTIVE_NODES)
        self.assertEqual(result, expected)

    def test_active_nodes_sorted(self):
        expected = ['a', 'b', 'c']

        def _sorted(x):
            if set(x) == set(self.ACTIVE_NODES):
                return expected

        with mock.patch.object(self.consumer, 'sorted_nodes') as sorted_nodes:
            sorted_nodes.side_effect = _sorted
            result = self.consumer.active_nodes
            self.assertEqual(result, expected)

    def test_all_nodes_set(self):
        result = set(self.consumer.all_nodes)
        expected = set(self.ALL_NODES)
        self.assertEqual(result, expected)

    def test_all_nodes_sorted(self):
        expected = ['a', 'b', 'c']

        def _sorted(x):
            if set(x) == set(self.ALL_NODES):
                return expected

        with mock.patch.object(self.consumer, 'sorted_nodes') as sorted_nodes:
            sorted_nodes.side_effect = _sorted
            result = self.consumer.all_nodes
            self.assertEqual(result, expected)

    def test_leader_active(self):
        expected = self.ACTIVE_NODES[-1]
        self.mock_sync_obj._getLeader.return_value = expected
        result = self.consumer.leader
        self.assertEqual(result, expected)

    def test_leader_inactive(self):
        expected = self.consumer.active_nodes[0]
        self.mock_sync_obj._getLeader.return_value = self.INACTIVE_NODES[-1]
        result = self.consumer.leader
        self.assertEqual(result, expected)

    def test_self_node(self):
        expected = self.mock_sync_obj.selfNode
        result = self.consumer.self_node
        self.assertEqual(result, expected)

    def test_self_node_undefined(self):
        self.consumer._syncObj = None
        self.assertIsNone(self.consumer.self_node)

    def test_is_self_leader_true(self):
        with \
                mock.patch.object(self.consumer.__class__, 'self_node', new_callable=mock.PropertyMock) as self_node, \
                mock.patch.object(self.consumer.__class__, 'leader', new_callable=mock.PropertyMock) as leader:
            self_node.return_value = 'abc'
            leader.return_value = 'abc'
            self.assertTrue(self.consumer.is_self_leader)

    def test_is_self_leader_false(self):
        with \
                mock.patch.object(self.consumer.__class__, 'self_node', new_callable=mock.PropertyMock) as self_node, \
                mock.patch.object(self.consumer.__class__, 'leader', new_callable=mock.PropertyMock) as leader:
            self_node.return_value = 'abc'
            leader.return_value = 'aaa'
            self.assertFalse(self.consumer.is_self_leader)

    def test_is_self_leader_none_self(self):
        with \
                mock.patch.object(self.consumer.__class__, 'self_node', new_callable=mock.PropertyMock) as self_node, \
                mock.patch.object(self.consumer.__class__, 'leader', new_callable=mock.PropertyMock) as leader:
            self_node.return_value = None
            leader.return_value = 'aaa'
            self.assertFalse(self.consumer.is_self_leader)

    def test_is_self_leader_none_leader(self):
        with \
                mock.patch.object(self.consumer.__class__, 'self_node', new_callable=mock.PropertyMock) as self_node, \
                mock.patch.object(self.consumer.__class__, 'leader', new_callable=mock.PropertyMock) as leader:
            self_node.return_value = 'aaa'
            leader.return_value = None
            self.assertIsNone(self.consumer.is_self_leader)

    def test_self_partition_id(self):
        expected = 1
        with mock.patch.object(self.consumer.__class__, 'self_node', new_callable=mock.PropertyMock) as self_node:
            self_node.return_value = self.consumer.active_nodes[expected]
            self.assertEqual(self.consumer.self_partition_id, expected)

    def test_self_partition_id_undefined(self):
        with mock.patch.object(self.consumer.__class__, 'self_node', new_callable=mock.PropertyMock) as self_node:
            self_node.return_value = self.INACTIVE_NODES[0]
            self.assertIsNone(self.consumer.self_partition_id)

    def test_clear_partitions_nothing_to_remove(self):
        expected = dict(self.PARTITIONS)
        self.consumer.clear_partitions(self.REVISION)
        self.assertEqual(self.consumer.partitions, expected)

    def test_clear_partitions_nothing_to_keep(self):
        expected = {}
        self.consumer.clear_partitions(self.REVISION + 1)
        self.assertEqual(self.consumer.partitions, expected)

    def test_clear_partitions_one_removed(self):
        id_to_remove = 1
        expected = dict(self.PARTITIONS)
        expected.pop(id_to_remove)
        self.consumer.partitions[id_to_remove] = Partition(
            **dict(self.consumer.partitions[id_to_remove]._asdict(), revision=self.REVISION - 1)
        )
        self.consumer.clear_partitions(self.REVISION)
        self.assertEqual(self.consumer.partitions, expected)

    def test_update_split_points_data(self):
        split_points = [mock.Mock(SQLAlchemyQueryResult), mock.Mock(SQLAlchemyQueryResult)]
        revision = 100
        expected = (split_points, revision)
        self.consumer._update_split_points(split_points, revision)
        self.assertEqual((self.consumer.split_points, self.consumer.revision), expected)

    def test_update_split_points_partitions(self):
        split_points = [mock.Mock(SQLAlchemyQueryResult), mock.Mock(SQLAlchemyQueryResult)]
        revision = 100
        expected = {
            self.consumer.self_partition_id: Partition(
                self.consumer.self_partition_id,
                self.consumer.self_node,
                revision,
            )
        }
        self.consumer._update_split_points(split_points, revision)
        self.assertEqual(self.consumer.partitions, expected)

    def test_reassign_partition(self):
        expected = Partition(1, TCPNode('a:10'), 19)
        self.consumer._reassign_partition(expected.id, expected.assigned_node, expected.revision)
        self.assertEqual(self.consumer.partitions[expected.id], expected)

    def test_reassign_partition_no_id(self):
        expected = dict(self.consumer.partitions)
        self.consumer._reassign_partition(None, self.INACTIVE_NODES[0], self.REVISION + 1)
        self.assertEqual(self.consumer.partitions, expected)

    def test_heartbeat(self):
        node = TCPNode('a:10')
        expected = self.NOW + 100
        self.consumer._heartbeat(node, expected)
        self.assertEqual(self.consumer._nodes[node], expected)

    def test_heartbeat_activates_node(self):
        node = self.INACTIVE_NODES[0]
        self.consumer._heartbeat(node, self.NOW + 100)
        self.assertIn(node, self.consumer.active_nodes)


class NonReplicatedPartitionManagerTestCase(PartitionedTestCase):
    MODEL = model.Beta

    def setUp(self):
        super(NonReplicatedPartitionManagerTestCase, self).setUp()
        class DummyManager(DBPartitionsManager):
            pass
        class DummyConsumer(DBPartitionsConsumer):
            pass
        class DummyModel(self.MODEL):
            pass
        self.manager = DummyManager(self.MODEL)
        self.manager.cluster = self.set_up_consumer(DummyConsumer)
        self.DummyManager = DummyManager
        self.DummyConsumer = DummyConsumer
        self.DummyModel = DummyModel


class NonReplicatedPartitionManagerBaseTestCase(NonReplicatedPartitionManagerTestCase):
    def test_all_partitions_alive_true(self):
        self.assertTrue(self.manager.all_partitions_alive)

    def test_all_partitions_alive_false_all_dead(self):
        self.mocked_now.return_value += self.NODE_TTL
        self.assertFalse(self.manager.all_partitions_alive)

    def test_all_partitions_alive_one_dead(self):
        self.manager.cluster._heartbeat(self.manager.cluster.active_nodes[0], self.NOW - self.NODE_TTL)
        self.assertFalse(self.manager.all_partitions_alive)

    def test_all_partitions_alive_one_missed(self):
        self.manager.cluster._nodes.pop(self.manager.cluster.active_nodes[0])
        self.assertFalse(self.manager.all_partitions_alive)

    def test_partition_id_undefined(self):
        with mock.patch.object(self.manager.cluster.__class__, 'self_partition_id', new_callable=mock.PropertyMock) as partition:
            partition.return_value = None
            self.assertRaises(
                PartitionNotAssigned,
                getattr,
                self.manager,
                'partition_id',
            )

    def test_cluster_revision_undefined(self):
        self.manager.cluster.revision = None
        self.assertEqual(self.manager.cluster_revision, 0)

    def test_cluster_class(self):
        self.DummyManager.CONSUMER_CLASS = self.DummyConsumer
        manager = self.DummyManager(self.DummyModel)
        self.assertIsInstance(manager.cluster, self.DummyConsumer)

    def test_next_revision(self):
        """ Bit stupid test, but I couldn't invent better one """
        self.assertNotEqual(self.manager.cluster_revision, self.manager.next_revision)

    def test_n_participants_no_last_rebalance(self):
        expected = len(self.ALL_NODES)
        with mock.patch.object(self.manager.__class__, 'last_rebalance', new_callable=mock.PropertyMock) as last_rebalance:
            last_rebalance.return_value = 0
            self.assertEqual(self.manager.n_participants, expected)

    def test_n_participants_has_last_rebalance(self):
        expected = len(self.ACTIVE_NODES)
        self.assertEqual(self.manager.n_participants, expected)

    def test_n_partitions(self):
        n_partitions = 3
        split_points = [mock.Mock(SQLAlchemyQueryResult) for _ in range(n_partitions - 1)]
        self.manager.update_split_points(split_points)
        self.assertEqual(self.manager.n_partitions, n_partitions)

    def test_n_partitions_split_points_not_defined(self):
        with mock.patch.object(self.manager.__class__, 'split_points', new_callable=mock.PropertyMock, return_value=None):
            self.assertEqual(self.manager.n_partitions, 0)

    def test_n_partitions_no_split_points(self):
        with mock.patch.object(self.manager.__class__, 'split_points', new_callable=mock.PropertyMock, return_value=[]):
            self.assertEqual(self.manager.n_partitions, 1)

    def test_update_split_points(self):
        points = ['a', 'b', 'c']
        revision = self.manager.next_revision
        self.manager.update_split_points(points)
        self.manager.cluster.update_split_points.assert_called_with(points, revision, timeout=mock.ANY)

    def test_partitions_inconsistent_wrong_revision(self):
        p = self.manager.cluster.partitions[0]
        self.manager.cluster.reassign_partition(p.id, p.assigned_node, self.manager.next_revision)
        self.assertTrue(self.manager.partitions_inconsistent)

    def test_partitions_inconsistent_false(self):
        self.assertFalse(self.manager.partitions_inconsistent)

    def test_rebalance_required_never_rebalanced(self):
        prop = 'last_rebalance'
        with mock.patch.object(self.manager.__class__, prop, new_callable=mock.PropertyMock) as last_rebalance:
            last_rebalance.return_value = 0
            self.assertTrue(self.manager.rebalance_required)

    def test_rebalance_required_inconsistent(self):
        prop = 'partitions_inconsistent'
        with mock.patch.object(self.manager.__class__, prop, new_callable=mock.PropertyMock) as inconsistent:
            inconsistent.return_value = True
            self.assertTrue(self.manager.rebalance_required)

    def test_rebalance_required_not_all_alive(self):
        prop = 'all_partitions_alive'
        with mock.patch.object(self.manager.__class__, prop, new_callable=mock.PropertyMock) as alive:
            alive.return_value = False
            self.assertTrue(self.manager.rebalance_required)

    def test_rebalance_required_configured_more_than_expected(self):
        prop = 'n_partitions'
        with mock.patch.object(self.manager.__class__, prop, new_callable=mock.PropertyMock) as n_partitions:
            n_partitions.return_value = len(self.manager.partitions) - 1
            self.assertTrue(self.manager.rebalance_required)

    def test_rebalance_required_assigned_less_than_configured(self):
        prop = 'n_partitions'
        with mock.patch.object(self.manager.__class__, prop, new_callable=mock.PropertyMock) as n_partitions:
            n_partitions.return_value = len(self.manager.partitions) + 1
            self.assertTrue(self.manager.rebalance_required)

    def test_rebalance_required_configured_less_than_participants(self):
        prop = 'n_partitions'
        with mock.patch.object(self.manager.__class__, prop, new_callable=mock.PropertyMock) as n_partitions:
            n_partitions.return_value = self.manager.n_participants - 1
            self.assertTrue(self.manager.rebalance_required)

    def test_rebalance_required_configured_more_than_participants(self):
        prop = 'n_partitions'
        with mock.patch.object(self.manager.__class__, prop, new_callable=mock.PropertyMock) as n_partitions:
            n_partitions.return_value = self.manager.n_participants + 1
            self.assertTrue(self.manager.rebalance_required)

    def test_rebalance_required_false(self):
        with mock.patch.object(self.manager.__class__, 'n_partitions', new_callable=mock.PropertyMock) as n_partitions:
            n_partitions.return_value = self.manager.n_participants
            self.assertFalse(self.manager.rebalance_required)

    def test_try_rebalance_partitions_split_points_failed(self):
        with mock.patch.object(self.manager, 'get_new_split_points') as split_points:
            split_points.side_effect = self.DummyException
            try:
                self.manager.try_rebalance_partitions()
            except self.DummyException:
                self.fail('unexpected DummyException raised by `get_new_split_points`')

    def test_try_rebalance_partitions_update_failed(self):
        with mock.patch.object(self.manager, 'update_split_points') as update:
            update.side_effect = self.DummyException
            try:
                self.manager.try_rebalance_partitions()
            except self.DummyException:
                self.fail('unexpected DummyException raised by `update_split_points`')

    def test_try_rebalance_logic(self):
        points = ['a', 'b', 'c']
        with \
                mock.patch.object(self.manager, 'get_new_split_points') as split_points, \
                mock.patch.object(self.manager, 'update_split_points') as update:
            split_points.return_value = points
            self.manager.try_rebalance_partitions()
            update.assert_called_with(points)

    def test_maybe_rebalance_not_requred(self):
        self.manager.rebalance_timeout = 0
        prop_name = 'rebalance_required'
        with \
                mock.patch.object(self.manager.__class__, prop_name, new_callable=mock.PropertyMock) as prop, \
                mock.patch.object(self.manager, 'try_rebalance_partitions') as try_rebalance:
            prop.return_value = False
            self.manager.maybe_rebalance()
            try_rebalance.assert_not_called()

    def test_maybe_rebalance_timeout(self):
        self.manager.rebalance_timeout = 100
        with mock.patch.object(self.manager, 'try_rebalance_partitions') as try_rebalance:
            self.manager.maybe_rebalance()
            try_rebalance.assert_not_called()

    def test_maybe_rebalance_not_leader(self):
        self.manager.rebalance_timeout = 0
        prop_name = 'is_self_leader'
        with \
                mock.patch.object(self.manager.cluster.__class__, prop_name, new_callable=mock.PropertyMock) as prop, \
                mock.patch.object(self.manager, 'try_rebalance_partitions') as try_rebalance:
            prop.return_value = False
            self.manager.maybe_rebalance()
            try_rebalance.assert_not_called()

    def test_maybe_rebalance_try_rebalance(self):
        self.manager.rebalance_timeout = 0
        prop_name = 'rebalance_required'
        with \
                mock.patch.object(self.manager.__class__, prop_name, new_callable=mock.PropertyMock) as prop, \
                mock.patch.object(self.manager, 'try_rebalance_partitions') as try_rebalance:
            prop.return_value = True
            self.manager.maybe_rebalance()
            try_rebalance.assert_called()

    def test_is_partition_alive_custom_probe_failed(self):
        self.manager.add_partition_liveness_probe(mock.Mock(return_value=False))
        self.assertFalse(self.manager.is_partition_alive)

    def test_is_partition_alive_custom_probe_succeed(self):
        self.manager.add_partition_liveness_probe(mock.Mock(return_value=True))
        self.assertTrue(self.manager.is_partition_alive)

    def test_is_partition_alive_one_of_custom_probes_failed(self):
        self.manager.add_partition_liveness_probe(mock.Mock(return_value=True))
        self.manager.add_partition_liveness_probe(mock.Mock(return_value=False))
        self.manager.add_partition_liveness_probe(mock.Mock(return_value=True))
        self.assertFalse(self.manager.is_partition_alive)

    def test_is_partition_alive_default_probe_failed(self):
        with mock.patch.object(self.manager, 'default_liveness_probe', return_value=False):
            self.assertFalse(self.manager.is_partition_alive)

    def test_is_partition_alive_default_probe_succeed(self):
        with mock.patch.object(self.manager, 'default_liveness_probe', return_value=True):
            self.assertTrue(self.manager.is_partition_alive)

    def test_is_partition_alive_custom_overrides_default(self):
        self.manager.add_partition_liveness_probe(mock.Mock(return_value=True))
        with mock.patch.object(self.manager, 'default_liveness_probe', return_value=False):
            self.assertTrue(self.manager.is_partition_alive)

    def test_heartbeat_alive(self):
        prop_name = 'is_partition_alive'
        with mock.patch.object(self.manager.__class__, prop_name, new_callable=mock.PropertyMock) as prop:
            prop.return_value = True
            self.manager.heartbeat()
            self.manager.cluster.heartbeat.assert_called()

    def test_heartbeat_dead(self):
        prop_name = 'is_partition_alive'
        with mock.patch.object(self.manager.__class__, prop_name, new_callable=mock.PropertyMock) as prop:
            prop.return_value = False
            self.manager.heartbeat()
            self.manager.cluster.heartbeat.assert_not_called()

    def test_starts_only_once(self):
        hb = mock.patch.object(self.manager.__class__, 'heartbeat_loop', new_callable=mock.PropertyMock)
        metrics = mock.patch.object(self.manager.__class__, 'metrics_loop', new_callable=mock.PropertyMock)
        rebalance = mock.patch.object(self.manager.__class__, 'rebalance_loop', new_callable=mock.PropertyMock)
        with hb, metrics, rebalance:
            self.manager.start()
            self.manager.start()
            self.manager.heartbeat_loop.start.assert_called_once()
            self.manager.metrics_loop.start.assert_called_once()
            self.manager.rebalance_loop.start.assert_called_once()


class PartitionManagerDBTestCase(NonReplicatedPartitionManagerTestCase, TestCaseWithStaticDB):
    COMPOSITE_KEY_MODEL = model.History
    SINGLE_KEY_MODEL = model.Beta
    N_COMPOSITE_RECORDS = 100
    N_SINGLE_RECORDS = 100

    def setUp(self):
        super(PartitionManagerDBTestCase, self).setUp()
        self.manager.records_pool = self.SINGLE_KEY_MODEL

    @classmethod
    def create_test_data(cls):
        with session_scope() as session:
            session.add_all([model.History(related_id=str(i), revision=i) for i in range(cls.N_COMPOSITE_RECORDS)])
            session.add_all([model.Beta(name=str(i)) for i in range(cls.N_COMPOSITE_RECORDS)])

    def test_get_new_split_points_number(self):
        n_partitions = 13
        split_points = self.manager.get_new_split_points(n_partitions)
        self.assertEqual(len(split_points), n_partitions - 1)

    def test_get_new_split_points_number_partitions_as_records(self):
        n_partitions = self.N_SINGLE_RECORDS
        split_points = self.manager.get_new_split_points(n_partitions)
        self.assertEqual(len(split_points), n_partitions - 1)

    def test_get_new_split_points_number_one_partition(self):
        n_partitions = 1
        split_points = self.manager.get_new_split_points(n_partitions)
        self.assertEqual(len(split_points), n_partitions - 1)

    def test_get_new_split_points_number_no_partitions(self):
        self.assertRaises(
            ValueError,
            self.manager.get_new_split_points,
            0,
        )

    def test_get_new_split_points_composite_key(self):
        self.manager.records_pool = self.COMPOSITE_KEY_MODEL
        n_partitions = 3
        split_points = self.manager.get_new_split_points(n_partitions)
        self.assertEqual(len(split_points), n_partitions - 1)

    def test_partition_borders(self):
        n_partitions = 3
        borders = []
        split_points = self.manager.get_new_split_points(n_partitions)
        self.manager.update_split_points(split_points)
        expected = [
            (None, split_points[0]),
            (split_points[0], split_points[1]),
            (split_points[1], None),
        ]
        for i in range(n_partitions):
            with mock.patch.object(self.manager.__class__, 'partition_id', new_callable=mock.PropertyMock, return_value=i):
                borders.append(self.manager.partition_borders)
        self.assertEqual(borders, expected)

    def test_partition_borders_single_partition(self):
        n_partitions = 1
        split_points = self.manager.get_new_split_points(n_partitions)
        self.manager.update_split_points(split_points)
        expected = (None, None)
        with mock.patch.object(self.manager.__class__, 'n_partitions', new_callable=mock.PropertyMock, return_value=0):
            self.assertEqual(self.manager.partition_borders, expected)

    def test_partition_filters_single_partition(self):
        expected = []
        with \
                mock.patch.object(self.manager.__class__, 'partition_id', new_callable=mock.PropertyMock, return_value=0), \
                mock.patch.object(self.manager.__class__, 'partition_borders', new_callable=mock.PropertyMock) as borders:
            borders.return_value = (None, None)
            self.assertEqual(self.manager.partition_filters, expected)

    def test_partition_filters_first_partition(self):
        right_border = ('name', )
        expected = (tuple_(model.Beta.name) < right_border)
        with \
                mock.patch.object(self.manager.__class__, 'partition_id', new_callable=mock.PropertyMock, return_value=0), \
                mock.patch.object(self.manager.__class__, 'partition_borders', new_callable=mock.PropertyMock) as borders:
            borders.return_value = (None, right_border)
            partition_filters = self.manager.partition_filters
            self.assertEqual(len(partition_filters), 1)
            self.assertTrue(partition_filters[0].compare(expected))

    def test_partition_filters_last_partition(self):
        left_border = ('name', )
        expected = (tuple_(model.Beta.name) >= left_border)
        with \
                mock.patch.object(self.manager.__class__, 'partition_id', new_callable=mock.PropertyMock, return_value=0), \
                mock.patch.object(self.manager.__class__, 'partition_borders', new_callable=mock.PropertyMock) as borders:
            borders.return_value = (left_border, None)
            partition_filters = self.manager.partition_filters
            self.assertEqual(len(partition_filters), 1)
            self.assertTrue(partition_filters[0].compare(expected))

    def test_partition_filters_mid_partition(self):
        left_border = ('name-1', )
        right_border = ('name-2', )
        expected = [tuple_(model.Beta.name) >= left_border, tuple_(model.Beta.name) < right_border]
        with \
                mock.patch.object(self.manager.__class__, 'partition_id', new_callable=mock.PropertyMock, return_value=0), \
                mock.patch.object(self.manager.__class__, 'partition_borders', new_callable=mock.PropertyMock) as borders:
            borders.return_value = (left_border, right_border)
            partition_filters = self.manager.partition_filters
            self.assertEqual(len(partition_filters), 2)
            for i, f in enumerate(expected):
                self.assertTrue(partition_filters[i].compare(f))

    def test_apply_partition_filters(self):
        q = Query(model.Beta.name)
        expected = ['filter1', 'filter2']
        with \
                mock.patch.object(self.manager.__class__, 'partition_filters', new_callable=mock.PropertyMock) as filters, \
                mock.patch.object(q, 'filter') as f:
            filters.return_value = expected
            self.manager.apply_partition_filters(q)
            f.assert_called_with(*expected)

    def test_apply_partition_filters_wrong_query(self):
        q = Query(model.Check.id)
        with \
                mock.patch.object(self.manager.__class__, 'partition_filters', new_callable=mock.PropertyMock, return_value=[]), \
                mock.patch.object(q, 'filter'):
            self.assertRaises(
                QueryPartitionFailure,
                self.manager.apply_partition_filters,
                q,
            )

    def test_apply_partition_filters_not_assigned(self):
        q = Query(model.Beta.name)
        with mock.patch.object(self.manager.__class__, 'partition_id', new_callable=mock.PropertyMock) as partition_id:
            partition_id.side_effect = PartitionNotAssigned('node', 'name')
            self.assertRaises(
                PartitionNotAssigned,
                self.manager.apply_partition_filters,
                q,
            )

    def test_partitions_power(self):
        n_partitions = 3
        expected = self.N_SINGLE_RECORDS / n_partitions
        split_points = self.manager.get_new_split_points(n_partitions)
        self.manager.update_split_points(split_points)
        with session_scope() as session:
            q = session.query(model.Beta.name)
            result = self.manager.apply_partition_filters(q).count()
        self.assertAlmostEqual(result, expected, delta=1)

    def test_partitions_power_single_partition(self):
        n_partitions = 1
        expected = self.N_SINGLE_RECORDS / n_partitions
        split_points = self.manager.get_new_split_points(n_partitions)
        self.manager.update_split_points(split_points)
        with session_scope() as session:
            q = session.query(model.Beta.name)
            result = self.manager.apply_partition_filters(q).count()
        self.assertAlmostEqual(result, expected, delta=1)

    def test_partitions_power_composite_key(self):
        self.manager.records_pool = self.COMPOSITE_KEY_MODEL
        n_partitions = 3
        expected = self.N_SINGLE_RECORDS / n_partitions
        split_points = self.manager.get_new_split_points(n_partitions)
        self.manager.update_split_points(split_points)
        with session_scope() as session:
            q = session.query(model.History.related_id)
            result = self.manager.apply_partition_filters(q).count()
        self.assertAlmostEqual(result, expected, delta=1)

    def test_n_partitions_after_rebalance(self):
        expected = 6
        with mock.patch.object(self.manager.__class__, 'n_participants', new_callable=mock.PropertyMock, return_value=6):
            self.manager.try_rebalance_partitions()
            self.assertEqual(self.manager.n_partitions, expected)


class PartitionedMetaTestCase(TestCase):

    def test_abstract_class_not_requires_model(self):
        try:
            class DummyPartitioned(with_metaclass(PartitionedMeta)):  # noqa
                __abstract_partitioned__ = True
        except ValueError as err:
            self.fail('unexpected ValueError: {}'.format(err))

    def test_abstract_property_non_inheritable(self):
        def declare():
            class DummyPartitioned(with_metaclass(PartitionedMeta)):  # noqa
                __abstract_partitioned__ = True
            class MyClass(DummyPartitioned):
                pass

        self.assertRaisesRegexp(
            ValueError,
            'MODEL attribute missed',
            declare,
        )

    def test_model_required(self):
        def declare():
            class DummyPartitioned(with_metaclass(PartitionedMeta)):  # noqa
                pass

        self.assertRaisesRegexp(
            ValueError,
            'MODEL attribute missed',
            declare,
        )

    def test_partitions_manager_init_on_declaration(self):
        with mock.patch.object(PartitionedMeta, 'init_partitions') as init_partitions:
            class DummyPartitioned(with_metaclass(PartitionedMeta)):  # noqa
                MODEL = model.Beta
            init_partitions.assert_called()

    def test_partitions_manager_no_init_for_abstract(self):
        with mock.patch.object(PartitionedMeta, 'init_partitions') as init_partitions:
            class DummyPartitioned(with_metaclass(PartitionedMeta)):  # noqa
                __abstract_partitioned__ = True
                MODEL = model.Beta
            init_partitions.assert_not_called()

    def test_init_partitions(self):
        class DummyClass(with_metaclass(PartitionedMeta)):
            MODEL = model.Beta
            partitions_manager = None

        self.assertEqual(DummyClass.partitions_manager, DBPartitionsManager(DummyClass.MODEL))

    def test_init_partitions_already_initialized(self):
        class DummyClass(with_metaclass(PartitionedMeta)):
            MODEL = model.Beta
            partitions_manager = None

        def declare():
            class DoubleDummy(DummyClass):  # noqa
                MODEL = model.History

        self.assertRaisesRegexp(
            ValueError,
            'partitions manager already configured',
            declare,
        )

    def test_model_in_bases(self):
        def declare():
            class PartitionedWithModel(with_metaclass(PartitionedMeta)):
                MODEL = model.Beta
            class HeirPartitioned(PartitionedWithModel):  # noqa
                pass

        try:
            declare()
        except ValueError as err:
            self.fail('unexpected ValueError: {}'.format(err))
