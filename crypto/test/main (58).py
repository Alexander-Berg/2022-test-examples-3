import os

import pytest
import yatest.common
import yt.wrapper as yt

from crypta.lib.python import (
    templater,
    time_utils,
    yaml_config,
)
from crypta.lib.python.yt.test_helpers import (
    row_transformers,
    tables,
    tests,
)
from crypta.lookalike.lib.python.utils import yt_schemas
from crypta.lookalike.services.visit_log_parser.proto.parse_visit_log_job_config_pb2 import TParseVisitLogJobConfig

FROZEN_TIME = "1500000000"


@pytest.fixture(scope="function")
def config_file(yt_stuff):
    config_file_path = yatest.common.test_output_path("config.yaml")
    templater.render_file(
        yatest.common.source_path("crypta/lookalike/services/visit_log_parser/bundle/config.yaml"),
        config_file_path,
        {
            "environment": "qa",
            "yt_proxy": yt_stuff.get_server(),
        },
    )
    return config_file_path


def test_basic(yt_stuff, config_file):
    config = yaml_config.parse_config(TParseVisitLogJobConfig, config_file)

    return tests.yt_test(
        yt_client=yt_stuff.get_yt_client(),
        binary=yatest.common.binary_path("crypta/lookalike/services/visit_log_parser/bin/crypta-lookalike-visit-log-parser"),
        args=[
            "--config", config_file
        ],
        data_path=yatest.common.test_source_path("data"),
        input_tables=[
            (
                tables.YsonTable(
                    "visit_v2_log_1.yson",
                    yt.ypath_join(config.Source.SourceDir, "table_1"),
                    on_write=tables.OnWrite(attributes={"schema": yt_schemas.get_visit_v2_log_schema()})
                ), tests.TableIsNotChanged()
            ),
            (
                tables.YsonTable(
                    "visit_v2_log_2.yson",
                    yt.ypath_join(config.Source.SourceDir, "table_2"),
                    on_write=tables.OnWrite(attributes={"schema": yt_schemas.get_visit_v2_log_schema()})
                ), tests.TableIsNotChanged()
            ),
        ],
        output_tables=[
            (
                tables.YsonTable(
                    "counter_visits.yson",
                    os.path.join(config.CounterVisitsDir, "{}-{}".format(FROZEN_TIME, config.Log)),
                    yson_format="pretty"
                ), [
                    tests.Diff(),
                    tests.SchemaEquals(yt_schemas.get_metrika_counter_audiences_schema())
                ]
            ),
            (
                tables.YsonTable(
                    "goal_achievements.yson",
                    os.path.join(config.GoalAchievementsDir, "{}-{}".format(FROZEN_TIME, config.Log)),
                    yson_format="pretty"
                ), [
                    tests.Diff(),
                    tests.SchemaEquals(yt_schemas.get_goal_audiences_schema())
                ]
            ),
            (
                tables.YsonTable(
                    "errors.yson",
                    os.path.join(config.ErrorsDir, "{}-{}".format(FROZEN_TIME, config.Log)),
                    yson_format="pretty",
                    on_read=tables.OnRead(row_transformer=row_transformers.remove_frame_info(field="error"))
                ), tests.Diff()
            ),
            (
                tables.YsonTable(
                    "track_table.yson",
                    config.Source.TrackTable,
                    yson_format="pretty"
                ), tests.Diff()
            ),
        ],
        env={"LOCAL_YT_SERVER": yt_stuff.get_server(), time_utils.CRYPTA_FROZEN_TIME_ENV: FROZEN_TIME},
    )
