# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest
from hamcrest import assert_that, has_entries, contains_inanyorder, has_properties

from common.models.geo import Station2Settlement, StationMajority
from common.models.schedule import RTStation
from common.tester.factories import create_station, create_settlement, create_thread
from common.utils.settlement import fetch_station_settlement_ids, fill_best_rts_for_settlement, \
    fetch_station_settlement_ids_with_none


@pytest.mark.dbuser
def test_fetch_station_settlement_ids():
    station = create_station(settlement=create_settlement())
    settlement = create_settlement()
    Station2Settlement.objects.create(station=station, settlement=settlement)
    station_without_settlement = create_station()

    station_settlement_ids = fetch_station_settlement_ids()

    assert_that(station_settlement_ids, has_entries({
        station.id: contains_inanyorder(station.settlement.id, settlement.id),
    }))
    assert station_without_settlement not in station_settlement_ids


@pytest.mark.dbuser
def test_fetch_station_settlement_ids_with_none():
    station = create_station(settlement=create_settlement())
    settlement = create_settlement()
    Station2Settlement.objects.create(station=station, settlement=settlement)
    station_without_settlement = create_station()

    station_settlement_ids = fetch_station_settlement_ids_with_none()

    assert_that(station_settlement_ids, has_entries({
        station.id: contains_inanyorder(station.settlement.id, settlement.id),
        station_without_settlement.id: contains_inanyorder(None),
    }))


@pytest.mark.dbuser
def test_fill_best_rts_for_settlement():
    settlement = create_settlement()
    settlement2 = create_settlement()
    stations = [create_station(majority=StationMajority.IN_TABLO_ID),
                create_station(settlement=settlement, majority=StationMajority.IN_TABLO_ID),
                create_station(settlement=settlement, majority=StationMajority.MAIN_IN_CITY_ID),
                create_station(settlement=settlement, majority=StationMajority.IN_TABLO_ID),
                create_station(settlement=settlement, majority=StationMajority.MAIN_IN_CITY_ID),
                create_station(majority=StationMajority.IN_TABLO_ID),
                create_station(settlement=settlement, majority=StationMajority.IN_TABLO_ID),
                create_station(settlement=settlement2, majority=StationMajority.IN_TABLO_ID)]
    settlement3 = create_settlement()
    Station2Settlement.objects.create(station=stations[-1], settlement=settlement3)
    thread = create_thread(
        schedule_v1=[
            [None, 0, stations[0]],
            [10, 20, stations[1]],
            [30, 40, stations[2]],
            [50, 60, stations[3]],
            [70, 80, stations[4]],
            [90, 100, stations[5]],
            [110, 120, stations[6]],
            [130, None, stations[7]],
        ]
    )
    rts_from, rts_to = fill_best_rts_for_settlement(RTStation.objects.filter(thread=thread))
    assert_that(rts_from, has_entries({
        settlement.id: has_properties(id=RTStation.objects.get(thread=thread, station=stations[4]).id),
    }))
    assert_that(rts_to, has_entries({
        settlement.id: has_properties(id=RTStation.objects.get(thread=thread, station=stations[2]).id),
        settlement2.id: has_properties(id=RTStation.objects.get(thread=thread, station=stations[-1]).id),
        settlement3.id: has_properties(id=RTStation.objects.get(thread=thread, station=stations[-1]).id),
    }))
