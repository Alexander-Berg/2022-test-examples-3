import os

import pytest
import yatest.common
from yt.wrapper import ypath

from crypta.lookalike.lib.python.utils import yt_schemas
from crypta.lookalike.services.metrics.sizes_diff_calc.lib.config_pb2 import TConfig
from crypta.lookalike.proto.segment_meta_entry_pb2 import TSegmentMetaEntry
from crypta.lib.python import (
    templater,
    yaml_config,
)
from crypta.lib.python.yt import schema_utils
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)


VERSION_1 = "1584085850"
VERSION_2 = "1584085851"


@pytest.fixture(scope="function")
def config_file(local_yt, mock_solomon_server):
    config_file_path = yatest.common.test_output_path("config.yaml")

    templater.render_file(
        yatest.common.source_path("crypta/lookalike/services/metrics/sizes_diff_calc/bundle/config.yaml"),
        config_file_path,
        {
            "environment": "qa",
            "yt_proxy": local_yt.get_server(),
            "solomon_schema": "http",
            "solomon_host": "localhost",
            "solomon_port": mock_solomon_server.port,
        },
    )
    return config_file_path


def test_basic(local_yt, local_yt_and_yql_env, mock_solomon_server, config_file):
    os.environ["SOLOMON_TOKEN"] = "unused"
    config = yaml_config.parse_config(TConfig, config_file)

    env = {
        "SOLOMON_TOKEN": "unused",
    }
    env.update(local_yt_and_yql_env)

    result_tables = tests.yt_test(
        yt_client=local_yt.get_yt_client(),
        binary=yatest.common.binary_path("crypta/lookalike/services/metrics/sizes_diff_calc/bin/crypta-lookalike-sizes-diff-calc"),
        args=[
            "--config", config_file
        ],
        data_path=yatest.common.test_source_path("data"),
        input_tables=[
            (tables.get_yson_table_with_schema("segment_metas_1.yson", ypath.ypath_join(config.VersionsDir, VERSION_1, "segment_metas"), schema_utils.get_schema_from_proto(TSegmentMetaEntry)),
             [tests.TableIsNotChanged()]),
            (tables.get_yson_table_with_schema("segment_metas_2.yson", ypath.ypath_join(config.VersionsDir, VERSION_2, "segment_metas"), schema_utils.get_schema_from_proto(TSegmentMetaEntry)),
             [tests.TableIsNotChanged()]),
            (tables.get_yson_table_with_schema("audience_segments.yson", config.AudienceSegmentsTable, yt_schemas.get_audience_segments_audiences_schema()),
             [tests.TableIsNotChanged()]),
        ],
        output_tables=[
            (tables.YsonTable("sizes.yson", config.SizesTable, yson_format="pretty"), [tests.Diff()]),
        ],
        env=env,
    )

    return {
        "tables": result_tables,
        "solomon": mock_solomon_server.dump_push_requests()
    }
