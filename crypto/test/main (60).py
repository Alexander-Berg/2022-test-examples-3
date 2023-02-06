import logging

import yatest.common
import yt.wrapper as yt

from crypta.lib.python.yt import schema_utils
from crypta.lib.python.yt.test_helpers import (
    files,
    tables,
    tests,
)
from crypta.ltp.viewer.lib import ltp_logs
import crypta.ltp.viewer.lib.compact_index.py.pack_index as index_packer
from crypta.ltp.viewer.proto import index_pb2


logger = logging.getLogger(__name__)


def get_index_schema():
    return schema_utils.yt_schema_from_dict(
        {
            "id": "string",
            "id_type": "string",
            "sources": "string",
        },
        ["id", "id_type"]
    )


def get_log_schema():
    return schema_utils.yt_schema_from_dict(
        {
            "id": "string",
            "id_type": "string",
        },
        ["id", "id_type"]
    )


def pack_index(row):
    row["sources"] = index_packer.pack(row["sources"])
    return row


def unpack_index(row):
    row["sources"] = index_packer.unpack(yt.yson.get_bytes(row["sources"]))
    return row


def test_build_index(local_yt, local_yt_and_yql_env):
    start_date = "2020-10-10"
    end_date = "2020-10-20"
    index_path = "//index_path_v2"
    yt_client = local_yt.get_yt_client()
    min_history_date = "2020-10-09"
    max_history_date = "2020-10-15"

    local_udf_path = yatest.common.binary_path("yql/udfs/crypta/ltp/libcrypta_ltp_viewer_udf.so")
    yt_udf_path = "libcrypta_ltp_viewer_udf.so"

    local_yt.get_yt_client().create("medium", attributes={"name": "ssd_blobs"})

    for log in ltp_logs.LOGS:
        yt_client.mkdir(log.path, recursive=True)

    return tests.yt_test(
        yt_client=yt_client,
        binary=yatest.common.binary_path("crypta/ltp/viewer/services/build_index/bin/main"),
        args=[
            "--start-date", start_date,
            "--end-date", end_date,
            "--min-history-date", min_history_date,
            "--index-path", index_path,
            "--crypta-ltp-viewer-udf-url", "yt://plato/{}".format(yt_udf_path),
        ],
        data_path=yatest.common.test_source_path("data"),
        input_tables=[
            (tables.YsonTable(
                'index.yson',
                index_path,
                on_write=tables.OnWrite(
                    attributes={
                        "schema": get_index_schema(),
                        "_min_history_date": min_history_date,
                        "_max_history_date": max_history_date,
                    },
                    row_transformer=pack_index,
                ),
            ), []),
            (tables.get_yson_table_with_schema(
                'start_date.yson',
                yt.ypath_join(ltp_logs.LOGS_DICT[index_pb2.LtpWatch].path, start_date),
                schema=get_log_schema(),
            ), [tests.TableIsNotChanged()]),
            (tables.get_yson_table_with_schema(
                'end_date.yson',
                yt.ypath_join(ltp_logs.LOGS_DICT[index_pb2.LtpVisitStates].path, end_date),
                schema=get_log_schema(),
            ), [tests.TableIsNotChanged()]),
            (tables.get_yson_table_with_schema(
                'min_history_date.yson',
                yt.ypath_join(ltp_logs.LOGS_DICT[index_pb2.UserSessionQueries].path, min_history_date),
                schema=get_log_schema(),
            ), [tests.TableIsNotChanged()]),
            (files.YtFile(
                local_udf_path,
                yt_udf_path,
            ), [])
        ],
        output_tables=[
            (tables.YsonTable(
                'index.yson',
                index_path,
                on_read=tables.OnRead(
                    row_transformer=unpack_index,
                ),
            ), [tests.Diff()]),
        ],
        env=local_yt_and_yql_env,
    )
