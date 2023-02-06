# -*- coding: utf-8 -*-
from __future__ import absolute_import, division, print_function, unicode_literals

import pytest

from travel.proto.dicts.rasp.station_pb2 import TStation
from travel.proto.dicts.rasp.station_to_settlement_pb2 import TStation2Settlement
from travel.library.python.dicts.station_repository import StationRepository
from travel.library.python.dicts.station_to_settlement_repository import StationToSettlementRepository

from util.lazy_setuper import WasNotSetupError
from yabus.providers.point_relations_provider import PointRelationsProvider


class TestPointPointRelationsProvider(object):

    @staticmethod
    def make_point_relations_provider():
        station_repo = StationRepository()
        station_repo.add_object(TStation(Id=1))
        station_repo.add_object(TStation(Id=2, SettlementId=10))
        station_repo.add_object(TStation(Id=3, SettlementId=10))

        settlement2station_repo = StationToSettlementRepository()
        settlement2station_repo.add_object(TStation2Settlement(StationId=3, SettlementId=20))
        settlement2station_repo.add_object(TStation2Settlement(StationId=4, SettlementId=30))
        settlement2station_repo.add_object(TStation2Settlement(StationId=5, SettlementId=30))

        point_relations_provider = PointRelationsProvider()
        point_relations_provider.setup(station_repo=station_repo, station2settlement_repo=settlement2station_repo)
        return point_relations_provider

    def test_not_setuped(self):
        provider = PointRelationsProvider()

        with pytest.raises(WasNotSetupError):
            provider.get_children('c213')

        with pytest.raises(WasNotSetupError):
            provider.get_parents('s123')

    def test_setuped(self):
        provider = self.make_point_relations_provider()

        assert (
            tuple(provider.get_children('c213')) ==
            tuple(provider.get_children('s1')) ==
            tuple(provider.get_children('s2')) ==
            tuple(provider.get_children('s3')) ==
            tuple(provider.get_children('s4')) == ()
        )
        assert tuple(provider.get_children('c10')) == ('s2', 's3',)
        assert tuple(provider.get_children('c20')) == ('s3',)
        assert tuple(provider.get_children('c30')) == ('s4', 's5',)

        assert (
            tuple(provider.get_parents('s123')) ==
            tuple(provider.get_parents('c10')) ==
            tuple(provider.get_parents('c20')) ==
            tuple(provider.get_parents('c30')) == ()
        )
        assert tuple(provider.get_parents('s2')) == ('c10',)
        assert tuple(provider.get_parents('s3')) == ('c10', 'c20',)
        assert tuple(provider.get_parents('s4')) == tuple(provider.get_parents('s5')) == ('c30',)
