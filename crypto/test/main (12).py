import pytest
import yatest.common
import yt.wrapper as yt

from crypta.dmp.adobe.bin.common.python import config_fields
from crypta.dmp.adobe.bin.prepare_bindings_to_upload.lib.python import (
    prepare_bindings_to_upload_errors,
)
from crypta.dmp.common.data.python import (
    bb_upload_state,
    bindings,
    collector,
    meta,
)
from crypta.lib.python import (
    time_utils,
    yaml_config,
)
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)


YANDEXUID_BINDINGS_TABLE = "//adobe/yandexuid_bindings"
BB_UPLOAD_STATE_TABLE = "//adobe/bb_state"
META_TABLE = "//adobe/meta"
ERRORS_DIR = "//errors"
TO_BB_COLLECTOR_DIR = "//adobe/to_bb_collector"
FROZEN_TIME = "1400005000"
DMP_ID = 1

PARAMETRIZE_ARGS = "meta_filename,yandexuid_bindings_filename,bb_state_filename,bb_upload_enabled"

ZERO_RC_TEST_CASES = {
    "MetaExists__YandexuidBindingsExists__BbUploadStateIsMissing__UploadEnabled": (
        "meta.yson", "yandexuid_bindings.yson", None, True
    ),
    "MetaExists__YandexuidBindingsExists__BbUploadStateIsMissing__UploadDisabled": (
        "meta.yson", "yandexuid_bindings.yson", None, False
    ),
    "MetaExists__YandexuidBindingsExists__BbUploadStateExists__UploadEnabled": (
        "meta.yson", "yandexuid_bindings.yson", "bb_state.yson", True
    ),
    "MetaExists__YandexuidBindingsExists__BbUploadStateExists__UploadDisabled": (
        "meta.yson", "yandexuid_bindings.yson", "bb_state.yson", False
    )
}

NON_ZERO_RC_TEST_CASES = {
    "MetaIsMissing__YandexuidBindingsExists__BbUploadStateExists_UploadEnabled": (
        None, "yandexuid_bindings.yson", "bb_state.yson", True
    ),
    "MetaExists__YandexuidBindingsIsMissing__BbUploadStateExists_UploadEnabled": (
        "meta.yson", None, "bb_state.yson", True
    ),
    "MetaIsMissing__YandexuidBindingsIsMissing__BbUploadStateExists_UploadEnabled": (
        None, None, "bb_state.yson", True
    ),
    "MetaIsMissing__YandexuidBindingsIsMissing__BbUploadStateIsMissing_UploadEnabled": (
        None, None, None, True
    ),
    "MetaIsMissing__YandexuidBindingsExists__BbUploadStateIsMissing_UploadEnabled": (
        None, "yandexuid_bindings.yson", None, True
    ),
    "MetaExists__YandexuidBindingsIsMissing__BbUploadStateIsMissing_UploadEnabled": (
        "meta.yson", None, None, True
    )
}


@pytest.fixture(scope="function")
def config(yt_stuff, bb_upload_enabled):
    return {
        config_fields.YT_PROXY: yt_stuff.get_server(),
        config_fields.YT_POOL: "yt_pool",

        config_fields.META_TABLE: META_TABLE,
        config_fields.FILTERED_YANDEXUID_BINDINGS_TABLE: YANDEXUID_BINDINGS_TABLE,
        config_fields.BB_COLLECTOR_FRESH_DIR: TO_BB_COLLECTOR_DIR,
        config_fields.BB_UPLOAD_ENABLED: bb_upload_enabled,
        config_fields.BB_UPLOAD_TTL_DAYS: 2000,
        config_fields.BB_UPLOAD_STATE_TABLE: BB_UPLOAD_STATE_TABLE,
        config_fields.BB_UPLOAD_DMP_ID: DMP_ID,

        config_fields.PREPARE_BINDINGS_TO_UPLOAD_ERRORS_DIR: ERRORS_DIR,
        config_fields.ERRORS_TTL_DAYS: 1
    }


def get_input_tables(meta_filename, yandexuid_bindings_filename, bb_state_filename):
    input_tables = []
    if meta_filename is not None:
        on_write = tables.OnWrite(attributes={"schema": meta.get_schema()})
        table = tables.YsonTable(meta_filename, META_TABLE, on_write=on_write)
        input_tables.append((table, tests.TableIsNotChanged()))
    if yandexuid_bindings_filename is not None:
        on_write = tables.OnWrite(attributes={"schema": bindings.get_yandexuid_schema()})
        table = tables.YsonTable(yandexuid_bindings_filename, YANDEXUID_BINDINGS_TABLE, on_write=on_write)
        input_tables.append((table, tests.TableIsNotChanged()))
    if bb_state_filename is not None:
        on_write = tables.OnWrite(attributes={"schema": bb_upload_state.get_schema()})
        table = tables.YsonTable(bb_state_filename, BB_UPLOAD_STATE_TABLE, on_write=on_write)
        input_tables.append((table, None))
    return input_tables


def get_output_tables(bb_upload_enabled):
    diff = tests.Diff()
    output_tables = [
        (
            tables.YsonTable("errors", yt.ypath_join(ERRORS_DIR, FROZEN_TIME), yson_format="pretty"),
            [diff, tests.SchemaEquals(prepare_bindings_to_upload_errors.get_schema())]
        ),
        (
            tables.YsonTable("bb_state", BB_UPLOAD_STATE_TABLE, yson_format="pretty"),
            [diff, tests.SchemaEquals(bb_upload_state.get_schema())]
        ),
        (
            tables.YsonTable("bb_collector_fresh", yt.ypath_join(TO_BB_COLLECTOR_DIR, "{}_{}".format(DMP_ID, FROZEN_TIME)), yson_format="pretty", on_read=collector.on_read()),
            [diff, tests.SchemaEquals(collector.get_schema())] if bb_upload_enabled else tests.IsAbsent()
        ),
    ]
    return output_tables


def execute(yt_stuff, config, meta_filename, yandexuid_bindings_filename, bb_state_filename):
    return tests.yt_test(
        yt_client=yt_stuff.get_yt_client(),
        binary=yatest.common.binary_path("crypta/dmp/adobe/bin/prepare_bindings_to_upload/bin/crypta-adobe-prepare-bindings-to-upload"),
        args=[
            "--config", yaml_config.dump(config)
        ],
        data_path=yatest.common.test_source_path("data"),
        input_tables=get_input_tables(meta_filename, yandexuid_bindings_filename, bb_state_filename),
        output_tables=get_output_tables(config[config_fields.BB_UPLOAD_ENABLED]),
        env={
            time_utils.CRYPTA_FROZEN_TIME_ENV: str(FROZEN_TIME)
        }
    )


@pytest.mark.parametrize(PARAMETRIZE_ARGS, ZERO_RC_TEST_CASES.values(), ids=ZERO_RC_TEST_CASES.keys())
def test_zero_rc(yt_stuff, config, meta_filename, yandexuid_bindings_filename, bb_state_filename):
    return execute(yt_stuff, config, meta_filename, yandexuid_bindings_filename, bb_state_filename)


@pytest.mark.parametrize(PARAMETRIZE_ARGS, NON_ZERO_RC_TEST_CASES.values(), ids=NON_ZERO_RC_TEST_CASES.keys())
def test_nonzero_rc(yt_stuff, config, meta_filename, yandexuid_bindings_filename, bb_state_filename):
    with pytest.raises(yatest.common.ExecutionError):
        execute(yt_stuff, config, meta_filename, yandexuid_bindings_filename, bb_state_filename)
