# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime, time, timedelta

import pytest
from hamcrest import assert_that, has_properties

from common.apps.suburban_events.api import (
    get_cancelled_path_for_segments, get_states_for_segments, get_states_for_thread, get_states_by_schedule_routes
)
from common.apps.suburban_events.factories import (
    ThreadStationStateFactory, EventStateFactory, ThreadStationKeyFactory
)
from common.apps.suburban_events.models import SuburbanKey
from common.apps.suburban_events.utils import (
    get_rtstation_key, ThreadEventsTypeCodes, ClockDirection, get_thread_suburban_key, EventStateType
)
from common.models.schedule import RTStation
from common.tester.factories import create_thread, create_station, create_rthread_segment
from common.tester.skippers import has_route_search, has_stationschedule

create_thread = create_thread.mutate(t_type="suburban")
create_station = create_station.mutate(t_type="suburban")

pytestmark = [pytest.mark.mongouser, pytest.mark.dbuser]


def test_get_states_for_thread():
    station_from, station_to = create_station(id=121), create_station(id=122)
    station_mid_1, station_mid_2 = create_station(id=123), create_station(id=124)

    start_dt_1 = datetime(2017, 3, 20, 17)
    start_dt_2 = datetime(2017, 3, 25, 11)

    thread_1 = create_thread(
        uid=u'uid_1',
        number=u'number_1',
        id=121,
        tz_start_time=time(17),
        schedule_v1=[
            [None, 0, station_from],
            [10, 20, station_mid_1],
            [50, None, station_to],
        ],
    )

    thread_2 = create_thread(
        uid=u'uid_2',
        number=u'number_2',
        id=123,
        tz_start_time=time(11),
        schedule_v1=[
            [None, 0, station_from],
            [20, 25, station_mid_1],
            [35, 40, station_mid_2],
            [50, None, station_to],
        ],
    )

    rts_th_1_mid_1 = RTStation.objects.get(thread=thread_1, station=station_mid_1)
    rts_th_1_to = RTStation.objects.get(thread=thread_1, station=station_to)
    rts_th_2_mid_2 = RTStation.objects.get(thread=thread_2, station=station_mid_2)  # noqa
    rts_th_2_from = RTStation.objects.get(thread=thread_2, station=station_from)
    rts_th_2_to = RTStation.objects.get(thread=thread_2, station=station_to)

    tss = ThreadStationStateFactory.create_from_rtstation(
        rts=rts_th_1_mid_1,
        thread_start_date=start_dt_1,
        arrival={
            'dt': datetime(2017, 3, 20, 17, 11),
            'type': u'fact',
            'thread_uid': u'uid_1',
            'minutes_from': 1,
            'minutes_to': 1
        },
        departure={
            'dt': datetime(2017, 3, 20, 17, 27),
            'type': u'fact',
            'thread_uid': u'uid_1',
            'minutes_from': 7,
            'minutes_to': 7
        },
        passed_several_times=False
    )

    ThreadStationStateFactory.create_from_rtstation(
        rts=rts_th_1_to,
        thread_start_date=start_dt_1,
        arrival={
            'thread_uid': u'uid_1',
            'type': u'possible_delay',
            'minutes_from': None,
            'minutes_to': None,
            'dt': None
        },
        passed_several_times=True
    )

    ThreadStationStateFactory.create_from_rtstation(
        rts=rts_th_2_from,
        thread_start_date=start_dt_2,
        departure={
            'dt': datetime(2017, 3, 20, 11, 5),
            'type': u'fact',
            'thread_uid': u'uid_2',
            'minutes_from': 5,
            'minutes_to': 5
        },
        passed_several_times=True
    )

    ThreadStationStateFactory.create_from_rtstation(
        rts=rts_th_2_to,
        thread_start_date=start_dt_2,
        arrival={
            'dt': datetime(2017, 3, 20, 11, 56),
            'type': u'fact',
            'thread_uid': u'uid_2',
            'minutes_from': 6,
            'minutes_to': 6
        },
        passed_several_times=True
    )

    thread_states, rts_states = get_states_for_thread(thread=thread_1, start_date=start_dt_1, rtstations=thread_1.path)

    assert_that(rts_states[rts_th_1_mid_1].arrival,
                has_properties({
                    'dt': datetime(2017, 3, 20, 17, 11),
                    'type': u'fact',
                    'tz': u'Europe/Moscow',
                    'minutes_from': 1,
                    'minutes_to': 1
                }))

    assert_that(rts_states[rts_th_1_mid_1].departure,
                has_properties({
                    'dt': datetime(2017, 3, 20, 17, 27),
                    'type': u'fact',
                    'tz': u'Europe/Moscow',
                    'minutes_from': 7,
                    'minutes_to': 7
                }))

    assert_that(rts_states[rts_th_1_to].arrival,
                has_properties({
                    'type': u'possible_delay',
                    'tz': u'Europe/Moscow',
                    'minutes_from': None,
                    'minutes_to': None,
                    'dt': None,
                }))

    # проверяем, что outdated записи не используются
    tss.update(outdated=True)
    thread_states, rts_states = get_states_for_thread(thread=thread_1, start_date=start_dt_1, rtstations=thread_1.path)
    assert rts_th_1_mid_1 not in rts_states

    # Проверяем с флагом all_keys.
    # Ключи должны быть у всех стейтов.
    thread_states, rts_states = get_states_for_thread(
        thread=thread_1, start_date=start_dt_1, rtstations=thread_1.path, all_keys=True)
    assert_that(rts_states[rts_th_1_mid_1], has_properties({
        'arrival': None,
        'departure': None,
        'key': 'number_1__121___2017-03-20T17:00:00___123___10___20___None___None',
    }))

    # проверяем для другой даты
    thread_states, rts_states = get_states_for_thread(thread=thread_2, start_date=start_dt_2, rtstations=thread_2.path)

    assert_that(rts_states[rts_th_2_from].departure,
                has_properties({
                    'dt': datetime(2017, 3, 20, 11, 5),
                    'type': u'fact',
                    'tz': u'Europe/Moscow',
                    'minutes_from': 5,
                    'minutes_to': 5
                }))

    assert_that(rts_states[rts_th_2_to].arrival,
                has_properties({
                    'dt': datetime(2017, 3, 20, 11, 56),
                    'type': u'fact',
                    'tz': u'Europe/Moscow',
                    'minutes_from': 6,
                    'minutes_to': 6
                }))


@has_route_search
def test_get_states_for_segments():
    stations = [create_station(id=i) for i in range(121, 125)]

    start_dt_1 = datetime(2017, 3, 20, 17)
    start_dt_2 = datetime(2017, 3, 25, 11)

    thread_1 = create_thread(
        uid=u'uid_1',
        number=u'number_1',
        tz_start_time=start_dt_1.time(),
        schedule_v1=[
            [None, 0, stations[0]],
            [10, 20, stations[1]],
            [50, None, stations[3]],
        ],
    )
    th_1_rts = thread_1.path

    thread_2 = create_thread(
        uid=u'uid_2',
        number=u'number_2',
        tz_start_time=start_dt_2.time(),
        schedule_v1=[
            [None, 0, stations[0]],
            [20, 25, stations[1]],
            [35, 40, stations[2]],
            [50, None, stations[3]],
        ],
    )
    th_2_rts = thread_2.path

    segments = [
        create_rthread_segment(
            rts_from=th_1_rts[0],
            rts_to=th_1_rts[2],
            start_date=start_dt_1.date()
        ),
        create_rthread_segment(
            rts_from=th_2_rts[1],
            rts_to=th_2_rts[2],
            start_date=start_dt_2.date()
        )
    ]

    ThreadStationStateFactory.create_from_rtstation(
        rts=th_1_rts[0],
        thread_start_date=start_dt_1,
        departure={
            'dt': datetime(2017, 3, 20, 17, 3),
            'type': u'fact',
            'thread_uid': u'uid_1',
            'minutes_from': 3,
            'minutes_to': 3
        }
    )

    tss = ThreadStationStateFactory.create_from_rtstation(
        rts=th_1_rts[2],
        thread_start_date=start_dt_1,
        arrival={
            'thread_uid': u'uid_1',
            'type': u'possible_delay',
        },
        departure={
            'thread_uid': u'uid_1',
            'type': u'possible_delay',
        }
    )

    ThreadStationStateFactory.create_from_rtstation(
        rts=th_2_rts[2],
        thread_start_date=start_dt_2,
        arrival={
            'dt': datetime(2017, 3, 25, 11, 37),
            'type': u'fact',
            'thread_uid': u'uid_2',
            'minutes_from': 2,
            'minutes_to': 2
        }
    )

    segments_states = get_states_for_segments(segments)

    assert_that(segments_states[segments[0]].departure,
                has_properties({
                    'dt': datetime(2017, 3, 20, 17, 3),
                    'type': u'fact',
                    'tz': u'Europe/Moscow',
                    'minutes_from': 3,
                    'minutes_to': 3
                }))

    assert_that(segments_states[segments[0]].arrival,
                has_properties({
                    'type': u'possible_delay',
                    'tz': u'Europe/Moscow'
                }))

    assert_that(segments_states[segments[1]].arrival,
                has_properties({
                    'dt': datetime(2017, 3, 25, 11, 37),
                    'type': u'fact',
                    'tz': u'Europe/Moscow',
                    'minutes_from': 2,
                    'minutes_to': 2
                }))
    assert segments_states[segments[1]].departure is None

    # проверяем, что outdated записи не используются
    tss.update(outdated=True)
    segments_states = get_states_for_segments(segments)
    assert segments_states[segments[0]].arrival is None
    assert segments_states[segments[0]].departure is not None

    # Проверяем с флагом all_keys.
    # Ключи должны быть у всех стейтов.
    segments_states = get_states_for_segments(segments, all_keys=True)
    assert_that(segments_states[segments[0]], has_properties({
        'arrival': has_properties({
            'key': 'number_1__121___2017-03-20T17:00:00___124___50___None___None___None',
            'type': 'undefined'
        }),
        'departure': has_properties({
            'key': 'number_1__121___2017-03-20T17:00:00___121___None___0___None___None',
            'type': 'fact'
        }),
    }))


@has_route_search
def test_get_states_for_segments_from_arrival_to_departure():
    stations = [create_station() for i in range(3)]
    start_dt = datetime(2017, 3, 20, 17)

    thread = create_thread(
        uid=u'uid_1',
        number=u'number_1',
        tz_start_time=start_dt.time(),
        schedule_v1=[
            [None, 0, stations[0]],
            [10, 20, stations[1]],
            [50, None, stations[2]],
        ],
    )
    rtstations = thread.path

    segments = [
        create_rthread_segment(
            rts_from=rtstations[1],
            rts_to=rtstations[2],
            start_date=start_dt.date()
        ),
        create_rthread_segment(
            rts_from=rtstations[0],
            rts_to=rtstations[1],
            start_date=start_dt.date()
        )
    ]

    ThreadStationStateFactory.create_from_rtstation(
        rts=rtstations[1],
        thread_start_date=start_dt,
        arrival={
            'dt': start_dt + timedelta(minutes=13),
            'type': 'fact',
            'minutes_from': 3,
            'minutes_to': 3
        },
        departure={
            'dt': start_dt + timedelta(minutes=25),
            'type': 'fact',
            'minutes_from': 5,
            'minutes_to': 5
        }
    )

    ThreadStationStateFactory.create_from_rtstation(
        rts=rtstations[2],
        thread_start_date=start_dt,
        arrival={
            'type': 'possible_delay',
        }
    )

    segments_states = get_states_for_segments(segments)

    assert_that(segments_states[segments[0]], has_properties({
        'arrival': has_properties({
            'type': 'possible_delay'
        }),
        'departure': has_properties({
            'dt': start_dt + timedelta(minutes=25),
            'type': 'fact',
            'minutes_from': 5,
            'minutes_to': 5
        }),
        'from_arrival': has_properties({
            'dt': start_dt + timedelta(minutes=13),
            'type': 'fact',
            'minutes_from': 3,
            'minutes_to': 3
        }),
        'to_departure': None
    }))

    assert_that(segments_states[segments[1]], has_properties({
        'from_arrival': None,
        'departure': None,
        'arrival': has_properties({
            'dt': start_dt + timedelta(minutes=13),
            'type': 'fact',
            'minutes_from': 3,
            'minutes_to': 3
        }),
        'to_departure': has_properties({
            'dt': start_dt + timedelta(minutes=25),
            'type': 'fact',
            'minutes_from': 5,
            'minutes_to': 5
        })
    }))


@has_stationschedule
def test_get_states_by_schedule_routes():
    from stationschedule import SUBURBAN, get_schedule_class

    station_from, station_to = create_station(id=121), create_station(id=122)
    station_mid_1, station_mid_2 = create_station(id=123), create_station(id=124)

    start_date_1 = datetime(2017, 3, 20, 17)
    start_date_2 = datetime(2017, 3, 25, 11)

    thread_1 = create_thread(
        uid=u'uid_1',
        number=u'number_1',
        id=500,
        year_days=[start_date_1],
        tz_start_time=start_date_1.time(),
        schedule_v1=[
            [None, 0, station_from],
            [10, 20, station_mid_1],
            [50, None, station_to],
        ],
    )

    thread_2 = create_thread(
        uid=u'uid_2',
        number=u'number_2',
        id=600,
        year_days=[start_date_1, start_date_2],
        tz_start_time=start_date_2.time(),
        schedule_v1=[
            [None, 0, station_from],
            [10, 25, station_mid_1],
            [35, 40, station_mid_2],
            [50, None, station_to],
        ],
    )

    thread_key = get_thread_suburban_key(thread_1.number, station_from)
    SuburbanKey.objects.get_or_create(thread=thread_1, key=thread_key)
    # sub_key_1 = SuburbanKey.objects.create(thread=thread_1, key='thread_1_key')
    # sub_key_2 = SuburbanKey.objects.create(thread=thread_2, key='thread_2_key')
    rts_th_1_from = RTStation.objects.get(thread=thread_1, station=station_from)
    rts_th_1_mid_1 = RTStation.objects.get(thread=thread_1, station=station_mid_1)
    rts_th_1_to = RTStation.objects.get(thread=thread_1, station=station_to)  # noqa
    rts_th_2_from = RTStation.objects.get(thread=thread_2, station=station_from)
    rts_th_2_mid_1 = RTStation.objects.get(thread=thread_2, station=station_mid_1)

    # Проверяем с флагом all_keys.
    # Ключи должны быть у всех стейтов.
    # Стейтов отправления/прибытия у конечных/начальных станций быть не должно.
    schedule_cls = get_schedule_class(station_from, schedule_type=SUBURBAN, t_type_code=SUBURBAN)
    schedule = schedule_cls(station_from, requested_direction='all', schedule_date=start_date_1)
    schedule.build(schedule_date=start_date_1)
    schedule_routes = sorted(schedule.schedule, key=lambda x: x.number)
    schedule_routes_states = get_states_by_schedule_routes(schedule_routes, all_keys=True)
    assert_that(schedule_routes_states[schedule_routes[0]], has_properties({
        'arrival': None,
        'departure': has_properties({'key': 'number_1__121___2017-03-20T17:00:00___121___None___0___None___None',
                                     'type': 'undefined'}),
    }))

    schedule_cls = get_schedule_class(station_mid_1, schedule_type=SUBURBAN, t_type_code=SUBURBAN)
    schedule = schedule_cls(station_mid_1, requested_direction='all', schedule_date=start_date_1)
    schedule.build(schedule_date=start_date_1)
    schedule_routes = sorted(schedule.schedule, key=lambda x: x.number)

    ThreadStationStateFactory.create_from_rtstation(
        rts=rts_th_1_from,
        thread_start_date=start_date_1,
        departure={
            'dt': datetime(2017, 3, 20, 17, 3),
            'type': 'fact',
            'thread_uid': 'uid_1',
            'minutes_from': 3,
            'minutes_to': 3
        }
    )

    tss = ThreadStationStateFactory.create_from_rtstation(
        rts=rts_th_1_mid_1,
        thread_start_date=start_date_1,
        arrival={
            'thread_uid': 'uid_1',
            'type': 'possible_delay',
        },
        departure={
            'thread_uid': 'uid_1',
            'type': 'possible_delay',
        }
    )

    ThreadStationStateFactory.create_from_rtstation(
        rts=rts_th_2_mid_1,
        thread_start_date=datetime.combine(start_date_1.date(), start_date_2.time()),
        arrival={
            'dt': datetime(2017, 3, 20, 11, 37),
            'type': 'fact',
            'thread_uid': 'uid_2',
            'minutes_from': 2,
            'minutes_to': 2
        },
        departure={}
    )

    ThreadStationStateFactory.create_from_rtstation(
        rts=rts_th_2_from,
        thread_start_date=start_date_2,
        departure={
            'dt': datetime(2017, 3, 25, 11, 55),
            'type': u'fact',
            'thread_uid': u'uid_2',
            'minutes_from': 5,
            'minutes_to': 5
        }
    )

    schedule_routes_states = get_states_by_schedule_routes(schedule_routes)

    assert_that(schedule_routes_states[schedule_routes[0]].departure,
                has_properties({
                    'type': u'possible_delay',
                    'tz': u'Europe/Moscow'
                }))

    assert_that(schedule_routes_states[schedule_routes[0]].arrival,
                has_properties({
                    'type': u'possible_delay',
                    'tz': u'Europe/Moscow'
                }))

    assert_that(schedule_routes_states[schedule_routes[1]].arrival,
                has_properties({
                    'dt': datetime(2017, 3, 20, 11, 37),
                    'type': u'fact',
                    'tz': u'Europe/Moscow',
                    'minutes_from': 2,
                    'minutes_to': 2
                }))

    assert schedule_routes_states[schedule_routes[1]].departure is None

    # проверяем, что outdated записи не используются
    tss.update(outdated=True)
    schedule_routes_states = get_states_by_schedule_routes(schedule_routes)
    assert schedule_routes_states[schedule_routes[0]].departure is None
    assert schedule_routes_states[schedule_routes[0]].arrival is None

    # Проверяем с флагом all_keys.
    # Ключи должны быть у всех стейтов.
    schedule_routes_states = get_states_by_schedule_routes(schedule_routes, all_keys=True)
    assert_that(schedule_routes_states[schedule_routes[0]], has_properties({
        'arrival': has_properties({'key': 'number_1__121___2017-03-20T17:00:00___123___10___20___None___None',
                                   'type': 'undefined'}),
        'departure': has_properties({'key': 'number_1__121___2017-03-20T17:00:00___123___10___20___None___None',
                                     'type': 'undefined'}),
    }))

    # проверяем для другой даты
    schedule_cls = get_schedule_class(station_from, schedule_type=SUBURBAN, t_type_code=SUBURBAN)
    schedule = schedule_cls(station_from, requested_direction='all', schedule_date=start_date_2)
    schedule.build(schedule_date=start_date_2)
    schedule_routes = schedule.schedule

    schedule_routes_states = get_states_by_schedule_routes(schedule_routes)

    assert_that(schedule_routes_states[schedule_routes[0]].departure,
                has_properties({
                    'dt': datetime(2017, 3, 25, 11, 55),
                    'type': u'fact',
                    'tz': u'Europe/Moscow',
                    'minutes_from': 5,
                    'minutes_to': 5
                }))


@has_route_search
def test_get_states_for_mczk_segments():
    station_from, station_mid, station_to = create_station(id=121), create_station(id=122), create_station(id=123)
    start_dt_1 = datetime(2017, 3, 20, 17)
    start_dt_2 = datetime(2017, 3, 20, 17, 10)

    thread_1 = create_thread(
        number='МЦК',
        uid='MCZK_1',
        title='по часовой стрелке',
        tz_start_time=start_dt_1.time(),
        schedule_v1=[
            [None, 10, station_from],
            [20, 30, station_mid],
            [50, None, station_to],
        ],
    )
    thread_2 = create_thread(
        number='МЦК',
        uid='MCZK_2',
        title='против часовой стрелки',
        tz_start_time=start_dt_2.time(),
        schedule_v1=[
            [None, 10, station_from],
            [20, 30, station_mid],
            [50, None, station_to],
        ],
    )

    th_1_rtstations = thread_1.path
    th_2_rtstations = thread_2.path

    segments = [
        create_rthread_segment(
            rts_from=th_1_rtstations[1],
            rts_to=th_1_rtstations[2],
            start_date=start_dt_1.date(),
            departure=start_dt_1 + timedelta(minutes=30),
            arrival=start_dt_1 + timedelta(minutes=50),
        ),
        create_rthread_segment(
            rts_from=th_2_rtstations[1],
            rts_to=th_2_rtstations[2],
            start_date=start_dt_2.date(),
            departure=start_dt_2 + timedelta(minutes=30),
            arrival=start_dt_2 + timedelta(minutes=50),
        )
    ]

    sub_key_1 = SuburbanKey.objects.create(thread=thread_1, key='thread_1_key')
    SuburbanKey.objects.create(thread=thread_2, key='thread_2_key')

    ThreadStationStateFactory(**{
        'key': ThreadStationKeyFactory(**{
            'thread_key': sub_key_1.key,
            'thread_start_date': start_dt_1,
            'station_key': get_rtstation_key(th_1_rtstations[1]),
            'thread_type': ThreadEventsTypeCodes.MCZK,
            'clock_direction': 1,
            'arrival': th_1_rtstations[1].tz_arrival,
            'departure': th_1_rtstations[1].tz_departure
        }),
        'departure_state': EventStateFactory(**{
            'dt': datetime(2017, 3, 20, 17, 40),
            'type': u'fact',
            'thread_uid': u'MCZK_1',
            'minutes_from': 0,
            'minutes_to': 0
        }),
        'passed_several_times': False,
        'arrival': th_1_rtstations[1].tz_arrival,
        'departure': th_1_rtstations[1].tz_departure,
        'tz': th_1_rtstations[1].station.time_zone,

    })

    ThreadStationStateFactory(**{
        'key': ThreadStationKeyFactory(**{
            'thread_key': sub_key_1.key,
            'thread_start_date': start_dt_1,
            'station_key': get_rtstation_key(th_1_rtstations[2]),
            'thread_type': ThreadEventsTypeCodes.MCZK,
            'clock_direction': 1,
            'arrival': th_1_rtstations[2].tz_arrival
        }),
        'arrival_state': EventStateFactory(**{
            'thread_uid': u'MCZK_1',
            'type': u'possible_delay',
        }),
        'arrival': th_1_rtstations[2].tz_arrival,
        'tz': th_1_rtstations[2].station.time_zone,
        'passed_several_times': False
    })

    segments_states = get_states_for_segments(segments)

    assert_that(segments_states[segments[0]].departure,
                has_properties({
                    'dt': datetime(2017, 3, 20, 17, 40),
                    'type': u'fact',
                    'tz': u'Europe/Moscow',
                    'minutes_from': 0,
                    'minutes_to': 0
                }))

    assert_that(segments_states[segments[0]].arrival,
                has_properties({
                    'type': u'possible_delay',
                    'tz': u'Europe/Moscow'
                }))

    assert segments_states[segments[1]].departure is None
    assert segments_states[segments[1]].arrival is None


def test_get_states_for_mczk_thread():
    station_from, station_mid, station_to = create_station(id=121), create_station(id=122), create_station(id=123)
    start_dt = datetime(2017, 3, 20, 17)

    thread = create_thread(
        number='МЦК',
        uid='MCZK_1',
        title='по часовой стрелке',
        tz_start_time=start_dt.time(),
        schedule_v1=[
            [None, 10, station_from],
            [20, 30, station_mid],
            [50, None, station_to],
        ],
    )
    th_rtstations = thread.path
    sub_key = SuburbanKey.objects.create(thread=thread, key='МЦК_121')

    ThreadStationStateFactory(**{
        'key': ThreadStationKeyFactory(**{
            'thread_key': sub_key.key,
            'thread_start_date': start_dt,
            'station_key': '122',
            'clock_direction': ClockDirection.CLOCK_WISE,
            'thread_type': ThreadEventsTypeCodes.MCZK,
            'arrival': th_rtstations[1].tz_arrival,
            'departure': th_rtstations[1].tz_departure
        }),
        'arrival_state': EventStateFactory(**{
            'dt': datetime(2017, 3, 20, 17, 23),
            'type': u'fact',
            'thread_uid': 'MCZK_1',
            'minutes_from': 3,
            'minutes_to': 3
        }),
        'departure_state': EventStateFactory(**{
            'dt': datetime(2017, 3, 20, 17, 31),
            'type': 'fact',
            'thread_uid': 'MCZK_1',
            'minutes_from': 1,
            'minutes_to': 1
        }),
        'arrival': 20,
        'departure': 30,
        'tz': station_mid.time_zone,
    })

    ThreadStationStateFactory(**{
        'key': ThreadStationKeyFactory(**{
            'thread_key': sub_key.key,
            'thread_start_date': start_dt,
            'station_key': '123',
            'clock_direction': ClockDirection.CLOCK_WISE,
            'thread_type': ThreadEventsTypeCodes.MCZK,
            'arrival': th_rtstations[2].tz_arrival
        }),
        'arrival_state': EventStateFactory(**{
            'dt': datetime(2017, 3, 20, 17, 55),
            'type': 'fact',
            'thread_uid': 'MCZK_1',
            'minutes_from': 5,
            'minutes_to': 5
        }),
        'arrival': 50,
        'tz': station_to.time_zone,
    })

    thread_states, rts_states = get_states_for_thread(thread=thread, start_date=start_dt, rtstations=thread.path)
    assert len(rts_states) == 2

    assert_that(rts_states[thread.path[1]].arrival,
                has_properties({
                    'dt': datetime(2017, 3, 20, 17, 23),
                    'type': 'fact',
                    'tz': 'Europe/Moscow',
                    'minutes_from': 3,
                    'minutes_to': 3
                }))

    assert_that(rts_states[thread.path[1]].departure,
                has_properties({
                    'dt': datetime(2017, 3, 20, 17, 31),
                    'type': 'fact',
                    'tz': 'Europe/Moscow',
                    'minutes_from': 1,
                    'minutes_to': 1
                }))

    assert_that(rts_states[thread.path[2]].arrival,
                has_properties({
                    'dt': datetime(2017, 3, 20, 17, 55),
                    'type': 'fact',
                    'tz': 'Europe/Moscow',
                    'minutes_from': 5,
                    'minutes_to': 5
                }))


@has_route_search
def test_get_states_for_segments_station_double():
    stations = [create_station(id=i) for i in range(121, 124)]
    start_dt = datetime(2017, 3, 20, 17)

    thread = create_thread(
        number='number_1',
        tz_start_time=start_dt.time(),
        schedule_v1=[
            [None, 0, stations[0]],
            [10, 15, stations[1]],
            [50, 55, stations[0]],
            [80, None, stations[2]],
        ],
    )
    rtstations = thread.path

    segments = [
        create_rthread_segment(
            rts_from=rtstations[0],
            rts_to=rtstations[2],
            start_date=start_dt.date()
        ),
        create_rthread_segment(
            rts_from=rtstations[2],
            rts_to=rtstations[3],
            start_date=start_dt.date()
        )
    ]

    ThreadStationStateFactory.create_from_rtstation(
        rts=rtstations[0],
        thread_start_date=start_dt,
        departure={
            'dt': datetime(2017, 3, 20, 17, 3),
            'minutes_from': 3,
            'minutes_to': 3
        }
    )

    tss = ThreadStationStateFactory.create_from_rtstation(
        rts=rtstations[2],
        thread_start_date=start_dt,
        arrival={
            'type': 'possible_delay',
            'minutes_from': 1,
            'minutes_to': 4,
        },
        departure={
            'type': 'possible_delay',
            'minutes_from': 11,
            'minutes_to': 14,

        }
    )

    segments_states = get_states_for_segments(segments)

    assert_that(segments_states[segments[0]], has_properties({
        'departure': has_properties({
            'dt': datetime(2017, 3, 20, 17, 3),
            'type': 'fact',
            'minutes_from': 3,
            'minutes_to': 3
        }),
        'arrival': has_properties({
            'type': u'possible_delay',
            'minutes_from': 1,
            'minutes_to': 4
        })
    }))

    assert_that(segments_states[segments[1]], has_properties({
        'departure': has_properties({
            'type': u'possible_delay',
            'minutes_from': 11,
            'minutes_to': 14
        }),
        'arrival': None
    }))

    tss.update(set__arrival_state=None)
    segments_states = get_states_for_segments(segments)

    assert_that(segments_states[segments[0]], has_properties({
        'departure': has_properties({
            'dt': datetime(2017, 3, 20, 17, 3),
            'type': 'fact'
        }),
        'arrival': None
    }))


def test_get_states_for_thread_station_double():
    stations = [create_station(id=i) for i in range(121, 123)]
    start_dt = datetime(2017, 3, 20, 17)

    thread = create_thread(
        number='number_1',
        tz_start_time=start_dt.time(),
        schedule_v1=[
            [None, 0, stations[0]],
            [10, 15, stations[1]],
            [50, None, stations[0]],
        ],
    )
    rtstations = thread.path

    ThreadStationStateFactory.create_from_rtstation(
        rts=rtstations[0],
        thread_start_date=start_dt,
        departure={
            'dt': datetime(2017, 3, 20, 17, 27),
            'type': 'fact',
            'minutes_from': 7,
            'minutes_to': 7
        }
    )

    ThreadStationStateFactory.create_from_rtstation(
        rts=rtstations[2],
        thread_start_date=start_dt,
        arrival={
            'type': 'possible_delay',
            'minutes_from': None,
            'minutes_to': None
        }
    )

    thread_states, rts_states = get_states_for_thread(thread=thread, start_date=start_dt, rtstations=rtstations)

    assert_that(rts_states[rtstations[0]], has_properties({
        'arrival': None,
        'departure': has_properties({
            'dt': datetime(2017, 3, 20, 17, 27),
            'type': 'fact',
            'minutes_from': 7,
            'minutes_to': 7
        })
    }))

    assert_that(rts_states[rtstations[2]], has_properties({
        'arrival': has_properties({
            'type': 'possible_delay',
            'minutes_from': None,
            'minutes_to': None
        }),
        'departure': None
    }))


def test_get_states_for_segments_with_cancels():
    stations = [create_station() for _ in range(3)]
    start_dt = datetime(2021, 4, 19, 15)
    thread_uid = 'thread_uid'
    thread = create_thread(
        number='thread_number',
        uid=thread_uid,
        tz_start_time=start_dt.time(),
        schedule_v1=[
            [None, 0, stations[0]],
            [10, 15, stations[1]],
            [20, None, stations[2]],
        ],
    )
    rtstations = thread.path

    ThreadStationStateFactory.create_from_rtstation(
        rts=rtstations[0],
        thread_start_date=start_dt,
        departure={
            'type': EventStateType.FACT,
            'dt': start_dt + timedelta(minutes=1),
            'minutes_from': 1,
            'minutes_to': 1
        }
    )
    ThreadStationStateFactory.create_from_rtstation(
        rts=rtstations[1],
        thread_start_date=start_dt,
        arrival={
            'type': EventStateType.POSSIBLE_DELAY,
            'dt': start_dt + timedelta(minutes=10),
            'minutes_from': None,
            'minutes_to': None
        },
        departure={
            'type': EventStateType.CANCELLED,
            'dt': start_dt + timedelta(minutes=15),
            'minutes_from': None,
            'minutes_to': None
        }
    )
    ThreadStationStateFactory.create_from_rtstation(
        rts=rtstations[2],
        thread_start_date=start_dt,
        arrival={
            'type': EventStateType.CANCELLED,
            'dt': start_dt + timedelta(minutes=20),
            'minutes_from': None,
            'minutes_to': None
        }
    )

    segments = [
        create_rthread_segment(
            rts_from=rtstations[0],
            rts_to=rtstations[1],
            start_date=start_dt.date()
        ),
        create_rthread_segment(
            rts_from=rtstations[1],
            rts_to=rtstations[2],
            start_date=start_dt.date()
        )
    ]

    states = get_states_for_segments(segments)
    state_1, state_2 = [states[segment] for segment in segments]

    assert_that(state_1, has_properties({
        'from_arrival': None,
        'departure': has_properties({
            'type': EventStateType.FACT,
            'dt': start_dt + timedelta(minutes=1),
            'minutes_from': 1,
            'minutes_to': 1
        }),
        'arrival': has_properties({
            'type': EventStateType.POSSIBLE_DELAY,
            'dt': start_dt + timedelta(minutes=10),
            'minutes_from': None,
            'minutes_to': None
        }),
        'to_departure': has_properties({
            'type': EventStateType.POSSIBLE_DELAY,
            'dt': start_dt + timedelta(minutes=15),
            'minutes_from': None,
            'minutes_to': None
        })
    }))

    assert_that(state_2, has_properties({
        'from_arrival': has_properties({
            'type': EventStateType.POSSIBLE_DELAY,
            'dt': start_dt + timedelta(minutes=10),
            'minutes_from': None,
            'minutes_to': None
        }),
        'departure': has_properties({
            'type': EventStateType.POSSIBLE_DELAY,
            'dt': start_dt + timedelta(minutes=15),
            'minutes_from': None,
            'minutes_to': None
        }),
        'arrival': has_properties({
            'type': EventStateType.POSSIBLE_DELAY,
            'dt': start_dt + timedelta(minutes=20),
            'minutes_from': None,
            'minutes_to': None
        }),
        'to_departure': None
    }))

    states = get_states_for_segments(segments, cancels_as_possible_delay=False)

    assert_that(states[segments[0]], has_properties({
        'from_arrival': None,
        'departure': has_properties({
            'type': EventStateType.FACT,
            'dt': start_dt + timedelta(minutes=1),
            'minutes_from': 1,
            'minutes_to': 1
        }),
        'arrival': has_properties({
            'type': EventStateType.POSSIBLE_DELAY,
            'dt': start_dt + timedelta(minutes=10),
            'minutes_from': None,
            'minutes_to': None
        }),
        'to_departure': has_properties({
            'type': EventStateType.CANCELLED,
            'dt': start_dt + timedelta(minutes=15),
            'minutes_from': None,
            'minutes_to': None
        })
    }))

    assert_that(states[segments[1]], has_properties({
        'from_arrival': has_properties({
            'type': EventStateType.POSSIBLE_DELAY,
            'dt': start_dt + timedelta(minutes=10),
            'minutes_from': None,
            'minutes_to': None
        }),
        'departure': has_properties({
            'type': EventStateType.CANCELLED,
            'dt': start_dt + timedelta(minutes=15),
            'minutes_from': None,
            'minutes_to': None
        }),
        'arrival': has_properties({
            'type': EventStateType.CANCELLED,
            'dt': start_dt + timedelta(minutes=20),
            'minutes_from': None,
            'minutes_to': None
        }),
        'to_departure': None
    }))


def test_get_states_for_thread_with_cancels():
    stations = [create_station() for _ in range(3)]
    start_dt = datetime(2021, 4, 19, 15)
    thread_uid = 'thread_uid'
    thread = create_thread(
        number='thread_number',
        uid=thread_uid,
        tz_start_time=start_dt.time(),
        schedule_v1=[
            [None, 0, stations[0]],
            [10, 15, stations[1]],
            [20, None, stations[2]],
        ],
    )
    rtstations = thread.path

    ThreadStationStateFactory.create_from_rtstation(
        rts=rtstations[0],
        thread_start_date=start_dt,
        departure={
            'type': EventStateType.FACT,
            'dt': start_dt + timedelta(minutes=1),
            'minutes_from': 1,
            'minutes_to': 1
        }
    )
    ThreadStationStateFactory.create_from_rtstation(
        rts=rtstations[1],
        thread_start_date=start_dt,
        arrival={
            'type': EventStateType.POSSIBLE_DELAY,
            'dt': start_dt + timedelta(minutes=10),
            'minutes_from': None,
            'minutes_to': None
        },
        departure={
            'type': EventStateType.CANCELLED,
            'dt': start_dt + timedelta(minutes=15),
            'minutes_from': None,
            'minutes_to': None
        }
    )
    ThreadStationStateFactory.create_from_rtstation(
        rts=rtstations[2],
        thread_start_date=start_dt,
        arrival={
            'type': EventStateType.CANCELLED,
            'dt': start_dt + timedelta(minutes=20),
            'minutes_from': None,
            'minutes_to': None
        }
    )

    thread_state, states_by_rtstation = get_states_for_thread(thread, start_dt, rtstations)
    assert thread_state is None

    states = [states_by_rtstation[rts] for rts in rtstations]
    assert_that(states[0], has_properties({
        'arrival': None,
        'departure': has_properties({
            'type': EventStateType.FACT,
            'dt': start_dt + timedelta(minutes=1),
            'minutes_from': 1,
            'minutes_to': 1
        })
    }))
    assert_that(states[1], has_properties({
        'arrival': has_properties({
            'type': EventStateType.POSSIBLE_DELAY,
            'dt': start_dt + timedelta(minutes=10),
            'minutes_from': None,
            'minutes_to': None
        }),
        'departure': has_properties({
            'type': EventStateType.POSSIBLE_DELAY,
            'dt': start_dt + timedelta(minutes=15),
            'minutes_from': None,
            'minutes_to': None
        })
    }))
    assert_that(states[2], has_properties({
        'arrival': has_properties({
            'type': EventStateType.POSSIBLE_DELAY,
            'dt': start_dt + timedelta(minutes=20),
            'minutes_from': None,
            'minutes_to': None
        }),
        'departure': None
    }))

    thread_state, states_by_rtstation = get_states_for_thread(thread, start_dt, rtstations, cancels_as_possible_delay=False)
    assert thread_state is None

    states = [states_by_rtstation[rts] for rts in rtstations]
    assert_that(states[0], has_properties({
        'arrival': None,
        'departure': has_properties({
            'type': EventStateType.FACT,
            'dt': start_dt + timedelta(minutes=1),
            'minutes_from': 1,
            'minutes_to': 1
        })
    }))
    assert_that(states[1], has_properties({
        'arrival': has_properties({
            'type': EventStateType.POSSIBLE_DELAY,
            'dt': start_dt + timedelta(minutes=10),
            'minutes_from': None,
            'minutes_to': None
        }),
        'departure': has_properties({
            'type': EventStateType.CANCELLED,
            'dt': start_dt + timedelta(minutes=15),
            'minutes_from': None,
            'minutes_to': None
        })
    }))
    assert_that(states[2], has_properties({
        'arrival': has_properties({
            'type': EventStateType.CANCELLED,
            'dt': start_dt + timedelta(minutes=20),
            'minutes_from': None,
            'minutes_to': None
        }),
        'departure': None
    }))


def test_get_cancelled_path_for_segments_fully_cancelled():
    stations = [create_station() for _ in range(4)]
    start_dt = datetime(2021, 4, 19, 15)
    th_number = 'thread_number'
    th_uid = 'thread_uid'

    thread = create_thread(
        number=th_number,
        uid=th_uid,
        tz_start_time=start_dt.time(),
        schedule_v1=[
            [None, 0, stations[0]],
            [10, 15, stations[1]],
            [20, 25, stations[2]],
            [30, None, stations[3]]
        ],
    )
    th_rts = list(thread.path)

    segment = create_rthread_segment(
        rts_from=th_rts[0],
        rts_to=th_rts[-1],
        start_date=start_dt.date()
    )

    th_key = get_thread_suburban_key(th_number, stations[0])
    SuburbanKey.objects.get_or_create(thread=thread, key=th_key)

    ThreadStationStateFactory.create_from_rtstation(
        rts=th_rts[0],
        thread_start_date=start_dt,
        departure={
            'type': EventStateType.CANCELLED,
            'dt': start_dt,
            'minutes_from': None,
            'minutes_to': None
        }
    )

    ThreadStationStateFactory.create_from_rtstation(
        rts=th_rts[1],
        thread_start_date=start_dt,
        arrival={
            'type': EventStateType.CANCELLED,
            'dt': start_dt + timedelta(minutes=10),
            'minutes_from': None,
            'minutes_to': None
        },
        departure={
            'type': EventStateType.CANCELLED,
            'dt': start_dt + timedelta(minutes=15),
            'minutes_from': None,
            'minutes_to': None
        }
    )

    ThreadStationStateFactory.create_from_rtstation(
        rts=th_rts[2],
        thread_start_date=start_dt,
        arrival={
            'type': EventStateType.CANCELLED,
            'dt': start_dt + timedelta(minutes=20),
            'minutes_from': None,
            'minutes_to': None
        },
        departure={
            'type': EventStateType.CANCELLED,
            'dt': start_dt + timedelta(minutes=25),
            'minutes_from': None,
            'minutes_to': None
        }
    )

    ThreadStationStateFactory.create_from_rtstation(
        rts=th_rts[3],
        thread_start_date=start_dt,
        arrival={
            'type': EventStateType.CANCELLED,
            'dt': start_dt + timedelta(minutes=30),
            'minutes_from': None,
            'minutes_to': None
        }
    )

    result = get_cancelled_path_for_segments([segment])
    assert segment in result
    thread_cancels_state = result[segment]
    assert thread_cancels_state.is_fully_cancelled is True
    assert thread_cancels_state.cancelled_segments[0].rtstation_from == th_rts[0]
    assert thread_cancels_state.cancelled_segments[0].rtstation_to == th_rts[-1]


def test_get_cancelled_path_for_segments_partially_cancelled():
    stations = [create_station() for _ in range(4)]
    start_dt = datetime(2021, 4, 19, 15)
    th_number = 'thread_number'
    th_uid = 'thread_uid'

    thread = create_thread(
        number=th_number,
        uid=th_uid,
        tz_start_time=start_dt.time(),
        schedule_v1=[
            [None, 0, stations[0]],
            [10, 15, stations[1]],
            [20, 25, stations[2]],
            [30, None, stations[3]]
        ],
    )
    th_rts = list(thread.path)

    segments = [
        create_rthread_segment(
            rts_from=th_rts[0],
            rts_to=th_rts[1],
            start_date=start_dt.date()
        ),
        create_rthread_segment(
            rts_from=th_rts[0],
            rts_to=th_rts[3],
            start_date=start_dt.date()
        ),
        create_rthread_segment(
            rts_from=th_rts[2],
            rts_to=th_rts[3],
            start_date=start_dt.date()
        )
    ]

    th_key = get_thread_suburban_key(th_number, stations[0])
    SuburbanKey.objects.get_or_create(thread=thread, key=th_key)

    ThreadStationStateFactory.create_from_rtstation(
        rts=th_rts[0],
        thread_start_date=start_dt,
        departure={
            'type': EventStateType.CANCELLED,
            'dt': start_dt,
            'minutes_from': None,
            'minutes_to': None
        }
    )

    ThreadStationStateFactory.create_from_rtstation(
        rts=th_rts[1],
        thread_start_date=start_dt,
        arrival={
            'type': EventStateType.CANCELLED,
            'dt': start_dt + timedelta(minutes=10),
            'minutes_from': None,
            'minutes_to': None
        },
        departure={
            'type': EventStateType.CANCELLED,
            'dt': start_dt + timedelta(minutes=15),
            'minutes_from': None,
            'minutes_to': None
        }
    )

    ThreadStationStateFactory.create_from_rtstation(
        rts=th_rts[2],
        thread_start_date=start_dt,
        arrival={
            'type': EventStateType.CANCELLED,
            'dt': start_dt + timedelta(minutes=20),
            'minutes_from': None,
            'minutes_to': None
        },
        departure={
            'type': EventStateType.POSSIBLE_DELAY,
            'dt': start_dt + timedelta(minutes=25),
            'minutes_from': 1,
            'minutes_to': 1
        }
    )

    ThreadStationStateFactory.create_from_rtstation(
        rts=th_rts[3],
        thread_start_date=start_dt,
        arrival={
            'type': EventStateType.POSSIBLE_DELAY,
            'dt': start_dt + timedelta(minutes=30),
            'minutes_from': 2,
            'minutes_to': 2
        }
    )

    result = get_cancelled_path_for_segments(segments)
    assert len(result) == 3

    for segment in segments:
        thread_cancels_state = result[segment]
        assert thread_cancels_state.is_fully_cancelled is False
        assert thread_cancels_state.cancelled_segments[0].rtstation_from == th_rts[0]
        assert thread_cancels_state.cancelled_segments[0].rtstation_to == th_rts[2]
