# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import time

from search.martylib.test_utils import TestCase
from search.martylib.threading_utils import Interval


class TestInterval(TestCase):
    def test_no_errors(self):
        result = []

        def f():
            result.append(1)

        i = Interval(sleep_interval=0.5, target=f)
        i.start()
        time.sleep(2.1)
        i.stop()

        self.assertEqual(result, [1] * 5)

    def test_delay(self):
        result = []

        def f():
            result.append(1)

        i = Interval(target=f, sleep_interval=0.5, delay=1.0)
        i.start()
        time.sleep(2.1)
        i.stop()

        self.assertEqual(result, [1] * 3)
