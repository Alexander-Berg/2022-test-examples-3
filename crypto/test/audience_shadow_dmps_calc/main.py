import datetime
import os

import pytest
import yatest.common
from yt.wrapper import ypath

from crypta.buchhalter.services.main.lib.audience_shadow_dmps_calc.config_pb2 import TConfig
from crypta.buchhalter.services.main.lib.common.proto.multipliers_flattened_chevent_pb2 import TMultipliersFlattenedChevent
from crypta.buchhalter.services.main.lib.common.proto.flattened_segment_chevent_pb2 import TFlattenedSegmentChevent
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


DATE_1 = "2020-01-01"
DATE_2 = "2020-01-02"


@pytest.fixture(scope="function")
def config_file(local_yt):
    config_file_path = yatest.common.test_output_path("config.yaml")

    templater.render_file(
        yatest.common.source_path("crypta/buchhalter/services/main/config/audience_shadow_dmps_calc/config.yaml"),
        config_file_path,
        {
            "environment": "qa",
            "yt_proxy": local_yt.get_server(),
            "output_ttl_days": utils.get_unexpired_ttl_days_for_daily(DATE_1),
        },
    )
    return config_file_path


def test_basic(local_yt, local_yt_and_yql_env, config_file):
    config = yaml_config.parse_config(TConfig, config_file)
    data_path = yatest.common.test_source_path("data")

    diff_test = tests.Diff()
    output_tests = [
        diff_test,
        tests.ExpirationTimeByTableName(datetime.timedelta(days=config.OutputTtlDays)),
    ]

    return tests.yt_test(
        yt_client=local_yt.get_yt_client(),
        binary=yatest.common.binary_path("crypta/buchhalter/services/main/bin/crypta-buchhalter"),
        args=[
            "--config", config_file,
            "audience_shadow_dmps_calc",
            "--dmp-index", os.path.join(data_path, "dmp_index.yaml"),
            "--adobe-index", os.path.join(data_path, "adobe_index.yaml"),
            "--block-lists", os.path.join(data_path, "block_lists.yaml"),
        ],
        data_path=data_path,
        input_tables=[
            (
                tables.get_yson_table_with_schema(
                    "flattened_shared_chevents_1.yson",
                    ypath.ypath_join(config.TargetingFlattenedSharedChevents.SourceDir, DATE_1),
                    schema_utils.get_schema_from_proto(TFlattenedSegmentChevent)
                ),
                [tests.Exists()]
            ),
            (
                tables.get_yson_table_with_schema(
                    "flattened_shared_chevents_2.yson",
                    ypath.ypath_join(config.TargetingFlattenedSharedChevents.SourceDir, DATE_2),
                    schema_utils.get_schema_from_proto(TFlattenedSegmentChevent)
                ),
                [tests.Exists()]
            ),
            (
                tables.get_yson_table_with_schema(
                    "multipliers_flattened_shared_chevents_1.yson",
                    ypath.ypath_join(config.MultipliersFlattenedSharedCheventsDir, DATE_1),
                    schema_utils.get_schema_from_proto(TMultipliersFlattenedChevent)
                ),
                [tests.Exists()]
            ),
            (
                tables.get_yson_table_with_schema(
                    "multipliers_flattened_shared_chevents_2.yson",
                    ypath.ypath_join(config.MultipliersFlattenedSharedCheventsDir, DATE_2),
                    schema_utils.get_schema_from_proto(TMultipliersFlattenedChevent)
                ),
                [tests.Exists()]
            ),
        ],
        output_tables=[
            (tables.YsonTable("shadow_dmps_stats_1.yson", ypath.ypath_join(config.TargetingStatsDir, DATE_1), yson_format="pretty"),
             output_tests),
            (tables.YsonTable("shadow_dmps_stats_2.yson", ypath.ypath_join(config.TargetingStatsDir, DATE_2), yson_format="pretty"),
             output_tests),
            (tables.YsonTable("multipliers_stats_1.yson", ypath.ypath_join(config.MultipliersStatsDir, DATE_1), yson_format="pretty"),
             output_tests),
            (tables.YsonTable("multipliers_stats_2.yson", ypath.ypath_join(config.MultipliersStatsDir, DATE_2), yson_format="pretty"),
             output_tests),
            (tables.YsonTable("total_stats_1.yson", ypath.ypath_join(config.TotalStatsDir, DATE_1), yson_format="pretty"),
             output_tests),
            (tables.YsonTable("total_stats_2.yson", ypath.ypath_join(config.TotalStatsDir, DATE_2), yson_format="pretty"),
             output_tests),
            (tables.YsonTable("track_table.yson", config.TargetingFlattenedSharedChevents.TrackTable, yson_format="pretty"),
             [diff_test]),
        ],
        env=local_yt_and_yql_env,
    )
