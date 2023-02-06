# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

from travel.buses.connectors.tests. yabus.common.library.test_utils import converter_patch
from yabus.common.entities.entity import Entity
from yabus.ecolines.entities.ride import Ride


class TestRide(object):
    def test_endpoint(self):
        class DummyRide(Entity):
            fields = {"endpoint": Ride.Endpoint}

        with converter_patch("ecolines"):
            assert DummyRide.init(
                {"endpoint": "station_id"}, stations={"station_id": "station_desc"}
            ) == {"endpoint": {"id": "backmap(station_id)", "desc": "station_desc", "supplier_id": "station_id"}}
