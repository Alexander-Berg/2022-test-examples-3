# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

import pytest

from common.tester.factories import create_settlement, create_station
from geosearch.models import NameSearchIndex
from geosearch.views.pointtopoint import POINT_REPLACEMENTS
from travel.rasp.wizards.proxy_api.lib.direction.points import find_points, PointNotFound, PointsAreInvalid

pytestmark = pytest.mark.dbuser


@pytest.mark.parametrize('args', (
    (None, 'Отправление', None, 'Прибытие'),

    # geoid has higher priority only when it is valid
    ('99', 'Отправление', '99', 'Прибытие'),
    ('99', 'Отправление', '2', 'unknown'),
    ('1', 'unknown', '99', 'Прибытие'),
    ('1', 'unknown', '2', 'unknown'),
))
def test_find_points(args):
    departure_settlement = create_settlement(_geo_id=1, title_ru='Отправление')
    arrival_settlement = create_settlement(_geo_id=2, title_ru='Прибытие')
    NameSearchIndex.objects.bulk_create(NameSearchIndex.get_records())

    assert find_points(*args) == (departure_settlement, arrival_settlement)


@pytest.mark.parametrize('args', (
    (None, 'unknown', None, 'Город'),
    (None, 'СкрытыйГород', None, 'Город'),
    (None, 'Город', None, 'unknown'),
    (None, 'Город', None, 'СкрытыйГород'),
    ('1', None, '99', None),
    ('1', None, '2', None),
    ('99', None, '1', None),
    ('2', None, '1', None),
))
def test_find_points_notfound(args):
    create_settlement(_geo_id=1, title_ru='Город')
    create_settlement(_geo_id=2, title_ru='СкрытыйГород', hidden=True)
    NameSearchIndex.objects.bulk_create(NameSearchIndex.get_records())

    with pytest.raises(PointNotFound):
        find_points(*args)


def test_find_points_samepointerror():
    settlement = create_settlement(title_ru='Город')
    create_station(
        id=1,
        title_ru='Станция',
        settlement=settlement,
        t_type='train',
        __={'ext_directions': [{'code': 'grd_dir'}]}
    )
    POINT_REPLACEMENTS.append(['город', 'grd_dir', 1])
    NameSearchIndex.objects.bulk_create(NameSearchIndex.get_records())

    with pytest.raises(PointsAreInvalid):
        find_points(None, 'Станция', None, 'Город')

    with pytest.raises(PointsAreInvalid):
        find_points(None, 'Город', None, 'Станция')

    POINT_REPLACEMENTS.pop()


@pytest.mark.parametrize('args', (
    (None, 'Город', None, 'Город'),
    ('1', None, '1', None),
))
def test_find_points_invalid(args):
    create_settlement(_geo_id=1, title_ru='Город')
    NameSearchIndex.objects.bulk_create(NameSearchIndex.get_records())

    with pytest.raises(PointsAreInvalid):
        find_points(*args)


def test_find_points_with_client_geoid():
    client_city = create_settlement(_geo_id=1, region={})
    major_station = create_station(title_ru='Название А')
    local_station = create_station(title_ru='Название А', majority='station', region=client_city.region)
    station = create_station(title_ru='Название Б')
    NameSearchIndex.objects.bulk_create(NameSearchIndex.get_records())

    assert find_points(None, 'Название А', None, 'Название Б', client_geoid=None) == (major_station, station)
    assert find_points(None, 'Название А', None, 'Название Б', client_geoid=1) == (local_station, station)
    assert find_points(None, 'Название Б', None, 'Название А', client_geoid=1) == (station, local_station)


def test_find_points_with_transport_code():
    departure_settlement = create_settlement(title_ru='Название А', type_choices='suburban')
    departure_station = create_station(title_ru='Название А', t_type='train', type_choices='suburban')
    arrival_station = create_station(title_ru='Название Б', t_type='train', type_choices='suburban')
    NameSearchIndex.objects.bulk_create(NameSearchIndex.get_records())

    assert find_points(
        None, 'Название А', None, 'Название Б', transport_code=None
    ) == (departure_settlement, arrival_station)

    assert find_points(
        None, 'Название А', None, 'Название Б', transport_code='suburban'
    ) == (departure_station, arrival_station)

    with pytest.raises(PointNotFound):
        assert find_points(
            None, 'Название А', None, 'Название Б', transport_code='bus'
        )
