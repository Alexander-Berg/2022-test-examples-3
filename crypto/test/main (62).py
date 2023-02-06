import yatest.common
from yt.wrapper import ypath

from crypta.lib.python.yt import schema_utils
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)


DATE_1 = "2020-02-01"
DATE_2 = "2020-02-02"


def test_basic(local_yt, local_yt_and_yql_env, config_file, config):
    return tests.yt_test(
        yt_client=local_yt.get_yt_client(),
        binary=yatest.common.binary_path("crypta/profile/services/calc_lab_segments_stats/bin/crypta-profile-calc-lab-segments-stats"),
        args=[
            "--config", config_file,
        ],
        data_path=yatest.common.test_source_path("data"),
        input_tables=[
            (tables.YsonTable("segments.yson", config.SegmentsTable),
             [tests.TableIsNotChanged()]),

            # DATE_1
            (tables.get_yson_table_with_schema("direct_retargeting_1.yson", ypath.ypath_join(config.DirectRetargeting.SourceDir, DATE_1), get_direct_retargeting_schema()),
             [tests.TableIsNotChanged()]),
            (tables.get_yson_table_with_schema("direct_multipliers_1.yson", ypath.ypath_join(config.DirectMultipliersDir, DATE_1), get_direct_multipliers_schema()),
             [tests.TableIsNotChanged()]),
            (tables.get_yson_table_with_schema("adfox_1.yson", ypath.ypath_join(config.AdfoxDir, DATE_1), get_adfox_schema()),
             [tests.TableIsNotChanged()]),

            # DATE_2
            (tables.get_yson_table_with_schema("direct_retargeting_2.yson", ypath.ypath_join(config.DirectRetargeting.SourceDir, DATE_2), get_direct_retargeting_schema()),
             [tests.TableIsNotChanged()]),
            (tables.get_yson_table_with_schema("direct_multipliers_2.yson", ypath.ypath_join(config.DirectMultipliersDir, DATE_2), get_direct_multipliers_schema()),
             [tests.TableIsNotChanged()]),
            (tables.get_yson_table_with_schema("adfox_2.yson", ypath.ypath_join(config.AdfoxDir, DATE_2), get_adfox_schema()),
             [tests.TableIsNotChanged()]),
        ],
        output_tables=[
            (tables.YsonTable("export_stats_date_1.yson", ypath.ypath_join(config.ExportsStatsDir, DATE_1), yson_format="pretty"), [tests.Diff()]),
            (tables.YsonTable("export_stats_date_2.yson", ypath.ypath_join(config.ExportsStatsDir, DATE_2), yson_format="pretty"), [tests.Diff()]),
            (tables.YsonTable("track_table.yson", config.DirectRetargeting.TrackTable, yson_format="pretty"), [tests.Diff()]),
        ],
        env=local_yt_and_yql_env,
    )


def get_direct_retargeting_schema():
    return schema_utils.get_strict_schema([
        {"name": "retargeting_id", "type": "uint64", "sort_order": "ascending"},
        {"name": "keyword_id", "type": "uint64", "sort_order": "ascending"},
        {"name": "keyword_value", "type": "uint64", "sort_order": "ascending"},
        {"name": "placeid", "type": "int64", "sort_order": "ascending"},
        {"name": "campaigns_count", "type": "uint64"},
        {"name": "shows", "type": "uint64"},
        {"name": "clicks", "type": "uint64"},
        {"name": "total_cost", "type": "double"},
        {"name": "options_commerce", "type": "boolean"},
    ])


def get_direct_multipliers_schema():
    return schema_utils.get_strict_schema([
        {"name": "retargeting_id", "type": "int64", "required": True},
        {"name": "placeid", "type": "string", "required": True},
        {"name": "campaigns_count", "type": "uint64", "required": True},
        {"name": "shows", "type": "uint64", "required": True},
        {"name": "clicks", "type": "uint64", "required": True},
        {"name": "total_cost", "type": "double"},
        {"name": "options_commerce", "type": "boolean"},
    ])


def get_adfox_schema():
    return schema_utils.get_strict_schema([
        {"name": "SegmentId", "type": "uint64"},
        {"name": "Shows", "type": "uint64"},
        {"name": "Clicks", "type": "int64"},
    ])
