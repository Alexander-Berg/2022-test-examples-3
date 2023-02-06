# coding: utf8
from __future__ import absolute_import, division, print_function, unicode_literals

from datetime import date, time, datetime, timedelta

import pytest
from hamcrest import assert_that, has_entries, contains, contains_inanyorder

from common.tester.factories import create_station, create_thread, create_direction, create_train_schedule_plan
from common.models.transport import TransportType
from common.models.schedule import RThreadType
from common.tester.utils.datetime import replace_now

from travel.rasp.api_public.api_public.v3.station.rasp_db_scheduler import RaspDbScheduler


pytestmark = [pytest.mark.dbuser]
create_thread = create_thread.mutate(__={"calculate_noderoute": True})


def _create_bus_map():
    station_from = create_station(id=101, t_type=TransportType.BUS_ID)
    station_to = create_station(id=102, t_type=TransportType.BUS_ID)

    create_thread(
        uid="uid1",
        t_type=TransportType.BUS_ID,
        year_days=[date(2020, 12, 22)],
        tz_start_time=time(1),
        schedule_v1=[
            [None, 0, station_from],
            [60, None, station_to]
        ]
    )

    create_thread(
        uid="uid2",

        t_type=TransportType.BUS_ID,
        type=RThreadType.INTERVAL_ID,
        year_days=[date(2020, 12, 22)],
        begin_time=time(9),
        end_time=time(22),
        schedule_v1=[
            [None, 0, station_from],
            [60, None, station_to]
        ]
    )

    return station_from


def _create_suburban_map():
    station_from = create_station(id=103, t_type=TransportType.SUBURBAN_ID)
    station_to = create_station(id=104, t_type=TransportType.SUBURBAN_ID)

    dir_vpered = {
        "departure_subdir": "vpered",
        "departure_direction": create_direction(code="vpered")
    }
    dir_nazad = {
        "departure_subdir": "nazad",
        "departure_direction": create_direction(code="nazad")
    }

    plan = create_train_schedule_plan(start_date=datetime.now() + timedelta(days=2))

    create_thread(
        uid="uid3",
        translated_days_texts='[{}, {"ru": "ежедневно"}]',
        t_type=TransportType.SUBURBAN_ID,
        year_days=[date(2020, 12, 22)],
        tz_start_time=time(1),
        schedule_v1=[
            [None, 0, station_from, dir_vpered],
            [60, None, station_to]
        ]
    )

    create_thread(
        uid="uid4",
        schedule_plan=plan,
        translated_days_texts='[{}, {"ru": "ежедневно"}]',
        t_type=TransportType.SUBURBAN_ID,
        year_days=[date(2020, 12, 22)],
        tz_start_time=time(2),
        schedule_v1=[
            [None, 0, station_from, dir_nazad],
            [60, None, station_to]
        ]
    )

    return station_from


def _get_scheduler(t_type_id):
    station = _create_bus_map() if t_type_id == TransportType.BUS_ID else _create_suburban_map()
    t_type = TransportType.objects.get(id=t_type_id)
    query = {
        "transport_types": [t_type],
        "dt": None,
        "result_pytz": None,
        "event": "departure",
        "show_systems": ["yandex"],
        "direction": None
    }
    return RaspDbScheduler(query, station, [t_type.code])


@replace_now("2020-12-22")
def test_rasp_db_scheduler():
    scheduler = _get_scheduler(TransportType.BUS_ID)
    routes = scheduler.get_schedule()

    assert scheduler.direction is None
    assert scheduler.next_plan is None
    assert scheduler.t_type_codes == ["bus"]
    assert scheduler.schedule_type == "schedule"

    assert len(routes) == 2

    scheduler = _get_scheduler(TransportType.SUBURBAN_ID)
    routes = scheduler.get_schedule()

    assert scheduler.t_type_codes == ["suburban"]
    assert scheduler.schedule_type == "suburban"

    assert len(routes) == 2


@replace_now("2020-12-22")
def test_rasp_db_schedule2json():
    scheduler = _get_scheduler(TransportType.BUS_ID)
    routes = scheduler.get_schedule()
    schedule_json = scheduler.schedule2json("pagination_json", routes)

    assert_that(schedule_json, has_entries({
        "pagination": "pagination_json",
        "schedule": contains(has_entries({
            "thread": has_entries({"uid": "uid1"})
        })),
        "interval_schedule": contains(has_entries({
            "thread": has_entries({"uid": "uid2"})
        }))
    }))


@replace_now("2020-12-22")
def test_rasp_db_schedule2json_suburban():
    scheduler = _get_scheduler(TransportType.SUBURBAN_ID)
    routes = scheduler.get_schedule()
    schedule_json = scheduler.schedule2json("pagination_json", routes)

    assert_that(schedule_json, has_entries({
        "pagination": "pagination_json",
        "schedule": contains(
            has_entries({
                "thread": has_entries({"uid": "uid3"}),
                "days": "ежедневно"
            }),
            has_entries({
                "thread": has_entries({"uid": "uid4"}),
                "days": "ежедневно " + scheduler.next_plan.L_appendix()
            }),
        ),
        "interval_schedule": [],
        "directions": contains_inanyorder(
            {"code": "vpered", "title": "vpered"},
            {"code": "nazad", "title": "nazad"},
            {"code": "all", "title": "все направления"}
        ),
        "schedule_direction": has_entries({"code": "all", "title": "все направления"})
    }))
