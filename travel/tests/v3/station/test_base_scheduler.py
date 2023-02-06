# coding: utf8
from __future__ import absolute_import, division, print_function, unicode_literals

import pytz
from datetime import date, datetime

import pytest

from common.tester.factories import create_station

from travel.rasp.api_public.api_public.v3.station.base_scheduler import BaseScheduler
from travel.rasp.api_public.api_public.v3.core.api_errors import ApiError


pytestmark = [pytest.mark.dbuser]


def _make_scheduler_and_check_base(station, event):
    moscow_pytz = pytz.timezone("Europe/Moscow")
    query = {
        "dt": datetime(2020, 12, 22, 0, 0, 0),
        "result_pytz": moscow_pytz,
        "event": event,
        "show_systems": ["yandex"]
    }
    scheduler = BaseScheduler(query, station)
    scheduler.set_event()

    assert scheduler.station.id == station.id
    assert scheduler.date == date(2020, 12, 22)
    assert scheduler.result_pytz == moscow_pytz
    assert scheduler.base_json["date"] == date(2020, 12, 22)
    assert scheduler.base_json["station"]["code"] == "s100"

    return scheduler


def test_base_scheduler():
    station = create_station(id=100)

    scheduler =_make_scheduler_and_check_base(
        station, event=""
    )
    assert scheduler.event == "departure"
    assert scheduler.base_json["event"] == "departure"

    scheduler =_make_scheduler_and_check_base(station, event="departure")
    assert scheduler.event == "departure"
    assert scheduler.base_json["event"] == "departure"

    scheduler =_make_scheduler_and_check_base(station, event="arrival")
    assert scheduler.event == "arrival"
    assert scheduler.base_json["event"] == "arrival"

    with pytest.raises(ApiError) as ex:
        _make_scheduler_and_check_base(station, event="pribitie")
    assert ex.value.message == "event должен принимать значения arrival, departure или быть пустым."
