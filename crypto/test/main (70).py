import yatest.common
from yt.wrapper import ypath

from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)
from crypta.s2s.lib import schemas


def test_process_conversions(local_yt, local_yt_and_yql_env, frozen_time, config, config_file):
    diff_test = tests.Diff()
    conversion_schema = schemas.get_conversion_schema()
    conversion_state_schema = schemas.get_conversion_state_schema()
    input_table_names = ("1500000000", "1500086400")
    return tests.yt_test(
        yt_client=local_yt.get_yt_client(),
        binary=yatest.common.binary_path("crypta/s2s/services/process_conversions/bin/crypta-s2s-process-conversions"),
        args=[
            "--config", config_file,
        ],
        data_path=yatest.common.test_source_path("data"),
        input_tables=[
            (tables.get_yson_table_with_schema("{}.yson".format(table_name), ypath.ypath_join(config.FreshConversionsDir, table_name), conversion_schema), [tests.IsAbsent()])
            for table_name in input_table_names
        ] + [
            (tables.get_yson_table_with_schema("state.yson", config.ConversionsStateTable, conversion_state_schema), []),
        ],
        output_tables=[
            (tables.YsonTable("to_postback_{}.yson".format(table_name), ypath.ypath_join(config.ToPostbackDir, table_name), yson_format="pretty"), [diff_test, tests.SchemaEquals(conversion_schema)])
            for table_name in input_table_names
        ] + [
            (tables.YsonTable("output_state.yson", config.ConversionsStateTable, yson_format="pretty"), [diff_test, tests.SchemaEquals(conversion_state_schema)]),
        ],
        env=local_yt_and_yql_env,
    )
