# coding: utf8
from __future__ import absolute_import, division, print_function, unicode_literals

import pytz
from datetime import datetime
from dateutil import parser

import pytest
from hamcrest import assert_that, has_entries, contains

from common.tester.factories import create_station, create_settlement, create_company, create_transport_model

from travel.rasp.api_public.api_public.v3.search.baris_segments import (
    OneDayBarisSegment, AllDaysBarisSegment, _get_tz_times, baris_segment2json
)
from travel.rasp.api_public.api_public.v3.core.helpers import get_code_getter


pytestmark = [pytest.mark.dbuser]


class BarisDataStub(object):
    def __init__(self, stations, companies=None, transport_models=None):
        self.stations_by_ids = {s.id: s for s in stations}
        self.settlements_by_ids = {s.settlement.id: s.settlement for s in stations if s.settlement}
        self.companies_by_ids = {c.id: c for c in companies} if companies else {}
        self.transport_models_by_ids = {m.id: m for m in transport_models} if transport_models else {}


def test_one_day_baris_segment():
    baris_data = BarisDataStub([create_station(id=101), create_station(id=102)])

    baris_flight = {
        "title": "SU 1",
        "departureStation": 101,
        "arrivalStation": 102,
        "startDatetime": "2020-12-12T01:10:00+03:00",
        "departureDatetime": "2020-12-12T02:20:00+04:00",
        "arrivalDatetime": "2020-12-12T03:30:00+05:00",
        "route": [101, 102]
    }

    segment = OneDayBarisSegment(baris_flight, baris_data)

    assert segment.number == "SU 1"
    assert segment.start.isoformat() == "2020-12-12T01:10:00+03:00"
    assert segment.departure.isoformat() == "2020-12-12T02:20:00+04:00"
    assert segment.arrival.isoformat() == "2020-12-12T03:30:00+05:00"

    # Добавление сегмента с расписанием на все дни
    all_days_flight = {"runDays": "дни"}
    segment.set_all_days_flight(all_days_flight)
    assert segment.run_days == "дни"

    all_days_flight["tzRunDays"] = "сдвинутые дни"
    segment.set_all_days_flight(all_days_flight)
    assert segment.run_days == "сдвинутые дни"


def test_all_days_baris_segment():
    baris_data = BarisDataStub([create_station(id=101, time_zone="Etc/GMT-3"), create_station(id=102)])

    baris_flight = {
        "title": "SU 1",
        "departureStation": 101,
        "arrivalStation": 102,
        "naiveStart": datetime(2020, 12, 12, 1, 10, 0),
        "departure": parser.parse("2020-12-12T02:20:00+04:00"),
        "arrival": parser.parse("2020-12-12T03:30:00+05:00"),
        "route": [101, 102],
        "runDays": "дни",
        "daysText": "текст дней"
    }

    segment = AllDaysBarisSegment(baris_flight, baris_data)

    assert segment.number == "SU 1"
    assert segment.start.isoformat() == "2020-12-12T01:10:00+03:00"
    assert segment.departure.isoformat() == "2020-12-12T02:20:00+04:00"
    assert segment.arrival.isoformat() == "2020-12-12T03:30:00+05:00"
    assert segment.run_days == "дни"
    assert segment.days_text == "текст дней"

    baris_flight.update({
        "tzRunDays": "сдвинутые дни",
        "tzDaysText": "сдвинутый текст дней"
    })

    segment = AllDaysBarisSegment(baris_flight, baris_data)

    assert segment.run_days == "сдвинутые дни"
    assert segment.days_text == "сдвинутый текст дней"


class SegmentTimesStub(object):
    def __init__(self):
        self.start = parser.parse("2020-12-12T01:10:00+03:00")
        self.departure = parser.parse("2020-12-12T02:20:00+04:00")
        self.arrival = parser.parse("2020-12-12T03:30:00+05:00")


def test_get_tz_times():
    segment = SegmentTimesStub()

    _get_tz_times(segment, None)

    assert segment.start.isoformat() == "2020-12-12T01:10:00+03:00"
    assert segment.departure.isoformat() == "2020-12-12T02:20:00+04:00"
    assert segment.arrival.isoformat() == "2020-12-12T03:30:00+05:00"

    _get_tz_times(segment, pytz.timezone("Etc/GMT-3"))

    assert segment.start.isoformat() == "2020-12-12T01:10:00+03:00"
    assert segment.departure.isoformat() == "2020-12-12T01:20:00+03:00"
    assert segment.arrival.isoformat() == "2020-12-12T01:30:00+03:00"


UNIVERSAL_BARIS_FLIGHT = {
    "title": "SU 1",
    "departureStation": 101,
    "departureTerminal": "A",
    "arrivalStation": 102,
    "arrivalTerminal": "B",
    "route": [101, 102],
    "airlineID": 301,
    "transportModelID": 201,
    "tzRunDays": {
        "2020": {
            "12": [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
        }
    },
    "daysText": "дни",

    "startDatetime": "2020-12-12T00:10:00+04:00",
    "departureDatetime": "2020-12-12T00:10:00+04:00",
    "arrivalDatetime": "2020-12-12T01:20:00+05:00",

    "naiveStart": datetime(2020, 12, 12, 0, 10, 0),
    "departure": parser.parse("2020-12-12T00:10:00+04:00"),
    "arrival": parser.parse("2020-12-12T01:20:00+05:00"),
}


def _make_map():
    city_from = create_settlement(id=1001, time_zone="Etc/GMT-4", title="CityFrom")
    city_to = create_settlement(id=1002, time_zone="Etc/GMT-5", title="CityTo")
    station_from = create_station(id=101, settlement=city_from, time_zone="Etc/GMT-4", title="StationFrom")
    station_to = create_station(id=102, settlement=city_to, time_zone="Etc/GMT-5", title="StationTo")
    company = create_company(id=301, title="Company")
    model = create_transport_model(id=201, title="Model")

    code_getter = get_code_getter((city_from, city_to, station_from, station_to), ["yandex"])
    baris_data = BarisDataStub([station_from, station_to], [company], [model])
    return code_getter, baris_data, city_from, city_to


def test_one_day_baris_segment2json():
    code_getter, baris_data, _, _ = _make_map()
    segment = OneDayBarisSegment(UNIVERSAL_BARIS_FLIGHT, baris_data)
    segment_json = baris_segment2json(segment, code_getter)

    assert_that(segment_json, has_entries({
        "thread": has_entries({
            "number": "SU 1",
            "title": "CityFrom \u2014 CityTo",
            "short_title": "CityFrom \u2014 CityTo",
            "carrier": has_entries({"code": 301, "title": "Company"}),
            "vehicle": "Model",
            "express_type": None,
            "transport_type": "plane",
            "transport_subtype": has_entries({"title": None, "code": None, "color": None}),
            "uid": "SU-1_201212_c301_12"
        }),

        "from": has_entries({"code": "s101", "title": "StationFrom"}),
        "to": has_entries({"code": "s102", "title": "StationTo"}),
        "departure_platform": "",
        "arrival_platform": "",
        "departure_terminal": "A",
        "arrival_terminal": "B",
        "stops": "",
        "duration": 600,
        "start_date": "2020-12-12",

        "departure": "2020-12-12T00:10:00+04:00",
        "arrival": "2020-12-12T01:20:00+05:00",
        "has_transfers": False,
        "tickets_info": has_entries({"et_marker": False, "places": []}),
    }))

    assert "thread_method_link" not in segment_json["thread"]
    assert "schedule" not in segment_json
    assert "days" not in segment_json
    assert "except_days" not in segment_json


def test_all_days_baris_segment2json():
    code_getter, baris_data, city_from, city_to = _make_map()
    segment = AllDaysBarisSegment(UNIVERSAL_BARIS_FLIGHT, baris_data)
    segment_json = baris_segment2json(
        segment, code_getter, "thread_url/?", pytz.timezone("Etc/GMT-3"), city_from, city_to
    )

    assert_that(segment_json, has_entries({
        "thread": has_entries({
            "number": "SU 1",
            "title": "CityFrom \u2014 CityTo",
            "thread_method_link": "thread_url/?date=2020-12-12&uid=SU-1_201212_c301_12"
        }),
        "from": has_entries({"code": "c1001", "title": "CityFrom"}),
        "to": has_entries({"code": "c1002", "title": "CityTo"}),
        "duration": 600,
        "start_date": "2020-12-11",

        "departure": "23:10:00",
        "arrival": "23:20:00",
        "days": "дни",
        "except_days": "",
        "schedule": contains({
            "year": "2020", "month": "12",
            "days": [0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0]
        }),
    }))

    assert "has_transfers" not in segment_json
    assert "tickets_info" not in segment_json
