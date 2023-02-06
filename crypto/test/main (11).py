import pytest
import datetime

import yatest.common
import yt.wrapper as yt
from yt.wrapper import yson

from crypta.dmp.adobe.bin.common.python import config_fields
from crypta.lib.python import (
    time_utils,
    yaml_config,
)
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests
)


TTL_IN_DAYS = 30
INPUT_DIR = "//dmp/input"
OUTPUT_DIR = "//dmp/output"
ERRORS_DIR = "//dmp/errors"


@pytest.fixture(scope="function")
def config(yt_stuff):
    return {
        config_fields.YT_PROXY: yt_stuff.get_server(),
        config_fields.YT_POOL: "yt-pool",
        config_fields.RAW_EXT_ID_BINDINGS_DIR: INPUT_DIR,
        config_fields.PARSED_EXT_ID_BINDINGS_DIR: OUTPUT_DIR,
        config_fields.PARSE_BINDINGS_ERRORS_DIR: ERRORS_DIR,
        config_fields.ERRORS_TTL_DAYS: TTL_IN_DAYS,
        config_fields.SHOULD_DROP_INPUT: True
    }


def get_upload_attributes(sync_mode, timestamp):
    return {"upload":
                {
                    "sync_mode": sync_mode,
                    "timestamp": timestamp
                }
            }


def test_parse_raw_bindings(yt_stuff, config):
    config_path = yaml_config.dump(config)

    iter_tables_attrs = get_upload_attributes("iter", yson.YsonUint64(1111111111000))
    full_tables_attrs = get_upload_attributes("full", yson.YsonUint64(1111111112000))

    frozen_time = "1000000500"
    return tests.yt_test(
        yt_client=yt_stuff.get_yt_client(),
        binary=yatest.common.binary_path("crypta/dmp/adobe/bin/parse_raw_bindings/bin/crypta-adobe-parse-raw-bindings"),
        args=[
            "--config", config_path
        ],
        data_path=yatest.common.test_source_path("data"),
        input_tables=[
            (tables.YsonTable("raw_iter_bindings", yt.ypath_join(INPUT_DIR, "1111111111000_1000000000"), on_write=tables.OnWrite(attributes=iter_tables_attrs)),
                              tests.IsAbsent()),
            (tables.YsonTable("raw_full_bindings", yt.ypath_join(INPUT_DIR, "1111111112000_1000000001"), on_write=tables.OnWrite(attributes=full_tables_attrs)),
                              tests.IsAbsent()),
        ],
        output_tables=[
            (tables.YsonTable("parsed_iter_bindings", yt.ypath_join(OUTPUT_DIR, "1111111111000_{}".format(frozen_time))),
                              [tests.Diff()]),
            (tables.YsonTable("parsed_full_bindings", yt.ypath_join(OUTPUT_DIR, "1111111112000_{}".format(frozen_time))),
                              [tests.Diff()]),
            (tables.YsonTable("iter_errors", yt.ypath_join(ERRORS_DIR, "1111111111000_{}".format(frozen_time))),
                              [tests.Diff(), tests.ExpirationTime(datetime.timedelta(days=TTL_IN_DAYS))]),
            (tables.YsonTable("full_errors", yt.ypath_join(ERRORS_DIR, "1111111112000_{}".format(frozen_time))),
                              [tests.Diff(), tests.ExpirationTime(datetime.timedelta(days=TTL_IN_DAYS))])
        ],
        env={
            "YT_TOKEN": "unused",
            time_utils.CRYPTA_FROZEN_TIME_ENV: frozen_time
        }
    )
