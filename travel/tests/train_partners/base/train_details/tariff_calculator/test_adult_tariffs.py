# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

from decimal import Decimal

from hamcrest import assert_that, contains, has_properties

from common.apps.train.models import PlacePriceRules
from travel.rasp.train_api.train_partners.base.test_utils import CoachDetailsStub
from travel.rasp.train_api.train_partners.base.train_details.parsers import PlaceDetails
from travel.rasp.train_api.train_partners.base.train_details.tariff_calculator.adult_tariffs import ensure_adult_tariffs


PLATZKARTE_RULES = [
    PlacePriceRules(place_numbers='1, 3', price_percent=Decimal('100')),
    PlacePriceRules(place_numbers='2, 4', price_percent=Decimal('80')),
    PlacePriceRules(place_numbers='5 - 8', price_percent=Decimal('60')),
]


def test_adult_tariffs_already_set():
    tariff1 = Decimal('1000.80')
    tariff2 = Decimal('800.45')
    place1 = PlaceDetails(number=1, original_adult_tariff=tariff1)
    place2 = PlaceDetails(number=2, original_adult_tariff=tariff2)
    coach = CoachDetailsStub(places=[place1, place2])

    ensure_adult_tariffs(coach)
    assert_that(coach.places, contains(
        has_properties(number=1, original_adult_tariff=tariff1),
        has_properties(number=2, original_adult_tariff=tariff2)
    ))


def test_no_rules():
    max_tariff = Decimal('1000.80')
    tariff2 = Decimal('800.45')
    tariff3 = Decimal('1200.70')
    place1 = PlaceDetails(number=1)
    place2 = PlaceDetails(number=2, original_adult_tariff=tariff2)
    place3 = PlaceDetails(number=3, original_adult_tariff=tariff3)
    coach = CoachDetailsStub(places=[place1, place2, place3], max_tariff=max_tariff)

    ensure_adult_tariffs(coach)
    assert_that(coach.places, contains(
        has_properties(number=1, original_adult_tariff=max_tariff),
        has_properties(number=2, original_adult_tariff=tariff2),
        has_properties(number=3, original_adult_tariff=tariff3)
    ))


def test_two_rules():
    """
    Тест описывает купейные вагоны. Места двух категорий: дешевые и дорогие.
    В случае двух тарифных правил не учитываются price_percent этих правил. То есть скидку вручную не вычисляем.
    """
    min_tariff = Decimal('800')
    max_tariff = Decimal('1000')
    base_tariff = max_tariff

    service_tariff = Decimal('100')
    rules = [
        PlacePriceRules(place_numbers='1, 3, 5', price_percent=Decimal('100')),
        PlacePriceRules(place_numbers='2, 4, 6', price_percent=Decimal('50'))
    ]
    tariff5 = Decimal('1200')
    place1 = PlaceDetails(number=1)
    place2 = PlaceDetails(number=2)
    place3 = PlaceDetails(number=3)
    place4 = PlaceDetails(number=4)
    place5 = PlaceDetails(number=5, original_adult_tariff=tariff5)
    coach = CoachDetailsStub(places=[place1, place2, place3, place4, place5],
                             price_rules=rules,
                             min_tariff=min_tariff, max_tariff=max_tariff,
                             base_tariff=base_tariff, service_tariff=service_tariff)

    ensure_adult_tariffs(coach)
    assert_that(coach.places, contains(
        has_properties(number=1, original_adult_tariff=max_tariff),
        has_properties(number=2, original_adult_tariff=min_tariff),
        has_properties(number=3, original_adult_tariff=max_tariff),
        has_properties(number=4, original_adult_tariff=min_tariff),
        has_properties(number=5, original_adult_tariff=tariff5)
    ))


def test_three_rules_all_prices_exist():
    """
    Тест описывает плацкартные вагоны. Места трех категорий: дешевые, средние и дорогие. Максимальный тариф - 100%.
    В случае трех тарифных правил учитываются price_percent только среднего правила, для вычисления среднего тарифа.
    """
    min_tariff = Decimal('701')
    max_tariff = Decimal('1100')
    base_tariff = max_tariff
    middle_tariff = Decimal('900')
    service_tariff = Decimal('100')
    tariff6 = Decimal('1200')
    place1 = PlaceDetails(number=1)
    place2 = PlaceDetails(number=2)
    place3 = PlaceDetails(number=3)
    place4 = PlaceDetails(number=4)
    place5 = PlaceDetails(number=5)
    place6 = PlaceDetails(number=6, original_adult_tariff=tariff6)
    coach = CoachDetailsStub(places=[place1, place2, place3, place4, place5, place6],
                             price_rules=PLATZKARTE_RULES,
                             min_tariff=min_tariff, max_tariff=max_tariff,
                             base_tariff=base_tariff, service_tariff=service_tariff)

    ensure_adult_tariffs(coach)
    assert_that(coach.places, contains(
        has_properties(number=1, original_adult_tariff=max_tariff),
        has_properties(number=2, original_adult_tariff=middle_tariff),
        has_properties(number=3, original_adult_tariff=max_tariff),
        has_properties(number=4, original_adult_tariff=middle_tariff),
        has_properties(number=5, original_adult_tariff=min_tariff),
        has_properties(number=6, original_adult_tariff=tariff6)
    ))


def test_three_rules_high_and_middle_exist():
    """
    Тест описывает плацкартные вагоны. Доступны места двух категорий: средние и дорогие; дешевые уже раскупили.
    Максимальный тариф - 100%.
    Скидки вручную не вычисляем.
    """
    min_tariff = Decimal('901')
    max_tariff = Decimal('1099')
    base_tariff = max_tariff
    service_tariff = Decimal('100')
    tariff4 = Decimal('1200')
    place1 = PlaceDetails(number=1)
    place2 = PlaceDetails(number=2)
    place3 = PlaceDetails(number=3)
    place4 = PlaceDetails(number=4, original_adult_tariff=tariff4)
    coach = CoachDetailsStub(places=[place1, place2, place3, place4],
                             price_rules=PLATZKARTE_RULES,
                             min_tariff=min_tariff, max_tariff=max_tariff,
                             base_tariff=base_tariff, service_tariff=service_tariff)

    ensure_adult_tariffs(coach)
    assert_that(coach.places, contains(
        has_properties(number=1, original_adult_tariff=max_tariff),
        has_properties(number=2, original_adult_tariff=min_tariff),
        has_properties(number=3, original_adult_tariff=max_tariff),
        has_properties(number=4, original_adult_tariff=tariff4)
    ))


def test_three_rules_only_middle_exists():
    """
    Тест описывает плацкартные вагоны. Доступны места одной категорий: средние; дешевые и дорогие уже раскупили.
    Максимальный тариф - 100%.
    Скидки вручную не вычисляем.
    """
    min_tariff = Decimal('900')
    max_tariff = Decimal('900')
    base_tariff = Decimal('1100')
    service_tariff = Decimal('100')
    tariff4 = Decimal('1200')
    place2 = PlaceDetails(number=2)
    place4 = PlaceDetails(number=4, original_adult_tariff=tariff4)
    coach = CoachDetailsStub(places=[place2, place4],
                             price_rules=PLATZKARTE_RULES,
                             min_tariff=min_tariff, max_tariff=max_tariff,
                             base_tariff=base_tariff, service_tariff=service_tariff)

    ensure_adult_tariffs(coach)
    assert_that(coach.places, contains(
        has_properties(number=2, original_adult_tariff=max_tariff),
        has_properties(number=4, original_adult_tariff=tariff4)
    ))


def test_three_rules_all_prices_exist_max_120():
    """
    Тест описывает Ласточки. Места трех категорий: дешевые, средние и дорогие. Максимальный тариф - 120%.
    В случае трех тарифных правил учитываются price_percent только среднего правила, для вычисления среднего тарифа.
    """
    min_tariff = Decimal('701')
    max_tariff = Decimal('1300')
    base_tariff = Decimal('1100')
    middle_tariff = Decimal('1100')
    service_tariff = Decimal('100')
    rules = [
        PlacePriceRules(place_numbers='1, 2', price_percent=Decimal('120')),
        PlacePriceRules(place_numbers='3, 4', price_percent=Decimal('100')),
        PlacePriceRules(place_numbers='5, 6', price_percent=Decimal('80')),
    ]
    tariff6 = Decimal('1400')
    place1 = PlaceDetails(number=1)
    place2 = PlaceDetails(number=2)
    place3 = PlaceDetails(number=3)
    place4 = PlaceDetails(number=4)
    place5 = PlaceDetails(number=5)
    place6 = PlaceDetails(number=6, original_adult_tariff=tariff6)
    coach = CoachDetailsStub(places=[place1, place2, place3, place4, place5, place6],
                             price_rules=rules,
                             min_tariff=min_tariff, max_tariff=max_tariff,
                             base_tariff=base_tariff, service_tariff=service_tariff)

    ensure_adult_tariffs(coach)
    assert_that(coach.places, contains(
        has_properties(number=1, original_adult_tariff=max_tariff),
        has_properties(number=2, original_adult_tariff=max_tariff),
        has_properties(number=3, original_adult_tariff=middle_tariff),
        has_properties(number=4, original_adult_tariff=middle_tariff),
        has_properties(number=5, original_adult_tariff=min_tariff),
        has_properties(number=6, original_adult_tariff=tariff6)
    ))
