# coding=utf-8
from __future__ import unicode_literals

from datetime import datetime

from travel.avia.shared_flights.lib.python.date_utils.date_index import DateIndex

base_date = datetime(
    year=2020,
    month=2,
    day=5,
    hour=12,
    minute=0,
)

di = DateIndex(base_date)

next_day = datetime(year=2020, month=2, day=6, hour=12, minute=0)
next_month = datetime(year=2020, month=3, day=5, hour=12, minute=0)
next_year = datetime(year=2021, month=2, day=5, hour=12, minute=0)
previous_day = datetime(year=2020, month=2, day=4, hour=12, minute=0)
previous_year = datetime(year=2019, month=2, day=5, hour=12, minute=0)


def test_get_index():
    assert di.get_index(next_day) == 1
    assert di.get_index(next_month) == 29
    assert di.get_index(next_year) == 366
    assert di.get_index(previous_day) == -1
    assert di.get_index(previous_year) == -365


def test_get_index_for_date_str():
    assert di.get_index_for_date_str('2020-02-06') == 1
    assert di.get_index_for_date_str('2020-03-05') == 29
    assert di.get_index_for_date_str('2021-02-05') == 366
    assert di.get_index_for_date_str('2020-02-04') == -1
    assert di.get_index_for_date_str('2019-02-05') == -365


def test_get_date():
    assert di.get_date(0) == base_date
    assert di.get_date(1) == next_day
    assert di.get_date(29) == next_month
    assert di.get_date(366) == next_year
    assert di.get_date(-1) == previous_day
    assert di.get_date(-365) == previous_year


def test_get_str_date():
    assert di.get_date_str(0) == '2020-02-05'
    assert di.get_date_str(1) == '2020-02-06'
    assert di.get_date_str(29) == '2020-03-05'
    assert di.get_date_str(366) == '2021-02-05'
    assert di.get_date_str(-1) == '2020-02-04'
    assert di.get_date_str(-365) == '2019-02-05'


def test_get_weekday_by_index():
    assert di.get_weekday_by_index(0) == 3
    assert di.get_weekday_by_index(1) == 4
    assert di.get_weekday_by_index(29) == 4
    assert di.get_weekday_by_index(366) == 5
    assert di.get_weekday_by_index(-1) == 2
    assert di.get_weekday_by_index(-365) == 2
    assert di.get_weekday_by_index(4) == 7
    assert di.get_weekday_by_index(5) == 1


def test_get_weekday_by_date():
    assert di.get_weekday_by_date(base_date) == 3
    assert di.get_weekday_by_date(next_day) == 4
    assert di.get_weekday_by_date(next_month) == 4
    assert di.get_weekday_by_date(next_year) == 5
    assert di.get_weekday_by_date(previous_day) == 2
    assert di.get_weekday_by_date(previous_year) == 2


def test_get_weekday_by_date_str():
    assert di.get_weekday_by_date_str('2020-02-05') == 3
    assert di.get_weekday_by_date_str('2020-02-06') == 4
    assert di.get_weekday_by_date_str('2020-03-05') == 4
    assert di.get_weekday_by_date_str('2021-02-05') == 5
    assert di.get_weekday_by_date_str('2020-02-04') == 2
    assert di.get_weekday_by_date_str('2019-02-05') == 2


def test_get_str():
    assert DateIndex.get_str(base_date) == '2020-02-05'
