import pytest
import yatest.common

from crypta.lib.python import (
    templater,
    yaml_config,
)
from crypta.profile.services.disable_unused_segments.proto.config_pb2 import TConfig


pytest_plugins = [
    "crypta.lib.python.yql.test_helpers.fixtures",
    "crypta.lib.python.smtp.test_helpers.fixtures",
]


@pytest.fixture(scope="function")
def config_file(local_yt, local_smtp_server):
    config_file_path = yatest.common.test_output_path("config.yaml")
    smtp_host, smtp_port = local_smtp_server.local_address

    templater.render_file(
        yatest.common.source_path("crypta/profile/services/disable_unused_segments/bundle/config.yaml"),
        config_file_path,
        {
            "environment": "qa",
            "yt_proxy": local_yt.get_server(),
            "smtp_host": smtp_host,
            "smtp_port": smtp_port,
        },
        strict=True,
    )

    return config_file_path


@pytest.fixture(scope="function")
def config(config_file):
    return yaml_config.parse_config(TConfig, config_file)
