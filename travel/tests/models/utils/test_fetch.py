# coding: utf-8

import pytest

from travel.avia.library.python.common.models.geo import Station
from travel.avia.library.python.common.models_utils import fetch_related, fetch_related_safe
from travel.avia.library.python.tester.factories import create_station, create_country, create_settlement
from travel.avia.library.python.tester.testcase import TestCase


class TestFetchRelated(TestCase):
    def test_fetch_related(self):
        """Проверяем добавление связанных полей."""
        settlement = create_settlement()
        country = create_country()
        create_station(settlement=settlement, country=country)
        create_station(settlement=settlement, country=country)

        stations = Station.objects.all()

        for station in stations:
            assert getattr(station, '_settlement_cache', None) is None
            assert getattr(station, '_country_cache', None) is None

        fetch_related(stations, 'settlement', 'country', model=Station)

        for station in stations:
            assert getattr(station, '_settlement_cache') == settlement
            assert station.settlement == settlement
            assert getattr(station, '_country_cache') == country
            assert station.country == country

    def test_fetch_related_default_model(self):
        """Проверяем добавление связанных полей без указания модели."""
        settlement = create_settlement()
        create_station(settlement=settlement)
        stations = Station.objects.all()

        assert getattr(stations[0], '_settlement_cache', None) is None
        fetch_related(stations, 'settlement')
        assert getattr(stations[0], '_settlement_cache') == settlement
        assert stations[0].settlement == settlement

        with pytest.raises(ValueError):
            stations.model = None
            fetch_related(stations, 'settlement')

    def test_fetch_related_safe(self):
        """Проверяем "безопасное" добавление полей."""
        settlement = create_settlement()
        create_station(id=1, settlement=settlement)
        create_station(id=2, settlement=settlement)
        stations = list(Station.objects.all().order_by())

        stations[0].settlement_id = settlement.id + 1
        with pytest.raises(KeyError) as exc_info:
            fetch_related(stations, 'settlement', model=Station)
        assert exc_info.value.args[0] == settlement.id + 1

        fetched_stations = fetch_related_safe(stations, 'settlement', model=Station)
        assert len(fetched_stations) == 1
        assert fetched_stations[0].id == 2
        assert getattr(fetched_stations[0], '_settlement_cache') == settlement
        assert fetched_stations[0].settlement == settlement
