# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import time, datetime, timedelta

import pytest
import six
from pytz import UTC

from common.apps.suburban_events.api import EventState
from common.apps.suburban_events.utils import EventStateType
from common.tester.factories import create_thread, create_rtstation, create_station
from common.utils.date import MSK_TZ, RunMask
from travel.rasp.library.python.common23.date.environment import now
from common.views.thread import calc_thread_start_date, get_event_status, get_event_message, get_event_state_minutes


DEPARTURE_DT = datetime(2016, 1, 1, 11, 0)


@pytest.mark.parametrize("departure_from,tz_departure,station_tz,expected", [
    # выезд со станции в дату старта нитки
    (DEPARTURE_DT, 60, MSK_TZ, DEPARTURE_DT.date()),

    # выезд со станции на следующий день после старта нитки
    (DEPARTURE_DT - timedelta(hours=1), 1440, MSK_TZ, DEPARTURE_DT.date()),

    # выезд со станции на следующий день после старта нитки,
    # станция во временной зоне отличной от нитки
    (DEPARTURE_DT - timedelta(hours=1), 1440, UTC, None),

    # не нашли соответствующей даты старты нитки (разница во времени 1час)
    (DEPARTURE_DT, 1440, MSK_TZ, None),
])
@pytest.mark.dbuser
def test_calculate_thread_departure(departure_from, tz_departure, station_tz, expected):
    thread = create_thread(tz_start_time=time(10, 0))
    station_from = create_station(time_zone=station_tz.zone)
    for i, station in enumerate([station_from, station_from]):
        create_rtstation(thread=thread, station=station, tz_departure=tz_departure * i)

    departure_date = calc_thread_start_date(thread, station_from, departure_from)
    assert departure_date == expected


@pytest.mark.dbuser
def test_calculate_thread_departure_no_station():
    thread = create_thread()
    station_from = create_station()
    departure_from = now()
    departure_date = calc_thread_start_date(thread, station_from, departure_from)
    assert departure_date is None


@pytest.mark.dbuser
def test_calculate_thread_departure_thread_not_running():
    thread = create_thread(year_days='0' * RunMask.MASK_LENGTH, tz_start_time=time(10, 0))
    station_from = create_station()
    create_rtstation(thread=thread, station=station_from, tz_departure=60)
    departure_date = calc_thread_start_date(thread, station_from, DEPARTURE_DT)
    assert departure_date is None


@pytest.mark.parametrize('event_state_type, minutes, expected_status', [
    (EventStateType.POSSIBLE_DELAY, None, 'possible-delay'),
    (EventStateType.FACT, 2, 'fact-delay'),
    (EventStateType.FACT, 1, 'ok'),
    (EventStateType.FACT, 0, 'ok'),
    ('UNKNOWN', 10, None)
])
def test_get_event_status(event_state_type, minutes, expected_status):
    assert get_event_status(event_state_type, minutes) == expected_status


@pytest.mark.parametrize("event, minutes_from, minutes_to, status, nonstop, expected", [
    ('departure', None, None, 'ok', False, u'по расписанию'),
    ('arrival', None, None, 'ok', False, u'по расписанию'),
    ('departure', None, None, 'possible-delay', False, u'возможно опоздание'),
    ('departure', 3, 3, 'possible-delay', False, u'возможно опоздание на 3 мин.'),
    ('departure', 3, 5, 'possible-delay', False, u'возможно опоздание на 3 \N{em dash} 5 мин.'),
    ('arrival', None, None, 'possible-delay', False, u'возможно опоздание'),
    ('arrival', 3, 3, 'possible-delay', False, u'возможно опоздание на 3 мин.'),
    ('arrival', 3, 5, 'possible-delay', False, u'возможно опоздание на 3 \N{em dash} 5 мин.'),
    ('departure', None, None, 'fact-delay', False, u'отправился с опозданием'),
    ('departure', 3, 3, 'fact-delay', False, u'отправился с опозданием на 3 мин.'),
    ('departure', 3, 5, 'fact-delay', False, u'отправился с опозданием на 5 мин.'),
    ('arrival', None, None, 'fact-delay', False, u'прибыл с опозданием'),
    ('arrival', 3, 3, 'fact-delay', False, u'прибыл с опозданием на 3 мин.'),
    ('arrival', 3, 5, 'fact-delay', False, u'прибыл с опозданием на 5 мин.'),
    ('departure', 3, 3, 'fact-delay', True, u'опаздывает на 3 мин.'),
    ('arrival', 3, 3, 'fact-delay', True, u'опаздывает на 3 мин.'),
])
def test_get_event_message(event, minutes_from, minutes_to, status, nonstop, expected):
    message_as_list = get_event_message(event, minutes_from, minutes_to, status, nonstop)
    assert ''.join(six.text_type(s) for s in message_as_list) == expected


def test_get_event_state_minutes():
    local_dt = MSK_TZ.localize(datetime(2017, 11, 15, hour=13, minute=40))
    event_state = EventState(type_=EventStateType.POSSIBLE_DELAY,
                             minutes_from=10,
                             minutes_to=15,
                             dt=datetime(2017, 11, 15, hour=15, minute=45),
                             tz='Asia/Yekaterinburg')
    assert get_event_state_minutes(local_dt, event_state) == (5, 5)
