# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

from decimal import Decimal

from hamcrest import assert_that, contains, has_properties

from common.apps.train.models import PlacePriceRules
from travel.rasp.train_api.train_partners.base.test_utils import CoachDetailsStub
from travel.rasp.train_api.train_partners.base.train_details.parsers import PlaceDetails
from travel.rasp.train_api.train_partners.base.train_details.tariff_calculator.child_tariffs import set_child_tariffs


def test_no_rules():
    service_tariff = Decimal(100)
    adult_tariff1 = Decimal(Decimal(1100))
    adult_tariff2 = Decimal(Decimal(900))
    base_tariff = Decimal(1100)
    coach = CoachDetailsStub(places=[PlaceDetails(number=1, original_adult_tariff=adult_tariff1),
                                     PlaceDetails(number=2, original_adult_tariff=adult_tariff2)],
                             service_tariff=service_tariff,
                             base_tariff=base_tariff,
                             price_rules=[])

    set_child_tariffs(coach)

    assert_that(coach.places, contains(
        has_properties(number=1, original_adult_tariff=adult_tariff1, original_child_tariff=Decimal(450)),
        has_properties(number=2, original_adult_tariff=adult_tariff2, original_child_tariff=Decimal(380))
    ))


def test_rule_can_apply_for_child():
    service_tariff = Decimal(100)
    adult_tariff1 = Decimal(Decimal(1100))
    adult_tariff2 = Decimal(Decimal(900))
    base_tariff = Decimal(1100)
    coach = CoachDetailsStub(
        places=[PlaceDetails(number=1, original_adult_tariff=adult_tariff1),
                PlaceDetails(number=2, original_adult_tariff=adult_tariff2)],
        service_tariff=service_tariff,
        base_tariff=base_tariff,
        price_rules=[PlacePriceRules(price_percent=Decimal(100), place_numbers='1, 3', can_apply_for_child=True),
                     PlacePriceRules(price_percent=Decimal(80), place_numbers='2, 4', can_apply_for_child=True)])

    set_child_tariffs(coach)

    assert_that(coach.places, contains(
        has_properties(number=1, original_adult_tariff=adult_tariff1, original_child_tariff=Decimal(450)),
        has_properties(number=2, original_adult_tariff=adult_tariff2, original_child_tariff=Decimal(380))
    ))


def test_rule_cannot_apply_for_child():
    service_tariff = Decimal(100)
    adult_tariff1 = Decimal(Decimal(1100))
    adult_tariff2 = Decimal(Decimal(900))
    base_tariff = Decimal(1100)
    coach = CoachDetailsStub(
        places=[PlaceDetails(number=1, original_adult_tariff=adult_tariff1),
                PlaceDetails(number=2, original_adult_tariff=adult_tariff2)],
        service_tariff=service_tariff,
        base_tariff=base_tariff,
        price_rules=[PlacePriceRules(price_percent=Decimal(100), place_numbers='1, 3', can_apply_for_child=False),
                     PlacePriceRules(price_percent=Decimal(80), place_numbers='2, 4', can_apply_for_child=False)])

    set_child_tariffs(coach)

    assert_that(coach.places, contains(
        has_properties(number=1, original_adult_tariff=adult_tariff1, original_child_tariff=Decimal(450)),
        has_properties(number=2, original_adult_tariff=adult_tariff2, original_child_tariff=Decimal(450))
    ))
