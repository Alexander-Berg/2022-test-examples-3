import os

import pytest
import yatest.common

from crypta.buchhalter.services.main.lib.audience_per_segment_login_metrics.config_pb2 import TConfig
from crypta.lib.python import (
    templater,
    yaml_config,
)


pytest_plugins = [
    "crypta.lib.python.solomon.test_utils.fixtures",
    "crypta.lib.python.yt.test_helpers.fixtures",
]


@pytest.fixture
def config_file(yt_stuff, mock_solomon_server):
    config_file_path = yatest.common.test_output_path("config.yaml")

    templater.render_file(
        yatest.common.source_path("crypta/buchhalter/services/main/config/audience_per_segment_login_metrics/config.yaml"),
        config_file_path,
        {
            "environment": "qa",
            "yt_proxy": yt_stuff.get_server(),
            "solomon_schema": "http",
            "solomon_host": "localhost",
            "solomon_port": mock_solomon_server.port,
        },
    )
    return config_file_path


@pytest.fixture
def config(config_file):
    os.environ["SOLOMON_TOKEN"] = "XXX"
    return yaml_config.parse_config(TConfig, config_file)
