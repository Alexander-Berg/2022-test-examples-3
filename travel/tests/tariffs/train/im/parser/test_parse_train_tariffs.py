# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

import pytest

from travel.rasp.train_api.tariffs.train.base.availability_indication import AvailabilityIndication
from travel.rasp.train_api.tariffs.train.base.models import PlaceReservationType
from travel.rasp.train_api.tariffs.train.factories.im import ImCarGroupFactory
from travel.rasp.train_api.tariffs.train.im.parser import _parse_train_tariff


@pytest.mark.parametrize("car_type,expected", [
    ('Unknown', 'unknown'),
    ('Shared', 'common'),
    ('Soft', 'soft'),
    ('Luxury', 'suite'),
    ('Compartment', 'compartment'),
    ('ReservedSeat', 'platzkarte'),
    ('Sedentary', 'sitting'),
    ('Baggage', 'unknown'),
])
def test_parse_coach_type(car_type, expected):
    im_coach_group = ImCarGroupFactory(CarType=car_type)

    tariffs = _parse_train_tariff(im_coach_group)
    assert tariffs.coach_type == expected


def test_parse_ticket_price():
    im_coach_group = ImCarGroupFactory(MinPrice=2000, MaxPrice=20000)

    tariffs = _parse_train_tariff(im_coach_group)
    assert tariffs.ticket_price.value == 2000


def test_parse_service_cost():
    im_coach_group = ImCarGroupFactory(ServiceCosts=[10000, 20000])

    tariffs = _parse_train_tariff(im_coach_group)
    assert tariffs.service_price.value == 10000


def test_parse_single_price():
    im_coach_group = ImCarGroupFactory(MinPrice=1000, MaxPrice=1000)

    tariffs = _parse_train_tariff(im_coach_group)
    assert tariffs.several_prices is False


def test_parse_several_prices():
    im_coach_group = ImCarGroupFactory(MinPrice=1000, MaxPrice=1200)

    tariffs = _parse_train_tariff(im_coach_group)
    assert tariffs.several_prices is True


def test_parse_seats():
    im_coach_group = ImCarGroupFactory(TotalPlaceQuantity=999)

    tariffs = _parse_train_tariff(im_coach_group)
    assert tariffs.seats == 999
    assert tariffs.max_seats_in_the_same_car == 999


def test_parse_lower_seats():
    im_coach_group = ImCarGroupFactory(LowerPlaceQuantity=101, LowerSidePlaceQuantity=2020)

    tariffs = _parse_train_tariff(im_coach_group)
    assert tariffs.lower_seats == 101
    assert tariffs.lower_side_seats == 2020


def test_parse_upper_seats():
    im_coach_group = ImCarGroupFactory(UpperPlaceQuantity=303, UpperSidePlaceQuantity=4040)

    tariffs = _parse_train_tariff(im_coach_group)
    assert tariffs.upper_seats == 303
    assert tariffs.upper_side_seats == 4040


@pytest.mark.parametrize("is_transit_document_required,expected", [
    (True, True),
    (False, False),
])
def test_parse_is_transit_document_required(is_transit_document_required, expected):
    im_coach_group = ImCarGroupFactory(IsTransitDocumentRequired=is_transit_document_required)

    tariffs = _parse_train_tariff(im_coach_group)
    assert tariffs.is_transit_document_required is expected


@pytest.mark.parametrize("place_reservation_type,expected", [
    ('Usual', PlaceReservationType.USUAL),
    ('FourPlacesAtOnce', PlaceReservationType.FOUR_PLACES_AT_ONCE),
    ('TwoPlacesAtOnce', PlaceReservationType.TWO_PLACES_AT_ONCE),
    ('broken', PlaceReservationType.UNKNOWN),
])
def test_parse_place_reservation(place_reservation_type, expected):
    im_coach_group = ImCarGroupFactory(PlaceReservationTypes=[place_reservation_type])

    tariffs = _parse_train_tariff(im_coach_group)
    assert tariffs.place_reservation_type == expected


@pytest.mark.parametrize("availability_indication,expected", [
    ('Available', AvailabilityIndication.AVAILABLE),
    ('NotAvailableInWeb', AvailabilityIndication.NOT_AVAILABLE_IN_WEB),
    ('FeatureNotAllowed', AvailabilityIndication.FEATURE_NOT_ALLOWED),
    ('ServiceNotAllowed', AvailabilityIndication.SERVICE_NOT_ALLOWED),
    ('CarrierNotAllowedForSales', AvailabilityIndication.CARRIER_NOT_ALLOWED_FOR_SALES),
    ('OtherReasonOfInaccessibility', AvailabilityIndication.OTHER_REASON_OF_INACCESSIBILITY),
    ('broken', AvailabilityIndication.UNKNOWN),
])
def test_parse_available_availability(availability_indication, expected):
    im_coach_group = ImCarGroupFactory(AvailabilityIndication=availability_indication)

    tariffs = _parse_train_tariff(im_coach_group)
    assert tariffs.availability_indication == expected
