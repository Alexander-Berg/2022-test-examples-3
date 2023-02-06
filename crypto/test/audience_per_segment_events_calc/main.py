import datetime

import pytest
import yatest.common
from yt.wrapper import ypath

from crypta.buchhalter.services.main.lib.audience_per_segment_events_calc.config_pb2 import TConfig
from crypta.buchhalter.services.main.lib.common.proto.audience_segments_info_with_login_pb2 import TAudienceSegmentsInfoWithLogin
from crypta.buchhalter.services.main.lib.common.proto.direct_campaigns_pb2 import TDirectCampaigns
from crypta.buchhalter.services.main.lib.common.proto.direct_clients_pb2 import TDirectClients
from crypta.buchhalter.services.main.lib.common.proto.direct_users_pb2 import TDirectUsers
from crypta.buchhalter.services.main.lib.common.proto.flattened_segment_chevent_pb2 import TFlattenedSegmentChevent
from crypta.buchhalter.services.main.lib.common.proto.retargeting_chevent_pb2 import TRetargetingChevent
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
        yatest.common.source_path("crypta/buchhalter/services/main/config/audience_per_segment_events_calc/config.yaml"),
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

    diff_test = tests.Diff()
    expiration_time_test = tests.ExpirationTimeByTableName(datetime.timedelta(days=config.OutputTtlDays))

    return tests.yt_test(
        yt_client=local_yt.get_yt_client(),
        binary=yatest.common.binary_path("crypta/buchhalter/services/main/bin/crypta-buchhalter"),
        args=[
            "--config", config_file,
            "audience_per_segment_events_calc",
        ],
        data_path=yatest.common.test_source_path("data"),
        input_tables=[
            (tables.get_yson_table_with_schema("retargeting_chevents_1.yson", ypath.ypath_join(config.RetargetingChevents.SourceDir, DATE_1), schema_utils.get_schema_from_proto(TRetargetingChevent)),
             [tests.Exists()]),
            (tables.get_yson_table_with_schema("retargeting_chevents_2.yson", ypath.ypath_join(config.RetargetingChevents.SourceDir, DATE_2), schema_utils.get_schema_from_proto(TRetargetingChevent)),
             [tests.Exists()]),
            (tables.get_yson_table_with_schema("direct_campaigns.yson", config.DirectCampaignsTable, schema_utils.get_schema_from_proto(TDirectCampaigns)), [tests.Exists()]),
            (tables.get_yson_table_with_schema("direct_clients.yson", config.DirectClientsTable, schema_utils.get_schema_from_proto(TDirectClients)),  [tests.Exists()]),
            (tables.get_yson_table_with_schema("direct_users.yson", config.DirectUsersTable, schema_utils.get_schema_from_proto(TDirectUsers)), [tests.Exists()]),
            (tables.get_yson_table_with_schema("segments_with_logins.yson", config.SegmentsWithLoginsTable, schema_utils.get_schema_from_proto(TAudienceSegmentsInfoWithLogin)), [tests.Exists()]),
        ],
        output_tables=[
            (tables.YsonTable("per_segment_events_1.yson", ypath.ypath_join(config.SegmentCheventsDir, DATE_1), yson_format="pretty"),
             [diff_test, expiration_time_test, tests.SchemaEquals(schema_utils.get_schema_from_proto(TFlattenedSegmentChevent))]),
            (tables.YsonTable("per_segment_events_2.yson", ypath.ypath_join(config.SegmentCheventsDir, DATE_2), yson_format="pretty"),
             [diff_test, expiration_time_test, tests.SchemaEquals(schema_utils.get_schema_from_proto(TFlattenedSegmentChevent))]),
            (tables.YsonTable("track_table.yson", config.RetargetingChevents.TrackTable, yson_format="pretty"), [diff_test]),
        ],
        env=local_yt_and_yql_env,
    )
