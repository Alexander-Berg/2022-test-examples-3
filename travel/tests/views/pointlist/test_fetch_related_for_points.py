# -*- coding: utf-8 -*-
from travel.avia.library.python.common.models.geo import Country, Station, Settlement
from travel.avia.library.python.geosearch.views.pointlist import fetch_related_for_points
from travel.avia.library.python.tester.factories import create_station, create_settlement
from travel.avia.library.python.tester.testcase import TestCase


class TestFetchRelatedForPoints(TestCase):
    def test_one_station_and_one_settlement(self):
        station_settlement = create_settlement(id=666)
        create_station(id=777, settlement=station_settlement, country_id=Country.RUSSIA_ID)

        create_settlement(id=888, country_id=Country.RUSSIA_ID)

        station = Station.objects.get(id=777)
        settlement = Settlement.objects.get(id=888)

        fetch_related_for_points([station, settlement])

        with self.assertNumQueries(0):
            assert station.settlement
            assert station.country
            assert settlement.country
