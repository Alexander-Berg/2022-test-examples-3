import datetime
import random

from yt.wrapper import ypath

from crypta.buchhalter.services.main.lib.common.helpers import month_grouper


def test_get_full_months():
    year = 2020

    def get_path(month, day):
        name = datetime.datetime(year=year, month=month, day=day).strftime("%Y-%m-%d")
        return ypath.ypath_join("//xxx/zzz", name)

    not_full_january = [get_path(month=1, day=day) for day in range(1, 14)]
    full_february = [get_path(month=2, day=day) for day in range(1, 29 + 1)]
    not_full_march = [get_path(month=3, day=day) for day in range(1, 14)]
    full_may = [get_path(month=5, day=day) for day in range(1, 31 + 1)]

    paths = not_full_january + full_february + not_full_march + full_may
    random.shuffle(paths)

    expected = [
        month_grouper.MonthPaths(year=year, month=5, paths=full_may),
        month_grouper.MonthPaths(year=year, month=2, paths=full_february),
    ]

    assert expected == month_grouper.get_full_months(paths)
