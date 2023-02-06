import pytest
import yatest.common
from yt.wrapper import ypath

from crypta.lookalike.services.metrics.counts_per_parent_type_calc.lib.config_pb2 import TConfig
from crypta.lookalike.proto.segment_meta_entry_pb2 import TSegmentMetaEntry
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


VERSION_1 = "1584085850"
VERSION_2 = "1584085851"


@pytest.fixture(scope="function")
def config_file(local_yt):
    config_file_path = yatest.common.test_output_path("config.yaml")

    templater.render_file(
        yatest.common.source_path("crypta/lookalike/services/metrics/counts_per_parent_type_calc/bundle/config.yaml"),
        config_file_path,
        {
            "environment": "qa",
            "yt_proxy": local_yt.get_server(),
            "scope": "test_scope",
        },
    )
    return config_file_path


def test_basic(local_yt, local_yt_and_yql_env, config_file):
    config = yaml_config.parse_config(TConfig, config_file)

    env = {
        time_utils.CRYPTA_FROZEN_TIME_ENV: "1600000000",
    }
    env.update(local_yt_and_yql_env)

    return tests.yt_test(
        yt_client=local_yt.get_yt_client(),
        binary=yatest.common.binary_path("crypta/lookalike/services/metrics/counts_per_parent_type_calc/bin/crypta-lookalike-counts-per-parent-type-calc"),
        args=[
            "--config", config_file
        ],
        data_path=yatest.common.test_source_path("data"),
        input_tables=[
            (tables.get_yson_table_with_schema("segment_metas_1.yson", ypath.ypath_join(config.VersionsDir, VERSION_1, "segment_metas"), schema_utils.get_schema_from_proto(TSegmentMetaEntry)),
             [tests.TableIsNotChanged()]),
            (tables.get_yson_table_with_schema("segment_metas_2.yson", ypath.ypath_join(config.VersionsDir, VERSION_2, "segment_metas"), schema_utils.get_schema_from_proto(TSegmentMetaEntry)),
             [tests.TableIsNotChanged()]),
        ],
        output_tables=[
            (tables.YsonTable("metrics.yson", config.MetricsTable, yson_format="pretty"), [tests.Diff()]),
        ],
        env=env,
    )
