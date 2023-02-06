from dataclasses import dataclass
from typing import Generic, List, TypeVar

import pytest
from marshmallow import ValidationError, fields

from sendr_utils.schemas.camel_case import CamelCaseSchema
from sendr_utils.schemas.dataclass import BaseDataClassSchema

T = TypeVar('T')


@dataclass
class Example:
    a: int
    b: str


@dataclass
class ExampleMany:
    examples: List[Example]


class ExampleSchema(BaseDataClassSchema[Example]):
    a = fields.Integer(required=True)
    b = fields.String()


class ExampleManySchema(BaseDataClassSchema[ExampleMany]):
    examples = fields.Nested(ExampleSchema, many=True)


class ExampleMultipleBasesSchema(CamelCaseSchema, Generic[T], BaseDataClassSchema[Example]):
    a = fields.Integer(required=True)
    b = fields.String()


class TestDataClassSchema:
    def test_load_success(self):
        result = ExampleSchema().load({'a': 5, 'b': 'hey'})

        assert result.data == Example(a=5, b='hey')
        assert not result.errors

    def test_load_schema_error(self):
        with pytest.raises(ValidationError):
            ExampleSchema().load({'b': 'hey'})

    def test_load_dataclass_error(self):
        """Возможно, не очень приятное поведение: если в схеме ошибка (пропущено required=True), то бросается
        исключение TypeError из конструктора dataclass. Сейчас это так работает. С этим можно что-то сделать, но
        сейчас я просто хочу подчеркнуть такое поведение этим тестом."""
        with pytest.raises(TypeError):
            ExampleSchema().load({'a': 1})

    def test_load_many_success(self):
        expected = ExampleMany(examples=[Example(a=1, b='hey')])
        actual = ExampleManySchema().load({'examples': [{'a': 1, 'b': 'hey'}]}).data
        assert expected == actual

    def test_multiple_bases_success(self):
        expected = Example(a=5, b='hey')
        actual = ExampleMultipleBasesSchema().load({'a': 5, 'b': 'hey'}).data
        assert expected == actual
