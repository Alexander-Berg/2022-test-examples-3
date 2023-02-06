import collections
import os

import yatest.common
from yt import yson

from crypta.dmp.common.data.python import bindings
from crypta.dmp.common import match_cookies
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)


EXT_ID_FIELD = "ext_id"
CM_YANDEXUID_FIELD = "yuid"


TestAssets = collections.namedtuple("TestAssets", [
    "cm_schema",
    "cm_path",
    "test_case",
])


def get_cm_schema_with_strings():
    return yson.YsonList([
        {"name": EXT_ID_FIELD, "type": "string", "required": True},
        {"name": CM_YANDEXUID_FIELD, "type": "string", "required": True},
    ])


def get_cm_schema_with_ints():
    return yson.YsonList([
        {"name": EXT_ID_FIELD, "type": "uint64", "required": True},
        {"name": CM_YANDEXUID_FIELD, "type": "uint64", "required": True},
    ])


def run_test(local_yt, local_yt_and_yql_env, assets):
    os.environ.update(local_yt_and_yql_env)
    yt_client = local_yt.get_yt_client()

    ext_id_bindings_table = tables.get_yson_table_with_schema("ext_id_bindings.yson", "//ext_id_bindings_{}".format(assets.test_case), bindings.get_id_schema())
    cm_table = tables.get_yson_table_with_schema(assets.cm_path, "//cm_{}".format(assets.test_case), assets.cm_schema)
    yandexuid_bindings_table = tables.YsonTable(
        "yandexuid_bindings_{}.yson".format(assets.test_case),
        "//yandexuid_bindings_{}".format(assets.test_case),
        yson_format="pretty",
    )

    return tests.yt_test_func(
        yt_client,
        lambda: match_cookies.run(
            yt_proxy=local_yt.get_server(),
            yt_pool="pool",
            yt_tmp_dir="//tmp",
            ext_id_bindings_table=ext_id_bindings_table.cypress_path,
            bindings_ext_id_field=bindings.ID,
            cm_table=cm_table.cypress_path,
            cm_ext_id_field=EXT_ID_FIELD,
            cm_yandexuid_field=CM_YANDEXUID_FIELD,
            yandexuid_bindings_table=yandexuid_bindings_table.cypress_path,
            bindings_yandexuid_field=bindings.YANDEXUID,
            yandexuid_bindings_table_schema=bindings.get_yandexuid_schema()
        ),
        data_path=yatest.common.test_source_path("data"),
        input_tables=[
            (table, tests.TableIsNotChanged()) for table in [ext_id_bindings_table, cm_table]
        ],
        output_tables=[
            (yandexuid_bindings_table, (tests.SchemaEquals(bindings.get_yandexuid_schema()), tests.Diff())),
        ],
    )


def test_with_strings(local_yt, local_yt_and_yql_env):
    return run_test(local_yt, local_yt_and_yql_env, TestAssets(
        cm_schema=get_cm_schema_with_strings(),
        cm_path="cm_strings.yson",
        test_case="strings",
    ))


def test_with_ints(local_yt, local_yt_and_yql_env):
    return run_test(local_yt, local_yt_and_yql_env, TestAssets(
        cm_schema=get_cm_schema_with_ints(),
        cm_path="cm_ints.yson",
        test_case="ints",
    ))
