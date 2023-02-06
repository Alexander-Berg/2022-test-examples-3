# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from builtins import range
import threading
import time as os_time

import mock

from travel.rasp.library.python.common23.utils_db.threadutils import Ticker


class Target(object):
    def __init__(self):
        self.condition = threading.Condition()
        self.mock_call = mock.Mock()

    def __call__(self, *args, **kwargs):
        with self.condition:
            self.mock_call(*args, **kwargs)
            self.condition.notify()


class TestTicker(object):
    def test_target(self):
        target = Target()
        ticker = Ticker(0.001, target=target, args=(1, 2), kwargs={'b': 42})
        self._check_ticker(ticker, target)

    def test_subclass(self):
        target = Target()

        class SubTicker(Ticker):
            def tick(self, *args, **kwargs):
                target(*args, **kwargs)

        ticker = SubTicker(0.001, args=(1, 2), kwargs={'b': 42})
        self._check_ticker(ticker, target)

    def _check_ticker(self, ticker, target):
        with target.condition:
            ticker.start()
            assert ticker.is_alive()
            time = os_time.time()
            intervals = []
            for _ in range(6):
                target.condition.wait()
                next_time = os_time.time()
                intervals.append(next_time - time)
                time = next_time

            target.mock_call.assert_called_with(1, 2, b=42)

        assert 0.006 <= sum(intervals) < 0.04

        ticker.stop()
        ticker.join(timeout=0.002)
        assert not ticker.is_alive()
