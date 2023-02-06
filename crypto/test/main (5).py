import datetime

import pytest
import yatest.common
import yt.wrapper as yt

from crypta.lib.python import (
    time_utils,
    yaml_config,
)
from crypta.cm.offline.bin.common.python import (
    config_fields,
    match,
)
from crypta.cm.offline.bin.parse_cm_access_log.lib.python import errors
from crypta.lib.python.yt.test_helpers import (
    row_transformers,
    tables,
    tests,
)


INPUT_DIR = "//input"
OUTPUT_DIR = "//output"
ERRORS_DIR = "//errors"
TRACK_TABLE = "//track"
ERRORS_TTL_DAYS = 1
CRYPTA_FROZEN_TIME = "1500000000"


@pytest.fixture(scope="function")
def config(yt_stuff):
    return {
        config_fields.YT_PROXY: yt_stuff.get_server(),
        config_fields.YT_POOL: "pool",
        config_fields.CM_ACCESS_LOG_DIR: INPUT_DIR,
        config_fields.CM_ACCESS_LOG_PARSER_ERRORS_DIR: ERRORS_DIR,
        config_fields.CM_ACCESS_LOG_TRACK_TABLE: TRACK_TABLE,
        config_fields.FRESH_CM_DIR: OUTPUT_DIR,
        config_fields.TABLE_PACK_SIZE: 4,
        config_fields.ERRORS_TTL_DAYS: ERRORS_TTL_DAYS,
    }


def test_parse_cm_access_log(yt_stuff, config):
    diff = tests.Diff()
    errors_tests = [
        diff,
        tests.SchemaEquals(errors.get_schema()),
        tests.ExpirationTime(datetime.timedelta(days=ERRORS_TTL_DAYS)),
    ]
    errors_table = tables.YsonTable(
        "output_errors.yson",
        yt.ypath_join(ERRORS_DIR, CRYPTA_FROZEN_TIME),
        yson_format="pretty",
        on_read=tables.OnRead(row_transformer=row_transformers.remove_frame_info(field="what")),
    )

    return tests.yt_test(
        yt_client=yt_stuff.get_yt_client(),
        binary=yatest.common.binary_path("crypta/cm/offline/bin/parse_cm_access_log/bin/crypta-offline-cm-parse-cm-access-log"),
        args=["--config", yaml_config.dump(config)],
        data_path=yatest.common.test_source_path("data"),
        input_tables=[
            (tables.YsonTable("1300000000.yson", yt.ypath_join(INPUT_DIR, "1300000000")), tests.TableIsNotChanged()),
            (tables.YsonTable("1400000000.yson", yt.ypath_join(INPUT_DIR, "1400000000")), tests.TableIsNotChanged()),
            (tables.YsonTable("track.yson", TRACK_TABLE), None),
        ],
        output_tables=[
            (tables.YsonTable("output.yson", yt.ypath_join(OUTPUT_DIR, CRYPTA_FROZEN_TIME), yson_format="pretty"), [diff, tests.SchemaEquals(match.get_unsorted_schema())]),
            (tables.YsonTable("output_track.yson", TRACK_TABLE, yson_format="pretty"), diff),
            (errors_table, errors_tests),
        ],
        env={
            time_utils.CRYPTA_FROZEN_TIME_ENV: CRYPTA_FROZEN_TIME
        },
    )
