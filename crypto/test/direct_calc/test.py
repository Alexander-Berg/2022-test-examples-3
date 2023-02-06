import datetime

import pytest
import yatest.common
from yt.wrapper import ypath

from crypta.buchhalter.services.main.lib.common.proto.action_checked_pb2 import TActionChecked
from crypta.buchhalter.services.main.lib.common.proto.chevent_pb2 import TChevent
from crypta.buchhalter.services.main.lib.common.proto.yabs_goal_context_pb2 import TYabsGoalContext
from crypta.buchhalter.services.main.lib.direct_calc.config_pb2 import TConfig
from crypta.lib.python import (
    templater,
    yaml_config,
)
from crypta.lib.python.yt import schema_utils
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
    utils,
)


DATE = "2020-01-01"


@pytest.fixture(scope="function")
def config_file(local_yt):
    config_file_path = yatest.common.test_output_path("config.yaml")

    templater.render_file(
        yatest.common.source_path("crypta/buchhalter/services/main/config/direct_calc/config.yaml"),
        config_file_path,
        {
            "environment": "qa",
            "yt_proxy": local_yt.get_server(),
            "output_ttl_days": utils.get_unexpired_ttl_days_for_daily(DATE),
        },
    )
    return config_file_path


def test_basic(local_yt, local_yt_and_yql_env, config_file):
    config = yaml_config.parse_config(TConfig, config_file)

    diff_test = tests.Diff()
    expiration_time_test = tests.ExpirationTimeByTableName(datetime.timedelta(days=config.OutputTtlDays))

    return tests.yt_test(
        yt_client=local_yt.get_yt_client(),
        binary=yatest.common.binary_path("crypta/buchhalter/services/main/bin/crypta-buchhalter"),
        args=[
            "--config", config_file,
            "direct_calc",
        ],
        data_path=yatest.common.test_source_path("data"),
        input_tables=[
            (tables.get_yson_table_with_schema("chevent_log.yson", ypath.ypath_join(config.CheventLog.SourceDir, DATE), schema_utils.get_schema_from_proto(TChevent)), [tests.Exists()]),
            (tables.get_yson_table_with_schema("action_checked_log.yson", ypath.ypath_join(config.ActionCheckedLogDir, DATE), schema_utils.get_schema_from_proto(TActionChecked)), [tests.Exists()]),
            (tables.get_yson_table_with_schema("goal_context.yson", ypath.ypath_join(config.GoalContextDir, DATE), schema_utils.get_schema_from_proto(TYabsGoalContext)), [tests.Exists()]),
        ],
        output_tables=[
            (tables.YsonTable("retargeting_chevents.yson", ypath.ypath_join(config.RetargetingCheventsDir, DATE), yson_format="pretty"), [diff_test, expiration_time_test]),
            (tables.YsonTable("retargeting_stats.yson", ypath.ypath_join(config.RetargetingStatsDir, DATE), yson_format="pretty"), [diff_test, expiration_time_test]),
            (tables.YsonTable("track_table.yson", config.CheventLog.TrackTable, yson_format="pretty"), [diff_test]),
        ],
        env=local_yt_and_yql_env,
    )
