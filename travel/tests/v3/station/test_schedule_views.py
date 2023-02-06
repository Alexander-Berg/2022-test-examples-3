# coding: utf8
from __future__ import absolute_import, division, print_function, unicode_literals

from datetime import datetime, timedelta, time

from hamcrest import assert_that, has_entries, contains

from common.tester.utils.datetime import replace_now
from common.tester.factories import (
    create_station, create_train_schedule_plan, create_thread, create_company, create_transport_model
)
from common.data_api.baris.test_helpers import mock_baris_response
from common.models.transport import TransportType

from travel.rasp.api_public.tests.v3 import ApiTestCase
from travel.rasp.api_public.tests.v3.helpers import check_response_invalid_date


class TestRaspDbScheduleView(ApiTestCase):
    def test_valid(self):
        station = create_station()
        station_code = "s{}".format(station.id)

        query = {"station": station_code}
        response = self.api_get("schedule", query)
        assert response.status_code == 200

    def test_train_schedule_plan(self):
        station = create_station()
        station_code = "s{}".format(station.id)

        plan = create_train_schedule_plan(start_date=datetime.now() + timedelta(days=2))
        current_plan, next_plan = plan.get_current_and_next(datetime.now().date())
        create_thread(
            schedule_plan=plan, translated_days_texts=u'[{}, {"ru": "ежедневно"}]',
            __={"calculate_noderoute": True},
            schedule_v1=[[None, 0, station], [10, None]]
        )

        query = {"station": station_code}
        response = self.api_get_json("schedule", query)
        assert response["schedule"][0]["days"] == u"ежедневно " + next_plan.L_appendix()

    @replace_now("2001-01-01 00:00:00")
    def test_date_range(self):
        date = "1920-01-01"
        schedule_json = self.api_get("schedule", {"date": date})
        check_response_invalid_date(schedule_json, date)

        date = "2010-01-01"
        schedule_json = self.api_get("schedule", {"date": date})
        check_response_invalid_date(schedule_json, date)

    @replace_now("2005-10-5 00:00:00")
    def test_result_timezone(self):
        departure_date = datetime(2005, 10, 6)
        station_1 = create_station(time_zone="Europe/Moscow")
        station_code = "s{}".format(station_1.id)

        create_thread(
            __={"calculate_noderoute": True},
            uid="12345",
            time_zone="Europe/Moscow",
            year_days=[departure_date.date()],
            tz_start_time=time(22),
            schedule_v1=[
                [None, 0, station_1],
                [10, None, create_station()],
            ],
        )
        schedule = self.api_get_json("schedule", {"station": station_code})["schedule"][0]
        assert schedule["departure"] == "22:00"
        assert schedule["days"] == u"только 6 октября"

        schedule = self.api_get_json(
            "schedule",
            {"station": station_code, "result_timezone": "Asia/Krasnoyarsk"}
        )["schedule"][0]

        assert schedule["departure"] == "02:00"
        assert schedule["days"] == u"только 7 октября"

    @replace_now("2016-10-5 00:00:00")
    def test_iso_date(self):
        departure_date = datetime(2016, 10, 6)
        station_1 = create_station(time_zone="Europe/Moscow")
        station_code = "s{}".format(station_1.id)

        create_thread(
            __={"calculate_noderoute": True},
            uid="12345",
            time_zone="Europe/Moscow",
            year_days=[departure_date.date()],
            tz_start_time=time(22),
            schedule_v1=[
                [None, 0, station_1],
                [10, None, create_station()],
            ],
        )
        schedule = self.api_get_json(
            "schedule",
            {"station": station_code, "date": "2016-10-07T01:50:10+05:00"}
        )["schedule"][0]

        assert schedule["departure"] == "2016-10-06T22:00:00+03:00"
        assert schedule["days"] == u"только 6 октября"

        assert len(self.api_get_json(
            "schedule",
            {"station": station_code, "date": "2016-10-07T02:50:10+05:00"}
        )["schedule"]) == 0


# Универсальный ответ для запросов на дель и на все дни
UNIVERSAL_TABLO_BARIS_RESPONSE = {
    "direction": "departure",
    "station": 101,
    "flights": [
        {
            "airlineID": 301,
            "transportModelID": 201,
            "title": "SU 1",

            "datetime": "2020-12-22T01:30:00+03:00",
            "startDatetime": "2020-12-22T01:30:00+03:00",
            "terminal": "A",
            "route": [101, 102],
            "status": {
                "departure": "2020-12-22 02:30:00",
                "status": "unknown",
                "departureTerminal": "B",
            },

            "schedules": [
                {
                    "time": "01:30",
                    "startTime": "01:30",
                    "startDayShift": 0,
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


class TestBarisScheduleView(ApiTestCase):
    def _create_map(self):
        create_station(id=101, t_type=TransportType.PLANE_ID, type_choices="tablo", tablo_state="real")
        create_station(id=102, t_type=TransportType.PLANE_ID, type_choices="tablo", tablo_state="real")
        create_company(id=301)
        create_transport_model(id=201)

    @replace_now("2020-12-22")
    def test_one_day_airport_view(self):
        self._create_map()
        with mock_baris_response(UNIVERSAL_TABLO_BARIS_RESPONSE):
            response_json = self.api_get_json("schedule", {"station": "s101", "date": "2020-12-22", "tablo": "true"})

            assert_that(response_json, has_entries({
                "date": "2020-12-22",
                "station": has_entries({
                    "code": "s101",
                    "transport_type": "plane"
                }),
                "pagination": has_entries({"total": 1, "limit": 100, "offset": 0}),
                "interval_schedule": has_entries({}),
                "schedule": contains(has_entries({
                    "thread": has_entries({
                        "number": "SU 1",
                        "carrier": has_entries({"code": 301}),
                        "transport_type": "plane",
                    }),
                    "terminal": "A",
                    "departure": "2020-12-22T01:30:00+03:00",
                    "arrival": None,
                    "days": "22, 23\xa0декабря",
                    "tablo": has_entries({
                        "departure": "2020-12-22T01:30:00",
                        "real_departure": "2020-12-22T02:30:00",
                        "terminal": "B"
                    })
                }))
            }))

    @replace_now("2020-12-22")
    def test_all_days_airport_view(self):
        self._create_map()
        with mock_baris_response(UNIVERSAL_TABLO_BARIS_RESPONSE):
            response_json = self.api_get_json("schedule", {"station": "s101"})

            assert_that(response_json, has_entries({
                "date": None,
                "station": has_entries({
                    "code": "s101",
                    "transport_type": "plane"
                }),
                "pagination": has_entries({"total": 1, "limit": 100, "offset": 0}),
                "schedule": contains(has_entries({
                    "thread": has_entries({
                        "number": "SU 1",
                        "carrier": has_entries({"code": 301}),
                        "transport_type": "plane",
                    }),
                    "terminal": "A",
                    "departure": "01:30",
                    "arrival": None,
                    "days": "22, 23\xa0декабря"
                }))
            }))
