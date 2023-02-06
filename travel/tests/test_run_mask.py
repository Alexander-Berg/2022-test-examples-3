# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import date, datetime, timedelta
from types import GeneratorType

import mock
import pytest
from django.utils import translation

from travel.rasp.library.python.common23.date.date import daterange, DaysText, group_days
from travel.rasp.library.python.common23.date.run_mask import RunMask


class TestRunMask(object):
    def test_init(self):
        """Проверяем конструктор с различными параметрами."""
        rm = RunMask()
        assert rm.mask == 0
        assert rm.today is None

        rm = RunMask('', today=date(2000, 12, 12))
        assert rm.mask == 0
        assert rm.today == date(2000, 12, 12)

        with mock.patch.object(RunMask, 'MASK_LENGTH') as m_mask_length:
            m_mask_length.__get__ = mock.Mock(return_value=5)
            mask = '11115'
            with pytest.raises(ValueError):
                rm = RunMask(mask=mask, strict=True)

            rm = RunMask(mask=mask)
            assert rm.mask == 0

            mask = '11110'
            rm = RunMask(mask=mask)
            assert rm.mask == int(mask, 2)

        mask = '1'
        with pytest.raises(ValueError):
            rm = RunMask(mask=mask, strict=True)

        rm = RunMask(mask=mask)
        assert rm.mask == 0

        days = daterange(date(2000, 1, 1), date(2000, 10, 10))

        rm = RunMask(days=days)
        for day in days:
            assert rm[day] == 1

        mask = 12
        rm = RunMask(mask=mask)
        assert rm.mask == mask

    def test_get_today(self):
        """Проверяем получение даты."""
        rm = RunMask()
        rm.today = date(2000, 1, 1)
        assert rm.today == date(2000, 1, 1)

    def test_set_today(self):
        """Проверяем установку даты."""
        rm = RunMask()
        d = date(2000, 1, 31)
        dates = list(daterange(date(2000, 1, 1), date(2000, 12, 31), include_end=True))
        rm.today = d
        assert rm.today == d
        assert rm.all_days == dates
        assert rm.date_masks == [(day, rm.date_mask(day)) for day in dates]

        rm.today = None
        assert rm.today is None
        assert rm.date_masks is None

        d = date(2000, 12, 12)
        rm.cached[d] = 1, 2
        rm.today = d
        assert rm.all_days == 1
        assert rm.date_masks == 2

    def test_int(self):
        rm = RunMask(10)
        assert int(rm) == 10

    def test_nonzero(self):
        rm = RunMask(0)
        assert not bool(rm)

        rm = RunMask(1)
        assert bool(rm)

    def test_index(self):
        d = date(2000, 1, 1)
        assert RunMask.index(d) == 0

        d = date(2000, 12, 31)
        assert RunMask.index(d) == RunMask.MASK_LENGTH - 1

    def test_date_mask(self):
        for d in [date(2000, 1, 1), date(2000, 12, 31)]:
            mask = RunMask.date_mask(d)
            assert d in RunMask(mask)
            assert bin(mask)[2:].count('1') == 1

    def test_get_item(self):
        d = date(2000, 1, 1)
        rm = RunMask()
        assert rm[d] is False

        rm = RunMask('1' * RunMask.MASK_LENGTH)
        assert rm[d] is True

    def test_set_item(self):
        d = date(2000, 1, 1)
        rm = RunMask()
        rm[d] = 1
        assert rm.mask == RunMask.date_mask(d)

        rm[d] = 0
        assert rm.mask == 0

    def test_issubset(self):
        rm_1 = RunMask(2)
        rm_2 = RunMask(3)
        assert rm_1.issubset(rm_2)

        rm_1 = RunMask(1)
        assert rm_1.issubset(rm_2)

        rm_1 = RunMask(4)
        assert not rm_1.issubset(rm_2)

    def test_issuperset(self):
        rm_1 = RunMask(3)
        rm_2 = RunMask(2)
        assert rm_1.issuperset(rm_2)

        rm_2 = RunMask(1)
        assert rm_1.issuperset(rm_2)

        rm_2 = RunMask(4)
        assert not rm_1.issuperset(rm_2)

    def test_or(self):
        d = date(2000, 1, 1)
        rm_1 = RunMask(10, today=d)
        rm_2 = RunMask(5)
        rm_3 = rm_1 | rm_2

        assert rm_3.mask == 15
        assert rm_3.today == d

    def test_sub_and_difference(self):
        d = date(2000, 1, 1)
        rm_1 = RunMask(7, today=d)
        rm_2 = RunMask(5)

        rm_3 = rm_1 - rm_2
        assert rm_3.mask == 2
        assert rm_3.today == d

        rm_3 = rm_1.difference(rm_2)
        assert rm_3.mask == 2
        assert rm_3.today == d

    def test_and(self):
        d = date(2000, 1, 1)
        rm_1 = RunMask(10, today=d)
        rm_2 = RunMask(5)

        rm_3 = rm_1 & rm_2
        assert rm_3.mask == 0
        assert rm_3.today == d

        rm_1 = RunMask(9)
        rm_3 = rm_1 & rm_2
        assert rm_3.mask == 1

    def test_eq_and_ne(self):
        d_1 = date(2000, 1, 1)
        d_2 = date(2000, 1, 2)
        rm_1 = RunMask(10, today=d_1)
        rm_2 = RunMask(5, today=d_1)
        assert not rm_1 == rm_2
        assert rm_1 != rm_2

        rm_2 = RunMask(10, today=d_1)
        assert rm_1 == rm_2
        assert not rm_1 != rm_2

        rm_2 = RunMask(10, today=d_2)
        assert not rm_1 == rm_2
        assert rm_1 != rm_2

    def test_xor(self):
        today = date(2000, 1, 1)
        rm_1 = RunMask(5, today)
        rm_2 = RunMask(3)

        rm_3 = rm_1 ^ rm_2
        assert rm_3.mask == 6
        assert rm_3.today == today

    def test_hash(self):
        today = date(2000, 1, 1)
        rm = RunMask(mask=10, today=today)
        assert hash(rm) == hash((hash(today), 10))

    def test_range(self):
        """Проверяем, что range генерирует правильную маску."""
        today = date(2000, 10, 10)
        start_d = date(2000, 1, 1)
        end_d = date(2000, 2, 10)
        delta_d = (end_d - start_d).days

        rm = RunMask.range(start_d, end_d, today=today)
        assert rm.mask == int('1' * delta_d + '0' * (RunMask.MASK_LENGTH - delta_d), 2)
        assert rm.today == today

        rm = RunMask.range(start_d, end_d, today=today, include_end=True)
        assert rm.mask == int('1' * (delta_d + 1) + '0' * (RunMask.MASK_LENGTH - (delta_d + 1)), 2)

    def test_portion(self):
        """Проверяем получение части маски указанной длины, начиная с заданного дня."""
        start_d = date(2000, 1, 1)
        end_d = date(2000, 2, 10)
        length = 10
        rm = RunMask.range(start_d, end_d)

        rm_1 = rm.portion(start_d, length)
        assert rm_1.mask == int('1' * length + '0' * (RunMask.MASK_LENGTH - length), 2)

        start_portion_d = date(2000, 1, 10)
        shift = (start_portion_d - start_d).days
        rm_1 = rm.portion(start_portion_d, length)
        assert rm_1.mask == int('0' * shift + '1' * length + '0' * (RunMask.MASK_LENGTH - (length + shift)), 2)

    def test_dates(self):
        """Проверяем, что возвращаются нужные даты
        и не учитываются даты в прошлом при заданном параметре past."""
        rm = RunMask()

        with pytest.raises(ValueError):
            rm.dates()

        today = date(2000, 1, 15)
        start_d = date(2000, 1, 10)
        end_d = date(2000, 1, 20)
        rm = RunMask.range(start_d, end_d, today=today)

        assert rm.dates() == [start_d + timedelta(i) for i in range(10)]
        assert rm.dates(past=False) == [today + timedelta(i) for i in range(5)]

    def test_iter_date(self):
        """Проверяем, что возвращаются нужные даты
        и не учитываются даты в прошлом при заданном параметре past."""
        rm = RunMask()

        with pytest.raises(ValueError):
            rm.dates()

        today = date(2000, 1, 15)
        start_d = date(2000, 1, 10)
        end_d = date(2000, 1, 20)
        rm = RunMask.range(start_d, end_d, today=today)

        dates = rm.iter_dates()
        assert isinstance(dates, GeneratorType)
        assert list(dates) == [start_d + timedelta(i) for i in range(10)]
        assert list(rm.iter_dates(past=False)) == [today + timedelta(i) for i in range(5)]

    def test_shifted(self):
        """Проверяем, что маска сдвигается корректно."""
        today = date(2000, 1, 15)
        start_d = date(2000, 1, 10)
        end_d = date(2000, 1, 20)
        rm = RunMask.range(start_d, end_d, today=today)

        rm_1 = rm.shifted(None)
        assert rm_1 == rm

        rm_1 = rm.shifted(5)
        assert rm_1.dates() == [start_d + timedelta(5 + i) for i in range(10)]
        assert rm_1.today == rm.today == today

    def test_range_equal(self):
        """Проверяем, что маски совпадают на заданном диапазоне."""
        rm_1 = RunMask.range(date(2000, 1, 1), date(2000, 2, 10))
        rm_2 = RunMask.range(date(2000, 1, 20), date(2000, 2, 7))

        assert RunMask.range_equal(rm_1, rm_2, date(2000, 1, 25), date(2000, 2, 5))
        assert not RunMask.range_equal(rm_1, rm_2, date(2000, 1, 25), date(2000, 2, 8))

    def test_mask_day_index(self):
        dt = datetime(2000, 1, 1)
        assert RunMask.mask_day_index(dt) == (dt.month - 1) * 31 + dt.day - 1
        assert RunMask.mask_day_index(dt.date()) == (dt.month - 1) * 31 + dt.day - 1

    def test_runs_at(self):
        """Проверяем, что runs_at возвращает True только в дни хождения."""
        assert not RunMask.runs_at(None, None)
        assert not RunMask.runs_at('', None)

        rm = RunMask.range(date(2000, 2, 5), date(2000, 2, 10))
        for i in range(5, 10):
            assert RunMask.runs_at(year_days=bin(rm.mask)[2:].zfill(RunMask.MASK_LENGTH), dt=date(2000, 2, i))

        for start, end in [(date(2000, 1, 1), date(2000, 2, 4)),
                           (date(2000, 2, 10), date(2000, 12, 31))]:
            for d in daterange(start, end, include_end=True):
                assert not RunMask.runs_at(year_days=bin(rm.mask)[2:].zfill(RunMask.MASK_LENGTH), dt=d)

    def test_first_run(self):
        """Проверяем, что возвращается первая дата хождения,
        если запрашиваемая дата не превосходит дату последнего отпраления.
        И что возвращает последнюю дату отправления в ином случае."""
        rm = RunMask.range(date(2000, 2, 5), date(2000, 2, 10))
        today = date(2000, 2, 5)

        assert RunMask.first_run('', today) is None
        assert RunMask.first_run('0' * RunMask.MASK_LENGTH, today) is None

        assert RunMask.first_run(bin(rm.mask)[2:].zfill(RunMask.MASK_LENGTH), today) == date(2000, 2, 5)
        assert RunMask.first_run(bin(rm.mask)[2:].zfill(RunMask.MASK_LENGTH), date(2000, 2, 20)) == date(2000, 2, 9)
        assert RunMask.first_run(bin(rm.mask)[2:].zfill(RunMask.MASK_LENGTH), date(2001, 2, 20)) == date(2001, 2, 9)

        rm = RunMask.range(date(2000, 12, 30), date(2000, 12, 31))
        assert RunMask.first_run(bin(rm.mask)[2:].zfill(RunMask.MASK_LENGTH), date(2000, 1, 1)) == date(1999, 12, 30)

    @translation.override('ru')
    def test_format_days_text(self):
        """Проверяем корректность генерируемого текста для параметров сдвига и ограничения количества."""
        today = date(2000, 1, 1)

        rm = RunMask(0, today=today)
        assert rm.format_days_text() is None

        rm = RunMask.range(date(2000, 2, 5), date(2000, 2, 10), today=today)
        days = rm.format_days_text(days_limit=None, shift=0)
        group_d = group_days([(d.day, d.month) for d in daterange(date(2000, 2, 5), date(2000, 2, 10))])
        assert days == DaysText(group_d, False)

        days = rm.format_days_text(days_limit=None, shift=5)
        group_d = group_days([(d.day, d.month) for d in daterange(date(2000, 2, 10), date(2000, 2, 15))])
        assert days == DaysText(group_d, False)

        rm = RunMask.range(date(2000, 1, 1), date(2000, 12, 31), today=today)
        days = rm.format_days_text()
        group_d = group_days([(d.day, d.month) for d in daterange(date(2000, 1, 1), date(2000, 1, 21))])
        assert days == DaysText(group_d, True)

        rm = RunMask.range(date(2000, 1, 1), date(2000, 12, 31), today=date(2000, 2, 11))
        days = rm.format_days_text()
        group_d = group_days([(d.day, d.month) for d in daterange(date(2000, 2, 11), date(2000, 3, 2))])
        assert days == DaysText(group_d, True)

    def test_is_first_run_day(self):
        day1 = datetime(2019, 1, 10).date()
        day2 = datetime(2019, 1, 12).date()
        mask = RunMask(days=[day1, day2])

        assert mask.is_first_run_day(day1) is True
        assert mask.is_first_run_day(day2) is False

        day1 = datetime(2018, 12, 30).date()
        day2 = datetime(2019, 1, 10).date()
        day3 = datetime(2019, 11, 1).date()
        mask = RunMask(days=[day1, day2])

        assert mask.is_first_run_day(day1) is True
        assert mask.is_first_run_day(day2) is False
        assert mask.is_first_run_day(day3) is False
