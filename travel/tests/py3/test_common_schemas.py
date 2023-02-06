# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest
from django.utils.datastructures import MultiValueDict
from marshmallow import Schema, fields

from travel.rasp.library.python.common23.models.currency.price import Price
from travel.rasp.library.python.common23.tester.factories import create_currency
from travel.rasp.library.python.common23.models.serialization.common_schemas import PriceSchema, MultiValueDictSchemaMixin


@pytest.mark.dbuser
def test_get_currency_iso_code():
    create_currency(code='RUR', iso_code='RUB')

    # код iso сохраняется
    result = PriceSchema().dump(Price(10, 'RUB'))
    assert result == {'value': 10, 'currency': 'RUB'}

    # код расписаний конвертируется
    result = PriceSchema().dump(Price(10, 'RUR'))
    assert result == {'value': 10, 'currency': 'RUB'}

    # неизвестный код остается неизменным
    result = PriceSchema().dump(Price(1, 'BTC'))
    assert result == {'value': 1, 'currency': 'BTC'}

    # поддерживается сериализация из dict
    result = PriceSchema().dump({'value': 1, 'currency': 'BTC'})
    assert result == {'value': 1, 'currency': 'BTC'}

    # если в dict нет currency, то значение не сериализуется
    result = PriceSchema().dump({'value': 1})
    assert result == {'value': 1}


def test_load_price():
    data = '{"value": 200, "currency": "RUB"}'
    result = PriceSchema().loads(data)
    assert result.value == 200
    assert result.currency == 'RUB'

    data = '{"value": 20, "currency": "USD"}'
    result = PriceSchema().loads(data)
    assert result.value == 20
    assert result.currency == 'USD'

    data = '{"value": 1}'
    result = PriceSchema().loads(data)
    assert result.value == 1
    assert result.currency == 'RUR'


@pytest.mark.dbuser
@pytest.mark.parametrize('data, expected', (
    # получение значений из dict
    ({
        'field': 'field value',
        'list_field': ['list_field value 1', 'list_field value 2']
    }, {
        'field': 'field value',
        'list_field': ['list_field value 1', 'list_field value 2']
    }),

    # получение значений из MultiValueDict
    (MultiValueDict({
        'field': ['bogus field value', 'field value'],
        'list_field': ['list_field value 1', 'list_field value 2']
    }), {
        'field': 'field value',
        'list_field': ['list_field value 1', 'list_field value 2']
    }),

    # пропуск значений из MultiValueDict
    (MultiValueDict({
        'field': ['bogus field value', 'field value']
    }), {
        'field': 'field value'
    }),
))
def test_MultiValueDictSchemaMixin(data, expected):
    class TestSchema(Schema, MultiValueDictSchemaMixin):
        field = fields.String()
        list_field = fields.List(fields.String)

    result = TestSchema().load(data)

    assert result == expected


@pytest.mark.dbuser
@pytest.mark.parametrize('data, expected', (
    # получение значений из dict
    ({
        'raw_list_field': ['list_field value 1', 'list_field value 2']
    }, {
        'list_field': ['list_field value 1', 'list_field value 2']
    }),

    # получение значений из MultiValueDict
    (MultiValueDict({
        'raw_list_field': ['list_field value 1', 'list_field value 2']
    }), {
        'list_field': ['list_field value 1', 'list_field value 2']
    }),

    # пропуск значений из MultiValueDict
    (MultiValueDict({}), {}),
))
def test_MultiValueDictSchemaMixin_load_from(data, expected):
    class TestSchema(Schema, MultiValueDictSchemaMixin):
        list_field = fields.List(fields.String, data_key='raw_list_field')

    # получение значений из dict
    result = TestSchema().load(data)

    assert result == expected
