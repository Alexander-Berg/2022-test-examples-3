# coding: utf-8

import market.pylibrary.marketprices.price_expression as pe
from collections import namedtuple

# value precision expected price in price expression.
VALUE_PRECISION_DATA = [[120000000, 7, '12.000000'],
                        [120000,    3, '120.000000'],
                        [120000001, 7, '12.000000'],  # Смотрим округление к ближайшему целому
                        [120000009, 7, '12.000001'],  # Смотрим округление к ближайшему целому
                        [1200, 7,      '0.000120']]    # Ведущий '0', 6 знаков после '.'


# Тестируем корректное представление цена в price_expression с учётом precision.
def test_correct_price_value():
    plus = 0
    rate = '1'
    currency = 'RUR'
    ref_currency = 'RUR'

    for value, precision, expected_price in VALUE_PRECISION_DATA:
        expected_price_expression = "{} 1 0 RUR RUR".format(expected_price)
        price_expression = pe.get_price_expression(value, precision, plus, rate, currency, ref_currency)

        assert price_expression == expected_price_expression


# Тестируем корретное представленте plus в price_expression
def test_correct_plus_value():
    assert pe.get_price_expression(10, 1, int(0), '1', 'RUR', 'RUR') == '1.000000 1 0 RUR RUR'
    assert pe.get_price_expression(10, 1, float(0.0), '1', 'RUR', 'RUR') == '1.000000 1 0 RUR RUR'


# Проверяем корректные дефолтные значения
def test_correct_default_values():
    assert pe.get_price_expression(10, 1, int(0)) == '1.000000 1 0 RUR RUR'


# Проверяем, что корректно задаются валюты
def test_correct_currencies():
    assert pe.get_price_expression(10, 1, int(0), '1', 'USD', 'RUR') == '1.000000 1 0 USD RUR'
    assert pe.get_price_expression(10, 1, int(0), '1', 'RUR', 'USD') == '1.000000 1 0 RUR USD'


# Тестируем корректное значение цены в price_expression для функции get_price_expression_from_binary_price
def test_correct_price_for_binary_input():
    TPriceExpression = namedtuple('TPriceExpression', 'price plus rate id ref_id')

    plus = 0
    rate = '1'
    currency = 'RUR'
    ref_currency = 'RUR'
    default_precision = 7

    for value, precision, expected_price in list([arr for arr in VALUE_PRECISION_DATA if arr[1] == default_precision]):
        expected_price_expression = "{} 1 0 RUR RUR".format(expected_price)
        binary_price = TPriceExpression(value, plus, rate, currency, ref_currency)
        price_expression = pe.get_price_expression_from_binary_price(binary_price)

        assert price_expression == expected_price_expression
