import pytest

from mail.ciao.ciao.api.schemas.megamind.parsers.sys_date import parse_sys_date
from mail.ciao.ciao.utils.datetime import UserDate


@pytest.mark.parametrize('value,expected', (
    (
        '{}',
        UserDate(),
    ),
    (
        '{"days":1,"months":2,"years":3,"weeks":4,"weekday":5,"days_relative":1}',
        UserDate(day=1, month=2, year=3, weeks=4, weekday=5, relative=True),
    ),
))
def test_parse_sys_date(value, expected):
    assert parse_sys_date(value) == expected
