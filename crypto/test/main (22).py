import yatest.common
from yt import yson

from crypta.dmp.yandex.bin.make_mac_hash_yuid.lib import mac_hash_yuid_schema
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
    utils,
)


def test_basic(local_yt, local_yt_and_yql_env, config_file, config):
    utils.create_yt_dirs(local_yt, [config.Yt.TmpDir])

    diff = tests.Diff()
    schema_test = tests.SchemaEquals(mac_hash_yuid_schema.get())
    return tests.yt_test(
        yt_client=local_yt.get_yt_client(),
        binary=yatest.common.binary_path("crypta/dmp/yandex/bin/make_mac_hash_yuid/bin/crypta-dmp-yandex-make-mac-hash-yuid"),
        args=["--config", config_file],
        data_path=yatest.common.test_source_path("data"),
        input_tables=[
            get_input_table("indevice_mac_yuid.yson", config.IndeviceMacYuidTable, ["id", "target_id"]),
            get_input_table("vmetro_mac_yuid.yson", config.VmetroMacYuidTable, ["id_value", "yuid"]),
            get_input_table("actual_yuid.yson", config.ActualYuidTable, ["id"]),
        ],
        output_tables=[
            (tables.YsonTable("output_mac_hash_yuid.yson", config.OutputMacHashYuidTable, yson_format="pretty"), [schema_test, diff]),
        ],
        env=local_yt_and_yql_env,
    )


def get_input_table(filename, path, fields):
    schema = yson.YsonList([{"name": field, "type": "string"} for field in fields])
    schema.attributes["strict"] = True
    schema.attributes["unique_keys"] = False

    table = tables.get_yson_table_with_schema(filename, path, schema)

    return (table, tests.TableIsNotChanged())
