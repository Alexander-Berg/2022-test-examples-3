import pytest
import yatest
import yt.wrapper as yt

from crypta.lib.python import templater
from crypta.lib.python.yt import schema_utils
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)

LOGS_DIR = "//logs_dir"
YT_WORKING_DIR = "//home/crypta/qa"
OUTPUT_DIR = yt.ypath_join(YT_WORKING_DIR, "cookie_matching/offline/touch/to_touch")


def get_log_schema():
    return schema_utils.yt_schema_from_dict({
        "query": "string",
        "reply_body": "string",
        "http_code": "uint64",
        "unixtime": "uint64",
    })


@pytest.fixture
def config_path(local_yt):
    context = {
        "environment": "qa",
        "yt_working_dir": "//home/crypta/qa",
        "logs_dir": LOGS_DIR,
        "yt_proxy": local_yt.get_server(),
    }
    output_path = "config.yaml"
    templater.render_file(
        yatest.common.source_path("crypta/cm/services/prepare_touch/bundle/config.yaml"),
        output_path,
        context,
        strict=True,
    )
    return output_path


def test_cm_prepare_touch(local_yt, config_path, local_yt_and_yql_env):
    result = tests.yt_test(
        yt_client=local_yt.get_yt_client(),
        binary=yatest.common.binary_path("crypta/cm/services/prepare_touch/bin/crypta-cm-prepare-touch"),
        args=[
            "--config", config_path
        ],
        data_path=yatest.common.test_source_path("data"),
        input_tables=[
            (tables.get_yson_table_with_schema("log.yson", yt.ypath_join(LOGS_DIR, "2022-01-30"), get_log_schema()), tests.TableIsNotChanged()),
        ],
        output_tables=[
            (tables.YsonTable("output.yson", yt.ypath_join(OUTPUT_DIR, "2022-01-30")), tests.Diff()),
        ],
        env=local_yt_and_yql_env,
    )

    return result
