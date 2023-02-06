from marshmallow import fields, Schema

from crm.space.test.schemas.common.common import MessageInResponseSchema
from crm.space.test.schemas.common.mail import MailTimelineSchema


# TO DO: add "scheme" and "categorization" Schemas
class TimelineMailSchema(Schema):
    data = fields.Nested(MailTimelineSchema, required=True)
    scheme = fields.Dict(required=True)
    props = fields.Dict(required=True)
    categorization = fields.Dict(required=True)
    id = fields.Int(required=True)


# TO DO: add another Schemas (plannedActivities, meetings, etc...)
class TimelineStorageSchema(Schema):
    issues = fields.Dict(required=True)
    comments = fields.Dict(required=True)
    mails = fields.Dict(keys=fields.Str(), values=fields.Nested(TimelineMailSchema), required=True)
    meetings = fields.Dict(required=True)
    calls = fields.Dict(required=True)
    meetings = fields.Dict(required=True)
    plannedActivities = fields.Dict(required=True)
    nodes = fields.Dict(required=True)


class TimelineSchema(Schema):
    storage = fields.Nested(TimelineStorageSchema, required=True)
    messages = fields.List(fields.Nested(MessageInResponseSchema), required=True)
