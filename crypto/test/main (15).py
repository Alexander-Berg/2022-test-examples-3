import datetime
import pytest

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


ITER_SYNC_MODE = "iter"
FULL_SYNC_MODE = "full"


TTL_IN_DAYS = 30
FROZEN_TIME = "1000005000"
EXT_ID_BINDINGS_TABLE = "//dmp/parsed_bindings"
ERRORS_DIR = "//dmp/errors"
PARSED_EXT_ID_BINDINGS_DIR = "//dmp/fresh_parsed_bindings"


BASIC_CONFIG = {
    config_fields.YT_POOL: "yt-pool",

    config_fields.PARSED_EXT_ID_BINDINGS_DIR: PARSED_EXT_ID_BINDINGS_DIR,
    config_fields.EXT_ID_BINDINGS_TABLE: EXT_ID_BINDINGS_TABLE,
    config_fields.UPDATE_BINDINGS_ERRORS_DIR: ERRORS_DIR,

    config_fields.ERRORS_TTL_DAYS: TTL_IN_DAYS,
    config_fields.SHOULD_DROP_INPUT: True
}


@pytest.fixture(scope="function")
def config(yt_stuff):
    res = {config_fields.YT_PROXY: yt_stuff.get_server()}
    res.update(BASIC_CONFIG)
    return res


def get_upload_attributes(sync_mode, timestamp):
    return {"upload": {"sync_mode": sync_mode,
                       "timestamp": yson.YsonUint64(timestamp)}}


def get_last_upload_ts_attributes(timestamp):
    return {"last_upload_timestamp": yson.YsonUint64(timestamp)}


def get_input_state_table_test(last_upload_timestamp):
    return tables.YsonTable("bindings_state", EXT_ID_BINDINGS_TABLE, on_write=tables.OnWrite(sort_by=["id"], attributes=get_last_upload_ts_attributes(last_upload_timestamp))), None


def get_output_state_table_test(last_upload_timestamp):
    return tables.YsonTable("bindings_state", EXT_ID_BINDINGS_TABLE), [tests.Diff(),
                                                                       tests.AttrsEquals(get_last_upload_ts_attributes(last_upload_timestamp))]


def get_parsed_bindings_table_test(resource_name, sync_mode, upload_timestamp):
    return tables.YsonTable(resource_name, yt.ypath_join(PARSED_EXT_ID_BINDINGS_DIR, "{}_1000000000".format(upload_timestamp)),
                            on_write=tables.OnWrite(attributes=get_upload_attributes(sync_mode, upload_timestamp))), tests.IsAbsent()


def get_errors_table(out_name, upload_timestamp):
    return tables.YsonTable(out_name, yt.ypath_join(BASIC_CONFIG[config_fields.UPDATE_BINDINGS_ERRORS_DIR], "{}_{}".format(upload_timestamp, FROZEN_TIME)))


def get_iter_errors_table_test(out_name, upload_timestamp):
    return get_errors_table(out_name, upload_timestamp), [tests.Diff(),
                                                          tests.AttrsEquals(get_upload_attributes(ITER_SYNC_MODE, upload_timestamp)),
                                                          tests.ExpirationTime(datetime.timedelta(days=TTL_IN_DAYS))]


def get_absent_errors_table_test(out_name, upload_timestamp):
    return get_errors_table(out_name, upload_timestamp), tests.IsAbsent()


TEST_CASES = {
    "one_iter_table_and_state": (
        [
            get_parsed_bindings_table_test("parsed_iter_bindings_1", ITER_SYNC_MODE, 1111111111001),
            get_input_state_table_test(1111111111000)
        ],
        [
            get_output_state_table_test(1111111111001),
            get_iter_errors_table_test("errors_iter_1", 1111111111001)
        ]
    ),
    "one_full_table_and_state": (
        [
            get_parsed_bindings_table_test("parsed_full_bindings", FULL_SYNC_MODE, 1111111111001),
            get_input_state_table_test(1111111111000)
        ],
        [
            get_output_state_table_test(1111111111001),
            get_absent_errors_table_test("errors_full", 1111111111001)
        ]
    ),
    "one_iter_table_no_state": (
        [
            get_parsed_bindings_table_test("parsed_iter_bindings_1", ITER_SYNC_MODE, 1111111111001)
        ],
        [
            get_output_state_table_test(1111111111001),
            get_iter_errors_table_test("errors_iter_1", 1111111111001)
        ]
    ),
    "one_full_table_no_state": (
        [
            get_parsed_bindings_table_test("parsed_full_bindings", FULL_SYNC_MODE, 1111111111001)
        ],
        [
            get_output_state_table_test(1111111111001),
            get_absent_errors_table_test("errors_full", 1111111111001)
        ]
    ),
    "one_old_iter_table_and_state": (
        [
            get_parsed_bindings_table_test("parsed_iter_bindings_1", ITER_SYNC_MODE, 1111111111000),
            get_input_state_table_test(1111111111001)
        ],
        [
            get_output_state_table_test(1111111111001),
            get_absent_errors_table_test("errors_iter_1", 1111111111000)
        ]
    ),
    "one_old_full_table_and_state": (
        [
            get_parsed_bindings_table_test("parsed_full_bindings", FULL_SYNC_MODE, 1111111111000),
            get_input_state_table_test(1111111111001)
        ],
        [
            get_output_state_table_test(1111111111001),
            get_absent_errors_table_test("errors_full", 1111111111000)
        ]
    ),
    "two_iter_tables_and_state": (
        [
            get_parsed_bindings_table_test("parsed_iter_bindings_1", ITER_SYNC_MODE, 1111111111001),
            get_parsed_bindings_table_test("parsed_iter_bindings_2", ITER_SYNC_MODE, 1111111111002),
            get_input_state_table_test(1111111111000)
        ],
        [
            get_output_state_table_test(1111111111002),
            get_iter_errors_table_test("errors_iter_1", 1111111111001),
            get_iter_errors_table_test("errors_iter_2", 1111111111002)
        ]
    ),
    "one_full_one_iter_tables_and_state": (
        [
            get_parsed_bindings_table_test("parsed_full_bindings", FULL_SYNC_MODE, 1111111111001),
            get_parsed_bindings_table_test("parsed_iter_bindings_2", ITER_SYNC_MODE, 1111111111002),
            get_input_state_table_test(1111111111000)
        ],
        [
            get_output_state_table_test(1111111111002),
            get_absent_errors_table_test("errors_full", 1111111111001),
            get_iter_errors_table_test("errors_iter_2", 1111111111002)
        ]
    )
}


@pytest.mark.parametrize(
    "input_tables,output_tables",
    TEST_CASES.values(),
    ids=TEST_CASES.keys())
def test_update_bindings(yt_stuff, config, input_tables, output_tables):
    config_path = yaml_config.dump(config)

    return tests.yt_test(
        yt_client=yt_stuff.get_yt_client(),
        binary=yatest.common.binary_path("crypta/dmp/adobe/bin/update_bindings/bin/crypta-adobe-update-bindings"),
        args=[
            "--config", config_path
        ],
        data_path=yatest.common.test_source_path("data"),
        input_tables=input_tables,
        output_tables=output_tables,
        env={
            "YT_TOKEN": "unused",
            time_utils.CRYPTA_FROZEN_TIME_ENV: FROZEN_TIME
        }
    )
