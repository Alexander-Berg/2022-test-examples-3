# coding: utf8

from __future__ import unicode_literals

from datetime import date

import pytest

from common.utils.date import RunMask
from travel.rasp.admin.lib.mask_builder.bounds import MaskBounds
from travel.rasp.admin.lib.mask_builder.standard_builders import (
    mask_from_days_of_week, odd_mask, mask_from_day_condition, empty_mask,
    even_mask, daily_mask, through_the_day_mask, gap_days_mask, one_day_mask
)
from travel.rasp.admin.lib.mask_description import run_mask_from_mask_description


bounds = MaskBounds(date(2016, 3, 1), date(2016, 3, 31))

test_masks_parameters = [
    ["""март 2016
        пн   вт   ср   чт   пт   сб   вс
            # 1    2    3    4    5    6
       # 7  # 8    9   10   11   12   13
       #14  #15   16   17   18   19   20
       #21  #22   23   24   25   26   27
       #28  #29   30   31
     """, mask_from_days_of_week, ('12',)],
    ["""март 2016
        пн   вт   ср   чт   пт   сб   вс
            # 1    2  # 3    4  # 5    6
       # 7    8  # 9   10  #11   12  #13
        14  #15   16  #17   18  #19   20
       #21   22  #23   24  #25   26  #27
        28  #29   30  #31
     """, odd_mask, ()],
    ["""март 2016
        пн    вт   ср   чт   пт   сб   вс
              1  # 2    3  # 4    5  # 6
         7  # 8    9  #10   11  #12   13
       #14   15  #16   17  #18   19  #20
        21  #22   23  #24   25  #26   27
       #28   29  #30   31
     """, even_mask, ()],
    ["""март 2016
        пн   вт   ср   чт   пт   сб   вс
            # 1  # 2  # 3  # 4  # 5  # 6
       # 7  # 8  # 9  #10  #11  #12  #13
       #14  #15  #16  #17  #18  #19  #20
       #21  #22  #23  #24  #25  #26  #27
       #28  #29  #30  #31
     """, daily_mask, ()],
    ["""март 2016
        пн   вт   ср   чт   пт   сб   вс
              1    2    3    4    5    6
         7    8    9   10   11   12   13
        14   15   16  #17   18  #19   20
       #21   22  #23   24  #25   26  #27
        28  #29   30  #31
     """, through_the_day_mask, (date(2016, 3, 17),)],
    ["""март 2016
        пн   вт   ср   чт   пт   сб   вс
              1    2    3    4    5    6
         7    8    9   10   11   12   13
        14   15   16  #17   18   19   20
       #21   22   23   24  #25   26   27
        28  #29   30   31
     """, gap_days_mask, (3, date(2016, 3, 17))],
    ["""март 2016
        пн   вт   ср   чт   пт   сб   вс
              1  # 2    3    4    5    6
         7    8    9   10   11   12   13
        14   15   16   17   18   19   20
        21   22   23   24   25   26   27
        28   29   30   31
     """, mask_from_day_condition, (lambda x: x.day == 2,)],
    ["""март 2016
        пн   вт   ср   чт   пт   сб   вс
              1  # 2    3    4    5    6
         7    8    9   10   11   12   13
        14   15   16   17   18   19   20
        21   22   23   24   25   26   27
        28   29   30   31
     """, one_day_mask, (date(2016, 3, 2),)],
    ["""март 2016
        пн   вт   ср   чт   пт   сб   вс
              1    2    3    4    5    6
         7    8    9   10   11   12   13
        14   15   16   17   18   19   20
        21   22   23   24   25   26   27
        28   29   30   31
     """, one_day_mask, (date(2016, 2, 2),)]
]


@pytest.mark.parametrize('mask_description, function, args', test_masks_parameters)
def test_masks(mask_description, function, args):
    etalon_mask = run_mask_from_mask_description(mask_description)
    test_mask = function(bounds, *args)
    assert test_mask == etalon_mask
    assert test_mask.today is None

    today = date(2016, 3, 5)
    test_mask = function(bounds, *(args + (today,)))
    etalon_mask.set_today(today)
    assert test_mask == etalon_mask
    assert test_mask.today == today


def test_empty_mask():
    assert isinstance(empty_mask(), RunMask)
    assert not empty_mask()
    assert not empty_mask(date(2016, 1, 1))
    assert empty_mask(date(2016, 1, 1)).today == date(2016, 1, 1)
