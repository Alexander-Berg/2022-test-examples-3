import datetime

import yatest.common
from yt.wrapper import ypath

from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)
from crypta.s2s.lib import schemas


def test_upload_to_postback(yt_stuff, mock_postback_server, config, config_file):
    table_name = "1500000000"
    diff_test = tests.Diff()
    output_files = tests.yt_test(
        yt_client=yt_stuff.get_yt_client(),
        binary=yatest.common.binary_path("crypta/s2s/services/upload_to_postback/bin/crypta-s2s-upload-to-postback"),
        args=[
            "--config", config_file,
        ],
        data_path=yatest.common.test_source_path("data"),
        input_tables=[
            (tables.get_yson_table_with_schema("to_postback_1500000000.yson", ypath.ypath_join(config.ToPostbackDir, table_name), schemas.get_conversion_schema()), [tests.IsAbsent()]),
        ],
        output_tables=[
            (
                tables.get_yson_table_with_schema("backup.yson", ypath.ypath_join(config.ToPostbackBackup.Dir, table_name), schemas.get_conversion_schema()),
                [
                    diff_test,
                    tests.SchemaEquals(schemas.get_conversion_schema()),
                    tests.ExpirationTime(ttl=datetime.timedelta(days=config.ToPostbackBackup.TtlDays)),
                ],
            ),
            (
                tables.YsonTable("errors.yson", ypath.ypath_join(config.ToPostbackErrors.Dir, table_name), yson_format="pretty"),
                [
                    diff_test,
                    tests.SchemaEquals(schemas.get_conversion_upload_error_schema()),
                    tests.ExpirationTime(ttl=datetime.timedelta(days=config.ToPostbackErrors.TtlDays)),
                ],
            ),
        ],
    )

    return {
        "yt": output_files,
        "postback_requests": sorted(mock_postback_server.requests, key=lambda x: x["reqid"]),
    }
