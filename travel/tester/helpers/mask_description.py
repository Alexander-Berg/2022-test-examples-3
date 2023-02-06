# -*- coding: utf-8 -*-

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

from travel.avia.library.python.common.utils.date import RunMask


MONTH_NAMES = {
    u'январь': 1,
    u'февраль': 2,
    u'март': 3,
    u'апрель': 4,
    u'май': 5,
    u'июнь': 6,
    u'июль': 7,
    u'август': 8,
    u'сентябрь': 9,
    u'октябрь': 10,
    u'ноябрь': 11,
    u'декабрь': 12,
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
        values = filter(None, map(unicode.strip, line.split(u'  ')))
        for v in values:
            if v.startswith('#'):
                days.append(int(v.replace('#', '').strip()))

    for day in days:
        out_days.append(date(year, month, day))

    return out_days


def make_dates_from_mask_description(mask_description):
    month_mask_descriptions = mask_description.split(u'---')
    return sum(map(make_dates_from_month_mask_description, month_mask_descriptions), [])


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
    first_str = u" " + month_str + u" " + unicode(year)

    weekdays = u" пн   вт   ср   чт   пт   сб   вс"

    first_days_str = u""
    first_month_day = month_all_dates[0]
    if first_month_day.isoweekday() != 1:
        skiped_days_count = first_month_day.isoweekday() - 1
        first_days_str += u"   " + u"     " * (skiped_days_count - 1)

    days_strs = []

    days_str = first_days_str
    for day in month_all_dates:
        if day in dates:
            str_day = u"#%2d" % day.day
        else:
            str_day = u" %2d" % day.day

        if day.isoweekday() > 1:
            str_day = u"  " + str_day

        days_str += str_day

        if day.isoweekday() == 7:
            days_strs.append(days_str)
            days_str = u""

    if days_str:
        days_strs.append(days_str)

    parts = [first_str, weekdays] + days_strs

    result = u"\n".join(parts)
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

    return u"\n ---\n".join(month_descriptions)


def get_description_from_mask_in_range(mask, mask_range):
    start, end = mask_range

    range_mask = RunMask.range(start, end + timedelta(1), mask.today)

    return get_description_from_mask(mask & range_mask)


def run_mask_from_mask_description(description):
    return RunMask(days=make_dates_from_mask_description(description))
