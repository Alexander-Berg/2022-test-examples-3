# -*- coding: utf-8 -*-

import pytest

from travel.avia.library.python.common.models.currency import Currency
from travel.avia.library.python.tester.factories import create_currency


@pytest.mark.dbuser
def test_get_ordering_field_name():
    # по-умолчанию сортировка по order
    assert Currency.get_ordering_field_name('ru') == 'order'
    assert Currency.get_ordering_field_name('us') == 'order'
    assert Currency.get_ordering_field_name('kz') == 'order'

    # в турецкой и украинской версиях свой порядок сортировки
    assert Currency.get_ordering_field_name('tr') == 'order_tr'
    assert Currency.get_ordering_field_name('ua') == 'order_ua'


@pytest.mark.dbuser
def test_get_ordered_queryset():
    currency_1 = create_currency(code='c1', order=1, order_ua=2)
    currency_2 = create_currency(code='c2', order=2, order_ua=1)

    # сортировка по order
    assert list(Currency.get_ordered_queryset('ru')) == [currency_1, currency_2]

    # сортировка по специальному полю
    assert list(Currency.get_ordered_queryset('ua')) == [currency_2, currency_1]
