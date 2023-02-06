# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest
from hamcrest import has_entries, assert_that, contains_inanyorder

from common.models.geo import Country, StationMajority
from common.models.transport import TransportType
from common.tester.factories import create_settlement, create_station
from travel.rasp.morda_backend.morda_backend.station.data_layer.city_stations import get_station_city_stations


pytestmark = [pytest.mark.dbuser]


def test_get_station_city_stations():
    station_0 = create_station(t_type=TransportType.TRAIN_ID, majority=StationMajority.IN_TABLO_ID)

    assert get_station_city_stations({'station_id': station_0.id}) == {'settlement': None, 'cityStations': {}}

    settlement = create_settlement(country=Country.RUSSIA_ID, title_ru='Город', title_ru_genitive='Города')
    station_1 = create_station(
        id=101, settlement=settlement, t_type=TransportType.TRAIN_ID,
        title='вокзал_1', majority=StationMajority.MAIN_IN_CITY_ID,
        type_choices='train,suburban'
    )
    station_2 = create_station(
        id=102, settlement=settlement, t_type=TransportType.TRAIN_ID,
        title='вокзал_2', majority=StationMajority.IN_TABLO,
        type_choices='suburban'
    )
    create_station(
        id=103, settlement=settlement, t_type=TransportType.TRAIN_ID,
        title='вокзал_3', majority=StationMajority.NOT_IN_TABLO_ID,
        type_choices='train'
    )
    create_station(
        id=104, settlement=settlement, t_type=TransportType.TRAIN_ID,
        title='вокзал_4', majority=StationMajority.IN_TABLO,
        type_choices=''
    )

    station_r_1 = create_station(
        id=111, t_type=TransportType.TRAIN_ID, title='вокзал_р_1',
        majority=StationMajority.MAIN_IN_CITY_ID, type_choices='train,suburban'
    )
    station_r_2 = create_station(
        id=112, t_type=TransportType.TRAIN_ID, title='вокзал_р_2',
        majority=StationMajority.IN_TABLO, type_choices='suburban'
    )
    station_r_3 = create_station(
        id=113, t_type=TransportType.TRAIN_ID, title='вокзал_р_3',
        majority=StationMajority.NOT_IN_TABLO_ID, type_choices='train'
    )
    station_r_4 = create_station(
        id=114, t_type=TransportType.TRAIN_ID, title='вокзал_р_4',
        majority=StationMajority.IN_TABLO, type_choices=''
    )

    settlement.related_stations.create(station=station_r_1)
    settlement.related_stations.create(station=station_r_2)
    settlement.related_stations.create(station=station_r_3)
    settlement.related_stations.create(station=station_r_4)

    create_station(
        id=121, settlement=settlement, t_type=TransportType.PLANE_ID, title='аэропорт_1', type_choices='tablo'
    )
    create_station(
        id=122, settlement=settlement, t_type=TransportType.PLANE_ID, title='аэропорт_2'
    )
    airport_r_1 = create_station(
        id=131, t_type=TransportType.PLANE_ID, title='аэропорт_р_1', type_choices='tablo'
    )
    airport_r_2 = create_station(
        id=132, t_type=TransportType.PLANE_ID, title='аэропорт_р_2'
    )

    settlement.related_stations.create(station=airport_r_1)
    settlement.related_stations.create(station=airport_r_2)

    create_station(
        id=141, settlement=settlement, t_type=TransportType.BUS_ID, title='автовокзал_1', type_choices='schedule'
    )
    create_station(
        id=142, settlement=settlement, t_type=TransportType.BUS_ID, title='автовокзал_2'
    )
    stop_r_1 = create_station(
        id=151, t_type=TransportType.BUS_ID, title='автовокзал_р_1', type_choices='schedule'
    )
    stop_r_2 = create_station(
        id=152, t_type=TransportType.BUS_ID, title='автовокзал_р_2'
    )

    settlement.related_stations.create(station=stop_r_1)
    settlement.related_stations.create(station=stop_r_2)

    create_station(
        id=161, settlement=settlement, t_type=TransportType.WATER_ID, title='порт_1', type_choices='schedule'
    )
    create_station(
        id=162, settlement=settlement, t_type=TransportType.WATER_ID, title='порт_2'
    )
    port_r_1 = create_station(
        id=171, t_type=TransportType.WATER_ID, title='порт_р_1', type_choices='schedule'
    )
    port_r_2 = create_station(
        id=172, t_type=TransportType.WATER_ID, title='порт_р_2'
    )

    settlement.related_stations.create(station=port_r_1)
    settlement.related_stations.create(station=port_r_2)

    city_stations_1 = get_station_city_stations({'station_id': station_1.id})
    city_stations_2 = get_station_city_stations({'station_id': station_2.id})

    assert city_stations_1 == city_stations_2

    assert 'settlement' in city_stations_1
    assert_that(city_stations_1['settlement'], has_entries({
        'title': 'Город',
        'titleGenitive': 'Города'
    }))

    assert 'cityStations' in city_stations_1
    assert_that(city_stations_1['cityStations'], has_entries({
        'train': contains_inanyorder(
            has_entries({
                'id': 101, 'title': 'вокзал_1',
                'mainSubtype': 'train', 'subtypes': ['train', 'suburban', 'tablo']
            }),
            has_entries({
                'id': 111, 'title': 'вокзал_р_1',
                'mainSubtype': 'train', 'subtypes': ['train', 'suburban', 'tablo']
            }),
            has_entries({
                'id': 102, 'title': 'вокзал_2',
                'mainSubtype': 'suburban', 'subtypes': ['suburban']
            }),
            has_entries({
                'id': 112, 'title': 'вокзал_р_2',
                'mainSubtype': 'suburban', 'subtypes': ['suburban']
            }),
        ),
        'plane': contains_inanyorder(
            has_entries({
                'id': 121, 'title': 'аэропорт_1',
                'mainSubtype': 'plane', 'subtypes': ['plane']
            }),
            has_entries({
                'id': 131, 'title': 'аэропорт_р_1',
                'mainSubtype': 'plane', 'subtypes': ['plane']
            }),
        ),
        'bus': contains_inanyorder(
            has_entries({
                'id': 141, 'title': 'автовокзал_1',
                'mainSubtype': 'schedule', 'subtypes': ['schedule']
            }),
            has_entries({
                'id': 151, 'title': 'автовокзал_р_1',
                'mainSubtype': 'schedule', 'subtypes': ['schedule']
            }),
        ),
        'water': contains_inanyorder(
            has_entries({
                'id': 161, 'title': 'порт_1',
                'mainSubtype': 'schedule', 'subtypes': ['schedule']
            }),
            has_entries({
                'id': 171, 'title': 'порт_р_1',
                'mainSubtype': 'schedule', 'subtypes': ['schedule']
            }),
        )
    }))
