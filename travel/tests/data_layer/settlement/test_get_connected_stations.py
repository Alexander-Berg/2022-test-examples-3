# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

from common.models.geo import StationMajority, Country
from common.tester.factories import create_settlement, create_country, create_station
from common.tester.testcase import TestCase
from travel.rasp.train_api.data_layer.settlement import get_connected_stations


class TestGetConnectedStations(TestCase):
    def test_foreign(self):
        """
        Город не входит в 'наши страны'.
        В результат попадает только аэропорт. Автобусная, ж/д станции не попадают в результат.
        """
        country = create_country(id=84, title=u'США')
        settlement = create_settlement(country=country)

        create_station(t_type=u'bus', settlement=settlement, majority=StationMajority.IN_TABLO_ID)
        create_station(t_type=u'train', settlement=settlement, majority=StationMajority.IN_TABLO_ID)
        create_station(t_type=u'water', settlement=settlement, majority=StationMajority.IN_TABLO_ID)
        airport = create_station(t_type=u'plane', settlement=settlement, majority=StationMajority.IN_TABLO_ID)

        assert get_connected_stations(settlement, 3) == {airport}

    def test_lithuania(self):
        """
        Город входит в 'наши страны'. В результат попадают все станции.
        """
        country = create_country(id=Country.LITVA_ID)
        settlement = create_settlement(country=country)

        bus_station = create_station(t_type=u'bus', settlement=settlement, majority=StationMajority.IN_TABLO_ID)
        train_station = create_station(t_type=u'train', settlement=settlement, majority=StationMajority.IN_TABLO_ID)
        water_station = create_station(t_type=u'water', settlement=settlement, majority=StationMajority.IN_TABLO_ID)
        airport = create_station(t_type=u'plane', settlement=settlement, majority=StationMajority.IN_TABLO_ID)

        assert get_connected_stations(settlement, 3) == set([bus_station, train_station, water_station, airport])
