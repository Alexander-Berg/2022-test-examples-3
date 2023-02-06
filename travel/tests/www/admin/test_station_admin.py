# coding: utf8

import pytest

from tester.factories import create_station, create_settlement, create_district
from travel.rasp.admin.www.admin import StationAdmin


@pytest.mark.dbuser
def test_station_district_in_path():
    district = create_district(id=101, title='d1')
    district2 = create_district(id=102, title='d2')
    settlement = create_settlement(id=201, title='s1', district=district2)
    station = create_station(id=301, title='st1', settlement=settlement, district=district)
    station2 = create_station(id=302, title='st2', district=district2)

    path = StationAdmin.station_full_path(station, True)

    assert 'd1' in path
    assert 'd2' not in path

    path = StationAdmin.station_full_path(station2, True)
    assert 'd2' in path

@pytest.mark.dbuser
def test_station_settlement_district_in_path():
    district = create_district(id=101, title='d1')
    settlement = create_settlement(id=201, title='s1', district=district)
    station = create_station(id=301, title='st1', settlement=settlement)

    path = StationAdmin.station_full_path(station, True)

    assert 'd1' in path

@pytest.mark.dbuser
def test_station_without_settlement_in_path():
    station = create_station(id=301, title='st1')

    path = StationAdmin.station_full_path(station, True)

    assert 'st1' in path
