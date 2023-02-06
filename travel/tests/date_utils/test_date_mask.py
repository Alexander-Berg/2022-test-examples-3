# coding=utf-8
from __future__ import unicode_literals

from datetime import date, datetime

from travel.avia.shared_flights.lib.python.date_utils.date_index import DateIndex
from travel.avia.shared_flights.lib.python.date_utils.date_mask import DateMask, DateMaskMatcher

start_date = datetime(
    year=2020,
    month=2,
    day=5,
    hour=12,
    minute=0,
)

di = DateIndex(start_date)

ALL_DAYS = 1234567


def test_pos():
    mask = DateMask(100)
    mask.add_pos(1)
    mask.add_pos(4)
    mask.add_pos(9)
    values = []
    for idx in range(1, 10):
        values.append('1' if mask.get_pos(idx) else '0')
    assert '100100001' == ''.join(values)

    mask.remove_pos(4)
    values = []
    for idx in range(1, 10):
        values.append('1' if mask.get_pos(idx) else '0')
    assert '100000001' == ''.join(values)


def test_single_day():
    date_mask_matcher = DateMaskMatcher(di, start_date, 400)
    date_mask = date_mask_matcher.new_date_mask()
    date_mask_matcher.add_date_str('2020-02-05', date_mask)
    assert date_mask_matcher.get_dates(date_mask) == [20200205]

    # add another date to the same mask
    date_mask_matcher.add_date_str('2020-02-07', date_mask)
    assert date_mask_matcher.get_dates(date_mask) == [20200205, 20200207]

    # adding the same date again does not change anything
    date_mask_matcher.add_date_str('2020-02-07', date_mask)
    assert date_mask_matcher.get_dates(date_mask) == [20200205, 20200207]

    # try the date 63 days from the start
    date_mask = date_mask_matcher.new_date_mask()
    date_mask_matcher.add_date_str('2020-04-08', date_mask)
    assert date_mask_matcher.get_dates(date_mask) == [20200408]

    # try the date 64 days from the start
    date_mask = date_mask_matcher.new_date_mask()
    date_mask_matcher.add_date_str('2020-04-09', date_mask)
    assert date_mask_matcher.get_dates(date_mask) == [20200409]

    # try the date 65 days from the start
    date_mask = date_mask_matcher.new_date_mask()
    date_mask_matcher.add_date_str('2020-04-10', date_mask)
    assert date_mask_matcher.get_dates(date_mask) == [20200410]

    # try a day per month for a year
    date_mask = date_mask_matcher.new_date_mask()
    date_mask_matcher.add_date_str('2020-02-15', date_mask)
    date_mask_matcher.add_date_str('2020-03-15', date_mask)
    date_mask_matcher.add_date_str('2020-04-15', date_mask)
    date_mask_matcher.add_date_str('2020-05-15', date_mask)
    date_mask_matcher.add_date_str('2020-06-15', date_mask)
    date_mask_matcher.add_date_str('2020-07-15', date_mask)
    date_mask_matcher.add_date_str('2020-08-15', date_mask)
    date_mask_matcher.add_date_str('2020-09-15', date_mask)
    date_mask_matcher.add_date_str('2020-10-15', date_mask)
    date_mask_matcher.add_date_str('2020-11-15', date_mask)
    date_mask_matcher.add_date_str('2020-12-15', date_mask)
    date_mask_matcher.add_date_str('2021-01-15', date_mask)
    date_mask_matcher.add_date_str('2021-02-15', date_mask)
    assert date_mask_matcher.get_dates(date_mask) == [
        20200215,
        20200315,
        20200415,
        20200515,
        20200615,
        20200715,
        20200815,
        20200915,
        20201015,
        20201115,
        20201215,
        20210115,
        20210215,
    ]

    # try the dates beyond range
    date_mask = date_mask_matcher.new_date_mask()
    date_mask_matcher.add_date_str('2020-02-01', date_mask)
    date_mask_matcher.add_date_str('2020-02-04', date_mask)
    date_mask_matcher.add_date_str('2022-02-01', date_mask)
    assert date_mask_matcher.get_dates(date_mask) == []


def test_add_range():
    date_mask_matcher = DateMaskMatcher(di, start_date, 400)
    date_mask = date_mask_matcher.new_date_mask()
    date_mask_matcher.add_range('2020-02-04', '2020-02-16', 237, date_mask)
    assert date_mask_matcher.get_dates(date_mask) == [20200205, 20200209, 20200211, 20200212, 20200216]


def test_add_mask():
    date_mask_matcher = DateMaskMatcher(di, start_date, 400)

    date_mask1 = date_mask_matcher.new_date_mask()
    date_mask_matcher.add_date_str('2020-02-15', date_mask1)
    date_mask_matcher.add_date_str('2020-03-15', date_mask1)

    date_mask2 = date_mask_matcher.new_date_mask()
    date_mask_matcher.add_date_str('2020-03-15', date_mask2)
    date_mask_matcher.add_date_str('2020-04-15', date_mask2)

    date_mask_matcher.add_mask(date_mask1, date_mask2)

    assert date_mask_matcher.get_dates(date_mask1) == [20200215, 20200315, 20200415]


def test_intersect_mask():
    date_mask_matcher = DateMaskMatcher(di, start_date, 400)

    date_mask1 = date_mask_matcher.new_date_mask()
    date_mask_matcher.add_range('2020-02-15', '2020-02-20', ALL_DAYS, date_mask1)
    date_mask_matcher.add_range('2020-03-15', '2020-03-20', ALL_DAYS, date_mask1)
    date_mask_matcher.add_range('2020-03-25', '2020-03-27', ALL_DAYS, date_mask1)

    date_mask2 = date_mask_matcher.new_date_mask()
    date_mask_matcher.add_range('2020-03-19', '2020-04-15', ALL_DAYS, date_mask2)

    date_mask_matcher.intersect_mask(date_mask1, date_mask2)

    assert date_mask_matcher.get_dates(date_mask1) == [20200319, 20200320, 20200325, 20200326, 20200327]


def test_shift_days():
    date_mask_matcher = DateMaskMatcher(di, date(2020, 5, 1), 31)
    date_mask = date_mask_matcher.new_date_mask()
    date_mask_matcher.add_date_str('2020-05-02', date_mask)
    date_mask_matcher.add_date_str('2020-05-03', date_mask)
    date_mask_matcher.add_date_str('2020-05-10', date_mask)
    date_mask_matcher.add_date_str('2020-05-31', date_mask)
    date_mask_matcher.shift_days(date_mask, -2)
    assert date_mask_matcher.get_dates(date_mask) == [20200501, 20200508, 20200529]

    date_mask_matcher.shift_days(date_mask, 4)
    assert date_mask_matcher.get_dates(date_mask) == [20200505, 20200512]


def test_remove_day():
    date_mask_matcher = DateMaskMatcher(di, start_date, 400)
    date_mask = date_mask_matcher.new_date_mask()
    date_mask_matcher.add_date_str('2020-02-05', date_mask)
    date_mask_matcher.add_date_str('2020-02-07', date_mask)
    date_mask_matcher.add_date_str('2020-02-10', date_mask)
    assert date_mask_matcher.get_dates(date_mask) == [20200205, 20200207, 20200210]

    date_mask_matcher.remove_date_str('2020-02-07', date_mask)
    assert date_mask_matcher.get_dates(date_mask) == [20200205, 20200210]

    # try the date 63 days from the start
    date_mask = date_mask_matcher.new_date_mask()
    date_mask_matcher.add_date_str('2020-02-07', date_mask)
    date_mask_matcher.add_date_str('2020-04-08', date_mask)
    date_mask_matcher.remove_date_str('2020-04-08', date_mask)
    assert date_mask_matcher.get_dates(date_mask) == [20200207]

    # try the date 64 days from the start
    date_mask = date_mask_matcher.new_date_mask()
    date_mask_matcher.add_date_str('2020-02-07', date_mask)
    date_mask_matcher.add_date_str('2020-04-09', date_mask)
    date_mask_matcher.remove_date_str('2020-04-09', date_mask)
    assert date_mask_matcher.get_dates(date_mask) == [20200207]

    # try the date 65 days from the start
    date_mask = date_mask_matcher.new_date_mask()
    date_mask_matcher.add_date_str('2020-02-07', date_mask)
    date_mask_matcher.add_date_str('2020-04-10', date_mask)
    date_mask_matcher.remove_date_str('2020-04-10', date_mask)
    assert date_mask_matcher.get_dates(date_mask) == [20200207]
    date_mask_matcher.remove_date_str('2020-02-07', date_mask)
    assert date_mask_matcher.get_dates(date_mask) == []


def test_remove_range():
    date_mask_matcher = DateMaskMatcher(di, start_date, 400)
    date_mask = date_mask_matcher.new_date_mask()
    date_mask_matcher.add_range('2020-02-04', '2020-02-16', 237, date_mask)
    date_mask_matcher.remove_range('2020-02-09', '2020-02-11', 1234567, date_mask)
    assert date_mask_matcher.get_dates(date_mask) == [20200205, 20200212, 20200216]


def test_remove_mask():
    date_mask_matcher = DateMaskMatcher(di, start_date, 400)

    date_mask1 = date_mask_matcher.new_date_mask()
    date_mask_matcher.add_date_str('2020-02-15', date_mask1)
    date_mask_matcher.add_date_str('2020-03-15', date_mask1)
    date_mask_matcher.add_date_str('2020-04-15', date_mask1)

    date_mask2 = date_mask_matcher.new_date_mask()
    date_mask_matcher.add_range('2020-01-01', '2020-03-15', 1234567, date_mask2)

    date_mask_matcher.remove_mask(date_mask1, date_mask2)

    assert date_mask_matcher.get_dates(date_mask1) == [20200415]


def test_diff1():
    date_mask_matcher = DateMaskMatcher(di, start_date, 400)

    date_mask1 = date_mask_matcher.new_date_mask()
    date_mask_matcher.add_date_str('2020-02-15', date_mask1)
    date_mask_matcher.add_date_str('2020-03-15', date_mask1)
    date_mask_matcher.add_date_str('2020-04-15', date_mask1)

    date_mask2 = date_mask_matcher.new_date_mask()
    date_mask_matcher.add_range('2020-01-01', '2020-03-15', 1234567, date_mask2)

    diff = date_mask_matcher.diff1(date_mask1, date_mask2)

    assert date_mask_matcher.get_dates(diff) == [20200415]


def test_is_empty():
    date_mask_matcher = DateMaskMatcher(di, start_date, 400)

    date_mask = date_mask_matcher.new_date_mask()
    assert date_mask.is_empty()

    date_mask_matcher.add_date_str('2020-04-15', date_mask)
    assert not date_mask.is_empty()

    date_mask_matcher.remove_date_str('2020-04-15', date_mask)
    assert date_mask.is_empty()


def test_generate_masks():
    date_mask_matcher = DateMaskMatcher(di, start_date, 400)

    date_mask1 = date_mask_matcher.new_date_mask()

    date_mask_matcher.add_date_str('2020-04-15', date_mask1)
    assert [('2020-04-15', '2020-04-15', 3)] == date_mask_matcher.generate_masks(date_mask1)

    date_mask_matcher.add_date_str('2020-04-16', date_mask1)
    assert [('2020-04-15', '2020-04-21', 34)] == date_mask_matcher.generate_masks(date_mask1)

    date_mask_matcher.add_date_str('2020-04-23', date_mask1)
    date_mask_matcher.add_date_str('2020-04-24', date_mask1)
    assert [
        ('2020-04-15', '2020-04-21', 34),
        ('2020-04-22', '2020-04-28', 45),
    ] == date_mask_matcher.generate_masks(date_mask1)

    date_mask_matcher.add_date_str('2020-04-30', date_mask1)
    date_mask_matcher.add_date_str('2020-05-01', date_mask1)
    assert [
        ('2020-04-15', '2020-04-21', 34),
        ('2020-04-22', '2020-05-05', 45),
    ] == date_mask_matcher.generate_masks(date_mask1)

    # it is easy to miss Sunday by taking (day_num % 7), so test Sunday separately
    date_mask2 = date_mask_matcher.new_date_mask()

    date_mask_matcher.add_date_str('2020-04-19', date_mask2)
    assert [('2020-04-19', '2020-04-19', 7)] == date_mask_matcher.generate_masks(date_mask2)
