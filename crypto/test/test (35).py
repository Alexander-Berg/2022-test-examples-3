import yatest.common
import yt.wrapper as yt

from crypta.lib.python.yt import schema_utils
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)
from crypta.ltp.viewer.lib.test_helpers import transformers


def get_index_schema():
    return schema_utils.yt_schema_from_dict({
        column: "string"
        for column in ("id", "id_type", "sources")
    })


def get_ltp_browser_url_title_schema():
    return schema_utils.yt_schema_from_dict({
        "id": "string",
        "id_type": "string",
        "ActionTimestamp": "int64",
        "EventName": "string",
        "Title": "string",
        "Yasoft": "string",
        "APIKey": "int64",
    })


def test_get_logs_from_index(clean_local_yt_with_chyt, client, yuid, crypta_id):
    index_on_write = tables.OnWrite(
        attributes={"schema": get_index_schema()},
        sort_by=["id", "id_type"],
        row_transformer=transformers.index_row_transformer,
    )
    index_path = "//index"
    return tests.yt_test_func(
        clean_local_yt_with_chyt.get_yt_client(),
        lambda: {":".join(key): value for key, value in client.get_logs_from_index([yuid, crypta_id], index_path).items()},
        data_path=yatest.common.test_source_path("data"),
        input_tables=[
            (tables.YsonTable(
                "index.yson",
                index_path,
                on_write=index_on_write,
            ), tests.TableIsNotChanged()),
        ],
        return_result=True,
    )


def test_get_entries_from_logs(clean_local_yt_with_chyt, client, yuid):
    ltp_on_write = tables.OnWrite(
        attributes={"schema": get_ltp_browser_url_title_schema()},
        sort_by=["id", "id_type", "ActionTimestamp"],
    )
    log_path = "//logs"
    date = "2021-10-20"

    columns = [
        "EventName",
        "Title",
        "Yasoft",
        "APIKey",
    ]

    return tests.yt_test_func(
        clean_local_yt_with_chyt.get_yt_client(),
        lambda: list(client.get_entries_from_logs(yuid, log_path, date, columns, {column: column for column in columns})),
        data_path=yatest.common.test_source_path("data"),
        input_tables=[
            (tables.YsonTable(
                "ltp_browser_url_title.yson",
                yt.ypath_join(log_path, date),
                on_write=ltp_on_write,
            ), tests.TableIsNotChanged()),
        ],
        return_result=True,
    )
