# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

from decimal import Decimal

import pytest

from common.apps.train.models import PlacePriceRules
from travel.rasp.train_api.train_partners.base.test_utils import CoachDetailsStub
from travel.rasp.train_api.train_partners.base.train_details.tariff_calculator.tariff_calculator import get_base_tariff


SERVICE_TARIFF = Decimal(100)


@pytest.mark.parametrize('rules_percents, max_tariff, expected', (
    ([], 1000, None),
    ([100, 60], 1100, 1100),
    ([120, 100, 80], 1300, 1100),
    ([80, 60], 900, 1100)
))
def test_get_base_tariff(rules_percents, max_tariff, expected):
    rules = [PlacePriceRules(price_percent=Decimal(percent)) for percent in rules_percents]
    coach = CoachDetailsStub(max_tariff=Decimal(max_tariff), service_tariff=SERVICE_TARIFF, price_rules=rules)

    base_tariff = get_base_tariff(coach)

    assert base_tariff == expected
