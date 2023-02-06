import re

from google.protobuf import (
    json_format,
    message,
)
from mapreduce.yt.interface.protos import extension_pb2
import six
from yt import yson

from crypta.lib.python import tskv


def tskv_value_to_dict(row):
    row["value"] = tskv.tskv_to_dict(row["value"])
    return row


def delete_subkey(row):
    del row["subkey"]
    return row


def delete_ts_from_value(row):
    del row["value"]["ts"]
    return row


def remove_frame_info(field):
    def row_transformer(row):
        pattern = "(\\w|/|\\.)+:(\\d)+: "
        row[field] = re.sub(pattern, "", row[field])
        return row

    return row_transformer


def stream_transformer_from_row_transformer(row_transformer):
    def stream_transformer(stream):
        output = six.BytesIO()
        yson.dump([
            row_transformer(row) for row in yson.load(stream, yson_type="list_fragment")
        ], output, yson_format="binary", yson_type="list_fragment")
        output.seek(0)

        return output

    return stream_transformer


def proto_dict_to_yson(proto_class):
    def row_transformer(row):
        result = {}
        proto = json_format.ParseDict(yson.yson_to_json(row), proto_class())

        for descriptor, value in proto.ListFields():
            column_name = _yt_column_name(descriptor)
            column_value = value.SerializeToString() if isinstance(value, message.Message) else value
            result[column_name] = column_value

        return result

    return row_transformer


def yson_to_proto_dict(proto_class):
    def row_transformer(row):
        proto = proto_class()

        for descriptor in proto.DESCRIPTOR.fields:
            column_name = _yt_column_name(descriptor)
            column_value = row[column_name]

            if column_value is not None:
                if descriptor.message_type is not None:
                    getattr(proto, descriptor.name).ParseFromString(yson.get_bytes(column_value))
                else:
                    setattr(proto, descriptor.name, column_value)

            del row[column_name]

        assert not row, "Unexpected fields: '{}'".format(row)

        return json_format.MessageToDict(proto)

    return row_transformer


def _yt_column_name(descriptor):
    return descriptor.GetOptions().Extensions[extension_pb2.column_name] or descriptor.name
