# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

import datetime

import pytest

from yabus.util.humanize import humanize_duration


@pytest.mark.parametrize(
    "minutes, expected", ((0, ""), (10, "10 м"), (-10, "10 м"), (60, "1 ч"), (70, "1 ч 10 м"), (6000, "100 ч"),)
)
def test_humanize_duration(minutes, expected):
    assert humanize_duration(datetime.timedelta(minutes=minutes)) == expected
