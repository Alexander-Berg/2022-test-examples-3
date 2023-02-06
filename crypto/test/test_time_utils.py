import datetime

from crypta.lib.python import time_utils
from crypta.lib.python.yt import yt_time_utils


def test_yt_date_to_datetime():
    assert time_utils.MOSCOW_TZ.localize(datetime.datetime(2021, 6, 2, 3, 0)) == yt_time_utils.yt_date_to_datetime("2021-06-02T00:00:00.000000Z")


def test_unixtime_to_yt_timestamp():
    assert "2020-10-09T09:00:00.000000Z" == yt_time_utils.unixtime_to_yt_timestamp(1602234000)
