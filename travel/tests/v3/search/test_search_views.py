# coding: utf8
from __future__ import absolute_import, division, print_function, unicode_literals

from datetime import datetime, time, date
from copy import copy

import mock
import pytest
from django.utils.six.moves.urllib.parse import urlparse, parse_qs
from hamcrest import assert_that, has_entries, contains

from common.data_api.ticket_daemon.factories import create_segment as create_var_segment, create_variant
from common.models.currency import Price
from common.models.geo import Settlement
from common.models.schedule import RThreadType
from common.models.transport import TransportType
from common.tester.factories import (
    create_station, create_thread, create_settlement, create_partner, create_company, create_transport_model
)
from common.tester.utils.datetime import replace_now
from common.data_api.baris.test_helpers import mock_baris_response
from route_search.transfers.transfers import parse_pathfinder_response

from travel.rasp.api_public.tests.v3 import ApiTestCase
from travel.rasp.api_public.tests.v3.helpers import check_response_invalid_date
from travel.rasp.api_public.tests.v3.search.helpers import get_stub_currency_info


create_thread = create_thread.mutate(__={"calculate_noderoute": True})


ONE_VARIANT_XML = """<?xml version="1.0" encoding="utf-8" ?>
<routes>
    <group>
        <variant >
            <route
                start_date="2016-10-10" thread_id="42"
                departure_datetime="2016-10-10 20:52" departure_station_id="42"
                arrival_datetime="2016-10-12 22:38" arrival_station_id="43"
            />
            <route
                start_date="2016-10-10" thread_id="NULL"
                departure_datetime="2016-10-12 22:38" departure_station_id="43"
                arrival_datetime="2016-10-12 22:49" arrival_station_id="44"
            />
            <route
                start_date="2016-10-10" thread_id="44"
                departure_datetime="2016-10-12 22:49" departure_station_id="44"
                arrival_datetime="2016-10-12 23:49" arrival_station_id="45"
            />
        </variant>
    </group>
</routes>"""


ONE_DAY_P2P_BARIS_RESPONSE = {
    "departureStations": [101],
    "arrivalStations": [102],
    "flights": [
        {
            "airlineID": 301,
            "title": "SU 1",
            "departureDatetime": "2020-12-12T01:00:00+03:00",
            "departureStation": 101,
            "departureTerminal": "A",
            "arrivalDatetime": "2020-12-12T03:00:00+03:00",
            "arrivalStation": 102,
            "arrivalTerminal": "B",
            "route": [101, 102],
            "transportModelID": 201,
            "startDatetime": "2020-12-11T23:00:00+03:00",
        }
    ]
}


class TestSearch(ApiTestCase):
    def setUp(self):
        super(TestSearch, self).setUp()

        self.station_from = create_station(settlement=create_settlement(id=100500, title="Гондор"))
        self.station_to = create_station(settlement=Settlement.get_default_city())

        self.create_thread = create_thread.mutate(
            __={"calculate_noderoute": True},
            schedule_v1=[
                [None, 0, self.station_from],
                [10, None, self.station_to],
            ],
        )

        self.query_params = {
            "format": "json",
            "lang": "ru_RU",
            "from": "c100500",
            "to": "c213",
            "page": "1",
        }

    def test_limit_offset(self):
        for i in range(10):
            self.create_thread(uid="uid_{}".format(i))

        result = self.api_get_json("search", self.query_params)
        segments, pagination = result["segments"], result["pagination"]
        assert len(segments) == 10
        assert pagination == {"offset": 0,
                              "limit": 100,
                              "total": 10}

        query_params = copy(self.query_params)
        query_params.update({"offset": 5, "limit": 2})
        result = self.api_get_json("search", query_params)
        segments, pagination = result["segments"], result["pagination"]
        assert len(segments) == 2
        assert pagination == {"offset": 5,
                              "limit": 2,
                              "total": 10}

        query_params.update({"offset": 10, "limit": 50})
        result = self.api_get_json("search", query_params)
        segments, pagination = result["segments"], result["pagination"]
        assert len(segments) == 0
        assert pagination == {"offset": 10,
                              "limit": 50,
                              "total": 10}

    def test_valid(self):
        self.create_thread(company={"title": u"Company1"})

        result = self.api_get_json("search", self.query_params)
        assert len(result["segments"]) == 1
        segment = result["segments"][0]
        assert segment["thread"]["carrier"]["title"] == u"Company1"
        assert not segment["thread"]["carrier"]["logo_svg"]

    @replace_now("2016-01-10 00:00:00")
    def test_thread_method_link(self):
        departure_date = datetime(2016, 1, 10)

        create_thread(
            uid="12345",
            year_days=[departure_date.date()],
            tz_start_time=time(22),
            schedule_v1=[
                [None, 0, create_station(id=666, time_zone="Asia/Barnaul")],
                [10, None, self.station_to],
            ],
        )

        query_params = copy(self.query_params)
        query_params["from"] = "s666"
        result = self.api_get_json("search", query_params)
        segment = result["segments"][0]
        assert segment["start_date"] == "2016-01-11"
        assert parse_qs(urlparse(segment["thread"]["thread_method_link"]).query) == {
            "date": ["2016-01-11"], "uid": ["12345"]
        }

    def test_logo_included(self):
        self.create_thread(company={"title": u"Company1", "svg_logo": u"http://somelogo.ru"})

        result = self.api_get_json("search", self.query_params)
        segment = result["segments"][0]
        assert segment["thread"]["carrier"]["logo_svg"]

    @replace_now("2001-01-01 00:00:00")
    def test_date_range(self):
        with mock.patch(
            "travel.rasp.api_public.api_public.v3.core.helpers.get_currency_info",
            return_value=get_stub_currency_info()
        ):
            date = "1920-01-01"
            search_json = self.api_get("search", {"date": date, "from": "from", "to": "to"})
            check_response_invalid_date(search_json, date)

            date = "1920-01-01"
            search_json = self.api_get("search", {"date": date, "from": "from", "to": "to"})
            check_response_invalid_date(search_json, date)

    @replace_now("2001-01-01 00:00:00")
    def test_days_mask(self):
        self.create_thread()
        result = self.api_get_json("search", self.query_params)
        assert result["segments"][0].get("schedule") is None
        params = copy(self.query_params)
        params.update(add_days_mask="true")
        result = self.api_get_json("search", params)
        assert len(result["segments"][0]["schedule"]) == 12

    @pytest.mark.skip("https://st.yandex-team.ru/SUBURBAN-102")
    @replace_now("2005-10-5 00:00:00")
    @mock.patch("common.data_api.ticket_daemon.query.Query")
    def test_tariff_in_view(self, m_query):
        spb = create_settlement(id=Settlement.SPB_ID)
        station_to = create_station(settlement=spb)
        station_from = create_station(settlement=Settlement.get_default_city())

        query_params = {
            "format": "json",
            "lang": "ru_RU",
            "from": "c213",
            "to": "c2",
            "page": "1",
            "date": "2005-10-6"
        }
        create_thread(
            uid="12345",
            t_type="plane",
            number="num 1",
            year_days=[date(2005, 10, 6)],
            tz_start_time=time(10),
            schedule_v1=[
                [None, 0, station_from],
                [60, None, station_to],
            ],
        )
        _statuses = {"ozone": "done"}
        variant = create_variant(
            tariff=Price(2145), order_data={},
            segments=[create_var_segment(departure=datetime(2005, 10, 6, 10),
                                         arrival=datetime(2005, 10, 6, 11),
                                         number="num 1",
                                         t_type=TransportType.objects.get(id=TransportType.PLANE_ID))]
        )
        variants = {"ozone": [variant]}

        m_query.return_value.collect_variants = mock.Mock(return_value=(variants, _statuses))
        create_partner(enabled_in_rasp_ru=True, code="ozone", t_type="plane")

        with mock.patch(
            "travel.rasp.api_public.api_public.v3.core.helpers.get_currency_info",
            return_value=get_stub_currency_info()
        ):
            result = self.api_get_json("search", query_params)
            assert result["segments"][0]["tickets_info"]["places"][0]["price"] == {"whole": 2145, "cents": 0}

    @replace_now("2005-10-5 00:00:00")
    def test_result_timezone(self):
        departure_date = datetime(2005, 10, 6)

        create_thread(
            uid="12345",
            year_days=[departure_date.date()],
            tz_start_time=time(22),
            schedule_v1=[
                [None, 0, create_station(id=123, time_zone="Europe/Moscow"), {"time_zone": "Europe/Moscow"}],
                [10, None, self.station_to],
            ],
        )
        query_params = copy(self.query_params)
        query_params["from"] = "s123"

        result = self.api_get_json("search", query_params)
        segment = result["segments"][0]
        assert segment["start_date"] == "2005-10-06"
        assert segment["departure"] == "22:00:00"
        assert segment["arrival"] == "22:10:00"
        assert segment["days"] == u"только 6 октября"

        query_params["result_timezone"] = "Asia/Krasnoyarsk"
        result = self.api_get_json("search", query_params)
        segment = result["segments"][0]
        assert segment["start_date"] == "2005-10-07"
        assert segment["departure"] == "02:00:00"
        assert segment["arrival"] == "02:10:00"
        assert segment["days"] == u"только 7 октября"

    @replace_now("2016-10-09 00:00:00")
    def test_transfers(self):
        s1 = create_station(id=42, settlement=create_settlement(id=54))
        s2 = create_station(id=43, settlement=Settlement.objects.get(id=213))
        s3 = create_station(id=44)
        s4 = create_station(id=45)

        create_thread(
            uid="42",
            year_days=[date(2016, 10, 10)],
            tz_start_time=time(22),
            schedule_v1=[[None, 0, s1], [10, None, s2]],
        )
        create_thread(uid="44", schedule_v1=[[None, 0, s3], [30, None, s4]])

        query_params = {
            "lang": "ru_RU",
            "from": "c54",
            "to": "c213",
            "date": "2016-10-09",
            "transfers": "true"
        }

        # создаем сегмент с пересадкой
        groups = parse_pathfinder_response(ONE_VARIANT_XML, False, None, None)

        with mock.patch(
            "travel.rasp.api_public.api_public.v3.search.transfers_search.get_transfer_variants",
            return_value=groups[0].variants
        ):
            with mock.patch(
                "travel.rasp.api_public.api_public.v3.core.helpers.get_currency_info",
                return_value=get_stub_currency_info()
            ):
                result = self.api_get_json("search", query_params)
                segment = result["segments"][0]
                assert segment["has_transfers"] is True
                assert segment["details"][0]["from"]["code"] == "c54"
                assert segment["details"][0]["to"]["code"] == "c213"

    @replace_now("2018-8-16 12:00:00")
    def test_interval_segment(self):

        departure_date = datetime(2018, 8, 16)

        thread = create_thread(
            uid="12345",
            type=RThreadType.INTERVAL_ID,
            t_type=TransportType.BUS_ID,
            year_days=[departure_date.date()],
            begin_time=time(7),
            end_time=time(23),
            schedule_v1=[
                [None, 0, create_station(id=123)],
                [50, None, self.station_to],
            ],
        )

        with mock.patch(
            "travel.rasp.api_public.api_public.v3.core.helpers.get_currency_info",
            return_value=get_stub_currency_info()
        ):
            query_params = copy(self.query_params)
            query_params["from"] = "s123"
            query_params["date"] = "2018-8-16T12:00:00+03:00"
            result = self.api_get_json("search", query_params)
            segment = result["interval_segments"][0]
            assert_that(segment, has_entries({
                "thread": has_entries({
                    "uid": thread.uid,
                    "interval": has_entries({
                        "begin_time": "2018-08-16T07:00:00",
                        "end_time": "2018-08-16T23:00:00"
                    })
                })
            }))

    @replace_now("2020-12-12 00:00:00")
    def test_baris_segment(self):
        create_station(id=101, t_type=TransportType.PLANE_ID, type_choices="tablo")
        create_station(id=102, t_type=TransportType.PLANE_ID, type_choices="tablo")
        create_company(id=301)
        create_transport_model(id=201, title="model")

        with mock_baris_response(ONE_DAY_P2P_BARIS_RESPONSE):
            query_params = {
                "format": "json",
                "lang": "ru_RU",
                "from": "s101",
                "to": "s102",
                "date": "2020-12-12",
                "transport_types": "plane"
            }
            result = self.api_get_json("search", query_params)

            assert_that(result["search"], has_entries({
                "from": has_entries({"code": "s101"}),
                "to": has_entries({"code": "s102"}),
                "date": "2020-12-12"
            }))

            assert len(result["segments"]) == 1
            assert_that(result["segments"][0], has_entries({
                "thread": has_entries({
                    "number": "SU 1",
                    "carrier": has_entries({"code": 301}),
                    "vehicle": "model",
                    "transport_type": "plane",
                    "uid": "SU-1_201211_c301_12"
                }),
                "from": has_entries({"code": "s101"}),
                "to": has_entries({"code": "s102"}),
                "departure_terminal": "A",
                "arrival_terminal": "B",
                "start_date": "2020-12-11",
                "departure": "2020-12-12T01:00:00+03:00",
                "arrival": "2020-12-12T03:00:00+03:00",
                "has_transfers": False,
            }))


PATHFINDER_XML = """<?xml version="1.0" encoding="utf-8" ?>
<routes>
    <group>
        <variant >
            <route
                start_date="2020-12-12" thread_id="41"
                departure_datetime="2020-12-12 01:00" departure_station_id="111"
                arrival_datetime="2020-12-12 02:00" arrival_station_id="101"
            />
            <route
                start_date="2020-12-12" thread_id="NULL"
                departure_datetime="2020-12-12 02:00" departure_station_id="101"
                arrival_datetime="2020-12-12 03:00" arrival_station_id="102"
            />
            <route
                start_date="2020-12-12" thread_id="42"
                departure_datetime="2020-12-12 03:00" departure_station_id="102"
                arrival_datetime="2020-12-12 04:00" arrival_station_id="121"
            />
        </variant>
    </group>
</routes>"""


ONE_DAY_BARIS_RESPONSE = {
    "departureStations": [112],
    "arrivalStations": [122],
    "flights": [
        {
            "airlineID": 301,
            "title": "SU 1",
            "departureDatetime": "2020-12-12T07:00:00+03:00",
            "departureStation": 112,
            "departureTerminal": "A",
            "arrivalDatetime": "2020-12-12T08:00:00+03:00",
            "arrivalStation": 122,
            "arrivalTerminal": "B",
            "route": [112, 122],
            "transportModelID": 201,
            "startDatetime": "2020-12-12T07:00:00+03:00",
        }
    ]
}


ALL_DAYS_BARIS_RESPONSE = {
    "departureStations": [112],
    "arrivalStations": [122],
    "flights": [
        {
            "airlineID": 301,
            "title": "SU 1",
            "departureTime": "07:00",
            "departureStation": 112,
            "arrivalTime": "08:00",
            "arrivalDayShift": 0,
            "arrivalStation": 122,
            "startTime": "07:00",
            "startDayShift": 0,
            "route": [112, 122],
            "masks": [
                {
                    "from": "2020-12-12",
                    "until": "2020-12-12",
                    "on": 6
                }
            ]
        }
    ]
}


class TestAllTypesSearch(ApiTestCase):
    def _set_up(self):
        city1 = create_settlement(id=10)
        city2 = create_settlement(id=20)
        create_station(
            id=111, settlement=city1, time_zone="Europe/Moscow",
            t_type=TransportType.BUS_ID, type_choices="schedule"
        )
        create_station(
            id=112, settlement=city1, time_zone="Europe/Moscow",
            t_type=TransportType.PLANE_ID, type_choices="tablo"
        )
        create_station(
            id=121, settlement=city2, time_zone="Europe/Moscow",
            t_type=TransportType.BUS_ID, type_choices="schedule"
        )
        create_station(
            id=122, settlement=city2, time_zone="Europe/Moscow",
            t_type=TransportType.PLANE_ID, type_choices="tablo"
        )
        create_station(
            id=101, time_zone="Europe/Moscow",
            t_type=TransportType.BUS_ID, type_choices="schedule"
        )
        create_station(
            id=102, time_zone="Europe/Moscow",
            t_type=TransportType.BUS_ID, type_choices="schedule"
        )
        create_company(id=301)

        create_thread(
            uid="41",
            t_type=TransportType.BUS_ID,
            year_days=[date(2020, 12, 12)],
            tz_start_time=time(1),
            schedule_v1=[[None, 0, 111], [60, None, 101]]
        )

        create_thread(
            uid="42",
            t_type=TransportType.BUS_ID,
            year_days=[date(2020, 12, 12)],
            tz_start_time=time(3),
            schedule_v1=[[None, 0, 102], [60, None, 121]]
        )

        create_thread(
            uid="43",
            t_type=TransportType.BUS_ID,
            year_days=[date(2020, 12, 12)],
            tz_start_time=time(5),
            schedule_v1=[[None, 0, 111], [60, None, 121]]
        )

        create_thread(
            uid="44",
            type=RThreadType.INTERVAL_ID,
            t_type=TransportType.BUS_ID,
            year_days=[date(2020, 12, 12)],
            begin_time=time(9),
            end_time=time(22),
            schedule_v1=[[None, 0, 111], [60, None, 121]],
        )

    @replace_now("2020-12-12")
    def test_one_day_search(self):
        self._set_up()

        with mock_baris_response(ONE_DAY_BARIS_RESPONSE):
            with mock.patch("route_search.transfers.transfers.get_pathfinder_response", return_value=PATHFINDER_XML):
                with mock.patch(
                    "travel.rasp.api_public.api_public.v3.core.helpers.get_currency_info",
                    return_value=get_stub_currency_info()
                ):
                    query_params = {
                        "format": "json",
                        "lang": "ru_RU",
                        "from": "c10",
                        "to": "c20",
                        "date": "2020-12-12",
                        "transfers": True,
                    }
                    result = self.api_get_json("search", query_params)

                    assert_that(result["search"], has_entries({
                        "from": has_entries({"code": "c10"}),
                        "to": has_entries({"code": "c20"}),
                        "date": "2020-12-12"
                    }))

                    assert_that(result["pagination"], has_entries({
                        "offset": 0, "limit": 100, "total": 4
                    }))

                    assert len(result["interval_segments"]) == 1
                    assert_that(result["interval_segments"][0], has_entries({
                        "thread": has_entries({
                            "uid": "44",
                            "transport_type": "bus",
                            "interval": has_entries({
                                "begin_time": "2020-12-12T09:00:00",
                                "end_time": "2020-12-12T22:00:00"
                            })
                        }),
                        "has_transfers": False,
                        "from": has_entries({"code": "s111"}),
                        "to": has_entries({"code": "s121"}),
                    }))

                    assert len(result["segments"]) == 3
                    assert_that(result["segments"][0], has_entries({
                        "thread": has_entries({
                            "uid": "43",
                            "transport_type": "bus",
                        }),
                        "has_transfers": False,
                        "from": has_entries({"code": "s111"}),
                        "to": has_entries({"code": "s121"}),
                        "departure": "2020-12-12T05:00:00+03:00",
                        "arrival": "2020-12-12T06:00:00+03:00",
                    }))

                    assert_that(result["segments"][1], has_entries({
                        "thread": has_entries({
                            "uid": "SU-1_201212_c301_12",
                            "transport_type": "plane",
                        }),
                        "has_transfers": False,
                        "from": has_entries({"code": "s112"}),
                        "to": has_entries({"code": "s122"}),
                        "departure": "2020-12-12T07:00:00+03:00",
                        "arrival": "2020-12-12T08:00:00+03:00",
                    }))

                    assert_that(result["segments"][2], has_entries({
                        "has_transfers": True,
                        "transport_types": contains("bus", "bus"),
                        "departure": "2020-12-12T01:00:00+03:00",
                        "arrival": "2020-12-12T04:00:00+03:00",
                        "details": contains(
                            has_entries({
                                "from": has_entries({"code": "c10"}),
                                "to": has_entries({"code": "s101"}),
                                "departure": "2020-12-12T01:00:00+03:00",
                                "arrival": "2020-12-12T02:00:00+03:00",
                                "start_date": "2020-12-12"
                            }),
                            has_entries({
                                "is_transfer": True,
                                "transfer_from": has_entries({"code": "s101"}),
                                "transfer_to": has_entries({"code": "s102"})
                            }),
                            has_entries({
                                "from": has_entries({"code": "s102"}),
                                "to": has_entries({"code": "c20"}),
                                "departure": "2020-12-12T03:00:00+03:00",
                                "arrival": "2020-12-12T04:00:00+03:00",
                                "start_date": "2020-12-12"
                            })
                        )
                    }))

    @replace_now("2020-12-12")
    def test_all_days_search(self):
        self._set_up()

        with mock_baris_response(ALL_DAYS_BARIS_RESPONSE):
            query_params = {
                "format": "json",
                "lang": "ru_RU",
                "from": "c10",
                "to": "c20",
            }
            result = self.api_get_json("search", query_params)

            assert_that(result["search"], has_entries({
                "from": has_entries({"code": "c10"}),
                "to": has_entries({"code": "c20"}),
                "date": None
            }))

            assert_that(result["pagination"], has_entries({
                "offset": 0, "limit": 100, "total": 3
            }))

            assert len(result["interval_segments"]) == 1
            assert_that(result["interval_segments"][0], has_entries({
                "thread": has_entries({
                    "uid": "44",
                    "transport_type": "bus",
                    "interval": has_entries({
                        "begin_time": "09:00",
                        "end_time": "22:00"
                    })
                }),
                "from": has_entries({"code": "s111"}),
                "to": has_entries({"code": "s121"}),
            }))

            assert len(result["segments"]) == 2
            assert_that(result["segments"][0], has_entries({
                "thread": has_entries({
                    "uid": "43",
                    "transport_type": "bus",
                }),
                "from": has_entries({"code": "s111"}),
                "to": has_entries({"code": "s121"}),
                "departure": "05:00:00",
                "arrival": "06:00:00",
            }))

            assert_that(result["segments"][1], has_entries({
                "thread": has_entries({
                    "uid": "SU-1_201212_c301_12",
                    "transport_type": "plane",
                }),
                "from": has_entries({"code": "s112"}),
                "to": has_entries({"code": "s122"}),
                "departure": "07:00:00",
                "arrival": "08:00:00",
            }))
