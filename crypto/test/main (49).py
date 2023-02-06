from google.protobuf import json_format
import pytest
from yabs.proto import user_profile_pb2
import yatest.common
import yt.wrapper as yt

from crypta.lib.python import (
    templater,
    time_utils,
    yaml_config,
)
from crypta.lib.python.yt import schema_utils
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)
from crypta.lookalike.lib.python.utils import yt_schemas
from crypta.lookalike.proto.beh_profile_regular_log_entry_pb2 import TBehProfileRegularLogEntry
from crypta.lookalike.services.calc_metrika_segments.lib.proto.calc_metrika_segments_job_config_pb2 import TCalcMetrikaSegmentsJobConfig


CRYPTA_FROZEN_TIME = "1500000000"


def get_beh_profile_regular_log_row_transformer(row):
    return {
        "UniqID": row["UniqID"],
        "TimeStamp": row["TimeStamp"],
        "ProfileDump": json_format.ParseDict(row["ProfileDump"], user_profile_pb2.Profile()).SerializeToString(),
    } if isinstance(row["ProfileDump"], dict) else row


def get_beh_profile_regular_log_on_write():
    return tables.OnWrite(
        attributes={"schema": schema_utils.get_schema_from_proto(TBehProfileRegularLogEntry)},
        row_transformer=get_beh_profile_regular_log_row_transformer
    )


@pytest.fixture(scope="function")
def config_file(yt_stuff):
    config_file_path = yatest.common.test_output_path("config.yaml")
    templater.render_file(
        yatest.common.source_path("crypta/lookalike/services/calc_metrika_segments/bundle/config.yaml"),
        config_file_path,
        {
            "environment": "qa",
            "yt_proxy": yt_stuff.get_server(),
            "max_audience_size": 2,
        },
        strict=True,
    )
    return config_file_path


def test_basic(yt_stuff, config_file):
    config = yaml_config.parse_config(TCalcMetrikaSegmentsJobConfig, config_file)

    return tests.yt_test(
        yt_client=yt_stuff.get_yt_client(),
        binary=yatest.common.binary_path("crypta/lookalike/services/calc_metrika_segments/bin/crypta-lookalike-calc-metrika-segments"),
        args=[
            "--config", config_file,
        ],
        data_path=yatest.common.test_source_path("data"),
        input_tables=[
            (tables.YsonTable("beh_profile_regular_log_1.yson",
                              yt.ypath_join(config.BehProfileRegularLogDir, "table_1"), on_write=get_beh_profile_regular_log_on_write()), tests.TableIsNotChanged()),
            (tables.YsonTable("beh_profile_regular_log_2.yson",
                              yt.ypath_join(config.BehProfileRegularLogDir, "table_2"), on_write=get_beh_profile_regular_log_on_write()), tests.TableIsNotChanged()),
            (tables.YsonTable("metrika_segments.yson", config.MetrikaSegmentsDstTable, on_write=tables.OnWrite(attributes={"schema": yt_schemas.get_metrika_segments_schema()})), None),
            (tables.YsonTable("metrika_ecommerce.yson", config.MetrikaEcommerceDstTable, on_write=tables.OnWrite(attributes={"schema": yt_schemas.get_metrika_segments_schema()})), None),
            (tables.YsonTable("mobile_event.yson", config.MobileEventDstTable, on_write=tables.OnWrite(attributes={"schema": yt_schemas.get_metrika_segments_schema()})), None),
        ],
        output_tables=[
            (
                tables.YsonTable(
                    "metrika_segments.yson",
                    config.MetrikaSegmentsDstTable,
                    yson_format="pretty"
                ),
                [tests.Diff(), tests.SchemaEquals(yt_schemas.get_metrika_segments_schema())]
            ),
            (
                tables.YsonTable(
                    "metrika_ecommerce.yson",
                    config.MetrikaEcommerceDstTable,
                    yson_format="pretty"
                ),
                [tests.Diff(), tests.SchemaEquals(yt_schemas.get_metrika_segments_schema())]
            ),
            (
                tables.YsonTable(
                    "mobile_event.yson",
                    config.MobileEventDstTable,
                    yson_format="pretty"
                ),
                [tests.Diff(), tests.SchemaEquals(yt_schemas.get_metrika_segments_schema())]
            ),
            (
                tables.YsonTable(
                    "track_table.yson",
                    config.TrackTable,
                    yson_format="pretty"
                ),
                [tests.Diff()]
            ),
            (
                tables.YsonTable(
                    "errors.yson",
                    yt.ypath_join(config.ErrorsDir, CRYPTA_FROZEN_TIME),
                    yson_format="pretty"
                ),
                [tests.IsAbsent()]
            ),
        ],
        env={
            "YT_TOKEN": "FAKE",
            time_utils.CRYPTA_FROZEN_TIME_ENV: CRYPTA_FROZEN_TIME,
        },
    )
