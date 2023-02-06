# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import date, datetime, time, timedelta

import pytest
import pytz
from hamcrest import assert_that, contains_inanyorder, has_properties

from common.models.transport import TransportType
from common.tester.factories import create_station, create_thread
from common.tester.utils.datetime import replace_now
from stationschedule.utils import EVENT_ARRIVAL, EVENT_DEPARTURE
from stationschedule.views import station_schedule


pytestmark = pytest.mark.dbuser


@replace_now('2019-01-25')
def test_station_schedule_wo_code_sharing():
    thread_tzinfo = pytz.timezone('Europe/Moscow')
    stations = [
        create_station(title='begin', id=100, t_type=TransportType.PLANE_ID, time_zone=thread_tzinfo),  # UTC+3
        create_station(title='middle', id=200, t_type=TransportType.PLANE_ID, time_zone=thread_tzinfo),  # UTC+3
        create_station(title='end', id=900, t_type=TransportType.PLANE_ID, time_zone=thread_tzinfo),  # UTC+3
    ]

    def st_by_id(station_id):
        return next(st for st in stations if st.id == station_id)

    base_thread_date = datetime.combine(date(2019, 1, 24), time.min)
    thread_start_time = time(13, 15)
    shift_1d = 24 * 60   # 1 day
    base_year_days = [
        base_thread_date,
        base_thread_date + timedelta(days=1),
        base_thread_date + timedelta(days=2),
    ]
    threads = [
        create_thread(
            uid='t_0',
            number='1_common',
            t_type=TransportType.PLANE_ID,
            year_days=base_year_days,
            tz_start_time=thread_start_time,
            schedule_v1=[  # path: 100->200->900
                [None, 0, st_by_id(100), {'time_zone': st_by_id(100).time_zone}],
                [shift_1d, shift_1d + 60, st_by_id(200), {'time_zone': st_by_id(200).time_zone}],
                [shift_1d * 2, None, st_by_id(900), {'time_zone': st_by_id(900).time_zone}],
            ]
        ),
        create_thread(  # departure_code_sharing
            uid='t_0_dep',
            number='2_share_departure',
            t_type=TransportType.PLANE_ID,
            year_days=base_year_days,
            tz_start_time=thread_start_time,
            schedule_v1=[  # path: 100->200->900
                [None, 0, st_by_id(100), {'time_zone': st_by_id(100).time_zone}],
                [shift_1d, shift_1d + 60, st_by_id(200), {'time_zone': st_by_id(200).time_zone,
                                                          'departure_code_sharing': True}],
                [shift_1d * 2, None, st_by_id(900), {'time_zone': st_by_id(900).time_zone}],
            ]
        ),
        create_thread(  # arrival_code_sharing
            uid='t_0_arr',
            number='3_share_arrival',
            t_type=TransportType.PLANE_ID,
            year_days=base_year_days,
            tz_start_time=thread_start_time,
            schedule_v1=[  # path: 100->200->900
                [None, 0, st_by_id(100), {'time_zone': st_by_id(100).time_zone}],
                [shift_1d, shift_1d + 60, st_by_id(200), {'time_zone': st_by_id(200).time_zone,
                                                          'arrival_code_sharing': True}],
                [shift_1d * 2, None, st_by_id(900), {'time_zone': st_by_id(900).time_zone}],
            ]
        ),
    ]

    def thread_by_uid(uid):
        return next(t for t in threads if t.uid == uid)

    departure_schedule = station_schedule(st_by_id(200), event=EVENT_DEPARTURE)
    arrival_schedule = station_schedule(st_by_id(200), event=EVENT_ARRIVAL)

    assert len(departure_schedule.schedule) == 2
    assert_that(departure_schedule.schedule, contains_inanyorder(
        has_properties({'number': thread_by_uid('t_0').number}),
        has_properties({'number': thread_by_uid('t_0_arr').number}),
    ))

    assert len(arrival_schedule.schedule) == 2
    assert_that(arrival_schedule.schedule, contains_inanyorder(
        has_properties({'number': thread_by_uid('t_0').number}),
        has_properties({'number': thread_by_uid('t_0_dep').number}),
    ))
