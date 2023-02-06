import datetime

import pytest
import yatest.common
from yt.wrapper import ypath

from crypta.buchhalter.services.main.lib.audience_filter_shared_chevents.config_pb2 import TConfig
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
        yatest.common.source_path("crypta/buchhalter/services/main/config/audience_filter_shared_chevents/config.yaml"),
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
            "audience_filter_shared_chevents",
        ],
        data_path=yatest.common.test_source_path("data"),
        input_tables=[
            (tables.get_yson_table_with_schema("flattened_chevents_1.yson", ypath.ypath_join(config.FlattenedChevents.SourceDir, DATE_1), schema_utils.get_schema_from_proto(TFlattenedSegmentChevent)),
             [tests.Exists()]),
            (tables.get_yson_table_with_schema("flattened_chevents_2.yson", ypath.ypath_join(config.FlattenedChevents.SourceDir, DATE_2), schema_utils.get_schema_from_proto(TFlattenedSegmentChevent)),
             [tests.Exists()]),
        ],
        output_tables=[
            (tables.YsonTable("flattened_shared_chevents_1.yson", ypath.ypath_join(config.FlattenedSharedCheventsDir, DATE_1), yson_format="pretty"),
             [diff_test, expiration_time_test, tests.SchemaEquals(schema_utils.get_schema_from_proto(TFlattenedSegmentChevent))]),
            (tables.YsonTable("flattened_shared_chevents_2.yson", ypath.ypath_join(config.FlattenedSharedCheventsDir, DATE_2), yson_format="pretty"),
             [diff_test, expiration_time_test, tests.SchemaEquals(schema_utils.get_schema_from_proto(TFlattenedSegmentChevent))]),
            (tables.YsonTable("track_table.yson", config.FlattenedChevents.TrackTable, yson_format="pretty"), [diff_test]),
        ],
        env=local_yt_and_yql_env,
    )
