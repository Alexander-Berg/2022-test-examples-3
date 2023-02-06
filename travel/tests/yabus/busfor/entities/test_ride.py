# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

from yabus.busfor.entities.ride import Ride


class TestRideDateTime(object):
    def test_format(self):
        assert Ride.DateTime(dt_format=b"iso8601").format("2000-01-01 12:00") == "2000-01-01T12:00:00"


class TestRideDescription(object):
    def test_output(self):
        field = Ride.Description(attribute="foo.bar", default="default_description")

        assert field.output(None, {"foo": {"bar": "point_id"}, "points": {}}) == "default_description"
        assert (
            field.output(None, {"foo": {"bar": "point_id"}, "points": {"point_id": {"name": None, "address": None}}})
            == "default_description"
        )
        assert (
            field.output(
                None,
                {
                    "foo": {"bar": "point_id"},
                    "points": {"point_id": {"name": "point_name", "address": "point_address"}},
                },
            )
            == "point_name, point_address"
        )


class TestRidePrice(object):
    def test_format(self):
        assert Ride.Price().format(12345) == 123.45
