import datetime
import os

import yatest.common
from yt.wrapper import ypath

from crypta.buchhalter.services.main.lib.common.proto.action_checked_pb2 import TActionChecked
from crypta.buchhalter.services.main.lib.common.proto.audience_segments_info_with_login_pb2 import TAudienceSegmentsInfoWithLogin
from crypta.buchhalter.services.main.lib.common.proto.chevent_pb2 import TChevent
from crypta.buchhalter.services.main.lib.common.proto.campaign_logins_pb2 import TCampaignLogins
from crypta.buchhalter.services.main.lib.common.proto.direct_retargeting_goals_pb2 import TDirectRetargetingGoals
from crypta.buchhalter.services.main.lib.common.proto.multipliers_pb2 import TMultipliers
from crypta.buchhalter.services.main.lib.common.proto.multipliers_flattened_chevent_pb2 import TMultipliersFlattenedChevent
from crypta.buchhalter.services.main.lib.common.proto.multipliers_retargeting_id_chevent_pb2 import TMultipliersRetargetingIdChevent
from crypta.lib.python.yt import schema_utils
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
    utils,
)


def test_basic(local_yt, local_yt_and_yql_env, config_file, config, date):
    diff = tests.Diff()
    expiration_time_test = tests.ExpirationTimeByTableName(datetime.timedelta(days=config.OutputTtlDays))

    utils.create_yt_dirs(local_yt, [ypath.ypath_dirname(config.CheventLog.TrackTable)])

    output_files = tests.yt_test(
        yt_client=local_yt.get_yt_client(),
        binary=yatest.common.binary_path("crypta/buchhalter/services/main/bin/crypta-buchhalter"),
        args=[
            "--config", config_file,
            "multipliers_calc",
        ],
        data_path=yatest.common.test_source_path("data"),

        input_tables=[
            (tables.get_yson_table_with_schema("chevent_log.yson", ypath.ypath_join(config.CheventLog.SourceDir, date), schema_utils.get_schema_from_proto(TChevent)), [tests.Exists()]),
            (tables.get_yson_table_with_schema("action_checked_log.yson", ypath.ypath_join(config.ActionCheckedLogDir, date), schema_utils.get_schema_from_proto(TActionChecked)), [tests.Exists()]),
            (tables.get_yson_table_with_schema("audience_segments.yson", config.AudienceSegmentsTable, schema_utils.get_schema_from_proto(TAudienceSegmentsInfoWithLogin)), [tests.Exists()]),
            (tables.get_yson_table_with_schema("multipliers.yson", ypath.ypath_join(config.MultipliersDir, date), schema_utils.get_schema_from_proto(TMultipliers)), [tests.Exists()]),
            (
                tables.get_yson_table_with_schema("retargeting_goals.yson", ypath.ypath_join(config.RetargetingGoalsDir, date), schema_utils.get_schema_from_proto(TDirectRetargetingGoals)),
                [tests.Exists()]
            ),
            (tables.get_yson_table_with_schema("campaign_logins.yson", ypath.ypath_join(config.CampaignLoginsDir, date), schema_utils.get_schema_from_proto(TCampaignLogins)), [tests.Exists()]),
        ],
        output_tables=[
            (tables.YsonTable("track_table.yson", config.CheventLog.TrackTable, yson_format="pretty"),
             [diff]),
            (tables.YsonTable("output_retargeting_id_chevents.yson", ypath.ypath_join(config.OutputRetargetingIdCheventsDir, date), yson_format="pretty"),
             [diff, expiration_time_test, tests.SchemaEquals(schema_utils.get_schema_from_proto(TMultipliersRetargetingIdChevent))]),
            (tables.YsonTable("output_retargeting_id_stats.yson", ypath.ypath_join(config.OutputRetargetingIdStatsDir, date), yson_format="pretty"),
             [diff, expiration_time_test]),
            (tables.YsonTable("output_flattened_audience_segment_chevents.yson", ypath.ypath_join(config.OutputFlattenedAudienceSegmentCheventsDir, date), yson_format="pretty"),
             [diff, expiration_time_test, tests.SchemaEquals(schema_utils.get_schema_from_proto(TMultipliersFlattenedChevent))]),
            (tables.YsonTable("output_shared_flattened_audience_segment_chevents.yson", ypath.ypath_join(config.OutputSharedFlattenedAudienceSegmentCheventsDir, date), yson_format="pretty"),
             [diff, expiration_time_test, tests.SchemaEquals(schema_utils.get_schema_from_proto(TMultipliersFlattenedChevent))]),
        ],
        env=local_yt_and_yql_env,
    )

    return {os.path.basename(item["file"]["uri"]): item for item in output_files}
