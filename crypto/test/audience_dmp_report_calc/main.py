import datetime
import os

import yatest.common
from yt.wrapper import ypath

from crypta.buchhalter.services.main.lib import audience_dmp_report_calc
from crypta.buchhalter.services.main.lib.common.proto.flattened_segment_chevent_pb2 import TFlattenedSegmentChevent
from crypta.buchhalter.services.main.lib.common.proto.multipliers_flattened_chevent_pb2 import TMultipliersFlattenedChevent
from crypta.buchhalter.services.main.lib.common import test_helpers
from crypta.lib.python.yt import schema_utils
from crypta.lib.python.yt.test_helpers import (
    cypress,
    tables,
    tests,
    utils,
)


DATE_1 = "2020-01-31"
DATE_2 = "2020-02-01"


def get_retargeting_chevents_table(config, date):
    flattened_segment_chevent_schema = schema_utils.get_schema_from_proto(TFlattenedSegmentChevent)
    local_path = "flattened_shared_chevents_{}.yson".format(date)
    yt_path = ypath.ypath_join(config.FlattenedSharedRetargetingChevents.SourceDir, date)
    return tables.get_yson_table_with_schema(local_path, yt_path, flattened_segment_chevent_schema)


def get_multipliers_chevents_table(config, date):
    multipliers_flattened_chevent_schema = schema_utils.get_schema_from_proto(TMultipliersFlattenedChevent)
    local_path = "flattened_shared_multipliers_chevents_{}.yson".format(date)
    yt_path = ypath.ypath_join(config.FlattenedSharedMultipliersCheventsDir, date)
    return tables.get_yson_table_with_schema(local_path, yt_path, multipliers_flattened_chevent_schema)


def get_output_stats_table(config, date):
    local_path = "output_stats_table_{}.yson".format(date)
    yt_path = ypath.ypath_join(config.StatsDir, date)
    return tables.YsonTable(local_path, yt_path, yson_format="pretty")


def test_basic(mock_audience_server, local_yt, local_yt_and_yql_env, config_file, config, input_stats_tables, tvm_api):
    utils.create_yt_dirs(local_yt, [config.Yt.TmpDir])
    utils.create_yt_dirs(local_yt, [ypath.ypath_dirname(config.FlattenedSharedRetargetingChevents.TrackTable)])

    canonize_excel_data = test_helpers.CanonizeExcelData(audience_dmp_report_calc.ExcelReportRow)
    diff = tests.Diff()
    stats_expiration_time_test = tests.ExpirationTimeByTableName(datetime.timedelta(days=config.StatsTtlDays))
    daily_expiration_time_test = tests.ExpirationTimeByTableName(datetime.timedelta(days=config.DailyReportTtlDays), "%Y-%m-%d.xlsx")
    monthly_expiration_time_test = tests.ExpirationTimeByTableName(datetime.timedelta(days=config.MonthlyReportTtlDays), "%Y-%m.xlsx")

    empty_reports = [
        ypath.ypath_join(config.DailyReportDir, "dmp_login_2/2020-02-01.xlsx"),
        ypath.ypath_join(config.DailyReportDir, "dmp_login_4/2020-01-31.xlsx"),
        ypath.ypath_join(config.DailyReportDir, "dmp_login_4/2020-02-01.xlsx"),
        ypath.ypath_join(config.MonthlyReportDir, "dmp_login_4/2020-01.xlsx"),
    ]
    check_report_empty_attribute = test_helpers.CheckReportEmptyAttribute(expected_getter=lambda x: any(path == x.cypress_path for path in empty_reports))

    output_files = tests.yt_test(
        yt_client=local_yt.get_yt_client(),
        binary=yatest.common.binary_path("crypta/buchhalter/services/main/bin/crypta-buchhalter"),
        args=[
            "--config", config_file,
            "audience_dmp_report_calc",
        ],
        data_path=yatest.common.test_source_path("data"),
        input_tables=[
            (tables.YsonTable("dmp_index.yson", config.DmpIndexTable), [tests.TableIsNotChanged()]),
            (get_retargeting_chevents_table(config, DATE_1), [tests.Exists()]),
            (get_retargeting_chevents_table(config, DATE_2), [tests.Exists()]),
            (get_multipliers_chevents_table(config, DATE_1), [tests.Exists()]),
            (get_multipliers_chevents_table(config, DATE_2), [tests.Exists()]),
        ] + input_stats_tables,
        output_tables=[
            (get_output_stats_table(config, DATE_1), [diff, stats_expiration_time_test]),
            (get_output_stats_table(config, DATE_2), [diff, stats_expiration_time_test]),
            (tables.YsonTable("track_table.yson", config.FlattenedSharedRetargetingChevents.TrackTable, yson_format="pretty"), [diff]),
            (tables.YsonTable("dmp_segment_ids.yson", config.DmpSegmentIdsTable, yson_format="pretty"), [diff]),
            (cypress.CypressNode(config.DailyReportDir), tests.TestNodesInMapNodeChildren([canonize_excel_data, check_report_empty_attribute, daily_expiration_time_test], tag="output_daily")),
            (tables.YsonTable("output_monthly_track_table.yson", config.MonthlyTrackTable, yson_format="pretty"), [diff]),
            (cypress.CypressNode(config.MonthlyReportDir), tests.TestNodesInMapNodeChildren([canonize_excel_data, check_report_empty_attribute, monthly_expiration_time_test], tag="output_monthly")),
        ],
        env=dict(CRYPTA_AUDIENCE_TVM_SECRET=tvm_api.get_secret(str(config.Audience.SrcTvmId)), **local_yt_and_yql_env),
    )

    result = {
        _get_filename(item): item if "file" in item else item["data"]
        for item in output_files
    }
    result["audience"] = sorted(mock_audience_server.dump_requests(), key=lambda x: x["path"])

    return result


def _get_filename(item):
    return os.path.basename(
        item["local_path"] if "local_path" in item else item["file"]["uri"]
    )
