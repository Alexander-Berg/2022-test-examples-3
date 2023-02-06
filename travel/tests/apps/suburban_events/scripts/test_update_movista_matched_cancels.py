# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime

import pytest

from common.apps.suburban_events.forecast.match_cppk import CppkEvent
from common.apps.suburban_events.models import CancelledStation, ThreadEvents
from common.apps.suburban_events.scripts.update_movista_matched_cancels import save_matched_cancels
from common.apps.suburban_events.utils import get_rtstation_key
from common.tester.factories import create_rtstation, create_thread, create_station
from common.tester.utils.datetime import replace_now

create_thread = create_thread.mutate(t_type="suburban")
create_station = create_station.mutate(t_type="suburban")


@pytest.mark.mongouser
@pytest.mark.dbuser
@replace_now('2021-04-02 18:00:00')
def test_save_matched_cancels():
    create_dt_1 = datetime(2021, 4, 2, 1, 30)
    create_dt_2 = datetime(2021, 4, 2, 2, 0)
    create_dt_3 = datetime(2021, 4, 2, 2, 30)

    stations_1 = [create_station(title='station_1_{}'.format(i)) for i in range(2)]
    thread_1 = create_thread(
        number='thread_1',
        schedule_v1=[
            [None, 0, stations_1[0]],
            [10, None, stations_1[1]],
        ]
    )
    cancelled_stations_1 = list(thread_1.path)

    stations_2 = [create_station(title='station_2_{}'.format(i)) for i in range(3)]
    thread_2 = create_thread(
        number='thread_2',
        schedule_v1=[
            [None, 0, stations_2[0]],
            [10, 20, stations_2[1]],
            [30, None, stations_2[2]]
        ])
    cancelled_stations_2 = list(thread_2.path)

    # отмена thread_1
    cancel_1 = CppkEvent({'create_dt': create_dt_1})
    cancel_1.suburban_key = 'sk_1'
    cancel_1.thread_start_dt = datetime(2021, 4, 2, 10, 0)
    cancel_1.cancelled_stations = cancelled_stations_1

    # отмена thread_2
    cancel_2 = CppkEvent({'create_dt': create_dt_2})
    cancel_2.suburban_key = 'sk_2'
    cancel_2.thread_start_dt = datetime(2021, 4, 2, 15, 0)
    cancel_2.cancelled_stations = cancelled_stations_2

    # отмена отмены thread_2 (cancel_2)
    cancel_3 = CppkEvent({'create_dt': create_dt_3})
    cancel_3.suburban_key = 'sk_2'
    cancel_3.thread_start_dt = datetime(2021, 4, 2, 15, 0)
    cancel_3.cancelled_stations = []

    save_matched_cancels([cancel_1, cancel_2, cancel_3])

    assert ThreadEvents.objects.count() == 2

    th_event_1 = ThreadEvents.objects.get(key__thread_key='sk_1')
    assert len(th_event_1.stations_cancels) == 1

    th_1_cancel_1 = th_event_1.stations_cancels[0]
    assert th_1_cancel_1.dt_save == create_dt_1
    assert th_1_cancel_1.cancelled_stations == [CancelledStation(
        station_key=get_rtstation_key(rts),
        departure_time=rts.tz_departure,
        arrival_time=rts.tz_arrival
    ) for rts in cancelled_stations_1]

    th_event_2 = ThreadEvents.objects.get(key__thread_key='sk_2')
    assert len(th_event_2.stations_cancels) == 2

    th_2_cancel_1, th_2_cancel_2 = th_event_2.stations_cancels
    assert th_2_cancel_1.dt_save == create_dt_2
    assert th_2_cancel_1.cancelled_stations == [CancelledStation(
        station_key=get_rtstation_key(rts),
        departure_time=rts.tz_departure,
        arrival_time=rts.tz_arrival
    ) for rts in cancelled_stations_2]

    assert th_2_cancel_2.dt_save == create_dt_3
    assert len(th_2_cancel_2.cancelled_stations) == 0

    assert th_2_cancel_1.dt_save < th_2_cancel_2.dt_save
