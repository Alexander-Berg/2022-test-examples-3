# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime

from travel.rasp.library.python.common23.date.date import naive_to_timestamp


def test_naive_to_timestamp():
    assert naive_to_timestamp(datetime(2018, 11, 24, 12, 45, 1)) == 1543063501
