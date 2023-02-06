# coding: utf8

from __future__ import unicode_literals

from datetime import date

import mock
import pytest

from common.utils.calendar_matcher import YCalendar

from travel.rasp.admin.lib.mask_builder.afmask_builders import AfMaskBuilder
from travel.rasp.admin.lib.mask_builder.bounds import MaskBounds
from travel.rasp.admin.lib.mask_builder.standard_builders import empty_mask, daily_mask, even_mask, odd_mask, mask_from_days_of_week
from travel.rasp.admin.lib.mask_builder.ycal_builders import (CountryRequiredError, ycal_workdays_mask, ycal_weekends_and_holidays_mask,
                                            ycal_holidays_mask)
from travel.rasp.admin.lib.mask_description import run_mask_from_mask_description

from tester.factories import create_country


bounds = MaskBounds(date(2016, 3, 1), date(2016, 3, 31))
today_param = date(2016, 3, 3)

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


@pytest.yield_fixture()
def builder():
    yield AfMaskBuilder()


@pytest.mark.dbuser
def test_is_af_mask(builder):
    assert builder.is_af_mask_text('H123')
    assert not builder.is_af_mask_text('RT')
    assert not builder.is_af_mask_text('')


@pytest.mark.dbuser
@pytest.mark.parametrize('today', [None, today_param])
def test_build_empty_mask(builder, today):
    assert builder.build(bounds, 'C', today=today) == empty_mask(today)
    assert builder.build(bounds, 'Срусская'[0], today=today) == empty_mask(today)


@pytest.mark.dbuser
@pytest.mark.parametrize('text,func', [
    ['D', daily_mask],
    ['E', even_mask],
    ['U', odd_mask]
])
@pytest.mark.parametrize('today', [None, today_param])
def test_build_like_standard_mask(builder, text, func, today):
    assert builder.build(bounds, text, today=today) == func(bounds, today)


@pytest.mark.dbuser
@pytest.mark.parametrize('text,func,mock_name,mask_description', [
    ['H', ycal_weekends_and_holidays_mask, 'get_weekends', weekends],
    ['F', ycal_holidays_mask, 'get_holidays', holidays],
    ['W', ycal_workdays_mask, 'get_workdays', workdays],
])
@pytest.mark.parametrize('today', [None, today_param])
def test_build_like_ycal_mask(builder, text, func, mock_name, mask_description, today):
    country = create_country(_geo_id=222222222)
    with mock.patch.object(YCalendar, mock_name) as ycal_mock:
        ycal_mock.return_value = run_mask_from_mask_description(mask_description, today_param).dates(True)

        assert (builder.build(bounds, text, country=country, today=today) ==
                func(bounds, country, today))

        ycal_mock.assert_has_calls([mock.call(bounds.start_date, bounds.end_date, country)] * 2)


@pytest.mark.dbuser
@pytest.mark.parametrize('today', [None, today_param])
def test_build_from_days_of_week_mask(builder, today):
    assert builder.build(bounds, '123', today=today) == mask_from_days_of_week(bounds, '123', today)


@pytest.mark.dbuser
@pytest.mark.parametrize('today', [None, today_param])
def test_build_not_mask(builder, today):
    assert builder.build(bounds, '-123', today=today) == mask_from_days_of_week(bounds, '4567', today)


@pytest.mark.dbuser
@pytest.mark.parametrize('today', [None, today_param])
def test_build_letters_and_digit_mask(builder, today):
    assert (builder.build(bounds, 'E-123', today=today) ==
            even_mask(bounds, today) - mask_from_days_of_week(bounds, '123', today))
    assert (builder.build(bounds, 'E1', today=today) ==
            even_mask(bounds, today) | mask_from_days_of_week(bounds, '1', today))

