# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

from common.models.geo import StationMajority
from common.tester.factories import create_station
from common.tester.testcase import TestCase
from travel.rasp.morda_backend.morda_backend.settlement.serialization.station_helpers import station_majority_json


class TestStationMajorityJson(TestCase):
    def test_station_majority_json_in_tablo(self):
        station = create_station(majority=StationMajority.IN_TABLO_ID)

        assert station_majority_json(station) == {
            'id': StationMajority.IN_TABLO_ID,
            'code': 'in_tablo'
        }

    def test_station_majority_json_main_in_city(self):
        station = create_station(majority=StationMajority.MAIN_IN_CITY_ID)

        assert station_majority_json(station) == {
            'id': StationMajority.MAIN_IN_CITY_ID,
            'code': 'main_in_city'
        }
