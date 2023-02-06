# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import time
import concurrent.futures

from search.martylib.test_utils import TestCase
from search.martylib.throttle import BaseThrottle, BaseThrottleAdjuster, throttled


class TestThrottle(TestCase):
    def test_invalid_init(self):
        for throughput, window_size, precision in (
            (1, 0, 1),
            (1, 1, 0),
            (None, 1, 1),
            (1, None, 1),
            (1, 1, None),
            (0.1, 1, 1),
            (1, 0.1, 1),
            (1, 1, 0.1),
        ):
            with self.assertRaises(ValueError):
                BaseThrottle(throughput, window_size, precision)

    def test_slow_throttle(self):
        t = BaseThrottle(1, 1, 1)
        start = time.time()

        for _ in range(5):
            t.throttle()

        self.assertGreaterEqual(time.time(), start + 4)

    def test_multithreading(self):
        t = BaseThrottle(1, 1, 1)
        pool = concurrent.futures.ThreadPoolExecutor(5)

        @throttled(t)
        def fn():
            pass

        start = time.time()
        futures = []
        for _ in range(5):
            futures.append(pool.submit(fn))

        for f in futures:
            f.result()

        self.assertGreaterEqual(time.time(), start + 4)

    def test_decorator(self):
        t = BaseThrottle(1, 1, 1)

        @throttled(t)
        def fn():
            pass

        start = time.time()

        for _ in range(5):
            fn()

        self.assertGreaterEqual(time.time(), start + 4)

    def test_context_manager(self):
        t = BaseThrottle(1, 1, 1)

        start = time.time()

        for _ in range(5):
            with t:
                pass

        self.assertGreaterEqual(time.time(), start + 4)

    def test_hits_adjustments(self):
        t = BaseThrottle(1, 1)

        with t:
            pass

        self.assertEqual(len(t.hits), 1)

        with t:
            pass

        self.assertEqual(len(t.hits), 1)

        t.adjust(throughput=10)

        with t:
            pass

        self.assertEqual(len(t.hits), 2)
        latest_hit = t.hits[-1]

        t.adjust(throughput=1)
        self.assertEqual(len(t.hits), 1)
        self.assertEqual(t.hits[0], latest_hit)

    def test_zero_or_negative_throughput(self):
        self.assertFalse(BaseThrottle(-1, 1)._should_throttle)
        self.assertFalse(BaseThrottle(0, 1)._should_throttle)

        t = BaseThrottle(-1, 1)
        start = time.time()

        for _ in range(5):
            with t:
                pass

        self.assertLessEqual(time.time(), start + 1)


class TestThrottleAdjuster(TestCase):
    def test_base(self):
        class DummyAdjuster(BaseThrottleAdjuster):
            def get_adjustments(self):
                return 10, 1

        t = BaseThrottle(1, 1, 1)
        a = DummyAdjuster()
        a(t)

        start = time.time()

        # Should take 5 seconds – throughput is 1 RPS.
        for _ in range(5):
            with t:
                pass

        a.adjust()

        # Should take less then a second – throughput is 10 RPS.
        for _ in range(5):
            with t:
                pass

        self.assertGreaterEqual(time.time(), start + 4)
        self.assertLessEqual(time.time(), start + 5)
