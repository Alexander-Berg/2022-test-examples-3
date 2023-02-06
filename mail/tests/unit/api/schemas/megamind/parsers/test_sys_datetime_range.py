import pytest
from dateutil.relativedelta import relativedelta

from mail.ciao.ciao.api.schemas.megamind.parsers.sys_datetime_range import parse_sys_datetime_range


@pytest.mark.parametrize('value,expected', (
    (
        '{"start":{"minutes":0,"minutes_relative":true},"end":{"minutes":30,"minutes_relative":true}}',
        relativedelta(minutes=30),
    ),
    (
        '{"end":{"weeks":2,"weeks_relative":true},"start":{"weeks":0,"weeks_relative":true}}',
        relativedelta(weeks=2),
    ),
))
def test_parse_sys_datetime_range(value, expected):
    assert parse_sys_datetime_range(value) == expected
