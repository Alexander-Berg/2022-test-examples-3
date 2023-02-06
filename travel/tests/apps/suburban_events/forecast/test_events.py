# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime

import pytest
from hamcrest import assert_that, has_properties

from common.apps.suburban_events.factories import ThreadEventsFactory
from common.apps.suburban_events.forecast.events import prepare_events, prepare_mczk_events, save_prepared_events, Event
from common.apps.suburban_events.models import ThreadEvents, SuburbanKey, ThreadKey
from common.apps.suburban_events.utils import ThreadEventsTypeCodes, get_rtstation_key
from common.models.schedule import RTStation
from common.tester.factories import create_thread, create_station

create_thread = create_thread.mutate(t_type="suburban")
create_station = create_station.mutate(t_type="suburban")


class EventForTest(Event):
    def __init__(self, data):
        self.data = data

    @property
    def rtstation(self):
        return self.data['rts']

    @property
    def thread(self):
        return self.data['thread']

    @property
    def thread_start_date(self):
        return self.data['thread_start_date']

    @property
    def dt_normative(self):
        return self.data['dt_normative']

    @property
    def dt_fact(self):
        return self.data['dt_fact']

    @property
    def twin_key(self):
        return self.data['twin_key']

    @property
    def passed_several_times(self):
        return self.data['passed_several_times']

    @property
    def type(self):
        return self.data['type']

    @property
    def weight(self):
        return self.data['weight']

    @property
    def time(self):
        return getattr(self.rtstation, 'tz_' + self.type)

    @property
    def station_key(self):
        return get_rtstation_key(self.rtstation)


class MCZKEventForTest(EventForTest):
    @property
    def th_event(self):
        return self.data['th_event']

    @property
    def station_key(self):
        return self.data['station_key']

    @property
    def time(self):
        return self.data['time']


@pytest.mark.mongouser
@pytest.mark.dbuser
def test_save_events():
    station_from, station_to = create_station(id=121), create_station(id=122)
    station_mid_1, station_mid_2 = create_station(id=123), create_station(id=124)

    start_date_1 = datetime(2017, 3, 20, 13)
    start_date_2 = datetime(2017, 3, 25, 15)

    thread_1 = create_thread(
        tz_start_time=start_date_1.time(),
        schedule_v1=[
            [None, 0, station_from],
            [10, 20, station_mid_1],
            [50, None, station_to],
        ],
    )

    thread_2 = create_thread(
        tz_start_time=start_date_2.time(),
        schedule_v1=[
            [None, 0, station_from],
            [20, 25, station_mid_1],
            [35, 40, station_mid_2],
            [50, None, station_to],
        ],
    )

    thread_1_key, thread_2_key = 'th_1_key', 'th_2_key'
    SuburbanKey.objects.create(thread=thread_1, key=thread_1_key)
    SuburbanKey.objects.create(thread=thread_2, key=thread_2_key)

    ThreadEventsFactory(**{
        'key': ThreadKey(
            thread_start_date=start_date_1,
            thread_key=thread_1_key,
            thread_type=ThreadEventsTypeCodes.SUBURBAN,
            clock_direction=None
        ),
        'stations_events': []
    })

    ThreadEventsFactory(**{
        'key': ThreadKey(
            thread_start_date=start_date_2,
            thread_key=thread_2_key,
            thread_type=ThreadEventsTypeCodes.SUBURBAN,
            clock_direction=None
        ),
        'stations_events': []
    })

    rts_th_1_mid_1 = RTStation.objects.get(thread=thread_1, station=station_mid_1)
    rts_th_2_mid_1 = RTStation.objects.get(thread=thread_2, station=station_mid_1)
    rts_th_2_mid_2 = RTStation.objects.get(thread=thread_2, station=station_mid_2)

    prepared_events = prepare_events([
        EventForTest({
            'rts': rts_th_1_mid_1,
            'thread': thread_1,
            'thread_start_date': start_date_1,
            'dt_normative': datetime(2017, 3, 20, 13, 10),
            'dt_fact': datetime(2017, 3, 20, 13, 14),
            'twin_key': 'number_1',
            'passed_several_times': False,
            'type': 'arrival',
            'weight': 0.1
        }),
        EventForTest({
            'rts': rts_th_2_mid_1,
            'thread': thread_2,
            'thread_start_date': start_date_2,
            'dt_normative': datetime(2017, 3, 25, 15, 20),
            'dt_fact': datetime(2017, 3, 25, 15, 23),
            'twin_key': 'number_2',
            'passed_several_times': False,
            'type': 'arrival',
            'weight': 0.1
        }),
    ])
    save_prepared_events(prepared_events)

    thread_events = list(ThreadEvents.objects.all().order_by('key.thread_key'))
    assert len(thread_events) == 2

    st_events = thread_events[0].stations_events
    assert len(st_events) == 1
    assert_that(st_events[0],
                has_properties({
                    'dt_fact': datetime(2017, 3, 20, 13, 14),
                    'dt_normative': datetime(2017, 3, 20, 13, 10),
                    'weight': 0.1,
                    'twin_key': u'number_1',
                    'passed_several_times': False,
                    'time': 10,
                    'station_key': u'123',
                    'type': u'arrival'
                }))

    st_events = thread_events[1].stations_events
    assert len(st_events) == 1
    assert_that(st_events[0],
                has_properties({
                    'dt_fact': datetime(2017, 3, 25, 15, 23),
                    'dt_normative': datetime(2017, 3, 25, 15, 20),
                    'weight': 0.1,
                    'twin_key': u'number_2',
                    'passed_several_times': False,
                    'time': 20,
                    'station_key': u'123',
                    'type': u'arrival'
                }))

    # Добавляем еще одно событие к thread_2.
    prepared_events = prepare_events([
        EventForTest({
            'rts': rts_th_2_mid_2,
            'thread': thread_2,
            'thread_start_date': start_date_2,
            'dt_normative': datetime(2017, 3, 25, 15, 40),
            'dt_fact': datetime(2017, 3, 25, 15, 45),
            'twin_key': 'number_2',
            'passed_several_times': False,
            'type': 'departure',
            'weight': 0.1
        })
    ])
    save_prepared_events(prepared_events)

    thread_events = list(ThreadEvents.objects.all().order_by('key.thread_key'))

    st_events = thread_events[1].stations_events
    assert len(st_events) == 2
    assert_that(st_events[1],
                has_properties({
                    'dt_fact': datetime(2017, 3, 25, 15, 45),
                    'dt_normative': datetime(2017, 3, 25, 15, 40),
                    'weight': 0.1,
                    'twin_key': u'number_2',
                    'passed_several_times': False,
                    'time': 40,
                    'station_key': u'124',
                    'type': u'departure'
                }))


@pytest.mark.mongouser
@pytest.mark.dbuser
def test_save_events_without_suburban_key():
    station_from, station_to = create_station(id=121), create_station(id=122)
    start_date = datetime(2017, 3, 20, 13)
    thread_key = 'th_1_key'

    thread = create_thread(
        schedule_v1=[
            [None, 0, station_from],
            [50, None, station_to],
        ],
    )
    rts_from = RTStation.objects.get(thread=thread, station=station_from)
    event = EventForTest({
        'rts': rts_from,
        'thread': thread,
        'thread_start_date': start_date,
        'dt_normative': datetime(2017, 3, 10, 13, 10),
        'dt_fact': datetime(2017, 3, 10, 13, 14),
        'twin_key': 'number_1',
        'passed_several_times': False,
        'type': 'departure',
        'weight': 0.1
    })

    ThreadEventsFactory(**{
        'key': ThreadKey(
            thread_start_date=start_date,
            thread_key=thread_key,
            thread_type=ThreadEventsTypeCodes.SUBURBAN,
            clock_direction=None
        ),
    })

    save_prepared_events(prepare_events([event]))
    thread_events = list(ThreadEvents.objects.all())
    assert len(thread_events) == 1
    assert len(thread_events[0].stations_events) == 0

    SuburbanKey.objects.create(thread=thread, key=thread_key)
    save_prepared_events(prepare_events([event]))
    thread_events = list(ThreadEvents.objects.all())
    assert len(thread_events) == 1
    assert len(thread_events[0].stations_events) == 1


@pytest.mark.mongouser
@pytest.mark.dbuser
def test_save_mczk_events():
    start_date = datetime(2017, 11, 12, 13)
    thread_1_key, thread_2_key = 'th_1_key', 'th_2_key'

    th_events_1 = ThreadEventsFactory(**{
        'key': ThreadKey(
            thread_start_date=start_date,
            thread_key=thread_1_key,
            thread_type=ThreadEventsTypeCodes.MCZK,
            clock_direction=1
        ),
    })

    th_events_2 = ThreadEventsFactory(**{
        'key': ThreadKey(
            thread_start_date=start_date,
            thread_key=thread_2_key,
            thread_type=ThreadEventsTypeCodes.MCZK,
            clock_direction=0
        ),
    })

    event_1 = MCZKEventForTest({
        'station_key': '121',
        'th_event': th_events_1,
        'time': '50',
        'thread_start_date': start_date,
        'dt_normative': datetime(2017, 11, 12, 13, 10),
        'dt_fact': datetime(2017, 11, 12, 13, 14),
        'twin_key': 'number_1',
        'passed_several_times': False,
        'type': 'departure',
        'weight': 0.1
    })

    event_2 = MCZKEventForTest({
        'station_key': '122',
        'time': '40',
        'th_event': th_events_2,
        'thread_start_date': start_date,
        'dt_normative': datetime(2017, 11, 12, 13, 10),
        'dt_fact': datetime(2017, 11, 12, 13, 14),
        'twin_key': 'number_2',
        'passed_several_times': False,
        'type': 'arrival',
        'weight': 0.1
    })

    save_prepared_events(prepare_mczk_events([event_1, event_2]))
    thread_events = list(ThreadEvents.objects.all().order_by('key.thread_key'))
    assert len(thread_events) == 2
    assert len(thread_events[0].stations_events) == 1
    assert_that(thread_events[0].stations_events[0],
                has_properties({
                    'dt_normative': datetime(2017, 11, 12, 13, 10),
                    'dt_fact': datetime(2017, 11, 12, 13, 14),
                    'weight': 0.1,
                    'twin_key': u'number_1',
                    'passed_several_times': False,
                    'time': 50,
                    'station_key': u'121',
                    'type': u'departure'
                }))
    assert len(thread_events[1].stations_events) == 1
