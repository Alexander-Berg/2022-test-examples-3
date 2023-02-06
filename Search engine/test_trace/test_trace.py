# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import threading

from search.martylib.test_utils import TestCase
from search.martylib.trace import TraceManager, ThreadedTraceManager, GlobalTraceManager, trace, global_trace
from search.martylib.unistat.metrics import GlobalMetricStorage


class TestTraceManager(TestCase):
    metrics = GlobalMetricStorage()

    def test_slots(self):
        for c in (TraceManager, ThreadedTraceManager, GlobalTraceManager):
            self.assertFalse(hasattr(c(), '__dict__'), msg='class {} is not slotted'.format(c.__name__))

    def test_overriding(self):
        tm = ThreadedTraceManager()

        with trace(foo='alpha'):
            self.assertEqual(tm.compiled_layers, dict(foo='alpha'))

            with trace(foo='bravo'):
                self.assertEqual(tm.compiled_layers, dict(foo='bravo'))

            self.assertEqual(tm.compiled_layers, dict(foo='alpha'))

        self.assertEqual(tm.compiled_layers, {})

    def test_threading(self):
        results = {}

        def _test(target, **kwargs):
            with trace(**kwargs):
                results[target] = ThreadedTraceManager().compiled_layers

        bravo_thread = threading.Thread(target=_test, args=('bravo', ), kwargs=dict(foo='bravo'))
        charlie_thread = threading.Thread(target=_test, args=('charlie', ), kwargs=dict(foo='charlie'))

        with trace(foo='alpha'):
            for thread in (bravo_thread, charlie_thread):
                thread.start()
                thread.join()

            self.assertEqual(ThreadedTraceManager().compiled_layers, dict(foo='alpha'))

        self.assertEqual(results['bravo'], dict(foo='bravo'))
        self.assertEqual(results['charlie'], dict(foo='charlie'))

    def test_global(self):
        results = {}

        def _test(target, **kwargs):
            with global_trace(**kwargs):
                results[target] = GlobalTraceManager().compiled_layers

        bravo_thread = threading.Thread(target=_test, args=('bravo',), kwargs=dict(bar='bravo'))
        charlie_thread = threading.Thread(target=_test, args=('charlie',), kwargs=dict(bar='charlie'))

        with global_trace(foo='alpha'):
            for thread in (bravo_thread, charlie_thread):
                thread.start()
                thread.join()

            self.assertEqual(GlobalTraceManager().compiled_layers, dict(foo='alpha'))

        self.assertEqual(results['bravo'], dict(foo='alpha', bar='bravo'))
        self.assertEqual(results['charlie'], dict(foo='alpha', bar='charlie'))

    def test_suppress(self):
        with trace(_suppress=ZeroDivisionError):
            # noinspection PyStatementEffect
            1 / 0

        with trace(_suppress=(ZeroDivisionError, )):
            # noinspection PyStatementEffect
            1 / 0

    def test_untraced(self):
        before = self.metrics.to_protobuf().numerical['global-traced-exceptions-ZeroDivisionError_summ']

        with self.assertRaises(ZeroDivisionError):
            with trace(_untraced_exceptions=ZeroDivisionError):
                # noinspection PyStatementEffect
                1 / 0

        with self.assertRaises(ZeroDivisionError):
            with trace(_untraced_exceptions=(ZeroDivisionError, )):
                # noinspection PyStatementEffect
                1 / 0

        after = self.metrics.to_protobuf().numerical['global-traced-exceptions-ZeroDivisionError_summ']

        self.assertEqual(before, after)
