# -*- coding: utf-8 -*-

from common.models.geo import Country, Station, Settlement
from common.tester.factories import create_settlement, create_station
from common.tester.testcase import TestCase
from geosearch.views.pointlist import fetch_related_for_points


class TestFetchRelatedForPoints(TestCase):
    def test_one_station_and_one_settlement(self):
        station_settlement = create_settlement(id=666)
        create_station(id=777, settlement=station_settlement, country_id=Country.RUSSIA_ID)

        create_settlement(id=888, country_id=Country.RUSSIA_ID)

        station = Station.objects.get(id=777)
        settlement = Settlement.objects.get(id=888)

        fetch_related_for_points([station, settlement])

        with self.assertNumQueries(0):
            station_settlement = station.settlement  # noqa
            station_country = station.country  # noqa
            settlement_country = settlement.country  # noqa
