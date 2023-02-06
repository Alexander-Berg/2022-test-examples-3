# coding: utf-8

from __future__ import absolute_import
from __future__ import division
from __future__ import unicode_literals

import datetime

from search.martylib.core.date_utils import get_datetime, now
from search.martylib.test_utils import TestCase
from search.martylib.business_hours import BusinessHours


class TestBusinessHours(TestCase):
    def test_non_empty_intervals(self):
        _start = get_datetime('2018-11-01T00:00:00+03:00')  # Monday, 1.11.2018
        _end = get_datetime('2018-11-11T00:00:00+03:00')    # Sunday, 11.11.2018

        self.assertEqual(BusinessHours(_start, _end).days, 7)

        # 8 working hours per day
        self.assertEqual(BusinessHours(_start, _end).hours, 7 * 8)

    def test_empty_intervals(self):
        _now = now()
        self.assertEqual(BusinessHours(_now, _now).minutes, 0)
        self.assertEqual(BusinessHours(_now, _now - datetime.timedelta(seconds=1)).minutes, 0)
