# -*- coding: utf-8 -*-
from __future__ import absolute_import, division, print_function, unicode_literals

from travel.proto.dicts.buses.carrier_pb2 import TCarrier
from travel.proto.dicts.buses.register_type_pb2 import TRegisterType
from travel.library.python.dicts.buses.carrier_repository import CarrierRepository
from travel.library.python.dicts.buses.register_type_repository import RegisterTypeRepository

from yabus.providers.register_type_provider import RegisterTypeProvider


class TestRegisterTypeProvider(object):

    def test_cases(self):
        carrier_repo = CarrierRepository()
        carrier_repo.add_objects([
            TCarrier(id=1, name="some", register_type_id=1),
            TCarrier(id=2, name="another", register_type_id=1),
        ])

        register_type_repo = RegisterTypeRepository()
        register_type_repo.add_object(TRegisterType(id=1, code="OGRNIP"))

        register_type_provider = RegisterTypeProvider()
        register_type_provider.setup(register_type_repo=register_type_repo)

        assert register_type_provider.get_by_id(id=1).code == "OGRNIP"
