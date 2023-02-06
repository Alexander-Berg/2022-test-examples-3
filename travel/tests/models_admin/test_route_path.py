# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest

from common.models_admin.geo import RoutePath
from common.tester.factories import create_station

pytestmark = pytest.mark.dbuser


def create_route_path(s_from, s_to, status_direct=RoutePath.STATUS_CONFIRMED, status_back=RoutePath.STATUS_CONFIRMED):
    RoutePath.objects.create(
        station_from_id=s_from,
        station_to_id=s_to,
        status_direct=status_direct,
        status_back=status_back,
        for_two_directions=True
    )


def test_get_route_paths():
    st0 = create_station(id=100)
    st1 = create_station(id=101)
    st2 = create_station(id=102)
    st3 = create_station(id=103)

    create_route_path(100, 101)
    create_route_path(101, 102)
    create_route_path(102, 103, RoutePath.STATUS_CHANGED, RoutePath.STATUS_REMOVED)

    stations = [st0, st1, st2, st3]
    route_paths = RoutePath.get_route_paths(stations)

    assert len(route_paths) == 3

    assert route_paths[0].station_from == st0
    assert route_paths[0].station_to == st1
    assert route_paths[0].status == RoutePath.STATUS_CONFIRMED

    assert route_paths[1].station_from == st1
    assert route_paths[1].station_to == st2
    assert route_paths[1].status == RoutePath.STATUS_CONFIRMED

    assert route_paths[2].station_from == st2
    assert route_paths[2].station_to == st3
    assert route_paths[2].status == RoutePath.STATUS_CHANGED


def test_bulk_load():
    st0 = create_station(id=100)
    st1 = create_station(id=101)
    st2 = create_station(id=102)

    create_route_path(100, 101)
    create_route_path(101, 102)
    create_route_path(100, 102, RoutePath.STATUS_CHANGED, RoutePath.STATUS_REMOVED)

    segments = [(st0, st1), (st1, st2), (st0, st2), (st2, st0)]
    route_paths = RoutePath.bulk_load(segments)

    assert len(route_paths) == 4

    assert route_paths[0].station_from == st0
    assert route_paths[0].station_to == st1
    assert route_paths[0].status == RoutePath.STATUS_CONFIRMED

    assert route_paths[1].station_from == st1
    assert route_paths[1].station_to == st2
    assert route_paths[1].status == RoutePath.STATUS_CONFIRMED

    assert route_paths[2].station_from == st0
    assert route_paths[2].station_to == st2
    assert route_paths[2].status == RoutePath.STATUS_CHANGED

    assert route_paths[3].station_from == st2
    assert route_paths[3].station_to == st0
    assert route_paths[3].status == RoutePath.STATUS_CHANGED


def test_is_route_paths_confirmed():
    create_station(id=100)
    create_station(id=101)
    create_station(id=102)
    create_station(id=103)

    create_route_path(100, 101)
    create_route_path(101, 102)
    create_route_path(100, 102, RoutePath.STATUS_CHANGED, RoutePath.STATUS_REMOVED)

    route_stations = [
        ('r1', 100),
        ('r1', 101),
        ('r1', 102),

        ('r2', 100),
        ('r2', 102),

        ('r3', 101),
        ('r3', 102),
        ('r3', 103)
    ]

    route_paths = RoutePath.is_route_paths_confirmed(route_stations)

    assert len(route_paths) == 3

    assert route_paths['r1']
    assert not route_paths['r2']
    assert not route_paths['r3']
