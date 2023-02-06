# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from builtins import map
"""
    Генерируем текстовые календари типа
    # Значит в этот день ходим

    март 2011
    пн   вт   ср   чт   пт   сб   вс
          1    2    3    4  # 5  # 6
     7    8    9   10   11  #12  #13
    14   15   16   17   18  #19  #20
    21   22   23   24   25  #26  #27
    28   29   30   31
    ---
    апрель 2011
    пн   вт   ср   чт   пт   сб   вс
                         1    2    3
     4    5    6    7    8    9   10
    11   12   13   14   15   16   17
    18   19   20   21   22   23   24
    25   26   27   28   29   30
"""

from collections import OrderedDict
from datetime import date, timedelta

import six

from travel.rasp.library.python.common23.date.run_mask import RunMask


MONTH_NAMES = {
    'январь': 1,
    'февраль': 2,
    'март': 3,
    'апрель': 4,
    'май': 5,
    'июнь': 6,
    'июль': 7,
    'август': 8,
    'сентябрь': 9,
    'октябрь': 10,
    'ноябрь': 11,
    'декабрь': 12,
}


def make_dates_from_month_mask_description(month_calendar_description):
    out_days = []

    month_calendar_description = month_calendar_description.strip()
    lines = month_calendar_description.splitlines()
    month_year = lines[0]
    day_table = lines[2:]

    month, year = month_year.split()
    year = int(year)

    month = MONTH_NAMES[month]

    days = []

    for line in day_table:
        values = [x for x in map(six.text_type.strip, line.split('  ')) if x]
        for v in values:
            if v.startswith('#'):
                days.append(int(v.replace('#', '').strip()))

    for day in days:
        out_days.append(date(year, month, day))

    return out_days


def make_dates_from_mask_description(mask_description):
    month_mask_descriptions = mask_description.split('---')
    return sum(list(map(make_dates_from_month_mask_description, month_mask_descriptions)), [])


def get_month_str(month):
    for month_str, month_index in MONTH_NAMES.items():
        if month_index == month:
            return month_str


def get_month_description(year, month, dates):

    month_all_dates = []
    month_day = date(year, month, 1)
    while month_day.month == month:
        month_all_dates.append(month_day)
        month_day += timedelta(1)

    month_str = get_month_str(month)
    first_str = ' ' + month_str + ' ' + six.text_type(year)

    weekdays = ' пн   вт   ср   чт   пт   сб   вс'

    first_days_str = ''
    first_month_day = month_all_dates[0]
    if first_month_day.isoweekday() != 1:
        skiped_days_count = first_month_day.isoweekday() - 1
        first_days_str += '   ' + '     ' * (skiped_days_count - 1)

    days_strs = []

    days_str = first_days_str
    for day in month_all_dates:
        if day in dates:
            str_day = '#%2d' % day.day
        else:
            str_day = ' %2d' % day.day

        if day.isoweekday() > 1:
            str_day = '  ' + str_day

        days_str += str_day

        if day.isoweekday() == 7:
            days_strs.append(days_str)
            days_str = ''

    if days_str:
        days_strs.append(days_str)

    parts = [first_str, weekdays] + days_strs

    result = '\n'.join(parts)
    return result


def get_description_from_mask(mask):
    dates = mask.dates()

    by_month = OrderedDict()

    dates.sort()

    for day in dates:
        month_key = day.year, day.month
        by_month.setdefault(month_key, list()).append(day)

    month_descriptions = []

    for year, month in by_month:
        month_descriptions.append(get_month_description(year, month, by_month[(year, month)]))

    return '\n ---\n'.join(month_descriptions)


def get_description_from_mask_in_range(mask, mask_range):
    start, end = mask_range

    range_mask = RunMask.range(start, end + timedelta(1), mask.today)

    return get_description_from_mask(mask & range_mask)


def run_mask_from_mask_description(description):
    return RunMask(days=make_dates_from_mask_description(description))
