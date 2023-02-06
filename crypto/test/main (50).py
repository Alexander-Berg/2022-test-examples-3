import os

import pytest
import yatest.common

from crypta.lib.python import (
    templater,
    time_utils,
    yaml_config,
)
from crypta.lib.python.yt.test_helpers import (
    cypress,
    tables,
    tests,
)
from crypta.lookalike.proto.yt_node_names_pb2 import TYtNodeNames
from crypta.lookalike.services.default_user_segments_exporter.proto.default_user_segments_exporter_config_pb2 import TDefaultUserSegmentsExporterConfig


YT_NODE_NAMES = TYtNodeNames()
VERSION = "1584085850"
TIMESTAMP = "1637930966"


@pytest.fixture(scope="function")
def config_file(yt_stuff):
    config_file_path = yatest.common.test_output_path("config.yaml")

    templater.render_file(
        yatest.common.source_path("crypta/lookalike/services/default_user_segments_exporter/bundle/config.yaml"),
        config_file_path,
        {
            "environment": "qa",
            "yt_proxy": yt_stuff.get_server(),
        },
    )

    return config_file_path


def test_basic(yt_stuff, config_file):
    config = yaml_config.parse_config(TDefaultUserSegmentsExporterConfig, config_file)

    def get_src_versioned_path(node):
        return os.path.join(config.VersionsDir, VERSION, node)

    return tests.yt_test(
        yt_client=yt_stuff.get_yt_client(),
        binary=yatest.common.binary_path("crypta/lookalike/services/default_user_segments_exporter/bin/crypta-lookalike-default-user-segments-exporter"),
        args=[
            "--config", config_file
        ],
        data_path=yatest.common.test_source_path("data"),
        input_tables=[
            (tables.YsonTable("user_segments.yson", get_src_versioned_path(YT_NODE_NAMES.UserSegmentsTable)), tests.Exists()),
        ],
        output_tables=[
            (cypress.CypressNode(config.ExportDir), tests.TestNodesInMapNode([tests.Diff()], tag="exported_user_segments")),
        ],
        env={
            "LOCAL_YT_SERVER": yt_stuff.get_server(),
            time_utils.CRYPTA_FROZEN_TIME_ENV: TIMESTAMP,
        },
    )
