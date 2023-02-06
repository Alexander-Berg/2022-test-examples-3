# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import json
from datetime import datetime, time

import freezegun
import pytest
from django.test import Client
from hamcrest import assert_that, contains_inanyorder, has_entries

from common.apps.suburban_events.factories import ThreadStationStateFactory, EventStateFactory, ThreadStationKeyFactory
from common.apps.suburban_events.models import SuburbanKey
from common.apps.suburban_events.utils import get_rtstation_key
from common.tester.factories import create_thread, create_station, RTStation


create_thread = create_thread.mutate(t_type="suburban", __={'calculate_noderoute': True})
create_station = create_station.mutate(t_type="suburban")


def generate_threads_with_events():
    station_from, station_to = create_station(id=121), create_station(id=122)
    station_mid_1, station_mid_2 = create_station(id=123), create_station(id=124)

    start_date_1 = datetime(2017, 3, 20, 17)
    start_date_2 = datetime(2017, 3, 25, 11)

    thread_1 = create_thread(
        uid=u'uid_1',
        number=u'number_1',
        id=500,
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
        id=600,
        tz_start_time=time(11),
        schedule_v1=[
            [None, 0, station_from],
            [24, 25, station_mid_1],
            [35, 40, station_mid_2],
            [50, None, station_to],
        ],
    )

    sub_key_1 = SuburbanKey.objects.create(thread=thread_1, key='thread_1Ш_key')
    sub_key_2 = SuburbanKey.objects.create(thread=thread_2, key='thread_2Ш_key')
    rts_th_1_from = RTStation.objects.get(thread=thread_1, station=station_from)
    rts_th_1_mid_1 = RTStation.objects.get(thread=thread_1, station=station_mid_1)
    rts_th_1_to = RTStation.objects.get(thread=thread_1, station=station_to)
    rts_th_2_mid_2 = RTStation.objects.get(thread=thread_2, station=station_mid_2)

    ThreadStationStateFactory(
        key=ThreadStationKeyFactory(
            thread_key=sub_key_1.key,
            thread_start_date=start_date_1,
            station_key=get_rtstation_key(rts_th_1_from),
            arrival=None,
            departure=0
        ),
        departure_state=EventStateFactory(
            dt=datetime(2017, 3, 20, 17, 3),
            type='fact',
            thread_uid='uid_1',
            minutes_from=3,
            minutes_to=3
        ),
        passed_several_times=False,
        departure=rts_th_1_from.tz_departure,

        tz=rts_th_1_to.station.time_zone,
    )

    ThreadStationStateFactory(
        key=ThreadStationKeyFactory(
            thread_key=sub_key_1.key,
            thread_start_date=start_date_1,
            station_key=get_rtstation_key(rts_th_1_mid_1),
            arrival=10,
            departure=20
        ),
        arrival_state=EventStateFactory(
            thread_uid='uid_1',
            type='possible_delay',
        ),
        departure_state=EventStateFactory(
            thread_uid='uid_1',
            type='possible_delay',
        ),
        arrival=rts_th_1_mid_1.tz_arrival,
        departure=rts_th_1_mid_1.tz_departure,
        tz=rts_th_1_mid_1.station.time_zone,
        passed_several_times=False
    )

    ThreadStationStateFactory(
        key=ThreadStationKeyFactory(
            thread_key=sub_key_2.key,
            thread_start_date=start_date_2,
            station_key=get_rtstation_key(rts_th_2_mid_2),
            arrival=35,
            departure=40
        ),
        arrival_state=EventStateFactory(
            dt=datetime(2017, 3, 25, 11, 37),
            type='fact',
            thread_uid='uid_2',
            minutes_from=2,
            minutes_to=2
        ),
        passed_several_times=False,
        arrival=rts_th_2_mid_2.tz_arrival,
        departure=rts_th_2_mid_2.tz_departure,
        tz=rts_th_2_mid_2.station.time_zone,
    )
    return station_mid_1, station_mid_2, station_to


@pytest.mark.mongouser
@pytest.mark.dbuser
@freezegun.freeze_time('2017-03-20')
def test_suburban_search_event_keys():
    station_mid_1, station_mid_2, station_to = generate_threads_with_events()

    response = Client().get('/ru/search/search/', {
        'pointFrom': station_mid_1.point_key,
        'pointTo': station_to.point_key,
        'when': '2017-03-20',
        'transportType': 'suburban'
    })

    result = json.loads(response.content)

    assert 'result' in result

    assert_that(result['result']['segments'], contains_inanyorder(
        has_entries({
            'number': 'number_1',
            'departureEvent': has_entries({'minutesFromNew': None, 'minutesToNew': None,
                                           'minutesFrom': 0, 'minutesTo': 0,
                                           'type': 'possible_delay'}),
            'arrivalEvent': None,
        }),
        has_entries({
            'number': 'number_2',
            'departureEvent': None,
            'arrivalEvent': None,
        })
    ))
    for segment in result['result']['segments']:
        assert segment['departureEventKey']
        assert segment['arrivalEventKey']

    response = Client().get('/ru/search/search/', {
        'pointFrom': station_mid_1.point_key,
        'pointTo': station_mid_2.point_key,
        'when': '2017-03-25',
        'transportType': 'suburban'
    })

    result = json.loads(response.content)

    assert_that(result['result']['segments'], contains_inanyorder(
        has_entries({
            'number': 'number_2',
            'departureEvent': None,
            'arrivalEvent': has_entries({'minutesFrom': 2, 'minutesTo': 2,
                                         'minutesFromNew': 2, 'minutesToNew': 2,
                                         'type': u'fact'}),
        }),
    ))
    for segment in result['result']['segments']:
        assert segment['departureEventKey']
        assert segment['arrivalEventKey']
