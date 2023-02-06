# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

from search.martylib.test_utils import TestCase
from search.martylib.executor import TracedThreadPoolExecutor
from search.martylib.trace import ThreadedTraceManager, trace


class TestTracedPool(TestCase):
    pool = TracedThreadPoolExecutor(10)

    def test_threading(self):
        results = {}

        def _test(target, **kwargs):
            with trace(**kwargs):
                results[target] = ThreadedTraceManager().compiled_layers

        futures = []

        with trace(foo='alpha') as tm:
            futures.append(
                self.pool.submit(
                    _test,
                    'beta',
                    foo='beta'
                )
            )

            futures.append(
                self.pool.submit(
                    _test,
                    'casper',
                    foo='casper'
                )
            )

            for future in futures:
                future.result()

        self.assertEqual(results['beta'], dict(foo='beta'))
        self.assertEqual(results['casper'], dict(foo='casper'))
