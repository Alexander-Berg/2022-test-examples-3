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


EXT_ID_BINDINGS_TABLE = "//adobe/ext_id_bindings_table"
YANDEXUID_BINDINGS_TABLE = "//adobe/yandexuid_bindings_table"
COOKIES_MATCHES_TABLE = "//adobe/cm_table"
CM_EXT_ID_FIELD = "ext_id"
CM_YANDEXUID_FIELD = "yuid"


def get_cm_schema():
    schema = yson.YsonList([
        dict(name=CM_EXT_ID_FIELD, type="string", required=True, sort="ascending"),
        dict(name=CM_YANDEXUID_FIELD, type="string", required=True)
    ])
    schema.attributes["strict"] = True
    return schema


@pytest.fixture(scope="function")
def config(local_yt):
    return {
        config_fields.YT_PROXY: local_yt.get_server(),
        config_fields.YT_POOL: "crypta_adobe",
        config_fields.YT_TMP_DIR: "//tmp",

        config_fields.BINDINGS_EXT_ID_FIELD: bindings.ID,
        config_fields.CM_EXT_ID_FIELD: CM_EXT_ID_FIELD,
        config_fields.CM_YANDEXUID_FIELD: CM_YANDEXUID_FIELD,
        config_fields.BINDINGS_YANDEXUID_FIELD: bindings.YANDEXUID,
        config_fields.YANDEXUID_BINDINGS_TABLE: YANDEXUID_BINDINGS_TABLE,
        config_fields.EXT_ID_BINDINGS_TABLE: EXT_ID_BINDINGS_TABLE,
        config_fields.COOKIES_MATCHES_TABLE: COOKIES_MATCHES_TABLE
    }


def test_match_cookies(local_yt, local_yt_and_yql_env, config):
    config_path = yaml_config.dump(config)

    return tests.yt_test(
        yt_client=local_yt.get_yt_client(),
        binary=yatest.common.binary_path("crypta/dmp/adobe/bin/match_cookies/bin/crypta-adobe-match-cookies"),
        args=["--config", config_path],
        data_path=yatest.common.test_source_path("data"),
        input_tables=[
            (tables.get_yson_table_with_schema("ext_id_bindings", EXT_ID_BINDINGS_TABLE, bindings.get_id_schema()), None),
            (tables.get_yson_table_with_schema("cookies_matches", COOKIES_MATCHES_TABLE, get_cm_schema()), None),
        ],
        output_tables=[
            (tables.YsonTable("yandexuid_bindings", YANDEXUID_BINDINGS_TABLE, yson_format="pretty"), [tests.SchemaEquals(bindings.get_yandexuid_schema()), tests.Diff()]),
        ],
        env=local_yt_and_yql_env,
    )
