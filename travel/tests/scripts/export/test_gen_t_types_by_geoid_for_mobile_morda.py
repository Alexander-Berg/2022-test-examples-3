# -*- coding: utf-8 -*-

import pytest

from travel.avia.library.python.common.models.transport import TransportType
from travel.avia.admin.scripts.export.gen_t_types_by_geoid_for_mobile_morda import generate_t_types_by_geoid
from travel.avia.library.python.tester.factories import create_settlement, create_thread, create_station


@pytest.mark.dbuser
@pytest.mark.parametrize('has_train,has_suburban', [
    [True, True],
    [True, False],
    [False, True],
])
def test_add_suburban_suburanban_only(has_train, has_suburban):
    settlement = create_settlement(_geo_id=200)
    if has_suburban:
        create_thread(
            t_type=TransportType.SUBURBAN_ID,
            schedule_v1=[
                [None, 0, create_station(t_type=TransportType.TRAIN_ID, settlement=settlement)],
                [10, None],
        ])

    if has_train:
        create_thread(
            t_type=TransportType.TRAIN_ID,
            schedule_v1=[
                [None, 0, create_station(t_type=TransportType.TRAIN_ID, settlement=settlement)],
                [10, None],
            ])

    data = generate_t_types_by_geoid()

    assert data[200]['suburban'] == has_suburban
    assert data[200]['train'] == has_train
