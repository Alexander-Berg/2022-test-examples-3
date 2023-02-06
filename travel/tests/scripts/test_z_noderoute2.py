# -*- coding: utf-8 -*-

from __future__ import unicode_literals, absolute_import, division, print_function

import pytest
from hamcrest import assert_that, has_properties

from common.models.transport import TransportType
from common.tester.factories import create_thread, create_station, create_settlement, create_route
from travel.rasp.admin.lib import tmpfiles
from route_search.models import ZNodeRoute2
from travel.rasp.admin.scripts.z_noderoute2 import ZNodeRouteImporter


@tmpfiles.clean_temp
@pytest.mark.dbuser
def test_build_znoderoute():
    station_from = create_station()
    station_to = create_station()
    thread = create_thread(
        t_type=TransportType.HELICOPTER_ID,
        schedule_v1=[
            [None, 0, station_from],
            [10, None, station_to],
        ],
        __={'calculate_noderoute': True}
    )

    importer = ZNodeRouteImporter(partial=True)
    importer.has_suburban_stops = False
    importer.refill_main_table()

    path = list(thread.path)
    assert_that(ZNodeRoute2.objects.get(thread=thread), has_properties(
        route_id=thread.route_id,
        thread_id=thread.id,
        t_type_id=thread.t_type_id,
        settlement_from_id=None,
        station_from_id=station_from.id,
        rtstation_from_id=path[0].id,
        settlement_to=None,
        station_to_id=station_to.id,
        rtstation_to_id=path[1].id,
        good_for_start=True,
        good_for_finish=True
    ))

@tmpfiles.clean_temp
@pytest.mark.dbuser
def test_build_znoderoute_station():
    station_from = create_station()
    station_to = create_station()
    thread = create_thread(
        t_type=TransportType.HELICOPTER_ID,
        schedule_v1=[
            [None, 0, station_from],
            [10, None, station_to],
        ]
    )

    importer = ZNodeRouteImporter(partial=False)
    importer.has_suburban_stops = False
    importer.refill_main_table()

    path = list(thread.path)
    assert_that(ZNodeRoute2.objects.get(thread=thread), has_properties(
        route_id=thread.route_id,
        thread_id=thread.id,
        t_type_id=thread.t_type_id,
        settlement_from_id=None,
        station_from_id=station_from.id,
        rtstation_from_id=path[0].id,
        settlement_to=None,
        station_to_id=station_to.id,
        rtstation_to_id=path[1].id,
        good_for_start=False,
        good_for_finish=False
    ))

@tmpfiles.clean_temp
@pytest.mark.dbuser
def test_build_znoderoute_settlement():
    station_from = create_station(settlement=create_settlement())
    station_to = create_station(settlement=create_settlement())
    thread = create_thread(
        t_type=TransportType.HELICOPTER_ID,
        schedule_v1=[
            [None, 0, station_from],
            [10, None, station_to],
        ]
    )

    importer = ZNodeRouteImporter(partial=False)
    importer.has_suburban_stops = False
    importer.refill_main_table()

    path = list(thread.path)
    assert_that(ZNodeRoute2.objects.get(thread=thread), has_properties(
        route_id=thread.route_id,
        thread_id=thread.id,
        t_type_id=thread.t_type_id,
        settlement_from_id=station_from.settlement.id,
        station_from_id=station_from.id,
        rtstation_from_id=path[0].id,
        settlement_to_id=station_to.settlement.id,
        station_to_id=station_to.id,
        rtstation_to_id=path[1].id,
        good_for_start=True,
        good_for_finish=True
    ))

@tmpfiles.clean_temp
@pytest.mark.dbuser
def test_fix_znoderoute():
    station_from = create_station()
    station_to = create_station()
    settlement_from = create_settlement()
    thread = create_thread(
        schedule_v1=[
            [None, 0, station_from],
            [10, None, station_to],
        ],
        __={'calculate_noderoute': True}
    )

    station_from.settlement = settlement_from
    station_from.save()

    station_to.settlement = settlement_from
    station_to.save()

    stations_with_settlement = [
        (station_from.id, station_from.settlement_id),
        (station_to.id, station_from.settlement_id),
    ]
    importer = ZNodeRouteImporter(partial=True)
    importer.fix_znoderoute('settlement_from_id', 'station_from_id', stations_with_settlement)

    for znoderoute in ZNodeRoute2.objects.filter(thread=thread):
        assert znoderoute.settlement_from_id == settlement_from.id
        assert znoderoute.settlement_to_id is None

@pytest.mark.dbuser
def test_clean_znoderoute_partial():
    create_thread_noderoute = create_thread.mutate(__={'calculate_noderoute': True})
    changed_thread1 = create_thread_noderoute(changed=True, path_and_time_unchanged=False)
    changed_thread2 = create_thread_noderoute(changed=True, path_and_time_unchanged=False)
    changed_thread3 = create_thread_noderoute(changed=True, path_and_time_unchanged=False)

    un_changed_thread = create_thread_noderoute(changed=False)
    thread_with_changed_route1 = create_thread_noderoute(route=create_route(hidden=True))
    thread_with_changed_route2 = create_thread_noderoute(route=create_route(hidden=True))

    importer = ZNodeRouteImporter(partial=True)
    importer.clean_znoderoute()

    assert not ZNodeRoute2.objects.filter(thread=changed_thread1).exists()
    assert not ZNodeRoute2.objects.filter(thread=changed_thread2).exists()
    assert not ZNodeRoute2.objects.filter(thread=changed_thread3).exists()
    assert not ZNodeRoute2.objects.filter(thread=thread_with_changed_route1).exists()
    assert not ZNodeRoute2.objects.filter(thread=thread_with_changed_route2).exists()
    assert ZNodeRoute2.objects.filter(thread=un_changed_thread).exists()
