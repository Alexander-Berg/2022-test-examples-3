# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import datetime

import mock
import pytest

from common.apps.suburban_events.api import CancelledSegment, SegmentEventState, ThreadCancelsState
from common.apps.suburban_events.utils import EventStateType
from common.tester.factories import create_rthread_segment, create_station, create_thread

from travel.rasp.morda_backend.morda_backend.search.search.data_layer import backend
from travel.rasp.morda_backend.morda_backend.search.search.data_layer.backend import fill_thread_cancels, log

create_thread = create_thread.mutate(t_type="suburban")
create_station = create_station.mutate(t_type="suburban")

pytestmark = [pytest.mark.dbuser, pytest.mark.mongouser]


def _get_segment(departure_event=None, arrival_event=None):
    start_dt = datetime(2021, 4, 19, 16, 0)
    stations = [create_station() for _ in range(4)]
    thread = create_thread(
        tz_start_time=start_dt.time(),
        schedule_v1=[
            [None, 0, stations[0]],
            [10, 12, stations[1]],
            [25, 30, stations[2]],
            [40, None, stations[3]]
        ]
    )
    segment = create_rthread_segment(thread=thread, station_from=stations[0], station_to=stations[1])
    if departure_event is not None:
        segment.departure_event = departure_event
    if arrival_event is not None:
        segment.arrival_event = arrival_event
    return segment


@mock.patch.object(log, 'exception', autospec=True)
@mock.patch.object(backend, 'get_cancelled_path_for_segments', side_effect=Exception('Unknown error'), autospec=True)
def test_api_failed(m_get_cancelled_path_for_segments, m_exception):
    segment = _get_segment(departure_event=SegmentEventState(type_=EventStateType.CANCELLED, key='123'))

    fill_thread_cancels([segment])

    m_get_cancelled_path_for_segments.assert_called_once_with([segment])
    m_exception.assert_called_once_with(mock.ANY)
    assert not hasattr(segment.thread, 'fully_cancelled')
    assert not hasattr(segment.thread, 'cancelled_segments')


@mock.patch.object(backend, 'get_cancelled_path_for_segments', autospec=True)
def test_no_cancelled_events(m_get_cancelled_path_for_segments):
    segment = _get_segment()
    fill_thread_cancels([segment])
    m_get_cancelled_path_for_segments.assert_called_once_with([])


def test_fill_thread_cancels():
    segment_1 = _get_segment(
        departure_event=SegmentEventState(type_=EventStateType.CANCELLED, key='s1'),
        arrival_event=SegmentEventState(type_=EventStateType.CANCELLED, key='s1')
    )
    segment_2 = _get_segment(
        arrival_event=SegmentEventState(type_=EventStateType.CANCELLED, key='s2')
    )
    segment_3 = _get_segment()

    rts_path_1 = list(segment_1.thread.path)
    rts_path_2 = list(segment_2.thread.path)

    with mock.patch.object(
        backend, 'get_cancelled_path_for_segments',
        return_value={
            segment_1: ThreadCancelsState(True, [CancelledSegment(rts_path_1[0], rts_path_1[-1])]),
            segment_2: ThreadCancelsState(False, [CancelledSegment(rts_path_2[0], rts_path_2[1])])
        }
    ) as m_get_cancelled_path_for_segments:
        fill_thread_cancels([segment_1, segment_2, segment_3])
        m_get_cancelled_path_for_segments.assert_called_once_with([segment_1, segment_2])

    assert segment_1.thread.fully_cancelled
    assert segment_1.thread.cancelled_segments == [{
        'station_from': rts_path_1[0].station,
        'station_to': rts_path_1[-1].station
    }]

    assert not segment_2.thread.fully_cancelled
    assert segment_2.thread.cancelled_segments == [{
        'station_from': rts_path_2[0].station,
        'station_to': rts_path_2[1].station
    }]

    assert not hasattr(segment_3.thread, 'fully_cancelled')
    assert not hasattr(segment_3.thread, 'cancelled_segments')
