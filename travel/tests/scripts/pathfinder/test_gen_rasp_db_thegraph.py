# coding: utf8
from __future__ import absolute_import, division, print_function, unicode_literals

from datetime import date, time, timedelta, datetime

import pytest

from common.models.schedule import RThreadType
from common.models.transport import TransportType
from common.tester.factories import create_thread, create_station
from common.tester.utils.datetime import replace_now
from common.utils.date import KIEV_TZ, MSK_TZ
from common.utils.tz_mask_split import MaskSplitter

from travel.rasp.rasp_scripts.scripts.pathfinder.helpers import get_to_pathfinder_year_days_converter
from travel.rasp.rasp_scripts.scripts.pathfinder.gen_rasp_db_thegraph import (
    _get_rows_for_thread, _split_thread_mask_to_given_tz_invariant_chunks
)
from travel.rasp.rasp_scripts.scripts.pathfinder.mask_builder.mask_builders import MaskBuilder


@pytest.mark.dbuser
def test_msk_thread():
    today = date(2015, 11, 1)
    builder = MaskBuilder(today - timedelta(14), today + timedelta(200), today)
    to_pathfinder_year_days = get_to_pathfinder_year_days_converter(today)
    splitter = MaskSplitter()
    st1 = create_station(time_zone=MSK_TZ)
    st2 = create_station(time_zone=MSK_TZ)
    st3 = create_station(time_zone=MSK_TZ)

    thread = create_thread(
        uid='uid', tz_start_time='10:00', number='number', t_type='bus', type=RThreadType.BASIC_ID,
        express_type=None, time_zone=MSK_TZ, schedule_v1=[
            [None, 0, st1],
            [10, 20, st2],
            [30, None, st3],
        ],
        year_days=builder.daily_mask()
    )
    rtstations = list(thread.rtstation_set.all().order_by('id'))

    rows = list(_get_rows_for_thread(thread, rtstations, today, to_pathfinder_year_days, splitter))
    assert rows == [
        map(unicode, [st1.id, st2.id, '0', 'uid', time(10), 0, 10, 'number',
                      TransportType.BUS_ID, RThreadType.BASIC_ID, to_pathfinder_year_days(builder.daily_mask())]),
        map(unicode, [st2.id, st3.id, '0', 'uid', time(10, 20), 10, 10, 'number',
                      TransportType.BUS_ID, RThreadType.BASIC_ID, to_pathfinder_year_days(builder.daily_mask())]),
    ]


@pytest.mark.dbuser
def test_msk_thread_minus_one_duration_hack():
    today = date(2015, 11, 1)
    builder = MaskBuilder(today - timedelta(14), today + timedelta(200), today)
    to_pathfinder_year_days = get_to_pathfinder_year_days_converter(today)
    splitter = MaskSplitter()
    st1 = create_station(time_zone=MSK_TZ)
    st2 = create_station(time_zone=MSK_TZ)
    st3 = create_station(time_zone=MSK_TZ)

    thread = create_thread(
        uid='uid', tz_start_time='10:00', number='number', t_type='bus', type=RThreadType.BASIC_ID,
        express_type=None, time_zone=MSK_TZ, schedule_v1=[
            [None, 0, st1],
            [0, 1, st2],
            [0, None, st3],
        ],
        year_days=builder.daily_mask()
    )
    rtstations = list(thread.rtstation_set.all().order_by('id'))

    rows = list(_get_rows_for_thread(thread, rtstations, today, to_pathfinder_year_days, splitter))
    assert rows == [
        map(unicode, [st1.id, st2.id, '0', 'uid', time(10), 0, 0, 'number',
                      TransportType.BUS_ID, RThreadType.BASIC_ID, to_pathfinder_year_days(builder.daily_mask())]),
        map(unicode, [st2.id, st3.id, '0', 'uid', time(10, 1), 1, 0, 'number',
                      TransportType.BUS_ID, RThreadType.BASIC_ID, to_pathfinder_year_days(builder.daily_mask())]),
    ]


def _make_days(colon_date_format):
    month_name_to_digit = {
        'Dec': 12, 'Jan': 1, 'Feb': 2,
        'Mar': 3, 'Apr': 4, 'May': 5,
        'Jun': 6, 'Jul': 7, 'Aug': 8,
        'Sep': 9, 'Oct': 10, 'Nov': 11
    }
    dates = []
    year = None
    prev_month = None
    for month_days in colon_date_format.split(';'):
        parts = month_days.split()
        if len(parts) == 3:
            days, month_name, year = parts
            current_year = int(year)
        else:
            days, month_name = parts
            current_year = None
        month = month_name_to_digit[month_name]
        if not current_year:
            assert year
            if prev_month is not None and prev_month >= month:
                year += 1
        else:
            year = current_year

        for d in days.split(','):
            dates.append(date(year, month_name_to_digit[month_name], int(d)))

        prev_month = month

    return dates


def _create_thread(stops, days, mask_builder):
    stations = [create_station(time_zone=tz) for dep_time, tz in stops]
    dates = _make_days(days)
    mask = mask_builder.mask_from_day_condition(lambda d: d in dates)

    schedule_v1 = []
    _new_stops = []
    for time_info, tz in stops:
        parts = time_info.split()
        dep_time = time(*map(int, parts[0].split(':')))
        if len(parts) == 2:
            day_shift = int(parts[1])
        else:
            day_shift = 0

        _new_stops.append((dep_time, tz, day_shift))

    stops = _new_stops

    for idx, (dep_time, tz, day_shift) in enumerate(stops):
        event_dt = datetime.combine(mask_builder.today, dep_time) + timedelta(day_shift)
        if idx == 0:
            schedule_v1.append([None, 0, stations[idx], {'time_zone': stations[idx].time_zone}])
            start_dt = event_dt
        elif idx < len(stops) - 1:
            shift = int((event_dt - start_dt).total_seconds()) / 60
            schedule_v1.append([shift - 1, shift, stations[idx], {'time_zone': stations[idx].time_zone}])
        else:
            shift = int((event_dt - start_dt).total_seconds()) / 60
            schedule_v1.append([shift, None, stations[idx], {'time_zone': stations[idx].time_zone}])

    thread = create_thread(
        tz_start_time=start_dt.time(), number='number', t_type='bus', type=RThreadType.BASIC_ID,
        express_type=None, time_zone=MSK_TZ, schedule_v1=schedule_v1,
        year_days=str(mask)
    )

    thread.stations = stations
    thread.mask = mask
    thread.rtstations = list(thread.rtstation_set.all().order_by('id'))
    return thread


@pytest.mark.parametrize('schedule,expected_split_result', [
    [{
        'stops': [['10:00 0', KIEV_TZ], ['12:00', KIEV_TZ], ['14:00', KIEV_TZ]],
        'days': '27,28,29,30 Mar 2015; 1 Apr'
    }, ['27,28 Mar 2015', '29,30 Mar 2015; 1 Apr']],
    [{
        'stops': [['22:00 0', KIEV_TZ], ['02:00 1', KIEV_TZ], ['06:00 1', KIEV_TZ]],
        'days': '26,27,28,29,30 Mar 2015; 1 Apr'
    }, ['26,27 Mar 2015', '28 Mar 2015', '29,30 Mar 2015; 1 Apr']],
    [{
        'stops': [['22:00 0', MSK_TZ], ['02:00 1', KIEV_TZ], ['06:00 1', KIEV_TZ]],
        'days': '26,27,28,29,30 Mar 2015; 1 Apr'
    }, ['26,27 Mar 2015', '28 Mar 2015', '29,30 Mar 2015; 1 Apr']],
    [{
        'stops': [['22:00 0', MSK_TZ], ['04:00 1', KIEV_TZ]],
        'days': '26,27,28,29,30 Mar 2015; 1 Apr'
    }, ['26,27 Mar 2015', '28,29,30 Mar 2015; 1 Apr']],
    [{
        'stops': [['22:00 0', MSK_TZ], ['03:00 1', KIEV_TZ]],
        'days': '26,27,28,29,30 Mar 2015; 1 Apr'
    }, ['26,27,28 Mar 2015', '29,30 Mar 2015; 1 Apr']],
    [{
        'stops': [['5:00 0', KIEV_TZ], ['05:00 1', KIEV_TZ], ['05:00 2', KIEV_TZ], ['05:00 3', KIEV_TZ]],
        'days': '24,25,26,27,28,29,30 Mar 2015; 1 Apr'
    }, ['24,25 Mar 2015', '26 Mar 2015', '27 Mar 2015', '28 Mar 2015', '29,30 Mar 2015; 1 Apr']],
])
@pytest.mark.dbuser
@replace_now('2015-03-10 00:00:00')
def test_split_thread_mask_to_msk_invariant_intervals(schedule, expected_split_result):
    today = date(2015, 3, 1)
    builder = MaskBuilder(today - timedelta(14), today + timedelta(200), today)
    expected_split_result = {
        builder.mask_from_day_condition(lambda d: d in _make_days(sr))
        for sr in expected_split_result
    }
    splitter = MaskSplitter()
    thread = _create_thread(schedule['stops'], schedule['days'], builder)

    split_result = _split_thread_mask_to_given_tz_invariant_chunks(
        thread.mask, thread.rtstations, thread.tz_start_time, MSK_TZ, splitter
    )

    assert set(split_result) == expected_split_result


@pytest.mark.dbuser
def test_kiev_thread():
    today = date(2015, 3, 1)
    builder = MaskBuilder(today - timedelta(14), today + timedelta(200), today)
    to_pathfinder_year_days = get_to_pathfinder_year_days_converter(today)
    splitter = MaskSplitter()
    st1 = create_station(time_zone=KIEV_TZ)
    st2 = create_station(time_zone=KIEV_TZ)
    st3 = create_station(time_zone=KIEV_TZ)

    dates = _make_days('27,28,29,30 Mar 2015')
    thread = create_thread(
        uid='uid', tz_start_time='10:00', number='number', t_type='bus', type=RThreadType.BASIC_ID,
        express_type=None, time_zone=MSK_TZ, schedule_v1=[
            [None, 0, st1, {'time_zone': st1.time_zone}],
            [10, 20, st2, {'time_zone': st2.time_zone}],
            [30, None, st3, {'time_zone': st3.time_zone}],
        ],
        year_days=builder.mask_from_day_condition(lambda d: d in dates)
    )
    rtstations = list(thread.rtstation_set.all().order_by('id'))

    dates1 = _make_days('27,28 Mar 2015')
    dates2 = _make_days('29,30 Mar 2015')

    rows = list(_get_rows_for_thread(thread, rtstations, today, to_pathfinder_year_days, splitter))
    assert rows == [
        map(unicode, [st1.id, st2.id, '0', 'uid', time(11), 0, 10, 'number',
                      TransportType.BUS_ID, RThreadType.BASIC_ID,
                      to_pathfinder_year_days(builder.mask_from_day_condition(lambda d: d in dates1))]),
        map(unicode, [st2.id, st3.id, '0', 'uid', time(11, 20), 10, 10, 'number',
                      TransportType.BUS_ID, RThreadType.BASIC_ID,
                      to_pathfinder_year_days(builder.mask_from_day_condition(lambda d: d in dates1))]),
        map(unicode, [st1.id, st2.id, '0', 'uid(1)', time(10), 0, 10, 'number',
                      TransportType.BUS_ID, RThreadType.BASIC_ID,
                      to_pathfinder_year_days(builder.mask_from_day_condition(lambda d: d in dates2))]),
        map(unicode, [st2.id, st3.id, '0', 'uid(1)', time(10, 20), 10, 10, 'number',
                      TransportType.BUS_ID, RThreadType.BASIC_ID,
                      to_pathfinder_year_days(builder.mask_from_day_condition(lambda d: d in dates2))]),
    ]


@pytest.mark.dbuser
def test_kiev_thread_minus_one_duration_hack():
    today = date(2015, 3, 1)
    builder = MaskBuilder(today - timedelta(14), today + timedelta(200), today)
    to_pathfinder_year_days = get_to_pathfinder_year_days_converter(today)
    splitter = MaskSplitter()
    st1 = create_station(time_zone=KIEV_TZ)
    st2 = create_station(time_zone=KIEV_TZ)
    st3 = create_station(time_zone=KIEV_TZ)

    dates = _make_days('27,28,29,30 Mar 2015')
    thread = create_thread(
        uid='uid', tz_start_time='10:00', number='number', t_type='bus', type=RThreadType.BASIC_ID,
        express_type=None, time_zone=MSK_TZ, schedule_v1=[
            [None, 0, st1, {'time_zone': st1.time_zone}],
            [0, 1, st2, {'time_zone': st2.time_zone}],
            [0, None, st3, {'time_zone': st3.time_zone}],
        ],
        year_days=builder.mask_from_day_condition(lambda d: d in dates)
    )
    rtstations = list(thread.rtstation_set.all().order_by('id'))

    dates1 = _make_days('27,28 Mar 2015')
    dates2 = _make_days('29,30 Mar 2015')

    rows = list(_get_rows_for_thread(thread, rtstations, today, to_pathfinder_year_days, splitter))
    assert rows == [
        map(unicode, [st1.id, st2.id, '0', 'uid', time(11), 0, 0, 'number',
                      TransportType.BUS_ID, RThreadType.BASIC_ID,
                      to_pathfinder_year_days(builder.mask_from_day_condition(lambda d: d in dates1))]),
        map(unicode, [st2.id, st3.id, '0', 'uid', time(11, 1), 1, 0, 'number',
                      TransportType.BUS_ID, RThreadType.BASIC_ID,
                      to_pathfinder_year_days(builder.mask_from_day_condition(lambda d: d in dates1))]),
        map(unicode, [st1.id, st2.id, '0', 'uid(1)', time(10), 0, 0, 'number',
                      TransportType.BUS_ID, RThreadType.BASIC_ID,
                      to_pathfinder_year_days(builder.mask_from_day_condition(lambda d: d in dates2))]),
        map(unicode, [st2.id, st3.id, '0', 'uid(1)', time(10, 1), 1, 0, 'number',
                      TransportType.BUS_ID, RThreadType.BASIC_ID,
                      to_pathfinder_year_days(builder.mask_from_day_condition(lambda d: d in dates2))]),
    ]
