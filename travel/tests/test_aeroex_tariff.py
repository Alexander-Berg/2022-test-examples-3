# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest

from travel.rasp.library.python.common23.models.currency.currency import Currency
from travel.rasp.library.python.common23.models.currency.price import Price
from travel.rasp.library.python.common23.models.tariffs.tester.factories import create_aeroex_tariff


@pytest.mark.dbuser
@pytest.mark.parametrize('value, currency, expected_currency', (
    (10, None, Currency.BASE_CURRENCY),
    (20, 'USD', 'USD')
))
def test_price(value, currency, expected_currency):
    tariff = create_aeroex_tariff(tariff=value, currency=currency)
    assert tariff.price == Price(value, expected_currency)
