# -*- coding: utf-8 -*-

from __future__ import unicode_literals, absolute_import

from django.conf import settings

from travel.avia.library.python.common.models.geo import Settlement
from travel.avia.library.python.common.models.geo import StationMajority, Station2Settlement
from travel.avia.library.python.common.models.transport import TransportType
from travel.avia.backend.main.lib.geo import get_airport_city, get_capital
from travel.avia.library.python.tester.factories import create_settlement, create_station, create_country, create_region
from travel.avia.library.python.tester.testcase import TestCase


class TestGetAirportCity(TestCase):
    def setUp(self):
        super(TestGetAirportCity, self).setUp()

        self.settlement = create_settlement(majority=StationMajority.NOT_IN_TABLO_ID)

    def test_station_related(self):
        station = create_station(settlement=self.settlement, t_type=TransportType.PLANE_ID)
        Station2Settlement.objects.create(station=station, settlement=self.settlement)

        assert get_airport_city(self.settlement, 'ru') == self.settlement

    def test_region_capital(self):
        country = create_country(title='Country')
        region = create_region(title='Region', country=country)
        expected = create_settlement(region=region)

        self.settlement.region = region

        # Опа. А тут бага. Столица региона тоже может быть без аэропорта

        assert get_airport_city(self.settlement, 'ru') == expected

    def test_aliases(self):
        from_id = settings.SETTLEMENT_ALIASES.keys()[0]
        to_id = settings.SETTLEMENT_ALIASES[from_id]

        settlement = create_settlement(id=from_id)
        expected = create_settlement(id=to_id)

        assert get_airport_city(settlement, 'ru') == expected

    def test_country(self):
        country = create_country(title='Country')

        expected = create_settlement(country=country)

        self.settlement.country = country

        # И тут бага. Столица страны тоже может быть аэропорта.

        assert get_airport_city(expected, 'ru').id == get_capital(country, 'ru').id

    def test_default(self):
        assert get_airport_city(self.settlement, 'ru') == Settlement.get_default_city()
