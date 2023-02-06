import pytest

from yamarec_log_parsers import utils


@pytest.mark.parametrize("dt, tz, expected_ts", [
    ("2016-05-16T06:45:45", "+0300", 1463370345),
    ("2016-05-16T07:45:45", "+0400", 1463370345),
    ("2016-05-17T02:45:45", "+2300", 1463370345),
    ("2016-05-16T02:45:45", "-0100", 1463370345)
])
def test_parse_timestamp(dt, tz, expected_ts):
    time_format = "%Y-%m-%dT%H:%M:%S"
    assert utils.make_unixtimestamp(dt, tz, time_format), expected_ts
