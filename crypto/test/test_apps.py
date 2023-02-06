import yatest.common

from crypta.lab.lib.crypta_id.apps import PrepareApps
from crypta.lib.python.bt.conf import conf
from crypta.lib.python.yt import schema_utils
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)


def get_app_metrica_month_attrs():
    return {
        "schema": schema_utils.get_strict_schema([
            {"name": "id", "required": False, "type": "string", "sort_order": "ascending"},
            {"name": "id_type", "required": True, "type": "string", "sort_order": "ascending"},
            {"name": "apps", "required": False, "type": "any"}
        ]),
        "_yql_row_spec": {
            "SortDirections": [1, 1],
            "SortMembers": ["id", "id_type"],
            "SortedBy": ["id", "id_type"],
            "SortedByTypes": [["OptionalType", ["DataType", "String"]], ["DataType", "String"]],
            "StrictSchema": True,
            "Type": ["StructType", [["apps", ["ListType", ["DataType", "String"]]], ["id", ["OptionalType", ["DataType", "String"]]], ["id_type", ["DataType", "String"]]]],
            "UniqueKeys": False,
        }
    }


def test_apps(local_yt, config, day):
    task = PrepareApps(day=day)
    task.transaction_id = "0-0-0-0"

    return tests.yt_test_func(
        yt_client=local_yt.get_yt_client(),
        func=task.run,
        data_path=yatest.common.test_source_path('data'),
        input_tables=[
            (
                tables.YsonTable(
                    'app_metrica_month.yson',
                    config.paths.ids_storage.device_id.app_metrica_month,
                    on_write=tables.OnWrite(attributes=get_app_metrica_month_attrs())
                ),
                tests.TableIsNotChanged()
            ),
            (
                tables.get_yson_table_with_schema(
                    'vertices_no_multi_profile.yson',
                    config.paths.graph.vertices_no_multi_profile,
                    schema=schema_utils.yt_schema_from_dict(
                        {
                            "id": "string",
                            "id_type": "string",
                            "cryptaId": "string",
                        }
                    )
                ),
                tests.TableIsNotChanged()
            ),
        ],
        output_tables=[
            (tables.YsonTable('apps_by_crypta_id.yson', task.destination, yson_format='pretty'), tests.Diff()),
            (tables.YsonTable('apps_weights.yson', conf.paths.ids_storage.crypta_id.apps_weights, yson_format='pretty'), tests.Diff()),
        ],
    )
