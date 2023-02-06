import yatest.common

from crypta.lib.python.yt.test_helpers import (
    row_transformers,
    tables,
    tests,
)
from crypta.siberia.bin.common import yt_schemas
from crypta.siberia.bin.common.proto.crypta_id_user_data_pb2 import TCryptaIdUserData


def test_basic(yt_stuff, config_path, config):
    schema = yt_schemas.get_crypta_id_user_data_schema()
    output_row_transformer = row_transformers.yson_to_proto_dict(TCryptaIdUserData)
    return tests.yt_test(
        yt_client=yt_stuff.get_yt_client(),
        binary=yatest.common.binary_path("crypta/siberia/bin/convert_to_user_data_stats/bin/crypta-siberia-convert-to-user-data-stats"),
        args=["--config", config_path],
        data_path=yatest.common.test_source_path("data"),
        input_tables=[(
            tables.YsonTable("input.yson", config.InputTable),
            tests.TableIsNotChanged(),
        ), (
            tables.get_yson_table_with_schema("old_output.yson", config.OutputTable, schema),
            None,
        )],
        output_tables=[
            (
                tables.YsonTable("output.yson", config.OutputTable, yson_format="pretty", on_read=tables.OnRead(row_transformer=output_row_transformer)),
                [tests.Diff(), tests.SchemaEquals(schema)],
            ),
        ],
        env=None,
    )
