import pytest

import yatest.common

from crypta.dmp.adobe.bin.common.python import config_fields
from crypta.dmp.common.data.python import (
    bindings,
    meta,
)
from crypta.lib.python import yaml_config
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)


@pytest.fixture(scope="function")
def config(local_yt):
    return {
        config_fields.YT_PROXY: local_yt.get_server(),
        config_fields.YT_POOL: "crypta_adobe",
        config_fields.YT_TMP_DIR: "//tmp",

        config_fields.FILTERED_YANDEXUID_BINDINGS_TABLE: "//adobe/yandexuid_bindings",
        config_fields.META_TABLE: "//adobe/meta",
        config_fields.OUT_META_TABLE: "//dmp/out/meta/dmp-adform"
    }


def test_calc_sizes(local_yt, local_yt_and_yql_env, config):
    config_path = yaml_config.dump(config)

    return tests.yt_test(
        yt_client=local_yt.get_yt_client(),
        binary=yatest.common.binary_path("crypta/dmp/adobe/bin/calc_sizes/bin/crypta-adobe-calc-sizes"),
        args=["--config", config_path],
        data_path=yatest.common.test_source_path("data"),
        input_tables=[
            (tables.YsonTable("bindings", config[config_fields.FILTERED_YANDEXUID_BINDINGS_TABLE], on_write=tables.OnWrite(attributes={"schema": bindings.get_yandexuid_schema()})), None),
            (tables.YsonTable("meta", config[config_fields.META_TABLE], on_write=tables.OnWrite(attributes={"schema": meta.get_schema()})), None),
        ],
        output_tables=[
            (tables.YsonTable("out_meta", config[config_fields.OUT_META_TABLE], yson_format="pretty"), [tests.Diff(), tests.SchemaEquals(meta.get_schema_with_sizes())])
        ],
        env=local_yt_and_yql_env,
    )
