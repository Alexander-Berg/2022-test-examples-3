import pytest
import yatest.common
from yt.wrapper import ypath

from crypta.buchhalter.services.main.lib.common.proto.audience_segments_info_with_login_pb2 import TAudienceSegmentsInfoWithLogin
from crypta.buchhalter.services.main.lib.common.proto.segment_group_pb2 import TSegmentGroup
from crypta.buchhalter.services.main.lib.display_calc.config_pb2 import TConfig

from crypta.lib.python import (
    templater,
    yaml_config,
)
from crypta.lib.python.yt import schema_utils
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)


def get_comdep_schedule_stats_schema():
    return schema_utils.get_strict_schema([
        {"name": "schedule_nmb", "type": "int32", "required": True},
        {"name": "report_date", "type": "string"},
        {"name": "impressions", "type": "int64"},
        {"name": "clicks", "type": "int64"},
        {"name": "amount_realized_rub_gross_w_nds", "type": "double"},
    ])


def get_ado_schedules_schema():
    return schema_utils.get_strict_schema([
        {"name": "nmb", "type": "int32", "required": True},
        {"name": "line_nmb", "type": "int32", "required": True},
    ])


def get_ado_line_parameters_schema():
    return schema_utils.get_strict_schema([
        {"name": "line_nmb", "type": "int32", "required": True},
        {"name": "parameter_nmb", "type": "int32", "required": True},
        {"name": "value_guid", "type": "string"},
    ])


def get_ado_audience_segments_schema():
    return schema_utils.get_strict_schema([
        {"name": "id", "type": "string", "required": True},
        {"name": "internal_key", "type": "string", "required": True},
    ])


@pytest.fixture(scope="function")
def config_file(local_yt):
    config_file_path = yatest.common.test_output_path("config.yaml")

    templater.render_file(
        yatest.common.source_path("crypta/buchhalter/services/main/config/display_calc/config.yaml"),
        config_file_path,
        {
            "environment": "qa",
            "yt_proxy": local_yt.get_server(),
            "top_owners_threshold": 1,
        },
    )
    return config_file_path


def test_basic(local_yt, local_yt_and_yql_env, config_file):
    config = yaml_config.parse_config(TConfig, config_file)

    return tests.yt_test(
        yt_client=local_yt.get_yt_client(),
        binary=yatest.common.binary_path("crypta/buchhalter/services/main/bin/crypta-buchhalter"),
        args=[
            "--config", config_file,
            "display_calc",
        ],
        data_path=yatest.common.test_source_path("data"),
        input_tables=[
            (tables.get_yson_table_with_schema("comdep_schedule_stats.yson", config.ComdepScheduleStatsTable, get_comdep_schedule_stats_schema()),
             [tests.Exists()]),
            (tables.get_yson_table_with_schema("ado_schedules.yson", config.AdoSchedulesTable, get_ado_schedules_schema()),
             [tests.Exists()]),
            (tables.get_yson_table_with_schema("ado_line_parameters.yson", config.AdoLineParametersTable, get_ado_line_parameters_schema()),
             [tests.Exists()]),
            (tables.get_yson_table_with_schema("ado_audience_segments.yson", config.AdoAudienceSegmentsTable, get_ado_audience_segments_schema()),
             [tests.Exists()]),
            (tables.get_yson_table_with_schema("segment_owners.yson", config.SegmentOwnersTable, schema_utils.get_schema_from_proto(TAudienceSegmentsInfoWithLogin)),
             [tests.Exists()]),
            (tables.get_yson_table_with_schema("segment_groups.yson", ypath.ypath_join(config.SegmentGroupsDir, "segment_groups"), schema_utils.get_schema_from_proto(TSegmentGroup)),
             [tests.Exists()]),
        ],
        output_tables=[
            (tables.YsonTable("per_owner_stats.yson", config.PerOwnerStatsTable, yson_format="pretty"), [tests.Diff()]),
            (tables.YsonTable("top_owners_stats.yson", config.TopOwnersStatsTable, yson_format="pretty"), [tests.Diff()]),
            (tables.YsonTable("per_segment_stats.yson", config.PerSegmentStatsTable, yson_format="pretty"), [tests.Diff()]),
            (tables.YsonTable("per_group_stats.yson", config.PerGroupTable, yson_format="pretty"), [tests.Diff()]),
        ],
        env=local_yt_and_yql_env,
    )
