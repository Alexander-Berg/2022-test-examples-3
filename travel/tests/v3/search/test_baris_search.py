# coding: utf8
from __future__ import absolute_import, division, print_function, unicode_literals

import pytz
from datetime import date

import pytest
from hamcrest import assert_that, has_properties, contains_inanyorder, has_entries, contains

from common.tester.factories import create_station, create_settlement
from common.tester.utils.datetime import replace_now
from common.models.transport import TransportType
from common.data_api.baris.test_helpers import mock_baris_response

from travel.rasp.api_public.api_public.v3.search.baris_segments import OneDayBarisSegment, AllDaysBarisSegment
from travel.rasp.api_public.api_public.v3.search.baris_search import (
    BaseBarisSearch, OneDayBarisSearch, AllDaysBarisSearch
)
from travel.rasp.api_public.tests.v3.search.helpers import RequestStub, QueryPointsStub


pytestmark = [pytest.mark.dbuser]
create_station = create_station.mutate(t_type=TransportType.PLANE_ID, type_choices="tablo", time_zone="Etc/GMT-3")


ONE_DAY_P2P_BARIS_RESPONSE = {
    "departureStations": [101],
    "arrivalStations": [102],
    "flights": [
        {
            "airlineID": 301,
            "title": "SU 1",
            "departureDatetime": "2020-12-12T01:00:00+03:00",
            "departureStation": 101,
            "arrivalDatetime": "2020-12-12T03:00:00+03:00",
            "arrivalStation": 102,
            "startDatetime": "2020-12-12T01:00:00+03:00",
            "route": [101, 102],
        },
        {
            "airlineID": 301,
            "title": "SU 2",
            "departureDatetime": "2020-12-12T11:00:00+03:00",
            "departureStation": 101,
            "arrivalDatetime": "2020-12-12T13:00:00+03:00",
            "arrivalStation": 102,
            "startDatetime": "2020-12-12T11:00:00+03:00",
            "route": [101, 102],
        },
    ]
}


ALL_DAYS_P2P_BARIS_RESPONSE = {
    "departureStations": [101],
    "arrivalStations": [102],
    "flights": [
        {
            "airlineID": 301,
            "title": "SU 1",
            "departureTime": "01:00",
            "departureStation": 101,
            "arrivalTime": "03:00",
            "arrivalDayShift": 0,
            "arrivalStation": 102,
            "startTime": "01:00",
            "startDayShift": 0,
            "route": [101, 102],
            "masks": [
                {
                    "from": "2020-12-12",
                    "until": "2021-01-02",
                    "on": 6
                }
            ]
        },
        {
            "airlineID": 301,
            "title": "SU 2",
            "departureTime": "11:00",
            "departureStation": 101,
            "arrivalTime": "13:00",
            "arrivalDayShift": 0,
            "arrivalStation": 102,
            "startTime": "11:00",
            "startDayShift": 0,
            "route": [101, 102],
            "masks": [
                {
                    "from": "2020-12-12",
                    "until": "2020-12-12",
                    "on": 6
                }
            ]
        },
        {
            "airlineID": 301,
            "title": "SU 2",
            "departureTime": "12:00",
            "departureStation": 101,
            "arrivalTime": "14:00",
            "arrivalDayShift": 0,
            "arrivalStation": 102,
            "startTime": "12:00",
            "startDayShift": 0,
            "route": [101, 102],
            "masks": [
                {
                    "from": "2020-12-19",
                    "until": "2020-12-19",
                    "on": 6
                }
            ]
        }
    ]
}


def _create_cities():
    city1 = create_settlement(id=1001)
    city2 = create_settlement(id=1002)
    create_station(settlement=city1, id=101)
    create_station(settlement=city2, id=102)
    return city1, city2


def test_base_baris_search():
    city1, city2 = _create_cities()
    search = BaseBarisSearch(
        {"add_days_mask": False, "result_pytz": pytz.timezone("Europe/Moscow")},
        RequestStub(),
        QueryPointsStub(city1, city2)
    )

    assert search.add_days_mask is False
    assert search.result_pytz.zone == "Europe/Moscow"

    assert search.station_from_ids == [101]
    assert search.station_to_ids == [102]


@replace_now("2020-12-12")
def test_one_day_baris_search():
    city1, city2 = _create_cities()
    search = OneDayBarisSearch(
        {"add_days_mask": False, "result_pytz": None},
        RequestStub(),
        QueryPointsStub(city1, city2, date(2020, 12, 12))
    )
    with mock_baris_response(ONE_DAY_P2P_BARIS_RESPONSE) as m_get_baris_response:
        search.search()

        m_get_baris_response.assert_called_with(
            "flight-p2p",
            params={
                "from": [101], "to": [102], "national_version": "ru_RU",
                "after": "2020-12-12T00:00:00", "before": "2020-12-13T00:00:00",
            }
        )

        assert_that(list(search.used_points), contains_inanyorder(
            has_properties({"id": 101}),
            has_properties({"id": 102}),
            has_properties({"id": 1001}),
            has_properties({"id": 1002}),
        ))

        assert len(search.segments) == 2
        assert isinstance(search.segments[0], OneDayBarisSegment) is True
        assert isinstance(search.segments[1], OneDayBarisSegment) is True
        assert len(search.interval_segments) == 0


@replace_now("2020-12-12")
def test_add_flights_run_days():
    city1, city2 = _create_cities()
    search = OneDayBarisSearch(
        {"add_days_mask": False, "result_pytz": None},
        RequestStub(),
        QueryPointsStub(city1, city2, date(2020, 12, 12))
    )
    with mock_baris_response(ONE_DAY_P2P_BARIS_RESPONSE):
        search.search()

    with mock_baris_response(ALL_DAYS_P2P_BARIS_RESPONSE) as m_get_baris_response:
        search.add_days_mask = True
        search._add_flights_run_days(search.segments)

        m_get_baris_response.assert_called_with(
            "flight-p2p-schedule",
            params={"from": [101], "to": [102], "national_version": "ru_RU"}
        )

        assert len(search.segments) == 2
        assert_that(search.segments[0].run_days, has_entries({
            "2020": has_entries({
                "12": contains(
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0,
                ),
            }),
            "2021": has_entries({
                "1": contains(
                    0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                )
            })
        }))
        assert_that(search.segments[1].run_days, has_entries({
            "2020": has_entries({
                "12": contains(
                    0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0,
                ),
            })
        }))


@replace_now("2020-12-12")
def test_all_days_baris_search():
    city1, city2 = _create_cities()
    search = AllDaysBarisSearch(
        {"add_days_mask": False, "result_pytz": None},
        RequestStub(),
        QueryPointsStub(city1, city2, date(2020, 12, 12))
    )
    with mock_baris_response(ALL_DAYS_P2P_BARIS_RESPONSE) as m_get_baris_response:
        search.search()

        m_get_baris_response.assert_called_with(
            "flight-p2p-schedule",
            params={"from": [101], "to": [102], "national_version": "ru_RU"}
        )

        assert_that(search.used_points, contains_inanyorder(
            has_properties({"id": 101}),
            has_properties({"id": 102}),
            has_properties({"id": 1001}),
            has_properties({"id": 1002}),
        ))

        assert len(search.segments) == 3
        assert isinstance(search.segments[0], AllDaysBarisSegment) is True
        assert isinstance(search.segments[1], AllDaysBarisSegment) is True
        assert isinstance(search.segments[2], AllDaysBarisSegment) is True
        assert len(search.interval_segments) == 0
