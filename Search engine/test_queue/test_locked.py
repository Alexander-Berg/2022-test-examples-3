# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import six

if six.PY2:
    import mock
    from contextlib2 import ExitStack
else:
    import unittest.mock as mock
    from contextlib import ExitStack

from search.martylib.core.exceptions import Locked
from search.priemka.yappy.proto.structures.lock_pb2 import Lock
from search.priemka.yappy.src.yappy_lib import queue
from search.priemka.yappy.tests.test_yappy_lib.test_queue.test_base import (
    _SourceQueueTestCase as SourceQueueTestCase,
    SinkQueueTestCase,
)


class LockedSourceTestCase(SourceQueueTestCase):

    @classmethod
    def set_up_queue(cls):
        cls.queue = queue.LockedDBSource(lock_type=Lock.Type.GLOBAL)

    @classmethod
    def setUpClass(cls):
        super(LockedSourceTestCase, cls).setUpClass()
        cls.lock_patch = mock.patch.object(cls.queue, 'lock')

    def test_poll_locked_metrics(self):
        """ Check that ``poll`` increases metrics on success """
        expected = [mock.call('queue-{}-locked-out_summ'.format(self.queue.metric_name))]
        with \
                mock.patch.object(self.queue.metrics, 'increment') as increment, \
                self.lock_patch:
            self.queue.lock.__enter__.side_effect = Locked('dummy')
            self.queue.poll()
            self.assertEqual(increment.call_args_list, expected)

    def test_poll_get_data_locked(self):
        """ Check that ``poll`` executes ``get_data`` within ``lock`` context """
        with \
                mock.patch('search.priemka.yappy.src.yappy_lib.queue.base.session_scope') as session_scope, \
                self.lock_patch:
            chunk_size = 5
            session = session_scope().__enter__.return_value
            expected = [
                mock.call.enter_lock(),
                mock.call.get_data(chunk_size, session),
                mock.call.exit_lock(None, None, None),
            ]
            manager = mock.Mock()
            manager.attach_mock(self.get_data, 'get_data')
            manager.attach_mock(self.queue.lock.__enter__, 'enter_lock')
            manager.attach_mock(self.queue.lock.__exit__, 'exit_lock')
            self.queue.poll(chunk_size)
            self.assertEqual(manager.mock_calls, expected)

    def test_poll_outside_session_hooks_locked(self):
        """ Check that ``poll`` executes ``{pre|post}_session`` hooks within same ``lock`` context

        NOTE: this test is intended to make someone who wants to move them outside the ``lock`` think twice...
              ...and maybe add pre/post lock hooks instead.
        """
        with ExitStack() as stack:
            stack.enter_context(self.lock_patch)
            hooks = self.mock_hooks(stack)
            expected = [
                mock.call.enter_lock(),
                mock.call.pre_session(),
                mock.call.post_session(self.get_data.return_value),
                mock.call.exit_lock(None, None, None),
            ]
            manager = mock.Mock()
            manager.attach_mock(hooks['pre_session'], 'pre_session')
            manager.attach_mock(hooks['post_session'], 'post_session')
            manager.attach_mock(self.queue.lock.__enter__, 'enter_lock')
            manager.attach_mock(self.queue.lock.__exit__, 'exit_lock')
            self.queue.poll()
            self.assertEqual(manager.mock_calls, expected)

    def test_poll_in_session_hooks_locked(self):
        """ Check that ``poll`` executes ``{pre|post}_query`` hooks within same ``lock`` context """
        with ExitStack() as stack:
            session_scope = stack.enter_context(mock.patch('search.priemka.yappy.src.yappy_lib.queue.base.session_scope'))
            stack.enter_context(self.lock_patch)
            hooks = self.mock_hooks(stack)
            session = session_scope().__enter__.return_value
            expected = [
                mock.call.enter_lock(),
                mock.call.pre_query(session),
                mock.call.post_query(session, self.get_data.return_value),
                mock.call.exit_lock(None, None, None),
            ]
            manager = mock.Mock()
            manager.attach_mock(hooks['pre_query'], 'pre_query')
            manager.attach_mock(hooks['post_query'], 'post_query')
            manager.attach_mock(self.queue.lock.__enter__, 'enter_lock')
            manager.attach_mock(self.queue.lock.__exit__, 'exit_lock')
            self.queue.poll()
            self.assertEqual(manager.mock_calls, expected)


class LockedSinkTestCase(SinkQueueTestCase):
    TEST_MIN_FLUSH_INTERVAL = 5

    @classmethod
    def set_up_queue(cls):
        super(LockedSinkTestCase, cls).set_up_queue()
        queue.LockedDBSink.MIN_FLUSH_INTERVAL = cls.TEST_MIN_FLUSH_INTERVAL
        cls.queue = queue.LockedDBSink(lock_type=Lock.Type.GLOBAL)

    @classmethod
    def setUpClass(cls):
        super(LockedSinkTestCase, cls).setUpClass()
        cls.decrease_sleep_patch = mock.patch.object(cls.queue, 'decrease_flush_interval')
        cls.restore_sleep_patch = mock.patch.object(cls.queue, 'restore_flush_interval')
        cls.lock_patch = mock.patch.object(cls.queue, 'lock')

    def setUp(self):
        super(LockedSinkTestCase, self).setUp()
        self.decrease = self.decrease_sleep_patch.start()
        self.restore = self.restore_sleep_patch.start()
        self.lock = self.lock_patch.start()

        self.addCleanup(self.decrease_sleep_patch.stop)
        self.addCleanup(self.restore_sleep_patch.stop)
        self.addCleanup(self.lock_patch.stop)


class LockedSinkConfigTestCase(LockedSinkTestCase):
    def test_min_flush_interval_default(self):
        self.flush_loop_patch.stop()
        self.assertEqual(self.queue.MIN_FLUSH_INTERVAL, self.TEST_MIN_FLUSH_INTERVAL)

    def test_min_flush_interval_custom(self):
        self.flush_loop_patch.stop()
        interval = 7
        q = queue.LockedDBSink(lock_type=Lock.Type.GLOBAL, min_flush_interval=interval)
        self.assertEqual(q.MIN_FLUSH_INTERVAL, interval)


class LockedSinkFlushTestCase(LockedSinkTestCase):
    def test_not_raises_locked(self):
        self.queue.lock.__enter__.side_effect = Locked()
        try:
            self.queue.flush()
        except Locked:
            self.failureException('unexpected `Locked` exception')

    def test_locked_metric(self):
        self.queue.lock.__enter__.side_effect = Locked()
        self.queue.flush()
        total_increment = self.total_metric_increment('{}-locked_summ'.format(self.queue.metric_name))
        self.assertEqual(total_increment, 1)

    def test_decrease_flush_interval_when_locked(self):
        self.queue.lock.__enter__.side_effect = Locked()
        self.queue.flush()
        self.decrease.assert_called_once()

    def test_restore_flush_interval_on_success(self):
        self.queue.flush()
        self.restore.assert_called_once()

    def test_restore_flush_interval_on_not_locked_exception(self):
        self.queue.lock.__enter__.side_effect = self.DummyException
        try:
            self.queue.flush()
        except self.DummyException:
            pass
        self.restore.assert_called_once()


class LockedSinkFlushIntervalTestCase(LockedSinkTestCase):

    DECREASE_FACTOR = 0.7658

    def setUp(self):
        super(LockedSinkFlushIntervalTestCase, self).setUp()
        self.decrease_sleep_patch.stop()
        self.restore_sleep_patch.stop()
        self.flush_loop_patch.stop()

        self.factor_patch = mock.patch.object(
            self.queue.__class__,
            'random_decrease_factor',
            new_callable=mock.PropertyMock,
            return_value=self.DECREASE_FACTOR,
        )
        self.factor = self.factor_patch.start()
        self.sleep_interval_patch = mock.patch.object(
            self.queue.flush_loop.__class__,
            'sleep_interval',
            new_callable=mock.PropertyMock,
            return_value=self.TEST_FLUSH_INTERVAL,
        )
        self.set_sleep_interval_patch = mock.patch.object(self.queue.flush_loop, 'set_sleep_interval')
        self.sleep_interval = self.sleep_interval_patch.start()
        self.set_sleep_interval = self.set_sleep_interval_patch.start()

        self.addCleanup(self.factor_patch.stop)
        self.addCleanup(self.sleep_interval_patch.stop)
        self.addCleanup(self.set_sleep_interval_patch.stop)

    def test_decrease_once(self):
        interval = 20
        expected = self.DECREASE_FACTOR * interval
        self.sleep_interval.return_value = interval
        self.queue.decrease_flush_interval()
        self.set_sleep_interval.assert_called_with(expected)

    def test_dont_decrease_below_minimal(self):
        interval = self.TEST_MIN_FLUSH_INTERVAL + 1
        expected = self.TEST_MIN_FLUSH_INTERVAL
        self.sleep_interval.return_value = interval
        self.queue.decrease_flush_interval()
        self.set_sleep_interval.assert_called_with(expected)

    def test_decrease_dont_change_already_below_minimal(self):
        interval = self.TEST_MIN_FLUSH_INTERVAL - 1
        expected = interval
        self.sleep_interval.return_value = interval
        self.queue.decrease_flush_interval()
        self.set_sleep_interval.assert_called_with(expected)

    def test_decrease_custom_factor(self):
        factor = 0.3
        expected = self.TEST_FLUSH_INTERVAL * factor
        self.queue.decrease_flush_interval(factor)
        self.set_sleep_interval.assert_called_with(expected)

    def test_decrease_factor_zero(self):
        """ Zero is treated as 'not specified' """
        interval = 20
        expected = self.DECREASE_FACTOR * interval
        self.sleep_interval.return_value = interval
        self.queue.decrease_flush_interval(0.0)
        self.set_sleep_interval.assert_called_with(expected)

    def test_negative_decrease_factor(self):
        self.assertRaises(ValueError, self.queue.decrease_flush_interval, -0.9)

    def test_decrease_factor_one(self):
        self.assertRaises(ValueError, self.queue.decrease_flush_interval, 1.0)

    def test_decrease_factor_above_one(self):
        self.assertRaises(ValueError, self.queue.decrease_flush_interval, 1.3)

    def test_restore(self):
        self.sleep_interval.return_value = self.TEST_FLUSH_INTERVAL - 1
        self.queue.restore_flush_interval()
        self.set_sleep_interval.assert_called_with(self.TEST_FLUSH_INTERVAL)


class LockedSinkDataTestCase(LockedSinkTestCase):
    def test_data_limited(self):
        self.data_patch.stop()
        self._fill_queue()
        limit = self.TEST_QUEUE_LIMIT / 2
        with mock.patch.object(self.queue.queue, 'qsize', return_value=limit):
            read_items = list(self.queue._data)
            n_read = len(read_items)
        self.assertLessEqual(n_read, limit + 1)
