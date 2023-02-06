import pytest
import yatest.common

from crypta.lib.python import (
    templater,
    yaml_config,
)
from crypta.profile.services.precalculate_tables.lib.common.config_pb2 import TConfig


@pytest.fixture(scope="function")
def config_file(local_yt, mock_sandbox_server_with_identifiers_udf):
    config_file_path = yatest.common.test_output_path("config.yaml")

    templater.render_file(
        yatest.common.source_path("crypta/profile/services/precalculate_tables/bundle/config.yaml"),
        config_file_path,
        {
            "environment": "qa",
            "yt_proxy": local_yt.get_server(),
            "crypta_identifier_udf_url": mock_sandbox_server_with_identifiers_udf.get_udf_url(),
        },
        strict=True,
    )

    return config_file_path


@pytest.fixture(scope="function")
def config(config_file):
    return yaml_config.parse_config(TConfig, config_file)
