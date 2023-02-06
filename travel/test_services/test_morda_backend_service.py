from datetime import datetime

import pytest

from travel.rasp.pathfinder_maps.const import MSK_TZ
from travel.rasp.pathfinder_maps.models.variant import Variant
from travel.rasp.pathfinder_maps.services.morda_backend_service import MordaBackendService
from travel.rasp.pathfinder_maps.tests.utils import MordaBackendClientStub, create_route
from travel.rasp.pathfinder_maps.tests.utils.fixtures import (  # noqa: F401
    ekb_station, moscow_station, saratov_station, protobuf_data_provider
)


@pytest.mark.asyncio
async def test_morda_backend_service_search(ekb_station, moscow_station, saratov_station, protobuf_data_provider):  # noqa: F811
    values = 'station', 2000005, 'settlement', 54, datetime(2020, 2, 7, 20, 31, 53, tzinfo=MSK_TZ)
    keys = 'from_type', 'from_id', 'to_type', 'to_id', 'date'
    query = dict(zip(keys, values))

    thread_info_1 = [578026383, '047Й', 1, (2000005, 9623135), [2000005, 9623135]]
    thread_info_2 = [597522912, '105Ж', 1, (9623135, 9607404), [9623135, 9607404]]

    client = MordaBackendClientStub({
        values: [
            Variant([
                create_route(
                    '047J_0_2',
                    datetime(2020, 2, 8, 13, 44), 2000005,
                    datetime(2020, 2, 9, 5, 29), 9623135,
                    thread_info_1
                ),
                create_route(
                    '105ZH_1_2',
                    datetime(2020, 2, 9, 7, 0), 9623135,
                    datetime(2020, 2, 10, 14, 20), 9607404,
                    thread_info_2
                )
            ])]
    })

    morda_backend_service = MordaBackendService(client, protobuf_data_provider)

    variants = await morda_backend_service.search(query)
    assert len(variants) == 1

    routes = variants[0].routes
    assert len(routes) == 3

    reference_stations = [
        [moscow_station, saratov_station],
        [saratov_station, saratov_station],
        [saratov_station, ekb_station]
    ]
    res_stations = [[x.departure_station, x.arrival_station] for x in routes]

    for (reference_arrival, reference_departure), (res_arrival, res_departure) in zip(reference_stations, res_stations):
        assert res_arrival == reference_arrival
        assert res_departure == reference_departure
