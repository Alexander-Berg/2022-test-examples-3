import datetime
import os

import yatest.common
from yt.wrapper import ypath

from crypta.buchhalter.services.main.lib.common.proto.ads_caesar_goal_contexts_dump_pb2 import TAdsCaesarGoalContextsDump
from crypta.buchhalter.services.main.lib.common.proto.campaign_logins_pb2 import TCampaignLogins
from crypta.buchhalter.services.main.lib.common.proto.direct_campaigns_pb2 import TDirectCampaigns
from crypta.buchhalter.services.main.lib.common.proto.direct_clients_pb2 import TDirectClients
from crypta.buchhalter.services.main.lib.common.proto.direct_hierarchial_multipliers_pb2 import TDirectHierarchialMultipliers
from crypta.buchhalter.services.main.lib.common.proto.direct_retargeting_goals_pb2 import TDirectRetargetingGoals
from crypta.buchhalter.services.main.lib.common.proto.direct_users_pb2 import TDirectUsers
from crypta.buchhalter.services.main.lib.common.proto.multipliers_pb2 import TMultipliers
from crypta.buchhalter.services.main.lib.common.proto.yabs_goal_context_pb2 import TYabsGoalContext
from crypta.lib.python import time_utils
from crypta.lib.python.yt import schema_utils
from crypta.lib.python.yt.test_helpers import (
    row_transformers,
    tables,
    tests,
)


FROZEN_TIME = "1604584320"


def test_basic(local_yt, local_yt_and_yql_env, config_file, config, date):
    diff = tests.Diff()
    expiration_time_test = tests.ExpirationTimeByTableName(datetime.timedelta(days=config.OutputTtlDays))

    env = dict(local_yt_and_yql_env)
    env[time_utils.CRYPTA_FROZEN_TIME_ENV] = FROZEN_TIME

    output_files = tests.yt_test(
        yt_client=local_yt.get_yt_client(),
        binary=yatest.common.binary_path("crypta/buchhalter/services/main/bin/crypta-buchhalter"),
        args=[
            "--config", config_file,
            "backuper",
        ],
        data_path=yatest.common.test_source_path("data"),
        input_tables=[
            (tables.get_yson_table_with_schema("direct_campaigns.yson", config.DirectCampaignsTable, schema_utils.get_schema_from_proto(TDirectCampaigns)), [tests.Exists()]),
            (tables.get_yson_table_with_schema("direct_users.yson", config.DirectUsersTable, schema_utils.get_schema_from_proto(TDirectUsers)), [tests.Exists()]),
            (tables.get_yson_table_with_schema("direct_clients.yson", config.DirectClientsTable, schema_utils.get_schema_from_proto(TDirectClients)), [tests.Exists()]),
            (
                tables.get_yson_table_with_schema(
                    "direct_hierarchial_multipliers.yson",
                    config.DirectHierarchialMultipliersTable,
                    schema_utils.get_schema_from_proto(TDirectHierarchialMultipliers)
                ),
                [tests.Exists()]
            ),
            (tables.get_yson_table_with_schema("direct_retargeting_goals.yson", config.DirectRetargetingGoalsTable, schema_utils.get_schema_from_proto(TDirectRetargetingGoals)), [tests.Exists()]),
            (tables.YsonTable("goal_context.yson", config.GoalContextTable, on_write=get_goal_context_on_write()), [tests.Exists()]),
        ],
        output_tables=[
            (tables.YsonTable("output_goal_context.yson", ypath.ypath_join(config.OutputGoalContextDir, date), yson_format="pretty"),
             [diff, expiration_time_test, tests.SchemaEquals(schema_utils.get_schema_from_proto(TYabsGoalContext))]),
            (tables.YsonTable("output_multipliers.yson", ypath.ypath_join(config.OutputMultipliersDir, date), yson_format="pretty"),
             [diff, expiration_time_test, tests.SchemaEquals(schema_utils.get_schema_from_proto(TMultipliers))]),
            (tables.YsonTable("output_retargeting_goals.yson", ypath.ypath_join(config.OutputRetargetingGoalsDir, date), yson_format="pretty"),
             [diff, expiration_time_test, tests.SchemaEquals(schema_utils.get_schema_from_proto(TDirectRetargetingGoals))]),
            (tables.YsonTable("output_campaign_logins.yson", ypath.ypath_join(config.OutputCampaignLoginsDir, date), yson_format="pretty"),
             [diff, expiration_time_test, tests.SchemaEquals(schema_utils.get_schema_from_proto(TCampaignLogins))]),
        ],
        env=env,
    )

    return {os.path.basename(item["file"]["uri"]): item for item in output_files}


def get_goal_context_on_write():
    return tables.OnWrite(
        attributes={
            "schema": schema_utils.get_schema_from_proto(TAdsCaesarGoalContextsDump),
        },
        row_transformer=row_transformers.proto_dict_to_yson(TAdsCaesarGoalContextsDump),
    )
