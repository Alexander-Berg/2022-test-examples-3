# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

import sys
import pytest
from mock import mock_open, patch

import msgpack

from travel.library.python.dicts.settlement_repository import SettlementRepository
from travel.library.python.dicts.station_repository import StationRepository
from travel.library.python.dicts.station_to_settlement_repository import StationToSettlementRepository
from travel.proto.dicts.rasp.settlement_pb2 import TSettlement
from travel.proto.dicts.rasp.station_pb2 import TStation
from travel.proto.dicts.rasp.station_to_settlement_pb2 import TStation2Settlement
from travel.rasp.bus.db.tests.factories import PointMatchingFactory
from travel.rasp.bus.scripts.pointkey_filter import PointKeyFilterGenerator


def add_pointkey_data(pointkey_data):
    for pointkey in pointkey_data:
        PointMatchingFactory(point_key=pointkey)


def exec_pointkey_filter(station_data, station_2_settlement_data=None, settlement_data=None, ttype=None):
    def station_data_load_from_file(self, _):
        for proto in station_data:
            self.add(self._PB.SerializeToString(proto))

    def station_2_settlement_load_from_file(self, _):
        for proto in station_2_settlement_data:
            self.add(self._PB.SerializeToString(proto))

    def settlement_load_from_file(self, _):
        for proto in settlement_data:
            self.add(self._PB.SerializeToString(proto))

    with patch.object(StationRepository, 'load_from_file',
                      autospec=True, side_effect=station_data_load_from_file), \
            patch.object(StationToSettlementRepository, 'load_from_file',
                         autospec=True, side_effect=station_2_settlement_load_from_file), \
            patch('travel.rasp.bus.scripts.pointkey_filter.open', mock_open()) as m_open, \
            patch.object(SettlementRepository, 'load_from_file', autospec=True, side_effect=settlement_load_from_file):

        if not ttype:
            PointKeyFilterGenerator.gen('station_fn', 'pointkey_fn', station_to_settlement_fn='station_to_settlement_fn')
        else:
            PointKeyFilterGenerator.gen('station_fn', 'pointkey_fn', settlement_fn='settlement_fn', ttype=ttype)
        output_data = m_open.return_value.write.call_args[0][0]
        if sys.version_info[0] >= 3:
            return sorted(msgpack.loads(output_data)['PointKeys'])
        else:
            return sorted(msgpack.loads(output_data, encoding='utf8')['PointKeys'])


def test_no_data(session):
    assert exec_pointkey_filter([], station_2_settlement_data=[]) == []


def test_no_expansion(session):
    add_pointkey_data(['s1'])
    assert exec_pointkey_filter([], station_2_settlement_data=[]) == ['s1']


def test_station_expansion(session):
    add_pointkey_data(['s1'])
    assert exec_pointkey_filter([
        TStation(
            Id=1,
            SettlementId=2,
        ),
    ], station_2_settlement_data=[]) == ['c2', 's1']


def test_station_2_settlement_expansion(session):
    add_pointkey_data(['s1'])
    assert exec_pointkey_filter([], station_2_settlement_data=[
        TStation2Settlement(
            StationId=1,
            SettlementId=2,
        ),
    ]) == ['c2', 's1']


def test_ttype_no_data(session):
    assert exec_pointkey_filter([], settlement_data=[], ttype='bus') == []


def test_ttype_invalid_type(session):
    with pytest.raises(ValueError):
        exec_pointkey_filter([], settlement_data=[], ttype='unknown')


def test_ttype_no_settlement(session):
    assert exec_pointkey_filter([
        TStation(
            Id=1,
            SettlementId=None,
            TransportType=3,
        ),
    ], settlement_data=[], ttype='bus') == ['s1']


def test_ttype_got_settlement(session):
    assert exec_pointkey_filter([
        TStation(
            Id=1,
            SettlementId=None,
            TransportType=3,
        ),
    ], settlement_data=[
        TSettlement(
            Id=1,
        ),
        TSettlement(
            Id=5,
        ),
    ], ttype='bus') == ['c1', 'c5', 's1']


def test_ttype_filter(session):
    assert exec_pointkey_filter([
        TStation(
            Id=1,
            SettlementId=2,
            TransportType=3,
        ),
        TStation(
            Id=2,
            SettlementId=2,
            TransportType=1,
        ),
        TStation(
            Id=3,
            SettlementId=2,
            TransportType=3,
        ),
        TStation(
            Id=4,
            SettlementId=3,
            TransportType=None,
        ),
    ], settlement_data=[], ttype='bus') == ['s1', 's3']
