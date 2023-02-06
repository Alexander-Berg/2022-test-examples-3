import pytest

import yatest.common

from crypta.dmp.common.data.python import bindings
from crypta.dmp.yandex.bin.common.python import config_fields
from crypta.lib.python import yaml_config
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests
)


UNMERGED_YANDEXUID_BINDINGS_TABLE = "//dmp/unmerged_yandexuid_bindings"
YANDEXUID_BINDINGS_TABLE = "//dmp/yandexuid_bindings"


@pytest.fixture(scope="function")
def config(yt_stuff):
    return {
        config_fields.YT_PROXY: yt_stuff.get_server(),
        config_fields.YT_POOL: "crypta_dmp",
        config_fields.UNMERGED_YANDEXUID_BINDINGS_TABLE: UNMERGED_YANDEXUID_BINDINGS_TABLE,
        config_fields.YANDEXUID_BINDINGS_TABLE: YANDEXUID_BINDINGS_TABLE
    }


def test_merge_yandexuid_bindings(yt_stuff, config):
    return tests.yt_test(
        yt_client=yt_stuff.get_yt_client(),
        binary=yatest.common.binary_path("crypta/dmp/yandex/bin/merge_yandexuid_bindings/bin/crypta-dmp-yandex-merge-yandexuid-bindings"),
        args=["--config", yaml_config.dump(config)],
        data_path=yatest.common.test_source_path("data"),
        input_tables=[
            (tables.get_yson_table_with_schema("input.yson", UNMERGED_YANDEXUID_BINDINGS_TABLE, bindings.get_not_unique_yandexuid_schema()), tests.TableIsNotChanged())
        ],
        output_tables=[
            (tables.YsonTable("output.yson", YANDEXUID_BINDINGS_TABLE, yson_format="pretty"), [tests.SchemaEquals(bindings.get_yandexuid_schema()), tests.Diff()])
        ]
    )
