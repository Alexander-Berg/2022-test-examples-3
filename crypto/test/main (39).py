import time

from library.python.protobuf.json import proto2json
import requests
import yatest.common
import yt.wrapper as yt

from crypta.lab.rule_estimator.proto import update_pb2
from crypta.lab.rule_estimator.services.api.proto import api_pb2
from crypta.lib.python.yt import schema_utils
from crypta.lib.python.yt.test_helpers import (
    files,
    tables,
    tests,
)
from crypta.lib.python.yt.dyntables import kv_schema
from crypta.profile.utils import utils


def get_url_schema():
    return schema_utils.yt_schema_from_dict({
        "yandexuid": "uint64",
        "host": "string",
        "url": "string",
        "is_yandex_referer": "boolean",
    })


def get_words_schema():
    return schema_utils.yt_schema_from_dict({
        "yandexuid": "uint64",
        "lemmas": "string",
    })


def get_hosts_schema():
    return schema_utils.yt_schema_from_dict({
        "host": "string",
        "yandexuid": "uint64",
    })


def get_apps_schema():
    return schema_utils.yt_schema_from_dict({
        "app": "string",
        "id": "string",
        "id_type": "string",
    })


def get_metrica_schema():
    return schema_utils.yt_schema_from_dict({
        "site": "string",
        "bar_visitors_count": "int64",
        "metrica_visitors_count": "int64",
    })


def get_direct_users_schema():
    return schema_utils.yt_schema_from_dict({
        "login": "string",
        "ClientID": "int64",
    })


def get_idfa_gaid_schema():
    return schema_utils.yt_schema_from_dict({
        "id": "string",
    })


def get_app_idf_schema():
    return schema_utils.yt_schema_from_dict({
        "app": "string",
    })


def canonize_stats(stats):
    for stat in stats["sensors"]:
        if stat["kind"] == "HIST_RATE":
            stat["sum"] = sum(stat["hist"]["buckets"])
            del stat["hist"]
    stats["sensors"].sort()
    return stats


def test_worker(rule_estimator_api_client, rule_estimator_api_config, rule_estimator_worker_config, rule_conditions, local_yt, patched_config, precalculated_table):
    url_on_write = tables.OnWrite(attributes={"schema": get_url_schema()}, sort_by=["host"])
    words_on_write = tables.OnWrite(attributes={"schema": get_words_schema()})
    hosts_on_write = tables.OnWrite(attributes={"schema": get_hosts_schema()})
    apps_on_write = tables.OnWrite(attributes={"schema": get_apps_schema()})
    metrica_counter_on_write = tables.OnWrite(attributes={"schema": get_metrica_schema()})
    direct_users_on_write = tables.OnWrite(attributes={"schema": get_direct_users_schema()})
    idfa_gaid_on_write = tables.OnWrite(attributes={"schema": get_idfa_gaid_schema()})
    app_idf_on_write = tables.OnWrite(attributes={"schema": get_app_idf_schema()})

    date = "2021-10-20"

    def test_func():
        request = update_pb2.Update(
            RuleId="rule-id",
            RuleConditionIds=rule_conditions.keys(),
        )
        for i in range(2):
            rule_estimator_api_client.Update(request)
        time.sleep(450)
        result = canonize_stats(requests.get("http://{}:{}/metrics".format(rule_estimator_worker_config.StatsHost, rule_estimator_worker_config.StatsPort)).json())
        return [
            proto2json.proto2json(rule_estimator_api_client.GetRuleConditionStats(api_pb2.GetRuleConditionStatsRequest(
                RuleConditionId=id_,
            )))
            for id_ in rule_conditions.keys()
        ] + [
            proto2json.proto2json(rule_estimator_api_client.GetRuleStats(api_pb2.GetRuleStatsRequest(
                RuleConditionIds=rule_conditions.keys(),
            )))
        ] + [result]

    for path in (
        yt.ypath_join(patched_config.PROFILES_YQL_TMP_YT_DIRECTORY, "tmp"),
        rule_estimator_worker_config.OutputDir,
        yt.ypath_join(rule_estimator_worker_config.RuleDir, "GetStandardSegmentsByPrecalculatedTables"),
    ):
        local_yt.get_yt_client().create("map_node", path, recursive=True)

    return tests.yt_test_func(
        local_yt.get_yt_client(),
        test_func,
        data_path=yatest.common.test_source_path("data"),
        input_tables=[
            (files.YtFile(
                yatest.common.binary_path("yql/udfs/crypta/identifiers/libcrypta_identifier_udf.so"),
                patched_config.CRYPTA_IDENTIFIERS_UDF_PATH,
            ), None),
            (tables.YsonTable(
                "metrica_sites.yson",
                yt.ypath_join(patched_config.METRICS_URLS_INDEX_DIR, date),
                on_write=url_on_write,
            ), tests.TableIsNotChanged()),
            (tables.YsonTable(
                "browser_sites.yson",
                yt.ypath_join(patched_config.BAR_URLS_INDEX_DIR, date),
                on_write=url_on_write,
            ), tests.TableIsNotChanged()),
            (tables.YsonTable(
                "browser_titles.yson",
                yt.ypath_join(patched_config.BAR_WORDS_INDEX_DIR, date),
                on_write=words_on_write,
            ), tests.TableIsNotChanged()),
            (tables.YsonTable(
                "yandexuid_metrica_browser_visitor_counter.yson",
                patched_config.YANDEXUID_METRICA_BROWSER_COUNTER_TABLE,
                on_write=metrica_counter_on_write,
            ), tests.TableIsNotChanged()),
            (tables.DynamicYsonTable(
                "rule_conditions_stats.yson",
                rule_estimator_api_config.RuleConditionStatsPath,
                on_write=tables.OnWrite(attributes={"schema": kv_schema.get()}),
            ), None),
            (tables.YsonTable(
                "precalculated_table.yson",
                precalculated_table["path"],
                on_write=tables.OnWrite(attributes={"schema": schema_utils.yt_schema_from_dict({
                    precalculated_table["idKey"]: 'uint64',
                })})
            ), tests.TableIsNotChanged()),
            (tables.YsonTable(
                "direct_users.yson",
                patched_config.DIRECT_USERS,
                on_write=direct_users_on_write,
            ), tests.TableIsNotChanged()),
            (tables.YsonTable(
                "gaid_cryptaid.yson",
                utils.get_matching_table('gaid', 'crypta_id'),
                on_write=idfa_gaid_on_write,
            ), tests.TableIsNotChanged()),
            (tables.YsonTable(
                "idfa_cryptaid.yson",
                utils.get_matching_table('idfa', 'crypta_id'),
                on_write=idfa_gaid_on_write,
            ), tests.TableIsNotChanged()),
            (tables.YsonTable(
                "apps.yson",
                patched_config.DEVID_BY_APP_MONTHLY_TABLE,
                on_write=apps_on_write,
            ), tests.TableIsNotChanged()),
            (tables.YsonTable(
                "app_idf.yson",
                patched_config.APP_IDF_TABLE,
                on_write=app_idf_on_write,
            ), tests.TableIsNotChanged()),
            (tables.YsonTable(
                "reqans_hosts.yson",
                yt.ypath_join(patched_config.REQANS_HOSTS_INDEX_DIR, date),
                on_write=hosts_on_write,
            ), tests.TableIsNotChanged()),
        ],
        output_tables=[
            (tables.YsonTable(
                "segment{}.yson".format(rule_condition_id),
                yt.ypath_join(rule_estimator_worker_config.OutputDir, str(rule_condition_id)),
                yson_format="pretty",
            ), tests.Diff())
            for rule_condition_id in [1, 2, 3, 4, 5]
        ],
        return_result=True,
    )
