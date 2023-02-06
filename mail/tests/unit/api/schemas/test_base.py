from marshmallow import fields

from mail.payments.payments.api.schemas.base import BaseSchema
from mail.payments.payments.utils.helpers import without_none


class BaseTestSchema(BaseSchema):
    asd = fields.String()
    name = fields.String()


def test_base_schema():
    json = {'asd': None, 'name': 'name'}
    assert BaseTestSchema().load(json).data == without_none(json)
