# coding: utf8

from __future__ import unicode_literals

from datetime import date

import mock
import pytest

from common.utils.calendar_matcher import YCalendar
from travel.rasp.admin.lib.mask_builder.bounds import MaskBounds
from travel.rasp.admin.lib.mask_builder.ycal_builders import (CountryRequiredError, ycal_workdays_mask, ycal_weekends_and_holidays_mask,
                                            ycal_holidays_mask)
from travel.rasp.admin.lib.mask_description import run_mask_from_mask_description
from tester.factories import create_country


bounds = MaskBounds(date(2016, 3, 1), date(2016, 3, 31))
today = date(2016, 3, 3)

weekends = """
 март 2016
 пн   вт   ср   чт   пт   сб   вс
       1    2    3    4  # 5  # 6
  7  # 8    9   10   11  #12  #13
 14   15   16   17   18  #19  #20
 21   22   23   24   25  #26  #27
 28   29   30   31
"""

workdays = """
 март 2016
 пн   вт   ср   чт   пт   сб   вс
     # 1  # 2  # 3  # 4    5    6
# 7    8  # 9  #10  #11   12   13
#14  #15  #16  #17  #18   19   20
#21  #22  #23  #24  #25   26   27
#28  #29  #30  #31
"""

holidays = """
 март 2016
 пн   вт   ср   чт   пт   сб   вс
       1    2    3    4    5    6
  7  # 8    9   10   11   12   13
 14   15   16   17   18   19   20
 21   22   23   24   25   26   27
 28   29   30   31
"""


@pytest.mark.parametrize('builder', [ycal_workdays_mask, ycal_weekends_and_holidays_mask, ycal_holidays_mask])
@pytest.mark.dbuser
def test_check_country(builder):
    with pytest.raises(CountryRequiredError):
        builder(bounds, None)

    country = create_country(_geo_id=None)
    with pytest.raises(CountryRequiredError):
        builder(bounds, country)


@pytest.mark.dbuser
def test_ycal_workdays_mask():
    country = create_country(_geo_id=222222222)

    with mock.patch.object(YCalendar, 'get_workdays') as m_get_workdays:
        m_get_workdays.return_value = run_mask_from_mask_description(workdays, today).dates(True)

        mask = ycal_workdays_mask(bounds, country)
        mask_with_today = ycal_workdays_mask(bounds, country, today)

        m_get_workdays.assert_has_calls([mock.call(bounds.start_date, bounds.end_date, country)] * 2)

    assert mask == run_mask_from_mask_description(workdays)
    assert mask_with_today == run_mask_from_mask_description(workdays, today)


@pytest.mark.dbuser
def test_ycal_weekends_and_holidays_mask():
    country = create_country(_geo_id=222222222)

    with mock.patch.object(YCalendar, 'get_weekends') as m_get_weekends:
        m_get_weekends.return_value = run_mask_from_mask_description(weekends, today).dates(True)

        mask = ycal_weekends_and_holidays_mask(bounds, country)
        mask_with_today = ycal_weekends_and_holidays_mask(bounds, country, today)

        m_get_weekends.assert_has_calls([mock.call(bounds.start_date, bounds.end_date, country)] * 2)

    assert mask == run_mask_from_mask_description(weekends)
    assert mask_with_today == run_mask_from_mask_description(weekends, today)


@pytest.mark.dbuser
def test_ycal_holidays_mask():
    country = create_country(_geo_id=222222222)

    with mock.patch.object(YCalendar, 'get_holidays') as m_get_holidays:
        m_get_holidays.return_value = run_mask_from_mask_description(holidays, today).dates(True)

        mask = ycal_holidays_mask(bounds, country)
        mask_with_today = ycal_holidays_mask(bounds, country, today)

        m_get_holidays.assert_has_calls([mock.call(bounds.start_date, bounds.end_date, country)] * 2)

    assert mask == run_mask_from_mask_description(holidays)
    assert mask_with_today == run_mask_from_mask_description(holidays, today)
