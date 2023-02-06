import pytest
import yatest.common

from crypta.dmp.yandex.bin.common.python import config_fields
from crypta.dmp.common.data.python import (
    collector,
    bb_upload_state,
    bindings,
)
from crypta.lib.python import (
    time_utils,
    yaml_config,
)
from crypta.lib.python.yt.test_helpers import (
    cypress,
    tables,
    tests
)


DMP_ID = 11
YANDEXUID_BINDINGS_TABLE = "//dmp/yandexuid_bindings"
BB_UPLOAD_STATE_TABLE = "//dmp/bb_upload_state"
BB_COLLECTOR_FRESH_DIR = "//dmp/bb_collector/fresh"
FROZEN_TIME = 1500001000
BB_UPLOAD_TTL = 10000


@pytest.fixture(scope="function")
def config(yt_stuff, bb_upload_enabled):
    return {
        config_fields.YT_PROXY: yt_stuff.get_server(),
        config_fields.YT_POOL: "pool",
        config_fields.DMP_ID: DMP_ID,
        config_fields.YANDEXUID_BINDINGS_TABLE: YANDEXUID_BINDINGS_TABLE,
        config_fields.BB_UPLOAD_STATE_TABLE: BB_UPLOAD_STATE_TABLE,
        config_fields.BB_COLLECTOR_FRESH_DIR: BB_COLLECTOR_FRESH_DIR,
        config_fields.BB_UPLOAD_TTL: BB_UPLOAD_TTL,
        config_fields.BB_UPLOAD_ENABLED: bb_upload_enabled
    }


def get_input_tables(yandexuid_bindings_filename, bb_upload_state_filename):
    input_tables = []
    if yandexuid_bindings_filename is not None:
        table = tables.get_yson_table_with_schema(yandexuid_bindings_filename, YANDEXUID_BINDINGS_TABLE, bindings.get_yandexuid_schema())
        input_tables.append((table, tests.TableIsNotChanged()))
    if bb_upload_state_filename is not None:
        table = tables.get_yson_table_with_schema(bb_upload_state_filename, BB_UPLOAD_STATE_TABLE, bb_upload_state.get_schema())
        input_tables.append((table, None))
    return input_tables


@pytest.mark.parametrize("yandexuid_bindings_filename", ["yandexuid_bindings.yson", None], ids=["YandexuidBindingsExists", "YandexuidBindingsIsMissing"])
@pytest.mark.parametrize("bb_upload_state_filename", ["bb_upload_state.yson", None], ids=["BbUploadStateExists", "BbUploadStateIsMissing"])
@pytest.mark.parametrize("bb_upload_enabled", [True, False], ids=["BbUploadEnabled", "BbUploadDisabled"])
def test_zero_rc(yt_stuff, config, yandexuid_bindings_filename, bb_upload_state_filename):
    diff = tests.Diff()
    return tests.yt_test(
        yt_client=yt_stuff.get_yt_client(),
        binary=yatest.common.binary_path("crypta/dmp/yandex/bin/prepare_bindings_to_upload/bin/crypta-dmp-yandex-prepare-bindings-to-upload"),
        args=[
            "--config", yaml_config.dump(config)
        ],
        data_path=yatest.common.test_source_path("data"),
        input_tables=get_input_tables(yandexuid_bindings_filename, bb_upload_state_filename),
        output_tables=[
            (
                tables.YsonTable("output_bb_upload_state.yson", BB_UPLOAD_STATE_TABLE, yson_format="pretty"),
                tests.IfExists([tests.SchemaEquals(bb_upload_state.get_schema()), diff])
            ),
            (
                cypress.CypressNode(BB_COLLECTOR_FRESH_DIR),
                tests.TestNodesInMapNode([tests.SchemaEquals(collector.get_schema()), diff], tag="bb_collector_fresh", on_read=collector.on_read())
            ),
        ],
        env={
            time_utils.CRYPTA_FROZEN_TIME_ENV: str(FROZEN_TIME)
        }
    )
