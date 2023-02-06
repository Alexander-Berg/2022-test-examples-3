import datetime

import pytest
import yatest.common
from yt.wrapper import ypath

from crypta.buchhalter.services.main.lib.adfox_calc.config_pb2 import TConfig
from crypta.buchhalter.services.main.lib.common.proto.audience_segments_info_with_login_pb2 import TAudienceSegmentsInfoWithLogin
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


def get_adfox_event_log_schema():
    return schema_utils.get_strict_schema([
        {"name": "request_session", "type": "string"},
        {"name": "campaign_id", "type": "uint64"},
        {"name": "tp_id", "type": "uint64"},
        {"name": "ignore", "type": "string"},
        {"name": "banner_id", "type": "uint64"},
        {"name": "flag_virtual", "type": "uint64"},
        {"name": "owner_id", "type": "uint64"},
        {"name": "audience_hit.dmp_id", "type": "any"},
        {"name": "audience_hit.segment_id", "type": "any"},
        {"name": "audience_hit.tariff_id", "type": "any"},
        {"name": "click", "type": "uint64"},
        {"name": "view", "type": "uint64"},
        {"name": "load", "type": "uint64"}
    ])


def get_adfox_clients_schema():
    return schema_utils.get_strict_schema([
        {"name": "id", "type": "int64"},
        {"name": "yandex_login", "type": "string"},
    ])


def get_dmp_tariff_data_schema():
    return schema_utils.get_strict_schema([
        {"name": "dmp_id", "type": "uint64"},
        {"name": "tariff_id", "type": "uint64"},
        {"name": "name", "type": "string"},
        {"name": "price_rub", "type": "double"},
    ])


@pytest.fixture(scope="function")
def config_file(local_yt, mock_sandbox_server_with_identifiers_udf):
    config_file_path = yatest.common.test_output_path("config.yaml")

    templater.render_file(
        yatest.common.source_path("crypta/buchhalter/services/main/config/adfox_calc/config.yaml"),
        config_file_path,
        {
            "environment": "qa",
            "yt_proxy": local_yt.get_server(),
            "top_owners_threshold": 1,
            "crypta_identifier_udf_url": mock_sandbox_server_with_identifiers_udf.get_udf_url(),
            "output_ttl_days": utils.get_unexpired_ttl_days_for_daily(DATE),
        },
    )
    return config_file_path


def test_basic(local_yt, local_yt_and_yql_env, config_file):
    config = yaml_config.parse_config(TConfig, config_file)

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
            "adfox_calc",
        ],
        data_path=yatest.common.test_source_path("data"),
        input_tables=[
            (tables.get_yson_table_with_schema("adfox_event_log.yson", ypath.ypath_join(config.AdfoxEventLog.SourceDir, DATE), get_adfox_event_log_schema()),
             [tests.TableIsNotChanged()]),
            (tables.get_yson_table_with_schema("adfox_clients.yson", config.AdfoxClientsTable, get_adfox_clients_schema()),
             [tests.TableIsNotChanged()]),
            (tables.get_yson_table_with_schema("dmp_tariff_data.yson", config.DmpTariffDataTable, get_dmp_tariff_data_schema()),
             [tests.TableIsNotChanged()]),
            (tables.get_yson_table_with_schema("segment_owners.yson", config.AudienceSegmentOwnersTable, schema_utils.get_schema_from_proto(TAudienceSegmentsInfoWithLogin)),
             [tests.TableIsNotChanged()]),
            (tables.get_yson_table_with_schema("groups.yson", ypath.ypath_join(config.GroupsDir, "groups"), schema_utils.get_schema_from_proto(TSegmentGroup)),
             [tests.TableIsNotChanged()]),
        ],
        output_tables=[
            (tables.YsonTable("paid_per_segment_stats.yson", ypath.ypath_join(config.PaidPerSegmentStatsDir, DATE), yson_format="pretty"), output_tests),
            (tables.YsonTable("paid_per_dmp_stats.yson", ypath.ypath_join(config.PaidPerDmpStatsDir, DATE), yson_format="pretty"), output_tests),
            (tables.YsonTable("paid_total_stats.yson", ypath.ypath_join(config.PaidTotalStatsDir, DATE), yson_format="pretty"), output_tests),

            (tables.YsonTable("audience_per_segment_stats.yson", ypath.ypath_join(config.AudiencePerSegmentStatsDir, DATE), yson_format="pretty"), output_tests),
            (tables.YsonTable("audience_per_owner_stats.yson", ypath.ypath_join(config.AudiencePerOwnerStatsDir, DATE), yson_format="pretty"), output_tests),
            (tables.YsonTable("audience_top_owners_stats.yson", ypath.ypath_join(config.AudienceTopOwnersStatsDir, DATE), yson_format="pretty"), output_tests),
            (tables.YsonTable("audience_per_group_stats.yson", ypath.ypath_join(config.AudiencePerGroupStatsDir, DATE), yson_format="pretty"), output_tests),
            (tables.YsonTable("audience_total_stats.yson", ypath.ypath_join(config.AudienceTotalStatsDir, DATE), yson_format="pretty"), output_tests),

            (tables.YsonTable("track_table.yson", config.AdfoxEventLog.TrackTable, yson_format="pretty"), [diff_test]),
        ],
        env=local_yt_and_yql_env,
    )
