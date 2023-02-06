# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest

from common.models.transport import TransportType
from common.tester.factories import create_settlement, create_thread, create_station
from travel.rasp.trains.scripts.dump_actual_directions.dump_actual_directions import Runner

pytestmark = [pytest.mark.dbuser]


def create_segment_thread(station_from, station_to, t_type=TransportType.TRAIN_ID):
    return create_thread(
        __={'calculate_noderoute': True},
        t_type=t_type,
        schedule_v1=[
            [None, 0, station_from],
            [10, None, station_to],
        ]
    )


def test_fetch_rotes():
    from_settlement = create_settlement(slug='from_city')
    to_settlement = create_settlement(slug='to_city')
    from_station = create_station(title='from_station', slug='from_station', settlement=from_settlement, id=1)
    to_station = create_station(title='to_station', slug='to_station', settlement=to_settlement, id=2)

    create_segment_thread(from_station, to_station, t_type=TransportType.TRAIN_ID)
    create_segment_thread(from_station, to_station, t_type=TransportType.SUBURBAN_ID)
    create_segment_thread(from_station, to_station, t_type=TransportType.BUS_ID)
    create_segment_thread(from_station, to_station, t_type=TransportType.WATER_ID)
    create_segment_thread(from_station, to_station, t_type=TransportType.PLANE_ID)

    routes = list(Runner('proxy', 'token').get_directions())

    assert routes == [
        {'station_id_from': 1, 'station_id_to': 2},
    ]
