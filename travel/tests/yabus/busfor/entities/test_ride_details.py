# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

import hamcrest
import pytest

from yabus.busfor.entities.ride_details import RideDetails


class TestRideDetailsTicketType(object):
    @pytest.mark.parametrize(
        "code, expected",
        (
            ("Y99", {"id": 1, "name": "full"}),
            ("Y3B01 - 12", {"id": 2, "name": "child"}),
            ("YLL60", None),
            ("Y3ZB00-03", None),
            ("YBINF00-05", None),
            ("YPROM2B00-05", None),
            ("R123", None),
        ),
    )
    def test_format(self, code, expected):
        assert RideDetails.TicketType().format(code) == expected


class TestRideDetails(object):
    def test_format_ticket_types(self):
        hamcrest.assert_that(
            RideDetails().format(
                {"segment": {"price": {"tariffs": [{"code": "Y99"}, {"code": "R99"}]}}, "map_seat": {}, "points": {}}
            ),
            hamcrest.has_entries(
                "ticketTypes", hamcrest.contains(hamcrest.has_entries("type", {"id": 1, "name": "full"}))
            ),
        )

    def test_format_multitiers(self):
        hamcrest.assert_that(
            RideDetails().format(
                {
                    "segment": {"price": {"tariffs": [{"code": "Y99"}]}},
                    "map_seat": {"map_seat": [{"z": 0, "status": 1}, {"z": 0, "status": 1}]},
                    "points": {},
                }
            ),
            hamcrest.has_entries("map", hamcrest.instance_of(list)),
        )

        hamcrest.assert_that(
            RideDetails().format(
                {
                    "segment": {"price": {"tariffs": [{"code": "Y99"}]}},
                    "map_seat": {"map_seat": [{"z": 0, "status": 1}, {"z": 1, "status": 1}]},
                    "points": {},
                }
            ),
            hamcrest.has_entries("map", None),
        )
