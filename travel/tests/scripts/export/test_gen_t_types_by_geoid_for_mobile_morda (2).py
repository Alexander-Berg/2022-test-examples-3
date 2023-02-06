# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest
import mock
from hamcrest import assert_that, has_entries

from common.models.geo import CityMajority
from common.models.transport import TransportType
from travel.rasp.admin.scripts.export.gen_t_types_by_geoid_for_mobile_morda import generate_t_types_by_geoid
from tester.factories import create_settlement, create_thread, create_station, create_region


pytestmark = [pytest.mark.dbuser]


def create_settlement_with_t_types(t_types, *args, **kwargs):
    settlement = create_settlement(*args, **kwargs)
    for t_type in t_types:
        create_station(settlement=settlement, t_type=t_type)

    return settlement


@mock.patch('travel.rasp.admin.scripts.export.gen_t_types_by_geoid_for_mobile_morda.FORCE_PLANE_REGIONS', {4242})
def test_force_plane_regions():
    region_forced, region_not_forced = create_region(_geo_id=4242), create_region(_geo_id=123)

    create_settlement_with_t_types(
        _geo_id=200, region=region_forced, t_types=[TransportType.BUS_ID, TransportType.WATER_ID]
    )
    create_settlement_with_t_types(
        _geo_id=201, region=region_not_forced, t_types=[TransportType.PLANE_ID, TransportType.BUS_ID]
    )
    create_settlement_with_t_types(
        _geo_id=202, region=region_not_forced, t_types=[TransportType.BUS_ID, TransportType.WATER_ID]
    )

    data = generate_t_types_by_geoid()
    assert_that(data, has_entries({
        # forced plane
        200: {
             'train': False,
             'suburban': False,
             'plane': True,
             'bus': True,
             'water': True,
        },
        # not forced plane, but has station with plane
        201: {
            'train': False,
            'suburban': False,
            'plane': True,
            'bus': True,
            'water': False,
        },
        # no plane
        202: {
            'train': False,
            'suburban': False,
            'plane': False,
            'bus': True,
            'water': True,
        }
    }))


def test_region_t_types():
    region1, region2, region3 = create_region(_geo_id=101), create_region(_geo_id=102), create_region(_geo_id=103)

    create_settlement_with_t_types(
        _geo_id=200, region=region1, majority=CityMajority.CAPITAL_ID,
        t_types=[TransportType.BUS_ID, TransportType.WATER_ID]
    )
    create_settlement_with_t_types(
        _geo_id=201, region=region2, majority=CityMajority.REGION_CAPITAL_ID,
        t_types=[TransportType.PLANE_ID]
    )
    create_settlement_with_t_types(
        _geo_id=202, region=region3, majority=CityMajority.POPULATION_MILLION_ID,
        t_types=[TransportType.PLANE_ID]
    )

    data = generate_t_types_by_geoid()

    assert_that(data, has_entries({
        # t_types from capital
        101: {
            'train': False,
            'suburban': False,
            'plane': False,
            'bus': True,
            'water': True,
        },
        # t_types from region capital
        102: {
            'train': False,
            'suburban': False,
            'plane': True,
            'bus': False,
            'water': False,
        }
    }))

    # no capital - no t_types
    assert 103 not in data


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
