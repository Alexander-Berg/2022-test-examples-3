import zlib

from google.protobuf import json_format

from crypta.cm.services.common.proto import match_pb2
from crypta.lib.python.yt.dyntables import kv_schema
from crypta.lib.python.yt.test_helpers import tables


def is_back_ref_record(key):
    return key.startswith("yandexuid:") or key.startswith("icookie:")


def get_empty_protobuf(key):
    return match_pb2.TBackReference() if is_back_ref_record(key) else match_pb2.TMatch()


def match_row_transformer(row):
    return {
        "key": row["key"],
        "value": zlib.compress(json_format.ParseDict(row["value"], get_empty_protobuf(row["key"])).SerializeToString()),
    }


def cm_db_on_write():
    return tables.OnWrite(attributes={"schema": kv_schema.get()}, row_transformer=match_row_transformer)
