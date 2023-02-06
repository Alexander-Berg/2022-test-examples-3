from datetime import datetime

import pytest

from travel.rasp.pathfinder_maps.const import UTC_TZ
from travel.rasp.pathfinder_maps.models.variant import Variant
from travel.rasp.pathfinder_maps.models.route import Route
from travel.rasp.pathfinder_maps.services.protobuf_builder import ProtobufBuilder
from travel.rasp.pathfinder_maps.tests.utils.fixtures import (  # noqa: F401
    maps_client, pathfinder_protobuf, pickle_data_provider, protobuf_data_provider,
    moscow_station, saratov_station, ekb_station
)


@pytest.mark.asyncio
async def test_build_pathfinder_protobuf(
    maps_client, pickle_data_provider, protobuf_data_provider,         # noqa: F811
    moscow_station, saratov_station, ekb_station, pathfinder_protobuf  # noqa: F811
):
    pb = ProtobufBuilder(protobuf_data_provider, pickle_data_provider, maps_client, 'rasp_link')
    stations = [
        (moscow_station, saratov_station),
        (saratov_station, saratov_station),
        (saratov_station, ekb_station)
    ]
    threads = [
        (578026383, '047Й', 1, (2000005, 9623135), [2000005, 9623135]),
        None,
        (597522912, '105Ж', 1, (9623135, 9607404), [9623135, 9607404])
    ]

    variant = Variant([
        Route(
            '047J_0_2',
            datetime(2020, 2, 8, 10, 44, tzinfo=UTC_TZ), 2000005,
            datetime(2020, 2, 9, 2, 29, tzinfo=UTC_TZ), 9623135
        ),
        Route(
            'NULL',
            datetime(2020, 2, 9, 2, 32, tzinfo=UTC_TZ), 9623135,
            datetime(2020, 2, 10, 1, 32, tzinfo=UTC_TZ), 9623135
        ),
        Route(
            '105ZH_1_2',
            datetime(2020, 2, 9, 4, 0, tzinfo=UTC_TZ), 9623135,
            datetime(2020, 2, 10, 11, 20, tzinfo=UTC_TZ), 9607404
        )
    ])

    for route, (departure_station, arrival_station), thread in zip(variant.routes, stations, threads):
        route.departure_station = departure_station
        route.arrival_station = arrival_station
        route.thread_info = thread

    res_points, res_sections = await pb.build_pathfinder_protobuf(variant)
    real_points, real_sections = pathfinder_protobuf

    assert res_points == real_points

    for x, y in zip(res_sections, real_sections):
        assert x.SerializeToString() == y.SerializeToString()
