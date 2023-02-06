# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

from common.tester.factories import create_station, create_settlement
from common.tester.testcase import TestCase
from travel.rasp.morda_backend.morda_backend.settlement.serialization.station_helpers import station_aeroexpress_json


class TestStationAeroexpressJson(TestCase):
    def setUp(self):
        super(TestStationAeroexpressJson, self).setUp()
        self.settlement_id = 888
        self.settlement = create_settlement(id=self.settlement_id)

    def test_bus(self):
        station = create_station(t_type='bus', has_aeroexpress=True, settlement=self.settlement)

        assert station_aeroexpress_json(station) is None

    def test_no_aeroexpress(self):
        station = create_station(t_type='plane', has_aeroexpress=False, settlement=self.settlement)

        assert station_aeroexpress_json(station) is None

    def test_no_settlement(self):
        station = create_station(t_type='plane', has_aeroexpress=True, settlement=None)

        assert station_aeroexpress_json(station) is None

    def test_aeroexpress(self):
        station = create_station(t_type='plane', has_aeroexpress=True, settlement=self.settlement)

        assert station_aeroexpress_json(station) == {
            'settlement_id': self.settlement_id
        }
