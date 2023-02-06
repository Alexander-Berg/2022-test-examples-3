# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from typing import Any, AnyStr, Dict, Optional

import threading
import six

if six.PY2:
    import mock
    from collections import MutableSequence, Sequence
    from contextlib2 import ExitStack
else:
    import unittest.mock as mock
    from collections.abc import MutableSequence, Sequence
    from contextlib import ExitStack

from concurrent.futures import ThreadPoolExecutor, wait
from itertools import chain

from search.priemka.yappy.src.yappy_lib.queue import (
    base as queue,
    Message,
)
from search.priemka.yappy.src.yappy_lib.queue.base import queue as _queue

from search.priemka.yappy.tests.utils.test_cases import TestCase


class QueueTestCase(TestCase):
    class DummyMessage(Message):
        pass

    class DummyQueue(queue.Queue):
        pass

    @classmethod
    def setUpClass(cls):
        cls.DummyQueue.MESSAGE_CLASS = cls.DummyMessage

    def test_default_metric_name(self):
        expected = 'dummy_queue'
        result = self.DummyQueue().metric_name
        self.assertEqual(result, expected)

    def test_custom_metric_name(self):
        expected = 'custom_metric_name'
        result = queue.Queue(expected).metric_name
        self.assertEqual(result, expected)

    def test_default_message_class(self):
        expected = self.DummyMessage
        result = self.DummyQueue().message_class
        self.assertEqual(result, expected)

    def test_custom_message_class(self):
        expected = self.DummyMessage
        result = queue.Queue(message_class=expected).message_class
        self.assertEqual(result, expected)

    def test_trace_kwargs(self):
        expected = {'key': 'value'}
        result = queue.Queue(trace_kwargs=expected).trace_kwargs
        self.assertEqual(result, expected)

    def test_traced_call(self):
        """ Test that ``_traced_call`` calls passed ``func`` within ``trace`` context and passes proper params to both """
        dummy = mock.MagicMock()
        args = ('val1', 'val2')
        default_kwargs = {'key': 'value', 'trace_kw': 'default_val'}
        trace_kwargs = {'trace_kw': 'val'}
        kwargs = {'kw1': 'v1', 'kw2': 'v2', 'trace_kwargs': trace_kwargs}
        expected_trace_kwargs = dict(default_kwargs)
        expected_trace_kwargs.update(trace_kwargs)
        trace_title = 'trace title'
        expected = [
            mock.call.trace(trace_title, **expected_trace_kwargs),
            mock.call.trace_enter(),
            mock.call.func(*args, **kwargs),
            mock.call.trace_exit(None, None, None),
        ]
        q = queue.Queue(trace_kwargs=default_kwargs)
        with mock.patch('search.priemka.yappy.src.yappy_lib.queue.base.trace') as mocked_trace:
            manager = mock.Mock()
            manager.attach_mock(mocked_trace, 'trace')
            manager.attach_mock(dummy, 'func')
            manager.attach_mock(mocked_trace.return_value.__enter__, 'trace_enter')
            manager.attach_mock(mocked_trace.return_value.__exit__, 'trace_exit')
            q._traced_call(dummy, trace_title, *args, **kwargs)
        self.assertEqual(manager.mock_calls, expected)


class _SourceQueueTestCase(TestCase):

    # Queue hooks in order of their calls in `poll`
    queue_hooks = ['pre_session', 'pre_query', 'post_query', 'post_session']

    def mock_hooks(self, stack):
        # type: (ExitStack) -> Dict[mock.MagicMock]
        mocked = {
            hook_name: stack.enter_context(mock.patch.object(self.queue, '{}_hook'.format(hook_name)))
            for hook_name in self.queue_hooks
        }
        return mocked

    @classmethod
    def set_up_queue(cls):
        cls.queue = queue.SourceDBQueue()

    @classmethod
    def setUpClass(cls):
        cls.set_up_queue()
        cls.get_data_patch = mock.patch.object(cls.queue, '_get_data')
        cls.get_query_patch = mock.patch.object(cls.queue, 'get_query')

    def setUp(self):
        self.get_data = self.get_data_patch.start()
        self.get_query = self.get_query_patch.start()
        self.get_data.return_value = []

        self.addCleanup(self.get_data_patch.stop)
        self.addCleanup(self.get_query_patch.stop)


class SourceQueueTestCase(_SourceQueueTestCase):

    def test_hooks_called(self):
        """ Check that all hooks mentioned in ``queue_hooks`` are called """
        with ExitStack() as stack:
            hooks = self.mock_hooks(stack)
            self.queue.poll()
        result = {
            hook: mocked.called
            for hook, mocked in hooks.items()
        }
        expected = {
            hook: True
            for hook, mocked in hooks.items()
        }
        self.assertEqual(result, expected)

    def test_hooks_order(self):
        """ Check that hooks are called in order specified in ``queue_hooks`` """
        expected = [getattr(mock.call, hook) for hook in self.queue_hooks]
        with ExitStack() as stack:
            hooks = self.mock_hooks(stack)
            manager = mock.Mock()
            for hook_name, mocked in hooks.items():
                manager.attach_mock(mocked, hook_name)
            self.queue.poll()
            result = manager.mock_calls
            # Update expected calls with real args/kwargs, for we only care about the order
            expected = [
                expected[i](*result[i].args, **result[i].kwargs) for i in range(max(len(result), len(expected)))
            ]
            self.assertEqual(result, expected)

    def test_hook_calls_traced(self):
        """ Check that ``_traced_call`` is called with each of the hooks """
        with ExitStack() as stack:
            hooks = self.mock_hooks(stack)
            traced = stack.enter_context(mock.patch.object(self.queue, '_traced_call'))
            self.queue.poll()
        expected = [(hooks[hook], '{}_hook'.format(hook)) for hook in self.queue_hooks]
        result = [(c.args[0], c.args[1]) for c in traced.call_args_list]
        self.assertEqual(result, expected)

    def test_get_data_query(self):
        """ Check that ``_get_data`` executes ``get_query`` with passed args """
        self.get_data_patch.stop()
        session = mock.MagicMock(name='session')
        args = ('val1', 'val2')
        kwargs = {'kw1': 'v1', 'kw2': 'v2'}
        self.queue._get_data(1, session, *args, **kwargs)
        self.get_query.assert_called_with(session, *args, **kwargs)

    def test_get_data_limit(self):
        """ Checks that ``_get_data`` limits query result to ``chunk_size`` """
        self.get_data_patch.stop()
        session = mock.MagicMock(name='session')
        chunk_size = 5
        args = ('val1', 'val2')
        kwargs = {'kw1': 'v1', 'kw2': 'v2'}
        self.queue._get_data(chunk_size, session, *args, **kwargs)
        self.get_query().limit.assert_called_with(chunk_size)

    def test_get_data_result(self):
        """ Check return value of the ``_get_data`` """
        self.get_data_patch.stop()
        session = mock.MagicMock(name='session')
        result = self.queue._get_data(123, session)
        expected = self.get_query().limit().all()
        self.assertEqual(result, expected)

    def test_poll_result(self):
        """ Check return value of the ``poll`` """
        expected = self.get_data()
        result = self.queue.poll(1)
        self.assertEqual(result, expected)

    def test_poll_chunk_size(self):
        """ Check ``poll`` passes ``chunk_size`` to ``_get_data`` """
        chunk_size = 5
        self.queue.poll(chunk_size)
        result = self.get_data.call_args.args[0]
        self.assertEqual(result, chunk_size)

    def test_poll_success_metrics(self):
        """ Check that ``poll`` increases metrics on success """
        expected = [mock.call('queue-{}-poll-success_summ'.format(self.queue.metric_name))]
        with mock.patch.object(self.queue.metrics, 'increment') as increment:
            self.queue.poll()
            self.assertEqual(increment.call_args_list, expected)

    def test_poll_fail_metrics(self):
        """ Check that ``poll`` increases metrics on success """
        expected = 'queue-{}-poll-fail_summ'.format(self.queue.metric_name)
        with mock.patch.object(self.queue.metrics, 'increment') as increment:
            self.get_data.side_effect = Exception('dummy')
            try:
                self.queue.poll()
            except Exception:
                pass
            # can't check against the whole list of calls for there are lots of `Exception`-provoked metrics as well
            increment.assert_called_with(expected)


class SinkQueueTestCase(TestCase):

    TEST_QUEUE_LIMIT = 10
    TEST_FLUSH_INTERVAL = 33

    _data = tuple('a very long string with data where each character is representing a single message in a queue')
    queue = None  # type: queue.SinkDBQueue

    class DummyException(Exception):
        """ Dummy exception to be used in tests as "some exception" and not mask real ones """

    @classmethod
    def data(cls, limit=TEST_QUEUE_LIMIT):
        d = cls._data * ((len(cls._data) // limit) + 1)
        return list(map(Message, list(d[:limit])))

    @classmethod
    def set_up_queue(cls):
        queue.SinkDBQueue.MAXSIZE = cls.TEST_QUEUE_LIMIT
        queue.SinkDBQueue.FLUSH_INTERVAL = cls.TEST_FLUSH_INTERVAL
        cls.queue = queue.SinkDBQueue()

    @classmethod
    def setUpClass(cls):
        cls.set_up_queue()

        # patches for queue methods/internals
        cls.data_patch = mock.patch.object(
            cls.queue.__class__,
            '_data',
            new_callable=mock.PropertyMock,
            return_value=cls.data(),
        )
        cls.save_patch = mock.patch.object(cls.queue, '_save_data', return_value=True)
        cls.put_patch = mock.patch.object(cls.queue, '_put')
        cls.bulk_save_patch = mock.patch.object(cls.queue, 'save_data', return_value=True)
        cls.message_save_patch = mock.patch.object(cls.queue, 'save_message', return_value=True)

        # patches for queue.queue methods
        cls.task_done_patch = mock.patch.object(cls.queue.queue, 'task_done')
        cls.queue_get_patch = mock.patch.object(
            cls.queue.queue,
            'get',
            side_effect=cls.data() + [_queue.Empty],
        )
        cls.queue_put_patch = mock.patch.object(cls.queue.queue, 'put')

        # mocking metrics storage as a whole
        cls.metrics_patch = mock.patch.object(cls.queue, 'metrics')

        # mocking flush loop as a whole
        cls.flush_loop_patch = mock.patch.object(cls.queue, 'flush_loop')

    def setUp(self):
        self._empty_queue()
        self.queue.queue.unfinished_tasks = 0
        self.queue.flush_loop.stop()
        self.queue.flush_loop_started = False

        self.queue_data = self.data_patch.start()
        self.save_data = self.save_patch.start()
        self.bulk_save = self.bulk_save_patch.start()
        self.save_message = self.message_save_patch.start()
        self._put = self.put_patch.start()

        self.task_done = self.task_done_patch.start()
        self.queue_get = self.queue_get_patch.start()
        self.queue_put = self.queue_put_patch.start()

        self.metrics = self.metrics_patch.start()
        self.flush_loop = self.flush_loop_patch.start()

        self.addCleanup(self.data_patch.stop)
        self.addCleanup(self.save_patch.stop)
        self.addCleanup(self.bulk_save_patch.stop)
        self.addCleanup(self.message_save_patch.stop)
        self.addCleanup(self.put_patch.stop)
        self.addCleanup(self.task_done_patch.stop)
        self.addCleanup(self.queue_get_patch.stop)
        self.addCleanup(self.queue_put_patch.stop)
        self.addCleanup(self.metrics_patch.stop)
        self.addCleanup(self.flush_loop_patch.stop)

    def _fill_queue(self, limit=0):
        i = 0
        try:
            for c in self.data():
                self.queue.queue.put_nowait(c)
                i += 1
                if i >= limit:
                    break
        except _queue.Full:
            pass

    def _empty_queue(self):
        try:
            while True:
                self.queue.queue.get_nowait()
                self.queue.queue.task_done()
        except _queue.Empty:
            pass

    def total_metric_increment(self, metric):
        # type: (AnyStr) -> int
        return sum([
            call.args[1] if len(call.args) > 1 else 1
            for call in self.metrics.increment.call_args_list
            if call.args[0] == metric
        ])


class SinkQueueConfigTestCase(SinkQueueTestCase):
    def test_qlimit_default(self):
        self.assertEqual(self.queue.queue.maxsize, self.TEST_QUEUE_LIMIT)

    def test_qlimit_custom(self):
        limit = 7
        q = queue.SinkDBQueue(qlimit=limit)
        self.assertEqual(q.queue.maxsize, limit)

    def test_flush_interval_default(self):
        self.flush_loop_patch.stop()
        self.assertEqual(self.queue.flush_loop.sleep_interval, self.TEST_FLUSH_INTERVAL)

    def test_flush_interval_custom(self):
        self.flush_loop_patch.stop()
        interval = 7
        q = queue.SinkDBQueue(flush_interval=interval)
        self.assertEqual(q.flush_loop.sleep_interval, interval)

    def test_message_class_custom(self):
        expected = QueueTestCase.DummyMessage
        self.assertEqual(queue.SinkDBQueue(message_class=expected).message_class, expected)


class SinkQueueDataTestCase(SinkQueueTestCase):
    def test_data_iterator(self):
        self.data_patch.stop()
        self._fill_queue()
        iterator = self.queue._data
        result = list(iterator)
        self.assertEqual(result, self.data())

    def test_data_iterator_tasks_done_for_all_read_items_on_full_iteration(self):
        self.data_patch.stop()
        self._fill_queue()
        iterator = self.queue._data
        list(iterator)
        n_task_done_calls = len(self.task_done.call_args_list)
        self.assertEqual(n_task_done_calls, self.TEST_QUEUE_LIMIT)

    def test_data_iterator_task_done_for_single_item(self):
        self.data_patch.stop()
        self._fill_queue()
        iterator = self.queue._data
        next(iterator)
        n_task_done_calls = len(self.task_done.call_args_list)
        self.assertEqual(n_task_done_calls, 1)


class SinkQueueFlushTestCase(SinkQueueTestCase):
    def test_save_data_called(self):
        self.queue.flush()
        self.save_data.assert_called_once_with(self.queue._data)

    def test_success_metric_on_success(self):
        self.queue.flush()
        total_increment = self.total_metric_increment('{}-save-success_summ'.format(self.queue.metric_name))
        self.assertEqual(total_increment, 1)

    def test_success_metric_on_failure(self):
        self.save_data.return_value = False
        self.queue.flush()
        total_increment = self.total_metric_increment('{}-save-success_summ'.format(self.queue.metric_name))
        self.assertEqual(total_increment, 0)

    def test_failure_metric_on_success(self):
        self.queue.flush()
        total_increment = self.total_metric_increment('{}-save-fail_summ'.format(self.queue.metric_name))
        self.assertEqual(total_increment, 0)

    def test_failure_metric_on_failure(self):
        self.save_data.return_value = False
        self.queue.flush()
        total_increment = self.total_metric_increment('{}-save-fail_summ'.format(self.queue.metric_name))
        self.assertEqual(total_increment, 1)

    def test_failure_metric_on_exception(self):
        self.save_data.side_effect = self.DummyException('test exception')
        try:
            self.queue.flush()
        except self.DummyException:
            pass
        total_increment = self.total_metric_increment('{}-save-fail_summ'.format(self.queue.metric_name))
        self.assertEqual(total_increment, 1)

    def test_propagate_exception(self):
        self.save_data.side_effect = self.DummyException('test exception')
        self.assertRaises(
            self.DummyException,
            self.queue.flush,
        )

    def test_calls__save_data(self):
        self.queue.flush()
        self.save_data.assert_called()


class SinkQueuePutTestCase(SinkQueueTestCase):
    values = (Message('item'), Message('another'), Message('more'))
    block = mock.ANY     # type: bool
    timeout = -5

    def setUp(self):
        super(SinkQueuePutTestCase, self).setUp()
        self.put_patch.stop()

    @staticmethod
    def side_effect_for_exception_on_n(n, exc=Exception, val=None):
        # type: (int, Optional[Exception], Any) -> list[Optional[Exception]]
        return [val] * n + [exc]

    def test_parameters(self):
        self.queue._put([self.values[0]], self.block, self.timeout)
        self.queue_put.assert_called_with(self.values[0], self.block, timeout=self.timeout)

    def test_all_values_added(self):
        self.queue._put(list(self.values), self.block, self.timeout)
        calls = self.queue_put.call_args_list
        expected = [mock.call(val, mock.ANY, timeout=mock.ANY) for val in self.values[::-1]]
        self.assertEqual(calls, expected)

    def test_saved_values_removed(self):
        list_values = list(self.values)
        written_n = 2
        self.queue_put.side_effect = self.side_effect_for_exception_on_n(written_n)
        try:
            self.queue._put(list_values, self.block, self.timeout)
        except Exception:
            pass
        self.assertEqual(list_values, list(self.values[:-written_n]))

    def test_full_exception_propagated(self):
        self.queue_put.side_effect = _queue.Full
        self.assertRaises(
            _queue.Full,
            self.queue._put,
            self.values,
            self.block,
            self.timeout,
        )

    def test_records_written_metric_on_success(self):
        metric = '{}-put-records-written_summ'.format(self.queue.metric_name)
        n = len(self.values)
        self.queue._put(list(self.values), self.block, self.timeout)
        total_increment = self.total_metric_increment(metric)
        self.assertEqual(total_increment, n)

    def test_records_written_metric_on_failure(self):
        metric = '{}-put-records-written_summ'.format(self.queue.metric_name)
        n = 2
        self.queue_put.side_effect = self.side_effect_for_exception_on_n(n)
        try:
            self.queue._put(list(self.values), self.block, self.timeout)
        except Exception:
            pass
        total_increment = self.total_metric_increment(metric)
        self.assertEqual(total_increment, n)

    def test_no_records_written_metric_on_empty_list(self):
        metric = '{}-put-records-written_summ'.format(self.queue.metric_name)
        self.queue._put([], self.block, self.timeout)
        total_increment = self.total_metric_increment(metric)
        self.assertEqual(total_increment, 0)

    def test_no_records_stuck_metric_on_success(self):
        metric = '{}-put-records-stuck_summ'.format(self.queue.metric_name)
        self.queue._put(list(self.values), self.block, self.timeout)
        total_increment = self.total_metric_increment(metric)
        self.assertEqual(total_increment, 0)

    def test_records_stuck_metric_on_failure(self):
        metric = '{}-put-records-stuck_summ'.format(self.queue.metric_name)
        n_succeed = 2
        expected_stuck = len(self.values) - n_succeed
        self.queue_put.side_effect = self.side_effect_for_exception_on_n(n_succeed, ValueError)
        try:
            self.queue._put(list(self.values), self.block, self.timeout)
        except ValueError:
            pass
        total_increment = self.total_metric_increment(metric)
        self.assertEqual(total_increment, expected_stuck)


class SinkQueuePutWrapperTestCase(SinkQueueTestCase):
    values = ('item', 'another', 'more')

    def test_flush_loop_starts(self):
        self.queue.put(self.values[0])
        self.flush_loop.start.assert_called_once()

    def test_flush_loop_not_restarts(self):
        self.queue.put(self.values[0])
        self.queue.put(self.values[0])
        self.flush_loop.start.assert_called_once()

    def test_concurrent_flush_loop_starts(self):
        def exec_on_event(event, func, *args):
            event.wait()
            func(*args)
        e = threading.Event()
        pool = ThreadPoolExecutor(max_workers=2)
        futures = (
            pool.submit(exec_on_event, e, self.queue.put, self.values[0]),
            pool.submit(exec_on_event, e, self.queue.put, self.values[0]),
        )
        e.set()
        wait(futures)
        self.flush_loop.start.assert_called_once()

    def test_flush_loop_not_started_if_disabled(self):
        self.queue.put(self.values[0], start_flush_loop=False)
        self.flush_loop.start.assert_not_called()

    def test_unmutable_values(self):
        values = mock.MagicMock(spec=Sequence)
        self.queue.put(values)
        self._put.assert_called_with([values], mock.ANY, mock.ANY)

    def test_mutable_values(self):
        values = mock.MagicMock(spec=MutableSequence)
        self.queue.put(values)
        self._put.assert_called_with(values, mock.ANY, mock.ANY)

    def test_exception_propagated(self):
        self._put.side_effect = Exception
        self.assertRaises(
            Exception,
            self.queue.put,
            self.values[0],
        )

    def test_parameters(self):
        block = mock.ANY  # type: bool
        timeout = -5
        self.queue.put(self.values[0], block=block, timeout=timeout)
        self._put.assert_called_with([self.values[0]], block, timeout)

    def test_success_metric_on_success(self):
        metric = '{}-put-success_summ'.format(self.queue.metric_name)
        self.queue.put(list(self.values))
        total_increment = self.total_metric_increment(metric)
        self.assertEqual(total_increment, 1)

    def test_success_metric_on_failure(self):
        metric = '{}-put-success_summ'.format(self.queue.metric_name)
        self._put.side_effect = Exception
        try:
            self.queue.put(list(self.values))
        except Exception:
            pass
        total_increment = self.total_metric_increment(metric)
        self.assertEqual(total_increment, 0)

    def test_fail_metric_on_success(self):
        metric = '{}-put-fail_summ'.format(self.queue.metric_name)
        self.queue.put(list(self.values))
        total_increment = self.total_metric_increment(metric)
        self.assertEqual(total_increment, 0)

    def test_fail_metric_on_failure(self):
        metric = '{}-put-fail_summ'.format(self.queue.metric_name)
        self._put.side_effect = Exception
        try:
            self.queue.put(list(self.values))
        except Exception:
            pass
        total_increment = self.total_metric_increment(metric)
        self.assertEqual(total_increment, 1)


class SinkQueueSaveTestCase(SinkQueueTestCase):
    def setUp(self):
        super(SinkQueueSaveTestCase, self).setUp()
        self.save_patch.stop()

    @mock.patch('search.priemka.yappy.src.yappy_lib.queue.base.session_scope')
    def test_bulk_save(self, session_scope):
        session = mock.Mock(name='session')
        session_scope().__enter__.return_value = session
        data = self.data()
        orig_content = self.queue.data_content
        with mock.patch.object(self.queue, 'data_content') as content:
            content.side_effect = lambda x: list(orig_content(x))
            self.queue._save_data(data)
            self.bulk_save.assert_called_with(content(data), session)

    @mock.patch('search.priemka.yappy.src.yappy_lib.queue.base.session_scope')
    def test_message_save(self, session_scope):
        self.bulk_save.side_effect = NotImplementedError
        session = mock.Mock(name='session')
        session_scope().__enter__.return_value = session
        data = self.data()
        self.queue._save_data(data)
        called_with = list(
            chain([c.args for c in self.save_message.call_args_list])
        )
        expected = list(zip(data, [session] * len(data)))
        self.assertEqual(called_with, expected)


class MessageTestCase(TestCase):
    class DummyMessage(Message):
        pass

    def test_custom_message_from_base_message(self):
        content = 'content'
        metadata = {'key': 'value'}
        msg = Message(content, **metadata)
        custom = self.DummyMessage(msg)
        self.assertEqual((custom.content, custom.metadata), (content, metadata))
