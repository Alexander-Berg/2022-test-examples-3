# coding: utf8
from __future__ import absolute_import, division, print_function, unicode_literals

import pytz
from datetime import date, time, datetime

import pytest
from hamcrest import assert_that, has_entries

from common.tester.factories import create_station, create_thread, create_direction
from common.models.schedule import RThreadType
from common.models.transport import TransportType
from common.tester.utils.datetime import replace_now

from travel.rasp.api_public.api_public.v3.station.rasp_db_scheduler import RaspDbScheduler


pytestmark = [pytest.mark.dbuser]
create_thread = create_thread.mutate(__={"calculate_noderoute": True})


def _create_suburban_map():
    station_before = create_station(id=100, t_type=TransportType.SUBURBAN_ID)
    station_from = create_station(id=101, t_type=TransportType.SUBURBAN_ID)
    station_to = create_station(id=102, t_type=TransportType.SUBURBAN_ID)

    params = {
        "departure_subdir": "vpered",
        "departure_direction": create_direction(code="vpered"),
        "platform": "1"
    }
    create_thread(
        uid="uid1",
        translated_days_texts='[{}, {"ru": "дни"}]',
        t_type=TransportType.SUBURBAN_ID,
        year_days=[date(2020, 12, 22)],
        tz_start_time=time(1),
        schedule_v1=[
            [None, 0, station_before],
            [60, 70, station_from, params],
            [60, None, station_to]
        ]
    )

    return station_from


def _create_bus_map():
    station_from = create_station(id=101, t_type=TransportType.BUS_ID)
    station_to = create_station(id=102, t_type=TransportType.BUS_ID)

    create_thread(
        uid="uid2",
        t_type=TransportType.BUS_ID,
        type=RThreadType.INTERVAL_ID,
        year_days=[date(2020, 12, 22)],
        begin_time=time(9),
        end_time=time(22),
        density="Раз в 20 мин",
        schedule_v1=[
            [None, 0, station_from],
            [60, None, station_to]
        ]
    )

    return station_from


def _get_schedule_json(dt=None, t_type_id=None, result_pytz=None):
    station = _create_bus_map() if t_type_id == TransportType.BUS_ID else _create_suburban_map()
    t_type = TransportType.objects.get(id=t_type_id)
    query = {
        "transport_types": [t_type],
        "dt": dt,
        "result_pytz": result_pytz,
        "event": "departure",
        "show_systems": ["yandex"],
        "direction": None
    }
    scheduler = RaspDbScheduler(query, station, [t_type.code])
    routes = scheduler.get_schedule()
    return scheduler.schedule2json("pagination_json", routes)


@replace_now("2020-12-22")
def test_rasp_db_schedule_route2json():
    schedule_json = _get_schedule_json(dt=None, t_type_id=TransportType.SUBURBAN_ID)

    assert len(schedule_json["schedule"]) == 1
    assert_that(schedule_json["schedule"][0], has_entries({
        "thread": has_entries({"uid": "uid1"}),
        "is_fuzzy": False,
        "platform": "1",
        "terminal": None,
        "days": "дни",
        "except_days": None,
    }))


@replace_now("2020-12-22")
def test_one_day_schedule_route2json():
    schedule_json = _get_schedule_json(
        dt=datetime(2020, 12, 22, 0, 0, 0),
        t_type_id=TransportType.SUBURBAN_ID,
        result_pytz=pytz.timezone("Etc/GMT-5")
    )

    assert len(schedule_json["schedule"]) == 1
    assert_that(schedule_json["schedule"][0], has_entries({
        "thread": has_entries({"uid": "uid1"}),
        "direction": "vpered",
        "arrival": "2020-12-22T04:00:00+05:00",
        "departure": "2020-12-22T04:10:00+05:00",
    }))


@replace_now("2020-12-22")
def test_all_days_schedule_route2json():
    schedule_json = _get_schedule_json(dt=None, t_type_id=TransportType.SUBURBAN_ID)

    assert len(schedule_json["schedule"]) == 1
    assert_that(schedule_json["schedule"][0], has_entries({
        "thread": has_entries({"uid": "uid1"}),
        "direction": "vpered",
        "arrival": time(2, 0, 0),
        "departure": time(2, 10, 0)
    }))


@replace_now("2020-12-22")
def test_one_day_interval_schedule_route2json():
    schedule_json = _get_schedule_json(dt=datetime(2020, 12, 22, 0, 0, 0), t_type_id=TransportType.BUS_ID)

    assert_that(schedule_json["interval_schedule"][0], has_entries({
        "thread": has_entries({
            "interval": has_entries({
                "density": "Раз в 20 мин",
                "begin_time": "2020-12-22T09:00:00",
                "end_time": "2020-12-22T22:00:00"
            })
        })
    }))


@replace_now("2020-12-22")
def test_all_days_interval_schedule_route2json():
    schedule_json = _get_schedule_json(dt=None, t_type_id=TransportType.BUS_ID)

    assert_that(schedule_json["interval_schedule"][0], has_entries({
        "thread": has_entries({
            "interval": has_entries({
                "density": "Раз в 20 мин",
                "begin_time": time(9, 0, 0),
                "end_time": time(22, 0, 0)
            })
        })
    }))
