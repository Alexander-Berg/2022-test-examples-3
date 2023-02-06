# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime

import json
import mock
import pytest

from common.apps.suburban_events import models
from common.apps.suburban_events.factories import ThreadEventsFactory
from common.apps.suburban_events.forecast.events import Event
from common.apps.suburban_events.logs import log_suburban_events, log_rzd_raw_data
from common.apps.suburban_events.utils import EventStateType, get_thread_suburban_key, get_rtstation_key
from common.tester.factories import create_thread, create_station
from common.utils.marketstat import JsonLog


def create_thread_station_state(rts, **kwargs):
    thread_start_date = kwargs.get('thread_start_date')
    if not thread_start_date:
        thread_start_date = datetime.now().replace(hour=0, minute=0, second=0, microsecond=0)

    tss = models.ThreadStationState(
        key=models.ThreadStationKey(
            thread_key=get_thread_suburban_key(rts.thread.number, rts.thread.path[0].station),
            thread_start_date=thread_start_date,
            station_key=get_rtstation_key(rts),
        ),
        tz=kwargs.get('tz', 'Europe/Moscow'),
    )
    tss.rts = rts

    if rts.tz_arrival is not None:
        tss.arrival = kwargs.get('arrival', 1)

        arrival_state = kwargs.get('arrival_state')
        if not arrival_state:
            arrival_state = {
                'type': EventStateType.FACT,
                'dt': datetime.now()
            }

        tss.set_arrival_state(models.EventState(**arrival_state))

    if rts.tz_departure is not None:
        tss.departure = kwargs.get('departure', 2)

        departure_state = kwargs.get('departure_state')
        if not departure_state:
            departure_state = {
                'type': EventStateType.POSSIBLE_DELAY,
                'minutes_from': 10,
                'minutes_to': 15,
            }

        tss.set_departure_state(models.EventState(**departure_state))

    return tss


@pytest.mark.dbuser
def test_log_suburban_events():
    station1, station2, station3 = [
        create_station(id=i, title='st{}{}'.format(i, i))
        for i in range(21, 24)
    ]

    thread = create_thread(
        uid='aa123456',
        title='thread_title_42',
        number='6667',
        schedule_v1=[
            [None, 11, station1],
            [325, 330, station2],
            [331, 331],
            [350, None, station3]
        ],
    )
    th_event = ThreadEventsFactory(thread=thread)

    rts1, rts2, rts3, rts4 = thread.path

    rts1.departure_subdir = u'на Екатеринбург'

    rts2.time_zone = 'Asia/Yekaterinburg'
    rts2.departure_subdir = u'на Москву'

    tss1 = create_thread_station_state(
        rts1,
        thread_start_date=datetime(2042, 12, 30),
        departure_state={
            'type': EventStateType.FACT,
            'sub_type': 'chaos',
            'trigger': 'godzilla',
        }
    )
    tss2 = create_thread_station_state(
        rts2,
        thread_start_date=datetime(2042, 12, 30),
        arrival_state={
            'type': EventStateType.FACT,
            'dt': datetime(2010, 10, 28, 13, 42, 44, 100500)
        },
        departure_state={
            'type': EventStateType.POSSIBLE_DELAY,
            'time_from_delay_station': 42,
            'forecast_dt': datetime(2017, 3, 28, 16, 30),
            'minutes_from': 142,
            'minutes_to': 242,
            'sub_type': 'no_data',
            'trigger': '123',
        }
    )
    tss3 = create_thread_station_state(rts3)
    tss4 = create_thread_station_state(
        rts4,
        thread_start_date=datetime(2042, 12, 30),
        arrival_state={
            'type': EventStateType.POSSIBLE_DELAY,
        },
    )

    for tss in [tss1, tss2, tss3, tss4]:
        tss.th_event = th_event

    class MyEvent(Event):
        @property
        def dt_normative(self):
            return datetime(2222, 12, 22, 22, 22, 22)

    tss1.departure_state.original_event = MyEvent()

    m_writer = mock.Mock()
    m_writer.__enter__ = mock.Mock(return_value=(mock.Mock(), None))
    m_writer.__exit__ = mock.Mock(return_value=None)
    m_writer.write = mock.Mock()

    with mock.patch('common.apps.suburban_events.logs.create_log_broker_writer', return_value=m_writer):
        log_suburban_events(
            'some_forecast_uid',
            datetime(2111, 10, 11, 12, 13, 14),
            [tss1, tss2, tss3, tss4],
        )

        data1, data2_1, data2_2, data3_1, data3_2, data4 = [json.loads(c[0][0]) for c in m_writer.write.call_args_list]

        common_expected = {
            'eventtime': '2111-10-11 12:13:14',
            'forecast_uid': 'some_forecast_uid',
            'thread_key': '6667__21',
            'thread_start_date': '2042-12-30',
            'first_station_departure': '2042-12-30 00:11:00',
            'last_station_arrival': '2042-12-30 05:50:00',
            'thread_title': 'thread_title_42',
            'thread_uid': 'aa123456',
            'event_cause': {}
        }

        assert data1 == dict(common_expected, **{
            'station_key': '21',
            'station_title': 'st2121',
            'action_type': 'departure',
            'event_type': 'fact',
            'event_normative_time': '2042-12-30 00:11:00',
            'event_rzd_normative_time': '2222-12-22 22:22:22',
            'event_cause': {
                'sub_type': 'chaos',
                'trigger': 'godzilla'
            },
            'departure_subdir': u'на Екатеринбург',
        })

        assert data2_1 == dict(common_expected, **{
            'station_key': '22',
            'station_title': 'st2222',
            'action_type': 'arrival',
            'event_type': 'fact',
            'event_fact_time': '2010-10-28 13:42:44',
            'event_normative_time': '2042-12-30 03:25:00',
            'departure_subdir': u'на Москву',

        })

        assert data2_2 == dict(common_expected, **{
            'event_forecast_time': '2017-03-28 16:30:00',
            'station_key': '22',
            'station_title': 'st2222',
            'action_type': 'departure',
            'event_type': 'possible_delay',
            'event_minutes_from': 142,
            'event_minutes_to': 242,
            'event_cause': {
                'sub_type': 'no_data',
                'trigger': '123',
                'time_from_delay_station': 42
            },
            'event_normative_time': '2042-12-30 03:30:00',
            'departure_subdir': u'на Москву',
        })

        assert data3_1['no_stop']
        assert data3_2['no_stop']

        assert data4 == dict(common_expected, **{
            'station_key': '23',
            'station_title': 'st2323',
            'action_type': 'arrival',
            'event_type': 'possible_delay',
            'event_normative_time': '2042-12-30 05:50:00',
            'departure_subdir': None,
        })


def test_log_rzd_raw_data():
    with mock.patch.object(JsonLog, 'log') as m_log:
        log_rzd_raw_data(
            'some_uid',
            datetime(2111, 10, 11, 12, 13, 14),
            [{'a': 1}, {'b': 2}, {'c': 3, 'dt': datetime(2042, 11, 12, 13, 14, 15)}]
        )

        data1, data2, data3 = [c[0][0] for c in m_log.call_args_list]

        common_data = {
            'fetch_uid': 'some_uid',
            'eventtime': '2111-10-11 12:13:14',
        }

        assert data1 == dict(common_data, **{'a': 1})
        assert data2 == dict(common_data, **{'b': 2})
        assert data3 == dict(common_data, **{'c': 3, 'dt': '2042-11-12 13:14:15'})
