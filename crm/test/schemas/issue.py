from marshmallow import fields, Schema, validate

from crm.space.test.schemas.common.account import AccountSchema
from crm.space.test.schemas.common.common import TagsSchema, MessageInResponseSchema, IdNameSchema
from crm.space.test.schemas.common.user import UserSchema


class IssueAttributeFieldPropsSchema(Schema):
    time = fields.Bool()
    minDate = fields.DateTime()
    isSingleValue = fields.Bool()
    provider = fields.Str()
    mode = fields.Str()
    items = fields.List(fields.Nested(IdNameSchema))


class IssueAttributeSchema(Schema):
    name = fields.Str(required=True)
    label = fields.Str(required=True)
    component = fields.Str(required=True)
    backendUpdateKey = fields.Str()
    access = fields.Int()
    fieldProps = fields.Nested(IssueAttributeFieldPropsSchema)


class IssueAttributesSchema(Schema):
    attributes = fields.List(fields.Nested(IssueAttributeSchema))


class IssueFilterSchema(Schema):
    name = fields.Str(required=True)
    color = fields.Str()
    counter = fields.Int(strict=True)
    disabled = fields.Bool()
    id = fields.Int()


class IssueInnSchema(Schema):
    id = fields.Str(required=True)
    name = fields.Str(required=True)


class TimerSchema(Schema):
    id = fields.Int(required=True, strict=True)
    author = fields.Nested(UserSchema, required=True)
    users = fields.List(fields.Nested(UserSchema))
    startDate = fields.DateTime(required=True)
    action = fields.Int(required=True)
    comment = fields.Str()


class SkillSchema(Schema):
    id = fields.Int(required=True)
    name = fields.Str(required=True)
    value = fields.Int(required=True)


class IssueDataSchema(Schema):
    account = fields.Nested(AccountSchema)
    inn = fields.Nested(IssueInnSchema)
    followers = fields.List(fields.Nested(UserSchema))
    priority = fields.Nested(IdNameSchema, required=True)
    ticketLine = fields.Int(validate=validate.OneOf([1, 2]), required=True)
    byEid = fields.Int(required=True)
    byEtype = fields.Int(required=True)
    communicationTypeId = fields.Nested(IdNameSchema)
    skills = fields.List(fields.Nested(SkillSchema), required=True)
    createdOn = fields.DateTime(required=True)
    modifiedOn = fields.DateTime(required=True)
    typeId = fields.Int(required=True)
    type = fields.Str(required=True)
    number = fields.Int(required=True)
    name = fields.Str(required=True)
    state = fields.Nested(IdNameSchema, required=True)
    isDone = fields.Bool(required=True)
    author = fields.Nested(UserSchema, required=True)
    owner = fields.Nested(UserSchema)
    tags = fields.List(fields.Nested(TagsSchema))
    timers = fields.List(fields.Nested(TimerSchema), required=True)
    workflow = fields.Nested(IdNameSchema, required=True)
    category = fields.Nested(IdNameSchema)
    issueDt = fields.DateTime(required=True)
    queue = fields.Nested(IdNameSchema, required=True)
    id = fields.Int(required=True)
    standardHours = fields.Int(strict=True)
    startBeforeDt = fields.DateTime()
    spentHours = fields.Int(strict=True)
    complexity = fields.Nested(IdNameSchema)
    businessUnitId = fields.Nested(IdNameSchema)
    yaServiceId = fields.Nested(IdNameSchema)
    geoPresentationDate = fields.DateTime()
    geoExpectedPaymentDate = fields.DateTime()
    geoPaymentDate = fields.DateTime()
    geoBudget = fields.Str()
    geoSMVPOrderId = fields.Int(strict=True)
    geoSMVPCampaignId = fields.Int(strict=True)
    geoSMVPFlightGroupId = fields.Int(strict=True)
    geoPlacementPeriod = fields.Nested(IdNameSchema)


class IssueContentSchema(Schema):
    data = fields.Nested(IssueDataSchema)
    scheme = fields.Nested(IssueAttributesSchema, required=True)
    props = fields.Dict(required=True)
    id = fields.Int(required=True)


class IssueStorageSchema(Schema):
    issues = fields.Dict(keys=fields.Str(), values=fields.Nested(IssueContentSchema), required=True)
    issueFilters = fields.Dict(keys=fields.Str(), values=fields.Nested(IssueFilterSchema), many=True)
    nodes = fields.Dict(required=True)


class IssueSchema(Schema):
    storage = fields.Nested(IssueStorageSchema, required=True)
    messages = fields.List(fields.Nested(MessageInResponseSchema), required=True)
