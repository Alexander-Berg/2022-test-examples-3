# -*- coding: utf-8 -*-
from hamcrest import assert_that, calling, raises

from mpfs.platform.exceptions import FieldMustBeListError, FieldValidationError, FieldWrongIntegerRangeError
from mpfs.platform.fields import SerializerField, DateTimeToTSField, IntegerRangeField
from mpfs.platform.serializers import BaseSerializer
from test.unit.base import NoDBTestCase


class TestSerializer(BaseSerializer):
    fields = {
        'created': DateTimeToTSField(required=True, pbid=4, source='ctime', help_text=u'Дата создания'),
    }


class SerializerFieldTestCase(NoDBTestCase):
    value = [{
        'created': 1495541328,
    }]

    def test_non_list_error(self):
        f = SerializerField(TestSerializer, many=True)
        f.parent = BaseSerializer
        f.parent.format_options.datetime_as_timestamp = True
        f.to_native(self.value)
        with self.assertRaises(FieldMustBeListError):
            f.to_native(0)

    def test_timestamp_error(self):
        self.value[0]['created'] = "1495541328"

        f = SerializerField(TestSerializer, many=True)
        f.parent = BaseSerializer
        f.parent.format_options.datetime_as_timestamp = True
        with self.assertRaises(FieldValidationError):
            f.to_native(self.value)


class IntegerRangeFieldTestCase(NoDBTestCase):

    def test_valid_range_value(self):
        f = IntegerRangeField(0, 100)
        f.validate(0)
        f.validate(50)
        f.validate(100)

    def test_invalid_range_value(self):
        f = IntegerRangeField(0, 100)
        assert_that(calling(f.validate).with_args(-1), raises(FieldWrongIntegerRangeError))
        assert_that(calling(f.validate).with_args(101), raises(FieldWrongIntegerRangeError))
