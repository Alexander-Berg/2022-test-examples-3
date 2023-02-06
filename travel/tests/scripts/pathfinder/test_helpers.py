# coding: utf8
from __future__ import absolute_import, division, print_function, unicode_literals

from datetime import datetime

import pytest

from common.utils.date import RunMask
from travel.rasp.rasp_scripts.scripts.pathfinder.helpers import get_to_pathfinder_year_days_converter


@pytest.mark.parametrize('today, expected_size', (
    (datetime(1999, 6, 1), 366),
    (datetime(2000, 3, 1), 366),
    (datetime(2000, 4, 1), 365),
    (datetime(2000, 3, 15), 366),
    (datetime(2000, 4, 15), 365),
))
def test_pathfinder_year_days_converter_leap_mask(today, expected_size):
    assert len(get_to_pathfinder_year_days_converter(today)(RunMask.ALL_YEAR_DAYS)) == expected_size
