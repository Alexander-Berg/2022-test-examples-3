# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from decimal import Decimal


def check_places_tariffs(actual_places, expected_places_tariffs):
    for index, (number, expected_adult_tariff, expected_child_tariff) in enumerate(expected_places_tariffs):
        _check_place_tariffs(actual_places[index], number, expected_adult_tariff, expected_child_tariff)


def _check_place_tariffs(actual_place, number, expected_adult_tariff, expected_child_tariff):
    assert actual_place.number == number
    assert actual_place.original_adult_tariff == Decimal(expected_adult_tariff)
    assert actual_place.original_child_tariff == Decimal(expected_child_tariff)
