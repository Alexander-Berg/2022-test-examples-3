# -*- coding: utf-8 -*-
import pytest
from hamcrest import assert_that, contains_inanyorder, has_properties

from common.models.geo import StationMajority
from common.tester.factories import create_station, create_settlement, create_thread

from route_search.base import get_threads_from_znoderoute


@pytest.mark.dbuser
def test_get_threads_from_znoderoute():
    settlement_to = create_settlement()
    settlement_from = create_settlement()
    station_from_1 = create_station(settlement=settlement_from, majority=StationMajority.IN_TABLO_ID)
    station_from_2 = create_station(settlement=settlement_from, majority=StationMajority.MAIN_IN_CITY_ID)
    station_to_1 = create_station(settlement=settlement_to, majority=StationMajority.MAIN_IN_CITY_ID)
    station_to_2 = create_station(settlement=settlement_to, majority=StationMajority.IN_TABLO_ID)

    create_thread(
        uid=1, supplier={'id': 1},
        schedule_v1=[
            [None, 0, station_from_1],
            [10, 20, station_from_2],
            [30, 40, station_to_1],
            [50, None, station_to_2],
        ],
        __={'calculate_noderoute': True}
    )

    threads = get_threads_from_znoderoute(settlement_from, settlement_to)
    assert_that(threads, contains_inanyorder(
        has_properties(station_from_id=station_from_1.id, station_to_id=station_to_1.id),
        has_properties(station_from_id=station_from_1.id, station_to_id=station_to_2.id),
        has_properties(station_from_id=station_from_2.id, station_to_id=station_to_1.id),
        has_properties(station_from_id=station_from_2.id, station_to_id=station_to_2.id),
    ))
