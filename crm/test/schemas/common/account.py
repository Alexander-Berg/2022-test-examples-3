from marshmallow import fields, Schema

from crm.space.test.schemas.common.common import CurrencySchema, ContactsSchema, IdNameSchema
from crm.space.test.schemas.common.user import UserSchema


class MonetizatorsSchema(Schema):
    eid = fields.Int(required=True, strict=True)
    date = fields.DateTime(required=True)
    userId = fields.Int(required=True, string=True)
    userName = fields.Str(required=True)
    authorRole = fields.Str(required=True)
    id = fields.Int(required=True, strict=True)
    name = fields.Str(required=True)
    color = fields.Str(required=True)
    isArchived = fields.Bool(required=True)


class SubAccountSchema(Schema):
    id = fields.Int(required=True, strict=True)
    name = fields.Str(required=True)
    login = fields.Str(required=True)
    clientId = fields.Int(required=True, strict=True)
    type = fields.Str(required=True)
    w0Total = fields.Float(required=True)


class AccountContractorInfoSchema(Schema):
    id = fields.Int(required=True)
    name = fields.Str(required=True)
    type = fields.Str(required=True)
    state = fields.Str(required=True)
    organization = fields.Nested(IdNameSchema, required=True)


class AccountContractorSchema(Schema):
    id = fields.Int(required=True)
    info = fields.Nested(AccountContractorInfoSchema, required=True)
    managers = fields.List(fields.Nested(UserSchema), required=True)
    isBindable = fields.Bool(required=True)
    appearanceKey = fields.Str(required=True)


class AccountZenPublisherSchema(Schema):
    publisherId = fields.Str(required=True)


class AccountCitySchema(Schema):
    id = fields.Int(required=True, strict=True)
    name = fields.Str(required=True)
    regionPath = fields.Str(required=True)


class AccountInfoSchema(Schema):
    id = fields.Int(required=True, strict=True)
    clientId = fields.Int(required=True, strict=True)
    name = fields.Str(required=True)
    login = fields.Str(required=True)
    uid = fields.Int(required=True, strict=True)
    type = fields.Str(required=True)
    isVip = fields.Bool()
    brand = fields.Nested(IdNameSchema, required=True)
    office = fields.Str(required=True)
    state = fields.Str(required=True)
    contractors = fields.List(fields.Nested(AccountContractorSchema), required=True)
    currency = fields.Nested(CurrencySchema)
    welcomeZone = fields.Bool()
    loginMetrika = fields.Str(required=True)
    marketDomain = fields.Str(required=True)
    zenPublishers = fields.List(fields.Nested(AccountZenPublisherSchema))
    yatelMcId = fields.Str()
    zenPublisherIds = fields.List(fields.Str())
    tier = fields.Nested(IdNameSchema)
    domain = fields.Str()
    city = fields.Nested(AccountCitySchema)
    isDifficultClient = fields.Bool(required=True)
    isFreelancer = fields.Bool(required=True)
    isInfluencer = fields.Bool(required=True)


class AccountSchema(Schema):
    id = fields.Int(required=True)
    info = fields.Nested(AccountInfoSchema, required=True)
    services = fields.Dict(keys=fields.Str(), values=fields.Bool, required=True)
    boClientId = fields.Int(required=True, strict=True)
    managers = fields.List(fields.Nested(UserSchema), required=True)
    importedKiks = fields.Nested(ContactsSchema, required=True)
    contacts = fields.Nested(ContactsSchema, required=True)
    sysDates = fields.Dict(required=True)
    subAccounts = fields.List(fields.Nested(SubAccountSchema), required=True)
    isBindable = fields.Bool(required=True)
    monetizators = fields.List(fields.Nested(MonetizatorsSchema), required=True)
    appearanceKey = fields.Str(required=True)
