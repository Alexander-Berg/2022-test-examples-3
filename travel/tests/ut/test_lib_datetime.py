# -*- coding: utf-8 -*-
from __future__ import unicode_literals

from datetime import date

from travel.cpa.lib.lib_datetime import iter_day, iter_day_reversed, iter_month, iter_periods


def test_iter_day():
    d = list(iter_day(date(year=2, month=3, day=1), date(year=1, month=5, day=1)))
    assert [] == d

    d = list(iter_day(date(year=1, month=5, day=1), date(year=1, month=1, day=1)))
    assert [] == d

    d = list(iter_day(date(year=1, month=3, day=7), date(year=1, month=3, day=1)))
    assert [] == d

    d = list(iter_day(date(year=1, month=3, day=1), date(year=1, month=3, day=1)))
    assert [date(year=1, month=3, day=1)] == d

    d = list(iter_day(date(year=1, month=3, day=1), date(year=1, month=3, day=3)))
    assert [date(year=1, month=3, day=1), date(year=1, month=3, day=2), date(year=1, month=3, day=3)] == d

    d = list(iter_day(date(year=1, month=1, day=31), date(year=1, month=2, day=1)))
    assert [date(year=1, month=1, day=31), date(year=1, month=2, day=1)] == d

    d = list(iter_day(date(year=1, month=1, day=31), date(year=1, month=2, day=1)))
    assert [date(year=1, month=1, day=31), date(year=1, month=2, day=1)] == d

    d = list(iter_day(date(year=1, month=12, day=31), date(year=1, month=12, day=31)))
    assert [date(year=1, month=12, day=31)] == d

    d = list(iter_day(date(year=1, month=1, day=5), date(year=1, month=1, day=9)))
    expected = [
        date(year=1, month=1, day=5),
        date(year=1, month=1, day=6),
        date(year=1, month=1, day=7),
        date(year=1, month=1, day=8),
        date(year=1, month=1, day=9),
    ]
    assert expected == d


def test_iter_day_reversed():
    d = list(iter_day_reversed(date(year=1, month=5, day=1), date(year=2, month=3, day=1)))
    assert [] == d

    d = list(iter_day_reversed(date(year=1, month=1, day=1), date(year=1, month=5, day=1)))
    assert [] == d

    d = list(iter_day_reversed(date(year=1, month=3, day=1), date(year=1, month=3, day=7)))
    assert [] == d

    d = list(iter_day_reversed(date(year=1, month=3, day=1), date(year=1, month=3, day=1)))
    assert [date(year=1, month=3, day=1)] == d

    d = list(iter_day_reversed(date(year=1, month=3, day=3), date(year=1, month=3, day=1)))
    assert [date(year=1, month=3, day=3), date(year=1, month=3, day=2), date(year=1, month=3, day=1)] == d

    d = list(iter_day_reversed(date(year=1, month=2, day=1), date(year=1, month=1, day=31)))
    assert [date(year=1, month=2, day=1), date(year=1, month=1, day=31)] == d

    d = list(iter_day_reversed(date(year=1, month=2, day=1), date(year=1, month=1, day=31)))
    assert [date(year=1, month=2, day=1), date(year=1, month=1, day=31)] == d

    d = list(iter_day_reversed(date(year=1, month=12, day=31), date(year=1, month=12, day=31)))
    assert [date(year=1, month=12, day=31)] == d

    d = list(iter_day_reversed(date(year=1, month=1, day=9), date(year=1, month=1, day=5)))
    expected = [
        date(year=1, month=1, day=9),
        date(year=1, month=1, day=8),
        date(year=1, month=1, day=7),
        date(year=1, month=1, day=6),
        date(year=1, month=1, day=5),
    ]
    assert expected == d


def test_iter_month():
    m = list(iter_month(date(year=2, month=3, day=1), date(year=1, month=5, day=1)))
    assert [] == m

    m = list(iter_month(date(year=1, month=5, day=1), date(year=1, month=1, day=1)))
    assert [] == m

    m = list(iter_month(date(year=1, month=3, day=1), date(year=1, month=3, day=1)))
    assert [date(year=1, month=3, day=1)] == m

    m = list(iter_month(date(year=1, month=3, day=1), date(year=1, month=5, day=1)))
    assert [date(year=1, month=3, day=1), date(year=1, month=4, day=1), date(year=1, month=5, day=1)] == m

    m = list(iter_month(date(year=1, month=12, day=5), date(year=2, month=1, day=1)))
    assert [date(year=1, month=12, day=1), date(year=2, month=1, day=1)] == m

    m = list(iter_month(date(year=1, month=12, day=1), date(year=1, month=12, day=1)))
    assert [date(year=1, month=12, day=1)] == m

    m = list(iter_month(date(year=1, month=12, day=1), date(year=3, month=1, day=1)))
    expected = [
        date(year=1, month=12, day=1),
        date(year=2, month=1, day=1),
        date(year=2, month=2, day=1),
        date(year=2, month=3, day=1),
        date(year=2, month=4, day=1),
        date(year=2, month=5, day=1),
        date(year=2, month=6, day=1),
        date(year=2, month=7, day=1),
        date(year=2, month=8, day=1),
        date(year=2, month=9, day=1),
        date(year=2, month=10, day=1),
        date(year=2, month=11, day=1),
        date(year=2, month=12, day=1),
        date(year=3, month=1, day=1),
    ]
    assert expected == m


def test_iter_periods():
    m = list(iter_periods(date(year=2020, month=7, day=2), date(year=1970, month=7, day=2), 7))
    assert [] == m

    m = list(iter_periods(date(year=2020, month=7, day=2), date(year=2020, month=7, day=2), 7))
    assert [] == m

    m = list(iter_periods(date(year=2020, month=6, day=29), date(year=2020, month=7, day=3), 7))
    assert [(date(year=2020, month=6, day=29), date(year=2020, month=7, day=3))] == m

    m = list(iter_periods(date(year=2020, month=6, day=29), date(year=2020, month=7, day=6), 7))
    assert [(date(year=2020, month=6, day=29), date(year=2020, month=7, day=6))] == m

    m = list(iter_periods(date(year=2020, month=6, day=29), date(year=2020, month=7, day=7), 7))
    assert [
        (date(year=2020, month=6, day=29), date(year=2020, month=7, day=6)),
        (date(year=2020, month=7, day=6), date(year=2020, month=7, day=7))
    ] == m

    m = list(iter_periods(date(year=2020, month=6, day=29), date(year=2020, month=7, day=7), 3))
    assert [
        (date(year=2020, month=6, day=29), date(year=2020, month=7, day=2)),
        (date(year=2020, month=7, day=2), date(year=2020, month=7, day=5)),
        (date(year=2020, month=7, day=5), date(year=2020, month=7, day=7))
    ] == m

    m = list(iter_periods(date(year=2020, month=6, day=29), date(year=2020, month=7, day=3), 1))
    assert [
        (date(year=2020, month=6, day=29), date(year=2020, month=6, day=30)),
        (date(year=2020, month=6, day=30), date(year=2020, month=7, day=1)),
        (date(year=2020, month=7, day=1), date(year=2020, month=7, day=2)),
        (date(year=2020, month=7, day=2), date(year=2020, month=7, day=3))
    ] == m
