import pytest
import yatest.common

from crypta.buchhalter.services.main.lib.audience_add_logins_to_segments.config_pb2 import TConfig
from crypta.buchhalter.services.main.lib.common.proto.audience_segments_info_pb2 import TAudienceSegmentsInfo
from crypta.buchhalter.services.main.lib.common.proto.puid_2_login_entry_pb2 import TPuid2LoginEntry
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
        yatest.common.source_path("crypta/buchhalter/services/main/config/audience_add_logins_to_segments/config.yaml"),
        config_file_path,
        {
            "environment": "qa",
            "yt_proxy": local_yt.get_server(),
        },
    )
    return config_file_path


def test_basic(local_yt, local_yt_and_yql_env, config_file):
    config = yaml_config.parse_config(TConfig, config_file)

    return tests.yt_test(
        yt_client=local_yt.get_yt_client(),
        binary=yatest.common.binary_path("crypta/buchhalter/services/main/bin/crypta-buchhalter"),
        args=[
            "--config", config_file,
            "audience_add_logins_to_segments",
        ],
        data_path=yatest.common.test_source_path("data"),
        input_tables=[
            (tables.get_yson_table_with_schema("audience_segments.yson", config.SegmentsTable, schema_utils.get_schema_from_proto(TAudienceSegmentsInfo)),
             [tests.Exists()]),
            (tables.get_yson_table_with_schema("puid_to_login.yson", config.PuidToLoginTable, schema_utils.get_schema_from_proto(TPuid2LoginEntry)),
             [tests.Exists()]),
        ],
        output_tables=[
            (tables.YsonTable("segments_with_logins.yson", config.SegmentsWithLoginsTable, yson_format="pretty"), [tests.Diff()]),
        ],
        env=local_yt_and_yql_env,
    )
