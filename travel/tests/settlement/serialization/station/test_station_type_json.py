# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

from common.models.geo import StationType
from common.tester.factories import create_station
from common.tester.testcase import TestCase
from travel.rasp.morda_backend.morda_backend.settlement.serialization.station_helpers import station_type_json


class TestStationTypeJson(TestCase):
    def test_station_type_json_airport(self):
        station = create_station(station_type_id=StationType.AIRPORT_ID)

        assert station_type_json(station, 'uk') == {
            'id': StationType.AIRPORT_ID,
            'code': 'airport',
            'title': u'Аеропорт'
        }

    def test_station_type_json_stop(self):
        station = create_station(station_type_id=StationType.MARINE_STATION_ID)

        assert station_type_json(station, 'uk') == {
            'id': StationType.MARINE_STATION_ID,
            'code': 'marine_station',
            'title': u'Морський вокзал'
        }

    def test_station_type_unknown(self):
        station = create_station(station_type_id=StationType.UNKNOWN_ID, t_type='water')

        assert station_type_json(station, 'uk') == {
            'id': StationType.UNKNOWN_ID,
            'code': 'station_without_type',
            'title': u'Станція без типу'
        }
