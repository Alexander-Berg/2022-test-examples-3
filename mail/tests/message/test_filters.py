from datetime import datetime

import pytest

from fan.message.env import strftime, strptime


@pytest.mark.parametrize(
    "value,fmt", [(datetime.now(), "%Y is year"), (datetime.now(), "%S seconds")]
)
def test_strftime_filter(value, fmt):
    assert strftime(value, fmt) == value.strftime(fmt)


@pytest.mark.parametrize(
    "date_string,fmt,result",
    [
        ("2019-07-16", "%Y-%m-%d", datetime(year=2019, month=7, day=16)),
        (
            "2019-08-19 12:45:09",
            "%Y-%m-%d %H:%M:%S",
            datetime(year=2019, month=8, day=19, hour=12, minute=45, second=9),
        ),
    ],
)
def test_strptime_filter(date_string, fmt, result):
    assert strptime(date_string, fmt) == result
