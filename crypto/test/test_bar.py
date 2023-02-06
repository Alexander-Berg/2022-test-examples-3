import os

import yatest.common
import yt.wrapper as yt

from crypta.lib.python.yt import schema_utils
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)
from crypta.profile.runners.log_parsing.lib import bar
from crypta.profile.utils.config import config


def get_bar_schema():
    return schema_utils.yt_schema_from_dict({
        field: "string"
        for field in ("timestamp", "yasoft", "ip", "http_params", "unixtime", "icookie", "yandexuid")
    })


def test_bar(local_yt, udf_patched_config):
    input_dir = yatest.common.test_source_path('data/bar')
    date = "2021-08-28"
    task = bar.BarParser()

    return tests.yt_test_func(
        yt_client=local_yt.get_yt_client(),
        func=task.run,
        data_path=input_dir,
        input_tables=[
            (tables.get_yson_table_with_schema(
                filename,
                yt.ypath_join(config.BAR_LOG_DIRECTORY, filename),
                schema=get_bar_schema(),
            ), tests.TableIsNotChanged())
            for filename in sorted(os.listdir(input_dir))
        ],
        output_tables=[
            (tables.YsonTable(date, yt.ypath_join(config.TESTING_PARSED_LOGS_DIRECTORY, 'bar', date), yson_format='pretty'), tests.Diff())
        ],
    )
