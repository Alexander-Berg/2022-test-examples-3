# -*- coding: utf-8 -*-

from __future__ import unicode_literals, absolute_import

import pytest
from mock import Mock
from marshmallow import Schema, ValidationError, fields

from travel.avia.library.python.common.models.geo import Station, Settlement, Country

from travel.avia.backend.main.api.fields import DictNestedField, ListToDictField, Related, PointKey, SettlementKey, StationKey
from travel.avia.backend.tests.main.api_test import TestApiHandler


class TestPointKey(TestApiHandler):
    def test_station(self):
        field = PointKey()
        assert field.deserialize('s123') == (Station, u'123')
        assert field.serialize('point_key', Station(id=123)) == 's123'

    def test_settlement(self):
        field = PointKey()
        assert field.deserialize('c123') == (Settlement, u'123')
        assert field.serialize('point_key', Settlement(id=123)) == 'c123'

    def test_country(self):
        field = PointKey()
        assert field.deserialize('l123') == (Country, u'123')
        assert field.serialize('point_key', Country(id=123)) == 'l123'

    # FIXME не уверен что тут нужно уметь g123
    # def test_geoid(self):
    #     pass

    def test_incorrect(self):
        field = PointKey()
        with pytest.raises(ValidationError):
            field.deserialize(123)
        with pytest.raises(ValidationError):
            field.deserialize('haha')


class TestSettlementKey(TestApiHandler):
    def test_correct(self):
        field = SettlementKey()
        assert field.deserialize('c213') == 'c213'

    def test_incorrect(self):
        field = SettlementKey()
        with pytest.raises(ValidationError):
            field.deserialize(123)
        with pytest.raises(ValidationError):
            field.deserialize('haha')
        with pytest.raises(ValidationError):
            field.deserialize('s213')


class TestStationKey(TestApiHandler):
    def test_correct(self):
        field = StationKey()
        assert field.deserialize('s213') == 's213'

    def test_incorrect(self):
        field = StationKey()
        with pytest.raises(ValidationError):
            field.deserialize(123)
        with pytest.raises(ValidationError):
            field.deserialize('haha')
        with pytest.raises(ValidationError):
            field.deserialize('c213')


def handler_wrap(result):
    def inner_wrapper(params, field):
        return result

    def wrapper(obj=None, params=None):
        if callable(result):
            return result

        return inner_wrapper

    return wrapper


class TestRelated(TestApiHandler):
    def test_no_handler(self):
        with pytest.raises(ValueError):
            Related()

    def test_none(self):
        field = Related(handler=handler_wrap('result'))
        assert field.serialize('key', None) is None

    def test_require_context(self):
        field = Related(handler=handler_wrap('result'))
        field.parent = Mock(context=None)

        assert field.serialize('myfield', 1) == {
            'status': 'error',
            'reason': 'No context available for Related field "myfield"'
        }

    def test_field_not_in_fields(self):
        field = Related(handler=handler_wrap('result'))
        field.name = 'myname'
        field.parent = Mock(context={'fields': {}})

        assert field.serialize('key', 1) is None

    def _patch_field(self, field, name):
        field.name = name
        field.parent = Mock(context={'fields': {name: {
            'params': {},
            'fields': []
        }}})

    def test_simple(self):
        field = Related(handler=handler_wrap('result'))
        self._patch_field(field, 'myname')

        assert field.serialize('myname', 1) == 'result'

    def test_set_params_dict(self):
        field = Related(
            handler=handler_wrap(lambda params, fields: params['target']),
            params={'target': 'source'}
        )
        self._patch_field(field, 'myname')

        obj = {
            'source': 'source value'
        }

        assert field.serialize('myname', obj) == 'source value'

    def test_set_params_obj(self):
        field = Related(
            handler=handler_wrap(lambda params, fields: params['target']),
            params={'target': 'source'}
        )
        self._patch_field(field, 'myname')

        obj = Mock(source='source value')

        assert field.serialize('myname', obj) == 'source value'

    def test_set_params_callable(self):
        obj = {
            'source': 'source value'
        }

        field = Related(
            handler=handler_wrap(lambda params, fields: params['target']),
            params={'target': lambda obj, context: obj['source']}
        )
        self._patch_field(field, 'myname')

        assert field.serialize('myname', obj) == 'source value'

    def test_set_self_param(self):
        field = Related(
            handler=handler_wrap(lambda params, fields: params['target']),
            params={'target': 'self'}
        )
        self._patch_field(field, 'myname')

        assert field.serialize('myname', 'obj value') == 'obj value'

    def test_set_extra_params(self):
        field = Related(
            handler=handler_wrap(lambda params, fields: params['target']),
            extra_params={'target': 'hello'}
        )
        self._patch_field(field, 'myname')

        assert field.serialize('myname', 'obj value') == 'hello'


class TestListToDictField(TestApiHandler):
    def test_in_schema(self):
        class Item(object):
            def __init__(self, id, title):
                self.id = id
                self.title = title

        class ItemSchema(Schema):
            id = fields.Int()
            title = fields.Str()

        class TestSchema(Schema):
            items = ListToDictField(ItemSchema)

        schema = TestSchema()
        result = schema.dump({
            'items': [Item(1, 'one'), Item(2, 'two')]
        }).data

        # уберём обёртки MarshalResult
        # result['items'] = {
        #     key: value.data
        #     for key, value in result['items'].items()
        # }

        expected = {
            'items': {
                u'1': {'id': 1, 'title': u'one'},
                u'2': {'id': 2, 'title': u'two'},
            }
        }

        assert result == expected


class TestDictField(TestApiHandler):
    def test_in_schema(self):
        class ItemSchema(Schema):
            title = fields.Str()

        class TestSchema(Schema):
            items = DictNestedField(ItemSchema)

        schema = TestSchema()
        result = schema.dump({
            'items': {
                'key1': {'title': 'title1'},
                'key2': {'title': 'title2'}
            }
        }).data

        # уберём обёртки MarshalResult
        # result['items'] = {
        #     key: value.data
        #     for key, value in result['items'].items()
        # }

        expected = {
            'items': {
                'key1': {'title': 'title1'},
                'key2': {'title': 'title2'}
            }
        }

        assert result == expected
