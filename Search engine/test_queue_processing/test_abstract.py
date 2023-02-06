# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import concurrent.futures
import time

from six import PY3, PY2

import mock
from unittest import skipIf

from search.martylib.executor import MonitoredThreadPoolExecutor
from search.martylib.queue_processing.abstract import AbstractQueueProcessor
from search.martylib.test_utils import TestCase


class AbstractProcessorTestCase(TestCase):

    class DummyExecutor(object):
        def __init__(self, **kwargs):
            pass

    class DummyException(Exception):
        """ Dummy exception for test purposes """

    class Worker(AbstractQueueProcessor):
        exceptions = None

        def run_once(self):
            if self.exceptions:
                for exc in self.exceptions:
                    self.submit(AbstractProcessorTestCase.dummy_func, exc)

    @staticmethod
    def dummy_func(exc):
        if exc:
            raise exc

    def test_default_executor_class(self):
        processor = AbstractQueueProcessor()
        expected = AbstractQueueProcessor.DEFAULT_EXECUTOR_CLASS
        self.assertIsInstance(processor.executor, expected)

    def test_custom_executor_class(self):
        expected = AbstractProcessorTestCase.DummyExecutor
        processor = AbstractQueueProcessor(executor_class=expected)
        self.assertIsInstance(processor.executor, expected)

    def test__run_last_run_exceptions_no_exceptions(self):
        worker = self.Worker(executor_class=MonitoredThreadPoolExecutor)
        worker.exceptions = [None, None]
        worker._run()
        worker.executor.shutdown()
        with mock.patch.object(worker, 'run_once'):
            worker._run()
        self.assertEqual(worker.last_run_exceptions, [])

    def _test__run_last_run_exceptions(self):
        exceptions = [self.DummyException(), None, NameError()]
        expected = list(filter(None, exceptions))
        worker = self.Worker(executor_class=MonitoredThreadPoolExecutor)
        worker.exceptions = exceptions
        worker._run()
        worker.executor.shutdown()
        with mock.patch.object(worker, 'run_once'):
            worker._run()
        return worker.last_run_exceptions, expected
        self.assertItemsEqual(worker.last_run_exceptions, expected)

    @skipIf(PY3, 'py2 only')
    def test__run_last_run_exceptions_py2(self):
        self.assertItemsEqual(*self._test__run_last_run_exceptions())

    @skipIf(PY2, 'py3 only')
    def test__run_last_run_exceptions_py3(self):
        self.assertCountEqual(*self._test__run_last_run_exceptions())

    def test__run_last_run_exceptions_empty_after_next_run(self):
        worker = self.Worker(executor_class=MonitoredThreadPoolExecutor)
        worker.exceptions = [self.DummyException(), None, NameError()]
        worker._run()
        worker.exceptions = [None, None]
        concurrent.futures.wait(worker.futures)
        worker._run()
        worker.executor.shutdown()
        with mock.patch.object(worker, 'run_once'):
            worker._run()
        self.assertEqual(worker.last_run_exceptions, [])

    def test__run_handles_exceptions(self):
        worker = AbstractQueueProcessor()
        with mock.patch.object(worker, 'run_once') as run, mock.patch.object(worker, 'handle_exceptions') as handle:
            manager = mock.Mock()
            manager.attach_mock(run, 'run')
            manager.attach_mock(handle, 'handle')
            worker._run()
            manager.assert_has_calls([mock.call.handle(mock.ANY), mock.call.run()])

    def test_liveness_probe_not_started(self):
        worker = AbstractQueueProcessor()
        self.assertFalse(worker.liveness_probe())

    def test_liveness_probe_just_started(self):
        start_time = 1000
        delay = 100
        worker = self.Worker(sleep_interval=1, delay=delay)
        with \
                mock.patch.object(worker, 'is_alive', return_value=True), \
                mock.patch.object(worker.__class__, 'start_time', new_callable=mock.PropertyMock) as mock_start, \
                mock.patch('{}.now'.format(AbstractQueueProcessor.__module__)) as mock_now:
            mock_start().timestamp.return_value = start_time
            mock_now().timestamp.return_value = start_time + 0.5 * delay
            self.assertTrue(worker.liveness_probe())

    def test_liveness_probe_was_free_recently(self):
        start_time = 1000
        sleep_interval = 10
        worker = self.Worker(sleep_interval=sleep_interval)
        with \
                mock.patch.object(worker, 'is_alive', return_value=True), \
                mock.patch.object(worker.__class__, 'start_time', new_callable=mock.PropertyMock) as mock_start, \
                mock.patch.object(worker.__class__, 'is_free', new_callable=mock.PropertyMock) as mock_free, \
                mock.patch('{}.now'.format(AbstractQueueProcessor.__module__)) as mock_now:
            mock_start().timestamp.return_value = start_time
            mock_now().timestamp.return_value = start_time + worker._delay
            mock_free.return_value = True
            worker._run()  # set last attempt timestamp
            mock_now().timestamp.return_value += worker.sleep_interval
            mock_free.return_value = False
            worker._run()  # run with exceeded worker's capacity
            self.assertTrue(worker.liveness_probe())

    def test_liveness_probe_wasnt_free_for_a_while(self):
        start_time = 1000
        sleep_interval = 10
        worker = self.Worker(sleep_interval=sleep_interval)
        with \
                mock.patch.object(worker, 'is_alive', return_value=True), \
                mock.patch.object(worker.__class__, 'start_time', new_callable=mock.PropertyMock) as mock_start, \
                mock.patch.object(worker.__class__, 'is_free', new_callable=mock.PropertyMock) as mock_free, \
                mock.patch('{}.now'.format(AbstractQueueProcessor.__module__)) as mock_now:
            mock_start().timestamp.return_value = start_time
            mock_now().timestamp.return_value = start_time + worker._delay
            mock_free.return_value = True
            worker._run()  # set last attempt timestamp
            mock_now().timestamp.return_value += 1.2 * worker.ttl_after_last_run
            mock_free.return_value = False
            worker._run()  # run with exceeded worker's capacity
            self.assertFalse(worker.liveness_probe())
