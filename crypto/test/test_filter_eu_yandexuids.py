import pytest

import yatest.common
import yt.yson as yson

from crypta.dmp.adobe.bin.common.python import config_fields
from crypta.dmp.common.data.python import bindings
from crypta.lib.python import yaml_config
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)


YANDEXUID_GEO_TABLE = "//adobe/geo_table"
YANDEXUID_BINDINGS_TABLE = "//adobe/yandexuid-bindings-table"
FILTERED_YANDEXUID_BINDINGS_TABLE = "//adobe/filtered_yandexuid_bindings_table"


@pytest.fixture(scope="function")
def config(local_yt):
    return {
        config_fields.YT_PROXY: local_yt.get_server(),
        config_fields.YT_POOL: "crypta_adobe",
        config_fields.YT_TMP_DIR: "//tmp",

        config_fields.BINDINGS_YANDEXUID_FIELD: "yandexuid",
        config_fields.GEO_TABLE_YANDEXUID_FIELD: "id",
        config_fields.GEO_TABLE_COUNTRY_ID_FIELD: "main_region_country",
        config_fields.EU_COUNTRY_IDS: [1, 2],

        config_fields.YANDEXUID_BINDINGS_TABLE: YANDEXUID_BINDINGS_TABLE,
        config_fields.FILTERED_YANDEXUID_BINDINGS_TABLE: FILTERED_YANDEXUID_BINDINGS_TABLE,
        config_fields.YANDEXUID_GEO_TABLE: YANDEXUID_GEO_TABLE
    }


def get_yandexuid_geo_schema():
    return yson.YsonList([
        {"name": "id", "type": "string", "required": True},
        {"name": "main_region_country", "type": "uint64", "required": True}
    ])


def test_filter_eu_yandexuids(local_yt, local_yt_and_yql_env, config):
    config_path = yaml_config.dump(config)

    return tests.yt_test(
        yt_client=local_yt.get_yt_client(),
        binary=yatest.common.binary_path("crypta/dmp/adobe/bin/filter_eu_yandexuids/bin/crypta-adobe-filter-eu-yandexuids"),
        args=["--config", config_path],
        data_path=yatest.common.test_source_path("data"),
        input_tables=[
            (tables.YsonTable("yandexuid_geo", YANDEXUID_GEO_TABLE, on_write=tables.OnWrite(attributes={"schema": get_yandexuid_geo_schema()})), tests.Exists()),
            (tables.YsonTable("yandexuid_bindings", YANDEXUID_BINDINGS_TABLE, on_write=tables.OnWrite(attributes={"schema": bindings.get_yandexuid_schema()})), tests.Exists()),
        ],
        output_tables=[
            (tables.YsonTable("filtered_yandexuid_bindings", FILTERED_YANDEXUID_BINDINGS_TABLE, yson_format="pretty"),
             [tests.Diff(), tests.SchemaEquals(bindings.get_yandexuid_schema())]),
        ],
        env=local_yt_and_yql_env,
    )
