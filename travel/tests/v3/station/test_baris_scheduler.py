# coding: utf8
from __future__ import absolute_import, division, print_function, unicode_literals

import pytz
from datetime import datetime

import pytest
from hamcrest import assert_that, has_entries

from common.models.transport import TransportType
from common.data_api.baris.test_helpers import mock_baris_response
from common.tester.factories import create_station, create_company, create_transport_model
from common.tester.utils.datetime import replace_now

from travel.rasp.api_public.api_public.v3.station.baris_scheduler import OneDayBarisScheduler, AllDaysBarisScheduler

pytestmark = [pytest.mark.dbuser]
create_station = create_station.mutate(
    t_type=TransportType.PLANE_ID, type_choices="tablo", time_zone="Etc/GMT-3", tablo_state="real"
)


ONE_DAY_DEPARTURE_TABLO_BARIS_RESPONSE = {
    "direction": "departure",
    "station": 102,
    "flights": [
        {
            "airlineID": 301,
            "transportModelID": 201,
            "title": "SU 1",
            "datetime": "2020-12-22T01:30:00+03:00",
            "startDatetime": "2020-12-21T22:30:00+03:00",
            "terminal": "A",
            "route": [101, 102, 103],
            "status": {
                "departure": "2020-12-22 02:30:00",
                "status": "unknown",
                "departureTerminal": "B",
            }
        },
        {
            "airlineID": 301,
            "transportModelID": 201,
            "title": "SU 2",
            "datetime": "2020-12-22T11:30:00+03:00",
            "terminal": "",
            "route": [102, 103],
            "status": {
                "departure": "",
                "status": "cancelled",
                "departureTerminal": "",
            }
        },
        {
            "title": "SU 3",
            "airlineID": 301,
            "transportModelID": 201,
            "datetime": "2020-12-21T15:00:00+03:00",
            "terminal": "A",
            "route": [102, 103],
            "status": {
                "departure": "2020-12-22 01:00:00",
                "status": "",
                "departureTerminal": "B",
            }
        }
    ]
}


ONE_DAY_ARRIVAL_TABLO_BARIS_RESPONSE = {
    "direction": "arrival",
    "station": 102,
    "flights": [
        {
            "airlineID": 301,
            "transportModelID": 201,
            "title": "SU 1",
            "datetime": "2020-12-22T01:30:00+03:00",
            "startDatetime": "2020-12-21T22:30:00+03:00",
            "terminal": "A",
            "route": [101, 102],
            "status": {
                "arrival": "2020-12-22 02:30:00",
                "status": "unknown",
                "arrivalTerminal": "B",
            }
        }
    ]
}


ALL_DAYS_DEPARTURE_TABLO_BARIS_RESPONSE = {
    "direction": "departure",
    "station": 102,
    "flights": [
        {
            "airlineID": 301,
            "title": "SU 1",
            "schedules": [
                {
                    "time": "01:30",
                    "startTime": "22:30",
                    "startDayShift": -1,
                    "terminal": "A",
                    "transportModelID": 201,
                    "route": [101, 102, 103],
                    "masks": [
                        {
                            "from": "2020-12-22",
                            "until": "2020-12-23",
                            "on": 23
                        }
                    ]
                },
                {
                    "time": "01:40",
                    "startTime": "22:40",
                    "startDayShift": -1,
                    "terminal": "A",
                    "transportModelID": 201,
                    "route": [102, 103],
                    "masks": [
                        {
                            "from": "2020-12-24",
                            "until": "2020-12-25",
                            "on": 45
                        }
                    ]
                }
            ]
        },
        {
            "airlineID": 301,
            "title": "SU 2",
            "schedules": [
                {
                    "time": "11:30",
                    "startTime": "08:30",
                    "startDayShift": 0,
                    "terminal": "A",
                    "transportModelID": 201,
                    "route": [102, 103],
                    "masks": [
                        {
                            "from": "2020-12-21",
                            "until": "2020-12-22",
                            "on": 12
                        }
                    ]
                }
            ]
        }
    ]
}


ALL_DAYS_ARRIVAL_TABLO_BARIS_RESPONSE = {
    "direction": "arrival",
    "station": 102,
    "flights": [
        {
            "airlineID": 301,
            "title": "SU 1",
            "schedules": [
                {
                    "time": "01:30",
                    "startTime": "22:30",
                    "startDayShift": -1,
                    "terminal": "A",
                    "transportModelID": 201,
                    "route": [101, 102],
                    "masks": [
                        {
                            "from": "2020-12-22",
                            "until": "2020-12-23",
                            "on": 23
                        }
                    ]
                }
            ]
        }
    ]
}


def _create_map():
    create_station(id=101, title="Airport 1")
    station = create_station(id=102, title="Airport 2")
    create_station(id=103, title="Airport 3")
    create_company(id=301)
    create_transport_model(id=201, title="Model")

    return station


def _get_scheduler(dt=None, event="departure", transport_types=None, result_pytz=None, tablo=False):
    station = _create_map()
    query = {
        "transport_types": transport_types,
        "dt": dt,
        "result_pytz": result_pytz,
        "event": event,
        "show_systems": ["yandex"],
        "tablo": tablo
    }
    if dt:
        return OneDayBarisScheduler(query, station)
    return AllDaysBarisScheduler(query, station)


@replace_now('2020-12-22')
def test_one_day_baris_scheduler():
    scheduler = _get_scheduler(datetime(2020, 12, 22, 0, 0, 0))

    assert scheduler.show_tablo is False
    assert scheduler.baris_data is None


@replace_now('2020-12-22')
def test_one_day_departure_baris_schedule():
    scheduler = _get_scheduler(
        datetime(2020, 12, 22, 0, 0, 0),
        transport_types=[TransportType.get_plane_type()],
        tablo=True
    )

    assert scheduler.show_tablo is True

    with mock_baris_response(ONE_DAY_DEPARTURE_TABLO_BARIS_RESPONSE) as m_get_baris_response:
        flights = scheduler.get_schedule()

        m_get_baris_response.assert_called_with(
            "flight-board/102/",
            params={
                "after": "2020-12-22T00:00:00",
                "before": "2020-12-23T00:00:00",
                "direction": "departure"
            }
        )

        assert 101 in scheduler.baris_data.stations_by_ids
        assert 102 in scheduler.baris_data.stations_by_ids
        assert 103 in scheduler.baris_data.stations_by_ids

        assert len(flights) == 2
        assert "parsedDatetime" in flights[0]
        assert flights[0]["parsedDatetime"].isoformat() == "2020-12-22T01:30:00+03:00"
        assert "parsedDatetime" in flights[1]
        assert flights[1]["parsedDatetime"].isoformat() == "2020-12-22T11:30:00+03:00"

    with mock_baris_response(ALL_DAYS_DEPARTURE_TABLO_BARIS_RESPONSE) as m_get_baris_response:
        scheduler.add_extra_data(flights)

        m_get_baris_response.assert_called_with(
            "flight-board/102/schedule/",
            params={"direction": "departure"},
            timeout=10
        )

        assert "daysText" in flights[0]
        assert flights[0]["daysText"] == "22, 23\xa0декабря"
        assert "daysText" in flights[0]
        assert flights[1]["daysText"] == "21, 22\xa0декабря"


@replace_now('2020-12-22')
def test_one_day_arrival_baris_schedule():
    scheduler = _get_scheduler(
        datetime(2020, 12, 22, 0, 0, 0),
        result_pytz=pytz.timezone("Etc/GMT-1"),
        tablo=True, event="arrival"
    )

    with mock_baris_response(ONE_DAY_ARRIVAL_TABLO_BARIS_RESPONSE) as m_get_baris_response:
        flights = scheduler.get_schedule()

        m_get_baris_response.assert_called_with(
            "flight-board/102/",
            params={
                "after": "2020-12-22T00:00:00",
                "before": "2020-12-23T00:00:00",
                "direction": "arrival"
            }
        )

        assert len(flights) == 1
        assert "parsedDatetime" in flights[0]
        assert flights[0]["parsedDatetime"].isoformat() == "2020-12-22T01:30:00+03:00"

    with mock_baris_response(ALL_DAYS_ARRIVAL_TABLO_BARIS_RESPONSE) as m_get_baris_response:
        scheduler.add_extra_data(flights)

        m_get_baris_response.assert_called_with(
            "flight-board/102/schedule/",
            params={"direction": "arrival"},
            timeout=10
        )

        assert "daysText" in flights[0]
        assert flights[0]["daysText"] == "21, 22\xa0декабря"


@replace_now('2020-12-22')
def test_all_days_departure_baris_scheduler():
    scheduler = _get_scheduler()

    with mock_baris_response(ALL_DAYS_DEPARTURE_TABLO_BARIS_RESPONSE) as m_get_baris_response:
        flights = scheduler.get_schedule()

        m_get_baris_response.assert_called_with(
            "flight-board/102/schedule/",
            params={"direction": "departure"},
            timeout=10
        )

        assert len(flights) == 3
        assert flights[0]["time"] == "01:30"
        assert flights[1]["time"] == "01:40"
        assert flights[2]["time"] == "11:30"


@replace_now('2020-12-22')
def test_all_days_arrival_baris_scheduler():
    scheduler = _get_scheduler(event="arrival")

    with mock_baris_response(ALL_DAYS_ARRIVAL_TABLO_BARIS_RESPONSE) as m_get_baris_response:
        flights = scheduler.get_schedule()

        m_get_baris_response.assert_called_with(
            "flight-board/102/schedule/",
            params={"direction": "arrival"},
            timeout=10
        )

        assert len(flights) == 1
        assert flights[0]["time"] == "01:30"


@replace_now('2020-12-22')
def test_base_baris_schedule_route2json():
    scheduler = _get_scheduler(event="arrival")

    with mock_baris_response(ALL_DAYS_ARRIVAL_TABLO_BARIS_RESPONSE):
        flights = scheduler.get_schedule()

        flight_json = scheduler.baris_schedule_route2json(flights[0])

        assert_that(flight_json, has_entries({
            "thread": has_entries({
                "number": "SU 1",
                "title": "Airport 1 \u2014 Airport 2",
                "short_title": "Airport 1 \u2014 Airport 2",
                "carrier": has_entries({"code": 301}),
                "vehicle": "Model",
                "transport_type": "plane",
                "express_type": None,
                "transport_subtype": has_entries({"title": None, "code": None, "color": None})
            }),
            "terminal": "A",
            "is_fuzzy": False,
            "stops": "",
            "platform": "",
            "except_days": None
        }))


@replace_now('2020-12-22')
def test_one_day_departure_baris_schedule_route2json():
    scheduler = _get_scheduler(datetime(2020, 12, 22, 0, 0, 0), tablo=True)

    with mock_baris_response(ONE_DAY_DEPARTURE_TABLO_BARIS_RESPONSE):
        flights = scheduler.get_schedule()

    with mock_baris_response(ALL_DAYS_DEPARTURE_TABLO_BARIS_RESPONSE):
        scheduler.add_extra_data(flights)

    flight_json = scheduler.baris_schedule_route2json(flights[0])

    assert_that(flight_json, has_entries({
        "thread": has_entries({
            "number": "SU 1",
            "uid": "SU-1_201221_c301_12"
        }),
        "departure": "2020-12-22T01:30:00+03:00",
        "arrival": None,
        "days": "22, 23\xa0декабря",
        "tablo": has_entries({
            "departure": "2020-12-22T01:30:00",
            "real_departure": "2020-12-22T02:30:00",
            "arrival": None,
            "real_arrival": None,
            "terminal": "B",
            "status": None
        })
    }))

    flight_json = scheduler.baris_schedule_route2json(flights[1])

    assert_that(flight_json, has_entries({
        "thread": has_entries({
            "number": "SU 2",
            "uid": "SU-2_201222_c301_12"
        }),
        "tablo": has_entries({
            "departure": "2020-12-22T11:30:00",
            "real_departure": None,
            "arrival": None,
            "real_arrival": None,
            "terminal": None,
            "status": "cancelled"
        })
    }))


@replace_now('2020-12-22')
def test_one_day_arrival_baris_schedule_route2json():
    scheduler = _get_scheduler(
        datetime(2020, 12, 22, 0, 0, 0),
        tablo=True, event="arrival",
        result_pytz=pytz.timezone("Etc/GMT-1")
    )

    with mock_baris_response(ONE_DAY_ARRIVAL_TABLO_BARIS_RESPONSE):
        flights = scheduler.get_schedule()

    with mock_baris_response(ALL_DAYS_ARRIVAL_TABLO_BARIS_RESPONSE):
        scheduler.add_extra_data(flights)

    flight_json = scheduler.baris_schedule_route2json(flights[0])

    assert_that(flight_json, has_entries({
        "thread": has_entries({
            "number": "SU 1",
            "uid": "SU-1_201221_c301_12"
        }),
        "departure": None,
        "arrival": "2020-12-21T23:30:00+01:00",
        "days": "21, 22\xa0декабря",
        "tablo": has_entries({
            "departure": None,
            "real_departure": None,
            "arrival": "2020-12-22T01:30:00",
            "real_arrival": "2020-12-22T02:30:00",
            "terminal": "B",
            "status": None
        })
    }))


@replace_now('2020-12-22')
def test_all_days_departure_baris_schedule_route2json():
    scheduler = _get_scheduler()

    with mock_baris_response(ALL_DAYS_DEPARTURE_TABLO_BARIS_RESPONSE):
        flights = scheduler.get_schedule()
        flight_json = scheduler.baris_schedule_route2json(flights[0])

        assert_that(flight_json, has_entries({
            "thread": has_entries({
                "number": "SU 1",
                "uid": "SU-1_201221_c301_12"
            }),
            "departure": "01:30",
            "arrival": None,
            "days": "22, 23\xa0декабря"
        }))


@replace_now('2020-12-22')
def test_all_days_arrival_baris_schedule_route2json():
    scheduler = _get_scheduler(event="arrival", result_pytz=pytz.timezone("Etc/GMT-1"))

    with mock_baris_response(ALL_DAYS_ARRIVAL_TABLO_BARIS_RESPONSE):
        flights = scheduler.get_schedule()
        flight_json = scheduler.baris_schedule_route2json(flights[0])

        assert_that(flight_json, has_entries({
            "thread": has_entries({
                "number": "SU 1",
                "uid": "SU-1_201221_c301_12"
            }),
            "arrival": "23:30",
            "departure": None,
            "days": "21, 22\xa0декабря"
        }))
