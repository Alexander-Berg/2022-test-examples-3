# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

from contextlib import contextmanager

import hamcrest
import mock
import pytest

from common.tester.factories import create_direction, create_station, create_thread
from travel.rasp.wizards.suburban_wizard_api.lib.schedule_cache import schedule_cache
from travel.rasp.wizards.suburban_wizard_api.lib.station.suburban_directions_cache import (
    ALL_DIRECTION, ARRIVAL_DIRECTION, DEPARTURE_DIRECTION, SuburbanDirection, SuburbanDirectionsCache
)
from travel.rasp.wizards.wizard_lib.station.direction_type import DirectionType

pytestmark = pytest.mark.dbuser


@contextmanager
def using_precache():
    with schedule_cache.using_precache(), SuburbanDirectionsCache.using_precache():
        yield


def test_unknown_station():
    station = create_station()
    with using_precache():
        assert SuburbanDirectionsCache.list_directions_with_counts(station) == []


def test_station_without_directions():
    station = create_station(use_direction=None)
    create_thread(t_type='suburban', schedule_v1=[
        [None, 0, station],
        [10, None],
    ])

    with using_precache():
        assert SuburbanDirectionsCache.list_directions_with_counts(station) == []


def test_station_with_dirs():
    station = create_station(use_direction='dir')
    direction_1 = create_direction(code='direction_1')
    direction_2 = create_direction(code='direction_2')
    create_thread(t_type='suburban', schedule_v1=[
        [None, 0, station, dict(id=1)],
        [10, None],
    ])
    create_thread(t_type='suburban', schedule_v1=[
        [None, 0, station, dict(id=2, departure_direction=direction_1)],
        [10, None],
    ])
    create_thread(t_type='suburban', schedule_v1=[
        [None, 0, station, dict(id=3, departure_direction=direction_2)],
        [10, None],
    ])
    create_thread(t_type='suburban', schedule_v1=[
        [None, 0],
        [10, None, station, dict(id=4)],
    ])
    segment_1 = mock.Mock(**{'event_stop.id': 1})
    segment_2 = mock.Mock(**{'event_stop.id': 2})
    segment_3 = mock.Mock(**{'event_stop.id': 3})
    segment_4 = mock.Mock(**{'event_stop.id': 4})

    with using_precache():
        hamcrest.assert_that(
            SuburbanDirectionsCache.list_directions_with_counts(station),
            hamcrest.contains_inanyorder(
                (DEPARTURE_DIRECTION, 1),
                (SuburbanDirection(DirectionType.DIR, 'direction_1'), 1),
                (SuburbanDirection(DirectionType.DIR, 'direction_2'), 1),
                (ARRIVAL_DIRECTION, 1),
                (ALL_DIRECTION, 4),
            )
        )

        assert tuple(
            SuburbanDirectionsCache.iter_raw_segments_with_directions([segment_1, segment_2, segment_3, segment_4])
        ) == (
            (segment_1, DEPARTURE_DIRECTION),
            (segment_2, SuburbanDirection(DirectionType.DIR, 'direction_1')),
            (segment_3, SuburbanDirection(DirectionType.DIR, 'direction_2')),
            (segment_4, ARRIVAL_DIRECTION),
        )


def test_station_with_poor_dirs():
    station = create_station(use_direction='dir')
    direction = create_direction(code='direction')
    create_thread(t_type='suburban', schedule_v1=[
        [None, 0, station, dict(id=1, departure_direction=direction, departure_subdir='на Юг')],
        [10, None],
    ])
    create_thread(t_type='suburban', schedule_v1=[
        [None, 0, station, dict(id=2, departure_direction=direction, departure_subdir='на Север')],
        [10, None],
    ])
    create_thread(t_type='suburban', schedule_v1=[
        [None, 0],
        [10, None, station, dict(id=3)],
    ])
    segment_1 = mock.Mock(**{'event_stop.id': 1})
    segment_2 = mock.Mock(**{'event_stop.id': 2})
    segment_3 = mock.Mock(**{'event_stop.id': 3})

    with using_precache():
        hamcrest.assert_that(
            SuburbanDirectionsCache.list_directions_with_counts(station),
            hamcrest.contains_inanyorder(
                (SuburbanDirection(DirectionType.SUBDIR, 'на Юг'), 1),
                (SuburbanDirection(DirectionType.SUBDIR, 'на Север'), 1),
                (ARRIVAL_DIRECTION, 1),
                (ALL_DIRECTION, 3),
            )
        )

        assert tuple(
            SuburbanDirectionsCache.iter_raw_segments_with_directions([segment_1, segment_2, segment_3])
        ) == (
            (segment_1, SuburbanDirection(DirectionType.SUBDIR, 'на Юг')),
            (segment_2, SuburbanDirection(DirectionType.SUBDIR, 'на Север')),
            (segment_3, ARRIVAL_DIRECTION),
        )


def test_station_with_subdirs():
    station = create_station(use_direction='subdir')
    create_thread(t_type='suburban', schedule_v1=[
        [None, 0, station, dict(id=1, departure_subdir='на Юг')],
        [10, None],
    ])
    create_thread(t_type='suburban', schedule_v1=[
        [None, 0, station, dict(id=2, departure_subdir='на Север')],
        [10, None],
    ])
    create_thread(t_type='suburban', schedule_v1=[
        [None, 0, station, dict(id=3, departure_subdir=None)],
        [10, None],
    ])
    create_thread(t_type='suburban', schedule_v1=[
        [None, 0],
        [10, None, station, dict(id=4)],
    ])
    segment_1 = mock.Mock(**{'event_stop.id': 1})
    segment_2 = mock.Mock(**{'event_stop.id': 2})
    segment_3 = mock.Mock(**{'event_stop.id': 3})
    segment_4 = mock.Mock(**{'event_stop.id': 4})

    with using_precache():
        hamcrest.assert_that(
            SuburbanDirectionsCache.list_directions_with_counts(station),
            hamcrest.contains_inanyorder(
                (SuburbanDirection(DirectionType.SUBDIR, 'на Юг'), 1),
                (SuburbanDirection(DirectionType.SUBDIR, 'на Север'), 1),
                (ARRIVAL_DIRECTION, 1),
                (ALL_DIRECTION, 4),
            )
        )

        assert tuple(
            SuburbanDirectionsCache.iter_raw_segments_with_directions([segment_1, segment_2, segment_3, segment_4])
        ) == (
            (segment_1, SuburbanDirection(DirectionType.SUBDIR, 'на Юг')),
            (segment_2, SuburbanDirection(DirectionType.SUBDIR, 'на Север')),
            (segment_3, ALL_DIRECTION),
            (segment_4, ARRIVAL_DIRECTION),
        )


def test_missing_all_direction_with_single_direction():
    station = create_station()
    create_thread(t_type='suburban', schedule_v1=[
        [None, 0],
        [10, None, station],
    ])

    with using_precache():
        assert SuburbanDirectionsCache.list_directions_with_counts(station) == [
            (ARRIVAL_DIRECTION, 1),
        ]


def test_all_direction_with_multiple_directions():
    station = create_station()
    create_thread(t_type='suburban', schedule_v1=[
        [None, 0],
        [10, None, station],
    ])
    create_thread(t_type='suburban', schedule_v1=[
        [None, 0, station, dict(departure_subdir='на Юг')],
        [10, None],
    ])

    with using_precache():
        hamcrest.assert_that(
            SuburbanDirectionsCache.list_directions_with_counts(station),
            hamcrest.contains_inanyorder(
                (ARRIVAL_DIRECTION, 1),
                (SuburbanDirection(DirectionType.SUBDIR, 'на Юг'), 1),
                (ALL_DIRECTION, 2),
            )
        )


def test_all_direction_with_hidden_direction():
    station = create_station()
    create_thread(t_type='suburban', schedule_v1=[
        [None, 0],
        [10, None, station],
    ])
    create_thread(t_type='suburban', schedule_v1=[
        [None, 0, station, dict(departure_subdir=None)],
        [10, None],
    ])

    with using_precache():
        assert SuburbanDirectionsCache.list_directions_with_counts(station) == [
            (ARRIVAL_DIRECTION, 1),
            (ALL_DIRECTION, 2),
        ]


def test_directions_order():
    station = create_station()
    create_thread(t_type='suburban', schedule_v1=[
        [None, 0],
        [10, None, station],
    ])
    create_thread(t_type='suburban', schedule_v1=[
        [None, 0, station, dict(departure_subdir='на Юг')],
        [10, None],
    ])
    create_thread(t_type='suburban', schedule_v1=[
        [None, 0, station, dict(departure_subdir='на Юг')],
        [10, None],
    ])

    with using_precache():
        assert SuburbanDirectionsCache.list_directions_with_counts(station) == [
            (SuburbanDirection(DirectionType.SUBDIR, 'на Юг'), 2),
            (ARRIVAL_DIRECTION, 1),
            (ALL_DIRECTION, 3),
        ]


def test_stop_in_station_schedule():
    station = create_station(use_direction='subdir')
    create_thread(t_type='suburban', schedule_v1=[
        [None, 0],
        [10, None, station, dict(id=1, in_station_schedule=True)],
    ])
    create_thread(t_type='suburban', schedule_v1=[
        [None, 0],
        [10, None, station, dict(in_station_schedule=False)],
    ])
    segment_1 = mock.Mock(**{'event_stop.id': 1})

    with using_precache():
        assert SuburbanDirectionsCache.list_directions_with_counts(station) == [
            (ARRIVAL_DIRECTION, 1),
        ]

        assert tuple(
            SuburbanDirectionsCache.iter_raw_segments_with_directions([segment_1])
        ) == (
            (segment_1, ARRIVAL_DIRECTION),
        )
