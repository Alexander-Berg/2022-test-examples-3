import json
import re

from crypta.lib.python.rtmr.format import (
    json_rtmr,
    lenval,
)
from crypta.lib.python.rtmr.model import parsers


class LenvalToJson(object):
    def __init__(self, parser=None, extension="json"):
        self.encoder = json_rtmr.make_encoder(parser)
        self.extension = extension

    def __call__(self, table):
        new_table = "{}.{}".format(table.replace(":", "-"), self.extension)
        with open(new_table, "w") as f, lenval.Reader(table) as reader:
            json.dump(
                list(reader),
                f,
                cls=self.encoder,
                indent=2,
                ensure_ascii=False,
                sort_keys=True,
            )

        return new_table


class ProtoToJson(LenvalToJson):
    def __init__(self, proto):
        super(ProtoToJson, self).__init__(parsers.make_proto_parser(proto))


class StateToJson(LenvalToJson):
    def __init__(self, parser):
        super(StateToJson, self).__init__(lambda x: parser(parsers.parse_state(x)))


class StateProtoToJson(LenvalToJson):
    def __init__(self, proto):
        super(StateProtoToJson, self).__init__(parsers.make_state_proto_parser(proto))


class ErrorsToJson(LenvalToJson):
    def __init__(self):
        super(ErrorsToJson, self).__init__(lambda x: re.sub(r":\d+:", ":line:", x), "errors.json")
