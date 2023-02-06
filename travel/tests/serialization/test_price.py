# coding: utf-8
from __future__ import absolute_import, division, print_function, unicode_literals

import pytest

from common.models.currency import Price
from travel.rasp.wizards.wizard_lib.serialization.price import dump_price


@pytest.mark.parametrize('price, expected', (
    (None, None),
    (Price(10.5), {'value': 10.5, 'currency': 'RUR'}),
    (Price(100.5), {'value': 101, 'currency': 'RUR'}),
))
def test_dump_price(price, expected):
    assert dump_price(price) == expected
