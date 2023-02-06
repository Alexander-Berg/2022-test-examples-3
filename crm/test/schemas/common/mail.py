from marshmallow import fields, Schema
from crm.space.test.schemas.common.user import UserSchema


class MailTimelineSchema(Schema):
    eType = fields.Str(required=True)
    dt = fields.Str(required=True)
    type = fields.Str(required=True)
    midX = fields.Str(required=True)
    hasAttach = fields.Bool(required=True)
    isExternal = fields.Bool(required=True)
    bodyPreview = fields.Str(required=True)
    author = fields.Nested(UserSchema, required=True)
    id = fields.Int(required=True)


class MailFileSchema(Schema):
    id = fields.Int(required=True)
    size = fields.Str(required=True)
    type = fields.Str(required=True)
    name = fields.Str(required=True)
    urlName = fields.Str(required=True)
    extension = fields.Str(required=True)


class MailSchema(Schema):
    id = fields.Int(required=True)
    date = fields.Str(required=True)
    from_ = fields.Str(required=True, attribute='from')
    to = fields.Str(required=True)
    cc = fields.Str(required=True)
    bcc = fields.Str(required=True)
    midX = fields.Str(required=True)
    subject = fields.Str(required=True)
    body = fields.Str(required=True)
    type = fields.Str(required=True)
    isHtml = fields.Bool(required=True)
    isSpam = fields.Bool(required=True)
    files = fields.List(fields.Nested(MailFileSchema), required=True)
