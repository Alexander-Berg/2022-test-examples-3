# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from datetime import date, datetime, time, timedelta

import mock
import pytest
import pytz
from dateutil import parser
from django.db import connection
from django.test.utils import CaptureQueriesContext
from hamcrest import assert_that, has_entries, has_property

import travel.rasp.morda_backend.morda_backend.thread.data_layer
from common.apps.suburban_events.api import StationEventState, EventState
from common.apps.suburban_events.utils import EventStateType
from common.models.geo import CityMajority
from common.models.schedule import RThread, RThreadType, RTStation, DeLuxeTrain
from common.models.transport import TransportType
from common.tester.factories import (
    create_thread, create_station, create_country, create_settlement, create_route, create_deluxe_train
)
from common.tester.utils.datetime import replace_now
from travel.rasp.morda_backend.morda_backend.thread.data_layer import (
    _add_capitals_time_zones_info, _add_domain_capital_info, _attach_states_to_stations, _define_main_segment,
    _define_thread_uids, _find_other_today_threads, _get_related_threads, _get_segments_and_select_current,
    _get_united_segments, _load_segments, _make_path, _make_run_days, get_thread, get_thread_map,
    FindThreadError, RelatedThread, ThreadSegment, UnitedThreadSegment
)
from travel.rasp.morda_backend.morda_backend.thread.serialization import ThreadContext, ResponseSchema


pytestmark = pytest.mark.dbuser


def test_define_thread_uids():
    create_thread(uid='1', canonical_uid='R_1')
    create_thread(uid='2', canonical_uid='T_2')
    create_thread(uid='3', canonical_uid='T_3', type=RThreadType.CANCEL_ID)

    assert ('1', 'R_1') == _define_thread_uids('1', 'R_1')
    assert (None, 'T_2') == _define_thread_uids(None, 'T_2')
    assert ('1', 'R_1') == _define_thread_uids(None, '1')

    with pytest.raises(FindThreadError) as ex:
        _define_thread_uids(None, 'hhh')
    assert ex.value.message == 'Нитки с uid hhh нет в базе'

    with pytest.raises(FindThreadError) as ex:
        _define_thread_uids(None, '3')
    assert ex.value.message == 'Нитка с uid 3 является ниткой отмены'


def test_load_segments():
    st1 = create_station()
    st2 = create_station()
    st3 = create_station()

    thread1 = create_thread(
        id=1, uid='1',
        canonical_uid='R_1',
        type=RThreadType.BASIC_ID,
        schedule_v1=[[None, 0, st1], [10, None, st2]]
    )

    thread2 = create_thread(
        id=2, uid='2',
        canonical_uid='R_1',
        type=RThreadType.BASIC_ID,
        schedule_v1=[[None, 0, st1], [5, 6, st3], [10, 11, st1], [15, 16, st2], [20, None, st3]]
    )

    thread3 = create_thread(
        id=3, uid='3',
        canonical_uid='R_1',
        type=RThreadType.BASIC_ID,
        schedule_v1=[[None, 0, st3], [5, 6, st1], [10, 11, st2], [15, 16, st3], [20, None, st2]]
    )

    thread4 = create_thread(
        id=4, uid='4',
        canonical_uid='R_1',
        type=RThreadType.CHANGE_ID,
        schedule_v1=[[None, 0, st3], [5, 6, st1], [10, 11, st2], [15, None, st3]]
    )

    thread5 = create_thread(
        id=5, uid='5',
        canonical_uid='R_1',
        type=RThreadType.BASIC_ID,
        schedule_v1=[[None, 0, st3], [10, None, st2]]
    )

    thread6 = create_thread(
        id=6, uid='6',
        canonical_uid='R_1',
        type=RThreadType.CANCEL_ID,
        schedule_v1=[[None, 0, st1], [5, 5, st2], [10, None, st3]]
    )

    thread7 = create_thread(
        id=7, uid='7',
        canonical_uid='R_1',
        type=RThreadType.BASIC_ID,
        schedule_v1=[[None, 0, st2], [10, None, st1]]
    )

    thread8 = create_thread(
        id=8, uid='8',
        canonical_uid='R_1',
        type=RThreadType.BASIC_ID,
        schedule_v1=[[None, 0, st3], [9, 10, st2], [19, 20, st1], [30, None, st3]]
    )

    create_thread(
        id=9, uid='9',
        canonical_uid='R_1',
        type=RThreadType.BASIC_ID,
        year_days=[],
        schedule_v1=[[None, 0, st1], [5, 6, st2], [10, None, st3]]
    )

    segments_list = _load_segments('R_1', st1, st2)
    segments = {segment.id: segment for segment in segments_list}

    assert len(segments) == 8
    _check_loaded_segment(segments, thread1, [st1, st2], st1, st2, [0], [1])
    _check_loaded_segment(segments, thread2, [st1, st3, st1, st2, st3], st1, st2, [0, 2], [3])
    _check_loaded_segment(segments, thread3, [st3, st1, st2, st3, st2], st1, st2, [1], [2, 4])
    _check_loaded_segment(segments, thread4, [st3, st1, st2, st3], st1, st2, [1], [2])
    _check_loaded_segment(segments, thread5, [st3, st2], st1, st2, [], [1])
    _check_loaded_segment(segments, thread6, [st1, st2, st3], st1, st2, [0], [])
    _check_loaded_segment(segments, thread7, [st2, st1], st1, st2, [], [])
    _check_loaded_segment(segments, thread8, [st3, st2, st1, st3], st1, st2, [2], [1])

    segments_list = _load_segments('R_1', st1, None)
    segments = {segment.id: segment for segment in segments_list}

    _check_loaded_segment(segments, thread1, [st1, st2], st1, None, [0], [1])
    _check_loaded_segment(segments, thread2, [st1, st3, st1, st2, st3], st1, None, [0, 2], [4])
    _check_loaded_segment(segments, thread3, [st3, st1, st2, st3, st2], st1, None, [1], [4])
    _check_loaded_segment(segments, thread4, [st3, st1, st2, st3], st1, None, [1], [3])
    _check_loaded_segment(segments, thread5, [st3, st2], st1, None, [], [1])
    _check_loaded_segment(segments, thread6, [st1, st2, st3], st1, None, [0], [2])
    _check_loaded_segment(segments, thread7, [st2, st1], st1, None, [], [1])
    _check_loaded_segment(segments, thread8, [st3, st2, st1, st3], st1, None, [2], [3])

    segments_list = _load_segments('R_1', None, None)
    segments = {segment.id: segment for segment in segments_list}

    _check_loaded_segment(segments, thread1, [st1, st2], st1, None, [0], [1])
    _check_loaded_segment(segments, thread2, [st1, st3, st1, st2, st3], st1, None, [0], [4])
    _check_loaded_segment(segments, thread3, [st3, st1, st2, st3, st2], st3, None, [0], [4])
    _check_loaded_segment(segments, thread4, [st3, st1, st2, st3], st3, None, [0], [3])
    _check_loaded_segment(segments, thread5, [st3, st2], st3, None, [0], [1])
    _check_loaded_segment(segments, thread6, [st1, st2, st3], st1, None, [0], [2])
    _check_loaded_segment(segments, thread7, [st2, st1], st2, None, [0], [1])
    _check_loaded_segment(segments, thread8, [st3, st2, st1, st3], st3, None, [0], [3])


def _check_loaded_segment(segments, thread, stations, station_from, station_to, from_indices, to_indices):
    assert thread.id in segments
    segment = segments[thread.id]

    assert segment.thread == thread
    assert segment.id == thread.id
    assert segment.uid == thread.uid
    assert segment.type == thread.type.code
    assert stations == [rts.station for rts in segment.path]
    assert segment.station_from == station_from
    assert segment.station_to == station_to
    assert segment.rtstations_from_indices == from_indices
    assert segment.rtstations_to_indices == to_indices


def test_make_thread_title():
    def create_test_segment(number, title, t_type_id, type_id=RThreadType.BASIC_ID):
        thread = create_thread(
            number=number,
            title=title,
            t_type=t_type_id,
            type=type_id
        )
        segment = ThreadSegment(thread, None, None)
        segment.set_thread_deluxe_train()
        return segment

    segment = create_test_segment('456', 'вагончик', TransportType.TRAIN_ID, RThreadType.THROUGH_TRAIN_ID)
    assert segment.get_thread_full_title() == 'Беспересадочный вагон 456 вагончик'

    segment = create_test_segment('', 'вагончик', TransportType.TRAIN_ID, RThreadType.THROUGH_TRAIN_ID)
    assert segment.get_thread_full_title() == 'Беспересадочный вагон вагончик'

    segment = create_test_segment('', '', TransportType.TRAIN_ID, RThreadType.THROUGH_TRAIN_ID)
    assert segment.get_thread_full_title() == 'Беспересадочный вагон'

    create_deluxe_train(deluxe=True, numbers='123/333', title_ru='Сапсан')
    create_deluxe_train(deluxe=False, numbers='124', title_ru='Мапсан')
    DeLuxeTrain._number2deluxe_train = {}

    segment = create_test_segment('123', 'нитка_поезда', TransportType.TRAIN_ID)
    assert segment.get_thread_full_title() == 'Фирменный поезд «Сапсан» 123 нитка_поезда'

    segment = create_test_segment('124', 'нитка_поезда', TransportType.TRAIN_ID)
    assert segment.get_thread_full_title() == 'Поезд «Мапсан» 124 нитка_поезда'

    segment = create_test_segment('125', 'нитка_поезда', TransportType.TRAIN_ID)
    assert segment.get_thread_full_title() == 'Поезд 125 нитка_поезда'

    segment = create_test_segment('', '', TransportType.TRAIN_ID)
    assert segment.get_thread_full_title() == 'Поезд'

    segment = create_test_segment('123', 'нитка_автобуса', TransportType.BUS_ID)
    assert segment.get_thread_full_title() == 'Маршрут автобуса 123 нитка_автобуса'

    segment = create_test_segment('', 'нитка_автобуса', TransportType.BUS_ID)
    assert segment.get_thread_full_title() == 'Маршрут автобуса нитка_автобуса'

    segment = create_test_segment('', '', TransportType.BUS_ID)
    assert segment.get_thread_full_title() == 'Маршрут автобуса'

    segment = create_test_segment('', 'нитка_электрички', TransportType.SUBURBAN_ID)
    assert segment.get_thread_full_title() == 'Поезд нитка_электрички'

    segment = create_test_segment('123', '', TransportType.SUBURBAN_ID)
    assert segment.get_thread_full_title() == 'Поезд 123'

    segment = create_test_segment('', 'нитка_воды', TransportType.WATER_ID)
    assert segment.get_thread_full_title() == 'Теплоход нитка_воды'

    segment = create_test_segment('', '', TransportType.WATER_ID)
    assert segment.get_thread_full_title() == 'Теплоход'

    segment = create_test_segment('12345', 'название_нитки', TransportType.HELICOPTER_ID)
    assert segment.get_thread_full_title() == 'название_нитки'


def test_express_no_stop():
    st1 = create_station(t_type=TransportType.TRAIN_ID)
    st2 = create_station(t_type=TransportType.TRAIN_ID)
    st3 = create_station(t_type=TransportType.TRAIN_ID)
    st4 = create_station(t_type=TransportType.TRAIN_ID)

    express = create_thread(t_type=TransportType.SUBURBAN_ID, express_type='express',
                            schedule_v1=[[None, 0, st1], [10, 10, st2], [20, 21, st3], [30, None, st4]])
    segment = ThreadSegment(express, st1, st4)
    _check_filtered_stations(segment, [st1, st3, st4])

    aeroexpress = create_thread(t_type=TransportType.SUBURBAN_ID, express_type='aeroexpress',
                                schedule_v1=[[None, 0, st1], [10, 10, st2], [20, 21, st3], [30, None, st4]])
    segment = ThreadSegment(aeroexpress, st1, st4)
    _check_filtered_stations(segment, [st1, st3, st4])

    suburban = create_thread(t_type=TransportType.SUBURBAN_ID,
                             schedule_v1=[[None, 0, st1], [10, 10, st2], [20, 21, st3], [30, None, st4]])
    segment = ThreadSegment(suburban, st1, st4)
    _check_filtered_stations(segment, [st1, st2, st3, st4])


def _check_filtered_stations(segment, stations):
    path = segment.path

    assert len(path) == len(stations)
    for index in range(len(path)):
        assert path[index].station == stations[index]

    segment.naive_start_dt = datetime(2019, 1, 1)
    segment.thread.capital_tz = 'Etc/GMT-3'
    path = _make_path(segment, None)

    assert len(path) == len(stations)
    for index in range(len(path)):
        assert path[index].station == stations[index]


@replace_now('2019-05-01')
def test_process_departure_from():
    ekb_tz = pytz.timezone('Etc/GMT-5')
    samara_tz = pytz.timezone('Etc/GMT-4')
    msk_tz = pytz.timezone('Etc/GMT-3')
    ekb = create_station(time_zone=ekb_tz)
    samara = create_station(time_zone=samara_tz)
    msk = create_station(time_zone=msk_tz)

    thread = create_thread(
        tz_start_time=time(16, 30),
        year_days=[date(2019, 5, 1), datetime(2019, 5, 2)],
        schedule_v1=[
            [None, 0, ekb, {'time_zone': ekb_tz}],
            [420, 430, samara, {'time_zone': samara_tz}],
            [960, None, msk, {'time_zone': msk_tz}]
        ]
    )

    _check_departure_from(
        thread, ekb, samara, rts_from_indices=[0], is_from_date=False, departure_from=datetime(2019, 5, 1, 16, 30),
        rts_from_index=0, start_date=date(2019, 5, 1),
        naive_start_dt=datetime(2019, 5, 1, 16, 30), station_from_dt='2019-05-01T16:30:00+05:00'
    )
    _check_departure_from(
        thread, ekb, samara, rts_from_indices=[0], is_from_date=False, departure_from=datetime(2019, 5, 2, 16, 30),
        rts_from_index=0, start_date=date(2019, 5, 2),
        naive_start_dt=datetime(2019, 5, 2, 16, 30), station_from_dt='2019-05-02T16:30:00+05:00'
    )
    _check_departure_from(
        thread, ekb, samara, rts_from_indices=[0], is_from_date=False, departure_from=datetime(2019, 5, 3, 16, 30)
    )
    _check_departure_from(
        thread, samara, msk, rts_from_indices=[1], is_from_date=False, departure_from=datetime(2019, 5, 1, 23, 40),
        rts_from_index=1, start_date=date(2019, 5, 1),
        naive_start_dt=datetime(2019, 5, 1, 16, 30), station_from_dt='2019-05-01T23:40:00+04:00'
    )
    _check_departure_from(
        thread, samara, ekb, rts_from_indices=[1], is_from_date=False, departure_from=datetime(2019, 5, 1, 23, 40)
    )
    _check_departure_from(
        thread, samara, samara, rts_from_indices=[1], is_from_date=False, departure_from=datetime(2019, 5, 1, 23, 40)
    )

    thread = create_thread(
        tz_start_time=time(1, 0),
        year_days=[date(2019, 5, 1)],
        schedule_v1=[
            [None, 0, ekb, {'time_zone': ekb_tz}],
            [600, 610, samara, {'time_zone': samara_tz}],
            [1500, 1510, ekb, {'time_zone': ekb_tz}],
            [1800, None, msk, {'time_zone': msk_tz}]
        ]
    )

    _check_departure_from(
        thread, ekb, msk, rts_from_indices=[0, 2], is_from_date=False, departure_from=datetime(2019, 5, 1, 1, 0),
        rts_from_index=0, start_date=date(2019, 5, 1),
        naive_start_dt=datetime(2019, 5, 1, 1, 0), station_from_dt='2019-05-01T01:00:00+05:00'
    )
    _check_departure_from(
        thread, ekb, msk, rts_from_indices=[0, 2], is_from_date=False, departure_from=datetime(2019, 5, 2, 2, 10),
        rts_from_index=2, start_date=date(2019, 5, 1),
        naive_start_dt=datetime(2019, 5, 1, 1, 0), station_from_dt='2019-05-02T02:10:00+05:00'
    )


@replace_now('2019-05-01')
def test_process_departure_from_date():
    ekb_tz = pytz.timezone('Etc/GMT-5')
    samara_tz = pytz.timezone('Etc/GMT-4')
    msk_tz = pytz.timezone('Etc/GMT-3')
    ekb = create_station(time_zone=ekb_tz)
    samara = create_station(time_zone=samara_tz)
    msk = create_station(time_zone=msk_tz)

    thread = create_thread(
        tz_start_time=time(16, 30),
        year_days=[date(2019, 5, 1), date(2019, 5, 2)],
        schedule_v1=[
            [None, 0, ekb, {'time_zone': ekb_tz}],
            [480, 490, samara, {'time_zone': samara_tz}],
            [960, None, msk, {'time_zone': msk_tz}]
        ]
    )

    _check_departure_from(
        thread, ekb, samara, rts_from_indices=[0], is_from_date=True, departure_from=date(2019, 5, 1),
        rts_from_index=0, start_date=date(2019, 5, 1),
        naive_start_dt=datetime(2019, 5, 1, 16, 30), station_from_dt='2019-05-01T16:30:00+05:00'
    )
    _check_departure_from(
        thread, samara, msk, rts_from_indices=[1], is_from_date=True, departure_from=date(2019, 5, 2),
        rts_from_index=1, start_date=date(2019, 5, 1),
        naive_start_dt=datetime(2019, 5, 1, 16, 30), station_from_dt='2019-05-02T00:40:00+04:00'
    )
    _check_departure_from(
        thread, samara, msk, rts_from_indices=[1], is_from_date=True, departure_from=date(2019, 5, 2),
        time_zone='Etc/GMT-5', rts_from_index=1, start_date=date(2019, 5, 1),
        naive_start_dt=datetime(2019, 5, 1, 16, 30), station_from_dt='2019-05-02T00:40:00+04:00'
    )
    _check_departure_from(
        thread, samara, msk, rts_from_indices=[1], is_from_date=True, departure_from=date(2019, 5, 1),
        time_zone='Etc/GMT-3', rts_from_index=1, start_date=date(2019, 5, 1),
        naive_start_dt=datetime(2019, 5, 1, 16, 30), station_from_dt='2019-05-02T00:40:00+04:00'
    )
    _check_departure_from(thread, samara, msk, rts_from_indices=[1], is_from_date=True, departure_from=date(2019, 5, 4))

    thread = create_thread(
        tz_start_time=time(1, 0),
        year_days=[date(2019, 5, 1)],
        schedule_v1=[
            [None, 0, ekb, {'time_zone': ekb_tz}],
            [600, 610, samara, {'time_zone': samara_tz}],
            [1500, 1510, ekb, {'time_zone': ekb_tz}],
            [1800, None, msk, {'time_zone': msk_tz}]
        ]
    )

    _check_departure_from(
        thread, ekb, msk, rts_from_indices=[0, 2], is_from_date=True, departure_from=date(2019, 5, 1),
        rts_from_index=0, start_date=date(2019, 5, 1),
        naive_start_dt=datetime(2019, 5, 1, 1, 0), station_from_dt='2019-05-01T01:00:00+05:00'
    )
    _check_departure_from(
        thread, ekb, msk, rts_from_indices=[0, 2], is_from_date=True, departure_from=date(2019, 5, 2),
        rts_from_index=2, start_date=date(2019, 5, 1),
        naive_start_dt=datetime(2019, 5, 1, 1, 0), station_from_dt='2019-05-02T02:10:00+05:00'
    )


def _check_departure_from(thread, st_from, st_to, rts_from_indices, is_from_date, departure_from, time_zone=None,
                          rts_from_index=None, start_date=None, naive_start_dt=None, station_from_dt=None):
    segment = ThreadSegment(thread, st_from, st_to)
    segment.rtstations_from = [segment.path[i] for i in rts_from_indices]
    if is_from_date:
        segment._process_for_departure_from_date(departure_from, time_zone)
    else:
        segment._process_for_departure_from(departure_from)
    if rts_from_index is None:
        assert segment.rtstation_from is None
        assert segment.thread_start_date is None
        assert segment.naive_start_dt is None
        assert segment.station_from_dt is None
    else:
        assert segment.rtstation_from == segment.path[rts_from_index]
        assert segment.thread_start_date == start_date
        assert segment.naive_start_dt == naive_start_dt
        assert segment.station_from_dt.isoformat() == station_from_dt


@replace_now('2019-05-01')
def test_process_departure():
    ekb = create_station(time_zone=(pytz.timezone('Etc/GMT-5')))
    msk = create_station(time_zone=(pytz.timezone('Etc/GMT-3')))

    thread = create_thread(
        tz_start_time=time(1, 30),
        year_days=[date(2019, 5, 2)],
        schedule_v1=[
            [None, 0, ekb, {'time_zone': (pytz.timezone('Etc/GMT-5'))}],
            [960, None, msk, {'time_zone': (pytz.timezone('Etc/GMT-3'))}]
        ]
    )

    segment = ThreadSegment(thread, None, None)
    segment._process_for_departure(date(2019, 5, 1))
    assert segment.rtstation_from == segment.path[0]
    assert segment.thread_start_date == date(2019, 5, 2)
    assert segment.naive_start_dt == datetime(2019, 5, 2, 1, 30)
    assert segment.station_from_dt.isoformat() == '2019-05-02T01:30:00+05:00'

    segment = ThreadSegment(thread, None, None)
    segment._process_for_departure(date(2019, 5, 3))
    assert segment.rtstation_from is None
    assert segment.thread_start_date is None
    assert segment.naive_start_dt is None
    assert segment.station_from_dt is None

    thread = create_thread(
        tz_start_time=time(11, 30),
        year_days=[date(2019, 5, 2)],
        schedule_v1=[
            [None, 1440, ekb, {'time_zone': (pytz.timezone('Etc/GMT-5'))}],
            [2400, None, msk, {'time_zone': (pytz.timezone('Etc/GMT-3'))}]
        ]
    )

    segment = ThreadSegment(thread, None, None)
    segment._process_for_departure(date(2019, 5, 3))
    assert segment.rtstation_from == segment.path[0]
    assert segment.thread_start_date == date(2019, 5, 2)
    assert segment.naive_start_dt == datetime(2019, 5, 2, 11, 30)
    assert segment.station_from_dt.isoformat() == '2019-05-03T11:30:00+05:00'


@replace_now('2019-05-01')
def test_process_without_context():
    ekb = create_station(time_zone=(pytz.timezone('Etc/GMT-5')))
    msk = create_station(time_zone=(pytz.timezone('Etc/GMT-3')))

    thread = create_thread(
        tz_start_time=time(16, 30),
        year_days=[date(2019, 5, 3)],
        schedule_v1=[
            [None, 0, ekb, {'time_zone': (pytz.timezone('Etc/GMT-5'))}],
            [960, None, msk, {'time_zone': (pytz.timezone('Etc/GMT-3'))}]
        ]
    )

    segment = ThreadSegment(thread, None, None)
    segment._process_without_context()
    assert segment.rtstation_from == segment.path[0]
    assert segment.thread_start_date == date(2019, 5, 3)
    assert segment.naive_start_dt == datetime(2019, 5, 3, 16, 30)
    assert segment.station_from_dt.isoformat() == '2019-05-03T16:30:00+05:00'


@replace_now('2019-05-04')
def test_define_main_segment():
    st1 = create_station()
    st2 = create_station()

    threads = [
        create_thread(
            uid='0', canonical_uid='R_1',
            year_days=[date(2019, 5, 1)],
            schedule_v1=[[None, 0, st1], [60, None, st2]]
        ),
        create_thread(
            uid='1', canonical_uid='R_1',
            year_days=[date(2019, 5, 2), date(2019, 5, 3)],
            schedule_v1=[[None, 0, st1], [60, None, st2]]
        ),
        create_thread(
            uid='2', canonical_uid='R_1',
            year_days=[date(2019, 5, 4)],
            schedule_v1=[[None, 0, st1], [60, None, st2]]
        ),
        create_thread(
            uid='3', canonical_uid='R_1',
            year_days=[date(2019, 5, 5), date(2019, 5, 6)],
            schedule_v1=[[None, 0, st1], [60, None, st2]]
        ),
        create_thread(
            uid='4', canonical_uid='R_1',
            type=RThreadType.CANCEL_ID,
            year_days=[date(2019, 5, 6)],
            schedule_v1=[[None, 0, st1], [60, None, st2]]
        )
    ]

    segments = []
    for thread in threads:
        segment = ThreadSegment(thread, st1, st2)
        segment.thread_start_date = thread.first_run(date(2019, 5, 4))
        segment.station_from_dt = datetime.combine(segment.thread_start_date, time(0))
        segments.append(segment)

    with pytest.raises(FindThreadError) as ex:
        _define_main_segment([], None, 'R_2')
    assert ex.value.message == 'Подходящих под параметры запроса ниток с canonical_uid R_2 нет в базе'

    with pytest.raises(FindThreadError) as ex:
        _define_main_segment([segments[4]], None, 'R_1')
    assert ex.value.message == 'Подходящих под параметры запроса ниток с canonical_uid R_1 нет в базе'

    with pytest.raises(FindThreadError) as ex:
        _define_main_segment(segments, '5', 'R_1')
    assert ex.value.message == 'Нитка с uid 5 не подходит под параметры запроса'

    main_segment = _define_main_segment(segments, '0', 'R_1')
    assert main_segment.uid == '0'
    assert main_segment.current is True

    main_segment = _define_main_segment(segments, None, 'R_1')
    assert main_segment.uid == '2'

    main_segment = _define_main_segment([segments[0], segments[1]], None, 'R_1')
    assert main_segment.uid == '1'

    main_segment = _define_main_segment([segments[1], segments[3]], None, 'R_1')
    assert main_segment.uid == '3'

    main_segment = _define_main_segment([segments[1], segments[4]], None, 'R_1')
    assert main_segment.uid == '1'

    main_segment = _define_main_segment([segments[2], segments[3]], None, 'R_1')
    assert main_segment.uid == '2'


@replace_now('2019-01-25')
def test_get_segments_and_select_current():
    thread_tzinfo = pytz.timezone('Etc/GMT-3')
    st10 = create_station(title='begin', id=100, time_zone=thread_tzinfo)  # UTC+3
    st11 = create_station(title='begin1', id=101, time_zone=thread_tzinfo)  # UTC+3
    st21 = create_station(title='middle1', id=201, time_zone=pytz.timezone('Etc/GMT-5'))  # UTC+5
    st22 = create_station(title='middle2', id=202, time_zone=pytz.timezone('Etc/GMT-7'))  # UTC+7
    st90 = create_station(title='end', id=900, time_zone=pytz.timezone('Etc/GMT-7'))  # UTC+7

    canonical_uid = 'R_canonical_uid'
    base_thread_date = datetime.combine(date(2019, 1, 25), time.min)
    thread_start_time = time(13, 15)
    past_canonical_uid = 'R_past_canonical_uid'
    past_base_thread_date = datetime.combine(date(2019, 1, 10), time.min)
    past_thread_start_time = time(6, 45)
    future_canonical_uid = 'R_future_canonical_uid'
    future_base_thread_date = datetime.combine(date(2019, 2, 1), time.min)
    future_thread_start_time = time(13, 15)
    shift_1d_12h = (24 + 12) * 60  # 1,5 day
    shift_3d = 24 * 3 * 60  # 3 days

    create_thread(
        uid='tp_0',
        canonical_uid=past_canonical_uid,
        t_type=TransportType.TRAIN_ID,
        year_days=[
            past_base_thread_date,
            past_base_thread_date + timedelta(days=7),
            past_base_thread_date + timedelta(days=7 * 2)
        ],
        tz_start_time=past_thread_start_time,
        schedule_v1=[  # path: 10->21->90
            [None, 0, st10, {'time_zone': st10.time_zone}],
            [shift_1d_12h - 15, shift_1d_12h + 15, st21, {'time_zone': st21.time_zone}],
            [shift_3d - 15, None, st90, {'time_zone': st90.time_zone}],
        ]
    )
    create_thread(
        uid='tp_1',
        canonical_uid=past_canonical_uid,
        t_type=TransportType.TRAIN_ID,
        year_days=[
            past_base_thread_date + timedelta(days=2),
            past_base_thread_date + timedelta(days=2 + 7),
            past_base_thread_date + timedelta(days=2 + 7 * 2)
        ],
        tz_start_time=past_thread_start_time,
        schedule_v1=[  # path: 10->21->90
            [None, 0, st10, {'time_zone': st10.time_zone}],
            [shift_1d_12h - 15, shift_1d_12h + 15, st21, {'time_zone': st21.time_zone}],
            [shift_3d - 15, None, st90, {'time_zone': st90.time_zone}],
        ]
    )
    create_thread(
        uid='t_0-03:00',
        canonical_uid=canonical_uid,
        t_type=TransportType.TRAIN_ID,
        year_days=[
            base_thread_date,
            base_thread_date + timedelta(days=7),
            base_thread_date + timedelta(days=7 * 2)
        ],
        tz_start_time=time(thread_start_time.hour - 3, thread_start_time.minute),
        schedule_v1=[  # path: 10->21->90
            [None, 0, st10, {'time_zone': st10.time_zone}],
            [shift_1d_12h - 15, shift_1d_12h + 15, st21, {'time_zone': st21.time_zone}],
            [shift_3d - 15, None, st90, {'time_zone': st90.time_zone}],
        ]
    )
    create_thread(
        uid='t_0',
        canonical_uid=canonical_uid,
        t_type=TransportType.TRAIN_ID,
        year_days=[
            base_thread_date,
            base_thread_date + timedelta(days=7),
            base_thread_date + timedelta(days=7 * 2)
        ],
        tz_start_time=thread_start_time,
        schedule_v1=[  # path: 10->21->90
            [None, 0, st10, {'time_zone': st10.time_zone}],
            [shift_1d_12h - 15, shift_1d_12h + 15, st21, {'time_zone': st21.time_zone}],
            [shift_3d - 15, None, st90, {'time_zone': st90.time_zone}],
        ]
    )
    create_thread(
        uid='t_1',
        canonical_uid=canonical_uid,
        t_type=TransportType.TRAIN_ID,
        year_days=[
            base_thread_date,
            base_thread_date + timedelta(days=7),
            base_thread_date + timedelta(days=7 * 2)
        ],
        tz_start_time=thread_start_time,
        schedule_v1=[  # path: 10->21->90
            [None, 0, st10, {'time_zone': st10.time_zone}],
            [shift_1d_12h - 30, shift_1d_12h, st21, {'time_zone': st21.time_zone}],
            [shift_3d - 30, None, st90, {'time_zone': st90.time_zone}],
        ]
    ),
    create_thread(
        uid='t_2',
        canonical_uid=canonical_uid,
        t_type=TransportType.TRAIN_ID,
        year_days=[
            base_thread_date,
            base_thread_date + timedelta(days=7),
            base_thread_date + timedelta(days=7 * 2)
        ],
        tz_start_time=thread_start_time,
        schedule_v1=[  # path: 10->22->90
            [None, 0, st10, {'time_zone': st10.time_zone}],
            [shift_1d_12h - 30, shift_1d_12h, st22, {'time_zone': st22.time_zone}],
            [shift_3d - 30, None, st90, {'time_zone': st90.time_zone}],
        ]
    )
    create_thread(
        uid='t_3',
        canonical_uid=canonical_uid,
        t_type=TransportType.TRAIN_ID,
        year_days=[
            datetime.combine(date(2019, 1, 26), time.min),
            base_thread_date + timedelta(days=7),
            base_thread_date + timedelta(days=7 * 2)
        ],
        tz_start_time=thread_start_time,
        schedule_v1=[  # path: 11->22->90
            [None, 0, st11, {'time_zone': st11.time_zone}],
            [shift_1d_12h - 30, shift_1d_12h, st22, {'time_zone': st22.time_zone}],
            [shift_3d - 30, None, st90, {'time_zone': st90.time_zone}],
        ]
    )
    create_thread(
        uid='tf_0-03:00',
        canonical_uid=future_canonical_uid,
        t_type=TransportType.TRAIN_ID,
        year_days=[
            future_base_thread_date,
            future_base_thread_date + timedelta(days=7),
            future_base_thread_date + timedelta(days=7 * 2)
        ],
        tz_start_time=time(future_thread_start_time.hour - 3, future_thread_start_time.minute),
        schedule_v1=[  # path: 10->21->90
            [None, 0, st10, {'time_zone': st10.time_zone}],
            [shift_1d_12h - 15, shift_1d_12h + 15, st21, {'time_zone': st21.time_zone}],
            [shift_3d - 15, None, st90, {'time_zone': st90.time_zone}],
        ]
    )
    create_thread(
        uid='tf_0',
        canonical_uid=future_canonical_uid,
        t_type=TransportType.TRAIN_ID,
        year_days=[
            future_base_thread_date,
            future_base_thread_date + timedelta(days=7),
            future_base_thread_date + timedelta(days=7 * 2)
        ],
        tz_start_time=future_thread_start_time,
        schedule_v1=[  # path: 10->21->90
            [None, 0, st10, {'time_zone': st10.time_zone}],
            [shift_1d_12h - 15, shift_1d_12h + 15, st21, {'time_zone': st21.time_zone}],
            [shift_3d - 15, None, st90, {'time_zone': st90.time_zone}],
        ]
    )

    _run_select_current_segment('R_canonical_uid', None, st21, '2019-01-27T01:15:00', None, None, 't_1')
    _run_select_current_segment('R_canonical_uid', None, st21, None, '2019-01-27', None, 't_1')
    _run_select_current_segment('R_canonical_uid', '2019-01-26', None, None, None, None, 't_3')
    _run_select_current_segment('R_canonical_uid', None, None, None, None, None, 't_0-03:00')
    _run_select_current_segment('R_canonical_uid', None, st21, None, '2019-01-28', None, None)  # no departure
    _run_select_current_segment('R_canonical_uid', None, st21, None, '2019-01-27', st11, None)  # no path from 21 to 11
    _run_select_current_segment('R_past_canonical_uid', None, None, None, None, None, 'tp_1')
    _run_select_current_segment('R_future_canonical_uid', None, None, None, None, None, 'tf_0-03:00')


def _run_select_current_segment(
        mixed_uid, departure, station_from, departure_from, departure_from_date, station_to, expected_uid
):
    with CaptureQueriesContext(connection) as captured_queries:
        context = ThreadContext(
            mixed_uid=mixed_uid,
            departure=parser.parse(departure).date() if departure else None,
            station_from=station_from,
            departure_from=parser.parse(departure_from) if departure_from else None,
            departure_from_date=parser.parse(departure_from_date).date() if departure_from_date else None,
            station_to=station_to
        )
        try:
            segment, _ = _get_segments_and_select_current(context)
        except FindThreadError:
            segment = None

    assert len(captured_queries) <= 2

    if expected_uid is None:
        assert segment is None
    else:
        assert segment.uid == expected_uid


def test_define_rtstations_for_selector():
    ekb = create_station()
    shar = create_station()
    viz = create_station()
    main_thread = create_thread(schedule_v1=[[None, 0, shar], [10, None, viz]])
    main_segment = ThreadSegment(main_thread, shar, viz)
    main_segment.current = True

    main_segment._define_rtstations_for_selector(main_segment)
    assert main_segment.has_context_stations is True

    _check_rtstations_for_selector_segment(main_segment, [shar, ekb, viz], shar, viz, True, 0, 2)
    _check_rtstations_for_selector_segment(main_segment, [shar, ekb, shar, ekb, viz], shar, viz, True, 2, 4)
    _check_rtstations_for_selector_segment(main_segment, [viz, shar, viz], shar, viz, True, 1, 2)
    _check_rtstations_for_selector_segment(main_segment, [shar, viz, shar, viz], shar, viz, True, 0, 1)
    _check_rtstations_for_selector_segment(main_segment, [ekb, viz], shar, viz, False, 0, 1)
    _check_rtstations_for_selector_segment(main_segment, [shar, ekb, shar, ekb], shar, viz, False, 0, 3)
    _check_rtstations_for_selector_segment(main_segment, [viz, shar], shar, viz, False, 0, 1)

    _check_rtstations_for_selector_segment(main_segment, [shar, ekb, viz], shar, None, True, 0, 2)
    _check_rtstations_for_selector_segment(main_segment, [ekb, shar, viz], shar, None, True, 1, 2)
    _check_rtstations_for_selector_segment(main_segment, [ekb, viz], shar, None, False, 0, 1)
    _check_rtstations_for_selector_segment(main_segment, [ekb, viz], viz, None, False, 0, 1)


def _check_rtstations_for_selector_segment(main_segment, stations, station_from, station_to,
                                           has_context_stations, rts_from_index, rts_to_index):
    schedule = []
    for i in range(len(stations)):
        arrival = None if i == 0 else i * 10 - 1
        departure = None if i == len(stations) - 1 else i * 10
        schedule.append([arrival, departure, stations[i]])
    thread = create_thread(schedule_v1=schedule)
    segment = ThreadSegment(thread, station_from, station_to)
    segment._define_rtstations_for_selector(main_segment)
    assert segment.has_context_stations == has_context_stations
    assert segment.rtstation_from is None if rts_from_index is None else segment.path[rts_from_index]
    assert segment.rtstation_to is None if rts_to_index is None else segment.path[rts_to_index]


@replace_now('2019-05-01')
def test_prepare_for_selector():
    novosib_tz = pytz.timezone('Etc/GMT-7')
    ekb_tz = pytz.timezone('Etc/GMT-5')
    samara_tz = pytz.timezone('Etc/GMT-4')
    msk_tz = pytz.timezone('Etc/GMT-3')
    novosib = create_station(time_zone=novosib_tz)
    ekb = create_station(time_zone=ekb_tz)
    samara = create_station(time_zone=samara_tz)
    msk = create_station(time_zone=msk_tz)

    thread = create_thread(
        tz_start_time=time(1, 0),
        year_days=[date(2019, 5, 2), date(2019, 5, 5)],
        t_type=TransportType.TRAIN_ID,
        time_zone='Etc/GMT-3',
        schedule_v1=[
            [None, 0, novosib, {'time_zone': novosib_tz}],
            [60, 70, ekb, {'time_zone': ekb_tz}],
            [120, 130, samara, {'time_zone': samara_tz}],
            [180, None, msk, {'time_zone': msk_tz}]
        ]
    )

    main_segment = ThreadSegment(thread, ekb, samara)

    segment = ThreadSegment(thread, ekb, samara)
    segment.thread_start_date = date(2019, 5, 2)

    segment.prepare_for_selector(main_segment, True, None)

    assert segment.first_departure == '2019-05-02T02:10:00+05:00'
    assert segment.start_departure_time == '02:10'
    assert segment.stop_arrival_time == '03:00'
    assert segment.days_text == 'только 2, 5 мая'
    assert segment.in_context is True

    segment.prepare_for_selector(main_segment, True, 'Etc/GMT-3')

    assert segment.first_departure == '2019-05-02T02:10:00+05:00'
    assert segment.start_departure_time == '00:10'
    assert segment.stop_arrival_time == '02:00'
    assert segment.days_text == 'только 2, 5 мая'
    assert segment.in_context is True

    segment.prepare_for_selector(main_segment, False, None)
    assert segment.first_departure == '2019-05-02'
    assert segment.start_departure_time == '01:00'
    assert segment.stop_arrival_time == '04:00'
    assert segment.days_text == 'только 2, 5 мая'
    assert segment.in_context is False

    segment.prepare_for_selector(main_segment, False, 'Etc/GMT-3')
    assert segment.first_departure == '2019-05-02'
    assert segment.start_departure_time == '21:00'
    assert segment.stop_arrival_time == '04:00'
    assert segment.days_text == 'только 1, 4 мая'
    assert segment.in_context is False

    thread = create_thread(
        tz_start_time=time(1, 0),
        year_days=[date(2019, 5, 2), date(2019, 5, 5)],
        t_type=TransportType.SUBURBAN_ID,
        time_zone='Etc/GMT-3',
        schedule_v1=[
            [None, 0, novosib, {'time_zone': novosib_tz}],
            [60, 70, ekb, {'time_zone': ekb_tz}],
            [120, 130, samara, {'time_zone': samara_tz}],
            [180, None, msk, {'time_zone': msk_tz}]
        ]
    )
    segment = ThreadSegment(thread, ekb, samara)
    segment.thread_start_date = date(2019, 5, 2)

    segment.prepare_for_selector(main_segment, False, None)
    assert segment.first_departure == '2019-05-02'
    assert segment.start_departure_time == '01:00'
    assert segment.stop_arrival_time == '04:00'
    assert segment.days_text == 'только 2, 5 мая'
    assert segment.in_context is False

    thread = create_thread(
        tz_start_time=time(1, 0),
        year_days=[date(2019, 5, 2), date(2019, 5, 5)],
        t_type=TransportType.TRAIN_ID,
        time_zone='Etc/GMT-3',
        schedule_v1=[
            [None, 0, novosib, {'time_zone': novosib_tz}],
            [180, None, msk, {'time_zone': msk_tz}]
        ]
    )
    segment = ThreadSegment(thread, ekb, samara)
    segment.thread_start_date = date(2019, 5, 2)

    segment.prepare_for_selector(main_segment, False, None)
    assert segment.first_departure == '2019-05-02'
    assert segment.start_departure_time == '01:00'
    assert segment.stop_arrival_time == '04:00'
    assert segment.days_text == 'только 2, 5 мая'
    assert segment.in_context is False

    segment = ThreadSegment(thread, novosib, msk)
    segment.thread_start_date = date(2019, 5, 2)

    segment.prepare_for_selector(main_segment, False, None)
    assert segment.first_departure == '2019-05-02'
    assert segment.start_departure_time == '01:00'
    assert segment.stop_arrival_time == '04:00'
    assert segment.days_text == 'только 2, 5 мая'
    assert segment.in_context is False

    thread = create_thread(
        tz_start_time=time(10, 0),
        year_days=[date(2019, 5, 2), date(2019, 5, 5)],
        t_type=TransportType.TRAIN_ID,
        time_zone='Etc/GMT-3',
        schedule_v1=[
            [None, 1440, ekb, {'time_zone': ekb_tz}],
            [1620, None, samara, {'time_zone': samara_tz}]
        ]
    )

    segment = ThreadSegment(thread, ekb, samara)
    segment.thread_start_date = date(2019, 5, 2)

    segment.prepare_for_selector(main_segment, False, None)
    assert segment.first_departure == '2019-05-03'
    assert segment.start_departure_time == '10:00'
    assert segment.stop_arrival_time == '13:00'
    assert segment.days_text == 'только 3, 6 мая'
    assert segment.in_context is False


@replace_now('2019-05-01')
def test_make_path():
    ekb_tz = pytz.timezone('Etc/GMT-5')
    samara_tz = pytz.timezone('Etc/GMT-4')
    msk_tz = pytz.timezone('Etc/GMT-3')
    kiev_tz = pytz.timezone('Etc/GMT-2')
    ekb = create_station(time_zone=ekb_tz)
    samara = create_station(time_zone=samara_tz)
    msk = create_station(time_zone=msk_tz)
    kiev = create_station(time_zone=kiev_tz)

    thread = create_thread(
        tz_start_time=time(10, 0),
        t_type=TransportType.TRAIN_ID,
        schedule_v1=[
            [None, 0, ekb, {'time_zone': ekb_tz}],
            [60, 70, samara, {'time_zone': samara_tz}],
            [120, 130, msk, {'time_zone': msk_tz}],
            [180, None, kiev, {'time_zone': kiev_tz}],
        ]
    )
    segment = ThreadSegment(thread, ekb, msk)
    segment.rtstation_from = thread.path[0]
    segment.rtstation_to = thread.path[2]
    segment.naive_start_dt = datetime.combine(datetime(2019, 5, 1), thread.tz_start_time)

    _add_domain_capital_info(segment, 'RU')

    path = _make_path(segment, None)

    assert len(path) == 4
    _check_path_station(path[0], None, '2019-05-01T10:00:00+05:00', True, False, '+02:00')
    _check_path_station(path[1], '2019-05-01T11:00:00+04:00', '2019-05-01T11:10:00+04:00', False, False, '+01:00')
    _check_path_station(path[2], '2019-05-01T12:00:00+03:00', '2019-05-01T12:10:00+03:00', False, True, '+00:00')
    _check_path_station(path[3], '2019-05-01T13:00:00+02:00', None, False, False, '-01:00')

    path = _make_path(segment, 'Etc/GMT-5')

    assert len(path) == 4
    _check_path_station(path[0], None, '2019-05-01T10:00:00+05:00', True, False, '+02:00')
    _check_path_station(path[1], '2019-05-01T12:00:00+05:00', '2019-05-01T12:10:00+05:00', False, False, '+01:00')
    _check_path_station(path[2], '2019-05-01T14:00:00+05:00', '2019-05-01T14:10:00+05:00', False, True, '+00:00')
    _check_path_station(path[3], '2019-05-01T16:00:00+05:00', None, False, False, '-01:00')

    path = _make_path(segment, 'Etc/GMT-3')

    assert len(path) == 4
    _check_path_station(path[0], None, '2019-05-01T08:00:00+03:00', True, False, '+02:00')
    _check_path_station(path[1], '2019-05-01T10:00:00+03:00', '2019-05-01T10:10:00+03:00', False, False, '+01:00')
    _check_path_station(path[2], '2019-05-01T12:00:00+03:00', '2019-05-01T12:10:00+03:00', False, True, '+00:00')
    _check_path_station(path[3], '2019-05-01T14:00:00+03:00', None, False, False, '-01:00')


def _check_path_station(rts, arrival_dt, departure_dt, is_station_from, is_station_to, capital_time_offset):
    if arrival_dt is not None:
        assert rts.arrival_dt.isoformat() == arrival_dt
    if departure_dt is not None:
        assert rts.departure_dt.isoformat() == departure_dt
    assert rts.is_station_from == is_station_from
    assert rts.is_station_to == is_station_to
    assert rts.capital_time_offset == capital_time_offset


@replace_now('2019-05-01')
def test_add_capitals_info():
    ru = create_country()
    kz = create_country()
    ua = create_country()

    msk_city = create_settlement(country=ru, slug='msk',
                                 title='Москва', title_ru_genitive='Москвы', abbr_title='МСК',
                                 time_zone=pytz.timezone('Etc/GMT+3'), majority=CityMajority.CAPITAL_ID)
    piter_city = create_settlement(country=ru, slug='piter',
                                   title='Санкт-Петербург', title_ru_genitive='Санкт-Петербурга', abbr_title='СПБ',
                                   time_zone=pytz.timezone('Etc/GMT+3'), majority=CityMajority.REGION_CAPITAL_ID)
    ekb_city = create_settlement(country=ru, slug='ekb',
                                 title='Екатеринбург', title_ru_genitive='Екатеринбурга', abbr_title='ЕКБ',
                                 time_zone=pytz.timezone('Etc/GMT+5'), majority=CityMajority.REGION_CAPITAL_ID)
    nur_city = create_settlement(country=kz, slug='nur',
                                 title='Нур-Султан', title_ru_genitive='Нур-Султана', abbr_title='НУР',
                                 time_zone=pytz.timezone('Etc/GMT+6'), majority=CityMajority.CAPITAL_ID)
    ur_city = create_settlement(country=kz, slug='ur',
                                title='Уральск', title_ru_genitive='Уральска', abbr_title='УР',
                                time_zone=pytz.timezone('Etc/GMT+5'), majority=CityMajority.REGION_CAPITAL_ID)
    kiev_city = create_settlement(country=ua, slug='kiev',
                                  title='Киев', title_ru_genitive='Киева', abbr_title='КИЕВ',
                                  time_zone=pytz.timezone('Etc/GMT+2'), majority=CityMajority.CAPITAL_ID)
    har_city = create_settlement(country=ua, slug='har',
                                 title='Харьков', title_ru_genitive='Харькова', abbr_title='ХАР',
                                 time_zone=pytz.timezone('Etc/GMT+2'), majority=CityMajority.REGION_CAPITAL_ID)

    msk = create_station(country=ru, settlement=msk_city, time_zone=msk_city.time_zone)
    piter = create_station(country=ru, settlement=piter_city, time_zone=piter_city.time_zone)
    ekb = create_station(country=ru, settlement=ekb_city, time_zone=ekb_city.time_zone)
    ekb2 = create_station(country=ru, settlement=ekb_city, time_zone=ekb_city.time_zone)
    nur = create_station(country=kz, settlement=nur_city, time_zone=nur_city.time_zone)
    ur = create_station(country=kz, settlement=ur_city, time_zone=ur_city.time_zone)
    kiev = create_station(country=ua, settlement=kiev_city, time_zone=kiev_city.time_zone)
    har = create_station(country=ua, settlement=har_city, time_zone=har_city.time_zone)

    msk_res = {'slug': 'msk', 'time_zone': 'Etc/GMT+3',
               'title': 'Москва', 'title_genitive': 'Москвы', 'abbr_title': 'МСК'}
    nur_res = {'slug': 'nur', 'time_zone': 'Etc/GMT+6',
               'title': 'Нур-Султан', 'title_genitive': 'Нур-Султана', 'abbr_title': 'НУР'}
    kiev_res = {'slug': 'kiev', 'time_zone': 'Etc/GMT+2',
                'title': 'Киев', 'title_genitive': 'Киева', 'abbr_title': 'КИЕВ'}

    _check_capitals([msk, piter], [])
    _check_capitals([msk, piter], [msk_res, kiev_res], 'Etc/GMT+2')
    _check_capitals([msk, ekb], [msk_res])
    _check_capitals([piter, msk, ekb], [msk_res])
    _check_capitals([piter, msk, ekb], [msk_res], 'Etc/GMT+3')
    _check_capitals([piter, ekb], [msk_res])
    _check_capitals([ur, nur], [nur_res])
    _check_capitals([kiev, har], [])
    _check_capitals([msk, ekb, nur], [msk_res, nur_res])
    _check_capitals([msk, ekb, nur], [msk_res, nur_res, kiev_res], 'Etc/GMT+2')
    _check_capitals([msk, nur], [msk_res, nur_res])
    _check_capitals([msk, nur, ekb], [msk_res, nur_res])
    _check_capitals([ur, ekb], [msk_res, nur_res])
    _check_capitals([piter, msk, kiev, har], [msk_res, kiev_res])
    _check_capitals([piter, har, piter], [msk_res, kiev_res])
    _check_capitals([piter, kiev, msk, ekb, ur, nur], [msk_res, kiev_res, nur_res])
    _check_capitals([piter, har, ur], [msk_res, kiev_res, nur_res])
    _check_capitals([piter, har, ur], [msk_res, kiev_res, nur_res], 'Etc/GMT+2')
    _check_capitals([piter, har, ur], [msk_res, kiev_res, nur_res], 'Etc/GMT+2')
    _check_capitals([ekb, ekb2], [msk_res], 'Etc/GMT+3')


def _check_capitals(stations, capitals, time_zone=None):
    schedule = []
    for i in range(0, len(stations)):
        schedule.append([
            None if i == 0 else i * 60 - 5,
            None if i == len(stations) - 1 else i * 60,
            stations[i]
        ])

    thread = create_thread(schedule_v1=schedule)
    segment = ThreadSegment(thread, stations[0], stations[-1])
    segment.naive_start_dt = datetime(2019, 5, 1, 6)

    _add_domain_capital_info(segment, 'RU')
    path = _make_path(segment, None)
    _add_capitals_time_zones_info(segment, path, time_zone)

    assert {str(cap) for cap in segment.thread.capitals} == {str(cap) for cap in capitals}


@replace_now('2019-04-10')
def test_united_thread_segment_class():
    st1 = create_station()
    st2 = create_station()

    def on_day(num):
        return datetime(2019, 3, 31) + timedelta(days=num)

    thread1 = create_thread(
        id=1, uid='1',
        title='thread',
        t_type=TransportType.TRAIN_ID,
        year_days=[on_day(1), on_day(11), on_day(21)],
        type=RThreadType.BASIC_ID,
        tz_start_time=time(23, 0),
        schedule_v1=[
            [None, 0, st1],
            [60, None, st2]
        ]
    )

    thread2 = create_thread(
        id=2, uid='2',
        title='thread',
        t_type=TransportType.TRAIN_ID,
        year_days=[on_day(3), on_day(13), on_day(23)],
        type=RThreadType.BASIC_ID,
        tz_start_time=time(23, 0),
        schedule_v1=[
            [None, 0, st1],
            [60, None, st2]
        ]
    )

    united = UnitedThreadSegment(('thread', '23:00', '00:00', True, True, 'basic'))
    segment = ThreadSegment(thread1, st1, st2)
    segment.rtstation_from = thread1.path[0]
    segment.naive_first_departure_dt = datetime(2019, 5, 1, 23)
    segment.current = True
    segment.first_departure_dt = datetime(2019, 5, 1, 0, 0, 0)
    segment.first_departure = '2019-05-01T23:00:00+03:00'
    united.segments.append(segment)

    segment = ThreadSegment(thread2, st1, st2)
    segment.rtstation_from = thread1.path[0]
    segment.naive_first_departure_dt = datetime(2019, 5, 3, 23)
    segment.current = False
    segment.first_departure_dt = datetime(2019, 5, 3, 0, 0, 0)
    segment.first_departure = '2019-05-03T23:00:00+03:00'
    united.segments.append(segment)

    united.calc_attributes(None)

    assert united.id == 1
    assert united.uid == '1'
    assert united.title == 'thread'
    assert united.start_departure_time == '23:00'
    assert united.stop_arrival_time == '00:00'
    assert united.in_context is True
    assert united.type == 'basic'
    assert united.current is True

    assert united.first_departure == '2019-05-01T23:00:00+03:00'
    assert united.days_text == 'только 1, 3, 11, 13, 21, 23 апреля'

    united.calc_attributes('Etc/GMT-5')

    assert united.days_text == 'только 2, 4, 12, 14, 22, 24 апреля'


@replace_now('2019-04-10')
def test_get_united_segments():
    st_start = create_station()
    st_from = create_station()
    st_to = create_station()

    def on_day(num):
        return datetime(2019, 3, 31) + timedelta(days=num)

    threads = [
        create_thread(
            id=1, uid='1',
            title='thread',
            t_type=TransportType.TRAIN_ID,
            tz_start_time=time(23, 0, 0),
            year_days=[on_day(1), on_day(11), on_day(36)],
            type=RThreadType.BASIC_ID,
            schedule_v1=[
                [None, 0, st_start],
                [120, 130, st_from],
                [180, None, st_to],
            ]
        ),
        create_thread(
            id=2, uid='2',
            title='thread',
            t_type=TransportType.TRAIN_ID,
            tz_start_time=time(23, 0, 0),
            year_days=[on_day(2), on_day(12), on_day(22), on_day(37)],
            type=RThreadType.BASIC_ID,
            schedule_v1=[
                [None, 0, st_start],
                [120, 130, st_from],
                [180, None, st_to],
            ]
        ),
        create_thread(
            id=3, uid='3',
            title='thread',
            t_type=TransportType.TRAIN_ID,
            tz_start_time=time(23, 0, 0),
            year_days=[on_day(3)],
            type=RThreadType.BASIC_ID,
            schedule_v1=[
                [None, 0, st_start],
                [120, 130, st_from],
                [180, None, st_to],
            ]
        ),
        create_thread(
            id=4, uid='4',
            title='another thread',
            t_type=TransportType.TRAIN_ID,
            tz_start_time=time(23, 0, 0),
            year_days=[on_day(4), on_day(14), on_day(39)],
            type=RThreadType.BASIC_ID,
            schedule_v1=[
                [None, 0, st_start],
                [120, 130, st_from],
                [180, None, st_to],
            ]
        ),
        create_thread(
            id=5, uid='5',
            title='thread',
            t_type=TransportType.TRAIN_ID,
            tz_start_time=time(23, 0, 0),
            year_days=[on_day(5), on_day(15), on_day(40)],
            type=RThreadType.BASIC_ID,
            schedule_v1=[
                [None, 0, st_start],
                [120, 130, st_from],
                [190, None, st_to],
            ]
        ),
        create_thread(
            id=6, uid='6',
            title='thread',
            t_type=TransportType.TRAIN_ID,
            tz_start_time=time(23, 0, 0),
            year_days=[on_day(6), on_day(16), on_day(41)],
            type=RThreadType.BASIC_ID,
            schedule_v1=[
                [None, 0, st_start],
                [180, None, st_to],
            ]
        ),
        create_thread(
            id=7, uid='7',
            title='thread',
            t_type=TransportType.TRAIN_ID,
            tz_start_time=time(23, 0, 0),
            year_days=[on_day(17)],
            type=RThreadType.BASIC_ID,
            schedule_v1=[
                [None, 0, st_start],
                [180, None, st_to],
            ]
        ),
        create_thread(
            id=8, uid='8',
            title='thread',
            t_type=TransportType.TRAIN_ID,
            tz_start_time=time(23, 0, 0),
            year_days=[on_day(8), on_day(18), on_day(43)],
            type=RThreadType.CANCEL_ID,
            schedule_v1=[
                [None, 0, st_start],
                [120, 130, st_from],
                [180, None, st_to],
            ]
        ),
        create_thread(
            id=9, uid='9',
            title='thread',
            t_type=TransportType.TRAIN_ID,
            tz_start_time=time(23, 0, 0),
            year_days=[on_day(19)],
            translated_manual_days_texts='{"0": {"ru": "день первый"}, "1": {"ru": "день второй"}}',
            type=RThreadType.BASIC_ID,
            schedule_v1=[
                [None, 0, st_start],
                [120, 130, st_from],
                [180, None, st_to],
            ]
        ),
        create_thread(
            id=10, uid='10',
            title='thread',
            t_type=TransportType.TRAIN_ID,
            tz_start_time=time(23, 0, 0),
            year_days=[on_day(20)],
            translated_days_texts='[{"ru": "нулевой"}, {"ru": "первый"}, {"ru": "второй"}]',
            type=RThreadType.BASIC_ID,
            schedule_v1=[
                [None, 0, st_start],
                [120, 130, st_from],
                [180, None, st_to],
            ]
        ),
    ]

    main_thread = create_thread(schedule_v1=[[None, 0, st_start], [120, 130, st_from], [180, None, st_to]])
    main_segment = ThreadSegment(main_thread, st_from, st_to)

    segments = []
    for thread in threads:
        segment = ThreadSegment(thread, st_from, st_to)
        segment.prepare_for_selector(main_segment, True, None)
        segment.current = segment.id == 5

        segments.append(segment)
    segments[7].start_departure_time = '01:10'
    segments[7].stop_arrival_time = '02:00'

    uniteds_list = _get_united_segments(segments, None)
    uniteds = {united.id: united for united in uniteds_list}

    assert len(uniteds) == 7

    _check_united_segment(uniteds[1], '1', '2019-04-12T01:10:00+03:00', 'только 3, 4, 12, 13, 23 апреля, 7, 8 мая')
    _check_united_segment(uniteds[4], '4', '2019-04-15T01:10:00+03:00', 'только 5, 15 апреля, 10 мая',
                          title='another thread')
    _check_united_segment(uniteds[5], '5', '2019-04-16T01:10:00+03:00', 'только 6, 16 апреля, 11 мая',
                          current=True, stop_arrival_time='02:10')
    _check_united_segment(uniteds[6], '6', '2019-04-16', 'только 6, 16, 17 апреля, 11 мая',
                          in_context=False, start_departure_time='23:00')
    _check_united_segment(uniteds[8], '8', '2019-04-19T01:10:00+03:00', '9, 19 апреля, 14 мая', thread_type='cancel')
    _check_united_segment(uniteds[9], '9', '2019-04-20T01:10:00+03:00', 'день второй')
    _check_united_segment(uniteds[10], '10', '2019-04-21T01:10:00+03:00', 'второй')


def _check_united_segment(united, uid, first_departure, days_text, title='thread', current=False, thread_type='basic',
                          start_departure_time='01:10', stop_arrival_time='02:00', in_context=True):
    assert united.uid == uid
    assert united.title == title
    assert united.current == current
    assert united.type == thread_type
    assert united.start_departure_time == start_departure_time
    assert united.stop_arrival_time == stop_arrival_time
    assert united.in_context == in_context
    assert united.first_departure == first_departure
    assert united.days_text == days_text


def test_get_related_threads():
    create_route(id=10, t_type=TransportType.TRAIN_ID, __={'threads': [
        {'canonical_uid': 'R_1', 'title': 'Поезд', 'type': RThreadType.BASIC_ID},
        {'canonical_uid': 'R_2', 'title': 'Вагон 2', 'type': RThreadType.THROUGH_TRAIN_ID},
        {'canonical_uid': 'R_3', 'title': 'Вагон 3', 'type': RThreadType.THROUGH_TRAIN_ID},
    ]})

    threads = {}
    for thread in RThread.objects.filter(route_id=10):
        threads[thread.canonical_uid] = thread

    related = _get_related_threads(threads['R_1'])
    assert len(related) == 2
    assert related[0].canonical_uid == 'R_2'
    assert related[0].title == 'Вагон 2'
    assert related[0].relation_type == RelatedThread.NO_CHANGE_WAGON_RELATION
    assert related[1].canonical_uid == 'R_3'
    assert related[1].title == 'Вагон 3'
    assert related[1].relation_type == RelatedThread.NO_CHANGE_WAGON_RELATION

    related = _get_related_threads(threads['R_2'])
    assert len(related) == 2
    assert related[0].canonical_uid == 'R_1'
    assert related[0].title == 'Поезд'
    assert related[0].relation_type == RelatedThread.BASIC_TRAIN_RELATION
    assert related[1].canonical_uid == 'R_3'
    assert related[1].title == 'Вагон 3'
    assert related[1].relation_type == RelatedThread.NO_CHANGE_WAGON_RELATION

    related = _get_related_threads(threads['R_3'])
    assert len(related) == 2
    assert related[0].canonical_uid == 'R_1'
    assert related[0].title == 'Поезд'
    assert related[0].relation_type == RelatedThread.BASIC_TRAIN_RELATION
    assert related[1].canonical_uid == 'R_2'
    assert related[1].title == 'Вагон 2'
    assert related[1].relation_type == RelatedThread.NO_CHANGE_WAGON_RELATION


@replace_now('2019-01-01')
def test_make_run_days():
    canonical_uid = 'R_canonical_uid'
    base_thread_date = datetime(2019, 1, 1, 0, 0, 0)
    thread_start_time = time(11, 15)
    shift_1d_12h = (24 + 12) * 60   # 1,5 day
    shift_3d = 24 * 3 * 60          # 3 days

    msk_tz = pytz.timezone('Etc/GMT-3')
    ash_tz = pytz.timezone('Etc/GMT-6')
    st1 = create_station(title='a', id=100, time_zone=msk_tz)
    st20 = create_station(title='a', id=200, time_zone=ash_tz)
    st21 = create_station(title='a', id=201, time_zone=ash_tz)
    st22 = create_station(title='a', id=202, time_zone=ash_tz)
    st23 = create_station(title='a', id=203, time_zone=ash_tz)
    st9 = create_station(title='b', id=900, time_zone=pytz.timezone('Etc/GMT-7'))

    threads = [
        create_thread(
            id=1,
            uid='t_1',
            canonical_uid=canonical_uid,
            t_type=TransportType.SUBURBAN_ID,
            year_days=[
                base_thread_date,
                base_thread_date + timedelta(days=7),
                base_thread_date + timedelta(days=7 * 2)
            ],
            tz_start_time=thread_start_time,
            schedule_v1=[  # path: 100->200->201->202->900
                [None, 0, st1, {'time_zone': msk_tz}],
                [-15, 0, st20, {'time_zone': msk_tz}],
                [shift_1d_12h - 15, shift_1d_12h, st21, {'time_zone': msk_tz}],
                [shift_1d_12h + 45, shift_1d_12h + 60, st22, {'time_zone': msk_tz}],
                [shift_3d - 15, None, st9, {'time_zone': msk_tz}],
            ]
        ),
        create_thread(
            id=2,
            uid='t_2',
            canonical_uid=canonical_uid,
            t_type=TransportType.SUBURBAN_ID,
            year_days=[
                base_thread_date + timedelta(days=1),
                base_thread_date + timedelta(days=7 + 1),
                base_thread_date + timedelta(days=7 * 2 + 1)
            ],
            tz_start_time=thread_start_time,
            schedule_v1=[  # path: 100->100->201->202(no_stop)->203->900
                [None, 0, st1, {'time_zone': msk_tz}],
                [-15, 0, st20, {'time_zone': msk_tz}],
                [shift_1d_12h - 30, shift_1d_12h, st21, {'time_zone': msk_tz}],
                [shift_1d_12h + 60, shift_1d_12h + 60, st22, {'time_zone': msk_tz}],
                [shift_1d_12h + 90, shift_1d_12h + 100, st23, {'time_zone': msk_tz}],
                [shift_3d - 30, None, st9, {'time_zone': msk_tz}],
            ]
        ),
        create_thread(
            id=3,
            uid='t_3',
            canonical_uid=canonical_uid,
            t_type=TransportType.SUBURBAN_ID,
            year_days=[
                base_thread_date + timedelta(days=3),
                base_thread_date + timedelta(days=7 + 3),
                base_thread_date + timedelta(days=7 * 2 + 3)
            ],
            tz_start_time=thread_start_time,
            schedule_v1=[  # path: 100->100->201->203(technical_stop)->900
                [None, 0, st1, {'time_zone': st1.time_zone}],
                [-15, 0, st20, {'time_zone': st20.time_zone}],
                [shift_1d_12h - 30, shift_1d_12h, st21, {'time_zone': st21.time_zone}],
                [shift_1d_12h + 90, shift_1d_12h + 100, st23, {'time_zone': msk_tz, 'is_technical_stop': True}],
                [shift_3d - 30, None, st9, {'time_zone': st9.time_zone}],
            ]
        ),
    ]

    _run_test_run_days_case(threads, st1, st9, [
        1, 2, 0, 3, 0, 0, 0, 1, 2, 0, 3, 0, 0, 0, 1, 2, 0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
    ])
    _run_test_run_days_case(threads, st20, st9, [
        1, 2, 0, 3, 0, 0, 0, 1, 2, 0, 3, 0, 0, 0, 1, 2, 0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
    ])
    _run_test_run_days_case(threads, st21, st9, [
        0, 0, 1, 2, 3, 0, 0, 0, 0, 1, 2, 3, 0, 0, 0, 0, 1, 2, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
    ])
    _run_test_run_days_case(threads, st1, st21, [
        1, 2, 0, 3, 0, 0, 0, 1, 2, 0, 3, 0, 0, 0, 1, 2, 0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
    ])
    _run_test_run_days_case(threads, st1, st22, [
        1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
    ])
    _run_test_run_days_case(threads, st1, st23, [
        0, 2, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
    ])
    _run_test_run_days_case(threads, st22, st9, [
        0, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
    ])

    _run_test_run_days_case(threads, st1, st9, [
        1, 2, 0, 3, 0, 0, 0, 1, 2, 0, 3, 0, 0, 0, 1, 2, 0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
    ], 'Etc/GMT-3')
    _run_test_run_days_case(threads, st20, st9, [
        1, 2, 0, 3, 0, 0, 0, 1, 2, 0, 3, 0, 0, 0, 1, 2, 0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
    ], 'Etc/GMT-3')
    _run_test_run_days_case(threads, st21, st9, [
        0, 1, 2, 0, 3, 0, 0, 0, 1, 2, 0, 3, 0, 0, 0, 1, 2, 0, 3, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
    ], 'Etc/GMT-3')
    _run_test_run_days_case(threads, st22, st9, [
        0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
    ], 'Etc/GMT+0')


def _run_test_run_days_case(threads, station_from, station_to, expected_run_days, time_zone=None):
    segments = []
    for thread in threads:
        segment = ThreadSegment(thread, station_from, station_to)
        main_segment = ThreadSegment(thread, station_from, station_to)
        segment.prepare_for_selector(main_segment, True, time_zone)
        segments.append(segment)

    run_days = _make_run_days(segments, time_zone)

    assert_that(run_days, has_entries({'2019': {'1': expected_run_days}}))


@replace_now('2019-01-01')
def test_run_days_thread_type_order():
    st1 = create_station()
    st2 = create_station()

    basic = create_thread(id=1, type=RThreadType.BASIC_ID, tz_start_time=time(10, 0),
                          year_days=[date(2019, 1, 1), date(2019, 1, 2), date(2019, 1, 3)],
                          schedule_v1=[[None, 0, st1], [20, None, st2]])
    assigment = create_thread(id=2, type=RThreadType.ASSIGNMENT_ID, tz_start_time=time(10, 0),
                              year_days=[date(2019, 1, 4), date(2019, 1, 5)],
                              schedule_v1=[[None, 0, st1], [20, None, st2]])
    change = create_thread(id=3, type=RThreadType.CHANGE_ID, tz_start_time=time(10, 0),
                           year_days=[date(2019, 1, 2), date(2019, 1, 4)],
                           schedule_v1=[[None, 0, st1], [20, None, st2]])
    cancel = create_thread(id=4, type=RThreadType.CANCEL_ID, tz_start_time=time(10, 0),
                           year_days=[date(2019, 1, 3)],
                           schedule_v1=[[None, 0, st1], [20, None, st2]])

    _run_test_run_days_case([cancel, change, assigment, basic], st1, st2, [
        1, 3, 4, 3, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
    ])


@replace_now('2019-01-01')
def test_make_united_run_days():
    st1 = create_station(id=10)
    st2 = create_station(id=20)
    st3 = create_station(id=30)

    create_thread(
        id=1,
        canonical_uid='R_1',
        t_type=TransportType.TRAIN_ID,
        title='thread',
        year_days=[date(2019, 1, 1), date(2019, 1, 4)],
        tz_start_time=time(23, 0),
        schedule_v1=[
            [None, 0, st1],
            [120, 130, st2],
            [180, None, st3],
        ]
    )

    create_thread(
        id=2,
        canonical_uid='R_1',
        t_type=TransportType.TRAIN_ID,
        title='thread',
        year_days=[date(2019, 1, 2), date(2019, 1, 5)],
        tz_start_time=time(23, 10),
        schedule_v1=[
            [None, 0, st1],
            [110, 120, st2],
            [170, None, st3],
        ]
    )

    context = ThreadContext(mixed_uid='R_1', station_from=st2, station_to=st3, departure_from_date=date(2019, 1, 2))
    result = get_thread(context)

    assert result['thread'].run_days == {'2019': {'1': [
        0, 1, 1, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
    ]}}

    context = ThreadContext(mixed_uid='R_1', station_from=st2, station_to=st3,
                            departure_from_date=date(2019, 1, 2), time_zone='Etc/GMT+0')
    result = get_thread(context)

    assert result['thread'].run_days == {'2019': {'1': [
        1, 1, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
    ]}}

    context = ThreadContext(mixed_uid='R_1', station_from=st1, station_to=st3, departure_from_date=date(2019, 1, 1))
    result = get_thread(context)

    assert result['thread'].run_days == {'2019': {'1': [
        1, 2, 0, 1, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
    ]}}

    context = ThreadContext(mixed_uid='R_1', station_from=st1, station_to=st3,
                            departure_from_date=date(2019, 1, 2), time_zone='Etc/GMT-5')
    result = get_thread(context)

    assert result['thread'].run_days == {'2019': {'1': [
        0, 1, 2, 0, 1, 2, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0
    ]}}


@replace_now('2019-01-01')
def test_make_run_days_text():
    segment = create_thread(uid='123', t_type=TransportType.SUBURBAN_ID)
    context = ThreadContext(mixed_uid=segment.uid)

    run_days_text = '***дни хождения***'
    with mock.patch.object(
        RThread, 'L_days_text', autospec=True, return_value=run_days_text
    ) as mock_L_days_text:
        result = get_thread(context)

    for call_args in mock_L_days_text.call_args_list:
        assert_that(call_args[1], has_entries({
            'except_separator': ', ',
            'html': False,
            'template_only': False,
            'show_days': True,
            'show_all_days': False,
        }))

    assert_that(result['thread'], has_property('run_days_text', run_days_text))
    for segment in result['threads']:
        assert_that(segment, has_property('days_text', run_days_text))


@replace_now('2019-05-01')
def test_other_today_threads():
    st1 = create_station(time_zone=(pytz.timezone('Etc/GMT-5')))
    st2 = create_station(time_zone=(pytz.timezone('Etc/GMT-3')))

    segments = [
        _make_segment_for_others(st1, st2, index=10, days=[1], zone_dt=datetime(2019, 5, 1, 10), stop_time='11:00'),
        _make_segment_for_others(st1, st2, index=11, days=[2], zone_dt=datetime(2019, 5, 1, 23), stop_time='02:00')
    ]

    _find_other_today_threads(segments[0], segments, None)
    assert hasattr(segments[0].thread, 'other_today_threads') is False

    _find_other_today_threads(segments[0], segments, 'Etc/GMT-3')
    assert hasattr(segments[0].thread, 'other_today_threads') is True
    assert len(segments[0].thread.other_today_threads) == 1
    other = segments[0].thread.other_today_threads[0]
    assert_that(other, has_entries({
        'uid': '11',
        'title': 'thread11',
        'start_departure_time': '23:00',
        'stop_arrival_time': '02:00',
        'departure_dt': '2019-05-02T01:00:00+05:00'
    }))

    segments = [
        _make_segment_for_others(st1, st2, index=20, days=[1], zone_dt=datetime(2019, 5, 1, 10), stop_time='11:00'),
        _make_segment_for_others(st1, st2, index=21, days=[1], zone_dt=datetime(2019, 5, 1, 21), stop_time='00:00')
    ]

    _find_other_today_threads(segments[0], segments, None)
    assert hasattr(segments[0].thread, 'other_today_threads') is True
    assert len(segments[0].thread.other_today_threads) == 1
    other = segments[0].thread.other_today_threads[0]
    assert_that(other, has_entries({
        'uid': '21',
        'title': 'thread21',
        'start_departure_time': '23:00',
        'stop_arrival_time': '00:00',
        'departure_dt': '2019-05-01T23:00:00+05:00'
    }))

    segments = [
        _make_segment_for_others(st1, st2, index=30, days=[1], zone_dt=datetime(2019, 5, 1, 10), stop_time='11:00'),

        _make_segment_for_others(st1, st2, index=31, days=[2], zone_dt=datetime(2019, 5, 1, 23), stop_time='02:00'),
        _make_segment_for_others(st1, st2, index=32, days=[3], zone_dt=datetime(2019, 5, 2, 23), stop_time='02:00'),

        _make_segment_for_others(st1, st2, index=33, days=[2], zone_dt=datetime(2019, 5, 2, 1), stop_time='04:00'),
        _make_segment_for_others(st1, st2, index=34, days=[3], zone_dt=datetime(2019, 5, 3, 1), stop_time='04:00')
    ]

    united1 = UnitedThreadSegment(('thread', '23:00', '02:00', True, True, 'basic'))
    united1.segments = segments[1:3]
    united2 = UnitedThreadSegment(('thread', '01:00', '04:00', True, True, 'basic'))
    united2.segments = segments[3:5]

    _find_other_today_threads(segments[0], [segments[0], united1, united2], 'Etc/GMT-3')
    assert hasattr(segments[0].thread, 'other_today_threads') is True
    assert len(segments[0].thread.other_today_threads) == 1
    other = segments[0].thread.other_today_threads[0]
    assert_that(other, has_entries({
        'uid': '31',
        'title': 'thread31',
        'start_departure_time': '23:00',
        'stop_arrival_time': '02:00',
        'departure_dt': '2019-05-02T01:00:00+05:00'
    }))


def _make_segment_for_others(st1, st2, index, days, zone_dt, stop_time):
    station_dt = zone_dt + timedelta(seconds=7200)
    thread = create_thread(
        uid=str(index), title='thread{}'.format(index),
        year_days=[datetime(2019, 5, day) for day in days],
        tz_start_time=station_dt.time(),
        schedule_v1=[
            [None, 0, st1, {'time_zone': pytz.timezone('Etc/GMT-5')}],
            [60, None, st2, {'time_zone': pytz.timezone('Etc/GMT-3')}]
        ]
    )
    segment = ThreadSegment(thread, st1, st2)
    segment.rtstation_from = segment.path[0]
    segment.rtstation_to = segment.path[1]
    segment.start_departure_time = zone_dt.time().strftime('%H:%M')
    segment.stop_arrival_time = stop_time
    segment.station_from_dt = station_dt
    segment.naive_first_departure_dt = station_dt
    return segment


@replace_now(datetime(2018, 11, 20, 16))
def test_suburban_thread_states():
    station_1 = create_station(id=601)
    station_2 = create_station(id=602)
    station_3 = create_station(id=603)

    thread_start_dt = datetime(2018, 11, 20, 16)
    thread_uid = '123'
    thread = create_thread(
        uid=thread_uid,
        number='456',
        t_type=TransportType.SUBURBAN_ID,
        tz_start_time=thread_start_dt.time(),
        year_days=[datetime(2018, 11, 20)],
        schedule_v1=[
            [None, 0, station_1],
            [10, 11, station_2],
            [20, None, station_3],
        ]
    )
    thread.run_days_text = 'дни хождения'

    rts_1 = RTStation.objects.get(thread=thread, station=station_1)
    rts_1_state = StationEventState()
    rts_1_state.key = 'rts_1_key'
    rts_1_state.departure = EventState(
        type_=EventStateType.FACT,
        dt=datetime(2018, 11, 20, 16, 3),
        tz='Etc/GMT-3',
        minutes_from=3,
        minutes_to=3,
    )

    rts_2 = RTStation.objects.get(thread=thread, station=station_2)
    rts_2_state = StationEventState()
    rts_2_state.key = 'rts_2_key'
    rts_2_state.arrival = EventState(
        type_=EventStateType.FACT_INTERPOLATED,
        minutes_from=-2,
        minutes_to=5,
    )
    rts_2_state.departure = EventState(
        type_=EventStateType.POSSIBLE_DELAY,
        minutes_from=1,
        minutes_to=6,
    )

    rts_3 = RTStation.objects.get(thread=thread, station=station_3)
    rts_3_state = StationEventState()
    rts_3_state.key = 'rts_3_key'
    rts_3_state.arrival = EventState(
        type_=EventStateType.POSSIBLE_OK
    )

    station_states = {rts_1: rts_1_state, rts_2: rts_2_state, rts_3: rts_3_state}

    segment = ThreadSegment(thread, None, None)
    segment.naive_start_dt = datetime(2018, 11, 20, 16)
    _add_domain_capital_info(segment, 'RU')
    path = _make_path(segment, None)
    _attach_states_to_stations(path, station_states)

    result, errors = ResponseSchema().dump({
        'thread': segment.thread,
        'rtstations': path
    })

    assert 'thread' in result
    assert_that(result['thread'], has_entries({
        'transportType': 'suburban',
        'uid': '123',
    }))

    assert 'rtstations' in result
    path = result['rtstations']
    assert len(path) == 3

    assert path[0]['id'] == 601
    state = path[0]['state']
    assert state == {
        'key': 'rts_1_key',
        'departure': {
            'factTime': '2018-11-20T16:03:00+03:00',
            'type': EventStateType.FACT,
            'minutesFrom': 3,
            'minutesTo': 3,
        }
    }

    assert path[1]['id'] == 602
    state = path[1]['state']
    assert state == {
        'key': 'rts_2_key',
        'arrival': {
            'type': EventStateType.FACT_INTERPOLATED,
            'minutesFrom': -2,
            'minutesTo': 5,
        },
        'departure': {
            'type': EventStateType.POSSIBLE_DELAY,
            'minutesFrom': 1,
            'minutesTo': 6,
        }
    }

    assert path[2]['id'] == 603
    state = path[2]['state']
    assert state == {
        'key': 'rts_3_key',
        'arrival': {
            'type': EventStateType.POSSIBLE_OK,
        }
    }


@replace_now('2018-11-20')
@pytest.mark.parametrize(
    'draw_path_result, expected_map_data',
    [
        [
            {
                'stations': [
                    [
                        [20.20, 10.10],
                        'Москва (Ленинградский вокзал)',
                        None,
                        '05:22',
                        2006004
                    ],
                    [
                        [21.21, 11.11],
                        'Рижская',
                        '05:26',
                        '05:27',
                        9603518
                    ],
                ],
                'segments': [
                    [
                        [[20.20, 10.10], [21.21, 11.11]],
                        'ANOMA',
                    ],
                ],
                'first': [20.20, 10.10],
                'last': [21.21, 11.11],
                'position': [21.21, 11.11],
            },
            {
                'stations': [
                    [
                        [10.10, 20.20],
                        'Москва (Ленинградский вокзал)',
                        None,
                        '05:22',
                        2006004
                    ],
                    [
                        [11.11, 21.21],
                        'Рижская',
                        '05:26',
                        '05:27',
                        9603518
                    ],
                ],
                'segments': [
                    [
                        [[10.10, 20.20], [11.11, 21.21]],
                        'ANOMA',
                    ],
                ],
                'first': [10.10, 20.20],
                'last': [11.11, 21.21],
                'position': [11.11, 21.21],
            }

        ]
    ]
)
def test_get_map(draw_path_result, expected_map_data):
    thread_tzinfo = pytz.timezone('Etc/GMT-4')
    station_from = create_station(title='from', id=111, time_zone=thread_tzinfo)
    station_to = create_station(title='to', id=222, time_zone=thread_tzinfo)
    departure_date = datetime(2018, 11, 20)
    thread_start_time = time(0, 5)
    departure_shift = 11
    thread = create_thread(
        uid='123',
        t_type=TransportType.SUBURBAN_ID,
        year_days=[datetime(2018, 11, 5), departure_date],
        tz_start_time=thread_start_time,
        schedule_v1=[
            [None, 0, station_from, {'time_zone': thread_tzinfo}],
            [departure_shift, None, station_to, {'time_zone': thread_tzinfo}],
        ]
    )

    context = ThreadContext(uid=thread.uid,
                            station_from=station_from,
                            station_to=station_to,
                            departure_from=datetime.combine(departure_date.date(), thread_start_time))

    mock_draw_path_result = mock.Mock()
    mock_draw_path_result.__json__ = mock.Mock(return_value=draw_path_result)
    with mock.patch.object(
        travel.rasp.morda_backend.morda_backend.thread.data_layer, 'draw_path',
        return_value=mock_draw_path_result
    ) as mock_draw_path:
        map_data = get_thread_map(context)

    mock_draw_path.assert_called_once()
    draw_path_kwargs = mock_draw_path.call_args_list[0][1]
    assert_that(draw_path_kwargs, has_entries({
        'thread': thread,
        'thread_start_dt': datetime.combine(departure_date.date(), thread_start_time),
        'path': list(thread.path),
        'first': station_from,
        'last': station_to,
    }))

    assert_that(map_data, has_entries(expected_map_data))


def test_train_full_title():
    st1 = create_station()
    st2 = create_station()
    st3 = create_station()

    thread = create_thread(
        number='122A',
        title='Москва - Екатеринбург',
        t_type=TransportType.TRAIN_ID,
        year_days=[datetime(2020, 8, 8)],
        schedule_v1=[
            [None, 0, st1, {'train_number': '122A'}],
            [11, 22, st2, {'train_number': '121A'}],
            [33, None, st3, {'train_number': '121A'}]
        ]
    )

    segment = ThreadSegment(thread, None, None)

    assert segment.get_thread_full_title() == 'Поезд 122A/121A Москва - Екатеринбург'
