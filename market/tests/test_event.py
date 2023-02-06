# coding: utf-8

import time
import unittest

from market.idx.pylibrary.metrics.event import Event


class EventTestCase(unittest.TestCase):

    def test_event_start_end(self):
        e = Event('test-name')
        self.assertEqual(e.name, 'test-name')

        start_time = int(time.time())
        self.assertIsNone(e.start)
        with e:
            self.assertGreaterEqual(e.start, start_time)
            self.assertIsNone(e.end)

        end_time = int(time.time())
        self.assertLessEqual(e.end, end_time)

    def test_event_duration(self):
        e = Event('test-name')

        self.assertIsNone(e.duration)
        with e:
            self.assertIsNotNone(e.start)
            self.assertIsNone(e.duration)
            self.assertIsNone(e.end)

        self.assertIsNotNone(e.end)
        self.assertEqual(e.duration, e.end - e.start)
