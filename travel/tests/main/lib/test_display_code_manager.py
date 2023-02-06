from __future__ import absolute_import

from mock import Mock

from travel.avia.library.python.tester.testcase import TestCase

from travel.avia.backend.main.lib.display_code_manager import DisplayCodeManager
from travel.avia.backend.tests.main.fabriks.airport import create_airport_model
from travel.avia.backend.tests.main.fabriks.settlement import create_settlement_model
from travel.avia.backend.tests.main.fakes.airport_repository import FakeAirportRepository
from travel.avia.backend.tests.main.fakes.geo_relation_repository import FakeGeoRelationsRepository
from travel.avia.backend.tests.main.fakes.settlement_repository import FakeSettlementRepository


class DisplayCodeManagerTest(TestCase):
    def setUp(self):
        self.settlement_with_iata = create_settlement_model(
            pk=1,
            iata='settlement_with_iata_code',
        )

        self.settlement_with_sirena = create_settlement_model(
            pk=2,
            sirena='settlement_with_sirena_code',
        )

        self.settlement_with_iata_and_sirena = create_settlement_model(
            pk=3,
            iata='settlement_with_iata_and_sirena_iata_code',
            sirena='settlement_with_iata_and_sirena_sirena_code',
        )

        self.settlement_without_iata_and_sirena = create_settlement_model(
            pk=4
        )

        self.settlement_with_airport_iata = create_settlement_model(
            pk=5,
            sirena='will_ignored'
        )
        self.airport_with_iata = create_airport_model(
            pk=1,
            settlement_id=self.settlement_with_airport_iata.pk,
            iata='airport_with_iata_code',
        )

        self.settlement_with_airport_sirena = create_settlement_model(
            pk=6
        )
        self.airport_with_sirena = create_airport_model(
            pk=2,
            settlement_id=self.settlement_with_airport_sirena.pk,
            sirena='airport_with_sirena_code'
        )

        self.settlement_with_airport_codes = create_settlement_model(
            pk=7,
            sirena='will_ignored'
        )
        self.airport_with_codes = create_airport_model(
            pk=3,
            settlement_id=self.settlement_with_airport_codes.pk,
            iata='airport_with_codes_iata',
            sirena='airport_with_codes_sirena',
        )

        self._fake_geo_relation_repository = Mock()
        self._fake_geo_relation_repository.get_airport_ids_for = Mock(
            return_value=[]
        )

        self._manager = DisplayCodeManager(
            settlement_repository=FakeSettlementRepository(
                [
                    self.settlement_with_iata,
                    self.settlement_with_sirena,
                    self.settlement_with_iata_and_sirena,
                    self.settlement_without_iata_and_sirena,
                    self.settlement_with_airport_iata,
                    self.settlement_with_airport_sirena,
                    self.settlement_with_airport_codes
                ]
            ),
            station_repository=FakeAirportRepository(
                [
                    self.airport_with_iata,
                    self.airport_with_sirena,
                    self.airport_with_codes
                ]
            ),
            geo_relation_repository=FakeGeoRelationsRepository(
                {
                    self.settlement_with_airport_iata.pk: [
                        self.airport_with_iata.pk
                    ],
                    self.settlement_with_airport_sirena.pk: [
                        self.airport_with_sirena.pk
                    ],
                    self.settlement_with_airport_codes.pk: [
                        self.airport_with_codes.pk
                    ]
                }
            ),
        )

    def test_iata(self):
        test_cases = [
            (self.settlement_with_iata, 'settlement_with_iata_code'),
            (self.settlement_with_sirena, None),
            (
                self.settlement_with_iata_and_sirena,
                'settlement_with_iata_and_sirena_iata_code'
            ),
            (self.settlement_without_iata_and_sirena, None),
            (self.settlement_with_airport_iata, 'airport_with_iata_code'),
            (self.settlement_with_airport_sirena, None),
            (self.settlement_with_airport_codes, 'airport_with_codes_iata')
        ]

        self._manager.pre_cache()

        for s, answer in test_cases:
            assert self._manager.get_iata_for_settlement(
                settlement_id=s.pk
            ) == answer

    def test_codes(self):
        test_cases = [
            (self.settlement_with_iata, 'settlement_with_iata_code'),
            (self.settlement_with_sirena, 'settlement_with_sirena_code'),
            (
                self.settlement_with_iata_and_sirena,
                'settlement_with_iata_and_sirena_iata_code'
            ),
            (self.settlement_without_iata_and_sirena, None),
            (self.settlement_with_airport_iata, 'airport_with_iata_code'),
            (self.settlement_with_airport_sirena, 'airport_with_sirena_code'),
            (self.settlement_with_airport_codes, 'airport_with_codes_iata')
        ]

        self._manager.pre_cache()

        for s, answer in test_cases:
            assert self._manager.get_code_for_settlement(
                settlement_id=s.pk
            ) == answer

    def test_airport_code(self):
        test_cases = [
            (self.airport_with_iata, 'airport_with_iata_code'),
            (self.airport_with_sirena, 'airport_with_sirena_code'),
            (self.airport_with_codes, 'airport_with_codes_iata')
        ]

        self._manager.pre_cache()

        for s, answer in test_cases:
            assert self._manager.get_code_for_airport(
                airport_id=s.pk
            ) == answer
