# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import mock
import pytest
from hamcrest import assert_that, contains_inanyorder

from common.data_api.baris.test_helpers import mock_baris_response
from common.models.geo import Station2Settlement
from common.models.schedule import RThread
from common.models.transport import TransportType as tp

from travel.rasp.suggests_tasks.suggests.generate import shared_objects
from travel.rasp.suggests_tasks.suggests.generate.ttypes import (
    get_ttypes, get_ttypes_for_ids, get_thread_ttypes, merge_stations_ttypes, get_settlements_ttypes,
    get_baris_station_ids
)
from travel.rasp.suggests_tasks.suggests.generate.utils import generate_parallel

from common.tester.testcase import TestCase
from common.tester.factories import create_station, create_thread, create_settlement


class TestTransportTypes(TestCase):
    def test_get_ttypes_for_ids(self):
        stations = [create_station(id=i) for i in range(1, 5)]
        threads = []

        for i, (t_type, schedule) in enumerate(
                [
                    (tp.BUS_ID,
                     [
                         [None, 0, stations[0], {'id': 11}],
                         [10, None,  stations[1], {'id': 12}]
                     ]),
                    (tp.SUBURBAN_ID,
                     [
                         [None, 0, stations[1], {'id': 21}],
                         [10, None, stations[2], {'id': 22}]
                     ]),
                    (tp.TRAIN_ID,
                     [
                         [None, 0, stations[2], {'id': 31}],
                         [10, None, stations[3], {'id': 32}]
                     ])
                ], 1):
            threads.append(create_thread(id=i, t_type=t_type, schedule_v1=schedule))

        shared_objects.set_objs(threads_ttypes={thread_id: t_type for thread_id, t_type in
                                                RThread.objects.filter().values_list('id', 't_type_id')})

        rtstations_ids = [11, 22, 31]
        ttypes_for_ids = get_ttypes_for_ids((0, rtstations_ids))

        assert ttypes_for_ids == {1: {tp.BUS_ID}, 3: {tp.SUBURBAN_ID, tp.TRAIN_ID}}

    def test_get_thread_ttypes(self):
        for i, t_type in enumerate([tp.BUS_ID, tp.SUBURBAN_ID, tp.TRAIN_ID], 1):
            create_thread(id=i, t_type=t_type)
        assert get_thread_ttypes() == {1: tp.BUS_ID, 2: tp.SUBURBAN_ID, 3: tp.TRAIN_ID}

    def test_merge_stations_ttypes(self):
        ttypes_for_ids_iter = [{1: {10}, 3: {11, 12}},
                               {1: {13}, 2: {14}, 3: {11, 15}}]

        assert merge_stations_ttypes(ttypes_for_ids_iter) == {1: {10, 13},
                                                              2: {14},
                                                              3: {11, 12, 15}}

    def test_settlements_ttypes(self):
        settlements = [create_settlement(id=i) for i in range(1, 4)]
        stations = [create_station(id=i, settlement=v) for i, v in enumerate(settlements * 2, 1)]
        stations_ttypes = {1: {10, 11},
                           2: {12},
                           3: {13, 14},
                           4: {15, 16},
                           5: {17, 19},
                           6: {20}}

        settlements_ttypes = {k: set(v) for k, v in get_settlements_ttypes(stations_ttypes).items()}

        assert settlements_ttypes == {1: {16, 10, 11, 15},
                                      2: {17, 19, 12},
                                      3: {20, 13, 14}}

        Station2Settlement.objects.create(station=stations[1], settlement=settlements[0])
        Station2Settlement.objects.create(station=stations[5], settlement=settlements[1])

        settlements_ttypes = {k: set(v) for k, v in get_settlements_ttypes(stations_ttypes).items()}
        assert settlements_ttypes == {1: {16, 10, 11, 12, 15},
                                      2: {17, 19, 12, 20},
                                      3: {20, 13, 14}}


@pytest.mark.dbripper
def test_get_ttypes():
    settlements = [create_settlement(id=i) for i in range(1, 3)]
    stations = [create_station(id=i + 1, settlement=v) for i, v in enumerate(settlements * 2)]
    pool_size = 1
    threads = []

    for i, (t_type, schedule) in enumerate(
            [
                (tp.BUS_ID,
                 [
                     [None, 0, stations[0], {'id': 11}],
                     [10, None, stations[1], {'id': 12}]
                 ]),
                (tp.SUBURBAN_ID,
                 [
                     [None, 0, stations[1], {'id': 21}],
                     [10, None, stations[2], {'id': 22}]
                 ]),
                (tp.TRAIN_ID,
                 [
                     [None, 0, stations[2], {'id': 31}],
                     [10, None, stations[3], {'id': 32}]
                 ]),
                (tp.PLANE_ID,
                 [
                     [None, 0, stations[1], {'id': 41}],
                     [10, None, stations[2], {'id': 42}]
                 ]),
            ], 1):
        threads.append(create_thread(id=i, t_type=t_type, schedule_v1=schedule))

    with mock.patch('travel.rasp.suggests_tasks.suggests.generate.ttypes.generate_parallel',
                    side_effect=generate_parallel) as m_gen_par, \
        mock.patch('travel.rasp.suggests_tasks.suggests.generate.ttypes.merge_stations_ttypes',
                   side_effect=merge_stations_ttypes) as m_merge_st, \
        mock.patch('travel.rasp.suggests_tasks.suggests.generate.ttypes.get_settlements_ttypes',
                   side_effect=get_settlements_ttypes) as m_get_sett:
        with mock_baris_response({
            'flights': [
                {'departureStation': 1, 'arrivalStation': 2, 'flightsCount': 1, 'totalFlightsCount': 1},
            ]}
        ):
            stations_t_types, settlements_t_types = get_ttypes(pool_size)

    m_gen_call_args = m_gen_par.call_args
    assert m_gen_call_args[0][0] == get_ttypes_for_ids
    assert m_gen_call_args[0][2] == pool_size
    assert set(m_gen_call_args[0][1][0]) == {11, 12, 21, 22, 31, 32}
    assert m_merge_st.call_count == 1
    m_get_sett.assert_called_once_with({1: [2, 3], 2: [2, 3, 6], 3: [1, 6], 4: [1]})

    assert shared_objects.get_obj('threads_ttypes') == {1: tp.BUS_ID, 2: tp.SUBURBAN_ID, 3: tp.TRAIN_ID}
    assert stations_t_types == {1: [2, 3], 2: [2, 3, 6], 3: [1, 6], 4: [1]}
    assert settlements_t_types == {1: [1, 2, 3, 6], 2: [1, 2, 3, 6]}


P2P_SUMMARY_BARIS_RESPONSE = {'flights': [
    {'departureStation': 211, 'arrivalStation': 212, 'flightsCount': 1, 'totalFlightsCount': 1},
    {'departureStation': 212, 'arrivalStation': 213, 'flightsCount': 3, 'dopFlightsCount': 2, 'totalFlightsCount': 4}
]}


def test_get_t_types_from_baris():
    with mock_baris_response(P2P_SUMMARY_BARIS_RESPONSE):
        baris_t_types = get_baris_station_ids()
        assert_that(baris_t_types, contains_inanyorder(211, 212, 213))
