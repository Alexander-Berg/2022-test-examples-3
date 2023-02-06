# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime

from common.apps.suburban_events.api import EventState, EventStateType
from travel.rasp.export.export.v3.core.suburban_events import get_event_state_dict


def test_get_event_state_dict():
    dt = datetime(2019, 6, 20, 13, 42, 14)

    event = EventState(
        type_=EventStateType.FACT, minutes_from=10, minutes_to=20, dt=dt, tz='Asia/Yekaterinburg')
    event_dict = get_event_state_dict(event)
    assert event_dict == {
        'type': 'fact',
        'fact_time': '2019-06-20T13:42:14+05:00',
        'minutes_from': 10,
        'minutes_to': 20,
    }

    event = EventState(
        type_=EventStateType.POSSIBLE_DELAY, minutes_from=10, minutes_to=20, dt=dt, tz='Asia/Yekaterinburg')
    event_dict = get_event_state_dict(event)
    assert event_dict == {
        'type': 'possible_delay',
        'minutes_from': 10,
        'minutes_to': 20,
    }

    event = EventState(
        type_=EventStateType.FACT_INTERPOLATED, minutes_from=10, minutes_to=20, dt=dt, tz='Asia/Yekaterinburg')
    event_dict = get_event_state_dict(event)
    assert event_dict == {
        'type': 'fact_interpolated',
        'minutes_from': 10,
        'minutes_to': 20,
    }

    event = EventState(
        type_=EventStateType.UNDEFINED, minutes_from=10, minutes_to=20, dt=dt, tz='Asia/Yekaterinburg')
    event_dict = get_event_state_dict(event)
    assert event_dict == {
        'type': 'undefined',
    }
