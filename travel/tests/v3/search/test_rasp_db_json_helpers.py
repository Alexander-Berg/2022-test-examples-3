# coding: utf8
from __future__ import absolute_import, division, print_function, unicode_literals

import pytz
from datetime import date, time

import mock
import pytest
from hamcrest import assert_that, has_entries, contains

from common.tester.factories import (
    create_station, create_train_schedule_plan, create_thread, create_company
)
from common.tester.factories import create_rthread_segment
from common.tester.utils.datetime import replace_now
from travel.rasp.library.python.common23.date import environment
from common.models.transport import TransportType
from common.models.schedule import RThreadType

from travel.rasp.api_public.api_public.v3.search.helpers import fill_thread_local_start_dt
from travel.rasp.api_public.api_public.v3.search.rasp_db_json_helpers import rasp_db_segment2json, interval_segment2json
from travel.rasp.api_public.api_public.v3.core.helpers import get_code_getter
from travel.rasp.api_public.api_public.v3.search.rasp_db_search import OneDayRaspDbSearch, AllDaysRaspDbSearch
from travel.rasp.api_public.tests.v3.search.helpers import QueryPointsStub, RequestStub, get_stub_currency_info


pytestmark = [pytest.mark.dbuser]
create_thread = create_thread.mutate(__={"calculate_noderoute": True})


def _make_map():
    station_from = create_station(id=101, time_zone="Etc/GMT-5")
    station_to = create_station(id=102, time_zone="Etc/GMT-5")
    company = create_company(id=301, title="Company", address="addr")

    create_thread(
        t_type=TransportType.BUS_ID,
        number="111",
        uid="uid1",
        company=company,
        year_days=[date(2020, 12, 12)],
        tz_start_time=time(1),
        schedule_v1=[
            [None, 0, station_from, {"time_zone": "Etc/GMT-5", "platform": "1"}],
            [60, None, station_to, {"time_zone": "Etc/GMT-5", "platform": "2"}]
        ]
    )

    create_thread(
        t_type=TransportType.BUS_ID,
        type=RThreadType.INTERVAL_ID,
        number="222",
        uid="uid2",
        year_days=[date(2020, 12, 12)],
        begin_time=time(9),
        end_time=time(22),
        density=30,
        schedule_v1=[
            [None, 0, station_from, {"time_zone": "Etc/GMT-5"}],
            [60, None, station_to, {"time_zone": "Etc/GMT-5"}]
        ]
    )

    code_getter = get_code_getter((station_from, station_to), ["yandex"])
    return code_getter, station_from, station_to


@replace_now("2020-12-12")
def test_one_day_rasp_db_segment():
    code_getter, station_from, station_to = _make_map()

    currency_info = get_stub_currency_info()
    search = OneDayRaspDbSearch(
        {
            "add_days_mask": True,
            "result_pytz": pytz.timezone("Etc/GMT-3"),
            "transport_types": None,
        },
        RequestStub(),
        QueryPointsStub(station_from, station_to, date(2020, 12, 12)),
        currency_info
    )
    search.search()
    fill_thread_local_start_dt(search.segments)

    assert len(search.segments) == 1

    segment = search.segments[0]
    segment_json = rasp_db_segment2json(
        segment, station_from, station_to, date(2020, 12, 12),
        code_getter, "thread_url/?", pytz.timezone("Etc/GMT-3"), currency_info
    )

    assert_that(segment_json, has_entries({
        "thread": has_entries({
            "number": "111",
            "uid": "uid1",
            "carrier": has_entries({"code": 301, "address": "addr"}),
            "thread_method_link": "thread_url/?date=2020-12-12&uid=uid1"
        }),
        "from": has_entries({"code": "s101"}),
        "to": has_entries({"code": "s102"}),
        "departure_platform": "1",
        "arrival_platform": "2",
        "duration": 3600,
        "schedule": contains(has_entries({
            "year": "2020", "month": "12",
            "days": [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
        })),
        "has_transfers": False,
        "departure": "2020-12-11T23:00:00+03:00",
        "arrival": "2020-12-12T00:00:00+03:00",
        "start_date": "2020-12-11"
    }))

    assert "tickets_info" in segment_json
    assert "days" not in segment_json
    assert "except_days" not in segment_json


@replace_now("2020-12-12")
def test_all_days_rasp_db_segment():
    code_getter, station_from, station_to = _make_map()

    search = AllDaysRaspDbSearch(
        {
            "add_days_mask": False,
            "result_pytz": None,
            "transport_types": None,
        },
        RequestStub(),
        QueryPointsStub(station_from, station_to),
    )
    search.search()
    fill_thread_local_start_dt(search.segments)

    assert len(search.segments) == 1

    segment = search.segments[0]
    segment_json = rasp_db_segment2json(
        segment, station_from, station_to, None, code_getter, "thread_url/?"
    )

    assert_that(segment_json, has_entries({
        "thread": has_entries({"uid": "uid1"}),
        "from": has_entries({"code": "s101"}),
        "to": has_entries({"code": "s102"}),
        "duration": 3600,
        "departure": "01:00:00",
        "arrival": "02:00:00",
        "start_date": "2020-12-12"
    }))

    assert "has_transfers" not in segment_json
    assert "tickets_info" not in segment_json
    assert "schedule" not in segment_json
    assert "days" in segment_json
    assert "except_days" in segment_json


@replace_now("2020-12-12")
def test_one_day_interval_segment():
    code_getter, station_from, station_to = _make_map()

    currency_info = get_stub_currency_info()
    search = OneDayRaspDbSearch(
        {
            "add_days_mask": False,
            "result_pytz": None,
            "transport_types": None,
        },
        RequestStub(),
        QueryPointsStub(station_from, station_to, date(2020, 12, 12)),
        currency_info
    )
    search.search()

    assert len(search.interval_segments) == 1

    segment = search.interval_segments[0]
    segment_json = interval_segment2json(
        segment, station_from, station_to, date(2020, 12, 12), code_getter, "thread_url/?", currency_info=currency_info
    )

    assert_that(segment_json, has_entries({
        "thread": has_entries({
            "number": "222",
            "uid": "uid2",
            "thread_method_link": "thread_url/?date=2020-12-12&uid=uid2",
            "interval": has_entries({
                "density": "30",
                "begin_time": "2020-12-12T09:00:00",
                "end_time": "2020-12-12T22:00:00"
            })
        }),
        "from": has_entries({"code": "s101"}),
        "to": has_entries({"code": "s102"}),
        "departure_platform": "",
        "arrival_platform": "",
        "duration": 3600,
        "has_transfers": False,
        "start_date": "2020-12-12"
    }))

    assert "tickets_info" in segment_json
    assert "days" not in segment_json
    assert "except_days" not in segment_json
    assert "departure" not in segment_json
    assert "arrival" not in segment_json


@replace_now("2020-12-12")
def test_all_days_interval_segment():
    code_getter, station_from, station_to = _make_map()

    search = AllDaysRaspDbSearch(
        {
            "add_days_mask": False,
            "result_pytz": None,
            "transport_types": None,
        },
        RequestStub(),
        QueryPointsStub(station_from, station_to),
    )
    search.search()

    assert len(search.interval_segments) == 1

    segment = search.interval_segments[0]
    segment_json = interval_segment2json(
        segment, station_from, station_to, None, code_getter, "thread_url/?"
    )

    assert_that(segment_json, has_entries({
        "thread": has_entries({
            "uid": "uid2",
            "interval": has_entries({
                "density": "30",
                "begin_time": time(9, 0, 0),
                "end_time": time(22, 0, 0)
            })
        }),
        "duration": 3600,
        "start_date": "2020-12-12"
    }))

    assert "tickets_info" not in segment_json
    assert "days" in segment_json
    assert "except_days" in segment_json


def test_next_plan():
    segment = create_rthread_segment()
    segment.thread_local_start_dt = environment.now()

    def check_call(next_plan):
        with mock.patch("common.models.schedule.RThread.L_days_text_dict") as m_text:
            m_text.return_value = {"days_text": "bla"}
            result = rasp_db_segment2json(segment, create_station(), create_station(), None, None, "")

            assert len(m_text.call_args_list) == 1
            assert m_text.call_args_list[0][1] == {"next_plan": next_plan}

            assert result["days"] == "bla"

    check_call(None)

    segment.next_plan = create_train_schedule_plan()
    check_call(segment.next_plan)
