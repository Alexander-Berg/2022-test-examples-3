# coding=utf-8
from __future__ import unicode_literals

from travel.avia.shared_flights.lib.python.date_utils.date_shift import get_days_shift, shift_week_days


def test_shift_week_days():
    assert shift_week_days(6, 1) == 7
    assert shift_week_days(7, 1) == 1
    assert shift_week_days(7, 6) == 6
    assert shift_week_days(7, 7) == 7
    assert shift_week_days(124, 1) == 235
    assert shift_week_days(1234567, 4) == 1234567
    assert shift_week_days(123567, 1) == 123467


def test_get_days_shift():
    assert get_days_shift(1, 1) == 0
    assert get_days_shift(1, 2) == 1
    assert get_days_shift(1, 3) == 2
    assert get_days_shift(7, 2) == 2
    assert get_days_shift(2, 1) == 6
    assert get_days_shift(12, 23) == 1
    assert get_days_shift(17, 12) == 1
    assert get_days_shift(123467, 123456) == 2
    assert get_days_shift(123467, 134567) == 4
    assert get_days_shift(0, 1) == 0
