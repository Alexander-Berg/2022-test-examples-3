# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

import pytest

from common.models.transport import TransportType
from common.tester.factories import create_settlement, create_station
from travel.rasp.wizards.proxy_api.lib.station.settlement_stations_cache import SettlementStationsCache

pytestmark = pytest.mark.dbuser


@pytest.mark.parametrize('station_factories', (
    (),
    (create_station.mutate(hidden=True),),
    (create_station.mutate(), create_station.mutate(),),
))
def test_station_not_found(station_factories):
    settlement = create_settlement()

    for station_factory in station_factories:
        station_factory(settlement=settlement)

    with SettlementStationsCache.using_precache():
        assert SettlementStationsCache.get(settlement) is None
        assert SettlementStationsCache.get(settlement, TransportType.BUS_ID) is None


def test_major_station_found():
    settlement = create_settlement()
    create_station(settlement=settlement, majority='in_tablo')
    station = create_station(settlement=settlement, majority='main_in_city')

    with SettlementStationsCache.using_precache():
        assert SettlementStationsCache.get(settlement) == station
        assert SettlementStationsCache.get(settlement, TransportType.BUS_ID) == station


def test_transport_station_found():
    settlement = create_settlement()
    major_station = create_station(settlement=settlement, t_type='plane', majority='main_in_city')
    bus_station = create_station(settlement=settlement, t_type='bus', majority='in_tablo')
    train_station = create_station(settlement=settlement, t_type='train', majority='in_tablo')

    with SettlementStationsCache.using_precache():
        assert SettlementStationsCache.get(settlement) == major_station
        assert SettlementStationsCache.get(settlement, TransportType.PLANE_ID) == major_station
        assert SettlementStationsCache.get(settlement, TransportType.BUS_ID) == bus_station
        assert SettlementStationsCache.get(settlement, TransportType.TRAIN_ID) == train_station
