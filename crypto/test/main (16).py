import os

import pytest
import yatest.common

from crypta.dmp.common import calc_sizes
from crypta.dmp.common.data.python import (
    bindings,
    meta,
)
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)


def get_table(filename, yt_path, schema):
    if filename is None:
        return None

    return tables.get_yson_table_with_schema(filename, yt_path, schema)


@pytest.mark.parametrize("yandexuid_bindings_filename", ["yandexuid_bindings.yson", None], ids=["WithYandexuidBindings", "WithoutYandexuidBindings"])
@pytest.mark.parametrize("ext_id_bindings_filename", ["ext_id_bindings.yson", None], ids=["WithExtIdBindings", "WithoutExtIdBindings"])
def test_run(local_yt, local_yt_and_yql_env, ext_id_bindings_filename, yandexuid_bindings_filename):
    yt_client = local_yt.get_yt_client()
    os.environ.update(local_yt_and_yql_env)

    yt_client.remove("//dmp", recursive=True, force=True)

    meta_table = get_table("meta.yson", "//dmp/meta", meta.get_schema())
    ext_id_bindings_table = get_table(ext_id_bindings_filename, "//dmp/ext_id_bindings", bindings.get_ext_id_schema())
    yandexuid_bindings_table = get_table(yandexuid_bindings_filename, "//dmp/yandexuid_bindings", bindings.get_yandexuid_schema())

    meta_with_sizes_table = tables.YsonTable("meta_with_sizes.yson", "//dmp/meta_with_sizes", yson_format="pretty")

    return tests.yt_test_func(
        yt_client,
        lambda: calc_sizes.run(
            yt_proxy=local_yt.get_server(),
            yt_pool="pool",
            yt_tmp_dir="//tmp",
            meta_table=meta_table.cypress_path,
            meta_with_sizes_table=meta_with_sizes_table.cypress_path,
            ext_id_bindings_table=ext_id_bindings_table.cypress_path if ext_id_bindings_table is not None else None,
            yandexuid_bindings_table=yandexuid_bindings_table.cypress_path if yandexuid_bindings_table is not None else None
        ),
        data_path=yatest.common.test_source_path("data"),
        input_tables=[
            (table, tests.TableIsNotChanged()) for table in [meta_table, ext_id_bindings_table, yandexuid_bindings_table] if table is not None
        ],
        output_tables=[
            (meta_with_sizes_table, (tests.SchemaEquals(meta.get_schema_with_sizes()), tests.Diff())),
        ],
    )
