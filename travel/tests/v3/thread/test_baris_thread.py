# coding: utf8
from __future__ import absolute_import, division, print_function, unicode_literals

import json

import httpretty
from django.conf import settings
from hamcrest import has_entries, assert_that, contains

import common.data_api.baris.instance  # noqa # для загрузки settings.BARIS_API_URL
from common.data_api.baris.test_helpers import mock_baris_response
from common.tester.factories import create_station, create_transport_model, create_company, create_settlement
from common.tester.utils.datetime import replace_now

from travel.rasp.api_public.tests.v3 import ApiTestCase


FLIGHT_BARIS_RESPONSE = {
    "title": "SU 1",
    "airlineID": 301,
    "schedules": [
        {
            "transportModelID": 202,
            "route": [
                {
                    "airportID": 101,
                    "departureTime": "00:00:00",
                },
                {
                    "airportID": 102,
                    "arrivalTime": "02:00:00",
                }
            ],
            "masks": [
                {
                    "from": "2020-11-26",
                    "until": "2020-11-26",
                    "on": 4
                }
            ]
        },
        {
            "transportModelID": 201,
            "route": [
                {
                    "airportID": 101,
                    "departureTime": "21:10:00",
                    "departureTerminal": "A"
                },
                {
                    "airportID": 102,
                    "arrivalTime": "01:20:00",
                    "arrivalDayShift": 1,
                    "arrivalTerminal": "1",
                    "departureTime": "11:20:00",
                    "departureDayShift": 1,
                    "departureTerminal": ""
                },
                {
                    "airportID": 103,
                    "arrivalTime": "01:30:00",
                    "arrivalDayShift": 2,
                    "arrivalTerminal": "",
                }
            ],
            "masks": [
                {
                    "from": "2020-11-27",
                    "until": "2020-12-04",
                    "on": 5
                }
            ]
        }
    ]
}


class TestBarisThread(ApiTestCase):
    def _fill_db(self):
        settlement1 = create_settlement(id=1001, title="City1", time_zone="Etc/GMT-1")
        settlement2 = create_settlement(id=1002, title="City2", time_zone="Etc/GMT-2")
        settlement3 = create_settlement(id=1003, title="City3", time_zone="Etc/GMT-3")
        create_station(
            id=101, title="Station1", title_uk="Station1_uk", time_zone="Etc/GMT-1",
            settlement=settlement1, __={"codes": {"esr": "esr1"}}
        )
        create_station(
            id=102, title="Station2", title_uk="Station2_uk", time_zone="Etc/GMT-2",
            settlement=settlement2, __={"codes": {"esr": "esr2"}}
        )
        create_station(
            id=103, title="Station3", title_uk="Station3_uk", time_zone="Etc/GMT-3",
            settlement=settlement3, __={"codes": {"esr": "esr3"}}
        )
        create_transport_model(id=201, title="Model1")
        create_transport_model(id=202, title="Model2")
        create_company(id=301, title="Company1")

    @httpretty.activate
    @replace_now("2020-11-26")
    def test_flight_bad_response(self):
        httpretty.register_uri(
            httpretty.GET, "{}api/v1/flight-schedule/SU/11111".format(settings.BARIS_API_URL), status=404
        )
        response = self.api_get("thread", {"uid": "SU-11111_20201204_c26_547"})
        assert response.status_code == 404
        assert json.loads(response.content)["error"]["text"] == "Рейса с uid SU-11111_20201204_c26_547 нет в базе"

        self._fill_db()
        with mock_baris_response(FLIGHT_BARIS_RESPONSE):
            response = self.api_get("thread", {
                "uid": "SU-1_20201204_c26_547", "date": "2020-11-28"
            })
            assert response.status_code == 404
            error_text = json.loads(response.content)["error"]["text"]
            assert error_text == "Рейс с uid SU-1_20201204_c26_547 не ходит 2020-11-28"

            response = self.api_get("thread", {
                "uid": "SU-1_20201126_c26_547", "from": "s101", "to": "s103"
            })
            assert response.status_code == 400
            error_text = json.loads(response.content)["error"]["text"]
            assert error_text == "Нитка не проходит через указанный пункт прибытия"

            response = self.api_get("thread", {
                "uid": "SU-1_20201126_c26_547", "from": "s103", "to": "s102"
            })
            assert response.status_code == 400
            error_text = json.loads(response.content)["error"]["text"]
            assert error_text == "Нитка не проходит через указанный пункт отправления"

    @replace_now("2020-11-26")
    def test_flight(self):
        self._fill_db()
        with mock_baris_response(FLIGHT_BARIS_RESPONSE):
            thread_json = self.api_get_json("thread", {
                "uid": "SU-1_20201204_c26_547", "show_systems": "all"
            })

            assert_that(thread_json, has_entries({
                "uid": "SU-1_20201204_c26_547",
                "number": "SU 1",
                "express_type": None,
                "transport_type": "plane",
                "transport_subtype": has_entries({"color": None, "code": None, "title": None}),

                "start_time": "21:10",
                "start_date": "2020-11-27",
                "days": "27 ноября, 4 декабря",
                "except_days": "",
                "title": "City1 \u2014 City3",
                "short_title": "City1 \u2014 City3",

                "carrier": has_entries({"code": 301, "title": "Company1"}),
                "vehicle": "Model1",

                "from": None,
                "to": None,
                "departure_date": None,
                "arrival_date": None,

                "stops": contains(
                    has_entries({
                        "arrival": None,
                        "departure": "2020-11-27 21:10:00",
                        "duration": 0,
                        "stop_time": None,
                        "platform": "",
                        "terminal": "A",
                        "station": has_entries({
                            "code": "s101",
                            "title": "Station1",
                            "codes": has_entries({
                                "yandex": "s101",
                                "esr": "esr1"
                            })
                        })
                    }),
                    has_entries({
                        "arrival": "2020-11-28 01:20:00",
                        "departure": "2020-11-28 11:20:00",
                        "duration": 47400,
                        "stop_time": 36000,
                        "platform": "",
                        "terminal": "1",
                        "station": has_entries({
                            "code": "s102",
                            "title": "Station2",
                            "codes": has_entries({
                                "yandex": "s102",
                                "esr": "esr2"
                            })
                        })
                    }),
                    has_entries({
                        "arrival": "2020-11-29 01:30:00",
                        "departure": None,
                        "duration": 94800,
                        "stop_time": None,
                        "platform": "",
                        "terminal": None,
                        "station": has_entries({
                            "code": "s103",
                            "title": "Station3",
                            "codes": has_entries({
                                "yandex": "s103",
                                "esr": "esr3"
                            })
                        })
                    })
                )
            }))

    @replace_now("2020-11-26")
    def test_flight_with_stops(self):
        self._fill_db()
        with mock_baris_response(FLIGHT_BARIS_RESPONSE):
            thread_json = self.api_get_json("thread", {
                "uid": "SU-1_20201204_c26_547", "from": "s102", "to": "s103", "show_systems": "all"
            })

            assert_that(thread_json, has_entries({
                "start_time": "21:10",
                "start_date": "2020-11-27",
                "days": "27 ноября, 4 декабря",
                "title": "City1 \u2014 City3",

                "from": has_entries({
                    "code": "s102",
                    "title": "Station2",
                    "codes": has_entries({
                        "yandex": "s102",
                        "esr": "esr2"
                    })
                }),
                "to": has_entries({
                    "code": "s103",
                    "title": "Station3",
                    "codes": has_entries({
                        "yandex": "s103",
                        "esr": "esr3"
                    })
                }),
                "departure_date": "2020-11-28",
                "arrival_date": "2020-11-29",
            }))

            thread_json = self.api_get_json("thread", {
                "uid": "SU-1_20201204_c26_547", "from": "c1001", "to": "c1002", "show_systems": "all"
            })

            assert_that(thread_json, has_entries({
                "start_time": "21:10",
                "start_date": "2020-11-27",
                "days": "27 ноября, 4 декабря",
                "title": "City1 \u2014 City3",

                "from": has_entries({
                    "code": "c1001",
                    "title": "City1",
                    "codes": has_entries({
                        "yandex": "c1001"
                    })
                }),
                "to": has_entries({
                    "code": "c1002",
                    "title": "City2",
                    "codes": has_entries({
                        "yandex": "c1002"
                    })
                }),
                "departure_date": "2020-11-27",
                "arrival_date": "2020-11-28",
            }))

    @replace_now("2020-11-26")
    def test_flight_on_date(self):
        self._fill_db()
        with mock_baris_response(FLIGHT_BARIS_RESPONSE):
            thread_json = self.api_get_json("thread", {
                "uid": "SU-1_20201204_c26_547", "date": "2020-12-04"
            })

        assert_that(thread_json, has_entries({
            "start_time": "21:10",
            "start_date": "2020-12-04",
            "days": "27 ноября, 4 декабря",
            "title": "City1 \u2014 City3",
        }))

        with mock_baris_response(FLIGHT_BARIS_RESPONSE):
            thread_json = self.api_get_json("thread", {
                "uid": "SU-1_20201126_c26_547", "date": "2020-11-26"
            })

        assert_that(thread_json, has_entries({
            "start_time": "00:00",
            "start_date": "2020-11-26",
            "days": "26 ноября",
            "title": "City1 \u2014 City2",
        }))

        with mock_baris_response(FLIGHT_BARIS_RESPONSE):
            thread_json = self.api_get_json("thread", {
                "uid": "SU-1_20201126_c26_547", "date": "2020-11-26T00:00:00+01:00"
            })

        assert_that(thread_json, has_entries({
            "start_time": "00:00",
            "start_date": "2020-11-26",
            "days": "26 ноября",
            "title": "City1 \u2014 City2",
        }))

        with mock_baris_response(FLIGHT_BARIS_RESPONSE):
            thread_json = self.api_get_json("thread", {
                "uid": "SU-1_20201126_c26_547", "date": "2020-11-25T23:00:00+00:00"
            })

        assert_that(thread_json, has_entries({
            "start_time": "00:00",
            "start_date": "2020-11-26",
            "days": "26 ноября",
            "title": "City1 \u2014 City2",
        }))

    @replace_now("2020-11-26")
    def test_flight_in_language(self):
        self._fill_db()
        with mock_baris_response(FLIGHT_BARIS_RESPONSE):
            thread_json = self.api_get_json("thread", {
                "uid": "SU-1_20201126_c26_547", "lang": "uk_UA"
            })

            assert_that(thread_json, has_entries({
                "days": "26 листопада",
                "stops": contains(
                    has_entries({
                        "station": has_entries({"title": "Station1_uk"})
                    }),
                    has_entries({
                        "station": has_entries({"title": "Station2_uk"})
                    })
                )
            }))
