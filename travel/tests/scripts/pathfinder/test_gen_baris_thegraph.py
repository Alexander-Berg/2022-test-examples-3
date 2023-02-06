# coding: utf8
from __future__ import absolute_import, division, print_function, unicode_literals

import json
from datetime import date, time

import mock
import pytz

import pytest
from google.protobuf import json_format

from common.models.transport import TransportType
from common.models.schedule import RThreadType
from common.tester.factories import create_station
from common.tester.utils.datetime import replace_now
from common.utils.date import RunMask
from travel.rasp.library.python.common23.date import environment
from common.utils.tz_mask_split import ThreadForMaskSplit
from travel.proto.dicts.avia.schedule_dump_pb2 import TFlight

from travel.rasp.rasp_scripts.scripts.pathfinder.helpers import get_to_pathfinder_year_days_converter
from travel.rasp.rasp_scripts.scripts.pathfinder.gen_baris_thegraph import (
    _make_baris_thegraph_thread, _get_mask_last_run_day, ThegraphStation, _iterate_thread_thegraph_rows,
    _iterate_on_baris_all_flights
)


@replace_now('2020-09-01')
def test_make_baris_thegraph_thread():
    kiev_pytz = pytz.timezone('Europe/Kiev')
    moscow_pytz = pytz.timezone('Europe/Moscow')
    stations_pytz_dict = {101: moscow_pytz, 102: moscow_pytz, 103: kiev_pytz}

    flight_json = json.dumps({
        'Title': 'SU 1',
        'AirlineID': 301,
        'Schedules': [
            {
                'Route': [
                    {
                        'AirportID': 101,
                        'ArrivalTime': '',
                        'ArrivalDayShift': 0,
                        'DepartureTime': '01:00:00',
                        'DepartureDayShift': 0
                    },
                    {
                        'AirportID': 102,
                        'ArrivalTime': '02:00:00',
                        'ArrivalDayShift': 0,
                        'DepartureTime': '03:00:00',
                        'DepartureDayShift': 1
                    },
                    {
                        'AirportID': 103,
                        'ArrivalTime': '04:00:00',
                        'ArrivalDayShift': 2,
                        'DepartureTime': '',
                        'DepartureDayShift': 0
                    }
                ],
                'Masks': [
                    {
                        'From': '2020-10-01',
                        'Until': '2020-10-11',
                        'On': 12
                    },
                    {
                        'From': '2020-10-21',
                        'Until': '2020-10-31',
                        'On': 3
                    }
                ]
            },
            {
                'Route': [
                    {
                        'AirportID': 101,
                        'ArrivalTime': '',
                        'ArrivalDayShift': 0,
                        'DepartureTime': '11:00:00',
                        'DepartureDayShift': 0
                    },
                    {
                        'AirportID': 103,
                        'ArrivalTime': '14:00:00',
                        'ArrivalDayShift': 1,
                        'DepartureTime': '',
                        'DepartureDayShift': 0
                    }
                ],
                'Masks': [
                    {
                        'From': '2020-10-01',
                        'Until': '2020-10-02',
                        'On': 4
                    }
                ]
            }
        ]
    })
    flight = json_format.Parse(flight_json, TFlight())

    thegraph_thread = _make_baris_thegraph_thread(flight.Schedules[0], stations_pytz_dict)
    assert thegraph_thread.run_mask.dates() == [
        date(2020, 10, 5), date(2020, 10, 6), date(2020, 10, 21), date(2020, 10, 28)
    ]

    assert len(thegraph_thread.stations) == 3
    station = thegraph_thread.stations[0]
    assert station.station_id == 101
    assert station.arrival_time is None
    assert station.arrival_day_shift == 0
    assert station.departure_time == time(1)
    assert station.departure_day_shift == 0

    station = thegraph_thread.stations[1]
    assert station.station_id == 102
    assert station.arrival_time == time(2)
    assert station.arrival_day_shift == 0
    assert station.departure_time == time(3)
    assert station.departure_day_shift == 1

    station = thegraph_thread.stations[2]
    assert station.station_id == 103
    assert station.arrival_time == time(4)
    assert station.arrival_day_shift == 2
    assert station.departure_time is None
    assert station.departure_day_shift == 0

    thegraph_thread = _make_baris_thegraph_thread(flight.Schedules[1], stations_pytz_dict)
    assert thegraph_thread.run_mask.dates() == [date(2020, 10, 1)]

    assert len(thegraph_thread.stations) == 2

    station = thegraph_thread.stations[0]
    assert station.station_id == 101
    assert station.arrival_time is None
    assert station.arrival_day_shift == 0
    assert station.departure_time == time(11)
    assert station.departure_day_shift == 0

    station = thegraph_thread.stations[1]
    assert station.station_id == 103
    assert station.arrival_time == time(14)
    assert station.arrival_day_shift == 1
    assert station.departure_time is None
    assert station.departure_day_shift == 0


@replace_now('2020-09-01')
def test_get_mask_last_run_day():
    today = environment.today()
    run_mask = RunMask(today=today, days=[date(2020, 9, 2), date(2020, 9, 4)])
    assert _get_mask_last_run_day(run_mask) == date(2020, 9, 4)

    run_mask = RunMask(today=today, days=[date(2020, 8, 30), date(2020, 9, 3)])
    assert _get_mask_last_run_day(run_mask) == date(2020, 9, 3)

    run_mask = RunMask(today=today, days=[date(2020, 8, 28), date(2020, 8, 29)])
    assert _get_mask_last_run_day(run_mask) == date(2020, 8, 29)

    run_mask = RunMask(today=today, days=[])
    assert _get_mask_last_run_day(run_mask) is None


@replace_now('2020-09-01')
def test_iterate_thread_thegraph_rows():
    today = environment.today()
    ekb_pytz = pytz.timezone('Asia/Yekaterinburg')
    year_days_fun = get_to_pathfinder_year_days_converter(today)
    run_mask = RunMask(today=today, days=[date(2020, 9, 2), date(2020, 9, 4)])

    thegraph_thread = ThreadForMaskSplit(run_mask, [
        ThegraphStation(
            station_id=101,
            station_pytz=ekb_pytz,
            arrival_time=None,
            arrival_day_shift=0,
            departure_time=time(23),
            departure_day_shift=0
        ),
        ThegraphStation(
            station_id=102,
            station_pytz=ekb_pytz,
            arrival_time=time(2, 30),
            arrival_day_shift=1,
            departure_time=time(3),
            departure_day_shift=1
        ),
        ThegraphStation(
            station_id=103,
            station_pytz=ekb_pytz,
            arrival_time=time(4),
            arrival_day_shift=1,
            departure_time=None,
            departure_day_shift=0
        ),
    ])

    thegraph_rows = [
        row for row in _iterate_thread_thegraph_rows(
            thegraph_thread, 'RUSQCU-1_200904_c10_12', 'ЧУ 1'.encode('utf-8'), date(2020, 9, 4), run_mask, year_days_fun
        )
    ]

    assert thegraph_rows == [
        map(unicode, [
            101, 102, '0', 'RUSQCU-1_200904_c10_12',
            time(21), 0, 210, 'ЧУ 1', TransportType.PLANE_ID, RThreadType.BASIC_ID,
            year_days_fun(RunMask(today=today, days=[date(2020, 9, 2), date(2020, 9, 4)]))
        ]),
        map(unicode, [
            102, 103, '0', 'RUSQCU-1_200904_c10_12',
            time(1), 30, 60, 'ЧУ 1', TransportType.PLANE_ID, RThreadType.BASIC_ID,
            year_days_fun(RunMask(today=today, days=[date(2020, 9, 3), date(2020, 9, 5)]))
        ])
    ]

    thegraph_rows = [
        row for row in _iterate_thread_thegraph_rows(
            thegraph_thread, 'SU-1_200904_c10_12', 'SU 1', date(2020, 9, 4), run_mask, year_days_fun
        )
    ]

    assert thegraph_rows[0] == map(unicode, [
        101, 102, '0', 'SU-1_200904_c10_12', time(21), 0, 210, 'SU 1', TransportType.PLANE_ID, RThreadType.BASIC_ID,
        year_days_fun(RunMask(today=today, days=[date(2020, 9, 2), date(2020, 9, 4)]))
    ])


@pytest.mark.dbuser
@replace_now('2020-09-01')
def test_iterate_on_baris_all_flights():
    today = environment.today()
    year_days_fun = get_to_pathfinder_year_days_converter(today)
    ekb_pytz = pytz.timezone('Asia/Yekaterinburg')

    create_station(id=101, time_zone=ekb_pytz, t_type_id=TransportType.PLANE_ID)
    create_station(id=102, time_zone=ekb_pytz, t_type_id=TransportType.PLANE_ID)

    flights = []
    for index in range(1, 4):
        flight_json = json.dumps({
            'Title': 'SU {}'.format(index),
            'AirlineID': 301,
            'Schedules': [
                {
                    'Route': [
                        {
                            'AirportID': 101,
                            'ArrivalTime': '',
                            'ArrivalDayShift': 0,
                            'DepartureTime': '0{}:00:00'.format(index),
                            'DepartureDayShift': 0
                        },
                        {
                            'AirportID': 102,
                            'ArrivalTime': '0{}:00:00'.format(index + 1),
                            'ArrivalDayShift': 0,
                            'DepartureTime': '',
                            'DepartureDayShift': 0
                        }
                    ],
                    'Masks': [
                        {
                            'From': '2020-10-01',
                            'Until': '2020-10-01',
                            'On': 4
                        },
                    ]
                }
            ]
        })
        flights.append(json_format.Parse(flight_json, TFlight()))

    with mock.patch(
        'travel.rasp.rasp_scripts.scripts.pathfinder.gen_baris_thegraph.all_flights_iterate', return_value=flights
    ):
        thegraph_rows = [row for row in _iterate_on_baris_all_flights()]

    assert len(thegraph_rows) == 3
    assert thegraph_rows == [
        map(unicode, [
            101, 102, '0', 'SU-1_201001_c301_12', time(23), 0, 60, 'SU 1',
            TransportType.PLANE_ID, RThreadType.BASIC_ID,
            year_days_fun(RunMask(today=today, days=[date(2020, 9, 30)]))
        ]),
        map(unicode, [
            101, 102, '0', 'SU-2_201001_c301_12', time(0), 0, 60, 'SU 2',
            TransportType.PLANE_ID, RThreadType.BASIC_ID,
            year_days_fun(RunMask(today=today, days=[date(2020, 10, 1)]))
        ]),
        map(unicode, [
            101, 102, '0', 'SU-3_201001_c301_12', time(1), 0, 60, 'SU 3',
            TransportType.PLANE_ID, RThreadType.BASIC_ID,
            year_days_fun(RunMask(today=today, days=[date(2020, 10, 1)]))
        ])
    ]
