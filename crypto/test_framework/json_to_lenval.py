import json
import os

from crypta.lib.python.rtmr.format import (
    json_rtmr,
    lenval
)
from crypta.lib.python.rtmr.model import serializers


class JsonToLenval(object):
    def __init__(self, serialize_value=None):
        self.decoder = json_rtmr.make_decoder(serialize_value)

    def __call__(self, table, extension="lenval"):
        new_table = "{}.{}".format(os.path.splitext(table)[0], extension)

        with open(table, "r") as f, lenval.Writer(new_table) as writer:
            for entry in json.load(f, cls=self.decoder):
                writer.write(entry)

        return new_table


class JsonToProto(JsonToLenval):
    def __init__(self, proto):
        super(JsonToProto, self).__init__(serializers.make_proto_serializer(proto))


class JsonToState(JsonToLenval):
    def __init__(self, serialize_value):
        super(JsonToState, self).__init__(lambda x: serializers.serialize_state(serialize_value(x)))

    def __call__(self, table):
        super(JsonToState, self).__call__(table, "state")


class JsonToProtoState(JsonToLenval):
    def __init__(self, proto):
        super(JsonToProtoState, self).__init__(serializers.make_state_proto_seralizer(proto))

    def __call__(self, table):
        super(JsonToProtoState, self).__call__(table, "state")
