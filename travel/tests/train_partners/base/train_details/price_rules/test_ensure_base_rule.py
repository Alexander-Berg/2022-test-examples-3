# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

from hamcrest import assert_that, contains, has_properties

from common.apps.train.models import PlacePriceRules
from travel.rasp.train_api.train_partners.base.train_details.price_rules import ensure_base_rule


def test_ensure_base_rule():
    rule1 = PlacePriceRules(coach_type='platzkarte',
                            place_numbers='1, 2',
                            price_percent=120,
                            can_apply_for_child=True)

    rule2 = PlacePriceRules(coach_type='platzkarte',
                            place_numbers='3, 4',
                            price_percent=80,
                            can_apply_for_child=True)

    rules = ensure_base_rule([rule1, rule2], 'platzkarte', {1, 2, 3, 4, 5, 6})

    assert_that(rules, contains(
        has_properties(
            place_numbers_set={1, 2},
            price_percent=120
        ),
        has_properties(
            place_numbers_set={5, 6},
            price_percent=100
        ),
        has_properties(
            place_numbers_set={3, 4},
            price_percent=80
        )
    ))


def test_ensure_base_rule_only_base():
    rule = PlacePriceRules(coach_type='platzkarte',
                           place_numbers='1, 2, 10',
                           price_percent=100,
                           can_apply_for_child=True)

    rules = ensure_base_rule([rule], 'platzkarte', {1, 2, 3, 4, 5, 6})

    assert_that(rules, contains(
        has_properties(
            place_numbers_set={1, 2, 3, 4, 5, 6},
            price_percent=100
        )
    ))
