# -*- coding: utf-8 -*-
from nose_parameterized.parameterized import parameterized

from test.unit.base import NoDBTestCase
from mpfs.platform.v1.disk.serializers import ResourceSerializer
from mpfs.platform.v1.disk.fields import MpfsSortField


class MpfsSortFieldTestCase(NoDBTestCase):
    @parameterized.expand([
        ('flat_sort', 'sort'),
        ('nested.sort', 'sort'),
    ])
    def test_fields_mapping_only(self, input, output):
        field = MpfsSortField(fields_mapping={input: output})
        assert field.to_native(input)['field'] == output

    @parameterized.expand([
        ('exif.date_time', ResourceSerializer, 'etime'),
        ('created', ResourceSerializer, 'fields_mapping_value'),
    ])
    def test_fields_mapping_override_serializer_cls(self, input, serializer_cls, output):
        field = MpfsSortField(serializer_cls=serializer_cls, fields_mapping={input: output})
        assert field.to_native(input)['field'] == output

    @parameterized.expand([
        ('created', ResourceSerializer, 'ctime'),
        ('modified', ResourceSerializer, 'mtime'),
    ])
    def test_serializer_cls(self, input, serializer_cls, output):
        field = MpfsSortField(serializer_cls=serializer_cls, fields_mapping={input: output})
        assert field.to_native(input)['field'] == output