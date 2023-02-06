import pytest

from crypta.lib.proto.user_data.user_data_pb2 import TUserData
from crypta.lib.python.yql import proto_field as yql_proto_field
from crypta.lib.python.yt import schema_utils
from crypta.lib.python.yt.test_helpers import (
    row_transformers,
    tables,
)
from crypta.siberia.bin.common.proto.crypta_id_user_data_pb2 import TCryptaIdUserData
from crypta.siberia.bin.common.yt_describer.proto.group_stats_pb2 import TGroupStats


@pytest.fixture(scope="function")
def crypta_id_user_data_table():
    def get_crypta_id_user_data_table(filename, path):
        on_write = tables.OnWrite(
            sort_by=['crypta_id'],
            attributes={'schema': schema_utils.get_schema_from_proto(TCryptaIdUserData)},
            row_transformer=row_transformers.proto_dict_to_yson(TCryptaIdUserData),
        )
        return tables.YsonTable(
            filename,
            path,
            on_write=on_write,
        )
    return get_crypta_id_user_data_table


@pytest.fixture(scope="function")
def user_data_table():
    def get_user_data_table(filename, path):
        on_write = tables.OnWrite(
            sort_by=['yuid'],
            attributes=dict([('schema', schema_utils.get_schema_from_proto(TUserData))] +
                            list(yql_proto_field.get_attrs(TUserData).items())),
            row_transformer=row_transformers.proto_dict_to_yson(TUserData),
        )
        return tables.YsonTable(
            filename,
            path,
            on_write=on_write,
        )
    return get_user_data_table


@pytest.fixture(scope="function")
def segment_stats_table():
    def get_segment_stats_table(filename, path):
        on_write = tables.OnWrite(
            attributes={'schema': schema_utils.get_schema_from_proto(TGroupStats, key_columns=['GroupID'])},
            row_transformer=row_transformers.proto_dict_to_yson(TGroupStats),
        )
        return tables.YsonTable(
            filename,
            path,
            on_write=on_write,
        )
    return get_segment_stats_table
