# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

from datetime import date

import pytest
from rest_framework import serializers

from common.tester.utils.datetime import replace_now
from common.tester.utils.replace_setting import replace_setting
from common.utils.date import MSK_TZ, UTC_TZ
from travel.rasp.wizards.wizard_lib.serialization.date import parse_date


@pytest.fixture
def fixed_environment():
    with replace_now('2000-01-01'), replace_setting('DAYS_TO_PAST', 10):
        yield


def test_parse_date_defaults(fixed_environment):
    assert parse_date(None, MSK_TZ) is None
    assert parse_date('', MSK_TZ) is None
    assert parse_date('1999-01-01', MSK_TZ) is None
    assert parse_date('1999-01-01', MSK_TZ, today_default=True) == date(2000, 1, 1)


def test_parse_date_local_tz(fixed_environment):
    assert parse_date('1999-12-31', MSK_TZ) is None
    assert parse_date('1999-12-31', UTC_TZ) == date(1999, 12, 31)


@pytest.mark.parametrize('value', ('20000101', '2000-01-01T12:00:00'))
def test_parse_date_parsing_validation(fixed_environment, value):
    with pytest.raises(serializers.ValidationError) as excinfo:
        parse_date(value, MSK_TZ)
    assert excinfo.value.detail == ['invalid date value: it should be in YYYY-MM-DD format']


def test_parse_date_past_validation(fixed_environment):
    with pytest.raises(serializers.ValidationError) as excinfo:
        parse_date("1999-12-31", MSK_TZ, ignore_past=False)

    assert excinfo.value.detail == ['invalid date value: it should be present date']

    parse_date("2000-01-01", MSK_TZ, ignore_past=False)
    parse_date("2000-01-02", MSK_TZ, ignore_past=False)


def test_parse_date_range_validation(fixed_environment):
    assert parse_date('2000-12-21', MSK_TZ) == date(2000, 12, 21)

    with pytest.raises(serializers.ValidationError) as excinfo:
        parse_date('2000-12-22', MSK_TZ)
    assert excinfo.value.detail == ['invalid date value: it should be within 355 days in future']
