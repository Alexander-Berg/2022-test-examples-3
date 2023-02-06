# -*- coding: utf-8 -*-
from __future__ import absolute_import, division, print_function, unicode_literals

from logging import Logger

import mock

from travel.proto.dicts.buses.carrier_pb2 import TCarrier
from travel.proto.dicts.buses.supplier_pb2 import TSupplier

from yabus.carrier_matcher import CarrierMatcher
from yabus.providers.carrier_matching_provider import CarrierMatchingProvider
from yabus.providers.carrier_provider import CarrierProvider
from yabus.providers.supplier_provider import SupplierProvider


class TestCarrierMatchingProvider(object):
    def setup_method(self, _):
        self._logger = mock.Mock(Logger)

        self._carrier_matching_provider = mock.Mock(CarrierMatchingProvider)
        self._carrier_provider = mock.Mock(CarrierProvider)
        self._supplier_provider = mock.Mock(SupplierProvider)

        self._matcher = CarrierMatcher(
            carrier_matching_provider=self._carrier_matching_provider,
            carrier_provider=self._carrier_provider,
            supplier_provider=self._supplier_provider,
            logger=self._logger,
        )

    def test_can_not_find_supplier_by_code(self):
        supplier_code = 'some_supplier_code'
        self._supplier_provider.get_by_code = mock.Mock(return_value=None)
        assert self._matcher.get_carrier(supplier_code, 'carrier_code') is None
        self._supplier_provider.get_by_code.assert_called_with(supplier_code)
        self._logger.error.assert_called_with('Unknown supplier %s', supplier_code)

    def test_can_not_find_carrier_by_matching_rule(self):
        supplier_code = 'some_supplier_code'
        supplier_id = 42
        carrier_code = 'some_carrier_code'

        self._supplier_provider.get_by_code = mock.Mock(return_value=TSupplier(id=supplier_id))
        self._carrier_matching_provider.get_by_key = mock.Mock(return_value=None)
        assert self._matcher.get_carrier(supplier_code, carrier_code) is None
        self._supplier_provider.get_by_code.assert_called_with(supplier_code)
        self._carrier_matching_provider.get_by_key.assert_called_with(
            supplier_id=supplier_id,
            carrier_code=carrier_code,
        )
        self._logger.debug.assert_called_with(
            'Can not find carrier by matching rule (%d/%s)',
            supplier_id, carrier_code
        )

    def test_can_not_find_carrier_by_id(self):
        supplier_code = 'some_supplier_code'
        supplier_id = 42
        carrier_code = 'some_carrier_code'
        carrier_id = 42

        self._supplier_provider.get_by_code = mock.Mock(return_value=TSupplier(id=supplier_id))
        self._carrier_matching_provider.get_by_key = mock.Mock(return_value=carrier_id)
        self._carrier_provider.get_by_id = mock.Mock(return_value=None)

        assert self._matcher.get_carrier(supplier_code, carrier_code) is None

        self._supplier_provider.get_by_code.assert_called_with(supplier_code)
        self._carrier_matching_provider.get_by_key.assert_called_with(
            supplier_id=supplier_id,
            carrier_code=carrier_code,
        )
        self._carrier_provider.get_by_id.assert_called_with(
            carrier_id
        )
        self._logger.debug.assert_called_with('Can not find carrier by carrier_id %d', carrier_id)

    def test_all_is_right(self):
        supplier_code = 'some_supplier_code'
        supplier_id = 42
        carrier_code = 'some_carrier_code'
        carrier_id = 42
        carrier_model = TCarrier(id=42)

        self._supplier_provider.get_by_code = mock.Mock(return_value=TSupplier(id=supplier_id))
        self._carrier_matching_provider.get_by_key = mock.Mock(return_value=carrier_id)
        self._carrier_provider.get_by_id = mock.Mock(return_value=carrier_model)

        assert self._matcher.get_carrier(supplier_code, carrier_code) == carrier_model

        self._supplier_provider.get_by_code.assert_called_with(supplier_code)
        self._carrier_matching_provider.get_by_key.assert_called_with(
            supplier_id=supplier_id,
            carrier_code=carrier_code,
        )
        self._carrier_provider.get_by_id.assert_called_with(
            carrier_id
        )
        assert self._logger.debug.call_count == 0
