import datetime

import pytest
import yatest.common
from yt.wrapper import ypath

from crypta.buchhalter.services.main.lib.common.proto.multipliers_retargeting_id_chevent_pb2 import TMultipliersRetargetingIdChevent
from crypta.buchhalter.services.main.lib.common.proto.retargeting_chevent_pb2 import TRetargetingChevent
from crypta.buchhalter.services.main.lib.lookalike_calc.config_pb2 import TConfig
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
        yatest.common.source_path("crypta/buchhalter/services/main/config/lookalike_calc/config.yaml"),
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
            "lookalike_calc",
        ],
        data_path=yatest.common.test_source_path("data"),
        input_tables=[
            (
                tables.get_yson_table_with_schema(
                    "retargeting_chevents.yson",
                    ypath.ypath_join(config.TargetingChevents.SourceDir, DATE),
                    schema_utils.get_schema_from_proto(TRetargetingChevent)
                ),
                [tests.Exists()]
            ),
            (
                tables.get_yson_table_with_schema(
                    "multipliers_chevents.yson",
                    ypath.ypath_join(config.MultipliersCheventsDir, DATE),
                    schema_utils.get_schema_from_proto(TMultipliersRetargetingIdChevent)
                ),
                [tests.Exists()]
            ),
        ],
        output_tables=[
            (tables.YsonTable("targeting_stats.yson", ypath.ypath_join(config.TargetingStatsDir, DATE), yson_format="pretty"), [diff_test, expiration_time_test]),
            (tables.YsonTable("multipliers_stats.yson", ypath.ypath_join(config.MultipliersStatsDir, DATE), yson_format="pretty"), [diff_test, expiration_time_test]),
            (tables.YsonTable("total_stats.yson", ypath.ypath_join(config.TotalStatsDir, DATE), yson_format="pretty"), [diff_test, expiration_time_test]),
            (tables.YsonTable("track_table.yson", config.TargetingChevents.TrackTable, yson_format="pretty"), [diff_test]),
        ],
        env=local_yt_and_yql_env,
    )
