# coding: utf8
from __future__ import unicode_literals, absolute_import, division, print_function

from marshmallow import fields, Schema

from travel.rasp.library.python.common23.models.serialization.schema import CachingSchema, get_defaults_from_schema


class Sample(object):
    def __init__(self, id, value):
        self.id = id
        self.value = value


class SampleSchema(CachingSchema):
    id = fields.Integer()
    value = fields.String()

    def get_cache_key(self, obj):
        return obj.id


def test_cashing_schema():
    obj = Sample(42, 'Yes!')
    serialized_obj = {'id': obj.id, 'value': obj.value}
    schema = SampleSchema()
    assert schema.dump(obj) == serialized_obj
    assert len(schema._dump_cache) == 1

    obj.value = 'No'
    assert schema.dump(obj) == serialized_obj
    assert len(schema._dump_cache) == 1


def test_cashing_schema_different_instances_have_different_caches():
    obj = Sample(42, 'Yes!')
    serialized_obj = {'id': obj.id, 'value': obj.value}
    schema = SampleSchema()
    assert schema.dump(obj) == serialized_obj

    other_schema = SampleSchema()
    obj.value = 'No'
    assert other_schema.dump(obj) != serialized_obj


def test_get_defaults_from_schema():
    class SchemaWithDefaults(Schema):
        foo = fields.String()
        bar = fields.String(allow_none=True)
        baz = fields.String(default=None)
        qux = fields.String(default='qux')

    assert get_defaults_from_schema(SchemaWithDefaults) == {
        'bar': None, 'baz': None, 'qux': 'qux'
    }
