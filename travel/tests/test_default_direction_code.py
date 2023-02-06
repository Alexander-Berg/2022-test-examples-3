# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import mock
import pytest

from common.tester.factories import create_direction, create_station
from common.tester.transaction_context import transaction_fixture
from stationschedule.type.suburban import SuburbanSchedule


@pytest.fixture(scope='module')
@transaction_fixture
def stations(request):
    station = create_station(use_direction='subdir', t_type='suburban')

    direction = create_direction(title_from=u'от Города', title_to=u'на Город')
    dir_station = create_station(use_direction='subdir', t_type='suburban',
                                 __={'direction': direction})
    dir_airport = create_station(use_direction='subdir', t_type='plane',
                                 __={'direction': direction})

    return (station, dir_station, dir_airport)


@mock.patch.object(SuburbanSchedule, 'direction_code_title_count_list',
                   new_callable=mock.PropertyMock)
@pytest.mark.dbuser
def test_default_direction_code(directions_mock, stations):
    station, dir_station, dir_airport = stations
    direction = dir_station.get_direction()

    # станция с рейсами без направлений
    directions_mock.return_value = []
    assert SuburbanSchedule(station).direction_code is None

    # станция без направления
    directions_mock.return_value = [
        (SuburbanSchedule.DIRECTION_ARRIVAL, 'Arrival', 100),
        ('some_2', 'Direction with 2 threads', 2),
        ('some_1', 'Direction with 1 thread', 1),
    ]
    assert SuburbanSchedule(station).direction_code == 'some_2'

    directions_mock.return_value = [
        (SuburbanSchedule.DIRECTION_ARRIVAL, 'Arrival', 100),
    ]
    assert SuburbanSchedule(station).direction_code == SuburbanSchedule.DIRECTION_ARRIVAL

    # аэропорт с направлением
    directions_mock.return_value = [
        ('some_100', 'Direction with 100 threads', 100),
        ('to', direction.title_to, 1),
    ]
    assert SuburbanSchedule(dir_airport).direction_code == 'to'

    directions_mock.return_value = [
        (SuburbanSchedule.DIRECTION_ARRIVAL, 'Arrival', 100),
        ('from', direction.title_from, 100),
        ('some_2', 'Direction with 2 threads', 2),
        ('some_1', 'Direction with 1 thread', 1),
    ]
    assert SuburbanSchedule(dir_airport).direction_code == 'some_2'

    directions_mock.return_value = [
        (SuburbanSchedule.DIRECTION_ARRIVAL, 'Arrival', 100),
        ('from', direction.title_from, 1),
    ]
    assert SuburbanSchedule(dir_airport).direction_code == 'from'

    directions_mock.return_value = [
        (SuburbanSchedule.DIRECTION_ARRIVAL, 'Arrival', 100),
    ]
    assert SuburbanSchedule(dir_airport).direction_code == SuburbanSchedule.DIRECTION_ARRIVAL

    # станция с направлением
    directions_mock.return_value = [
        ('some_100', 'Direction with 100 threads', 100),
        ('from', direction.title_from, 1),
    ]
    assert SuburbanSchedule(dir_station).direction_code == 'from'

    directions_mock.return_value = [
        (SuburbanSchedule.DIRECTION_ARRIVAL, 'Arrival', 100),
        ('to', direction.title_to, 100),
        ('some_2', 'Direction with 2 threads', 2),
        ('some_1', 'Direction with 1 thread', 1),
    ]
    assert SuburbanSchedule(dir_station).direction_code == 'some_2'

    directions_mock.return_value = [
        (SuburbanSchedule.DIRECTION_ARRIVAL, 'Arrival', 100),
        ('to', direction.title_to, 1),
    ]
    assert SuburbanSchedule(dir_station).direction_code == 'to'

    directions_mock.return_value = [
        (SuburbanSchedule.DIRECTION_ARRIVAL, 'Arrival', 100),
    ]
    assert SuburbanSchedule(dir_station).direction_code == SuburbanSchedule.DIRECTION_ARRIVAL
