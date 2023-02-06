from google.protobuf import json_format
import yt.yson as yson

from crypta.lib.python.yt.dyntables import kv_schema
from crypta.lib.python.yt.test_helpers import tables
from crypta.lib.proto.user_data.user_data_stats_pb2 import TUserDataStats
from crypta.lookalike.proto.lal_state_pb2 import TLalState


def audience_segments_row_transformer(row):
    return {
        "SegmentID": row["SegmentID"],
        "SegmentType": row["SegmentType"],
        "Stats": json_format.ParseDict(row["Stats"], TUserDataStats()).SerializeToString(),
    } if isinstance(row["Stats"], dict) else row


def get_audience_segments_schema():
    schema = [
        {"name": "Hash", "type": "uint64", "sort_order": "ascending", "expression": "farm_hash((SegmentID, SegmentType))"},
        {"name": "SegmentID", "type": "int64", "sort_order": "ascending"},
        {"name": "SegmentType", "type": "string", "sort_order": "ascending"},
        {"name": "Stats", "type": "string"},
    ]
    schema = yson.YsonList(schema)
    schema.attributes["strict"] = True
    schema.attributes["unique_keys"] = True
    return schema


def audience_segments_on_write():
    return tables.OnWrite(attributes={"schema": get_audience_segments_schema()}, row_transformer=audience_segments_row_transformer)


def lals_row_transformer(row):
    return {
        "key": row["key"],
        "value": json_format.ParseDict(yson.yson_to_json(row["value"]), TLalState()).SerializeToString(),
    } if isinstance(row["value"], dict) else row


def lals_on_write():
    return tables.OnWrite(attributes={"schema": kv_schema.get()}, row_transformer=lals_row_transformer)


def lal_row_transformer(row):
    lal_state = TLalState()
    lal_state.ParseFromString(row["value"])
    row["value"] = json_format.MessageToDict(lal_state)
    return row


def lals_on_read():
    return tables.OnRead(row_transformer=lal_row_transformer)


def remove_error_transformer(row):
    row.pop("error")
    return row


def errors_on_read():
    return tables.OnRead(row_transformer=remove_error_transformer)


def convert_embedding_to_length(row):
    row["embedding_length"] = len(row["embedding"])
    del row["embedding"]
    return row


def embeddings_on_read():
    return tables.OnRead(row_transformer=convert_embedding_to_length)
