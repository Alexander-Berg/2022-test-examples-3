import datetime

import pytest
import yatest.common
from yt.wrapper import ypath

from crypta.buchhalter.services.main.lib.audience_per_group_calc.config_pb2 import TConfig
from crypta.buchhalter.services.main.lib.common.proto.multipliers_flattened_chevent_pb2 import TMultipliersFlattenedChevent
from crypta.buchhalter.services.main.lib.common.proto.flattened_segment_chevent_pb2 import TFlattenedSegmentChevent
from crypta.buchhalter.services.main.lib.common.proto.segment_group_pb2 import TSegmentGroup
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
        yatest.common.source_path("crypta/buchhalter/services/main/config/audience_per_group_calc/config.yaml"),
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
            "audience_per_group_calc",
        ],
        data_path=yatest.common.test_source_path("data"),
        input_tables=[
            (
                tables.get_yson_table_with_schema(
                    "targeting_flattened_shared_chevents.yson",
                    ypath.ypath_join(config.TargetingFlattenedSharedChevents.SourceDir, DATE),
                    schema_utils.get_schema_from_proto(TFlattenedSegmentChevent)
                ),
                [tests.Exists()]
            ),
            (
                tables.get_yson_table_with_schema(
                    "multipliers_flattened_shared_chevents.yson",
                    ypath.ypath_join(config.MultipliersFlattenedSharedCheventsDir, DATE),
                    schema_utils.get_schema_from_proto(TMultipliersFlattenedChevent)
                ),
                [tests.Exists()]),
            (
                tables.get_yson_table_with_schema(
                    "segment_groups.yson",
                    ypath.ypath_join(config.SegmentGroupsDir, "from_arcadia"),
                    schema_utils.get_schema_from_proto(TSegmentGroup)
                ),
                [tests.Exists()]
            ),
        ],
        output_tables=[
            (tables.YsonTable("targeting_group_stats.yson", ypath.ypath_join(config.TargetingGroupStatsDir, DATE), yson_format="pretty"), [diff_test, expiration_time_test]),
            (tables.YsonTable("multipliers_group_stats.yson", ypath.ypath_join(config.MultipliersGroupStatsDir, DATE), yson_format="pretty"), [diff_test, expiration_time_test]),
            (tables.YsonTable("total_group_stats.yson", ypath.ypath_join(config.TotalGroupStatsDir, DATE), yson_format="pretty"), [diff_test, expiration_time_test]),
            (tables.YsonTable("track_table.yson", config.TargetingFlattenedSharedChevents.TrackTable, yson_format="pretty"), [diff_test]),
        ],
        env=local_yt_and_yql_env,
    )
