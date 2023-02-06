from datetime import datetime

import pytest
from asynctest import patch, CoroutineMock

from travel.rasp.pathfinder_maps.clients.morda_backend_client import MordaBackendClient
from travel.rasp.pathfinder_maps.const import UTC_TZ
from travel.rasp.pathfinder_maps.models.route import Route


reference_patch = {
    'variants': [
        {
            'segments': [
                {
                    'departure': '2020-02-08T13:44:00+00:00',
                    'arrival': '2020-02-09T05:29:00+00:00',
                    'departure_station': {'id': 2000005},
                    'arrival_station': {'id': 9623135},
                    'thread': {
                        'title': '',
                        'uid': '047J_0_2',
                        'number': '047Й',
                        'full_path': [
                            2000005,
                            101,
                            102,
                            9623135
                        ]
                    },
                    'transport': {'id': 1}
                },
                {
                    'departure': '2020-02-09T07:00:00+00:00',
                    'arrival': '2020-02-10T14:20:00+00:00',
                    'departure_station': {'id': 9623135},
                    'arrival_station': {'id': 9607404},
                    'thread': {
                        'title': '',
                        'uid': '105ZH_1_2',
                        'number': '105Ж'
                    },
                    'transport': {'id': 2}
                }
            ]
        }
    ]
}

empty_patch = {'variants': []}


@pytest.mark.asyncio
async def test_morda_backend_client_real():
    with patch(
            'travel.rasp.pathfinder_maps.clients.morda_backend_client.MordaBackendClient._get_variants',
            CoroutineMock(return_value=reference_patch)
    ):
        morda_backend_client = MordaBackendClient('')
        variants = await morda_backend_client.get_pm_variants(
            'station', 2000005, 'settlement', 54, datetime(2020, 2, 7, 20, 31, 53), [1, 3]
        )

    assert len(variants) == 1

    routes = variants[0].routes
    assert len(routes) == 2

    route1 = Route(
        '047J_0_2',
        datetime(2020, 2, 8, 13, 44, tzinfo=UTC_TZ), 2000005,
        datetime(2020, 2, 9, 5, 29, tzinfo=UTC_TZ), 9623135
    )
    route1.thread_info = ['047J_0_2', '047Й', 1, [2000005, 101, 102, 9623135], '']

    route2 = Route(
        '105ZH_1_2',
        datetime(2020, 2, 9, 7, 0, tzinfo=UTC_TZ), 9623135,
        datetime(2020, 2, 10, 14, 20, tzinfo=UTC_TZ), 9607404
    )
    route2.thread_info = ['105ZH_1_2', '105Ж', 2, [], '']

    reference_routes = [route1, route2]

    for route, reference_route in zip(routes, reference_routes):
        assert route == reference_route


@pytest.mark.asyncio
async def test_morda_backend_client_empty():
    with patch(
            'travel.rasp.pathfinder_maps.clients.morda_backend_client.MordaBackendClient._get_variants',
            CoroutineMock(return_value=empty_patch)
    ):
        morda_backend_client = MordaBackendClient('')
        variants = await morda_backend_client.get_pm_variants(
            'station', 2000005, 'settlement', 54, '2020-02-07 20:31:53', [3]
        )

    assert len(variants) == 0
