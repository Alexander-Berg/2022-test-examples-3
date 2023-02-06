from marshmallow import fields

from sendr_utils.schemas.base import BaseSchema
from sendr_utils.schemas.decorators import skip_none_on_dump


def test_skip_none_on_dump():
    @skip_none_on_dump(field_names=['y'])
    class MySchema(BaseSchema):
        x = fields.String()
        y = fields.String()

    assert MySchema().dump({'x': None, 'y': None}).data == {'x': None}
