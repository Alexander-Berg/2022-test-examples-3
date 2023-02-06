# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import date, timedelta

import pytest

from travel.rasp.library.python.common23.date.date import (
    parse_date, group_days, timedelta_to_str_hours_and_minutes, get_plural_form
)


class TestDate(object):
    def test_parse_date(self):
        """Проверяем, что дата с разделителем "-" парсится."""
        date_for_parse = '2000-01-01'
        assert date(2000, 1, 1) == parse_date(date_for_parse)
        date_for_parse = '2000'
        with pytest.raises(TypeError):
            assert parse_date(date_for_parse)
        assert parse_date(None) is None

    def test_get_plural_form(self):
        def get_range(exs, starts, stop, step=10):
            numbers = []
            for i in starts:
                for n in range(i, stop, step):
                    if n not in exs:
                        numbers.append(n)
            return numbers

        exs = (11, 12, 13, 14, 111, 112, 113, 114)
        for n in [x for x in range(1, 202, 10) if x not in exs]:
            assert get_plural_form(n, [u'день', u'дня', u'дней']) == u'%s %s' % (n, u'день')
        for n in get_range(exs, [2, 3, 4], 205):
            assert get_plural_form(n, [u'день', u'дня', u'дней']) == u'%s %s' % (n, u'дня')
        for n in get_range([], [0, 5, 6, 7, 8, 9], 210):
            assert get_plural_form(n, [u'день', u'дня', u'дней']) == u'%s %s' % (n, u'дней')
        for n in exs:
            assert get_plural_form(n, [u'день', u'дня', u'дней']) == u'%s %s' % (n, u'дней')


def test_timedelta_to_str_hours_and_minutes():
    assert u'00:00' == timedelta_to_str_hours_and_minutes(timedelta(seconds=0))
    assert u'02:01' == timedelta_to_str_hours_and_minutes(timedelta(hours=2, minutes=1))
    assert u'23:59' == timedelta_to_str_hours_and_minutes(timedelta(hours=23, minutes=59))
    assert u'230:42' == timedelta_to_str_hours_and_minutes(timedelta(hours=230, minutes=42))


def test_group_days():
    assert group_days([(1, 1), (2, 1), (1, 2)], 'ru', days_month_separator=u' ') == u'1, 2 января, 1 февраля'
    assert group_days('1.1,2.1,1.2', 'ru', days_month_separator=u' ') == u'1, 2 января, 1 февраля'
