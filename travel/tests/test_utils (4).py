# -*- coding: utf-8 -*-


import pytest
from datetime import date

from travel.rasp.suburban_tasks.suburban_tasks.utils import format_dates

d = date


@pytest.mark.parametrize('dates,output', [
    ([d(2015, 1, 1), d(2015, 1, 2), d(2015, 1, 3)], u'2015-01-01, 2015-01-02, 2015-01-03'),
    ([d(2015, 1, 1), d(2015, 1, 2), d(2015, 1, 3),
      d(2015, 1, 4), d(2015, 1, 5), d(2015, 1, 6)], u'2015-01-01..2015-01-06'),
    ([d(2015, 1, 1), d(2015, 1, 2),
      d(2015, 1, 4), d(2015, 1, 5), d(2015, 1, 6),
      d(2015, 1, 9)], u'2015-01-01..2015-01-02, 2015-01-04..2015-01-06, 2015-01-09'),
    ([d(2015, 1, 1), d(2015, 1, 3), d(2015, 1, 5), d(2015, 1, 7), d(2015, 1, 9),
      d(2015, 1, 11)], u'2015-01-01 ..... 2015-01-11: всего 6'),
])
def test_format_dates(dates, output):
    assert format_dates(dates) == output
