# -*- coding: utf-8 -*-
from __future__ import absolute_import, division, print_function, unicode_literals

from travel.proto.dicts.buses.supplier_pb2 import TSupplier
from travel.library.python.dicts.buses.supplier_repository import SupplierRepository
from yabus.providers.supplier_provider import SupplierProvider


class TestSupplierProvider(object):

    def test_cases(self):
        supplier_repo = SupplierRepository()
        supplier_repo.add_objects([
            TSupplier(id=1, code="some"),
            TSupplier(id=2, code="another"),
        ])

        supplier_provider = SupplierProvider()
        supplier_provider.setup(supplier_repo=supplier_repo)

        assert supplier_provider.get_by_id(1).code == "some"
        assert supplier_provider.get_by_code("some").id == 1
        assert supplier_provider.get_by_id(id=3) is None
