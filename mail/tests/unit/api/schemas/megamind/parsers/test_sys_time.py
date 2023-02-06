import pytest

from mail.ciao.ciao.api.schemas.megamind.parsers.sys_time import parse_sys_time
from mail.ciao.ciao.utils.datetime import Period, UserTime


@pytest.mark.parametrize('value,expected', (
    (
        '{}',
        UserTime(),
    ),
    (
        '{"hours":1,"minutes":2,"seconds":3,"period":"am"}',
        UserTime(hour=1, minute=2, second=3, period=Period.AM),
    ),
))
def test_parse_sys_time(value, expected):
    assert parse_sys_time(value) == expected
