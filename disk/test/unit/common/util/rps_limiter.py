# -*- coding: utf-8 -*-

import unittest
import time
from mpfs.common.util.rps_limiter import InMemoryRPSLimiter, LimitExceed


class InMemoryRPSLimiterTestCase(unittest.TestCase):
    def test_different_limits(self):
        for limit in range(1, 5):
            rps_limiter = InMemoryRPSLimiter(limit)
            for i in range(10):
                if i + 1 <= limit:
                    rps_limiter.check()
                else:
                    with self.assertRaises(LimitExceed):
                        rps_limiter.check()

    def test_check(self):
        limit = 3
        duration = 2
        rps_limiter = InMemoryRPSLimiter(limit)
        start_ts = time.time()
        allowed = limited = 0
        while time.time() - start_ts < duration:
            try:
                rps_limiter.check()
            except LimitExceed:
                limited += 1
            else:
                allowed += 1
        assert limited > 100
        assert limit * duration <= allowed <= limit * (duration + 1)

    def test_block_until_allowed(self):
        limit = 3
        duration = 2
        rps_limiter = InMemoryRPSLimiter(limit)
        start_ts = time.time()
        allowed = 0
        while time.time() - start_ts < duration:
            rps_limiter.block_until_allowed()
            allowed += 1
        assert limit * duration <= allowed <= limit * (duration + 1)
