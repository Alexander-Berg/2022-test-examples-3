import pytest

from crypta.lib.python.yt import schema_utils
from crypta.lib.python.yt.test_helpers import (
    row_transformers,
    tables,
)
from crypta.siberia.bin.common.yt_describer.proto.grouped_id_pb2 import TGroupedId
from crypta.siberia.bin.make_id_to_crypta_id.lib.maker import schema
from crypta.siberia.bin.common.yt_describer.proto.group_stats_pb2 import TGroupStats

pytest_plugins = [
    "crypta.lib.python.test_utils.user_data_fixture",
]


@pytest.fixture(scope="function")
def id_to_crypta_id_table():
    return tables.get_yson_table_with_schema("id_to_crypta_id.yson", "//home/crypta/qa/siberia/id_to_crypta_id", schema.get_id_to_crypta_id_schema())


@pytest.fixture(scope="function")
def input_table():
    return tables.get_yson_table_with_schema("input.yson", "//home/crypta/qa/input", schema_utils.get_schema_from_proto(TGroupedId))


@pytest.fixture(scope="function")
def output_table():
    row_transformer = row_transformers.yson_to_proto_dict(TGroupStats)
    return tables.YsonTable("output.yson", "//home/crypta/qa/output", yson_format="pretty", on_read=tables.OnRead(row_transformer=row_transformer))
