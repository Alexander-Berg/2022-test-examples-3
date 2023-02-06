# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from logging import Logger
from mock import Mock
from typing import cast
from unittest import TestCase

from common.apps.train.models import CoachType, Facility
from common.apps.train.tariff_error import TariffError
from travel.rasp.wizards.train_wizard_api.lib.train_info_parser import TrainInfoParser
from travel.rasp.wizards.train_wizard_api.protobuf_models.train_price_pb2 import Price
from travel.rasp.wizards.train_wizard_api.protobuf_models.train_place_info_pb2 import TrainPlaceInfo, TrainPlace


class TestTrainInfoParser(TestCase):
    def setUp(self):
        self._logger = Mock(spec=Logger)
        self._parser = TrainInfoParser(
            logger=cast(Logger, self._logger)
        )
        self.tversk_id = 999

    def _make_coach(self, str_number, coach_type_id, company_id, facilities_ids, places, errors=None):
        return {
            "number": str_number,
            "type_id": coach_type_id,
            "company_id": company_id,
            "facilities_ids": facilities_ids,
            "places": places,
            "errors": errors or [],
        }

    def _make_place(self, facilities_ids, price_value, price_currency='RUR', group_number=1, place_number=1):
        return {
            "facilities_ids": facilities_ids,
            "group_number": group_number,
            "number": place_number,
            "price": {
                "currency": price_currency,
                "value": price_value
            }
        }

    def test_parse(self):
        def _make_coach_with_place():
            return self._make_coach(
                str_number='1',
                coach_type_id=CoachType.COMMON_ID,
                company_id=self.tversk_id,
                facilities_ids=[Facility.WIFI_ID],
                places=[
                    self._make_place([], 1000, group_number=10, place_number='100A')
                ]
            )

        train_tariff_info = self._parser.parse([
            _make_coach_with_place(),
        ])

        assert train_tariff_info == TrainPlaceInfo(places=[TrainPlace(
            coach_number=1,
            group=10,
            number='100A',
            coach_type_id=CoachType.COMMON_ID,
            company_id=self.tversk_id,
            facilities_ids=[Facility.WIFI_ID],
            price=Price(
                value=1000,
                base_value=1000
            )
        )])

    def test_parse_coach_number_with_leading_zero(self):
        def _make_coach_with_place():
            return self._make_coach(
                str_number='01',
                coach_type_id=CoachType.COMMON_ID,
                company_id=self.tversk_id,
                facilities_ids=[Facility.WIFI_ID],
                places=[
                    self._make_place([], 1000)
                ]
            )

        train_tariff_info = self._parser.parse([
            _make_coach_with_place(),
        ])

        assert train_tariff_info == TrainPlaceInfo(places=[TrainPlace(
            coach_number=1,
            group=1,
            number='1',
            coach_type_id=CoachType.COMMON_ID,
            company_id=self.tversk_id,
            facilities_ids=[Facility.WIFI_ID],
            price=Price(
                value=1000,
                base_value=1000
            )
        )])

    def test_parse_with_errors(self):
        def _make_coach_with_errors():
            return self._make_coach(
                str_number='1',
                coach_type_id=CoachType.COMMON_ID,
                company_id=self.tversk_id,
                facilities_ids=[Facility.WIFI_ID],
                places=[
                    self._make_place([], 1000, group_number=10, place_number='100A')
                ],
                errors=[TariffError.SOLD_OUT.value],
            )

        train_tariff_info = self._parser.parse([
            _make_coach_with_errors(),
        ])

        assert train_tariff_info == TrainPlaceInfo(places=[TrainPlace(
            coach_number=1,
            group=10,
            number='100A',
            coach_type_id=CoachType.COMMON_ID,
            company_id=self.tversk_id,
            facilities_ids=[Facility.WIFI_ID],
            price=Price(
                value=1000,
                base_value=1000
            ),
            coach_errors=[TariffError.SOLD_OUT.value],
        )])
