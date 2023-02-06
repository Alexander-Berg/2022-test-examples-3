# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime

import mock

from common.apps.suburban_events.api import SegmentEventState, SegmentStates
from common.apps.suburban_events.utils import EventStateType
from common.utils.date import MSK_TZ, MSK_TIMEZONE
from route_search.models import RThreadSegment

from travel.rasp.morda_backend.morda_backend.search.search.data_layer import backend
from travel.rasp.morda_backend.morda_backend.search.search.data_layer.backend import log, fill_suburban_events


SEGMENT_DEPARTURE = MSK_TZ.localize(datetime(2017, 11, 15, hour=13, minute=40))
SEGMENT_ARRIVAL = MSK_TZ.localize(datetime(2017, 11, 16, hour=21, minute=20))


def _get_segment():
    segment = RThreadSegment()
    segment.departure = SEGMENT_DEPARTURE
    segment.arrival = SEGMENT_ARRIVAL
    return segment


@mock.patch.object(log, 'exception', autospec=True)
@mock.patch.object(backend, 'get_states_for_segments', side_effect=Exception('Unknown error'), autospec=True)
def test_api_failed(m_get_states_for_segments, m_exception):
    segment = _get_segment()

    fill_suburban_events([segment])

    m_get_states_for_segments.assert_called_once_with([segment], cancels_as_possible_delay=False)
    m_exception.assert_called_once_with(mock.ANY)
    assert segment.departure_event is None
    assert segment.arrival_event is None


@mock.patch.object(backend, 'get_states_for_segments', return_value={}, autospec=True)
def test_no_events_info(m_get_states_for_segments):
    segment = _get_segment()

    fill_suburban_events([segment])

    m_get_states_for_segments.assert_called_once_with([segment], cancels_as_possible_delay=False)
    assert segment.departure_event is None
    assert segment.arrival_event is None


@mock.patch.object(backend, 'get_states_for_segments', autospec=True)
def test_events_info_without_dt(m_get_states_for_segments):
    segment = _get_segment()

    segment_states = SegmentStates()
    segment_states.departure = SegmentEventState(
        key='dep_key',
        type_=EventStateType.POSSIBLE_DELAY,
        minutes_from=10,
        minutes_to=20,
        tz=MSK_TZ,
        dt=None)
    segment_states.arrival = SegmentEventState(
        key='arr_key',
        type_=EventStateType.POSSIBLE_DELAY,
        minutes_from=5,
        minutes_to=7,
        tz=MSK_TZ,
        dt=None)

    m_get_states_for_segments.return_value = {segment: segment_states}

    fill_suburban_events([segment])

    m_get_states_for_segments.assert_called_once_with([segment], cancels_as_possible_delay=False)
    _check_event_state(segment.departure_event, 'dep_key', EventStateType.POSSIBLE_DELAY, 10, 20)
    _check_event_state(segment.arrival_event, 'arr_key', EventStateType.POSSIBLE_DELAY, 5, 7)


@mock.patch.object(backend, 'get_states_for_segments', autospec=True)
def test_events_info_with_dt(m_get_states_for_segments):
    segment = _get_segment()

    segment_states = SegmentStates()
    segment_states.departure = SegmentEventState(
        key='dep_key',
        type_=EventStateType.POSSIBLE_DELAY,
        minutes_from=10,
        minutes_to=20,
        tz='Asia/Yekaterinburg',
        dt=datetime(2017, 11, 15, hour=15, minute=45))
    segment_states.arrival = SegmentEventState(
        key='arr_key',
        type_=EventStateType.POSSIBLE_DELAY,
        minutes_from=5,
        minutes_to=7,
        tz='Asia/Omsk',
        dt=datetime(2017, 11, 17, hour=0, minute=22))

    m_get_states_for_segments.return_value = {segment: segment_states}

    fill_suburban_events([segment])

    m_get_states_for_segments.assert_called_once_with([segment], cancels_as_possible_delay=False)
    _check_event_state(segment.departure_event, 'dep_key', EventStateType.POSSIBLE_DELAY, 10, 20)
    _check_event_state(segment.arrival_event, 'arr_key', EventStateType.POSSIBLE_DELAY, 5, 7)


@mock.patch.object(backend, 'get_states_for_segments', autospec=True)
def test_events_info_with_dt_departure_only(m_get_states_for_segments):
    segment = _get_segment()
    segment.arrival = None

    segment_states = SegmentStates()
    segment_states.departure = SegmentEventState(
        key='dep_key',
        type_=EventStateType.POSSIBLE_DELAY,
        minutes_from=10,
        minutes_to=20,
        tz=MSK_TIMEZONE,
        dt=datetime(2017, 11, 15, hour=13, minute=45))

    m_get_states_for_segments.return_value = {segment: segment_states}

    fill_suburban_events([segment])

    m_get_states_for_segments.assert_called_once_with([segment], cancels_as_possible_delay=False)
    _check_event_state(segment.departure_event, 'dep_key', EventStateType.POSSIBLE_DELAY, 10, 20)
    assert segment.arrival_event is None


@mock.patch.object(backend, 'get_states_for_segments', autospec=True)
def test_events_with_cancels(m_get_states_for_segments):
    segment = _get_segment()
    segment.arrival = None

    segment_states = SegmentStates()
    segment_states.departure = SegmentEventState(
        key='dep_key',
        type_=EventStateType.CANCELLED
    )
    m_get_states_for_segments.return_value = {segment: segment_states}

    fill_suburban_events([segment])

    m_get_states_for_segments.assert_called_once_with([segment], cancels_as_possible_delay=False)
    _check_event_state(segment.departure_event, 'dep_key', EventStateType.CANCELLED, None, None)
    assert segment.arrival_event is None


def _check_event_state(event_state, expexted_key, expected_type, expected_minutes_from, expected_minutes_to):
    assert event_state.key == expexted_key
    assert event_state.type == expected_type
    assert event_state.minutes_from == expected_minutes_from
    assert event_state.minutes_to == expected_minutes_to
