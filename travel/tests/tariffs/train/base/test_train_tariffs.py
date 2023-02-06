# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

from decimal import Decimal

import pytest

from common.apps.train.tariff_error import TariffError
from common.apps.train_order.enums import CoachType
from common.models.currency import Price
from travel.rasp.train_api.tariffs.train.base.availability_indication import AvailabilityIndication
from travel.rasp.train_api.tariffs.train.base.coach_category_traits import CoachCategoryTraits
from travel.rasp.train_api.tariffs.train.base.models import TrainTariff, PlaceReservationType
from travel.rasp.train_api.tariffs.train.base.train_tariffs import build_train_tariffs_classes, join_train_tariffs


def _check_tariff(train_tariff, price_value, service_price_value, seats, several_prices):
    assert train_tariff.ticket_price == Price(price_value, 'RUB')
    assert train_tariff.service_price == Price(service_price_value, 'RUB')
    assert train_tariff.seats == seats
    assert train_tariff.several_prices == several_prices


class TestJoinCoachGroupTariffs(object):
    def test_different_types(self):
        tariff1 = TrainTariff(
            coach_type=CoachType.COMPARTMENT,
            ticket_price=Price(Decimal(1000), 'RUB'),
            service_price=Price(Decimal(100), 'RUB'),
            seats=10,
            several_prices=False,
        )

        tariff2 = TrainTariff(
            coach_type=CoachType.PLATZKARTE,
            ticket_price=Price(Decimal(500), 'RUB'),
            service_price=Price(Decimal(50), 'RUB'),
            seats=5,
            several_prices=False,
        )

        with pytest.raises(ValueError):
            join_train_tariffs(tariff1, tariff2)

    def test_same_prices(self):
        tariff1 = TrainTariff(
            coach_type=CoachType.COMPARTMENT,
            ticket_price=Price(Decimal(1000), 'RUB'),
            service_price=Price(Decimal(50), 'RUB'),
            seats=10,
            several_prices=False,
        )

        tariff2 = TrainTariff(
            coach_type=CoachType.COMPARTMENT,
            ticket_price=Price(Decimal(1000), 'RUB'),
            service_price=Price(Decimal(100), 'RUB'),
            seats=5,
            several_prices=False,
        )

        result = join_train_tariffs(tariff1, tariff2)
        _check_tariff(result, Decimal(1000), Decimal(100), 15, False)

    def test_same_prices_already_several_prices(self):
        tariff1 = TrainTariff(
            coach_type=CoachType.COMPARTMENT,
            ticket_price=Price(Decimal(1000), 'RUB'),
            service_price=Price(Decimal(50), 'RUB'),
            seats=10,
            several_prices=True,
        )

        tariff2 = TrainTariff(
            coach_type=CoachType.COMPARTMENT,
            ticket_price=Price(Decimal(1000), 'RUB'),
            service_price=Price(Decimal(100), 'RUB'),
            seats=5,
            several_prices=False,
        )

        result = join_train_tariffs(tariff1, tariff2)
        _check_tariff(result, Decimal(1000), Decimal(100), 15, True)

    def test_different_prices(self):
        tariff1 = TrainTariff(
            coach_type=CoachType.COMPARTMENT,
            ticket_price=Price(Decimal(500), 'RUB'),
            service_price=Price(Decimal(50), 'RUB'),
            seats=10,
            several_prices=False,
        )

        tariff2 = TrainTariff(
            coach_type=CoachType.COMPARTMENT,
            ticket_price=Price(Decimal(1000), 'RUB'),
            service_price=Price(Decimal(100), 'RUB'),
            seats=5,
            several_prices=False,
        )

        result = join_train_tariffs(tariff1, tariff2)
        _check_tariff(result, Decimal(500), Decimal(50), 15, True)


class TestTrainTariffValidations(object):
    def test_unsupported_coach_types(self):
        tariff = TrainTariff(
            coach_type=CoachType.UNKNOWN,
            ticket_price=Price(Decimal(1000), 'RUB'),
            service_price=Price(Decimal(200), 'RUB'),
            seats=10,
            max_seats_in_the_same_car=10,
            several_prices=False,
            place_reservation_type=PlaceReservationType.USUAL,
            is_transit_document_required=False,
            availability_indication=AvailabilityIndication.AVAILABLE,
        )

        is_valid, errors = tariff.validate()
        assert is_valid is False
        assert errors == {TariffError.UNSUPPORTED_COACH_TYPE.value}

    def test_soft_coach_type(self):
        tariff = TrainTariff(
            coach_type=CoachType.SOFT,
            ticket_price=Price(Decimal(1000), 'RUB'),
            service_price=Price(Decimal(200), 'RUB'),
            seats=10,
            max_seats_in_the_same_car=10,
            several_prices=False,
            place_reservation_type=PlaceReservationType.USUAL,
            is_transit_document_required=False,
            availability_indication=AvailabilityIndication.AVAILABLE,
        )

        is_valid, errors = tariff.validate()
        assert is_valid

    def test_too_cheap(self):
        for p in (-100, -10, -1, 0, 1, 5, 9):
            tariff = TrainTariff(
                coach_type=CoachType.PLATZKARTE,
                ticket_price=Price(Decimal(p), 'RUB'),
                service_price=Price(Decimal(200), 'RUB'),
                seats=10,
                max_seats_in_the_same_car=10,
                several_prices=False,
                place_reservation_type=PlaceReservationType.USUAL,
                is_transit_document_required=False,
                availability_indication=AvailabilityIndication.AVAILABLE,
            )

            is_valid, errors = tariff.validate()
            assert is_valid is False
            assert errors == {TariffError.TOO_CHEAP.value}

    def test_wrong_place_reservation_type(self):
        tariff = TrainTariff(
            coach_type=CoachType.COMPARTMENT,
            ticket_price=Price(Decimal(1000), 'RUB'),
            service_price=Price(Decimal(200), 'RUB'),
            seats=10,
            max_seats_in_the_same_car=10,
            several_prices=False,
            place_reservation_type=PlaceReservationType.UNKNOWN,
            is_transit_document_required=False,
            availability_indication=AvailabilityIndication.AVAILABLE,
        )

        is_valid, errors = tariff.validate()
        assert is_valid is False
        assert errors == {TariffError.UNSUPPORTED_RESERVATION_TYPE.value}

    def test_many_place_reservation_type(self):
        for t in (PlaceReservationType.TWO_PLACES_AT_ONCE, PlaceReservationType.FOUR_PLACES_AT_ONCE):
            tariff = TrainTariff(
                coach_type=CoachType.COMPARTMENT,
                ticket_price=Price(Decimal(1000), 'RUB'),
                service_price=Price(Decimal(200), 'RUB'),
                seats=10,
                max_seats_in_the_same_car=10,
                several_prices=False,
                place_reservation_type=t,
                is_transit_document_required=False,
                availability_indication=AvailabilityIndication.AVAILABLE,
            )

            is_valid, errors = tariff.validate()
            assert is_valid

    def test_wrong_seats_count(self):
        for s in (-100, -10, -1, 0):
            tariff = TrainTariff(
                coach_type=CoachType.COMPARTMENT,
                ticket_price=Price(Decimal(1000), 'RUB'),
                service_price=Price(Decimal(200), 'RUB'),
                seats=s,
                max_seats_in_the_same_car=10,
                several_prices=False,
                place_reservation_type=PlaceReservationType.USUAL,
                is_transit_document_required=False,
                availability_indication=AvailabilityIndication.AVAILABLE,
            )

            is_valid, errors = tariff.validate()
            assert is_valid is False
            assert errors == {TariffError.SOLD_OUT.value}

    def test_transit_document_required(self):
        tariff = TrainTariff(
            coach_type=CoachType.COMPARTMENT,
            ticket_price=Price(Decimal(1000), 'RUB'),
            service_price=Price(Decimal(200), 'RUB'),
            seats=10,
            max_seats_in_the_same_car=10,
            several_prices=False,
            place_reservation_type=PlaceReservationType.USUAL,
            is_transit_document_required=True,
            availability_indication=AvailabilityIndication.AVAILABLE,
        )

        is_valid, errors = tariff.validate()
        assert is_valid is False
        assert errors == {TariffError.TRANSIT_DOCUMENT_REQUIRED.value}

    def test_for_children(self):
        tariff = TrainTariff(
            coach_type=CoachType.COMPARTMENT,
            ticket_price=Price(Decimal(1000), 'RUB'),
            service_price=Price(Decimal(200), 'RUB'),
            seats=10,
            max_seats_in_the_same_car=10,
            several_prices=False,
            place_reservation_type=PlaceReservationType.USUAL,
            is_transit_document_required=False,
            availability_indication=AvailabilityIndication.AVAILABLE,
            category_traits=[CoachCategoryTraits(False, False, False, False, False, is_for_children=True)],
        )

        is_valid, errors = tariff.validate()
        assert is_valid is False
        assert errors == {TariffError.CHILD_TARIFF.value}

    def test_unknown_availability_indication(self):
        tariff = TrainTariff(
            coach_type=CoachType.COMPARTMENT,
            ticket_price=Price(Decimal(1000), 'RUB'),
            service_price=Price(Decimal(200), 'RUB'),
            seats=10,
            max_seats_in_the_same_car=10,
            several_prices=False,
            place_reservation_type=PlaceReservationType.USUAL,
            is_transit_document_required=False,
            availability_indication=AvailabilityIndication.UNKNOWN,
        )

        is_valid, errors = tariff.validate()
        assert is_valid is False
        assert errors == {TariffError.UNKNOWN.value}

    def test_not_available_in_web(self):
        tariff = TrainTariff(
            coach_type=CoachType.COMPARTMENT,
            ticket_price=Price(Decimal(1000), 'RUB'),
            service_price=Price(Decimal(200), 'RUB'),
            seats=10,
            max_seats_in_the_same_car=10,
            several_prices=False,
            place_reservation_type=PlaceReservationType.USUAL,
            is_transit_document_required=False,
            availability_indication=AvailabilityIndication.NOT_AVAILABLE_IN_WEB,
        )

        is_valid, errors = tariff.validate()
        assert is_valid is False
        assert errors == {TariffError.NOT_AVAILABLE_IN_WEB.value}

    def test_feature_is_not_allowed(self):
        tariff = TrainTariff(
            coach_type=CoachType.COMPARTMENT,
            ticket_price=Price(Decimal(1000), 'RUB'),
            service_price=Price(Decimal(200), 'RUB'),
            seats=10,
            max_seats_in_the_same_car=10,
            several_prices=False,
            place_reservation_type=PlaceReservationType.USUAL,
            is_transit_document_required=False,
            availability_indication=AvailabilityIndication.FEATURE_NOT_ALLOWED,
        )

        is_valid, errors = tariff.validate()
        assert is_valid is False
        assert errors == {TariffError.FEATURE_NOT_ALLOWED.value}

    def test_service_is_not_alllowed(self):
        tariff = TrainTariff(
            coach_type=CoachType.COMPARTMENT,
            ticket_price=Price(Decimal(1000), 'RUB'),
            service_price=Price(Decimal(200), 'RUB'),
            seats=10,
            max_seats_in_the_same_car=10,
            several_prices=False,
            place_reservation_type=PlaceReservationType.USUAL,
            is_transit_document_required=False,
            availability_indication=AvailabilityIndication.SERVICE_NOT_ALLOWED,
        )

        is_valid, errors = tariff.validate()
        assert is_valid is False
        assert errors == {TariffError.SERVICE_NOT_ALLOWED.value}

    def test_carrier_is_not_allowed(self):
        tariff = TrainTariff(
            coach_type=CoachType.COMPARTMENT,
            ticket_price=Price(Decimal(1000), 'RUB'),
            service_price=Price(Decimal(200), 'RUB'),
            seats=10,
            max_seats_in_the_same_car=10,
            several_prices=False,
            place_reservation_type=PlaceReservationType.USUAL,
            is_transit_document_required=False,
            availability_indication=AvailabilityIndication.CARRIER_NOT_ALLOWED_FOR_SALES,
        )

        is_valid, errors = tariff.validate()
        assert is_valid is False
        assert errors == {TariffError.CARRIER_NOT_ALLOWED_FOR_SALES.value}

    def test_other_reason(self):
        tariff = TrainTariff(
            coach_type=CoachType.COMPARTMENT,
            ticket_price=Price(Decimal(1000), 'RUB'),
            service_price=Price(Decimal(200), 'RUB'),
            seats=10,
            max_seats_in_the_same_car=10,
            several_prices=False,
            place_reservation_type=PlaceReservationType.USUAL,
            is_transit_document_required=False,
            availability_indication=AvailabilityIndication.OTHER_REASON_OF_INACCESSIBILITY,
        )

        is_valid, errors = tariff.validate()
        assert is_valid is False
        assert errors == {TariffError.OTHER_REASON_OF_INACCESSIBILITY.value}

    def test_normal_tariff(self):
        tariff = TrainTariff(
            coach_type=CoachType.COMPARTMENT,
            ticket_price=Price(Decimal(1000), 'RUB'),
            service_price=Price(Decimal(200), 'RUB'),
            seats=10,
            max_seats_in_the_same_car=10,
            several_prices=False,
            place_reservation_type=PlaceReservationType.USUAL,
            is_transit_document_required=False,
            availability_indication=AvailabilityIndication.AVAILABLE,
        )

        is_valid, errors = tariff.validate()
        assert is_valid is True
        assert errors == set()


class TestBuildTrainTariffsClasses(object):
    # 2 купейных группы вагонов
    # 2 плацкартных группы вагонов, в одной из групп слишком маленький тариф
    # 2 сидячих группы вагонов, одна из групп - купе-переговорные в Сапсане
    # 1 группа с мягкими вагонами
    # 2 группы с СВ вагонами, одна из групп - СТРИЖ, выкуп купе целиком
    compartmet_tariffs1 = TrainTariff(
        coach_type=CoachType.COMPARTMENT,
        ticket_price=Price(Decimal(1000), 'RUB'),
        service_price=Price(Decimal(200), 'RUB'),
        seats=10,
        max_seats_in_the_same_car=10,
        several_prices=False,
        place_reservation_type=PlaceReservationType.USUAL,
        is_transit_document_required=False,
        availability_indication=AvailabilityIndication.AVAILABLE,
    )

    compartmet_tariffs2 = TrainTariff(
        coach_type=CoachType.COMPARTMENT,
        ticket_price=Price(Decimal(1200), 'RUB'),
        service_price=Price(Decimal(220), 'RUB'),
        seats=12,
        max_seats_in_the_same_car=12,
        several_prices=False,
        place_reservation_type=PlaceReservationType.USUAL,
        is_transit_document_required=False,
        availability_indication=AvailabilityIndication.AVAILABLE,
    )

    platzkarte_tariffs1 = TrainTariff(
        coach_type=CoachType.PLATZKARTE,
        ticket_price=Price(Decimal(800), 'RUB'),
        service_price=Price(Decimal(100), 'RUB'),
        seats=8,
        max_seats_in_the_same_car=8,
        several_prices=False,
        place_reservation_type=PlaceReservationType.USUAL,
        is_transit_document_required=False,
        availability_indication=AvailabilityIndication.AVAILABLE,
    )

    platzkarte_tariffs2 = TrainTariff(
        coach_type=CoachType.PLATZKARTE,
        ticket_price=Price(Decimal(5), 'RUB'),
        service_price=Price(Decimal(0), 'RUB'),
        seats=100,
        max_seats_in_the_same_car=100,
        several_prices=False,
        place_reservation_type=PlaceReservationType.USUAL,
        is_transit_document_required=False,
        availability_indication=AvailabilityIndication.AVAILABLE,
    )

    sitting_tariffs1 = TrainTariff(
        coach_type=CoachType.SITTING,
        ticket_price=Price(Decimal(2000), 'RUB'),
        service_price=Price(Decimal(0), 'RUB'),
        seats=10,
        max_seats_in_the_same_car=10,
        several_prices=False,
        place_reservation_type=PlaceReservationType.FOUR_PLACES_AT_ONCE,
        is_transit_document_required=False,
        availability_indication=AvailabilityIndication.AVAILABLE,
    )

    sitting_tariffs2 = TrainTariff(
        coach_type=CoachType.SITTING,
        ticket_price=Price(Decimal(400), 'RUB'),
        service_price=Price(Decimal(0), 'RUB'),
        seats=4,
        max_seats_in_the_same_car=4,
        several_prices=False,
        place_reservation_type=PlaceReservationType.USUAL,
        is_transit_document_required=False,
        availability_indication=AvailabilityIndication.AVAILABLE,
    )

    soft_tariffs = TrainTariff(
        coach_type=CoachType.SOFT,
        ticket_price=Price(Decimal(4000), 'RUB'),
        service_price=Price(Decimal(0), 'RUB'),
        seats=4,
        max_seats_in_the_same_car=4,
        several_prices=False,
        place_reservation_type=PlaceReservationType.USUAL,
        is_transit_document_required=False,
        availability_indication=AvailabilityIndication.AVAILABLE,
    )

    suite_tariffs1 = TrainTariff(
        coach_type=CoachType.SUITE,
        ticket_price=Price(Decimal(3000), 'RUB'),
        service_price=Price(Decimal(500), 'RUB'),
        seats=10,
        max_seats_in_the_same_car=10,
        several_prices=False,
        place_reservation_type=PlaceReservationType.TWO_PLACES_AT_ONCE,
        is_transit_document_required=False,
        availability_indication=AvailabilityIndication.AVAILABLE,
    )

    suite_tariffs2 = TrainTariff(
        coach_type=CoachType.SUITE,
        ticket_price=Price(Decimal(1000), 'RUB'),
        service_price=Price(Decimal(500), 'RUB'),
        seats=50,
        max_seats_in_the_same_car=50,
        several_prices=False,
        place_reservation_type=PlaceReservationType.USUAL,
        is_transit_document_required=False,
        availability_indication=AvailabilityIndication.AVAILABLE,
    )

    def test_always_many_places_at_once(self):
        train_tariffs, broken_train_tariffs = build_train_tariffs_classes([
            self.platzkarte_tariffs1,
            self.compartmet_tariffs1,
            self.sitting_tariffs1,
            self.soft_tariffs,
            self.suite_tariffs1,
            self.platzkarte_tariffs2,
            self.compartmet_tariffs2,
            self.sitting_tariffs2,
            self.suite_tariffs2
        ])

        assert train_tariffs == {
            'platzkarte': self.platzkarte_tariffs1,
            'soft': self.soft_tariffs,
            'suite': TrainTariff(
                coach_type=CoachType.SUITE,
                ticket_price=Price(Decimal(1000), 'RUB'),
                service_price=Price(Decimal(500), 'RUB'),
                seats=60,
                max_seats_in_the_same_car=50,
                several_prices=True,
                place_reservation_type=PlaceReservationType.USUAL,
                is_transit_document_required=False,
                availability_indication=AvailabilityIndication.AVAILABLE,
            ),
            'sitting': TrainTariff(
                coach_type=CoachType.SITTING,
                ticket_price=Price(Decimal(400), 'RUB'),
                service_price=Price(Decimal(0), 'RUB'),
                seats=14,
                max_seats_in_the_same_car=10,
                several_prices=True,
                place_reservation_type=PlaceReservationType.USUAL,
                is_transit_document_required=False,
                availability_indication=AvailabilityIndication.AVAILABLE,
            ),
            'compartment': TrainTariff(
                coach_type=CoachType.COMPARTMENT,
                ticket_price=Price(Decimal(1000), 'RUB'),
                service_price=Price(Decimal(200), 'RUB'),
                seats=22,
                max_seats_in_the_same_car=12,
                several_prices=True,
                place_reservation_type=PlaceReservationType.USUAL,
                is_transit_document_required=False,
                availability_indication=AvailabilityIndication.AVAILABLE,
            ),
        }
        assert not broken_train_tariffs
