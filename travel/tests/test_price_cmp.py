# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from travel.rasp.library.python.common23.models.currency.price import Price


def test_cmp():
    assert Price(10) > Price(9)
    assert Price(9) < Price(10)

    assert not Price(10) < Price(9)
    assert not Price(9) > Price(10)

    assert Price(10) > 9
    assert Price(9) < 10

    assert not Price(10) < 9
    assert not Price(9) > 10

    assert 10 > Price(9)
    assert 9 < Price(10)

    assert not 10 < Price(9)
    assert not 9 > Price(10)


def test_sort():
    assert sorted([Price(11), Price(9), Price(10)]) == [Price(9), Price(10), Price(11)]
    assert sorted([Price(11), Price(9), 10]) == [Price(9), 10, Price(11)]
