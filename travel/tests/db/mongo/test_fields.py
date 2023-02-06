# coding: utf-8
from __future__ import unicode_literals, absolute_import, division, print_function

import pytest
from enum import Enum
from mongoengine import Document

from common.db.mongo.fields import StringEnumField


class SomeTestEnum(Enum):
    FIRST_VALUE = 'first_value'
    SECOND_VALUE = 'second_value'
    LAST_VALUE = 'last_value'


class FieldsTestDocument(Document):
    string_enum_field = StringEnumField(SomeTestEnum)


@pytest.mark.mongouser
def test_string_enum_field():
    doc = FieldsTestDocument.objects.create(string_enum_field=SomeTestEnum.SECOND_VALUE)
    assert doc.string_enum_field == SomeTestEnum.SECOND_VALUE

    query_result = FieldsTestDocument.objects.get(string_enum_field=SomeTestEnum.SECOND_VALUE)
    assert query_result.id == doc.id
    assert query_result.string_enum_field == SomeTestEnum.SECOND_VALUE
