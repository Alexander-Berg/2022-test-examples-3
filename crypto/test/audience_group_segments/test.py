import os
import pytest
import yatest.common

from crypta.buchhalter.services.main.lib.audience_group_segments.config_pb2 import TConfig
from crypta.buchhalter.services.main.lib.common.proto.audience_segments_info_with_login_pb2 import TAudienceSegmentsInfoWithLogin
from crypta.buchhalter.services.main.lib.common.proto.login_group_pb2 import TLoginGroup
from crypta.buchhalter.services.main.lib.common.proto.segment_group_pb2 import TSegmentGroup
from crypta.lib.python import (
    templater,
    yaml_config,
)
from crypta.lib.python.yt import schema_utils
from crypta.lib.python.yt.test_helpers import (
    tables,
    tests,
)


@pytest.fixture(scope="function")
def config_file(local_yt):
    config_file_path = yatest.common.test_output_path("config.yaml")

    templater.render_file(
        yatest.common.source_path("crypta/buchhalter/services/main/config/audience_group_segments/config.yaml"),
        config_file_path,
        {
            "environment": "qa",
            "yt_proxy": local_yt.get_server(),
        },
    )
    return config_file_path


def test_basic(local_yt, local_yt_and_yql_env, config_file):
    config = yaml_config.parse_config(TConfig, config_file)
    data_path = yatest.common.test_source_path("data")

    return tests.yt_test(
        yt_client=local_yt.get_yt_client(),
        binary=yatest.common.binary_path("crypta/buchhalter/services/main/bin/crypta-buchhalter"),
        args=[
            "--config", config_file,
            "audience_group_segments",
            "--dmp-index", os.path.join(data_path, "dmp_index.yaml"),
            "--adobe-index", os.path.join(data_path, "adobe_index.yaml"),
            "--shadow-dmp-index", os.path.join(data_path, "shadow_dmp_index.yaml"),
            "--groups", os.path.join(data_path, "groups.yaml"),
        ],
        data_path=data_path,
        input_tables=[
            (tables.get_yson_table_with_schema("segments_with_logins.yson", config.SegmentsWithLoginsTable, schema_utils.get_schema_from_proto(TAudienceSegmentsInfoWithLogin)),
             [tests.Exists()]),
        ],
        output_tables=[
            (tables.YsonTable("login_groups_table.yson", config.LoginGroupsTable, yson_format="pretty"),
             [tests.Diff(), tests.SchemaEquals(schema_utils.get_schema_from_proto(TLoginGroup))]),
            (tables.YsonTable("segment_groups_table.yson", config.SegmentGroupsTable, yson_format="pretty"),
             [tests.Diff(), tests.SchemaEquals(schema_utils.get_schema_from_proto(TSegmentGroup))]),
        ],
        env=local_yt_and_yql_env,
    )
