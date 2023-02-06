from marshmallow import fields, Schema


class StaffGroupSchema(Schema):
    name = fields.Str(required=True)
    url = fields.Str(required=True)


class StaffGapSchema(Schema):
    color = fields.Str(required=True)
    name = fields.Str(required=True)


class StaffPhonesSchema(Schema):
    work = fields.Str()


class UserEmailSchema(Schema):
    label = fields.Str()
    value = fields.Str()


class UserSchema(Schema):
    id = fields.Int(required=True)
    login = fields.Str(required=True)
    name = fields.Str(required=True)
    first_name = fields.Str()
    last_name = fields.Str()
    group = fields.Nested(StaffGroupSchema, required=True, allow_none=True)
    is_dismissed = fields.Bool(required=True)
    is_homie = fields.Bool(required=True)
    gap = fields.Nested(StaffGapSchema)
    office = fields.Str()
    phones = fields.Nested(StaffPhonesSchema)
    position = fields.Str()
    crm_email = fields.Nested(UserEmailSchema)
    crm_position = fields.Str()
    binded = fields.Bool()
    allowedToUnbind = fields.Bool()
    accountManagerRole = fields.Str()
    etype = fields.Str()
