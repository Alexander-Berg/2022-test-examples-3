import os

import yatest.common
import yt.wrapper as yt

from crypta.lib.python.yt import schema_utils
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)
from crypta.profile.runners.log_parsing.lib import metrics
from crypta.profile.utils.config import config


def get_metrics_schema():
    return schema_utils.yt_schema_from_dict({
        field: "string"
        for field in ("browserinfo", "clientip", "cookiei", "counterclass", "counterid", "eventtime", "params", "referer", "regionid", "uniqid", "url")
    })


def test_metrics(local_yt, udf_patched_config):
    input_dir = yatest.common.test_source_path('data/metrics')
    date = "2021-08-30"
    task = metrics.MetricsParser()

    return tests.yt_test_func(
        yt_client=local_yt.get_yt_client(),
        func=task.run,
        data_path=input_dir,
        input_tables=[
            (tables.get_yson_table_with_schema(
                filename,
                yt.ypath_join(config.METRICS_LOG_DIRECTORY, filename),
                schema=get_metrics_schema(),
            ), tests.TableIsNotChanged())
            for filename in sorted(os.listdir(input_dir))
        ],
        output_tables=[
            (tables.YsonTable(date, yt.ypath_join(config.TESTING_PARSED_LOGS_DIRECTORY, 'metrics', date), yson_format='pretty'), tests.Diff())
        ],
    )
