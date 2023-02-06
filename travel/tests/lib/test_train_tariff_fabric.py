# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from unittest import TestCase

from common.apps.train.models import CoachType, Facility
from common.apps.train.tariff_error import TariffError
from travel.rasp.wizards.train_wizard_api.lib.train_tariff_fabric import TrainTariffFabric
from travel.rasp.wizards.train_wizard_api.protobuf_models.train_price_pb2 import Price
from travel.rasp.wizards.train_wizard_api.protobuf_models.train_place_info_pb2 import TrainPlace, TrainPlaceInfo
from travel.rasp.wizards.train_wizard_api.protobuf_models.train_tariff_info_pb2 import TrainTariff, TrainTariffInfo


class TestTrainTariffFabric(TestCase):
    def setUp(self):
        self._fabric = TrainTariffFabric()

    def _make_place(self, coach_type_id=CoachType.COMMON_ID, coach_number=1, group=2, number='3', company_id=1,
                    facilities_ids=None, price_value=1000, coach_errors=None):
        if facilities_ids is None:
            facilities_ids = []

        return TrainPlace(
            coach_number=coach_number,
            group=group,
            number=number,
            company_id=company_id,
            coach_type_id=coach_type_id,
            facilities_ids=facilities_ids,
            price=Price(
                value=price_value,
                base_value=price_value
            ),
            coach_errors=coach_errors or [],
        )

    def test_the_same_tariffs(self):
        place_info = TrainPlaceInfo(places=[
            self._make_place(),
            self._make_place(),
        ])
        train_tariff_info = self._fabric.make_tariffs_info(
            place_info
        )

        assert train_tariff_info == TrainTariffInfo(tariffs=[TrainTariff(
            coach_number=1,
            company_id=1,
            coach_type_id=CoachType.COMMON_ID,
            price=Price(
                value=1000,
                base_value=1000
            ),
            seats=2
        )])

    def test_different_numbers(self):
        place_info = TrainPlaceInfo(places=[
            self._make_place(
                coach_number=1
            ),
            self._make_place(
                coach_number=10
            ),
        ])
        train_tariff_info = self._fabric.make_tariffs_info(
            place_info
        )

        def _make_train_tariff(number):
            return TrainTariff(
                coach_number=number,
                coach_type_id=CoachType.COMMON_ID,
                company_id=1,
                price=Price(
                    value=1000,
                    base_value=1000
                ),
                seats=1
            )

        assert train_tariff_info == TrainTariffInfo(
            tariffs=[
                _make_train_tariff(1),
                _make_train_tariff(10)
            ]
        )

    def test_different_coach_type_id(self):
        place_info = TrainPlaceInfo(places=[
            self._make_place(
                coach_type_id=CoachType.COMMON_ID
            ),
            self._make_place(
                coach_type_id=CoachType.SUITE_ID
            ),
        ])

        train_tariff_info = self._fabric.make_tariffs_info(
            place_info
        )

        def _make_train_tariff(coach_type_id):
            return TrainTariff(
                coach_number=1,
                coach_type_id=coach_type_id,
                company_id=1,
                price=Price(
                    value=1000,
                    base_value=1000
                ),
                seats=1
            )

        assert train_tariff_info == TrainTariffInfo(
            tariffs=[
                _make_train_tariff(CoachType.COMMON_ID),
                _make_train_tariff(CoachType.SUITE_ID),
            ]
        )

    def test_different_facilities_ids_type(self):
        place_info = TrainPlaceInfo(places=[
            self._make_place(
                facilities_ids=[Facility.WIFI_ID]
            ),
            self._make_place(
                facilities_ids=[Facility.EATING_ID]
            ),
        ])

        train_tariff_info = self._fabric.make_tariffs_info(
            place_info
        )

        def _make_train_tariff(coach_facility):
            return TrainTariff(
                coach_number=1,
                coach_type_id=CoachType.COMMON_ID,
                company_id=1,
                facilities_ids=[coach_facility],
                price=Price(
                    value=1000,
                    base_value=1000
                ),
                seats=1
            )

        assert train_tariff_info == TrainTariffInfo(
            tariffs=[
                _make_train_tariff(Facility.EATING_ID),
                _make_train_tariff(Facility.WIFI_ID),
            ]
        )

    def test_different_company_id(self):
        lg_id = 666
        tversk_id = 999

        place_info = TrainPlaceInfo(places=[
            self._make_place(
                company_id=lg_id
            ),
            self._make_place(
                company_id=tversk_id
            ),
        ])

        train_tariff_info = self._fabric.make_tariffs_info(
            place_info
        )

        def _make_train_tariff(coach_owner_value):
            return TrainTariff(
                coach_number=1,
                coach_type_id=CoachType.COMMON_ID,
                company_id=coach_owner_value,
                price=Price(
                    value=1000,
                    base_value=1000
                ),
                seats=1
            )

        assert train_tariff_info == TrainTariffInfo(
            tariffs=[
                _make_train_tariff(lg_id),
                _make_train_tariff(tversk_id),
            ]
        )

    def test_different_price(self):
        place_info = TrainPlaceInfo(places=[
            self._make_place(
                price_value=1000
            ),
            self._make_place(
                price_value=10000
            ),
        ])

        train_tariff_info = self._fabric.make_tariffs_info(
            place_info
        )

        def _make_train_tariff(price):
            return TrainTariff(
                coach_number=1,
                coach_type_id=CoachType.COMMON_ID,
                company_id=1,
                price=Price(
                    value=price,
                    base_value=price
                ),
                seats=1
            )

        assert train_tariff_info == TrainTariffInfo(
            tariffs=[
                _make_train_tariff(1000),
                _make_train_tariff(10000),
            ]
        )

    def test_tariffs_with_errors(self):
        place_info = TrainPlaceInfo(places=[
            self._make_place(coach_errors=[TariffError.SOLD_OUT.value]),
            self._make_place(number='5', coach_errors=[TariffError.SOLD_OUT.value]),
        ])
        train_tariff_info = self._fabric.make_tariffs_info(
            place_info
        )

        assert train_tariff_info == TrainTariffInfo(tariffs=[TrainTariff(
            coach_number=1,
            company_id=1,
            coach_type_id=CoachType.COMMON_ID,
            price=Price(
                value=1000,
                base_value=1000
            ),
            seats=2,
            coach_errors=[TariffError.SOLD_OUT.value],
        )])
