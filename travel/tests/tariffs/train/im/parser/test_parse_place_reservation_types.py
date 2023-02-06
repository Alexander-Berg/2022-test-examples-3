# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

from decimal import Decimal

from travel.rasp.train_api.tariffs.train.base.models import PlaceReservationType
from travel.rasp.train_api.tariffs.train.im.parser import parse_place_reservation_types


def test_absent_type():
    im_coach_group = {
        'CarType': 'Compartment',
        'MinPrice': Decimal(1000),
        'MaxPrice': Decimal(1500),
        'ServiceCosts': [0],
        'TotalPlaceQuantity': 10,
        'PlaceReservationTypes': ['Usual']
    }
    assert parse_place_reservation_types(im_coach_group) == PlaceReservationType.USUAL


def test_unknown_type():
    im_coach_group = {
        'CarType': 'Compartment',
        'MinPrice': Decimal(1000),
        'MaxPrice': Decimal(1500),
        'ServiceCosts': [0],
        'TotalPlaceQuantity': 10,
        'PlaceReservationTypes': ['Unknown']
    }
    assert parse_place_reservation_types(im_coach_group) == PlaceReservationType.UNKNOWN


def test_two_places():
    im_coach_group = {
        'CarType': 'Compartment',
        'MinPrice': Decimal(1000),
        'MaxPrice': Decimal(1500),
        'ServiceCosts': [0],
        'TotalPlaceQuantity': 10,
        'PlaceReservationTypes': ['TwoPlacesAtOnce']
    }
    assert parse_place_reservation_types(im_coach_group) == PlaceReservationType.TWO_PLACES_AT_ONCE


def test_four_places():
    im_coach_group = {
        'CarType': 'Compartment',
        'MinPrice': Decimal(1000),
        'MaxPrice': Decimal(1500),
        'ServiceCosts': [0],
        'TotalPlaceQuantity': 10,
        'PlaceReservationTypes': ['FourPlacesAtOnce']
    }
    assert parse_place_reservation_types(im_coach_group) == PlaceReservationType.FOUR_PLACES_AT_ONCE
