from google.protobuf import json_format

from crypta.ltp.viewer.lib.compact_index.py import pack_index
from crypta.graph.export.proto.graph_pb2 import TGraph


def index_row_transformer(row):
    row["sources"] = pack_index.pack(row["sources"])
    return row


def crypta_id_to_graph_row_transformer(row):
    proto = json_format.ParseDict(row["Graph"], TGraph())
    row["Graph"] = proto.SerializeToString()
    return row
