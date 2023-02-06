from datetime import date

from travel.avia.library.python.common.lib.holiday_search_date_provider import generate_holiday_search_dates
from travel.avia.library.python.tester.testcase import TestCase


class FakeHoliday(object):
    def __init__(self, first_day, last_day):
        self.first_day = first_day
        self.last_day = last_day


class TestHolidaySearchDateProvider(TestCase):
    def test_without_overlapping(self):
        holiday = FakeHoliday(
            first_day=date(2017, 11, 4),
            last_day=date(2017, 11, 8)
        )

        generated_dates = set(generate_holiday_search_dates(
            holiday, one_way_trip=False, window_width=1
        ))

        right_dates = set([
            (date(2017, 11, 3), date(2017, 11, 7)),
            (date(2017, 11, 3), date(2017, 11, 8)),
            (date(2017, 11, 3), date(2017, 11, 9)),
            (date(2017, 11, 4), date(2017, 11, 7)),
            (date(2017, 11, 4), date(2017, 11, 8)),
            (date(2017, 11, 4), date(2017, 11, 9)),
            (date(2017, 11, 5), date(2017, 11, 7)),
            (date(2017, 11, 5), date(2017, 11, 8)),
            (date(2017, 11, 5), date(2017, 11, 9)),
        ])
        assert generated_dates == right_dates

    def test_one_way(self):
        holiday = FakeHoliday(
            first_day=date(2017, 11, 4),
            last_day=date(2017, 11, 8)
        )

        generated_dates = set(generate_holiday_search_dates(
            holiday, one_way_trip=True, window_width=1
        ))

        right_dates = set([
            (date(2017, 11, 3), None),
            (date(2017, 11, 4), None),
            (date(2017, 11, 5), None),
        ])
        assert generated_dates == right_dates

    def test_with_overlapping(self):
        holiday = FakeHoliday(
            first_day=date(2017, 11, 4),
            last_day=date(2017, 11, 5)
        )

        generated_dates = set(generate_holiday_search_dates(
            holiday, one_way_trip=False, window_width=1
        ))

        right_dates = set([
            (date(2017, 11, 3), date(2017, 11, 4)),
            (date(2017, 11, 3), date(2017, 11, 5)),
            (date(2017, 11, 3), date(2017, 11, 6)),
            (date(2017, 11, 4), date(2017, 11, 4)),
            (date(2017, 11, 4), date(2017, 11, 5)),
            (date(2017, 11, 4), date(2017, 11, 6)),
            (date(2017, 11, 5), date(2017, 11, 5)),
            (date(2017, 11, 5), date(2017, 11, 6)),
        ])
        assert generated_dates == right_dates
