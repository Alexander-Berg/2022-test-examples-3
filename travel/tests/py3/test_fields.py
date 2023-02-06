# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

import marshmallow
import pytest
import pytz
from datetime import datetime
from marshmallow import Schema, ValidationError, fields

from travel.rasp.library.python.common23.models.serialization.fields import DatetimeAwareField, DictField, DictNestedField, PointField
from travel.rasp.library.python.common23.tester.factories import create_settlement


class NestedValueSchema(Schema):
    title = fields.String()
    number = fields.Function(
        serialize=lambda v, c: (v['number'], c['number_context']),
        deserialize=lambda v, c: '<{} {}>'.format(v, c['number_context'])
    )


class DictsTestSchema(Schema):
    dict_value = DictField(fields.Number())
    dict_nested_value = DictNestedField(NestedValueSchema)


@pytest.fixture
def data():
    return {
        'dict_value': {'first': 100500, 'second': 200300},
        'dict_nested_value': {
            'first_key': {'title': 'FirstTitle', 'number': 'FirstNumber'},
            'second_key': {'title': 'SecondTitle', 'number': 'SecondNumber'},
        },
    }


def test_datetime_aware_field():
    class TestSchema(Schema):
        test_field = DatetimeAwareField()

    with pytest.raises(ValidationError):
        TestSchema().load({'test_field': {}})

    with pytest.raises(ValidationError):
        TestSchema().load({'test_field': {'tzname': 'bad timezone'}})

    with pytest.raises(ValidationError):
        TestSchema().load({'test_field': {'tzname': 'UTC', 'local': None}})

    with pytest.raises(ValidationError):
        TestSchema().load({'test_field': {'tzname': 'UTC', 'local': '2000-01-01'}})

    assert TestSchema().load({
        'test_field': {
            'local': '2000-01-01 12:00:00',
            'tzname': 'Asia/Yekaterinburg',
        }
    })['test_field'] == pytz.timezone('Asia/Yekaterinburg').localize(datetime(2000, 1, 1, 12))


def test_dicts_serialize(data):
    result = DictsTestSchema(context={'number_context': 'NumberContext'}).dump(data)
    assert result == {
        'dict_value': {'first': 100500, 'second': 200300},
        'dict_nested_value': {
            'first_key': {'title': 'FirstTitle', 'number': ('FirstNumber', 'NumberContext')},
            'second_key': {'title': 'SecondTitle', 'number': ('SecondNumber', 'NumberContext')},
        }
    }


def test_dicts_deserialize(data):
    result = DictsTestSchema(context={'number_context': 'NumberContext'}).load(data)
    assert result == {
        'dict_value': {'first': 100500, 'second': 200300},
        'dict_nested_value': {
            'first_key': {'title': 'FirstTitle', 'number': '<FirstNumber NumberContext>'},
            'second_key': {'title': 'SecondTitle', 'number': '<SecondNumber NumberContext>'},
        }
    }


def test_dictfield_valueerror():
    with pytest.raises(ValueError):
        DictField(fields.Number)

    with pytest.raises(ValueError):
        DictField(fields.Nested(NestedValueSchema))


def test_dictnestedfield_validation():
    with pytest.raises(ValidationError) as ex:
        DictsTestSchema().load({
            'dict_value': {'first': 100500, 'second': 200300},
            'dict_nested_value': 'foo'
        })

    assert ex.value.messages == {
        'dict_nested_value': ["Invalid type str of 'foo' value for DictNestedField"]
    }
    assert ex.value.valid_data == {
        'dict_value': {'first': 100500, 'second': 200300}
    }

    with pytest.raises(ValidationError) as ex:
        DictsTestSchema().load({
            'dict_value': {'first': 100500, 'second': 200300},
            'dict_nested_value': {
                'first_key': {'title': []},
                'second_key': {'title': 'SecondTitle'}
            }
        })
    assert ex.value.messages == {
        'dict_nested_value': {
            'first_key': {'title': ['Not a valid string.']}
        }
    }
    assert ex.value.valid_data == {
        'dict_value': {'first': 100500, 'second': 200300},
    }


@pytest.mark.dbuser
def test_point_field():
    class TestSchema(marshmallow.Schema):
        point = PointField()

    settlement = create_settlement()

    result = TestSchema().load({'point': settlement.point_key})
    assert result['point'] == settlement

    result = TestSchema().dump({'point': settlement})
    assert result['point'] == settlement.point_key

    with pytest.raises(ValidationError) as ex:
        TestSchema().load({'point': ''})
    assert ex.value.messages == {'point': ['Wrong value "" for PointField']}

    with pytest.raises(ValidationError) as ex:
        TestSchema().load({'point': 'c100500'})
    assert ex.value.messages == {'point': ['Point c100500 was not found']}
