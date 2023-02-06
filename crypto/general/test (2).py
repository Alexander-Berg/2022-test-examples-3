from library.python.protobuf.json import (
    json2proto,
    proto2json,
)

from crypta.lib.python import yaml2proto
from crypta.lib.python.yaml2proto.ut.my_message_pb2 import TMyMessage


def get_msg():
    msg = TMyMessage()
    msg.Float = 1.0
    msg.InnerMessage.Int32 = 10
    msg.InnerMessage.String = "string"
    return msg


def test_proto2yaml():
    config = proto2json.Proto2JsonConfig(field_name_mode=proto2json.FldNameMode.FieldNameSnakeCase)

    return yaml2proto.proto2yaml(get_msg(), config)


def test_yaml2proto():
    src = (
        "inner_message:\n"
        "   int32: 10\n"
        "   string: string\n"
        "float: 1\n"
    )
    msg = TMyMessage()

    config = json2proto.Json2ProtoConfig(field_name_mode=json2proto.FldNameMode.FieldNameSnakeCase)
    yaml2proto.yaml2proto(src, msg, config)
    assert get_msg() == msg

    msg = TMyMessage()
    yaml2proto.yaml2proto(src, msg)
    assert get_msg() == msg
