import json

import grpc
from library.python.protobuf.json import proto2json
import yatest.common
import yt.yson

from crypta.lab.rule_estimator.proto import (
    rule_estimate_stats_pb2,
    update_pb2,
)
from crypta.lab.rule_estimator.services.api.proto import api_pb2
from crypta.lib.python.logbroker.test_helpers import consumer_utils
from crypta.lib.python.yt.dyntables import kv_schema
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)


RULE_CONDITION_IDS = [1, 2, 3]
USER_SET_ID = 1234


def rule_estimates_stats_transformer(row):
    stats = rule_estimate_stats_pb2.RuleEstimateStats()
    stats.ParseFromString(yt.yson.get_bytes(row["value"]))
    row["value"] = json.loads(proto2json.proto2json(stats))
    return row


def test_update(rule_estimator_api_client, logbroker_client, clean_local_yt, rule_estimator_api_config):
    def test_func():
        response = proto2json.proto2json(rule_estimator_api_client.Update(update_pb2.Update(
            RuleId="rule-id",
            RuleConditionIds=RULE_CONDITION_IDS,
        )))
        stats = [
            proto2json.proto2json(rule_estimator_api_client.GetRuleConditionStats(api_pb2.GetRuleConditionStatsRequest(
                RuleConditionId=rule_condition_id,
            )))
            for rule_condition_id in RULE_CONDITION_IDS
        ]
        return {"response": response, "stats": stats}

    result = tests.yt_test_func(
        clean_local_yt.get_yt_client(),
        test_func,
        data_path=yatest.common.test_source_path("data"),
        input_tables=[(
            tables.DynamicYsonTable(
                "rule_conditions_stats.yson",
                rule_estimator_api_config.RuleConditionStatsPath,
                on_write=tables.OnWrite(attributes={"schema": kv_schema.get()}),
            ), None
        )],
        output_tables=[(
            tables.DynamicYsonTable(
                "rule_conditions_stats.yson",
                rule_estimator_api_config.RuleConditionStatsPath,
                yson_format="pretty",
                on_read=tables.OnRead(row_transformer=rule_estimates_stats_transformer)
            ), tests.Diff(local=True)
        )],
        return_result=True,
    )

    result.append(consumer_utils.read_all(logbroker_client.create_consumer(), timeout=30))
    return result


def test_rule_condition_stats(rule_estimator_api_client, rule_estimator_api_config, clean_local_yt):
    def test_func():
        try:
            rule_estimator_api_client.GetRuleConditionStats(api_pb2.GetRuleConditionStatsRequest(
                RuleConditionId=1,
            ))
            raise Exception
        except grpc.RpcError as e:
            assert grpc.StatusCode.NOT_FOUND == e.code()

        return [proto2json.proto2json(x) for x in [
            rule_estimator_api_client.SetRuleConditionStats(api_pb2.SetRuleConditionStatsRequest(
                RuleConditionId=1,
                Stats=rule_estimate_stats_pb2.RuleEstimateStats(
                    Coverage=100,
                    UserSetId=USER_SET_ID,
                    Timestamp=1600000000,
                ),
            )),
            rule_estimator_api_client.GetRuleConditionStats(api_pb2.GetRuleConditionStatsRequest(
                RuleConditionId=1,
            )),
        ]]

    return tests.yt_test_func(
        clean_local_yt.get_yt_client(),
        test_func,
        data_path=yatest.common.test_source_path("data"),
        input_tables=[(
            tables.DynamicYsonTable(
                "rule_conditions_stats.yson",
                rule_estimator_api_config.RuleConditionStatsPath,
                on_write=tables.OnWrite(attributes={"schema": kv_schema.get()}),
            ), None
        )],
        output_tables=[(
            tables.DynamicYsonTable(
                "rule_conditions_stats.yson",
                rule_estimator_api_config.RuleConditionStatsPath,
                yson_format="pretty",
                on_read=tables.OnRead(row_transformer=rule_estimates_stats_transformer)
            ), tests.Diff(local=True)
        )],
        return_result=True,
    )


def test_rule_stats(rule_estimator_api_client, rule_estimator_api_config, clean_local_yt):
    def test_func():
        try:
            rule_estimator_api_client.GetRuleStats(api_pb2.GetRuleStatsRequest(
                RuleConditionIds=RULE_CONDITION_IDS,
            ))
            raise Exception
        except grpc.RpcError as e:
            assert grpc.StatusCode.NOT_FOUND == e.code()

        return [proto2json.proto2json(x) for x in [
            rule_estimator_api_client.SetRuleStats(api_pb2.SetRuleStatsRequest(
                RuleConditionIds=RULE_CONDITION_IDS,
                Stats=rule_estimate_stats_pb2.RuleEstimateStats(
                    Coverage=100,
                    UserSetId=USER_SET_ID,
                    Timestamp=1600000000,
                ),
            )),
            rule_estimator_api_client.GetRuleStats(api_pb2.GetRuleStatsRequest(
                RuleConditionIds=RULE_CONDITION_IDS,
            )),
        ]]

    return tests.yt_test_func(
        clean_local_yt.get_yt_client(),
        test_func,
        data_path=yatest.common.test_source_path("data"),
        input_tables=[(
            tables.DynamicYsonTable(
                "rule_conditions_stats.yson",
                rule_estimator_api_config.RuleConditionStatsPath,
                on_write=tables.OnWrite(attributes={"schema": kv_schema.get()}),
            ), None
        )],
        output_tables=[(
            tables.DynamicYsonTable(
                "rule_conditions_stats.yson",
                rule_estimator_api_config.RuleConditionStatsPath,
                yson_format="pretty",
                on_read=tables.OnRead(row_transformer=rule_estimates_stats_transformer)
            ), tests.Diff(local=True)
        )],
        return_result=True,
    )
