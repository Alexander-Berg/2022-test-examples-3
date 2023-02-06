from datetime import timedelta

import yatest.common
from yt.wrapper import ypath

from crypta.lib.python.yt import schema_utils
from crypta.lib.python.yt.test_helpers import (
    cypress,
    tables,
    tests,
)


def test_history_collecting(local_yt, local_yt_and_yql_env, config_file, config):
    date = "2021-07-21"
    diff_test = tests.Diff()
    expiration_time_test = tests.ExpirationTime(ttl=timedelta(days=config.HistoryTtlDays))

    return tests.yt_test(
        yt_client=local_yt.get_yt_client(),
        binary=yatest.common.binary_path("crypta/adhoc/history/bin/crypta-adhoc-history"),
        args=[
            "--config", config_file,
        ],
        data_path=yatest.common.test_source_path("data"),
        input_tables=[
            (tables.get_yson_table_with_schema("yandexuid_to_crypta_id.yson", config.YandexuidToCryptaIdTable, _matching_schema()), [tests.TableIsNotChanged()]),
        ] + _input_logs(config, date),
        output_tables=[
            (cypress.CypressNode(config.OutputDir), tests.TestNodesInMapNode([diff_test, expiration_time_test], tag="output")),
            (tables.YsonTable("tracker.yson", config.BarNavig.TrackTable), [diff_test]),
        ],
        env=local_yt_and_yql_env,
    )


def _input_logs(config, date):
    logs = []

    for local_filename, cypress_dir, schema in [
        ("bar_navig_{}.yson", config.BarNavig.SourceDir, _bar_navig_schema()),
        ("chevent_{}.yson", config.CheventDir, _chevent_schema()),
        ("facebook_{}.yson", config.FacebookAdsDir, _facebook_schema()),
        ("reqans_{}.yson", config.ReqansDir, _reqans_schema()),
        ("visit_{}.yson", config.VisitDir, _visit_schema()),
        ("visit_private_{}.yson", config.VisitPrivateDir, _visit_schema()),
    ]:
        logs.append((tables.get_yson_table_with_schema(local_filename.format(date), ypath.ypath_join(cypress_dir, date), schema), [tests.TableIsNotChanged()]))

    return logs


def _matching_schema():
    return schema_utils.get_strict_schema(schema_utils.yt_schema_from_dict({
        "id": "string",
        "target_id": "string",
    }))


def _bar_navig_schema():
    return schema_utils.get_strict_schema(schema_utils.yt_schema_from_dict({
        "yandexuid": "string",
        "http_params": "string",
        "unixtime": "string",
    }))


def _chevent_schema():
    return schema_utils.get_strict_schema(schema_utils.yt_schema_from_dict({
        "uniqid": "uint64",
        "bannerid": "int64",
        "eventtime": "int64",
        "countertype": "int64",
        "cryptaidv2": "uint64",
    }, sort_by=["uniqid"]))


def _facebook_schema():
    return schema_utils.get_strict_schema(schema_utils.yt_schema_from_dict({
        "Yandexuid": "uint64",
        "Timestamp": "uint64",
        "Banners": "any",
        "Text": "string",
        "Title": "string",
        "Type": "string",
    }))


def _reqans_schema():
    schema = schema_utils.get_strict_schema(schema_utils.yt_schema_from_dict({
        "UserId": "any",
        "Query": "string",
        "Timestamp": "uint64",
    }))

    for field in schema:
        if field["name"] == "UserId":
            field["type_v3"] = {
                "type_name": "optional",
                "item": {
                    "type_name": "struct",
                    "members": [{
                        "type": {
                            "type_name": "optional",
                            "item": "string",
                        },
                        "name": "YandexUid",
                    }],
                },
            }

    return schema


def _visit_schema():
    return schema_utils.get_strict_schema(schema_utils.yt_schema_from_dict({
        "UserID": "uint64",
        "StartURL": "string",
        "StartTime": "uint32",
        "CryptaID": "uint64",
        "VisitID": "uint64",
    }, sort_by=["UserID", "VisitID"]))
