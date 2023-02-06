import datetime

import pytest
import yatest.common
import yt.wrapper as yt

from crypta.cm.services.common.db_sync import (
    errors_schema,
    to_identify_schema,
)
from crypta.lib.python import (
    time_utils,
    yaml_config,
)
from crypta.lib.python.yt.test_helpers import (
    row_transformers,
    tables,
    tests,
)


SOURCE_DIR = "//source"
TO_IDENTIFY_DIR = "//to_identify"
TRACK_TABLE = "//track"
ERRORS_DIR = "//errors"
ERRORS_TTL_DAYS = 1
CRYPTA_FROZEN_TIME = "1500000000"


@pytest.fixture(scope="function")
def config(yt_stuff):
    return {
        "yt_proxy": yt_stuff.get_server(),
        "yt_pool": "pool",
        "source_dir": SOURCE_DIR,
        "to_identify_dir": TO_IDENTIFY_DIR,
        "track_table": TRACK_TABLE,
        "errors_dir": ERRORS_DIR,
        "errors_ttl_days": ERRORS_TTL_DAYS,
        "max_tables_to_process": 7,
        "sampler_denominator": 11,
        "sampler_rest": 4,
    }


def test_basic(yt_stuff, config):
    diff = tests.Diff()

    errors_test = [
        diff,
        tests.ExpirationTime(datetime.timedelta(days=ERRORS_TTL_DAYS)),
        tests.SchemaEquals(errors_schema.get()),
    ]
    errors_table = tables.YsonTable(
        "errors.yson",
        yt.ypath_join(ERRORS_DIR, CRYPTA_FROZEN_TIME),
        yson_format="pretty",
        on_read=tables.OnRead(row_transformer=row_transformers.remove_frame_info(field="what")),
    )

    return tests.yt_test(
        yt_client=yt_stuff.get_yt_client(),
        binary=yatest.common.binary_path("crypta/cm/services/prepare_to_sync_db/bin/crypta-cm-prepare-to-sync-db"),
        args=[
            "--config", yaml_config.dump(config)
        ],
        data_path=yatest.common.test_source_path("data"),
        input_tables=[
            (tables.YsonTable("input1.yson", yt.ypath_join(SOURCE_DIR, "2019-06-11T00:00:00")), tests.TableIsNotChanged()),
            (tables.YsonTable("input2.yson", yt.ypath_join(SOURCE_DIR, "2019-06-12T00:00:00")), tests.TableIsNotChanged()),
            (tables.YsonTable("input3.yson", yt.ypath_join(SOURCE_DIR, "2019-06-13T00:00:00")), tests.TableIsNotChanged()),
        ],
        output_tables=[
            (tables.YsonTable("output_track.yson", TRACK_TABLE, yson_format="pretty"), diff),
            (tables.YsonTable("to_identify.yson", yt.ypath_join(TO_IDENTIFY_DIR, CRYPTA_FROZEN_TIME), yson_format="pretty"), [diff, tests.SchemaEquals(to_identify_schema.get())]),
            (errors_table, errors_test),
        ],
        env={
            time_utils.CRYPTA_FROZEN_TIME_ENV: CRYPTA_FROZEN_TIME,
        },
    )
