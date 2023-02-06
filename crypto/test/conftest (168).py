import calendar
import datetime

import pytest
import pytz


@pytest.fixture
def frozen_time():
    dt = datetime.datetime(year=2020, month=1, day=4, hour=0, minute=1, second=1, tzinfo=pytz.timezone("Europe/Moscow"))
    return int(calendar.timegm(dt.astimezone(pytz.timezone("UTC")).timetuple()))
