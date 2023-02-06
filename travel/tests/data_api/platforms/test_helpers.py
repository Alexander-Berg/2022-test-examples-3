# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import date, datetime, time, timedelta

import mock
import pytest

from common.data_api.platforms.client import PlatformsClient, get_dynamic_platform_collection
from common.data_api.platforms.helpers import PathPlatformsBatch, SegmentPlatformsBatch, ScheduleRoutePlatformsBatch
from common.data_api.platforms.serialization import PlatformData, PlatformKey, PlatformRecord
from common.tester.factories import create_thread, create_rtstation, create_rthread_segment, create_station
from common.tests.utils import has_route_search


pytestmark = [pytest.mark.dbuser, pytest.mark.mongouser]


@has_route_search
def test_segment_platforms_batch():
    stations = [create_station(id=i) for i in range(220, 224)]
    dates = [date(2019, 10, 10), date(2019, 10, 11)]
    start_time = time(1, 0)

    thread = create_thread(
        number='number0',
        year_days=[dates[0], dates[1]],
        tz_start_time=start_time,
        schedule_v1=[
            [None, 0, stations[0]],
            [10, 15, stations[1]],
            [50, 55, stations[2]],
            [80, None, stations[3]],
        ],
    )
    rtstations = thread.path_cached

    segments = [
        create_rthread_segment(
            rts_from=rtstations[0],
            departure=datetime.combine(dates[0], time(0)) + timedelta(minutes=rtstations[0].tz_departure),
            rts_to=rtstations[2],
            arrival=datetime.combine(dates[0], time(0)) + timedelta(minutes=rtstations[2].tz_arrival),
            start_date=dates[0],
        ),
        create_rthread_segment(
            rts_from=rtstations[2],
            departure=datetime.combine(dates[1], time(0)) + timedelta(minutes=rtstations[2].tz_departure),
            rts_to=rtstations[3],
            arrival=datetime.combine(dates[1], time(0)) + timedelta(minutes=rtstations[3].tz_arrival),
            start_date=dates[1]
        )
    ]

    platforms_client = PlatformsClient(get_dynamic_platform_collection())
    platforms_client.update([
        PlatformRecord(
            key=PlatformKey(date=dates[0], station_id=stations[0].id, train_number=thread.number),
            data=PlatformData(departure_platform='platform_0d', arrival_platform='platform_0a')
        ),
        PlatformRecord(
            key=PlatformKey(date=dates[1], station_id=stations[2].id, train_number=thread.number),
            data=PlatformData(departure_platform='platform_2d')
        ),
        PlatformRecord(
            key=PlatformKey(date=dates[1], station_id=stations[3].id, train_number=thread.number),
            data=PlatformData(departure_platform='platform_3d', arrival_platform='platform_3a')
        ),
    ])

    batch = SegmentPlatformsBatch()
    batch.try_load(platforms_client, segments)

    assert batch.get_departure(segments[0]) == 'platform_0d'
    assert batch.get_arrival(segments[0]) is None
    assert batch.get_departure(segments[1]) == 'platform_2d'
    assert batch.get_arrival(segments[1]) == 'platform_3a'


def test_path_platforms_batch():
    dates = [date(2019, 10, 10)]
    stations = [create_station(id=i) for i in range(120, 123)]
    thread = create_thread(
        year_days=[dates[0]],
        uid='*thread_uid0*',
        number='1000',
        schedule_v1=[
            [None, 10, stations[0]],
            [15, 20, stations[1]],
            [25, None, stations[2]],
        ],
    )

    platforms_client = PlatformsClient(get_dynamic_platform_collection())
    platforms_client.update([
        PlatformRecord(
            key=PlatformKey(date=dates[0], station_id=stations[0].id, train_number=thread.number),
            data=PlatformData(departure_platform='platform_0d', arrival_platform='platform_0a')
        ),
        PlatformRecord(
            key=PlatformKey(date=dates[0], station_id=stations[1].id, train_number=thread.number),
            data=PlatformData(departure_platform='platform_1d')
        ),
        PlatformRecord(
            key=PlatformKey(date=dates[0], station_id=stations[2].id, train_number=thread.number),
            data=PlatformData(departure_platform='platform_2d', arrival_platform='platform_2a')
        ),
    ])

    path = list(thread.path)
    batch = PathPlatformsBatch(datetime.combine(dates[0], time(1, 0)), thread.number)
    batch.try_load(platforms_client, path)

    assert batch.get_platform(path[0]) == 'platform_0d'
    assert batch.get_platform(path[1]) == 'platform_1d'
    assert batch.get_platform(path[2]) == 'platform_2a'


def test_schedule_route_platforms_batch():
    dates = [date(2019, 10, 10), date(2019, 10, 11)]
    stations = [create_station(id=i) for i in range(120, 122)]
    threads = [
        create_thread(
            year_days=[dates[0]],
            uid='*thread_uid0*',
            number='1000',
            schedule_v1=[
                [None, 10, stations[0]],
                [11, None]
            ],
        ),
        create_thread(  # arrival on dates[0], departure on dates[1]
            year_days=[dates[0]],
            uid='*thread_uid1*',
            number='1001',
            tz_start_time=time(23, 00),
            schedule_v1=[
                [None, 55, stations[1]],
                [65, None]
            ],
        ),
    ]
    schedule_routes = [
        mock.Mock(
            thread=threads[0],
            rtstation=create_rtstation(thread=threads[0], station=stations[0]),
            event_dt=datetime.combine(dates[0], time(10, 10)),
            naive_start_dt=datetime.combine(dates[0], time(1, 0)),
            arrival_dt=datetime.combine(dates[0], time(10, 5)),
            departure_dt=datetime.combine(dates[0], time(10, 10))
        ),
        mock.Mock(  # arrival on dates[0], departure on dates[1]
            thread=threads[1],
            rtstation=create_rtstation(thread=threads[1], station=stations[1]),
            event_dt=datetime.combine(dates[1], time(0, 5)),
            naive_start_dt=datetime.combine(dates[0], time(23, 0)),
            arrival_dt=datetime.combine(dates[0], time(23, 55)),
            departure_dt=datetime.combine(dates[1], time(0, 5))
        ),
        mock.Mock(  # arrival on dates[1], no departure
            thread=threads[1],
            rtstation=create_rtstation(thread=threads[1], station=stations[0]),
            event_dt=datetime.combine(dates[1], time(0, 5)),
            naive_start_dt=datetime.combine(dates[0], time(23, 0)),
            arrival_dt=datetime.combine(dates[1], time(11, 00)),
            departure_dt=None
        ),
    ]

    platforms_client = PlatformsClient(get_dynamic_platform_collection())
    platforms_client.update([
        PlatformRecord(
            key=PlatformKey(date=dates[0], station_id=stations[0].id, train_number=threads[0].number),
            data=PlatformData(departure_platform='platform_0d', arrival_platform='platform_0a')
        ),
        PlatformRecord(
            key=PlatformKey(date=dates[0], station_id=stations[1].id, train_number=threads[1].number),
            data=PlatformData(arrival_platform='platform_1a')
        ),
        PlatformRecord(
            key=PlatformKey(date=dates[1], station_id=stations[1].id, train_number=threads[1].number),
            data=PlatformData(departure_platform='platform_1d')
        ),
        PlatformRecord(
            key=PlatformKey(date=dates[1], station_id=stations[0].id, train_number=threads[1].number),
            data=PlatformData(arrival_platform='platform_2a')
        ),
    ])

    batch = ScheduleRoutePlatformsBatch()
    batch.try_load(platforms_client, schedule_routes)

    assert batch.get_departure(schedule_routes[0]) == 'platform_0d'
    assert batch.get_arrival(schedule_routes[0]) == 'platform_0a'
    assert batch.get_departure(schedule_routes[1]) == 'platform_1d'
    assert batch.get_arrival(schedule_routes[1]) == 'platform_1a'
    assert batch.get_departure(schedule_routes[2], 'default') == 'default'
    assert batch.get_arrival(schedule_routes[2]) == 'platform_2a'
