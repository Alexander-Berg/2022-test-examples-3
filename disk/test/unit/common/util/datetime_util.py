# -*- coding: utf-8 -*-

import datetime

from unittest import TestCase

from test.base import time_machine
from mpfs.common.util.datetime_util import next_datetime_with_hour, SafeTimestampDateTime


class NextDatetimeWithHourTestCase(TestCase):
    def test_hour_greater_than_current(self):
        with time_machine(datetime.datetime(2010, 10, 10, 16, 35, 10, 22)):
            result = next_datetime_with_hour(17)
        assert result == datetime.datetime(2010, 10, 10, 17, 00, 00)

    def test_hour_less_than_current(self):
        with time_machine(datetime.datetime(2010, 10, 10, 16, 35, 10, 22)):
            result = next_datetime_with_hour(16)
        assert result == datetime.datetime(2010, 10, 11, 16, 00, 00)


class SafeTimestampDateTimeTestCase(TestCase):
    def test_fromtimestamp_negative(self):
        """Отрицательные значения должны нормальна преобразовываться в дату без исключений"""
        SafeTimestampDateTime.fromtimestamp(-62135769583)
