# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

import itertools
from datetime import date, datetime, time

import hamcrest
import pytest

from common.tester.factories import create_station, create_thread
from travel.rasp.wizards.suburban_wizard_api.lib.schedule_cache import schedule_cache
from travel.rasp.wizards.suburban_wizard_api.lib.station.segments_search import (
    find_date_segments, find_directions_with_segments, find_hacked_future_segments, find_future_segments,
    split_segments_by_directions
)
from travel.rasp.wizards.suburban_wizard_api.lib.station.suburban_directions_cache import (
    ALL_DIRECTION, SuburbanDirection, SuburbanDirectionsCache
)
from travel.rasp.wizards.wizard_lib.station.direction_type import DirectionType

pytestmark = pytest.mark.dbuser


def test_find_date_segments(fixed_now):
    station = create_station()
    thread = create_thread(t_type='suburban', schedule_v1=[
        [None, 0, station],
        [10, None],
    ])

    with schedule_cache.using_precache():
        hamcrest.assert_that(find_date_segments(station, date(2000, 6, 1)), hamcrest.contains(
            hamcrest.has_properties(thread=hamcrest.has_properties(id=thread.id), thread_start_dt=datetime(2000, 6, 1))
        ))


def test_find_future_segments(fixed_now):
    station = create_station()
    thread = create_thread(t_type='suburban', schedule_v1=[
        [None, 0, station],
        [10, None],
    ])

    with schedule_cache.using_precache():
        hamcrest.assert_that(itertools.islice(find_future_segments(station), 3), hamcrest.contains(
            hamcrest.has_properties(thread=hamcrest.has_properties(id=thread.id), thread_start_dt=datetime(2000, 1, 1)),
            hamcrest.has_properties(thread=hamcrest.has_properties(id=thread.id), thread_start_dt=datetime(2000, 1, 2)),
            hamcrest.has_properties(thread=hamcrest.has_properties(id=thread.id), thread_start_dt=datetime(2000, 1, 3)),
        ))


def test_find_hacked_future_segments(fixed_now):
    station = create_station(time_zone='Asia/Yekaterinburg')
    thread = create_thread(t_type='suburban', schedule_v1=[
        [None, 0, station],
        [10, None],
    ])

    with schedule_cache.using_precache():
        hamcrest.assert_that(itertools.islice(find_hacked_future_segments(station), 3), hamcrest.contains(
            hamcrest.has_properties(thread=hamcrest.has_properties(id=thread.id), thread_start_dt=datetime(2000, 1, 2)),
            hamcrest.has_properties(thread=hamcrest.has_properties(id=thread.id), thread_start_dt=datetime(2000, 1, 3)),
            hamcrest.has_properties(thread=hamcrest.has_properties(id=thread.id), thread_start_dt=datetime(2000, 1, 4)),
        ))


def test_split_segments_by_directions(fixed_now):
    station = create_station()
    direction_1 = SuburbanDirection(DirectionType.SUBDIR, 'на Юг')
    direction_2 = SuburbanDirection(DirectionType.SUBDIR, 'на Север')
    thread_1 = create_thread(t_type='suburban', schedule_v1=[
        [None, 0, station, dict(departure_subdir=direction_1.code)],
        [10, None],
    ])
    thread_2 = create_thread(t_type='suburban', year_days=[date(2000, 2, 1), date(2000, 2, 2)], schedule_v1=[
        [None, 0, station, dict(departure_subdir=direction_2.code)],
        [10, None],
    ])
    thread_3 = create_thread(t_type='suburban', tz_start_time=time(10), schedule_v1=[
        [None, 0, station, dict(departure_subdir=None)],
        [10, None],
    ])

    with schedule_cache.using_precache(), SuburbanDirectionsCache.using_precache():
        raw_segments_iter = find_future_segments(station)

        assert split_segments_by_directions(raw_segments_iter, [direction_1, direction_2, ALL_DIRECTION]) == {
            direction_1: [
                hamcrest.match_equality(hamcrest.has_properties(
                    thread=hamcrest.has_properties(id=thread_1.id),
                    thread_start_dt=datetime(2000, 1, 1)
                ))
            ],
            direction_2: [
                hamcrest.match_equality(hamcrest.has_properties(
                    thread=hamcrest.has_properties(id=thread_2.id),
                    thread_start_dt=datetime(2000, 2, 1)
                ))
            ],
            ALL_DIRECTION: [
                hamcrest.match_equality(hamcrest.has_properties(
                    thread=hamcrest.has_properties(id=thread_1.id),
                    thread_start_dt=datetime(2000, 1, 1)
                )),
                hamcrest.match_equality(hamcrest.has_properties(
                    thread=hamcrest.has_properties(id=thread_3.id),
                    thread_start_dt=datetime(2000, 1, 1, 10)
                ))
            ]
        }


def test_find_directions_with_segments_of_empty_station():
    station = create_station()
    with schedule_cache.using_precache(), SuburbanDirectionsCache.using_precache():
        assert find_directions_with_segments(station) == ()


def test_find_directions_with_segments_without_event_date(fixed_now):
    station = create_station()
    direction = SuburbanDirection(DirectionType.SUBDIR, 'на Юг')
    thread = create_thread(t_type='suburban', year_days=[date(2000, 2, 1)], schedule_v1=[
        [None, 0, station, dict(departure_subdir=direction.code)],
        [10, None],
    ])
    with schedule_cache.using_precache(), SuburbanDirectionsCache.using_precache():
        hamcrest.assert_that(
            find_directions_with_segments(station),
            hamcrest.contains(
                hamcrest.contains(direction, 1, hamcrest.contains(
                    hamcrest.has_properties(
                        thread=hamcrest.has_properties(id=thread.id),
                        thread_start_dt=datetime(2000, 2, 1)
                    )
                ))
            )
        )


def test_find_directions_with_segments_with_event_date(fixed_now):
    station = create_station()
    direction_1 = SuburbanDirection(DirectionType.SUBDIR, 'на Юг')
    direction_2 = SuburbanDirection(DirectionType.SUBDIR, 'на Север')
    thread = create_thread(t_type='suburban', schedule_v1=[
        [None, 0, station, dict(departure_subdir=direction_1.code)],
        [10, None],
    ])
    create_thread(t_type='suburban', year_days=[date(2000, 1, 2)], schedule_v1=[
        [None, 0, station, dict(departure_subdir=direction_1.code)],
        [10, None],
    ])
    create_thread(t_type='suburban', year_days=[date(2000, 1, 2)], schedule_v1=[
        [None, 0, station, dict(departure_subdir=direction_2.code)],
        [10, None],
    ])

    with schedule_cache.using_precache(), SuburbanDirectionsCache.using_precache():
        assert find_directions_with_segments(station, date(2000, 1, 1)) == (
            (direction_1, 2, [
                hamcrest.match_equality(hamcrest.has_properties(
                    thread=hamcrest.has_properties(id=thread.id),
                    thread_start_dt=datetime(2000, 1, 1)
                ))
            ]),
            (ALL_DIRECTION, 3, [
                hamcrest.match_equality(hamcrest.has_properties(
                    thread=hamcrest.has_properties(id=thread.id),
                    thread_start_dt=datetime(2000, 1, 1)
                ))
            ])
        )
