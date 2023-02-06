#!/usr/bin/env python
# -*- coding: utf-8 -*-
import datetime

import pytest

from crypta.profile.lib import date_helpers


@pytest.mark.parametrize('test_input,expected', [
    (datetime.datetime(year=2019, month=8, day=12), '2019-08-12'),
    (datetime.datetime(year=2025, month=8, day=12), '2025-08-12'),
    (datetime.date(year=2019, month=8, day=12), '2019-08-12'),
    (datetime.date(year=1995, month=8, day=12), '1995-08-12'),
])
def test_to_date_string(test_input, expected):
    assert date_helpers.to_date_string(test_input) == expected


@pytest.mark.parametrize('str_date,date_format,expected', [
    ('2019-08-12', date_helpers.DATE_FORMAT, datetime.datetime(year=2019, month=8, day=12)),
    ('2025-08-12', date_helpers.DATE_FORMAT, datetime.datetime(year=2025, month=8, day=12)),
    ('2020-10-09 20:50:43', date_helpers.DATETIME_FORMAT, datetime.datetime(year=2020, month=10, day=9, hour=20, minute=50, second=43)),
])
def test_from_date_string_to_datetime(str_date, date_format, expected):
    assert date_helpers.from_date_string_to_datetime(str_date, date_format) == expected


@pytest.mark.parametrize('str_date,date_format,expected', [
    ('2019-08-12', date_helpers.DATE_FORMAT, datetime.datetime(year=2019, month=8, day=12)),
    ('2025-08-12', date_helpers.DATE_FORMAT, datetime.datetime(year=2025, month=8, day=12)),
    ('2020-10-09 20:50:43', date_helpers.DATETIME_FORMAT, datetime.datetime(year=2020, month=10, day=9, hour=20, minute=50, second=43)),
])
def test_from_date_string_to_msk_datetime(str_date, date_format, expected):
    assert date_helpers.from_date_string_to_msk_datetime(str_date, date_format) == date_helpers.MSK.localize(expected)


@pytest.mark.parametrize('test_input,expected', [
    (datetime.datetime(year=2019, month=8, day=12, hour=20, minute=50, second=43),
     datetime.datetime(year=2019, month=8, day=11, hour=20, minute=50, second=43)),
    (datetime.date(year=2019, month=8, day=12), datetime.date(year=2019, month=8, day=11)),
    ('2019-08-12', '2019-08-11'),
])
def test_get_yesterday(test_input, expected):
    assert date_helpers.get_yesterday(test_input) == expected


@pytest.mark.parametrize('test_input,expected', [
    (datetime.datetime(year=2019, month=8, day=12, hour=20, minute=50, second=43),
     datetime.datetime(year=2019, month=8, day=13, hour=20, minute=50, second=43)),
    (datetime.date(year=2019, month=8, day=12), datetime.date(year=2019, month=8, day=13)),
    ('2019-08-12', '2019-08-13'),
])
def test_get_tomorrow(test_input, expected):
    assert date_helpers.get_tomorrow(test_input) == expected


@pytest.mark.parametrize('test_input,expected', [
    ('2025-01-03', 1735905600),
    ('2019-08-12', 1565611200),
])
def test_get_utc_noon_timestamp(test_input, expected):
    assert date_helpers.from_utc_date_string_to_noon_timestamp(test_input) == expected


@pytest.mark.parametrize('start_date,end_date,expected', [
    ('2019-01-03', '2019-01-05', ['2019-01-03', '2019-01-04', '2019-01-05']),
    ('2019-01-03', '2019-01-03', ['2019-01-03']),
    ('2019-01-03', '2019-01-02', []),
])
def test_generate_date_strings(start_date, end_date, expected):
    assert date_helpers.generate_date_strings(start_date, end_date) == expected


@pytest.mark.parametrize('end_date,back_days,expected', [
    ('2020-09-10', 10, ['2020-09-01', '2020-09-02', '2020-09-03',
                        '2020-09-04', '2020-09-05', '2020-09-06',
                        '2020-09-07', '2020-09-08', '2020-09-09',
                        '2020-09-10']),
    ('2019-01-03', 2, ['2019-01-02', '2019-01-03']),
    ('2019-01-03', 1, ['2019-01-03']),
    ('2019-01-03', 0, []),
])
def test_generate_back_dates(end_date, back_days, expected):
    assert date_helpers.generate_back_dates(end_date, back_days) == expected
