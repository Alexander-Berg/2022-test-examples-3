# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import six

if six.PY2:
    import mock
else:
    import unittest.mock as mock

from sqlalchemy.orm import Session, Query

from search.priemka.yappy.sqla.yappy import model
from search.priemka.yappy.src.yappy_lib.queue.exceptions import DBPartitionException
from search.priemka.yappy.src.yappy_lib.queue.partitioned import (
    PartitionedDBSink,
    PartitionedDBSource,
    PartitionedMessage,
)
from search.priemka.yappy.tests.test_yappy_lib.test_queue.test_base import (
    _SourceQueueTestCase as SourceQueueTestCase,
    SinkQueueTestCase,
)


class PartitionedSinkTestCase(SinkQueueTestCase):
    queue = None  # type: PartitionedDBSink

    class TestSink(PartitionedDBSink):
        MODEL = model.Beta

    @classmethod
    def set_up_queue(cls):
        with mock.patch.object(cls.TestSink.partitions_manager, 'start'):
            cls.queue = cls.TestSink()

    def setUp(self):
        super(PartitionedSinkTestCase, self).setUp()
        self.cluster_revision_patch = mock.patch.object(
            self.queue.partitions_manager.__class__,
            'cluster_revision',
            new_callable=mock.PropertyMock,
        )
        self.cluster_revision = self.cluster_revision_patch.start()
        self.commit_patch = mock.patch.object(self.queue, 'commit_changes')
        self.commit = self.commit_patch.start()

        self.addCleanup(self.cluster_revision_patch.stop)
        self.addCleanup(self.commit_patch.stop)

    def test_verify_revision_false(self):
        self.cluster_revision.return_value = 3
        msg = PartitionedMessage('content', revision=2)
        self.assertFalse(self.queue.verify_message_revision(msg))

    def test_verify_revision_true(self):
        self.cluster_revision.return_value = 2
        msg = PartitionedMessage('content', revision=2)
        self.assertTrue(self.queue.verify_message_revision(msg))

    def test_save_message_dont_save_if_not_verified(self):
        with mock.patch.object(self.queue, 'verify_message_revision', return_value=False):
            self.queue._save_message(PartitionedMessage('content', revision=2))
            self.save_message.assert_not_called()

    def test_save_message(self):
        manager = mock.Mock()
        manager.attach_mock(self.save_message, 'save')
        manager.attach_mock(self.commit, 'commit')
        msg = PartitionedMessage('content', revision=2)
        with mock.patch.object(self.queue, 'verify_message_revision', return_value=True):
            self.queue._save_message(msg)
            manager.assert_has_calls([mock.call.save(msg, mock.ANY), mock.call.commit(mock.ANY, msg)])

    def test_commit_not_verified(self):
        self.commit_patch.stop()
        session = mock.Mock(Session)
        with mock.patch.object(self.queue, 'verify_message_revision', return_value=False):
            committed = self.queue.commit_changes(session, PartitionedMessage('content', revision=2))
            self.assertFalse(committed)

    def test_commit_verified(self):
        self.commit_patch.stop()
        session = mock.Mock(Session)
        with mock.patch.object(self.queue, 'verify_message_revision', return_value=True):
            committed = self.queue.commit_changes(session, PartitionedMessage('content', revision=2))
            self.assertTrue(committed)


class PartitionedSourceTestCase(SourceQueueTestCase):
    queue = None  # type: PartitionedDBSource

    class DummyPartitionException(DBPartitionException):
        """ Dummy exception to use in tests """

    class TestSource(PartitionedDBSource):
        MODEL = model.Beta

    @classmethod
    def set_up_queue(cls):
        with mock.patch.object(cls.TestSource.partitions_manager, 'start'):
            cls.queue = cls.TestSource()

    def setUp(self):
        super(PartitionedSourceTestCase, self).setUp()
        self.cluster_revision_patch = mock.patch.object(
            self.queue.partitions_manager.__class__,
            'cluster_revision',
            new_callable=mock.PropertyMock,
        )
        self.cluster_revision = self.cluster_revision_patch.start()
        self.apply_filters_patch = mock.patch.object(self.queue.partitions_manager, 'apply_partition_filters')
        self.apply_filters = self.apply_filters_patch.start()

        self.addCleanup(self.cluster_revision_patch.stop)
        self.addCleanup(self.apply_filters_patch.stop)

    def test_poll_partitioning_error(self):
        self.get_data.side_effect = self.DummyPartitionException
        expected = []
        self.assertEqual(self.queue.poll(1), expected)

    def test_poll_message_revision(self):
        self.cluster_revision.return_value = 7
        self.get_data.return_value = ['content-1', 'content-2']
        result = self.queue.poll(10)
        revisions = [msg.revision for msg in result]
        self.assertEqual(revisions, [7] * len(self.get_data.return_value))

    def test_get_query(self):
        q = mock.Mock(Query)
        filtered_q = mock.Mock(Query)
        session = mock.Mock(Session)
        self.get_query.return_value = q
        def apply_filters(q_):
            if q == q_:
                return filtered_q
        self.apply_filters.side_effect = apply_filters
        result = self.queue._get_query(session)
        self.assertEqual(result, filtered_q)
