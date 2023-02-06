# -*- coding: utf-8 -*-
from __future__ import absolute_import, division, print_function, unicode_literals

from travel.proto.dicts.buses.carrier_pb2 import TCarrier
from travel.library.python.dicts.buses.carrier_repository import CarrierRepository

from yabus.providers.carrier_provider import CarrierProvider


class TestCarrierProvider(object):

    def test_cases(self):
        carrier_repo = CarrierRepository()
        carrier_repo.add_objects([
            TCarrier(id=1, name="some", register_type_id=1),
            TCarrier(id=2, name="another", register_type_id=1),
        ])

        carrier_provider = CarrierProvider()
        carrier_provider.setup(carrier_repo=carrier_repo)

        assert carrier_provider.get_by_id(id=1).name == "some"
        assert carrier_provider.get_by_id(id=2).name == "another"
        assert carrier_provider.get_by_id(id=3) is None
