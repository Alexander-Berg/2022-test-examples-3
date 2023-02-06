# coding: utf8
from __future__ import absolute_import, division, print_function, unicode_literals

from datetime import date, time

import mock
import pytest
from hamcrest import assert_that, has_properties, contains_inanyorder, has_entries, contains

from common.tester.factories import (
    create_settlement, create_station, create_thread, create_company, create_transport_model
)
from common.models.transport import TransportType
from common.tester.utils.datetime import replace_now
from common.data_api.baris.test_helpers import mock_baris_response
from route_search.transfers.transfer_segment import RaspDBTransferSegment, BarisTransferSegment
from route_search.transfers.variant import Variant

from travel.rasp.api_public.api_public.v3.search.transfers_search import TransfersSearch, transfer_variant2json
from travel.rasp.api_public.tests.v3.search.helpers import RequestStub, QueryPointsStub
from travel.rasp.api_public.api_public.v3.search.helpers import fill_thread_local_start_dt
from travel.rasp.api_public.api_public.v3.core.helpers import get_code_getter


pytestmark = [pytest.mark.dbuser]


PATHFINDER_XML = """<?xml version="1.0" encoding="utf-8" ?>
<routes>
    <group>
        <variant >
            <route
                start_date="2020-12-12" thread_id="SU-1_201212_c301_12"
                departure_datetime="2020-12-12 01:00" departure_station_id="101"
                arrival_datetime="2020-12-12 02:00" arrival_station_id="102"
            />
            <route
                start_date="2020-12-12" thread_id="NULL"
                departure_datetime="2020-12-12 02:00" departure_station_id="102"
                arrival_datetime="2020-12-12 03:00" arrival_station_id="103"
            />
            <route
                start_date="2020-12-12" thread_id="uid_train"
                departure_datetime="2020-12-12 03:00" departure_station_id="103"
                arrival_datetime="2020-12-12 04:00" arrival_station_id="104"
            />
        </variant>
    </group>
</routes>"""


ONE_DAY_BARIS_RESPONSE = {
    "departureStations": [101],
    "arrivalStations": [102],
    "flights": [
        {
            "airlineID": 301,
            "title": "SU 1",
            "departureDatetime": "2020-12-12T01:00:00+03:00",
            "departureStation": 101,
            "departureTerminal": "A",
            "arrivalDatetime": "2020-12-12T02:00:00+03:00",
            "arrivalStation": 102,
            "arrivalTerminal": "B",
            "route": [101, 102],
            "transportModelID": 201,
            "startDatetime": "2020-12-12T01:00:00+03:00",
        }
    ]
}


def _make_map_and_search():
    city_from = create_settlement(id=1001, time_zone="Etc/GMT-3", title="CityFrom")
    city_to = create_settlement(id=1004, time_zone="Etc/GMT-3", title="CityTo")
    middle_city = create_settlement(id=1002, time_zone="Etc/GMT-3", title="MiddleCity")
    station_from = create_station(settlement=city_from, id=101, time_zone="Etc/GMT-3")
    middle_station_to = create_station(settlement=middle_city, id=102, time_zone="Etc/GMT-3")
    middle_station_from = create_station(settlement=middle_city, id=103, time_zone="Etc/GMT-3")
    station_to = create_station(settlement=city_to, id=104, time_zone="Etc/GMT-3")
    create_company(id=301, title="CompanyPlane")
    company_train = create_company(id=302, title="CompanyTrain")
    create_transport_model(id=201, title="ModelPlane")

    create_thread(
        __={"calculate_noderoute": True},
        t_type=TransportType.TRAIN_ID,
        uid="uid_train",
        title="Train",
        company=company_train,
        year_days=[date(2020, 12, 12)],
        tz_start_time=time(3),
        schedule_v1=[
            [None, 0, middle_station_from, {"time_zone": "Etc/GMT-3"}],
            [60, None, station_to, {"time_zone": "Etc/GMT-3"}]
        ]
    )

    search = TransfersSearch(
        {
            "add_days_mask": False, "result_pytz": None,
            "transport_types": [TransportType.get_plane_type(), TransportType.get_train_type()]
        },
        RequestStub(),
        QueryPointsStub(city_from, city_to, date(2020, 12, 12))
    )

    with mock_baris_response(ONE_DAY_BARIS_RESPONSE):
        with mock.patch("route_search.transfers.transfers.get_pathfinder_response", return_value=PATHFINDER_XML):
            search.search()

    code_getter = get_code_getter(
        (station_from, middle_station_to, middle_station_from, station_to, city_from, middle_city, city_to), ["yandex"]
    )

    return search, code_getter


@replace_now("2020-12-12")
def test_transfers_search():
    search, _ = _make_map_and_search()

    assert_that(list(search.used_points), contains_inanyorder(
        has_properties({"id": 101}),
        has_properties({"id": 102}),
        has_properties({"id": 103}),
        has_properties({"id": 104}),
        has_properties({"id": 1001}),
        has_properties({"id": 1002}),
        has_properties({"id": 1004}),
    ))

    assert len(search.segments) == 1
    variant = search.segments[0]
    assert isinstance(variant, Variant)
    assert len(variant.segments) == 2
    assert isinstance(variant.segments[0], BarisTransferSegment)
    assert "transfer" in variant.segments[0].display_info
    assert isinstance(variant.segments[1], RaspDBTransferSegment)

    assert len(search.interval_segments) == 0


@replace_now("2020-12-12")
def test_transfer_variant2json():
    search, code_getter = _make_map_and_search()
    fill_thread_local_start_dt(search.segments)

    assert len(search.segments) == 1
    variant = search.segments[0]
    segment_json = transfer_variant2json(variant, code_getter)

    assert_that(segment_json, has_entries({
        "has_transfers": True,
        "departure_from": has_entries({"code": "s101"}),
        "arrival_to": has_entries({"code": "s104"}),
        "transport_types": contains("plane", "train"),
        "departure": "2020-12-12T01:00:00+03:00",
        "arrival": "2020-12-12T04:00:00+03:00",
        "transfers": contains(
            has_entries({"code": "c1002"})
        ),

        "details": contains(
            has_entries({
                "thread": has_entries({
                    "number": "SU 1",
                    "title": "CityFrom \u2014 MiddleCity",
                    "short_title": "CityFrom \u2014 MiddleCity",
                    "carrier": has_entries({"code": 301}),
                    "vehicle": "ModelPlane",
                    "express_type": None,
                    "transport_type": "plane",
                    "transport_subtype": {"title": None, "code": None, "color": None},
                    "uid": "SU-1_201212_c301_12",
                }),

                "from": has_entries({"code": "c1001"}),
                "to": has_entries({"code": "c1002"}),
                "departure_platform": "",
                "arrival_platform": "",
                "departure_terminal": "A",
                "arrival_terminal": "B",
                "stops": "",
                "duration": 3600,
                "start_date": "2020-12-12",

                "departure": "2020-12-12T01:00:00+03:00",
                "arrival": "2020-12-12T02:00:00+03:00",
            }),

            has_entries({
                "is_transfer": True,
                "duration": 3600,
                "transfer_point": has_entries({"code": "c1002"}),
                "transfer_from": has_entries({"code": "s102"}),
                "transfer_to": has_entries({"code": "s103"}),
            }),

            has_entries({
                "thread": has_entries({
                    "title": "Train",
                    "carrier": has_entries({"code": 302}),
                    "transport_type": "train",
                    "uid": "uid_train",
                }),
                "stops": "",
                "from": has_entries({"code": "c1002"}),
                "to": has_entries({"code": "c1004"}),
                "departure_platform": "",
                "arrival_platform": "",
                "departure_terminal": None,
                "arrival_terminal": None,
                "duration": 3600,

                "departure": "2020-12-12T03:00:00+03:00",
                "arrival": "2020-12-12T04:00:00+03:00",
                "start_date": "2020-12-12"
            })
        )
    }))
