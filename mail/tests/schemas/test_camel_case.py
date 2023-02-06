from decimal import Decimal

from marshmallow import fields

from sendr_utils.schemas import AmountField
from sendr_utils.schemas.camel_case import CamelCaseSchema


class InnerSchema(CamelCaseSchema):
    some_str = fields.String()


class ExampleSchema(CamelCaseSchema):
    some_int = fields.Integer(dump_to='i')
    some_amount = AmountField(load_from='d')
    inner_schema = fields.Nested(InnerSchema)


class CapitalFirstLetterSchema(ExampleSchema):
    CAPITAL_FIRST_LETTER = True


class TestSnakeCaseSchema:
    data = dict(some_int=5, some_amount=Decimal('13.23'), inner_schema=dict(some_str='x'))

    def test_load(self):
        result = ExampleSchema().load({'someInt': 5, 'd': '13.23', 'innerSchema': {'someStr': 'x'}})

        assert result.data == self.data
        assert not result.errors

    def test_dump(self):
        result = ExampleSchema().dump(self.data)

        assert result.data == {'i': 5, 'someAmount': '13.23', 'innerSchema': {'someStr': 'x'}}
        assert not result.errors


class TestCapitalFirstLetterSchema:
    data = dict(some_int=5, some_amount=Decimal('13.23'), inner_schema=dict(some_str='x'))

    def test_load(self):
        result = CapitalFirstLetterSchema().load({'SomeInt': 5, 'd': '13.23', 'InnerSchema': {'someStr': 'x'}})

        assert result.data == self.data
        assert not result.errors

    def test_dump(self):
        result = CapitalFirstLetterSchema().dump(self.data)

        assert result.data == {'i': 5, 'SomeAmount': '13.23', 'InnerSchema': {'someStr': 'x'}}
        assert not result.errors
