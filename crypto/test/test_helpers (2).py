import datetime

from crypta.lib.python import time_utils
from crypta.spine.pushers.yt_output_table_latencies.lib import helpers


DATE_02_STR = "2021-06-02"
DATE_03_STR = "2021-06-03"
DATE_04_STR = "2021-06-04"
DATETIME_NOW_02 = time_utils.MOSCOW_TZ.localize(datetime.datetime(2021, 6, 2))
DATETIME_NOW_03 = time_utils.MOSCOW_TZ.localize(datetime.datetime(2021, 6, 3))
DATETIME_NOW_04 = time_utils.MOSCOW_TZ.localize(datetime.datetime(2021, 6, 4))
DATETIME_NOW_11 = time_utils.MOSCOW_TZ.localize(datetime.datetime(2021, 6, 11))
DATE_02 = datetime.date(2021, 6, 2)
DATE_03 = datetime.date(2021, 6, 3)
DATE_04 = datetime.date(2021, 6, 4)


def test_get_latest_expected_date():
    assert helpers.get_latest_expected_date([], DATETIME_NOW_03) is None
    assert helpers.get_latest_expected_date(["non-date"], DATETIME_NOW_03) is None

    assert DATE_02 == helpers.get_latest_expected_date([DATE_02_STR, DATE_03_STR], DATETIME_NOW_03)
    assert DATE_02 == helpers.get_latest_expected_date([DATE_02_STR, DATE_03_STR, DATE_04_STR], DATETIME_NOW_03)
    assert DATE_03 == helpers.get_latest_expected_date([DATE_02_STR, DATE_03_STR, DATE_04_STR], DATETIME_NOW_04)
    assert helpers.get_latest_expected_date(["non-date", DATE_03_STR, "non-date-2"], DATETIME_NOW_03) is None

    assert DATE_02 == helpers.get_latest_expected_date([DATE_02_STR], DATETIME_NOW_03)
    assert DATE_03 == helpers.get_latest_expected_date([DATE_02_STR], DATETIME_NOW_11)
    assert DATE_04 == helpers.get_latest_expected_date([DATE_03_STR], DATETIME_NOW_11)

    assert helpers.get_latest_expected_date([DATE_04_STR], DATETIME_NOW_03) is None
