# -*- coding=utf-8 -*-

from travel.avia.library.python.common.models.transport import TransportType
from travel.avia.library.python.tester.factories import create_settlement, create_station
from travel.avia.library.python.tester.testcase import TestCase


class TestSettlementWithWaterStations(TestCase):
    def test_get_stations_by_type_old_style(self):
        settlement = create_settlement()
        water_station = create_station(settlement=settlement, t_type=TransportType.WATER_ID)

        stations_by_type = settlement.get_stations_by_type()
        assert {s[0] for s in stations_by_type['sea']['stations']} == {water_station}
        assert 'water' not in stations_by_type

    def test_get_stations_by_type_new_style(self):
        settlement = create_settlement()
        water_station = create_station(settlement=settlement, t_type=TransportType.WATER_ID)

        stations_by_type = settlement.get_stations_by_type(use_water=True)
        assert {s[0] for s in stations_by_type['water']['stations']} == {water_station}
        assert 'sea' not in stations_by_type
        assert 'river' not in stations_by_type

        stations_by_type = settlement.get_stations_by_type(use_water=False)
        assert {s[0] for s in stations_by_type['sea']['stations']} == {water_station}
        assert 'water' not in stations_by_type
