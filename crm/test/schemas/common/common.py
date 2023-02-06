from marshmallow import fields, Schema, ValidationError
from crm.space.test.schemas.common.user import UserSchema


def validate_string_isnumeric(value):

    if not value.isnumeric():
        raise ValidationError(f'{value} - is not a numeric string')


class IdNameSchema(Schema):
    id = fields.Int(required=True, strict=True)
    name = fields.Str(required=True)


class TagsSchema(Schema):
    eid = fields.Int(required=True)
    date = fields.DateTime(required=True)
    userId = fields.Int(required=True)
    userName = fields.Str(required=True)
    userLogin = fields.Str(required=True)
    authorRole = fields.Str(required=True)
    id = fields.Int(required=True)
    name = fields.Str(required=True)
    color = fields.Str(required=True)
    isArchived = fields.Bool(required=True)


class TagsKikSchema(Schema):
    id = fields.Int(required=True, strict=True)
    name = fields.Str(required=True)
    color = fields.Str(required=True)
    isArchived = fields.Bool(required=True)


class MessageInResponseSchema(Schema):
    level = fields.Str(required=True)
    text = fields.Str(required=True)


class CurrencySchema(Schema):
    code = fields.Str(required=True)
    id = fields.Int(strict=True, required=True)
    name = fields.Str(required=True)


class ContactEmailSchema(Schema):
    id = fields.Int(required=True, strict=True)
    email = fields.Email(required=True)
    isBroadcast = fields.Bool(required=True)
    tags = fields.List(fields.Nested(TagsKikSchema, required=True))


class ContactPhoneSchema(Schema):
    id = fields.Int(required=True, strict=True)
    phone = fields.Str(required=True)
    phoneE164 = fields.Str()


class CommentSchema(Schema):
    date = fields.DateTime(required=True)
    author = fields.Nested(UserSchema, required=True)
    text = fields.Str(required=True)


class KikSchema(Schema):
    contactId = fields.Int(required=True, strict=True)
    emails = fields.List(fields.Nested(ContactEmailSchema), required=True)
    phones = fields.List(fields.Nested(ContactPhoneSchema), required=True)
    comment = fields.Nested(CommentSchema, required=True)
    fax = fields.Str()
    firstName = fields.Str()
    lastName = fields.Str()
    middleName = fields.Str()
    name = fields.Str(required=True)
    isMain = fields.Bool(required=True)
    source = fields.Str(required=True)
    modifiedOn = fields.DateTime(required=True)
    birthDate = fields.DateTime()
    position = fields.Nested(IdNameSchema)


class ContactsSchema(Schema):
    items = fields.List(fields.Nested(KikSchema), required=True)
    messages = fields.List(fields.Nested(MessageInResponseSchema), required=True)
