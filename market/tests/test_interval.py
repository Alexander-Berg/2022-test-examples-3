# -*- coding: utf-8 -*-

import pytest

from market.idx.cron.update_cron_available.src.interval import (
    Interval,
    On,
    Every,
    FromTo,
    ANY,
)


def test_interval_is_valid():
    assert Interval().is_valid


@pytest.mark.parametrize('interval, is_valid', [
    (Interval(minute=Every(5)), True),
    (Interval(minute=On(5)), True),

    (Interval(minute=Every(0)), True),
    (Interval(minute=On(0)), True),

    (Interval(minute=Every(59)), True),
    (Interval(minute=On(59)), True),

    (Interval(minute=Every(-1)), False),
    (Interval(minute=On(-1)), False),

    (Interval(minute=Every(60)), False),
    (Interval(minute=On(60)), False),

    (Interval(minute=On(5, 10, 45)), True),
    (Interval(minute=On(5, 10, 45, 60)), False),
    (Interval(minute=On(5, 10, 45, -1)), False),
])
def test_interval_minutes_is_valid(interval, is_valid):
    assert is_valid == interval.is_valid


@pytest.mark.parametrize('interval, is_valid', [
    (Interval(minute=On(0), hour=Every(5)), True),
    (Interval(minute=On(0), hour=On(5)), True),

    (Interval(minute=On(0), hour=Every(0)), True),
    (Interval(minute=On(0), hour=On(0)), True),

    (Interval(minute=On(0), hour=Every(23)), True),
    (Interval(minute=On(0), hour=On(23)), True),

    (Interval(minute=On(0), hour=Every(-1)), False),
    (Interval(minute=On(0), hour=On(-1)), False),

    (Interval(minute=On(0), hour=Every(24)), False),
    (Interval(minute=On(0), hour=On(25)), False),

    (Interval(minute=On(0), hour=On(5, 10, 17)), True),
    (Interval(minute=On(0), hour=On(5, 10, 17, 24)), False),
    (Interval(minute=On(0), hour=On(5, 10, 45, -10)), False),
])
def test_interval_hour_is_valid(interval, is_valid):
    assert is_valid == interval.is_valid


def test_interval_cron_str():
    assert str(Interval()) == ' '.join([ANY]*5)


@pytest.mark.parametrize('interval, cron_str', [
    (Interval(minute=Every(5)), '*/5 * * * *'),
    (Interval(minute=On(5)), '5 * * * *'),

    (Interval(minute=Every(0)), '*/0 * * * *'),
    (Interval(minute=On(0)), '0 * * * *'),

    (Interval(minute=Every(59)), '*/59 * * * *'),
    (Interval(minute=On(59)), '59 * * * *'),

    (Interval(minute=On(5, 10, 45)), '5,10,45 * * * *'),
])
def test_interval_minutes_cron_str(interval, cron_str):
    assert cron_str == interval.cron_str


@pytest.mark.parametrize('interval, cron_str', [
    (Interval(hour=Every(5)), '* */5 * * *'),
    (Interval(hour=On(5)), '* 5 * * *'),

    (Interval(hour=Every(0)), '* */0 * * *'),
    (Interval(hour=On(0)), '* 0 * * *'),

    (Interval(hour=Every(23)), '* */23 * * *'),
    (Interval(hour=On(23)), '* 23 * * *'),

    (Interval(hour=On(5, 10, 17)), '* 5,10,17 * * *'),
])
def test_interval_hour_cron_str(interval, cron_str):
    assert cron_str == interval.cron_str


@pytest.mark.parametrize('interval, is_valid', [
    (Interval(minute=On(0), hour=On(0), dow=FromTo(1, 6)), True),
    (Interval(minute=On(0), hour=On(0), dow=FromTo(0, 6)), True),
    (Interval(minute=On(0), hour=On(0), dow=FromTo(0, 7)), False),
    (Interval(minute=On(0), hour=On(0), dow=FromTo(2, 9)), False),
])
def test_dow_from_to_is_valid(interval, is_valid):
    assert is_valid == interval.is_valid


@pytest.mark.parametrize('interval, is_valid', [
    (Interval(hour=On(0)), False),
    (Interval(dow=On(0)), False),
    (Interval(day=On(0)), False),
    (Interval(month=On(0)), False),
    (Interval(minute=On(0), dow=On(0)), False),
    (Interval(minute=On(0), day=On(0), month=On(0)), False),
    (Interval(minute=On(0), hour=On(0)), True),
])
def test_field_combinations(interval, is_valid):
    assert is_valid == interval.is_valid


@pytest.mark.parametrize('interval, cron_str', [
    (Interval(dow=FromTo(1, 6)), '* * * * 1-6'),
    (Interval(dow=FromTo(0, 6)), '* * * * 0-6'),
    (Interval(dow=FromTo(2, 7)), '* * * * 2-7'),
    (Interval(dow=FromTo(2, 9)), '* * * * 2-9'),
])
def test_dow_from_to_cron_str(interval, cron_str):
    assert interval.cron_str == cron_str
