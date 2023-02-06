import yatest.common
import yt.wrapper as yt

from crypta.lib.python import time_utils
from crypta.lib.python.yt import schema_utils
from crypta.lib.python.yt.test_helpers import (
    cypress,
    tables,
    tests,
)


def test_travelers_to_moscow_with_night_stay(local_yt, local_yt_and_yql_env, config_file, config):
    diff = tests.Diff()
    test = tests.TestNodesInMapNode(tests_getter=[diff], tag="output")

    local_yt_and_yql_env[time_utils.CRYPTA_FROZEN_TIME_ENV] = "1622322000"  # 2021-05-30 00:00:00

    return tests.yt_test(
        yt_client=local_yt.get_yt_client(),
        binary=yatest.common.binary_path("crypta/profile/services/precalculate_tables/bin/crypta-profile-precalculate-tables"),
        args=[
            "--config", config_file,
            "travelers_to_moscow_with_night_stay",
        ],
        data_path=yatest.common.test_source_path("data"),
        input_tables=[
            (_crypta_id_regions(config), [tests.TableIsNotChanged()]),
            (_vertices_no_multi_profile(config), [tests.TableIsNotChanged()]),
            (_weekly_travelers(config, "2020-05-30"), [tests.TableIsNotChanged()]),
            (_weekly_travelers(config, "2021-05-29"), [tests.TableIsNotChanged()]),
        ],
        output_tables=[
            (cypress.CypressNode(config.TravelersToMoscowWithNightStayDir), [test])
        ],
        env=local_yt_and_yql_env,
    )


def _crypta_id_regions(config):
    schema = schema_utils.get_strict_schema([
        {"name": "crypta_id", "type": "string", "required": False, "sort_order": "ascending"},
        {"name": "main_region", "type": "int32", "required": False},
    ])
    schema.attributes["unique_keys"] = True
    return tables.get_yson_table_with_schema("crypta_id_regions.yson", config.CryptaIdRegionsTable, schema)


def _vertices_no_multi_profile(config):
    schema = schema_utils.get_strict_schema([
        {"name": "id", "type": "string", "required": False, "sort_order": "ascending"},
        {"name": "id_type", "type": "string", "required": False, "sort_order": "ascending"},
        {"name": "cryptaId", "type": "string", "required": False},
    ])
    return tables.get_yson_table_with_schema("vertices_no_multi_profile.yson", config.VerticesNoMultiProfileTable, schema)


def _weekly_travelers(config, name):
    attrs = {
        "schema": schema_utils.get_strict_schema([
            {"name": "id", "type": "string", "required": False},
            {"name": "id_type", "type": "string", "required": True},
            {"name": "region", "type": "int32", "required": False},
            {"name": "days", "type": "any", "required": False, "type_v3": {"type_name": "optional", "item": "yson"}},
        ]),
        "_yql_row_spec": {
            "StrictSchema": True,
            "Type": ["StructType", [
                ["id", ["OptionalType", ["DataType", "String"]]],
                ["id_type", ["OptionalType", ["DataType", "String"]]],
                ["region", ["OptionalType", ["DataType", "Int32"]]],
                ["days", ["ListType", ["DataType", "String"]]],
            ]],
            "UniqueKeys": False,
        }
    }
    cypress_path = yt.ypath_join(config.WeeklyTravelersDir, name)
    local_path = "{}.yson".format(name)
    return tables.YsonTable(local_path, cypress_path, on_write=tables.OnWrite(attributes=attrs))
