# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import date, timedelta

import pytest

from travel.rasp.library.python.common23.date.date import daterange


@pytest.mark.parametrize('start, end, shift, result', [
    (date(2016, 1, 1), date(2016, 1, 1), 1, []),
    (date(2016, 1, 1), date(2016, 1, 1), 2, []),
    (date(2016, 1, 1), date(2016, 1, 2), 1, [date(2016, 1, 1)]),
    (date(2016, 1, 1), date(2016, 1, 3), 1, [date(2016, 1, 1), date(2016, 1, 2)]),
    (date(2016, 1, 1), date(2016, 1, 3), 0, []),
    (date(2016, 1, 1), date(2016, 1, 5), 2, [date(2016, 1, 1), date(2016, 1, 3)]),
    (date(2016, 1, 1), date(2016, 1, 4), 2, [date(2016, 1, 1), date(2016, 1, 3)]),
    (date(2016, 1, 1), date(2016, 1, 3), 2, [date(2016, 1, 1)]),

    (date(2016, 1, 1), date(2016, 1, 1), -1, []),
    (date(2016, 1, 1), date(2016, 1, 2), -1, []),
    (date(2016, 1, 3), date(2016, 1, 1), -1, [date(2016, 1, 3), date(2016, 1, 2)]),
    (date(2016, 1, 5), date(2016, 1, 1), -2, [date(2016, 1, 5), date(2016, 1, 3)]),
])
def test_exclude_end(start, end, shift, result):
    assert list(daterange(start, end, shift)) == result
    assert list(daterange(start, end, timedelta(shift))) == result


@pytest.mark.parametrize('start, end, shift, result', [
    (date(2016, 1, 1), date(2016, 1, 1), 1, [date(2016, 1, 1)]),
    (date(2016, 1, 1), date(2016, 1, 1), 2, []),
    (date(2016, 1, 1), date(2016, 1, 2), 1, [date(2016, 1, 1), date(2016, 1, 2)]),
    (date(2016, 1, 1), date(2016, 1, 3), 1, [date(2016, 1, 1), date(2016, 1, 2), date(2016, 1, 3)]),
    (date(2016, 1, 1), date(2016, 1, 3), 0, []),
    (date(2016, 1, 1), date(2016, 1, 5), 2, [date(2016, 1, 1), date(2016, 1, 3), date(2016, 1, 5)]),
    (date(2016, 1, 1), date(2016, 1, 4), 2, [date(2016, 1, 1), date(2016, 1, 3)]),
    (date(2016, 1, 1), date(2016, 1, 3), 2, [date(2016, 1, 1), date(2016, 1, 3)]),

    (date(2016, 1, 1), date(2016, 1, 1), -1, [date(2016, 1, 1)]),
    (date(2016, 1, 1), date(2016, 1, 2), -1, []),
    (date(2016, 1, 3), date(2016, 1, 1), -1, [date(2016, 1, 3), date(2016, 1, 2), date(2016, 1, 1)]),
    (date(2016, 1, 5), date(2016, 1, 1), -2, [date(2016, 1, 5), date(2016, 1, 3), date(2016, 1, 1)]),
])
def test_include_end(start, end, shift, result):
    assert list(daterange(start, end, shift, include_end=True)) == result
    assert list(daterange(start, end, timedelta(shift), include_end=True)) == result
