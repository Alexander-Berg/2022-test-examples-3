import pytest

import yatest.common

from crypta.dmp.common.data.python import (
    bindings,
    meta,
)
from crypta.dmp.yandex.bin.common.python import config_fields
from crypta.lib.python import yaml_config
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)


@pytest.fixture(scope="function")
def config(local_yt):
    return {
        config_fields.YT_PROXY: local_yt.get_server(),
        config_fields.YT_POOL: "pool",
        config_fields.YT_TMP_DIR: "//tmp",
        config_fields.EXT_ID_BINDINGS_TABLE: "//dmp/ext_id_bindings",
        config_fields.YANDEXUID_BINDINGS_TABLE: "//dmp/yandexuid_bindings",
        config_fields.META_TABLE: "//dmp/meta",
        config_fields.OUT_META_TABLE: "//dmp/meta_with_sizes"
    }


def test_calc_sizes(local_yt, local_yt_and_yql_env, config):
    return tests.yt_test(
        yt_client=local_yt.get_yt_client(),
        binary=yatest.common.binary_path("crypta/dmp/yandex/bin/calc_sizes/bin/crypta-dmp-yandex-calc-sizes"),
        args=["--config", yaml_config.dump(config)],
        data_path=yatest.common.test_source_path("data"),
        input_tables=[
            (
                tables.YsonTable("ext_id_bindings.yson", config[config_fields.EXT_ID_BINDINGS_TABLE], on_write=tables.OnWrite(attributes={"schema": bindings.get_ext_id_schema()})),
                tests.TableIsNotChanged(),
            ),
            (
                tables.YsonTable("yandexuid_bindings.yson", config[config_fields.YANDEXUID_BINDINGS_TABLE], on_write=tables.OnWrite(attributes={"schema": bindings.get_yandexuid_schema()})),
                tests.TableIsNotChanged(),
            ),
            (
                tables.YsonTable("meta.yson", config[config_fields.META_TABLE], on_write=tables.OnWrite(attributes={"schema": meta.get_schema()})),
                tests.TableIsNotChanged(),
            ),
        ],
        output_tables=[
            (
                tables.YsonTable("out_meta.yson", config[config_fields.OUT_META_TABLE], yson_format="pretty"),
                [tests.Diff(), tests.SchemaEquals(meta.get_schema_with_sizes())],
            )
        ],
        env=local_yt_and_yql_env,
    )
