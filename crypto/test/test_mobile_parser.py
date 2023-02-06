import os

import yatest.common
import yt.wrapper as yt

from crypta.lib.python.yt import schema_utils
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)
from crypta.profile.runners.log_parsing.lib import mobile
from crypta.profile.utils.config import config


def test_basic(local_yt, udf_patched_config):
    input_dir = yatest.common.test_source_path('data/mobile/input')
    date = "2021-04-27"
    log_name = 'mobile_bar'
    task = mobile.MobileParser(log_name=log_name, log_dir=config.BROWSER_METRIKA_MOBILE_LOG_5MIN)

    return tests.yt_test_func(
        yt_client=local_yt.get_yt_client(),
        func=task.run,
        data_path=input_dir,
        input_tables=[
            (tables.get_yson_table_with_schema(
                filename,
                yt.ypath_join(config.BROWSER_METRIKA_MOBILE_LOG_5MIN, filename),
                schema=schema_utils.yt_schema_from_dict(
                    {
                        "DeviceID": "string",
                        "CryptaID": "uint64",
                        "EventTimestamp": "uint64",
                        "EventName": "string",
                        "EventValue": "string",
                    }
                )
            ), tests.TableIsNotChanged())
            for filename in sorted(os.listdir(input_dir))
        ],
        output_tables=[
            (tables.YsonTable(date, yt.ypath_join(config.TESTING_PARSED_LOGS_DIRECTORY, log_name, date), yson_format='pretty'), tests.Diff())
        ],
    )
