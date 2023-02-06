# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest
from django.utils.datastructures import MultiValueDict
from marshmallow import Schema, ValidationError, fields

from common.tester.factories import create_country, create_settlement
from common.tester.testcase import TestCase
from travel.rasp.train_api.serialization.schema_bases import MultiValueDictSchemaMixin, PointsQuerySchema


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

    assert not result.errors
    assert result.data == expected


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
        list_field = fields.List(fields.String, load_from='raw_list_field')

    # получение значений из dict
    result = TestSchema().load(data)

    assert not result.errors
    assert result.data == expected


class TestPointsQuerySchema(TestCase):
    def setUp(self):
        self.schema = PointsQuerySchema()

    def test_validate_point_is_country(self):
        data, errors = self.schema.load({'pointFrom': create_country().point_key,
                                         'pointTo': create_country().point_key})
        assert errors['_schema'] == [{
            'point_from': 'country_point',
            'point_to': 'country_point'
        }]

    def test_validate_point_not_found(self):
        data, errors = self.schema.load({'pointFrom': '', 'pointTo': 'c1111111'})
        assert errors['_schema'] == [{
            'point_from': 'no_such_point',
            'point_to': 'no_such_point'
        }]

    def test_validate_ambiguous_points(self):
        settlement = create_settlement()
        with pytest.raises(ValidationError) as exc:
            self.schema.validate_points({
                'point_from': settlement,
                'point_to': settlement
            })
            assert exc.messages == {
                'ambiguous': 'ambiguous_points',
            }
