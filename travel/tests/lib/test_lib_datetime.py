# -*- coding: utf-8 -*-
from datetime import datetime
from travel.cpa.lib.lib_datetime import parse_datetime_iso


def test_parse_datetime_iso():
    assert parse_datetime_iso("2022-07-15T10:11:12.123") == datetime(2022, 7, 15, 10, 11, 12, 123000)
