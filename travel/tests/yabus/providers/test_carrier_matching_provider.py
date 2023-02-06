# -*- coding: utf-8 -*-
from __future__ import absolute_import, division, print_function, unicode_literals

from logging import Logger

import mock

from travel.library.python.dicts.buses.carrier_matching_repository import CarrierMatchingRepository
from travel.proto.dicts.buses.carrier_matching_pb2 import TCarrierMatching

from yabus.providers.carrier_matching_provider import CarrierMatchingProvider


class TestCarrierMatchingProvider(object):
    def setup(self):
        self._logger = mock.Mock(Logger)

    def test_empty_base(self):
        provider = CarrierMatchingProvider(logger=self._logger)
        provider.setup(carrier_matching_repository=CarrierMatchingRepository())
        assert provider.get_by_key(
            supplier_id=1,
            carrier_code='ddd',
        ) is None

    def test_no_conflicts(self):
        carrier_matching_repository = CarrierMatchingRepository()
        carrier_matching_repository.add_objects([
            TCarrierMatching(
                supplier_id=1,
                code="carrier_code",
                carrier_id=42,
            ),
            TCarrierMatching(
                supplier_id=2,
                code="carrier_code",
                carrier_id=42,
            ),
            TCarrierMatching(
                supplier_id=2,
                code="other_carrier_code",
                carrier_id=42,
            ),
        ])
        provider = CarrierMatchingProvider(logger=self._logger)
        provider.setup(carrier_matching_repository=carrier_matching_repository)

        assert self._logger.error.call_count == 0

    def test_some_conflicts(self):
        carrier_matching_repository = CarrierMatchingRepository()
        carrier_matching_repository.add_objects([
            TCarrierMatching(
                id=1,
                supplier_id=1,
                code="carrier_code",
                carrier_id=42,
            ),
            TCarrierMatching(
                id=2,
                supplier_id=1,
                code="carrier_code",
                carrier_id=42,
            ),
            TCarrierMatching(
                id=3,
                supplier_id=2,
                code="other_carrier_code",
                carrier_id=42,
            ),
        ])
        provider = CarrierMatchingProvider(logger=self._logger)
        provider.setup(carrier_matching_repository=carrier_matching_repository)

        assert self._logger.warning.call_count == 1

    def test_base_case(self):
        carrier_matching_repository = CarrierMatchingRepository()
        carrier_matching_repository.add_object(TCarrierMatching(
            supplier_id=1,
            code="carrier_code",
            carrier_id=42,
        ))
        provider = CarrierMatchingProvider(logger=self._logger)
        provider.setup(carrier_matching_repository=carrier_matching_repository)

        good_cases = [
            'carrier_code',
            'carriercode',
            ' CaRrier Code ',
        ]

        for c in good_cases:
            assert provider.get_by_key(
                supplier_id=1,
                carrier_code=c,
            ) == 42, 'Can not find by code: [{}]'.format(c)

        bad_cases = [
            '',
            'Garriercode',
            '^arriercode ',
            'arriercode ',
        ]

        for c in bad_cases:
            assert provider.get_by_key(
                supplier_id=1,
                carrier_code=c,
            ) is None, 'Can find by code: [{}]'.format(c)
