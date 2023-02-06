# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

import pytest

from common.tester.factories import create_country, create_region, create_settlement, create_station
from travel.rasp.wizards.proxy_api.lib.station.search import find_aeroexpress_station, find_station, get_region_and_country_ids
from travel.rasp.wizards.proxy_api.lib.station.settlement_stations_cache import SettlementStationsCache
from travel.rasp.wizards.proxy_api.lib.tests_utils import create_indexed_points

pytestmark = pytest.mark.dbuser


@pytest.mark.parametrize('point_factory, expected', (
    (None, (None, None)),
    (create_settlement.mutate(region=dict(id=100), country=dict(id=200)), (100, 200)),
    (create_region.mutate(id=100, country=dict(id=200)), (100, 200)),
    (create_country.mutate(id=200), (None, 200)),
))
def test_get_region_and_country_ids(point_factory, expected):
    if point_factory is not None:
        point_factory(_geo_id=123)

    assert get_region_and_country_ids(123) == expected


def test_find_aeroexpress_station_not_found():
    assert find_aeroexpress_station('airport') is None


def test_find_aeroexpress_station():
    (airport,) = create_indexed_points(create_station.mutate(title_en='airport', t_type='plane'))

    assert find_aeroexpress_station('airport') == airport


def test_find_aeroexpress_station_by_code():
    (airport,) = create_indexed_points(create_station.mutate(t_type='plane', __={'codes': {'iata': 'AIR'}}))

    assert find_aeroexpress_station('AIR') == airport


def test_find_aeroexpress_station_hidden():
    create_indexed_points(create_station.mutate(title_en='airport', t_type='plane', hidden=True))

    assert find_aeroexpress_station('airport') is None


def test_find_aeroexpress_station_other_transport():
    create_indexed_points(create_station.mutate(title_en='station', t_type='bus'))

    assert find_aeroexpress_station('station') is None


def test_find_aeroexpress_station_ambiguous():
    create_indexed_points(
        create_station.mutate(title_en='airport', t_type='plane'),
        create_station.mutate(title_en='airport', t_type='plane')
    )

    assert find_aeroexpress_station('airport') is None


def test_find_station_not_found():
    with SettlementStationsCache.using_precache():
        assert find_station('station_name') is None


def test_find_station():
    minor_station, station = create_indexed_points(
        create_station.mutate(title_en='station_name', majority='in_tablo'),
        create_station.mutate(title_en='station_name', majority='main_in_city')
    )

    with SettlementStationsCache.using_precache():
        assert find_station('station_name') == station


def test_find_station_by_code():
    (station,) = create_indexed_points(create_station.mutate(__={'codes': {'express': '123'}}))

    with SettlementStationsCache.using_precache():
        assert find_station('123') == station


def test_find_station_by_client_geoid():
    local_station, major_station = create_indexed_points(
        create_station.mutate(
            title_en='station_name', majority='in_tablo', settlement=dict(_geo_id=123, country=dict())
        ),
        create_station.mutate(
            title_en='station_name', majority='main_in_city'
        )
    )

    with SettlementStationsCache.using_precache():
        assert find_station('station_name') == major_station
        assert find_station('station_name', client_geoid=999) == major_station
        assert find_station('station_name', client_geoid=123) == local_station


def test_find_station_by_settlement():
    (station,) = create_indexed_points(
        create_station.mutate(settlement=dict(title_en='settlement_name'))
    )

    with SettlementStationsCache.using_precache():
        assert find_station('settlement_name') == station


def test_find_station_by_settlement_code():
    (station,) = create_indexed_points(create_station.mutate(settlement=dict(iata='CODE')))

    with SettlementStationsCache.using_precache():
        assert find_station('CODE') == station


def test_find_station_by_ambigous_settlement():
    create_indexed_points(
        create_settlement.mutate(title_en='settlement_name'),
        create_station.mutate(settlement=dict(title_en='settlement_name'))
    )

    with SettlementStationsCache.using_precache():
        assert find_station('settlement_name') is None


def test_find_station_by_settlement_and_client_geoid():
    local_station, _other_station = create_indexed_points(
        create_station.mutate(settlement=dict(title_en='settlement_name', _geo_id=123, country=dict())),
        create_station.mutate(settlement=dict(title_en='settlement_name'))
    )

    with SettlementStationsCache.using_precache():
        assert find_station('settlement_name') is None
        assert find_station('settlement_name', client_geoid=123) == local_station


def test_find_station_by_transport_code():
    train_station, major_station = create_indexed_points(
        create_station.mutate(title_en='station_name', majority='in_tablo', t_type='train'),
        create_station.mutate(title_en='station_name', majority='main_in_city')
    )

    with SettlementStationsCache.using_precache():
        assert find_station('station_name') == major_station
        assert find_station('station_name', 'train') == train_station
        assert find_station('station_name', 'suburban') == train_station


def test_find_station_by_settlement_and_transport_code():
    settlement = create_settlement(title_en='settlement_name')
    train_station, major_station = create_indexed_points(
        create_station.mutate(majority='in_tablo', t_type='train', settlement=settlement),
        create_station.mutate(majority='main_in_city', settlement=settlement)
    )

    with SettlementStationsCache.using_precache():
        assert find_station('settlement_name') == major_station
        assert find_station('settlement_name', 'train') == train_station
        assert find_station('settlement_name', 'suburban') == train_station
