# coding=utf-8
from __future__ import unicode_literals

from datetime import datetime

from travel.avia.shared_flights.lib.python.date_utils.date_index import DateIndex
from travel.avia.shared_flights.lib.python.date_utils.date_matcher import DateMatcher

base_date = datetime(
    year=2020,
    month=2,
    day=5,
    hour=12,
    minute=0,
)

di = DateIndex(base_date)
matcher = DateMatcher(di)


def test_get_bits():
    assert DateMatcher.get_bits(0) == 0
    assert DateMatcher.get_bits(1) == 2
    assert DateMatcher.get_bits(2) == 4
    assert DateMatcher.get_bits(3) == 8
    assert DateMatcher.get_bits(4) == 16
    assert DateMatcher.get_bits(5) == 32
    assert DateMatcher.get_bits(6) == 64
    assert DateMatcher.get_bits(7) == 128
    assert DateMatcher.get_bits(237) == 140
    assert DateMatcher.get_bits(273) == 140
    assert DateMatcher.get_bits(1234567) == 254


def test_get_bits_from_string():
    assert DateMatcher.get_bits_from_string('') == 0
    assert DateMatcher.get_bits_from_string('0') == 0
    assert DateMatcher.get_bits_from_string('1') == 2
    assert DateMatcher.get_bits_from_string('6') == 64
    assert DateMatcher.get_bits_from_string('7') == 128
    assert DateMatcher.get_bits_from_string('237') == 140
    assert DateMatcher.get_bits_from_string('273') == 140


def test_get_days_mask():
    assert DateMatcher.get_days_mask(0) == 0
    assert DateMatcher.get_days_mask(1) == 0
    assert DateMatcher.get_days_mask(2) == 1
    assert DateMatcher.get_days_mask(4) == 2
    assert DateMatcher.get_days_mask(8) == 3
    assert DateMatcher.get_days_mask(16) == 4
    assert DateMatcher.get_days_mask(32) == 5
    assert DateMatcher.get_days_mask(64) == 6
    assert DateMatcher.get_days_mask(128) == 7
    assert DateMatcher.get_days_mask(140) == 237
    assert DateMatcher.get_days_mask(254) == 1234567


def test_operates_on():
    bits = 140
    assert not DateMatcher.operates_on(0, bits)
    assert not DateMatcher.operates_on(1, bits)
    assert DateMatcher.operates_on(2, bits)
    assert DateMatcher.operates_on(3, bits)
    assert not DateMatcher.operates_on(4, bits)
    assert not DateMatcher.operates_on(5, bits)
    assert not DateMatcher.operates_on(6, bits)
    assert DateMatcher.operates_on(7, bits)


def test_operates_on_days_mask():
    days_mask = 237
    assert not DateMatcher.operates_on_days_mask(0, days_mask)
    assert not DateMatcher.operates_on_days_mask(1, days_mask)
    assert DateMatcher.operates_on_days_mask(2, days_mask)
    assert DateMatcher.operates_on_days_mask(3, days_mask)
    assert not DateMatcher.operates_on_days_mask(4, days_mask)
    assert not DateMatcher.operates_on_days_mask(5, days_mask)
    assert not DateMatcher.operates_on_days_mask(6, days_mask)
    assert DateMatcher.operates_on_days_mask(7, days_mask)


def test_operates_on_date():
    bits = 140  # Tuesday, Wednesday and Sunday
    assert not matcher.operates_on_date(datetime(year=2020, month=2, day=3), bits)  # Monday
    assert matcher.operates_on_date(datetime(year=2020, month=2, day=4), bits)  # Tuesday
    assert matcher.operates_on_date(datetime(year=2020, month=2, day=5), bits)  # Wednesday
    assert not matcher.operates_on_date(datetime(year=2020, month=2, day=6), bits)  # Thursday
    assert not matcher.operates_on_date(datetime(year=2020, month=2, day=7), bits)  # Friday
    assert not matcher.operates_on_date(datetime(year=2020, month=2, day=8), bits)  # Saturday
    assert matcher.operates_on_date(datetime(year=2020, month=2, day=9), bits)  # Sunday


def test_operates_on_datestr():
    bits = 140  # Tuesday, Wednesday and Sunday
    assert not matcher.operates_on_datestr('2020-02-03', bits)  # Monday
    assert matcher.operates_on_datestr('2020-02-04', bits)  # Tuesday
    assert matcher.operates_on_datestr('2020-02-05', bits)  # Wednesday
    assert not matcher.operates_on_datestr('2020-02-06', bits)  # Thursday
    assert not matcher.operates_on_datestr('2020-02-07', bits)  # Friday
    assert not matcher.operates_on_datestr('2020-02-08', bits)  # Saturday
    assert matcher.operates_on_datestr('2020.02.09', bits)  # Sunday


def test_operates_on_index():
    bits = 140  # Tuesday, Wednesday and Sunday
    assert matcher.operates_on_index(0, bits)      # Wednesday
    assert not matcher.operates_on_index(1, bits)  # Thursday


def test_intersect():
    # Date ranges don't intersect
    assert not matcher.intersect(
        from_date_str1='2020-02-05',
        until_date_str1='2020-02-08',
        days_mask1=1234567,
        from_date_str2='2020-02-09',
        until_date_str2='2020-02-12',
        days_mask2=1234567,
    )

    # Date ranges do intersect, but operating days don't
    assert not matcher.intersect(
        from_date_str1='2020-02-05',
        until_date_str1='2020-02-12',
        days_mask1=123,
        from_date_str2='2020-02-05',
        until_date_str2='2020-02-12',
        days_mask2=4567,
    )

    # Date ranges intersect on a single day
    assert matcher.intersect(
        from_date_str1='2020-02-05',
        until_date_str1='2020-02-12',
        days_mask1=1234,
        from_date_str2='2020-02-05',
        until_date_str2='2020-02-12',
        days_mask2=4567,
    ) == ('2020-02-06', '2020-02-06', 4)

    # Date ranges intersect on 2 days, single week
    assert matcher.intersect(
        from_date_str1='2020-02-05',
        until_date_str1='2020-02-12',
        days_mask1=12345,
        from_date_str2='2020-02-05',
        until_date_str2='2020-02-12',
        days_mask2=4567,
    ) == ('2020-02-06', '2020-02-07', 45)

    # Date ranges intersect on 2 days, multiple weeks
    assert matcher.intersect(
        from_date_str1='2020-02-05',
        until_date_str1='2020-02-19',
        days_mask1=12345,
        from_date_str2='2020-02-05',
        until_date_str2='2020-02-19',
        days_mask2=4567,
    ) == ('2020-02-06', '2020-02-14', 45)

    # Just some random case from AF filing
    assert matcher.intersect(
        from_date_str1='2020-02-03',
        until_date_str1='2020-02-16',
        days_mask1=167,
        from_date_str2='2020-01-27',
        until_date_str2='2020-02-17',
        days_mask2=1,
    ) == ('2020-02-03', '2020-02-10', 1)

    # Another random AF case
    assert matcher.intersect(
        from_date_str1='2020-02-03',
        until_date_str1='2020-02-17',
        days_mask1=167,
        from_date_str2='2020-02-08',
        until_date_str2='2020-02-23',
        days_mask2=67,
    ) == ('2020-02-08', '2020-02-16', 67)
