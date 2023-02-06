# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

import hamcrest
import pytest

from travel.buses.connectors.tests.yabus.common.library.test_utils import converter_patch
from yabus.etraffic.entities.ride_details import RideDetails
from yabus.util.formatting import python


class TestDocumentType(object):
    @pytest.mark.parametrize(
        "value, expected",
        (
            ("0", {"id": RideDetails.DOC_TYPE_PASSPORT, "name": "id"}),
            ("00", {"id": RideDetails.DOC_TYPE_PASSPORT, "name": "id"}),
            ("3", {"id": RideDetails.DOC_TYPE_FOREIGN_PASSPORT, "name": "foreign passport"}),
            ("99", None),
            ("a", None),
            ("", None),
            (None, None),
        ),
    )
    def test_format(self, value, expected):
        assert RideDetails.DocumentType().format(value) == expected


class TestRideDetails(object):
    DUMMY_RIDE = {
        "race": {
            "uid": "141856298:0000002863:20211228:000000021:000000018",
            "freeSeatCount": 14,
            "price": 550.0,
            "supplierPrice": 550.0,
            "dispatchPointId": 141856298,
            "arrivalPointId": 141856335,
        },
        "docTypes": [{"code": "00", "name": "Паспорт РФ", "type": "00"}],
        "ticketTypes": [{"code": "1#0#0", "name": "Полный", "price": 550.0, "ticketClass": "P"}],
        "seats": [{"code": "1", "name": "Место 1"}],
        "citizenships": [],
        "dispatchPointId": 141856298,
        "dispatchStationName": "Воронеж (Остужева 6а)",
        "dispatchDate": "2021-12-28T08:20:00",
        "arrivalPointId": 141856335,
        "arrivalStationName": "Тамбов АВ Северный",
        "arrivalDate": "2021-12-28T11:25:00",
    }

    def test_seats(self):
        with converter_patch("etraffic"):
            ride_details = RideDetails.init(python(TestRideDetails.DUMMY_RIDE))

            hamcrest.assert_that(ride_details, hamcrest.has_entry("seats", [{"code": "1", "number": "1"}]))

    @pytest.mark.parametrize("seat_name", ("свободная рассадка", "Без места"))
    def test_no_seats(self, seat_name):
        with converter_patch("etraffic"):
            ride = TestRideDetails.DUMMY_RIDE.copy()
            ride["seats"] = [{"code": "1", "name": seat_name}]
            ride_details = RideDetails.init(python(ride))

            hamcrest.assert_that(ride_details, hamcrest.has_entry("seats", None))

    def test_ticket_types(self):
        with converter_patch("etraffic"):
            ride = TestRideDetails.DUMMY_RIDE.copy()
            ride["ticketTypes"] = [
                {"code": "1#0#0", "name": "Полный", "price": 550.0, "ticketClass": "P"},
                {"code": "1#0#0", "name": "Полный дешёвый", "price": 500.0, "ticketClass": "P"},
                {"code": "0#0#0", "name": "Неправильный", "price": -100.0, "ticketClass": "X"},
            ]
            ride_details = RideDetails.init(python(ride))

            hamcrest.assert_that(
                ride_details,
                hamcrest.has_entry(
                    "ticketTypes",
                    hamcrest.contains(
                        hamcrest.has_entries({"code": "1#0#0", "type": {"id": 1, "name": "full"}, "price": 550.0})
                    ),
                ),
            )

    def test_doc_types(self):
        with converter_patch("etraffic"):
            ride = TestRideDetails.DUMMY_RIDE.copy()
            ride["docTypes"] = [
                {"code": "3", "name": "Паспорт иностранного гражданина", "type": "3"},
                {"code": "2", "name": "Удостоверение иностранного гражданина", "type": "3"},
            ]
            ride_details = RideDetails.init(python(ride))

            hamcrest.assert_that(
                ride_details,
                hamcrest.has_entry(
                    "docTypes",
                    hamcrest.contains(
                        hamcrest.has_entries({"code": "3", "type": {"id": 4, "name": "foreign passport"}})
                    ),
                ),
            )
