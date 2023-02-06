# coding: utf8
from __future__ import absolute_import, division, print_function, unicode_literals

import pytz
from datetime import date, time

import mock
import pytest
from hamcrest import assert_that, has_properties, contains_inanyorder, has_entries, contains

from common.tester.factories import create_rthread_segment, create_train_schedule_plan
from common.tester.utils.datetime import replace_now
from common.tester.factories import create_station, create_thread, create_settlement
from common.models.transport import TransportType
from common.models.schedule import RThreadType
from route_search.models import RThreadSegment, IntervalRThreadSegment, AllDaysRThreadSegment

from travel.rasp.api_public.api_public.v3.search.rasp_db_search import (
    BaseRaspDbSearch, OneDayRaspDbSearch, AllDaysRaspDbSearch
)
from travel.rasp.api_public.tests.v3.search.helpers import RequestStub, QueryPointsStub, get_stub_currency_info


pytestmark = [pytest.mark.dbuser]
create_thread = create_thread.mutate(__={"calculate_noderoute": True})


@replace_now("2014-11-30")
def test_set_train_schedule_plan():
    segments = [create_rthread_segment() for _ in range(3)]
    result_threads = set(s.thread for s in segments)

    with mock.patch("common.models.schedule.TrainSchedulePlan.add_to_threads") as m_add:
        plans = (create_train_schedule_plan(), create_train_schedule_plan())
        m_add.return_value = plans

        BaseRaspDbSearch._set_train_schedule_plan(segments)

        assert all(s.next_plan == plans[1] for s in segments)

        assert len(m_add.call_args_list) == 1
        threads, dt = m_add.call_args_list[0][0]
        assert set(threads) == result_threads
        assert dt == date(2014, 11, 30)


def _create_map():
    city_from = create_settlement(id=1001, time_zone="Etc/GMT-5")
    city_to = create_settlement(id=1002, time_zone="Etc/GMT-5")
    station_from = create_station(settlement=city_from, id=101, time_zone="Etc/GMT-5")
    station_to = create_station(settlement=city_to, id=102, time_zone="Etc/GMT-5")

    create_thread(
        t_type=TransportType.BUS_ID,
        year_days=[date(2020, 12, 12)],
        tz_start_time=time(1),
        schedule_v1=[
            [None, 0, station_from, {"time_zone": "Etc/GMT-5"}],
            [60, None, station_to, {"time_zone": "Etc/GMT-5"}]
        ]
    )

    create_thread(
        t_type=TransportType.BUS_ID,
        type=RThreadType.INTERVAL_ID,
        year_days=[date(2020, 12, 12)],
        begin_time=time(9),
        end_time=time(22),
        schedule_v1=[
            [None, 0, station_from, {"time_zone": "Etc/GMT-5"}],
            [60, None, station_to, {"time_zone": "Etc/GMT-5"}]
        ]
    )

    return city_from, city_to


def test_base_rasp_db_search():
    city_from, city_to = _create_map()

    search = BaseRaspDbSearch(
        {
            "add_days_mask": False,
            "result_pytz": None,
            "transport_types": [
                TransportType.get_plane_type(), TransportType.get_train_type(), TransportType.get_bus_type()
            ]
        },
        RequestStub(),
        QueryPointsStub(city_from, city_to)
    )

    assert search.client_city is None

    assert len(search.t_types) == 2
    assert TransportType.get_bus_type() in search.t_types
    assert TransportType.get_train_type() in search.t_types
    assert TransportType.get_plane_type() not in search.t_types


@replace_now("2020-12-12")
def test_one_day_rasp_db_search():
    city_from, city_to = _create_map()
    currency_info = get_stub_currency_info()
    search = OneDayRaspDbSearch(
        {
            "add_days_mask": True,
            "result_pytz": pytz.timezone("Etc/GMT-3"),
            "transport_types": [TransportType.get_bus_type()]
        },
        RequestStub(),
        QueryPointsStub(city_from, city_to, date(2020, 12, 12)),
        currency_info
    )
    search.search()

    assert_that(list(search.used_points), contains_inanyorder(
        has_properties({"id": 101}),
        has_properties({"id": 102}),
        has_properties({"id": 1001}),
        has_properties({"id": 1002}),
    ))

    assert len(search.segments) == 1
    assert isinstance(search.segments[0], RThreadSegment) is True
    assert_that(search.segments[0].schedule, contains(
        has_entries({
            "year": "2020", "month": "12",
            "days": [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
        })
    ))

    assert len(search.interval_segments) == 1
    assert isinstance(search.interval_segments[0], IntervalRThreadSegment) is True
    assert_that(search.interval_segments[0].schedule, contains(
        has_entries({
            "year": "2020", "month": "12",
            "days": [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
        })
    ))


@replace_now("2020-12-12")
def test_all_days_rasp_db_search():
    city_from, city_to = _create_map()
    search = AllDaysRaspDbSearch(
        {
            "add_days_mask": True,
            "result_pytz": None,
            "transport_types": [TransportType.get_bus_type()]
        },
        RequestStub(),
        QueryPointsStub(city_from, city_to)
    )
    search.search()

    assert len(search.segments) == 1
    assert isinstance(search.segments[0], AllDaysRThreadSegment) is True
    assert_that(search.segments[0].schedule, contains(
        has_entries({
            "year": "2020", "month": "12",
            "days": [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
        })
    ))

    assert len(search.interval_segments) == 1
    assert isinstance(search.interval_segments[0], IntervalRThreadSegment) is True
    assert_that(search.interval_segments[0].schedule, contains(
        has_entries({
            "year": "2020", "month": "12",
            "days": [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
        })
    ))
