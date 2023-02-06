import pytest

import yatest.common
from yt import yson

from crypta.dmp.common.data.python import bindings
from crypta.dmp.yandex.bin.common.python import config_fields
from crypta.lib.python import yaml_config
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)


EXT_ID_BINDINGS_TABLE = "//dmp/ext_id_bindings"
UNMERGED_YANDEXUID_BINDINGS_TABLE = "//dmp/unmerged_yandexuid_bindings"
COOKIE_MATCHING_TABLE = "//dmp/cookie_matching"
CM_EXT_ID_FIELD = "ext_id"
CM_YANDEXUID_FIELD = "yuid"


def get_cm_schema():
    schema = yson.YsonList([
        dict(name=CM_EXT_ID_FIELD, type="string", required=True, sort="ascending"),
        dict(name=CM_YANDEXUID_FIELD, type="string", required=True),
    ])
    schema.attributes["strict"] = True
    return schema


@pytest.fixture(scope="function")
def config(local_yt):
    return {
        config_fields.YT_PROXY: local_yt.get_server(),
        config_fields.YT_POOL: "crypta_adobe",
        config_fields.YT_TMP_DIR: "//tmp",
        config_fields.UNMERGED_YANDEXUID_BINDINGS_TABLE: UNMERGED_YANDEXUID_BINDINGS_TABLE,
        config_fields.EXT_ID_BINDINGS_TABLE: EXT_ID_BINDINGS_TABLE,
        config_fields.COOKIE_MATCHING_TABLE: COOKIE_MATCHING_TABLE,
        config_fields.CM_EXT_ID_FIELD: CM_EXT_ID_FIELD,
        config_fields.CM_YANDEXUID_FIELD: CM_YANDEXUID_FIELD,
    }


def test_match_cookies(local_yt, local_yt_and_yql_env, config):
    schema_test = tests.SchemaEquals(bindings.get_not_unique_yandexuid_schema())
    diff = tests.Diff()
    return tests.yt_test(
        yt_client=local_yt.get_yt_client(),
        binary=yatest.common.binary_path("crypta/dmp/yandex/bin/match_cookies/bin/crypta-dmp-yandex-match-cookies"),
        args=["--config", yaml_config.dump(config)],
        data_path=yatest.common.test_source_path("data"),
        input_tables=[
            (tables.get_yson_table_with_schema("ext_id_bindings", EXT_ID_BINDINGS_TABLE, bindings.get_ext_id_schema()), None),
            (tables.get_yson_table_with_schema("cookies_matches", COOKIE_MATCHING_TABLE, get_cm_schema()), None),
        ],
        output_tables=[
            (tables.YsonTable("unmerged_yandexuid_bindings", UNMERGED_YANDEXUID_BINDINGS_TABLE, yson_format="pretty"), [schema_test, diff]),
        ],
        env=local_yt_and_yql_env,
    )
